<template>
  <div class="page tc-page">
    <PageHeader title="类型类" subtitle="Type classes · 属性 / 关系 / 操作 三类元数据扩展目录">
      <template #actions>
        <div class="ov">
          <span class="ov-item"><span class="ov-lbl">总数</span><b>{{ rows.length }}</b></span>
          <span class="ov-item ov-ok"><span class="ov-lbl">可用</span><b>{{ activeCount }}</b></span>
          <span class="ov-item ov-risk"><span class="ov-lbl">已弃用</span><b>{{ deprecatedCount }}</b></span>
        </div>
        <select class="bl-input hd-filter" v-model="filterApplicable">
          <option value="">全部类别</option>
          <option value="property">属性 (property)</option>
          <option value="relation">关系 (relation)</option>
          <option value="action">操作 (action)</option>
        </select>
        <select class="bl-input hd-filter" v-model="filterDeprecated">
          <option value="">全部状态</option>
          <option value="0">可用</option>
          <option value="1">已弃用</option>
        </select>
        <div class="search-wrap">
          <span class="search-icon" v-html="BL.icon('search', 14)"></span>
          <input class="bl-input search-input" placeholder="搜索 (种类 / 名称 / 说明)" v-model="q" />
        </div>
        <button class="bl-btn bl-btn-primary" @click="openCreate">
          <span v-html="BL.icon('plus', 12, '#fff')"></span>
          <span style="margin-left:4px">新建类型类</span>
        </button>
      </template>
    </PageHeader>

    <div class="tc-main">
      <!-- 左侧: 按种类聚合树 -->
      <aside class="tc-tree">
        <div class="tc-tree-hd">
          <span style="font-weight:600;font-size:13px">类型分组</span>
          <span class="bl-muted" style="font-size:11px;margin-left:auto">{{ stats.length }} 类</span>
        </div>
        <div class="tc-tree-list">
          <div :class="['tc-grp', filterApplicable === '' && filterCategory === '' && 'is-active']"
               @click="filterApplicable = ''; filterCategory = ''">
            <span class="tc-grp-ic" style="background:#86909c" v-html="BL.icon('grid', 11, '#fff')"></span>
            <span class="tc-grp-lbl">全部</span>
            <span class="tc-grp-cnt">{{ rows.length }}</span>
          </div>
          <template v-for="apt in applicableTypes" :key="apt.k">
            <div :class="['tc-grp tc-grp-top', filterApplicable === apt.k && !filterCategory && 'is-active']"
                 @click="filterApplicable = apt.k; filterCategory = ''">
              <span class="tc-grp-ic" :style="{ background: apt.color }" v-html="BL.icon(apt.icon, 11, '#fff')"></span>
              <span class="tc-grp-lbl"><b>{{ apt.label }}</b></span>
              <span class="tc-grp-cnt">{{ statsByApt[apt.k]?.total || 0 }}</span>
            </div>
            <div v-for="s in statsByCategory(apt.k)" :key="apt.k + '-' + s.category"
                 :class="['tc-grp tc-grp-sub', filterApplicable === apt.k && filterCategory === s.category && 'is-active']"
                 @click="filterApplicable = apt.k; filterCategory = s.category">
              <span class="tc-grp-spacer"></span>
              <span class="tc-grp-lbl">{{ s.category_cn }}</span>
              <span class="tc-grp-cnt">{{ s.total_count }}</span>
            </div>
          </template>
        </div>
      </aside>

      <!-- 主表 -->
      <section class="pane pane-list">
        <div class="tc-list-scroll">
          <table class="bl-table tc-table">
            <thead>
              <tr>
                <th class="t-center" style="width:36px"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
                <th class="t-center" style="width:48px"></th>
                <th class="t-left" style="width:120px">适用</th>
                <th class="t-left">种类 / Category</th>
                <th class="t-left">名称 / Name</th>
                <th class="t-left" style="width:140px">参数 / 可选值</th>
                <th class="t-left">业务说明</th>
                <th class="t-center" style="width:80px">挂载</th>
                <th class="t-center" style="width:90px">状态</th>
                <th class="t-center" style="width:120px">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in filtered" :key="r.id" :class="['tc-row', r.is_deprecated && 'is-deprecated']"
                  @click="openEdit(r)">
                <td class="t-center" @click.stop>
                  <input type="checkbox" :checked="checked.has(r.id)" @change="toggleCheck(r.id)" />
                </td>
                <td class="t-center">
                  <span class="tc-ic" :style="{ background: aptColor(r.applicable_type) }"
                        v-html="BL.icon(aptIcon(r.applicable_type), 11, '#fff')"></span>
                </td>
                <td class="t-left"><span :class="['bl-tag', aptTagCls(r.applicable_type)]">{{ aptLabel(r.applicable_type) }}</span></td>
                <td class="t-left">
                  <div class="bl-mono" style="font-weight:500">{{ r.category }}</div>
                  <div class="bl-muted" style="font-size:11px">{{ r.category_cn }}</div>
                </td>
                <td class="t-left">
                  <div class="bl-mono" style="font-weight:500">{{ r.name }}</div>
                  <div class="bl-muted" style="font-size:11px">{{ r.name_cn }}</div>
                </td>
                <td class="t-left"><span class="bl-mono bl-muted bl-truncate" :title="r.value">{{ r.value || '—' }}</span></td>
                <td class="t-left"><span class="bl-muted bl-truncate" :title="r.description">{{ r.description || '—' }}</span></td>
                <td class="t-center">
                  <span v-if="r.link_type_id || r.object_type_id || r.action_type_id"
                        class="bl-tag bl-tag-success" title="已挂载到对象">已挂载</span>
                  <span v-else class="bl-tag" title="目录预置">目录</span>
                </td>
                <td class="t-center">
                  <span :class="['bl-tag', r.is_deprecated ? 'bl-tag-danger' : 'bl-tag-success']">
                    {{ r.is_deprecated ? '已弃用' : '可用' }}
                  </span>
                </td>
                <td class="t-center" @click.stop>
                  <div class="tc-act-row">
                    <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" :title="r.is_deprecated ? '恢复可用' : '标记弃用'"
                            @click="toggleDeprecated(r)" v-html="BL.icon(r.is_deprecated ? 'check' : 'archive', 12)"></button>
                    <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="编辑" @click="openEdit(r)" v-html="BL.icon('edit', 12)"></button>
                    <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click="onDelete(r)" v-html="BL.icon('trash', 12)"></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="!filtered.length" class="bl-empty" style="padding:48px">暂无匹配的类型类</div>
        </div>

        <div class="tc-pager">
          <div class="tc-pager-l">
            <template v-if="checked.size">
              已选 <b style="color:var(--bl-primary)">{{ checked.size }}</b> 项
              <button class="bl-btn bl-btn-sm tc-del-btn" style="margin-left:8px" @click="onBatchDelete">
                <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">批量删除</span>
              </button>
              <button class="bl-btn bl-btn-sm tc-arc-btn" style="margin-left:6px" @click="onBatchDeprecate">
                <span v-html="BL.icon('archive', 12)"></span><span style="margin-left:4px">批量弃用</span>
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

    <!-- 新建/编辑 右侧抽屉 (与共享属性 / 链接编辑器 视觉一致) -->
    <Teleport to="body">
      <transition name="tc-drawer">
        <aside v-if="formOpen" class="tc-drawer" :style="{ width: drawerW + 'px' }">
          <div class="tc-drag-handle" @mousedown="onHandleDown" :class="resizing && 'is-resizing'"></div>

          <!-- 头部 -->
          <header class="tc-d-hd">
            <div class="tc-d-hd-l">
              <span class="tc-d-ic" :style="{ background: aptColor(form.applicable_type) }"
                    v-html="BL.icon(aptIcon(form.applicable_type), 18, '#fff')"></span>
              <div class="tc-d-title-wrap">
                <div class="tc-d-title bl-truncate">{{ form.id ? '编辑类型类' : '新建类型类' }}<span v-if="form.id" class="bl-mono bl-muted tc-d-code">{{ form.category }}:{{ form.name }}</span></div>
                <div class="tc-d-meta" v-if="form.id">
                  <span :class="['bl-tag', form.is_deprecated ? 'bl-tag-danger' : 'bl-tag-success']">{{ form.is_deprecated ? '已弃用' : '可用' }}</span>
                </div>
              </div>
            </div>
            <div class="tc-d-hd-r">
              <button class="bl-btn bl-btn-text bl-btn-icon" :title="isMax ? '还原' : '最大化'"
                      @click="toggleMax"
                      v-html="BL.icon(isMax ? 'minimize' : 'maximize', 13)"></button>
              <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="formOpen=false" v-html="BL.icon('x', 14)"></button>
            </div>
          </header>

          <!-- 主体 -->
          <div class="tc-d-body">
            <div class="sec">基础信息</div>
            <FieldRow label="适用类型 *" inline>
              <select class="bl-input" v-model="form.applicable_type">
                <option value="property">属性 (property)</option>
                <option value="relation">关系 (relation)</option>
                <option value="action">操作 (action)</option>
              </select>
            </FieldRow>
            <FieldRow label="种类 *" inline hint="英文标识 (如 vertex / timeseries / hubble)">
              <input class="bl-input bl-mono" v-model="form.category" placeholder="vertex" />
            </FieldRow>
            <FieldRow label="种类中文 *" inline>
              <input class="bl-input" v-model="form.category_cn" placeholder="知识图谱" />
            </FieldRow>
            <FieldRow label="名称 *" inline hint="英文标识, 支持参数化 (如 event_intent.<intent_>)">
              <input class="bl-input bl-mono" v-model="form.name" placeholder="component" />
            </FieldRow>
            <FieldRow label="名称中文 *" inline>
              <input class="bl-input" v-model="form.name_cn" placeholder="图谱边组件" />
            </FieldRow>

            <div class="sec">配置参数</div>
            <FieldRow label="参数 / 可选值" inline>
              <input class="bl-input bl-mono" v-model="form.value" placeholder="danger/warning/major/success 等" />
            </FieldRow>
            <FieldRow label="业务说明">
              <textarea class="bl-textarea" rows="4" v-model="form.description" placeholder="详细业务释义、使用场景、规则"></textarea>
            </FieldRow>

            <div class="sec">状态</div>
            <FieldRow label="启用 / 弃用" inline>
              <div class="radio-group">
                <label class="radio-item"><input type="radio" :value="0" v-model.number="form.is_deprecated" /> 可用</label>
                <label class="radio-item"><input type="radio" :value="1" v-model.number="form.is_deprecated" /> 已弃用</label>
              </div>
            </FieldRow>
          </div>

          <!-- 底部 -->
          <footer class="tc-d-ft">
            <button v-if="form.id" class="bl-btn bl-btn-danger" @click="onDeleteCurrent">
              <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">删除</span>
            </button>
            <span style="flex:1"></span>
            <button class="bl-btn" @click="formOpen=false">取消</button>
            <button class="bl-btn bl-btn-primary" :disabled="!canSubmit" @click="onSubmit">保存</button>
          </footer>
        </aside>
      </transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { BL } from '@/lib/bl.js'
import { typeClassApi } from '@/api'
import PageHeader from '@/components/PageHeader.vue'
import FieldRow from '@/views/config/category/FieldRow.vue'

const applicableTypes = [
  { k: 'property', label: '属性', icon: 'list',   color: '#1677ff' },
  { k: 'relation', label: '关系', icon: 'link',   color: '#FF7D00' },
  { k: 'action',   label: '操作', icon: 'zap',    color: '#00B42A' }
]

const rows = ref([])
const stats = ref([])
const checked = ref(new Set())
const q = ref('')
const filterApplicable = ref('')
const filterCategory = ref('')
const filterDeprecated = ref('')

const formOpen = ref(false)
const form = reactive(defaultForm())
function defaultForm() {
  return { id: '', applicable_type: 'property', category: '', category_cn: '', name: '', name_cn: '',
           value: '', description: '', is_deprecated: 0,
           link_type_id: null, object_type_id: null, action_type_id: null }
}

async function load() {
  rows.value = await typeClassApi.list().catch(() => [])
  stats.value = await typeClassApi.stats().catch(() => [])
}
onMounted(load)

/* 统计 */
const activeCount = computed(() => rows.value.filter(r => !r.is_deprecated).length)
const deprecatedCount = computed(() => rows.value.filter(r => !!r.is_deprecated).length)
const statsByApt = computed(() => {
  const m = {}
  stats.value.forEach(s => {
    if (!m[s.applicable_type]) m[s.applicable_type] = { total: 0, items: [] }
    m[s.applicable_type].total += s.total_count
    m[s.applicable_type].items.push(s)
  })
  return m
})
function statsByCategory(apt) {
  return (statsByApt.value[apt]?.items || []).slice().sort((a, b) => a.category.localeCompare(b.category))
}

/* 过滤 */
const filtered = computed(() => {
  let list = rows.value
  if (filterApplicable.value) list = list.filter(r => r.applicable_type === filterApplicable.value)
  if (filterCategory.value) list = list.filter(r => r.category === filterCategory.value)
  if (filterDeprecated.value !== '') list = list.filter(r => String(r.is_deprecated) === filterDeprecated.value)
  const k = q.value.trim().toLowerCase()
  if (k) list = list.filter(r => [r.category, r.category_cn, r.name, r.name_cn, r.description, r.value]
    .filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
  return list
})

const allChecked = computed(() => filtered.value.length > 0 && filtered.value.every(r => checked.value.has(r.id)))
function toggleCheck(id) { const s = new Set(checked.value); s.has(id) ? s.delete(id) : s.add(id); checked.value = s }
function toggleAll() {
  const s = new Set(checked.value)
  if (allChecked.value) filtered.value.forEach(r => s.delete(r.id))
  else filtered.value.forEach(r => s.add(r.id))
  checked.value = s
}

/* 工具 */
function aptLabel(apt) { return applicableTypes.find(a => a.k === apt)?.label || apt }
function aptIcon(apt) { return applicableTypes.find(a => a.k === apt)?.icon || 'box' }
function aptColor(apt) { return applicableTypes.find(a => a.k === apt)?.color || '#86909c' }
function aptTagCls(apt) {
  return ({ property: 'bl-tag-primary', relation: 'bl-tag-warning', action: 'bl-tag-success' })[apt] || ''
}

/* 操作 */
function openCreate() {
  Object.assign(form, defaultForm())
  // 用当前过滤项作为默认
  if (filterApplicable.value) form.applicable_type = filterApplicable.value
  if (filterCategory.value) form.category = filterCategory.value
  formOpen.value = true
}
function openEdit(r) {
  Object.assign(form, defaultForm(), r)
  formOpen.value = true
}
const canSubmit = computed(() =>
  form.applicable_type && form.category && form.category_cn && form.name && form.name_cn
)
async function onSubmit() {
  try {
    if (form.id) {
      await typeClassApi.update(form.id, { ...form })
      BL.success('已保存')
    } else {
      await typeClassApi.create({ ...form })
      BL.success('已创建')
    }
    formOpen.value = false
    await load()
  } catch (e) { BL.error(e?.msg || '保存失败') }
}

async function onDelete(r) {
  const ok = await BL.confirm({
    title: '删除类型类', content: `确定删除「${r.category}:${r.name}」?`, danger: true, okText: '删除'
  })
  if (!ok) return
  await typeClassApi.remove(r.id).catch(() => null)
  BL.success('已删除')
  await load()
}
async function toggleDeprecated(r) {
  await typeClassApi.deprecate(r.id, r.is_deprecated ? 0 : 1).catch(() => null)
  await load()
}
async function onBatchDelete() {
  const ids = [...checked.value]
  if (!ids.length) return
  const ok = await BL.confirm({ title: '批量删除', content: `确定删除选中的 ${ids.length} 个类型类?`, danger: true, okText: '删除' })
  if (!ok) return
  for (const id of ids) await typeClassApi.remove(id).catch(() => null)
  BL.success(`已删除 ${ids.length} 个`)
  checked.value = new Set()
  await load()
}
async function onBatchDeprecate() {
  const ids = [...checked.value]
  if (!ids.length) return
  for (const id of ids) await typeClassApi.deprecate(id, 1).catch(() => null)
  BL.success(`已弃用 ${ids.length} 个`)
  checked.value = new Set()
  await load()
}

/* —— 抽屉里的"删除当前"快捷入口 (编辑态可用) —— */
async function onDeleteCurrent() {
  if (!form.id) return
  const ok = await BL.confirm({
    title: '删除类型类',
    content: `确定删除「${form.category}:${form.name}」?`,
    danger: true, okText: '删除'
  })
  if (!ok) return
  await typeClassApi.remove(form.id).catch(() => null)
  BL.success('已删除')
  formOpen.value = false
  await load()
}

/* —— 抽屉宽度 / 最大化 / 拖拽 —— */
const drawerW = ref(parseInt(localStorage.getItem('bl.tc.drawerW') || '560'))
const isMax = computed(() => drawerW.value >= 960)
function toggleMax() {
  drawerW.value = isMax.value ? 560 : 960
  localStorage.setItem('bl.tc.drawerW', String(drawerW.value))
}
const resizing = ref(false)
function onHandleDown(ev) {
  ev.preventDefault()
  resizing.value = true
  const startX = ev.clientX, baseW = drawerW.value
  function move(e) {
    const dx = startX - e.clientX
    drawerW.value = Math.max(440, Math.min(Math.floor(window.innerWidth * 0.85), baseW + dx))
  }
  function up() {
    resizing.value = false
    localStorage.setItem('bl.tc.drawerW', String(drawerW.value))
    window.removeEventListener('mousemove', move)
    window.removeEventListener('mouseup', up)
    document.body.style.userSelect = ''
  }
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', move)
  window.addEventListener('mouseup', up)
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }

.ov { display: inline-flex; gap: 14px; padding: 4px 12px; background: var(--bl-bg-2); border-radius: var(--bl-radius-2); }
.ov-item { font-size: 13px; color: var(--bl-text-2); }
.ov-item .ov-lbl { color: var(--bl-text-3); margin-right: 6px; }
.ov-item b { font-weight: 600; color: var(--bl-text-1); }
.ov-ok b { color: var(--bl-success); }
.ov-risk b { color: var(--bl-warning); }

.hd-filter { width: 130px; }
.search-wrap { position: relative; width: 240px; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.search-input { padding-left: 30px; }

.tc-main { flex: 1; display: flex; gap: 12px; padding: 12px; overflow: hidden; }

/* 左侧分组树 */
.tc-tree {
  width: 240px; flex-shrink: 0;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  display: flex; flex-direction: column; overflow: hidden;
}
.tc-tree-hd {
  display: flex; align-items: center;
  padding: 8px 10px;
  border-bottom: 1px solid var(--bl-divider);
}
.tc-tree-list { flex: 1; overflow: auto; padding: 6px; }
.tc-grp {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 8px; border-radius: 4px;
  font-size: 12.5px; color: var(--bl-text-2); cursor: pointer;
}
.tc-grp:hover { background: var(--bl-bg-hover); color: var(--bl-text-1); }
.tc-grp.is-active { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.tc-grp-ic { width: 18px; height: 18px; border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.tc-grp-lbl { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tc-grp-cnt {
  flex-shrink: 0; font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-2); border-radius: 9px;
  padding: 0 7px; min-width: 20px; height: 17px;
  line-height: 17px; text-align: center;
}
.tc-grp.is-active .tc-grp-cnt { background: #fff; color: var(--bl-primary); }
.tc-grp-sub { padding-left: 22px; }
.tc-grp-spacer { width: 18px; }

/* 主表 */
.pane {
  flex: 1; background: var(--bl-bg-1);
  border: 1px solid var(--bl-border); border-radius: var(--bl-radius-3);
  overflow: hidden; display: flex; flex-direction: column;
}
.pane-list { display: flex; flex-direction: column; }

.tc-list-scroll { flex: 1; min-height: 0; overflow: auto; }
.tc-pager {
  flex-shrink: 0; padding: 8px 12px; border-top: 1px solid var(--bl-divider);
  display: flex; justify-content: space-between; align-items: center; font-size: 12px;
}
.tc-pager-l { display: inline-flex; align-items: center; }
.tc-del-btn { background: #fff; border: 1px solid #f53f3f; color: #f53f3f; }
.tc-del-btn:hover { background: #fff1f0; }
.tc-arc-btn { background: #fff; border: 1px solid #86909c; color: #4e5969; }
.tc-arc-btn:hover { background: #f7f8fa; }

.tc-table { width: 100%; }
.tc-table thead th {
  position: sticky; top: 0; z-index: 2;
  background: var(--bl-bg-2);
  box-shadow: inset 0 -1px 0 var(--bl-divider);
  font-weight: 600; font-size: 12px; height: 34px; padding: 0 8px;
  color: #333; white-space: nowrap;
}
.tc-table thead th.t-left { text-align: left; }
.tc-table tbody tr { background: #fff; cursor: pointer; }
.tc-table tbody tr:hover { background: #f5f7fa; }
.tc-table tbody tr.is-deprecated { background: #fafafa; opacity: 0.7; }
.tc-table td { padding: 6px 8px; font-size: 12px; vertical-align: middle; }
.tc-table td.t-left { text-align: left; }
.tc-table td.t-center { text-align: center; }
.tc-ic { width: 22px; height: 22px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center; }

.tc-act-row {
  display: inline-flex; align-items: center; gap: 2px; flex-wrap: nowrap;
}
.tc-act-row .bl-btn { flex-shrink: 0; }

.radio-group { display: inline-flex; gap: 20px; }
.radio-item { display: inline-flex; align-items: center; gap: 6px; cursor: pointer; }
.bl-textarea {
  width: 100%; min-height: 80px; padding: 8px 10px;
  border: 1px solid var(--bl-border); border-radius: 4px;
  background: #fff; font-family: inherit; font-size: 13px;
  resize: vertical;
}
.bl-textarea:focus { outline: none; border-color: var(--bl-primary); }

/* —— 新建 / 编辑右侧抽屉 (与共享属性 / 链接编辑器视觉一致) —— */
.tc-drawer {
  position: fixed; top: 0; right: 0; bottom: 0;
  background: var(--bl-bg-1);
  border-left: 1px solid var(--bl-border);
  box-shadow: -4px 0 16px rgba(0,0,0,.10);
  display: flex; flex-direction: column;
  min-width: 440px; max-width: 95vw; z-index: 1000;
  overflow: hidden;
}
.tc-drag-handle {
  position: absolute; left: -2px; top: 0; bottom: 0; width: 5px;
  cursor: col-resize; transition: background-color .15s; z-index: 6;
}
.tc-drag-handle:hover, .tc-drag-handle.is-resizing { background: var(--bl-primary); }
.tc-drawer-enter-active, .tc-drawer-leave-active { transition: transform .25s, opacity .2s; }
.tc-drawer-enter-from, .tc-drawer-leave-to { transform: translateX(20px); opacity: 0; }

.tc-d-hd {
  height: 56px; padding: 0 14px;
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid var(--bl-divider);
  gap: 8px; flex-shrink: 0;
}
.tc-d-hd-l { display: flex; align-items: center; gap: 10px; min-width: 0; flex: 1; }
.tc-d-ic {
  width: 36px; height: 36px; border-radius: 8px;
  display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.tc-d-title-wrap { min-width: 0; }
.tc-d-title { font-size: 14px; font-weight: 600; color: var(--bl-text-1); }
.tc-d-code { font-size: 12px; font-weight: 400; margin-left: 6px; }
.tc-d-meta { font-size: 11px; color: var(--bl-text-3); margin-top: 2px; display: flex; align-items: center; }
.tc-d-hd-r { display: inline-flex; align-items: center; gap: 4px; flex-shrink: 0; }

.tc-d-body { flex: 1; overflow: auto; padding: 10px 14px; }
.tc-d-body .sec {
  font-size: 12px; color: var(--bl-text-3);
  margin: 14px 0 6px; padding-left: 8px;
  border-left: 3px solid var(--bl-primary);
}
.tc-d-body .sec:first-child { margin-top: 4px; }

.tc-d-ft {
  padding: 10px 14px;
  border-top: 1px solid var(--bl-divider);
  display: flex; align-items: center; gap: 8px;
  flex-shrink: 0;
}

/* 抽屉内 FieldRow inline label 加宽避免换行 */
.tc-drawer :deep(.fr.fr-inline .fr-label) { width: 90px; }
</style>
