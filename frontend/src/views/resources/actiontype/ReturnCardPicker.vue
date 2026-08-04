<template>
  <Teleport to="body">
    <div v-if="open" class="rcp-mask" @click.self="close">
      <div class="rcp-modal">
        <div class="rcp-hd">
          <span v-html="BL.icon('box', 14)"></span>
          <span style="margin-left:6px">返回属性 · 下拉候选卡片</span>
          <span class="bl-muted rcp-sub">左侧选属性, 右侧挑呈现模版</span>
          <span style="flex:1"></span>
          <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="close" v-html="BL.icon('x', 14)"></button>
        </div>

        <div class="rcp-body">
          <!-- 左: 选指标属性 -->
          <div class="rcp-col rcp-left">
            <div class="rcp-col-hd">
              <span>指标属性</span>
              <span style="flex:1"></span>
              <span class="bl-muted rcp-cnt">已选 {{ codes.length }} 项</span>
              <button v-if="codes.length" class="rcp-clear" @click="codes = []">清空</button>
            </div>
            <div class="rcp-search">
              <span class="bl-muted" v-html="BL.icon('search', 12)"></span>
              <input class="bl-input bl-input-sm" v-model="kw" placeholder="搜索属性 (名称 / 编码)" />
            </div>
            <div class="rcp-list">
              <button v-for="p in filtered" :key="p.value" :class="['rcp-item', isOn(p.value) && 'is-on']" @click="toggle(p.value)">
                <span class="rcp-ck" v-html="isOn(p.value) ? BL.icon('check', 10, '#fff') : ''"></span>
                <span class="bl-truncate" style="flex:1">{{ p.name || p.label }}</span>
                <span class="bl-mono bl-muted" style="font-size:11px">{{ p.value }}</span>
                <span v-if="p.status === 0" class="rcp-off">停用</span>
              </button>
              <div v-if="!filtered.length" class="bl-muted rcp-tip">{{ kw ? '无匹配属性' : '该对象暂无属性' }}</div>
            </div>
          </div>

          <!-- 右: 呈现模版备选方案 -->
          <div class="rcp-col">
            <div class="rcp-col-hd"><span>呈现模版</span><span class="bl-muted" style="font-weight:400;margin-left:6px">选一种, 下方为实时效果</span></div>
            <div class="rcp-schemes">
              <button v-for="s in SCHEMES" :key="s.key" :class="['rcp-scheme', scheme === s.key && 'is-on']" @click="scheme = s.key">
                <div class="rcp-scheme-hd">
                  <span class="rcp-radio" :class="{ 'is-on': scheme === s.key }"></span>
                  <span class="rcp-scheme-name">{{ s.label }}</span>
                  <span class="bl-muted rcp-scheme-desc">{{ s.desc }}</span>
                </div>
                <ReturnCard :items="items" :scheme="s.key" compact />
              </button>
            </div>

            <!-- 顺序 = 槽位, 所以给上下移 -->
            <div class="rcp-col-hd" style="margin-top:4px"><span>属性顺序</span><span class="bl-muted" style="font-weight:400;margin-left:6px">拖动排序 · {{ orderHint }}</span></div>
            <div class="rcp-order">
              <div v-for="(c, i) in codes" :key="c"
                   class="rcp-ord" draggable="true"
                   :class="{ 'is-dragging': dragIdx === i, 'is-over': overIdx === i && dragIdx !== null && dragIdx !== i }"
                   @dragstart="onDragStart(i, $event)" @dragover.prevent="overIdx = i" @drop="onDrop(i)"
                   @dragend="dragIdx = null; overIdx = null">
                <span class="rcp-grip" v-html="BL.icon('grip', 12)"></span>
                <span class="rcp-ord-role" :class="roleClass(i)">{{ roleLabel(i) }}</span>
                <span class="bl-truncate" style="flex:1">{{ nameOf(c) }}</span>
                <span class="bl-mono bl-muted rcp-ord-code bl-truncate">{{ c }}</span>
                <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="上移" :disabled="i === 0" @click="move(i, -1)" v-html="BL.icon('chevronUp', 11)"></button>
                <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="下移" :disabled="i === codes.length - 1" @click="move(i, 1)" v-html="BL.icon('chevronDown', 11)"></button>
                <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="移出" @click="toggle(c)" v-html="BL.icon('x', 11)"></button>
              </div>
              <div v-if="!codes.length" class="bl-muted rcp-tip">请先在左侧选择属性</div>
            </div>
          </div>
        </div>

        <div class="rcp-ft">
          <span class="bl-muted" style="font-size:12px">运行时下拉的每一项都按该模版渲染</span>
          <span style="flex:1"></span>
          <button class="bl-btn bl-btn-sm" @click="close">取消</button>
          <button class="bl-btn bl-btn-primary bl-btn-sm" @click="confirm">确定</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import ReturnCard from './ReturnCard.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  propOptions: { type: Array, default: () => [] },   // [{ value, label, name, status }]
  propCodes: { type: Array, default: () => [] },     // 已选属性编码 (有序)
  cardScheme: { type: String, default: 'title_sub' },
})
const emit = defineEmits(['update:open', 'confirm'])

const SCHEMES = [
  { key: 'title_sub',      label: '标题 + 副标题',        desc: '第 1 个作标题, 其余拼成副标题' },
  { key: 'title_tags',     label: '标题 + 标签',          desc: '第 1 个作标题, 其余每个一个标签' },
  { key: 'title_sub_tags', label: '标题 + 副标题 + 标签', desc: '第 1 / 第 2 个作两行, 其余作标签' },
  { key: 'inline',         label: '单行拼接',             desc: '全部属性按顺序用「·」连成一行' },
]
const kw = ref('')
const codes = ref([])
const scheme = ref('title_sub')

const filtered = computed(() => {
  const q = kw.value.trim().toLowerCase()
  if (!q) return props.propOptions
  return props.propOptions.filter(p => `${p.name || p.label} ${p.value}`.toLowerCase().includes(q))
})
const nameOf = c => props.propOptions.find(p => p.value === c)?.name || c
const items = computed(() => codes.value.map(c => ({ code: c, name: nameOf(c) })))
const orderHint = computed(() => scheme.value === 'inline' ? '决定拼接先后' : '第 1 个作标题, 依次向下')

function isOn(c) { return codes.value.includes(c) }
function toggle(c) {
  const i = codes.value.indexOf(c)
  i >= 0 ? codes.value.splice(i, 1) : codes.value.push(c)
}
function move(i, d) {
  const j = i + d
  if (j < 0 || j >= codes.value.length) return
  const a = codes.value
  ;[a[i], a[j]] = [a[j], a[i]]
}
/* 拖拽排序: 与规则卡片/表单字段同一套写法 (draggable + 插到目标位) */
const dragIdx = ref(null)
const overIdx = ref(null)
function onDragStart(i, ev) { dragIdx.value = i; if (ev?.dataTransfer) ev.dataTransfer.effectAllowed = 'move' }
function onDrop(target) {
  const from = dragIdx.value
  dragIdx.value = null; overIdx.value = null
  if (from === null || from === target) return
  const [it] = codes.value.splice(from, 1)
  codes.value.splice(target, 0, it)
}
function roleLabel(i) {
  if (scheme.value === 'inline') return `第 ${i + 1} 段`
  if (i === 0) return '主标题'
  if (scheme.value === 'title_tags') return '标签'
  if (scheme.value === 'title_sub') return '副标题'
  return i === 1 ? '副标题' : '标签'
}
function roleClass(i) {
  const r = roleLabel(i)
  return r === '主标题' ? 'is-title' : (r === '副标题' ? 'is-sub' : 'is-tag')
}
function confirm() {
  emit('confirm', { codes: codes.value.slice(), scheme: scheme.value })
  close()
}
function close() { emit('update:open', false) }

watch(() => props.open, v => {
  if (!v) return
  kw.value = ''
  codes.value = (props.propCodes || []).slice()
  scheme.value = props.cardScheme || 'title_sub'
})
</script>

<style scoped>
.rcp-mask { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: center; justify-content: center; z-index: 1300; }
.rcp-modal { width: 1120px; max-width: 94vw; height: 680px; max-height: 90vh; background: var(--bl-bg-1); border-radius: 12px;
  box-shadow: 0 16px 48px rgba(0,0,0,.3); display: flex; flex-direction: column; overflow: hidden; }
.rcp-hd { display: flex; align-items: center; padding: 14px 18px; font-size: 14px; font-weight: 600; border-bottom: 1px solid var(--bl-divider); }
.rcp-sub { font-size: 12px; font-weight: 400; margin-left: 10px; }
.rcp-body { flex: 1; min-height: 0; display: grid; grid-template-columns: 360px minmax(0, 1fr); }
.rcp-col { display: flex; flex-direction: column; min-width: 0; min-height: 0; }
.rcp-left { border-right: 1px solid var(--bl-divider); }
.rcp-col-hd { display: flex; align-items: center; gap: 6px; font-size: 12.5px; font-weight: 600; color: var(--bl-text-2); padding: 12px 18px 8px; }
.rcp-cnt { font-size: 11.5px; font-weight: 400; }
.rcp-clear { border: 0; background: transparent; color: var(--bl-primary); font-size: 11.5px; cursor: pointer; padding: 0; }
.rcp-clear:hover { text-decoration: underline; }
.rcp-search { display: flex; align-items: center; gap: 6px; padding: 0 16px 10px; }
.rcp-search .bl-input { flex: 1; }
.rcp-list { flex: 1; overflow-y: auto; padding: 0 12px 12px; }
.rcp-item { width: 100%; display: flex; align-items: center; gap: 8px; padding: 7px 10px; background: transparent; border: 0;
  border-radius: 6px; font-size: 13px; color: var(--bl-text-1); cursor: pointer; text-align: left; }
.rcp-item:hover { background: var(--bl-bg-hover); }
.rcp-item.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 600; }
.rcp-ck { width: 15px; height: 15px; flex-shrink: 0; border: 1px solid var(--bl-border); border-radius: 3px;
  display: inline-flex; align-items: center; justify-content: center; background: var(--bl-bg-1); }
.rcp-item.is-on .rcp-ck { background: var(--bl-primary); border-color: var(--bl-primary); }
.rcp-off { flex-shrink: 0; font-size: 10.5px; padding: 1px 5px; border-radius: 4px; background: var(--bl-bg-2); color: var(--bl-text-3); }
.rcp-tip { padding: 12px; font-size: 12px; text-align: center; }
/* 模版方案: 2 列卡片, 说明换行不挤掉预览 */
.rcp-schemes { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; padding: 0 18px 4px; }
.rcp-scheme { display: flex; flex-direction: column; gap: 8px; padding: 10px 12px; text-align: left; cursor: pointer; min-width: 0;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; }
.rcp-scheme:hover { border-color: var(--bl-primary); }
.rcp-scheme.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.rcp-scheme-hd { display: flex; align-items: flex-start; gap: 8px; min-width: 0; flex-wrap: wrap; }
.rcp-scheme-name { font-size: 13px; font-weight: 600; color: var(--bl-text-1); flex-shrink: 0; }
.rcp-scheme-desc { font-size: 11px; line-height: 1.45; flex: 1 1 100%; min-width: 0; }
.rcp-radio { width: 13px; height: 13px; border-radius: 50%; border: 1px solid var(--bl-border-strong); flex-shrink: 0; margin-top: 3px; }
.rcp-radio.is-on { border: 4px solid var(--bl-primary); }
/* 属性顺序 */
.rcp-order { flex: 1; overflow-y: auto; padding: 0 18px 14px; display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr)); grid-auto-rows: max-content; gap: 6px; align-content: start; }
.rcp-ord { display: flex; align-items: center; gap: 6px; padding: 5px 8px; border-radius: 6px; background: var(--bl-bg-2);
  font-size: 12.5px; min-width: 0; border: 1px solid transparent; }
.rcp-ord.is-dragging { opacity: .45; }
.rcp-ord.is-over { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.rcp-grip { flex-shrink: 0; color: var(--bl-text-3); display: inline-flex; cursor: grab; }
.rcp-ord:active .rcp-grip { cursor: grabbing; }
.rcp-ord-code { font-size: 11px; flex-shrink: 0; }
.rcp-ord-role { flex-shrink: 0; font-size: 10.5px; padding: 1px 6px; border-radius: 4px; }
.rcp-ord-role.is-title { background: var(--bl-primary-soft); color: var(--bl-primary); }
.rcp-ord-role.is-sub { background: var(--bl-bg-hover); color: var(--bl-text-2); }
.rcp-ord-role.is-tag { background: color-mix(in srgb, #00b42a 12%, transparent); color: #00b42a; }
.rcp-ft { display: flex; align-items: center; gap: 8px; padding: 12px 18px; border-top: 1px solid var(--bl-divider); }
</style>
