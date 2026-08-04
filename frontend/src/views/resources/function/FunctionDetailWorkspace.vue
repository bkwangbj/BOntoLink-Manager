<template>
  <Teleport to="body">
    <transition name="fdw-slide">
      <aside v-if="open" class="fdw-drawer" :style="{ width: drawerWidth + 'px' }">
        <div class="fdw-drag" :class="resizing && 'is-resizing'" @mousedown="onDragStart"></div>

        <!-- ===== 顶部信息栏 ===== -->
        <div class="fdw-hd">
          <div class="fdw-hd-l">
            <span class="fdw-fx">Fx</span>
            <div class="fdw-hd-txt">
              <div class="fdw-api bl-mono bl-truncate" :title="view.api_name">{{ view.api_name || '—' }}</div>
              <div class="fdw-hd-meta">
                <input v-if="editMode" class="bl-input fdw-label-input" v-model="form.function_label" placeholder="中文显示名称" />
                <span v-else class="fdw-label bl-truncate" :title="base.function_label">{{ base.function_label || '—' }}</span>
                <span class="fdw-lang" :style="{ background: langMeta.color }">{{ langMeta.label }}</span>
                <BlSelect v-model="viewVersionId" :options="versionOpts" size="sm" style="width:132px" />
              </div>
            </div>
          </div>
          <div class="fdw-hd-r">
            <button class="fdw-repo" @click="openRepo">
              在代码仓库中打开<span v-html="BL.icon('externalLink', 12)"></span>
            </button>
            <span class="fdw-sep"></span>
            <label class="fdw-switch" :title="editMode ? '退出编辑模式' : '进入编辑模式'">
              <span class="fdw-switch-lbl">编辑模式</span>
              <span class="fdw-toggle" :class="editMode && 'is-on'" @click="toggleEdit"><i></i></span>
            </label>
            <button class="bl-btn bl-btn-text bl-btn-icon" :title="maxed ? '恢复' : '最大化'" @click="toggleMax"
                    v-html="BL.icon(maxed ? 'minimize' : 'maximize', 14)"></button>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="onClose" v-html="BL.icon('x', 14)"></button>
          </div>
        </div>

        <!-- ===== 只读提示条 (全程展示) ===== -->
        <div class="fdw-notice">
          <span v-html="BL.icon('info', 12)"></span>
          函数签名与代码由代码仓库同步,业务元数据与运行配置可在编辑模式下修改
        </div>

        <!-- ===== 主体: 左导航 + 内容区 ===== -->
        <div class="fdw-body">
          <aside class="fdw-nav">
            <div v-for="m in MENUS" :key="m.k" :class="['fdw-nav-item', activeMenu === m.k && 'is-on']"
                 @click="activeMenu = m.k">
              <span class="fdw-nav-ic" v-html="BL.icon(m.icon, 14)"></span>{{ m.label }}
            </div>
          </aside>

          <main class="fdw-main">
            <!-- 加载 / 未找到 -->
            <div v-if="loading" class="fdw-state">
              <span class="fdw-spin"></span>加载函数详情…
            </div>
            <div v-else-if="!view.id" class="fdw-state">
              <span v-html="BL.icon('warning', 20)"></span>
              <div>未找到该函数,可能已被删除</div>
            </div>

            <!-- ================= 概览 ================= -->
            <template v-else-if="activeMenu === 'overview'">
              <!-- 基础信息 -->
              <div class="fdw-card">
                <div class="fdw-cols">
                  <!-- 左栏: 主信息 -->
                  <div class="fdw-col-main">
                    <div class="fdw-f">
                      <div class="fdw-f-k">API 名称</div>
                      <div class="fdw-f-v bl-mono fdw-strong">{{ view.api_name || '—' }}</div>
                    </div>
                    <div class="fdw-f">
                      <div class="fdw-f-k">函数说明</div>
                      <textarea v-if="editMode" class="bl-input fdw-ta" rows="3" v-model="form.rdfs_comment"
                                placeholder="描述函数的业务用途、适用场景"></textarea>
                      <div v-else class="fdw-f-v fdw-desc">{{ base.rdfs_comment || '—' }}</div>
                    </div>
                    <div class="fdw-f">
                      <div class="fdw-f-k">RID</div>
                      <div class="fdw-f-v bl-mono fdw-rid" :title="view.rid">{{ view.rid || '—' }}</div>
                    </div>
                  </div>
                  <!-- 右栏: 属性 -->
                  <div class="fdw-col-attr">
                    <div class="fdw-a">
                      <span class="fdw-a-k">可见性</span>
                      <BlSelect v-if="editMode" v-model="visibilityStr" :options="VISIBILITY_OPTS" size="sm" style="width:130px" />
                      <span v-else class="fdw-vis">
                        <span v-html="BL.icon(visMeta(base.visibility).icon, 12)"></span>{{ visMeta(base.visibility).label }}
                      </span>
                    </div>
                    <div class="fdw-a">
                      <span class="fdw-a-k">类型</span>
                      <span class="fdw-a-v">{{ typeMeta(view.function_type).label }}</span>
                    </div>
                    <div class="fdw-a">
                      <span class="fdw-a-k">发布时间</span>
                      <span class="fdw-a-v">{{ fmtTime(view.publish_time) || '未发布' }}</span>
                    </div>
                    <div class="fdw-a">
                      <span class="fdw-a-k">业务分类</span>
                      <template v-if="editMode">
                        <div class="fdw-a-dirs">
                          <ComboInput v-model="form.industry_dir" :options="industryOptions" placeholder="行业" />
                          <ComboInput v-model="form.category_dir" :options="categoryOptions" placeholder="领域" />
                        </div>
                      </template>
                      <span v-else class="fdw-a-v fdw-cat">
                        <span v-html="BL.icon('folder', 12)"></span>{{ base.industry_dir }} / {{ base.category_dir }}
                      </span>
                    </div>
                    <div class="fdw-a">
                      <span class="fdw-a-k">文件路径</span>
                      <span class="fdw-a-v bl-mono fdw-dim" :title="view.code_file_path">{{ view.code_file_path || '—' }}</span>
                    </div>
                    <div class="fdw-a">
                      <span class="fdw-a-k">类名</span>
                      <span class="fdw-a-v bl-mono">{{ view.class_name || '—' }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 代码预览 -->
              <div class="fdw-card">
                <div class="fdw-card-hd">
                  <span class="fdw-card-title">代码预览</span>
                  <span class="fdw-lang fdw-lang-sm" :style="{ background: langMeta.color }">{{ langMeta.label }}</span>
                  <span class="bl-grow"></span>
                  <button class="fdw-repo" @click="openRepo">
                    在代码仓库编辑<span v-html="BL.icon('externalLink', 12)"></span>
                  </button>
                </div>
                <CodePreview :code="view.code_content" :language="Number(view.language) || 2" />
              </div>

              <!-- 输入参数 -->
              <div class="fdw-card">
                <div class="fdw-card-hd">
                  <span class="fdw-card-title">输入参数</span>
                  <span class="fdw-badge">{{ inParams.length }}</span>
                </div>
                <table class="bl-table fdw-ptable">
                  <colgroup><col style="width:200px" /><col style="width:220px" /><col /></colgroup>
                  <thead><tr><th class="t-left">参数</th><th class="t-left">类型</th><th class="t-left">说明</th></tr></thead>
                  <tbody>
                    <tr v-for="p in inParams" :key="p.id">
                      <td class="t-left">
                        <span class="fdw-pico" v-html="BL.icon('cube', 11, '#fff')"></span>
                        <span class="bl-mono fdw-strong">{{ p.param_name }}</span>
                      </td>
                      <td class="t-left">
                        <button class="fdw-typelink" :title="isObjectType(p.param_type) ? '查看对象详情' : '基础类型'"
                                @click="gotoObject(p)">{{ p.param_type }}</button>
                      </td>
                      <td class="t-left">
                        <input v-if="editMode && isLatestView" class="bl-input fdw-desc-input" v-model="p.param_desc"
                               placeholder="补充业务含义" />
                        <span v-else class="bl-muted">{{ p.param_desc || '—' }}</span>
                      </td>
                    </tr>
                    <tr v-if="!inParams.length"><td colspan="3" class="t-center bl-muted" style="height:52px">无输入参数</td></tr>
                  </tbody>
                </table>
              </div>

              <!-- 输出类型 -->
              <div class="fdw-card">
                <div class="fdw-card-hd"><span class="fdw-card-title">输出类型</span></div>
                <table class="bl-table fdw-ptable">
                  <colgroup><col style="width:200px" /><col style="width:220px" /><col /></colgroup>
                  <thead><tr><th class="t-left">参数</th><th class="t-left">类型</th><th class="t-left">说明</th></tr></thead>
                  <tbody>
                    <tr v-for="p in outParams" :key="p.id">
                      <td class="t-left">
                        <span class="fdw-pico is-out" v-html="BL.icon('arrowRight', 11, '#fff')"></span>
                        <span class="bl-mono fdw-strong">{{ p.param_name }}</span>
                      </td>
                      <td class="t-left">
                        <button class="fdw-typelink" @click="gotoObject(p)">{{ p.param_type }}</button>
                      </td>
                      <td class="t-left">
                        <input v-if="editMode && isLatestView" class="bl-input fdw-desc-input" v-model="p.param_desc"
                               placeholder="补充业务含义" />
                        <span v-else class="bl-muted">{{ p.param_desc || '—' }}</span>
                      </td>
                    </tr>
                    <tr v-if="!outParams.length"><td colspan="3" class="t-center bl-muted" style="height:52px">无返回值</td></tr>
                  </tbody>
                </table>
              </div>
            </template>

            <!-- ================= 配置 ================= -->
            <template v-else-if="activeMenu === 'config'">
              <!-- 运行配置 -->
              <div class="fdw-card">
                <div class="fdw-card-hd"><span class="fdw-card-title">运行配置</span></div>
                <div class="fdw-rt">
                  <div v-for="f in RUNTIME_FIELDS" :key="f.k" class="fdw-rt-item">
                    <label class="fdw-rt-k">{{ f.label }}</label>
                    <div class="fdw-rt-c">
                      <div class="fdw-rt-line">
                        <!-- 结果缓存是开关型, 其余是数字输入 -->
                        <template v-if="f.k === 'enable_cache'">
                          <BlSelect v-if="editMode" v-model="enableCacheStr" :options="CACHE_OPTS" size="sm" style="width:88px" />
                          <span v-else class="fdw-rt-ro">{{ Number(runtime.enable_cache) === 1 ? '开启' : '关闭' }}</span>
                        </template>
                        <template v-else>
                          <span v-if="editMode" class="fdw-num-wrap" :class="{ 'is-err': rtError(f.k) }">
                            <input class="bl-input fdw-num" :class="f.w" v-model="runtime[f.k]" />
                            <span v-if="rtError(f.k)" class="fdw-num-badge" :title="`取值范围 ${f.min} ~ ${f.max}`">!</span>
                          </span>
                          <span v-else class="fdw-rt-ro">{{ runtime[f.k] }} {{ f.unit }}</span>
                        </template>
                        <span class="fdw-rt-range">{{ f.rangeText }}</span>
                      </div>
                      <div class="fdw-rt-desc">{{ f.desc }}</div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 环境变量 -->
              <div class="fdw-card">
                <div class="fdw-card-hd">
                  <span class="fdw-card-title">环境变量</span>
                  <span class="fdw-badge">{{ envVars.length }}</span>
                  <span class="bl-grow"></span>
                  <button v-if="editMode" class="bl-btn bl-btn-text fdw-addv" @click="addEnvVar">
                    <span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">新增变量</span>
                  </button>
                </div>
                <table class="bl-table fdw-etable">
                  <colgroup>
                    <col style="width:170px" /><col style="width:170px" /><col style="width:104px" />
                    <col style="width:150px" /><col /><col style="width:52px" />
                  </colgroup>
                  <thead>
                    <tr>
                      <th class="t-left">变量名</th><th class="t-left">值</th><th class="t-left">数据类型</th>
                      <th class="t-left">取值范围 / 可选项</th><th class="t-left">说明</th><th class="t-center">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(v, i) in envVars" :key="i" class="fdw-erow">
                      <td class="t-left">
                        <input v-if="editMode" class="bl-input bl-mono" v-model="v.var_name" placeholder="大写下划线" />
                        <span v-else class="bl-mono">{{ v.var_name }}</span>
                      </td>
                      <td class="t-left">
                        <!-- 值控件随数据类型变化 -->
                        <template v-if="editMode">
                          <BlSelect v-if="Number(v.var_type) === 3" v-model="v.var_value" :options="BOOL_OPTS" size="sm" />
                          <BlSelect v-else-if="Number(v.var_type) === 4" v-model="v.var_value" :options="enumOptsOf(v)" size="sm" />
                          <span v-else class="fdw-num-wrap" :class="{ 'is-err': envError(v) }">
                            <input class="bl-input" v-model="v.var_value" />
                            <span v-if="envError(v)" class="fdw-num-badge" :title="`取值范围 ${v.value_range}`">!</span>
                          </span>
                        </template>
                        <span v-else class="bl-mono">{{ v.is_encrypt ? '••••••' : v.var_value }}</span>
                      </td>
                      <td class="t-left">
                        <BlSelect v-if="editMode" v-model="v._typeStr" :options="VAR_TYPE_OPTS" size="sm" @change="onVarTypeChange(v)" />
                        <span v-else>{{ varTypeLabel(v.var_type) }}</span>
                      </td>
                      <td class="t-left">
                        <span v-if="Number(v.var_type) === 3" class="bl-muted">固定可选值</span>
                        <input v-else-if="editMode" class="bl-input" v-model="v.value_range"
                               :placeholder="Number(v.var_type) === 2 ? '最小值~最大值' : '逗号分隔可选值'" />
                        <span v-else class="bl-muted">{{ v.value_range || '—' }}</span>
                      </td>
                      <td class="t-left">
                        <input v-if="editMode" class="bl-input" v-model="v.var_desc" placeholder="变量业务含义" />
                        <span v-else class="bl-muted">{{ v.var_desc || '—' }}</span>
                      </td>
                      <td class="t-center">
                        <button v-if="editMode" class="fdw-edel" title="删除变量" @click="envVars.splice(i, 1)"
                                v-html="BL.icon('trash', 12)"></button>
                        <span v-else class="bl-muted">—</span>
                      </td>
                    </tr>
                    <tr v-if="!envVars.length">
                      <td colspan="6" class="t-center bl-muted" style="height:52px">
                        暂无环境变量{{ editMode ? ',点击右上角「新增变量」添加' : '' }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- 权限配置 -->
              <div class="fdw-card">
                <div class="fdw-card-hd"><span class="fdw-card-title">权限配置</span></div>
                <div class="fdw-rt-item">
                  <label class="fdw-rt-k">可见范围</label>
                  <div class="fdw-rt-c">
                    <div class="fdw-rt-line">
                      <BlSelect v-if="editMode" v-model="visibilityStr" :options="VISIBILITY_OPTS" size="sm" style="width:150px" />
                      <span v-else class="fdw-rt-ro">{{ visMeta(base.visibility).label }}</span>
                      <span class="fdw-rt-range">(调用权限)</span>
                    </div>
                    <div class="fdw-rt-desc">控制函数在平台内的可见范围与可调用主体;私有状态下仅创建者与平台管理员可见、可调用。</div>
                  </div>
                </div>
              </div>
            </template>

            <!-- ================= 可观测性 ================= -->
            <template v-else>
              <div class="fdw-stats">
                <div class="fdw-stat"><div class="fdw-stat-k">总调用量</div><div class="fdw-stat-v">{{ fmtNum(stats.total_calls) }}</div></div>
                <div class="fdw-stat"><div class="fdw-stat-k">成功率</div><div class="fdw-stat-v is-ok">{{ stats.success_rate ?? 0 }}%</div></div>
                <div class="fdw-stat"><div class="fdw-stat-k">平均耗时</div><div class="fdw-stat-v">{{ stats.avg_cost_ms ?? 0 }} ms</div></div>
                <div class="fdw-stat"><div class="fdw-stat-k">错误次数</div><div class="fdw-stat-v is-err">{{ fmtNum(stats.error_count) }}</div></div>
              </div>

              <div class="fdw-card">
                <div class="fdw-card-hd">
                  <span class="fdw-card-title">近 {{ statsDays }} 天调用趋势</span>
                  <span class="bl-grow"></span>
                  <BlSelect v-model="statsDaysStr" :options="DAY_OPTS" size="sm" style="width:110px" />
                </div>
                <FunctionTrendChart :trend="stats.trend || []" />
              </div>

              <div class="fdw-card">
                <div class="fdw-card-hd">
                  <span class="fdw-card-title">调用方</span>
                  <span class="bl-grow"></span>
                  <button class="fdw-repo" @click="onMoreCallers">查看更多<span v-html="BL.icon('chevronRight', 12)"></span></button>
                </div>
                <table class="bl-table fdw-ctable">
                  <colgroup><col style="width:60px" /><col /><col style="width:120px" /><col style="width:100px" /></colgroup>
                  <thead><tr><th class="t-center">排名</th><th class="t-left">应用名称</th><th class="t-right">调用次数</th><th class="t-right">占比</th></tr></thead>
                  <tbody>
                    <tr v-for="(c, i) in topCallers" :key="c.app">
                      <td class="t-center">{{ i + 1 }}</td>
                      <td class="t-left">{{ c.app || '—' }}</td>
                      <td class="t-right">{{ fmtNum(c.calls) }}</td>
                      <td class="t-right">{{ c.ratio }}%</td>
                    </tr>
                    <tr v-if="!topCallers.length"><td colspan="4" class="t-center bl-muted" style="height:52px">暂无调用记录</td></tr>
                  </tbody>
                </table>
              </div>
            </template>
          </main>
        </div>

        <!-- ===== 底部操作栏: 仅编辑模式生效 ===== -->
        <div v-if="editMode" class="fdw-ft">
          <span v-if="saveErr" class="fdw-err"><span v-html="BL.icon('warning', 12)"></span>{{ saveErr }}</span>
          <span class="bl-grow"></span>
          <button class="bl-btn" @click="cancelEdit">取消</button>
          <button class="bl-btn bl-btn-primary" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存修改' }}</button>
        </div>
      </aside>
    </transition>
  </Teleport>
</template>

<script setup>
/**
 * 函数详情抽屉工作台 (P3 · 文档 5.3)
 *
 * 骨架复刻 ActionDetailWorkspace:右侧抽屉、左边缘拖宽、最大化、宽度记忆、不带蒙层。
 * 内部按文档组织为「顶部信息栏 + 只读提示条 + 左导航(概览/配置/可观测性) + 底部操作栏」。
 *
 * 字段权责 (文档二):
 * - 代码派生 (api_name / 版本 / 类型 / RID / 类名 / 文件路径 / 发布时间 / 代码 / 参数名与类型) 全程只读
 * - 平台元数据 (中文名 / 说明 / 业务分类 / 可见性 / 参数说明 / 运行配置 / 环境变量) 编辑模式可改
 * - 运行统计全程只读
 *
 * 版本切换 (文档 5.3 五、5.3):代码派生字段跟随所选版本, 可编辑字段始终绑定最新版 (base)。
 */
import { ref, reactive, computed, watch, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { BL } from '@/lib/bl.js'
import { functionApi, versionRepoApi } from '@/api'
import BlSelect from '@/components/BlSelect.vue'
import ComboInput from './ComboInput.vue'
import CodePreview from './CodePreview.vue'
import FunctionTrendChart from './FunctionTrendChart.vue'
import { isObjectType } from './codeTemplate.js'

const props = defineProps({
  open: { type: Boolean, default: false },
  functionId: { type: String, default: '' }
})
const emit = defineEmits(['update:open', 'saved'])
const router = useRouter()

const MENUS = [
  { k: 'overview', label: '概览', icon: 'monitor' },
  { k: 'config', label: '配置', icon: 'sliders' },
  { k: 'observe', label: '可观测性', icon: 'trendingUp' },
]
const TYPE_META = {
  1: { label: '常规函数', color: '#165DFF' }, 2: { label: '动作函数', color: '#722ED1' },
  3: { label: '聚合函数', color: '#00B42A' }, 4: { label: '衍生函数', color: '#FF7D00' },
  5: { label: '时序函数', color: '#0FC6C2' },
}
const VIS_META = {
  1: { label: '全平台可见', icon: 'eye' }, 2: { label: '仅本部门可见', icon: 'users' },
  3: { label: '指定角色可见', icon: 'userCheck' }, 4: { label: '私有', icon: 'lock' },
}
const VISIBILITY_OPTS = [
  { value: '1', label: '全平台可见' }, { value: '2', label: '仅本部门可见' },
  { value: '3', label: '指定角色可见' }, { value: '4', label: '私有' },
]
const CACHE_OPTS = [{ value: '1', label: '开启' }, { value: '0', label: '关闭' }]
const BOOL_OPTS = [{ value: 'true', label: 'true' }, { value: 'false', label: 'false' }]
const VAR_TYPE_OPTS = [
  { value: '1', label: '字符串' }, { value: '2', label: '数字型' },
  { value: '3', label: '布尔型' }, { value: '4', label: '枚举型' },
]
const DAY_OPTS = [{ value: '7', label: '近 7 天' }, { value: '30', label: '近 30 天' }, { value: '90', label: '近 90 天' }]

/* 运行配置字段明细 (文档 4.5.3.2);cache_ttl 表里有列, 文档字段表漏列, 一并放出来可编辑 */
const RUNTIME_FIELDS = [
  { k: 'timeout', label: '超时时间', min: 1, max: 3600, unit: '秒', w: 'is-w4', rangeText: '(1~3600秒)',
    desc: '函数执行最大时长,超出将强制终止,防止死循环 / 慢查询占用资源' },
  { k: 'retry_count', label: '重试次数', min: 0, max: 5, unit: '次', w: 'is-w1', rangeText: '(0~5次)',
    desc: '执行失败后的自动重试次数,0 表示不重试,应对临时网络波动' },
  { k: 'retry_interval', label: '重试间隔', min: 0, max: 60, unit: '秒', w: 'is-w2', rangeText: '(0~60秒)',
    desc: '两次重试之间的等待时长,避免高频重试打垮下游服务' },
  { k: 'memory_quota', label: '内存配额', min: 128, max: 4096, unit: 'MB', w: 'is-w4', rangeText: '(128~4096MB)',
    desc: '函数运行最大可用内存,超出将触发 OOM 终止执行' },
  { k: 'concurrency_limit', label: '并发限制', min: 1, max: 1000, unit: '个', w: 'is-w4', rangeText: '(1~1000个)',
    desc: '单实例最大同时执行请求数,控制流量峰值保护下游依赖' },
  { k: 'enable_cache', label: '结果缓存', unit: '', w: '', rangeText: '(开/关)',
    desc: '相同入参直接返回缓存结果,提升重复调用响应速度' },
  { k: 'cache_ttl', label: '缓存有效期', min: 60, max: 86400, unit: '秒', w: 'is-w5', rangeText: '(60~86400秒)',
    desc: '缓存数据的有效时长,超过后重新执行函数' },
]

/* —— 数据 —— */
const base = ref({})        // 最新版本 (可编辑字段的来源)
const view = ref({})        // 当前查看版本 (代码派生字段的来源)
const versions = ref([])
const viewVersionId = ref('')
const activeMenu = ref('overview')
const editMode = ref(false)
const loading = ref(false)
const saving = ref(false)
const saveErr = ref('')

const form = reactive({ function_label: '', rdfs_comment: '', visibility: 1, industry_dir: '', category_dir: '' })
const runtime = reactive({ timeout: 30, retry_count: 2, retry_interval: 1, memory_quota: 512, concurrency_limit: 100, enable_cache: 1, cache_ttl: 3600 })
const envVars = ref([])
const params = ref([])
const stats = ref({})
const statsDays = ref(30)

const visibilityStr = computed({ get: () => String(form.visibility), set: v => { form.visibility = Number(v) || 1 } })
const enableCacheStr = computed({ get: () => String(runtime.enable_cache), set: v => { runtime.enable_cache = Number(v) || 0 } })
const statsDaysStr = computed({ get: () => String(statsDays.value), set: v => { statsDays.value = Number(v) || 30; loadStats() } })

const langMeta = computed(() => Number(view.value.language) === 1
  ? { label: 'Python', color: '#3776AB' } : { label: 'TypeScript', color: '#3178C6' })
function typeMeta(v) { return TYPE_META[Number(v)] || { label: '—', color: '#86909c' } }
function visMeta(v) { return VIS_META[Number(v)] || VIS_META[1] }

const isLatestView = computed(() => !viewVersionId.value || viewVersionId.value === base.value.id)
const inParams = computed(() => params.value.filter(p => Number(p.param_direction) !== 2))
const outParams = computed(() => params.value.filter(p => Number(p.param_direction) === 2))
const versionOpts = computed(() => versions.value.map((v, i) => ({
  value: v.id, label: `${v.version_no}${i === 0 ? ' 最新' : ''}`
})))
const topCallers = computed(() => (stats.value.callers || []).slice(0, 3))

/* —— 目录选项 (业务分类编辑) —— */
const dirOptions = ref([])
const industryOptions = computed(() => dirOptions.value.map(d => d.industry_dir))
const categoryOptions = computed(() => {
  const hit = dirOptions.value.find(d => d.industry_dir === form.industry_dir)
  return hit ? (hit.categories || []) : []
})

/* —— 加载 —— */
async function load() {
  if (!props.functionId) return
  loading.value = true
  const d = await functionApi.get(props.functionId).catch(() => null).finally(() => { loading.value = false })
  if (!d) { base.value = {}; view.value = {}; versions.value = []; params.value = []; return }
  base.value = d
  view.value = d
  versions.value = (d.versions || []).map(v => ({ ...v }))
  viewVersionId.value = d.id
  params.value = (d.params || []).map(p => ({ ...p }))
  resetForms()
  loadStats()
  versionRepoApi.dirOptions().then(list => { dirOptions.value = Array.isArray(list) ? list : [] }).catch(() => {})
}
function resetForms() {
  const d = base.value
  form.function_label = d.function_label || ''
  form.rdfs_comment = d.rdfs_comment || ''
  form.visibility = Number(d.visibility) || 1
  form.industry_dir = d.industry_dir || ''
  form.category_dir = d.category_dir || ''
  const rc = d.runtime_config || {}
  Object.keys(runtime).forEach(k => { if (rc[k] !== undefined && rc[k] !== null) runtime[k] = rc[k] })
  envVars.value = (d.env_vars || []).map(v => ({ ...v, _typeStr: String(v.var_type) }))
  params.value = (d.params || []).map(p => ({ ...p }))
  saveErr.value = ''
}
async function loadStats() {
  if (!props.functionId) return
  stats.value = await functionApi.stats(props.functionId, statsDays.value).catch(() => ({}))
}

watch(() => props.open, (v) => {
  if (!v) return
  activeMenu.value = 'overview'
  editMode.value = false
  statsDays.value = 30
  load()
})
watch(() => props.functionId, () => { if (props.open) load() })

/* 版本切换: 只换代码派生字段, 可编辑字段仍绑定 base */
watch(viewVersionId, async (id) => {
  if (!id || id === base.value.id) { view.value = base.value; params.value = (base.value.params || []).map(p => ({ ...p })); return }
  const d = await functionApi.get(id).catch(() => null)
  if (!d) return
  view.value = d
  params.value = (d.params || []).map(p => ({ ...p }))
})

/* —— 编辑模式 —— */
function toggleEdit() {
  if (editMode.value) { cancelEdit(); return }
  if (!isLatestView.value) { BL.warning('历史版本为只读,请切回最新版本再编辑'); return }
  editMode.value = true
}
function cancelEdit() {
  resetForms()
  editMode.value = false
}

/* —— 校验 —— */
function rtError(k) {
  const f = RUNTIME_FIELDS.find(x => x.k === k)
  if (!f || f.min === undefined) return false
  const raw = String(runtime[k] ?? '').trim()
  if (raw === '' || isNaN(Number(raw))) return false     // 空值 / 非数字不触发错误视觉
  const n = Number(raw)
  return n < f.min || n > f.max
}
/** 数字型环境变量按 "最小值~最大值" 校验 */
function envError(v) {
  if (Number(v.var_type) !== 2) return false
  const raw = String(v.var_value ?? '').trim()
  if (raw === '' || isNaN(Number(raw))) return false
  const m = String(v.value_range || '').match(/(-?\d+(?:\.\d+)?)\s*[~-]\s*(-?\d+(?:\.\d+)?)/)
  if (!m) return false
  const n = Number(raw)
  return n < Number(m[1]) || n > Number(m[2])
}
const hasError = computed(() =>
  RUNTIME_FIELDS.some(f => rtError(f.k)) || envVars.value.some(v => envError(v)))

/* —— 环境变量 —— */
function addEnvVar() {
  envVars.value.push({ var_name: '', var_value: '', var_type: 1, _typeStr: '1', value_range: '', var_desc: '', is_encrypt: 0 })
}
function onVarTypeChange(v) {
  v.var_type = Number(v._typeStr) || 1
  if (v.var_type === 3) { v.value_range = ''; if (!['true', 'false'].includes(String(v.var_value))) v.var_value = 'true' }
}
function varTypeLabel(t) { return (VAR_TYPE_OPTS.find(o => o.value === String(t)) || {}).label || '字符串' }
function enumOptsOf(v) {
  return String(v.value_range || '').split(',').map(s => s.trim()).filter(Boolean)
    .map(s => ({ value: s, label: s }))
}

/* —— 保存 —— */
async function save() {
  saveErr.value = ''
  if (hasError.value) { saveErr.value = '存在超出取值范围的配置项,请修正后再保存'; return }
  if (!form.industry_dir.trim() || !form.category_dir.trim()) { saveErr.value = '业务分类的行业与领域不能为空'; return }
  if (envVars.value.some(v => !String(v.var_name || '').trim())) { saveErr.value = '环境变量名不能为空'; return }

  saving.value = true
  try {
    await functionApi.update(props.functionId, {
      function_label: form.function_label.trim(),
      rdfs_comment: form.rdfs_comment.trim(),
      visibility: form.visibility,
      industry_dir: form.industry_dir.trim(),
      category_dir: form.category_dir.trim()
    })
    await functionApi.saveRuntime(props.functionId, { ...runtime })
    await functionApi.saveEnvVars(props.functionId, envVars.value.map((v, i) => ({
      var_name: v.var_name, var_value: v.var_value, var_type: Number(v.var_type) || 1,
      value_range: v.value_range, var_desc: v.var_desc, is_encrypt: Number(v.is_encrypt) || 0, sort_num: i + 1
    })))
    await functionApi.saveParamDesc(props.functionId,
      params.value.map(p => ({ id: p.id, param_desc: p.param_desc })))
    BL.success('已保存')
    editMode.value = false
    await load()
    emit('saved')
  } catch (e) {
    saveErr.value = e?.message || '保存失败'
    BL.error(saveErr.value)
  } finally {
    saving.value = false
  }
}

/* —— 跳转 —— */
function gotoObject(p) {
  if (!isObjectType(p.param_type)) { BL.info(`${p.param_type} 是基础类型,无对应本体对象`); return }
  if (!p.object_class_id) { BL.info('该参数尚未绑定本体对象 id,无法跳转对象详情'); return }
  router.push({ path: '/resources/object-types', query: { openId: p.object_class_id } })
  emit('update:open', false)
}
/** 新标签页打开在线编排 IDE (文档 5.2 六、6.2:所有代码仓入口均为新标签页跳转) */
function openRepo() {
  const url = router.resolve({ name: 'functionIde', params: { id: props.functionId } }).href
  window.open(url, '_blank')
}
function onMoreCallers() {
  BL.info('全量调用方明细页尚未建设,当前展示 TOP3')
}

async function onClose() {
  if (editMode.value) {
    const ok = await BL.confirm({ title: '退出编辑', content: '有未保存的修改,确定关闭?关闭后修改不会保存。', okText: '关闭' })
    if (!ok) return
  }
  emit('update:open', false)
}

/* —— 抽屉宽度: 拖拽 / 最大化 / localStorage 记忆 (与动作类型抽屉同款) —— */
const DRAWER_MIN = 820
const maxed = ref(false)
const resizing = ref(false)
function drawerMaxPx() { return Math.floor(window.innerWidth * 0.92) }
function defaultWidth() { return Math.max(DRAWER_MIN, Math.floor(window.innerWidth * 0.66)) }
const storedW = Number(localStorage.getItem('bl.fdw.width')) || 0
const drawerWidth = ref(storedW && storedW >= DRAWER_MIN ? storedW : defaultWidth())
let dragStartX = 0, dragStartW = 0
function onDragStart(e) {
  resizing.value = true; dragStartX = e.clientX; dragStartW = drawerWidth.value
  document.body.style.cursor = 'col-resize'; document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onDragMove); window.addEventListener('mouseup', onDragEnd)
}
function onDragMove(e) {
  const next = Math.min(drawerMaxPx(), Math.max(DRAWER_MIN, dragStartW + (dragStartX - e.clientX)))
  drawerWidth.value = next; maxed.value = next === drawerMaxPx()
}
function onDragEnd() {
  resizing.value = false; document.body.style.cursor = ''; document.body.style.userSelect = ''
  localStorage.setItem('bl.fdw.width', String(drawerWidth.value))
  window.removeEventListener('mousemove', onDragMove); window.removeEventListener('mouseup', onDragEnd)
}
function toggleMax() {
  drawerWidth.value = maxed.value ? defaultWidth() : drawerMaxPx()
  maxed.value = !maxed.value
  localStorage.setItem('bl.fdw.width', String(drawerWidth.value))
}
onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onDragMove); window.removeEventListener('mouseup', onDragEnd)
})

/* —— 展示工具 —— */
function fmtNum(n) { return Number(n || 0).toLocaleString('en-US') }
function fmtTime(t) { return String(t || '').slice(0, 16) }
</script>

<style scoped>
.fdw-drawer {
  position: fixed; top: 0; right: 0; bottom: 0; z-index: 1010;
  min-width: 820px;
  background: var(--bl-bg-2);
  border-left: 1px solid var(--bl-border-strong);
  box-shadow: -12px 0 32px rgba(0,0,0,.22), -2px 0 6px rgba(0,0,0,.12);
  display: flex; flex-direction: column; overflow: hidden;
}
:root[data-theme="dark"] .fdw-drawer {
  box-shadow: -16px 0 48px rgba(0,0,0,.65), -2px 0 8px rgba(0,0,0,.5), inset 1px 0 0 rgba(255,255,255,.05);
}
.fdw-drag {
  position: absolute; left: -2px; top: 0; bottom: 0; width: 5px;
  cursor: col-resize; z-index: 3; transition: background-color .15s;
}
.fdw-drag:hover, .fdw-drag.is-resizing { background: var(--bl-primary); }

/* 顶部信息栏 */
.fdw-hd {
  flex-shrink: 0; display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 10px 14px; background: var(--bl-bg-1); border-bottom: 1px solid var(--bl-divider);
}
.fdw-hd-l { display: flex; align-items: center; gap: 10px; min-width: 0; flex: 1; }
.fdw-fx {
  width: 34px; height: 34px; border-radius: 7px; flex-shrink: 0; background: #165DFF; color: #fff;
  display: inline-flex; align-items: center; justify-content: center;
  font-family: Consolas, Monaco, monospace; font-size: 13px; font-weight: 700;
}
.fdw-hd-txt { min-width: 0; }
.fdw-api { font-size: 16px; font-weight: 700; color: var(--bl-text-1); line-height: 20px; }
.fdw-hd-meta { display: flex; align-items: center; gap: 8px; margin-top: 3px; min-width: 0; }
.fdw-label { font-size: 12.5px; color: var(--bl-text-3); max-width: 220px; }
.fdw-label-input { height: 24px; width: 200px; font-size: 12.5px; }
.fdw-lang {
  flex-shrink: 0; padding: 1px 7px; border-radius: 3px; color: #fff;
  font-size: 11px; line-height: 16px;
}
.fdw-lang-sm { font-size: 10.5px; }
.fdw-hd-r { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.fdw-repo {
  display: inline-flex; align-items: center; gap: 4px;
  border: 0; background: transparent; cursor: pointer;
  color: var(--bl-primary); font-size: 12.5px; padding: 0;
}
.fdw-repo:hover { text-decoration: underline; }
.fdw-sep { width: 1px; height: 16px; background: var(--bl-border); }
.fdw-switch { display: inline-flex; align-items: center; gap: 6px; cursor: pointer; }
.fdw-switch-lbl { font-size: 12.5px; color: var(--bl-text-2); }
.fdw-toggle {
  width: 36px; height: 20px; border-radius: 10px; background: var(--bl-bg-2);
  box-shadow: inset 0 0 0 1px var(--bl-border);   /* 关闭态在浅色主题下也要看得见轨道 */
  position: relative; transition: background-color .2s; display: inline-block;
}
.fdw-toggle i {
  position: absolute; top: 2px; left: 2px; width: 16px; height: 16px; border-radius: 50%;
  background: #fff; transition: transform .2s; box-shadow: 0 1px 3px rgba(0,0,0,.2);
}
.fdw-toggle.is-on { background: var(--bl-primary); box-shadow: none; }
.fdw-toggle.is-on i { transform: translateX(16px); }

/* 只读提示条 */
.fdw-notice {
  flex-shrink: 0; height: 32px; display: flex; align-items: center; gap: 6px;
  padding: 0 14px; background: var(--bl-bg-2); color: var(--bl-text-3); font-size: 12px;
}

/* 主体 */
.fdw-body { flex: 1; min-height: 0; display: flex; }
.fdw-nav {
  flex-shrink: 0; width: 140px; background: var(--bl-bg-1);
  border-right: 1px solid var(--bl-divider); padding: 8px 6px;
}
.fdw-nav-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border-radius: 5px; font-size: 13px;
  color: var(--bl-text-2); cursor: pointer; user-select: none;
}
.fdw-nav-item:hover { background: var(--bl-bg-hover); }
.fdw-nav-item.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.fdw-nav-ic { display: inline-flex; }
.fdw-main { flex: 1; min-width: 0; overflow: auto; padding: 12px; }

/* 卡片 */
.fdw-card {
  background: var(--bl-bg-1); border: 1px solid var(--bl-border);
  border-radius: 4px; padding: 14px 16px; margin-bottom: 12px;
}
.fdw-card:last-child { margin-bottom: 0; }
.fdw-card-hd { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.fdw-card-title { font-size: 13px; font-weight: 600; color: var(--bl-text-1); }
.fdw-badge {
  padding: 0 6px; border-radius: 9px; background: var(--bl-bg-2);
  color: var(--bl-text-2); font-size: 11px; line-height: 17px;
}
.bl-grow { flex: 1; }

/* 概览: 左右两栏 */
.fdw-cols { display: grid; grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr); gap: 24px; }
.fdw-f { margin-bottom: 14px; }
.fdw-f:last-child { margin-bottom: 0; }
.fdw-f-k { font-size: 11.5px; color: var(--bl-text-3); margin-bottom: 4px; }
.fdw-f-v { font-size: 13px; color: var(--bl-text-1); }
.fdw-strong { font-weight: 600; }
.fdw-desc { color: var(--bl-text-2); line-height: 19px; font-size: 12.5px; }
.fdw-rid { font-size: 11px; color: var(--bl-text-3); word-break: break-all; }
.fdw-ta { width: 100%; resize: vertical; min-height: 62px; padding: 6px 10px; line-height: 18px; }

.fdw-a { display: flex; align-items: center; justify-content: space-between; gap: 10px; min-height: 30px; margin-bottom: 8px; }
.fdw-a-k { font-size: 12px; color: var(--bl-text-3); flex-shrink: 0; }
.fdw-a-v { font-size: 12.5px; color: var(--bl-text-1); min-width: 0; text-align: right; }
.fdw-a-dirs { display: flex; gap: 6px; min-width: 0; flex: 1; }
.fdw-vis { display: inline-flex; align-items: center; gap: 4px; color: var(--bl-primary); font-size: 12.5px; }
.fdw-cat { display: inline-flex; align-items: center; gap: 4px; }
.fdw-dim { color: var(--bl-text-3); font-size: 11.5px; }

/* 参数表 */
.fdw-ptable, .fdw-etable, .fdw-ctable { width: 100%; }
.fdw-ptable thead th, .fdw-etable thead th, .fdw-ctable thead th {
  background: var(--bl-thead-bg); font-size: 11.5px; font-weight: 600;
  height: 32px; padding: 0 8px; color: var(--bl-text-2); white-space: nowrap;
}
.fdw-ptable thead th.t-left, .fdw-etable thead th.t-left, .fdw-ctable thead th.t-left { text-align: left; }
.fdw-ctable thead th.t-right { text-align: right; }
.fdw-ptable td, .fdw-etable td, .fdw-ctable td { padding: 6px 8px; font-size: 12px; }
.fdw-ptable td.t-center, .fdw-etable td.t-center, .fdw-ctable td.t-center { text-align: center; }
.fdw-ctable td.t-right { text-align: right; }
.fdw-pico {
  width: 18px; height: 18px; border-radius: 4px; background: #165DFF; margin-right: 6px;
  display: inline-flex; align-items: center; justify-content: center; vertical-align: middle;
}
.fdw-pico.is-out { background: #00B42A; }
.fdw-typelink {
  border: 0; background: transparent; padding: 0; cursor: pointer;
  color: var(--bl-primary); font-family: Consolas, Monaco, monospace; font-size: 12px;
}
.fdw-typelink:hover { text-decoration: underline; }
.fdw-desc-input { height: 28px; width: 100%; }
.fdw-etable .bl-input { height: 28px; width: 100%; }
.fdw-erow .fdw-edel { opacity: .35; border: 0; background: transparent; cursor: pointer; color: var(--bl-text-3); }
.fdw-erow:hover .fdw-edel { opacity: 1; color: #f53f3f; }

/* 运行配置: 两列网格 */
.fdw-rt { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px 24px; }
.fdw-rt-item { display: flex; align-items: flex-start; gap: 10px; }
.fdw-rt-k { flex-shrink: 0; width: 74px; text-align: right; font-size: 12.5px; color: var(--bl-text-2); line-height: 28px; }
.fdw-rt-c { flex: 1; min-width: 0; }
.fdw-rt-line { display: flex; align-items: center; gap: 8px; min-height: 28px; }
.fdw-rt-ro { font-size: 13px; color: var(--bl-text-1); font-weight: 500; }
.fdw-rt-range { font-size: 11px; color: var(--bl-text-3); }
.fdw-rt-desc { font-size: 11px; color: var(--bl-text-3); margin-top: 3px; line-height: 16px; }
.fdw-num-wrap { position: relative; display: inline-flex; }
.fdw-num { height: 28px; text-align: center; }
.fdw-num.is-w1 { width: 46px; }
.fdw-num.is-w2 { width: 56px; }
.fdw-num.is-w4 { width: 76px; }
.fdw-num.is-w5 { width: 88px; }
.fdw-num-wrap.is-err .bl-input { border-color: #f53f3f; }
.fdw-num-badge {
  position: absolute; top: -5px; right: -5px; width: 14px; height: 14px; border-radius: 50%;
  background: #f53f3f; color: #fff; font-size: 10px; font-weight: 700; line-height: 14px; text-align: center;
}

/* 可观测性 */
.fdw-stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 12px; }
.fdw-stat { background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 4px; padding: 12px 14px; }
.fdw-stat-k { font-size: 11.5px; color: var(--bl-text-3); }
.fdw-stat-v { font-size: 22px; font-weight: 600; color: var(--bl-text-1); margin-top: 4px; font-variant-numeric: tabular-nums; }
.fdw-stat-v.is-ok { color: #00B42A; }
.fdw-stat-v.is-err { color: #F53F3F; }

/* 加载 / 空状态 */
.fdw-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px;
  height: 260px; color: var(--bl-text-3); font-size: 13px;
}
.fdw-spin {
  width: 20px; height: 20px; border-radius: 50%;
  border: 2px solid var(--bl-border); border-top-color: var(--bl-primary);
  animation: fdw-rot .8s linear infinite;
}
@keyframes fdw-rot { to { transform: rotate(360deg); } }

/* 底部操作栏 */
.fdw-ft {
  flex-shrink: 0; display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; background: var(--bl-bg-1); border-top: 1px solid var(--bl-border);
}
.fdw-err { display: inline-flex; align-items: center; gap: 4px; color: #f53f3f; font-size: 12px; }

.fdw-slide-enter-active, .fdw-slide-leave-active { transition: transform .22s ease; }
.fdw-slide-enter-from, .fdw-slide-leave-to { transform: translateX(100%); }
</style>
