package com.beiktech.bontolink.common;

import com.beiktech.bontolink.service.DictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 字典缓存助手 — 为各 Service 层提供便捷的字典查询方法。
 * 所有查询走 DictService（已通过 JetCache 缓存），
 * 修改字典后调用 evictDictCache() / refresh-cache 接口使缓存失效。
 */
@Component
public class DictCacheHelper {

    @Autowired
    private DictService dictService;

    /** 获取字典的所有条目（已缓存） */
    public List<Map<String, Object>> getItems(String dictCode) {
        return dictService.getItemsByCode(dictCode);
    }

    /** 获取某编码对应的显示值，如 getValue("sys_act_status", "1") → "启用" */
    public String getValue(String dictCode, String itemCode) {
        if (itemCode == null) return "";
        List<Map<String, Object>> items = getItems(dictCode);
        for (Map<String, Object> item : items) {
            if (itemCode.equals(item.get("item_code"))) {
                Object val = item.get("item_value");
                return val != null ? String.valueOf(val) : "";
            }
        }
        return "";
    }

    /** 判断指定编码是否有效（存在于字典中） */
    public boolean exists(String dictCode, String itemCode) {
        if (itemCode == null) return false;
        List<Map<String, Object>> items = getItems(dictCode);
        return items.stream().anyMatch(item -> itemCode.equals(item.get("item_code")));
    }

    /** 将字典转为 Map<item_code, item_value>，便于快速查找 */
    public Map<String, String> toMap(String dictCode) {
        return getItems(dictCode).stream()
                .filter(item -> item.get("item_code") != null)
                .collect(Collectors.toMap(
                        item -> String.valueOf(item.get("item_code")),
                        item -> item.get("item_value") != null ? String.valueOf(item.get("item_value")) : "",
                        (a, b) -> b
                ));
    }

    /** 获取字典条目数 */
    public int count(String dictCode) {
        return getItems(dictCode).size();
    }
}
