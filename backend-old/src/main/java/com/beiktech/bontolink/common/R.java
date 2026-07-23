package com.beiktech.bontolink.common;

import java.util.LinkedHashMap;

/** 统一响应封装 */
public class R<T> extends LinkedHashMap<String, Object> {
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.put("code", 0);
        r.put("msg", "ok");
        r.put("data", data);
        return r;
    }

    public static <T> R<T> ok() { return ok(null); }

    public static <T> R<T> error(int code, String msg) {
        R<T> r = new R<>();
        r.put("code", code);
        r.put("msg", msg);
        return r;
    }
}
