package com.beiktech.bontolink.data.mapper;

import com.beiktech.bontolink.data.entity.SysDataSource;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 数据源 mapper，覆盖 sys_data_source 表。
 * 支持 JDBC（MySQL/PostgreSQL 等）和 MongoDB 两类连接配置。
 */
@Mapper
public interface SysDataSourceMapper {

    // 查询所有数据源，按创建时间倒序
    @Select("SELECT * FROM sys_data_source ORDER BY create_time DESC")
    List<SysDataSource> listAll();

    // 按 id 查单条数据源
    @Select("SELECT * FROM sys_data_source WHERE id = #{id}")
    SysDataSource findById(@Param("id") String id);

    // 按数据源类型分组统计数量（用于首页统计卡片）
    @Select("SELECT ds_type, COUNT(*) AS cnt FROM sys_data_source GROUP BY ds_type")
    List<Map<String, Object>> groupByType();

    // 新增数据源（含连接参数和监控初始值）
    @Insert("""
        INSERT INTO sys_data_source(
            id, category_code, ds_code, ds_name, ds_type, jdbc_driver, jdbc_url,
            username, password, mongo_url, status, remark,
            ref_count, connect_status, active_conn, max_conn, response_ms, collection_cnt
        ) VALUES (
            #{id}, #{categoryCode}, #{dsCode}, #{dsName}, #{dsType}, #{jdbcDriver}, #{jdbcUrl},
            #{username}, #{password}, #{mongoUrl}, #{status}, #{remark},
            #{refCount}, #{connectStatus}, #{activeConn}, #{maxConn}, #{responseMs}, #{collectionCnt}
        )
    """)
    int insert(SysDataSource d);

    // 更新数据源基本配置（不更新探活类监控字段；max_conn 为用户可配置项，随表单更新，null 时保留原值）
    @Update("""
        UPDATE sys_data_source SET
            category_code=#{categoryCode}, ds_code=#{dsCode}, ds_name=#{dsName}, ds_type=#{dsType},
            jdbc_driver=#{jdbcDriver}, jdbc_url=#{jdbcUrl}, username=#{username}, password=#{password},
            mongo_url=#{mongoUrl}, status=#{status}, remark=#{remark},
            max_conn=COALESCE(#{maxConn}, max_conn)
        WHERE id=#{id}
    """)
    int update(SysDataSource d);

    // 单独更新启用/禁用状态
    @Update("UPDATE sys_data_source SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") String id, @Param("status") int status);

    // 更新连接探活结果（连接状态/响应时长/活跃连接数）
    @Update("UPDATE sys_data_source SET connect_status=#{connectStatus}, response_ms=#{responseMs}, active_conn=#{activeConn} WHERE id=#{id}")
    int updateMonitor(@Param("id") String id,
                      @Param("connectStatus") String connectStatus,
                      @Param("responseMs") int responseMs,
                      @Param("activeConn") int activeConn);

    // 单独更新数据源最大连接数（连接池 resize 时使用）
    @Update("UPDATE sys_data_source SET max_conn=#{maxConn} WHERE id=#{id}")
    int updateMaxConn(@Param("id") String id, @Param("maxConn") int maxConn);

    // 删除数据源
    @Delete("DELETE FROM sys_data_source WHERE id = #{id}")
    int delete(@Param("id") String id);
}
