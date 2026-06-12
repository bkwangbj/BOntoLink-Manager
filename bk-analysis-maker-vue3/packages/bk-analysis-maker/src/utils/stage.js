import { componentRegistry } from './componentRegistry'

class Stage {
  // constructor () {
  //   // 可以在这里初始化一些全局状态或工具
  // }

  // 获取组件
  get (key) {
    const component = componentRegistry.get(key)

    if (!component) {
      return null
    }

    // 返回包装后的组件代理
    return this.createComponentProxy(component)
  }

  // 创建组件代理，控制对组件的访问
  createComponentProxy (component) {
    const handler = {
      get: (target, prop) => {
        // 特殊处理$root属性
        if (prop === '$root') {
          return target.$root
        }

        // 检查是否是允许访问的方法或属性
        if (prop in target.instance && this.isAllowedProperty(prop)) {
          const value = target.instance[prop]

          // 如果是函数，绑定正确的this上下文
          if (typeof value === 'function') {
            return value.bind(target.instance)
          }

          return value
        }

        // 不允许访问的属性
        console.warn(`Property "${prop}" is not accessible or does not exist`)
        return undefined
      },

      set: (target, prop, value) => {
        // 控制哪些属性可以设置
        if (this.isAllowedProperty(prop)) {
          target.instance[prop] = value
          return true
        }

        console.warn(`Setting property "${prop}" is not allowed`)
        return false
      }
    }

    return new Proxy(component, handler)
  }

  // 检查属性是否允许访问
  isAllowedProperty (prop) {
    const allowedProperties = [
      // DOM相关
      '$root',

      // 组件方法
      'show', 'hide', 'update', 'setStyle', 'addClass', 'setContentConfig', 'setData', 'getData',

      // 事件相关
      'on', 'off', 'emit'
    ]

    return allowedProperties.includes(prop)
  }

  // 获取所有组件
  getAll () {
    const allComponents = componentRegistry.getAll()
    const result = {}

    for (const [key, component] of Object.entries(allComponents)) {
      result[key] = this.createComponentProxy(component)
    }

    return result
  }

  // 通过模式查找组件
  find (pattern) {
    const foundComponents = componentRegistry.find(pattern)
    const result = {}

    for (const [key, component] of Object.entries(foundComponents)) {
      result[key] = this.createComponentProxy(component)
    }

    return result
  }
}

// 创建全局stage实例
export const stage = new Stage()
