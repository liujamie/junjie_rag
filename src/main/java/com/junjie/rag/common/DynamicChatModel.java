package com.junjie.rag.common;

import com.junjie.rag.entity.LlmModelConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 运行时动态切换 LLM 的 ChatModel 代理
 */
@Slf4j
@Primary
@Component
public class DynamicChatModel implements ChatModel {

    private volatile ChatModel delegate;
    private volatile Long currentModelId;
    private volatile String currentModelName;

    @Value("${spring.ai.openai.api-key:}")
    private String defaultApiKey;

    @PostConstruct
    public void init() {
        // 启动时先用 DeepSeek 作为默认模型，LlmModelConfigService 初始化后会切换到配置中的模型
        if (defaultApiKey != null && !defaultApiKey.isEmpty()) {
            OpenAiApi api = OpenAiApi.builder()
                    .baseUrl("https://api.deepseek.com")
                    .apiKey(defaultApiKey)
                    .build();
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model("deepseek-v4-flash")
                    .temperature(0.7)
                    .maxTokens(4096)
                    .build();
            this.delegate = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(options)
                    .build();
            log.info("DynamicChatModel 初始默认模型: deepseek-v4-flash");
        } else {
            log.warn("DynamicChatModel 无默认 API Key，等待模型初始化");
        }
    }

    public Long getCurrentModelId() {
        return currentModelId;
    }

    public String getCurrentModelName() {
        return currentModelName;
    }

    /** 切换到指定模型配置 */
    public void switchTo(LlmModelConfig config) {
        ChatModel newModel = createModel(config);
        this.delegate = newModel;
        this.currentModelId = config.getId();
        this.currentModelName = config.getModelName();
        log.info("LLM 已切换到: {} ({})", config.getName(), config.getModelName());
    }

    private ChatModel createModel(LlmModelConfig config) {
        return switch (config.getProvider()) {
            case "deepseek", "openai" -> createOpenAiModel(config);
            case "ollama" -> createOllamaModel(config);
            default -> throw new IllegalArgumentException("不支持的提供商: " + config.getProvider());
        };
    }

    private ChatModel createOpenAiModel(LlmModelConfig config) {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(config.getModelName())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .topP(config.getTopP())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
    }

    private ChatModel createOllamaModel(LlmModelConfig config) {
        org.springframework.ai.ollama.api.OllamaApi api =
                org.springframework.ai.ollama.api.OllamaApi.builder()
                        .baseUrl(config.getBaseUrl())
                        .build();
        org.springframework.ai.ollama.api.OllamaOptions options =
                org.springframework.ai.ollama.api.OllamaOptions.builder()
                        .model(config.getModelName())
                        .temperature(config.getTemperature())
                        .numPredict(config.getMaxTokens())
                        .topP(config.getTopP())
                        .build();
        return org.springframework.ai.ollama.OllamaChatModel.builder()
                .ollamaApi(api)
                .defaultOptions(options)
                .build();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return ensureDelegate().call(prompt);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return ensureDelegate().stream(prompt);
    }

    private ChatModel ensureDelegate() {
        if (delegate == null) {
            throw new IllegalStateException("DynamicChatModel 尚未初始化，没有可用模型");
        }
        return delegate;
    }
}
