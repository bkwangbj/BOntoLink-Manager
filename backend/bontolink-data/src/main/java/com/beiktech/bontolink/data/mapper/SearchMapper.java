package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 全局搜索 Mapper — 跨表 LIKE 搜索 8 大模块, 每类限 LIMIT 用于头部全局搜索弹框.
 * 字段返回结构尽量统一: id / title / subtitle / extra / rid.
 */
@Mapper
public interface SearchMapper {

    /* ---------- 对象类型 ---------- */
    /** 按关键字搜索对象类型, 优先展示 display_name */
    @Select("SELECT id, " +
            "       COALESCE(display_name, api_name) AS title, " +
            "       api_name AS subtitle, " +
            "       category_code AS extra, " +
            "       rdfs_comment AS description, " +
            "       icon, color, rid " +
            "FROM ont_class " +
            "WHERE display_name LIKE #{q} OR api_name LIKE #{q} OR rdfs_label LIKE #{q} OR rdfs_comment LIKE #{q} " +
            "ORDER BY update_time DESC LIMIT #{limit}")
    List<Map<String, Object>> searchClasses(@Param("q") String q, @Param("limit") int limit);

    /* ---------- 链接类型 ---------- */
    /** 按关键字搜索链接类型; title 优先取 rdfs_label, 否则拼接左右显示名 */
    @Select("SELECT id, " +
            "       COALESCE(rdfs_label, l_display_name || ' / ' || r_display_name) AS title, " +
            "       link_type_id AS subtitle, " +
            "       category_code AS extra, " +
            "       rdfs_comment AS description, " +
            "       rid " +
            "FROM ont_link_types " +
            "WHERE link_type_id LIKE #{q} OR rdfs_label LIKE #{q} OR l_display_name LIKE #{q} OR r_display_name LIKE #{q} OR l_api_name LIKE #{q} OR r_api_name LIKE #{q} " +
            "ORDER BY updated_at DESC LIMIT #{limit}")
    List<Map<String, Object>> searchLinkTypes(@Param("q") String q, @Param("limit") int limit);

    /* ---------- 值类型 ---------- */
    /** 按关键字搜索值类型 */
    @Select("SELECT id, " +
            "       COALESCE(rdfs_label, api_name) AS title, " +
            "       api_name AS subtitle, " +
            "       base_type AS extra, " +
            "       rdfs_comment AS description, " +
            "       rid " +
            "FROM ont_value_types " +
            "WHERE api_name LIKE #{q} OR rdfs_label LIKE #{q} OR rdfs_comment LIKE #{q} " +
            "ORDER BY update_time DESC LIMIT #{limit}")
    List<Map<String, Object>> searchValueTypes(@Param("q") String q, @Param("limit") int limit);

    /* ---------- 枚举类型 ---------- */
    /** 按关键字搜索枚举类型 */
    @Select("SELECT id, " +
            "       COALESCE(rdfs_label, api_name) AS title, " +
            "       api_name AS subtitle, " +
            "       enum_type AS extra, " +
            "       rdfs_comment AS description, " +
            "       rid " +
            "FROM ont_enum_types " +
            "WHERE api_name LIKE #{q} OR rdfs_label LIKE #{q} OR rdfs_comment LIKE #{q} " +
            "ORDER BY update_time DESC LIMIT #{limit}")
    List<Map<String, Object>> searchEnumTypes(@Param("q") String q, @Param("limit") int limit);

    /* ---------- 共享属性 ---------- */
    /** 按关键字搜索共享属性 */
    @Select("SELECT id, " +
            "       COALESCE(rdfs_label, prop_code) AS title, " +
            "       prop_code AS subtitle, " +
            "       prop_type AS extra, " +
            "       rdfs_comment AS description, " +
            "       rid " +
            "FROM ont_shared_properties " +
            "WHERE prop_code LIKE #{q} OR rdfs_label LIKE #{q} OR rdfs_comment LIKE #{q} " +
            "ORDER BY update_time DESC LIMIT #{limit}")
    List<Map<String, Object>> searchSharedProperties(@Param("q") String q, @Param("limit") int limit);

    /* ---------- 接口 ---------- */
    /** 按关键字搜索接口定义 */
    @Select("SELECT id, " +
            "       COALESCE(display_name, api_name) AS title, " +
            "       api_name AS subtitle, " +
            "       category_code AS extra, " +
            "       rdfs_comment AS description, " +
            "       icon, color, rid " +
            "FROM ont_interface " +
            "WHERE api_name LIKE #{q} OR display_name LIKE #{q} OR rdfs_label LIKE #{q} OR rdfs_comment LIKE #{q} " +
            "ORDER BY update_time DESC LIMIT #{limit}")
    List<Map<String, Object>> searchInterfaces(@Param("q") String q, @Param("limit") int limit);

    /* ---------- 数据源 ---------- */
    /** 按关键字搜索数据源 */
    @Select("SELECT id, " +
            "       ds_name AS title, " +
            "       ds_code AS subtitle, " +
            "       ds_type AS extra, " +
            "       remark AS description " +
            "FROM sys_data_source " +
            "WHERE ds_name LIKE #{q} OR ds_code LIKE #{q} OR ds_type LIKE #{q} OR remark LIKE #{q} " +
            "ORDER BY update_time DESC LIMIT #{limit}")
    List<Map<String, Object>> searchDataSources(@Param("q") String q, @Param("limit") int limit);

    /* ---------- 类型类 ---------- */
    /** 按关键字搜索类型类 */
    @Select("SELECT id, " +
            "       name_cn AS title, " +
            "       name AS subtitle, " +
            "       category_cn AS extra, " +
            "       description " +
            "FROM ont_type_class " +
            "WHERE name LIKE #{q} OR name_cn LIKE #{q} OR category LIKE #{q} OR category_cn LIKE #{q} OR description LIKE #{q} " +
            "ORDER BY updated_at DESC LIMIT #{limit}")
    List<Map<String, Object>> searchTypeClasses(@Param("q") String q, @Param("limit") int limit);

    /* ---------- 行业分类 ---------- */
    /** 按关键字搜索行业/领域分类; CASE WHEN 将 category_type 数值转为"行业/领域/其他"中文标签 */
    @Select("SELECT id, " +
            "       COALESCE(rdfs_label, category_code) AS title, " +
            "       category_code AS subtitle, " +
            "       CASE category_type WHEN 1 THEN '行业' WHEN 2 THEN '领域' ELSE '其他' END AS extra, " +
            "       rdfs_comment AS description " +
            "FROM ont_biz_category " +
            "WHERE category_code LIKE #{q} OR rdfs_label LIKE #{q} OR rdfs_comment LIKE #{q} " +
            "ORDER BY update_time DESC LIMIT #{limit}")
    List<Map<String, Object>> searchCategories(@Param("q") String q, @Param("limit") int limit);
}
