<template>
  <Teleport to="body">
    <transition name="evp-fade">
      <div v-if="open" class="evp-mask" @click.self="close">
        <div class="evp-modal">
          <div class="evp-hd">
            <div style="min-width:0">
              <div class="evp-title">选择枚举值</div>
              <div class="evp-sub bl-truncate" :title="subtitle">{{ subtitle }}</div>
            </div>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="close" v-html="BL.icon('x', 14)"></button>
          </div>

          <div class="evp-body">
            <!-- 左: 枚举类型 (属性未绑定值类型时由用户现场指定) -->
            <div class="evp-left">
              <div class="evp-search">
                <span class="evp-search-ic" v-html="BL.icon('search', 12)"></span>
                <input v-model="qType" placeholder="搜索枚举类型" />
              </div>
              <div class="evp-type-list">
                <div v-if="typeLoading" class="evp-empty">加载枚举类型…</div>
                <template v-else>
                  <div v-for="t in filteredTypes" :key="t.id"
                       :class="['evp-type', curEnumId === t.id && 'is-on']" @click="pickType(t)">
                    <div class="bl-truncate" :title="t.label">{{ t.label }}</div>
                    <div class="bl-mono bl-muted bl-truncate" style="font-size:11px">{{ t.api_name }}</div>
                  </div>
                  <div v-if="!filteredTypes.length" class="evp-empty">无匹配枚举类型</div>
                </template>
              </div>
            </div>

            <!-- 右: 该枚举的值, 多选 -->
            <div class="evp-right">
              <div class="evp-tools">
                <div class="evp-search" style="flex:1">
                  <span class="evp-search-ic" v-html="BL.icon('search', 12)"></span>
                  <input v-model="q" placeholder="搜索枚举值 (名称 / 编码)" />
                </div>
                <button class="bl-btn bl-btn-sm" :disabled="!filtered.length" @click="selectAll">全选</button>
                <button class="bl-btn bl-btn-sm" :disabled="!picked.size" @click="clearAll">清空</button>
              </div>
              <div class="evp-items">
                <div v-if="!curEnumId" class="evp-empty">请先在左侧选择枚举类型</div>
                <div v-else-if="loading" class="evp-empty">加载枚举值…</div>
                <div v-else-if="!items.length" class="evp-empty">该枚举类型下暂无枚举值</div>
                <div v-else-if="!filtered.length" class="evp-empty">无匹配枚举值</div>
                <template v-else>
                  <div v-for="it in filtered" :key="it.code"
                       :class="['evp-item', picked.has(it.code) && 'is-on']"
                       :style="{ paddingLeft: (10 + indentOf(it) * 16) + 'px' }"
                       @click="toggle(it)">
                    <span class="evp-box" v-html="picked.has(it.code) ? BL.icon('check', 11, '#fff') : ''"></span>
                    <span class="evp-label bl-truncate" :title="it.label">{{ it.label }}</span>
                    <span class="bl-mono bl-muted evp-code">{{ it.code }}</span>
                    <span v-if="it.status && it.status !== 'active'" class="bl-tag evp-off">停用</span>
                  </div>
                </template>
              </div>
            </div>
          </div>

          <div class="evp-ft">
            <span class="bl-muted" style="font-size:12px">已选 <b>{{ picked.size }}</b> 项 · 保存为 [{ code, label }] JSON</span>
            <span style="flex:1"></span>
            <button class="bl-btn" @click="close">取消</button>
            <button class="bl-btn bl-btn-primary" :disabled="!curEnumId" @click="confirm">确定</button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { enumTypeApi } from '@/api'

const props = defineProps({
  open: Boolean,
  enumId: { type: String, default: '' },      // 属性已绑定枚举时传入, 未绑定则由用户在左栏选
  subtitle: { type: String, default: '' },
  modelValue: { type: Array, default: () => [] },   // [{ code, label }]
})
const emit = defineEmits(['update:open', 'confirm'])

const enumTypes = ref([])
const typeLoading = ref(false)
const curEnumId = ref('')
const items = ref([])
const loading = ref(false)
const q = ref('')
const qType = ref('')
const picked = ref(new Set())
const itemCache = ref({})   // enumId -> items[]

const filteredTypes = computed(() => {
  const k = qType.value.trim().toLowerCase()
  if (!k) return enumTypes.value
  return enumTypes.value.filter(t => [t.label, t.api_name].filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
})
const curType = computed(() => enumTypes.value.find(t => t.id === curEnumId.value) || null)

/* 多层级枚举按 parent_code 缩进; 搜索时拉平, 免得父级被过滤掉后子级悬空 */
const codeSet = computed(() => new Set(items.value.map(i => i.code)))
function indentOf(it) {
  if (q.value.trim()) return 0
  const lv = Number(it.level || 1)
  return lv > 1 && it.parent_code && codeSet.value.has(it.parent_code) ? lv - 1 : 0
}
const filtered = computed(() => {
  const k = q.value.trim().toLowerCase()
  if (!k) return items.value
  return items.value.filter(i => [i.label, i.code, i.api_name].filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
})

async function loadTypes() {
  if (enumTypes.value.length) return
  typeLoading.value = true
  try {
    const list = await enumTypeApi.list().catch(() => [])
    const arr = Array.isArray(list) ? list : (list?.data || [])
    enumTypes.value = arr.map(e => ({
      id: e.id, api_name: e.api_name || '', label: e.rdfs_label || e.display_name || e.api_name || e.id,
    }))
  } finally { typeLoading.value = false }
}
async function loadItems(enumId) {
  if (!enumId) { items.value = []; return }
  if (itemCache.value[enumId]) { items.value = itemCache.value[enumId]; return }
  loading.value = true
  try {
    const list = await enumTypeApi.listItems(enumId).catch(() => [])
    const arr = Array.isArray(list) ? list : (list?.data || [])
    const mapped = arr.map(x => ({
      code: String(x.code ?? x.item_code ?? ''),
      label: x.label || x.rdfs_label || String(x.code ?? ''),
      api_name: x.api_name || '',
      parent_code: x.parent_code || '',
      level: Number(x.level || 1),
      status: x.status || 'active',
    })).filter(x => x.code)
    itemCache.value = { ...itemCache.value, [enumId]: mapped }
    items.value = mapped
  } finally { loading.value = false }
}

function pickType(t) {
  if (curEnumId.value === t.id) return
  curEnumId.value = t.id
  q.value = ''
  picked.value = new Set()   // 换枚举类型, 旧的勾选不再适用
  loadItems(t.id)
}
function toggle(it) {
  const s = new Set(picked.value)
  s.has(it.code) ? s.delete(it.code) : s.add(it.code)
  picked.value = s
}
function selectAll() { picked.value = new Set([...picked.value, ...filtered.value.map(i => i.code)]) }
function clearAll() { picked.value = new Set() }
function close() { emit('update:open', false) }
function confirm() {
  const values = items.value.filter(i => picked.value.has(i.code)).map(i => ({ code: i.code, label: i.label }))
  emit('confirm', { enum_id: curEnumId.value, enum_label: curType.value?.label || '', values })
  emit('update:open', false)
}

watch(() => props.open, async v => {
  if (!v) return
  q.value = ''; qType.value = ''
  await loadTypes()
  curEnumId.value = props.enumId || ''
  if (curEnumId.value) await loadItems(curEnumId.value)
  else items.value = []
  picked.value = new Set((props.modelValue || []).map(x => String(x && x.code != null ? x.code : x)))
})
</script>

<style scoped>
.evp-mask { position: fixed; inset: 0; background: rgba(0,0,0,.5); backdrop-filter: blur(3px); z-index: 1300; display: flex; align-items: center; justify-content: center; }
:root[data-theme="dark"] .evp-mask { background: rgba(0,0,0,.62); }
.evp-modal { background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 12px; width: 700px; max-width: 96vw; height: 500px; max-height: 88vh; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 24px 56px rgba(0,0,0,0.5); }
.evp-hd { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; padding: 12px 16px; border-bottom: 1px solid var(--bl-divider); }
.evp-title { font-size: 15px; font-weight: 600; }
.evp-sub { font-size: 12px; color: var(--bl-text-3); margin-top: 3px; }
.evp-body { flex: 1; min-height: 0; display: flex; }
.evp-left { flex: 0 0 230px; border-right: 1px solid var(--bl-divider); display: flex; flex-direction: column; overflow: hidden; }
.evp-right { flex: 1; min-width: 0; display: flex; flex-direction: column; overflow: hidden; }
.evp-tools { display: flex; align-items: center; gap: 8px; padding: 8px 10px; border-bottom: 1px solid var(--bl-divider); }
.evp-search { display: flex; align-items: center; gap: 6px; border: 1px solid var(--bl-border); border-radius: 6px; padding: 5px 8px; margin: 8px 10px; }
.evp-left .evp-search { margin-bottom: 0; }
.evp-right .evp-search { margin: 0; }
.evp-search-ic { color: var(--bl-text-3); display: inline-flex; }
.evp-search input { flex: 1; min-width: 0; border: 0; outline: none; background: transparent; font-size: 13px; color: var(--bl-text-1); }
.evp-type-list { flex: 1; overflow-y: auto; padding: 6px; }
.evp-type { padding: 7px 10px; border-radius: 6px; cursor: pointer; font-size: 13px; }
.evp-type:hover { background: var(--bl-bg-hover); }
.evp-type.is-on { background: var(--bl-primary-soft); }
.evp-items { flex: 1; overflow-y: auto; padding: 6px; }
.evp-item { display: flex; align-items: center; gap: 8px; padding: 7px 10px; border-radius: 6px; cursor: pointer; font-size: 13px; }
.evp-item:hover { background: var(--bl-bg-hover); }
.evp-item.is-on { background: var(--bl-primary-soft); }
.evp-box { width: 14px; height: 14px; flex-shrink: 0; border: 1px solid var(--bl-border); border-radius: 3px; display: inline-flex; align-items: center; justify-content: center; background: var(--bl-bg-1); }
.evp-item.is-on .evp-box { background: var(--bl-primary); border-color: var(--bl-primary); }
.evp-label { min-width: 0; }
.evp-code { font-size: 11px; margin-left: auto; flex-shrink: 0; }
.evp-off { font-size: 11px; flex-shrink: 0; }
.evp-empty { padding: 32px 16px; text-align: center; color: var(--bl-text-3); font-size: 12px; }
.evp-ft { display: flex; align-items: center; gap: 8px; padding: 10px 16px; border-top: 1px solid var(--bl-divider); }
.evp-fade-enter-active, .evp-fade-leave-active { transition: opacity .15s; }
.evp-fade-enter-from, .evp-fade-leave-to { opacity: 0; }
</style>
