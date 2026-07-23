# API_NAME 管理工具

本目录包含用于管理和检测 BOntoLink 系统中 `api_name` 字段的工具。

---

## 文件清单

| 文件 | 类型 | 用途 |
|------|------|------|
| `check-api-name-conflicts.sql` | SQL 脚本 | 完整的冲突检测 SQL（10 项检查） |
| `check-api-name.ps1` | PowerShell | 一键运行冲突检测的包装脚本 |
| `ApiNameConflictChecker.java` | Java 工具类 | 无需 sqlite3 的独立 Java 检测器 |
| `README.md` | 文档 | 本文件 |

---

## 快速开始

### 方式一：使用 PowerShell 脚本（推荐）

```powershell
# 默认检测 c:\beiktech-jyx\BOntoLink02\backend\bontolink.db
cd f:\aiProject\BOnotLink-Manager\tools
.\check-api-name.ps1

# 指定数据库路径
.\check-api-name.ps1 -DbPath "C:\path\to\bontolink.db"

# 输出结果到文件
.\check-api-name.ps1 -OutputFile "conflicts-report.txt"
```

### 方式二：使用 Java 工具类（无需 sqlite3）

**适用场景**：系统未安装 SQLite CLI 工具

```powershell
# 先编译后端项目
cd c:\beiktech-jyx\BOntoLink02\backend
mvnw -DskipTests clean compile

# 运行检测器
cd c:\beiktech-jyx\BOntoLink02\backend
java -cp "target/classes;lib/*" com.watf.bontolink.util.ApiNameConflictChecker

# 指定数据库路径
java -cp "target/classes;lib/*" com.watf.bontolink.util.ApiNameConflictChecker "C:\path\to\bontolink.db"
```

**注意**：
- Java 版本与 SQL 脚本功能完全一致
- 依赖项目编译后的 SQLite JDBC 驱动
- 输出格式与 SQL 脚本略有不同（更适合程序化处理）

### 方式三：直接使用 SQLite 命令

**前提条件**：系统已安装 [SQLite CLI](https://www.sqlite.org/download.html)

```bash
# Windows (PowerShell)
cd f:\aiProject\BOnotLink-Manager\tools
sqlite3 c:\beiktech-jyx\BOntoLink02\backend\bontolink.db < check-api-name-conflicts.sql

# Windows (Git Bash)
sqlite3 /c/beiktech-jyx/BOntoLink02/backend/bontolink.db < check-api-name-conflicts.sql

# 输出到文件
sqlite3 bontolink.db < check-api-name-conflicts.sql > report.txt
```

---

## 检测项说明

脚本会依次执行以下 10 项检测：

### 0. 全局统计
- 显示每个表的总记录数、唯一 api_name 数、重复数量
- 快速了解整体数据状况

### 1-8. 各表内部重复检测
逐表检查以下实体是否存在内部重复：
1. 对象类型 (`ont_class.api_name`)
2. 共享属性 (`ont_shared_properties.prop_code`)
3. 值类型 (`ont_value_types.api_name`)
4. 枚举类型 (`ont_enum_types.api_name`)
5. 枚举项 (`ont_enum_items.api_name`)
6. 接口 (`ont_interface.api_name`)
7. 链接类型 (`ont_link_types.link_type_code`)
8. 结构类型 (`ont_struct_types.struct_code`)

**输出示例**:
```
api_name          count  ids                        names
---------------   -----  ------------------------   -----------
Reservoir         2      class-001, class-002       水库 | 水库工程
```

### 9. 跨表全局冲突检测（核心）
检测不同模块之间是否使用了相同的 `api_name`。

**输出示例**:
```
api_name      conflict_count  affected_tables  conflict_sources
-----------   --------------  ---------------  ---------------------------
text          3               3                ont_class + ont_value_types + ont_shared_properties
```

### 10. 命名规范检查
检测 `api_name` 是否包含非法字符（不符合 `[a-z0-9_]` 规范）。

**违规示例**:
- 大写字母: `ReservoirType`（应为 `reservoir_type`）
- 中文字符: `水库类型`
- 特殊符号: `type-name`（应为 `type_name`）
- 空格: `type name`（应为 `type_name`）

---

## 输出结果解读

### 无冲突的理想输出
```
============ 9. 跨表全局冲突检测 ============
(无输出)
```

### 有冲突的输出示例
```
api_name      conflict_count  affected_tables  conflict_sources                detail_list
-----------   --------------  ---------------  ------------------------------  --------------------------
location      2               2                ont_class + ont_struct_types    class-loc-01(ont_class) | struct-types-00000004(ont_struct_types)
```

**说明**:
- `location` 同时被对象类型和结构类型使用
- 需要重命名其中一个以保证全局唯一
- 建议改为 `Location`（对象类）和 `LocationStruct`（结构类型）

---

## 修复冲突指南

### 步骤 1: 识别影响范围
运行检测脚本，记录所有冲突的 `api_name`：
```powershell
.\check-api-name.ps1 -OutputFile "conflicts-$(Get-Date -Format 'yyyyMMdd').txt"
```

### 步骤 2: 制定重命名方案
参考 [API_NAME_CODING_STANDARD.md](../API_NAME_CODING_STANDARD.md) 中的命名规则，为冲突项设计新名称。

**示例重命名方案**:
| 原名 | 表 | 新名 | 理由 |
|------|------|------|------|
| `text` | `ont_class` | `TextEntity` | 对象类用 PascalCase |
| `text` | `ont_value_types` | `text` | 保留（值类型优先） |

### 步骤 3: 创建 Flyway 迁移脚本
在 `backend/src/main/resources/db/migration/` 创建新迁移文件：

**SQLite 版本** (`sqlite/V21__fix_api_name_conflicts.sql`):
```sql
-- 修复 api_name 冲突: text 重命名
UPDATE ont_class SET api_name = 'TextEntity' WHERE api_name = 'text' AND id = 'class-xxx';

-- 修复 api_name 冲突: location 重命名
UPDATE ont_struct_types SET struct_code = 'LocationStruct' WHERE struct_code = 'location' AND id = 'struct-types-00000004';
```

**PostgreSQL 版本** (`postgresql/V21__fix_api_name_conflicts.sql`):
```sql
-- 同上（两方言语法兼容）
```

### 步骤 4: 同步更新前端代码
搜索所有引用旧 `api_name` 的前端代码并更新：
```powershell
# 搜索引用
cd f:\aiProject\BOnotLink-Manager\frontend
grep -r "text" src/

# 手动更新涉及的 .vue / .js 文件
```

### 步骤 5: 验证修复
```powershell
# 重置数据库（会自动应用新迁移）
cd c:\beiktech-jyx\BOntoLink02\backend
Remove-Item bontolink.db* -Force
mvnw spring-boot:run

# 重新检测
cd f:\aiProject\BOnotLink-Manager\tools
.\check-api-name.ps1
```

---

## 预防冲突的最佳实践

### 1. 新增实体前先检索
```bash
# 快速检索现有 api_name
sqlite3 bontolink.db "SELECT api_name FROM ont_class UNION SELECT prop_code FROM ont_shared_properties" | grep "your_name"
```

### 2. 遵循模块前缀约定
| 模块 | 前缀示例 | 说明 |
|------|----------|------|
| 行业分类 | `ind_`, `dom_`, `grp_` | 明确层级 |
| 对象类型 | 无前缀（PascalCase） | `Reservoir`, `HydrologicalStation` |
| 共享属性 | 无前缀（snake_case） | `start_time`, `water_level` |
| 值类型 | 语义名或缩写 | `text`, `mobile`, `area_code` |
| 枚举类型 | 语义名 | `area_district`, `gender`, `eng_kind` |

### 3. 代码审查必检项
每次 PR 提交前运行：
```powershell
.\check-api-name.ps1
```

### 4. CI 集成（可选）
在 `.github/workflows/` 或 Jenkins 流水线中添加：
```yaml
- name: Check API_NAME conflicts
  run: |
    cd tools
    ./check-api-name.ps1 -OutputFile conflicts.txt
    if (Select-String -Path conflicts.txt -Pattern "conflict_count") { exit 1 }
```

---

## 常见问题

### Q1: 枚举项的 api_name 可以为 NULL 吗？
**A**: 可以。对于层级编码场景（如行政区划），`api_name` 允许为空，依赖 `code` 字段作为标识。

### Q2: 跨表冲突是否必须修复？
**A**: 是的。`api_name` 全局唯一是系统设计要求，冲突会导致：
- API 响应歧义
- 前端选择器混乱
- 数据关联错误

### Q3: 如何批量重命名？
**A**: 使用 Flyway 迁移脚本 + 事务保证原子性：
```sql
BEGIN TRANSACTION;
UPDATE ont_class SET api_name = 'NewName' WHERE api_name = 'OldName';
-- 同步更新所有关联表...
COMMIT;
```

### Q4: 命名规范检查能否自动修复？
**A**: 不建议。命名转换（如 PascalCase → snake_case）可能丢失语义，应人工审核后手动修复。

---

## 相关文档

- [API_NAME_CODING_STANDARD.md](../API_NAME_CODING_STANDARD.md) - 完整命名规范
- [CLAUDE.md](../CLAUDE.md) - 项目协作指南
- [V1__baseline_schema.sql](../backend/src/main/resources/db/migration/sqlite/V1__baseline_schema.sql) - 数据库表结构

---

## 维护日志

| 日期 | 版本 | 修改内容 |
|------|------|----------|
| 2026-07-23 | v1.0 | 初始版本，包含 10 项检测规则 |
