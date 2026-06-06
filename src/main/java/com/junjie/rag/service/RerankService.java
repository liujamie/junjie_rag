package com.junjie.rag.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.junjie.rag.annotation.TrackLlmCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RerankService {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String RERANK_URL = "https://dashscope.aliyuncs.com/compatible-api/v1/reranks";

    @Value("${dashscope.api-key:}")
    private String dashscopeApiKey;

    /**
     * 对检索结果进行重排序，按相关性降序排列
     *
     * @param query     用户原始问题
     * @param documents 待重排序的文档列表
     * @return 按相关性分数降序排列的文档列表
     */
    @TrackLlmCall(service = "rerank", model = "qwen3-rerank")
    public List<Document> rerank(String query, List<Document> documents) {
        if (documents == null || documents.size() <= 1) {
            return documents;
        }

        if (dashscopeApiKey == null || dashscopeApiKey.isEmpty()) {
            log.warn("DashScope API Key 未配置，跳过 Rerank");
            return documents;
        }

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "qwen3-rerank");
            requestBody.put("query", query);
            requestBody.put("top_n", documents.size());

            JSONArray docArray = new JSONArray();
            docArray.addAll(documents.stream().map(Document::getText).toList());
            requestBody.put("documents", docArray);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + dashscopeApiKey);
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(RERANK_URL, entity, String.class);

            JSONObject body = JSON.parseObject(response.getBody());

            JSONArray results = body.getJSONArray("results");

            if (results == null || results.isEmpty()) {
                log.warn("Rerank 返回空结果，使用原始排序");
                return documents;
            }

            List<Document> reranked = results.stream()
                    .map(r -> (JSONObject) r)
                    .sorted(Comparator.comparingDouble(
                            r -> -r.getDoubleValue("relevance_score")))
                    .map(r -> documents.get(r.getIntValue("index")))
                    .collect(Collectors.toList());

            log.info("Rerank 完成: {}→{} 条, 最高分: {}",
                    documents.size(), reranked.size(),
                    String.format("%.4f", results.getJSONObject(0).getDoubleValue("relevance_score")));

            return reranked;

        } catch (Exception e) {
            log.error("Rerank 调用失败，降级为原始排序", e);
            return documents;
        }
    }
}
