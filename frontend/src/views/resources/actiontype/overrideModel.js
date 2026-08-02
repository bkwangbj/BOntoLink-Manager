/* 覆盖规则 (Overrides) 数据模型与摘要生成 — 文档 1.4.4.3
 * 运算符与类型兼容判断统一取自 conditionModel, 此处只保留覆盖规则特有的语义(动作、模板、冗余判定)。
 */
import { pickOps, opsFor as opsForAll, numLike as numLikeAll, needValue, normalizeOp, condReady as condReadyAll } from './conditionModel.js'

let seq = 0
export function ovUid() { return 'ov-' + Date.now().toString(36) + '-' + (seq++) }

/* Then 动作类型: 可见 / 必填 / 禁用 */
export const OV_ACTIONS = [
  { value: 'visible', label: '可见', bool: true },
  { value: 'required', label: '必填', bool: true },
  { value: 'disabled', label: '禁用', bool: true },
]
/* 已下线的动作类型 — 仅用于老数据回显, 不再出现在下拉里 */
const OV_LEGACY_ACTIONS = [
  { value: 'default', label: '默认值', bool: false },
  { value: 'constraint', label: '约束规则', bool: false },
]
export const OV_ACTION_LABEL = Object.fromEntries([...OV_ACTIONS, ...OV_LEGACY_ACTIONS].map(a => [a.value, a.label]))
export function ovActionMeta(type) { return OV_ACTIONS.find(a => a.value === type) || OV_LEGACY_ACTIONS.find(a => a.value === type) || null }

/* 条件模板: 文档只允许「基于当前用户」「基于参数」两类 */
export const OV_SUBJECTS = [
  { key: 'user', label: '当前用户', desc: '根据用户身份、角色、用户组触发覆盖规则', icon: 'user', color: '#722ED1' },
  { key: 'param', label: '表单参数', desc: '根据表单内上方字段的取值触发覆盖规则', icon: 'edit', color: '#00B42A' },
]
export const OV_USER_FIELDS = [
  { code: 'user_group', name: '用户组', dataType: 'multi' },
  { code: 'user_id', name: '用户ID', dataType: 'string' },
  { code: 'role', name: '角色', dataType: 'enum' },
  { code: 'org', name: '所属部门', dataType: 'string' },
]

/* 覆盖规则沿用的运算符子集与顺序 */
export const OV_OPERATORS = pickOps(['eq', 'ne', 'contains', 'startsWith', 'gt', 'lt', 'empty', 'notEmpty'])
export const OV_NO_VALUE_OPS = OV_OPERATORS.filter(o => o.noValue).map(o => o.key)
export const OV_OP_LABEL = Object.fromEntries(OV_OPERATORS.map(o => [o.key, o.label]))
const LOGIC_LABEL = { all: '全部满足', any: '任一满足', none: '全部不满足' }

export const numLike = numLikeAll
export function opsFor(dataType) { return opsForAll(dataType, OV_OPERATORS) }

/* —— 空白结构 —— */
export function emptyCondGroup() { return { _k: ovUid(), type: 'group', logic: 'all', children: [] } }
export function emptyBlock(presetType) {
  return {
    _k: ovUid(),
    cond: emptyCondGroup(),
    actions: presetType ? [{ _k: ovUid(), type: presetType, value: presetType === 'disabled' ? 1 : 0 }] : [],
  }
}

/* —— 单条条件 —— */
export const condReady = condReadyAll
export function condText(c) {
  if (!c || !c.subject) return '在右侧配置条件…'
  const who = c.subject === 'user' ? '当前用户' : '参数'
  const f = c.fieldName || c.field || '(未选属性)'
  const op = OV_OP_LABEL[normalizeOp(c.operator)] || '(未选运算符)'
  if (!needValue(c.operator)) return `${who} ${f} ${op}`
  return `${who} ${f} ${op} ${String(c.value ?? '').trim() || '(未填值)'}`
}

/* —— 摘要 —— */
export function ifSummary(group) {
  if (!group || !group.children?.length) return '(未配置条件)'
  const parts = group.children.map(ch => ch.type === 'group' ? `( ${ifSummary(ch)} )` : condText(ch))
  const sep = group.logic === 'any' ? ' 或 ' : ' 且 '
  const body = parts.join(sep)
  return group.logic === 'none' ? `非( ${body} )` : body
}
export function actionText(a) {
  const label = OV_ACTION_LABEL[a.type] || a.type
  const meta = ovActionMeta(a.type)
  if (meta && meta.bool) return `${a.value ? '' : '非'}${label}`
  if (a.type === 'default') return `默认值 = ${String(a.value ?? '').trim() || '(空)'}`
  return `约束: ${String(a.value ?? '').trim() || '(未填)'}`
}
export function thenSummary(actions) {
  if (!actions?.length) return '(未配置动作)'
  return actions.map(actionText).join('、')
}
/* 卡片头部自动生成的规则语义标题 */
export function blockTitle(block) {
  const g = block?.cond
  const first = g?.children?.find(ch => ch.type !== 'group')
  if (!first) return g?.children?.length ? `条件组(${LOGIC_LABEL[g.logic] || ''})` : '未配置条件'
  const more = (g.children.length - 1) > 0 ? ` 等 ${g.children.length} 个条件` : ''
  return `条件: ${condText(first)}${more}`
}
/* 覆盖值与字段全局默认一致 → 冗余 */
export function isRedundant(a, defaults) {
  const meta = ovActionMeta(a.type)
  if (!meta || !meta.bool || !defaults) return false
  return Number(defaults[a.type] ?? -1) === Number(a.value)
}

/* —— 旧数据迁移: {target,value,condition} → {cond,actions} —— */
export function normalizeOverrides(list) {
  return (list || []).map(o => {
    if (o && Array.isArray(o.actions)) {
      return {
        _k: o._k || ovUid(),
        cond: normalizeGroup(o.cond),
        actions: o.actions.map(a => ({ _k: a._k || ovUid(), type: a.type || 'visible', value: a.value ?? 0 })),
      }
    }
    /* 旧结构: 条件只是一段自由文本, 迁移成一条未配置条件并把原文放进值里备查 */
    const b = emptyBlock(o?.target || 'visible')
    b.actions[0].value = o?.value ?? 0
    if (String(o?.condition || '').trim()) {
      b.cond.children.push({ _k: ovUid(), type: 'cond', subject: 'param', field: '', fieldName: `(旧条件) ${o.condition}`,
        dataType: 'string', operator: '', value: '' })
    }
    return b
  })
}
function normalizeGroup(g) {
  if (!g || !Array.isArray(g.children)) return emptyCondGroup()
  return {
    _k: g._k || ovUid(), type: 'group', logic: g.logic || 'all',
    children: g.children.map(ch => ch.type === 'group' ? normalizeGroup(ch)
      : { _k: ch._k || ovUid(), type: 'cond', subject: ch.subject || '', field: ch.field || '', fieldName: ch.fieldName || '',
          dataType: ch.dataType || '', operator: normalizeOp(ch.operator), value: ch.value ?? '' }),
  }
}
/* 存库前剔除内部 key */
export function serializeOverrides(list) {
  const g = (n) => ({ logic: n.logic, children: n.children.map(ch => ch.type === 'group' ? { type: 'group', ...g(ch) }
    : { type: 'cond', subject: ch.subject, field: ch.field, fieldName: ch.fieldName, dataType: ch.dataType, operator: ch.operator, value: ch.value }) })
  return (list || []).map(b => ({ cond: g(b.cond), actions: b.actions.map(a => ({ type: a.type, value: a.value })) }))
}
