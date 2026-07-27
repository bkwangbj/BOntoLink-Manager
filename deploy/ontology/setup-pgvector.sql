-- ============================================================
-- PostgreSQL pgvector 扩展 + 向量表初始化脚本
-- 在远程数据服务器上执行一次即可
-- 用法: psql -U bontolink -d bontolink -f setup-pgvector.sql
-- ============================================================

-- 第1步：创建 pgvector 扩展（需要 superuser 权限）
-- 如果已有则跳过（CREATE IF NOT EXISTS）
CREATE EXTENSION IF NOT EXISTS vector;

-- 第2步：验证扩展版本
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';

-- ============================================================
-- 第3步：创建向量表
-- class_embeddings 存储每个 ont_class 的向量表示
-- 用于语义相似度检索
-- ============================================================
CREATE TABLE IF NOT EXISTS class_embeddings (
    class_id      TEXT PRIMARY KEY,                    -- 对应 ont_class.id
    ns_code       TEXT NOT NULL,                       -- namespace 分区键
    embedding     vector(768) NOT NULL,                -- 768 维向量（OpenAI ada-002 标准）
    source_text   TEXT NOT NULL,                       -- 用于生成向量的原文
    version       BIGINT NOT NULL DEFAULT 0,           -- 版本号（用于增量追踪）
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 第4步：创建索引
-- 4a. 按 namespace 过滤索引（加速按 ns 查询）
CREATE INDEX IF NOT EXISTS idx_class_embeddings_ns
    ON class_embeddings (ns_code);

-- 4b. HNSW 向量索引（余弦距离）
--     这是 pgvector 最先进的索引类型，适合高精度近似搜索
--     如果数据量 < 1000 行可以跳过这个索引（全表扫描即可）
CREATE INDEX IF NOT EXISTS idx_class_embeddings_hnsw
    ON class_embeddings
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 200);
-- 参数说明：
--   m = 16: 每个节点最多 16 个连接（越大精度越高，内存越多）
--   ef_construction = 200: 构建时的动态候选列表大小

-- 第5步：创建更新时间触发器
CREATE OR REPLACE FUNCTION update_embeddings_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_class_embeddings_updated_at ON class_embeddings;
CREATE TRIGGER trigger_class_embeddings_updated_at
    BEFORE UPDATE ON class_embeddings
    FOR EACH ROW
    EXECUTE FUNCTION update_embeddings_updated_at();

-- ============================================================
-- 第6步：验证
-- ============================================================
SELECT
    'vector extension' AS item,
    CASE WHEN count(*) > 0 THEN '已安装' ELSE '未安装' END AS status
FROM pg_extension WHERE extname = 'vector'
UNION ALL
SELECT
    'class_embeddings table' AS item,
    CASE WHEN count(*) > 0 THEN '已创建' ELSE '未创建' END AS status
FROM pg_tables WHERE tablename = 'class_embeddings';

-- ============================================================
-- 常用查询示例（仅供验证，不执行）
-- ============================================================
-- 查询最相似的 10 个类（按向量余弦距离）:
-- SELECT class_id, ns_code, 1 - (embedding <=> '[0.1, 0.2, ...]'::vector) AS similarity
-- FROM class_embeddings
-- WHERE ns_code = 'w_wtr'
-- ORDER BY embedding <=> '[0.1, 0.2, ...]'::vector
-- LIMIT 10;

-- 查询某个 namespace 的 embedding 数量:
-- SELECT ns_code, count(*) FROM class_embeddings GROUP BY ns_code;
