package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.*;
import java.util.Map;

/**
 * Ontology 本体模型版本 Mapper
 * 用于 OntologyModelManager 判断 OntModel 是否需要重建
 *
 * 版本号递增策略：
 * - admin 端修改 ont_class / ont_class_property / ont_class_link 等表时
 *   调用 incrementVersion() 使版本号 +1
 * - ontology 各实例检测到版本号变化后自动重建 OntModel
 */
@Mapper
public interface OntologyVersionMapper {

    /**
     * 获取当前版本号
     */
    @Select("SELECT version FROM ont_ontology_version WHERE id = 'ontology-model'")
    Integer getCurrentVersion();

    /**
     * 获取版本记录全部字段
     */
    @Select("SELECT * FROM ont_ontology_version WHERE id = 'ontology-model'")
    Map<String, Object> getVersionInfo();

    /**
     * 版本号 +1（admin 端修改本体数据后调用）
     */
    @Update("UPDATE ont_ontology_version SET version = version + 1, updated_at = CURRENT_TIMESTAMP, updated_by = #{userId} WHERE id = 'ontology-model'")
    int incrementVersion(@Param("userId") String userId);

    /**
     * 重置版本号为指定值（用于重建后标记已同步）
     */
    @Update("UPDATE ont_ontology_version SET version = #{version}, updated_at = CURRENT_TIMESTAMP WHERE id = 'ontology-model'")
    int resetVersion(@Param("version") int version);
}
