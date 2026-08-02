<template>
  <div class="rne">
    <div class="fe-warn">通知属于副作用规则,在所有编辑参数明执行成功、事务提交完成后触发;通知发送失败不影响主操作结果。</div>

    <!-- 1 收件人配置 -->
    <div class="adw-card"><div class="adw-card-hd">收件人配置</div>
      <div class="fe-row"><span class="fe-lbl">收件人来源</span><BlSelect v-model="rule.notify_recipient_source" :options="NOTIFY_RECIPIENT_SRC" style="flex:1" /></div>
      <div v-if="rule.notify_recipient_source === 'object_prop'" class="fe-row" style="margin-top:12px">
        <span class="fe-lbl">对象参数</span>
        <BlSelect v-model="rule.notify_recipient_object_param" :options="objectParamOptions" size="sm" clearable placeholder="对象参数" style="width:180px" />
        <span class="fe-lbl fe-lbl-inline">用户ID属性</span>
        <BlSelect v-model="rule.notify_recipient_user_attr" :options="[]" size="sm" clearable placeholder="选择(待接口)" style="flex:1;max-width:260px" />
      </div>
      <div v-else-if="rule.notify_recipient_source === 'param'" class="fe-row" style="margin-top:12px"><span class="fe-lbl">收件人参数</span>
        <BlSelect v-model="rule.notify_recipient_object_param" :options="formParamOptions" size="sm" clearable placeholder="选表单参数" style="flex:1;max-width:320px" /></div>
      <div v-else class="fe-row" style="margin-top:12px"><span class="fe-lbl">静态收件人</span>
        <input class="bl-input bl-input-sm" style="flex:1" v-model="rule.notify_to" placeholder="用户 / 用户组标识,多个用逗号分隔" /></div>
    </div>

    <!-- 2 内容配置 (模板 / 来自函数) -->
    <div class="adw-card"><div class="adw-card-hd">内容配置</div>
      <div class="fd-tabs2">
        <button :class="['fd-tab2', rule.notify_content_mode !== 'function' && 'is-on']" @click="rule.notify_content_mode = 'desc'">模板</button>
        <button :class="['fd-tab2', rule.notify_content_mode === 'function' && 'is-on']" @click="rule.notify_content_mode = 'function'">来自函数</button>
      </div>
      <template v-if="rule.notify_content_mode !== 'function'">
        <div class="fe-row" style="margin-top:14px"><span class="fe-lbl">主题</span><input class="bl-input" style="flex:1" v-model="rule.notify_title" placeholder="工单 {{ticketId}} 优先级已变更" /></div>
        <div class="fe-row fe-row-top" style="margin-top:12px"><span class="fe-lbl">正文</span>
          <textarea class="bl-textarea" style="flex:1" v-model="rule.notify_content" rows="4" placeholder="您好 {{recipient_first_name}},{{current_user_first_name}} 已将工单 {{ticketId}} 的优先级变更为 {{priority}}。请及时跟进处理。"></textarea></div>
      </template>
      <template v-else>
        <div class="fe-row" style="margin-top:14px"><span class="fe-lbl">通知函数</span>
          <input class="bl-input bl-input-sm bl-mono" style="flex:1;max-width:320px" v-model="rule.notify_func_code" placeholder="函数编码" />
          <button class="bl-btn bl-btn-sm" style="margin-left:8px" @click="BL.info('函数选择器后续接入')">选择</button></div>
        <div class="adw-card-hd" style="margin-top:12px;display:flex;justify-content:space-between;align-items:center;border-left:0;padding-left:0">
          <span style="border-left:3px solid var(--bl-primary);padding-left:8px">消息代码预览</span>
          <a class="fe-repo" @click="BL.info('跳转代码仓库,后续接入')"><span v-html="BL.icon('code', 11)"></span> 在代码仓库中编辑</a></div>
        <CodeEditor :model-value="codePreview(rule)" language="text" disabled :rows="8" />
      </template>
    </div>

    <!-- 3 通知渠道 -->
    <div class="adw-card"><div class="adw-card-hd">通知渠道</div>
      <div class="fe-row"><span class="fe-lbl">启用渠道</span>
        <label class="adw-sw" style="margin-right:22px"><input type="checkbox" v-model="rule.notify_ch_push" :true-value="1" :false-value="0" /> 平台内推送</label>
        <label class="adw-sw" style="margin-right:22px"><input type="checkbox" v-model="rule.notify_ch_email" :true-value="1" :false-value="0" /> 邮件通知</label>
        <label class="adw-sw"><input type="checkbox" v-model="rule.notify_ch_sms" :true-value="1" :false-value="0" /> 短信通知</label>
      </div>
    </div>

    <!-- 4 链接配置 -->
    <div class="adw-card"><div class="adw-card-hd adw-card-hd-flex"><span>链接配置</span>
        <span class="adw-showsw rl-sw" :class="{ 'is-on': rule.notify_link_enabled === 1 }" @click="rule.notify_link_enabled = rule.notify_link_enabled ? 0 : 1"><span class="adw-showsw-dot"></span></span></div>
      <div v-if="rule.notify_link_enabled" class="fe-row fe-row-wrap">
        <span class="fe-lbl">链接类型</span><BlSelect v-model="rule.notify_link_type" :options="NOTIFY_LINK_TYPES" size="sm" style="width:150px" />
        <span class="fe-lbl fe-lbl-inline">链接目标</span><BlSelect v-model="rule.notify_link_target" :options="objectParamOptions" size="sm" clearable placeholder="目标对象" style="width:150px" />
        <span class="fe-lbl fe-lbl-inline">按钮文字</span><input class="bl-input bl-input-sm" style="flex:1;min-width:160px;max-width:220px" v-model="rule.notify_link_text" placeholder="查看工单详情" />
      </div>
      <div v-else class="bl-muted" style="font-size:12px">未启用,通知不带跳转按钮。</div>
    </div>

    <!-- 5 高级配置 -->
    <div class="adw-card"><div class="adw-card-hd">高级配置</div>
      <div class="adw-card-hd-flex" style="border-left:0;padding-left:0;margin-bottom:2px">
        <span style="font-size:13px;font-weight:600;color:var(--bl-text-1)">自定义邮件 HTML 内容</span>
        <span class="adw-showsw rl-sw" :class="{ 'is-on': rule.notify_custom_html === 1 }" @click="rule.notify_custom_html = rule.notify_custom_html ? 0 : 1"><span class="adw-showsw-dot"></span></span></div>
      <textarea v-if="rule.notify_custom_html" class="bl-textarea bl-mono" style="margin-top:10px" v-model="rule.notify_html_content" rows="4" placeholder="<html>… 支持参数占位 …</html>"></textarea>
      <div class="fe-row fe-row-wrap" style="margin-top:16px">
        <span class="fe-lbl">权限范围</span>
        <label class="fe-radio"><input type="radio" value="all" v-model="rule.notify_permission_scope" /><span class="fe-radio-lbl">要求所有用户具有权限(默认)</span></label>
        <label class="fe-radio" style="margin-left:24px"><input type="radio" value="authorized" v-model="rule.notify_permission_scope" /><span class="fe-radio-lbl">要求任意用户具有权限</span></label>
      </div>
    </div>
  </div>
</template>

<script setup>
import { BL } from '@/lib/bl.js'
import BlSelect from '@/components/BlSelect.vue'
import CodeEditor from '@/components/CodeEditor.vue'
import { NOTIFY_RECIPIENT_SRC, NOTIFY_LINK_TYPES } from './ruleModel.js'

defineProps({
  rule: { type: Object, required: true },
  formParamOptions: { type: Array, default: () => [] },
  objectParamOptions: { type: Array, default: () => [] },
  codePreview: { type: Function, default: () => '' },
})
</script>

<style scoped src="./ruleEditorShared.css"></style>
<style scoped>
/* 内容配置的模板/函数切换 */
.fd-tabs2 { display: inline-flex; gap: 2px; padding: 3px; background: var(--bl-bg-2); border-radius: 8px; }
.fd-tab2 { padding: 5px 18px; font-size: 13px; color: var(--bl-text-2); cursor: pointer; background: transparent; border: 0; border-radius: 6px; transition: background .12s, color .12s; }
.fd-tab2:hover { color: var(--bl-text-1); }
.fd-tab2.is-on { background: var(--bl-bg-1); color: var(--bl-primary); font-weight: 600; box-shadow: 0 1px 2px rgba(0,0,0,.08); }
</style>
