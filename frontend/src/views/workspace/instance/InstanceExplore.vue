<template>
  <div class="ixe-root" ref="rootEl">
    <!-- 头部:对象名/类型(标题) + 搜索(含菜单面板) + 保存 -->
    <div class="ixe-head">
      <!-- 从某实例进入:固定标题(不可选择) -->
      <div v-if="fixedTitle" class="ixe-fixed-title" :title="fixedTitle">
        <span class="bl-truncate" style="font-weight:600;max-width:200px">{{ fixedTitle }}</span>
      </div>
      <!-- 否则:对象类型下拉(可选择) -->
      <div v-else class="ixe-type" @click.stop="typeMenu=!typeMenu" v-click-outside="()=>typeMenu=false">
        <span class="ixe-type-ic" :style="{ background:(curType?.color||'#86909c')+'1f', color:curType?.color||'#86909c' }"
              v-html="BL.icon(curType?.icon||'cube', 14, curType?.color||'#86909c')"></span>
        <span class="bl-truncate" style="max-width:160px;font-weight:600">{{ curType?.display_name || '选择对象类型' }}</span>
        <span v-html="BL.icon('chevronDown', 12)"></span>
        <div v-if="typeMenu" class="ixe-type-menu" @click.stop>
          <input class="bl-input bl-input-sm" v-model="typeKw" placeholder="搜索对象类型…" style="margin-bottom:6px" />
          <!-- 范围切换:默认仅当前分组,可切全部(有搜索词时隐藏) -->
          <div v-if="!typeKw.trim() && groupTypes.length > 1" class="ixe-type-scope">
            <span class="bl-grow">{{ typeShowAll ? '全部对象类型' : '当前分组' }} · {{ typeOptions.length }}</span>
            <button class="ixe-type-scope-tg" @click.stop="typeShowAll = !typeShowAll">
              {{ typeShowAll ? '仅当前分组' : '显示全部' }}
            </button>
          </div>
          <div class="ixe-type-list">
            <div v-for="t in typeOptions" :key="t.id" class="ixe-type-item" :class="t.id===classId&&'is-on'" @click="selectType(t.id)">
              <span class="ixe-type-ic" :style="{ background:(t.color||'#165DFF')+'1f', color:t.color||'#165DFF' }"
                    v-html="BL.icon(t.icon||'cube', 12, t.color||'#165DFF')"></span>
              <span class="bl-grow bl-truncate">{{ t.display_name }}</span>
              <span class="bl-muted" style="font-size:11px">{{ t.instanceCount }}</span>
            </div>
            <div v-if="!typeOptions.length" class="ixe-type-empty">无匹配对象类型</div>
          </div>
        </div>
      </div>

      <!-- 搜索栏(内含筛选条件块,依次排列) + 菜单面板 -->
      <div class="ixe-head-search" v-click-outside="()=>searchPanelOpen=false">
        <span class="ixe-hs-ic" v-html="BL.icon('search', 14, 'var(--bl-text-3)')"></span>
        <span v-for="p in pills" :key="p.id" class="ixe-pill" @click.stop="reopenFilter(p, $event)" :title="pillText(p)">
          <span class="bl-truncate">{{ pillText(p) }}</span>
          <button class="ixe-pill-x" @click.stop="removePill(p.id)" v-html="BL.icon('x', 11)"></button>
        </span>
        <input class="ixe-hs-input" v-model="kw" :placeholder="pills.length ? '点击搜索以展开属性筛选面板…' : '搜索属性以添加图表或筛选…'"
               @keyup.enter="reload" @focus="searchPanelOpen=true" />
        <button v-if="pills.length || kw.trim()" class="ixe-hs-clear" @click.stop="clearAll"
                v-html="iconText3('x','清空')"></button>
        <!-- 搜索菜单面板:左 主对象/关联对象类型 + 右 属性列表(按 ② 顶部搜索过滤、统计) -->
        <div v-if="searchPanelOpen && classId" class="ixe-sp" @click.stop>
          <aside class="ixe-sp-left">
            <div class="ixe-sp-scroll">
              <!-- ③ 主对象 -->
              <div class="ixe-sp-ent" :class="panelSel==='__main__' && 'is-on'" @click="panelSel='__main__'">
                <span class="ixe-link-ic" :style="{ background:(curType?.color||'#165DFF')+'1f', color:curType?.color||'#165DFF' }"
                      v-html="BL.icon(curType?.icon||'cube', 13, curType?.color||'#165DFF')"></span>
                <span class="bl-grow bl-truncate" style="font-weight:600">{{ curType?.display_name }}</span>
                <span class="ixe-ent-n">{{ matchCount(columns) }}</span>
                <span v-if="panelSel==='__main__'" class="ixe-ent-arrow" v-html="BL.icon('chevronsRight',14,'var(--bl-primary)')"></span>
              </div>
              <!-- ④ 关联对象类型(前缀“链接类型”) -->
              <div class="ixe-sp-subhd">关联对象类型 <span class="ixe-sp-n">{{ links.length }}</span></div>
              <div v-if="!links.length" class="ixe-sp-empty">暂无关联</div>
              <div v-for="l in links" :key="l.linkId+l.targetClassId" class="ixe-sp-ent ixe-sp-ent-link"
                   :class="panelSel===l.targetClassId && 'is-on'" :title="l.linkLabel" @click="selectLinkEntity(l)">
                <span class="ixe-link-ic" :style="{ background:(l.targetColor||'#86909c')+'1f', color:l.targetColor||'#86909c' }"
                      v-html="BL.icon(l.targetIcon||'cube', 12, l.targetColor||'#86909c')"></span>
                <div class="bl-grow" style="min-width:0">
                  <div class="ixe-link-tag bl-truncate"><span class="ixe-link-kind">链接类型</span>{{ l.linkLabel }}</div>
                  <div class="bl-truncate" style="font-size:12.5px;font-weight:500">{{ l.targetClassName }}</div>
                </div>
                <span class="ixe-ent-n">{{ matchCount(linkColsCache[l.targetClassId]) }}</span>
                <span v-if="panelSel===l.targetClassId" class="ixe-ent-arrow" v-html="BL.icon('chevronsRight',14,'var(--bl-primary)')"></span>
              </div>
            </div>
          </aside>
          <div class="ixe-sp-right">
            <div class="ixe-sp-hd">
              <span class="ixe-sp-hd-ic" :style="{ color: selEntity.color }" v-html="BL.icon(selEntity.icon, 13, selEntity.color)"></span>
              <span class="bl-grow bl-truncate">{{ selEntity.name }} · 属性列表</span>
              <span class="ixe-sp-n">{{ rightCols.length }}</span>
            </div>
            <div class="ixe-sp-scroll">
              <div v-if="panelSel!=='__main__'" class="ixe-sp-tip ixe-sp-tip-act">点击关联属性 → 加为显示列 / 筛选条件</div>
              <template v-if="rightNormal.length">
                <div class="ixe-sp-grp"><span class="ixe-sp-grp-dot" style="background: var(--bl-primary)"></span>普通属性 <span class="ixe-sp-gn">{{ rightNormal.length }}</span></div>
                <div v-for="c in rightNormal" :key="c.field" class="ixe-sp-prop"
                     :class="panelSel==='__main__' && hasPill(c.field) && 'is-filtered'" @click="onPropClick(c, $event)">
                  <span class="ixe-sp-prop-ic" v-html="BL.icon(typeIcon(c.dataType), 13, 'var(--bl-primary)')"></span>
                  <span class="bl-grow bl-truncate">{{ c.label }}</span>
                  <span v-if="panelSel==='__main__' && hasPill(c.field)" v-html="BL.icon('filter', 11, 'var(--bl-primary)')"></span>
                </div>
              </template>
              <template v-if="rightObject.length">
                <div class="ixe-sp-grp"><span class="ixe-sp-grp-dot" style="background: #ff7d00"></span>对象属性 <span class="ixe-sp-gn">{{ rightObject.length }}</span></div>
                <div v-for="c in rightObject" :key="c.field" class="ixe-sp-prop ixe-sp-prop-obj"
                     :class="panelSel==='__main__' && hasPill(c.field) && 'is-filtered'" @click="onPropClick(c, $event)">
                  <span class="ixe-sp-prop-ic" v-html="BL.icon('link', 13, '#ff7d00')"></span>
                  <span class="bl-grow bl-truncate">{{ c.label }}</span>
                  <span class="ixe-sp-obj-tag">对象</span>
                </div>
              </template>
              <div v-if="!rightCols.length" class="ixe-sp-empty">无匹配属性</div>
            </div>
          </div>
        </div>

        <!-- 关联属性操作菜单:加为显示列 / 加为筛选条件 -->
        <div v-if="relMenu" class="ixe-relmenu" :style="{ left: relMenu.left+'px', top: relMenu.top+'px' }"
             v-click-outside="()=>relMenu=null" @click.stop>
          <div class="ixe-relmenu-hd bl-truncate">{{ relMenu.targetName }}·{{ relMenu.col.label }}</div>
          <div class="ixe-relmenu-item" @click="addRelColumn()">
            <span v-html="BL.icon('columns', 13, 'var(--bl-text-2)')"></span>加为显示列
          </div>
          <div class="ixe-relmenu-item" @click="addRelFilter($event)">
            <span v-html="BL.icon('filter', 13, 'var(--bl-text-2)')"></span>加为筛选条件
          </div>
        </div>

        <!-- 多条件筛选抽屉:放在搜索容器内,与菜单面板并存(点抽屉不收起面板) -->
        <FilterDrawer v-if="filterField" :field="filterField" :options="filterOptions" :model="filterModel" :anchor="filterAnchor"
                      @confirm="onFilterConfirm" @cancel="filterField=null" />
      </div>
      <!-- 保存整合实例探索(查询条件 + 显示形式 + 表格) -->
      <button v-if="classId" class="ixe-save" @click="onSave" v-html="iconText('save','保存')"></button>
    </div>

    <!-- 子头:视图页签(前) + 布局名 + 结果数 -->
    <div class="ixe-subhead" v-if="classId">
      <div class="ixe-vtabs">
        <button :class="['ixe-vtab', viewMode==='list' && 'is-on']" @click="viewMode='list'">
          <span v-html="BL.icon('list', 14)"></span>列表
        </button>
        <button :class="['ixe-vtab', viewMode==='charts' && 'is-on']" @click="viewMode='charts'">
          <span v-html="BL.icon('barChart', 14)"></span>看板
        </button>
      </div>
      <span class="bl-grow"></span>
      <!-- maker 顶栏 Teleport 到这里(看板模式,靠右,与布局选择同行) -->
      <div v-show="viewMode==='charts'" id="ixe-maker-tools" class="ixe-maker-tools"></div>
      <div class="ixe-layout-sel" @click.stop="layoutMenu=!layoutMenu" v-click-outside="()=>layoutMenu=false">
        <span class="bl-truncate">{{ currentDesignId ? designName : defaultLayoutName }}</span>
        <span v-html="BL.icon('chevronDown', 11)"></span>
        <div v-if="layoutMenu" class="ixe-layout-menu" @click.stop>
          <div class="ixe-layout-item" :class="!currentDesignId && 'is-on'" @click="resetDesign">{{ defaultLayoutName }}</div>
          <template v-if="designsForType.length">
            <div class="ixe-layout-sep"></div>
            <div v-for="d in designsForType" :key="d.id" class="ixe-layout-item" :class="d.id===currentDesignId && 'is-on'"
                 @click="applyDesign(d)">
              <span class="bl-grow bl-truncate">{{ d.name }}</span>
              <button class="ixe-layout-del" title="删除" @click.stop="removeDesign(d)" v-html="BL.icon('trash', 12)"></button>
            </div>
          </template>
          <div class="ixe-layout-sep"></div>
          <div class="ixe-layout-item ixe-layout-new" @click="onSave"
               v-html="iconText2('plus', saveAsLabel)"></div>
        </div>
      </div>
      <span v-if="viewMode!=='charts'" class="ixe-result-badge">{{ total.toLocaleString() }} 条结果</span>
      <button v-if="viewMode==='charts'" class="ixe-full-btn" :title="dashFull ? '退出全屏' : '全屏'"
              @click="toggleDashFull" v-html="BL.icon(dashFull ? 'minimize' : 'maximize', 15)"></button>
    </div>

    <!-- 主体:图表看板 + 右结果列 -->
    <div class="ixe-main" v-if="classId">
      <!-- 看板模式:内嵌数据可视化设计器(按数据特征自动出图,可增删改) -->
      <MakerEmbed v-if="viewMode==='charts'" ref="chartsRef" class="ixe-dash" :class-id="classId"
                  :columns="displayColumns" :filter-params="filterParams" :saved-config="savedConfig"
                  toolbar-target="#ixe-maker-tools"
                  @save-as="onSave" @save-page="onSavePage" />

      <!-- 列表模式:列表探索(滚动加载 + 预览/多实例/比较) -->
      <InstanceListView v-else class="ixe-listview" :class-id="classId" :type-name="curType?.display_name"
                        :columns="displayColumns" :filter-params="filterParams"
                        @open-instance="(p)=>$emit('open-instance', p)"
                        @selection-change="(ids)=>listSelectedIds=ids" />
    </div>
    <div v-else class="ixe-pick bl-empty">从左上角下拉选择一个对象类型开始探索</div>

    <!-- 保存为探索设计 弹框 -->
    <div v-if="saveModal" class="ixe-save-mask" @click.self="saveModal=false">
      <div class="ixe-save-modal">
        <div class="ixe-save-hd">
          <span>{{ viewMode==='charts' ? '保存图表' : '保存查询或列表' }}</span>
          <button class="ixe-save-x" @click="saveModal=false" v-html="BL.icon('x', 16)"></button>
        </div>
        <div class="ixe-save-body">
          <div v-if="viewMode!=='charts'" class="ixe-save-field">
            <span class="ixe-save-label">保存对象为</span>
            <div class="ixe-save-vis">
              <button :class="['ixe-vis-card', sdKind==='query' && 'is-on']" @click="sdKind='query'">
                <span class="ixe-vis-ic" v-html="BL.icon('barChart', 15, 'var(--bl-primary)')"></span>
                <span class="bl-grow" style="text-align:left">查询</span>
                <span v-if="sdKind==='query'" v-html="BL.icon('check', 14, 'var(--bl-primary)')"></span>
              </button>
              <button :class="['ixe-vis-card', sdKind==='list' && 'is-on']" @click="sdKind='list'">
                <span class="ixe-vis-ic" v-html="BL.icon('list', 15, 'var(--bl-text-2)')"></span>
                <span class="bl-grow" style="text-align:left">列表</span>
                <span v-if="sdKind==='list'" v-html="BL.icon('check', 14, 'var(--bl-primary)')"></span>
              </button>
            </div>
            <span class="ixe-save-hint">{{ sdKind==='query' ? '将筛选条件保存为动态查询,新数据结果会自动更新' : '将当前结果保存为静态列表' }}</span>
          </div>
          <label class="ixe-save-field">
            <span class="ixe-save-label">{{ viewMode==='charts' ? '名称' : (sdKind==='query' ? '查询名称' : '列表名称') }}</span>
            <input class="bl-input" v-model="sdName" placeholder="如「洪水位高级筛选」"
                   @input="sdErr=''" @keyup.enter="confirmSave" autofocus />
            <span v-if="sdErr" class="ixe-save-err">{{ sdErr }}</span>
          </label>
          <div v-if="sdKind==='list' && listSelectedIds.length" class="ixe-save-field">
            <span class="ixe-save-label">保存范围</span>
            <div class="ixe-save-vis">
              <button :class="['ixe-vis-card', sdScope==='all' && 'is-on']" @click="sdScope='all'">
                <span class="ixe-vis-ic" v-html="BL.icon('list', 15, 'var(--bl-text-2)')"></span>
                <span class="bl-grow" style="text-align:left">全部 {{ total.toLocaleString() }} 条</span>
                <span v-if="sdScope==='all'" v-html="BL.icon('check', 14, 'var(--bl-primary)')"></span>
              </button>
              <button :class="['ixe-vis-card', sdScope==='selected' && 'is-on']" @click="sdScope='selected'">
                <span class="ixe-vis-ic" v-html="BL.icon('check', 15, 'var(--bl-primary)')"></span>
                <span class="bl-grow" style="text-align:left">已选 {{ listSelectedIds.length }} 条</span>
                <span v-if="sdScope==='selected'" v-html="BL.icon('check', 14, 'var(--bl-primary)')"></span>
              </button>
            </div>
          </div>
          <label class="ixe-save-field">
            <span class="ixe-save-label">描述(选填)</span>
            <textarea class="bl-input ixe-save-textarea" v-model="sdDesc" rows="2" placeholder="自定义说明"></textarea>
          </label>
          <div class="ixe-save-vis">
            <button :class="['ixe-vis-card', sdVisibility==='private' && 'is-on']" @click="sdVisibility='private'">
              <span class="ixe-vis-ic" v-html="BL.icon('lock', 15, '#ff7d00')"></span>
              <span class="bl-grow" style="text-align:left">私有</span>
              <span v-if="sdVisibility==='private'" v-html="BL.icon('check', 14, 'var(--bl-primary)')"></span>
            </button>
            <button :class="['ixe-vis-card', sdVisibility==='public' && 'is-on']" @click="sdVisibility='public'">
              <span class="ixe-vis-ic" v-html="BL.icon('users', 15, 'var(--bl-primary)')"></span>
              <span class="bl-grow" style="text-align:left">公开</span>
              <span v-if="sdVisibility==='public'" v-html="BL.icon('check', 14, 'var(--bl-primary)')"></span>
            </button>
          </div>
          <button v-if="sdVisibility==='public'" class="ixe-save-proj"
                  @click="BL.info('演示:公共项目选择规划中')">
            {{ sdProject || '选择要保存到的公共项目' }}
          </button>
        </div>
        <div class="ixe-save-ft">
          <button class="bl-btn" @click="saveModal=false">取消</button>
          <button class="bl-btn bl-btn-primary" @click="confirmSave">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { BL } from '@/lib/bl.js'
import { instanceApi } from '@/api'
import FilterDrawer from './FilterDrawer.vue'
import MakerEmbed from '../MakerEmbed.vue'
import InstanceListView from './InstanceListView.vue'
import { useDesigns } from './designs.js'

const props = defineProps({
  types: { type: Array, default: () => [] },
  initialClassId: { type: String, default: '' },
  fixedTitle: { type: String, default: '' }   // 从某实例进入时的固定标题(对象类型不可选)
})
defineEmits(['open-instance'])

const viewMode = ref('list')   // list(列表,默认) | charts(看板/可视化设计器)

/* 看板全屏(整块实例探索区,工具栏一并全屏) */
const rootEl = ref(null)
const dashFull = ref(false)
function toggleDashFull () {
  const el = rootEl.value
  if (!el) return
  if (!document.fullscreenElement) el.requestFullscreen?.().catch(() => {})
  else document.exitFullscreen?.()
}
function onFsChange () { dashFull.value = !!document.fullscreenElement }
const searchPanelOpen = ref(false)
const layoutMenu = ref(false)
const designName = ref('默认探索布局')
/* 默认布局的显示名:看板模式叫「默认看板」,列表模式叫「默认列表」(仅展示,内部判定不变) */
const defaultLayoutName = computed(() => viewMode.value === 'charts' ? '默认看板' : '默认列表')
/* 「另存为新…」按钮文案:看板→新设计,列表→新列表 */
const saveAsLabel = computed(() => viewMode.value === 'charts' ? '另存为新设计…' : '另存为新列表…')
const chartsRef = ref(null)
function iconText(ic, t) { return `${BL.icon(ic, 13, '#fff')}<span style="margin-left:5px">${t}</span>` }
function iconText2(ic, t) { return `${BL.icon(ic, 12)}<span style="margin-left:5px">${t}</span>` }
function iconText3(ic, t) { return `${BL.icon(ic, 12, 'var(--bl-text-3)')}<span style="margin-left:3px">${t}</span>` }

/* 跳转到可视化制作(大屏),带上当前对象类型 + 筛选 */
const router = useRouter()
function openMaker() {
  const query = { classId: classId.value }
  if (kw.value.trim()) query.q = kw.value.trim()
  if (filterParams.value.filter) query.filter = filterParams.value.filter
  router.push({ path: '/workspace/maker', query })
}

/* —— 保存设计 / 模版(后端入库) —— */
const { listFor, save: saveDesign, remove: removeDesignStore, getDefault, saveDefault } = useDesigns()
const currentDesignId = ref(null)
const designsForType = ref([])              // 当前对象类型的命名设计列表(异步加载)
const savedConfig = ref(null)               // 当前看板的存档(layoutConfig);null=按数据特征自动生成

async function loadDesigns() {
  try { designsForType.value = classId.value ? await listFor(classId.value) : [] }
  catch { designsForType.value = [] }
}
async function loadDefaultDash() {
  try { savedConfig.value = classId.value ? await getDefault(classId.value) : null }
  catch { savedConfig.value = null }
}
/* maker「保存」→ 把完整布局存为该对象类型的默认探索布局 */
async function onSavePage(config) {
  if (!classId.value || !config) return
  try { await saveDefault(classId.value, config); BL.success('看板已保存') }
  catch { /* http 拦截器已弹错误 */ }
}

/* 列表模式下勾选的实例 id */
const listSelectedIds = ref([])

/* 保存弹框表单 */
const saveModal = ref(false)
const sdKind = ref('query')           // query(动态查询) | list(静态列表)
const sdScope = ref('all')            // all(全部) | selected(已选)
const sdName = ref('')
const sdDesc = ref('')
const sdVisibility = ref('private')   // private | public
const sdProject = ref('')
const sdErr = ref('')

function onSave() {
  layoutMenu.value = false
  sdKind.value = viewMode.value === 'list' ? 'list' : 'query'
  sdScope.value = listSelectedIds.value.length ? 'selected' : 'all'
  sdName.value = designName.value === '默认探索布局' ? '' : designName.value
  sdDesc.value = ''
  sdVisibility.value = 'private'
  sdProject.value = ''
  sdErr.value = ''
  saveModal.value = true
}
async function confirmSave() {
  const name = sdName.value.trim()
  if (!name) { sdErr.value = '请输入名称'; return }
  const useSel = sdKind.value === 'list' && sdScope.value === 'selected' && listSelectedIds.value.length
  // 命名设计同时存当前看板布局(layoutConfig),应用时可整盘恢复
  const layoutConfig = (viewMode.value === 'charts' && chartsRef.value) ? chartsRef.value.getConfig() : null
  try {
    const d = await saveDesign({
      name,
      kind: sdKind.value,
      instanceIds: useSel ? [...listSelectedIds.value] : [],
      desc: sdDesc.value.trim(),
      visibility: sdVisibility.value,
      project: sdVisibility.value === 'public' ? sdProject.value.trim() : '',
      classId: classId.value,
      className: curType.value?.display_name || '',
      icon: curType.value?.icon || 'search',
      color: curType.value?.color || '#165DFF',
      fixedTitle: props.fixedTitle || '',
      kw: kw.value,
      pills: JSON.parse(JSON.stringify(pills.value)),
      sort: sort.value, order: order.value,
      viewMode: viewMode.value,
      layoutConfig
    })
    currentDesignId.value = d.id
    designName.value = d.name
    saveModal.value = false
    await loadDesigns()
    BL.success(`已保存设计「${d.name}」`)
  } catch { /* http 拦截器已弹错误 */ }
}
function applyDesign(d) {
  layoutMenu.value = false
  currentDesignId.value = d.id
  designName.value = d.name
  kw.value = d.kw || ''
  sort.value = d.sort || ''
  order.value = d.order || 'asc'
  viewMode.value = d.viewMode || 'charts'
  // pills 补回唯一 id(老数据可能无 id)
  pills.value = (d.pills || []).map(p => ({ ...p, id: p.id != null ? p.id : ++pidSeq }))
  page.value = 1
  // 整盘恢复看板布局(MakerEmbed 监听 savedConfig 变化重建)
  savedConfig.value = d.layoutConfig || null
  reload()
}
function resetDesign() {
  layoutMenu.value = false
  currentDesignId.value = null
  designName.value = '默认探索布局'
  pills.value = []; kw.value = ''; sort.value = ''; page.value = 1
  // 回到默认探索布局(若该对象类型存过默认探索布局则恢复,否则自动生成)
  loadDefaultDash()
  reload()
}
async function removeDesign(d) {
  const ok = await BL.confirm({ title: '删除设计', content: `确定删除「${d.name}」?`, danger: true, okText: '删除' })
  if (!ok) return
  try {
    await removeDesignStore(d.id)
    await loadDesigns()
    if (currentDesignId.value === d.id) resetDesign()
    BL.success('已删除')
  } catch { /* http 拦截器已弹错误 */ }
}

/* —— 状态 —— */
const classId = ref(props.initialClassId || '')
const typeMenu = ref(false)
const typeKw = ref('')
const typeShowAll = ref(false)   // 对象类型下拉:默认仅当前分组,可切到全部
const columns = ref([])
const links = ref([])
const pills = ref([])          // [{ field, label, dataType, logic, conditions:[...] }]
const kw = ref('')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(25)
const sort = ref('')
const order = ref('asc')

const curType = computed(() => props.types.find(t => t.id === classId.value))
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))
/* 当前对象类型所属分组(category_code);下拉默认只列同组对象类型 */
const curGroup = computed(() => curType.value?.category_code || '')
const groupTypes = computed(() => curGroup.value ? props.types.filter(t => t.category_code === curGroup.value) : [])
const typeOptions = computed(() => {
  const q = typeKw.value.trim().toLowerCase()
  // 有搜索词:跨全部对象类型匹配;无搜索词:默认仅当前分组(组内 >1 才收窄),可切「显示全部」
  if (q) return props.types.filter(t => (t.display_name || '').toLowerCase().includes(q) || (t.api_name || '').toLowerCase().includes(q))
  if (!typeShowAll.value && groupTypes.value.length > 1) return groupTypes.value
  return props.types
})
/* —— 搜索面板:主对象/关联对象的属性列表(由 ② 顶部搜索 kw 统一过滤+统计) —— */
const panelSel = ref('__main__')                 // '__main__'=主对象 | targetClassId=某关联对象
const linkColsCache = reactive({})               // targetClassId -> columns[]
function kwFilter(list) {
  const q = kw.value.trim().toLowerCase()
  const arr = list || []
  if (!q) return arr
  return arr.filter(c => (c.label || '').toLowerCase().includes(q) || (c.field || '').toLowerCase().includes(q))
}
function matchCount(list) { return kwFilter(list).length }
async function ensureLinkCols(cid) {
  if (linkColsCache[cid]) return linkColsCache[cid]
  try { linkColsCache[cid] = (await instanceApi.columns(cid)) || [] } catch { linkColsCache[cid] = [] }
  return linkColsCache[cid]
}
function selectLinkEntity(l) { panelSel.value = l.targetClassId; ensureLinkCols(l.targetClassId) }
const selEntity = computed(() => {
  if (panelSel.value === '__main__')
    return { name: curType.value?.display_name || '主对象', icon: curType.value?.icon || 'cube', color: curType.value?.color || '#165DFF' }
  const l = links.value.find(x => x.targetClassId === panelSel.value)
  return { name: l?.targetClassName || '关联对象', icon: l?.targetIcon || 'cube', color: l?.targetColor || '#86909c' }
})
const selEntityCols = computed(() => panelSel.value === '__main__' ? columns.value : (linkColsCache[panelSel.value] || []))
const rightCols = computed(() => kwFilter(selEntityCols.value))
const rightNormal = computed(() => rightCols.value.filter(c => c.propType !== 'object'))
const rightObject = computed(() => rightCols.value.filter(c => c.propType === 'object'))

/* —— 关联对象属性:加为显示列 / 筛选条件(后端确定性联表 rel::{targetClassId}::{field}) —— */
const extraCols = ref([])                          // 用户从关联对象加入的显示列
const relFieldReg = reactive({})                   // 关联字段元数据(供编辑筛选块回填):field -> {field,label,dataType}
const relMenu = ref(null)                          // {col,targetClassId,targetName,left,top} | null
const displayColumns = computed(() => [...columns.value, ...extraCols.value])
function relFieldKey(t, f) { return `rel::${t}::${f}` }
function onPropClick(c, e) {
  if (panelSel.value === '__main__') { editingPillId.value = null; openFilter(c, e); return }
  // 关联对象属性:弹菜单选择加为显示列 / 筛选条件
  const l = links.value.find(x => x.targetClassId === panelSel.value)
  const rect = e.currentTarget.getBoundingClientRect()
  relMenu.value = { col: c, targetClassId: panelSel.value, targetName: l?.targetClassName || selEntity.value.name, left: rect.left, top: rect.bottom + 4 }
}
function addRelColumn() {
  const rm = relMenu.value; if (!rm) return
  const field = relFieldKey(rm.targetClassId, rm.col.field)
  const label = `${rm.targetName}·${rm.col.label}`
  if (extraCols.value.some(c => c.field === field)) BL.info('该显示列已存在')
  else {
    extraCols.value = [...extraCols.value, { field, label, dataType: rm.col.dataType, propType: rm.col.propType }]
    relFieldReg[field] = { field, label, dataType: rm.col.dataType }
    BL.success('已加为显示列:' + label)
  }
  relMenu.value = null
}
function addRelFilter(ev) {
  const rm = relMenu.value; if (!rm) return
  const rect = ev.currentTarget.getBoundingClientRect()
  const field = relFieldKey(rm.targetClassId, rm.col.field)
  const label = `${rm.targetName}·${rm.col.label}`
  relFieldReg[field] = { field, label, dataType: rm.col.dataType }
  editingPillId.value = null
  relMenu.value = null
  openFilter({ field, label, dataType: rm.col.dataType }, { currentTarget: { getBoundingClientRect: () => rect } })
}
/* 打开面板/切换类型时预取关联对象列(供数量统计 + 右侧展示) */
watch(searchPanelOpen, (v) => { if (v) { panelSel.value = '__main__'; links.value.forEach(l => ensureLinkCols(l.targetClassId)) } else { relMenu.value = null } })
watch(links, (ls) => { ls.forEach(l => ensureLinkCols(l.targetClassId)) })

/* —— 筛选抽屉 —— */
const filterField = ref(null)
const filterOptions = ref([])
const filterModel = ref(null)
const filterAnchor = ref(null)
let pidSeq = 0
const editingPillId = ref(null)   // 非空=编辑某条件块,空=新增

/* —— 类型/数据加载 —— */
function selectType(id) {
  if (classId.value === id) { typeMenu.value = false; return }
  classId.value = id
  typeMenu.value = false
  typeKw.value = ''
  typeShowAll.value = false
  pills.value = []
  kw.value = ''
  sort.value = ''
  page.value = 1
  panelSel.value = '__main__'
  extraCols.value = []
  Object.keys(linkColsCache).forEach(k => delete linkColsCache[k])
  currentDesignId.value = null
  designName.value = '默认探索布局'
  savedConfig.value = null
  loadMeta()
}

async function loadMeta() {
  if (!classId.value) return
  try {
    const [cols, lk] = await Promise.all([
      instanceApi.columns(classId.value),
      instanceApi.links(classId.value)
    ])
    columns.value = cols || []
    links.value = lk || []
  } catch { columns.value = []; links.value = [] }
  loadDesigns()
  loadDefaultDash()
  reload()
}

/* 当前 q + filter(图表与列表共用，保证图表反映筛选) */
const filterParams = computed(() => {
  const p = {}
  if (kw.value.trim()) p.q = kw.value.trim()
  if (pills.value.length) {
    p.filter = JSON.stringify({
      logic: 'AND',
      conditions: pills.value.map(x => ({ logic: x.logic, conditions: x.conditions }))
    })
  }
  return p
})

async function reload() {
  if (!classId.value) return
  const params = { classId: classId.value, page: page.value, size: size.value, ...filterParams.value }
  if (sort.value) { params.sort = sort.value; params.order = order.value }
  try {
    const res = await instanceApi.list(params)
    rows.value = res.rows || []
    total.value = res.total || 0
    if (res.columns) columns.value = res.columns
  } catch { rows.value = []; total.value = 0 }
}

function go(p) { if (p >= 1 && p <= totalPages.value) { page.value = p; reload() } }
function sortBy(f) {
  if (sort.value === f) order.value = order.value === 'desc' ? 'asc' : 'desc'
  else { sort.value = f; order.value = 'asc' }
  page.value = 1; reload()
}

/* —— 筛选抽屉交互 —— */
async function openFilter(col, ev) {
  const rect = ev.currentTarget.getBoundingClientRect()
  // 面板出现在触发元素正下方;cx=触发元素中心 X(供燕尾精确指向,面板被夹边时仍准)
  filterAnchor.value = { left: rect.left, top: rect.bottom + 6, cx: rect.left + rect.width / 2 }
  filterField.value = { field: col.field, label: col.label, dataType: detectType(col) }
  // 从面板点属性 = 新增独立条件块(model 为空);编辑已有块由 reopenFilter 设置 editingId/model
  filterModel.value = null
  filterOptions.value = []
  // 字符串/枚举：拉取去重值作为可选项(≤15 视作枚举)
  if (col.dataType === 'string' || col.dataType === 'enum') {
    try {
      const agg = await instanceApi.aggregate({ classId: classId.value, groupBy: col.field, limit: 30 })
      if (agg && agg.length && agg.length <= 15) {
        filterOptions.value = agg.map(a => a.key)
        filterField.value.dataType = 'enum'
      }
    } catch {}
  }
}
function detectType(col) {
  return col.dataType || 'string'
}
function reopenFilter(pill, ev) {
  const col = columns.value.find(c => c.field === pill.field) || relFieldReg[pill.field]
  if (!col) return
  editingPillId.value = pill.id            // 编辑该块
  openFilter(col, { currentTarget: ev.currentTarget })
  filterModel.value = pill                 // 回填该块已有条件(openFilter 内置空,这里覆盖)
}
function onFilterConfirm(group) {
  if (editingPillId.value != null) {
    const i = pills.value.findIndex(p => p.id === editingPillId.value)
    if (i >= 0) pills.value[i] = { ...group, id: editingPillId.value }
    else pills.value.push({ ...group, id: ++pidSeq })
  } else {
    pills.value.push({ ...group, id: ++pidSeq })   // 永远新增独立块(同字段也各自成块)
  }
  editingPillId.value = null
  filterField.value = null
  page.value = 1
  reload()
}
function removePill(id) {
  pills.value = pills.value.filter(p => p.id !== id)
  page.value = 1; reload()
}
function hasPill(field) { return pills.value.some(p => p.field === field) }
function clearAll() {
  pills.value = []; kw.value = ''; sort.value = ''; page.value = 1
  reload()
}

/* —— 展示辅助 —— */
const OP_LABEL = { eq:'=', ne:'≠', gt:'>', lt:'<', gte:'≥', lte:'≤', between:'介于', contains:'含', notContains:'不含',
  startsWith:'开头', endsWith:'结尾', empty:'为空', notEmpty:'非空', in:'∈', notIn:'∉', after:'晚于', before:'早于',
  notStartsWith:'非开头', notEndsWith:'非结尾' }
function pillText(p) {
  const c = p.conditions[0]
  if (!c) return p.label
  const op = OP_LABEL[c.op] || c.op
  let v = c.op === 'between' ? `${c.value}~${c.value2}` : (Array.isArray(c.value) ? c.value.join('/') : (c.value ?? ''))
  if (c.op === 'empty' || c.op === 'notEmpty') v = ''
  const more = p.conditions.length > 1 ? ` +${p.conditions.length - 1}` : ''
  return `${p.label} ${op} ${v}${more}`.trim()
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

/* click-outside 局部指令 */
const vClickOutside = {
  mounted(el, binding) { el.__h = (e) => { if (!el.contains(e.target)) binding.value(e) }; setTimeout(() => document.addEventListener('mousedown', el.__h), 0) },
  unmounted(el) { document.removeEventListener('mousedown', el.__h) }
}

watch(() => props.initialClassId, (v) => { if (v && v !== classId.value) selectType(v) })
onMounted(() => { if (classId.value) loadMeta(); document.addEventListener('fullscreenchange', onFsChange) })
onBeforeUnmount(() => { document.removeEventListener('fullscreenchange', onFsChange) })
</script>

<style scoped>
.ixe-root { flex: 1; display: flex; flex-direction: column; min-height: 0; background: var(--bl-bg-2); }

/* —— 头部:对象类型 + 搜索 + 保存 —— */
.ixe-head { flex-shrink: 0; display: flex; align-items: center; gap: 10px; padding: 8px 16px; background: var(--bl-bg-1); border-bottom: 1px solid var(--bl-border); }
.ixe-head-search { position: relative; flex: 1; min-width: 0; display: flex; align-items: center; flex-wrap: wrap; gap: 6px; min-height: 34px; box-sizing: border-box; padding: 4px 12px; border: 1px solid var(--bl-border); border-radius: 6px; background: var(--bl-bg-1); }
.ixe-head-search .ixe-pill { flex-shrink: 0; max-width: 240px; }
.ixe-head-search .ixe-hs-input { min-width: 140px; }
.ixe-hs-clear { flex-shrink: 0; display: inline-flex; align-items: center; gap: 2px; height: 22px; padding: 0 8px; border: 0; border-radius: 4px; background: var(--bl-bg-2); color: var(--bl-text-3); font-size: 12px; cursor: pointer; }
.ixe-hs-clear:hover { background: var(--bl-bg-hover); color: var(--bl-text-1); }
.ixe-head-search:focus-within { border-color: var(--bl-primary); }
.ixe-hs-ic { display: inline-flex; flex-shrink: 0; }
.ixe-hs-input { flex: 1; border: 0; outline: none; background: transparent; font-size: 13px; }
.ixe-fixed-title { display: flex; align-items: center; gap: 6px; flex-shrink: 0; font-size: 14px; color: var(--bl-text-1); padding: 4px 2px; }
.ixe-maker { flex-shrink: 0; height: 34px; padding: 0 14px; border: 1px solid var(--bl-primary); border-radius: 6px; background: var(--bl-bg-1); color: var(--bl-primary); font-size: 13px; cursor: pointer; display: inline-flex; align-items: center; }
.ixe-maker:hover { background: var(--bl-primary-soft); }
.ixe-save { flex-shrink: 0; height: 34px; padding: 0 16px; border: 0; border-radius: 6px; background: var(--bl-primary); color: #fff; font-size: 13px; cursor: pointer; display: inline-flex; align-items: center; }
.ixe-save:hover { background: var(--bl-primary-hover, #0e42d2); }
.ixe-save-sm { height: 28px; box-sizing: border-box; padding: 0 14px; font-size: 12.5px; }

/* 保存为探索设计 弹框 */
.ixe-save-mask { position: fixed; inset: 0; z-index: 1300; background: rgba(0,0,0,.32); display: flex; align-items: center; justify-content: center; }
.ixe-save-modal { width: 440px; max-width: 92vw; background: var(--bl-bg-1); border-radius: 10px; box-shadow: 0 16px 48px rgba(0,0,0,.24); overflow: hidden; animation: ixe-pop .16s ease; }
@keyframes ixe-pop { from { transform: translateY(8px); opacity: .6 } to { transform: none; opacity: 1 } }
.ixe-save-hd { display: flex; align-items: center; padding: 16px 18px 4px; font-size: 16px; font-weight: 600; color: var(--bl-text-1); }
.ixe-save-hd span { flex: 1; }
.ixe-save-x { width: 28px; height: 28px; border: 0; background: transparent; border-radius: 6px; color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.ixe-save-x:hover { background: var(--bl-bg-hover); color: var(--bl-text-1); }
.ixe-save-body { padding: 12px 18px 4px; display: flex; flex-direction: column; gap: 14px; }
.ixe-save-field { display: flex; flex-direction: column; gap: 6px; }
.ixe-save-label { font-size: 12.5px; color: var(--bl-text-2); }
.ixe-save-field .bl-input { width: 100%; box-sizing: border-box; }
.ixe-save-textarea { resize: vertical; min-height: 52px; font-family: inherit; line-height: 1.5; padding: 7px 10px; }
.ixe-save-err { font-size: 11.5px; color: #f53f3f; }
.ixe-save-hint { font-size: 11.5px; color: var(--bl-text-3); }
.ixe-save-vis { display: flex; gap: 12px; }
.ixe-vis-card { flex: 1; display: flex; align-items: center; gap: 8px; padding: 12px 14px; border: 1px solid var(--bl-border); border-radius: 8px; background: var(--bl-bg-1); cursor: pointer; font-size: 13px; color: var(--bl-text-1); }
.ixe-vis-card:hover { border-color: var(--bl-primary-border); }
.ixe-vis-card.is-on { border-color: var(--bl-primary); box-shadow: 0 0 0 2px var(--bl-primary-soft); }
.ixe-vis-ic { display: inline-flex; }
.ixe-save-proj { width: 100%; padding: 10px 12px; border: 1px dashed var(--bl-border); border-radius: 8px; background: var(--bl-bg-2); color: var(--bl-primary); cursor: pointer; font-size: 12.5px; text-align: left; }
.ixe-save-proj:hover { border-color: var(--bl-primary); }
.ixe-save-ft { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 18px; margin-top: 8px; background: var(--bl-bg-2); border-top: 1px solid var(--bl-divider); }

/* —— 子头:布局名 + 结果数 + pills —— */
.ixe-subhead { flex-shrink: 0; display: flex; align-items: center; gap: 10px; padding: 6px 16px; background: var(--bl-bg-1); border-bottom: 1px solid var(--bl-border); }
.ixe-layout-sel { position: relative; display: flex; align-items: center; gap: 4px; height: 28px; box-sizing: border-box; padding: 0 10px; border: 1px solid var(--bl-border); border-radius: 6px; cursor: pointer; font-size: 13px; flex-shrink: 0; max-width: 200px; }
.ixe-layout-sel:hover { border-color: var(--bl-primary-border); }
.ixe-layout-menu { position: absolute; top: 100%; left: 0; margin-top: 6px; width: 200px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; box-shadow: 0 8px 28px rgba(0,0,0,.14); z-index: 50; padding: 6px; }
.ixe-layout-item { display: flex; align-items: center; gap: 6px; padding: 7px 8px; border-radius: 6px; cursor: pointer; font-size: 12.5px; }
.ixe-layout-item:hover { background: var(--bl-bg-hover); }
.ixe-layout-item.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); }
.ixe-layout-sep { height: 1px; background: var(--bl-divider); margin: 4px 0; }
.ixe-layout-del { border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer; padding: 2px; border-radius: 4px; display: none; flex-shrink: 0; }
.ixe-layout-item:hover .ixe-layout-del { display: inline-flex; }
.ixe-layout-del:hover { color: #f53f3f; background: #fff1f0; }
.ixe-layout-new { color: var(--bl-primary); }
.ixe-result-badge { font-size: 12px; color: var(--bl-primary); background: var(--bl-primary-soft); border-radius: 4px; padding: 3px 8px; flex-shrink: 0; }
.ixe-pills-bar { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; min-width: 0; }

/* —— 主体:看板 + 结果列 —— */
.ixe-main { flex: 1; display: flex; min-height: 0; overflow: hidden; }
.ixe-dash { flex: 1; min-width: 0; }
.ixe-results { width: 300px; flex-shrink: 0; border-left: 1px solid var(--bl-border); background: var(--bl-bg-1); display: flex; flex-direction: column; }
.ixe-results-hd { display: flex; align-items: center; gap: 8px; padding: 12px 14px 8px; border-bottom: 1px solid var(--bl-divider); }
.ixe-results-title { font-size: 13px; font-weight: 600; }
.ixe-results-cnt { font-size: 11px; color: var(--bl-text-3); }
.ixe-results-sort { font-size: 12px; color: var(--bl-text-2); display: inline-flex; align-items: center; gap: 2px; cursor: pointer; }
.ixe-results-list { flex: 1; overflow: auto; padding: 4px 8px; }
.ixe-results-item { display: flex; align-items: center; gap: 8px; padding: 7px 6px; border-radius: 6px; cursor: pointer; }
.ixe-results-item:hover { background: var(--bl-bg-hover); }
.ixe-results-ic { width: 22px; height: 22px; border-radius: 5px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.ixe-results-more { padding: 10px 14px; font-size: 12.5px; color: var(--bl-primary); text-align: center; cursor: pointer; border-top: 1px solid var(--bl-divider); }
.ixe-results-more:hover { background: var(--bl-primary-soft); }

/* 搜索菜单面板:左 主对象+关联对象(宽) + 右 属性列表(宽度减半) */
.ixe-sp {
  position: absolute; top: calc(100% + 8px); left: 0; width: 640px; max-width: 92vw; height: 380px;
  display: flex; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 10px;
  box-shadow: 0 16px 48px rgba(0,0,0,.16); z-index: 60; overflow: hidden;
}
.ixe-sp-left { width: 360px; flex-shrink: 0; border-right: 1px solid var(--bl-border); display: flex; flex-direction: column; }
.ixe-sp-right { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.ixe-sp-hd { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--bl-text-2); font-weight: 600; padding: 10px 12px 6px; border-bottom: 1px solid var(--bl-divider); }
.ixe-sp-hd-ic { display: inline-flex; flex-shrink: 0; }
.ixe-sp-subhd { font-size: 11px; color: var(--bl-text-3); font-weight: 500; padding: 12px 10px 4px; letter-spacing: .3px; display: flex; align-items: center; gap: 6px; }
.ixe-sp-n { font-size: 11px; color: var(--bl-text-3); background: var(--bl-bg-2); border-radius: 9px; padding: 0 6px; }
.ixe-sp-scroll { flex: 1; overflow: auto; padding: 6px; }
.ixe-sp-empty { font-size: 12px; color: var(--bl-text-3); padding: 16px; text-align: center; }
/* 左:实体行(主对象 / 关联对象) */
.ixe-sp-ent { display: flex; align-items: center; gap: 8px; padding: 8px; border-radius: 8px; cursor: pointer; font-size: 12.5px; color: var(--bl-text-1); border: 1px solid transparent; }
.ixe-sp-ent:hover { background: var(--bl-bg-hover); }
.ixe-sp-ent.is-on { background: var(--bl-primary-soft); border-color: var(--bl-primary-border); }
.ixe-sp-ent-link { align-items: center; }
.ixe-ent-n { font-size: 11px; color: var(--bl-text-3); background: var(--bl-bg-3,#f0f2f5); border-radius: 9px; padding: 0 7px; flex-shrink: 0; }
.ixe-sp-ent.is-on .ixe-ent-n { background: #fff; color: var(--bl-primary); }
.ixe-ent-arrow { display: inline-flex; flex-shrink: 0; }
.ixe-link-tag { font-size: 10.5px; color: var(--bl-text-3); display: flex; align-items: center; gap: 4px; }
.ixe-link-kind { font-size: 10px; color: var(--bl-primary); background: var(--bl-primary-soft); border-radius: 3px; padding: 0 4px; flex-shrink: 0; }
/* 右:属性分组 + 属性行 */
.ixe-sp-grp { font-size: 11px; color: var(--bl-text-3); font-weight: 600; padding: 8px 8px 4px; display: flex; align-items: center; gap: 5px; }
.ixe-sp-grp-dot { width: 11px; height: 11px; border-radius: 3px; flex-shrink: 0; display: inline-block; }
.ixe-sp-gn { font-size: 10px; color: var(--bl-text-3); background: var(--bl-bg-2); border-radius: 8px; padding: 0 6px; }
.ixe-sp-prop { display: flex; align-items: center; gap: 8px; padding: 7px 8px; border-radius: 6px; cursor: pointer; font-size: 12.5px; color: var(--bl-text-1); }
.ixe-sp-prop:hover { background: var(--bl-bg-hover); }
.ixe-sp-prop.is-filtered { background: var(--bl-primary-soft); color: var(--bl-primary); }
.ixe-sp-prop-obj { cursor: pointer; }
.ixe-sp-prop-static { cursor: default; }
.ixe-sp-prop-static:hover { background: transparent; }
.ixe-sp-tip { font-size: 11px; color: var(--bl-text-3); background: var(--bl-bg-2); border-radius: 5px; padding: 5px 8px; margin: 4px 4px 2px; }
.ixe-sp-tip-act { color: var(--bl-primary); background: var(--bl-primary-soft); }
/* 关联属性操作菜单 */
.ixe-relmenu { position: fixed; z-index: 60; min-width: 160px; max-width: 240px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; box-shadow: 0 6px 20px rgba(0,0,0,.14); padding: 4px; }
.ixe-relmenu-hd { font-size: 11px; color: var(--bl-text-3); padding: 5px 8px; margin-bottom: 4px; border-bottom: 1px solid var(--bl-divider); }
.ixe-relmenu-item { display: flex; align-items: center; gap: 7px; padding: 7px 8px; border-radius: 6px; font-size: 12.5px; color: var(--bl-text-1); cursor: pointer; }
.ixe-relmenu-item:hover { background: var(--bl-bg-hover); color: var(--bl-primary); }
.ixe-sp-obj-tag { font-size: 10px; color: #ff7d00; background: rgba(255,125,0,.12); border-radius: 3px; padding: 0 5px; flex-shrink: 0; }
.ixe-sp-prop-ic { display: inline-flex; flex-shrink: 0; }
.ixe-type {
  position: relative; display: flex; align-items: center; gap: 6px;
  height: 30px; box-sizing: border-box; padding: 0 10px; border: 1px solid var(--bl-border); border-radius: 6px;
  cursor: pointer; flex-shrink: 0; font-size: 13px; background: var(--bl-bg-1);
}
.ixe-type:hover { border-color: var(--bl-primary-border); }
.ixe-type-ic { width: 22px; height: 22px; border-radius: 5px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ixe-type-menu {
  position: absolute; top: 100%; left: 0; margin-top: 6px; width: 280px;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px;
  box-shadow: 0 8px 28px rgba(0,0,0,.14); z-index: 50; padding: 8px;
}
.ixe-type-scope { display: flex; align-items: center; gap: 6px; padding: 2px 4px 6px; margin-bottom: 4px; border-bottom: 1px solid var(--bl-divider); font-size: 11px; color: var(--bl-text-3); }
.ixe-type-scope-tg { flex-shrink: 0; border: 0; background: transparent; color: var(--bl-primary); font-size: 11.5px; cursor: pointer; padding: 2px 4px; border-radius: 4px; }
.ixe-type-scope-tg:hover { background: var(--bl-primary-soft); }
.ixe-type-list { max-height: 320px; overflow: auto; }
.ixe-type-item { display: flex; align-items: center; gap: 8px; padding: 6px 8px; border-radius: 6px; cursor: pointer; font-size: 13px; }
.ixe-type-item:hover { background: var(--bl-bg-hover); }
.ixe-type-item.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); }
.ixe-type-empty { font-size: 12px; color: var(--bl-text-3); text-align: center; padding: 16px; }

.ixe-pills {
  flex: 0 1 260px; min-width: 160px; max-width: 260px;
  display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
  border: 1px solid var(--bl-border); border-radius: 6px; padding: 0 8px; min-height: 30px; box-sizing: border-box; background: var(--bl-bg-1);
}
.ixe-pill {
  display: inline-flex; align-items: center; gap: 4px; max-width: 220px;
  background: var(--bl-primary-soft); color: var(--bl-primary);
  border-radius: 4px; padding: 2px 6px; font-size: 12px; cursor: pointer;
}
.ixe-pill-x { border: 0; background: transparent; color: inherit; cursor: pointer; display: inline-flex; padding: 0; opacity: .7; }
.ixe-pill-x:hover { opacity: 1; }
.ixe-kw { flex: 1; min-width: 120px; border: 0; outline: none; background: transparent; font-size: 13px; padding: 4px; }
.ixe-icon-btn {
  width: 30px; height: 30px; flex-shrink: 0; border: 1px solid var(--bl-border); background: var(--bl-bg-1);
  border-radius: 6px; color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
}
.ixe-icon-btn:hover { background: var(--bl-bg-hover); color: var(--bl-primary); border-color: var(--bl-primary-border); }
/* 视图页签(下划线 tab 形式) */
.ixe-vtabs { display: inline-flex; align-items: center; gap: 22px; height: 28px; flex-shrink: 0; margin-right: 6px; }
.ixe-vtab { display: inline-flex; align-items: center; gap: 5px; height: 100%; border: 0; background: transparent; color: var(--bl-text-2); font-size: 13.5px; cursor: pointer; padding: 0; position: relative; }
.ixe-vtab:hover:not(.is-on) { color: var(--bl-text-1); }
.ixe-vtab.is-on { color: var(--bl-primary); font-weight: 600; }
.ixe-vtab.is-on::after { content: ''; position: absolute; left: 0; right: 0; bottom: -5px; height: 2px; background: var(--bl-primary); border-radius: 2px; }

.ixe-seg { display: inline-flex; border: 1px solid var(--bl-border); border-radius: 6px; overflow: hidden; flex-shrink: 0; }
.ixe-seg button {
  display: inline-flex; align-items: center; gap: 4px; height: 28px; box-sizing: border-box; padding: 0 12px; font-size: 12.5px;
  border: 0; background: var(--bl-bg-1); color: var(--bl-text-2); cursor: pointer;
}
.ixe-seg button + button { border-left: 1px solid var(--bl-border); }
.ixe-seg button span { display: inline-flex; }
.ixe-seg button.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.ixe-seg button:hover:not(.is-on) { background: var(--bl-bg-hover); }

/* 主体 */
.ixe-body { flex: 1; display: flex; min-height: 0; overflow: hidden; }
.ixe-left {
  width: 260px; flex-shrink: 0; border-right: 1px solid var(--bl-border);
  overflow: auto; padding: 8px; background: var(--bl-bg-1);
}
.ixe-cur {
  display: flex; align-items: center; gap: 8px; padding: 8px;
  background: var(--bl-primary-soft); border-radius: 6px; margin-bottom: 8px; font-size: 13px;
}
.ixe-sec {
  display: flex; align-items: center; gap: 6px;
  font-size: 11px; color: var(--bl-text-3); padding: 10px 8px 4px; letter-spacing: .3px;
}
.ixe-sec-n { background: var(--bl-bg-3,#f0f2f5); border-radius: 8px; padding: 0 6px; font-size: 10px; }
.ixe-sec-empty { font-size: 12px; color: var(--bl-text-3); padding: 4px 8px; }
.ixe-link, .ixe-prop {
  display: flex; align-items: center; gap: 8px; padding: 6px 8px; border-radius: 6px;
  cursor: pointer; font-size: 12.5px; color: var(--bl-text-2);
}
.ixe-link:hover, .ixe-prop:hover { background: var(--bl-bg-hover); }
.ixe-prop.is-filtered { background: var(--bl-primary-soft); color: var(--bl-primary); }
.ixe-link-ic, .ixe-prop-ic { display: inline-flex; flex-shrink: 0; }
.ixe-link-ic { width: 20px; height: 20px; border-radius: 5px; align-items: center; justify-content: center; }
.ixe-link-cnt { font-size: 11px; color: var(--bl-text-3); background: var(--bl-bg-3,#f0f2f5); border-radius: 8px; padding: 0 6px; }

.ixe-right { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.ixe-listview { flex: 1; min-width: 0; }
.ixe-result-hd { display: flex; align-items: center; padding: 8px 14px; border-bottom: 1px solid var(--bl-divider); font-size: 13px; }
.ixe-table-wrap { flex: 1; overflow: auto; background: var(--bl-bg-1); }
.ixe-table { font-size: 12.5px; border-collapse: separate; border-spacing: 0; width: 100%; }
/* 表头：吸顶 + 灰底，与内容拉开层次 */
.ixe-table thead th {
  position: sticky; top: 0; z-index: 3;
  background: var(--bl-bg-2); color: var(--bl-text-2); font-weight: 500; text-align: left;
  white-space: nowrap; cursor: pointer; user-select: none;
  padding: 8px 12px; border-bottom: 1px solid var(--bl-border);
}
/* 数据行：白底 + 浅灰行分隔线 */
.ixe-table tbody td {
  background: var(--bl-bg-1);
  padding: 8px 12px; border-bottom: 1px solid var(--bl-divider);
  white-space: nowrap; max-width: 220px; overflow: hidden; text-overflow: ellipsis;
}
.ixe-table tbody tr { cursor: pointer; }
.ixe-table tbody tr:hover td { background: var(--bl-bg-hover); }
/* 左侧固定列 */
.ixe-sticky-col { position: sticky; left: 0; z-index: 2; min-width: 180px; box-shadow: 1px 0 0 var(--bl-divider); }
.ixe-table thead .ixe-sticky-col { z-index: 4; }
.ixe-table tbody tr:hover .ixe-sticky-col { background: var(--bl-bg-hover); }
.ixe-code-col { width: 90px; }
.ixe-pager { display: flex; align-items: center; gap: 12px; justify-content: center; padding: 8px; border-top: 1px solid var(--bl-divider); }
.ixe-pick { flex: 1; padding: 80px 20px; }
.ixe-full-btn { display: inline-flex; align-items: center; justify-content: center; width: 28px; height: 28px; margin-left: 10px; border: 0; background: transparent; color: var(--bl-text-2); cursor: pointer; border-radius: 5px; }
.ixe-full-btn:hover { background: var(--bl-bg-hover); color: var(--bl-primary); }
</style>

<!-- 非 scoped:maker 顶栏 Teleport 到看板子头后,子 app 的 scoped 样式失效,这里全局还原工具栏外观 -->
<style>
.ixe-maker-tools { display: flex; align-items: center; min-width: 0; margin-right: 12px; }
.ixe-maker-tools .top-header { height: auto !important; min-height: 0 !important; padding: 0 !important; margin: 0 !important; border: 0 !important; background: transparent !important; box-shadow: none !important; }
.ixe-maker-tools .top-header .left { display: none !important; }
.ixe-maker-tools .top-header .right { display: flex; align-items: center; gap: 4px; }
.ixe-maker-tools .text-button { display: inline-flex; align-items: center; gap: 3px; padding: 0 8px; height: 28px; font-size: 13px; line-height: 1; color: var(--bl-text-2, #4e5969); cursor: pointer; border-radius: 4px; white-space: nowrap; }
.ixe-maker-tools .text-button:hover { background: var(--bl-bg-hover, #f2f3f5); color: var(--bl-primary, #1f6aff); }
.ixe-maker-tools .text-button.is-on { background: rgba(31,106,255,.1); color: var(--bl-primary, #1f6aff); }
.ixe-maker-tools .text-button svg { width: 14px; height: 14px; }
.ixe-maker-tools .mode-toggle.is-design { color: var(--bl-primary, #1f6aff); }
.ixe-maker-tools .fold-icon { display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; height: 28px; padding: 0 9px; cursor: pointer; color: var(--bl-text-2, #4e5969); border-radius: 4px; white-space: nowrap; font-size: 13px; }
.ixe-maker-tools .fold-icon > span { white-space: nowrap; }
.ixe-maker-tools .fold-icon:hover { background: var(--bl-bg-hover, #f2f3f5); color: var(--bl-text-1, #1d2129); }
.ixe-maker-tools .fold-icon.active { background: var(--bl-primary-soft, #e8f3ff); color: var(--bl-primary, #1f6aff); }
.ixe-maker-tools .buttons { display: flex; align-items: center; gap: 6px; margin-left: 6px; }
/* 画布缩放控件(teleport 到宿主后的兜底样式) */
.ixe-maker-tools .canvas-zoom-ctrl { display: inline-flex; align-items: center; height: 26px; margin-left: 4px; flex-shrink: 0; }
.ixe-maker-tools .canvas-zoom-ctrl .cz-btn { display: inline-flex; align-items: center; justify-content: center; width: 22px; height: 22px; font-size: 16px; line-height: 1; color: var(--bl-text-2, #4e5969); cursor: pointer; border-radius: 4px; user-select: none; }
.ixe-maker-tools .canvas-zoom-ctrl .cz-btn:hover { background: var(--bl-bg-hover, #f2f3f5); color: var(--bl-text-1, #1d2129); }
.ixe-maker-tools .canvas-zoom-ctrl .cz-val { min-width: 42px; text-align: center; font-size: 12px; color: var(--bl-text-2, #4e5969); cursor: pointer; user-select: none; }
.ixe-maker-tools .canvas-zoom-ctrl .cz-val:hover { color: var(--bl-primary, #1f6aff); }
.ixe-maker-tools .canvas-zoom-ctrl .cz-fit { margin-left: 4px; padding: 0 7px; height: 22px; line-height: 22px; font-size: 12px; color: var(--bl-text-2, #4e5969); cursor: pointer; border-radius: 4px; user-select: none; }
.ixe-maker-tools .canvas-zoom-ctrl .cz-fit:hover { background: var(--bl-bg-hover, #f2f3f5); color: var(--bl-text-1, #1d2129); }
.ixe-maker-tools .more-dropdown, .ixe-maker-tools .add-area-dropdown, .ixe-maker-tools .save-dropdown { margin: 0; }
/* 深色:添加区域 split-button(默认白底 el-button)→ 跟随主题 token */
:root[data-theme="dark"] .ixe-maker-tools .add-area-dropdown .el-button {
  background-color: var(--bl-bg-2, #242832) !important;
  border-color: var(--bl-border, #363B47) !important;
  color: var(--bl-text-2, #C9CDD4) !important;
}
:root[data-theme="dark"] .ixe-maker-tools .add-area-dropdown .el-button:hover,
:root[data-theme="dark"] .ixe-maker-tools .add-area-dropdown .el-button:focus {
  background-color: var(--bl-bg-hover, #2E323D) !important;
  border-color: var(--bl-primary, #165DFF) !important;
  color: var(--bl-text-1, #fff) !important;
}
/* caret 按钮与主按钮之间的分隔竖线 → 比主边框略明显,但不刺眼 */
:root[data-theme="dark"] .ixe-maker-tools .add-area-dropdown .el-dropdown__caret-button::before {
  background-color: var(--bl-border-strong, #4A5060) !important;
}
:root[data-theme="dark"] .ixe-maker-tools .add-area-dropdown .el-dropdown__caret-button {
  border-left-color: var(--bl-border-strong, #4A5060) !important;
}
.ixe-maker-tools .top-header .right > * + * { margin-left: 2px; }
/* 分隔竖线 / 自动保存开关 */
.ixe-maker-tools .tb-divider { width: 1px; height: 18px; background: var(--bl-divider, #e5e6eb); margin: 0 6px; flex-shrink: 0; }
.ixe-maker-tools .am-switch { margin: 0 6px; flex-shrink: 0; }
/* 深色:自动保存开关后的 label 文字(默认深色文字 → 看不清) */
:root[data-theme="dark"] .ixe-maker-tools .am-switch .el-switch__label { color: var(--bl-text-2, #C9CDD4) !important; }
:root[data-theme="dark"] .ixe-maker-tools .am-switch .el-switch__label.is-active { color: var(--bl-text-1, #fff) !important; }
/* 模式切换下拉(预览/设计):传送后 scoped 失效,这里全局还原触发器外观 */
.ixe-maker-tools .mode-dropdown { margin-left: 8px; }
.ixe-maker-tools .mode-dd-trigger { display: inline-flex; align-items: center; gap: 5px; width: 96px; box-sizing: border-box; height: 28px; padding: 0 8px 0 10px; border: 1px solid var(--bl-border, #e5e6eb); border-radius: 6px; font-size: 13px; color: var(--bl-text-2, #4e5969); cursor: pointer; outline: none; user-select: none; }
.ixe-maker-tools .mode-dd-trigger:hover { border-color: var(--bl-primary, #1f6aff); color: var(--bl-primary, #1f6aff); }
.ixe-maker-tools .mode-dd-trigger svg { width: 14px; height: 14px; flex-shrink: 0; }
.ixe-maker-tools .mode-dd-label { flex: 1; }
.ixe-maker-tools .mode-dd-caret { margin-left: auto; color: var(--bl-text-3, #86909c); }
/* 模式下拉菜单 teleport 到 body(popper-class=mode-dd-menu):宽度与触发器一致、项高 28px、当前项高亮 */
.mode-dd-menu.el-dropdown__popper { /* 容器 */ }
.mode-dd-menu .el-dropdown-menu { padding: 4px; }
.mode-dd-menu .el-dropdown-menu__item { width: 96px; box-sizing: border-box; min-height: 0; height: auto; line-height: 1.4; gap: 8px; padding: 7px 10px; margin: 0; border-radius: 6px; font-size: 12.5px; }
.mode-dd-menu .el-dropdown-menu__item svg { width: 14px; height: 14px; flex-shrink: 0; }
.mode-dd-menu .el-dropdown-menu__item.is-cur { color: var(--bl-primary, #1f6aff); background: var(--bl-primary-soft, rgba(31,106,255,.08)); }
</style>
