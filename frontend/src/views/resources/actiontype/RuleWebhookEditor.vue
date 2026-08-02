<template>
  <div class="rwe">
    <div class="fe-warn">Webhook 用于向外部系统发送 HTTP 请求,实现跨系统数据同步与流程联动;分为回写(事务提交前执行)和副作用(事务提交后执行)两种类型。</div>

    <!-- 1 Webhook 类型 -->
    <div class="adw-card"><div class="adw-card-hd">Webhook 类型</div>
      <div class="wh-type-grid">
        <button v-for="t in WH_SUBTYPES" :key="t.value" :class="['wh-type-card', rule.wh_subtype===t.value && 'is-on']" @click="rule.wh_subtype=t.value">
          <span class="wh-type-ic" :style="{ background: rule.wh_subtype===t.value ? accentColor : 'var(--bl-bg-3)' }" v-html="BL.icon(t.icon, 13, '#fff')"></span>
          <span class="wh-type-lbl">{{ t.label }}</span>
          <span v-if="rule.wh_subtype===t.value" class="wh-type-chk" v-html="BL.icon('check', 14)"></span>
        </button>
      </div>
      <div class="bl-muted" style="font-size:12px;line-height:1.6;margin-top:10px">{{ WH_SUBTYPES.find(t=>t.value===rule.wh_subtype)?.desc }}</div>
    </div>

    <!-- 2 基本信息 -->
    <div class="adw-card"><div class="adw-card-hd">Webhook 基本信息</div>
      <div class="adw-grid">
        <label class="adw-fld"><span class="adw-lbl">选择 Webhook <i>*</i></span>
          <div class="rwe-row"><input class="bl-input" style="flex:1" v-model="rule.wh_code" placeholder="选择已注册的 Webhook" />
            <button class="bl-btn bl-btn-sm" @click="BL.info('Webhook 选择器后续接入')">选择</button></div></label>
        <label class="adw-fld"><span class="adw-lbl">版本</span><input class="bl-input bl-mono" v-model="rule.wh_version" placeholder="v1" /></label>
      </div>
    </div>

    <!-- 3 输入参数 -->
    <div class="adw-card"><div class="adw-card-hd adw-card-hd-flex"><span>Webhook 输入参数</span><a class="fe-repo" @click="BL.info('跳转动作参数定义,后续接入')">跳过动作参数定义</a></div>
      <label class="rwe-ck"><input type="checkbox" :checked="rule.wh_input_mode==='function'" @change="rule.wh_input_mode = rule.wh_input_mode==='function' ? 'manual' : 'function'" /> 选择返回 Webhook 预期输入约束的函数</label>
      <div v-if="rule.wh_input_mode==='function'" class="adw-grid" style="margin-bottom:12px">
        <label class="adw-fld"><span class="adw-lbl">函数</span>
          <div class="rwe-row"><input class="bl-input bl-mono" style="flex:1" v-model="rule.wh_input_func" placeholder="returnWebhookInput" />
            <button class="bl-btn bl-btn-sm" @click="BL.info('函数选择器后续接入')">选择</button></div></label>
        <label class="adw-fld"><span class="adw-lbl">版本</span><input class="bl-input bl-mono" v-model="rule.wh_input_func_version" placeholder="0.22.0" /></label>
      </div>
      <div class="fe-subhd">必须输入项</div>
      <table class="bl-table ate-mini-table"><thead><tr><th class="t-left">输入项名</th><th class="t-left">类型</th><th class="t-left">值来源</th><th class="t-left">取值配置</th><th></th></tr></thead>
        <tbody>
          <tr v-for="(p, pi) in rule.wh_params" :key="pi">
            <td><input class="bl-input bl-input-xs bl-mono" v-model="p.name" placeholder="input_name" /></td>
            <td><BlSelect v-model="p.param_type" :options="FUNC_PTYPE_OPTS" size="sm" /></td>
            <td><BlSelect v-model="p.value_source" :options="VALUE_SOURCE_OPTS" size="sm" /></td>
            <td>
              <BlSelect v-if="Number(p.value_source)===1" v-model="p.value_content" :options="formParamOptions" size="sm" clearable placeholder="选表单参数" />
              <BlSelect v-else-if="Number(p.value_source)===5" v-model="p.value_content" :options="objectParamOptions" size="sm" clearable placeholder="选对象引用参数" />
              <input v-else-if="Number(p.value_source)===2" class="bl-input bl-input-xs" v-model="p.value_content" placeholder="静态值" />
              <span v-else class="bl-muted" style="font-size:12px;padding-left:4px">自动</span>
            </td>
            <td class="t-center"><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="rule.wh_params.splice(pi,1)" v-html="BL.icon('x', 11)"></button></td>
          </tr>
          <tr v-if="!rule.wh_params.length"><td colspan="5" class="bl-muted" style="text-align:center;padding:8px;font-size:12px">暂无输入项</td></tr>
        </tbody></table>
      <button class="bl-btn bl-btn-text bl-btn-sm" style="margin-top:4px" @click="rule.wh_params.push(newWhParam())"><span v-html="BL.icon('plus', 11)"></span><span style="margin-left:3px">添加输入项</span></button>
      <template v-if="rule.wh_input_mode==='function'">
        <div class="adw-card-hd" style="margin-top:14px;display:flex;justify-content:space-between;align-items:center;border-left:0;padding-left:0">
          <span style="border-left:3px solid var(--bl-primary);padding-left:8px">函数代码预览</span>
          <a class="fe-repo" @click="BL.info('跳转代码仓库,后续接入')"><span v-html="BL.icon('code', 11)"></span> 在代码仓库中编辑</a></div>
        <CodeEditor :model-value="codePreview(rule)" language="text" disabled :rows="10" />
      </template>
    </div>

    <!-- 4 执行说明 -->
    <div class="adw-card"><div class="adw-card-hd">执行说明</div>
      <div v-if="rule.wh_subtype==='writeback'" class="fe-hints">
        <div>✓ 回写模式在所有编辑保存提交之前执行,失败则终止整个动作</div>
        <div>✓ 外部系统返回结构响应可用于后续编辑规则,通知数据中引用(回写响应)</div>
        <div>✓ 每个动作仅可配置 1 个回写型 Webhook</div>
      </div>
      <div v-else class="fe-hints">
        <div>✓ 副作用模式在所有编辑保存提交之后执行,事务提交完成后</div>
        <div>✓ 执行失败不影响主操作结果,仅记录日志</div>
        <div>✓ 每个动作可配置多个副作用 Webhook,支持并行执行</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { BL } from '@/lib/bl.js'
import BlSelect from '@/components/BlSelect.vue'
import CodeEditor from '@/components/CodeEditor.vue'
import { VALUE_SOURCE_OPTS, FUNC_PTYPE_OPTS } from './funcParamModel.js'
import { WH_SUBTYPES, newWhParam } from './ruleModel.js'

defineProps({
  rule: { type: Object, required: true },
  accentColor: { type: String, default: '#0FC6C2' },
  formParamOptions: { type: Array, default: () => [] },
  objectParamOptions: { type: Array, default: () => [] },
  codePreview: { type: Function, default: () => '' },
})
</script>

<style scoped src="./ruleEditorShared.css"></style>
<style scoped>
.rwe-row { display: flex; align-items: center; gap: 8px; }
.rwe-ck { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--bl-text-2); cursor: pointer; margin-bottom: 10px; }
.wh-type-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.wh-type-card { display: flex; align-items: center; gap: 8px; padding: 12px 14px; border: 1px solid var(--bl-border); border-radius: 8px; background: var(--bl-bg-1); cursor: pointer; font-size: 13px; color: var(--bl-text-1); }
.wh-type-card:hover { border-color: var(--bl-primary); }
.wh-type-card.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.wh-type-ic { width: 24px; height: 24px; border-radius: 6px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.wh-type-lbl { font-weight: 600; }
.wh-type-chk { margin-left: auto; color: var(--bl-primary); display: inline-flex; }
</style>
