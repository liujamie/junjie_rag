package com.junjie.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName log_info
 */
@TableName(value ="log_info")
@Data
public class LogInfo {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 类目
     */
    private String className;

    /**
     * 请求时间戳
     */
    private Date requestTime;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 响应结果
     */
    private String response;

    /**
     * 执行耗时(毫秒)
     */
    private Long duration;

    /**
     * 状态: success / fail
     */
    private String status;

    /**
     * 异常信息
     */
    private String errorMessage;
}