import axios from 'axios'
import { ElMessage } from 'element-plus'
const handleData = ({ config, data, status, statusText }) => {
  // if (config.params.method === 'expertDoc') {
  //   return data
  // }
  if (data[0]) {
    data = data[0]
  }
  if (data.result === 'SUCCESS' || data.success || data.status) {
    // // 统一处理分页数据的总数
    // if (data.page && data.page.total) {
    //   data.total = parseInt(data.page.total)
    // }
    return data.data
  }
  const msg = data.msg || '操作失败'
  ElMessage.error(msg)
  return Promise.reject(msg)
}
const instance = axios.create({
  baseURL: 'http://dev.beiktech.com:7887/',
  // baseURL: 'http://39.107.246.141:10731',
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

instance.interceptors.response.use(
  (response) => handleData(response),
  (error) => {
    // Vue.prototype.$message.error(error)
    const { response } = error
    return Promise.reject(response || error)
  }
)

export default {
  post (url, data, useJson = true) {
    const fd = new FormData()
    if (data && !useJson) {
      for (const key in data) {
        if (Object.hasOwnProperty.call(data, key)) {
          const value = data[key]
          if (typeof value === 'object') {
            fd.append(key, JSON.stringify(value))
          } else {
            fd.append(key, value)
          }
        }
      }
    }
    return instance({
      url,
      method: 'post',
      data: useJson ? data : fd
    })
  },
  get (url, params = {}) {
    return instance({
      url,
      method: 'get',
      params
    })
  },
  singleTableOperation (configs) {
    return this.post(configs.url || '/SingleTable/curd', {
      appId: configs.appId || 'watf',
      bzCode: configs.bzCode || 'WATF',
      metaSet: configs.metaSet,
      viewCode: configs.viewCode,
      methodCode: configs.methodCode || 'select',
      methodParams: JSON.stringify(configs.methodParams),
      pageNum: configs.pageNum,
      pageSize: configs.pageSize
    })
  },
  copyTextToClipboard (text) {
    const textArea = document.createElement('textarea')
    textArea.style.position = 'fixed'
    textArea.style.visibility = '-10000px'
    textArea.value = text
    document.body.appendChild(textArea)
    textArea.focus()
    textArea.select()

    if (!document.execCommand('copy')) {
      ElMessage.warning('浏览器暂不支持此操作！')
    } else {
      ElMessage.success('复制成功！')
    }
    document.body.removeChild(textArea)
  },
  async fetchPost (url, params = {}, isJson = true, externalHeaders = {}) {
    let query = new URLSearchParams(params).toString()
    if (isJson) {
      query = JSON.stringify(params)
    }
    const res = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': isJson ? 'application/json' : 'application/x-www-form-urlencoded',
        ...externalHeaders
      },
      body: query
    })
    const response = await res.json()
    return response
  },
  async fetchGet (url, params = {}, isJson = true) {
    const query = new URLSearchParams(params).toString()
    const queryUrl = url + '?' + query
    const res = await fetch(queryUrl, {
      method: 'GET',
      headers: {
        'Content-Type': isJson ? 'application/json' : 'application/x-www-form-urlencoded'
      }
    })
    const response = await res.json()
    return response
  }
}
