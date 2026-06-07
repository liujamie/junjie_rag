/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.junjie.rag.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.junjie.rag.annotation.Loggable;
import com.junjie.rag.common.SensitiveWordHolder;
import com.junjie.rag.common.ApplicationConstant;
import com.junjie.rag.context.BaseContext;
import com.junjie.rag.entity.LlmCallRecord;
import com.junjie.rag.service.HybridSearchService;
import com.junjie.rag.service.LlmCallRecordService;
import com.junjie.rag.service.QueryRewriteService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import com.junjie.rag.service.RerankService;
import com.junjie.rag.tools.RagTool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.springframework.ai.document.Document;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Tag(name = "AiRagController", description = "Rag接口")
@Slf4j
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/ai")
public class AiRagController {

    ChatClient chatClient;
    VectorStore vectorStore;

    @Autowired
    @Qualifier("aiStreamExecutor")
    private ExecutorService aiStreamExecutor;

    @Autowired
    private SensitiveWordHolder sensitiveWordHolder;

    @Autowired
    @Qualifier("ragResultCache")
    private Cache<String, String> ragResultCache;

    @Autowired
    private RerankService rerankService;

    @Autowired
    private QueryRewriteService queryRewriteService;

    @Autowired
    private HybridSearchService hybridSearchService;

    @Autowired
    private LlmCallRecordService llmCallRecordService;

    @Autowired
    private MeterRegistry meterRegistry;

    private final ChatMemory chatMemory;

    public AiRagController(ChatModel chatModel, ChatMemory chatMemory, VectorStore vectorStore,
                            RagTool ragTool) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                你是一家名为"LIU-RAG-AI"的知识库系统的对话助手。请友好乐于助人，充满喜悦地回复。

                你的回答来源分为两类：
                1. 【知识库】—— 下方 DOCUMENTS 中的内容，仅用于回答与已上传文档相关的问题
                2. 【网络搜索】—— 调用 searchWeb 工具获取实时信息

                注意规则：
                - 如果用户问的是最近新闻、实时事件、网络上的最新信息，直接调用 searchWeb 搜索，不要依赖知识库
                - 如果用户问的是与知识库文档（如Java面试题）相关的问题，优先使用知识库内容
                - 如果DOCUMENTS为空或内容不相关，说明知识库没有相关文档，此时必须调用 searchWeb 搜索

                你有以下工具可用：
                1. getCurrentTime / getCurrentDate - 用户询问时间日期时调用
                2. searchWeb(query) - 搜索网络获取最新信息，关键词越精确越好

                当用户问题涉及实时信息、新闻、非知识库内容时，优先使用 searchWeb 工具。
                """)
                .defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(chatMemory).build(),
                        SimpleLoggerAdvisor.builder().build()
                        )
                .defaultTools(ragTool)
                .build();

        this.vectorStore = vectorStore;
        this.chatMemory = chatMemory;
    }

    private String buildRagPrompt(String documentsText) {
        return """
                你是一家名为"LIU-RAG-AI"的知识库系统的对话助手。请友好乐于助人，充满喜悦地回复。

                你的回答来源分为两类：
                1. 【知识库】—— 下方 DOCUMENTS 中的内容，仅用于回答与已上传文档相关的问题
                2. 【网络搜索】—— 调用 searchWeb 工具获取实时信息

                注意规则：
                - 如果用户问的是最近新闻、实时事件、网络上的最新信息，直接调用 searchWeb 搜索，不要依赖知识库
                - 如果用户问的是与知识库文档（如Java面试题）相关的问题，优先使用知识库内容
                - 如果DOCUMENTS为空或内容不相关，说明知识库没有相关文档，此时必须调用 searchWeb 搜索

                DOCUMENTS:
                %s

                你有以下工具可用：
                1. getCurrentTime / getCurrentDate - 用户询问时间日期时调用
                2. searchWeb(query) - 搜索网络获取最新信息，关键词越精确越好

                当用户问题涉及实时信息、新闻、非知识库内容时，优先使用 searchWeb 工具。
                """.formatted(documentsText.isEmpty() ? "（无相关文档）" : documentsText);
    }

    @Operation(summary = "rag", description = "Rag对话接口")
    @GetMapping(value = "/rag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Loggable
    public SseEmitter generate(@RequestParam(value = "message", defaultValue = "你好") String message) throws IOException {

        // AC 自动机敏感词过滤（O(消息长度)）
        List<String> matched = sensitiveWordHolder.get().matches(message);
        if (!matched.isEmpty()) {
            SseEmitter emitter = new SseEmitter();
            try { emitter.send("包含敏感词:" + String.join(",", matched)); } catch (IOException ignored) {}
            emitter.complete();
            return emitter;
        }

        // 检查缓存
        String cached = ragResultCache.getIfPresent(message);
        if (cached != null) {
            SseEmitter cachedEmitter = new SseEmitter();
            aiStreamExecutor.execute(() -> {
                try {
                    cachedEmitter.send(cached);
                } catch (IOException e) {
                    cachedEmitter.completeWithError(e);
                    return;
                }
                cachedEmitter.complete();
                    try {
                        meterRegistry.counter("rag.request.total", "status", "cache").increment();
                    } catch (Exception ignored) {}
            });
            return cachedEmitter;
        }

        SseEmitter emitter = new SseEmitter(300000L);
        Long currentId = BaseContext.getCurrentId();
        StringBuilder fullContent = new StringBuilder();

        aiStreamExecutor.execute(() -> {
            long tRequestStart = System.currentTimeMillis();
            try {
                // === 1. 查询改写 ===
                long t0 = System.currentTimeMillis();
                List<Message> historyMessages = chatMemory.get(String.valueOf(currentId));
                java.util.List<String> userQuestions = historyMessages.stream()
                        .filter(m -> m.getMessageType().name().equals("USER"))
                        .map(org.springframework.ai.chat.messages.Message::getText)
                        .collect(java.util.stream.Collectors.toList());
                String historyText = userQuestions.stream()
                        .skip(Math.max(0, userQuestions.size() - 3))
                        .collect(java.util.stream.Collectors.joining(" | "));
                String searchQuery = queryRewriteService.rewrite(message, historyText);
                long t1 = System.currentTimeMillis();
                log.info("[RAG耗时] 查询改写: {}ms | 检索查询: '{}'", t1 - t0, searchQuery);

                if (searchQuery == null || searchQuery.isBlank()) {
                    String hint = "您的问题有点模糊，能否补充一些关键信息？例如：\n• 您想了解哪个知识点？\n• 需要查询哪方面的内容？\n• 有什么具体的问题需要解答？\n\n您可以这样提问：\n\"什么是RAG？\"\n\"Java有哪些设计模式？\"\n\"帮我总结一下知识库中关于性能优化的内容\"";
                    emitter.send(hint);
                    emitter.complete();
                    log.info("[RAG耗时分析] 用户提问不明确，已提示补充信息");
                    return;
                }

                // === 2. 混合检索（向量 + BM25 + RRF） ===
                List<Document> docs = hybridSearchService.hybridSearch(searchQuery, 0.5d);
                long t2 = System.currentTimeMillis();
                int preRerankCount = docs.size();
                log.info("[RAG耗时] 向量检索: {}ms | 召回: {}条", t2 - t1, preRerankCount);

                // === 3. Rerank 重排序 ===
                if (docs.size() > 1) {
                    docs = rerankService.rerank(searchQuery, docs);
                    docs = docs.subList(0, Math.min(3, docs.size()));
                }
                long t3 = System.currentTimeMillis();
                log.info("[RAG耗时] Rerank重排序: {}ms | 保留: {}条", t3 - t2, docs.size());

                // === 4. 构建提示词 ===
                String documentsText = docs.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n\n---\n\n"));
                String systemPrompt = buildRagPrompt(documentsText);
                long t4 = System.currentTimeMillis();
                log.info("[RAG耗时] 构建提示词: {}ms", t4 - t3);

                // === 5. LLM 流式响应（失败自动重试3次） ===
                long tLlmStart = System.currentTimeMillis();
                chatClient.prompt()
                        .user(message)
                        .system(systemPrompt)
                        .advisors(a -> a.param("current_Date", LocalDate.now().toString()).param(ChatMemory.CONVERSATION_ID, currentId))
                        .stream()
                        .content()
                        .retryWhen(reactor.util.retry.Retry.backoff(3, java.time.Duration.ofSeconds(1))
                                .filter(e -> e.getMessage() != null && e.getMessage().contains("Connection reset"))
                                .onRetryExhaustedThrow((spec, sig) -> sig.failure()))
                        .subscribe(
                                chunk -> {
                                    try {
                                        fullContent.append(chunk);
                                        emitter.send(chunk);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                },
                                emitter::completeWithError,
                                () -> {
                                    long tEnd = System.currentTimeMillis();
                                    ragResultCache.put(message, fullContent.toString());
                                    emitter.complete();
                                    // === 耗时分析报告 ===
                                    long rewriteMs = t1 - t0;
                                    long searchMs = t2 - t1;
                                    long rerankMs = t3 - t2;
                                    long llmMs = tEnd - tLlmStart;
                                    long totalMs = tEnd - tRequestStart;
                                    log.info("==============================================");
                                    log.info("[RAG耗时分析] 总耗时: {}ms", totalMs);
                                    log.info("[RAG耗时分析]   ├── 查询改写: {}ms ({}%)", rewriteMs, rewriteMs * 100 / totalMs);
                                    log.info("[RAG耗时分析]   ├── 向量检索: {}ms ({}%)", searchMs, searchMs * 100 / totalMs);
                                    log.info("[RAG耗时分析]   ├── Rerank:    {}ms ({}%)", rerankMs, rerankMs * 100 / totalMs);
                                    log.info("[RAG耗时分析]   └── LLM输出:  {}ms ({}%)", llmMs, llmMs * 100 / totalMs);
                                    log.info("[RAG耗时分析] 响应长度: {} 字符", fullContent.length());
                                    // 分析瓶颈
                                    long[] stages = {rewriteMs, searchMs, rerankMs, llmMs};
                                    String[] names = {"查询改写", "向量检索", "Rerank", "LLM输出"};
                                    int maxIdx = 0;
                                    for (int i = 1; i < stages.length; i++) if (stages[i] > stages[maxIdx]) maxIdx = i;
                                    double pct = stages[maxIdx] * 100.0 / totalMs;
                                    String pctStr = String.format("%.1f", pct);
                                    log.info("[RAG耗时分析] ⚠ 耗时最长阶段: {} ({}ms, {}%)", names[maxIdx], stages[maxIdx], pctStr);
                                    if (maxIdx == 0) log.info("[RAG优化建议] 查询改写慢 → 考虑减少历史轮数(当前取4条)或改用更快的模型");
                                    if (maxIdx == 1) log.info("[RAG优化建议] 向量检索慢 → 检查Milvus索引类型(当前IVF_FLAT)，考虑换HNSW或减小topK(当前15)");
                                    if (maxIdx == 2) log.info("[RAG优化建议] Rerank慢 → 减少rerank候选数(当前{}条)或检查DashScope API响应时间", preRerankCount);
                                    if (maxIdx == 3) log.info("[RAG优化建议] LLM输出慢 → 检查模型(当前deepseek-v4-flash)响应速度或减小max_tokens");
                                    log.info("==============================================");

                                    // 记录 Micrometer 指标
                                    try {
                                        Timer.builder("rag.stage.duration").tags("stage", "rewrite").register(meterRegistry).record(java.time.Duration.ofMillis(rewriteMs));
                                        Timer.builder("rag.stage.duration").tags("stage", "search").register(meterRegistry).record(java.time.Duration.ofMillis(searchMs));
                                        Timer.builder("rag.stage.duration").tags("stage", "rerank").register(meterRegistry).record(java.time.Duration.ofMillis(rerankMs));
                                        Timer.builder("rag.stage.duration").tags("stage", "llm").register(meterRegistry).record(java.time.Duration.ofMillis(llmMs));
                                        meterRegistry.counter("rag.request.total", "status", "success").increment();
                                        meterRegistry.counter("llm.token.total", "service", "rag", "type", "input").increment(message.length() / 2);
                                        meterRegistry.counter("llm.token.total", "service", "rag", "type", "output").increment(fullContent.length() / 2);
                                    } catch (Exception ignored) {}

                                    // 记录 LLM 调用记录
                                    try {
                                        LlmCallRecord llmRecord = new LlmCallRecord();
                                        llmRecord.setTraceId(org.slf4j.MDC.get("traceId"));
                                        llmRecord.setUserId(currentId);
                                        llmRecord.setServiceName("rag");
                                        llmRecord.setModelName("deepseek-v4-flash");
                                        llmRecord.setInputTokens(message.length() / 2);
                                        llmRecord.setOutputTokens(fullContent.length() / 2);
                                        llmRecord.setDurationMs(totalMs);
                                        llmRecord.setStatus("success");
                                        llmRecord.setRequestPreview(message.length() > 200 ? message.substring(0, 200) : message);
                                        llmRecord.setResponsePreview(fullContent.length() > 200 ? fullContent.substring(0, 200) : fullContent.toString());
                                        llmRecord.setCreateTime(new java.util.Date());
                                        llmCallRecordService.save(llmRecord);
                                    } catch (Exception ignored) {}
                                }
                        );
            } catch (Exception e) {
                long tFail = System.currentTimeMillis();
                log.error("[RAG耗时分析] 请求在 {}ms 后失败", tFail - tRequestStart, e);
                emitter.completeWithError(e);
                // 记录失败的 LLM 调用
                try {
                    meterRegistry.counter("rag.request.total", "status", "fail").increment();
                    LlmCallRecord llmRecord = new LlmCallRecord();
                    llmRecord.setTraceId(org.slf4j.MDC.get("traceId"));
                    llmRecord.setUserId(currentId);
                    llmRecord.setServiceName("rag");
                    llmRecord.setModelName("deepseek-v4-flash");
                    llmRecord.setDurationMs(tFail - tRequestStart);
                    llmRecord.setStatus("fail");
                    llmRecord.setRequestPreview(message.length() > 200 ? message.substring(0, 200) : message);
                    llmRecord.setResponsePreview(e.getClass().getSimpleName() + ": " + e.getMessage());
                    llmRecord.setCreateTime(new java.util.Date());
                    llmCallRecordService.save(llmRecord);
                } catch (Exception ignored) {}
            }
        });

        return emitter;
    }
}