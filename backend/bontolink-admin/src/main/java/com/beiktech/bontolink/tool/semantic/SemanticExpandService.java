package com.beiktech.bontolink.tool.semantic;

import com.beiktech.bontolink.tool.jena.JenaToolService;
import com.beiktech.bontolink.tool.vector.VectorToolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语义扩展服务：向量搜索 → Jena 本体结构联动
 *
 * Pipeline:
 *   1. 向量搜索（pgvector 或 Milvus）
 *   2. 每条命中 → DB 反查 api_name → Jena 拉完整本体结构
 *   3. 无命中 → 降级到 Jena 关键词检索
 */
@Slf4j
@Service
public class SemanticExpandService {

    private final VectorToolService vectorToolService;
    private final JenaToolService jenaToolService;
    private final JdbcTemplate jdbcTemplate;

    /** 枚举项缓存: enumApiName → List<Map> (从 Jena 查询的 Individual 列表) */
    private final Map<String, List<Map<String, Object>>> enumItemsCache = new ConcurrentHashMap<>();

    public SemanticExpandService(VectorToolService vectorToolService,
                                 JenaToolService jenaToolService,
                                 JdbcTemplate jdbcTemplate) {
        this.vectorToolService = vectorToolService;
        this.jenaToolService = jenaToolService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 语义扩展主入口。
     *
     * @param question  用户输入的自然语言问题
     * @param nsCode    可选命名空间过滤
     * @param topK      向量搜索条数（<=0 用默认）
     * @param threshold 相似度阈值（<=0 用配置默认）
     */
    public Map<String, Object> expand(String question, String nsCode, int topK, double threshold) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", question);

        // Step 1: 向量搜索
        Map<String, Object> vectorResult = vectorToolService.search(question, topK, threshold, nsCode);
        boolean vectorOk = Boolean.TRUE.equals(vectorResult.get("success"));
        // 区分"未启用"和"连接失败"
        String vectorMsg = vectorResult.get("message") != null ? String.valueOf(vectorResult.get("message")) : null;
        boolean vectorError = !vectorOk && vectorMsg != null
                && !vectorMsg.contains("未启用") && !vectorMsg.contains("非 PostgreSQL");
        result.put("vectorAvailable", vectorOk);
        if (vectorError) result.put("vectorError", "向量库连接异常，已自动降级");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matches = vectorOk
                ? (List<Map<String, Object>>) vectorResult.getOrDefault("matches", Collections.emptyList())
                : Collections.emptyList();

        if (!matches.isEmpty()) {
            // Step 2: 命中 → Jena 结构扩展
            List<Map<String, Object>> entities = new ArrayList<>();
            for (Map<String, Object> match : matches) {
                Map<String, Object> entity = expandMatch(match);
                if (entity != null) entities.add(entity);
            }
            result.put("matched", !entities.isEmpty());
            result.put("entities", entities);
            result.put("fallback", null);
        } else {
            // Step 3: 无命中 → fallback
            result.put("matched", false);
            result.put("entities", Collections.emptyList());
            result.put("fallback", buildFallback(question, vectorResult));
        }
        result.put("success", true);
        return result;
    }

    private Map<String, Object> expandMatch(Map<String, Object> match) {
        // 兼容 pgvector（class_id）和 Milvus（entity_id）
        String entityId = match.get("entity_id") != null
                ? String.valueOf(match.get("entity_id"))
                : match.get("class_id") != null ? String.valueOf(match.get("class_id")) : null;
        if (entityId == null) return null;

        String entityType = match.get("entity_type") != null
                ? String.valueOf(match.get("entity_type"))
                : inferTypeFromId(entityId);

        double similarity = match.get("similarity") instanceof Number n ? n.doubleValue() : 0.0;
        String sourceText = match.get("source_text") != null ? String.valueOf(match.get("source_text")) : null;
        String nsCode = match.get("ns_code") != null ? String.valueOf(match.get("ns_code")) : null;

        // 优先使用向量库直接存储的 api_name，避免 DB 反查
        String apiName = match.get("api_name") instanceof String s && !s.isBlank()
                ? s : lookupApiName(entityType, entityId);

        Map<String, Object> entity = new LinkedHashMap<>();
        entity.put("entityId", entityId);
        entity.put("similarity", similarity);
        entity.put("sourceText", sourceText);
        entity.put("nsCode", nsCode);
        entity.put("apiName", apiName);

        if (apiName != null && !"enum_item".equalsIgnoreCase(entityType)) {
            Map<String, Object> ontology = fetchOntology(entityType, apiName);
            // 从 ontology 中提取 label 方便前端展示
            if (ontology.get("label") != null) entity.put("label", ontology.get("label"));
            entity.put("ontology", ontology);
        } else {
            entity.put("label", sourceText);
            entity.put("ontology", null);
            if (apiName == null) {
                entity.put("ontologyNote", "DB 中未找到 api_name，Jena 扩展跳过");
            } else {
                entity.put("ontologyNote", "enum_item 不做 Jena 类查询");
            }
        }
        return entity;
    }

    /** 根据 ID 前缀推断实体类型（仅 pgvector 模式需要） */
    private String inferTypeFromId(String id) {
        if (id == null) return "class";
        String lower = id.toLowerCase();
        if (lower.startsWith("class-")) return "class";
        if (lower.startsWith("shared-properties-")) return "shared_property";
        if (lower.startsWith("enum-")) return "enum";
        if (lower.startsWith("link-types-")) return "link_type";
        return "class";
    }

    /** entity_id → api_name（DB 反查）。注意 entity_type 与向量化时写入的值保持一致 */
    private String lookupApiName(String entityType, String entityId) {
        try {
            return switch (entityType.toLowerCase()) {
                case "class" -> queryOne("SELECT api_name FROM ont_class WHERE id = ?", entityId);
                // Jena 注册属性为 "类api_name.属性api_name"，需联表拼接
                case "class_prop", "property" -> {
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                            "SELECT p.api_name AS prop_api, c.api_name AS class_api" +
                            " FROM ont_class_property p JOIN ont_class c ON p.class_id = c.id" +
                            " WHERE p.id = ?", entityId);
                    if (!rows.isEmpty()) {
                        Object ca = rows.get(0).get("class_api");
                        Object pa = rows.get(0).get("prop_api");
                        yield (ca != null && pa != null) ? ca + "." + pa : null;
                    }
                    yield null;
                }
                case "shared_prop", "shared_property" ->
                        queryOne("SELECT api_name FROM ont_shared_properties WHERE id = ?", entityId);
                case "enum" -> queryOne("SELECT api_name FROM ont_enum_types WHERE id = ?", entityId);
                case "enum_item" -> {
                    // 枚举项 → 取父枚举 api_name（Jena 按枚举类检索才有意义）
                    String enumId = queryOne("SELECT enum_id FROM ont_enum_items WHERE id = ?", entityId);
                    yield enumId != null ? queryOne("SELECT api_name FROM ont_enum_types WHERE id = ?", enumId) : null;
                }
                case "link_type" -> queryOne("SELECT api_name FROM ont_link_types WHERE id = ?", entityId);
                case "biz_category" -> queryOne("SELECT category_code FROM ont_biz_category WHERE id = ?", entityId);
                default -> null;
            };
        } catch (Exception e) {
            log.warn("查 api_name 失败: type={}, id={}: {}", entityType, entityId, e.getMessage());
            return null;
        }
    }

    private String queryOne(String sql, String param) {
        List<String> rows = jdbcTemplate.queryForList(sql, String.class, param);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 根据 entity_type + api_name 从 Jena 获取完整本体结构 */
    private Map<String, Object> fetchOntology(String entityType, String apiName) {
        return switch (entityType.toLowerCase()) {
            case "class" -> {
                Map<String, Object> detail = jenaToolService.classDetail(apiName);
                if (Boolean.TRUE.equals(detail.get("success")) && apiName.startsWith("Enum_")) {
                    // 枚举类额外拉成员列表
                    Map<String, Object> membersResult = jenaToolService.members(apiName, null, 1, 30);
                    if (Boolean.TRUE.equals(membersResult.get("success"))) {
                        detail.put("members", membersResult.get("members"));
                        detail.put("membersTotal", membersResult.get("total"));
                    }
                }
                yield detail;
            }
            case "property", "class_prop", "shared_prop", "shared_property", "link_type" ->
                    jenaToolService.propertyDetail(apiName);
            case "enum", "enum_item" -> {
                Map<String, Object> detail = jenaToolService.classDetail(apiName);
                Map<String, Object> members = jenaToolService.members(apiName, null, 1, 50);
                if (Boolean.TRUE.equals(detail.get("success")) && Boolean.TRUE.equals(members.get("success"))) {
                    detail.put("members", members.get("members"));
                    detail.put("membersTotal", members.get("total"));
                }
                yield detail;
            }
            default -> jenaToolService.classDetail(apiName);
        };
    }

    /**
     * 最小关系集：向量搜索命中的实体 → 从 Jena 提取统一子图结构，供外部调用方喂给 LLM 生成 SPARQL。
     *
     * <p>返回结构：
     * <pre>
     * {
     *   "prefixes":   { "": "http://...", "owl": "...", ... },   // SPARQL 命名空间前缀
     *   "classes":    [ { localName, uri, label, superClasses, subClasses } ],
     *   "properties": [ { localName, uri, label, type, domains, ranges } ],
     *   "edges":      [ { from, property, to, label } ],          // 命中实体间的关系边
     *   "matchedEntities": [ { entityType, entityId, apiName, similarity, nsCode } ]
     * }
     * </pre>
     */
    public Map<String, Object> subgraph(String question, String nsCode, int topK, double threshold) {
        // 1. 向量搜索（带重试）
        Map<String, Object> vectorResult = null;
        int maxAttempts = 5;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            vectorResult = vectorToolService.search(question, topK, threshold, nsCode);
            boolean success = Boolean.TRUE.equals(vectorResult.get("success"));
            if (success) break;

            if (attempt < maxAttempts) {
                String msg = vectorResult.get("message") != null
                        ? String.valueOf(vectorResult.get("message")) : "";
                log.warn("向量搜索失败（第{}/{}次）: {}", attempt, maxAttempts, msg);
                try { Thread.sleep((long)(100 + attempt * 100 + Math.random() * 100)); } catch (InterruptedException ignored) {}
            }
        }

        boolean vectorOk = Boolean.TRUE.equals(vectorResult.get("success"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matches = vectorOk
                ? (List<Map<String, Object>>) vectorResult.getOrDefault("matches", Collections.emptyList())
                : Collections.emptyList();

        // 2. 解析命中实体，反查 apiName
        List<Map<String, Object>> matchedEntities = new ArrayList<>();
        // apiName → entity_type（用于后续按类型查 Jena）
        Map<String, String> classApiNames = new LinkedHashMap<>();    // class/enum 类型
        Map<String, String> propertyApiNames = new LinkedHashMap<>(); // property 类型

        for (Map<String, Object> match : matches) {
            String entityId = match.get("entity_id") != null
                    ? String.valueOf(match.get("entity_id"))
                    : match.get("class_id") != null ? String.valueOf(match.get("class_id")) : null;
            if (entityId == null) continue;

            String entityType = match.get("entity_type") != null
                    ? String.valueOf(match.get("entity_type"))
                    : inferTypeFromId(entityId);
            // 优先使用向量库直接存储的 api_name，避免 DB 反查
            String apiName = match.get("api_name") instanceof String s && !s.isBlank()
                    ? s : lookupApiName(entityType, entityId);
            double similarity = match.get("similarity") instanceof Number n ? n.doubleValue() : 0.0;
            String ns = match.get("ns_code") != null ? String.valueOf(match.get("ns_code")) : null;

            Map<String, Object> em = new LinkedHashMap<>();
            em.put("entityType", entityType);
            em.put("entityId", entityId);
            em.put("apiName", apiName);
            em.put("similarity", similarity);
            em.put("nsCode", ns);
            matchedEntities.add(em);

            if (apiName == null) continue;
            switch (entityType.toLowerCase()) {
                case "synonym" -> {
                    // synonym 实体的 api_name 是中文词，Jena 里没有对应类，跳过本体查找
                    // 仅记录在 matchedEntities 供调用方参考，不做 Jena 扩展
                }
                case "class", "enum" -> classApiNames.put(apiName, entityType);
                case "class_prop", "property", "shared_prop", "shared_property", "link_type" ->
                        propertyApiNames.put(apiName, entityType);
                case "enum_item" -> {
                    // enum_item 的 apiName 实际是父枚举的 api_name，直接加入 classApiNames
                    classApiNames.put(apiName, "enum");
                }
                default -> classApiNames.put(apiName, entityType);
            }
        }

        // 3. 从 Jena 拉取每个命中实体的结构（去重）
        Map<String, Map<String, Object>> classDetailMap = new LinkedHashMap<>();
        for (String api : classApiNames.keySet()) {
            if (classDetailMap.containsKey(api)) continue;

            // 枚举类型需要加 Enum_ 前缀
            String entityType = classApiNames.get(api);
            String classApiName = "enum".equalsIgnoreCase(entityType) ? "Enum_" + api : api;

            Map<String, Object> detail = jenaToolService.classDetail(classApiName);
            if (Boolean.TRUE.equals(detail.get("success"))) {
                classDetailMap.put(api, detail);  // 用原始 apiName 作为 key
            }
        }

        // 3a. 枚举类型特殊处理：从 Jena 本体查询枚举项（Individual），使用缓存，并从数据库补充额外字段
        Map<String, List<Map<String, Object>>> enumItemsMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : classApiNames.entrySet()) {
            String apiName = entry.getKey();
            String entityType = entry.getValue();
            if ("enum".equalsIgnoreCase(entityType)) {
                // 先查缓存
                List<Map<String, Object>> items = enumItemsCache.get(apiName);
                if (items == null) {
                    // 缓存未命中，从 Jena 本体查询枚举的所有 Individual
                    Map<String, Object> enumResult = jenaToolService.getEnumIndividuals(apiName);
                    if (Boolean.TRUE.equals(enumResult.get("success"))) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> fetchedItems = (List<Map<String, Object>>) enumResult.get("items");
                        if (fetchedItems != null && !fetchedItems.isEmpty()) {
                            // 从数据库补充额外字段（parent_code, level, sort_num, status）
                            items = enrichEnumItemsFromDatabase(apiName, fetchedItems);
                            // 存入缓存
                            enumItemsCache.put(apiName, items);
                            log.debug("枚举 {} 从本体加载了 {} 个枚举项并缓存", apiName, items.size());
                        }
                    }
                } else {
                    log.debug("枚举 {} 从缓存读取 {} 个枚举项", apiName, items.size());
                }

                if (items != null && !items.isEmpty()) {
                    enumItemsMap.put(apiName, items);
                }
            }
        }

        Map<String, Map<String, Object>> propDetailMap = new LinkedHashMap<>();
        for (String api : propertyApiNames.keySet()) {
            if (propDetailMap.containsKey(api)) continue;
            Map<String, Object> detail = jenaToolService.propertyDetail(api);
            if (Boolean.TRUE.equals(detail.get("success"))) {
                propDetailMap.put(api, detail);
            }
        }

        // 3b. class_prop 命中时，补充其 domain 类（属性所属的类）
        for (Map<String, Object> pd : new ArrayList<>(propDetailMap.values())) {
            @SuppressWarnings("unchecked")
            List<String> doms = pd.get("domains") instanceof List<?> l
                    ? (List<String>) l : Collections.emptyList();
            for (String domClass : doms) {
                if (!classDetailMap.containsKey(domClass)) {
                    Map<String, Object> detail = jenaToolService.classDetail(domClass);
                    if (Boolean.TRUE.equals(detail.get("success"))) {
                        classDetailMap.put(domClass, detail);
                    }
                }
            }
        }

        // 4. 扩展：收集命中类的属性，把属性的 range 在命中类集合里的也纳入 propDetailMap
        // 4a. 同时收集属性 ranges 中的枚举类，查询枚举项
        // 注意：不能在迭代 classDetailMap.values() 期间直接 put，用 pending map 暂存
        Set<String> classSet = classDetailMap.keySet();
        Map<String, Map<String, Object>> pendingClassDetails = new LinkedHashMap<>();
        for (Map<String, Object> cd : classDetailMap.values()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> props = (List<Map<String, Object>>) cd.get("properties");
            if (props == null) continue;
            for (Map<String, Object> p : props) {
                String pName = p.get("localName") != null ? String.valueOf(p.get("localName")) : null;
                if (pName == null || propDetailMap.containsKey(pName)) continue;
                Map<String, Object> pd = jenaToolService.propertyDetail(pName);
                if (Boolean.TRUE.equals(pd.get("success"))) {
                    propDetailMap.put(pName, pd);

                    // 检查属性的 ranges 中是否有枚举类（localName 以 Enum_ 开头）
                    @SuppressWarnings("unchecked")
                    List<String> ranges = pd.get("ranges") instanceof List<?> l
                            ? (List<String>) l : Collections.emptyList();
                    for (String rangeClass : ranges) {
                        if (rangeClass != null && rangeClass.startsWith("Enum_")) {
                            // 提取枚举 api_name（去掉 Enum_ 前缀）
                            String enumApiName = rangeClass.replace("Enum_", "");
                            if (!enumItemsMap.containsKey(enumApiName)) {
                                // 查询枚举项
                                List<Map<String, Object>> items = enumItemsCache.get(enumApiName);
                                if (items == null) {
                                    Map<String, Object> enumResult = jenaToolService.getEnumIndividuals(enumApiName);
                                    if (Boolean.TRUE.equals(enumResult.get("success"))) {
                                        @SuppressWarnings("unchecked")
                                        List<Map<String, Object>> fetchedItems = (List<Map<String, Object>>) enumResult.get("items");
                                        if (fetchedItems != null && !fetchedItems.isEmpty()) {
                                            items = enrichEnumItemsFromDatabase(enumApiName, fetchedItems);
                                            enumItemsCache.put(enumApiName, items);
                                            log.debug("属性 {} 的枚举 {} 加载了 {} 个枚举项", pName, enumApiName, items.size());
                                        }
                                    }
                                }
                                if (items != null && !items.isEmpty()) {
                                    enumItemsMap.put(enumApiName, items);

                                    // 延迟加入：不在迭代中直接修改 classDetailMap
                                    if (!classDetailMap.containsKey(enumApiName) && !pendingClassDetails.containsKey(enumApiName)) {
                                        Map<String, Object> enumClassDetail = jenaToolService.classDetail(rangeClass);
                                        if (Boolean.TRUE.equals(enumClassDetail.get("success"))) {
                                            pendingClassDetails.put(enumApiName, enumClassDetail);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // 迭代结束后统一合并，避免迭代中修改 classDetailMap
        classDetailMap.putAll(pendingClassDetails);

        // 5. 构建 classes 列表（移除枚举类，不再放入 classes）
        List<Map<String, Object>> classes = new ArrayList<>();
        log.debug("开始构建 classes 列表，classDetailMap.size={}, enumItemsMap.size={}, enumItemsMap.keys={}",
                classDetailMap.size(), enumItemsMap.size(), enumItemsMap.keySet());

        for (Map.Entry<String, Map<String, Object>> e : classDetailMap.entrySet()) {
            Map<String, Object> cd = e.getValue();
            String localName = cd.get("localName") != null ? String.valueOf(cd.get("localName")) : null;

            // 跳过枚举类，不放入 classes 列表
            if (localName != null && localName.startsWith("Enum_")) {
                continue;
            }

            Map<String, Object> cls = new LinkedHashMap<>();
            cls.put("localName", cd.get("localName"));
            cls.put("uri", cd.get("uri"));
            cls.put("label", cd.get("label"));
            cls.put("comment", cd.get("comment"));
            cls.put("superClasses", cd.get("superClasses"));
            cls.put("subClasses", cd.get("subClasses"));

            classes.add(cls);
        }

        // 6. 构建 properties 列表
        List<Map<String, Object>> properties = new ArrayList<>();
        for (Map<String, Object> pd : propDetailMap.values()) {
            Map<String, Object> prop = new LinkedHashMap<>();
            prop.put("localName", pd.get("localName"));
            prop.put("uri", pd.get("uri"));
            prop.put("label", pd.get("label"));
            prop.put("type", pd.get("type"));
            prop.put("domains", pd.get("domains"));
            prop.put("ranges", pd.get("ranges"));
            prop.put("inverseOf", pd.get("inverseOf"));
            prop.put("dataType", pd.get("dataType"));  // XSD 数据类型：xsd:string, xsd:integer 等
            prop.put("physicalTable", pd.get("physicalTable"));
            prop.put("dataSourceCode", pd.get("dataSourceCode"));
            properties.add(prop);
        }

        // 7. 检测跨实体边（命中类集合内的 ObjectProperty 连接）
        List<Map<String, Object>> edges = new ArrayList<>();
        for (Map<String, Object> pd : propDetailMap.values()) {
            if (!"ObjectProperty".equals(pd.get("type"))) continue;
            @SuppressWarnings("unchecked")
            List<String> domains = pd.get("domains") instanceof List<?> l
                    ? (List<String>) l : Collections.emptyList();
            @SuppressWarnings("unchecked")
            List<String> ranges = pd.get("ranges") instanceof List<?> l
                    ? (List<String>) l : Collections.emptyList();
            for (String from : domains) {
                for (String to : ranges) {
                    if (classSet.contains(from) || classSet.contains(to)) {
                        Map<String, Object> edge = new LinkedHashMap<>();
                        edge.put("from", from);
                        edge.put("property", pd.get("localName"));
                        edge.put("to", to);
                        String fromLabel = classDetailMap.containsKey(from)
                                ? (String) classDetailMap.get(from).get("label") : from;
                        String toLabel = classDetailMap.containsKey(to)
                                ? (String) classDetailMap.get(to).get("label") : to;
                        edge.put("label", fromLabel + " --[" + pd.get("label") + "]--> " + toLabel);
                        edges.add(edge);
                    }
                }
            }
        }

        // 8. 命名空间前缀（供 SPARQL PREFIX 声明使用）
        Map<String, Object> vocab = jenaToolService.vocab();
        @SuppressWarnings("unchecked")
        Map<String, String> prefixes = vocab.get("prefixes") instanceof Map<?, ?> m
                ? (Map<String, String>) m : Collections.emptyMap();

        // 9. 构建枚举字典: Enum_XXX → { label, items: [{code, label}] }
        Map<String, Object> enums = new LinkedHashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : enumItemsMap.entrySet()) {
            String apiName = entry.getKey();  // KJLB, JGLXN, ...
            List<Map<String, Object>> items = entry.getValue();

            // 从 classDetailMap 获取枚举类的 label
            Map<String, Object> enumClassDetail = classDetailMap.get(apiName);
            String enumLabel = enumClassDetail != null && enumClassDetail.get("label") != null
                    ? String.valueOf(enumClassDetail.get("label"))
                    : apiName;

            Map<String, Object> enumObj = new LinkedHashMap<>();
            enumObj.put("label", enumLabel);
            enumObj.put("items", items);
            enums.put("Enum_" + apiName, enumObj);  // key 用 Enum_XXX 格式，与 ranges 一致
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("prefixes", prefixes);
        result.put("classes", classes);
        result.put("properties", properties);
        result.put("enums", enums);  // 新增：所有枚举集中放这里
        result.put("edges", edges);
        result.put("matchedEntities", matchedEntities);
        result.put("vectorAvailable", vectorOk);
        return result;
    }

    /**
     * 向量搜索无命中时的 fallback 策略：
     *   优先 Jena keyword 检索，仍无结果返回 none。
     */
    private Map<String, Object> buildFallback(String question, Map<String, Object> vectorResult) {
        Map<String, Object> fallback = new LinkedHashMap<>();

        Map<String, Object> jenaResult = jenaToolService.entities("all", null, question, 1, 10);
        if (Boolean.TRUE.equals(jenaResult.get("success"))) {
            @SuppressWarnings("unchecked")
            List<?> items = (List<?>) jenaResult.get("items");
            if (items != null && !items.isEmpty()) {
                fallback.put("source", "jena_keyword");
                fallback.put("items", items);
                fallback.put("note", "向量搜索无命中，已降级到 Jena 关键词检索");
                return fallback;
            }
        }

        fallback.put("source", "none");
        fallback.put("items", Collections.emptyList());
        fallback.put("note", "未找到相关本体实体");
        return fallback;
    }

    /**
     * 清理枚举项缓存（可在本体重建后调用）
     */
    public void clearEnumItemsCache() {
        enumItemsCache.clear();
        log.info("枚举项缓存已清空");
    }

    /**
     * 清理指定枚举的缓存
     *
     * @param enumApiName 枚举的 api_name
     */
    public void clearEnumItemsCache(String enumApiName) {
        enumItemsCache.remove(enumApiName);
        log.debug("枚举 {} 的缓存已清除", enumApiName);
    }

    /**
     * 从数据库补充枚举项的额外字段（parent_code, level, sort_num, status）
     *
     * @param enumApiName 枚举的 api_name
     * @param jenaItems 从 Jena 查询的枚举项列表（包含 code, label, apiName 等）
     * @return 补充了数据库字段的枚举项列表
     */
    private List<Map<String, Object>> enrichEnumItemsFromDatabase(String enumApiName, List<Map<String, Object>> jenaItems) {
        if (jenaItems == null || jenaItems.isEmpty()) {
            return jenaItems;
        }

        try {
            // 1. 查询枚举ID
            String sqlEnumId = "SELECT id FROM ont_enum_types WHERE api_name = ?";
            List<Map<String, Object>> enumRows = jdbcTemplate.queryForList(sqlEnumId, enumApiName);
            if (enumRows.isEmpty()) {
                log.warn("数据库中未找到枚举类型: {}", enumApiName);
                return jenaItems;
            }
            String enumId = String.valueOf(enumRows.get(0).get("id"));

            // 2. 查询所有枚举项
            String sqlItems = """
                SELECT code, api_name, label, parent_code, level, sort_num, status
                FROM ont_enum_items
                WHERE enum_id = ?
                ORDER BY level, sort_num, code
                """;
            List<Map<String, Object>> dbItems = jdbcTemplate.queryForList(sqlItems, enumId);

            // 3. 构建 code -> dbItem 的映射
            Map<String, Map<String, Object>> dbItemsMap = new LinkedHashMap<>();
            for (Map<String, Object> dbItem : dbItems) {
                String code = String.valueOf(dbItem.get("code"));
                dbItemsMap.put(code, dbItem);
            }

            // 4. 合并 Jena 和数据库数据，只保留 code 和 label
            List<Map<String, Object>> enrichedItems = new ArrayList<>();
            for (Map<String, Object> jenaItem : jenaItems) {
                String code = jenaItem.get("code") != null ? String.valueOf(jenaItem.get("code")) : null;
                if (code == null) continue;

                Map<String, Object> enriched = new LinkedHashMap<>();
                enriched.put("code", code);
                enriched.put("label", jenaItem.get("label"));

                enrichedItems.add(enriched);
            }

            return enrichedItems;

        } catch (Exception e) {
            log.warn("从数据库补充枚举项字段失败: enumApiName={}, error={}", enumApiName, e.getMessage());
            return jenaItems;
        }
    }
}
