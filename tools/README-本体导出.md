# 本体导出与转换工具

## 概述

BOnotLink-Manager 提供两种导出方案:

1. **export-ontology.ps1** - 完整导出(JSON格式,保留原始结构)
2. **convert-to-bonotlink-ui.ps1** - 转换为 bonotlink-ui 可用格式

---

## 方案1: 完整导出

### 用途
导出完整的本体配置,包括图谱、分类树、对象类型、值类型、枚举、链接、共享属性等。

### 使用方法
```powershell
# 1. 启动后端
cd f:\aiProject\BOnotLink-Manager\backend
mvnw spring-boot:run

# 2. 执行导出(新窗口)
cd f:\aiProject\BOnotLink-Manager\tools
.\export-ontology.ps1
```

### 导出文件
- `graph-ontology.json` - 图谱数据(nodes + edges)
- `category-tree.json` - 行业分类树
- `object-types.json` - 对象类型定义
- `value-types.json` - 值类型
- `enum-types.json` - 枚举类型
- `link-types.json` - 链接类型
- `shared-properties.json` - 共享属性
- `export-metadata.json` - 导出元数据

### 自定义参数
```powershell
# 指定输出目录
.\export-ontology.ps1 -OutputPath "d:/exports/ontology"

# 指定 API 地址
.\export-ontology.ps1 -BaseUrl "http://192.168.1.100:8088/bontolink/api"
```

---

## 方案2: 转换为 bonotlink-ui 格式

### 用途
将 BOnotLink-Manager 的本体配置转换为 bonotlink-ui 的 `OntologyDefinition` 格式,直接可用于自然语言查询。

### 目标格式
```json
{
  "entities": [
    {
      "name": "实体名",
      "table": "物理表名",
      "dataSource": "数据源代码",
      "description": "实体描述",
      "fields": [
        {
          "name": "字段名",
          "label": "显示名",
          "type": "VARCHAR|INTEGER|DECIMAL|...",
          "metric": true,
          "remark": "备注"
        }
      ]
    }
  ],
  "relationships": [
    {
      "from": { "entity": "源实体", "field": "源字段" },
      "to": { "entity": "目标实体", "field": "目标字段" },
      "joinType": "INNER|LEFT",
      "description": "关系描述"
    }
  ],
  "dictionaries": [
    {
      "name": "字典名",
      "entries": [
        { "code": "编码", "label": "显示值" }
      ]
    }
  ],
  "promptHints": [
    "查询规划提示1",
    "查询规划提示2"
  ]
}
```

### 使用方法
```powershell
# 1. 启动 BOnotLink-Manager 后端
cd f:\aiProject\BOnotLink-Manager\backend
mvnw spring-boot:run

# 2. 执行转换(新窗口)
cd f:\aiProject\BOnotLink-Manager\tools
.\convert-to-bonotlink-ui.ps1

# 输出: f:/aiproject/bonotlink-ui/app/backend/src/main/resources/ontology/water-service-ontology.json
```

### 自定义参数
```powershell
# 指定输出路径
.\convert-to-bonotlink-ui.ps1 -OutputPath "f:/custom/path/ontology"

# 指定输出文件名
.\convert-to-bonotlink-ui.ps1 -OutputFile "my-ontology.json"

# 指定领域筛选
.\convert-to-bonotlink-ui.ps1 -Domain "水利服务业"
```

### 转换规则

#### 1. 实体映射
- **name**: `display_name ?? rdfs_label ?? api_name`
- **table**: 从 `ont_class_ds.physical_table` 获取
- **dataSource**: 从 `ont_class_ds.ds_code` 获取
- **fields**: 从 `ont_class_property` 转换

#### 2. 字段类型映射
| XSD 类型 | SQL 类型 |
|---------|---------|
| xsd:string | VARCHAR |
| xsd:integer | INTEGER |
| xsd:decimal | DECIMAL |
| xsd:boolean | BOOLEAN |
| xsd:dateTime | TIMESTAMP |
| xsd:date | DATE |

#### 3. 关系映射
- 从 `link_type_mapping` 提取
- **joinType**: `is_identifying ? "INNER" : "LEFT"`
- 字段映射: `field_from` → `field_to`

#### 4. 字典映射
- 从 `ont_enum_type` + `ont_enum_item` 转换
- 枚举项的 `code_value` → `label`

### 加载到 bonotlink-ui

转换完成后,`OntologyManager` 会自动加载:

```java
// OntologyManager.java
@PostConstruct
public void init() {
    // 扫描 classpath:ontology/*.json
    Resource[] resources = resolver.getResources("classpath:ontology/*.json");
    // 自动合并所有 JSON 文件
}
```

**重启 bonotlink-ui 后端**即可生效:
```bash
cd f:/aiproject/bonotlink-ui/app/backend
mvn spring-boot:run
```

---

## 图谱可视化

BOnotLink-Manager 前端已生成完整图谱:

### 访问地址
```
http://localhost:5173/#/workspace/graph
```

### 功能特性
- **双画布**: 左侧行业分类树 + 右侧对象本体图
- **5种关系**: 父子类(sub) / 等价类(eq) / 不相交(dis) / 互斥并集(union) / 链接(link)
- **多种布局**: force / dagre / circular / grid / concentric / random
- **交互探索**: 搜索 / 缩放 / 点击查看详情 / 联动定位
- **导出图片**: 工具栏导出按钮

### 后端接口
```
GET /api/graph/ontology         - 对象本体图谱(nodes + edges)
GET /api/graph/industry-tree    - 行业分类层级树
```

---

## 常见问题

### Q1: 后端未运行
```
[×] 后端未运行,请先启动: cd backend && mvnw spring-boot:run
```

**解决**: 先启动 BOnotLink-Manager 后端

### Q2: 输出目录不存在
脚本会自动创建目录,无需手动创建。

### Q3: 类没有物理表映射
转换时会跳过没有绑定数据集的类,检查 `ont_class_ds` 表。

### Q4: 字段类型异常
默认映射为 `VARCHAR`,需要在数据库中正确设置 `data_type` 字段。

---

## 技术架构

### BOntLink-Manager
- **数据库**: PostgreSQL / SQLite
- **ORM**: MyBatis
- **表结构**:
  - `ont_class` - 对象类型
  - `ont_class_property` - 属性
  - `ont_class_ds` - 类-数据集绑定
  - `link_type` + `link_type_mapping` - 链接
  - `ont_enum_type` + `ont_enum_item` - 枚举

### bonotlink-ui
- **本体格式**: `OntologyDefinition.java`
- **加载器**: `OntologyManager.java`
- **扫描路径**: `classpath:ontology/*.json`
- **热替换**: 支持运行时更新本体(调用 `reload()`)

---

## 下一步

1. ✅ 导出本体配置
2. ✅ 转换为 bonotlink-ui 格式
3. ⬜ 在 bonotlink-ui 中测试自然语言查询
4. ⬜ 根据查询效果调整 `promptHints`
5. ⬜ 完善实体关系映射

---

## 参考资料

- [BOntLink-Manager 项目](f:/aiProject/BOnotLink-Manager)
- [bonotlink-ui 项目](f:/aiproject/bonotlink-ui)
- [GraphG6.vue](../frontend/src/views/workspace/GraphG6.vue) - 图谱可视化
- [GraphController.java](../backend/src/main/java/com/beiktech/bontolink/controller/GraphController.java) - 图谱 API
