package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.mapper.EnumTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 枚举类型 (Enum types) REST 接口：分组 / 类型 / 项 / 编码规则
 */
@RestController
@RequestMapping("/api/enum-types")
public class EnumTypeController {

    @Autowired private EnumTypeMapper mapper;
    @Autowired private com.beiktech.bontolink.mapper.BizGroupMapper groupMapper;
    @Autowired private com.beiktech.bontolink.service.EnumSyncService syncService;

    /* ===== 分组 ===== */

    /** 查询所有枚举分组列表 */
    @GetMapping("/groups")
    public R<List<Map<String, Object>>> listGroups() { return R.ok(mapper.listGroups()); }

    /** 新建枚举分组；id 前缀 "enum-groups-"，sort_num/status 缺省补全 */
    @PostMapping("/groups")
    public R<Map<String, Object>> createGroup(@RequestBody Map<String, Object> body) {
        body.put("id", "enum-groups-" + UUID.randomUUID());
        body.putIfAbsent("sort_num", 0);
        body.putIfAbsent("status", "active");
        mapper.insertGroup(body);
        return R.ok(body);
    }

    /** 更新指定分组的名称/排序/状态等基本信息 */
    @PutMapping("/groups/{id}")
    public R<?> updateGroup(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("sort_num", 0);
        body.putIfAbsent("status", "active");
        mapper.updateGroup(body);
        return R.ok();
    }

    /** 删除指定枚举分组 */
    @DeleteMapping("/groups/{id}")
    public R<?> deleteGroup(@PathVariable String id) {
        mapper.deleteGroup(id);
        return R.ok();
    }

    /* ===== 枚举类型主表 ===== */

    /** 查询所有枚举类型列表（不含枚举项） */
    @GetMapping
    public R<List<Map<String, Object>>> listTypes() { return R.ok(mapper.listTypes()); }

    /** 获取枚举类型详情，含枚举项列表（items）和层次编码规则（levelRules） */
    @GetMapping("/{id}")
    public R<Map<String, Object>> getType(@PathVariable String id) {
        Map<String, Object> t = mapper.findType(id);
        if (t == null) return R.ok(new LinkedHashMap<>());
        t.put("items", mapper.listItems(id));
        t.put("levelRules", mapper.listLevelRules(id));
        return R.ok(t);
    }

    /** 新建枚举类型；rid 优先取 api_name 生成，enum_type 默认 "general_single"，max_level 默认 1 */
    @PostMapping
    public R<Map<String, Object>> createType(@RequestBody Map<String, Object> body) {
        String id = "enum-types-" + UUID.randomUUID();
        body.put("id", id);
        // rid 格式: ri.ont.enum.library.{api_name}，api_name 缺失时回退使用 id
        body.putIfAbsent("rid", "ri.ont.enum.library." + (body.get("api_name") != null ? body.get("api_name") : id));
        body.putIfAbsent("enum_type", "general_single");
        body.putIfAbsent("max_level", 1);
        body.putIfAbsent("status", "active");
        mapper.insertType(body);
        return R.ok(body);
    }

    /** 更新枚举类型基本信息（名称/说明/分组/状态等） */
    @PutMapping("/{id}")
    public R<?> updateType(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("status", "active");
        body.putIfAbsent("enum_type", "general_single");
        body.putIfAbsent("max_level", 1);
        mapper.updateType(body);
        return R.ok();
    }

    /** 删除枚举类型（级联删除枚举项和编码规则由 Mapper/数据库负责） */
    @DeleteMapping("/{id}")
    public R<?> deleteType(@PathVariable String id) {
        groupMapper.deleteRefsByRefId(id, "enum_types");
        mapper.deleteType(id);
        return R.ok();
    }

    /* ===== 枚举项 ===== */

    /** 查询指定枚举类型下的所有枚举项 */
    @GetMapping("/{enumId}/items")
    public R<List<Map<String, Object>>> listItems(@PathVariable String enumId) {
        return R.ok(mapper.listItems(enumId));
    }

    /** 向指定枚举类型添加枚举项；level 默认 1（单层），sort_num 默认 0 */
    @PostMapping("/{enumId}/items")
    public R<Map<String, Object>> addItem(@PathVariable String enumId, @RequestBody Map<String, Object> body) {
        body.put("id", "enum-item-" + UUID.randomUUID());
        body.put("enum_id", enumId);
        body.putIfAbsent("level", 1);
        body.putIfAbsent("sort_num", 0);
        body.putIfAbsent("status", "active");
        mapper.insertItem(body);
        return R.ok(body);
    }

    /** 更新单个枚举项的值/名称/排序/层级等信息 */
    @PutMapping("/items/{id}")
    public R<?> updateItem(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("level", 1);
        body.putIfAbsent("sort_num", 0);
        body.putIfAbsent("status", "active");
        mapper.updateItem(body);
        return R.ok();
    }

    /** 删除单个枚举项 */
    @DeleteMapping("/items/{id}")
    public R<?> deleteItem(@PathVariable String id) {
        mapper.deleteItem(id);
        return R.ok();
    }

    /* ===== 层次编码规则 (整体保存：先删后写) ===== */

    /** 查询指定枚举类型的层次编码规则列表 */
    @GetMapping("/{enumId}/level-rules")
    public R<List<Map<String, Object>>> listLevelRules(@PathVariable String enumId) {
        return R.ok(mapper.listLevelRules(enumId));
    }

    /** 整体保存层次编码规则（先清空再批量写入），rule_level 按传入顺序自动从 1 递增 */
    @PostMapping("/{enumId}/level-rules")
    public R<?> saveLevelRules(@PathVariable String enumId, @RequestBody List<Map<String, Object>> rules) {
        mapper.deleteLevelRulesByEnum(enumId);
        if (rules != null) {
            int level = 1;
            for (Map<String, Object> rule : rules) {
                rule.put("id", "enum_level_code_rule-" + UUID.randomUUID());
                rule.put("enum_id", enumId);
                rule.putIfAbsent("rule_level", level++);
                rule.putIfAbsent("code_separator", "");
                rule.putIfAbsent("fill_char", "0");
                rule.putIfAbsent("fill_pos", 0);
                mapper.insertLevelRule(rule);
            }
        }
        return R.ok();
    }

    /* ===== 数据库同步配置 ===== */

    /** 获取指定枚举类型的外部数据库同步配置；未配置时返回空对象 */
    @GetMapping("/{enumId}/sync-config")
    public R<Map<String, Object>> getSyncConfig(@PathVariable String enumId) {
        Map<String, Object> cfg = mapper.findSyncConfig(enumId);
        return R.ok(cfg == null ? new LinkedHashMap<>() : cfg);
    }

    /** 保存同步配置（有则更新、无则新建），sync_mode 默认 "level_diff"，sync_strategy 默认 "once" */
    @PostMapping("/{enumId}/sync-config")
    public R<Map<String, Object>> saveSyncConfig(@PathVariable String enumId, @RequestBody Map<String, Object> body) {
        Map<String, Object> exist = mapper.findSyncConfig(enumId);
        body.put("enum_id", enumId);
        body.putIfAbsent("sync_mode", "level_diff");
        body.putIfAbsent("sync_strategy", "once");
        if (exist == null) {
            // 首次保存，生成新 id
            body.put("id", "enum-sync-config-" + UUID.randomUUID());
            mapper.insertSyncConfig(body);
        } else {
            body.put("id", exist.get("id"));
            mapper.updateSyncConfig(body);
        }
        return R.ok(body);
    }

    /* ===== 同步日志 ===== */

    /** 查询指定枚举类型的同步执行历史日志列表 */
    @GetMapping("/{enumId}/sync-logs")
    public R<List<Map<String, Object>>> listSyncLogs(@PathVariable String enumId) {
        return R.ok(mapper.listSyncLogs(enumId));
    }

    /**
     * 立即执行同步: 连接配置的数据源, 按字段映射拉取目标表数据写入 ont_enum_items, 返回带真实计数的日志。
     */
    @PostMapping("/{enumId}/sync-run")
    public R<Map<String, Object>> runSync(@PathVariable String enumId, @RequestBody(required = false) Map<String, Object> body) {
        String syncType = body != null && body.get("sync_type") != null ? String.valueOf(body.get("sync_type")) : "manual";
        String operUser = body != null && body.get("oper_user") != null ? String.valueOf(body.get("oper_user")) : null;
        Map<String, Object> log = syncService.run(enumId, syncType, operUser);
        if ("failed".equals(log.get("sync_status"))) {
            return R.error(500, "同步失败: " + (log.get("error_msg") == null ? "未知错误" : log.get("error_msg")));
        }
        return R.ok(log);
    }

    /**
     * 测试数据库连接 (占位实现): 真实实现需要根据 data_source_id 调用数据源管理模块的连通性测试.
     */
    @PostMapping("/{enumId}/sync-test")
    public R<Map<String, Object>> testSync(@PathVariable String enumId, @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("message", "连接测试占位返回 OK (待联调数据源测试接口)");
        return R.ok(result);
    }

    /**
     * SQL 模式预览：执行自定义 SQL (LIMIT 5)，返回列名 + 示例行，供前端字段映射使用。
     */
    @PostMapping("/sync-preview-sql")
    public R<Map<String, Object>> previewSql(@RequestBody Map<String, Object> body) {
        String dsId = body.get("data_source_id") == null ? "" : String.valueOf(body.get("data_source_id"));
        String sql  = body.get("custom_sql") == null ? "" : String.valueOf(body.get("custom_sql"));
        if (dsId.isBlank() || sql.isBlank()) {
            return R.error(400, "data_source_id 和 custom_sql 不能为空");
        }
        try {
            Map<String, Object> preview = syncService.previewSql(dsId, sql);
            return R.ok(preview);
        } catch (Exception e) {
            return R.error(500, "SQL 预览失败: " + e.getMessage());
        }
    }

    /* ===== 被引用查询 ===== */

    /** 查询枚举类型被哪些对象属性/共享属性引用，用于删除前的引用检查 */
    @GetMapping("/{enumId}/references")
    public R<List<Map<String, Object>>> listReferences(@PathVariable String enumId) {
        return R.ok(mapper.listReferences(enumId));
    }
}
