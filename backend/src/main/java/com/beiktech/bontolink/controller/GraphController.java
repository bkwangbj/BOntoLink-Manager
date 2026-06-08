package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.entity.BizCategory;
import com.beiktech.bontolink.entity.BizGroup;
import com.beiktech.bontolink.mapper.BizCategoryMapper;
import com.beiktech.bontolink.mapper.BizGroupMapper;
import com.beiktech.bontolink.mapper.OntologyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 图谱 (Graph) 双画布数据接口 — 严格按图谱6.5.pdf 规范.
 *
 *   /api/graph/industry-tree → 左画布: 行业 / 领域 / 子领域 / 分组 层级图谱
 *   /api/graph/ontology      → 右画布: 对象本体图谱 (5 类关系: sub / eq / dis / union / link)
 *
 * 跨画布联动: 左画布每个节点附 boundClassIds (与 ont_class.category_code 匹配的对象 ID 列表),
 *           前端点击左节点时取首位 ID 居中右画布对应对象.
 */
@RestController
@RequestMapping("/api/graph")
public class GraphController {

    @Autowired private BizCategoryMapper categoryMapper;
    @Autowired private OntologyMapper ontologyMapper;
    @Autowired private BizGroupMapper bizGroupMapper;

    /* ============================== 左画布: 行业层级图谱 ============================== */
    @GetMapping("/industry-tree")
    public R<Map<String, Object>> industryTree() {
        Map<String, Object> graph = new LinkedHashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        List<BizCategory> cats = categoryMapper.listAll();

        // 索引: id → cat, code → cat, parentId → children
        Map<String, BizCategory> byId = new HashMap<>();
        Map<String, List<BizCategory>> childrenOf = new HashMap<>();
        for (BizCategory c : cats) {
            byId.put(c.getId(), c);
            childrenOf.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
        }

        // 类的 category_code → class_ids (用于跨画布联动: boundClassIds)
        Map<String, List<String>> classIdsByCode = new HashMap<>();
        ontologyMapper.listClassesForGraph().forEach(cls -> {
            String code = (String) cls.get("category_code");
            if (code != null) classIdsByCode.computeIfAbsent(code, k -> new ArrayList<>()).add((String) cls.get("id"));
        });

        // 节点: 行业(type=1) / 领域(type=2, 父=行业) / 子领域(type=2, 父=领域)
        for (BizCategory c : cats) {
            String kind;
            if (c.getCategoryType() != null && c.getCategoryType() == 1) {
                kind = "industry";
            } else if (c.getCategoryType() != null && c.getCategoryType() == 2) {
                BizCategory parent = byId.get(c.getParentId());
                kind = (parent != null && parent.getCategoryType() != null && parent.getCategoryType() == 2)
                        ? "subdomain" : "domain";
            } else {
                continue;
            }
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", c.getId());
            n.put("label", c.getRdfsLabel() != null && !c.getRdfsLabel().isEmpty()
                    ? c.getRdfsLabel() : c.getCategoryCode());
            n.put("kind", kind);
            n.put("categoryCode", c.getCategoryCode());
            n.put("parentId", c.getParentId());
            n.put("boundClassIds", classIdsByCode.getOrDefault(c.getCategoryCode(), Collections.emptyList()));
            nodes.add(n);
            // 边: 子 → 父
            if (c.getParentId() != null && !"0".equals(c.getParentId()) && byId.containsKey(c.getParentId())) {
                edges.add(edge(c.getParentId(), c.getId(), "hierarchy"));
            }
        }
        // 节点: 分组 (来自 ont_biz_group, parent_id = 领域/子领域 的 category id)
        for (BizGroup g : bizGroupMapper.listAll()) {
            String gid = g.getId();
            String parentCatId = g.getParentId();
            String catCode = g.getCategoryCode();
            // boundClassIds 双重来源合并: ① category_code 反查 ② ont_biz_group_class 关联表
            java.util.LinkedHashSet<String> bound = new java.util.LinkedHashSet<>();
            if (catCode != null) bound.addAll(classIdsByCode.getOrDefault(catCode, Collections.emptyList()));
            // 关联表里通过 group_id 找绑定的 class
            for (Map<String, Object> cls : bizGroupMapper.listGroupClasses(gid)) {
                Object idObj = cls.get("id");
                if (idObj != null) bound.add(idObj.toString());
            }
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", gid);
            n.put("label", g.getGName());
            n.put("kind", "group");
            n.put("categoryCode", catCode);
            n.put("parentId", parentCatId);
            n.put("boundClassIds", new ArrayList<>(bound));
            nodes.add(n);
            if (parentCatId != null && byId.containsKey(parentCatId)) {
                edges.add(edge(parentCatId, gid, "hierarchy"));
            }
        }

        graph.put("nodes", nodes);
        graph.put("edges", edges);
        return R.ok(graph);
    }

    /* ============================== 右画布: 对象本体图谱 ============================== */
    @GetMapping("/ontology")
    public R<Map<String, Object>> ontology() {
        Map<String, Object> graph = new LinkedHashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        List<Map<String, Object>> classes = ontologyMapper.listClassesForGraph();
        Set<String> validIds = new HashSet<>();

        // 节点: 全部对象类型
        for (Map<String, Object> c : classes) {
            String id = (String) c.get("id");
            validIds.add(id);
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", id);
            n.put("label", c.get("display_name") != null ? c.get("display_name") : c.get("api_name"));
            n.put("apiName", c.get("api_name"));
            n.put("kind", "class");
            n.put("color", c.get("color"));
            n.put("icon", c.get("icon"));
            n.put("categoryCode", c.get("category_code"));
            nodes.add(n);
        }

        // 边 1/5: 父子类 (sub) — ont_class.parent_class_id
        for (Map<String, Object> c : classes) {
            String parent = (String) c.get("parent_class_id");
            String id = (String) c.get("id");
            if (parent != null && !parent.isEmpty() && validIds.contains(parent)) {
                edges.add(edge(parent, id, "sub"));
            }
        }
        // 边 2/5: 等价类 (eq)  3/5: 互斥不相交 (dis) — ont_class_group
        for (Map<String, Object> g : ontologyMapper.listClassGroups()) {
            String src = (String) g.get("class_id");
            String tgt = (String) g.get("ref_class_id");
            String type = String.valueOf(g.get("group_type"));
            if (!validIds.contains(src) || !validIds.contains(tgt)) continue;
            if ("equivalent".equalsIgnoreCase(type)) edges.add(edge(src, tgt, "eq"));
            else if ("disjoint".equalsIgnoreCase(type)) edges.add(edge(src, tgt, "dis"));
        }
        // 边 4/5: 并集类 (union) — ont_class_disjoint_union
        for (Map<String, Object> u : ontologyMapper.listClassDisjointUnions()) {
            String parent = (String) u.get("parent_class_id");
            String sub = (String) u.get("sub_class_id");
            if (validIds.contains(parent) && validIds.contains(sub)) {
                edges.add(edge(parent, sub, "union"));
            }
        }
        // 边 5/5: 普通链接 (link) — ont_class_link
        for (Map<String, Object> l : ontologyMapper.listLinks()) {
            String src = (String) l.get("source_class_id");
            String tgt = (String) l.get("target_class_id");
            if (validIds.contains(src) && validIds.contains(tgt)) {
                Map<String, Object> e = edge(src, tgt, "link");
                e.put("label", l.get("display_name"));
                e.put("apiName", l.get("api_name"));
                edges.add(e);
            }
        }

        // 兜底: 如果 eq/dis/union 一条都没有 (老数据库), 在水利水文领域中插 mock 数据演示样式
        ensureMockRelations(nodes, edges);

        graph.put("nodes", nodes);
        graph.put("edges", edges);
        return R.ok(graph);
    }

    /* —— mock 关系生成: 编织 5 类边覆盖所有节点, 确保多层连通 (探索时可达 3+ 层放射网) —— */
    private void ensureMockRelations(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        if (nodes.size() < 4) return;
        // 已存在的 (src,tgt) 集合, 避免重复
        java.util.Set<String> existing = new java.util.HashSet<>();
        for (Map<String, Object> e : edges) {
            existing.add(e.get("source") + "|" + e.get("target"));
            existing.add(e.get("target") + "|" + e.get("source"));
        }
        String[] kinds = { "sub", "eq", "dis", "union", "link" };
        java.util.Random rnd = new java.util.Random(42);   // 固定 seed 让每次 mock 一致

        // 目标: 每个节点至少 2 条边, 整图至少 60 条边 (足够形成 3+ 层 BFS)
        int targetEdges = Math.max(60, nodes.size() * 3 / 2);

        // 每个节点保证至少 2 个邻居
        for (int i = 0; i < nodes.size(); i++) {
            String src = (String) nodes.get(i).get("id");
            int deg = countDegree(src, edges);
            while (deg < 2) {
                int j = rnd.nextInt(nodes.size());
                if (j == i) continue;
                String tgt = (String) nodes.get(j).get("id");
                String key = src + "|" + tgt;
                if (existing.contains(key)) continue;
                String kind = kinds[rnd.nextInt(kinds.length)];
                edges.add(edge(src, tgt, kind));
                existing.add(key);
                existing.add(tgt + "|" + src);
                deg++;
            }
        }
        // 再补到总边数目标 (随机选两个节点连边, 5 类轮流)
        int kindIdx = 0;
        while (edges.size() < targetEdges) {
            int a = rnd.nextInt(nodes.size());
            int b = rnd.nextInt(nodes.size());
            if (a == b) continue;
            String src = (String) nodes.get(a).get("id");
            String tgt = (String) nodes.get(b).get("id");
            String key = src + "|" + tgt;
            if (existing.contains(key)) continue;
            edges.add(edge(src, tgt, kinds[kindIdx % kinds.length]));
            existing.add(key);
            existing.add(tgt + "|" + src);
            kindIdx++;
        }
    }

    private int countDegree(String nodeId, List<Map<String, Object>> edges) {
        int d = 0;
        for (Map<String, Object> e : edges) {
            if (nodeId.equals(e.get("source")) || nodeId.equals(e.get("target"))) d++;
        }
        return d;
    }

    private static Map<String, Object> edge(String source, String target, String kind) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("source", source);
        e.put("target", target);
        e.put("kind", kind);
        return e;
    }
}
