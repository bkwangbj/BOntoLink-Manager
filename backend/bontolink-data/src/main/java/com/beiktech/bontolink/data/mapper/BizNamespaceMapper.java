package com.beiktech.bontolink.data.mapper;

import com.beiktech.bontolink.data.entity.BizNamespace;
import com.beiktech.bontolink.data.entity.BizNamespaceVersion;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 业务命名空间 mapper，覆盖 ont_biz_namespace（命名空间主表）
 * 和 ont_biz_namespace_version（版本历史）两张表。
 */
@Mapper
public interface BizNamespaceMapper {

    // 查询所有命名空间，按编码排序
    @Select("SELECT * FROM ont_biz_namespace ORDER BY ns_code")
    List<BizNamespace> listAll();

    // 按命名空间编码查单条
    @Select("SELECT * FROM ont_biz_namespace WHERE ns_code = #{code}")
    BizNamespace findByCode(@Param("code") String code);

    // 新增命名空间
    @Insert("""
        INSERT INTO ont_biz_namespace(
            id, ns_code, ns_name, ns_uri, hierarchy_path, curr_version, status, metadata, rdfs_label, rdfs_comment
        ) VALUES (
            #{id}, #{nsCode}, #{nsName}, #{nsUri}, #{hierarchyPath}, #{currVersion}, #{status}, #{metadata}, #{rdfsLabel}, #{rdfsComment}
        )
    """)
    int insert(BizNamespace ns);

    // 更新命名空间基本信息（编码不可改）
    @Update("""
        UPDATE ont_biz_namespace SET
            ns_name=#{nsName}, ns_uri=#{nsUri}, hierarchy_path=#{hierarchyPath},
            curr_version=#{currVersion}, status=#{status}, metadata=#{metadata},
            rdfs_label=#{rdfsLabel}, rdfs_comment=#{rdfsComment}
        WHERE ns_code=#{nsCode}
    """)
    int update(BizNamespace ns);

    // 查询某命名空间的所有历史版本，当前版本优先，再按发布时间倒序
    @Select("SELECT * FROM ont_biz_namespace_version WHERE ns_code = #{code} ORDER BY is_current DESC, publish_time DESC")
    List<BizNamespaceVersion> listVersions(@Param("code") String code);

    // 按版本 id 查单条版本记录
    @Select("SELECT * FROM ont_biz_namespace_version WHERE id = #{id}")
    BizNamespaceVersion findVersionById(@Param("id") String id);

    /** 清掉某命名空间下所有版本的当前标记 */
    @Update("UPDATE ont_biz_namespace_version SET is_current = 0 WHERE ns_code = #{code}")
    int clearCurrentByNs(@Param("code") String code);

    /** 把某个版本标记为当前 */
    @Update("UPDATE ont_biz_namespace_version SET is_current = 1 WHERE id = #{id}")
    int setVersionCurrent(@Param("id") String id);

    /** 同步命名空间主表的当前版本号 */
    @Update("UPDATE ont_biz_namespace SET curr_version = #{version} WHERE ns_code = #{code}")
    int updateCurrVersion(@Param("code") String code, @Param("version") String version);

    // 新增版本记录（含 OWL 内容快照）
    @Insert("""
        INSERT INTO ont_biz_namespace_version(
            id, ns_code, version, uri, snapshot_data, owl_content, publish_time,
            is_current, status, rdfs_label, rdfs_comment, rdfs_see_also, rdfs_defined_by
        ) VALUES (
            #{id}, #{nsCode}, #{version}, #{uri}, #{snapshotData}, #{owlContent}, #{publishTime},
            #{isCurrent}, #{status}, #{rdfsLabel}, #{rdfsComment}, #{rdfsSeeAlso}, #{rdfsDefinedBy}
        )
    """)
    int insertVersion(BizNamespaceVersion v);

    // 删除指定版本记录
    @Delete("DELETE FROM ont_biz_namespace_version WHERE id = #{id}")
    int deleteVersion(@Param("id") String id);
}
