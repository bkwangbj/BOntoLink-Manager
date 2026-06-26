package com.beiktech.bontolink.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 类型类 (Type Classes) Mapper
 * <p>
 * 三类: property / relation / action,通过 applicable_type 区分。
 * 当 link_type_id / object_type_id / action_type_id 三者其一非空时,
 * 表示挂到具体对象上;均为空时表示目录预置项。
 */
@Mapper
public interface TypeClassMapper {

    /**
     * 动态条件查询类型类列表。
     * catalogOnly=true 时筛选三个外键均为 null 的目录预置项（即未挂到具体对象的公共类型类）。
     */
    @Select("""
        <script>
        SELECT * FROM ont_type_class
        <where>
          <if test='applicableType != null and applicableType != ""'> AND applicable_type = #{applicableType} </if>
          <if test='category != null and category != ""'> AND category = #{category} </if>
          <if test='isDeprecated != null'> AND is_deprecated = #{isDeprecated} </if>
          <if test='catalogOnly != null and catalogOnly == true'>
            AND link_type_id IS NULL AND object_type_id IS NULL AND action_type_id IS NULL
          </if>
        </where>
        ORDER BY is_deprecated, applicable_type, category, name
        </script>
    """)
    List<Map<String, Object>> list(@Param("applicableType") String applicableType,
                                    @Param("category") String category,
                                    @Param("isDeprecated") Integer isDeprecated,
                                    @Param("catalogOnly") Boolean catalogOnly);

    /** 按 id 查单条类型类 */
    @Select("SELECT * FROM ont_type_class WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") String id);

    /** 按种类聚合 (用于左侧分组树: category → 包含数量) */
    @Select("""
        SELECT applicable_type, category, category_cn,
               SUM(CASE WHEN is_deprecated = 0 THEN 1 ELSE 0 END) AS active_count,
               SUM(CASE WHEN is_deprecated = 1 THEN 1 ELSE 0 END) AS deprecated_count,
               COUNT(1) AS total_count
        FROM ont_type_class
        GROUP BY applicable_type, category
        ORDER BY applicable_type, category
    """)
    List<Map<String, Object>> categoryStats();

    /** 新增类型类 */
    @Insert("""
        INSERT INTO ont_type_class(id, link_type_id, object_type_id, action_type_id,
            applicable_type, is_deprecated, category, category_cn, name, name_cn,
            value, description)
        VALUES (#{id}, #{link_type_id}, #{object_type_id}, #{action_type_id},
            #{applicable_type}, #{is_deprecated}, #{category}, #{category_cn}, #{name}, #{name_cn},
            #{value}, #{description})
    """)
    int insert(Map<String, Object> row);

    /** 更新类型类（同步 updated_at） */
    @Update("""
        UPDATE ont_type_class SET
          link_type_id = #{link_type_id}, object_type_id = #{object_type_id}, action_type_id = #{action_type_id},
          applicable_type = #{applicable_type}, is_deprecated = #{is_deprecated},
          category = #{category}, category_cn = #{category_cn},
          name = #{name}, name_cn = #{name_cn},
          value = #{value}, description = #{description},
          updated_at = datetime('now','localtime')
        WHERE id = #{id}
    """)
    int update(Map<String, Object> row);

    /** 删除类型类 */
    @Delete("DELETE FROM ont_type_class WHERE id = #{id}")
    int delete(@Param("id") String id);

    /** 切换弃用 */
    @Update("UPDATE ont_type_class SET is_deprecated = #{deprecated}, updated_at = datetime('now','localtime') WHERE id = #{id}")
    int setDeprecated(@Param("id") String id, @Param("deprecated") Integer deprecated);
}
