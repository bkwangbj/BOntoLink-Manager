package com.beiktech.bontolink.mapper;

import com.beiktech.bontolink.entity.BizCategory;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 行业分类 mapper，覆盖 ont_biz_category 表。
 * 分类为树形结构，通过 parent_id 自关联，供各资源模块的行业分类筛选树使用。
 */
@Mapper
public interface BizCategoryMapper {
    // 查询所有分类节点，按排序号和创建时间排列（调用方自行组装树）
    @Select("SELECT * FROM ont_biz_category ORDER BY sort, create_time")
    List<BizCategory> listAll();

    // 按 id 查单条分类
    @Select("SELECT * FROM ont_biz_category WHERE id = #{id}")
    BizCategory findById(@Param("id") String id);

    // 按分类编码查单条
    @Select("SELECT * FROM ont_biz_category WHERE category_code = #{code}")
    BizCategory findByCode(@Param("code") String code);

    // 统计指定父节点下的直接子节点数量（用于判断是否可以删除）
    @Select("SELECT COUNT(1) FROM ont_biz_category WHERE parent_id = #{parentId}")
    int countByParent(@Param("parentId") String parentId);

    // 统计指定父节点下 type=2 领域子节点数量（限制每个领域只能有 1 个子领域）
    @Select("SELECT COUNT(1) FROM ont_biz_category WHERE parent_id = #{parentId} AND category_type = 2")
    int countDomainChildren(@Param("parentId") String parentId);

    // 新增行业分类节点
    @Insert("""
        INSERT INTO ont_biz_category(
            id, parent_id, rid, category_code, category_type, ns_code, status, sort,
            icon, color, rdfs_label, rdfs_comment, rdfs_see_also, rdfs_defined_by,
            description, metadata
        ) VALUES (
            #{id}, #{parentId}, #{rid}, #{categoryCode}, #{categoryType}, #{nsCode}, #{status}, #{sort},
            #{icon}, #{color}, #{rdfsLabel}, #{rdfsComment}, #{rdfsSeeAlso}, #{rdfsDefinedBy},
            #{description}, #{metadata}
        )
    """)
    int insert(BizCategory c);

    // 更新行业分类（编码一般不可改，id 不变）
    @Update("""
        UPDATE ont_biz_category SET
            parent_id=#{parentId}, category_code=#{categoryCode}, category_type=#{categoryType},
            ns_code=#{nsCode}, status=#{status}, sort=#{sort}, icon=#{icon}, color=#{color},
            rdfs_label=#{rdfsLabel}, rdfs_comment=#{rdfsComment}, rdfs_see_also=#{rdfsSeeAlso},
            rdfs_defined_by=#{rdfsDefinedBy}, description=#{description}, metadata=#{metadata}
        WHERE id=#{id}
    """)
    int update(BizCategory c);

    // 删除行业分类节点（调用方须先确认无子节点）
    @Delete("DELETE FROM ont_biz_category WHERE id = #{id}")
    int delete(@Param("id") String id);
}
