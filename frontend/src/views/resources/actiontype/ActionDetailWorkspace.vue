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
                <template v-for="g in ruleNavGroups" :key="g.label">
                  <div class="adw-tree-sec" :class="{ 'is-fold': isNavFold('rule:' + g.label) }" @click="toggleNavFold('rule:' + g.label)">
                    <span class="bl-truncate">{{ g.label }}</span>
                    <span class="adw-tree-sec-n">{{ g.items.length }}</span>
                    <span class="adw-tree-sec-chev" v-html="BL.icon('chevronDown', 12)"></span>
                  </div>
                  <template v-if="!isNavFold('rule:' + g.label)">
                    <div v-for="x in g.items" :key="x.r._k"
                         :class="['adw-tree-field', activeMenu === 'rules' && ruleSelKey === x.r._k && 'is-on', x.r.status === 0 && 'is-off']"
                         :title="ruleNavLabel(x.r)" @click="openRuleFromNav(x.i)">
                      <span class="adw-tree-dt" :style="{ background: kindMeta(x.r.kind).color }" v-html="BL.icon(kindMeta(x.r.kind).icon, 11, '#fff')"></span>
                      <span class="bl-truncate adw-tree-fname">{{ ruleNavLabel(x.r) }}</span>
                    </div>
                  </template>
                </template>
                <div v-if="!rules.length" class="adw-tree-empty">暂无规则</div>
              </div>
              <!-- 表单结构树: 可独立收展 (不依赖当前菜单) -->
              <div v-if="m.k === 'form' && formNavOpen" class="adw-tree">
                <template v-for="sec in sections" :key="sec">
                  <div class="adw-tree-sec" :class="{ 'is-fold': isNavFold('form:' + sec) }" @click="toggleNavFold('form:' + sec)">
                    <span class="bl-truncate">{{ sec }}</span>
                    <span class="adw-tree-sec-n">{{ paramsOfSection(sec).length }}</span>
                    <span class="adw-tree-sec-chev" v-html="BL.icon('chevronDown', 12)"></span>
                  </div>
                  <template v-if="!isNavFold('form:' + sec)">
                    <div v-for="x in paramsOfSection(sec)" :key="x.i"
                         :class="['adw-tree-field', activeMenu === 'form' && formView === 'detail' && selIdx === x.i && 'is-on']" @click="openParam(x.i)">
                      <span class="adw-tree-dt" :style="{ background: dtMeta(x.p.param_type).color }" v-html="BL.icon(dtMeta(x.p.param_type).icon, 11, '#fff')"></span>
                      <span class="bl-truncate adw-tree-fname">{{ x.p.param_name || x.p.param_code || '未命名' }}</span>
                      <span v-if="x.p.visible === 0" class="adw-tree-disp" title="表单中隐藏" v-html="BL.icon('eyeOff', 12)"></span>
                      <span v-else class="adw-tree-disp" :title="displayMeta(x.p.display_type).label" v-html="BL.icon(displayMeta(x.p.display_type).icon, 12)"></span>
                    </div>
                  </template>
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
                <label class="adw-fld"><span class="adw-lbl">API 编码</span><input class="bl-input bl-mono" :value="form.api_name" disabled /></label>
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
          <div v-else-if="activeMenu === 'rules'" class="adw-page">
            <div class="adw-page-hd"><div class="adw-page-title">规则 <span class="bl-muted" style="font-size:11.5px">(按从上至下顺序生效,后置规则覆盖前置)</span></div><button class="bl-btn bl-btn-sm bl-btn-primary" @click="onSave" :disabled="saving">保存</button></div>
            <div v-if="ruleConflicts.length" class="rl-conflict">
              <div class="rl-conflict-hd"><span v-html="BL.icon('lock', 13)"></span><span>检测到 {{ ruleConflicts.length }} 处无效规则组合,保存前需修正:</span></div>
              <div v-for="(c, ci) in ruleConflicts" :key="ci" class="rl-conflict-item">· {{ c }}</div>
            </div>

            <div v-for="(rule, ri) in rules" :key="rule._k" class="rl-card"
                 draggable="true" @dragstart="onRuleDragStart(ri, $event)" @dragover.prevent @drop="onRuleDrop(ri)" @dragend="rDragIdx = null"
                 :class="{ 'is-dragging': rDragIdx === ri, 'is-off': rule.status === 0 }">
              <div class="rl-hd" @click="toggleRuleCard(rule)">
                <span class="rl-grip" @mousedown.stop v-html="BL.icon('grip', 13)"></span>
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
                  <RuleObjectEditor :rule="rule" :class-options="objClassOptions" :pk-options="objPkOptionsOf(rule)"
                                    :prop-options="rulePropOptionsOf(rule)" :class-name="ruleClassNameOf(rule)"
                                    :form-param-options="formParamOptions" :object-param-options="objectParamOptions"
                                    :link-type-options="linkTypeOptions" :link-card-label="linkCardLabel"
                                    :main-object-props="mainObjectProps" :show-on-detail="Number(form.show_on_detail) || 0"
                                    :prior-created="priorCreatedOf(rule)"
                                    @load-props="v => loadRuleClassProps(v)" />
                </template>
                <RuleFuncEditor v-else-if="rule.kind === 'function'" :rule="rule" :subject-summary="subjectSummary"
                                :form-param-options="formParamOptions" :object-param-options="objectParamOptions"
                                :mismatch="paramTypeMismatch" :code-preview="funcCodePreview" />
                <RuleNotifyEditor v-else-if="rule.kind === 'notification'" :rule="rule"
                                  :form-param-options="formParamOptions" :object-param-options="objectParamOptions"
                                  :code-preview="notifyCodePreview" />
                <RuleWebhookEditor v-else-if="rule.kind === 'webhook'" :rule="rule" :accent-color="kindMeta('webhook').color"
                                   :form-param-options="formParamOptions" :object-param-options="objectParamOptions"
                                   :code-preview="whInputCodePreview" />
              </div>
            </div>
            <div v-if="!rules.length" class="bl-empty" style="padding:20px;font-size:12px">暂无规则,点下方「添加新规则」</div>

            <button class="rl-add-btn" @click="rulePickerOpen = true"><span v-html="BL.icon('plus', 14)"></span><span style="margin-left:5px">添加新规则</span></button>

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
              <div v-for="sec in sections" :key="sec"
                   :class="['fd-part', secCollapsed.has(sec) && 'is-collapsed', secDrag === sec && 'is-secdrag']"
                   @dragover.prevent @drop="onPartDrop(sec)">
                <div class="fd-part-hd" draggable="true" title="点击收起/展开, 拖动调整分区顺序"
                     @dragstart="onSecDragStart(sec, $event)" @dragend="secDrag = null" @click="toggleSec(sec)">
                  <span class="fd-part-grip" v-html="BL.icon('grip', 12)"></span>
                  <span class="fd-part-name" title="重命名分区" @click.stop="renameSection(sec)">{{ sec }}</span>
                  <span class="fd-part-count">{{ paramsOfSection(sec).length }} 个参数</span>
                  <span style="flex:1"></span>
                  <button class="fd-part-op" title="重命名分区" @click.stop="renameSection(sec)" v-html="BL.icon('edit', 12)"></button>
                  <button v-if="sections.length > 1" class="fd-part-del" title="删除分区" @click.stop="removeSection(sec)" v-html="BL.icon('trash2', 12)"></button>
                  <span class="fd-part-chev" v-html="BL.icon('chevronDown', 13)"></span>
                </div>
                <div class="fd-part-body">
                  <div v-for="x in paramsOfSection(sec)" :key="x.i" class="fd-row" :class="{ 'is-dragging': fDragIdx === x.i }"
                       draggable="true" @dragstart="onFieldDragStart(x.i, $event)" @dragover.prevent @drop.stop="onFieldDrop(x.p, sec)" @dragend="fDragIdx = null"
                       @click="openParam(x.i)">
                    <span class="fd-grip" v-html="BL.icon('grip', 12)"></span>
                    <span class="fd-dt" :style="{ background: dtMeta(x.p.param_type).color }" v-html="BL.icon(dtMeta(x.p.param_type).icon, 12, '#fff')"></span>
                    <div class="fd-row-txt">
                      <div class="fd-row-name bl-truncate">{{ x.p.param_name || x.p.param_code || '未命名参数' }}</div>
                      <div v-if="isRefByRule(x.p.param_code)" class="fd-row-warn">已在规则中引用</div>
                    </div>
                    <span v-if="!x.p.param_code" class="bl-tag at-mini-tag">新建</span>
                    <span v-else-if="x.p.param_type === 'object'" class="bl-tag at-mini-tag">对象引用</span>
                    <span v-if="x.p.visible === 0" class="fd-hidden" title="表单中隐藏, 不渲染给用户"><span v-html="BL.icon('eyeOff', 12)"></span>隐藏</span>
                    <span v-if="srcTagOf(x.p)" class="fd-src-tag is-sm" :title="`取自关联对象「${srcTagOf(x.p).name}」`"><span v-html="BL.icon('box', 10, '#fff')"></span>{{ srcTagOf(x.p).name }}</span>
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
                  <label class="adw-fld"><span class="adw-lbl">清空按钮</span><span class="adw-showsw adw-showsw-sm" :class="{ 'is-on': globalConf.clear === 1 }" @click="globalConf.clear = globalConf.clear ? 0 : 1"><span class="adw-showsw-dot"></span></span></label>
                  <label class="adw-fld"><span class="adw-lbl">标签宽度</span><BlSelect v-model="globalConf.labelW" :disabled="globalConf.labelPos !== 'left'" :options="DSP_LABEL_WS" /></label>
                  <label class="adw-fld"><span class="adw-lbl">标签对齐</span><BlSelect v-model="globalConf.labelAlign" :disabled="globalConf.labelPos !== 'left'" :options="DSP_LABEL_ALIGNS" /></label>
                  <div class="adw-fld"><span class="bl-muted" style="font-size:12px">{{ globalConf.labelPos === 'left' ? (globalConf.labelW === 'auto' ? `当前自适应宽度约 ${autoLabelW()}px` : '固定宽度可保证各字段控件左边缘对齐') : '标签宽度 / 对齐仅在「标签位置 = 左侧显示」时生效' }}</span></div>
                </div>
                <div class="gd-sub">辅助提示</div>
                <div class="gd-grid">
                  <label class="adw-fld"><span class="adw-lbl">帮助文案</span><span class="adw-showsw adw-showsw-sm" :class="{ 'is-on': globalConf.helpOn === 1 }" @click="globalConf.helpOn = globalConf.helpOn ? 0 : 1"><span class="adw-showsw-dot"></span></span></label>
                  <label class="adw-fld" style="grid-column:2/-1"><span class="adw-lbl">默认文案</span><input class="bl-input" v-model="globalConf.helpText" placeholder="全局默认帮助文案" /></label>
                </div>
                <div class="gd-sub">表单级样式<span class="bl-muted" style="font-weight:400;font-size:11px;margin-left:6px">无参数级覆盖</span></div>
                <div class="gd-grid">
                  <label class="adw-fld"><span class="adw-lbl">必填标识</span><BlSelect v-model="globalConf.reqMark" :options="[{value:'prefix',label:'名称前红色星号'},{value:'suffix',label:'名称后红色星号'}]" /></label>
                  <label class="adw-fld"><span class="adw-lbl">字段间距</span><BlSelect v-model="globalConf.density" :options="[{value:'compact',label:'紧凑'},{value:'normal',label:'标准'},{value:'loose',label:'宽松'}]" /></label>
                  <label class="adw-fld"><span class="adw-lbl">分区标题</span><span class="adw-showsw adw-showsw-sm" :class="{ 'is-on': globalConf.sectionTitle === 1 }" @click="globalConf.sectionTitle = globalConf.sectionTitle ? 0 : 1"><span class="adw-showsw-dot"></span></span><span class="bl-muted" style="font-size:12px;margin-left:8px">在表单中展示分区标题</span></label>
                </div>
                <div class="gd-sub">提交交互</div>
                <div class="gd-grid">
                  <label class="adw-fld"><span class="adw-lbl">自定义提交按钮</span><span class="adw-showsw adw-showsw-sm" :class="{ 'is-on': globalConf.custom_submit === 1 }" @click="globalConf.custom_submit = globalConf.custom_submit ? 0 : 1"><span class="adw-showsw-dot"></span></span></label>
                  <label class="adw-fld"><span class="adw-lbl">自定义成功提示</span><span class="adw-showsw adw-showsw-sm" :class="{ 'is-on': globalConf.custom_success === 1 }" @click="globalConf.custom_success = globalConf.custom_success ? 0 : 1"><span class="adw-showsw-dot"></span></span></label>
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
                <!-- 头部三列: 返回 | 参数信息(上下两行) | 操作 -->
                <div class="fd-phd">
                  <button class="fd-back" title="返回表单内容" @click="backToList"><span v-html="BL.icon('chevronLeft', 13)"></span><span>返回</span></button>
                  <span class="fd-phd-sep"></span>
                  <div class="fd-phd-mid">
                    <div class="fd-phd-r1">
                      <span class="fd-dt" :style="{ background: dtMeta(selParam.param_type).color }" v-html="BL.icon(dtMeta(selParam.param_type).icon, 13, '#fff')"></span>
                      <input class="fd-pname2" v-model="selParam.param_name" :size="nameSize(selParam.param_name)" placeholder="参数名称" />
                      <span class="fd-code-tag bl-mono" :class="{ 'is-empty': !selParam.param_code }" :title="'参数编码:' + (selParam.param_code || '未设置')">{{ selParam.param_code || '未设编码' }}</span>
                      <span class="bl-tag" :style="{ background:`color-mix(in srgb, ${dtMeta(selParam.param_type).color} 12%, transparent)`, color:dtMeta(selParam.param_type).color }">{{ dtMeta(selParam.param_type).label }}</span>
                      <span v-if="selParam.is_required" class="fd-req-tag">必填</span>
                      <span v-if="selParam.visible === 0" class="fd-hidden" title="表单中隐藏"><span v-html="BL.icon('eyeOff', 12)"></span>隐藏</span>
                      <span v-if="srcTagOf(selParam)" class="fd-src-tag" :title="`该参数取自关联对象「${srcTagOf(selParam).name}」的属性 ${selParam.property_code}`">
                        <span v-html="BL.icon('box', 11, '#fff')"></span>{{ srcTagOf(selParam).name }}
                        <i v-if="srcTagOf(selParam).api" class="bl-mono">{{ srcTagOf(selParam).api }}</i>
                      </span>
                    </div>
                    <div class="fd-phd-r2">
                      <span>{{ selParam.section || '基础参数' }}</span>
                      <span class="fd-meta-dot"></span><span>{{ displayMeta(selParam.display_type).label }}</span>
                      <span class="fd-meta-dot"></span><span>{{ dspLabel('width', dspOf(selParam).width) }}</span>
                      <span class="fd-meta-dot"></span><span>{{ dspLabel('labelPos', dspOf(selParam).labelPos) }}</span>
                      <template v-if="selParam.description"><span class="fd-meta-dot"></span><span class="bl-truncate">{{ selParam.description }}</span></template>
                    </div>
                  </div>
                  <div class="fd-phd-act">
                    <button class="bl-btn bl-btn-sm bl-btn-primary" @click="onSave" :disabled="saving">保存</button>
                    <button class="bl-btn bl-btn-text bl-btn-icon at-del-op" title="删除参数" @click="removeParamAt(selIdx)" v-html="BL.icon('trash2', 13)"></button>
                  </div>
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
                      <label class="adw-fld"><span class="adw-lbl">数据类型</span><BlSelect v-model="selParam.param_type" :options="PARAM_TYPE_OPTS" @change="onParamTypeChange" /></label>
                      <label class="adw-fld"><span class="adw-lbl">前端组件</span>
                        <IconGridSelect v-model="selParam.display_type" :options="DISPLAY_TYPE_OPTS" :columns="2" /></label>
                    </div>
                  </div>
                  <div class="adw-card"><div class="adw-card-hd adw-card-hd-flex"><span>通用设置</span><span class="bl-muted" style="font-size:12px;font-weight:400">控制参数在表单中的展示、编辑与校验行为</span></div>
                    <div class="fd-triple">
                      <div class="fd-tri">
                        <div class="fd-tri-hd"><span>可见</span><span v-if="ovCountOf('visible')" class="bl-tag bl-tag-primary at-mini-tag">{{ ovCountOf('visible') }} 条覆盖</span><span style="flex:1"></span><span class="adw-showsw" :class="{'is-on':selParam.visible===1}" @click="selParam.visible = selParam.visible?0:1"><span class="adw-showsw-dot"></span></span></div>
                        <a class="fd-ovr" @click="addOverrideFor('visible')">+ 添加覆盖规则</a>
                      </div>
                      <div class="fd-tri">
                        <div class="fd-tri-hd"><span>禁用</span><span v-if="ovCountOf('disabled')" class="bl-tag bl-tag-primary at-mini-tag">{{ ovCountOf('disabled') }} 条覆盖</span><span style="flex:1"></span><span class="adw-showsw" :class="{'is-on':selParam.disabled===1}" @click="selParam.disabled = selParam.disabled?0:1"><span class="adw-showsw-dot"></span></span></div>
                        <a class="fd-ovr" @click="addOverrideFor('disabled')">+ 添加覆盖规则</a>
                      </div>
                      <div class="fd-tri">
                        <div class="fd-tri-hd"><span>必填</span><span v-if="ovCountOf('required')" class="bl-tag bl-tag-primary at-mini-tag">{{ ovCountOf('required') }} 条覆盖</span><span style="flex:1"></span><span class="adw-showsw" :class="{'is-on':selParam.is_required===1}" @click="selParam.is_required = selParam.is_required?0:1"><span class="adw-showsw-dot"></span></span></div>
                        <a class="fd-ovr" @click="addOverrideFor('required')">+ 添加覆盖规则</a>
                      </div>
                    </div>
                  </div>
                  <div class="adw-card"><div class="adw-card-hd">约束设置</div>
                    <label class="adw-sw" style="margin-bottom:12px"><input type="checkbox" v-model="selParam.allow_multi" :true-value="1" :false-value="0" /> 允许多值 <span class="bl-muted" style="font-size:11px;margin-left:4px">({{ selParam.param_type === 'object' ? '用户可选择多个对象引用' : '用户可选择多个字符串值' }})</span></label>
                    <!-- 对象引用参数: 选择类型锁定为「对象下拉选择」+ 筛选规则配置 (文档 3.3) -->
                    <template v-if="selParam.param_type === 'object'">
                      <div class="fd-locked"><span v-html="BL.icon('box', 13, '#165DFF')"></span><span>对象下拉选择</span><span style="flex:1"></span><span v-html="BL.icon('check', 13, '#165DFF')"></span></div>
                      <ObjectSetFilter variant="object" v-bind="objsetBind" />
                    </template>
                    <template v-else>
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
                        <div v-for="(o, oi) in selParam.options" :key="oi" class="fd-opt"><span class="fd-grip" v-html="BL.icon('grip', 11)"></span><input class="bl-input bl-input-sm bl-mono" v-model="o.value" placeholder="参数值" /><input class="bl-input bl-input-sm" v-model="o.label" placeholder="显示名称" /><button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" @click="selParam.options.splice(oi,1)" v-html="BL.icon('x', 11)"></button></div>
                        <button class="bl-btn bl-btn-text bl-btn-sm" @click="addOption(selParam)"><span v-html="BL.icon('plus', 11)"></span><span style="margin-left:3px">添加选项</span></button>
                      </template>
                      <ObjectSetFilter v-else variant="multi" v-bind="objsetBind" />
                      <label class="adw-sw" style="margin-top:10px;display:flex"><input type="checkbox" v-model="selParam.allow_other" :true-value="1" :false-value="0" /> 允许其他值(用户可自定义输入)</label>
                    </div>
                    <div v-else class="fd-mode-body"><div class="bl-muted" style="font-size:12px">「{{ selParam.input_mode === 'user' ? '用户' : '用户组' }}」模式:下拉从系统人员/用户组获取,无需额外配置。</div></div>
                    </template>
                  </div>
                  <div class="adw-card"><div class="adw-card-hd">默认值</div>
                    <label class="adw-sw" style="margin-bottom:10px"><input type="checkbox" v-model="selParam.default_enabled" :true-value="1" :false-value="0" /> 启用默认值 <span class="bl-muted" style="font-size:11px;margin-left:4px">(用户可自行修改)</span></label>
                    <template v-if="selParam.default_enabled">
                      <div class="fd-src"><label class="fd-src-opt" :class="{'is-on':selParam.default_type==='objectprop'}" @click="selParam.default_type='objectprop'">对象参数属性</label><label class="fd-src-opt" :class="{'is-on':selParam.default_type==='static'}" @click="selParam.default_type='static'">设为静态值</label></div>
                      <input v-if="selParam.default_type === 'static'" class="bl-input" v-model="selParam.default_value" placeholder="固定默认值" />
                      <!-- 对象参数属性: 两级级联 (选表单内对象参数 → 选其属性字段), 文档 2.3.3 类型一 -->
                      <div v-else class="fd-cascade">
                        <div class="fd-cascade-row">
                          <span class="fd-cascade-lbl">对象参数</span>
                          <BlSelect v-model="selParam.default_obj_param" :options="objectParamOptions" clearable
                                    :placeholder="objectParamOptions.length ? '选择表单内对象参数' : '暂无对象引用参数,请先在表单添加'"
                                    @change="selParam.default_obj_prop = ''" style="flex:1" />
                        </div>
                        <div class="fd-cascade-row">
                          <span class="fd-cascade-lbl">属性字段</span>
                          <BlSelect v-model="selParam.default_obj_prop" :options="defObjPropOptions" clearable
                                    :placeholder="defObjPlaceholder" style="flex:1" />
                          <span v-if="defObjProp" :class="['bl-tag', defObjProp.status === 0 ? 'bl-tag-warning' : 'bl-tag-success']">{{ defObjProp.status === 0 ? '停用' : '启用' }}</span>
                        </div>
                        <div :class="['fd-cascade-hint', defObjFallback && 'is-warn']">{{ defObjHint }}</div>
                      </div>
                    </template>
                  </div>
                </section>

                <!-- 显示 -->
                <section v-show="detailTab === 'display'" class="fd-sec">
                  <!-- 占位文字: 只有会渲染输入区的组件才有意义, 其余组件不显示该配置 -->
                  <div v-if="hasPlaceholder(selParam)" class="adw-card"><div class="adw-card-hd">占位文字</div>
                    <div class="fd-ph-row">
                      <input class="bl-input" v-model="selParam.placeholder" :placeholder="previewPlaceholder(selParam)" />
                      <button v-if="selParam.placeholder" class="bl-btn bl-btn-text bl-btn-sm" @click="selParam.placeholder = ''">恢复默认</button>
                    </div>
                    <div class="fd-ph-tip">显示在<b>{{ displayMeta(selParam.display_type).label }}</b>内的灰色提示语,用户开始输入后消失。留空则用默认文案「{{ previewPlaceholder(selParam) }}」。</div>
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
                      <span class="fd-inh-ctl"><span class="adw-showsw adw-showsw-sm" :class="{ 'is-on': dspOf(selParam).clear === 1, 'is-lock': isInherit(selParam,'clear') }" @click="!isInherit(selParam,'clear') && setDsp(selParam,'clear', dspOf(selParam).clear ? 0 : 1)"><span class="adw-showsw-dot"></span></span></span>
                      <label class="fd-inh-ck"><input type="checkbox" :checked="isInherit(selParam,'clear')" @change="toggleInherit(selParam,'clear')" /> 跟随全局</label>
                      <span v-if="!isInherit(selParam,'clear')" class="bl-tag bl-tag-primary at-mini-tag">已覆盖</span>
                    </div>
                  </div>

                  <div class="adw-card"><div class="adw-card-hd">辅助提示</div>
                    <div class="fd-inh-row">
                      <span class="fd-inh-lbl">帮助文案</span>
                      <span class="fd-inh-ctl"><span class="adw-showsw adw-showsw-sm" :class="{ 'is-on': dspOf(selParam).helpOn === 1, 'is-lock': isInherit(selParam,'helpOn') }" @click="!isInherit(selParam,'helpOn') && setDsp(selParam,'helpOn', dspOf(selParam).helpOn ? 0 : 1)"><span class="adw-showsw-dot"></span></span></span>
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
                    <!-- 规则说明区 (文档 1.4.4.3 优先级匹配) -->
                    <div class="ov-note">多个覆盖块按从上到下的优先级依次匹配,<b>仅第一个条件命中的块生效</b>,后续块直接忽略、不叠加;所有块均不命中时,沿用「值」页的全局默认配置。</div>

                    <!-- 覆盖块列表: 就地编辑, 不再弹框 -->
                    <div v-for="(ov, ovi) in selParam.overrides" :key="ov._k || ovi" class="ov-card">
                      <div class="ov-card-hd" @click="toggleOvFold(ov)">
                        <span class="ov-prio">优先级 {{ ovi + 1 }}</span>
                        <span class="bl-truncate ov-title">{{ blockTitle(ov) }}</span>
                        <span style="flex:1"></span>
                        <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="上移" :disabled="ovi === 0" @click.stop="moveOverride(ovi, -1)" v-html="BL.icon('chevronUp', 12)"></button>
                        <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="下移" :disabled="ovi === selParam.overrides.length - 1" @click.stop="moveOverride(ovi, 1)" v-html="BL.icon('chevronDown', 12)"></button>
                        <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon at-del-op" title="删除" @click.stop="removeOverride(ovi)" v-html="BL.icon('trash2', 12)"></button>
                        <span class="ov-chev" :class="{ 'is-fold': ovFold.has(ov._k) }" v-html="BL.icon('chevronDown', 13)"></span>
                      </div>
                      <!-- 折叠态: 只看摘要 -->
                      <div v-if="ovFold.has(ov._k)" class="ov-card-bd">
                        <div class="ov-sum"><span class="ov-sum-tag ov-sum-if">If</span><span>{{ ifSummary(ov.cond) }}</span></div>
                        <div class="ov-sum"><span class="ov-sum-tag ov-sum-then">Then</span><span>{{ thenSummary(ov.actions) }}</span></div>
                      </div>
                      <!-- 展开态: If 条件 + Then 动作 直接编辑 -->
                      <div v-else class="ov-edit">
                        <div class="ov-tag ov-tag-if">If</div>
                        <ConditionGroup :node="ov.cond" :depth="0" :param-fields="ovParamFields" :user-fields="OV_USER_FIELDS"
                                        :subjects="['user', 'param']" :logics="['all', 'any', 'none']"
                                        :operators="OV_OP_KEYS" :value-options-of="ovValueOptions" show-ready />
                        <div class="ov-tag ov-tag-then">Then</div>
                        <div class="ov-acts">
                          <div v-for="(a, ai) in ov.actions" :key="a._k" :class="['ov-act', isRedundant(a, ovDefaults) && 'is-redundant']">
                            <span class="ov-act-lbl">设为</span>
                            <BlSelect v-model="a.type" :options="ovActionOptsOf(a)" size="sm" style="width:112px" @change="onOvActTypeChange(a)" />
                            <template v-if="ovBoolAction(a.type)">
                              <span class="adw-showsw adw-showsw-sm" :class="{ 'is-on': Number(a.value) === 1 }"
                                    @click="a.value = Number(a.value) ? 0 : 1"><span class="adw-showsw-dot"></span></span>
                              <span class="bl-muted" style="font-size:12px">{{ Number(a.value) ? '是' : '否' }}</span>
                            </template>
                            <input v-else class="bl-input bl-input-sm" style="flex:1;min-width:0" v-model="a.value"
                                   :placeholder="a.type === 'default' ? '默认值内容' : '约束说明 / 正则'" />
                            <span v-if="isRedundant(a, ovDefaults)" class="ov-redundant" title="与该字段的全局默认配置一致, 此覆盖不产生任何变化">与默认值相同,配置冗余</span>
                            <span style="flex:1"></span>
                            <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除覆盖动作" @click="ov.actions.splice(ai,1)" v-html="BL.icon('x', 11)"></button>
                          </div>
                          <div v-if="!ov.actions.length" class="ov-empty-line">暂无覆盖动作,点下方添加</div>
                          <a class="ov-add-act" @click="addOvAction(ov)">+ 添加覆盖</a>
                        </div>
                      </div>
                    </div>

                    <button class="fe-add-row" @click="addOverride('')"><span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">添加条件覆盖</span></button>
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
                    <label class="adw-fld" style="margin-top:12px"><span class="adw-lbl">参数描述</span>
                      <textarea class="bl-textarea" v-model="selParam.description" rows="3" placeholder="说明该参数的业务含义与填写要求(仅配置人员可见,不展示给终端用户)"></textarea></label>
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
            <div class="adw-page-hd"><div class="adw-page-title">安全与提交准入</div><button class="bl-btn bl-btn-sm bl-btn-primary" @click="onSave" :disabled="saving">保存</button></div>
            <label class="adw-sw" style="margin-bottom:12px"><input type="checkbox" v-model="submit.enabled" :true-value="1" :false-value="0" /> 启用提交标准校验</label>
            <template v-if="submit.enabled">
              <div class="ate-grp-hd" style="margin-bottom:8px"><div class="ate-grp-title">执行规则</div></div>
              <ConditionGroup :node="submitTree" :depth="0" :object-fields="editorObjectFields" :param-fields="editorParamFields"
                              :subjects="['object', 'user', 'usergroup']" :subject-labels="{ object: subjectName }" />
              <label class="adw-fld" style="margin-top:14px"><span class="adw-lbl">校验失败提示</span><textarea class="bl-textarea" v-model="submit.error_message" rows="2" placeholder="不满足条件时的提示"></textarea></label>
            </template>
          </div>
        </main>

        <!-- ===== 右侧预览 384px (仅表单菜单) ===== -->
        <aside v-if="activeMenu === 'form'" class="adw-preview" :style="{ flex: `0 0 ${pvWidth}px` }">
          <div class="adw-pv-drag" :class="pvResizing && 'is-resizing'" @mousedown="onPvDragStart" title="拖动调整预览宽度"></div>
          <div class="adw-preview-hd adw-card-hd-flex"><span>表单预览</span>
            <span class="adw-pv-mode" :class="pvGrid && 'is-real'">{{ pvGrid ? '真实栅格' : '单列示意' }}</span>
            <span style="flex:1"></span>
            <button class="bl-btn bl-btn-text bl-btn-sm" @click="togglePvWide">{{ pvWide ? '收窄' : '加宽' }}</button></div>
          <div class="adw-preview-body">
            <div :class="['adw-preview-form', 'is-' + globalConf.density]">
              <div class="adw-preview-title">{{ form.rdfs_label || '动作表单' }}</div>
              <template v-for="g in previewGroups" :key="g.sec">
              <div v-if="globalConf.sectionTitle === 1" class="adw-pv-sec">{{ g.sec }}</div>
              <div class="adw-pv-grid">
              <div v-for="x in g.items" :key="x.i"
                   :class="['adw-preview-fld', selIdx === x.i && formView === 'detail' && 'is-sel', dspOf(x.p).labelPos === 'left' && 'is-side']"
                   :style="pvGrid ? { width: widthPct(x.p) } : null"
                   @click="openParam(x.i)">
                <div v-if="dspOf(x.p).labelPos !== 'none'" class="adw-preview-lbl"
                     :style="dspOf(x.p).labelPos === 'left' ? { flex:`0 0 ${labelWpx(x.p)}px`, textAlign: globalConf.labelAlign } : null">
                  <i v-if="x.p.is_required && globalConf.reqMark === 'prefix'" class="adw-pv-req">*</i>{{ x.p.param_name || x.p.param_code }}<i v-if="x.p.is_required && globalConf.reqMark === 'suffix'" class="adw-pv-req is-suffix">*</i>
                </div>
                <!-- 预览一律渲染真实控件, 未定位的字段只是关掉交互(点它先定位参数, 再点才可试填) -->
                <div class="adw-pv-ctl" :class="{ 'is-static': !pvLive(x.i) }" @click.stop="pvLive(x.i) || openParam(x.i)">
                  <div v-if="x.p.display_type === 'switch'" class="adw-pv-switch">
                    <span class="adw-showsw" :class="{ 'is-on': pvVals[x.p.param_code] }" @click="pvVals[x.p.param_code] = !pvVals[x.p.param_code]"><span class="adw-showsw-dot"></span></span>
                  </div>
                  <textarea v-else-if="x.p.display_type === 'textarea'" class="bl-textarea" rows="2" v-model="pvVals[x.p.param_code]"
                            :disabled="x.p.disabled" :placeholder="x.p.placeholder || previewPlaceholder(x.p)"></textarea>
                  <BlSelect v-else-if="['select','user','tree'].includes(x.p.display_type)" size="sm" clearable :disabled="!!x.p.disabled"
                            :model-value="pvVals[x.p.param_code] ?? ''" @update:modelValue="v => pvVals[x.p.param_code] = v"
                            :options="pvOptList(x.p)" :placeholder="x.p.placeholder || '请选择…'" />
                  <div v-else-if="x.p.display_type === 'radio'" class="adw-pv-opts">
                    <label v-for="(o, oi) in previewOpts(x.p)" :key="oi" class="adw-pv-opt is-live" @click="pvVals[x.p.param_code] = o"><span class="adw-pv-radio" :class="{ 'is-on': pvVals[x.p.param_code] === o }"></span>{{ o }}</label>
                  </div>
                  <div v-else-if="x.p.display_type === 'checkbox'" class="adw-pv-opts">
                    <label v-for="(o, oi) in previewOpts(x.p)" :key="oi" class="adw-pv-opt is-live" @click="pvToggleOpt(x.p, o)"><span class="adw-pv-checkbox" :class="{ 'is-on': pvChecked(x.p, o) }"></span>{{ o }}</label>
                  </div>
                  <input v-else class="bl-input" :type="x.p.display_type === 'number' || x.p.param_type === 'number' ? 'number' : 'text'"
                         :disabled="x.p.disabled || x.p.display_type === 'readonly'"
                         v-model="pvVals[x.p.param_code]" :placeholder="x.p.placeholder || previewPlaceholder(x.p)" />
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

  <!-- 添加参数: 从对象属性中挑选 -->
  <PropertyPickerModal v-model:open="propPickerOpen" :sources="propSources" :used-codes="usedPropCodes" @pick="onPropsPicked" />

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

</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { BL } from '@/lib/bl.js'
import { actionTypeApi, categoryApi, resourceApi } from '@/api'
import BlSelect from '@/components/BlSelect.vue'
import IconPickerField from '@/components/IconPickerField.vue'
import ColorPickerField from '@/components/ColorPickerField.vue'
import ConditionGroup from './ConditionGroup.vue'
import ObjectSetFilter from './ObjectSetFilter.vue'
import IconGridSelect from './IconGridSelect.vue'
import RuleObjectEditor from './RuleObjectEditor.vue'
import PropertyPickerModal from './PropertyPickerModal.vue'
import RuleFuncEditor from './RuleFuncEditor.vue'
import RuleNotifyEditor from './RuleNotifyEditor.vue'
import RuleWebhookEditor from './RuleWebhookEditor.vue'
import { newMapping, WH_SUBTYPES } from './ruleModel.js'
import { normalizeOverrides, serializeOverrides, blockTitle, ifSummary, thenSummary, emptyBlock,
  OV_ACTIONS, OV_ACTION_LABEL, ovActionMeta, OV_USER_FIELDS, OV_OPERATORS, isRedundant, ovUid } from './overrideModel.js'
import { VALUE_SOURCE_OPTS, FUNC_PTYPE_OPTS } from './funcParamModel.js'
import { normalizeOp } from './conditionModel.js'
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
  if (pvWidth.value > pvMaxPx()) { pvWidth.value = pvMaxPx(); savePvWidth() }
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
onBeforeUnmount(() => { window.removeEventListener('mousemove', onDragMove); window.removeEventListener('mouseup', onDragEnd); window.removeEventListener('mousemove', onPvDragMove); window.removeEventListener('mouseup', onPvDragEnd); scrollEl.value?.removeEventListener('scroll', onDetailScroll) })

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
/* 表单设计器: 数据类型 / 显示组件 / 录入模式 */
/* 图标取「类型本身的写法」而非动作: 字母 A=文本 / #=数值 / 勾=布尔 / 立方=对象 / 日历=日期 / 带勾方框=枚举 */
const DATA_TYPE_META = { string:{icon:'textType',color:'#2563eb',label:'字符串'}, number:{icon:'hash',color:'#1f2937',label:'数值'}, boolean:{icon:'check',color:'#10b981',label:'布尔'}, object:{icon:'cube',color:'#8b5cf6',label:'对象引用'}, date:{icon:'calendar',color:'#0891b2',label:'日期'}, enum:{icon:'checkSquare',color:'#dc2626',label:'枚举'} }
/* 下拉标签与头部类型胶囊同源 (都取 DATA_TYPE_META.label), 避免一处中文一处英文 */
const PARAM_TYPE_OPTS = ['string','number','boolean','enum','object','date'].map(t => ({ value: t, label: `${DATA_TYPE_META[t].label} (${t})` }))
/* 图标画控件本身的形态: 单选=圆点 / 复选=方框勾, 二者此前同用一个勾无法区分 */
const DISPLAY_TYPES = [
  { v:'input', label:'单行输入框', icon:'fieldInput' }, { v:'textarea', label:'多行文本域', icon:'fieldTextarea' },
  { v:'select', label:'下拉选择', icon:'fieldSelect' }, { v:'radio', label:'单选按钮组', icon:'radioDot' },
  { v:'number', label:'数字步进器', icon:'stepperUpDown' }, { v:'switch', label:'开关', icon:'toggleSwitch' },
  { v:'user', label:'人员选择器', icon:'user' }, { v:'readonly', label:'只读文本', icon:'lock' }, { v:'checkbox', label:'复选框', icon:'checkSquare' },
]
const DISPLAY_TYPE_OPTS = DISPLAY_TYPES.map(d => ({ value: d.v, label: d.label, icon: d.icon }))
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
    section:'基础参数', visible:1, disabled:0, display_type:'input', placeholder:'', help_text:'', description:'',
    src_class_id:'', src_class_name:'', src_class_api:'',
    allow_multi:0, input_mode:'input', min_length_on:0, min_length:'', max_length_on:0, max_length:'', regex_on:0, regex:'',
    options:[], allow_other:0, option_source:'manual', objset:defaultObjset(),
    default_enabled:0, default_type:'static', default_obj_param:'', default_obj_prop:'', overrides:[], dsp:{} }
}
/* 从对象集获取选项 (文档 1.4.4.1.3) */
function defaultObjset() { return { class_id:'', set_var:'', filters:[], link_on:0, link_type_code:'', label_prop:'' } }
function normalizeParam(p) {
  const cfg = parseCfg(p.config)
  const d = defaultParam()
  return { ...d, param_code:p.param_code, param_name:p.param_name, param_type:p.param_type||'string', is_required:Number(p.is_required)||0,
    value_source:Number(cfg.value_source)||1, default_value:p.default_value||'', property_code:cfg.property_code||'',
    section:cfg.section||'基础参数', visible:cfg.visible??1, disabled:cfg.disabled??0, display_type:cfg.display_type||autoDisplay(p.param_type),
    placeholder:cfg.placeholder||'', help_text:cfg.help_text||'', description:cfg.description||'', allow_multi:cfg.allow_multi??0, input_mode:cfg.input_mode||'input',
    src_class_id:cfg.src_class_id||'', src_class_name:cfg.src_class_name||'', src_class_api:cfg.src_class_api||'',
    min_length_on:cfg.min_length_on??0, min_length:cfg.min_length??'', max_length_on:cfg.max_length_on??0, max_length:cfg.max_length??'',
    regex_on:cfg.regex_on??0, regex:cfg.regex||'', options:cfg.options||[], allow_other:cfg.allow_other??0, option_source:cfg.option_source||'manual',
    objset:{ ...defaultObjset(), ...(cfg.objset||{}), filters:(cfg.objset?.filters||[]).map(f=>({ property_code:f.property_code||'', operator:f.operator||'eq', value:f.value||'' })) },
    default_enabled:cfg.default_enabled??0, default_type:cfg.default_type||'static',
    default_obj_param:cfg.default_obj_param||'', default_obj_prop:cfg.default_obj_prop||'',
    overrides:normalizeOverrides(cfg.overrides), dsp:cfg.dsp||{} }
}
function paramConfig(p) {
  return { value_source:p.value_source, property_code:p.property_code||null, section:p.section, visible:p.visible, disabled:p.disabled,
    display_type:p.display_type, placeholder:p.placeholder, help_text:p.help_text, description:p.description||'', allow_multi:p.allow_multi, input_mode:p.input_mode,
    src_class_id:p.src_class_id||'', src_class_name:p.src_class_name||'', src_class_api:p.src_class_api||'',
    min_length_on:p.min_length_on, min_length:p.min_length, max_length_on:p.max_length_on, max_length:p.max_length, regex_on:p.regex_on, regex:p.regex,
    options:p.options, allow_other:p.allow_other, option_source:p.option_source, objset:p.objset || defaultObjset(),
    default_enabled:p.default_enabled, default_type:p.default_type, default_obj_param:p.default_obj_param || '', default_obj_prop:p.default_obj_prop || '',
    /* 自动给出的第一条若一直没填, 不落库 */
    overrides:serializeOverrides((p.overrides || []).filter(b => b.cond?.children?.length || b.actions?.length)), dsp:p.dsp || {} }
}
function autoDisplay(t) { return ({ string:'input', number:'number', boolean:'switch', enum:'select', object:'select', date:'input' })[t] || 'input' }

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
/* 根节点即该模块的列表页入口, 因此点菜单要回到列表视图而不只是切菜单 */
function onMenuClick(m) {
  activeMenu.value = m.k
  if (m.k === 'form') { formNavOpen.value = true; backToList() }
  if (m.k === 'rules') { rulesNavOpen.value = true; backToRules(true) }
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
  ruleClassPropsCache[classId] = arr.map(p => ({ code:p.api_name||p.prop_code, name:p.display_name||p.rdfs_label||p.api_name,
    status:Number(p.status ?? 1), dataType:p.data_type, required:!!p.is_required }))
}
/* 每条规则各自的对象类 → 各自的属性候选 (多条规则可指向不同对象类) */
function ruleClassIdOf(r) { return r?.obj_class_id || form.object_class_id || '' }
function ruleClassNameOf(r) { return classOptions.value.find(c => c.id === ruleClassIdOf(r))?.cn || '' }
function rulePropOptionsOf(r) {
  const id = ruleClassIdOf(r)
  const arr = ruleClassPropsCache[id] || (id === form.object_class_id ? classProps.value : [])
  return arr.map(p => ({ value:p.code, label:`${p.name} (${p.code})`, dataType:p.dataType, required:!!p.required }))
}
/* 排在该规则之前的「创建对象」规则 — 它们的产物可被后面的规则引用 (value_source=7) */
function priorCreatedOf(rule) {
  const idx = rules.value.indexOf(rule)
  return rules.value.slice(0, idx < 0 ? 0 : idx)
    .filter(r => r.kind === 'create_object' && (r.obj_class_id || form.object_class_id))
    .map(r => {
      const id = r.obj_class_id || form.object_class_id
      const cn = classOptions.value.find(c => c.id === id)?.cn || '对象'
      return { classId: id, label: `${cn} (由「${r.rule_name || '创建对象'}」创建)` }
    })
}
/* 主对象 = 动作绑定的对象, 其属性可作为规则赋值来源 (value_source=6) */
const mainObjectProps = computed(() => classProps.value.map(p => ({ value:p.code, label:`${p.name} (${p.code})` })))
function objPkOptionsOf(r) { return [{ value:'', label:'系统自动生成' }, ...rulePropOptionsOf(r)] }
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
/* 条件左栏第一项显示所选对象类 / 链接类型本身的名字, 而不是笼统的「对象」 */
const subjectName = computed(() => {
  if (form.m_type === 1) return classOptions.value.find(x => x.id === form.object_class_id)?.cn || '对象'
  if (form.m_type === 2) { const l = (props.allLinkTypes||[]).find(x => x.id === form.link_type_id); return l ? (l.rdfs_label||l.link_type_id) : '链接' }
  return '对象'
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
/* 预览试填: 值只存在内存, 不参与保存; 仅当前定位的字段可交互, 避免误触改坏别的字段 */
const pvVals = reactive({})
function pvLive(i) { return selIdx.value === i && formView.value === 'detail' }
function pvOptList(p) { return previewOpts(p).map(o => ({ value: o, label: o })) }
function pvToggleOpt(p, o) {
  const cur = Array.isArray(pvVals[p.param_code]) ? pvVals[p.param_code] : []
  pvVals[p.param_code] = cur.includes(o) ? cur.filter(v => v !== o) : [...cur, o]
}
function pvChecked(p, o) { const v = pvVals[p.param_code]; return Array.isArray(v) && v.includes(o) }
function previewOpts(p) {
  if (p.option_source === 'objectset') {
    const cn = classOptions.value.find(c => c.id === p.objset?.class_id)?.cn
    return [cn ? `${cn} 实例(运行时加载)` : '(从对象集动态获取)']
  }
  const o = (p.options || []).map(x => x.label || x.value).filter(Boolean); return o.length ? o : ['选项一', '选项二']
}

/* 条件树 ↔ 扁平节点 */
function buildSubmitTree(nodes) {
  submitTree.logic = 'all'; submitTree.children = []
  if (!Array.isArray(nodes) || !nodes.length) return
  const root = nodes.find(n => !n.parent_id); if (!root) return
  const childrenOf = (pid) => nodes.filter(n => n.parent_id === pid).sort((a,b)=>(a.sort||0)-(b.sort||0)).map(n => {
    if (n.node_type === 'group') return { _k:'ek-'+(etk++), type:'group', logic:n.logic_op||'all', children:childrenOf(n.id) }
    const parts = String(n.left_code||'').split(':'); const subj = parts[0]==='user'?'user':parts[0]==='param'?'param':'object'
    return { _k:'ek-'+(etk++), type:'cond', subject:subj, field:parts[1]||'', operator:normalizeOp(n.operator)||'eq', value:n.right_value||'' }
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
  activeMenu.value = 'overview'; editMode.value = false; formSel.value = 0
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
/* 函数规则 完整编辑态 */
/* 左导航规则树: 按 编辑类(ruleType=1) / 副作用(ruleType=2) 分组, 组内保持规则生效顺序 */
const RULE_NAV_GROUPS = [{ ruleType: 1, label: '编辑类' }, { ruleType: 2, label: '副作用' }]
const ruleSelKey = ref(null)
const ruleNavGroups = computed(() => RULE_NAV_GROUPS
  .map(g => ({ label: g.label, items: rules.value.map((r, i) => ({ r, i })).filter(x => kindMeta(x.r.kind).ruleType === g.ruleType) }))
  .filter(g => g.items.length))
function ruleNavLabel(r) { const m = kindMeta(r.kind); return r.rule_name ? `${m.label}: ${r.rule_name}` : m.label }
/* 点下级菜单 = 回列表并展开那条规则的卡片 (所有规则都已改为卡片内就地编辑) */
function openRuleFromNav(i) {
  const rule = rules.value[i]
  if (!rule) return
  activeMenu.value = 'rules'
  ruleSelKey.value = rule._k
  rules.value.forEach(r => { r._collapsed = r !== rule })
  loadRuleClassProps(ruleClassIdOf(rule))
}
function whInputCodePreview(r) {
  return `import { Function, UserFacingError } from "@foundry/functions-api"\nimport { Company } from "@foundry/ontology-api"\n\n// 定义一个接口, 用于表达 Webhook 输入约束结构\nexport interface MyWebhookInput {\n  name: string;      // 公司名称\n  industry: string;  // 所属行业\n  country: string;   // 所在国家\n}\n\n// 定义一个类, 包含承接 Webhook 输入的函数\nexport class MyWebhookFunctions {\n  @Function()\n  public ${r.wh_input_func || 'returnWebhookInput'}(company: Company): MyWebhookInput {\n    if (!company.name || !company.industry || !company.country) {\n      throw new UserFacingError("");\n    }\n    return { /* MyWebhookInput 实例 */ };\n  }\n}`
}
function backToRules(fromNav) { activeMenu.value = 'rules'; if (fromNav === true) ruleSelKey.value = null }
function notifyCodePreview(r) {
  return `// 只读预览 · 在代码仓库中编辑\n@NotificationFunction("${r.notify_func_code || 'unnamed'}")\npublic Notification build(User recipient, Object subject) {\n  return Notification.builder()\n    .heading("${r.notify_title || '通知标题'}")\n    .content("...")\n    .build();\n}`
}
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
function addMapping(rule) { rule.prop_mappings.push(newMapping()) }
/* 手风琴: 同时只展开一条, 否则几条规则全展开后页面长到没法用 */
function toggleRuleCard(rule) {
  const open = rule._collapsed
  rules.value.forEach(r => { r._collapsed = true })
  if (open) {
    rule._collapsed = false
    loadRuleClassProps(ruleClassIdOf(rule))
    ruleSelKey.value = rule._k
  }
}
/* 规则拖拽排序 (顺序生效) */
const rulePickerOpen = ref(false)
const rDragIdx = ref(null)
function onRuleDragStart(i, ev) { rDragIdx.value = i; if (ev?.dataTransfer) ev.dataTransfer.effectAllowed = 'move' }
function onRuleDrop(target) { const from = rDragIdx.value; rDragIdx.value = null; if (from === null || from === target) return; const [it] = rules.value.splice(from, 1); rules.value.splice(target, 0, it) }
/* 添加参数 = 从对象属性里挑 (主体对象 + 规则中创建的对象), 已加入的不再列出 */
const propPickerOpen = ref(false)
const propPickerSec = ref('')
const propSources = ref([])
async function addParam(sec) {
  propPickerSec.value = (typeof sec === 'string' && sec) ? sec : (sections.value[0] || '基础参数')
  const out = []
  if (form.object_class_id) {
    await loadClassProps()
    const c = classOptions.value.find(x => x.id === form.object_class_id)
    out.push({ key: 'subject', classId: form.object_class_id, color: '#165DFF',
      label: c?.cn || '本动作对象', sub: c?.api_name || '', props: classProps.value })
  }
  /* 创建对象规则会产出新对象, 它们的属性同样可以做成表单参数 */
  for (const r of rules.value) {
    if (r.kind !== 'create_object') continue
    const id = r.obj_class_id || form.object_class_id
    if (!id || out.some(s => s.classId === id)) continue
    await loadRuleClassProps(id)
    const c = classOptions.value.find(x => x.id === id)
    out.push({ key: 'rule-' + r._k, classId: id, color: '#00B42A',
      label: c?.cn || '规则创建的对象', sub: `规则:${r.rule_name || '创建对象'}`,
      props: ruleClassPropsCache[id] || [] })
  }
  propSources.value = out
  if (!out.length) {   // 没有可选来源时退回原来的手工新建
    formParams.value.push({ ...defaultParam(), section: propPickerSec.value })
    return openParam(formParams.value.length - 1)
  }
  propPickerOpen.value = true
}
const usedPropCodes = computed(() => formParams.value.map(p => p.property_code || p.param_code).filter(Boolean))
function onPropsPicked(list) {
  const sec = propPickerSec.value
  list.forEach(p => {
    const t = mapXsd(p.dataType)
    const cls = classOptions.value.find(c => c.id === p.classId)
    formParams.value.push({ ...defaultParam(), section: sec, param_code: 'p_' + p.code, param_name: p.name,
      param_type: t, display_type: autoDisplay(t), is_required: p.required ? 1 : 0, property_code: p.code,
      /* 记住取自哪个对象类, 头部据此标出来源 */
      src_class_id: p.classId || '', src_class_name: cls?.cn || '', src_class_api: cls?.api_name || '' })
  })
  BL.success(`已添加 ${list.length} 个参数`)
}
/* 非本动作主体对象的参数才需要标来源, 否则每个参数都挂一个标签太吵 */
function srcTagOf(p) {
  if (!p?.src_class_id || p.src_class_id === form.object_class_id) return null
  return { name: p.src_class_name || '关联对象', api: p.src_class_api || '' }
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
function ovCountOf(target) {
  return (selParam.value?.overrides || []).filter(o => (o.actions || []).some(a => a.type === target)).length
}
/* ===== 覆盖规则 (文档 1.4.4.3) — 就地编辑, 折叠态只看摘要 ===== */
const ovFold = ref(new Set())
const OV_ACTION_OPTS = OV_ACTIONS.map(a => ({ value: a.value, label: a.label }))
const OV_OP_KEYS = OV_OPERATORS.map(o => o.key)
function ovBoolAction(t) { return !!ovActionMeta(t)?.bool }
/* 老数据里已下线的类型仍要能显示出来, 临时补进该行的下拉 */
function ovActionOptsOf(a) {
  const opts = OV_ACTION_OPTS.slice()
  if (a.type && !opts.some(o => o.value === a.type)) opts.push({ value: a.type, label: OV_ACTION_LABEL[a.type] || a.type })
  return opts
}
function toggleOvFold(ov) {
  const s = new Set(ovFold.value)
  s.has(ov._k) ? s.delete(ov._k) : s.add(ov._k)
  ovFold.value = s
}
/* 枚举类参数给出候选值, 其余自由输入 */
function ovValueOptions(c) {
  if (!c || c.subject !== 'param') return []
  const f = ovParamFields.value.find(x => x.code === c.field)
  return (f?.options || []).filter(o => String(o.value ?? '').trim()).map(o => ({ value: o.value, label: o.label || o.value }))
}
function addOvAction(ov) {
  const used = ov.actions.map(a => a.type)
  const next = OV_ACTIONS.find(a => !used.includes(a.value)) || OV_ACTIONS[0]
  ov.actions.push({ _k: ovUid(), type: next.value, value: next.bool ? 1 : '' })
}
function onOvActTypeChange(a) { a.value = ovBoolAction(a.type) ? 1 : '' }
/* 条件只能引用「当前字段上方」的参数 — 避免循环依赖 */
const ovParamFields = computed(() => formParams.value.slice(0, Math.max(0, selIdx.value))
  .filter(p => String(p.param_code || '').trim())
  .map(p => ({ code: p.param_code, name: p.param_name || p.param_code, dataType: p.param_type, options: p.options || [] })))
const ovDefaults = computed(() => ({
  visible: Number(selParam.value?.visible ?? 1),
  required: Number(selParam.value?.is_required ?? 0),
  disabled: Number(selParam.value?.disabled ?? 0),
}))
function addOverride(presetType) {
  const b = emptyBlock(presetType || '')
  selParam.value.overrides.push(b)          // 新块默认展开, 直接就能填
  return b
}
async function removeOverride(i) {
  const list = selParam.value.overrides
  /* 未填过的空块直接删, 不必确认 */
  const b = list[i]
  const blank = !b?.cond?.children?.length && !b?.actions?.length
  if (!blank) {
    const ok = await BL.confirm({ title: '删除覆盖规则', content: `确定删除「${blockTitle(b)}」?`, danger: true, okText: '删除' })
    if (!ok) return
  }
  list.splice(i, 1)
}
function moveOverride(i, dir) {
  const list = selParam.value.overrides, j = i + dir
  if (j < 0 || j >= list.length) return
  const [it] = list.splice(i, 1); list.splice(j, 0, it)
}
/* 「值」页快捷入口: 复用进入页面时自动给出的空规则, 避免多出一条 */
function addOverrideFor(target) {
  switchDetailTab('override')
  nextTick(() => {
    const list = selParam.value?.overrides || []
    const last = list[list.length - 1]
    if (last && !last.cond?.children?.length && !last.actions?.length) {
      if (target) last.actions.push({ _k: ovUid(), type: target, value: target === 'disabled' ? 1 : 0 })
    } else addOverride(target)
  })
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
/* 头部元信息行: 把 width/labelPos 的值翻成中文 */
function dspLabel(key, v) {
  const list = key === 'width' ? DSP_WIDTHS : DSP_LABEL_POS
  return list.find(x => x.value === v)?.label || v || ''
}
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
/* 预览栏可调宽: 够宽(≥PV_GRID_MIN)才按真实栅格铺排, 否则退化单列, 免去全屏浮层 */
const PV_MIN = 300, PV_NARROW = 384, PV_WIDE = 660, PV_GRID_MIN = 380, PV_RESERVE = 640
const pvStoredW = Number(localStorage.getItem('bl.adw.pvWidth')) || 0
const pvWidth = ref(pvStoredW >= PV_MIN ? pvStoredW : PV_NARROW)
const pvResizing = ref(false)
const pvGrid = computed(() => pvWidth.value >= PV_GRID_MIN)
const pvWide = computed(() => pvWidth.value > PV_NARROW)
function pvMaxPx() { return Math.max(PV_MIN, drawerWidth.value - PV_RESERVE) }
function savePvWidth() { localStorage.setItem('bl.adw.pvWidth', String(pvWidth.value)) }
let pvStartX = 0, pvStartW = 0
function onPvDragStart(e) {
  pvResizing.value = true; pvStartX = e.clientX; pvStartW = pvWidth.value
  document.body.style.cursor = 'col-resize'; document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onPvDragMove); window.addEventListener('mouseup', onPvDragEnd)
}
function onPvDragMove(e) { pvWidth.value = Math.min(pvMaxPx(), Math.max(PV_MIN, pvStartW + (pvStartX - e.clientX))) }
function onPvDragEnd() {
  pvResizing.value = false; document.body.style.cursor = ''; document.body.style.userSelect = ''
  savePvWidth()
  window.removeEventListener('mousemove', onPvDragMove); window.removeEventListener('mouseup', onPvDragEnd)
}
/* 加宽时抽屉本身不够放就顺带把抽屉撑开, 否则「加宽」点了看不出变化 */
function togglePvWide() {
  if (pvWide.value) pvWidth.value = PV_NARROW
  else {
    const need = PV_WIDE + PV_RESERVE
    if (drawerWidth.value < need) {
      drawerWidth.value = Math.min(drawerMaxPx(), need)
      localStorage.setItem('bl.adw.width', String(drawerWidth.value))
    }
    pvWidth.value = Math.min(pvMaxPx(), PV_WIDE)
  }
  savePvWidth()
}
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
/* 左侧导航分组收起状态; 键加 rule:/form: 前缀避免规则分组与同名分区互相干扰 */
const navFold = reactive(new Set())
function isNavFold(k) { return navFold.has(k) }
function toggleNavFold(k) { navFold.has(k) ? navFold.delete(k) : navFold.add(k) }

/* 分区收展仅为配置期视图状态, 不入库 */
const secCollapsed = reactive(new Set())
function toggleSec(sec) { secCollapsed.has(sec) ? secCollapsed.delete(sec) : secCollapsed.add(sec) }

/* 分区顺序由 formParams 中各分区首个参数的位置决定, 因此排序 = 按新分区顺序整体重排参数 */
const secDrag = ref(null)
function onSecDragStart(sec, e) { secDrag.value = sec; e.dataTransfer.effectAllowed = 'move' }
function onPartDrop(sec) {
  if (secDrag.value) { moveSection(secDrag.value, sec); secDrag.value = null; return }
  onFieldDropSection(sec)
}
function moveSection(src, target) {
  const order = sections.value.slice()
  const from = order.indexOf(src), to = order.indexOf(target)
  if (from < 0 || to < 0 || from === to) return
  order.splice(from, 1)
  order.splice(to, 0, src)
  const bucket = new Map(order.map(s => [s, []]))
  formParams.value.forEach(p => {
    const s = p.section || '基础参数'
    if (!bucket.has(s)) bucket.set(s, [])
    bucket.get(s).push(p)
  })
  formParams.value = order.flatMap(s => bucket.get(s) || [])
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
/* 开关/单选/复选/只读没有可输入区域, 占位文字对它们无效 */
const PLACEHOLDER_TYPES = ['input', 'textarea', 'number', 'select', 'user', 'tree']
function hasPlaceholder(p) { return !!p && PLACEHOLDER_TYPES.includes(p.display_type) }
function isRefByRule(code) {
  if (!String(code || '').trim()) return false
  return rules.value.some(r =>
    (r.prop_mappings || []).some(m => Number(m.value_source) === 1 && m.value_content === code) ||
    (r.func_params || []).some(fp => Number(fp.value_source) === 1 && fp.value_content === code))
}
function addOption(p) { p.options.push({ value: '', label: '' }) }
/* 标题输入框的 size: 中日韩全角按 2 个字符宽算 */
function nameSize(s) {
  let w = 0
  for (const ch of String(s || '')) w += /[⺀-鿿＀-￯]/.test(ch) ? 2 : 1
  return Math.min(30, Math.max(5, w + 1))
}
/* 切数据类型: 前端组件跟着换; 枚举必须走「多选」页签配选项, 用户输入/用户组对枚举无意义 */
function onParamTypeChange(t) {
  const p = selParam.value; if (!p) return
  p.display_type = autoDisplay(t)
  if (t === 'enum' && p.input_mode !== 'multi') p.input_mode = 'multi'
}

/* ===== 从对象集获取选项 / 对象引用参数筛选规则 (文档 1.4.4.1.3 / 3.3.2) ===== */
/* 该对象类型的属性候选 (复用规则页的按类缓存) */
const objsetProps = computed(() => {
  const id = selParam.value?.objset?.class_id
  if (!id) return []
  return ruleClassPropsCache[id] || (id === form.object_class_id ? classProps.value : [])
})
const objsetPropOptions = computed(() => objsetProps.value.map(p => ({ value:p.code, label:`${p.name} (${p.code})` })))
const objsetLabelProp = computed(() => objsetProps.value.find(p => p.code === selParam.value?.objset?.label_prop) || null)
/* 关联搜索路径文案: 关联搜索至 {链接类型} 的 {对端对象} */
const objsetLinkPath = computed(() => {
  const code = selParam.value?.objset?.link_type_code
  if (!code) return '选择链接类型后,可通过关联对象的属性搜索当前对象集'
  const l = (props.allLinkTypes || []).find(x => linkTypeCode(x) === code)
  const peer = classOptions.value.find(c => c.id === (l?.r_object_type_id || l?.rObjectTypeId))?.cn || '关联对象'
  return `关联搜索至 ${l ? (l.rdfs_label || l.rdfsLabel || code) : code} 的 ${peer}`
})
/* ===== 默认值 · 对象参数属性 两级级联 (文档 2.3.3 类型一) ===== */
/* 第一级选中的对象参数, 其指向的对象类型来自该参数自身的「起始对象集」配置 */
const defObjParam = computed(() => formParams.value.find(p => p.param_code === selParam.value?.default_obj_param) || null)
/* 对象参数没配起始对象集时, 回退到动作主体对象, 否则属性下拉会空到没法用 */
const defObjOwnClassId = computed(() => defObjParam.value?.objset?.class_id || '')
const defObjClassId = computed(() => defObjOwnClassId.value || form.object_class_id || '')
const defObjFallback = computed(() => !!selParam.value?.default_obj_param && !defObjOwnClassId.value)
const defObjPropOptions = computed(() => {
  const arr = ruleClassPropsCache[defObjClassId.value] || (defObjClassId.value === form.object_class_id ? classProps.value : [])
  return arr.map(p => ({ value:p.code, label:`${p.name} (${p.code})` }))
})
const defObjProp = computed(() => {
  const arr = ruleClassPropsCache[defObjClassId.value] || []
  return arr.find(p => p.code === selParam.value?.default_obj_prop) || null
})
const defObjClassName = computed(() => classOptions.value.find(c => c.id === defObjClassId.value)?.cn || '')
const defObjPlaceholder = computed(() => {
  if (!selParam.value?.default_obj_param) return '请先选择对象参数'
  if (!defObjClassId.value) return '无法确定对象类型,请先给该对象参数配置起始对象集'
  return defObjPropOptions.value.length ? '选择属性字段' : `「${defObjClassName.value || '该对象'}」暂无属性`
})
const defObjHint = computed(() => {
  const p = selParam.value; if (!p) return ''
  const pn = defObjParam.value?.param_name || p.default_obj_param
  if (defObjFallback.value) return `「${pn}」还没配置起始对象集,属性暂按动作主体对象「${defObjClassName.value || '—'}」列出;建议先到该参数的「约束设置」里选定对象集。`
  if (!p.default_obj_param || !p.default_obj_prop) return '表单打开时,自动取所选对象参数上的该属性值回填为本参数默认值,用户仍可自行修改。'
  const cn = defObjPropOptions.value.find(o => o.value === p.default_obj_prop)?.label || p.default_obj_prop
  return `表单打开时自动取「${pn}」的「${cn}」回填,用户仍可自行修改。`
})
watch(defObjClassId, id => { if (id) loadRuleClassProps(id) }, { immediate: true })

/* 传给 ObjectSetFilter 的整包 props (字符串多选 / 对象引用参数共用) */
const objsetBind = computed(() => ({
  objset: selParam.value?.objset,
  classOptions: classOptions.value,
  varOptions: objectParamOptions.value,
  propOptions: objsetPropOptions.value,
  labelProp: objsetLabelProp.value,
  linkTypeOptions: linkTypeOptions.value,
  linkPath: objsetLinkPath.value,
  fallbackClassId: form.object_class_id || '',
}))
/* 进入参数详情 / 切换起始对象集时按需拉属性 */
watch(() => selParam.value?.objset?.class_id, id => { if (id) loadRuleClassProps(id) }, { immediate: true })
function mapXsd(dt){ const s=String(dt||'').toLowerCase(); if(s.includes('enum'))return'enum'; if(/(int|decimal|double|float)/.test(s))return'number'; if(s.includes('bool'))return'boolean'; if(s.includes('date')||s.includes('time'))return'date'; return'string' }

async function onSave() {
  if (!String(form.rdfs_label||'').trim()) { activeMenu.value='overview'; editMode.value=true; return BL.warning('请填写动作名称') }
  if (ruleConflicts.value.length) { activeMenu.value = 'rules'; return BL.error('存在无效规则组合,请先修正:' + ruleConflicts.value[0]) }
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
/* 右侧内边距 6px 与 .adw-menu-item 对齐, 让计数/箭头和一级菜单同一竖线 */
.adw-tree-sec { display: flex; align-items: center; gap: 4px; font-size: 12px; color: var(--bl-text-3); font-weight: 600;
  padding: 6px 6px 4px 12px; cursor: pointer; user-select: none; border-radius: 6px; }
.adw-tree-sec:hover { color: var(--bl-text-2); background: var(--bl-bg-hover); }
/* margin-left 8 + 容器 gap 4 = 12, 与一级菜单 (gap 8 + chev margin 4) 等距 */
.adw-tree-sec-chev { margin-left: 8px; display: inline-flex; color: var(--bl-text-4); transition: transform .15s; }
.adw-tree-sec.is-fold .adw-tree-sec-chev { transform: rotate(-90deg); }
/* 计数盒子尺寸对齐 .adw-menu-badge (只是不要底色), 数字才会落在同一竖线 */
.adw-tree-sec-n { margin-left: auto; min-width: 16px; height: 16px; padding: 0 5px; font-size: 10px; font-weight: 400;
  color: var(--bl-text-4); display: inline-flex; align-items: center; justify-content: center; }
.adw-tree-item { display: flex; align-items: center; gap: 6px; padding: 6px 12px 6px 20px; font-size: 12.5px; color: var(--bl-text-2); cursor: pointer; border-radius: 6px; }
.adw-tree-item:hover { background: var(--bl-bg-hover); }
.adw-tree-item.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); }
.adw-tree-empty { padding: 8px 20px; font-size: 12px; color: var(--bl-text-3); }

/* 中间内容 */
/* 中栏隐藏滚动条(滚轮/触控板照常滚), 避免与右侧预览面板之间露出一条空白滚动槽; 同 AppSidebar .nav 的做法 */
.adw-main { flex: 1; min-width: 0; overflow-y: auto; padding: 10px 10px; scrollbar-width: none; -ms-overflow-style: none; }
.adw-main::-webkit-scrollbar { width: 0; height: 0; display: none; }
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
.fd-part-hd { display: flex; align-items: center; gap: 6px; padding: 10px 14px; border-bottom: 1px solid var(--bl-divider); color: var(--bl-text-2); cursor: pointer; user-select: none; }
.fd-part-hd:hover { background: var(--bl-bg-hover); }
.fd-part-grip { color: var(--bl-text-3); display: inline-flex; cursor: grab; }
.fd-part-hd:hover .fd-part-grip { color: var(--bl-text-2); }
.fd-part-hd:active .fd-part-grip { cursor: grabbing; }
.fd-part-chev { color: var(--bl-text-3); display: inline-flex; margin-left: 4px; transition: transform .15s; }
.fd-part.is-collapsed .fd-part-chev { transform: rotate(-90deg); }
.fd-part.is-collapsed .fd-part-hd { border-bottom: 0; }
.fd-part.is-collapsed .fd-part-body { display: none; }
.fd-part.is-secdrag { opacity: .45; }
.fd-part-op { padding: 2px; border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer; display: inline-flex;
  align-items: center; border-radius: 5px; opacity: 0; transition: opacity .12s, color .12s, background .12s; }
.fd-part:hover .fd-part-op { opacity: 1; }
.fd-part-op:hover { color: var(--bl-primary); background: var(--bl-primary-soft); }
.fd-part-name { display: inline-flex; align-items: center; font-size: 13px; font-weight: 600; color: var(--bl-text-1); cursor: pointer; border-radius: 5px; padding: 1px 4px; margin-left: -4px; }
.fd-part-name:hover { color: var(--bl-primary); background: var(--bl-bg-hover); }
/* 抽屉可拖窄, 分区名过长时截断而非折行, 否则头部会撑成两行 */
.fd-part-name { min-width: 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: block; }
.fd-part-count { padding: 0 7px; border-radius: 10px; background: var(--bl-bg-2); color: var(--bl-text-2); font-size: 11px; flex-shrink: 0; }
.fd-part-del { margin-left: 6px; padding: 2px; border: 0; background: transparent; color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; border-radius: 5px; opacity: 0; transition: opacity .12s, color .12s, background .12s; }
.fd-part:hover .fd-part-del { opacity: 1; }
.fd-part-del:hover { color: #f53f3f; background: color-mix(in srgb, #f53f3f 12%, transparent); }
.fd-part-body { padding: 0; }
/* 字段行: 扁平分组列表项 (去内边框, 靠分隔线) */
.fd-row { display: flex; align-items: center; gap: 10px; padding: 11px 14px; border-bottom: 1px solid var(--bl-divider); cursor: pointer; background: transparent; transition: background .12s; }
.fd-row:hover { background: var(--bl-bg-hover); }
.fd-row.is-dragging { opacity: .4; }
/* 常显但压低不透明度: 拖拽入口需要可被发现, 又不该跟参数名抢注意力 */
.fd-grip { color: var(--bl-text-3); cursor: grab; display: inline-flex; flex-shrink: 0; opacity: .45; transition: opacity .12s, color .12s; }
.fd-row:hover .fd-grip, .fd-opt:hover .fd-grip { opacity: 1; color: var(--bl-text-2); }
.fd-grip:active { cursor: grabbing; }
/* 分区内添加参数 */
.fd-part-add { display: flex; align-items: center; justify-content: center; gap: 4px; width: 100%; padding: 9px; background: transparent; border: 0; color: var(--bl-text-3); font-size: 12.5px; cursor: pointer; transition: color .12s, background .12s; }
.fd-part-add:hover { color: var(--bl-primary); background: var(--bl-bg-hover); }
.fd-dt { width: 24px; height: 24px; border-radius: 6px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center; }
.fd-row-txt { flex: 1; min-width: 0; }
.fd-row-name { font-size: 13px; color: var(--bl-text-1); }
.fd-row-warn { font-size: 11px; color: var(--bl-warning); margin-top: 2px; }
.fd-disp { display: inline-flex; align-items: center; gap: 4px; flex-shrink: 0; padding: 3px 8px; border-radius: 6px; background: var(--bl-bg-2); color: var(--bl-text-2); }
.fd-ph-row { display: flex; align-items: center; gap: 8px; }
.fd-ph-row .bl-input { flex: 1; min-width: 0; }
.fd-ph-tip { font-size: 11.5px; color: var(--bl-text-3); line-height: 1.6; margin-top: 8px; }
/* 隐藏参数: 列表里仍然列出, 但明确标出不渲染给用户 */
.fd-hidden { display: inline-flex; align-items: center; gap: 3px; flex-shrink: 0; font-size: 11.5px; padding: 2px 7px; border-radius: 5px; color: var(--bl-text-3); background: var(--bl-bg-2); }
.fd-hidden > span { display: inline-flex; }
.fd-disp-ic { display: inline-flex; color: var(--bl-text-3); }
.fd-disp-txt { font-size: 12px; }
.fd-chev { color: var(--bl-text-3); display: inline-flex; flex-shrink: 0; }
.fd-part-empty { padding: 14px; text-align: center; color: var(--bl-text-3); font-size: 12px; }
.fd-actions { display: flex; gap: 10px; margin: 12px 0 0; }

/* 参数详情页 */
/* 紧凑头部 (单行: 返回 + 参数信息) */
.fd-phd2 { display: flex; align-items: center; gap: 8px; margin: -8px -16px 0; padding: 5px 16px; background: var(--bl-bg-1); min-height: 40px; }
/* 参数详情头部: 返回 | 参数信息(上下两行) | 操作 三列 */
.fd-phd { display: flex; align-items: center; gap: 6px; margin: -8px -16px 0; padding: 7px 16px; background: var(--bl-bg-1); }
.fd-phd-sep { width: 1px; align-self: stretch; background: var(--bl-divider); flex-shrink: 0; }
.fd-phd-mid { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.fd-phd-r1 { display: flex; align-items: center; gap: 8px; min-width: 0; }
.fd-phd-r2 { display: flex; align-items: center; gap: 7px; font-size: 11.5px; color: var(--bl-text-3); min-width: 0; }
.fd-phd-r2 > span { flex-shrink: 0; }
.fd-phd-r2 > span.bl-truncate { flex: 1; min-width: 0; }
.fd-phd-act { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.fd-meta-dot { width: 3px; height: 3px; border-radius: 50%; background: var(--bl-border-strong); }
/* 头部三个标签统一高度: 全局 .bl-tag 是 22px, 自定义的两个跟齐 */
.fd-phd-r1 .bl-tag, .fd-code-tag, .fd-req-tag { height: 22px; display: inline-flex; align-items: center; flex-shrink: 0; border-radius: 4px; font-size: 11.5px; box-sizing: border-box; }
.fd-code-tag { color: var(--bl-text-2); background: var(--bl-bg-2); border: 1px solid var(--bl-divider); padding: 0 7px; max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.fd-code-tag.is-empty { color: #f53f3f; border-color: color-mix(in srgb, #f53f3f 35%, transparent); background: color-mix(in srgb, #f53f3f 8%, transparent); }
.fd-req-tag { font-weight: 600; color: #f53f3f; background: color-mix(in srgb, #f53f3f 12%, transparent); padding: 0 7px; }
/* 参数取自关联对象(非本动作主体)时的来源标记 */
.fd-src-tag { display: inline-flex; align-items: center; gap: 4px; height: 22px; flex-shrink: 0; padding: 0 8px; border-radius: 4px; box-sizing: border-box;
  font-size: 11.5px; color: #fff; background: #00B42A; max-width: 260px; overflow: hidden; white-space: nowrap; }
.fd-src-tag > span { display: inline-flex; flex-shrink: 0; }
.fd-src-tag > i { font-style: normal; opacity: .8; font-size: 10.5px; overflow: hidden; text-overflow: ellipsis; }
.fd-src-tag.is-sm { height: 19px; font-size: 11px; padding: 0 6px; }
.fd-back { display: inline-flex; align-items: center; gap: 2px; padding: 3px 8px 3px 4px; font-size: 12.5px; color: var(--bl-text-2); background: transparent; border: 0; cursor: pointer; border-radius: 6px; flex-shrink: 0; }
.fd-back:hover { color: var(--bl-primary); background: var(--bl-bg-hover); }
.fd-phd2-sep { width: 1px; height: 16px; background: var(--bl-divider); flex-shrink: 0; }
/* 宽度跟着内容走, 让类型胶囊紧贴标题; size 属性是不支持 field-sizing 的浏览器的兜底 */
.fd-pname2 { border: 0; outline: none; background: transparent; font-size: 16px; font-weight: 700; color: var(--bl-text-1); padding: 0;
  field-sizing: content; width: auto; min-width: 48px; max-width: 240px; }
.fd-pname2:focus { border-bottom: 1px solid var(--bl-primary); }

/* 表单详情: 头部固定 + 下方独立滚动, 收紧内边距节省高度 */
.adw-page.is-detail { margin: -10px; height: calc(100% + 20px); padding: 8px 10px 0; display: flex; flex-direction: column; min-height: 0; }
.fd-detail-hd { flex: 0 0 auto; background: var(--bl-bg-2); }
.fd-detail-body { flex: 1; min-height: 0; overflow-y: auto; margin-right: -10px; padding: 10px 8px 2px 0; }

/* 锚点导航 */
.fd-tabs { display: flex; gap: 2px; padding: 8px 0 4px; background: var(--bl-bg-2); border-top: 1px solid var(--bl-divider); }
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
.fd-tri-hd { display: flex; align-items: center; gap: 8px; font-size: var(--bl-fs-13); font-weight: 600; margin-bottom: 10px; }
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
/* 默认值 · 对象参数属性 两级级联 */
.fd-cascade { display: flex; flex-direction: column; gap: 10px; }
.fd-cascade-row { display: flex; align-items: center; gap: 8px; }
.fd-cascade-lbl { flex-shrink: 0; width: 66px; font-size: 12.5px; color: var(--bl-text-2); }
.fd-cascade-hint { font-size: 12px; color: var(--bl-text-3); line-height: 1.6; padding-left: 74px; }
.fd-cascade-hint.is-warn { color: #92400E; background: #FEF3C7; border: 1px solid #FDE68A; border-radius: 6px; padding: 8px 12px; margin-left: 74px; }
.fd-warn { background: #FEF3C7; border: 1px solid #FDE68A; border-radius: 6px; padding: 8px 12px; font-size: 12px; color: #92400E; }
/* 覆盖规则列表 (文档 1.4.4.3) */
.ov-note { background: var(--bl-primary-soft); border: 1px solid color-mix(in srgb, var(--bl-primary) 22%, transparent);
  border-radius: 8px; padding: 9px 13px; font-size: 12px; line-height: 1.7; color: var(--bl-text-2); margin-bottom: 12px; }
.ov-card { border: 1px solid var(--bl-border); border-radius: 9px; margin-bottom: 10px; overflow: hidden; }
.ov-card-hd { display: flex; align-items: center; gap: 8px; padding: 8px 10px 8px 12px; background: var(--bl-bg-2); cursor: pointer; }
.ov-chev { display: inline-flex; color: var(--bl-text-3); transition: transform .15s; flex-shrink: 0; }
.ov-chev.is-fold { transform: rotate(-90deg); }
/* 就地编辑区 (原覆盖弹框内容) */
.ov-edit { padding: 12px; border-top: 1px solid var(--bl-divider); }
.ov-tag { display: inline-block; font-size: 11.5px; font-weight: 700; padding: 2px 8px; border-radius: 4px; margin-bottom: 8px; }
.ov-tag-if { background: var(--bl-primary-soft); color: var(--bl-primary); }
.ov-tag-then { background: #FEF3C7; color: #92400E; margin-top: 16px; }
.ov-acts { display: flex; flex-direction: column; gap: 8px; }
.ov-act { display: flex; align-items: center; gap: 8px; padding: 8px 10px; border: 1px solid var(--bl-border); border-radius: 8px; }
.ov-act.is-redundant { border-color: #FDE68A; background: #FEF3C7; }
.ov-act-lbl { font-size: 12.5px; color: var(--bl-text-2); flex-shrink: 0; }
.ov-redundant { font-size: 11.5px; color: #92400E; flex-shrink: 0; }
.ov-empty-line { font-size: 12px; color: var(--bl-text-3); }
.ov-add-act { font-size: 12.5px; color: var(--bl-primary); cursor: pointer; align-self: flex-start; }
.ov-add-act:hover { text-decoration: underline; }
.ov-prio { flex-shrink: 0; font-size: 11px; font-weight: 600; color: var(--bl-primary); background: var(--bl-primary-soft); padding: 2px 7px; border-radius: 4px; }
.ov-title { font-size: 12.5px; font-weight: 600; color: var(--bl-text-1); min-width: 0; }
.ov-card-bd { padding: 9px 12px; display: flex; flex-direction: column; gap: 6px; }
.ov-sum { display: flex; align-items: flex-start; gap: 8px; font-size: 12px; color: var(--bl-text-2); line-height: 1.6; }
.ov-sum-tag { flex-shrink: 0; font-size: 10.5px; font-weight: 700; padding: 1px 6px; border-radius: 4px; margin-top: 1px; }
.ov-sum-if { background: var(--bl-primary-soft); color: var(--bl-primary); }
.ov-sum-then { background: #FEF3C7; color: #92400E; }
/* 对象引用参数: 选择类型锁定卡 (文档 3.3.1 样式高亮锁定, 不支持切换) */
.fd-locked { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border: 1px solid var(--bl-primary); background: var(--bl-primary-soft); border-radius: 8px; font-size: 13px; font-weight: 600; color: var(--bl-primary); cursor: default; }
:root[data-theme="dark"] .fd-warn { background: color-mix(in srgb, #D97706 16%, transparent); border-color: color-mix(in srgb, #D97706 40%, transparent); color: #FBBF24; }

/* 显示组件网格 */

/* 覆盖 */
.fd-ov-row { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }

/* 开关 (通用设置/预览) */
.adw-showsw { display: inline-block; width: 40px; height: 22px; border-radius: 11px; background: var(--bl-bg-3, #c9cdd4); position: relative; cursor: pointer; transition: background .15s; vertical-align: middle; flex-shrink: 0; }
.adw-showsw.is-on { background: var(--bl-primary); }
.adw-showsw-dot { position: absolute; left: 2px; top: 2px; width: 18px; height: 18px; border-radius: 50%; background: #fff; transition: left .15s; box-shadow: 0 1px 2px rgba(0,0,0,.3); }
.adw-showsw.is-on .adw-showsw-dot { left: 20px; }
/* 小号开关: 用于配置项密集的行 (全局显示配置 / 参数显示配置), 与规则页 .rl-sw 同尺寸 */
.adw-showsw-sm { width: 32px; height: 18px; border-radius: 9px; }
.adw-showsw-sm .adw-showsw-dot { width: 14px; height: 14px; }
.adw-showsw-sm.is-on .adw-showsw-dot { left: 16px; }

/* 预览增强 */
.adw-preview-fld { cursor: pointer; border-radius: 6px; padding: 4px 12px 8px; }
.adw-preview-fld.is-sel { background: var(--bl-primary-soft); outline: 1px solid var(--bl-primary); }
.adw-pv-switch { padding: 2px 0; }
/* 预览: 单选按钮组 / 复选框 */
.adw-pv-opts { display: flex; flex-wrap: wrap; gap: 8px 18px; padding: 5px 0; }
.adw-pv-opt { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: var(--bl-text-2); }
.adw-pv-radio { width: 14px; height: 14px; border-radius: 50%; border: 1.5px solid var(--bl-border-strong); flex-shrink: 0; }
.adw-pv-checkbox { width: 14px; height: 14px; border-radius: 3px; border: 1.5px solid var(--bl-border-strong); flex-shrink: 0; }
.adw-pv-help { font-size: 11px; color: var(--bl-text-3); margin-top: 4px; }
/* 预览控件与真实表单同款; 未定位的字段只是禁用交互, 不另画一套外观 */
.adw-pv-ctl .bl-input, .adw-pv-ctl .bl-textarea { width: 100%; font-size: 12px; box-sizing: border-box; }
.adw-pv-ctl .bl-input { height: 32px; }
.adw-pv-ctl.is-static { cursor: pointer; }
.adw-pv-ctl.is-static > * { pointer-events: none; }
.adw-pv-opt.is-live { cursor: pointer; }
.adw-pv-radio.is-on { border-width: 4px; border-color: var(--bl-primary); }
.adw-pv-checkbox.is-on { position: relative; background: var(--bl-primary); border-color: var(--bl-primary); }
.adw-pv-checkbox.is-on::after { content: ''; position: absolute; left: 4px; top: 1px; width: 3px; height: 7px; border: solid #fff; border-width: 0 1.5px 1.5px 0; transform: rotate(45deg); }

/* 右预览 */
.adw-preview { position: relative; background: var(--bl-bg-1); border-left: 1px solid var(--bl-border); display: flex; flex-direction: column; overflow: hidden; }
.adw-pv-drag { position: absolute; left: -2px; top: 0; bottom: 0; width: 5px; cursor: col-resize; background: transparent; transition: background-color .15s; z-index: 3; }
.adw-pv-drag:hover, .adw-pv-drag.is-resizing { background: var(--bl-primary); }
.adw-pv-mode { margin-left: 8px; font-size: 11px; font-weight: 400; color: var(--bl-text-3); border: 1px solid var(--bl-border); border-radius: 4px; padding: 1px 6px; }
.adw-pv-mode.is-real { color: var(--bl-primary); border-color: color-mix(in srgb, var(--bl-primary) 40%, transparent); background: var(--bl-primary-soft); }
.adw-preview-hd { padding: 8px 16px; font-size: 13px; font-weight: 600; border-bottom: 1px solid var(--bl-divider); }
.adw-preview-body { flex: 1; overflow-y: auto; padding: 16px 8px 16px 16px; }
.adw-preview-form {border-radius: 10px; padding: 0px; }
.adw-preview-title { font-size: 15px; font-weight: 600; margin-bottom: 14px; }
.adw-preview-fld { margin-bottom: 12px; }
.adw-preview-lbl { font-size: 12px; color: var(--bl-text-2); margin-bottom: 5px; }
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
.rl-grip { color: var(--bl-text-3); cursor: grab; display: inline-flex; transition: color .12s; }
.rl-grip:hover { color: var(--bl-text-2); }
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
