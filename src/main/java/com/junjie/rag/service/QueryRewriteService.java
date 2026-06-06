package com.junjie.rag.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.junjie.rag.annotation.TrackLlmCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class QueryRewriteService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey;

    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    public QueryRewriteService(@Value("${spring.ai.openai.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 根据对话历史，将当前问题改写为独立的检索查询
     *
     * @param question 当前用户问题
     * @param history  最近的对话历史文本（用户/AI消息）
     * @return 改写后的独立查询，失败时返回原问题
     */
    @TrackLlmCall(service = "rewrite", model = "deepseek-v4-flash")
    public String rewrite(String question, String history) {
        if (history == null || history.isBlank()) {
            return question;
        }

        try {
            long start = System.currentTimeMillis();

            JSONObject body = new JSONObject();
            body.put("model", "deepseek-v4-flash");
            body.put("max_tokens", 80);
            body.put("temperature", 0);
            body.put("stream", false);

            JSONArray messages = new JSONArray();
            JSONObject userMsg = new JSONObject();
            userMsg.put("content", "改写为搜索查询。\n历史问题：" + history + "\n当前问题：" + question + "\n改写结果：");
            userMsg.put("role", "user");
            messages.add(userMsg);
            body.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    API_URL, new HttpEntity<>(body.toJSONString(), headers), String.class);

            JSONObject resultBody = JSONObject.parseObject(response.getBody());
            String rewritten = resultBody.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            long cost = System.currentTimeMillis() - start;
            String result = (rewritten != null && !rewritten.isBlank()) ? rewritten.trim() : question;
            log.info("查询改写: '{}' → '{}' ({}ms)", question, result, cost);
            return result;

        } catch (Exception e) {
            log.warn("查询改写失败，使用原问题: {}ms", e.getMessage());
            return question;
        }
    }
}
