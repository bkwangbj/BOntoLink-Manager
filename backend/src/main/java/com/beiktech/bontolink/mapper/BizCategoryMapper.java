package com.beiktech.bontolink.mapper;

import com.beiktech.bontolink.entity.BizCategory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BizCategoryMapper {
    @Select("SELECT * FROM ont_biz_category ORDER BY sort, create_time")
    List<BizCategory> listAll();

    @Select("SELECT * FROM ont_biz_category WHERE id = #{id}")
    BizCategory findById(@Param("id") String id);

    @Select("SELECT * FROM ont_biz_category WHERE category_code = #{code}")
    BizCategory findByCode(@Param("code") String code);

    @Select("SELECT COUNT(1) FROM ont_biz_category WHERE parent_id = #{parentId}")
    int countByParent(@Param("parentId") String parentId);

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

    @Update("""
        UPDATE ont_biz_category SET
            parent_id=#{parentId}, category_code=#{categoryCode}, category_type=#{categoryType},
            ns_code=#{nsCode}, status=#{status}, sort=#{sort}, icon=#{icon}, color=#{color},
            rdfs_label=#{rdfsLabel}, rdfs_comment=#{rdfsComment}, rdfs_see_also=#{rdfsSeeAlso},
            rdfs_defined_by=#{rdfsDefinedBy}, description=#{description}, metadata=#{metadata}
        WHERE id=#{id}
    """)
    int update(BizCategory c);

    @Delete("DELETE FROM ont_biz_category WHERE id = #{id}")
    int delete(@Param("id") String id);
}
