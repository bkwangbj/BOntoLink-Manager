package com.beiktech.bontolink.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface PhysicalTableMapper {

    @Select("SELECT * FROM ont_physical_table WHERE ds_id = #{dsId} ORDER BY table_type, physical_table")
    List<Map<String, Object>> listByDs(@Param("dsId") String dsId);

    @Select("SELECT * FROM ont_physical_table ORDER BY ds_id, table_type, physical_table")
    List<Map<String, Object>> listAll();

    @Select("SELECT * FROM ont_physical_table WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") String id);

    @Insert("""
        INSERT INTO ont_physical_table(
            id, ds_id, physical_table, display_name, table_type, columns_json, column_count, sync_time
        ) VALUES (
            #{id}, #{dsId}, #{physicalTable}, #{displayName}, #{tableType}, #{columnsJson}, #{columnCount}, #{syncTime}
        )
    """)
    int insert(Map<String, Object> row);

    /** 同步: 只更新结构相关字段, 不动 display_name(中文名) */
    @Update("""
        UPDATE ont_physical_table SET
            table_type = #{tableType}, columns_json = #{columnsJson}, column_count = #{columnCount},
            sync_time = #{syncTime}, update_time = #{updateTime}
        WHERE id = #{id}
    """)
    int updateStructure(Map<String, Object> row);

    /** 改中文名 */
    @Update("UPDATE ont_physical_table SET display_name = #{displayName}, update_time = #{updateTime} WHERE id = #{id}")
    int updateDisplayName(@Param("id") String id, @Param("displayName") String displayName,
                          @Param("updateTime") String updateTime);

    @Delete("DELETE FROM ont_physical_table WHERE id = #{id}")
    int deleteById(@Param("id") String id);

    @Delete("DELETE FROM ont_physical_table WHERE ds_id = #{dsId}")
    int deleteByDs(@Param("dsId") String dsId);
}
