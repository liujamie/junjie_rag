package com.junjie.rag.service;

import com.junjie.rag.annotation.TrackLlmCall;
import com.junjie.rag.common.DynamicChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QueryRewriteService {

    private final ChatClient chatClient;
    private final DynamicChatModel dynamicChatModel;

    public QueryRewriteService(DynamicChatModel dynamicChatModel) {
        this.dynamicChatModel = dynamicChatModel;
        this.chatClient = ChatClient.builder(dynamicChatModel).build();
    }

    /**
     * 根据对话历史，将当前问题改写为独立的检索查询
     *
     * @param question 当前用户问题
     * @param history  最近的对话历史文本（用户/AI消息）
     * @return 改写后的独立查询，失败时返回原问题
     */
    @TrackLlmCall(service = "rewrite", model = "")
    public String rewrite(String question, String history) {
        if (history == null || history.isBlank()) {
            return question;
        }

        try {
            long start = System.currentTimeMillis();

            String rewritten = chatClient.prompt()
                    .user(u -> u.text("改写为搜索查询。\n历史问题：{history}\n当前问题：{question}\n改写结果：")
                            .param("history", history)
                            .param("question", question))
                    .options(OpenAiChatOptions.builder()
                            .temperature(0.0)
                            .maxTokens(80)
                            .build())
                    .call()
                    .content();

            long cost = System.currentTimeMillis() - start;
            String result = (rewritten != null && !rewritten.isBlank()) ? rewritten.trim() : question;
            log.info("查询改写: '{}' → '{}' ({}ms, 模型:{})", question, result, cost, dynamicChatModel.getCurrentModelName());
            return result;

        } catch (Exception e) {
            log.warn("查询改写失败，使用原问题: {}", e.getMessage());
            return question;
        }
    }
}
