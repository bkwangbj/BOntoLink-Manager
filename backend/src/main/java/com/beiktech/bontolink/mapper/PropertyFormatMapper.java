package com.beiktech.bontolink.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface PropertyFormatMapper {

    @Select("SELECT * FROM t_ont_property_format WHERE property_id = #{propertyId}")
    Map<String, Object> findByProperty(@Param("propertyId") String propertyId);

    @Select("""
        <script>
        SELECT * FROM t_ont_property_format
        WHERE property_id IN
        <foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach>
        </script>
    """)
    List<Map<String, Object>> findByProperties(@Param("ids") java.util.Collection<String> ids);

    @Insert("""
        INSERT INTO t_ont_property_format(
            format_id, property_id, property_scope, format_enabled, format_type,
            decimal_places, use_thousand_sep, negative_mode, currency_symbol, accounting_align,
            date_pattern, time_pattern, locale, fraction_type, special_type, custom_format,
            text_force, text_max_length, text_regex, percent_auto_multiply, create_user)
        VALUES (#{format_id}, #{property_id}, #{property_scope}, #{format_enabled}, #{format_type},
            #{decimal_places}, #{use_thousand_sep}, #{negative_mode}, #{currency_symbol}, #{accounting_align},
            #{date_pattern}, #{time_pattern}, #{locale}, #{fraction_type}, #{special_type}, #{custom_format},
            #{text_force}, #{text_max_length}, #{text_regex}, #{percent_auto_multiply}, #{create_user})
    """)
    int insert(Map<String, Object> row);

    @Update("""
        UPDATE t_ont_property_format SET
            property_scope = #{property_scope}, format_enabled = #{format_enabled}, format_type = #{format_type},
            decimal_places = #{decimal_places}, use_thousand_sep = #{use_thousand_sep},
            negative_mode = #{negative_mode}, currency_symbol = #{currency_symbol}, accounting_align = #{accounting_align},
            date_pattern = #{date_pattern}, time_pattern = #{time_pattern}, locale = #{locale},
            fraction_type = #{fraction_type}, special_type = #{special_type}, custom_format = #{custom_format},
            text_force = #{text_force}, text_max_length = #{text_max_length}, text_regex = #{text_regex},
            percent_auto_multiply = #{percent_auto_multiply},
            update_time = datetime('now','localtime')
        WHERE property_id = #{property_id}
    """)
    int updateByProperty(Map<String, Object> row);

    @Delete("DELETE FROM t_ont_property_format WHERE property_id = #{propertyId}")
    int deleteByProperty(@Param("propertyId") String propertyId);
}
