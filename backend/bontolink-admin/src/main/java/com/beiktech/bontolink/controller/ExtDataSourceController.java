package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.data.mapper.ExtDataSourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 外部数据源(HTTP 接口类)管理接口。
 * 数据库类数据源仍走 /api/datasource，两者在前端数据源列表页合并展示。
 */
@RestController
@RequestMapping("/api/ext-datasource")
public class ExtDataSourceController {

    @Autowired private ExtDataSourceMapper mapper;

    /** 默认值：新建时前端未传的字段按文档规定的默认值补齐 */
    private static final Map<String, Object> DEFAULTS = Map.ofEntries(
        Map.entry("ds_type", "http_rest"), Map.entry("read_write_type", 1),
        Map.entry("default_method", "POST"), Map.entry("content_type", "application/json"),
        Map.entry("connect_timeout", 5000), Map.entry("read_timeout", 10000),
        Map.entry("retry_count", 1), Map.entry("retry_interval", 1000),
        Map.entry("ssl_verify", 1), Map.entry("log_enable", 1),
        Map.entry("header_enable", 0), Map.entry("auth_type", "none"), Map.entry("status", 1)
    );

    @GetMapping
    public R<List<Map<String, Object>>> list() { return R.ok(mapper.listAll()); }

    @GetMapping("/{id}")
    public R<Map<String, Object>> get(@PathVariable String id) { return R.ok(mapper.findById(id)); }

    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Map<String, Object> row = normalize(body);
        String code = str(row.get("ds_code"));
        if (code.isEmpty()) return R.error(400, "数据源编码必填");
        if (str(row.get("ds_name")).isEmpty()) return R.error(400, "数据源名称必填");
        if (mapper.countByCode(code, "") > 0) return R.error(400, "数据源编码已存在: " + code);
        String id = "ext_ds-" + UUID.randomUUID();
        row.put("id", id);
        mapper.insert(row);
        return R.ok(mapper.findById(id));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        if (mapper.findById(id) == null) return R.error(404, "数据源不存在");
        Map<String, Object> row = normalize(body);
        String code = str(row.get("ds_code"));
        if (code.isEmpty()) return R.error(400, "数据源编码必填");
        if (mapper.countByCode(code, id) > 0) return R.error(400, "数据源编码已存在: " + code);
        row.put("id", id);
        mapper.update(row);
        return R.ok(mapper.findById(id));
    }

    /** 删除数据源时一并清掉其下的分组与接口定义, 调用日志保留供审计 */
    @DeleteMapping("/{id}")
    public R<?> remove(@PathVariable String id) {
        mapper.deleteInterfacesByDs(id);
        mapper.deleteGroupsByDs(id);
        mapper.delete(id);
        return R.ok(null);
    }

    @PostMapping("/batch-delete")
    public R<?> batchRemove(@RequestBody Map<String, Object> body) {
        Object ids = body.get("ids");
        if (ids instanceof List<?> list) {
            for (Object o : list) {
                String id = String.valueOf(o);
                mapper.deleteInterfacesByDs(id);
                mapper.deleteGroupsByDs(id);
                mapper.delete(id);
            }
        }
        return R.ok(null);
    }

    /** 连通性测试：当前为形态校验(地址是否合法), 真正发起请求待接口执行引擎接入 */
    @PostMapping("/{id}/test")
    public R<Map<String, Object>> test(@PathVariable String id) {
        Map<String, Object> ds = mapper.findById(id);
        if (ds == null) return R.error(404, "数据源不存在");
        String base = str(ds.get("base_url"));
        Map<String, Object> out = new LinkedHashMap<>();
        boolean ok = base.startsWith("http://") || base.startsWith("https://");
        out.put("ok", ok);
        out.put("message", ok ? "基础地址格式合法(实际连通性测试待接口执行引擎接入)" : "基础地址必须以 http:// 或 https:// 开头");
        return R.ok(out);
    }

    @GetMapping("/{id}/groups")
    public R<List<Map<String, Object>>> groups(@PathVariable String id) { return R.ok(mapper.listGroups(id)); }

    @GetMapping("/{id}/interfaces")
    public R<List<Map<String, Object>>> interfaces(@PathVariable String id) { return R.ok(mapper.listInterfaces(id)); }

    /** 补默认值 + 把前端可能传来的驼峰键归一成下划线键 */
    private Map<String, Object> normalize(Map<String, Object> body) {
        Map<String, Object> row = new LinkedHashMap<>();
        body.forEach((k, v) -> row.put(toSnake(k), v));
        DEFAULTS.forEach(row::putIfAbsent);
        for (String k : new String[]{"category_code", "base_url", "global_header", "auth_config", "remark"}) {
            row.putIfAbsent(k, null);
        }
        return row;
    }

    private static String toSnake(String s) {
        return s == null ? null : s.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
}
