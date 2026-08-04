-- V32: 为 common/V35、V36 演示种子数据补齐 PG 唯一约束
-- PostgreSQL 专用
--
-- 背景: V1 baseline 中下列表未对业务键建 UNIQUE 索引，
-- 而 common/V35__comprehensive_demo_seed.sql / V36__flood_control_demo_seed.sql
-- 使用 ON CONFLICT (<业务键>) DO NOTHING 幂等插入，PG 要求冲突列必须匹配
-- PRIMARY KEY 或 UNIQUE 约束，否则报 "没有匹配ON CONFLICT说明的唯一或者排除约束"。
-- 本迁移补齐与 SQLite V1 声明一致（api_name/prop_code/struct_code/enum_id NOT NULL UNIQUE）的唯一索引。
-- 幂等: CREATE UNIQUE INDEX IF NOT EXISTS，可重复执行

CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_enum_types_u_api_name ON ont_enum_types(api_name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_struct_types_u_struct_code ON ont_struct_types(struct_code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_shared_properties_u_prop_code ON ont_shared_properties(prop_code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_enum_sync_config_u_enum_id ON ont_enum_sync_config(enum_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_class_link_u_api_name ON ont_class_link(api_name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_class_action_u_api_name ON ont_class_action(api_name);
