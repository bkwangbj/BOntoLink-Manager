<template>
  <Teleport to="body">
    <transition name="wz-fade">
      <div v-if="open" class="wz-mask">
        <div class="wz-modal">
          <!-- 顶部标题栏 -->
          <div class="wz-hd">
            <div>
              <div class="wz-title">新建对象类型</div>
              <div class="wz-sub bl-muted">步骤 {{ step }} / 2 · {{ step === 1 ? '数据源绑定' : '对象类型详情' }}</div>
            </div>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="onCancel" v-html="BL.icon('x', 14)"></button>
          </div>

          <!-- 步骤条 -->
          <div class="wz-steps">
            <div :class="['wz-step', step >= 1 && 'is-on']"><span class="num">1</span> 数据源绑定</div>
            <div class="wz-step-bar"></div>
            <div :class="['wz-step', step >= 2 && 'is-on']"><span class="num">2</span> 对象类型详情</div>
          </div>

          <!-- 主体 -->
          <div class="wz-body">
            <!-- 模式选择 -->
            <div class="mode-row">
              <div :class="['mode-card', form.mode==='exist' && 'is-on']" @click="form.mode='exist'">
                <span class="mode-ic" v-html="BL.icon('database', 18, '#fff')"></span>
                <div>
                  <div class="mode-title">使用已经存在的数据源</div>
                  <div class="bl-muted" style="font-size:12px">关联物理库表，自动映射字段为属性，适用基于现有业务数据建模</div>
                </div>
                <span class="mode-check" v-if="form.mode==='exist'" v-html="BL.icon('check', 12, '#fff')"></span>
              </div>
              <div :class="['mode-card', form.mode==='none' && 'is-on']" @click="form.mode='none'">
                <span class="mode-ic" style="background:#722ED1" v-html="BL.icon('cube', 18, '#fff')"></span>
                <div>
                  <div class="mode-title">不使用数据源</div>
                  <div class="bl-muted" style="font-size:12px">纯逻辑建模，不关联物理表（权限对象 / 虚拟对象 / 后补数据源）</div>
                </div>
                <span class="mode-check" v-if="form.mode==='none'" v-html="BL.icon('check', 12, '#fff')"></span>
              </div>
            </div>

            <!-- 数据源关联配置 -->
            <template v-if="form.mode === 'exist'">
              <!-- 先选数据源, 再列其物理表 -->
              <div class="ds-select-bar">
                <span class="ds-select-lbl">数据源 <span style="color:#f53f3f">*</span></span>
                <select class="bl-input" style="max-width:320px" v-model="form.dsId" @change="onDsChange">
                  <option value="">— 请选择数据源 —</option>
                  <option v-for="d in datasources" :key="d.id" :value="d.id">{{ d.dsName }}（{{ d.dsCode }}）</option>
                </select>
                <span class="bl-muted" style="font-size:11px;margin-left:8px">先选数据源，再从其已同步的物理表中选择主表</span>
              </div>

              <div v-if="!form.dsId" class="bl-empty" style="padding:40px;font-size:13px">请先选择数据源</div>
              <div v-else class="ds-area">
                <!-- 左：主表 -->
                <div class="ds-panel">
                  <div class="ds-panel-hd">
                    <div class="ds-panel-title">主表 <span class="bl-muted" style="font-size:11px">(Primary Backing Dataset)</span></div>
                  </div>
                  <div class="ds-panel-body">
                    <div v-if="form.main.physical_table" class="main-form">
                      <FieldRow label="物理表" inline><span class="bl-mono bl-tag">{{ form.main.physical_table }}</span></FieldRow>
                      <FieldRow label="表名称" inline hint="可输入中文名, 或从下拉选友好名/物理名">
                        <input class="bl-input" v-model="form.main.alias_name" list="main-alias-options" placeholder="输入表中文名" />
                        <datalist id="main-alias-options">
                          <option v-for="o in subAliasOptions(form.main.physical_table)" :key="'main-alias-'+o.value" :value="o.value">{{ o.label }}</option>
                        </datalist>
                      </FieldRow>
                      <FieldRow label="主键字段" inline>
                        <div class="pk-list">
                          <div v-for="(k, i) in form.main.pk_keys" :key="'pk-'+i"
                               class="pk-row"
                               draggable="true"
                               @dragstart="onPkDragStart(i, $event)"
                               @dragover.prevent
                               @drop="onPkDrop(i)"
                               @dragend="pkDragIdx = null"
                               :class="{ 'is-dragging': pkDragIdx === i }">
                            <span class="pk-seq" :title="'拖拽 #'+ (i+1) +' 调整顺序'">{{ i + 1 }}</span>
                            <select class="bl-input bl-input-sm bl-mono pk-select" v-model="form.main.pk_keys[i]">
                              <option value="">— 主键字段 —</option>
                              <option v-for="c in mainTableColumns" :key="'pk-opt-'+i+'-'+c.name" :value="c.name">{{ c.name }}</option>
                            </select>
                            <button v-if="form.main.pk_keys.length > 1" class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm"
                                    title="移除该主键字段"
                                    @click="form.main.pk_keys.splice(i, 1)" v-html="BL.icon('x', 11)"></button>
                          </div>
                          <button class="bl-btn bl-btn-text bl-btn-sm pk-add" @click="form.main.pk_keys.push('')"
                                  title="添加主键字段 (复合主键)">
                            <span v-html="BL.icon('plus', 11)"></span><span style="margin-left:4px">添加字段</span>
                          </button>
                        </div>
                      </FieldRow>
                      <div style="text-align:right;margin-top:6px">
                        <button class="bl-btn bl-btn-sm bl-btn-text" @click="resetMain">
                          <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">删除重选</span>
                        </button>
                      </div>
                    </div>
                    <div v-else class="inline-tbl-pick">
                      <input class="bl-input bl-input-sm" placeholder="搜索物理表 / 视图" v-model="tablePickerQ" style="margin-bottom:6px" />
                      <div class="inline-tbl-list">
                        <div v-for="t in mainTableOptions" :key="t.physical_table" class="tbl-row" @click="pickMain(t)">
                          <span class="tbl-side" style="background:#165DFF"></span>
                          <span class="bl-mono" style="font-weight:500">{{ t.physical_table }}</span>
                          <span class="bl-tag" :class="t.type === 'view' ? 'bl-tag-warning' : 'bl-tag-primary'" style="margin-left:6px;font-size:11px">{{ t.type === 'view' ? '视图' : '表' }}</span>
                          <span class="bl-muted" style="font-size:11px;margin-left:6px">{{ t.column_count }} 字段</span>
                        </div>
                        <div v-if="!mainTableOptions.length" class="bl-empty" style="padding:16px;font-size:12px">
                          {{ tables.length ? '无匹配物理表' : '该数据源暂无物理表，请先到「数据源 → 物理表」同步' }}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 右：附表 -->
                <div class="ds-panel ds-sub-panel">
                  <div class="ds-panel-hd">
                    <div class="ds-panel-title">附表 <span class="bl-muted" style="font-size:11px">(Additional Backing Datasets)</span></div>
                    <button class="bl-btn bl-btn-sm" :disabled="!form.main.physical_table" @click="openSubPicker">
                      <span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">添加附表</span>
                    </button>
                  </div>
                  <div class="ds-panel-body">
                    <table class="bl-table sub-table">
                      <thead>
                        <tr><th style="width:30px">#</th><th>物理表</th><th>表名称</th><th>关联字段</th><th>连接</th><th></th></tr>
                      </thead>
                      <tbody>
                        <tr v-for="(s, i) in form.subs" :key="s.physical_table"
                            draggable="true"
                            @dragstart="dragIdx=i" @dragover.prevent
                            @drop="dropSub(i)">
                          <td class="t-center bl-muted" style="cursor:grab" :title="`拖拽调整顺序 #${i+1}`">{{ i+1 }}</td>
                          <td><span class="bl-mono bl-tag">{{ s.physical_table }}</span></td>
                          <td>
                            <input class="bl-input bl-input-xs" v-model="s.alias_name" :list="'alias-list-'+i" placeholder="表中文名" />
                            <datalist :id="'alias-list-'+i">
                              <option v-for="o in subAliasOptions(s.physical_table)" :key="'alias-'+i+'-'+o.value" :value="o.value">{{ o.label }}</option>
                            </datalist>
                          </td>
                          <td>
                            <select class="bl-input bl-input-xs bl-mono" v-model="s.join_on_keys">
                              <option value="">— 关联字段 —</option>
                              <option v-for="c in subColumns(s.physical_table)" :key="'join-'+i+'-'+c.name" :value="c.name">{{ c.name }}</option>
                            </select>
                          </td>
                          <td>
                            <select class="bl-input bl-input-xs" v-model="s.join_type">
                              <option value="LEFT">LEFT</option>
                              <option value="INNER">INNER</option>
                              <option value="RIGHT">RIGHT</option>
                              <option value="FULL">FULL</option>
                            </select>
                          </td>
                          <td @click.stop>
                            <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="form.subs.splice(i,1)" title="移除" v-html="BL.icon('x', 12)"></button>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                    <div v-if="!form.subs.length" class="bl-empty" style="padding:24px;font-size:12px">未关联附表（最多建议 ≤ 3 个）</div>
                  </div>
                </div>
              </div>

              <!-- 物理 ~ 对象属性映射 -->
              <div class="map-area">
                <div class="map-hd">
                  <div class="map-title">物理~对象属性映射 <span class="bl-muted" style="font-size:11px">（自动加载所有选中表字段；可批量编辑）</span></div>
                  <div class="bl-row" style="gap:8px;align-items:center">
                    <select class="bl-input bl-input-xs" v-model="propFilterTable" style="width:140px">
                      <option value="">全部物理表</option>
                      <option v-for="t in allMappedTables" :key="t" :value="t">{{ t }}</option>
                    </select>
                    <div class="map-search">
                      <span v-html="BL.icon('search', 12)"></span>
                      <input class="bl-input bl-input-xs" v-model="propQ" placeholder="搜索字段 / 编码" />
                    </div>
                    <button v-if="propChecked.size" class="bl-btn bl-btn-sm bl-btn-danger" @click="batchDelProps">
                      <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">批量删除 ({{ propChecked.size }})</span>
                    </button>
                  </div>
                </div>
                <div class="map-table-wrap">
                  <table class="bl-table map-table">
                    <thead>
                      <!-- 行1: 分组横幅 (物理 / 属性 + 两端空位用配色填充) -->
                      <tr>
                        <th class="th-corner-l" style="width:24px"></th>
                        <th class="th-corner-l" style="width:34px"></th>
                        <th colspan="2" class="th-group bg-l">物理</th>
                        <th colspan="7" class="th-group bg-c">属性</th>
                        <th class="th-corner-r" style="width:48px"></th>
                      </tr>
                      <!-- 行2: 列名 -->
                      <tr>
                        <th class="th-col-l" title="拖拽手柄"></th>
                        <th class="th-col-l"><input type="checkbox" :checked="allPropsChecked" @change="togglePropAll" /></th>
                        <th class="bg-l">表</th><th class="bg-l">字段</th>
                        <th class="bg-c">代码</th><th class="bg-c">名称</th><th class="bg-c">数据类型</th>
                        <th class="bg-c t-center">主键</th><th class="bg-c t-center">必填</th>
                        <th class="bg-c t-center">多值</th><th class="bg-c t-center" title="值域约束">约束</th>
                        <th class="th-col-r">操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="p in propsView" :key="p._key"
                          draggable="true"
                          @dragstart="onPropDragStart(p)" @dragover.prevent
                          @drop="onPropDrop(p)"
                          :class="{ 'is-dragging': propDragKey === p._key }">
                        <td class="prop-grip t-center bl-muted" :title="`拖拽调整顺序`"
                            v-html="BL.icon('move', 12, '#86909c')"></td>
                        <td @click.stop><input type="checkbox" :checked="propChecked.has(p._key)" @change="togglePropCheck(p._key)" /></td>
                        <td class="bl-mono bl-muted">{{ p.physical_table }}</td>
                        <td class="bl-mono bl-muted">{{ p.physical_column }}</td>
                        <td><input class="bl-input bl-input-xs bl-mono" v-model="p.api_name" /></td>
                        <td><input class="bl-input bl-input-xs" v-model="p.display_name" /></td>
                        <td>
                          <select class="bl-input bl-input-xs" v-model="p.data_type">
                            <option v-for="t in xsdTypes" :key="t" :value="t">{{ t }}</option>
                          </select>
                        </td>
                        <td class="t-center"><input type="checkbox" v-model="p.is_key" :true-value="1" :false-value="0" /></td>
                        <td class="t-center"><input type="checkbox" v-model="p.is_required" :true-value="1" :false-value="0" /></td>
                        <td class="t-center"><input type="checkbox" v-model="p.is_multi_valued_prop" :true-value="1" :false-value="0" /></td>
                        <td class="t-center"><input type="checkbox" v-model="p.is_range_constraint_prop" :true-value="1" :false-value="0" /></td>
                        <td @click.stop>
                          <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="移除" @click="removeProp(p._key)" v-html="BL.icon('trash', 12)"></button>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                  <div v-if="!propsView.length" class="bl-empty" style="padding:32px;font-size:12px">{{ form.main.physical_table ? '当前筛选无字段；点击主表/附表后自动生成字段映射' : '请先选择主表' }}</div>
                </div>
              </div>
            </template>

            <!-- 不使用数据源 -->
            <div v-else class="empty-mode">
              <div class="bl-empty-icon" v-html="BL.icon('cube', 40)"></div>
              <div style="margin-top:8px;font-weight:500">不绑定物理数据源</div>
              <div class="bl-muted" style="font-size:12px;margin-top:6px;max-width:420px;text-align:center">
                直接进入对象类型详情页（纯逻辑建模），适用于权限对象、虚拟对象、后续再补充数据源的场景
              </div>
            </div>
          </div>

          <!-- 底部操作栏 -->
          <div class="wz-ft">
            <button class="bl-btn" @click="onCancel">取消</button>
            <button class="bl-btn bl-btn-primary" @click="goNext">
              下一步 → 对象类型详情<span v-html="BL.icon('chevronRight', 12, '#fff')" style="margin-left:4px"></span>
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 物理表选择面板 -->
    <div v-if="tablePickerOpen" class="bl-modal-mask" @click.self="tablePickerOpen=false">
      <div class="bl-modal" style="width:520px;max-height:78vh;display:flex;flex-direction:column">
        <div class="bl-modal-hd">
          {{ tablePickerMode === 'main' ? '选择主表' : '添加附表' }}（{{ tablePickerMode === 'main' ? '单选' : '多选' }}）
        </div>
        <div class="bl-modal-body" style="display:flex;flex-direction:column;gap:8px;overflow:hidden">
          <input class="bl-input" placeholder="搜索物理表" v-model="tablePickerQ" />
          <div style="flex:1;overflow:auto">
            <div v-for="t in tableOptions" :key="t.physical_table"
                 :class="['tbl-row', tableSelected.has(t.physical_table) && 'is-on', t._disabled && 'is-disabled']"
                 @click="t._disabled || pickTable(t)">
              <span class="tbl-side" :style="{ background: t._disabled ? '#86909C' : '#165DFF' }"></span>
              <span class="bl-mono" style="font-weight:500">{{ t.physical_table }}</span>
              <span class="bl-tag" :class="t.type === 'view' ? 'bl-tag-warning' : 'bl-tag-primary'" style="margin-left:6px;font-size:11px">{{ t.type === 'view' ? '视图' : '表' }}</span>
              <span class="bl-muted" style="font-size:11px;margin-left:6px">{{ t.column_count }} 字段</span>
              <span v-if="t._disabled" class="bl-tag" style="margin-left:auto">已选</span>
            </div>
          </div>
        </div>
        <div class="bl-modal-ft">
          <button class="bl-btn" @click="tablePickerOpen=false">取消</button>
          <button v-if="tablePickerMode==='sub'" class="bl-btn bl-btn-primary" @click="confirmSubs">确定 ({{ tableSelected.size }})</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, reactive } from 'vue'
import { BL } from '@/lib/bl.js'
import { physicalTableApi, datasourceApi } from '@/api'
import FieldRow from '@/views/config/category/FieldRow.vue'

const props = defineProps({
  open: { type: Boolean, default: false }
})
const emit = defineEmits(['update:open', 'next', 'cancel'])

/* ---------- 内置 mock 物理表清单（无后端时演示用） ---------- */
const MOCK_TABLES = [
  { physical_table: 't_hydrology_station', display_name: '水文测站', column_count: 12, columns: [
    { name: 'station_code', type: 'string' }, { name: 'station_name', type: 'string' },
    { name: 'lng', type: 'decimal' }, { name: 'lat', type: 'decimal' },
    { name: 'altitude', type: 'decimal' }, { name: 'install_date', type: 'date' }
  ]},
  { physical_table: 't_river', display_name: '河流', column_count: 6, columns: [
    { name: 'river_code', type: 'string' }, { name: 'name', type: 'string' },
    { name: 'length_km', type: 'decimal' }, { name: 'basin_id', type: 'string' }
  ]},
  { physical_table: 't_water_quality', display_name: '水质监测', column_count: 8, columns: [
    { name: 'sample_id', type: 'string' }, { name: 'sampled_at', type: 'dateTime' },
    { name: 'station_code', type: 'string' }, { name: 'cod', type: 'decimal' },
    { name: 'ph', type: 'decimal' }
  ]},
  { physical_table: 't_reservoir', display_name: '水库', column_count: 9, columns: [
    { name: 'res_no', type: 'string' }, { name: 'res_name', type: 'string' },
    { name: 'basin_code', type: 'string' }, { name: 'capacity', type: 'decimal' }
  ]},
  { physical_table: 't_water_basin_info', display_name: '流域信息', column_count: 5, columns: [
    { name: 'res_serial', type: 'string' }, { name: 'basin_id', type: 'string' },
    { name: 'basin_name', type: 'string' }
  ]},
  { physical_table: 't_water_level_monitor', display_name: '水位监测', column_count: 7, columns: [
    { name: 'reservoir_sn', type: 'string' }, { name: 'basin_mark', type: 'string' },
    { name: 'level', type: 'decimal' }, { name: 'observed_at', type: 'dateTime' }
  ]}
]

/* 数据源 + 其已同步的物理表清单 */
const datasources = ref([])
const tables = ref([])
async function loadDatasources() {
  datasources.value = await datasourceApi.list().catch(() => [])
}
async function loadTables(dsId) {
  if (!dsId) { tables.value = []; return }
  tables.value = await physicalTableApi.list(dsId).catch(() => [])
}
function onDsChange() {
  // 切换数据源: 清空已选主表/附表/属性, 重新按数据源加载物理表
  form.main = { physical_table: '', alias_name: '', pk_keys: [''] }
  form.subs = []
  form.props = []
  loadTables(form.dsId)
}

const xsdTypes = ['xsd:string','xsd:decimal','xsd:integer','xsd:boolean','xsd:dateTime','xsd:date','xsd:anyURI']

const step = ref(1)
const form = reactive({
  mode: 'exist',
  dsId: '',
  main: { physical_table: '', alias_name: '', pk_keys: [''] },
  subs: [],
  props: []   // { _key, physical_table, physical_column, api_name, display_name, data_type, is_key, is_required, is_multi_valued_prop, is_range_constraint_prop }
})

// 重置（每次打开时）
function resetAll() {
  step.value = 1
  form.mode = 'exist'
  form.dsId = ''
  form.main = { physical_table: '', alias_name: '', pk_keys: [''] }
  form.subs = []
  form.props = []
  tables.value = []
  propChecked.value = new Set()
  propFilterTable.value = ''
  propQ.value = ''
}
watch(() => props.open, (v) => { if (v) { resetAll(); loadDatasources() } })

/* ---------- 物理表选择 ---------- */
const tablePickerOpen = ref(false)
const tablePickerMode = ref('main')   // main | sub
const tablePickerQ = ref('')
const tableSelected = ref(new Set())

function openSubPicker() {
  if (!form.main.physical_table) { BL.warning('请先选择主表'); return }
  tablePickerMode.value = 'sub'; tableSelected.value = new Set(); tablePickerOpen.value = true; tablePickerQ.value = ''
}
/* 主表内联候选: 该数据源已同步的物理表(排除已作为附表的), 支持搜索 */
const mainTableOptions = computed(() => {
  const used = new Set(form.subs.map(s => s.physical_table))
  const k = tablePickerQ.value.trim().toLowerCase()
  return tables.value.filter(t => !used.has(t.physical_table) && (!k || t.physical_table.toLowerCase().includes(k)))
})
function pickMain(t) {
  form.main.physical_table = t.physical_table
  form.main.alias_name = t.display_name || t.physical_table
  form.main.pk_keys = [(t.columns?.[0]?.name) || '']
  tablePickerQ.value = ''
  syncPropsFromTables()
}

const tableOptions = computed(() => {
  const used = new Set([form.main.physical_table, ...form.subs.map(s => s.physical_table)])
  const k = tablePickerQ.value.trim().toLowerCase()
  return tables.value
    .filter(t => !k || t.physical_table.toLowerCase().includes(k))
    .map(t => ({ ...t, _disabled: used.has(t.physical_table) }))
})

function pickTable(t) {
  if (tablePickerMode.value === 'main') {
    form.main.physical_table = t.physical_table
    form.main.alias_name = t.display_name || t.physical_table     // 优先用中文友好名
    form.main.pk_keys = [(t.columns?.[0]?.name) || '']
    syncPropsFromTables()
    tablePickerOpen.value = false
  } else {
    const s = new Set(tableSelected.value)
    s.has(t.physical_table) ? s.delete(t.physical_table) : s.add(t.physical_table)
    tableSelected.value = s
  }
}
function confirmSubs() {
  const used = new Set(form.subs.map(s => s.physical_table))
  for (const tname of tableSelected.value) {
    if (used.has(tname)) continue
    const meta = tables.value.find(t => t.physical_table === tname)
    form.subs.push({
      physical_table: tname,
      alias_name: meta?.display_name || tname,    // 默认用中文友好名 (可下拉切回物理名)
      join_on_keys: (meta?.columns?.[0]?.name) || '',
      join_type: 'LEFT'
    })
  }
  syncPropsFromTables()
  tablePickerOpen.value = false
}
function resetMain() {
  form.main = { physical_table: '', alias_name: '', pk_keys: [''] }
  form.subs = []
  form.props = []
}

function dropSub(target) {
  if (dragIdx.value == null || dragIdx.value === target) return
  const arr = form.subs
  const item = arr.splice(dragIdx.value, 1)[0]
  arr.splice(target, 0, item)
  dragIdx.value = null
}
const dragIdx = ref(null)

/* 已选主表的列清单 — 主键字段下拉 / 关联字段提示用 */
const mainTableColumns = computed(() => {
  const t = tables.value.find(x => x.physical_table === form.main.physical_table)
  return t?.columns || []
})

/* 主键字段拖拽排序 */
const pkDragIdx = ref(null)
function onPkDragStart(i, ev) {
  pkDragIdx.value = i
  // 显式声明 move 效果, 让浏览器禁用 IO 形态 (避免拖到 select 上变成"禁止")
  if (ev?.dataTransfer) ev.dataTransfer.effectAllowed = 'move'
}
function onPkDrop(targetIdx) {
  const from = pkDragIdx.value
  pkDragIdx.value = null
  if (from == null || from === targetIdx) return
  const arr = form.main.pk_keys
  if (from < 0 || from >= arr.length || targetIdx < 0 || targetIdx >= arr.length) return
  const [item] = arr.splice(from, 1)
  arr.splice(targetIdx, 0, item)
}

/* 附表行的辅助查询 (基于 physical_table 拿元信息) */
function subColumns(physTable) {
  const t = tables.value.find(x => x.physical_table === physTable)
  return t?.columns || []
}
function subAliasOptions(physTable) {
  const t = tables.value.find(x => x.physical_table === physTable)
  if (!t) return []
  const opts = []
  if (t.display_name && t.display_name !== t.physical_table) opts.push({ value: t.display_name, label: t.display_name + ' (友好名)' })
  opts.push({ value: t.physical_table, label: t.physical_table + ' (物理名)' })
  return opts
}

/* 属性映射: 行拖拽排序 (通过 _key 映射回 form.props, 兼容筛选视图) */
const propDragKey = ref(null)
function onPropDragStart(p) { propDragKey.value = p._key }
function onPropDrop(targetP) {
  const from = propDragKey.value
  propDragKey.value = null
  if (!from || !targetP || from === targetP._key) return
  const arr = form.props
  const fi = arr.findIndex(p => p._key === from)
  const ti = arr.findIndex(p => p._key === targetP._key)
  if (fi < 0 || ti < 0) return
  const [item] = arr.splice(fi, 1)
  arr.splice(ti, 0, item)
}

/* ---------- 属性映射 ---------- */
const propFilterTable = ref('')
const propQ = ref('')
const propChecked = ref(new Set())

function syncPropsFromTables() {
  const existing = new Set(form.props.map(p => p.physical_table + ':' + p.physical_column))
  const out = [...form.props]
  const tableNames = [form.main.physical_table, ...form.subs.map(s => s.physical_table)].filter(Boolean)
  for (const tname of tableNames) {
    const meta = tables.value.find(t => t.physical_table === tname)
    if (!meta) continue
    for (const col of (meta.columns || [])) {
      const key = tname + ':' + col.name
      if (existing.has(key)) continue
      out.push({
        _key: key,
        physical_table: tname,
        physical_column: col.name,
        api_name: col.name.toLowerCase(),
        display_name: (col.comment && col.comment.trim()) || col.name,   // 优先用注释, 无注释退回字段名
        data_type: 'xsd:' + (col.type === 'dateTime' ? 'dateTime' : col.type === 'date' ? 'date' : col.type === 'decimal' ? 'decimal' : col.type === 'integer' ? 'integer' : col.type === 'boolean' ? 'boolean' : 'string'),
        is_key: col.is_key ? 1 : 0, is_required: col.is_required ? 1 : 0, is_multi_valued_prop: 0, is_range_constraint_prop: 0   // 自动标识主键/必填
      })
    }
  }
  // 过滤掉已不存在的表的字段
  const validTables = new Set(tableNames)
  form.props = out.filter(p => validTables.has(p.physical_table))
}

const allMappedTables = computed(() => [...new Set(form.props.map(p => p.physical_table))])
const propsView = computed(() => {
  let list = form.props
  if (propFilterTable.value) list = list.filter(p => p.physical_table === propFilterTable.value)
  const k = propQ.value.trim().toLowerCase()
  if (k) list = list.filter(p => [p.api_name, p.display_name, p.physical_column].some(s => String(s).toLowerCase().includes(k)))
  return list
})
const allPropsChecked = computed(() => propsView.value.length > 0 && propsView.value.every(p => propChecked.value.has(p._key)))
function togglePropCheck(k) { const s = new Set(propChecked.value); s.has(k) ? s.delete(k) : s.add(k); propChecked.value = s }
function togglePropAll() {
  const s = new Set(propChecked.value)
  if (allPropsChecked.value) propsView.value.forEach(p => s.delete(p._key))
  else propsView.value.forEach(p => s.add(p._key))
  propChecked.value = s
}
function removeProp(k) { form.props = form.props.filter(p => p._key !== k) }
async function batchDelProps() {
  const n = propChecked.value.size
  if (!n) return
  const ok = await BL.confirm({ title:'批量删除', content:`确定删除选中的 ${n} 条属性？`, danger:true, okText:'删除' })
  if (!ok) return
  form.props = form.props.filter(p => !propChecked.value.has(p._key))
  propChecked.value = new Set()
}

function onCancel() { emit('cancel'); emit('update:open', false) }
function goNext() {
  if (form.mode === 'exist' && !form.main.physical_table) { BL.warning('请先选择主表'); return }
  // 带上所选数据源的编码/领域, 供父级创建对象类型用
  const ds = datasources.value.find(d => d.id === form.dsId)
  const payload = JSON.parse(JSON.stringify(form))
  payload.dsCode = ds?.dsCode || ds?.ds_code || ''
  payload.categoryCode = ds?.categoryCode || ds?.category_code || ''
  emit('next', payload)
  emit('update:open', false)
}
</script>

<style scoped>
.wz-mask {
  position: fixed; inset: 0;
  background: rgba(0,0,0,.55);
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
  z-index: 998;
  display: flex; align-items: center; justify-content: center;
}
.wz-modal {
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: 12px;
  width: 1100px; max-width: 96vw;
  /* 固定高度：视口高度 - 112px；切换模式或字段映射条数变化时，弹框尺寸保持不变 */
  height: calc(100vh - 112px);
  min-height: 480px;
  display: flex; flex-direction: column; overflow: hidden;
  /* 双层阴影 + 内侧高光, 让弹框在深色下有明显立体感 */
  box-shadow:
    0 24px 56px rgba(0,0,0,0.55),
    0 4px 12px rgba(0,0,0,0.3);
}
/* 深色下: 边框加强 + 顶部 1px 高光描边模拟立体浮起 */
:root[data-theme="dark"] .wz-mask { background: rgba(0,0,0,.65); }
:root[data-theme="dark"] .wz-modal {
  border-color: var(--bl-border-strong);
  box-shadow:
    0 24px 60px rgba(0,0,0,0.75),
    0 6px 16px rgba(0,0,0,0.5),
    inset 0 1px 0 rgba(255,255,255,0.06);
}
.wz-hd { display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; border-bottom: 1px solid var(--bl-divider); }
.wz-title { font-size: 16px; font-weight: 600; }
.wz-sub { font-size: 12px; }

.wz-steps { display: flex; align-items: center; padding: 12px 16px; gap: 12px; background: var(--bl-bg-2); }
.wz-step { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; color: var(--bl-text-3); }
.wz-step .num { width: 22px; height: 22px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center;
  background: var(--bl-bg-3); color: var(--bl-text-2); font-weight: 600; font-size: 12px; }
.wz-step.is-on { color: var(--bl-primary); font-weight: 500; }
.wz-step.is-on .num { background: var(--bl-primary); color: #fff; }
.wz-step-bar { flex: 0 0 80px; height: 2px; background: var(--bl-divider); }

/* wz-body 改 flex: 1 + overflow: hidden, 让里面的 ds-area / map-area 各自 flex 控制高度;
   不让 body 自己滚 — 否则上层会因 pk 字段变多把映射表挤到底部 */
.wz-body {
  flex: 1; min-height: 0;
  overflow: hidden;
  padding: 16px;
  display: flex; flex-direction: column; gap: 14px;
}

.mode-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.mode-card { display: flex; gap: 12px; padding: 14px; border: 1px solid var(--bl-border);
  border-radius: 8px; cursor: pointer; position: relative; align-items: flex-start; transition: border-color .15s; }
.mode-card.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.mode-card .mode-ic { width: 36px; height: 36px; border-radius: 6px; display: inline-flex;
  align-items: center; justify-content: center; flex-shrink: 0; background: #165DFF; }
.mode-title { font-weight: 600; font-size: 13.5px; margin-bottom: 4px; }
.mode-check { position: absolute; top: 10px; right: 10px; width: 18px; height: 18px;
  background: var(--bl-primary); border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; }

/* 数据源配置区: 限高, 内部内容超出时各 panel 内部独立滚动 (不影响下方映射表空间) */
.ds-area {
  display: grid; grid-template-columns: 360px 1fr; gap: 12px;
  flex: 0 0 auto;            /* 不参与剩余空间分配 */
  max-height: 300px;         /* 配置区总高度上限, 主键字段加再多也不会撑高 */
  min-height: 200px;
}
.ds-panel {
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 6px;
  overflow: hidden;
  display: flex; flex-direction: column;     /* 让 body 区可 flex:1 接 panel 高度 */
}
.ds-panel-hd {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 12px; background: var(--bl-bg-2); border-bottom: 1px solid var(--bl-divider);
  flex-shrink: 0;
}
.ds-panel-title { font-size: 13px; font-weight: 600; }
.ds-panel-body {
  padding: 10px;
  flex: 1; min-height: 0; overflow-y: auto;   /* 内部独立滚动 */
}
.main-form { display: flex; flex-direction: column; gap: 4px; }

/* 主键字段: 多条下拉 + 添加 + 拖拽排序 */
.pk-list { display: flex; flex-direction: column; gap: 4px; flex: 1; min-width: 0; }
.pk-row {
  display: flex; align-items: center; gap: 6px; min-width: 0;
  padding: 2px 0;                                /* 留 hit-area 给 drag */
  border-radius: 4px;
  transition: background-color .15s, opacity .15s;
}
.pk-row.is-dragging { opacity: .45; background: var(--bl-primary-soft); }
.pk-seq {
  width: 20px; height: 20px; line-height: 20px;
  text-align: center; background: var(--bl-primary-soft); color: var(--bl-primary);
  font-size: 10px; font-weight: 600; border-radius: 50%; flex-shrink: 0;
  cursor: grab;                                  /* 序号圈作为拖拽手柄 */
  user-select: none;
}
.pk-seq:active { cursor: grabbing; }
.pk-select { flex: 1; min-width: 0; height: 28px; font-size: 12px; }
.pk-add { align-self: flex-start; padding: 2px 6px; font-size: 12px; }

.main-pick-btn { width: 100%; padding: 14px; border: 1px dashed var(--bl-border-strong);
  background: var(--bl-bg-1); color: var(--bl-primary); border-radius: 6px; cursor: pointer; font-size: 13px;
  display: inline-flex; align-items: center; justify-content: center; }
.main-pick-btn:hover { background: var(--bl-primary-soft); border-color: var(--bl-primary); }

.sub-table { width: 100%; font-size: 12px; }
.sub-table th { background: var(--bl-bg-2); padding: 6px 8px; }
.sub-table td { padding: 4px 6px; }
.sub-table .bl-input.bl-input-xs { height: 26px; padding: 0 6px; font-size: 12px; }

/* 映射区: flex:1 自动占据上方配置区之外的所有剩余高度 */
.map-area {
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 6px;
  overflow: hidden;
  flex: 1; min-height: 0;                /* 关键: 拿到剩余高度且允许内部 scroll */
  display: flex; flex-direction: column;
}
.map-hd {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 12px; background: var(--bl-bg-2); border-bottom: 1px solid var(--bl-divider);
  flex-shrink: 0;
}
.map-title { font-size: 13px; font-weight: 600; }
.map-search { position: relative; display: inline-flex; align-items: center; gap: 6px; }
.map-search .bl-input { width: 160px; height: 26px; padding: 0 6px; font-size: 12px; }
/* 表体撑满映射卡内剩余空间, 内部独立滚动 */
.map-table-wrap {
  flex: 1; min-height: 0;
  overflow-y: auto;
  overflow-x: auto;
  border-top: 1px solid var(--bl-divider);
}
.map-table { width: 100%; min-width: 1000px; font-size: 12px; border-collapse: separate; border-spacing: 0; }
/* 表头固定: 滚动时表头始终可见,行在其下方滚动 */
.map-table thead th {
  position: sticky;
  z-index: 2;
}
.map-table thead tr:first-child th { top: 0; }
.map-table thead tr:last-child th  { top: 30px; }   /* 第一行约 30px 高,第二行紧贴其下 */
.map-table th { padding: 6px 8px; }
.map-table td { padding: 4px 6px; }
/* —— 表头两层结构 (无 rowspan,所有 th 高度严格对齐,sticky 表现一致) —— */
/* 行1: 分组横幅 */
.map-table thead tr:first-child th {
  border-bottom: 1px solid var(--bl-divider);
}
.map-table thead tr:first-child th.th-corner-l { background: color-mix(in srgb, var(--bl-primary) 12%, var(--bl-bg-1)); }
.map-table thead tr:first-child th.th-corner-r { background: color-mix(in srgb, var(--bl-warning) 12%, var(--bl-bg-1)); }

/* 行2: 列名 + 统一颜色下划线 */
.map-table thead tr:last-child th {
  font-weight: 600;
  border-bottom: 2px solid var(--bl-divider);
}
.map-table thead tr:last-child th.bg-l {
  background: color-mix(in srgb, var(--bl-primary) 8%, var(--bl-bg-1));
  border-bottom-color: var(--bl-primary-border);
}
.map-table thead tr:last-child th.bg-c {
  background: color-mix(in srgb, var(--bl-warning) 8%, var(--bl-bg-1));
  border-bottom-color: color-mix(in srgb, var(--bl-warning) 32%, var(--bl-border));
}
/* 列名行: ☐ / 操作 两端的下划线分别匹配相邻分组色,使整行下划线连续 */
.map-table thead tr:last-child th.th-col-l {
  background: color-mix(in srgb, var(--bl-primary) 12%, var(--bl-bg-1));
  border-bottom-color: var(--bl-primary-border);
}
.map-table thead tr:last-child th.th-col-r {
  background: color-mix(in srgb, var(--bl-warning) 12%, var(--bl-bg-1));
  border-bottom-color: color-mix(in srgb, var(--bl-warning) 32%, var(--bl-border));
}
.th-group { font-weight: 700; text-align: center; font-size: 12.5px; letter-spacing: 0.5px; }
.bg-l { background: color-mix(in srgb, var(--bl-primary) 8%, var(--bl-bg-1)); }
.bg-c { background: color-mix(in srgb, var(--bl-warning) 8%, var(--bl-bg-1)); }
.map-table tbody tr { background: var(--bl-bg-1); }
.map-table tbody tr:nth-child(even) { background: var(--bl-bg-2); }
.map-table tbody tr:hover { background: var(--bl-bg-hover); }
.map-table tbody tr.is-dragging { opacity: .45; background: var(--bl-primary-soft); }
.map-table tbody td { border-bottom: 1px solid var(--bl-divider); }
.map-table .prop-grip { cursor: grab; user-select: none; padding: 4px; }
.map-table .prop-grip:active { cursor: grabbing; }
.map-table .bl-input.bl-input-xs { height: 26px; padding: 0 6px; font-size: 12px; }
.t-center { text-align: center; }

.empty-mode {
  flex: 1; min-height: 0;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 40px; gap: 8px; color: var(--bl-text-3);
}

.wz-ft { display: flex; justify-content: flex-end; gap: 8px; padding: 10px 16px;
  border-top: 1px solid var(--bl-divider); }

/* 物理表选择弹窗 */
.tbl-row { display: flex; align-items: center; gap: 8px; padding: 8px 12px;
  border: 1px solid var(--bl-border); border-radius: 6px; cursor: pointer; margin-bottom: 4px;
  position: relative; padding-left: 16px; }
.tbl-row:hover { border-color: var(--bl-primary); }
.tbl-row.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.tbl-row.is-disabled { opacity: .5; cursor: not-allowed; }
.tbl-side { position: absolute; left: 0; top: 0; bottom: 0; width: 4px; }

.inline-tbl-pick { display: flex; flex-direction: column; }
.inline-tbl-list { max-height: 320px; overflow: auto; }

.wz-fade-enter-active, .wz-fade-leave-active { transition: opacity .15s; }
.wz-fade-enter-from, .wz-fade-leave-to { opacity: 0; }
</style>
