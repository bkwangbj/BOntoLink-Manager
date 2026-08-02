/* 函数规则入参映射: 常量与工厂, 供 ActionDetailWorkspace 与 FuncParamMapTable 共用 */

/* 6 = 主对象: 动作绑定的那个对象实例, 仅动作挂在对象详情页时存在
 * 7 = 本动作创建的对象: 同一动作中前序「创建对象」规则的运行时产物 */
export const VALUE_SOURCES = { 1: '表单参数', 2: '静态值', 3: '当前用户', 4: '系统时间', 5: '关联对象属性', 6: '主对象', 7: '本动作创建的对象' }
export const VALUE_SOURCE_OPTS = [1, 2, 3, 4, 5, 6, 7].map(v => ({ value: v, label: VALUE_SOURCES[v] }))

export const FUNC_PTYPE_OPTS = [
  { value: 'string', label: '字符串' }, { value: 'number', label: '数字' }, { value: 'boolean', label: '布尔' },
  { value: 'object', label: '对象' }, { value: 'date', label: '日期' },
]

export function newFuncParam(required = 1) {
  return { name: '', param_type: 'string', required, value_source: 1, value_content: '' }
}
