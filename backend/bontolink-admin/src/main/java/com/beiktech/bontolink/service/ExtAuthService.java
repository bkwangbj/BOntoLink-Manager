package com.beiktech.bontolink.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 外部数据源鉴权装配 — 把 auth_type / auth_config 翻译成实际的请求头或查询参数。
 *
 * 生效范围与前端 authModel.js 的 9 种类型一一对应。其中 oauth2_code / cas / digest
 * 需要浏览器跳转或 401 挑战应答, 在线调试拿不到凭证, 只回 note 提示而不阻断请求。
 */
@Service
public class ExtAuthService {

    /** 装配结果: note 非空时前端展示提示条, applied 说明实际用了哪种鉴权 */
    public static class Result {
        public String applied = "none";
        public String note;
        public boolean authFailed;   // 取令牌失败, 调用方应记 call_status=4
    }

    /** OAuth2 客户端模式的令牌缓存, key = tokenUrl|clientId */
    private static final Map<String, Token> TOKEN_CACHE = new ConcurrentHashMap<>();
    private record Token(String value, long expireAt) {}

    /**
     * @param ds      数据源行
     * @param api     接口行, override_auth=1 时用接口自身的鉴权配置
     * @param headers 就地写入请求头
     * @param query   就地写入查询参数
     */
    public Result apply(Map<String, Object> ds, Map<String, Object> api,
                        Map<String, String> headers, Map<String, String> query) {
        Result r = new Result();
        boolean override = api != null && intOf(api.get("override_auth"), 0) == 1;
        String type = str(override ? api.get("auth_type") : ds.get("auth_type"));
        String cfgJson = str(override ? api.get("auth_config") : ds.get("auth_config"));
        if (type.isEmpty()) type = "none";
        r.applied = type + (override ? "(接口覆盖)" : "");
        if ("none".equals(type)) { r.applied = "none"; return r; }

        JSONObject c;
        try { c = cfgJson.isEmpty() ? new JSONObject() : JSON.parseObject(cfgJson); }
        catch (Exception e) { r.note = "鉴权配置不是合法 JSON, 已跳过鉴权"; return r; }

        switch (type) {
            case "apikey" -> {
                String name = s(c, "key_name", "X-API-Key");
                String val = join(s(c, "key_prefix", ""), s(c, "key_value", ""));
                put(headers, query, s(c, "param_position", "header"), name, val);
            }
            case "basic" -> {
                String u = s(c, "username", ""), p = s(c, "password", "");
                /* auto_encode=0 表示密码栏里存的已是编好的凭证串, 直接透传 */
                String cred = intOf(c.get("auto_encode"), 1) == 1
                    ? Base64.getEncoder().encodeToString((u + ":" + p).getBytes(StandardCharsets.UTF_8)) : p;
                headers.put("Authorization", "Basic " + cred);
            }
            case "bearer" -> {
                String tok = s(c, "token_value", "");
                if (intOf(c.get("auto_renew"), 0) == 1) r.note = "「自动续期」暂未接入, 本次直接使用已配置的 Token";
                placeToken(headers, query, s(c, "param_position", "header"), s(c, "token_prefix", "Bearer"), tok);
            }
            case "jwt" -> {
                String alg = s(c, "sign_algorithm", "HS256");
                if (!alg.startsWith("HS")) { r.note = alg + " 需要非对称私钥签名, 在线调试暂只支持 HS256/HS384/HS512"; return r; }
                String tok;
                try { tok = signJwt(alg, s(c, "sign_key", ""), s(c, "issuer", ""), s(c, "audience", ""), intOf(c.get("expire_time"), 3600)); }
                catch (Exception e) { r.authFailed = true; r.note = "JWT 签发失败: " + e.getMessage(); return r; }
                String pos = s(c, "token_position", "header");
                if ("cookie".equals(pos)) headers.put("Cookie", "token=" + tok);
                else placeToken(headers, query, pos, s(c, "token_prefix", "Bearer"), tok);
            }
            case "oauth2_client" -> {
                String tok;
                try { tok = clientCredentialsToken(c); }
                catch (Exception e) { r.authFailed = true; r.note = "获取 Token 失败: " + e.getMessage(); return r; }
                placeToken(headers, query, s(c, "token_position", "header"), s(c, "token_prefix", "Bearer"), tok);
            }
            case "oauth2_code" -> r.note = "授权码模式需要用户在浏览器完成授权, 在线调试无法自动取得令牌, 本次未附带鉴权信息";
            case "cas" -> r.note = "CAS 单点登录需要跳转登录页换取 Ticket, 在线调试无法自动完成, 本次未附带鉴权信息";
            case "digest" -> r.note = "Digest 摘要认证需要先接收服务端 401 挑战再应答, 在线调试暂不支持, 本次未附带鉴权信息";
            default -> r.note = "未知鉴权类型: " + type;
        }
        return r;
    }

    /* ===== OAuth2 客户端模式 ===== */

    private String clientCredentialsToken(JSONObject c) throws Exception {
        String tokenUrl = s(c, "token_url", "");
        String clientId = s(c, "client_id", ""), secret = s(c, "client_secret", "");
        if (tokenUrl.isEmpty()) throw new IllegalStateException("未配置 Token 获取地址");

        String key = tokenUrl + "|" + clientId;
        Token cached = TOKEN_CACHE.get(key);
        if (cached != null && cached.expireAt > System.currentTimeMillis() + 5000) return cached.value;

        StringBuilder form = new StringBuilder("grant_type=client_credentials");
        Object scope = c.get("scope");
        String scopeStr = scope instanceof List<?> l ? String.join(" ", l.stream().map(String::valueOf).toList()) : str(scope);
        if (!scopeStr.isEmpty()) form.append("&scope=").append(enc(scopeStr));

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(tokenUrl))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/x-www-form-urlencoded");
        if ("body".equals(s(c, "auth_method", "basic"))) {
            form.append("&client_id=").append(enc(clientId)).append("&client_secret=").append(enc(secret));
        } else {
            b.header("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString((clientId + ":" + secret).getBytes(StandardCharsets.UTF_8)));
        }
        b.POST(HttpRequest.BodyPublishers.ofString(form.toString()));

        HttpResponse<String> resp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
            .send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) throw new IllegalStateException("HTTP " + resp.statusCode() + " " + cut(resp.body()));

        JSONObject j = JSON.parseObject(resp.body());
        String token = j == null ? null : j.getString("access_token");
        if (token == null || token.isBlank()) throw new IllegalStateException("响应中没有 access_token: " + cut(resp.body()));

        int ttl = j.getIntValue("expires_in", intOf(c.get("expire_time"), 7200));
        TOKEN_CACHE.put(key, new Token(token, System.currentTimeMillis() + ttl * 1000L));
        return token;
    }

    /* ===== JWT 自签发 ===== */

    private static String signJwt(String alg, String key, String iss, String aud, int ttlSec) throws Exception {
        if (key.isBlank()) throw new IllegalStateException("签名密钥为空");
        String mac = switch (alg) { case "HS384" -> "HmacSHA384"; case "HS512" -> "HmacSHA512"; default -> "HmacSHA256"; };
        long now = System.currentTimeMillis() / 1000;
        Map<String, Object> claims = new LinkedHashMap<>();
        if (!iss.isBlank()) claims.put("iss", iss);
        if (!aud.isBlank()) claims.put("aud", aud);
        claims.put("iat", now);
        claims.put("exp", now + Math.max(60, ttlSec));

        String head = b64u(("{\"alg\":\"" + alg + "\",\"typ\":\"JWT\"}").getBytes(StandardCharsets.UTF_8));
        String payload = b64u(JSON.toJSONString(claims).getBytes(StandardCharsets.UTF_8));
        String signing = head + "." + payload;

        Mac m = Mac.getInstance(mac);
        m.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), mac));
        return signing + "." + b64u(m.doFinal(signing.getBytes(StandardCharsets.UTF_8)));
    }

    /* ===== 小工具 ===== */

    private static void placeToken(Map<String, String> headers, Map<String, String> query,
                                   String position, String prefix, String token) {
        if ("query".equals(position)) query.put("access_token", token);
        else headers.put("Authorization", join(prefix, token));
    }
    private static void put(Map<String, String> headers, Map<String, String> query,
                            String position, String name, String value) {
        if ("query".equals(position)) query.put(name, value); else headers.put(name, value);
    }
    private static String join(String prefix, String value) {
        String p = str(prefix);
        return p.isEmpty() ? value : p + " " + value;
    }
    private static String b64u(byte[] b) { return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    private static String s(JSONObject c, String k, String def) {
        String v = c.getString(k);
        return v == null || v.isBlank() ? def : v.trim();
    }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
    private static int intOf(Object o, int def) {
        try { return o == null ? def : Integer.parseInt(String.valueOf(o).trim()); } catch (Exception e) { return def; }
    }
    private static String enc(String s) { return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8); }
    private static String cut(String s) { return s == null ? "" : (s.length() <= 200 ? s : s.substring(0, 200) + "…"); }
}
