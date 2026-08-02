package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 外部数据源(HTTP 接口类)。与 sys_data_source(数据库类) 并列，两者在数据源列表页合并展示。
 */
@Mapper
public interface ExtDataSourceMapper {

    /** 列表：附带该数据源下的启用接口数，列表页「接口数」列用 */
    @Select("""
        SELECT d.*, (SELECT COUNT(1) FROM ont_ext_api_interface i
                      WHERE i.ds_id = d.id AND i.status = 1) AS api_count
          FROM ont_ext_data_source d
         ORDER BY d.update_time DESC
    """)
    List<Map<String, Object>> listAll();

    @Select("""
        SELECT d.*, (SELECT COUNT(1) FROM ont_ext_api_interface i
                      WHERE i.ds_id = d.id AND i.status = 1) AS api_count
          FROM ont_ext_data_source d WHERE d.id = #{id}
    """)
    Map<String, Object> findById(@Param("id") String id);

    /** 同领域内编码唯一性校验 */
    @Select("SELECT COUNT(1) FROM ont_ext_data_source WHERE ds_code = #{dsCode} AND id <> #{excludeId}")
    int countByCode(@Param("dsCode") String dsCode, @Param("excludeId") String excludeId);

    @Insert("""
        INSERT INTO ont_ext_data_source
          (id, category_code, ds_code, ds_name, ds_type, read_write_type, base_url, default_method, content_type,
           connect_timeout, read_timeout, retry_count, retry_interval, ssl_verify, log_enable,
           header_enable, global_header, auth_type, auth_config, status, remark)
        VALUES
          (#{id}, #{category_code}, #{ds_code}, #{ds_name}, #{ds_type}, #{read_write_type}, #{base_url},
           #{default_method}, #{content_type}, #{connect_timeout}, #{read_timeout}, #{retry_count}, #{retry_interval},
           #{ssl_verify}, #{log_enable}, #{header_enable}, #{global_header}, #{auth_type}, #{auth_config},
           #{status}, #{remark})
    """)
    void insert(Map<String, Object> row);

    @Update("""
        UPDATE ont_ext_data_source SET
          category_code = #{category_code}, ds_code = #{ds_code}, ds_name = #{ds_name}, ds_type = #{ds_type},
          read_write_type = #{read_write_type}, base_url = #{base_url}, default_method = #{default_method},
          content_type = #{content_type}, connect_timeout = #{connect_timeout}, read_timeout = #{read_timeout},
          retry_count = #{retry_count}, retry_interval = #{retry_interval}, ssl_verify = #{ssl_verify},
          log_enable = #{log_enable}, header_enable = #{header_enable}, global_header = #{global_header},
          auth_type = #{auth_type}, auth_config = #{auth_config}, status = #{status}, remark = #{remark},
          update_time = datetime('now','localtime')
        WHERE id = #{id}
    """)
    void update(Map<String, Object> row);

    @Delete("DELETE FROM ont_ext_data_source WHERE id = #{id}")
    void delete(@Param("id") String id);

    @Delete("DELETE FROM ont_ext_api_interface WHERE ds_id = #{dsId}")
    void deleteInterfacesByDs(@Param("dsId") String dsId);

    @Delete("DELETE FROM ont_ext_api_group WHERE ds_id = #{dsId}")
    void deleteGroupsByDs(@Param("dsId") String dsId);

    /* —— 接口分组 / 接口定义, 供阶段二的接口管理页使用 —— */

    @Select("SELECT * FROM ont_ext_api_group WHERE ds_id = #{dsId} ORDER BY sort, create_time")
    List<Map<String, Object>> listGroups(@Param("dsId") String dsId);

    @Select("SELECT * FROM ont_ext_api_interface WHERE ds_id = #{dsId} ORDER BY sort, create_time")
    List<Map<String, Object>> listInterfaces(@Param("dsId") String dsId);
}
