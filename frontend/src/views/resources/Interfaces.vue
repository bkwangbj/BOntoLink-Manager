<template>
  <div class="page">
    <PageHeader title="接口" subtitle="纯抽象契约，定义共享属性，应用对象类型">
      <template #actions>
        <div class="seg">
          <button v-for="t in statusTabs" :key="t.k" :class="['seg-btn', statusFilter===t.k && 'is-on']" @click="statusFilter=t.k">
            {{ t.label }}<span class="seg-cnt">{{ statusCount(t.k) }}</span>
          </button>
        </div>
        <select class="bl-input" style="width:180px" v-model="domainFilter">
          <option value="">全部业务领域</option>
          <option v-for="d in domainOpts" :key="d.code" :value="d.code">{{ d.name }}</option>
        </select>
        <div class="search-wrap">
          <span class="search-icon" v-html="BL.icon('search', 14)"></span>
          <input class="bl-input search-input" v-model="q" placeholder="搜索接口（名称/编码/类型）" />
        </div>
        <button class="bl-btn bl-btn-primary" @click="openCreate">
          <span v-html="BL.icon('plus', 12, '#fff')"></span>
          <span style="margin-left:4px">新建接口</span>
        </button>
      </template>
    </PageHeader>

    <div :class="['two-pane', !drawerOpen && 'no-detail']">
      <!-- 列表 -->
      <div class="bl-card list-card">
        <table class="bl-table if-table">
          <thead>
            <tr>
              <th style="width:30px"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
              <th></th>
              <th>接口名称</th>
              <th>描述</th>
              <th>属性数</th>
              <th>实现数</th>
              <th>所在领域</th>
              <th>状态</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in pageRows" :key="r.id" :class="['if-row', selected?.id===r.id && 'is-active']" @click="openDetail(r, 'overview')">
              <td @click.stop><input type="checkbox" :checked="checked.has(r.id)" @change="toggleCheck(r.id)" /></td>
              <td>
                <span class="if-ic" :style="{ background: r.color || '#13C2C2' }" v-html="BL.icon(r.icon || 'station', 12, '#fff')"></span>
              </td>
              <td>
                <div class="if-name">{{ r.display_name || r.rdfs_label }}</div>
                <div class="if-api bl-mono bl-muted">{{ r.api_name }}</div>
              </td>
              <td class="bl-truncate" style="max-width:280px" :title="r.rdfs_comment">{{ r.rdfs_comment || '—' }}</td>
              <td @click.stop="openDetail(r, 'props')"><a class="link">{{ r.prop_count || 0 }}</a></td>
              <td @click.stop="openDetail(r, 'impl')"><a class="link">{{ r.impl_count || 0 }}</a></td>
              <td><span class="bl-tag">{{ r.ns_code || '—' }}</span></td>
              <td>
                <span :class="['bl-tag', r.status===1 ? 'bl-tag-success' : 'bl-tag-warning']">{{ r.status===1 ? '启用' : '实验' }}</span>
              </td>
              <td @click.stop>
                <div class="bl-row" style="gap:0">
                  <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="编辑" @click="openDetail(r, 'overview')" v-html="BL.icon('edit', 12)"></button>
                  <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" :title="r.status===1 ? '禁用' : '启用'" @click="toggleStatus(r)" v-html="BL.icon('zap', 12)"></button>
                  <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click="removeOne(r)" v-html="BL.icon('trash', 12)"></button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!filtered.length" class="bl-empty" style="padding:60px">
          <div class="bl-empty-icon" v-html="BL.icon('station', 36)"></div>
          <div>未匹配到接口数据</div>
          <div style="margin-top:12px">
            <button class="bl-btn" @click="clearFilters">清除筛选</button>
          </div>
        </div>
        <!-- 分页 + 批量条 -->
        <div class="list-ft">
          <div>
            <template v-if="checked.size">
              已选 <b style="color:var(--bl-primary)">{{ checked.size }}</b> 项
              <button class="bl-btn bl-btn-sm bl-btn-danger" style="margin-left:8px" @click="removeBatch">批量删除</button>
            </template>
            <template v-else>
              共 {{ filtered.length }} 项
            </template>
          </div>
          <div class="bl-row" style="gap:4px">
            <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page<=1" @click="page--">‹</button>
            <span class="bl-muted" style="font-size:12px">{{ page }} / {{ totalPages }}</span>
            <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page>=totalPages" @click="page++">›</button>
          </div>
        </div>
      </div>

      <!-- 右侧详情面板（固定占位） -->
      <aside v-if="drawerOpen" class="if-detail">
        <div class="detail-hd">
          <div class="bl-row" style="gap:10px;flex:1;min-width:0">
            <span class="if-ic if-ic-lg" :style="{ background: form.color || '#13C2C2' }" v-html="BL.icon(form.icon || 'station', 18, '#fff')"></span>
            <div class="bl-grow" style="min-width:0">
              <div class="dh-title bl-truncate">{{ form.display_name || form.rdfs_label || form.api_name || '新建接口' }}</div>
              <div class="bl-mono bl-muted" style="font-size:11px">{{ form.api_name || '—' }}</div>
            </div>
          </div>
          <button class="bl-btn bl-btn-text bl-btn-icon" @click="drawerOpen=false" v-html="BL.icon('x', 14)"></button>
        </div>

        <div class="tabs">
          <div v-for="t in tabs" :key="t.k" :class="['tab', drawerTab===t.k && 'is-active']" @click="drawerTab=t.k">{{ t.label }}</div>
        </div>

        <div class="detail-body">
          <!-- 概览 -->
          <div v-if="drawerTab==='overview'">
            <div class="sec">基础信息</div>
            <FieldRow label="接口编码" inline><input class="bl-input bl-mono" v-model="form.interface_code" /></FieldRow>
            <FieldRow label="API 名称" inline hint="snake_case · 全局唯一 · 创建后不可修改">
              <input class="bl-input bl-mono" :value="form.api_name" :readonly="!!form.id" :disabled="!!form.id" />
            </FieldRow>
            <FieldRow label="标准对外名称" hint="rdfs:label"><input class="bl-input" v-model="form.rdfs_label" /></FieldRow>
            <FieldRow label="显示名称"><input class="bl-input" v-model="form.display_name" /></FieldRow>
            <FieldRow label="状态" inline>
              <div class="radio-group">
                <label class="radio-item"><input type="radio" :value="1" v-model.number="form.status" /> 启用</label>
                <label class="radio-item"><input type="radio" :value="0" v-model.number="form.status" /> 实验</label>
              </div>
            </FieldRow>

            <div class="sec">分类归属</div>
            <FieldRow label="业务领域" inline>
              <select class="bl-input" v-model="form.category_code">
                <option value="">— 无 —</option>
                <option v-for="d in domainOpts" :key="d.code" :value="d.code">{{ d.name }}</option>
              </select>
            </FieldRow>
            <FieldRow label="命名空间" inline><input class="bl-input bl-mono" v-model="form.ns_code" /></FieldRow>

            <div class="sec">语义定义</div>
            <FieldRow label="注释" hint="rdfs:comment · 给大模型 / 推理引擎 / 开发者看的正式语义定义">
              <textarea class="bl-textarea" rows="3" v-model="form.rdfs_comment"></textarea>
            </FieldRow>
            <FieldRow label="参考资料" hint="rdfs:seeAlso · 关联文档 / 链接 / 参考出处"><input class="bl-input" v-model="form.rdfs_see_also" /></FieldRow>
            <FieldRow label="定义来源" hint="rdfs:isDefinedBy · 权威定义出处"><input class="bl-input" v-model="form.rdfs_defined_by" /></FieldRow>
            <FieldRow label="说明"><textarea class="bl-textarea" rows="2" v-model="form.description"></textarea></FieldRow>

            <div class="sec">规则配置</div>
            <FieldRow label="元数据 (JSON)" hint="机器可读：接口规则、查询约束、语义配置">
              <textarea class="bl-textarea bl-mono" rows="5" v-model="form.metadata" placeholder='{ "query": "...", "unit_normalization": true }'></textarea>
            </FieldRow>
          </div>

          <!-- 显示 -->
          <div v-if="drawerTab==='style'">
            <div class="sec">分组颜色</div>
            <div class="color-row">
              <span v-for="c in colorPalette" :key="c"
                    :class="['color-dot', form.color===c && 'is-active']"
                    :style="{ background: c }"
                    @click="form.color = c"></span>
            </div>
            <div class="hex-picker-group" style="margin-top:10px">
              <input class="bl-input bl-mono hex-input" v-model="form.color" placeholder="#RRGGBB" />
              <input type="color" class="color-picker color-picker-attached" v-model="form.color" title="自定义颜色" />
            </div>

            <div class="sec">图标</div>
            <div class="icon-row">
              <span v-for="ic in iconPalette" :key="ic"
                    :class="['icon-cell', form.icon===ic && 'is-active']"
                    @click="form.icon = ic"
                    v-html="BL.icon(ic, 18)"></span>
              <input class="bl-input bl-mono" style="width:140px; margin-left:8px" v-model="form.icon" placeholder="自定义图标 key" />
            </div>

            <div class="sec">RID</div>
            <div class="rid-row">
              <span class="bl-mono">{{ form.rid }}</span>
              <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" @click="copyText(form.rid)" title="复制 RID" v-html="BL.icon('copy', 12)"></button>
            </div>
          </div>

          <!-- 属性 -->
          <div v-if="drawerTab==='props'">
            <div class="row-between">
              <span class="bl-muted" style="font-size:12px">共 {{ properties.length }} 个属性</span>
              <button class="bl-btn bl-btn-primary bl-btn-sm" @click="addPropForm">
                <span v-html="BL.icon('plus', 12, '#fff')"></span>
                <span style="margin-left:4px">新增属性</span>
              </button>
            </div>
            <table class="bl-table" style="margin-top:8px">
              <thead><tr><th>编码</th><th>名称</th><th>类型</th><th>必填</th><th>注释</th><th></th></tr></thead>
              <tbody>
                <tr v-for="p in properties" :key="p.id">
                  <td class="bl-mono">{{ p.api_name }}</td>
                  <td>{{ p.display_name || p.rdfs_label }}</td>
                  <td><span class="bl-tag">{{ p.data_type }}</span></td>
                  <td><span :class="['bl-tag', p.is_required ? 'bl-tag-danger' : '']">{{ p.is_required ? '必填' : '可选' }}</span></td>
                  <td class="bl-truncate" style="max-width:200px" :title="p.rdfs_comment">{{ p.rdfs_comment || '—' }}</td>
                  <td><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="removeProp(p)" v-html="BL.icon('trash', 12)"></button></td>
                </tr>
              </tbody>
            </table>
            <div v-if="!properties.length" class="bl-empty" style="padding:32px">尚未定义接口属性</div>
          </div>

          <!-- 实现 -->
          <div v-if="drawerTab==='impl'">
            <div class="row-between">
              <span class="bl-muted" style="font-size:12px">共 {{ implementers.length }} 个实现类</span>
              <button class="bl-btn bl-btn-primary bl-btn-sm" @click="openAddImpl">
                <span v-html="BL.icon('plus', 12, '#fff')"></span>
                <span style="margin-left:4px">绑定实现类</span>
              </button>
            </div>
            <div class="impl-list">
              <div v-for="c in implementers" :key="c.id" class="impl-row">
                <span class="if-ic" :style="{ background: c.color || '#165DFF' }" v-html="BL.icon(c.icon || 'cube', 12, '#fff')"></span>
                <div class="bl-grow">
                  <div>{{ c.display_name || c.rdfs_label || c.api_name }}</div>
                  <div class="bl-mono bl-muted" style="font-size:11px">{{ c.api_name }} · {{ c.ns_code }}</div>
                </div>
                <button class="bl-btn bl-btn-text bl-btn-sm" @click="goObject(c)">查看对象</button>
                <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="解绑" @click="removeImpl(c)" v-html="BL.icon('x', 12)"></button>
              </div>
              <div v-if="!implementers.length" class="bl-empty" style="padding:32px">尚未有对象实现该接口</div>
            </div>
          </div>
        </div>

        <div class="detail-ft">
          <button class="bl-btn" @click="drawerOpen=false">取消</button>
          <button class="bl-btn bl-btn-primary" @click="saveInterface">保存</button>
        </div>
      </aside>
    </div>

    <!-- 新增属性弹窗 -->
    <div v-if="propFormOpen" class="bl-modal-mask" @click.self="propFormOpen=false">
      <div class="bl-modal" style="width:440px">
        <div class="bl-modal-hd">新增接口属性</div>
        <div class="bl-modal-body bl-col" style="gap:10px">
          <FieldRow label="API 名称 *" inline><input class="bl-input bl-mono" v-model="propForm.api_name" placeholder="snake_case" /></FieldRow>
          <FieldRow label="属性代码" inline><input class="bl-input bl-mono" v-model="propForm.prop_code" placeholder="camelCase" /></FieldRow>
          <FieldRow label="数据类型 *" inline>
            <select class="bl-input" v-model="propForm.data_type">
              <option v-for="t in xsdTypes" :key="t" :value="t">{{ t }}</option>
            </select>
          </FieldRow>
          <FieldRow label="显示名"><input class="bl-input" v-model="propForm.display_name" /></FieldRow>
          <FieldRow label="必填" inline>
            <div class="radio-group">
              <label class="radio-item"><input type="radio" :value="1" v-model.number="propForm.is_required" /> 必填</label>
              <label class="radio-item"><input type="radio" :value="0" v-model.number="propForm.is_required" /> 可选</label>
            </div>
          </FieldRow>
          <FieldRow label="注释"><textarea class="bl-textarea" rows="2" v-model="propForm.rdfs_comment"></textarea></FieldRow>
        </div>
        <div class="bl-modal-ft">
          <button class="bl-btn" @click="propFormOpen=false">取消</button>
          <button class="bl-btn bl-btn-primary" @click="submitProp">添加</button>
        </div>
      </div>
    </div>

    <!-- 绑定实现类弹窗 -->
    <div v-if="implPickerOpen" class="bl-modal-mask" @click.self="implPickerOpen=false">
      <div class="bl-modal" style="width:480px;max-height:80vh;display:flex;flex-direction:column">
        <div class="bl-modal-hd">绑定实现该接口的对象类型</div>
        <div class="bl-modal-body bl-col" style="gap:8px;overflow:hidden">
          <input class="bl-input" placeholder="搜索对象类型" v-model="implPickerQ" />
          <div style="flex:1; overflow:auto; display:flex; flex-direction:column; gap:4px">
            <label v-for="c in implCandidates" :key="c.id"
                   :class="['mp-row', implDraft.has(c.id) && 'is-on']">
              <input type="checkbox" :checked="implDraft.has(c.id)" @change="toggleImplDraft(c.id)" />
              <span class="if-ic" :style="{ background: c.color || '#165DFF' }" v-html="BL.icon(c.icon || 'cube', 12, '#fff')"></span>
              <div class="bl-grow"><div>{{ c.display_name || c.api_name }}</div>
                <div class="bl-mono bl-muted" style="font-size:11px">{{ c.api_name }} · {{ c.ns_code }}</div>
              </div>
            </label>
            <div v-if="!implCandidates.length" class="bl-empty" style="padding:24px">暂无可绑定的对象类型</div>
          </div>
        </div>
        <div class="bl-modal-ft">
          <button class="bl-btn" @click="implPickerOpen=false">取消</button>
          <button class="bl-btn bl-btn-primary" :disabled="!implDraft.size" @click="submitImpls">绑定 ({{ implDraft.size }})</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FieldRow from '@/views/config/category/FieldRow.vue'
import { BL } from '@/lib/bl.js'
import { interfaceApi, resourceApi, categoryApi } from '@/api'

const rows = ref([])
const statusFilter = ref('all')
const domainFilter = ref('')
const q = ref('')
const checked = ref(new Set())
const page = ref(1)
const PAGE_SIZE = 10

const drawerOpen = ref(false)
const drawerTab = ref('overview')
const selected = ref(null)
const form = reactive({})
const properties = ref([])
const implementers = ref([])
const allClasses = ref([])
const domainOpts = ref([])

const statusTabs = [{ k:'all', label:'全部' }, { k:'1', label:'启用' }, { k:'0', label:'实验' }]
const tabs = [
  { k:'overview', label:'概览' },
  { k:'style',    label:'显示' },
  { k:'props',    label:'属性' },
  { k:'impl',     label:'实现' }
]
const colorPalette = ['#165DFF','#00B42A','#722ED1','#FF7D00','#EB2F96','#13C2C2','#FADB14','#F53F3F','#86909C']
const iconPalette  = ['grid','cube','folder','share','network','station','user','sliders']
const xsdTypes = ['xsd:string','xsd:decimal','xsd:integer','xsd:boolean','xsd:dateTime','xsd:date','xsd:anyURI']

function statusCount(k) {
  if (k === 'all') return rows.value.length
  return rows.value.filter(r => String(r.status) === k).length
}

const filtered = computed(() => {
  let list = rows.value
  if (statusFilter.value !== 'all') list = list.filter(r => String(r.status) === statusFilter.value)
  if (domainFilter.value)         list = list.filter(r => r.category_code === domainFilter.value)
  const k = q.value.trim().toLowerCase()
  if (k) list = list.filter(r =>
    [r.api_name, r.interface_code, r.display_name, r.rdfs_label, r.rdfs_comment, r.ns_code]
      .filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
  return list
})
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / PAGE_SIZE)))
const pageRows   = computed(() => filtered.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE))
const allChecked = computed(() => pageRows.value.length && pageRows.value.every(r => checked.value.has(r.id)))

// 重置页码当筛选变化
import { watch } from 'vue'
watch([statusFilter, domainFilter, q], () => { page.value = 1 })

function toggleAll() {
  const s = new Set(checked.value)
  if (allChecked.value) pageRows.value.forEach(r => s.delete(r.id))
  else pageRows.value.forEach(r => s.add(r.id))
  checked.value = s
}
function toggleCheck(id) {
  const s = new Set(checked.value)
  s.has(id) ? s.delete(id) : s.add(id)
  checked.value = s
}
function clearFilters() { statusFilter.value='all'; domainFilter.value=''; q.value='' }

async function load() {
  rows.value = await interfaceApi.list().catch(() => [])
}

function openCreate() {
  selected.value = null
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, { status: 1, color: '#165DFF', icon: 'station' })
  drawerTab.value = 'overview'
  drawerOpen.value = true
}

async function openDetail(r, tab) {
  selected.value = r
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, r)
  drawerTab.value = tab || 'overview'
  drawerOpen.value = true
  await loadProps()
  await loadImpls()
}

async function loadProps() {
  if (!selected.value) { properties.value = []; return }
  properties.value = await interfaceApi.properties(selected.value.id).catch(() => [])
}
async function loadImpls() {
  if (!selected.value) { implementers.value = []; return }
  implementers.value = await interfaceApi.implementers(selected.value.id).catch(() => [])
}

async function saveInterface() {
  if (!form.api_name) { BL.warning('API 名称为必填'); return }
  if (!/^[a-z][a-z0-9_]*$/.test(form.api_name)) { BL.warning('API 名称需 snake_case'); return }
  if (form.id) await interfaceApi.update(form.id, form)
  else         await interfaceApi.create(form)
  BL.success('已保存')
  drawerOpen.value = false
  await load()
}

async function toggleStatus(r) {
  await interfaceApi.toggle(r.id)
  await load()
}

async function removeOne(r) {
  const ok = await BL.confirm({ title:'删除接口', content:`确定删除「${r.display_name || r.api_name}」？关联属性与实现关系会一并删除。`, danger:true, okText:'删除' })
  if (!ok) return
  await interfaceApi.remove(r.id)
  BL.success('已删除')
  if (selected.value?.id === r.id) drawerOpen.value = false
  await load()
}

async function removeBatch() {
  const ok = await BL.confirm({ title:'批量删除', content:`确定删除已选 ${checked.value.size} 个接口？`, danger:true, okText:'删除' })
  if (!ok) return
  for (const id of checked.value) await interfaceApi.remove(id)
  BL.success('已删除')
  checked.value = new Set()
  await load()
}

/* ---- 属性 ---- */
const propFormOpen = ref(false)
const propForm = reactive({})

function addPropForm() {
  Object.keys(propForm).forEach(k => delete propForm[k])
  Object.assign(propForm, { data_type:'xsd:string', is_required: 0 })
  propFormOpen.value = true
}
async function submitProp() {
  if (!propForm.api_name || !propForm.data_type) { BL.warning('API 名称、数据类型必填'); return }
  await interfaceApi.addProperty(selected.value.id, propForm)
  BL.success('已添加')
  propFormOpen.value = false
  await loadProps()
  await load()
}
async function removeProp(p) {
  const ok = await BL.confirm({ title:'删除属性', content:`确定删除属性「${p.api_name}」？`, danger:true })
  if (!ok) return
  await interfaceApi.removeProperty(p.id)
  await loadProps()
  await load()
}

/* ---- 实现 ---- */
const implPickerOpen = ref(false)
const implPickerQ = ref('')
const implDraft = ref(new Set())

async function openAddImpl() {
  if (!allClasses.value.length) allClasses.value = await resourceApi.classes().catch(() => [])
  implDraft.value = new Set()
  implPickerQ.value = ''
  implPickerOpen.value = true
}
const implCandidates = computed(() => {
  const bound = new Set(implementers.value.map(c => c.id))
  const k = implPickerQ.value.trim().toLowerCase()
  return allClasses.value
    .filter(c => !bound.has(c.id))
    .filter(c => !k || [c.display_name, c.api_name, c.ns_code].filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
})
function toggleImplDraft(id) {
  const s = new Set(implDraft.value)
  s.has(id) ? s.delete(id) : s.add(id)
  implDraft.value = s
}
async function submitImpls() {
  for (const cid of implDraft.value) {
    const c = allClasses.value.find(x => x.id === cid)
    await interfaceApi.addImpl(selected.value.id, { classId: cid, categoryCode: c?.category_code })
  }
  BL.success(`已绑定 ${implDraft.value.size} 个对象类型`)
  implPickerOpen.value = false
  await loadImpls()
  await load()
}
async function removeImpl(c) {
  const ok = await BL.confirm({ title:'解绑实现关系', content:`从该接口中解除「${c.display_name || c.api_name}」？`, danger:true })
  if (!ok) return
  await interfaceApi.removeImpl(selected.value.id, c.id)
  await loadImpls()
  await load()
}
function goObject(c) { BL.info(`跳转到对象类型: ${c.display_name || c.api_name}（演示）`) }

async function copyText(t) {
  try { await navigator.clipboard.writeText(String(t || '')); BL.success('已复制') } catch {}
}

onMounted(async () => {
  await load()
  // 业务领域候选
  const tree = await categoryApi.tree().catch(() => [])
  const list = []
  const walk = (ns) => ns.forEach(n => { if (n.categoryCode) list.push({ code:n.categoryCode, name:n.label }); if (n.children) walk(n.children) })
  walk(tree)
  domainOpts.value = list
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.two-pane {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 480px;
  gap: 12px; padding: 12px;
  overflow: hidden;
}
.two-pane.no-detail { grid-template-columns: 1fr; }
.list-card { flex: 1; overflow: hidden; display: flex; flex-direction: column; min-width: 0; }
.list-ft {
  padding: 8px 12px; border-top: 1px solid var(--bl-divider);
  display: flex; justify-content: space-between; align-items: center;
  font-size: var(--bl-fs-12);
}

.seg {
  display: inline-flex; background: var(--bl-bg-2);
  border-radius: var(--bl-radius-2);
  padding: 2px; gap: 2px;
}
.seg-btn {
  height: 26px; padding: 0 12px;
  border: 0; background: transparent;
  font-size: var(--bl-fs-12); color: var(--bl-text-2); cursor: pointer;
  border-radius: 4px;
  display: inline-flex; align-items: center; gap: 6px;
}
.seg-btn.is-on { background: var(--bl-bg-1); color: var(--bl-primary); box-shadow: var(--bl-shadow-1); }
.seg-cnt { font-size: var(--bl-fs-11); color: var(--bl-text-3); }
.seg-btn.is-on .seg-cnt { color: var(--bl-primary); }

.search-wrap { position: relative; width: 240px; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.search-input { padding-left: 30px; }

.if-table thead th { position: sticky; top: 0; background: var(--bl-bg-2); z-index: 1; }
.if-row { cursor: pointer; }
.if-row.is-active { background: var(--bl-primary-soft); }
.if-row.is-active td { color: var(--bl-primary); }
.if-ic { width: 22px; height: 22px; border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; }
.if-ic-lg { width: 36px; height: 36px; border-radius: 8px; }
.if-name { font-weight: 500; }
.if-api  { font-size: var(--bl-fs-11); }
.link { color: var(--bl-primary); cursor: pointer; font-weight: 500; }
.link:hover { text-decoration: underline; }

/* 右侧详情面板（固定占位，非浮层） */
.if-detail {
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  overflow: hidden;
  display: flex; flex-direction: column;
  min-width: 0;
}
.detail-hd {
  height: 56px; padding: 0 14px;
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid var(--bl-divider);
  gap: 8px;
}
.detail-body { flex: 1; overflow: auto; padding: 8px 14px; }
.detail-ft {
  padding: 10px 14px;
  display: flex; justify-content: flex-end; gap: 8px;
  border-top: 1px solid var(--bl-divider);
}
.dh-title { font-size: var(--bl-fs-14); font-weight: 600; }

.tabs { display: flex; padding: 0 16px; border-bottom: 1px solid var(--bl-divider); }
.tab {
  padding: 10px 14px; font-size: var(--bl-fs-13);
  color: var(--bl-text-2); cursor: pointer;
  border-bottom: 2px solid transparent; margin-bottom: -1px;
}
.tab.is-active { color: var(--bl-primary); border-color: var(--bl-primary); font-weight: 500; }

.sec {
  font-size: var(--bl-fs-12); color: var(--bl-text-3);
  margin: 16px 0 8px; padding-left: 8px;
  border-left: 3px solid var(--bl-primary);
}
.row-between { display: flex; align-items: center; justify-content: space-between; padding: 6px 0; }

.color-row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.color-dot { width: 26px; height: 26px; border-radius: 50%; cursor: pointer; border: 2px solid transparent; }
.color-dot.is-active { border-color: var(--bl-bg-1); box-shadow: 0 0 0 2px var(--bl-primary); }
.icon-row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.icon-cell {
  width: 32px; height: 32px; border-radius: var(--bl-radius-2);
  border: 1px solid var(--bl-border);
  display: inline-flex; align-items: center; justify-content: center;
  color: var(--bl-text-2); cursor: pointer;
}
.icon-cell.is-active { color: var(--bl-primary); border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.rid-row {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; background: var(--bl-bg-2); border-radius: var(--bl-radius-2);
  font-size: var(--bl-fs-12);
}

.impl-list { display: flex; flex-direction: column; gap: 6px; margin-top: 8px; }
.impl-row {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border: 1px solid var(--bl-border); border-radius: var(--bl-radius-2);
  font-size: var(--bl-fs-13);
}

.radio-group { display: inline-flex; align-items: center; gap: 20px; }
.radio-item { display: inline-flex; align-items: center; gap: 6px; cursor: pointer; }
.radio-item input { accent-color: var(--bl-primary); }

.mp-row {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 10px; border: 1px solid var(--bl-border); border-radius: var(--bl-radius-2);
  cursor: pointer;
}
.mp-row:hover { border-color: var(--bl-primary-border); }
.mp-row.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.mp-row input { accent-color: var(--bl-primary); }
</style>
