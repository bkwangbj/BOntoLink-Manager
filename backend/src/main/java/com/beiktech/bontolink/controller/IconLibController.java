package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.entity.IconLibGroup;
import com.beiktech.bontolink.entity.IconLibIcon;
import com.beiktech.bontolink.service.IconLibService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 共享「我的图库」(图标目录树 + 上传 SVG)
 * 所有用户可见可操作,数据落库到 icon_lib_group / icon_lib_icon。
 */
@RestController
@RequestMapping("/api/icon-lib")
public class IconLibController {
    @Autowired private IconLibService service;

    /** 取全量:groups + icons */
    @GetMapping
    public R<Map<String, Object>> all() { return R.ok(service.all()); }

    /** 按行业/领域结构 seed 建目录(仅当表为空时,除非 force=true) */
    @PostMapping("/seed")
    public R<Map<String, Object>> seed(@RequestParam(value = "force", required = false, defaultValue = "false") boolean force) {
        return R.ok(service.seedFromCategory(force));
    }

    /** 按分组关键字批量种入演示图标(图标=0 的分组生效；force=true 覆盖) */
    @PostMapping("/seed-icons")
    public R<Map<String, Object>> seedIcons(@RequestParam(value = "force", required = false, defaultValue = "false") boolean force) {
        return R.ok(service.seedDemoIcons(force));
    }

    /* ===== Group CRUD ===== */
    @PostMapping("/groups")
    public R<IconLibGroup> createGroup(@RequestBody Map<String, Object> body) {
        String parentId = (String) body.get("parentId");
        String name = (String) body.get("name");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("分组名不能为空");
        return R.ok(service.createGroup(parentId, name.trim()));
    }

    @PutMapping("/groups/{id}")
    public R<IconLibGroup> renameGroup(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("分组名不能为空");
        return R.ok(service.renameGroup(id, name.trim()));
    }

    @DeleteMapping("/groups/{id}")
    public R<Void> deleteGroup(@PathVariable String id) {
        service.deleteGroup(id);
        return R.ok();
    }

    /* ===== Icon CRUD ===== */
    @PostMapping("/groups/{groupId}/icons")
    public R<IconLibIcon> addIcon(@PathVariable String groupId, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String viewBox = (String) body.get("viewBox");
        String content = (String) body.get("content");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("name 不能为空");
        if (content == null || content.trim().isEmpty()) throw new IllegalArgumentException("content 不能为空");
        return R.ok(service.addIcon(groupId, name.trim(), viewBox, content));
    }

    @DeleteMapping("/icons/{id}")
    public R<Void> deleteIcon(@PathVariable String id) {
        service.deleteIcon(id);
        return R.ok();
    }

    @PostMapping("/icons/batch-delete")
    public R<Void> deleteIconBatch(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        service.deleteIconBatch(ids);
        return R.ok();
    }
}
