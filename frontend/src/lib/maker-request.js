/**
 * analysis-maker(数据可视化设计器)的 request 适配器
 * 把 maker 内部 `componentConfigs.request[type](path, params)` 桥接到 BOntoLink02 的 /api。
 * 说明:我们主要用「静态数据」喂图表(由 instanceApi 取数后构造 config),
 *      接口型数据源/上传等暂作最小实现或占位,后续按需接。
 */
import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 20000 })
http.interceptors.response.use(
  res => {
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body) return body.code === 0 ? body.data : Promise.reject(body)
    return body
  },
  err => Promise.reject(err)
)

function toForm (data) {
  const fd = new FormData()
  if (data) for (const k in data) {
    const v = data[k]
    fd.append(k, typeof v === 'object' ? JSON.stringify(v) : v)
  }
  return fd
}

export default {
  post (url, data, useJson = true) { return http.post(url, useJson ? data : toForm(data)) },
  get (url, params) { return http.get(url, { params }) },
  fetchGet (url, params) { return http.get(url, { params }) },
  fetchPost (url, data, useJson = true) { return http.post(url, useJson ? data : toForm(data)) },
  put (url, data) { return http.put(url, data) },
  del (url, params) { return http.delete(url, { params }) },
  // 表格/视图取数(maker 内部用于「接口」数据源,BOntoLink02 暂未提供对应端点)
  singleTableOperation (params) { return http.post('/instance/list', params) },
  // 文件上传占位(主题/背景图等用,演示阶段不接)
  upload () { return Promise.resolve({ data: '' }) },
  copyTextToClipboard (text) {
    try { navigator.clipboard.writeText(String(text)) } catch (e) { /* ignore */ }
  }
}
