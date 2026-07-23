package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.service.InterfaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 接口（Interface）资源管理：接口定义、属性、实现类绑定的 CRUD */
@RestController
@RequestMapping("/api/interface")
public class InterfaceController {

    @Autowired private InterfaceService service;

    /** 查询所有接口列表 */
    @GetMapping
    public R<List<Map<String, Object>>> list() { return R.ok(service.list()); }

    /** 按 id 获取接口详情 */
    @GetMapping("/{id}")
    public R<Map<String, Object>> get(@PathVariable String id) { return R.ok(service.get(id)); }

    /** 新建接口；id 由 service 生成，前端传入的 id 字段强制移除 */
    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        body.remove("id");
        return R.ok(service.save(body));
    }

    /** 更新接口基础信息；路径 id 注入 body 后统一走 save */
    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return R.ok(service.save(body));
    }

    /** 切换接口启用/禁用状态（active ↔ inactive） */
    @PostMapping("/{id}/toggle")
    public R<?> toggle(@PathVariable String id) {
        service.toggleStatus(id);
        return R.ok();
    }

    /** 删除接口及其关联属性和实现类绑定 */
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        service.delete(id);
        return R.ok();
    }

    /* ---------- properties ---------- */

    /** 查询接口下的属性列表 */
    @GetMapping("/{id}/properties")
    public R<List<Map<String, Object>>> listProperties(@PathVariable String id) {
        return R.ok(service.properties(id));
    }

    /** 向接口添加属性；属性 id 由 service 生成 */
    @PostMapping("/{id}/properties")
    public R<Map<String, Object>> addProperty(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return R.ok(service.addProperty(id, body));
    }

    /** 更新接口属性；路径 propId 注入 body 后由 service 定位更新 */
    @PutMapping("/properties/{propId}")
    public R<Map<String, Object>> updateProperty(@PathVariable String propId, @RequestBody Map<String, Object> body) {
        body.put("id", propId);
        return R.ok(service.updateProperty(body));
    }

    /** 删除接口的某个属性 */
    @DeleteMapping("/properties/{propId}")
    public R<?> deleteProperty(@PathVariable String propId) {
        service.deleteProperty(propId);
        return R.ok();
    }

    /* ---------- implementers ---------- */

    /** 查询实现该接口的对象类列表 */
    @GetMapping("/{id}/implementers")
    public R<List<Map<String, Object>>> implementers(@PathVariable String id) {
        return R.ok(service.implementers(id));
    }

    /** 为接口绑定实现类；body 含 classId（对象类 id）和 categoryCode（所属行业分类） */
    @PostMapping("/{id}/implementers")
    public R<?> addImpl(@PathVariable String id, @RequestBody Map<String, String> body) {
        service.addImpl(id, body.get("classId"), body.get("categoryCode"));
        return R.ok();
    }

    /** 解除接口与指定实现类的绑定 */
    @DeleteMapping("/{id}/implementers/{classId}")
    public R<?> removeImpl(@PathVariable String id, @PathVariable String classId) {
        service.removeImpl(id, classId);
        return R.ok();
    }
}
