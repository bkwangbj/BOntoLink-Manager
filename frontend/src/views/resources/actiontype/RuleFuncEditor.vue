<template>
  <div class="rfe">
    <div class="fe-warn">⚠ 配置函数规则后,不可叠加其他 Ontology 编辑规则(创建 / 修改 / 删除对象、创建 / 删除链接均不可用),所有对象编辑逻辑由本函数全权处理;可与通知 / Webhook 等副作用规则共存。</div>

    <!-- 1 基础配置 -->
    <div class="adw-card"><div class="adw-card-hd">基础配置</div>
      <div class="adw-grid">
        <label class="adw-fld"><span class="adw-lbl">绑定函数 <i>*</i></span>
          <div class="rfe-row"><input class="bl-input bl-mono" style="flex:1" v-model="rule.func_code" placeholder="函数编码" />
            <button class="bl-btn bl-btn-sm" @click="BL.info('函数选择器后续接入')">选择</button></div></label>
        <label class="adw-fld"><span class="adw-lbl">所属本体</span><input class="bl-input" :value="subjectSummary" disabled /></label>
        <label class="adw-fld"><span class="adw-lbl">函数版本 <i>*</i></span><input class="bl-input bl-mono" v-model="rule.func_version" placeholder="v1.0.0" /></label>
        <label class="adw-fld"><span class="adw-lbl">版本升级策略</span><BlSelect v-model="rule.func_autoupgrade" :options="FUNC_UPGRADE_OPTS" /></label>
      </div>
    </div>

    <!-- 2 入参映射配置 -->
    <div class="adw-card"><div class="adw-card-hd">入参映射配置 <span class="bl-muted" style="font-size:11px;font-weight:400">(值来源与入参类型不匹配会标红)</span></div>
      <FuncParamMapTable :params="rule.func_params" :required="1" :form-options="formParamOptions" :object-options="objectParamOptions" :mismatch="fp => mismatch(rule, fp)" />
      <div style="height:14px"></div>
      <FuncParamMapTable :params="rule.func_params" :required="0" :form-options="formParamOptions" :object-options="objectParamOptions" :mismatch="fp => mismatch(rule, fp)" />
    </div>

    <!-- 3 函数代码预览 -->
    <div class="adw-card">
      <div class="adw-card-hd" style="display:flex;justify-content:space-between;align-items:center;border-left:0;padding-left:0">
        <span style="border-left:3px solid var(--bl-primary);padding-left:8px">函数代码预览</span>
        <a class="fe-repo" @click="BL.info('跳转代码仓库,后续接入')"><span v-html="BL.icon('code', 11)"></span> 在代码仓库中编辑</a>
      </div>
      <CodeEditor :model-value="codePreview(rule)" language="text" disabled :rows="8" />
    </div>

    <!-- 4 执行配置 -->
    <div class="adw-card"><div class="adw-card-hd">执行配置</div>
      <div class="adw-grid">
        <label class="adw-fld"><span class="adw-lbl">执行身份</span><BlSelect v-model="rule.func_exec_identity" :options="FUNC_IDENTITY_OPTS" /></label>
        <label class="adw-fld"><span class="adw-lbl">异常处理策略</span><BlSelect v-model="rule.func_error_strategy" :options="FUNC_ERR_OPTS" /></label>
        <label class="adw-fld"><span class="adw-lbl">超时时长 (秒)</span><input class="bl-input" type="number" v-model="rule.func_timeout" /></label>
        <label class="adw-fld"><span class="adw-lbl">最大重试次数</span><input class="bl-input" type="number" v-model="rule.func_retry" /></label>
      </div>
      <div class="adw-switch-row" style="margin-top:12px">
        <label class="adw-sw"><span class="adw-showsw rl-sw" :class="{ 'is-on': rule.func_concurrent===1 }" @click="rule.func_concurrent = rule.func_concurrent?0:1"><span class="adw-showsw-dot"></span></span> 并发执行</label>
        <label class="adw-sw"><span class="adw-showsw rl-sw" :class="{ 'is-on': rule.func_return_attachment===1 }" @click="rule.func_return_attachment = rule.func_return_attachment?0:1"><span class="adw-showsw-dot"></span></span> 返回附件(可写回本体或推送到前端)</label>
      </div>
    </div>

    <!-- 5 异常映射 -->
    <div class="adw-card"><div class="adw-card-hd">异常映射 <span class="bl-muted" style="font-size:11px;font-weight:400">(函数抛出异常码 → 前端提示文案)</span></div>
      <table class="bl-table ate-mini-table"><thead><tr><th class="t-left">异常码</th><th class="t-left">提示文案</th><th></th></tr></thead>
        <tbody>
          <tr v-for="(ex, ei) in rule.func_exceptions" :key="ei">
            <td style="width:180px"><input class="bl-input bl-input-xs bl-mono" v-model="ex.code" placeholder="如 E_PERMISSION" /></td>
            <td><input class="bl-input bl-input-xs" v-model="ex.message" placeholder="向用户展示的提示" /></td>
            <td class="t-center"><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="rule.func_exceptions.splice(ei,1)" v-html="BL.icon('x', 11)"></button></td>
          </tr>
          <tr v-if="!rule.func_exceptions.length"><td colspan="3" class="bl-muted" style="text-align:center;padding:8px;font-size:12px">暂无异常映射,未匹配的异常将展示默认错误提示</td></tr>
        </tbody></table>
      <button class="bl-btn bl-btn-text bl-btn-sm" style="margin-top:4px" @click="rule.func_exceptions.push({ code:'', message:'' })"><span v-html="BL.icon('plus', 11)"></span><span style="margin-left:3px">添加异常映射</span></button>
    </div>
  </div>
</template>

<script setup>
import { BL } from '@/lib/bl.js'
import BlSelect from '@/components/BlSelect.vue'
import CodeEditor from '@/components/CodeEditor.vue'
import FuncParamMapTable from './FuncParamMapTable.vue'
import { FUNC_UPGRADE_OPTS, FUNC_IDENTITY_OPTS, FUNC_ERR_OPTS } from './ruleModel.js'

defineProps({
  rule: { type: Object, required: true },
  subjectSummary: { type: String, default: '' },
  formParamOptions: { type: Array, default: () => [] },
  objectParamOptions: { type: Array, default: () => [] },
  mismatch: { type: Function, default: () => false },       // (rule, funcParam) => boolean
  codePreview: { type: Function, default: () => '' },
})
</script>

<style scoped src="./ruleEditorShared.css"></style>
<style scoped>
.rfe-row { display: flex; align-items: center; gap: 8px; }
</style>
