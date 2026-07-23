package com.beiktech.bontolink.data.mapper;

import com.beiktech.bontolink.data.entity.BizGroup;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 业务分组 (Biz Group) Mapper
 * <p>
 * 操作 ont_biz_group（分组树）和 ont_biz_group_class（分组与资源的关联）两张表。
 * 支持对象类型、链接类型、共享属性等多种资源类型的统一分组管理。
 */
@Mapper
public interface BizGroupMapper {

    /** 查询某节点的直接子分组（按 g_sort 排序） */
    @Select("SELECT * FROM ont_biz_group WHERE parent_id = #{parentId} ORDER BY g_sort")
    List<BizGroup> listByParent(@Param("parentId") String parentId);

    /** 查询所有分组，按父节点 + 排序号排列（COALESCE 把 null parent_id 排前面） */
    @Select("SELECT * FROM ont_biz_group ORDER BY COALESCE(parent_id,''), g_sort")
    List<BizGroup> listAll();

    /** 查询归属指定领域的分组（domain_code 或 category_code 命中当前领域），供各资源"所属分组"下拉按需加载 */
    @Select("SELECT * FROM ont_biz_group WHERE domain_code = #{domain} OR category_code = #{domain} ORDER BY COALESCE(parent_id,''), g_sort")
    List<BizGroup> listByDomain(@Param("domain") String domain);

    /** 新增分组节点 */
    @Insert("""
        INSERT INTO ont_biz_group(id, parent_id, category_code, domain_code, g_name, g_sort, icon, color, description)
        VALUES(#{id}, #{parentId}, #{categoryCode}, #{domainCode}, #{gName}, #{gSort}, #{icon}, #{color}, #{description})
    """)
    int insert(BizGroup g);

    /** 更新分组基本信息（不修改 parent_id / category_code） */
    @Update("""
        UPDATE ont_biz_group SET
            g_name=#{gName}, g_sort=#{gSort}, icon=#{icon}, color=#{color}, description=#{description}, domain_code=#{domainCode}
        WHERE id=#{id}
    """)
    int update(BizGroup g);

    /** 删除分组节点 */
    @Delete("DELETE FROM ont_biz_group WHERE id = #{id}")
    int delete(@Param("id") String id);

    /** 查询某分组下关联的对象类列表（JOIN ont_class 取完整对象类信息，按 g_sort 排序） */
    @Select("""
        SELECT c.* FROM ont_biz_group_class gc
        JOIN ont_class c ON c.id = gc.ref_id
        WHERE gc.group_id = #{groupId} AND gc.group_type = 'object_types'
        ORDER BY gc.g_sort
    """)
    List<Map<String, Object>> listGroupClasses(@Param("groupId") String groupId);

    /** 按行业分类编码查询对应的第一个分组 id */
    @Select("SELECT id FROM ont_biz_group WHERE category_code = #{code} LIMIT 1")
    String findGroupIdByCategoryCode(@Param("code") String code);

    /** 查询某分类下对象类关联的最大 g_sort（用于追加时确定排序号，COALESCE 兜底 0） */
    @Select("SELECT COALESCE(MAX(g_sort), 0) FROM ont_biz_group_class WHERE group_type = 'object_types' AND category_code = #{code}")
    int maxSortByCategoryCode(@Param("code") String code);

    /** 按行业分类编码查询已关联的对象类 id 列表（按排序号） */
    @Select("SELECT ref_id FROM ont_biz_group_class WHERE group_type = 'object_types' AND category_code = #{code} ORDER BY g_sort")
    java.util.List<String> listClassIdsByCategoryCode(@Param("code") String code);

    /** 检查某对象类是否已在分类分组中（防重复添加） */
    @Select("SELECT COUNT(*) FROM ont_biz_group_class WHERE group_type = 'object_types' AND category_code = #{code} AND ref_id = #{classId}")
    int existsMember(@Param("code") String code, @Param("classId") String classId);

    /** 新增对象类到分组关联（固定 group_type = 'object_types'） */
    @Insert("""
        INSERT INTO ont_biz_group_class(id, group_id, ref_id, group_type, category_code, g_sort)
        VALUES(#{id}, #{groupId}, #{classId}, 'object_types', #{categoryCode}, #{gSort})
    """)
    int insertMember(@Param("id") String id,
                     @Param("groupId") String groupId,
                     @Param("classId") String classId,
                     @Param("categoryCode") String categoryCode,
                     @Param("gSort") int gSort);

    /** 从分组中移除某对象类关联 */
    @Delete("DELETE FROM ont_biz_group_class WHERE group_type = 'object_types' AND category_code = #{code} AND ref_id = #{classId}")
    int deleteMember(@Param("code") String code, @Param("classId") String classId);

    /** 更新对象类在分组中的排序号 */
    @Update("UPDATE ont_biz_group_class SET g_sort = #{sort} WHERE group_type = 'object_types' AND category_code = #{code} AND ref_id = #{classId}")
    int updateMemberSort(@Param("code") String code, @Param("classId") String classId, @Param("sort") int sort);

    /* ============================================================
     *  统一分组关联表 API (ref_id + group_type)
     *  group_type: object_types / link_types / action_types / value_types
     *              / shared_props / functions / interface / datasources / enum_types
     * ============================================================ */

    /** 查询某资源类型下的所有分组关联记录（按 group_id + g_sort 排序） */
    @Select("""
        SELECT id, group_id, ref_id, group_type, category_code, g_sort, create_time, update_time
          FROM ont_biz_group_class
         WHERE group_type = #{type}
         ORDER BY group_id, g_sort
    """)
    List<Map<String, Object>> listRefsByType(@Param("type") String type);

    /** 查询某分组内指定资源类型的关联列表 */
    @Select("""
        SELECT id, group_id, ref_id, group_type, category_code, g_sort
          FROM ont_biz_group_class
         WHERE group_id = #{groupId} AND group_type = #{type}
         ORDER BY g_sort
    """)
    List<Map<String, Object>> listRefsByGroupAndType(@Param("groupId") String groupId, @Param("type") String type);

    /** 新增通用分组关联（支持多种 group_type） */
    @Insert("""
        INSERT INTO ont_biz_group_class(id, group_id, ref_id, group_type, category_code, g_sort)
        VALUES(#{id}, #{group_id}, #{ref_id}, #{group_type}, #{category_code}, #{g_sort})
    """)
    int insertRef(Map<String, Object> row);

    /** 更新分组关联的归属分组、分类和排序（同步更新 update_time） */
    @Update("""
        UPDATE ont_biz_group_class
           SET group_id = #{group_id}, category_code = #{category_code}, g_sort = #{g_sort},
               update_time = CURRENT_TIMESTAMP
         WHERE id = #{id}
    """)
    int updateRef(Map<String, Object> row);

    /** 按关联记录 id 删除 */
    @Delete("DELETE FROM ont_biz_group_class WHERE id = #{id}")
    int deleteRefById(@Param("id") String id);

    /** 删除某资源在所有分组中的关联（资源删除时级联清理） */
    @Delete("DELETE FROM ont_biz_group_class WHERE ref_id = #{refId} AND group_type = #{type}")
    int deleteRefsByRefId(@Param("refId") String refId, @Param("type") String type);

    /** 删除某分组下的所有资源关联（分组删除时级联清理） */
    @Delete("DELETE FROM ont_biz_group_class WHERE group_id = #{groupId}")
    int deleteRefsByGroupId(@Param("groupId") String groupId);
}
