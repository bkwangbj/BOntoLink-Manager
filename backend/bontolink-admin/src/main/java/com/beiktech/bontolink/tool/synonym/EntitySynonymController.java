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

    /** 批量生成：{"entityType":"biz_category","limit":0} limit<=0 表示全量 */
    @PostMapping("/batch-generate")
    public R<Map<String, Object>> batchGenerate(@RequestBody Map<String, Object> body) {
        String entityType = (String) body.get("entityType");
        if (entityType == null) {
            return R.error(400, "entityType 不能为空");
        }
        int limit = body.get("limit") instanceof Number n ? n.intValue() : 0;
        Map<String, Object> result = synonymService.batchGenerate(entityType, limit);
        return R.ok(result);
    }

    /** 配置状态：embedding provider、Milvus 是否可用、向量维度 */
    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("embeddingProvider", embeddingService.getClass().getSimpleName());
        s.put("dimension", embeddingService.getDimension());
        s.put("milvusAvailable", milvusClient != null);
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
}
