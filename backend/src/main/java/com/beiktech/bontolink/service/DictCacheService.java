package com.beiktech.bontolink.service;

import java.util.List;
import java.util.Map;

/**
 * 字典缓存服务接口
 * 根据 bontolink.cache.type 配置加载不同实现
 */
public interface DictCacheService {

    /**
     * 按字典编码查询启用的条目
     */
    List<Map<String, Object>> getItemsByCode(String code);

    /**
     * 清空指定字典编码的缓存
     */
    void evictDictCacheByCode(String code);

    /**
     * 清空所有字典缓存
     */
    void evictDictCache();
}
