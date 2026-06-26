package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.entity.BizNamespace;
import com.beiktech.bontolink.entity.BizNamespaceVersion;
import com.beiktech.bontolink.service.NamespaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务命名空间管理接口：命名空间的 CRUD 及版本管理
 */
@RestController
@RequestMapping("/api/namespace")
public class NamespaceController {
    @Autowired private NamespaceService service;

    /** 查询所有业务命名空间列表 */
    @GetMapping
    public R<List<BizNamespace>> list() { return R.ok(service.list()); }

    /** 按命名空间编码获取详情（code 为业务主键） */
    @GetMapping("/{code}")
    public R<BizNamespace> get(@PathVariable String code) { return R.ok(service.get(code)); }

    /** 新建命名空间，nsCode 由调用方传入或 service 层生成 */
    @PostMapping
    public R<BizNamespace> create(@RequestBody BizNamespace ns) { return R.ok(service.create(ns)); }

    /** 更新指定命名空间的名称/描述等信息；路径 code 覆盖 body 中的 nsCode */
    @PutMapping("/{code}")
    public R<BizNamespace> update(@PathVariable String code, @RequestBody BizNamespace ns) {
        ns.setNsCode(code);
        return R.ok(service.update(ns));
    }

    /** 查询指定命名空间的所有版本列表 */
    @GetMapping("/{code}/versions")
    public R<List<BizNamespaceVersion>> versions(@PathVariable String code) {
        return R.ok(service.listVersions(code));
    }

    /** 为命名空间新建版本快照 */
    @PostMapping("/versions")
    public R<BizNamespaceVersion> createVersion(@RequestBody BizNamespaceVersion v) {
        return R.ok(service.createVersion(v));
    }

    /** 将指定版本设为当前生效版本（同时将同命名空间其他版本置为非当前） */
    @PostMapping("/versions/{id}/current")
    public R<BizNamespaceVersion> setCurrentVersion(@PathVariable String id) {
        return R.ok(service.setCurrentVersion(id));
    }

    /** 删除指定命名空间版本 */
    @DeleteMapping("/versions/{id}")
    public R<?> deleteVersion(@PathVariable String id) {
        service.deleteVersion(id);
        return R.ok();
    }
}
