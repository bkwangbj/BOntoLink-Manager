/* 函数规则入参映射: 常量与工厂, 供 ActionDetailWorkspace 与 FuncParamMapTable 共用 */

export const VALUE_SOURCES = { 1: '来自参数', 2: '静态值', 3: '当前用户', 4: '系统时间', 5: '对象参数属性' }

/* 文档 5.3.3 四类取值来源顺序: 来自参数 / 对象参数属性 / 静态值 / 当前用户 / 系统时间 */
export const VALUE_SOURCE_OPTS = [1, 5, 2, 3, 4].map(v => ({ value: v, label: VALUE_SOURCES[v] }))

export const FUNC_PTYPE_OPTS = [
  { value: 'string', label: '字符串' }, { value: 'number', label: '数字' }, { value: 'boolean', label: '布尔' },
  { value: 'object', label: '对象' }, { value: 'date', label: '日期' },
]

export function newFuncParam(required = 1) {
  return { name: '', param_type: 'string', required, value_source: 1, value_content: '' }
}
