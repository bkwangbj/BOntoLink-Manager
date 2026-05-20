package com.beiktech.bontolink.service;

import com.beiktech.bontolink.mapper.InterfaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class InterfaceService {

    @Autowired private InterfaceMapper mapper;

    public List<Map<String, Object>> list() { return mapper.listAll(); }

    public Map<String, Object> get(String id) { return mapper.findById(id); }

    @Transactional
    public Map<String, Object> save(Map<String, Object> body) {
        String id = (String) body.get("id");
        if (id == null || id.isEmpty()) {
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

    public void toggleStatus(String id) {
        Map<String, Object> row = mapper.findById(id);
        if (row == null) throw new IllegalArgumentException("接口不存在");
        int curr = ((Number) row.get("status")).intValue();
        mapper.updateStatus(id, curr == 1 ? 0 : 1);
    }

    @Transactional
    public void delete(String id) {
        mapper.deletePropertiesByInterface(id);
        mapper.deleteImplByInterface(id);
        mapper.delete(id);
    }

    /* ---- properties ---- */

    public List<Map<String, Object>> properties(String interfaceId) {
        return mapper.listProperties(interfaceId);
    }

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

    public Map<String, Object> updateProperty(Map<String, Object> body) {
        mapper.updateProperty(body);
        return body;
    }

    public void deleteProperty(String id) { mapper.deleteProperty(id); }

    /* ---- implementers ---- */

    public List<Map<String, Object>> implementers(String interfaceId) {
        return mapper.listImplementers(interfaceId);
    }

    public void addImpl(String interfaceId, String classId, String categoryCode) {
        if (mapper.existsImpl(interfaceId, classId) > 0) return;
        mapper.addImpl("interface-class-" + UUID.randomUUID(), interfaceId, classId, categoryCode);
    }

    public void removeImpl(String interfaceId, String classId) {
        mapper.removeImpl(interfaceId, classId);
    }
}
