package com.beiktech.bontolink.mapper;

import com.beiktech.bontolink.entity.SysDataSource;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysDataSourceMapper {

    @Select("SELECT * FROM sys_data_source ORDER BY create_time DESC")
    List<SysDataSource> listAll();

    @Select("SELECT * FROM sys_data_source WHERE id = #{id}")
    SysDataSource findById(@Param("id") String id);

    @Select("SELECT ds_type, COUNT(*) AS cnt FROM sys_data_source GROUP BY ds_type")
    List<Map<String, Object>> groupByType();

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

    @Update("""
        UPDATE sys_data_source SET
            category_code=#{categoryCode}, ds_code=#{dsCode}, ds_name=#{dsName}, ds_type=#{dsType},
            jdbc_driver=#{jdbcDriver}, jdbc_url=#{jdbcUrl}, username=#{username}, password=#{password},
            mongo_url=#{mongoUrl}, status=#{status}, remark=#{remark}
        WHERE id=#{id}
    """)
    int update(SysDataSource d);

    @Update("UPDATE sys_data_source SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") String id, @Param("status") int status);

    @Update("UPDATE sys_data_source SET connect_status=#{connectStatus}, response_ms=#{responseMs}, active_conn=#{activeConn} WHERE id=#{id}")
    int updateMonitor(@Param("id") String id,
                      @Param("connectStatus") String connectStatus,
                      @Param("responseMs") int responseMs,
                      @Param("activeConn") int activeConn);

    @Delete("DELETE FROM sys_data_source WHERE id = #{id}")
    int delete(@Param("id") String id);
}
