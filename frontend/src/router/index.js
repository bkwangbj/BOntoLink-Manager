import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/workspace/discover' },
  { path: '/workspace/discover', name: 'discover', component: () => import('@/views/workspace/Discover.vue'),  meta: { title: '总览' } },
  { path: '/workspace/graph',    name: 'graph',    component: () => import('@/views/workspace/Graph.vue'),     meta: { title: '图谱' } },
  { path: '/workspace/instances',name: 'instances',component: () => import('@/views/workspace/Instances.vue'), meta: { title: '实例' } },

  { path: '/resources/object-types', name: 'objectTypes', component: () => import('@/views/resources/ObjectTypes.vue'), meta: { title: '对象类型' } },
  { path: '/resources/link-types',   name: 'linkTypes',   component: () => import('@/views/resources/LinkTypes.vue'),   meta: { title: '关系类型' } },
  { path: '/resources/action-types', name: 'actionTypes', component: () => import('@/views/resources/ActionTypes.vue'), meta: { title: '动作类型' } },
  { path: '/resources/value-types',  name: 'valueTypes',  component: () => import('@/views/config/ValueTypes.vue'), meta: { title: '值类型' } },
  { path: '/resources/enum-types',   name: 'enumTypes',   component: () => import('@/views/config/EnumTypes.vue'),  meta: { title: '枚举类型' } },
  { path: '/resources/shared-props', name: 'sharedProps', component: () => import('@/views/resources/PlaceholderList.vue'), props: { kind: 'sharedProps', title: '共享属性', cn: '共享属性', en: 'Shared props' }, meta: { title: '共享属性' } },
  { path: '/resources/functions',    name: 'functions',   component: () => import('@/views/resources/PlaceholderList.vue'), props: { kind: 'functions', title: '函数', cn: '函数', en: 'Functions' }, meta: { title: '函数' } },
  { path: '/resources/interfaces',   name: 'interfaces',  component: () => import('@/views/resources/Interfaces.vue'),  meta: { title: '接口' } },
  { path: '/resources/datasources',  name: 'datasources', component: () => import('@/views/resources/Datasources.vue'), meta: { title: '数据源' } },

  { path: '/tools/ai',            name: 'ai',           component: () => import('@/views/tools/AiAssistant.vue'), meta: { title: 'AI 助手' } },
  { path: '/tools/import-export', name: 'importExport', component: () => import('@/views/tools/ImportExport.vue'), meta: { title: '导入导出' } },

  { path: '/config/category', name: 'category', component: () => import('@/views/config/Category.vue'), meta: { title: '行业分类管理' } },
  { path: '/config/security', name: 'security', component: () => import('@/views/config/Security.vue'), meta: { title: '权限安全' } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.afterEach((to) => {
  document.title = `${to.meta.title || ''} · BOntoLink`
})

export default router
