package com.junjie.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName(value = "llm_call_record")
@Data
public class LlmCallRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceId;

    private Long userId;

    private String serviceName;

    private String modelName;

    private Integer inputTokens;

    private Integer outputTokens;

    private Long durationMs;

    private String status;

    private String requestPreview;

    private String responsePreview;

    private Date createTime;
}
