package com.beiktech.bontolink.service.semantic;

import com.alibaba.fastjson2.JSON;
import com.beiktech.bontolink.data.mapper.DomainTermMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 领域术语服务
 */
@Slf4j
@Service
public class DomainTermService {

    @Autowired
    private DomainTermMapper domainTermMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public List<Map<String, Object>> listAll() {
        return domainTermMapper.listAll();
    }

    public Map<String, Object> findById(String id) {
        return domainTermMapper.findById(id);
    }

    @Transactional
    public String create(Map<String, Object> data) {
        String id = "dt-" + UUID.randomUUID().toString().substring(0, 8);
        data.put("id", id);

        if (data.get("similarity") == null) {
            data.put("similarity", 0.9);
        }
        if (data.get("usage_count") == null) {
            data.put("usage_count", 0);
        }
        if (data.get("source") == null) {
            data.put("source", "MANUAL");
        }

        // common_terms 转 JSON
        Object commonTerms = data.get("common_terms");
        if (commonTerms instanceof List) {
            data.put("common_terms", JSON.toJSONString(commonTerms));
        }

        domainTermMapper.insert(data);
        log.info("创建领域术语: standard_term={}, id={}", data.get("standard_term"), id);
        notifyChange("CREATE", id);
        return id;
    }

    @Transactional
    public void update(String id, Map<String, Object> data) {
        data.put("id", id);

        Object commonTerms = data.get("common_terms");
        if (commonTerms instanceof List) {
            data.put("common_terms", JSON.toJSONString(commonTerms));
        }

        domainTermMapper.update(data);
        log.info("更新领域术语: id={}", id);
        notifyChange("UPDATE", id);
    }

    @Transactional
    public void delete(String id) {
        domainTermMapper.deleteById(id);
        log.info("删除领域术语: id={}", id);
        notifyChange("DELETE", id);
    }

    private void notifyChange(String action, Object detail) {
        if (redisTemplate == null) return;

        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "DOMAIN_TERM_CHANGED");
            message.put("action", action);
            message.put("detail", detail);
            message.put("timestamp", System.currentTimeMillis());
            message.put("source", "bontolink-admin");

            redisTemplate.convertAndSend("ontology-events", JSON.toJSONString(message));
        } catch (Exception e) {
            log.warn("发送 Redis 消息失败", e);
        }
    }
}
