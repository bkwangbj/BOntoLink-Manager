# 本体JSON生成与数据查询完整方案

## 一、JSON格式生成

### ✅ 已实现: convert-to-bonotlink-ui-full.ps1

**输出格式**: 完全兼容 `jjdata-ontology.json` 的标准格式

```json
{
  "promptHints": [
    "查询规划提示1",
    "查询规划提示2"
  ],
  "entities": [
    {
      "name": "实体名",
      "table": "物理表名",
      "dataSource": "数据源代码",
      "description": "实体描述",
      "owlClass": "OWL类名",
      "owlPrefix": "owl前缀",
      "fields": [
        {
          "name": "字段名",
          "label": "显示名",
          "type": "VARCHAR|INTEGER|DECIMAL|...",
          "owlProperty": "owl属性名",
          "remark": "备注",
          "metric": true,
          "dickey": "字典名",
          "dictValues": [
            { "code": "01", "label": "选项1" },
            { "code": "02", "label": "选项2" }
          ]
        }
      ]
    }
  ],
  "relationships": [
    {
      "name": "实体1→实体2",
      "from": {
        "entity": "实体1",
        "field": "源字段"
      },
      "to": {
        "entity": "实体2",
        "field": "目标字段"
      },
      "joinType": "SAME_DB_STRONG | SAME_DB_WEAK | CROSS_DB_WEAK",
      "description": "关系描述"
    }
  ],
  "dictionaries": [
    {
      "name": "字典名",
      "entries": [
        { "code": "01", "label": "选项1" }
      ]
    }
  ]
}
```

### 使用方法

```powershell
# 启动 BOnotLink-Manager 后端
cd f:\aiProject\BOnotLink-Manager\backend
mvnw spring-boot:run

# 执行转换
cd f:\aiProject\BOnotLink-Manager\tools
.\convert-to-bonotlink-ui-full.ps1

# 输出: f:/aiproject/bonotlink-ui/app/backend/src/main/resources/ontology/water-service-ontology.json
```

---

## 二、数据查询解决方案

### 方案对比

| 方案 | 技术栈 | 优势 | 劣势 | 适用场景 |
|-----|--------|------|------|---------|
| **方案A: 直接SQL查询** | JDBC | 简单快速 | 需手动编写JOIN | 简单单表查询 |
| **方案B: 基于本体规划的SQL生成** | LLM + 本体元数据 | 灵活,支持自然语言 | 需实现规划器 | 复杂多表查询 |
| **方案C: SPARQL + Ontop** | Ontop框架 | 标准语义查询 | 配置复杂 | 需要SPARQL支持 |
| **方案D: 导入bonotlink-ui** | 现成系统 | 无需开发 | 需维护两套系统 | 快速上线 |

---

### ⭐ 推荐: 方案B (基于本体规划的SQL生成)

#### 为什么推荐方案B?

1. **最适合BOnotLink-Manager的定位**: 本体管理系统,核心是本体定义,查询是附加功能
2. **开发成本适中**: 不需要引入Ontop等重型框架,可分阶段实施
3. **灵活性强**: 可以根据实际需求逐步增强功能
4. **无额外部署**: 不需要维护两套系统

---

### 方案B 实施路线图

#### 第一阶段: MVP版本 (3-5天)

**目标**: 支持简单的单表查询

```
用户输入: "查询水利服务企业的企业名称和注册资本"
↓
SimpleQueryParser (正则解析)
  → 识别: 实体名="水利服务企业", 属性=["企业名称","注册资本"]
↓
OntologyMetadataManager (查找本体)
  → 找到: class_id, physical_table, properties
↓
SimpleSQL Generator
  → 生成: SELECT company_name, capital FROM t_water_service WHERE 1=1
↓
JDBC执行 → 返回结果
```

**核心代码**:

```java
// 1. 简单查询解析器
@Component
public class SimpleQueryParser {
    public QueryIntent parse(String question) {
        // 正则匹配: "查询【实体名】的【属性1】和【属性2】"
        Pattern p = Pattern.compile("查询(.+?)的(.+)");
        Matcher m = p.matcher(question);
        if (m.find()) {
            String entity = m.group(1).trim();
            String[] props = m.group(2).split("[和、,]");
            return new QueryIntent(entity, Arrays.asList(props));
        }
        throw new IllegalArgumentException("无法解析问题");
    }
}

// 2. 本体元数据管理器
@Component
public class OntologyMetadataManager {
    
    private Map<String, ClassMetadata> classCache;
    
    @PostConstruct
    public void init() {
        // 从数据库加载
        List<OntClass> classes = ontClassMapper.selectAll();
        classCache = classes.stream()
            .collect(Collectors.toMap(OntClass::getDisplayName, this::buildMetadata));
    }
    
    public ClassMetadata findByName(String name) {
        return classCache.get(name);
    }
    
    @Data
    public static class ClassMetadata {
        String classId;
        String displayName;
        String physicalTable;
        String dataSource;
        List<PropertyMetadata> properties;
    }
}

// 3. 简单SQL生成器
@Component
public class SimpleSQLGenerator {
    public String generate(ClassMetadata meta, List<String> propNames) {
        List<PropertyMetadata> props = meta.getProperties().stream()
            .filter(p -> propNames.contains(p.getDisplayName()))
            .collect(Collectors.toList());
        
        String columns = props.stream()
            .map(PropertyMetadata::getPhysicalColumn)
            .collect(Collectors.joining(", "));
        
        return String.format("SELECT %s FROM %s", columns, meta.getPhysicalTable());
    }
}

// 4. 查询服务
@RestController
@RequestMapping("/api/nlquery")
public class NLQueryController {
    
    @Autowired SimpleQueryParser parser;
    @Autowired OntologyMetadataManager ontologyMgr;
    @Autowired SimpleSQLGenerator sqlGen;
    @Autowired DataSourceFactory dsFact;
    
    @PostMapping("/simple")
    public R<QueryResult> query(@RequestBody QueryRequest req) {
        // 1. 解析
        QueryIntent intent = parser.parse(req.getQuestion());
        
        // 2. 查找本体
        ClassMetadata meta = ontologyMgr.findByName(intent.getEntityName());
        
        // 3. 生成SQL
        String sql = sqlGen.generate(meta, intent.getPropertyNames());
        
        // 4. 执行
        DataSource ds = dsFact.getDataSource(meta.getDataSource());
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        List<Map<String, Object>> data = jdbc.queryForList(sql);
        
        // 5. 返回
        return R.ok(QueryResult.builder()
            .sql(sql)
            .data(data)
            .rowCount(data.size())
            .build());
    }
}
```

**测试**:

```bash
POST /api/nlquery/simple
{
  "question": "查询水利服务企业的企业名称和注册资本"
}

# 响应
{
  "code": 0,
  "data": {
    "sql": "SELECT company_name, capital FROM t_water_service_company",
    "data": [
      {"company_name": "XX水利", "capital": 5000000},
      {"company_name": "YY水务", "capital": 3000000}
    ],
    "rowCount": 2
  }
}
```

---

#### 第二阶段: 支持条件过滤 (2-3天)

**目标**: 支持 WHERE 条件

```java
// 增强解析器
public QueryIntent parse(String question) {
    // "查询注册资本大于100万的水利服务企业的企业名称"
    // → conditions: ["capital > 1000000"]
}

// 增强SQL生成器
public String generate(ClassMetadata meta, List<String> props, List<Condition> conditions) {
    String sql = baseSql;
    if (!conditions.isEmpty()) {
        sql += " WHERE " + conditions.stream()
            .map(Condition::toSql)
            .collect(Collectors.joining(" AND "));
    }
    return sql;
}
```

---

#### 第三阶段: 支持多表JOIN (3-5天)

**目标**: 支持跨对象类型查询

```java
// 关系解析器
@Component
public class RelationshipResolver {
    
    public List<JoinClause> resolveJoins(List<String> entityNames) {
        List<JoinClause> joins = new ArrayList<>();
        
        for (int i = 0; i < entityNames.size() - 1; i++) {
            String from = entityNames.get(i);
            String to = entityNames.get(i + 1);
            
            // 从 link_type_mapping 查找连接字段
            LinkMapping mapping = linkTypeMapper.findMapping(from, to);
            
            joins.add(new JoinClause(
                mapping.getJoinType(),
                to,
                mapping.getOnCondition()
            ));
        }
        
        return joins;
    }
}
```

**示例**:

```
问题: "查询承接了服务项目的水利服务企业的企业名称和项目名称"

解析:
  - entities: ["水利服务企业", "服务项目"]
  - properties: ["企业名称", "项目名称"]

生成SQL:
SELECT e.company_name, p.project_name
FROM t_water_service_company e
INNER JOIN t_service_project p ON e.company_id = p.company_id
```

---

#### 第四阶段: 集成LLM (2-3天)

**目标**: 使用LLM理解复杂自然语言

```java
@Component
public class LLMQueryPlanner {
    
    @Value("${llm.api.key}")
    private String apiKey;
    
    public QueryIntent understand(String question) {
        String systemPrompt = ontologyMgr.buildLLMPrompt();
        String userPrompt = """
            请分析问题并输出JSON:
            {
              "entities": ["对象类型"],
              "properties": ["属性"],
              "conditions": ["条件"],
              "aggregations": ["聚合"]
            }
            
            问题: %s
            """.formatted(question);
        
        String response = callLLM(systemPrompt, userPrompt);
        return JSON.parseObject(response, QueryIntent.class);
    }
}
```

---

### 数据查询的三种使用场景

#### 场景1: API直接查询 (推荐用于程序集成)

```bash
POST /api/nlquery
{
  "question": "查询注册资本超过100万的水利服务企业"
}
```

#### 场景2: 前端交互式查询 (推荐用于人工探索)

在前端添加查询面板:
```vue
<!-- frontend/src/views/workspace/DataQuery.vue -->
<template>
  <div class="query-panel">
    <el-input v-model="question" placeholder="输入自然语言问题" />
    <el-button @click="executeQuery">查询</el-button>
    
    <div v-if="result">
      <div class="sql-display">{{ result.sql }}</div>
      <el-table :data="result.data" />
    </div>
  </div>
</template>
```

#### 场景3: 导入bonotlink-ui (推荐用于即时使用)

如果急需查询功能,直接:

1. 运行 `convert-to-bonotlink-ui-full.ps1`
2. 重启 bonotlink-ui 后端
3. 使用 bonotlink-ui 的自然语言查询界面

---

## 三、方案决策建议

### 短期(1-2周): 方案D (导入bonotlink-ui)

**理由**:
- 无需开发,立即可用
- 可以先验证本体定义是否完整
- 为后续自研积累经验

**步骤**:
1. 运行 `convert-to-bonotlink-ui-full.ps1` 生成 JSON
2. 配置 bonotlink-ui 的数据源(application.yml)
3. 测试查询功能
4. 收集用户反馈,完善本体定义

### 中期(1-2月): 方案B 阶段1-3 (MVP→JOIN)

**理由**:
- BOnotLink-Manager 作为本体管理系统,应有独立查询能力
- 渐进式开发,风险可控
- 不依赖外部系统

**路线**:
1. 实现 OntologyMetadataManager (核心)
2. 实现 SimpleQueryParser + SimpleSQLGenerator (MVP)
3. 增强条件解析
4. 增强关系解析(JOIN)

### 长期(3-6月): 方案B 阶段4 (LLM增强)

**理由**:
- 自然语言理解能力提升
- 支持复杂查询
- 体验接近 bonotlink-ui

---

## 四、总结

| 问题 | 解决方案 |
|-----|---------|
| 能生成 jjdata-ontology.json 格式吗? | ✅ 能,使用 `convert-to-bonotlink-ui-full.ps1` |
| 如何解决数据查询? | ✅ 推荐方案B (分4阶段实施) |
| 最快上线方案? | ✅ 方案D (导入bonotlink-ui,1天) |
| 长期自研方案? | ✅ 方案B (MVP 3-5天,完整版2-3周) |

**下一步行动**:

1. ✅ 运行 `convert-to-bonotlink-ui-full.ps1` 生成JSON
2. ⬜ 验证JSON格式(在bonotlink-ui中加载测试)
3. ⬜ 决定查询方案(短期用D,长期用B)
4. ⬜ 开始实施(如选B,先做MVP)
