package com.beiktech.bontolink.tool;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.tool.db.DatabaseToolService;
import com.beiktech.bontolink.tool.jena.JenaToolService;
import com.beiktech.bontolink.tool.semantic.SemanticExpandService;
import com.beiktech.bontolink.tool.vector.VectorToolService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具模块 - 统一查询控制器
 *
 * <p>提供对三大底层数据源的只读查询能力，全部挂载在 {@code /api/tool} 下：
 * <ul>
 *   <li><b>Jena 本体库</b>：SPARQL 查询、OWL 类/属性列举、类详情</li>
 *   <li><b>向量库</b>：文本相似度检索（PostgreSQL + pgvector）</li>
 *   <li><b>关系数据库</b>：只读 SELECT、表列表、表结构</li>
 * </ul>
 *
 * <p>统一入口 {@code POST /api/tool/query} 按 {@code source} 字段分发到上述三类；
 * 同时提供各数据源的独立端点，便于前端工具页按需调用。
 *
 * <p>⚠️ 该模块为诊断/管理工具，生产环境应加鉴权保护。
 */
@RestController
@RequestMapping("/api/tool")
public class ToolQueryController {

    private final JenaToolService jenaToolService;
    private final VectorToolService vectorToolService;
    private final DatabaseToolService databaseToolService;
    private final SemanticExpandService semanticExpandService;

    public ToolQueryController(JenaToolService jenaToolService,
                               VectorToolService vectorToolService,
                               DatabaseToolService databaseToolService,
                               SemanticExpandService semanticExpandService) {
        this.jenaToolService = jenaToolService;
        this.vectorToolService = vectorToolService;
        this.databaseToolService = databaseToolService;
        this.semanticExpandService = semanticExpandService;
    }

    // ============================ 统一入口 ============================

    /**
     * 统一查询分发。
     * Body 示例：
     *   { "source": "jena",   "sparql": "SELECT ?c WHERE { ?c a owl:Class } LIMIT 10" }
     *   { "source": "vector", "text": "水库", "topK": 10, "threshold": 0.6, "nsCode": "w_wtr" }
     *   { "source": "db",     "sql": "SELECT * FROM ont_class LIMIT 10" }
     *   { "source": "db",     "table": "ont_class" }   // 查表结构
     *   { "source": "db" }                              // 列所有表
     */
    @PostMapping("/query")
    public R<Map<String, Object>> query(@RequestBody Map<String, Object> body) {
        String source = body.get("source") == null ? null : String.valueOf(body.get("source")).toLowerCase();
        if (source == null || source.isBlank()) {
            return R.error(400, "缺少 source 字段（jena | vector | db）");
        }
        return switch (source) {
            case "jena" -> {
                if (body.get("sparql") != null) {
                    int limit = toInt(body.get("limit"), 0);
                    yield wrap(jenaToolService.sparql(String.valueOf(body.get("sparql")), limit));
                }
                yield wrap(jenaToolService.listClasses());
            }
            case "vector" -> {
                String text = body.get("text") == null ? null : String.valueOf(body.get("text"));
                int topK = toInt(body.get("topK"), 0);
                double threshold = toDouble(body.get("threshold"), 0);
                String nsCode = body.get("nsCode") == null ? null : String.valueOf(body.get("nsCode"));
                yield wrap(vectorToolService.search(text, topK, threshold, nsCode));
            }
            case "db" -> {
                if (body.get("sql") != null) {
                    yield wrap(databaseToolService.query(String.valueOf(body.get("sql"))));
                }
                if (body.get("table") != null) {
                    yield wrap(databaseToolService.schema(String.valueOf(body.get("table"))));
                }
                yield wrap(databaseToolService.listTables());
            }
            default -> R.error(400, "未知 source: " + source + "（支持 jena | vector | db）");
        };
    }

    /**
     * 三大数据源状态聚合（便于工具页一次性展示可用性）。
     */
    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        Map<String, Object> all = new LinkedHashMap<>();
        all.put("jena", jenaToolService.status());
        all.put("vector", vectorToolService.status());
        all.put("database", databaseToolService.status());
        return R.ok(all);
    }

    // ============================ Jena ============================

    @PostMapping("/jena/sparql")
    public R<Map<String, Object>> jenaSparql(@RequestBody Map<String, Object> body) {
        String sparql = body.get("sparql") == null ? null : String.valueOf(body.get("sparql"));
        int limit = toInt(body.get("limit"), 0);
        String format = body.get("format") == null ? null : String.valueOf(body.get("format"));
        return wrap(jenaToolService.sparql(sparql, limit, format));
    }

    /** 预设 SPARQL 查询模板（通用，可直接执行）。 */
    @GetMapping("/jena/query/templates")
    public R<Map<String, Object>> jenaQueryTemplates() {
        return wrap(jenaToolService.queryTemplates());
    }

    @GetMapping("/jena/classes")
    public R<Map<String, Object>> jenaClasses() {
        return wrap(jenaToolService.listClasses());
    }

    @GetMapping("/jena/properties")
    public R<Map<String, Object>> jenaProperties() {
        return wrap(jenaToolService.listProperties());
    }

    @GetMapping("/jena/class/{localName}")
    public R<Map<String, Object>> jenaClassDetail(@PathVariable String localName) {
        return wrap(jenaToolService.classDetail(localName));
    }

    @GetMapping("/jena/property/{localName}")
    public R<Map<String, Object>> jenaPropertyDetail(@PathVariable String localName) {
        return wrap(jenaToolService.propertyDetail(localName));
    }

    /**
     * 通用资源页（仿 Fuseki，只读）：任意 IRI 资源的出站/入站三元组 + 类型 + 标签。
     * 例：/api/tool/jena/resource?uri=http%3A%2F%2F...%23Enum_DWLX_201&page=1&size=50
     */
    @GetMapping("/jena/resource")
    public R<Map<String, Object>> jenaResource(@RequestParam String uri,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "50") int size) {
        return wrap(jenaToolService.resource(uri, page, size));
    }

    /**
     * 成员浏览：列出某类的全部个体（实例/枚举值通用，只读），支持关键字与分页。
     * 例：/api/tool/jena/members?class=Enum_DWLX&keyword=水利&page=1&size=50
     */
    @GetMapping("/jena/members")
    public R<Map<String, Object>> jenaMembers(@RequestParam("class") String classRef,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "50") int size) {
        return wrap(jenaToolService.members(classRef, keyword, page, size));
    }

    /** 本体元数据：命名空间/前缀、本体声明、imports、规模统计。 */
    @GetMapping("/jena/meta/vocab")
    public R<Map<String, Object>> jenaVocab() {
        return wrap(jenaToolService.vocab());
    }

    /** 实体类型清单及数量。 */
    @GetMapping("/jena/meta/kinds")
    public R<Map<String, Object>> jenaKinds() {
        return wrap(jenaToolService.kinds());
    }

    /** 通用实体目录检索：kind=class|objectProperty|datatypeProperty|annotationProperty|individual|all */
    @GetMapping("/jena/entities")
    public R<Map<String, Object>> jenaEntities(@RequestParam(required = false) String kind,
                                               @RequestParam(required = false) String ns,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "50") int size) {
        return wrap(jenaToolService.entities(kind, ns, keyword, page, size));
    }

    /** 类层级树：root 为空=全部顶层类；depth 默认 4。 */
    @GetMapping("/jena/hierarchy")
    public R<Map<String, Object>> jenaHierarchy(@RequestParam(required = false) String root,
                                                @RequestParam(defaultValue = "0") int depth) {
        return wrap(jenaToolService.hierarchy(root, depth));
    }

    // ============================ 向量库 ============================

    @PostMapping("/vector/search")
    public R<Map<String, Object>> vectorSearch(@RequestBody Map<String, Object> body) {
        String text = body.get("text") == null ? null : String.valueOf(body.get("text"));
        int topK = toInt(body.get("topK"), 0);
        double threshold = toDouble(body.get("threshold"), 0);
        String nsCode = body.get("nsCode") == null ? null : String.valueOf(body.get("nsCode"));
        return wrap(vectorToolService.search(text, topK, threshold, nsCode));
    }

    /** 分页浏览向量库全部内容。 */
    @GetMapping("/vector/list")
    public R<Map<String, Object>> vectorList(@RequestParam(required = false) String nsCode,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        return wrap(vectorToolService.list(nsCode, page, size));
    }

    /** 单条向量详情（按 class_id）。 */
    @GetMapping("/vector/detail/{classId}")
    public R<Map<String, Object>> vectorDetail(@PathVariable String classId) {
        return wrap(vectorToolService.detail(classId));
    }

    // ============================ 语义扩展 ============================

    /**
     * 语义扩展：输入问题 → 向量搜索 → Jena 本体结构联动 → 未命中降级。
     * Body 示例：
     *   { "question": "水利工程中的堤坝类型", "nsCode": "w_wtr", "topK": 5, "threshold": 0.7 }
     */
    @PostMapping("/semantic-expand")
    public R<Map<String, Object>> semanticExpand(@RequestBody Map<String, Object> body) {
        String question = body.get("question") == null ? null : String.valueOf(body.get("question"));
        if (question == null || question.isBlank()) {
            return R.error(400, "question 不能为空");
        }
        String nsCode = body.get("nsCode") == null ? null : String.valueOf(body.get("nsCode"));
        int topK = toInt(body.get("topK"), 0);
        double threshold = toDouble(body.get("threshold"), 0);
        Map<String, Object> result = semanticExpandService.expand(question, nsCode, topK, threshold);
        return R.ok(result);
    }

    /**
     * 最小关系集（子图上下文）：向量搜索命中实体 → 提取 Jena 本体子图结构，供外部调用方喂给 LLM 生成 SPARQL。
     *
     * <p>返回结构（可直接作为 SPARQL 生成的上下文）：
     * <pre>
     * {
     *   "prefixes":        { "": "http://...", "owl": "..." },      // SPARQL PREFIX 声明用
     *   "classes":         [ { localName, uri, label, superClasses, subClasses } ],
     *   "properties":      [ { localName, uri, label, type, domains, ranges, inverseOf } ],
     *   "edges":           [ { from, property, to, label } ],       // 命中类之间的 ObjectProperty 边
     *   "matchedEntities": [ { entityType, entityId, apiName, similarity, nsCode } ]
     * }
     * </pre>
     * Body 示例：
     *   { "question": "水利工程中的堤坝类型", "nsCode": "w_wtr", "topK": 5, "threshold": 0.7 }
     */
    @PostMapping("/subgraph")
    public R<Map<String, Object>> subgraph(@RequestBody Map<String, Object> body) {
        String question = body.get("question") == null ? null : String.valueOf(body.get("question"));
        if (question == null || question.isBlank()) {
            return R.error(400, "question 不能为空");
        }
        String nsCode = body.get("nsCode") == null ? null : String.valueOf(body.get("nsCode"));
        int topK = toInt(body.get("topK"), 0);
        double threshold = toDouble(body.get("threshold"), 0);
        Map<String, Object> result = semanticExpandService.subgraph(question, nsCode, topK, threshold);
        return R.ok(result);
    }

    /**
     * 清除语义扩展服务的枚举项缓存
     */
    @DeleteMapping("/semantic/enum-cache")
    public R<String> clearEnumCache() {
        semanticExpandService.clearEnumItemsCache();
        return R.ok("枚举项缓存已清空");
    }

    // ============================ 数据库 ============================

    @PostMapping("/db/query")
    public R<Map<String, Object>> dbQuery(@RequestBody Map<String, Object> body) {
        String sql = body.get("sql") == null ? null : String.valueOf(body.get("sql"));
        return wrap(databaseToolService.query(sql));
    }

    @GetMapping("/db/tables")
    public R<Map<String, Object>> dbTables() {
        return wrap(databaseToolService.listTables());
    }

    @GetMapping("/db/schema/{table}")
    public R<Map<String, Object>> dbSchema(@PathVariable String table) {
        return wrap(databaseToolService.schema(table));
    }

    // ============================ 内部工具 ============================

    private R<Map<String, Object>> wrap(Map<String, Object> svc) {
        if (Boolean.TRUE.equals(svc.get("success"))) {
            return R.ok(svc);
        }
        String msg = svc.get("message") == null ? "查询失败" : String.valueOf(svc.get("message"));
        return R.error(400, msg);
    }

    private int toInt(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }

    private double toDouble(Object v, double def) {
        if (v == null) return def;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }
}
