<!--
  事件时间轴(P3:事件域类型类真实渲染)。
  读该对象类型的事件类型类绑定(tcRenderApi.resolve → resolveEvent),真实落到时间轴/列表:
    event_type  → 业务类型分类颜色 + 标签(type_style_map)
    event_level → 严重等级配色 / 优先级排序 / 闪烁高亮(level_style_map + sort_by_priority)
    is_closed   → 已闭环降透明度 + 「默认过滤已闭环」开关
  事件为演示合成数据(真实接入时替换为事件实例查询)。
-->
<template>
  <div class="etl">
    <div class="etl-hd">
      <span class="etl-title">事件时间轴</span>
      <span class="etl-sub">按绑定的事件类型类真实渲染 · 演示数据</span>
      <span class="bl-grow"></span>
      <label v-if="ev.isClosed" class="etl-filter"><input type="checkbox" v-model="hideClosed" /> 过滤已闭环</label>
      <button class="bl-btn bl-btn-sm" @click="reload" v-html="iconRefresh"></button>
    </div>

    <div v-if="loading" class="etl-empty">加载中…</div>
    <div v-else-if="!hasConfig" class="etl-empty">
      该对象类型未绑定事件类型类。<br>
      到「属性 → 类型类」绑定 <b>event_type / event_level / is_closed</b> 后,此处按配置真实渲染时间轴。
    </div>
    <template v-else>
      <!-- 等级图例 -->
      <div v-if="levels.length" class="etl-legend">
        <span v-for="l in levels" :key="l.code" class="etl-lg">
          <span class="etl-dot" :class="l.blink && 'is-blink'" :style="{ background: l.color }"></span>{{ l.code }}<span v-if="ev.enableBadge" class="etl-badge" :style="{ background: l.color }">P{{ l.priority }}</span>
        </span>
      </div>
      <!-- 时间轴 -->
      <div class="etl-list">
        <div v-for="(e, i) in shownEvents" :key="i" class="etl-item" :style="{ opacity: e.closed ? closedOpacity : 1 }">
          <span class="etl-time">{{ e.time }}</span>
          <span class="etl-rail"><span class="etl-node" :class="e.blink && 'is-blink'" :style="{ background: e.levelColor || '#86909c' }"></span></span>
          <div class="etl-body">
            <div class="etl-line1">
              <span v-if="e.typeLabel" class="etl-type" :style="{ color: e.typeColor, background: (e.typeColor||'#888')+'1a', borderColor: e.typeColor }">{{ e.typeLabel }}</span>
              <span class="etl-name">{{ e.title }}</span>
              <span v-if="e.closed" class="etl-closed">已闭环</span>
            </div>
            <div class="etl-line2">等级 {{ e.level || '—' }}<span v-if="e.priority != null"> · 优先级 {{ e.priority }}</span></div>
          </div>
        </div>
        <div v-if="!shownEvents.length" class="etl-empty" style="padding:24px">无事件(可能已被「过滤已闭环」隐藏)</div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { tcRenderApi } from '@/api'
import { resolveEvent } from '@/lib/tcResolver.js'

const props = defineProps({ classId: { type: String, default: '' } })
const iconRefresh = `${BL.icon('refresh', 13)}<span style="margin-left:4px">刷新</span>`

const loading = ref(false)
const ev = ref({ typeStyleMap: null, levelStyleMap: null, enableBadge: false, sortByPriority: false, isClosed: null })
const hideClosed = ref(false)

const hasConfig = computed(() => !!(ev.value.typeStyleMap || ev.value.levelStyleMap || ev.value.isClosed))
const closedOpacity = computed(() => (ev.value.isClosed && ev.value.isClosed.closed_opacity != null) ? ev.value.isClosed.closed_opacity : 0.5)

const levels = computed(() => Object.entries(ev.value.levelStyleMap || {}).map(([code, v]) => ({ code, color: v.color, priority: v.priority, blink: !!v.blink })))
const types = computed(() => Object.entries(ev.value.typeStyleMap || {}).map(([code, v]) => ({ code, label: v.label, color: v.color })))

/* 合成演示事件:循环覆盖配置的类型/等级,展示各样式 */
const events = computed(() => {
  const ts = types.value, lv = levels.value
  const titles = ['设备温度越限', '通信中断告警', '定时巡检完成', '阀门异常关闭', '数据补录成功', '离线恢复']
  const out = titles.map((title, i) => {
    const t = ts.length ? ts[i % ts.length] : null
    const l = lv.length ? lv[i % lv.length] : null
    return {
      time: `08:${String(10 + i * 7).padStart(2, '0')}`,
      title,
      typeLabel: t ? t.label : '', typeColor: t ? t.color : '',
      level: l ? l.code : '', levelColor: l ? l.color : '', priority: l ? l.priority : null, blink: l ? l.blink : false,
      closed: i % 3 === 0
    }
  })
  if (ev.value.sortByPriority) out.sort((a, b) => (b.priority ?? -1) - (a.priority ?? -1))
  return out
})
const shownEvents = computed(() => hideClosed.value ? events.value.filter(e => !e.closed) : events.value)

async function reload() {
  if (!props.classId) { ev.value = { typeStyleMap: null, levelStyleMap: null, isClosed: null }; return }
  loading.value = true
  try {
    const payload = await tcRenderApi.resolve({ classId: props.classId })
    ev.value = resolveEvent(payload)
    hideClosed.value = !!(ev.value.isClosed && ev.value.isClosed.default_filter_closed)
  } catch { ev.value = { typeStyleMap: null, levelStyleMap: null, isClosed: null } }
  finally { loading.value = false }
}
watch(() => props.classId, reload, { immediate: true })
</script>

<style scoped>
.etl { display: flex; flex-direction: column; height: 100%; min-height: 0; padding: 4px 2px; }
.etl-hd { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.etl-title { font-size: 14px; font-weight: 600; color: var(--bl-text-1); }
.etl-sub { font-size: 12px; color: var(--bl-text-3); }
.etl-filter { font-size: 12.5px; color: var(--bl-text-2); display: inline-flex; align-items: center; gap: 4px; cursor: pointer; }
.etl-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; font-size: 13px; color: var(--bl-text-3); line-height: 1.9; padding: 40px; }
.etl-legend { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 12px; }
.etl-lg { display: inline-flex; align-items: center; gap: 5px; font-size: 12px; color: var(--bl-text-2); }
.etl-dot, .etl-node { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.etl-badge { font-size: 10px; color: #fff; border-radius: 8px; padding: 0 5px; margin-left: 2px; }
.etl-list { flex: 1; overflow: auto; }
.etl-item { display: flex; gap: 10px; padding: 8px 0; }
.etl-time { flex-shrink: 0; width: 46px; font-size: 12px; color: var(--bl-text-3); font-variant-numeric: tabular-nums; padding-top: 2px; }
.etl-rail { position: relative; width: 12px; flex-shrink: 0; display: flex; justify-content: center; }
.etl-rail::before { content: ''; position: absolute; top: 0; bottom: -8px; width: 2px; background: var(--bl-divider); }
.etl-node { position: relative; z-index: 1; margin-top: 4px; box-shadow: 0 0 0 3px var(--bl-bg-1); }
.etl-body { flex: 1; min-width: 0; }
.etl-line1 { display: flex; align-items: center; gap: 8px; }
.etl-type { font-size: 10.5px; border: 1px solid; border-radius: 3px; padding: 0 6px; }
.etl-name { font-size: 13px; color: var(--bl-text-1); font-weight: 500; }
.etl-closed { font-size: 10.5px; color: var(--bl-text-3); background: var(--bl-bg-2); border-radius: 3px; padding: 0 5px; }
.etl-line2 { font-size: 11.5px; color: var(--bl-text-3); margin-top: 2px; }
.is-blink { animation: etl-blink 1s steps(2) infinite; }
@keyframes etl-blink { 50% { opacity: .35 } }
</style>
