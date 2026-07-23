package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/** 类型类统一枚举字典 Mapper(ont_dic_type_class)。 */
@Mapper
public interface TcEnumMapper {

    @Select("SELECT DISTINCT enum_name FROM ont_dic_type_class ORDER BY enum_name")
    List<String> listEnumNames();

    /** 某枚举的可选项(默认仅启用,按排序) */
    @Select("SELECT * FROM ont_dic_type_class WHERE enum_name = #{enumName} ORDER BY sort_no, code")
    List<Map<String, Object>> listByName(@Param("enumName") String enumName);

    @Select("SELECT * FROM ont_dic_type_class ORDER BY enum_name, sort_no, code")
    List<Map<String, Object>> listAll();

    @Select("SELECT * FROM ont_dic_type_class WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") String id);

    @Insert("INSERT INTO ont_dic_type_class(id, enum_name, code, name, sort_no, status) VALUES (#{id}, #{enum_name}, #{code}, #{name}, #{sort_no}, #{status})")
    int insert(Map<String, Object> row);

    @Update("UPDATE ont_dic_type_class SET code = #{code}, name = #{name}, sort_no = #{sort_no}, status = #{status}, updated_at = #{updated_at} WHERE id = #{id}")
    int update(Map<String, Object> row);

    @Delete("DELETE FROM ont_dic_type_class WHERE id = #{id}")
    int delete(@Param("id") String id);
}
