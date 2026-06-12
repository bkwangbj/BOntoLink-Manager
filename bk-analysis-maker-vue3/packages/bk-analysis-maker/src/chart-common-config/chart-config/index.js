const requireComponent = import.meta.glob(
  './*.vue',
  { eager: true }
)
// 获取所有组件对象
export const baseChartComponents = Object.keys(requireComponent).reduce((preModule, curModule) => {
  // 模块对象
  const value = requireComponent[curModule]
  // 组件名
  const moduleName = curModule.replace(/^\.\/(.*)\.\w+$/, '$1').split('/')[0]
  preModule[moduleName] = value.default
  return preModule
}, {})
