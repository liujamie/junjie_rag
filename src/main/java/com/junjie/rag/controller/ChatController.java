package com.junjie.rag.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.junjie.rag.annotation.Loggable;
import com.junjie.rag.common.ApplicationConstant;
import com.junjie.rag.context.BaseContext;
import com.junjie.rag.entity.SensitiveWord;
import com.junjie.rag.service.SensitiveWordService;
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
    private SensitiveWordService sensitiveWordService;

    @Autowired
    @Qualifier("aiStreamExecutor")
    private ExecutorService aiStreamExecutor;

    @Autowired
    @Qualifier("sensitiveWordCache")
    private Cache<String, List<SensitiveWord>> sensitiveWordCache;

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
        List<SensitiveWord> list = sensitiveWordCache.get("all", k -> sensitiveWordService.list());

        for (SensitiveWord sensitiveWord : list) {
            if (message.contains(sensitiveWord.getWord())) {
                SseEmitter emitter = new SseEmitter();
                try {
                    emitter.send("包含敏感词:" + sensitiveWord.getWord());
                } catch (IOException ignored) {
                }
                emitter.complete();
                return emitter;
            }
        }

        SseEmitter emitter = new SseEmitter(300000L);
        Long userId = BaseContext.getCurrentId();

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
                                        emitter.send(chunk);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                },
                                emitter::completeWithError,
                                emitter::complete
                        );
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
