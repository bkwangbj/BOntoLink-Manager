<!--
  类型类选择弹窗(1.4)。全屏模拟弹窗,左右分栏:
    左=大类导航(全部+计数);右=搜索/状态筛选 + 类型类表格(复选)。
  按当前载体 applicableType(property/relation/action)过滤可选项(allow_apply_types 白名单)。
  已绑定项(excludeIds)置灰不可重复选。确定 → emit('confirm', [metaId...])。
-->
<template>
  <div class="tcp-mask" @click.self="$emit('close')">
    <div class="tcp">
      <header class="tcp-hd">
        <span class="tcp-title">添加类型类</span>
        <button class="tcp-x" @click="$emit('close')" v-html="BL.icon('x', 18)"></button>
      </header>

      <div class="tcp-body">
        <!-- 左:大类导航 -->
        <aside class="tcp-nav">
          <div class="tcp-cat" :class="curCat === '' && 'is-on'" @click="curCat = ''">
            <span class="bl-grow">全部</span><span class="tcp-cnt">{{ list.length }}</span>
          </div>
          <div v-for="c in cats" :key="c.category_code" class="tcp-cat" :class="curCat === c.category_code && 'is-on'"
               @click="curCat = c.category_code">
            <span class="tcp-cat-ic" v-html="BL.icon(c.icon || 'tag', 13, c.color || 'var(--bl-text-3)')"></span>
            <span class="bl-grow bl-truncate">{{ c.category_name_cn }}<span class="tcp-cat-en bl-mono">{{ c.category_code }}</span></span>
            <span class="tcp-cnt">{{ catCount(c.category_code) }}</span>
          </div>
        </aside>

        <!-- 右:筛选 + 表格 -->
        <div class="tcp-main">
          <div class="tcp-filter">
            <div class="tcp-search">
              <span v-html="BL.icon('search', 13, 'var(--bl-text-3)')"></span>
              <input v-model="kw" placeholder="搜索类型类名称、英文编码" />
            </div>
            <select v-model="statusFilter" class="bl-input bl-input-sm tcp-sel">
              <option value="">全部状态</option><option value="active">可用</option><option value="deprecated">已弃用</option>
            </select>
          </div>

          <div class="tcp-tblwrap">
            <table class="tcp-tbl">
              <thead>
                <tr>
                  <th class="tcp-ck"><input type="checkbox" :checked="allChecked" @change="toggleAll($event.target.checked)" /></th>
                  <th style="width:76px">适用类型</th>
                  <th style="width:120px">种类</th>
                  <th>名称</th>
                  <th>参数说明</th>
                  <th style="width:80px">状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="t in filtered" :key="t.id" :class="[sel.has(t.id) && 'is-sel', bound(t.id) && 'is-bound']"
                    @click="!bound(t.id) && toggle(t.id)">
                  <td class="tcp-ck"><input type="checkbox" :checked="sel.has(t.id)" :disabled="bound(t.id)" @click.stop @change="toggle(t.id)" /></td>
                  <td><span v-for="a in applyTypes(t)" :key="a" class="tcp-apt" :class="'apt-'+a">{{ APT_CN[a] }}</span></td>
                  <td class="bl-mono tcp-cat-cell">{{ t.category_code }}</td>
                  <td><span class="tcp-name">{{ t.name_cn_base }}</span><span class="tcp-en bl-mono">{{ t.name_template || t.name_prefix }}</span></td>
                  <td class="tcp-desc">{{ t.param_desc || t.description || '—' }}</td>
                  <td><span class="tcp-st" :class="toInt(t.is_deprecated) ? 'is-dep' : 'is-ok'">{{ toInt(t.is_deprecated) ? '已弃用' : '可用' }}</span>
                    <span v-if="bound(t.id)" class="tcp-bound">已绑定</span></td>
                </tr>
                <tr v-if="!filtered.length"><td colspan="6" class="tcp-empty">暂无匹配的类型类</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <footer class="tcp-ft">
        <span class="tcp-selinfo">{{ sel.size ? `已选 ${sel.size} 项` : '' }}</span>
        <button class="bl-btn bl-btn-sm" @click="$emit('close')">取消</button>
        <button class="bl-btn bl-btn-sm bl-btn-primary" :disabled="!sel.size" @click="confirm">确定</button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { BL } from '@/lib/bl.js'
import { typeClassApi, tcCategoryApi } from '@/api'

const props = defineProps({
  applicableType: { type: String, required: true },   // property / relation / action
  excludeIds:     { type: Array, default: () => [] }   // 已绑定 metaId(置灰)
})
const emit = defineEmits(['confirm', 'close'])

const APT_CN = { property: '属性', relation: '关系', action: '操作' }
const cats = ref([])
const list = ref([])
const curCat = ref('')
const kw = ref('')
const statusFilter = ref('')
const sel = ref(new Set())
const excl = computed(() => new Set(props.excludeIds))

function toInt(v) { return v === true ? 1 : Number(v) || 0 }
function applyTypes(t) {
  const v = t.allow_apply_types
  if (Array.isArray(v)) return v
  if (typeof v === 'string') { try { return JSON.parse(v) } catch { return [] } }
  return []
}
function bound(id) { return excl.value.has(id) }
function catCount(code) { return list.value.filter(t => t.category_code === code).length }

const filtered = computed(() => {
  const q = kw.value.trim().toLowerCase()
  return list.value.filter(t => {
    if (curCat.value && t.category_code !== curCat.value) return false
    if (statusFilter.value === 'active' && toInt(t.is_deprecated)) return false
    if (statusFilter.value === 'deprecated' && !toInt(t.is_deprecated)) return false
    if (q) {
      const hay = (t.name_cn_base + ' ' + t.name_prefix + ' ' + (t.name_template || '') + ' ' + t.category_code).toLowerCase()
      if (!hay.includes(q)) return false
    }
    return true
  })
})
const allChecked = computed(() => filtered.value.length > 0 && filtered.value.every(t => bound(t.id) || sel.value.has(t.id)))

function toggle(id) { const s = new Set(sel.value); s.has(id) ? s.delete(id) : s.add(id); sel.value = s }
function toggleAll(on) {
  const s = new Set(sel.value)
  filtered.value.forEach(t => { if (bound(t.id)) return; on ? s.add(t.id) : s.delete(t.id) })
  sel.value = s
}
function confirm() { emit('confirm', [...sel.value]) }

onMounted(async () => {
  try { cats.value = (await tcCategoryApi.list()) || [] } catch { cats.value = [] }
  try { list.value = (await typeClassApi.list({ applicableType: props.applicableType })) || [] } catch { list.value = [] }
})
</script>

<style scoped>
.tcp-mask { position: fixed; inset: 0; z-index: 1300; background: rgba(0,0,0,.36); display: flex; align-items: center; justify-content: center; }
.tcp { width: 96vw; height: 92vh; max-width: 1280px; background: var(--bl-bg-1); border-radius: 12px; box-shadow: 0 20px 60px rgba(0,0,0,.28); display: flex; flex-direction: column; overflow: hidden; }
.tcp-hd { display: flex; align-items: center; padding: 14px 20px; border-bottom: 1px solid var(--bl-divider); }
.tcp-title { flex: 1; font-size: 16px; font-weight: 600; color: var(--bl-text-1); }
.tcp-x { width: 30px; height: 30px; border: 0; background: transparent; border-radius: 6px; color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.tcp-x:hover { background: var(--bl-bg-hover); color: var(--bl-text-1); }
.tcp-body { flex: 1; display: flex; min-height: 0; }
.tcp-nav { width: 240px; flex-shrink: 0; border-right: 1px solid var(--bl-border); overflow: auto; padding: 8px; background: var(--bl-bg-2); }
.tcp-cat { display: flex; align-items: center; gap: 8px; padding: 8px 10px; border-radius: 6px; cursor: pointer; font-size: 13px; color: var(--bl-text-2); }
.tcp-cat:hover { background: var(--bl-bg-hover); }
.tcp-cat.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.tcp-cat-ic { display: inline-flex; flex-shrink: 0; }
.tcp-cat-en { font-size: 10.5px; color: var(--bl-text-3); margin-left: 5px; }
.tcp-cat.is-on .tcp-cat-en { color: var(--bl-primary); opacity: .7; }
.tcp-cnt { font-size: 11px; color: var(--bl-text-3); background: var(--bl-bg-3, #f0f2f5); border-radius: 9px; padding: 0 7px; flex-shrink: 0; }
.tcp-main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.tcp-filter { flex-shrink: 0; display: flex; align-items: center; gap: 10px; padding: 12px 16px; border-bottom: 1px solid var(--bl-divider); }
.tcp-search { flex: 1; max-width: 360px; display: flex; align-items: center; gap: 6px; height: 32px; padding: 0 10px; border: 1px solid var(--bl-border); border-radius: 6px; }
.tcp-search input { flex: 1; border: 0; outline: none; background: transparent; font-size: 13px; }
.tcp-sel { width: 130px; }
.tcp-tblwrap { flex: 1; overflow: auto; }
.tcp-tbl { width: 100%; border-collapse: separate; border-spacing: 0; font-size: 12.5px; }
.tcp-tbl thead th { position: sticky; top: 0; z-index: 1; background: var(--bl-bg-2); color: var(--bl-text-2); font-weight: 500; text-align: left; padding: 9px 12px; border-bottom: 1px solid var(--bl-border); white-space: nowrap; }
.tcp-tbl tbody td { padding: 9px 12px; border-bottom: 1px solid var(--bl-divider); vertical-align: middle; }
.tcp-tbl tbody tr { cursor: pointer; }
.tcp-tbl tbody tr:hover { background: var(--bl-bg-hover); }
.tcp-tbl tbody tr.is-sel { background: var(--bl-primary-soft); }
.tcp-tbl tbody tr.is-bound { color: var(--bl-text-3); cursor: not-allowed; }
.tcp-ck { width: 40px; text-align: center; }
.tcp-apt { font-size: 11px; border-radius: 3px; padding: 1px 6px; margin-right: 4px; }
.apt-property { color: #165dff; background: rgba(22,93,255,.1); }
.apt-relation { color: #00b42a; background: rgba(0,180,42,.1); }
.apt-action { color: #ff7d00; background: rgba(255,125,0,.1); }
.tcp-cat-cell { color: var(--bl-text-3); }
.tcp-name { font-weight: 500; color: var(--bl-text-1); }
.tcp-en { font-size: 11px; color: var(--bl-text-3); margin-left: 6px; }
.tcp-desc { color: var(--bl-text-3); max-width: 340px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tcp-st { font-size: 11px; border-radius: 3px; padding: 1px 7px; }
.tcp-st.is-ok { color: #00b42a; background: rgba(0,180,42,.1); }
.tcp-st.is-dep { color: var(--bl-text-3); background: var(--bl-bg-3, #f0f2f5); }
.tcp-bound { font-size: 10.5px; color: var(--bl-primary); margin-left: 5px; }
.tcp-empty { text-align: center; color: var(--bl-text-3); padding: 40px; }
.tcp-ft { display: flex; align-items: center; gap: 10px; padding: 12px 20px; border-top: 1px solid var(--bl-divider); background: var(--bl-bg-2); }
.tcp-selinfo { flex: 1; font-size: 12.5px; color: var(--bl-primary); }
</style>
