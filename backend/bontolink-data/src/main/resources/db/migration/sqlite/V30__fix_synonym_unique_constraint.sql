-- 修复同义词表唯一约束：允许不同实体有相同的词
-- 原约束 (word, domain) 不合理，改为 (entity_type, entity_id, word)

-- 1. 删除旧的唯一约束（SQLite 语法）
-- SQLite 不支持 ALTER TABLE DROP CONSTRAINT，需要重建表

-- 保存数据
CREATE TABLE sys_synonym_dict_backup AS SELECT * FROM sys_synonym_dict;

-- 删除旧表
DROP TABLE sys_synonym_dict;

-- 重建表（去掉 word+domain 唯一约束）
CREATE TABLE sys_synonym_dict (
    id TEXT PRIMARY KEY,
    word TEXT NOT NULL,
    synonyms TEXT NOT NULL,
    domain TEXT,
    confidence REAL DEFAULT 0.9,
    source TEXT DEFAULT 'MANUAL',
    usage_count INTEGER DEFAULT 0,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    update_time TEXT DEFAULT (datetime('now','localtime')),
    entity_type TEXT,
    entity_id TEXT,
    UNIQUE(entity_type, entity_id, word)
);

-- 恢复数据（跳过重复记录）
INSERT OR IGNORE INTO sys_synonym_dict
SELECT * FROM sys_synonym_dict_backup;

-- 删除备份表
DROP TABLE sys_synonym_dict_backup;

-- 重建索引
CREATE INDEX idx_synonym_word ON sys_synonym_dict(word);
CREATE INDEX idx_synonym_domain ON sys_synonym_dict(domain);
CREATE INDEX idx_synonym_confidence ON sys_synonym_dict(confidence DESC);
CREATE INDEX idx_synonym_entity ON sys_synonym_dict(entity_type, entity_id);
