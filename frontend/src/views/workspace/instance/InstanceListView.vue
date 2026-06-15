<template>
  <div class="ilv-root" ref="rootRef">
    <!-- 表格区 -->
    <div class="ilv-table-wrap" ref="scroller">
      <table class="ilv-table" :class="noTruncate && 'is-nowrap-off'" :style="{ width: tableWidth + 'px' }">
        <colgroup>
          <col :style="{ width: '44px' }" />
          <col v-for="c in renderCols" :key="c.key" :style="{ width: c.width + 'px' }" />
        </colgroup>
        <thead>
          <!-- 第一行:复选 + 分组标题(colspan) / 独立列(rowspan2) -->
          <tr>
            <th class="ilv-ck-col ilv-frozen" :style="{ left: '0px', zIndex: 6 }" :rowspan="headerLayout.hasGroups ? 2 : 1">
              <input type="checkbox" :checked="allChecked" :indeterminate.prop="someChecked" @change="toggleAll" />
            </th>
            <template v-for="seg in headerLayout.top" :key="seg.key || seg.col.key">
              <th v-if="seg.type==='group'" class="ilv-group-th" :colspan="seg.span">{{ seg.name }}</th>
              <th v-else
                  :class="['ilv-th', frozenSet.has(seg.ri) && 'ilv-frozen', dragKey===seg.col.key && 'is-dragging']"
                  :rowspan="headerLayout.hasGroups ? 2 : 1"
                  :style="frozenSet.has(seg.ri) ? { left: leftOffset(seg.ri) + 'px', zIndex: 5 } : null"
                  :draggable="!seg.col.system"
                  @dragstart="onColDragStart(seg.col, $event)" @dragover.prevent="onColDragOver(seg.col)" @drop="onColDrop(seg.col)" @dragend="dragKey=null">
                <span class="ilv-th-in">
                  <span v-if="!seg.col.system" class="ilv-th-grip" title="拖动排序列" v-html="BL.icon('grip', 13, 'var(--bl-text-2)')"></span>
                  <span class="ilv-th-label bl-truncate" @click="quickSort(seg.col.key)">{{ seg.col.label }}</span>
                  <span v-if="sortOf(seg.col.key)" class="ilv-th-sort">
                    <span v-html="BL.icon(sortOf(seg.col.key).order==='desc' ? 'arrowDown' : 'arrowUp', 14, 'var(--bl-primary)')"></span>
                    <span v-if="sorts.length>1" class="ilv-th-prio">{{ sortOf(seg.col.key).idx + 1 }}</span>
                  </span>
                  <button class="ilv-th-menu" :class="headerMenu===seg.col.key && 'is-on'" title="列操作"
                          @click.stop="toggleHeaderMenu(seg.col.key, $event)" v-html="BL.icon('chevronDown', 15, 'currentColor')"></button>
                </span>
                <span class="ilv-th-resizer" @mousedown.stop.prevent="startResize(seg.col, $event)" @click.stop></span>
              </th>
            </template>
          </tr>
          <!-- 第二行:分组内的二级业务列 -->
          <tr v-if="headerLayout.hasGroups">
            <th v-for="b in headerLayout.bottom" :key="b.col.key"
                :class="['ilv-th', frozenSet.has(b.ri) && 'ilv-frozen', dragKey===b.col.key && 'is-dragging']"
                :style="frozenSet.has(b.ri) ? { left: leftOffset(b.ri) + 'px', zIndex: 5 } : null"
                :draggable="true"
                @dragstart="onColDragStart(b.col, $event)" @dragover.prevent="onColDragOver(b.col)" @drop="onColDrop(b.col)" @dragend="dragKey=null">
              <span class="ilv-th-in">
                <span class="ilv-th-grip" title="拖动排序列" v-html="BL.icon('grip', 13, 'var(--bl-text-2)')"></span>
                <span class="ilv-th-label bl-truncate" @click="quickSort(b.col.key)">{{ b.col.label }}</span>
                <span v-if="sortOf(b.col.key)" class="ilv-th-sort">
                  <span v-html="BL.icon(sortOf(b.col.key).order==='desc' ? 'arrowDown' : 'arrowUp', 14, 'var(--bl-primary)')"></span>
                  <span v-if="sorts.length>1" class="ilv-th-prio">{{ sortOf(b.col.key).idx + 1 }}</span>
                </span>
                <button class="ilv-th-menu" :class="headerMenu===b.col.key && 'is-on'" title="列操作"
                        @click.stop="toggleHeaderMenu(b.col.key, $event)" v-html="BL.icon('chevronDown', 15, 'currentColor')"></button>
              </span>
              <span class="ilv-th-resizer" @mousedown.stop.prevent="startResize(b.col, $event)" @click.stop></span>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in displayRows" :key="r.id"
              :class="['ilv-row', sel.has(r.id) && 'is-sel', focusId===r.id && 'is-focus']"
              @click="focusRow(r)">
            <td class="ilv-ck-col ilv-frozen" :style="{ left: '0px' }" @click.stop>
              <input type="checkbox" :checked="sel.has(r.id)" @change="toggleOne(r.id)" />
            </td>
            <td v-for="(c, i) in renderCols" :key="c.key"
                :class="['ilv-td', frozenSet.has(i) && 'ilv-frozen', c.key==='__name__' && 'ilv-name-td']"
                :style="frozenSet.has(i) ? { left: leftOffset(i) + 'px' } : null">
              <template v-if="c.key==='__name__'">
                <span class="ilv-row-ic" :style="{ background:(r.color||'#165DFF')+'1f', color:r.color||'#165DFF' }"
                      v-html="BL.icon(r.icon||'cube', 12, r.color||'#165DFF')"></span>
                <span class="ilv-cell" style="font-weight:500">{{ r.title }}</span>
              </template>
              <span v-else-if="c.key==='__code__'" class="ilv-cell bl-mono bl-muted">{{ r.code }}</span>
              <span v-else class="ilv-cell">{{ fmt(r[c.key], c.dataType) }}</span>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!rows.length && !loading" class="bl-empty" style="padding:48px">无匹配实例</div>
      <div class="ilv-foot">
        <span v-if="loading">加载中…</span>
        <span v-else-if="rows.length">共 {{ total.toLocaleString() }} 条 · 已显示 {{ displayRows.length }}</span>
      </div>
    </div>

    <!-- 列头下拉菜单 -->
    <div v-if="headerMenu" class="ilv-hmenu" :style="hmenuStyle" v-click-outside="closeHeaderMenu" @click.stop>
      <div class="ilv-hm-item" @click="setSort(headerMenu,'asc')"><span v-html="BL.icon('arrowUp',13,'var(--bl-primary)')"></span>升序</div>
      <div class="ilv-hm-item" @click="setSort(headerMenu,'desc')"><span v-html="BL.icon('arrowDown',13,'var(--bl-primary)')"></span>降序</div>
      <div class="ilv-hm-item" @click="clearSort(headerMenu)"><span v-html="BL.icon('refresh',13,'var(--bl-text-3)')"></span>清除排序</div>
      <div class="ilv-hm-sep"></div>
      <div class="ilv-hm-item" :class="hmIsSystem && 'is-disabled'" @click="hideColumn(headerMenu)"><span v-html="BL.icon('eyeOff',13,'var(--bl-text-3)')"></span>隐藏此列</div>
      <div class="ilv-hm-item" @click="openConfig(); closeHeaderMenu()"><span v-html="BL.icon('settings',13,'var(--bl-text-3)')"></span>配置列</div>
      <div class="ilv-hm-sep"></div>
      <div class="ilv-hm-freeze" @click.stop>
        <span v-html="BL.icon('lock',13,'#ff7d00')"></span>冻结前
        <input type="number" min="0" :max="renderCols.length" v-model.number="freezeCount" class="ilv-hm-num" @click.stop />列
        <button class="ilv-hm-ok" @click="closeHeaderMenu" v-html="BL.icon('check',12,'#fff')"></button>
      </div>
    </div>

    <!-- 拖拽分隔条(浮层,贴整个探索区右侧) -->
    <div v-if="previewOpen && (focusRow_ || selectedRows.length)"
         class="ilv-divider" :class="dragging && 'is-active'"
         :style="{ top: drawerTop + 'px', right: previewWidth + 'px' }" @mousedown="onDragStart"></div>

    <!-- 预览 / 多实例 / 比较 面板(浮层:横跨整个探索区高度) -->
    <aside v-if="previewOpen && (focusRow_ || selectedRows.length)" class="ilv-preview"
           :style="{ width: previewWidth + 'px', top: drawerTop + 'px' }">
      <header class="ilv-pv-hd">
        <span class="ilv-pv-title">{{ (mode==='compare' || selectedRows.length>1) ? `已选 ${selectedRows.length} 项` : '预览' }}</span>
        <span class="bl-grow"></span>
        <!-- 预览 | 对比 下拉 -->
        <div class="ilv-pv-mode" @click.stop="modeMenu=!modeMenu" v-click-outside="()=>modeMenu=false">
          <span v-html="BL.icon(mode==='compare'?'columns':'eye', 13, 'var(--bl-text-2)')"></span>
          <span>{{ mode==='compare' ? '对比' : '预览' }}</span>
          <span v-html="BL.icon('chevronDown', 11, 'var(--bl-text-3)')"></span>
          <div v-if="modeMenu" class="ilv-pv-modemenu" @click.stop>
            <div class="ilv-pv-modeitem" :class="mode==='preview' && 'is-on'" @click="setMode('preview'); modeMenu=false">
              <span v-html="BL.icon('eye', 13)"></span>预览
            </div>
            <div class="ilv-pv-modeitem" :class="mode==='compare' && 'is-on'" @click="setMode('compare'); modeMenu=false">
              <span v-html="BL.icon('columns', 13)"></span>对比
            </div>
          </div>
        </div>
        <button class="ilv-pv-collapse" :title="pvMax ? '还原' : '最大化'" @click="toggleMax"
                v-html="BL.icon(pvMax ? 'minimize' : 'maximize', 15)"></button>
        <button class="ilv-pv-collapse" title="关闭面板" @click="previewOpen=false" v-html="BL.icon('x', 16)"></button>
      </header>

      <div v-if="mode==='compare'" class="ilv-compare2">
        <!-- ②③ 左右两列:各自有一组备选项 chips + 详情 -->
        <div class="ilv-cmp-cols">
          <div v-for="side in cmpSides" :key="side.key" class="ilv-cmp-col">
            <!-- 该列备选项 chips(两行,点选切换该列实例) -->
            <div class="ilv-cmp-chips">
              <span v-for="r in selectedRows" :key="r.id" class="ilv-cmp-chip"
                    :class="side.id===r.id && 'is-on'" @click="setCmpSide(side.key, r.id)" :title="r.title">
                <span class="ilv-row-ic" :style="{ background:(r.color||'#165DFF')+'1f', color:r.color||'#165DFF' }"
                      v-html="BL.icon(r.icon||'cube', 12, r.color||'#165DFF')"></span>
                <span class="bl-truncate">{{ r.title }}</span>
                <button class="ilv-cmp-chip-x" title="移除" @click.stop="toggleOne(r.id)" v-html="BL.icon('x', 11)"></button>
              </span>
              <span v-if="!selectedRows.length" class="bl-muted" style="font-size:12px;padding:4px">勾选多个实例后可对比</span>
            </div>
            <!-- 详情头 -->
            <div v-if="side.row" class="ilv-cmp-colhd">
              <span class="ilv-row-ic ilv-cmp-hdic" :style="{ background:(side.row.color||'#165DFF')+'1f', color:side.row.color||'#165DFF' }"
                    v-html="BL.icon(side.row.icon||'cube', 16, side.row.color||'#165DFF')"></span>
              <div class="bl-grow" style="min-width:0">
                <div class="bl-truncate" style="font-weight:600;font-size:13px">{{ side.row.title }}</div>
                <div class="bl-truncate bl-mono bl-muted" style="font-size:10.5px">{{ typeName }} · {{ side.row.code }}</div>
              </div>
              <button class="bl-btn bl-btn-sm ilv-cmp-detail" title="详情"
                      @click="$emit('open-instance', { classId, row: side.row })" v-html="iconText('externalLink','详情')"></button>
            </div>
            <!-- 关键信息 kv(两列字段对齐,值不同高亮) -->
            <div v-if="side.row" class="ilv-kv-list">
              <div class="ilv-cmp-sec">关键信息</div>
              <div class="ilv-kv"><span class="ilv-kv-l">编码</span><span class="ilv-kv-r bl-mono">{{ side.row.code }}</span></div>
              <div v-for="c in businessCols" :key="c.key" class="ilv-kv" :class="cmpDiff(c.key) && 'is-diff'">
                <span class="ilv-kv-l">{{ c.label }}</span>
                <span class="ilv-kv-r">{{ fmt(side.row[c.key], c.dataType) }}</span>
              </div>
            </div>
            <div v-else class="bl-empty" style="padding:24px;font-size:12px">从上方备选项选择实例</div>
          </div>
        </div>
      </div>

      <template v-else>
        <div v-if="selectedRows.length>1" class="ilv-cards">
          <div v-for="r in selectedRows.slice(0,20)" :key="r.id"
               :class="['ilv-card', focusId===r.id && 'is-on']" @click="focusId=r.id">
            <span class="ilv-row-ic" :style="{ background:(r.color||'#165DFF')+'1f', color:r.color||'#165DFF' }"
                  v-html="BL.icon(r.icon||'cube', 12, r.color||'#165DFF')"></span>
            <span class="bl-grow bl-truncate">{{ r.title }}</span>
            <button class="ilv-card-x" title="移除" @click.stop="toggleOne(r.id)" v-html="BL.icon('x', 11)"></button>
          </div>
          <div v-if="selectedRows.length>20" class="ilv-cards-more">仅显示前 20 / 共 {{ selectedRows.length }} 项</div>
        </div>

        <div v-if="focusRow_" class="ilv-detail">
          <div class="ilv-pv-obj">
            <span class="ilv-pv-obj-ic" :style="{ background:(focusRow_.color||'#165DFF')+'1f', color:focusRow_.color||'#165DFF' }"
                  v-html="BL.icon(focusRow_.icon||'cube', 18, focusRow_.color||'#165DFF')"></span>
            <div class="bl-grow" style="min-width:0">
              <div class="bl-truncate" style="font-weight:600;font-size:14px">{{ focusRow_.title }}</div>
              <div class="bl-truncate bl-mono bl-muted" style="font-size:11.5px">{{ typeName }} · {{ focusRow_.code }}</div>
            </div>
            <button class="bl-btn bl-btn-sm" @click="$emit('open-instance', { classId, row: focusRow_ })"
                    v-html="iconText('externalLink','详情')"></button>
          </div>
          <div class="ilv-sec">关键信息</div>
          <div class="ilv-kv-list">
            <div class="ilv-kv"><span class="ilv-kv-l">编码</span><span class="ilv-kv-r bl-mono">{{ focusRow_.code }}</span></div>
            <div v-for="c in businessCols" :key="c.key" class="ilv-kv">
              <span class="ilv-kv-l"><span class="ilv-kv-ic" v-html="BL.icon(typeIcon(c.dataType), 11, 'var(--bl-text-3)')"></span>{{ c.label }}</span>
              <span class="ilv-kv-r">{{ fmt(focusRow_[c.key], c.dataType) }}</span>
            </div>
          </div>
          <div class="ilv-sec">关联对象类型 <span class="ilv-sec-n">{{ focusLinks.length }}</span></div>
          <div v-if="!focusLinks.length" class="bl-muted" style="font-size:12px;padding:6px 2px">{{ linksLoading ? '加载中…' : '暂无关联' }}</div>
          <div v-for="l in focusLinks" :key="l.linkId+l.targetClassId" class="ilv-link">
            <span class="ilv-link-ic" :style="{ background:(l.targetColor||'#86909c')+'1f', color:l.targetColor||'#86909c' }"
                  v-html="BL.icon(l.targetIcon||'cube', 12, l.targetColor||'#86909c')"></span>
            <div class="bl-grow" style="min-width:0">
              <div class="bl-truncate" style="font-size:12.5px;font-weight:500">{{ l.targetClassName }}</div>
              <div class="bl-truncate bl-muted" style="font-size:11px">{{ l.linkLabel }}</div>
            </div>
            <span class="ilv-link-cnt">{{ l.count }}</span>
          </div>
        </div>
      </template>
    </aside>

    <!-- 配置列 弹窗 -->
    <div v-if="configOpen" class="ilv-cfg-mask" @click.self="cancelConfig">
      <div class="ilv-cfg">
        <div class="ilv-cfg-hd">
          <span v-html="BL.icon('settings', 16, 'var(--bl-text-2)')"></span>
          <span class="bl-grow" style="font-weight:600">配置列</span>
          <button class="ilv-cfg-x" @click="cancelConfig" v-html="BL.icon('x', 16)"></button>
        </div>
        <div class="ilv-cfg-body">
          <!-- 待选列 -->
          <div class="ilv-cfg-pane">
            <div class="ilv-cfg-pt">待选列</div>
            <input class="bl-input bl-input-sm" v-model="cfgSearch" placeholder="搜索列…" style="margin-bottom:8px" />
            <div class="ilv-cfg-list">
              <div v-for="c in cfgAvailable" :key="c.key"
                   :class="['ilv-cfg-item', cfgPickL.has(c.key) && 'is-pick']" @click="pickL(c.key, $event)">
                <span class="bl-grow bl-truncate">{{ c.label }}</span>
                <span class="ilv-cfg-ic" v-html="BL.icon(typeIcon(c.dataType), 12, 'var(--bl-text-3)')"></span>
              </div>
              <div v-if="!cfgAvailable.length" class="ilv-cfg-empty">无</div>
            </div>
          </div>
          <!-- 操作 -->
          <div class="ilv-cfg-ops">
            <button class="ilv-cfg-op" :disabled="!cfgPickL.size" @click="moveToSelected" title="添加" v-html="BL.icon('chevronRight', 16)"></button>
            <button class="ilv-cfg-op" :disabled="!cfgPickR.size" @click="moveToAvailable" title="移除" v-html="BL.icon('chevronLeft', 16)"></button>
          </div>
          <!-- 已选列(分组树) -->
          <div class="ilv-cfg-pane">
            <div class="ilv-cfg-pt">
              已选列
              <span class="bl-grow"></span>
              <button class="ilv-cfg-addg" title="新建分组" @click="addGroup" v-html="BL.icon('plus', 14)"></button>
            </div>
            <div class="ilv-cfg-list" @dragover.prevent @drop="cfgDrop(null)">
              <template v-for="node in cfgTree" :key="node.type==='group' ? 'g'+node.id : node.col.key">
                <!-- 分组行 -->
                <template v-if="node.type==='group'">
                  <div class="ilv-cfg-group" @dragover.prevent @drop.stop="dropToGroup(node.id)">
                    <button class="ilv-cfg-fold" @click="toggleGroup(node.id)"
                            v-html="BL.icon(node.collapsed?'chevronRight':'chevronDown', 12, 'var(--bl-text-3)')"></button>
                    <span class="ilv-cfg-folder" v-html="BL.icon('folder', 13, '#ff7d00')"></span>
                    <input v-if="renameId===node.id" class="ilv-cfg-rename" v-model="renameText"
                           @keyup.enter="commitRename" @keyup.esc="renameId=null" @blur="commitRename" v-focus />
                    <span v-else class="bl-grow bl-truncate" @dblclick="startRename(node.id, node.name)">{{ node.name }}</span>
                    <span class="ilv-cfg-gn">{{ node.children.length }}</span>
                  </div>
                  <template v-if="!node.collapsed">
                    <div v-for="c in node.children" :key="c.key"
                         :class="['ilv-cfg-item', 'is-child', cfgPickR.has(c.key) && 'is-pick', !c.visible && 'is-hidden']"
                         :draggable="true" @click="pickR(c.key, $event)"
                         @dragstart="cfgDrag=c.key" @dragover.prevent @drop.stop="cfgDrop(c)" @dragend="cfgDrag=null">
                      <span class="ilv-cfg-grip" v-html="BL.icon('grip', 12, 'var(--bl-text-3)')"></span>
                      <span class="bl-grow bl-truncate">{{ c.label }}</span>
                      <span class="ilv-cfg-ic" v-html="BL.icon(typeIcon(c.dataType), 12, 'var(--bl-text-3)')"></span>
                      <button class="ilv-cfg-eye" :title="c.visible?'隐藏':'显示'" @click.stop="toggleEye(c.key)"
                              v-html="BL.icon(c.visible?'eye':'eyeOff', 13, c.visible?'var(--bl-primary)':'var(--bl-text-3)')"></button>
                    </div>
                    <div v-if="!node.children.length" class="ilv-cfg-gempty" @dragover.prevent @drop.stop="dropToGroup(node.id)">拖列到此分组</div>
                  </template>
                </template>
                <!-- 独立列 -->
                <div v-else
                     :class="['ilv-cfg-item', cfgPickR.has(node.col.key) && 'is-pick', node.col.system && 'is-sys']"
                     :draggable="!node.col.system" @click="pickR(node.col.key, $event)"
                     @dragstart="cfgDrag=node.col.key" @dragover.prevent @drop.stop="cfgDrop(node.col)" @dragend="cfgDrag=null">
                  <span v-if="!node.col.system" class="ilv-cfg-grip" v-html="BL.icon('grip', 12, 'var(--bl-text-3)')"></span>
                  <span class="bl-grow bl-truncate">{{ node.col.label }}<span v-if="node.col.system" class="ilv-cfg-sys">固定</span></span>
                  <span class="ilv-cfg-ic" v-html="BL.icon(typeIcon(node.col.dataType), 12, 'var(--bl-text-3)')"></span>
                  <button v-if="!node.col.system" class="ilv-cfg-eye" :title="node.col.visible?'隐藏':'显示'" @click.stop="toggleEye(node.col.key)"
                          v-html="BL.icon(node.col.visible?'eye':'eyeOff', 13, node.col.visible?'var(--bl-primary)':'var(--bl-text-3)')"></button>
                </div>
              </template>
            </div>
          </div>
        </div>
        <div class="ilv-cfg-ft">
          <button class="bl-btn bl-btn-sm bl-btn-text" @click="restoreDefault" v-html="iconText('refresh','恢复默认')"></button>
          <label class="ilv-cfg-wrap"><input type="checkbox" v-model="cfgNoTruncate" /> 表格中不截断文本</label>
          <span class="bl-grow"></span>
          <button class="bl-btn bl-btn-sm" @click="cancelConfig">取消</button>
          <button class="bl-btn bl-btn-sm bl-btn-primary" @click="applyConfig">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { BL } from '@/lib/bl.js'
import { instanceApi } from '@/api'

const props = defineProps({
  classId: { type: String, required: true },
  typeName: { type: String, default: '' },
  columns: { type: Array, default: () => [] },
  filterParams: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['open-instance', 'selection-change'])

function iconText(ic, t) { return `${BL.icon(ic, 12)}<span style="margin-left:4px">${t}</span>` }

/* —— 数据(全量加载,前端排序/列配置) —— */
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const scroller = ref(null)
const rootRef = ref(null)

async function fetchAll() {
  loading.value = true
  try {
    const res = await instanceApi.list({ classId: props.classId, page: 1, size: 100000, ...props.filterParams })
    rows.value = res.rows || []
    total.value = res.total || 0
  } catch { rows.value = []; total.value = 0 }
  loading.value = false
}

/* —— 列模型 —— */
function defWidth(dt) {
  if (dt === 'decimal' || dt === 'int') return 110
  if (dt === 'date' || dt === 'datetime') return 130
  if (dt === 'boolean') return 90
  return 150
}
const cols = ref([])
function buildCols() {
  cols.value = [
    { key: '__name__', label: '名称', dataType: 'string', system: true, visible: true, removed: false, width: 220, groupId: null },
    { key: '__code__', label: '编码', dataType: 'string', system: true, visible: true, removed: false, width: 130, groupId: null },
    ...props.columns.map(c => ({ key: c.field, label: c.label, dataType: c.dataType, system: false, visible: true, removed: false, width: defWidth(c.dataType), groupId: null }))
  ]
  groups.value = []
}
const businessCols = computed(() => cols.value.filter(c => !c.system && !c.removed))
const renderCols = computed(() => cols.value.filter(c => !c.removed && c.visible))
const tableWidth = computed(() => 44 + renderCols.value.reduce((s, c) => s + c.width, 0))

/* —— 分组(二级表头) —— */
let gidSeq = 0
const groups = ref([])   // [{ id, name, collapsed }]
function groupName(id) { const g = groups.value.find(x => x.id === id); return g ? g.name : '分组' }
// 表头两行布局:top = 分组段(colspan)/独立列(rowspan2);bottom = 分组内的二级列
const headerLayout = computed(() => {
  const rc = renderCols.value, top = [], bottom = []
  let i = 0
  while (i < rc.length) {
    const c = rc[i]
    if (c.groupId) {
      let j = i
      while (j < rc.length && rc[j].groupId === c.groupId) j++
      top.push({ type: 'group', name: groupName(c.groupId), span: j - i, key: 'g' + c.groupId + '-' + i })
      for (let k = i; k < j; k++) bottom.push({ col: rc[k], ri: k })
      i = j
    } else { top.push({ type: 'col', col: c, ri: i }); i++ }
  }
  return { top, bottom, hasGroups: bottom.length > 0 }
})

/* —— 冻结列 —— */
const freezeCount = ref(2)   // 含复选列在内冻结的列数(默认 复选 + 名称)
const frozenSet = computed(() => {
  const n = Math.max(0, Math.min(freezeCount.value - 1, renderCols.value.length)) // 减去复选列
  const s = new Set()
  for (let i = 0; i < n; i++) s.add(i)
  return s
})
function leftOffset(i) {
  let x = 44
  for (let k = 0; k < i; k++) x += renderCols.value[k].width
  return x
}

/* —— 多列排序 —— */
const sorts = ref([])   // [{ key, order }]
function sortOf(key) { const idx = sorts.value.findIndex(s => s.key === key); return idx < 0 ? null : { ...sorts.value[idx], idx } }
function setSort(key, order) {
  const rest = sorts.value.filter(s => s.key !== key)
  sorts.value = [{ key, order }, ...rest]   // 最后选择的优先级最高(置首)
  closeHeaderMenu()
}
function quickSort(key) {
  const cur = sortOf(key)
  if (!cur) setSort(key, 'asc')
  else if (cur.order === 'asc') setSort(key, 'desc')
  else clearSort(key)
}
function clearSort(key) { sorts.value = sorts.value.filter(s => s.key !== key); closeHeaderMenu() }
function clearAllSorts() { sorts.value = [] }
const displayRows = computed(() => {
  if (!sorts.value.length) return rows.value
  const arr = [...rows.value]
  const valOf = (r, key) => key === '__name__' ? r.title : key === '__code__' ? r.code : r[key]
  arr.sort((a, b) => {
    for (const s of sorts.value) {
      let va = valOf(a, s.key), vb = valOf(b, s.key)
      let c
      if (typeof va === 'number' && typeof vb === 'number') c = va - vb
      else c = String(va ?? '').localeCompare(String(vb ?? ''), 'zh')
      if (c) return s.order === 'desc' ? -c : c
    }
    return 0
  })
  return arr
})

/* —— 列头菜单 —— */
const headerMenu = ref(null)
const hmenuStyle = ref({})
const hmIsSystem = computed(() => { const c = cols.value.find(x => x.key === headerMenu.value); return !!(c && c.system) })
function toggleHeaderMenu(key, e) {
  if (headerMenu.value === key) { closeHeaderMenu(); return }
  const r = e.currentTarget.getBoundingClientRect()
  const root = rootRef.value.getBoundingClientRect()
  hmenuStyle.value = { left: Math.min(r.left - root.left, root.width - 200) + 'px', top: (r.bottom - root.top + 4) + 'px' }
  headerMenu.value = key
}
function closeHeaderMenu() { headerMenu.value = null }
function hideColumn(key) {
  const c = cols.value.find(x => x.key === key)
  if (c && !c.system) c.visible = false
  closeHeaderMenu()
}

/* —— 列宽拖拽 —— */
function startResize(col, e) {
  const startX = e.clientX, startW = col.width
  const move = (ev) => { col.width = Math.max(70, startW + (ev.clientX - startX)) }
  const up = () => { document.removeEventListener('mousemove', move); document.removeEventListener('mouseup', up) }
  document.addEventListener('mousemove', move); document.addEventListener('mouseup', up)
}

/* —— 列拖拽排序(表头) —— */
const dragKey = ref(null)
function onColDragStart(c, e) { if (c.system) { e.preventDefault(); return } dragKey.value = c.key }
function onColDragOver(c) { /* 视觉提示由 CSS hover 体现 */ }
function onColDrop(target) {
  if (!dragKey.value || target.system || dragKey.value === target.key) return
  reorderCols(dragKey.value, target.key)
  dragKey.value = null
}
function reorderCols(fromKey, toKey) {
  const arr = cols.value
  const fi = arr.findIndex(c => c.key === fromKey), ti = arr.findIndex(c => c.key === toKey)
  if (fi < 0 || ti < 0) return
  const [m] = arr.splice(fi, 1)
  arr.splice(arr.findIndex(c => c.key === toKey) + (ti > fi ? 1 : 0), 0, m)
}

/* —— 配置列弹窗 —— */
const configOpen = ref(false)
const cfgSearch = ref('')
const cfgNoTruncate = ref(false)
const noTruncate = ref(false)
let cfgSnapshot = null, cfgGroupsSnap = null
function openConfig() {
  cfgSnapshot = cols.value.map(c => ({ ...c }))
  cfgGroupsSnap = groups.value.map(g => ({ ...g }))
  cfgNoTruncate.value = noTruncate.value
  cfgPickL.value = new Set(); cfgPickR.value = new Set(); cfgSearch.value = ''
  configOpen.value = true
}
const cfgAvailable = computed(() => {
  const q = cfgSearch.value.trim().toLowerCase()
  return cols.value.filter(c => c.removed && !c.system && (!q || c.label.toLowerCase().includes(q)))
})
const cfgPickL = ref(new Set())
const cfgPickR = ref(new Set())
function pickL(key, e) { togglePick(cfgPickL, key, e) }
function pickR(key, e) { const c = cols.value.find(x => x.key === key); if (c && c.system) return; togglePick(cfgPickR, key, e) }
function togglePick(setRef, key, e) {
  const s = new Set(setRef.value)
  if (!(e && (e.ctrlKey || e.metaKey || e.shiftKey))) s.clear()
  s.has(key) ? s.delete(key) : s.add(key)
  setRef.value = s
}
function moveToSelected() { cfgPickL.value.forEach(k => { const c = cols.value.find(x => x.key === k); if (c) { c.removed = false; c.visible = true } }); cfgPickL.value = new Set() }
function moveToAvailable() { cfgPickR.value.forEach(k => { const c = cols.value.find(x => x.key === k); if (c && !c.system) { c.removed = true; c.groupId = null } }); cfgPickR.value = new Set() }
function toggleEye(key) { const c = cols.value.find(x => x.key === key); if (c && !c.system) c.visible = !c.visible }
const cfgDrag = ref(null)
// 拖到列上:重排到其位置并继承其分组(拖到独立列=移出分组);拖到空白=末尾、移出分组
function cfgDrop(target) {
  if (!cfgDrag.value) return
  const dk = cfgDrag.value; cfgDrag.value = null
  const dragCol = cols.value.find(c => c.key === dk)
  if (!dragCol || dragCol.system) return
  if (!target) { assignGroup(dk, null); return }
  if (dk === target.key) return
  dragCol.groupId = target.groupId || null
  reorderCols(dk, target.key)
}
function dropToGroup(groupId) { if (cfgDrag.value) { assignGroup(cfgDrag.value, groupId); cfgDrag.value = null } }
function restoreDefault() { buildCols(); sorts.value = []; freezeCount.value = 2; cfgNoTruncate.value = false; cfgGroupsSnap = [] }
function applyConfig() { noTruncate.value = cfgNoTruncate.value; cfgSnapshot = null; cfgGroupsSnap = null; configOpen.value = false }
function cancelConfig() {
  if (cfgSnapshot) { cols.value = cfgSnapshot.map(c => ({ ...c })); cfgSnapshot = null }   // 回退实时预览
  if (cfgGroupsSnap) { groups.value = cfgGroupsSnap.map(g => ({ ...g })); cfgGroupsSnap = null }
  configOpen.value = false
}

/* —— 配置列:分组树 —— */
const renameId = ref(null)
const renameText = ref('')
// 已选树:顶层项(独立列 / 分组),分组下挂子列(按 cols 顺序、连续)
const cfgTree = computed(() => {
  const out = []
  const sel = cols.value.filter(c => !c.removed)
  let i = 0
  while (i < sel.length) {
    const c = sel[i]
    if (c.groupId) {
      const g = groups.value.find(x => x.id === c.groupId)
      const children = []
      let j = i
      while (j < sel.length && sel[j].groupId === c.groupId) { children.push(sel[j]); j++ }
      out.push({ type: 'group', id: c.groupId, name: g ? g.name : '分组', collapsed: g ? g.collapsed : false, children })
      i = j
    } else { out.push({ type: 'col', col: c }); i++ }
  }
  // 空分组(无可见子列)也要显示
  for (const g of groups.value) {
    if (!sel.some(c => c.groupId === g.id)) out.push({ type: 'group', id: g.id, name: g.name, collapsed: g.collapsed, children: [] })
  }
  return out
})
function addGroup() {
  const id = 'grp-' + (++gidSeq)
  groups.value = [...groups.value, { id, name: '新分组', collapsed: false }]
  renameId.value = id; renameText.value = '新分组'
}
function toggleGroup(id) { const g = groups.value.find(x => x.id === id); if (g) g.collapsed = !g.collapsed }
function startRename(id, name) { renameId.value = id; renameText.value = name }
function commitRename() {
  const g = groups.value.find(x => x.id === renameId.value)
  if (g && renameText.value.trim()) g.name = renameText.value.trim()
  renameId.value = null
}
// 把列归入某分组(置于该组连续区末尾);groupId=null 表示移出分组
function assignGroup(colKey, groupId) {
  const arr = cols.value
  const ci = arr.findIndex(c => c.key === colKey)
  if (ci < 0 || arr[ci].system) return
  const [m] = arr.splice(ci, 1)
  m.groupId = groupId
  if (groupId) {
    let last = -1
    arr.forEach((c, idx) => { if (c.groupId === groupId) last = idx })
    if (last >= 0) arr.splice(last + 1, 0, m)
    else arr.push(m)   // 空分组:放末尾
  } else {
    arr.push(m)
  }
  cols.value = [...arr]
}

/* —— 选择 / 预览 —— */
const sel = ref(new Set())
const focusId = ref(null)
const previewOpen = ref(false)
const mode = ref('preview')
const modeMenu = ref(false)
const previewWidth = ref(480)
const pvMax = ref(false)
let pvPrevWidth = 480
function toggleMax() {
  if (pvMax.value) { previewWidth.value = pvPrevWidth; pvMax.value = false }
  else { pvPrevWidth = previewWidth.value; previewWidth.value = Math.max(480, (typeof window !== 'undefined' ? window.innerWidth : 1280) - 80); pvMax.value = true }
}
/* —— 对比:左右两列各自独立选择 —— */
const cmpLeftId = ref(null)
const cmpRightId = ref(null)
const cmpPick = ref(null)   // 'L' | 'R' | null
const cmpLeftRow = computed(() => rows.value.find(r => r.id === cmpLeftId.value) || null)
const cmpRightRow = computed(() => rows.value.find(r => r.id === cmpRightId.value) || null)
const cmpSides = computed(() => [
  { key: 'L', id: cmpLeftId.value, row: cmpLeftRow.value },
  { key: 'R', id: cmpRightId.value, row: cmpRightRow.value }
])
function setCmpSide(key, id) { if (key === 'L') cmpLeftId.value = id; else cmpRightId.value = id; cmpPick.value = null }
function initCompare() {
  const s = selectedRows.value.length ? selectedRows.value : (focusRow_.value ? [focusRow_.value] : [])
  if (!cmpLeftId.value || !s.some(r => r.id === cmpLeftId.value)) cmpLeftId.value = s[0] ? s[0].id : null
  if (!cmpRightId.value || !s.some(r => r.id === cmpRightId.value)) cmpRightId.value = s[1] ? s[1].id : (s[0] ? s[0].id : null)
}
// 左右值不同的字段高亮
function cmpDiff(key) {
  const a = cmpLeftRow.value, b = cmpRightRow.value
  if (!a || !b) return false
  return String(a[key] ?? '') !== String(b[key] ?? '')
}
const dragging = ref(false)
/* 抽屉浮层顶部:对齐整个探索区(.ixe-root)顶部,使其铺满黄色区域高度 */
const drawerTop = ref(0)
function measureDrawer() {
  const el = rootRef.value
  if (!el) return
  // 顶部对齐到"探索实例"整块容器(含页签行),抽屉位于其右侧、横跨全高
  const host = el.closest('.ix-root') || el.closest('.ixe-root') || el
  drawerTop.value = Math.max(0, Math.round(host.getBoundingClientRect().top))
}
watch(previewOpen, (v) => { if (v) nextTick(measureDrawer) })
onMounted(() => { measureDrawer(); window.addEventListener('resize', measureDrawer) })
onUnmounted(() => window.removeEventListener('resize', measureDrawer))
function onDragStart(e) {
  dragging.value = true
  const move = (ev) => { const rect = rootRef.value.getBoundingClientRect(); previewWidth.value = Math.max(300, Math.min(rect.right - ev.clientX, rect.width - 320)) }
  const up = () => { dragging.value = false; document.removeEventListener('mousemove', move); document.removeEventListener('mouseup', up) }
  document.addEventListener('mousemove', move); document.addEventListener('mouseup', up); e.preventDefault()
}
watch(sel, (s) => emit('selection-change', [...s]))
const selectedRows = computed(() => rows.value.filter(r => sel.value.has(r.id)))
const focusRow_ = computed(() => rows.value.find(r => r.id === focusId.value) || selectedRows.value[0] || null)
const compareRows = computed(() => selectedRows.value.length ? selectedRows.value.slice(0, 6) : (focusRow_.value ? [focusRow_.value] : []))
const pageIds = computed(() => displayRows.value.map(r => r.id))
const allChecked = computed(() => displayRows.value.length > 0 && pageIds.value.every(id => sel.value.has(id)))
const someChecked = computed(() => !allChecked.value && pageIds.value.some(id => sel.value.has(id)))
function focusRow(r) { focusId.value = r.id; previewOpen.value = true }
function toggleOne(id) {
  const s = new Set(sel.value); s.has(id) ? s.delete(id) : s.add(id); sel.value = s
  if (s.size) previewOpen.value = true
  if (s.has(id)) focusId.value = id
}
function toggleAll() {
  const s = new Set(sel.value)
  if (allChecked.value) pageIds.value.forEach(id => s.delete(id))
  else { pageIds.value.forEach(id => s.add(id)); previewOpen.value = true }
  sel.value = s
}
function setMode(m) {
  mode.value = m
  if (m === 'compare') { previewOpen.value = true; if (previewWidth.value < 680) previewWidth.value = 760; initCompare() }
}

/* —— 关联对象类型 —— */
const linksCache = reactive({})
const linksLoading = ref(false)
const focusLinks = computed(() => linksCache[focusId.value || (focusRow_.value && focusRow_.value.id)] || [])
watch(() => focusRow_.value && focusRow_.value.id, async (id) => {
  if (!id || linksCache[id]) return
  linksLoading.value = true
  try { const d = await instanceApi.detail(props.classId, id); linksCache[id] = d.links || [] } catch { linksCache[id] = [] }
  linksLoading.value = false
})

/* —— 类型/筛选变化 → 重置 —— */
watch(() => [props.classId, props.filterParams], () => {
  sel.value = new Set(); focusId.value = null; sorts.value = []
  Object.keys(linksCache).forEach(k => delete linksCache[k])
  fetchAll()
}, { deep: true, immediate: true })
watch(() => props.columns, buildCols, { immediate: true })

/* —— 展示辅助 —— */
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

/* click-outside 指令(局部) */
const vClickOutside = {
  mounted(el, binding) { el.__h = (e) => { if (!el.contains(e.target)) binding.value(e) }; setTimeout(() => document.addEventListener('mousedown', el.__h), 0) },
  unmounted(el) { document.removeEventListener('mousedown', el.__h) }
}
const vFocus = { mounted(el) { setTimeout(() => el.focus && el.focus(), 0) } }
</script>

<style scoped>
.ilv-root { flex: 1; display: flex; min-height: 0; overflow: hidden; position: relative; }

/* 表格 */
.ilv-table-wrap { flex: 1; min-width: 0; overflow: auto; }
.ilv-table { border-collapse: separate; border-spacing: 0; table-layout: fixed; font-size: 12.5px; }
.ilv-table thead th {
  position: sticky; top: 0; z-index: 3; background: var(--bl-bg-2);
  text-align: center; padding: 0; font-weight: 600; color: var(--bl-text-2);
  border-bottom: 1px solid var(--bl-border); border-right: 1px solid var(--bl-border);
  height: 38px; box-sizing: border-box; white-space: nowrap;
}
.ilv-th { position: relative; }
.ilv-th-in { display: flex; align-items: center; gap: 4px; padding: 0 8px; height: 100%; }
.ilv-th-grip { cursor: grab; display: inline-flex; opacity: .9; }
.ilv-th-grip:hover { opacity: 1; }
.ilv-th-grip:active { cursor: grabbing; }
.ilv-th-label { flex: 1; min-width: 0; text-align: center; cursor: pointer; }
.ilv-th-sort { display: inline-flex; align-items: center; gap: 2px; flex-shrink: 0; padding: 1px 4px; background: var(--bl-primary-soft); border-radius: 4px; }
.ilv-th-prio { font-size: 10px; color: var(--bl-primary); font-weight: 700; }
.ilv-th-menu { width: 22px; height: 22px; border: 0; background: transparent; color: var(--bl-text-1); border-radius: 5px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; opacity: 1; transition: background .12s, color .12s; }
.ilv-th-menu:hover, .ilv-th-menu.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); }
.ilv-th-resizer { position: absolute; top: 0; right: -3px; width: 6px; height: 100%; cursor: col-resize; z-index: 4; }
.ilv-th-resizer:hover { background: var(--bl-primary); }
.ilv-th.is-dragging { opacity: .5; }
/* 一级分组表头(仅视觉,不可交互) */
.ilv-group-th { text-align: center; font-weight: 600; color: var(--bl-text-2); background: var(--bl-bg-3, #f0f2f5) !important; border-right: 1px solid var(--bl-border); }

.ilv-table tbody td { padding: 0 10px; height: 36px; border-bottom: 1px solid var(--bl-divider); border-right: 1px solid var(--bl-border); color: var(--bl-text-1); background: var(--bl-bg-1); text-align: center; }
.ilv-name-td { text-align: left !important; }
.ilv-cell { display: inline-block; max-width: 100%; vertical-align: middle; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ilv-table.is-nowrap-off .ilv-cell { white-space: normal; overflow: visible; }
.ilv-table tbody tr:nth-child(even) td { background: var(--bl-bg-2); }
.ilv-row { cursor: pointer; }
.ilv-row:hover td { background: var(--bl-bg-hover) !important; }
.ilv-row.is-sel td { background: var(--bl-primary-soft) !important; }
.ilv-row.is-focus td:first-child { box-shadow: inset 3px 0 0 var(--bl-primary); }
.ilv-ck-col { width: 44px; text-align: center; }
.ilv-ck-col input { cursor: pointer; }
.ilv-row-ic { width: 22px; height: 22px; border-radius: 5px; display: inline-flex; align-items: center; justify-content: center; vertical-align: middle; margin-right: 8px; }

/* 冻结列 */
.ilv-frozen { position: sticky; z-index: 2; }
thead .ilv-frozen { z-index: 5 !important; }
.ilv-table tbody td.ilv-frozen { background: var(--bl-bg-1); }
.ilv-table tbody tr:nth-child(even) td.ilv-frozen { background: var(--bl-bg-2); }
.ilv-row:hover td.ilv-frozen { background: var(--bl-bg-hover) !important; }
.ilv-row.is-sel td.ilv-frozen { background: var(--bl-primary-soft) !important; }

.ilv-foot { text-align: center; padding: 14px; font-size: 12px; color: var(--bl-text-3); }

/* 列头下拉菜单 */
.ilv-hmenu { position: absolute; z-index: 30; width: 188px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; box-shadow: 0 8px 28px rgba(0,0,0,.16); padding: 4px; }
.ilv-hm-item { display: flex; align-items: center; gap: 8px; padding: 7px 10px; border-radius: 6px; font-size: 12.5px; color: var(--bl-text-1); cursor: pointer; }
.ilv-hm-item:hover { background: var(--bl-bg-hover); }
.ilv-hm-item.is-disabled { opacity: .4; pointer-events: none; }
.ilv-hm-sep { height: 1px; background: var(--bl-divider); margin: 4px 0; }
.ilv-hm-freeze { display: flex; align-items: center; gap: 5px; padding: 7px 10px; font-size: 12.5px; color: var(--bl-text-1); }
.ilv-hm-num { width: 44px; height: 24px; border: 1px solid var(--bl-border); border-radius: 5px; text-align: center; font-size: 12px; }
.ilv-hm-ok { width: 24px; height: 24px; border: 0; background: var(--bl-primary); border-radius: 5px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; margin-left: auto; }

/* 拖拽分隔条(浮层) */
.ilv-divider { position: fixed; bottom: 0; width: 5px; background: var(--bl-border); cursor: col-resize; z-index: 1011; }
.ilv-divider:hover, .ilv-divider.is-active { background: var(--bl-primary); }
.ilv-divider::after { content: ''; position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); width: 2px; height: 26px; background: rgba(255,255,255,.7); border-radius: 2px; }

/* 预览面板(浮层:贴整个探索区右侧、横跨全高) */
.ilv-preview { position: fixed; right: 0; bottom: 0; border-left: 1px solid var(--bl-border); background: var(--bl-bg-1); display: flex; flex-direction: column; overflow: hidden; z-index: 1010; box-shadow: -6px 0 20px rgba(0,0,0,.08); }
.ilv-pv-hd { display: flex; align-items: center; gap: 8px; padding: 10px 12px; border-bottom: 1px solid var(--bl-divider); flex-shrink: 0; }
.ilv-pv-collapse { width: 26px; height: 26px; border: 0; background: transparent; border-radius: 6px; color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.ilv-pv-collapse:hover { background: var(--bl-bg-hover); color: var(--bl-text-1); }
.ilv-pv-title { font-size: 13px; font-weight: 600; color: var(--bl-text-1); }
.ilv-pv-seg { display: inline-flex; border: 1px solid var(--bl-border); border-radius: 6px; overflow: hidden; flex-shrink: 0; }
.ilv-pv-seg button { height: 26px; padding: 0 12px; border: 0; background: var(--bl-bg-1); color: var(--bl-text-2); cursor: pointer; font-size: 12px; }
.ilv-pv-seg button + button { border-left: 1px solid var(--bl-border); }
.ilv-pv-seg button:hover:not(.is-on) { background: var(--bl-bg-hover); }
.ilv-pv-seg button.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }

.ilv-cards { display: flex; flex-wrap: wrap; gap: 8px; padding: 10px; border-bottom: 1px solid var(--bl-divider); max-height: 220px; overflow: auto; flex-shrink: 0; }
.ilv-card { flex: 1 1 150px; min-width: 140px; max-width: 240px; display: flex; align-items: center; gap: 8px; padding: 8px 10px; border: 1px solid var(--bl-border); border-radius: 8px; background: var(--bl-bg-1); cursor: pointer; font-size: 12.5px; box-sizing: border-box; }
.ilv-card:hover { border-color: var(--bl-primary-border); background: var(--bl-bg-hover); }
.ilv-card.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); color: var(--bl-primary); }
.ilv-card-x { border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer; padding: 2px; border-radius: 4px; display: inline-flex; flex-shrink: 0; }
.ilv-card-x:hover { color: #f53f3f; }
.ilv-cards-more { flex: 1 1 100%; font-size: 11px; color: var(--bl-text-3); text-align: center; padding: 4px; }

.ilv-detail { flex: 1; overflow: auto; padding: 12px 14px; }
.ilv-pv-obj { display: flex; align-items: center; gap: 10px; padding-bottom: 10px; }
.ilv-pv-obj-ic { width: 36px; height: 36px; border-radius: 8px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.ilv-sec { font-size: 12px; color: var(--bl-text-3); font-weight: 500; padding-left: 8px; border-left: 3px solid var(--bl-primary); margin: 14px 0 6px; display: flex; align-items: center; gap: 6px; }
.ilv-sec-n { background: var(--bl-bg-3,#f0f2f5); border-radius: 8px; padding: 0 6px; font-size: 10px; }
.ilv-kv-list { display: flex; flex-direction: column; }
.ilv-kv { display: flex; align-items: center; gap: 10px; padding: 7px 2px; border-bottom: 1px solid var(--bl-divider); font-size: 12.5px; }
.ilv-kv-l { width: 110px; flex-shrink: 0; color: var(--bl-text-3); display: inline-flex; align-items: center; gap: 4px; }
.ilv-kv-ic { display: inline-flex; }
.ilv-kv-r { flex: 1; min-width: 0; color: var(--bl-text-1); word-break: break-all; }
.ilv-link { display: flex; align-items: center; gap: 8px; padding: 8px 6px; border-radius: 6px; }
.ilv-link:hover { background: var(--bl-bg-hover); }
.ilv-link-ic { width: 24px; height: 24px; border-radius: 6px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.ilv-link-cnt { font-size: 11px; color: var(--bl-text-3); background: var(--bl-bg-3,#f0f2f5); border-radius: 8px; padding: 1px 7px; }

/* 预览|对比 下拉 */
.ilv-pv-mode { position: relative; display: inline-flex; align-items: center; gap: 5px; height: 26px; padding: 0 8px; border: 1px solid var(--bl-border); border-radius: 6px; cursor: pointer; font-size: 12px; color: var(--bl-text-1); flex-shrink: 0; }
.ilv-pv-mode:hover { border-color: var(--bl-primary-border); }
.ilv-pv-modemenu { position: absolute; top: 100%; right: 0; margin-top: 4px; width: 120px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; box-shadow: 0 8px 24px rgba(0,0,0,.16); padding: 4px; z-index: 20; }
.ilv-pv-modeitem { display: flex; align-items: center; gap: 8px; padding: 7px 10px; border-radius: 6px; font-size: 12.5px; cursor: pointer; color: var(--bl-text-1); }
.ilv-pv-modeitem:hover { background: var(--bl-bg-hover); }
.ilv-pv-modeitem.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); }

/* 对比视图:左右两列,各列各自有一组备选 chips + 详情 */
.ilv-compare2 { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.ilv-cmp-cols { flex: 1; min-height: 0; display: flex; gap: 0; overflow: hidden; }
.ilv-cmp-col { flex: 1; min-width: 0; display: flex; flex-direction: column; overflow: auto; }
.ilv-cmp-col + .ilv-cmp-col { border-left: 1px solid var(--bl-border); }
/* 该列备选项 chips:两行,点选切换该列实例 */
.ilv-cmp-chips { flex-shrink: 0; display: flex; flex-wrap: wrap; align-content: flex-start; gap: 8px; padding: 10px; max-height: 86px; overflow: hidden; border-bottom: 1px solid var(--bl-divider); background: var(--bl-bg-2); }
.ilv-cmp-chip { flex: 1 1 calc(33.333% - 6px); min-width: 96px; max-width: 100%; box-sizing: border-box; display: inline-flex; align-items: center; gap: 6px; padding: 4px 7px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 6px; font-size: 12px; cursor: pointer; }
.ilv-cmp-chip:hover { border-color: var(--bl-primary-border); }
.ilv-cmp-chip.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); color: var(--bl-primary); box-shadow: 0 0 0 1px var(--bl-primary-soft); }
.ilv-cmp-chip .bl-truncate { flex: 1; min-width: 0; }
.ilv-cmp-chip .ilv-row-ic { width: 20px; height: 20px; margin: 0; }
.ilv-cmp-chip-x { border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer; padding: 0; display: inline-flex; flex-shrink: 0; }
.ilv-cmp-chip-x:hover { color: #f53f3f; }
/* 详情头 */
.ilv-cmp-colhd { position: sticky; top: 0; z-index: 2; display: flex; align-items: center; gap: 9px; padding: 10px 12px; background: var(--bl-bg-1); border-bottom: 1px solid var(--bl-divider); }
.ilv-cmp-hdic { width: 32px; height: 32px; margin: 0; border-radius: 8px; }
.ilv-cmp-detail { flex-shrink: 0; }
.ilv-cmp-sec { font-size: 12px; color: var(--bl-text-3); font-weight: 500; padding-left: 8px; border-left: 3px solid var(--bl-primary); margin: 4px 0 6px; }
.ilv-cmp-col .ilv-kv-list { padding: 4px 12px 12px; }
.ilv-cmp-col .ilv-kv.is-diff { background: rgba(245,63,63,.06); border-radius: 4px; }
.ilv-cmp-col .ilv-kv.is-diff .ilv-kv-r { color: #f53f3f; font-weight: 500; }

/* 配置列弹窗 */
.ilv-cfg-mask { position: fixed; inset: 0; z-index: 1300; background: rgba(0,0,0,.32); display: flex; align-items: center; justify-content: center; }
.ilv-cfg { width: 720px; max-width: 94vw; max-height: 86vh; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 10px; box-shadow: 0 16px 48px rgba(0,0,0,.24); display: flex; flex-direction: column; }
.ilv-cfg-hd { display: flex; align-items: center; gap: 8px; padding: 14px 16px; border-bottom: 1px solid var(--bl-divider); font-size: 15px; }
.ilv-cfg-x { width: 28px; height: 28px; border: 0; background: transparent; border-radius: 6px; color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.ilv-cfg-x:hover { background: var(--bl-bg-hover); }
.ilv-cfg-body { flex: 1; min-height: 0; display: flex; gap: 0; padding: 14px 16px; }
.ilv-cfg-pane { flex: 1; min-width: 0; display: flex; flex-direction: column; border: 1px solid var(--bl-border); border-radius: 8px; padding: 10px; }
.ilv-cfg-pt { font-size: 12px; font-weight: 600; color: var(--bl-text-2); margin-bottom: 8px; display: flex; align-items: center; }
.ilv-cfg-addg { width: 22px; height: 22px; border: 1px solid var(--bl-border); background: var(--bl-bg-1); border-radius: 5px; color: var(--bl-text-2); cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.ilv-cfg-addg:hover { border-color: var(--bl-primary); color: var(--bl-primary); }
.ilv-cfg-group { display: flex; align-items: center; gap: 5px; padding: 6px 6px; border-radius: 6px; font-size: 12.5px; font-weight: 600; color: var(--bl-text-1); background: var(--bl-bg-2); }
.ilv-cfg-fold { width: 18px; height: 18px; border: 0; background: transparent; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; padding: 0; }
.ilv-cfg-folder { display: inline-flex; }
.ilv-cfg-rename { flex: 1; min-width: 0; height: 22px; border: 1px solid var(--bl-primary); border-radius: 4px; padding: 0 6px; font-size: 12.5px; }
.ilv-cfg-gn { font-size: 10px; color: var(--bl-text-3); background: var(--bl-bg-3,#f0f2f5); border-radius: 8px; padding: 0 6px; }
.ilv-cfg-item.is-child { margin-left: 18px; }
.ilv-cfg-gempty { margin-left: 18px; font-size: 11px; color: var(--bl-text-3); padding: 8px; border: 1px dashed var(--bl-border); border-radius: 6px; text-align: center; }
.ilv-cfg-list { flex: 1; min-height: 220px; max-height: 360px; overflow: auto; display: flex; flex-direction: column; gap: 2px; }
.ilv-cfg-item { display: flex; align-items: center; gap: 6px; padding: 7px 8px; border-radius: 6px; font-size: 12.5px; cursor: pointer; color: var(--bl-text-1); }
.ilv-cfg-item:hover { background: var(--bl-bg-hover); }
.ilv-cfg-item.is-pick { background: var(--bl-primary-soft); color: var(--bl-primary); }
.ilv-cfg-item.is-hidden { color: var(--bl-text-3); }
.ilv-cfg-item.is-sys { cursor: default; }
.ilv-cfg-grip { cursor: grab; display: inline-flex; opacity: .5; }
.ilv-cfg-sys { font-size: 10px; color: var(--bl-text-3); margin-left: 6px; background: var(--bl-bg-3,#f0f2f5); border-radius: 6px; padding: 0 5px; }
.ilv-cfg-ic { display: inline-flex; flex-shrink: 0; }
.ilv-cfg-eye { width: 22px; height: 22px; border: 0; background: transparent; border-radius: 4px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ilv-cfg-eye:hover { background: var(--bl-bg-hover); }
.ilv-cfg-empty { font-size: 12px; color: var(--bl-text-3); padding: 12px; text-align: center; }
.ilv-cfg-ops { display: flex; flex-direction: column; justify-content: center; gap: 8px; padding: 0 12px; }
.ilv-cfg-op { width: 32px; height: 32px; border: 1px solid var(--bl-border); background: var(--bl-bg-1); border-radius: 6px; color: var(--bl-text-2); cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.ilv-cfg-op:hover:not(:disabled) { border-color: var(--bl-primary); color: var(--bl-primary); }
.ilv-cfg-op:disabled { opacity: .4; cursor: not-allowed; }
.ilv-cfg-ft { display: flex; align-items: center; gap: 12px; padding: 12px 16px; border-top: 1px solid var(--bl-divider); background: var(--bl-bg-2); border-radius: 0 0 10px 10px; }
.ilv-cfg-wrap { display: inline-flex; align-items: center; gap: 5px; font-size: 12px; color: var(--bl-text-2); cursor: pointer; }
</style>
