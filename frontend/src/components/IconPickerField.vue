<template>
  <div class="ipf">
    <div v-if="label" class="ipf-head">
      <span class="ipf-label">{{ label }}</span>
    </div>
    <div class="icon-grid">
      <span v-for="ic in presets" :key="ic"
            :class="['icon-cell', modelValue === ic && 'is-active']"
            :title="shortIconName(ic)"
            @click="pickOne(ic)"
            v-html="BL.icon(ic, 16)"></span>
    </div>
    <!-- 更多选择：网格下方独立一行 -->
    <button class="ipf-more-row" @click="open">
      <span v-html="BL.icon('grid', 12)"></span>
      <span>更多选择</span>
    </button>

    <!-- 模态选择器 -->
    <Teleport to="body">
      <div v-if="open_" class="bl-modal-mask" @click.self="cancel">
        <div class="bl-modal icon-picker-modal">
          <div class="bl-modal-hd ip-hd">
            <span>图标选择</span>
            <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="关闭" @click="cancel" v-html="BL.icon('x', 14)"></button>
          </div>
          <div class="bl-modal-body ip-body">
            <aside class="ip-side">
              <!-- 业务图库 -->
              <div class="ip-side-section">
                <div class="ip-side-head" @click="customExpanded = !customExpanded">
                  <span class="ip-side-head-chev" :class="customExpanded && 'is-open'" v-html="BL.icon('chevronRight', 11)"></span>
                  <span class="ip-side-head-title">业务图库</span>
                  <span class="ip-side-head-count">{{ customTopGroups.length }}</span>
                  <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="新建编组"
                          @click.stop="createGroup()" v-html="BL.icon('plus', 12)"></button>
                </div>
                <div class="ip-side-list" v-show="customExpanded">
                  <template v-for="top in customTopGroups" :key="top.id">
                    <div :class="['ip-cat ip-cat-custom ip-cat-parent', iconCat===top.id && 'is-active']"
                         @click="iconCat = top.id">
                      <span class="ip-cat-parent-chev" :class="expandedParents.has(top.id) && 'is-open'"
                            @click.stop="toggleParent(top.id)" v-html="BL.icon('chevronRight', 10)"></span>
                      <template v-if="renameKey === top.id">
                        <input ref="renameInputRef" class="bl-input ip-cat-rename"
                               v-model="renameVal" @keydown.enter="commitRename" @keydown.esc="cancelRename"
                               @blur="commitRename" @click.stop />
                      </template>
                      <template v-else>
                        <span class="ip-cat-label">{{ top.name }}</span>
                        <span class="ip-cat-actions">
                          <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="新建子分组" @click.stop="createGroup(top.id)" v-html="BL.icon('plus', 11)"></button>
                          <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="重命名" @click.stop="startRename(top)" v-html="BL.icon('edit', 11)"></button>
                          <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="删除编组" @click.stop="removeGroup(top)" v-html="BL.icon('trash', 11)"></button>
                        </span>
                        <span class="ip-cat-count">{{ aggregateIcons(top).length }}</span>
                      </template>
                    </div>
                    <template v-if="expandedParents.has(top.id)">
                      <div v-for="child in childrenOf(top.id)" :key="child.id"
                           :class="['ip-cat ip-cat-custom ip-cat-child', iconCat===child.id && 'is-active']"
                           @click="iconCat = child.id">
                        <template v-if="renameKey === child.id">
                          <input ref="renameInputRef" class="bl-input ip-cat-rename"
                                 v-model="renameVal" @keydown.enter="commitRename" @keydown.esc="cancelRename"
                                 @blur="commitRename" @click.stop />
                        </template>
                        <template v-else>
                          <span class="ip-cat-label">{{ child.name }}</span>
                          <span class="ip-cat-actions">
                            <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="重命名" @click.stop="startRename(child)" v-html="BL.icon('edit', 11)"></button>
                            <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="删除编组" @click.stop="removeGroup(child)" v-html="BL.icon('trash', 11)"></button>
                          </span>
                          <span class="ip-cat-count">{{ (iconsByGroup[child.id] || []).length }}</span>
                        </template>
                      </div>
                    </template>
                  </template>
                  <div v-if="!customTopGroups.length" class="ip-side-empty">尚无自定义编组</div>
                </div>
              </div>
              <!-- 内置图标库 -->
              <div class="ip-side-section">
                <div class="ip-side-head" @click="builtinExpanded = !builtinExpanded">
                  <span class="ip-side-head-chev" :class="builtinExpanded && 'is-open'" v-html="BL.icon('chevronRight', 11)"></span>
                  <span class="ip-side-head-title">内置图标库</span>
                  <span class="ip-side-head-count">{{ ICON_CATEGORIES.length }}</span>
                </div>
                <div class="ip-side-list" v-show="builtinExpanded">
                  <div v-for="cat in ICON_CATEGORIES" :key="cat.key"
                       :class="['ip-cat', iconCat===cat.key && 'is-active']"
                       @click="iconCat = cat.key">
                    <span>{{ cat.label }}</span>
                    <span class="ip-cat-count">{{ cat.icons.length }}</span>
                  </div>
                </div>
              </div>
            </aside>
            <div class="ip-main">
              <div class="ip-toolbar">
                <div class="ip-crumb">
                  <span class="bl-muted">图标</span>
                  <span class="crumb-sep" v-html="BL.icon('chevronRight', 11)"></span>
                  <span>{{ currentCatLabel }}</span>
                </div>
                <div class="ip-toolbar-right">
                  <template v-if="isCustomCat">
                    <input ref="uploadInputRef" type="file" accept=".svg,image/svg+xml" multiple style="display:none" @change="onSvgFiles" />
                    <template v-if="!batchMode">
                      <button class="bl-btn bl-btn-sm" @click="toggleBatch" :disabled="!pickerIcons.length">
                        <span v-html="BL.icon('check', 12)"></span><span style="margin-left:4px">批量操作</span>
                      </button>
                      <button class="bl-btn bl-btn-primary bl-btn-sm" @click="triggerUpload">
                        <span v-html="BL.icon('upload', 12, '#fff')"></span><span style="margin-left:4px">上传 SVG</span>
                      </button>
                    </template>
                    <template v-else>
                      <span class="ip-batch-info">已选 <b>{{ batchSelected.size }}</b> / {{ pickerIcons.length }}</span>
                      <button class="bl-btn bl-btn-sm" @click="batchSelectAll">全选</button>
                      <button class="bl-btn bl-btn-sm" :disabled="!batchSelected.size" @click="batchClear">取消选择</button>
                      <button class="bl-btn bl-btn-sm bl-btn-danger" :disabled="!batchSelected.size" @click="batchDelete">
                        <span v-html="BL.icon('trash', 12, '#fff')"></span><span style="margin-left:4px">删除 ({{ batchSelected.size }})</span>
                      </button>
                      <button class="bl-btn bl-btn-sm" @click="toggleBatch">退出批量</button>
                    </template>
                  </template>
                  <div class="ip-search">
                    <span class="ip-search-ic" v-html="BL.icon('search', 12)"></span>
                    <input class="bl-input ip-search-input" v-model="iconQ" placeholder="搜索图标名称…" />
                  </div>
                </div>
              </div>
              <div class="ip-grid" v-if="pickerIcons.length">
                <span v-for="ic in pickerIcons" :key="ic"
                      :class="['icon-cell ip-cell',
                               !batchMode && iconStaged===ic && 'is-active',
                               batchMode && batchSelected.has(ic) && 'is-batch-on']"
                      :title="shortIconName(ic)"
                      @click="batchMode ? toggleBatchSelect(ic) : (iconStaged = ic)"
                      @dblclick="!batchMode && (iconStaged = ic, confirmPick())">
                  <span v-html="BL.icon(ic, 20)"></span>
                  <span v-if="batchMode" class="ip-cell-check" :class="batchSelected.has(ic) && 'is-on'"
                        v-html="batchSelected.has(ic) ? BL.icon('check', 10, '#fff') : ''"></span>
                  <button v-else-if="isCustomCat" class="ip-cell-x" title="移除"
                          @click.stop="removeIcon(ic)" v-html="BL.icon('x', 10)"></button>
                </span>
              </div>
              <div v-else-if="isCustomCat" class="bl-empty" style="padding:48px 0">
                <div class="bl-empty-icon" v-html="BL.icon('folder', 32)"></div>
                点击右上「上传 SVG」批量添加图标<br/>
                <span class="bl-muted" style="font-size:12px">支持阿里字库 / Iconfont 导出的单色 SVG</span>
              </div>
              <div v-else class="bl-empty" style="padding:48px 0">未找到匹配的图标</div>
            </div>
          </div>
          <div class="bl-modal-ft">
            <button class="bl-btn" @click="cancel">取消</button>
            <button class="bl-btn bl-btn-primary" :disabled="!iconStaged" @click="confirmPick">确定</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { BL, antdIconNames,
         getCustomIconData, setCustomIconData, onCustomIconsChange,
         customIconUpsert, customIconRemoveByIds, customGroupUpsert, customGroupRemove } from '@/lib/bl.js'
import { ANTD_ZH, antdZhName } from '@/lib/icon-zh.js'
import { iconLibApi } from '@/api'

const props = defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, default: '图标' },
  suggestName: { type: String, default: '' },
  presetCount: { type: Number, default: 40 }
})
const emit = defineEmits(['update:modelValue', 'change'])

/* ===== 内置图标 + Ant Design ===== */
const iconChoices = [
  'search','plus','minus','edit','trash','more','moreV','check','x','copy','save','refresh','scissors',
  'chevronRight','chevronLeft','chevronUp','chevronDown','arrowUp','arrowDown','arrowLeft','arrowRight',
  'home','menu','folder','folderOpen','folderPlus','file','fileText','fileCode','clipboard','book','bookmark','archive','inbox',
  'mail','phone','message','chat','send','image','video','camera','microphone','music','volume','play','pause','stop',
  'clock','calendar','timer','history','hourglass','info','alert','success','error','help','lock','unlock','eye','eyeOff',
  'database','list','grid','cube','box','package','layers','network','link','share','branch','workflow','hash','tag',
  'chart','barChart','pieChart','trendingUp','trendingDown','activity','dashboard','user','users','userPlus','team',
  'bell','cog','settings','sliders','tool','filter','power','logout','login','sun','moon','maximize','minimize',
  'star','heart','flag','award','gift','globe','map','pin','compass','cloud','rain','snowflake','umbrella','thermometer',
  'flame','zap','battery','wifi','bluetooth','wave','droplet','dam','station','ship','leaf','tree','wheat',
  'industry','factory','building','warehouse','briefcase','shield','car','truck','bus','plane','bike','rocket',
  'dollar','creditCard','wallet','percent','monitor','smartphone','tablet','laptop','server','printer',
  'mouse','keyboard','layout','columns','sidebar','rows','code','terminal','ai','ribbon'
]
const ICON_KEYWORDS = {
  droplet:['水','液'], wave:['浪','波','河'], dam:['坝','闸'], station:['站','测','监测'],
  factory:['工厂','工业'], industry:['行业','产业'], tree:['林','树'], leaf:['叶','植','生态'],
  shield:['防','安全'], car:['车','交通'], user:['用户','人员'], bell:['通知','提醒'],
  ai:['智能','算法'], folder:['目录','文件夹','分类'], database:['库','数据','存储'],
  network:['网','互联'], link:['连','联','链','关联'], share:['共享','分享'],
  cube:['对象','实体'], branch:['分支','层'], workflow:['工作流','流程'],
  zap:['动作','执行'], cog:['配置','设置'], settings:['设置'], sliders:['调节','开关']
}
const ANTD_OUTLINED = antdIconNames('outlined')
const antd = (n) => 'antd:' + n
const ANTD_ALL = ANTD_OUTLINED.map(antd)
const dedupe = (arr) => Array.from(new Set(arr))
const ICON_CATEGORIES = [
  { key: 'all',      label: '全部',     icons: dedupe([...iconChoices, ...ANTD_ALL]) },
  { key: 'common',   label: '常用',     icons: dedupe(['folder','grid','database','cube','link','share','user','cog','list','network', antd('folder'),antd('database'),antd('cluster'),antd('apartment'),antd('api'),antd('tags'),antd('search'),antd('edit'),antd('setting'),antd('tool')]) },
  { key: 'ontology', label: '本体/知识图谱', icons: dedupe([antd('apartment'),antd('partition'),antd('cluster'),antd('deployment-unit'),antd('branches'),antd('share-alt'),antd('node-collapse'),antd('node-expand'),antd('node-index'),antd('sisternode'),antd('subnode'),antd('fork'),antd('block'),antd('database'),antd('container'),antd('api'),antd('function'),antd('experiment'),antd('robot'),antd('open-ai'),antd('build'),antd('bulb'),antd('console-sql'),antd('code'),'cube','network','branch','link','share','workflow','layers']) },
  { key: 'arrow',    label: '方向/箭头', icons: dedupe([antd('arrow-up'),antd('arrow-down'),antd('arrow-left'),antd('arrow-right'),antd('caret-up'),antd('caret-down'),antd('caret-left'),antd('caret-right'),antd('up'),antd('down'),antd('left'),antd('right'),antd('double-left'),antd('double-right'),antd('swap'),antd('rollback'),antd('undo'),antd('redo'),antd('rise'),antd('fall')]) },
  { key: 'feedback', label: '反馈/状态', icons: dedupe([antd('check'),antd('close'),antd('plus'),antd('minus'),antd('check-circle'),antd('close-circle'),antd('question-circle'),antd('exclamation-circle'),antd('info-circle'),antd('warning'),antd('stop'),antd('sync'),antd('reload')]) },
  { key: 'file',     label: '文件/文档', icons: dedupe([antd('file'),antd('file-add'),antd('file-text'),antd('file-search'),antd('file-pdf'),antd('file-excel'),antd('file-word'),antd('file-image'),antd('folder'),antd('folder-open'),antd('folder-add'),antd('snippets'),antd('book')]) },
  { key: 'antd',     label: 'Ant Design 全集', icons: ANTD_ALL }
]

/* ===== 自定义图库 ===== */
const customData = ref(getCustomIconData())
let unsub = null
const customLoaded = ref(false)

const iconsByGroup = computed(() => {
  const m = {}
  for (const ic of (customData.value.icons || [])) {
    if (!m[ic.groupId]) m[ic.groupId] = []
    m[ic.groupId].push('custom:' + ic.id)
  }
  return m
})
const customTopGroups = computed(() =>
  (customData.value.groups || []).filter(g => !g.parentId).sort((a,b) => (a.sort||0)-(b.sort||0))
)
const childrenOf = (id) =>
  (customData.value.groups || []).filter(g => g.parentId === id).sort((a,b) => (a.sort||0)-(b.sort||0))
function aggregateIcons(g) {
  const own = iconsByGroup.value[g.id] || []
  if (g.parentId) return own
  const kids = (customData.value.groups || []).filter(x => x.parentId === g.id)
  if (!kids.length) return own
  const seen = new Set(), out = []
  for (const list of [own, ...kids.map(c => iconsByGroup.value[c.id] || [])]) {
    for (const ic of list) if (!seen.has(ic)) { seen.add(ic); out.push(ic) }
  }
  return out
}

async function loadLib() {
  try {
    const data = await iconLibApi.all()
    setCustomIconData(data || { groups: [], icons: [] })
    const s = new Set()
    for (const g of (data?.groups || [])) if (!g.parentId) s.add(g.id)
    expandedParents.value = s
    customLoaded.value = true
    if (!(data?.groups || []).length) {
      try {
        await iconLibApi.seed(false)
        const reloaded = await iconLibApi.all()
        setCustomIconData(reloaded || { groups: [], icons: [] })
        const s2 = new Set()
        for (const g of (reloaded?.groups || [])) if (!g.parentId) s2.add(g.id)
        expandedParents.value = s2
      } catch {}
    }
  } catch {
    BL.warning('「业务图库」加载失败')
  }
}

/* ===== 模态 state ===== */
const open_ = ref(false)
const iconStaged = ref('')
const iconQ = ref('')
const iconCat = ref('all')
const customExpanded = ref(true)
const builtinExpanded = ref(true)
const expandedParents = ref(new Set())
const renameKey = ref(null)
const renameVal = ref('')
const renameInputRef = ref(null)
const uploadInputRef = ref(null)
const batchMode = ref(false)
const batchSelected = ref(new Set())

const isCustomCat = computed(() => (customData.value.groups || []).some(g => g.id === iconCat.value))
const currentGroupId = computed(() => isCustomCat.value ? iconCat.value : null)
const allCategories = computed(() => [
  ...ICON_CATEGORIES,
  ...(customData.value.groups || []).map(g => ({
    key: g.id, rawKey: g.id, label: g.name,
    icons: aggregateIcons(g), custom: true, parentId: g.parentId || null
  }))
])
const currentCatLabel = computed(() => allCategories.value.find(c => c.key === iconCat.value)?.label || '全部')

const pickerIcons = computed(() => {
  const cat = allCategories.value.find(c => c.key === iconCat.value) || allCategories.value[0]
  const list = cat?.icons || []
  const q = iconQ.value.trim().toLowerCase()
  if (!q) return list
  return list.filter(ic => {
    if (ic.toLowerCase().includes(q)) return true
    if (ic.startsWith('custom:')) {
      const name = customData.value.iconsById?.[ic.slice(7)]?.name || ''
      return name.toLowerCase().includes(q)
    }
    if (ic.startsWith('antd:')) {
      const zh = antdZhName(ic) || ''
      if (zh && zh.toLowerCase().includes(q)) return true
    }
    const kws = ICON_KEYWORDS[ic]
    return !!(kws && kws.some(k => String(k).toLowerCase().includes(q)))
  })
})

function shortIconName(ic) {
  const s = String(ic || '')
  if (s.startsWith('custom:')) return customData.value.iconsById?.[s.slice(7)]?.name || s.slice(7)
  if (s.startsWith('antd:')) {
    const en = s.replace(/^antd:/, ''), zh = ANTD_ZH[en]
    return zh ? `${en} · ${zh}` : en
  }
  return s
}

/* ===== preset grid ===== */
function suggestIcons(name) {
  const s = String(name || '').toLowerCase()
  if (!s) return []
  const out = []
  for (const [icon, kws] of Object.entries(ICON_KEYWORDS)) {
    if (kws.some(k => s.includes(k.toLowerCase()))) out.push(icon)
  }
  return out
}
const presets = computed(() => {
  const customAll = (customData.value.icons || []).map(ic => 'custom:' + ic.id)
  const current = props.modelValue ? [props.modelValue] : []
  const sugg = suggestIcons(props.suggestName)
  const seen = new Set(), out = []
  for (const ic of [...current, ...customAll, ...sugg, ...iconChoices]) {
    if (!seen.has(ic)) { seen.add(ic); out.push(ic) }
    if (out.length >= props.presetCount) break
  }
  return out
})

function pickOne(ic) {
  emit('update:modelValue', ic)
  emit('change', ic)
}

/* ===== open/close ===== */
async function open() {
  open_.value = true
  iconStaged.value = props.modelValue || ''
  iconQ.value = ''
  customExpanded.value = true
  if (!customLoaded.value) await loadLib()
  const firstTop = customTopGroups.value[0]
  iconCat.value = firstTop ? firstTop.id : 'all'
}
function cancel() { open_.value = false; batchMode.value = false; batchSelected.value = new Set() }
function confirmPick() {
  if (!iconStaged.value) return
  emit('update:modelValue', iconStaged.value)
  emit('change', iconStaged.value)
  cancel()
}

/* ===== group ops ===== */
function toggleParent(id) {
  const s = new Set(expandedParents.value); s.has(id) ? s.delete(id) : s.add(id); expandedParents.value = s
}
async function createGroup(parentId = null) {
  const name = await BL.prompt({
    title: parentId ? '新建子编组' : '新建编组',
    label: '编组名称',
    placeholder: parentId ? '例如:水土保持' : '例如:水利行业',
    defaultValue: '',
    validate: v => !v || !v.trim() ? '名称不能为空' : true
  })
  if (name == null) return
  try {
    const g = await iconLibApi.createGroup(parentId || null, name.trim())
    customGroupUpsert(g)
    if (parentId) { const s = new Set(expandedParents.value); s.add(parentId); expandedParents.value = s }
    else { const s = new Set(expandedParents.value); s.add(g.id); expandedParents.value = s }
    iconCat.value = g.id
    BL.success('已创建')
  } catch (e) { BL.error(e?.msg || '创建失败') }
}
function startRename(g) {
  renameKey.value = g.id
  renameVal.value = g.name
  nextTick(() => { renameInputRef.value?.focus?.(); renameInputRef.value?.select?.() })
}
function cancelRename() { renameKey.value = null; renameVal.value = '' }
async function commitRename() {
  if (!renameKey.value) return
  const nv = renameVal.value.trim(), id = renameKey.value
  cancelRename()
  if (!nv) return
  try { customGroupUpsert(await iconLibApi.renameGroup(id, nv)) }
  catch (e) { BL.error(e?.msg || '重命名失败') }
}
async function removeGroup(g) {
  const kids = childrenOf(g.id)
  const cnt = (iconsByGroup.value[g.id] || []).length + kids.reduce((s, c) => s + ((iconsByGroup.value[c.id] || []).length), 0)
  const ok = await BL.confirm({ title: '删除编组', danger: true, okText: '删除',
    content: `将删除编组「${g.name}」${kids.length ? `及 ${kids.length} 个子分组` : ''}, 共 ${cnt} 个图标。` })
  if (!ok) return
  try {
    await iconLibApi.deleteGroup(g.id)
    customGroupRemove(g.id)
    if (iconCat.value === g.id) iconCat.value = 'all'
    BL.success('已删除')
  } catch (e) { BL.error(e?.msg || '删除失败') }
}
async function removeIcon(ic) {
  const id = String(ic || '').replace(/^custom:/, '')
  if (!id) return
  try {
    await iconLibApi.deleteIcon(id)
    customIconRemoveByIds([id])
    if (iconStaged.value === ic) iconStaged.value = ''
  } catch (e) { BL.error(e?.msg || '删除失败') }
}

/* ===== upload ===== */
function readFileText(f) {
  return new Promise((res, rej) => { const fr = new FileReader(); fr.onload = () => res(String(fr.result || '')); fr.onerror = rej; fr.readAsText(f) })
}
function parseSvgText(text) {
  if (typeof text !== 'string') return null
  try {
    const doc = new DOMParser().parseFromString(text, 'image/svg+xml')
    if (doc.querySelector('parsererror')) return null
    const svg = doc.documentElement
    if (!svg || svg.tagName.toLowerCase() !== 'svg') return null
    let viewBox = svg.getAttribute('viewBox')
    if (!viewBox) {
      const w = svg.getAttribute('width'), h = svg.getAttribute('height')
      viewBox = (w && h) ? `0 0 ${parseFloat(w)} ${parseFloat(h)}` : '0 0 1024 1024'
    }
    const xs = new XMLSerializer()
    let inner = Array.from(svg.childNodes).map(n => {
      if (n.nodeType === 3) return n.nodeValue
      if (n.nodeType !== 1) return ''
      return xs.serializeToString(n)
    }).join('').trim()
    if (!inner) return null
    inner = inner
      .replace(/\sfill="(?!none)([^"]*)"/gi, ' fill="currentColor"')
      .replace(/\sstroke="(?!none)([^"]*)"/gi, ' stroke="currentColor"')
      .replace(/fill:\s*#[0-9a-f]{3,8}/gi, 'fill: currentColor')
      .replace(/stroke:\s*#[0-9a-f]{3,8}/gi, 'stroke: currentColor')
    return { viewBox, content: inner }
  } catch { return null }
}
function triggerUpload() {
  if (!isCustomCat.value) { BL.warning('请先选择一个自定义编组'); return }
  uploadInputRef.value?.click()
}
async function onSvgFiles(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = ''
  if (!files.length) return
  if (!isCustomCat.value) { BL.warning('请先选择自定义编组'); return }
  const gid = currentGroupId.value
  if (!gid) { BL.error('编组不存在'); return }
  let ok = 0, fail = 0
  for (const f of files) {
    if (!/svg/i.test(f.type) && !/\.svg$/i.test(f.name)) { fail++; continue }
    try {
      const text = await readFileText(f)
      const parsed = parseSvgText(text)
      if (!parsed) { fail++; continue }
      const name = f.name.replace(/\.svg$/i, '')
      const ic = await iconLibApi.addIcon(gid, { name, viewBox: parsed.viewBox, content: parsed.content })
      customIconUpsert(ic)
      ok++
    } catch { fail++ }
  }
  if (ok) BL.success(`已导入 ${ok} 个图标${fail ? `,${fail} 个失败` : ''}`)
  else BL.error('未能解析任何 SVG 文件')
}

/* ===== batch ===== */
function toggleBatch() { batchMode.value = !batchMode.value; if (!batchMode.value) batchSelected.value = new Set() }
function toggleBatchSelect(ic) { const s = new Set(batchSelected.value); s.has(ic) ? s.delete(ic) : s.add(ic); batchSelected.value = s }
function batchSelectAll() { batchSelected.value = new Set(pickerIcons.value) }
function batchClear() { batchSelected.value = new Set() }
async function batchDelete() {
  if (!batchSelected.value.size) return
  const ok = await BL.confirm({ title: '批量删除', danger: true, okText: '删除',
    content: `将从「业务图库」永久删除 ${batchSelected.value.size} 个图标,且无法恢复?` })
  if (!ok) return
  const ids = [...batchSelected.value].map(ic => String(ic).replace(/^custom:/, ''))
  try {
    await iconLibApi.deleteIconBatch(ids)
    customIconRemoveByIds(ids)
    if (iconStaged.value && ids.includes(String(iconStaged.value).replace(/^custom:/, ''))) iconStaged.value = ''
    BL.success(`已删除 ${ids.length} 个图标`)
    batchSelected.value = new Set()
  } catch (e) { BL.error(e?.msg || '批量删除失败') }
}
watch(iconCat, () => { batchMode.value = false; batchSelected.value = new Set() })

onMounted(() => { unsub = onCustomIconsChange(d => { customData.value = JSON.parse(JSON.stringify(d)) }) })
onUnmounted(() => { unsub && unsub() })

defineExpose({ open })
</script>

<style scoped>
.ipf { display: flex; flex-direction: column; gap: 6px; }
.ipf-head { display: flex; align-items: center; justify-content: space-between; }
.ipf-label { font-size: var(--bl-fs-12); color: var(--bl-text-3); }
.ipf-more-row {
  width: 100%;
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  height: 30px;
  border: 1px dashed var(--bl-border-strong);
  border-radius: var(--bl-radius-2);
  background: var(--bl-bg-1);
  color: var(--bl-primary);
  cursor: pointer; font-size: var(--bl-fs-12);
  transition: background-color .15s, border-color .15s;
}
.ipf-more-row:hover { background: var(--bl-primary-soft); border-color: var(--bl-primary); }
.icon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(40px, 1fr));
  gap: 6px;
}
.icon-cell {
  width: 100%; aspect-ratio: 1; max-height: 48px;
  display: inline-flex; align-items: center; justify-content: center;
  border: 1px solid var(--bl-border); border-radius: var(--bl-radius-2);
  cursor: pointer; transition: border-color .15s, background-color .15s;
  color: var(--bl-text-2);
}
.icon-cell:hover { border-color: var(--bl-primary); color: var(--bl-primary); }
.icon-cell.is-active { border-color: var(--bl-primary); background: var(--bl-primary-soft); color: var(--bl-primary); }
</style>

<style>
/* —— 模态相关样式(全局,因为 Teleport 到 body) —— */
.icon-picker-modal {
  width: 1280px; max-width: 95vw;
  display: flex; flex-direction: column;
  max-height: 94vh;
}
.icon-picker-modal .ip-hd {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 16px 14px 20px;
  border-bottom: 1px solid var(--bl-border); flex-shrink: 0;
}
.icon-picker-modal .ip-body {
  padding: 0 !important;
  display: grid; grid-template-columns: 200px 1fr;
  height: 580px; min-height: 0; overflow: hidden;
}
.icon-picker-modal .ip-side {
  background: var(--bl-bg-1);
  padding: 10px 8px; gap: 10px;
  display: flex; flex-direction: column; overflow: auto;
}
.icon-picker-modal .ip-side-section {
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border); border-radius: var(--bl-radius-2);
  overflow: hidden; flex-shrink: 0;
}
.icon-picker-modal .ip-side-head {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 10px; cursor: pointer; user-select: none;
  color: var(--bl-text-2); font-size: var(--bl-fs-12);
  font-weight: 600; letter-spacing: .2px;
  background: var(--bl-bg-2); border-bottom: 1px solid var(--bl-divider);
  white-space: nowrap;
}
.icon-picker-modal .ip-side-head:hover { color: var(--bl-text-1); background: var(--bl-bg-3); }
.icon-picker-modal .ip-side-head-chev {
  display: inline-flex; align-items: center; justify-content: center;
  width: 14px; height: 14px; transition: transform .15s;
  color: var(--bl-text-3);
}
.icon-picker-modal .ip-side-head-chev.is-open { transform: rotate(90deg); }
.icon-picker-modal .ip-side-head-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.icon-picker-modal .ip-side-head-count {
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-1); border: 1px solid var(--bl-divider);
  border-radius: 10px; padding: 0 6px; min-width: 22px; text-align: center; font-weight: 500;
}
.icon-picker-modal .ip-side-list { padding: 4px 0; }
.icon-picker-modal .ip-side-empty {
  padding: 10px 16px; font-size: var(--bl-fs-12); color: var(--bl-text-4); text-align: center;
}
.icon-picker-modal .ip-cat {
  padding: 8px 16px; cursor: pointer;
  font-size: var(--bl-fs-13); color: var(--bl-text-2);
  display: flex; align-items: center; justify-content: space-between;
  border-left: 3px solid transparent; white-space: nowrap;
}
.icon-picker-modal .ip-cat > span:first-child { overflow: hidden; text-overflow: ellipsis; }
.icon-picker-modal .ip-cat:hover { background: var(--bl-bg-1); color: var(--bl-text-1); }
.icon-picker-modal .ip-cat.is-active {
  background: var(--bl-bg-1); color: var(--bl-primary);
  font-weight: 500; border-left-color: var(--bl-primary);
}
.icon-picker-modal .ip-cat-count {
  font-size: var(--bl-fs-12); color: var(--bl-text-4);
  background: var(--bl-bg-3); border-radius: 10px;
  padding: 0 6px; min-width: 22px; text-align: center;
}
.icon-picker-modal .ip-cat.is-active .ip-cat-count { background: var(--bl-primary-soft); color: var(--bl-primary); }
.icon-picker-modal .ip-cat-custom { position: relative; gap: 4px; min-height: 32px; }
.icon-picker-modal .ip-cat-label { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.icon-picker-modal .ip-cat-actions {
  display: none; align-items: center; gap: 0;
}
.icon-picker-modal .ip-cat-actions .bl-btn { height: 20px; width: 20px; padding: 0; }
.icon-picker-modal .ip-cat-custom:hover .ip-cat-actions { display: inline-flex; }
.icon-picker-modal .ip-cat-custom:hover .ip-cat-count { display: none; }
.icon-picker-modal .ip-cat-rename {
  height: 22px; padding: 0 4px; font-size: var(--bl-fs-12); flex: 1; min-width: 0;
}
.icon-picker-modal .ip-cat-parent { padding-left: 6px; font-weight: 500; }
.icon-picker-modal .ip-cat-parent-chev {
  display: inline-flex; width: 14px; height: 14px;
  align-items: center; justify-content: center;
  color: var(--bl-text-3); transition: transform .15s; cursor: pointer;
}
.icon-picker-modal .ip-cat-parent-chev.is-open { transform: rotate(90deg); }
.icon-picker-modal .ip-cat-child { padding-left: 28px; font-size: 12.5px; }
.icon-picker-modal .ip-cat-child .ip-cat-label { color: var(--bl-text-2); }
.icon-picker-modal .ip-main {
  display: flex; flex-direction: column;
  padding: 14px 16px; min-width: 0; min-height: 0; overflow: hidden;
}
.icon-picker-modal .ip-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px; gap: 12px;
}
.icon-picker-modal .ip-toolbar-right { display: inline-flex; align-items: center; gap: 8px; }
.icon-picker-modal .ip-crumb {
  display: flex; align-items: center; gap: 6px;
  font-size: var(--bl-fs-13); color: var(--bl-text-1);
}
.icon-picker-modal .ip-search { position: relative; width: 220px; }
.icon-picker-modal .ip-search-ic {
  position: absolute; left: 8px; top: 50%; transform: translateY(-50%);
  color: var(--bl-text-3); display: inline-flex;
}
.icon-picker-modal .ip-search-input { padding-left: 26px !important; }
.icon-picker-modal .ip-grid {
  flex: 1; min-height: 0;
  display: grid; grid-template-columns: repeat(15, 1fr); gap: 8px;
  overflow-y: auto; overflow-x: hidden;
  align-content: start; padding-right: 4px;
}
.icon-picker-modal .ip-cell { aspect-ratio: 1; padding: 0; position: relative; }
.icon-picker-modal .ip-cell-x {
  position: absolute; top: 2px; right: 2px;
  width: 14px; height: 14px; display: none;
  align-items: center; justify-content: center;
  border: 0; border-radius: 50%; background: var(--bl-bg-3);
  color: var(--bl-text-2); cursor: pointer; padding: 0;
}
.icon-picker-modal .ip-cell:hover .ip-cell-x { display: inline-flex; }
.icon-picker-modal .ip-cell-x:hover { background: var(--bl-danger, #F53F3F); color: #fff; }
.icon-picker-modal .ip-batch-info { font-size: var(--bl-fs-12); color: var(--bl-text-2); margin-right: 4px; }
.icon-picker-modal .ip-batch-info b { color: var(--bl-primary); }
.icon-picker-modal .ip-cell.is-batch-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.icon-picker-modal .ip-cell-check {
  position: absolute; top: 3px; right: 3px;
  width: 14px; height: 14px; display: inline-flex;
  align-items: center; justify-content: center;
  border-radius: 3px; background: var(--bl-bg-1);
  border: 1px solid var(--bl-border); box-sizing: border-box;
}
.icon-picker-modal .ip-cell-check.is-on { background: var(--bl-primary); border-color: var(--bl-primary); }
</style>
