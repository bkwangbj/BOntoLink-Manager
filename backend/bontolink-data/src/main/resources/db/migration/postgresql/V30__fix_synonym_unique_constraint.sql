-- 修复同义词表唯一约束：允许不同实体有相同的词
-- 原约束 (word, domain) 不合理，改为 (entity_type, entity_id, word)

-- 1. 删除旧的唯一约束
ALTER TABLE sys_synonym_dict DROP CONSTRAINT IF EXISTS sys_synonym_dict_word_domain_key;

-- 2. 添加新的唯一约束：同一实体不重复同一个词
ALTER TABLE sys_synonym_dict ADD CONSTRAINT uk_synonym_entity_word
    UNIQUE (entity_type, entity_id, word);

-- 3. 为新约束字段添加索引（提升查询性能）
CREATE INDEX IF NOT EXISTS idx_synonym_entity ON sys_synonym_dict(entity_type, entity_id);
