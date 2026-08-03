<template>
  <div class="apt">
    <!-- 数据源标题: 名称(编码), 只读, 由数据源配置页维护 -->
    <div class="apt-hd">
      <div class="apt-title bl-truncate" :title="`${ds.ds_name || ''}（${ds.ds_code || ''}）`">
        {{ ds.ds_name || '数据源' }}<span class="apt-title-code">（{{ ds.ds_code || '' }}）</span>
      </div>
    </div>

    <!-- 搜索 + 状态多选 + 新增 -->
    <div class="apt-search">
      <span class="apt-search-ic" v-html="BL.icon('search', 12)"></span>
      <input v-model="kw" placeholder="搜索目录或接口" />
    </div>
    <div class="apt-filter">
      <label v-for="s in API_STATUS" :key="s.value" class="apt-ck">
        <input type="checkbox" :checked="picked.includes(s.value)" @change="toggleStatus(s.value)" />
        <span>{{ s.label }}</span><span class="apt-ck-n">{{ countOf(s.value) }}</span>
      </label>
      <span style="flex:1"></span>
      <div class="apt-add-wrap">
        <button class="apt-add" title="新增" @click.stop="addOpen = !addOpen" v-html="BL.icon('plus', 14)"></button>
        <div v-if="addOpen" class="apt-add-menu">
          <button @click="emitAdd('group')"><span v-html="BL.icon('folder', 12)"></span>新增分组</button>
          <button @click="emitAdd('api')"><span v-html="BL.icon('plug', 12)"></span>新增接口</button>
        </div>
      </div>
    </div>

    <!-- 目录树: 分组 → 接口 两级 -->
    <div class="apt-tree">
      <template v-for="g in tree" :key="g.id">
        <div class="apt-group" @click="toggleFold(g.id)">
          <span class="apt-chev" :class="{ 'is-fold': folded.has(g.id) }" v-html="BL.icon('chevronDown', 11)"></span>
          <span class="apt-folder" v-html="BL.icon(folded.has(g.id) ? 'folder' : 'folderOpen', 13)"></span>
          <span class="bl-truncate">{{ g.group_name }}</span>
          <span class="apt-group-n">{{ g.items.length }}</span>
          <button v-if="g.id !== '0'" class="apt-gx" title="删除分组(组内接口移到未分组)"
                  @click.stop="$emit('remove-group', g)" v-html="BL.icon('trash2', 11)"></button>
        </div>
        <template v-if="!folded.has(g.id)">
          <div v-for="a in g.items" :key="a.id"
               :class="['apt-api', activeId === a.id && 'is-on']" @click="$emit('open', a)">
            <span class="apt-dot" :style="{ background: API_STATUS_META[a.api_status]?.color || '#86909c' }"></span>
            <span class="apt-method" :style="{ color: METHOD_COLOR[a.method] || '#86909c' }">{{ a.method }}</span>
            <span class="bl-truncate apt-api-name">{{ a.api_name }}</span>
            <button class="apt-gx" title="删除接口" @click.stop="$emit('remove-api', a)" v-html="BL.icon('trash2', 11)"></button>
          </div>
          <div v-if="!g.items.length" class="apt-empty-g">该分组暂无接口</div>
        </template>
      </template>
      <div v-if="!tree.length" class="apt-empty">没有匹配的目录或接口</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { BL } from '@/lib/bl.js'
import { API_STATUS, API_STATUS_META, METHOD_COLOR } from './apiModel.js'

const props = defineProps({
  ds: { type: Object, default: () => ({}) },
  groups: { type: Array, default: () => [] },
  apis: { type: Array, default: () => [] },
  activeId: { type: String, default: '' },
})
const emit = defineEmits(['open', 'add', 'remove-group', 'remove-api'])

const kw = ref('')
const picked = ref(API_STATUS.map(s => s.value))   // 默认全部勾选
const folded = ref(new Set())
const addOpen = ref(false)

function toggleStatus(v) {
  picked.value = picked.value.includes(v) ? picked.value.filter(x => x !== v) : [...picked.value, v]
}
function countOf(v) { return props.apis.filter(a => a.api_status === v).length }
function toggleFold(id) {
  const s = new Set(folded.value)
  s.has(id) ? s.delete(id) : s.add(id)
  folded.value = s
}
function emitAdd(kind) { addOpen.value = false; emit('add', kind) }

/* 未分组固定排在最后, 与真实分组一起展示 */
const tree = computed(() => {
  const k = kw.value.trim().toLowerCase()
  const hit = a => (!k || [a.api_name, a.api_code, a.api_path].filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
    && picked.value.includes(a.api_status)
  const groups = [...props.groups.map(g => ({ ...g, items: [] })), { id: '0', group_name: '未分组', items: [] }]
  const byId = Object.fromEntries(groups.map(g => [g.id, g]))
  props.apis.filter(hit).forEach(a => (byId[a.group_id] || byId['0']).items.push(a))
  /* 搜索时隐藏空分组, 免得一堆空文件夹 */
  return k ? groups.filter(g => g.items.length) : groups.filter(g => g.id !== '0' || g.items.length)
})

function onDocClick() { addOpen.value = false }
onMounted(() => document.addEventListener('click', onDocClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocClick))
</script>

<style scoped>
.apt { display: flex; flex-direction: column; height: 100%; min-height: 0; background: #252526; color: #ccc; }
.apt-hd { padding: 12px 14px 10px; border-bottom: 1px solid #333; }
.apt-title { font-size: 13.5px; font-weight: 600; color: #fff; }
.apt-title-code { font-weight: 400; color: #8b8b8b; font-size: 12px; }
.apt-search { display: flex; align-items: center; gap: 6px; padding: 8px 12px; border-bottom: 1px solid #333; }
.apt-search-ic { color: #8b8b8b; display: inline-flex; }
.apt-search input { flex: 1; min-width: 0; background: transparent; border: 0; outline: none; color: #ddd; font-size: 12.5px; }
.apt-search input::placeholder { color: #6b6b6b; }
.apt-filter { display: flex; align-items: center; gap: 12px; padding: 8px 12px; border-bottom: 1px solid #333; }
.apt-ck { display: inline-flex; align-items: center; gap: 5px; font-size: 12px; color: #bbb; cursor: pointer; }
.apt-ck-n { color: #7a7a7a; font-size: 11px; }
.apt-add-wrap { position: relative; }
.apt-add { display: inline-flex; align-items: center; justify-content: center; width: 24px; height: 24px; border: 0;
  border-radius: 5px; background: transparent; color: #bbb; cursor: pointer; }
.apt-add:hover { background: #3a3a3a; color: #fff; }
.apt-add-menu { position: absolute; right: 0; top: 28px; z-index: 20; min-width: 128px; background: #2d2d2d;
  border: 1px solid #3d3d3d; border-radius: 6px; padding: 4px; box-shadow: 0 8px 20px rgba(0,0,0,.45); }
.apt-add-menu button { display: flex; align-items: center; gap: 7px; width: 100%; padding: 7px 9px; border: 0;
  background: transparent; color: #ddd; font-size: 12.5px; cursor: pointer; border-radius: 4px; text-align: left; }
.apt-add-menu button:hover { background: #3b82f6; color: #fff; }
.apt-tree { flex: 1; overflow-y: auto; padding: 6px 0 12px; }
.apt-group { display: flex; align-items: center; gap: 6px; padding: 6px 12px; font-size: 12.5px; color: #ddd; cursor: pointer; }
.apt-group:hover { background: #2d2d2d; }
.apt-group:hover .apt-gx { opacity: 1; }
.apt-chev { display: inline-flex; color: #8b8b8b; transition: transform .12s; flex-shrink: 0; }
.apt-chev.is-fold { transform: rotate(-90deg); }
.apt-folder { display: inline-flex; color: #d7a860; flex-shrink: 0; }
.apt-group-n { color: #6b6b6b; font-size: 11px; }
.apt-api { display: flex; align-items: center; gap: 6px; padding: 6px 12px 6px 30px; font-size: 12.5px; color: #bbb;
  cursor: pointer; border-left: 2px solid transparent; }
.apt-api:hover { background: #2d2d2d; }
.apt-api:hover .apt-gx { opacity: 1; }
.apt-api.is-on { background: #37373d; border-left-color: #3b82f6; color: #fff; }
.apt-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.apt-method { font-size: 10px; font-weight: 700; flex-shrink: 0; width: 34px; }
.apt-api-name { flex: 1; min-width: 0; }
.apt-gx { opacity: 0; border: 0; background: transparent; color: #8b8b8b; cursor: pointer; display: inline-flex;
  padding: 2px; border-radius: 3px; flex-shrink: 0; transition: opacity .12s; }
.apt-gx:hover { color: #f87171; background: #3a3a3a; }
.apt-empty, .apt-empty-g { font-size: 11.5px; color: #6b6b6b; padding: 10px 12px 10px 30px; }
.apt-empty { text-align: center; padding: 20px 12px; }
</style>
