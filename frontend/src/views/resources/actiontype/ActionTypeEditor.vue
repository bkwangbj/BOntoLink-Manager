<template>
  <Teleport to="body">
    <transition name="ate-drawer">
      <aside v-if="open" class="ate-drawer" :style="{ width: width + 'px' }">
        <div class="ate-handle" @mousedown="onHandleDown" :class="resizing && 'is-resizing'"></div>

        <!-- 头部 -->
        <header class="ate-hd">
          <div class="ate-hd-l">
            <span class="ate-ic ate-ic-lg" :style="{ background: headColor }"
                  v-html="BL.icon(form.icon || headIcon, 18, '#fff')"></span>
            <div class="ate-title-wrap">
              <div class="ate-title bl-truncate" :title="form.rdfs_label || '新建动作'">
                {{ form.rdfs_label || '新建动作' }}
                <span v-if="form.api_name" class="bl-mono bl-muted ate-code">- {{ form.api_name }}</span>
              </div>
              <div class="ate-meta" v-if="form.id">
                <span class="bl-tag" :style="typeTagStyle">{{ typeLabel }}</span>
                <span :class="['bl-tag', statusTagCls(form.status)]" style="margin-left:6px">{{ statusLabel(form.status) }}</span>
                <span class="bl-muted" style="margin-left:6px">更新于 {{ shortTime(form.update_time) }}</span>
              </div>
            </div>
          </div>
          <div class="ate-hd-r">
            <button :class="['bl-btn bl-btn-text bl-btn-sm ate-edit-btn', editMode && 'is-edit-on']"
                    :title="editMode ? '关闭编辑 · 切回只读' : '开启编辑模式'"
                    @click="editMode = !editMode">
              <span v-html="BL.icon(editMode ? 'edit' : 'lock', 12)"></span>
              <span style="margin-left:4px">{{ editMode ? '编辑' : '只读' }}</span>
            </button>
            <span class="ate-divider"></span>
            <button class="bl-btn bl-btn-text bl-btn-icon" :title="isMax ? '还原' : '最大化'"
                    @click="toggleMax" v-html="BL.icon(isMax ? 'minimize' : 'maximize', 13)"></button>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="onClose" v-html="BL.icon('x', 14)"></button>
          </div>
        </header>

        <!-- Tab 导航 -->
        <nav class="ate-tabs">
          <button v-for="t in TABS" :key="t.k" :class="['ate-tab', activeTab === t.k && 'is-on']" @click="activeTab = t.k">
            {{ t.label }}
            <span v-if="t.count !== undefined && tabCount(t.k)" class="ate-tab-badge">{{ tabCount(t.k) }}</span>
          </button>
        </nav>

        <div class="ate-body" :class="{ 'is-readonly': !editMode }">
          <!-- ========== 概览 ========== -->
          <div v-show="activeTab === 'overview'">
            <div class="sec">基础信息</div>
            <div class="ate-grid">
              <label class="ate-fld"><span class="ate-lbl">动作名称 <i>*</i></span>
                <input class="bl-input" v-model="form.rdfs_label" :disabled="!editMode" placeholder="如: 新建监测样地" /></label>
              <label class="ate-fld"><span class="ate-lbl">动作编码 <i>*</i></span>
                <input class="bl-input bl-mono" v-model="form.api_name" :disabled="!editMode || !!form.id" placeholder="create_plot" /></label>
              <label class="ate-fld"><span class="ate-lbl">动作大类</span>
                <BlSelect v-model="form.m_type" :options="M_TYPE_OPTS" :disabled="!editMode || !!form.id" @change="onMTypeChange" /></label>
              <label class="ate-fld"><span class="ate-lbl">动作类型</span>
                <BlSelect v-model="form.action_type" :options="actionTypeSelectOpts" :disabled="!editMode" /></label>
              <label class="ate-fld" v-if="form.m_type === 1"><span class="ate-lbl">关联对象类</span>
                <SearchSelect v-model="form.object_class_id" :options="objectClassOptions" :disabled="!editMode"
                              placeholder="请选择对象类" @change="onSubjectChange" /></label>
              <label class="ate-fld" v-else-if="form.m_type === 2"><span class="ate-lbl">关联链接类型</span>
                <SearchSelect v-model="form.link_type_id" :options="linkTypeOptions" :disabled="!editMode"
                              placeholder="请选择链接类型" /></label>
              <label class="ate-fld" v-else-if="form.m_type === 3"><span class="ate-lbl">函数编码</span>
                <input class="bl-input bl-mono" v-model="form.function_code" :disabled="!editMode" placeholder="function code" /></label>
              <label class="ate-fld"><span class="ate-lbl">所属领域</span>
                <BlSelect v-model="form.category_code" :options="editorDomainOptions" :disabled="!editMode" clearable placeholder="未分类" /></label>
              <label class="ate-fld"><span class="ate-lbl">按钮文案</span>
                <input class="bl-input" v-model="form.button_text" :disabled="!editMode" placeholder="展示在详情/批量操作栏" /></label>
              <label class="ate-fld"><span class="ate-lbl">状态</span>
                <BlSelect v-model="form.status" :options="STATUS_OPTS" :disabled="!editMode" /></label>
              <label class="ate-fld"><span class="ate-lbl">图标颜色</span>
                <div class="ate-icon-row">
                  <input class="bl-input" v-model="form.icon" :disabled="!editMode" placeholder="图标名" style="flex:1" />
                  <input class="ate-color" type="color" v-model="form.color" :disabled="!editMode" />
                </div></label>
            </div>
            <div class="sec">展示与能力</div>
            <div class="ate-switch-row">
              <label class="ate-sw"><input type="checkbox" v-model="form.show_on_detail" :true-value="1" :false-value="0" :disabled="!editMode" /> 详情页展示</label>
              <label class="ate-sw"><input type="checkbox" v-model="form.show_on_batch" :true-value="1" :false-value="0" :disabled="!editMode" /> 批量场景展示</label>
              <label class="ate-sw"><input type="checkbox" v-model="form.form_enabled" :true-value="1" :false-value="0" :disabled="!editMode" /> 启用表单</label>
              <label class="ate-sw"><input type="checkbox" v-model="form.submit_criteria_enabled" :true-value="1" :false-value="0" :disabled="!editMode" /> 启用提交标准</label>
            </div>
            <div class="sec">元数据</div>
            <div class="ate-grid">
              <label class="ate-fld ate-fld-full"><span class="ate-lbl">描述 (rdfs:comment)</span>
                <textarea class="bl-input" v-model="form.rdfs_comment" :disabled="!editMode" rows="2"></textarea></label>
              <label class="ate-fld"><span class="ate-lbl">当前版本</span>
                <input class="bl-input bl-mono" v-model="form.current_version" :disabled="!editMode" placeholder="如 v1" /></label>
              <label class="ate-fld"><span class="ate-lbl">RID</span>
                <input class="bl-input bl-mono" :value="form.rid || '(保存后自动生成)'" disabled /></label>
            </div>
          </div>

          <!-- ========== 规则 ========== -->
          <div v-show="activeTab === 'rules'">
            <!-- 编辑类规则 -->
            <div class="ate-grp-hd">
              <div class="ate-grp-title"><span class="ate-grp-dot" style="background:#165DFF"></span>编辑类规则 <span class="bl-muted" style="font-size:11.5px">(动作写入对象属性的映射)</span></div>
              <button class="bl-btn bl-btn-sm" :disabled="!editMode" @click="addRule(1)"><span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">添加规则</span></button>
            </div>
            <div v-for="(rule, ri) in editRules" :key="rule._k" class="ate-rule-card">
              <div class="ate-rule-hd">
                <input class="bl-input bl-input-sm" v-model="rule.rule_name" :disabled="!editMode" placeholder="规则名称" style="max-width:240px" />
                <span style="flex:1"></span>
                <button class="bl-btn bl-btn-text bl-btn-sm" :disabled="!editMode" @click="addMapping(rule)"><span v-html="BL.icon('plus', 11)"></span><span style="margin-left:3px">加属性</span></button>
                <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon at-del-op" :disabled="!editMode" title="删除规则" @click="removeRule(rule)" v-html="BL.icon('trash', 12)"></button>
              </div>
              <table class="bl-table ate-mini-table">
                <thead><tr><th class="t-left">写入属性</th><th class="t-left">操作</th><th class="t-left">值来源</th><th class="t-left">值内容</th><th class="t-center">必填</th><th class="t-center"></th></tr></thead>
                <tbody>
                  <tr v-for="(m, mi) in rule.prop_mappings" :key="mi">
                    <td>
                      <BlSelect v-if="classProps.length" v-model="m.property_code" :options="classPropsOptions" :disabled="!editMode" size="sm" clearable placeholder="属性编码" />
                      <input v-else class="bl-input bl-input-xs bl-mono" v-model="m.property_code" :disabled="!editMode" placeholder="属性编码" />
                    </td>
                    <td><BlSelect v-model="m.prop_operator" :options="PROP_OPERATOR_OPTS" :disabled="!editMode" size="sm" /></td>
                    <td><BlSelect v-model="m.value_source" :options="VALUE_SOURCE_OPTS" :disabled="!editMode" size="sm" /></td>
                    <td><input class="bl-input bl-input-xs" v-model="m.value_content" :disabled="!editMode || m.value_source===3 || m.value_source===4" :placeholder="valuePlaceholder(m.value_source)" /></td>
                    <td class="t-center"><input type="checkbox" v-model="m.is_required" :true-value="1" :false-value="0" :disabled="!editMode" /></td>
                    <td class="t-center"><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" :disabled="!editMode" @click="rule.prop_mappings.splice(mi,1)" v-html="BL.icon('x', 11)"></button></td>
                  </tr>
                  <tr v-if="!rule.prop_mappings.length"><td colspan="6" class="bl-muted" style="text-align:center;padding:10px;font-size:12px">暂无属性映射,点「加属性」</td></tr>
                </tbody>
              </table>
            </div>
            <div v-if="!editRules.length" class="bl-empty" style="padding:24px;font-size:12px">暂无编辑类规则</div>

            <!-- 副作用规则 -->
            <div class="ate-grp-hd" style="margin-top:18px">
              <div class="ate-grp-title"><span class="ate-grp-dot" style="background:#722ED1"></span>副作用规则 <span class="bl-muted" style="font-size:11.5px">(联动创建/删除链接、调用函数、通知、Webhook)</span></div>
              <button class="bl-btn bl-btn-sm" :disabled="!editMode" @click="addRule(2)"><span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">添加规则</span></button>
            </div>
            <div v-for="rule in sideRules" :key="rule._k" class="ate-rule-card">
              <div class="ate-rule-hd">
                <input class="bl-input bl-input-sm" v-model="rule.rule_name" :disabled="!editMode" placeholder="规则名称" style="max-width:240px" />
                <input class="bl-input bl-input-sm bl-mono" v-model="rule.link_type_code" :disabled="!editMode" placeholder="目标编码 (链接/函数/模板)" style="max-width:220px;margin-left:8px" />
                <span style="flex:1"></span>
                <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon at-del-op" :disabled="!editMode" title="删除规则" @click="removeRule(rule)" v-html="BL.icon('trash', 12)"></button>
              </div>
              <textarea class="bl-input" v-model="rule.rule_config" :disabled="!editMode" rows="2" placeholder='副作用配置 JSON (如 {"channel":"email"}); 详细可视化配置后续完善'></textarea>
            </div>
            <div v-if="!sideRules.length" class="bl-empty" style="padding:24px;font-size:12px">暂无副作用规则</div>
          </div>

          <!-- ========== 表单 ========== -->
          <div v-show="activeTab === 'form'">
            <div class="ate-grp-hd">
              <div class="ate-grp-title">表单参数 <span class="bl-muted" style="font-size:11.5px">(动作执行时的输入表单)</span></div>
              <div class="bl-row" style="gap:8px">
                <button v-if="form.m_type === 1 && form.object_class_id" class="bl-btn bl-btn-sm" :disabled="!editMode" @click="importParams"><span v-html="BL.icon('download', 12)"></span><span style="margin-left:4px">从对象属性导入</span></button>
                <button class="bl-btn bl-btn-sm bl-btn-primary" :disabled="!editMode" @click="addParam"><span v-html="BL.icon('plus', 12, '#fff')"></span><span style="margin-left:4px">添加参数</span></button>
              </div>
            </div>
            <table class="bl-table ate-mini-table">
              <thead><tr><th class="t-left">参数编码</th><th class="t-left">参数名称</th><th class="t-left">类型</th><th class="t-center">必填</th><th class="t-left">值来源</th><th class="t-left">默认值/写入属性</th><th class="t-center"></th></tr></thead>
              <tbody>
                <tr v-for="(p, i) in formParams" :key="i">
                  <td><input class="bl-input bl-input-xs bl-mono" v-model="p.param_code" :disabled="!editMode" placeholder="param_code" /></td>
                  <td><input class="bl-input bl-input-xs" v-model="p.param_name" :disabled="!editMode" placeholder="参数名称" /></td>
                  <td><BlSelect v-model="p.param_type" :options="PARAM_TYPE_OPTS" :disabled="!editMode" size="sm" /></td>
                  <td class="t-center"><input type="checkbox" v-model="p.is_required" :true-value="1" :false-value="0" :disabled="!editMode" /></td>
                  <td><BlSelect v-model="p.value_source" :options="VALUE_SOURCE_OPTS" :disabled="!editMode" size="sm" /></td>
                  <td><input v-if="p.value_source===5" class="bl-input bl-input-xs bl-mono" v-model="p.property_code" :disabled="!editMode" placeholder="写入属性编码" /><input v-else class="bl-input bl-input-xs" v-model="p.default_value" :disabled="!editMode" placeholder="默认值" /></td>
                  <td class="t-center"><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" :disabled="!editMode" @click="formParams.splice(i,1)" v-html="BL.icon('trash', 12)"></button></td>
                </tr>
                <tr v-if="!formParams.length"><td colspan="7" class="bl-muted" style="text-align:center;padding:16px;font-size:12px">暂无参数</td></tr>
              </tbody>
            </table>
            <div class="ate-hint">「显示」「覆盖」「详情」等参数细粒度配置(控件类型/条件覆盖块/关联对象展示)将在后续迭代中补充。</div>
          </div>

          <!-- ========== 安全与提交 ========== -->
          <div v-show="activeTab === 'submit'">
            <div class="sec">提交标准</div>
            <label class="ate-sw" style="margin-bottom:12px"><input type="checkbox" v-model="submit.enabled" :true-value="1" :false-value="0" :disabled="!editMode" /> 启用提交标准校验</label>
            <template v-if="submit.enabled">
              <div class="ate-grp-hd" style="margin-bottom:8px"><div class="ate-grp-title">执行规则 <span class="bl-muted" style="font-size:11.5px">(条件与逻辑运算符组合;根级规则全部满足才可提交)</span></div></div>
              <div :class="{ 'ate-ro-mask': !editMode }">
                <ConditionGroup :node="submitTree" :depth="0" :object-fields="editorObjectFields" :param-fields="editorParamFields" />
              </div>
              <label class="ate-fld ate-fld-full" style="margin-top:14px"><span class="ate-lbl">校验失败提示</span>
                <textarea class="bl-input" v-model="submit.error_message" :disabled="!editMode" rows="2" placeholder="不满足提交条件时向用户展示的提示"></textarea></label>
            </template>
            <div class="ate-hint">校验模式由根条件组的「全部 / 任一」决定;支持嵌套逻辑组与「当前用户 / 当前对象」字段条件。</div>
          </div>
        </div>

        <!-- 底部 -->
        <footer class="ate-ft">
          <button v-if="form.id" class="bl-btn bl-btn-text ate-del" @click="onDelete"><span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">删除</span></button>
          <span style="flex:1"></span>
          <button class="bl-btn" @click="onClose">取消</button>
          <button class="bl-btn bl-btn-primary" :disabled="!editMode || saving" @click="onSave">{{ saving ? '保存中…' : '保存' }}</button>
        </footer>
      </aside>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { actionTypeApi, categoryApi, resourceApi } from '@/api'
import SearchSelect from '@/components/SearchSelect.vue'
import BlSelect from '@/components/BlSelect.vue'
import ConditionGroup from './ConditionGroup.vue'

const props = defineProps({
  open: Boolean,
  actionId: String,
  allClasses: { type: Array, default: () => [] },
  allLinkTypes: { type: Array, default: () => [] },
  initCategory: String,
})
const emit = defineEmits(['update:open', 'saved', 'deleted'])

const TABS = [
  { k: 'overview', label: '概览' },
  { k: 'rules', label: '规则', count: true },
  { k: 'form', label: '表单', count: true },
  { k: 'submit', label: '安全与提交' },
]
const M_TYPES = { 1:{label:'对象',icon:'box',color:'#165DFF'}, 2:{label:'链接',icon:'link',color:'#14C9C9'}, 3:{label:'函数',icon:'code',color:'#722ED1'}, 4:{label:'Webhook',icon:'zap',color:'#FF7D00'}, 5:{label:'接口',icon:'plug',color:'#0FC6C2'}, 6:{label:'通知',icon:'bell',color:'#B71DE8'} }
const ACTION_TYPES = {
  11:{label:'创建对象',color:'#00B42A',icon:'plus'}, 12:{label:'修改对象',color:'#165DFF',icon:'edit'}, 13:{label:'Upsert 对象',color:'#165DFF',icon:'edit'}, 14:{label:'删除对象',color:'#F53F3F',icon:'trash'},
  21:{label:'创建链接',color:'#14C9C9',icon:'link'}, 22:{label:'删除链接',color:'#F53F3F',icon:'link'}, 30:{label:'函数',color:'#722ED1',icon:'code'}, 40:{label:'Webhook',color:'#FF7D00',icon:'zap'},
  51:{label:'接口·创建',color:'#0FC6C2',icon:'plug'}, 52:{label:'接口·修改',color:'#0FC6C2',icon:'plug'}, 53:{label:'接口·删除',color:'#0FC6C2',icon:'plug'}, 54:{label:'接口·查询',color:'#0FC6C2',icon:'plug'}, 60:{label:'通知',color:'#B71DE8',icon:'bell'},
}
const M_TYPE_ACTIONS = { 1:[11,12,13,14], 2:[21,22], 3:[30], 4:[40], 5:[51,52,53,54], 6:[60] }
const PARAM_TYPES = ['string','number','boolean','object','date']
const VALUE_SOURCES = { 1:'表单参数', 2:'静态值', 3:'当前用户', 4:'系统时间', 5:'关联对象属性' }
const PROP_OPERATORS = { set:'赋值', add:'增加', sub:'减少', append:'追加', clear:'清空' }
const M_TYPE_OPTS = Object.entries(M_TYPES).map(([k, m]) => ({ value: Number(k), label: m.label }))
const STATUS_OPTS = [{ value: 0, label: '草稿' }, { value: 1, label: '已发布' }, { value: 2, label: '已停用' }]
const VALUE_SOURCE_OPTS = Object.entries(VALUE_SOURCES).map(([v, l]) => ({ value: Number(v), label: l }))
const PROP_OPERATOR_OPTS = Object.entries(PROP_OPERATORS).map(([v, l]) => ({ value: v, label: l }))
const PARAM_TYPE_OPTS = PARAM_TYPES.map(t => ({ value: t, label: t }))

function defaultForm() {
  return { id:'', rid:'', api_name:'', m_type:1, action_type:11, object_class_id:'', link_type_id:'', function_code:'',
    category_code:'', button_text:'', icon:'', color:'#165DFF', show_on_detail:0, show_on_batch:0, form_enabled:0,
    submit_criteria_enabled:0, compile_status:0, current_version:'', status:0, rdfs_label:'', rdfs_comment:'', metadata:null }
}
const form = reactive(defaultForm())
const rules = ref([])
const formParams = ref([])
const submit = reactive({ enabled:0, validate_mode:'all', error_message:'' })
const classProps = ref([])
const submitTree = reactive({ logic: 'all', children: [] })
const editorObjectFields = computed(() => classProps.value.map(p => ({ code: p.code, name: p.name, dataType: p.dataType })))
const editorParamFields = computed(() => formParams.value.map(p => ({ code: p.param_code, name: p.param_name || p.param_code, dataType: p.param_type })))
let etk = 0
/* 扁平节点 → 条件树 */
function buildSubmitTree(nodes) {
  submitTree.logic = 'all'; submitTree.children = []
  if (!Array.isArray(nodes) || !nodes.length) return
  const root = nodes.find(n => !n.parent_id)
  if (!root) return
  const childrenOf = (pid) => nodes.filter(n => n.parent_id === pid).sort((a, b) => (a.sort || 0) - (b.sort || 0)).map(n => {
    if (n.node_type === 'group') return { _k: 'ek-' + (etk++), type: 'group', logic: n.logic_op || 'all', children: childrenOf(n.id) }
    const parts = String(n.left_code || '').split(':')
    const subj = parts[0] === 'user' ? 'user' : parts[0] === 'param' ? 'param' : 'object'
    return { _k: 'ek-' + (etk++), type: 'cond', subject: subj, field: parts[1] || '', operator: n.operator || 'eq', value: n.right_value || '' }
  })
  submitTree.logic = root.logic_op || 'all'
  submitTree.children = childrenOf(root.id)
}
/* 条件树 → 扁平节点 (parent_id 引用) */
function flattenSubmitTree() {
  const out = []; let c = 0
  const walk = (node, parentId, sort) => {
    const id = 'cn-' + (++c); const isGroup = node.type !== 'cond'
    out.push({ id, parent_id: parentId, sort, node_type: isGroup ? 'group' : 'condition', logic_op: isGroup ? (node.logic || 'all') : null,
      left_code: isGroup ? null : `${node.subject}:${node.field || ''}`, operator: isGroup ? null : (node.operator || null),
      right_value: isGroup ? null : (node.value || null), value_source: isGroup ? null : (node.subject === 'user' ? 3 : null) })
    if (isGroup && node.children) node.children.forEach((ch, i) => walk(ch, id, i))
  }
  walk(submitTree, null, 0)
  return out
}
const activeTab = ref('overview')
const editMode = ref(true)
const saving = ref(false)
let ruleKey = 0

const actionTypeOpts = computed(() => (M_TYPE_ACTIONS[form.m_type] || []).map(v => ({ v, label: ACTION_TYPES[v]?.label || v })))
const actionTypeSelectOpts = computed(() => actionTypeOpts.value.map(t => ({ value: t.v, label: t.label })))
const classPropsOptions = computed(() => classProps.value.map(p => ({ value: p.code, label: `${p.name} (${p.code})` })))
const editorDomainOptions = computed(() => domainOpts.value.map(d => ({ value: d.code, label: (d.indent || '') + d.label })))
const classOptions = computed(() => (props.allClasses || []).map(c => ({ id:c.id, cn:c.display_name||c.rdfs_label||c.api_name, api_name:c.api_name, category_code:c.category_code })))
const objectClassOptions = computed(() => classOptions.value.map(c => ({ value: c.id, label: `${c.cn} (${c.api_name})` })))
const linkTypeOptions = computed(() => (props.allLinkTypes || []).map(l => ({ value: l.id, label: l.rdfs_label || l.link_type_id })))
const headMeta = computed(() => ACTION_TYPES[Number(form.action_type)] || M_TYPES[form.m_type] || { color:'#165DFF', icon:'zap' })
const headColor = computed(() => form.color || headMeta.value.color)
const headIcon = computed(() => headMeta.value.icon)
const typeLabel = computed(() => headMeta.value.label || '—')
const typeTagStyle = computed(() => { const c = headMeta.value.color || '#86909c'; return { background:`color-mix(in srgb, ${c} 12%, transparent)`, color:c, border:`1px solid color-mix(in srgb, ${c} 30%, transparent)` } })
const editRules = computed(() => rules.value.filter(r => Number(r.rule_type) === 1))
const sideRules = computed(() => rules.value.filter(r => Number(r.rule_type) === 2))
function tabCount(k) { return k === 'rules' ? rules.value.length : k === 'form' ? formParams.value.length : 0 }

function onMTypeChange() {
  const opts = M_TYPE_ACTIONS[form.m_type] || []
  if (!opts.includes(Number(form.action_type))) form.action_type = opts[0]
  if (form.m_type !== 1) form.object_class_id = ''
  if (form.m_type !== 2) form.link_type_id = ''
  if (form.m_type !== 3) form.function_code = ''
  if (!form.id) form.color = M_TYPES[form.m_type]?.color || form.color
}
function onSubjectChange() {
  const c = (props.allClasses || []).find(x => x.id === form.object_class_id)
  if (c && c.category_code && !form.category_code) form.category_code = c.category_code
  loadClassProps()
}

/* 规则 / 映射 */
function addRule(type) {
  rules.value.push({ _k: ++ruleKey, rule_type: type, rule_name: type === 1 ? '编辑规则' : '副作用规则', target_param_code:'', link_type_code:'', rule_config:'', prop_mappings: [] })
}
function removeRule(rule) { rules.value = rules.value.filter(r => r !== rule) }
function addMapping(rule) { rule.prop_mappings.push({ property_code:'', property_name:'', prop_operator:'set', value_source:1, value_content:'', is_required:0 }) }
function valuePlaceholder(vs) { return ({1:'表单参数编码',2:'静态值',3:'(当前用户,自动)',4:'(系统时间,自动)',5:'关联对象属性'})[Number(vs)] || '' }

/* 参数 */
function addParam() { formParams.value.push({ param_code:'', param_name:'', param_type:'string', is_required:0, value_source:1, default_value:'', property_code:'' }) }
async function importParams() {
  if (!form.object_class_id) return BL.warning('请先选择对象类')
  await loadClassProps()
  const exist = new Set(formParams.value.map(p => p.param_code))
  let n = 0
  for (const p of classProps.value) {
    if (exist.has(p.code)) continue
    formParams.value.push({ param_code:p.code, param_name:p.name, param_type:p.type, is_required:p.required?1:0, value_source:1, default_value:'', property_code:p.code }); n++
  }
  BL.success(`已导入 ${n} 个参数`)
}
async function loadClassProps() {
  if (!form.object_class_id) { classProps.value = []; return }
  const list = await resourceApi.properties(form.object_class_id).catch(() => [])
  const arr = Array.isArray(list) ? list : (list?.data || [])
  classProps.value = arr.map(p => ({ code:p.api_name||p.prop_code, name:p.display_name||p.rdfs_label||p.api_name, type:mapXsd(p.data_type), dataType:p.data_type, required:!!p.is_required }))
}
function mapXsd(dt){ const s=String(dt||'').toLowerCase(); if(s.includes('int')||s.includes('decimal')||s.includes('double')||s.includes('float'))return'number'; if(s.includes('bool'))return'boolean'; if(s.includes('date')||s.includes('time'))return'date'; return'string' }

/* 领域候选 */
const domainOpts = ref([])
async function loadDomainOpts() {
  if (domainOpts.value.length) return
  const tree = await categoryApi.tree().catch(() => [])
  const list = []
  const walk = (ns, depth) => (ns || []).forEach(n => { if (n.categoryCode && n.categoryType === 2) list.push({ code:n.categoryCode, label:n.label||n.rdfsLabel||n.categoryCode, indent:'　'.repeat(depth) }); if (n.children) walk(n.children, depth+1) })
  walk(tree, 0); domainOpts.value = list
}

/* 加载 */
async function loadEditor() {
  loadDomainOpts()
  activeTab.value = 'overview'
  if (props.actionId) {
    const res = await actionTypeApi.get(props.actionId).catch(() => null)
    Object.assign(form, defaultForm(), res || {})
    rules.value = (res?.rules || []).map(r => ({
      _k: ++ruleKey, id:r.id, rule_type:Number(r.rule_type)||1, rule_name:r.rule_name||'', target_param_code:r.target_param_code||'',
      link_type_code:r.link_type_code||'', rule_config:r.rule_config||'',
      prop_mappings: (r.prop_mappings||[]).map(m => ({ property_code:m.property_code||'', property_name:m.property_name||'', prop_operator:m.prop_operator||'set', value_source:Number(m.value_source)||1, value_content:m.value_content||'', is_required:Number(m.is_required)||0 }))
    }))
    formParams.value = (res?.form_params || []).map(p => { const cfg = parseCfg(p.config); return { param_code:p.param_code, param_name:p.param_name, param_type:p.param_type||'string', is_required:Number(p.is_required)||0, value_source:Number(cfg.value_source)||1, default_value:p.default_value||'', property_code:cfg.property_code||'' } })
    const ss = res?.submit_standard
    submit.enabled = Number(ss?.enabled)||0; submit.validate_mode = ss?.validate_mode||'all'; submit.error_message = ss?.error_message||''
    buildSubmitTree(ss?.nodes)
    if (form.object_class_id) loadClassProps()
  } else {
    Object.assign(form, defaultForm())
    if (props.initCategory) form.category_code = props.initCategory
    rules.value = []; formParams.value = []; classProps.value = []
    submit.enabled = 0; submit.validate_mode = 'all'; submit.error_message = ''; buildSubmitTree(null)
  }
  editMode.value = true
}
function parseCfg(s){ try { return JSON.parse(s||'{}') } catch { return {} } }
watch(() => props.open, v => { if (v) loadEditor() })
watch(() => props.actionId, () => { if (props.open) loadEditor() })

/* 保存 */
async function onSave() {
  if (!String(form.rdfs_label||'').trim()) { activeTab.value='overview'; return BL.warning('请填写动作名称') }
  if (!String(form.api_name||'').trim()) { activeTab.value='overview'; return BL.warning('请填写动作编码') }
  saving.value = true
  try {
    const body = { ...form,
      form_params: formParams.value.filter(p => String(p.param_code).trim()).map((p,i) => ({ param_code:p.param_code, param_name:p.param_name, param_type:p.param_type, is_required:p.is_required, default_value:p.value_source===5?null:p.default_value, config:JSON.stringify({value_source:p.value_source, property_code:p.property_code||null}), sort:i })),
      rules: rules.value.map((r,i) => ({ action_type:form.action_type, rule_type:r.rule_type, rule_name:r.rule_name, target_param_code:r.target_param_code||null, link_type_code:r.link_type_code||null, rule_config:r.rule_config||null, sort:i, prop_mappings:(r.prop_mappings||[]).filter(m=>String(m.property_code).trim()).map((m,j)=>({ property_code:m.property_code, property_name:m.property_name||null, prop_operator:m.prop_operator, value_source:m.value_source, value_content:m.value_content||null, is_required:m.is_required, sort:j })) })),
      submit_standard: { enabled:submit.enabled, validate_mode:submitTree.logic, error_message:submit.error_message||null, nodes: submit.enabled ? flattenSubmitTree() : [] },
    }
    // form_enabled / submit_criteria_enabled 与子配置联动
    if (formParams.value.length) body.form_enabled = 1
    body.submit_criteria_enabled = submit.enabled ? 1 : form.submit_criteria_enabled
    if (form.id) { await actionTypeApi.update(form.id, body); BL.success('已保存') }
    else { await actionTypeApi.create(body); BL.success('已创建') }
    emit('saved'); emit('update:open', false)
  } catch (e) { BL.error(e?.msg || '保存失败') } finally { saving.value = false }
}
async function onDelete() {
  const ok = await BL.confirm({ title:'删除动作', content:`确定删除「${form.rdfs_label||form.api_name}」?`, danger:true, okText:'删除' })
  if (!ok) return
  try { await actionTypeApi.remove(form.id); BL.success('已删除'); emit('deleted') } catch (e) { BL.error(e?.msg||'删除失败') }
}
function onClose() { emit('update:open', false) }

/* 宽度 / 拖拽 */
const width = ref(parseInt(localStorage.getItem('bl.ate.width') || '1000'))
const isMax = computed(() => width.value >= 1320)
function toggleMax() { width.value = isMax.value ? 1000 : 1320; localStorage.setItem('bl.ate.width', String(width.value)) }
const resizing = ref(false)
function onHandleDown(ev) {
  ev.preventDefault(); resizing.value = true
  const startX = ev.clientX, baseW = width.value
  function move(e) { width.value = Math.max(760, Math.min(Math.floor(window.innerWidth*0.95), baseW + (startX - e.clientX))) }
  function up() { resizing.value=false; localStorage.setItem('bl.ate.width', String(width.value)); window.removeEventListener('mousemove',move); window.removeEventListener('mouseup',up); document.body.style.userSelect='' }
  document.body.style.userSelect='none'; window.addEventListener('mousemove',move); window.addEventListener('mouseup',up)
}

function statusLabel(s){ return ({0:'草稿',1:'已发布',2:'已停用'})[Number(s)]||'—' }
function statusTagCls(s){ return ({0:'',1:'bl-tag-success',2:'bl-tag-warning'})[Number(s)]||'' }
function shortTime(t){ if(!t)return'—'; return String(t).slice(0,16) }
</script>

<style scoped>
.ate-drawer { position: fixed; top: 0; right: 0; bottom: 0; background: var(--bl-bg-1); border-left: 1px solid var(--bl-border); box-shadow: -4px 0 16px rgba(0,0,0,.10); display: flex; flex-direction: column; min-width: 760px; max-width: 95vw; z-index: 1000; overflow: hidden; }
.ate-handle { position: absolute; left: -2px; top: 0; bottom: 0; width: 5px; cursor: col-resize; transition: background-color .15s; z-index: 6; }
.ate-handle:hover, .ate-handle.is-resizing { background: var(--bl-primary); }
.ate-drawer-enter-active, .ate-drawer-leave-active { transition: transform .25s, opacity .2s; }
.ate-drawer-enter-from, .ate-drawer-leave-to { transform: translateX(20px); opacity: 0; }

.ate-hd { height: 56px; padding: 0 14px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--bl-divider); gap: 8px; flex-shrink: 0; }
.ate-hd-l { display: flex; align-items: center; gap: 10px; min-width: 0; flex: 1; }
.ate-ic { width: 22px; height: 22px; border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ate-ic-lg { width: 36px; height: 36px; border-radius: 8px; }
.ate-title-wrap { min-width: 0; }
.ate-title { font-size: 14px; font-weight: 600; color: var(--bl-text-1); }
.ate-code { font-size: 12px; font-weight: 400; margin-left: 4px; }
.ate-meta { font-size: 11px; color: var(--bl-text-3); margin-top: 2px; display: flex; align-items: center; }
.ate-hd-r { display: inline-flex; align-items: center; gap: 4px; flex-shrink: 0; }
.ate-divider { width: 1px; height: 18px; background: var(--bl-divider); margin: 0 6px; flex-shrink: 0; }
.ate-edit-btn.is-edit-on { color: var(--bl-primary); }

.ate-tabs { display: flex; padding: 0 16px; border-bottom: 1px solid var(--bl-divider); flex-shrink: 0; gap: 2px; }
.ate-tab { padding: 10px 14px; font-size: 13px; color: var(--bl-text-2); cursor: pointer; background: transparent; border: 0; border-bottom: 2px solid transparent; margin-bottom: -1px; display: inline-flex; align-items: center; gap: 6px; }
.ate-tab:hover { color: var(--bl-text-1); }
.ate-tab.is-on { color: var(--bl-primary); border-color: var(--bl-primary); font-weight: 500; }
.ate-tab-badge { min-width: 16px; height: 16px; padding: 0 4px; border-radius: 8px; background: var(--bl-bg-3); color: var(--bl-text-2); font-size: 10px; display: inline-flex; align-items: center; justify-content: center; }
.ate-tab.is-on .ate-tab-badge { background: var(--bl-primary); color: #fff; }

.ate-body { flex: 1; min-height: 0; overflow: auto; padding: 14px 16px 24px; }

/* 原生 select 自定义箭头 (与 SearchSelect 视觉一致) */
select.bl-input {
  appearance: none; -webkit-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2386909c' stroke-width='2.4' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat; background-position: right 10px center;
  padding-right: 28px;
}
select.bl-input.bl-input-xs { background-position: right 7px center; padding-right: 22px; }
:root[data-theme="dark"] select.bl-input {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23a9b0bd' stroke-width='2.4' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
}
.sec { font-size: 12px; color: var(--bl-text-3); font-weight: 600; padding-left: 8px; border-left: 3px solid var(--bl-primary); margin: 16px 0 10px; line-height: 1; }
.sec:first-child { margin-top: 4px; }
.ate-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 16px; }
.ate-fld { display: flex; flex-direction: column; gap: 5px; min-width: 0; }
.ate-fld-full { grid-column: 1 / -1; }
.ate-lbl { font-size: 12px; color: var(--bl-text-2); }
.ate-lbl i { color: #f53f3f; font-style: normal; }
.ate-icon-row { display: flex; gap: 8px; align-items: center; }
.ate-color { width: 40px; height: 30px; padding: 2px; border: 1px solid var(--bl-border); border-radius: var(--bl-radius-2); background: var(--bl-bg-1); cursor: pointer; }
.ate-switch-row { display: flex; flex-wrap: wrap; gap: 8px 20px; }
.ate-sw { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: var(--bl-text-2); cursor: pointer; }
.ate-hint { margin-top: 14px; padding: 10px 12px; background: var(--bl-bg-2); border-radius: var(--bl-radius-2); font-size: 12px; color: var(--bl-text-3); }

/* 规则组 */
.ate-grp-hd { display: flex; align-items: center; justify-content: space-between; margin: 8px 0 10px; }
.ate-grp-title { font-size: 13px; font-weight: 600; display: inline-flex; align-items: center; gap: 7px; }
.ate-grp-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.ate-rule-card { border: 1px solid var(--bl-border); border-radius: 8px; padding: 10px; margin-bottom: 10px; }
.ate-rule-hd { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }
.at-del-op { color: #f53f3f; }

.ate-mini-table { width: 100%; font-size: 12px; }
.ate-mini-table thead th { background: var(--bl-bg-2); font-weight: 600; height: 30px; padding: 0 6px; white-space: nowrap; color: var(--bl-text-2); }
.ate-mini-table thead th.t-left { text-align: left; }
.ate-mini-table td { padding: 3px 5px; border-top: 1px solid var(--bl-divider); }
.ate-mini-table td.t-center { text-align: center; }
.ate-mini-table .bl-input-xs { height: 28px; padding: 0 6px; font-size: 12px; }

.ate-ft { flex-shrink: 0; height: 56px; padding: 0 16px; display: flex; align-items: center; gap: 8px; border-top: 1px solid var(--bl-divider); }
.ate-del { color: #f53f3f; }
.ate-body.is-readonly .bl-input:disabled { background: var(--bl-bg-2); }
.ate-ro-mask { pointer-events: none; opacity: .7; }
</style>
