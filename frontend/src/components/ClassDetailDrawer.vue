<template>
  <aside class="gr-drawer" :class="{ 'is-open': !!node }">
    <div v-if="node" class="gr-drawer-inner">
      <header class="gr-drawer-hd">
        <div class="gr-drawer-title">
          <div class="gr-drawer-cn">{{ node.label }}
            <span v-if="detail" class="bl-tag bl-tag-success" style="margin-left:8px;font-size:11px">{{ detail.status === 1 ? '启用' : '禁用' }}</span>
          </div>
          <div class="gr-drawer-en bl-mono">{{ node.apiName || node.id }}</div>
        </div>
        <button class="gr-drawer-close" title="关闭" @click="$emit('close')"><span v-html="BL.icon('x', 16)"></span></button>
      </header>
      <div class="gr-drawer-body">
        <div class="gr-section">
          <div class="gr-section-hd">基础信息</div>
          <div v-if="!detail" class="bl-muted gr-loading">加载中…</div>
          <template v-else>
            <div class="gr-kv"><span class="gr-kv-l">名称</span><span class="gr-kv-r">{{ detail.display_name || node.label }}</span></div>
            <div class="gr-kv"><span class="gr-kv-l">API</span><span class="gr-kv-r bl-mono">{{ detail.api_name }}</span></div>
            <div v-if="detail.rdfs_label" class="gr-kv"><span class="gr-kv-l">标准名</span><span class="gr-kv-r bl-mono">{{ detail.rdfs_label }}</span></div>
            <div v-if="detail.ns_code" class="gr-kv"><span class="gr-kv-l">命名空间</span><span class="gr-kv-r bl-mono">{{ detail.ns_code }}</span></div>
            <div v-if="detail.category_code" class="gr-kv"><span class="gr-kv-l">领域</span><span class="gr-kv-r bl-mono">{{ detail.category_code }}</span></div>
            <div class="gr-kv"><span class="gr-kv-l">父类</span><span class="gr-kv-r">{{ detail.parent_class_id || '—' }}</span></div>
            <div v-if="detail.rdfs_comment" class="gr-kv gr-kv-block"><div class="gr-kv-l">描述</div><div class="gr-kv-text bl-muted">{{ detail.rdfs_comment }}</div></div>
          </template>
        </div>
        <div v-if="detail" class="gr-section">
          <div class="gr-section-hd">标志位</div>
          <div class="gr-flags">
            <span class="gr-flag" :class="detail.is_thing && 'is-on'">顶层类: {{ detail.is_thing ? '是' : '否' }}</span>
            <span class="gr-flag" :class="detail.is_nothing && 'is-on'">底层类: {{ detail.is_nothing ? '是' : '否' }}</span>
            <span class="gr-flag" :class="detail.is_common && 'is-on'">公共类: {{ detail.is_common ? '是' : '否' }}</span>
            <span class="gr-flag" :class="detail.status === 1 && 'is-on'">{{ detail.status === 1 ? '启用' : '禁用' }}</span>
          </div>
        </div>
        <div class="gr-section">
          <div class="gr-section-hd">关联关系 ({{ relations.length }})</div>
          <ul class="gr-rels">
            <li v-for="(r, i) in relations" :key="'rel'+i">
              <span class="gr-rel-mark" :style="{ background: (RMAP[r.kind] || RMAP.link).color }"></span>
              <span>{{ (RMAP[r.kind] || RMAP.link).cn }}</span>
              <span class="bl-muted" style="margin-left:auto">{{ r.name }}</span>
            </li>
            <li v-if="!relations.length" class="bl-muted">无关联</li>
          </ul>
        </div>
        <button class="bl-btn bl-btn-text gr-edit-btn" @click="$emit('edit', node)">
          <span v-html="BL.icon('externalLink', 12)"></span><span style="margin-left:6px">在对象类型管理中编辑</span>
        </button>
      </div>
      <div class="gr-drawer-ft">
        <button class="bl-btn bl-btn-primary gr-explore-btn" @click="$emit('explore', node)">
          <span v-html="BL.icon('search', 13, '#fff')"></span><span style="margin-left:6px">开始探索</span>
        </button>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { resourceApi } from '@/api'

const props = defineProps({
  node: { type: Object, default: null },          // { id, label, apiName }
  relations: { type: Array, default: () => [] }    // [{ kind, name }] 关联关系(由调用方按图边传入)
})
defineEmits(['close', 'edit', 'explore'])

const RMAP = {
  link: { cn: '普通链接', color: '#6b7280' },
  sub: { cn: '父子类', color: '#3b82f6' },
  eq: { cn: '等价类', color: '#10b981' },
  union: { cn: '并集类', color: '#8b5cf6' },
  dis: { cn: '互斥不相交', color: '#ef4444' }
}

const detail = ref(null)
watch(() => props.node && props.node.id, async (id) => {
  detail.value = null
  if (!id) return
  try { const res = await resourceApi.classDetail(id); if (props.node && props.node.id === id) detail.value = res }
  catch { detail.value = null }
}, { immediate: true })
</script>

<style scoped>
.gr-drawer { position: absolute; top: 0; right: 0; bottom: 0; width: 320px; background: var(--bl-bg-1); border-left: 1px solid var(--bl-border); box-shadow: -4px 0 12px rgba(0,0,0,.06); transform: translateX(100%); transition: transform .25s ease; z-index: 10; display: flex; flex-direction: column; }
.gr-drawer.is-open { transform: translateX(0); }
.gr-drawer-inner { height: 100%; display: flex; flex-direction: column; }
.gr-drawer-hd { display: flex; align-items: flex-start; gap: 10px; padding: 14px 16px 12px; border-bottom: 1px solid var(--bl-divider); }
.gr-drawer-title { flex: 1; min-width: 0; }
.gr-drawer-close { flex-shrink: 0; width: 28px; height: 28px; border: 0; background: var(--bl-bg-2); border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; color: var(--bl-text-2); cursor: pointer; }
.gr-drawer-close:hover { background: color-mix(in srgb, #f53f3f 12%, var(--bl-bg-1)); color: #f53f3f; }
.gr-drawer-cn { font-size: 14px; font-weight: 700; color: var(--bl-text-1); }
.gr-drawer-en { font-size: 11px; color: var(--bl-text-3); margin-top: 2px; }
.gr-drawer-body { flex: 1; overflow-y: auto; padding: 12px 16px 16px; }
.gr-kv { display: flex; padding: 6px 0; border-bottom: 1px dashed var(--bl-border); font-size: 12px; }
.gr-kv-l { width: 70px; color: var(--bl-text-3); flex-shrink: 0; }
.gr-kv-r { flex: 1; color: var(--bl-text-1); word-break: break-all; }
.gr-kv-block { display: block; }
.gr-kv-block .gr-kv-l { width: auto; margin-bottom: 4px; font-weight: 500; }
.gr-kv-text { line-height: 1.55; margin-top: 4px; }
.gr-rels { list-style: none; margin: 4px 0 0; padding: 0; }
.gr-rels li { display: flex; align-items: center; gap: 8px; padding: 6px 0; border-bottom: 1px dashed var(--bl-border); font-size: 12px; }
.gr-rel-mark { width: 14px; height: 3px; border-radius: 2px; flex-shrink: 0; }
.gr-section { margin-top: 12px; padding-top: 8px; border-top: 1px solid var(--bl-divider); }
.gr-section:first-child { margin-top: 0; padding-top: 0; border-top: 0; }
.gr-section-hd { font-size: 12px; font-weight: 600; color: var(--bl-text-1); padding-left: 8px; border-left: 3px solid var(--bl-primary); margin-bottom: 6px; }
.gr-loading { padding: 8px 0; font-size: 12px; }
.gr-flags { display: flex; flex-wrap: wrap; gap: 6px; }
.gr-flag { display: inline-flex; align-items: center; padding: 3px 8px; background: var(--bl-bg-2); color: var(--bl-text-3); border-radius: 3px; font-size: 11px; }
.gr-flag.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.gr-drawer-ft { flex-shrink: 0; padding: 12px 16px; border-top: 1px solid var(--bl-divider); }
.gr-explore-btn { width: 100%; justify-content: center; }
.gr-edit-btn { width: 100%; margin-top: 14px; justify-content: center; }
</style>
