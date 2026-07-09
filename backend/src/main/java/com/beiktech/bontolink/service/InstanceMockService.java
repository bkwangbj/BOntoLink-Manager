package com.beiktech.bontolink.service;

import com.beiktech.bontolink.mapper.OntologyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实例探索 — 内存模拟数据引擎。
 *
 * <p>没有真实"对象实例(业务数据行)"的存储表，本服务按 {@code ont_class} 的属性定义
 * <b>确定性</b>地生成实例池(每个对象类型几十~上百条)，缓存在内存中，
 * 支持列表 / 关键词搜索 / 多条件筛选 / 分组聚合 / 详情。重启后数据一致(种子固定)。</p>
 *
 * <p>每条实例 = 一个 {@code LinkedHashMap}，含 id / classId / className / code / title / rid
 * / createdAt 以及把所有属性平铺进去的 {@code props}(键为属性 api_name)。</p>
 */
@Service
public class InstanceMockService {

    @Autowired private OntologyMapper ontologyMapper;

    /** classId -> 已生成的实例列表(确定性，懒加载缓存) */
    private final Map<String, List<Map<String, Object>>> cache = new ConcurrentHashMap<>();
    /** classId -> 属性元数据(api_name/display_name/data_type/prop_type) */
    private final Map<String, List<Map<String, Object>>> propCache = new ConcurrentHashMap<>();
    /** classId -> 附加了关联对象字段的实例列表(供跨对象显示列/筛选,确定性联表) */
    private final Map<String, List<Map<String, Object>>> enrichedCache = new ConcurrentHashMap<>();

    /** 关联字段键前缀:rel::{关联对象类型id}::{关联属性api_name} */
    public static final String REL_PREFIX = "rel::";

    /* ——— 中文水利领域词库(用于生成像样的名称/取值) ——— */
    private static final String[] PROVINCES = {
            "黄河", "长江", "珠江", "海河", "淮河", "松花江", "辽河", "汉江",
            "湘江", "赣江", "嘉陵江", "雅砻江", "渭河", "汾河", "沂河", "钱塘江" };
    private static final String[] REGIONS = {
            "上游", "中游", "下游", "干流", "支流", "北岸", "南岸", "东段", "西段", "源头" };
    private static final String[] CITIES = {
            "郑州", "武汉", "南京", "广州", "济南", "成都", "西安", "兰州",
            "长沙", "南昌", "杭州", "合肥", "石家庄", "太原", "哈尔滨", "沈阳" };
    private static final String[] ADJ = {
            "示范", "重点", "中心", "综合", "试验", "应急", "标准化", "智慧", "生态", "在建" };
    private static final String[] ENUM_STATUS = { "正常", "预警", "停用", "检修", "新建" };
    private static final String[] ENUM_GRADE = { "一级", "二级", "三级", "四级", "五级" };
    private static final String[] ENUM_REGION = { "华北地区", "华东地区", "华中地区", "华南地区", "西南地区", "西北地区", "东北地区" };

    /* ===================== 公共 API ===================== */

    /** 取某对象类型的全部实例(确定性生成 + 缓存)。返回的是缓存引用，调用方不要直接改。 */
    public List<Map<String, Object>> all(String classId) {
        return cache.computeIfAbsent(classId, this::generate);
    }

    /** 某对象类型的实例总数。 */
    public int count(String classId) {
        return all(classId).size();
    }

    /**
     * 取某对象类型「附加了关联对象字段」的实例列表(确定性联表 + 缓存)。
     * <p>每条主实例按 id 确定性挑选一条关联实例,把其属性平铺进主行,键为
     * {@code rel::{关联对象类型id}::{关联属性api_name}}。用于关联属性作显示列/筛选。</p>
     */
    public List<Map<String, Object>> allEnriched(String classId) {
        return enrichedCache.computeIfAbsent(classId, this::enrich);
    }

    private List<Map<String, Object>> enrich(String classId) {
        List<Map<String, Object>> base = all(classId);
        // 相邻对象类型(出/入向去重,排除自身)
        LinkedHashSet<String> targets = new LinkedHashSet<>();
        for (Map<String, Object> link : ontologyMapper.listLinksOfClass(classId)) {
            String src = str(link.get("source_class_id"));
            String tgt = str(link.get("target_class_id"));
            String other = classId.equals(src) ? tgt : src;
            if (!other.isEmpty() && !other.equals(classId)) targets.add(other);
        }
        if (targets.isEmpty()) return base;   // 无关联:附加列表 == 基础列表
        // 预取关联对象的实例池 + 属性
        Map<String, List<Map<String, Object>>> relRows = new LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> relProps = new LinkedHashMap<>();
        for (String t : targets) { relRows.put(t, all(t)); relProps.put(t, properties(t)); }

        List<Map<String, Object>> out = new ArrayList<>(base.size());
        for (Map<String, Object> row : base) {
            Map<String, Object> e = new LinkedHashMap<>(row);
            for (String t : targets) {
                List<Map<String, Object>> rr = relRows.get(t);
                if (rr == null || rr.isEmpty()) continue;
                int idx = Math.floorMod(Objects.hash(row.get("id"), t), rr.size());
                Map<String, Object> rel = rr.get(idx);
                for (Map<String, Object> p : relProps.get(t)) {
                    String f = str(p.get("api_name"));
                    if (f.isEmpty()) continue;
                    e.put(REL_PREFIX + t + "::" + f, rel.get(f));
                }
            }
            out.add(e);
        }
        return out;
    }

    /** 属性元数据(api_name/display_name/data_type/...)。 */
    public List<Map<String, Object>> properties(String classId) {
        return propCache.computeIfAbsent(classId, cid -> {
            List<Map<String, Object>> list = new ArrayList<>(ontologyMapper.listProperties(cid));
            // 演示:给「水文测站」注入一个可正可负的度量,便于测试正负柱图(净蓄水/水位变化)
            if ("class-00000000-0000-0000-0000-000000000001".equals(cid)) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("api_name", "waterLevelDelta");
                p.put("display_name", "水位变化量");
                p.put("data_type", "decimal");
                list.add(p);
            }
            return list;
        });
    }

    /**
     * 过滤 + 关键词搜索 + 排序。
     *
     * @param classId 对象类型
     * @param q       关键词(空格拆词 OR 命中 title/code 及字符串属性)
     * @param filter  结构化筛选 {logic:'AND'|'OR', conditions:[{field,op,value,value2}]}，可为 null
     */
    public List<Map<String, Object>> query(String classId, String q, Map<String, Object> filter) {
        List<Map<String, Object>> rows = allEnriched(classId);   // 附加关联对象字段(跨对象显示列/筛选)
        BoolQuery.Node ast = BoolQuery.parse(q);   // 支持 引号短语 / AND·OR / 括号 / 通配符 ?·*

        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (ast != null && !ast.eval(rowText(row))) continue;
            if (filter != null && !evalNode(row, filter)) continue;
            out.add(row);
        }
        return out;
    }

    /** 一行的可搜索文本：所有字段值拼接并小写。 */
    private static String rowText(Map<String, Object> row) {
        StringBuilder sb = new StringBuilder();
        for (Object v : row.values()) if (v != null) sb.append(v).append(' ');
        return sb.toString().toLowerCase();
    }

    /** 用搜索语法(引号/AND·OR/括号/通配符)判断一段文本是否命中 q。 */
    public boolean matchesText(String q, String text) {
        BoolQuery.Node ast = BoolQuery.parse(q);
        return ast == null || ast.eval(text == null ? "" : text.toLowerCase());
    }

    /** 单条实例详情。 */
    public Map<String, Object> detail(String classId, String instanceId) {
        for (Map<String, Object> row : all(classId)) {
            if (instanceId.equals(row.get("id"))) return row;
        }
        return null;
    }

    /**
     * 分组聚合(列表直方图 / 统计表用)。按 groupBy 分组，对 metric 给出 count/sum/avg/min/max。
     *
     * @param sortAgg 排序依据 (count/sum/avg/min/max)；默认 count 降序
     * @return [{key, count, sum, avg, min, max, value}] 最多 limit 组(value=sortAgg 对应值，供柱状条用)
     */
    public List<Map<String, Object>> aggregate(List<Map<String, Object>> rows, String groupBy,
                                               String metric, String sortAgg, int limit) {
        boolean hasMetric = metric != null && !metric.isBlank();
        Map<String, int[]> cnt = new LinkedHashMap<>();           // key -> [count]
        Map<String, double[]> acc = new LinkedHashMap<>();        // key -> [sum, min, max, num]
        for (Map<String, Object> row : rows) {
            Object g = row.get(groupBy);
            String key = g == null || String.valueOf(g).isBlank() ? "(空)" : String.valueOf(g);
            cnt.computeIfAbsent(key, k -> new int[1])[0]++;
            if (hasMetric) {
                Double v = toDouble(row.get(metric));
                if (v != null) {
                    double[] a = acc.computeIfAbsent(key, k -> new double[]{ 0, Double.MAX_VALUE, -Double.MAX_VALUE, 0 });
                    a[0] += v; a[1] = Math.min(a[1], v); a[2] = Math.max(a[2], v); a[3]++;
                }
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, int[]> e : cnt.entrySet()) {
            String key = e.getKey();
            int c = e.getValue()[0];
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", key);
            m.put("count", c);
            double[] a = acc.get(key);
            if (a != null && a[3] > 0) {
                m.put("sum", round(a[0]));
                m.put("avg", round(a[0] / a[3]));
                m.put("min", round(a[1]));
                m.put("max", round(a[2]));
            } else {
                m.put("sum", 0); m.put("avg", 0); m.put("min", 0); m.put("max", 0);
            }
            m.put("value", sortValue(m, sortAgg));
            out.add(m);
        }
        String sa = sortAgg == null ? "count" : sortAgg;
        out.sort((x, y) -> Double.compare(toDouble(y.get("value")), toDouble(x.get("value"))));
        if (limit > 0 && out.size() > limit) return new ArrayList<>(out.subList(0, limit));
        return out;
    }

    /** 整组单值统计(单一统计图)。返回 {count, sum, avg, min, max, unique}。 */
    public Map<String, Object> overall(List<Map<String, Object>> rows, String metric) {
        boolean hasMetric = metric != null && !metric.isBlank();
        double sum = 0, min = Double.MAX_VALUE, max = -Double.MAX_VALUE; int num = 0;
        Set<String> uniq = new HashSet<>();
        for (Map<String, Object> row : rows) {
            if (hasMetric) {
                uniq.add(String.valueOf(row.get(metric)));
                Double v = toDouble(row.get(metric));
                if (v != null) { sum += v; min = Math.min(min, v); max = Math.max(max, v); num++; }
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("count", rows.size());
        m.put("sum", num > 0 ? round(sum) : 0);
        m.put("avg", num > 0 ? round(sum / num) : 0);
        m.put("min", num > 0 ? round(min) : 0);
        m.put("max", num > 0 ? round(max) : 0);
        m.put("unique", uniq.size());
        return m;
    }

    /** 二维网格(网格图/热力图)。rowField × colField 交叉，单元格为 metric 的 agg(无 metric 用 count)。 */
    public Map<String, Object> matrix(List<Map<String, Object>> rows, String rowField, String colField,
                                      String metric, String agg, int limit) {
        boolean hasMetric = metric != null && !metric.isBlank();
        // 单元累计
        Map<String, Map<String, double[]>> cell = new LinkedHashMap<>();  // r -> c -> [sum,min,max,count]
        Map<String, Integer> rowCnt = new LinkedHashMap<>(), colCnt = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String r = keyOf(row.get(rowField)), c = keyOf(row.get(colField));
            rowCnt.merge(r, 1, Integer::sum);
            colCnt.merge(c, 1, Integer::sum);
            double[] a = cell.computeIfAbsent(r, k -> new LinkedHashMap<>())
                    .computeIfAbsent(c, k -> new double[]{ 0, Double.MAX_VALUE, -Double.MAX_VALUE, 0 });
            a[3]++;
            if (hasMetric) {
                Double v = toDouble(row.get(metric));
                if (v != null) { a[0] += v; a[1] = Math.min(a[1], v); a[2] = Math.max(a[2], v); }
            }
        }
        List<String> rowKeys = topKeys(rowCnt, limit);
        List<String> colKeys = topKeys(colCnt, limit);
        List<Map<String, Object>> cells = new ArrayList<>();
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (String r : rowKeys) for (String c : colKeys) {
            double[] a = cell.getOrDefault(r, Collections.emptyMap()).get(c);
            double val = 0;
            if (a != null) val = cellAgg(a, hasMetric ? agg : "count");
            min = Math.min(min, val); max = Math.max(max, val);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("row", r); m.put("col", c); m.put("value", round(val));
            cells.add(m);
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("rows", rowKeys);
        resp.put("cols", colKeys);
        resp.put("cells", cells);
        resp.put("min", round(min == Double.MAX_VALUE ? 0 : min));
        resp.put("max", round(max == -Double.MAX_VALUE ? 0 : max));
        return resp;
    }

    /** 清缓存(对象类型/属性变更后可调；当前未挂触发器，保留接口)。 */
    public void evict(String classId) {
        cache.remove(classId);
        propCache.remove(classId);
        enrichedCache.clear();   // 关联字段可能来自任意类型,一律清空重建
    }

    /* ===================== 生成逻辑 ===================== */

    /**
     * 核心生成逻辑：按对象类型 classId 的属性定义确定性生成实例列表，结果写入 cache。
     * <p>实例数量由 classId.hashCode() 决定（18~107 条），保证同一类型每次重启结果一致。</p>
     */
    private List<Map<String, Object>> generate(String classId) {
        Map<String, Object> cls = ontologyMapper.findClassById(classId);
        if (cls == null) return Collections.emptyList();
        List<Map<String, Object>> props = properties(classId);

        String apiName = str(cls.get("api_name"));
        String displayName = firstNonBlank(str(cls.get("display_name")), str(cls.get("rdfs_label")), apiName);
        String icon = str(cls.get("icon"));
        String color = str(cls.get("color"));
        String categoryCode = str(cls.get("category_code"));

        // 命名属性 / 编码属性识别
        Map<String, Object> nameProp = pickNameProp(props);
        Map<String, Object> codeProp = pickCodeProp(props);
        String codePrefix = codePrefix(apiName);

        // 数量由 classId 哈希值决定，保证同类型结果稳定
        int n = 18 + Math.floorMod(classId.hashCode(), 90);   // 18..107 条
        List<Map<String, Object>> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            String code = codePrefix + "-" + pad(i + 1, 3);
            for (Map<String, Object> p : props) {
                String field = str(p.get("api_name"));
                if (field.isEmpty()) continue;
                Object val;
                if (p == codeProp) {
                    val = code;
                } else if (p == nameProp) {
                    val = genName(displayName, classId, i);
                } else {
                    val = genValue(p, classId, i);
                }
                row.put(field, val);
            }
            String title = nameProp != null ? str(row.get(str(nameProp.get("api_name"))))
                    : displayName + " " + code;
            if (title.isBlank()) title = displayName + " " + code;

            // id 格式: {apiName}-{序号}，rid 格式: ri.inst.{apiName}.{序号}
            row.put("id", apiName + "-" + (i + 1));
            row.put("classId", classId);
            row.put("className", displayName);
            row.put("classApiName", apiName);
            row.put("icon", icon);
            row.put("color", color);
            row.put("categoryCode", categoryCode);
            row.put("code", code);
            row.put("title", title);
            row.put("rid", "ri.inst." + apiName + "." + (i + 1));
            row.put("createdAt", genDate(classId, i, 0));
            list.add(row);
        }
        return list;
    }

    /**
     * 按属性元数据（data_type / api_name / display_name）生成确定性取值。
     * <p>随机数通过 {@link #seeded} 以 (classId, field, i) 为种子构造，保证同参数结果一致。</p>
     */
    private Object genValue(Map<String, Object> p, String classId, int i) {
        String field = str(p.get("api_name"));
        String display = str(p.get("display_name"));
        String dt = normType(str(p.get("data_type")));
        Random r = seeded(classId, field, i);

        switch (dt) {
            case "int":
                return intByName(field, display, r);
            case "decimal":
                return decimalByName(field, display, r);
            case "date":
                return genDate(classId, i, field.hashCode());
            case "datetime":
                return genDate(classId, i, field.hashCode()) + " " + pad(r.nextInt(24), 2) + ":" + pad(r.nextInt(60), 2) + ":" + pad(r.nextInt(60), 2);
            case "time":
                return pad(r.nextInt(24), 2) + ":" + pad(r.nextInt(60), 2) + ":" + pad(r.nextInt(60), 2);
            case "boolean":
                return r.nextBoolean();
            case "enum":
                return enumByName(field, display, r);
            default:
                return stringByName(field, display, classId, i, r);
        }
    }

    private Object intByName(String field, String display, Random r) {
        String s = (field + display).toLowerCase();
        if (s.contains("year") || display.contains("年")) return 1990 + r.nextInt(36);
        if (s.contains("count") || display.contains("数") || display.contains("数量")) return r.nextInt(500);
        if (s.contains("age") || display.contains("龄")) return r.nextInt(80);
        return r.nextInt(1000);
    }

    /**
     * 按字段语义生成合理范围的浮点数（水利领域物理量范围）。
     * 经度 73~135、纬度 18~53（覆盖中国版图），其余按物理量量级约定。
     */
    private Object decimalByName(String field, String display, Random r) {
        String s = (field + display).toLowerCase();
        double v;
        // 变化/增减/净值/差值类度量:生成可正可负的值(供正负柱图演示)
        if (display.contains("变化") || display.contains("增减") || display.contains("净") || display.contains("差")
                || s.contains("delta") || s.contains("change")) {
            v = r.nextDouble() * 100 - 50;   // -50 ~ +50
            return Math.round(v * 100.0) / 100.0;
        }
        if (s.contains("lng") || s.contains("longitude") || display.contains("经度")) v = 73 + r.nextDouble() * 62;        // 73~135
        else if (s.contains("lat") || s.contains("latitude") || display.contains("纬度")) v = 18 + r.nextDouble() * 35;   // 18~53
        else if (display.contains("水位") || s.contains("level")) v = r.nextDouble() * 200;
        else if (display.contains("流量") || s.contains("flow")) v = r.nextDouble() * 10000;
        else if (display.contains("面积") || s.contains("area")) v = r.nextDouble() * 5000;
        else if (display.contains("库容") || display.contains("容量") || s.contains("capacity")) v = r.nextDouble() * 100000;
        else if (display.contains("率") || display.contains("比") || s.contains("rate") || s.contains("ratio")) v = r.nextDouble() * 100;
        else if (display.contains("温度") || s.contains("temp")) v = r.nextDouble() * 40;
        else if (display.contains("长") || display.contains("距离") || s.contains("length") || s.contains("distance")) v = r.nextDouble() * 2000;
        else v = r.nextDouble() * 1000;
        return Math.round(v * 100.0) / 100.0;
    }

    private String enumByName(String field, String display, Random r) {
        String s = field + display;
        if (s.contains("状态") || s.toLowerCase().contains("status") || s.contains("state")) return ENUM_STATUS[r.nextInt(ENUM_STATUS.length)];
        if (s.contains("等级") || s.contains("级别") || s.toLowerCase().contains("grade") || s.toLowerCase().contains("level")) return ENUM_GRADE[r.nextInt(ENUM_GRADE.length)];
        if (s.contains("地区") || s.contains("区域") || s.toLowerCase().contains("region")) return ENUM_REGION[r.nextInt(ENUM_REGION.length)];
        return ENUM_STATUS[r.nextInt(ENUM_STATUS.length)];
    }

    /**
     * 对 string 类型属性按字段语义选取合适的水利领域词库词汇。
     * 无法匹配语义时回退为"河流+方位"组合（如"黄河上游"）。
     */
    private String stringByName(String field, String display, String classId, int i, Random r) {
        String s = field + display;
        if (display.contains("状态") || s.toLowerCase().contains("status")) return ENUM_STATUS[r.nextInt(ENUM_STATUS.length)];
        if (display.contains("等级") || display.contains("级别")) return ENUM_GRADE[r.nextInt(ENUM_GRADE.length)];
        if (display.contains("地区") || display.contains("区域") || display.contains("省")) return ENUM_REGION[r.nextInt(ENUM_REGION.length)];
        if (display.contains("河流") || display.contains("水系") || s.toLowerCase().contains("river")) return PROVINCES[r.nextInt(PROVINCES.length)];
        if (display.contains("城市") || display.contains("地点") || display.contains("位置")) return CITIES[r.nextInt(CITIES.length)];
        if (display.contains("编码") || display.contains("编号") || s.toLowerCase().contains("code") || s.toLowerCase().contains("no")) return codePrefix(field).toUpperCase() + pad(r.nextInt(100000), 5);
        if (display.contains("名称") || s.toLowerCase().contains("name")) return genName(display, classId, i);
        // 通用：地名 + 方位修饰，如"黄河上游"
        return PROVINCES[r.nextInt(PROVINCES.length)] + REGIONS[r.nextInt(REGIONS.length)];
    }

    /**
     * 生成中文名称，格式：{河流}{修饰词}{类名(最多6字)}NN号，如"黄河示范水库03号"。
     * classDisplay 括号及括号内内容先剥离，避免名称过长。
     */
    private String genName(String classDisplay, String classId, int i) {
        Random r = seeded(classId, "__name__", i);
        String base = classDisplay.replaceAll("[（(].*", "").trim();
        if (base.length() > 6) base = base.substring(0, 6);
        return PROVINCES[r.nextInt(PROVINCES.length)] + ADJ[r.nextInt(ADJ.length)] + base + pad(i + 1, 2) + "号";
    }

    /**
     * 生成格式为 yyyy-MM-dd 的确定性日期（2018~2025 年）。
     * salt 用于区分同一实例不同日期属性，避免取值完全相同。
     */
    private String genDate(String classId, int i, int salt) {
        Random r = seeded(classId, "__date__" + salt, i);
        int year = 2018 + r.nextInt(8);     // 2018~2025
        int month = 1 + r.nextInt(12);
        int day = 1 + r.nextInt(28);        // 最大取28，规避月份天数差异
        return year + "-" + pad(month, 2) + "-" + pad(day, 2);
    }

    /* ===================== 筛选求值 ===================== */

    /**
     * 布尔搜索语法解析器：支持
     * <ul>
     *   <li>默认分词 = 任一命中(OR)：<code>大坝 防渗</code></li>
     *   <li>引号短语精确：<code>"大坝 防渗"</code></li>
     *   <li>逻辑符 AND / OR(AND 优先级高于 OR)</li>
     *   <li>括号分组：<code>("大坝 防渗" OR "渠道 衬砌") AND 南水北调</code></li>
     *   <li>通配符 <code>?</code>=单字符、<code>*</code>=任意长度</li>
     * </ul>
     */
    static final class BoolQuery {
        interface Node { boolean eval(String text); }
        enum K { AND, OR, LP, RP, TERM, PHRASE }
        record Tok(K k, String s) {}

        static final Node ALWAYS = t -> true;

        static Node parse(String q) {
            if (q == null || q.isBlank()) return null;
            List<Tok> toks = lex(q);
            if (toks.isEmpty()) return null;
            int[] i = {0};
            return orExpr(toks, i);
        }

        private static List<Tok> lex(String q) {
            List<Tok> out = new ArrayList<>();
            int n = q.length();
            for (int i = 0; i < n; ) {
                char c = q.charAt(i);
                if (Character.isWhitespace(c)) { i++; continue; }
                if (c == '(' || c == '（') { out.add(new Tok(K.LP, "(")); i++; continue; }
                if (c == ')' || c == '）') { out.add(new Tok(K.RP, ")")); i++; continue; }
                if (isQuote(c)) {
                    int j = i + 1; StringBuilder sb = new StringBuilder();
                    while (j < n && !isQuote(q.charAt(j))) sb.append(q.charAt(j++));
                    out.add(new Tok(K.PHRASE, sb.toString()));
                    i = (j < n) ? j + 1 : j;
                    continue;
                }
                int j = i; StringBuilder sb = new StringBuilder();
                while (j < n) {
                    char d = q.charAt(j);
                    if (Character.isWhitespace(d) || d == '(' || d == '）' || d == '（' || d == ')' || isQuote(d)) break;
                    sb.append(d); j++;
                }
                String w = sb.toString();
                if (w.equalsIgnoreCase("AND")) out.add(new Tok(K.AND, w));
                else if (w.equalsIgnoreCase("OR")) out.add(new Tok(K.OR, w));
                else out.add(new Tok(K.TERM, w));
                i = j;
            }
            return out;
        }

        private static boolean isQuote(char c) { return c == '"' || c == '“' || c == '”'; }

        private static Node orExpr(List<Tok> t, int[] i) {
            Node left = andExpr(t, i);
            while (i[0] < t.size()) {
                K k = t.get(i[0]).k();
                if (k == K.OR) { i[0]++; left = or(left, andExpr(t, i)); }
                else if (k == K.TERM || k == K.PHRASE || k == K.LP) { left = or(left, andExpr(t, i)); } // 隐式 OR
                else break;
            }
            return left;
        }
        private static Node andExpr(List<Tok> t, int[] i) {
            Node left = unary(t, i);
            while (i[0] < t.size() && t.get(i[0]).k() == K.AND) { i[0]++; left = and(left, unary(t, i)); }
            return left;
        }
        private static Node unary(List<Tok> t, int[] i) {
            if (i[0] >= t.size()) return ALWAYS;
            Tok tk = t.get(i[0]);
            if (tk.k() == K.LP) {
                i[0]++; Node n = orExpr(t, i);
                if (i[0] < t.size() && t.get(i[0]).k() == K.RP) i[0]++;
                return n;
            }
            if (tk.k() == K.TERM) { i[0]++; return term(tk.s(), false); }
            if (tk.k() == K.PHRASE) { i[0]++; return term(tk.s(), true); }
            i[0]++; return ALWAYS;
        }

        private static Node and(Node a, Node b) { return t -> a.eval(t) && b.eval(t); }
        private static Node or(Node a, Node b)  { return t -> a.eval(t) || b.eval(t); }

        /**
         * 构造单词/短语匹配节点。
         * 非短语且含通配符时，将 {@code ?/?} 转为 {@code .}、{@code *} 转为 {@code .*}，
         * 其余字符用 Pattern.quote 转义，编译为正则进行子串匹配（find，非全匹配）。
         */
        private static Node term(String w, boolean phrase) {
            String s = w.toLowerCase();
            if (!phrase && (s.indexOf('?') >= 0 || s.indexOf('*') >= 0 || s.indexOf('？') >= 0)) {
                // 通配符模式：? → 单字符, * → 任意长度；其余字面量字符转义防止正则注入
                StringBuilder rx = new StringBuilder();
                for (int k = 0; k < s.length(); k++) {
                    char c = s.charAt(k);
                    if (c == '?' || c == '？') rx.append('.');
                    else if (c == '*') rx.append(".*");
                    else rx.append(java.util.regex.Pattern.quote(String.valueOf(c)));
                }
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(rx.toString());
                return t -> p.matcher(t).find();
            }
            return t -> t.contains(s);
        }
    }

    /**
     * 递归求值一个筛选节点：
     * <ul>
     *   <li>分组节点：含 {@code conditions} → 用 {@code logic}(AND/OR) 组合各子节点;</li>
     *   <li>叶子节点：含 {@code field}+{@code op} → 直接对单元格求值。</li>
     * </ul>
     * 这样前端"每属性一个条件组、组内多条件与/或、组间 AND"的结构可被忠实求值。
     */
    private boolean evalNode(Map<String, Object> row, Map<String, Object> node) {
        if (node.containsKey("conditions")) {
            List<Map<String, Object>> subs = filterConditions(node);
            if (subs.isEmpty()) return true;
            boolean and = !"OR".equalsIgnoreCase(filterLogic(node));
            boolean acc = and;
            for (Map<String, Object> s : subs) {
                boolean ok = evalNode(row, s);
                acc = and ? (acc && ok) : (acc || ok);
            }
            return acc;
        }
        return evalCond(row, node);
    }

    @SuppressWarnings("unchecked")
    private boolean evalCond(Map<String, Object> row, Map<String, Object> c) {
        String field = str(c.get("field"));
        String op = str(c.get("op"));
        if (field.isEmpty() || op.isEmpty()) return true;
        Object cell = row.get(field);
        String sv = cell == null ? "" : String.valueOf(cell);
        String val = c.get("value") == null ? "" : String.valueOf(c.get("value"));
        Double cellNum = toDouble(cell);
        Double valNum = toDouble(c.get("value"));
        Double val2Num = toDouble(c.get("value2"));

        switch (op) {
            case "eq":          return sv.equals(val);
            case "ne":          return !sv.equals(val);
            case "contains":    return sv.toLowerCase().contains(val.toLowerCase());
            case "notContains": return !sv.toLowerCase().contains(val.toLowerCase());
            case "startsWith":  return sv.startsWith(val);
            case "notStartsWith": return !sv.startsWith(val);
            case "endsWith":    return sv.endsWith(val);
            case "notEndsWith": return !sv.endsWith(val);
            case "empty":       return sv.isBlank();
            case "notEmpty":    return !sv.isBlank();
            case "gt":          return cellNum != null && valNum != null && cellNum > valNum;
            case "lt":          return cellNum != null && valNum != null && cellNum < valNum;
            case "gte":         return cellNum != null && valNum != null && cellNum >= valNum;
            case "lte":         return cellNum != null && valNum != null && cellNum <= valNum;
            case "after":       return sv.compareTo(val) > 0;
            case "before":      return sv.compareTo(val) < 0;
            case "between":
                // 数值时用数值比较，否则字典序比较（适用于日期字符串 yyyy-MM-dd）
                if (cellNum != null && valNum != null && val2Num != null) return cellNum >= valNum && cellNum <= val2Num;
                return sv.compareTo(val) >= 0 && sv.compareTo(String.valueOf(c.get("value2"))) <= 0;
            case "in": {
                // value 可能是 List（前端直传数组）或逗号分隔字符串，两种格式兼容
                Object raw = c.get("value");
                if (raw instanceof List<?> arr) return arr.stream().anyMatch(x -> sv.equals(String.valueOf(x)));
                return Arrays.asList(val.split("\\s*,\\s*")).contains(sv);
            }
            case "notIn": {
                Object raw = c.get("value");
                if (raw instanceof List<?> arr) return arr.stream().noneMatch(x -> sv.equals(String.valueOf(x)));
                return !Arrays.asList(val.split("\\s*,\\s*")).contains(sv);
            }
            default: return true;
        }
    }

    /* ===================== 工具 ===================== */

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> filterConditions(Map<String, Object> filter) {
        if (filter == null) return Collections.emptyList();
        Object c = filter.get("conditions");
        if (c instanceof List<?> l) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object o : l) if (o instanceof Map<?, ?> m) out.add((Map<String, Object>) m);
            return out;
        }
        return Collections.emptyList();
    }

    private String filterLogic(Map<String, Object> filter) {
        if (filter == null) return "AND";
        Object l = filter.get("logic");
        return l == null ? "AND" : String.valueOf(l);
    }


    /**
     * 从属性列表中识别"名称属性"：api_name 以 name 结尾或 display_name 含"名称"，且为 string 类型。
     * 找到的第一个属性用于生成实例 title。
     */
    private Map<String, Object> pickNameProp(List<Map<String, Object>> props) {
        for (Map<String, Object> p : props) {
            String a = str(p.get("api_name")).toLowerCase();
            String d = str(p.get("display_name"));
            if ((a.endsWith("name") || d.contains("名称")) && isString(p)) return p;
        }
        return null;
    }

    /**
     * 从属性列表中识别"编码属性"：api_name 以 code 结尾、display_name 含"编码"，或标记了 is_primary，且为 string 类型。
     * 找到的第一个属性的值固定填充为 {codePrefix}-NNN 格式编码。
     */
    private Map<String, Object> pickCodeProp(List<Map<String, Object>> props) {
        for (Map<String, Object> p : props) {
            String a = str(p.get("api_name")).toLowerCase();
            String d = str(p.get("display_name"));
            if ((a.endsWith("code") || d.contains("编码") || Boolean.TRUE.equals(asBool(p.get("is_primary")))) && isString(p)) return p;
        }
        return null;
    }

    private boolean isString(Map<String, Object> p) {
        String dt = normType(str(p.get("data_type")));
        return dt.equals("string");
    }

    /**
     * 将数据库中的 data_type 字符串统一归一化为内部枚举值：
     * string / int / decimal / date / datetime / time / boolean / enum。
     * 未能匹配的类型一律归为 string。
     */
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

    /**
     * 从 apiName 中提取大写字母作为编码前缀（最多4位，如 WaterGate → WG）。
     * 全小写时取首字母大写。apiName 为空时返回 "OBJ"。
     */
    private static String codePrefix(String apiName) {
        if (apiName == null || apiName.isEmpty()) return "OBJ";
        StringBuilder sb = new StringBuilder();
        for (char ch : apiName.toCharArray()) {
            if (Character.isUpperCase(ch)) sb.append(ch);
        }
        if (sb.length() == 0) sb.append(Character.toUpperCase(apiName.charAt(0)));
        return sb.length() > 4 ? sb.substring(0, 4) : sb.toString();
    }

    private static Random seeded(String classId, String field, int i) {
        // 注意：Java 的 Random 对相邻种子首个 nextDouble 输出高度相关，
        // 直接用 Objects.hash 会让连续实例取值雷同 —— 先做 splitmix64 混淆解耦。
        long h = ((long) Objects.hash(classId, field) << 20) ^ (i * 0x9E3779B97F4A7C15L);
        h ^= (h >>> 33); h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33); h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return new Random(h);
    }

    private static Double toDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return null; }
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }

    private static double sortValue(Map<String, Object> m, String agg) {
        String a = agg == null ? "count" : agg;
        return toDouble(m.getOrDefault(a, m.get("count")));
    }

    private static String keyOf(Object o) {
        return o == null || String.valueOf(o).isBlank() ? "(空)" : String.valueOf(o);
    }

    /** 按出现次数取前 N 个键(降序)。 */
    private static List<String> topKeys(Map<String, Integer> cnt, int limit) {
        List<Map.Entry<String, Integer>> es = new ArrayList<>(cnt.entrySet());
        es.sort((a, b) -> b.getValue() - a.getValue());
        List<String> out = new ArrayList<>();
        int n = limit > 0 ? Math.min(limit, es.size()) : es.size();
        for (int i = 0; i < n; i++) out.add(es.get(i).getKey());
        return out;
    }

    /** cell = [sum, min, max, count] → 按 agg 取值。 */
    private static double cellAgg(double[] a, String agg) {
        return switch (agg == null ? "count" : agg) {
            case "sum" -> a[0];
            case "avg" -> a[3] > 0 ? a[0] / a[3] : 0;
            case "min" -> a[1] == Double.MAX_VALUE ? 0 : a[1];
            case "max" -> a[2] == -Double.MAX_VALUE ? 0 : a[2];
            default -> a[3];
        };
    }

    private static Boolean asBool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.intValue() != 0;
        return "1".equals(String.valueOf(o)) || "true".equalsIgnoreCase(String.valueOf(o));
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }

    private static String firstNonBlank(String... ss) {
        for (String s : ss) if (s != null && !s.isBlank()) return s;
        return "";
    }

    private static String pad(int v, int width) {
        String s = String.valueOf(v);
        while (s.length() < width) s = "0" + s;
        return s;
    }
}
