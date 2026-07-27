-- PG V24: Ontology 本体模型版本跟踪表
-- 用于 OntologyModelManager 判断 OntModel 是否需要重建
-- admin 端修改类/属性/关系时 version +1，ontology 实例检测到变化后重建 OntModel

CREATE TABLE IF NOT EXISTS ont_ontology_version (
    id          VARCHAR(64) PRIMARY KEY,
    version     INTEGER NOT NULL DEFAULT 0,
    updated_by  VARCHAR(255),
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO ont_ontology_version (id, version, updated_by)
VALUES ('ontology-model', 0, 'system')
ON CONFLICT (id) DO NOTHING;
