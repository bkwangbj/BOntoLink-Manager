package com.beiktech.bontolink.base.vector;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.ConnectParam;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "bontolink.ontology.vector.type", havingValue = "milvus")
public class MilvusConfig {

    @Value("${bontolink.ontology.vector.milvus-host:127.0.0.1}")
    private String host;

    @Value("${bontolink.ontology.vector.milvus-port:19530}")
    private int port;

    @Value("${bontolink.ontology.vector.milvus-collection:ont_entity_embeddings}")
    private String collectionName;

    @Value("${bontolink.ontology.vector.dimension:768}")
    private int dimension;

    public String getCollectionName() {
        return collectionName;
    }

    @Bean
    public MilvusServiceClient milvusServiceClient() {
        log.info("Connecting to Milvus at {}:{}", host, port);
        MilvusServiceClient client = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(host)
                        .withPort(port)
                        // 空闲连接保活：服务端 keepAliveTime=10s / keepAliveTimeout=20s。
                        // 客户端 ping 间隔 45s（服务端 10s 的整数倍，避免 enforcement 拒绝），
                        // 超时 25s（必须 > 服务端 20s，否则慢请求时被误判连接失效）
                        .withKeepAliveTime(45, java.util.concurrent.TimeUnit.SECONDS)
                        .withKeepAliveTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
                        .keepAliveWithoutCalls(true)
                        // 不主动关闭空闲 channel（默认 30min 会关）
                        .withIdleTimeout(Long.MAX_VALUE, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .build()
        );
        ensureCollection(client);
        return client;
    }

    /** drop + recreate，供运维接口调用 */
    public void rebuildCollection(MilvusServiceClient client) {
        try {
            Boolean exists = client.hasCollection(
                    HasCollectionParam.newBuilder().withCollectionName(collectionName).build()
            ).getData();
            if (Boolean.TRUE.equals(exists)) {
                client.dropCollection(
                        DropCollectionParam.newBuilder().withCollectionName(collectionName).build()
                );
                log.info("Milvus collection '{}' dropped", collectionName);
            }
        } catch (Exception e) {
            log.warn("Drop collection '{}' failed: {}", collectionName, e.getMessage());
        }
        ensureCollection(client);
    }

    private void ensureCollection(MilvusServiceClient client) {
        try {
            Boolean exists = client.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            ).getData();

            if (!Boolean.TRUE.equals(exists)) {
                log.info("Creating Milvus collection '{}'", collectionName);
                List<FieldType> fields = List.of(
                        FieldType.newBuilder()
                                .withName("pk")
                                .withDataType(DataType.VarChar)
                                .withMaxLength(256)
                                .withPrimaryKey(true)
                                .withAutoID(false)
                                .build(),
                        FieldType.newBuilder()
                                .withName("entity_type")
                                .withDataType(DataType.VarChar)
                                .withMaxLength(32)
                                .build(),
                        FieldType.newBuilder()
                                .withName("entity_id")
                                .withDataType(DataType.VarChar)
                                .withMaxLength(128)
                                .build(),
                        FieldType.newBuilder()
                                .withName("parent_id")
                                .withDataType(DataType.VarChar)
                                .withMaxLength(128)
                                .build(),
                        FieldType.newBuilder()
                                .withName("ns_code")
                                .withDataType(DataType.VarChar)
                                .withMaxLength(64)
                                .build(),
                        FieldType.newBuilder()
                                .withName("source_text")
                                .withDataType(DataType.VarChar)
                                .withMaxLength(2048)
                                .build(),
                        FieldType.newBuilder()
                                .withName("api_name")
                                .withDataType(DataType.VarChar)
                                .withMaxLength(256)
                                .build(),
                        FieldType.newBuilder()
                                .withName("embedding")
                                .withDataType(DataType.FloatVector)
                                .withDimension(dimension)
                                .build()
                );
                client.createCollection(
                        CreateCollectionParam.newBuilder()
                                .withCollectionName(collectionName)
                                .withFieldTypes(fields)
                                .build()
                );
                log.info("Milvus collection '{}' created", collectionName);
            } else {
                log.info("Milvus collection '{}' already exists, ensuring index and load", collectionName);
            }
        } catch (Exception e) {
            log.warn("Failed to create Milvus collection '{}': {}", collectionName, e.getMessage());
            return;
        }

        // 索引：幂等，已存在时 Milvus 会报 index already exists，直接忽略。
        // collection 刚创建后立即 createIndex 可能因未就绪而失败（如 "Failed to describe collection"），
        // 这类非幂等错误需要等待后重试，否则索引缺失导致后续无法 loadCollection。
        int maxRetries = 5;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                io.milvus.param.R<io.milvus.param.RpcStatus> resp = client.createIndex(
                        CreateIndexParam.newBuilder()
                                .withCollectionName(collectionName)
                                .withFieldName("embedding")
                                .withIndexType(IndexType.IVF_FLAT)
                                .withMetricType(MetricType.IP)   // BGE 向量已 L2 归一化，IP == COSINE
                                .withExtraParam("{\"nlist\":128}")
                                .build()
                );
                if (resp.getStatus() == 0 || "index already exists".equalsIgnoreCase(resp.getMessage())) {
                    log.info("Milvus index created on collection '{}'", collectionName);
                    break;  // 成功则退出重试循环
                }
                // createIndex 返回非零状态，重试
                if (attempt < maxRetries) {
                    log.warn("createIndex on '{}' failed (attempt {}/{}): {}, retrying...",
                            collectionName, attempt, maxRetries, resp.getMessage());
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    log.warn("createIndex on '{}' failed after {} attempts: {}", collectionName, maxRetries, resp.getMessage());
                }
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (attempt < maxRetries) {
                    log.warn("createIndex on '{}' threw (attempt {}/{}): {}, retrying...",
                            collectionName, attempt, maxRetries, e.getMessage());
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    log.warn("createIndex on '{}' skipped after {} attempts: {}", collectionName, maxRetries, e.getMessage());
                }
            }
        }

        // 加载：索引就绪后 load，幂等，syncLoad 等待真正加载完成
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                io.milvus.param.R<io.milvus.param.RpcStatus> resp = client.loadCollection(
                        LoadCollectionParam.newBuilder()
                                .withCollectionName(collectionName)
                                .withSyncLoad(true)
                                .build()
                );
                if (resp.getStatus() == 0) {
                    log.info("Milvus collection '{}' loaded", collectionName);
                    break;  // 成功则退出重试循环
                }
                // load 返回非零状态（如 index doesn't exist），重试
                if (attempt < maxRetries) {
                    log.warn("loadCollection '{}' failed (attempt {}/{}): {}, retrying...",
                            collectionName, attempt, maxRetries, resp.getMessage());
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    log.warn("loadCollection '{}' failed after {} attempts: {}", collectionName, maxRetries, resp.getMessage());
                }
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                String errType = e.getClass().getSimpleName().toLowerCase();
                boolean isConnectionError = msg.contains("internal")
                        || msg.contains("end-of-stream")
                        || msg.contains("unavailable")
                        || msg.contains("keepalive")
                        || errType.contains("statusruntimeexception");

                if (isConnectionError && attempt < maxRetries) {
                    log.warn("loadCollection '{}' failed (attempt {}/{}): {}, retrying...",
                            collectionName, attempt, maxRetries, e.getMessage());
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    log.warn("loadCollection '{}' skipped: {}", collectionName, e.getMessage());
                    break;
                }
            }
        }
    }
}
