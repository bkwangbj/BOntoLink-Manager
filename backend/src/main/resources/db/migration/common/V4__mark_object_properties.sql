-- ============================================================
-- V4: 把"引用其它对象类型"的属性标记为对象属性(prop_type='object')
--   目的: 实例探索-属性搜索面板里能区分"普通属性 / 对象属性"
--   说明: 仅改 prop_type,data_type 保留(前端按 propType 分组展示)
--   方言: 纯 UPDATE,sqlite / postgresql 通用,放 common/。
-- ============================================================

-- 水文测站.所属河流 → 指向"河流"对象类型,属于对象属性
UPDATE ont_class_property SET prop_type = 'object' WHERE id = 'cp-7';
