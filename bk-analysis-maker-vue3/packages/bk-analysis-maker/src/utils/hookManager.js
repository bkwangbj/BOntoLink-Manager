import { stage } from './stage'

class HookManager {
  constructor () {
    this.hooks = {
      onMount: [],
      onUpdate: [],
      onDestroy: [],
      onDataChange: []
    }
    this.sandboxContexts = new WeakMap()
  }

  // 执行 Hook 脚本
  executeHookScript (script, componentInstance) {
    if (!script || typeof script !== 'string') return

    try {
      const sandbox = this.createSandbox(componentInstance)

      // 使用 new Function 创建执行环境
      const hookFunction = new Function(
        'sandbox',
        `with(sandbox) {
          try {
            ${script}
          } catch(e) {
            console.error('Hook execution error:', e);
          }
        }`
      )

      // 在沙箱中执行脚本
      hookFunction(sandbox)
    } catch (error) {
      console.error('Failed to execute hook script:', error)
    }
  }

  // 创建沙箱环境
  createSandbox (componentInstance) {
    // 如果已有此实例的沙箱，则返回它
    if (this.sandboxContexts.has(componentInstance)) {
      return this.sandboxContexts.get(componentInstance)
    }

    // 创建沙箱上下文
    const sandbox = {
      // 生命周期注册函数
      onMount: (fn) => this.registerHook('onMount', fn, componentInstance),
      onUpdate: (fn) => this.registerHook('onUpdate', fn, componentInstance),
      onDestroy: (fn) => this.registerHook('onDestroy', fn, componentInstance),
      onDataChange: (fn) => this.registerHook('onDataChange', fn, componentInstance),

      // 全局 stage 对象
      stage,

      // 工具函数
      console: {
        log: (...args) => {
          // 安全地打印参数，避免 Symbol 转换错误
          const safeArgs = args.map(arg => {
            if (typeof arg === 'symbol') {
              return arg.toString()
            }
            if (arg && typeof arg === 'object') {
              try {
                JSON.stringify(arg)
                return arg
              } catch {
                return '[Object]'
              }
            }
            return arg
          })
          console.log.apply(console, safeArgs)
        },
        warn: console.warn,
        error: console.error
      },
      setTimeout,
      clearTimeout,
      setInterval,
      clearInterval,

      // 数学函数
      Math,
      Date,

      // 组件API - 通过stage获取
      comp: null // 将在下面设置
    }

    // 创建代理沙箱以限制访问
    const sandboxProxy = new Proxy(sandbox, {
      has (target, key) {
        return true // 欺骗with语句，让它认为所有属性都已存在
      },
      get (target, key, receiver) {
        if (key in target) return target[key]

        // 安全地将 key 转换为字符串，处理 Symbol 类型
        const keyString = typeof key === 'symbol' ? key.toString() : String(key)

        // 阻止访问未允许的全局对象
        console.warn(`Access to '${keyString}' is not allowed in hook sandbox.`)
        return undefined
      },
      set (target, key, value, receiver) {
      // 安全地将 key 转换为字符串，处理 Symbol 类型
        const keyString = typeof key === 'symbol' ? key.toString() : String(key)

        // 只允许修改沙箱对象上的特定属性
        if (keyString in target && !keyString.startsWith('_')) {
          target[key] = value
          return true
        }

        console.warn(`Setting property '${keyString}' is not allowed in hook sandbox.`)
        return false
      }
    })

    this.sandboxContexts.set(componentInstance, sandboxProxy)
    return sandboxProxy
  }

  // 注册钩子函数
  registerHook (hookName, fn, componentInstance) {
    if (typeof fn === 'function') {
      this.hooks[hookName].push({
        fn,
        componentInstance
      })
    }
  }

  // 触发钩子执行
  triggerHook (hookName, componentInstance, ...args) {
    const hooksToExecute = this.hooks[hookName].filter(
      h => h.componentInstance === componentInstance
    )

    hooksToExecute.forEach(hook => {
      try {
        const sandbox = this.createSandbox(componentInstance)
        hook.fn.call(sandbox, ...args)
      } catch (error) {
        console.error(`Error executing ${hookName} hook:`, error)
      }
    })
  }

  // 清理组件相关的钩子
  cleanupComponentHooks (componentInstance) {
    Object.keys(this.hooks).forEach(hookName => {
      this.hooks[hookName] = this.hooks[hookName].filter(
        h => h.componentInstance !== componentInstance
      )
    })
    this.sandboxContexts.delete(componentInstance)
  }
}

// 创建全局单例
export const hookManager = new HookManager()
