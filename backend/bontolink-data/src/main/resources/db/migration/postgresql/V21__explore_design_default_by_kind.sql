-- 默认看板/默认列表按 kind 区分:UNIQUE(class_id,name) → UNIQUE(class_id,name,kind)
ALTER TABLE ont_explore_design DROP CONSTRAINT IF EXISTS ont_explore_design_class_id_name_key;
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'ont_explore_design_class_name_kind_key'
  ) THEN
    ALTER TABLE ont_explore_design ADD CONSTRAINT ont_explore_design_class_name_kind_key UNIQUE (class_id, name, kind);
  END IF;
END$$;
