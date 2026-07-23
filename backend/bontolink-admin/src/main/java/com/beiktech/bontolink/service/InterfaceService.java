package com.beiktech.bontolink.service;

import com.beiktech.bontolink.data.mapper.InterfaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 接口(ont_interface)管理服务：接口的增删改查、状态切换、属性维护及实现类关联管理。
 */
@Service
public class InterfaceService {

    @Autowired private InterfaceMapper mapper;

    /** 查询所有接口，按 update_time 降序。 */
    public List<Map<String, Object>> list() { return mapper.listAll(); }

    /** 按 id 查单条接口。 */
    public Map<String, Object> get(String id) { return mapper.findById(id); }

    /**
     * 新增或更新接口（依据 body 中是否含有效 id 判断）。
     * <p>新增时自动生成 {@code id}(格式 {@code interface-{UUID}})、{@code rid} 和默认 {@code status=1}。</p>
     *
     * @return 操作后从数据库重新读取的最新记录
     */
    @Transactional
    public Map<String, Object> save(Map<String, Object> body) {
        String id = (String) body.get("id");
        if (id == null || id.isEmpty()) {
            // 新增：生成主键及 RID
            id = "interface-" + UUID.randomUUID();
            body.put("id", id);
            body.putIfAbsent("rid", "ri.ont.interface." + id.replaceFirst("^interface-", ""));
            body.putIfAbsent("status", 1);
            mapper.insert(body);
        } else {
            mapper.update(body);
        }
        return mapper.findById(id);
    }

    /**
     * 切换接口启用/禁用状态（1→0 或 0→1）。
     *
     * @throws IllegalArgumentException 接口 id 不存在时抛出
     */
    public void toggleStatus(String id) {
        Map<String, Object> row = mapper.findById(id);
        if (row == null) throw new IllegalArgumentException("接口不存在");
        int curr = ((Number) row.get("status")).intValue();
        mapper.updateStatus(id, curr == 1 ? 0 : 1);
    }

    /**
     * 删除接口及其所有属性、实现类关联（级联删除，事务保证原子性）。
     */
    @Transactional
    public void delete(String id) {
        mapper.deletePropertiesByInterface(id);   // 先删属性，再删实现类关联
        mapper.deleteImplByInterface(id);
        mapper.delete(id);
    }

    /* ---- properties ---- */

    /** 查询指定接口的所有属性定义。 */
    public List<Map<String, Object>> properties(String interfaceId) {
        return mapper.listProperties(interfaceId);
    }

    /**
     * 向接口新增一条属性定义。
     * <p>主键格式 {@code interface-pro-{UUID}}；默认 {@code status=1, is_required=0}。
     * 返回写入后的 body（不重查数据库）。</p>
     */
    public Map<String, Object> addProperty(String interfaceId, Map<String, Object> body) {
        body.put("interface_id", interfaceId);
        String id = "interface-pro-" + UUID.randomUUID();
        body.put("id", id);
        body.putIfAbsent("rid", "ri.ont.interface.property." + id);
        body.putIfAbsent("status", 1);
        body.putIfAbsent("is_required", 0);
        mapper.insertProperty(body);
        return body;
    }

    /**
     * 更新接口属性定义（body 需含 id）。返回更新后的 body（不重查数据库）。
     */
    public Map<String, Object> updateProperty(Map<String, Object> body) {
        mapper.updateProperty(body);
        return body;
    }

    /** 删除单条接口属性定义。 */
    public void deleteProperty(String id) { mapper.deleteProperty(id); }

    /* ---- implementers ---- */

    /** 查询实现了该接口的对象类型列表（ont_interface_class 关联表）。 */
    public List<Map<String, Object>> implementers(String interfaceId) {
        return mapper.listImplementers(interfaceId);
    }

    /**
     * 将对象类型 classId 关联到接口（添加实现关系）。已存在则幂等跳过。
     *
     * @param categoryCode 对象类型所属行业分类，用于关联表冗余字段
     */
    public void addImpl(String interfaceId, String classId, String categoryCode) {
        if (mapper.existsImpl(interfaceId, classId) > 0) return; // 幂等：已关联则跳过
        mapper.addImpl("interface-class-" + UUID.randomUUID(), interfaceId, classId, categoryCode);
    }

    /** 解除对象类型 classId 与接口的实现关系。 */
    public void removeImpl(String interfaceId, String classId) {
        mapper.removeImpl(interfaceId, classId);
    }
}
