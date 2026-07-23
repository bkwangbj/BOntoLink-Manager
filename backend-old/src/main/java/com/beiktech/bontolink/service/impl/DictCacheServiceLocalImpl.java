package com.beiktech.bontolink.service.impl;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.beiktech.bontolink.data.mapper.DictMapper;
import com.beiktech.bontolink.service.DictCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 字典缓存服务 - 本地缓存实现
 * 仅使用 Caffeine 本地内存缓存
 */
@Service
@ConditionalOnProperty(name = "bontolink.cache.type", havingValue = "local", matchIfMissing = true)
public class DictCacheServiceLocalImpl implements DictCacheService {

    @Autowired
    private DictMapper dictMapper;

    @Override
    @Cached(name = "dict:items", key = "#code", expire = 0, cacheType = CacheType.LOCAL)
    public List<Map<String, Object>> getItemsByCode(String code) {
        return dictMapper.getItemsByCode(code);
    }

    @Override
    @CacheInvalidate(name = "dict:items", key = "#code")
    public void evictDictCacheByCode(String code) {
    }

    @Override
    @CacheInvalidate(name = "dict:items")
    public void evictDictCache() {
    }
}
