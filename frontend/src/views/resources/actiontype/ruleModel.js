/* 规则编辑器共用常量与结构工厂 — 供 ActionDetailWorkspace 与各 Rule*Editor 共用 */

export const PROP_OPERATORS = { set: '赋值', add: '增加', sub: '减少', append: '追加', clear: '清空' }

/* 用户在本体里是对象, 取值时要指明取它的哪个属性 (value_source=3 的二级选择) */
export const USER_ATTR_OPTS = [
  { value: 'user_id', label: '用户 ID' }, { value: 'username', label: '用户名' },
  { value: 'role', label: '角色' }, { value: 'org', label: '所属部门' },
]
export const PROP_OPERATOR_OPTS = Object.entries(PROP_OPERATORS).map(([v, l]) => ({ value: v, label: l }))

export function newMapping() {
  return { property_code: '', property_name: '', prop_operator: 'set', value_source: 1, value_content: '',
    param_name: '', default_type: 'static', default_source: '', is_required: 0 }
}
export function newObjLink() { return { link_type_code: '', peer_param: '' } }
export function newWhParam() { return { name: '', param_type: 'string', value_source: 1, value_content: '' } }

/* —— 函数规则 —— */
export const FUNC_UPGRADE_OPTS = [{ value: 1, label: '自动升级到兼容版本' }, { value: 0, label: '锁定当前版本' }]
export const FUNC_IDENTITY_OPTS = [{ value: 'caller', label: '以调用者身份执行' }, { value: 'service', label: '以服务账号身份执行' }]
export const FUNC_ERR_OPTS = [{ value: 'rollback', label: '中断操作,回滚所有 Ontology 变更' }, { value: 'continue', label: '继续执行,记录异常日志' }]

/* —— 通知规则 —— */
export const NOTIFY_RECIPIENT_SRC = [{ value: 'object_prop', label: '来自对象参数属性' }, { value: 'param', label: '来自参数' }, { value: 'static', label: '静态指定' }]
export const NOTIFY_LINK_TYPES = [{ value: 'object_detail', label: '对象详情区' }, { value: 'external', label: '外部链接' }, { value: 'action', label: '触发其它动作' }]

/* —— Webhook —— */
export const WH_SUBTYPES = [
  { value: 'writeback', label: '回写', icon: 'edit', desc: '使用回写模式编辑外部数据系统。外部系统返回结构响应可用于其他动作编辑规则。如果回写执行失败,所有动作编辑都不会生效,错误会立即显示给终端用户。' },
  { value: 'sideeffect', label: '副作用', icon: 'zap', desc: '副作用模式在本体对象修改完成、事务提交后执行。支持配置多个副作用 Webhook;执行失败不影响主操作结果,用户可看到有物或待提示后异步执行。' },
]
