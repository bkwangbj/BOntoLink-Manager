<template>
  <Teleport to="body">
    <transition name="fnw-fade">
      <div v-if="open" class="fnw-mask">
        <div class="fnw-modal">
          <!-- ===== 顶部步骤条 ===== -->
          <div class="fnw-steps">
            <template v-for="(s, i) in STEPS" :key="s">
              <div class="fnw-step" :class="[step === i + 1 && 'is-cur', step > i + 1 && 'is-done']">
                <span class="fnw-step-num">{{ i + 1 }}</span>
                <span class="fnw-step-lbl">{{ s }}</span>
              </div>
              <span v-if="i < STEPS.length - 1" class="fnw-step-line" :class="step > i + 1 && 'is-done'"></span>
            </template>
            <button class="bl-btn bl-btn-text bl-btn-icon fnw-x" title="关闭" @click="onCancel" v-html="BL.icon('x', 14)"></button>
          </div>

          <!-- ===== 内容区 ===== -->
          <div class="fnw-body">
            <div class="fnw-inner">

              <!-- ========== 步骤 1: 选择开发语言 ========== -->
              <template v-if="step === 1">
                <h2 class="fnw-h1">选择开发语言</h2>
                <div class="fnw-h1-sub">函数仓库支持创建可复用的业务逻辑,可在全平台范围内共享调用。</div>

                <div v-if="langLocked" class="fnw-locked">
                  <span v-html="BL.icon('lock', 12)"></span>
                  开发语言已锁定,如需更换请取消创建后重新开始(文档 3.1.4)
                </div>

                <div class="fnw-lang">
                  <div v-for="l in LANGS" :key="l.v"
                       :class="['fnw-lang-card', form.language === l.v && 'is-on', langLocked && form.language !== l.v && 'is-dim']"
                       @click="pickLang(l.v)">
                    <div class="fnw-lang-hd">
                      <span class="fnw-lang-ic" :style="{ background: l.color }">{{ l.badge }}</span>
                      <span class="fnw-lang-name">{{ l.label }}</span>
                    </div>
                    <div class="fnw-lang-desc">{{ l.desc }}</div>
                  </div>
                </div>

                <!-- 语言能力差异对比表 (6 维) -->
                <table class="bl-table fnw-cmp">
                  <thead>
                    <tr>
                      <th class="t-left" style="width:110px">对比项</th>
                      <th class="t-left">Python 函数</th>
                      <th class="t-left">TypeScript 函数</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="c in COMPARE" :key="c.k">
                      <td class="t-left fnw-cmp-k">{{ c.k }}</td>
                      <td class="t-left" :class="form.language === 1 && 'is-hl'">{{ c.py }}</td>
                      <td class="t-left" :class="form.language === 2 && 'is-hl'">{{ c.ts }}</td>
                    </tr>
                  </tbody>
                </table>
              </template>

              <!-- ========== 步骤 2: 配置基础信息 ========== -->
              <template v-else-if="step === 2">
                <h2 class="fnw-h1">配置基础信息</h2>
                <div class="fnw-h1-sub">设置函数的归属、命名与基础属性</div>

                <div class="fnw-card">
                  <!-- 目录: 行业 1/3 + 领域 1/3 + 路径 1/3 -->
                  <div class="fnw-row">
                    <label class="fnw-lbl"><i class="fnw-req">*</i>目录:</label>
                    <div class="fnw-dir">
                      <ComboInput v-model="form.industry_dir" :options="industryOptions" placeholder="行业(如 水利)" />
                      <ComboInput v-model="form.category_dir" :options="categoryOptions" placeholder="领域(如 水文监测函数集)" />
                      <div class="fnw-dir-path">
                        <input class="bl-input bl-mono" :value="dirPath" readonly placeholder="/行业/领域" />
                        <button class="bl-btn fnw-pick-btn" @click="dirPickerOpen = true">
                          <span v-html="BL.icon('folderOpen', 12)"></span><span style="margin-left:4px">选择</span>
                        </button>
                      </div>
                    </div>
                  </div>

                  <!-- 文件 + API 两列均分 -->
                  <div class="fnw-row fnw-row-2">
                    <div class="fnw-col">
                      <label class="fnw-lbl"><i class="fnw-req">*</i>文件:</label>
                      <div class="fnw-file">
                        <div class="fnw-file-grp">
                          <input class="bl-input bl-mono fnw-file-input" v-model="fileBase" placeholder="文件名" />
                          <span class="fnw-file-ext bl-mono">.{{ ext }}</span>
                        </div>
                        <button class="bl-btn fnw-pick-btn" @click="filePickerOpen = true">
                          <span v-html="BL.icon('file', 12)"></span><span style="margin-left:4px">选择</span>
                        </button>
                      </div>
                    </div>
                    <div class="fnw-col">
                      <label class="fnw-lbl"><i class="fnw-req">*</i>API:</label>
                      <input class="bl-input bl-mono" v-model="form.api_name" placeholder="小驼峰,如 getHydrologyStationThresholds" />
                    </div>
                  </div>
                  <!-- 文件 / API 实时校验提示 -->
                  <div class="fnw-row fnw-row-2 fnw-hintrow">
                    <div class="fnw-col">
                      <div class="fnw-hint" :class="check.file_exists ? 'is-info' : 'is-muted'">
                        {{ fileHint }}
                      </div>
                    </div>
                    <div class="fnw-col">
                      <div class="fnw-hint" :class="apiHintCls">{{ apiHint }}</div>
                    </div>
                  </div>

                  <!-- 名称 + 函数类型 两列均分 -->
                  <div class="fnw-row fnw-row-2">
                    <div class="fnw-col">
                      <label class="fnw-lbl">名称:</label>
                      <input class="bl-input" v-model="form.function_label" placeholder="中文显示名称(非必填)" />
                    </div>
                    <div class="fnw-col">
                      <label class="fnw-lbl"><i class="fnw-req">*</i>函数类型:</label>
                      <BlSelect v-model="functionTypeStr" :options="TYPE_OPTS" placeholder="选择函数类型" style="flex:1" />
                    </div>
                  </div>

                  <!-- 函数说明 -->
                  <div class="fnw-row fnw-row-ta">
                    <label class="fnw-lbl">函数说明:</label>
                    <textarea class="bl-input fnw-ta" v-model="form.rdfs_comment" rows="3"
                              placeholder="描述函数的业务用途、适用场景(非必填)"></textarea>
                  </div>

                  <!-- 系统派生: 类名与访问路径预览 -->
                  <div class="fnw-derived">
                    <span v-html="BL.icon('info', 12)"></span>
                    创建后系统自动生成:类名 <b class="bl-mono">{{ derivedClassName || '—' }}</b> ·
                    访问路径 <b class="bl-mono">{{ fullAccessPath || '—' }}</b> ·
                    初始版本 <b class="bl-mono">v0.0.1 草稿</b> · 全局 RID · 初始模板代码
                  </div>
                </div>
              </template>

              <!-- ========== 步骤 3: 定义函数签名 ========== -->
              <template v-else>
                <h2 class="fnw-h1">定义函数签名</h2>
                <div class="fnw-h1-sub">设置输入参数与返回值类型,系统将自动关联对应类与数据模型</div>

                <!-- 输入参数 -->
                <div class="fnw-card">
                  <div class="fnw-card-hd">
                    <span class="fnw-card-title">输入参数</span>
                    <button class="bl-btn bl-btn-text fnw-addp" @click="addParam">
                      <span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">新增参数</span>
                    </button>
                  </div>
                  <table class="bl-table fnw-ptable">
                    <colgroup><col style="width:210px" /><col style="width:200px" /><col /><col style="width:60px" /></colgroup>
                    <thead>
                      <tr>
                        <th class="t-left">参数名称</th>
                        <th class="t-left">参数类型</th>
                        <th class="t-left">参数说明</th>
                        <th class="t-center">操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(p, i) in params" :key="i">
                        <td class="t-left"><input class="bl-input bl-mono" v-model="p.param_name" placeholder="小驼峰,如 station" /></td>
                        <td class="t-left">
                          <button class="fnw-typelink" :class="{ 'is-empty': !p.param_type }" @click="openTypePicker('param', i)">
                            {{ p.param_type || '选择类型' }}
                          </button>
                        </td>
                        <td class="t-left"><input class="bl-input" v-model="p.param_desc" placeholder="描述业务含义" /></td>
                        <td class="t-center">
                          <button class="bl-btn bl-btn-sm bl-btn-text fnw-del" title="删除参数" @click="removeParam(i)"
                                  v-html="BL.icon('trash', 12)"></button>
                        </td>
                      </tr>
                      <tr v-if="!params.length">
                        <td colspan="4" class="t-center bl-muted" style="height:60px">暂无输入参数,点击右上角「新增参数」添加</td>
                      </tr>
                    </tbody>
                  </table>
                  <div v-if="hasObjectParam" class="fnw-tip">
                    <span v-html="BL.icon('info', 12)"></span>
                    已选择本体对象类型,系统将自动关联对应的实体类、属性模型与关联关系,生成类型定义
                  </div>
                </div>

                <!-- 返回值 -->
                <div class="fnw-card">
                  <div class="fnw-card-hd"><span class="fnw-card-title">返回值</span></div>
                  <table class="bl-table fnw-ptable">
                    <colgroup><col style="width:210px" /><col /></colgroup>
                    <thead>
                      <tr><th class="t-left">返回类型</th><th class="t-left">返回说明</th></tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td class="t-left">
                          <button class="fnw-typelink" :class="{ 'is-empty': !ret.param_type }" @click="openTypePicker('ret', -1)">
                            {{ ret.param_type || '选择类型' }}
                          </button>
                        </td>
                        <td class="t-left"><input class="bl-input" v-model="ret.param_desc" placeholder="描述返回值业务含义" /></td>
                      </tr>
                    </tbody>
                  </table>
                  <div v-if="isObjectType(ret.param_type)" class="fnw-tip">
                    <span v-html="BL.icon('info', 12)"></span>
                    返回值对象将自动生成对应的数据模型类,包含完整的字段类型定义
                  </div>
                </div>
              </template>

            </div>
          </div>

          <!-- ===== 底部操作栏 ===== -->
          <div class="fnw-ft">
            <span v-if="errMsg" class="fnw-err"><span v-html="BL.icon('warning', 12)"></span>{{ errMsg }}</span>
            <span class="bl-grow"></span>
            <button class="bl-btn" @click="onCancel">取消</button>
            <button v-if="step > 1" class="bl-btn" @click="step--">上一步</button>
            <button v-if="step < 3" class="bl-btn bl-btn-primary" @click="next">下一步</button>
            <button v-else class="bl-btn bl-btn-primary" :disabled="saving" @click="submit">
              {{ saving ? '创建中…' : '创建' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 子弹窗 -->
    <TypePickerModal v-model:open="typePickerOpen" :value="typePickerValue"
                     :title="typePickerTarget === 'ret' ? '选择返回值类型' : '选择参数类型'"
                     @confirm="onTypePicked" />
    <DirPickerModal v-model:open="dirPickerOpen" :industry="form.industry_dir" :category="form.category_dir"
                    @confirm="onDirPicked" />
    <FilePickerModal v-model:open="filePickerOpen" :industry="form.industry_dir" :category="form.category_dir"
                     :ext="ext" @confirm="onFilePicked" />
  </Teleport>
</template>

<script setup>
/**
 * 新增函数向导 (P2 · 文档 5.2)
 *
 * 三步式:选择开发语言 → 配置基础信息 → 定义函数签名。
 * 创建成功后生成初始版本 v0.0.1 草稿 + 模板代码, 并向父组件抛出新函数 id。
 *
 * 边界规则实现情况:
 * - 语言不可变更:进入第 2 步后回到第 1 步语言卡片锁定 (只读展示 + 提示)
 * - 目录自动创建:行业 / 领域用 ComboInput, 不存在的值创建时由后端补版本库记录
 * - 文件自动创建:文件名不存在即新建, 已存在则追加到该文件
 * - 同名函数校验:失焦/输入防抖调 check-api-name, 重名时阻止创建
 * - 取消无草稿:确认后清空全部表单
 */
import { ref, reactive, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { functionApi, versionRepoApi } from '@/api'
import BlSelect from '@/components/BlSelect.vue'
import ComboInput from './ComboInput.vue'
import TypePickerModal from './TypePickerModal.vue'
import DirPickerModal from './DirPickerModal.vue'
import FilePickerModal from './FilePickerModal.vue'
import { buildTemplateCode, extOf, toClassName, isObjectType } from './codeTemplate.js'

const props = defineProps({
  open: { type: Boolean, default: false },
  /** 打开时预填的行业 / 领域 (来自左侧分组树的当前选中) */
  initIndustry: { type: String, default: '' },
  initCategory: { type: String, default: '' }
})
const emit = defineEmits(['update:open', 'created'])

const STEPS = ['选择语言', '基础信息', '定义签名']
const LANGS = [
  { v: 1, label: 'Python 函数', badge: 'Py', color: '#3776AB',
    desc: '编写可复用的 Python 逻辑,可作为管道计算的自定义函数,或在前端应用中作为服务函数调用。' },
  { v: 2, label: 'TypeScript 函数', badge: 'TS', color: '#3178C6',
    desc: '编写 TypeScript 逻辑,用于前端应用中分析或编辑本体数据,支持外部应用调用与服务编排。' },
]
const COMPARE = [
  { k: '主要用途', py: '后端服务逻辑、数据计算管道、批量数据处理', ts: '前端本体操作、前端交互逻辑、页面级编排' },
  { k: '核心能力', py: '复杂数值计算、批量数据清洗、第三方库集成、服务化接口封装、大数据量处理', ts: '本体数据读写编辑、前端事件响应、表单校验逻辑、轻量业务计算、前端组件联动' },
  { k: '典型适用场景', py: '数据管道自定义计算节点、后端服务函数调用、批量本体数据处理、算法逻辑封装', ts: '前端应用本体数据编辑、表单校验与交互逻辑、前端页面服务编排、本体实例实时计算' },
  { k: '执行环境', py: '服务端运行时,独立资源隔离', ts: '浏览器端运行时,随前端页面加载' },
  { k: '调用方式', py: '管道节点调用、前端服务调用、外部 API 调用、定时任务触发', ts: '前端页面调用、前端编排器调用、组件事件触发、前端流程节点' },
  { k: '本体数据操作', py: '读取本体数据,服务端计算处理,结果回写本体', ts: '直接读写前端本体实例,实时编辑修改本体属性与关联' },
]
/* 函数类型: 文档 3.2.3 列了常规/动作/聚合/衍生 4 种, 这里补上枚举表中的时序函数, 与列表页筛选口径一致 */
const TYPE_OPTS = [
  { value: '1', label: '常规函数' },
  { value: '2', label: '动作函数' },
  { value: '3', label: '聚合函数' },
  { value: '4', label: '衍生函数' },
  { value: '5', label: '时序函数' },
]

const step = ref(1)
const langLocked = ref(false)
const saving = ref(false)
const errMsg = ref('')

const form = reactive({
  language: 2,
  industry_dir: '',
  category_dir: '',
  api_name: '',
  function_label: '',
  function_type: 1,
  rdfs_comment: ''
})
const fileBase = ref('')
const params = ref([])
const ret = reactive({ param_type: '', param_desc: '', object_class_id: null })

/* BlSelect 的值是字符串, 与数值型 function_type 做双向桥接 */
const functionTypeStr = computed({
  get: () => String(form.function_type),
  set: (v) => { form.function_type = Number(v) || 1 }
})

const ext = computed(() => extOf(form.language))
const dirPath = computed(() => form.industry_dir ? `/${form.industry_dir}${form.category_dir ? '/' + form.category_dir : ''}` : '')
const derivedClassName = computed(() => toClassName(fileBase.value))
const codeFilePath = computed(() => fileBase.value ? `${fileBase.value.replace(/\.(ts|py)$/i, '')}.${ext.value}` : '')
const fullAccessPath = computed(() => {
  if (!form.industry_dir || !form.category_dir || !form.api_name) return ''
  const cls = derivedClassName.value
  return `/${form.industry_dir}/${form.category_dir}${cls ? '/' + cls : ''}/${form.api_name}`
})
const hasObjectParam = computed(() => params.value.some(p => isObjectType(p.param_type)))

/* —— 目录选项 —— */
const dirOptions = ref([])
async function loadDirOptions() {
  const list = await versionRepoApi.dirOptions().catch(() => [])
  dirOptions.value = Array.isArray(list) ? list : []
}
const industryOptions = computed(() => dirOptions.value.map(d => d.industry_dir))
const categoryOptions = computed(() => {
  const hit = dirOptions.value.find(d => d.industry_dir === form.industry_dir)
  return hit ? (hit.categories || []) : []
})

/* —— 打开 / 重置 —— */
watch(() => props.open, (v) => {
  if (!v) return
  reset()
  loadDirOptions()
})
function reset() {
  step.value = 1
  langLocked.value = false
  saving.value = false
  errMsg.value = ''
  form.language = 2
  form.industry_dir = props.initIndustry || ''
  form.category_dir = props.initCategory || ''
  form.api_name = ''
  form.function_label = ''
  form.function_type = 1
  form.rdfs_comment = ''
  fileBase.value = ''
  params.value = []
  ret.param_type = ''
  ret.param_desc = ''
  ret.object_class_id = null
  check.value = { name_valid: true, duplicated: false, file_exists: false, message: '' }
}

function pickLang(v) {
  if (langLocked.value) {
    if (form.language !== v) BL.warning('开发语言已锁定,如需更换请取消创建后重新开始')
    return
  }
  form.language = v
}

/* —— 实时校验 (文件重名 / API 重名, 300ms 防抖) —— */
const check = ref({ name_valid: true, duplicated: false, file_exists: false, message: '' })
let checkTimer = null
watch([codeFilePath, () => form.api_name], () => {
  clearTimeout(checkTimer)
  if (!codeFilePath.value) {
    check.value = { name_valid: true, duplicated: false, file_exists: false, message: '' }
    return
  }
  checkTimer = setTimeout(async () => {
    const r = await functionApi.checkApiName(codeFilePath.value, form.api_name || '').catch(() => null)
    if (r) check.value = r
  }, 300)
})
const fileHint = computed(() => {
  if (!codeFilePath.value) return '支持直接输入文件名,也可点击「选择」挑选已有文件'
  return check.value.file_exists
    ? '该文件已存在,函数将添加到该文件中'
    : '输入名称不存在,将自动创建新文件'
})
const apiHint = computed(() => {
  if (!form.api_name) return '小驼峰命名,同文件内唯一'
  if (!check.value.name_valid) return check.value.message || 'API 名称需为小驼峰'
  if (check.value.duplicated) return '该文件内已存在同名 API'
  return '命名可用'
})
const apiHintCls = computed(() => {
  if (!form.api_name) return 'is-muted'
  if (!check.value.name_valid || check.value.duplicated) return 'is-err'
  return 'is-ok'
})

/* —— 步骤流转与校验 —— */
function next() {
  errMsg.value = ''
  if (step.value === 1) {
    langLocked.value = true       // 进入第 2 步后语言锁定
    step.value = 2
    return
  }
  if (step.value === 2) {
    if (!form.industry_dir.trim() || !form.category_dir.trim()) return fail('请填写行业目录与领域目录')
    if (!fileBase.value.trim()) return fail('请填写文件名')
    if (!form.api_name.trim()) return fail('请填写 API 名称')
    if (!/^[a-z][a-zA-Z0-9]*$/.test(form.api_name.trim())) return fail('API 名称需为小驼峰: 首字母小写, 只含字母与数字')
    if (check.value.duplicated) return fail('该文件内已存在同名 API,请更换 API 名称')
    step.value = 3
  }
}
function fail(msg) { errMsg.value = msg; BL.warning(msg) }

/* —— 参数表 —— */
function addParam() { params.value.push({ param_name: '', param_type: '', param_desc: '', object_class_id: null }) }
function removeParam(i) { params.value.splice(i, 1) }

/* —— 类型选择弹窗 —— */
const typePickerOpen = ref(false)
const typePickerTarget = ref('param')
const typePickerIndex = ref(-1)
const typePickerValue = ref('')
function openTypePicker(target, i) {
  typePickerTarget.value = target
  typePickerIndex.value = i
  typePickerValue.value = target === 'ret' ? ret.param_type : (params.value[i]?.param_type || '')
  typePickerOpen.value = true
}
function onTypePicked(p) {
  if (typePickerTarget.value === 'ret') {
    ret.param_type = p.param_type
    ret.object_class_id = p.object_class_id
  } else {
    const row = params.value[typePickerIndex.value]
    if (row) { row.param_type = p.param_type; row.object_class_id = p.object_class_id }
  }
}

/* —— 目录 / 文件弹窗 —— */
const dirPickerOpen = ref(false)
const filePickerOpen = ref(false)
function onDirPicked(p) { form.industry_dir = p.industry; form.category_dir = p.category }
function onFilePicked(path) { fileBase.value = String(path).replace(/\.(ts|py)$/i, '') }

/* —— 提交 —— */
async function submit() {
  errMsg.value = ''
  if (params.value.some(p => p.param_name.trim() && !p.param_type))
    return fail('请为每个输入参数选择参数类型')
  if (params.value.some(p => !p.param_name.trim() && p.param_type))
    return fail('请填写参数名称')

  const cleanParams = params.value
    .filter(p => p.param_name.trim())
    .map((p, i) => ({
      param_name: p.param_name.trim(),
      param_type: p.param_type,
      param_desc: p.param_desc,
      object_class_id: p.object_class_id,
      param_direction: 1,
      is_required: 1,
      sort_num: i + 1
    }))
  const meta = {
    api_name: form.api_name.trim(),
    function_label: form.function_label.trim(),
    function_type: form.function_type,
    class_name: derivedClassName.value,
    rdfs_comment: form.rdfs_comment.trim(),
    version_no: 'v0.0.1'
  }
  const payload = {
    ...meta,
    language: form.language,
    industry_dir: form.industry_dir.trim(),
    category_dir: form.category_dir.trim(),
    code_file_path: codeFilePath.value,
    code_content: buildTemplateCode(form.language, meta, cleanParams, ret.param_type ? ret : null),
    status: 1,          // 1 草稿
    visibility: 1,      // 1 全平台可见
    params: ret.param_type
      ? [...cleanParams, { param_name: 'result', param_type: ret.param_type, param_desc: ret.param_desc,
                           object_class_id: ret.object_class_id, param_direction: 2, is_required: 0, sort_num: 1 }]
      : cleanParams
  }

  saving.value = true
  try {
    const created = await functionApi.create(payload)
    BL.success(`函数「${payload.api_name}」已创建 · v0.0.1 草稿`)
    emit('created', created?.id || null)
    emit('update:open', false)
  } catch (e) {
    fail(e?.message || '创建失败')
  } finally {
    saving.value = false
  }
}

async function onCancel() {
  const dirty = form.api_name || fileBase.value || form.function_label || params.value.length || form.rdfs_comment
  if (dirty) {
    const ok = await BL.confirm({ title: '取消创建', content: '取消后不保留任何已填写内容,确定退出向导?', okText: '确定退出' })
    if (!ok) return
  }
  emit('update:open', false)
}
</script>

<style scoped>
.fnw-mask {
  position: fixed; inset: 0; z-index: 1200;
  background: rgba(0, 0, 0, .45);
  display: flex; align-items: center; justify-content: center;
}
.fnw-modal {
  width: 1024px; max-width: calc(100vw - 48px); height: 82vh;
  background: var(--bl-bg-2);
  border-radius: 8px; border: 1px solid var(--bl-border);
  box-shadow: 0 12px 40px rgba(0, 0, 0, .2);
  display: flex; flex-direction: column; overflow: hidden;
}

/* 步骤条 */
.fnw-steps {
  position: relative;
  display: flex; align-items: center; justify-content: center; gap: 0;
  padding: 16px 20px; background: var(--bl-bg-1);
  border-bottom: 1px solid var(--bl-divider);
}
.fnw-step { display: inline-flex; align-items: center; gap: 8px; }
.fnw-step-num {
  width: 22px; height: 22px; border-radius: 50%;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 12px; border: 1px solid var(--bl-border); color: var(--bl-text-3); background: var(--bl-bg-1);
}
.fnw-step-lbl { font-size: 13px; color: var(--bl-text-3); }
.fnw-step.is-cur .fnw-step-num { background: var(--bl-primary); border-color: var(--bl-primary); color: #fff; }
.fnw-step.is-cur .fnw-step-lbl { color: var(--bl-text-1); font-weight: 600; }
.fnw-step.is-done .fnw-step-num { border-color: var(--bl-primary); color: var(--bl-primary); }
.fnw-step.is-done .fnw-step-lbl { color: var(--bl-primary); }
.fnw-step-line { width: 96px; height: 1px; background: var(--bl-border); margin: 0 12px; }
.fnw-step-line.is-done { background: var(--bl-primary); }
.fnw-x { position: absolute; right: 12px; top: 12px; }

/* 内容区 */
.fnw-body { flex: 1; min-height: 0; overflow: auto; }
.fnw-inner { max-width: 940px; margin: 0 auto; padding: 20px 24px 24px; }
.fnw-h1 { margin: 0 0 4px; font-size: 20px; font-weight: 600; color: var(--bl-text-1); }
.fnw-h1-sub { font-size: 13px; color: var(--bl-text-3); margin-bottom: 16px; }

/* 步骤1: 语言卡片 */
.fnw-locked {
  display: flex; align-items: center; gap: 6px; margin-bottom: 12px;
  padding: 8px 10px; border-radius: 4px; font-size: 12px;
  background: var(--bl-bg-2); color: var(--bl-text-2);
}
.fnw-lang { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; margin-bottom: 20px; }
.fnw-lang-card {
  padding: 16px; border-radius: 8px; border: 1px solid var(--bl-border);
  background: var(--bl-bg-1); cursor: pointer; transition: border-color .15s, background-color .15s;
}
.fnw-lang-card:hover { border-color: var(--bl-primary); }
.fnw-lang-card.is-on { border: 2px solid var(--bl-primary); background: var(--bl-primary-soft); padding: 15px; }
.fnw-lang-card.is-dim { opacity: .5; }
.fnw-lang-hd { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.fnw-lang-ic {
  width: 26px; height: 26px; border-radius: 5px; color: #fff;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700; font-family: Consolas, Monaco, monospace;
}
.fnw-lang-name { font-size: 15px; font-weight: 600; color: var(--bl-text-1); }
.fnw-lang-desc { font-size: 12.5px; color: var(--bl-text-3); line-height: 19px; }

.fnw-cmp { width: 100%; background: var(--bl-bg-1); border-radius: 6px; overflow: hidden; }
.fnw-cmp thead th {
  background: var(--bl-thead-bg); font-size: 12px; font-weight: 600;
  height: 36px; padding: 0 12px; color: var(--bl-text-1); text-align: left;
}
.fnw-cmp td { padding: 10px 12px; font-size: 12px; color: var(--bl-text-2); vertical-align: top; line-height: 18px; }
.fnw-cmp-k { color: var(--bl-text-1); font-weight: 500; }
.fnw-cmp td.is-hl { background: var(--bl-primary-soft); color: var(--bl-text-1); }

/* 步骤2/3: 白卡片 */
.fnw-card {
  background: var(--bl-bg-1); border: 1px solid var(--bl-border);
  border-radius: 8px; padding: 20px; margin-bottom: 12px;
}
.fnw-card-hd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.fnw-card-title { font-size: 13px; font-weight: 600; color: var(--bl-text-1); }
.fnw-addp { color: var(--bl-primary); }

.fnw-row { display: flex; align-items: center; gap: 8px; margin-bottom: 20px; }
.fnw-row:last-child { margin-bottom: 0; }
.fnw-row-2 { gap: 24px; }
.fnw-row-ta { align-items: flex-start; }
.fnw-col { flex: 1; display: flex; align-items: center; gap: 8px; min-width: 0; }
.fnw-lbl { flex-shrink: 0; width: 72px; text-align: right; font-size: 13px; color: var(--bl-text-2); }
.fnw-req { color: #f53f3f; font-style: normal; margin-right: 2px; }
.fnw-col .bl-input { flex: 1; min-width: 0; }

.fnw-dir { flex: 1; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
.fnw-dir-path { display: flex; gap: 6px; min-width: 0; }
.fnw-dir-path .bl-input { flex: 1; min-width: 0; }
.fnw-pick-btn { flex-shrink: 0; display: inline-flex; align-items: center; }

.fnw-file { flex: 1; display: flex; gap: 6px; min-width: 0; }
.fnw-file-grp {
  flex: 1; min-width: 0; display: flex; align-items: center;
  border: 1px solid var(--bl-border); border-radius: 4px; background: var(--bl-bg-1); overflow: hidden;
}
.fnw-file-input { flex: 1; min-width: 0; border: 0 !important; text-align: right; background: transparent; }
.fnw-file-ext {
  flex-shrink: 0; padding: 0 8px; font-size: 12px; color: var(--bl-text-3);
  border-left: 1px solid var(--bl-divider); line-height: 30px; background: var(--bl-bg-2);
}
.fnw-hintrow { margin-top: -14px; margin-bottom: 16px; }
.fnw-hint { padding-left: 80px; font-size: 11.5px; }
.fnw-hint.is-muted { color: var(--bl-text-3); }
.fnw-hint.is-info { color: var(--bl-primary); }
.fnw-hint.is-ok { color: var(--bl-success); }
.fnw-hint.is-err { color: #f53f3f; }

.fnw-ta { flex: 1; resize: vertical; min-height: 68px; padding: 6px 10px; line-height: 18px; }
.fnw-derived {
  display: flex; align-items: center; flex-wrap: wrap; gap: 4px;
  margin-top: 18px; padding: 8px 10px; border-radius: 4px;
  background: var(--bl-bg-2); color: var(--bl-text-3); font-size: 11.5px;
}
.fnw-derived b { color: var(--bl-text-1); font-weight: 600; }

/* 参数表 */
.fnw-ptable { width: 100%; }
.fnw-ptable thead th {
  background: var(--bl-thead-bg); font-size: 12px; font-weight: 600;
  height: 34px; padding: 0 8px; color: var(--bl-text-2); text-align: left;
}
.fnw-ptable thead th.t-center { text-align: center; }
.fnw-ptable td { padding: 6px 8px; font-size: 12px; }
.fnw-ptable td.t-center { text-align: center; }
.fnw-ptable .bl-input { width: 100%; height: 30px; }
.fnw-typelink {
  border: 0; background: transparent; cursor: pointer; padding: 0;
  color: var(--bl-primary); font-size: 12.5px;
  font-family: Consolas, Monaco, monospace; text-align: left;
}
.fnw-typelink:hover { text-decoration: underline; }
.fnw-typelink.is-empty { color: var(--bl-text-3); font-style: italic; }
.fnw-del { color: #f53f3f; }
.fnw-tip {
  display: flex; align-items: center; gap: 6px;
  margin-top: 10px; padding: 8px 10px; border-radius: 4px;
  background: var(--bl-primary-soft); color: var(--bl-primary); font-size: 12px;
}

/* 底部操作栏 */
.fnw-ft {
  flex-shrink: 0; display: flex; align-items: center; gap: 8px;
  padding: 12px 20px; background: var(--bl-bg-1); border-top: 1px solid var(--bl-border);
}
.fnw-err { display: inline-flex; align-items: center; gap: 4px; color: #f53f3f; font-size: 12px; }
.bl-grow { flex: 1; }
.fnw-fade-enter-active, .fnw-fade-leave-active { transition: opacity .15s; }
.fnw-fade-enter-from, .fnw-fade-leave-to { opacity: 0; }
</style>
