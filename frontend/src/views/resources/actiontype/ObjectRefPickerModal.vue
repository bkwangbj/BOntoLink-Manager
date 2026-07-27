<template>
  <Teleport to="body">
    <transition name="orp-fade">
      <div v-if="open" class="orp-mask" @click.self="close">
        <div class="orp-modal">
          <div class="orp-hd">
            <div class="orp-title">选择关联对象属性</div>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="close" v-html="BL.icon('x', 14)"></button>
          </div>
          <div class="orp-body">
            <!-- 左: 关联对象 -->
            <div class="orp-left">
              <div class="orp-sub">关联对象</div>
              <div class="orp-obj-list">
                <div v-for="o in relatedObjects" :key="o.key"
                     :class="['orp-obj', sel && sel.key === o.key && 'is-on']" @click="selectObj(o)">
                  <span class="orp-obj-ic" :style="{ background: o.color || '#165DFF' }" v-html="BL.icon(o.icon || 'box', 12, '#fff')"></span>
                  <div class="orp-obj-txt">
                    <div class="bl-truncate" :title="o.label">{{ o.label }}</div>
                    <div class="bl-muted bl-truncate" style="font-size:11px">{{ o.via }}</div>
                  </div>
                </div>
                <div v-if="!relatedObjects.length" class="orp-empty">无关联对象</div>
              </div>
            </div>
            <!-- 右: 属性 -->
            <div class="orp-right">
              <div class="orp-search">
                <span class="orp-search-ic" v-html="BL.icon('search', 12)"></span>
                <input v-model="q" placeholder="搜索属性 (名称 / 编码)" />
              </div>
              <div class="orp-props">
                <div v-if="!sel" class="orp-empty">请先在左侧选择关联对象</div>
                <div v-else-if="loading" class="orp-empty">加载属性…</div>
                <template v-else>
                  <div v-for="p in filteredProps" :key="p.code" class="orp-prop" @click="pickProp(p)">
                    <span class="bl-truncate">{{ p.name }}</span>
                    <span class="bl-mono bl-muted" style="font-size:11px;margin-left:8px">{{ p.code }}</span>
                  </div>
                  <div v-if="!filteredProps.length" class="orp-empty">无匹配属性</div>
                </template>
              </div>
            </div>
          </div>
          <div class="orp-ft">
            <span class="bl-muted" style="font-size:12px">选中属性即返回「对象.属性」</span>
            <span style="flex:1"></span>
            <button class="bl-btn" @click="close">取消</button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { resourceApi } from '@/api'

const props = defineProps({
  open: Boolean,
  sourceClassId: String,
  allClasses: { type: Array, default: () => [] },
  allLinkTypes: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:open', 'pick'])

const sel = ref(null)
const propList = ref([])
const loading = ref(false)
const q = ref('')

function classById(id) {
  const c = (props.allClasses || []).find(x => x.id === id)
  if (!c) return null
  return { id: c.id, label: c.display_name || c.rdfs_label || c.api_name, api: c.api_name, icon: c.icon, color: c.color }
}

/* 当前对象 + 通过链接关联的对象 */
const relatedObjects = computed(() => {
  const out = []
  const seen = new Set()
  const push = (id, via) => {
    const c = classById(id)
    if (!c || seen.has(id)) return
    seen.add(id)
    out.push({ key: id, classId: id, label: c.label, api: c.api, icon: c.icon, color: c.color, via })
  }
  push(props.sourceClassId, '本对象')
  for (const l of (props.allLinkTypes || [])) {
    const name = l.rdfs_label || l.link_type_id || '链接'
    if (l.l_object_type_id === props.sourceClassId && l.r_object_type_id) push(l.r_object_type_id, `经「${name}」关联`)
    else if (l.r_object_type_id === props.sourceClassId && l.l_object_type_id) push(l.l_object_type_id, `经「${name}」关联`)
  }
  return out
})

async function selectObj(o) {
  sel.value = o
  q.value = ''
  loading.value = true
  try {
    const list = await resourceApi.properties(o.classId).catch(() => [])
    const arr = Array.isArray(list) ? list : (list?.data || [])
    propList.value = arr.map(p => ({ code: p.api_name || p.prop_code, name: p.display_name || p.rdfs_label || p.api_name }))
  } finally { loading.value = false }
}
const filteredProps = computed(() => {
  const k = q.value.trim().toLowerCase()
  if (!k) return propList.value
  return propList.value.filter(p => [p.name, p.code].filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
})
function pickProp(p) {
  emit('pick', {
    object_label: sel.value.label, object_api: sel.value.api,
    prop_code: p.code, prop_name: p.name,
    display: `${sel.value.label}.${p.name}`,
    code: `${sel.value.api}.${p.code}`,
  })
  emit('update:open', false)
}
function close() { emit('update:open', false) }

watch(() => props.open, v => { if (v) { sel.value = null; propList.value = []; q.value = '' } })
</script>

<style scoped>
.orp-mask { position: fixed; inset: 0; background: rgba(0,0,0,.5); backdrop-filter: blur(3px); z-index: 1300; display: flex; align-items: center; justify-content: center; }
:root[data-theme="dark"] .orp-mask { background: rgba(0,0,0,.62); }
.orp-modal { background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 12px; width: 640px; max-width: 96vw; height: 480px; max-height: 88vh; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 24px 56px rgba(0,0,0,0.5); }
.orp-hd { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid var(--bl-divider); }
.orp-title { font-size: 15px; font-weight: 600; }
.orp-body { flex: 1; min-height: 0; display: flex; }
.orp-left { flex: 0 0 220px; border-right: 1px solid var(--bl-divider); display: flex; flex-direction: column; overflow: hidden; }
.orp-sub { font-size: 12px; color: var(--bl-text-3); font-weight: 600; padding: 10px 12px 6px; }
.orp-obj-list { flex: 1; overflow-y: auto; padding: 0 6px 8px; }
.orp-obj { display: flex; align-items: center; gap: 8px; padding: 8px 8px; border-radius: 6px; cursor: pointer; }
.orp-obj:hover { background: var(--bl-bg-hover); }
.orp-obj.is-on { background: var(--bl-primary-soft); }
.orp-obj-ic { width: 22px; height: 22px; border-radius: 4px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.orp-obj-txt { min-width: 0; font-size: 13px; }
.orp-right { flex: 1; min-width: 0; display: flex; flex-direction: column; overflow: hidden; }
.orp-search { display: flex; align-items: center; gap: 6px; padding: 10px 12px; border-bottom: 1px solid var(--bl-divider); }
.orp-search-ic { color: var(--bl-text-3); display: inline-flex; }
.orp-search input { flex: 1; border: 0; outline: none; background: transparent; font-size: 13px; color: var(--bl-text-1); }
.orp-props { flex: 1; overflow-y: auto; padding: 6px; }
.orp-prop { display: flex; align-items: center; padding: 8px 10px; border-radius: 6px; cursor: pointer; font-size: 13px; }
.orp-prop:hover { background: var(--bl-bg-hover); }
.orp-empty { padding: 24px; text-align: center; color: var(--bl-text-3); font-size: 12px; }
.orp-ft { display: flex; align-items: center; gap: 8px; padding: 10px 16px; border-top: 1px solid var(--bl-divider); }
.orp-fade-enter-active, .orp-fade-leave-active { transition: opacity .15s; }
.orp-fade-enter-from, .orp-fade-leave-to { opacity: 0; }
</style>
