package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;

/**
 * 总览 (Overview) 统计 Mapper —— 15 项资源 active/total 计数, 支持 category_code 过滤
 * 严格对应 总览6.5.pdf 规范的"统计项及顺序"表 15 行 (含三行布局).
 *
 * 约定:
 *  - "active" 指状态为 启用/正式的资源
 *  - "total"  指所有状态 (含禁用 / 实验中 / 已弃用)
 *  - 入参 codes 为 null 或空集合时, 视为"全部范围", SQL 跳过 WHERE category_code IN (...)
 */
@Mapper
public interface OverviewMapper {

    /* ============================== 第一行: 行业 / 领域 / 分组 / 数据源 ============================== */

    /** 统计行业数量 (category_type=1) */
    @Select({"<script>",
            "SELECT COUNT(*) FROM ont_biz_category WHERE category_type = 1",
            "<if test='codes != null and codes.size() > 0'>",
            "  AND category_code IN <foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>",
            "</if>",
            "<if test='activeOnly'> AND status = 1</if>",
            "</script>"})
    int countIndustries(@Param("codes") Collection<String> codes, @Param("activeOnly") boolean activeOnly);

    /** 统计领域数量 (category_type=2) */
    @Select({"<script>",
            "SELECT COUNT(*) FROM ont_biz_category WHERE category_type = 2",
            "<if test='codes != null and codes.size() > 0'>",
            "  AND category_code IN <foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>",
            "</if>",
            "<if test='activeOnly'> AND status = 1</if>",
            "</script>"})
    int countDomains(@Param("codes") Collection<String> codes, @Param("activeOnly") boolean activeOnly);

    /** 分组 ont_biz_group 无 status 字段, active == total (无禁用态) */
    @Select({"<script>",
            "SELECT COUNT(*) FROM ont_biz_group",
            "<where>",
            "  <if test='codes != null and codes.size() > 0'>",
            "    category_code IN <foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>",
            "  </if>",
            "</where></script>"})
    int countGroups(@Param("codes") Collection<String> codes, @Param("activeOnly") boolean activeOnly);

    /** 统计数据源数量 (sys_data_source) */
    @Select({"<script>",
            "SELECT COUNT(*) FROM sys_data_source",
            "<where>",
            "  <if test='codes != null and codes.size() > 0'>",
            "    category_code IN <foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>",
            "  </if>",
            "  <if test='activeOnly'> AND status = 1</if>",
            "</where></script>"})
    int countDatasources(@Param("codes") Collection<String> codes, @Param("activeOnly") boolean activeOnly);

    /* ============================== 第二行: 对象 / 关系 / 动作 / 函数 / 类型类 / 接口 ============================== */

    /** 统计对象类型数量 (ont_class) */
    @Select({"<script>",
            "SELECT COUNT(*) FROM ont_class",
            "<where>",
            "  <if test='codes != null and codes.size() > 0'>",
            "    category_code IN <foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>",
            "  </if>",
            "  <if test='activeOnly'> AND status = 1</if>",
            "</where></script>"})
    int countObjectTypes(@Param("codes") Collection<String> codes, @Param("activeOnly") boolean activeOnly);

    /** 关系类型 = ont_link_types, status 字符串 active / experimental / deprecated */
    @Select({"<script>",
            "SELECT COUNT(*) FROM ont_link_types",
            "<where>",
            "  <if test='codes != null and codes.size() > 0'>",
            "    category_code IN <foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>",
            "  </if>",
            "  <if test='activeOnly'> AND status = 'active'</if>",
            "</where></script>"})
    int countLinkTypes(@Param("codes") Collection<String> codes, @Param("activeOnly") boolean activeOnly);

    /** 动作类型: ont_class_action, scope 经 ont_class 转换 */
    @Select({"<script>",
            "SELECT COUNT(*) FROM ont_class_action a",
            "<where>",
            "  <if test='codes != null and codes.size() > 0'>",
            "    EXISTS (SELECT 1 FROM ont_class c WHERE c.id = a.class_id AND c.category_code IN ",
            "    <foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>)",
            "  </if>",
            "  <if test='activeOnly'> AND a.status = 1</if>",
            "</where></script>"})
    int countActionTypes(@Param("codes") Collection<String> codes, @Param("activeOnly") boolean activeOnly);

    /** 函数 = ont_class_property is_derived=1 (即"派生/计算字段") */
    @Select({"<script>",
            "SELECT COUNT(*) FROM ont_class_property p WHERE p.is_derived = 1",
            "<if test='codes != null and codes.size() > 0'>",
            "  AND EXISTS (SELECT 1 FROM ont_class c WHERE c.id = p.class_id AND c.category_code IN ",
            "  <foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>)",
            "</if>",
            "<if test='activeOnly'> AND p.status = 1</if>",
            "</script>"})
    int countFunctions(@Param("codes") Collection<String> codes, @Param("activeOnly") boolean activeOnly);

    /** 类型类 ont_type_class, 用 is_deprecated 字段表达启用/弃用; 无 category_code, codes 不参与过滤 */
    @Select({"<script>",
            "SELECT COUNT(*) FROM ont_type_class",
            "<if test='activeOnly'> WHERE is_deprecated = 0</if>",
            "</script>"})
    int countTypeClasses(@Param("activeOnly") boolean activeOnly);

    /** 接口 ont_interface — 没 category_code 字段, 走 ns_code 关联到 category 比较麻烦; 简化为全局计数 */
    @Select({"<script>",
            "SELECT COUNT(*) FROM ont_interface",
            "<where>",
            "  <if test='codes != null and codes.size() > 0'>",
            "    category_code IN <foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>",
            "  </if>",
            "  <if test='activeOnly'> AND status = 1</if>",
            "</where></script>"})
    int countInterfaces(@Param("codes") Collection<String> codes, @Param("activeOnly") boolean activeOnly);

    /* ============================== 第三行: 属性 / 枚举 / 值类型 / 结构属性 / 共享属性 ============================== */

    /** 统计对象属性数量; 通过 EXISTS 子查询关联 ont_class 实现 category_code 过滤 */
    @Select({"<script>",
            "SELECT COUNT(*) FROM ont_class_property p",
            "<where>",
            "  <if test='codes != null and codes.size() > 0'>",
            "    EXISTS (SELECT 1 FROM ont_class c WHERE c.id = p.class_id AND c.category_code IN ",
            "    <foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>)",
            "  </if>",
            "  <if test='activeOnly'> AND p.status = 1</if>",
            "</where></script>"})
    int countProperties(@Param("codes") Collection<String> codes, @Param("activeOnly") boolean activeOnly);

    /** 枚举类型 ont_enum_types, status TEXT 'active' / 'deprecated' */
    @Select({"<script>",
            "SELECT COUNT(*) FROM ont_enum_types",
            "<where>",
            "  <if test='codes != null and codes.size() > 0'>",
            "    category_code IN <foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>",
            "  </if>",
            "  <if test='activeOnly'> AND status = 'active'</if>",
            "</where></script>"})
    int countEnumTypes(@Param("codes") Collection<String> codes, @Param("activeOnly") boolean activeOnly);

    /** 统计值类型数量 (ont_value_types) */
    @Select({"<script>",
            "SELECT COUNT(*) FROM ont_value_types",
            "<where>",
            "  <if test='codes != null and codes.size() > 0'>",
            "    category_code IN <foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>",
            "  </if>",
            "  <if test='activeOnly'> AND status = 1</if>",
            "</where></script>"})
    int countValueTypes(@Param("codes") Collection<String> codes, @Param("activeOnly") boolean activeOnly);

    /** 统计结构属性类型数量 (ont_struct_types) */
    @Select({"<script>",
            "SELECT COUNT(*) FROM ont_struct_types",
            "<where>",
            "  <if test='codes != null and codes.size() > 0'>",
            "    category_code IN <foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>",
            "  </if>",
            "  <if test='activeOnly'> AND status = 1</if>",
            "</where></script>"})
    int countStructProperties(@Param("codes") Collection<String> codes, @Param("activeOnly") boolean activeOnly);

    /** 统计共享属性数量 (ont_shared_properties) */
    @Select({"<script>",
            "SELECT COUNT(*) FROM ont_shared_properties",
            "<where>",
            "  <if test='codes != null and codes.size() > 0'>",
            "    category_code IN <foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>",
            "  </if>",
            "  <if test='activeOnly'> AND status = 1</if>",
            "</where></script>"})
    int countSharedProperties(@Param("codes") Collection<String> codes, @Param("activeOnly") boolean activeOnly);
}
