package com.junjie.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QueryRewriteService {

    private final ChatClient rewriteClient;

    public QueryRewriteService(ChatModel chatModel) {
        this.rewriteClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个查询改写助手。用户正在进行多轮对话，当前问题可能依赖对话历史。
                        请把当前问题改写成一个独立的、完整的检索查询，补全代词和上下文，不依赖对话历史也能搜到相关内容。
                        只返回改写后的查询文本，不要解释，不要加引号。
                        """)
                .build();
    }

    /**
     * 根据对话历史，将当前问题改写为独立的检索查询
     *
     * @param question 当前用户问题
     * @param history  最近的对话历史文本（用户/AI消息）
     * @return 改写后的独立查询，失败时返回原问题
     */
    public String rewrite(String question, String history) {
        if (history == null || history.isBlank()) {
            return question;
        }

        try {
            String rewritten = rewriteClient.prompt()
                    .user(u -> u.text("""
                            历史对话：
                            {history}

                            当前问题：
                            {question}

                            改写结果：""")
                            .param("history", history)
                            .param("question", question))
                    .call()
                    .content();

            String result = rewritten != null ? rewritten.trim() : question;
            log.info("查询改写: '{}' → '{}'", question, result);
            return result;

        } catch (Exception e) {
            log.warn("查询改写失败，使用原问题: {}", e.getMessage());
            return question;
        }
    }
}
