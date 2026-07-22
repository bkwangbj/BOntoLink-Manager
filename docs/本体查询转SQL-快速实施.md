# 本体查询转SQL - 快速实施方案

## 🎯 目标
在 BOnotLink-Manager 中实现: **自然语言问题 → 本体查询 → SQL 执行 → 返回结果**

---

## 📋 核心组件清单

### 必需组件 (5个)

| 组件 | 文件 | 功能 | 优先级 |
|-----|------|------|-------|
| 1️⃣ 本体元数据管理器 | `OntologyMetadataManager.java` | 缓存本体定义,提供查询接口 | ⭐⭐⭐ |
| 2️⃣ 查询规划器 | `OntologyQueryPlanner.java` | 将查询意图转为查询计划 | ⭐⭐⭐ |
| 3️⃣ SQL生成器 | `SQLGenerator.java` | 生成标准SQL | ⭐⭐⭐ |
| 4️⃣ 数据源路由器 | `DataSourceRouter.java` | 执行SQL查询 | ⭐⭐ |
| 5️⃣ 查询服务 | `NaturalLanguageQueryService.java` | 统一入口 | ⭐⭐ |

### 可选组件 (2个)

| 组件 | 功能 | 说明 |
|-----|------|------|
| LLM集成 | 自然语言理解 | 可先用规则引擎代替 |
| 结果格式化 | LLM总结 | 可先返回原始数据 |

---

## 🔧 实施步骤

### 阶段1: 基础框架 (3-5天)

#### Step 1.1: 创建本体元数据管理器

```java
// OntologyMetadataManager.java
@Component
public class OntologyMetadataManager {
    
    @PostConstruct
    public void init() {
        // 从数据库加载
        loadClassMetadata();    // ont_class + ont_class_property
        loadLinkMetadata();     // link_type + link_type_mapping
        loadDataSources();      // ont_class_ds
    }
    
    // 核心方法
    public OntClass findClassByName(String name) { }
    public List<Property> getProperties(String classId) { }
    public List<LinkMapping> getLinkMappings(String classId) { }
    public String getPhysicalTable(String classId) { }
    public String getDataSource(String classId) { }
}
```

**数据库查询**:
```sql
-- 加载对象类型
SELECT c.id, c.api_name, c.display_name, c.rdfs_label,
       ds.physical_table, ds.ds_code
FROM ont_class c
LEFT JOIN ont_class_ds ds ON c.id = ds.class_id
WHERE c.status = 1

-- 加载属性
SELECT class_id, api_name, display_name, data_type, physical_column
FROM ont_class_property
WHERE status = 1

-- 加载链接映射
SELECT lt.id, lt.display_name,
       m.class_from_id, m.class_to_id, 
       m.field_from, m.field_to
FROM link_type lt
JOIN link_type_mapping m ON lt.id = m.link_type_id
WHERE lt.status = 'active'
```

#### Step 1.2: 创建简单查询规划器

```java
// SimpleQueryPlanner.java (先不用LLM)
@Component
public class SimpleQueryPlanner {
    
    public QueryPlan plan(SimpleQuery query) {
        // 示例: { classNames: ["水利服务企业"], properties: ["企业名称", "注册资本"] }
        
        OntClass mainClass = ontologyManager.findClassByName(query.getClassName());
        List<Property> props = ontologyManager.getProperties(mainClass.getId());
        
        return QueryPlan.builder()
            .mainTable(mainClass.getPhysicalTable())
            .dataSource(mainClass.getDataSource())
            .selectColumns(mapProperties(props))
            .build();
    }
}
```

#### Step 1.3: 创建SQL生成器

```java
// SQLGenerator.java
@Component
public class SQLGenerator {
    
    public String generate(QueryPlan plan) {
        StringBuilder sql = new StringBuilder();
        
        // SELECT
        sql.append("SELECT ");
        sql.append(plan.getSelectColumns().stream()
            .map(c -> c.getColumnName() + " AS " + c.getAlias())
            .collect(Collectors.joining(", ")));
        
        // FROM
        sql.append(" FROM ").append(plan.getMainTable());
        
        // WHERE
        if (!plan.getConditions().isEmpty()) {
            sql.append(" WHERE ");
            sql.append(plan.getConditions().stream()
                .map(Condition::toSql)
                .collect(Collectors.joining(" AND ")));
        }
        
        return sql.toString();
    }
}
```

#### Step 1.4: 创建数据源路由器

```java
// DataSourceRouter.java
@Component
public class DataSourceRouter {
    
    @Autowired
    private Map<String, DataSource> dataSourceMap;  // 从 application.yml 注入
    
    public List<Map<String, Object>> execute(String dsCode, String sql) {
        DataSource ds = dataSourceMap.get(dsCode);
        if (ds == null) {
            throw new IllegalArgumentException("数据源不存在: " + dsCode);
        }
        
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        return jdbc.queryForList(sql);
    }
}
```

#### Step 1.5: 创建查询服务

```java
// NaturalLanguageQueryService.java
@RestController
@RequestMapping("/api/nlquery")
public class NaturalLanguageQueryController {
    
    @PostMapping
    public R<QueryResult> query(@RequestBody QueryRequest request) {
        // 1. 简单解析(先不用LLM)
        SimpleQuery simpleQuery = parseSimple(request.getQuestion());
        
        // 2. 生成计划
        QueryPlan plan = planner.plan(simpleQuery);
        
        // 3. 生成SQL
        String sql = sqlGenerator.generate(plan);
        
        // 4. 执行查询
        List<Map<String, Object>> data = router.execute(plan.getDataSource(), sql);
        
        // 5. 返回结果
        return R.ok(QueryResult.builder()
            .sql(sql)
            .data(data)
            .rowCount(data.size())
            .build());
    }
    
    // 简单解析(正则匹配)
    private SimpleQuery parseSimple(String question) {
        // "查询水利服务企业的企业名称和注册资本"
        // → className: "水利服务企业", properties: ["企业名称", "注册资本"]
    }
}
```

---

### 阶段2: 集成LLM (2-3天)

#### Step 2.1: 添加 LLM Client

```java
// LLMClient.java
@Component
public class LLMClient {
    
    @Value("${llm.api.key}")
    private String apiKey;
    
    public QueryIntent understand(String prompt, String question) {
        String systemPrompt = buildSystemPrompt(prompt);
        String response = callLLM(systemPrompt, question);
        return JSON.parseObject(response, QueryIntent.class);
    }
    
    private String callLLM(String system, String user) {
        // 调用 DeepSeek / GPT API
    }
}
```

#### Step 2.2: 构建 Prompt

```java
public String buildSystemPrompt() {
    return ontologyManager.buildLLMPrompt() + """
        
        请分析用户问题,输出JSON:
        {
          "entities": ["对象类型名"],
          "properties": ["属性名"],
          "conditions": ["过滤条件"],
          "aggregations": [],
          "orderBy": null
        }
        """;
}
```

---

### 阶段3: 增强功能 (3-5天)

#### 支持 JOIN (多表关联)

```java
// 在 OntologyQueryPlanner 中
public QueryPlan plan(QueryIntent intent) {
    // ...
    
    // 解析关联关系
    List<JoinClause> joins = new ArrayList<>();
    if (intent.getEntities().size() > 1) {
        // 查找连接路径
        for (int i = 0; i < classes.size() - 1; i++) {
            OntClass from = classes.get(i);
            OntClass to = classes.get(i + 1);
            LinkMapping link = findLinkBetween(from.getId(), to.getId());
            
            joins.add(JoinClause.builder()
                .joinType("LEFT JOIN")
                .targetTable(to.getPhysicalTable())
                .onCondition(String.format("%s.%s = %s.%s",
                    from.getPhysicalTable(), link.getFieldFrom(),
                    to.getPhysicalTable(), link.getFieldTo()))
                .build());
        }
    }
    
    return plan;
}
```

#### 支持聚合查询

```java
// 识别聚合关键词: "统计", "总数", "平均"
if (question.contains("统计") || question.contains("总数")) {
    plan.setAggregations(List.of(
        Aggregation.builder()
            .function("COUNT")
            .column("*")
            .alias("total_count")
            .build()
    ));
}
```

---

## 📊 测试用例

### 测试1: 单表查询
```json
{
  "question": "查询水利服务企业的企业名称和注册资本"
}
```

**预期SQL**:
```sql
SELECT company_name AS 企业名称, 
       capital AS 注册资本
FROM t_water_service_company
```

### 测试2: 带条件查询
```json
{
  "question": "查询注册资本超过100万的水利服务企业"
}
```

**预期SQL**:
```sql
SELECT company_name, capital
FROM t_water_service_company
WHERE capital > 1000000
```

### 测试3: 关联查询
```json
{
  "question": "查询每个水利服务企业承接的服务项目数量"
}
```

**预期SQL**:
```sql
SELECT c.company_name, COUNT(p.id) AS project_count
FROM t_water_service_company c
LEFT JOIN t_service_project p ON c.id = p.company_id
GROUP BY c.company_name
```

---

## 🔍 调试工具

### 前端调试页面

创建 `frontend/src/views/debug/NLQuery.vue`:

```vue
<template>
  <div class="page">
    <el-input v-model="question" placeholder="输入自然语言问题" />
    <el-button @click="query">查询</el-button>
    
    <div v-if="result">
      <h3>生成的SQL:</h3>
      <pre>{{ result.sql }}</pre>
      
      <h3>查询结果:</h3>
      <el-table :data="result.data">
        <el-table-column 
          v-for="col in columns" 
          :key="col" 
          :prop="col" 
          :label="col" 
        />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { nlQueryApi } from '@/api'

const question = ref('')
const result = ref(null)

async function query() {
  result.value = await nlQueryApi.query(question.value)
}
</script>
```

---

## 📝 配置文件

### application.yml

```yaml
# LLM 配置
llm:
  provider: deepseek  # deepseek / openai
  api:
    key: ${LLM_API_KEY:your-api-key}
    base-url: https://api.deepseek.com
    model: deepseek-chat

# 查询配置
query:
  cache:
    enabled: true
    ttl: 300  # 缓存5分钟
  max-rows: 10000  # 最大返回行数
```

---

## 🎯 MVP 功能清单

### 第1周 (阶段1)
- [x] 本体元数据管理器
- [x] 简单查询规划器(正则匹配)
- [x] SQL生成器(单表)
- [x] 数据源路由器
- [x] REST API + 前端调试页面

### 第2周 (阶段2)
- [ ] 集成 LLM (DeepSeek)
- [ ] Prompt 工程
- [ ] 支持复杂查询意图解析

### 第3周 (阶段3)
- [ ] 支持 JOIN (多表关联)
- [ ] 支持聚合查询
- [ ] 支持排序/分页
- [ ] 结果格式化 + LLM 总结

---

## 🔗 参考资料

- bonotlink-ui 源码: `f:/aiproject/bonotlink-ui/app/backend/src/main/java/com/bontolink/query/`
- 本体导出工具: `tools/convert-to-bonotlink-ui.ps1`
- GraphController: `backend/src/main/java/com/beiktech/bontolink/controller/GraphController.java`

---

## 💡 关键技术点

1. **本体定义缓存**: 启动时加载,避免频繁查数据库
2. **查询计划生成**: 基于本体定义的智能规划
3. **SQL模板引擎**: 使用 StringBuilder 或 Freemarker
4. **多数据源支持**: 基于 `ont_class_ds` 的动态路由
5. **LLM Prompt工程**: 清晰的 schema 定义 + few-shot examples
