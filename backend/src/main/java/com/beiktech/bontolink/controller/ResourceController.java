package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.mapper.BizCategoryMapper;
import com.beiktech.bontolink.mapper.BizNamespaceMapper;
import com.beiktech.bontolink.mapper.OntologyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 工作区&资源相关只读 API：Discover / 对象类型 / 关系 / 动作 / 接口 / 属性
 */
@RestController
@RequestMapping("/api/resource")
public class ResourceController {

    @Autowired private OntologyMapper ontologyMapper;
    @Autowired private BizCategoryMapper categoryMapper;
    @Autowired private BizNamespaceMapper namespaceMapper;

    @GetMapping("/discover/stats")
    public R<Map<String, Object>> discoverStats() {
        Map<String, Object> r = new LinkedHashMap<>();
        long industries = categoryMapper.listAll().stream().filter(c -> c.getCategoryType() != null && c.getCategoryType() == 1).count();
        long domains = categoryMapper.listAll().stream().filter(c -> c.getCategoryType() != null && c.getCategoryType() == 2).count();
        r.put("industry", industries);
        r.put("domain", domains);
        r.put("ontClass", ontologyMapper.countClasses());
        r.put("ontInterface", ontologyMapper.countInterfaces());
        r.put("ontLink", ontologyMapper.countLinks());
        r.put("ontAction", ontologyMapper.countActions());
        r.put("ontProperty", ontologyMapper.countProperties());
        r.put("namespace", namespaceMapper.listAll().size());
        return R.ok(r);
    }

    @GetMapping("/classes")
    public R<List<Map<String, Object>>> classes() { return R.ok(ontologyMapper.listClasses()); }

    @GetMapping("/links")
    public R<List<Map<String, Object>>> links() { return R.ok(ontologyMapper.listLinks()); }

    @GetMapping("/actions")
    public R<List<Map<String, Object>>> actions() { return R.ok(ontologyMapper.listActions()); }

    @GetMapping("/interfaces")
    public R<List<Map<String, Object>>> interfaces() { return R.ok(ontologyMapper.listInterfaces()); }

    @GetMapping("/interfaces/{id}")
    public R<Map<String, Object>> interfaceDetail(@PathVariable String id) {
        Map<String, Object> r = new LinkedHashMap<>();
        Map<String, Object> info = ontologyMapper.findInterfaceById(id);
        if (info == null) return R.ok(r);
        r.putAll(info);
        r.put("properties", ontologyMapper.listInterfaceProperties(id));
        return R.ok(r);
    }

    @GetMapping("/classes/{id}/properties")
    public R<List<Map<String, Object>>> properties(@PathVariable String id) {
        return R.ok(ontologyMapper.listProperties(id));
    }

    /** 图谱：返回行业-领域-分组-对象-接口节点 + 边 */
    @GetMapping("/graph")
    public R<Map<String, Object>> graph() {
        Map<String, Object> g = new LinkedHashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        categoryMapper.listAll().forEach(c -> {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", c.getId());
            n.put("label", c.getRdfsLabel() == null ? c.getCategoryCode() : c.getRdfsLabel());
            n.put("kind", c.getCategoryType() == 1 ? "industry"
                       : c.getCategoryType() == 2 ? "domain" : "group");
            n.put("color", c.getColor());
            n.put("icon", c.getIcon());
            nodes.add(n);
            if (c.getParentId() != null && !"0".equals(c.getParentId())) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("source", c.getParentId());
                e.put("target", c.getId());
                e.put("kind", "hierarchy");
                edges.add(e);
            }
        });
        ontologyMapper.listClasses().forEach(c -> {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", c.get("id"));
            n.put("label", c.get("display_name") == null ? c.get("api_name") : c.get("display_name"));
            n.put("kind", "class");
            n.put("color", c.get("color"));
            n.put("icon", c.get("icon"));
            nodes.add(n);
        });
        ontologyMapper.listLinks().forEach(l -> {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("source", l.get("source_class_id"));
            e.put("target", l.get("target_class_id"));
            e.put("label", l.get("display_name"));
            e.put("kind", "link");
            edges.add(e);
        });
        g.put("nodes", nodes);
        g.put("edges", edges);
        return R.ok(g);
    }
}
