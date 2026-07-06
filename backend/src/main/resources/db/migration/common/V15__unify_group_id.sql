-- 统一分组 id:让 type=3 分类节点复用对应 ont_biz_group 的现有 id（group- 规则）
-- 此后 ont_biz_category(type=3) 与 ont_biz_group 指向同一分组时共用同一 id，
-- 无需再靠 category_code 反查。ont_biz_group / ont_biz_group_class 不动，只改分类节点两行的 id/rid。
-- rid 保持不含前缀的形式 ri.ont.biz.category.<uuid>（剥掉 group- 前缀）。

UPDATE ont_biz_category
   SET rid = 'ri.ont.biz.category.' || REPLACE(
             (SELECT g.id FROM ont_biz_group g WHERE g.category_code = ont_biz_category.category_code), 'group-', ''),
       id  = (SELECT g.id FROM ont_biz_group g WHERE g.category_code = ont_biz_category.category_code)
 WHERE category_type = 3
   AND EXISTS (SELECT 1 FROM ont_biz_group g WHERE g.category_code = ont_biz_category.category_code);
