<template>
  <div class="os-wrap">
    <!-- 起始对象集 -->
    <div class="os-lbl">起始对象集</div>
    <button class="os-pick" @click="openPicker()">
      <template v-if="curClass">
        <span class="os-pick-ic" v-html="BL.icon('box', 11, '#fff')"></span>
        <span>{{ curClass.cn }}</span>
        <span class="bl-mono bl-muted" style="font-size:11px">{{ curClass.api_name }}</span>
      </template>
      <span v-else class="bl-muted">选择对象集…</span>
      <span v-if="objset.set_var" class="bl-tag bl-tag-primary" style="margin-left:2px">变量 {{ objset.set_var }}</span>
      <span style="flex:1"></span>
      <span class="bl-muted" v-html="BL.icon('chevronDown', 12)"></span>
    </button>

    <!-- 按属性过滤 -->
    <div v-for="(f, fi) in objset.filters" :key="fi" class="os-cond os-cond-row">
      <BlSelect v-model="f.property_code" :options="propOptions" size="sm" clearable placeholder="选择属性" style="width:180px;flex-shrink:0" />
      <BlSelect v-model="f.operator" :options="OS_OPERATORS" size="sm" style="width:100px;flex-shrink:0" />
      <input v-if="needValue(f.operator)" class="bl-input bl-input-sm" style="flex:1;min-width:0" v-model="f.value" placeholder="比较值" />
      <span v-else class="bl-muted os-cond-na">该运算符无需比较值</span>
      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" style="flex-shrink:0" title="移除过滤条件" @click="objset.filters.splice(fi,1)" v-html="BL.icon('x', 11)"></button>
    </div>
    <button class="fe-add-row" @click="addFilter"><span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">按属性过滤</span></button>

    <!-- 关联搜索: 链式多跳, 第 N 跳从第 N-1 跳的对端对象继续往外关联 -->
    <div v-for="(h, hi) in hops" :key="hi" class="os-cond">
      <div class="os-cond-hd">
        <span class="os-cond-lbl">{{ hi === 0 ? '关联搜索至' : `第 ${hi + 1} 跳关联至` }}</span>
        <BlSelect :model-value="h.link_type_code" @update:model-value="v => setHopLink(hi, v)"
                  :options="hopMeta[hi].options" size="sm" clearable :disabled="!hopMeta[hi].srcClassId"
                  :placeholder="hopMeta[hi].placeholder" style="flex:1;max-width:260px" />
        <button class="bl-btn bl-btn-sm os-graph-btn" title="在关联图谱上选择" @click="openGraph(hi)">
          <span v-html="BL.icon('network', 12)"></span><span style="margin-left:4px">图谱选择</span>
        </button>
        <span style="flex:1"></span>
        <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="移除本跳 (及其后续跳)" @click="removeHop(hi)" v-html="BL.icon('x', 11)"></button>
      </div>
      <div class="os-path">{{ hopMeta[hi].path }}</div>
      <!-- 嵌套过滤: 属性取自本跳的对端对象 -->
      <template v-if="h.link_type_code">
        <div v-for="(f, fi) in h.filters" :key="fi" class="os-cond-row os-sub-row">
          <BlSelect v-model="f.property_code" :options="hopMeta[hi].propOptions" size="sm" clearable
                    :placeholder="`${hopMeta[hi].peerName} 的属性`" style="width:180px;flex-shrink:0" />
          <BlSelect v-model="f.operator" :options="OS_OPERATORS" size="sm" style="width:100px;flex-shrink:0" />
          <input v-if="needValue(f.operator)" class="bl-input bl-input-sm" style="flex:1;min-width:0" v-model="f.value" placeholder="比较值" />
          <span v-else class="bl-muted os-cond-na">该运算符无需比较值</span>
          <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" style="flex-shrink:0" title="移除过滤条件" @click="h.filters.splice(fi,1)" v-html="BL.icon('x', 11)"></button>
        </div>
        <button class="fe-add-row is-sub" @click="addLinkFilter(hi)"><span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">按 {{ hopMeta[hi].peerName }} 的属性过滤</span></button>
      </template>
    </div>
    <button class="fe-add-row" @click="addHop"><span v-html="BL.icon('link', 12)"></span><span style="margin-left:4px">{{ hops.length ? '继续关联搜索' : '关联搜索' }}</span></button>

    <!-- 返回属性 (仅字符串多选: 作为下拉展示值; 对象引用参数的展示字段在「显示」页配置) -->
    <template v-if="variant === 'multi'">
      <div class="os-lbl os-lbl-row" style="margin-top:6px">
        <span>返回属性 <span class="bl-muted" style="font-weight:400">(选中的属性拼成下拉候选卡片)</span></span>
        <span style="flex:1"></span>
        <button class="bl-btn bl-btn-sm" :disabled="!objset.class_id"
                :title="objset.class_id ? '选择属性与呈现模版' : '请先选择起始对象集'" @click="cardOpen = true">选择</button>
      </div>
      <!-- 回显区: 确定后直接是所选模版的卡片形态 -->
      <div class="osc-echo" :class="!labelPropCodes.length && 'is-empty'">
        <template v-if="labelPropCodes.length">
          <ReturnCard :items="cardItems" :scheme="cardScheme" />
          <ReturnCard :items="cardItems" :scheme="cardScheme" class="osc-ghost" />
          <div class="osc-echo-meta">
            <span class="osc-echo-tag">{{ schemeLabel }}</span>
            <span class="bl-muted">{{ labelPropCodes.length }} 个属性 · {{ cardItems.map(i => i.name).join('、') }}</span>
            <span style="flex:1"></span>
            <a class="os-reset" style="margin:0" @click="clearCard">清空</a>
          </div>
        </template>
        <div v-else class="bl-muted osc-echo-empty">
          {{ objset.class_id ? '未配置返回属性 — 点右上角「选择」挑属性与呈现模版' : '请先选择起始对象集' }}
        </div>
      </div>
    </template>
    <a v-else class="os-reset" @click="reset">重置筛选</a>

    <!-- 对象集选择弹窗 (只选起始对象: 顶部搜索 + 相关对象 / 全部对象 / 对象集变量 三个来源) -->
    <Teleport to="body">
      <div v-if="pickerOpen" class="rlm-mask" @click.self="pickerOpen = false">
        <div class="rlm-modal osp-modal">
          <div class="rlm-hd"><span v-html="BL.icon('box', 14)"></span><span style="margin-left:6px">选择起始对象集</span><span style="flex:1"></span><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="pickerOpen = false" v-html="BL.icon('x', 14)"></button></div>
          <div class="osp-search"><span class="bl-muted" v-html="BL.icon('search', 13)"></span><input class="bl-input bl-input-sm" v-model="kw" placeholder="搜索对象类型 / 对象集变量…" /></div>
          <div class="osp-body">
            <!-- 起始对象 (对象类 / 表单内对象引用参数 两个来源用页签切) -->
            <div class="osp-col">
              <div class="osp-tabs">
                <button v-for="t in SRC_TABS" :key="t.k" :class="['osp-tab', srcTab === t.k && 'is-on']" @click="srcTab = t.k">
                  {{ t.label }}<span class="osp-tab-n">{{ tabCount(t.k) }}</span>
                </button>
              </div>
              <div class="osp-list">
                <!-- 相关对象: 主对象自身 + 与它一跳关联的对象, 绝大多数场景选的就是这几个 -->
                <template v-if="srcTab === 'rel'">
                  <button v-for="c in pickRelated" :key="c.id" :class="['osp-item', objset.class_id === c.id && 'is-on']" @click="chooseClass(c)">
                    <span class="osp-item-ic" v-html="BL.icon('box', 11, '#fff')"></span>
                    <span class="bl-truncate" style="flex:1">{{ c.cn }}</span>
                    <span class="bl-muted osp-via">{{ c.via }}</span>
                    <span class="bl-mono bl-muted" style="font-size:11px">{{ c.api_name }}</span>
                  </button>
                  <div v-if="!pickRelated.length" class="bl-muted osp-tip">{{ kw ? '无匹配对象' : '主对象暂无关联对象, 请到「全部对象」选择' }}</div>
                </template>
                <template v-else-if="srcTab === 'class'">
                  <button v-for="c in pickClasses" :key="c.id" :class="['osp-item', objset.class_id === c.id && 'is-on']" @click="chooseClass(c)">
                    <span class="osp-item-ic" v-html="BL.icon('box', 11, '#fff')"></span>
                    <span class="bl-truncate" style="flex:1">{{ c.cn }}</span>
                    <span class="bl-mono bl-muted" style="font-size:11px">{{ c.api_name }}</span>
                  </button>
                  <div v-if="!pickClasses.length" class="bl-muted osp-tip">无匹配对象类型</div>
                </template>
                <template v-else>
                  <button v-for="v in pickVars" :key="v.value" :class="['osp-item', objset.set_var === v.value && 'is-on']" @click="chooseVar(v)">
                    <span class="osp-item-ic is-var" v-html="BL.icon('code', 11, '#fff')"></span>
                    <span class="bl-truncate" style="flex:1">{{ v.label }}</span>
                  </button>
                  <div v-if="!pickVars.length" class="bl-muted osp-tip">暂无对象引用参数</div>
                </template>
              </div>
            </div>
          </div>
          <div class="rlm-ft">
            <button class="bl-btn bl-btn-sm" style="margin-right:auto" @click="reset">清空选择</button>
            <button class="bl-btn bl-btn-primary bl-btn-sm" @click="pickerOpen = false">完成</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 返回属性: 左选属性 + 右选呈现模版 -->
    <ReturnCardPicker v-model:open="cardOpen" :prop-options="propOptions" :prop-codes="labelPropCodes"
                      :card-scheme="cardScheme" @confirm="applyCard" />

    <!-- 关联图谱选择: 以本跳起点对象为中心的一跳关联图, 点连线即选中链接类型 -->
    <LinkGraphPicker v-model:open="graphOpen" :model-value="hops[graphHop]?.link_type_code || ''"
                     @update:model-value="v => setHopLink(graphHop, v)"
                     :start-class-id="hopMeta[graphHop]?.srcClassId || ''"
                     :class-options="classOptions" :link-types="linkTypes" />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import BlSelect from '@/components/BlSelect.vue'
import LinkGraphPicker from './LinkGraphPicker.vue'
import ReturnCard from './ReturnCard.vue'
import ReturnCardPicker from './ReturnCardPicker.vue'
import { pickOps, needValue } from './conditionModel.js'

const props = defineProps({
  objset: { type: Object, required: true },
  classOptions: { type: Array, default: () => [] },   // [{ id, cn, api_name }]
  varOptions: { type: Array, default: () => [] },     // [{ value, label }] 表单内对象引用参数
  propOptions: { type: Array, default: () => [] },
  linkTypes: { type: Array, default: () => [] },      // 原始链接类型 (要两端对象/基数, 用于逐跳解析)
  propsByClass: { type: Object, default: () => ({}) },// classId -> [{ code, name, status }] 由父组件按需加载
  fallbackClassId: { type: String, default: '' },
  variant: { type: String, default: 'multi' },        // 'multi' 字符串多选 | 'object' 对象引用参数
})
const emit = defineEmits(['load-props'])

/* 对象集过滤沿用的运算符子集与顺序; BlSelect 要 {value,label} 形态 */
const OS_OPERATORS = pickOps(['eq', 'ne', 'contains', 'startsWith', 'empty', 'notEmpty']).map(o => ({ value: o.key, label: o.label }))

const pickerOpen = ref(false)
const kw = ref('')
const SRC_TABS = [{ k: 'rel', label: '相关对象' }, { k: 'class', label: '全部对象' }, { k: 'var', label: '对象集变量' }]
const srcTab = ref('rel')
const curClass = computed(() => props.classOptions.find(c => c.id === props.objset.class_id) || null)
const pickClasses = computed(() => {
  const q = kw.value.trim().toLowerCase()
  return props.classOptions.filter(c => !q || `${c.cn} ${c.api_name}`.toLowerCase().includes(q))
})
const pickVars = computed(() => {
  const q = kw.value.trim().toLowerCase()
  return props.varOptions.filter(v => !q || v.label.toLowerCase().includes(q))
})
/* 相关对象 = 动作主对象 + 与它一跳关联的对象 (起始对象集不限于此, 只是把常用的顶到前面) */
const pickRelated = computed(() => {
  const main = props.fallbackClassId
  if (!main) return []
  const q = kw.value.trim().toLowerCase()
  const seen = new Set()
  const out = []
  const push = (id, via) => {
    const c = props.classOptions.find(x => x.id === id)
    if (!c || seen.has(id)) return
    seen.add(id)
    if (q && !`${c.cn} ${c.api_name}`.toLowerCase().includes(q)) return
    out.push({ ...c, via })
  }
  push(main, '主对象')
  for (const l of props.linkTypes || []) {
    const [lid, rid] = linkEnds(l)
    if (lid !== main && rid !== main) continue
    push(lid === main ? rid : lid, `经「${linkName(l)}」关联`)
  }
  return out
})
function tabCount(k) { return k === 'rel' ? pickRelated.value.length : (k === 'class' ? pickClasses.value.length : pickVars.value.length) }
/* label_prop 存逗号分隔的属性编码, 单个时与旧的单选值完全一致, 老数据不用迁移 */
/* 顺序即卡片槽位: 第 1 个作主标题, 第 2 个作副标题, 其余作标签 */
const labelPropCodes = computed({
  get: () => String(props.objset.label_prop || '').split(',').map(s => s.trim()).filter(Boolean),
  set: v => { props.objset.label_prop = (v || []).join(',') },
})
function propLabel(code) { return props.propOptions.find(o => o.value === code)?.name || code }
const cardScheme = computed(() => props.objset.label_card || 'title_sub')
const cardItems = computed(() => labelPropCodes.value.map(c => ({ code: c, name: propLabel(c) })))
const SCHEME_LABEL = { title_sub: '标题 + 副标题', title_tags: '标题 + 标签', title_sub_tags: '标题 + 副标题 + 标签', inline: '单行拼接' }
const schemeLabel = computed(() => SCHEME_LABEL[cardScheme.value] || SCHEME_LABEL.title_sub)
const cardOpen = ref(false)
function applyCard({ codes, scheme }) {
  labelPropCodes.value = codes
  props.objset.label_card = scheme
}
function clearCard() { labelPropCodes.value = [] }
/* 默认落在「相关对象」页签; 已选的对象不在相关列表里 (或主对象没关联) 就退回全部 */
function openPicker() {
  kw.value = ''
  const cur = props.objset.class_id
  const inRel = pickRelated.value.some(c => c.id === cur)
  srcTab.value = pickRelated.value.length && (!cur || inRel) ? 'rel' : 'class'
  pickerOpen.value = true
}
/* ===== 关联搜索: 链式多跳 (links[i] 的起点 = links[i-1] 的对端对象) ===== */
const hops = computed(() => {
  if (!Array.isArray(props.objset.links)) props.objset.links = []
  return props.objset.links
})
const linkByCode = code => (props.linkTypes || []).find(l => (l.link_type_id || l.linkTypeId || l.id) === code) || null
const linkEnds = l => [l.l_object_type_id || l.lObjectTypeId || '', l.r_object_type_id || l.rObjectTypeId || '']
const linkName = l => l.rdfs_label || l.rdfsLabel || l.link_type_id || l.linkTypeId || l.id
const className = id => props.classOptions.find(c => c.id === id)?.cn || ''
/* 某个对象出发的链接候选 (任一端是它), 标签带对端对象便于区分同名关联 */
function optionsFrom(sid) {
  if (!sid) return []
  const out = []
  for (const l of props.linkTypes || []) {
    const [lid, rid] = linkEnds(l)
    if (lid !== sid && rid !== sid) continue
    const peerId = lid === sid ? rid : lid
    const peer = className(peerId)
    out.push({ value: l.link_type_id || l.linkTypeId || l.id, label: peer ? `${linkName(l)} → ${peer}` : linkName(l) })
  }
  return out
}
function peerOf(code, sid) {
  const l = linkByCode(code)
  if (!l) return ''
  const [lid, rid] = linkEnds(l)
  return lid === sid ? rid : (rid === sid ? lid : (rid || lid))
}
/* 逐跳解析: 起点 / 候选链接 / 对端对象 / 对端属性; 上一跳没选完则本跳起点为空 */
const hopMeta = computed(() => {
  const out = []
  let src = props.objset.class_id || ''
  hops.value.forEach(h => {
    const peerId = src ? peerOf(h.link_type_code, src) : ''
    const peerName = className(peerId) || '关联对象'
    const arr = props.propsByClass[peerId] || []
    out.push({
      srcClassId: src,
      srcName: className(src) || '起始对象',
      options: optionsFrom(src),
      peerClassId: peerId,
      peerName,
      propOptions: arr.map(p => ({ value: p.code, label: `${p.name} (${p.code})`,
        name: p.name, code: p.code, dataType: p.dataType, status: p.status, required: p.required, isPrimary: p.isPrimary })),
      placeholder: !src ? '请先完成上一跳' : (optionsFrom(src).length ? '选择链接类型' : '该对象暂无可用关联'),
      path: h.link_type_code
        ? `从 ${className(src) || '起始对象'} 关联搜索至 ${linkName(linkByCode(h.link_type_code) || {}) || h.link_type_code} 的 ${peerName}`
        : '选择链接类型后, 可通过关联对象的属性搜索当前对象集',
    })
    src = peerId
  })
  return out
})
/* 对端对象的属性由父组件按需加载 (只盯对端对象变化, 不跟着过滤条件抖动) */
const hopPeerIds = computed(() => hopMeta.value.map(m => m.peerClassId).filter(Boolean).join(','))
watch(hopPeerIds, s => s.split(',').filter(Boolean).forEach(id => emit('load-props', id)), { immediate: true })
/* 换了起始对象集, 整条关联链的起点都变了, 不再成立 */
watch(() => props.objset.class_id, () => {
  const first = hops.value[0]
  if (first && first.link_type_code && !optionsFrom(props.objset.class_id).some(o => o.value === first.link_type_code)) {
    props.objset.links.splice(0)
  }
})
/* 改本跳链接 → 本跳过滤条件失效, 后续跳的起点也变了, 一并截断 */
function setHopLink(hi, v) {
  const h = hops.value[hi]
  if (!h || h.link_type_code === v) return
  h.link_type_code = v || ''
  h.filters = []
  if (hops.value.length > hi + 1) props.objset.links.splice(hi + 1)
}
function addHop() {
  if (!props.objset.class_id) return BL.warning('请先选择起始对象集')
  const last = hops.value[hops.value.length - 1]
  if (last && !last.link_type_code) return BL.warning('请先选择上一跳的链接类型')
  if (last && !hopMeta.value[hops.value.length - 1]?.peerClassId) return BL.warning('上一跳的对端对象无法解析, 不能继续关联')
  props.objset.links.push({ link_type_code: '', filters: [] })
}
function removeHop(hi) {
  const dropped = hops.value.length - hi
  props.objset.links.splice(hi)
  if (dropped > 1) BL.info(`已移除该跳及其后续 ${dropped - 1} 跳`)
}
const graphOpen = ref(false)
const graphHop = ref(0)
function openGraph(hi) {
  if (!hopMeta.value[hi]?.srcClassId) return BL.warning('请先完成上一跳的关联')
  graphHop.value = hi
  graphOpen.value = true
}

function addFilter() {
  if (!props.objset.class_id) return BL.warning('请先选择起始对象集')
  props.objset.filters.push({ property_code:'', operator:'eq', value:'' })
}
function addLinkFilter(hi) {
  const h = hops.value[hi]
  if (!h?.link_type_code) return BL.warning('请先选择链接类型')
  if (!Array.isArray(h.filters)) h.filters = []
  h.filters.push({ property_code:'', operator:'eq', value:'' })
}
function chooseClass(c) {
  const os = props.objset
  if (os.class_id !== c.id) { os.filters = []; os.label_prop = ''; os.links = [] }
  os.class_id = c.id; os.set_var = ''
  pickerOpen.value = false
}
function chooseVar(v) {
  const os = props.objset
  os.set_var = os.set_var === v.value ? '' : v.value
  if (os.set_var && !os.class_id && props.fallbackClassId) os.class_id = props.fallbackClassId
  if (os.set_var) pickerOpen.value = false
}
function reset() {
  const os = props.objset
  os.class_id = ''; os.set_var = ''; os.filters = []; os.links = []; os.label_prop = ''
}
</script>

<style scoped>
.os-wrap { display: flex; flex-direction: column; }
.os-lbl { font-size: 12.5px; font-weight: 600; color: var(--bl-text-2); margin-top: 10px; margin-bottom: 6px; }
.os-pick { width: 100%; display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; font-size: 13px; color: var(--bl-text-1); cursor: pointer; }
.os-pick:hover { border-color: var(--bl-primary); }
.os-pick-ic { width: 20px; height: 20px; border-radius: 5px; background: var(--bl-primary); flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.os-cond { border: 1px solid var(--bl-divider); border-radius: 8px; padding: 10px 12px; display: flex; flex-direction: column; gap: 8px; margin-top: 10px; }
.os-cond-hd { display: flex; align-items: center; gap: 8px; }
/* 过滤条件: 属性 / 运算符 / 比较值 / 删除 单行排布 */
.os-cond-row { flex-direction: row; align-items: center; }
.os-cond-na { flex: 1; min-width: 0; font-size: 12px; }
.os-cond-lbl { font-size: 12.5px; color: var(--bl-text-2); flex-shrink: 0; }
.os-graph-btn { flex-shrink: 0; display: inline-flex; align-items: center; }
.os-path { font-size: 12px; color: var(--bl-text-3); line-height: 1.5; }
/* 关联搜索内的嵌套过滤: 左侧竖线 + 缩进, 表明从属于上面的链接 */
/* 横排靠自身声明 — 这里没套 .os-cond, 拿不到它的 display:flex */
.os-sub-row { display: flex; align-items: center; gap: 8px; margin-top: 8px;
  margin-left: 10px; padding-left: 10px; border-left: 2px solid var(--bl-divider); }
.fe-add-row.is-sub { margin-left: 10px; width: calc(100% - 10px); margin-top: 8px; padding: 7px; font-size: 12px; }
/* 返回属性回显区: 确定后直接呈现所选模版的卡片形态 (第二张淡化, 示意这是列表) */
.osc-echo { display: flex; flex-direction: column; gap: 6px; padding: 10px;
  border: 1px dashed var(--bl-border-strong); border-radius: 8px; }
.osc-echo.is-empty { padding: 14px 10px; }
.osc-ghost { opacity: .45; }
.osc-echo-empty { font-size: 12.5px; text-align: center; }
.osc-echo-meta { display: flex; align-items: center; gap: 8px; font-size: 11.5px; padding-top: 2px; min-width: 0; }
.osc-echo-meta > .bl-muted { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.osc-echo-tag { flex-shrink: 0; padding: 1px 6px; border-radius: 4px; background: var(--bl-primary-soft); color: var(--bl-primary); }
.os-lbl-row { display: flex; align-items: center; gap: 8px; }
.os-ret { display: flex; align-items: center; gap: 8px; }
.os-reset { align-self: flex-start; margin-top: 10px; font-size: 12.5px; color: var(--bl-primary); cursor: pointer; }
.os-reset:hover { text-decoration: underline; }
/* 满宽虚线「添加一行」按钮 (与规则页 .fe-add-row 同形) */
.fe-add-row { width: 100%; display: flex; align-items: center; justify-content: center; padding: 9px; margin-top: 10px; background: transparent; border: 1px dashed var(--bl-border-strong); border-radius: 8px; color: var(--bl-text-2); font-size: 12.5px; cursor: pointer; transition: border-color .12s, color .12s, background .12s; }
.fe-add-row:hover { border-color: var(--bl-primary); color: var(--bl-primary); background: var(--bl-primary-soft); }
/* 对象集选择弹窗 */
.rlm-mask { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: center; justify-content: center; z-index: 1300; }
.rlm-modal { width: 640px; max-width: 92vw; max-height: 82vh; background: var(--bl-bg-1); border-radius: 12px; box-shadow: 0 16px 48px rgba(0,0,0,.3); display: flex; flex-direction: column; overflow: hidden; }
.rlm-hd { display: flex; align-items: center; padding: 14px 16px; font-size: 14px; font-weight: 600; border-bottom: 1px solid var(--bl-divider); }
.rlm-ft { padding: 12px 16px; border-top: 1px solid var(--bl-divider); display: flex; justify-content: flex-end; }
.osp-modal { width: 520px; }
.osp-search { display: flex; align-items: center; gap: 8px; padding: 12px 16px; border-bottom: 1px solid var(--bl-divider); }
.osp-search .bl-input { flex: 1; }
.osp-body { display: flex; flex-direction: column; overflow: hidden; }
.osp-col { display: flex; flex-direction: column; min-width: 0; }
/* 左栏来源页签: 对象类 / 对象集变量 */
.osp-tabs { display: flex; gap: 4px; padding: 8px 10px 4px; }
.osp-tab { display: inline-flex; align-items: center; gap: 5px; padding: 4px 10px; border: 0; border-radius: 6px; background: transparent;
  font-size: 12px; color: var(--bl-text-2); cursor: pointer; }
.osp-tab:hover { background: var(--bl-bg-hover); }
.osp-tab.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 600; }
.osp-tab-n { font-size: 11px; color: var(--bl-text-3); }
.osp-tab.is-on .osp-tab-n { color: var(--bl-primary); }
.osp-tip { padding: 12px; font-size: 12px; text-align: center; }
.osp-via { font-size: 11px; flex-shrink: 0; }
/* 定高: 搜索过滤后条目变少也不改变弹窗高度 */
.osp-list { height: min(340px, 46vh); overflow-y: auto; padding: 0 8px 10px; }
.osp-item { width: 100%; display: flex; align-items: center; gap: 8px; padding: 8px 10px; background: transparent; border: 0; border-radius: 6px; font-size: 13px; color: var(--bl-text-1); cursor: pointer; text-align: left; }
.osp-item:hover { background: var(--bl-bg-hover); }
.osp-item.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 600; }
.osp-item-ic { width: 18px; height: 18px; border-radius: 4px; background: var(--bl-primary); flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.osp-item-ic.is-var { background: #722ED1; }
</style>
