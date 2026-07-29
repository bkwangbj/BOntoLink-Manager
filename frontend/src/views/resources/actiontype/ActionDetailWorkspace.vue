<template>
  <Teleport to="body">
    <transition name="adw-drawer">
      <aside v-if="open" class="adw-drawer" :style="{ width: drawerWidth + 'px' }">
        <div class="adw-drag-handle" @mousedown="onDragStart" :class="resizing && 'is-resizing'"></div>
        <!-- ===== 抽屉头部条 ===== -->
        <div class="adw-drawer-hd">
          <div class="adw-drawer-hd-l">
            <span class="adw-title-ic" :style="{ background: headColor }" v-html="BL.icon(form.icon || headIcon, 18, '#fff')"></span>
            <div class="bl-grow" style="min-width:0">
              <div class="adw-drawer-title">
                <span class="bl-truncate">{{ form.rdfs_label || '未命名动作' }}</span>
                <span v-if="form.api_name" class="bl-mono bl-muted" style="font-size:12px">({{ form.api_name }})</span>
                <span :class="['bl-tag', statusTagCls(form.status)]">{{ statusLabel(form.status) }}</span>
              </div>
            </div>
          </div>
          <div class="adw-drawer-hd-r">
            <button class="bl-btn bl-btn-text bl-btn-icon at-del-op" title="删除动作" @click="onDelete" v-html="BL.icon('trash2', 14)"></button>
            <span class="adw-drawer-sep"></span>
            <button class="bl-btn bl-btn-text bl-btn-icon" :title="drawerMaxed ? '恢复' : '最大化'" @click="toggleMax" v-html="BL.icon(drawerMaxed ? 'minimize' : 'maximize', 14)"></button>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="onClose" v-html="BL.icon('x', 14)"></button>
          </div>
        </div>
        <!-- ===== 抽屉主体: 左导航 + 主内容 + 右预览 ===== -->
        <div class="adw-drawer-body">
          <!-- ===== 左侧导航 ===== -->
          <aside class="adw-nav">
            <nav class="adw-menu">
            <template v-for="m in MENUS" :key="m.k">
              <div :class="['adw-menu-item', activeMenu === m.k && 'is-on']" @click="onMenuClick(m)">
                <span class="adw-menu-ic" v-html="BL.icon(m.icon, 14)"></span>
                <span class="adw-menu-label">{{ m.label }}</span>
                <span v-if="menuCount(m.k)" class="adw-menu-badge">{{ menuCount(m.k) }}</span>
                <span v-if="m.k === 'form'" class="adw-menu-chev" :class="{ 'is-open': formNavOpen }" title="展开/收起表单结构" @click.stop="formNavOpen = !formNavOpen" v-html="BL.icon('chevronRight', 12)"></span>
                <span v-if="m.k === 'rules'" class="adw-menu-chev" :class="{ 'is-open': rulesNavOpen }" title="展开/收起规则列表" @click.stop="rulesNavOpen = !rulesNavOpen" v-html="BL.icon('chevronRight', 12)"></span>
              </div>
              <!-- 规则结构树: 按 编辑类 / 副作用 分组 -->
              <div v-if="m.k === 'rules' && rulesNavOpen" class="adw-tree">
                <div :class="['adw-tree-root', activeMenu === 'rules' && ruleView === 'list' && 'is-on']" @click="backToRules(true)">
                  <span class="adw-tree-ic" v-html="BL.icon('code', 11)"></span>规则总览
                </div>
                <template v-for="g in ruleNavGroups" :key="g.label">
                  <div class="adw-tree-sec">{{ g.label }}</div>
                  <div v-for="x in g.items" :key="x.r._k"
                       :class="['adw-tree-field', activeMenu === 'rules' && ruleSelKey === x.r._k && 'is-on', x.r.status === 0 && 'is-off']"
                       :title="ruleNavLabel(x.r)" @click="openRuleFromNav(x.i)">
                    <span class="adw-tree-dt" :style="{ background: kindMeta(x.r.kind).color }" v-html="BL.icon(kindMeta(x.r.kind).icon, 11, '#fff')"></span>
                    <span class="bl-truncate adw-tree-fname">{{ ruleNavLabel(x.r) }}</span>
                  </div>
                </template>
                <div v-if="!rules.length" class="adw-tree-empty">暂无规则</div>
              </div>
              <!-- 表单结构树: 可独立收展 (不依赖当前菜单) -->
              <div v-if="m.k === 'form' && formNavOpen" class="adw-tree">
                <div :class="['adw-tree-root', activeMenu === 'form' && formView === 'list' && 'is-on']" @click="backToList">
                  <span class="adw-tree-ic" v-html="BL.icon('menu', 11)"></span>表单内容
                </div>
                <template v-for="sec in sections" :key="sec">
                  <div class="adw-tree-sec">{{ sec }}</div>
                  <div v-for="x in paramsOfSection(sec)" :key="x.i"
                       :class="['adw-tree-field', activeMenu === 'form' && formView === 'detail' && selIdx === x.i && 'is-on']" @click="openParam(x.i)">
                    <span class="adw-tree-dt" :style="{ background: dtMeta(x.p.param_type).color }" v-html="BL.icon(dtMeta(x.p.param_type).icon, 11, '#fff')"></span>
                    <span class="bl-truncate adw-tree-fname">{{ x.p.param_name || x.p.param_code || '未命名' }}</span>
                    <span class="adw-tree-disp" :title="displayMeta(x.p.display_type).label" v-html="BL.icon(displayMeta(x.p.display_type).icon, 12)"></span>
                  </div>
                </template>
                <div v-if="!formParams.length" class="adw-tree-empty">暂无参数</div>
              </div>
            </template>
          </nav>
        </aside>

        <!-- ===== 中间主内容区 ===== -->
        <main class="adw-main">
          <!-- ========= 概览 ========= -->
          <div v-if="activeMenu === 'overview'" class="adw-ov" :class="{ 'is-view': !editMode }">
            <!-- 配置统计区: 4 卡片 -->
            <div class="adw-stats">
              <div class="adw-stat"><span class="adw-stat-ic" style="background:#165DFF" v-html="BL.icon('menu', 16, '#fff')"></span>
                <div><div class="adw-stat-num">{{ formParams.length }} <span class="adw-stat-sub">/ {{ sections.length }} 分区</span></div><div class="adw-stat-lbl">表单字段</div></div></div>
              <div class="adw-stat"><span class="adw-stat-ic" style="background:#722ED1" v-html="BL.icon('code', 16, '#fff')"></span>
                <div><div class="adw-stat-num">{{ rules.length }} <span class="adw-stat-sub">/ {{ activeRuleCount }} 生效</span></div><div class="adw-stat-lbl">业务规则</div></div></div>
              <div class="adw-stat"><span class="adw-stat-ic" style="background:#00B42A" v-html="BL.icon('zap', 16, '#fff')"></span>
                <div><div class="adw-stat-num">{{ triggerCount }}</div><div class="adw-stat-lbl">触发入口</div></div></div>
              <div class="adw-stat"><span class="adw-stat-ic" style="background:#FF7D00" v-html="BL.icon('lock', 16, '#fff')"></span>
                <div><div class="adw-stat-num">{{ submit.enabled ? '已启用' : '未启用' }}</div><div class="adw-stat-lbl">安全校验</div></div></div>
            </div>

            <!-- ===== 上组: 基础信息 (标题 + 编辑唤醒按钮) ===== -->
            <div class="adw-ov-ghd">
              <div class="adw-ov-gtitle">基础信息</div>
              <span class="adw-card-hd-act">
                <template v-if="!editMode">
                  <button class="bl-btn bl-btn-sm bl-btn-primary" @click="editMode = true"><span v-html="BL.icon('edit', 12, '#fff')"></span><span style="margin-left:4px">编辑</span></button>
                </template>
                <template v-else>
                  <button class="bl-btn bl-btn-sm" @click="cancelEdit">取消</button>
                  <button class="bl-btn bl-btn-sm bl-btn-primary" :disabled="saving" @click="onSave">{{ saving ? '保存中…' : '保存' }}</button>
                </template>
              </span>
            </div>

            <!-- 基础信息面板 (一整块, 内含三小节) -->
            <div class="adw-card">
              <div class="adw-card-hd">基础元信息</div>
              <div class="adw-grid">
                <label class="adw-fld"><span class="adw-lbl">动作名称 <i>*</i></span><input class="bl-input" v-model="form.rdfs_label" :disabled="!editMode" /></label>
                <label class="adw-fld"><span class="adw-lbl">动作编码</span><input class="bl-input bl-mono" :value="form.api_name" disabled /></label>
                <label class="adw-fld"><span class="adw-lbl">动作类型</span><input class="bl-input" :value="typeLabel" disabled /></label>
                <label class="adw-fld"><span class="adw-lbl">关联主体</span><input class="bl-input" :value="subjectSummary" disabled /></label>
                <label class="adw-fld"><span class="adw-lbl">按钮文案</span><input class="bl-input" v-model="form.button_text" :disabled="!editMode" placeholder="展示在详情/批量操作栏" /></label>
                <label class="adw-fld"><span class="adw-lbl">所属领域</span><BlSelect v-model="form.category_code" :options="domainOptions" :disabled="!editMode" clearable placeholder="未分类" /></label>
              </div>

              <div class="adw-card-hd adw-sec-gap">展示配置</div>
              <div class="adw-switch-row">
                <label class="adw-sw"><input type="checkbox" v-model="form.show_on_detail" :true-value="1" :false-value="0" :disabled="!editMode" /> 详情页展示</label>
                <label class="adw-sw"><input type="checkbox" v-model="form.show_on_batch" :true-value="1" :false-value="0" :disabled="!editMode" /> 批量操作展示</label>
              </div>
              <div class="adw-grid" style="margin-top:12px">
                <label class="adw-fld"><span class="adw-lbl">按钮图标</span>
                  <IconPickerField v-if="editMode" v-model="form.icon" label="" :preset-count="6" :suggest-name="form.rdfs_label || form.api_name" />
                  <div v-else class="adw-ro-icon"><span v-if="form.icon" v-html="BL.icon(form.icon, 18)"></span><span v-else class="bl-muted">—</span></div>
                </label>
                <label class="adw-fld"><span class="adw-lbl">按钮颜色</span>
                  <ColorPickerField v-if="editMode" v-model="form.color" :palette="COMPACT_COLORS" />
                  <div v-else class="adw-ro-icon"><span v-if="form.color" class="adw-color-sw" :style="{ background: form.color }"></span><span v-else class="bl-muted">—</span></div>
                </label>
              </div>

              <div class="adw-card-hd adw-sec-gap">业务说明</div>
              <label class="adw-fld"><span class="adw-lbl">描述</span><textarea class="bl-textarea" v-model="form.rdfs_comment" :disabled="!editMode" rows="3" placeholder="动作的业务语义描述"></textarea></label>
              <div class="adw-grid" style="margin-top:10px">
                <label class="adw-fld"><span class="adw-lbl">参考资料</span><input class="bl-input" v-model="form.rdfs_see_also" :disabled="!editMode" placeholder="参考文档/规范链接" /></label>
                <label class="adw-fld"><span class="adw-lbl">定义来源</span><input class="bl-input" v-model="form.rdfs_defined_by" :disabled="!editMode" placeholder="如 水利公共本体库" /></label>
              </div>
            </div>

            <!-- ===== 下组: 系统信息 ===== -->
            <div class="adw-ov-ghd" style="margin-top:20px"><div class="adw-ov-gtitle">系统信息 <span class="bl-muted" style="font-size:12px;font-weight:400">(只读,系统自动生成)</span></div></div>
            <div class="adw-card adw-card-ro">
              <div class="adw-grid">
                <label class="adw-fld"><span class="adw-lbl">资源 ID</span><input class="bl-input bl-mono" :value="form.api_name || '—'" disabled /></label>
                <label class="adw-fld"><span class="adw-lbl">RID</span><input class="bl-input bl-mono" :value="form.rid || '—'" disabled /></label>
                <label class="adw-fld"><span class="adw-lbl">当前版本</span><input class="bl-input bl-mono" :value="form.current_version || '—'" disabled /></label>
                <label class="adw-fld"><span class="adw-lbl">编译状态</span><input class="bl-input" :value="compileLabel(form.compile_status)" disabled /></label>
                <label class="adw-fld"><span class="adw-lbl">创建时间</span><input class="bl-input bl-mono" :value="shortTime(form.create_time)" disabled /></label>
                <label class="adw-fld"><span class="adw-lbl">更新时间</span><input class="bl-input bl-mono" :value="shortTime(form.update_time)" disabled /></label>
              </div>
            </div>
          </div>

          <!-- ========= 规则 (移植) ========= -->
          <div v-else-if="activeMenu === 'rules'" class="adw-page" :class="{ 'is-detail': ruleView !== 'list' }">
            <template v-if="ruleView === 'list'">
            <div class="adw-page-hd"><div class="adw-page-title">规则 <span class="bl-muted" style="font-size:11.5px">(按从上至下顺序生效,后置规则覆盖前置)</span></div><button class="bl-btn bl-btn-primary" @click="onSave" :disabled="saving">保存</button></div>
            <div v-if="ruleConflicts.length" class="rl-conflict">
              <div class="rl-conflict-hd"><span v-html="BL.icon('lock', 13)"></span><span>检测到 {{ ruleConflicts.length }} 处无效规则组合,保存前需修正:</span></div>
              <div v-for="(c, ci) in ruleConflicts" :key="ci" class="rl-conflict-item">· {{ c }}</div>
            </div>

            <div v-for="(rule, ri) in rules" :key="rule._k" class="rl-card"
                 draggable="true" @dragstart="onRuleDragStart(ri, $event)" @dragover.prevent @drop="onRuleDrop(ri)" @dragend="rDragIdx = null"
                 :class="{ 'is-dragging': rDragIdx === ri, 'is-off': rule.status === 0 }">
              <div class="rl-hd" @click="rule._collapsed = !rule._collapsed">
                <span class="rl-grip" @mousedown.stop v-html="BL.icon('move', 13)"></span>
                <span class="rl-ic" :style="{ background: kindMeta(rule.kind).color }" v-html="BL.icon(kindMeta(rule.kind).icon, 12, '#fff')"></span>
                <span class="rl-kind">{{ kindMeta(rule.kind).label }}</span>
                <input class="rl-name" v-model="rule.rule_name" placeholder="规则名称" @click.stop />
                <span v-if="rule.status === 0" class="bl-tag bl-tag-warning" style="margin-left:6px">已停用</span>
                <span style="flex:1"></span>
                <span class="adw-showsw rl-sw" :class="{ 'is-on': rule.status === 1 }" :title="rule.status ? '已启用,点击停用' : '已停用,点击启用'" @click.stop="rule.status = rule.status ? 0 : 1"><span class="adw-showsw-dot"></span></span>
                <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon at-del-op" title="删除规则" @click.stop="removeRule(rule)" v-html="BL.icon('trash2', 12)"></button>
                <span class="rl-chev" v-html="BL.icon(rule._collapsed ? 'chevronDown' : 'chevronUp', 13)"></span>
              </div>
              <div v-if="rule._collapsed" class="rl-preview">{{ rulePreview(rule) }}</div>
              <div v-else class="rl-body">
                <!-- 对象/链接规则 -->
                <template v-if="['create_object','modify_object','delete_object','create_link','delete_link'].includes(rule.kind)">
                  <div class="rl-row" style="justify-content:flex-end"><button class="bl-btn bl-btn-sm bl-btn-primary" @click="openObjectEdit(ri)"><span v-html="BL.icon('edit', 12, '#fff')"></span><span style="margin-left:4px">完整编辑</span></button></div>
                  <div v-if="rule.kind.includes('link')" class="rl-row"><span class="rl-lbl">链接类型编码</span><input class="bl-input bl-input-sm bl-mono" v-model="rule.link_type_code" placeholder="link_type_id" /></div>
                  <!-- 目标对象参数 (修改/删除对象) -->
                  <div v-if="rule.kind === 'modify_object' || rule.kind === 'delete_object'" class="rl-row">
                    <span class="rl-lbl">目标对象参数</span>
                    <BlSelect v-model="rule.target_param_code" :options="objectParamOptions" size="sm" clearable :placeholder="objectParamOptions.length ? '选择对象引用参数' : '暂无对象引用参数,请先在表单添加'" style="flex:1;max-width:300px" />
                  </div>
                  <!-- 链接两端 (创建/删除链接) -->
                  <template v-if="rule.kind.includes('link')">
                    <div class="rl-row"><span class="rl-lbl">源对象参数</span><BlSelect v-model="rule.link_src_param" :options="objectParamOptions" size="sm" clearable placeholder="源对象引用参数" style="flex:1;max-width:300px" /></div>
                    <div class="rl-row"><span class="rl-lbl">目标对象参数</span><BlSelect v-model="rule.link_dst_param" :options="objectParamOptions" size="sm" clearable placeholder="目标对象引用参数" style="flex:1;max-width:300px" /></div>
                  </template>
                  <template v-if="!rule.kind.startsWith('delete')">
                    <div class="rl-sub">属性赋值映射 <button class="bl-btn bl-btn-text bl-btn-sm" @click="addMapping(rule)"><span v-html="BL.icon('plus', 11)"></span><span style="margin-left:3px">加属性</span></button></div>
                    <table class="bl-table ate-mini-table"><thead><tr><th class="t-left">写入属性</th><th class="t-left">操作</th><th class="t-left">值来源</th><th class="t-left">值内容</th><th class="t-center">必填</th><th></th></tr></thead>
                      <tbody><tr v-for="(m, mi) in rule.prop_mappings" :key="mi">
                        <td><BlSelect v-if="classProps.length" v-model="m.property_code" :options="classPropsOptions" size="sm" clearable placeholder="属性编码" /><input v-else class="bl-input bl-input-xs bl-mono" v-model="m.property_code" placeholder="属性编码" /></td>
                        <td><BlSelect v-model="m.prop_operator" :options="PROP_OPERATOR_OPTS" size="sm" /></td>
                        <td><BlSelect v-model="m.value_source" :options="VALUE_SOURCE_OPTS" size="sm" /></td>
                        <td>
                          <BlSelect v-if="Number(m.value_source)===1" v-model="m.value_content" :options="formParamOptions" size="sm" clearable placeholder="选表单参数" />
                          <BlSelect v-else-if="Number(m.value_source)===5" v-model="m.value_content" :options="objectParamOptions" size="sm" clearable placeholder="选对象引用参数" />
                          <input v-else-if="Number(m.value_source)===2" class="bl-input bl-input-xs" v-model="m.value_content" placeholder="静态值" />
                          <span v-else class="bl-muted" style="font-size:12px;padding-left:4px">自动(系统上下文)</span>
                        </td>
                        <td class="t-center"><input type="checkbox" v-model="m.is_required" :true-value="1" :false-value="0" /></td>
                        <td class="t-center"><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="rule.prop_mappings.splice(mi,1)" v-html="BL.icon('x', 11)"></button></td>
                      </tr><tr v-if="!rule.prop_mappings.length"><td colspan="6" class="bl-muted" style="text-align:center;padding:10px;font-size:12px">暂无属性映射</td></tr></tbody></table>
                  </template>
                  <div v-else class="fd-warn">删除规则:目标对象/链接仅支持对象引用参数,不支持本次新建的临时对象。</div>
                </template>
                <!-- 函数规则 (折叠卡片态: 快速配置 + 完整编辑入口) -->
                <template v-else-if="rule.kind === 'function'">
                  <div class="rl-row" style="justify-content:space-between"><span class="fd-warn" style="flex:1">配置函数规则后不可叠加其他 Ontology 编辑规则;可与通知 / Webhook / 表单校验共存。</span><button class="bl-btn bl-btn-sm bl-btn-primary" style="flex-shrink:0;margin-left:10px" @click="openFuncEdit(ri)"><span v-html="BL.icon('edit', 12, '#fff')"></span><span style="margin-left:4px">完整编辑</span></button></div>
                  <div class="rl-row"><span class="rl-lbl">绑定函数</span><input class="bl-input bl-input-sm bl-mono" v-model="rule.func_code" placeholder="函数编码" /></div>
                  <div class="rl-row"><span class="rl-lbl">函数版本</span><input class="bl-input bl-input-sm" v-model="rule.func_version" style="max-width:120px" /><label class="adw-sw" style="margin-left:16px"><input type="checkbox" v-model="rule.func_autoupgrade" :true-value="1" :false-value="0" /> 自动升级到兼容版本</label></div>
                  <div class="rl-sub">必填入参 · {{ (rule.func_params||[]).filter(p=>p.required).length }} 个 <button class="bl-btn bl-btn-text bl-btn-sm" @click="openParamMap(rule)"><span v-html="BL.icon('link', 11)"></span><span style="margin-left:3px">快速入参映射</span></button></div>
                  <div class="rl-code">// 代码预览(只读) · {{ rule.func_code || '未绑定函数' }}@{{ rule.func_version }}
// 在代码仓库中编辑函数逻辑</div>
                </template>
                <!-- 通知规则 (折叠卡片态: 快速配置 + 完整编辑入口) -->
                <template v-else-if="rule.kind === 'notification'">
                  <div class="rl-row" style="justify-content:space-between"><span class="fd-warn" style="flex:1">通知内容基于编辑应用前的本体状态生成,在所有编辑提交后发送。</span><button class="bl-btn bl-btn-sm bl-btn-primary" style="flex-shrink:0;margin-left:10px" @click="openNotifyEdit(ri)"><span v-html="BL.icon('edit', 12, '#fff')"></span><span style="margin-left:4px">完整编辑</span></button></div>
                  <div class="rl-row"><span class="rl-lbl">通知标题</span><input class="bl-input bl-input-sm" v-model="rule.notify_title" placeholder="支持 {参数} 动态渲染" /></div>
                  <div class="rl-row rl-row-top"><span class="rl-lbl">通知正文</span><textarea class="bl-textarea" v-model="rule.notify_content" rows="2" placeholder="通知正文,支持参数占位"></textarea></div>
                  <div class="rl-sub">渠道:<span class="bl-muted" style="font-weight:400">{{ [rule.notify_ch_push&&'平台内消息', rule.notify_ch_email&&'邮件', rule.notify_ch_sms&&'短信'].filter(Boolean).join(' / ') || '未选择' }}</span></div>
                </template>
                <!-- Webhook (折叠卡片态: 快速配置 + 完整编辑入口) -->
                <template v-else-if="rule.kind === 'webhook'">
                  <div class="rl-row" style="justify-content:space-between"><span class="fd-warn" style="flex:1">Webhook 向外部系统发起 HTTP 请求,分「回写 / 副作用」两种类型,行为与失败影响不同。</span><button class="bl-btn bl-btn-sm bl-btn-primary" style="flex-shrink:0;margin-left:10px" @click="openWebhookEdit(ri)"><span v-html="BL.icon('edit', 12, '#fff')"></span><span style="margin-left:4px">完整编辑</span></button></div>
                  <div class="rl-row"><span class="rl-lbl">类型</span><BlSelect v-model="rule.wh_subtype" :options="WH_SUBTYPES.map(t=>({value:t.value,label:t.label}))" style="max-width:140px" /><span class="rl-lbl" style="margin-left:16px">Webhook</span><input class="bl-input bl-input-sm bl-mono" style="flex:1" v-model="rule.wh_code" placeholder="选择 / 输入 Webhook 编码" /></div>
                  <div class="rl-sub">必填输入项 · {{ (rule.wh_params||[]).length }} 个 <button class="bl-btn bl-btn-text bl-btn-sm" @click="openWebhookEdit(ri)"><span v-html="BL.icon('link', 11)"></span><span style="margin-left:3px">配置输入参数</span></button></div>
                </template>
              </div>
            </div>
            <div v-if="!rules.length" class="bl-empty" style="padding:20px;font-size:12px">暂无规则,点下方「添加新规则」</div>

            <button class="rl-add-btn" @click="rulePickerOpen = true"><span v-html="BL.icon('plus', 14)"></span><span style="margin-left:5px">添加新规则</span></button>
            </template>

            <!-- ===== 对象 / 链接规则 完整编辑态 ===== -->
            <template v-else-if="ruleView === 'object' && selEditRule">
              <div class="fd-detail-hd">
                <div class="fd-phd2" style="justify-content:space-between">
                  <div class="bl-row" style="gap:8px;min-width:0">
                    <button class="fd-back" @click="backToRules"><span v-html="BL.icon('chevronLeft', 13)"></span><span>返回规则</span></button>
                    <span class="fd-phd2-sep"></span>
                    <span class="rl-ic" :style="{ background: kindMeta(selEditRule.kind).color }" v-html="BL.icon(kindMeta(selEditRule.kind).icon, 12, '#fff')"></span>
                    <input class="fd-pname2" v-model="selEditRule.rule_name" :placeholder="kindMeta(selEditRule.kind).label + ' 规则名称'" />
                  </div>
                  <button class="bl-btn bl-btn-sm bl-btn-primary" @click="onSave" :disabled="saving">保存</button>
                </div>
              </div>
              <div class="fd-detail-body">
                <div class="fe-info"><span v-html="BL.icon('info', 14, '#165DFF')"></span><span>规则按从上到下顺序依次执行,后行的规则会覆盖前序规则对同一对象同属性的修改。可在「规则总览」拖拽调整规则顺序。</span></div>

                <!-- 1 基础配置 -->
                <div class="adw-card"><div class="adw-card-hd">基础配置</div>
                  <div class="adw-grid">
                    <template v-if="!selEditRule.kind.includes('link')">
                      <label class="adw-fld"><span class="adw-lbl">对象类型 <i>*</i></span>
                        <BlSelect v-model="selEditRule.obj_class_id" :options="objClassOptions" clearable placeholder="选择对象类型" @update:modelValue="loadRuleClassProps" />
                      </label>
                      <label v-if="selEditRule.kind === 'create_object'" class="adw-fld"><span class="adw-lbl">主键属性</span>
                        <BlSelect v-model="selEditRule.obj_pk_property" :options="objPkOptions" placeholder="系统自动生成" />
                      </label>
                      <label v-else class="adw-fld"><span class="adw-lbl">目标对象参数 <i>*</i></span>
                        <BlSelect v-model="selEditRule.target_param_code" :options="objectParamOptions" clearable :placeholder="objectParamOptions.length ? '选择对象引用参数' : '暂无对象引用参数,请先在表单添加'" />
                      </label>
                    </template>
                    <template v-else>
                      <label class="adw-fld"><span class="adw-lbl">链接类型 <i>*</i></span>
                        <BlSelect v-model="selEditRule.link_type_code" :options="linkTypeOptions" clearable placeholder="选择链接类型" />
                      </label>
                      <label class="adw-fld"><span class="adw-lbl">链接基数</span><input class="bl-input" :value="linkCardLabel(selEditRule.link_type_code)" disabled /></label>
                      <label class="adw-fld"><span class="adw-lbl">源对象参数 <i>*</i></span>
                        <BlSelect v-model="selEditRule.link_src_param" :options="objectParamOptions" clearable placeholder="源对象引用参数" />
                      </label>
                      <label class="adw-fld"><span class="adw-lbl">对端对象参数 <i>*</i></span>
                        <BlSelect v-model="selEditRule.link_dst_param" :options="objectParamOptions" clearable placeholder="目标对象引用参数" />
                      </label>
                    </template>
                  </div>
                </div>

                <!-- 2 属性映射 -->
                <div v-if="!selEditRule.kind.startsWith('delete')" class="adw-card">
                  <div class="adw-card-hd adw-card-hd-flex"><span>属性映射</span><span class="bl-muted" style="font-size:11.5px;font-weight:400">配置对象属性的取值来源</span></div>
                  <table class="bl-table ate-mini-table">
                    <thead><tr><th class="t-left">属性名称</th><th v-if="selEditRule.kind === 'modify_object'" class="t-left">操作</th><th class="t-left">值来源类型</th><th class="t-left">取值配置</th><th class="t-center">必填</th><th class="t-center">操作</th></tr></thead>
                    <tbody>
                      <tr v-for="(m, mi) in selEditRule.prop_mappings" :key="mi">
                        <td><BlSelect v-if="objRulePropOptions.length" v-model="m.property_code" :options="objRulePropOptions" size="sm" clearable placeholder="选择属性" /><input v-else class="bl-input bl-input-xs bl-mono" v-model="m.property_code" placeholder="属性编码" /></td>
                        <td v-if="selEditRule.kind === 'modify_object'"><BlSelect v-model="m.prop_operator" :options="PROP_OPERATOR_OPTS" size="sm" /></td>
                        <td><BlSelect v-model="m.value_source" :options="VALUE_SOURCE_OPTS" size="sm" /></td>
                        <td>
                          <BlSelect v-if="Number(m.value_source)===1" v-model="m.value_content" :options="formParamOptions" size="sm" clearable placeholder="表单参数" />
                          <BlSelect v-else-if="Number(m.value_source)===5" v-model="m.value_content" :options="objectParamOptions" size="sm" clearable placeholder="对象引用参数" />
                          <input v-else-if="Number(m.value_source)===2" class="bl-input bl-input-xs" v-model="m.value_content" placeholder="静态值" />
                          <span v-else class="bl-muted" style="font-size:12px;padding-left:4px">{{ Number(m.value_source)===3 ? '取动作提交时的当前登录用户' : '取动作提交时的服务器时间' }}</span>
                        </td>
                        <td class="t-center"><input type="checkbox" v-model="m.is_required" :true-value="1" :false-value="0" /></td>
                        <td class="t-center"><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="移除" @click="selEditRule.prop_mappings.splice(mi,1)" v-html="BL.icon('x', 11)"></button></td>
                      </tr>
                      <tr v-if="!selEditRule.prop_mappings.length"><td :colspan="selEditRule.kind === 'modify_object' ? 6 : 5" class="bl-muted" style="text-align:center;padding:12px;font-size:12px">暂无属性映射</td></tr>
                    </tbody>
                  </table>
                  <button class="fe-add-row" @click="addMapping(selEditRule)"><span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">添加属性映射</span></button>
                </div>

                <!-- 3 关联链接配置 (创建对象时顺带建链接) -->
                <div v-if="selEditRule.kind === 'create_object'" class="adw-card">
                  <div class="adw-card-hd adw-card-hd-flex"><span>关联链接配置(多对多)</span><span class="bl-muted" style="font-size:11.5px;font-weight:400">创建对象同时建立多对多关联</span></div>
                  <div v-for="(lk, li) in selEditRule.obj_links" :key="li" class="fe-link-item">
                    <div class="fe-link-hd">
                      <span v-html="BL.icon('link', 13, '#14C9C9')"></span>
                      <span class="fe-link-lbl">链接类型</span>
                      <BlSelect v-model="lk.link_type_code" :options="linkTypeOptions" size="sm" clearable placeholder="选择链接类型" style="width:220px" />
                      <span class="bl-tag" style="margin-left:6px">{{ linkCardLabel(lk.link_type_code) }}</span>
                      <span style="flex:1"></span>
                      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon at-del-op" title="移除链接" @click="selEditRule.obj_links.splice(li,1)" v-html="BL.icon('trash2', 12)"></button>
                    </div>
                    <div class="fe-link-bd">
                      <div class="fe-row"><span class="fe-lbl">本端对象</span><span class="bl-muted" style="font-size:12.5px">当前创建的{{ objRuleClassName || '对象' }}(本规则生成)</span></div>
                      <div class="fe-row"><span class="fe-lbl">对端对象来源</span><BlSelect v-model="lk.peer_param" :options="objectParamOptions" size="sm" clearable :placeholder="objectParamOptions.length ? '表单参数:对端对象' : '暂无对象引用参数,请先在表单添加'" style="flex:1;max-width:320px" /></div>
                    </div>
                  </div>
                  <div v-if="!selEditRule.obj_links.length" class="bl-muted" style="font-size:12px;padding:8px 2px">暂无关联链接</div>
                  <button class="fe-add-row" @click="addObjLink(selEditRule)"><span v-html="BL.icon('link', 12)"></span><span style="margin-left:4px">添加多对多链接</span></button>
                </div>

                <!-- 删除类规则说明 -->
                <div v-if="selEditRule.kind.startsWith('delete')" class="fe-warn">⚠ 删除规则只支持通过对象引用参数指定已存在的目标,不能删除本次提交中新建的临时对象;删除对象会级联移除其所有链接。</div>
              </div>
            </template>

            <!-- ===== 函数规则 完整编辑态 ===== -->
            <template v-else-if="ruleView === 'func' && selEditRule">
              <div class="fd-detail-hd">
                <div class="fd-phd2" style="justify-content:space-between">
                  <div class="bl-row" style="gap:8px;min-width:0">
                    <button class="fd-back" @click="backToRules"><span v-html="BL.icon('chevronLeft', 13)"></span><span>返回规则</span></button>
                    <span class="fd-phd2-sep"></span>
                    <span class="rl-ic" :style="{ background: kindMeta('function').color }" v-html="BL.icon('code', 12, '#fff')"></span>
                    <input class="fd-pname2" v-model="selEditRule.rule_name" placeholder="函数规则名称" />
                  </div>
                  <button class="bl-btn bl-btn-sm bl-btn-primary" @click="onSave" :disabled="saving">保存</button>
                </div>
              </div>
              <div class="fd-detail-body">
                <div class="fe-warn">⚠ 配置函数规则后,不可叠加其他 Ontology 编辑规则(创建 / 修改 / 删除对象、创建 / 删除链接均不可用),所有对象编辑逻辑由本函数全权处理;可与通知 / Webhook 等副作用规则共存。</div>

                <!-- 1 基础配置 -->
                <div class="adw-card"><div class="adw-card-hd">基础配置</div>
                  <div class="adw-grid">
                    <label class="adw-fld"><span class="adw-lbl">绑定函数 <i>*</i></span><div class="adw-color-row"><input class="bl-input bl-mono" style="flex:1" v-model="selEditRule.func_code" placeholder="函数编码" /><button class="bl-btn bl-btn-sm" @click="BL.info('函数选择器后续接入')">选择</button></div></label>
                    <label class="adw-fld"><span class="adw-lbl">所属本体</span><input class="bl-input" :value="subjectSummary" disabled /></label>
                    <label class="adw-fld"><span class="adw-lbl">函数版本 <i>*</i></span><input class="bl-input bl-mono" v-model="selEditRule.func_version" placeholder="v1.0.0" /></label>
                    <label class="adw-fld"><span class="adw-lbl">版本升级策略</span><BlSelect v-model="selEditRule.func_autoupgrade" :options="FUNC_UPGRADE_OPTS" /></label>
                    <label class="adw-fld"><span class="adw-lbl">执行身份</span><BlSelect v-model="selEditRule.func_exec_identity" :options="FUNC_IDENTITY_OPTS" /></label>
                  </div>
                </div>

                <!-- 2 入参映射配置 (必选 / 可选分组) -->
                <div class="adw-card"><div class="adw-card-hd">入参映射配置 <span class="bl-muted" style="font-size:11px;font-weight:400">(值来源与入参类型不匹配会标红)</span></div>
                  <div class="fe-subhd">必选入参 <span class="bl-muted">(函数定义决定,不可删除)</span></div>
                  <table class="bl-table ate-mini-table"><thead><tr><th class="t-left">参数名</th><th class="t-left">类型</th><th class="t-center">必填</th><th class="t-left">值来源类型</th><th class="t-left">取值配置</th><th></th></tr></thead>
                    <tbody>
                      <tr v-for="(fp, fi) in selEditRule.func_params.filter(p=>p.required)" :key="'req'+fi" :class="{ 'fe-mismatch': paramTypeMismatch(selEditRule, fp) }">
                        <td><input class="bl-input bl-input-xs bl-mono" v-model="fp.name" placeholder="param_name" /></td>
                        <td><BlSelect v-model="fp.param_type" :options="FUNC_PTYPE_OPTS" size="sm" /></td>
                        <td class="t-center"><span class="bl-tag bl-tag-danger">必填</span></td>
                        <td><BlSelect v-model="fp.value_source" :options="VALUE_SOURCE_OPTS" size="sm" /></td>
                        <td><BlSelect v-if="Number(fp.value_source)===1" v-model="fp.value_content" :options="formParamOptions" size="sm" clearable placeholder="选表单参数" /><BlSelect v-else-if="Number(fp.value_source)===5" v-model="fp.value_content" :options="objectParamOptions" size="sm" clearable placeholder="选对象引用参数" /><input v-else-if="Number(fp.value_source)===2" class="bl-input bl-input-xs" v-model="fp.value_content" placeholder="静态值" /><span v-else class="bl-muted" style="font-size:12px;padding-left:4px">自动</span></td>
                        <td class="t-center"><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="selEditRule.func_params.splice(selEditRule.func_params.indexOf(fp),1)" v-html="BL.icon('x', 11)"></button></td>
                      </tr>
                      <tr v-if="!selEditRule.func_params.some(p=>p.required)"><td colspan="6" class="bl-muted" style="text-align:center;padding:8px;font-size:12px">暂无必选入参</td></tr>
                    </tbody></table>
                  <button class="bl-btn bl-btn-text bl-btn-sm" style="margin-top:4px" @click="addFuncParamRow(selEditRule, 1)"><span v-html="BL.icon('plus', 11)"></span><span style="margin-left:3px">添加必选入参</span></button>

                  <div class="fe-subhd" style="margin-top:14px">可选入参</div>
                  <table class="bl-table ate-mini-table"><thead><tr><th class="t-left">参数名</th><th class="t-left">类型</th><th class="t-center">必填</th><th class="t-left">值来源类型</th><th class="t-left">取值配置</th><th></th></tr></thead>
                    <tbody>
                      <tr v-for="(fp, fi) in selEditRule.func_params.filter(p=>!p.required)" :key="'opt'+fi" :class="{ 'fe-mismatch': paramTypeMismatch(selEditRule, fp) }">
                        <td><input class="bl-input bl-input-xs bl-mono" v-model="fp.name" placeholder="param_name" /></td>
                        <td><BlSelect v-model="fp.param_type" :options="FUNC_PTYPE_OPTS" size="sm" /></td>
                        <td class="t-center"><span class="bl-tag">可选</span></td>
                        <td><BlSelect v-model="fp.value_source" :options="VALUE_SOURCE_OPTS" size="sm" /></td>
                        <td><BlSelect v-if="Number(fp.value_source)===1" v-model="fp.value_content" :options="formParamOptions" size="sm" clearable placeholder="选表单参数" /><BlSelect v-else-if="Number(fp.value_source)===5" v-model="fp.value_content" :options="objectParamOptions" size="sm" clearable placeholder="选对象引用参数" /><input v-else-if="Number(fp.value_source)===2" class="bl-input bl-input-xs" v-model="fp.value_content" placeholder="静态值" /><span v-else class="bl-muted" style="font-size:12px;padding-left:4px">自动</span></td>
                        <td class="t-center"><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="selEditRule.func_params.splice(selEditRule.func_params.indexOf(fp),1)" v-html="BL.icon('x', 11)"></button></td>
                      </tr>
                      <tr v-if="!selEditRule.func_params.some(p=>!p.required)"><td colspan="6" class="bl-muted" style="text-align:center;padding:8px;font-size:12px">暂无可选入参</td></tr>
                    </tbody></table>
                  <button class="bl-btn bl-btn-text bl-btn-sm" style="margin-top:4px" @click="addFuncParamRow(selEditRule, 0)"><span v-html="BL.icon('plus', 11)"></span><span style="margin-left:3px">添加可选入参</span></button>
                </div>

                <!-- 3 函数代码预览 -->
                <div class="adw-card"><div class="adw-card-hd" style="display:flex;justify-content:space-between;align-items:center;border-left:0;padding-left:0"><span style="border-left:3px solid var(--bl-primary);padding-left:8px">函数代码预览</span><a class="fe-repo" @click="BL.info('跳转代码仓库,后续接入')"><span v-html="BL.icon('code', 11)"></span> 在代码仓库中编辑</a></div>
                  <CodeEditor :model-value="funcCodePreview(selEditRule)" language="text" disabled :rows="8" />
                </div>

                <!-- 4 执行配置 -->
                <div class="adw-card"><div class="adw-card-hd">执行配置</div>
                  <div class="adw-grid">
                    <label class="adw-fld"><span class="adw-lbl">执行身份</span><BlSelect v-model="selEditRule.func_exec_identity" :options="FUNC_IDENTITY_OPTS" /></label>
                    <label class="adw-fld"><span class="adw-lbl">异常处理策略</span><BlSelect v-model="selEditRule.func_error_strategy" :options="FUNC_ERR_OPTS" /></label>
                    <label class="adw-fld"><span class="adw-lbl">超时时长 (秒)</span><input class="bl-input" type="number" v-model="selEditRule.func_timeout" /></label>
                    <label class="adw-fld"><span class="adw-lbl">最大重试次数</span><input class="bl-input" type="number" v-model="selEditRule.func_retry" /></label>
                  </div>
                  <div class="adw-switch-row" style="margin-top:12px">
                    <label class="adw-sw"><span class="adw-showsw rl-sw" :class="{ 'is-on': selEditRule.func_concurrent===1 }" @click="selEditRule.func_concurrent = selEditRule.func_concurrent?0:1"><span class="adw-showsw-dot"></span></span> 并发执行</label>
                    <label class="adw-sw"><span class="adw-showsw rl-sw" :class="{ 'is-on': selEditRule.func_return_attachment===1 }" @click="selEditRule.func_return_attachment = selEditRule.func_return_attachment?0:1"><span class="adw-showsw-dot"></span></span> 返回附件(可写回本体或推送到前端)</label>
                  </div>
                </div>

                <!-- 5 异常映射 -->
                <div class="adw-card"><div class="adw-card-hd">异常映射 <span class="bl-muted" style="font-size:11px;font-weight:400">(函数抛出异常码 → 前端提示文案)</span></div>
                  <table class="bl-table ate-mini-table"><thead><tr><th class="t-left">异常码</th><th class="t-left">提示文案</th><th></th></tr></thead>
                    <tbody>
                      <tr v-for="(ex, ei) in selEditRule.func_exceptions" :key="ei">
                        <td style="width:180px"><input class="bl-input bl-input-xs bl-mono" v-model="ex.code" placeholder="如 E_PERMISSION" /></td>
                        <td><input class="bl-input bl-input-xs" v-model="ex.message" placeholder="向用户展示的提示" /></td>
                        <td class="t-center"><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="selEditRule.func_exceptions.splice(ei,1)" v-html="BL.icon('x', 11)"></button></td>
                      </tr>
                      <tr v-if="!selEditRule.func_exceptions.length"><td colspan="3" class="bl-muted" style="text-align:center;padding:8px;font-size:12px">暂无异常映射,未匹配的异常将展示默认错误提示</td></tr>
                    </tbody></table>
                  <button class="bl-btn bl-btn-text bl-btn-sm" style="margin-top:4px" @click="addFuncException(selEditRule)"><span v-html="BL.icon('plus', 11)"></span><span style="margin-left:3px">添加异常映射</span></button>
                </div>
              </div>
            </template>

            <!-- ===== 通知规则 完整编辑态 (5.3.8) ===== -->
            <template v-else-if="ruleView === 'notify' && selEditRule">
              <div class="fd-detail-hd">
                <div class="fd-phd2" style="justify-content:space-between">
                  <div class="bl-row" style="gap:8px;min-width:0">
                    <button class="fd-back" @click="backToRules"><span v-html="BL.icon('chevronLeft', 13)"></span><span>返回规则</span></button>
                    <span class="fd-phd2-sep"></span>
                    <span class="rl-ic" :style="{ background: kindMeta('notification').color }" v-html="BL.icon('bell', 12, '#fff')"></span>
                    <input class="fd-pname2" v-model="selEditRule.rule_name" placeholder="通知规则名称" />
                  </div>
                  <button class="bl-btn bl-btn-sm bl-btn-primary" @click="onSave" :disabled="saving">保存</button>
                </div>
              </div>
              <div class="fd-detail-body">
                <div class="fe-warn">通知属于副作用规则,在所有编辑参数明执行成功、事务提交完成后触发;通知发送失败不影响主操作结果。</div>

                <!-- 1 收件人配置 -->
                <div class="adw-card"><div class="adw-card-hd">收件人配置</div>
                  <div class="fe-row"><span class="fe-lbl">收件人来源</span><BlSelect v-model="selEditRule.notify_recipient_source" :options="NOTIFY_RECIPIENT_SRC" style="flex:1" /></div>
                  <div v-if="selEditRule.notify_recipient_source === 'object_prop'" class="fe-row" style="margin-top:12px">
                    <span class="fe-lbl">对象参数</span>
                    <BlSelect v-model="selEditRule.notify_recipient_object_param" :options="objectParamOptions" size="sm" clearable placeholder="对象参数" style="width:180px" />
                    <span class="fe-lbl fe-lbl-inline">用户ID属性</span>
                    <BlSelect v-model="selEditRule.notify_recipient_user_attr" :options="[]" size="sm" clearable placeholder="选择(待接口)" style="flex:1;max-width:260px" />
                  </div>
                  <div v-else-if="selEditRule.notify_recipient_source === 'param'" class="fe-row" style="margin-top:12px"><span class="fe-lbl">收件人参数</span><BlSelect v-model="selEditRule.notify_recipient_object_param" :options="formParamOptions" size="sm" clearable placeholder="选表单参数" style="flex:1;max-width:320px" /></div>
                  <div v-else class="fe-row" style="margin-top:12px"><span class="fe-lbl">静态收件人</span><input class="bl-input bl-input-sm" style="flex:1" v-model="selEditRule.notify_to" placeholder="用户 / 用户组标识,多个用逗号分隔" /></div>
                </div>

                <!-- 2 内容配置 (模板 / 来自函数) -->
                <div class="adw-card"><div class="adw-card-hd">内容配置</div>
                  <div class="fd-tabs2">
                    <button :class="['fd-tab2', selEditRule.notify_content_mode !== 'function' && 'is-on']" @click="selEditRule.notify_content_mode = 'desc'">模板</button>
                    <button :class="['fd-tab2', selEditRule.notify_content_mode === 'function' && 'is-on']" @click="selEditRule.notify_content_mode = 'function'">来自函数</button>
                  </div>
                  <template v-if="selEditRule.notify_content_mode !== 'function'">
                    <div class="fe-row" style="margin-top:14px"><span class="fe-lbl">主题</span><input class="bl-input" style="flex:1" v-model="selEditRule.notify_title" placeholder="工单 {{ticketId}} 优先级已变更" /></div>
                    <div class="fe-row fe-row-top" style="margin-top:12px"><span class="fe-lbl">正文</span><textarea class="bl-textarea" style="flex:1" v-model="selEditRule.notify_content" rows="4" placeholder="您好 {{recipient_first_name}},{{current_user_first_name}} 已将工单 {{ticketId}} 的优先级变更为 {{priority}}。请及时跟进处理。"></textarea></div>
                  </template>
                  <template v-else>
                    <div class="fe-row" style="margin-top:14px"><span class="fe-lbl">通知函数</span><input class="bl-input bl-input-sm bl-mono" style="flex:1;max-width:320px" v-model="selEditRule.notify_func_code" placeholder="函数编码" /><button class="bl-btn bl-btn-sm" style="margin-left:8px" @click="BL.info('函数选择器后续接入')">选择</button></div>
                    <div class="adw-card-hd" style="margin-top:12px;display:flex;justify-content:space-between;align-items:center;border-left:0;padding-left:0"><span style="border-left:3px solid var(--bl-primary);padding-left:8px">消息代码预览</span><a class="fe-repo" @click="BL.info('跳转代码仓库,后续接入')"><span v-html="BL.icon('code', 11)"></span> 在代码仓库中编辑</a></div>
                    <CodeEditor :model-value="notifyCodePreview(selEditRule)" language="text" disabled :rows="8" />
                  </template>
                </div>

                <!-- 3 通知渠道 -->
                <div class="adw-card"><div class="adw-card-hd">通知渠道</div>
                  <div class="fe-row"><span class="fe-lbl">启用渠道</span>
                    <label class="adw-sw" style="margin-right:22px"><input type="checkbox" v-model="selEditRule.notify_ch_push" :true-value="1" :false-value="0" /> 平台内推送</label>
                    <label class="adw-sw" style="margin-right:22px"><input type="checkbox" v-model="selEditRule.notify_ch_email" :true-value="1" :false-value="0" /> 邮件通知</label>
                    <label class="adw-sw"><input type="checkbox" v-model="selEditRule.notify_ch_sms" :true-value="1" :false-value="0" /> 短信通知</label>
                  </div>
                </div>

                <!-- 4 链接配置 -->
                <div class="adw-card"><div class="adw-card-hd adw-card-hd-flex"><span>链接配置</span><span class="adw-showsw rl-sw" :class="{ 'is-on': selEditRule.notify_link_enabled === 1 }" @click="selEditRule.notify_link_enabled = selEditRule.notify_link_enabled ? 0 : 1"><span class="adw-showsw-dot"></span></span></div>
                  <div v-if="selEditRule.notify_link_enabled" class="fe-row fe-row-wrap">
                    <span class="fe-lbl">链接类型</span><BlSelect v-model="selEditRule.notify_link_type" :options="NOTIFY_LINK_TYPES" size="sm" style="width:150px" />
                    <span class="fe-lbl fe-lbl-inline">链接目标</span><BlSelect v-model="selEditRule.notify_link_target" :options="objectParamOptions" size="sm" clearable placeholder="目标对象" style="width:150px" />
                    <span class="fe-lbl fe-lbl-inline">按钮文字</span><input class="bl-input bl-input-sm" style="flex:1;min-width:160px;max-width:220px" v-model="selEditRule.notify_link_text" placeholder="查看工单详情" />
                  </div>
                  <div v-else class="bl-muted" style="font-size:12px">未启用,通知不带跳转按钮。</div>
                </div>

                <!-- 5 高级配置 -->
                <div class="adw-card"><div class="adw-card-hd">高级配置</div>
                  <div class="adw-card-hd-flex" style="border-left:0;padding-left:0;margin-bottom:2px"><span style="font-size:13px;font-weight:600;color:var(--bl-text-1)">自定义邮件 HTML 内容</span><span class="adw-showsw rl-sw" :class="{ 'is-on': selEditRule.notify_custom_html === 1 }" @click="selEditRule.notify_custom_html = selEditRule.notify_custom_html ? 0 : 1"><span class="adw-showsw-dot"></span></span></div>
                  <textarea v-if="selEditRule.notify_custom_html" class="bl-textarea bl-mono" style="margin-top:10px" v-model="selEditRule.notify_html_content" rows="4" placeholder="<html>… 支持参数占位 …</html>"></textarea>
                  <div class="fe-row fe-row-wrap" style="margin-top:16px">
                    <span class="fe-lbl">权限范围</span>
                    <label class="fe-radio"><input type="radio" value="all" v-model="selEditRule.notify_permission_scope" /><span class="fe-radio-lbl">要求所有用户具有权限(默认)</span></label>
                    <label class="fe-radio" style="margin-left:24px"><input type="radio" value="authorized" v-model="selEditRule.notify_permission_scope" /><span class="fe-radio-lbl">要求任意用户具有权限</span></label>
                  </div>
                </div>
              </div>
            </template>

            <!-- ===== Webhook 完整编辑态 (5.3.9) ===== -->
            <template v-else-if="ruleView === 'webhook' && selEditRule">
              <div class="fd-detail-hd">
                <div class="fd-phd2" style="justify-content:space-between">
                  <div class="bl-row" style="gap:8px;min-width:0">
                    <button class="fd-back" @click="backToRules"><span v-html="BL.icon('chevronLeft', 13)"></span><span>返回规则</span></button>
                    <span class="fd-phd2-sep"></span>
                    <span class="rl-ic" :style="{ background: kindMeta('webhook').color }" v-html="BL.icon('zap', 12, '#fff')"></span>
                    <input class="fd-pname2" v-model="selEditRule.rule_name" placeholder="Webhook 规则名称" />
                  </div>
                  <button class="bl-btn bl-btn-sm bl-btn-primary" @click="onSave" :disabled="saving">保存 Webhook 规则</button>
                </div>
              </div>
              <div class="fd-detail-body">
                <div class="fe-warn">Webhook 用于向外部系统发送 HTTP 请求,实现跨系统数据同步与流程联动;分为回写(事务提交前执行)和副作用(事务提交后执行)两种类型。</div>

                <!-- 1 Webhook 类型 -->
                <div class="adw-card"><div class="adw-card-hd">Webhook 类型</div>
                  <div class="wh-type-grid">
                    <button v-for="t in WH_SUBTYPES" :key="t.value" :class="['wh-type-card', selEditRule.wh_subtype===t.value && 'is-on']" @click="selEditRule.wh_subtype=t.value">
                      <span class="wh-type-ic" :style="{ background: selEditRule.wh_subtype===t.value ? kindMeta('webhook').color : 'var(--bl-bg-3)' }" v-html="BL.icon(t.icon, 13, '#fff')"></span>
                      <span class="wh-type-lbl">{{ t.label }}</span>
                      <span v-if="selEditRule.wh_subtype===t.value" class="wh-type-chk" v-html="BL.icon('check', 14)"></span>
                    </button>
                  </div>
                  <div class="bl-muted" style="font-size:12px;line-height:1.6;margin-top:10px">{{ WH_SUBTYPES.find(t=>t.value===selEditRule.wh_subtype)?.desc }}</div>
                </div>

                <!-- 2 基本信息 -->
                <div class="adw-card"><div class="adw-card-hd">Webhook 基本信息</div>
                  <div class="adw-grid">
                    <label class="adw-fld"><span class="adw-lbl">选择 Webhook <i>*</i></span><div class="adw-color-row"><input class="bl-input" style="flex:1" v-model="selEditRule.wh_code" placeholder="选择已注册的 Webhook" /><button class="bl-btn bl-btn-sm" @click="BL.info('Webhook 选择器后续接入')">选择</button></div></label>
                    <label class="adw-fld"><span class="adw-lbl">版本</span><input class="bl-input bl-mono" v-model="selEditRule.wh_version" placeholder="v1" /></label>
                  </div>
                </div>

                <!-- 3 输入参数 -->
                <div class="adw-card"><div class="adw-card-hd adw-card-hd-flex"><span>Webhook 输入参数</span><a class="fe-repo" @click="BL.info('跳转动作参数定义,后续接入')">跳过动作参数定义</a></div>
                  <label class="fd-ck" style="display:flex;gap:8px;margin-bottom:10px"><input type="checkbox" :checked="selEditRule.wh_input_mode==='function'" @change="selEditRule.wh_input_mode = selEditRule.wh_input_mode==='function' ? 'manual' : 'function'" /> 选择返回 Webhook 预期输入约束的函数</label>
                  <div v-if="selEditRule.wh_input_mode==='function'" class="adw-grid" style="margin-bottom:12px">
                    <label class="adw-fld"><span class="adw-lbl">函数</span><div class="adw-color-row"><input class="bl-input bl-mono" style="flex:1" v-model="selEditRule.wh_input_func" placeholder="returnWebhookInput" /><button class="bl-btn bl-btn-sm" @click="BL.info('函数选择器后续接入')">选择</button></div></label>
                    <label class="adw-fld"><span class="adw-lbl">版本</span><input class="bl-input bl-mono" v-model="selEditRule.wh_input_func_version" placeholder="0.22.0" /></label>
                  </div>
                  <div class="fe-subhd">必须输入项</div>
                  <table class="bl-table ate-mini-table"><thead><tr><th class="t-left">输入项名</th><th class="t-left">类型</th><th class="t-left">值来源</th><th class="t-left">取值配置</th><th></th></tr></thead>
                    <tbody>
                      <tr v-for="(p, pi) in selEditRule.wh_params" :key="pi">
                        <td><input class="bl-input bl-input-xs bl-mono" v-model="p.name" placeholder="input_name" /></td>
                        <td><BlSelect v-model="p.param_type" :options="FUNC_PTYPE_OPTS" size="sm" /></td>
                        <td><BlSelect v-model="p.value_source" :options="VALUE_SOURCE_OPTS" size="sm" /></td>
                        <td>
                          <BlSelect v-if="Number(p.value_source)===1" v-model="p.value_content" :options="formParamOptions" size="sm" clearable placeholder="选表单参数" />
                          <BlSelect v-else-if="Number(p.value_source)===5" v-model="p.value_content" :options="objectParamOptions" size="sm" clearable placeholder="选对象引用参数" />
                          <input v-else-if="Number(p.value_source)===2" class="bl-input bl-input-xs" v-model="p.value_content" placeholder="静态值" />
                          <span v-else class="bl-muted" style="font-size:12px;padding-left:4px">自动</span>
                        </td>
                        <td class="t-center"><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="selEditRule.wh_params.splice(pi,1)" v-html="BL.icon('x', 11)"></button></td>
                      </tr>
                      <tr v-if="!selEditRule.wh_params.length"><td colspan="5" class="bl-muted" style="text-align:center;padding:8px;font-size:12px">暂无输入项</td></tr>
                    </tbody></table>
                  <button class="bl-btn bl-btn-text bl-btn-sm" style="margin-top:4px" @click="addWhParam(selEditRule)"><span v-html="BL.icon('plus', 11)"></span><span style="margin-left:3px">添加输入项</span></button>
                  <template v-if="selEditRule.wh_input_mode==='function'">
                    <div class="adw-card-hd" style="margin-top:14px;display:flex;justify-content:space-between;align-items:center;border-left:0;padding-left:0"><span style="border-left:3px solid var(--bl-primary);padding-left:8px">函数代码预览</span><a class="fe-repo" @click="BL.info('跳转代码仓库,后续接入')"><span v-html="BL.icon('code', 11)"></span> 在代码仓库中编辑</a></div>
                    <CodeEditor :model-value="whInputCodePreview(selEditRule)" language="text" disabled :rows="10" />
                  </template>
                </div>

                <!-- 4 执行说明 -->
                <div class="adw-card"><div class="adw-card-hd">执行说明</div>
                  <div v-if="selEditRule.wh_subtype==='writeback'" class="fe-hints">
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
          </div>

          <!-- ========= 表单 (P1 移植参数表; P2 升级为设计器) ========= -->
          <div v-else-if="activeMenu === 'form'" class="adw-page is-detail">
            <!-- ===== 表单列表页 ===== -->
            <template v-if="formView === 'list'">
              <div class="fd-detail-hd">
                <div class="fd-phd2" style="justify-content:space-between">
                  <div class="adw-page-title">表单内容</div>
                  <div class="bl-row" style="gap:8px">
                    <button v-if="form.m_type === 1 && form.object_class_id" class="bl-btn bl-btn-sm" @click="importParams"><span v-html="BL.icon('download', 12)"></span><span style="margin-left:4px">从属性导入</span></button>
                    <button class="bl-btn bl-btn-sm bl-btn-primary" @click="onSave" :disabled="saving">保存</button>
                  </div>
                </div>
              </div>
              <div class="fd-detail-body">
              <div v-for="sec in sections" :key="sec" class="fd-part" @dragover.prevent @drop="onFieldDropSection(sec)">
                <div class="fd-part-hd"><span v-html="BL.icon('chevronDown', 12)"></span><span class="fd-part-name" title="点击重命名分区" @click.stop="renameSection(sec)">{{ sec }}<span class="fd-part-edit" v-html="BL.icon('edit', 11)"></span></span><span class="fd-part-count">{{ paramsOfSection(sec).length }}</span><button v-if="sections.length > 1" class="fd-part-del" title="删除分区" @click.stop="removeSection(sec)" v-html="BL.icon('trash2', 12)"></button></div>
                <div class="fd-part-body">
                  <div v-for="x in paramsOfSection(sec)" :key="x.i" class="fd-row" :class="{ 'is-dragging': fDragIdx === x.i }"
                       draggable="true" @dragstart="onFieldDragStart(x.i, $event)" @dragover.prevent @drop.stop="onFieldDrop(x.p, sec)" @dragend="fDragIdx = null"
                       @click="openParam(x.i)">
                    <span class="fd-grip" v-html="BL.icon('move', 12)"></span>
                    <span class="fd-dt" :style="{ background: dtMeta(x.p.param_type).color }" v-html="BL.icon(dtMeta(x.p.param_type).icon, 12, '#fff')"></span>
                    <div class="fd-row-txt">
                      <div class="fd-row-name bl-truncate">{{ x.p.param_name || x.p.param_code || '未命名参数' }}</div>
                      <div v-if="isRefByRule(x.p.param_code)" class="fd-row-warn">已在规则中引用</div>
                    </div>
                    <span v-if="!x.p.param_code" class="bl-tag at-mini-tag">新建</span>
                    <span v-else-if="x.p.param_type === 'object'" class="bl-tag at-mini-tag">对象引用</span>
                    <span class="fd-disp" :title="'显示组件:' + displayMeta(x.p.display_type).label"><span class="fd-disp-ic" v-html="BL.icon(displayMeta(x.p.display_type).icon, 13)"></span><span class="fd-disp-txt">{{ displayMeta(x.p.display_type).label }}</span></span>
                    <span class="fd-chev" v-html="BL.icon('chevronRight', 12)"></span>
                  </div>
                  <div v-if="!paramsOfSection(sec).length" class="fd-part-empty">该分区暂无参数,可拖入或点下方添加</div>
                  <button class="fd-part-add" @click="addParam(sec)"><span v-html="BL.icon('plus', 12)"></span><span>添加参数到本分区</span></button>
                </div>
              </div>
              <button class="rl-add-btn" style="margin-top:12px" @click="addSection"><span v-html="BL.icon('plus', 14)"></span><span style="margin-left:5px">添加分区</span></button>
              <div class="adw-card gd-card" style="margin-top:16px">
                <div class="adw-card-hd adw-card-hd-flex"><span>全局显示配置<span class="bl-tag at-mini-tag" style="margin-left:8px">表单级默认</span></span>
                  <span class="bl-muted" style="font-size:12px;font-weight:400">{{ formParams.length - dspOverParams.length }} / {{ formParams.length }} 个参数继承全局,{{ dspOverParams.length }} 个已单独覆盖</span></div>
                <div class="fd-inh-tip">此处的配置作为<b>全表单参数的默认值</b>;单个参数可在其「显示」标签页取消继承、单独覆盖,<b>参数级优先于全局</b>。修改全局配置会立即影响所有仍在继承的参数。</div>
                <div class="gd-sub">通用布局</div>
                <div class="gd-grid">
                  <label class="adw-fld"><span class="adw-lbl">字段宽度</span><BlSelect v-model="globalConf.width" :options="DSP_WIDTHS" /></label>
                  <label class="adw-fld"><span class="adw-lbl">标签位置</span><BlSelect v-model="globalConf.labelPos" :options="DSP_LABEL_POS" /></label>
                  <label class="adw-fld"><span class="adw-lbl">清空按钮</span><span class="adw-showsw" :class="{ 'is-on': globalConf.clear === 1 }" @click="globalConf.clear = globalConf.clear ? 0 : 1"><span class="adw-showsw-dot"></span></span></label>
                  <label class="adw-fld"><span class="adw-lbl">标签宽度</span><BlSelect v-model="globalConf.labelW" :disabled="globalConf.labelPos !== 'left'" :options="DSP_LABEL_WS" /></label>
                  <label class="adw-fld"><span class="adw-lbl">标签对齐</span><BlSelect v-model="globalConf.labelAlign" :disabled="globalConf.labelPos !== 'left'" :options="DSP_LABEL_ALIGNS" /></label>
                  <div class="adw-fld"><span class="bl-muted" style="font-size:12px">{{ globalConf.labelPos === 'left' ? (globalConf.labelW === 'auto' ? `当前自适应宽度约 ${autoLabelW()}px` : '固定宽度可保证各字段控件左边缘对齐') : '标签宽度 / 对齐仅在「标签位置 = 左侧显示」时生效' }}</span></div>
                </div>
                <div class="gd-sub">辅助提示</div>
                <div class="gd-grid">
                  <label class="adw-fld"><span class="adw-lbl">帮助文案</span><span class="adw-showsw" :class="{ 'is-on': globalConf.helpOn === 1 }" @click="globalConf.helpOn = globalConf.helpOn ? 0 : 1"><span class="adw-showsw-dot"></span></span></label>
                  <label class="adw-fld" style="grid-column:2/-1"><span class="adw-lbl">默认文案</span><input class="bl-input" v-model="globalConf.helpText" placeholder="全局默认帮助文案" /></label>
                </div>
                <div class="gd-sub">表单级样式<span class="bl-muted" style="font-weight:400;font-size:11px;margin-left:6px">无参数级覆盖</span></div>
                <div class="gd-grid">
                  <label class="adw-fld"><span class="adw-lbl">必填标识</span><BlSelect v-model="globalConf.reqMark" :options="[{value:'prefix',label:'名称前红色星号'},{value:'suffix',label:'名称后红色星号'}]" /></label>
                  <label class="adw-fld"><span class="adw-lbl">字段间距</span><BlSelect v-model="globalConf.density" :options="[{value:'compact',label:'紧凑'},{value:'normal',label:'标准'},{value:'loose',label:'宽松'}]" /></label>
                  <label class="adw-fld"><span class="adw-lbl">分区标题</span><span class="adw-showsw" :class="{ 'is-on': globalConf.sectionTitle === 1 }" @click="globalConf.sectionTitle = globalConf.sectionTitle ? 0 : 1"><span class="adw-showsw-dot"></span></span><span class="bl-muted" style="font-size:12px;margin-left:8px">在表单中展示分区标题</span></label>
                </div>
                <div class="gd-sub">提交交互</div>
                <div class="gd-grid">
                  <label class="adw-fld"><span class="adw-lbl">自定义提交按钮</span><span class="adw-showsw" :class="{ 'is-on': globalConf.custom_submit === 1 }" @click="globalConf.custom_submit = globalConf.custom_submit ? 0 : 1"><span class="adw-showsw-dot"></span></span></label>
                  <label class="adw-fld"><span class="adw-lbl">自定义成功提示</span><span class="adw-showsw" :class="{ 'is-on': globalConf.custom_success === 1 }" @click="globalConf.custom_success = globalConf.custom_success ? 0 : 1"><span class="adw-showsw-dot"></span></span></label>
                </div>
                <template v-if="dspOverParams.length">
                  <div class="gd-sub">已覆盖全局的参数({{ dspOverParams.length }})</div>
                  <div v-for="x in dspOverParams" :key="x.i" class="gd-over" @click="openParam(x.i)">
                    <span class="fd-dt" :style="{ background: dtMeta(x.p.param_type).color }" v-html="BL.icon(dtMeta(x.p.param_type).icon, 11, '#fff')"></span>
                    <b>{{ x.p.param_name || x.p.param_code }}</b>
                    <span class="bl-muted" style="font-size:12px">覆盖了 {{ dspOverLabels(x.p) }}</span>
                    <span style="flex:1"></span>
                    <button class="bl-btn bl-btn-text bl-btn-sm" @click.stop="resetDsp(x.p)">恢复继承</button>
                  </div>
                </template>
              </div>
              </div>
            </template>

            <!-- ===== 参数详情页 ===== -->
            <template v-else-if="selParam">
              <!-- 固定头部: 参数信息行 + 锚点导航 -->
              <div class="fd-detail-hd">
                <div class="fd-phd2">
                  <button class="fd-back" title="返回表单内容" @click="backToList"><span v-html="BL.icon('chevronLeft', 13)"></span><span>返回</span></button>
                  <span class="fd-phd2-sep"></span>
                  <span class="fd-dt" :style="{ background: dtMeta(selParam.param_type).color }" v-html="BL.icon(dtMeta(selParam.param_type).icon, 13, '#fff')"></span>
                  <input class="fd-pname2" v-model="selParam.param_name" placeholder="参数名称" />
                  <span class="bl-mono bl-muted" style="font-size:12px;flex-shrink:0">{{ selParam.param_code || '未设编码' }}</span>
                  <span style="flex:1"></span>
                  <span class="bl-tag" :style="{ background:`color-mix(in srgb, ${dtMeta(selParam.param_type).color} 12%, transparent)`, color:dtMeta(selParam.param_type).color }">{{ dtMeta(selParam.param_type).label }}</span>
                  <button class="bl-btn bl-btn-text bl-btn-icon at-del-op" title="删除参数" @click="removeParamAt(selIdx)" v-html="BL.icon('trash2', 13)"></button>
                </div>
                <div class="fd-tabs">
                  <button v-for="t in DETAIL_TABS" :key="t.k" :class="['fd-tab-btn', detailTab === t.k && 'is-on']" @click="switchDetailTab(t.k)">{{ t.label }}</button>
                </div>
              </div>

              <!-- 下方独立滚动区: 全部区块从上到下 -->
              <div class="fd-detail-body" ref="scrollEl">
              <div class="fd-tab-wrap">
                <!-- 值 -->
                <section v-show="detailTab === 'value'" class="fd-sec">
                  <div class="adw-card"><div class="adw-card-hd">参数类型</div>
                    <div class="adw-grid">
                      <label class="adw-fld"><span class="adw-lbl">数据类型</span><BlSelect v-model="selParam.param_type" :options="PARAM_TYPE_OPTS" /></label>
                      <label class="adw-fld"><span class="adw-lbl">前端组件</span><span class="fd-ro-txt">{{ displayMeta(selParam.display_type).label }}</span></label>
                    </div>
                  </div>
                  <div class="adw-card"><div class="adw-card-hd adw-card-hd-flex"><span>通用设置</span><span class="bl-muted" style="font-size:12px;font-weight:400">控制参数在表单中的展示、编辑与校验行为</span></div>
                    <div class="fd-triple">
                      <div class="fd-tri">
                        <div class="fd-tri-hd"><span>可见</span><span v-if="ovCountOf('visible')" class="bl-tag bl-tag-primary at-mini-tag">{{ ovCountOf('visible') }} 条覆盖</span><span style="flex:1"></span><span class="adw-showsw" :class="{'is-on':selParam.visible===1}" @click="selParam.visible = selParam.visible?0:1"><span class="adw-showsw-dot"></span></span></div>
                        <div class="fd-tri-desc">控制参数在最终业务表单中是否展示</div>
                        <a class="fd-ovr" @click="addOverrideFor('visible')">+ 添加覆盖规则</a>
                      </div>
                      <div class="fd-tri">
                        <div class="fd-tri-hd"><span>禁用</span><span v-if="ovCountOf('disabled')" class="bl-tag bl-tag-primary at-mini-tag">{{ ovCountOf('disabled') }} 条覆盖</span><span style="flex:1"></span><span class="adw-showsw" :class="{'is-on':selParam.disabled===1}" @click="selParam.disabled = selParam.disabled?0:1"><span class="adw-showsw-dot"></span></span></div>
                        <div class="fd-tri-desc">控制参数是否为只读状态,禁用后用户无法编辑</div>
                        <a class="fd-ovr" @click="addOverrideFor('disabled')">+ 添加覆盖规则</a>
                      </div>
                      <div class="fd-tri">
                        <div class="fd-tri-hd"><span>必填</span><span v-if="ovCountOf('required')" class="bl-tag bl-tag-primary at-mini-tag">{{ ovCountOf('required') }} 条覆盖</span><span style="flex:1"></span><span class="adw-showsw" :class="{'is-on':selParam.is_required===1}" @click="selParam.is_required = selParam.is_required?0:1"><span class="adw-showsw-dot"></span></span></div>
                        <div class="fd-tri-desc">控制表单提交校验规则,开启后该参数不能为空</div>
                        <a class="fd-ovr" @click="addOverrideFor('required')">+ 添加覆盖规则</a>
                      </div>
                    </div>
                  </div>
                  <div class="adw-card"><div class="adw-card-hd">约束设置</div>
                    <label class="adw-sw" style="margin-bottom:12px"><input type="checkbox" v-model="selParam.allow_multi" :true-value="1" :false-value="0" /> 允许多值</label>
                    <div class="fd-modetab">
                      <button v-for="m in INPUT_MODES" :key="m.v" :class="['fd-mode', selParam.input_mode === m.v && 'is-on']" @click="selParam.input_mode = m.v">{{ m.label }}</button>
                    </div>
                    <div v-if="selParam.input_mode === 'input'" class="fd-mode-body">
                      <label class="fd-ck"><input type="checkbox" v-model="selParam.min_length_on" :true-value="1" :false-value="0" /> 启用最小长度<input v-if="selParam.min_length_on" class="bl-input bl-input-sm" style="width:100px;margin-left:8px" type="number" v-model="selParam.min_length" /></label>
                      <label class="fd-ck"><input type="checkbox" v-model="selParam.max_length_on" :true-value="1" :false-value="0" /> 启用最大长度<input v-if="selParam.max_length_on" class="bl-input bl-input-sm" style="width:100px;margin-left:8px" type="number" v-model="selParam.max_length" /></label>
                      <label class="fd-ck"><input type="checkbox" v-model="selParam.regex_on" :true-value="1" :false-value="0" /> 正则匹配<input v-if="selParam.regex_on" class="bl-input bl-input-sm bl-mono" style="flex:1;margin-left:8px" v-model="selParam.regex" placeholder="正则表达式" /></label>
                    </div>
                    <div v-else-if="selParam.input_mode === 'multi'" class="fd-mode-body">
                      <div class="fd-src"><label class="fd-src-opt" :class="{'is-on':selParam.option_source==='manual'}" @click="selParam.option_source='manual'">手动定义选项</label><label class="fd-src-opt" :class="{'is-on':selParam.option_source==='objectset'}" @click="selParam.option_source='objectset'">从对象集获取</label></div>
                      <template v-if="selParam.option_source === 'manual'">
                        <div v-for="(o, oi) in selParam.options" :key="oi" class="fd-opt"><span class="fd-grip" v-html="BL.icon('move', 11)"></span><input class="bl-input bl-input-sm bl-mono" v-model="o.value" placeholder="参数值" /><input class="bl-input bl-input-sm" v-model="o.label" placeholder="显示名称" /><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="selParam.options.splice(oi,1)" v-html="BL.icon('x', 11)"></button></div>
                        <button class="bl-btn bl-btn-text bl-btn-sm" @click="addOption(selParam)"><span v-html="BL.icon('plus', 11)"></span><span style="margin-left:3px">添加选项</span></button>
                      </template>
                      <div v-else class="fd-warn">从对象集获取选项:起始对象集 / 按属性过滤 / 关联搜索 / 返回属性 —— 将在后续迭代接入对象集选择器。</div>
                      <label class="adw-sw" style="margin-top:10px;display:flex"><input type="checkbox" v-model="selParam.allow_other" :true-value="1" :false-value="0" /> 允许其他值(用户可自定义输入)</label>
                    </div>
                    <div v-else class="fd-mode-body"><div class="bl-muted" style="font-size:12px">「{{ selParam.input_mode === 'user' ? '用户' : '用户组' }}」模式:下拉从系统人员/用户组获取,无需额外配置。</div></div>
                  </div>
                  <div class="adw-card"><div class="adw-card-hd">默认值</div>
                    <label class="adw-sw" style="margin-bottom:10px"><input type="checkbox" v-model="selParam.default_enabled" :true-value="1" :false-value="0" /> 启用默认值 <span class="bl-muted" style="font-size:11px;margin-left:4px">(用户可自行修改)</span></label>
                    <template v-if="selParam.default_enabled">
                      <div class="fd-src"><label class="fd-src-opt" :class="{'is-on':selParam.default_type==='objectprop'}" @click="selParam.default_type='objectprop'">对象参数属性</label><label class="fd-src-opt" :class="{'is-on':selParam.default_type==='static'}" @click="selParam.default_type='static'">设为静态值</label></div>
                      <input v-if="selParam.default_type === 'static'" class="bl-input" v-model="selParam.default_value" placeholder="固定默认值" />
                      <div v-else class="fd-warn">对象参数属性:两级级联(选表单内对象参数 → 选其属性字段),后续迭代接入。</div>
                    </template>
                  </div>
                </section>

                <!-- 显示 -->
                <section v-show="detailTab === 'display'" class="fd-sec">
                  <div class="adw-card"><div class="adw-card-hd">显示组件</div>
                    <div class="fd-disp-grid">
                      <button v-for="d in DISPLAY_TYPES" :key="d.v" :class="['fd-disp-opt', selParam.display_type === d.v && 'is-on']" @click="selParam.display_type = d.v">
                        <span v-html="BL.icon(d.icon, 15)"></span><span>{{ d.label }}</span>
                      </button>
                    </div>
                    <div class="adw-grid" style="margin-top:14px">
                      <label class="adw-fld"><span class="adw-lbl">占位文字</span><input class="bl-input" v-model="selParam.placeholder" placeholder="输入框占位提示" /></label>
                    </div>
                  </div>

                  <div class="adw-card"><div class="adw-card-hd adw-card-hd-flex"><span>通用布局</span>
                      <span class="adw-card-hd-act"><span class="bl-muted" style="font-size:12px;font-weight:400;line-height:26px">{{ dspOverCount(selParam) ? `本参数已覆盖 ${dspOverCount(selParam)} 项全局配置` : '全部继承表单全局配置' }}</span>
                      <button class="bl-btn bl-btn-text bl-btn-sm" @click="gotoGlobalDisplay">查看全局配置</button></span></div>
                    <div class="fd-inh-tip">下列配置默认<b>继承表单全局显示配置</b>;取消勾选「跟随全局」即为本参数单独覆盖,参数级优先于全局。</div>
                    <div class="fd-inh-row">
                      <span class="fd-inh-lbl">字段宽度</span>
                      <BlSelect :model-value="dspOf(selParam).width" :disabled="isInherit(selParam,'width')" :options="DSP_WIDTHS" @update:modelValue="v => setDsp(selParam,'width',v)" />
                      <label class="fd-inh-ck"><input type="checkbox" :checked="isInherit(selParam,'width')" @change="toggleInherit(selParam,'width')" /> 跟随全局</label>
                      <span v-if="!isInherit(selParam,'width')" class="bl-tag bl-tag-primary at-mini-tag">已覆盖</span>
                    </div>
                    <div class="fd-inh-row">
                      <span class="fd-inh-lbl">标签位置</span>
                      <BlSelect :model-value="dspOf(selParam).labelPos" :disabled="isInherit(selParam,'labelPos')" :options="DSP_LABEL_POS" @update:modelValue="v => setDsp(selParam,'labelPos',v)" />
                      <label class="fd-inh-ck"><input type="checkbox" :checked="isInherit(selParam,'labelPos')" @change="toggleInherit(selParam,'labelPos')" /> 跟随全局</label>
                      <span v-if="!isInherit(selParam,'labelPos')" class="bl-tag bl-tag-primary at-mini-tag">已覆盖</span>
                    </div>
                    <div class="fd-inh-row" :class="{ 'is-na': dspOf(selParam).labelPos !== 'left' }">
                      <span class="fd-inh-lbl">标签宽度</span>
                      <BlSelect :model-value="dspOf(selParam).labelW" :disabled="isInherit(selParam,'labelW') || dspOf(selParam).labelPos !== 'left'" :options="DSP_LABEL_WS" @update:modelValue="v => setDsp(selParam,'labelW',v)" />
                      <label class="fd-inh-ck"><input type="checkbox" :checked="isInherit(selParam,'labelW')" :disabled="dspOf(selParam).labelPos !== 'left'" @change="toggleInherit(selParam,'labelW')" /> 跟随全局</label>
                      <span v-if="!isInherit(selParam,'labelW')" class="bl-tag bl-tag-primary at-mini-tag">已覆盖</span>
                    </div>
                    <div class="fd-inh-row" :class="{ 'is-na': dspOf(selParam).labelPos !== 'left' }">
                      <span class="fd-inh-lbl">标签对齐</span>
                      <BlSelect :model-value="globalConf.labelAlign" disabled :options="DSP_LABEL_ALIGNS" />
                      <span class="bl-muted" style="font-size:12px;flex:1">表单级统一配置,不支持单参数覆盖</span>
                    </div>
                    <div class="fd-inh-row">
                      <span class="fd-inh-lbl">清空按钮</span>
                      <span class="fd-inh-ctl"><span class="adw-showsw" :class="{ 'is-on': dspOf(selParam).clear === 1, 'is-lock': isInherit(selParam,'clear') }" @click="!isInherit(selParam,'clear') && setDsp(selParam,'clear', dspOf(selParam).clear ? 0 : 1)"><span class="adw-showsw-dot"></span></span></span>
                      <label class="fd-inh-ck"><input type="checkbox" :checked="isInherit(selParam,'clear')" @change="toggleInherit(selParam,'clear')" /> 跟随全局</label>
                      <span v-if="!isInherit(selParam,'clear')" class="bl-tag bl-tag-primary at-mini-tag">已覆盖</span>
                    </div>
                  </div>

                  <div class="adw-card"><div class="adw-card-hd">辅助提示</div>
                    <div class="fd-inh-row">
                      <span class="fd-inh-lbl">帮助文案</span>
                      <span class="fd-inh-ctl"><span class="adw-showsw" :class="{ 'is-on': dspOf(selParam).helpOn === 1, 'is-lock': isInherit(selParam,'helpOn') }" @click="!isInherit(selParam,'helpOn') && setDsp(selParam,'helpOn', dspOf(selParam).helpOn ? 0 : 1)"><span class="adw-showsw-dot"></span></span></span>
                      <label class="fd-inh-ck"><input type="checkbox" :checked="isInherit(selParam,'helpOn')" @change="toggleInherit(selParam,'helpOn')" /> 跟随全局</label>
                      <span v-if="!isInherit(selParam,'helpOn')" class="bl-tag bl-tag-primary at-mini-tag">已覆盖</span>
                    </div>
                    <div class="fd-inh-row">
                      <span class="fd-inh-lbl">文案内容</span>
                      <input class="bl-input" style="flex:1;max-width:320px" v-model="selParam.help_text" :disabled="!dspOf(selParam).helpOn" placeholder="留空则使用全局默认文案" />
                      <span class="bl-muted" style="font-size:12px">{{ dspOf(selParam).helpOn ? (selParam.help_text ? '' : '留空 → 使用全局:' + globalConf.helpText) : '帮助文案未开启' }}</span>
                    </div>
                  </div>
                </section>

                <!-- 覆盖 -->
                <section v-show="detailTab === 'override'" class="fd-sec fd-sec-last">
                  <div class="adw-card"><div class="adw-card-hd">覆盖规则</div>
                    <div v-for="(ov, ovi) in selParam.overrides" :key="ovi" class="fd-ov-row">
                      <BlSelect v-model="ov.target" :options="[{value:'visible',label:'可见'},{value:'disabled',label:'禁用'},{value:'required',label:'必填'}]" size="sm" style="width:90px" />
                      <span class="bl-muted" style="font-size:12px">设为</span>
                      <BlSelect v-model="ov.value" :options="[{value:1,label:'是'},{value:0,label:'否'}]" size="sm" style="width:70px" />
                      <input class="bl-input bl-input-sm" style="flex:1" v-model="ov.condition" placeholder="满足条件(如 当前用户.角色 = 管理员)" />
                      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="selParam.overrides.splice(ovi,1)" v-html="BL.icon('x', 11)"></button>
                    </div>
                    <button class="bl-btn bl-btn-text bl-btn-sm" @click="selParam.overrides.push({ target:'visible', value:1, condition:'' })"><span v-html="BL.icon('plus', 11)"></span><span style="margin-left:3px">添加覆盖规则</span></button>
                    <div class="fd-warn" style="margin-top:10px">覆盖规则:按条件动态控制该参数的 可见/禁用/必填,条件构建器(If-Then)将在后续迭代接入完整条件树。</div>
                  </div>
                </section>

                <!-- 详情 -->
                <section v-show="detailTab === 'detail'" class="fd-sec fd-sec-last">
                  <div class="adw-card"><div class="adw-card-hd">基础信息</div>
                    <div class="adw-grid">
                      <label class="adw-fld"><span class="adw-lbl">参数编码</span><input class="bl-input bl-mono" v-model="selParam.param_code" placeholder="param_code" /></label>
                      <label class="adw-fld"><span class="adw-lbl">参数名称</span><input class="bl-input" v-model="selParam.param_name" /></label>
                      <label class="adw-fld"><span class="adw-lbl">所属分区</span><BlSelect v-model="selParam.section" :options="sections.map(s=>({value:s,label:s}))" /></label>
                      <label class="adw-fld"><span class="adw-lbl">数据类型</span><span class="fd-ro-txt">{{ dtMeta(selParam.param_type).label }}</span></label>
                    </div>
                  </div>
                  <div class="adw-card"><div class="adw-card-hd">依赖与引用</div>
                    <div class="adw-grid">
                      <label class="adw-fld"><span class="adw-lbl">规则引用</span>
                        <span v-if="isRefByRule(selParam.param_code)" class="bl-tag bl-tag-warning">已被规则引用</span>
                        <span v-else class="bl-tag">未被规则引用</span></label>
                      <label class="adw-fld"><span class="adw-lbl">覆盖规则数</span><span class="fd-ro-txt">{{ (selParam.overrides || []).length }} 条</span></label>
                    </div>
                  </div>
                </section>
              </div>
              </div>
            </template>
          </div>

          <!-- ========= 安全与提交 (移植条件树) ========= -->
          <div v-else-if="activeMenu === 'submit'" class="adw-page">
            <div class="adw-page-hd"><div class="adw-page-title">安全与提交准入</div><button class="bl-btn bl-btn-primary" @click="onSave" :disabled="saving">保存</button></div>
            <label class="adw-sw" style="margin-bottom:12px"><input type="checkbox" v-model="submit.enabled" :true-value="1" :false-value="0" /> 启用提交标准校验</label>
            <template v-if="submit.enabled">
              <div class="ate-grp-hd" style="margin-bottom:8px"><div class="ate-grp-title">执行规则</div></div>
              <ConditionGroup :node="submitTree" :depth="0" :object-fields="editorObjectFields" :param-fields="editorParamFields" />
              <label class="adw-fld" style="margin-top:14px"><span class="adw-lbl">校验失败提示</span><textarea class="bl-textarea" v-model="submit.error_message" rows="2" placeholder="不满足条件时的提示"></textarea></label>
            </template>
          </div>
        </main>

        <!-- ===== 右侧预览 384px (仅表单菜单) ===== -->
        <aside v-if="activeMenu === 'form'" :class="['adw-preview', previewFull && 'is-full']">
          <div class="adw-preview-hd adw-card-hd-flex"><span>表单预览</span>
            <button class="bl-btn bl-btn-text bl-btn-sm" @click="previewFull = !previewFull">{{ previewFull ? '退出全屏' : '全屏预览' }}</button></div>
          <div class="adw-preview-body">
            <div :class="['adw-preview-form', 'is-' + globalConf.density]">
              <div class="adw-preview-title">{{ form.rdfs_label || '动作表单' }}</div>
              <div v-if="!previewFull" class="adw-pv-tip">侧栏为单列示意,字段宽度请用「全屏预览」查看真实栅格</div>
              <template v-for="g in previewGroups" :key="g.sec">
              <div v-if="globalConf.sectionTitle === 1" class="adw-pv-sec">{{ g.sec }}</div>
              <div class="adw-pv-grid">
              <div v-for="x in g.items" :key="x.i"
                   :class="['adw-preview-fld', selIdx === x.i && formView === 'detail' && 'is-sel', dspOf(x.p).labelPos === 'left' && 'is-side']"
                   :style="previewFull ? { width: widthPct(x.p) } : null"
                   @click="openParam(x.i)">
                <div v-if="dspOf(x.p).labelPos !== 'none'" class="adw-preview-lbl"
                     :style="dspOf(x.p).labelPos === 'left' ? { flex:`0 0 ${labelWpx(x.p)}px`, textAlign: globalConf.labelAlign } : null">
                  <i v-if="x.p.is_required && globalConf.reqMark === 'prefix'" class="adw-pv-req">*</i>{{ x.p.param_name || x.p.param_code }}<i v-if="x.p.is_required && globalConf.reqMark === 'suffix'" class="adw-pv-req is-suffix">*</i>
                </div>
                <div class="adw-pv-ctl">
                <!-- 开关 -->
                <div v-if="x.p.display_type === 'switch'" class="adw-pv-switch"><span class="adw-showsw"><span class="adw-showsw-dot"></span></span></div>
                <!-- 多行 -->
                <div v-else-if="x.p.display_type === 'textarea'" class="adw-preview-input" style="height:56px;align-items:flex-start;padding-top:8px" :class="{ 'is-ro': x.p.disabled }">{{ x.p.placeholder || previewPlaceholder(x.p) }}</div>
                <!-- 下拉/单选/复选/人员/树: 带箭头 -->
                <div v-else-if="['select','user','tree'].includes(x.p.display_type)" class="adw-preview-input" :class="{ 'is-ro': x.p.disabled }"><span style="flex:1">{{ x.p.placeholder || '请选择…' }}</span><span v-html="BL.icon('chevronDown', 12)"></span></div>
                <!-- 单选按钮组 -->
                <div v-else-if="x.p.display_type === 'radio'" class="adw-pv-opts">
                  <label v-for="(o, oi) in previewOpts(x.p)" :key="oi" class="adw-pv-opt"><span class="adw-pv-radio"></span>{{ o }}</label>
                </div>
                <!-- 复选框 -->
                <div v-else-if="x.p.display_type === 'checkbox'" class="adw-pv-opts">
                  <label v-for="(o, oi) in previewOpts(x.p)" :key="oi" class="adw-pv-opt"><span class="adw-pv-checkbox"></span>{{ o }}</label>
                </div>
                <!-- 输入/数字/只读 -->
                <div v-else class="adw-preview-input" :class="{ 'is-ro': x.p.disabled || x.p.display_type === 'readonly' }">{{ x.p.placeholder || previewPlaceholder(x.p) }}</div>
                <div v-if="previewHelp(x.p)" class="adw-pv-help">{{ previewHelp(x.p) }}</div>
                </div>
              </div>
              </div>
              </template>
              <div v-if="!visibleParams.length" class="bl-muted" style="text-align:center;padding:24px;font-size:12px">无表单字段</div>
            </div>
          </div>
          <div class="adw-preview-ft"><span class="bl-muted" style="font-size:12px">共 {{ visibleParams.length }} 个字段 · {{ previewGroups.length }} 个分区</span></div>
        </aside>
        </div>
      </aside>
    </transition>
  </Teleport>

  <!-- 添加规则 选择弹框 -->
  <Teleport to="body">
    <div v-if="rulePickerOpen" class="rlm-mask" @click.self="rulePickerOpen = false">
      <div class="rlm-modal rlp-modal">
        <div class="rlm-hd"><span>添加规则</span><span style="flex:1"></span><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="rulePickerOpen = false" v-html="BL.icon('x', 14)"></button></div>
        <div class="rlm-body">
          <template v-for="g in RULE_KIND_GROUPS" :key="g">
            <div class="rlp-grp">{{ g }}</div>
            <div class="rlp-grid">
              <button v-for="k in RULE_KINDS.filter(x => x.group === g)" :key="k.kind" class="rlp-card" :class="{ 'is-disabled': kindDisabled(k) }" :disabled="kindDisabled(k)" :title="kindDisabledReason(k)" @click="addRuleKind(k.kind)">
                <span class="rlp-card-ic" :style="{ background:`color-mix(in srgb, ${k.color} 14%, transparent)`, color:k.color }" v-html="BL.icon(k.icon, 15, k.color)"></span>
                <div class="rlp-card-txt"><div class="rlp-card-lbl">{{ k.label }}</div><div class="rlp-card-desc">{{ k.desc }}</div></div>
              </button>
            </div>
          </template>
          <div class="fd-warn rlp-warn">
            <div><b>1.</b> 仅多对多链接使用「创建 / 删除链接」规则;一对多、一对一外键链接请使用「修改对象」规则编辑外键属性。</div>
            <div style="margin-top:4px"><b>2.</b> 配置函数规则后,将无法再添加其他 Ontology 规则。</div>
            <div style="margin-top:4px"><b>3.</b> 每个动作只能有一条启用的「创建对象」规则;需要创建多个对象请拆分成多个动作或改用函数规则。</div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>

  <!-- 入参映射弹窗 -->
  <Teleport to="body">
    <div v-if="paramMapOpen" class="rlm-mask" @click.self="paramMapOpen = false">
      <div class="rlm-modal">
        <div class="rlm-hd"><span v-html="BL.icon('link', 14)"></span><span style="margin-left:6px">函数入参映射</span><span style="flex:1"></span><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="paramMapOpen = false" v-html="BL.icon('x', 14)"></button></div>
        <div class="rlm-body" v-if="paramMapRule">
          <div class="fd-warn">为函数 <b>{{ paramMapRule.func_code || '(未绑定)' }}</b> 的入参绑定取值来源;类型不匹配将在校验阶段拦截。</div>
          <table class="bl-table ate-mini-table"><thead><tr><th class="t-left">入参名</th><th class="t-center">必填</th><th class="t-left">值来源</th><th class="t-left">值内容</th><th></th></tr></thead>
            <tbody><tr v-for="(fp, fi) in paramMapRule.func_params" :key="fi">
              <td><input class="bl-input bl-input-xs bl-mono" v-model="fp.name" placeholder="param_name" /></td>
              <td class="t-center"><input type="checkbox" v-model="fp.required" :true-value="1" :false-value="0" /></td>
              <td><BlSelect v-model="fp.value_source" :options="VALUE_SOURCE_OPTS" size="sm" /></td>
              <td><input class="bl-input bl-input-xs" v-model="fp.value_content" :placeholder="valuePlaceholder(fp.value_source)" /></td>
              <td class="t-center"><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="paramMapRule.func_params.splice(fi,1)" v-html="BL.icon('x', 11)"></button></td>
            </tr><tr v-if="!paramMapRule.func_params.length"><td colspan="5" class="bl-muted" style="text-align:center;padding:12px;font-size:12px">暂无入参,点下方添加</td></tr></tbody></table>
          <button class="bl-btn bl-btn-sm" style="margin-top:8px" @click="addFuncParam"><span v-html="BL.icon('plus', 11)"></span><span style="margin-left:3px">添加入参</span></button>
        </div>
        <div class="rlm-ft"><button class="bl-btn bl-btn-primary bl-btn-sm" @click="paramMapOpen = false">完成</button></div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { BL } from '@/lib/bl.js'
import { actionTypeApi, categoryApi, resourceApi } from '@/api'
import BlSelect from '@/components/BlSelect.vue'
import IconPickerField from '@/components/IconPickerField.vue'
import ColorPickerField from '@/components/ColorPickerField.vue'
import CodeEditor from '@/components/CodeEditor.vue'
import ConditionGroup from './ConditionGroup.vue'
const COMPACT_COLORS = ['#165DFF', '#00B42A', '#722ED1', '#FF7D00', '#EB2F96', '#13C2C2', '#FADB14', '#F53F3F']

const props = defineProps({
  open: Boolean,
  actionId: String,
  allClasses: { type: Array, default: () => [] },
  allLinkTypes: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:open', 'saved', 'deleted'])

/* ===== 抽屉尺寸: 右锚定 + 左缘拖拽 + 最大化 + 宽度持久化 (对标对象详情) ===== */
const DRAWER_MIN = 760
const drawerMaxed = ref(false)
const resizing = ref(false)
function drawerMaxPx() { return Math.floor(window.innerWidth * 0.92) }
function defaultDrawerWidth() { return Math.max(DRAWER_MIN, Math.floor(window.innerWidth * 0.68)) }
const storedW = Number(localStorage.getItem('bl.adw.width')) || 0
const drawerWidth = ref(storedW && storedW >= DRAWER_MIN ? storedW : defaultDrawerWidth())
let dragStartX = 0, dragStartW = 0
function onDragStart(e) {
  resizing.value = true; dragStartX = e.clientX; dragStartW = drawerWidth.value
  document.body.style.cursor = 'col-resize'; document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onDragMove); window.addEventListener('mouseup', onDragEnd)
}
function onDragMove(e) {
  const next = Math.min(drawerMaxPx(), Math.max(DRAWER_MIN, dragStartW + (dragStartX - e.clientX)))
  drawerWidth.value = next; drawerMaxed.value = next === drawerMaxPx()
}
function onDragEnd() {
  resizing.value = false; document.body.style.cursor = ''; document.body.style.userSelect = ''
  localStorage.setItem('bl.adw.width', String(drawerWidth.value))
  window.removeEventListener('mousemove', onDragMove); window.removeEventListener('mouseup', onDragEnd)
}
function toggleMax() {
  if (drawerMaxed.value) { drawerWidth.value = defaultDrawerWidth(); drawerMaxed.value = false }
  else { drawerWidth.value = drawerMaxPx(); drawerMaxed.value = true }
  localStorage.setItem('bl.adw.width', String(drawerWidth.value))
}
onBeforeUnmount(() => { window.removeEventListener('mousemove', onDragMove); window.removeEventListener('mouseup', onDragEnd); scrollEl.value?.removeEventListener('scroll', onDetailScroll) })

const MENUS = [
  { k: 'overview', label: '概览', icon: 'box' },
  { k: 'rules', label: '规则', icon: 'code' },
  { k: 'form', label: '表单', icon: 'menu' },
  { k: 'submit', label: '安全与提交', icon: 'lock' },
]
const M_TYPES = { 1:{label:'对象',icon:'box',color:'#165DFF'}, 2:{label:'链接',icon:'link',color:'#14C9C9'}, 3:{label:'函数',icon:'code',color:'#722ED1'}, 4:{label:'Webhook',icon:'zap',color:'#FF7D00'}, 5:{label:'接口',icon:'plug',color:'#0FC6C2'}, 6:{label:'通知',icon:'bell',color:'#B71DE8'} }
const ACTION_TYPES = {
  11:{label:'创建对象',color:'#00B42A',icon:'plus'}, 12:{label:'修改对象',color:'#165DFF',icon:'edit'}, 13:{label:'Upsert 对象',color:'#165DFF',icon:'edit'}, 14:{label:'删除对象',color:'#F53F3F',icon:'trash'},
  21:{label:'创建链接',color:'#14C9C9',icon:'link'}, 22:{label:'删除链接',color:'#F53F3F',icon:'link'}, 30:{label:'函数',color:'#722ED1',icon:'code'}, 40:{label:'Webhook',color:'#FF7D00',icon:'zap'},
  51:{label:'接口·创建',color:'#0FC6C2',icon:'plug'}, 52:{label:'接口·修改',color:'#0FC6C2',icon:'plug'}, 53:{label:'接口·删除',color:'#0FC6C2',icon:'plug'}, 54:{label:'接口·查询',color:'#0FC6C2',icon:'plug'}, 60:{label:'通知',color:'#B71DE8',icon:'bell'},
}
const VALUE_SOURCES = { 1:'来自参数', 2:'静态值', 3:'当前用户', 4:'系统时间', 5:'对象参数属性' }
const PROP_OPERATORS = { set:'赋值', add:'增加', sub:'减少', append:'追加', clear:'清空' }
/* 文档 5.3.3 四类取值来源顺序: 来自参数 / 对象参数属性 / 静态值 / 当前用户 / 系统时间 */
const VALUE_SOURCE_OPTS = [1, 5, 2, 3, 4].map(v => ({ value: v, label: VALUE_SOURCES[v] }))
const PROP_OPERATOR_OPTS = Object.entries(PROP_OPERATORS).map(([v, l]) => ({ value: v, label: l }))
const PARAM_TYPE_OPTS = ['string','number','boolean','object','date'].map(t => ({ value: t, label: t }))
/* 表单设计器: 数据类型 / 显示组件 / 录入模式 */
/* 图标取「类型本身的写法」而非动作: 字母 A=文本 / #=数值 / 勾=布尔 / 立方=对象 / 日历=日期 / 带勾方框=枚举 */
const DATA_TYPE_META = { string:{icon:'textType',color:'#2563eb',label:'字符串'}, number:{icon:'hash',color:'#1f2937',label:'数值'}, boolean:{icon:'check',color:'#10b981',label:'布尔'}, object:{icon:'cube',color:'#8b5cf6',label:'对象引用'}, date:{icon:'calendar',color:'#0891b2',label:'日期'}, enum:{icon:'checkSquare',color:'#dc2626',label:'枚举'} }
const DISPLAY_TYPES = [
  { v:'input', label:'单行输入框', icon:'edit' }, { v:'textarea', label:'多行文本域', icon:'menu' },
  { v:'select', label:'下拉选择', icon:'chevronDown' }, { v:'radio', label:'单选按钮组', icon:'check' },
  { v:'number', label:'数字步进器', icon:'code' }, { v:'switch', label:'开关', icon:'zap' },
  { v:'user', label:'人员选择器', icon:'user' }, { v:'readonly', label:'只读文本', icon:'lock' }, { v:'checkbox', label:'复选框', icon:'check' },
]
const INPUT_MODES = [{ v:'input', label:'用户输入' }, { v:'multi', label:'多选' }, { v:'user', label:'用户' }, { v:'usergroup', label:'用户组' }]
const DETAIL_TABS = [{ k:'value', label:'值' }, { k:'display', label:'显示' }, { k:'override', label:'覆盖' }, { k:'detail', label:'详情' }]
/* 规则类型 */
const RULE_KINDS = [
  { kind:'create_object', label:'创建对象', icon:'plus', color:'#00B42A', ruleType:1, group:'Ontology 编辑规则', desc:'创建指定类型的新对象实例' },
  { kind:'modify_object', label:'修改对象', icon:'edit', color:'#165DFF', ruleType:1, group:'Ontology 编辑规则', desc:'修改已有对象属性(含外键)' },
  { kind:'delete_object', label:'删除对象', icon:'trash', color:'#F53F3F', ruleType:1, group:'Ontology 编辑规则', desc:'删除指定的已有对象实例' },
  { kind:'create_link', label:'创建链接', icon:'link', color:'#14C9C9', ruleType:1, group:'Ontology 编辑规则', desc:'建立多对多对象关联' },
  { kind:'delete_link', label:'删除链接', icon:'link', color:'#F53F3F', ruleType:1, group:'Ontology 编辑规则', desc:'移除多对多对象关联' },
  { kind:'function', label:'函数规则', icon:'code', color:'#722ED1', ruleType:1, group:'Ontology 编辑规则', desc:'调用 Ontology 编辑函数处理' },
  { kind:'notification', label:'通知规则', icon:'bell', color:'#B71DE8', ruleType:2, group:'副作用规则', desc:'发送业务通知给指定用户' },
  { kind:'webhook', label:'Webhook', icon:'zap', color:'#FF7D00', ruleType:2, group:'副作用规则', desc:'向外部系统发起回调请求' },
]
function kindMeta(kind) { return RULE_KINDS.find(k => k.kind === kind) || RULE_KINDS[1] }
const RULE_KIND_GROUPS = ['Ontology 编辑规则', '副作用规则']
const NOTIFY_CHANNELS = [{ value:'push', label:'平台推送' }, { value:'email', label:'电子邮件' }, { value:'sms', label:'短信' }, { value:'both', label:'推送+邮件' }]
const WH_METHODS = [{ value:'POST', label:'POST' }, { value:'GET', label:'GET' }, { value:'PUT', label:'PUT' }, { value:'DELETE', label:'DELETE' }]
const WH_TIMINGS = [{ value:'before', label:'编辑应用前' }, { value:'after', label:'编辑应用后' }]

function defaultParam() {
  return { param_code:'', param_name:'', param_type:'string', is_required:1, value_source:1, default_value:'', property_code:'',
    section:'基础参数', visible:1, disabled:0, display_type:'input', placeholder:'', help_text:'',
    allow_multi:0, input_mode:'input', min_length_on:0, min_length:'', max_length_on:0, max_length:'', regex_on:0, regex:'',
    options:[], allow_other:0, option_source:'manual', default_enabled:0, default_type:'static', overrides:[], dsp:{} }
}
function normalizeParam(p) {
  const cfg = parseCfg(p.config)
  const d = defaultParam()
  return { ...d, param_code:p.param_code, param_name:p.param_name, param_type:p.param_type||'string', is_required:Number(p.is_required)||0,
    value_source:Number(cfg.value_source)||1, default_value:p.default_value||'', property_code:cfg.property_code||'',
    section:cfg.section||'基础参数', visible:cfg.visible??1, disabled:cfg.disabled??0, display_type:cfg.display_type||autoDisplay(p.param_type),
    placeholder:cfg.placeholder||'', help_text:cfg.help_text||'', allow_multi:cfg.allow_multi??0, input_mode:cfg.input_mode||'input',
    min_length_on:cfg.min_length_on??0, min_length:cfg.min_length??'', max_length_on:cfg.max_length_on??0, max_length:cfg.max_length??'',
    regex_on:cfg.regex_on??0, regex:cfg.regex||'', options:cfg.options||[], allow_other:cfg.allow_other??0, option_source:cfg.option_source||'manual',
    default_enabled:cfg.default_enabled??0, default_type:cfg.default_type||'static', overrides:cfg.overrides||[], dsp:cfg.dsp||{} }
}
function paramConfig(p) {
  return { value_source:p.value_source, property_code:p.property_code||null, section:p.section, visible:p.visible, disabled:p.disabled,
    display_type:p.display_type, placeholder:p.placeholder, help_text:p.help_text, allow_multi:p.allow_multi, input_mode:p.input_mode,
    min_length_on:p.min_length_on, min_length:p.min_length, max_length_on:p.max_length_on, max_length:p.max_length, regex_on:p.regex_on, regex:p.regex,
    options:p.options, allow_other:p.allow_other, option_source:p.option_source, default_enabled:p.default_enabled, default_type:p.default_type, overrides:p.overrides, dsp:p.dsp || {} }
}
function autoDisplay(t) { return ({ string:'input', number:'number', boolean:'switch', object:'select', date:'input' })[t] || 'input' }

function defaultForm() {
  return { id:'', rid:'', api_name:'', m_type:1, action_type:11, object_class_id:'', link_type_id:'', function_code:'',
    category_code:'', button_text:'', icon:'', color:'#165DFF', show_on_detail:0, show_on_batch:0, form_enabled:0,
    submit_criteria_enabled:0, compile_status:0, current_version:'', status:0, rdfs_label:'', rdfs_comment:'',
    rdfs_see_also:'', rdfs_defined_by:'', create_time:'', update_time:'', metadata:null }
}
const form = reactive(defaultForm())
let formSnapshot = null
const rules = ref([])
const formParams = ref([])
const submit = reactive({ enabled:0, validate_mode:'all', error_message:'' })
const classProps = ref([])
const sectionCount = ref(0)
const submitTree = reactive({ logic:'all', children:[] })
const activeMenu = ref('overview')
const formNavOpen = ref(false)
const rulesNavOpen = ref(false)
function onMenuClick(m) {
  activeMenu.value = m.k
  if (m.k === 'form') formNavOpen.value = true
  if (m.k === 'rules') { rulesNavOpen.value = true; ruleView.value = 'list'; ruleEditIdx.value = -1; ruleSelKey.value = null }
}
const editMode = ref(false)
const saving = ref(false)
const formSel = ref(0)
let ruleKey = 0, etk = 0

const editorObjectFields = computed(() => classProps.value.map(p => ({ code:p.code, name:p.name, dataType:p.dataType })))
const editorParamFields = computed(() => formParams.value.map(p => ({ code:p.param_code, name:p.param_name || p.param_code, dataType:p.param_type })))
const classPropsOptions = computed(() => classProps.value.map(p => ({ value:p.code, label:`${p.name} (${p.code})` })))
const objectParamOptions = computed(() => formParams.value.filter(p => p.param_type === 'object' && String(p.param_code||'').trim()).map(p => ({ value:p.param_code, label:`${p.param_name||p.param_code} (${p.param_code})` })))
const formParamOptions = computed(() => formParams.value.filter(p => String(p.param_code||'').trim()).map(p => ({ value:p.param_code, label:`${p.param_name||p.param_code} (${p.param_code})` })))
const classOptions = computed(() => (props.allClasses || []).map(c => ({ id:c.id, cn:c.display_name||c.rdfs_label||c.api_name, api_name:c.api_name, category_code:c.category_code })))
const objClassOptions = computed(() => classOptions.value.map(c => ({ value:c.id, label:`${c.cn} (${c.api_name})` })))
const linkTypeCode = l => l.link_type_id || l.linkTypeId || l.id
const linkTypeOptions = computed(() => (props.allLinkTypes || []).map(l => ({ value:linkTypeCode(l), label:l.rdfs_label || l.rdfsLabel || linkTypeCode(l) })))
const LINK_CARD = { one_one:'一对一', one_many:'一对多', many_one:'多对一', many_many:'多对多' }
function linkCardLabel(code) {
  const l = (props.allLinkTypes || []).find(x => linkTypeCode(x) === code)
  if (!l) return '—'
  return LINK_CARD[`${l.l_cardinality || l.lCardinality || 'one'}_${l.r_cardinality || l.rCardinality || 'one'}`] || '—'
}
/* 规则目标对象的属性候选 (按规则自身的对象类型加载, 与动作主体可以不同) */
const ruleClassPropsCache = reactive({})
async function loadRuleClassProps(classId) {
  if (!classId || ruleClassPropsCache[classId]) return
  const list = await resourceApi.properties(classId).catch(() => [])
  const arr = Array.isArray(list) ? list : (list?.data || [])
  ruleClassPropsCache[classId] = arr.map(p => ({ code:p.api_name||p.prop_code, name:p.display_name||p.rdfs_label||p.api_name }))
}
const objRuleClassId = computed(() => selEditRule.value?.obj_class_id || form.object_class_id || '')
const objRuleClassName = computed(() => classOptions.value.find(c => c.id === objRuleClassId.value)?.cn || '')
const objRulePropOptions = computed(() => {
  const arr = ruleClassPropsCache[objRuleClassId.value] || (objRuleClassId.value === form.object_class_id ? classProps.value : [])
  return arr.map(p => ({ value:p.code, label:`${p.name} (${p.code})` }))
})
const objPkOptions = computed(() => [{ value:'', label:'系统自动生成' }, ...objRulePropOptions.value])
function addObjLink(rule) { rule.obj_links.push({ link_type_code:'', peer_param:'' }) }
const headMeta = computed(() => ACTION_TYPES[Number(form.action_type)] || M_TYPES[form.m_type] || { color:'#165DFF', icon:'zap' })
const headColor = computed(() => form.color || headMeta.value.color)
const headIcon = computed(() => headMeta.value.icon)
const typeLabel = computed(() => headMeta.value.label || '—')
const subjectSummary = computed(() => {
  if (form.m_type === 1) { const c = classOptions.value.find(x => x.id === form.object_class_id); return c ? `${c.cn} (${c.api_name})` : '—' }
  if (form.m_type === 2) { const l = (props.allLinkTypes||[]).find(x => x.id === form.link_type_id); return l ? (l.rdfs_label||l.link_type_id) : '—' }
  if (form.m_type === 3) return form.function_code || '—'
  return '—'
})
const activeRuleCount = computed(() => rules.value.filter(r => Number(r.status ?? 1) === 1).length)
/* 文档 5.3.5 无效规则组合检测 (保存阶段拦截) */
const ruleConflicts = computed(() => {
  const out = []
  const rs = rules.value.filter(r => Number(r.status ?? 1) === 1)
  if (rs.filter(r => r.kind === 'create_object').length > 1)
    out.push('存在多条「创建对象」规则:同一对象在单次提交中不能被创建两次(主键冲突 / 数据重复)。')
  rs.forEach((r, i) => {
    if (r.kind === 'delete_object' && r.target_param_code) {
      const later = rs.slice(i + 1).find(x => x.kind === 'modify_object' && x.target_param_code === r.target_param_code)
      if (later) out.push(`「删除对象」(${r.rule_name || '未命名'}) 排在「修改对象」(${later.rule_name || '未命名'}) 之前:目标对象已被删除,后续修改无操作目标。`)
    }
    if (r.kind === 'modify_object' && r.target_param_code) {
      const laterCreate = rs.slice(i + 1).find(x => x.kind === 'create_object')
      // 修改引用的对象若由后置创建规则产生, 则前置修改无目标 (仅当同 target 语义, 这里给出弱提示)
    }
  })
  const sideIdx = rs.findIndex(r => kindMeta(r.kind).ruleType === 2)
  const lastOntoIdx = rs.reduce((m, r, i) => kindMeta(r.kind).ruleType === 1 ? i : m, -1)
  if (sideIdx >= 0 && lastOntoIdx >= 0 && sideIdx < lastOntoIdx)
    out.push('存在「副作用规则(通知 / Webhook)」排在「本体编辑规则」之前:副作用应在本体编辑完成后触发,请将其移到本体规则之后。')
  return out
})
const triggerCount = computed(() => (form.show_on_detail ? 1 : 0) + (form.show_on_batch ? 1 : 0))
const visibleParams = computed(() => formParams.value.map((p, i) => ({ p, i })).filter(x => String(x.p.param_code || '').trim() && x.p.visible !== 0))
function menuCount(k) { return k === 'rules' ? rules.value.length : k === 'form' ? formParams.value.length : 0 }
function paramTypeIcon(t) { return ({ string:'edit', number:'code', boolean:'check', object:'box', date:'zap' })[t] || 'edit' }
function previewPlaceholder(p) { return ({ string:'请输入…', number:'0', boolean:'是 / 否', object:'选择对象…', date:'选择日期…' })[p.param_type] || '请输入…' }
function previewOpts(p) { const o = (p.options || []).map(x => x.label || x.value).filter(Boolean); return o.length ? o : ['选项一', '选项二'] }

/* 条件树 ↔ 扁平节点 */
function buildSubmitTree(nodes) {
  submitTree.logic = 'all'; submitTree.children = []
  if (!Array.isArray(nodes) || !nodes.length) return
  const root = nodes.find(n => !n.parent_id); if (!root) return
  const childrenOf = (pid) => nodes.filter(n => n.parent_id === pid).sort((a,b)=>(a.sort||0)-(b.sort||0)).map(n => {
    if (n.node_type === 'group') return { _k:'ek-'+(etk++), type:'group', logic:n.logic_op||'all', children:childrenOf(n.id) }
    const parts = String(n.left_code||'').split(':'); const subj = parts[0]==='user'?'user':parts[0]==='param'?'param':'object'
    return { _k:'ek-'+(etk++), type:'cond', subject:subj, field:parts[1]||'', operator:n.operator||'eq', value:n.right_value||'' }
  })
  submitTree.logic = root.logic_op || 'all'; submitTree.children = childrenOf(root.id)
}
function flattenSubmitTree() {
  const out = []; let c = 0
  const walk = (node, parentId, sort) => {
    const id = 'cn-'+(++c); const isGroup = node.type !== 'cond'
    out.push({ id, parent_id:parentId, sort, node_type:isGroup?'group':'condition', logic_op:isGroup?(node.logic||'all'):null,
      left_code:isGroup?null:`${node.subject}:${node.field||''}`, operator:isGroup?null:(node.operator||null),
      right_value:isGroup?null:(node.value||null), value_source:isGroup?null:(node.subject==='user'?3:null) })
    if (isGroup && node.children) node.children.forEach((ch,i)=>walk(ch,id,i))
  }
  walk(submitTree, null, 0); return out
}

/* 领域候选 */
const domainOpts = ref([])
const domainOptions = computed(() => domainOpts.value.map(d => ({ value:d.code, label:(d.indent||'')+d.label })))
async function loadDomainOpts() {
  if (domainOpts.value.length) return
  const tree = await categoryApi.tree().catch(() => [])
  const list = []
  const walk = (ns, depth) => (ns || []).forEach(n => { if (n.categoryCode && n.categoryType === 2) list.push({ code:n.categoryCode, label:n.label||n.rdfsLabel||n.categoryCode, indent:'　'.repeat(depth) }); if (n.children) walk(n.children, depth+1) })
  walk(tree, 0); domainOpts.value = list
}
async function loadClassProps() {
  if (!form.object_class_id) { classProps.value = []; return }
  const list = await resourceApi.properties(form.object_class_id).catch(() => [])
  const arr = Array.isArray(list) ? list : (list?.data || [])
  classProps.value = arr.map(p => ({ code:p.api_name||p.prop_code, name:p.display_name||p.rdfs_label||p.api_name, dataType:p.data_type, required:!!p.is_required }))
}

/* 加载 / 保存 */
function parseCfg(s){ try { return JSON.parse(s||'{}') } catch { return {} } }
function readMeta(){ return (form.metadata && typeof form.metadata === 'object') ? { ...form.metadata } : parseCfg(form.metadata) }
async function load() {
  loadDomainOpts()
  activeMenu.value = 'overview'; editMode.value = false; formSel.value = 0; ruleView.value = 'list'; ruleEditIdx.value = -1
  ruleSelKey.value = null
  const res = await actionTypeApi.get(props.actionId).catch(() => null)
  Object.assign(form, defaultForm(), res || {})
  rules.value = (res?.rules || []).map(normalizeRule)
  formParams.value = (res?.form_params || []).map(normalizeParam)
  sectionCount.value = (res?.form_sections || []).length
  const ss = res?.submit_standard
  submit.enabled = Number(ss?.enabled)||0; submit.validate_mode = ss?.validate_mode||'all'; submit.error_message = ss?.error_message||''
  buildSubmitTree(ss?.nodes)
  const meta = readMeta()
  const fg = meta.form_global || {}
  globalConf.custom_submit = Number(fg.custom_submit) || 0
  globalConf.custom_success = Number(fg.custom_success) || 0
  globalConf.width = fg.width || 'full'
  globalConf.labelPos = fg.labelPos || 'top'
  globalConf.labelW = fg.labelW || '90'
  globalConf.labelAlign = fg.labelAlign || 'left'
  globalConf.clear = fg.clear ?? 1
  globalConf.helpOn = fg.helpOn ?? 0
  globalConf.helpText = fg.helpText || '请按要求填写该字段'
  globalConf.reqMark = fg.reqMark || 'prefix'
  globalConf.density = fg.density || 'normal'
  globalConf.sectionTitle = fg.sectionTitle ?? 1
  if (form.object_class_id) loadClassProps()
  formSnapshot = JSON.stringify(pickEditable())
  nextTick(() => { fullSnapshot = snapshotAll() })
}
function pickEditable() { return { rdfs_label:form.rdfs_label, button_text:form.button_text, category_code:form.category_code, show_on_detail:form.show_on_detail, show_on_batch:form.show_on_batch, icon:form.icon, color:form.color, rdfs_comment:form.rdfs_comment, rdfs_see_also:form.rdfs_see_also, rdfs_defined_by:form.rdfs_defined_by } }
function cancelEdit() { if (formSnapshot) Object.assign(form, JSON.parse(formSnapshot)); editMode.value = false }
/* 全量快照 (用于未保存关闭拦截), 剔除瞬时字段 */
function snapshotAll() {
  return JSON.stringify({
    f: pickEditable(),
    p: formParams.value,
    r: rules.value.map(({ _k, _collapsed, ...rest }) => rest),
    s: { e: submit.enabled, m: submit.error_message, t: flattenSubmitTree() },
    g: { ...globalConf },
  })
}
let fullSnapshot = null
watch(() => props.open, v => { if (v) load() })
watch(() => props.actionId, () => { if (props.open) load() })

/* 规则/参数 操作 */
function normalizeRule(r) {
  const cfg = parseCfg(r.rule_config)
  return { _k:++ruleKey, id:r.id, rule_type:Number(r.rule_type)||1, rule_name:r.rule_name||'', target_param_code:r.target_param_code||'', link_type_code:r.link_type_code||'', status:Number(r.status??1),
    kind: cfg.kind || (Number(r.rule_type)===2 ? 'notification' : 'modify_object'), _collapsed: true,
    func_code:cfg.func_code||'', func_version:cfg.func_version||'v1', func_autoupgrade:cfg.func_autoupgrade??1, func_params:(cfg.func_params||[]).map(fp=>({ name:fp.name||'', param_type:fp.param_type||'string', required:fp.required??1, value_source:Number(fp.value_source)||1, value_content:fp.value_content||'' })),
    func_exec_identity:cfg.func_exec_identity||'caller', func_error_strategy:cfg.func_error_strategy||'rollback', func_timeout:cfg.func_timeout??30, func_retry:cfg.func_retry??0, func_concurrent:cfg.func_concurrent??0, func_return_attachment:cfg.func_return_attachment??1, func_exceptions:(cfg.func_exceptions||[]).map(e=>({ code:e.code||'', message:e.message||'' })),
    link_src_param:cfg.link_src_param||'', link_dst_param:cfg.link_dst_param||'',
    obj_class_id:cfg.obj_class_id||form.object_class_id||'', obj_pk_property:cfg.obj_pk_property||'',
    obj_links:(cfg.obj_links||[]).map(l=>({ link_type_code:l.link_type_code||'', peer_param:l.peer_param||'' })),
    notify_channel:cfg.notify_channel||'push', notify_title:cfg.notify_title||'', notify_content:cfg.notify_content||'', notify_to:cfg.notify_to||'',
    notify_recipient_source:cfg.notify_recipient_source||'object_prop', notify_recipient_object_param:cfg.notify_recipient_object_param||'', notify_recipient_user_attr:cfg.notify_recipient_user_attr||'', notify_recipient_field:cfg.notify_recipient_field||'',
    notify_content_mode:cfg.notify_content_mode||'desc', notify_func_code:cfg.notify_func_code||'',
    notify_ch_push: cfg.notify_ch_push ?? ((cfg.notify_channel==='push'||cfg.notify_channel==='both'||!cfg.notify_channel)?1:0), notify_ch_email: cfg.notify_ch_email ?? ((cfg.notify_channel==='email'||cfg.notify_channel==='both')?1:0), notify_ch_sms: cfg.notify_ch_sms ?? (cfg.notify_channel==='sms'?1:0),
    notify_link_enabled:cfg.notify_link_enabled??0, notify_link_type:cfg.notify_link_type||'object_detail', notify_link_target:cfg.notify_link_target||'', notify_link_text:cfg.notify_link_text||'',
    notify_custom_html:cfg.notify_custom_html??0, notify_html_content:cfg.notify_html_content||'', notify_permission_scope:cfg.notify_permission_scope||'all',
    wh_url:cfg.wh_url||'', wh_method:cfg.wh_method||'POST', wh_timing:cfg.wh_timing||'after', wh_body:cfg.wh_body||'',
    wh_subtype:cfg.wh_subtype||'sideeffect', wh_code:cfg.wh_code||'', wh_version:cfg.wh_version||'v1', wh_input_mode:cfg.wh_input_mode||'manual', wh_input_func:cfg.wh_input_func||'', wh_input_func_version:cfg.wh_input_func_version||'', wh_params:(cfg.wh_params||[]).map(p=>({ name:p.name||'', param_type:p.param_type||'string', value_source:Number(p.value_source)||1, value_content:p.value_content||'' })),
    prop_mappings:(r.prop_mappings||[]).map(m => ({ property_code:m.property_code||'', property_name:m.property_name||'', prop_operator:m.prop_operator||'set', value_source:Number(m.value_source)||1, value_content:m.value_content||'', is_required:Number(m.is_required)||0 })) }
}
function ruleConfig(r) {
  return { kind:r.kind, func_code:r.func_code, func_version:r.func_version, func_autoupgrade:r.func_autoupgrade, func_params:r.func_params,
    func_exec_identity:r.func_exec_identity, func_error_strategy:r.func_error_strategy, func_timeout:r.func_timeout, func_retry:r.func_retry, func_concurrent:r.func_concurrent, func_return_attachment:r.func_return_attachment, func_exceptions:r.func_exceptions,
    link_src_param:r.link_src_param, link_dst_param:r.link_dst_param,
    obj_class_id:r.obj_class_id, obj_pk_property:r.obj_pk_property, obj_links:r.obj_links,
    notify_channel:r.notify_channel, notify_title:r.notify_title, notify_content:r.notify_content, notify_to:r.notify_to,
    notify_recipient_source:r.notify_recipient_source, notify_recipient_object_param:r.notify_recipient_object_param, notify_recipient_user_attr:r.notify_recipient_user_attr, notify_recipient_field:r.notify_recipient_field,
    notify_content_mode:r.notify_content_mode, notify_func_code:r.notify_func_code,
    notify_ch_push:r.notify_ch_push, notify_ch_email:r.notify_ch_email, notify_ch_sms:r.notify_ch_sms,
    notify_link_enabled:r.notify_link_enabled, notify_link_type:r.notify_link_type, notify_link_target:r.notify_link_target, notify_link_text:r.notify_link_text,
    notify_custom_html:r.notify_custom_html, notify_html_content:r.notify_html_content, notify_permission_scope:r.notify_permission_scope,
    wh_url:r.wh_url, wh_method:r.wh_method, wh_timing:r.wh_timing, wh_body:r.wh_body,
    wh_subtype:r.wh_subtype, wh_code:r.wh_code, wh_version:r.wh_version, wh_input_mode:r.wh_input_mode, wh_input_func:r.wh_input_func, wh_input_func_version:r.wh_input_func_version, wh_params:r.wh_params }
}
function addRuleKind(kind) {
  const meta = kindMeta(kind)
  rules.value.push({ _k:++ruleKey, rule_type:meta.ruleType, rule_name:meta.label, target_param_code:'', link_type_code:'', status:1, kind, _collapsed:false,
    func_code:'', func_version:'v1', func_autoupgrade:1, func_params:[], func_exec_identity:'caller', func_error_strategy:'rollback', func_timeout:30, func_retry:0, func_concurrent:0, func_return_attachment:1, func_exceptions:[], link_src_param:'', link_dst_param:'',
    obj_class_id:form.object_class_id||'', obj_pk_property:'', obj_links:[], notify_channel:'push', notify_title:'', notify_content:'', notify_to:'',
    notify_recipient_source:'object_prop', notify_recipient_object_param:'', notify_recipient_user_attr:'', notify_recipient_field:'', notify_content_mode:'desc', notify_func_code:'', notify_ch_push:1, notify_ch_email:0, notify_ch_sms:0,
    notify_link_enabled:0, notify_link_type:'object_detail', notify_link_target:'', notify_link_text:'', notify_custom_html:0, notify_html_content:'', notify_permission_scope:'all',
    wh_url:'', wh_method:'POST', wh_timing:'after', wh_body:'', wh_subtype:'sideeffect', wh_code:'', wh_version:'v1', wh_input_mode:'manual', wh_input_func:'', wh_input_func_version:'', wh_params:[], prop_mappings:[] })
  rulePickerOpen.value = false
  rulesNavOpen.value = true; ruleSelKey.value = ruleKey
}
/* 折叠态预览文本 */
function rulePreview(r) {
  if (r.kind === 'function') return `函数 ${r.func_code || '未绑定'}@${r.func_version} · ${(r.func_params||[]).length} 个入参${r.func_autoupgrade ? ' · 自动升级' : ''}`
  if (r.kind === 'notification') return `${[r.notify_ch_push&&'推送', r.notify_ch_email&&'邮件', r.notify_ch_sms&&'短信'].filter(Boolean).join('/') || '未选渠道'} · ${r.notify_title || '未设置标题'}`
  if (r.kind === 'webhook') return `${WH_SUBTYPES.find(t=>t.value===r.wh_subtype)?.label||''} · ${r.wh_code || '未选择 Webhook'} · ${(r.wh_params||[]).length} 个输入项`
  if (r.kind.includes('link')) return `链接 ${r.link_type_code || '未指定'}${r.kind.startsWith('delete') ? '' : ' · ' + (r.prop_mappings||[]).length + ' 项赋值'}`
  if (r.kind.startsWith('delete')) return '按对象引用参数删除目标对象'
  return `${(r.prop_mappings||[]).length} 项属性赋值`
}
/* 入参映射弹窗 */
const paramMapOpen = ref(false)
const paramMapRule = ref(null)
function openParamMap(rule) { paramMapRule.value = rule; paramMapOpen.value = true }
function addFuncParam() { if (paramMapRule.value) paramMapRule.value.func_params.push({ name:'', param_type:'string', required:1, value_source:1, value_content:'' }) }
/* 函数规则 完整编辑态 */
const FUNC_UPGRADE_OPTS = [{ value:1, label:'自动升级到兼容版本' }, { value:0, label:'锁定当前版本' }]
const FUNC_IDENTITY_OPTS = [{ value:'caller', label:'以调用者身份执行' }, { value:'service', label:'以服务账号身份执行' }]
const FUNC_ERR_OPTS = [{ value:'rollback', label:'中断操作,回滚所有 Ontology 变更' }, { value:'continue', label:'继续执行,记录异常日志' }]
const FUNC_PTYPE_OPTS = [{ value:'string', label:'字符串' }, { value:'number', label:'数字' }, { value:'boolean', label:'布尔' }, { value:'object', label:'对象' }, { value:'date', label:'日期' }]
const NOTIFY_RECIPIENT_SRC = [{ value:'object_prop', label:'来自对象参数属性' }, { value:'param', label:'来自参数' }, { value:'static', label:'静态指定' }]
const WH_SUBTYPES = [
  { value:'writeback', label:'回写', icon:'edit', desc:'使用回写模式编辑外部数据系统。外部系统返回结构响应可用于其他动作编辑规则。如果回写执行失败,所有动作编辑都不会生效,错误会立即显示给终端用户。' },
  { value:'sideeffect', label:'副作用', icon:'zap', desc:'副作用模式在本体对象修改完成、事务提交后执行。支持配置多个副作用 Webhook;执行失败不影响主操作结果,用户可看到有物或待提示后异步执行。' },
]
const NOTIFY_LINK_TYPES = [{ value:'object_detail', label:'对象详情区' }, { value:'external', label:'外部链接' }, { value:'action', label:'触发其它动作' }]
const ruleView = ref('list')
const ruleEditIdx = ref(-1)
const selEditRule = computed(() => rules.value[ruleEditIdx.value] || null)
/* 左导航规则树: 按 编辑类(ruleType=1) / 副作用(ruleType=2) 分组, 组内保持规则生效顺序 */
const RULE_NAV_GROUPS = [{ ruleType: 1, label: '编辑类' }, { ruleType: 2, label: '副作用' }]
const ruleSelKey = ref(null)
const ruleNavGroups = computed(() => RULE_NAV_GROUPS
  .map(g => ({ label: g.label, items: rules.value.map((r, i) => ({ r, i })).filter(x => kindMeta(x.r.kind).ruleType === g.ruleType) }))
  .filter(g => g.items.length))
function ruleNavLabel(r) { const m = kindMeta(r.kind); return r.rule_name ? `${m.label}: ${r.rule_name}` : m.label }
/* 点下级菜单 = 直接进该规则的完整编辑页 */
const RULE_VIEW_BY_KIND = { function:'func', notification:'notify', webhook:'webhook' }
function openRuleFromNav(i) {
  const rule = rules.value[i]
  if (!rule) return
  activeMenu.value = 'rules'
  ruleSelKey.value = rule._k
  const view = RULE_VIEW_BY_KIND[rule.kind] || 'object'
  if (view === 'object') openObjectEdit(i); else { ruleEditIdx.value = i; ruleView.value = view }
}
function openObjectEdit(ri) {
  ruleEditIdx.value = ri; ruleSelKey.value = rules.value[ri]?._k ?? null; ruleView.value = 'object'
  loadRuleClassProps(rules.value[ri]?.obj_class_id || form.object_class_id)
}
function openFuncEdit(ri) { ruleEditIdx.value = ri; ruleSelKey.value = rules.value[ri]?._k ?? null; ruleView.value = 'func' }
function openNotifyEdit(ri) { ruleEditIdx.value = ri; ruleSelKey.value = rules.value[ri]?._k ?? null; ruleView.value = 'notify' }
function openWebhookEdit(ri) { ruleEditIdx.value = ri; ruleSelKey.value = rules.value[ri]?._k ?? null; ruleView.value = 'webhook' }
function addWhParam(rule) { rule.wh_params.push({ name:'', param_type:'string', value_source:1, value_content:'' }) }
function whInputCodePreview(r) {
  return `import { Function, UserFacingError } from "@foundry/functions-api"\nimport { Company } from "@foundry/ontology-api"\n\n// 定义一个接口, 用于表达 Webhook 输入约束结构\nexport interface MyWebhookInput {\n  name: string;      // 公司名称\n  industry: string;  // 所属行业\n  country: string;   // 所在国家\n}\n\n// 定义一个类, 包含承接 Webhook 输入的函数\nexport class MyWebhookFunctions {\n  @Function()\n  public ${r.wh_input_func || 'returnWebhookInput'}(company: Company): MyWebhookInput {\n    if (!company.name || !company.industry || !company.country) {\n      throw new UserFacingError("");\n    }\n    return { /* MyWebhookInput 实例 */ };\n  }\n}`
}
function backToRules(fromNav) { activeMenu.value = 'rules'; ruleView.value = 'list'; ruleEditIdx.value = -1; if (fromNav === true) ruleSelKey.value = null }
function notifyCodePreview(r) {
  return `// 只读预览 · 在代码仓库中编辑\n@NotificationFunction("${r.notify_func_code || 'unnamed'}")\npublic Notification build(User recipient, Object subject) {\n  return Notification.builder()\n    .heading("${r.notify_title || '通知标题'}")\n    .content("...")\n    .build();\n}`
}
function addFuncParamRow(rule, required) { rule.func_params.push({ name:'', param_type:'string', required, value_source: required ? 1 : 1, value_content:'' }) }
function addFuncException(rule) { rule.func_exceptions.push({ code:'', message:'' }) }
/* 值来源与入参类型不匹配的粗校验 (静态值不校验) */
function paramTypeMismatch(rule, fp) {
  if (Number(fp.value_source) !== 1) return false
  const src = formParams.value.find(p => p.param_code === fp.value_content)
  return !!(src && fp.param_type && src.param_type !== fp.param_type)
}
function funcCodePreview(r) {
  const args = (r.func_params || []).map(p => `${p.name || 'arg'}: ${p.param_type || 'any'}`).join(', ')
  return `// 只读预览 · 在代码仓库中编辑\n@OntologyEditFunction("${r.func_code || 'unnamed'}", version = "${r.func_version || 'v1'}")\npublic void ${r.func_code || 'run'}(${args}): void {\n  // 函数逻辑在代码仓库维护\n}`
}
function removeRule(rule) {
  rules.value = rules.value.filter(r => r !== rule)
  if (ruleSelKey.value === rule._k) ruleSelKey.value = null
}
/* 添加规则弹框: 按当前规则禁用冲突项 (函数规则与其它 Ontology 编辑规则互斥) */
/* 已启用规则 (禁用态的规则不参与冲突判定, 与 ruleConflicts 口径一致) */
const activeRules = computed(() => rules.value.filter(r => Number(r.status ?? 1) === 1))
function kindDisabled(k) {
  if (k.group !== 'Ontology 编辑规则') return false
  if (activeRules.value.some(r => r.kind === 'function')) return true
  if (k.kind === 'function') return activeRules.value.some(r => kindMeta(r.kind).ruleType === 1 && r.kind !== 'function')
  if (k.kind === 'create_object') return activeRules.value.some(r => r.kind === 'create_object')
  return false
}
function kindDisabledReason(k) {
  if (!kindDisabled(k)) return ''
  if (activeRules.value.some(r => r.kind === 'function')) return '已配置函数规则,不能再叠加其他 Ontology 规则'
  if (k.kind === 'create_object') return '已有「创建对象」规则:同一对象在单次提交中不能被创建两次(主键冲突 / 数据重复)'
  return '已有其他 Ontology 编辑规则,不能再添加函数规则'
}
function addMapping(rule) { rule.prop_mappings.push({ property_code:'', property_name:'', prop_operator:'set', value_source:1, value_content:'', is_required:0 }) }
/* 规则拖拽排序 (顺序生效) */
const rulePickerOpen = ref(false)
const rDragIdx = ref(null)
function onRuleDragStart(i, ev) { rDragIdx.value = i; if (ev?.dataTransfer) ev.dataTransfer.effectAllowed = 'move' }
function onRuleDrop(target) { const from = rDragIdx.value; rDragIdx.value = null; if (from === null || from === target) return; const [it] = rules.value.splice(from, 1); rules.value.splice(target, 0, it) }
function valuePlaceholder(vs) { return ({1:'表单参数编码',2:'静态值',3:'(当前用户,自动)',4:'(系统时间,自动)',5:'关联对象属性'})[Number(vs)] || '' }
function addParam(sec) {
  const target = (typeof sec === 'string' && sec) ? sec : (sections.value[0] || '基础参数')
  formParams.value.push({ ...defaultParam(), section: target })
  openParam(formParams.value.length - 1)
}
/* 字段拖拽排序 (支持跨分区: 落点采用目标分区) */
const fDragIdx = ref(null)
function onFieldDragStart(i, ev) { fDragIdx.value = i; if (ev?.dataTransfer) ev.dataTransfer.effectAllowed = 'move' }
function onFieldDrop(targetParam, sec) {
  const from = fDragIdx.value; fDragIdx.value = null
  if (from == null) return
  const moved = formParams.value[from]
  if (!moved || moved === targetParam) return
  moved.section = sec
  const arr = formParams.value.slice()
  arr.splice(arr.indexOf(moved), 1)
  arr.splice(arr.indexOf(targetParam), 0, moved)
  formParams.value = arr
}
function onFieldDropSection(sec) {
  const from = fDragIdx.value; fDragIdx.value = null
  if (from == null) return
  const moved = formParams.value[from]
  if (!moved) return
  moved.section = sec
  const arr = formParams.value.slice()
  arr.splice(arr.indexOf(moved), 1)
  arr.push(moved)
  formParams.value = arr
}
async function importParams() {
  if (!form.object_class_id) return BL.warning('该动作未关联对象类')
  await loadClassProps()
  const exist = new Set(formParams.value.map(p => p.param_code))
  let n = 0
  for (const p of classProps.value) { if (exist.has(p.code)) continue; formParams.value.push({ ...defaultParam(), param_code:p.code, param_name:p.name, param_type:mapXsd(p.dataType), display_type:autoDisplay(mapXsd(p.dataType)), is_required:p.required?1:0, property_code:p.code }); n++ }
  BL.success(`已导入 ${n} 个参数`)
}

/* —— 表单设计器状态 —— */
const formView = ref('list')       // 'list' | 'detail'
const selIdx = ref(-1)
const detailTab = ref('value')
const globalConf = reactive({
  custom_submit: 0, custom_success: 0,
  width: 'full', labelPos: 'top', labelW: '90', labelAlign: 'left',
  clear: 1, helpOn: 0, helpText: '请按要求填写该字段', reqMark: 'prefix',
  density: 'normal', sectionTitle: 1,
})
const selParam = computed(() => formParams.value[selIdx.value] || null)
const sections = computed(() => {
  const s = []
  formParams.value.forEach(p => { const sec = p.section || '基础参数'; if (!s.includes(sec)) s.push(sec) })
  return s.length ? s : ['基础参数']
})
function paramsOfSection(sec) { return formParams.value.map((p, i) => ({ p, i })).filter(x => (x.p.section || '基础参数') === sec) }
function openParam(i) { activeMenu.value = 'form'; selIdx.value = i; detailTab.value = 'value'; formView.value = 'detail'; nextTick(() => { scrollEl.value && (scrollEl.value.scrollTop = 0) }) }
function backToList() { activeMenu.value = 'form'; formView.value = 'list'; selIdx.value = -1 }

const scrollEl = ref(null)
function switchDetailTab(k) { detailTab.value = k; nextTick(() => { scrollEl.value && (scrollEl.value.scrollTop = 0) }) }
function ovCountOf(target) { return (selParam.value?.overrides || []).filter(o => o.target === target).length }
function addOverrideFor(target) {
  selParam.value.overrides.push({ target, value: target === 'disabled' ? 1 : 0, condition: '' })
  switchDetailTab('override')
}

/* —— 表单级显示配置 → 参数级覆盖 的继承解析 ——
   dsp 为空对象表示全部继承; 某 key 有值即该项被本参数单独覆盖 */
const DSP_KEYS = ['width', 'labelPos', 'labelW', 'clear', 'helpOn']
const DSP_WIDTHS = [{ value:'full', label:'占满整行' }, { value:'half', label:'二分之一' }, { value:'third', label:'三分之一' }, { value:'quarter', label:'四分之一' }]
const DSP_LABEL_POS = [{ value:'top', label:'顶部显示' }, { value:'left', label:'左侧显示' }, { value:'none', label:'隐藏标签' }]
const DSP_LABEL_WS = [{ value:'auto', label:'自适应(按最长标签)' }, { value:'80', label:'80px' }, { value:'90', label:'90px' }, { value:'100', label:'100px' }, { value:'120', label:'120px' }, { value:'160', label:'160px' }]
const DSP_LABEL_ALIGNS = [{ value:'left', label:'左对齐' }, { value:'right', label:'右对齐' }]
function dspOf(p) {
  const d = p?.dsp || {}
  return {
    width: d.width ?? globalConf.width,
    labelPos: d.labelPos ?? globalConf.labelPos,
    labelW: d.labelW ?? globalConf.labelW,
    clear: d.clear ?? globalConf.clear,
    helpOn: d.helpOn ?? globalConf.helpOn,
  }
}
function isInherit(p, k) { return (p?.dsp || {})[k] == null }
function setDsp(p, k, v) { if (!p.dsp) p.dsp = {}; p.dsp[k] = v }
function toggleInherit(p, k) {
  if (!p.dsp) p.dsp = {}
  if (isInherit(p, k)) p.dsp[k] = dspOf(p)[k]   // 覆盖时以当前生效值为起点, 避免取消继承瞬间值跳变
  else delete p.dsp[k]
}
function dspOverCount(p) { return DSP_KEYS.filter(k => !isInherit(p, k)).length }
const DSP_LABELS = { width:'字段宽度', labelPos:'标签位置', labelW:'标签宽度', clear:'清空按钮', helpOn:'帮助文案' }
function dspOverLabels(p) { return DSP_KEYS.filter(k => !isInherit(p, k)).map(k => DSP_LABELS[k]).join('、') }
const dspOverParams = computed(() => formParams.value.map((p, i) => ({ p, i })).filter(x => dspOverCount(x.p) > 0))
function resetDsp(p) { p.dsp = {} }
function gotoGlobalDisplay() {
  backToList()
  nextTick(() => { document.querySelector('.gd-card')?.scrollIntoView({ behavior: 'smooth', block: 'center' }) })
}
/* 标签宽度「自适应」按当前左标签字段中最长的名称估算, 保证同一表单内控件左边缘对齐 */
function autoLabelW() {
  const ns = formParams.value.filter(p => dspOf(p).labelPos === 'left').map(p => (p.param_name || p.param_code || '').length)
  if (!ns.length) return 80
  return Math.min(200, Math.max(56, Math.max(...ns) * 14 + 12))
}
function labelWpx(p) { const v = dspOf(p).labelW; return v === 'auto' ? autoLabelW() : Number(v) || 90 }
function previewHelp(p) { return dspOf(p).helpOn ? (p.help_text || globalConf.helpText) : '' }
function widthPct(p) { return ({ full:'100%', half:'50%', third:'33.33%', quarter:'25%' })[dspOf(p).width] || '100%' }
const previewFull = ref(false)
const previewGroups = computed(() => sections.value
  .map(sec => ({ sec, items: visibleParams.value.filter(x => (x.p.section || '基础参数') === sec) }))
  .filter(g => g.items.length))
async function removeParamAt(i) {
  const p = formParams.value[i]
  if (!p) return
  const name = p.param_name || p.param_code || '该参数'
  const refWarn = isRefByRule(p.param_code) ? ',该参数已在规则中被引用,删除后相关引用将失效' : ''
  const ok = await BL.confirm({ title: '删除参数', content: `确定删除参数「${name}」?${refWarn}`, danger: true, okText: '删除' })
  if (!ok) return
  formParams.value.splice(i, 1)
  if (selIdx.value === i) backToList()
}
function addSection() {
  let n = sections.value.length + 1, name = `分区 ${n}`
  while (sections.value.includes(name)) { n++; name = `分区 ${n}` }
  formParams.value.push({ ...defaultParam(), param_code: '', param_name: '新参数', section: name })
}
async function renameSection(sec) {
  const name = await BL.prompt({
    title: '重命名分区', label: '分区名称', defaultValue: sec,
    validate: v => {
      const t = String(v || '').trim()
      if (!t) return '请输入分区名称'
      if (t !== sec && sections.value.includes(t)) return '分区名称已存在'
      return true
    },
  })
  if (name == null) return
  const nn = String(name).trim()
  if (!nn || nn === sec) return
  formParams.value.forEach(p => { if ((p.section || '基础参数') === sec) p.section = nn })
}
async function removeSection(sec) {
  if (sections.value.length <= 1) return BL.warning('至少保留一个分区')
  const inSec = formParams.value.filter(p => (p.section || '基础参数') === sec)
  const real = inSec.filter(p => String(p.param_code || '').trim()).length
  if (inSec.length) {
    const ok = await BL.confirm({ title: '删除分区', content: `删除分区「${sec}」?该分区下 ${inSec.length} 个参数将一并删除${real ? `(含 ${real} 个已配置参数)` : ''}。`, danger: true, okText: '删除' })
    if (!ok) return
  }
  formParams.value = formParams.value.filter(p => (p.section || '基础参数') !== sec)
  if (formView.value === 'detail') backToList()
}
function dtMeta(t) { return DATA_TYPE_META[t] || DATA_TYPE_META.string }
function displayMeta(v) { return DISPLAY_TYPES.find(d => d.v === v) || DISPLAY_TYPES[0] }
function isRefByRule(code) {
  if (!String(code || '').trim()) return false
  return rules.value.some(r =>
    (r.prop_mappings || []).some(m => Number(m.value_source) === 1 && m.value_content === code) ||
    (r.func_params || []).some(fp => Number(fp.value_source) === 1 && fp.value_content === code))
}
function addOption(p) { p.options.push({ value: '', label: '' }) }
function mapXsd(dt){ const s=String(dt||'').toLowerCase(); if(/(int|decimal|double|float)/.test(s))return'number'; if(s.includes('bool'))return'boolean'; if(s.includes('date')||s.includes('time'))return'date'; return'string' }

async function onSave() {
  if (!String(form.rdfs_label||'').trim()) { activeMenu.value='overview'; editMode.value=true; return BL.warning('请填写动作名称') }
  if (ruleConflicts.value.length) { activeMenu.value = 'rules'; ruleView.value = 'list'; return BL.error('存在无效规则组合,请先修正:' + ruleConflicts.value[0]) }
  saving.value = true
  try {
    const body = { ...form,
      form_params: formParams.value.filter(p => String(p.param_code).trim()).map((p,i) => ({ param_code:p.param_code, param_name:p.param_name, param_type:p.param_type, is_required:p.is_required, default_value:p.value_source===5?null:p.default_value, config:JSON.stringify(paramConfig(p)), sort:i })),
      rules: rules.value.map((r,i) => ({ action_type:form.action_type, rule_type:kindMeta(r.kind).ruleType, rule_name:r.rule_name, status:Number(r.status??1), target_param_code:r.target_param_code||null, link_type_code:r.link_type_code||null, rule_config:JSON.stringify(ruleConfig(r)), sort:i, prop_mappings:(r.prop_mappings||[]).filter(m=>String(m.property_code).trim()).map((m,j)=>({ property_code:m.property_code, property_name:m.property_name||null, prop_operator:m.prop_operator, value_source:m.value_source, value_content:m.value_content||null, is_required:m.is_required, sort:j })) })),
      submit_standard: { enabled:submit.enabled, validate_mode:submitTree.logic, error_message:submit.error_message||null, nodes: submit.enabled ? flattenSubmitTree() : [] },
    }
    if (formParams.value.length) body.form_enabled = 1
    body.submit_criteria_enabled = submit.enabled ? 1 : form.submit_criteria_enabled
    const meta = readMeta(); meta.form_global = { ...globalConf }
    body.metadata = JSON.stringify(meta)
    await actionTypeApi.update(form.id, body)
    BL.success('已保存'); editMode.value = false; formSnapshot = JSON.stringify(pickEditable()); fullSnapshot = snapshotAll()
    emit('saved')
  } catch (e) { BL.error(e?.msg || '保存失败') } finally { saving.value = false }
}
async function onDelete() {
  const ok = await BL.confirm({ title:'删除动作', content:`确定删除「${form.rdfs_label||form.api_name}」?`, danger:true, okText:'删除' })
  if (!ok) return
  try { await actionTypeApi.remove(form.id); BL.success('已删除'); emit('deleted'); emit('update:open', false) } catch (e) { BL.error(e?.msg||'删除失败') }
}
async function onClose() {
  if (fullSnapshot && snapshotAll() !== fullSnapshot) {
    const ok = await BL.confirm({ title: '放弃修改', content: '当前有未保存的修改,关闭后将丢失,确定关闭?', danger: true, okText: '关闭', cancelText: '继续编辑' })
    if (!ok) return
  }
  emit('update:open', false)
}

/* 工具 */
function statusLabel(s) { return ({ 0:'草稿', 1:'已发布', 2:'已停用' })[Number(s)] || '草稿' }
function statusTagCls(s) { return ({ 0:'', 1:'bl-tag-success', 2:'bl-tag-warning' })[Number(s)] || '' }
function compileLabel(s) { return ({ 0:'未编译', 1:'编译通过', 2:'编译失败' })[Number(s)] || '未编译' }
function shortTime(t) { if (!t) return '—'; return String(t).slice(0, 19) }
</script>

<style scoped>
/* ===== 抽屉外壳 (右锚定, 对标对象详情) ===== */
.adw-drawer {
  position: fixed; top: 0; right: 0; bottom: 0;
  background: var(--bl-bg-2);
  border-left: 1px solid var(--bl-border-strong);
  box-shadow: -12px 0 32px rgba(0,0,0,0.22), -2px 0 6px rgba(0,0,0,0.12);
  display: flex; flex-direction: column; overflow: hidden;
  z-index: 1010; min-width: 760px;
}
:root[data-theme="dark"] .adw-drawer {
  box-shadow: -16px 0 48px rgba(0,0,0,0.65), -2px 0 8px rgba(0,0,0,0.5), inset 1px 0 0 rgba(255,255,255,0.05);
}
.adw-drag-handle { position: absolute; left: -2px; top: 0; bottom: 0; width: 5px; cursor: col-resize; background: transparent; transition: background-color .15s; z-index: 3; }
.adw-drag-handle:hover, .adw-drag-handle.is-resizing { background: var(--bl-primary); }
.adw-drawer-enter-active, .adw-drawer-leave-active { transition: transform .25s ease, opacity .2s ease; }
.adw-drawer-enter-from, .adw-drawer-leave-to { transform: translateX(24px); opacity: 0; }

/* 抽屉头部条 */
.adw-drawer-hd { flex: 0 0 auto; display: flex; align-items: center; justify-content: space-between; gap: 12px; height: 52px; padding: 0 12px 0 18px; background: var(--bl-bg-1); border-bottom: 1px solid var(--bl-border); }
.adw-drawer-hd-l { display: flex; align-items: center; gap: 10px; min-width: 0; flex: 1; }
.adw-drawer-hd-r { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.adw-drawer-title { display: flex; align-items: center; gap: 8px; min-width: 0; font-size: 15px; font-weight: 600; color: var(--bl-text-1); }
.adw-drawer-title .bl-truncate { max-width: 340px; }
.adw-drawer-sep { width: 1px; height: 18px; background: var(--bl-divider); margin: 0 4px; }

/* 抽屉主体: 三栏 */
.adw-drawer-body { flex: 1; display: flex; min-height: 0; overflow: hidden; }

/* 左导航 */
.adw-nav { flex: 0 0 220px; background: var(--bl-bg-1); border-right: 1px solid var(--bl-border); display: flex; flex-direction: column; overflow: hidden; }
.adw-title-ic { width: 30px; height: 30px; border-radius: 7px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.adw-menu { flex: 1; overflow-y: auto; padding: 8px; }
.adw-menu-item { display: flex; align-items: center; gap: 8px; padding: 8px 6px; border-radius: 6px; font-size: 13.5px; color: var(--bl-text-2); cursor: pointer; border-left: 2px solid transparent; }
.adw-menu-item:hover { background: var(--bl-bg-hover); }
.adw-menu-item.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 600; border-left-color: var(--bl-primary); }
.adw-menu-ic { display: inline-flex; }
.adw-menu-badge { margin-left: auto; min-width: 16px; height: 16px; padding: 0 5px; border-radius: 8px; background: var(--bl-bg-3); color: var(--bl-text-2); font-size: 10px; display: inline-flex; align-items: center; justify-content: center; font-weight: 400; }
.adw-menu-item.is-on .adw-menu-badge { background: var(--bl-primary); color: #fff; }
.adw-menu-label { flex: 1; min-width: 0; }
.adw-menu-chev { margin-left: 4px; color: var(--bl-text-3); display: inline-flex; border-radius: 4px; transition: transform .15s, background .12s; }
.adw-menu-chev:hover { background: var(--bl-bg-hover); }
.adw-menu-chev.is-open { transform: rotate(90deg); }
/* 表单结构树 */
.adw-tree { padding: 4px 0 4px 10px; }
.adw-tree-sec { font-size: 12px; color: var(--bl-text-3); font-weight: 600; padding: 6px 12px 4px; }
.adw-tree-item { display: flex; align-items: center; gap: 6px; padding: 6px 12px 6px 20px; font-size: 12.5px; color: var(--bl-text-2); cursor: pointer; border-radius: 6px; }
.adw-tree-item:hover { background: var(--bl-bg-hover); }
.adw-tree-item.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); }
.adw-tree-ic { color: var(--bl-text-3); display: inline-flex; }
.adw-tree-empty { padding: 8px 20px; font-size: 12px; color: var(--bl-text-3); }

/* 中间内容 */
.adw-main { flex: 1; min-width: 0; overflow-y: auto; padding: 16px; }
.adw-page-hd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.adw-page-title { font-size: 16px; font-weight: 600; }

/* 概览 */
.adw-ov { max-width: none; }
/* 非编辑态: 字段以纯文本呈现, 去掉输入框边线/底色 */
.adw-ov.is-view .bl-input:disabled,
.adw-ov.is-view textarea.bl-input:disabled,
.adw-ov.is-view .bl-textarea:disabled { background: transparent; border-color: transparent; padding-left: 0; padding-right: 0; color: var(--bl-text-1); -webkit-text-fill-color: var(--bl-text-1); opacity: 1; cursor: default; resize: none; }
.adw-ov.is-view :deep(.bs-control.is-disabled) { background: transparent; border-color: transparent; padding-left: 0; opacity: 1; cursor: default; }
.adw-ov.is-view :deep(.bs-control.is-disabled .bs-arrow) { display: none; }
/* 系统元信息(只读卡): 字段恒为纯文本, 不显示输入框边线/底色 */
.adw-card-ro .bl-input, .adw-card-ro .bl-input:disabled { background: transparent !important; background-color: transparent !important; border-color: transparent !important; box-shadow: none !important; padding-left: 0; padding-right: 0; color: var(--bl-text-1); -webkit-text-fill-color: var(--bl-text-1); opacity: 1; cursor: default; }
.adw-ov-hd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.adw-ov-hd-l { display: flex; align-items: center; gap: 12px; }
.adw-ov-ic { width: 40px; height: 40px; border-radius: 9px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.adw-ov-name { font-size: 18px; font-weight: 700; color: var(--bl-text-1); }
.adw-ov-hd-r { display: inline-flex; gap: 8px; }
.adw-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.adw-stat { display: flex; align-items: center; gap: 12px; padding: 14px 16px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 10px; }
.adw-stat-ic { width: 36px; height: 36px; border-radius: 8px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.adw-stat-num { font-size: 20px; font-weight: 700; color: var(--bl-text-1); line-height: 1.2; }
.adw-stat-sub { font-size: 12px; font-weight: 400; color: var(--bl-text-3); }
.adw-stat-lbl { font-size: 12px; color: var(--bl-text-3); margin-top: 2px; }
.adw-card { background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 10px; padding: 16px 18px; margin-bottom: 14px; }
.adw-card-ro { background: var(--bl-bg-1); }
.adw-card-hd { font-size: 13px; font-weight: 600; color: var(--bl-text-1); padding-left: 8px; border-left: 3px solid var(--bl-primary); margin-bottom: 14px; line-height: 1; }
.adw-card-ro .adw-card-hd { border-left-color: var(--bl-text-3); }
.adw-card-hd-flex { display: flex; align-items: center; justify-content: space-between; line-height: 1.2; }
.adw-card-hd-act { display: inline-flex; gap: 8px; }
/* 概览分组标题栏 */
.adw-ov-ghd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.adw-ov-gtitle { font-size: 15px; font-weight: 700; color: var(--bl-text-1); }
/* 面板内小节间距 */
.adw-sec-gap { margin-top: 22px; }
/* 只读态图标行 */
.adw-ro-icon { display: inline-flex; align-items: center; gap: 8px; height: 32px; color: var(--bl-text-1); }
.adw-ro-icon .bl-mono { font-size: 13px; }
.adw-color-sw { width: 18px; height: 18px; border-radius: 4px; display: inline-block; border: 1px solid var(--bl-border); }
/* 概览图标选择器: 横向一行, 图标更窄, 更多选择内联 */
.adw-ov :deep(.ipf) { flex-direction: row; flex-wrap: wrap; align-items: center; gap: 6px; }
.adw-ov :deep(.ipf .icon-grid) { display: flex; gap: 6px; max-width: none; }
.adw-ov :deep(.ipf .icon-cell) { width: 28px; max-width: 28px; height: 28px; aspect-ratio: auto; }
.adw-ov :deep(.ipf .ipf-more-row) { width: auto; max-width: none; padding: 0 10px; }
.gfc-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 16px; }
.gfc-item { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 10px 14px; border: 1px solid var(--bl-border); border-radius: 8px; font-size: 13px; color: var(--bl-text-2); }
.adw-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 20px; }
.adw-fld { display: flex; flex-direction: column; gap: 5px; min-width: 0; }
.adw-lbl { font-size: 12px; color: var(--bl-text-2); }
.adw-lbl i { color: #f53f3f; font-style: normal; }
.adw-switch-row { display: flex; gap: 24px; }
.adw-sw { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; color: var(--bl-text-2); cursor: pointer; }
.adw-color-row { display: flex; align-items: center; gap: 8px; }
.adw-color-dot { width: 28px; height: 28px; border-radius: 6px; border: 1px solid var(--bl-border); flex-shrink: 0; }
.adw-main .bl-input:disabled { background: var(--bl-bg-2); color: var(--bl-text-2); }

/* 规则/表单 移植样式 */
.ate-grp-hd { display: flex; align-items: center; justify-content: space-between; margin: 8px 0 10px; }
.ate-grp-title { font-size: 13px; font-weight: 600; display: inline-flex; align-items: center; gap: 7px; }
.ate-grp-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.ate-rule-card { border: 1px solid var(--bl-border); border-radius: 8px; padding: 10px; margin-bottom: 10px; background: var(--bl-bg-1); }
.ate-rule-hd { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }
.at-del-op { color: #f53f3f; }
.ate-mini-table { width: 100%; font-size: 12px; background: var(--bl-bg-1); }
.ate-mini-table thead th { background: var(--bl-bg-2); font-weight: 600; height: 30px; padding: 0 6px; white-space: nowrap; color: var(--bl-text-2); }
.ate-mini-table thead th.t-left { text-align: left; }
.ate-mini-table td { padding: 3px 5px; border-top: 1px solid var(--bl-divider); }
.ate-mini-table td.t-center { text-align: center; }
.ate-mini-table .bl-input-xs { height: 28px; padding: 0 6px; font-size: 12px; }
.adw-row-sel { background: var(--bl-primary-soft); }

/* 表单结构树 (字段双图标) */
.adw-tree-root { display: flex; align-items: center; gap: 6px; padding: 7px 12px; font-size: 13px; font-weight: 600; color: var(--bl-text-2); cursor: pointer; border-radius: 6px; }
.adw-tree-root:hover { background: var(--bl-bg-hover); }
.adw-tree-root.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); }
.adw-tree-field { display: flex; align-items: center; gap: 7px; padding: 7px 6px 7px 8px; font-size: 12.5px; color: var(--bl-text-2); cursor: pointer; border-radius: 6px; border-left: 2px solid transparent; }
.adw-tree-field:hover { background: var(--bl-bg-hover); }
.adw-tree-field:hover .adw-tree-disp { color: var(--bl-text-1); }
.adw-tree-field.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); border-left-color: var(--bl-primary); }
.adw-tree-field.is-on .adw-tree-disp { color: var(--bl-primary); }
.adw-tree-dt { width: 18px; height: 18px; border-radius: 4px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.adw-tree-field.is-off { opacity: .55; }
.adw-tree-fname { flex: 1; min-width: 0; }
.adw-tree-disp { flex-shrink: 0; color: var(--bl-text-3); display: inline-flex; }

/* 表单列表页 - 分区 */
.fd-part { border: 1px solid var(--bl-border); border-radius: 10px; margin-bottom: 12px; overflow: hidden; background: var(--bl-bg-1); }
.fd-part-hd { display: flex; align-items: center; gap: 6px; padding: 10px 14px; border-bottom: 1px solid var(--bl-divider); color: var(--bl-text-2); }
.fd-part-hd > span:first-child { color: var(--bl-text-3); display: inline-flex; }
.fd-part-name { display: inline-flex; align-items: center; font-size: 13px; font-weight: 600; color: var(--bl-text-1); cursor: pointer; border-radius: 5px; padding: 1px 4px; margin-left: -4px; }
.fd-part-name:hover { color: var(--bl-primary); background: var(--bl-bg-hover); }
.fd-part-edit { margin-left: 5px; display: inline-flex; color: var(--bl-text-3); opacity: 0; transition: opacity .12s; }
.fd-part-name:hover .fd-part-edit { opacity: 1; }
.fd-part-count { margin-left: auto; padding: 0 7px; border-radius: 10px; background: var(--bl-bg-2); color: var(--bl-text-2); font-size: 11px; }
.fd-part-del { margin-left: 6px; padding: 2px; border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; border-radius: 5px; opacity: 0; transition: opacity .12s, color .12s, background .12s; }
.fd-part:hover .fd-part-del { opacity: 1; }
.fd-part-del:hover { color: #f53f3f; background: color-mix(in srgb, #f53f3f 12%, transparent); }
.fd-part-body { padding: 0; }
/* 字段行: 扁平分组列表项 (去内边框, 靠分隔线) */
.fd-row { display: flex; align-items: center; gap: 10px; padding: 11px 14px; border-bottom: 1px solid var(--bl-divider); cursor: pointer; background: transparent; transition: background .12s; }
.fd-row:hover { background: var(--bl-bg-hover); }
.fd-row.is-dragging { opacity: .4; }
.fd-grip { color: var(--bl-text-3); cursor: grab; display: inline-flex; flex-shrink: 0; opacity: 0; transition: opacity .12s; }
.fd-row:hover .fd-grip { opacity: 1; }
.fd-grip:active { cursor: grabbing; }
/* 分区内添加参数 */
.fd-part-add { display: flex; align-items: center; justify-content: center; gap: 4px; width: 100%; padding: 9px; background: transparent; border: 0; color: var(--bl-text-3); font-size: 12.5px; cursor: pointer; transition: color .12s, background .12s; }
.fd-part-add:hover { color: var(--bl-primary); background: var(--bl-bg-hover); }
.fd-dt { width: 24px; height: 24px; border-radius: 6px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.fd-row-txt { flex: 1; min-width: 0; }
.fd-row-name { font-size: 13px; color: var(--bl-text-1); }
.fd-row-warn { font-size: 11px; color: var(--bl-warning); margin-top: 2px; }
.fd-disp { display: inline-flex; align-items: center; gap: 4px; flex-shrink: 0; padding: 3px 8px; border-radius: 6px; background: var(--bl-bg-2); color: var(--bl-text-2); }
.fd-disp-ic { display: inline-flex; color: var(--bl-text-3); }
.fd-disp-txt { font-size: 12px; }
.fd-chev { color: var(--bl-text-3); display: inline-flex; flex-shrink: 0; }
.fd-part-empty { padding: 14px; text-align: center; color: var(--bl-text-3); font-size: 12px; }
.fd-actions { display: flex; gap: 10px; margin: 12px 0 0; }

/* 参数详情页 */
/* 紧凑头部 (单行: 返回 + 参数信息) */
.fd-phd2 { display: flex; align-items: center; gap: 8px; margin: -8px -16px 0; padding: 4px 16px; background: var(--bl-bg-1); border-bottom: 1px solid var(--bl-divider); height: 43px; }
.fd-back { display: inline-flex; align-items: center; gap: 2px; padding: 3px 8px 3px 4px; font-size: 12.5px; color: var(--bl-text-2); background: transparent; border: 0; cursor: pointer; border-radius: 6px; flex-shrink: 0; }
.fd-back:hover { color: var(--bl-primary); background: var(--bl-bg-hover); }
.fd-phd2-sep { width: 1px; height: 16px; background: var(--bl-divider); flex-shrink: 0; }
.fd-pname2 { border: 0; outline: none; background: transparent; font-size: 16px; font-weight: 700; color: var(--bl-text-1); padding: 0; min-width: 80px; max-width: 240px; }
.fd-pname2:focus { border-bottom: 1px solid var(--bl-primary); }

/* 表单详情: 头部固定 + 下方独立滚动, 收紧内边距节省高度 */
.adw-page.is-detail { margin: -16px; height: calc(100% + 32px); padding: 8px 16px 0; display: flex; flex-direction: column; min-height: 0; }
.fd-detail-hd { flex: 0 0 auto; background: var(--bl-bg-2); }
.fd-detail-body { flex: 1; min-height: 0; overflow-y: auto; margin-right: -16px; padding: 10px 8px 20px 0; }

/* 锚点导航 */
.fd-tabs { display: flex; gap: 2px; padding: 8px 0 4px; background: var(--bl-bg-2); }
.fd-tab-btn { padding: 6px 16px; font-size: 13px; color: var(--bl-text-2); cursor: pointer; background: transparent; border: 0; border-radius: 6px; transition: background .12s, color .12s; }
.fd-tab-btn:hover { color: var(--bl-text-1); background: var(--bl-bg-hover); }
.fd-tab-btn.is-on { color: var(--bl-primary); background: var(--bl-primary-soft); font-weight: 600; }

/* 区块容器 */
.fd-tab-wrap { max-width: 780px; }
.fd-sec { scroll-margin-top: 8px; }

/* 通用设置 三宫格 */
.fd-triple { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0; }
.fd-tri { padding: 4px 16px; border-right: 1px solid var(--bl-divider); }
.fd-tri:first-child { padding-left: 0; }
.fd-tri:last-child { border-right: 0; padding-right: 0; }
.fd-tri-hd { display: flex; align-items: center; gap: 8px; font-size: var(--bl-fs-13); font-weight: 600; margin-bottom: 4px; }
.fd-tri-desc { font-size: var(--bl-fs-12); color: var(--bl-text-3); line-height: 1.6; margin-bottom: 10px; min-height: 34px; }
.fd-ovr { font-size: 12px; color: var(--bl-primary); cursor: pointer; }
.fd-ovr:hover { text-decoration: underline; }
.fd-ro-txt { font-size: var(--bl-fs-13); color: var(--bl-text-2); line-height: 32px; }

/* 继承 / 覆盖行: 控件 + 「跟随全局」勾选 + 覆盖标记 */
.fd-inh-tip { font-size: var(--bl-fs-12); color: var(--bl-text-2); background: var(--bl-primary-soft); border: 1px solid var(--bl-primary-border);
  border-radius: var(--bl-radius-2); padding: 8px 12px; line-height: 1.7; margin-bottom: var(--bl-sp-3); }
.fd-inh-row { display: flex; align-items: center; gap: var(--bl-sp-3); padding: 8px 0; border-bottom: 1px dashed var(--bl-divider); }
.fd-inh-row:last-child { border-bottom: 0; }
.fd-inh-row.is-na { opacity: .5; }
.fd-inh-lbl { width: 90px; flex-shrink: 0; font-size: var(--bl-fs-13); color: var(--bl-text-3); }
.fd-inh-row > :deep(.bs), .fd-inh-row > .bl-input { width: 220px; flex-shrink: 0; }
.fd-inh-ctl { width: 220px; flex-shrink: 0; display: flex; align-items: center; }
.fd-inh-ck { display: inline-flex; align-items: center; gap: 6px; font-size: var(--bl-fs-12); color: var(--bl-text-3); cursor: pointer; flex-shrink: 0; }
.adw-showsw.is-lock { opacity: .55; pointer-events: none; }

/* 全局显示配置卡 */
.gd-sub { font-size: var(--bl-fs-13); font-weight: 600; color: var(--bl-text-2); margin: var(--bl-sp-4) 0 var(--bl-sp-2); }
.gd-sub:first-of-type { margin-top: var(--bl-sp-2); }
.gd-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--bl-sp-3) var(--bl-sp-5); }
.gd-over { display: flex; align-items: center; gap: var(--bl-sp-2); padding: 7px 10px; border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-2); margin-bottom: 6px; font-size: var(--bl-fs-13); cursor: pointer; }
.gd-over:hover { border-color: var(--bl-primary); background: var(--bl-primary-soft); }

/* 约束-模式选项卡 */
.fd-modetab { display: flex; gap: 4px; background: var(--bl-bg-2); padding: 3px; border-radius: 8px; margin-bottom: 14px; }
.fd-mode { flex: 1; padding: 7px 0; font-size: 13px; text-align: center; border: 0; background: transparent; color: var(--bl-text-2); cursor: pointer; border-radius: 6px; }
.fd-mode.is-on { background: var(--bl-bg-1); color: var(--bl-primary); font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,.08); }
.fd-mode-body { display: flex; flex-direction: column; gap: 10px; }
.fd-ck { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--bl-text-2); }
.fd-src { display: flex; gap: 10px; margin-bottom: 12px; }
.fd-src-opt { flex: 1; text-align: center; padding: 9px 0; border: 1px solid var(--bl-border); border-radius: 8px; font-size: 13px; color: var(--bl-text-2); cursor: pointer; }
.fd-src-opt.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 600; }
.fd-opt { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
.fd-warn { background: #FEF3C7; border: 1px solid #FDE68A; border-radius: 6px; padding: 8px 12px; font-size: 12px; color: #92400E; }
:root[data-theme="dark"] .fd-warn { background: color-mix(in srgb, #D97706 16%, transparent); border-color: color-mix(in srgb, #D97706 40%, transparent); color: #FBBF24; }

/* 显示组件网格 */
.fd-disp-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.fd-disp-opt { display: flex; align-items: center; gap: 8px; padding: 10px 12px; border: 1px solid var(--bl-border); border-radius: 8px; background: var(--bl-bg-1); color: var(--bl-text-2); cursor: pointer; font-size: 13px; }
.fd-disp-opt:hover { border-color: var(--bl-primary); }
.fd-disp-opt.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 600; }

/* 覆盖 */
.fd-ov-row { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }

/* 开关 (通用设置/预览) */
.adw-showsw { display: inline-block; width: 40px; height: 22px; border-radius: 11px; background: var(--bl-bg-3, #c9cdd4); position: relative; cursor: pointer; transition: background .15s; vertical-align: middle; flex-shrink: 0; }
.adw-showsw.is-on { background: var(--bl-primary); }
.adw-showsw-dot { position: absolute; left: 2px; top: 2px; width: 18px; height: 18px; border-radius: 50%; background: #fff; transition: left .15s; box-shadow: 0 1px 2px rgba(0,0,0,.3); }
.adw-showsw.is-on .adw-showsw-dot { left: 20px; }

/* 预览增强 */
.adw-preview-fld { cursor: pointer; border-radius: 6px; padding: 4px 12px 8px; }
.adw-preview-fld.is-sel { background: var(--bl-primary-soft); outline: 1px solid var(--bl-primary); }
.adw-preview-input.is-ro { background: var(--bl-bg-2); color: var(--bl-text-3); }
.adw-preview-input { justify-content: space-between; }
.adw-pv-switch { padding: 2px 0; }
/* 预览: 单选按钮组 / 复选框 */
.adw-pv-opts { display: flex; flex-wrap: wrap; gap: 8px 18px; padding: 5px 0; }
.adw-pv-opt { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: var(--bl-text-2); }
.adw-pv-radio { width: 14px; height: 14px; border-radius: 50%; border: 1.5px solid var(--bl-border-strong); flex-shrink: 0; }
.adw-pv-checkbox { width: 14px; height: 14px; border-radius: 3px; border: 1.5px solid var(--bl-border-strong); flex-shrink: 0; }
.adw-pv-help { font-size: 11px; color: var(--bl-text-3); margin-top: 4px; }

/* 右预览 */
.adw-preview { flex: 0 0 384px; background: var(--bl-bg-1); border-left: 1px solid var(--bl-border); display: flex; flex-direction: column; overflow: hidden; }
.adw-preview-hd { padding: 12px 16px; font-size: 13px; font-weight: 600; border-bottom: 1px solid var(--bl-divider); }
.adw-preview-body { flex: 1; overflow-y: auto; padding: 16px 6px 16px 16px; }
.adw-preview-form {border-radius: 10px; padding: 0px; }
.adw-preview-title { font-size: 15px; font-weight: 600; margin-bottom: 14px; }
.adw-preview-fld { margin-bottom: 12px; }
.adw-preview-lbl { font-size: 12px; color: var(--bl-text-2); margin-bottom: 5px; }
/* 全屏预览: 复用同一段 DOM, 仅改容器形态, 避免与侧栏预览重复维护两套结构 */
.adw-preview.is-full { position: fixed; inset: 48px; width: auto; z-index: 30; border-radius: var(--bl-radius-3);
  box-shadow: var(--bl-shadow-3); border: 1px solid var(--bl-border); }
.adw-preview.is-full .adw-preview-form { max-width: 1000px; margin: 0 auto; }
.adw-pv-tip { font-size: 11px; color: var(--bl-text-3); margin: -4px 0 10px; line-height: 1.6; }
.adw-pv-sec { font-size: 11px; color: var(--bl-text-3); margin: 14px 0 8px; padding-bottom: 4px; border-bottom: 1px dashed var(--bl-border); }
.adw-pv-grid { display: flex; flex-wrap: wrap; margin: 0 -6px; }
/* 默认单列; 仅全屏预览时由内联 width 覆盖为真实栅格比例 */
.adw-pv-grid > .adw-preview-fld { width: 100%; box-sizing: border-box; padding-left: 6px; padding-right: 6px; }
/* 字段间距: 表单级样式, 无参数级覆盖 */
.adw-preview-form.is-compact .adw-preview-fld { margin-bottom: 6px; }
.adw-preview-form.is-loose .adw-preview-fld { margin-bottom: 20px; }
/* 标签位于左侧: 标签列定宽 + 控件占满剩余, 宽度由 labelWpx 内联给出 */
.adw-preview-fld.is-side { display: flex; align-items: flex-start; gap: 8px; }
.adw-preview-fld.is-side .adw-preview-lbl { margin-bottom: 0; padding-top: 7px; padding-right: 6px; }
.adw-preview-fld.is-side .adw-pv-ctl { flex: 1; min-width: 0; }
.adw-pv-req { color: var(--bl-danger); font-style: normal; margin-right: 2px; }
.adw-pv-req.is-suffix { margin-right: 0; margin-left: 2px; }
.adw-preview-input { height: 32px; border: 1px solid var(--bl-border); border-radius: var(--bl-radius-2); background: var(--bl-bg-1); display: flex; align-items: center; padding: 0 10px; font-size: 12px; color: var(--bl-text-3); }
.adw-preview-ft { padding: 10px 16px; border-top: 1px solid var(--bl-divider); }

/* ===== 规则折叠卡片 (P3) ===== */
.rl-card { border: 1px solid var(--bl-border); border-radius: 10px; background: var(--bl-bg-1); margin-bottom: 10px; overflow: hidden; }
.rl-card.is-dragging { opacity: .5; border-style: dashed; }
.rl-conflict { background: color-mix(in srgb, #f53f3f 8%, transparent); border: 1px solid color-mix(in srgb, #f53f3f 30%, transparent); border-radius: 8px; padding: 10px 14px; margin-bottom: 12px; }
.rl-conflict-hd { display: flex; align-items: center; gap: 6px; font-size: 12.5px; font-weight: 600; color: #f53f3f; margin-bottom: 4px; }
.rl-conflict-item { font-size: 12px; color: var(--bl-text-2); line-height: 1.7; padding-left: 4px; }
.rl-sw { flex-shrink: 0; margin-right: 2px; width: 32px; height: 18px; border-radius: 9px; }
.rl-sw .adw-showsw-dot { width: 14px; height: 14px; }
.rl-sw.is-on .adw-showsw-dot { left: 16px; }
/* 函数规则 完整编辑态 */
.fe-warn { background: #FEF3C7; border: 1px solid #FDE68A; border-radius: 8px; padding: 10px 14px; font-size: 12.5px; line-height: 1.6; color: #92400E; margin-bottom: 14px; }
:root[data-theme="dark"] .fe-warn { background: color-mix(in srgb, #D97706 16%, transparent); border-color: color-mix(in srgb, #D97706 40%, transparent); color: #FBBF24; }
.fe-info { display: flex; align-items: flex-start; gap: 8px; background: var(--bl-primary-soft); border: 1px solid color-mix(in srgb, var(--bl-primary) 25%, transparent); border-radius: 8px; padding: 10px 14px; font-size: 12.5px; line-height: 1.6; color: var(--bl-text-2); margin-bottom: 14px; }
.fe-info > span:first-child { flex-shrink: 0; display: inline-flex; margin-top: 1px; }
/* 满宽虚线「添加一行」按钮 */
.fe-add-row { width: 100%; display: flex; align-items: center; justify-content: center; padding: 9px; margin-top: 10px; background: transparent; border: 1px dashed var(--bl-border-strong); border-radius: 8px; color: var(--bl-text-2); font-size: 12.5px; cursor: pointer; transition: border-color .12s, color .12s, background .12s; }
.fe-add-row:hover { border-color: var(--bl-primary); color: var(--bl-primary); background: var(--bl-primary-soft); }
/* 关联链接配置项 */
.fe-link-item { border: 1px solid var(--bl-divider); border-radius: 8px; margin-bottom: 10px; overflow: hidden; }
.fe-link-hd { display: flex; align-items: center; gap: 8px; padding: 9px 12px; background: var(--bl-bg-2); }
.fe-link-lbl { font-size: 12.5px; color: var(--bl-text-2); flex-shrink: 0; }
.fe-link-bd { padding: 12px; display: flex; flex-direction: column; gap: 10px; }
.fe-link-bd .fe-lbl { width: 96px; }
.fe-subhd { font-size: 12.5px; font-weight: 600; color: var(--bl-text-2); margin-bottom: 6px; }
.fe-subhd .bl-muted { font-weight: 400; }
.fe-code { font-family: var(--bl-mono, monospace); font-size: 12px; line-height: 1.6; color: #d4d4d4; background: #1e1e2e; border-radius: 8px; padding: 14px 16px; white-space: pre-wrap; overflow-x: auto; margin: 0; }
.fe-repo { font-size: 12px; color: var(--bl-primary); cursor: pointer; display: inline-flex; align-items: center; gap: 4px; font-weight: 400; }
.fe-repo:hover { text-decoration: underline; }
.ate-mini-table tr.fe-mismatch td { background: color-mix(in srgb, #f53f3f 8%, transparent); }
.ate-mini-table tr.fe-mismatch .bl-input { border-color: #f53f3f; }
/* 内容配置 tab */
.fd-tabs2 { display: inline-flex; gap: 2px; padding: 3px; background: var(--bl-bg-2); border-radius: 8px; }
.fd-tab2 { padding: 5px 18px; font-size: 13px; color: var(--bl-text-2); cursor: pointer; background: transparent; border: 0; border-radius: 6px; transition: background .12s, color .12s; }
.fd-tab2:hover { color: var(--bl-text-1); }
.fd-tab2.is-on { background: var(--bl-bg-1); color: var(--bl-primary); font-weight: 600; box-shadow: 0 1px 2px rgba(0,0,0,.08); }
/* 收件人级联 */
.fe-cascade { display: flex; flex-wrap: wrap; gap: 8px; flex: 1; }
/* 完整编辑态 表单行 (label 左 + 控件) */
.fe-row { display: flex; align-items: center; gap: 8px; }
.fe-row-top { align-items: flex-start; }
.fe-row-wrap { flex-wrap: wrap; }
.fe-lbl { flex-shrink: 0; width: 84px; font-size: 12.5px; color: var(--bl-text-2); }
.fe-lbl-inline { width: auto; margin-left: 14px; }
.fe-hint { font-size: 11.5px; color: var(--bl-text-3); margin-top: 8px; padding-left: 84px; line-height: 1.5; }
/* 权限范围 单选 */
.fe-radio { display: inline-flex; align-items: center; gap: 6px; cursor: pointer; }
.fe-radio-lbl { font-size: 13px; color: var(--bl-text-1); }
.fe-radio-desc { font-size: 11.5px; color: var(--bl-text-3); margin-top: 2px; line-height: 1.5; }
/* Webhook 类型选择卡 */
.wh-type-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.wh-type-card { display: flex; align-items: center; gap: 10px; padding: 16px 18px; background: var(--bl-bg-1); border: 1.5px solid var(--bl-border); border-radius: 10px; cursor: pointer; transition: border-color .12s, background .12s; }
.wh-type-card:hover { border-color: var(--bl-primary); }
.wh-type-card.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.wh-type-ic { width: 26px; height: 26px; border-radius: 7px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.wh-type-lbl { font-size: 14px; font-weight: 600; color: var(--bl-text-1); }
.wh-type-chk { margin-left: auto; color: var(--bl-primary); display: inline-flex; }
/* 执行说明 */
.fe-hints { display: flex; flex-direction: column; gap: 6px; font-size: 12.5px; color: var(--bl-text-2); line-height: 1.6; }
.fe-hints > div { color: var(--bl-success, #00b42a); }
/* 通用设置 开关同样改小 */
.fd-tri-hd .adw-showsw { width: 32px; height: 18px; border-radius: 9px; }
.fd-tri-hd .adw-showsw-dot { width: 14px; height: 14px; }
.fd-tri-hd .adw-showsw.is-on .adw-showsw-dot { left: 16px; }
.rl-card.is-off .rl-preview, .rl-card.is-off .rl-body { opacity: .5; }
.rl-hd { display: flex; align-items: center; gap: 8px; padding: 10px 12px; cursor: pointer; user-select: none; }
.rl-hd:hover { background: var(--bl-bg-hover); }
.rl-grip { color: var(--bl-text-3); cursor: grab; display: inline-flex; }
.rl-ic { width: 22px; height: 22px; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.rl-kind { font-size: 12px; font-weight: 600; color: var(--bl-text-2); background: var(--bl-bg-2); padding: 2px 8px; border-radius: 5px; flex-shrink: 0; }
.rl-name { border: 0; background: transparent; font-size: 13.5px; font-weight: 600; color: var(--bl-text-1); outline: none; min-width: 120px; max-width: 280px; }
.rl-name:focus { border-bottom: 1px solid var(--bl-primary); }
.rl-chev { color: var(--bl-text-3); display: inline-flex; }
.rl-preview { padding: 0 12px 10px 42px; font-size: 12px; color: var(--bl-text-3); }
.rl-body { padding: 12px 14px 14px; border-top: 1px solid var(--bl-divider); background: var(--bl-bg-1); display: flex; flex-direction: column; gap: 10px; }
.rl-row { display: flex; align-items: center; gap: 8px; }
.rl-row-top { align-items: flex-start; }
.rl-lbl { flex-shrink: 0; width: 88px; font-size: 12.5px; color: var(--bl-text-2); }
.rl-row .bl-input { flex: 1; }
.rl-row-top .bl-input { flex: 1; }
.rl-sub { font-size: 12.5px; font-weight: 600; color: var(--bl-text-2); display: flex; align-items: center; gap: 8px; margin-top: 2px; }
.rl-code { font-family: var(--bl-mono, monospace); font-size: 11.5px; color: var(--bl-text-3); background: var(--bl-bg-1); border: 1px solid var(--bl-divider); border-radius: 6px; padding: 10px 12px; white-space: pre-wrap; }

/* 添加新规则 + 类型选择器 */
/* 添加新规则: 整行居中虚线按钮 */
.rl-add-btn { width: 100%; display: flex; align-items: center; justify-content: center; gap: 2px; padding: 11px; margin-top: 4px; background: var(--bl-bg-1); border: 1px dashed var(--bl-border-strong); border-radius: 10px; color: var(--bl-text-2); font-size: 13px; cursor: pointer; transition: border-color .12s, color .12s, background .12s; }
.rl-add-btn:hover { border-color: var(--bl-primary); color: var(--bl-primary); background: var(--bl-primary-soft); }

/* 添加规则 弹框 */
.rlp-modal { width: 680px; }
.rlp-grp { font-size: 12px; font-weight: 600; color: var(--bl-text-2); margin-bottom: -2px; }
.rlp-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.rlp-card { display: flex; align-items: flex-start; gap: 10px; padding: 12px 14px; text-align: left; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 10px; cursor: pointer; transition: border-color .12s, background .12s, box-shadow .12s; }
.rlp-card:hover { border-color: var(--bl-primary); background: var(--bl-primary-soft); box-shadow: 0 2px 8px rgba(0,0,0,.06); }
.rlp-card.is-disabled { opacity: .45; cursor: not-allowed; }
.rlp-card.is-disabled:hover { border-color: var(--bl-border); background: var(--bl-bg-1); box-shadow: none; }
.rlp-card-ic { width: 30px; height: 30px; border-radius: 8px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.rlp-card-txt { min-width: 0; }
.rlp-card-lbl { font-size: 13.5px; font-weight: 600; color: var(--bl-text-1); }
.rlp-card-desc { font-size: 11.5px; color: var(--bl-text-3); margin-top: 3px; line-height: 1.4; }
.rlp-warn { line-height: 1.6; }

/* 入参映射弹窗 */
.rlm-mask { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: center; justify-content: center; z-index: 1300; }
.rlm-modal { width: 640px; max-width: 92vw; max-height: 82vh; background: var(--bl-bg-1); border-radius: 12px; box-shadow: 0 16px 48px rgba(0,0,0,.3); display: flex; flex-direction: column; overflow: hidden; }
.rlm-hd { display: flex; align-items: center; padding: 14px 16px; font-size: 14px; font-weight: 600; border-bottom: 1px solid var(--bl-divider); }
.rlm-body { padding: 16px; overflow-y: auto; display: flex; flex-direction: column; gap: 12px; }
.rlm-ft { padding: 12px 16px; border-top: 1px solid var(--bl-divider); display: flex; justify-content: flex-end; }
</style>
