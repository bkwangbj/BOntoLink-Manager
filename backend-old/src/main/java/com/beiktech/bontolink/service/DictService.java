package com.beiktech.bontolink.service;

import com.beiktech.bontolink.data.mapper.DictMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 字典管理服务：业务逻辑 + 缓存管理（委托给 DictCacheService）。
 * 供其他模块通过 getItemsByCode() 获取字典条目（走 JetCache 缓存）。
 */
@Service
public class DictService {

    @Autowired
    private DictMapper dictMapper;

    @Autowired
    private DictCacheService dictCacheService;

    // ==================== 字典定义 ====================

    public List<Map<String, Object>> listDefs() {
        return dictMapper.listDefs();
    }

    public Map<String, Object> getDef(String id) {
        return dictMapper.getDef(id);
    }

    public Map<String, Object> createDef(Map<String, Object> body) {
        String id = "dict-" + UUID.randomUUID();
        body.put("id", id);
        dictMapper.insertDef(body);
        return dictMapper.getDef(id);
    }

    public Map<String, Object> updateDef(String id, Map<String, Object> body) {
        body.put("id", id);
        dictMapper.updateDef(body);
        return dictMapper.getDef(id);
    }

    public void deleteDef(String id) {
        dictMapper.deleteItemsByDict(id);
        dictMapper.deleteDef(id);
    }

    // ==================== 字典条目 ====================

    public List<Map<String, Object>> listItems(String dictId) {
        return dictMapper.listItems(dictId);
    }

    public Map<String, Object> createItem(Map<String, Object> body) {
        String id = "dict-item-" + UUID.randomUUID();
        body.put("id", id);
        dictMapper.insertItem(body);
        return dictMapper.getItem(id);
    }

    public Map<String, Object> updateItem(String id, Map<String, Object> body) {
        body.put("id", id);
        dictMapper.updateItem(body);
        return dictMapper.getItem(id);
    }

    public void deleteItem(String id) {
        // 级联删除子节点
        List<Map<String, Object>> children = dictMapper.listItemsByParent(id);
        for (Map<String, Object> child : children) {
            deleteItem((String) child.get("id"));
        }
        dictMapper.deleteItem(id);
    }

    // ==================== 树形组装 ====================

    /**
     * 查询字典条目并组装为树形结构（后端递归挂载 children）。
     */
    public List<Map<String, Object>> treeItems(String dictId) {
        List<Map<String, Object>> all = dictMapper.listAllItems(dictId);
        Map<String, List<Map<String, Object>>> childrenMap = all.stream()
                .filter(item -> item.get("parentId") != null && !((String) item.get("parentId")).isEmpty())
                .collect(Collectors.groupingBy(item -> (String) item.get("parentId")));
        List<Map<String, Object>> roots = all.stream()
                .filter(item -> item.get("parentId") == null || ((String) item.get("parentId")).isEmpty())
                .peek(item -> attachChildren(item, childrenMap))
                .collect(Collectors.toList());
        return roots;
    }

    @SuppressWarnings("unchecked")
    private void attachChildren(Map<String, Object> parent,
                                Map<String, List<Map<String, Object>>> childrenMap) {
        List<Map<String, Object>> kids = childrenMap.get(parent.get("id"));
        if (kids != null) {
            parent.put("children", kids);
            for (Map<String, Object> kid : kids) {
                attachChildren(kid, childrenMap);
            }
        }
    }

    // ==================== 公开查询（长效缓存 - 委托给 DictCacheService） ====================

    /**
     * 按 dict_code 查启用条目。
     * 根据 bontolink.cache.type 配置自动使用 LOCAL 或 BOTH 缓存策略。
     */
    public List<Map<String, Object>> getItemsByCode(String code) {
        return dictCacheService.getItemsByCode(code);
    }

    /** 清空指定字典编码的缓存 */
    public void evictDictCacheByCode(String code) {
        dictCacheService.evictDictCacheByCode(code);
    }

    /**
     * 清空 dict:items 缓存（增删改或手动刷新时调用）。
     */
    public void evictDictCache() {
        dictCacheService.evictDictCache();
    }
}
