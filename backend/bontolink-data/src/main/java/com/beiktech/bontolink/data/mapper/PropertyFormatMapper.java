package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 属性格式化 mapper (ont_property_format).
 * src_type: 1=属性, 2=共享属性
 */
@Mapper
public interface PropertyFormatMapper {

    // 按属性 id 查格式化配置
    @Select("SELECT * FROM ont_property_format WHERE property_id = #{propertyId}")
    Map<String, Object> findByProperty(@Param("propertyId") String propertyId);

    // 批量按属性 id 列表查格式化配置（用于列表页一次性加载多属性的格式）
    @Select("""
        <script>
        SELECT * FROM ont_property_format
        WHERE property_id IN
        <foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach>
        </script>
    """)
    List<Map<String, Object>> findByProperties(@Param("ids") java.util.Collection<String> ids);

    // 新增属性格式化配置（包含数值/日期/文本/百分比等全量格式字段）
    @Insert("""
        INSERT INTO ont_property_format(
            format_id, src_type, property_id, property_scope, format_enabled, format_type,
            decimal_places, use_thousand_sep, negative_mode, currency_symbol, accounting_align,
            date_pattern, time_pattern, locale, fraction_type, special_type, custom_format,
            text_force, text_max_length, text_regex, percent_auto_multiply, create_user)
        VALUES (#{format_id}, #{src_type}, #{property_id}, #{property_scope}, #{format_enabled}, #{format_type},
            #{decimal_places}, #{use_thousand_sep}, #{negative_mode}, #{currency_symbol}, #{accounting_align},
            #{date_pattern}, #{time_pattern}, #{locale}, #{fraction_type}, #{special_type}, #{custom_format},
            #{text_force}, #{text_max_length}, #{text_regex}, #{percent_auto_multiply}, #{create_user})
    """)
    int insert(Map<String, Object> row);

    // 按属性 id 更新格式化配置（format_id/property_id 不变）
    @Update("""
        UPDATE ont_property_format SET
            src_type = #{src_type},
            property_scope = #{property_scope}, format_enabled = #{format_enabled}, format_type = #{format_type},
            decimal_places = #{decimal_places}, use_thousand_sep = #{use_thousand_sep},
            negative_mode = #{negative_mode}, currency_symbol = #{currency_symbol}, accounting_align = #{accounting_align},
            date_pattern = #{date_pattern}, time_pattern = #{time_pattern}, locale = #{locale},
            fraction_type = #{fraction_type}, special_type = #{special_type}, custom_format = #{custom_format},
            text_force = #{text_force}, text_max_length = #{text_max_length}, text_regex = #{text_regex},
            percent_auto_multiply = #{percent_auto_multiply},
            update_time = CURRENT_TIMESTAMP
        WHERE property_id = #{property_id}
    """)
    int updateByProperty(Map<String, Object> row);

    // 删除指定属性的格式化配置
    @Delete("DELETE FROM ont_property_format WHERE property_id = #{propertyId}")
    int deleteByProperty(@Param("propertyId") String propertyId);
}
