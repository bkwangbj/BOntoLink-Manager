package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.data.mapper.ActionTypeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 动作类型 (Action Types) REST 接口
 * <p>
 * 路径: /api/action-types 。P1 覆盖主表 CRUD + 状态切换 + 批量删除。
 * 规则/表单/覆盖/提交标准等子配置由后续阶段 (P2/P3) 扩展。
 */
@RestController
@RequestMapping("/api/action-types")
public class ActionTypeController {

    @Autowired private ActionTypeMapper mapper;

    /** 查询所有动作类型列表 */
    @GetMapping
    public R<List<Map<String, Object>>> list() { return R.ok(mapper.listAll()); }

    /** 详情 (含表单分组/参数 + 规则[带属性映射] + 提交标准) */
    @GetMapping("/{id}")
    public R<Map<String, Object>> get(@PathVariable String id) {
        Map<String, Object> row = mapper.findById(id);
        if (row == null) return R.ok(null);
        row.put("form_sections", mapper.listFormSections(id));
        row.put("form_params", mapper.listFormParams(id));
        List<Map<String, Object>> rules = mapper.listRules(id);
        for (Map<String, Object> r : rules) {
            r.put("prop_mappings", mapper.listRulePropMappings(String.valueOf(r.get("id"))));
        }
        row.put("rules", rules);
        Map<String, Object> ss = mapper.getSubmitStandard(id);
        if (ss != null) ss.put("nodes", mapper.listSubmitConditionNodes(String.valueOf(ss.get("id"))));
        row.put("submit_standard", ss);
        return R.ok(row);
    }

    /** 创建 (可携带 form_params: 新建向导「参数映射」步骤) */
    @PostMapping
    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String code = String.valueOf(body.getOrDefault("api_name", "")).trim();
        if (code.isEmpty()) return R.error(400, "动作编码 (api_name) 不能为空");
        if (!code.matches("^[a-z][a-z0-9_]*$"))
            return R.error(400, "api_name 只能含小写字母 / 数字 / 下划线, 且首字符必须为小写字母");
        if (mapper.existsByCode(code) != null) return R.error(400, "api_name " + code + " 已存在");
        String label = String.valueOf(body.getOrDefault("rdfs_label", "")).trim();
        if (label.isEmpty()) return R.error(400, "动作名称 (rdfs_label) 不能为空");

        String id = "class_action-" + UUID.randomUUID();
        body.put("id", id);
        body.put("api_name", code);
        // rid 自动生成: 空白则按编码生成
        String rid = String.valueOf(body.getOrDefault("rid", "")).trim();
        if (rid.isEmpty() || "null".equalsIgnoreCase(rid)) body.put("rid", "ri.ont.action." + code);
        // NOT NULL 带默认值的字段, 显式补默认避免传 NULL 违反约束
        body.putIfAbsent("status", 0);                    // 0草稿
        body.putIfAbsent("is_deleted", 0);
        body.putIfAbsent("show_on_detail", 0);
        body.putIfAbsent("show_on_batch", 0);
        body.putIfAbsent("compile_status", 0);            // 0未编译
        body.putIfAbsent("form_enabled", 0);
        body.putIfAbsent("submit_criteria_enabled", 0);
        // 有子配置则推导能力开关
        List<Map<String, Object>> params = asList(body.get("form_params"));
        if (!params.isEmpty()) body.put("form_enabled", 1);
        Object ss = body.get("submit_standard");
        if (ss instanceof Map && toInt(((Map<String, Object>) ss).getOrDefault("enabled", 0)) == 1)
            body.put("submit_criteria_enabled", 1);
        mapper.insert(body);
        saveFormParams(id, params);
        if (body.containsKey("rules")) saveRules(id, asList(body.get("rules")));
        if (body.containsKey("submit_standard")) saveSubmitStandard(id, (Map<String, Object>) ss);
        return R.ok(mapper.findById(id));
    }

    /** 保存表单参数: 建一个默认分组, 逐条插入参数 */
    private void saveFormParams(String actionId, List<Map<String, Object>> params) {
        if (params.isEmpty()) return;
        String sectionId = "action_form_section-" + UUID.randomUUID();
        Map<String, Object> sec = new LinkedHashMap<>();
        sec.put("id", sectionId);
        sec.put("action_id", actionId);
        sec.put("section_name", "default");
        sec.put("title", "基础参数");
        sec.put("description", null);
        sec.put("collapsed", 0);
        sec.put("sort", 0);
        mapper.insertFormSection(sec);
        int i = 0;
        for (Map<String, Object> p : params) {
            String code = String.valueOf(p.getOrDefault("param_code", "")).trim();
            if (code.isEmpty()) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "action_form_param-" + UUID.randomUUID());
            row.put("action_id", actionId);
            row.put("section_id", sectionId);
            row.put("param_code", code);
            row.put("param_name", p.get("param_name"));
            row.put("param_type", p.getOrDefault("param_type", "string"));
            row.put("data_type", p.get("data_type"));
            row.put("is_required", toInt(p.get("is_required")));
            row.put("is_multi", toInt(p.get("is_multi")));
            row.put("default_value", p.get("default_value"));
            row.put("placeholder", p.get("placeholder"));
            row.put("sort", p.getOrDefault("sort", i++));
            row.put("config", p.get("config"));
            mapper.insertFormParam(row);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object raw) {
        return raw instanceof List ? (List<Map<String, Object>>) raw : Collections.emptyList();
    }
    private int toInt(Object v) {
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof Boolean) return (Boolean) v ? 1 : 0;
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return 0; }
    }

    /** 更新 */
    @PutMapping("/{id}")
    @SuppressWarnings("unchecked")
    public R<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Map<String, Object> old = mapper.findById(id);
        if (old == null) return R.error(404, "未找到动作");
        body.put("id", id);
        // rdfs_label 必填 (为空则沿用旧值)
        String label = String.valueOf(body.getOrDefault("rdfs_label", "")).trim();
        if (label.isEmpty()) body.put("rdfs_label", old.get("rdfs_label"));
        // api_name 沿用旧值 (编辑态不允许改编码, 前端只读; 缺省时回填)
        if (String.valueOf(body.getOrDefault("api_name", "")).trim().isEmpty())
            body.put("api_name", old.get("api_name"));
        // rid 为空补生成
        String rid = String.valueOf(body.getOrDefault("rid", "")).trim();
        if (rid.isEmpty() || "null".equalsIgnoreCase(rid)) {
            String code = String.valueOf(body.getOrDefault("api_name", "")).trim();
            body.put("rid", code.isEmpty() ? old.get("rid") : "ri.ont.action." + code);
        }
        // 类型/身份字段: 请求未携带该键时保留旧值 (避免 partial PUT 误清空)
        for (String k : new String[]{"m_type", "action_type", "object_class_id", "link_type_id",
                "function_code", "category_code", "button_text", "icon", "color", "current_version",
                "metadata", "rdfs_comment", "rdfs_see_also", "rdfs_defined_by"}) {
            if (!body.containsKey(k)) body.put(k, old.get(k));
        }
        // NOT NULL 字段: body 缺省时回填旧值, 避免传 NULL
        for (String k : new String[]{"status", "show_on_detail", "show_on_batch",
                "compile_status", "form_enabled", "submit_criteria_enabled"}) {
            if (body.get(k) == null) body.put(k, old.getOrDefault(k, 0));
        }
        // 有子配置时自动推导能力开关 (客户端未显式传时兜底)
        if (!asList(body.get("form_params")).isEmpty()) body.put("form_enabled", 1);
        Object ss0 = body.get("submit_standard");
        if (ss0 instanceof Map && toInt(((Map<String, Object>) ss0).getOrDefault("enabled", 0)) == 1)
            body.put("submit_criteria_enabled", 1);
        mapper.update(body);
        // 子配置整体覆盖 (仅当请求携带对应键时)
        if (body.containsKey("form_params")) {
            mapper.deleteFormSectionsByAction(id);
            mapper.deleteFormParamsByAction(id);
            saveFormParams(id, asList(body.get("form_params")));
        }
        if (body.containsKey("rules")) {
            mapper.deleteRulePropMappingsByAction(id);
            mapper.deleteRulesByAction(id);
            saveRules(id, asList(body.get("rules")));
        }
        if (body.containsKey("submit_standard")) {
            saveSubmitStandard(id, (Map<String, Object>) body.get("submit_standard"));
        }
        return R.ok();
    }

    /** 保存规则 + 其属性映射 */
    private void saveRules(String actionId, List<Map<String, Object>> rules) {
        int ri = 0;
        for (Map<String, Object> rule : rules) {
            String ruleId = "action_rule-" + UUID.randomUUID();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", ruleId);
            row.put("action_id", actionId);
            row.put("action_type", rule.get("action_type"));
            row.put("rule_type", toInt(rule.getOrDefault("rule_type", 1)));
            row.put("rule_name", rule.get("rule_name"));
            row.put("target_param_code", rule.get("target_param_code"));
            row.put("link_type_code", rule.get("link_type_code"));
            row.put("sort", rule.getOrDefault("sort", ri++));
            row.put("status", toInt(rule.getOrDefault("status", 1)));
            row.put("rule_config", rule.get("rule_config"));
            mapper.insertRule(row);
            int mi = 0;
            for (Map<String, Object> m : asList(rule.get("prop_mappings"))) {
                String code = String.valueOf(m.getOrDefault("property_code", "")).trim();
                if (code.isEmpty()) continue;
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("id", "rule_prop-" + UUID.randomUUID());
                mm.put("rule_id", ruleId);
                mm.put("property_code", code);
                mm.put("property_name", m.get("property_name"));
                mm.put("prop_operator", m.getOrDefault("prop_operator", "set"));
                mm.put("value_source", toInt(m.getOrDefault("value_source", 1)));
                mm.put("value_content", m.get("value_content"));
                mm.put("is_primary_key", toInt(m.get("is_primary_key")));
                mm.put("is_required", toInt(m.get("is_required")));
                mm.put("sort", m.getOrDefault("sort", mi++));
                mapper.insertRulePropMapping(mm);
            }
        }
    }

    /** 保存提交标准 (单条覆盖) + 条件节点树 */
    private void saveSubmitStandard(String actionId, Map<String, Object> cfg) {
        mapper.deleteSubmitConditionNodesByAction(actionId);
        mapper.deleteSubmitStandardsByAction(actionId);
        if (cfg == null) return;
        String standardId = "submit_standard-" + UUID.randomUUID();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", standardId);
        row.put("action_id", actionId);
        row.put("enabled", toInt(cfg.getOrDefault("enabled", 0)));
        row.put("validate_mode", cfg.getOrDefault("validate_mode", "all"));
        row.put("error_message", cfg.get("error_message"));
        row.put("config", cfg.get("config"));
        mapper.insertSubmitStandard(row);
        // 条件节点树: remap 客户端临时 id → 服务端 id, 保持 parent 引用
        List<Map<String, Object>> nodes = asList(cfg.get("nodes"));
        if (nodes.isEmpty()) return;
        Map<String, String> idMap = new LinkedHashMap<>();
        for (Map<String, Object> n : nodes) idMap.put(String.valueOf(n.get("id")), "submit_node-" + UUID.randomUUID());
        for (Map<String, Object> n : nodes) {
            Map<String, Object> nr = new LinkedHashMap<>();
            nr.put("id", idMap.get(String.valueOf(n.get("id"))));
            nr.put("standard_id", standardId);
            Object pid = n.get("parent_id");
            nr.put("parent_id", pid == null ? null : idMap.get(String.valueOf(pid)));
            nr.put("node_type", n.getOrDefault("node_type", "group"));
            nr.put("logic_op", n.get("logic_op"));
            nr.put("left_code", n.get("left_code"));
            nr.put("operator", n.get("operator"));
            nr.put("right_value", n.get("right_value"));
            nr.put("value_source", n.get("value_source"));
            nr.put("error_message", null);
            nr.put("sort", n.getOrDefault("sort", 0));
            mapper.insertSubmitConditionNode(nr);
        }
    }

    /** 删除 (级联清理直接子配置) */
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        cascadeDelete(id);
        return R.ok();
    }

    /** 批量删除 */
    @PostMapping("/batch-delete")
    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> batchDelete(@RequestBody Map<String, Object> body) {
        List<String> ids = (List<String>) body.getOrDefault("ids", Collections.emptyList());
        int ok = 0;
        for (String id : ids) { cascadeDelete(id); ok++; }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deleted", ok);
        return R.ok(result);
    }

    /** 切换状态 (0草稿 / 1已发布 / 2已停用) */
    @PostMapping("/{id}/status")
    public R<?> setStatus(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Map<String, Object> old = mapper.findById(id);
        if (old == null) return R.error(404, "未找到动作");
        mapper.updateStatus(id, body.getOrDefault("status", 0));
        return R.ok();
    }

    /* =========================================================================
     * 执行引擎 (P4): 参数校验 → 规则/值来源解析 → 计算结果实例 → 记录执行日志
     * 说明: 系统当前无真实对象实例存储, 故为"模拟执行"——计算出"将写入的实例记录"
     * 与"将触发的副作用", 记入 ont_action_execution, 不写物理表。
     * ========================================================================= */
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<Integer, String> ACTION_TYPE_LABEL = Map.ofEntries(
        Map.entry(11, "创建对象"), Map.entry(12, "修改对象"), Map.entry(13, "Upsert 对象"), Map.entry(14, "删除对象"),
        Map.entry(21, "创建链接"), Map.entry(22, "删除链接"), Map.entry(30, "函数"), Map.entry(40, "Webhook"),
        Map.entry(51, "接口·创建"), Map.entry(52, "接口·修改"), Map.entry(53, "接口·删除"), Map.entry(54, "接口·查询"), Map.entry(60, "通知"));

    /** 执行 / 试运行 一个动作 */
    @PostMapping("/{id}/execute")
    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> execute(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> action = mapper.findById(id);
        if (action == null) return R.error(404, "未找到动作");
        if (body == null) body = new LinkedHashMap<>();
        Map<String, Object> params = body.get("params") instanceof Map ? (Map<String, Object>) body.get("params") : new LinkedHashMap<>();
        boolean dryRun = !Boolean.FALSE.equals(body.get("dry_run"));   // 默认试运行
        String user = String.valueOf(body.getOrDefault("executed_by", "admin"));

        int opType = toInt(action.get("action_type"));
        List<Map<String, Object>> formParams = mapper.listFormParams(id);
        List<Map<String, Object>> rules = mapper.listRules(id);
        Map<String, Object> submitStd = mapper.getSubmitStandard(id);

        // 1) 校验: 必填表单参数
        List<String> errors = new ArrayList<>();
        for (Map<String, Object> fp : formParams) {
            if (toInt(fp.get("is_required")) == 1) {
                Object v = params.get(String.valueOf(fp.get("param_code")));
                if (v == null || String.valueOf(v).trim().isEmpty())
                    errors.add("缺少必填参数: " + fp.getOrDefault("param_name", fp.get("param_code")));
            }
        }
        boolean submitEnabled = submitStd != null && toInt(submitStd.get("enabled")) == 1;

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("op_type", opType);
        result.put("op_label", ACTION_TYPE_LABEL.getOrDefault(opType, "动作"));
        result.put("object_class_id", action.get("object_class_id"));
        result.put("object_class_name", action.get("object_class_name"));
        result.put("dry_run", dryRun);
        result.put("submit_check", submitEnabled ? "已启用(条件树待配置, 视为通过)" : "未启用");

        if (!errors.isEmpty()) {
            result.put("errors", errors);
            record(id, action, opType, params, result, "validation_failed", String.join("; ", errors), dryRun, user);
            result.put("status", "validation_failed");
            return R.ok(result);
        }

        // 2) 解析: 表单参数为基底, 编辑类规则的属性映射覆盖
        Map<String, Object> instance = new LinkedHashMap<>();
        List<Map<String, Object>> detail = new ArrayList<>();
        for (Map<String, Object> fp : formParams) {
            String code = String.valueOf(fp.get("param_code"));
            if (params.containsKey(code)) instance.put(code, params.get(code));
        }
        for (Map<String, Object> rule : rules) {
            if (toInt(rule.get("rule_type")) != 1) continue;
            for (Map<String, Object> m : mapper.listRulePropMappings(String.valueOf(rule.get("id")))) {
                String prop = String.valueOf(m.get("property_code"));
                if (prop.isEmpty() || "null".equals(prop)) continue;
                int vs = toInt(m.get("value_source"));
                Object val = resolveValue(vs, m.get("value_content"), prop, params, user, now);
                String op = String.valueOf(m.getOrDefault("prop_operator", "set"));
                instance.put(prop, applyOperator(op, instance.get(prop), val));
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("property", prop); d.put("operator", op); d.put("source", VALUE_SOURCE_LABEL.getOrDefault(vs, "?"));
                d.put("value", instance.get(prop));
                detail.add(d);
            }
        }

        // 3) 副作用规则 (登记, 不真执行)
        List<Map<String, Object>> sideEffects = new ArrayList<>();
        for (Map<String, Object> rule : rules) {
            if (toInt(rule.get("rule_type")) != 2) continue;
            Map<String, Object> se = new LinkedHashMap<>();
            se.put("rule_name", rule.get("rule_name"));
            se.put("target", rule.get("link_type_code"));
            se.put("config", rule.get("rule_config"));
            sideEffects.add(se);
        }

        result.put("instance", instance);
        result.put("resolution", detail);
        result.put("side_effects", sideEffects);
        result.put("status", "success");
        String execId = record(id, action, opType, params, result, "success", dryRun ? "试运行成功" : "执行成功", dryRun, user);
        result.put("execution_id", execId);
        return R.ok(result);
    }

    /** 执行历史 */
    @GetMapping("/{id}/executions")
    public R<List<Map<String, Object>>> executions(@PathVariable String id) {
        return R.ok(mapper.listExecutions(id));
    }

    private static final Map<Integer, String> VALUE_SOURCE_LABEL = Map.of(
        1, "表单参数", 2, "静态值", 3, "当前用户", 4, "系统时间",
        5, "关联对象属性", 6, "主对象", 7, "本动作创建的对象");

    /** 按值来源解析取值 */
    private Object resolveValue(int vs, Object content, String propCode, Map<String, Object> params, String user, String now) {
        String c = content == null ? "" : String.valueOf(content);
        switch (vs) {
            case 1: {  // 表单参数: value_content 为参数编码, 空则回退用属性编码
                String key = c.isEmpty() ? propCode : c;
                return params.getOrDefault(key, null);
            }
            case 2: return c;                                   // 静态值
            case 3: return user;                                // 当前用户
            case 4: return now;                                 // 系统时间
            case 5: return "[关联对象." + (c.isEmpty() ? propCode : c) + "]"; // 关联对象属性(占位)
            case 6: return "[主对象." + (c.isEmpty() ? propCode : c) + "]";   // 主对象属性(占位, 需详情页上下文)
            case 7: return "[本动作新建." + c + "]";                          // 前序创建规则的产物(占位, 需事务内实例引用)
            default: return c;
        }
    }

    /** 应用属性操作符 (模拟, 无既有值时按语义处理) */
    private Object applyOperator(String op, Object oldVal, Object newVal) {
        switch (op == null ? "set" : op) {
            case "clear":  return null;
            case "append": return (oldVal == null ? "" : oldVal.toString()) + (newVal == null ? "" : newVal.toString());
            case "add": case "sub": {
                Double a = toDouble(oldVal), b = toDouble(newVal);
                if (a != null && b != null) return "add".equals(op) ? a + b : a - b;
                return newVal;   // 非数值退回赋值
            }
            default: return newVal;   // set
        }
    }
    private Double toDouble(Object v) { if (v == null) return null; try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return null; } }

    /** 记录一条执行日志 */
    private String record(String actionId, Map<String, Object> action, int opType, Map<String, Object> params,
                          Map<String, Object> result, String status, String message, boolean dryRun, String user) {
        String execId = "action_exec-" + UUID.randomUUID();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", execId);
        row.put("action_id", actionId);
        row.put("action_api_name", action.get("api_name"));
        row.put("object_class_id", action.get("object_class_id"));
        row.put("op_type", opType);
        row.put("input_params", writeJson(params));
        row.put("resolved_result", writeJson(result));
        row.put("status", status);
        row.put("message", message);
        row.put("dry_run", dryRun ? 1 : 0);
        row.put("executed_by", user);
        try { mapper.insertExecution(row); } catch (Exception ignore) {}
        return execId;
    }
    private String writeJson(Object o) { try { return JSON.writeValueAsString(o); } catch (Exception e) { return "{}"; } }

    private void cascadeDelete(String id) {
        mapper.deleteRulesByAction(id);
        mapper.deleteFormSectionsByAction(id);
        mapper.deleteFormParamsByAction(id);
        mapper.deleteOverrideBlocksByAction(id);
        mapper.deleteFunctionConfigsByAction(id);
        mapper.deleteNotificationConfigsByAction(id);
        mapper.deleteWebhookConfigsByAction(id);
        mapper.deleteSubmitConditionNodesByAction(id);
        mapper.deleteSubmitStandardsByAction(id);
        mapper.deleteGlobalConfigByAction(id);
        mapper.deleteExecutionsByAction(id);
        mapper.delete(id);
    }
}
