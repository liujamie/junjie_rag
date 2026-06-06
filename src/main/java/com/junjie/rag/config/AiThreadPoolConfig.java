package com.junjie.rag.config;

import com.junjie.rag.common.MdcThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class AiThreadPoolConfig {

    @Bean("aiStreamExecutor")
    public ExecutorService aiStreamExecutor() {
        int core = Runtime.getRuntime().availableProcessors();
        return new MdcThreadPoolExecutor(
                core,
                core * 2,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
