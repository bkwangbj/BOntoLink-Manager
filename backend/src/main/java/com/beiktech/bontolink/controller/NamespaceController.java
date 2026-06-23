package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.entity.BizNamespace;
import com.beiktech.bontolink.entity.BizNamespaceVersion;
import com.beiktech.bontolink.service.NamespaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/namespace")
public class NamespaceController {
    @Autowired private NamespaceService service;

    @GetMapping
    public R<List<BizNamespace>> list() { return R.ok(service.list()); }

    @GetMapping("/{code}")
    public R<BizNamespace> get(@PathVariable String code) { return R.ok(service.get(code)); }

    @PostMapping
    public R<BizNamespace> create(@RequestBody BizNamespace ns) { return R.ok(service.create(ns)); }

    @PutMapping("/{code}")
    public R<BizNamespace> update(@PathVariable String code, @RequestBody BizNamespace ns) {
        ns.setNsCode(code);
        return R.ok(service.update(ns));
    }

    @GetMapping("/{code}/versions")
    public R<List<BizNamespaceVersion>> versions(@PathVariable String code) {
        return R.ok(service.listVersions(code));
    }

    @PostMapping("/versions")
    public R<BizNamespaceVersion> createVersion(@RequestBody BizNamespaceVersion v) {
        return R.ok(service.createVersion(v));
    }

    @PostMapping("/versions/{id}/current")
    public R<BizNamespaceVersion> setCurrentVersion(@PathVariable String id) {
        return R.ok(service.setCurrentVersion(id));
    }

    @DeleteMapping("/versions/{id}")
    public R<?> deleteVersion(@PathVariable String id) {
        service.deleteVersion(id);
        return R.ok();
    }
}
