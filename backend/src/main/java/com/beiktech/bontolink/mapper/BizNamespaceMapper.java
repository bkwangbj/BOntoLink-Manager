package com.beiktech.bontolink.mapper;

import com.beiktech.bontolink.entity.BizNamespace;
import com.beiktech.bontolink.entity.BizNamespaceVersion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BizNamespaceMapper {

    @Select("SELECT * FROM ont_biz_namespace ORDER BY ns_code")
    List<BizNamespace> listAll();

    @Select("SELECT * FROM ont_biz_namespace WHERE ns_code = #{code}")
    BizNamespace findByCode(@Param("code") String code);

    @Insert("""
        INSERT INTO ont_biz_namespace(
            id, ns_code, ns_name, ns_uri, hierarchy_path, curr_version, status, metadata, rdfs_label, rdfs_comment
        ) VALUES (
            #{id}, #{nsCode}, #{nsName}, #{nsUri}, #{hierarchyPath}, #{currVersion}, #{status}, #{metadata}, #{rdfsLabel}, #{rdfsComment}
        )
    """)
    int insert(BizNamespace ns);

    @Update("""
        UPDATE ont_biz_namespace SET
            ns_name=#{nsName}, ns_uri=#{nsUri}, hierarchy_path=#{hierarchyPath},
            curr_version=#{currVersion}, status=#{status}, metadata=#{metadata},
            rdfs_label=#{rdfsLabel}, rdfs_comment=#{rdfsComment}
        WHERE ns_code=#{nsCode}
    """)
    int update(BizNamespace ns);

    @Select("SELECT * FROM ont_biz_namespace_version WHERE ns_code = #{code} ORDER BY publish_time DESC")
    List<BizNamespaceVersion> listVersions(@Param("code") String code);

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

    @Delete("DELETE FROM ont_biz_namespace_version WHERE id = #{id}")
    int deleteVersion(@Param("id") String id);
}
