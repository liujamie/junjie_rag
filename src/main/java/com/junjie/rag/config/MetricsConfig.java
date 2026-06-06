package com.junjie.rag.config;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class MetricsConfig {

    @Bean
    public MeterBinder ragMetrics() {
        return registry -> {
            Timer.builder("rag.stage.duration")
                    .description("RAG pipeline stage duration")
                    .tags("stage", "rewrite")
                    .publishPercentileHistogram()
                    .sla(
                            Duration.ofMillis(100),
                            Duration.ofMillis(500),
                            Duration.ofSeconds(1),
                            Duration.ofSeconds(3),
                            Duration.ofSeconds(5)
                    )
                    .register(registry);

            Timer.builder("rag.stage.duration")
                    .description("RAG pipeline stage duration")
                    .tags("stage", "search")
                    .publishPercentileHistogram()
                    .register(registry);

            Timer.builder("rag.stage.duration")
                    .description("RAG pipeline stage duration")
                    .tags("stage", "rerank")
                    .publishPercentileHistogram()
                    .register(registry);

            Timer.builder("rag.stage.duration")
                    .description("RAG pipeline stage duration")
                    .tags("stage", "llm")
                    .publishPercentileHistogram()
                    .register(registry);
        };
    }
}
