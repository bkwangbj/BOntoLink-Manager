package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.entity.SysDataSource;
import com.beiktech.bontolink.service.DataSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datasource")
public class DataSourceController {

    @Autowired private DataSourceService service;

    @GetMapping
    public R<List<SysDataSource>> list() { return R.ok(service.list()); }

    @GetMapping("/{id}")
    public R<SysDataSource> get(@PathVariable String id) { return R.ok(service.get(id)); }

    @GetMapping("/overview")
    public R<Map<String, Object>> overview() { return R.ok(service.overview()); }

    @GetMapping("/drivers")
    public R<Map<String, List<String>>> drivers() { return R.ok(service.driverCatalog()); }

    @PostMapping
    public R<SysDataSource> create(@RequestBody SysDataSource d) { return R.ok(service.save(d)); }

    @PutMapping("/{id}")
    public R<SysDataSource> update(@PathVariable String id, @RequestBody SysDataSource d) {
        d.setId(id);
        return R.ok(service.save(d));
    }

    @DeleteMapping("/{id}")
    public R<?> remove(@PathVariable String id) {
        service.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/toggle")
    public R<?> toggle(@PathVariable String id) {
        service.toggleStatus(id);
        return R.ok();
    }

    @PostMapping("/{id}/test")
    public R<Map<String, Object>> test(@PathVariable String id) {
        return R.ok(service.testConnection(id));
    }

    @GetMapping("/{id}/monitor")
    public R<Map<String, Object>> monitor(@PathVariable String id) {
        return R.ok(service.monitor(id));
    }
}
