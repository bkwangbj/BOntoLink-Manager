package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.data.mapper.TcEnumMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 类型类统一枚举字典接口。路径:/api/tc-enum */
@RestController
@RequestMapping("/api/tc-enum")
public class TcEnumController {

    @Autowired private TcEnumMapper mapper;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private String now() { return LocalDateTime.now().format(TS); }

    /** 全部枚举名 */
    @GetMapping("/names")
    public R<List<String>> names() { return R.ok(mapper.listEnumNames()); }

    /** 某枚举的可选项(onlyEnabled=true 仅启用,供下拉) */
    @GetMapping
    public R<List<Map<String, Object>>> list(@RequestParam String enumName,
                                             @RequestParam(required = false, defaultValue = "false") boolean onlyEnabled) {
        List<Map<String, Object>> rows = mapper.listByName(enumName);
        if (onlyEnabled) rows.removeIf(r -> toInt(r.get("status")) != 1);
        return R.ok(rows);
    }

    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        if (body.get("enum_name") == null || String.valueOf(body.get("enum_name")).isBlank()) return R.error(400, "enum_name 必填");
        if (body.get("code") == null || String.valueOf(body.get("code")).isBlank()) return R.error(400, "code 必填");
        String id = "tcdic-" + UUID.randomUUID().toString().substring(0, 8);
        body.put("id", id);
        body.putIfAbsent("name", body.get("code"));
        body.putIfAbsent("sort_no", 0);
        body.putIfAbsent("status", 1);
        mapper.insert(body);
        return R.ok(mapper.findById(id));
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Map<String, Object> exist = mapper.findById(id);
        if (exist == null) return R.error(404, "枚举项不存在");
        Map<String, Object> merged = new HashMap<>(exist);
        merged.putAll(body);
        merged.put("id", id);
        merged.put("sort_no", toInt(merged.getOrDefault("sort_no", 0)));
        merged.put("status", toInt(merged.getOrDefault("status", 1)));
        merged.put("updated_at", now());
        mapper.update(merged);
        return R.ok(mapper.findById(id));
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) { mapper.delete(id); return R.ok(); }

    /** 批量重排序:body = [{id, sort_no}, ...] */
    @PostMapping("/reorder")
    public R<?> reorder(@RequestBody List<Map<String, Object>> items) {
        for (Map<String, Object> it : items) {
            Map<String, Object> row = mapper.findById(String.valueOf(it.get("id")));
            if (row == null) continue;
            row.put("sort_no", toInt(it.get("sort_no")));
            row.put("status", toInt(row.getOrDefault("status", 1)));
            row.put("updated_at", now());
            mapper.update(row);
        }
        return R.ok();
    }

    private static int toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof Boolean b) return b ? 1 : 0;
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }
}
