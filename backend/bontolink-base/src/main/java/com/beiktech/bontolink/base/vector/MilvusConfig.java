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

    @Bean
    public MilvusServiceClient milvusServiceClient() {
        log.info("Connecting to Milvus at {}:{}", host, port);
        MilvusServiceClient client = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(host)
                        .withPort(port)
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

        // 索引：幂等，已存在时 Milvus 会报 index already exists，直接忽略
        try {
            client.createIndex(
                    CreateIndexParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withFieldName("embedding")
                            .withIndexType(IndexType.IVF_FLAT)
                            .withMetricType(MetricType.IP)   // BGE 向量已 L2 归一化，IP == COSINE
                            .withExtraParam("{\"nlist\":128}")
                            .build()
            );
            log.info("Milvus index created on collection '{}'", collectionName);
        } catch (Exception e) {
            log.warn("createIndex on '{}' skipped: {}", collectionName, e.getMessage());
        }

        // 加载：索引就绪后 load，幂等
        try {
            client.loadCollection(
                    LoadCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            log.info("Milvus collection '{}' loaded", collectionName);
        } catch (Exception e) {
            log.warn("loadCollection '{}' skipped: {}", collectionName, e.getMessage());
        }
    }
}
