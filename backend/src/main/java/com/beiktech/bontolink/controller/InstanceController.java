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
    /** 返回所有对象类型，附加模拟实例数、属性数、链接数及行业/领域标签，供概览卡片与侧边导航使用。 */
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
    /**
     * 查询指定对象类型的实例列表。
     * 支持关键词 q、JSON 格式多条件 filter、字段排序(sort/order)、分页(page/size)。
     * 返回 {total, page, size, rows, columns}，columns 为表头定义。
     */
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
    /** 返回指定对象类型的列定义（系统列 + 属性列），供探索视图右侧字段面板使用。 */
    @GetMapping("/columns")
    public R<List<Map<String, Object>>> columnsApi(@RequestParam String classId) {
        return R.ok(columns(classId));
    }

    /* ============ 单条实例详情 + 关联对象类型 ============ */
    /** 返回单条实例详情，附带列定义(columns)和关联对象类型(links)；实例不存在时返回空对象。 */
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
    /** 返回与指定对象类型存在链接关系的相邻类型列表，含模拟链接条数；id 为可选实例 ID（影响随机种子）。 */
    @GetMapping("/links")
    public R<List<Map<String, Object>>> links(@RequestParam String classId,
                                              @RequestParam(required = false) String id) {
        return R.ok(relatedLinks(classId, id));
    }

    /* ============ 分组聚合(列表直方图 / 统计表) ============ */
    /**
     * 对指定对象类型按 groupBy 字段分组聚合，支持 count/sum/avg/min/max。
     * 返回 [{key, count, sum, avg, min, max, value}]，limit 控制最大分组数。
     */
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

    /**
     * 逐点经纬度(散点地图用):按 lngField/latField 取每个实例的经纬度,
     * 返回扁平数组 [{name, lng, lat, value:1}];经纬度缺失或非数值的行跳过。
     */
    @GetMapping("/geo-points")
    public R<List<Map<String, Object>>> geoPoints(
            @RequestParam String classId,
            @RequestParam String lngField,
            @RequestParam String latField,
            @RequestParam(required = false) String nameField,
            @RequestParam(required = false) String valueField,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String filter) {
        List<Map<String, Object>> rows = mock.query(classId, q, parseFilter(filter));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Double lng = numOrNull(r.get(lngField));
            Double lat = numOrNull(r.get(latField));
            if (lng == null || lat == null) continue;
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("name", (nameField != null && !nameField.isBlank()) ? r.get(nameField) : r.get("title"));
            o.put("lng", lng);
            o.put("lat", lat);
            // 有指定数值字段则取其值(散点大小随之变化),否则固定 1(仅打点)
            Double v = (valueField != null && !valueField.isBlank()) ? numOrNull(r.get(valueField)) : null;
            o.put("value", v != null ? v : 1);
            out.add(o);
        }
        return R.ok(out);
    }

    /** 数值或 null(非数值返回 null,便于跳过无坐标的行) */
    private static Double numOrNull(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return null; }
    }

    /* ============ 可视化制作(analysis-maker)图表数据:返回 [{name, value}] ============ */
    /**
     * 为可视化制作页面提供图表数据，输出长格式 [{name, value, x, y, colorField}]，前端按 colorField 自动分组为多序列。
     * 支持多指标(metrics JSON)、分组筛选(grouping JSON)、排序(sorts JSON)；
     * 三个 JSON 参数均缺省时回退到单序列旧行为(groupBy/metric/agg)。
     */
    @GetMapping("/chart-data")
    public R<List<Map<String, Object>>> chartData(
            @RequestParam String classId,
            @RequestParam String groupBy,
            @RequestParam(required = false) String metric,
            @RequestParam(required = false, defaultValue = "count") String agg,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String filter,
            // 数据源面板配置(P5):指标 / 分组与筛选 / 排序(JSON 字符串)
            @RequestParam(required = false) String metrics,
            @RequestParam(required = false) String grouping,
            @RequestParam(required = false) String sorts,
            @RequestParam(defaultValue = "12") int limit) {
        // 1) 解析面板配置(缺省时回退到 groupBy/metric/agg 旧行为)
        String gb = groupBy, mField = metric, mAgg = agg, sortAgg = agg;
        boolean sortDesc = true, includeOther = false;
        String groupMode = "include";
        Set<String> selected = null;

        // 序列规格:每个 (field, agg) 组合 = 一条序列(支持多指标/多聚合并排对比)
        List<String[]> specs = new ArrayList<>();   // [field, agg, fieldLabel]
        List<Map<String, Object>> metricList = parseList(metrics);
        Set<String> specSeen = new HashSet<>();
        for (Map<String, Object> m : metricList) {
            String f = m.get("field") == null ? "" : String.valueOf(m.get("field"));
            String lbl = m.get("label") == null ? "" : String.valueOf(m.get("label"));
            Object aggsO = m.get("aggs");
            // metrics 中每个指标可携带多个聚合函数，缺省为 count
            List<?> aggs = (aggsO instanceof List && !((List<?>) aggsO).isEmpty()) ? (List<?>) aggsO : List.of("count");
            for (Object a : aggs) {
                String ag = String.valueOf(a);
                // 空字段只对「计数」有意义(组大小);其余聚合需字段
                if (f.isBlank() && !"count".equals(ag)) continue;
                // specSeen 去重，防止同一 (field,agg) 被多个指标条目重复加入
                if (specSeen.add(f + "|" + ag)) specs.add(new String[]{f, ag, lbl});
            }
        }
        if (specs.isEmpty()) specs.add(new String[]{mField == null ? "" : mField, mAgg, ""});  // 旧行为:单序列
        // 排序基准 = 第一条序列的聚合
        sortAgg = specs.get(0)[1];

        // 分组与筛选
        Map<String, Object> gp = parseFilter(grouping);
        if (gp != null) {
            Object f = gp.get("field");
            if (f != null && !String.valueOf(f).isBlank()) gb = String.valueOf(f);
            if (gp.get("mode") != null) groupMode = String.valueOf(gp.get("mode"));
            includeOther = Boolean.TRUE.equals(gp.get("includeOther"));
            if (gp.get("selected") instanceof List) {
                selected = new HashSet<>();
                for (Object s : (List<?>) gp.get("selected")) selected.add(String.valueOf(s));
            }
        }
        // 排序:取第一项
        List<Map<String, Object>> sortList = parseList(sorts);
        if (!sortList.isEmpty()) {
            Map<String, Object> s0 = sortList.get(0);
            if (s0.get("agg") != null && !String.valueOf(s0.get("agg")).isBlank()) sortAgg = String.valueOf(s0.get("agg"));
            sortDesc = !Boolean.FALSE.equals(s0.get("desc"));
        }

        // 2) 每个序列字段各做一次全量分组聚合(每组含 count/sum/avg/min/max)
        List<Map<String, Object>> rows = mock.query(classId, q, parseFilter(filter));
        final String fgb = gb;   // lambda 需 effectively-final
        // aggByField: field -> (groupKey -> aggRow)；同字段多聚合只聚合一次，复用结果
        Map<String, Map<String, Map<String, Object>>> aggByField = new LinkedHashMap<>();  // field -> (key -> aggRow)
        Map<String, List<Map<String, Object>>> listByField = new LinkedHashMap<>();
        for (String[] s : specs) {
            // computeIfAbsent 保证同一字段只查一次
            aggByField.computeIfAbsent(s[0], f -> {
                List<Map<String, Object>> list = new ArrayList<>(mock.aggregate(rows, fgb, f, "count", 0));
                listByField.put(f, list);
                Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
                for (Map<String, Object> r : list) byKey.put(String.valueOf(r.get("key")), r);
                return byKey;
            });
        }

        // 3) 用第一条序列确定分组顺序:筛选 / 归其他 / 排序 / 截断
        String[] primary = specs.get(0);
        List<Map<String, Object>> prim = new ArrayList<>(listByField.get(primary[0]));
        List<Map<String, Object>> kept = new ArrayList<>(), rest = new ArrayList<>();
        if (selected != null && !selected.isEmpty()) {
            boolean exclude = "exclude".equals(groupMode);
            for (Map<String, Object> row : prim) {
                boolean hit = selected.contains(String.valueOf(row.get("key")));
                (((exclude) ? !hit : hit) ? kept : rest).add(row);
            }
        } else {
            kept = prim;
        }
        final boolean desc = sortDesc;
        final String pAgg = primary[1];
        kept.sort((x, y) -> {
            int c = Double.compare(toD(aggVal(x, pAgg)), toD(aggVal(y, pAgg)));
            return desc ? -c : c;
        });
        if (limit > 0 && kept.size() > limit) kept = new ArrayList<>(kept.subList(0, limit));
        List<String> orderedKeys = new ArrayList<>();
        for (Map<String, Object> row : kept) orderedKeys.add(String.valueOf(row.get("key")));
        boolean hasOther = includeOther && !rest.isEmpty();

        // 「其他」按每个字段分别合并被排除的分组
        Set<String> restKeys = new HashSet<>();
        for (Map<String, Object> r : rest) restKeys.add(String.valueOf(r.get("key")));
        Map<String, Map<String, Object>> otherByField = new HashMap<>();
        if (hasOther) {
            for (String f : aggByField.keySet()) {
                List<Map<String, Object>> fRest = new ArrayList<>();
                for (Map<String, Object> r : listByField.get(f)) {
                    if (restKeys.contains(String.valueOf(r.get("key")))) fRest.add(r);
                }
                otherByField.put(f, mergeOther(fRest, "count"));
            }
        }

        // 4) 序列标签:单序列保持「数量」;同字段多聚合用聚合名;多字段用「字段·聚合」
        boolean single = specs.size() == 1;
        boolean sameField = specs.stream().map(s -> s[0]).distinct().count() <= 1;

        // 5) 长格式输出:每组 × 每序列一行,colorField 区分序列(前端 autoSeries 据此分组)
        List<Map<String, Object>> out = new ArrayList<>();
        List<String> keys = new ArrayList<>(orderedKeys);
        if (hasOther) keys.add("其他");
        for (String key : keys) {
            for (String[] s : specs) {
                Map<String, Object> aggRow = "其他".equals(key)
                        ? otherByField.get(s[0])
                        : aggByField.get(s[0]).get(key);
                Object v = aggRow == null ? 0 : aggVal(aggRow, s[1]);
                Map<String, Object> o = new LinkedHashMap<>();
                o.put("name", key);
                o.put("value", v);
                o.put("x", key);
                o.put("y", v);
                o.put("colorField", seriesLabel(s, single, sameField));
                out.add(o);
            }
        }
        return R.ok(out);
    }

    /** 从聚合行取指定聚合值(count/sum/avg/min/max),缺省回退 count。 */
    private Object aggVal(Map<String, Object> aggRow, String agg) {
        if (aggRow == null) return 0;
        Object v = "count".equals(agg) ? aggRow.get("count") : aggRow.get(agg);
        return v != null ? v : aggRow.get("count");
    }

    private static final Map<String, String> AGG_LABEL = Map.of(
            "count", "计数", "sum", "总和", "avg", "平均值", "min", "最小值", "max", "最大值");

    /** 序列名:单序列「数量」;同字段多聚合用聚合名;多字段用「字段·聚合」。 */
    private String seriesLabel(String[] spec, boolean single, boolean sameField) {
        if (single) return "数量";
        String aggLbl = AGG_LABEL.getOrDefault(spec[1], spec[1]);
        if (sameField) return aggLbl;
        String fieldLbl = (spec[2] != null && !spec[2].isBlank()) ? spec[2] : spec[0];
        return fieldLbl + "·" + aggLbl;
    }

    /** 把多个分组合并为「其他」一条(count/sum 累加,min/max 取极值,avg=sum/count)。 */
    private Map<String, Object> mergeOther(List<Map<String, Object>> rest, String sortAgg) {
        double count = 0, sum = 0, min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (Map<String, Object> r : rest) {
            count += toD(r.get("count"));
            sum += toD(r.get("sum"));
            min = Math.min(min, toD(r.get("min")));
            max = Math.max(max, toD(r.get("max")));
        }
        double avg = count > 0 ? sum / count : 0;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", "其他");
        m.put("count", (int) count);
        m.put("sum", round2(sum));
        m.put("avg", round2(avg));
        m.put("min", min == Double.MAX_VALUE ? 0 : round2(min));
        m.put("max", max == -Double.MAX_VALUE ? 0 : round2(max));
        double v = switch (sortAgg == null ? "count" : sortAgg) {
            case "sum" -> sum; case "avg" -> avg; case "min" -> min; case "max" -> max; default -> count;
        };
        m.put("value", round2(v));
        return m;
    }

    private List<Map<String, Object>> parseList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return JSON.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static double toD(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0; }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /* ============ 单一统计(整组单值聚合) ============ */
    /** 对全量数据做整体聚合统计（不分组），返回 {count, sum, avg, min, max} 供指标卡片使用。 */
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
    /**
     * 二维交叉聚合：以 rowField 和 colField 作为两个维度，对 metric 字段做 agg 聚合。
     * 返回 {rowKeys, colKeys, cells} 供热力图/矩阵图渲染，limit 控制每维度取前 N 个值。
     */
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
    /**
     * 简版全局搜索：按关键词 q 匹配对象类型名称，并从每个对象类型取最多 perType 条实例样本。
     * 返回 {objectTypes, instances, counts}，实例命中总数上限 60 条，objectTypes 按实例数升序排列。
     */
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
    /**
     * 完整搜索结果页数据：支持搜索语法(AND/OR/引号短语)，同时匹配对象类型名称和实例字段。
     * 返回 {query, objectTypes, instanceGroups, groups(分类Facet), counts}；
     * instanceGroups 按命中数升序，每组附最多 sampleN 条样本摘要(id/title/desc)。
     */
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

    /** JSON 字符串 → filter Map；解析失败或空串返回 null（视为无筛选条件）。 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseFilter(String filter) {
        if (filter == null || filter.isBlank()) return null;
        try {
            return JSON.readValue(filter, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    /** 比较两个单元格值：数字按数值比，其余转字符串字典序；null 视为最小值。 */
    private static int compareCell(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        if (a instanceof Number && b instanceof Number) {
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    /** 将数据库/本体中的 data_type 字符串统一规范为前端识别的类型标识（string/int/decimal/datetime/date/time/boolean/enum）。 */
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

    /** 从多个候选值中返回第一个非空/非 blank 的字符串，全部为空则返回 ""。 */
    private static String firstNonBlank(Object... ss) {
        for (Object s : ss) if (s != null && !String.valueOf(s).isBlank()) return String.valueOf(s);
        return "";
    }
}
