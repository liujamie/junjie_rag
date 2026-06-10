package com.junjie.rag.config;

import com.junjie.rag.entity.LlmModelConfig;
import com.junjie.rag.service.LlmModelConfigService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 启动时初始化预置 LLM 模型配置
 */
@Slf4j
@Component
public class DefaultModelsInitializer {

    @Autowired
    private LlmModelConfigService llmModelConfigService;

    @Value("${spring.ai.openai.api-key:}")
    private String deepseekApiKey;

    @PostConstruct
    public void init() {
        if (llmModelConfigService.count() > 0) {
            log.info("LLM 模型配置已存在，跳过初始化");
            return;
        }

        Date now = new Date();
        LlmModelConfig flash = create("DeepSeek V4 Flash", "deepseek",
                "https://api.deepseek.com", "deepseek-v4-flash",
                deepseekApiKey, 0.7, 4096, 0.9, true, 0, now);
        llmModelConfigService.save(flash);
        log.info("已初始化预置 LLM 模型: DeepSeek V4 Flash");
    }

    private LlmModelConfig create(String name, String provider, String baseUrl, String modelName,
                                   String apiKey, double temp, int maxTokens, double topP,
                                   boolean isDefault, int sortOrder, Date now) {
        LlmModelConfig c = new LlmModelConfig();
        c.setName(name);
        c.setProvider(provider);
        c.setBaseUrl(baseUrl);
        c.setModelName(modelName);
        c.setApiKey(apiKey);
        c.setTemperature(temp);
        c.setMaxTokens(maxTokens);
        c.setTopP(topP);
        c.setIsDefault(isDefault);
        c.setSortOrder(sortOrder);
        c.setCreateTime(now);
        c.setUpdateTime(now);
        return c;
    }
}
