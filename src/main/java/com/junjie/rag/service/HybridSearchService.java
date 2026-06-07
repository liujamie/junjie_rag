package com.junjie.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class HybridSearchService {

    private static final int RRF_K = 60;
    private static final int VECTOR_TOP_K = 15;
    private static final int BM25_TOP_K = 15;
    private static final int FINAL_TOP_K = 10;

    private final VectorStore vectorStore;
    private final Bm25IndexService bm25IndexService;

    public HybridSearchService(VectorStore vectorStore, Bm25IndexService bm25IndexService) {
        this.vectorStore = vectorStore;
        this.bm25IndexService = bm25IndexService;
    }

    /**
     * 混合检索：向量搜索 + BM25 关键词搜索 → RRF 融合排序
     */
    public List<Document> hybridSearch(String query, double similarityThreshold) {
        // 双路并发检索
        CompletableFuture<List<Document>> vectorFuture = CompletableFuture.supplyAsync(
                () -> {
                    SearchRequest req = SearchRequest.builder()
                            .query(query)
                            .similarityThreshold(similarityThreshold)
                            .topK(VECTOR_TOP_K)
                            .build();
                    return vectorStore.similaritySearch(req);
                });

        CompletableFuture<List<Document>> bm25Future = CompletableFuture.supplyAsync(
                () -> bm25IndexService.search(query, BM25_TOP_K));

        List<Document> vectorResults = vectorFuture.exceptionally(e -> {
            log.warn("向量搜索失败: {}", e.getMessage());
            return List.of();
        }).join();

        List<Document> bm25Results = bm25Future.exceptionally(e -> {
            log.warn("BM25搜索失败: {}", e.getMessage());
            return List.of();
        }).join();

        log.info("混合检索: 向量={}条, BM25={}条", vectorResults.size(), bm25Results.size());

        // RRF 融合
        Map<String, RrfItem> fused = new HashMap<>();

        // 向量结果分排名
        for (int i = 0; i < vectorResults.size(); i++) {
            Document doc = vectorResults.get(i);
            String id = doc.getId() != null ? doc.getId() : doc.getText().hashCode() + "";
            fused.put(id, new RrfItem(doc, (i + 1), 0));
        }

        // BM25 结果分配排名
        for (int i = 0; i < bm25Results.size(); i++) {
            Document doc = bm25Results.get(i);
            String id = doc.getId() != null ? doc.getId() : doc.getText().hashCode() + "";
            if (fused.containsKey(id)) {
                fused.get(id).bm25Rank = i + 1;
            } else {
                fused.put(id, new RrfItem(doc, Integer.MAX_VALUE, i + 1));
            }
        }

        // 计算 RRF 得分并排序
        List<RrfItem> ranked = new ArrayList<>(fused.values());
        for (RrfItem item : ranked) {
            double vectorScore = item.vectorRank != Integer.MAX_VALUE ? 1.0 / (RRF_K + item.vectorRank) : 0;
            double bm25Score = item.bm25Rank != Integer.MAX_VALUE ? 1.0 / (RRF_K + item.bm25Rank) : 0;
            item.rrfScore = vectorScore + bm25Score;
        }

        ranked.sort(Comparator.<RrfItem>comparingDouble(i -> i.rrfScore).reversed());

        // 取 topK
        List<Document> result = ranked.subList(0, Math.min(FINAL_TOP_K, ranked.size()))
                .stream()
                .map(i -> i.document)
                .toList();

        log.info("RRF融合后: {}条", result.size());
        return result;
    }

    private static class RrfItem {
        final Document document;
        int vectorRank;
        int bm25Rank;
        double rrfScore;

        RrfItem(Document doc, int vRank, int bRank) {
            this.document = doc;
            this.vectorRank = vRank;
            this.bm25Rank = bRank;
        }
    }
}
