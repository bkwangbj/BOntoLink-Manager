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
  setCurrentVersion: (id) => http.post(`/namespace/versions/${id}/current`),
  removeVersion: (id) => http.delete(`/namespace/versions/${id}`)
}

export const groupApi = {
  listByParent: (parentId) => http.get('/group', { params: { parentId } }),
  listAll:      () => http.get('/group/all'),
  create: (data) => http.post('/group', data),
  update: (id, data) => http.put(`/group/${id}`, data),
  remove: (id) => http.delete(`/group/${id}`),
  classes:(id) => http.get(`/group/${id}/classes`)
}

/* 统一分组关联 (ont_biz_group_class with ref_id + group_type)
   group_type: object_types / link_types / action_types / value_types
              / shared_props / functions / interface / datasources / enum_types */
export const groupRefApi = {
  list:       (type) => http.get('/group-refs', { params: { type } }),
  byGroup:    (groupId, type) => http.get('/group-refs/by-group', { params: { groupId, type } }),
  create:     (data) => http.post('/group-refs', data),
  update:     (id, data) => http.put(`/group-refs/${id}`, data),
  remove:     (id) => http.delete(`/group-refs/${id}`),
  removeByRef:(refId, type) => http.delete('/group-refs/by-ref', { params: { refId, type } })
}

export const resourceApi = {
  stats:       () => http.get('/resource/discover/stats'),
  /* 总览综合统计 — industries / domains 为逗号分隔的 category_code, 不传或空 = 全部 */
  overview:    (params) => http.get('/resource/discover/overview', { params }),
  classes:     (params) => http.get('/resource/classes', { params }),
  classDetail: (id) => http.get(`/resource/classes/${id}`),
  links:       () => http.get('/resource/links'),
  actions:     () => http.get('/resource/actions'),
  interfaces:  () => http.get('/resource/interfaces'),
  interface:   (id) => http.get(`/resource/interfaces/${id}`),
  properties:  (id) => http.get(`/resource/classes/${id}/properties`),
  graph:       () => http.get('/resource/graph')
}

/* 图谱 — 双画布数据 (左: 行业层级, 右: 对象本体) */
export const graphApi = {
  industryTree: () => http.get('/graph/industry-tree'),
  ontology:     () => http.get('/graph/ontology')
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
  reorderProps:      (classId, ids) => http.post(`/class-meta/classes/${classId}/properties/reorder`, { ids }),

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

/* 共享属性 (Shared properties) */
export const sharedPropertyApi = {
  list:        () => http.get('/shared-properties'),
  get:         (id) => http.get(`/shared-properties/${id}`),
  create:      (data) => http.post('/shared-properties', data),
  update:      (id, data) => http.put(`/shared-properties/${id}`, data),
  remove:      (id) => http.delete(`/shared-properties/${id}`),
  batchRemove: (ids) => http.post('/shared-properties/batch-delete', { ids }),
  references:  (id) => http.get(`/shared-properties/${id}/references`),
}

/* 结构属性 (Struct types) — 一组共享属性的复合: 姓名 / 地址 / 时间段 / 金额 等 */
export const structTypeApi = {
  list:        () => http.get('/struct-types'),
  get:         (id) => http.get(`/struct-types/${id}`),       // 返回结构 + items
  items:       (id) => http.get(`/struct-types/${id}/items`),
  create:      (data) => http.post('/struct-types', data),    // data 可带 items: [{ prop_id, sort_no }]
  update:      (id, data) => http.put(`/struct-types/${id}`, data),  // items 整体覆盖
  remove:      (id) => http.delete(`/struct-types/${id}`),
  batchRemove: (ids) => http.post('/struct-types/batch-delete', { ids }),
}

/* 链接类型 (Link Types) — 升级版双向关系建模 (ont_link_types + ont_link_mappings) */
export const linkTypeApi = {
  list:        () => http.get('/link-types'),
  get:         (id) => http.get(`/link-types/${id}`),         // 含 mappings + type_classes
  mappings:    (id) => http.get(`/link-types/${id}/mappings`),
  create:      (data) => http.post('/link-types', data),      // 可带 mappings: [{ side, seq, object_field, join_table_column }]
  update:      (id, data) => http.put(`/link-types/${id}`, data),
  remove:      (id) => http.delete(`/link-types/${id}`),
  batchRemove: (ids) => http.post('/link-types/batch-delete', { ids }),
  setStatus:   (id, status) => http.post(`/link-types/${id}/status`, { status }),
}

/* 类型类 (Type Classes) — 三类: property / relation / action */
export const typeClassApi = {
  list:        (params) => http.get('/type-classes', { params }),   // { applicableType, category, deprecated, catalogOnly }
  get:         (id) => http.get(`/type-classes/${id}`),
  stats:       () => http.get('/type-classes/category-stats'),
  create:      (data) => http.post('/type-classes', data),
  update:      (id, data) => http.put(`/type-classes/${id}`, data),
  remove:      (id) => http.delete(`/type-classes/${id}`),
  deprecate:   (id, deprecated = 1) => http.post(`/type-classes/${id}/deprecate`, { deprecated }),
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

/* 物理表/视图元数据 — 落库于 ont_physical_table, 同步源为后端自身库 */
export const physicalTableApi = {
  list:       (dsId) => http.get('/physical-tables', { params: dsId ? { dsId } : {} }),
  sync:       (dsId) => http.post('/physical-tables/sync', null, { params: { dsId } }),
  updateName: (id, displayName) => http.put(`/physical-tables/${id}/name`, { displayName })
}

/* 全局搜索 — 头部 ⌘K 弹框驱动. type: all / object / link / prop / ds / other */
export const searchApi = {
  global: (q, type = 'all') => http.get('/search/global', { params: { q, type } })
}

/* 实例探索 (Individual Discover) — 内存模拟实例数据
   object-types: 对象类型 + 实例计数 + 行业/领域标签 (概览卡片/侧边导航)
   list: 分页 + 关键词 q + 多条件 filter(JSON) + 排序
   detail: 单实例 + 列定义 + 关联对象类型
   aggregate: 分组聚合(图表/统计)
   search: 全局搜索(对象类型 + 实例样本) */
export const instanceApi = {
  objectTypes: () => http.get('/instance/object-types'),
  list: (params) => http.get('/instance/list', { params }),
  columns: (classId) => http.get('/instance/columns', { params: { classId } }),
  detail: (classId, id) => http.get('/instance/detail', { params: { classId, id } }),
  links: (classId, id) => http.get('/instance/links', { params: { classId, id } }),
  aggregate: (params) => http.get('/instance/aggregate', { params }),
  stat: (params) => http.get('/instance/stat', { params }),
  matrix: (params) => http.get('/instance/matrix', { params }),
  search: (q, perType = 8) => http.get('/instance/search', { params: { q, perType } }),
  searchResults: (q, sampleN = 4) => http.get('/instance/search-results', { params: { q, sampleN } })
}

export default http
