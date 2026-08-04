# dbnew — 合并后的 Flyway 迁移脚本

**版本**: 2026-08-04  
**状态**: 待替换现有 `postgresql/` + `sqlite/` + `common/` 三目录

---

## 📦 结构

```
dbnew/
├── postgresql/
│   └── V1__ddl.sql          (1351 行，PG 完整 DDL)
├── sqlite/
│   └── V1__ddl.sql          (855 行，SQLite 完整 DDL)
├── common/
│   └── V2__seed.sql         (精简种子数据骨架)
└── README.md                (本文件)
```

---

## 🎯 合并规则

### DDL（结构）

| 新文件 | 来源 | 说明 |
|---|---|---|
| `postgresql/V1__ddl.sql` | PG/V1 + V5~V32 | 所有 CREATE TABLE + ALTER 变更已内联到最终版本 |
| `sqlite/V1__ddl.sql` | SQLite/V1 + V5~V31 | 同上，SQLite 方言 |

**关键变更已内联**：
- ✅ V21: `ont_explore_design` 唯一约束改为 `UNIQUE(class_id, name, kind)`
- ✅ V22: 语义扩展 7 张表（同义词/领域术语/层次/停用词/扩展缓存/查询日志/候选）
- ✅ V24: `ont_ontology_version` 版本跟踪表
- ✅ V26: 动作类型模块 15 张表（完整版 `ont_class_action`）
- ✅ V27: `ont_action_execution` 执行日志
- ✅ V28: `sys_synonym_dict` 新增 `entity_type` / `entity_id` 列
- ✅ V30: 同义词表唯一约束改为 `UNIQUE(entity_type, entity_id, word)`
- ✅ V31: 外部数据源 4 张表（`ont_ext_data_source` / `ont_ext_api_*` / `ont_ext_api_call_log`）
- ✅ V32: PG 唯一索引补齐（`ont_enum_types.api_name` / `ont_struct_types.struct_code` 等）
- ✅ V37: `ont_action_rule_property_mapping` 新增 `param_name` / `default_type` / `default_source`

### Seed（数据）

| 新文件 | 来源 | 说明 |
|---|---|---|
| `common/V2__seed.sql` | V20 + V3~V38 所有数据文件 | **完整种子数据**（7470行） |

**包含内容**：
- V20：5000+ 行完整基础种子（行业/命名空间/类/属性/枚举/GB2260 区划码等）
- V3/V4/V7：实例属性 / 对象属性标记 / 枚举同步父字段
- V10~V12：类型类种子（measure_subtype / full / analyzed）
- V14：分组 domain_code 种子
- V16/V18：字典种子
- V23：语义扩展种子
- V33/V34：链接关系 / 动作表单参数演示
- V35：综合演示（10 个类 + 16 个共享属性 + 7 个链接类型）
- V36：防汛演示
- V38：通用领域本体（数据管理/数据治理/元数据管理）

**幂等性**：所有 INSERT 使用 `ON CONFLICT DO NOTHING` 或 `ON CONFLICT DO UPDATE`，可重复执行

---

## 🔄 替换步骤

### 1. 备份现有脚本
```bash
cd f:/aiProject/BOnotLink-Manager/backend/bontolink-data/src/main/resources/db/migration
mv postgresql postgresql.bak
mv sqlite sqlite.bak
mv common common.bak
```

### 2. 使用新脚本
```bash
mv dbnew/postgresql ./
mv dbnew/sqlite ./
mv dbnew/common ./
rmdir dbnew
```

### 3. 清理数据库并重新迁移
```bash
# SQLite（开发环境）
rm f:/aiProject/BOnotLink-Manager/backend/bontolink.db*

# PostgreSQL（生产环境）
psql -U postgres -d bontolink -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

# 启动后端，Flyway 自动执行新脚本
cd f:/aiProject/BOnotLink-Manager/backend
mvnw -q -DskipTests spring-boot:run
```

---

## ✅ 验证清单

启动后端后检查：

1. **Flyway 执行日志**：
   ```
   Successfully applied 2 migrations to schema "public", now at version v2
   ```

2. **核心表存在**：
   ```sql
   -- 52 张表应全部创建成功
   SELECT count(*) FROM information_schema.tables WHERE table_schema='public';  -- 应返回 ~52
   ```

3. **唯一约束正确**：
   ```sql
   -- V21 约束
   SELECT constraint_name FROM information_schema.table_constraints 
   WHERE table_name='ont_explore_design' AND constraint_type='UNIQUE';
   -- 应返回 ont_explore_design_class_name_kind_key

   -- V30 约束
   SELECT constraint_name FROM information_schema.table_constraints 
   WHERE table_name='sys_synonym_dict' AND constraint_type='UNIQUE';
   -- 应返回 uk_synonym_entity_word
   ```

4. **动作模块表完整**（V26）：
   ```sql
   SELECT table_name FROM information_schema.tables 
   WHERE table_name LIKE 'ont_action%' OR table_name LIKE 'ont_class_action%'
   ORDER BY table_name;
   -- 应返回 15 张表
   ```

5. **种子数据加载**：
   ```sql
   SELECT category_code FROM ont_biz_category;  -- 应包含 ind_water + ind_general
   SELECT api_name FROM ont_enum_types;         -- 应包含 gender + dm_ds_type
   ```

---

## 📊 统计

| 项目 | PostgreSQL | SQLite | Common |
|---|---|---|---|
| DDL 行数 | 1351 | 855 | - |
| Seed 行数 | - | - | 7470 |
| 表数量 | 52 | 52 | - |
| 合并来源 | V1+V5~V32 | V1+V5~V31 | V20+V3~V38 |

---

## 🚨 注意事项

1. **不要改已发布的 Vn 文件**：`dbnew/` 下的 V1/V2 是最终版本，后续增量改动请新建 V3、V4...
2. **PG 时间戳字段**：统一用 `TIMESTAMP` 类型 + `DEFAULT CURRENT_TIMESTAMP`，Java 更新时间戳传 `java.sql.Timestamp` 对象（不要传字符串）
3. **幂等插入**：种子数据使用 `ON CONFLICT DO NOTHING`，可重复执行
4. **完整数据**：V2__seed.sql 包含 7470 行完整种子（V20 + V3~V38），启动后端后会自动加载全部数据

---

## 📝 变更日志

| 日期 | 版本 | 说明 |
|---|---|---|
| 2026-08-04 | v1.0 | 初版，合并 V1~V38 所有迁移脚本 |

---

**维护者**: Claude + 项目组  
**最后更新**: 2026-08-04
