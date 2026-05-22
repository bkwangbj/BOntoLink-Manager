import axios from 'axios'
import { BL } from '@/lib/bl.js'

const http = axios.create({
  baseURL: '/api',
  timeout: 20000
})

http.interceptors.response.use(
  res => {
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 0) {
        BL.error(body.msg || `请求失败 ${body.code}`)
        return Promise.reject(body)
      }
      return body.data
    }
    return body
  },
  err => {
    BL.error(err.message || '网络错误')
    return Promise.reject(err)
  }
)

export const categoryApi = {
  tree:   () => http.get('/category/tree'),
  get:    (id) => http.get(`/category/${id}`),
  stats:  (id) => http.get(`/category/${id}/stats`),
  statsBatch: (ids) => http.get('/category/stats-batch', { params: { ids: ids.join(',') } }),
  graph:  (id) => http.get(`/category/${id}/graph`),
  picker: () => http.get('/category/picker'),
  addMember:     (id, classId) => http.post(`/category/${id}/members`, { classId }),
  removeMember:  (id, classId) => http.delete(`/category/${id}/members/${classId}`),
  reorderMembers:(id, classIds) => http.put(`/category/${id}/members/reorder`, { classIds }),
  create: (data) => http.post('/category', data),
  update: (id, data) => http.put(`/category/${id}`, data),
  remove: (id) => http.delete(`/category/${id}`)
}

export const namespaceApi = {
  list:    () => http.get('/namespace'),
  get:     (code) => http.get(`/namespace/${code}`),
  create:  (data) => http.post('/namespace', data),
  update:  (code, data) => http.put(`/namespace/${code}`, data),
  versions:(code) => http.get(`/namespace/${code}/versions`),
  createVersion: (data) => http.post('/namespace/versions', data),
  removeVersion: (id) => http.delete(`/namespace/versions/${id}`)
}

export const groupApi = {
  listByParent: (parentId) => http.get('/group', { params: { parentId } }),
  create: (data) => http.post('/group', data),
  update: (id, data) => http.put(`/group/${id}`, data),
  remove: (id) => http.delete(`/group/${id}`),
  classes:(id) => http.get(`/group/${id}/classes`)
}

export const resourceApi = {
  stats:       () => http.get('/resource/discover/stats'),
  classes:     (params) => http.get('/resource/classes', { params }),
  classDetail: (id) => http.get(`/resource/classes/${id}`),
  links:       () => http.get('/resource/links'),
  actions:     () => http.get('/resource/actions'),
  interfaces:  () => http.get('/resource/interfaces'),
  interface:   (id) => http.get(`/resource/interfaces/${id}`),
  properties:  (id) => http.get(`/resource/classes/${id}/properties`),
  graph:       () => http.get('/resource/graph')
}

/* 共享「业务图库」(图标目录树 + 上传 SVG) */
export const iconLibApi = {
  all:           () => http.get('/icon-lib'),
  seed:          (force = false) => http.post(`/icon-lib/seed?force=${force}`),
  createGroup:   (parentId, name) => http.post('/icon-lib/groups', { parentId, name }),
  renameGroup:   (id, name) => http.put(`/icon-lib/groups/${id}`, { name }),
  deleteGroup:   (id) => http.delete(`/icon-lib/groups/${id}`),
  addIcon:       (groupId, payload) => http.post(`/icon-lib/groups/${groupId}/icons`, payload),
  deleteIcon:    (id) => http.delete(`/icon-lib/icons/${id}`),
  deleteIconBatch: (ids) => http.post('/icon-lib/icons/batch-delete', { ids })
}

export const interfaceApi = {
  list:        () => http.get('/interface'),
  get:         (id) => http.get(`/interface/${id}`),
  create:      (data) => http.post('/interface', data),
  update:      (id, data) => http.put(`/interface/${id}`, data),
  toggle:      (id) => http.post(`/interface/${id}/toggle`),
  remove:      (id) => http.delete(`/interface/${id}`),
  properties:      (id) => http.get(`/interface/${id}/properties`),
  addProperty:     (id, data) => http.post(`/interface/${id}/properties`, data),
  updateProperty:  (propId, data) => http.put(`/interface/properties/${propId}`, data),
  removeProperty:  (propId) => http.delete(`/interface/properties/${propId}`),
  implementers:    (id) => http.get(`/interface/${id}/implementers`),
  addImpl:         (id, data) => http.post(`/interface/${id}/implementers`, data),
  removeImpl:      (id, classId) => http.delete(`/interface/${id}/implementers/${classId}`)
}

export const datasourceApi = {
  list:     () => http.get('/datasource'),
  get:      (id) => http.get(`/datasource/${id}`),
  overview: () => http.get('/datasource/overview'),
  drivers:  () => http.get('/datasource/drivers'),
  create:   (data) => http.post('/datasource', data),
  update:   (id, data) => http.put(`/datasource/${id}`, data),
  remove:   (id) => http.delete(`/datasource/${id}`),
  toggle:   (id) => http.post(`/datasource/${id}/toggle`),
  test:     (id) => http.post(`/datasource/${id}/test`),
  monitor:  (id) => http.get(`/datasource/${id}/monitor`)
}

export default http
