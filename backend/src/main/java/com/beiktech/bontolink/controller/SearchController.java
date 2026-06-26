package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.mapper.SearchMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 全局搜索接口 — 头部 ⌘K 弹框驱动.
 *
 *   GET /api/search/global?q=xxx&type=all|object|link|prop|ds|other
 *
 * 返回结构 (按类型分组):
 *   { objects: [...], links: [...], valueTypes: [...], enumTypes: [...],
 *     sharedProps: [...], interfaces: [...], datasources: [...], typeClasses: [...], categories: [...] }
 *
 * 每条结果统一字段: id / title / subtitle / extra / description / rid / kind / route
 *   - kind: 用于前端渲染图标色 & 类型标签
 *   - route: 前端用 router.push 跳转 (如 /resources/object-types?openId=xxx)
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired private SearchMapper mapper;

    /** 每类返回条数上限 (综合模式 5, 单类型 30) */
    private static final int LIMIT_DIGEST = 5;
    private static final int LIMIT_FOCUSED = 30;

    /**
     * 全局搜索：按关键字 q 在各类资源表中模糊查询，type 指定范围(all/object/link/prop/ds/other)。
     * 综合模式每类上限 5 条，单类型聚焦时上限 30 条；结果附 kind + route 供前端直接跳转。
     */
    @GetMapping("/global")
    public R<Map<String, Object>> global(@RequestParam(required = false) String q,
                                          @RequestParam(required = false, defaultValue = "all") String type) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (q == null || q.trim().isEmpty()) {
            return R.ok(result);
        }
        String pat = "%" + q.trim() + "%";
        int limit = "all".equals(type) ? LIMIT_DIGEST : LIMIT_FOCUSED;

        if (matchType(type, "object")) {
            result.put("objects", decorate(mapper.searchClasses(pat, limit), "object",
                    "/resources/object-types"));
        }
        if (matchType(type, "link")) {
            result.put("links", decorate(mapper.searchLinkTypes(pat, limit), "link",
                    "/resources/link-types"));
        }
        if (matchType(type, "prop")) {
            result.put("sharedProps", decorate(mapper.searchSharedProperties(pat, limit), "sharedProp",
                    "/resources/shared-props"));
            result.put("valueTypes", decorate(mapper.searchValueTypes(pat, limit), "valueType",
                    "/resources/value-types"));
            result.put("enumTypes", decorate(mapper.searchEnumTypes(pat, limit), "enumType",
                    "/resources/enum-types"));
        }
        if (matchType(type, "ds")) {
            result.put("datasources", decorate(mapper.searchDataSources(pat, limit), "datasource",
                    "/resources/datasources"));
        }
        if (matchType(type, "other")) {
            result.put("interfaces", decorate(mapper.searchInterfaces(pat, limit), "interface",
                    "/resources/interfaces"));
            result.put("typeClasses", decorate(mapper.searchTypeClasses(pat, limit), "typeClass",
                    "/config/type-classes"));
            result.put("categories", decorate(mapper.searchCategories(pat, limit), "category",
                    "/config/category"));
        }

        int total = result.values().stream()
                .mapToInt(v -> ((List<?>) v).size())
                .sum();
        result.put("__total", total);
        result.put("__query", q);
        return R.ok(result);
    }

    private static boolean matchType(String type, String target) {
        return "all".equalsIgnoreCase(type) || target.equalsIgnoreCase(type);
    }

    /** 给每条 row 加上 kind + route, 方便前端用 router.push 跳转 */
    private static List<Map<String, Object>> decorate(List<Map<String, Object>> rows, String kind, String basePath) {
        for (Map<String, Object> row : rows) {
            row.put("kind", kind);
            // 详情页打开方式: 拼 openId 让目标列表自动打开侧抽屉 (各模块已支持)
            String id = row.get("id") == null ? "" : row.get("id").toString();
            row.put("route", basePath + (id.isEmpty() ? "" : "?openId=" + id));
        }
        return rows;
    }
}
