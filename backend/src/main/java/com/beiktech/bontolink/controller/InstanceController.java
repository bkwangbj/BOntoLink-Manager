package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.entity.BizCategory;
import com.beiktech.bontolink.mapper.BizCategoryMapper;
import com.beiktech.bontolink.mapper.OntologyMapper;
import com.beiktech.bontolink.service.InstanceMockService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 实例探索 (Individual Discover) — 只读 API。
 * 数据来自 {@link InstanceMockService} 的内存模拟引擎(按对象类型属性确定性生成)。
 */
@RestController
@RequestMapping("/api/instance")
public class InstanceController {

    @Autowired private InstanceMockService mock;
    @Autowired private OntologyMapper ontologyMapper;
    @Autowired private BizCategoryMapper categoryMapper;

    private static final ObjectMapper JSON = new ObjectMapper();

    /* ============ 对象类型列表(概览卡片 / 侧边导航 / 对象类型页) ============ */
    @GetMapping("/object-types")
    public R<List<Map<String, Object>>> objectTypes() {
        Map<String, BizCategory> byCode = new HashMap<>();
        Map<String, BizCategory> byId = new HashMap<>();
        for (BizCategory c : categoryMapper.listAll()) {
            if (c.getCategoryCode() != null) byCode.put(c.getCategoryCode(), c);
            if (c.getId() != null) byId.put(c.getId(), c);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> c : ontologyMapper.listClasses()) {
            String id = (String) c.get("id");
            String categoryCode = (String) c.get("category_code");
            Map<String, Object> row = new LinkedHashMap<>(c);
            row.put("instanceCount", mock.count(id));
            row.put("propCount", ontologyMapper.countPropertiesOfClass(id));
            row.put("linkCount", ontologyMapper.countLinksOfClass(id));
            String[] labels = categoryLabels(categoryCode, byCode, byId);
            row.put("industryLabel", labels[0]);
            row.put("domainLabel", labels[1]);
            row.put("groupKey", labels[1] != null ? labels[1] : "未分组");
            out.add(row);
        }
        return R.ok(out);
    }

    /* ============ 实例列表(分页 + 关键词 + 多条件筛选 + 排序) ============ */
    @GetMapping("/list")
    public R<Map<String, Object>> list(
            @RequestParam String classId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "asc") String order,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int size) {

        Map<String, Object> filterMap = parseFilter(filter);
        List<Map<String, Object>> rows = mock.query(classId, q, filterMap);

        // 排序
        if (sort != null && !sort.isBlank()) {
            boolean desc = "desc".equalsIgnoreCase(order);
            rows = new ArrayList<>(rows);
            rows.sort((a, b) -> {
                Object va = a.get(sort), vb = b.get(sort);
                int cmp = compareCell(va, vb);
                return desc ? -cmp : cmp;
            });
        }

        int total = rows.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(total, from + size);
        List<Map<String, Object>> pageRows = from >= total ? Collections.emptyList() : rows.subList(from, to);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("total", total);
        resp.put("page", page);
        resp.put("size", size);
        resp.put("rows", new ArrayList<>(pageRows));
        resp.put("columns", columns(classId));
        return R.ok(resp);
    }

    /* ============ 属性列表(探索视图右侧字段面板) ============ */
    @GetMapping("/columns")
    public R<List<Map<String, Object>>> columnsApi(@RequestParam String classId) {
        return R.ok(columns(classId));
    }

    /* ============ 单条实例详情 + 关联对象类型 ============ */
    @GetMapping("/detail")
    public R<Map<String, Object>> detail(@RequestParam String classId, @RequestParam String id) {
        Map<String, Object> inst = mock.detail(classId, id);
        if (inst == null) return R.ok(new LinkedHashMap<>());
        Map<String, Object> resp = new LinkedHashMap<>(inst);
        resp.put("columns", columns(classId));
        resp.put("links", relatedLinks(classId, id));
        return R.ok(resp);
    }

    /* ============ 关联对象类型(探索视图左侧 Linked Object Types) ============ */
    @GetMapping("/links")
    public R<List<Map<String, Object>>> links(@RequestParam String classId,
                                              @RequestParam(required = false) String id) {
        return R.ok(relatedLinks(classId, id));
    }

    /* ============ 分组聚合(列表直方图 / 统计表) ============ */
    @GetMapping("/aggregate")
    public R<List<Map<String, Object>>> aggregate(
            @RequestParam String classId,
            @RequestParam String groupBy,
            @RequestParam(required = false) String metric,
            @RequestParam(required = false, defaultValue = "count") String agg,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "20") int limit) {
        List<Map<String, Object>> rows = mock.query(classId, q, parseFilter(filter));
        return R.ok(mock.aggregate(rows, groupBy, metric, agg, limit));
    }

    /* ============ 可视化制作(analysis-maker)图表数据:返回 [{name, value}] ============ */
    @GetMapping("/chart-data")
    public R<List<Map<String, Object>>> chartData(
            @RequestParam String classId,
            @RequestParam String groupBy,
            @RequestParam(required = false) String metric,
            @RequestParam(required = false, defaultValue = "count") String agg,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "12") int limit) {
        List<Map<String, Object>> rows = mock.query(classId, q, parseFilter(filter));
        List<Map<String, Object>> aggd = mock.aggregate(rows, groupBy, metric, agg, limit);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> g : aggd) {
            Object key = g.get("key");
            Object v = "count".equals(agg) ? g.get("count") : g.get(agg);
            if (v == null) v = g.get("count");
            Map<String, Object> o = new LinkedHashMap<>();
            // 多字段兼容:饼图用 name/value;柱/折线用 x/y(+colorField 单系列)
            o.put("name", key);
            o.put("value", v);
            o.put("x", key);
            o.put("y", v);
            o.put("colorField", "数量");
            out.add(o);
        }
        return R.ok(out);
    }

    /* ============ 单一统计(整组单值聚合) ============ */
    @GetMapping("/stat")
    public R<Map<String, Object>> stat(
            @RequestParam String classId,
            @RequestParam(required = false) String metric,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String filter) {
        List<Map<String, Object>> rows = mock.query(classId, q, parseFilter(filter));
        return R.ok(mock.overall(rows, metric));
    }

    /* ============ 网格图 / 热力图(二维交叉) ============ */
    @GetMapping("/matrix")
    public R<Map<String, Object>> matrix(
            @RequestParam String classId,
            @RequestParam String rowField,
            @RequestParam String colField,
            @RequestParam(required = false) String metric,
            @RequestParam(required = false, defaultValue = "count") String agg,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "8") int limit) {
        List<Map<String, Object>> rows = mock.query(classId, q, parseFilter(filter));
        return R.ok(mock.matrix(rows, rowField, colField, metric, agg, limit));
    }

    /* ============ 全局搜索(对象类型 + 实例样本) ============ */
    @GetMapping("/search")
    public R<Map<String, Object>> search(@RequestParam String q,
                                         @RequestParam(defaultValue = "8") int perType) {
        Map<String, Object> resp = new LinkedHashMap<>();
        String kw = q == null ? "" : q.trim().toLowerCase();

        List<Map<String, Object>> matchedTypes = new ArrayList<>();
        List<Map<String, Object>> instanceHits = new ArrayList<>();
        if (!kw.isBlank()) {
            for (Map<String, Object> c : ontologyMapper.listClasses()) {
                String id = (String) c.get("id");
                String dn = String.valueOf(c.getOrDefault("display_name", ""));
                String an = String.valueOf(c.getOrDefault("api_name", ""));
                if (dn.toLowerCase().contains(kw) || an.toLowerCase().contains(kw)) {
                    Map<String, Object> m = new LinkedHashMap<>(c);
                    m.put("instanceCount", mock.count(id));
                    matchedTypes.add(m);
                }
                // 实例样本命中
                List<Map<String, Object>> hits = mock.query(id, q, null);
                int take = Math.min(perType, hits.size());
                for (int i = 0; i < take; i++) instanceHits.add(hits.get(i));
            }
        }
        // 重要类型优先(实例数升序)：先少后多，对齐文档排序规则
        matchedTypes.sort(Comparator.comparingInt(m -> (int) m.getOrDefault("instanceCount", 0)));
        resp.put("objectTypes", matchedTypes);
        resp.put("instances", instanceHits.size() > 60 ? instanceHits.subList(0, 60) : instanceHits);
        resp.put("counts", Map.of("objectTypes", matchedTypes.size(), "instances", instanceHits.size()));
        return R.ok(resp);
    }

    /* ============ 搜索结果页面(1.1.1.2.4：多标签 + 分类导航 + 分组结果) ============ */
    @GetMapping("/search-results")
    public R<Map<String, Object>> searchResults(@RequestParam String q,
                                                @RequestParam(defaultValue = "4") int sampleN) {
        Map<String, BizCategory> byCode = new HashMap<>();
        Map<String, BizCategory> byId = new HashMap<>();
        for (BizCategory c : categoryMapper.listAll()) {
            if (c.getCategoryCode() != null) byCode.put(c.getCategoryCode(), c);
            if (c.getId() != null) byId.put(c.getId(), c);
        }

        List<Map<String, Object>> matchedTypes = new ArrayList<>();
        List<Map<String, Object>> instanceGroups = new ArrayList<>();
        Map<String, Integer> groupFacet = new LinkedHashMap<>();
        int totalInstanceHits = 0;

        for (Map<String, Object> c : ontologyMapper.listClasses()) {
            String id = (String) c.get("id");
            String name = firstNonBlank(c.get("display_name"), c.get("rdfs_label"), c.get("api_name"));
            String api = String.valueOf(c.getOrDefault("api_name", ""));
            String[] labels = categoryLabels((String) c.get("category_code"), byCode, byId);
            String domainLabel = labels[1] != null ? labels[1] : "未分组";

            // 对象类型名称命中(用搜索语法)
            if (mock.matchesText(q, name + " " + api)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", id); m.put("display_name", c.get("display_name")); m.put("api_name", api);
                m.put("icon", c.get("icon")); m.put("color", c.get("color"));
                m.put("instanceCount", mock.count(id));
                m.put("domainLabel", domainLabel);
                matchedTypes.add(m);
            }

            // 实例命中
            List<Map<String, Object>> hits = mock.query(id, q, null);
            if (!hits.isEmpty()) {
                totalInstanceHits += hits.size();
                groupFacet.merge(domainLabel, hits.size(), Integer::sum);
                List<Map<String, Object>> cols = columns(id);
                List<Map<String, Object>> samples = new ArrayList<>();
                int take = Math.min(sampleN, hits.size());
                for (int i = 0; i < take; i++) {
                    Map<String, Object> r = hits.get(i);
                    Map<String, Object> s = new LinkedHashMap<>();
                    s.put("id", r.get("id")); s.put("classId", id);
                    s.put("title", r.get("title")); s.put("code", r.get("code"));
                    s.put("icon", c.get("icon")); s.put("color", c.get("color"));
                    s.put("className", name);
                    s.put("desc", descOf(r, cols));
                    samples.add(s);
                }
                Map<String, Object> g = new LinkedHashMap<>();
                g.put("classId", id); g.put("className", name);
                g.put("icon", c.get("icon")); g.put("color", c.get("color"));
                g.put("domainLabel", domainLabel);
                g.put("hitCount", hits.size());
                g.put("samples", samples);
                instanceGroups.add(g);
            }
        }

        // 排序：按实例数升序(对齐文档——最少结果的类型先显示)
        instanceGroups.sort(Comparator.comparingInt(g -> (int) g.get("hitCount")));
        matchedTypes.sort(Comparator.comparingInt(m -> (int) m.getOrDefault("instanceCount", 0)));

        List<Map<String, Object>> groups = new ArrayList<>();
        for (Map.Entry<String, Integer> e : groupFacet.entrySet()) {
            groups.add(new LinkedHashMap<>(Map.of("key", e.getKey(), "count", e.getValue())));
        }
        groups.sort(Comparator.comparingInt(g -> (int) g.get("count")));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("query", q);
        resp.put("objectTypes", matchedTypes);
        resp.put("instanceGroups", instanceGroups);
        resp.put("groups", groups);
        resp.put("counts", Map.of(
                "all", totalInstanceHits + matchedTypes.size(),
                "instances", totalInstanceHits,
                "objectTypes", matchedTypes.size()));
        return R.ok(resp);
    }

    /** 取实例 1~2 个字符串属性拼出一句描述。 */
    private String descOf(Map<String, Object> row, List<Map<String, Object>> cols) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        String title = String.valueOf(row.get("title"));
        String code = String.valueOf(row.get("code"));
        for (Map<String, Object> col : cols) {
            if (n >= 2) break;
            if (!"string".equals(col.get("dataType"))) continue;
            String field = String.valueOf(col.get("field"));
            String label = String.valueOf(col.get("label"));
            Object v = row.get(field);
            if (v == null || String.valueOf(v).isBlank()) continue;
            String sv = String.valueOf(v);
            if (sv.equals(title) || sv.equals(code)) continue;   // 跳过名称/编码本身
            if (sb.length() > 0) sb.append(" · ");
            sb.append(label).append(": ").append(sv);
            n++;
        }
        return sb.toString();
    }

    /* ===================== 内部辅助 ===================== */

    /** 列定义：系统列(编码/名称) + 属性列。供表头 / 字段面板用。 */
    private List<Map<String, Object>> columns(String classId) {
        List<Map<String, Object>> cols = new ArrayList<>();
        for (Map<String, Object> p : mock.properties(classId)) {
            Map<String, Object> col = new LinkedHashMap<>();
            col.put("field", p.get("api_name"));
            col.put("label", firstNonBlank(p.get("display_name"), p.get("rdfs_label"), p.get("api_name")));
            col.put("dataType", normType(String.valueOf(p.getOrDefault("data_type", "string"))));
            col.put("propType", p.get("prop_type"));
            col.put("isPrimary", p.get("is_primary"));
            cols.add(col);
        }
        return cols;
    }

    /** 关联对象类型：基于 ont_class_link 取相邻类型，附带模拟链接条数。 */
    private List<Map<String, Object>> relatedLinks(String classId, String instanceId) {
        Map<String, Map<String, Object>> classById = new HashMap<>();
        for (Map<String, Object> c : ontologyMapper.listClasses()) classById.put((String) c.get("id"), c);

        List<Map<String, Object>> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> link : ontologyMapper.listLinksOfClass(classId)) {
            String src = (String) link.get("source_class_id");
            String tgt = (String) link.get("target_class_id");
            String other = classId.equals(src) ? tgt : src;
            if (other == null || other.equals(classId)) continue;
            Map<String, Object> oc = classById.get(other);
            if (oc == null) continue;
            String key = link.get("id") + ":" + other;
            if (!seen.add(key)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("linkId", link.get("id"));
            m.put("linkLabel", firstNonBlank(link.get("display_name"), link.get("api_name")));
            m.put("targetClassId", other);
            m.put("targetClassName", firstNonBlank(oc.get("display_name"), oc.get("api_name")));
            m.put("targetIcon", oc.get("icon"));
            m.put("targetColor", oc.get("color"));
            // 模拟链接条数(确定性)
            int seed = Math.abs(Objects.hash(classId, other, instanceId == null ? "" : instanceId));
            m.put("count", 1 + seed % 40);
            out.add(m);
        }
        return out;
    }

    /** 返回 [industryLabel, domainLabel]。 */
    private String[] categoryLabels(String categoryCode, Map<String, BizCategory> byCode, Map<String, BizCategory> byId) {
        if (categoryCode == null) return new String[]{ null, null };
        BizCategory cat = byCode.get(categoryCode);
        if (cat == null) return new String[]{ null, null };
        String domainLabel = cat.getRdfsLabel() != null ? cat.getRdfsLabel() : cat.getCategoryCode();
        BizCategory cur = cat;
        while (cur.getParentId() != null && !"0".equals(cur.getParentId())) {
            BizCategory p = byId.get(cur.getParentId());
            if (p == null) break;
            cur = p;
        }
        String industryLabel = cur != cat ? (cur.getRdfsLabel() != null ? cur.getRdfsLabel() : cur.getCategoryCode()) : null;
        return new String[]{ industryLabel, domainLabel };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseFilter(String filter) {
        if (filter == null || filter.isBlank()) return null;
        try {
            return JSON.readValue(filter, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private static int compareCell(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        if (a instanceof Number && b instanceof Number) {
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    private static String normType(String dt) {
        if (dt == null) return "string";
        String d = dt.toLowerCase();
        if (d.contains("datetime") || d.contains("timestamp")) return "datetime";
        if (d.equals("date")) return "date";
        if (d.equals("time")) return "time";
        if (d.contains("bool")) return "boolean";
        if (d.contains("enum")) return "enum";
        if (d.equals("int") || d.equals("integer") || d.equals("long") || d.equals("bigint")) return "int";
        if (d.contains("decimal") || d.contains("double") || d.contains("float") || d.equals("number") || d.contains("numeric")) return "decimal";
        return "string";
    }

    private static String firstNonBlank(Object... ss) {
        for (Object s : ss) if (s != null && !String.valueOf(s).isBlank()) return String.valueOf(s);
        return "";
    }
}
