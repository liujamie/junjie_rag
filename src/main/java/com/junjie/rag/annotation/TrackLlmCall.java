package com.junjie.rag.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要追踪的 LLM 调用方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackLlmCall {

    /** 服务名称: rewrite / rerank / searchWeb / chat / rag */
    String service() default "";

    /** 模型名称（可使用占位符，运行时动态替换） */
    String model() default "";
}
