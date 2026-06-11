<template>
  <div class="ixd-mask" @click.self="$emit('close')">
    <aside class="ixd-drawer">
      <!-- 头部 -->
      <div class="ixd-hd">
        <span class="ixd-ic" :style="{ background:(color)+'1f', color }" v-html="BL.icon(icon, 20, color)"></span>
        <div class="ixd-hd-text">
          <div class="ixd-hd-title bl-truncate">{{ title }}</div>
          <div class="ixd-hd-sub bl-truncate">{{ subtitle }}</div>
        </div>
        <button class="ixd-x" @click="$emit('close')" v-html="BL.icon('x', 16)"></button>
      </div>

      <div class="ixd-body">
        <!-- 实例模式 -->
        <template v-if="mode==='instance'">
          <div class="ixd-sec">基础信息</div>
          <div class="ixd-fields">
            <div class="ixd-field">
              <span class="ixd-f-label">编码</span>
              <span class="ixd-f-val bl-mono">{{ row.code }}</span>
            </div>
            <div v-for="c in columns" :key="c.field" class="ixd-field">
              <span class="ixd-f-label">
                <span class="ixd-f-ic" v-html="BL.icon(typeIcon(c.dataType), 11, 'var(--bl-text-3)')"></span>
                {{ c.label }}
              </span>
              <span class="ixd-f-val">{{ fmt(row[c.field], c.dataType) }}</span>
            </div>
          </div>

          <div class="ixd-sec">关联对象类型 <span class="ixd-sec-n">{{ links.length }}</span></div>
          <div v-if="!links.length" class="ixd-empty">暂无关联</div>
          <div v-for="l in links" :key="l.linkId+l.targetClassId" class="ixd-link" @click="$emit('explore-type', l.targetClassId)">
            <span class="ixd-link-ic" :style="{ background:(l.targetColor||'#86909c')+'1f', color:l.targetColor||'#86909c' }"
                  v-html="BL.icon(l.targetIcon||'cube', 13, l.targetColor||'#86909c')"></span>
            <div class="bl-grow" style="min-width:0">
              <div class="bl-truncate" style="font-size:12.5px;font-weight:500">{{ l.targetClassName }}</div>
              <div class="bl-truncate bl-muted" style="font-size:11px">{{ l.linkLabel }}</div>
            </div>
            <span class="ixd-link-cnt">{{ l.count }}</span>
          </div>
        </template>

        <!-- 对象类型模式 -->
        <template v-else>
          <div class="ixd-typestat">
            <div class="ixd-stat"><b>{{ type.instanceCount ?? '—' }}</b><span>实例</span></div>
            <div class="ixd-stat"><b>{{ type.propCount ?? columns.length }}</b><span>属性</span></div>
            <div class="ixd-stat"><b>{{ type.linkCount ?? links.length }}</b><span>关系</span></div>
          </div>
          <div class="ixd-sec">所属分类</div>
          <div class="ixd-fields">
            <div class="ixd-field"><span class="ixd-f-label">行业</span><span class="ixd-f-val">{{ type.industryLabel || '—' }}</span></div>
            <div class="ixd-field"><span class="ixd-f-label">领域</span><span class="ixd-f-val">{{ type.domainLabel || '—' }}</span></div>
            <div class="ixd-field"><span class="ixd-f-label">API 名</span><span class="ixd-f-val bl-mono">{{ type.api_name }}</span></div>
          </div>

          <div class="ixd-sec">属性 <span class="ixd-sec-n">{{ columns.length }}</span></div>
          <div v-for="c in columns" :key="c.field" class="ixd-prop">
            <span class="ixd-f-ic" v-html="BL.icon(typeIcon(c.dataType), 12, 'var(--bl-text-3)')"></span>
            <span class="bl-grow bl-truncate">{{ c.label }}</span>
            <span class="bl-muted bl-mono" style="font-size:11px">{{ c.dataType }}</span>
          </div>

          <div class="ixd-sec">关联对象类型 <span class="ixd-sec-n">{{ links.length }}</span></div>
          <div v-for="l in links" :key="l.linkId+l.targetClassId" class="ixd-link" @click="$emit('explore-type', l.targetClassId)">
            <span class="ixd-link-ic" :style="{ background:(l.targetColor||'#86909c')+'1f', color:l.targetColor||'#86909c' }"
                  v-html="BL.icon(l.targetIcon||'cube', 13, l.targetColor||'#86909c')"></span>
            <span class="bl-grow bl-truncate" style="font-size:12.5px">{{ l.targetClassName }}</span>
            <span class="ixd-link-cnt">{{ l.count }}</span>
          </div>
        </template>
      </div>

      <!-- 底部操作 -->
      <div class="ixd-ft">
        <button class="bl-btn bl-btn-primary bl-grow"
                @click="$emit('explore-type', mode==='instance' ? { classId: row.classId, title: row.title } : { classId: type.id })"
                v-html="iconText('externalLink', mode==='instance' ? '在探索中打开此类型' : '开始探索')"></button>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { BL } from '@/lib/bl.js'
import { instanceApi } from '@/api'

const props = defineProps({
  mode: { type: String, default: 'instance' },   // instance | type
  type: { type: Object, default: () => ({}) },    // 对象类型(两种模式都给基础信息)
  row:  { type: Object, default: () => ({}) }      // 实例(instance 模式)
})
defineEmits(['close', 'explore-type'])

function iconText(ic, t) { return `${BL.icon(ic, 12)}<span style="margin-left:4px">${t}</span>` }

const columns = ref([])
const links = ref([])

const isInstance = computed(() => props.mode === 'instance')
const title = computed(() => isInstance.value ? (props.row.title || props.row.code) : (props.type.display_name || props.type.api_name))
const subtitle = computed(() => isInstance.value ? `${props.type.display_name || props.row.className || ''} · ${props.row.code || ''}` : (props.type.api_name || ''))
const icon = computed(() => (isInstance.value ? props.row.icon : props.type.icon) || 'cube')
const color = computed(() => (isInstance.value ? props.row.color : props.type.color) || '#165DFF')

async function load() {
  const cid = isInstance.value ? props.row.classId : props.type.id
  if (!cid) return
  try {
    if (isInstance.value && props.row.id) {
      const d = await instanceApi.detail(cid, props.row.id)
      columns.value = d.columns || []
      links.value = d.links || []
    } else {
      const [cols, lk] = await Promise.all([instanceApi.columns(cid), instanceApi.links(cid)])
      columns.value = cols || []
      links.value = lk || []
    }
  } catch { columns.value = []; links.value = [] }
}

function typeIcon(dt) {
  switch (dt) {
    case 'int': case 'decimal': return 'hash'
    case 'date': case 'datetime': return 'calendar'
    case 'time': return 'clock'
    case 'boolean': return 'check'
    case 'enum': return 'list'
    default: return 'fileText'
  }
}
function fmt(v, dt) {
  if (v === null || v === undefined || v === '') return '—'
  if (typeof v === 'boolean') return v ? '是' : '否'
  if (dt === 'decimal' && typeof v === 'number') return v.toLocaleString(undefined, { maximumFractionDigits: 2 })
  return v
}

watch(() => [props.mode, props.row?.id, props.type?.id], load)
onMounted(load)
</script>

<style scoped>
.ixd-mask { position: fixed; inset: 0; z-index: 1010; display: flex; justify-content: flex-end; }
.ixd-drawer {
  width: 420px; max-width: 92vw; height: 100%;
  background: var(--bl-bg-1); border-left: 1px solid var(--bl-border);
  box-shadow: -8px 0 28px rgba(0,0,0,.12);
  display: flex; flex-direction: column;
  animation: ixd-in .18s ease;
}
@keyframes ixd-in { from { transform: translateX(20px); opacity: .4 } to { transform: none; opacity: 1 } }

.ixd-hd { display: flex; align-items: center; gap: 10px; padding: 14px 16px; border-bottom: 1px solid var(--bl-divider); }
.ixd-ic { width: 36px; height: 36px; border-radius: 8px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.ixd-hd-text { flex: 1; min-width: 0; }
.ixd-hd-title { font-size: 15px; font-weight: 600; color: var(--bl-text-1); }
.ixd-hd-sub { font-size: 12px; color: var(--bl-text-3); margin-top: 2px; }
.ixd-x { width: 30px; height: 30px; border: 0; background: transparent; border-radius: 6px; color: var(--bl-text-3); cursor: pointer; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.ixd-x:hover { background: var(--bl-bg-hover); color: var(--bl-text-1); }

.ixd-body { flex: 1; overflow: auto; padding: 4px 16px 16px; }
.ixd-sec {
  font-size: 12px; color: var(--bl-text-3); font-weight: 500;
  padding: 14px 0 6px; border-left: 3px solid var(--bl-primary); padding-left: 8px; margin: 12px -8px 6px;
  display: flex; align-items: center; gap: 6px;
}
.ixd-sec-n { background: var(--bl-bg-3,#f0f2f5); border-radius: 8px; padding: 0 6px; font-size: 10px; }
.ixd-fields { display: flex; flex-direction: column; gap: 1px; }
.ixd-field { display: flex; align-items: center; gap: 10px; padding: 7px 4px; border-bottom: 1px solid var(--bl-divider); font-size: 12.5px; }
.ixd-f-label { width: 110px; flex-shrink: 0; color: var(--bl-text-3); display: inline-flex; align-items: center; gap: 4px; }
.ixd-f-ic { display: inline-flex; }
.ixd-f-val { flex: 1; min-width: 0; color: var(--bl-text-1); word-break: break-all; }
.ixd-prop { display: flex; align-items: center; gap: 8px; padding: 6px 4px; font-size: 12.5px; border-bottom: 1px solid var(--bl-divider); }
.ixd-empty { font-size: 12px; color: var(--bl-text-3); padding: 6px 4px; }
.ixd-link { display: flex; align-items: center; gap: 8px; padding: 8px 6px; border-radius: 6px; cursor: pointer; }
.ixd-link:hover { background: var(--bl-bg-hover); }
.ixd-link-ic { width: 24px; height: 24px; border-radius: 6px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.ixd-link-cnt { font-size: 11px; color: var(--bl-text-3); background: var(--bl-bg-3,#f0f2f5); border-radius: 8px; padding: 1px 7px; }

.ixd-typestat { display: flex; gap: 8px; padding: 8px 0; }
.ixd-stat { flex: 1; text-align: center; padding: 12px; background: var(--bl-bg-2); border-radius: 8px; }
.ixd-stat b { display: block; font-size: 20px; color: var(--bl-text-1); }
.ixd-stat span { font-size: 11px; color: var(--bl-text-3); }

.ixd-ft { padding: 12px 16px; border-top: 1px solid var(--bl-divider); display: flex; }
</style>
