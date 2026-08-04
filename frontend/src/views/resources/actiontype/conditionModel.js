/* 条件配置单一事实源 — 主体 / 运算符 / 类型兼容 / 摘要 / 老数据迁移
 *
 * 提交标准、覆盖规则、对象集过滤三处原本各维护一份运算符表, key 拼写还不一致
 * (notempty vs notEmpty), 导致同一语义在库里存成两种字符串。此处收敛为一份。
 * 各调用点用 pickOps() 取自己那份子集, 保持现有 UI 不变。
 */

export const SUBJECTS = [
  { key: 'object', label: '对象', icon: 'box', color: '#165DFF' },
  { key: 'user', label: '用户', icon: 'user', color: '#722ED1' },
  { key: 'usergroup', label: '用户组', icon: 'users', color: '#0FC6C2' },
  { key: 'param', label: '表单参数', icon: 'edit', color: '#00B42A' },
]

/* 用户 / 用户组主体的内置可比较字段 */
export const USER_FIELDS = [
  { code: 'user_id', name: '用户 ID', dataType: 'string' },
  { code: 'username', name: '用户名', dataType: 'string' },
  { code: 'role', name: '角色', dataType: 'enum' },
  { code: 'org', name: '所属组织', dataType: 'string' },
]
export const USERGROUP_FIELDS = [
  { code: 'group_code', name: '用户组编码', dataType: 'string' },
  { code: 'group_name', name: '用户组名称', dataType: 'string' },
]
export const SUBJECT_META = Object.fromEntries(SUBJECTS.map(s => [s.key, s]))

/* dt: any=不限 / num=仅数值或日期 / str=仅非数值; noValue=该运算符不需要比较值 */
export const OPERATORS = [
  { key: 'eq', label: '等于', dt: 'any' },
  { key: 'ne', label: '不等于', dt: 'any' },
  { key: 'gt', label: '大于', dt: 'num' },
  { key: 'lt', label: '小于', dt: 'num' },
  { key: 'ge', label: '大于等于', dt: 'num' },
  { key: 'le', label: '小于等于', dt: 'num' },
  { key: 'contains', label: '包含', dt: 'str' },
  { key: 'startsWith', label: '开头是', dt: 'str' },
  { key: 'in', label: '包含于', dt: 'any' },
  { key: 'regex', label: '匹配正则', dt: 'str' },
  { key: 'empty', label: '为空', dt: 'any', noValue: true },
  { key: 'notEmpty', label: '不为空', dt: 'any', noValue: true },
]
export const OP_META = Object.fromEntries(OPERATORS.map(o => [o.key, o]))
export const OP_LABEL = Object.fromEntries(OPERATORS.map(o => [o.key, o.label]))
export const NO_VALUE_OPS = OPERATORS.filter(o => o.noValue).map(o => o.key)

/* 老 key → 新 key。提交标准历史上存的是全小写 notempty */
const OP_ALIAS = { notempty: 'notEmpty', notempy: 'notEmpty', not_empty: 'notEmpty', startswith: 'startsWith' }
export function normalizeOp(op) {
  const k = String(op ?? '').trim()
  return OP_ALIAS[k] || OP_ALIAS[k.toLowerCase()] || k
}

/** 按 key 取子集并保持给定顺序 — 调用点用它维持各自现有的运算符列表 */
export function pickOps(keys) { return keys.map(k => OP_META[k]).filter(Boolean) }

export function numLike(dt) { return /(int|decimal|double|float|number|numeric|date|time)/i.test(String(dt || '')) }
export function needValue(op) { return !NO_VALUE_OPS.includes(normalizeOp(op)) }

/** 按字段数据类型过滤可用运算符: 字符串字段不给「大于」, 数值字段不给「包含」 */
export function opsFor(dataType, list = OPERATORS) {
  const isNum = numLike(dataType)
  return list.filter(o => o.dt === 'any' || (o.dt === 'num' ? isNum : !isNum))
}

/** 可用于条件比较的标量类型: 排除对象引用 / 结构 / 注解 */
export function comparable(dt) { return !/(object|ref|struct|annotation|entity)/i.test(String(dt || '')) }

export function dtLabel(dt) {
  const s = String(dt || '').toLowerCase()
  if (!s) return ''
  if (/(int|decimal|double|float|number|numeric)/.test(s)) return '数值'
  if (s.includes('bool')) return '布尔'
  if (s.includes('datetime') || s.includes('timestamp')) return '日期时间'
  if (s.includes('date')) return '日期'
  if (s.includes('enum')) return '枚举'
  if (/(object|ref|entity)/.test(s)) return '对象引用'
  if (s.includes('text') || s.includes('clob')) return '长文本'
  return '字符串'
}

/* —— 结构工厂 —— */
let seq = 0
export function condUid() { return 'cn-' + Date.now().toString(36) + '-' + (seq++) }
export function emptyCond(subject = '') {
  return { _k: condUid(), type: 'cond', subject, field: '', fieldName: '', dataType: '', operator: '', value: '' }
}
export function emptyGroup(logic = 'all') { return { _k: condUid(), type: 'group', logic, children: [] } }

/* —— 摘要与完整性 —— */
export function condReady(c) {
  if (!c || !c.subject || !c.field || !c.operator) return false
  if (!needValue(c.operator)) return true
  return String(c.value ?? '').trim() !== ''
}
export function condText(c, placeholder = '在右侧配置条件…') {
  if (!c || !c.subject) return placeholder
  const who = SUBJECT_META[c.subject]?.label || c.subject
  const f = c.fieldName || c.field || '(未选属性)'
  const op = OP_LABEL[normalizeOp(c.operator)] || '(未选运算符)'
  if (!needValue(c.operator)) return `${who} ${f} ${op}`
  return `${who} ${f} ${op} ${String(c.value ?? '').trim() || '(未填值)'}`
}
export function groupSummary(g, empty = '(未配置条件)') {
  if (!g || !g.children?.length) return empty
  const parts = g.children.map(ch => ch.type === 'group' ? `( ${groupSummary(ch, empty)} )` : condText(ch))
  const body = parts.join(g.logic === 'any' ? ' 或 ' : ' 且 ')
  return g.logic === 'none' ? `非( ${body} )` : body
}

/* —— 数据类型视觉元数据 (与参数详情头部的类型胶囊同源) —— */
export const DATA_TYPE_META = {
  string:  { icon: 'textType',    color: '#2563eb', label: '字符串' },
  number:  { icon: 'hash',        color: '#1f2937', label: '数值' },
  boolean: { icon: 'check',       color: '#10b981', label: '布尔' },
  object:  { icon: 'cube',        color: '#8b5cf6', label: '对象引用' },
  date:    { icon: 'calendar',    color: '#0891b2', label: '日期' },
  enum:    { icon: 'checkSquare', color: '#dc2626', label: '枚举' },
}
/* xsd:decimal / xsd:dateTime 等原始数据类型收敛到上面 6 类 */
export function mapXsd(dt) {
  const s = String(dt || '').toLowerCase()
  if (s.includes('enum')) return 'enum'
  if (/(int|decimal|double|float)/.test(s)) return 'number'
  if (s.includes('bool')) return 'boolean'
  if (s.includes('date') || s.includes('time')) return 'date'
  return 'string'
}
export function dtMeta(t) { return DATA_TYPE_META[t] || DATA_TYPE_META.string }
