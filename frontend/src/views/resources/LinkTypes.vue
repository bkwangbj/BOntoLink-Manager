<template>
  <div class="page lk-page">
    <PageHeader title="链接" subtitle="链接关系 · 管理实体关联关系,支持多基数可视化建模">
      <template #actions>
        <div class="ov">
          <span class="ov-item"><span class="ov-lbl">总数</span><b>{{ rows.length }}</b></span>
          <span class="ov-item ov-ok"><span class="ov-lbl">正式</span><b>{{ activeCount }}</b></span>
          <span class="ov-item"><span class="ov-lbl">实验</span><b style="color:#FF7D00">{{ experimentalCount }}</b></span>
          <span class="ov-item ov-risk"><span class="ov-lbl">废弃</span><b>{{ deprecatedCount }}</b></span>
        </div>
        <select class="bl-input hd-filter" v-model="filterStatus" title="状态">
          <option value="">全部状态</option>
          <option value="active">正式</option>
          <option value="experimental">实验中</option>
          <option value="deprecated">已废弃</option>
        </select>
        <select class="bl-input hd-filter" v-model="filterCardinality" title="基数" style="width:140px">
          <option value="">全部基数</option>
          <option value="1:1">一对一 (1:1)</option>
          <option value="1:N">一对多 (1:*)</option>
          <option value="N:1">多对一 (*:1)</option>
          <option value="N:N">多对多 (*:*)</option>
        </select>
        <div class="search-wrap">
          <span class="search-icon" v-html="BL.icon('search', 14)"></span>
          <input class="bl-input search-input" placeholder="搜索链接 (名称 / 编码 / 实体 / 基数)" v-model="q" />
        </div>
        <button class="bl-btn bl-btn-primary" @click="openCreate">
          <span v-html="BL.icon('plus', 12, '#fff')"></span>
          <span style="margin-left:4px">新建链接</span>
        </button>
      </template>
    </PageHeader>

    <div class="lk-main">
      <CategoryTreeFilter :rows="rows"
                          title="行业领域分组"
                          total-label="全部链接"
                          store-key="link-types"
                          @change="onCategoryChange" />

      <section class="pane pane-list">
        <div class="lk-list-scroll">
          <table class="bl-table lk-table">
            <colgroup>
              <col style="width:36px" /><col style="width:48px" />
              <col style="width:200px" /><col style="width:220px" />
              <col style="width:130px" /><col style="width:200px" />
              <col style="width:80px" /><col style="width:80px" /><col style="width:80px" />
              <col style="width:80px" /><col />
            </colgroup>
            <thead>
              <tr>
                <th class="t-center"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
                <th class="t-center"></th>
                <th class="t-left">源实体</th>
                <th class="t-left">链接名称</th>
                <th class="t-center">关联基数</th>
                <th class="t-left">目标实体</th>
                <th class="t-center" title="是否数据源中间表关联">关联表</th>
                <th class="t-center" title="是否双向启用">双向</th>
                <th class="t-center">状态</th>
                <th class="t-center">引用</th>
                <th class="t-left">备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in filtered" :key="r.id"
                  :class="['lk-row', selectedId === r.id && 'is-active']"
                  @click="onRowClick(r)">
                <td class="t-center" @click.stop>
                  <input type="checkbox" :checked="checked.has(r.id)" @change="toggleCheck(r.id)" />
                </td>
                <td class="t-center">
                  <span class="lk-card-ic" :style="{ background: cardColor(r) }" :title="cardLabel(r)">
                    <span v-html="BL.icon('link', 12, '#fff')"></span>
                  </span>
                </td>
                <td class="t-left">
                  <div class="lk-ent-cell">
                    <span class="lk-ent-ic" :style="{ background: r.l_class_color || '#1677ff' }"
                          v-html="BL.icon(r.l_class_icon || 'box', 11, '#fff')"></span>
                    <div class="lk-ent-txt">
                      <div class="bl-truncate" :title="r.l_class_name">{{ r.l_class_name || '—' }}</div>
                      <div class="bl-mono bl-muted">{{ r.l_class_api || '—' }}</div>
                    </div>
                  </div>
                </td>
                <td class="t-left">
                  <div>
                    <div class="bl-truncate" style="font-weight:500" :title="r.rdfs_label || r.link_type_id">{{ r.rdfs_label || r.link_type_id }}</div>
                    <div class="bl-mono bl-muted bl-truncate" style="font-size:11px" :title="r.link_type_id">{{ r.link_type_id }}</div>
                  </div>
                </td>
                <td class="t-center"><span class="bl-tag">{{ cardLabel(r) }}</span></td>
                <td class="t-left">
                  <div class="lk-ent-cell">
                    <span class="lk-ent-ic" :style="{ background: r.r_class_color || '#FF7D00' }"
                          v-html="BL.icon(r.r_class_icon || 'box', 11, '#fff')"></span>
                    <div class="lk-ent-txt">
                      <div class="bl-truncate" :title="r.r_class_name">{{ r.r_class_name || '—' }}</div>
                      <div class="bl-mono bl-muted">{{ r.r_class_api || '—' }}</div>
                    </div>
                  </div>
                </td>
                <td class="t-center" @click.stop>
                  <MiniSwitch :checked="r.is_data_source_rel === 1" disabled
                              :title="r.is_data_source_rel === 1 ? `中间表: ${r.rel_data_table}` : '常规字段关联'" />
                </td>
                <td class="t-center" @click.stop>
                  <MiniSwitch :checked="r.l_enabled === 1 && r.r_enabled === 1" disabled />
                </td>
                <td class="t-center"><span :class="['bl-tag', statusTagCls(r.status)]">{{ statusLabel(r.status) }}</span></td>
                <td class="t-center"><span class="bl-tag">{{ r.mapping_count || 0 }}</span></td>
                <td class="t-left"><span class="bl-muted bl-truncate" :title="r.rdfs_comment">{{ r.rdfs_comment || '—' }}</span></td>
              </tr>
            </tbody>
          </table>
          <div v-if="!filtered.length" class="bl-empty" style="padding:48px">暂无匹配的链接,请调整筛选条件或点击「新建链接」</div>
        </div>

        <!-- 底部分页 + 批量 -->
        <div class="lk-pager">
          <div class="lk-pager-l">
            <template v-if="checked.size">
              已选 <b style="color:var(--bl-primary)">{{ checked.size }}</b> 项
              <button class="bl-btn bl-btn-sm lk-del-btn" style="margin-left:8px" @click="onBatchDelete">
                <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">批量删除</span>
              </button>
              <button class="bl-btn bl-btn-sm bl-btn-text" style="margin-left:6px" @click="checked = new Set()">取消选择</button>
            </template>
            <template v-else>
              共 {{ filtered.length }} 项
            </template>
          </div>
        </div>
      </section>
    </div>

    <!-- 右侧详情/编辑抽屉 -->
    <LinkTypeEditor v-model:open="editorOpen"
                    :link-id="editorLinkId"
                    :all-classes="allClasses"
                    @saved="onSaved"
                    @deleted="onDeleted" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BL } from '@/lib/bl.js'
import { linkTypeApi, resourceApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import CategoryTreeFilter from '@/components/CategoryTreeFilter.vue'
import LinkTypeEditor from './linktype/LinkTypeEditor.vue'

const MiniSwitch = {
  name: 'MiniSwitch',
  props: { checked: Boolean, disabled: Boolean, title: String },
  emits: ['change'],
  setup(p, { emit }) {
    return () => h('span', {
      class: ['mini-sw', p.checked && 'is-on', p.disabled && 'is-disabled'],
      title: p.title,
      onClick: (e) => { e.stopPropagation(); if (!p.disabled) emit('change', !p.checked) }
    }, h('span', { class: 'mini-sw-dot' }))
  }
}

const rows = ref([])
const allClasses = ref([])
const checked = ref(new Set())
const selectedId = ref(null)
const q = ref('')
const filterStatus = ref('')
const filterCardinality = ref('')

const selectedCategoryCodes = ref(null)
function onCategoryChange({ codes }) { selectedCategoryCodes.value = codes || null }

const editorOpen = ref(false)
const editorLinkId = ref('')

const route = useRoute()
const router = useRouter()

async function load() {
  rows.value = await linkTypeApi.list().catch(() => [])
}
async function loadClasses() {
  const list = await resourceApi.classes().catch(() => [])
  allClasses.value = Array.isArray(list) ? list : (list?.data || list?.rows || [])
}
// URL 带 ?openId=<id> 时打开对应行详情;消费后清掉 query,避免刷新自动弹、并支持同页再次点击
function applyOpenId(id) {
  if (!id) return
  const row = rows.value.find(r => r.id === id)
  if (row) { onRowClick(row); router.replace({ query: {} }) }
}
onMounted(async () => {
  await load()
  loadClasses()
  applyOpenId(route.query.openId)
})
watch(() => route.query.openId, applyOpenId)

/* 统计 */
const activeCount = computed(() => rows.value.filter(r => r.status === 'active').length)
const experimentalCount = computed(() => rows.value.filter(r => r.status === 'experimental').length)
const deprecatedCount = computed(() => rows.value.filter(r => r.status === 'deprecated').length)

/* 过滤 */
function cardKey(r) {
  const l = r.l_cardinality === 'many' ? 'N' : '1'
  const rr = r.r_cardinality === 'many' ? 'N' : '1'
  return `${l}:${rr}`
}
const filtered = computed(() => {
  let list = rows.value
  if (selectedCategoryCodes.value) list = list.filter(r => selectedCategoryCodes.value.has(r.category_code))
  if (filterStatus.value) list = list.filter(r => r.status === filterStatus.value)
  if (filterCardinality.value) list = list.filter(r => cardKey(r) === filterCardinality.value)
  const k = q.value.trim().toLowerCase()
  if (k) list = list.filter(r => [r.rdfs_label, r.link_type_id, r.l_class_name, r.r_class_name, r.l_api_name, r.r_api_name, cardLabel(r)]
    .filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
  return list
})

const allChecked = computed(() => filtered.value.length > 0 && filtered.value.every(r => checked.value.has(r.id)))
function toggleCheck(id) {
  const s = new Set(checked.value); s.has(id) ? s.delete(id) : s.add(id); checked.value = s
}
function toggleAll() {
  const s = new Set(checked.value)
  if (allChecked.value) filtered.value.forEach(r => s.delete(r.id))
  else filtered.value.forEach(r => s.add(r.id))
  checked.value = s
}

function onRowClick(r) {
  selectedId.value = r.id
  editorLinkId.value = r.id
  editorOpen.value = true
}

function openCreate() {
  editorLinkId.value = ''
  editorOpen.value = true
}

async function onSaved() { await load() }
async function onDeleted() { editorOpen.value = false; await load() }

async function onBatchDelete() {
  const ids = [...checked.value]
  if (!ids.length) return
  const ok = await BL.confirm({ title: '批量删除', content: `确定删除选中的 ${ids.length} 个链接?`, danger: true, okText: '删除' })
  if (!ok) return
  await linkTypeApi.batchRemove(ids).catch(() => null)
  BL.success(`已删除 ${ids.length} 个`)
  checked.value = new Set()
  await load()
}

/* 工具 */
function cardLabel(r) {
  return ({ 'one:one': '一对一', 'one:many': '一对多', 'many:one': '多对一', 'many:many': '多对多' })[`${r.l_cardinality}:${r.r_cardinality}`] || '—'
}
function cardColor(r) {
  const k = `${r.l_cardinality}:${r.r_cardinality}`
  return ({ 'one:one': '#1677ff', 'one:many': '#00B42A', 'many:one': '#FF7D00', 'many:many': '#722ED1' })[k] || '#86909c'
}
function statusLabel(s) { return ({ active: '正式', experimental: '实验中', deprecated: '已废弃' })[s] || s }
function statusTagCls(s) {
  return ({ active: 'bl-tag-success', experimental: 'bl-tag-warning', deprecated: 'bl-tag-danger' })[s] || ''
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }

/* 顶部统计 */
.ov { display: inline-flex; gap: 14px; padding: 4px 12px; background: var(--bl-bg-2); border-radius: var(--bl-radius-2); }
.ov-item { font-size: var(--bl-fs-13); color: var(--bl-text-2); }
.ov-item .ov-lbl { color: var(--bl-text-3); margin-right: 6px; }
.ov-item b { font-weight: 600; color: var(--bl-text-1); }
.ov-ok b { color: var(--bl-success); }
.ov-risk b { color: var(--bl-warning); }

.hd-filter { width: 130px; }
.search-wrap { position: relative; width: 280px; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.search-input { padding-left: 30px; }

/* 主体 */
.lk-main { flex: 1; display: flex; gap: 12px; padding: 12px; overflow: hidden; }
.pane {
  flex: 1; background: var(--bl-bg-1);
  border: 1px solid var(--bl-border); border-radius: var(--bl-radius-3);
  overflow: hidden; display: flex; flex-direction: column;
}
.pane-list { display: flex; flex-direction: column; overflow: hidden; }

.lk-list-scroll { flex: 1; min-height: 0; overflow: auto; }
.lk-pager {
  flex-shrink: 0; padding: 8px 12px; border-top: 1px solid var(--bl-divider);
  font-size: 12px; display: flex; justify-content: space-between; align-items: center;
}
.lk-pager-l { display: inline-flex; align-items: center; }
.lk-del-btn { background: var(--bl-bg-1); border: 1px solid #f53f3f; color: #f53f3f; }
.lk-del-btn:hover { background: color-mix(in srgb, #f53f3f 8%, var(--bl-bg-1)); }

/* 表格 */
.lk-table { width: 100%; min-width: 1300px; }
.lk-table thead th {
  position: sticky; top: 0; z-index: 2;
  background: var(--bl-bg-2);
  box-shadow: inset 0 -1px 0 var(--bl-divider);
  font-weight: 600; font-size: 12px; height: 34px; padding: 0 8px;
  color: var(--bl-text-1); white-space: nowrap;
}
.lk-table thead th.t-left { text-align: left; }
.lk-table tbody tr { background: var(--bl-bg-1); cursor: pointer; }
.lk-table tbody tr:hover { background: var(--bl-bg-hover); }
.lk-table tbody tr.is-active { background: var(--bl-primary-soft); }
.lk-table td { padding: 0 8px; font-size: 12px; height: 44px; vertical-align: middle; }
.lk-table td.t-center { text-align: center; }
.lk-table td.t-left { text-align: left; }

.lk-card-ic {
  width: 28px; height: 28px; border-radius: 6px;
  display: inline-flex; align-items: center; justify-content: center;
}

.lk-ent-cell { display: inline-flex; align-items: center; gap: 8px; min-width: 0; max-width: 100%; }
.lk-ent-ic { width: 22px; height: 22px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.lk-ent-txt { min-width: 0; }
.lk-ent-txt .bl-truncate:first-child { font-weight: 500; color: var(--bl-text-1); }
.lk-ent-txt .bl-mono { font-size: 11px; }
</style>

<style>
.mini-sw {
  display: inline-block; width: 26px; height: 14px;
  border-radius: 8px; background: var(--bl-bg-3, #c9cdd4);
  position: relative; cursor: pointer;
  transition: background-color .15s;
  vertical-align: middle;
}
.mini-sw.is-on { background: var(--bl-primary); }
.mini-sw.is-disabled { opacity: .8; cursor: default; }
.mini-sw-dot {
  position: absolute; left: 2px; top: 2px;
  width: 10px; height: 10px; border-radius: 50%;
  background: var(--bl-bg-1); transition: left .15s;
  box-shadow: 0 1px 2px rgba(0,0,0,.3);
}
.mini-sw.is-on .mini-sw-dot { left: 14px; }
</style>
