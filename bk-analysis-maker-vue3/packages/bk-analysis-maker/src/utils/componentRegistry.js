import { ElMessage } from 'element-plus'
// 组件注册表类
class ComponentRegistry {
  constructor () {
    this.registry = new Map()
  }

  // 注册组件
  register (key, instance, $root, isSetMode) {
    if (this.registry.has(key)) {
      if (isSetMode) {
        ElMessage.warning(`"${key}" 已存在`)
      }
      // console.warn(`Component with key "${key}" is already registered`)
      return false
    }

    this.registry.set(key, { key, instance, $root })
    return true
  }

  // 获取组件
  get (key) {
    const component = this.registry.get(key)

    if (!component) {
      console.warn(`Component with key "${key}" not found`)
      return null
    }

    return component
  }

  // 注销组件
  unregister (key) {
    return this.registry.delete(key)
  }

  // 获取所有组件
  getAll () {
    return Object.fromEntries(this.registry)
  }

  // 通过模式查找组件
  find (pattern) {
    const allComponents = this.getAll()
    const result = {}
    const regex = typeof pattern === 'string' ? new RegExp(pattern) : pattern

    for (const [key, component] of Object.entries(allComponents)) {
      if (regex.test(key)) {
        result[key] = component
      }
    }

    return result
  }
}

// 创建全局单例
export const componentRegistry = new ComponentRegistry()
