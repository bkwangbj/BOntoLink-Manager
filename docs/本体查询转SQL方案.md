# BOnotLink-Manager 本体查询转 SQL 实现方案

## 一、架构概述

```
自然语言问题
    ↓
[1] NLU (自然语言理解) + LLM
    → 提取: 实体/属性/条件/聚合
    ↓
[2] OntologyQueryPlanner (本体规划器)
    → 查找: 对象类型/物理表/关联关系
    ↓
[3] SQLGenerator (SQL 生成器)
    → 生成: SELECT/JOIN/WHERE/GROUP BY
    ↓
[4] DataSourceRouter (数据源路由)
    → 执行: 单源/多源查询
    ↓
[5] ResultFormatter (结果格式化)
    → 输出: 结构化数据 + LLM 总结
```

---

## 二、核心组件实现

### 1. 自然语言查询服务 (NaturalLanguageQueryService)

**路径**: `backend/src/main/java/com/beiktech/bontolink/service/NaturalLanguageQueryService.java`

```java
@Service
@RequiredArgsConstructor
public class NaturalLanguageQueryService {
    
    private final LLMClient llmClient;
    private final OntologyMetadataManager ontologyManager;
    private final OntologyQueryPlanner planner;
    private final SQLGenerator sqlGenerator;
    private final DataSourceRouter router;
    
    public QueryResult query(String question) {
        // 1. LLM 理解用户意图
        QueryIntent intent = llmClient.understandQuery(
            buildPrompt(question),
            question
        );
        
        // 2. 生成查询计划
        QueryPlan plan = planner.plan(intent);
        
        // 3. 生成 SQL
        String sql = sqlGenerator.generate(plan);
        
        // 4. 执行查询
        List<Map<String, Object>> data = router.execute(plan, sql);
        
        // 5. 格式化结果
        return QueryResult.builder()
            .sql(sql)
            .data(data)
            .rowCount(data.size())
            .build();
    }
    
    private String buildPrompt(String question) {
        return ontologyManager.buildLLMPrompt() + "\n\n用户问题: " + question;
    }
}
```

---

### 2. 本体元数据管理器 (OntologyMetadataManager)

**路径**: `backend/src/main/java/com/beiktech/bontolink/manager/OntologyMetadataManager.java`

```java
@Component
@RequiredArgsConstructor
public class OntologyMetadataManager {
    
    private final OntologyMapper ontologyMapper;
    private final DataSourceMapper dataSourceMapper;
    
    private Map<String, OntClassMeta> classCache;
    private Map<String, List<LinkMapping>> linkCache;
    
    @PostConstruct
    public void init() {
        refreshCache();
    }
    
    public void refreshCache() {
        // 加载所有对象类型
        List<Map<String, Object>> classes = ontologyMapper.listClassesForGraph();
        classCache = classes.stream()
            .map(this::buildClassMeta)
            .collect(Collectors.toMap(OntClassMeta::getId, c -> c));
        
        // 加载所有关联关系
        linkCache = ontologyMapper.listAllLinkMappings().stream()
            .collect(Collectors.groupingBy(LinkMapping::getClassFromId));
    }
    
    public OntClassMeta getClass(String classId) {
        return classCache.get(classId);
    }
    
    public List<OntClassMeta> searchClassesByName(String name) {
        return classCache.values().stream()
            .filter(c -> c.getDisplayName().contains(name) 
                      || c.getRdfsLabel().contains(name))
            .collect(Collectors.toList());
    }
    
    public String buildLLMPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("# 可用的本体定义\n\n");
        
        // 对象类型
        prompt.append("## 对象类型\n");
        for (OntClassMeta cls : classCache.values()) {
            prompt.append(String.format("- %s (%s)\n", 
                cls.getDisplayName(), cls.getApiName()));
            prompt.append(String.format("  物理表: %s\n", cls.getPhysicalTable()));
            prompt.append("  属性:\n");
            for (PropertyMeta prop : cls.getProperties()) {
                prompt.append(String.format("    - %s (%s): %s\n",
                    prop.getDisplayName(), prop.getApiName(), prop.getDataType()));
            }
        }
        
        // 关系
        prompt.append("\n## 关系\n");
        for (List<LinkMapping> links : linkCache.values()) {
            for (LinkMapping link : links) {
                OntClassMeta from = classCache.get(link.getClassFromId());
                OntClassMeta to = classCache.get(link.getClassToId());
                prompt.append(String.format("- %s --[%s]--> %s\n  连接: %s.%s = %s.%s\n",
                    from.getDisplayName(), link.getLinkTypeName(), to.getDisplayName(),
                    from.getPhysicalTable(), link.getFieldFrom(),
                    to.getPhysicalTable(), link.getFieldTo()));
            }
        }
        
        return prompt.toString();
    }
    
    @Data
    @Builder
    public static class OntClassMeta {
        private String id;
        private String apiName;
        private String displayName;
        private String rdfsLabel;
        private String physicalTable;
        private String dataSourceCode;
        private List<PropertyMeta> properties;
    }
    
    @Data
    public static class PropertyMeta {
        private String apiName;
        private String displayName;
        private String dataType;
        private String physicalColumn;
    }
    
    @Data
    public static class LinkMapping {
        private String classFromId;
        private String classToId;
        private String linkTypeName;
        private String fieldFrom;
        private String fieldTo;
    }
}
```

---

### 3. 本体查询规划器 (OntologyQueryPlanner)

**路径**: `backend/src/main/java/com/beiktech/bontolink/planner/OntologyQueryPlanner.java`

```java
@Component
@RequiredArgsConstructor
public class OntologyQueryPlanner {
    
    private final OntologyMetadataManager ontologyManager;
    
    public QueryPlan plan(QueryIntent intent) {
        // 1. 找到目标对象类型
        List<OntClassMeta> targetClasses = intent.getEntities().stream()
            .flatMap(e -> ontologyManager.searchClassesByName(e).stream())
            .collect(Collectors.toList());
        
        if (targetClasses.isEmpty()) {
            throw new IllegalArgumentException("未找到匹配的对象类型");
        }
        
        // 2. 解析属性到物理列
        List<SelectColumn> selectColumns = resolveProperties(targetClasses, intent.getProperties());
        
        // 3. 解析关联关系
        List<JoinClause> joins = resolveJoins(targetClasses);
        
        // 4. 解析条件
        List<Condition> conditions = parseConditions(intent.getConditions(), targetClasses);
        
        return QueryPlan.builder()
            .mainTable(targetClasses.get(0).getPhysicalTable())
            .mainTableAlias("t0")
            .dataSourceCode(targetClasses.get(0).getDataSourceCode())
            .selectColumns(selectColumns)
            .joins(joins)
            .conditions(conditions)
            .build();
    }
    
    private List<SelectColumn> resolveProperties(List<OntClassMeta> classes, List<String> propertyNames) {
        List<SelectColumn> columns = new ArrayList<>();
        int tableIndex = 0;
        
        for (OntClassMeta cls : classes) {
            String tableAlias = "t" + tableIndex++;
            for (String propName : propertyNames) {
                PropertyMeta prop = cls.getProperties().stream()
                    .filter(p -> p.getDisplayName().equals(propName) 
                              || p.getApiName().equals(propName))
                    .findFirst()
                    .orElse(null);
                
                if (prop != null) {
                    columns.add(SelectColumn.builder()
                        .tableAlias(tableAlias)
                        .columnName(prop.getPhysicalColumn())
                        .alias(prop.getApiName())
                        .build());
                }
            }
        }
        
        return columns;
    }
    
    private List<JoinClause> resolveJoins(List<OntClassMeta> classes) {
        // 如果只有一个类,不需要 JOIN
        if (classes.size() <= 1) {
            return Collections.emptyList();
        }
        
        List<JoinClause> joins = new ArrayList<>();
        // 查找类之间的关联关系
        // 简化版: 只处理相邻类的直接关联
        // TODO: 实现最短路径算法处理间接关联
        
        return joins;
    }
}
```

---

### 4. SQL 生成器 (SQLGenerator)

**路径**: `backend/src/main/java/com/beiktech/bontolink/generator/SQLGenerator.java`

```java
@Component
public class SQLGenerator {
    
    public String generate(QueryPlan plan) {
        StringBuilder sql = new StringBuilder();
        
        // SELECT
        sql.append("SELECT ");
        sql.append(plan.getSelectColumns().stream()
            .map(c -> String.format("%s.%s AS %s", 
                c.getTableAlias(), c.getColumnName(), c.getAlias()))
            .collect(Collectors.joining(", ")));
        
        // FROM
        sql.append("\nFROM ").append(plan.getMainTable())
           .append(" AS ").append(plan.getMainTableAlias());
        
        // JOIN
        for (JoinClause join : plan.getJoins()) {
            sql.append("\n").append(join.getJoinType()).append(" JOIN ")
               .append(join.getTargetTable()).append(" AS ").append(join.getTargetAlias())
               .append(" ON ").append(join.getOnCondition());
        }
        
        // WHERE
        if (!plan.getConditions().isEmpty()) {
            sql.append("\nWHERE ");
            sql.append(plan.getConditions().stream()
                .map(Condition::toSql)
                .collect(Collectors.joining(" AND ")));
        }
        
        // GROUP BY
        if (plan.getGroupBy() != null && !plan.getGroupBy().isEmpty()) {
            sql.append("\nGROUP BY ");
            sql.append(String.join(", ", plan.getGroupBy()));
        }
        
        // ORDER BY
        if (plan.getOrderBy() != null) {
            sql.append("\nORDER BY ").append(plan.getOrderBy());
        }
        
        // LIMIT
        if (plan.getLimit() != null) {
            sql.append("\nLIMIT ").append(plan.getLimit());
        }
        
        return sql.toString();
    }
}
```

---

### 5. 数据源路由器 (DataSourceRouter)

**路径**: `backend/src/main/java/com/beiktech/bontolink/router/DataSourceRouter.java`

```java
@Component
@RequiredArgsConstructor
public class DataSourceRouter {
    
    private final DataSourceFactory dataSourceFactory;
    
    public List<Map<String, Object>> execute(QueryPlan plan, String sql) {
        DataSource ds = dataSourceFactory.getDataSource(plan.getDataSourceCode());
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        
        try {
            return jdbc.queryForList(sql);
        } catch (DataAccessException e) {
            throw new RuntimeException("SQL 执行失败: " + e.getMessage(), e);
        }
    }
}
```

---

## 三、数据模型

### QueryIntent (查询意图)
```java
@Data
@Builder
public class QueryIntent {
    private List<String> entities;      // 实体: ["水利服务企业"]
    private List<String> properties;    // 属性: ["企业名称", "注册资本"]
    private List<String> conditions;    // 条件: ["注册资本 > 1000000"]
    private List<String> aggregations;  // 聚合: ["COUNT(*)", "SUM(注册资本)"]
    private String orderBy;             // 排序: "注册资本 DESC"
    private Integer limit;              // 限制: 10
}
```

### QueryPlan (查询计划)
```java
@Data
@Builder
public class QueryPlan {
    private String mainTable;
    private String mainTableAlias;
    private String dataSourceCode;
    private List<SelectColumn> selectColumns;
    private List<JoinClause> joins;
    private List<Condition> conditions;
    private List<String> groupBy;
    private String orderBy;
    private Integer limit;
}
```

---

## 四、API 接口设计

### POST /api/nl-query
自然语言查询接口

**请求**:
```json
{
  "question": "查询注册资本超过100万的水利服务企业名称"
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "sql": "SELECT t0.company_name AS company_name, t0.capital AS capital FROM t_water_service_company AS t0 WHERE t0.capital > 1000000",
    "rows": [
      {"company_name": "XX水利公司", "capital": 5000000},
      {"company_name": "YY水务集团", "capital": 3000000}
    ],
    "rowCount": 2,
    "executionTime": 125
  }
}
```

### GET /api/nl-query/plan?question=...
仅生成查询计划(不执行)

**响应**:
```json
{
  "intent": {
    "entities": ["水利服务企业"],
    "properties": ["企业名称", "注册资本"],
    "conditions": ["注册资本 > 1000000"]
  },
  "plan": {
    "mainTable": "t_water_service_company",
    "selectColumns": ["company_name", "capital"],
    "conditions": ["capital > 1000000"]
  },
  "sql": "SELECT ..."
}
```

---

## 五、实施步骤

### 阶段1: 基础框架 (1-2天)
1. ✅ 创建 `OntologyMetadataManager` - 缓存本体元数据
2. ✅ 创建 `QueryIntent` / `QueryPlan` 数据模型
3. ✅ 创建 `NaturalLanguageQueryController` - REST API

### 阶段2: LLM 集成 (2-3天)
1. ✅ 集成 LLM Client (DeepSeek/OpenAI)
2. ✅ 实现 `buildLLMPrompt()` - 本体定义转 Prompt
3. ✅ 实现 `understandQuery()` - LLM 理解问题
4. ✅ Prompt 调优 - 提高识别准确率

### 阶段3: 查询规划 (2-3天)
1. ✅ 实现 `OntologyQueryPlanner`
2. ✅ 属性映射: 显示名 → 物理列
3. ✅ 关系解析: 对象类型 → JOIN 条件
4. ✅ 条件解析: 自然语言 → SQL WHERE

### 阶段4: SQL 生成与执行 (1-2天)
1. ✅ 实现 `SQLGenerator`
2. ✅ 实现 `DataSourceRouter`
3. ✅ 错误处理与日志

### 阶段5: 前端集成 (2-3天)
1. ✅ 创建查询页面 `NaturalLanguageQuery.vue`
2. ✅ 展示 SQL 语句
3. ✅ 展示查询结果(表格)
4. ✅ 支持导出结果

---

## 六、技术难点与解决方案

### 1. 多表关联推理
**问题**: 用户问题涉及多个对象类型,如何找到它们之间的关联路径?

**解决**: 
- 使用**图算法**(BFS)在 `link_type_mapping` 上搜索最短路径
- 或限制只支持直接关联(1-hop)

### 2. 自然语言歧义
**问题**: "注册资本超过100万" → 100万 = 1000000?

**解决**:
- LLM Prompt 中明确单位换算规则
- 后处理: 正则提取数字并归一化

### 3. 聚合查询
**问题**: "统计各地区的企业数量" → GROUP BY + COUNT

**解决**:
- LLM 输出 `aggregations` 字段
- SQL Generator 识别聚合函数并生成 GROUP BY

### 4. 跨数据源查询
**问题**: 多个对象类型在不同数据库

**解决**:
- 分别查询 + 内存 JOIN (简单实现)
- 或使用联邦查询引擎 (Presto/Trino)

---

## 七、对比 bonotlink-ui

| 特性 | bonotlink-ui | BOnotLink-Manager |
|------|--------------|-------------------|
| 本体存储 | JSON 文件 (classpath:ontology/*.json) | PostgreSQL/SQLite 数据库 |
| 查询语言 | SPARQL | 直接 SQL |
| SQL 转换 | Ontop (SPARQL → SQL) | 自研 (本体 → SQL) |
| 多数据源 | 支持 | 支持(需扩展) |
| LLM 规划 | DeepSeek + 深度思考 | 待实现 |
| 前端集成 | Vue3 + SSE 流式 | Vue3 (待开发) |

---

## 八、下一步行动

### 立即可做
1. 创建 `OntologyMetadataManager` 并实现缓存
2. 创建 REST API `/api/nl-query`
3. 集成 LLM Client (复用 bonotlink-ui 的实现)

### 需要讨论
1. 是否支持 SPARQL (像 bonotlink-ui)?
2. 是否需要支持跨库 JOIN?
3. LLM API Key 配置

---

## 九、参考资料

- [bonotlink-ui QueryService.java](f:/aiproject/bonotlink-ui/app/backend/src/main/java/com/bontolink/query/service/QueryService.java)
- [bonotlink-ui OntologyManager.java](f:/aiproject/bonotlink-ui/app/backend/src/main/java/com/bontolink/query/ontology/OntologyManager.java)
- [Ontop 框架文档](https://ontop-vkg.org/)
- [GraphController.java](../backend/src/main/java/com/beiktech/bontolink/controller/GraphController.java) - 本体图谱 API
