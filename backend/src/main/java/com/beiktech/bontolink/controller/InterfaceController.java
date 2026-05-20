package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.service.InterfaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interface")
public class InterfaceController {

    @Autowired private InterfaceService service;

    @GetMapping
    public R<List<Map<String, Object>>> list() { return R.ok(service.list()); }

    @GetMapping("/{id}")
    public R<Map<String, Object>> get(@PathVariable String id) { return R.ok(service.get(id)); }

    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        body.remove("id");
        return R.ok(service.save(body));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return R.ok(service.save(body));
    }

    @PostMapping("/{id}/toggle")
    public R<?> toggle(@PathVariable String id) {
        service.toggleStatus(id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        service.delete(id);
        return R.ok();
    }

    /* ---------- properties ---------- */

    @GetMapping("/{id}/properties")
    public R<List<Map<String, Object>>> listProperties(@PathVariable String id) {
        return R.ok(service.properties(id));
    }

    @PostMapping("/{id}/properties")
    public R<Map<String, Object>> addProperty(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return R.ok(service.addProperty(id, body));
    }

    @PutMapping("/properties/{propId}")
    public R<Map<String, Object>> updateProperty(@PathVariable String propId, @RequestBody Map<String, Object> body) {
        body.put("id", propId);
        return R.ok(service.updateProperty(body));
    }

    @DeleteMapping("/properties/{propId}")
    public R<?> deleteProperty(@PathVariable String propId) {
        service.deleteProperty(propId);
        return R.ok();
    }

    /* ---------- implementers ---------- */

    @GetMapping("/{id}/implementers")
    public R<List<Map<String, Object>>> implementers(@PathVariable String id) {
        return R.ok(service.implementers(id));
    }

    @PostMapping("/{id}/implementers")
    public R<?> addImpl(@PathVariable String id, @RequestBody Map<String, String> body) {
        service.addImpl(id, body.get("classId"), body.get("categoryCode"));
        return R.ok();
    }

    @DeleteMapping("/{id}/implementers/{classId}")
    public R<?> removeImpl(@PathVariable String id, @PathVariable String classId) {
        service.removeImpl(id, classId);
        return R.ok();
    }
}
