package com.junjie.rag.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.junjie.rag.common.ApplicationConstant;
import com.junjie.rag.common.JwtTokenUserInterceptor;
import com.junjie.rag.entity.SensitiveWord;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author junjie
 * @date 2025/2/8
 */

@Configuration
public class ApplicationConfig implements WebMvcConfigurer {

    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    /**
     * ETL中的DocumentTransformer的实现，将文本数据源转换为多个分割段落
     * @return
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter(1000, 200, 2000, 100, true);
    }

    @Bean
    ChatClient chatclient(ChatClient.Builder builder){
        return builder.defaultSystem("你是一个乐于助人解决问题的AI机器人")
                .build();
    }

    @Bean("sensitiveWordCache")
    public Cache<String, List<SensitiveWord>> sensitiveWordCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(1)
                .build();
    }

    @Bean("ragResultCache")
    public Cache<String, String> ragResultCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();
    }

    /**
     * 注册拦截器
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册用户JWT拦截器
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/**") // 拦截所有请求
                .excludePathPatterns(ApplicationConstant.API_VERSION+"/user/login") // 排除用户登录接口拦截所有请求
                .excludePathPatterns(ApplicationConstant.API_VERSION+"/user/register") // 排除用户注册接口
                .excludePathPatterns("/doc.html", "/webjars/**", "/swagger-resources/**", "/v3/api-docs/**"); // 排除Swagger相关路径
    }
}