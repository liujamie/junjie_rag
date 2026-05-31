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

import com.junjie.rag.annotation.Loggable;
import com.junjie.rag.common.ApplicationConstant;
import com.junjie.rag.context.BaseContext;
import com.junjie.rag.entity.SensitiveWord;
import com.junjie.rag.service.SensitiveWordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "AiRagController", description = "Rag接口")
@Slf4j
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/ai")
public class AiRagController {

    ChatClient chatClient;
    VectorStore vectorStore;

    @Autowired
    private SensitiveWordService sensitiveWordService;

//    @Autowired
//    private RagTool ragTool;

    public AiRagController(ChatModel chatModel, ChatMemory chatMemory, VectorStore vectorStore) {
        this.chatClient = ChatClient.builder(chatModel)
                // 隐式提示语
                .defaultSystem("""
                你是一家名为“LIU-RAG-AI”的知识库系统的对话助手。请友好乐于助人，充满喜悦地回复。
                当用户的查询包含特定的人名刘梦杰时，调用工具addInfo
                """)
                .defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(chatMemory).build(),
                        SimpleLoggerAdvisor.builder().build()
                        )
                // 设置默认的tool
//                .defaultTools(ragTool)
                .build();

        this.vectorStore = vectorStore;
    }

    @Operation(summary = "rag", description = "Rag对话接口")
    @GetMapping(value = "/rag")
    @Loggable
    public Flux<String> generate(@RequestParam(value = "message", defaultValue = "你好") String message) throws IOException {
        // todo 针对大模型信息滞后的问题，可以通过网络检索来补充信息

        List<SensitiveWord> list = sensitiveWordService.list();

        for(SensitiveWord sensitiveWord: list){
            if (message.contains(sensitiveWord.getWord())){
                return Flux.just("包含敏感词:" + sensitiveWord.getWord());
            }
        }
//
//        Boolean isSql = chatClient.prompt()
//                .system("用户的查询是否涉及统计数据、求和、计数、平均值等聚合操作？")
//                .user(message)
//                .call()
//                .entity(Boolean.class);
//
//        if (isSql) {
//
//            return ;
//
//        }

        Long currentId = BaseContext.getCurrentId();
        Flux<String> content = chatClient.prompt()
                // 显式提示语
                .user(message)
                .advisors(a -> a.param("current_Date", LocalDate.now().toString()).param(ChatMemory.CONVERSATION_ID, currentId))
                .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder()
                                .query(message)
                                .similarityThreshold(0.1d)
                                .topK(3)
//                                .filterExpression()
                                .build())
                        .build())
//                .tools(ragTool)
                .stream()
                .content();

        return  content;
    }
}