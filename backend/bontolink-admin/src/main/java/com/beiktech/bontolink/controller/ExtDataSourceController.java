package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.data.mapper.ExtDataSourceMapper;
import com.beiktech.bontolink.service.ExtAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

/**
 * 外部数据源(HTTP 接口类)管理接口。
 * 数据库类数据源仍走 /api/datasource，两者在前端数据源列表页合并展示。
 */
@RestController
@RequestMapping("/api/ext-datasource")
public class ExtDataSourceController {

    @Autowired private ExtDataSourceMapper mapper;
    @Autowired private ExtAuthService authService;

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
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            out.put("ok", false);
            out.put("message", "基础地址必须以 http:// 或 https:// 开头");
            return R.ok(out);
        }
        /* 只探基础地址可达性: 任何 HTTP 响应码都算连通, 业务码由具体接口调试判定 */
        long t0 = System.currentTimeMillis();
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(intOf(ds.get("connect_timeout"), 5000)))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(base))
                .timeout(Duration.ofMillis(intOf(ds.get("read_timeout"), 10000)))
                .method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
            long ms = System.currentTimeMillis() - t0;
            out.put("ok", true);
            out.put("message", "连接成功 · HTTP " + resp.statusCode() + " · " + ms + "ms");
        } catch (HttpTimeoutException e) {
            out.put("ok", false);
            out.put("message", "连接超时(" + (System.currentTimeMillis() - t0) + "ms)");
        } catch (Exception e) {
            out.put("ok", false);
            out.put("message", "连接失败: " + e.getMessage());
        }
        return R.ok(out);
    }

    /* ===== 接口分组 ===== */

    @GetMapping("/{id}/groups")
    public R<List<Map<String, Object>>> groups(@PathVariable String id) { return R.ok(mapper.listGroups(id)); }

    @PostMapping("/{id}/groups")
    public R<Map<String, Object>> createGroup(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", "ext_group-" + UUID.randomUUID());
        row.put("ds_id", id);
        row.put("group_name", str(body.get("group_name")).isEmpty() ? "新建分组" : body.get("group_name"));
        row.put("parent_id", body.getOrDefault("parent_id", "0"));
        row.put("sort", body.getOrDefault("sort", 0));
        mapper.insertGroup(row);
        return R.ok(row);
    }

    @PutMapping("/groups/{groupId}")
    public R<?> updateGroup(@PathVariable String groupId, @RequestBody Map<String, Object> body) {
        Map<String, Object> row = new LinkedHashMap<>(body);
        row.put("id", groupId);
        row.putIfAbsent("parent_id", "0");
        row.putIfAbsent("sort", 0);
        mapper.updateGroup(row);
        return R.ok(null);
    }

    /** 删除分组: 组内接口挪到未分组而非一并删除 */
    @DeleteMapping("/groups/{groupId}")
    public R<?> removeGroup(@PathVariable String groupId) {
        mapper.detachInterfacesFromGroup(groupId);
        mapper.deleteGroup(groupId);
        return R.ok(null);
    }

    /* ===== 接口定义 ===== */

    @GetMapping("/{id}/interfaces")
    public R<List<Map<String, Object>>> interfaces(@PathVariable String id) { return R.ok(mapper.listInterfaces(id)); }

    @PostMapping("/{id}/interfaces")
    public R<Map<String, Object>> createInterface(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Map<String, Object> row = normalizeInterface(body);
        row.put("ds_id", id);
        String code = str(row.get("api_code"));
        if (code.isEmpty()) return R.error(400, "接口编码必填");
        if (mapper.countInterfaceByCode(id, code, "") > 0) return R.error(400, "接口编码已存在: " + code);
        String apiId = "ext_api-" + UUID.randomUUID();
        row.put("id", apiId);
        mapper.insertInterface(row);
        return R.ok(mapper.findInterface(apiId));
    }

    @PutMapping("/interfaces/{apiId}")
    public R<Map<String, Object>> updateInterface(@PathVariable String apiId, @RequestBody Map<String, Object> body) {
        Map<String, Object> old = mapper.findInterface(apiId);
        if (old == null) return R.error(404, "接口不存在");
        Map<String, Object> row = normalizeInterface(body);
        row.put("id", apiId);
        row.put("ds_id", old.get("ds_id"));
        String code = str(row.get("api_code"));
        if (code.isEmpty()) return R.error(400, "接口编码必填");
        if (mapper.countInterfaceByCode(str(old.get("ds_id")), code, apiId) > 0) return R.error(400, "接口编码已存在: " + code);
        mapper.updateInterface(row);
        return R.ok(mapper.findInterface(apiId));
    }

    @DeleteMapping("/interfaces/{apiId}")
    public R<?> removeInterface(@PathVariable String apiId) { mapper.deleteInterface(apiId); return R.ok(null); }

    /**
     * 在线调试: 按数据源全局配置 + 接口自身配置拼出请求并真实发起, 结果写入调用日志。
     * body 可覆盖 method / path / headers / query / body, 供调试面板未保存时试跑。
     */
    @PostMapping("/interfaces/{apiId}/send")
    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> send(@PathVariable String apiId, @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> api = mapper.findInterface(apiId);
        if (api == null) return R.error(404, "接口不存在");
        Map<String, Object> ds = mapper.findById(str(api.get("ds_id")));
        if (ds == null) return R.error(404, "数据源不存在");

        Map<String, Object> req = body == null ? Map.of() : body;
        String method = str(req.getOrDefault("method", api.getOrDefault("method", "GET"))).toUpperCase();
        String path = str(req.getOrDefault("path", api.getOrDefault("api_path", "")));

        Map<String, String> headers = new LinkedHashMap<>();
        if (Integer.parseInt(str(api.getOrDefault("header_inherit", "1"))) == 1) {
            headers.putAll(parseHeaderJson(str(ds.get("global_header"))));
        }
        headers.putAll((Map<String, String>) req.getOrDefault("headers", Map.of()));
        Map<String, String> query = new LinkedHashMap<>((Map<String, String>) req.getOrDefault("query", Map.of()));

        /* 调试面板可能改了鉴权还没保存, 以传入值为准, 保证所见即所得 */
        Map<String, Object> authApi = new LinkedHashMap<>(api);
        if (req.containsKey("overrideAuth")) {
            authApi.put("override_auth", intOf(req.get("overrideAuth"), 0));
            authApi.put("auth_type", req.get("authType"));
            authApi.put("auth_config", req.get("authConfig"));
        }
        /* 鉴权可能往 header 或 query 里塞东西, 必须在拼 URL 之前完成 */
        ExtAuthService.Result auth = authService.apply(ds, authApi, headers, query);

        String url = joinUrl(str(ds.get("base_url")), path);
        if (!query.isEmpty()) {
            StringBuilder qs = new StringBuilder(url.contains("?") ? "&" : "?");
            query.forEach((k, v) -> qs.append(enc(k)).append('=').append(enc(v)).append('&'));
            url += qs.substring(0, qs.length() - 1);
        }

        /* 请求体: raw/urlencoded 走文本, form-data/binary 组装二进制 */
        byte[] bodyBytes;
        String bodyPreview;
        try {
            Object[] built = buildBody(str(req.getOrDefault("bodyMode", "raw")), req, headers,
                str(firstNonEmpty(api.get("content_type"), ds.get("content_type"), "application/json")));
            bodyBytes = (byte[]) built[0];
            bodyPreview = (String) built[1];
        } catch (Exception e) {
            return R.error(400, "请求体组装失败: " + e.getMessage());
        }

        int timeoutSec = intOf(api.get("timeout"), 0);
        long timeoutMs = timeoutSec > 0 ? timeoutSec * 1000L : intOf(ds.get("read_timeout"), 10000);

        Map<String, Object> out = new LinkedHashMap<>();
        long started = System.currentTimeMillis();
        int callStatus = auth.authFailed ? 4 : 1;
        int httpStatus = 0, size = 0;
        String respBody = "", errMsg = auth.authFailed ? auth.note : null;
        if (auth.authFailed) {
            /* 令牌都没取到就别发请求了, 直接按鉴权失败记录 */
            out.put("respHeaders", Map.of());
        } else
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMillis(timeoutMs));
            headers.forEach((k, v) -> { if (k != null && !k.isBlank()) b.header(k, v == null ? "" : v); });
            HttpRequest.BodyPublisher pub = bodyBytes.length == 0
                ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(bodyBytes);
            b.method(method, pub);
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(intOf(ds.get("connect_timeout"), 5000)))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpResponse<String> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
            httpStatus = resp.statusCode();
            respBody = resp.body() == null ? "" : resp.body();
            size = respBody.getBytes().length;
            if (httpStatus == 401 || httpStatus == 403) callStatus = 4;
            else if (httpStatus >= 400) callStatus = 2;
            Map<String, String> respHeaders = new LinkedHashMap<>();
            resp.headers().map().forEach((k, v) -> respHeaders.put(k, String.join(", ", v)));
            out.put("respHeaders", respHeaders);
        } catch (HttpTimeoutException e) {
            callStatus = 3; errMsg = "请求超时(" + timeoutMs + "ms)";
        } catch (Exception e) {
            callStatus = 2; errMsg = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        int cost = (int) (System.currentTimeMillis() - started);

        out.put("ok", callStatus == 1);
        out.put("httpStatus", httpStatus);
        out.put("costTime", cost);
        out.put("responseSize", size);
        out.put("body", respBody);
        out.put("errorMsg", errMsg);
        out.put("fullUrl", url);
        out.put("reqHeaders", headers);
        out.put("authApplied", auth.applied);
        out.put("authNote", auth.note);

        if (intOf(ds.get("log_enable"), 1) == 1) writeLog(ds, api, url, headers, bodyPreview, callStatus, httpStatus, cost, size, respBody, errMsg);
        return R.ok(out);
    }

    /* ===== 日志 ===== */

    @GetMapping("/{id}/logs")
    public R<Map<String, Object>> logs(@PathVariable String id,
                                      @RequestParam(required = false) String from,
                                      @RequestParam(required = false) String to,
                                      @RequestParam(required = false) String interfaceId,
                                      @RequestParam(required = false) Integer callStatus,
                                      @RequestParam(required = false) String kw,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("dsId", id); q.put("from", from); q.put("to", to);
        q.put("interfaceId", interfaceId); q.put("callStatus", callStatus); q.put("kw", kw);
        q.put("size", Math.max(1, size)); q.put("offset", Math.max(0, (page - 1) * size));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rows", mapper.pageLogs(q));
        out.put("total", mapper.countLogs(q));
        return R.ok(out);
    }

    @GetMapping("/logs/{logId}")
    public R<Map<String, Object>> logDetail(@PathVariable String logId) { return R.ok(mapper.findLog(logId)); }

    /* ===== 监控聚合 ===== */

    /** days: 统计周期天数, 前端传 7 / 30 / 90 */
    @GetMapping("/{id}/monitor")
    public R<Map<String, Object>> monitor(@PathVariable String id, @RequestParam(defaultValue = "30") int days) {
        String from = LocalDate.now().minusDays(Math.max(1, days)).toString() + " 00:00:00";
        /* 上一个同长度周期, 用于算环比 */
        String prevFrom = LocalDate.now().minusDays(Math.max(1, days) * 2L).toString() + " 00:00:00";

        Map<String, Object> cur = nz(mapper.statSummary(id, from));
        Map<String, Object> prevAll = nz(mapper.statSummary(id, prevFrom));

        long total = lng(cur.get("total")), success = lng(cur.get("success")), failed = lng(cur.get("failed"));
        double avgCost = dbl(cur.get("avg_cost"));
        /* 上一周期 = 两个周期合计 - 本周期 */
        long prevTotal = Math.max(0, lng(prevAll.get("total")) - total);
        long prevSuccess = Math.max(0, lng(prevAll.get("success")) - success);
        long prevFailed = Math.max(0, lng(prevAll.get("failed")) - failed);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", total);
        out.put("successRate", total == 0 ? 0 : Math.round(success * 1000.0 / total) / 10.0);
        out.put("avgCost", Math.round(avgCost));
        out.put("failed", failed);
        out.put("totalDelta", pct(total, prevTotal));
        out.put("successRateDelta", round1((total == 0 ? 0 : success * 100.0 / total)
            - (prevTotal == 0 ? 0 : prevSuccess * 100.0 / prevTotal)));
        out.put("failedDelta", pct(failed, prevFailed));
        out.put("trend", mapper.statTrend(id, from));
        out.put("errors", mapper.statErrors(id, from));
        out.put("top", mapper.statTop(id, from));
        return R.ok(out);
    }

    private static Map<String, Object> nz(Map<String, Object> m) { return m == null ? new LinkedHashMap<>() : m; }
    private static long lng(Object o) { try { return o == null ? 0 : Long.parseLong(String.valueOf(o).split("\\.")[0]); } catch (Exception e) { return 0; } }
    private static double dbl(Object o) { try { return o == null ? 0 : Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0; } }
    private static double round1(double v) { return Math.round(v * 10) / 10.0; }
    /** 环比百分比, 上期为 0 时不显示(返回 null) */
    private static Double pct(long cur, long prev) { return prev == 0 ? null : round1((cur - prev) * 100.0 / prev); }

    /** 单个上传文件的大小上限, 文件内容以 base64 随 JSON 上送 */
    private static final int MAX_FILE_BYTES = 10 * 1024 * 1024;

    /**
     * 按 bodyMode 组装请求体。
     * form-data 生成带 boundary 的 multipart 字节流, binary 直接发文件原始字节,
     * 其余走文本。返回 [字节体, 落日志用的可读摘要] —— 摘要里不含 base64 内容。
     */
    @SuppressWarnings("unchecked")
    private static Object[] buildBody(String bodyMode, Map<String, Object> req,
                                      Map<String, String> headers, String defaultContentType) throws Exception {
        if ("form-data".equals(bodyMode)) {
            List<Map<String, Object>> parts = (List<Map<String, Object>>) req.getOrDefault("parts", List.of());
            String boundary = "----BOntoLinkBoundary" + UUID.randomUUID().toString().replace("-", "");
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            StringBuilder preview = new StringBuilder();
            for (Map<String, Object> p : parts) {
                String name = str(p.get("name"));
                if (name.isEmpty()) continue;
                boolean isFile = "file".equals(str(p.get("type")));
                bo.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"").getBytes(StandardCharsets.UTF_8));
                if (isFile) {
                    byte[] data = decodeFile(p.get("data"));
                    String fn = str(firstNonEmpty(p.get("filename"), "file"));
                    String ct = str(firstNonEmpty(p.get("contentType"), "application/octet-stream"));
                    bo.write(("; filename=\"" + fn + "\"\r\nContent-Type: " + ct + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    bo.write(data);
                    preview.append(name).append("=[文件 ").append(fn).append(' ').append(data.length).append("B]; ");
                } else {
                    String v = str(p.get("value"));
                    bo.write("\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                    bo.write(v.getBytes(StandardCharsets.UTF_8));
                    preview.append(name).append('=').append(v).append("; ");
                }
                bo.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            bo.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            /* multipart 的 boundary 必须与实际分隔符一致, 这里强制覆盖用户填的 Content-Type */
            headers.put("Content-Type", "multipart/form-data; boundary=" + boundary);
            return new Object[]{ bo.toByteArray(), preview.toString() };
        }

        if ("binary".equals(bodyMode)) {
            Map<String, Object> f = (Map<String, Object>) req.get("file");
            if (f == null || f.get("data") == null) return new Object[]{ new byte[0], "" };
            byte[] data = decodeFile(f.get("data"));
            headers.put("Content-Type", str(firstNonEmpty(f.get("contentType"), "application/octet-stream")));
            return new Object[]{ data, "[二进制 " + str(firstNonEmpty(f.get("filename"), "file")) + " " + data.length + "B]" };
        }

        String text = str(req.getOrDefault("body", ""));
        if (!text.isEmpty()) headers.putIfAbsent("Content-Type", defaultContentType);
        return new Object[]{ text.getBytes(StandardCharsets.UTF_8), text };
    }

    /** 前端上送的是 data:URL 或裸 base64 */
    private static byte[] decodeFile(Object raw) {
        String s = str(raw);
        int comma = s.indexOf(',');
        if (s.startsWith("data:") && comma > 0) s = s.substring(comma + 1);
        byte[] data = Base64.getDecoder().decode(s);
        if (data.length > MAX_FILE_BYTES) throw new IllegalArgumentException("单个文件不能超过 10MB");
        return data;
    }

    private void writeLog(Map<String, Object> ds, Map<String, Object> api, String url, Map<String, String> headers,
                          String reqBody, int callStatus, int httpStatus, int cost, int size, String respBody, String errMsg) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("id", "ext_log-" + UUID.randomUUID());
        log.put("trace_id", "trace-" + UUID.randomUUID().toString().substring(0, 8));
        log.put("ds_id", ds.get("id"));
        log.put("interface_id", api.get("id"));
        log.put("call_type", "debug");
        log.put("caller", "在线调试");
        log.put("full_url", url);
        log.put("request_header", maskJson(headers));
        log.put("request_body", cut(reqBody, 8000));
        log.put("call_status", callStatus);
        log.put("http_status", httpStatus);
        log.put("cost_time", cost);
        log.put("response_size", size);
        log.put("response_body", cut(respBody, 8000));
        log.put("error_msg", errMsg);
        try { mapper.insertLog(log); } catch (Exception ignore) { /* 日志失败不影响调试结果 */ }
    }

    /** 敏感请求头脱敏后再落日志 */
    private static String maskJson(Map<String, String> headers) {
        StringBuilder sb = new StringBuilder("{");
        headers.forEach((k, v) -> {
            String lk = k == null ? "" : k.toLowerCase();
            boolean secret = lk.contains("authorization") || lk.contains("token") || lk.contains("key")
                || lk.contains("secret") || lk.contains("password") || lk.contains("cookie");
            sb.append('"').append(k).append("\":\"").append(secret ? "******" : v).append("\",");
        });
        if (sb.length() > 1) sb.setLength(sb.length() - 1);
        return sb.append('}').toString();
    }

    private Map<String, Object> normalizeInterface(Map<String, Object> body) {
        Map<String, Object> row = new LinkedHashMap<>();
        body.forEach((k, v) -> row.put(toSnake(k), v));
        row.putIfAbsent("group_id", "0");
        row.putIfAbsent("method", "GET");
        row.putIfAbsent("api_status", "debug");
        row.putIfAbsent("read_write_type", 1);
        row.putIfAbsent("override_auth", 0);
        row.putIfAbsent("header_inherit", 1);
        row.putIfAbsent("status", 1);
        row.putIfAbsent("sort", 0);
        for (String k : new String[]{"api_path", "description", "request_params", "response_params",
                                     "auth_type", "auth_config", "content_type", "timeout"}) {
            row.putIfAbsent(k, null);
        }
        return row;
    }

    private static Map<String, String> parseHeaderJson(String json) {
        Map<String, String> out = new LinkedHashMap<>();
        if (json == null || json.isBlank()) return out;
        String s = json.trim();
        if (s.startsWith("{")) s = s.substring(1);
        if (s.endsWith("}")) s = s.substring(0, s.length() - 1);
        for (String pair : s.split(",")) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) out.put(unquote(kv[0]), unquote(kv[1]));
        }
        return out;
    }
    private static String unquote(String s) { return s == null ? "" : s.trim().replaceAll("^\"|\"$", ""); }
    private static String joinUrl(String base, String path) {
        String b = base == null ? "" : base.replaceAll("/+$", "");
        String p = path == null ? "" : path.trim();
        if (p.isEmpty()) return b;
        return b + (p.startsWith("/") ? p : "/" + p);
    }
    private static String enc(String s) { return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8); }
    private static int intOf(Object o, int def) {
        try { return o == null ? def : Integer.parseInt(String.valueOf(o).trim()); } catch (Exception e) { return def; }
    }
    private static Object firstNonEmpty(Object... vals) {
        for (Object v : vals) if (v != null && !String.valueOf(v).isBlank()) return v;
        return "";
    }
    private static String cut(String s, int max) { return s == null ? null : (s.length() <= max ? s : s.substring(0, max) + "…(truncated)"); }

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
