# API_NAME 编码规范实施总结

## 📋 概述

为保证 BOntoLink 系统中 `api_name` 的全局唯一性和命名一致性，本次规划产出以下成果：

---

## 📦 交付成果

### 1. 核心文档（3 份）

| 文档 | 路径 | 内容 |
|------|------|------|
| **编码规范** | [API_NAME_CODING_STANDARD.md](API_NAME_CODING_STANDARD.md) | 完整规范文档（18 页），覆盖 10 个模块的命名规则、前缀约定、示例对照表 |
| **速查表** | [API_NAME_CHEATSHEET.md](API_NAME_CHEATSHEET.md) | 快速参考手册（6 页），包含决策树、场景示例、禁止事项 |
| **工具指南** | [tools/README.md](tools/README.md) | 冲突检测工具使用文档（12 页） |

### 2. 检测工具（3 套）

| 工具 | 文件 | 用途 |
|------|------|------|
| **SQL 脚本** | [tools/check-api-name-conflicts.sql](tools/check-api-name-conflicts.sql) | 完整的 10 项冲突检测 SQL |
| **PowerShell 包装器** | [tools/check-api-name.ps1](tools/check-api-name.ps1) | 一键运行检测的友好脚本 |
| **Java 检测器** | [backend/.../ApiNameConflictChecker.java](backend/src/main/java/com/watf/bontolink/util/ApiNameConflictChecker.java) | 无需 sqlite3 的独立检测程序 |

### 3. 项目集成

- ✅ 已在 [CLAUDE.md](CLAUDE.md) "相关文档"章节添加规范引用
- ✅ 速查表、工具指南均已链接到主文档

---

## 🎯 核心规范要点

### 全局原则
1. **全局唯一性**: 所有 `api_name` 在整个系统中必须唯一（跨表）
2. **命名风格**: 小写字母 + 下划线（`snake_case`），对象类型/接口/结构类型除外（`PascalCase`）
3. **不可变性**: 一旦投入使用，原则上不允许修改

### 10 大模块命名规则

| 模块 | 前缀/格式 | 示例 |
|------|-----------|------|
| **行业分类** | `ind_` / `dom_` / `grp_` | `ind_water_resource`, `dom_water_hydrology` |
| **对象类型** | `PascalCase`（无前缀） | `Reservoir`, `HydrologicalStation` |
| **共享属性** | `snake_case` | `start_time`, `water_level` |
| **值类型** | `snake_case` / `ABBR` | `text`, `area_code`, `DJZCLXJDM` |
| **枚举类型** | `snake_case` | `area_district`, `gender`, `eng_kind` |
| **枚举项** | `snake_case` / `NULL` | `male`, `beijing`, `NULL`（层级编码） |
| **接口** | `PascalCase` + `I`（可选） | `IMonitoring`, `ITimeSeries` |
| **链接类型** | `{from}_to_{to}_{semantic}` | `reservoir_to_dam_has_structure` |
| **结构类型** | `PascalCase` | `FullName`, `Location`, `Address` |
| **数据源** | `ds_{system}_{instance}` | `ds_mysql_prod`, `ds_postgres_dev` |

---

## 🛠️ 使用指南

### 开发者日常流程

#### 1. 新增实体前（必须）
```powershell
# 查阅速查表，确定命名规则
浏览 API_NAME_CHEATSHEET.md

# 全局搜索，确认名称未被占用
cd f:\aiProject\BOnotLink-Manager\tools
.\check-api-name.ps1
```

#### 2. 创建实体
按照规范命名，在 Flyway 迁移脚本中添加：
```sql
INSERT INTO ont_class (id, api_name, display_name, ...)
VALUES ('class-xxx', 'Reservoir', '水库', ...);
```

#### 3. 提交前检查（强制）
```powershell
# 运行冲突检测
.\check-api-name.ps1

# 如有冲突，修改命名并重新检测
```

### Code Review 检查点
- [ ] `api_name` 符合模块命名规范（见速查表）
- [ ] 已运行冲突检测工具，无冲突
- [ ] 只包含合法字符 `[a-z0-9_]`（对象类型可用 `[A-Za-z]`）
- [ ] 长度在 3-50 字符范围内

---

## 📊 检测工具使用示例

### 方式 1: PowerShell（推荐）
```powershell
cd f:\aiProject\BOnotLink-Manager\tools
.\check-api-name.ps1
```

### 方式 2: Java（无 sqlite3 环境）
```powershell
cd c:\beiktech-jyx\BOntoLink02\backend
mvnw -DskipTests compile
java -cp "target/classes;lib/*" com.watf.bontolink.util.ApiNameConflictChecker
```

### 方式 3: SQL 直接执行
```bash
sqlite3 bontolink.db < tools/check-api-name-conflicts.sql
```

---

## 🔍 检测内容

工具会执行 **10 项检查**：

0. **全局统计** - 各表记录数 + 唯一性统计
1-8. **表内重复检测** - 8 个核心表逐一检查
9. **跨表冲突检测** - 核心检查，检测不同模块间的 `api_name` 冲突
10. **命名规范检查** - 非法字符、大小写、长度检查

**输出示例**（有冲突时）：
```
============ 跨表全局冲突检测 ============
api_name      conflict_count  affected_tables  conflict_sources
-----------   --------------  ---------------  --------------------------
text          2               2                ont_class + ont_value_types
```

---

## 🚨 发现冲突后的处理流程

### 步骤 1: 识别影响范围
```powershell
# 输出详细报告
.\check-api-name.ps1 -OutputFile "conflicts-$(Get-Date -Format 'yyyyMMdd').txt"
```

### 步骤 2: 制定重命名方案
| 原名 | 冲突表 | 新名 | 理由 |
|------|--------|------|------|
| `text` | `ont_class` | `TextEntity` | 对象类改为 PascalCase |
| `text` | `ont_value_types` | `text` | 保留（值类型优先） |

### 步骤 3: 创建迁移脚本
```sql
-- backend/src/main/resources/db/migration/sqlite/V21__fix_conflicts.sql
UPDATE ont_class SET api_name = 'TextEntity' WHERE api_name = 'text';
```

### 步骤 4: 同步前端代码
```powershell
cd f:\aiProject\BOnotLink-Manager\frontend
grep -r "\"text\"" src/  # 搜索所有引用
# 手动更新涉及文件
```

### 步骤 5: 验证修复
```powershell
# 重置数据库（自动应用迁移）
Remove-Item c:\beiktech-jyx\BOntoLink02\backend\bontolink.db* -Force
mvnw spring-boot:run

# 重新检测
.\check-api-name.ps1
```

---

## 📚 参考资源

### 主要文档
- 📖 [API_NAME_CODING_STANDARD.md](API_NAME_CODING_STANDARD.md) - 完整规范（18 页）
- 📋 [API_NAME_CHEATSHEET.md](API_NAME_CHEATSHEET.md) - 速查表（6 页）
- 🛠️ [tools/README.md](tools/README.md) - 工具使用指南（12 页）

### 相关文档
- [CLAUDE.md](CLAUDE.md) - 项目协作指南
- [UI规范.md](UI规范.md) - 前端视觉规范
- [V1__baseline_schema.sql](backend/src/main/resources/db/migration/sqlite/V1__baseline_schema.sql) - 数据库表结构

---

## ✅ 实施检查清单

### 团队培训
- [ ] 全员阅读 [API_NAME_CHEATSHEET.md](API_NAME_CHEATSHEET.md)
- [ ] 演示冲突检测工具使用（3 种方式）
- [ ] 模拟冲突修复流程

### 开发流程集成
- [ ] 新增实体前必查冲突（强制要求）
- [ ] Code Review 加入 `api_name` 检查项
- [ ] PR 合并前运行检测工具

### CI/CD 集成（可选）
- [ ] GitHub Actions / Jenkins 加入自动检测步骤
- [ ] 检测到冲突自动失败构建

### 文档维护
- [ ] 定期更新速查表中的示例
- [ ] 记录新增的命名模式
- [ ] 收集团队反馈，优化规范

---

## 📅 版本历史

| 版本 | 日期 | 内容 |
|------|------|------|
| v1.0 | 2026-07-23 | 初始版本，完整规范 + 3 套检测工具 |

---

## 🤝 反馈与改进

发现问题或有改进建议？
1. 在 CLAUDE.md 中记录
2. 在团队会议中讨论
3. 更新规范文档并通知全员

---

**记住核心原则**: `api_name` 全局唯一 + 命名规范 + 新增前必查 = 系统稳定运行的基础！
