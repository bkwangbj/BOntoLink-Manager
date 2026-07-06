-- 给 ont_biz_group 表增加 domain_code 字段，关联到 ont_biz_category.category_code
-- 用于将业务分组按所属领域（如 dom_water_statistics）归类
ALTER TABLE ont_biz_group ADD COLUMN domain_code TEXT;
CREATE INDEX IF NOT EXISTS idx_group_domain ON ont_biz_group(domain_code);
