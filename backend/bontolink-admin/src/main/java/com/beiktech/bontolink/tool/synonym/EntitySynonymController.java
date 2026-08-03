package com.beiktech.bontolink.tool.synonym;

import com.beiktech.bontolink.base.embedding.EmbeddingService;
import com.beiktech.bontolink.base.vector.MilvusConfig;
import com.beiktech.bontolink.common.R;
import io.milvus.client.MilvusServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/tool/synonym")
public class EntitySynonymController {

    @Autowired
    private EntitySynonymService synonymService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired(required = false)
    private MilvusServiceClient milvusClient;

    @Autowired(required = false)
    private MilvusConfig milvusConfig;

    /** 单条生成：{"entityType":"class","entityId":"class-xxx"} */
    @PostMapping("/generate")
    public R<Map<String, Object>> generate(@RequestBody Map<String, Object> body) {
        String entityType = (String) body.get("entityType");
        String entityId = (String) body.get("entityId");
        if (entityType == null || entityId == null) {
            return R.error(400, "entityType 和 entityId 不能为空");
        }
        Map<String, Object> result = synonymService.generate(entityType, entityId);
        Boolean success = (Boolean) result.get("success");
        if (Boolean.FALSE.equals(success)) {
            return R.error(400, (String) result.get("message"));
        }
        return R.ok(result);
    }

    /** 批量生成：{"entityType":"biz_category","limit":0} limit<=0 表示全量，异步执行立即返回 */
    @PostMapping("/batch-generate")
    public R<Map<String, Object>> batchGenerate(@RequestBody Map<String, Object> body) {
        String entityType = (String) body.get("entityType");
        if (entityType == null) {
            return R.error(400, "entityType 不能为空");
        }
        int limit = body.get("limit") instanceof Number n ? n.intValue() : 0;
        // 异步执行，避免长时间阻塞 HTTP 线程（JRebel 热加载时会关闭连接池导致失败）
        CompletableFuture.runAsync(() -> synonymService.batchGenerate(entityType, limit));
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("entityType", entityType);
        resp.put("message", "批量生成已在后台启动，请查看服务日志");
        return R.ok(resp);
    }

    /** 配置状态 + 批量进度 */
    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("embeddingProvider", embeddingService.getClass().getSimpleName());
        s.put("dimension", embeddingService.getDimension());
        s.put("milvusAvailable", milvusClient != null);
        s.put("batch", synonymService.getBatchProgress());
        return R.ok(s);
    }

    /** 重建 Milvus 向量集合（drop + recreate + index + load） */
    @PostMapping("/milvus/rebuild")
    public R<Map<String, Object>> rebuildMilvus() {
        if (milvusClient == null || milvusConfig == null) {
            return R.error(400, "Milvus 未启用，无法重建");
        }
        try {
            milvusConfig.rebuildCollection(milvusClient);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("success", true);
            r.put("message", "Milvus 集合重建完成");
            return R.ok(r);
        } catch (Exception e) {
            log.error("Milvus rebuild failed", e);
            return R.error(500, "重建失败: " + e.getMessage());
        }
    }

    /** 从同义词DB表同步到Milvus（不重跑LLM），异步执行 */
    @PostMapping("/milvus/sync-from-dict")
    public R<Map<String, Object>> syncFromDict(@RequestBody Map<String, Object> body) {
        String entityType = (String) body.get("entityType");
        int limit = body.get("limit") instanceof Number n ? n.intValue() : 0;
        CompletableFuture.runAsync(() -> synonymService.syncFromDict(entityType, limit));
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", "同步已在后台启动，可通过 /status 查看进度");
        return R.ok(resp);
    }

    /** 手动 flush：把内存 buffer 中的向量落盘成 sealed segment，之后 Attu / query 才能看到全部数据 */
    @PostMapping("/milvus/flush")
    public R<Map<String, Object>> flushMilvus() {
        if (milvusClient == null) {
            return R.error(400, "Milvus 未启用，无法 flush");
        }
        try {
            io.milvus.param.R<io.milvus.grpc.FlushResponse> resp = milvusClient.flush(
                    io.milvus.param.collection.FlushParam.newBuilder()
                            .withCollectionNames(java.util.List.of(milvusConfig.getCollectionName()))
                            .withSyncFlush(true)
                            .build());
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("success", resp.getStatus() == 0);
            r.put("message", resp.getStatus() == 0 ? "Milvus flush 完成" : "flush 失败: " + resp.getMessage());
            r.put("flushTimestamp", System.currentTimeMillis());
            return R.ok(r);
        } catch (Exception e) {
            log.error("Milvus flush failed", e);
            return R.error(500, "flush 失败: " + e.getMessage());
        }
    }

    /** 手动 load：把 collection 加载进 querynode（flush 后 query 仍报未加载时用） */
    @PostMapping("/milvus/load")
    public R<Map<String, Object>> loadMilvus() {
        if (milvusClient == null) {
            return R.error(400, "Milvus 未启用，无法 load");
        }
        try {
            // syncLoad=true 显式等待加载完成
            io.milvus.param.R<io.milvus.param.RpcStatus> resp = milvusClient.loadCollection(
                    io.milvus.param.collection.LoadCollectionParam.newBuilder()
                            .withCollectionName(milvusConfig.getCollectionName())
                            .withSyncLoad(true)
                            .build());
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("success", resp.getStatus() == 0);
            r.put("message", resp.getStatus() == 0 ? "Milvus load 完成" : "load 失败: " + resp.getMessage());
            return R.ok(r);
        } catch (Exception e) {
            log.error("Milvus load failed", e);
            return R.error(500, "load 失败: " + e.getMessage());
        }
    }
}
