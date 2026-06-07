package com.junjie.rag.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.junjie.rag.common.SensitiveWordHolder;
import com.junjie.rag.entity.SensitiveWord;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 监听敏感词变化，重建 AC 自动机
 */
@Slf4j
@Configuration
public class SensitiveWordConfig {

    @Autowired
    @Qualifier("sensitiveWordCache")
    private Cache<String, List<SensitiveWord>> sensitiveWordCache;

    @Autowired
    private com.junjie.rag.service.SensitiveWordService sensitiveWordService;

    @Autowired
    private SensitiveWordHolder sensitiveWordHolder;

    @PostConstruct
    public void init() {
        rebuild();
    }

    /** 从数据库加载敏感词，重建 AC 自动机 */
    public void rebuild() {
        try {
            List<SensitiveWord> words = sensitiveWordCache.get("all", k -> sensitiveWordService.list());
            if (words == null || words.isEmpty()) {
                sensitiveWordHolder.rebuild(List.of());
                log.info("AC 自动机已重建（无敏感词）");
                return;
            }
            List<String> wordList = words.stream()
                    .map(SensitiveWord::getWord)
                    .filter(w -> w != null && !w.isEmpty())
                    .collect(Collectors.toList());
            sensitiveWordHolder.rebuild(wordList);
            log.info("AC 自动机已重建，加载 {} 个敏感词", wordList.size());
        } catch (Exception e) {
            log.error("AC 自动机重建失败", e);
        }
    }
}
