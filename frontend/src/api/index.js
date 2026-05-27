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

/* 对象类型补充元数据 (等价/不相交/互斥并集/等价属性/互斥属性 等) */
export const classMetaApi = {
  candidates:        () => http.get('/class-meta/classes'),

  listGroup:         (classId, type) => http.get('/class-meta/class-group', { params: { classId, type } }),
  addGroup:          (data) => http.post('/class-meta/class-group', data),
  updateGroup:       (id, data) => http.put(`/class-meta/class-group/${id}`, data),
  removeGroup:       (id) => http.delete(`/class-meta/class-group/${id}`),

  listDisjointUnion: (parentClassId) => http.get('/class-meta/disjoint-union', { params: { parentClassId } }),
  addDisjointUnion:  (data) => http.post('/class-meta/disjoint-union', data),
  updateDisjointUnion: (id, data) => http.put(`/class-meta/disjoint-union/${id}`, data),
  removeDisjointUnion: (id) => http.delete(`/class-meta/disjoint-union/${id}`),

  listPropEquiv:     (classId) => http.get('/class-meta/property-equivalent', { params: { classId } }),
  addPropEquiv:      (data) => http.post('/class-meta/property-equivalent', data),
  updatePropEquiv:   (id, data) => http.put(`/class-meta/property-equivalent/${id}`, data),
  removePropEquiv:   (id) => http.delete(`/class-meta/property-equivalent/${id}`),

  listPropDisjoint:  (classId) => http.get('/class-meta/property-disjoint', { params: { classId } }),
  addPropDisjoint:   (data) => http.post('/class-meta/property-disjoint', data),
  updatePropDisjoint:(id, data) => http.put(`/class-meta/property-disjoint/${id}`, data),
  removePropDisjoint:(id) => http.delete(`/class-meta/property-disjoint/${id}`),

  listProps:         (classId) => http.get(`/class-meta/classes/${classId}/properties`),
  addProp:           (classId, data) => http.post(`/class-meta/classes/${classId}/properties`, data),
  updateProp:        (propId, data) => http.put(`/class-meta/properties/${propId}`, data),
  removeProp:        (propId) => http.delete(`/class-meta/properties/${propId}`),

  updateClass:       (id, data) => http.put(`/class-meta/classes/${id}`, data),
}

/* 值类型 (Value types) */
export const valueTypeApi = {
  list:          () => http.get('/value-types'),
  get:           (id) => http.get(`/value-types/${id}`),
  create:        (data) => http.post('/value-types', data),
  update:        (id, data) => http.put(`/value-types/${id}`, data),
  remove:        (id) => http.delete(`/value-types/${id}`),
  listUsageConfigs: () => http.get('/value-types/usage-configs'),
  createUsageConfig: (data) => http.post('/value-types/usage-configs', data),
  updateUsageConfig: (id, data) => http.put(`/value-types/usage-configs/${id}`, data),
}

/* 枚举类型 (Enum types) */
export const enumTypeApi = {
  listGroups:    () => http.get('/enum-types/groups'),
  createGroup:   (data) => http.post('/enum-types/groups', data),
  updateGroup:   (id, data) => http.put(`/enum-types/groups/${id}`, data),
  removeGroup:   (id) => http.delete(`/enum-types/groups/${id}`),
  list:          () => http.get('/enum-types'),
  get:           (id) => http.get(`/enum-types/${id}`),
  create:        (data) => http.post('/enum-types', data),
  update:        (id, data) => http.put(`/enum-types/${id}`, data),
  remove:        (id) => http.delete(`/enum-types/${id}`),
  listItems:     (enumId) => http.get(`/enum-types/${enumId}/items`),
  addItem:       (enumId, data) => http.post(`/enum-types/${enumId}/items`, data),
  updateItem:    (id, data) => http.put(`/enum-types/items/${id}`, data),
  removeItem:    (id) => http.delete(`/enum-types/items/${id}`),
  listLevelRules:(enumId) => http.get(`/enum-types/${enumId}/level-rules`),
  saveLevelRules:(enumId, rules) => http.post(`/enum-types/${enumId}/level-rules`, rules),
  /* 同步规则 */
  getSyncConfig: (enumId) => http.get(`/enum-types/${enumId}/sync-config`),
  saveSyncConfig:(enumId, data) => http.post(`/enum-types/${enumId}/sync-config`, data),
  listSyncLogs: (enumId) => http.get(`/enum-types/${enumId}/sync-logs`),
  runSync:      (enumId, body) => http.post(`/enum-types/${enumId}/sync-run`, body || {}),
  testSync:     (enumId, body) => http.post(`/enum-types/${enumId}/sync-test`, body || {}),
  /* 被引用查询 */
  listReferences:(enumId) => http.get(`/enum-types/${enumId}/references`),
}

/* 属性格式化 */
export const propertyFormatApi = {
  byProperties:  (ids) => http.get('/property-format/by-properties', { params: { ids: ids.join(',') }, paramsSerializer: { indexes: null } }),
  get:           (propertyId) => http.get(`/property-format/property/${propertyId}`),
  save:          (propertyId, data) => http.put(`/property-format/property/${propertyId}`, data),
  remove:        (propertyId) => http.delete(`/property-format/property/${propertyId}`),
  batchSave:     (ids, format, scope = 'class') => http.put('/property-format/batch', { ids, format, property_scope: scope }),
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
