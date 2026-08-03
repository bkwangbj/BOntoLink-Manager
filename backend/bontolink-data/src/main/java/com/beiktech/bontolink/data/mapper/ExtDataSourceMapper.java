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

    @Insert("INSERT INTO ont_ext_api_group (id, ds_id, group_name, parent_id, sort) " +
            "VALUES (#{id}, #{ds_id}, #{group_name}, #{parent_id}, #{sort})")
    void insertGroup(Map<String, Object> row);

    @Update("UPDATE ont_ext_api_group SET group_name = #{group_name}, parent_id = #{parent_id}, sort = #{sort} WHERE id = #{id}")
    void updateGroup(Map<String, Object> row);

    @Delete("DELETE FROM ont_ext_api_group WHERE id = #{id}")
    void deleteGroup(@Param("id") String id);

    /** 删组时把组内接口挪到未分组, 不连带删接口 */
    @Update("UPDATE ont_ext_api_interface SET group_id = '0' WHERE group_id = #{groupId}")
    void detachInterfacesFromGroup(@Param("groupId") String groupId);

    @Select("SELECT * FROM ont_ext_api_interface WHERE ds_id = #{dsId} ORDER BY sort, create_time")
    List<Map<String, Object>> listInterfaces(@Param("dsId") String dsId);

    @Select("SELECT * FROM ont_ext_api_interface WHERE id = #{id}")
    Map<String, Object> findInterface(@Param("id") String id);

    @Select("SELECT COUNT(1) FROM ont_ext_api_interface WHERE ds_id = #{dsId} AND api_code = #{apiCode} AND id <> #{excludeId}")
    int countInterfaceByCode(@Param("dsId") String dsId, @Param("apiCode") String apiCode, @Param("excludeId") String excludeId);

    @Insert("""
        INSERT INTO ont_ext_api_interface
          (id, ds_id, group_id, api_code, api_name, method, api_path, api_status, read_write_type, description,
           request_params, response_params, override_auth, auth_type, auth_config, header_inherit, content_type,
           timeout, status, sort)
        VALUES
          (#{id}, #{ds_id}, #{group_id}, #{api_code}, #{api_name}, #{method}, #{api_path}, #{api_status},
           #{read_write_type}, #{description}, #{request_params}, #{response_params}, #{override_auth},
           #{auth_type}, #{auth_config}, #{header_inherit}, #{content_type}, #{timeout}, #{status}, #{sort})
    """)
    void insertInterface(Map<String, Object> row);

    @Update("""
        UPDATE ont_ext_api_interface SET
          group_id = #{group_id}, api_code = #{api_code}, api_name = #{api_name}, method = #{method},
          api_path = #{api_path}, api_status = #{api_status}, read_write_type = #{read_write_type},
          description = #{description}, request_params = #{request_params}, response_params = #{response_params},
          override_auth = #{override_auth}, auth_type = #{auth_type}, auth_config = #{auth_config},
          header_inherit = #{header_inherit}, content_type = #{content_type}, timeout = #{timeout},
          status = #{status}, sort = #{sort}, update_time = datetime('now','localtime')
        WHERE id = #{id}
    """)
    void updateInterface(Map<String, Object> row);

    @Delete("DELETE FROM ont_ext_api_interface WHERE id = #{id}")
    void deleteInterface(@Param("id") String id);

    /* —— 调用日志 —— */

    @Insert("""
        INSERT INTO ont_ext_api_call_log
          (id, trace_id, ds_id, interface_id, call_type, caller, full_url, request_header, request_body,
           call_status, http_status, cost_time, response_size, response_body, error_msg)
        VALUES
          (#{id}, #{trace_id}, #{ds_id}, #{interface_id}, #{call_type}, #{caller}, #{full_url},
           #{request_header}, #{request_body}, #{call_status}, #{http_status}, #{cost_time},
           #{response_size}, #{response_body}, #{error_msg})
    """)
    void insertLog(Map<String, Object> row);

    /** 日志分页查询: interfaceId/callStatus/kw 为空则不参与过滤 */
    @Select("""
        <script>
        SELECT l.*, i.api_name FROM ont_ext_api_call_log l
          LEFT JOIN ont_ext_api_interface i ON i.id = l.interface_id
         WHERE l.ds_id = #{dsId}
           <if test="from != null and from != ''"> AND l.call_time &gt;= #{from} </if>
           <if test="to != null and to != ''"> AND l.call_time &lt;= #{to} </if>
           <if test="interfaceId != null and interfaceId != ''"> AND l.interface_id = #{interfaceId} </if>
           <if test="callStatus != null"> AND l.call_status = #{callStatus} </if>
           <if test="kw != null and kw != ''">
             AND (l.caller LIKE '%' || #{kw} || '%' OR l.trace_id LIKE '%' || #{kw} || '%' OR l.full_url LIKE '%' || #{kw} || '%')
           </if>
         ORDER BY l.call_time DESC
         LIMIT #{size} OFFSET #{offset}
        </script>
    """)
    List<Map<String, Object>> pageLogs(Map<String, Object> q);

    @Select("""
        <script>
        SELECT COUNT(1) FROM ont_ext_api_call_log l
         WHERE l.ds_id = #{dsId}
           <if test="from != null and from != ''"> AND l.call_time &gt;= #{from} </if>
           <if test="to != null and to != ''"> AND l.call_time &lt;= #{to} </if>
           <if test="interfaceId != null and interfaceId != ''"> AND l.interface_id = #{interfaceId} </if>
           <if test="callStatus != null"> AND l.call_status = #{callStatus} </if>
           <if test="kw != null and kw != ''">
             AND (l.caller LIKE '%' || #{kw} || '%' OR l.trace_id LIKE '%' || #{kw} || '%' OR l.full_url LIKE '%' || #{kw} || '%')
           </if>
        </script>
    """)
    int countLogs(Map<String, Object> q);

    @Select("SELECT l.*, i.api_name FROM ont_ext_api_call_log l " +
            "LEFT JOIN ont_ext_api_interface i ON i.id = l.interface_id WHERE l.id = #{id}")
    Map<String, Object> findLog(@Param("id") String id);

    /* —— 监控聚合: 均以调用发起时间为准, 仅统计启用接口 —— */

    @Select("""
        SELECT COUNT(1) AS total,
               SUM(CASE WHEN call_status = 1 THEN 1 ELSE 0 END) AS success,
               SUM(CASE WHEN call_status <> 1 THEN 1 ELSE 0 END) AS failed,
               AVG(CASE WHEN call_status = 1 THEN cost_time END) AS avg_cost
          FROM ont_ext_api_call_log
         WHERE ds_id = #{dsId} AND call_time >= #{from}
    """)
    Map<String, Object> statSummary(@Param("dsId") String dsId, @Param("from") String from);

    /** 按天聚合的调用趋势 */
    @Select("""
        SELECT substr(call_time, 1, 10) AS day, COUNT(1) AS total,
               SUM(CASE WHEN call_status = 1 THEN 1 ELSE 0 END) AS success,
               AVG(CASE WHEN call_status = 1 THEN cost_time END) AS avg_cost
          FROM ont_ext_api_call_log
         WHERE ds_id = #{dsId} AND call_time >= #{from}
         GROUP BY substr(call_time, 1, 10) ORDER BY day
    """)
    List<Map<String, Object>> statTrend(@Param("dsId") String dsId, @Param("from") String from);

    /** 错误类型分布: 按 call_status + http 段归类 */
    @Select("""
        SELECT CASE
                 WHEN call_status = 3 THEN 'timeout'
                 WHEN call_status = 4 THEN 'auth'
                 WHEN http_status >= 400 THEN 'server'
                 WHEN http_status = 0 THEN 'network'
                 ELSE 'other' END AS kind,
               COUNT(1) AS cnt
          FROM ont_ext_api_call_log
         WHERE ds_id = #{dsId} AND call_time >= #{from} AND call_status <> 1
         GROUP BY kind ORDER BY cnt DESC
    """)
    List<Map<String, Object>> statErrors(@Param("dsId") String dsId, @Param("from") String from);

    /** 接口调用量 TOP5 */
    @Select("""
        SELECT l.interface_id, COALESCE(i.api_name, '(已删除接口)') AS api_name, COUNT(1) AS cnt
          FROM ont_ext_api_call_log l
          LEFT JOIN ont_ext_api_interface i ON i.id = l.interface_id
         WHERE l.ds_id = #{dsId} AND l.call_time >= #{from}
         GROUP BY l.interface_id, i.api_name ORDER BY cnt DESC LIMIT 5
    """)
    List<Map<String, Object>> statTop(@Param("dsId") String dsId, @Param("from") String from);
}
