<template>
  <Teleport to="body">
    <div v-if="open" class="dkp-mask" @click.self="close">
      <div class="dkp-modal">
        <div class="dkp-hd"><span>新建数据源</span><span style="flex:1"></span>
          <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="close" v-html="BL.icon('x', 14)"></button></div>
        <div class="dkp-body">
          <div class="dkp-tip">选择数据源形式,不同形式的配置项与详情页不同,创建后不可切换。</div>
          <button v-for="k in KINDS" :key="k.value" class="dkp-card" @click="pick(k.value)">
            <span class="dkp-ic" :style="{ background: k.color }" v-html="BL.icon(k.icon, 18, '#fff')"></span>
            <div class="dkp-txt">
              <div class="dkp-lbl">{{ k.label }}</div>
              <div class="dkp-desc">{{ k.desc }}</div>
            </div>
            <span class="dkp-chev" v-html="BL.icon('chevronRight', 14)"></span>
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { BL } from '@/lib/bl.js'

defineProps({ open: Boolean })
const emit = defineEmits(['update:open', 'pick'])

const KINDS = [
  { value: 'db', label: '数据库', icon: 'database', color: '#165DFF',
    desc: 'MySQL / PostgreSQL / Oracle / 达梦 / 人大金仓 等，通过 JDBC 连接库表' },
  { value: 'ext', label: '外部接口', icon: 'plug', color: '#00B42A',
    desc: 'REST API / Webhook / GraphQL，通过 HTTP 调用外部系统接口' },
]

function pick(v) { emit('pick', v); close() }
function close() { emit('update:open', false) }
</script>

<style scoped>
.dkp-mask { position: fixed; inset: 0; background: rgba(0,0,0,.45); backdrop-filter: blur(3px); z-index: 1350; display: flex; align-items: center; justify-content: center; }
.dkp-modal { width: 520px; max-width: 94vw; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 12px;
  box-shadow: 0 24px 56px rgba(0,0,0,.4); overflow: hidden; }
.dkp-hd { display: flex; align-items: center; padding: 13px 16px; font-size: 14px; font-weight: 600; border-bottom: 1px solid var(--bl-divider); }
.dkp-body { padding: 14px 16px 18px; display: flex; flex-direction: column; gap: 10px; }
.dkp-tip { font-size: 12px; color: var(--bl-text-3); line-height: 1.6; margin-bottom: 2px; }
.dkp-card { display: flex; align-items: center; gap: 12px; padding: 14px 16px; border: 1px solid var(--bl-border); border-radius: 10px;
  background: var(--bl-bg-1); cursor: pointer; text-align: left; }
.dkp-card:hover { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.dkp-ic { width: 36px; height: 36px; border-radius: 9px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.dkp-txt { flex: 1; min-width: 0; }
.dkp-lbl { font-size: 14px; font-weight: 600; color: var(--bl-text-1); }
.dkp-desc { font-size: 11.5px; color: var(--bl-text-3); margin-top: 3px; line-height: 1.5; }
.dkp-chev { color: var(--bl-text-3); display: inline-flex; flex-shrink: 0; }
</style>
