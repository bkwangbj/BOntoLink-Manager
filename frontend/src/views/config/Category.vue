<template>
  <div class="page">
    <PageHeader title="行业 / 领域 / 分组分类" subtitle="支持行业、领域层级搭建、命名空间、版本管理">
      <template #actions>
        <div class="search-wrap">
          <span class="search-icon" v-html="BL.icon('search', 14)"></span>
          <input class="bl-input search-input" placeholder="搜索分类（中文 / 拼音 / 简拼）" v-model="treeQ" />
        </div>
        <button class="bl-btn" @click="exportTree" v-html="iconText('download','导出')"></button>
        <button class="bl-btn bl-btn-primary" @click="openCreate('industry', null)" v-html="iconText('plus','新建')"></button>
      </template>
    </PageHeader>

    <div :class="['three-pane', !selected && 'no-detail']">
      <!-- 左：分类树 -->
      <section class="pane pane-tree">
        <div class="pane-toolbar">
          <div class="view-toggle">
            <button :class="['vt-btn', viewMode==='tree' && 'is-on']" @click="viewMode='tree'" title="树形视图">
              <span v-html="BL.icon('list', 14)"></span>
              <span>树形</span>
            </button>
            <button :class="['vt-btn', viewMode==='list' && 'is-on']" @click="viewMode='list'" title="列表视图">
              <span v-html="BL.icon('grid', 14)"></span>
              <span>列表</span>
            </button>
          </div>
          <span class="bl-sep"></span>
          <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="新增" @click="openCreate('industry', null)" v-html="BL.icon('plus', 12)"></button>
          <button :class="['bl-btn bl-btn-text bl-btn-sm bl-btn-icon', treeSearchOpen && 'is-on']" title="搜索" @click="toggleTreeSearch" v-html="BL.icon('search', 12)"></button>
        </div>
        <div v-if="treeSearchOpen" class="pane-search-row">
          <span class="pane-search-ic" v-html="BL.icon('search', 12)"></span>
          <input ref="treeSearchInputRef" class="bl-input pane-search-input" v-model="treeQ"
                 placeholder="搜索分类（中文 / 拼音 / 简拼）" @keydown.esc="closeTreeSearch" />
          <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="关闭搜索" @click="closeTreeSearch" v-html="BL.icon('x', 12)"></button>
        </div>

        <div class="tree-scroll">
          <template v-if="viewMode==='tree'">
            <TreeNode
              v-for="node in filteredTree" :key="node.id"
              :node="node"
              :selected="selected"
              :search="treeQ"
              :expanded="expandedSet"
              @select="onSelect"
              @toggle="onToggle"
              @ctx="onCtx" />
            <div v-if="!filteredTree.length" class="bl-empty">
              <div class="bl-empty-icon" v-html="BL.icon('folder', 32)"></div>
              暂无分类
            </div>
          </template>
          <template v-else>
            <div v-for="row in flatList" :key="row.id"
                 :class="['bl-list-item li-row', selected?.id===row.id && 'is-active']"
                 @click="onSelect(row)">
              <span class="li-ico" :style="{ background: nodeProfile(row).color }" v-html="BL.icon(nodeProfile(row).icon, 12, '#fff')"></span>
              <span class="bl-grow bl-truncate">{{ row.label || row.categoryCode }}</span>
            </div>
          </template>
        </div>

        <!-- 版本（仅领域节点出现） -->
        <div class="versions" v-if="selected && selected.categoryType === 2">
          <div class="versions-hd">
            <span>版本列表</span>
            <button class="bl-btn bl-btn-sm bl-btn-text" @click="newVersion" v-html="iconText('plus','新建版本')"></button>
          </div>
          <div class="versions-body">
            <div v-for="v in versions" :key="v.id" class="version-row">
              <span class="bl-tag" :class="v.isCurrent ? 'bl-tag-success' : ''">{{ v.version }}</span>
              <span class="bl-grow bl-truncate" :title="v.rdfsLabel || ''">{{ v.rdfsLabel || '—' }}</span>
              <span class="bl-muted" style="font-size:11px">{{ shortDate(v.publishTime) }}</span>
              <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" v-html="BL.icon('trash', 12)" @click="removeVersion(v)"></button>
            </div>
            <div v-if="!versions.length" class="bl-empty" style="padding:16px">尚无版本</div>
          </div>
        </div>
      </section>

      <!-- 中：统计卡片 -->
      <section class="pane pane-center">
        <!-- 默认总览：未选中任何节点时显示 -->
        <template v-if="!selected">
          <div class="ov-head">
            <div>
              <h2 style="margin:0;font-size:var(--bl-fs-20)">行业分类总览</h2>
              <div class="bl-muted" style="font-size:12px;margin-top:4px">点击左侧任一节点查看详情；下方为全局统计与行业分布</div>
            </div>
          </div>

          <div class="stats">
            <div class="stat-card" v-for="s in globalStats" :key="s.key">
              <div class="stat-hd">
                <span class="stat-ic" :style="{ color: s.color, background: s.color + '14' }" v-html="BL.icon(s.icon, 14, s.color)"></span>
                <span class="stat-lbl">{{ s.label }}</span>
              </div>
              <div class="stat-val">{{ fmt(s.value) }}</div>
            </div>
          </div>

          <div class="section-title is-strong">
            <span class="st-bar"></span>
            <span>行业对象数分布</span>
          </div>
          <div class="ov-chart">
            <div class="ov-bars">
              <div v-for="ind in industries" :key="ind.id" class="ov-bar-col">
                <div class="ov-bar-val" :style="{ color: nodeProfile(ind).color }">{{ industryStatsMap[ind.id]?.classCount ?? 0 }}</div>
                <div class="ov-bar-bar" :style="{ background: nodeProfile(ind).color, height: barH(ind) + 'px' }"></div>
              </div>
            </div>
            <div class="ov-x">
              <span v-for="ind in industries" :key="ind.id" :style="{ flex: 1 }">{{ ind.label }}</span>
            </div>
          </div>

          <div class="section-title">行业列表</div>
          <div class="child-grid" v-if="industries.length">
            <div v-for="c in industries" :key="c.id" class="child-card" @click="onSelect(c)" @dblclick="onSelect(c)">
              <div class="cc-hd">
                <div class="cc-ic" :style="{ background: nodeProfile(c).color }" v-html="BL.icon(nodeProfile(c).icon, 22, '#fff')"></div>
                <div class="cc-body">
                  <div class="cc-title-row">
                    <span class="cc-title bl-truncate">{{ c.label }}</span>
                    <span class="bl-tag cc-tag">行业</span>
                  </div>
                  <div class="cc-mini">
                    <span class="cc-pri">{{ readable(industryStatsMap[c.id]?.classCount || 0) }}</span>
                    <span class="bl-muted">个对象</span>
                  </div>
                </div>
              </div>
              <div class="cc-ft">
                <span class="bl-muted">共 {{ (c.children || []).length }} 个领域</span>
                <span class="cc-sec">{{ readable(industryStatsMap[c.id]?.linkCount || 0) }} <span class="bl-muted">个关系</span></span>
              </div>
            </div>
          </div>
        </template>
        <template v-else>
          <!-- 面包屑：总览 / 行业 / 领域 / 分组 -->
          <nav class="crumb">
            <a class="crumb-link" @click="backToOverview" title="返回总览">
              <span v-html="BL.icon('grid', 12)"></span>
              <span style="margin-left:4px">总览</span>
            </a>
            <template v-for="(p, i) in selectedPath" :key="p.id">
              <span class="crumb-sep" v-html="BL.icon('chevronRight', 11)"></span>
              <a :class="['crumb-link', i === selectedPath.length - 1 && 'is-cur']"
                 @click="i < selectedPath.length - 1 && onSelect(p)">
                <span>{{ p.label }}</span>
              </a>
            </template>
          </nav>
          <div class="center-header">
            <div class="bl-icon-block" :style="{ background: nodeProfile(selected).color }" v-html="BL.icon(nodeProfile(selected).icon, 18, '#fff')"></div>
            <div class="bl-grow">
              <div class="title-row">
                <h2>{{ selected.label || selected.categoryCode }}</h2>
                <span class="bl-tag bl-tag-primary">{{ catTypeText(selected.categoryType) }}</span>
                <span v-if="selected.nsCode" class="bl-tag">{{ selected.nsCode }}</span>
              </div>
              <div class="bl-muted" style="font-size:12px">{{ selected.rdfsComment || selected.description || '尚未填写说明' }}</div>
            </div>
          </div>

          <div class="stats">
            <div class="stat-card" v-for="s in statCards" :key="s.key">
              <div class="stat-hd">
                <span class="stat-ic" :style="{ color: s.color, background: s.color + '14' }" v-html="BL.icon(s.icon, 14, s.color)"></span>
                <span class="stat-lbl">{{ s.label }}</span>
                <button v-if="s.createLabel" class="stat-add" :title="s.createLabel" @click.stop="jumpTo(s.link)" v-html="BL.icon('plus', 12)"></button>
              </div>
              <div class="stat-val">{{ fmt(s.value) }}</div>
            </div>
          </div>

          <!-- 分组节点：展示对象关系图谱；行业/领域节点：展示下级节点卡片 -->
          <template v-if="selected.categoryType === 3">
            <div class="section-title is-strong">
              <span class="st-bar"></span>
              <span>对象关系图谱</span>
              <span class="bl-sep"></span>
              <button class="bl-btn bl-btn-sm bl-btn-text" @click="downloadGraph" title="下载 SVG">
                <span v-html="BL.icon('download', 12)"></span>
                <span style="margin-left:4px">下载</span>
              </button>
            </div>
            <GroupGraph :node-id="selected.id" class="grp-graph-grow" />
          </template>
          <template v-else>
          <div class="section-title">下级节点</div>
          <div class="child-grid" v-if="children.length">
            <div v-for="c in children" :key="c.id" class="child-card" @click="onSelect(c)" @dblclick="onSelect(c)">
              <div class="cc-hd">
                <div class="cc-ic" :style="{ background: nodeProfile(c).color }" v-html="BL.icon(nodeProfile(c).icon, 22, '#fff')"></div>
                <div class="cc-body">
                  <div class="cc-title-row">
                    <span class="cc-title bl-truncate">{{ c.label }}</span>
                    <span class="bl-tag cc-tag">{{ catTypeText(c.categoryType) }}</span>
                  </div>
                  <div class="cc-mini">
                    <span class="cc-pri">{{ childStat(c).primary }}</span>
                    <span class="bl-muted">个对象</span>
                  </div>
                </div>
              </div>
              <div v-if="childChips(c).length" class="cc-chips">
                <template v-for="(chip, idx) in childChips(c)" :key="chip.id">
                  <span class="cc-chip" :title="chip.api_name">{{ chip.display_name || chip.api_name }}</span>
                  <span v-if="idx < childChips(c).length - 1" class="cc-sep">↔</span>
                </template>
                <span v-if="childStat(c).extra > 0" class="cc-more">+{{ childStat(c).extra }}</span>
              </div>
              <div class="cc-ft">
                <span class="bl-muted">共 {{ childStat(c).groups }} 个{{ c.categoryType === 1 ? '领域' : '分组' }}</span>
                <span class="cc-sec">{{ childStat(c).secondary }} <span class="bl-muted">个关系</span></span>
              </div>
            </div>
          </div>
          <div v-else class="bl-empty" style="padding:24px">无下级节点</div>
          </template>
        </template>
      </section>

      <!-- 右：详情 -->
      <section class="pane pane-detail">
        <div v-if="!selected" class="bl-empty" style="padding:60px">
          <div class="bl-empty-icon" v-html="BL.icon('cog', 36)"></div>
          请选择左侧节点
        </div>
        <template v-else>
          <div class="tabs">
            <div v-for="t in detailTabs" :key="t.key"
                 :class="['tab', tab===t.key && 'is-active']"
                 @click="tab = t.key">
              {{ t.label }}
            </div>
          </div>

          <div class="tab-body">
            <!-- 基础信息 -->
            <div v-if="tab==='basic'">
              <FieldRow label="编码" inline><span class="bl-mono">{{ selected.categoryCode }}</span></FieldRow>
              <FieldRow label="状态" inline>
                <span :class="['bl-tag', selected.status===1 ? 'bl-tag-success' : 'bl-tag-warning']">
                  {{ selected.status===1 ? '启用' : '禁用' }}
                </span>
              </FieldRow>
              <FieldRow label="标准名" hint="rdfs:label · 知识图谱通用标准显示名"><input class="bl-input" v-model="editForm.rdfsLabel" /></FieldRow>
              <FieldRow label="注释" hint="rdfs:comment · 给大模型 / 推理引擎 / 开发者看的正式语义定义"><textarea class="bl-textarea" rows="3" v-model="editForm.rdfsComment"></textarea></FieldRow>
              <FieldRow label="参考资料" hint="rdfs:seeAlso · 关联文档 / 链接 / 参考出处"><input class="bl-input" v-model="editForm.rdfsSeeAlso" /></FieldRow>
              <FieldRow label="定义来源" hint="rdfs:isDefinedBy · 该本体资源的权威定义出处、归属规范"><input class="bl-input" v-model="editForm.rdfsDefinedBy" /></FieldRow>
              <FieldRow label="说明"><textarea class="bl-textarea" rows="3" v-model="editForm.description"></textarea></FieldRow>
              <div class="id-row">
                <span class="id-lbl">ID</span>
                <span class="id-val bl-mono bl-muted" :title="selected.id">{{ selected.id }}</span>
                <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="复制 ID" @click="copyText(selected.id)" v-html="BL.icon('copy', 12)"></button>
              </div>
              <div class="id-row">
                <span class="id-lbl">RID</span>
                <span class="id-val bl-mono bl-muted" :title="selected.rid">{{ selected.rid }}</span>
                <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="复制 RID" @click="copyText(selected.rid)" v-html="BL.icon('copy', 12)"></button>
              </div>
              <div class="tab-save-bar">
                <button class="bl-btn bl-btn-primary" @click="saveBasic">保存</button>
              </div>
            </div>

            <!-- 命名空间（仅领域） -->
            <div v-if="tab==='namespace' && selected.categoryType === 2">
              <FieldRow label="绑定的命名空间">
                <span class="bl-tag bl-tag-primary">{{ selected.nsCode }}</span>
                <span class="bl-muted" style="margin-left:8px;font-size:12px">绑定后不可修改</span>
              </FieldRow>
              <template v-if="nsInfo">
                <FieldRow label="中文名称">{{ nsInfo.nsName }}</FieldRow>
                <FieldRow label="URI"><span class="bl-mono">{{ nsInfo.nsUri }}</span></FieldRow>
                <FieldRow label="层级标识"><span class="bl-mono">{{ nsInfo.hierarchyPath }}</span></FieldRow>
                <FieldRow label="当前版本"><span class="bl-tag bl-tag-primary">{{ nsInfo.currVersion }}</span></FieldRow>
              </template>
              <FieldRow label="元数据 (JSON)">
                <textarea class="bl-textarea bl-mono" rows="8" v-model="nsMetadata" placeholder='{ "timeResolution": "..." }'></textarea>
              </FieldRow>
              <div class="bl-flex-end" style="margin-top:8px">
                <button class="bl-btn bl-btn-primary" @click="saveNsMetadata">保存命名空间元数据</button>
              </div>
            </div>

            <!-- 分组：成员 -->
            <div v-if="tab==='members' && selected.categoryType === 3">
              <div class="grp-hd">
                <span>分组成员 ({{ boundClasses.length }})</span>
                <button class="bl-btn bl-btn-primary bl-btn-sm" @click="addMember">
                  <span v-html="BL.icon('plus', 12, '#fff')"></span>
                  <span style="margin-left:4px">添加成员</span>
                </button>
              </div>
              <div class="grp-list">
                <div v-for="(c, idx) in boundClasses" :key="c.id" class="grp-row"
                     draggable="true"
                     @dragstart="onDragStart(idx)"
                     @dragover="onDragOver"
                     @drop="onDrop(idx)">
                  <span class="grp-drag" title="拖拽排序" v-html="BL.icon('list', 12)"></span>
                  <div class="bl-icon-block bl-icon-block-sm" :style="{ background: c.color || '#165DFF' }" v-html="BL.icon(c.icon || 'cube', 12, '#fff')"></div>
                  <span class="bl-grow bl-truncate">{{ c.display_name || c.api_name }}</span>
                  <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="移除" @click="removeMember(c)" v-html="BL.icon('x', 12)"></button>
                </div>
                <div v-if="!boundClasses.length" class="bl-empty" style="padding:20px">尚未添加任何对象类型</div>
              </div>
              <div class="id-row id-row-top">
                <span class="id-lbl">RID</span>
                <span class="id-val bl-mono bl-muted" :title="selected.rid">{{ selected.rid }}</span>
                <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="复制 RID" @click="copyText(selected.rid)" v-html="BL.icon('copy', 12)"></button>
              </div>
            </div>

            <!-- 分组：显示 -->
            <div v-if="tab==='display' && selected.categoryType === 3">
              <div class="display-section">
                <div class="display-label">分组颜色</div>
                <div class="color-row">
                  <span v-for="c in colorChoices" :key="c"
                        :class="['color-dot', editForm.color===c && 'is-active']"
                        :style="{ background: c }"
                        @click="editForm.color = c"></span>
                </div>
                <div class="hex-picker-group" style="margin-top:10px">
                  <input class="bl-input bl-mono hex-input" v-model="editForm.color" placeholder="#RRGGBB" />
                  <input type="color" class="color-picker color-picker-attached" v-model="editForm.color" title="自定义颜色" />
                </div>
              </div>
              <div class="display-section">
                <div class="display-label-row">
                  <span class="display-label">图标</span>
                  <a class="more-link" @click="openIconPicker(false)">更多选择</a>
                </div>
                <div class="icon-grid">
                  <span v-for="ic in iconPresets" :key="ic"
                        :class="['icon-cell', editForm.icon===ic && 'is-active']"
                        @click="editForm.icon = ic"
                        v-html="BL.icon(ic, 16)"></span>
                </div>
              </div>
              <div class="display-section">
                <div class="display-label">描述</div>
                <textarea class="bl-textarea" v-model="editForm.description" rows="3" placeholder="分组用途说明…"></textarea>
              </div>
              <div class="display-section">
                <div class="display-label">在导航栏显示</div>
                <label class="grp-check"><input type="checkbox" v-model="displayOpts.showInNav" /> 在左侧导航展示此分组</label>
                <label class="grp-check"><input type="checkbox" v-model="displayOpts.favorite" /> 设为收藏分组</label>
                <label class="grp-check"><input type="checkbox" v-model="displayOpts.privateOnly" /> 仅自己可见</label>
              </div>
              <div class="display-actions">
                <button class="bl-btn bl-btn-primary" @click="saveBasic">保存</button>
              </div>
              <div class="id-row id-row-top">
                <span class="id-lbl">RID</span>
                <span class="id-val bl-mono bl-muted" :title="selected.rid">{{ selected.rid }}</span>
                <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="复制 RID" @click="copyText(selected.rid)" v-html="BL.icon('copy', 12)"></button>
              </div>
            </div>

            <!-- 样式 -->
            <div v-if="tab==='style'">
              <div class="fr fr-icon">
                <div class="fr-label-row">
                  <span class="fr-label">图标</span>
                  <a class="more-link" @click="openIconPicker(true)">更多选择</a>
                </div>
                <div class="icon-grid">
                  <span v-for="ic in iconPresets" :key="ic"
                        :class="['icon-cell', editForm.icon===ic && 'is-active']"
                        @click="editForm.icon = ic; saveBasic()"
                        v-html="BL.icon(ic, 16)"></span>
                </div>
              </div>
              <FieldRow label="颜色">
                <div class="color-stack">
                  <div class="color-row">
                    <span v-for="c in colorChoices" :key="c"
                          :class="['color-dot', editForm.color===c && 'is-active']"
                          :style="{ background: c }"
                          @click="editForm.color = c; saveBasic()"></span>
                  </div>
                  <div class="hex-picker-group">
                    <input class="bl-input bl-mono hex-input" v-model="editForm.color" @change="saveBasic" placeholder="#RRGGBB" />
                    <input type="color" class="color-picker color-picker-attached" v-model="editForm.color" @change="saveBasic" title="自定义颜色" />
                  </div>
                </div>
              </FieldRow>
              <FieldRow label="预览">
                <div class="bl-row" style="gap:10px">
                  <div class="bl-icon-block" :style="{ background: editForm.color || '#86909C' }" v-html="BL.icon(editForm.icon || 'folder', 18, '#fff')"></div>
                  <span>{{ editForm.rdfsLabel || selected.categoryCode }}</span>
                </div>
              </FieldRow>
            </div>
          </div>
        </template>
      </section>
    </div>

    <!-- 创建/编辑抽屉 -->
    <div v-if="formOpen" class="bl-drawer-mask" @click.self="formOpen=false">
      <aside class="bl-drawer">
        <div class="bl-drawer-hd">
          <span>{{ formMode==='create' ? '新建' : '编辑' }}{{ catTypeText(formData.categoryType) }}</span>
          <button class="bl-btn bl-btn-text bl-btn-icon" @click="formOpen=false" v-html="BL.icon('x', 14)"></button>
        </div>
        <div class="bl-drawer-body bl-col" style="gap:12px">
          <FieldRow label="类型">
            <select class="bl-input" v-model="formData.categoryType" :disabled="formMode==='edit'">
              <option :value="1">行业 (Industry)</option>
              <option :value="2">领域 (Domain)</option>
              <option :value="3">分组 (Group)</option>
            </select>
          </FieldRow>
          <FieldRow label="父级"><input class="bl-input" :value="formParentLabel" disabled /></FieldRow>
          <FieldRow label="编码 *"><input class="bl-input bl-mono" v-model="formData.categoryCode" placeholder="小写+下划线" /></FieldRow>
          <FieldRow label="中文名 (rdfs:label) *"><input class="bl-input" v-model="formData.rdfsLabel" /></FieldRow>
          <FieldRow label="命名空间编码">
            <select class="bl-input" v-model="formData.nsCode" :disabled="formMode==='edit' && formData.categoryType===2">
              <option value="">— 无 —</option>
              <option v-for="ns in nsList" :key="ns.nsCode" :value="ns.nsCode">{{ ns.nsCode }} · {{ ns.nsName }}</option>
            </select>
          </FieldRow>
          <FieldRow label="图标">
            <select class="bl-input" v-model="formData.icon">
              <option v-for="ic in iconChoices" :key="ic" :value="ic">{{ ic }}</option>
            </select>
          </FieldRow>
          <FieldRow label="颜色">
            <div class="color-row">
              <span v-for="c in colorChoices" :key="c"
                    :class="['color-dot', formData.color===c && 'is-active']"
                    :style="{ background: c }" @click="formData.color = c"></span>
            </div>
          </FieldRow>
          <FieldRow label="说明"><textarea class="bl-textarea" v-model="formData.description" rows="3"></textarea></FieldRow>
        </div>
        <div class="bl-drawer-ft">
          <button class="bl-btn" @click="formOpen=false">取消</button>
          <button class="bl-btn bl-btn-primary" @click="submitForm">保存</button>
        </div>
      </aside>
    </div>

    <!-- 添加分组成员弹窗 -->
    <div v-if="memberPickerOpen" class="bl-modal-mask" @click.self="memberPickerOpen=false">
      <div class="bl-modal mp-modal">
        <div class="bl-modal-hd">
          <span>添加分组成员</span>
        </div>
        <div class="mp-body">
          <div class="mp-search">
            <span class="search-icon" v-html="BL.icon('search', 12)"></span>
            <input class="bl-input" placeholder="搜索对象类型 / api_name" v-model="memberPickerQ" />
            <span class="bl-muted" style="font-size:12px; margin-left:8px">已选 {{ memberDraft.size }}</span>
          </div>
          <div class="mp-list">
            <label v-for="c in memberPickerList" :key="c.id"
                   :class="['mp-row', memberDraft.has(c.id) && 'is-on']">
              <input type="checkbox" :checked="memberDraft.has(c.id)" @change="toggleMemberDraft(c.id)" />
              <div class="bl-icon-block bl-icon-block-sm" :style="{ background: c.color || '#165DFF' }" v-html="BL.icon(c.icon || 'cube', 12, '#fff')"></div>
              <div class="bl-grow">
                <div>{{ c.display_name || c.api_name }}</div>
                <div class="bl-mono bl-muted" style="font-size:11px">{{ c.api_name }} · {{ c.ns_code }}</div>
              </div>
            </label>
            <div v-if="!memberPickerList.length" class="bl-empty" style="padding:24px">暂无可添加的对象类型</div>
          </div>
        </div>
        <div class="bl-modal-ft">
          <button class="bl-btn" @click="memberPickerOpen=false">取消</button>
          <button class="bl-btn bl-btn-primary" :disabled="!memberDraft.size" @click="submitMembers">添加 ({{ memberDraft.size }})</button>
        </div>
      </div>
    </div>

    <!-- 图标全库选择 -->
    <div v-if="iconPickerOpen" class="bl-modal-mask" @click.self="cancelIconPick">
      <div class="bl-modal icon-picker-modal">
        <div class="bl-modal-hd ip-hd">
          <span>图标选择</span>
          <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="关闭" @click="cancelIconPick" v-html="BL.icon('x', 14)"></button>
        </div>
        <div class="bl-modal-body ip-body">
          <aside class="ip-side">
            <!-- 我的编组（自定义，置顶） -->
            <div class="ip-side-section">
              <div class="ip-side-head" @click="customExpanded = !customExpanded">
                <span class="ip-side-head-chev" :class="customExpanded && 'is-open'" v-html="BL.icon('chevronRight', 11)"></span>
                <span class="ip-side-head-title">我的图库</span>
                <span class="ip-side-head-count">{{ customCategories.length }}</span>
                <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="新建编组"
                        @click.stop="createCustomGroup" v-html="BL.icon('plus', 12)"></button>
              </div>
              <div class="ip-side-list" v-show="customExpanded">
                <div v-for="cat in customCategories" :key="cat.key"
                     :class="['ip-cat ip-cat-custom', iconCat===cat.key && 'is-active']"
                     @click="iconCat = cat.key">
                  <template v-if="renameCustomKey === cat.rawKey">
                    <input ref="renameInputRef"
                           class="bl-input ip-cat-rename"
                           v-model="renameCustomValue"
                           @keydown.enter="commitRenameCustomGroup"
                           @keydown.esc="cancelRenameCustomGroup"
                           @blur="commitRenameCustomGroup"
                           @click.stop />
                  </template>
                  <template v-else>
                    <span class="ip-cat-label">{{ cat.label }}</span>
                    <span class="ip-cat-actions">
                      <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="重命名" @click.stop="startRenameCustomGroup(cat)" v-html="BL.icon('edit', 11)"></button>
                      <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="删除编组" @click.stop="removeCustomGroup(cat)" v-html="BL.icon('trash', 11)"></button>
                    </span>
                    <span class="ip-cat-count">{{ cat.icons.length }}</span>
                  </template>
                </div>
                <div v-if="!customCategories.length" class="ip-side-empty">尚无自定义编组</div>
              </div>
            </div>
            <!-- 内置图标库 -->
            <div class="ip-side-section">
              <div class="ip-side-head" @click="builtinExpanded = !builtinExpanded">
                <span class="ip-side-head-chev" :class="builtinExpanded && 'is-open'" v-html="BL.icon('chevronRight', 11)"></span>
                <span class="ip-side-head-title">内置图标库</span>
                <span class="ip-side-head-count">{{ builtinCategories.length }}</span>
              </div>
              <div class="ip-side-list" v-show="builtinExpanded">
                <div v-for="cat in builtinCategories" :key="cat.key"
                     :class="['ip-cat', iconCat===cat.key && 'is-active']"
                     @click="iconCat = cat.key">
                  <span>{{ cat.label }}</span>
                  <span class="ip-cat-count">{{ cat.icons.length }}</span>
                </div>
              </div>
            </div>
          </aside>
          <div class="ip-main">
            <div class="ip-toolbar">
              <div class="ip-crumb">
                <span class="bl-muted">图标</span>
                <span class="crumb-sep" v-html="BL.icon('chevronRight', 11)"></span>
                <span>{{ currentCatLabel }}</span>
              </div>
              <div class="ip-toolbar-right">
                <template v-if="isCustomCat">
                  <input ref="uploadInputRef" type="file" accept=".svg,image/svg+xml" multiple style="display:none" @change="onSvgFiles" />
                  <template v-if="!batchMode">
                    <button :class="['bl-btn bl-btn-sm', batchMode && 'bl-btn-primary']" @click="toggleBatchMode" :disabled="!pickerIcons.length" title="批量操作">
                      <span v-html="BL.icon('check', 12)"></span>
                      <span style="margin-left:4px">批量操作</span>
                    </button>
                    <button class="bl-btn bl-btn-primary bl-btn-sm" @click="triggerUpload">
                      <span v-html="BL.icon('upload', 12, '#fff')"></span>
                      <span style="margin-left:4px">上传 SVG</span>
                    </button>
                  </template>
                  <template v-else>
                    <span class="ip-batch-info">已选 <b>{{ batchSelected.size }}</b> / {{ pickerIcons.length }}</span>
                    <button class="bl-btn bl-btn-sm" @click="selectAllBatch">全选</button>
                    <button class="bl-btn bl-btn-sm" :disabled="!batchSelected.size" @click="clearBatchSelect">取消选择</button>
                    <button class="bl-btn bl-btn-sm bl-btn-danger" :disabled="!batchSelected.size" @click="batchDeleteIcons">
                      <span v-html="BL.icon('trash', 12, '#fff')"></span>
                      <span style="margin-left:4px">删除 ({{ batchSelected.size }})</span>
                    </button>
                    <button class="bl-btn bl-btn-sm" @click="toggleBatchMode">退出批量</button>
                  </template>
                </template>
                <div class="ip-search">
                  <span class="ip-search-ic" v-html="BL.icon('search', 12)"></span>
                  <input class="bl-input ip-search-input" v-model="iconQ" placeholder="搜索图标名称…" />
                </div>
              </div>
            </div>
            <div class="ip-grid" v-if="pickerIcons.length">
              <span v-for="ic in pickerIcons" :key="ic"
                    :class="[
                      'icon-cell ip-cell',
                      !batchMode && iconStaged===ic && 'is-active',
                      batchMode && batchSelected.has(ic) && 'is-batch-on'
                    ]"
                    :title="shortIconName(ic)"
                    @click="batchMode ? toggleBatchSelect(ic) : (iconStaged = ic)"
                    @dblclick="!batchMode && (iconStaged = ic, confirmIconPick())">
                <span v-html="BL.icon(ic, 20)"></span>
                <span v-if="batchMode" class="ip-cell-check"
                      :class="batchSelected.has(ic) && 'is-on'"
                      v-html="batchSelected.has(ic) ? BL.icon('check', 10, '#fff') : ''"></span>
                <button v-else-if="isCustomCat" class="ip-cell-x" title="移除"
                        @click.stop="removeCustomIcon(ic)"
                        v-html="BL.icon('x', 10)"></button>
              </span>
            </div>
            <div v-else-if="isCustomCat" class="bl-empty" style="padding:48px 0">
              <div class="bl-empty-icon" v-html="BL.icon('folder', 32)"></div>
              点击右上「上传 SVG」批量添加图标<br/>
              <span class="bl-muted" style="font-size:12px">支持阿里字库 / Iconfont 导出的单色 SVG</span>
            </div>
            <div v-else class="bl-empty" style="padding:48px 0">未找到匹配的图标</div>
          </div>
        </div>
        <div class="bl-modal-ft">
          <button class="bl-btn" @click="cancelIconPick">取消</button>
          <button class="bl-btn bl-btn-primary" :disabled="!iconStaged" @click="confirmIconPick">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, reactive, nextTick } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import TreeNode from './category/TreeNode.vue'
import FieldRow from './category/FieldRow.vue'
import GroupGraph from './category/GroupGraph.vue'
import { nodeProfile } from '@/lib/domain.js'
import { BL, antdIconNames, getCustomIconData, setCustomIconData, onCustomIconsChange } from '@/lib/bl.js'
import { categoryApi, namespaceApi, groupApi, resourceApi } from '@/api'

const tree = ref([])
const treeQ = ref('')
const treeSearchOpen = ref(false)
const treeSearchInputRef = ref(null)
const viewMode = ref('tree')

async function toggleTreeSearch() {
  treeSearchOpen.value = !treeSearchOpen.value
  if (treeSearchOpen.value) {
    await nextTick()
    treeSearchInputRef.value?.focus()
  } else {
    treeQ.value = ''
  }
}
function closeTreeSearch() {
  treeSearchOpen.value = false
  treeQ.value = ''
}
const expandedSet = ref(new Set())
const selected = ref(null)
const tab = ref('basic')

const versions = ref([])
const nsInfo = ref(null)
const nsList = ref([])
const nsMetadata = ref('')
const stats = ref({})
const boundClasses = ref([])
const childStatsMap = ref({})

/* —— 默认总览 —— */
const discoverStats = ref({})
const industryStatsMap = ref({})
const industries = computed(() => tree.value.filter(n => n.categoryType === 1))

const globalStats = computed(() => {
  const s = discoverStats.value || {}
  return [
    { key:'industry',  label:'行业',   value: s.industry   ?? 0, icon: 'industry', color: '#165DFF' },
    { key:'domain',    label:'领域',   value: s.domain     ?? 0, icon: 'folder',   color: '#00B42A' },
    { key:'class',     label:'对象类型', value: s.ontClass   ?? 0, icon: 'cube',     color: '#722ED1' },
    { key:'link',      label:'关系',    value: s.ontLink    ?? 0, icon: 'link',     color: '#FF7D00' },
    { key:'action',    label:'动作',    value: s.ontAction  ?? 0, icon: 'zap',      color: '#EB2F96' },
    { key:'interface', label:'接口',    value: s.ontInterface ?? 0, icon: 'station', color: '#13C2C2' },
    { key:'property',  label:'属性',    value: s.ontProperty ?? 0, icon: 'sliders', color: '#FADB14' }
  ]
})

function barH(ind) {
  const counts = industries.value.map(i => industryStatsMap.value[i.id]?.classCount || 0)
  const max = Math.max(...counts, 1)
  const v = industryStatsMap.value[ind.id]?.classCount || 0
  return Math.round((v / max) * 160) + 4
}

const editForm = reactive({})
const displayOpts = reactive({ showInNav: true, favorite: false, privateOnly: false })

/* —— 分组成员管理 —— */
const memberPickerOpen = ref(false)
const memberPickerQ = ref('')
const allClasses = ref([])
const memberDraft = ref(new Set())   // 临时选中的 class_id 集合
let dragFromIdx = null

const memberPickerList = computed(() => {
  const boundIds = new Set(boundClasses.value.map(c => c.id))
  const k = memberPickerQ.value.trim().toLowerCase()
  return allClasses.value
    .filter(c => !boundIds.has(c.id))
    .filter(c => !k || [c.display_name, c.api_name, c.ns_code].filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
})

async function addMember() {
  if (!selected.value || selected.value.categoryType !== 3) return
  // 加载全部对象作为可选源
  if (!allClasses.value.length) {
    const { resourceApi } = await import('@/api')
    allClasses.value = await resourceApi.classes().catch(() => [])
  }
  memberDraft.value = new Set()
  memberPickerQ.value = ''
  memberPickerOpen.value = true
}

async function submitMembers() {
  if (!memberDraft.value.size) { memberPickerOpen.value = false; return }
  const ids = [...memberDraft.value]
  for (const cid of ids) {
    await categoryApi.addMember(selected.value.id, cid)
  }
  BL.success(`已添加 ${ids.length} 个对象`)
  memberPickerOpen.value = false
  await loadStats()
  loadBoundClasses()
}

async function removeMember(c) {
  const ok = await BL.confirm({
    title: '移除成员',
    content: `从「${selected.value.label}」分组中移除「${c.display_name || c.api_name}」？`,
    danger: true, okText: '移除'
  })
  if (!ok) return
  await categoryApi.removeMember(selected.value.id, c.id)
  BL.success('已移除')
  await loadStats()
  loadBoundClasses()
}

/* —— 拖拽排序 —— */
function onDragStart(idx) { dragFromIdx = idx }
function onDragOver(e) { e.preventDefault() }
async function onDrop(targetIdx) {
  if (dragFromIdx === null || dragFromIdx === targetIdx) return
  const arr = [...boundClasses.value]
  const [moved] = arr.splice(dragFromIdx, 1)
  arr.splice(targetIdx, 0, moved)
  boundClasses.value = arr
  dragFromIdx = null
  // 同步到后端
  await categoryApi.reorderMembers(selected.value.id, arr.map(c => c.id))
  BL.success('已重新排序')
}
function toggleMemberDraft(id) {
  const s = new Set(memberDraft.value)
  s.has(id) ? s.delete(id) : s.add(id)
  memberDraft.value = s
}

function iconText(ic, txt) { return `${BL.icon(ic, 12)}<span style="margin-left:4px">${txt}</span>` }

function catTypeText(t) {
  return t === 1 ? '行业' : t === 2 ? '领域' : t === 3 ? '分组' : '—'
}
function shortDate(s) { return s ? String(s).slice(0, 16).replace('T',' ') : '—' }

const detailTabs = computed(() => {
  if (!selected.value) return []
  const t = selected.value.categoryType
  if (t === 3) {
    return [{ key: 'members', label: '成员' }, { key: 'display', label: '样式' }]
  }
  const base = [{ key: 'basic', label: '基础信息' }, { key: 'style', label: '样式' }]
  if (t === 2) base.splice(1, 0, { key: 'namespace', label: '命名空间' })
  return base
})

const iconChoices = [
  // 通用动作
  'search','plus','minus','edit','trash','more','moreV','check','x','copy','save','refresh','scissors',
  // 导航
  'chevronRight','chevronLeft','chevronUp','chevronDown',
  'arrowUp','arrowDown','arrowLeft','arrowRight','back','forward','externalLink','home','menu','navigation',
  // 文件
  'folder','folderOpen','folderPlus','file','fileText','fileCode','clipboard','book','bookmark','archive','inbox',
  // 通讯
  'mail','phone','message','chat','send','atSign',
  // 媒体
  'image','video','camera','microphone','music','volume','volumeOff','play','pause','stop','headphones','speaker',
  // 时间
  'clock','calendar','timer','history','hourglass',
  // 状态
  'info','alert','success','error','help','lock','unlock','eye','eyeOff',
  // 数据
  'database','list','grid','cube','box','package','layers','network','link','share','branch','workflow','hash','tag',
  // 统计
  'chart','barChart','pieChart','trendingUp','trendingDown','activity','dashboard',
  // 人员
  'user','users','userPlus','userCheck','userX','team',
  // 系统
  'bell','cog','settings','sliders','tool','filter','power','logout','login',
  // 主题
  'sun','moon','maximize','minimize',
  // 标记
  'star','heart','flag','award','gift','ribbon',
  // 位置
  'globe','map','pin','compass',
  // 天气/能源
  'cloud','rain','snowflake','umbrella','thermometer','flame','zap','battery','wifi','bluetooth',
  // 水利
  'wave','droplet','dam','station','ship',
  // 生态
  'leaf','tree','wheat',
  // 工业
  'industry','factory','building','warehouse','briefcase','shield',
  // 交通
  'car','truck','bus','plane','bike','rocket',
  // 金融
  'dollar','creditCard','wallet','percent',
  // 设备
  'monitor','smartphone','tablet','laptop','server','printer','mouse','keyboard',
  // 布局
  'layout','columns','sidebar','rows',
  // 代码
  'code','terminal',
  // AI
  'ai'
]

const ICON_KEYWORDS = {
  droplet:    ['水','液','雨','湿'],
  wave:       ['浪','波','河','海','流'],
  dam:        ['坝','闸','堤','蓄'],
  station:    ['站','测','监测'],
  ship:       ['船','航','水运'],
  rain:       ['雨','降水'],
  umbrella:   ['伞','防雨'],
  thermometer:['温度','测温'],
  cloud:      ['云','气候','天气'],
  snowflake:  ['雪','冷'],
  factory:    ['工厂','工业','制造'],
  industry:   ['行业','产业'],
  building:   ['建筑','大厦','楼'],
  warehouse:  ['仓库','仓储'],
  briefcase:  ['商务','公文包','业务'],
  tree:       ['林','树','木','保持'],
  leaf:       ['叶','植','绿','生态','环境','保护'],
  wheat:      ['农','田','麦','作物','灌溉'],
  flame:      ['火','燃','焰','热'],
  database:   ['库','数据','存储','data'],
  network:    ['网','互联','拓扑','关系'],
  link:       ['连','联','链','接','关联'],
  share:      ['共享','分享'],
  cube:       ['对象','实体','体','块'],
  box:        ['盒','包装'],
  package:    ['包','打包','包裹'],
  branch:     ['分支','层','结构'],
  workflow:   ['工作流','流程'],
  zap:        ['动作','执行','能','闪','电'],
  cog:        ['配置','设置','参数','工程'],
  settings:   ['设置'],
  sliders:    ['调节','开关'],
  tool:       ['工具'],
  filter:     ['过滤','筛选'],
  shield:     ['防','安全','防汛','防洪','盾'],
  car:        ['车','交通','运输'],
  truck:      ['卡车','货车','物流'],
  bus:        ['公交','客车'],
  plane:      ['飞机','航空'],
  bike:       ['自行车','单车'],
  rocket:     ['火箭','发射'],
  user:       ['用户','人员','人'],
  users:      ['用户组','成员','团队'],
  team:       ['团队','小组'],
  bell:       ['通知','提醒','告警'],
  ai:         ['智能','函数','算法'],
  folder:     ['目录','文件夹','分类','类别'],
  file:       ['文件'],
  fileText:   ['文档','文本'],
  fileCode:   ['代码文件','脚本'],
  clipboard:  ['剪贴板'],
  book:       ['书','手册','说明'],
  bookmark:   ['书签','收藏'],
  archive:    ['归档','档案'],
  inbox:      ['收件箱','收件'],
  mail:       ['邮件','邮箱','信件'],
  phone:      ['电话','通话'],
  message:    ['消息','信息'],
  chat:       ['聊天','对话'],
  send:       ['发送','寄出'],
  image:      ['图片','图像','照片'],
  video:      ['视频','影片'],
  camera:     ['相机','摄像','拍照'],
  microphone: ['麦克风','话筒','录音'],
  music:      ['音乐','歌曲'],
  volume:     ['音量','声音'],
  play:       ['播放','开始'],
  pause:      ['暂停'],
  stop:       ['停止'],
  clock:      ['时间','时钟'],
  calendar:   ['日历','日期','计划'],
  timer:      ['计时','定时'],
  history:    ['历史','记录'],
  hourglass:  ['沙漏','等待'],
  chart:      ['折线','图表'],
  barChart:   ['柱状','条形'],
  pieChart:   ['饼图','分布'],
  trendingUp: ['上升','增长','趋势'],
  trendingDown:['下降','下跌'],
  activity:   ['活动','活跃','心电'],
  dashboard:  ['仪表盘','看板','总览'],
  grid:       ['面板','矩阵','网格'],
  list:       ['列表','清单'],
  layers:     ['层','图层'],
  tag:        ['标签','标记'],
  hash:       ['编号','井号'],
  star:       ['星','收藏','重要'],
  heart:      ['心','喜欢'],
  flag:       ['标志','旗','里程碑'],
  award:      ['奖','证书','勋章'],
  gift:       ['礼物','奖励'],
  globe:      ['全球','世界','国际'],
  map:        ['地图'],
  pin:        ['定位','位置'],
  compass:    ['指南针','方向'],
  navigation: ['导航'],
  monitor:    ['显示器','屏幕'],
  smartphone: ['手机','移动'],
  tablet:     ['平板'],
  laptop:     ['笔记本','电脑'],
  server:     ['服务器','服务端'],
  printer:    ['打印机'],
  mouse:      ['鼠标'],
  keyboard:   ['键盘'],
  wifi:       ['无线','网络'],
  bluetooth:  ['蓝牙'],
  battery:    ['电池','电量'],
  power:      ['电源','开关'],
  code:       ['代码'],
  terminal:   ['终端','命令行'],
  dollar:     ['美元','金额','货币'],
  creditCard: ['信用卡','银行卡'],
  wallet:     ['钱包','余额'],
  percent:    ['百分比','比率'],
  upload:     ['上传','发布'],
  download:   ['下载','导出'],
  copy:       ['复制','副本'],
  search:     ['查询','搜索','查找'],
  edit:       ['编辑','修改'],
  trash:      ['删除','回收站'],
  save:       ['保存','存储'],
  refresh:    ['刷新','重载'],
  lock:       ['锁','锁定','加密'],
  unlock:     ['解锁'],
  eye:        ['查看','显示'],
  eyeOff:     ['隐藏'],
  info:       ['信息','提示'],
  alert:      ['警告','告警'],
  success:    ['成功','完成'],
  error:      ['错误','失败'],
  help:       ['帮助','疑问'],
  home:       ['首页','主页'],
  menu:       ['菜单'],
  layout:     ['布局','版式'],
  sidebar:    ['侧边栏'],
  maximize:   ['最大化','放大'],
  minimize:   ['最小化','缩小'],
  sun:        ['太阳','白天','晴'],
  moon:       ['月亮','夜','暗'],

  /* —— Ant Design (阿里图标) 关键词补充 —— */
  'antd:database':         ['库','数据','存储'],
  'antd:hdd':              ['硬盘','存储'],
  'antd:container':        ['容器','分组'],
  'antd:cluster':          ['集群','分布','节点'],
  'antd:partition':        ['分区','切片'],
  'antd:apartment':        ['组织','结构','层级','部门'],
  'antd:deployment-unit':  ['部署','单元','服务'],
  'antd:share-alt':        ['共享','分享','关联'],
  'antd:node-collapse':    ['节点','折叠'],
  'antd:node-expand':      ['节点','展开'],
  'antd:node-index':       ['节点','索引'],
  'antd:sisternode':       ['兄弟','节点'],
  'antd:subnode':          ['子节点','子'],
  'antd:branches':         ['分支','派生','结构'],
  'antd:fork':             ['分叉','派生','复刻'],
  'antd:pull-request':     ['合并','请求'],
  'antd:api':              ['接口','API'],
  'antd:function':         ['函数','算法'],
  'antd:experiment':       ['实验','化学','测试','研究'],
  'antd:robot':            ['机器人','智能','AI'],
  'antd:open-ai':          ['OpenAI','智能','AI'],
  'antd:console-sql':      ['SQL','控制台','查询'],
  'antd:code':             ['代码','编码'],
  'antd:code-sandbox':     ['沙箱','代码'],
  'antd:build':            ['构建','建造'],
  'antd:bug':              ['缺陷','错误','Bug'],
  'antd:bulb':             ['灯泡','灵感','想法','点子'],
  'antd:aim':              ['瞄准','目标','靶'],
  'antd:radar-chart':      ['雷达','分布','统计'],
  'antd:heat-map':         ['热力','分布','热点'],
  'antd:area-chart':       ['面积','图表','分布'],
  'antd:bar-chart':        ['柱状','统计','条形'],
  'antd:line-chart':       ['折线','趋势','统计'],
  'antd:pie-chart':        ['饼图','占比','统计'],
  'antd:fund':             ['基金','资金'],
  'antd:fund-projection-screen': ['看板','投影','大屏'],
  'antd:funnel-plot':      ['漏斗','转化'],
  'antd:dashboard':        ['仪表盘','看板','大屏'],
  'antd:dollar':           ['美元','金额','货币'],
  'antd:bank':             ['银行','金融'],
  'antd:gold':             ['黄金','宝藏'],
  'antd:wallet':           ['钱包','余额'],
  'antd:transaction':      ['交易','流转'],
  'antd:shop':             ['店铺','商店'],
  'antd:shopping-cart':    ['购物车','订单'],
  'antd:gift':             ['礼物','奖励','红包'],
  'antd:trophy':           ['奖杯','荣誉','成就'],
  'antd:crown':            ['皇冠','VIP','顶级'],
  'antd:fire':             ['火','热门','燃烧'],
  'antd:rocket':           ['火箭','启动','加速'],
  'antd:thunderbolt':      ['闪电','能源','电力'],
  'antd:cloud':            ['云','云端'],
  'antd:cloud-server':     ['云服务器','服务'],
  'antd:cloud-upload':     ['云上传','上传'],
  'antd:cloud-download':   ['云下载','下载'],
  'antd:cloud-sync':       ['云同步','同步'],
  'antd:safety':           ['安全','防护'],
  'antd:safety-certificate':['证书','安全','认证'],
  'antd:security-scan':    ['安全扫描','检测'],
  'antd:property-safety':  ['财产','安全'],
  'antd:audit':            ['审计','审核'],
  'antd:verified':         ['认证','已验证','勾选'],
  'antd:lock':             ['锁','锁定','保密'],
  'antd:unlock':           ['解锁','开放'],
  'antd:key':              ['密钥','钥匙','凭证'],
  'antd:scan':             ['扫描','二维码'],
  'antd:qrcode':           ['二维码','码'],
  'antd:barcode':          ['条形码','条码'],
  'antd:user':             ['用户','人员','人'],
  'antd:team':             ['团队','成员'],
  'antd:contacts':         ['联系人','通讯录'],
  'antd:customer-service': ['客服','服务'],
  'antd:idcard':           ['证件','身份'],
  'antd:man':              ['男','男性'],
  'antd:woman':            ['女','女性'],
  'antd:mail':             ['邮件','信件'],
  'antd:phone':            ['电话','通话'],
  'antd:message':          ['消息','私信'],
  'antd:comment':          ['评论','留言'],
  'antd:notification':     ['通知','提醒'],
  'antd:send':             ['发送','寄出'],
  'antd:wechat':           ['微信'],
  'antd:dingding':         ['钉钉'],
  'antd:dingtalk':         ['钉钉'],
  'antd:weibo':            ['微博'],
  'antd:qq':               ['QQ'],
  'antd:taobao':           ['淘宝'],
  'antd:alipay':           ['支付宝'],
  'antd:aliyun':           ['阿里云'],
  'antd:alibaba':          ['阿里','阿里巴巴'],
  'antd:tags':             ['标签','分类'],
  'antd:tag':              ['标签','标记'],
  'antd:flag':             ['标志','旗','里程碑'],
  'antd:pushpin':          ['图钉','定位'],
  'antd:environment':      ['环境','位置','地点'],
  'antd:global':           ['全球','地球','国际'],
  'antd:compass':          ['指南针','方向'],
  'antd:gateway':          ['网关','入口'],
  'antd:home':             ['首页','主页'],
  'antd:calendar':         ['日历','日期','排期'],
  'antd:clock-circle':     ['时钟','时间'],
  'antd:field-time':       ['时间','时段'],
  'antd:hourglass':        ['沙漏','等待'],
  'antd:schedule':         ['日程','计划','排程'],
  'antd:history':          ['历史','记录'],
  'antd:car':              ['汽车','轿车'],
  'antd:truck':            ['卡车','货运','物流'],
  'antd:tool':             ['工具'],
  'antd:setting':          ['设置','配置'],
  'antd:control':          ['控制','操控'],
  'antd:edit':             ['编辑','修改'],
  'antd:form':             ['表单','单据'],
  'antd:save':             ['保存'],
  'antd:delete':           ['删除'],
  'antd:copy':             ['复制','副本'],
  'antd:scissor':          ['剪刀','裁剪'],
  'antd:snippets':         ['代码段','片段','摘录'],
  'antd:profile':          ['档案','简介'],
  'antd:read':             ['阅读','查阅'],
  'antd:book':             ['书','手册','文档'],
  'antd:file':             ['文件'],
  'antd:file-text':        ['文档','文本'],
  'antd:file-protect':     ['受保护','文件保护'],
  'antd:file-search':      ['查找','文件搜索'],
  'antd:folder':           ['文件夹','目录','分类'],
  'antd:folder-open':      ['打开','文件夹'],
  'antd:inbox':            ['收件箱'],
  'antd:solution':         ['解决方案','工单'],
  'antd:reconciliation':   ['对账','核对'],
  'antd:project':          ['项目','工程'],
  'antd:medicine-box':     ['药箱','医疗'],
  'antd:rest':             ['休息','清零'],
  'antd:moon':             ['月亮','夜'],
  'antd:sun':              ['太阳','白天']
}

function suggestIcons(name) {
  const s = String(name || '').toLowerCase()
  if (!s) return []
  const out = []
  for (const [icon, kws] of Object.entries(ICON_KEYWORDS)) {
    if (kws.some(k => s.includes(k.toLowerCase()))) out.push(icon)
  }
  return out
}

const iconPickerOpen = ref(false)
const iconQ = ref('')
const iconCat = ref('all')
const iconStaged = ref('')
const iconPickerAutoSave = ref(false)

/* ===== Ant Design 图标库（阿里图标）— 由 @ant-design/icons-svg 提供 447 个 outlined 图标 ===== */
const ANTD_OUTLINED = antdIconNames('outlined')
const antd = (n) => 'antd:' + n
const ANTD_ALL = ANTD_OUTLINED.map(antd)
const dedupe = (arr) => Array.from(new Set(arr))

const ICON_CATEGORIES = [
  { key: 'all',      label: '全部',
    icons: dedupe([...iconChoices, ...ANTD_ALL]) },

  { key: 'common',   label: '常用',
    icons: dedupe([
      'folder','grid','database','cube','link','share','user','cog','list','network',
      antd('folder'),antd('folder-open'),antd('database'),antd('appstore'),antd('cluster'),
      antd('apartment'),antd('partition'),antd('block'),antd('api'),antd('schedule'),
      antd('tags'),antd('tag'),antd('appstore-add'),antd('plus'),antd('search'),
      antd('edit'),antd('delete'),antd('save'),antd('copy'),antd('setting'),antd('tool')
    ]) },

  { key: 'ontology', label: '本体/知识图谱',
    icons: dedupe([
      antd('apartment'),antd('partition'),antd('cluster'),antd('deployment-unit'),
      antd('branches'),antd('share-alt'),antd('node-collapse'),antd('node-expand'),
      antd('node-index'),antd('sisternode'),antd('subnode'),antd('fork'),
      antd('pull-request'),antd('block'),antd('database'),antd('container'),
      antd('api'),antd('function'),antd('experiment'),antd('robot'),antd('open-ai'),
      antd('schedule'),antd('build'),antd('bulb'),antd('console-sql'),antd('code'),
      antd('code-sandbox'),antd('aim'),antd('dot-chart'),antd('radar-chart'),
      antd('heat-map'),antd('disconnect'),antd('gateway'),antd('project'),
      'cube','network','branch','link','share','workflow','layers'
    ]) },

  { key: 'water',    label: '水利水务',
    icons: dedupe([
      'droplet','wave','dam','station','ship',
      antd('cloud'),antd('cloud-download'),antd('cloud-upload'),antd('cloud-sync'),
      antd('cloud-server'),antd('thunderbolt'),antd('environment'),antd('compass'),
      antd('global'),antd('aim')
    ]) },

  { key: 'env',      label: '生态环境',
    icons: dedupe([
      'tree','leaf','wheat','shield','sun','moon','cloud','rain','snowflake','flame',
      antd('sun'),antd('moon'),antd('cloud'),antd('fire'),antd('bulb'),
      antd('environment'),antd('global'),antd('safety'),antd('property-safety')
    ]) },

  { key: 'industry', label: '工业工程',
    icons: dedupe([
      'industry','factory','building','warehouse','briefcase','cog','sliders','dam','zap','car','truck',
      antd('build'),antd('tool'),antd('setting'),antd('control'),antd('sliders'),
      antd('thunderbolt'),antd('experiment'),antd('robot'),antd('rocket'),antd('car'),
      antd('truck'),antd('shop'),antd('bank'),antd('gold'),antd('container'),
      antd('deployment-unit'),antd('cluster'),antd('partition'),antd('api'),antd('gateway')
    ]) },

  { key: 'data',     label: '数据/对象',
    icons: dedupe([
      'database','grid','list','cube','branch','network','link','share','sliders','layers','workflow',
      antd('database'),antd('hdd'),antd('container'),antd('appstore'),antd('block'),
      antd('cluster'),antd('partition'),antd('apartment'),antd('deployment-unit'),
      antd('share-alt'),antd('node-collapse'),antd('node-expand'),antd('node-index'),
      antd('sisternode'),antd('subnode'),antd('branches'),antd('fork'),antd('pull-request'),
      antd('api'),antd('function'),antd('console-sql'),antd('code'),antd('code-sandbox'),
      antd('build'),antd('schedule'),antd('table'),antd('borderless-table')
    ]) },

  { key: 'chart',    label: '图表统计',
    icons: dedupe([
      antd('area-chart'),antd('bar-chart'),antd('box-plot'),antd('dot-chart'),
      antd('fund'),antd('fund-projection-screen'),antd('fund-view'),antd('funnel-plot'),
      antd('heat-map'),antd('line-chart'),antd('pie-chart'),antd('radar-chart'),
      antd('rise'),antd('fall'),antd('stock'),antd('sliders'),antd('dashboard'),
      'chart','barChart','pieChart','trendingUp','trendingDown','activity','dashboard'
    ]) },

  { key: 'arrow',    label: '方向/箭头',
    icons: dedupe([
      antd('arrow-up'),antd('arrow-down'),antd('arrow-left'),antd('arrow-right'),
      antd('caret-up'),antd('caret-down'),antd('caret-left'),antd('caret-right'),
      antd('up'),antd('down'),antd('left'),antd('right'),
      antd('up-circle'),antd('down-circle'),antd('left-circle'),antd('right-circle'),
      antd('up-square'),antd('down-square'),antd('left-square'),antd('right-square'),
      antd('double-left'),antd('double-right'),antd('vertical-left'),antd('vertical-right'),
      antd('vertical-align-top'),antd('vertical-align-middle'),antd('vertical-align-bottom'),
      antd('swap'),antd('swap-left'),antd('swap-right'),antd('to-top'),
      antd('rollback'),antd('rotate-left'),antd('rotate-right'),antd('undo'),antd('redo'),
      antd('retweet'),antd('forward'),antd('backward'),antd('enter'),antd('import'),antd('export'),
      antd('rise'),antd('fall'),antd('arrows-alt')
    ]) },

  { key: 'feedback', label: '反馈/状态',
    icons: dedupe([
      antd('check'),antd('close'),antd('plus'),antd('minus'),
      antd('check-circle'),antd('close-circle'),antd('plus-circle'),antd('minus-circle'),
      antd('check-square'),antd('close-square'),antd('plus-square'),antd('minus-square'),
      antd('question'),antd('question-circle'),antd('exclamation'),antd('exclamation-circle'),
      antd('warning'),antd('info'),antd('info-circle'),antd('stop'),antd('dash'),
      antd('loading'),antd('loading3-quarters'),antd('sync'),antd('reload'),antd('poweroff'),
      antd('pause'),antd('pause-circle'),antd('play-circle'),antd('play-square'),
      antd('fire'),antd('bulb'),antd('issues-close'),antd('frown'),antd('smile'),antd('meh')
    ]) },

  { key: 'file',     label: '文件/文档',
    icons: dedupe([
      antd('file'),antd('file-add'),antd('file-done'),antd('file-text'),antd('file-search'),
      antd('file-sync'),antd('file-protect'),antd('file-unknown'),antd('file-exclamation'),
      antd('file-image'),antd('file-gif'),antd('file-jpg'),antd('file-pdf'),antd('file-ppt'),
      antd('file-word'),antd('file-excel'),antd('file-markdown'),antd('file-zip'),
      antd('folder'),antd('folder-open'),antd('folder-add'),antd('folder-view'),
      antd('snippets'),antd('profile'),antd('container'),antd('inbox'),antd('solution'),
      antd('book'),antd('read'),antd('reconciliation'),antd('audit'),antd('form'),
      antd('diff'),antd('paper-clip'),antd('printer'),antd('copy'),antd('delete'),
      antd('save'),antd('cloud-download'),antd('cloud-upload')
    ]) },

  { key: 'edit',     label: '编辑/格式',
    icons: dedupe([
      antd('edit'),antd('form'),antd('clear'),antd('scissor'),antd('snippets'),antd('save'),
      antd('delete'),antd('rest'),antd('undo'),antd('redo'),antd('format-painter'),
      antd('bold'),antd('italic'),antd('underline'),antd('strikethrough'),antd('highlight'),
      antd('font-size'),antd('font-colors'),antd('bg-colors'),antd('line-height'),
      antd('signature'),antd('translation'),antd('select'),antd('drag'),antd('holder'),
      antd('ellipsis'),antd('bars')
    ]) },

  { key: 'table',    label: '表格/布局',
    icons: dedupe([
      antd('table'),antd('borderless-table'),antd('layout'),
      antd('insert-row-above'),antd('insert-row-below'),antd('insert-row-left'),antd('insert-row-right'),
      antd('delete-column'),antd('delete-row'),antd('merge-cells'),antd('merge'),antd('split-cells'),
      antd('column-height'),antd('column-width'),
      antd('border'),antd('border-bottom'),antd('border-top'),antd('border-left'),antd('border-right'),
      antd('border-inner'),antd('border-outer'),antd('border-horizontal'),antd('border-verticle'),
      antd('radius-bottomleft'),antd('radius-bottomright'),antd('radius-upleft'),antd('radius-upright'),
      antd('radius-setting'),
      antd('align-left'),antd('align-center'),antd('align-right'),
      antd('pic-left'),antd('pic-center'),antd('pic-right'),
      antd('menu'),antd('menu-fold'),antd('menu-unfold'),antd('switcher'),antd('ordered-list'),antd('unordered-list')
    ]) },

  { key: 'comm',     label: '通讯/社交',
    icons: dedupe([
      antd('mail'),antd('phone'),antd('message'),antd('comment'),antd('notification'),
      antd('send'),antd('customer-service'),antd('contacts'),antd('team'),antd('group'),antd('ungroup'),
      antd('audio'),antd('audio-muted'),antd('sound'),antd('muted'),
      antd('video-camera'),antd('video-camera-add'),
      antd('wechat'),antd('wechat-work'),antd('dingding'),antd('dingtalk'),antd('whats-app'),
      antd('weibo'),antd('weibo-circle'),antd('weibo-square'),
      antd('qq'),antd('skype'),antd('slack'),antd('slack-square'),antd('discord'),
      antd('twitter'),antd('facebook'),antd('linkedin'),antd('instagram')
    ]) },

  { key: 'user',     label: '用户/人员',
    icons: dedupe([
      'user','users','team',
      antd('user'),antd('user-add'),antd('user-delete'),antd('user-switch'),
      antd('usergroup-add'),antd('usergroup-delete'),antd('team'),antd('contacts'),
      antd('man'),antd('woman'),antd('idcard'),antd('solution'),antd('customer-service'),
      antd('smile'),antd('frown'),antd('meh'),antd('crown'),antd('like'),antd('dislike'),
      antd('robot'),antd('skin')
    ]) },

  { key: 'cloud',    label: '云/网络',
    icons: dedupe([
      antd('cloud'),antd('cloud-download'),antd('cloud-upload'),antd('cloud-server'),antd('cloud-sync'),
      antd('global'),antd('wifi'),antd('api'),antd('cluster'),antd('gateway'),antd('disconnect'),
      antd('hdd'),antd('container'),antd('database'),antd('desktop'),antd('laptop'),
      antd('tablet'),antd('mobile'),antd('monitor'),antd('usb'),antd('printer')
    ]) },

  { key: 'bus',      label: '商业/金融',
    icons: dedupe([
      antd('bank'),antd('credit-card'),antd('dollar'),antd('dollar-circle'),
      antd('euro'),antd('euro-circle'),antd('pound'),antd('pound-circle'),
      antd('property-safety'),antd('money-collect'),antd('pay-circle'),antd('red-envelope'),
      antd('wallet'),antd('transaction'),antd('trademark'),antd('trademark-circle'),
      antd('copyright'),antd('copyright-circle'),antd('insurance'),antd('gold'),
      antd('gift'),antd('shop'),antd('shopping'),antd('shopping-cart'),antd('fund'),
      antd('stock'),antd('rise'),antd('fall'),antd('percentage'),antd('calculator'),
      antd('barcode'),antd('qrcode'),antd('account-book'),antd('audit'),antd('book')
    ]) },

  { key: 'sec',      label: '安全/权限',
    icons: dedupe([
      antd('lock'),antd('unlock'),antd('safety'),antd('safety-certificate'),
      antd('security-scan'),antd('property-safety'),antd('audit'),antd('verified'),
      antd('file-protect'),antd('key'),antd('idcard'),antd('scan'),antd('qrcode'),
      antd('barcode'),antd('eye'),antd('eye-invisible'),antd('shake'),antd('bug')
    ]) },

  { key: 'device',   label: '设备硬件',
    icons: dedupe([
      'monitor','smartphone','tablet','laptop','server','printer','keyboard','mouse',
      antd('desktop'),antd('laptop'),antd('tablet'),antd('mobile'),antd('monitor'),
      antd('printer'),antd('hdd'),antd('usb'),antd('wifi'),antd('camera'),antd('phone'),
      antd('mac-command'),antd('audio'),antd('video-camera')
    ]) },

  { key: 'media',    label: '媒体',
    icons: dedupe([
      'image','video','camera','microphone','music','volume','play','pause','stop',
      antd('picture'),antd('camera'),antd('video-camera'),antd('play-circle'),
      antd('play-square'),antd('pause'),antd('pause-circle'),antd('stop'),
      antd('step-forward'),antd('step-backward'),antd('fast-forward'),antd('fast-backward'),
      antd('sound'),antd('muted'),antd('audio'),antd('audio-muted'),antd('gif')
    ]) },

  { key: 'time',     label: '时间日期',
    icons: dedupe([
      'clock','calendar','timer','history','hourglass',
      antd('calendar'),antd('clock-circle'),antd('field-time'),antd('history'),
      antd('hourglass'),antd('schedule'),antd('carry-out')
    ]) },

  { key: 'award',    label: '奖励/标记',
    icons: dedupe([
      'star','heart','flag','award','gift','bookmark','tag','hash',
      antd('star'),antd('heart'),antd('flag'),antd('trophy'),antd('crown'),antd('gift'),
      antd('like'),antd('dislike'),antd('tag'),antd('tags'),antd('pushpin'),antd('fire')
    ]) },

  { key: 'brand',    label: '品牌Logo',
    icons: dedupe([
      antd('alibaba'),antd('alipay'),antd('alipay-circle'),antd('aliwangwang'),antd('aliyun'),
      antd('ant-design'),antd('ant-cloud'),antd('taobao'),antd('taobao-circle'),
      antd('amazon'),antd('android'),antd('apple'),antd('baidu'),antd('chrome'),
      antd('behance'),antd('behance-square'),antd('bilibili'),antd('codepen'),antd('codepen-circle'),
      antd('code-sandbox'),antd('docker'),antd('dot-net'),antd('dribbble'),antd('dribbble-square'),
      antd('dropbox'),antd('facebook'),antd('github'),antd('gitlab'),antd('google'),antd('google-plus'),
      antd('harmony-os'),antd('html5'),antd('ie'),antd('instagram'),antd('java'),antd('java-script'),
      antd('kubernetes'),antd('linkedin'),antd('linux'),antd('medium'),antd('medium-workmark'),
      antd('open-ai'),antd('pinterest'),antd('python'),antd('reddit'),antd('ruby'),antd('sketch'),
      antd('skype'),antd('slack'),antd('slack-square'),antd('spotify'),antd('tik-tok'),
      antd('twitch'),antd('twitter'),antd('wechat'),antd('wechat-work'),antd('weibo'),
      antd('weibo-circle'),antd('weibo-square'),antd('whats-app'),antd('windows'),
      antd('yahoo'),antd('youtube'),antd('yuque'),antd('zhihu'),antd('mac-command')
    ]) },

  { key: 'action',   label: '常用操作',
    icons: dedupe([
      'edit','plus','trash','copy','search','upload','download','more','check','x','maximize','minimize',
      antd('edit'),antd('plus'),antd('minus'),antd('delete'),antd('copy'),antd('save'),
      antd('search'),antd('zoom-in'),antd('zoom-out'),antd('reload'),antd('sync'),
      antd('upload'),antd('download'),antd('import'),antd('export'),antd('enter'),
      antd('share-alt'),antd('link'),antd('disconnect'),antd('filter'),antd('select'),
      antd('drag'),antd('expand'),antd('expand-alt'),antd('compress'),antd('shrink'),
      antd('fullscreen'),antd('fullscreen-exit'),antd('more'),antd('ellipsis'),
      antd('plus-circle'),antd('minus-circle'),antd('close'),antd('check'),
      antd('login'),antd('logout'),antd('lock'),antd('unlock'),antd('setting'),antd('tool')
    ]) },

  { key: 'antd',     label: 'Ant Design 全集',
    icons: ANTD_ALL }
]

/* ===== 自定义编组（用户上传 SVG）===== */
const customData = ref(getCustomIconData())
let unsubCustom = null

const customCategories = computed(() =>
  customData.value.groups.map(g => ({
    key: 'custom:' + g.key,
    label: g.label,
    rawKey: g.key,
    icons: (g.iconIds || []).map(id => 'custom:' + id),
    custom: true
  }))
)
const builtinCategories = computed(() => ICON_CATEGORIES)
const allCategories = computed(() => [...builtinCategories.value, ...customCategories.value])

const isCustomCat = computed(() => String(iconCat.value).startsWith('custom:'))
const currentCustomGroupKey = computed(() => isCustomCat.value ? iconCat.value.slice(7) : null)

const currentCatLabel = computed(() => allCategories.value.find(c => c.key === iconCat.value)?.label || '全部')

const renameCustomKey = ref(null)
const renameCustomValue = ref('')
const renameInputRef = ref(null)
const uploadInputRef = ref(null)
const customExpanded = ref(true)
const builtinExpanded = ref(true)

function uid() {
  return Math.random().toString(36).slice(2, 8) + Date.now().toString(36).slice(-4)
}
async function createCustomGroup() {
  const label = await BL.prompt({
    title: '新建编组',
    label: '编组名称',
    placeholder: '例如：我的水务图标',
    defaultValue: '我的图标',
    validate: v => !v || !v.trim() ? '名称不能为空' : true
  })
  if (label == null) return
  const d = { ...customData.value, groups: [...customData.value.groups], icons: { ...customData.value.icons } }
  const key = 'g_' + uid()
  d.groups.push({ key, label: label.trim(), iconIds: [] })
  setCustomIconData(d)
  iconCat.value = 'custom:' + key
}
function startRenameCustomGroup(cat) {
  renameCustomKey.value = cat.rawKey
  renameCustomValue.value = cat.label
  nextTick(() => { renameInputRef.value?.focus?.(); renameInputRef.value?.select?.() })
}
function cancelRenameCustomGroup() {
  renameCustomKey.value = null
  renameCustomValue.value = ''
}
function commitRenameCustomGroup() {
  if (!renameCustomKey.value) return
  const nv = renameCustomValue.value.trim()
  if (nv) {
    const d = { ...customData.value, groups: customData.value.groups.map(g =>
      g.key === renameCustomKey.value ? { ...g, label: nv } : g) }
    setCustomIconData(d)
  }
  cancelRenameCustomGroup()
}
async function removeCustomGroup(cat) {
  const ok = await BL.confirm({
    title: '删除编组', danger: true, okText: '删除',
    content: `将删除编组「${cat.label}」及其下 ${cat.icons.length} 个图标？`
  })
  if (!ok) return
  const grp = customData.value.groups.find(g => g.key === cat.rawKey)
  const removeIds = new Set(grp?.iconIds || [])
  const icons = { ...customData.value.icons }
  removeIds.forEach(id => delete icons[id])
  const groups = customData.value.groups.filter(g => g.key !== cat.rawKey)
  setCustomIconData({ groups, icons })
  if (iconCat.value === 'custom:' + cat.rawKey) iconCat.value = 'all'
  BL.success('已删除')
}
function removeCustomIcon(ic) {
  const id = String(ic || '').replace(/^custom:/, '')
  if (!id) return
  const groups = customData.value.groups.map(g => ({
    ...g,
    iconIds: (g.iconIds || []).filter(i => i !== id)
  }))
  const icons = { ...customData.value.icons }
  delete icons[id]
  setCustomIconData({ groups, icons })
  if (iconStaged.value === ic) iconStaged.value = ''
}

/* SVG 解析：抽出 viewBox + 内部内容，将固定颜色替换为 currentColor 以支持染色 */
function parseSvgText(text) {
  if (typeof text !== 'string') return null
  try {
    const doc = new DOMParser().parseFromString(text, 'image/svg+xml')
    const errNode = doc.querySelector('parsererror')
    if (errNode) return null
    const svg = doc.documentElement
    if (!svg || svg.tagName.toLowerCase() !== 'svg') return null
    let viewBox = svg.getAttribute('viewBox')
    if (!viewBox) {
      const w = svg.getAttribute('width'), h = svg.getAttribute('height')
      viewBox = (w && h) ? `0 0 ${parseFloat(w)} ${parseFloat(h)}` : '0 0 1024 1024'
    }
    // 取所有子节点
    const xs = new XMLSerializer()
    let inner = Array.from(svg.childNodes).map(n => {
      if (n.nodeType === 3) return n.nodeValue   // text
      if (n.nodeType !== 1) return ''            // skip comments etc
      return xs.serializeToString(n)
    }).join('').trim()
    if (!inner) return null
    // 将 fill="#xxx"/stroke="#xxx" 统一替换为 currentColor（保留 none）
    inner = inner
      .replace(/\sfill="(?!none)([^"]*)"/gi, ' fill="currentColor"')
      .replace(/\sstroke="(?!none)([^"]*)"/gi, ' stroke="currentColor"')
      .replace(/fill:\s*#[0-9a-f]{3,8}/gi, 'fill: currentColor')
      .replace(/stroke:\s*#[0-9a-f]{3,8}/gi, 'stroke: currentColor')
    return { viewBox, content: inner }
  } catch { return null }
}

function readFileText(file) {
  return new Promise((resolve, reject) => {
    const fr = new FileReader()
    fr.onload = () => resolve(String(fr.result || ''))
    fr.onerror = reject
    fr.readAsText(file)
  })
}

function triggerUpload() {
  if (!isCustomCat.value) {
    BL.warning('请先选择一个自定义编组')
    return
  }
  uploadInputRef.value?.click()
}

/* —— 自定义编组：批量操作 —— */
const batchMode = ref(false)
const batchSelected = ref(new Set())
function toggleBatchMode() {
  batchMode.value = !batchMode.value
  if (!batchMode.value) batchSelected.value = new Set()
}
function toggleBatchSelect(ic) {
  const s = new Set(batchSelected.value)
  s.has(ic) ? s.delete(ic) : s.add(ic)
  batchSelected.value = s
}
function selectAllBatch() {
  batchSelected.value = new Set(pickerIcons.value)
}
function clearBatchSelect() {
  batchSelected.value = new Set()
}
async function batchDeleteIcons() {
  if (!batchSelected.value.size) return
  const ok = await BL.confirm({
    title: '批量删除', danger: true, okText: '删除',
    content: `将从当前编组移除 ${batchSelected.value.size} 个图标，且无法恢复？`
  })
  if (!ok) return
  const removeIds = new Set([...batchSelected.value].map(ic => String(ic).replace(/^custom:/, '')))
  const groups = customData.value.groups.map(g => ({
    ...g,
    iconIds: (g.iconIds || []).filter(i => !removeIds.has(i))
  }))
  const icons = { ...customData.value.icons }
  removeIds.forEach(id => delete icons[id])
  setCustomIconData({ groups, icons })
  if (iconStaged.value && removeIds.has(String(iconStaged.value).replace(/^custom:/, ''))) iconStaged.value = ''
  BL.success(`已删除 ${removeIds.size} 个图标`)
  batchSelected.value = new Set()
}
// 切换分类 / 关闭弹窗：自动退出批量模式
watch(iconCat, () => { batchMode.value = false; batchSelected.value = new Set() })
watch(iconPickerOpen, v => { if (!v) { batchMode.value = false; batchSelected.value = new Set() } })
async function onSvgFiles(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = ''   // 允许重复选同一文件
  if (!files.length) return
  if (!isCustomCat.value) { BL.warning('请先选择自定义编组'); return }
  const groupKey = currentCustomGroupKey.value
  const d = {
    groups: customData.value.groups.map(g => ({ ...g, iconIds: [...(g.iconIds || [])] })),
    icons: { ...customData.value.icons }
  }
  const grp = d.groups.find(g => g.key === groupKey)
  if (!grp) { BL.error('编组不存在'); return }
  let ok = 0, fail = 0
  for (const f of files) {
    if (!/svg/i.test(f.type) && !/\.svg$/i.test(f.name)) { fail++; continue }
    try {
      const text = await readFileText(f)
      const parsed = parseSvgText(text)
      if (!parsed) { fail++; continue }
      const id = 'i_' + uid()
      const name = f.name.replace(/\.svg$/i, '')
      d.icons[id] = { ...parsed, name }
      grp.iconIds.push(id)
      ok++
    } catch { fail++ }
  }
  setCustomIconData(d)
  if (ok) BL.success(`已导入 ${ok} 个图标${fail ? `，${fail} 个失败` : ''}`)
  else BL.error('未能解析任何 SVG 文件')
}

const iconPresets = computed(() => {
  const name = selected.value?.label || selected.value?.categoryCode || ''
  // 用户上传的自定义图标（所有编组）
  const customIds = Object.keys(customData.value.icons || {})
  const customAll = customIds.map(id => 'custom:' + id)
  // 当前已选图标置首
  const current = editForm.icon ? [editForm.icon] : []
  const sugg = suggestIcons(name)
  const seen = new Set()
  const out = []
  for (const ic of [...current, ...customAll, ...sugg, ...iconChoices]) {
    if (!seen.has(ic)) { seen.add(ic); out.push(ic) }
    if (out.length >= 40) break
  }
  return out
})

// 去掉 antd:/custom: 前缀展示更友好的图标名
function shortIconName(ic) {
  const s = String(ic || '')
  if (s.startsWith('custom:')) {
    const id = s.slice(7)
    return customData.value.icons[id]?.name || id
  }
  return s.replace(/^antd:/, '')
}

const pickerIcons = computed(() => {
  const cat = allCategories.value.find(c => c.key === iconCat.value) || allCategories.value[0]
  const list = cat?.icons || []
  const q = iconQ.value.trim().toLowerCase()
  if (!q) return list
  return list.filter(ic => {
    if (ic.toLowerCase().includes(q)) return true
    if (ic.startsWith('custom:')) {
      const name = customData.value.icons[ic.slice(7)]?.name || ''
      if (name.toLowerCase().includes(q)) return true
      return false
    }
    // 中文/拼音关键词命中（来自 ICON_KEYWORDS 字典）
    const kws = ICON_KEYWORDS[ic]
    if (kws && kws.some(k => String(k).toLowerCase().includes(q))) return true
    return false
  })
})

function openIconPicker(autoSave = false) {
  iconStaged.value = editForm.icon || ''
  iconQ.value = ''
  // 优先打开第一个自定义编组；没有则回退到「全部」
  const firstCustom = customCategories.value[0]
  iconCat.value = firstCustom ? firstCustom.key : 'all'
  // 自定义编组在「我的图库」分组里，确保其展开
  if (firstCustom) customExpanded.value = true
  iconPickerAutoSave.value = autoSave
  iconPickerOpen.value = true
}
function cancelIconPick() {
  iconPickerOpen.value = false
}
function confirmIconPick() {
  if (iconStaged.value) {
    editForm.icon = iconStaged.value
    if (iconPickerAutoSave.value) saveBasic()
  }
  iconPickerOpen.value = false
}
const colorChoices = [
  '#165DFF','#00B42A','#722ED1','#FF7D00','#EB2F96','#13C2C2','#FADB14','#F53F3F','#86909C'
]

function flatten(nodes, out = []) {
  for (const n of nodes) {
    out.push(n)
    if (n.children?.length) flatten(n.children, out)
  }
  return out
}

const flatList = computed(() => flatten(tree.value).filter(n => matchQ(n, treeQ.value)))

function matchQ(node, q) {
  if (!q) return true
  const k = q.toLowerCase()
  return [node.label, node.categoryCode, node.nsCode, node.rdfsLabel]
    .filter(Boolean).some(s => String(s).toLowerCase().includes(k))
}

function pruneTree(nodes, q) {
  if (!q) return nodes
  const result = []
  for (const n of nodes) {
    const hit = matchQ(n, q)
    const kids = n.children?.length ? pruneTree(n.children, q) : []
    if (hit || kids.length) result.push({ ...n, children: kids })
  }
  return result
}
const filteredTree = computed(() => pruneTree(tree.value, treeQ.value))

watch(treeQ, (v) => {
  if (v) {
    const all = new Set()
    const visit = (ns) => { for (const n of ns) { all.add(n.id); if (n.children) visit(n.children) } }
    visit(tree.value)
    expandedSet.value = all
  }
})

function onToggle(node) {
  const s = new Set(expandedSet.value)
  s.has(node.id) ? s.delete(node.id) : s.add(node.id)
  expandedSet.value = s
}

async function onSelect(node) {
  selected.value = node
  tab.value = node.categoryType === 3 ? 'members' : 'basic'
  Object.assign(editForm, {
    rdfsLabel: node.rdfsLabel || '',
    rdfsComment: node.rdfsComment || '',
    rdfsSeeAlso: node.rdfsSeeAlso || '',
    rdfsDefinedBy: node.rdfsDefinedBy || '',
    description: node.description || '',
    icon: node.icon || 'folder',
    color: node.color || '#86909C'
  })
  // 分组的展示设置从 metadata JSON 解析
  let m = {}
  try { m = node.metadata ? JSON.parse(node.metadata) : {} } catch { m = {} }
  Object.assign(displayOpts, {
    showInNav: m.showInNav !== false,
    favorite: !!m.favorite,
    privateOnly: !!m.privateOnly
  })
  await Promise.all([loadStats(), loadVersions(), loadNsInfo(), loadChildStats()])
  loadBoundClasses()
}

async function loadChildStats() {
  childStatsMap.value = {}
  const kids = selected.value?.children || []
  if (!kids.length) return
  try {
    const ids = kids.map(c => c.id)
    childStatsMap.value = await categoryApi.statsBatch(ids)
  } catch {}
}

async function loadStats() {
  if (!selected.value) return
  try { stats.value = await categoryApi.stats(selected.value.id) } catch { stats.value = {} }
}
async function loadVersions() {
  versions.value = []
  if (!selected.value || selected.value.categoryType !== 2 || !selected.value.nsCode) return
  try { versions.value = await namespaceApi.versions(selected.value.nsCode) } catch {}
}
async function loadNsInfo() {
  nsInfo.value = null; nsMetadata.value = ''
  if (!selected.value || !selected.value.nsCode) return
  try {
    nsInfo.value = await namespaceApi.get(selected.value.nsCode)
    nsMetadata.value = nsInfo.value?.metadata || ''
  } catch {}
}
function loadBoundClasses() {
  if (!selected.value || selected.value.categoryType !== 3) {
    boundClasses.value = []
    return
  }
  // 分组成员来自 stats 返回的 classChips（已通过 ont_biz_group_class 关联解析）
  boundClasses.value = [...(stats.value?.classChips || [])]
}

function fmt(n) {
  return Number(n ?? 0).toLocaleString('en-US')
}

function readable(n) {
  n = Number(n || 0)
  if (n >= 10000) return (n / 10000).toFixed(1).replace(/\.0$/, '') + '万'
  if (n >= 1000)  return (n / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  return String(n)
}

async function copyText(t) {
  try { await navigator.clipboard.writeText(String(t || '')); BL.success('已复制') }
  catch { BL.warning('复制失败，请手动选取') }
}
function downloadGraph() {
  const svg = document.querySelector('.grp-graph-grow svg')
  if (!svg) { BL.warning('暂无图谱可下载'); return }
  const xml = new XMLSerializer().serializeToString(svg)
  const blob = new Blob(['<?xml version="1.0"?>\n', xml], { type: 'image/svg+xml' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `${selected.value?.label || 'graph'}.svg`
  a.click()
}
// 真实数据来自 statsBatch；缺失时回退 0
function childStat(c) {
  const s = childStatsMap.value[c.id] || {}
  const chips = Array.isArray(s.classChips) ? s.classChips : []
  const cls = Number(s.classCount ?? 0)
  return {
    primary: readable(cls),
    secondary: readable(s.linkCount ?? 0),
    groups: s.childCount ?? (c.children?.length ?? 0),
    extra: Math.max(0, cls - chips.length)
  }
}
function childChips(c) {
  if (c.categoryType !== 3) return []   // 只在分组节点展示对象 chip
  const s = childStatsMap.value[c.id] || {}
  return Array.isArray(s.classChips) ? s.classChips : []
}
const statCards = computed(() => {
  const s = stats.value || {}
  return [
    { key: 'child',    label: '下级',     value: s.childCount ?? 0,     icon: 'folder',  color: '#86909C' },
    { key: 'class',    label: '对象',     value: s.classCount ?? 0,     icon: 'cube',    color: '#165DFF', link: '/resources/object-types', createLabel: '新建对象类型' },
    { key: 'link',     label: '关系',     value: s.linkCount ?? 0,      icon: 'link',    color: '#00B42A', link: '/resources/link-types',   createLabel: '新建关系' },
    { key: 'action',   label: '动作',     value: s.actionCount ?? 0,    icon: 'zap',     color: '#FF7D00', link: '/resources/action-types', createLabel: '新建动作' },
    { key: 'function', label: '函数',     value: s.functionCount ?? 0,  icon: 'branch',  color: '#722ED1', link: '/resources/functions',    createLabel: '新建函数' },
    { key: 'interface',label: '接口',     value: s.interfaceCount ?? 0, icon: 'station', color: '#13C2C2', link: '/resources/interfaces',   createLabel: '新建接口' },
    { key: 'property', label: '属性',     value: s.propertyCount ?? 0,  icon: 'sliders', color: '#EB2F96' }
  ]
})

const children = computed(() => selected.value?.children || [])

function jumpTo(p) {
  // 由 router-link 触发；这里通过 window.location 跳转保持简洁
  location.hash = '#' + p
}

/* ---------- 增删改 ---------- */
const formOpen = ref(false)
const formMode = ref('create')
const formParent = ref(null)
const formData = reactive({ categoryType: 1, categoryCode: '', rdfsLabel: '', nsCode: '', icon: 'folder', color: '#165DFF', description: '' })

const formParentLabel = computed(() => formParent.value?.label || '（顶级）')

function openCreate(kind, parent) {
  formMode.value = 'create'
  formParent.value = parent || selected.value
  const type = parent ? parent.categoryType + 1 : (selected.value ? selected.value.categoryType + 1 : 1)
  Object.assign(formData, {
    categoryType: Math.min(type, 3),
    categoryCode: '',
    rdfsLabel: '',
    nsCode: selected.value?.nsCode || '',
    icon: 'folder',
    color: '#165DFF',
    description: ''
  })
  formOpen.value = true
}

function openEdit() {
  if (!selected.value) return
  formMode.value = 'edit'
  formParent.value = findById(selected.value.parentId)
  Object.assign(formData, {
    id: selected.value.id,
    categoryType: selected.value.categoryType,
    categoryCode: selected.value.categoryCode,
    rdfsLabel: selected.value.rdfsLabel || '',
    nsCode: selected.value.nsCode || '',
    icon: selected.value.icon || 'folder',
    color: selected.value.color || '#165DFF',
    description: selected.value.description || ''
  })
  formOpen.value = true
}

function backToOverview() {
  selected.value = null
}

const selectedPath = computed(() => {
  if (!selected.value) return []
  const byId = new Map()
  const collect = (nodes) => nodes.forEach(n => { byId.set(n.id, n); if (n.children) collect(n.children) })
  collect(tree.value)
  const path = []
  let cur = byId.get(selected.value.id) || selected.value
  while (cur) {
    path.unshift(cur)
    if (!cur.parentId || cur.parentId === '0') break
    cur = byId.get(cur.parentId)
  }
  return path
})

function findById(id, list = tree.value) {
  for (const n of list) {
    if (n.id === id) return n
    if (n.children?.length) { const r = findById(id, n.children); if (r) return r }
  }
  return null
}

async function submitForm() {
  if (!formData.categoryCode || !formData.rdfsLabel) {
    BL.warning('编码、中文名为必填')
    return
  }
  const payload = {
    ...formData,
    parentId: formParent.value?.id || '0',
    status: 1
  }
  if (formMode.value === 'create') {
    await categoryApi.create(payload)
    BL.success('已创建')
  } else {
    await categoryApi.update(payload.id, payload)
    BL.success('已保存')
  }
  formOpen.value = false
  await loadTree()
}

async function saveBasic() {
  if (!selected.value) return
  const payload = { ...selected.value, ...editForm }
  if (selected.value.categoryType === 3) {
    payload.metadata = JSON.stringify({ ...displayOpts })
  }
  await categoryApi.update(selected.value.id, payload)
  // 立即把表单变更同步到 selected，让中间面板头部立刻刷新（无需等待 loadTree 完成）
  selected.value = { ...selected.value, ...editForm }
  BL.success('已保存')
  await loadTree(true)
}

async function saveNsMetadata() {
  if (!selected.value?.nsCode || !nsInfo.value) return
  await namespaceApi.update(selected.value.nsCode, { ...nsInfo.value, metadata: nsMetadata.value })
  BL.success('命名空间元数据已保存')
}

async function doDelete() {
  if (!selected.value) return
  const ok = await BL.confirm({
    title: '确认删除',
    content: `将删除「${selected.value.label}」，存在下级节点时不可删除。`,
    danger: true, okText: '删除'
  })
  if (!ok) return
  try {
    await categoryApi.remove(selected.value.id)
    BL.success('已删除')
    selected.value = null
    await loadTree()
  } catch (e) {
    BL.error(e?.msg || '删除失败')
  }
}

async function newVersion() {
  const v = await BL.prompt({
    title: '新建版本',
    label: '版本号',
    placeholder: '如 1.1',
    defaultValue: '1.1',
    validate: v => !v || !v.trim() ? '版本号不能为空' : true
  })
  if (v == null) return
  await namespaceApi.createVersion({
    nsCode: selected.value.nsCode,
    version: v,
    uri: nsInfo.value?.nsUri || '',
    rdfsLabel: `${nsInfo.value?.nsName || ''} v${v}`,
    isCurrent: 0,
    status: 1
  })
  BL.success('版本已创建')
  await loadVersions()
}
async function removeVersion(v) {
  const ok = await BL.confirm({ title: '删除版本', content: `删除版本 ${v.version}？`, danger: true })
  if (!ok) return
  await namespaceApi.removeVersion(v.id)
  await loadVersions()
}

async function loadTree(preserveSelection = true) {
  const data = await categoryApi.tree()
  tree.value = data || []
  // 默认展开第一层
  expandedSet.value = new Set(tree.value.map(n => n.id))
  if (preserveSelection && selected.value) {
    const hit = findById(selected.value.id)
    if (hit) selected.value = hit
  }
}

function onCtx(node, action) {
  if (action === 'addChild') {
    if (node.categoryType >= 3) { BL.warning('分组下不可再创建分类'); return }
    openCreate(null, node)
  } else if (action === 'edit') {
    onSelect(node); openEdit()
  } else if (action === 'delete') {
    onSelect(node); doDelete()
  }
}

function exportTree() {
  const blob = new Blob([JSON.stringify(tree.value, null, 2)], { type: 'application/json' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = 'bontolink-category.json'
  a.click()
}

async function loadOverview() {
  discoverStats.value = await resourceApi.stats().catch(() => ({}))
  if (industries.value.length) {
    industryStatsMap.value = await categoryApi.statsBatch(industries.value.map(n => n.id)).catch(() => ({}))
  }
}

onMounted(async () => {
  await loadTree()
  nsList.value = await namespaceApi.list().catch(() => [])
  await loadOverview()
  unsubCustom = onCustomIconsChange(d => { customData.value = JSON.parse(JSON.stringify(d)) })
})
onUnmounted(() => { unsubCustom && unsubCustom() })
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.search-wrap { position: relative; width: 280px; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.search-input { padding-left: 30px; }

.three-pane {
  flex: 1; display: grid;
  grid-template-columns: 240px 1fr 380px;
  gap: 12px;
  padding: 12px;
  overflow: hidden;
}
/* 未选中节点：隐藏右侧详情，让中间总览占满 */
.three-pane.no-detail { grid-template-columns: 240px 1fr; }
.three-pane.no-detail > .pane-detail { display: none; }
.pane {
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  display: flex; flex-direction: column;
  overflow: hidden;
}
.pane-toolbar {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 8px;
  border-bottom: 1px solid var(--bl-divider);
}
.pane-search-row {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 8px;
  border-bottom: 1px solid var(--bl-divider);
  background: var(--bl-bg-2);
  position: relative;
}
.pane-search-ic { position: absolute; left: 16px; color: var(--bl-text-3); pointer-events: none; }
.pane-search-input { padding-left: 28px; height: 28px; background: var(--bl-bg-1); }
.tree-scroll { flex: 1; overflow: auto; padding: 6px; }
.is-on { color: var(--bl-primary); background: var(--bl-primary-soft); }

.view-toggle {
  display: inline-flex;
  background: var(--bl-bg-2);
  border-radius: var(--bl-radius-2);
  padding: 2px;
  gap: 2px;
  height: 26px;
  box-sizing: border-box;
}
.vt-btn {
  height: 22px;
  padding: 0 10px;
  border: 0; border-radius: 4px;
  background: transparent;
  color: var(--bl-text-3);
  cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center; gap: 4px;
  font-size: var(--bl-fs-12);
  transition: background-color .15s, color .15s, box-shadow .15s;
}
.vt-btn:hover { color: var(--bl-text-1); }
.vt-btn.is-on {
  background: var(--bl-bg-1);
  color: var(--bl-primary);
  box-shadow: var(--bl-shadow-1);
}

.bl-list-item .dot { width: 8px; height: 8px; border-radius: 50%; }
.li-row { padding: 7px 8px; }
.li-ico {
  width: 20px; height: 20px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

.versions {
  border-top: 1px solid var(--bl-divider);
  flex-shrink: 0;
  max-height: 36%;
  display: flex; flex-direction: column;
}
.versions-hd {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 10px;
  font-size: var(--bl-fs-12); color: var(--bl-text-3);
}
.versions-body { overflow: auto; padding: 0 6px 6px; }
.version-row {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 8px; border-radius: var(--bl-radius-2);
  font-size: var(--bl-fs-12);
}
.version-row:hover { background: var(--bl-bg-hover); }

.pane-center { padding: 16px; gap: 12px; overflow: auto; display: flex; flex-direction: column; }
.center-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.crumb {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 16px;
  margin: -16px -16px 0;
  background: transparent;
  border-bottom: 1px solid var(--bl-divider);
  font-size: var(--bl-fs-12);
  flex-wrap: wrap;
}
.crumb-link {
  display: inline-flex; align-items: center; gap: 4px;
  color: var(--bl-text-2);
  cursor: pointer;
  padding: 2px 6px; border-radius: 4px;
  transition: background-color .15s, color .15s;
}
.crumb-link:hover { background: var(--bl-bg-1); color: var(--bl-primary); }
.crumb-link.is-cur { color: var(--bl-text-1); font-weight: 500; cursor: default; }
.crumb-link.is-cur:hover { background: transparent; color: var(--bl-text-1); }
.crumb-sep { color: var(--bl-text-4); display: inline-flex; }
.crumb-ic {
  width: 16px; height: 16px; border-radius: 3px;
  display: inline-flex; align-items: center; justify-content: center;
}
.ov-head { display: flex; align-items: center; justify-content: space-between; padding: 4px 0 8px; }
.ov-chart {
  background: var(--bl-bg-2);
  border-radius: var(--bl-radius-3);
  padding: 14px 12px 10px;
}
/* CSS 柱状图 */
.ov-bars {
  display: flex; align-items: flex-end; justify-content: space-around;
  height: 200px;
  padding: 14px 0 6px;
}
.ov-bar-col {
  flex: 1;
  display: flex; flex-direction: column; align-items: center;
  gap: 6px;
  max-width: 30px;
  min-width: 6px;
}
.ov-bar-val { font-size: var(--bl-fs-11); font-weight: 600; }
.ov-bar-bar {
  width: 100%;
  min-height: 4px;
  border-radius: 4px;
  transition: height .3s ease;
}
.ov-x {
  display: flex; align-items: center;
  margin-top: 4px;
  font-size: var(--bl-fs-11); color: var(--bl-text-3);
}
.ov-x > span { text-align: center; padding: 0 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.title-row { display: flex; align-items: center; gap: 8px; }
.title-row h2 { margin: 0; font-size: var(--bl-fs-14); font-weight: 600; }
.stats {
  display: grid; gap: 10px;
  grid-template-columns: repeat(7, minmax(0, 1fr));
}
@media (max-width: 1280px) {
  .stats { grid-template-columns: repeat(4, minmax(0, 1fr)); }
}
.stat-card {
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  padding: 8px 10px;
  display: flex; flex-direction: column; gap: 6px;
  align-items: stretch;
  text-align: left;
  min-width: 0;
  transition: border-color .15s, box-shadow .15s;
  position: relative;
}
.stat-card:hover { border-color: var(--bl-primary-border); box-shadow: var(--bl-shadow-1); }
.stat-hd { display: flex; align-items: center; justify-content: flex-start; gap: 6px; }
.stat-add {
  margin-left: auto;
  width: 22px; height: 22px;
  border: 0; border-radius: 4px;
  background: var(--bl-bg-2);
  color: var(--bl-text-2);
  display: inline-flex; align-items: center; justify-content: center;
  cursor: pointer;
  transition: background-color .15s, color .15s;
}
.stat-add:hover { background: var(--bl-primary-soft); color: var(--bl-primary); }
.stat-ic {
  width: 20px; height: 20px; border-radius: 6px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.stat-lbl { font-size: var(--bl-fs-12); color: var(--bl-text-2); }
.stat-val {
  font-size: var(--bl-fs-24); font-weight: 700;
  color: var(--bl-text-1);
  line-height: 1.1;
  font-feature-settings: "tnum";
  text-align: center;
}
.stat-act {
  font-size: var(--bl-fs-12);
  color: var(--bl-primary);
  cursor: pointer;
}
.stat-act:hover { text-decoration: underline; }
.stat-act-mute { color: transparent; cursor: default; height: 18px; }
.section-title { font-size: var(--bl-fs-12); color: var(--bl-text-3); margin-top: 12px; }
.section-title.is-strong {
  display: flex; align-items: center; gap: 8px;
  font-size: var(--bl-fs-13); color: var(--bl-text-1); font-weight: 600;
}
.st-bar { width: 3px; height: 14px; background: var(--bl-primary); border-radius: 2px; }
.grp-graph-grow { flex: 1; min-height: 420px; display: flex; }
.grp-graph-grow > * { flex: 1; }
.id-row {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 0;
  font-size: var(--bl-fs-12);
}
.id-row-top { margin-top: 16px; padding-top: 12px; border-top: 1px dashed var(--bl-divider); }
.id-lbl { width: 36px; flex-shrink: 0; color: var(--bl-text-3); }
.id-val {
  flex: 1; min-width: 0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.child-grid {
  display: grid; gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}
.child-card {
  display: flex; flex-direction: column;
  padding: 8px 10px;
  border: 1px solid var(--bl-border); border-radius: var(--bl-radius-3);
  cursor: pointer; background: var(--bl-bg-1);
  transition: border-color .15s, box-shadow .15s;
  min-width: 0;
}
.child-card:hover { border-color: var(--bl-primary); box-shadow: var(--bl-shadow-1); }
.cc-hd { display: flex; gap: 12px; align-items: flex-start; }
.cc-ic {
  width: 40px; height: 40px; border-radius: var(--bl-radius-3);
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.cc-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.cc-title-row { display: flex; align-items: center; gap: 8px; min-width: 0; }
.cc-title { font-size: var(--bl-fs-14); font-weight: 600; color: var(--bl-text-1); flex: 1; min-width: 0; }
.cc-tag { font-size: var(--bl-fs-11); flex-shrink: 0; }
.cc-mini { display: flex; align-items: baseline; gap: 4px; font-size: var(--bl-fs-12); }
.cc-pri { font-size: var(--bl-fs-14); font-weight: 600; color: var(--bl-text-1); font-feature-settings: "tnum"; }
.cc-chips {
  display: flex; flex-wrap: wrap; align-items: flex-start; gap: 8px;
  margin-top: 10px; padding: 14px 4px;
  border-top: 1px dashed var(--bl-divider);
  border-bottom: 1px dashed var(--bl-divider);
  min-height: 140px;
  align-content: flex-start;
  background: var(--bl-bg-2);
  border-radius: var(--bl-radius-2);
}
.cc-chip {
  display: inline-flex; align-items: center;
  padding: 2px 10px;
  font-size: var(--bl-fs-12);
  background: var(--bl-primary-soft);
  color: var(--bl-primary);
  border-radius: 999px;
  white-space: nowrap;
  max-width: 100px;
  overflow: hidden; text-overflow: ellipsis;
}
.cc-sep { color: var(--bl-text-4); font-size: var(--bl-fs-12); }
.cc-more {
  display: inline-flex; align-items: center;
  padding: 2px 8px; font-size: var(--bl-fs-12);
  background: var(--bl-bg-2); color: var(--bl-text-3);
  border-radius: 999px;
}
.cc-ft {
  display: flex; align-items: baseline; justify-content: space-between;
  margin-top: 6px; padding-top: 4px;
  border-top: 1px dashed var(--bl-divider);
  font-size: var(--bl-fs-12);
}
.cc-sec { font-weight: 500; color: var(--bl-text-1); }

.pane-detail { padding: 0; }
.tabs { display: flex; border-bottom: 1px solid var(--bl-divider); padding: 0 8px; }
.tab {
  padding: 12px 12px; font-size: var(--bl-fs-13);
  color: var(--bl-text-2); cursor: pointer;
  border-bottom: 2px solid transparent; margin-bottom: -1px;
}
.tab:hover { color: var(--bl-text-1); }
.tab.is-active { color: var(--bl-primary); border-color: var(--bl-primary); font-weight: 500; }
.tab-body { flex: 1; padding: 12px 16px; overflow: auto; }
.tab-save-bar {
  display: flex; justify-content: flex-end;
  margin-top: 16px; padding-top: 12px;
  border-top: 1px dashed var(--bl-divider);
}

.color-row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.color-dot { width: 22px; height: 22px; border-radius: 50%; cursor: pointer; border: 2px solid transparent; }
.color-dot.is-active { border-color: var(--bl-bg-1); box-shadow: 0 0 0 2px var(--bl-primary); }

.color-stack { display: flex; flex-direction: column; gap: 10px; }
.hex-picker-group { display: inline-flex; align-items: stretch; }
.hex-picker-group .hex-input {
  width: 130px;
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
  border-right: 0;
}
.color-picker-attached {
  width: 32px; height: 28px;
  border: 1px solid var(--bl-border);
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
  border-top-right-radius: var(--bl-radius-3);
  border-bottom-right-radius: var(--bl-radius-3);
  cursor: pointer;
  padding: 2px;
  background: var(--bl-bg-1);
  flex-shrink: 0;
}
.color-picker-attached::-webkit-color-swatch-wrapper { padding: 0; }
.color-picker-attached::-webkit-color-swatch { border: 0; border-radius: 6px; }
.color-picker-attached::-moz-color-swatch { border: 0; border-radius: 6px; }

.binding-row {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 8px;
  background: var(--bl-bg-2);
  border-radius: var(--bl-radius-2);
}

/* 分组 · 成员 */
.grp-hd {
  display: flex; align-items: center; justify-content: space-between;
  padding: 4px 0 10px;
  font-weight: 500;
}
.grp-list { display: flex; flex-direction: column; gap: 6px; }
.grp-row {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 8px;
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-2);
  background: var(--bl-bg-1);
  font-size: var(--bl-fs-13);
}
.grp-row:hover { border-color: var(--bl-primary-border); }
.grp-drag { color: var(--bl-text-3); cursor: grab; display: inline-flex; }
.grp-row[draggable="true"]:active { cursor: grabbing; }
.grp-row[draggable="true"]:hover .grp-drag { color: var(--bl-primary); }

.mp-modal { width: 520px; max-width: 92vw; max-height: 80vh; display: flex; flex-direction: column; }
.mp-body { padding: 0 16px; display: flex; flex-direction: column; gap: 8px; flex: 1; min-height: 320px; overflow: hidden; }
.mp-search { display: flex; align-items: center; gap: 8px; position: relative; }
.mp-search .search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); pointer-events: none; }
.mp-search input { padding-left: 28px; }
.mp-list { flex: 1; overflow: auto; display: flex; flex-direction: column; gap: 4px; padding-bottom: 12px; }
.mp-row {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 10px;
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-2);
  cursor: pointer;
  font-size: var(--bl-fs-13);
}
.mp-row:hover { border-color: var(--bl-primary-border); }
.mp-row.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.mp-row input { accent-color: var(--bl-primary); }
.grp-rid {
  margin-top: 16px; padding-top: 12px;
  border-top: 1px dashed var(--bl-divider);
  display: flex; flex-direction: column; gap: 4px;
}

/* 分组 · 显示 */
.display-section {
  padding: 10px 0;
  border-bottom: 1px dashed var(--bl-divider);
}
.display-section:last-of-type { border-bottom: 0; }
.display-label { font-size: var(--bl-fs-12); color: var(--bl-text-3); margin-bottom: 8px; }
.display-label-row {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 8px;
}
.display-label-row .display-label { margin-bottom: 0; }
.more-link {
  font-size: var(--bl-fs-12);
  color: var(--bl-primary);
  cursor: pointer;
}
.more-link:hover { text-decoration: underline; }
.icon-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 6px;
}
.fr-icon {
  display: flex; flex-direction: column; gap: 8px;
  padding: 8px 0 10px;
  border-bottom: 1px dashed var(--bl-divider);
}
.fr-label-row {
  display: flex; align-items: center; justify-content: space-between;
}
.fr-label-row .fr-label {
  color: var(--bl-text-3);
  font-size: var(--bl-fs-12);
}
.icon-picker-modal {
  width: 1280px; max-width: 95vw;
  display: flex; flex-direction: column;
  max-height: 94vh;
}
.ip-hd {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 16px 14px 20px;
  border-bottom: 1px solid var(--bl-border);
  flex-shrink: 0;
}
.ip-body {
  padding: 0 !important;
  display: grid;
  grid-template-columns: 200px 1fr;
  height: 580px;
  min-height: 0;
  overflow: hidden;
}
.ip-side {
  background: var(--bl-bg-2);
  border-right: 1px solid var(--bl-border);
  padding: 8px 0;
  overflow: auto;
}
.ip-cat {
  padding: 4px 16px;
  cursor: pointer;
  font-size: var(--bl-fs-13);
  color: var(--bl-text-2);
  display: flex; align-items: center; justify-content: space-between;
  border-left: 3px solid transparent;
  min-height: 32px;
}
.ip-cat:hover { background: var(--bl-bg-1); color: var(--bl-text-1); }
.ip-cat.is-active {
  background: var(--bl-bg-1);
  color: var(--bl-primary);
  font-weight: 500;
  border-left-color: var(--bl-primary);
}
.ip-cat-count {
  font-size: var(--bl-fs-12);
  color: var(--bl-text-4);
  background: var(--bl-bg-3);
  border-radius: 10px;
  padding: 0 6px;
  min-width: 22px; text-align: center;
}
.ip-cat.is-active .ip-cat-count { background: var(--bl-primary-soft); color: var(--bl-primary); }

/* —— 侧栏分组（自定义 / 内置）—— */
.ip-side {
  background: var(--bl-bg-1);
  padding: 10px 8px;
  gap: 10px;
  display: flex; flex-direction: column;
  overflow: auto;
}
.ip-side-section {
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-2);
  overflow: hidden;
  flex-shrink: 0;
}
.ip-side-head {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 10px;
  cursor: pointer;
  user-select: none;
  color: var(--bl-text-2);
  font-size: var(--bl-fs-12);
  font-weight: 600;
  letter-spacing: .2px;
  background: var(--bl-bg-2);
  border-bottom: 1px solid var(--bl-divider);
  white-space: nowrap;
}
.ip-side-head:hover { color: var(--bl-text-1); background: var(--bl-bg-3); }
.ip-side-head-chev {
  display: inline-flex; align-items: center; justify-content: center;
  width: 14px; height: 14px;
  transition: transform .15s ease;
  color: var(--bl-text-3);
}
.ip-side-head-chev.is-open { transform: rotate(90deg); }
.ip-side-head-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ip-side-head-count {
  font-size: 11px;
  color: var(--bl-text-3);
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-divider);
  border-radius: 10px;
  padding: 0 6px;
  min-width: 22px; text-align: center;
  font-weight: 500;
}
.ip-side-section:has(.ip-side-head:hover) { border-color: var(--bl-text-4); }
.ip-side-list { padding: 4px 0; }
.ip-cat { white-space: nowrap; }
.ip-cat > span:first-child { overflow: hidden; text-overflow: ellipsis; }
.ip-side-empty {
  padding: 10px 16px;
  font-size: var(--bl-fs-12);
  color: var(--bl-text-4);
  text-align: center;
}
.ip-cat-custom { position: relative; gap: 4px; min-height: 32px; }
.ip-cat-label { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ip-cat-actions {
  display: none;
  align-items: center; gap: 0;
}
.ip-cat-actions .bl-btn { height: 24px; width: 22px; padding: 0; }
.ip-cat-custom:hover .ip-cat-actions { display: inline-flex; }
.ip-cat-custom:hover .ip-cat-count { display: none; }
.ip-cat-rename {
  height: 22px; padding: 0 4px;
  font-size: var(--bl-fs-12);
  flex: 1; min-width: 0;
}

/* —— 工具栏右侧（上传 + 搜索）—— */
.ip-toolbar-right { display: inline-flex; align-items: center; gap: 8px; }

/* —— 上传/移除按钮 —— */
.ip-cell { position: relative; }
.ip-cell-x {
  position: absolute; top: 2px; right: 2px;
  width: 14px; height: 14px;
  display: none;
  align-items: center; justify-content: center;
  border: 0; border-radius: 50%;
  background: var(--bl-bg-3);
  color: var(--bl-text-2);
  cursor: pointer;
  padding: 0;
}
.ip-cell:hover .ip-cell-x { display: inline-flex; }
.ip-cell-x:hover { background: var(--bl-danger, #F53F3F); color: #fff; }

/* —— 批量操作 —— */
.ip-batch-info {
  font-size: var(--bl-fs-12);
  color: var(--bl-text-2);
  margin-right: 4px;
}
.ip-batch-info b { color: var(--bl-primary); }
.ip-cell.is-batch-on {
  border-color: var(--bl-primary);
  background: var(--bl-primary-soft);
}
.ip-cell-check {
  position: absolute; top: 3px; right: 3px;
  width: 14px; height: 14px;
  display: inline-flex; align-items: center; justify-content: center;
  border-radius: 3px;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  box-sizing: border-box;
}
.ip-cell-check.is-on {
  background: var(--bl-primary);
  border-color: var(--bl-primary);
}
.ip-main {
  display: flex; flex-direction: column;
  padding: 14px 16px;
  min-width: 0; min-height: 0;
  overflow: hidden;
}
.ip-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px; gap: 12px;
}
.ip-crumb {
  display: flex; align-items: center; gap: 6px;
  font-size: var(--bl-fs-13);
  color: var(--bl-text-1);
}
.ip-search { position: relative; width: 220px; }
.ip-search-ic {
  position: absolute; left: 8px; top: 50%; transform: translateY(-50%);
  color: var(--bl-text-3);
  display: inline-flex;
}
.ip-search-input { padding-left: 26px !important; }
.ip-grid {
  flex: 1; min-height: 0;
  display: grid;
  grid-template-columns: repeat(15, 1fr);
  gap: 8px;
  overflow-y: auto;
  overflow-x: hidden;
  align-content: start;
  padding-right: 4px;
}
.ip-cell {
  aspect-ratio: 1;
  padding: 0;
}
.icon-cell {
  width: 100%; aspect-ratio: 1;
  display: inline-flex; align-items: center; justify-content: center;
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-2);
  color: var(--bl-text-2);
  cursor: pointer; transition: all .15s;
}
.icon-cell:hover { color: var(--bl-primary); border-color: var(--bl-primary); }
.icon-cell.is-active { color: var(--bl-primary); border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.grp-check {
  display: flex; align-items: center; gap: 8px;
  padding: 4px 0;
  font-size: var(--bl-fs-13);
  cursor: pointer;
}
.grp-check input { accent-color: var(--bl-primary); }
.display-actions { padding-top: 12px; }
</style>
