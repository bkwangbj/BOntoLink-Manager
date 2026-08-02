<template>
  <Teleport to="body">
    <div v-if="open" class="ppm-mask" @click.self="close">
      <div class="ppm-modal">
        <div class="ppm-hd"><span>选择属性</span><span style="flex:1"></span>
          <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="close" v-html="BL.icon('x', 14)"></button></div>

        <div class="ppm-body">
          <!-- 左: 属性来源 (动作主体对象 / 规则中创建的对象) -->
          <div class="ppm-left">
            <div v-for="s in sources" :key="s.key" :class="['ppm-src', tab === s.key && 'is-on']" @click="tab = s.key">
              <span class="ppm-src-ic" :style="{ background: s.color }" v-html="BL.icon('box', 12, '#fff')"></span>
              <div class="ppm-src-txt">
                <div class="bl-truncate">{{ s.label }}</div>
                <div class="ppm-src-sub bl-truncate">{{ s.sub }}</div>
              </div>
              <span class="ppm-src-n">{{ availableOf(s.key).length }}</span>
            </div>
            <div v-if="!sources.length" class="ppm-empty">无可用对象</div>
          </div>

          <!-- 右: 未加入表单的属性, 可多选 -->
          <div class="ppm-right">
            <div class="ppm-search">
              <span class="ppm-search-ic" v-html="BL.icon('search', 12)"></span>
              <input ref="searchEl" v-model="q" placeholder="搜索属性 (名称 / 编码)" @keydown.esc="close" />
            </div>
            <div class="ppm-tool">
              <label class="ppm-ck"><input type="checkbox" :checked="allChecked" @change="toggleAll" /> 全选当前列表</label>
              <span style="flex:1"></span>
              <span class="bl-muted">已选 {{ picked.size }} 项</span>
            </div>
            <div class="ppm-list">
              <label v-for="p in filtered" :key="p.code" :class="['ppm-opt', picked.has(p.code) && 'is-on']">
                <input type="checkbox" :checked="picked.has(p.code)" @change="toggle(p.code)" />
                <span class="bl-truncate ppm-opt-name">{{ p.name }}</span>
                <span class="bl-mono bl-muted ppm-opt-code bl-truncate">{{ p.code }}</span>
                <span class="ppm-dt bl-muted">{{ dtLabel(p.dataType) }}</span>
              </label>
              <div v-if="!filtered.length" class="ppm-empty">{{ q ? '无匹配属性' : '该对象的属性已全部加入表单' }}</div>
            </div>
          </div>
        </div>

        <div class="ppm-ft">
          <span class="bl-muted ppm-ft-tip">已加入表单的属性不再列出</span>
          <span style="flex:1"></span>
          <button class="bl-btn bl-btn-sm" @click="close">取消</button>
          <button class="bl-btn bl-btn-sm bl-btn-primary" :disabled="!picked.size" @click="confirm">确定{{ picked.size ? `(${picked.size})` : '' }}</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { BL } from '@/lib/bl.js'
import { dtLabel } from './conditionModel.js'

const props = defineProps({
  open: Boolean,
  sources: { type: Array, default: () => [] },   // [{ key, label, sub, color, props:[{code,name,dataType,required}] }]
  usedCodes: { type: Array, default: () => [] }, // 已加入表单的属性编码
})
const emit = defineEmits(['update:open', 'pick'])

const q = ref('')
const tab = ref('')
const picked = ref(new Set())
const searchEl = ref(null)

const used = computed(() => new Set(props.usedCodes))
function availableOf(key) {
  const s = props.sources.find(x => x.key === key)
  return (s?.props || []).filter(p => !used.value.has(p.code))
}
const filtered = computed(() => {
  const k = q.value.trim().toLowerCase()
  return availableOf(tab.value).filter(p => !k || [p.name, p.code].filter(Boolean).some(x => String(x).toLowerCase().includes(k)))
})
const allChecked = computed(() => filtered.value.length > 0 && filtered.value.every(p => picked.value.has(p.code)))

function toggle(code) {
  const s = new Set(picked.value)
  s.has(code) ? s.delete(code) : s.add(code)
  picked.value = s
}
function toggleAll() {
  const s = new Set(picked.value)
  const all = allChecked.value
  filtered.value.forEach(p => all ? s.delete(p.code) : s.add(p.code))
  picked.value = s
}
function confirm() {
  /* 按来源归位, 让调用方知道每个属性来自哪个对象 */
  const out = []
  props.sources.forEach(s => (s.props || []).forEach(p => { if (picked.value.has(p.code)) out.push({ ...p, source: s.key, classId: s.classId }) }))
  emit('pick', out)
  close()
}
function close() { emit('update:open', false) }

watch(() => props.open, v => {
  if (!v) return
  q.value = ''
  picked.value = new Set()
  tab.value = props.sources[0]?.key || ''
  nextTick(() => searchEl.value?.focus())
})
</script>

<style scoped>
.ppm-mask { position: fixed; inset: 0; background: rgba(0,0,0,.45); backdrop-filter: blur(3px); z-index: 1350; display: flex; align-items: center; justify-content: center; }
.ppm-modal { width: 660px; max-width: 96vw; height: 480px; max-height: 88vh; background: var(--bl-bg-1); border: 1px solid var(--bl-border);
  border-radius: 12px; box-shadow: 0 24px 56px rgba(0,0,0,.4); display: flex; flex-direction: column; overflow: hidden; }
.ppm-hd { display: flex; align-items: center; padding: 12px 16px; font-size: 14px; font-weight: 600; border-bottom: 1px solid var(--bl-divider); }
.ppm-body { flex: 1; min-height: 0; display: flex; }
.ppm-left { flex: 0 0 190px; border-right: 1px solid var(--bl-divider); padding: 8px 6px; overflow-y: auto; }
.ppm-src { display: flex; align-items: center; gap: 8px; padding: 8px; border-radius: 6px; cursor: pointer; }
.ppm-src:hover { background: var(--bl-bg-hover); }
.ppm-src.is-on { background: var(--bl-primary-soft); }
.ppm-src.is-on .ppm-src-txt > div:first-child { color: var(--bl-primary); font-weight: 600; }
.ppm-src-ic { width: 22px; height: 22px; border-radius: 5px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.ppm-src-txt { flex: 1; min-width: 0; font-size: 12.5px; }
.ppm-src-sub { font-size: 11px; color: var(--bl-text-3); margin-top: 1px; }
.ppm-src-n { font-size: 11px; color: var(--bl-text-3); flex-shrink: 0; }
.ppm-right { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.ppm-search { display: flex; align-items: center; gap: 6px; padding: 10px 12px; border-bottom: 1px solid var(--bl-divider); }
.ppm-search-ic { color: var(--bl-text-3); display: inline-flex; }
.ppm-search input { flex: 1; min-width: 0; border: 0; outline: none; background: transparent; font-size: 13px; color: var(--bl-text-1); }
.ppm-tool { display: flex; align-items: center; gap: 8px; padding: 8px 12px 4px; font-size: 12px; color: var(--bl-text-2); }
.ppm-ck { display: inline-flex; align-items: center; gap: 6px; cursor: pointer; }
.ppm-list { flex: 1; overflow-y: auto; padding: 4px 8px 8px; }
.ppm-opt { display: flex; align-items: center; gap: 8px; padding: 8px 10px; border-radius: 6px; cursor: pointer; font-size: 13px; }
.ppm-opt:hover { background: var(--bl-bg-hover); }
.ppm-opt.is-on { background: var(--bl-primary-soft); }
.ppm-opt-name { flex: 0 1 auto; min-width: 0; }
.ppm-opt-code { flex: 1; min-width: 0; font-size: 11px; }
.ppm-dt { flex-shrink: 0; font-size: 11px; }
.ppm-empty { padding: 24px; text-align: center; color: var(--bl-text-3); font-size: 12px; }
.ppm-ft { display: flex; align-items: center; gap: 8px; padding: 10px 16px; border-top: 1px solid var(--bl-divider); }
.ppm-ft-tip { font-size: 11.5px; }
</style>
