package com.junjie.rag.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DescribeIndexResponse;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.index.CreateIndexParam;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 确保 Milvus 集合和索引存在。
 * 集合创建由 Spring AI (initializeSchema=true) 负责，
 * 此类负责兜底创建索引/恢复索引。
 */
@Slf4j
@Configuration
public class MilvusSchemaConfig {

    @Value("${spring.ai.vectorstore.milvus.client.host:localhost}")
    private String milvusHost;

    @Value("${spring.ai.vectorstore.milvus.client.port:19530}")
    private int milvusPort;

    @Value("${spring.ai.vectorstore.milvus.collectionName:vector_store}")
    private String collectionName;

    @PostConstruct
    public void ensureIndex() {
        try {
            MilvusServiceClient client = new MilvusServiceClient(
                    ConnectParam.newBuilder()
                            .withHost(milvusHost)
                            .withPort(milvusPort)
                            .build()
            );

            boolean exists = client.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            ).getData();

            if (!exists) {
                log.info("集合 '{}' 不存在，等待 Spring AI 创建", collectionName);
                return;
            }

            // 检查索引，不存在则创建
            try {
                io.milvus.param.R<DescribeIndexResponse> resp = client.describeIndex(
                        io.milvus.param.index.DescribeIndexParam.newBuilder()
                                .withCollectionName(collectionName)
                                .build()
                );
                if (resp.getStatus() == 0) {
                    log.info("索引已存在，跳过创建");
                } else {
                    throw new RuntimeException(resp.getMessage());
                }
            } catch (Exception e) {
                log.info("索引不存在，正在创建 IVF_FLAT 索引...");
                client.createIndex(
                        CreateIndexParam.newBuilder()
                                .withCollectionName(collectionName)
                                .withFieldName("embedding")
                                .withIndexType(IndexType.IVF_FLAT)
                                .withMetricType(MetricType.COSINE)
                                .withExtraParam("{\"nlist\":1024}")
                                .withSyncMode(Boolean.FALSE)
                                .build()
                );
                log.info("索引创建完成");
            }

            client.loadCollection(
                    LoadCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            log.info("Milvus 集合 '{}' 就绪", collectionName);
            client.close();
        } catch (Exception e) {
            log.error("Milvus 初始化失败（不影响启动，稍后重试）: {}", e.getMessage());
        }
    }
}
