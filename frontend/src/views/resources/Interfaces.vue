<template>
  <div class="page">
    <PageHeader title="接口" subtitle="纯抽象契约，定义共享属性，应用对象类型">
      <template #actions>
        <div class="seg">
          <button v-for="t in statusTabs" :key="t.k" :class="['seg-btn', statusFilter===t.k && 'is-on']" @click="statusFilter=t.k">
            {{ t.label }}<span class="seg-cnt">{{ statusCount(t.k) }}</span>
          </button>
        </div>
        <div class="if-view-toggle">
          <button :class="['vt-btn', !groupMode && 'is-on']" @click="groupMode=false" title="列表视图">
            <span v-html="BL.icon('list', 13)"></span><span>列表</span>
          </button>
          <button :class="['vt-btn', groupMode && 'is-on']" @click="groupMode=true" title="按行业分组">
            <span v-html="BL.icon('layers', 13)"></span><span>分组</span>
          </button>
        </div>
        <select class="bl-input hd-filter" v-model="industryFilter" title="所属行业">
          <option value="">全部行业</option>
          <option v-for="i in industryFilterOptions" :key="i" :value="i">{{ i }}</option>
        </select>
        <select class="bl-input" style="width:160px" v-model="domainFilter" title="业务领域">
          <option value="">全部业务领域</option>
          <option v-for="d in domainOpts" :key="d.code" :value="d.code">{{ d.name }}</option>
        </select>
        <div class="search-wrap">
          <span class="search-icon" v-html="BL.icon('search', 14)"></span>
          <input class="bl-input search-input" v-model="q" placeholder="搜索接口（名称/编码/类型）" />
        </div>
        <button class="bl-btn bl-btn-primary" @click="openCreate">
          <span v-html="BL.icon('plus', 12, '#fff')"></span>
          <span style="margin-left:4px">新建接口</span>
        </button>
      </template>
    </PageHeader>

    <div class="two-pane">
      <CategoryTreeFilter :rows="rows"
                          title="行业分类"
                          total-label="全部接口"
                          store-key="interfaces"
                          @change="onCategoryChange" />
      <!-- 列表 -->
      <div class="bl-card list-card">
        <div class="list-scroll">
        <table class="bl-table if-table">
          <thead>
            <tr>
              <th style="width:30px"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
              <th><span class="th-sort" @click="toggleSort('name')">接口名称<span class="th-arrow">{{ sortArrow('name') }}</span></span></th>
              <th><span class="th-sort" @click="toggleSort('industry')">所属行业<span class="th-arrow">{{ sortArrow('industry') }}</span></span></th>
              <th>描述</th>
              <th><span class="th-sort" @click="toggleSort('prop')">属性数<span class="th-arrow">{{ sortArrow('prop') }}</span></span></th>
              <th><span class="th-sort" @click="toggleSort('impl')">实现数<span class="th-arrow">{{ sortArrow('impl') }}</span></span></th>
              <th><span class="th-sort" @click="toggleSort('domain')">所在领域<span class="th-arrow">{{ sortArrow('domain') }}</span></span></th>
              <th><span class="th-sort" @click="toggleSort('status')">状态<span class="th-arrow">{{ sortArrow('status') }}</span></span></th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <template v-for="row in displayRows" :key="row.key">
              <!-- 分组标题行 -->
              <tr v-if="row.type==='group'" class="if-group-row" @click="toggleGroup(row.key)">
                <td :colspan="9">
                  <span class="if-group-chev" v-html="BL.icon(row.collapsed ? 'chevronRight' : 'chevronDown', 12)"></span>
                  <span class="if-group-label">{{ row.label }}</span>
                  <span class="if-group-count">{{ row.count }}</span>
                </td>
              </tr>
              <!-- 接口行 -->
              <tr v-else :class="['if-row', groupMode && 'is-grouped', selected?.id===row.data.id && 'is-active']" @click="openDetail(row.data, 'overview')">
                <td @click.stop><input type="checkbox" :checked="checked.has(row.data.id)" @change="toggleCheck(row.data.id)" /></td>
                <td>
                  <div class="if-name-cell">
                    <span class="if-ic" :style="{ background: row.data.color || '#13C2C2' }" v-html="BL.icon(row.data.icon || 'station', 12, '#fff')"></span>
                    <div class="if-name-text">
                      <div class="if-name bl-truncate" :title="row.data.display_name || row.data.rdfs_label">{{ row.data.display_name || row.data.rdfs_label }}</div>
                      <div class="if-api bl-mono bl-muted">{{ row.data.api_name }}</div>
                    </div>
                  </div>
                </td>
                <td><span class="bl-truncate" :title="ifDomainPath(row.data)">{{ ifIndustry(row.data) }}</span></td>
                <td class="bl-truncate" style="max-width:260px" :title="row.data.rdfs_comment">{{ row.data.rdfs_comment || '—' }}</td>
                <td @click.stop="openDetail(row.data, 'props')"><a class="link">{{ row.data.prop_count || 0 }}</a></td>
                <td @click.stop="openDetail(row.data, 'impl')"><a class="link">{{ row.data.impl_count || 0 }}</a></td>
                <td><span class="bl-tag">{{ row.data.ns_code || '—' }}</span></td>
                <td>
                  <span :class="['bl-tag', row.data.status===1 ? 'bl-tag-success' : 'bl-tag-warning']">{{ row.data.status===1 ? '启用' : '实验' }}</span>
                </td>
                <td @click.stop>
                  <div class="bl-row" style="gap:0">
                    <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="编辑" @click="openDetail(row.data, 'overview')" v-html="BL.icon('edit', 12)"></button>
                    <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" :title="row.data.status===1 ? '禁用' : '启用'" @click="toggleStatus(row.data)" v-html="BL.icon('zap', 12)"></button>
                    <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click="removeOne(row.data)" v-html="BL.icon('trash', 12)"></button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
        <div v-if="!filtered.length" class="bl-empty" style="padding:60px">
          <div class="bl-empty-icon" v-html="BL.icon('station', 36)"></div>
          <div>未匹配到接口数据</div>
          <div style="margin-top:12px">
            <button class="bl-btn" @click="clearFilters">清除筛选</button>
          </div>
        </div>
        </div>
        <!-- 分页 + 批量条 -->
        <div class="list-ft">
          <div>
            <template v-if="checked.size">
              已选 <b style="color:var(--bl-primary)">{{ checked.size }}</b> 项
              <button class="bl-btn bl-btn-sm if-del-btn" style="margin-left:8px" @click="removeBatch">
                <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">批量删除</span>
              </button>
              <button class="bl-btn bl-btn-sm if-ena-btn" style="margin-left:6px" @click="batchSetStatus(1)">
                <span v-html="BL.icon('check', 12)"></span><span style="margin-left:4px">启用</span>
              </button>
              <button class="bl-btn bl-btn-sm if-dis-btn" style="margin-left:6px" @click="batchSetStatus(0)">
                <span v-html="BL.icon('power', 12)"></span><span style="margin-left:4px">禁用</span>
              </button>
              <button class="bl-btn bl-btn-sm bl-btn-text if-clear-btn" style="margin-left:6px" @click="checked = new Set()">取消选择</button>
            </template>
            <template v-else>
              共 {{ filtered.length }} 项
              <span v-if="groupMode" class="bl-muted" style="margin-left:8px">· {{ groupCount }} 个行业</span>
            </template>
          </div>
          <div class="bl-row" style="gap:4px" v-if="!groupMode">
            <span class="bl-muted" style="font-size:12px;margin-right:6px">每页</span>
            <select class="bl-input if-page-size" v-model.number="pageSize">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
            <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page<=1" @click="page--">‹</button>
            <span class="bl-muted" style="font-size:12px">{{ page }} / {{ totalPages }}</span>
            <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page>=totalPages" @click="page++">›</button>
          </div>
          <div class="bl-row" style="gap:4px" v-else>
            <button class="bl-btn bl-btn-sm bl-btn-text" @click="expandAllGroups">展开全部</button>
            <button class="bl-btn bl-btn-sm bl-btn-text" @click="collapseAllGroups">折叠全部</button>
          </div>
        </div>
      </div>

      <!-- 右侧详情抽屉（浮层，可拖拽/最大/最小） -->
      <transition name="if-drawer">
      <aside v-if="drawerOpen" class="if-detail" :style="{ width: drawerWidth + 'px' }">
        <div class="if-drag-handle" @mousedown="onDragStart" :class="resizing && 'is-resizing'"></div>
        <div class="detail-hd">
          <div class="bl-row" style="gap:10px;flex:1;min-width:0">
            <span class="if-ic if-ic-lg" :style="{ background: form.color || '#13C2C2' }" v-html="BL.icon(form.icon || 'station', 18, '#fff')"></span>
            <div class="bl-grow" style="min-width:0">
              <div class="dh-title bl-truncate">{{ form.display_name || form.rdfs_label || form.api_name || '新建接口' }}</div>
              <div class="bl-mono bl-muted" style="font-size:11px">{{ form.api_name || '—' }}</div>
            </div>
          </div>
          <div class="bl-row" style="gap:4px;flex-shrink:0">
            <button class="bl-btn bl-btn-text bl-btn-icon" :title="drawerMaxed ? '恢复' : '最大'" @click="toggleMax" v-html="BL.icon(drawerMaxed ? 'minimize' : 'maximize', 14)"></button>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="drawerOpen=false" v-html="BL.icon('x', 14)"></button>
          </div>
        </div>

        <div class="tabs">
          <div v-for="t in tabs" :key="t.k" :class="['tab', drawerTab===t.k && 'is-active']" @click="drawerTab=t.k">{{ t.label }}</div>
        </div>

        <div class="detail-body">
          <!-- 概览 -->
          <div v-if="drawerTab==='overview'">
            <div class="sec">基础信息</div>
            <FieldRow label="接口编码" inline><input class="bl-input bl-mono" v-model="form.interface_code" /></FieldRow>
            <FieldRow label="API 名称" inline hint="snake_case · 全局唯一 · 创建后不可修改">
              <input class="bl-input bl-mono" :value="form.api_name" :readonly="!!form.id" :disabled="!!form.id" />
            </FieldRow>
            <FieldRow label="标准对外名称" inline hint="rdfs:label"><input class="bl-input" v-model="form.rdfs_label" /></FieldRow>
            <FieldRow label="显示名称" inline><input class="bl-input" v-model="form.display_name" /></FieldRow>
            <FieldRow label="状态" inline>
              <div class="radio-group">
                <label class="radio-item"><input type="radio" :value="1" v-model.number="form.status" /> 启用</label>
                <label class="radio-item"><input type="radio" :value="0" v-model.number="form.status" /> 实验</label>
              </div>
            </FieldRow>

            <div class="sec">分类归属</div>
            <FieldRow label="业务领域" inline>
              <select class="bl-input" v-model="form.category_code">
                <option value="">— 无 —</option>
                <option v-for="d in domainOpts" :key="d.code" :value="d.code">{{ d.name }}</option>
              </select>
            </FieldRow>
            <FieldRow label="命名空间" inline><input class="bl-input bl-mono" v-model="form.ns_code" /></FieldRow>

            <div class="sec">语义定义</div>
            <FieldRow label="注释" hint="rdfs:comment · 给大模型 / 推理引擎 / 开发者看的正式语义定义">
              <textarea class="bl-textarea" rows="3" v-model="form.rdfs_comment"></textarea>
            </FieldRow>
            <FieldRow label="参考资料" hint="rdfs:seeAlso · 关联文档 / 链接 / 参考出处"><textarea class="bl-textarea" rows="2" v-model="form.rdfs_see_also"></textarea></FieldRow>
            <FieldRow label="定义来源" hint="rdfs:isDefinedBy · 权威定义出处"><textarea class="bl-textarea" rows="2" v-model="form.rdfs_defined_by"></textarea></FieldRow>
            <FieldRow label="说明"><textarea class="bl-textarea" rows="2" v-model="form.description"></textarea></FieldRow>

            <div class="sec">规则配置</div>
            <FieldRow label="元数据 (JSON)" hint="机器可读：接口规则、查询约束、语义配置">
              <textarea class="bl-textarea bl-mono" rows="5" v-model="form.metadata" placeholder='{ "query": "...", "unit_normalization": true }'></textarea>
            </FieldRow>
          </div>

          <!-- 显示 -->
          <div v-if="drawerTab==='style'">
            <div class="sec">分组颜色</div>
            <ColorPickerField v-model="form.color" />

            <div class="sec">图标</div>
            <IconPickerField v-model="form.icon" label="" :preset-count="32" :suggest-name="form.rdfs_label || form.display_name || form.api_name" />

            <div class="sec">RID</div>
            <div class="rid-row">
              <span class="bl-mono">{{ form.rid }}</span>
              <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" @click="copyText(form.rid)" title="复制 RID" v-html="BL.icon('copy', 12)"></button>
            </div>
          </div>

          <!-- 属性 -->
          <div v-if="drawerTab==='props'">
            <div class="row-between">
              <span class="bl-muted" style="font-size:12px">共 {{ realPropCount }} 个属性</span>
              <div class="bl-row" style="gap:8px">
                <button v-if="propChecked.size" class="bl-btn bl-btn-sm" @click="openBatchFormat">
                  <span v-html="BL.icon('sliders', 12)"></span>
                  <span style="margin-left:4px">批量格式化 ({{ propChecked.size }})</span>
                </button>
                <button v-if="propChecked.size" class="bl-btn bl-btn-sm bl-btn-danger" @click="removePropsBatch">
                  <span v-html="BL.icon('trash', 12)"></span>
                  <span style="margin-left:4px">删除 ({{ propChecked.size }})</span>
                </button>
                <button class="bl-btn bl-btn-primary bl-btn-sm" @click="addPropRow">
                  <span v-html="BL.icon('plus', 12, '#fff')"></span>
                  <span style="margin-left:4px">新增属性</span>
                </button>
              </div>
            </div>
            <table class="bl-table prop-table" style="margin-top:8px">
              <thead>
                <tr>
                  <th style="width:34px"><input type="checkbox" :checked="allPropsChecked" @change="togglePropAll" /></th>
                  <th>编码</th><th>名称</th><th>类型</th><th>值类型</th><th>配置状态</th><th>格式化</th><th>注释</th><th style="width:64px"></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in properties" :key="p.id"
                    :class="['prop-row', p._editing && 'is-editing']"
                    @dblclick="editPropRow(p)">
                  <td @click.stop><input type="checkbox" :checked="propChecked.has(p.id)" :disabled="p._isNew" @change="togglePropCheck(p.id)" /></td>
                  <!-- 编码 -->
                  <td>
                    <input v-if="p._editing && p._isNew" class="bl-input bl-mono" v-model="p.api_name" placeholder="snake_case" />
                    <span v-else class="bl-mono">{{ p.api_name }}</span>
                  </td>
                  <!-- 名称 -->
                  <td>
                    <input v-if="p._editing" class="bl-input" v-model="p.display_name" placeholder="显示名" />
                    <span v-else>{{ p.display_name || p.rdfs_label || '—' }}</span>
                  </td>
                  <!-- 类型 -->
                  <td>
                    <select v-if="p._editing" class="bl-input" v-model="p.data_type">
                      <option v-for="t in xsdTypes" :key="t" :value="t">{{ t }}</option>
                    </select>
                    <span v-else class="bl-tag">{{ p.data_type }}</span>
                  </td>
                  <!-- 值类型 (引用 ont_value_types) -->
                  <td>
                    <select v-if="p._editing" class="bl-input" v-model="p.value_type">
                      <option value="">— 无 —</option>
                      <option v-for="vt in valueTypeOptions" :key="vt.id" :value="vt.id">{{ vt.rdfs_label || vt.api_name }} ({{ vt.base_type }})</option>
                    </select>
                    <span v-else>{{ valueTypeLabel(p.value_type) }}</span>
                  </td>
                  <!-- 配置状态: 0=可选 / 1=必填 / 2=多值 -->
                  <td>
                    <select v-if="p._editing" class="bl-input" v-model.number="p.is_required">
                      <option :value="0">可选</option>
                      <option :value="1">必填</option>
                      <option :value="2">多值</option>
                    </select>
                    <span v-else :class="['bl-tag', requiredTagCls(p.is_required)]">{{ requiredLabel(p.is_required) }}</span>
                  </td>
                  <!-- 格式化 -->
                  <td @click.stop="openFormat(p)">
                    <span :class="['fmt-pill', propFormatMap[p.id]?.format_enabled && 'is-on']" :title="propFormatMap[p.id]?.format_enabled ? '已配置 - 点击编辑' : '未配置 - 点击设置'">
                      <span v-html="BL.icon('sliders', 11)"></span>
                      <span style="margin-left:4px">{{ formatPreview(p) }}</span>
                    </span>
                  </td>
                  <!-- 注释 -->
                  <td>
                    <input v-if="p._editing" class="bl-input" v-model="p.rdfs_comment" placeholder="注释" />
                    <span v-else class="bl-truncate prop-comment" :title="p.rdfs_comment">{{ p.rdfs_comment || '—' }}</span>
                  </td>
                  <!-- 操作（仅编辑态显示 保存/取消） -->
                  <td @click.stop>
                    <div v-if="p._editing" class="bl-row" style="gap:0">
                      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="保存" @click="savePropRow(p)" v-html="BL.icon('check', 13)"></button>
                      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="取消" @click="cancelPropRow(p)" v-html="BL.icon('x', 13)"></button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="!properties.length" class="bl-empty" style="padding:32px">尚未定义接口属性，点击「新增属性」添加</div>
          </div>

          <!-- 实现 -->
          <div v-if="drawerTab==='impl'">
            <div class="row-between">
              <span class="bl-muted" style="font-size:12px">共 {{ implementers.length }} 个实现类</span>
              <button class="bl-btn bl-btn-primary bl-btn-sm" @click="openAddImpl">
                <span v-html="BL.icon('plus', 12, '#fff')"></span>
                <span style="margin-left:4px">绑定实现类</span>
              </button>
            </div>
            <div class="impl-list">
              <div v-for="c in implementers" :key="c.id" class="impl-row">
                <span class="if-ic" :style="{ background: c.color || '#165DFF' }" v-html="BL.icon(c.icon || 'cube', 12, '#fff')"></span>
                <div class="bl-grow">
                  <div>{{ c.display_name || c.rdfs_label || c.api_name }}</div>
                  <div class="bl-mono bl-muted" style="font-size:11px">{{ c.api_name }} · {{ c.ns_code }}</div>
                </div>
                <button class="bl-btn bl-btn-text bl-btn-sm" @click="goObject(c)">查看对象</button>
                <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="解绑" @click="removeImpl(c)" v-html="BL.icon('x', 12)"></button>
              </div>
              <div v-if="!implementers.length" class="bl-empty" style="padding:32px">尚未有对象实现该接口</div>
            </div>
          </div>
        </div>

        <div class="detail-ft">
          <button class="bl-btn" @click="drawerOpen=false">取消</button>
          <button class="bl-btn bl-btn-primary" @click="saveInterface">保存</button>
        </div>
      </aside>
      </transition>
    </div>

    <!-- 属性格式化弹窗 -->
    <PropertyFormatModal v-model:open="fmtOpen"
                         :property-id="fmtPropertyId"
                         :property-ids="fmtPropertyIds"
                         property-scope="interface"
                         :data-type="fmtDataType"
                         @saved="onFormatSaved" />

    <!-- 绑定实现类弹窗 -->
    <div v-if="implPickerOpen" class="bl-modal-mask" @click.self="implPickerOpen=false">
      <div class="bl-modal" style="width:480px;max-height:80vh;display:flex;flex-direction:column">
        <div class="bl-modal-hd">绑定实现该接口的对象类型</div>
        <div class="bl-modal-body bl-col" style="gap:8px;overflow:hidden">
          <input class="bl-input" placeholder="搜索对象类型" v-model="implPickerQ" />
          <div style="flex:1; overflow:auto; display:flex; flex-direction:column; gap:4px">
            <label v-for="c in implCandidates" :key="c.id"
                   :class="['mp-row', implDraft.has(c.id) && 'is-on']">
              <input type="checkbox" :checked="implDraft.has(c.id)" @change="toggleImplDraft(c.id)" />
              <span class="if-ic" :style="{ background: c.color || '#165DFF' }" v-html="BL.icon(c.icon || 'cube', 12, '#fff')"></span>
              <div class="bl-grow"><div>{{ c.display_name || c.api_name }}</div>
                <div class="bl-mono bl-muted" style="font-size:11px">{{ c.api_name }} · {{ c.ns_code }}</div>
              </div>
            </label>
            <div v-if="!implCandidates.length" class="bl-empty" style="padding:24px">暂无可绑定的对象类型</div>
          </div>
        </div>
        <div class="bl-modal-ft">
          <button class="bl-btn" @click="implPickerOpen=false">取消</button>
          <button class="bl-btn bl-btn-primary" :disabled="!implDraft.size" @click="submitImpls">绑定 ({{ implDraft.size }})</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, reactive } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FieldRow from '@/views/config/category/FieldRow.vue'
import IconPickerField from '@/components/IconPickerField.vue'
import ColorPickerField from '@/components/ColorPickerField.vue'
import { BL } from '@/lib/bl.js'
import { interfaceApi, resourceApi, categoryApi, valueTypeApi, propertyFormatApi } from '@/api'
import PropertyFormatModal from '@/components/PropertyFormatModal.vue'
import CategoryTreeFilter from '@/components/CategoryTreeFilter.vue'

const rows = ref([])
const statusFilter = ref('all')
const domainFilter = ref('')
const industryFilter = ref('')
const q = ref('')
const checked = ref(new Set())
const page = ref(1)
const pageSize = ref(10)

const drawerOpen = ref(false)
const drawerTab = ref('overview')
const selected = ref(null)
const form = reactive({})

/* —— 抽屉尺寸 / 拖拽 —— */
const drawerWidth = ref(0)
const drawerMaxed = ref(false)
const drawerMined = ref(false)
const resizing = ref(false)
const DRAWER_MIN = 420
function drawerMaxPx() { return Math.floor(window.innerWidth * 0.85) }
function defaultDrawerWidth() { return Math.max(DRAWER_MIN, Math.min(560, Math.floor(window.innerWidth * 0.45))) }
function ensureDrawerSize() {
  if (!drawerWidth.value) drawerWidth.value = defaultDrawerWidth()
  drawerMaxed.value = false; drawerMined.value = false
}
function toggleMax() {
  if (drawerMaxed.value) { drawerWidth.value = defaultDrawerWidth(); drawerMaxed.value = false }
  else { drawerWidth.value = drawerMaxPx(); drawerMaxed.value = true; drawerMined.value = false }
}
function toggleMin() {
  if (drawerMined.value) { drawerWidth.value = defaultDrawerWidth(); drawerMined.value = false }
  else { drawerWidth.value = DRAWER_MIN; drawerMined.value = true; drawerMaxed.value = false }
}
let dragStartX = 0, dragStartW = 0
function onDragStart(e) {
  resizing.value = true
  dragStartX = e.clientX; dragStartW = drawerWidth.value
  document.body.style.cursor = 'col-resize'; document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragEnd)
}
function onDragMove(e) {
  const dx = dragStartX - e.clientX
  const next = Math.min(drawerMaxPx(), Math.max(DRAWER_MIN, dragStartW + dx))
  drawerWidth.value = next
  drawerMaxed.value = next === drawerMaxPx()
  drawerMined.value = next === DRAWER_MIN
}
function onDragEnd() {
  resizing.value = false
  document.body.style.cursor = ''; document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
}
onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
})
const properties = ref([])
const implementers = ref([])
const allClasses = ref([])
const domainOpts = ref([])

const statusTabs = [{ k:'all', label:'全部' }, { k:'1', label:'启用' }, { k:'0', label:'实验' }]
const tabs = [
  { k:'overview', label:'概览' },
  { k:'style',    label:'显示' },
  { k:'props',    label:'属性' },
  { k:'impl',     label:'实现' }
]
const colorPalette = ['#165DFF','#00B42A','#722ED1','#FF7D00','#EB2F96','#13C2C2','#FADB14','#F53F3F','#86909C']
const iconPalette  = ['grid','cube','folder','share','network','station','user','sliders']
const xsdTypes = ['xsd:string','xsd:decimal','xsd:integer','xsd:boolean','xsd:dateTime','xsd:date','xsd:anyURI']

function statusCount(k) {
  if (k === 'all') return rows.value.length
  return rows.value.filter(r => String(r.status) === k).length
}

/* —— 所属行业(由 category_code 解析) —— */
const catInfoByCode = ref({})   // code -> { label, industryLabel, path }
function ifIndustry(r) {
  const info = catInfoByCode.value[r.category_code]
  return info ? (info.industryLabel || info.label) : '—'
}
function ifDomainPath(r) {
  const info = catInfoByCode.value[r.category_code]
  return info ? info.path : (r.category_code || '')
}

/* —— 排序 —— */
const sortKey = ref('')
const sortDir = ref('')   // 'asc' | 'desc' | ''
function toggleSort(key) {
  if (sortKey.value !== key) { sortKey.value = key; sortDir.value = 'asc' }
  else if (sortDir.value === 'asc') sortDir.value = 'desc'
  else { sortKey.value = ''; sortDir.value = '' }
}
function sortArrow(key) {
  if (sortKey.value !== key) return ' ⇅'
  return sortDir.value === 'asc' ? ' ↑' : ' ↓'
}

const industryFilterOptions = computed(() => {
  const set = new Set()
  rows.value.forEach(r => { const i = ifIndustry(r); if (i && i !== '—') set.add(i) })
  return [...set].sort((a, b) => a.localeCompare(b))
})

/* —— 左侧行业分类树 —— */
const selectedCategoryCodes = ref(null)
function onCategoryChange({ codes }) { selectedCategoryCodes.value = codes || null }

const filtered = computed(() => {
  let list = rows.value
  if (selectedCategoryCodes.value) list = list.filter(r => selectedCategoryCodes.value.has(r.category_code))
  if (statusFilter.value !== 'all') list = list.filter(r => String(r.status) === statusFilter.value)
  if (industryFilter.value)        list = list.filter(r => ifIndustry(r) === industryFilter.value)
  if (domainFilter.value)          list = list.filter(r => r.category_code === domainFilter.value)
  const k = q.value.trim().toLowerCase()
  if (k) list = list.filter(r =>
    [r.api_name, r.interface_code, r.display_name, r.rdfs_label, r.rdfs_comment, r.ns_code]
      .filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
  if (sortKey.value && sortDir.value) {
    const dir = sortDir.value === 'asc' ? 1 : -1
    list = [...list].sort((a, b) => {
      let va, vb
      if (sortKey.value === 'name')        { va = a.display_name || a.rdfs_label || a.api_name || ''; vb = b.display_name || b.rdfs_label || b.api_name || '' }
      else if (sortKey.value === 'industry'){ va = ifIndustry(a); vb = ifIndustry(b) }
      else if (sortKey.value === 'prop')    { va = a.prop_count || 0; vb = b.prop_count || 0 }
      else if (sortKey.value === 'impl')    { va = a.impl_count || 0; vb = b.impl_count || 0 }
      else if (sortKey.value === 'domain')  { va = a.ns_code || ''; vb = b.ns_code || '' }
      else if (sortKey.value === 'status')  { va = a.status ?? 9; vb = b.status ?? 9 }
      if (typeof va === 'number') return (va - vb) * dir
      return String(va).localeCompare(String(vb)) * dir
    })
  }
  return list
})
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize.value)))
const pageRows   = computed(() => filtered.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))

/* —— 按所属行业分组显示 —— */
const groupMode = ref(false)
const collapsedGroups = ref(new Set())
function toggleGroup(key) {
  const s = new Set(collapsedGroups.value)
  if (s.has(key)) s.delete(key); else s.add(key)
  collapsedGroups.value = s
}
const groupedRows = computed(() => {
  const groups = new Map()
  filtered.value.forEach(r => {
    const key = ifIndustry(r) || '—'
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key).push(r)
  })
  const out = []
  for (const [key, items] of groups) {
    const collapsed = collapsedGroups.value.has(key)
    out.push({ type: 'group', key, label: key, count: items.length, collapsed })
    if (!collapsed) items.forEach(r => out.push({ type: 'item', key: 'i' + r.id, data: r }))
  }
  return out
})
const groupCount = computed(() => new Set(filtered.value.map(r => ifIndustry(r) || '—')).size)
const displayRows = computed(() =>
  groupMode.value
    ? groupedRows.value
    : pageRows.value.map(r => ({ type: 'item', key: 'i' + r.id, data: r }))
)
function expandAllGroups() { collapsedGroups.value = new Set() }
function collapseAllGroups() { collapsedGroups.value = new Set(filtered.value.map(r => ifIndustry(r) || '—')) }

const visibleItems = computed(() => groupMode.value ? filtered.value : pageRows.value)
const allChecked = computed(() => visibleItems.value.length && visibleItems.value.every(r => checked.value.has(r.id)))

// 重置页码当筛选变化
import { watch } from 'vue'
watch([statusFilter, domainFilter, industryFilter, q, pageSize], () => { page.value = 1 })

function toggleAll() {
  const s = new Set(checked.value)
  if (allChecked.value) visibleItems.value.forEach(r => s.delete(r.id))
  else visibleItems.value.forEach(r => s.add(r.id))
  checked.value = s
}
function toggleCheck(id) {
  const s = new Set(checked.value)
  s.has(id) ? s.delete(id) : s.add(id)
  checked.value = s
}
function clearFilters() { statusFilter.value='all'; domainFilter.value=''; q.value='' }

async function load() {
  rows.value = await interfaceApi.list().catch(() => [])
}

function openCreate() {
  selected.value = null
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, { status: 1, color: '#165DFF', icon: 'station' })
  drawerTab.value = 'overview'
  ensureDrawerSize()
  drawerOpen.value = true
}

async function openDetail(r, tab) {
  selected.value = r
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, r)
  drawerTab.value = tab || 'overview'
  ensureDrawerSize()
  drawerOpen.value = true
  await loadProps()
  await loadImpls()
}

async function loadProps() {
  if (!selected.value) { properties.value = []; propChecked.value = new Set(); return }
  const list = await interfaceApi.properties(selected.value.id).catch(() => [])
  properties.value = list.map(p => ({ ...p, _editing: false, _isNew: false }))
  propChecked.value = new Set()
  await loadValueTypeOptions()
  await loadPropFormats()
}

/* —— 值类型下拉选项 —— */
const valueTypeOptions = ref([])
async function loadValueTypeOptions() {
  if (valueTypeOptions.value.length) return
  valueTypeOptions.value = (await valueTypeApi.list().catch(() => [])).filter(v => v.status === 1)
}
function valueTypeLabel(id) {
  if (!id) return '—'
  const v = valueTypeOptions.value.find(x => x.id === id)
  return v ? (v.rdfs_label || v.api_name) : id
}

/* —— 格式化批量加载 + 编辑 —— */
const propFormatMap = ref({})
async function loadPropFormats() {
  const ids = properties.value.filter(p => !p._isNew).map(p => p.id)
  if (!ids.length) { propFormatMap.value = {}; return }
  try {
    const list = await propertyFormatApi.byProperties(ids)
    const m = {}
    ;(list || []).forEach(f => { m[f.property_id] = f })
    propFormatMap.value = m
  } catch { propFormatMap.value = {} }
}
function formatPreview(p) {
  const f = propFormatMap.value[p.id]
  if (!f || !f.format_enabled) return '未设置'
  const types = { general:'常规', number:'数值', currency:'货币', accounting:'会计', date:'日期', time:'时间', percent:'百分比', fraction:'分数', scientific:'科学记数', text:'文本', special:'特殊', custom:'自定义' }
  return types[f.format_type] || f.format_type
}

/* —— 属性格式化弹窗 (单条 / 批量共用) —— */
const fmtOpen = ref(false)
const fmtPropertyId = ref('')
const fmtPropertyIds = ref([])
const fmtDataType = ref('')
function openFormat(p) {
  if (p._isNew) { BL.warning('请先保存属性后再设置格式化'); return }
  fmtPropertyId.value = p.id
  fmtPropertyIds.value = []
  fmtDataType.value = p.data_type || ''
  fmtOpen.value = true
}
function openBatchFormat() {
  const ids = properties.value.filter(p => propChecked.value.has(p.id) && !p._isNew).map(p => p.id)
  if (!ids.length) { BL.warning('选中的属性均为未保存的新增项,无法批量格式化'); return }
  const first = properties.value.find(p => p.id === ids[0])
  fmtPropertyId.value = ''
  fmtPropertyIds.value = ids
  fmtDataType.value = first?.data_type || ''
  fmtOpen.value = true
}
async function onFormatSaved() {
  await loadPropFormats()
}
async function loadImpls() {
  if (!selected.value) { implementers.value = []; return }
  implementers.value = await interfaceApi.implementers(selected.value.id).catch(() => [])
}

async function saveInterface() {
  if (!form.api_name) { BL.warning('API 名称为必填'); return }
  if (!/^[a-z][a-z0-9_]*$/.test(form.api_name)) { BL.warning('API 名称需 snake_case'); return }
  if (form.id) await interfaceApi.update(form.id, form)
  else         await interfaceApi.create(form)
  BL.success('已保存')
  drawerOpen.value = false
  await load()
}

async function toggleStatus(r) {
  await interfaceApi.toggle(r.id)
  await load()
}

async function removeOne(r) {
  const ok = await BL.confirm({ title:'删除接口', content:`确定删除「${r.display_name || r.api_name}」？关联属性与实现关系会一并删除。`, danger:true, okText:'删除' })
  if (!ok) return
  await interfaceApi.remove(r.id)
  BL.success('已删除')
  if (selected.value?.id === r.id) drawerOpen.value = false
  await load()
}

async function removeBatch() {
  const ok = await BL.confirm({ title:'批量删除', content:`确定删除已选 ${checked.value.size} 个接口？`, danger:true, okText:'删除' })
  if (!ok) return
  for (const id of checked.value) await interfaceApi.remove(id)
  BL.success('已删除')
  checked.value = new Set()
  await load()
}

/* —— 批量启用 / 禁用 (status: 1=启用 / 0=实验) —— */
async function batchSetStatus(targetStatus) {
  const ids = [...checked.value]
  if (!ids.length) return
  const targets = rows.value.filter(r => ids.includes(r.id))
  const toChange = targets.filter(r => r.status !== targetStatus)
  if (!toChange.length) {
    BL.warning(`所选 ${ids.length} 项已经全部是「${targetStatus === 1 ? '启用' : '实验'}」状态`)
    return
  }
  const label = targetStatus === 1 ? '启用' : '禁用'
  const ok = await BL.confirm({
    title: `批量${label}`,
    content: `确定将选中的 ${toChange.length} 个接口${label}?`,
    okText: label
  })
  if (!ok) return
  let okCount = 0, failCount = 0
  for (const r of toChange) {
    try {
      await interfaceApi.update(r.id, { ...r, status: targetStatus })
      okCount++
    } catch { failCount++ }
  }
  if (failCount) BL.warning(`成功 ${okCount} 个,失败 ${failCount} 个`)
  else BL.success(`已${label} ${okCount} 个`)
  await load()
}

/* ---- 属性（行内编辑） ---- */
const propChecked = ref(new Set())
const selectableProps = computed(() => properties.value.filter(p => !p._isNew))
const realPropCount = computed(() => selectableProps.value.length)
const allPropsChecked = computed(() =>
  selectableProps.value.length > 0 && selectableProps.value.every(p => propChecked.value.has(p.id)))

function togglePropCheck(id) {
  const s = new Set(propChecked.value)
  s.has(id) ? s.delete(id) : s.add(id)
  propChecked.value = s
}
function togglePropAll() {
  const s = new Set(propChecked.value)
  if (allPropsChecked.value) selectableProps.value.forEach(p => s.delete(p.id))
  else selectableProps.value.forEach(p => s.add(p.id))
  propChecked.value = s
}

/* 配置状态(is_required) 三态展示: 0=可选, 1=必填, 2=多值 */
function requiredLabel(v) {
  const n = Number(v)
  if (n === 2) return '多值'
  if (n === 1) return '必填'
  return '可选'
}
function requiredTagCls(v) {
  const n = Number(v)
  if (n === 2) return 'bl-tag-warning'
  if (n === 1) return 'bl-tag-danger'
  return ''
}

function addPropRow() {
  // 在最上面插入一行草稿，进入编辑态
  properties.value.unshift({
    id: 'new_' + Date.now(),
    api_name: '', prop_code: '', display_name: '',
    data_type: 'xsd:string', value_type: '',
    is_required: 0, rdfs_comment: '',
    _editing: true, _isNew: true
  })
}
function editPropRow(p) {
  if (p._editing) return
  p._backup = {
    prop_code: p.prop_code, data_type: p.data_type, display_name: p.display_name,
    rdfs_label: p.rdfs_label, rdfs_comment: p.rdfs_comment, is_required: p.is_required
  }
  p._editing = true
}
function cancelPropRow(p) {
  if (p._isNew) {
    properties.value = properties.value.filter(x => x !== p)
  } else {
    if (p._backup) Object.assign(p, p._backup)
    p._editing = false
    delete p._backup
  }
}
async function savePropRow(p) {
  if (!p.api_name || !p.data_type) { BL.warning('编码、类型为必填'); return }
  if (p._isNew && !/^[a-z][a-z0-9_]*$/.test(p.api_name)) { BL.warning('编码需 snake_case'); return }
  const { _editing, _isNew, _backup, ...payload } = p
  if (p._isNew) {
    delete payload.id
    payload.prop_code = payload.prop_code || payload.api_name
    await interfaceApi.addProperty(selected.value.id, payload)
  } else {
    await interfaceApi.updateProperty(p.id, payload)
  }
  BL.success('已保存')
  await loadProps()
  await load()
}
async function removePropsBatch() {
  const ids = [...propChecked.value]
  if (!ids.length) return
  const ok = await BL.confirm({ title:'删除属性', content:`确定删除选中的 ${ids.length} 个属性？`, danger:true, okText:'删除' })
  if (!ok) return
  for (const id of ids) await interfaceApi.removeProperty(id)
  BL.success('已删除')
  await loadProps()
  await load()
}

/* ---- 实现 ---- */
const implPickerOpen = ref(false)
const implPickerQ = ref('')
const implDraft = ref(new Set())

async function openAddImpl() {
  if (!allClasses.value.length) allClasses.value = await resourceApi.classes().catch(() => [])
  implDraft.value = new Set()
  implPickerQ.value = ''
  implPickerOpen.value = true
}
const implCandidates = computed(() => {
  const bound = new Set(implementers.value.map(c => c.id))
  const k = implPickerQ.value.trim().toLowerCase()
  return allClasses.value
    .filter(c => !bound.has(c.id))
    .filter(c => !k || [c.display_name, c.api_name, c.ns_code].filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
})
function toggleImplDraft(id) {
  const s = new Set(implDraft.value)
  s.has(id) ? s.delete(id) : s.add(id)
  implDraft.value = s
}
async function submitImpls() {
  for (const cid of implDraft.value) {
    const c = allClasses.value.find(x => x.id === cid)
    await interfaceApi.addImpl(selected.value.id, { classId: cid, categoryCode: c?.category_code })
  }
  BL.success(`已绑定 ${implDraft.value.size} 个对象类型`)
  implPickerOpen.value = false
  await loadImpls()
  await load()
}
async function removeImpl(c) {
  const ok = await BL.confirm({ title:'解绑实现关系', content:`从该接口中解除「${c.display_name || c.api_name}」？`, danger:true })
  if (!ok) return
  await interfaceApi.removeImpl(selected.value.id, c.id)
  await loadImpls()
  await load()
}
function goObject(c) { BL.info(`跳转到对象类型: ${c.display_name || c.api_name}（演示）`) }

async function copyText(t) {
  try { await navigator.clipboard.writeText(String(t || '')); BL.success('已复制') } catch {}
}

onMounted(async () => {
  await load()
  // 业务领域候选 + 所属行业解析
  const tree = await categoryApi.tree().catch(() => [])
  const list = []
  const info = {}
  const walk = (ns, ancestors) => ns.forEach(n => {
    const chain = [...ancestors, n]
    if (n.categoryCode) {
      list.push({ code: n.categoryCode, name: n.label })
      const industry = chain.find(x => x.categoryType === 1)
      info[n.categoryCode] = {
        label: n.label,
        industryLabel: industry ? industry.label : n.label,
        path: chain.map(x => x.label).join(' / ')
      }
    }
    if (n.children) walk(n.children, chain)
  })
  walk(tree, [])
  domainOpts.value = list
  catInfoByCode.value = info
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.two-pane {
  flex: 1;
  position: relative;
  display: flex;
  gap: 12px;
  padding: 12px;
  overflow: hidden;
}
.list-card { flex: 1; overflow: hidden; display: flex; flex-direction: column; min-width: 0; }
.list-scroll { flex: 1; min-height: 0; overflow: auto; }
.list-ft {
  flex-shrink: 0;
  padding: 8px 12px; border-top: 1px solid var(--bl-divider);
  display: flex; justify-content: space-between; align-items: center;
  font-size: var(--bl-fs-12);
}
.list-ft > div:first-child { display: inline-flex; align-items: center; flex-wrap: wrap; gap: 0; }

/* 批量操作按钮 (统一 outline 配色) */
.if-del-btn { background: #fff; border: 1px solid #f53f3f; color: #f53f3f; }
.if-del-btn:hover { background: #fff1f0; }
.if-ena-btn { background: #fff; border: 1px solid #00b42a; color: #00b42a; }
.if-ena-btn:hover { background: #e8fff4; }
.if-dis-btn { background: #fff; border: 1px solid #86909c; color: #4e5969; }
.if-dis-btn:hover { background: #f7f8fa; }
.if-clear-btn { color: var(--bl-text-3); }
.if-clear-btn:hover { color: var(--bl-primary); }

.seg {
  display: inline-flex; background: var(--bl-bg-2);
  border-radius: var(--bl-radius-2);
  padding: 2px; gap: 2px;
}
.seg-btn {
  height: 26px; padding: 0 12px;
  border: 0; background: transparent;
  font-size: var(--bl-fs-12); color: var(--bl-text-2); cursor: pointer;
  border-radius: 4px;
  display: inline-flex; align-items: center; gap: 6px;
}
.seg-btn.is-on { background: var(--bl-bg-1); color: var(--bl-primary); box-shadow: var(--bl-shadow-1); }
.seg-cnt { font-size: var(--bl-fs-11); color: var(--bl-text-3); }
.seg-btn.is-on .seg-cnt { color: var(--bl-primary); }

.search-wrap { position: relative; width: 240px; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.search-input { padding-left: 30px; }

/* 列表 / 分组 视图切换 */
.if-view-toggle {
  display: inline-flex; align-items: center;
  background: var(--bl-bg-2);
  border-radius: var(--bl-radius-2);
  padding: 2px;
}
.vt-btn {
  display: inline-flex; align-items: center; gap: 4px;
  height: 26px; padding: 0 10px;
  border: 0; background: transparent; cursor: pointer;
  font-size: var(--bl-fs-12); color: var(--bl-text-2);
  border-radius: var(--bl-radius-1, 4px);
}
.vt-btn:hover { color: var(--bl-text-1); }
.vt-btn.is-on {
  background: var(--bl-bg-1); color: var(--bl-primary);
  font-weight: 500; box-shadow: var(--bl-shadow-1, 0 1px 2px rgba(0,0,0,.08));
}
.hd-filter { width: 130px; }
.if-page-size { width: 64px; height: 26px; }

/* 表头排序 */
.th-sort { cursor: pointer; user-select: none; display: inline-flex; align-items: center; white-space: nowrap; }
.th-sort:hover { color: var(--bl-primary); }
.th-arrow { color: var(--bl-text-4); font-size: 11px; margin-left: 2px; }

/* 分组标题行 */
.if-group-row { cursor: pointer; background: var(--bl-bg-2); }
.if-group-row:hover { background: var(--bl-bg-hover); }
.if-group-row td {
  padding: 7px 12px !important;
  border-bottom: 1px solid var(--bl-border);
}
.if-group-chev { display: inline-flex; vertical-align: middle; color: var(--bl-text-3); margin-right: 6px; }
.if-group-label { font-weight: 600; font-size: var(--bl-fs-13); color: var(--bl-text-1); vertical-align: middle; }
.if-group-count {
  margin-left: 8px;
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-1); border: 1px solid var(--bl-border);
  border-radius: 9px; padding: 0 8px; line-height: 16px;
  vertical-align: middle; font-feature-settings: "tnum";
}
.if-row.is-grouped td:nth-child(2) { padding-left: 22px; }

.if-table thead th { position: sticky; top: 0; background: var(--bl-bg-2); z-index: 1; }
.if-row { cursor: pointer; }
.if-row.is-active { background: var(--bl-primary-soft); }
.if-row.is-active td { color: var(--bl-primary); }
.if-ic { width: 22px; height: 22px; border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.if-ic-lg { width: 36px; height: 36px; border-radius: 8px; }
.if-name-cell { display: flex; align-items: center; gap: 10px; min-width: 0; }
.if-name-text { min-width: 0; }
.if-name { font-weight: 500; }
.if-api  { font-size: var(--bl-fs-11); }
.link { color: var(--bl-primary); cursor: pointer; font-weight: 500; }
.link:hover { text-decoration: underline; }

/* 右侧详情抽屉 (全局层之上,覆盖整个视口高度) */
.if-detail {
  position: fixed; top: 0; right: 0; bottom: 0;
  background: var(--bl-bg-1);
  border-left: 1px solid var(--bl-border);
  box-shadow: -4px 0 16px rgba(0, 0, 0, 0.10);
  overflow: hidden;
  display: flex; flex-direction: column;
  min-width: 420px;
  z-index: 1000;
}
.if-drag-handle {
  position: absolute; left: -2px; top: 0; bottom: 0; width: 5px;
  cursor: col-resize; background: transparent;
  transition: background-color .15s; z-index: 6;
}
.if-drag-handle:hover, .if-drag-handle.is-resizing { background: var(--bl-primary); }
.if-drawer-enter-active, .if-drawer-leave-active { transition: transform .25s ease, opacity .2s ease; }
.if-drawer-enter-from, .if-drawer-leave-to { transform: translateX(20px); opacity: 0; }

/* 抽屉内 FieldRow 标签较长(标准对外名称 / API 名称 等),加宽避免换行 */
.if-detail :deep(.fr.fr-inline .fr-label) { width: 78px; }
.detail-hd {
  height: 56px; padding: 0 14px;
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid var(--bl-divider);
  gap: 8px;
}
.detail-body { flex: 1; overflow: auto; padding: 8px 14px; }
.detail-ft {
  padding: 10px 14px;
  display: flex; justify-content: flex-end; gap: 8px;
  border-top: 1px solid var(--bl-divider);
}
.dh-title { font-size: var(--bl-fs-14); font-weight: 600; }

.tabs { display: flex; padding: 0 16px; border-bottom: 1px solid var(--bl-divider); }
.tab {
  padding: 10px 14px; font-size: var(--bl-fs-13);
  color: var(--bl-text-2); cursor: pointer;
  border-bottom: 2px solid transparent; margin-bottom: -1px;
}
.tab.is-active { color: var(--bl-primary); border-color: var(--bl-primary); font-weight: 500; }

.sec {
  font-size: var(--bl-fs-12); color: var(--bl-text-3);
  margin: 16px 0 8px; padding-left: 8px;
  border-left: 3px solid var(--bl-primary);
}
.row-between { display: flex; align-items: center; justify-content: space-between; padding: 6px 0; }

.color-row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.color-dot { width: 26px; height: 26px; border-radius: 50%; cursor: pointer; border: 2px solid transparent; }
.color-dot.is-active { border-color: var(--bl-bg-1); box-shadow: 0 0 0 2px var(--bl-primary); }
.icon-row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.icon-cell {
  width: 32px; height: 32px; border-radius: var(--bl-radius-2);
  border: 1px solid var(--bl-border);
  display: inline-flex; align-items: center; justify-content: center;
  color: var(--bl-text-2); cursor: pointer;
}
.icon-cell.is-active { color: var(--bl-primary); border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.rid-row {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; background: var(--bl-bg-2); border-radius: var(--bl-radius-2);
  font-size: var(--bl-fs-12);
}

/* 属性行内编辑表格 */
.prop-table { table-layout: auto; }
.prop-table th, .prop-table td { padding: 6px 8px; vertical-align: middle; }
.prop-table td .bl-input { height: 28px; padding: 2px 8px; font-size: var(--bl-fs-12); width: 100%; min-width: 80px; }
.prop-row { cursor: default; }
.prop-row:hover { background: var(--bl-bg-hover); }
.prop-row.is-editing { background: var(--bl-primary-soft); }
.prop-row.is-editing:hover { background: var(--bl-primary-soft); }
.prop-comment { max-width: 160px; display: inline-block; vertical-align: middle; }
.fmt-pill {
  display: inline-flex; align-items: center;
  padding: 2px 8px; border-radius: 9px;
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-2); border: 1px dashed var(--bl-border);
  cursor: pointer;
  transition: background-color .12s, color .12s, border-color .12s;
}
.fmt-pill:hover { color: var(--bl-primary); border-color: var(--bl-primary); }
.fmt-pill.is-on {
  color: var(--bl-primary); background: var(--bl-primary-soft);
  border-style: solid; border-color: var(--bl-primary); font-weight: 500;
}

.impl-list { display: flex; flex-direction: column; gap: 6px; margin-top: 8px; }
.impl-row {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border: 1px solid var(--bl-border); border-radius: var(--bl-radius-2);
  font-size: var(--bl-fs-13);
}

.radio-group { display: inline-flex; align-items: center; gap: 20px; }
.radio-item { display: inline-flex; align-items: center; gap: 6px; cursor: pointer; }
.radio-item input { accent-color: var(--bl-primary); }

.mp-row {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 10px; border: 1px solid var(--bl-border); border-radius: var(--bl-radius-2);
  cursor: pointer;
}
.mp-row:hover { border-color: var(--bl-primary-border); }
.mp-row.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.mp-row input { accent-color: var(--bl-primary); }
</style>
