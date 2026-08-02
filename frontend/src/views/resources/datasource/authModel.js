/* 外部数据源鉴权配置 — 9 种类型的字段定义表
 *
 * 表驱动: 新增第 10 种鉴权只在这里加一项, 表单由 AuthConfigForm 按配置动态渲染。
 * field.type: text 单行 / password 密码 / number 数字 / select 下拉 / multi 下拉多选 / switch 开关 / textarea 多行
 * field.showIf: (cfg) => boolean, 依赖其它字段的联动显示
 */

export const AUTH_TYPES = [
  { value: 'none', label: '无鉴权', desc: '公开接口、内网免认证服务' },
  { value: 'apikey', label: 'API Key', desc: '开放平台简单认证、内部系统接口' },
  { value: 'basic', label: 'Basic Auth', desc: 'HTTP 基础认证、简单系统对接' },
  { value: 'bearer', label: 'Bearer Token', desc: '标准 REST API 令牌认证' },
  { value: 'oauth2_client', label: 'OAuth2.0 客户端模式', desc: '服务间调用、机器凭证认证' },
  { value: 'oauth2_code', label: 'OAuth2.0 授权码模式', desc: '用户授权类场景、第三方登录对接' },
  { value: 'cas', label: 'CAS 单点登录', desc: '企业统一身份认证、内网 SSO 体系' },
  { value: 'jwt', label: 'JWT 令牌', desc: '自包含令牌、微服务内部认证' },
  { value: 'digest', label: 'Digest Auth', desc: '比 Basic 更安全的 HTTP 摘要认证' },
]
export const AUTH_TYPE_OPTS = AUTH_TYPES.map(t => ({ value: t.value, label: t.label }))
export const AUTH_META = Object.fromEntries(AUTH_TYPES.map(t => [t.value, t]))

const POSITION_HQ = [{ value: 'header', label: 'Header 请求头' }, { value: 'query', label: 'Query 查询参数' }]

/* 各鉴权类型的字段定义, 顺序即渲染顺序 */
export const AUTH_FIELDS = {
  none: [],

  apikey: [
    { key: 'key_name', label: 'Key 名称', type: 'text', required: true, def: 'X-API-Key', tip: '请求头 / 查询参数的键名' },
    { key: 'param_position', label: '参数位置', type: 'select', required: true, def: 'header', options: POSITION_HQ },
    { key: 'key_value', label: '密钥值', type: 'password', required: true },
    { key: 'key_prefix', label: '密钥前缀', type: 'text', def: '', tip: '密钥前拼接的前缀, 如 ApiKey' },
  ],

  basic: [
    { key: 'username', label: '用户名', type: 'text', required: true },
    { key: 'password', label: '密码', type: 'password', required: true },
    { key: 'auto_encode', label: '自动 Base64 编码', type: 'switch', def: 1 },
  ],

  bearer: [
    { key: 'token_value', label: 'Token 值', type: 'password', required: true },
    { key: 'expire_time', label: '有效期(秒)', type: 'number', def: 7200 },
    { key: 'param_position', label: '传递位置', type: 'select', required: true, def: 'header',
      options: [{ value: 'header', label: 'Header(Authorization)' }, { value: 'query', label: 'Query 参数' }] },
    { key: 'token_prefix', label: '令牌前缀', type: 'text', def: 'Bearer' },
    { key: 'auto_renew', label: '自动续期', type: 'switch', def: 0 },
    { key: 'renew_url', label: '刷新令牌地址', type: 'text', showIf: c => Number(c.auto_renew) === 1, tip: '自动续期开启时生效' },
  ],

  oauth2_client: [
    { key: 'token_url', label: 'Token 获取地址', type: 'text', required: true, url: true },
    { key: 'client_id', label: 'Client ID', type: 'text', required: true },
    { key: 'client_secret', label: 'Client Secret', type: 'password', required: true },
    { key: 'scope', label: 'Scope 权限范围', type: 'multi', def: ['read', 'write'],
      options: ['read', 'write', 'userinfo', 'admin', 'openid'].map(v => ({ value: v, label: v })) },
    { key: 'token_position', label: 'Token 传递位置', type: 'select', required: true, def: 'header', options: POSITION_HQ },
    { key: 'auth_method', label: '认证方式', type: 'select', required: true, def: 'basic',
      options: [{ value: 'basic', label: '请求头 Basic Auth' }, { value: 'body', label: '请求体参数' }] },
    { key: 'token_prefix', label: '令牌前缀', type: 'text', def: 'Bearer' },
    { key: 'expire_time', label: 'Token 有效期(秒)', type: 'number', def: 7200 },
    { key: 'auto_refresh', label: '自动刷新 Token', type: 'switch', def: 1 },
  ],

  oauth2_code: [
    { key: 'authorize_url', label: '授权地址', type: 'text', required: true, url: true },
    { key: 'token_url', label: 'Token 获取地址', type: 'text', required: true, url: true },
    { key: 'client_id', label: 'Client ID', type: 'text', required: true },
    { key: 'client_secret', label: 'Client Secret', type: 'password', required: true },
    { key: 'redirect_url', label: '回调地址', type: 'text', required: true, url: true },
    { key: 'scope', label: 'Scope 权限范围', type: 'multi', def: ['userinfo'],
      options: ['read', 'write', 'userinfo', 'openid'].map(v => ({ value: v, label: v })) },
    { key: 'token_prefix', label: '令牌前缀', type: 'text', def: 'Bearer' },
    { key: 'state_check', label: '状态校验', type: 'switch', def: 1, tip: '开启 state 参数防 CSRF 攻击' },
    { key: 'enable_refresh', label: '支持 Refresh Token', type: 'switch', def: 1 },
  ],

  cas: [
    { key: 'cas_server_url', label: 'CAS 服务端地址', type: 'text', required: true, url: true },
    { key: 'protocol_version', label: '协议版本', type: 'select', required: true, def: 'cas3',
      options: [{ value: 'cas2', label: 'CAS 2.0' }, { value: 'cas3', label: 'CAS 3.0' }] },
    { key: 'service_url', label: '服务回调地址', type: 'text', required: true, url: true },
    { key: 'validate_path', label: '票据校验路径', type: 'select', required: true, def: '/p3/serviceValidate',
      options: ['/serviceValidate', '/p3/serviceValidate', '/proxyValidate'].map(v => ({ value: v, label: v })) },
    { key: 'logout_url', label: '登出地址', type: 'text', def: '/logout' },
    { key: 'auto_redirect', label: '自动跳转登录', type: 'switch', def: 0, tip: '未登录时是否自动跳转到 CAS' },
    { key: 'return_attr', label: '返回用户属性信息', type: 'switch', def: 1 },
  ],

  jwt: [
    { key: 'sign_algorithm', label: '签名算法', type: 'select', required: true, def: 'HS256',
      options: ['HS256', 'HS512', 'RS256', 'ES256'].map(v => ({ value: v, label: v })) },
    { key: 'sign_key', label: '签名密钥 / 公钥', type: 'textarea', required: true, secret: true },
    { key: 'issuer', label: '签发方(iss)', type: 'text' },
    { key: 'audience', label: '接收方(aud)', type: 'text' },
    { key: 'expire_time', label: '有效期(秒)', type: 'number', def: 3600 },
    { key: 'token_position', label: 'Token 传递位置', type: 'select', required: true, def: 'header',
      options: [{ value: 'header', label: 'Header Authorization' }, { value: 'query', label: 'Query 参数' }, { value: 'cookie', label: 'Cookie' }] },
    { key: 'check_expire', label: '校验过期时间', type: 'switch', def: 1 },
    { key: 'token_prefix', label: '令牌前缀', type: 'text', def: 'Bearer' },
  ],

  digest: [
    { key: 'username', label: '用户名', type: 'text', required: true },
    { key: 'password', label: '密码', type: 'password', required: true },
    { key: 'realm', label: 'Realm 域', type: 'text' },
    { key: 'algorithm', label: '摘要算法', type: 'select', required: true, def: 'MD5',
      options: ['MD5', 'SHA-256', 'SHA-512'].map(v => ({ value: v, label: v })) },
    { key: 'auto_nonce', label: '自动携带 nonce 随机数', type: 'switch', def: 1 },
  ],
}

/** 该类型的默认配置对象 */
export function defaultAuthConfig(type) {
  const out = {}
  ;(AUTH_FIELDS[type] || []).forEach(f => { if (f.def !== undefined) out[f.key] = Array.isArray(f.def) ? [...f.def] : f.def })
  return out
}

/** 当前该显示的字段(处理 showIf 联动) */
export function visibleAuthFields(type, cfg) {
  return (AUTH_FIELDS[type] || []).filter(f => !f.showIf || f.showIf(cfg || {}))
}

/** 必填与 URL 格式校验, 返回第一条错误信息 */
export function validateAuthConfig(type, cfg) {
  for (const f of visibleAuthFields(type, cfg)) {
    const v = cfg?.[f.key]
    const empty = v === undefined || v === null || v === '' || (Array.isArray(v) && !v.length)
    if (f.required && empty) return `请填写「${f.label}」`
    if (f.url && !empty && !/^https?:\/\//i.test(String(v))) return `「${f.label}」必须以 http:// 或 https:// 开头`
  }
  return ''
}
