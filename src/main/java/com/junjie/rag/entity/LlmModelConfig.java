package com.junjie.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("llm_model_config")
public class LlmModelConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 显示名称 */
    private String name;

    /** 提供商: deepseek / openai / ollama */
    private String provider;

    /** API 地址 */
    private String baseUrl;

    /** 模型名 */
    private String modelName;

    /** API Key */
    private String apiKey;

    /** 温度 0-2 */
    private Double temperature;

    /** 最大输出 token */
    private Integer maxTokens;

    /** Top P 0-1 */
    private Double topP;

    /** 是否预置（预置不可删除） */
    private Boolean isDefault;

    /** 排序 */
    private Integer sortOrder;

    private Date createTime;

    private Date updateTime;
}
