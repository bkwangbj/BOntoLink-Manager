package com.beiktech.bontolink.service;

import com.beiktech.bontolink.entity.BizCategory;
import com.beiktech.bontolink.entity.IconLibGroup;
import com.beiktech.bontolink.entity.IconLibIcon;
import com.beiktech.bontolink.mapper.BizCategoryMapper;
import com.beiktech.bontolink.mapper.IconLibMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class IconLibService {

    @Autowired private IconLibMapper mapper;
    @Autowired private BizCategoryMapper categoryMapper;

    /** 列表：返回所有 group + 每组 icon 数,以及全部 icon(供前端按 groupId 索引) */
    public Map<String, Object> all() {
        List<IconLibGroup> groups = mapper.listGroups();
        List<IconLibIcon> icons = mapper.listIcons();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("groups", groups);
        r.put("icons", icons);
        return r;
    }

    public IconLibGroup createGroup(String parentId, String name) {
        // 严格限制 2 级
        if (parentId != null && !parentId.isEmpty()) {
            IconLibGroup parent = mapper.findGroup(parentId);
            if (parent == null) throw new IllegalArgumentException("父节点不存在");
            if (parent.getParentId() != null && !parent.getParentId().isEmpty()) {
                throw new IllegalArgumentException("最多支持两级目录");
            }
        }
        IconLibGroup g = new IconLibGroup();
        g.setId("ig-" + UUID.randomUUID());
        g.setParentId(parentId == null || parentId.isEmpty() ? null : parentId);
        g.setName(name);
        g.setSort(0);
        g.setSource("manual");
        mapper.insertGroup(g);
        return g;
    }

    public IconLibGroup renameGroup(String id, String name) {
        IconLibGroup g = mapper.findGroup(id);
        if (g == null) return null;
        g.setName(name);
        g.setSort(g.getSort() == null ? 0 : g.getSort());
        mapper.updateGroup(g);
        return g;
    }

    /** 删除组：连同子组与组内图标一并删除 */
    public void deleteGroup(String id) {
        // 收集自身 + 所有子组 id
        List<IconLibGroup> all = mapper.listGroups();
        Set<String> ids = new HashSet<>();
        ids.add(id);
        for (IconLibGroup g : all) {
            if (id.equals(g.getParentId())) ids.add(g.getId());
        }
        // 删除组内图标
        mapper.deleteIconsByGroups(ids);
        // 删除组本身
        mapper.deleteGroupBatch(ids);
    }

    public IconLibIcon addIcon(String groupId, String name, String viewBox, String content) {
        IconLibGroup g = mapper.findGroup(groupId);
        if (g == null) throw new IllegalArgumentException("目标分组不存在");
        IconLibIcon ic = new IconLibIcon();
        ic.setId("ii-" + UUID.randomUUID());
        ic.setGroupId(groupId);
        ic.setName(name);
        ic.setViewBox(viewBox == null || viewBox.isEmpty() ? "0 0 1024 1024" : viewBox);
        ic.setContent(content);
        ic.setSort(0);
        mapper.insertIcon(ic);
        return ic;
    }

    public void deleteIcon(String id) { mapper.deleteIcon(id); }
    public void deleteIconBatch(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        mapper.deleteIconBatch(ids);
    }

    /**
     * 按 行业 → 领域 结构种子建目录(仅在表为空时执行,或可手动 force=true 重新建)
     * - 一级:行业(category_type=1)
     * - 二级:领域(category_type=2)
     */
    public Map<String, Object> seedFromCategory(boolean force) {
        if (!force && mapper.countGroups() > 0) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("created", 0);
            r.put("skipped", true);
            return r;
        }
        List<BizCategory> all = categoryMapper.listAll();
        // 用 id 索引
        Map<String, BizCategory> byId = new HashMap<>();
        for (BizCategory c : all) byId.put(c.getId(), c);

        // 1) 行业列表
        List<BizCategory> industries = all.stream()
                .filter(c -> c.getCategoryType() != null && c.getCategoryType() == 1)
                .sorted(Comparator.comparing(c -> c.getSort() == null ? 0 : c.getSort()))
                .collect(Collectors.toList());

        // 2) 创建行业(顶级 group)与领域(二级 group)
        int created = 0;
        Map<String, String> categoryId2GroupId = new HashMap<>();
        for (BizCategory ind : industries) {
            IconLibGroup g = new IconLibGroup();
            g.setId("ig-" + UUID.randomUUID());
            g.setParentId(null);
            g.setName(ind.getRdfsLabel() == null || ind.getRdfsLabel().isEmpty() ? ind.getCategoryCode() : ind.getRdfsLabel());
            g.setSort(ind.getSort() == null ? 0 : ind.getSort());
            g.setSource("seed");
            mapper.insertGroup(g);
            categoryId2GroupId.put(ind.getId(), g.getId());
            created++;
        }
        // 领域
        List<BizCategory> domains = all.stream()
                .filter(c -> c.getCategoryType() != null && c.getCategoryType() == 2)
                .sorted(Comparator.comparing(c -> c.getSort() == null ? 0 : c.getSort()))
                .collect(Collectors.toList());
        for (BizCategory dom : domains) {
            String parentGroupId = null;
            // 向上回溯找行业
            BizCategory cur = dom;
            while (cur != null && cur.getParentId() != null && !"0".equals(cur.getParentId())) {
                BizCategory p = byId.get(cur.getParentId());
                if (p == null) break;
                if (p.getCategoryType() != null && p.getCategoryType() == 1) {
                    parentGroupId = categoryId2GroupId.get(p.getId());
                    break;
                }
                cur = p;
            }
            if (parentGroupId == null) continue; // 没找到行业则跳过
            IconLibGroup g = new IconLibGroup();
            g.setId("ig-" + UUID.randomUUID());
            g.setParentId(parentGroupId);
            g.setName(dom.getRdfsLabel() == null || dom.getRdfsLabel().isEmpty() ? dom.getCategoryCode() : dom.getRdfsLabel());
            g.setSort(dom.getSort() == null ? 0 : dom.getSort());
            g.setSource("seed");
            mapper.insertGroup(g);
            created++;
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("created", created);
        r.put("skipped", false);
        return r;
    }
}
