-- SQLite V24: Ontology 本体模型版本跟踪表
-- 与 PostgreSQL V24 结构兼容，用于开发环境

CREATE TABLE IF NOT EXISTS ont_ontology_version (
    id          TEXT PRIMARY KEY,
    version     INTEGER NOT NULL DEFAULT 0,
    updated_by  TEXT,
    updated_at  TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

INSERT OR IGNORE INTO ont_ontology_version (id, version, updated_by)
VALUES ('ontology-model', 0, 'system');
