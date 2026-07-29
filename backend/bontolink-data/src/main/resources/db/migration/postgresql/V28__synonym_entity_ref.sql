ALTER TABLE sys_synonym_dict ADD COLUMN IF NOT EXISTS entity_type TEXT;
ALTER TABLE sys_synonym_dict ADD COLUMN IF NOT EXISTS entity_id TEXT;
