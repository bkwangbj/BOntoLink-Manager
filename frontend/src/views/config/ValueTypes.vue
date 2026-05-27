<template>
  <div class="page">
    <PageHeader title="值类型 (Value types)" subtitle="属性语义包装器 · 封装元数据约束 · 增安全表达">
      <template #actions>
        <!-- 状态分类标签 -->
        <div class="seg">
          <button v-for="t in statusTabs" :key="t.k"
                  :class="['seg-btn', filterStatus===t.k && 'is-on']"
                  @click="filterStatus = t.k">
            {{ t.label }}<span class="seg-cnt">{{ statusCount(t.k) }}</span>
          </button>
        </div>

        <!-- 业务领域 -->
        <select class="bl-input hd-filter" v-model="filterCategory" title="业务领域">
          <option value="">全部业务领域</option>
          <option v-for="d in domainOpts" :key="d.code" :value="d.code">{{ d.name }}</option>
        </select>

        <!-- 搜索 -->
        <div class="search-wrap">
          <span class="search-icon" v-html="BL.icon('search', 14)"></span>
          <input class="bl-input search-input" placeholder="搜索值（名称/编码/类型）" v-model="q" />
          <button v-if="q" class="search-clear" @click="q=''" v-html="BL.icon('x', 10)"></button>
        </div>

        <!-- 新建 -->
        <button class="bl-btn bl-btn-primary" @click="openCreate">
          <span v-html="BL.icon('plus', 12, '#fff')"></span>
          <span style="margin-left:4px">新建值类型</span>
        </button>
      </template>
    </PageHeader>

    <div class="vt-body">
      <div class="bl-card list-card">
        <div class="list-scroll">
          <table class="bl-table vt-table">
            <thead>
              <tr>
                <th style="width:34px"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
                <th><span class="th-sort" @click="toggleSort('name')">名称<span class="th-arrow">{{ sortArrow('name') }}</span></span></th>
                <th><span class="th-sort" @click="toggleSort('api')">API<span class="th-arrow">{{ sortArrow('api') }}</span></span></th>
                <th><span class="th-sort" @click="toggleSort('domain')">所在领域<span class="th-arrow">{{ sortArrow('domain') }}</span></span></th>
                <th><span class="th-sort" @click="toggleSort('base')">数据类型<span class="th-arrow">{{ sortArrow('base') }}</span></span></th>
                <th><span class="th-sort" @click="toggleSort('constraint')">约束<span class="th-arrow">{{ sortArrow('constraint') }}</span></span></th>
                <th><span class="th-sort" @click="toggleSort('enum')">枚举<span class="th-arrow">{{ sortArrow('enum') }}</span></span></th>
                <th>说明</th>
                <th>RID</th>
                <th><span class="th-sort" @click="toggleSort('status')">状态<span class="th-arrow">{{ sortArrow('status') }}</span></span></th>
                <th style="width:64px" class="t-actions"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in paged" :key="r.id" :class="['vt-row', selected?.id===r.id && 'is-active']" @click="openEdit(r)">
                <td @click.stop><input type="checkbox" :checked="checked.has(r.id)" @change="toggleCheck(r.id)" /></td>
                <td><div class="vt-name">{{ r.rdfs_label || r.api_name }}</div></td>
                <td><span class="bl-mono">{{ r.api_name }}</span></td>
                <td><span class="bl-muted">{{ r.category_code || '—' }}</span></td>
                <td><span class="bl-tag">{{ r.base_type }}</span></td>
                <td><span :class="['bl-tag', constraintTagCls(r.constraint_type)]">{{ r.constraint_type }}</span></td>
                <td>
                  <span v-if="r.constraint_type === 'Enum'" class="bl-tag">{{ r.enum_label || r.enum_api_name || '—' }}</span>
                  <span v-else class="bl-muted">—</span>
                </td>
                <td class="bl-truncate" style="max-width:220px" :title="r.rdfs_comment">{{ r.rdfs_comment || '—' }}</td>
                <td><span class="bl-mono bl-muted bl-truncate" :title="r.rid" style="max-width:140px;display:inline-block">{{ r.rid }}</span></td>
                <td>
                  <span :class="['bl-tag', r.status === 1 ? 'bl-tag-success' : 'bl-tag-danger']">{{ r.status === 1 ? '启用' : '禁用' }}</span>
                </td>
                <td @click.stop class="t-actions">
                  <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" :title="r.status===1?'禁用':'启用'" @click="toggleOne(r)" v-html="BL.icon('zap', 12)"></button>
                  <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click="doDelete(r)" v-html="BL.icon('trash', 12)"></button>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="!filtered.length" class="bl-empty" style="padding:60px">
            <div class="bl-empty-icon" v-html="BL.icon('layers', 36)"></div>
            <div>未匹配到值类型</div>
            <div style="margin-top:12px">
              <button class="bl-btn" @click="clearFilters">清除筛选</button>
            </div>
          </div>
        </div>

        <!-- 分页 / 批量操作 -->
        <div class="vt-pager" v-if="filtered.length > 0">
          <div class="vt-pager-l">
            <template v-if="checked.size">
              已选 <b style="color:var(--bl-primary)">{{ checked.size }}</b> 项
              <button class="bl-btn bl-btn-sm bl-btn-danger" style="margin-left:8px" @click="batchRemove">
                <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">删除</span>
              </button>
              <button class="bl-btn bl-btn-sm" style="margin-left:6px" @click="batchSetStatus(1)">
                <span v-html="BL.icon('check', 12)"></span><span style="margin-left:4px">启用</span>
              </button>
              <button class="bl-btn bl-btn-sm" style="margin-left:6px" @click="batchSetStatus(0)">
                <span v-html="BL.icon('power', 12)"></span><span style="margin-left:4px">禁用</span>
              </button>
              <button class="bl-btn bl-btn-sm bl-btn-text" style="margin-left:6px" @click="checked = new Set()">取消选择</button>
            </template>
            <template v-else>
              共 {{ filtered.length }} 项
            </template>
          </div>
          <div class="bl-row" style="gap:4px">
            <span class="bl-muted" style="font-size:12px;margin-right:6px">每页</span>
            <select class="bl-input vt-page-size" v-model.number="pageSize">
              <option :value="20">20</option><option :value="50">50</option><option :value="100">100</option>
            </select>
            <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page<=1" @click="page--">‹</button>
            <span class="bl-muted" style="font-size:12px">{{ page }} / {{ totalPages }}</span>
            <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page>=totalPages" @click="page++">›</button>
          </div>
        </div>
      </div>

      <!-- 右侧编辑抽屉:左侧 Tab + 右侧滚动内容 + 底部按钮栏 -->
      <transition name="vt-drawer">
        <aside v-if="drawerOpen" class="vt-drawer" :style="{ width: drawerWidth + 'px' }">
          <div class="vt-drag-handle" @mousedown="onDragStart" :class="resizing && 'is-resizing'"></div>
          <div class="vt-hd">
            <div class="bl-row" style="gap:8px;flex:1;min-width:0">
              <span class="vt-ic" v-html="BL.icon('layers', 18, '#fff')"></span>
              <div class="bl-grow" style="min-width:0">
                <div class="vt-title bl-truncate">{{ form.rdfs_label || form.api_name || '新建值类型' }}</div>
                <div class="bl-mono bl-muted" style="font-size:11px">{{ form.api_name || '—' }}</div>
              </div>
            </div>
            <button class="bl-btn bl-btn-text bl-btn-icon" @click="drawerOpen=false" v-html="BL.icon('x', 14)"></button>
          </div>

          <!-- 左 Tab + 右 内容 -->
          <div class="vt-body-2col">
            <nav class="vt-tabs">
              <button v-for="t in tabs" :key="t.k" :class="['vt-tab', activeTab===t.k && 'is-on']" @click="onPickTab(t.k)">
                <span v-html="BL.icon(t.icon, 12)"></span>
                <span style="margin-left:6px">{{ t.label }}</span>
              </button>
            </nav>

            <div class="vt-pane" ref="paneRef" @scroll="onPaneScroll">
              <!-- 元数据 -->
              <section ref="metaSec" data-sec="meta">
                <div class="sec">元数据</div>
                <FieldRow label="名称 *" inline><input class="bl-input" v-model="form.rdfs_label" @input="onLabelInput" /></FieldRow>
                <FieldRow label="描述"><textarea class="bl-textarea" rows="2" v-model="form.rdfs_comment"></textarea></FieldRow>
                <FieldRow label="API *" inline hint="snake_case · 全局唯一 · 创建后不可修改">
                  <div class="bl-row" style="gap:6px;flex:1">
                    <input class="bl-input bl-mono" v-model="form.api_name" :disabled="!!form.id" />
                    <button v-if="!form.id" class="bl-btn bl-btn-sm bl-btn-text" title="根据名称重新生成" @click="form.api_name = deriveApi(form.rdfs_label)">
                      <span v-html="BL.icon('refresh', 11)"></span>
                    </button>
                  </div>
                </FieldRow>
                <FieldRow label="基础类型 *" inline>
                  <div class="bl-row" style="gap:8px;flex:1;align-items:center;min-width:0">
                    <select class="bl-input" style="flex:1;min-width:0" v-model="form.base_type" :disabled="!!form.id">
                      <option v-for="t in baseTypes" :key="t" :value="t">{{ t }}</option>
                    </select>
                    <span style="color:var(--bl-danger);font-size:11px;white-space:nowrap;flex-shrink:0">(保存后不可修改)</span>
                  </div>
                </FieldRow>
                <FieldRow label="所在领域" inline>
                  <select class="bl-input" v-model="form.category_code">
                    <option value="">— 无 —</option>
                    <option v-for="d in domainOpts" :key="d.code" :value="d.code">{{ d.name }}</option>
                  </select>
                </FieldRow>
                <FieldRow label="状态" inline>
                  <div class="radio-group">
                    <label class="radio-item"><input type="radio" :value="1" v-model.number="form.status" /> 启用</label>
                    <label class="radio-item"><input type="radio" :value="0" v-model.number="form.status" /> 禁用</label>
                  </div>
                </FieldRow>
              </section>

              <!-- 约束配置 -->
              <section ref="conSec" data-sec="constraint">
                <div class="sec">约束配置</div>
                <div class="constraint-grid">
                  <div v-for="c in constraintTypes" :key="c.k"
                       :class="['constraint-card', form.constraint_type===c.k && 'is-on']"
                       @click="onPickConstraint(c.k)">
                    <div class="cc-row">
                      <div class="cc-title">{{ c.k }}</div>
                      <span class="cc-help" :title="c.tip" v-html="BL.icon('info', 11)"></span>
                    </div>
                    <div class="cc-desc bl-muted">{{ c.desc }}</div>
                  </div>
                </div>

                <!-- 动态配置 -->
                <template v-if="form.constraint_type === 'Length'">
                  <div class="sec sub">长度约束</div>
                  <div class="ov-grid">
                    <FieldRow label="最小长度" inline><input type="number" class="bl-input" v-model.number="cfg.min" /></FieldRow>
                    <FieldRow label="最大长度" inline><input type="number" class="bl-input" v-model.number="cfg.max" /></FieldRow>
                  </div>
                </template>
                <template v-if="form.constraint_type === 'Regex'">
                  <div class="sec sub">正则约束</div>
                  <FieldRow label="正则表达式"><input class="bl-input bl-mono" v-model="cfg.pattern" placeholder="例: ^1[3-9]\d{9}$" /></FieldRow>
                </template>
                <template v-if="form.constraint_type === 'RID' || form.constraint_type === 'UUID'">
                  <div class="bl-muted" style="font-size:12px;padding:12px;background:var(--bl-bg-2);border-radius:6px">
                    {{ form.constraint_type === 'RID' ? '平台全局资源标识 · 自动生成不可编辑' : '通用唯一标识 · 自动生成不可编辑' }}
                  </div>
                </template>

                <!-- 枚举专属配置 -->
                <template v-if="form.constraint_type === 'Enum'">
                  <div class="sec sub">引用枚举</div>
                  <FieldRow label="关联枚举 *" inline>
                    <div class="bl-row" style="gap:6px;flex:1">
                      <select class="bl-input" v-model="form.enum_id">
                        <option value="">— 请选择 —</option>
                        <option v-for="e in enumTypes" :key="e.id" :value="e.id">{{ e.rdfs_label || e.api_name }} ({{ enumTypeLabel(e.enum_type) }})</option>
                      </select>
                      <button class="bl-btn bl-btn-sm" @click="goCreateEnum" title="跳转到枚举类型页面创建新枚举">创建</button>
                    </div>
                  </FieldRow>

                  <div class="sec sub">默认使用配置</div>
                  <div class="usage-row">
                    <div class="usage-col">
                      <div class="usage-lbl">最大选择层级</div>
                      <select class="bl-input" v-model.number="usageCfg.max_select_level">
                        <option :value="0">不限制</option>
                        <option :value="1">1 级</option>
                        <option :value="2">2 级</option>
                        <option :value="3">3 级</option>
                        <option :value="4">4 级</option>
                        <option :value="5">5 级</option>
                      </select>
                    </div>
                    <div class="usage-col usage-cb">
                      <div class="usage-lbl">非叶子节点</div>
                      <label class="bl-row" style="gap:6px;align-items:center;height:30px">
                        <input type="checkbox" v-model="usageCfg.allow_non_leaf" :true-value="1" :false-value="0" />
                        允许选择
                      </label>
                    </div>
                    <div class="usage-col">
                      <div class="usage-lbl">显示格式</div>
                      <select class="bl-input" v-model="usageCfg.display_format">
                        <option value="label">label · 仅名称</option>
                        <option value="code">code · 仅编码</option>
                        <option value="code_label">code_label · 编码+名称</option>
                        <option value="full_label">full_label · 完整路径</option>
                      </select>
                    </div>
                  </div>
                  <div class="usage-example bl-muted">
                    示例:行政区划 1=省 2=市 3=县 4=乡 5=村 ｜ 完整路径示例:北京市 &gt; 北京市 &gt; 东城区
                  </div>
                </template>
              </section>

              <!-- 预览验证 -->
              <section ref="prevSec" data-sec="preview">
                <div class="sec">预览验证</div>
                <div class="bl-muted" style="font-size:12px;margin-bottom:8px">
                  根据当前配置实时生成对应表单控件,输入测试值即时校验
                </div>

                <FieldRow label="测试值" inline>
                  <!-- 数值类 -->
                  <input v-if="form.base_type === 'Integer' || form.base_type === 'Decimal'"
                         type="number" class="bl-input bl-mono" v-model="testValue" placeholder="输入数值..." />
                  <!-- 布尔 -->
                  <select v-else-if="form.base_type === 'Boolean'" class="bl-input" v-model="testValue">
                    <option value="">—</option>
                    <option :value="true">true</option>
                    <option :value="false">false</option>
                  </select>
                  <!-- 日期 -->
                  <input v-else-if="form.base_type === 'DateTime'"
                         type="datetime-local" class="bl-input bl-mono" v-model="testValue" />
                  <!-- 枚举 -->
                  <select v-else-if="form.constraint_type === 'Enum'" class="bl-input" v-model="testValue">
                    <option value="">— 选择枚举项 —</option>
                    <option v-for="i in currentEnumItems" :key="i.id" :value="i.code">{{ formatEnumItem(i) }}</option>
                  </select>
                  <!-- 默认字符串 -->
                  <input v-else type="text" class="bl-input" v-model="testValue" placeholder="输入测试值..." />
                </FieldRow>

                <div :class="['validate-result', validateResult.ok && 'is-ok', !validateResult.ok && testValue !== '' && testValue !== undefined && 'is-fail']" v-if="testValue !== '' && testValue !== undefined && testValue !== null">
                  <span v-if="validateResult.ok">
                    <span v-html="BL.icon('check', 12, '#fff')"></span>
                    <span style="margin-left:6px">验证通过 ✓</span>
                  </span>
                  <span v-else>
                    <span v-html="BL.icon('x', 12, '#fff')"></span>
                    <span style="margin-left:6px">{{ validateResult.msg }}</span>
                  </span>
                </div>

                <div class="sec sub">配置概要</div>
                <table class="mini-summary">
                  <tr><td>基础类型</td><td><span class="bl-tag">{{ form.base_type || '—' }}</span></td></tr>
                  <tr><td>约束类型</td><td><span :class="['bl-tag', constraintTagCls(form.constraint_type)]">{{ form.constraint_type || '—' }}</span></td></tr>
                  <tr v-if="form.constraint_type === 'Length'"><td>长度范围</td><td>{{ cfg.min ?? '—' }} ~ {{ cfg.max ?? '—' }}</td></tr>
                  <tr v-if="form.constraint_type === 'Regex'"><td>正则</td><td class="bl-mono">{{ cfg.pattern || '—' }}</td></tr>
                  <tr v-if="form.constraint_type === 'Enum' && form.enum_id"><td>关联枚举</td><td>{{ enumLabel(form.enum_id) }}</td></tr>
                  <tr v-if="form.constraint_type === 'Enum'"><td>最大层级</td><td>{{ usageCfg.max_select_level === 0 ? '不限制' : usageCfg.max_select_level }}</td></tr>
                </table>
              </section>
            </div>
          </div>

          <div class="vt-ft">
            <button class="bl-btn" @click="drawerOpen=false">关闭</button>
            <button class="bl-btn bl-btn-primary" @click="submitForm">保存</button>
          </div>
        </aside>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FieldRow from '@/views/config/category/FieldRow.vue'
import { BL } from '@/lib/bl.js'
import { valueTypeApi, enumTypeApi, categoryApi } from '@/api'
import { useRouter } from 'vue-router'

const router = useRouter()

const baseTypes = ['String', 'Integer', 'Decimal', 'Boolean', 'DateTime']
const constraintTypes = [
  { k: 'RID',    desc: '平台全局资源标识', tip: '自动生成 · 不可编辑 · 唯一性由平台保证' },
  { k: 'UUID',   desc: '通用唯一标识',     tip: '自动生成 · 不可编辑 · RFC 4122 兼容' },
  { k: 'Length', desc: '长度范围约束',     tip: '校验字符串长度 min / max,适用 String 类型' },
  { k: 'Regex',  desc: '正则表达式校验',   tip: '使用 JS RegExp 语法,适用 String 类型' },
  { k: 'Enum',   desc: '关联枚举库',       tip: '从指定枚举库选择取值,适用单/多级枚举' }
]
const statusTabs = [
  { k: 'all', label: '全部' },
  { k: 'on',  label: '启用' },
  { k: 'off', label: '禁用' }
]
const tabs = [
  { k: 'meta',       label: '元数据',   icon: 'fileText' },
  { k: 'constraint', label: '约束配置', icon: 'sliders' },
  { k: 'preview',    label: '预览验证', icon: 'eye' }
]

const rows = ref([])
const enumTypes = ref([])
const allEnumItems = ref({})  // enumId -> items[]
const domainOpts = ref([])
const usageConfigsCache = ref([])  // 缓存 ont_valuetypes_usage_config,用于 openEdit 时回填 usageCfg
const q = ref('')
const filterStatus = ref('all')
const filterCategory = ref('')
const selected = ref(null)
const drawerOpen = ref(false)
const form = reactive({})
const cfg = reactive({})
const usageCfg = reactive({ max_select_level: 0, allow_non_leaf: 0, display_format: 'label' })
const checked = ref(new Set())
const page = ref(1)
const pageSize = ref(20)
const activeTab = ref('meta')

function statusCount(k) {
  if (k === 'all') return rows.value.length
  if (k === 'on')  return rows.value.filter(r => r.status === 1).length
  return rows.value.filter(r => r.status === 0).length
}

/* —— 排序 —— */
const sortKey = ref('')
const sortDir = ref('')
function toggleSort(key) {
  if (sortKey.value !== key) { sortKey.value = key; sortDir.value = 'asc' }
  else if (sortDir.value === 'asc') sortDir.value = 'desc'
  else { sortKey.value = ''; sortDir.value = '' }
}
function sortArrow(key) {
  if (sortKey.value !== key) return ' ⇅'
  return sortDir.value === 'asc' ? ' ↑' : ' ↓'
}

const filtered = computed(() => {
  let list = rows.value
  if (filterStatus.value === 'on')  list = list.filter(r => r.status === 1)
  if (filterStatus.value === 'off') list = list.filter(r => r.status === 0)
  if (filterCategory.value) list = list.filter(r => r.category_code === filterCategory.value)
  const k = q.value.trim().toLowerCase()
  if (k) list = list.filter(r => [r.api_name, r.rdfs_label, r.rid, r.rdfs_comment, r.base_type, r.constraint_type]
    .filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
  if (sortKey.value && sortDir.value) {
    const dir = sortDir.value === 'asc' ? 1 : -1
    list = [...list].sort((a, b) => {
      let va, vb
      switch (sortKey.value) {
        case 'name':       va = a.rdfs_label || a.api_name || ''; vb = b.rdfs_label || b.api_name || ''; break
        case 'api':        va = a.api_name || ''; vb = b.api_name || ''; break
        case 'domain':     va = a.category_code || ''; vb = b.category_code || ''; break
        case 'base':       va = a.base_type || ''; vb = b.base_type || ''; break
        case 'constraint': va = a.constraint_type || ''; vb = b.constraint_type || ''; break
        case 'enum':       va = a.enum_label || a.enum_api_name || ''; vb = b.enum_label || b.enum_api_name || ''; break
        case 'status':     va = a.status ?? 9; vb = b.status ?? 9; break
        default:           va = ''; vb = ''
      }
      if (typeof va === 'number') return (va - vb) * dir
      return String(va).localeCompare(String(vb)) * dir
    })
  }
  return list
})
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize.value)))
const paged = computed(() => filtered.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))

watch([filterStatus, filterCategory, q], () => { page.value = 1 })

const allChecked = computed(() => paged.value.length > 0 && paged.value.every(r => checked.value.has(r.id)))
function toggleCheck(id) {
  const s = new Set(checked.value)
  s.has(id) ? s.delete(id) : s.add(id)
  checked.value = s
}
function toggleAll() {
  const s = new Set(checked.value)
  if (allChecked.value) paged.value.forEach(r => s.delete(r.id))
  else paged.value.forEach(r => s.add(r.id))
  checked.value = s
}

function constraintTagCls(t) {
  if (t === 'Enum')  return 'bl-tag-success'
  if (t === 'Regex') return 'bl-tag-warning'
  if (t === 'RID' || t === 'UUID') return 'bl-tag-primary'
  return ''
}
function clearFilters() { filterStatus.value = 'all'; filterCategory.value = ''; q.value = '' }

async function load() {
  rows.value = await valueTypeApi.list().catch(() => [])
  enumTypes.value = await enumTypeApi.list().catch(() => [])
  usageConfigsCache.value = await valueTypeApi.listUsageConfigs().catch(() => [])
  // 业务领域候选
  if (!domainOpts.value.length) {
    const tree = await categoryApi.tree().catch(() => [])
    const list = []
    const walk = (ns) => ns.forEach(n => { if (n.categoryCode) list.push({ code: n.categoryCode, name: n.label }); if (n.children) walk(n.children) })
    walk(tree)
    domainOpts.value = list
  }
}
onMounted(load)

function deriveApi(label) {
  if (!label) return ''
  // 简化版:中文 → 取拼音首字母(无库支持时回退到英文/数字+下划线)
  let s = String(label).trim().toLowerCase()
  s = s.replace(/[^\w一-龥]+/g, '_')   // 非字母/数字/下划线/中文 → _
  s = s.replace(/_+/g, '_').replace(/^_|_$/g, '')
  return s
}
function onLabelInput() {
  if (!form.id && !form._apiTouched) {
    form.api_name = deriveApi(form.rdfs_label)
  }
}

function openCreate() {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, { base_type: 'String', constraint_type: 'Length', status: 1, enum_id: '', category_code: '', default_usage_config_id: null, _apiTouched: false })
  Object.keys(cfg).forEach(k => delete cfg[k]); cfg.min = 0; cfg.max = 255
  Object.assign(usageCfg, { max_select_level: 0, allow_non_leaf: 0, display_format: 'label' })
  testValue.value = ''
  ensureDrawerSize()
  activeTab.value = 'meta'
  drawerOpen.value = true
  nextTick(() => paneRef.value?.scrollTo({ top: 0 }))
}
function openEdit(r) {
  selected.value = r
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, r, { _apiTouched: true })
  form.status = Number(form.status ?? 1)
  Object.keys(cfg).forEach(k => delete cfg[k])
  try {
    const parsed = r.constraint_config ? (typeof r.constraint_config === 'string' ? JSON.parse(r.constraint_config) : r.constraint_config) : {}
    Object.assign(cfg, parsed)
  } catch {}
  // 回填默认使用配置 (Enum 才有效,其它约束类型也复位以免脏数据残留)
  const uc = r.default_usage_config_id
    ? usageConfigsCache.value.find(x => x.id === r.default_usage_config_id)
    : null
  Object.assign(usageCfg, {
    max_select_level: uc ? Number(uc.max_select_level ?? 0) : 0,
    allow_non_leaf:   uc ? Number(uc.allow_non_leaf ?? 0)   : 0,
    display_format:   uc ? (uc.display_format || 'label')   : 'label'
  })
  // load enum items if Enum
  if (r.constraint_type === 'Enum' && r.enum_id) loadEnumItems(r.enum_id)
  testValue.value = ''
  ensureDrawerSize()
  activeTab.value = 'meta'
  drawerOpen.value = true
  nextTick(() => paneRef.value?.scrollTo({ top: 0 }))
}
function onPickConstraint(c) {
  form.constraint_type = c
  Object.keys(cfg).forEach(k => delete cfg[k])
  if (c === 'Length') { cfg.min = 0; cfg.max = 255 }
  if (c === 'Regex')  { cfg.pattern = '' }
  if (c !== 'Enum')   { form.enum_id = '' }
  testValue.value = ''
}

async function submitForm() {
  if (!form.rdfs_label || !form.api_name || !form.base_type) { BL.warning('名称 / API / 基础类型为必填'); return }
  if (form.constraint_type === 'Enum' && !form.enum_id) { BL.warning('请选择关联枚举'); return }
  if (form.constraint_type !== 'Enum') form.constraint_config = JSON.stringify(cfg)
  else form.constraint_config = null

  // 持久化 默认使用配置 (仅 Enum 约束): 复用已有的就 update,否则 create
  if (form.constraint_type === 'Enum') {
    const ucPayload = {
      max_select_level: Number(usageCfg.max_select_level ?? 0),
      allow_non_leaf:   Number(usageCfg.allow_non_leaf ?? 0),
      display_format:   usageCfg.display_format || 'label',
      is_system_default: 0
    }
    try {
      if (form.default_usage_config_id) {
        await valueTypeApi.updateUsageConfig(form.default_usage_config_id, ucPayload)
      } else {
        const created = await valueTypeApi.createUsageConfig(ucPayload)
        if (created && created.id) form.default_usage_config_id = created.id
      }
    } catch (e) {
      BL.warning('使用配置保存失败,但值类型仍会尝试保存')
    }
  } else {
    // 非 Enum 约束: 清空 default_usage_config_id (避免脏关联)
    form.default_usage_config_id = null
  }

  const payload = { ...form }; delete payload._apiTouched
  if (form.id) await valueTypeApi.update(form.id, payload)
  else         await valueTypeApi.create(payload)
  BL.success('已保存')
  drawerOpen.value = false
  await load()
}
async function doDelete(r) {
  const ok = await BL.confirm({ title:'删除值类型', content:`确定删除「${r.rdfs_label || r.api_name}」？已被引用的属性会受影响。`, danger:true, okText:'删除' })
  if (!ok) return
  await valueTypeApi.remove(r.id)
  BL.success('已删除')
  await load()
}
async function toggleOne(r) {
  const next = r.status === 1 ? 0 : 1
  await valueTypeApi.update(r.id, { ...r, status: next })
  await load()
}
async function batchRemove() {
  const ids = [...checked.value]
  if (!ids.length) return
  const ok = await BL.confirm({ title: '批量删除', content: `确定删除选中的 ${ids.length} 个值类型？`, danger: true, okText: '删除' })
  if (!ok) return
  for (const id of ids) await valueTypeApi.remove(id).catch(() => {})
  BL.success('已删除')
  checked.value = new Set()
  await load()
}
async function batchSetStatus(status) {
  for (const id of checked.value) {
    const row = rows.value.find(r => r.id === id)
    if (row) await valueTypeApi.update(id, { ...row, status }).catch(() => {})
  }
  BL.success(status === 1 ? '已启用' : '已禁用')
  await load()
}

function goCreateEnum() { router.push('/resources/enum-types') }

/* —— 枚举项展开(供预览验证下拉) —— */
async function loadEnumItems(enumId) {
  if (!enumId || allEnumItems.value[enumId]) return
  const list = await enumTypeApi.listItems(enumId).catch(() => [])
  allEnumItems.value[enumId] = list
}
const currentEnumItems = computed(() => form.enum_id ? (allEnumItems.value[form.enum_id] || []) : [])
function enumLabel(id) {
  const e = enumTypes.value.find(x => x.id === id)
  return e ? (e.rdfs_label || e.api_name) : id
}
function enumTypeLabel(t) {
  return ({ general_single: '一级通用', general_multi: '多级通用', biz_single: '业务一级', biz_multi: '业务多级' })[t] || t
}
function formatEnumItem(i) {
  switch (usageCfg.display_format) {
    case 'code':       return i.code
    case 'code_label': return `${i.code} ${i.label}`
    case 'full_label': return i.label  // 简化:实际需走 parent_code 链
    case 'label':
    default:           return i.label
  }
}
watch(() => form.enum_id, (v) => { if (v) loadEnumItems(v) })

/* —— 测试值 + 校验 —— */
const testValue = ref('')
const validateResult = computed(() => {
  const v = testValue.value
  if (v === '' || v === undefined || v === null) return { ok: false, msg: '' }
  switch (form.base_type) {
    case 'Integer':
      if (!Number.isInteger(Number(v))) return { ok: false, msg: '不是合法的整数' }
      break
    case 'Decimal':
      if (Number.isNaN(Number(v))) return { ok: false, msg: '不是合法的数值' }
      break
    case 'Boolean':
      if (v !== true && v !== false && v !== 'true' && v !== 'false') return { ok: false, msg: '布尔值只能是 true / false' }
      break
    case 'DateTime':
      if (Number.isNaN(Date.parse(v))) return { ok: false, msg: '不是合法的日期时间' }
      break
  }
  switch (form.constraint_type) {
    case 'Length': {
      const len = String(v).length
      if (cfg.min != null && len < cfg.min) return { ok: false, msg: `长度需 ≥ ${cfg.min} (当前 ${len})` }
      if (cfg.max != null && len > cfg.max) return { ok: false, msg: `长度需 ≤ ${cfg.max} (当前 ${len})` }
      break
    }
    case 'Regex': {
      if (cfg.pattern) {
        try { if (!(new RegExp(cfg.pattern).test(String(v)))) return { ok: false, msg: '不匹配正则表达式' } }
        catch { return { ok: false, msg: '正则表达式语法错误' } }
      }
      break
    }
    case 'Enum': {
      const items = currentEnumItems.value
      if (!items.length) return { ok: false, msg: '枚举尚未加载' }
      if (!items.some(i => String(i.code) === String(v))) return { ok: false, msg: '不在枚举范围内' }
      break
    }
    case 'RID':  if (!/^ri\..+/.test(String(v))) return { ok: false, msg: 'RID 应以 "ri." 开头' }; break
    case 'UUID': if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(String(v))) return { ok: false, msg: 'UUID 格式不合法 (RFC 4122)' }; break
  }
  return { ok: true, msg: '' }
})

/* —— Tab 切换 + 滚动联动 (scrollspy) —— */
const paneRef = ref(null)
const metaSec = ref(null)
const conSec = ref(null)
const prevSec = ref(null)

// 滚动到段落顶部时预留的偏移 (与 .vt-pane padding-top 一致,确保 .sec 标题完整可见)
const SCROLL_OFFSET = 10

function onPickTab(k) {
  activeTab.value = k
  const target = k === 'meta' ? metaSec.value : k === 'constraint' ? conSec.value : prevSec.value
  if (!target || !paneRef.value) return
  // 元数据 → 直接滚到顶,其他段落减去顶部 padding 让标题贴齐可视区上沿
  const top = k === 'meta' ? 0 : Math.max(0, target.offsetTop - SCROLL_OFFSET)
  paneRef.value.scrollTo({ top, behavior: 'smooth' })
}
function onPaneScroll() {
  if (!paneRef.value || !metaSec.value || !conSec.value || !prevSec.value) return
  // 让 spy 阈值跟 onPickTab 的落点对齐(再加少量缓冲,避免高亮抖动)
  const top = paneRef.value.scrollTop + SCROLL_OFFSET + 8
  if (top < conSec.value.offsetTop) activeTab.value = 'meta'
  else if (top < prevSec.value.offsetTop) activeTab.value = 'constraint'
  else activeTab.value = 'preview'
}

/* —— 抽屉拖拽 —— */
const drawerWidth = ref(0)
const resizing = ref(false)
const DRAWER_MIN = 480
function drawerMaxPx() { return Math.floor(window.innerWidth * 0.85) }
function defaultDrawerWidth() { return Math.max(DRAWER_MIN, Math.min(720, Math.floor(window.innerWidth * 0.50))) }
function ensureDrawerSize() { if (!drawerWidth.value) drawerWidth.value = defaultDrawerWidth() }
let dragStartX = 0, dragStartW = 0
function onDragStart(e) {
  resizing.value = true
  dragStartX = e.clientX; dragStartW = drawerWidth.value
  document.body.style.cursor = 'col-resize'; document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onDragMove); window.addEventListener('mouseup', onDragEnd)
}
function onDragMove(e) {
  const dx = dragStartX - e.clientX
  drawerWidth.value = Math.min(drawerMaxPx(), Math.max(DRAWER_MIN, dragStartW + dx))
}
function onDragEnd() {
  resizing.value = false
  document.body.style.cursor = ''; document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onDragMove); window.removeEventListener('mouseup', onDragEnd)
}
onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onDragMove); window.removeEventListener('mouseup', onDragEnd)
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }

/* —— 顶部分段状态 —— */
.seg { display: inline-flex; background: var(--bl-bg-2); border-radius: var(--bl-radius-2); padding: 2px; gap: 2px; }
.seg-btn {
  height: 26px; padding: 0 12px;
  border: 0; background: transparent;
  font-size: var(--bl-fs-12); color: var(--bl-text-2); cursor: pointer;
  border-radius: 4px;
  display: inline-flex; align-items: center; gap: 6px;
}
.seg-btn.is-on { background: var(--bl-bg-1); color: var(--bl-primary); box-shadow: var(--bl-shadow-1); font-weight: 500; }
.seg-cnt { font-size: var(--bl-fs-11); color: var(--bl-text-3); }
.seg-btn.is-on .seg-cnt { color: var(--bl-primary); }

.hd-filter { width: 140px; }
.search-wrap { position: relative; width: 240px; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.search-input { padding-left: 30px; padding-right: 26px; }
.search-clear { position: absolute; right: 6px; top: 50%; transform: translateY(-50%);
  width: 16px; height: 16px; border: 0; background: var(--bl-bg-3); color: var(--bl-text-3);
  border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; cursor: pointer; }

/* —— 主体 —— */
.vt-body { flex: 1; position: relative; display: flex; padding: 12px; overflow: hidden; }
.list-card { flex: 1; overflow: hidden; display: flex; flex-direction: column; min-width: 0; }

.list-scroll { flex: 1; min-height: 0; overflow: auto; }
.vt-table { width: 100%; }
.vt-table thead th { position: sticky; top: 0; background: var(--bl-bg-2); z-index: 2; box-shadow: inset 0 -1px 0 var(--bl-divider); }
.th-sort { cursor: pointer; user-select: none; display: inline-flex; align-items: center; white-space: nowrap; }
.th-sort:hover { color: var(--bl-primary); }
.th-arrow { color: var(--bl-text-4); font-size: 11px; margin-left: 2px; }
.vt-row { cursor: pointer; }
.vt-row:hover { background: var(--bl-bg-hover); }
.vt-row.is-active { background: var(--bl-primary-soft); }
.vt-name { font-weight: 500; color: var(--bl-text-1); }
.t-actions { white-space: nowrap; }

.vt-pager {
  flex-shrink: 0; padding: 8px 12px; border-top: 1px solid var(--bl-divider);
  display: flex; justify-content: space-between; align-items: center;
  gap: 12px;
  font-size: var(--bl-fs-12);
}
.vt-pager-l { display: flex; align-items: center; flex-wrap: wrap; gap: 0; min-width: 0; }
.vt-page-size { width: 64px; height: 26px; }

/* —— 抽屉 —— */
.vt-drawer {
  position: absolute; top: 0; right: 0; bottom: 0;
  background: var(--bl-bg-1);
  border-left: 1px solid var(--bl-border);
  box-shadow: -2px 0 12px rgba(0,0,0,.06);
  display: flex; flex-direction: column; min-width: 480px; z-index: 5;
}
.vt-drag-handle { position: absolute; left: -2px; top: 0; bottom: 0; width: 5px;
  cursor: col-resize; transition: background-color .15s; z-index: 6; }
.vt-drag-handle:hover, .vt-drag-handle.is-resizing { background: var(--bl-primary); }
.vt-hd { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border-bottom: 1px solid var(--bl-divider); flex-shrink: 0; }
.vt-ic { width: 32px; height: 32px; border-radius: 6px; background: var(--bl-primary);
  display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.vt-title { font-size: 14px; font-weight: 600; }

.vt-body-2col { flex: 1; display: grid; grid-template-columns: 140px 1fr; overflow: hidden; min-height: 0; }
.vt-tabs { background: #f5f7fa; border-right: 1px solid var(--bl-divider);
  padding: 8px 6px; display: flex; flex-direction: column; gap: 2px; overflow: auto; }
.vt-tab {
  text-align: left; padding: 8px 12px; border: 0; background: transparent;
  cursor: pointer; font-size: 13px; color: var(--bl-text-2);
  border-radius: 4px; border-left: 3px solid transparent;
  display: inline-flex; align-items: center;
}
.vt-tab:hover { background: var(--bl-bg-1); color: var(--bl-text-1); }
.vt-tab.is-on { background: var(--bl-bg-1); color: var(--bl-primary);
  font-weight: 500; border-left-color: var(--bl-primary); }

.vt-pane { position: relative; overflow: auto; padding: 10px 14px; min-width: 0; }
.vt-pane > section { padding-bottom: 16px; border-bottom: 1px dashed var(--bl-divider); margin-bottom: 8px; }
.vt-pane > section:last-child { border-bottom: 0; }

.sec { font-size: 12px; color: var(--bl-text-3); margin: 6px 0 6px;
  padding-left: 8px; border-left: 3px solid var(--bl-primary); font-weight: 500; }
.sec.sub { margin-top: 14px; }

.ov-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 4px 16px; }

.constraint-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 4px; }
.constraint-card { padding: 10px 12px; border: 1px solid var(--bl-border); border-radius: 6px;
  cursor: pointer; transition: border-color .15s, background-color .15s; }
.constraint-card:hover { border-color: var(--bl-primary); }
.constraint-card.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.cc-row { display: flex; align-items: center; justify-content: space-between; }
.cc-title { font-weight: 600; font-size: 13px; }
.cc-help { color: var(--bl-text-3); cursor: help; display: inline-flex; }
.cc-help:hover { color: var(--bl-primary); }
.cc-desc  { font-size: 11.5px; margin-top: 2px; }

/* 默认使用配置 3 列 */
.usage-row { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; }
.usage-col { display: flex; flex-direction: column; gap: 4px; }
.usage-lbl { font-size: 12px; color: var(--bl-text-3); }
.usage-example { font-size: 11.5px; margin-top: 8px; padding: 8px 10px;
  background: var(--bl-bg-2); border-radius: 4px; }

.validate-result {
  margin-top: 10px; padding: 10px 14px; border-radius: 6px; font-size: 13px; font-weight: 500;
  display: inline-flex; align-items: center;
}
.validate-result.is-ok   { background: var(--bl-success); color: #fff; }
.validate-result.is-fail { background: var(--bl-danger);  color: #fff; }

.mini-summary { width: 100%; font-size: 12px; margin-top: 6px; }
.mini-summary td { padding: 4px 8px; border-bottom: 1px dashed var(--bl-divider); }
.mini-summary td:first-child { color: var(--bl-text-3); width: 90px; }

.radio-group { display: inline-flex; gap: 20px; }
.radio-item { display: inline-flex; align-items: center; gap: 6px; cursor: pointer; font-size: 13px; }

.vt-ft { padding: 10px 14px; display: flex; justify-content: flex-end; gap: 8px; border-top: 1px solid var(--bl-divider); flex-shrink: 0; }

.vt-drawer-enter-active, .vt-drawer-leave-active { transition: transform .25s, opacity .2s; }
.vt-drawer-enter-from, .vt-drawer-leave-to { transform: translateX(20px); opacity: 0; }

/* 抽屉内 FieldRow inline label 加宽避免换行 */
.vt-drawer :deep(.fr.fr-inline .fr-label) { width: 78px; }
</style>
