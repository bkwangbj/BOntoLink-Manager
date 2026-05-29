<template>
  <Teleport to="body">
    <transition name="nsp-fade">
      <div v-if="open" class="nsp-mask" @click.self="close">
        <div class="nsp-modal">
          <!-- 标题栏 -->
          <header class="nsp-hd">
            <div>
              <div class="nsp-title">新建共享属性</div>
              <div class="bl-muted" style="font-size:12px;margin-top:2px">3 步完成 · 元数据 → 配置 → 保存位置</div>
            </div>
            <button class="bl-btn bl-btn-text bl-btn-icon" @click="close" v-html="BL.icon('x', 14)"></button>
          </header>

          <!-- 步骤条 -->
          <div class="nsp-steps">
            <div v-for="(s, i) in steps" :key="s.k" :class="['nsp-step', step === i && 'is-on', step > i && 'is-done']">
              <span class="nsp-step-num">{{ step > i ? '✓' : i + 1 }}</span>
              <span class="nsp-step-label">{{ s.label }}</span>
              <span v-if="i < steps.length - 1" class="nsp-step-bar"></span>
            </div>
          </div>

          <!-- 内容 -->
          <div class="nsp-body">
            <!-- 步骤1: 元数据 -->
            <section v-if="step === 0" class="nsp-section">
              <FieldRow label="属性编码 *" hint="英文小写+下划线,创建后不可修改">
                <input class="bl-input bl-mono" v-model="form.prop_code" placeholder="如: start_time" @input="autoApiName" />
                <div v-if="codeError" class="nsp-err">{{ codeError }}</div>
              </FieldRow>
              <FieldRow label="名称 *">
                <input class="bl-input" v-model="form.rdfs_label" placeholder="中文名,如: 开始时间" />
              </FieldRow>
              <FieldRow label="属性类型">
                <div class="nsp-type-row">
                  <label v-for="t in propTypes" :key="t.k" :class="['nsp-type-card', form.prop_type === t.k && 'is-on']">
                    <input type="radio" v-model="form.prop_type" :value="t.k" style="display:none" />
                    <span class="nsp-type-ic" :style="{ background: t.color }" v-html="BL.icon(t.icon, 14, '#fff')"></span>
                    <div>
                      <div style="font-weight:500">{{ t.label }}</div>
                      <div class="bl-muted" style="font-size:11px">{{ t.desc }}</div>
                    </div>
                  </label>
                </div>
              </FieldRow>
              <FieldRow label="RDFS 注释">
                <textarea class="bl-textarea nsp-ta" rows="6" v-model="form.rdfs_comment" placeholder="业务规则说明 / 字段含义 / 取值范围 / 来源依据..."></textarea>
              </FieldRow>
            </section>

            <!-- 步骤2: 配置 -->
            <section v-else-if="step === 1" class="nsp-section">
              <FieldRow label="数据类型">
                <select class="bl-input" v-model="form.data_type">
                  <option value="">—</option>
                  <option v-for="t in xsdTypes" :key="t" :value="t">{{ t }}</option>
                </select>
              </FieldRow>
              <FieldRow label="值类型">
                <div class="bl-row" style="gap:6px;align-items:center">
                  <span :class="['nsp-vt-pill', form.value_type && 'is-on']">
                    <span v-html="BL.icon('tag', 11)"></span>
                    <span style="margin-left:4px">{{ form.value_type_label || '未设置' }}</span>
                  </span>
                  <button class="bl-btn bl-btn-sm" @click="vtpOpen = true">
                    <span v-html="BL.icon('search', 11)"></span>
                    <span style="margin-left:4px">选择</span>
                  </button>
                </div>
              </FieldRow>
              <FieldRow label="是否必填" inline>
                <label class="bl-row" style="gap:6px"><input type="checkbox" v-model="form.is_required" :true-value="1" :false-value="0" /> 必填</label>
              </FieldRow>
              <FieldRow label="是否多值" inline>
                <label class="bl-row" style="gap:6px"><input type="checkbox" v-model="form.is_multi_valued_prop" :true-value="1" :false-value="0" /> 多值</label>
              </FieldRow>
              <FieldRow label="值域约束" inline>
                <label class="bl-row" style="gap:6px"><input type="checkbox" v-model="form.is_range_constraint_prop" :true-value="1" :false-value="0" /> 启用 XSD 约束</label>
              </FieldRow>
              <template v-if="form.prop_type === 'data' && form.is_range_constraint_prop">
                <div class="bl-muted" style="font-size:12px;margin-top:4px">XSD 约束(可选)</div>
                <div class="nsp-xsd-grid">
                  <input type="number" class="bl-input" v-model.number="form.xsd_min_length" placeholder="最小长度" />
                  <input type="number" class="bl-input" v-model.number="form.xsd_max_length" placeholder="最大长度" />
                  <input class="bl-input bl-mono" v-model="form.xsd_pattern" placeholder="正则" style="grid-column:1/3" />
                  <input class="bl-input" v-model="form.xsd_min_inclusive" placeholder="数值最小" />
                  <input class="bl-input" v-model="form.xsd_max_inclusive" placeholder="数值最大" />
                </div>
              </template>
            </section>

            <!-- 步骤3: 保存位置 -->
            <section v-else-if="step === 2" class="nsp-section">
              <FieldRow label="所属领域">
                <select class="bl-input" v-model="form.category_code">
                  <option value="">— 通用 (不属于任何领域) —</option>
                  <option v-for="c in flatCategories" :key="c.category_code" :value="c.category_code">{{ c.indent }}{{ c.label }}</option>
                </select>
              </FieldRow>
              <FieldRow label="所属分组">
                <select class="bl-input" v-model="form.group_id">
                  <option value="">— 不分组 —</option>
                  <option v-for="g in groups" :key="g.id" :value="g.id">{{ groupLabel(g) }}</option>
                </select>
                <div class="bl-muted" style="font-size:11px;margin-top:4px">分组用于左侧导航树筛选。可在「共享属性」页面左侧"行业领域分组"上方点击 + 新建分组。</div>
              </FieldRow>

              <!-- 概要 -->
              <div class="nsp-section-sub" style="margin-top:14px">配置概要</div>
              <table class="nsp-summary">
                <tr><td>属性编码</td><td><span class="bl-mono">{{ form.prop_code || '—' }}</span></td></tr>
                <tr><td>名称</td><td>{{ form.rdfs_label || '—' }}</td></tr>
                <tr><td>属性类型</td><td><span class="bl-tag">{{ propTypeLabel(form.prop_type) }}</span></td></tr>
                <tr><td>数据类型</td><td><span class="bl-tag">{{ form.data_type || '—' }}</span></td></tr>
                <tr><td>值类型</td><td>{{ form.value_type_label || form.value_type || '—' }}</td></tr>
                <tr><td>所属领域</td><td>{{ categoryLabel(form.category_code) || '通用' }}</td></tr>
              </table>
            </section>
          </div>

          <!-- 底部 -->
          <footer class="nsp-ft">
            <button class="bl-btn" @click="close">取消</button>
            <span style="flex:1"></span>
            <button v-if="step > 0" class="bl-btn" @click="step--">上一步</button>
            <button v-if="step < steps.length - 1" class="bl-btn bl-btn-primary" :disabled="!canNext" @click="onNext">下一步</button>
            <button v-else class="bl-btn bl-btn-primary" :disabled="submitting" @click="onSubmit">{{ submitting ? '创建中...' : '完成' }}</button>
          </footer>
        </div>
      </div>
    </transition>

    <!-- 值类型选择面板 -->
    <ValueTypePickerModal v-model:open="vtpOpen"
                          v-model="form.value_type"
                          :multi="false"
                          subtitle="为新建共享属性选择值类型"
                          @confirm="onValueTypeConfirm" />
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { sharedPropertyApi, categoryApi, groupApi, groupRefApi } from '@/api'
import FieldRow from '@/views/config/category/FieldRow.vue'
import ValueTypePickerModal from '@/components/ValueTypePickerModal.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  presetGroupId: { type: String, default: '' }
})
const emit = defineEmits(['update:open', 'created'])

const steps = [
  { k: 'meta',  label: '元数据' },
  { k: 'cfg',   label: '配置' },
  { k: 'where', label: '保存位置' }
]
const step = ref(0)
const submitting = ref(false)

const propTypes = [
  { k: 'data',       label: '数据属性',   desc: '关联物理字段/数据类型', icon: 'database', color: '#1677ff' },
  { k: 'object',     label: '对象属性',   desc: '关联另一对象类',         icon: 'link',     color: '#FF7D00' },
  { k: 'annotation', label: '注释属性',   desc: '仅用于标签说明',         icon: 'chat',     color: '#00B42A' }
]
const xsdTypes = ['xsd:string','xsd:decimal','xsd:integer','xsd:boolean','xsd:dateTime','xsd:date','xsd:anyURI']

const defaultForm = () => ({
  prop_code: '', rdfs_label: '', rdfs_comment: '',
  prop_type: 'data', data_type: 'xsd:string',
  value_type: '', value_type_label: '',
  is_required: 0, is_multi_valued_prop: 0, is_range_constraint_prop: 0,
  xsd_min_length: null, xsd_max_length: null, xsd_pattern: '', xsd_min_inclusive: '', xsd_max_inclusive: '',
  category_code: '', group_id: ''
})
const form = reactive(defaultForm())
const codeError = ref('')

watch(() => props.open, (v) => {
  if (v) {
    Object.assign(form, defaultForm())
    form.group_id = props.presetGroupId || ''
    step.value = 0
    codeError.value = ''
    loadAux()
  }
})

/* —— 编码校验 —— */
function autoApiName() {
  // 编码即 api_name 的形式: 小写 + 下划线
  form.prop_code = (form.prop_code || '').toLowerCase().replace(/[^a-z0-9_]/g, '_')
  codeError.value = ''
}
const canNext = computed(() => {
  if (step.value === 0) return form.prop_code && form.rdfs_label
  if (step.value === 1) return true
  return true
})
function onNext() {
  if (step.value === 0) {
    if (!/^[a-z][a-z0-9_]*$/.test(form.prop_code)) {
      codeError.value = '编码须以小写字母开头,只能包含小写字母/数字/下划线'
      return
    }
  }
  step.value++
}

/* —— 加载辅助数据 —— */
const categories = ref([])
const groups = ref([])
const groupRefs = ref([])
const flatCategories = computed(() => {
  const out = []
  const walk = (ns, depth) => ns.forEach(n => {
    if (n.categoryCode) out.push({ category_code: n.categoryCode, label: n.label, indent: '　'.repeat(depth) })
    if (n.children) walk(n.children, depth + 1)
  })
  walk(categories.value, 0)
  return out
})
async function loadAux() {
  try {
    if (!categories.value.length) categories.value = await categoryApi.tree().catch(() => [])
    if (!groups.value.length) {
      const all = await groupApi.listAll().catch(() => [])
      // 仅显示 type=shared_props 的分组: 通过 groupRefApi.list 拿出共享属性绑定的 group_id
      groupRefs.value = await groupRefApi.list('shared_props').catch(() => [])
      const usedGroupIds = new Set(groupRefs.value.map(r => r.groupId || r.group_id))
      groups.value = all.filter(g => usedGroupIds.has(g.id)
        // 还要包含 名称含"属性"且不带 category_code 的根分组 (按 data.sql 约定)
        || (!g.parentId && !g.categoryCode && /属性/.test(g.gName || g.gname || g.g_name || '')))
    }
  } catch {}
}

/* —— 值类型 —— */
const vtpOpen = ref(false)
function onValueTypeConfirm({ rows: picked }) {
  if (picked && picked[0]) {
    form.value_type_label = picked[0].rdfs_label || picked[0].api_name
    if (picked[0].data_type) form.data_type = picked[0].data_type
  }
}

/* —— 标签辅助 —— */
function propTypeLabel(t) { return ({ data: '数据属性', object: '对象属性', annotation: '注释属性' })[t] || '—' }
function categoryLabel(code) {
  if (!code) return ''
  return flatCategories.value.find(c => c.category_code === code)?.label || code
}
function groupLabel(g) {
  const name = g.gName || g.gname || g.g_name || '(未命名)'
  return name
}

/* —— 提交 —— */
async function onSubmit() {
  submitting.value = true
  try {
    const payload = { ...form }
    delete payload.value_type_label
    await sharedPropertyApi.create(payload)
    BL.success('创建成功')
    emit('created')
    close()
  } catch (e) {
    BL.error(e?.msg || '创建失败')
  } finally { submitting.value = false }
}
function close() { emit('update:open', false) }
</script>

<style scoped>
.nsp-mask {
  position: fixed; inset: 0; z-index: 1100;
  background: rgba(0,0,0,.40);
  display: flex; align-items: center; justify-content: center;
}
.nsp-modal {
  width: 720px; max-width: 95vw;
  max-height: calc(100vh - 80px);
  background: #fff; border-radius: 12px;
  display: flex; flex-direction: column;
  overflow: hidden;
  box-shadow: 0 12px 40px rgba(0,0,0,.20);
}

.nsp-hd { display: flex; justify-content: space-between; align-items: flex-start; padding: 14px 18px; border-bottom: 1px solid var(--bl-divider); }
.nsp-title { font-size: 16px; font-weight: 600; }

/* 步骤条 */
.nsp-steps { display: flex; padding: 14px 24px; background: #fafbfc; gap: 0; }
.nsp-step { display: inline-flex; align-items: center; gap: 8px; color: var(--bl-text-3); font-size: 12.5px; flex: 1; }
.nsp-step-num {
  width: 22px; height: 22px; border-radius: 50%;
  background: #c9cdd4; color: #fff;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 600; flex-shrink: 0;
}
.nsp-step.is-on { color: var(--bl-primary); }
.nsp-step.is-on .nsp-step-num { background: var(--bl-primary); }
.nsp-step.is-done .nsp-step-num { background: #00b42a; }
.nsp-step-bar { flex: 1; height: 1px; background: var(--bl-divider); margin: 0 8px; }
.nsp-step.is-done .nsp-step-bar { background: #00b42a; }

/* 内容 */
.nsp-body { flex: 1; overflow: auto; padding: 16px 22px; min-height: 320px; }
.nsp-section { display: flex; flex-direction: column; gap: 4px; }
.nsp-section-sub { font-size: 13px; font-weight: 500; color: var(--bl-text-2); margin: 8px 0 4px; padding-bottom: 4px; border-bottom: 1px dashed var(--bl-divider); }

.nsp-err { color: #f53f3f; font-size: 11px; margin-top: 4px; }

.nsp-type-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.nsp-type-card {
  display: flex; align-items: center; gap: 8px;
  padding: 10px; border: 1px solid var(--bl-border); border-radius: 6px;
  cursor: pointer; background: #fff;
  transition: border-color .12s, box-shadow .12s;
}
.nsp-type-card:hover { border-color: var(--bl-primary); }
.nsp-type-card.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); box-shadow: 0 0 0 2px rgba(22,93,255,.12); }
.nsp-type-ic { width: 24px; height: 24px; border-radius: 5px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }

.nsp-vt-pill {
  display: inline-flex; align-items: center;
  padding: 2px 8px; border-radius: 9px;
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-2); border: 1px dashed var(--bl-border);
}
.nsp-vt-pill.is-on {
  color: var(--bl-primary); background: var(--bl-primary-soft);
  border-style: solid; border-color: var(--bl-primary); font-weight: 500;
}

.nsp-xsd-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 6px; }

.nsp-summary { width: 100%; border-collapse: collapse; font-size: 13px; }
.nsp-summary td { padding: 6px 8px; border-bottom: 1px dashed var(--bl-divider); }
.nsp-summary td:first-child { color: var(--bl-text-3); width: 90px; }

.nsp-ft { padding: 12px 16px; border-top: 1px solid var(--bl-divider); display: flex; gap: 8px; align-items: center; background: #fafbfc; }

/* 文本域 (与抽屉一致, 高度更大 + 可拖高) */
.bl-textarea {
  width: 100%;
  min-height: 96px;
  padding: 8px 10px;
  border: 1px solid var(--bl-border);
  border-radius: 4px;
  background: #fff;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.55;
  resize: vertical;
  color: var(--bl-text-1);
  transition: border-color .15s;
}
.bl-textarea:focus { outline: none; border-color: var(--bl-primary); }
/* 向导内 RDFS 注释 默认高度更大,引导用户写完整说明 */
.nsp-ta { min-height: 140px; }
</style>
