package com.junjie.rag.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.junjie.rag.common.DynamicChatModel;
import com.junjie.rag.entity.LlmModelConfig;
import com.junjie.rag.mapper.LlmModelConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class LlmModelConfigService extends ServiceImpl<LlmModelConfigMapper, LlmModelConfig> {

    @Autowired
    private DynamicChatModel dynamicChatModel;

    /** 启动时加载默认模型，切换到第一个可用模型 */
    @PostConstruct
    public void initDefaultModel() {
        List<LlmModelConfig> list = lambdaQuery().orderByAsc(LlmModelConfig::getSortOrder).list();
        if (!list.isEmpty()) {
            dynamicChatModel.switchTo(list.get(0));
            log.info("初始 LLM 模型: {} ({})", list.get(0).getName(), list.get(0).getModelName());
        } else {
            log.warn("无可用 LLM 模型配置");
        }
    }

    public Long getCurrentModelId() {
        return dynamicChatModel.getCurrentModelId();
    }

    /** 切换模型 */
    public boolean switchTo(Long id) {
        LlmModelConfig config = getById(id);
        if (config == null) return false;
        dynamicChatModel.switchTo(config);
        return true;
    }

    /** 切换模型按名称 */
    public boolean switchTo(String name) {
        LlmModelConfig config = lambdaQuery().eq(LlmModelConfig::getName, name).one();
        if (config == null) return false;
        dynamicChatModel.switchTo(config);
        return true;
    }
}
