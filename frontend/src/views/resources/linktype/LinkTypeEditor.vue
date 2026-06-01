<template>
  <Teleport to="body">
    <transition name="lke-drawer">
      <aside v-if="open" class="lke-drawer" :style="{ width: width + 'px' }">
        <div class="lke-handle" @mousedown="onHandleDown" :class="resizing && 'is-resizing'"></div>

        <!-- 头部 (对齐 SharedPropertyDetailDrawer 视觉) -->
        <header class="lke-hd">
          <div class="lke-hd-l">
            <!-- 大头像图标 (按基数染色) -->
            <span class="lke-ic lke-ic-lg" :style="{ background: cardColor }"
                  v-html="BL.icon('link', 18, '#fff')"></span>
            <div class="lke-title-wrap">
              <div class="lke-title bl-truncate" :title="form.rdfs_label || form.link_type_id || '新建链接'">
                {{ form.rdfs_label || form.link_type_id || '新建链接' }}
                <span v-if="form.link_type_id" class="bl-mono bl-muted lke-code">- {{ form.link_type_id }}</span>
              </div>
              <div class="lke-meta" v-if="form.id">
                <span :class="['bl-tag', statusTagCls(form.status)]">{{ statusLabel(form.status) }}</span>
                <span class="bl-muted" style="margin-left:6px">更新于 {{ shortTime(form.updated_at) }}</span>
              </div>
            </div>
          </div>
          <div class="lke-hd-r">
            <!-- 编辑模式 (图标 + 文字 胶囊按钮, 与共享属性抽屉一致) -->
            <button :class="['bl-btn bl-btn-text bl-btn-sm lke-edit-btn', editMode && 'is-edit-on']"
                    :title="editMode ? '关闭编辑模式 · 切回只读' : '开启编辑模式'"
                    @click="editMode = !editMode">
              <span v-html="BL.icon(editMode ? 'edit' : 'lock', 12)"></span>
              <span style="margin-left:4px">{{ editMode ? '编辑' : '只读' }}</span>
            </button>
            <span class="lke-divider"></span>
            <button class="bl-btn bl-btn-text bl-btn-icon" :title="isMax ? '还原' : '最大化'"
                    @click="toggleMax"
                    v-html="BL.icon(isMax ? 'minimize' : 'maximize', 13)"></button>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="onClose" v-html="BL.icon('x', 14)"></button>
          </div>
        </header>

        <!-- 页签 -->
        <nav class="lke-tabs">
          <button v-for="t in tabs" :key="t.k"
                  :class="['lke-tab', activeTab === t.k && 'is-on']" @click="activeTab = t.k">{{ t.label }}</button>
        </nav>

        <!-- 主体 -->
        <div class="lke-body" :class="{ 'is-readonly': !editMode }">

          <!-- 基础信息: 双向对称配置 -->
          <section v-if="activeTab === 'basic'">
            <!-- 全局元数据 (状态 + ID + RID) -->
            <div class="lke-meta-row">
              <div class="lke-meta-l">
                <span class="bl-muted" style="margin-right:6px">状态</span>
                <select class="bl-input lke-status-sel" v-model="form.status" :disabled="!editMode">
                  <option value="experimental">实验中</option>
                  <option value="active">正式</option>
                  <option value="deprecated">已废弃</option>
                </select>
              </div>
              <div class="lke-meta-r">
                <div class="lke-id-row">
                  <span class="lke-id-lbl">ID</span>
                  <input class="bl-input bl-mono" v-model="form.link_type_id" :disabled="!editMode || !!form.id"
                         placeholder="aircraft-flight-operate" />
                  <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" :title="copiedId ? '已复制' : '复制 ID'"
                          @click="copy(form.link_type_id, 'id')"
                          v-html="BL.icon(copiedId ? 'check' : 'copy', 12)"></button>
                </div>
                <div class="lke-id-row">
                  <span class="lke-id-lbl">RID</span>
                  <input class="bl-input bl-mono" :value="form.rid || '—'" disabled />
                  <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" :title="copiedRid ? '已复制' : '复制 RID'"
                          @click="copy(form.rid, 'rid')"
                          v-html="BL.icon(copiedRid ? 'check' : 'copy', 12)"></button>
                </div>
              </div>
            </div>

            <!-- 左右对称配置 -->
            <div class="lke-sym">
              <!-- 左端 -->
              <div class="lke-col">
                <div class="lke-col-hd">
                  <span class="lke-col-tag">源</span>
                  <select class="bl-input" v-model="form.l_object_type_id" :disabled="!editMode">
                    <option value="">— 源对象类型 —</option>
                    <option v-for="c in classOptions" :key="'l-'+c.id" :value="c.id">{{ c.cn || c.api_name }}</option>
                  </select>
                  <label class="lke-enabled" :title="form.l_enabled ? '已启用' : '已禁用'">
                    <input type="checkbox" v-model="form.l_enabled" :true-value="1" :false-value="0" :disabled="!editMode" />
                    <span class="lke-en-track"><span class="lke-en-dot"></span></span>
                  </label>
                </div>

                <!-- 基数 -->
                <div class="lke-cell">
                  <div class="lke-cell-lbl">基数 <span class="bl-muted" v-html="BL.icon('help', 11)" title="约束源端关联数量"></span></div>
                  <div class="lke-card-seg">
                    <button :class="['lke-card-btn', form.l_cardinality === 'one' && 'is-on']"
                            @click="setCard('l', 'one')" :disabled="!editMode">一个</button>
                    <button :class="['lke-card-btn', form.l_cardinality === 'many' && 'is-on']"
                            @click="setCard('l', 'many')" :disabled="!editMode">多个</button>
                  </div>
                </div>

                <!-- 键配置 -->
                <div class="lke-cell">
                  <div class="lke-cell-lbl">{{ keyLabel('l') }} <span class="bl-muted" v-html="BL.icon('help', 11)" title="参与关联的字段"></span></div>
                  <div class="lke-keys">
                    <div v-for="(m, i) in leftMappings" :key="'lm-'+i" class="lke-key-row">
                      <span class="lke-key-seq">{{ i + 1 }}</span>
                      <input class="bl-input bl-mono" v-model="m.object_field" :disabled="!editMode" placeholder="属性字段" />
                      <input v-if="form.is_data_source_rel" class="bl-input bl-mono" v-model="m.join_table_column"
                             :disabled="!editMode" placeholder="中间表列" />
                      <button v-if="editMode && leftMappings.length > 1" class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon"
                              @click="removeMapping('left', i)" v-html="BL.icon('trash', 11)"></button>
                    </div>
                    <button v-if="editMode" class="bl-btn bl-btn-sm bl-btn-text lke-add-key" @click="addMapping('left')">
                      <span v-html="BL.icon('plus', 11)"></span><span style="margin-left:4px">添加</span>
                    </button>
                    <div class="bl-muted lke-key-tip">支持复合主键, 可添加多个字段对</div>
                  </div>
                </div>

                <div class="lke-cell">
                  <div class="lke-cell-lbl">显示名称 <span class="bl-muted" v-html="BL.icon('help', 11)" title="前端展示文案"></span></div>
                  <div class="lke-input-wrap">
                    <input class="bl-input" v-model="form.l_display_name" :disabled="!editMode" placeholder="如: 执飞航班" />
                    <span v-if="form.l_display_name" class="lke-valid-tag">有效</span>
                  </div>
                </div>

                <div v-if="form.l_cardinality === 'many'" class="lke-cell">
                  <div class="lke-cell-lbl">复数显示名称</div>
                  <input class="bl-input" v-model="form.l_plural_name" :disabled="!editMode" placeholder="如: 执飞航班列表" />
                </div>

                <div class="lke-cell">
                  <div class="lke-cell-lbl">可见性</div>
                  <select class="bl-input" v-model="form.l_visibility" :disabled="!editMode">
                    <option value="normal">正常</option>
                    <option value="prominent">优先</option>
                    <option value="hidden">隐藏</option>
                  </select>
                </div>

                <div class="lke-cell">
                  <div class="lke-cell-lbl">API 名称 <span class="bl-muted" v-html="BL.icon('help', 11)" title="代码调用名,驼峰命名,同对象下唯一"></span></div>
                  <div class="lke-input-wrap">
                    <input class="bl-input bl-mono" v-model="form.l_api_name" :disabled="!editMode" placeholder="operatedFlights" />
                    <span v-if="isValidApi(form.l_api_name)" class="lke-valid-tag">有效</span>
                  </div>
                </div>
              </div>

              <!-- 中间数据源开关 (跨两列) -->
              <div class="lke-bridge">
                <div class="lke-bridge-line"></div>
              </div>

              <!-- 右端 -->
              <div class="lke-col">
                <div class="lke-col-hd">
                  <span class="lke-col-tag lke-col-tag-r">目标</span>
                  <select class="bl-input" v-model="form.r_object_type_id" :disabled="!editMode">
                    <option value="">— 目标对象类型 —</option>
                    <option v-for="c in classOptions" :key="'r-'+c.id" :value="c.id">{{ c.cn || c.api_name }}</option>
                  </select>
                  <label class="lke-enabled">
                    <input type="checkbox" v-model="form.r_enabled" :true-value="1" :false-value="0" :disabled="!editMode" />
                    <span class="lke-en-track"><span class="lke-en-dot"></span></span>
                  </label>
                </div>

                <div class="lke-cell">
                  <div class="lke-cell-lbl">基数</div>
                  <div class="lke-card-seg">
                    <button :class="['lke-card-btn', form.r_cardinality === 'one' && 'is-on']"
                            @click="setCard('r', 'one')" :disabled="!editMode">一个</button>
                    <button :class="['lke-card-btn', form.r_cardinality === 'many' && 'is-on']"
                            @click="setCard('r', 'many')" :disabled="!editMode">多个</button>
                  </div>
                </div>

                <div class="lke-cell">
                  <div class="lke-cell-lbl">{{ keyLabel('r') }}</div>
                  <div class="lke-keys">
                    <div v-for="(m, i) in rightMappings" :key="'rm-'+i" class="lke-key-row">
                      <span class="lke-key-seq">{{ i + 1 }}</span>
                      <input class="bl-input bl-mono" v-model="m.object_field" :disabled="!editMode" placeholder="属性字段" />
                      <input v-if="form.is_data_source_rel" class="bl-input bl-mono" v-model="m.join_table_column"
                             :disabled="!editMode" placeholder="中间表列" />
                      <button v-if="editMode && rightMappings.length > 1" class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon"
                              @click="removeMapping('right', i)" v-html="BL.icon('trash', 11)"></button>
                    </div>
                    <button v-if="editMode" class="bl-btn bl-btn-sm bl-btn-text lke-add-key" @click="addMapping('right')">
                      <span v-html="BL.icon('plus', 11)"></span><span style="margin-left:4px">添加</span>
                    </button>
                    <div class="bl-muted lke-key-tip">支持复合主键, 字段顺序必须与对端对齐</div>
                  </div>
                </div>

                <div class="lke-cell">
                  <div class="lke-cell-lbl">显示名称</div>
                  <div class="lke-input-wrap">
                    <input class="bl-input" v-model="form.r_display_name" :disabled="!editMode" placeholder="如: 执飞机型" />
                    <span v-if="form.r_display_name" class="lke-valid-tag">有效</span>
                  </div>
                </div>

                <div v-if="form.r_cardinality === 'many'" class="lke-cell">
                  <div class="lke-cell-lbl">复数显示名称</div>
                  <input class="bl-input" v-model="form.r_plural_name" :disabled="!editMode" placeholder="如: 执飞机队列表" />
                </div>

                <div class="lke-cell">
                  <div class="lke-cell-lbl">可见性</div>
                  <select class="bl-input" v-model="form.r_visibility" :disabled="!editMode">
                    <option value="normal">正常</option>
                    <option value="prominent">优先</option>
                    <option value="hidden">隐藏</option>
                  </select>
                </div>

                <div class="lke-cell">
                  <div class="lke-cell-lbl">API 名称</div>
                  <div class="lke-input-wrap">
                    <input class="bl-input bl-mono" v-model="form.r_api_name" :disabled="!editMode" placeholder="operatingAircraft" />
                    <span v-if="isValidApi(form.r_api_name)" class="lke-valid-tag">有效</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 中间数据源关联 (data_source_rel) — 单独一行,在 sym 下方 -->
            <div class="lke-data-source">
              <label class="lke-ds-toggle">
                <input type="checkbox" v-model="form.is_data_source_rel" :true-value="1" :false-value="0" :disabled="!editMode" />
                <span>中间数据源 (依托物理中间表关联)</span>
              </label>
              <input v-if="form.is_data_source_rel" class="bl-input bl-mono"
                     v-model="form.rel_data_table" :disabled="!editMode"
                     placeholder="中间表名 (aircraft_flight_rel)" style="margin-left:8px;flex:1;max-width:280px" />
            </div>

            <!-- 类型类配置 -->
            <div class="lke-section">
              <div class="lke-section-hd">
                <span>类型类 <span class="bl-muted">({{ form.type_classes?.length || 0 }})</span></span>
                <button v-if="editMode" class="bl-btn bl-btn-sm bl-btn-text" @click="addTypeClass">
                  <span v-html="BL.icon('plus', 11)"></span><span style="margin-left:4px">添加新类型类</span>
                </button>
              </div>
              <div class="lke-tc-list" v-if="form.type_classes?.length">
                <div v-for="(tc, i) in form.type_classes" :key="i" class="lke-tc-row">
                  <input class="bl-input bl-mono" v-model="tc.category" placeholder="种类 (vertex)" :disabled="!editMode" style="width:140px" />
                  <span class="bl-muted">:</span>
                  <input class="bl-input bl-mono" v-model="tc.name" placeholder="名称 (component)" :disabled="!editMode" style="flex:1" />
                  <button v-if="editMode" class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" @click="removeTypeClass(i)" v-html="BL.icon('x', 11)"></button>
                </div>
              </div>
              <div v-else class="bl-muted" style="font-size:12px">尚未配置类型类</div>
            </div>

            <!-- 备注 -->
            <div class="lke-section">
              <div class="lke-section-hd">备注</div>
              <textarea class="bl-textarea" rows="3" v-model="form.rdfs_comment" :disabled="!editMode"
                        placeholder="业务说明 / 使用场景"></textarea>
            </div>
          </section>

          <!-- 链接关系图 -->
          <section v-else-if="activeTab === 'graph'" class="lke-graph-stub">
            <div class="bl-empty" style="padding:48px">
              <span v-html="BL.icon('link', 32)"></span>
              <div style="margin-top:12px">参考「对象类型 → 对象图谱」中的链接可视化</div>
              <div class="bl-muted" style="font-size:12px;margin-top:4px">该页签集成 Canvas 链接图谱组件 (与对象类型详情共用)</div>
            </div>
          </section>
        </div>

        <!-- 底部 -->
        <footer class="lke-ft">
          <button v-if="editMode && form.id" class="bl-btn bl-btn-danger" @click="onDelete">
            <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">删除</span>
          </button>
          <span style="flex:1"></span>
          <button class="bl-btn" @click="onClose">取消</button>
          <button v-if="editMode" class="bl-btn bl-btn-primary" :disabled="!canSave" @click="onSave">保存</button>
        </footer>
      </aside>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { linkTypeApi } from '@/api'

const props = defineProps({
  open: { type: Boolean, default: false },
  linkId: { type: String, default: '' },
  allClasses: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:open', 'saved', 'deleted'])

const tabs = [
  { k: 'basic', label: '基础信息' },
  { k: 'graph', label: '链接关系图' }
]
const activeTab = ref('basic')
const editMode = ref(true)
const form = reactive(defaultForm())
function defaultForm() {
  return {
    id: '', link_type_id: '', rid: '', status: 'experimental',
    l_object_type_id: '', r_object_type_id: '',
    l_cardinality: 'one', r_cardinality: 'one',
    l_display_name: '', l_plural_name: '', r_display_name: '', r_plural_name: '',
    l_visibility: 'normal', r_visibility: 'normal',
    l_api_name: '', r_api_name: '',
    l_enabled: 1, r_enabled: 1,
    is_data_source_rel: 0, rel_data_table: '',
    rdfs_label: '', rdfs_comment: '', category_code: '',
    mappings: [], type_classes: []
  }
}

const leftMappings = computed({
  get: () => form.mappings.filter(m => m.side === 'left'),
  set: (v) => {}
})
const rightMappings = computed({
  get: () => form.mappings.filter(m => m.side === 'right'),
  set: (v) => {}
})

const classOptions = computed(() => (props.allClasses || []).map(c => ({
  id: c.id, cn: c.display_name || c.rdfs_label || c.api_name, api_name: c.api_name
})))

/* —— 打开生命周期 —— */
watch(() => props.open, async (v) => {
  if (!v) return
  activeTab.value = 'basic'
  if (props.linkId) {
    // 加载详情
    const res = await linkTypeApi.get(props.linkId).catch(() => null)
    if (res) {
      Object.assign(form, defaultForm(), res)
      form.mappings = (res.mappings || []).map(m => ({ ...m }))
      form.type_classes = (res.type_classes || []).map(tc => ({ ...tc }))
      editMode.value = false  // 已有数据默认只读
    }
  } else {
    Object.assign(form, defaultForm())
    form.mappings = [
      { side: 'left',  seq: 1, object_field: '', join_table_column: '' },
      { side: 'right', seq: 1, object_field: '', join_table_column: '' }
    ]
    form.type_classes = []
    editMode.value = true
  }
})

/* —— 基数切换 —— */
function setCard(side, val) {
  if (!editMode.value) return
  if (side === 'l') form.l_cardinality = val
  else form.r_cardinality = val
}
function keyLabel(side) {
  const card = side === 'l' ? form.l_cardinality : form.r_cardinality
  if (form.is_data_source_rel) return '键 (中间表列)'
  if (card === 'one') return '键 (主键)'
  return '键 (外键)'
}

/* —— 映射 —— */
function addMapping(side) {
  const list = form.mappings.filter(m => m.side === side)
  const seq = (list[list.length - 1]?.seq || 0) + 1
  form.mappings.push({ side, seq, object_field: '', join_table_column: '' })
}
function removeMapping(side, idx) {
  const list = form.mappings.filter(m => m.side === side)
  const target = list[idx]
  if (target) form.mappings = form.mappings.filter(m => m !== target)
  // 重新编号
  let lSeq = 1, rSeq = 1
  form.mappings.forEach(m => {
    if (m.side === 'left') m.seq = lSeq++
    else m.seq = rSeq++
  })
}

/* —— 类型类 —— */
function addTypeClass() {
  form.type_classes = [...(form.type_classes || []), { category: '', name: '', applicable_type: 'relation' }]
}
function removeTypeClass(i) {
  form.type_classes.splice(i, 1)
}

/* —— 校验 —— */
function isValidApi(s) {
  return !!s && /^[a-z][A-Za-z0-9]*$/.test(s)
}
const canSave = computed(() =>
  form.link_type_id && /^[a-z][a-z0-9-]*$/.test(form.link_type_id)
  && form.l_object_type_id && form.r_object_type_id
  && form.l_display_name && form.r_display_name
  && isValidApi(form.l_api_name) && isValidApi(form.r_api_name)
  && (!form.is_data_source_rel || form.rel_data_table)
)

/* —— 保存 / 删除 —— */
async function onSave() {
  try {
    if (form.id) {
      await linkTypeApi.update(form.id, { ...form })
      BL.success('已保存')
    } else {
      await linkTypeApi.create({ ...form })
      BL.success('已创建')
    }
    emit('saved')
    emit('update:open', false)
  } catch (e) {
    BL.error(e?.msg || '保存失败')
  }
}
async function onDelete() {
  const ok = await BL.confirm({
    title: '删除链接',
    content: `确定删除「${form.rdfs_label || form.link_type_id}」?`,
    danger: true, okText: '删除'
  })
  if (!ok) return
  try {
    await linkTypeApi.remove(form.id)
    BL.success('已删除')
    emit('deleted')
  } catch (e) { BL.error(e?.msg || '删除失败') }
}
function onClose() { emit('update:open', false) }

/* —— RID / ID 复制 —— */
const copiedId = ref(false)
const copiedRid = ref(false)
async function copy(text, kind) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    if (kind === 'id') { copiedId.value = true; setTimeout(() => copiedId.value = false, 1500) }
    else { copiedRid.value = true; setTimeout(() => copiedRid.value = false, 1500) }
  } catch {}
}

/* —— 宽度 / 最大化 / 拖拽 —— */
const width = ref(parseInt(localStorage.getItem('bl.lke.width') || '900'))
const isMax = computed(() => width.value >= 1200)
function toggleMax() {
  width.value = isMax.value ? 900 : 1200
  localStorage.setItem('bl.lke.width', String(width.value))
}
const resizing = ref(false)
function onHandleDown(ev) {
  ev.preventDefault()
  resizing.value = true
  const startX = ev.clientX, baseW = width.value
  function move(e) {
    const dx = startX - e.clientX
    width.value = Math.max(720, Math.min(Math.floor(window.innerWidth * 0.95), baseW + dx))
  }
  function up() {
    resizing.value = false
    localStorage.setItem('bl.lke.width', String(width.value))
    window.removeEventListener('mousemove', move)
    window.removeEventListener('mouseup', up)
    document.body.style.userSelect = ''
  }
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', move)
  window.addEventListener('mouseup', up)
}

/* —— 工具 —— */
function statusLabel(s) { return ({ active: '正式', experimental: '实验中', deprecated: '已废弃' })[s] || s }
function statusTagCls(s) {
  return ({ active: 'bl-tag-success', experimental: 'bl-tag-warning', deprecated: 'bl-tag-danger' })[s] || ''
}
function shortTime(t) { if (!t) return '—'; return String(t).slice(0, 16) }

/* 头部图标染色: 按基数对应 (与列表行图标配色一致) */
const cardColor = computed(() => {
  const k = `${form.l_cardinality}:${form.r_cardinality}`
  return ({ 'one:one': '#1677ff', 'one:many': '#00B42A', 'many:one': '#FF7D00', 'many:many': '#722ED1' })[k] || '#1677ff'
})
</script>

<style scoped>
.lke-drawer {
  position: fixed; top: 0; right: 0; bottom: 0;
  background: var(--bl-bg-1);
  border-left: 1px solid var(--bl-border);
  box-shadow: -4px 0 16px rgba(0,0,0,.10);
  display: flex; flex-direction: column;
  min-width: 720px; max-width: 95vw; z-index: 1000;
  overflow: hidden;
}
.lke-handle {
  position: absolute; left: -2px; top: 0; bottom: 0; width: 5px;
  cursor: col-resize; transition: background-color .15s; z-index: 6;
}
.lke-handle:hover, .lke-handle.is-resizing { background: var(--bl-primary); }
.lke-drawer-enter-active, .lke-drawer-leave-active { transition: transform .25s, opacity .2s; }
.lke-drawer-enter-from, .lke-drawer-leave-to { transform: translateX(20px); opacity: 0; }

/* 头部 (对齐 SharedPropertyDetailDrawer .detail-hd 视觉) */
.lke-hd {
  height: 56px; padding: 0 14px;
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid var(--bl-divider);
  gap: 8px; flex-shrink: 0;
}
.lke-hd-l { display: flex; align-items: center; gap: 10px; min-width: 0; flex: 1; }
.lke-ic {
  width: 22px; height: 22px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.lke-ic-lg { width: 36px; height: 36px; border-radius: 8px; }
.lke-title-wrap { min-width: 0; }
.lke-title { font-size: 14px; font-weight: 600; color: var(--bl-text-1); }
.lke-code { font-size: 12px; font-weight: 400; margin-left: 4px; }
.lke-meta { font-size: 11px; color: var(--bl-text-3); margin-top: 2px; display: flex; align-items: center; }
.lke-hd-r { display: inline-flex; align-items: center; gap: 4px; flex-shrink: 0; }
.lke-divider { width: 1px; height: 18px; background: var(--bl-divider); margin: 0 6px; flex-shrink: 0; }

/* 编辑模式按钮 (图标 + 文字 胶囊): 默认灰, 启用蓝 */
.lke-edit-btn {
  display: inline-flex; align-items: center;
  padding: 0 8px; height: 26px;
  font-size: 12px; color: var(--bl-text-2);
  border-radius: 4px;
  transition: background-color .15s, color .15s;
}
.lke-edit-btn:hover { color: var(--bl-text-1); background: var(--bl-bg-2); }
.lke-edit-btn.is-edit-on {
  color: var(--bl-primary);
  background: var(--bl-primary-soft);
  font-weight: 500;
}

/* 页签 */
.lke-tabs { display: flex; padding: 0 16px; border-bottom: 1px solid var(--bl-divider); flex-shrink: 0; }
.lke-tab {
  padding: 10px 14px; font-size: 13px;
  color: var(--bl-text-2); cursor: pointer;
  border-bottom: 2px solid transparent; margin-bottom: -1px;
  background: transparent; border: 0; border-bottom: 2px solid transparent;
}
.lke-tab:hover { color: var(--bl-text-1); }
.lke-tab.is-on { color: var(--bl-primary); border-color: var(--bl-primary); font-weight: 500; }

/* 主体 */
.lke-body { flex: 1; overflow: auto; padding: 14px 18px; }
.lke-body.is-readonly :deep(input):disabled,
.lke-body.is-readonly :deep(select):disabled,
.lke-body.is-readonly :deep(textarea):disabled,
.lke-body.is-readonly :deep(button):disabled { background: var(--bl-bg-2); color: var(--bl-text-2); }

/* 全局元数据 */
.lke-meta-row {
  display: flex; justify-content: space-between; align-items: flex-start; gap: 16px;
  padding: 10px 12px; background: var(--bl-bg-2); border-radius: 6px;
  margin-bottom: 16px;
}
.lke-meta-l { display: inline-flex; align-items: center; }
.lke-status-sel { width: 110px; height: 28px; }
.lke-meta-r { display: flex; flex-direction: column; gap: 6px; flex: 1; max-width: 480px; }
.lke-id-row { display: flex; align-items: center; gap: 6px; }
.lke-id-lbl { width: 36px; font-size: 12px; color: var(--bl-text-3); text-align: right; flex-shrink: 0; }
.lke-id-row .bl-input { flex: 1; height: 28px; font-size: 12px; }

/* 左右对称 */
.lke-sym {
  display: grid;
  grid-template-columns: 1fr 24px 1fr;
  gap: 0;
}
.lke-bridge {
  display: flex; align-items: center; justify-content: center;
}
.lke-bridge-line { width: 1px; height: 100%; background: var(--bl-divider); }
.lke-col {
  display: flex; flex-direction: column;
  padding: 0 12px;
}
.lke-col-hd {
  display: flex; align-items: center; gap: 8px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--bl-divider);
  margin-bottom: 12px;
}
.lke-col-tag {
  padding: 2px 8px; border-radius: 4px;
  background: var(--bl-primary-soft); color: var(--bl-primary);
  font-size: 11px; font-weight: 600; flex-shrink: 0;
}
.lke-col-tag-r { background: #fff2e0; color: #FF7D00; }
.lke-col-hd .bl-input { flex: 1; height: 30px; }
.lke-enabled { display: inline-flex; align-items: center; cursor: pointer; flex-shrink: 0; }
.lke-enabled input { display: none; }
.lke-en-track { width: 28px; height: 14px; border-radius: 8px; background: #c9cdd4; position: relative; }
.lke-enabled input:checked + .lke-en-track { background: var(--bl-success); }
.lke-en-dot { position: absolute; left: 2px; top: 2px; width: 10px; height: 10px; border-radius: 50%; background: #fff; transition: left .15s; }
.lke-enabled input:checked + .lke-en-track .lke-en-dot { left: 16px; }

.lke-cell { padding: 8px 0; }
.lke-cell-lbl { font-size: 12px; color: var(--bl-text-3); margin-bottom: 4px; display: flex; align-items: center; gap: 4px; }
.lke-input-wrap { position: relative; display: flex; align-items: center; }
.lke-input-wrap .bl-input { flex: 1; }
.lke-valid-tag {
  position: absolute; right: 8px; top: 50%; transform: translateY(-50%);
  font-size: 10px; background: #e8fff4; color: #00b42a;
  padding: 1px 6px; border-radius: 3px;
}

/* 基数 segmented control */
.lke-card-seg {
  display: flex; background: var(--bl-bg-2);
  border-radius: 4px; padding: 2px;
}
.lke-card-btn {
  flex: 1; padding: 6px 0; border: 0; background: transparent;
  cursor: pointer; font-size: 12.5px; color: var(--bl-text-2);
  border-radius: 3px;
}
.lke-card-btn:hover:not(:disabled) { color: var(--bl-text-1); }
.lke-card-btn.is-on { background: var(--bl-bg-1); color: var(--bl-primary); font-weight: 500;
  box-shadow: 0 1px 2px rgba(0,0,0,.08); }
.lke-card-btn:disabled { opacity: .55; cursor: not-allowed; }

/* 键配置 */
.lke-keys { display: flex; flex-direction: column; gap: 4px; }
.lke-key-row { display: flex; align-items: center; gap: 4px; }
.lke-key-seq {
  width: 22px; height: 22px; line-height: 22px;
  text-align: center; background: var(--bl-primary-soft); color: var(--bl-primary);
  font-size: 11px; font-weight: 600; border-radius: 50%; flex-shrink: 0;
}
.lke-key-row .bl-input { flex: 1; height: 28px; font-size: 12px; }
.lke-add-key { align-self: flex-start; padding: 2px 4px; font-size: 12px; }
.lke-key-tip { font-size: 11px; margin-top: 2px; }

/* 中间数据源 */
.lke-data-source {
  margin: 18px 0 12px;
  padding: 10px 12px;
  background: #fffbf0;
  border: 1px dashed #FF7D00;
  border-radius: 6px;
  display: flex; align-items: center;
}
.lke-ds-toggle {
  display: inline-flex; align-items: center; gap: 6px;
  cursor: pointer; font-size: 13px; color: var(--bl-text-1);
}

/* 章节 */
.lke-section { margin-top: 16px; }
.lke-section-hd {
  display: flex; justify-content: space-between; align-items: center;
  padding-left: 8px; border-left: 3px solid var(--bl-primary);
  font-size: 12px; color: var(--bl-text-3); font-weight: 500;
  margin-bottom: 8px;
}

/* 类型类 */
.lke-tc-list { display: flex; flex-direction: column; gap: 4px; }
.lke-tc-row { display: flex; align-items: center; gap: 6px; }

/* textarea */
.bl-textarea {
  width: 100%; min-height: 70px; padding: 8px 10px;
  border: 1px solid var(--bl-border); border-radius: 4px;
  background: #fff; font-family: inherit; font-size: 13px;
  line-height: 1.55; resize: vertical;
}
.bl-textarea:focus { outline: none; border-color: var(--bl-primary); }
.bl-textarea:disabled { background: var(--bl-bg-2); color: var(--bl-text-2); }

/* 底部 */
.lke-ft {
  padding: 10px 14px;
  border-top: 1px solid var(--bl-divider);
  display: flex; align-items: center; gap: 8px;
  flex-shrink: 0;
}

.lke-graph-stub { display: flex; align-items: center; justify-content: center; padding: 60px 20px; text-align: center; color: var(--bl-text-3); }
</style>
