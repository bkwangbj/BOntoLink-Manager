package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.mapper.BizCategoryMapper;
import com.beiktech.bontolink.mapper.BizNamespaceMapper;
import com.beiktech.bontolink.mapper.OntologyMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 工作区&资源相关只读 API：Discover / 对象类型 / 关系 / 动作 / 接口 / 属性
 */
@RestController
@RequestMapping("/api/resource")
public class ResourceController {

    @Autowired private OntologyMapper ontologyMapper;
    @Autowired private BizCategoryMapper categoryMapper;
    @Autowired private BizNamespaceMapper namespaceMapper;
    @Autowired private com.beiktech.bontolink.mapper.OverviewMapper overviewMapper;

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * 总览 (Overview) 综合统计 —— 严格按 总览6.5.pdf 规范返回 15 项资源 {active, total} 计数.
     *
     * 入参:
     *   industries: 行业 category_code 列表 (逗号分隔), 空 = 全部行业
     *   domains:    领域 / 子领域 category_code 列表 (逗号分隔), 空 = 全部领域
     *
     * 范围解析: 若行业被选中, 自动展开为其下所有领域/子领域 codes; 与 domains 取并集得到最终 scope.
     * 行业本身的计数始终基于"行业选择"; 领域/分组/数据源等"结构资源"基于解析后的 scope.
     */
    @GetMapping("/discover/overview")
    public R<Map<String, Object>> discoverOverview(
            @RequestParam(value = "industries", required = false) String industries,
            @RequestParam(value = "domains", required = false) String domains
    ) {
        Map<String, Object> resp = new LinkedHashMap<>();

        // 1. 解析筛选 codes
        Set<String> industrySet = parseCsv(industries);
        Set<String> domainSet   = parseCsv(domains);

        // 2. 展开行业 → 其下所有 domains (递归子分类)
        List<com.beiktech.bontolink.entity.BizCategory> allCats = categoryMapper.listAll();
        Map<String, com.beiktech.bontolink.entity.BizCategory> byCode = new HashMap<>();
        Map<String, com.beiktech.bontolink.entity.BizCategory> byId = new HashMap<>();
        allCats.forEach(c -> {
            if (c.getCategoryCode() != null) byCode.put(c.getCategoryCode(), c);
            if (c.getId() != null) byId.put(c.getId(), c);
        });
        // 子集索引: parent_id -> children
        Map<String, List<com.beiktech.bontolink.entity.BizCategory>> childrenOf = new HashMap<>();
        allCats.forEach(c -> childrenOf.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c));

        // 行业选中 → 展开其下全部 codes
        Set<String> expandedFromIndustries = new LinkedHashSet<>();
        for (String code : industrySet) {
            com.beiktech.bontolink.entity.BizCategory cat = byCode.get(code);
            if (cat != null) collectDescendantCodes(cat, childrenOf, expandedFromIndustries);
        }

        // 最终 scope = (展开后的行业 codes) ∪ (显式选中的 domain codes)
        Set<String> scope = new LinkedHashSet<>();
        scope.addAll(expandedFromIndustries);
        scope.addAll(domainSet);
        boolean hasFilter = !scope.isEmpty();
        List<String> scopeList = hasFilter ? new ArrayList<>(scope) : null;

        // 3. 计算 15 项统计 {active, total}
        Map<String, Object> rows = new LinkedHashMap<>();
        // 第一行: 行业 / 领域 / 分组 / 数据源
        rows.put("industry",     pair(
                overviewMapper.countIndustries(industrySet.isEmpty() ? null : new ArrayList<>(industrySet), true),
                overviewMapper.countIndustries(industrySet.isEmpty() ? null : new ArrayList<>(industrySet), false)));
        rows.put("domain",       pair(
                overviewMapper.countDomains(scopeList, true),
                overviewMapper.countDomains(scopeList, false)));
        rows.put("group",        pair(
                overviewMapper.countGroups(scopeList, true),
                overviewMapper.countGroups(scopeList, false)));
        rows.put("datasource",   pair(
                overviewMapper.countDatasources(scopeList, true),
                overviewMapper.countDatasources(scopeList, false)));
        // 第二行: 对象 / 关系 / 动作 / 函数 / 类型类 / 接口
        rows.put("objectType",   pair(
                overviewMapper.countObjectTypes(scopeList, true),
                overviewMapper.countObjectTypes(scopeList, false)));
        rows.put("linkType",     pair(
                overviewMapper.countLinkTypes(scopeList, true),
                overviewMapper.countLinkTypes(scopeList, false)));
        rows.put("actionType",   pair(
                overviewMapper.countActionTypes(scopeList, true),
                overviewMapper.countActionTypes(scopeList, false)));
        rows.put("function",     pair(
                overviewMapper.countFunctions(scopeList, true),
                overviewMapper.countFunctions(scopeList, false)));
        rows.put("typeClass",    pair(
                overviewMapper.countTypeClasses(true),
                overviewMapper.countTypeClasses(false)));
        rows.put("interface",    pair(
                overviewMapper.countInterfaces(scopeList, true),
                overviewMapper.countInterfaces(scopeList, false)));
        // 第三行: 属性 / 枚举 / 值类型 / 结构 / 共享
        rows.put("property",        pair(
                overviewMapper.countProperties(scopeList, true),
                overviewMapper.countProperties(scopeList, false)));
        rows.put("enumType",        pair(
                overviewMapper.countEnumTypes(scopeList, true),
                overviewMapper.countEnumTypes(scopeList, false)));
        rows.put("valueType",       pair(
                overviewMapper.countValueTypes(scopeList, true),
                overviewMapper.countValueTypes(scopeList, false)));
        rows.put("structProperty",  pair(
                overviewMapper.countStructProperties(scopeList, true),
                overviewMapper.countStructProperties(scopeList, false)));
        rows.put("sharedProperty",  pair(
                overviewMapper.countSharedProperties(scopeList, true),
                overviewMapper.countSharedProperties(scopeList, false)));

        resp.put("rows", rows);
        resp.put("scope", new LinkedHashMap<String, Object>() {{
            put("industries", new ArrayList<>(industrySet));
            put("domains",    new ArrayList<>(domainSet));
            put("hasFilter",  hasFilter);
        }});
        return R.ok(resp);
    }

    private static Set<String> parseCsv(String s) {
        if (s == null || s.isEmpty()) return new LinkedHashSet<>();
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static void collectDescendantCodes(com.beiktech.bontolink.entity.BizCategory root,
                                               Map<String, List<com.beiktech.bontolink.entity.BizCategory>> childrenOf,
                                               Set<String> sink) {
        if (root == null) return;
        if (root.getCategoryCode() != null) sink.add(root.getCategoryCode());
        List<com.beiktech.bontolink.entity.BizCategory> kids = childrenOf.get(root.getId());
        if (kids != null) for (com.beiktech.bontolink.entity.BizCategory k : kids) collectDescendantCodes(k, childrenOf, sink);
    }

    private static Map<String, Object> pair(int active, int total) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("active", active);
        m.put("total",  total);
        return m;
    }

    /** 兼容旧端点 (sidebar 老页面仍在用) */
    @GetMapping("/discover/stats")
    public R<Map<String, Object>> discoverStats() {
        Map<String, Object> r = new LinkedHashMap<>();
        long industries = categoryMapper.listAll().stream().filter(c -> c.getCategoryType() != null && c.getCategoryType() == 1).count();
        long domains = categoryMapper.listAll().stream().filter(c -> c.getCategoryType() != null && c.getCategoryType() == 2).count();
        r.put("industry", industries);
        r.put("domain", domains);
        r.put("ontClass", ontologyMapper.countClasses());
        r.put("ontInterface", ontologyMapper.countInterfaces());
        r.put("ontLink", ontologyMapper.countLinks());
        r.put("ontAction", ontologyMapper.countActions());
        r.put("ontProperty", ontologyMapper.countProperties());
        r.put("namespace", namespaceMapper.listAll().size());
        return R.ok(r);
    }

    @GetMapping("/classes")
    public R<List<Map<String, Object>>> classes(
            @RequestParam(value = "aggregate", required = false, defaultValue = "false") boolean aggregate) {
        List<Map<String, Object>> rows = ontologyMapper.listClasses();
        if (!aggregate) return R.ok(rows);
        // 聚合附加字段：普通属性数/总属性数、子类数、数据源数、关联表数(列表)、关系数、动作数、接口数、领域中文名
        Map<String, com.beiktech.bontolink.entity.BizCategory> catByCode = new HashMap<>();
        categoryMapper.listAll().forEach(c -> { if (c.getCategoryCode() != null) catByCode.put(c.getCategoryCode(), c); });
        // 为了"领域中文路径"，再准备 id→cat 索引（向上回溯到行业）
        Map<String, com.beiktech.bontolink.entity.BizCategory> catById = new HashMap<>();
        categoryMapper.listAll().forEach(c -> catById.put(c.getId(), c));
        for (Map<String, Object> r : rows) {
            String id = (String) r.get("id");
            String categoryCode = (String) r.get("category_code");
            int total = ontologyMapper.countPropertiesOfClass(id);
            int normal = ontologyMapper.countNormalPropertiesOfClass(id);
            r.put("propTotal", total);
            r.put("propNormal", normal);
            r.put("childCount", ontologyMapper.countChildClassesOfClass(id));
            r.put("linkCount", ontologyMapper.countLinksOfClass(id));
            r.put("actionCount", ontologyMapper.countActionsOfClass(id));
            r.put("interfaceCount", ontologyMapper.countInterfacesOfClass(id));
            r.put("dsCount", categoryCode == null ? 0 : ontologyMapper.countDatasourcesByCategory(categoryCode));
            // 关联表名：暂以 datasource ds_code 列表填充（待 ont_class_ds 落地后改）
            List<Map<String, Object>> dsList = categoryCode == null ? Collections.emptyList()
                    : ontologyMapper.listDatasourcesByCategory(categoryCode);
            r.put("relatedTables", dsList.stream().map(x -> String.valueOf(x.get("ds_code"))).filter(s -> s != null && !"null".equals(s)).toList());
            // 父类暂无 parent_class_id 列，留占位
            r.put("parentLabel", null);
            r.put("parentApiName", null);
            // 领域中文路径：行业 / 领域
            String label = null;
            com.beiktech.bontolink.entity.BizCategory cat = categoryCode == null ? null : catByCode.get(categoryCode);
            if (cat != null) {
                String domainLabel = cat.getRdfsLabel() != null ? cat.getRdfsLabel() : cat.getCategoryCode();
                String industryLabel = null;
                com.beiktech.bontolink.entity.BizCategory cur = cat;
                while (cur.getParentId() != null && !"0".equals(cur.getParentId())) {
                    com.beiktech.bontolink.entity.BizCategory p = catById.get(cur.getParentId());
                    if (p == null) break;
                    cur = p;
                }
                if (cur != cat) industryLabel = cur.getRdfsLabel() != null ? cur.getRdfsLabel() : cur.getCategoryCode();
                label = industryLabel == null ? domainLabel : industryLabel + " / " + domainLabel;
            }
            r.put("categoryLabel", label);
        }
        return R.ok(rows);
    }

    /** 单个对象类型详情 + 聚合 */
    @GetMapping("/classes/{id}")
    public R<Map<String, Object>> classDetail(@PathVariable String id) {
        Map<String, Object> info = ontologyMapper.findClassById(id);
        if (info == null) return R.ok(new LinkedHashMap<>());
        info.put("properties", ontologyMapper.listProperties(id));
        info.put("links", ontologyMapper.listLinksOfClass(id));
        info.put("actions", ontologyMapper.listActionsOfClass(id));
        info.put("interfaces", ontologyMapper.listInterfacesOfClass(id));
        String categoryCode = (String) info.get("category_code");
        info.put("datasources", categoryCode == null ? Collections.emptyList()
                : ontologyMapper.listDatasourcesByCategory(categoryCode));
        // 对象类型挂接的物理数据集 (用于关系图画布): 解析 physical_fields JSON 为对象列表
        List<Map<String, Object>> classDs = ontologyMapper.listClassDatasources(id);
        for (Map<String, Object> row : classDs) {
            Object fields = row.get("physical_fields");
            if (fields instanceof String && !((String) fields).isBlank()) {
                try {
                    row.put("physical_fields", JSON.readValue((String) fields, new TypeReference<List<Map<String, Object>>>() {}));
                } catch (Exception ignored) {
                    row.put("physical_fields", Collections.emptyList());
                }
            } else if (fields == null) {
                row.put("physical_fields", Collections.emptyList());
            }
        }
        info.put("classDatasources", classDs);
        info.put("propTotal", ontologyMapper.countPropertiesOfClass(id));
        info.put("propNormal", ontologyMapper.countNormalPropertiesOfClass(id));
        info.put("linkCount", ontologyMapper.countLinksOfClass(id));
        info.put("actionCount", ontologyMapper.countActionsOfClass(id));
        info.put("interfaceCount", ontologyMapper.countInterfacesOfClass(id));
        return R.ok(info);
    }

    @GetMapping("/links")
    public R<List<Map<String, Object>>> links() { return R.ok(ontologyMapper.listLinks()); }

    @GetMapping("/actions")
    public R<List<Map<String, Object>>> actions() { return R.ok(ontologyMapper.listActions()); }

    @GetMapping("/interfaces")
    public R<List<Map<String, Object>>> interfaces() { return R.ok(ontologyMapper.listInterfaces()); }

    @GetMapping("/interfaces/{id}")
    public R<Map<String, Object>> interfaceDetail(@PathVariable String id) {
        Map<String, Object> r = new LinkedHashMap<>();
        Map<String, Object> info = ontologyMapper.findInterfaceById(id);
        if (info == null) return R.ok(r);
        r.putAll(info);
        r.put("properties", ontologyMapper.listInterfaceProperties(id));
        return R.ok(r);
    }

    @GetMapping("/classes/{id}/properties")
    public R<List<Map<String, Object>>> properties(@PathVariable String id) {
        return R.ok(ontologyMapper.listProperties(id));
    }

    /** 图谱：返回行业-领域-分组-对象-接口节点 + 边 */
    @GetMapping("/graph")
    public R<Map<String, Object>> graph() {
        Map<String, Object> g = new LinkedHashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        categoryMapper.listAll().forEach(c -> {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", c.getId());
            n.put("label", c.getRdfsLabel() == null ? c.getCategoryCode() : c.getRdfsLabel());
            n.put("kind", c.getCategoryType() == 1 ? "industry"
                       : c.getCategoryType() == 2 ? "domain" : "group");
            n.put("color", c.getColor());
            n.put("icon", c.getIcon());
            nodes.add(n);
            if (c.getParentId() != null && !"0".equals(c.getParentId())) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("source", c.getParentId());
                e.put("target", c.getId());
                e.put("kind", "hierarchy");
                edges.add(e);
            }
        });
        ontologyMapper.listClasses().forEach(c -> {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", c.get("id"));
            n.put("label", c.get("display_name") == null ? c.get("api_name") : c.get("display_name"));
            n.put("kind", "class");
            n.put("color", c.get("color"));
            n.put("icon", c.get("icon"));
            nodes.add(n);
        });
        ontologyMapper.listLinks().forEach(l -> {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("source", l.get("source_class_id"));
            e.put("target", l.get("target_class_id"));
            e.put("label", l.get("display_name"));
            e.put("kind", "link");
            edges.add(e);
        });
        g.put("nodes", nodes);
        g.put("edges", edges);
        return R.ok(g);
    }
}
