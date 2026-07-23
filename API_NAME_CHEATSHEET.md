# API_NAME 编码速查表

> 快速参考：新增实体时如何命名 `api_name`

---

## 一、快速决策树

```
你要创建的是？
│
├─ 行业/领域分类 (ont_biz_category)
│   ├─ 行业一级 → ind_{abbr}          例: ind_water_resource
│   ├─ 领域二级 → dom_{ind}_{abbr}    例: dom_water_hydrology
│   └─ 分组三级 → grp_{dom}_{name}    例: grp_water_reservoir_monitoring
│
├─ 对象类型 (ont_class)
│   └─ PascalCase 概念名              例: Reservoir, HydrologicalStation
│
├─ 共享属性 (ont_shared_properties)
│   └─ snake_case 语义名              例: start_time, water_level
│
├─ 值类型 (ont_value_types)
│   ├─ 基础类型 → 语义小写            例: text, mobile, email
│   ├─ 枚举关联 → 组合语义            例: area_code, gender_code
│   └─ 统计编码 → 业务缩写            例: DJZCLXJDM, ZYFW
│
├─ 枚举类型 (ont_enum_types)
│   ├─ 国标 → 标准缩写                例: area_district, gender
│   └─ 自定义 → 完整语义              例: eng_kind, water_quality_level
│
├─ 枚举项 (ont_enum_items)
│   ├─ 单词项 → 英文小写              例: male, female, beijing
│   └─ 层级编码 → 可为 NULL          (依赖 code 字段)
│
├─ 接口 (ont_interface)
│   └─ PascalCase + 前缀 I（可选）   例: IMonitoring, ITimeSeries
│
├─ 链接类型 (ont_link_types)
│   └─ {from}_to_{to}_{semantic}      例: reservoir_to_dam_has_structure
│
├─ 结构类型 (ont_struct_types)
│   └─ PascalCase 结构名              例: FullName, Location, Address
│
└─ 数据源 (sys_data_source)
    └─ ds_{system}_{instance}         例: ds_mysql_prod, ds_postgres_dev
```

---

## 二、命名规则速记表

| 模块 | 字段名 | 格式 | 示例 | 长度 |
|------|--------|------|------|------|
| **行业分类** | `category_code` | `ind_*` / `dom_*` / `grp_*` | `ind_water_resource` | 3-50 |
| **对象类型** | `api_name` | `PascalCase` | `Reservoir` | 3-50 |
| **共享属性** | `prop_code` | `snake_case` | `start_time` | 3-50 |
| **值类型** | `api_name` | `snake_case` / `ABBR` | `text`, `DJZCLXJDM` | 3-64 |
| **枚举类型** | `api_name` | `snake_case` | `area_district` | 3-50 |
| **枚举项** | `api_name` | `snake_case` / `NULL` | `beijing`, `NULL` | 0-50 |
| **接口** | `api_name` | `PascalCase` | `IMonitoring` | 3-50 |
| **链接类型** | `link_type_code` | `snake_case` | `reservoir_to_dam` | 5-64 |
| **结构类型** | `struct_code` | `PascalCase` | `FullName` | 3-50 |
| **数据源** | `ds_code` | `ds_*` | `ds_mysql_prod` | 5-50 |

---

## 三、常见场景示例

### 场景 1: 水利工程领域新增对象类型
```
需求: 新增"水闸"对象类型
决策:
  - 模块: ont_class
  - 格式: PascalCase
  - 英文名: Sluice（水闸的标准英文）
结果: api_name = 'Sluice'
```

### 场景 2: 新增通用时间属性
```
需求: 新增"完工时间"共享属性
决策:
  - 模块: ont_shared_properties
  - 格式: snake_case
  - 语义: completion_time（完整清晰）
结果: prop_code = 'completion_time'
```

### 场景 3: 新增水质等级枚举
```
需求: 新增"水质等级"枚举类型（Ⅰ-Ⅴ类）
决策:
  - 模块: ont_enum_types
  - 格式: snake_case
  - 语义: water_quality_level
结果: api_name = 'water_quality_level'

枚举项:
  - level_1 (code='1', label='Ⅰ类')
  - level_2 (code='2', label='Ⅱ类')
  - ...
```

### 场景 4: 新增统计表字段编码
```
需求: 统计表字段"KJLBN"（科技类别）
决策:
  - 模块: ont_value_types
  - 格式: 保留业务缩写（统计表习惯）
结果: api_name = 'KJLBN'
```

### 场景 5: 新增水库到流域的关系
```
需求: 定义"水库归属流域"关系
决策:
  - 模块: ont_link_types
  - 格式: {from}_to_{to}_{semantic}
  - 语义: reservoir_to_basin_belongs_to
结果: link_type_code = 'reservoir_to_basin_belongs_to'
```

---

## 四、冲突预防检查表

**新增前必查 3 步**：

### ✅ 步骤 1: 全局搜索
```sql
-- 快速检索现有 api_name
SELECT 'ont_class' AS tbl, api_name FROM ont_class WHERE api_name = 'YourName'
UNION ALL
SELECT 'ont_shared_properties', prop_code FROM ont_shared_properties WHERE prop_code = 'YourName'
UNION ALL
SELECT 'ont_value_types', api_name FROM ont_value_types WHERE api_name = 'YourName'
-- ... 其他表
```

### ✅ 步骤 2: 前端代码检索
```powershell
cd f:\aiProject\BOnotLink-Manager\frontend
grep -r "YourName" src/
```

### ✅ 步骤 3: 规范自查
- [ ] 只包含 `[a-z0-9_]`（小写、数字、下划线）
- [ ] 长度 3-50 字符（特殊情况最多 64）
- [ ] 符合模块命名风格（见速记表）
- [ ] 语义清晰，避免缩写歧义

---

## 五、禁止事项 ❌

| 禁止项 | 示例 | 正确做法 |
|--------|------|----------|
| 大写字母（对象类除外） | `ReservoirType` | `reservoir_type` |
| 中文字符 | `水库类型` | `reservoir_type` |
| 特殊符号（中划线） | `type-name` | `type_name` |
| 空格 | `type name` | `type_name` |
| 过度缩写 | `rsv_tp` | `reservoir_type` |
| 拼音 | `shuiku` | `Reservoir` |
| 前缀不一致 | 行业用 `industry_*` | `ind_*` |

---

## 六、快速查询命令

```powershell
# 查看所有对象类型 api_name
sqlite3 bontolink.db "SELECT api_name, display_name FROM ont_class LIMIT 20"

# 查看所有共享属性 prop_code
sqlite3 bontolink.db "SELECT prop_code, rdfs_label FROM ont_shared_properties LIMIT 20"

# 统计 api_name 使用频率
sqlite3 bontolink.db "
  SELECT api_name, COUNT(*) FROM (
    SELECT api_name FROM ont_class
    UNION ALL SELECT prop_code FROM ont_shared_properties
    UNION ALL SELECT api_name FROM ont_value_types
  ) GROUP BY api_name ORDER BY COUNT(*) DESC LIMIT 10
"

# 检测全局冲突（简版）
cd f:\aiProject\BOnotLink-Manager\tools
powershell -File check-api-name.ps1
```

---

## 七、参考资源

- 📖 [API_NAME_CODING_STANDARD.md](../API_NAME_CODING_STANDARD.md) - 完整规范文档
- 🛠️ [tools/README.md](README.md) - 冲突检测工具使用指南
- 📋 [CLAUDE.md](../CLAUDE.md) - 项目协作指南

---

**记住**：`api_name` 全局唯一 + 不可变 = 新增前必查！
