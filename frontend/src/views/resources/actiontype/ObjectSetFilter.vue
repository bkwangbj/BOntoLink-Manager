<template>
  <div class="os-wrap">
    <!-- 起始对象集 -->
    <div class="os-lbl">起始对象集</div>
    <button class="os-pick" @click="openPicker('class')">
      <template v-if="curClass">
        <span class="os-pick-ic" v-html="BL.icon('box', 11, '#fff')"></span>
        <span>{{ curClass.cn }}</span>
        <span class="bl-mono bl-muted" style="font-size:11px">{{ curClass.api_name }}</span>
      </template>
      <span v-else class="bl-muted">选择对象集…</span>
      <span v-if="objset.set_var" class="bl-tag bl-tag-primary" style="margin-left:2px">变量 {{ objset.set_var }}</span>
      <span style="flex:1"></span>
      <span class="bl-muted" v-html="BL.icon('chevronDown', 12)"></span>
    </button>

    <!-- 按属性过滤 -->
    <div v-for="(f, fi) in objset.filters" :key="fi" class="os-cond os-cond-row">
      <BlSelect v-model="f.property_code" :options="propOptions" size="sm" clearable placeholder="选择属性" style="width:180px;flex-shrink:0" />
      <BlSelect v-model="f.operator" :options="OS_OPERATORS" size="sm" style="width:100px;flex-shrink:0" />
      <input v-if="needValue(f.operator)" class="bl-input bl-input-sm" style="flex:1;min-width:0" v-model="f.value" placeholder="比较值" />
      <span v-else class="bl-muted os-cond-na">该运算符无需比较值</span>
      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" style="flex-shrink:0" title="移除过滤条件" @click="objset.filters.splice(fi,1)" v-html="BL.icon('x', 11)"></button>
    </div>
    <button class="fe-add-row" @click="addFilter"><span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">按属性过滤</span></button>

    <!-- 关联搜索 (可再按对端对象的属性过滤) -->
    <div v-if="objset.link_on" class="os-cond">
      <div class="os-cond-hd">
        <span class="os-cond-lbl">关联搜索至</span>
        <BlSelect v-model="objset.link_type_code" :options="linkTypeOptions" size="sm" clearable placeholder="选择链接类型" style="flex:1;max-width:260px" />
        <span style="flex:1"></span>
        <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="取消关联搜索" @click="objset.link_on = 0" v-html="BL.icon('x', 11)"></button>
      </div>
      <div class="os-path">{{ linkPath }}</div>
      <!-- 嵌套过滤: 属性取自对端对象, 不是起始对象 -->
      <template v-if="objset.link_type_code">
        <div v-for="(f, fi) in objset.link_filters" :key="fi" class="os-cond-row os-sub-row">
          <BlSelect v-model="f.property_code" :options="linkPropOptions" size="sm" clearable
                    :placeholder="`${linkClassName} 的属性`" style="width:180px;flex-shrink:0" />
          <BlSelect v-model="f.operator" :options="OS_OPERATORS" size="sm" style="width:100px;flex-shrink:0" />
          <input v-if="needValue(f.operator)" class="bl-input bl-input-sm" style="flex:1;min-width:0" v-model="f.value" placeholder="比较值" />
          <span v-else class="bl-muted os-cond-na">该运算符无需比较值</span>
          <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" style="flex-shrink:0" title="移除过滤条件" @click="objset.link_filters.splice(fi,1)" v-html="BL.icon('x', 11)"></button>
        </div>
        <button class="fe-add-row is-sub" @click="addLinkFilter"><span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">按 {{ linkClassName }} 的属性过滤</span></button>
      </template>
    </div>
    <button v-else class="fe-add-row" @click="objset.link_on = 1"><span v-html="BL.icon('link', 12)"></span><span style="margin-left:4px">关联搜索</span></button>

    <!-- 返回属性 (仅字符串多选: 作为下拉展示值; 对象引用参数的展示字段在「显示」页配置) -->
    <template v-if="variant === 'multi'">
      <div class="os-lbl" style="margin-top:6px">返回属性 <span class="bl-muted" style="font-weight:400">(作为下拉展示值, 可多选)</span></div>
      <button class="os-pick" @click="openPicker('prop')">
        <template v-if="labelPropList.length">
          <span v-for="p in labelPropList" :key="p.value" class="bl-tag bl-tag-primary os-ret-tag">{{ p.label }}</span>
        </template>
        <span v-else class="bl-muted">{{ objset.class_id ? '选择属性…' : '请先选择起始对象集' }}</span>
        <span style="flex:1"></span>
        <span class="bl-muted" v-html="BL.icon('chevronDown', 12)"></span>
      </button>
    </template>
    <a v-else class="os-reset" @click="reset">重置筛选</a>

    <!-- 对象集选择弹窗 (文档 3.6: 顶部搜索 + 全部对象 / 现有对象集变量 双栏) -->
    <Teleport to="body">
      <div v-if="pickerOpen" class="rlm-mask" @click.self="pickerOpen = false">
        <div class="rlm-modal osp-modal">
          <div class="rlm-hd"><span v-html="BL.icon('box', 14)"></span><span style="margin-left:6px">选择起始对象集</span><span style="flex:1"></span><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="pickerOpen = false" v-html="BL.icon('x', 14)"></button></div>
          <div class="osp-search"><span class="bl-muted" v-html="BL.icon('search', 13)"></span><input class="bl-input bl-input-sm" v-model="kw" placeholder="搜索对象类型 / 对象集变量…" /></div>
          <div class="osp-body">
            <!-- 左: 起始对象 (对象类 / 表单内对象引用参数 两个来源用页签切) -->
            <div class="osp-col">
              <div class="osp-tabs">
                <button v-for="t in SRC_TABS" :key="t.k" :class="['osp-tab', srcTab === t.k && 'is-on']" @click="srcTab = t.k">
                  {{ t.label }}<span class="osp-tab-n">{{ t.k === 'class' ? pickClasses.length : pickVars.length }}</span>
                </button>
              </div>
              <div class="osp-list">
                <template v-if="srcTab === 'class'">
                  <button v-for="c in pickClasses" :key="c.id" :class="['osp-item', objset.class_id === c.id && 'is-on']" @click="chooseClass(c)">
                    <span class="osp-item-ic" v-html="BL.icon('box', 11, '#fff')"></span>
                    <span class="bl-truncate" style="flex:1">{{ c.cn }}</span>
                    <span class="bl-mono bl-muted" style="font-size:11px">{{ c.api_name }}</span>
                  </button>
                  <div v-if="!pickClasses.length" class="bl-muted osp-tip">无匹配对象类型</div>
                </template>
                <template v-else>
                  <button v-for="v in pickVars" :key="v.value" :class="['osp-item', objset.set_var === v.value && 'is-on']" @click="chooseVar(v)">
                    <span class="osp-item-ic is-var" v-html="BL.icon('code', 11, '#fff')"></span>
                    <span class="bl-truncate" style="flex:1">{{ v.label }}</span>
                  </button>
                  <div v-if="!pickVars.length" class="bl-muted osp-tip">暂无对象引用参数</div>
                </template>
              </div>
            </div>

            <!-- 右: 该对象的属性, 多选 -->
            <div class="osp-col">
              <div class="osp-col-hd">
                <span>返回属性 <span class="bl-muted" style="font-weight:400">(可多选, 作为展示值)</span></span>
                <span style="flex:1"></span>
                <span class="bl-muted osp-cnt">已选 {{ labelPropList.length }} 项</span>
                <button v-if="labelPropList.length" class="osp-clear" @click="clearProps">清空</button>
              </div>
              <div class="osp-list">
                <template v-if="!objset.class_id">
                  <div class="bl-muted osp-tip">请先在左侧选择起始对象</div>
                </template>
                <template v-else>
                  <button v-for="p in pickProps" :key="p.value" :class="['osp-item', isPropOn(p.value) && 'is-on']" @click="toggleProp(p.value)">
                    <span class="osp-ck" v-html="isPropOn(p.value) ? BL.icon('check', 10, '#fff') : ''"></span>
                    <span class="bl-truncate" style="flex:1">{{ p.label }}</span>
                    <span v-if="p.status === 0" class="bl-tag bl-tag-warning osp-off">停用</span>
                  </button>
                  <div v-if="!pickProps.length" class="bl-muted osp-tip">{{ kw ? '无匹配属性' : '该对象暂无属性' }}</div>
                </template>
              </div>
            </div>
          </div>
          <div class="rlm-ft">
            <button class="bl-btn bl-btn-sm" style="margin-right:auto" @click="reset">清空选择</button>
            <button class="bl-btn bl-btn-primary bl-btn-sm" @click="pickerOpen = false">完成</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import BlSelect from '@/components/BlSelect.vue'
import { pickOps, needValue } from './conditionModel.js'

const props = defineProps({
  objset: { type: Object, required: true },
  classOptions: { type: Array, default: () => [] },   // [{ id, cn, api_name }]
  varOptions: { type: Array, default: () => [] },     // [{ value, label }] 表单内对象引用参数
  propOptions: { type: Array, default: () => [] },
  labelProp: { type: Object, default: null },
  linkTypeOptions: { type: Array, default: () => [] },
  linkPath: { type: String, default: '' },
  linkPropOptions: { type: Array, default: () => [] },   // 关联搜索对端对象的属性
  linkClassName: { type: String, default: '关联对象' },
  fallbackClassId: { type: String, default: '' },
  variant: { type: String, default: 'multi' },        // 'multi' 字符串多选 | 'object' 对象引用参数
})

/* 对象集过滤沿用的运算符子集与顺序; BlSelect 要 {value,label} 形态 */
const OS_OPERATORS = pickOps(['eq', 'ne', 'contains', 'startsWith', 'empty', 'notEmpty']).map(o => ({ value: o.key, label: o.label }))

const pickerOpen = ref(false)
const kw = ref('')
const SRC_TABS = [{ k: 'class', label: '全部对象' }, { k: 'var', label: '对象集变量' }]
const srcTab = ref('class')
const curClass = computed(() => props.classOptions.find(c => c.id === props.objset.class_id) || null)
const pickClasses = computed(() => {
  const q = kw.value.trim().toLowerCase()
  return props.classOptions.filter(c => !q || `${c.cn} ${c.api_name}`.toLowerCase().includes(q))
})
const pickVars = computed(() => {
  const q = kw.value.trim().toLowerCase()
  return props.varOptions.filter(v => !q || v.label.toLowerCase().includes(q))
})
const pickProps = computed(() => {
  const q = kw.value.trim().toLowerCase()
  return props.propOptions.filter(p => !q || String(p.label).toLowerCase().includes(q))
})
/* label_prop 存逗号分隔的属性编码, 单个时与旧的单选值完全一致, 老数据不用迁移 */
const labelPropCodes = computed(() => String(props.objset.label_prop || '').split(',').map(s => s.trim()).filter(Boolean))
const labelPropList = computed(() => labelPropCodes.value.map(code =>
  props.propOptions.find(p => p.value === code) || { value: code, label: code }))
function isPropOn(code) { return labelPropCodes.value.includes(code) }
function toggleProp(code) {
  const arr = labelPropCodes.value.slice()
  const i = arr.indexOf(code)
  i >= 0 ? arr.splice(i, 1) : arr.push(code)
  props.objset.label_prop = arr.join(',')
}
function clearProps() { props.objset.label_prop = '' }
function openPicker(tab) {
  kw.value = ''
  srcTab.value = tab === 'prop' ? 'class' : tab
  pickerOpen.value = true
}

function addFilter() {
  if (!props.objset.class_id) return BL.warning('请先选择起始对象集')
  props.objset.filters.push({ property_code:'', operator:'eq', value:'' })
}
function addLinkFilter() {
  if (!props.objset.link_type_code) return BL.warning('请先选择链接类型')
  if (!Array.isArray(props.objset.link_filters)) props.objset.link_filters = []
  props.objset.link_filters.push({ property_code:'', operator:'eq', value:'' })
}
function chooseClass(c) {
  const os = props.objset
  if (os.class_id !== c.id) { os.filters = []; os.label_prop = '' }
  os.class_id = c.id; os.set_var = ''
}
function chooseVar(v) {
  const os = props.objset
  os.set_var = os.set_var === v.value ? '' : v.value
  if (os.set_var && !os.class_id && props.fallbackClassId) os.class_id = props.fallbackClassId
}
function reset() {
  const os = props.objset
  os.class_id = ''; os.set_var = ''; os.filters = []
  os.link_on = 0; os.link_type_code = ''; os.link_filters = []; os.label_prop = ''
}
/* 换了链接类型, 对端对象跟着变, 旧的属性过滤条件不再成立 */
watch(() => props.objset.link_type_code, (nv, ov) => { if (ov !== undefined && nv !== ov) props.objset.link_filters = [] })
watch(() => props.objset.link_on, v => { if (!v) props.objset.link_filters = [] })
</script>

<style scoped>
.os-wrap { display: flex; flex-direction: column; }
.os-lbl { font-size: 12.5px; font-weight: 600; color: var(--bl-text-2); margin-top: 10px; margin-bottom: 6px; }
.os-pick { width: 100%; display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 8px; font-size: 13px; color: var(--bl-text-1); cursor: pointer; }
.os-pick:hover { border-color: var(--bl-primary); }
.os-pick-ic { width: 20px; height: 20px; border-radius: 5px; background: var(--bl-primary); flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.os-cond { border: 1px solid var(--bl-divider); border-radius: 8px; padding: 10px 12px; display: flex; flex-direction: column; gap: 8px; margin-top: 10px; }
.os-cond-hd { display: flex; align-items: center; gap: 8px; }
/* 过滤条件: 属性 / 运算符 / 比较值 / 删除 单行排布 */
.os-cond-row { flex-direction: row; align-items: center; }
.os-cond-na { flex: 1; min-width: 0; font-size: 12px; }
.os-cond-lbl { font-size: 12.5px; color: var(--bl-text-2); flex-shrink: 0; }
.os-path { font-size: 12px; color: var(--bl-text-3); line-height: 1.5; }
/* 关联搜索内的嵌套过滤: 左侧竖线 + 缩进, 表明从属于上面的链接 */
/* 横排靠自身声明 — 这里没套 .os-cond, 拿不到它的 display:flex */
.os-sub-row { display: flex; align-items: center; gap: 8px; margin-top: 8px;
  margin-left: 10px; padding-left: 10px; border-left: 2px solid var(--bl-divider); }
.fe-add-row.is-sub { margin-left: 10px; width: calc(100% - 10px); margin-top: 8px; padding: 7px; font-size: 12px; }
.os-ret { display: flex; align-items: center; gap: 8px; }
.os-reset { align-self: flex-start; margin-top: 10px; font-size: 12.5px; color: var(--bl-primary); cursor: pointer; }
.os-reset:hover { text-decoration: underline; }
/* 满宽虚线「添加一行」按钮 (与规则页 .fe-add-row 同形) */
.fe-add-row { width: 100%; display: flex; align-items: center; justify-content: center; padding: 9px; margin-top: 10px; background: transparent; border: 1px dashed var(--bl-border-strong); border-radius: 8px; color: var(--bl-text-2); font-size: 12.5px; cursor: pointer; transition: border-color .12s, color .12s, background .12s; }
.fe-add-row:hover { border-color: var(--bl-primary); color: var(--bl-primary); background: var(--bl-primary-soft); }
/* 对象集选择弹窗 */
.rlm-mask { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: center; justify-content: center; z-index: 1300; }
.rlm-modal { width: 640px; max-width: 92vw; max-height: 82vh; background: var(--bl-bg-1); border-radius: 12px; box-shadow: 0 16px 48px rgba(0,0,0,.3); display: flex; flex-direction: column; overflow: hidden; }
.rlm-hd { display: flex; align-items: center; padding: 14px 16px; font-size: 14px; font-weight: 600; border-bottom: 1px solid var(--bl-divider); }
.rlm-ft { padding: 12px 16px; border-top: 1px solid var(--bl-divider); display: flex; justify-content: flex-end; }
.osp-modal { width: 720px; }
.osp-search { display: flex; align-items: center; gap: 8px; padding: 12px 16px; border-bottom: 1px solid var(--bl-divider); }
.osp-search .bl-input { flex: 1; }
.osp-body { display: grid; grid-template-columns: 1fr 1fr; overflow: hidden; }
.osp-col { display: flex; flex-direction: column; min-width: 0; border-right: 1px solid var(--bl-divider); }
.osp-col:last-child { border-right: 0; }
.osp-col-hd { display: flex; align-items: center; gap: 6px; font-size: 12px; font-weight: 600; color: var(--bl-text-2); padding: 10px 14px 6px; }
.osp-cnt { font-size: 11.5px; font-weight: 400; }
.osp-clear { border: 0; background: transparent; color: var(--bl-primary); font-size: 11.5px; cursor: pointer; padding: 0; }
.osp-clear:hover { text-decoration: underline; }
/* 左栏来源页签: 对象类 / 对象集变量 */
.osp-tabs { display: flex; gap: 4px; padding: 8px 10px 4px; }
.osp-tab { display: inline-flex; align-items: center; gap: 5px; padding: 4px 10px; border: 0; border-radius: 6px; background: transparent;
  font-size: 12px; color: var(--bl-text-2); cursor: pointer; }
.osp-tab:hover { background: var(--bl-bg-hover); }
.osp-tab.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 600; }
.osp-tab-n { font-size: 11px; color: var(--bl-text-3); }
.osp-tab.is-on .osp-tab-n { color: var(--bl-primary); }
.osp-tip { padding: 12px; font-size: 12px; text-align: center; }
/* 属性多选: 勾选框贴左, 与对象列表的图标位对齐 */
.osp-ck { width: 15px; height: 15px; flex-shrink: 0; border: 1px solid var(--bl-border); border-radius: 3px;
  display: inline-flex; align-items: center; justify-content: center; background: var(--bl-bg-1); }
.osp-item.is-on .osp-ck { background: var(--bl-primary); border-color: var(--bl-primary); }
.osp-off { font-size: 11px; flex-shrink: 0; }
.os-ret-tag { margin-right: 4px; }
/* 定高: 搜索过滤后条目变少也不改变弹窗高度 */
.osp-list { height: min(340px, 46vh); overflow-y: auto; padding: 0 8px 10px; }
.osp-item { width: 100%; display: flex; align-items: center; gap: 8px; padding: 8px 10px; background: transparent; border: 0; border-radius: 6px; font-size: 13px; color: var(--bl-text-1); cursor: pointer; text-align: left; }
.osp-item:hover { background: var(--bl-bg-hover); }
.osp-item.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 600; }
.osp-item-ic { width: 18px; height: 18px; border-radius: 4px; background: var(--bl-primary); flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.osp-item-ic.is-var { background: #722ED1; }
</style>
