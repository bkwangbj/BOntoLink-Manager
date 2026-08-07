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

    /**
     * 当前生效的 Milvus 客户端（volatile 单例，可被 {@link #rebuild()} 替换）。
     * 所有下游（搜索/同步/keepalive）统一通过 {@link #current()} 获取，确保连接重建后引用同步。
     */
    private volatile MilvusServiceClient client;

    @Bean
    public MilvusServiceClient milvusServiceClient() {
        return current();
    }

    /**
     * 线程安全地获取当前客户端，首次调用时惰性建立连接。
     */
    public MilvusServiceClient current() {
        MilvusServiceClient c = client;
        if (c == null) {
            synchronized (this) {
                if (client == null) {
                    client = createClient();
                }
                return client;
            }
        }
        return c;
    }

    /**
     * 连接异常后重建客户端：关闭旧连接、按原配置新建并重建集合/索引/加载。
     * 幂等，调用方只需在连接性错误时触发即可。
     */
    public synchronized MilvusServiceClient rebuild() {
        MilvusServiceClient old = client;
        client = null;
        if (old != null) {
            try { old.close(); } catch (Exception e) { log.warn("关闭旧 Milvus 连接失败: {}", e.getMessage()); }
        }
        log.warn("重建 Milvus 连接 {}:{}", host, port);
        MilvusServiceClient c = createClient();
        client = c;
        return c;
    }

    private MilvusServiceClient createClient() {
        log.info("Connecting to Milvus at {}:{}", host, port);
        MilvusServiceClient c = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(host)
                        .withPort(port)
                        // 空闲连接保活：客户端 keepAliveTime 必须小于服务端 keepAliveTimeout，
                        // 否则服务端会在空闲窗口内判定连接僵死并主动断开（http2 GOAWAY/RST）。
                        // 这里用 30s 探活 + 15s 响应判定（服务端默认 timeout=30s 时保持足够余量）。
                        // 移除了 withIdleTimeout(MAX_VALUE) 反模式——那会让 netty 连接池无法回收损坏连接。
                        .withKeepAliveTime(30, java.util.concurrent.TimeUnit.SECONDS)
                        .withKeepAliveTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .keepAliveWithoutCalls(true)
                        .build()
        );
        ensureCollection(c);
        return c;
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
