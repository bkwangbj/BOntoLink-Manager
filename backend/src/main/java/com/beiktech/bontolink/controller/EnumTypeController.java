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

    /* ===== 分组 ===== */

    @GetMapping("/groups")
    public R<List<Map<String, Object>>> listGroups() { return R.ok(mapper.listGroups()); }

    @PostMapping("/groups")
    public R<Map<String, Object>> createGroup(@RequestBody Map<String, Object> body) {
        body.put("id", "enum-groups-" + UUID.randomUUID());
        body.putIfAbsent("sort_num", 0);
        body.putIfAbsent("status", "active");
        mapper.insertGroup(body);
        return R.ok(body);
    }

    @PutMapping("/groups/{id}")
    public R<?> updateGroup(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("sort_num", 0);
        body.putIfAbsent("status", "active");
        mapper.updateGroup(body);
        return R.ok();
    }

    @DeleteMapping("/groups/{id}")
    public R<?> deleteGroup(@PathVariable String id) {
        mapper.deleteGroup(id);
        return R.ok();
    }

    /* ===== 枚举类型主表 ===== */

    @GetMapping
    public R<List<Map<String, Object>>> listTypes() { return R.ok(mapper.listTypes()); }

    @GetMapping("/{id}")
    public R<Map<String, Object>> getType(@PathVariable String id) {
        Map<String, Object> t = mapper.findType(id);
        if (t == null) return R.ok(new LinkedHashMap<>());
        t.put("items", mapper.listItems(id));
        t.put("levelRules", mapper.listLevelRules(id));
        return R.ok(t);
    }

    @PostMapping
    public R<Map<String, Object>> createType(@RequestBody Map<String, Object> body) {
        String id = "enum-types-" + UUID.randomUUID();
        body.put("id", id);
        body.putIfAbsent("rid", "ri.ont.enum.library." + (body.get("api_name") != null ? body.get("api_name") : id));
        body.putIfAbsent("enum_type", "general_single");
        body.putIfAbsent("max_level", 1);
        body.putIfAbsent("status", "active");
        mapper.insertType(body);
        return R.ok(body);
    }

    @PutMapping("/{id}")
    public R<?> updateType(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("status", "active");
        body.putIfAbsent("enum_type", "general_single");
        body.putIfAbsent("max_level", 1);
        mapper.updateType(body);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<?> deleteType(@PathVariable String id) {
        mapper.deleteType(id);
        return R.ok();
    }

    /* ===== 枚举项 ===== */

    @GetMapping("/{enumId}/items")
    public R<List<Map<String, Object>>> listItems(@PathVariable String enumId) {
        return R.ok(mapper.listItems(enumId));
    }

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

    @PutMapping("/items/{id}")
    public R<?> updateItem(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("level", 1);
        body.putIfAbsent("sort_num", 0);
        body.putIfAbsent("status", "active");
        mapper.updateItem(body);
        return R.ok();
    }

    @DeleteMapping("/items/{id}")
    public R<?> deleteItem(@PathVariable String id) {
        mapper.deleteItem(id);
        return R.ok();
    }

    /* ===== 层次编码规则 (整体保存：先删后写) ===== */

    @GetMapping("/{enumId}/level-rules")
    public R<List<Map<String, Object>>> listLevelRules(@PathVariable String enumId) {
        return R.ok(mapper.listLevelRules(enumId));
    }

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

    @GetMapping("/{enumId}/sync-config")
    public R<Map<String, Object>> getSyncConfig(@PathVariable String enumId) {
        Map<String, Object> cfg = mapper.findSyncConfig(enumId);
        return R.ok(cfg == null ? new LinkedHashMap<>() : cfg);
    }

    @PostMapping("/{enumId}/sync-config")
    public R<Map<String, Object>> saveSyncConfig(@PathVariable String enumId, @RequestBody Map<String, Object> body) {
        Map<String, Object> exist = mapper.findSyncConfig(enumId);
        body.put("enum_id", enumId);
        body.putIfAbsent("sync_mode", "level_diff");
        body.putIfAbsent("sync_strategy", "once");
        if (exist == null) {
            body.put("id", "enum-sync-config-" + UUID.randomUUID());
            mapper.insertSyncConfig(body);
        } else {
            body.put("id", exist.get("id"));
            mapper.updateSyncConfig(body);
        }
        return R.ok(body);
    }

    /* ===== 同步日志 ===== */

    @GetMapping("/{enumId}/sync-logs")
    public R<List<Map<String, Object>>> listSyncLogs(@PathVariable String enumId) {
        return R.ok(mapper.listSyncLogs(enumId));
    }

    /**
     * 立即执行同步 (占位实现): 当前仅插入一条 success 日志,真实拉取数据由后续同步引擎实现.
     */
    @PostMapping("/{enumId}/sync-run")
    public R<Map<String, Object>> runSync(@PathVariable String enumId, @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("id", "enum-sync-log-" + UUID.randomUUID());
        log.put("enum_id", enumId);
        log.put("sync_type", body != null && body.get("sync_type") != null ? body.get("sync_type") : "manual");
        log.put("add_count", 0);
        log.put("update_count", 0);
        log.put("del_count", 0);
        log.put("fail_count", 0);
        log.put("sync_status", "success");
        log.put("error_msg", null);
        log.put("oper_user", body != null ? body.get("oper_user") : null);
        mapper.insertSyncLog(log);
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

    /* ===== 被引用查询 ===== */

    @GetMapping("/{enumId}/references")
    public R<List<Map<String, Object>>> listReferences(@PathVariable String enumId) {
        return R.ok(mapper.listReferences(enumId));
    }
}
