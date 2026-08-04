package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 函数 (Functions) Mapper
 * <p>
 * 覆盖 ont_function 主表及其四张子表 (参数 / 运行配置 / 环境变量 / 调用统计) 与
 * 版本库 ont_version_repo。
 * <p>
 * <b>方言中立约定</b>:入参出参类型串拼接、近 7 天调用量等聚合<b>一律不写进 SQL</b>
 * (group_concat / string_agg / 日期函数在 SQLite 与 PostgreSQL 上分叉),
 * 由 Mapper 返回明细行, Controller 在 Java 侧聚合。
 */
@Mapper
public interface FunctionMapper {

    /* ==================== 函数主表 ==================== */

    /** 全部函数 (不做聚合, 调用方自行按 full_access_path 取最新版本) */
    @Select("SELECT * FROM ont_function WHERE is_deleted = 0 ORDER BY update_time DESC")
    List<Map<String, Object>> listAll();

    @Select("SELECT * FROM ont_function WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") String id);

    /** 同一访问路径下的全部版本 (详情页顶部版本选择器) */
    @Select("SELECT * FROM ont_function WHERE full_access_path = #{path} AND is_deleted = 0 ORDER BY version_no DESC")
    List<Map<String, Object>> listVersionsByPath(@Param("path") String path);

    /** 同版本下访问路径是否已存在 (uk_path_version 前置校验) */
    @Select("SELECT 1 FROM ont_function WHERE full_access_path = #{path} AND version_no = #{version} AND is_deleted = 0 LIMIT 1")
    Integer existsByPathVersion(@Param("path") String path, @Param("version") String version);

    /**
     * 同文件内同名 api_name 的函数 id (文档 5.2 七、同名函数校验)。
     * 返回 id 而非布尔, 让调用方自行排除"改的就是它自己"这种情况 —— 避免在 SQL 里
     * 传 null 参数做比较 (PostgreSQL 对 setNull(OTHER) 会报无法推断类型)。
     */
    @Select("SELECT id FROM ont_function WHERE code_file_path = #{filePath} AND api_name = #{apiName} AND is_deleted = 0 LIMIT 1")
    String findIdByFileAndApiName(@Param("filePath") String filePath, @Param("apiName") String apiName);

    @Insert("""
        INSERT INTO ont_function(
            id, rid, version_no, api_name, function_label, function_type, language,
            industry_dir, category_dir, class_name, full_access_path, code_file_path,
            code_md5, code_content, file_line_start, file_line_end, status, visibility,
            rdfs_label, rdfs_comment, rdfs_see_also, rdfs_defined_by,
            create_user, publish_time, is_deleted)
        VALUES (
            #{id}, #{rid}, #{version_no}, #{api_name}, #{function_label}, #{function_type}, #{language},
            #{industry_dir}, #{category_dir}, #{class_name}, #{full_access_path}, #{code_file_path},
            #{code_md5}, #{code_content}, #{file_line_start}, #{file_line_end}, #{status}, #{visibility},
            #{rdfs_label}, #{rdfs_comment}, #{rdfs_see_also}, #{rdfs_defined_by},
            #{create_user}, #{publish_time}, #{is_deleted})
    """)
    int insert(Map<String, Object> row);

    /**
     * 更新函数。代码派生字段 (api_name / version_no / full_access_path / code_* / language)
     * 同样落在这里, 但 Controller 只在向导创建与代码同步场景下写, 详情页编辑不会传。
     */
    @Update("""
        UPDATE ont_function SET
          rid = #{rid}, version_no = #{version_no}, api_name = #{api_name},
          function_label = #{function_label}, function_type = #{function_type}, language = #{language},
          industry_dir = #{industry_dir}, category_dir = #{category_dir}, class_name = #{class_name},
          full_access_path = #{full_access_path}, code_file_path = #{code_file_path},
          code_md5 = #{code_md5}, code_content = #{code_content},
          file_line_start = #{file_line_start}, file_line_end = #{file_line_end},
          status = #{status}, visibility = #{visibility},
          rdfs_label = #{rdfs_label}, rdfs_comment = #{rdfs_comment},
          rdfs_see_also = #{rdfs_see_also}, rdfs_defined_by = #{rdfs_defined_by},
          create_user = #{create_user}, publish_time = #{publish_time},
          update_time = CURRENT_TIMESTAMP
        WHERE id = #{id}
    """)
    int update(Map<String, Object> row);

    @Update("UPDATE ont_function SET status = #{status}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") String id, @Param("status") Object status);

    @Update("UPDATE ont_function SET visibility = #{visibility}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateVisibility(@Param("id") String id, @Param("visibility") Object visibility);

    @Delete("DELETE FROM ont_function WHERE id = #{id}")
    int delete(@Param("id") String id);

    /** 行业 / 领域目录聚合 (左侧「行业领域分组」树, GROUP BY 两方言通用) */
    @Select("""
        SELECT industry_dir, category_dir, COUNT(1) AS cnt
        FROM ont_function WHERE is_deleted = 0
        GROUP BY industry_dir, category_dir
        ORDER BY industry_dir, category_dir
    """)
    List<Map<String, Object>> listDirCounts();

    /** 已有代码文件清单 (向导「文件」选择弹窗 + 重名检测) */
    @Select("""
        SELECT industry_dir, category_dir, code_file_path, class_name, COUNT(1) AS fn_count
        FROM ont_function WHERE is_deleted = 0
        GROUP BY industry_dir, category_dir, code_file_path, class_name
        ORDER BY code_file_path
    """)
    List<Map<String, Object>> listCodeFiles();

    /**
     * 对象类的 id ↔ api_name 对照 (供参数类型 "[命名空间] 类名" 反查对象类 id)。
     * 参数行的 object_class_id 只有走新建向导选过本体对象才有值, 代码同步过来的
     * 参数只有类型字符串, 靠这张对照表补上跳转所需的 id。
     */
    @Select("SELECT id, api_name FROM ont_class WHERE api_name IS NOT NULL")
    List<Map<String, Object>> listClassApiNames();

    /* ==================== 参数 ==================== */

    /** 全量参数 (列表页一次取回, Java 侧按 function_id 分组, 避免 N+1) */
    @Select("""
        SELECT p.* FROM ont_function_param p
        JOIN ont_function f ON f.id = p.function_id AND f.is_deleted = 0
        WHERE p.is_deleted = 0
        ORDER BY p.param_direction, p.sort_num
    """)
    List<Map<String, Object>> listAllParams();

    @Select("SELECT * FROM ont_function_param WHERE function_id = #{id} AND is_deleted = 0 ORDER BY param_direction, sort_num")
    List<Map<String, Object>> listParams(@Param("id") String id);

    @Insert("""
        INSERT INTO ont_function_param(
            id, function_id, param_name, param_type, param_direction, is_required,
            default_value, value_range, param_desc, object_class_id, sort_num, is_deleted)
        VALUES (
            #{id}, #{function_id}, #{param_name}, #{param_type}, #{param_direction}, #{is_required},
            #{default_value}, #{value_range}, #{param_desc}, #{object_class_id}, #{sort_num}, 0)
    """)
    int insertParam(Map<String, Object> row);

    /** 详情页只允许改参数说明 (代码派生的名称/类型只读) */
    @Update("UPDATE ont_function_param SET param_desc = #{param_desc}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateParamDesc(@Param("id") String id, @Param("param_desc") Object paramDesc);

    @Delete("DELETE FROM ont_function_param WHERE function_id = #{id}")
    int deleteParamsByFunction(@Param("id") String id);

    /* ==================== 运行配置 (1:1) ==================== */

    @Select("SELECT * FROM ont_function_runtime_config WHERE function_id = #{id} AND is_deleted = 0 LIMIT 1")
    Map<String, Object> getRuntimeConfig(@Param("id") String id);

    @Insert("""
        INSERT INTO ont_function_runtime_config(
            id, function_id, timeout, retry_count, retry_interval, memory_quota,
            concurrency_limit, enable_cache, cache_ttl, is_deleted)
        VALUES (
            #{id}, #{function_id}, #{timeout}, #{retry_count}, #{retry_interval}, #{memory_quota},
            #{concurrency_limit}, #{enable_cache}, #{cache_ttl}, 0)
    """)
    int insertRuntimeConfig(Map<String, Object> row);

    @Update("""
        UPDATE ont_function_runtime_config SET
          timeout = #{timeout}, retry_count = #{retry_count}, retry_interval = #{retry_interval},
          memory_quota = #{memory_quota}, concurrency_limit = #{concurrency_limit},
          enable_cache = #{enable_cache}, cache_ttl = #{cache_ttl},
          update_time = CURRENT_TIMESTAMP
        WHERE function_id = #{function_id}
    """)
    int updateRuntimeConfig(Map<String, Object> row);

    @Delete("DELETE FROM ont_function_runtime_config WHERE function_id = #{id}")
    int deleteRuntimeConfigByFunction(@Param("id") String id);

    /* ==================== 环境变量 (1:N) ==================== */

    @Select("SELECT * FROM ont_function_env_var WHERE function_id = #{id} AND is_deleted = 0 ORDER BY sort_num")
    List<Map<String, Object>> listEnvVars(@Param("id") String id);

    @Insert("""
        INSERT INTO ont_function_env_var(
            id, function_id, var_name, var_value, var_type, value_range,
            var_desc, is_encrypt, sort_num, is_deleted)
        VALUES (
            #{id}, #{function_id}, #{var_name}, #{var_value}, #{var_type}, #{value_range},
            #{var_desc}, #{is_encrypt}, #{sort_num}, 0)
    """)
    int insertEnvVar(Map<String, Object> row);

    @Delete("DELETE FROM ont_function_env_var WHERE function_id = #{id}")
    int deleteEnvVarsByFunction(@Param("id") String id);

    /* ==================== 调用统计 (扩展表) ==================== */

    /** 全量统计明细 (列表页近7天/总调用在 Java 侧算) */
    @Select("""
        SELECT s.function_id, s.stat_date, s.caller_app, s.call_count, s.success_count,
               s.error_count, s.avg_cost_ms
        FROM ont_function_call_stat s
        JOIN ont_function f ON f.id = s.function_id AND f.is_deleted = 0
        ORDER BY s.stat_date
    """)
    List<Map<String, Object>> listAllCallStats();

    @Select("SELECT * FROM ont_function_call_stat WHERE function_id = #{id} ORDER BY stat_date")
    List<Map<String, Object>> listCallStats(@Param("id") String id);

    @Delete("DELETE FROM ont_function_call_stat WHERE function_id = #{id}")
    int deleteCallStatsByFunction(@Param("id") String id);

    /* ==================== 版本库 ==================== */

    @Select("SELECT * FROM ont_version_repo WHERE is_deleted = 0 ORDER BY industry_dir, category_dir, version_no DESC")
    List<Map<String, Object>> listRepos();

    @Select("SELECT * FROM ont_version_repo WHERE id = #{id}")
    Map<String, Object> findRepoById(@Param("id") String id);

    @Select("""
        SELECT * FROM ont_version_repo
        WHERE industry_dir = #{industry} AND category_dir = #{category} AND version_no = #{version}
          AND is_deleted = 0 LIMIT 1
    """)
    Map<String, Object> findRepoByDirVersion(@Param("industry") String industry,
                                             @Param("category") String category,
                                             @Param("version") String version);

    @Insert("""
        INSERT INTO ont_version_repo(
            id, rid, industry_dir, category_dir, version_no, repo_branch, repo_commit_id, repo_url,
            version_status, is_default, release_note, publish_user, publish_time, is_deleted)
        VALUES (
            #{id}, #{rid}, #{industry_dir}, #{category_dir}, #{version_no}, #{repo_branch}, #{repo_commit_id}, #{repo_url},
            #{version_status}, #{is_default}, #{release_note}, #{publish_user}, #{publish_time}, 0)
    """)
    int insertRepo(Map<String, Object> row);

    @Update("""
        UPDATE ont_version_repo SET
          repo_branch = #{repo_branch}, repo_commit_id = #{repo_commit_id}, repo_url = #{repo_url},
          version_status = #{version_status}, is_default = #{is_default}, release_note = #{release_note},
          publish_user = #{publish_user}, publish_time = #{publish_time}, update_time = CURRENT_TIMESTAMP
        WHERE id = #{id}
    """)
    int updateRepo(Map<String, Object> row);
}
