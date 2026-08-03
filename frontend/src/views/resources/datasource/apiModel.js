/* 接口管理器共用常量与工厂 */

export const METHODS = ['GET', 'POST', 'PUT', 'DELETE']
export const METHOD_OPTS = METHODS.map(v => ({ value: v, label: v }))
/* 文档 3.4.1: 方法名按色值高亮 */
export const METHOD_COLOR = { GET: '#00B42A', POST: '#FF7D00', PUT: '#165DFF', DELETE: '#F53F3F' }

/* 文档 4.2: 接口状态枚举与目录圆点色 */
export const API_STATUS = [
  { value: 'debug', label: '调试', color: '#3b82f6' },
  { value: 'done', label: '完成', color: '#00B42A' },
]
export const API_STATUS_META = Object.fromEntries(API_STATUS.map(s => [s.value, s]))

export const CONTENT_TYPES = ['application/json', 'application/x-www-form-urlencoded', 'multipart/form-data']
export const CONTENT_TYPE_OPTS = CONTENT_TYPES.map(v => ({ value: v, label: v }))

export const BODY_MODES = [
  { value: 'none', label: 'none' }, { value: 'form-data', label: 'form-data' },
  { value: 'x-www-form-urlencoded', label: 'x-www-form-urlencoded' },
  { value: 'raw', label: 'raw' }, { value: 'binary', label: 'binary' },
]

export const REQ_TABS = [
  { k: 'setting', label: '设置' }, { k: 'auth', label: '鉴权' }, { k: 'header', label: 'Header' },
  { k: 'query', label: 'Query' }, { k: 'body', label: 'Body' },
]
/* form-data 的行可以是文本或文件, 其余 body 模式只有文本 */
export const PART_TYPES = [{ value: 'text', label: '文本' }, { value: 'file', label: '文件' }]
export const MAX_UPLOAD_BYTES = 10 * 1024 * 1024
export const RESP_TABS = [
  { k: 'live', label: '实时响应' }, { k: 'reqh', label: '请求头' },
  { k: 'resph', label: '响应头' }, { k: 'sample', label: '成功响应示例' },
]

export function newParamRow() { return { enabled: 1, name: '', value: '', desc: '', type: 'text', file: null } }

/** 选中的文件读成 { name, size, mime, data(dataURL) }, data 只存在内存中不入库 */
export function readFile(file) {
  return new Promise((resolve, reject) => {
    if (file.size > MAX_UPLOAD_BYTES) { reject(new Error(`「${file.name}」超过 10MB 上限`)); return }
    const fr = new FileReader()
    fr.onload = () => resolve({ name: file.name, size: file.size, mime: file.type || 'application/octet-stream', data: fr.result })
    fr.onerror = () => reject(new Error('文件读取失败'))
    fr.readAsDataURL(file)
  })
}

export function newApi(groupId = '0') {
  return {
    group_id: groupId, api_code: '', api_name: '未命名接口', method: 'GET', api_path: '/',
    api_status: 'debug', read_write_type: 1, description: '',
    header_inherit: 1, content_type: null, timeout: null, status: 1, sort: 0,
    request_params: JSON.stringify({ header: [], query: [], body: { mode: 'none', raw: '', form: [], file: null } }),
    response_params: null,
  }
}

/** 接口的请求参数结构统一从 request_params JSON 里取, 缺字段时补齐 */
export function parseRequestParams(api) {
  let p = {}
  try { p = typeof api?.request_params === 'string' ? JSON.parse(api.request_params) : (api?.request_params || {}) } catch { p = {} }
  const form = (Array.isArray(p.body?.form) ? p.body.form : []).map(r => ({ type: 'text', file: null, ...r }))
  return {
    header: Array.isArray(p.header) ? p.header : [],
    query: Array.isArray(p.query) ? p.query : [],
    body: { mode: p.body?.mode || 'none', raw: p.body?.raw || '', form, file: p.body?.file || null },
  }
}

/**
 * 入库前剥掉文件内容 —— base64 动辄几 MB, 不该塞进 request_params。
 * 只留文件名/大小/类型做回显, 下次调试需要重新选文件。
 */
export function serializeParams(params) {
  const dropData = f => (f ? { name: f.name, size: f.size, mime: f.mime } : null)
  return JSON.stringify({
    header: params.header,
    query: params.query,
    body: {
      ...params.body,
      form: params.body.form.map(r => (r.type === 'file' ? { ...r, file: dropData(r.file) } : r)),
      file: dropData(params.body.file),
    },
  })
}

/** 组装 /send 的请求载荷: 文本模式发字符串, form-data/binary 发结构化分片 */
export function buildSendPayload(api, params) {
  const b = params.body
  const base = {
    method: api.method, path: api.api_path,
    headers: pairsToObject(params.header), query: pairsToObject(params.query),
    bodyMode: b.mode,
    /* 鉴权覆盖可能还没保存, 随请求一起带过去, 让调试所见即所得 */
    overrideAuth: api.override_auth === 1 ? 1 : 0,
    authType: api.auth_type || null,
    authConfig: api.auth_config || null,
  }
  if (b.mode === 'form-data') {
    return { ...base, parts: b.form.filter(r => r.enabled && String(r.name || '').trim()).map(r => (
      r.type === 'file'
        ? { name: r.name.trim(), type: 'file', filename: r.file?.name, contentType: r.file?.mime, data: r.file?.data || '' }
        : { name: r.name.trim(), type: 'text', value: r.value ?? '' }
    )) }
  }
  if (b.mode === 'binary') {
    return { ...base, file: b.file?.data ? { filename: b.file.name, contentType: b.file.mime, data: b.file.data } : null }
  }
  if (b.mode === 'x-www-form-urlencoded') {
    return { ...base, bodyMode: 'raw', body: new URLSearchParams(pairsToObject(b.form)).toString() }
  }
  return { ...base, bodyMode: 'raw', body: b.mode === 'raw' ? b.raw : '' }
}

/** 发送前的本地校验, 返回错误信息或空串 */
export function validateSendable(params) {
  const b = params.body
  if (b.mode === 'binary' && !b.file?.data) return '请先选择要上传的文件'
  const missing = b.mode === 'form-data'
    && b.form.some(r => r.enabled && r.type === 'file' && String(r.name || '').trim() && !r.file?.data)
  if (missing) return 'form-data 中有文件参数未选择文件(保存后不保留文件内容,需重新选择)'
  return ''
}

/** 启用的键值对 → 普通对象, 发请求用 */
export function pairsToObject(rows) {
  const out = {}
  ;(rows || []).forEach(r => { if (r.enabled && String(r.name || '').trim()) out[r.name.trim()] = r.value ?? '' })
  return out
}

export function joinUrl(base, path) {
  const b = String(base || '').replace(/\/+$/, '')
  const p = String(path || '').trim()
  if (!p) return b
  return b + (p.startsWith('/') ? p : '/' + p)
}

export function prettyJson(s) {
  if (!s) return ''
  try { return JSON.stringify(JSON.parse(s), null, 2) } catch { return s }
}

export function formatSize(n) {
  const v = Number(n) || 0
  if (v < 1024) return v + ' B'
  if (v < 1024 * 1024) return (v / 1024).toFixed(1) + ' KB'
  return (v / 1024 / 1024).toFixed(1) + ' MB'
}
