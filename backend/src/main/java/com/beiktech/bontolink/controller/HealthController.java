package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/api/health")
    public R<Map<String, Object>> health() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("status", "UP");
        r.put("app", "BOntoLink Backend");
        r.put("version", "1.0.0");
        r.put("time", System.currentTimeMillis());
        return R.ok(r);
    }
}
