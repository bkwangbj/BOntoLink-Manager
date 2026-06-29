package com.beiktech.bontolink.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/** 类型类绑定实例 Mapper(ont_type_class_bind)。 */
@Mapper
public interface TypeClassBindMapper {

    /** 某类型类的全部绑定明细 */
    @Select("SELECT * FROM ont_type_class_bind WHERE type_class_meta_id = #{metaId} ORDER BY created_at DESC")
    List<Map<String, Object>> listByMeta(@Param("metaId") String metaId);

    /** 某类型类的绑定统计(总/属性/关系/操作) */
    @Select("""
        SELECT
          COUNT(*) AS total,
          SUM(CASE WHEN applicable_type = 'property' THEN 1 ELSE 0 END) AS property_count,
          SUM(CASE WHEN applicable_type = 'relation' THEN 1 ELSE 0 END) AS relation_count,
          SUM(CASE WHEN applicable_type = 'action'   THEN 1 ELSE 0 END) AS action_count
        FROM ont_type_class_bind WHERE type_class_meta_id = #{metaId}
    """)
    Map<String, Object> statsByMeta(@Param("metaId") String metaId);

    @Select("SELECT * FROM ont_type_class_bind WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") String id);

    @Insert("""
        INSERT INTO ont_type_class_bind(id, env, type_class_meta_id, applicable_type,
            property_owner_type, property_owner_id, property_id, link_type_id, action_type_id,
            suffix_custom, value, bind_deprecated, remark, create_user, update_user)
        VALUES (#{id}, #{env}, #{type_class_meta_id}, #{applicable_type},
            #{property_owner_type}, #{property_owner_id}, #{property_id}, #{link_type_id}, #{action_type_id},
            #{suffix_custom}, #{value}, #{bind_deprecated}, #{remark}, #{create_user}, #{update_user})
    """)
    int insert(Map<String, Object> row);

    @Delete("DELETE FROM ont_type_class_bind WHERE id = #{id}")
    int delete(@Param("id") String id);

    @Delete("DELETE FROM ont_type_class_bind WHERE type_class_meta_id = #{metaId}")
    int deleteByMeta(@Param("metaId") String metaId);
}
