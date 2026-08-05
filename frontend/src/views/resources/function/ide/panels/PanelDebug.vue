<template>
  <div class="idep-debug">
    <!-- 调试控制条 -->
    <div class="idep-dbg-bar">
      <button class="idep-icon-btn" :disabled="!dbg.canStep.value" title="继续 (F5)"
              @click="dbg.doContinue" v-html="BL.icon('play', 13)"></button>
      <button class="idep-icon-btn" :disabled="!dbg.canStep.value" title="逐过程 (F10)"
              @click="dbg.stepOver" v-html="BL.icon('arrowRight', 13)"></button>
      <button class="idep-icon-btn" :disabled="!dbg.canStep.value" title="单步进入 (F11)"
              @click="dbg.stepIn" v-html="BL.icon('arrowDown', 13)"></button>
      <button class="idep-icon-btn" :disabled="!dbg.canStep.value" title="单步跳出 (Shift+F11)"
              @click="dbg.stepOut" v-html="BL.icon('arrowUp', 13)"></button>
      <button class="idep-icon-btn is-danger" :disabled="!dbg.debugging.value" title="停止调试 (Shift+F5)"
              @click="dbg.stop" v-html="BL.icon('stop', 13)"></button>
      <span class="idep-dbg-state">
        {{ dbg.debugging.value ? (dbg.paused.value ? `已暂停 · ${dbg.stoppedReason.value}` : '运行中') : '未启动' }}
      </span>
    </div>

    <div class="idep-body">
      <!-- 变量 -->
      <div class="idep-group" @click="toggle('vars')">
        <span class="idep-group-chev" :class="open.has('vars') && 'is-open'" v-html="BL.icon('chevronRight', 10)"></span>
        变量<span class="idep-group-n">{{ dbg.variables.value.length }}</span>
      </div>
      <template v-if="open.has('vars')">
        <div v-if="!dbg.paused.value" class="idep-empty" style="padding:6px 12px">暂停在断点处时才能查看变量</div>
        <template v-else>
          <div v-for="(v, i) in dbg.variables.value" :key="i">
            <div class="idep-var" @click="dbg.expandVariable(v)">
              <span class="idep-var-chev" v-if="v.variablesReference" :class="v.expanded && 'is-open'"
                    v-html="BL.icon('chevronRight', 9)"></span>
              <span v-else class="idep-var-chev"></span>
              <span class="idep-var-name">{{ v.name }}</span>
              <span class="idep-var-val" :title="v.value">{{ v.value }}</span>
            </div>
            <div v-if="v.expanded" class="idep-var-kids">
              <div v-for="(c, ci) in (v.children || [])" :key="ci" class="idep-var">
                <span class="idep-var-chev"></span>
                <span class="idep-var-name">{{ c.name }}</span>
                <span class="idep-var-val" :title="c.value">{{ c.value }}</span>
              </div>
            </div>
          </div>
          <div v-if="!dbg.variables.value.length" class="idep-empty" style="padding:6px 12px">无局部变量</div>
        </template>
      </template>

      <!-- 监视 -->
      <div class="idep-group" @click="toggle('watch')">
        <span class="idep-group-chev" :class="open.has('watch') && 'is-open'" v-html="BL.icon('chevronRight', 10)"></span>
        监视<span class="idep-group-n">{{ dbg.watches.value.length }}</span>
      </div>
      <template v-if="open.has('watch')">
        <div v-for="(w, i) in dbg.watches.value" :key="i" class="idep-var">
          <span class="idep-var-chev"></span>
          <span class="idep-var-name">{{ w.expression }}</span>
          <span class="idep-var-val" :class="w.error && 'is-err'" :title="w.error || w.value">{{ w.error || w.value }}</span>
          <button class="idep-row-act" title="移除" @click.stop="dbg.removeWatch(i)" v-html="BL.icon('x', 10)"></button>
        </div>
        <div class="idep-watch-add">
          <input class="idep-input is-plain" v-model="watchExpr" placeholder="添加监视表达式" @keydown.enter="submitWatch" />
        </div>
      </template>

      <!-- 调用栈 -->
      <div class="idep-group" @click="toggle('stack')">
        <span class="idep-group-chev" :class="open.has('stack') && 'is-open'" v-html="BL.icon('chevronRight', 10)"></span>
        调用栈<span class="idep-group-n">{{ dbg.frames.value.length }}</span>
      </div>
      <template v-if="open.has('stack')">
        <div v-for="f in dbg.frames.value" :key="f.id"
             :class="['idep-row', dbg.activeFrameId.value === f.id && 'is-on']"
             :title="f.source?.path || ''" @click="onFrame(f)">
          <span class="idep-row-name">{{ f.name }}</span>
          <span class="idep-row-sub">{{ baseName(f.source?.path) }}:{{ f.line }}</span>
        </div>
        <div v-if="!dbg.frames.value.length" class="idep-empty" style="padding:6px 12px">未暂停</div>
      </template>

      <!-- 断点 -->
      <div class="idep-group" @click="toggle('bps')">
        <span class="idep-group-chev" :class="open.has('bps') && 'is-open'" v-html="BL.icon('chevronRight', 10)"></span>
        断点<span class="idep-group-n">{{ bpCount }}</span>
      </div>
      <template v-if="open.has('bps')">
        <template v-for="(list, path) in dbg.breakpoints.value" :key="path">
          <div v-for="b in list" :key="path + b.line" class="idep-bp" @click="$emit('goto', { path, line: b.line })">
            <input class="idep-bp-ck" type="checkbox" :checked="b.enabled !== false"
                   :title="b.enabled === false ? '已禁用' : '已启用'"
                   @click.stop @change="dbg.toggleEnabled(path, b.line)" />
            <span class="idep-bp-dot" :class="dotClass(path, b)"></span>
            <span class="idep-bp-main">
              <span class="idep-bp-line">
                <span class="idep-row-name">{{ baseName(path) }}</span>
                <span class="idep-row-sub">第 {{ b.line }} 行</span>
              </span>
              <!-- 条件类断点把表达式摆出来, 否则列表里几个断点长得一模一样 -->
              <span v-if="kindOf(b)" class="idep-bp-cond">{{ kindOf(b) }}</span>
            </span>
            <button class="idep-row-act" title="移除断点" @click.stop="dbg.removeBreakpoint(path, b.line)"
                    v-html="BL.icon('x', 10)"></button>
          </div>
        </template>
        <div v-if="!bpCount" class="idep-empty" style="padding:6px 12px">
          点击编辑器行号左侧的空白处打断点;条件断点 / 命中次数 / 日志点在「运行」菜单里新建。
        </div>
        <div v-else class="idep-watch-add">
          <button class="idep-btn" @click="dbg.setAllEnabled(true)">全部启用</button>
          <button class="idep-btn" @click="dbg.setAllEnabled(false)">全部禁用</button>
          <button class="idep-btn" @click="dbg.clearAllBreakpoints">全部删除</button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
/**
 * 运行和调试面板 (文档 模块4「调试侧边面板」)
 * 四个区:变量 / 监视 / 调用栈 / 断点,外加顶部的执行控制条。
 * 所有数据来自 useFnDebug 的 DAP 会话,未暂停时给明确提示而不是空白。
 */
import { ref, computed } from 'vue'
import { BL } from '@/lib/bl.js'

const props = defineProps({
  /** useFnDebug() 的返回值 */
  dbg: { type: Object, required: true }
})
const emit = defineEmits(['goto'])

const open = ref(new Set(['vars', 'stack', 'bps']))
const watchExpr = ref('')

function toggle(k) {
  const s = new Set(open.value)
  s.has(k) ? s.delete(k) : s.add(k)
  open.value = s
}
const bpCount = computed(() =>
  Object.values(props.dbg.breakpoints.value || {}).reduce((n, arr) => n + arr.length, 0))

/** 断点圆点的形态:禁用 / 未校验 / 日志点 / 条件 */
function dotClass(path, b) {
  if (b.enabled === false) return 'is-off'
  if (b.logMessage) return 'is-log'
  if (props.dbg.verified.value[`${path}:${b.line}`] === false) return 'is-unverified'
  if (b.condition || b.hitCondition) return 'is-cond'
  return ''
}
/** 列表里显示的条件摘要 */
function kindOf(b) {
  if (b.logMessage) return `日志:${b.logMessage}`
  if (b.condition && b.hitCondition) return `${b.condition} · 命中 ${b.hitCondition}`
  if (b.condition) return `条件:${b.condition}`
  if (b.hitCondition) return `命中次数:${b.hitCondition}`
  return ''
}

function submitWatch() {
  const e = watchExpr.value.trim()
  if (!e) return
  props.dbg.addWatch(e)
  watchExpr.value = ''
}
function onFrame(f) {
  props.dbg.selectFrame(f.id)
  if (f.source?.path) emit('goto', { path: f.source.path, line: f.line, absolute: true })
}
function baseName(p) { return String(p || '').replace(/\\/g, '/').split('/').pop() }
</script>

<style scoped>
.idep-debug { display: flex; flex-direction: column; min-height: 0; }
.idep-body { flex: 1; min-height: 0; overflow: auto; }
.idep-dbg-bar {
  display: flex; align-items: center; gap: 2px; padding: 4px 6px;
  border-bottom: 1px solid var(--ide-border);
}
.idep-dbg-state { margin-left: auto; font-size: 11px; color: var(--ide-text-dim); }
.idep-icon-btn.is-danger:hover:not(:disabled) { color: #f14c4c; }
.idep-icon-btn:disabled { opacity: .35; cursor: default; }

.idep-var {
  display: flex; align-items: center; gap: 4px;
  padding: 2px 8px; font-size: 11.5px; cursor: pointer;
  font-family: Consolas, Monaco, monospace;
}
.idep-var:hover { background: var(--ide-hover); }
.idep-var-chev {
  width: 11px; flex-shrink: 0; color: var(--ide-text-dim);
  display: inline-flex; transition: transform .12s;
}
.idep-var-chev.is-open { transform: rotate(90deg); }
.idep-var-name { color: #9cdcfe; flex-shrink: 0; max-width: 40%; overflow: hidden; text-overflow: ellipsis; }
.idep-var-val {
  flex: 1; min-width: 0; color: var(--ide-text);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.idep-var-val.is-err { color: #f14c4c; }
.idep-var-kids { padding-left: 14px; }
.idep-watch-add { padding: 6px 8px; display: flex; gap: 4px; flex-wrap: wrap; }
.idep-bp-dot {
  width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; background: #e51400;
}
.idep-bp-dot.is-unverified { background: transparent; border: 1px solid #848484; }
.idep-bp-dot.is-off { background: #848484; }
.idep-bp-dot.is-cond { box-shadow: inset 0 0 0 2px rgba(255, 255, 255, .85); }
.idep-bp-dot.is-log { background: #d18616; border-radius: 2px; transform: rotate(45deg); }

.idep-bp {
  display: flex; align-items: flex-start; gap: 6px;
  padding: 3px 8px; font-size: 12px; cursor: pointer; color: var(--ide-text);
}
.idep-bp:hover { background: var(--ide-hover); }
.idep-bp-ck { flex-shrink: 0; margin: 2px 0 0; cursor: pointer; }
.idep-bp-dot { margin-top: 4px; }
.idep-bp-main { flex: 1; min-width: 0; }
.idep-bp-line { display: flex; align-items: baseline; gap: 6px; }
.idep-bp-cond {
  display: block; font-size: 11px; color: var(--ide-text-dim);
  font-family: Consolas, Monaco, monospace;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.idep-bp .idep-row-act { margin-top: 2px; }
</style>
