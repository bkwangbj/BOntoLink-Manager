<template>
  <Teleport to="body">
    <transition name="wz-fade">
      <div v-if="open" class="wz-mask">
        <div class="wz-modal">
          <!-- 标题 -->
          <div class="wz-hd">
            <div>
              <div class="wz-title">新建动作</div>
              <div class="wz-sub bl-muted">步骤 {{ step }} / 5 · {{ STEPS[step - 1] }}</div>
            </div>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="onCancel" v-html="BL.icon('x', 14)"></button>
          </div>

          <!-- 主体: 左步骤栏 + 右内容区 -->
          <div class="wz-main">
            <aside class="wz-side">
              <div v-for="(s, i) in STEPS" :key="s"
                   :class="['wz-side-step', step === i + 1 && 'is-on', step > i + 1 && 'is-done']">
                <span class="wz-side-num">
                  <span v-if="step > i + 1" v-html="BL.icon('check', 11, '#fff')"></span>
                  <template v-else>{{ i + 1 }}</template>
                </span>
                <span class="wz-side-lbl">{{ s }}</span>
              </div>
            </aside>
            <div class="wz-content">
              <div class="wz-content-hd">
                <div class="wz-step-tag">第 {{ step }} 步</div>
                <div class="wz-content-title">{{ STEP_META[step - 1].title }}</div>
                <div class="wz-content-sub bl-muted">{{ STEP_META[step - 1].sub }}</div>
              </div>
              <div class="wz-body">
            <!-- ========== 步骤 1: 动作类型选型 ========== -->
            <template v-if="step === 1">
              <div class="acw-tabs">
                <button v-for="(m, k) in M_TYPES" :key="k"
                        :class="['acw-tab', form.m_type === Number(k) && 'is-on']"
                        @click="pickMType(Number(k))">
                  <span class="acw-tab-ic" :style="{ background: m.color }" v-html="BL.icon(m.icon, 13, '#fff')"></span>
                  {{ m.label }}
                </button>
              </div>

              <div class="sec">选择动作类型</div>
              <div class="acw-type-grid">
                <div v-for="t in currentActionTypes" :key="t.v"
                     :class="['acw-type-card', form.action_type === t.v && 'is-on']"
                     @click="pickActionType(t)">
                  <span class="acw-type-ic" :style="{ background: t.color }" v-html="BL.icon(t.icon, 16, '#fff')"></span>
                  <div class="acw-type-txt">
                    <div class="acw-type-name">{{ t.label }}</div>
                    <div class="bl-muted" style="font-size:11.5px">{{ t.desc }}</div>
                  </div>
                  <span class="acw-type-check" v-if="form.action_type === t.v" v-html="BL.icon('check', 12, '#fff')"></span>
                </div>
              </div>

              <div class="sec">{{ subjectLabel }}</div>
              <div class="acw-subject">
                <template v-if="form.m_type === 1">
                  <!-- 整行即选择器: 不再是下拉 + 按钮两个入口 -->
                  <div class="acw-pick" :class="{ 'is-empty': !selectedClass }" style="max-width:420px"
                       :title="selectedClass ? '点击更换对象类' : '点击从对象类型库中选择'"
                       @click="classPickerOpen = true">
                    <template v-if="selectedClass">
                      <span class="acw-pick-ic" :style="{ background: selectedClass.color || '#165DFF' }"
                            v-html="BL.icon(selectedClass.icon || 'cube', 13, '#fff')"></span>
                      <span class="acw-pick-text bl-truncate">
                        {{ selectedClass.cn }}<span class="acw-pick-api bl-mono bl-muted">{{ selectedClass.api_name }}</span>
                      </span>
                      <button class="acw-pick-x" title="清除" @click.stop="clearClass" v-html="BL.icon('x', 11)"></button>
                    </template>
                    <span v-else class="acw-pick-text is-ph">点击选择对象类</span>
                    <span class="acw-pick-act"><span v-html="BL.icon('search', 12)"></span>{{ selectedClass ? '更换' : '选择' }}</span>
                  </div>
                </template>
                <template v-else-if="form.m_type === 2">
                  <div class="acw-pick" :class="{ 'is-empty': !selectedLink }" style="max-width:420px"
                       :title="selectedLink ? '点击更换链接类型' : '点击从链接类型库中选择'"
                       @click="linkPickerOpen = true">
                    <template v-if="selectedLink">
                      <span class="acw-pick-ic" style="background:#722ED1" v-html="BL.icon('link', 13, '#fff')"></span>
                      <span class="acw-pick-text bl-truncate">
                        {{ selectedLink.cn }}<span class="acw-pick-api bl-mono bl-muted">{{ selectedLink.ends }}</span>
                      </span>
                      <button class="acw-pick-x" title="清除" @click.stop="form.link_type_id = ''" v-html="BL.icon('x', 11)"></button>
                    </template>
                    <span v-else class="acw-pick-text is-ph">点击选择链接类型</span>
                    <span class="acw-pick-act"><span v-html="BL.icon('search', 12)"></span>{{ selectedLink ? '更换' : '选择' }}</span>
                  </div>
                </template>
                <template v-else-if="form.m_type === 3">
                  <input class="bl-input bl-mono" v-model="form.function_code" placeholder="函数编码 function_code" style="max-width:420px" />
                </template>
                <div v-else class="bl-muted" style="font-size:12px">
                  {{ M_TYPES[form.m_type].label }} 类动作的目标配置将在编辑详情页完善,当前先创建动作骨架。
                </div>
              </div>
            </template>

            <!-- ========== 步骤 2: 对象属性与参数映射 ========== -->
            <template v-else-if="step === 2">
              <div class="acw-infobar">
                <span>当前对象类型 <b>{{ subjectSummary }}</b></span>
                <span class="acw-info-sep"></span>
                <span>动作子类型 <b>{{ currentTypeLabel }}</b></span>
                <template v-if="form.m_type === 1">
                  <span class="acw-info-sep"></span>
                  <span>属性总数 <b>{{ mapRows.length }}</b> 项</span>
                </template>
              </div>

              <!-- 对象动作: 属性映射矩阵 -->
              <template v-if="form.m_type === 1">
                <div v-if="!form.object_class_id" class="bl-empty" style="padding:32px;font-size:12px">请先在上一步选择对象类</div>
                <div v-else-if="mapLoading" class="bl-muted" style="padding:20px;font-size:12px">加载对象属性…</div>
                <div v-else-if="!mapRows.length" class="bl-empty" style="padding:32px;font-size:12px">该对象类暂无属性</div>
                <div v-else class="acw-table-wrap">
                  <table class="bl-table acw-map-table">
                    <colgroup>
                      <col style="width:38px" /><col style="width:170px" /><col style="width:72px" />
                      <col style="width:150px" /><col style="width:112px" /><col style="width:118px" /><col style="width:120px" /><col style="min-width:110px" />
                      <col style="width:76px" /><col style="width:150px" />
                    </colgroup>
                    <thead>
                      <tr>
                        <th class="t-left">序号</th><th class="t-left">属性</th><th class="t-left">数据类型</th>
                        <th class="t-center">表单展示</th><th class="t-left">赋值方式</th>
                        <th class="t-left">参数名称</th><th class="t-left">参数代码</th><th class="t-left">对象及属性</th>
                        <th class="t-left">默认值类型</th><th class="t-left">默认值配置</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(r, i) in mapRows" :key="r.property_code" :class="{ 'is-skip': r.show === 'none' }">
                        <td class="bl-muted">{{ i + 1 }}</td>
                        <td>
                          <div class="acw-prop-cell">
                            <div class="acw-prop-name">{{ r.property_name }}<span v-if="r.is_primary" class="acw-pk" title="主键">主键</span></div>
                            <div class="acw-prop-code bl-mono bl-muted">{{ r.property_code }}</div>
                          </div>
                        </td>
                        <td><span class="bl-tag acw-dt-tag">{{ dataTypeLabel(r.data_type) }}</span></td>
                        <td class="t-center">
                          <div class="acw-seg">
                            <button v-for="s in SHOW_MODES" :key="s.v" type="button" :class="['acw-seg-b', r.show === s.v && 'is-on']"
                                    :title="s.desc" @click="r.show = s.v">{{ s.label }}</button>
                          </div>
                        </td>
                        <td><BlSelect v-model="r.value_source" :options="VALUE_SOURCE_OPTS" size="sm" :disabled="r.show === 'none'" @change="onValueSourceChange(r)" /></td>
                        <td><input class="bl-input bl-input-xs" v-model="r.param_name" :disabled="!cellOn(r, 'param')" :placeholder="r.property_name" /></td>
                        <td><input class="bl-input bl-input-xs bl-mono" v-model="r.param_code" :disabled="!cellOn(r, 'param')" placeholder="参数代码" /></td>
                        <td>
                          <div class="acw-ref-box" :class="{ 'is-disabled': !cellOn(r, 'ref') }" :title="r.ref_prop || ''"
                               @click="cellOn(r, 'ref') && openRefPicker(r, 'ref')">
                            <span class="acw-ref-text" :class="{ 'is-ph': !r.ref_prop }">{{ r.ref_prop || (cellOn(r, 'ref') ? '点击选择' : '不适用') }}</span>
                            <span class="acw-ref-ic" v-html="BL.icon('search', 12)"></span>
                          </div>
                        </td>
                        <td><BlSelect v-model="r.default_type" :options="DEFAULT_TYPE_OPTS" size="sm" :disabled="!cellOn(r, 'dtype')" @change="r.default_source = ''" /></td>
                        <td>
                          <!-- 系统时间 / 当前用户: 类型锁定为静态, 配置由赋值方式决定 -->
                          <BlSelect v-if="r.value_source === 3 && r.show !== 'none'" v-model="r.default_custom" :options="USER_ATTR_OPTS" size="sm" placeholder="选择用户属性" />
                          <div v-else-if="r.value_source === 4 && r.show !== 'none'" class="acw-locked">当前系统时间</div>
                          <div v-else-if="!cellOn(r, 'dval')" class="acw-locked is-off">不适用</div>
                          <!-- 静态值 + 来源: 点选枚举等候选值, 存 JSON -->
                          <div v-else-if="r.default_type === 'source'" class="acw-ref-box" :title="r.default_source || ''" @click="openSourcePicker(r)">
                            <span class="acw-ref-text" :class="{ 'is-ph': !r.default_source }">{{ sourceLabel(r) || '点击选择' }}</span>
                            <span class="acw-ref-ic" v-html="BL.icon('search', 12)"></span>
                          </div>
                          <input v-else class="bl-input bl-input-xs" v-model="r.default_custom" placeholder="静态值" />
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </template>

              <!-- 非对象动作: 手动表单参数 -->
              <template v-else>
                <div class="acw-row-hd">
                  <div class="acw-hd-title">表单参数 <span class="bl-muted" style="font-size:11.5px">(动作执行时的输入表单字段)</span></div>
                  <button class="bl-btn bl-btn-sm bl-btn-primary" @click="addParam"><span v-html="BL.icon('plus', 12, '#fff')"></span><span style="margin-left:4px">添加参数</span></button>
                </div>
                <div class="acw-table-wrap">
                  <table class="bl-table acw-table">
                    <colgroup><col style="width:180px" /><col style="width:180px" /><col style="width:118px" /><col style="width:58px" /><col style="width:150px" /><col /><col style="width:52px" /></colgroup>
                    <thead><tr><th class="t-left">参数编码</th><th class="t-left">参数名称</th><th class="t-left">类型</th><th class="t-center">必填</th><th class="t-left">值来源</th><th class="t-left">默认值 / 写入属性</th><th class="t-center">操作</th></tr></thead>
                    <tbody>
                      <tr v-for="(p, i) in form.params" :key="i">
                        <td><input class="bl-input bl-input-xs bl-mono" v-model="p.param_code" placeholder="param_code" /></td>
                        <td><input class="bl-input bl-input-xs" v-model="p.param_name" placeholder="参数名称" /></td>
                        <td><BlSelect v-model="p.param_type" :options="PARAM_TYPE_OPTS" size="sm" /></td>
                        <td class="t-center"><input type="checkbox" v-model="p.is_required" :true-value="1" :false-value="0" /></td>
                        <td><BlSelect v-model="p.value_source" :options="VALUE_SOURCE_OPTS" size="sm" /></td>
                        <td><input class="bl-input bl-input-xs" v-model="p.default_value" placeholder="默认值 (可空)" /></td>
                        <td class="t-center"><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="移除" @click="form.params.splice(i, 1)" v-html="BL.icon('trash', 12)"></button></td>
                      </tr>
                    </tbody>
                  </table>
                  <div v-if="!form.params.length" class="bl-empty" style="padding:36px;font-size:12px">暂无参数。点击「添加参数」新增。</div>
                </div>
              </template>
            </template>

            <!-- ========== 步骤 3: 元数据 ========== -->
            <template v-else-if="step === 3">
              <div class="acw-sec3">基础信息</div>
              <div class="acw-m3-grid">
                <div class="acw-m3-fld"><span class="acw-m3-lbl">动作名称 <i>*</i></span><input class="bl-input" v-model="form.rdfs_label" placeholder="如: 新建监测样地" /></div>
                <div class="acw-m3-fld"><span class="acw-m3-lbl">API 编码 <i>*</i></span><input class="bl-input bl-mono" v-model="form.api_name" placeholder="create_plot" /></div>
                <div class="acw-m3-fld"><span class="acw-m3-lbl">按钮文案</span><input class="bl-input" v-model="form.button_text" placeholder="展示在详情/批量操作栏" /></div>
                <div class="acw-m3-fld"><span class="acw-m3-lbl">本体类 ID</span><input class="bl-input bl-mono" :value="subjectApi" disabled placeholder="—" /></div>
              </div>

              <div class="acw-sec3">展示配置</div>
              <div class="acw-m3-grid">
                <div class="acw-m3-fld acw-m3-inline"><span class="acw-m3-lbl">详情页展示</span>
                  <div class="acw-tg"><span class="acw-showsw" :class="{ 'is-on': form.show_on_detail === 1 }" @click="form.show_on_detail = form.show_on_detail === 1 ? 0 : 1"><span class="acw-showsw-dot"></span></span><span class="acw-tg-lbl">{{ form.show_on_detail === 1 ? '显示' : '隐藏' }}</span></div>
                </div>
                <div class="acw-m3-fld acw-m3-inline"><span class="acw-m3-lbl">批量操作展示</span>
                  <div class="acw-tg"><span class="acw-showsw" :class="{ 'is-on': form.show_on_batch === 1 }" @click="form.show_on_batch = form.show_on_batch === 1 ? 0 : 1"><span class="acw-showsw-dot"></span></span><span class="acw-tg-lbl">{{ form.show_on_batch === 1 ? '显示' : '隐藏' }}</span></div>
                </div>
              </div>
              <div class="acw-m3-grid">
                <div class="acw-m3-fld acw-pick"><span class="acw-m3-lbl">按钮颜色</span>
                  <ColorPickerField v-model="form.color" :palette="COMPACT_COLORS" />
                </div>
                <div class="acw-m3-fld acw-pick"><span class="acw-m3-lbl">按钮图标</span>
                  <IconPickerField v-model="form.icon" label="" :preset-count="7" :suggest-name="form.rdfs_label || form.api_name" />
                </div>
              </div>

              <div class="acw-sec3">业务说明</div>
              <div class="acw-m3-fld acw-m3-full"><span class="acw-m3-lbl">描述</span><textarea class="bl-textarea" v-model="form.rdfs_comment" rows="4" placeholder="用于说明该动作的用途与行为 (rdfs:comment)"></textarea></div>
              <div class="acw-m3-grid">
                <div class="acw-m3-fld"><span class="acw-m3-lbl">参考资料</span><input class="bl-input" v-model="form.rdfs_see_also" placeholder="请输入参考文档、规范链接" /></div>
                <div class="acw-m3-fld"><span class="acw-m3-lbl">定义来源</span><input class="bl-input" v-model="form.rdfs_defined_by" placeholder="如: 水利公共本体库" /></div>
              </div>

              <div class="acw-sec3">状态信息</div>
              <div class="acw-m3-grid">
                <div class="acw-m3-fld"><span class="acw-m3-lbl">动作状态</span><input class="bl-input" :value="statusLabel(form.status)" disabled /></div>
                <div class="acw-m3-fld"><span class="acw-m3-lbl">编译状态</span><input class="bl-input" value="未编译" disabled /></div>
              </div>
            </template>

            <!-- ========== 步骤 4: 提交校验标准 ========== -->
            <template v-else-if="step === 4">
              <div class="acw-sec3" style="font-size:14px; font-weight:700; border-bottom:0; margin-bottom:8px">执行规则</div>
              <ConditionGroup :node="submitTree" :depth="0" :object-fields="objectFields" :param-fields="paramFieldsForCond"
                              :subjects="['object', 'user', 'usergroup']" :subject-labels="{ object: subjectName }" />
              <div class="acw-m3-fld acw-m3-full" style="margin-top:18px">
                <span class="acw-m3-lbl">失败提示消息</span>
                <textarea class="bl-textarea" v-model="form.submit_error_message" rows="3" placeholder="校验不通过时向用户展示的提示,如: 您没有权限执行此操作,或提交数据不符合业务规则,请检查后重试。"></textarea>
              </div>
              <div class="acw-hint">仅当所有根级规则全部满足时,动作才可提交。留空则不启用提交校验。</div>
            </template>

            <!-- ========== 步骤 5: 保存位置 ========== -->
            <template v-else-if="step === 5">
              <div class="acw-sec3">存储位置</div>
              <div class="acw-m3-fld acw-m3-full">
                <span class="acw-m3-lbl">存储目录</span>
                <SearchSelect v-model="form.category_code" :options="storageDirOptions" placeholder="选择存储目录 (行业领域)" search-placeholder="搜索领域" />
              </div>
              <div class="acw-m3-fld acw-m3-full">
                <span class="acw-m3-lbl">存储路径 <i>*</i></span>
                <div class="acw-path-box">
                  <div class="bl-mono">完整路径:{{ storagePath }}</div>
                </div>
              </div>

              <div class="acw-sec3">资源信息确认</div>
              <div class="acw-m3-grid">
                <div class="acw-m3-fld"><span class="acw-m3-lbl">动作名称</span><input class="bl-input" v-model="form.rdfs_label" /></div>
                <div class="acw-m3-fld"><span class="acw-m3-lbl">资源 ID</span><input class="bl-input bl-mono" :value="form.api_name || '(由动作编码生成)'" disabled /></div>
              </div>

              <div class="acw-sec3">权限设置</div>
              <div class="acw-m3-grid">
                <div class="acw-m3-fld acw-m3-inline"><span class="acw-m3-lbl">继承父目录权限</span>
                  <div class="acw-tg"><span class="acw-showsw" :class="{ 'is-on': form.inherit_permission === 1 }" @click="form.inherit_permission = form.inherit_permission === 1 ? 0 : 1"><span class="acw-showsw-dot"></span></span><span class="acw-tg-lbl">{{ form.inherit_permission === 1 ? '开启' : '关闭' }}</span></div>
                </div>
                <div class="acw-m3-fld"><span class="acw-m3-lbl">可见范围</span>
                  <BlSelect v-model="form.visibility" :options="VISIBILITY_OPTS" />
                </div>
              </div>

              <div class="acw-sec3">保存设置</div>
              <div class="acw-m3-fld acw-m3-full">
                <span class="acw-m3-lbl">保存模式</span>
                <BlSelect v-model="form.save_mode" :options="SAVE_MODE_OPTS" />
              </div>
            </template>
              </div>
            </div>
          </div>

          <!-- 底部 -->
          <div class="wz-ft">
            <button class="bl-btn" @click="onCancel">取消</button>
            <span style="flex:1"></span>
            <button v-if="step > 1" class="bl-btn" @click="step--">
              <span v-html="BL.icon('chevronLeft', 12)"></span><span style="margin-left:2px">上一步</span>
            </button>
            <button v-if="step < 5" class="bl-btn bl-btn-primary" @click="goNext">
              下一步<span v-html="BL.icon('chevronRight', 12, '#fff')" style="margin-left:2px"></span>
            </button>
            <button v-else class="bl-btn bl-btn-primary" :disabled="saving" @click="onFinish">
              <span v-html="BL.icon('check', 12, '#fff')"></span><span style="margin-left:4px">{{ saving ? '创建中…' : '完成创建' }}</span>
            </button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>

  <!-- 关联对象类: 带领域树 / 属性数的卡片式挑选 -->
  <ObjectTypePickerModal v-model:open="classPickerOpen" :multi="false" required
                         :model-value="form.object_class_id ? [form.object_class_id] : []"
                         subtitle="选择本动作要操作的对象类" @confirm="onClassPicked" />

  <!-- 关联链接类型: 与对象类同一套挑选形态 -->
  <LinkTypePickerModal v-model:open="linkPickerOpen" :multi="false" required
                       :model-value="form.link_type_id ? [form.link_type_id] : []"
                       subtitle="选择本动作要操作的链接类型" @confirm="onLinkPicked" />

  <ObjectRefPickerModal v-model:open="refPickerOpen"
                        :source-class-id="form.object_class_id"
                        :all-classes="allClasses"
                        :all-link-types="allLinkTypes"
                        @pick="onRefPicked" />

  <!-- 枚举属性的「来源」= 限定该动作可选的枚举值范围 -->
  <EnumValuePickerModal v-model:open="enumPickerOpen"
                        :enum-id="enumPickerRow?.enum_id || ''"
                        :subtitle="enumPickerSubtitle"
                        :model-value="parseSourceList(enumPickerRow)"
                        @confirm="onEnumValuesPicked" />
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { actionTypeApi, resourceApi, categoryApi } from '@/api'
import FieldRow from '@/views/config/category/FieldRow.vue'
import SearchSelect from '@/components/SearchSelect.vue'
import BlSelect from '@/components/BlSelect.vue'
import ObjectTypePickerModal from '@/components/ObjectTypePickerModal.vue'
import LinkTypePickerModal from '@/components/LinkTypePickerModal.vue'
import ObjectRefPickerModal from './ObjectRefPickerModal.vue'
import EnumValuePickerModal from '@/components/EnumValuePickerModal.vue'
import ConditionGroup from './ConditionGroup.vue'
import { VALUE_SOURCE_OPTS as ALL_VALUE_SOURCE_OPTS } from './funcParamModel.js'
import { USER_ATTR_OPTS } from './ruleModel.js'
/* 「主对象」「本动作创建的对象」只在规则的属性映射里可用, 向导不提供 */
const VALUE_SOURCE_OPTS = ALL_VALUE_SOURCE_OPTS.filter(o => o.value !== 6 && o.value !== 7)
import IconPickerField from '@/components/IconPickerField.vue'
import ColorPickerField from '@/components/ColorPickerField.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  allClasses: { type: Array, default: () => [] },
  allLinkTypes: { type: Array, default: () => [] },
  initCategory: { type: String, default: '' },
})
const emit = defineEmits(['update:open', 'created'])

const STEPS = ['动作类型选型', '参数映射', '元数据', '提交标准', '保存位置']
const STEP_META = [
  { title: '选择要配置的动作类型', sub: '通过配置可执行的动作,让用户能够对本体数据进行标准化修改。' },
  { title: '配置对象属性与参数映射', sub: '将本体对象的属性与动作表单参数进行绑定,配置字段展示规则、赋值方式与默认值逻辑。' },
  { title: '配置动作元数据信息', sub: '配置动作的基础展示信息、业务说明与交互属性,决定动作在前端的展示形态与使用规则。' },
  { title: '配置提交标准', sub: '动作提交前的校验能力开关(详细条件在编辑详情页配置)。' },
  { title: '配置保存位置', sub: '选择动作类型的存储目录,确定资源归属与权限继承范围,保存后正式生成动作类型资源。' },
]
const M_TYPES = {
  1: { label: '对象', icon: 'box', color: '#165DFF' },
  2: { label: '链接', icon: 'link', color: '#14C9C9' },
  3: { label: '函数', icon: 'code', color: '#722ED1' },
  4: { label: 'Webhook', icon: 'zap', color: '#FF7D00' },
  5: { label: '接口', icon: 'plug', color: '#0FC6C2' },
  6: { label: '通知', icon: 'bell', color: '#B71DE8' },
}
const ACTION_TYPES = {
  1: [
    { v: 11, label: '创建对象', color: '#00B42A', icon: 'plus', desc: '新建一个对象实例', prefix: 'create' },
    { v: 12, label: '修改对象', color: '#165DFF', icon: 'edit', desc: '修改已有对象的属性', prefix: 'update' },
    { v: 13, label: 'Upsert 对象', color: '#165DFF', icon: 'edit', desc: '存在则更新, 否则创建', prefix: 'upsert' },
    { v: 14, label: '删除对象', color: '#F53F3F', icon: 'trash', desc: '删除一个对象实例', prefix: 'delete' },
  ],
  2: [
    { v: 21, label: '创建链接', color: '#14C9C9', icon: 'link', desc: '在两个对象间建立链接', prefix: 'link' },
    { v: 22, label: '删除链接', color: '#F53F3F', icon: 'link', desc: '解除两个对象间的链接', prefix: 'unlink' },
  ],
  3: [{ v: 30, label: '函数动作', color: '#722ED1', icon: 'code', desc: '调用已注册的函数', prefix: 'fn' }],
  4: [{ v: 40, label: 'Webhook', color: '#FF7D00', icon: 'zap', desc: '触发外部 HTTP 回调', prefix: 'webhook' }],
  5: [
    { v: 51, label: '接口·创建', color: '#0FC6C2', icon: 'plug', desc: '调用创建型接口', prefix: 'api_create' },
    { v: 52, label: '接口·修改', color: '#0FC6C2', icon: 'plug', desc: '调用修改型接口', prefix: 'api_update' },
    { v: 53, label: '接口·删除', color: '#0FC6C2', icon: 'plug', desc: '调用删除型接口', prefix: 'api_delete' },
    { v: 54, label: '接口·查询', color: '#0FC6C2', icon: 'plug', desc: '调用查询型接口', prefix: 'api_query' },
  ],
  6: [{ v: 60, label: '通知动作', color: '#B71DE8', icon: 'bell', desc: '发送站内/邮件/短信通知', prefix: 'notify' }],
}
const PARAM_TYPE_LABELS = { string:'字符串', number:'数值', boolean:'布尔', enum:'枚举', object:'对象引用', date:'日期' }
const PARAM_TYPES = ['string', 'number', 'boolean', 'enum', 'object', 'date']
const PARAM_TYPE_OPTS = PARAM_TYPES.map(t => ({ value: t, label: `${PARAM_TYPE_LABELS[t]} (${t})` }))
const DEFAULT_TYPE_OPTS = [{ value: 'static', label: '静态' }, { value: 'source', label: '来源' }]
/* 表单展示三态: 无 = 该属性完全不参与本动作(不生成参数, 也不生成属性映射) */
const SHOW_MODES = [
  { v: 'show', label: '展示', desc: '表单中可见可填' },
  { v: 'hidden', label: '隐藏', desc: '生成参数但不渲染, 由赋值方式静默写入' },
  { v: 'none', label: '无', desc: '该属性不参与本动作' },
]

/* 各单元格是否可编辑 — 由「表单展示 + 赋值方式」共同决定 */
function cellOn(r, cell) {
  if (r.show === 'none') return false
  const vs = Number(r.value_source)
  if (cell === 'param') return vs === 1
  if (cell === 'ref') return vs === 5
  if (cell === 'dtype') return vs === 2          // 仅「静态值」可切静态/来源
  if (cell === 'dval') return vs === 2 || vs === 3 || vs === 4
  return false
}
/* 切换赋值方式时把后续列对齐到该模式的固定取值, 避免留下上一模式的残值 */
function onValueSourceChange(r) {
  const vs = Number(r.value_source)
  /* 参数名称/代码始终保留默认命名, 切回「表单参数」时不用重新想名字 */
  if (!r.param_code) r.param_code = defaultParamCode(r.property_code)
  if (!r.param_name) r.param_name = r.property_name
  if (vs !== 5) r.ref_prop = ''
  if (vs === 3) { r.default_type = 'static'; r.default_source = ''; if (!USER_ATTR_OPTS.some(o => o.value === r.default_custom)) r.default_custom = 'user_id' }
  else if (vs === 4) { r.default_type = 'static'; r.default_source = ''; r.default_custom = '' }
  else if (vs === 2) { r.default_type = r.default_type || 'static' }
  else { r.default_type = 'static'; r.default_source = ''; r.default_custom = '' }
}
function defaultParamCode(code) { return 'p_' + String(code || '').replace(/^p_/, '') }
/* default_source 两种形态: 关联对象属性(纯文本) | 枚举候选范围 JSON [{code,label}] */
function parseSourceList(r) {
  const raw = String(r?.default_source || '')
  if (!raw.startsWith('[')) return []
  try {
    const a = JSON.parse(raw)
    return Array.isArray(a) ? a.map(x => (x && typeof x === 'object') ? x : { code: String(x), label: String(x) }) : []
  } catch { return [] }
}
function sourceLabel(r) {
  const raw = String(r.default_source || '')
  if (!raw.startsWith('[')) return raw
  const a = parseSourceList(r)
  if (!a.length) return raw
  const first = a[0].label || a[0].code
  return a.length > 1 ? `${first} 等 ${a.length} 项` : first
}
const VISIBILITY_OPTS = [{ value: 'project', label: '项目内所有成员可见' }, { value: 'creator', label: '仅创建者可见' }, { value: 'assigned', label: '指定成员可见' }]
const SAVE_MODE_OPTS = [{ value: 'compile', label: '保存并编译校验' }, { value: 'draft', label: '仅保存草稿' }]
const COMPACT_COLORS = ['#165DFF', '#00B42A', '#722ED1', '#FF7D00', '#EB2F96', '#13C2C2', '#FADB14', '#F53F3F']

function defaultForm() {
  return {
    m_type: 1, action_type: 11,
    object_class_id: '', link_type_id: '', function_code: '',
    api_name: '', rdfs_label: '', button_text: '', icon: 'zap', color: '#165DFF',
    category_code: '', rdfs_comment: '', rdfs_see_also: '', rdfs_defined_by: '', save_path: '', submit_error_message: '',
    inherit_permission: 1, visibility: 'project', save_mode: 'compile',
    show_on_detail: 0, show_on_batch: 0, form_enabled: 0, submit_criteria_enabled: 0,
    status: 0, params: [],
  }
}
const form = reactive(defaultForm())
const step = ref(1)
const saving = ref(false)

/* —— 对象属性映射矩阵 (步骤2, 对象动作) —— */
const mapRows = ref([])
const mapLoading = ref(false)
const mapLoadedClass = ref('')

/* —— 提交校验条件树 (步骤4) —— */
const submitTree = reactive({ logic: 'all', children: [] })
const objectFields = computed(() => mapRows.value.map(r => ({ code: r.property_code, name: r.property_name, dataType: r.data_type })))
const paramFieldsForCond = computed(() => {
  /* show 是 'show'/'hidden'/'none' 三态, 与出参一样「无」之外都会生成参数 */
  if (form.m_type === 1) return mapRows.value.filter(r => r.show !== 'none').map(r => ({ code: r.param_code || r.property_code, name: r.param_name || r.property_name, dataType: r.data_type }))
  return (form.params || []).map(p => ({ code: p.param_code, name: p.param_name || p.param_code, dataType: p.param_type }))
})
/* 递归拍平条件树 → 后端节点数组 (含 parent_id 引用) */
function flattenSubmitTree() {
  const out = []
  let n = 0
  const walk = (node, parentId, sort) => {
    const id = 'cn-' + (++n)
    const isGroup = node.type !== 'cond'
    out.push({
      id, parent_id: parentId, sort,
      node_type: isGroup ? 'group' : 'condition',
      logic_op: isGroup ? (node.logic || 'all') : null,
      left_code: isGroup ? null : `${node.subject}:${node.field || ''}`,
      operator: isGroup ? null : (node.operator || null),
      right_value: isGroup ? null : (node.value || null),
      value_source: isGroup ? null : (node.subject === 'user' ? 3 : null),
    })
    if (isGroup && node.children) node.children.forEach((c, i) => walk(c, id, i))
  }
  walk(submitTree, null, 0)
  return out
}
async function loadMapRows() {
  if (form.m_type !== 1 || !form.object_class_id) { mapRows.value = []; return }
  if (mapLoadedClass.value === form.object_class_id && mapRows.value.length) return
  mapLoading.value = true
  try {
    const list = await resourceApi.properties(form.object_class_id).catch(() => [])
    const arr = Array.isArray(list) ? list : (list?.data || [])
    mapRows.value = arr.map(p => {
      const code = p.api_name || p.prop_code || ''
      const lc = String(code).toLowerCase()
      /* 审计类字段默认由系统写入并隐藏, 不必让用户填 */
      let vs = 1, show = 'show', dcustom = ''
      if (/(create_?time|created_?at|update_?time|updated_?at)/.test(lc)) { vs = 4; show = 'hidden' }
      else if (/(creator|created_?by|create_?user|owner)/.test(lc)) { vs = 3; show = 'hidden'; dcustom = 'user_id' }
      const pname = p.display_name || p.rdfs_label || code
      return { property_code: code, property_name: pname, data_type: p.data_type || '',
               comment: p.rdfs_comment || '',      // 带到参数描述, 免得再手抄一遍属性语义
               required: p.is_required ? 1 : 0, is_primary: Number(p.is_primary ?? p.isPrimary ?? 0) === 1,
               /* 值类型是 Enum 约束时带上枚举 ID, 「来源」才能弹出该枚举的候选值 */
               enum_id: p.enum_id || p.enumId || '', enum_label: p.enum_label || p.enum_api_name || '',
               show, value_source: vs, param_name: pname, param_code: defaultParamCode(code), ref_prop: '',
               default_type: 'static', default_custom: dcustom, default_source: '' }
    })
    mapLoadedClass.value = form.object_class_id
  } finally { mapLoading.value = false }
}
watch(step, s => { if (s === 2) loadMapRows() })
function dataTypeLabel(dt) {
  const s = String(dt || '').toLowerCase()
  if (!s) return '—'
  if (/(int|decimal|double|float|number|numeric)/.test(s)) return '数值'
  if (s.includes('bool')) return '布尔'
  if (s.includes('datetime') || s.includes('timestamp')) return '日期时间'
  if (s.includes('date')) return '日期'
  if (s.includes('time')) return '时间'
  if (s.includes('enum')) return '枚举'
  if (s.includes('object') || s.includes('ref')) return '对象引用'
  if (s.includes('text') || s.includes('clob')) return '长文本'
  return '字符串'
}
function defaultPlaceholder(r) {
  if (r.value_source === 3) return '当前登录用户'
  if (r.value_source === 4) return '当前系统时间'
  return '无默认值'
}

/* 关联对象属性选择器 */
const refPickerOpen = ref(false)
const refPickerRow = ref(null)
const refPickerTarget = ref('ref')   // 'ref' = 赋值方式关联对象; 'default' = 默认值来源
function openRefPicker(r, target) {
  if (!form.object_class_id) return BL.warning('请先选择对象类')
  refPickerRow.value = r
  refPickerTarget.value = target
  refPickerOpen.value = true
}
/* 枚举候选范围选择器 */
const enumPickerOpen = ref(false)
const enumPickerRow = ref(null)
const enumPickerSubtitle = computed(() => {
  const r = enumPickerRow.value
  if (!r) return ''
  return `属性 ${r.property_name}(${r.property_code}) — 勾选的值即本动作可选范围`
})
/* 枚举属性挑候选枚举值; 库里多数枚举属性没绑值类型, 故 enum_id 允许为空, 由弹框内现场指定 */
function isEnumProp(r) { return !!r.enum_id || String(r.data_type || '').toLowerCase().includes('enum') }
/* 「静态值 + 来源」: 枚举属性挑候选枚举值, 其余仍走关联对象属性 */
function openSourcePicker(r) {
  if (isEnumProp(r)) { enumPickerRow.value = r; enumPickerOpen.value = true; return }
  openRefPicker(r, 'default')
}
function onEnumValuesPicked({ enum_id, enum_label, values }) {
  const r = enumPickerRow.value
  if (!r) return
  r.enum_id = enum_id || r.enum_id
  r.enum_label = enum_label || r.enum_label
  r.default_source = (values && values.length) ? JSON.stringify(values) : ''
}
function onRefPicked(ref) {
  const r = refPickerRow.value
  if (!r) return
  if (refPickerTarget.value === 'default') r.default_source = ref.display
  else r.ref_prop = ref.display
}
/* 枚举候选范围 (仅枚举属性 + 默认值类型=来源 时有值) */
function enumRangeOf(r) {
  if (!isEnumProp(r) || r.default_type !== 'source') return null
  const list = parseSourceList(r)
  return list.length ? list : null
}
/* 默认值配置 → 落库值 (静态: 无/系统时间/登录用户/自定义; 来源: 关联对象.属性 或 枚举范围 JSON) */
function computeDefault(r) {
  const vs = Number(r.value_source)
  if (vs === 4) return 'CURRENT_TIME'
  if (vs === 3) return 'CURRENT_USER' + (r.default_custom ? '.' + r.default_custom : '')
  if (vs === 2) return r.default_type === 'source' ? (r.default_source || null) : (r.default_custom || null)
  return null
}

const currentActionTypes = computed(() => ACTION_TYPES[form.m_type] || [])
const classOptions = computed(() => (props.allClasses || []).map(c => ({
  id: c.id, cn: c.display_name || c.rdfs_label || c.api_name, api_name: c.api_name, category_code: c.category_code
})))
const subjectLabel = computed(() => ({ 1: '关联对象类', 2: '关联链接类型', 3: '关联函数' })[form.m_type] || '目标配置')
const currentTypeMeta = computed(() => currentActionTypes.value.find(t => t.v === form.action_type) || {})
const currentTypeLabel = computed(() => currentTypeMeta.value.label || '—')
const typeTagStyle = computed(() => {
  const c = currentTypeMeta.value.color || '#86909c'
  return { background: `color-mix(in srgb, ${c} 12%, transparent)`, color: c, border: `1px solid color-mix(in srgb, ${c} 30%, transparent)` }
})
const subjectSummary = computed(() => {
  if (form.m_type === 1) { const c = classOptions.value.find(x => x.id === form.object_class_id); return c ? `${c.cn} (${c.api_name})` : '—' }
  if (form.m_type === 2) { const l = (props.allLinkTypes || []).find(x => x.id === form.link_type_id); return l ? (l.rdfs_label || l.link_type_id) : '—' }
  if (form.m_type === 3) return form.function_code || '—'
  return '—'
})
/* 条件左栏第一项显示所选对象类 / 链接类型本身的名字, 而不是笼统的「对象」 */
const subjectName = computed(() => {
  if (form.m_type === 1) { const c = classOptions.value.find(x => x.id === form.object_class_id); return c ? c.cn : '对象' }
  if (form.m_type === 2) { const l = (props.allLinkTypes || []).find(x => x.id === form.link_type_id); return l ? (l.rdfs_label || l.link_type_id) : '链接' }
  return '对象'
})
const subjectApi = computed(() => {
  if (form.m_type === 1) { const c = classOptions.value.find(x => x.id === form.object_class_id); return c ? c.api_name : '' }
  if (form.m_type === 2) { const l = (props.allLinkTypes || []).find(x => x.id === form.link_type_id); return l ? (l.link_type_id || '') : '' }
  if (form.m_type === 3) return form.function_code || ''
  return ''
})
function statusLabel(s) { return ({ 0: '草稿', 1: '已发布', 2: '已停用' })[Number(s)] || '草稿' }

function pickMType(k) {
  form.m_type = k
  const opts = ACTION_TYPES[k] || []
  if (!opts.find(t => t.v === form.action_type)) pickActionType(opts[0])
  form.color = M_TYPES[k].color
  // 清理非当前大类主体
  if (k !== 1) form.object_class_id = ''
  if (k !== 2) form.link_type_id = ''
  if (k !== 3) form.function_code = ''
}
function pickActionType(t) {
  if (!t) return
  form.action_type = t.v
  form.color = t.color
  form.icon = t.icon
  suggestNaming()
}
const classPickerOpen = ref(false)
/* 图标/配色取自对象类自身, 让这里和对象类型列表页认知一致 */
const selectedClass = computed(() => {
  if (!form.object_class_id) return null
  const c = (props.allClasses || []).find(x => x.id === form.object_class_id)
  if (!c) return null
  return { cn: c.display_name || c.rdfs_label || c.api_name, api_name: c.api_name, icon: c.icon, color: c.color }
})
function onClassPicked({ ids }) {
  if (!ids?.length) return
  form.object_class_id = ids[0]
  onObjectClassChange()
}
function clearClass() {
  form.object_class_id = ''
  mapLoadedClass.value = ''
  mapRows.value = []
}

const linkPickerOpen = ref(false)
const selectedLink = computed(() => {
  if (!form.link_type_id) return null
  const l = (props.allLinkTypes || []).find(x => x.id === form.link_type_id)
  if (!l) return null
  return { cn: l.rdfs_label || l.link_type_id, ends: `${l.l_class_name || ''} → ${l.r_class_name || ''}` }
})
function onLinkPicked({ ids }) {
  if (!ids?.length) return
  form.link_type_id = ids[0]
  suggestNaming()
}
function onObjectClassChange() {
  const c = classOptions.value.find(x => x.id === form.object_class_id)
  if (c && c.category_code && !form.category_code) form.category_code = c.category_code
  mapLoadedClass.value = ''   // 换对象类, 步骤2 重新加载属性
  suggestNaming()
}
/* 依据 动作类型 + 主体 建议 编码/名称 */
function suggestNaming() {
  const t = currentTypeMeta.value
  if (!t) return
  let base = ''
  if (form.m_type === 1) { const c = classOptions.value.find(x => x.id === form.object_class_id); base = c?.api_name || '' }
  else if (form.m_type === 2) { const l = (props.allLinkTypes || []).find(x => x.id === form.link_type_id); base = (l?.link_type_id || '').replace(/-/g, '_') }
  const snake = base ? base.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase() : ''
  if (!form._apiTouched) form.api_name = snake ? `${t.prefix}_${snake}` : ''
  if (!form._labelTouched) form.rdfs_label = base ? `${t.label}·${base}` : t.label
}

/* 参数 */
function addParam() {
  form.params.push({ param_code: '', param_name: '', param_type: 'string', is_required: 0, value_source: 1, default_value: '', property_code: '' })
}
async function importFromClass() {
  if (!form.object_class_id) return BL.warning('请先在步骤1选择对象类')
  const props2 = await resourceApi.properties(form.object_class_id).catch(() => [])
  const list = Array.isArray(props2) ? props2 : (props2?.data || [])
  if (!list.length) return BL.info('该对象类暂无属性')
  const exist = new Set(form.params.map(p => p.param_code))
  let n = 0
  for (const pr of list) {
    const code = pr.api_name || pr.prop_code
    if (!code || exist.has(code)) continue
    form.params.push({
      param_code: code, param_name: pr.display_name || pr.rdfs_label || code,
      param_type: mapXsdType(pr.data_type), is_required: pr.is_required ? 1 : 0,
      value_source: 1, default_value: '', property_code: code,
    })
    n++
  }
  BL.success(`已导入 ${n} 个参数`)
}
function mapXsdType(dt) {
  const s = String(dt || '').toLowerCase()
  if (s.includes('enum')) return 'enum'
  if (s.includes('int') || s.includes('decimal') || s.includes('double') || s.includes('float')) return 'number'
  if (s.includes('bool')) return 'boolean'
  if (s.includes('date') || s.includes('time')) return 'date'
  return 'string'
}

/* 领域候选 */
const domainOpts = ref([])
async function loadDomainOpts() {
  if (domainOpts.value.length) return
  const tree = await categoryApi.tree().catch(() => [])
  const list = []
  const walk = (ns, depth) => (ns || []).forEach(n => {
    if (n.categoryCode && n.categoryType === 2) list.push({ code: n.categoryCode, label: n.label || n.rdfsLabel || n.categoryCode, indent: '　'.repeat(depth) })
    if (n.children) walk(n.children, depth + 1)
  })
  walk(tree, 0)
  domainOpts.value = list
}
const storageDirOptions = computed(() => domainOpts.value.map(d => ({ value: d.code, label: (d.indent || '') + d.label })))
const storagePath = computed(() => {
  const dir = domainOpts.value.find(d => d.code === form.category_code)
  const seg = dir ? dir.label : '未分类'
  return `/ontology/actions/${seg}/${form.api_name || 'new-action'}`
})

/* 手动改过编码/名称后不再自动覆盖 */
watch(() => form.api_name, () => { if (step.value === 3) form._apiTouched = true })
watch(() => form.rdfs_label, () => { if (step.value === 3) form._labelTouched = true })

function reset() {
  Object.assign(form, defaultForm())
  form._apiTouched = false
  form._labelTouched = false
  if (props.initCategory) form.category_code = props.initCategory
  submitTree.logic = 'all'
  submitTree.children = []
  step.value = 1
  pickActionType(ACTION_TYPES[1][0])
}
watch(() => props.open, v => { if (v) { reset(); loadDomainOpts() } })

function goNext() {
  if (step.value === 1) {
    if (form.m_type === 1 && !form.object_class_id) return BL.warning('请选择关联对象类')
    if (form.m_type === 2 && !form.link_type_id) return BL.warning('请选择关联链接类型')
    if (form.m_type === 3 && !String(form.function_code).trim()) return BL.warning('请填写函数编码')
  }
  if (step.value === 3) {
    if (!String(form.rdfs_label).trim()) return BL.warning('请填写动作名称')
    if (!/^[a-z][a-z0-9_]*$/.test(form.api_name || '')) return BL.warning('动作编码需为小写字母/数字/下划线,且首字符为字母')
  }
  step.value++
}
function onCancel() { emit('update:open', false) }

async function onFinish() {
  if (!String(form.rdfs_label).trim()) { step.value = 3; return BL.warning('请填写动作名称') }
  if (!/^[a-z][a-z0-9_]*$/.test(form.api_name || '')) { step.value = 3; return BL.warning('动作编码不合法') }
  saving.value = true
  try {
    // 步骤2 映射: 对象动作用「属性映射矩阵」, 只生成 form_params; 规则留给用户在详情页按需添加
    let formParamsOut = []
    if (form.m_type === 1 && mapRows.value.length) {
      /* 「无」= 该属性完全不参与本动作: 既不生成表单参数, 也不生成属性映射 */
      const joined = mapRows.value.filter(r => r.show !== 'none')
      /* 展示 + 隐藏都建成表单参数(隐藏的 visible=0), 只有「无」不进表单 */
      formParamsOut = joined
        .filter(r => String(r.param_code).trim())
        .map((r, i) => ({
          param_code: r.param_code, param_name: r.param_name || r.property_name, param_type: mapXsdType(r.data_type),
          is_required: r.required, default_value: computeDefault(r),
          config: JSON.stringify({ value_source: Number(r.value_source), property_code: r.property_code,
            visible: r.show === 'hidden' ? 0 : 1, description: r.comment || '',
            default_type: r.default_type, default_source: r.default_source || null,
            /* 枚举「来源」= 候选范围, 单独给出便于表单渲染时裁剪下拉选项 */
            ...(enumRangeOf(r) ? { enum_id: r.enum_id, enum_options: enumRangeOf(r) } : {}) }),
          sort: i,
        }))
    } else {
      formParamsOut = form.params
        .filter(p => String(p.param_code).trim())
        .map((p, i) => ({
          param_code: p.param_code, param_name: p.param_name, param_type: p.param_type,
          is_required: p.is_required, default_value: p.value_source === 5 ? null : p.default_value,
          config: JSON.stringify({ value_source: p.value_source, property_code: p.property_code || null }), sort: i,
        }))
    }
    const body = {
      m_type: form.m_type, action_type: form.action_type,
      object_class_id: form.object_class_id || null,
      link_type_id: form.link_type_id || null,
      function_code: form.function_code || null,
      api_name: form.api_name, rdfs_label: form.rdfs_label,
      button_text: form.button_text || null, icon: form.icon || null, color: form.color || null,
      category_code: form.category_code || null, rdfs_comment: form.rdfs_comment || null,
      rdfs_see_also: form.rdfs_see_also || null, rdfs_defined_by: form.rdfs_defined_by || null,
      save_path: storagePath.value,
      metadata: JSON.stringify({ inherit_permission: form.inherit_permission, visibility: form.visibility, save_mode: form.save_mode }),
      show_on_detail: form.show_on_detail, show_on_batch: form.show_on_batch,
      form_enabled: form.form_enabled, submit_criteria_enabled: form.submit_criteria_enabled,
      status: form.status,
      form_params: formParamsOut,
    }
    // 提交校验: 有条件树或提示消息即启用
    const submitNodes = flattenSubmitTree()
    const hasSubmit = submitTree.children.length > 0 || !!String(form.submit_error_message).trim()
    if (hasSubmit) {
      body.submit_criteria_enabled = 1
      body.submit_standard = {
        enabled: 1,
        validate_mode: submitTree.logic === 'any' ? 'any' : 'all',
        error_message: form.submit_error_message || null,
        nodes: submitNodes,
      }
    }
    const created = await actionTypeApi.create(body)
    BL.success('动作已创建')
    emit('created', created?.id || '')
    emit('update:open', false)
  } catch (e) {
    BL.error(e?.msg || '创建失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.wz-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,.55);
  backdrop-filter: blur(3px); -webkit-backdrop-filter: blur(3px);
  z-index: 1200; display: flex; align-items: center; justify-content: center;
}
:root[data-theme="dark"] .wz-mask { background: rgba(0,0,0,.65); }
.wz-modal {
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 12px;
  width: 1120px; max-width: 96vw; height: calc(100vh - 120px); min-height: 480px;
  display: flex; flex-direction: column; overflow: hidden;
  box-shadow: 0 24px 56px rgba(0,0,0,0.55), 0 4px 12px rgba(0,0,0,0.3);
}
:root[data-theme="dark"] .wz-modal { border-color: var(--bl-border-strong); }

.wz-hd { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid var(--bl-divider); }
.wz-title { font-size: 16px; font-weight: 600; }
.wz-sub { font-size: 12px; }

.wz-main { flex: 1; min-height: 0; display: flex; overflow: hidden; }
.wz-side { flex: 0 0 172px; background: var(--bl-bg-2); border-right: 1px solid var(--bl-divider); padding: 16px 12px; display: flex; flex-direction: column; gap: 4px; overflow: auto; }
.wz-side-step { display: flex; align-items: center; gap: 10px; padding: 9px 10px; border-radius: 8px; font-size: 13px; color: var(--bl-text-3); }
.wz-side-num { width: 22px; height: 22px; border-radius: 50%; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; background: var(--bl-bg-3); color: var(--bl-text-2); font-weight: 600; font-size: 12px; }
.wz-side-step.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 600; }
.wz-side-step.is-on .wz-side-num { background: var(--bl-primary); color: #fff; }
.wz-side-step.is-done .wz-side-num { background: var(--bl-success); color: #fff; }
.wz-side-lbl { min-width: 0; }

.wz-content { flex: 1; min-width: 0; display: flex; flex-direction: column; overflow: hidden; }
.wz-content-hd { padding: 16px 20px 12px; border-bottom: 1px solid var(--bl-divider); flex-shrink: 0; }
.wz-step-tag { font-size: 12px; color: var(--bl-primary); font-weight: 600; margin-bottom: 4px; }
.wz-content-title { font-size: 16px; font-weight: 600; color: var(--bl-text-1); }
.wz-content-sub { font-size: 12.5px; margin-top: 4px; }

.wz-body { flex: 1; min-height: 0; overflow: auto; padding: 16px 20px; }

/* 原生 select 自定义箭头 (与 SearchSelect 视觉一致) */
select.bl-input {
  appearance: none; -webkit-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2386909c' stroke-width='2.4' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat; background-position: right 10px center;
  padding-right: 28px;
}
select.bl-input.bl-input-xs { background-position: right 7px center; padding-right: 22px; }
:root[data-theme="dark"] select.bl-input {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23a9b0bd' stroke-width='2.4' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
}
.sec { font-size: 12px; color: var(--bl-text-3); font-weight: 600; padding-left: 8px; border-left: 3px solid var(--bl-primary); margin: 16px 0 10px; line-height: 1; }
.sec:first-child { margin-top: 2px; }

/* 步骤1 */
.acw-tabs { display: flex; gap: 8px; flex-wrap: wrap; }
.acw-tab { display: inline-flex; align-items: center; gap: 6px; padding: 7px 14px; border: 1px solid var(--bl-border); background: var(--bl-bg-1); border-radius: 20px; cursor: pointer; font-size: 13px; color: var(--bl-text-2); }
.acw-tab:hover { border-color: var(--bl-primary); }
.acw-tab.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.acw-tab-ic { width: 20px; height: 20px; border-radius: 5px; display: inline-flex; align-items: center; justify-content: center; }

.acw-type-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.acw-type-card { display: flex; gap: 10px; padding: 12px; border: 1px solid var(--bl-border); border-radius: 8px; cursor: pointer; position: relative; align-items: center; transition: border-color .15s; }
.acw-type-card:hover { border-color: var(--bl-primary); }
.acw-type-card.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.acw-type-ic { width: 34px; height: 34px; border-radius: 7px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.acw-type-name { font-weight: 600; font-size: 13.5px; margin-bottom: 2px; }
.acw-type-check { position: absolute; top: 10px; right: 10px; width: 18px; height: 18px; background: var(--bl-primary); border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; }
.acw-subject { padding: 4px 0; display: flex; align-items: center; gap: 8px; }
/* 对象类选择: 整块可点, 与步骤2「对象及属性」的 acw-ref-box 同一套手感 */
.acw-pick { flex: 1; min-width: 0; display: flex; align-items: center; gap: 8px; height: 34px; padding: 0 6px 0 8px;
  border: 1px solid var(--bl-border); border-radius: var(--bl-radius-2); background: var(--bl-bg-1);
  cursor: pointer; font-size: 13px; transition: border-color .15s, background-color .15s; }
.acw-pick:hover { border-color: var(--bl-primary); }
.acw-pick.is-empty { border-style: dashed; }
.acw-pick-ic { width: 22px; height: 22px; border-radius: 5px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.acw-pick-text { flex: 1; min-width: 0; color: var(--bl-text-1); }
.acw-pick-text.is-ph { color: var(--bl-text-3); }
.acw-pick-api { margin-left: 7px; font-size: 12px; }
.acw-pick-x { flex-shrink: 0; border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer;
  display: inline-flex; padding: 3px; border-radius: 3px; }
.acw-pick-x:hover { color: var(--bl-danger); background: var(--bl-bg-2); }
.acw-pick-act { flex-shrink: 0; display: inline-flex; align-items: center; gap: 3px; height: 24px; padding: 0 9px;
  border-radius: var(--bl-radius-2); background: var(--bl-bg-2); color: var(--bl-text-2); font-size: 12px; }
.acw-pick:hover .acw-pick-act { background: var(--bl-primary-soft); color: var(--bl-primary); }

/* 步骤2 表格 */
.acw-row-hd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.acw-hd-title { font-size: 13px; font-weight: 600; }
.acw-table-wrap { border: 1px solid var(--bl-border); border-radius: 6px; overflow: auto; }
.acw-table { width: 100%; min-width: 740px; font-size: 12px; }

/* 步骤2 信息条 + 属性映射矩阵 */
.acw-infobar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; padding: 8px 12px; background: var(--bl-bg-2); border-radius: 6px; font-size: 12.5px; color: var(--bl-text-2); margin-bottom: 12px; }
.acw-infobar b { color: var(--bl-text-1); font-weight: 600; margin-left: 4px; }
.acw-info-sep { width: 1px; height: 12px; background: var(--bl-divider); }
.acw-map-table { width: 100%; min-width: 860px; font-size: 12px; }
.acw-map-table thead th { background: var(--bl-thead-bg); font-weight: 600; height: 34px; padding: 0 6px; white-space: nowrap; color: var(--bl-text-1); position: sticky; top: 0; z-index: 1; }
.acw-map-table thead th.t-left { text-align: left; }
.acw-map-table td { padding: 3px 5px; border-top: 1px solid var(--bl-divider); vertical-align: middle; }
.acw-map-table td.t-center { text-align: center; }
.acw-map-table .bl-input-xs { height: 28px; padding: 0 6px; font-size: 12px; }
.acw-dt-tag { font-size: 11px; padding: 0 6px; }
/* 表单展示 开关 */
.acw-showsw { display: inline-block; width: 34px; height: 18px; border-radius: 9px; background: var(--bl-bg-3, #c9cdd4); position: relative; cursor: pointer; transition: background .15s; vertical-align: middle; }
.acw-showsw.is-on { background: var(--bl-primary); }
.acw-showsw-dot { position: absolute; left: 2px; top: 2px; width: 14px; height: 14px; border-radius: 50%; background: #fff; transition: left .15s; box-shadow: 0 1px 2px rgba(0,0,0,.3); }
.acw-showsw.is-on .acw-showsw-dot { left: 18px; }
/* 默认值 自定义静态值 输入 */
.acw-custom-wrap { display: flex; align-items: center; gap: 3px; }
.acw-custom-wrap .bl-input { flex: 1; min-width: 0; }
/* 属性列: 名称在上, 编码在下, 主键单独标记 */
.acw-prop-cell { display: flex; flex-direction: column; gap: 1px; min-width: 0; }
.acw-prop-name { font-size: 12.5px; color: var(--bl-text-1); display: flex; align-items: center; gap: 5px; }
.acw-prop-code { font-size: 11px; line-height: 1.3; }
.acw-pk { flex-shrink: 0; font-size: 10px; font-weight: 600; color: #D97706; background: color-mix(in srgb, #D97706 14%, transparent); border-radius: 3px; padding: 0 4px; }
/* 表单展示三态 */
.acw-seg { display: inline-flex; padding: 2px; background: var(--bl-bg-2); border-radius: 6px; gap: 2px; }
.acw-seg-b { border: 0; background: transparent; font-size: 11.5px; color: var(--bl-text-2); padding: 3px 8px; border-radius: 4px; cursor: pointer; white-space: nowrap; }
.acw-seg-b:hover { color: var(--bl-text-1); }
.acw-seg-b.is-on { background: var(--bl-bg-1); color: var(--bl-primary); font-weight: 600; box-shadow: 0 1px 2px rgba(0,0,0,.08); }
/* 由赋值方式锁定的默认值配置 */
.acw-locked { height: 28px; display: flex; align-items: center; padding: 0 8px; border: 1px solid var(--bl-divider); border-radius: var(--bl-radius-2); background: var(--bl-bg-2); font-size: 12px; color: var(--bl-text-2); }
.acw-locked.is-off { color: var(--bl-text-3); }
.acw-map-table tr.is-skip td:not(:nth-child(-n+4)) { opacity: .45; }
.acw-custom-back { flex-shrink: 0; width: 20px; height: 24px; border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; justify-content: center; border-radius: 4px; }
.acw-custom-back:hover { background: var(--bl-bg-hover); color: var(--bl-text-1); }
/* 对象及属性 选择器 (输入框 + 内嵌放大镜, 一体) */
.acw-ref-box { display: flex; align-items: center; gap: 4px; height: 28px; padding: 0 6px 0 8px; border: 1px solid var(--bl-border); border-radius: var(--bl-radius-2); background: var(--bl-bg-1); cursor: pointer; font-size: 12px; }
.acw-ref-box:hover:not(.is-disabled) { border-color: var(--bl-primary); }
.acw-ref-box:hover:not(.is-disabled) .acw-ref-ic { color: var(--bl-primary); }
.acw-ref-box.is-disabled { background: var(--bl-bg-2); cursor: default; }
.acw-ref-text { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--bl-text-1); }
.acw-ref-text.is-ph { color: var(--bl-text-3); }
.acw-ref-ic { flex-shrink: 0; color: var(--bl-text-3); display: inline-flex; }
.acw-table thead th { background: var(--bl-thead-bg); font-weight: 600; height: 36px; padding: 0 8px; white-space: nowrap; color: var(--bl-text-1); }
.acw-table thead th.t-left { text-align: left; }
.acw-table td { padding: 4px 6px; border-top: 1px solid var(--bl-divider); }
.acw-table td.t-center { text-align: center; }
.acw-table .bl-input-xs { height: 28px; padding: 0 6px; font-size: 12px; }

/* 步骤3/5 表单 */
.acw-form-grid { display: flex; flex-direction: column; gap: 4px; max-width: 680px; }
/* FieldRow 行内 label 加宽防换行 + 顶对齐 (仅作用于向导内) */
.acw-form-grid :deep(.fr.fr-inline .fr-label) { width: 80px; white-space: nowrap; align-self: flex-start; padding-top: 8px; }
.acw-textarea { min-height: 84px; resize: vertical; line-height: 1.5; }

/* 步骤3 元数据 分段布局 */
.acw-sec3 { font-size: 13px; font-weight: 600; color: var(--bl-text-1); margin: 20px 0 12px; padding-bottom: 6px; border-bottom: 1px solid var(--bl-divider); }
.acw-sec3:first-child { margin-top: 2px; }
.acw-m3-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px 32px; }
.acw-m3-fld { display: flex; flex-direction: column; gap: 6px; min-width: 0; margin-bottom: 8px; }
.acw-m3-lbl { font-size: 12px; color: var(--bl-text-2); }
.acw-m3-lbl i { color: #f53f3f; font-style: normal; }
.acw-m3-inline { flex-direction: row; align-items: center; gap: 12px; }
.acw-m3-inline .acw-m3-lbl { width: 92px; flex-shrink: 0; }
.acw-tg { display: inline-flex; align-items: center; gap: 8px; }
.acw-tg-lbl { font-size: 13px; color: var(--bl-text-2); }
.acw-icon-picker { display: flex; flex-wrap: wrap; gap: 6px; }
.acw-icon-opt { width: 32px; height: 32px; border: 1px solid var(--bl-border); border-radius: 6px; background: var(--bl-bg-1); color: var(--bl-text-2); cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.acw-icon-opt:hover { border-color: var(--bl-primary); color: var(--bl-primary); }
.acw-icon-opt.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); color: var(--bl-primary); }
.acw-color-picker { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.acw-color-opt { width: 24px; height: 24px; border-radius: 6px; border: 2px solid transparent; cursor: pointer; box-shadow: 0 0 0 1px var(--bl-border) inset; }
.acw-color-opt.is-on { box-shadow: 0 0 0 2px var(--bl-primary); }
.wz-body .bl-input:disabled { background: var(--bl-bg-2); color: var(--bl-text-3); cursor: not-allowed; }
.acw-m3-hint { font-size: 11.5px; color: var(--bl-text-3); margin-top: 2px; margin-bottom: 6px; }
.acw-path-box { padding: 10px 12px; background: var(--bl-bg-2); border-radius: 6px; }
.acw-path-box .bl-mono { font-size: 12px; color: var(--bl-text-1); word-break: break-all; }
/* 紧凑单行版 图标/颜色选择器 (约束宽度, 强制单行) */
.acw-pick { max-width: 560px; }
.acw-pick :deep(.cpf-swatches) { max-width: none; flex-wrap: nowrap; }
.acw-pick :deep(.cpf-tail) { margin-left: 10px; }
/* 图标: 网格改 flex 单行, 「更多选择」拉到同一行行尾成内联按钮 */
.acw-pick :deep(.ipf) { display: flex; flex-direction: row !important; flex-wrap: wrap; align-items: center; gap: 8px; }
.acw-pick :deep(.icon-grid) { display: flex !important; flex-wrap: nowrap; gap: 6px; grid-template-columns: none; }
.acw-pick :deep(.icon-cell) { width: 34px; height: 34px; flex: 0 0 34px; }
.acw-pick :deep(.ipf-more-row) { width: auto !important; margin: 0 !important; padding: 0 14px; height: 34px; border-radius: 6px; flex: 0 0 auto; }
.acw-color { width: 40px; height: 30px; padding: 2px; border: 1px solid var(--bl-border); border-radius: var(--bl-radius-2); background: var(--bl-bg-1); cursor: pointer; }
.acw-switch-row { display: inline-flex; gap: 18px; }
.acw-sw { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: var(--bl-text-2); cursor: pointer; }

/* 步骤4 开关卡 */
.acw-toggle-card { border: 1px solid var(--bl-border); border-radius: 8px; padding: 14px; margin-bottom: 10px; transition: border-color .15s; }
.acw-toggle-card.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.acw-toggle { display: flex; gap: 10px; align-items: flex-start; cursor: pointer; }
.acw-toggle input { margin-top: 3px; }
.acw-toggle-title { font-weight: 600; font-size: 13.5px; margin-bottom: 3px; }
.acw-hint { margin-top: 8px; padding: 10px 12px; background: var(--bl-bg-2); border-radius: var(--bl-radius-2); font-size: 12px; color: var(--bl-text-3); }

/* 步骤5 概要 */
.acw-summary { display: flex; flex-direction: column; gap: 6px; font-size: 13px; padding: 12px 14px; background: var(--bl-bg-2); border-radius: 8px; }
.acw-summary .bl-muted { display: inline-block; width: 76px; }

.wz-ft { display: flex; align-items: center; gap: 8px; padding: 10px 16px; border-top: 1px solid var(--bl-divider); }

.wz-fade-enter-active, .wz-fade-leave-active { transition: opacity .15s; }
.wz-fade-enter-from, .wz-fade-leave-to { opacity: 0; }
</style>
