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
import com.junjie.rag.common.ApplicationConstant;
import com.junjie.rag.context.BaseContext;
import com.junjie.rag.entity.SensitiveWord;
import com.junjie.rag.service.RerankService;
import com.junjie.rag.service.SensitiveWordService;
import com.junjie.rag.tools.RagTool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
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
    private SensitiveWordService sensitiveWordService;

    @Autowired
    @Qualifier("aiStreamExecutor")
    private ExecutorService aiStreamExecutor;

    @Autowired
    @Qualifier("sensitiveWordCache")
    private Cache<String, List<SensitiveWord>> sensitiveWordCache;

    @Autowired
    @Qualifier("ragResultCache")
    private Cache<String, String> ragResultCache;

    @Autowired
    private RerankService rerankService;

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
                3. addInfo(question) - 用户查询刘梦杰的个人资料时调用

                当用户问题涉及实时信息、新闻、非知识库内容时，优先使用 searchWeb 工具。
                """)
                .defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(chatMemory).build(),
                        SimpleLoggerAdvisor.builder().build()
                        )
                .defaultTools(ragTool)
                .build();

        this.vectorStore = vectorStore;
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
                3. addInfo(question) - 用户查询刘梦杰的个人资料时调用

                当用户问题涉及实时信息、新闻、非知识库内容时，优先使用 searchWeb 工具。
                """.formatted(documentsText.isEmpty() ? "（无相关文档）" : documentsText);
    }

    @Operation(summary = "rag", description = "Rag对话接口")
    @GetMapping(value = "/rag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Loggable
    public SseEmitter generate(@RequestParam(value = "message", defaultValue = "你好") String message) throws IOException {

        List<SensitiveWord> list = sensitiveWordCache.get("all", k -> sensitiveWordService.list());

        for(SensitiveWord sensitiveWord: list){
            if (message.contains(sensitiveWord.getWord())){
                SseEmitter emitter = new SseEmitter();
                try { emitter.send("包含敏感词:" + sensitiveWord.getWord()); } catch (IOException ignored) {}
                emitter.complete();
                return emitter;
            }
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
            });
            return cachedEmitter;
        }

        SseEmitter emitter = new SseEmitter(300000L);
        Long currentId = BaseContext.getCurrentId();
        StringBuilder fullContent = new StringBuilder();

        aiStreamExecutor.execute(() -> {
            try {
                // 1. 向量检索（召回更多候选）
                SearchRequest searchRequest = SearchRequest.builder()
                        .query(message)
                        .similarityThreshold(0.5d)
                        .topK(15)
                        .build();
                List<Document> docs = vectorStore.similaritySearch(searchRequest);

                // 2. Rerank 重排序
                if (docs.size() > 1) {
                    docs = rerankService.rerank(message, docs);
                    docs = docs.subList(0, Math.min(3, docs.size()));
                }

                String documentsText = docs.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n\n---\n\n"));

                // 3. 构建系统提示词（含重排序后的知识库内容）
                String systemPrompt = buildRagPrompt(documentsText);

                chatClient.prompt()
                        .user(message)
                        .system(systemPrompt)
                        .advisors(a -> a.param("current_Date", LocalDate.now().toString()).param(ChatMemory.CONVERSATION_ID, currentId))
                        .stream()
                        .content()
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
                                    ragResultCache.put(message, fullContent.toString());
                                    emitter.complete();
                                }
                        );
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}