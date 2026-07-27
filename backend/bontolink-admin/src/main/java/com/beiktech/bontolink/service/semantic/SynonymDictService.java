package com.beiktech.bontolink.service.semantic;

import com.alibaba.fastjson2.JSON;
import com.beiktech.bontolink.data.mapper.SynonymDictMapper;
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
 * 同义词词典服务
 */
@Slf4j
@Service
public class SynonymDictService {

    @Autowired
    private SynonymDictMapper synonymDictMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /**
     * 查询列表
     */
    public List<Map<String, Object>> list(String domain) {
        if (domain != null && !domain.trim().isEmpty()) {
            return synonymDictMapper.listByDomain(domain);
        }
        return synonymDictMapper.listAll();
    }

    /**
     * 根据 ID 查询
     */
    public Map<String, Object> findById(String id) {
        return synonymDictMapper.findById(id);
    }

    /**
     * 创建
     */
    @Transactional
    public String create(Map<String, Object> data) {
        String id = "syn-" + UUID.randomUUID().toString().substring(0, 8);
        data.put("id", id);

        // 设置默认值
        if (data.get("confidence") == null) {
            data.put("confidence", 0.9);
        }
        if (data.get("source") == null) {
            data.put("source", "MANUAL");
        }
        if (data.get("usage_count") == null) {
            data.put("usage_count", 0);
        }

        // synonyms 如果是 List，转为 JSON 字符串
        Object synonyms = data.get("synonyms");
        if (synonyms instanceof List) {
            data.put("synonyms", JSON.toJSONString(synonyms));
        }

        int rows = synonymDictMapper.insert(data);
        if (rows > 0) {
            log.info("创建同义词: word={}, id={}", data.get("word"), id);
            notifyChange("CREATE", id);
            return id;
        }

        throw new RuntimeException("创建同义词失败");
    }

    /**
     * 更新
     */
    @Transactional
    public void update(String id, Map<String, Object> data) {
        data.put("id", id);

        // synonyms 转换
        Object synonyms = data.get("synonyms");
        if (synonyms instanceof List) {
            data.put("synonyms", JSON.toJSONString(synonyms));
        }

        int rows = synonymDictMapper.update(data);
        if (rows > 0) {
            log.info("更新同义词: id={}", id);
            notifyChange("UPDATE", id);
        } else {
            throw new RuntimeException("更新同义词失败，ID 不存在: " + id);
        }
    }

    /**
     * 删除
     */
    @Transactional
    public void delete(String id) {
        int rows = synonymDictMapper.deleteById(id);
        if (rows > 0) {
            log.info("删除同义词: id={}", id);
            notifyChange("DELETE", id);
        } else {
            throw new RuntimeException("删除同义词失败，ID 不存在: " + id);
        }
    }

    /**
     * 批量导入
     */
    @Transactional
    public int batchImport(List<Map<String, Object>> dataList) {
        int count = 0;
        for (Map<String, Object> data : dataList) {
            try {
                create(data);
                count++;
            } catch (Exception e) {
                log.error("导入失败: {}", data, e);
            }
        }

        log.info("批量导入完成，成功 {} 条", count);
        notifyChange("BATCH_IMPORT", count + " records");
        return count;
    }

    /**
     * 通知变更（发送 Redis 消息）
     */
    private void notifyChange(String action, Object detail) {
        if (redisTemplate == null) {
            log.debug("Redis 未配置，跳过通知");
            return;
        }

        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "SYNONYM_DICT_CHANGED");
            message.put("action", action);
            message.put("detail", detail);
            message.put("timestamp", System.currentTimeMillis());
            message.put("source", "bontolink-admin");

            redisTemplate.convertAndSend("ontology-events", JSON.toJSONString(message));
            log.debug("发送变更通知: {}", action);
        } catch (Exception e) {
            log.warn("发送 Redis 消息失败", e);
        }
    }
}
