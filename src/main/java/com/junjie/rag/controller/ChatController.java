package com.junjie.rag.controller;

import com.junjie.rag.annotation.Loggable;
import com.junjie.rag.common.SensitiveWordHolder;
import com.junjie.rag.common.ApplicationConstant;
import com.junjie.rag.context.BaseContext;
import com.junjie.rag.entity.LlmCallRecord;
import com.junjie.rag.service.LlmCallRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;


/**
 * @Title: ChatController
 * @Author junjie
 * @Package com.junjie.rag.controller
 * @description: 对话接口
 */

@Tag(name="AiRagController",description = "chat对话接口")
@Slf4j
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/chat")
public class ChatController {

    @Autowired
    private  ChatClient chatClient;

    @Autowired
    @Qualifier("aiStreamExecutor")
    private ExecutorService aiStreamExecutor;

    @Autowired
    private LlmCallRecordService llmCallRecordService;

    @Autowired
    private SensitiveWordHolder sensitiveWordHolder;

    public ChatController(ChatClient.Builder builder,ChatMemory chatMemory) {

        this.chatClient = builder
                .defaultSystem("""
                        你是一家名为“XS公司”的知识库系统的客户客服代理。请友好乐于助人，充满喜悦地回复。
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build() // CHAT MEMORY

                        )
                .build();
    }

    @Operation(summary = "stream",description = "流式对话接口")
    @GetMapping(value = "/stream",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Loggable("message")
    public SseEmitter streamRagChat(@RequestParam(value = "message", defaultValue = "你好" ) String message,
                                      @RequestParam(value = "prompt", defaultValue = "你是一名AI助手，致力于帮助人们解决问题.") String prompt){
        // AC 自动机敏感词过滤（O(消息长度)）
        List<String> matched = sensitiveWordHolder.get().matches(message);
        if (!matched.isEmpty()) {
            SseEmitter emitter = new SseEmitter();
            try { emitter.send("包含敏感词:" + String.join(",", matched)); } catch (IOException ignored) {}
            emitter.complete();
            return emitter;
        }

        SseEmitter emitter = new SseEmitter(300000L);
        Long userId = BaseContext.getCurrentId();
        long tStart = System.currentTimeMillis();
        StringBuilder fullContent = new StringBuilder();

        aiStreamExecutor.execute(() -> {
            try {
                chatClient.prompt()
                        .system(prompt)
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                        .user(message)
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
                                    emitter.complete();
                                    // 记录 LLM 调用
                                    long duration = System.currentTimeMillis() - tStart;
                                    try {
                                        LlmCallRecord record = new LlmCallRecord();
                                        record.setTraceId(org.slf4j.MDC.get("traceId"));
                                        record.setUserId(userId);
                                        record.setServiceName("chat");
                                        record.setModelName("deepseek-v4-flash");
                                        record.setInputTokens(message.length() / 2);
                                        record.setOutputTokens(fullContent.length() / 2);
                                        record.setDurationMs(duration);
                                        record.setStatus("success");
                                        record.setRequestPreview(message.length() > 200 ? message.substring(0, 200) : message);
                                        record.setResponsePreview(fullContent.length() > 200 ? fullContent.substring(0, 200) : fullContent.toString());
                                        record.setCreateTime(new java.util.Date());
                                        llmCallRecordService.save(record);
                                    } catch (Exception ignored) {}
                                }
                        );
            } catch (Exception e) {
                emitter.completeWithError(e);
                // 记录失败的调用
                try {
                    LlmCallRecord record = new LlmCallRecord();
                    record.setTraceId(org.slf4j.MDC.get("traceId"));
                    record.setUserId(userId);
                    record.setServiceName("chat");
                    record.setModelName("deepseek-v4-flash");
                    record.setDurationMs(System.currentTimeMillis() - tStart);
                    record.setStatus("fail");
                    record.setRequestPreview(message.length() > 200 ? message.substring(0, 200) : message);
                    record.setResponsePreview(e.getClass().getSimpleName() + ": " + e.getMessage());
                    record.setCreateTime(new java.util.Date());
                    llmCallRecordService.save(record);
                } catch (Exception ignored) {}
            }
        });

        return emitter;
    }
}
