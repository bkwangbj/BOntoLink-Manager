<!--
  编辑分词参数弹窗(文档 2.6)。920×700 左右分栏:
    左=分词能力配置(说明条 + 常用/全量 + 核心分词器富下拉 + 附加能力多选卡)
    右=已选配置 + 分词效果示例 + 参数 JSON
  输出 value = { analyzer_name: [核心分词器, ...附加过滤器] }(数组顺序即管道执行顺序)。
  keyword 精确匹配模式下,所有附加能力自动置灰、已选清空。
-->
<template>
  <div class="acm-mask" @click.self="$emit('close')">
    <div class="acm">
      <header class="acm-hd"><span class="acm-title">编辑分词参数</span>
        <button class="acm-x" @click="$emit('close')" v-html="BL.icon('x', 16)"></button></header>

      <div class="acm-body">
        <!-- 左:配置 -->
        <div class="acm-left">
          <div class="acm-sec">分词能力配置</div>
          <div class="acm-note">核心分词必选一个,附加能力可多选;引擎按数组顺序组装分词管道</div>
          <div class="acm-mode">
            <button :class="mode==='common' && 'is-on'" @click="mode='common'">常用</button>
            <button :class="mode==='full' && 'is-on'" @click="mode='full'">全量</button>
          </div>

          <!-- 核心分词器(单选,富下拉) -->
          <div class="acm-sub">核心分词器</div>
          <div class="acm-dd" v-click-outside="()=>ddOpen=false">
            <button class="acm-dd-btn" @click="ddOpen=!ddOpen">
              <span>{{ coreItem ? coreItem.en + '(' + coreItem.cn + ')' : '请选择核心分词器' }}</span>
              <span v-html="BL.icon(ddOpen?'chevronUp':'chevronDown', 12)"></span>
            </button>
            <div v-if="ddOpen" class="acm-dd-menu">
              <template v-for="g in coreGroups" :key="g">
                <div v-if="mode==='full'" class="acm-dd-grp">{{ g }}分词</div>
                <div v-for="c in coreByGroup(g)" :key="c.en" :class="['acm-dd-item', core===c.en && 'is-on']" @click="pickCore(c.en)">
                  <div class="acm-dd-tt"><span class="bl-mono">{{ c.en }}</span><span class="acm-tag">{{ c.cn }}</span></div>
                  <div class="acm-dd-feat">{{ c.feat }}</div>
                  <div class="acm-dd-scene">适用场景:{{ c.scene }}</div>
                </div>
              </template>
            </div>
          </div>
          <div v-if="coreItem" class="acm-core-feat">{{ coreItem.feat }}<br><span class="acm-core-scene">适用场景:{{ coreItem.scene }}</span></div>

          <!-- 附加能力(多选卡) -->
          <div class="acm-sub">附加能力</div>
          <div v-if="keywordMode" class="acm-kw-tip">精确匹配模式(keyword)下不可叠加附加能力</div>
          <div class="acm-cards" :class="keywordMode && 'is-disabled'">
            <template v-for="g in filterGroups" :key="g">
              <div v-if="mode==='full'" class="acm-cards-grp">{{ g }}类</div>
              <label v-for="f in filterByGroup(g)" :key="f.en" class="acm-card" :class="[filters.includes(f.en) && 'is-on', keywordMode && 'is-dis']">
                <input type="checkbox" :checked="filters.includes(f.en)" :disabled="keywordMode" @change="toggleFilter(f.en)" />
                <div class="acm-card-tt"><span class="bl-mono">{{ f.en }}</span><span class="acm-tag">{{ f.cn }}</span></div>
                <div class="acm-card-feat">{{ f.feat }}</div>
              </label>
            </template>
          </div>
        </div>

        <!-- 右:预览 -->
        <div class="acm-right">
          <div class="acm-sec">已选配置</div>
          <div class="acm-selbox">
            <div>核心分词: <b>{{ coreItem ? coreItem.en + '(' + coreItem.cn + ')' : '—' }}</b></div>
            <div>附加能力: <span v-if="selFilters.length">{{ selFilters.map(f=>f.cn+'('+f.en+')').join('、') }}</span><span v-else class="bl-muted">无</span></div>
          </div>

          <div class="acm-sec acm-sec2">分词效果示例</div>
          <div class="acm-prev">
            <div>示例文本:{{ SAMPLE_TEXT }}</div>
            <div class="acm-prev-r">拆分结果:{{ preview.base.join('、') }}</div>
            <div v-for="e in preview.extra" :key="e.label" class="acm-prev-x">{{ e.label }}:{{ e.words.join('、') }}</div>
            <div v-if="preview.stopped" class="acm-prev-x">已过滤:{{ preview.stopped }}</div>
          </div>

          <div class="acm-sec acm-sec2">参数 JSON</div>
          <CodeEditor :model-value="jsonText" disabled :rows="6" />
        </div>
      </div>

      <footer class="acm-ft"><span class="acm-ft-note">数组顺序为管道执行顺序,引擎侧按配置加载对应分词组件</span>
        <button class="bl-btn bl-btn-sm" @click="$emit('close')">取消</button>
        <button class="bl-btn bl-btn-sm bl-btn-primary" @click="save">保存</button></footer>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { BL } from '@/lib/bl.js'
import CodeEditor from '@/components/CodeEditor.vue'
import { CORES, FILTERS, byEn, CORE_GROUPS, FILTER_GROUPS, SAMPLE_TEXT, tokenize } from '@/lib/analyzers.js'

const props = defineProps({ value: { type: Object, default: () => ({}) } })
const emit = defineEmits(['save', 'close'])

function initArr() {
  let v = props.value
  if (typeof v === 'string' && v.trim()) { try { v = JSON.parse(v) } catch { v = {} } }
  const arr = Array.isArray(v?.analyzer_name) ? v.analyzer_name : []
  return arr
}
const arr0 = initArr()
const mode = ref('common')
const core = ref(arr0.find(x => byEn[x]?.role === 'core') || 'ik_max_word')
const filters = ref(arr0.filter(x => byEn[x]?.role === 'filter'))
const ddOpen = ref(false)

const coreItem = computed(() => byEn[core.value] || null)
const keywordMode = computed(() => core.value === 'keyword')
const selFilters = computed(() => filters.value.map(en => byEn[en]).filter(Boolean))
const analyzerName = computed(() => [core.value, ...filters.value])
const jsonText = computed(() => JSON.stringify({ analyzer_name: analyzerName.value }, null, 2))
const preview = computed(() => tokenize(core.value, filters.value))

const coreGroups = computed(() => mode.value === 'full' ? CORE_GROUPS : ['*'])
const filterGroups = computed(() => mode.value === 'full' ? FILTER_GROUPS : ['*'])
function coreByGroup(g) { return CORES.filter(c => (mode.value === 'common' ? c.scope === 'common' : c.group === g)) }
function filterByGroup(g) { return FILTERS.filter(f => (mode.value === 'common' ? f.scope === 'common' : f.group === g)) }

function pickCore(en) { core.value = en; if (en === 'keyword') filters.value = []; ddOpen.value = false }
function toggleFilter(en) {
  if (keywordMode.value) return
  filters.value = filters.value.includes(en) ? filters.value.filter(x => x !== en) : [...filters.value, en]
}
function save() { emit('save', { analyzer_name: analyzerName.value }) }

const vClickOutside = {
  mounted(el, b) { el.__h = e => { if (!el.contains(e.target)) b.value(e) }; setTimeout(() => document.addEventListener('mousedown', el.__h), 0) },
  unmounted(el) { document.removeEventListener('mousedown', el.__h) }
}
</script>

<style scoped>
.acm-mask { position: fixed; inset: 0; z-index: 1300; background: rgba(0,0,0,.32); display: flex; align-items: center; justify-content: center; }
.acm { width: 920px; max-width: 95vw; height: 700px; max-height: 92vh; background: var(--bl-bg-1); border-radius: 10px; box-shadow: 0 16px 48px rgba(0,0,0,.24); display: flex; flex-direction: column; overflow: hidden; }
.acm-hd { display: flex; align-items: center; padding: 14px 18px; border-bottom: 1px solid var(--bl-divider); }
.acm-title { flex: 1; font-size: 15px; font-weight: 600; color: var(--bl-text-1); }
.acm-x { width: 28px; height: 28px; border: 0; background: transparent; border-radius: 6px; color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.acm-x:hover { background: var(--bl-bg-hover); }
.acm-body { flex: 1; display: flex; min-height: 0; }
.acm-left { flex: 1; min-width: 0; padding: 14px 18px; overflow: auto; border-right: 1px solid var(--bl-divider); }
.acm-right { flex: 1; min-width: 0; padding: 14px 18px; overflow: auto; display: flex; flex-direction: column; }
.acm-sec { font-size: 14px; font-weight: 600; color: var(--bl-text-1); margin-bottom: 8px; }
.acm-sec2 { margin-top: 14px; }
.acm-note { font-size: 12px; color: #00875a; background: color-mix(in srgb, var(--bl-success) 14%, var(--bl-bg-1)); border-radius: 6px; padding: 7px 10px; margin-bottom: 10px; }
.acm-mode { display: inline-flex; border: 1px solid var(--bl-border); border-radius: 6px; overflow: hidden; margin-bottom: 12px; }
.acm-mode button { height: 28px; padding: 0 16px; border: 0; background: var(--bl-bg-1); color: var(--bl-text-2); cursor: pointer; font-size: 13px; }
.acm-mode button + button { border-left: 1px solid var(--bl-border); }
.acm-mode button.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.acm-sub { font-size: 13px; font-weight: 600; color: var(--bl-text-1); margin: 12px 0 6px; }
.acm-dd { position: relative; }
.acm-dd-btn { width: 100%; height: 34px; display: flex; align-items: center; justify-content: space-between; padding: 0 10px; border: 1px solid var(--bl-border); border-radius: 6px; background: var(--bl-bg-1); cursor: pointer; font-size: 13px; color: var(--bl-text-1); }
.acm-dd-btn:hover { border-color: var(--bl-primary-border); }
.acm-dd-menu { position: absolute; top: calc(100% + 4px); left: 0; right: 0; max-height: 320px; overflow: auto; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; box-shadow: 0 8px 28px rgba(0,0,0,.16); z-index: 5; padding: 4px; }
.acm-dd-grp { font-size: 11px; color: var(--bl-text-3); padding: 6px 8px 3px; position: sticky; top: -4px; background: var(--bl-bg-1); }
.acm-dd-item { padding: 7px 9px; border-radius: 6px; cursor: pointer; }
.acm-dd-item:hover { background: var(--bl-bg-hover); }
.acm-dd-item.is-on { background: var(--bl-primary-soft); }
.acm-dd-tt { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.acm-tag { font-size: 10.5px; color: var(--bl-primary); background: var(--bl-primary-soft); border-radius: 3px; padding: 0 5px; }
.acm-dd-feat { font-size: 11.5px; color: var(--bl-text-2); margin-top: 2px; }
.acm-dd-scene { font-size: 11px; color: var(--bl-text-3); margin-top: 1px; }
.acm-core-feat { font-size: 11.5px; color: var(--bl-text-3); margin-top: 6px; line-height: 1.6; }
.acm-core-scene { color: var(--bl-text-3); }
.acm-kw-tip { font-size: 11.5px; color: #ff7d00; background: #fff7e8; border-radius: 5px; padding: 5px 8px; margin-bottom: 6px; }
.acm-cards { display: flex; flex-direction: column; gap: 8px; }
.acm-cards.is-disabled { opacity: .55; }
.acm-cards-grp { font-size: 11px; color: var(--bl-text-3); margin-top: 6px; }
.acm-card { display: block; border: 1px solid var(--bl-border); border-radius: 8px; padding: 8px 10px; cursor: pointer; }
.acm-card:hover { border-color: var(--bl-primary-border); }
.acm-card.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.acm-card.is-dis { cursor: not-allowed; }
.acm-card input { margin-right: 6px; }
.acm-card-tt { display: inline-flex; align-items: center; gap: 8px; font-size: 13px; }
.acm-card-feat { font-size: 11.5px; color: var(--bl-text-2); margin-top: 3px; }
.acm-selbox { border: 1px solid var(--bl-border); border-radius: 8px; padding: 10px; font-size: 12.5px; color: var(--bl-text-2); display: flex; flex-direction: column; gap: 6px; background: var(--bl-bg-2); }
.acm-selbox b { color: var(--bl-primary); }
.acm-prev { border: 1px solid var(--bl-border); border-radius: 8px; padding: 10px; font-size: 12.5px; color: var(--bl-text-2); background: var(--bl-bg-2); line-height: 1.9; }
.acm-prev-r { color: var(--bl-text-1); }
.acm-prev-x { color: var(--bl-primary); }
.acm-json { flex: 1; min-height: 100px; margin: 0; padding: 10px 12px; background: var(--bl-bg-2); border: 1px solid var(--bl-border); border-radius: 8px; font-family: var(--bl-mono, ui-monospace, monospace); font-size: 12px; line-height: 1.6; white-space: pre-wrap; overflow: auto; }
.acm-ft { display: flex; align-items: center; gap: 10px; padding: 12px 18px; border-top: 1px solid var(--bl-divider); background: var(--bl-bg-2); }
.acm-ft-note { flex: 1; font-size: 11.5px; color: var(--bl-text-3); }
</style>
