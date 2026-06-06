package com.junjie.rag.tools;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.junjie.rag.annotation.TrackLlmCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class RagTool {

    @Value("${spring.ai.websearch.searxng.url:http://localhost:8080/search}")
    private String searxngUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Tool(description = "获取当前日期和时间")
    public String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Tool(description = "获取今天的日期")
    public String getCurrentDate() {
        return LocalDate.now().toString();
    }

    @TrackLlmCall(service = "searchWeb", model = "searxng")
    @Tool(description = "搜索网络获取实时信息，当需要最新新闻、实时数据、知识库未覆盖的内容时使用")
    public String searchWeb(@ToolParam(description = "搜索关键词", required = true) String query) {
        log.info("searchWeb 被调用, query: {}", query);
        try {
            String encoded = java.net.URLEncoder.encode(query, "UTF-8");
            String url = searxngUrl + "?format=json&q=" + encoded;
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.set("Accept", "application/json");
            headers.set("X-Forwarded-For", "127.0.0.1");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            String response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
            log.info("SearXNG 返回长度: {}", response != null ? response.length() : 0);

            if (response == null || response.isBlank()) return "未搜索到结果";

            JSONObject json = JSON.parseObject(response);
            JSONArray results = json.getJSONArray("results");
            if (results == null || results.isEmpty()) return "未搜索到相关结果";

            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (int i = 0; i < results.size() && count < 10; i++) {
                JSONObject r = results.getJSONObject(i);
                String title = r.getString("title");
                String content = r.getString("content");
                String source = r.getString("url");
                if (title == null || title.isBlank()) continue;
                sb.append(++count).append(". ").append(title).append("\n");
                if (content != null && !content.isBlank()) sb.append("   ").append(content).append("\n");
                if (source != null && !source.isBlank()) sb.append("   来源: ").append(source).append("\n");
                sb.append("\n");
                if (sb.length() > 4000) break;
            }
            return sb.length() > 0 ? sb.toString() : "未搜索到相关结果";
        } catch (Exception e) {
            log.error("searchWeb 异常", e);
            return "网络搜索暂时不可用: " + e.getMessage();
        }
    }
}
