<template>
  <div class="page tc-page">
    <PageHeader title="类型类" subtitle="Type classes · 属性 / 关系 / 操作 三类元数据扩展">
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
          <span style="margin-left:4px">新建</span>
        </button>
      </template>
    </PageHeader>

    <div class="tc-main">
      <!-- 左:大类导航 -->
      <aside class="tc-nav">
        <div class="tc-nav-hd">
          <span style="font-weight:600;font-size:13px">大类</span>
          <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="新增大类" @click="openCatCreate" v-html="BL.icon('plus', 13)"></button>
        </div>
        <div class="tc-nav-list">
          <div :class="['tc-cat', selectedCat === '' && 'is-active']" @click="selectedCat = ''">
            <span class="tc-cat-ic" style="background:#86909c" v-html="BL.icon('grid', 12, '#fff')"></span>
            <span class="tc-cat-lbl">全部</span>
            <span class="tc-cat-cnt">{{ allCount }}</span>
          </div>
          <div v-for="c in categories" :key="c.category_code"
               :class="['tc-cat', selectedCat === c.category_code && 'is-active']"
               @click="selectedCat = c.category_code"
               @contextmenu.prevent="onCatContext($event, c)">
            <span class="tc-cat-ic" :style="{ background: c.color || '#165DFF' }" v-html="BL.icon(c.icon || 'cube', 12, '#fff')"></span>
            <span class="tc-cat-lbl" :title="c.category_code">{{ c.category_name_cn }}<span class="tc-cat-en">{{ c.category_code }}</span></span>
            <span class="tc-cat-cnt">{{ c.tc_count }}</span>
          </div>
        </div>
        <div class="tc-nav-ft" @click="enumMgr.open = true" title="统一维护下拉/校验枚举值">
          <span v-html="BL.icon('list', 13, 'var(--bl-text-2)')"></span>
          <span style="margin-left:6px">枚举管理</span>
        </div>
      </aside>

      <!-- 中:列表 -->
      <section class="tc-list-wrap">
        <div v-if="selectedIds.length" class="tc-batch">
          <span class="tc-batch-n">已选 {{ selectedIds.length }} 项</span>
          <button class="tc-batch-btn del" @click="batchDelete"><span v-html="BL.icon('trash', 12)"></span> 批量删除</button>
          <button class="tc-batch-btn" @click="selectedIds = []">取消选择</button>
        </div>
        <div class="tc-table-scroll">
          <table class="tc-table">
            <thead>
              <tr>
                <th class="col-chk"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
                <th class="col-apt">适用类型</th>
                <th class="col-cat">种类</th>
                <th class="col-name">名称</th>
                <th class="col-param">参数 / 可选值</th>
                <th class="col-st">状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in rows" :key="r.id" :class="current && current.id === r.id && 'is-sel'" @click="openDetail(r)">
                <td class="col-chk" @click.stop>
                  <input type="checkbox" :value="r.id" v-model="selectedIds" />
                </td>
                <td class="col-apt">
                  <span v-for="a in applyTypes(r)" :key="a" :class="['apt-tag', 'apt-' + a]">{{ aptLabel(a) }}</span>
                </td>
                <td class="col-cat">
                  <span class="cat-dot" :style="{ background: catColor(r.category_code) }"></span>
                  <span class="cat-cn">{{ catName(r.category_code) }}</span>
                  <span class="cat-en">{{ r.category_code }}</span>
                </td>
                <td class="col-name">
                  <div class="nm-en bl-mono">{{ r.name_template || r.name_prefix }}</div>
                  <div class="nm-cn">{{ r.name_cn_base }}</div>
                </td>
                <td class="col-param">
                  <span class="param-type">{{ paramTypeLabel(r.param_type) }}</span>
                  <span v-if="optionPreview(r)" class="param-opts" :title="optionPreview(r)">{{ optionPreview(r) }}</span>
                  <span v-else-if="r.demo_value && r.demo_value !== '-'" class="param-demo">示例:{{ r.demo_value }}</span>
                </td>
                <td class="col-st">
                  <span :class="['st-badge', toInt(r.is_deprecated) ? 'st-dep' : 'st-ok']">{{ toInt(r.is_deprecated) ? '已弃用' : '可用' }}</span>
                </td>
              </tr>
              <tr v-if="!rows.length"><td colspan="6" class="tc-empty">暂无类型类</td></tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- 详情:右侧可拉伸/最大化抽屉 + 左右两栏 -->
      <aside v-if="current" class="tc-drawer" :style="{ width: drawerWidth + 'px' }">
        <div class="tc-dr-drag" @mousedown="onDragStart" :class="resizing && 'is-resizing'"></div>
        <div class="tc-dr-hd">
          <span class="tc-dr-ic" :style="{ background: editForm.color || catColor(editForm.category_code) || '#165DFF' }"
                v-html="BL.icon(editForm.icon || catIcon(editForm.category_code) || 'cube', 16, '#fff')"></span>
          <div class="tc-dr-title">
            <div class="tc-dr-cn">{{ editForm.name_cn_base || '未命名' }}</div>
            <div class="tc-dr-en bl-mono">{{ editForm.name_template || editForm.name_prefix }}</div>
          </div>
          <span :class="['st-badge', toInt(editForm.is_deprecated) ? 'st-dep' : 'st-ok']">{{ toInt(editForm.is_deprecated) ? '已弃用' : '可用' }}</span>
          <button class="tc-dr-x" :title="drawerMaxed ? '恢复' : '最大化'" @click="toggleMax" v-html="BL.icon(drawerMaxed ? 'minimize' : 'maximize', 15)"></button>
          <button class="tc-dr-x" @click="closeDetail" v-html="BL.icon('x', 16)"></button>
        </div>

        <div class="tc-dr-main">
        <div class="tc-dr-col-l">
        <div class="tc-tabs">
          <div v-for="t in ['详情','显示','引用']" :key="t" :class="['tc-tab', activeTab === t && 'is-on']" @click="switchTab(t)">{{ t }}</div>
        </div>

        <div class="tc-dr-body">
          <!-- 详情页签 -->
          <template v-if="activeTab === '详情'">
            <div class="sec">基础信息</div>
            <div class="fr"><span class="fr-l">适用类型</span>
              <div class="fr-r apt-checks">
                <label v-for="a in catAllow(editForm.category_code)" :key="a" class="apt-ck">
                  <input type="checkbox" :value="a" v-model="editForm._apply" /> {{ aptLabel(a) }}
                </label>
              </div>
            </div>
            <div class="fr2">
              <div class="fr"><span class="fr-l">种类 *</span>
                <select class="bl-input" v-model="editForm.category_code" :disabled="isProtected" @change="onCatChange">
                  <option v-for="c in categories" :key="c.category_code" :value="c.category_code">{{ c.category_code }}</option>
                </select>
              </div>
              <div class="fr"><span class="fr-l">种类中文</span>
                <input class="bl-input" :value="catName(editForm.category_code)" readonly />
              </div>
            </div>
            <div class="fr2">
              <div class="fr"><span class="fr-l">名称 *</span><input class="bl-input bl-mono" v-model="editForm.name_prefix" :disabled="isProtected" /></div>
              <div class="fr"><span class="fr-l">名称中文 *</span><input class="bl-input" v-model="editForm.name_cn_base" /></div>
            </div>
            <div class="fr"><span class="fr-l">模板表达式</span><input class="bl-input bl-mono" v-model="editForm.name_template" placeholder="如 event_intent.<intent_>,普通类型类留空" /></div>

            <div class="sec">配置参数</div>
            <div class="fr2">
              <div class="fr"><span class="fr-l">参数类型</span>
                <select class="bl-input" v-model="editForm.param_type" :disabled="isProtected">
                  <option v-for="o in enums.param_type" :key="o.code" :value="o.code">{{ o.code }} {{ o.name }}</option>
                </select>
              </div>
              <div class="fr"><span class="fr-l">前端控件</span>
                <select class="bl-input" v-model="editForm.frontend_component">
                  <option v-for="o in enums.frontend_component" :key="o.code" :value="o.code">{{ o.code }} {{ o.name }}</option>
                </select>
              </div>
            </div>
            <div v-if="showOptionsField" class="fr"><span class="fr-l">参数可选值</span>
              <div class="tc-editor-wrap">
                <CodeEditor v-model="editForm.param_options_json" :rows="4" placeholder='可留空;枚举结构化 JSON' />
                <div class="tc-editor-hint">示例:[{"value":"danger","label_cn":"危险"}]</div>
              </div>
            </div>
            <div class="fr"><span class="fr-l">参数说明</span><input class="bl-input" v-model="editForm.param_desc" /></div>
            <div class="fr"><span class="fr-l">示例值</span><input class="bl-input" v-model="editForm.demo_value" placeholder='如 {"line_type":"solid"}' /></div>

            <!-- 多字段参数 Schema(param_json);可视化预览见右栏 -->
            <div v-if="showSchemaField" class="fr"><span class="fr-l">参数 Schema</span>
              <div class="tc-editor-wrap">
                <CodeEditor v-model="editForm.param_json" :rows="9" placeholder='可留空;多字段参数 JSON Schema' />
                <div class="tc-editor-hint">示例:{"line_type":{"type":"enum","enum":["solid","dashed"],"default":"solid","description":"曲线线型"}}</div>
              </div>
            </div>

            <div class="fr"><span class="fr-l">业务说明</span><textarea class="bl-input ta" rows="3" v-model="editForm.description"></textarea></div>

            <div class="sec">高级配置</div>
            <div class="fr2">
              <div class="fr"><span class="fr-l">来源类型</span>
                <select class="bl-input" v-model="editForm.source_type">
                  <option v-for="o in enums.source_type" :key="o.code" :value="o.code">{{ o.code }} {{ o.name }}</option>
                </select>
              </div>
              <div class="fr"><span class="fr-l">排序权重</span><input class="bl-input" type="number" v-model.number="editForm.sort_weight" /></div>
            </div>
            <div class="fr3">
              <label class="sw"><input type="checkbox" :checked="!!toInt(editForm.system_protected)" @change="e=>editForm.system_protected=e.target.checked?1:0" /> 系统保护</label>
              <label class="sw"><input type="checkbox" :checked="!!toInt(editForm.allow_multi_bind)" @change="e=>editForm.allow_multi_bind=e.target.checked?1:0" /> 允许多次绑定</label>
              <label class="sw"><input type="checkbox" :checked="!!toInt(editForm.is_array_value)" @change="e=>editForm.is_array_value=e.target.checked?1:0" /> 参数支持数组</label>
            </div>
            <div class="fr"><span class="fr-l">替代类型类</span>
              <select class="bl-input" v-model="editForm.replacement_meta_id">
                <option value="">无</option>
                <option v-for="t in rows.filter(x=>x.id!==editForm.id)" :key="t.id" :value="t.id">{{ t.name_cn_base }} ({{ t.name_prefix }})</option>
              </select>
            </div>

            <div class="sec">状态管理</div>
            <div class="fr"><span class="fr-l">状态</span>
              <div class="fr-r">
                <label class="rd"><input type="radio" :value="0" v-model="depState" /> 启用</label>
                <label class="rd"><input type="radio" :value="1" v-model="depState" /> 已弃用</label>
              </div>
            </div>
            <div v-if="depState === 1" class="fr"><span class="fr-l">弃用原因</span><textarea class="bl-input ta" rows="2" v-model="editForm.deprecated_reason"></textarea></div>
          </template>

          <!-- 显示页签 -->
          <template v-else-if="activeTab === '显示'">
            <div class="sec">视觉标识</div>
            <div class="disp-row"><IconPickerField v-model="editForm.icon" label="图标" :suggest-name="editForm.name_cn_base || editForm.name_prefix" :preset-count="32" /></div>
            <div class="disp-row"><ColorPickerField v-model="editForm.color" /></div>
            <div class="disp-hint">未设置时继承所属大类的图标与颜色。</div>
          </template>

          <!-- 引用页签 -->
          <template v-else>
            <div class="ref-cards">
              <div class="ref-card"><div class="ref-n">{{ bind.stats.total || 0 }}</div><div class="ref-l">总绑定</div></div>
              <div class="ref-card"><div class="ref-n">{{ bind.stats.property_count || 0 }}</div><div class="ref-l">属性</div></div>
              <div class="ref-card"><div class="ref-n">{{ bind.stats.relation_count || 0 }}</div><div class="ref-l">关系</div></div>
              <div class="ref-card"><div class="ref-n">{{ bind.stats.action_count || 0 }}</div><div class="ref-l">操作</div></div>
            </div>
            <div class="ref-filter">
              <select class="bl-input" v-model="bind.filterApt" @change="loadBinds">
                <option value="">全部载体</option>
                <option value="property">属性</option>
                <option value="relation">关系</option>
                <option value="action">操作</option>
              </select>
              <input class="bl-input" placeholder="搜索绑定主体 / 备注" v-model="bind.q" @input="loadBindsDebounced" />
            </div>
            <div class="ref-list">
              <div v-for="b in bind.list" :key="b.id" class="ref-item">
                <span :class="['apt-tag', 'apt-' + b.applicable_type]">{{ aptLabel(b.applicable_type) }}</span>
                <span class="ref-owner">{{ bindOwner(b) }}</span>
                <span class="ref-el bl-mono">{{ b.property_id || b.link_type_id || b.action_type_id || '—' }}</span>
                <span v-if="b.remark" class="ref-remark">{{ b.remark }}</span>
              </div>
              <div v-if="!bind.list.length" class="tc-empty" style="padding:24px 0">暂无绑定</div>
            </div>
          </template>
        </div>
        </div><!-- /tc-dr-col-l -->

        <!-- 右栏:参数预览(按 Schema 自动生成表单 + 实时预览 + JSON) -->
        <div class="tc-dr-col-r">
          <div class="tc-dr-pv-hd">参数预览</div>
          <template v-if="hasSchema">
            <div class="tc-pv-sub">按 Schema 自动生成表单</div>
            <ParamSchemaForm v-model="pvVal" :schema="editForm.param_json" />
            <div class="tc-pv-sub tc-pv-sub2">实时预览</div>
            <div class="tc-preview-box"><ParamPreview :name-prefix="editForm.name_prefix" :category="editForm.category_code" :values="pvVal" /></div>
            <div class="tc-pv-sub tc-pv-sub2">参数 JSON</div>
            <CodeEditor :model-value="pvJson" disabled :rows="6" />
          </template>
          <div v-else class="tc-pv-empty">该类型类为纯标记型或未定义参数 Schema,无可配置参数。<br>在左侧「参数 Schema」填入 JSON Schema 即可在此预览动态表单。</div>
        </div>
        </div><!-- /tc-dr-main -->

        <div class="tc-dr-ft">
          <button v-if="!isProtected" class="tc-dr-del" @click="deleteCurrent"><span v-html="BL.icon('trash', 12)"></span> 删除</button>
          <span v-else class="tc-dr-protect" v-html="BL.icon('lock', 12, 'var(--bl-text-3)') + '系统保护'"></span>
          <span class="bl-grow"></span>
          <button class="bl-btn" @click="closeDetail">取消</button>
          <button class="bl-btn bl-btn-primary" @click="saveCurrent">保存</button>
        </div>
      </aside>
    </div>

    <!-- 大类 新建/编辑 弹框 -->
    <div v-if="catModal.open" class="tc-mask" @click.self="catModal.open = false">
      <div class="tc-modal">
        <div class="tc-modal-hd"><span>{{ catModal.edit ? '编辑大类' : '新建大类' }}</span><button class="tc-dr-x" @click="catModal.open=false" v-html="BL.icon('x',16)"></button></div>
        <div class="tc-modal-body">
          <div class="fr2">
            <div class="fr"><span class="fr-l">编码 *</span><input class="bl-input bl-mono" v-model="catModal.form.category_code" :disabled="catModal.edit" placeholder="如 custom" /></div>
            <div class="fr"><span class="fr-l">中文名 *</span><input class="bl-input" v-model="catModal.form.category_name_cn" /></div>
          </div>
          <div class="disp-row"><IconPickerField v-model="catModal.form.icon" label="图标" :suggest-name="catModal.form.category_name_cn" :preset-count="24" /></div>
          <div class="disp-row"><ColorPickerField v-model="catModal.form.color" /></div>
          <div class="fr"><span class="fr-l">全局可挂载载体</span>
            <div class="fr-r apt-checks">
              <label class="apt-ck"><input type="checkbox" value="property" v-model="catModal.form._apply" /> 属性</label>
              <label class="apt-ck"><input type="checkbox" value="relation" v-model="catModal.form._apply" /> 关系</label>
              <label class="apt-ck"><input type="checkbox" value="action" v-model="catModal.form._apply" /> 操作</label>
            </div>
          </div>
          <div class="fr2">
            <div class="fr"><span class="fr-l">排序权重</span><input class="bl-input" type="number" v-model.number="catModal.form.sort_weight" /></div>
          </div>
          <div class="fr"><span class="fr-l">说明</span><textarea class="bl-input ta" rows="2" v-model="catModal.form.description"></textarea></div>
        </div>
        <div class="tc-modal-ft"><button class="bl-btn" @click="catModal.open=false">取消</button><button class="bl-btn bl-btn-primary" @click="saveCat">保存</button></div>
      </div>
    </div>

    <!-- 枚举字典管理 弹框 -->
    <div v-if="enumMgr.open" class="tc-mask" @click.self="enumMgr.open = false">
      <div class="tc-modal tc-modal-lg">
        <div class="tc-modal-hd"><span>枚举字典管理</span><button class="tc-dr-x" @click="enumMgr.open=false" v-html="BL.icon('x',16)"></button></div>
        <div class="tc-enum-mgr">
          <div class="tc-enum-names">
            <div v-for="n in enumMgr.names" :key="n" :class="['tc-enum-name', enumMgr.cur === n && 'is-on']" @click="selectEnum(n)">{{ enumLabel(n) }}</div>
          </div>
          <div class="tc-enum-data">
            <div class="tc-enum-bar">
              <span class="bl-muted" style="font-size:12px">{{ enumMgr.cur }}</span>
              <span class="bl-grow"></span>
              <button class="bl-btn bl-btn-text bl-btn-sm" @click="addEnumRow"><span v-html="BL.icon('plus',12)"></span> 新增项</button>
            </div>
            <table class="tc-enum-table">
              <thead><tr><th>编码</th><th>说明</th><th class="w70">排序</th><th class="w60">状态</th><th class="w40"></th></tr></thead>
              <tbody>
                <tr v-for="(it,i) in enumMgr.items" :key="it.id || ('new'+i)">
                  <td><input class="bl-input bl-mono sm" v-model="it.code" /></td>
                  <td><input class="bl-input sm" v-model="it.name" /></td>
                  <td><input class="bl-input sm" type="number" v-model.number="it.sort_no" /></td>
                  <td><label class="sw"><input type="checkbox" :checked="toInt(it.status)===1" @change="e=>it.status=e.target.checked?1:0" /></label></td>
                  <td><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="saveEnumRow(it)" title="保存" v-html="BL.icon('check',12,'#00b42a')"></button>
                      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="delEnumRow(it,i)" title="删除" v-html="BL.icon('trash',12)"></button></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import IconPickerField from '@/components/IconPickerField.vue'
import ColorPickerField from '@/components/ColorPickerField.vue'
import ParamSchemaForm from '@/components/typeclass/ParamSchemaForm.vue'
import CodeEditor from '@/components/CodeEditor.vue'
import ParamPreview from '@/components/typeclass/ParamPreview.vue'
import { BL } from '@/lib/bl.js'
import { typeClassApi, tcCategoryApi, tcBindApi, tcEnumApi } from '@/api'

/* ---------- 工具 ---------- */
function toInt (v) { return v === true ? 1 : (v === false ? 0 : (Number(v) || 0)) }
function parseList (v) { if (Array.isArray(v)) return v; try { return JSON.parse(v || '[]') } catch { return [] } }
const APT = { property: { label: '属性', color: '#165DFF' }, relation: { label: '关系', color: '#722ED1' }, action: { label: '操作', color: '#00B42A' } }
function aptLabel (a) { return (APT[a] || {}).label || a }
function aptStyle (a) { const c = (APT[a] || {}).color || '#86909c'; return { color: c, background: c + '14', border: '1px solid ' + c + '40' } }

/* ---------- 状态 ---------- */
const categories = ref([])
const selectedCat = ref('')
const allRows = ref([])
const q = ref('')
const filterApplicable = ref('')
const filterDeprecated = ref('')
const selectedIds = ref([])
const enums = reactive({ param_type: [], frontend_component: [], source_type: [], applicable_type: [] })

const rows = computed(() => {
  const kw = q.value.trim().toLowerCase()
  return allRows.value.filter(r => {
    if (selectedCat.value && r.category_code !== selectedCat.value) return false
    if (filterApplicable.value && !parseList(r.allow_apply_types).includes(filterApplicable.value)) return false
    if (filterDeprecated.value !== '' && toInt(r.is_deprecated) !== Number(filterDeprecated.value)) return false
    if (kw) {
      const hay = `${r.category_code} ${r.name_prefix} ${r.name_cn_base} ${r.description || ''}`.toLowerCase()
      if (!hay.includes(kw)) return false
    }
    return true
  })
})
const allCount = computed(() => allRows.value.filter(r => !selectedCat.value || r.category_code === selectedCat.value).length)
const activeCount = computed(() => rows.value.filter(r => !toInt(r.is_deprecated)).length)
const deprecatedCount = computed(() => rows.value.filter(r => toInt(r.is_deprecated)).length)
const allChecked = computed(() => rows.value.length > 0 && rows.value.every(r => selectedIds.value.includes(r.id)))

/* ---------- 大类辅助 ---------- */
const catMap = computed(() => Object.fromEntries(categories.value.map(c => [c.category_code, c])))
function catName (code) { return (catMap.value[code] || {}).category_name_cn || code }
function catColor (code) { return (catMap.value[code] || {}).color || '#165DFF' }
function catIcon (code) { return (catMap.value[code] || {}).icon || 'cube' }
function catAllow (code) { return parseList((catMap.value[code] || {}).global_allow_apply_types) }

/* ---------- 列表展示辅助 ---------- */
function applyTypes (r) { return parseList(r.allow_apply_types) }
function paramTypeLabel (pt) { const o = enums.param_type.find(x => x.code === pt); return o ? o.name : (pt || 'text') }
function optionPreview (r) { const opts = parseList(r.param_options_json); if (!opts.length) return ''; return opts.map(o => o.label_cn || o.value).slice(0, 5).join(' / ') }

/* ---------- 加载 ---------- */
async function loadCategories () { categories.value = await tcCategoryApi.list() || [] }
async function loadRows () { allRows.value = await typeClassApi.list() || [] }
async function loadEnums () {
  for (const name of ['param_type', 'frontend_component', 'source_type', 'applicable_type']) {
    enums[name] = await tcEnumApi.list('ont_dic_tc_' + name, true) || []
  }
}

/* ---------- 列表勾选 ---------- */
function toggleAll (e) {
  if (e.target.checked) selectedIds.value = [...new Set([...selectedIds.value, ...rows.value.map(r => r.id)])]
  else { const ids = new Set(rows.value.map(r => r.id)); selectedIds.value = selectedIds.value.filter(id => !ids.has(id)) }
}
async function batchDelete () {
  const prot = allRows.value.filter(r => selectedIds.value.includes(r.id) && toInt(r.system_protected))
  if (prot.length) return BL.warning(`${prot.length} 项为系统保护,不可删除`)
  const ok = await BL.confirm({ title: '批量删除', content: `确定删除选中的 ${selectedIds.value.length} 个类型类?`, danger: true, okText: '删除' })
  if (!ok) return
  for (const id of selectedIds.value) await typeClassApi.remove(id).catch(() => {})
  selectedIds.value = []
  await refresh()
  BL.success('已删除')
}

/* ---------- 详情抽屉 ---------- */
const current = ref(null)
const editForm = reactive({})
const activeTab = ref('详情')
const isNew = ref(false)
const isProtected = computed(() => !!toInt(editForm.system_protected) && !isNew.value)
const depState = ref(0)
watch(depState, v => { editForm.is_deprecated = v })

/* 抽屉:可拉伸(左缘拖拽)+ 可最大化,与对象类型抽屉一致 */
const drawerWidth = ref(0)
const drawerMaxed = ref(false)
const resizing = ref(false)
const DR_MIN = 480
function drDefault() { return Math.max(DR_MIN, Math.floor(window.innerWidth * 0.62)) }
function drMax() { return Math.floor(window.innerWidth * 0.92) }
function ensureWidth() { if (!drawerWidth.value) drawerWidth.value = drDefault() }
function toggleMax() {
  if (drawerMaxed.value) { drawerWidth.value = drDefault(); drawerMaxed.value = false }
  else { drawerWidth.value = drMax(); drawerMaxed.value = true }
}
let _dragX = 0, _dragW = 0
function onDragStart (e) {
  resizing.value = true; _dragX = e.clientX; _dragW = drawerWidth.value
  document.body.style.cursor = 'col-resize'; document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onDragMove); window.addEventListener('mouseup', onDragEnd)
}
function onDragMove (e) {
  drawerWidth.value = Math.max(DR_MIN, Math.min(_dragW + (_dragX - e.clientX), Math.floor(window.innerWidth * 0.95)))
  drawerMaxed.value = false
}
function onDragEnd () {
  resizing.value = false
  document.body.style.cursor = ''; document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onDragMove); window.removeEventListener('mouseup', onDragEnd)
}

/* 「参数可选值 / 参数 Schema」条件显隐:内容感知,绝不隐藏已有内容的字段 */
function ceNonEmpty (s) { const t = String(s || '').trim(); return !!t && !['{}', '[]', 'null'].includes(t) }
const showOptionsField = computed(() => ceNonEmpty(editForm.param_options_json) || (editForm.param_type === 'enum' && !ceNonEmpty(editForm.param_json)))
const showSchemaField = computed(() => ceNonEmpty(editForm.param_json) || editForm.param_type === 'json')

/* 参数 Schema 可视化预览:pvVal 为演示用参数值(从示例值初始化,不入库) */
const pvVal = ref({})
const pvJson = computed(() => JSON.stringify(pvVal.value, null, 2))
const hasSchema = computed(() => {
  const s = editForm.param_json
  if (!s) return false
  try {
    const o = typeof s === 'string' ? JSON.parse(s) : s
    const fields = o.properties && typeof o.properties === 'object' ? o.properties : o
    return fields && typeof fields === 'object' && Object.keys(fields).some(k => !['type', 'required', 'properties', '$schema'].includes(k))
  } catch { return false }
})
function resetPreview () {
  const d = editForm.demo_value
  try { pvVal.value = d ? (typeof d === 'string' ? JSON.parse(d) : d) : {} } catch { pvVal.value = {} }
}

function fillForm (r) {
  Object.keys(editForm).forEach(k => delete editForm[k])
  Object.assign(editForm, JSON.parse(JSON.stringify(r)))
  editForm._apply = parseList(r.allow_apply_types)
  if (editForm.param_options_json && typeof editForm.param_options_json !== 'string') editForm.param_options_json = JSON.stringify(editForm.param_options_json)
  if (editForm.param_json && typeof editForm.param_json !== 'string') editForm.param_json = JSON.stringify(editForm.param_json, null, 2)
  depState.value = toInt(r.is_deprecated)
  resetPreview()
}
function openDetail (r) {
  isNew.value = false
  current.value = r
  fillForm(r)
  activeTab.value = '详情'
  ensureWidth(); drawerMaxed.value = false
}
function closeDetail () { current.value = null; isNew.value = false }
function onCatChange () {
  const allow = catAllow(editForm.category_code)
  editForm._apply = (editForm._apply || []).filter(a => allow.includes(a))
}
function openCreate () {
  isNew.value = true
  const cat = selectedCat.value || (categories.value[0] && categories.value[0].category_code) || 'custom'
  current.value = { id: '__new__' }
  fillForm({
    id: '', category_code: cat, name_prefix: '', name_cn_base: '', name_template: '',
    param_type: 'text', frontend_component: 'text_input', source_type: 'user_custom',
    allow_apply_types: JSON.stringify(catAllow(cat)), system_protected: 0, allow_multi_bind: 0,
    is_array_value: 0, is_deprecated: 0, sort_weight: 999
  })
  activeTab.value = '详情'
  ensureWidth(); drawerMaxed.value = false
}

async function saveCurrent () {
  if (!editForm.category_code) return BL.warning('请选择种类')
  if (!editForm.name_prefix) return BL.warning('请填写名称')
  if (!editForm.name_cn_base) return BL.warning('请填写中文名称')
  const payload = { ...editForm }
  payload.allow_apply_types = editForm._apply || []
  delete payload._apply
  if (payload.param_options_json && typeof payload.param_options_json === 'string' && payload.param_options_json.trim()) {
    try { payload.param_options_json = JSON.parse(payload.param_options_json) } catch { return BL.error('参数可选值不是合法 JSON') }
  } else payload.param_options_json = null
  if (payload.param_json && typeof payload.param_json === 'string' && payload.param_json.trim()) {
    try { JSON.parse(payload.param_json) } catch { return BL.error('参数 Schema 不是合法 JSON') }
  } else payload.param_json = null
  try {
    if (isNew.value) { await typeClassApi.create(payload); BL.success('已创建') }
    else { await typeClassApi.update(editForm.id, payload); BL.success('已保存') }
    closeDetail()
    await refresh()
  } catch (e) { /* 拦截器已弹错误 */ }
}
async function deleteCurrent () {
  const ok = await BL.confirm({ title: '删除类型类', content: `确定删除「${editForm.name_cn_base}」?`, danger: true, okText: '删除' })
  if (!ok) return
  await typeClassApi.remove(editForm.id)
  closeDetail(); await refresh(); BL.success('已删除')
}

/* ---------- 引用页签 ---------- */
const bind = reactive({ stats: {}, list: [], filterApt: '', q: '' })
let bindTimer = null
function loadBindsDebounced () { clearTimeout(bindTimer); bindTimer = setTimeout(loadBinds, 250) }
async function loadBinds () {
  if (!current.value || isNew.value) { bind.list = []; return }
  bind.list = await tcBindApi.list(editForm.id, { applicableType: bind.filterApt, q: bind.q }) || []
}
async function switchTab (t) {
  activeTab.value = t
  if (t === '引用' && !isNew.value) {
    bind.filterApt = ''; bind.q = ''
    bind.stats = await tcBindApi.stats(editForm.id) || {}
    await loadBinds()
  }
}
function bindOwner (b) {
  if (b.applicable_type === 'property') return `${b.property_owner_type || ''}:${b.property_owner_id || ''}`
  if (b.applicable_type === 'relation') return b.link_type_id || ''
  return b.action_type_id || ''
}

/* ---------- 大类弹框 ---------- */
const catModal = reactive({ open: false, edit: false, form: {} })
function openCatCreate () { catModal.edit = false; catModal.form = { category_code: '', category_name_cn: '', icon: '', color: '#165DFF', _apply: ['property'], sort_weight: 999, description: '' }; catModal.open = true }
function openCatEdit (c) { catModal.edit = true; catModal.form = { ...c, _apply: parseList(c.global_allow_apply_types) }; catModal.open = true }
async function saveCat () {
  const f = catModal.form
  if (!f.category_code) return BL.warning('请填写编码')
  if (!f.category_name_cn) return BL.warning('请填写中文名')
  const payload = { ...f, global_allow_apply_types: f._apply || [] }
  delete payload._apply
  try {
    if (catModal.edit) await tcCategoryApi.update(f.category_code, payload)
    else await tcCategoryApi.create(payload)
    catModal.open = false
    await loadCategories()
    BL.success('已保存')
  } catch (e) { /* */ }
}
function onCatContext (e, c) {
  BL.confirm({ title: '大类操作', content: `「${c.category_name_cn}」(${c.tc_count} 个类型类)`, okText: '编辑', cancelText: '删除' }).then(edit => {
    if (edit) openCatEdit(c)
    else deleteCat(c)
  })
}
async function deleteCat (c) {
  if (c.tc_count > 0) return BL.warning(`该大类下有 ${c.tc_count} 个类型类,无法删除`)
  const ok = await BL.confirm({ title: '删除大类', content: `确定删除大类「${c.category_name_cn}」?`, danger: true, okText: '删除' })
  if (!ok) return
  await tcCategoryApi.remove(c.category_code)
  if (selectedCat.value === c.category_code) selectedCat.value = ''
  await loadCategories(); BL.success('已删除')
}

/* ---------- 枚举字典管理 ---------- */
const ENUM_LABELS = {
  ont_dic_tc_applicable_type: '适用载体类型', ont_dic_tc_property_owner_type: '属性所属层级',
  ont_dic_tc_source_type: '来源类型', ont_dic_tc_param_type: '参数类型',
  ont_dic_tc_frontend_component: '前端控件类型', ont_dic_tc_env: '环境标识'
}
function enumLabel (n) { return ENUM_LABELS[n] || n }
const enumMgr = reactive({ open: false, names: [], cur: '', items: [] })
watch(() => enumMgr.open, async v => { if (v) { enumMgr.names = await tcEnumApi.names() || []; if (enumMgr.names.length) selectEnum(enumMgr.cur || enumMgr.names[0]) } })
async function selectEnum (n) { enumMgr.cur = n; enumMgr.items = await tcEnumApi.list(n) || [] }
function addEnumRow () { enumMgr.items.push({ id: '', enum_name: enumMgr.cur, code: '', name: '', sort_no: (enumMgr.items.length + 1) * 10, status: 1 }) }
async function saveEnumRow (it) {
  if (!it.code) return BL.warning('请填写编码')
  it.enum_name = enumMgr.cur
  try {
    if (it.id) await tcEnumApi.update(it.id, it)
    else { const r = await tcEnumApi.create(it); it.id = r.id }
    await loadEnums()
    BL.success('已保存')
  } catch (e) { /* */ }
}
async function delEnumRow (it, i) {
  if (!it.id) { enumMgr.items.splice(i, 1); return }
  const ok = await BL.confirm({ title: '删除枚举项', content: `删除「${it.code}」?`, danger: true, okText: '删除' })
  if (!ok) return
  await tcEnumApi.remove(it.id); enumMgr.items.splice(i, 1); await loadEnums(); BL.success('已删除')
}

/* ---------- 刷新 ---------- */
async function refresh () { await Promise.all([loadRows(), loadCategories()]) }
onMounted(async () => { await Promise.all([loadCategories(), loadRows(), loadEnums()]) })
</script>

<style scoped>
.tc-page { display: flex; flex-direction: column; height: 100%; min-height: 0; }
.ov { display: inline-flex; gap: 10px; margin-right: 8px; }
.ov-item { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: var(--bl-text-2); }
.ov-item b { color: var(--bl-text-1); }
.ov-ok b { color: #00b42a; } .ov-risk b { color: #f53f3f; }
.hd-filter { width: 120px; }
.search-wrap { position: relative; }
.search-icon { position: absolute; left: 8px; top: 50%; transform: translateY(-50%); }
.search-input { padding-left: 28px; width: 200px; }

.tc-main { flex: 1; min-height: 0; display: flex; gap: 12px; padding: 12px; overflow: hidden; }

/* 左:大类导航 */
.tc-nav { width: 200px; flex-shrink: 0; display: flex; flex-direction: column; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; overflow: hidden; }
.tc-nav-hd { display: flex; align-items: center; justify-content: space-between; padding: 8px 10px; border-bottom: 1px solid var(--bl-divider); }
.tc-nav-list { flex: 1; overflow: auto; padding: 6px; }
.tc-cat { display: flex; align-items: center; gap: 8px; padding: 7px 8px; border-radius: 6px; cursor: pointer; }
.tc-cat:hover { background: var(--bl-bg-hover); }
.tc-cat.is-active { background: var(--bl-primary-soft); }
.tc-cat-ic { width: 22px; height: 22px; border-radius: 5px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.tc-cat-lbl { flex: 1; min-width: 0; font-size: 13px; display: flex; flex-direction: column; line-height: 1.25; }
.tc-cat-en { font-size: 10px; color: var(--bl-text-3); font-family: var(--bl-mono, monospace); }
.tc-cat-cnt { font-size: 11px; color: var(--bl-text-3); background: var(--bl-bg-2); padding: 1px 7px; border-radius: 10px; }
.tc-nav-ft { display: flex; align-items: center; padding: 9px 12px; border-top: 1px solid var(--bl-divider); cursor: pointer; font-size: 13px; color: var(--bl-text-2); }
.tc-nav-ft:hover { background: var(--bl-bg-hover); color: var(--bl-primary); }

/* 中:列表 */
.tc-list-wrap { flex: 1; min-width: 0; display: flex; flex-direction: column; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; overflow: hidden; }
.tc-batch { display: flex; align-items: center; gap: 10px; padding: 8px 12px; background: var(--bl-primary-soft); border-bottom: 1px solid var(--bl-border); }
.tc-batch-n { font-size: 12px; color: var(--bl-primary); font-weight: 600; }
.tc-batch-btn { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; padding: 3px 10px; border-radius: 4px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); color: var(--bl-text-2); cursor: pointer; }
.tc-batch-btn.del { border-color: #f53f3f; color: #f53f3f; }
.tc-table-scroll { flex: 1; overflow: auto; }
.tc-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.tc-table th { position: sticky; top: 0; background: var(--bl-thead-bg); text-align: left; padding: 0 10px; height: 40px; font-weight: 600; color: var(--bl-text-2); font-size: 12px; border-bottom: 1px solid var(--bl-thead-border); z-index: 1; }
.tc-table td { padding: 8px 10px; border-bottom: 1px solid var(--bl-divider); vertical-align: middle; }
.tc-table tbody tr { cursor: pointer; }
.tc-table tbody tr:hover { background: var(--bl-bg-hover); }
.tc-table tbody tr.is-sel { background: var(--bl-primary-soft); }
.col-chk { width: 36px; } .col-apt { width: 130px; } .col-cat { width: 150px; } .col-st { width: 80px; }
.apt-tag { display: inline-block; font-size: 11px; padding: 1px 7px; border-radius: 10px; margin-right: 4px; border: 1px solid transparent; color: var(--bl-text-2); }
/* 适用类型标签:浅色沉稳色 / 深色改浅柔和色, 避免深底上刺眼 */
.apt-tag.apt-property { color: #2563EB; background: rgba(37,99,235,.08); border-color: rgba(37,99,235,.25); }
.apt-tag.apt-relation { color: #7C3AED; background: rgba(124,58,237,.08); border-color: rgba(124,58,237,.25); }
.apt-tag.apt-action   { color: #16A34A; background: rgba(22,163,74,.08); border-color: rgba(22,163,74,.25); }
:root[data-theme="dark"] .apt-tag.apt-property { color: #3585fb; background: rgba(59, 131, 246, 0.222); border-color: rgba(48, 125, 248, 0.491); }
:root[data-theme="dark"] .apt-tag.apt-relation { color: #B197E8; background: rgba(124,58,237,.20); border-color: rgba(124,58,237,.34); }
:root[data-theme="dark"] .apt-tag.apt-action   { color: #5CC088; background: rgba(22,163,74,.20); border-color: rgba(22,163,74,.34); }
.cat-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 5px; vertical-align: middle; }
.cat-cn { font-size: 12.5px; } .cat-en { font-size: 10px; color: var(--bl-text-3); margin-left: 5px; font-family: var(--bl-mono, monospace); }
.nm-en { font-size: 12.5px; color: var(--bl-text-1); } .nm-cn { font-size: 11px; color: var(--bl-text-3); }
.param-type { font-size: 11px; color: var(--bl-text-2); background: var(--bl-bg-2); padding: 1px 6px; border-radius: 4px; margin-right: 6px; }
.param-opts { font-size: 11px; color: var(--bl-text-3); } .param-demo { font-size: 11px; color: var(--bl-text-3); }
.st-badge { font-size: 11px; padding: 2px 9px; border-radius: 10px; border: 1px solid transparent; }
.st-ok { color: #00b42a; background: rgba(0,180,42,.1); } .st-dep { color: #86909c; background: rgba(134,144,156,.12); }
/* 深色: 底提亮 + 边框 + 文字提亮 */
:root[data-theme="dark"] .st-ok  { color: #58d68a; background: rgba(34,197,94,.18); border-color: rgba(34,197,94,.36); }
:root[data-theme="dark"] .st-dep { color: #aeb9cf; background: rgba(148,163,184,.16); border-color: rgba(148,163,184,.3); }
.tc-empty { text-align: center; color: var(--bl-text-3); padding: 30px 0; font-size: 13px; }

/* 右:抽屉 */
/* 详情:右侧可拉伸/最大化侧边抽屉 + 左右两栏(对齐对象类型抽屉行为 + 文档 1.5 布局) */
.tc-drawer { position: fixed; top: 0; right: 0; bottom: 0; z-index: 1000; display: flex; flex-direction: column; background: var(--bl-bg-1); border-left: 1px solid var(--bl-border-strong); box-shadow: -12px 0 32px rgba(0,0,0,.22), -2px 0 6px rgba(0,0,0,.12); min-width: 480px; overflow: hidden; }
.tc-dr-drag { position: absolute; left: -2px; top: 0; bottom: 0; width: 5px; cursor: col-resize; background: transparent; transition: background-color .15s; z-index: 3; }
.tc-dr-drag:hover, .tc-dr-drag.is-resizing { background: var(--bl-primary); }
.tc-dr-main { flex: 1; min-height: 0; display: flex; }
.tc-dr-col-l { flex: 1; min-width: 0; display: flex; flex-direction: column; overflow: hidden; }
/* 参数预览面板:单独取一个更浅的背景色, 区别于左侧表单区(不跟随加深后的 bg-2) */
.tc-dr-col-r { width: 44%; max-width: 520px; flex-shrink: 0; border-left: 1px solid var(--bl-border); overflow: auto; padding: 14px 16px; background: #f4f7fc; }
:root[data-theme="dark"] .tc-dr-col-r { background: #131d33; }
.tc-dr-pv-hd { font-size: 14px; font-weight: 600; color: var(--bl-text-1); margin-bottom: 12px; }
.tc-pv-sub { font-size: 12px; font-weight: 600; color: var(--bl-text-2); margin-bottom: 8px; }
.tc-pv-sub2 { margin-top: 14px; }
.tc-pv-empty { font-size: 12.5px; color: var(--bl-text-3); line-height: 1.8; padding: 20px 10px; background: var(--bl-bg-1); border: 1px dashed var(--bl-border); border-radius: 8px; }
.tc-dr-hd { display: flex; align-items: center; gap: 10px; padding: 12px 14px; border-bottom: 1px solid var(--bl-divider); }
.tc-dr-ic { width: 36px; height: 36px; border-radius: 8px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.tc-dr-title { flex: 1; min-width: 0; } .tc-dr-cn { font-size: 14px; font-weight: 600; } .tc-dr-en { font-size: 11px; color: var(--bl-text-3); }
.tc-dr-x { border: 0; background: transparent; cursor: pointer; color: var(--bl-text-3); padding: 4px; border-radius: 4px; }
.tc-dr-x:hover { background: var(--bl-bg-hover); }
.tc-tabs { display: flex; padding: 0 12px; border-bottom: 1px solid var(--bl-divider); }
.tc-tab { padding: 9px 14px; font-size: 13px; color: var(--bl-text-2); cursor: pointer; border-bottom: 2px solid transparent; }
.tc-tab.is-on { color: var(--bl-primary); border-bottom-color: var(--bl-primary); font-weight: 600; }
.tc-dr-body { flex: 1; overflow: auto; padding: 12px 14px; }
.sec { font-size: 12px; color: var(--bl-text-2); font-weight: 600; margin: 14px 0 8px; padding-left: 8px; border-left: 3px solid var(--bl-primary); }
.sec:first-child { margin-top: 0; }
.fr { display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; }
.fr-l { font-size: 12px; color: var(--bl-text-2); } .fr-r { display: flex; gap: 14px; align-items: center; }
.fr2 { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.fr3 { display: flex; gap: 16px; margin-bottom: 10px; }
.ta { resize: vertical; font-family: inherit; min-height: 84px; }
.tc-editor-wrap { display: flex; flex-direction: column; gap: 3px; }
.tc-editor-hint { font-size: 11px; color: var(--bl-text-3); line-height: 1.5; word-break: break-all; }
/* 参数 Schema 可视化预览 */
.tc-preview { margin: 4px 0 8px; padding: 10px; border: 1px solid var(--bl-border); border-radius: 8px; background: var(--bl-bg-2); }
.tc-preview-hd { font-size: 12px; font-weight: 600; color: var(--bl-text-2); margin-bottom: 8px; }
.tc-preview-hd2 { margin-top: 12px; }
.tc-preview-box { height: 140px; border: 1px solid var(--bl-border); border-radius: 6px; background: var(--bl-bg-1); display: flex; align-items: center; justify-content: center; overflow: hidden; }
.tc-preview-json { margin: 0; padding: 8px 10px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 6px; font-family: var(--bl-mono, ui-monospace, monospace); font-size: 11.5px; line-height: 1.6; white-space: pre-wrap; max-height: 160px; overflow: auto; }
.apt-checks { display: flex; gap: 14px; } .apt-ck, .sw, .rd { display: inline-flex; align-items: center; gap: 5px; font-size: 13px; cursor: pointer; }
.disp-row { margin-bottom: 14px; } .disp-hint { font-size: 12px; color: var(--bl-text-3); margin-top: 6px; }
.tc-dr-ft { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border-top: 1px solid var(--bl-divider); }
.tc-dr-del { display: inline-flex; align-items: center; gap: 4px; font-size: 13px; color: #f53f3f; background: transparent; border: 0; cursor: pointer; }
.tc-dr-protect { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: var(--bl-text-3); }
.bl-grow { flex: 1; }
/* 引用 */
.ref-cards { display: grid; grid-template-columns: repeat(4,1fr); gap: 8px; margin-bottom: 12px; }
.ref-card { background: var(--bl-bg-2); border-radius: 6px; padding: 10px; text-align: center; }
.ref-n { font-size: 20px; font-weight: 700; color: var(--bl-text-1); } .ref-l { font-size: 11px; color: var(--bl-text-3); }
.ref-filter { display: flex; gap: 8px; margin-bottom: 10px; } .ref-filter .bl-input { flex: 1; }
.ref-item { display: flex; align-items: center; gap: 8px; padding: 8px; border-bottom: 1px solid var(--bl-divider); font-size: 12.5px; }
.ref-owner { color: var(--bl-text-2); } .ref-el { color: var(--bl-text-1); } .ref-remark { color: var(--bl-text-3); margin-left: auto; font-size: 11px; }

/* 弹框 */
.tc-mask { position: fixed; inset: 0; background: rgba(0,0,0,.35); display: flex; align-items: center; justify-content: center; z-index: 1200; }
.tc-modal { width: 520px; max-height: 86vh; display: flex; flex-direction: column; background: var(--bl-bg-1); border-radius: 10px; overflow: hidden; }
.tc-modal-lg { width: 760px; }
.tc-modal-hd { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid var(--bl-divider); font-weight: 600; }
.tc-modal-body { flex: 1; overflow: auto; padding: 14px 16px; }
.tc-modal-ft { display: flex; justify-content: flex-end; gap: 8px; padding: 10px 16px; border-top: 1px solid var(--bl-divider); }
/* 枚举管理 */
.tc-enum-mgr { display: flex; height: 60vh; }
.tc-enum-names { width: 180px; border-right: 1px solid var(--bl-divider); overflow: auto; padding: 6px; }
.tc-enum-name { padding: 8px 10px; border-radius: 6px; font-size: 13px; cursor: pointer; }
.tc-enum-name:hover { background: var(--bl-bg-hover); } .tc-enum-name.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); }
.tc-enum-data { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.tc-enum-bar { display: flex; align-items: center; padding: 8px 12px; border-bottom: 1px solid var(--bl-divider); }
.tc-enum-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.tc-enum-table th { text-align: left; padding: 7px 8px; font-size: 12px; color: var(--bl-text-2); border-bottom: 1px solid var(--bl-border); }
.tc-enum-table td { padding: 5px 8px; border-bottom: 1px solid var(--bl-divider); }
.tc-enum-table .w70 { width: 70px; } .tc-enum-table .w60 { width: 56px; } .tc-enum-table .w40 { width: 56px; }
.bl-input.sm { padding: 4px 8px; font-size: 12.5px; }
</style>
