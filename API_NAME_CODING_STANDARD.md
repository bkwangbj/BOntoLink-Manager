# BOntoLink 全局 API_NAME 编码规范

> 版本: v1.0 | 日期: 2026-07-23  
> **重要**: `api_name` 是系统全局唯一标识符，用于 API 调用、跨表关联、前后端通信等场景  
> **快速参考**: 见 [API_NAME_CHEATSHEET.md](API_NAME_CHEATSHEET.md)

---

## 一、总体原则

### 1.1 唯一性范围（重要）

**全局唯一** - 以下实体的 `api_name` 在整个系统中必须唯一：
- ✅ 对象类型（ont_class）
- ✅ 共享属性（ont_shared_properties.prop_code）
- ✅ 值类型（ont_value_types）
- ✅ 枚举类型（ont_enum_types）
- ✅ 接口（ont_interface）
- ✅ 链接类型（ont_link_types.link_type_code）
- ✅ 结构类型（ont_struct_types.struct_code）
- ✅ 数据源（sys_data_source.ds_code）

**局部唯一（类内/接口内）** - 以下实体允许跨类重复，仅保证所属容器内唯一：
- 🔸 类属性（ont_class_property.api_name） - **类内唯一**，不同类可用相同属性名
- 🔸 接口属性（ont_interface_property.api_name） - **接口内唯一**，不同接口可重复
- 🔸 枚举项（ont_enum_items.api_name） - **枚举内唯一**，不同枚举可重复（可为NULL）

**设计理由**：
- 属性是依附于类/接口的，天然具有命名空间隔离（如 `Reservoir.name` vs `Person.name`）
- 全局唯一会导致属性名膨胀（如 `reservoir_name`, `person_name`），违背本体语义
- 查询时始终通过 `class_id + api_name` 组合键定位，不存在歧义

### 1.2 命名风格
- **格式**: 小写字母 + 下划线（`snake_case`）
- **字符集**: 仅限英文字母、数字、下划线 `[a-z0-9_]`
- **长度**: 建议 3-50 字符，最长不超过 64 字符
- **可读性**: 使用标准英文单词/缩写，避免拼音或无意义字符

### 1.3 不可变性
- 一旦分配 `api_name` 并投入使用（有数据引用），**原则上不允许修改**
- 修改会导致级联影响：API 调用失败、数据关联断裂、前端界面异常
- 如需重命名，必须通过**迁移脚本**同步更新所有引用点

---

## 二、分模块编码规则

### 2.1 行业分类（ont_biz_category）

**行业一级（industry）**
```
格式: ind_{industry_abbr}
示例:
  ind_water_resource    # 水利行业
  ind_transportation    # 交通行业
  ind_emergency         # 应急行业
  ind_environment       # 环保行业
  ind_forestry          # 林业行业
  ind_agriculture       # 农业农村
```

**领域二级（domain）**
```
格式: dom_{industry}_{domain_abbr}
示例:
  dom_water_soilconservation  # 水利 > 水土保持
  dom_water_resource          # 水利 > 水资源
  dom_water_hydrology         # 水利 > 水文
  dom_water_environment       # 水利 > 水环境
  dom_water_engineering       # 水利 > 水利工程
  dom_water_statistics        # 水利 > 水利统计
```

**分组三级（group）**
```
格式: grp_{domain}_{group_name}
示例:
  grp_water_reservoir_monitoring    # 水库监测分组
  grp_water_quality_indicators      # 水质指标分组
```

**前缀字典**
| 前缀 | 含义 | 层级 |
|------|------|------|
| `ind_` | Industry（行业） | 1级 |
| `dom_` | Domain（领域） | 2级 |
| `grp_` | Group（分组） | 3级 |

---

### 2.2 对象类型（ont_class）

**格式**: `{DomainConcept}` (PascalCase 英文概念名)

**规则**:
- 使用标准的本体概念命名（OWL/RDFS 风格）
- PascalCase（大驼峰）命名法
- 优先使用行业标准术语或国际通用名

```
示例:
  Reservoir              # 水库
  HydrologicalStation    # 水文站
  SoilErosionPlot        # 水土流失监测样地
  WaterIntake            # 取水口
  CheckDam               # 拦沙坝
  GroundwaterWell        # 地下水监测井
  WaterAllocationPlan    # 水量分配方案
```

**特殊说明**:
- 与 OWL 本体导出对齐：`<owl:Class rdf:about="#Reservoir">`
- 保持语义清晰：多个单词组合时使用完整词汇，避免过度缩写

---

### 2.3 共享属性（ont_shared_properties）

**格式**: `{semantic_name}` (语义化名称)

**规则**:
- 小写下划线风格 `snake_case`
- 体现属性的业务语义
- 通用属性使用简洁常见名

```
示例:
  # 时间类
  start_time             # 开始时间
  end_time               # 结束时间
  create_time            # 创建时间
  update_time            # 更新时间
  
  # 标识类
  name                   # 名称
  code                   # 编码
  rid                    # 资源唯一标识
  uuid                   # UUID
  
  # 状态类
  status                 # 状态
  enabled                # 启用标志
  
  # 地理类
  longitude              # 经度
  latitude               # 纬度
  elevation              # 海拔
  geohash                # 地理哈希
  
  # 度量类
  water_level            # 水位
  flow_rate              # 流量
  precipitation          # 降水量
  temperature            # 温度
  
  # 业务特定
  reservoir_capacity     # 水库库容
  dam_height             # 坝高
  catchment_area         # 流域面积
```

**特别说明**：共享属性的 `prop_code` 全局唯一，设计用于跨类复用。

---

### 2.3.1 类属性（ont_class_property）⚠️ 局部唯一

**唯一性范围**: **类内唯一**（class_id + api_name 组合键）

**格式**: `{semantic_name}` (snake_case)

**规则**:
- 同一个类内的属性名不能重复
- **不同类之间允许使用相同的属性名**（如 `name`, `code`, `status`）
- 属性具有类的命名空间隔离，不存在全局冲突
- 优先复用通用属性名，避免无意义的前缀

```
示例:
  # Reservoir 类的属性
  - name                 # 水库名称
  - capacity             # 库容
  - dam_height           # 坝高
  - status               # 状态
  
  # Person 类的属性（可复用相同名称）
  - name                 # 人员姓名
  - age                  # 年龄
  - status               # 在职状态
  
  # HydrologicalStation 类的属性
  - name                 # 站点名称
  - station_type         # 站点类型
  - status               # 运行状态
```

**为什么允许重复**：
1. **本体语义**：属性是类的组成部分，`Reservoir.name` 和 `Person.name` 在语义上完全不同
2. **查询场景**：前端始终通过 `{class_id: 'xxx', property: 'name'}` 组合查询，不会混淆
3. **避免膨胀**：全局唯一会导致 `reservoir_name`, `person_name` 等冗余命名

**冲突检测豁免**：
- 类属性不参与全局冲突检测（第 9 项）
- 仅检查同一 class_id 下的重复

---

### 2.3.2 接口属性（ont_interface_property）⚠️ 局部唯一

**规则与类属性相同**：接口内唯一，不同接口可重复。

---

### 2.4 值类型（ont_value_types）

**格式**: `{value_semantic}` 或 `{abbr_code}` (特定编码场景)

**规则**:
- 基础类型用语义名（如 `text`, `mobile`）
- 枚举关联类型可使用业务缩写（如 `DJZCLXJDM`, `ZYFW`）
- 组合语义时保持可读性（如 `area_code`, `gender_code`, `eng_kind`）

```
示例:
  # 基础类型
  text                   # 基础文本
  mobile                 # 手机号
  rid_text               # RID 文本
  
  # 枚举关联
  area_code              # 行政区划编码
  gender_code            # 性别编码
  eng_kind               # 水利工程类型
  job                    # 岗位类型
  
  # 业务缩写（统计表编码）
  sf                     # 是否（yes/no）
  DJZCLXJDM              # 登记注册类型及代码
  ZYFW                   # 主要服务或产出形式
  SSLY                   # 所属流域
  LSGX                   # 隶属关系
  KJLBN                  # 科技类别（简码）
```

**分类建议**:
| 场景 | 命名风格 | 示例 |
|------|----------|------|
| 通用数据类型 | 语义小写 | `text`, `email`, `url` |
| 国标/行标枚举 | 业务编码 | `area_code`, `DJZCLXJDM` |
| 行业自定义枚举 | 语义组合 | `eng_kind`, `water_quality_level` |

---

### 2.5 枚举类型（ont_enum_types）

**格式**: `{enum_semantic}` (枚举语义名)

**规则**:
- 小写下划线，清晰表达枚举集含义
- 国标/行标用标准缩写
- 自定义枚举用完整语义词

```
示例:
  # 国家标准
  area_district          # 行政区划 (GB/T 2260)
  gender                 # 性别 (GB/T 2261.1)
  
  # 行业标准
  eng_kind               # 水利工程类型
  job                    # 岗位类型
  
  # 自定义业务
  water_quality_level    # 水质等级
  flood_risk_category    # 洪水风险类别
  reservoir_scale        # 水库规模分类
```

---

### 2.6 枚举项（ont_enum_items）⚠️ 局部唯一

**唯一性范围**: **枚举内唯一**（enum_id + api_name 组合键）

**格式**: `{item_semantic}` (snake_case，可为 NULL)

**规则**:
- 小写下划线
- 同一枚举类型内不能重复
- **不同枚举类型可使用相同的枚举项名**（如 `active`, `pending` 可用于多个状态枚举）
- 优先使用英文单词（如 `male`, `female`, `beijing`）
- 层级编码场景（如行政区划）可留空，依赖 `code` 字段

```
示例:
  # 性别枚举 (enum_id = 'enum-types-gender')
  male                   # code='1'
  female                 # code='2'
  unknown                # code='9'
  
  # 行政区划枚举 (enum_id = 'enum-types-area-district')
  beijing                # code='110000'
  dongcheng              # code='110101'
  
  # 工程类型枚举 (enum_id = 'enum-types-eng-kind')
  reservoir              # code='01', 水库
  large_reservoir        # code='0101', 大型水库
  hydropower             # code='02', 水电站
  
  # 任务状态枚举 (enum_id = 'enum-types-task-status')
  pending                # code='pending' ✅ 可与其他枚举的 'pending' 共存
  active                 # code='active'
  completed              # code='completed'
  
  # 审批状态枚举 (enum_id = 'enum-types-approval-status')
  pending                # code='pending' ✅ 与任务状态的 'pending' 不冲突
  approved               # code='approved'
  rejected               # code='rejected'
  
  # 层级编码可为空（统计表导入场景）
  NULL                   # code='110000', label='北京市'
```

**为什么允许重复**：
- 枚举项依附于枚举类型，有明确的命名空间隔离
- 查询时始终通过 `enum_id + code` 或 `enum_id + api_name` 定位
- `pending`, `active`, `disabled` 等通用状态值适用于多种业务场景

---

### 2.7 接口（ont_interface）

**格式**: `I{ConceptName}` (接口语义名，可选前缀 `I`)

**规则**:
- PascalCase 或 snake_case（建议与 Class 对齐为 PascalCase）
- 可添加 `I` 前缀标识接口类型（可选）

```
示例:
  IMonitoring            # 监测接口
  ITimeSeries            # 时序数据接口
  IGeospatial            # 地理空间接口
  IHierarchical          # 层级结构接口
```

---

### 2.8 链接类型（ont_link_types）

**格式**: `{class_from}_to_{class_to}_{semantic}` (关系语义)

**规则**:
- 小写下划线
- 明确表达"源类-目标类-关系语义"
- 避免歧义（如 `belongs_to` / `has_many` / `part_of`）

```
示例:
  reservoir_to_dam_has_structure          # 水库 → 大坝（拥有结构）
  station_to_basin_belongs_to             # 水文站 → 流域（归属）
  project_to_person_managed_by            # 项目 → 人员（管理者）
  well_to_aquifer_monitors                # 监测井 → 含水层（监测对象）
```

---

### 2.9 结构类型（ont_struct_types）

**格式**: `{StructName}` (PascalCase 结构名)

**规则**:
- PascalCase 命名
- 体现复合数据结构语义

```
示例:
  FullName               # 姓名（姓 + 名）
  Address                # 地址（国家 + 城市 + 街道 + 邮编）
  Contact                # 联系方式（手机 + 邮箱）
  Location               # 地理位置（经度 + 纬度 + Geohash）
  Period                 # 时间段（开始时间 + 结束时间）
  Amount                 # 金额（数值 + 币种）
```

---

### 2.10 数据源（sys_data_source）

**格式**: `ds_{system_abbr}_{instance}` (数据源标识)

**规则**:
- `ds_` 前缀统一标识数据源
- 小写下划线
- 体现系统名称 + 实例标识

```
示例:
  ds_mysql_prod          # MySQL 生产库
  ds_pg_analytics        # PostgreSQL 分析库
  ds_oracle_legacy       # Oracle 遗留系统
  ds_api_weather         # 气象 API 数据源
```

---

### 2.11 命名空间（ont_biz_namespace）

**格式**: `{org}_{system}_{module}` (组织-系统-模块)

**规则**:
- 小写下划线
- 体现所属组织/系统/模块的层级关系

```
示例:
  w_wtr                  # 水利通用本体
  w_wtr_sc               # 水利-水土保持
  w_wtr_wr               # 水利-水资源
  w_wtr_hyd              # 水利-水文
  w_tfc                  # 交通本体
  w_eme                  # 应急本体
```

**命名空间代码表**:
| 代码 | 含义 | URI 前缀示例 |
|------|------|--------------|
| `w_wtr` | 水利行业 | `http://watf.com/ont/water#` |
| `w_tfc` | 交通行业 | `http://watf.com/ont/traffic#` |
| `w_eme` | 应急行业 | `http://watf.com/ont/emergency#` |
| `w_env` | 环保行业 | `http://watf.com/ont/environment#` |

---

### 2.12 类型类（ont_type_class）

**格式**: `{semantic_tag}` (功能语义标签)

**规则**:
- 小写下划线
- 简洁表达类型类的功能定位

```
示例:
  platform_built         # 平台内置（系统级别）
  display                # 显示属性类
  geo                    # 地理坐标类
  choropleth_map_config_id  # 等值热力地图类
  vertex                 # 知识图谱类
  timeseries             # 时间序列类
  hierarchy              # 层级导航类
  hubble-oe              # 对象操作展示类
  actions                # 通用操作能力类
```

---

## 三、冲突检测与解决

### 3.1 入库前检测（推荐）

在后端服务层或数据库层添加唯一性约束：

```sql
-- 所有 api_name 字段建 UNIQUE 约束（已存在）
CREATE UNIQUE INDEX idx_ont_class_api_name ON ont_class(api_name);
CREATE UNIQUE INDEX idx_ont_shared_properties_api_name ON ont_shared_properties(prop_code);
CREATE UNIQUE INDEX idx_ont_value_types_api_name ON ont_value_types(api_name);
CREATE UNIQUE INDEX idx_ont_enum_types_api_name ON ont_enum_types(api_name);
CREATE UNIQUE INDEX idx_ont_interface_api_name ON ont_interface(api_name);
```

### 3.2 跨表全局唯一检测（建议定期执行）

```sql
-- 检测是否有跨表重复的 api_name
SELECT api_name, COUNT(*) AS cnt FROM (
  SELECT api_name, 'ont_class' AS tbl FROM ont_class WHERE api_name IS NOT NULL
  UNION ALL
  SELECT prop_code AS api_name, 'ont_shared_properties' AS tbl FROM ont_shared_properties WHERE prop_code IS NOT NULL
  UNION ALL
  SELECT api_name, 'ont_value_types' AS tbl FROM ont_value_types WHERE api_name IS NOT NULL
  UNION ALL
  SELECT api_name, 'ont_enum_types' AS tbl FROM ont_enum_types WHERE api_name IS NOT NULL
  UNION ALL
  SELECT api_name, 'ont_enum_items' AS tbl FROM ont_enum_items WHERE api_name IS NOT NULL
  UNION ALL
  SELECT api_name, 'ont_interface' AS tbl FROM ont_interface WHERE api_name IS NOT NULL
)
GROUP BY api_name
HAVING cnt > 1;
```

### 3.3 冲突解决策略

| 场景 | 解决方案 | 示例 |
|------|----------|------|
| 新增实体与已有冲突 | 添加模块前缀或语义后缀 | `text` → `vt_text` (值类型专用) |
| 历史数据需重命名 | 编写迁移脚本批量更新 | Flyway `V21__rename_api_names.sql` |
| 临时/测试数据 | 添加时间戳或UUID后缀 | `test_enum_20260723` |

---

## 四、前后端协作规范

### 4.1 前端 API 调用
```javascript
// ✅ 推荐：使用 api_name 作为查询参数
objectTypesApi.get('Reservoir')
valueTypesApi.get('area_code')
enumTypesApi.get('gender')

// ❌ 避免：用 display_name 或 id 查询
objectTypesApi.get('水库')  // 中文不稳定
objectTypesApi.get('class-00000000-0000-0000-0000-000000000004')  // ID 难维护
```

### 4.2 前端枚举/字典缓存键
```javascript
// 使用 api_name 作为缓存 key
const cache = {
  'area_district': [...],      // 行政区划枚举项
  'gender': [...],             // 性别枚举项
  'eng_kind': [...]            // 工程类型枚举项
}
```

### 4.3 URL 路由与 api_name 对应
```
/resources/object-types/Reservoir
/resources/value-types/area_code
/config/enum-types/gender
```

---

## 五、版本与迁移

### 5.1 版本管理
- `api_name` 一旦发布到生产环境，即视为**不可变**
- 如需重大调整，通过**新版本命名空间**隔离
  ```
  w_wtr_v1  →  w_wtr_v2
  ```

### 5.2 迁移脚本示例
```sql
-- Flyway V21: 重命名 api_name（含级联更新）
BEGIN TRANSACTION;

-- 1. 更新主表
UPDATE ont_class SET api_name = 'NewReservoir' WHERE api_name = 'Reservoir';

-- 2. 更新关联表（ont_class_property 中的 prop_value_ref 等）
UPDATE ont_class_property 
SET prop_value_ref = 'NewReservoir' 
WHERE prop_value_ref = 'Reservoir';

-- 3. 更新链接关系
UPDATE ont_link_types 
SET class_from_id = (SELECT id FROM ont_class WHERE api_name = 'NewReservoir')
WHERE class_from_id = (SELECT id FROM ont_class WHERE api_name = 'Reservoir');

COMMIT;
```

---

## 六、检查清单（新增 api_name 前必读）

- [ ] **唯一性**: 在全系统范围内检索，确保不重复
- [ ] **可读性**: 使用标准英文单词/缩写，避免拼音或无意义字符
- [ ] **语义明确**: 名称能直接反映业务含义
- [ ] **前缀规范**: 遵循本文档的模块前缀规则
- [ ] **字符限制**: 仅包含 `[a-z0-9_]`，长度 3-50 字符
- [ ] **测试验证**: 在开发环境完整测试 API 调用、前端展示、跨表关联
- [ ] **文档同步**: 更新 API 文档和前端字段映射说明

---

## 七、快速参考表

| 模块 | 数据表 | 字段名 | 前缀/格式 | 示例 |
|------|--------|--------|-----------|------|
| 行业分类 | ont_biz_category | `category_code` | `ind_` / `dom_` / `grp_` | `ind_water_resource` |
| 对象类型 | ont_class | `api_name` | `PascalCase` | `Reservoir` |
| 共享属性 | ont_shared_properties | `prop_code` | `snake_case` | `start_time` |
| 值类型 | ont_value_types | `api_name` | `snake_case` | `area_code` |
| 枚举类型 | ont_enum_types | `api_name` | `snake_case` | `gender` |
| 枚举项 | ont_enum_items | `api_name` | `snake_case`（可选） | `male` |
| 接口 | ont_interface | `api_name` | `PascalCase` | `IMonitoring` |
| 链接类型 | ont_link_types | `link_code` | `from_to_semantic` | `reservoir_to_dam_has` |
| 结构类型 | ont_struct_types | `struct_code` | `PascalCase` | `FullName` |
| 数据源 | sys_data_source | `ds_code` | `ds_{system}` | `ds_mysql_prod` |
| 命名空间 | ont_biz_namespace | `ns_code` | `org_system_mod` | `w_wtr_sc` |
| 类型类 | ont_type_class | `type_class_code` | `snake_case` | `timeseries` |

---

## 八、常见问题 FAQ

### Q1: 为什么不用中文作为 api_name？
**A**: 
- API 调用需要 URL 编码，影响可读性和性能
- 跨系统集成时可能遇到字符集兼容问题
- 英文是国际标准，便于本体导出与互操作

### Q2: 枚举项的 api_name 能否为空？
**A**: 
- 允许为空（如统计表导入的历史数据）
- 但推荐为所有常用枚举项配置有意义的 api_name
- 前端展示优先使用 `label`，API 查询才用 `api_name`

### Q3: 如何处理业务缩写与语义可读性的平衡？
**A**: 
- **国标/行标编码**：保留原始缩写（如 `DJZCLXJDM`）
- **通用业务概念**：使用语义组合（如 `water_quality_level`）
- **特殊术语**：在数据库注释和前端 tooltip 补充说明

### Q4: 跨表关联时如何保证 api_name 一致性？
**A**: 
- 后端服务层添加外键校验逻辑
- 迁移脚本必须包含级联更新
- 定期运行跨表一致性检查 SQL

---

## 九、附录

### A1. 缩写词典（常用业务术语）

| 缩写 | 英文全称 | 中文 | 使用场景 |
|------|----------|------|----------|
| `eng` | Engineering | 工程 | 水利工程 |
| `hyd` | Hydrology | 水文 | 水文站 |
| `sc` | Soil Conservation | 水土保持 | 水土保持 |
| `wr` | Water Resource | 水资源 | 水资源管理 |
| `wae` | Water Environment | 水环境 | 水环境监测 |
| `wec` | Water Ecology | 水生态 | 水生态修复 |
| `fld` | Flood Control | 防洪 | 防洪减灾 |
| `tfc` | Traffic | 交通 | 交通运输 |
| `eme` | Emergency | 应急 | 应急管理 |
| `env` | Environment | 环境 | 环境保护 |
| `for` | Forestry | 林业 | 林业资源 |
| `agr` | Agriculture | 农业 | 农业农村 |

### A2. 数据类型基础词汇

| 英文 | 中文 | api_name 示例 |
|------|------|---------------|
| text | 文本 | `text` |
| number | 数值 | `number` |
| integer | 整数 | `integer` |
| decimal | 小数 | `decimal` |
| boolean | 布尔值 | `boolean` |
| date | 日期 | `date` |
| datetime | 日期时间 | `datetime` |
| time | 时间 | `time` |
| email | 邮箱 | `email` |
| url | 网址 | `url` |
| mobile | 手机号 | `mobile` |
| phone | 电话 | `phone` |
| zipcode | 邮编 | `zipcode` |
| ip | IP地址 | `ip_address` |
| uuid | UUID | `uuid` |
| rid | 资源标识 | `rid_text` |

### A3. 关系语义词汇（链接类型）

| 英文 | 中文 | 示例 |
|------|------|------|
| belongs_to | 归属于 | `station_to_basin_belongs_to` |
| has_many | 拥有多个 | `reservoir_to_dam_has_many` |
| has_one | 拥有单个 | `project_to_manager_has_one` |
| part_of | 部分属于 | `component_to_system_part_of` |
| contains | 包含 | `basin_to_river_contains` |
| monitors | 监测 | `station_to_river_monitors` |
| manages | 管理 | `user_to_project_manages` |
| references | 引用 | `document_to_standard_references` |
| depends_on | 依赖于 | `task_to_milestone_depends_on` |

---

**文档维护者**: wangbj  
**审核状态**: 待审核  
**更新记录**:
- 2026-07-23: v1.0 初稿，基于现有数据库实例分析

---

*本规范是 BOntoLink 项目的技术标准文档，所有开发人员必须遵守。如有疑问或建议，请联系架构组。*
