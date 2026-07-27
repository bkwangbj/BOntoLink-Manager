-- 语义扩充系统数据表
-- PostgreSQL 版本

-- 1. 同义词词典表
CREATE TABLE IF NOT EXISTS sys_synonym_dict (
    id VARCHAR(64) PRIMARY KEY,
    word VARCHAR(255) NOT NULL,
    synonyms TEXT NOT NULL,
    domain VARCHAR(50),
    confidence NUMERIC(3,2) DEFAULT 0.9,
    source VARCHAR(50) DEFAULT 'MANUAL',
    usage_count INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(word, domain)
);

CREATE INDEX IF NOT EXISTS idx_synonym_word ON sys_synonym_dict(word);
CREATE INDEX IF NOT EXISTS idx_synonym_domain ON sys_synonym_dict(domain);
CREATE INDEX IF NOT EXISTS idx_synonym_confidence ON sys_synonym_dict(confidence DESC);

-- 2. 领域术语映射表
CREATE TABLE IF NOT EXISTS ont_domain_term (
    id VARCHAR(64) PRIMARY KEY,
    standard_term VARCHAR(255) NOT NULL,
    common_terms TEXT NOT NULL,
    domain VARCHAR(50) NOT NULL,
    term_type VARCHAR(50),
    similarity NUMERIC(3,2) DEFAULT 0.9,
    context TEXT,
    usage_count INTEGER DEFAULT 0,
    source VARCHAR(50) DEFAULT 'MANUAL',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_domain_term_standard ON ont_domain_term(standard_term);
CREATE INDEX IF NOT EXISTS idx_domain_term_domain ON ont_domain_term(domain);
CREATE INDEX IF NOT EXISTS idx_domain_term_type ON ont_domain_term(term_type);

-- 3. 概念层次表
CREATE TABLE IF NOT EXISTS ont_class_hierarchy (
    id VARCHAR(64) PRIMARY KEY,
    child_class_id VARCHAR(64) NOT NULL,
    parent_class_id VARCHAR(64) NOT NULL,
    hierarchy_level INTEGER,
    relationship_type VARCHAR(50) DEFAULT 'IS_A',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (child_class_id) REFERENCES ont_class(id),
    FOREIGN KEY (parent_class_id) REFERENCES ont_class(id),
    UNIQUE(child_class_id, parent_class_id)
);

CREATE INDEX IF NOT EXISTS idx_hierarchy_child ON ont_class_hierarchy(child_class_id);
CREATE INDEX IF NOT EXISTS idx_hierarchy_parent ON ont_class_hierarchy(parent_class_id);

-- 4. 停用词表
CREATE TABLE IF NOT EXISTS sys_stopwords (
    id VARCHAR(64) PRIMARY KEY,
    word VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50) DEFAULT 'COMMON',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_stopword_category ON sys_stopwords(category);

-- 5. 扩充缓存表
CREATE TABLE IF NOT EXISTS ont_class_expansion (
    class_id VARCHAR(64) PRIMARY KEY,
    original_text TEXT NOT NULL,
    expanded_text TEXT NOT NULL,
    expansion_detail TEXT,
    embedding_vector BYTEA,
    token_count INTEGER,
    expansion_version INTEGER DEFAULT 1,
    last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (class_id) REFERENCES ont_class(id)
);

CREATE INDEX IF NOT EXISTS idx_expansion_update ON ont_class_expansion(last_update);

-- 6. 查询日志表
CREATE TABLE IF NOT EXISTS sys_query_log (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64),
    query_text TEXT NOT NULL,
    matched_entity_id VARCHAR(64),
    match_score NUMERIC(5,4),
    user_clicked INTEGER DEFAULT 0,
    session_id VARCHAR(128),
    query_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (matched_entity_id) REFERENCES ont_class(id)
);

CREATE INDEX IF NOT EXISTS idx_query_log_time ON sys_query_log(query_time);
CREATE INDEX IF NOT EXISTS idx_query_log_entity ON sys_query_log(matched_entity_id);
CREATE INDEX IF NOT EXISTS idx_query_log_clicked ON sys_query_log(user_clicked);

-- 7. 同义词候选表
CREATE TABLE IF NOT EXISTS sys_synonym_candidate (
    id VARCHAR(64) PRIMARY KEY,
    word VARCHAR(255) NOT NULL,
    synonym VARCHAR(255) NOT NULL,
    confidence NUMERIC(3,2),
    evidence_count INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING',
    source VARCHAR(50) DEFAULT 'AUTO_LEARN',
    reviewer VARCHAR(64),
    review_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(word, synonym)
);

CREATE INDEX IF NOT EXISTS idx_candidate_status ON sys_synonym_candidate(status);
CREATE INDEX IF NOT EXISTS idx_candidate_confidence ON sys_synonym_candidate(confidence DESC);
