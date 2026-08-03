<template>
  <Teleport to="body">
    <transition name="ltp-fade">
      <div v-if="open" class="ltp-mask" @click.self="onCancel">
        <div class="ltp-modal">
          <div class="ltp-hd">
            <span class="ltp-title">链接类型选择</span>
            <span class="bl-muted ltp-sub" v-if="subtitle">{{ subtitle }}</span>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="onCancel" v-html="BL.icon('x', 14)"></button>
          </div>

          <!-- 已选: 单选时卡片自带勾选 + 底部确定, 这块只在多选时才有意义 -->
          <div v-if="multi" class="ltp-sel">
            <div v-if="selectedRows.length" class="ltp-sel-grid">
              <div v-for="r in selectedRows" :key="r.id" class="ltp-tag">
                <span class="ltp-tag-ic" v-html="BL.icon('link', 12, '#fff')"></span>
                <div class="ltp-tag-body">
                  <div class="ltp-tag-title bl-truncate">{{ r.rdfs_label || r.link_type_id }}</div>
                  <div class="ltp-tag-cmt bl-muted bl-truncate">
                    <span class="bl-mono">{{ r.link_type_id }}</span>
                    <span style="margin:0 5px">·</span>{{ r.l_class_name }} → {{ r.r_class_name }}
                  </div>
                </div>
                <button class="ltp-tag-x" title="移除" @click="toggle(r)" v-html="BL.icon('x', 10)"></button>
              </div>
            </div>
            <div v-else class="ltp-empty-sel">{{ multi ? '请在下方选择参与的链接类型' : '请在下方选择一个链接类型' }}</div>
          </div>

          <!-- 左领域树 + 右卡片 -->
          <div class="ltp-body">
            <aside class="ltp-tree">
              <CategoryCountTree :nodes="tree" :counts="catCounts" :active="activeNode"
                                 :all-count="allLinks.length" @pick="n => activeNode = n" />
            </aside>
            <section class="ltp-pane">
            <div class="ltp-pane-hd">
              <div class="ltp-scope">
                <span class="bl-muted">范围：</span><b>{{ activeNodeLabel }}</b>
                <span class="bl-muted ltp-count">共 {{ filtered.length }} 项</span>
                <span class="ltp-tip">双击卡片可直接确定</span>
              </div>
              <div class="ltp-search">
                <span class="ltp-search-ic" v-html="BL.icon('search', 13)"></span>
                <input class="bl-input" placeholder="搜索名称 / 编码 / 两端对象" v-model="q" />
                <button v-if="q" class="ltp-clear" @click="q = ''" v-html="BL.icon('x', 10)"></button>
              </div>
            </div>

            <div v-if="filtered.length" class="ltp-grid">
              <div v-for="r in filtered" :key="r.id"
                   :class="['ltp-card', selectedIds.has(r.id) && 'is-on']"
                   title="单击选择，双击直接确定"
                   @click="toggle(r)" @dblclick="pickAndConfirm(r)">
                <!-- 链接名称 -->
                <div class="ltp-card-hd">
                  <span class="bl-truncate ltp-card-title">{{ r.rdfs_label || r.link_type_id }}</span>
                  <span v-if="r.status !== 'active'" class="bl-tag bl-tag-danger">{{ r.status === 'deprecated' ? '废弃' : '停用' }}</span>
                </div>
                <div class="ltp-card-code bl-mono bl-muted bl-truncate" :title="r.link_type_id">{{ r.link_type_id }}</div>

                <!-- 源实体 —关联基数→ 目标实体 -->
                <div class="ltp-ends">
                  <span class="ltp-end" :title="`源实体 ${r.l_class_name} (${r.l_class_api})`">
                    <i class="ltp-end-ic" :style="{ background: r.l_class_color || '#165DFF' }"
                       v-html="BL.icon(r.l_class_icon || 'cube', 11, '#fff')"></i>
                    <span class="ltp-end-txt">
                      <b class="bl-truncate">{{ r.l_class_name || r.l_class_api }}</b>
                      <em class="bl-mono bl-truncate">{{ r.l_class_api }}</em>
                    </span>
                  </span>
                  <span class="ltp-card-num" :title="cardTitle(r)">{{ cardLabel(r) }}</span>
                  <span class="ltp-end" :title="`目标实体 ${r.r_class_name} (${r.r_class_api})`">
                    <i class="ltp-end-ic" :style="{ background: r.r_class_color || '#00B42A' }"
                       v-html="BL.icon(r.r_class_icon || 'cube', 11, '#fff')"></i>
                    <span class="ltp-end-txt">
                      <b class="bl-truncate">{{ r.r_class_name || r.r_class_api }}</b>
                      <em class="bl-mono bl-truncate">{{ r.r_class_api }}</em>
                    </span>
                  </span>
                </div>
                <div v-if="selectedIds.has(r.id)" class="ltp-card-check" v-html="BL.icon('check', 10, '#fff')"></div>
              </div>
            </div>
            <div v-else class="bl-empty" style="padding:48px">暂无匹配数据</div>
            </section>
          </div>

          <div class="ltp-ft">
            <button class="bl-btn bl-btn-text bl-btn-sm" :disabled="!selectedIds.size" @click="clearAll">
              <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">清空</span>
            </button>
            <span style="flex:1"></span>
            <button class="bl-btn bl-btn-sm" @click="onCancel">取消</button>
            <button class="bl-btn bl-btn-primary bl-btn-sm" :disabled="!selectedIds.size && required" @click="onConfirm">
              确定<template v-if="multi"> ({{ selectedIds.size }})</template>
            </button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import CategoryCountTree from '@/components/CategoryCountTree.vue'
import { linkTypeApi, categoryApi } from '@/api'

const props = defineProps({
  open: { type: Boolean, default: false },
  multi: { type: Boolean, default: true },
  required: { type: Boolean, default: false },
  modelValue: { type: Array, default: () => [] },
  excludeIds: { type: Array, default: () => [] },
  subtitle: { type: String, default: '' },
})
const emit = defineEmits(['update:open', 'update:modelValue', 'confirm', 'cancel'])

const allLinks = ref([])
const tree = ref([])
const selectedIds = ref(new Set(props.modelValue || []))
const q = ref('')
const activeNode = ref(null)

const selectedRows = computed(() => allLinks.value.filter(l => selectedIds.value.has(l.id)))

const catCounts = computed(() => {
  const m = {}
  allLinks.value.forEach(l => { if (l.category_code) m[l.category_code] = (m[l.category_code] || 0) + 1 })
  return m
})
const activeNodeLabel = computed(() => {
  const n = activeNode.value
  return n ? (n.rdfsLabel || n.label || n.categoryCode) : '全部'
})
function collectCodes(node) {
  const out = new Set()
  const walk = n => { if (n.categoryCode) out.add(n.categoryCode); (n.children || []).forEach(walk) }
  walk(node)
  return out
}

const CARD_TEXT = { 'one|one': '一对一', 'one|many': '一对多', 'many|one': '多对一', 'many|many': '多对多' }
function cardLabel(r) { return CARD_TEXT[`${r.l_cardinality}|${r.r_cardinality}`] || '关联' }
function cardTitle(r) { return `${r.l_class_name} ${r.l_cardinality === 'many' ? '多' : '一'} : ${r.r_cardinality === 'many' ? '多' : '一'} ${r.r_class_name}` }

const filtered = computed(() => {
  let list = allLinks.value
  if (props.excludeIds?.length) {
    const ex = new Set(props.excludeIds)
    list = list.filter(l => !ex.has(l.id))
  }
  if (activeNode.value) {
    const codes = collectCodes(activeNode.value)
    list = list.filter(l => codes.has(l.category_code))
  }
  const k = (q.value || '').trim().toLowerCase()
  if (!k) return list
  return list.filter(l => [l.rdfs_label, l.link_type_id, l.l_class_name, l.r_class_name, l.l_class_api, l.r_class_api, l.rdfs_comment]
    .filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
})

function toggle(row) {
  const s = new Set(selectedIds.value)
  if (s.has(row.id)) s.delete(row.id)
  else { if (!props.multi) s.clear(); s.add(row.id) }
  selectedIds.value = s
}
function clearAll() { selectedIds.value = new Set() }
/* 双击 = 选中并确定。不能复用 toggle: 双击的两次 click 会先选中再取消, 这里直接置为选中 */
function pickAndConfirm(row) {
  const s = props.multi ? new Set(selectedIds.value) : new Set()
  s.add(row.id)
  selectedIds.value = s
  onConfirm()
}
function onConfirm() {
  const ids = [...selectedIds.value]
  emit('update:modelValue', ids)
  emit('confirm', { ids, rows: allLinks.value.filter(l => selectedIds.value.has(l.id)) })
  emit('update:open', false)
}
function onCancel() { emit('cancel'); emit('update:open', false) }

async function load() {
  const [links, t] = await Promise.all([
    linkTypeApi.list().catch(() => []),
    categoryApi.tree().catch(() => []),
  ])
  allLinks.value = Array.isArray(links) ? links : (links?.rows || links?.data || [])
  tree.value = t || []
}

watch(() => props.open, async v => {
  if (!v) return
  selectedIds.value = new Set(props.modelValue || [])
  q.value = ''
  activeNode.value = null
  await load()
}, { immediate: true })

watch(() => props.modelValue, v => { selectedIds.value = new Set(v || []) })
</script>

<style scoped>
/* z-index 与其它大弹框对齐, 必须高于详情抽屉(1010) */
.ltp-mask { position: fixed; inset: 0; background: rgba(0,0,0,.40); z-index: 1200;
  display: flex; align-items: center; justify-content: center; }
.ltp-modal { background: var(--bl-bg-1); border-radius: 12px; width: 1000px; max-width: 95vw;
  height: 76vh; min-height: 520px; max-height: 92vh;
  display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 12px 40px rgba(0,0,0,.20); }
.ltp-hd { display: flex; align-items: center; gap: 10px; padding: 12px 16px; border-bottom: 1px solid var(--bl-divider); }
.ltp-title { font-size: var(--bl-fs-15); font-weight: 600; }
.ltp-sub { font-size: 12px; flex: 1; }
.ltp-hd .bl-btn { margin-left: auto; }

.ltp-sel { padding: 10px 14px; border-bottom: 1px solid var(--bl-divider); background: var(--bl-bg-2); flex-shrink: 0; }
.ltp-sel-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 8px;
  max-height: 26vh; overflow-y: auto; padding-right: 4px; }
.ltp-tag { position: relative; display: flex; gap: 8px; background: var(--bl-bg-1);
  border: 1px solid var(--bl-border); border-radius: 6px; padding: 6px 22px 6px 6px; }
.ltp-tag-ic { width: 22px; height: 22px; border-radius: 4px; background: #722ED1; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center; }
.ltp-tag-body { min-width: 0; flex: 1; }
.ltp-tag-title { font-size: 13px; font-weight: 500; }
.ltp-tag-cmt { font-size: 11px; line-height: 1.4; }
.ltp-tag-x { position: absolute; top: 4px; right: 4px; width: 16px; height: 16px; border: 0;
  background: var(--bl-bg-3); color: var(--bl-text-2); border-radius: 50%; cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center; }
.ltp-tag-x:hover { background: var(--bl-danger); color: #fff; }
.ltp-empty-sel { padding: 8px 12px; text-align: center; color: var(--bl-text-3); font-size: 12px;
  background: var(--bl-bg-1); border: 1px dashed var(--bl-border); border-radius: 4px; }

.ltp-body { flex: 1; min-height: 0; display: grid; grid-template-columns: 240px 1fr; overflow: hidden; background: var(--bl-bg-1); }
.ltp-tree { border-right: 1px solid var(--bl-border); overflow: hidden; min-height: 0; }
.ltp-pane { display: flex; flex-direction: column; overflow: hidden; min-height: 0; }
.ltp-pane-hd { display: flex; align-items: center; justify-content: space-between; gap: 8px;
  padding: 10px 14px; border-bottom: 1px solid var(--bl-divider); flex-shrink: 0; }
.ltp-scope { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.ltp-count { font-size: 12px; }
.ltp-tip { font-size: 11.5px; color: var(--bl-text-3); background: var(--bl-bg-2); border-radius: 9px; padding: 1px 8px; }
.ltp-search { position: relative; width: 260px; }
.ltp-search .bl-input { padding-left: 28px; padding-right: 24px; height: 30px; }
.ltp-search-ic { position: absolute; left: 8px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.ltp-clear { position: absolute; right: 6px; top: 50%; transform: translateY(-50%); width: 14px; height: 14px;
  border: 0; background: var(--bl-bg-3); border-radius: 50%; cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center; }
.ltp-pane > .bl-empty { flex: 1; display: flex; align-items: center; justify-content: center; }

.ltp-grid { flex: 1; overflow: auto; padding: 12px; display: grid; gap: 10px;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); align-content: start; }
.ltp-card { position: relative; display: flex; flex-direction: column; gap: 3px;
  padding: 10px 12px; background: var(--bl-bg-1); border: 1px solid var(--bl-border);
  border-left: 4px solid #722ED1; border-radius: 6px; cursor: pointer;
  transition: border-color .15s, box-shadow .15s, background-color .15s;
  user-select: none;   /* 双击确定时不要选中卡片文字 */ }
.ltp-card:hover { border-color: var(--bl-primary); border-left-color: #722ED1; box-shadow: var(--bl-shadow-1); }
.ltp-card.is-on { border-color: var(--bl-primary); border-left-color: #722ED1; background: var(--bl-primary-soft); }
.ltp-card-hd { display: flex; align-items: center; gap: 6px; }
.ltp-card-title { flex: 1; min-width: 0; font-size: 13.5px; font-weight: 500; }
.ltp-card-code { font-size: 11.5px; line-height: 1.3; }

/* 两端实体: 图标 + 中文名/api 两行, 中间是关联基数 —— 与链接类型列表页四列同构 */
.ltp-ends { display: flex; align-items: center; gap: 8px; margin-top: 8px;
  padding-top: 8px; border-top: 1px dashed var(--bl-divider); font-size: 12px; min-width: 0; }
.ltp-end { flex: 1; min-width: 0; display: inline-flex; align-items: center; gap: 5px; }
.ltp-end-ic { width: 20px; height: 20px; border-radius: 4px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center; }
.ltp-end-txt { min-width: 0; display: flex; flex-direction: column; line-height: 1.25; }
.ltp-end-txt b { font-weight: 500; color: var(--bl-text-1); }
.ltp-end-txt em { font-style: normal; font-size: 10.5px; color: var(--bl-text-3); }
.ltp-card-num { flex-shrink: 0; font-size: 11px; color: var(--bl-text-2); background: var(--bl-bg-2);
  border-radius: 9px; padding: 2px 8px; white-space: nowrap; }
.ltp-card.is-on .ltp-card-num { background: var(--bl-bg-1); }
.ltp-card-check { position: absolute; right: 8px; top: 8px; width: 16px; height: 16px;
  background: var(--bl-primary); border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; }

.ltp-ft { display: flex; align-items: center; gap: 8px; padding: 10px 14px;
  border-top: 1px solid var(--bl-divider); background: var(--bl-bg-1); flex-shrink: 0; }

.ltp-fade-enter-active, .ltp-fade-leave-active { transition: opacity .15s; }
.ltp-fade-enter-from, .ltp-fade-leave-to { opacity: 0; }
</style>
