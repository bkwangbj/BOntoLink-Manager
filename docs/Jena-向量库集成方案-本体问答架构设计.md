# Jena + 向量库集成方案：本体问答架构设计

## 目录
1. [背景](#背景)
2. [核心约束与方案定性](#核心约束与方案定性)
3. [部署架构](#部署架构)
4. [缓存架构与幂等性](#缓存架构与幂等性)
5. [按 namespace 分区 + 增量同步](#按-namespace-分区--增量同步)
6. [Efficiency：效率设计专项](#efficiency效率设计专项)
7. [数据库行 → RDF 映射规则](#数据库行--rdf-映射规则)
8. [问答管道具体实现](#问答管道具体实现)
9. [完整数据流示例](#完整数据流示例)
10. [分阶段实施](#分阶段实施)
11. [验证方法](#验证方法)

---

## 背景

当前 ontology 模块虽然引入了 Jena 5.2.0 依赖并配置了 OntologyEngineConfig（storage-mode / reasoner-type / tdb2-location / sparql-enabled），但运行时零使用。数据库表结构已含丰富的 RDF/OWL 元数据（rdfs_label、rdfs_comment、owl_functional、owl_transitive 等），但所有搜索/匹配完全依赖 SQL LIKE，没有利用本体推理和向量语义。

目标：实现用户自然语言提问 → 判断本体相关 → 关联本体信息 → 组装 SPARQL → 转 SQL → 查业务数据 → 返回结果的完整链路。

---

## 核心约束与方案定性

| 约束 | 应对 |
|------|------|
| **规模大，启动不能重建** | 按 namespace 懒加载 + 增量同步（见第5节） |
| **部署结构** | ontology 独立服务（8089），与 admin（8088）共享数据层（见第3节） |
| **缓存与幂等** | 四级缓存，相同问题直接返回（见第4节） |
| **效率** | 三路匹配短路逻辑、Jena 只做推理不做关键词搜索、轻量推理、查询超时兜底（见第6节） |

---

## 部署架构

```
┌────────────────────────────────────────────────────────────────────┐
│                       客户端 (Vue 前端)                              │
│               POST /api/ontology/ask  { question }                  │
└──────────────────────────┬─────────────────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────────────────┐
│                    Nginx / 负载均衡                                  │
│          /api/ontology/* → ontology 服务 (8089)                     │
│          /api/*          → admin 服务     (8088)                    │
└──────────────────────────┬─────────────────────────────────────────┘
                           │
           ┌───────────────┴───────────────┐
           │                               │
┌──────────▼──────────┐     ┌──────────────▼──────────────┐
│  admin 服务 (8088)   │     │  ontology 服务 (8089)       │
│                     │     │                              │
│  SearchController   │     │  OntologyQueryController    │
│  ResourceController │     │  → QuestionAnalyzer         │
│  ...                │     │  → FusedMatchService        │
│                     │     │    ├─ Jena SPARQL (推理)      │
│  依赖：             │     │    ├─ Vector Search (相似度)   │
│  bontolink-data     │     │    └─ Keyword Match (关键词)  │
│  bontolink-base     │     │  → SparqlQueryService        │
│                     │     │  → SqlGeneratorService       │
└──────────┬──────────┘     └──────────┬──────────────────┘
           │                           │
           └───────────┬───────────────┘
                       │
            ┌──────────▼──────────┐
            │   bontolink-data     │      ← 两个服务共享数据层
            │   (MyBatis + Mapper) │
            └──────────┬──────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────────┐
│                       数据库层                                       │
│                                                                     │
│  ┌────────────────┐  ┌──────────────────────┐  ┌───────────────┐   │
│  │  业务数据库     │  │  TDB2 文件存储        │  │  向量库        │   │
│  │  (PostgreSQL/  │  │  ./data/tdb2/        │  │  pgvector     │   │
│  │   SQLite)      │  │  └─ namespaces/      │  │  + HNSW索引   │   │
│  │                │  │     ├─ w_wtr/        │  │  class_em-    │   │
│  │  ont_class     │  │     ├─ fin_acc/      │  │  beddings     │   │
│  │  ont_class_    │  │     └─ .../          │  │  ns_code分区  │   │
│  │  property      │  └──────────────────────┘  └───────────────┘   │
│  └────────────────┘                                                │
└────────────────────────────────────────────────────────────────────┘
```

| 方面 | 方案 |
|------|------|
| 服务关系 | ontology 独立进程（8089），与 admin（8088）共享数据层，不互相 RPC |
| TDB2 存储 | 本地磁盘，每个 namespace 独立目录；集群部署时挂载共享存储或每个节点独立副本 |
| 向量库 | pgvector（生产），SQLite BLOB（开发） |
| 共享缓存 | Redis 由两个服务共用 |

**启动方式（ontology 服务）**：

```bash
cd backend/bontolink-ontology
mvnw -DskipTests spring-boot:run              # 开发启动
# 或打包后
java -jar bontolink-ontology/target/bontolink-ontology-1.0.0.jar
```

---

## 缓存架构与幂等性

### 四级缓存

```
用户问题 "北京的水库去年供水量"
        │
        ▼
┌────────────────────────────────────────┐
│ L1：问题结果缓存 (Redis)                │
│ key = "ask:q:" + MD5(question)         │
│ value = 完整 JSON 结果                  │
│ TTL = 5 分钟                           │
│ 幂等性核心：相同问题直接返回              │
│ ★ CRUD 时不清除，TTL 到期自然淘汰        │
└──────────┬─────────────────────────────┘
     未命中 │
           ▼
┌────────────────────────────────────────┐
│ L2：NLU 分析缓存 (Redis)                │
│ key = "ask:nlu:" + MD5(question)       │
│ value = QuestionAnalysis JSON           │
│ TTL = 10 分钟                          │
│ 分词+扩词+意图分类 不做重复计算           │
└──────────┬─────────────────────────────┘
     未命中 │
           ▼
┌────────────────────────────────────────┐
│ L3：实体匹配缓存 (Redis)                │
│ key = "ask:match:" + MD5(扩展词+ns)    │
│ value = FusedMatch[] JSON              │
│ TTL = 5 分钟                           │
│ 本体 CRUD 时删除相关 key                │
└──────────┬─────────────────────────────┘
     未命中 │
           ▼
┌────────────────────────────────────────┐
│ L4：索引层（不缓存请求结果）              │
│  ├─ Jena OntModel → TDB2 持久化         │
│  │   namespace 粒度，懒加载，常驻内存      │
│  └─ Vector Embeddings → pgvector        │
│      HNSW 索引，O(log n) 检索            │
└─────────────────────────────────────────┘
```

### 缓存失效策略

| 缓存层 | 失效条件 | 操作 |
|--------|----------|------|
| L1 问题结果 | TTL 到期（5 分钟） | Redis 自动过期，不做主动清除 |
| L2 NLU 分析 | TTL 到期（10 分钟） | Redis 自动过期 |
| L3 实体匹配 | 该 namespace 下有本体 CRUD | 删除 `ask:match:{nsCode}:*` 相关 key |
| L4 OntModel | 本体 CRUD | `JenaSyncService.syncClassById()` 增量更新 model |
| L4 向量库 | 本体 CRUD | `VectorSyncService.syncClassById()` 增量 upsert |

### 幂等性核心代码

```java
@PostMapping("/ask")
public R<Map<String, Object>> ask(@RequestBody Map<String, String> body) {
    String question = body.get("question");
    String cacheKey = "ask:q:" + md5(question);

    // L1 检查：相同问题直接返回
    String cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) return R.ok(JSON.parseObject(cached));

    // ...完整问答流程...

    // 写缓存（5 分钟有效期）
    redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(result), 5, TimeUnit.MINUTES);
    return R.ok(result);
}
```

---

## 按 namespace 分区 + 增量同步

### 为什么不能启动全量重建

将来覆盖各行各业，本体量可能很大（上万类、数十万属性）。启动时全量从 DB 加载到 Jena + 向量库 -> 分钟级别，不可接受。

### 方案：懒加载 + 增量同步

| 时机 | 做什么 |
|------|--------|
| **首次查询某个 namespace** | 从 DB 取该 namespace 下的所有类+属性，构建 OntModel，写入 TDB2，生成 embedding |
| **启动时** | 不做全量构建。只检查版本号，若落后量 > 阈值（如 1000 条），后台异步追赶 |
| **CRUD 操作** | 调用 `syncClassById()` 单条同步 Jena + 向量库 |
| **手动重建** | `POST /api/ontology/sync/w_wtr` 重建指定 namespace |

### TDB2 存储目录结构

```
./data/tdb2/
└── namespaces/
    ├── w_wtr/      ← 水利 namespace 的独立模型
    ├── w_wtr_hyd/  ← 水文
    ├── fin_acc/    ← 财务
    └── ...
```

### 版本追踪

在 `ont_class` 表中增加版本字段：

```sql
-- V25__jena_version_tracking.sql
ALTER TABLE ont_class ADD COLUMN jena_version INTEGER DEFAULT 0;
ALTER TABLE ont_class_property ADD COLUMN jena_version INTEGER DEFAULT 0;
```

初次部署：所有行 jena_version = 0（标记为"未同步"）。
每同步一条，该行的 jena_version 递增。
增量同步时只取 `WHERE jena_version > currentVersion AND ns_code = ?` 的数据。

### ModelRegistry.java（核心管理类）

```java
@Component
public class ModelRegistry {
    // namespace → OntModel 映射，常驻内存
    private final Map<String, OntModel> modelCache = new ConcurrentHashMap<>();
    @Autowired private JenaSyncService syncService;

    /**
     * 获取某个 namespace 的 OntModel。
     * 首次调用时自动构建（从 DB 加载）并持久化到 TDB2。
     * 后续调用直接返回内存缓存。
     */
    public OntModel getOrLoadModel(String nsCode) {
        OntModel model = modelCache.get(nsCode);
        if (model != null) return model;

        synchronized (this) {
            model = modelCache.get(nsCode);
            if (model != null) return model;

            // 检查 TDB2 中是否有该 nsCode 的持久化数据
            if (hasTdbData(nsCode)) {
                model = loadFromTdb(nsCode);       // 从 TDB2 加载
            } else {
                model = buildFromDb(nsCode);        // 从数据库全量构建
                saveToTdb(nsCode, model);            // 写入 TDB2 持久化
            }
            modelCache.put(nsCode, model);

            // 后台检查增量（不阻塞请求）
            syncService.syncNamespaceAsync(nsCode);
            return model;
        }
    }

    /**
     * CRUD 时调用：只更新一个类的三元组，不重建整个 namespace
     */
    public void updateClass(String nsCode, String classId) {
        OntModel model = modelCache.get(nsCode);
        if (model == null) return;  // namespace 还没加载过，不需要更新

        // 删除该类的旧三元组 → 插入新三元组
        removeClassTriples(model, classId);
        addClassTriples(model, classId);
        // 同步到 TDB2
        syncToTdb(nsCode, model);
    }
}
```

### JenaSyncService.java（增量同步）

```java
@Service
public class JenaSyncService {

    /**
     * 同步整个 namespace 的增量
     * 查出 jena_version 落后的记录，逐条更新 OntModel
     */
    public void syncNamespace(String nsCode) {
        long currentVer = getCurrentVersion(nsCode);
        List<Map<String, Object>> changed = ontologyMapper.findChangedClasses(nsCode, currentVer);
        if (changed.isEmpty()) return;

        OntModel model = modelRegistry.getOrLoadModel(nsCode);
        for (Map<String, Object> cls : changed) {
            removeClassTriples(model, (String) cls.get("id"));
            addClassTriples(model, cls);
        }
        model.commit();
        updateVersion(nsCode, maxVersion(changed));
    }

    /**
     * 单条同步（CRUD 时调用）
     */
    public void syncClassById(String classId) {
        Map<String, Object> cls = ontologyMapper.findClassById(classId);
        String nsCode = (String) cls.get("ns_code");
        OntModel model = modelRegistry.getOrLoadModel(nsCode);
        removeClassTriples(model, classId);
        addClassTriples(model, cls);
        model.commit();
    }

    // Mapper 支持
    // ontologyMapper.findChangedClasses(nsCode, sinceVersion)
    // → SELECT * FROM ont_class WHERE ns_code = #{nsCode} AND jena_version > #{sinceVersion}
}
```

---

## Efficiency：效率设计专项

### 效率总览

| 环节 | 潜在瓶颈 | 优化手段 | 预期提升 |
|------|----------|----------|----------|
| Jena SPARQL 含 CONTAINS | O(n) 全表扫描 | **不做关键词搜索，只做推理**；URI 精确查询 O(1) | 10x-100x |
| OWL 推理 | 全 OWL 推理极慢 | 用 OWL_MICRO_RULE_INF + 预计算 inferred triples | 100x |
| 三路匹配 | 3 路都跑一遍 | **短路逻辑**：keyword 分 >0.9 时直接返回 | 2x-3x |
| 向量检索 | 全表扫描 | pgvector **HNSW 索引** O(log n) | 1000x |
| SPARQL→SQL 转换 | 每次解析 + 查映射 | **缓存同类结构**的转换结果 | 10x |
| 全流程超时 | 某环节卡住 | 5 秒超时兜底 + 降级 | 避免阻塞 |
| TDB2 I/O | 写多时磁盘瓶颈 | 批量提交 + 异步 sync | 5x |

### 6.1 Jena 只做推理，不做关键词搜索

这是最重要的设计决策。

**不要做的事**（避免全表扫描）：
```sparql
-- 这个查询在 Jena 中无法走索引，全量扫描所有类标签
SELECT ?class WHERE {
  ?class rdfs:label ?label .
  FILTER (CONTAINS(?label, "水库") || CONTAINS(?label, "蓄水池"))
}
```

**Jena 的职责限定为**：精确 URI 查询，做推理增强

| 能做 | 不能做 |
|------|--------|
| `SELECT ?eq WHERE { wtr:Reservoir owl:equivalentClass ?eq }` | `FILTER (CONTAINS(?label, "水库"))` |
| `SELECT ?sub WHERE { ?sub rdfs:subClassOf wtr:Reservoir }` | 全局模糊搜索 |
| `SELECT ?domain WHERE { wtr:waterSupply rdfs:domain ?domain }` | 全文检索 |

**具体调用方式**：Keyword 匹配先找出候选类，Jena 对这些候选类做推理展开：

```java
public List<ScoredEntity> matchViaJena(
        String nsCode, List<ScoredEntity> keywordSeeds) {
    OntModel model = modelRegistry.getModel(nsCode);
    if (model == null) return List.of();

    List<ScoredEntity> inferred = new ArrayList<>();
    for (ScoredEntity seed : keywordSeeds) {
        String uri = resolveNsUri(nsCode) + seed.getEntityName();

        // 等价类推理：找到的同义词继承原分 ×0.7
        String eqSparql = """
            PREFIX owl: <http://www.w3.org/2002/07/owl#>
            SELECT ?eq WHERE { <%s> owl:equivalentClass ?eq }
            """.formatted(uri);
        try (QueryExecution qe = QueryExecutionFactory.create(eqSparql, model)) {
            qe.execSelect().forEachRemaining(sol -> {
                String name = sol.getResource("eq").getLocalName();
                inferred.add(new ScoredEntity(name, seed.getScore() * 0.7, "JENA_INFER"));
            });
        }

        // 子类推理（传递闭包）
        String subSparql = """
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            SELECT ?sub WHERE { ?sub rdfs:subClassOf* <%s> }
            """.formatted(uri);
        try (QueryExecution qe = QueryExecutionFactory.create(subSparql, model)) {
            qe.execSelect().forEachRemaining(sol -> {
                String name = sol.getResource("sub").getLocalName();
                inferred.add(new ScoredEntity(name, seed.getScore() * 0.5, "JENA_INFER"));
            });
        }
    }
    return inferred;
}
```

### 6.2 三路匹配短路逻辑

```java
public List<FusedMatch> match(QuestionAnalysis analysis, int topK) {
    String nsCode = resolveDomainToNsCode(analysis.getDomain());
    if (nsCode == null) return List.of();

    // L3 缓存检查
    String cacheKey = "ask:match:" + md5(analysis.getExpandedTerms() + nsCode);
    String cached = redis.opsForValue().get(cacheKey);
    if (cached != null) return JSON.parseArray(cached, FusedMatch.class);

    // ====== 第1路：Keyword（最快，~5ms）======
    List<ScoredEntity> kw = matchViaKeyword(analysis, nsCode, 20);

    // ★ 短路：如果 keyword 匹配分 > 0.9，直接返回，不做向量和 Jena
    if (kw.stream().anyMatch(r -> r.getScore() > 0.9)) {
        List<FusedMatch> result = fuse(List.of(), List.of(), kw, topK);
        cacheResult(cacheKey, result);
        return result;
    }

    // ====== 需要继续：确保索引已加载 ======
    modelRegistry.getOrLoadModel(nsCode);
    vectorIndexService.ensureNamespaceIndexed(nsCode);

    // ====== 第2路：向量检索（并行，1 秒超时）======
    CompletableFuture<List<ScoredEntity>> vecFuture =
        CompletableFuture.supplyAsync(() -> matchViaVector(analysis, nsCode, 20));
    List<ScoredEntity> vec = vecFuture.get(1, TimeUnit.SECONDS);
    // 超时则 vec 为空列表，降级用 keyword 结果

    // ====== 第3路：Jena 推理增强（依赖 keyword 结果做种子）======
    // 不是独立搜索，而是对 keyword 结果做推理展开
    List<ScoredEntity> jena = matchViaJena(nsCode, kw);

    List<FusedMatch> result = fuse(jena, vec, kw, topK);
    cacheResult(cacheKey, result);
    return result;
}
```

### 6.3 SPARQL → SQL 转换缓存

相同类+属性组合的 SPARQL 结构应复用转换结果：

```java
@Component
public class SqlCache {
    @Autowired private StringRedisTemplate redis;

    public String getOrGenerate(FusedMatch primary, List<Map<String, Object>> props) {
        List<String> sortedProps = props.stream()
            .map(p -> (String) p.get("api_name")).sorted().toList();
        String key = "ask:sql:" + md5(primary.getEntityName() + sortedProps);

        String cached = redis.opsForValue().get(key);
        if (cached != null) return cached;

        // 缓存未命中 → 生成 SQL
        String sql = generateSql(primary, props);
        redis.opsForValue().set(key, sql, 30, TimeUnit.MINUTES);
        return sql;
    }
}
```

### 6.4 全流程超时兜底

```java
@PostMapping("/ask")
public R<Map<String, Object>> ask(@RequestBody Map<String, String> body) {
    String question = body.get("question");
    long t0 = System.currentTimeMillis();

    // L1 缓存
    String cacheKey = "ask:q:" + md5(question);
    String cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) return R.ok(JSON.parseObject(cached));

    try {
        CompletableFuture<R<Map<String, Object>>> future = CompletableFuture.supplyAsync(() -> {
            // 完整问答流程...
        });
        return future.get(5000, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
        // 超时：返回 keyword 匹配的部分结果
        QuestionAnalysis analysis = questionAnalyzer.analyze(question);
        List<ScoredEntity> kw = matchViaKeyword(analysis, null, 5);
        return R.ok(Map.of(
            "message", "查询超时，返回部分结果",
            "matches", kw,
            "elapsed", System.currentTimeMillis() - t0
        ));
    }
}
```

### 6.5 配置化控制

```yaml
bontolink:
  ontology:
    tdb2:
      bulkload: false
      sync-interval-ms: 5000       # 5 秒批量提交一次变更
    fusion:
      keyword-only-threshold: 0.9  # keyword 分 > 0.9 时跳过其他路
      jena-timeout-ms: 1000        # Jena 推理最多 1 秒
      vector-timeout-ms: 1000      # 向量检索最多 1 秒
      ask-timeout-ms: 5000         # 问答全流程最多 5 秒
```

### 6.6 效率手段汇总

| 优化项 | 效果 |
|--------|------|
| Jena 不做关键词搜索，只做推理 | FILTER CONTAINS 改成 URI 精确查询，O(n) → O(1) |
| OWL_MICRO_RULE_INF 替代全 OWL | 推理速度提升 100x |
| Keyword 高分 > 0.9 时短路 | 约 30% 的查询不需要向量和 Jena |
| 向量库 HNSW 索引 | 全表扫描 O(n) → O(log n) |
| SPARQL→SQL 转换缓存 | 同类结构复用，不重复解析映射 |
| 全流程 5 秒超时兜底 | Jena 模型首次加载或 Jena/I/O 慢时不会阻塞请求 |
| TDB2 批量提交 | 减少磁盘 I/O 次数 |
| 异步增量同步 | 启动不做全量重建，后台慢慢追 |

---

## 数据库行 → RDF 映射规则

| DB 表 | RDF 映射 |
|-------|----------|
| `ont_class` | `owl:Class` + `rdfs:label`/`rdfs:comment`/`rdfs:subClassOf` |
| `ont_class_property` | `owl:DatatypeProperty` 或 `ObjectProperty` + domain/range + 特性（functional/transitive 等） |
| `ont_class_group` | `equivalent` → `owl:equivalentClass`；`disjoint` → `owl:disjointWith` |
| `ont_class_disjoint_union` | `owl:DisjointUnion` |
| `ont_class_hierarchy` | `rdfs:subClassOf`（含层次深度） |
| `ont_property_equivalent` | `owl:equivalentProperty` |
| `ont_property_disjoint` | `owl:propertyDisjointWith` |
| `ont_link_types` | `owl:ObjectProperty`（左右方向） |

**构建类的核心代码**：

```java
private void buildClassesForNs(OntModel model, String nsCode, String nsUri) {
    List<Map<String, Object>> classes = ontologyMapper.listClassesByNs(nsCode);
    for (Map<String, Object> row : classes) {
        String apiName = (String) row.get("api_name");
        String displayName = (String) row.get("display_name");
        String comment = (String) row.get("rdfs_comment");
        String parentApiName = (String) row.get("parent_api_name");

        Resource cls = model.createClass(nsUri + apiName);
        cls.addProperty(RDFS.label, model.createLiteral(displayName, "zh"));
        if (comment != null)
            cls.addProperty(RDFS.comment, model.createLiteral(comment, "zh"));
        if (parentApiName != null)
            cls.addProperty(RDFS.subClassOf, model.createResource(nsUri + parentApiName));
    }
}
```

---

## 问答管道具体实现

### 8.1 第1步：NLU 理解 —— QuestionAnalyzer.java

```java
public class QuestionAnalyzer {
    @Autowired private ChineseSegmenter segmenter;
    @Autowired private SynonymExpander synonymExpander;
    @Autowired private StringRedisTemplate redis;

    public QuestionAnalysis analyze(String question) {
        // L2 缓存
        String cacheKey = "ask:nlu:" + md5(question);
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) return JSON.parseObject(cached, QuestionAnalysis.class);

        QuestionAnalysis analysis = new QuestionAnalysis();

        // 1. 分词
        List<String> tokens = segmenter.segment(question);
        analysis.setOriginalTokens(tokens);

        // 2. 同义词扩展
        ExpandedText expanded = synonymExpander.expand(question, null);
        Set<String> allTerms = new HashSet<>(tokens);
        expanded.getSynonymMap().values().forEach(list ->
            list.forEach(e -> allTerms.add(e.getSynonym())));
        analysis.setExpandedTerms(new ArrayList<>(allTerms));

        // 3. 意图分类
        analysis.setIntentType(classifyIntent(tokens));

        // 4. 领域判定（WATER / FINANCE / GENERAL / UNKNOWN）
        analysis.setDomain(detectDomain(tokens));

        redis.opsForValue().set(cacheKey, JSON.toJSONString(analysis), 10, TimeUnit.MINUTES);
        return analysis;
    }

    private IntentType classifyIntent(List<String> tokens) {
        Set<String> dataWords = Set.of("多少", "数据", "值", "量", "费用", "收入", "是");
        Set<String> schemaWords = Set.of("字段", "属性", "类", "类型", "什么", "哪些", "定义");
        Set<String> relationWords = Set.of("关联", "关系", "链接", "属于", "连接", "父", "子");
        for (String t : tokens) {
            if (dataWords.contains(t)) return IntentType.QUERY_DATA;
            if (schemaWords.contains(t)) return IntentType.QUERY_SCHEMA;
            if (relationWords.contains(t)) return IntentType.QUERY_RELATION;
        }
        return IntentType.UNKNOWN;
    }

    private String detectDomain(List<String> tokens) {
        // 用同义词词典统计每个领域命中数，取命中最多的
    }
}
```

### 8.2 第2步：相关性判断

```java
public boolean isOntologyRelated(QuestionAnalysis analysis) {
    if (analysis.getIntentType() != IntentType.UNKNOWN) return true;
    if (analysis.getDomain() != null && !"UNKNOWN".equals(analysis.getDomain())) return true;
    long matched = ontologyMapper.countMatchingClasses(analysis.getExpandedTerms());
    return matched >= 1;
}
```

### 8.3 第3步：三路融合匹配 —— FusedMatchService.java

```java
public class FusedMatchService {
    private static final double W_JENA = 0.35;
    private static final double W_VECTOR = 0.35;
    private static final double W_KEYWORD = 0.30;

    public List<FusedMatch> match(QuestionAnalysis analysis, int topK) {
        String nsCode = resolveDomainToNsCode(analysis.getDomain());
        if (nsCode == null) return List.of();

        // L3 缓存
        String cacheKey = "ask:match:" + md5(analysis.getExpandedTerms() + nsCode);
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) return JSON.parseArray(cached, FusedMatch.class);

        // 短路：Keyword 高分直接返回
        List<ScoredEntity> kw = matchViaKeyword(analysis, nsCode, 20);
        if (kw.stream().anyMatch(r -> r.getScore() > 0.9)) {
            List<FusedMatch> r = fuse(List.of(), List.of(), kw, topK);
            redis.opsForValue().set(cacheKey, JSON.toJSONString(r), 5, TimeUnit.MINUTES);
            return r;
        }

        // 加载索引
        modelRegistry.getOrLoadModel(nsCode);
        vectorIndexService.ensureNamespaceIndexed(nsCode);

        // 并行向量 + Jena 推理
        List<ScoredEntity> vec = matchViaVector(analysis, nsCode, 20);
        List<ScoredEntity> jena = matchViaJena(nsCode, kw);  // 用 keyword 做种子

        List<FusedMatch> result = fuse(jena, vec, kw, topK);
        redis.opsForValue().set(cacheKey, JSON.toJSONString(result), 5, TimeUnit.MINUTES);
        return result;
    }

    // Keyword 匹配（复用现有 SearchMapper）
    private List<ScoredEntity> matchViaKeyword(QuestionAnalysis a, String nsCode, int limit) {
        String pat = "%" + String.join("%", a.getExpandedTerms()) + "%";
        return searchMapper.searchClassesByNs(pat, nsCode, limit).stream()
            .map(r -> new ScoredEntity((String) r.get("id"), 0.5, "KEYWORD"))
            .toList();
    }

    // 向量检索
    private List<ScoredEntity> matchViaVector(QuestionAnalysis a, String nsCode, int limit) {
        String fullText = String.join(" ", a.getExpandedTerms());
        float[] queryVec = embeddingService.embed(fullText);
        return vectorIndexService.search(queryVec, nsCode, limit).stream()
            .map(vm -> new ScoredEntity(vm.getClassId(), vm.getScore(), "VECTOR"))
            .toList();
    }

    // Jena 推理增强（只处理 keyword 结果）
    private List<ScoredEntity> matchViaJena(String nsCode, List<ScoredEntity> seeds) {
        // 见 6.1 节代码
    }

    // 融合排序
    private List<FusedMatch> fuse(List<ScoredEntity>... sources, int topK) {
        Map<String, FusedMatch> map = new HashMap<>();
        for (List<ScoredEntity> src : sources) {
            for (ScoredEntity se : src) {
                FusedMatch fm = map.computeIfAbsent(se.getId(), FusedMatch::new);
                double w = switch (se.getSource()) {
                    case "JENA" -> W_JENA; case "VECTOR" -> W_VECTOR; default -> W_KEYWORD;
                };
                fm.addScore(se.getScore() * w);
            }
        }
        return map.values().stream()
            .sorted(Comparator.comparingDouble(FusedMatch::getScore).reversed())
            .limit(topK).toList();
    }
}
```

### 8.4 第4-5步：SPARQL 转 SQL

```java
// SparqlQueryService
public String generateSparql(QuestionAnalysis analysis, List<FusedMatch> matches) {
    FusedMatch primary = matches.get(0);
    String nsCode = resolveClassNsCode(primary.getEntityId());
    List<Map<String, Object>> props = ontologyMapper.listProperties(primary.getEntityId());

    if (analysis.getIntentType() == IntentType.QUERY_DATA) {
        return sqlCache.getOrGenerate(primary, props);
    }
    // 其他意图：QUERY_SCHEMA / QUERY_RELATION → 返回结构信息
}

// SqlGeneratorService（缓存未命中时调用）
private String generateSql(FusedMatch primary, List<Map<String, Object>> props) {
    PhysicalMapping classMapping = resolver.resolveClass(primary.getEntityName());
    String mainTable = classMapping.getPhysicalTable();
    List<String> selects = new ArrayList<>();
    List<String> joins = new ArrayList<>();

    for (Map<String, Object> p : props) {
        String apiName = (String) p.get("api_name");
        PhysicalMapping pm = resolver.resolveProperty(apiName);
        if (pm == null) continue;
        if (pm.isInMainTable()) {
            selects.add(mainTable + "." + pm.getColumn() + " AS " + apiName);
        } else {
            selects.add(pm.getJoinTable() + "." + pm.getColumn() + " AS " + apiName);
            String jk = pm.getJoinTable();
            if (!joins.contains(jk))
                joins.add("LEFT JOIN " + jk + " ON " + mainTable + ".id = " + jk + "." + pm.getFkColumn());
        }
    }
    return "SELECT " + String.join(", ", selects)
         + "\nFROM " + mainTable
         + (joins.isEmpty() ? "" : "\n" + String.join("\n", joins))
         + "\nLIMIT 100";
}

// PhysicalMappingResolver
public PhysicalMapping resolveProperty(String apiName) {
    Map<String, Object> prop = ontologyMapper.findPropertyByApiName(apiName);
    if (prop == null) return null;
    String table = (String) prop.get("physical_table");
    String column = (String) prop.get("physical_column");
    String classId = (String) prop.get("class_id");
    Map<String, Object> ds = ontologyMapper.findMainDataSource(classId);
    String mainTable = (String) ds.get("physical_table");
    boolean inMain = (table == null || table.equals(mainTable));
    String fk = (String) ds.get("pk_keys");
    return new PhysicalMapping(mainTable, inMain ? null : table, column, fk);
}
```

### 8.5 控制器 —— OntologyQueryController.java

```java
@RestController
@RequestMapping("/api/ontology")
public class OntologyQueryController {

    @Autowired private QuestionAnalyzer questionAnalyzer;
    @Autowired private FusedMatchService fusedMatchService;
    @Autowired private SparqlQueryService sparqlQueryService;
    @Autowired private SqlCache sqlCache;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private SearchController searchController;

    /**
     * 自然语言问答入口
     * POST /api/ontology/ask
     * Body: { "question": "北京的水库去年供水量是多少？" }
     */
    @PostMapping("/ask")
    public R<Map<String, Object>> ask(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        long t0 = System.currentTimeMillis();

        // L1 缓存：幂等性
        String cacheKey = "ask:q:" + md5(question);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return R.ok(JSON.parseObject(cached));

        // 超时兜底（5 秒）
        try {
            CompletableFuture<R<Map<String, Object>>> future = CompletableFuture.supplyAsync(() -> {
                // 第1步：NLU
                QuestionAnalysis analysis = questionAnalyzer.analyze(question);

                // 第2步：相关性判断
                if (!fusedMatchService.isOntologyRelated(analysis)) {
                    return searchController.global(question, "all", false);
                }

                // 第3步：融合匹配
                List<FusedMatch> matches = fusedMatchService.match(analysis, 5);
                if (matches.isEmpty()) {
                    return R.ok(Map.of("message", "未找到相关本体", "question", question));
                }

                Map<String, Object> result = new HashMap<>();
                result.put("question", question);
                result.put("expandedTerms", analysis.getExpandedTerms());
                result.put("matchedEntities", matches);

                // 第4步：数据查询（仅 QUERY_DATA 类型）
                if (analysis.getIntentType() == IntentType.QUERY_DATA) {
                    List<Map<String, Object>> props = ontologyMapper.listProperties(matches.get(0).getEntityId());
                    String sql = sqlCache.getOrGenerate(matches.get(0), props);
                    List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
                    result.put("sql", sql);
                    result.put("records", data);
                    result.put("totalRecords", data.size());
                } else {
                    result.put("ontologies", matches.stream()
                        .map(m -> ontologyMapper.findClassById(m.getEntityId())).toList());
                }

                result.put("elapsed", System.currentTimeMillis() - t0);
                result.put("cacheLayer", "MISS");

                // 写入 L1 缓存
                redisTemplate.opsForValue()
                    .set(cacheKey, JSON.toJSONString(result), 5, TimeUnit.MINUTES);
                return R.ok(result);
            });

            return future.get(5000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return R.ok(Map.of(
                "message", "查询超时，请稍后重试",
                "elapsed", System.currentTimeMillis() - t0
            ));
        } catch (Exception e) {
            return R.error(500, "查询失败: " + e.getMessage());
        }
    }

    // ========== 管理端点 ==========

    /** 全量重建（异步后台执行） */
    @PostMapping("/rebuild")
    public R<String> rebuild(@RequestParam(required = false) String nsCode) {
        if (nsCode != null) {
            modelRegistry.buildNamespace(nsCode);
            return R.ok("namespace " + nsCode + " 重建完成");
        }
        // 全量重建放后台
        CompletableFuture.runAsync(() -> modelRegistry.rebuildAll());
        return R.ok("全量后台重建已启动");
    }

    /** 增量同步某个 namespace */
    @PostMapping("/sync/{nsCode}")
    public R<String> syncNamespace(@PathVariable String nsCode) {
        jenaSyncService.syncNamespace(nsCode);
        vectorSyncService.syncVectors(nsCode);
        // 清除 L3 缓存
        redisTemplate.delete(redisTemplate.keys("ask:match:" + nsCode + ":*"));
        return R.ok("namespace " + nsCode + " 增量同步完成");
    }

    /** 单条同步 */
    @PostMapping("/sync/class/{classId}")
    public R<String> syncClass(@PathVariable String classId) {
        jenaSyncService.syncClassById(classId);
        vectorSyncService.syncClassById(classId);
        return R.ok("类 " + classId + " 同步完成");
    }

    /** 统计 */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        Map<String, Object> s = new HashMap<>();
        s.put("loadedNamespaces", modelRegistry.getLoadedNamespaces());
        s.put("totalTriples", modelRegistry.getTotalTriples());
        s.put("totalEmbeddings", vectorIndexService.count());
        return R.ok(s);
    }
}
```

---

## 完整数据流示例

用户问：**"北京的水库去年供水量是多少？"**

### 各步骤输出

```
第1步 NLU：
  tokens=["北京","水库","去年","供水量","多少"]
  expandedTerms=["北京","水库","蓄水池","水库工程","去年","供水量","水量","供水数量"]
  intentType=QUERY_DATA, domain=WATER, nsCode=w_wtr

第2步 判断：相关（命中"水库"、"供水量"）

第3步 匹配（效率优化版）：
  → keyword 查：Reservoir 命中了 display_name，分 1.0
  → 分 > 0.9 → 短路，不跑 vector + Jena 独立搜索
  → 但 QUERY_DATA 需要属性信息，仍用 Jena 展开推理
  → Jena 推理：Reservoir 的 owl:equivalentClass → 蓄水池（分 0.7）
  → FUSED：[Reservoir(0.92), 蓄水池(0.64), WaterSupply(0.35)]

第4步 SPARQL（由 SqlCache 生成，若同类结构已缓存则直接复用）：
  PREFIX ns: <http://bontolink/w_wtr/>
  SELECT ?waterSupply ?name WHERE {
    ?x rdf:type ns:Reservoir .
    ?x ns:waterSupply ?waterSupply .
    ?x ns:name ?name
  }

第5步 SQL（缓存命中）：
  SELECT r.name, ws.supply_volume
  FROM wtr_reservoir r
  LEFT JOIN wtr_water_supply ws ON r.id = ws.reservoir_id
  LIMIT 100

第6步 返回：
{
  "code": 200,
  "msg": "success",
  "data": {
    "question": "北京的水库去年供水量是多少？",
    "expandedTerms": ["北京","水库","蓄水池","供水量","水量"],
    "matchedEntities": [
      { "id": "class-xxx", "name": "Reservoir", "score": 0.92 }
    ],
    "sql": "SELECT r.name, ws.supply_volume FROM ...",
    "records": [
      { "name": "密云水库", "supply_volume": 12345.6 },
      { "name": "官厅水库", "supply_volume": 8901.2 }
    ],
    "totalRecords": 2,
    "elapsed": 215,
    "cacheLayer": "L1_MISS_L2_MISS_L3_MISS"
  }
}
```

### 第二次相同问题

```json
{
  "code": 200,
  "data": {
    "question": "北京的水库去年供水量是多少？",
    "records": [{ "name": "密云水库", "supply_volume": 12345.6 }],
    "elapsed": 0.5,
    "cacheLayer": "L1_HIT"
  }
}
```

---

## 分阶段实施

### Phase 1 —— 基础设施（预估 2-3 天）

需要新增的文件：

| 文件 | 包路径 | 职责 |
|------|--------|------|
| `OntModelBuilder.java` | `ontology/service/jena/` | 按 namespace 构建 OntModel + TDB2 持久化 |
| `ModelRegistry.java` | `ontology/service/jena/` | namespace → OntModel 懒加载管理 |
| `JenaSyncService.java` | `ontology/service/jena/` | 版本追踪 + 增量同步 + 单条同步 |
| `VectorIndexService.java` | `ontology/service/vector/` | pgvector/SQLite 向量写入/检索 |
| `VectorSyncService.java` | `ontology/service/vector/` | 向量增量同步 |
| `OpenAiEmbeddingService.java` | `base/embedding/` | 替换 MockEmbeddingService |
| `V24__vector_index.sql` | `data/db/migration/` | pgvector 建表 |
| `V25__jena_version.sql` | `data/db/migration/` | jena_version 字段 |

### Phase 2 —— 问答管道（预估 3-4 天）

| 文件 | 包路径 | 职责 |
|------|--------|------|
| `QuestionAnalyzer.java` | `ontology/service/nlu/` | 分词扩词 + 意图分类 + 领域判定 |
| `FusedMatchService.java` | `ontology/service/matching/` | 三路融合匹配（含短路逻辑） |
| `SparqlQueryService.java` | `ontology/service/query/` | SPARQL 动态组装 |
| `SqlGeneratorService.java` | `ontology/service/query/` | SPARQL→SQL 核心转换 |
| `PhysicalMappingResolver.java` | `ontology/service/query/` | 本体 → 物理表/字段映射 |
| `SqlCache.java` | `ontology/service/query/` | SPARQL→SQL 转换缓存 |
| `OntologyQueryController.java` | `ontology/controller/` | `/ask` + 管理端点 |

### Phase 3 —— CRUD 联动 + 验证（预估 2 天）

- 在 `ClassMetaMapper` 的 insert/update/delete 操作中调用 `JenaSyncService.syncClassById()` + `VectorSyncService.syncClassById()`
- 在 CRUD 操作中同步清除 `ask:match:*` 缓存
- 端到端测试 + 边界情况处理

---

## 验证方法

```
1. 编译验证：
   mvnw -DskipTests clean compile -pl bontolink-ontology -am

2. 启动验证：
   启动后 → curl http://localhost:8089/bontolink-ontology/api/ontology/health → 200

3. 首次查询触发 namespace 懒加载：
   curl -X POST http://localhost:8089/bontolink-ontology/api/ontology/ask \
     -H "Content-Type: application/json" \
     -d '{"question":"水库"}'
   → 触发 namespace w_wtr 自动构建，返回匹配结果

4. 统计验证：
   curl http://localhost:8089/bontolink-ontology/api/ontology/stats
   → 显示已加载的 namespace 列表和三元组数

5. 第二次查询（缓存命中）：
   重复步骤 3
   → elapsed 应在 1ms 级别，cacheLayer 显示 L1_HIT

6. 数据查询：
   curl -X POST ... -d '{"question":"水库供水量是多少"}'
   → 返回 SQL + records 数据

7. 不相关问题降级：
   curl -X POST ... -d '{"question":"今天天气怎么样"}'
   → 降级到全局搜索 / 返回 0 匹配结果

8. 增量同步：
   修改一个类的 display_name →
   curl -X POST http://.../api/ontology/sync/class/{classId}
   → 再次查询应看到新名称

9. 效率压测：
   重复同问题 100 次 → 验证 L1 命中率 > 95%
   不同 namespace 查询 → 验证 namespace 隔离
```
