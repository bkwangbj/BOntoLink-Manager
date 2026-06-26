package com.beiktech.bontolink.service;

import com.beiktech.bontolink.entity.BizCategory;
import com.beiktech.bontolink.entity.BizNamespace;
import com.beiktech.bontolink.mapper.BizCategoryMapper;
import com.beiktech.bontolink.mapper.BizNamespaceMapper;
import com.beiktech.bontolink.mapper.OntologyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 行业分类（BizCategory）的查询、树形构建、统计与图谱服务。
 * 分类分三级：行业(type=1) → 领域(type=2) → 分组(type=3)。
 */
@Service
public class CategoryService {

    @Autowired private BizCategoryMapper categoryMapper;
    @Autowired private OntologyMapper ontologyMapper;
    @Autowired private BizNamespaceMapper namespaceMapper;
    @Autowired private NamespaceService namespaceService;

    /**
     * 查询所有分类并组装为嵌套树（parentId=null/"0" 的节点为根）。
     * 父节点不存在时该节点归入根层，防止孤儿节点消失。
     */
    public List<Map<String, Object>> tree() {
        List<BizCategory> all = categoryMapper.listAll();
        Map<String, Map<String, Object>> idx = new LinkedHashMap<>();
        for (BizCategory c : all) {
            Map<String, Object> n = toNode(c);
            n.put("children", new ArrayList<Map<String, Object>>());
            idx.put(c.getId(), n);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (BizCategory c : all) {
            Map<String, Object> node = idx.get(c.getId());
            if (c.getParentId() == null || "0".equals(c.getParentId())) {
                roots.add(node);
            } else {
                Map<String, Object> parent = idx.get(c.getParentId());
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                    children.add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    private Map<String, Object> toNode(BizCategory c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("parentId", c.getParentId());
        m.put("rid", c.getRid());
        m.put("categoryCode", c.getCategoryCode());
        m.put("categoryType", c.getCategoryType());
        m.put("nsCode", c.getNsCode());
        m.put("status", c.getStatus());
        m.put("sort", c.getSort());
        m.put("icon", c.getIcon());
        m.put("color", c.getColor());
        m.put("rdfsLabel", c.getRdfsLabel());
        m.put("rdfsComment", c.getRdfsComment());
        m.put("description", c.getDescription());
        m.put("metadata", c.getMetadata());
        m.put("createTime", c.getCreateTime());
        m.put("updateTime", c.getUpdateTime());
        m.put("label", c.getRdfsLabel() != null && !c.getRdfsLabel().isEmpty()
                ? c.getRdfsLabel()
                : c.getCategoryCode());
        return m;
    }

    /** 按 id 查单条分类。 */
    public BizCategory get(String id) { return categoryMapper.findById(id); }

    /**
     * 新建分类节点。
     * - id 缺失时自动生成（格式 "category-{UUID}"），rid 同步生成。
     * - 领域(type=2)创建后自动创建同 code 的命名空间（若不存在），并将自身绑定该命名空间。
     * - 分组(type=3)的 nsCode 强制继承父级领域，前端传入值被忽略。
     */
    public BizCategory create(BizCategory c) {
        // 行业只能是第一级
        if (c.getCategoryType() != null && c.getCategoryType() == 1
                && c.getParentId() != null && !"0".equals(c.getParentId())) {
            throw new IllegalArgumentException("行业只能是第一级分类");
        }
        if (c.getId() == null || c.getId().isEmpty()) {
            c.setId("category-" + UUID.randomUUID());
        }
        if (c.getRid() == null) {
            c.setRid("ri.ont.biz.category." + c.getId().replaceFirst("^category-", ""));
        }
        if (c.getStatus() == null) c.setStatus(1);
        if (c.getSort() == null) c.setSort(0);
        if (c.getParentId() == null) c.setParentId("0");

        // 分组的命名空间只能继承所属领域（父级）的命名空间，忽略前端传入值
        if (c.getCategoryType() != null && c.getCategoryType() == 3
                && c.getParentId() != null && !"0".equals(c.getParentId())) {
            BizCategory parent = categoryMapper.findById(c.getParentId());
            c.setNsCode(parent != null ? parent.getNsCode() : null);
        }
        categoryMapper.insert(c);

        // 创建领域时自动创建命名空间（nsCode = categoryCode）
        if (c.getCategoryType() != null && c.getCategoryType() == 2) {
            String code = c.getCategoryCode();
            BizNamespace existing = namespaceMapper.findByCode(code);
            if (existing == null) {
                BizNamespace ns = new BizNamespace();
                ns.setNsCode(code);
                ns.setNsName(c.getRdfsLabel() != null ? c.getRdfsLabel() : code);
                ns.setNsUri("http://ont.beiktech.com/ns/" + code);
                ns.setHierarchyPath(code);
                // 通过 NamespaceService 创建，自动生成版本记录 v1.0
                namespaceService.create(ns);
            }
            // 领域绑定到该命名空间
            c.setNsCode(code);
            categoryMapper.update(c);
        }

        return c;
    }

    /**
     * 更新分类节点。分组(type=3)的 nsCode 始终跟随父级领域，不允许独立修改。
     * 返回更新后的完整对象。
     */
    public BizCategory update(BizCategory c) {
        // 分组的命名空间始终跟随所属领域，不允许独立修改
        if (c.getCategoryType() != null && c.getCategoryType() == 3
                && c.getParentId() != null && !"0".equals(c.getParentId())) {
            BizCategory parent = categoryMapper.findById(c.getParentId());
            c.setNsCode(parent != null ? parent.getNsCode() : null);
        }
        categoryMapper.update(c);
        return categoryMapper.findById(c.getId());
    }

    /**
     * 删除分类节点。存在下级子节点时拒绝删除（防止孤儿节点）。
     */
    public void delete(String id) {
        int children = categoryMapper.countByParent(id);
        if (children > 0) throw new IllegalArgumentException("存在下级节点，无法删除");
        categoryMapper.delete(id);
    }

    /** 中间统计区：基于 category_code 聚合直属下级 / 关联资源 */
    public Map<String, Object> stats(String id) {
        List<BizCategory> all = categoryMapper.listAll();
        return statsInternal(id, all, indexByParent(all));
    }

    /**
     * 「常用领域选择」弹窗数据：
     *   tree  = 仅含有下级的行业/领域（叶子不进入树）
     *   leaves= 所有叶子领域（type=2 且无子分组，或 type=3 分组），用于右侧待选区
     */
    public Map<String, Object> picker() {
        List<BizCategory> all = categoryMapper.listAll();
        Map<String, List<BizCategory>> idx = indexByParent(all);
        Map<String, BizCategory> byId = new HashMap<>();
        for (BizCategory c : all) byId.put(c.getId(), c);

        // 树形：递归构建，只保留有 children 的节点
        List<Map<String, Object>> tree = buildPickerTree(all, idx, "0");

        // 叶子
        List<Map<String, Object>> leaves = new ArrayList<>();
        for (BizCategory c : all) {
            if (c.getCategoryType() == null) continue;
            boolean isLeaf = (c.getCategoryType() == 3)
                    || (c.getCategoryType() == 2 && idx.getOrDefault(c.getId(), Collections.emptyList()).isEmpty());
            if (!isLeaf) continue;

            // 向上找根（行业）
            BizCategory ind = c;
            while (ind.getParentId() != null && !"0".equals(ind.getParentId())) {
                BizCategory p = byId.get(ind.getParentId());
                if (p == null) break;
                ind = p;
            }

            Map<String, Object> s = statsInternal(c.getId(), all, idx);

            Map<String, Object> leaf = new LinkedHashMap<>();
            leaf.put("id", c.getId());
            leaf.put("categoryCode", c.getCategoryCode());
            leaf.put("label", labelOf(c));
            leaf.put("nsCode", c.getNsCode());
            leaf.put("description", c.getRdfsComment() != null ? c.getRdfsComment() : c.getDescription());
            leaf.put("color", c.getColor());
            leaf.put("icon", c.getIcon());
            leaf.put("rid", c.getRid());
            leaf.put("categoryType", c.getCategoryType());
            leaf.put("industryId", ind.getId());
            leaf.put("industryLabel", labelOf(ind));
            leaf.put("industryColor", ind.getColor());
            leaf.put("industryNsCode", ind.getNsCode());
            leaf.put("objectCount", s.get("classCount"));
            leaf.put("linkCount", s.get("linkCount"));
            leaf.put("propertyCount", s.get("propertyCount"));
            leaf.put("version", "1.0");
            leaves.add(leaf);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tree", tree);
        out.put("leaves", leaves);
        return out;
    }

    /** 递归构建 picker 树，叶子节点（无子节点）不进入树，只出现在 leaves 区。 */
    private List<Map<String, Object>> buildPickerTree(List<BizCategory> all,
                                                     Map<String, List<BizCategory>> idx,
                                                     String parentId) {
        List<BizCategory> children = idx.getOrDefault(parentId, Collections.emptyList());
        List<Map<String, Object>> out = new ArrayList<>();
        for (BizCategory c : children) {
            List<BizCategory> myChildren = idx.getOrDefault(c.getId(), Collections.emptyList());
            if (myChildren.isEmpty()) continue;   // 叶子不进入树
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", c.getId());
            node.put("categoryCode", c.getCategoryCode());
            node.put("label", labelOf(c));
            node.put("nsCode", c.getNsCode());
            node.put("color", c.getColor());
            node.put("icon", c.getIcon());
            node.put("categoryType", c.getCategoryType());
            node.put("children", buildPickerTree(all, idx, c.getId()));
            out.add(node);
        }
        return out;
    }

    /** 优先返回 rdfsLabel，为空时降级为 categoryCode。 */
    private String labelOf(BizCategory c) {
        return c.getRdfsLabel() != null && !c.getRdfsLabel().isEmpty() ? c.getRdfsLabel() : c.getCategoryCode();
    }

    /** 节点的关系图谱：节点 = 子树下所有 class，边 = 节点间的 link */
    public Map<String, Object> graph(String id) {
        List<BizCategory> all = categoryMapper.listAll();
        Map<String, List<BizCategory>> idx = indexByParent(all);
        Set<String> classIds = resolveClassIds(id, all, idx);
        Map<String, Object> r = new LinkedHashMap<>();
        if (classIds.isEmpty()) {
            r.put("nodes", Collections.emptyList());
            r.put("edges", Collections.emptyList());
            return r;
        }
        r.put("nodes", ontologyMapper.findClassesByIds(classIds));
        r.put("edges", ontologyMapper.findLinksWithin(classIds));
        return r;
    }

    /**
     * 收集指定节点子树下关联的所有 class id。
     * 领域/行业节点走 category_code 直查 ont_class，分组节点走 ont_biz_group_class。
     */
    private Set<String> resolveClassIds(String id, List<BizCategory> all, Map<String, List<BizCategory>> idx) {
        BizCategory self = all.stream().filter(c -> id.equals(c.getId())).findFirst().orElse(null);
        if (self == null) return Collections.emptySet();
        List<BizCategory> subtree = new ArrayList<>();
        Deque<BizCategory> q = new ArrayDeque<>();
        q.add(self);
        while (!q.isEmpty()) {
            BizCategory cur = q.poll();
            subtree.add(cur);
            q.addAll(idx.getOrDefault(cur.getId(), Collections.emptyList()));
        }
        Set<String> domainCodes = new HashSet<>();
        Set<String> groupCodes = new HashSet<>();
        for (BizCategory c : subtree) {
            if (c.getCategoryCode() == null) continue;
            if (c.getCategoryType() != null && c.getCategoryType() == 3) groupCodes.add(c.getCategoryCode());
            else domainCodes.add(c.getCategoryCode());
        }
        Set<String> classIds = new HashSet<>();
        if (!domainCodes.isEmpty()) classIds.addAll(ontologyMapper.findClassIdsByCategoryCodes(domainCodes));
        if (!groupCodes.isEmpty())  classIds.addAll(ontologyMapper.findClassIdsByGroupCategoryCodes(groupCodes));
        return classIds;
    }

    /** 批量返回多个节点的统计（只查一次 listAll，效率显著优于循环 stats） */
    public Map<String, Map<String, Object>> statsBatch(List<String> ids) {
        Map<String, Map<String, Object>> r = new LinkedHashMap<>();
        if (ids == null || ids.isEmpty()) return r;
        List<BizCategory> all = categoryMapper.listAll();
        Map<String, List<BizCategory>> idx = indexByParent(all);
        for (String id : ids) r.put(id, statsInternal(id, all, idx));
        return r;
    }

    /** 将所有分类按 parentId 分组，便于 O(1) 查子节点，null parentId 归入 "0" 桶。 */
    private Map<String, List<BizCategory>> indexByParent(List<BizCategory> all) {
        Map<String, List<BizCategory>> m = new HashMap<>();
        for (BizCategory c : all) {
            m.computeIfAbsent(c.getParentId() == null ? "0" : c.getParentId(), k -> new ArrayList<>()).add(c);
        }
        return m;
    }

    /**
     * 内部统计实现：BFS 遍历子树，汇总子节点数/对象数/链接数/属性数/接口数等指标，
     * 并截取前 8 个 class 作为 classChips 供页面气泡展示。
     * @param all 全量分类列表（外部一次性查出，避免重复查库）
     * @param idx parentId → children 的索引
     */
    private Map<String, Object> statsInternal(String id,
                                              List<BizCategory> all,
                                              Map<String, List<BizCategory>> idx) {
        Map<String, Object> r = new LinkedHashMap<>();
        BizCategory self = all.stream().filter(c -> id.equals(c.getId())).findFirst().orElse(null);
        if (self == null) return r;

        // 自身 + 所有后代节点
        List<BizCategory> subtree = new ArrayList<>();
        Deque<BizCategory> q = new ArrayDeque<>();
        q.add(self);
        while (!q.isEmpty()) {
            BizCategory cur = q.poll();
            subtree.add(cur);
            List<BizCategory> kids = idx.getOrDefault(cur.getId(), Collections.emptyList());
            q.addAll(kids);
        }

        Set<String> domainCodes = new HashSet<>();  // 行业/领域 节点的 category_code → 直查 ont_class
        Set<String> groupCodes  = new HashSet<>();  // 分组节点的 category_code → 走 ont_biz_group_class
        Set<String> nsCodes     = new HashSet<>();
        for (BizCategory c : subtree) {
            if (c.getCategoryCode() != null) {
                if (c.getCategoryType() != null && c.getCategoryType() == 3) {
                    groupCodes.add(c.getCategoryCode());
                } else {
                    domainCodes.add(c.getCategoryCode());
                }
            }
            if (c.getNsCode() != null) nsCodes.add(c.getNsCode());
        }

        Set<String> classIds = new HashSet<>();
        if (!domainCodes.isEmpty()) classIds.addAll(ontologyMapper.findClassIdsByCategoryCodes(domainCodes));
        if (!groupCodes.isEmpty())  classIds.addAll(ontologyMapper.findClassIdsByGroupCategoryCodes(groupCodes));

        int childCount    = idx.getOrDefault(id, Collections.emptyList()).size();
        int classCount    = classIds.size();
        int linkCount     = classIds.isEmpty() ? 0 : ontologyMapper.countLinksByClassIds(classIds);
        int actionCount   = classIds.isEmpty() ? 0 : ontologyMapper.countActionsByClassIds(classIds);
        int propertyCount = classIds.isEmpty() ? 0 : ontologyMapper.countPropertiesByClassIds(classIds);
        int interfaceCount= nsCodes.isEmpty() ? 0 : ontologyMapper.countInterfacesByNsCodes(nsCodes);

        // 取前 8 个对象作为 chip 展示
        List<Map<String, Object>> chips = classIds.isEmpty()
                ? Collections.emptyList()
                : ontologyMapper.findClassesByIds(classIds);
        List<Map<String, Object>> classChips = new ArrayList<>(chips.size());
        for (int i = 0; i < chips.size() && i < 8; i++) classChips.add(chips.get(i));

        r.put("childCount", childCount);
        r.put("classCount", classCount);
        r.put("linkCount", linkCount);
        r.put("actionCount", actionCount);
        r.put("functionCount", 0);
        r.put("interfaceCount", interfaceCount);
        r.put("propertyCount", propertyCount);
        r.put("classChips", classChips);
        return r;
    }
}
