package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.data.mapper.ClassMetaMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 对象类型补充元数据 REST 接口：等价类 / 不相交类 / 互斥并集 / 等价属性 / 互斥属性 /
 * 类属性扩展 CRUD / 类→物理表绑定(ont_class_ds) / 候选对象类清单。
 *
 * <p>错误处理约定：写操作(增改删)统一 try/catch 并通过 {@code log.error} 把异常打到日志,
 * 再以 {@code R.error(500, ...)} 返回前端可读消息；未捕获的异常兜底由
 * {@link com.beiktech.bontolink.common.GlobalExceptionHandler} 记录。
 */
@Slf4j
@RestController
@RequestMapping("/api/class-meta")
public class ClassMetaController {

    @Autowired private ClassMetaMapper mapper;
    @Autowired private com.beiktech.bontolink.service.ObjectTypeCreateService createService;

    /* ============ 候选对象类 ============ */

    /** 返回所有对象类的简要列表（id/名称/编码），供等价类/不相交类等关系选择器使用。 */
    @GetMapping("/classes")
    public R<List<Map<String, Object>>> listAllClasses() {
        return R.ok(mapper.listAllClassesForPicker());
    }

    /* ============ 等价类 / 不相交类 ============ */

    /** 查询指定类的等价类或不相交类关系列表；type 取值 equivalent | disjoint。 */
    @GetMapping("/class-group")
    public R<List<Map<String, Object>>> listClassGroup(
            @RequestParam String classId,
            @RequestParam String type /* equivalent | disjoint */) {
        return R.ok(mapper.listGroupByType(classId, type));
    }

    /**
     * 新增等价类或不相交类关系。
     * 存储时将 (class_id, ref_class_id) 按字典序规范化为 (minId, maxId)，避免重复存两个方向；
     * 若同类型的类对已存在则返回 400。
     */
    @PostMapping("/class-group")
    public R<Map<String, Object>> addClassGroup(@RequestBody Map<String, Object> body) {
        String type = (String) body.get("group_type");
        String a = String.valueOf(body.get("class_id"));
        String b = String.valueOf(body.get("ref_class_id"));
        if (a.equals(b)) return R.error(400,"不能与自身建立关系");
        // 按字典序统一存储方向，保证唯一性校验不因顺序不同而漏检
        String minId = a.compareTo(b) < 0 ? a : b;
        String maxId = a.compareTo(b) < 0 ? b : a;
        if (mapper.existsGroupPair(type, minId, maxId) > 0) {
            return R.error(400,"该关联关系已存在");
        }
        Map<String, Object> row = new LinkedHashMap<>(body);
        row.put("id", "class-group-" + UUID.randomUUID());
        row.put("class_id", minId);
        row.put("ref_class_id", maxId);
        row.put("group_type", type);
        row.putIfAbsent("status", 1);
        mapper.insertGroup(row);
        return R.ok(row);
    }

    /** 更新等价类/不相交类关系的备注等字段（不允许改关联的两个类）。 */
    @PutMapping("/class-group/{id}")
    public R<?> updateClassGroup(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("status", 1);
        mapper.updateGroup(body);
        return R.ok();
    }

    /** 删除一条等价类/不相交类关系记录。 */
    @DeleteMapping("/class-group/{id}")
    public R<?> deleteClassGroup(@PathVariable String id) {
        mapper.deleteGroup(id);
        return R.ok();
    }

    /* ============ 互斥并集 (Disjoint Union) ============ */

    /** 查询指定父类的互斥并集（DisjointUnion）子类成员列表。 */
    @GetMapping("/disjoint-union")
    public R<List<Map<String, Object>>> listDisjointUnion(@RequestParam String parentClassId) {
        return R.ok(mapper.listDisjointUnion(parentClassId));
    }

    /** 新增一条互斥并集成员记录，ID 格式 "class-disjoint-union-{UUID}"。 */
    @PostMapping("/disjoint-union")
    public R<Map<String, Object>> addDisjointUnion(@RequestBody Map<String, Object> body) {
        Map<String, Object> row = new LinkedHashMap<>(body);
        row.put("id", "class-disjoint-union-" + UUID.randomUUID());
        row.putIfAbsent("status", 1);
        mapper.insertDisjointUnion(row);
        return R.ok(row);
    }

    /** 更新互斥并集成员的字段（如排序、备注）。 */
    @PutMapping("/disjoint-union/{id}")
    public R<?> updateDisjointUnion(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("status", 1);
        mapper.updateDisjointUnion(body);
        return R.ok();
    }

    /** 删除一条互斥并集成员记录。 */
    @DeleteMapping("/disjoint-union/{id}")
    public R<?> deleteDisjointUnion(@PathVariable String id) {
        mapper.deleteDisjointUnion(id);
        return R.ok();
    }

    /* ============ 等价属性 / 互斥属性 ============ */

    /** 查询指定类的等价属性（owl:equivalentProperty）关系列表。 */
    @GetMapping("/property-equivalent")
    public R<List<Map<String, Object>>> listPropEquiv(@RequestParam String classId) {
        return R.ok(mapper.listPropertyEquivalentsByClass(classId));
    }

    /** 新增等价属性关系，ID 格式 "prop-equivalent-{UUID}"。 */
    @PostMapping("/property-equivalent")
    public R<Map<String, Object>> addPropEquiv(@RequestBody Map<String, Object> body) {
        Map<String, Object> row = new LinkedHashMap<>(body);
        row.put("id", "prop-equivalent-" + UUID.randomUUID());
        row.putIfAbsent("status", 1);
        mapper.insertPropertyEquivalent(row);
        return R.ok(row);
    }

    /** 更新等价属性关系记录。 */
    @PutMapping("/property-equivalent/{id}")
    public R<?> updatePropEquiv(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("status", 1);
        mapper.updatePropertyEquivalent(body);
        return R.ok();
    }

    /** 删除一条等价属性关系记录。 */
    @DeleteMapping("/property-equivalent/{id}")
    public R<?> deletePropEquiv(@PathVariable String id) {
        mapper.deletePropertyEquivalent(id);
        return R.ok();
    }

    /** 查询指定类的互斥属性（owl:propertyDisjointWith）关系列表。 */
    @GetMapping("/property-disjoint")
    public R<List<Map<String, Object>>> listPropDisjoint(@RequestParam String classId) {
        return R.ok(mapper.listPropertyDisjointByClass(classId));
    }

    /** 新增互斥属性关系，ID 格式 "prop-disjoint-{UUID}"。 */
    @PostMapping("/property-disjoint")
    public R<Map<String, Object>> addPropDisjoint(@RequestBody Map<String, Object> body) {
        Map<String, Object> row = new LinkedHashMap<>(body);
        row.put("id", "prop-disjoint-" + UUID.randomUUID());
        row.putIfAbsent("status", 1);
        mapper.insertPropertyDisjoint(row);
        return R.ok(row);
    }

    /** 更新互斥属性关系记录。 */
    @PutMapping("/property-disjoint/{id}")
    public R<?> updatePropDisjoint(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("status", 1);
        mapper.updatePropertyDisjoint(body);
        return R.ok();
    }

    /** 删除一条互斥属性关系记录。 */
    @DeleteMapping("/property-disjoint/{id}")
    public R<?> deletePropDisjoint(@PathVariable String id) {
        mapper.deletePropertyDisjoint(id);
        return R.ok();
    }

    /* ============ 类属性扩展 CRUD ============ */

    /** 查询指定对象类的全量属性列表（含 OWL 特性标志、值类型等完整字段）。 */
    @GetMapping("/classes/{classId}/properties")
    public R<List<Map<String, Object>>> listClassProps(@PathVariable String classId) {
        return R.ok(mapper.listClassPropertiesFull(classId));
    }

    /**
     * 为指定对象类新增一个属性。
     * ID 格式 "class-property-{UUID}"，RID 格式 "ri.ont.class.property.{id}"；
     * OWL 特性标志（functional/transitive 等）默认全为 0，前端按需传入覆盖。
     */
    @PostMapping("/classes/{classId}/properties")
    public R<Map<String, Object>> addClassProp(@PathVariable String classId, @RequestBody Map<String, Object> body) {
        Map<String, Object> row = new LinkedHashMap<>(body);
        String id = "class-property-" + UUID.randomUUID();
        row.put("id", id);
        row.put("class_id", classId);
        row.putIfAbsent("rid", "ri.ont.class.property." + id);
        row.putIfAbsent("prop_type", "data");
        row.putIfAbsent("status", 1);
        row.putIfAbsent("is_primary", 0);
        row.putIfAbsent("is_required", 0);
        row.putIfAbsent("is_key", 0);
        row.putIfAbsent("is_derived", 0);
        row.putIfAbsent("is_multi_valued_prop", 0);
        row.putIfAbsent("is_range_constraint_prop", 0);
        row.putIfAbsent("owl_functional", 0);
        row.putIfAbsent("owl_inverse_functional", 0);
        row.putIfAbsent("owl_transitive", 0);
        row.putIfAbsent("owl_symmetric", 0);
        row.putIfAbsent("owl_asymmetric", 0);
        row.putIfAbsent("owl_reflexive", 0);
        row.putIfAbsent("owl_irreflexive", 0);
        row.putIfAbsent("sort", 0);
        mapper.insertClassProperty(row);
        return R.ok(row);
    }

    /**
     * 更新指定属性的全量字段（含 OWL 特性标志）。
     * putIfAbsent 补默认值防止 MyBatis 动态 SQL 取到 null 时跳过字段更新。
     */
    @PutMapping("/properties/{propId}")
    public R<?> updateClassProp(@PathVariable String propId, @RequestBody Map<String, Object> body) {
        body.put("id", propId);
        body.putIfAbsent("prop_type", "data");
        body.putIfAbsent("status", 1);
        body.putIfAbsent("is_primary", 0);
        body.putIfAbsent("is_required", 0);
        body.putIfAbsent("is_key", 0);
        body.putIfAbsent("is_derived", 0);
        body.putIfAbsent("is_multi_valued_prop", 0);
        body.putIfAbsent("is_range_constraint_prop", 0);
        body.putIfAbsent("owl_functional", 0);
        body.putIfAbsent("owl_inverse_functional", 0);
        body.putIfAbsent("owl_transitive", 0);
        body.putIfAbsent("owl_symmetric", 0);
        body.putIfAbsent("owl_asymmetric", 0);
        body.putIfAbsent("owl_reflexive", 0);
        body.putIfAbsent("owl_irreflexive", 0);
        body.putIfAbsent("sort", 0);
        mapper.updateClassProperty(body);
        return R.ok();
    }

    /** 删除指定类属性记录。 */
    @DeleteMapping("/properties/{propId}")
    public R<?> deleteClassProp(@PathVariable String propId) {
        mapper.deleteClassProperty(propId);
        return R.ok();
    }

    /**
     * 拖拽排序: 接收类下属性 id 数组,按数组顺序设置每条的 sort 值 (0,1,2,...).
     * 请求体: { "ids": ["prop-1","prop-2", ...] }
     */
    @PostMapping("/classes/{classId}/properties/reorder")
    @SuppressWarnings("unchecked")
    public R<?> reorderClassProps(@PathVariable String classId, @RequestBody Map<String, Object> body) {
        List<String> ids = (List<String>) body.get("ids");
        if (ids == null || ids.isEmpty()) return R.error(400, "ids 必填");
        int i = 0;
        for (String pid : ids) {
            mapper.updateClassPropertySort(pid, i++);
        }
        return R.ok();
    }

    /* ============ ont_class_ds (类→物理表 绑定: 主表/附表) ============ */

    /**
     * 更新一条 类→物理表 绑定。
     *
     * <p>可改字段：别名(alias) / 中文名(table_label) / 主附表(rel_type: 1=主表, 2=附表) /
     * 关联键(join_on_keys, 格式 "主表字段=附表字段") / 连接方式(join_type: left|inner|right)。
     * 不更新 physical_fields(物理字段快照由建表/同步流程维护)。
     *
     * @param id   ont_class_ds 主键
     * @param body 待更新字段；缺省项用 putIfAbsent 补默认值, 避免 MyBatis 取参 NPE
     */
    @PutMapping("/datasources/{id}")
    public R<?> updateClassDs(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            body.put("id", id);
            body.putIfAbsent("rel_type", 2);
            body.putIfAbsent("status", 1);
            body.putIfAbsent("sort", 0);
            body.putIfAbsent("alias", null);
            body.putIfAbsent("pk_keys", null);
            body.putIfAbsent("join_on_keys", null);
            body.putIfAbsent("join_type", null);
            body.putIfAbsent("table_label", null);
            mapper.updateClassDs(body);
            return R.ok();
        } catch (Exception e) {
            log.error("更新类→物理表绑定失败, id={}, body={}", id, body, e);
            return R.error(500, "更新数据源绑定失败: " + e.getMessage());
        }
    }

    /**
     * 删除一条 类→物理表 绑定。
     *
     * <p>事务内先解除引用该绑定的属性映射(ont_class_property 的 class_ds_id/physical_table/physical_column 置空),
     * 再删除绑定本身, 避免关系图残留指向已删表的脏连线。
     *
     * @param id ont_class_ds 主键
     */
    @DeleteMapping("/datasources/{id}")
    @Transactional
    public R<?> deleteClassDs(@PathVariable String id) {
        try {
            mapper.clearPropMappingsByDs(id);
            mapper.deleteClassDs(id);
            return R.ok();
        } catch (Exception e) {
            log.error("删除类→物理表绑定失败, id={}", id, e);
            return R.error(500, "删除数据源绑定失败: " + e.getMessage());
        }
    }

    /* ============ 新建对象类型 (向导一站式创建) ============ */

    /**
     * 向导一站式创建对象类：含基础信息、分类绑定、初始属性等，由 ObjectTypeCreateService 统一事务处理。
     * 返回新建的对象类完整记录。
     */
    @PostMapping("/classes")
    public R<Map<String, Object>> createClass(@RequestBody Map<String, Object> body) {
        try {
            return R.ok(createService.create(body));
        } catch (Exception e) {
            // 之前只返回不记日志(静默吞异常), 排障时拿不到堆栈; 这里补上 error 日志
            log.error("创建对象类型失败, api_name={}", body == null ? null : body.get("api_name"), e);
            return R.error(500, "创建对象类型失败: " + e.getMessage());
        }
    }

    /* ============ 类基础保存 (概览各 sub-tab) ============ */

    /** 保存对象类基础信息（名称、编码、描述、分类、OWL 标志等），覆盖写全量字段。 */
    @PutMapping("/classes/{id}")
    public R<?> updateClass(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("status", 1);
        body.putIfAbsent("is_thing", 0);
        body.putIfAbsent("is_nothing", 0);
        body.putIfAbsent("is_common", 0);
        mapper.updateClassFull(body);
        return R.ok();
    }

    /** 仅切换对象类状态 (启用/禁用),不触碰其他字段。请求体: { "status": 0|1 } */
    @PutMapping("/classes/{id}/status")
    public R<?> updateClassStatus(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Object s = body.get("status");
        int status = (s instanceof Number) ? ((Number) s).intValue() : Integer.parseInt(String.valueOf(s));
        mapper.updateClassStatus(id, status == 1 ? 1 : 0);
        return R.ok();
    }

    /**
     * 删除对象类:被其他资源引用(子类 / 链接类型 / 类关系)时禁止删除并返回原因;
     * 否则级联清理类自有子数据(属性 / 表映射 / 动作 / 规则约束 / 分组关联)后删除主记录。
     * 返回: { deleted: true } 或 { deleted: false, blocked: true, reason: "..." }
     */
    @DeleteMapping("/classes/{id}")
    @Transactional
    public R<Map<String, Object>> deleteClass(@PathVariable String id) {
        Map<String, Object> result = new LinkedHashMap<>();

        int children   = mapper.countChildClasses(id);
        int linkTypes  = mapper.countLinkTypeRefs(id);
        int classLinks = mapper.countClassLinkRefs(id);
        if (children > 0 || linkTypes > 0 || classLinks > 0) {
            List<String> reasons = new ArrayList<>();
            if (children > 0)   reasons.add(children + " 个子类");
            if (linkTypes > 0)  reasons.add(linkTypes + " 个链接类型");
            if (classLinks > 0) reasons.add(classLinks + " 个类关系");
            result.put("deleted", false);
            result.put("blocked", true);
            result.put("reason", "被引用,无法删除:" + String.join("、", reasons));
            return R.ok(result);
        }

        mapper.deletePropertiesByClass(id);
        mapper.deleteClassDsByClass(id);
        mapper.deleteActionsByClass(id);
        mapper.deleteClassGroupByClass(id);
        mapper.deleteDisjointUnionByClass(id);
        mapper.deletePropEquivByClass(id);
        mapper.deletePropDisjointByClass(id);
        mapper.deleteInterfaceClassByClass(id);
        mapper.deleteGroupRefByClass(id);
        mapper.deleteClass(id);

        result.put("deleted", true);
        return R.ok(result);
    }
}
