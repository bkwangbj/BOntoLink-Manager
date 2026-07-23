package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.data.entity.SysDataSource;
import com.beiktech.bontolink.service.DataSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 外部数据源管理接口：支持数据源的 CRUD、连通性测试、运行状态监控及连接池管理。
 */
@RestController
@RequestMapping("/api/datasource")
public class DataSourceController {

    @Autowired private DataSourceService service;

    /** 查询所有数据源列表 */
    @GetMapping
    public R<List<SysDataSource>> list() { return R.ok(service.list()); }

    /** 获取指定数据源详情 */
    @GetMapping("/{id}")
    public R<SysDataSource> get(@PathVariable String id) { return R.ok(service.get(id)); }

    /** 获取数据源统计概览（总数/启用数/各类型分布等） */
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() { return R.ok(service.overview()); }

    /** 获取系统支持的 JDBC 驱动目录，按数据库类型分组 */
    @GetMapping("/drivers")
    public R<Map<String, List<String>>> drivers() { return R.ok(service.driverCatalog()); }

    /** 新建数据源，id 由 service 层生成 */
    @PostMapping
    public R<SysDataSource> create(@RequestBody SysDataSource d) { return R.ok(service.save(d)); }

    /** 更新指定数据源的连接信息/名称/描述等 */
    @PutMapping("/{id}")
    public R<SysDataSource> update(@PathVariable String id, @RequestBody SysDataSource d) {
        d.setId(id);
        return R.ok(service.save(d));
    }

    /** 删除指定数据源 */
    @DeleteMapping("/{id}")
    public R<?> remove(@PathVariable String id) {
        service.delete(id);
        return R.ok();
    }

    /** 切换数据源启用/禁用状态 */
    @PostMapping("/{id}/toggle")
    public R<?> toggle(@PathVariable String id) {
        service.toggleStatus(id);
        return R.ok();
    }

    /** 测试指定数据源的数据库连接是否可达，返回测试结果和耗时 */
    @PostMapping("/{id}/test")
    public R<Map<String, Object>> test(@PathVariable String id) {
        return R.ok(service.testConnection(id));
    }

    /** 获取指定数据源的运行监控指标（连接池状态、最近同步时间等） */
    @GetMapping("/{id}/monitor")
    public R<Map<String, Object>> monitor(@PathVariable String id) {
        return R.ok(service.monitor(id));
    }

    // ========== 连接池管理端点 ==========

    /** 获取数据源连接池实时运行时指标（MXBean 数据） */
    @GetMapping("/{id}/pool")
    public R<Map<String, Object>> poolMetrics(@PathVariable String id) {
        return R.ok(service.getPoolMetrics(id));
    }

    /** 热刷新连接池（销毁 + 用当前配置重建） */
    @PostMapping("/{id}/pool/refresh")
    public R<?> refreshPool(@PathVariable String id) {
        service.refreshPool(id);
        return R.ok();
    }

    /** 动态调整连接池最大大小，body 格式: {"maxPoolSize": 20} */
    @PostMapping("/{id}/pool/resize")
    public R<?> resizePool(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        Integer newMax = body.get("maxPoolSize");
        if (newMax == null || newMax <= 0) {
            return R.error(400, "maxPoolSize 必须 > 0");
        }
        service.resizePool(id, newMax);
        return R.ok();
    }
}
