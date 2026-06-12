import * as VueRouter from 'vue-router'
import Layout from '@/components/main-layout.vue'
import { useComponentsStore } from '../store'

const router = VueRouter.createRouter({
  history: VueRouter.createWebHashHistory(),
  routes: [
    {
      path: '/',
      name: 'Layout',
      component: Layout,
      children: []
    }
  ]
})

export {
  router
}

async function addRoutes () {
  const components = import.meta.glob('../views/*.vue', { eager: true })
  const routes = Object.keys(components).map(c => {
    const fileName = c.split('/').pop()
    const name = fileName.substring(0, fileName.length - 4)
    return {
      path: `${name}`,
      name,
      component: components[c].default
    }
  })

  const store = useComponentsStore()

  store.setComponents(routes.map(r => r.name))
  for (const r of routes) {
    await router.addRoute('Layout', r)
  }
}

let routeInited = false

router.beforeEach(async (to, from) => {
  if (!routeInited) {
    routeInited = true
    await addRoutes()
    return to.fullPath
  }
})
