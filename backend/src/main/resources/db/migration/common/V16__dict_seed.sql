-- ============================================================
-- V16: 字典管理模块 — 建表 + 种子数据
-- ============================================================

CREATE TABLE IF NOT EXISTS ont_dict_def (
    id            TEXT PRIMARY KEY,
    dict_code     TEXT NOT NULL UNIQUE,
    dict_name     TEXT NOT NULL,
    rdfs_comment  TEXT,
    status        INTEGER NOT NULL DEFAULT 1,
    sort_no       INTEGER NOT NULL DEFAULT 0,
    create_time   TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    update_time   TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS ont_dict_item (
    id            TEXT PRIMARY KEY,
    dict_id       TEXT NOT NULL REFERENCES ont_dict_def(id) ON DELETE CASCADE,
    parent_id     TEXT REFERENCES ont_dict_item(id),
    item_code     TEXT NOT NULL,
    item_value    TEXT NOT NULL,
    sort_no       INTEGER NOT NULL DEFAULT 0,
    status        INTEGER NOT NULL DEFAULT 1,
    color         TEXT,
    ext_data      TEXT,
    create_time   TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    update_time   TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    UNIQUE(dict_id, item_code)
);

CREATE INDEX IF NOT EXISTS idx_dict_item_dict   ON ont_dict_item(dict_id);
CREATE INDEX IF NOT EXISTS idx_dict_item_parent ON ont_dict_item(parent_id);

DELETE FROM ont_dict_item;
DELETE FROM ont_dict_def;

-- 是否
INSERT INTO ont_dict_def(id, dict_code, dict_name, sort_no) VALUES ('dict-yes-no', 'sys_yes_no', '是否', 1);
INSERT INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-yes-no-1', 'dict-yes-no', '1', '是', 1),
  ('di-yes-no-0', 'dict-yes-no', '0', '否', 2);

-- 启用状态
INSERT INTO ont_dict_def(id, dict_code, dict_name, sort_no) VALUES ('dict-status', 'sys_status', '启用状态', 2);
INSERT INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no, color) VALUES
  ('di-status-1', 'dict-status', '1', '启用', 1, '#00b42a'),
  ('di-status-0', 'dict-status', '0', '禁用', 2, '#f53f3f');

-- 连接类型
INSERT INTO ont_dict_def(id, dict_code, dict_name, sort_no) VALUES ('dict-join-type', 'sys_join_type', '连接类型', 3);
INSERT INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-join-1', 'dict-join-type', 'LEFT',  '左连接', 1),
  ('di-join-2', 'dict-join-type', 'INNER', '内连接', 2),
  ('di-join-3', 'dict-join-type', 'RIGHT', '右连接', 3),
  ('di-join-4', 'dict-join-type', 'FULL',  '全连接', 4);
