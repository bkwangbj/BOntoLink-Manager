<template>
  <Teleport to="body">
    <transition name="otp-fade">
      <div v-if="open" class="otp-mask" @click.self="onCancel">
        <div class="otp-modal">
          <!-- 标题 -->
          <div class="otp-hd">
            <span class="otp-title">对象类型选择</span>
            <span class="bl-muted otp-sub" v-if="subtitle">{{ subtitle }}</span>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="onCancel" v-html="BL.icon('x', 14)"></button>
          </div>

          <!-- 上：已选对象类型。单选时卡片自带勾选标记 + 底部确定, 这块纯属重复, 只在多选时保留 -->
          <div v-if="multi" class="otp-sel">
            <div v-if="selectedRows.length" class="otp-sel-grid">
              <div v-for="r in selectedRows" :key="r.id" class="otp-tag">
                <span class="otp-tag-ic" :style="{ background: r.color || '#165DFF' }" v-html="BL.icon(r.icon || 'cube', 12, '#fff')"></span>
                <div class="otp-tag-body">
                  <div class="otp-tag-title bl-truncate">
                    {{ r.display_name || r.rdfs_label || r.api_name }}
                    <span class="otp-tag-stat" :title="`普通属性 ${r.propNormal ?? 0} 项 / 全部属性 ${r.propTotal ?? 0} 项`">属性 {{ r.propNormal ?? 0 }} | {{ r.propTotal ?? 0 }}</span>
                    <span v-if="r.status !== 1" class="bl-tag bl-tag-danger">禁用</span>
                  </div>
                  <div class="otp-tag-cmt bl-muted bl-truncate">
                    <span class="bl-mono">{{ r.api_name }}</span>
                    <span style="margin:0 5px">·</span>{{ r.categoryLabel || '未分类' }}
                  </div>
                </div>
                <button class="otp-tag-x" title="移除" @click="toggle(r)" v-html="BL.icon('x', 10)"></button>
              </div>
            </div>
            <div v-else class="otp-empty-sel">{{ multi ? '请在下方选择参与的对象类型' : '请在下方选择一个对象类型' }}</div>
          </div>

          <!-- 下：选择操作区（左树 + 右卡片） -->
          <div class="otp-body">
            <aside class="otp-tree">
              <CategoryCountTree :nodes="tree" :counts="catCounts" :active="activeNode"
                                 :all-count="allClasses.length" @pick="onPickNode" />
            </aside>
            <section class="otp-pane">
              <div class="otp-pane-hd">
                <div class="bl-row" style="gap:6px;align-items:center;font-size:13px;color:var(--bl-text-2)">
                  <span class="bl-muted">范围：</span><b>{{ activeNodeLabel }}</b>
                  <span class="bl-muted">共 {{ filtered.length }} 项</span>
                  <span class="otp-tip">双击卡片可直接确定</span>
                </div>
                <div class="otp-search">
                  <span class="ot-ic" v-html="BL.icon('search', 13)"></span>
                  <input class="bl-input" placeholder="搜索：支持中文、全拼、简拼" v-model="q" />
                  <button v-if="q" class="otp-clear" @click="q=''" v-html="BL.icon('x', 10)"></button>
                </div>
              </div>
              <div class="otp-grid" v-if="filtered.length">
                <div v-for="r in filtered" :key="r.id"
                     :class="['otp-card', selectedIds.has(r.id) && 'is-on']"
                     :style="{ '--otp-side': r.color || '#165DFF' }"
                     title="单击选择，双击直接确定"
                     @click="toggle(r)" @dblclick="pickAndConfirm(r)">
                  <div class="otp-card-ic" :style="{ background: r.color || '#165DFF' }" v-html="BL.icon(r.icon || 'cube', 14, '#fff')"></div>
                  <div class="otp-card-body">
                    <div class="otp-card-title">
                      <span class="bl-truncate">{{ r.display_name || r.rdfs_label || r.api_name }}</span>
                      <span v-if="r.status !== 1" class="bl-tag bl-tag-danger">禁用</span>
                    </div>
                    <div class="otp-card-api bl-mono bl-muted bl-truncate" :title="r.api_name">{{ r.api_name }}</div>
                    <div class="otp-card-meta">
                      <span class="otp-card-dom bl-truncate" :title="r.categoryLabel || '未分类'">
                        <i class="otp-meta-ic" v-html="BL.icon('folder', 11)"></i>{{ r.categoryLabel || '未分类' }}
                      </span>
                      <span class="otp-card-prop" :title="`普通属性 ${r.propNormal ?? 0} 项 / 全部属性 ${r.propTotal ?? 0} 项`">
                        属性 <b>{{ r.propNormal ?? 0 }}</b><i>|</i><b>{{ r.propTotal ?? 0 }}</b>
                      </span>
                    </div>
                    <div v-if="r.rdfs_comment" class="otp-card-cmt bl-muted bl-truncate2">{{ r.rdfs_comment }}</div>
                  </div>
                  <div v-if="selectedIds.has(r.id)" class="otp-card-check" v-html="BL.icon('check', 10, '#fff')"></div>
                </div>
              </div>
              <div v-else class="bl-empty" style="padding:48px">暂无匹配数据</div>
            </section>
          </div>

          <!-- 底部操作行 -->
          <div class="otp-ft">
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
import { resourceApi, categoryApi } from '@/api'

const props = defineProps({
  open: { type: Boolean, default: false },
  multi: { type: Boolean, default: true },
  required: { type: Boolean, default: false },
  modelValue: { type: Array, default: () => [] },   // 已选 id 列表（input/output）
  excludeIds: { type: Array, default: () => [] },   // 不可选的 id（已被使用、自身等）
  subtitle: { type: String, default: '' }
})
const emit = defineEmits(['update:open', 'update:modelValue', 'confirm', 'cancel'])

const allClasses = ref([])      // 全量对象类型
const tree = ref([])            // 分类树
const selectedIds = ref(new Set(props.modelValue || []))
const activeNode = ref(null)    // 当前选中的树节点（category_code 串）
const q = ref('')

const selectedRows = computed(() => allClasses.value.filter(c => selectedIds.value.has(c.id)))

// 每个 category_code 下的对象类数量统计
const catCounts = computed(() => {
  const m = {}
  for (const c of allClasses.value) {
    if (!c.category_code) continue
    m[c.category_code] = (m[c.category_code] || 0) + 1
  }
  return m
})

const activeNodeLabel = computed(() => {
  if (!activeNode.value) return '全部'
  // activeNode 是 category_code 或 node 对象
  const n = activeNode.value
  return n.rdfs_label || n.label || n.categoryCode || '全部'
})

const filtered = computed(() => {
  let list = allClasses.value
  if (props.excludeIds && props.excludeIds.length) {
    const ex = new Set(props.excludeIds)
    list = list.filter(c => !ex.has(c.id))
  }
  if (activeNode.value) {
    const codes = collectCodes(activeNode.value)
    list = list.filter(c => codes.has(c.category_code))
  }
  const k = (q.value || '').trim().toLowerCase()
  if (!k) return list
  return list.filter(c =>
    [c.api_name, c.display_name, c.rdfs_label, c.rdfs_comment].filter(Boolean)
      .some(s => String(s).toLowerCase().includes(k)))
})

function collectCodes(node) {
  const out = new Set()
  const walk = (n) => { if (n.categoryCode) out.add(n.categoryCode); (n.children || []).forEach(walk) }
  walk(node)
  return out
}

function onPickNode(node) { activeNode.value = node }

function toggle(row) {
  const s = new Set(selectedIds.value)
  if (s.has(row.id)) s.delete(row.id)
  else {
    if (!props.multi) s.clear()
    s.add(row.id)
  }
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
  const rows = allClasses.value.filter(c => selectedIds.value.has(c.id))
  emit('update:modelValue', ids)
  emit('confirm', { ids, rows })
  emit('update:open', false)
}
function onCancel() { emit('cancel'); emit('update:open', false) }

async function load() {
  try {
    const [classes, t] = await Promise.all([
      resourceApi.classes({ aggregate: true }).catch(() => []),
      categoryApi.tree().catch(() => [])
    ])
    allClasses.value = classes || []
    tree.value = t || []
  } catch { allClasses.value = []; tree.value = [] }
}

watch(() => props.open, async (v) => {
  if (v) {
    selectedIds.value = new Set(props.modelValue || [])
    activeNode.value = null
    q.value = ''
    await load()
  }
}, { immediate: true })

watch(() => props.modelValue, (v) => {
  selectedIds.value = new Set(v || [])
})

</script>

<style scoped>
.otp-mask {
  /* z-index 必须高于:对象类型详情抽屉(1000) + 属性详情抽屉(1010) + 抽屉 resize 把手(1011),
     与 ValueTypePicker / EnumPicker / SharedPropertyPicker / PropertyFormatModal 等大弹框对齐 */
  position: fixed; inset: 0; background: rgba(0,0,0,.40);
  z-index: 1200; display: flex; align-items: center; justify-content: center;
}
.otp-modal {
  background: var(--bl-bg-1); border-radius: 12px;
  width: 1100px; max-width: 95vw;
  /* 固定高度：无论左侧树切到哪个节点、右侧卡片多少，弹框尺寸都保持一致 */
  height: 80vh; min-height: 560px; max-height: 92vh;
  display: flex; flex-direction: column; overflow: hidden;
  box-shadow: 0 12px 40px rgba(0,0,0,.20);
}
.otp-hd {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px; border-bottom: 1px solid var(--bl-divider);
}
.otp-title { font-size: var(--bl-fs-15); font-weight: 600; }
.otp-sub { font-size: 12px; flex: 1; }
.otp-hd .bl-btn { margin-left: auto; }

/* 上：已选 — 设最高 ~3 行高度，超出滚动；不挤压下方树/卡片区 */
.otp-sel {
  padding: 10px 14px; border-bottom: 1px solid var(--bl-divider);
  display: flex; flex-direction: column; gap: 8px;
  background: var(--bl-bg-2);
  flex-shrink: 0;
  max-height: 30vh;       /* 顶部已选区最高不超过弹框 30% */
  overflow: hidden;
}
.otp-sel-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 8px;
  overflow-y: auto;       /* 超出 max-height 时滚动 */
  max-height: 30vh;
  padding-right: 4px;     /* 给滚动条留白 */
}
.otp-tag {
  position: relative;
  display: flex; gap: 8px; align-items: stretch;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border);
  border-radius: 6px; padding: 6px 22px 6px 6px;
}
.otp-tag-ic { width: 22px; height: 22px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.otp-tag-body { min-width: 0; flex: 1; }
.otp-tag-title { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 500; }
.otp-tag-stat { font-size: 11px; color: var(--bl-text-3); margin-left: auto; }
.otp-tag-cmt { font-size: 11px; line-height: 1.4; }
.otp-tag-x {
  position: absolute; top: 4px; right: 4px;
  width: 16px; height: 16px; border: 0; background: var(--bl-bg-3); color: var(--bl-text-2);
  border-radius: 50%; cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
}
.otp-tag-x:hover { background: var(--bl-danger); color: #fff; }
.otp-empty-sel {
  padding: 8px 12px; text-align: center;
  color: var(--bl-text-3); font-size: 12px;
  background: var(--bl-bg-1); border: 1px dashed var(--bl-border);
  border-radius: 4px;
}
.otp-ft {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; border-top: 1px solid var(--bl-divider);
  background: var(--bl-bg-1); flex-shrink: 0;
}

/* 下：树 + 卡片 — 左侧浅灰底，右侧白底；高度由父级 flex:1 决定，左右始终同高 */
.otp-body {
  flex: 1; display: grid; grid-template-columns: 240px 1fr;
  overflow: hidden; min-height: 0;
  background: var(--bl-bg-1);
}
/* 树本体样式在 CategoryCountTree 里, 这里只负责分栏边框 */
.otp-tree { border-right: 1px solid var(--bl-border); overflow: hidden; min-height: 0; }

.otp-pane { display: flex; flex-direction: column; overflow: hidden; min-height: 0; background: var(--bl-bg-1); }
.otp-pane-hd {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  padding: 10px 14px; border-bottom: 1px solid var(--bl-divider);
  flex-shrink: 0;
}
/* 空数据态填满右侧 */
.otp-pane > .bl-empty { flex: 1; display: flex; align-items: center; justify-content: center; flex-direction: column; gap: 8px; }
.otp-tip { font-size: 11.5px; color: var(--bl-text-3); background: var(--bl-bg-2); border-radius: 9px; padding: 1px 8px; }
.otp-search { position: relative; width: 280px; }
.otp-search .bl-input { padding-left: 28px; padding-right: 24px; height: 30px; }
.otp-search .ot-ic { position: absolute; left: 8px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.otp-clear {
  position: absolute; right: 6px; top: 50%; transform: translateY(-50%);
  width: 14px; height: 14px; border: 0; background: var(--bl-bg-3); border-radius: 50%; cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center;
}
.otp-grid {
  flex: 1; overflow: auto; padding: 12px;
  display: grid; gap: 10px;
  grid-template-columns: repeat(auto-fill, minmax(272px, 1fr));
  align-content: start;
}
.otp-card {
  position: relative;
  display: flex; align-items: flex-start; gap: 10px;
  padding: 10px 12px 10px 10px;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-left: 4px solid var(--otp-side, var(--bl-primary));
  border-radius: 6px;
  cursor: pointer; transition: border-color .15s, box-shadow .15s, background-color .15s;
  min-height: 86px; box-sizing: border-box;
  user-select: none;   /* 双击确定时不要选中卡片文字 */
}
.otp-card:hover { border-color: var(--bl-primary); border-left-color: var(--otp-side); box-shadow: var(--bl-shadow-1); }
.otp-card.is-on { border-color: var(--bl-primary); border-left-color: var(--otp-side); background: var(--bl-primary-soft); }
.otp-card-ic {
  width: 28px; height: 28px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.otp-card-body { min-width: 0; flex: 1; display: flex; flex-direction: column; gap: 3px; }
.otp-card-title {
  display: flex; align-items: center; gap: 6px;
  font-size: 13.5px; font-weight: 500; line-height: 1.4;
}
.otp-card-title .bl-truncate { flex: 1; min-width: 0; }
.otp-card-api { font-size: 11.5px; line-height: 1.3; }
/* 领域占剩余宽度, 属性数固定靠右, 长领域名截断也不会把数字挤走 */
.otp-card-meta {
  display: flex; align-items: center; gap: 8px;
  font-size: 11.5px; line-height: 1.4; margin-top: 1px;
}
.otp-card-dom { flex: 1; min-width: 0; display: inline-flex; align-items: center; gap: 3px; color: var(--bl-text-3); }
.otp-meta-ic { display: inline-flex; flex-shrink: 0; color: var(--bl-text-3); }
.otp-card-prop {
  flex-shrink: 0; display: inline-flex; align-items: baseline; gap: 2px;
  color: var(--bl-text-3); background: var(--bl-bg-2);
  border-radius: 9px; padding: 1px 8px; font-feature-settings: "tnum";
}
.otp-card-prop b { color: var(--bl-text-2); font-weight: 600; }
.otp-card-prop i { color: var(--bl-border-strong); font-style: normal; margin: 0 2px; }
.otp-card.is-on .otp-card-prop { background: var(--bl-bg-1); }
.otp-card-cmt { font-size: 12px; line-height: 1.4; color: var(--bl-text-3); }
.otp-card-check {
  position: absolute; right: 8px; top: 8px;
  width: 16px; height: 16px; background: var(--bl-primary); border-radius: 50%;
  display: inline-flex; align-items: center; justify-content: center;
}

.bl-truncate2 { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.otp-fade-enter-active, .otp-fade-leave-active { transition: opacity .15s; }
.otp-fade-enter-from, .otp-fade-leave-to { opacity: 0; }
</style>
