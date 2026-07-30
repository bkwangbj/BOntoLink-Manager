<template>
  <div class="os-wrap">
    <div class="fd-warn os-warn"><span v-html="BL.icon('info', 13, '#92400E')"></span><span>此筛选条件对所有可查看此操作的用户可见。</span><a class="os-more" @click="BL.info('帮助文档待接入')">了解更多</a></div>

    <!-- 起始对象集 -->
    <div class="os-lbl">起始对象集</div>
    <button class="os-pick" @click="pickerOpen = true">
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
      <input v-if="!OS_NO_VALUE_OPS.includes(f.operator)" class="bl-input bl-input-sm" style="flex:1;min-width:0" v-model="f.value" placeholder="比较值" />
      <span v-else class="bl-muted os-cond-na">该运算符无需比较值</span>
      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" style="flex-shrink:0" title="移除过滤条件" @click="objset.filters.splice(fi,1)" v-html="BL.icon('x', 11)"></button>
    </div>
    <button class="fe-add-row" @click="addFilter"><span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">按属性过滤</span></button>

    <!-- 关联搜索 -->
    <div v-if="objset.link_on" class="os-cond">
      <div class="os-cond-hd">
        <span class="os-cond-lbl">关联搜索至</span>
        <BlSelect v-model="objset.link_type_code" :options="linkTypeOptions" size="sm" clearable placeholder="选择链接类型" style="flex:1;max-width:260px" />
        <span style="flex:1"></span>
        <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="取消关联搜索" @click="objset.link_on = 0" v-html="BL.icon('x', 11)"></button>
      </div>
      <div class="os-path">{{ linkPath }}</div>
    </div>
    <button v-else class="fe-add-row" @click="objset.link_on = 1"><span v-html="BL.icon('link', 12)"></span><span style="margin-left:4px">关联搜索</span></button>

    <!-- 返回属性 (仅字符串多选: 作为下拉展示值; 对象引用参数的展示字段在「显示」页配置) -->
    <template v-if="variant === 'multi'">
      <div class="os-lbl" style="margin-top:6px">返回属性 <span class="bl-muted" style="font-weight:400">(作为下拉展示值)</span></div>
      <div class="os-ret">
        <BlSelect v-model="objset.label_prop" :options="propOptions" clearable :placeholder="objset.class_id ? '选择属性' : '请先选择起始对象集'" style="flex:1" />
        <span v-if="labelProp" :class="['bl-tag', labelProp.status === 0 ? 'bl-tag-warning' : 'bl-tag-success']">{{ labelProp.status === 0 ? '停用' : '启用' }}</span>
      </div>
    </template>
    <a v-else class="os-reset" @click="reset">重置筛选</a>

    <!-- 对象集选择弹窗 (文档 3.6: 顶部搜索 + 全部对象 / 现有对象集变量 双栏) -->
    <Teleport to="body">
      <div v-if="pickerOpen" class="rlm-mask" @click.self="pickerOpen = false">
        <div class="rlm-modal osp-modal">
          <div class="rlm-hd"><span v-html="BL.icon('box', 14)"></span><span style="margin-left:6px">选择起始对象集</span><span style="flex:1"></span><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="pickerOpen = false" v-html="BL.icon('x', 14)"></button></div>
          <div class="osp-search"><span class="bl-muted" v-html="BL.icon('search', 13)"></span><input class="bl-input bl-input-sm" v-model="kw" placeholder="搜索对象类型 / 对象集变量…" /></div>
          <div class="osp-body">
            <div class="osp-col">
              <div class="osp-col-hd">全部对象</div>
              <div class="osp-list">
                <button v-for="c in pickClasses" :key="c.id" :class="['osp-item', objset.class_id === c.id && 'is-on']" @click="chooseClass(c)">
                  <span class="osp-item-ic" v-html="BL.icon('box', 11, '#fff')"></span>
                  <span class="bl-truncate" style="flex:1">{{ c.cn }}</span>
                  <span class="bl-mono bl-muted" style="font-size:11px">{{ c.api_name }}</span>
                </button>
                <div v-if="!pickClasses.length" class="bl-muted" style="padding:12px;font-size:12px;text-align:center">无匹配对象类型</div>
              </div>
            </div>
            <div class="osp-col">
              <div class="osp-col-hd">现有对象集变量 <span class="bl-muted" style="font-weight:400">(表单内对象引用参数)</span></div>
              <div class="osp-list">
                <button v-for="v in pickVars" :key="v.value" :class="['osp-item', objset.set_var === v.value && 'is-on']" @click="chooseVar(v)">
                  <span class="osp-item-ic is-var" v-html="BL.icon('code', 11, '#fff')"></span>
                  <span class="bl-truncate" style="flex:1">{{ v.label }}</span>
                </button>
                <div v-if="!pickVars.length" class="bl-muted" style="padding:12px;font-size:12px;text-align:center">暂无对象引用参数</div>
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
import { ref, computed } from 'vue'
import { BL } from '@/lib/bl.js'
import BlSelect from '@/components/BlSelect.vue'

const props = defineProps({
  objset: { type: Object, required: true },
  classOptions: { type: Array, default: () => [] },   // [{ id, cn, api_name }]
  varOptions: { type: Array, default: () => [] },     // [{ value, label }] 表单内对象引用参数
  propOptions: { type: Array, default: () => [] },
  labelProp: { type: Object, default: null },
  linkTypeOptions: { type: Array, default: () => [] },
  linkPath: { type: String, default: '' },
  fallbackClassId: { type: String, default: '' },
  variant: { type: String, default: 'multi' },        // 'multi' 字符串多选 | 'object' 对象引用参数
})

const OS_OPERATORS = [
  { value:'eq', label:'等于' }, { value:'ne', label:'不等于' }, { value:'contains', label:'包含' },
  { value:'startsWith', label:'开头是' }, { value:'empty', label:'为空' }, { value:'notEmpty', label:'不为空' },
]
const OS_NO_VALUE_OPS = ['empty', 'notEmpty']

const pickerOpen = ref(false)
const kw = ref('')
const curClass = computed(() => props.classOptions.find(c => c.id === props.objset.class_id) || null)
const pickClasses = computed(() => {
  const q = kw.value.trim().toLowerCase()
  return props.classOptions.filter(c => !q || `${c.cn} ${c.api_name}`.toLowerCase().includes(q))
})
const pickVars = computed(() => {
  const q = kw.value.trim().toLowerCase()
  return props.varOptions.filter(v => !q || v.label.toLowerCase().includes(q))
})

function addFilter() {
  if (!props.objset.class_id) return BL.warning('请先选择起始对象集')
  props.objset.filters.push({ property_code:'', operator:'eq', value:'' })
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
  os.class_id = ''; os.set_var = ''; os.filters = []; os.link_on = 0; os.link_type_code = ''; os.label_prop = ''
}
</script>

<style scoped>
.os-wrap { display: flex; flex-direction: column; }
.os-warn { display: flex; align-items: center; gap: 6px; }
.os-warn > span:first-child { flex-shrink: 0; display: inline-flex; }
.os-more { color: var(--bl-primary); cursor: pointer; margin-left: 2px; }
.os-more:hover { text-decoration: underline; }
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
.os-ret { display: flex; align-items: center; gap: 8px; }
.os-reset { align-self: flex-start; margin-top: 10px; font-size: 12.5px; color: var(--bl-primary); cursor: pointer; }
.os-reset:hover { text-decoration: underline; }
/* 满宽虚线「添加一行」按钮 (与规则页 .fe-add-row 同形) */
.fe-add-row { width: 100%; display: flex; align-items: center; justify-content: center; padding: 9px; margin-top: 10px; background: transparent; border: 1px dashed var(--bl-border-strong); border-radius: 8px; color: var(--bl-text-2); font-size: 12.5px; cursor: pointer; transition: border-color .12s, color .12s, background .12s; }
.fe-add-row:hover { border-color: var(--bl-primary); color: var(--bl-primary); background: var(--bl-primary-soft); }
.fd-warn { background: #FEF3C7; border: 1px solid #FDE68A; border-radius: 6px; padding: 8px 12px; font-size: 12px; color: #92400E; }
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
.osp-col-hd { font-size: 12px; font-weight: 600; color: var(--bl-text-2); padding: 10px 14px 6px; }
/* 定高: 搜索过滤后条目变少也不改变弹窗高度 */
.osp-list { height: min(340px, 46vh); overflow-y: auto; padding: 0 8px 10px; }
.osp-item { width: 100%; display: flex; align-items: center; gap: 8px; padding: 8px 10px; background: transparent; border: 0; border-radius: 6px; font-size: 13px; color: var(--bl-text-1); cursor: pointer; text-align: left; }
.osp-item:hover { background: var(--bl-bg-hover); }
.osp-item.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 600; }
.osp-item-ic { width: 18px; height: 18px; border-radius: 4px; background: var(--bl-primary); flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.osp-item-ic.is-var { background: #722ED1; }
</style>
