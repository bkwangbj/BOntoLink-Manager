package com.beiktech.bontolink.service;

import com.beiktech.bontolink.entity.BizCategory;
import com.beiktech.bontolink.entity.BizGroup;
import com.beiktech.bontolink.mapper.BizCategoryMapper;
import com.beiktech.bontolink.mapper.BizGroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GroupService {
    @Autowired private BizGroupMapper mapper;
    @Autowired private BizCategoryMapper categoryMapper;

    public List<BizGroup> listByParent(String parentId) { return mapper.listByParent(parentId); }

    public List<BizGroup> listAll() { return mapper.listAll(); }

    public BizGroup create(BizGroup g) {
        if (g.getId() == null) g.setId("group-" + UUID.randomUUID());
        if (g.getGSort() == null) g.setGSort(0);
        mapper.insert(g);
        return g;
    }

    public BizGroup update(BizGroup g) {
        mapper.update(g);
        return g;
    }

    public void delete(String id) { mapper.delete(id); }

    public List<Map<String, Object>> listClasses(String groupId) { return mapper.listGroupClasses(groupId); }

    /**
     * 分组成员管理 —— 基于分类节点（categoryType=3）的 category_code 操作
     * 兼容现有 ont_biz_group 数据：若已有匹配 category_code 的 group 则复用其 group_id，
     * 否则用 "auto-" 前缀生成 group_id（不影响业务，主要让 NOT NULL 字段有值）
     */

    public void addMember(String categoryId, String classId) {
        BizCategory cat = categoryMapper.findById(categoryId);
        if (cat == null) throw new IllegalArgumentException("分类不存在");
        if (cat.getCategoryType() == null || cat.getCategoryType() != 3) {
            throw new IllegalArgumentException("仅分组节点支持成员管理");
        }
        String code = cat.getCategoryCode();
        if (mapper.existsMember(code, classId) > 0) return;     // 已存在 -> 跳过
        String groupId = mapper.findGroupIdByCategoryCode(code);
        if (groupId == null || groupId.isEmpty()) groupId = "auto-" + code;
        int sort = mapper.maxSortByCategoryCode(code) + 1;
        mapper.insertMember("gc-" + UUID.randomUUID(), groupId, classId, code, sort);
    }

    public void removeMember(String categoryId, String classId) {
        BizCategory cat = categoryMapper.findById(categoryId);
        if (cat == null) throw new IllegalArgumentException("分类不存在");
        mapper.deleteMember(cat.getCategoryCode(), classId);
    }

    public void reorderMembers(String categoryId, List<String> classIds) {
        BizCategory cat = categoryMapper.findById(categoryId);
        if (cat == null) throw new IllegalArgumentException("分类不存在");
        int sort = 1;
        for (String classId : classIds) {
            mapper.updateMemberSort(cat.getCategoryCode(), classId, sort++);
        }
    }
}
