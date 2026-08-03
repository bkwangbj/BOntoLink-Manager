-- 规则属性映射对齐向导的属性映射矩阵:
--   param_name     赋值方式=表单参数时, 冗余存所选参数的名称 (便于列表回显, 不用再回查表单参数)
--   default_type   赋值方式=静态值时的取值形态: static=直接填值 / source=来自候选来源
--   default_source default_type=source 时的来源配置 (枚举候选范围等, JSON)
ALTER TABLE ont_action_rule_property_mapping ADD COLUMN param_name TEXT;
ALTER TABLE ont_action_rule_property_mapping ADD COLUMN default_type TEXT;
ALTER TABLE ont_action_rule_property_mapping ADD COLUMN default_source TEXT;
