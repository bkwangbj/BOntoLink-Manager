<template>
  <Teleport to="body">
    <transition name="sp-drawer">
      <aside v-if="open" class="sp-detail" :style="{ width: width + 'px' }">
        <!-- 左边缘拖拽手柄 -->
        <div class="sp-drag-handle" @mousedown="onHandleMouseDown" :class="resizing && 'is-resizing'"></div>

        <!-- 头部: 图标 + 标题 + 编码 / 最大化 / 关闭 -->
        <div class="detail-hd">
          <div class="bl-row" style="gap:10px;flex:1;min-width:0">
            <span class="sp-ic sp-ic-lg" :style="{ background: propTypeColor(form) }"
                  v-html="BL.icon(propTypeIcon(form), 18, '#fff')"></span>
            <div class="bl-grow" style="min-width:0">
              <div class="dh-title bl-truncate">{{ form.rdfs_label || form.prop_code || '共享属性' }}</div>
              <div class="bl-mono bl-muted" style="font-size:11px">{{ form.prop_code || '—' }}</div>
            </div>
          </div>
          <div class="bl-row" style="gap:4px;flex-shrink:0">
            <!-- 编辑模式 (锁/笔 + 文字) -->
            <button :class="['bl-btn bl-btn-text bl-btn-sm spd-edit-btn', editMode && 'is-edit-on']"
                    :title="editMode ? '关闭编辑模式 · 切回只读' : '开启编辑模式'"
                    @click="editMode = !editMode">
              <span v-html="BL.icon(editMode ? 'edit' : 'lock', 12)"></span>
              <span style="margin-left:4px">{{ editMode ? '编辑' : '只读' }}</span>
            </button>
            <span class="spd-hd-divider"></span>
            <button class="bl-btn bl-btn-text bl-btn-icon" :title="isMax ? '恢复' : '最大'"
                    @click="toggleMax"
                    v-html="BL.icon(isMax ? 'minimize' : 'maximize', 14)"></button>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="onClose" v-html="BL.icon('x', 14)"></button>
          </div>
        </div>

        <!-- 页签 -->
        <div class="tabs">
          <div v-for="t in tabs" :key="t.k" :class="['tab', activeTab === t.k && 'is-active']" @click="activeTab = t.k">
            {{ t.label }}
            <span v-if="t.k === 'ref' && (form.ref_count || 0) > 0" class="tab-cnt">{{ form.ref_count }}</span>
          </div>
        </div>

        <!-- 主体 -->
        <div class="detail-body" :class="{ 'is-readonly': !editMode }">

          <!-- 基础信息 -->
          <div v-if="activeTab === 'basic'">
            <div class="sec">基础信息</div>
            <FieldRow label="名称" inline>
              <input class="bl-input" v-model="form.rdfs_label" :disabled="!editMode" />
            </FieldRow>
            <FieldRow label="所属分组" inline>
              <select class="bl-input" v-model="form.group_id" :disabled="!editMode">
                <option value="">— 未分组 —</option>
                <option v-for="g in filteredGroups" :key="g.id" :value="g.id">{{ g.group_name }}</option>
              </select>
            </FieldRow>
            <FieldRow label="属性编码" inline hint="snake_case · 全局唯一 · 创建后不可修改">
              <input class="bl-input bl-mono" :value="form.prop_code" disabled />
            </FieldRow>
            <FieldRow label="属性类型" inline>
              <select class="bl-input" v-model="form.prop_type" :disabled="!editMode">
                <option value="data">data · 数据属性</option>
                <option value="object">object · 对象属性</option>
                <option value="annotation">annotation · 注释属性</option>
              </select>
            </FieldRow>
            <FieldRow label="数据类型" inline>
              <select class="bl-input" v-model="form.data_type" :disabled="!editMode">
                <option value="">—</option>
                <option v-for="t in xsdTypes" :key="t" :value="t">{{ t }}</option>
              </select>
            </FieldRow>
            <FieldRow label="值类型" inline>
              <div class="bl-row" style="gap:6px;flex:1;align-items:center">
                <span :class="['spd-vt-pill', form.value_type && 'is-on']" style="flex-shrink:0">
                  <span v-html="BL.icon('tag', 11)"></span>
                  <span style="margin-left:4px">{{ form.value_type_label || form.value_type || '未设置' }}</span>
                </span>
                <button class="bl-btn bl-btn-sm" :disabled="!editMode" @click="vtpOpen = true">
                  <span v-html="BL.icon('search', 11)"></span>
                  <span style="margin-left:4px">选择</span>
                </button>
                <button v-if="form.value_type" class="bl-btn bl-btn-sm bl-btn-text" :disabled="!editMode"
                        title="清除" @click="form.value_type = null; form.value_type_label = ''"
                        v-html="BL.icon('x', 11)"></button>
              </div>
            </FieldRow>
            <FieldRow label="格式化" inline>
              <div class="bl-row" style="gap:6px;flex:1;align-items:center">
                <span :class="['spd-vt-pill', form.format_enabled && 'is-on']" style="flex-shrink:0">
                  <span v-html="BL.icon('sliders', 11)"></span>
                  <span style="margin-left:4px">{{ form.format_enabled ? '已配置' : '未设置' }}</span>
                </span>
                <button class="bl-btn bl-btn-sm" :disabled="!editMode" @click="openFormat">
                  <span v-html="BL.icon('sliders', 11)"></span>
                  <span style="margin-left:4px">设置</span>
                </button>
              </div>
            </FieldRow>
            <FieldRow label="状态" inline>
              <div class="radio-group">
                <label class="radio-item"><input type="radio" :value="1" v-model.number="form.status" :disabled="!editMode" /> 启用</label>
                <label class="radio-item"><input type="radio" :value="0" v-model.number="form.status" :disabled="!editMode" /> 禁用</label>
              </div>
            </FieldRow>

            <div class="sec">概要</div>
            <div class="kv-row"><span class="kv-lbl">被引用</span><span><b :style="{ color: (form.ref_count || 0) > 0 ? 'var(--bl-primary)' : '#86909c' }">{{ form.ref_count || 0 }}</b> 次</span></div>
            <div class="kv-row"><span class="kv-lbl">创建于</span><span class="bl-muted">{{ shortTime(form.create_time) }}</span></div>
            <div class="kv-row"><span class="kv-lbl">最近更新</span><span class="bl-muted">{{ shortTime(form.update_time) }}</span></div>

            <div class="sec">RID</div>
            <div class="rid-row">
              <span class="bl-mono">{{ form.rid || '—' }}</span>
              <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" :title="copied ? '已复制' : '复制 RID'"
                      @click="copyRid"
                      v-html="BL.icon(copied ? 'check' : 'copy', 12)"></button>
            </div>
          </div>

          <!-- OWL 特性 -->
          <div v-else-if="activeTab === 'owl'">
            <div class="sec">逻辑性质</div>
            <div class="bl-muted" style="font-size:12px;margin: -4px 0 8px;padding-left:8px">
              W3C OWL 公理。<b>函数型</b> 与 <b>多值</b> 互斥;对称/非对称/自反/禁自反仅对对象属性生效。
            </div>
            <FieldRow label="函数型" inline hint="Functional · 同一实例对该属性只有唯一值">
              <input type="checkbox" :checked="!!form.owl_functional" :disabled="!editMode"
                     @change="onOwlToggle('owl_functional', $event.target.checked)" />
            </FieldRow>
            <FieldRow label="反函数型" inline hint="InverseFunctional · 取值唯一标识来源实例">
              <input type="checkbox" v-model="form.owl_inverse_functional" :true-value="1" :false-value="0" :disabled="!editMode" />
            </FieldRow>
            <FieldRow label="多值属性" inline hint="Multi-valued · 允许同一实例上有多个值">
              <input type="checkbox" :checked="!!form.is_multi_valued_prop" :disabled="!editMode"
                     @change="onOwlToggle('is_multi_valued_prop', $event.target.checked)" />
            </FieldRow>

            <div class="sec">对象属性专属</div>
            <FieldRow label="传递性" inline hint="Transitive · 仅对象属性">
              <input type="checkbox" v-model="form.owl_transitive" :true-value="1" :false-value="0" :disabled="!editMode || form.prop_type !== 'object'" />
            </FieldRow>
            <FieldRow label="对称性" inline hint="Symmetric · 仅对象属性">
              <input type="checkbox" v-model="form.owl_symmetric" :true-value="1" :false-value="0" :disabled="!editMode || form.prop_type !== 'object'" />
            </FieldRow>
            <FieldRow label="非对称性" inline hint="Asymmetric · 仅对象属性">
              <input type="checkbox" v-model="form.owl_asymmetric" :true-value="1" :false-value="0" :disabled="!editMode || form.prop_type !== 'object'" />
            </FieldRow>
            <FieldRow label="自反性" inline hint="Reflexive · 仅对象属性">
              <input type="checkbox" v-model="form.owl_reflexive" :true-value="1" :false-value="0" :disabled="!editMode || form.prop_type !== 'object'" />
            </FieldRow>
            <FieldRow label="禁自反" inline hint="Irreflexive · 仅对象属性">
              <input type="checkbox" v-model="form.owl_irreflexive" :true-value="1" :false-value="0" :disabled="!editMode || form.prop_type !== 'object'" />
            </FieldRow>
          </div>

          <!-- 约束 -->
          <div v-else-if="activeTab === 'constraint'">
            <div class="sec">基本约束</div>
            <FieldRow label="是否必填" inline>
              <input type="checkbox" v-model="form.is_required" :true-value="1" :false-value="0" :disabled="!editMode" />
            </FieldRow>
            <FieldRow label="值域约束" inline hint="启用后下方 XSD 约束生效">
              <input type="checkbox" v-model="form.is_range_constraint_prop" :true-value="1" :false-value="0" :disabled="!editMode" />
            </FieldRow>

            <template v-if="form.prop_type === 'data'">
              <div class="sec">XSD 约束 (仅数据属性)</div>
              <FieldRow label="最小长度" inline><input type="number" class="bl-input" v-model.number="form.xsd_min_length" :disabled="!editMode" /></FieldRow>
              <FieldRow label="最大长度" inline><input type="number" class="bl-input" v-model.number="form.xsd_max_length" :disabled="!editMode" /></FieldRow>
              <FieldRow label="固定长度" inline><input type="number" class="bl-input" v-model.number="form.xsd_length" :disabled="!editMode" /></FieldRow>
              <FieldRow label="正则表达式" inline><input class="bl-input bl-mono" v-model="form.xsd_pattern" :disabled="!editMode" placeholder="^[A-Za-z]+$" /></FieldRow>
              <FieldRow label="数值最小值" inline><input class="bl-input" v-model="form.xsd_min_inclusive" :disabled="!editMode" /></FieldRow>
              <FieldRow label="数值最大值" inline><input class="bl-input" v-model="form.xsd_max_inclusive" :disabled="!editMode" /></FieldRow>
            </template>
          </div>

          <!-- 引用 -->
          <div v-else-if="activeTab === 'ref'">
            <div class="sec">引用情况</div>
            <div class="row-between">
              <input class="bl-input" v-model="refQ" placeholder="搜索对象属性 / 对象类" style="width:240px" />
              <span class="bl-muted" style="font-size:12px">共 {{ refsFiltered.length }} 条</span>
            </div>
            <div v-if="refsLoading" class="bl-empty" style="padding:24px;margin-top:8px">加载中...</div>
            <div v-else-if="!refsFiltered.length" class="bl-empty" style="padding:24px;margin-top:8px">该共享属性尚未被引用</div>
            <table v-else class="bl-table sp-ref-table" style="margin-top:8px">
              <thead>
                <tr><th>对象属性</th><th>所属对象类</th><th>领域</th><th>引用时间</th></tr>
              </thead>
              <tbody>
                <tr v-for="r in refsFiltered" :key="r.id">
                  <td><span class="bl-mono">{{ r.api_name }}</span><div class="bl-muted" style="font-size:11px">{{ r.display_name || r.prop_code }}</div></td>
                  <td>{{ r.class_label || r.class_name || '—' }}</td>
                  <td class="bl-muted">{{ r.category_code || '—' }}</td>
                  <td class="bl-muted">{{ shortTime(r.create_time) }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 注释 -->
          <div v-else-if="activeTab === 'note'">
            <div class="sec">语义定义</div>
            <FieldRow label="RDFS 注释" hint="rdfs:comment · 业务规则说明">
              <textarea class="bl-textarea" rows="5" v-model="form.rdfs_comment" :disabled="!editMode" placeholder="业务规则说明"></textarea>
            </FieldRow>
            <FieldRow label="参考资料" hint="rdfs:seeAlso · URL / 文档链接 (可多行)">
              <textarea class="bl-textarea" rows="4" v-model="form.rdfs_see_also" :disabled="!editMode" placeholder="https://..."></textarea>
            </FieldRow>
            <FieldRow label="定义来源" hint="rdfs:isDefinedBy · 来源说明 / 标准编号 / 参考文档">
              <textarea class="bl-textarea" rows="4" v-model="form.rdfs_defined_by" :disabled="!editMode" placeholder="标准编号 / 参考文档..."></textarea>
            </FieldRow>

            <div class="sec">业务元数据公理</div>
            <FieldRow label="元数据 (JSON)" hint="机器可读:接口规则 / 查询约束 / 语义配置">
              <textarea class="bl-textarea bl-mono" rows="8" v-model="form.metadata" :disabled="!editMode" placeholder='{"approval":"strict","query":"indexed"}'></textarea>
            </FieldRow>
          </div>
        </div>

        <!-- 底部操作 -->
        <div class="detail-ft">
          <button v-if="editMode" class="bl-btn bl-btn-danger" :disabled="(form.ref_count || 0) > 0"
                  :title="(form.ref_count || 0) > 0 ? `被引用 ${form.ref_count} 次,无法删除` : '删除'"
                  @click="onDelete">
            <span v-html="BL.icon('trash', 12)"></span>
            <span style="margin-left:4px">删除</span>
          </button>
          <span style="flex:1"></span>
          <button class="bl-btn" @click="onClose">取消</button>
          <button v-if="editMode" class="bl-btn bl-btn-primary" @click="onSave">保存</button>
        </div>
      </aside>
    </transition>

    <!-- 值类型选择面板 -->
    <ValueTypePickerModal v-model:open="vtpOpen"
                          v-model="form.value_type"
                          :multi="false"
                          :subtitle="`为共享属性「${form.rdfs_label || form.prop_code}」选择值类型`"
                          @confirm="onValueTypeConfirm" />

    <!-- 属性格式化 -->
    <PropertyFormatModal v-model:open="fmtOpen"
                         :property-id="form.id"
                         property-scope="shared"
                         :data-type="form.data_type"
                         :subtitle="`共享属性 · ${form.rdfs_label || form.prop_code || ''}`"
                         @saved="onFormatSaved" />
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { BL } from '@/lib/bl.js'
import { sharedPropertyApi, propertyFormatApi, groupApi, groupRefApi } from '@/api'
import FieldRow from '@/views/config/category/FieldRow.vue'
import ValueTypePickerModal from '@/components/ValueTypePickerModal.vue'
import PropertyFormatModal from '@/components/PropertyFormatModal.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  property: { type: Object, default: null }
})
const emit = defineEmits(['update:open', 'saved', 'deleted'])

const xsdTypes = ['xsd:string','xsd:decimal','xsd:integer','xsd:boolean','xsd:dateTime','xsd:date','xsd:anyURI']
const tabs = [
  { k: 'basic',      label: '基础信息' },
  { k: 'owl',        label: 'OWL 特性' },
  { k: 'constraint', label: '约束' },
  { k: 'ref',        label: '引用' },
  { k: 'note',       label: '注释' }
]
const activeTab = ref('basic')
const editMode = ref(localStorage.getItem('bontolink.sp.editMode') === '1')
watch(editMode, v => localStorage.setItem('bontolink.sp.editMode', v ? '1' : '0'))

const form = reactive({})
const groups = ref([])
const spGroupRef = ref(null)
const spGroupIds = ref(new Set())  // 共享属性的分组 id 集合(group_type='shared_props')
/* 只显示共享属性的分组;领域相关组按当前领域收窄;已选分组始终保留兜底 */
const filteredGroups = computed(() => {
  const domain = form.category_code || form.categoryCode || ''
  return groups.value.filter(g =>
    g.id === form.group_id ||
    (spGroupIds.value.has(g.id) && (!g.domain_code || g.domain_code === domain))
  )
})
async function loadSpGroups() {
  if (groups.value.length) return
  const bizGroups = await groupApi.listAll().catch(() => [])
  groups.value = (bizGroups || []).map(g => ({
    id: g.id, parent_id: g.parentId || g.parent_id,
    group_name: g.gname || g.gName || g.g_name || g.group_name || '',
    category_code: g.categoryCode || g.category_code,
    domain_code: g.domainCode || g.domain_code || '',
    status: g.status || 'active'
  }))
  // 共享属性的分组集合(用于"所属分组"下拉按资源类型过滤)
  const spRefs = await groupRefApi.list('shared_props').catch(() => [])
  spGroupIds.value = new Set((spRefs || []).map(r => r.groupId || r.group_id))
}
async function loadSpGroupRef(id) {
  try {
    const refs = await groupRefApi.list('shared_props').catch(() => [])
    const ref = (refs || []).find(r => r.ref_id === id)
    spGroupRef.value = ref || null
    form.group_id = ref ? (ref.groupId || ref.group_id || '') : ''
  } catch { form.group_id = '' }
}
onMounted(() => loadSpGroups())

function resetForm(src) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, src || {})
  if (form.id) loadSpGroupRef(form.id)
}

/* refs/refQ 提前声明以避免 immediate watch 触发 TDZ */
const refs = ref([])
const refsLoading = ref(false)
const refQ = ref('')

watch(() => props.property, (v) => {
  resetForm(v)
  refs.value = []
  refQ.value = ''
}, { immediate: true })
watch(() => props.open, (v) => {
  if (v && form.id) {
    activeTab.value = 'basic'
    loadFormatPreview()
  }
})
watch(activeTab, async (v) => {
  if (v === 'ref' && form.id) await loadRefs()
})

/* —— 宽度: 420-960px, localStorage 持久化 (与 Interfaces 范围一致) —— */
const width = ref(parseInt(localStorage.getItem('bontolink.sp.drawerW') || '560'))
const isMax = computed(() => width.value >= 960)
function clamp(w) { return Math.max(420, Math.min(960, w)) }
watch(width, v => localStorage.setItem('bontolink.sp.drawerW', String(clamp(v))))
function toggleMax() { width.value = isMax.value ? 560 : 960 }

/* 拖动改宽 */
const resizing = ref(false)
function onHandleMouseDown(ev) {
  ev.preventDefault()
  resizing.value = true
  const startX = ev.clientX
  const baseW = width.value
  function onMove(e) { width.value = clamp(baseW + (startX - e.clientX)) }
  function onUp() {
    resizing.value = false
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
    document.body.style.userSelect = ''
  }
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

/* —— 互斥: functional ↔ multi —— */
function onOwlToggle(field, val) {
  if (field === 'owl_functional' && val) {
    if (form.is_multi_valued_prop) {
      BL.warning('函数型与多值互斥,已自动关闭多值')
      form.is_multi_valued_prop = 0
    }
    form.owl_functional = 1
  } else if (field === 'is_multi_valued_prop' && val) {
    if (form.owl_functional) {
      BL.warning('多值与函数型互斥,已自动关闭函数型')
      form.owl_functional = 0
    }
    form.is_multi_valued_prop = 1
  } else {
    form[field] = 0
  }
}

/* —— 格式化预览 —— */
const fmtOpen = ref(false)
async function loadFormatPreview() {
  if (!form.id) { form.format_enabled = false; return }
  try {
    const f = await propertyFormatApi.get(form.id).catch(() => null)
    form.format_enabled = !!(f && f.format_enabled)
  } catch { form.format_enabled = false }
}
function openFormat() {
  if (!form.id) { BL.warning('请先保存属性'); return }
  fmtOpen.value = true
}
async function onFormatSaved() { await loadFormatPreview() }

/* —— 值类型 —— */
const vtpOpen = ref(false)
function onValueTypeConfirm({ rows: picked }) {
  if (picked && picked[0]) {
    form.value_type_label = picked[0].rdfs_label || picked[0].api_name
    if (picked[0].data_type) form.data_type = picked[0].data_type
  }
}

/* —— 引用列表 —— */
const refsFiltered = computed(() => {
  const k = refQ.value.trim().toLowerCase()
  if (!k) return refs.value
  return refs.value.filter(r => [r.api_name, r.display_name, r.class_label, r.class_name]
    .filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
})
async function loadRefs() {
  if (!form.id) return
  refsLoading.value = true
  try { refs.value = await sharedPropertyApi.references(form.id).catch(() => []) }
  finally { refsLoading.value = false }
}

/* —— 保存 / 删除 —— */
async function onSave() {
  try {
    await sharedPropertyApi.update(form.id, { ...form })
    // 同步分组绑定
    try {
      const curRef = spGroupRef.value
      if (form.group_id && form.group_id !== (curRef?.groupId || curRef?.group_id || '')) {
        if (curRef) { await groupRefApi.removeByRef(form.id, 'shared_props'); spGroupRef.value = null }
        await groupRefApi.create({ ref_id: form.id, group_id: form.group_id, group_type: 'shared_props' })
      } else if (!form.group_id && curRef) {
        await groupRefApi.removeByRef(form.id, 'shared_props')
        spGroupRef.value = null
      }
    } catch (e) { /* 分组绑定失败不影响主流程 */ }
    BL.success('已保存')
    emit('saved')
  } catch (e) { BL.error(e?.msg || '保存失败') }
}
async function onDelete() {
  const ok = await BL.confirm({
    title: '删除共享属性',
    content: `确定删除「${form.rdfs_label || form.prop_code}」?`,
    danger: true, okText: '删除'
  })
  if (!ok) return
  try {
    await sharedPropertyApi.remove(form.id)
    BL.success('已删除')
    emit('deleted')
  } catch (e) { BL.error(e?.msg || '删除失败') }
}
function onClose() { emit('update:open', false) }
function shortTime(t) { if (!t) return '—'; return String(t).slice(0, 16) }

/* —— RID 复制 —— */
const copied = ref(false)
async function copyRid() {
  try {
    await navigator.clipboard.writeText(form.rid || '')
    copied.value = true; setTimeout(() => copied.value = false, 2000)
  } catch {}
}

/* —— 头部图标 —— */
function propTypeIcon(p) {
  if (!p) return 'database'
  if (p.prop_type === 'object') return 'link'
  if (p.prop_type === 'annotation') return 'chat'
  return 'database'
}
function propTypeColor(p) {
  if (!p) return '#1677ff'
  if (p.prop_type === 'object') return '#FF7D00'
  if (p.prop_type === 'annotation') return '#00B42A'
  return '#1677ff'
}
</script>

<style scoped>
/* ============ 抽屉 (复用 Interfaces .if-detail 视觉, 无蒙层不锁定背景) ============ */
.sp-detail {
  position: fixed; top: 0; right: 0; bottom: 0;
  background: var(--bl-bg-1);
  border-left: 1px solid var(--bl-border);
  box-shadow: -4px 0 16px rgba(0, 0, 0, 0.10);
  overflow: hidden;
  display: flex; flex-direction: column;
  min-width: 420px; max-width: 90vw;
  z-index: 1000;
}
.sp-drag-handle {
  position: absolute; left: -2px; top: 0; bottom: 0; width: 5px;
  cursor: col-resize; background: transparent;
  transition: background-color .15s; z-index: 6;
}
.sp-drag-handle:hover, .sp-drag-handle.is-resizing { background: var(--bl-primary); }
.sp-drawer-enter-active, .sp-drawer-leave-active { transition: transform .25s ease, opacity .2s ease; }
.sp-drawer-enter-from, .sp-drawer-leave-to { transform: translateX(20px); opacity: 0; }

/* FieldRow inline 标签宽度 (与 Interfaces 一致) */
.sp-detail :deep(.fr.fr-inline .fr-label) { width: 86px; }

/* ============ 头部 ============ */
.detail-hd {
  height: 56px; padding: 0 14px;
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid var(--bl-divider);
  gap: 8px; flex-shrink: 0;
}
.dh-title { font-size: var(--bl-fs-14); font-weight: 600; }
.sp-ic {
  width: 22px; height: 22px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.sp-ic-lg { width: 36px; height: 36px; border-radius: 8px; }

/* 头部右侧分隔竖线 (编辑模式按钮 与 最大化/关闭 之间) */
.spd-hd-divider {
  display: inline-block;
  width: 1px; height: 18px;
  background: var(--bl-divider);
  margin: 0 6px;
  flex-shrink: 0;
}

/* 编辑模式按钮 (图标 + 文字, 默认灰色, 启用时蓝色) */
.spd-edit-btn {
  display: inline-flex; align-items: center;
  padding: 0 8px; height: 26px;
  font-size: 12px; color: var(--bl-text-2);
  border-radius: 4px;
  transition: background-color .15s, color .15s;
}
.spd-edit-btn:hover { color: var(--bl-text-1); background: var(--bl-bg-2); }
.spd-edit-btn.is-edit-on {
  color: var(--bl-primary);
  background: var(--bl-primary-soft);
  font-weight: 500;
}

/* ============ 页签 ============ */
.tabs { display: flex; padding: 0 16px; border-bottom: 1px solid var(--bl-divider); flex-shrink: 0; }
.tab {
  padding: 10px 14px; font-size: var(--bl-fs-13);
  color: var(--bl-text-2); cursor: pointer;
  border-bottom: 2px solid transparent; margin-bottom: -1px;
  display: inline-flex; align-items: center; gap: 6px;
}
.tab:hover { color: var(--bl-text-1); }
.tab.is-active { color: var(--bl-primary); border-color: var(--bl-primary); font-weight: 500; }
.tab-cnt {
  padding: 0 6px; min-width: 16px; height: 16px;
  background: var(--bl-primary); color: #fff;
  border-radius: 8px; font-size: 11px; line-height: 16px;
  text-align: center; font-feature-settings: "tnum";
}

/* ============ 主体 ============ */
.detail-body { flex: 1; overflow: auto; padding: 8px 14px; }
.detail-body.is-readonly :deep(input):disabled,
.detail-body.is-readonly :deep(select):disabled,
.detail-body.is-readonly :deep(textarea):disabled {
  background: var(--bl-bg-2); color: var(--bl-text-2);
}

/* 章节标题 (蓝色左侧条) */
.sec {
  font-size: var(--bl-fs-12); color: var(--bl-text-3);
  margin: 16px 0 8px; padding-left: 8px;
  border-left: 3px solid var(--bl-primary);
}
.row-between { display: flex; align-items: center; justify-content: space-between; padding: 6px 0; }

/* KV 行 (基础信息底部 概要) */
.kv-row {
  display: flex; padding: 4px 0;
  font-size: 13px;
  border-bottom: 1px dashed var(--bl-divider);
}
.kv-row:last-child { border-bottom: 0; }
.kv-lbl { width: 86px; color: var(--bl-text-3); font-size: 12px; flex-shrink: 0; }

/* RID 行 */
.rid-row {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px;
  background: var(--bl-bg-2);
  border-radius: 4px;
  font-size: 12px;
}
.rid-row .bl-mono { flex: 1; color: var(--bl-text-2); }

/* 单选组 */
.radio-group { display: inline-flex; align-items: center; gap: 20px; }
.radio-item { display: inline-flex; align-items: center; gap: 6px; cursor: pointer; }
.radio-item input { accent-color: var(--bl-primary); }

/* 值类型 / 格式化 pill */
.spd-vt-pill {
  display: inline-flex; align-items: center;
  padding: 2px 8px; border-radius: 9px;
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-2); border: 1px dashed var(--bl-border);
}
.spd-vt-pill.is-on {
  color: var(--bl-primary); background: var(--bl-primary-soft);
  border-style: solid; border-color: var(--bl-primary); font-weight: 500;
}

/* 文本域 (与 Interfaces 一致使用 bl-textarea, 自动可拖高) */
.bl-textarea {
  width: 100%;
  min-height: 80px;
  padding: 8px 10px;
  border: 1px solid var(--bl-border);
  border-radius: 4px;
  background: #fff;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.55;
  resize: vertical;
  color: var(--bl-text-1);
  transition: border-color .15s;
}
.bl-textarea:focus { outline: none; border-color: var(--bl-primary); }
.bl-textarea.bl-mono { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 12px; }
.bl-textarea:disabled { background: var(--bl-bg-2); color: var(--bl-text-2); cursor: not-allowed; resize: none; }

/* 引用表 */
.sp-ref-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.sp-ref-table th, .sp-ref-table td { padding: 8px; border-bottom: 1px solid var(--bl-divider); text-align: left; }
.sp-ref-table th { background: var(--bl-bg-2); font-weight: 500; color: var(--bl-text-3); }

/* ============ 底部 ============ */
.detail-ft {
  padding: 10px 14px;
  display: flex; align-items: center; gap: 8px;
  border-top: 1px solid var(--bl-divider);
  flex-shrink: 0;
}
</style>
