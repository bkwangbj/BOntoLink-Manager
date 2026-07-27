-- 语义扩充系统数据表
-- SQLite 版本

-- 1. 同义词词典表
CREATE TABLE IF NOT EXISTS sys_synonym_dict (
    id TEXT PRIMARY KEY,
    word TEXT NOT NULL,
    synonyms TEXT NOT NULL,
    domain TEXT,
    confidence REAL DEFAULT 0.9,
    source TEXT DEFAULT 'MANUAL',
    usage_count INTEGER DEFAULT 0,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    update_time TEXT DEFAULT (datetime('now','localtime')),
    UNIQUE(word, domain)
);

CREATE INDEX IF NOT EXISTS idx_synonym_word ON sys_synonym_dict(word);
CREATE INDEX IF NOT EXISTS idx_synonym_domain ON sys_synonym_dict(domain);
CREATE INDEX IF NOT EXISTS idx_synonym_confidence ON sys_synonym_dict(confidence DESC);

-- 2. 领域术语映射表
CREATE TABLE IF NOT EXISTS ont_domain_term (
    id TEXT PRIMARY KEY,
    standard_term TEXT NOT NULL,
    common_terms TEXT NOT NULL,
    domain TEXT NOT NULL,
    term_type TEXT,
    similarity REAL DEFAULT 0.9,
    context TEXT,
    usage_count INTEGER DEFAULT 0,
    source TEXT DEFAULT 'MANUAL',
    create_time TEXT DEFAULT (datetime('now','localtime')),
    update_time TEXT DEFAULT (datetime('now','localtime'))
);

CREATE INDEX IF NOT EXISTS idx_domain_term_standard ON ont_domain_term(standard_term);
CREATE INDEX IF NOT EXISTS idx_domain_term_domain ON ont_domain_term(domain);
CREATE INDEX IF NOT EXISTS idx_domain_term_type ON ont_domain_term(term_type);

-- 3. 概念层次表
CREATE TABLE IF NOT EXISTS ont_class_hierarchy (
    id TEXT PRIMARY KEY,
    child_class_id TEXT NOT NULL,
    parent_class_id TEXT NOT NULL,
    hierarchy_level INTEGER,
    relationship_type TEXT DEFAULT 'IS_A',
    create_time TEXT DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (child_class_id) REFERENCES ont_class(id),
    FOREIGN KEY (parent_class_id) REFERENCES ont_class(id),
    UNIQUE(child_class_id, parent_class_id)
);

CREATE INDEX IF NOT EXISTS idx_hierarchy_child ON ont_class_hierarchy(child_class_id);
CREATE INDEX IF NOT EXISTS idx_hierarchy_parent ON ont_class_hierarchy(parent_class_id);

-- 4. 停用词表
CREATE TABLE IF NOT EXISTS sys_stopwords (
    id TEXT PRIMARY KEY,
    word TEXT NOT NULL UNIQUE,
    category TEXT DEFAULT 'COMMON',
    create_time TEXT DEFAULT (datetime('now','localtime'))
);

CREATE INDEX IF NOT EXISTS idx_stopword_category ON sys_stopwords(category);

-- 5. 扩充缓存表
CREATE TABLE IF NOT EXISTS ont_class_expansion (
    class_id TEXT PRIMARY KEY,
    original_text TEXT NOT NULL,
    expanded_text TEXT NOT NULL,
    expansion_detail TEXT,
    embedding_vector BLOB,
    token_count INTEGER,
    expansion_version INTEGER DEFAULT 1,
    last_update TEXT DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (class_id) REFERENCES ont_class(id)
);

CREATE INDEX IF NOT EXISTS idx_expansion_update ON ont_class_expansion(last_update);

-- 6. 查询日志表
CREATE TABLE IF NOT EXISTS sys_query_log (
    id TEXT PRIMARY KEY,
    user_id TEXT,
    query_text TEXT NOT NULL,
    matched_entity_id TEXT,
    match_score REAL,
    user_clicked INTEGER DEFAULT 0,
    session_id TEXT,
    query_time TEXT DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (matched_entity_id) REFERENCES ont_class(id)
);

CREATE INDEX IF NOT EXISTS idx_query_log_time ON sys_query_log(query_time);
CREATE INDEX IF NOT EXISTS idx_query_log_entity ON sys_query_log(matched_entity_id);
CREATE INDEX IF NOT EXISTS idx_query_log_clicked ON sys_query_log(user_clicked);

-- 7. 同义词候选表
CREATE TABLE IF NOT EXISTS sys_synonym_candidate (
    id TEXT PRIMARY KEY,
    word TEXT NOT NULL,
    synonym TEXT NOT NULL,
    confidence REAL,
    evidence_count INTEGER DEFAULT 0,
    status TEXT DEFAULT 'PENDING',
    source TEXT DEFAULT 'AUTO_LEARN',
    reviewer TEXT,
    review_time TEXT,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    UNIQUE(word, synonym)
);

CREATE INDEX IF NOT EXISTS idx_candidate_status ON sys_synonym_candidate(status);
CREATE INDEX IF NOT EXISTS idx_candidate_confidence ON sys_synonym_candidate(confidence DESC);
