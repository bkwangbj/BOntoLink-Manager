<template>
  <transition name="pd-drawer">
    <aside v-if="open" class="pd-drawer" :style="{ width: width + 'px' }">
      <div class="pd-drag" @mousedown="onDragStart" :class="resizing && 'is-resizing'"></div>

      <!-- 顶部 -->
      <div class="pd-hd">
        <div class="bl-row" style="gap:8px;flex:1;min-width:0">
          <span class="pd-ic" :style="{ background: typeColor }" v-html="BL.icon(typeIcon, 16, '#fff')"></span>
          <div class="bl-grow" style="min-width:0">
            <div class="pd-title bl-truncate">
              {{ form.display_name || form.rdfs_label || form.api_name || '新建属性' }}
              <span class="bl-mono bl-muted" style="font-size:11px;font-weight:400">（{{ form.api_name || '—' }}）</span>
            </div>
            <div class="bl-mono bl-muted" style="font-size:11px">
              所属类: {{ className || '—' }} · <span :class="['pd-type-tag', typeClass]">{{ typeLabel }}</span>
            </div>
          </div>
        </div>
        <button class="bl-btn bl-btn-text bl-btn-icon" @click="onCancel" v-html="BL.icon('x', 14)"></button>
      </div>

      <!-- 左导航 + 右内容 -->
      <div class="pd-body">
        <nav class="pd-nav">
          <button v-for="n in navItems" :key="n.k"
                  :class="['pd-nav-item', activeNav === n.k && 'is-on']"
                  @click="onPickNav(n.k)">
            <span v-html="BL.icon(n.icon, 12)"></span>
            <span style="margin-left:8px">{{ n.label }}</span>
            <span v-if="n.badge != null" class="pd-badge">{{ n.badge }}</span>
          </button>
        </nav>

        <div class="pd-pane" ref="paneRef" @scroll="onPaneScroll">
          <!-- 1. 基础信息 -->
          <section ref="sec_basic" data-sec="basic">
            <div class="pd-sec">基础信息</div>
            <div class="pd-grid-2">
              <FieldRow label="属性代码 *" inline hint="snake_case · 类内唯一 · 创建后不可改">
                <input class="bl-input bl-mono" v-model="form.api_name" :disabled="!isNew" />
              </FieldRow>
              <FieldRow label="属性名称 *" inline>
                <input class="bl-input" v-model="form.display_name" placeholder="中文名称" />
              </FieldRow>
              <FieldRow label="属性类型" inline>
                <select class="bl-input" v-model="form.prop_type">
                  <option value="data">数据属性</option>
                  <option value="object">对象属性</option>
                  <option value="annotation">注释属性</option>
                </select>
              </FieldRow>
              <FieldRow label="派生字段" inline>
                <label class="pd-switch"><input type="checkbox" :checked="form.is_derived" :disabled="form.prop_type!=='data'" @change="form.is_derived = $event.target.checked ? 1 : 0" />计算派生</label>
              </FieldRow>
              <FieldRow v-if="form.is_derived" label="计算函数" :inline="false" style="grid-column:1/-1">
                <textarea class="bl-textarea bl-mono" rows="3" v-model="form.computeExpr" placeholder="例: lng * 100 + lat"></textarea>
              </FieldRow>
              <FieldRow label="数据类型" inline>
                <select class="bl-input" v-model="form.data_type" :disabled="form.prop_type==='object'">
                  <option value="">—</option>
                  <option v-for="t in xsdTypes" :key="t" :value="t">{{ t }}</option>
                </select>
              </FieldRow>
              <FieldRow label="值类型" inline>
                <select class="bl-input" v-model="form.value_type">
                  <option value="">— 无 —</option>
                  <option v-for="vt in valueTypeOptions" :key="vt.id" :value="vt.id">{{ vt.rdfs_label || vt.api_name }}</option>
                </select>
              </FieldRow>
              <FieldRow label="是否主键" inline>
                <label class="pd-switch"><input type="checkbox" :checked="form.is_key" :disabled="form.prop_type!=='data'" @change="form.is_key = $event.target.checked ? 1 : 0" />主键</label>
              </FieldRow>
              <FieldRow label="是否必填" inline>
                <label class="pd-switch"><input type="checkbox" :checked="form.is_required" @change="form.is_required = $event.target.checked ? 1 : 0" />必填</label>
              </FieldRow>
              <FieldRow label="属性 ID" inline>
                <div class="bl-row" style="gap:4px;flex:1;min-width:0">
                  <input class="bl-input bl-mono" :value="form.id || '(待生成)'" readonly />
                  <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" :title="copied==='id'?'已复制':'复制 ID'" @click="copy(form.id, 'id')"
                          v-html="BL.icon(copied==='id'?'check':'copy', 12)"></button>
                </div>
              </FieldRow>
              <FieldRow label="状态" inline>
                <select class="bl-input" v-model.number="form.status">
                  <option :value="1">启用</option>
                  <option :value="0">禁用</option>
                </select>
              </FieldRow>
            </div>
          </section>

          <!-- 2. 表映射 (单列三行,避免输入框过窄) -->
          <section ref="sec_mapping" data-sec="mapping" v-if="form.prop_type !== 'annotation' && !form.is_derived">
            <div class="pd-sec">表映射</div>
            <div class="pd-grid-1">
              <FieldRow label="数据源" inline>
                <select class="bl-input" v-model="form.class_ds_id">
                  <option value="">— 无 —</option>
                  <option v-for="d in datasources" :key="d.id" :value="d.id">{{ d.alias }} · {{ d.physical_table }}</option>
                </select>
              </FieldRow>
              <FieldRow label="物理表" inline>
                <input class="bl-input bl-mono" v-model="form.physical_table" />
              </FieldRow>
              <FieldRow label="物理字段" inline>
                <input class="bl-input bl-mono" v-model="form.physical_column" />
              </FieldRow>
            </div>
          </section>

          <!-- 3. OWL 特性 -->
          <section ref="sec_owl" data-sec="owl" v-if="form.prop_type !== 'annotation'">
            <div class="pd-sec">OWL 特性</div>
            <div class="pd-grid-2">
              <FieldRow label="函数型" inline hint="一个主体实例只能有一个值"><label class="pd-switch"><input type="checkbox" :checked="form.owl_functional" @change="toggleOwl('owl_functional', $event)" />functional</label></FieldRow>
              <FieldRow label="反函数型" inline hint="一个值只能对应一个主体实例"><label class="pd-switch"><input type="checkbox" :checked="form.owl_inverse_functional" @change="form.owl_inverse_functional = $event.target.checked ? 1 : 0" />inverseFunctional</label></FieldRow>
              <FieldRow v-if="form.prop_type === 'object'" label="对称性" inline><label class="pd-switch"><input type="checkbox" :checked="form.owl_symmetric" @change="toggleOwl('owl_symmetric', $event)" />symmetric</label></FieldRow>
              <FieldRow v-if="form.prop_type === 'object'" label="非对称性" inline><label class="pd-switch"><input type="checkbox" :checked="form.owl_asymmetric" @change="toggleOwl('owl_asymmetric', $event)" />asymmetric</label></FieldRow>
              <FieldRow v-if="form.prop_type === 'object'" label="自反性" inline><label class="pd-switch"><input type="checkbox" :checked="form.owl_reflexive" @change="toggleOwl('owl_reflexive', $event)" />reflexive</label></FieldRow>
              <FieldRow v-if="form.prop_type === 'object'" label="禁自反性" inline><label class="pd-switch"><input type="checkbox" :checked="form.owl_irreflexive" @change="toggleOwl('owl_irreflexive', $event)" />irreflexive</label></FieldRow>
              <FieldRow v-if="form.prop_type === 'object'" label="传递性" inline><label class="pd-switch"><input type="checkbox" :checked="form.owl_transitive" @change="form.owl_transitive = $event.target.checked ? 1 : 0" />transitive</label></FieldRow>
            </div>
            <div v-if="owlWarn" class="pd-warn">⚠ {{ owlWarn }}</div>
          </section>

          <!-- 4. 属性约束 -->
          <section ref="sec_constraint" data-sec="constraint" v-if="form.prop_type !== 'annotation'">
            <div class="pd-sec">属性约束</div>
            <div v-if="form.prop_type === 'object'" class="pd-grid-1">
              <FieldRow label="关联类 *" inline>
                <select class="bl-input" v-model="form.range_class_id">
                  <option value="">— 请选择 —</option>
                  <option v-for="c in classCandidates" :key="c.id" :value="c.id">{{ c.display_name || c.api_name }}</option>
                </select>
              </FieldRow>
            </div>
            <template v-else>
              <div class="pd-grid-3">
                <FieldRow label="多值属性" inline>
                  <label class="pd-switch"><input type="checkbox" :checked="form.is_multi_valued_prop" @change="form.is_multi_valued_prop = $event.target.checked ? 1 : 0" />允许多值</label>
                </FieldRow>
                <FieldRow label="值域约束" inline>
                  <label class="pd-switch"><input type="checkbox" :checked="form.is_range_constraint_prop" @change="form.is_range_constraint_prop = $event.target.checked ? 1 : 0" />启用</label>
                </FieldRow>
                <div></div>
                <FieldRow label="最小长度" inline><input type="number" class="bl-input" v-model.number="form.xsd_min_length" /></FieldRow>
                <FieldRow label="最大长度" inline><input type="number" class="bl-input" v-model.number="form.xsd_max_length" /></FieldRow>
                <FieldRow label="固定长度" inline><input type="number" class="bl-input" v-model.number="form.xsd_length" /></FieldRow>
                <FieldRow label="最小值" inline><input class="bl-input bl-mono" v-model="form.xsd_min_inclusive" placeholder="minInclusive" /></FieldRow>
                <FieldRow label="最大值" inline><input class="bl-input bl-mono" v-model="form.xsd_max_inclusive" placeholder="maxInclusive" /></FieldRow>
                <div></div>
              </div>
              <FieldRow label="正则表达式" hint="JS RegExp 语法。优先级最高,会覆盖长度约束">
                <input class="bl-input bl-mono" v-model="form.xsd_pattern" placeholder="例: ^[A-Z]{2}\d{6}$" />
              </FieldRow>
            </template>
          </section>

          <!-- 5. 等价属性 -->
          <section ref="sec_equiv" data-sec="equiv">
            <div class="pd-sec-row">
              <div class="pd-sec">等价属性</div>
              <button class="bl-btn bl-btn-sm bl-btn-text" @click="$emit('navigateToTab', 'propEquiv')">
                <span v-html="BL.icon('arrowRight', 12)"></span>
                <span style="margin-left:4px">前往等价属性管理</span>
              </button>
            </div>
            <div class="bl-muted" style="font-size:12px;margin-bottom:8px">
              只能添加同类型属性 (数据↔数据,对象↔对象),语义完全相同可互相替换。
            </div>
            <table class="bl-table pd-mini-table">
              <thead><tr><th>属性编码</th><th>对象类</th><th style="width:80px">状态</th></tr></thead>
              <tbody>
                <tr v-for="(r, i) in equivList" :key="i">
                  <td class="bl-mono">{{ r.other_api_name || r.ref_property_id }}</td>
                  <td>{{ r.other_class_label || '—' }}</td>
                  <td><span class="pp-status is-on">激活</span></td>
                </tr>
              </tbody>
            </table>
            <div v-if="!equivList.length" class="bl-empty" style="padding:18px;font-size:12px">尚未配置等价属性关系</div>
          </section>

          <!-- 6. 互斥属性 -->
          <section ref="sec_disjoint" data-sec="disjoint">
            <div class="pd-sec-row">
              <div class="pd-sec">互斥属性</div>
              <button class="bl-btn bl-btn-sm bl-btn-text" @click="$emit('navigateToTab', 'propDisjoint')">
                <span v-html="BL.icon('arrowRight', 12)"></span>
                <span style="margin-left:4px">前往不相交属性管理</span>
              </button>
            </div>
            <div class="bl-muted" style="font-size:12px;margin-bottom:8px">
              只能添加同类型属性,两个属性不能同时为真。
            </div>
            <table class="bl-table pd-mini-table">
              <thead><tr><th>属性编码</th><th>对象类</th><th style="width:80px">状态</th></tr></thead>
              <tbody>
                <tr v-for="(r, i) in disjointList" :key="i">
                  <td class="bl-mono">{{ r.other_api_name || r.ref_property_id }}</td>
                  <td>{{ r.other_class_label || '—' }}</td>
                  <td><span class="pp-status is-on">激活</span></td>
                </tr>
              </tbody>
            </table>
            <div v-if="!disjointList.length" class="bl-empty" style="padding:18px;font-size:12px">尚未配置互斥属性关系</div>
          </section>

          <!-- 7. 属性注释 -->
          <section ref="sec_note" data-sec="note">
            <div class="pd-sec">属性注释</div>
            <FieldRow label="属性说明">
              <textarea class="bl-textarea" rows="3" v-model="form.rdfs_comment" placeholder="对该属性的业务说明"></textarea>
            </FieldRow>
            <div class="pd-grid-2">
              <FieldRow label="参考资料" inline><input class="bl-input" v-model="form.rdfs_see_also" placeholder="rdfs:seeAlso URL" /></FieldRow>
              <FieldRow label="定义来源" inline><input class="bl-input" v-model="form.rdfs_defined_by" placeholder="rdfs:isDefinedBy" /></FieldRow>
            </div>
            <FieldRow label="元数据 JSON" hint="自由扩展字段,需为合法 JSON">
              <textarea class="bl-textarea bl-mono" rows="3" v-model="form.metadata" placeholder='{"key":"value"}'></textarea>
            </FieldRow>
          </section>
        </div>
      </div>

      <!-- 底部 -->
      <div class="pd-ft">
        <span class="bl-muted" style="font-size:11px">
          {{ isNew ? '新建属性' : '编辑属性' }} · 保存后立即应用到系统
        </span>
        <div class="bl-row" style="gap:8px">
          <button class="bl-btn" @click="onCancel">取消</button>
          <button class="bl-btn bl-btn-primary" @click="onSave">{{ isNew ? '创建' : '保存' }}</button>
        </div>
      </div>
    </aside>
  </transition>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import FieldRow from '@/views/config/category/FieldRow.vue'
import { BL } from '@/lib/bl.js'
import { classMetaApi, valueTypeApi } from '@/api'

const props = defineProps({
  open: { type: Boolean, default: false },
  classId: { type: String, default: '' },
  className: { type: String, default: '' },
  property: { type: Object, default: null }, // 编辑模式: 完整属性对象;新建: null
  datasources: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:open', 'saved', 'navigateToTab'])

const xsdTypes = ['xsd:string','xsd:decimal','xsd:integer','xsd:boolean','xsd:dateTime','xsd:date','xsd:anyURI']

const navItems = computed(() => [
  { k: 'basic',      label: '基础信息', icon: 'fileText' },
  ...((form.prop_type !== 'annotation' && !form.is_derived) ? [{ k: 'mapping', label: '表映射', icon: 'database' }] : []),
  ...(form.prop_type !== 'annotation' ? [{ k: 'owl',  label: 'OWL 特性', icon: 'link' }] : []),
  ...(form.prop_type !== 'annotation' ? [{ k: 'constraint', label: '属性约束', icon: 'sliders' }] : []),
  { k: 'equiv',    label: '等价属性', icon: 'check', badge: equivList.value.length },
  { k: 'disjoint', label: '互斥属性', icon: 'x',     badge: disjointList.value.length },
  { k: 'note',     label: '属性注释', icon: 'chat' }
])

const isNew = computed(() => !props.property || !props.property.id || String(props.property.id).startsWith('new_'))

/* —— 表单 —— */
const defaultForm = () => ({
  id: '', api_name: '', display_name: '', rdfs_label: '', rdfs_comment: '',
  rdfs_see_also: '', rdfs_defined_by: '', metadata: '',
  prop_type: 'data', data_type: 'xsd:string', value_type: '',
  is_required: 0, is_key: 0, is_derived: 0, is_primary: 0,
  is_multi_valued_prop: 0, is_range_constraint_prop: 0, range_class_id: '',
  class_ds_id: '', physical_table: '', physical_column: '',
  xsd_min_length: null, xsd_max_length: null, xsd_length: null,
  xsd_pattern: '', xsd_min_inclusive: '', xsd_max_inclusive: '',
  owl_functional: 0, owl_inverse_functional: 0, owl_transitive: 0,
  owl_symmetric: 0, owl_asymmetric: 0, owl_reflexive: 0, owl_irreflexive: 0,
  status: 1,
  computeExpr: ''  // 派生字段表达式 (仅前端)
})
const form = reactive(defaultForm())

const valueTypeOptions = ref([])
const classCandidates = ref([])
const equivList = ref([])
const disjointList = ref([])

/* —— OWL 互斥规则 —— */
const owlWarn = ref('')
function toggleOwl(field, ev) {
  const v = ev.target.checked ? 1 : 0
  form[field] = v
  // 互斥规则
  const pairs = [
    ['owl_symmetric', 'owl_asymmetric'],
    ['owl_reflexive', 'owl_irreflexive']
  ]
  for (const [a, b] of pairs) {
    if (field === a && v === 1) form[b] = 0
    if (field === b && v === 1) form[a] = 0
  }
  // 提示
  if ((form.owl_symmetric && form.owl_asymmetric) || (form.owl_reflexive && form.owl_irreflexive)) {
    owlWarn.value = '对称/非对称、自反/禁自反互斥,不能同时开启'
  } else {
    owlWarn.value = ''
  }
}

/* —— 类型标签 —— */
const typeIcon = computed(() => {
  if (form.is_derived) return 'zap'
  if (form.prop_type === 'object') return 'link'
  if (form.prop_type === 'annotation') return 'chat'
  return 'database'
})
const typeColor = computed(() => {
  if (form.is_derived) return '#722ED1'
  if (form.prop_type === 'object') return '#FF7D00'
  if (form.prop_type === 'annotation') return '#00B42A'
  return '#1677ff'
})
const typeLabel = computed(() => {
  if (form.is_derived) return '计算派生属性'
  if (form.prop_type === 'object') return '对象属性'
  if (form.prop_type === 'annotation') return '注释属性'
  return '数据属性'
})
const typeClass = computed(() => {
  if (form.is_derived) return 'is-derived'
  if (form.prop_type === 'object') return 'is-object'
  if (form.prop_type === 'annotation') return 'is-annotation'
  return 'is-data'
})

/* —— 重新装载: 打开时 OR 切换 property 时 —— */
async function reloadForm() {
  if (!props.open) return
  // 初始化表单
  Object.assign(form, defaultForm())
  if (props.property) Object.assign(form, props.property)
  // 加载值类型 / 类候选 (仅首次)
  if (!valueTypeOptions.value.length) {
    valueTypeOptions.value = (await valueTypeApi.list().catch(() => [])).filter(v => v.status === 1)
  }
  if (!classCandidates.value.length) {
    classCandidates.value = await classMetaApi.candidates().catch(() => [])
  }
  // 加载当前属性的等价 / 互斥关系
  if (props.classId && !isNew.value) {
    const all = await classMetaApi.listPropEquiv(props.classId).catch(() => [])
    equivList.value = all.filter(r => r.property_id === form.id || r.ref_property_id === form.id)
    const allD = await classMetaApi.listPropDisjoint(props.classId).catch(() => [])
    disjointList.value = allD.filter(r => r.property_id === form.id || r.ref_property_id === form.id)
  } else {
    equivList.value = []; disjointList.value = []
  }
  activeNav.value = 'basic'
  owlWarn.value = ''
  nextTick(() => paneRef.value?.scrollTo({ top: 0 }))
}
/* 打开 (false → true) 时载入 */
watch(() => props.open, (v) => { if (v) reloadForm() })
/* 抽屉已打开,切换到不同属性时也要刷新内容 (按 id 监听,避免引用相同但内部已改) */
watch(() => props.property?.id, () => { if (props.open) reloadForm() })

/* —— 左导航 / 滚动联动 —— */
const activeNav = ref('basic')
const paneRef = ref(null)
const sec_basic = ref(null)
const sec_mapping = ref(null)
const sec_owl = ref(null)
const sec_constraint = ref(null)
const sec_equiv = ref(null)
const sec_disjoint = ref(null)
const sec_note = ref(null)
const secMap = () => ({ basic: sec_basic.value, mapping: sec_mapping.value, owl: sec_owl.value, constraint: sec_constraint.value, equiv: sec_equiv.value, disjoint: sec_disjoint.value, note: sec_note.value })
function onPickNav(k) {
  activeNav.value = k
  const target = secMap()[k]
  if (!target || !paneRef.value) return
  paneRef.value.scrollTo({ top: Math.max(0, target.offsetTop - 8), behavior: 'smooth' })
}
function onPaneScroll() {
  if (!paneRef.value) return
  const top = paneRef.value.scrollTop + 30
  const order = ['note','disjoint','equiv','constraint','owl','mapping','basic']
  for (const k of order) {
    const el = secMap()[k]
    if (el && top >= el.offsetTop) { activeNav.value = k; return }
  }
}

/* —— 复制 —— */
const copied = ref('')
function copy(text, tag) {
  if (!text) return
  navigator.clipboard?.writeText(String(text)).then(() => {
    copied.value = tag
    setTimeout(() => { if (copied.value === tag) copied.value = '' }, 1500)
  })
}

/* —— 拖拽宽度 —— */
const width = ref(0)
const resizing = ref(false)
const MIN_W = 520, MAX_PCT = 0.85
function ensureSize() {
  if (!width.value) {
    const saved = parseInt(localStorage.getItem('bl.pd-drawer.width') || '0', 10)
    width.value = saved && saved >= MIN_W ? saved : Math.max(MIN_W, Math.min(820, Math.floor(window.innerWidth * 0.55)))
  }
}
let dragStartX = 0, dragStartW = 0
function onDragStart(e) {
  resizing.value = true
  dragStartX = e.clientX; dragStartW = width.value
  document.body.style.cursor = 'col-resize'; document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onDragMove); window.addEventListener('mouseup', onDragEnd)
}
function onDragMove(e) {
  const dx = dragStartX - e.clientX
  width.value = Math.min(Math.floor(window.innerWidth * MAX_PCT), Math.max(MIN_W, dragStartW + dx))
}
function onDragEnd() {
  resizing.value = false
  document.body.style.cursor = ''; document.body.style.userSelect = ''
  localStorage.setItem('bl.pd-drawer.width', String(width.value))
  window.removeEventListener('mousemove', onDragMove); window.removeEventListener('mouseup', onDragEnd)
}
watch(() => props.open, (v) => { if (v) ensureSize() })
onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onDragMove); window.removeEventListener('mouseup', onDragEnd)
})

/* —— 保存 / 取消 —— */
async function onSave() {
  if (!form.api_name) { BL.warning('属性代码必填'); activeNav.value = 'basic'; return }
  if (isNew.value && !/^[a-z][a-z0-9_]*$/.test(form.api_name)) { BL.warning('属性代码需 snake_case (小写+下划线)'); return }
  if (!form.display_name) { BL.warning('属性名称必填'); activeNav.value = 'basic'; return }
  if (form.prop_type === 'object' && !form.range_class_id) { BL.warning('对象属性必须选择关联类'); activeNav.value = 'constraint'; return }
  // 字段互斥
  if (form.prop_type === 'object') form.data_type = ''
  if (form.prop_type !== 'object') form.range_class_id = ''

  const { computeExpr, ...payload } = form
  payload.prop_code = payload.prop_code || payload.api_name
  try {
    if (isNew.value) {
      delete payload.id
      await classMetaApi.addProp(props.classId, payload)
    } else {
      await classMetaApi.updateProp(form.id, payload)
    }
    BL.success('已保存')
    emit('saved')
    emit('update:open', false)
  } catch (e) {
    BL.error(e?.msg || '保存失败')
  }
}
function onCancel() { emit('update:open', false) }
</script>

<style scoped>
/* 属性详情抽屉 — 嵌套于对象抽屉之上,层级再加 1 (1010 > 对象抽屉 1000) */
.pd-drawer {
  position: fixed; top: 0; right: 0; bottom: 0;
  background: var(--bl-bg-1);
  border-left: 1px solid var(--bl-border);
  box-shadow: -4px 0 16px rgba(0,0,0,.12);
  display: flex; flex-direction: column;
  min-width: 520px; z-index: 1010;
}
.pd-drag { position: absolute; left: -2px; top: 0; bottom: 0; width: 5px;
  cursor: col-resize; transition: background-color .15s; z-index: 1011; }
.pd-drag:hover, .pd-drag.is-resizing { background: var(--bl-primary); }

.pd-hd { flex-shrink: 0; display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; border-bottom: 1px solid var(--bl-divider); }
.pd-ic { width: 32px; height: 32px; border-radius: 6px;
  display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.pd-title { font-size: 15px; font-weight: 600; }

.pd-type-tag { display: inline-block; padding: 1px 7px; border-radius: 9px; font-size: 10.5px; font-weight: 500; }
.pd-type-tag.is-data { background: #e8f3ff; color: #1677ff; }
.pd-type-tag.is-object { background: #fff5e6; color: #FF7D00; }
.pd-type-tag.is-annotation { background: #e8fff4; color: #00B42A; }
.pd-type-tag.is-derived { background: #f5edff; color: #722ED1; }

.pd-body { flex: 1; display: grid; grid-template-columns: 160px 1fr; overflow: hidden; min-height: 0; }
.pd-nav {
  background: #f8f9fb; border-right: 1px solid var(--bl-divider);
  padding: 8px 6px; display: flex; flex-direction: column; gap: 2px; overflow: auto;
}
.pd-nav-item {
  text-align: left; padding: 9px 12px; border: 0; background: transparent;
  cursor: pointer; font-size: 13px; color: var(--bl-text-2);
  border-radius: 4px; border-left: 3px solid transparent;
  display: inline-flex; align-items: center;
}
.pd-nav-item:hover { background: var(--bl-bg-1); color: var(--bl-text-1); }
.pd-nav-item.is-on { background: #e8f3ff; color: #1677ff; font-weight: 500; border-left-color: #1677ff; }
.pd-badge { margin-left: auto; font-size: 11px; color: #999;
  background: var(--bl-bg-2); border-radius: 9px; padding: 0 7px; min-width: 18px; text-align: center; }
.pd-nav-item.is-on .pd-badge { background: #fff; color: #1677ff; }

.pd-pane { overflow: auto; padding: 12px 18px; min-width: 0; position: relative; }
.pd-pane > section { padding: 8px 0 14px; border-bottom: 1px dashed var(--bl-divider); margin-bottom: 4px; }
.pd-pane > section:last-child { border-bottom: 0; }

.pd-sec { font-size: 13px; font-weight: 600; color: var(--bl-text-1);
  padding-left: 8px; border-left: 3px solid #1677ff; margin: 6px 0 10px; }
.pd-sec-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.pd-sec-row .pd-sec { margin: 0; flex: 1; }

.pd-grid-1 { display: grid; grid-template-columns: 1fr; gap: 4px 16px; }
.pd-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 4px 16px; }
.pd-grid-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 4px 16px; }

/* 开关样式 (复用普通 checkbox label) */
.pd-switch { display: inline-flex; align-items: center; gap: 6px; cursor: pointer; font-size: 13px; user-select: none; }
.pd-switch input { cursor: pointer; }

.pd-warn { padding: 8px 12px; background: #fff7e6; color: #fa8c16;
  border: 1px solid #ffd591; border-radius: 4px; font-size: 12px; margin-top: 8px; }

.pd-mini-table { width: 100%; font-size: 12px; }
.pd-mini-table th { background: #fafafa; padding: 6px 8px; font-weight: 600; text-align: left; }
.pd-mini-table td { padding: 6px 8px; border-top: 1px solid var(--bl-divider); }

.pd-ft {
  flex-shrink: 0; display: flex; justify-content: space-between; align-items: center;
  padding: 10px 14px; border-top: 1px solid var(--bl-divider); gap: 8px;
}

/* 统一收窄 inline label 宽度 — 最长 5 字符 (属性代码 *) 仅需 ~70px */
.pd-drawer :deep(.fr.fr-inline .fr-label) { width: 72px; flex: 0 0 72px; }
/* 非 inline 模式 (label 在上的多行项) 也收紧 */
.pd-drawer :deep(.fr .fr-label) { font-size: 12.5px; }

.pd-drawer-enter-active, .pd-drawer-leave-active { transition: transform .22s, opacity .18s; }
.pd-drawer-enter-from, .pd-drawer-leave-to { transform: translateX(20px); opacity: 0; }

/* 借用 pp-status 样式定义 (复用 TabProps 中的状态样式) */
.pp-status { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 11px; font-weight: 500; }
.pp-status.is-on  { background: #e8fff4; color: #00b42a; }
.pp-status.is-off { background: #f2f3f5; color: #666; }
</style>
