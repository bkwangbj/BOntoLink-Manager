-- 实例探索「看板/设计」持久化(替代原前端 localStorage)
-- name 为空('') = 该对象类型的默认看板(每 class 一条);非空 = 命名设计
CREATE TABLE IF NOT EXISTS ont_explore_design (
  id           VARCHAR(46)  PRIMARY KEY,
  class_id     VARCHAR(46)  NOT NULL,
  name         VARCHAR(255) NOT NULL DEFAULT '',
  kind         VARCHAR(16)  NOT NULL DEFAULT 'query',
  config       TEXT,
  created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(class_id, name)
);
CREATE INDEX IF NOT EXISTS idx_explore_design_class ON ont_explore_design(class_id);
