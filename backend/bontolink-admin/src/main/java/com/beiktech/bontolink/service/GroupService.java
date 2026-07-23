package com.beiktech.bontolink.service;

import com.beiktech.bontolink.data.entity.BizCategory;
import com.beiktech.bontolink.data.entity.BizGroup;
import com.beiktech.bontolink.data.mapper.BizCategoryMapper;
import com.beiktech.bontolink.data.mapper.BizGroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 业务分组服务：管理 ont_biz_group 分组的 CRUD，以及基于分类节点（categoryType=3）的成员（对象类）管理。
 */
@Service
public class GroupService {
    @Autowired private BizGroupMapper mapper;
    @Autowired private BizCategoryMapper categoryMapper;

    /** 按父组 ID 查询直属子分组列表，parentId 为 null 时返回顶级分组。 */
    public List<BizGroup> listByParent(String parentId) { return mapper.listByParent(parentId); }

    /** 查询所有分组（不分层级）。 */
    public List<BizGroup> listAll() { return mapper.listAll(); }

    /** 查询归属指定领域的分组（供各资源"所属分组"下拉按领域按需加载）。 */
    public List<BizGroup> listByDomain(String domain) { return mapper.listByDomain(domain); }

    /**
     * 创建分组。ID 为空时自动生成（格式 "group-" + UUID），gSort 为空时默认 0。
     */
    public BizGroup create(BizGroup g) {
        if (g.getId() == null) g.setId("group-" + UUID.randomUUID());
        if (g.getGSort() == null) g.setGSort(0);
        mapper.insert(g);
        return g;
    }

    /** 更新分组基本信息，返回更新后的对象。 */
    public BizGroup update(BizGroup g) {
        mapper.update(g);
        return g;
    }

    /** 删除指定分组（不级联删除成员，调用方自行保证）。 */
    public void delete(String id) { mapper.delete(id); }

    /** 查询指定分组下关联的对象类列表。 */
    public List<Map<String, Object>> listClasses(String groupId) { return mapper.listGroupClasses(groupId); }

    /**
     * 分组成员管理 —— 基于分类节点（categoryType=3）的 category_code 操作
     * 兼容现有 ont_biz_group 数据：若已有匹配 category_code 的 group 则复用其 group_id，
     * 否则用 "auto-" 前缀生成 group_id（不影响业务，主要让 NOT NULL 字段有值）
     */

    /**
     * 向分类分组节点添加成员（对象类）。
     * - 仅 categoryType=3 的分类节点允许管理成员
     * - 成员已存在时幂等跳过；groupId 不存在时以 "auto-{categoryCode}" 填充，保证 NOT NULL
     * - 成员 ID 格式："gc-" + UUID，sort 取当前最大值 +1
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
        // 无关联分组时用 "auto-" 前缀保证字段非空，不影响业务查询
        if (groupId == null || groupId.isEmpty()) groupId = "auto-" + code;
        int sort = mapper.maxSortByCategoryCode(code) + 1;
        mapper.insertMember("gc-" + UUID.randomUUID(), groupId, classId, code, sort);
    }

    /**
     * 从分类分组节点移除指定对象类成员。
     */
    public void removeMember(String categoryId, String classId) {
        BizCategory cat = categoryMapper.findById(categoryId);
        if (cat == null) throw new IllegalArgumentException("分类不存在");
        mapper.deleteMember(cat.getCategoryCode(), classId);
    }

    /**
     * 按传入的 classIds 顺序重排成员 sort 值（从 1 开始递增）。
     */
    public void reorderMembers(String categoryId, List<String> classIds) {
        BizCategory cat = categoryMapper.findById(categoryId);
        if (cat == null) throw new IllegalArgumentException("分类不存在");
        int sort = 1;
        for (String classId : classIds) {
            mapper.updateMemberSort(cat.getCategoryCode(), classId, sort++);
        }
    }
}
