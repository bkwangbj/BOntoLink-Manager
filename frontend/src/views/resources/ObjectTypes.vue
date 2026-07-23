<template>
  <div class="ot-page">
    <!-- 顶部操作栏 48px -->
    <header class="ot-topbar">
      <div class="ot-title-wrap" :title="titleTip">
        <span class="ot-title">对象类别</span>
        <span class="ot-subtitle">对象、属性、物理映射、规则</span>
      </div>
      <div class="ot-actions">
        <!-- 分组：仅列表视图下启用，按所属领域折叠分组 -->
        <button :class="['ot-group-btn', groupMode && 'is-on']"
                :disabled="viewMode !== 'list'"
                :title="viewMode !== 'list' ? '仅列表视图可用' : (groupMode ? '取消分组' : '按领域分组')"
                @click="groupMode = !groupMode">
          <span v-html="BL.icon('layers', 12)"></span>
          <span style="margin-left:4px">分组</span>
        </button>
        <select class="bl-input ot-select" v-model="filterIndustry" :title="'行业'">
          <option value="">全部行业</option>
          <option v-for="i in industryOpts" :key="i.code" :value="i.code">{{ i.label }}</option>
        </select>
        <select class="bl-input ot-select" v-model="filterDomain" :title="'业务领域 (受所选行业级联)'">
          <option value="">{{ filterIndustry ? '全部领域' : '全部业务领域' }}</option>
          <option v-for="d in domainOpts" :key="d.code" :value="d.code">{{ d.label }}</option>
        </select>
        <select class="bl-input ot-select" v-model="filterStatus" :title="'状态'">
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">禁用</option>
        </select>
        <div class="ot-search">
          <span class="ot-search-ic" v-html="BL.icon('search', 14)"></span>
          <input class="bl-input ot-search-input" v-model="q" placeholder="搜索对象（名称/编码）" />
          <button v-if="q" class="ot-search-clear" @click="q=''" v-html="BL.icon('x', 10)"></button>
        </div>
        <button class="bl-btn bl-btn-primary" @click="onCreate">
          <span v-html="BL.icon('plus', 12, '#fff')"></span>
          <span style="margin-left:4px">新建对象</span>
        </button>
      </div>
    </header>

    <!-- 主体区域 -->
    <section class="ot-body">
      <!-- 左侧分组树: 统一行业分类树 (与所有资源模块共用) -->
      <CategoryTreeFilter :rows="rows"
                          title="行业分类"
                          total-label="全部对象"
                          store-key="object-types"
                          :custom-counts="groupNodeCounts"
                          @change="onCategoryChange" />
      <!-- 左侧 TAB 页签 -->
      <div class="ot-pane" :style="paneStyle">
        <div class="ot-tabs">
          <button v-for="v in viewTabs" :key="v.k" :class="['ot-tab', viewMode === v.k && 'is-on']" @click="viewMode = v.k">
            <span v-html="BL.icon(v.icon, 12)"></span>
            <span style="margin-left:4px">{{ v.label }}</span>
          </button>
        </div>

        <!-- 列表 -->
        <div v-if="viewMode === 'list'" class="ot-list-card">
          <div class="ot-list-scroll">
            <table class="bl-table ot-table">
              <thead>
                <tr>
                  <th style="width:28px"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th>
                  <th><span class="th-sort" @click="toggleSort('name')">对象<span class="th-arrow">{{ sortArrow('name') }}</span></span></th>
                  <th><span class="th-sort" @click="toggleSort('category')">领域<span class="th-arrow">{{ sortArrow('category') }}</span></span></th>
                  <th><span class="th-sort" @click="toggleSort('prop')">属性数<span class="th-arrow">{{ sortArrow('prop') }}</span></span></th>
                  <th><span class="th-sort" @click="toggleSort('parent')">父类<span class="th-arrow">{{ sortArrow('parent') }}</span></span></th>
                  <th><span class="th-sort" @click="toggleSort('child')">子类数<span class="th-arrow">{{ sortArrow('child') }}</span></span></th>
                  <th><span class="th-sort" @click="toggleSort('ds')">数据源数<span class="th-arrow">{{ sortArrow('ds') }}</span></span></th>
                  <th>关联表数</th>
                  <th>RID</th>
                  <th><span class="th-sort" @click="toggleSort('status')">状态<span class="th-arrow">{{ sortArrow('status') }}</span></span></th>
                  <th style="width:90px"></th>
                </tr>
              </thead>
              <tbody>
                <template v-for="row in displayRows" :key="row.key">
                  <!-- 分组标题行 -->
                  <tr v-if="row.type==='group'" class="ot-group-row" @click="toggleDomain(row.key)">
                    <td :colspan="11">
                      <span class="ot-group-chev" v-html="BL.icon(row.collapsed ? 'chevronRight' : 'chevronDown', 12)"></span>
                      <span class="ot-group-label">{{ row.label }}</span>
                      <span class="ot-group-count">{{ row.count }}</span>
                    </td>
                  </tr>
                  <!-- 对象行 -->
                  <tr v-else
                      :class="['ot-row', groupMode && 'is-grouped', selected?.id===row.data.id && 'is-active']"
                      @click="openDrawer(row.data)">
                    <td @click.stop><input type="checkbox" :checked="checked.has(row.data.id)" @change="toggleOne(row.data.id)" /></td>
                    <td class="ot-cell-obj">
                      <div class="ot-name-cell">
                        <span class="ot-ic" :style="{ background: row.data.color || '#165DFF' }" v-html="BL.icon(row.data.icon || 'cube', 12, '#fff')"></span>
                        <div class="ot-name-text">
                          <div class="ot-obj-label bl-truncate" :title="row.data.display_name || row.data.rdfs_label || row.data.api_name">{{ row.data.display_name || row.data.rdfs_label || row.data.api_name }}</div>
                          <div class="ot-obj-api bl-mono bl-muted bl-truncate">{{ row.data.api_name }}</div>
                        </div>
                      </div>
                    </td>
                    <td><span class="bl-muted">{{ row.data.categoryLabel || '—' }}</span></td>
                    <td>
                      <a class="ot-link" @click.stop="openDrawer(row.data, 'props')">
                        {{ row.data.propNormal ?? 0 }} <span class="bl-muted">|</span> {{ row.data.propTotal ?? 0 }}
                      </a>
                    </td>
                    <td>
                      <a v-if="row.data.parentLabel" class="ot-link">{{ row.data.parentLabel }}<span class="bl-mono bl-muted" style="margin-left:4px">{{ row.data.parentApiName }}</span></a>
                      <span v-else class="bl-muted">—</span>
                    </td>
                    <td><a class="ot-link" @click.stop="openDrawer(row.data, 'hierarchy')">{{ row.data.childCount ?? 0 }}</a></td>
                    <td><a class="ot-link" @click.stop="openDrawer(row.data, 'ds')">{{ row.data.dsCount ?? 0 }}</a></td>
                    <td class="bl-truncate" :title="(row.data.relatedTables || []).join('、')">
                      <span v-if="(row.data.relatedTables || []).length" class="bl-mono">{{ (row.data.relatedTables || []).join('、') }}</span>
                      <span v-else class="bl-muted">—</span>
                    </td>
                    <td class="ot-rid">
                      <span class="bl-mono bl-muted bl-truncate" :title="row.data.rid">{{ shortRid(row.data.rid) }}</span>
                      <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" title="复制 RID" @click.stop="copyText(row.data.rid)" v-html="BL.icon('copy', 11)"></button>
                    </td>
                    <td>
                      <span :class="['bl-tag', row.data.status === 1 ? 'bl-tag-success' : 'bl-tag-danger']">
                        <StatusTag :status="row.data.status" />
                      </span>
                    </td>
                    <td @click.stop>
                      <div class="bl-row" style="gap:0">
                        <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="编辑" @click="openDrawer(row.data, 'overview')" v-html="BL.icon('edit', 12)"></button>
                        <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" :title="row.data.status===1?'禁用':'启用'" @click="toggleStatus(row.data)" v-html="BL.icon('zap', 12)"></button>
                        <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click="removeOne(row.data)" v-html="BL.icon('trash', 12)"></button>
                      </div>
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
            <div v-if="!paged.length" class="bl-empty" style="padding:60px">
              <div class="bl-empty-icon" v-html="BL.icon('cube', 36)"></div>
              <div>未匹配到对象类型</div>
              <div style="margin-top:12px">
                <button class="bl-btn" @click="clearFilters">清除筛选</button>
              </div>
            </div>
          </div>
          <!-- 分页钉底 -->
          <div class="ot-pager">
            <div class="ot-pager-l">
              <template v-if="checked.size">
                已选 <b style="color:var(--bl-primary)">{{ checked.size }}</b> 项
                <button class="bl-btn bl-btn-sm ot-del-btn" style="margin-left:8px" @click="removeBatch">
                  <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">批量删除</span>
                </button>
                <button class="bl-btn bl-btn-sm ot-ena-btn" style="margin-left:6px" @click="batchSetStatus(1)">
                  <span v-html="BL.icon('check', 12)"></span><span style="margin-left:4px">启用</span>
                </button>
                <button class="bl-btn bl-btn-sm ot-dis-btn" style="margin-left:6px" @click="batchSetStatus(0)">
                  <span v-html="BL.icon('power', 12)"></span><span style="margin-left:4px">禁用</span>
                </button>
                <button class="bl-btn bl-btn-sm bl-btn-text ot-clear-btn" style="margin-left:6px" @click="checked = new Set()">取消选择</button>
              </template>
              <template v-else>
                共 {{ filtered.length }} 项
                <span v-if="groupMode" class="bl-muted" style="margin-left:8px">· {{ groupCount }} 个领域</span>
              </template>
            </div>
            <div class="ot-pager-r" v-if="!groupMode">
              <span class="bl-muted" style="font-size:12px;margin-right:8px">每页</span>
              <select class="bl-input ot-page-size" v-model.number="pageSize">
                <option :value="20">20</option>
                <option :value="50">50</option>
                <option :value="100">100</option>
              </select>
              <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page<=1" @click="page--">‹</button>
              <span class="bl-muted" style="font-size:12px">{{ page }} / {{ totalPages }}</span>
              <button class="bl-btn bl-btn-sm bl-btn-text" :disabled="page>=totalPages" @click="page++">›</button>
            </div>
            <div class="ot-pager-r" v-else>
              <button class="bl-btn bl-btn-sm bl-btn-text" @click="expandAllGroups">展开全部</button>
              <button class="bl-btn bl-btn-sm bl-btn-text" @click="collapseAllGroups">折叠全部</button>
            </div>
          </div>
        </div>

        <!-- 卡片 -->
        <div v-else-if="viewMode === 'card'" class="ot-card-grid">
          <div v-for="r in filtered" :key="r.id" class="ot-card" @click="openDrawer(r)">
            <div class="ot-card-hd">
              <div class="ot-ic ot-ic-lg" :style="{ background: r.color || '#165DFF' }" v-html="BL.icon(r.icon || 'cube', 18, '#fff')"></div>
              <div class="bl-grow" style="min-width:0">
                <div class="ot-card-title bl-truncate">{{ r.display_name || r.rdfs_label || r.api_name }}</div>
                <div class="ot-card-api bl-mono bl-muted bl-truncate">{{ r.api_name }}</div>
              </div>
              <StatusTag :status="r.status" />
            </div>
            <div class="ot-card-stats">
              <div class="ot-card-stat"><span class="bl-muted">属性</span><b>{{ r.propTotal ?? 0 }}</b></div>
              <div class="ot-card-stat"><span class="bl-muted">关系</span><b>{{ r.linkCount ?? 0 }}</b></div>
              <div class="ot-card-stat"><span class="bl-muted">动作</span><b>{{ r.actionCount ?? 0 }}</b></div>
              <div class="ot-card-stat"><span class="bl-muted">数据源</span><b>{{ r.dsCount ?? 0 }}</b></div>
            </div>
            <div class="ot-card-ft bl-muted">{{ r.categoryLabel || '—' }}</div>
          </div>
          <div v-if="!filtered.length" class="bl-empty" style="padding:60px;grid-column:1/-1">
            <div class="bl-empty-icon" v-html="BL.icon('cube', 36)"></div>
            <div>未匹配到对象类型</div>
          </div>
        </div>

        <!-- 图谱（占位） -->
        <div v-else class="ot-graph-stub">
          <div class="bl-empty" style="padding:80px">
            <div class="bl-empty-icon" v-html="BL.icon('network', 48)"></div>
            <div>图谱视图建设中</div>
            <div class="bl-muted" style="margin-top:8px;font-size:12px">支持行业 → 领域 → 分组 → 对象 → 接口 → 关系探索</div>
          </div>
        </div>
      </div>

      <!-- 右侧抽屉（浮在左侧之上） -->
      <transition name="ot-drawer">
      <aside v-if="drawerOpen" class="ot-drawer" :style="{ width: drawerWidth + 'px' }">
        <div class="ot-drag-handle"
             @mousedown="onDragStart"
             :class="resizing && 'is-resizing'"></div>
        <div class="ot-drawer-hd">
          <div class="ot-drawer-hd-l">
            <div class="ot-ic ot-ic-lg" :style="{ background: selected?.color || '#165DFF' }" v-html="BL.icon(selected?.icon || 'cube', 18, '#fff')"></div>
            <div class="bl-grow" style="min-width:0">
              <div class="ot-drawer-title">
                <span class="bl-truncate">{{ selected?.display_name || selected?.rdfs_label || selected?.api_name }}</span>
                <span class="bl-mono bl-muted" style="font-size:12px">({{ selected?.api_name }})</span>
                <StatusTag :status="selected?.status" />
              </div>
            </div>
          </div>
          <div class="ot-drawer-hd-r">
            <button class="bl-btn bl-btn-sm" @click="onCreateFromDrawer" title="关闭当前抽屉, 打开新建对象类型向导"><span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">新建对象</span></button>
            <button class="bl-btn bl-btn-sm" @click="onAi"><span v-html="BL.icon('ai', 12)"></span><span style="margin-left:4px">AI 助手</span></button>
            <button class="bl-btn bl-btn-sm" @click="onViewInstances"><span v-html="BL.icon('database', 12)"></span><span style="margin-left:4px">查看实例</span></button>
            <button class="bl-btn bl-btn-sm bl-btn-primary" @click="onEdit"><span v-html="BL.icon('edit', 12, '#fff')"></span><span style="margin-left:4px">编辑</span></button>
            <span class="ot-drawer-sep"></span>
            <button class="bl-btn bl-btn-text bl-btn-icon" :title="drawerMaxed ? '恢复' : '最大'" @click="toggleMax" v-html="BL.icon(drawerMaxed ? 'minimize' : 'maximize', 14)"></button>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="closeDrawer" v-html="BL.icon('x', 14)"></button>
          </div>
        </div>

        <div class="ot-drawer-body">
          <!-- 左侧 TAB 分组 -->
          <nav class="ot-tab-side">
            <div v-for="g in tabGroups" :key="g.key" class="ot-tab-group">
              <div class="ot-tab-group-hd" @click="toggleGroup(g.key)">
                <span class="ot-tab-group-chev" :class="!collapsedGroups.has(g.key) && 'is-open'" v-html="BL.icon('chevronRight', 11)"></span>
                <span class="ot-tab-group-ic" :style="{ background: g.color }" v-html="BL.icon(g.icon, 11, '#fff')"></span>
                <span>{{ g.label }}</span>
              </div>
              <div v-show="!collapsedGroups.has(g.key)" class="ot-tab-group-body">
                <button v-for="t in g.tabs" :key="t.key"
                        :class="['ot-tab-item', drawerTab === t.key && 'is-on']"
                        @click="drawerTab = t.key">
                  <span class="ot-tab-label">{{ t.label }}</span>
                  <span v-if="tabCount(t.key) !== null" class="ot-tab-cnt">{{ tabCount(t.key) }}</span>
                </button>
              </div>
            </div>
          </nav>

          <!-- 右侧 TAB 内容区 -->
          <section class="ot-tab-pane">
            <!-- 概览（4 sub-tabs: 基础/显示/类表达式/其他） -->
            <div v-if="drawerTab === 'overview'" class="ot-tab-content">
              <TabOverview :detail="detail" @saved="onClassSaved" />
            </div>

            <!-- 属性（行内编辑） -->
            <div v-else-if="drawerTab === 'props'" class="ot-tab-content">
              <TabProps :class-id="selected?.id" :class-name="selected?.display_name || selected?.api_name || ''" @navigate-tab="drawerTab = $event" />
            </div>

            <!-- 时序图表 (类型类真实渲染:颜色/线型/左右轴/单位/枚举/倒置/插值) -->
            <div v-else-if="drawerTab === 'tsChart'" class="ot-tab-content">
              <TimeseriesChart :class-id="selected?.id" />
            </div>

            <!-- 事件时间轴 (event 类型类:类型配色/等级/闭环) -->
            <div v-else-if="drawerTab === 'evTimeline'" class="ot-tab-content">
              <EventTimeline :class-id="selected?.id" />
            </div>

            <!-- 链接关系 (Canvas: 链接类型可视化) -->
            <div v-else-if="drawerTab === 'graph'" class="ot-tab-content ot-tab-canvas">
              <TabLinkGraph :class-id="selected?.id" />
            </div>

            <!-- 对象图谱 (七大维度全景, 见对象图谱需求文档) -->
            <div v-else-if="drawerTab === 'objgraph'" class="ot-tab-content ot-tab-canvas">
              <TabObjectGraph :class-id="selected?.id"
                              @open-object="onGraphOpenObject"
                              @editor-open-change="onLinkEditorOpenChange" />
            </div>

            <!-- 等价类 -->
            <div v-else-if="drawerTab === 'equiv'" class="ot-tab-content">
              <TabClassGroup :class-id="selected?.id" type="equivalent" />
            </div>

            <!-- 不相交类 -->
            <div v-else-if="drawerTab === 'disjoint'" class="ot-tab-content">
              <TabClassGroup :class-id="selected?.id" type="disjoint" />
            </div>

            <!-- 互斥并集类 -->
            <div v-else-if="drawerTab === 'disjointUnion'" class="ot-tab-content">
              <TabDisjointUnion
                :class-id="selected?.id"
                :parent-label="selected?.display_name || selected?.rdfs_label || selected?.api_name"
                :parent-api="selected?.api_name"
                :parent-icon="selected?.icon || 'cube'"
                :parent-color="selected?.color || '#165DFF'" />
            </div>

            <!-- 等价属性 -->
            <div v-else-if="drawerTab === 'propEquiv'" class="ot-tab-content">
              <TabPropertyRelation
                :class-id="selected?.id"
                :class-label="selected?.display_name || selected?.rdfs_label || selected?.api_name"
                type="equivalent" />
            </div>

            <!-- 不相交属性 -->
            <div v-else-if="drawerTab === 'propDisjoint'" class="ot-tab-content">
              <TabPropertyRelation
                :class-id="selected?.id"
                :class-label="selected?.display_name || selected?.rdfs_label || selected?.api_name"
                type="disjoint" />
            </div>

            <!-- 被引用 -->
            <div v-else-if="drawerTab === 'refBy'" class="ot-tab-content">
              <Placeholder icon="link" label="被引用" desc="呈现引用当前对象的接口 / 关系 / 派生属性 / 视图清单" />
            </div>

            <!-- 接口 -->
            <div v-else-if="drawerTab === 'iface'" class="ot-tab-content">
              <div class="ot-tab-toolbar">
                <span class="bl-muted">已实现 {{ (detail.interfaces || []).length }} 个接口</span>
                <div class="bl-grow"></div>
                <button class="bl-btn bl-btn-sm bl-btn-primary"><span v-html="BL.icon('plus', 12, '#fff')"></span><span style="margin-left:4px">实现接口</span></button>
              </div>
              <table class="bl-table">
                <thead><tr><th>接口</th><th>API</th><th>状态</th></tr></thead>
                <tbody>
                  <tr v-for="i in (detail.interfaces || [])" :key="i.id">
                    <td>
                      <div class="bl-row" style="gap:8px;align-items:center">
                        <span class="ot-ic" :style="{ background: i.color || '#165DFF' }" v-html="BL.icon(i.icon || 'station', 11, '#fff')"></span>
                        {{ i.display_name || i.rdfs_label || i.api_name }}
                      </div>
                    </td>
                    <td class="bl-mono">{{ i.api_name }}</td>
                    <td><span :class="['bl-tag', i.status === 1 ? 'bl-tag-success' : 'bl-tag-warning']">{{ i.status === 1 ? '启用' : '废弃' }}</span></td>
                  </tr>
                </tbody>
              </table>
              <div v-if="!(detail.interfaces || []).length" class="bl-empty" style="padding:32px">暂未实现任何接口</div>
            </div>

            <!-- 动作 -->
            <div v-else-if="drawerTab === 'actions'" class="ot-tab-content">
              <div class="ot-tab-toolbar">
                <span class="bl-muted">共 {{ (detail.actions || []).length }} 个动作</span>
              </div>
              <table class="bl-table">
                <thead><tr><th>动作</th><th>API</th><th>类型</th><th>状态</th></tr></thead>
                <tbody>
                  <tr v-for="a in (detail.actions || [])" :key="a.id">
                    <td>{{ a.display_name || a.rdfs_label || a.api_name }}</td>
                    <td class="bl-mono">{{ a.api_name }}</td>
                    <td><span class="bl-tag">{{ a.action_kind || '—' }}</span></td>
                    <td><StatusTag :status="a.status" /></td>
                  </tr>
                </tbody>
              </table>
              <div v-if="!(detail.actions || []).length" class="bl-empty" style="padding:32px">暂无动作</div>
            </div>

            <!-- 函数 -->
            <div v-else-if="drawerTab === 'fn'" class="ot-tab-content">
              <Placeholder icon="branch" label="派生属性函数" desc="管理与当前对象关联的 Function 计算逻辑,支持实时计算与 TTL 缓存" />
            </div>

            <!-- 数据源 (本对象实际挂接的物理表: 主表 + 附表, 可编辑/删除) -->
            <div v-else-if="drawerTab === 'ds'" class="ot-tab-content">
              <div class="ot-tab-toolbar">
                <span class="bl-muted">本对象数据源 {{ (detail.classDatasources || []).length }} 个 (主表 / 附表)</span>
              </div>
              <table class="bl-table ot-ds-table">
                <thead><tr>
                  <th>物理表</th><th>名称/别名</th><th>类型</th><th>字段数</th>
                  <th>关联键 (附表)</th><th>连接 (附表)</th><th style="width:60px">操作</th>
                </tr></thead>
                <tbody>
                  <tr v-for="d in (detail.classDatasources || [])" :key="d.id">
                    <td class="bl-mono">{{ d.physical_table }}</td>
                    <td>
                      <input class="bl-input bl-input-sm" :value="d.table_label || d.alias || ''"
                             placeholder="中文名/别名" @change="onSaveClassDs(d, { table_label: $event.target.value })" />
                    </td>
                    <td>
                      <select class="bl-input bl-input-sm" :value="d.rel_type"
                              @change="onSaveClassDs(d, { rel_type: Number($event.target.value) })">
                        <option :value="1">主表</option>
                        <option :value="2">附表</option>
                      </select>
                    </td>
                    <td>{{ (d.physical_fields || []).length }}</td>
                    <td>
                      <div v-if="d.rel_type !== 1" class="ot-ds-join">
                        <select class="bl-input bl-input-sm bl-mono" :value="joinParts(d).main" title="主表关联字段"
                                @change="onSaveJoin(d, 'main', $event.target.value)">
                          <option value="">主表字段…</option>
                          <option v-for="f in dsMainFields" :key="'m-'+f" :value="f">{{ f }}</option>
                        </select>
                        <span class="bl-muted">=</span>
                        <select class="bl-input bl-input-sm bl-mono" :value="joinParts(d).supp" title="附表关联字段"
                                @change="onSaveJoin(d, 'supp', $event.target.value)">
                          <option value="">附表字段…</option>
                          <option v-for="f in (d.physical_fields || [])" :key="'s-'+f.name" :value="f.name">{{ f.name }}</option>
                        </select>
                      </div>
                      <span v-else class="bl-muted">—</span>
                    </td>
                    <td>
                      <select v-if="d.rel_type !== 1" class="bl-input bl-input-sm" :value="d.join_type || 'left'"
                              @change="onSaveClassDs(d, { join_type: $event.target.value })">
                        <option value="left">LEFT</option>
                        <option value="inner">INNER</option>
                        <option value="right">RIGHT</option>
                      </select>
                      <span v-else class="bl-muted">—</span>
                    </td>
                    <td>
                      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除绑定"
                              @click="onDeleteClassDs(d)" v-html="BL.icon('trash', 13, '#f53f3f')"></button>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div v-if="!(detail.classDatasources || []).length" class="bl-empty" style="padding:32px">本对象暂未挂接物理表，请在「新建对象向导」的数据源映射步骤配置</div>
            </div>

            <!-- 使用情况 -->
            <div v-else-if="drawerTab === 'usage'" class="ot-tab-content">
              <Placeholder icon="activity" label="使用情况" desc="呈现引用本对象的接口/页面/实例数量、最近调用时间、热度趋势" />
            </div>
          </section>
        </div>
      </aside>
      </transition>

      <!-- 嵌套抽屉 (第二层): 从对象图谱钻取查看其他对象, 顶部 tab 栈管理多对象 -->
      <transition name="ot-drawer">
      <aside v-if="nestedDrawerOpen && nestedSelected" class="ot-drawer ot-drawer-nested"
             :style="{ width: nestedDrawerWidth + 'px' }">
        <!-- 左边缘拖拽手柄 — 调整第二层宽度, 范围 [400, 主抽屉宽-200] -->
        <div class="ot-drag-handle"
             @mousedown="onNestedDragStart"
             :class="nestedResizing && 'is-resizing'"></div>

        <!-- tab 栈条 (无图标版本) -->
        <div class="ot-tab-bar">
          <div v-for="(t, i) in nestedTabs" :key="t.id"
               :class="['ot-tab-bar-item', i === nestedActiveTabIdx && 'is-active']"
               :title="nestedTabTooltip(i)"
               @click="activateNestedTab(i)">
            <span class="ot-tab-bar-lbl bl-truncate">{{ tabLabel(t) }}</span>
            <button class="ot-tab-bar-close" @click.stop="closeNestedTab(i)"
                    :title="'关闭 ' + tabLabel(t)"
                    v-html="BL.icon('x', 12)"></button>
          </div>
        </div>

        <!-- 头部 (同主抽屉) -->
        <div class="ot-drawer-hd">
          <div class="ot-drawer-hd-l">
            <div class="ot-ic ot-ic-lg" :style="{ background: nestedSelected?.color || '#165DFF' }"
                 v-html="BL.icon(nestedSelected?.icon || 'cube', 18, '#fff')"></div>
            <div class="bl-grow" style="min-width:0">
              <div class="ot-drawer-title">
                <span class="bl-truncate">{{ tabLabel(nestedSelected) }}</span>
                <span class="bl-mono bl-muted" style="font-size:12px">({{ nestedSelected?.api_name }})</span>
                <StatusTag :status="nestedSelected?.status" />
              </div>
            </div>
          </div>
          <div class="ot-drawer-hd-r">
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="closeNestedDrawer"
                    v-html="BL.icon('x', 14)"></button>
          </div>
        </div>

        <!-- 主体 (左导航 + 右内容); 嵌套抽屉简化为 概览 / 属性 / 链接关系 / 对象图谱 4 个 tab -->
        <div class="ot-drawer-body">
          <nav class="ot-tab-side">
            <div class="ot-tab-group">
              <div class="ot-tab-group-body">
                <button v-for="t in nestedSubTabs" :key="t.key"
                        :class="['ot-tab-item', nestedDrawerTab === t.key && 'is-on']"
                        @click="nestedDrawerTab = t.key">
                  <span class="ot-tab-label">{{ t.label }}</span>
                </button>
              </div>
            </div>
          </nav>
          <section class="ot-tab-pane">
            <div v-if="nestedDrawerTab === 'overview'" class="ot-tab-content">
              <TabOverview :detail="nestedDetail" @saved="() => loadNestedDetail(nestedSelected.id)" />
            </div>
            <div v-else-if="nestedDrawerTab === 'props'" class="ot-tab-content">
              <TabProps :class-id="nestedSelected?.id" :class-name="tabLabel(nestedSelected)" @navigate-tab="nestedDrawerTab = $event" />
            </div>
            <div v-else-if="nestedDrawerTab === 'graph'" class="ot-tab-content ot-tab-canvas">
              <TabLinkGraph :class-id="nestedSelected?.id" />
            </div>
            <div v-else-if="nestedDrawerTab === 'objgraph'" class="ot-tab-content ot-tab-canvas">
              <TabObjectGraph :class-id="nestedSelected?.id"
                              @open-object="onGraphOpenObject"
                              @editor-open-change="onLinkEditorOpenChange" />
            </div>
          </section>
        </div>
      </aside>
      </transition>
    </section>

    <!-- 新建对象类型向导 -->
    <NewObjectTypeWizard v-model:open="wizardOpen" :initial-group-id="initialGroupId" @next="onWizardNext" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, h } from 'vue'
import { BL } from '@/lib/bl.js'
import { resourceApi, categoryApi, classMetaApi, groupApi, groupRefApi } from '@/api'
import FieldRow from '@/views/config/category/FieldRow.vue'
import CategoryTreeFilter from '@/components/CategoryTreeFilter.vue'
import TabOverview from '@/views/resources/objecttype/TabOverview.vue'
import TabProps from '@/views/resources/objecttype/TabProps.vue'
import TimeseriesChart from '@/views/resources/objecttype/TimeseriesChart.vue'
import EventTimeline from '@/views/resources/objecttype/EventTimeline.vue'
import TabClassGroup from '@/views/resources/objecttype/TabClassGroup.vue'
import TabLinkGraph from '@/views/resources/objecttype/TabLinkGraph.vue'
import TabObjectGraph from '@/views/resources/objecttype/TabObjectGraph.vue'
import TabDisjointUnion from '@/views/resources/objecttype/TabDisjointUnion.vue'
import TabPropertyRelation from '@/views/resources/objecttype/TabPropertyRelation.vue'
import NewObjectTypeWizard from '@/views/resources/objecttype/NewObjectTypeWizard.vue'
import StatusTag from '@/components/StatusTag.vue'

// 占位组件（内嵌 functional 风格）
const Placeholder = {
  props: { icon: { type: String, default: 'cube' }, label: String, desc: String },
  setup(props) {
    return () => h('div', { class: 'bl-empty', style: 'padding:60px' }, [
      h('div', { class: 'bl-empty-icon', innerHTML: BL.icon(props.icon, 40) }),
      h('div', { style: 'margin-top:8px;font-weight:500' }, props.label),
      h('div', { class: 'bl-muted', style: 'margin-top:6px;font-size:12px;max-width:360px;margin-left:auto;margin-right:auto' }, props.desc),
      h('div', { class: 'bl-muted', style: 'margin-top:14px;font-size:11px' }, '该 TAB 待与对应模块联调')
    ])
  }
}

/* ===== 数据 ===== */
const rows = ref([])
const tree = ref([])

/* ===== 顶部筛选 ===== */
const q = ref('')
const filterIndustry = ref('')   // 行业 code (level 1)
const filterDomain = ref('')     // 领域 code (level 2+, 受 filterIndustry 级联约束)
const filterStatus = ref('')

const titleTip = computed(() => '对象类别 · 对象、属性、物理映射、规则')

// 行业选项: 顶层 (category_type=1)
const industryOpts = computed(() => {
  return (tree.value || [])
    .filter(n => n.categoryType === 1)
    .map(n => ({ code: n.categoryCode, label: n.label || n.rdfsLabel || n.categoryCode }))
})

// 领域选项: 受行业级联约束 (filterIndustry 为空时显示所有领域)
const domainOpts = computed(() => {
  const out = []
  const indCode = filterIndustry.value
  const walk = (ns) => {
    for (const n of (ns || [])) {
      if (n.categoryType === 2) out.push({ code: n.categoryCode, label: n.label || n.rdfsLabel || n.categoryCode })
      if (n.children?.length) walk(n.children)
    }
  }
  if (indCode) {
    // 找到选中的行业, 只遍历它的子树
    const ind = (tree.value || []).find(n => n.categoryCode === indCode && n.categoryType === 1)
    if (ind) walk(ind.children)
  } else {
    // 无行业筛选 → 列出全部领域
    walk(tree.value)
  }
  return out
})

// 行业切换 → 自动重置领域 (避免脏选)
watch(filterIndustry, () => { filterDomain.value = '' })

// 工具: 收集行业下所有后代 category_code (含自身)
function descendantCodesOfIndustry(indCode) {
  const set = new Set()
  const ind = (tree.value || []).find(n => n.categoryCode === indCode && n.categoryType === 1)
  if (!ind) return set
  const walk = (n) => {
    if (n.categoryCode) set.add(n.categoryCode)
    ;(n.children || []).forEach(walk)
  }
  walk(ind)
  return set
}

/* ===== 排序 ===== */
const sortKey = ref('')
const sortDir = ref('')  // 'asc' | 'desc' | ''
function toggleSort(key) {
  if (sortKey.value !== key) { sortKey.value = key; sortDir.value = 'asc' }
  else if (sortDir.value === 'asc') sortDir.value = 'desc'
  else { sortKey.value = ''; sortDir.value = '' }
}
function sortArrow(key) {
  if (sortKey.value !== key) return ' ⇅'
  return sortDir.value === 'asc' ? ' ↑' : ' ↓'
}

/* —— 左侧行业分类树 (统一组件,按 category_code 子树过滤) —— */
const selectedCategoryCodes = ref(null)  // null = 全部, Set<string> = 当前分类及子分类的 category_code 集合
const groupClassIds = ref(null)          // null = 非分组过滤态; Set<string> = 选中分组关联的对象类 id
const groupNodeCounts = ref({})          // 分组节点真实计数覆盖,传给 CategoryTreeFilter customCounts
const selectedTreeNode = ref(null)       // 当前选中的分类树节点,用于新建向导上下文
const initialGroupId = computed(() => {
  // 如果当前选中了分组节点(type=3), 新建对象时默认归属该分组
  const node = selectedTreeNode.value
  return node && node.categoryType === 3 ? (node.id || '') : ''
})
async function onCategoryChange({ codes, node }) {
  selectedCategoryCodes.value = codes || null
  selectedTreeNode.value = node || null
  // 选中分组节点(type=3): 对象类归属分组走 ont_biz_group_class 关联,不能靠 category_code(那是领域)。
  // 分组节点 id 与 ont_biz_group.id 已统一,可直接按 group id 取关联对象类。
  if (node && node.categoryType === 3) {
    try {
      const list = await groupApi.classes(node.id)
      groupClassIds.value = new Set((list || []).map(c => c.id))
      groupNodeCounts.value = { [node.id]: (list || []).length }
    } catch { groupClassIds.value = new Set(); groupNodeCounts.value = { [node.id]: 0 } }
  } else {
    groupClassIds.value = null
    groupNodeCounts.value = {}
  }
}

const filtered = computed(() => {
  let list = rows.value
  if (groupClassIds.value) {
    // 分组过滤态: 只显示该分组关联的对象类(走 ont_biz_group_class)
    list = list.filter(r => groupClassIds.value.has(r.id))
  } else if (selectedCategoryCodes.value) {
    list = list.filter(r => selectedCategoryCodes.value.has(r.category_code))
  }
  // 行业过滤: 命中该行业下所有后代 codes
  if (filterIndustry.value) {
    const codes = descendantCodesOfIndustry(filterIndustry.value)
    list = list.filter(r => codes.has(r.category_code))
  }
  if (filterDomain.value) list = list.filter(r => r.category_code === filterDomain.value)
  if (filterStatus.value !== '') list = list.filter(r => String(r.status) === filterStatus.value)
  const k = q.value.trim().toLowerCase()
  if (k) {
    list = list.filter(r => [r.api_name, r.display_name, r.rdfs_label, r.category_code, r.rid]
      .filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
  }
  // 排序
  if (sortKey.value && sortDir.value) {
    const dir = sortDir.value === 'asc' ? 1 : -1
    list = [...list].sort((a, b) => {
      let va, vb
      switch (sortKey.value) {
        case 'name':     va = a.display_name || a.rdfs_label || a.api_name || ''; vb = b.display_name || b.rdfs_label || b.api_name || ''; break
        case 'category': va = a.categoryLabel || ''; vb = b.categoryLabel || ''; break
        case 'prop':     va = a.propTotal || 0; vb = b.propTotal || 0; break
        case 'parent':   va = a.parentLabel || ''; vb = b.parentLabel || ''; break
        case 'child':    va = a.childCount || 0; vb = b.childCount || 0; break
        case 'ds':       va = a.dsCount || 0; vb = b.dsCount || 0; break
        case 'status':   va = a.status ?? 9; vb = b.status ?? 9; break
        default:         va = ''; vb = ''
      }
      if (typeof va === 'number') return (va - vb) * dir
      return String(va).localeCompare(String(vb)) * dir
    })
  }
  return list
})

/* ===== 视图切换 ===== */
const viewMode = ref('list')
const viewTabs = [
  { k: 'list', label: '列表', icon: 'list' },
  { k: 'card', label: '卡片', icon: 'grid' },
  { k: 'graph', label: '图谱', icon: 'network' }
]

/* ===== 分页 ===== */
const page = ref(1)
const pageSize = ref(20)
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize.value)))
const paged = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})
watch(filtered, () => { if (page.value > totalPages.value) page.value = 1 })

/* ===== 按所属领域分组显示 ===== */
const groupMode = ref(false)
const collapsedDomains = ref(new Set())
function toggleDomain(key) {
  const s = new Set(collapsedDomains.value)
  s.has(key) ? s.delete(key) : s.add(key)
  collapsedDomains.value = s
}
function rowGroupKey(r) { return r.categoryLabel || '—' }
const groupedRows = computed(() => {
  const groups = new Map()
  filtered.value.forEach(r => {
    const k = rowGroupKey(r)
    if (!groups.has(k)) groups.set(k, [])
    groups.get(k).push(r)
  })
  const out = []
  for (const [key, items] of groups) {
    const collapsed = collapsedDomains.value.has(key)
    out.push({ type: 'group', key, label: key, count: items.length, collapsed })
    if (!collapsed) items.forEach(r => out.push({ type: 'item', key: 'i' + r.id, data: r }))
  }
  return out
})
const groupCount = computed(() => new Set(filtered.value.map(rowGroupKey)).size)
const displayRows = computed(() =>
  groupMode.value
    ? groupedRows.value
    : paged.value.map(r => ({ type: 'item', key: 'i' + r.id, data: r }))
)
function expandAllGroups() { collapsedDomains.value = new Set() }
function collapseAllGroups() { collapsedDomains.value = new Set(filtered.value.map(rowGroupKey)) }

/* ===== 多选 ===== */
const checked = ref(new Set())
const allChecked = computed(() => paged.value.length > 0 && paged.value.every(r => checked.value.has(r.id)))
function toggleAll() {
  const s = new Set(checked.value)
  if (allChecked.value) paged.value.forEach(r => s.delete(r.id))
  else paged.value.forEach(r => s.add(r.id))
  checked.value = s
}
function toggleOne(id) {
  const s = new Set(checked.value)
  s.has(id) ? s.delete(id) : s.add(id)
  checked.value = s
}

/* ===== 右侧抽屉 ===== */
const selected = ref(null)
const detail = ref({})
const drawerOpen = ref(false)
const drawerTab = ref('overview')
const drawerWidth = ref(0)
const drawerMaxed = ref(false)
const drawerMined = ref(false)
const resizing = ref(false)

// 抽屉尺寸约束
const DRAWER_MIN = 450
function drawerMaxPx() {
  return Math.floor(window.innerWidth * 0.85)
}
function defaultDrawerWidth() {
  // 默认占左侧 TAB 区 85%（视为占视口 85%）
  return Math.max(DRAWER_MIN, Math.floor(window.innerWidth * 0.55))
}

const paneStyle = computed(() => ({}))

function openDrawer(r, tab = 'overview') {
  selected.value = r
  drawerTab.value = tab
  drawerOpen.value = true
  drawerMaxed.value = false
  drawerMined.value = false
  if (!drawerWidth.value) drawerWidth.value = defaultDrawerWidth()
  loadDetail(r.id)
}
function closeDrawer() {
  drawerOpen.value = false
  selected.value = null
  closeNestedDrawer()
}

/* ===== 嵌套抽屉 (双层钻取层) =====
   场景: 用户在主抽屉对象图谱里点击其他对象时, 不嵌套小子抽屉,
        而是: 主抽屉自动最大化, 上层弹出第二层抽屉, 第二层抽屉里以 tab 栈形式管理多个钻取对象.
   - 入口对象 (主抽屉的 selected) 始终不变, 不参与 tab 栈
   - nestedTabs[i]: 钻取的对象, 含 sourceId 用于关闭后切回来源 tab
   - 第二层关闭时, 主抽屉根据 preNestedMainMaxed 还原 (如果之前不是最大化)
   - 第二层抽屉宽度可拖拽; 默认 520, 范围 [400, 主抽屉宽 - 100], 保证主抽屉图谱仍露出 */
const TAB_MAX = 8
const NESTED_DRAWER_MIN = 400
const NESTED_DRAWER_DEFAULT = 520
const nestedDrawerOpen = ref(false)
const nestedDrawerWidth = ref(NESTED_DRAWER_DEFAULT)
const nestedResizing = ref(false)
const nestedTabs = ref([])
const nestedActiveTabIdx = ref(-1)
const nestedSelected = ref(null)
const nestedDetail = ref({})
const nestedDrawerTab = ref('overview')
const nestedTabCounts = ref({})
let preNestedMainMaxed = null

function nestedDrawerMaxPx() {
  // 不能盖住主抽屉, 至少给主抽屉留 200px
  return Math.max(NESTED_DRAWER_MIN, drawerWidth.value - 200)
}

/* 第二层抽屉左边缘拖拽 — 改宽度 */
let nestedDragStartX = 0
let nestedDragStartW = 0
function onNestedDragStart(e) {
  nestedResizing.value = true
  nestedDragStartX = e.clientX
  nestedDragStartW = nestedDrawerWidth.value
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onNestedDragMove)
  window.addEventListener('mouseup', onNestedDragEnd)
}
function onNestedDragMove(e) {
  const dx = nestedDragStartX - e.clientX
  const next = Math.min(nestedDrawerMaxPx(),
                        Math.max(NESTED_DRAWER_MIN, nestedDragStartW + dx))
  nestedDrawerWidth.value = next
}
function onNestedDragEnd() {
  nestedResizing.value = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onNestedDragMove)
  window.removeEventListener('mouseup', onNestedDragEnd)
}

function tabLabel(t) {
  return t?.display_name || t?.rdfs_label || t?.api_name || '—'
}
function nestedTabTooltip(i) {
  const chain = []
  const seen = new Set()
  let cur = i
  while (cur >= 0 && cur < nestedTabs.value.length && !seen.has(cur)) {
    seen.add(cur)
    chain.push(tabLabel(nestedTabs.value[cur]))
    const srcId = nestedTabs.value[cur].sourceId
    if (!srcId) break
    cur = nestedTabs.value.findIndex(t => t.id === srcId)
  }
  // 入口对象作为最起点
  if (selected.value) chain.push(tabLabel(selected.value))
  return chain.reverse().join('  ›  ')
}

async function loadNestedDetail(id) {
  try { nestedDetail.value = await resourceApi.classDetail(id) }
  catch { nestedDetail.value = {} }
}

function activateNestedTab(idx) {
  if (idx < 0 || idx >= nestedTabs.value.length) return
  nestedActiveTabIdx.value = idx
  const t = nestedTabs.value[idx]
  nestedSelected.value = t
  nestedDrawerTab.value = 'overview'
  loadNestedDetail(t.id)
}

function pushNestedTab(record, sourceId = null) {
  if (!record || !record.id) return
  const existIdx = nestedTabs.value.findIndex(t => t.id === record.id)
  if (existIdx >= 0) { activateNestedTab(existIdx); return }
  if (nestedTabs.value.length >= TAB_MAX) {
    let removeIdx = -1
    for (let i = 0; i < nestedTabs.value.length; i++) {
      if (i !== nestedActiveTabIdx.value) { removeIdx = i; break }
    }
    if (removeIdx >= 0) {
      nestedTabs.value.splice(removeIdx, 1)
      if (nestedActiveTabIdx.value > removeIdx) nestedActiveTabIdx.value--
    }
  }
  nestedTabs.value.push({ ...record, sourceId })
  activateNestedTab(nestedTabs.value.length - 1)
}

function closeNestedTab(idx) {
  if (idx < 0 || idx >= nestedTabs.value.length) return
  const closed = nestedTabs.value[idx]
  nestedTabs.value.splice(idx, 1)
  if (nestedTabs.value.length === 0) { closeNestedDrawer(); return }
  if (idx === nestedActiveTabIdx.value) {
    let next = closed.sourceId
      ? nestedTabs.value.findIndex(t => t.id === closed.sourceId)
      : -1
    if (next < 0) next = Math.max(0, idx - 1)
    activateNestedTab(next)
  } else if (idx < nestedActiveTabIdx.value) {
    nestedActiveTabIdx.value--
  }
}

function closeNestedDrawer() {
  nestedDrawerOpen.value = false
  nestedTabs.value = []
  nestedActiveTabIdx.value = -1
  nestedSelected.value = null
  nestedDetail.value = {}
  nestedDrawerWidth.value = NESTED_DRAWER_DEFAULT
  // 主抽屉如果在打开嵌套抽屉之前不是最大化, 还原回去
  if (preNestedMainMaxed === false) {
    drawerWidth.value = defaultDrawerWidth()
    drawerMaxed.value = false
  }
  preNestedMainMaxed = null
}

/* 链接编辑抽屉 (LinkTypeEditor) 在图谱里打开时, 主抽屉同步最大化;
   关闭时按记录还原. 若第二层钻取抽屉已开, 主抽屉的最大化由它管, 这里跳过. */
let preLinkEditorMainMaxed = null
function onLinkEditorOpenChange(open) {
  if (nestedDrawerOpen.value) return
  if (open) {
    preLinkEditorMainMaxed = drawerMaxed.value
    if (!drawerMaxed.value) {
      drawerWidth.value = drawerMaxPx()
      drawerMaxed.value = true
      drawerMined.value = false
    }
  } else {
    if (preLinkEditorMainMaxed === false) {
      drawerWidth.value = defaultDrawerWidth()
      drawerMaxed.value = false
    }
    preLinkEditorMainMaxed = null
  }
}

/* 图谱节点点击 → 主抽屉自动最大化 + 上层抽屉打开 + push tab */
function onGraphOpenObject(payload) {
  if (!payload || !payload.id) {
    BL.info('该节点无对应对象详情')
    return
  }
  if (!nestedDrawerOpen.value) {
    preNestedMainMaxed = drawerMaxed.value
    if (!drawerMaxed.value) {
      drawerWidth.value = drawerMaxPx()
      drawerMaxed.value = true
      drawerMined.value = false
    }
    nestedDrawerOpen.value = true
  }
  const sourceId = nestedSelected.value?.id || selected.value?.id || null
  pushNestedTab({
    id: payload.id,
    api_name:     payload.api_name     || payload.en  || '',
    display_name: payload.display_name || payload.cn  || payload.label || '',
    rdfs_label:   payload.rdfs_label   || payload.label || payload.cn || '',
    color:        payload.color || '#165DFF',
    icon:         payload.icon  || 'cube',
    status:       payload.status ?? 1
  }, sourceId)
}
function toggleMax() {
  if (drawerMaxed.value) { drawerWidth.value = defaultDrawerWidth(); drawerMaxed.value = false }
  else { drawerWidth.value = drawerMaxPx(); drawerMaxed.value = true; drawerMined.value = false }
}
function toggleMin() {
  if (drawerMined.value) { drawerWidth.value = defaultDrawerWidth(); drawerMined.value = false }
  else { drawerWidth.value = DRAWER_MIN; drawerMined.value = true; drawerMaxed.value = false }
}

// 拖拽 resize
let dragStartX = 0
let dragStartW = 0
function onDragStart(e) {
  resizing.value = true
  dragStartX = e.clientX
  dragStartW = drawerWidth.value
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragEnd)
}
function onDragMove(e) {
  const dx = dragStartX - e.clientX
  const next = Math.min(drawerMaxPx(), Math.max(DRAWER_MIN, dragStartW + dx))
  drawerWidth.value = next
  drawerMaxed.value = next === drawerMaxPx()
  drawerMined.value = next === DRAWER_MIN
}
function onDragEnd() {
  resizing.value = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
}

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
})

async function loadDetail(id) {
  try {
    detail.value = await resourceApi.classDetail(id)
  } catch {
    detail.value = {}
  }
  await loadTabCounts(id)
}

/* —— 数据源 tab: 编辑 / 删除 本对象挂接的物理表绑定 (ont_class_ds) —— */
function curClassId() { return selected.value?.id || detail.value?.id || '' }
/* 主表字段列表 (供附表选择关联字段) */
const dsMainFields = computed(() => {
  const main = (detail.value?.classDatasources || []).find(d => d.rel_type === 1)
  return (main?.physical_fields || []).map(f => f.name)
})
/* join_on_keys 存储格式: "主表字段=附表字段"; 无 '=' 则视为两侧同名 */
function joinParts(d) {
  const raw = (d.join_on_keys || '').split(',')[0].trim()
  if (raw.includes('=')) { const [m, s] = raw.split('='); return { main: (m || '').trim(), supp: (s || '').trim() } }
  return { main: raw, supp: raw }
}
function onSaveJoin(d, side, val) {
  const p = joinParts(d)
  p[side] = val
  const joined = p.main && p.supp ? `${p.main}=${p.supp}` : (p.main || p.supp || '')
  onSaveClassDs(d, { join_on_keys: joined })
}
async function onSaveClassDs(d, patch) {
  const body = {
    table_label: d.table_label, alias: d.alias, rel_type: d.rel_type,
    pk_keys: d.pk_keys, join_on_keys: d.join_on_keys, join_type: d.join_type,
    sort: d.sort ?? 0, status: d.status ?? 1,
    ...patch
  }
  try {
    await classMetaApi.updateClassDs(d.id, body)
    BL.success('已保存')
    await loadDetail(curClassId())
  } catch (e) { BL.error(e?.msg || '保存失败') }
}
async function onDeleteClassDs(d) {
  const ok = await BL.confirm({
    title: '删除数据源绑定',
    content: `确定删除「${d.physical_table}」(${d.rel_type === 1 ? '主表' : '附表'}) 的绑定？引用它的属性映射会一并解除。`,
    danger: true, okText: '删除'
  })
  if (!ok) return
  try {
    await classMetaApi.removeClassDs(d.id)
    BL.success('已删除绑定')
    await loadDetail(curClassId())
  } catch (e) { BL.error(e?.msg || '删除失败') }
}

/* —— 左导航每个 tab 的数量徽标 —— */
const tabCounts = ref({})
async function loadTabCounts(id) {
  if (!id) { tabCounts.value = {}; return }
  const d = detail.value || {}
  const base = {
    props:    d.propTotal ?? selected.value?.propTotal ?? (d.properties || []).length,
    tables:   (d.classDatasources || []).length,
    links:    d.linkCount ?? (d.links || []).length,
    hierarchy: selected.value?.childCount ?? 0,
    iface:    d.interfaceCount ?? (d.interfaces || []).length,
    actions:  d.actionCount ?? (d.actions || []).length,
    ds:       (d.classDatasources || []).length || (d.datasources || []).length
  }
  // 规则约束 + 业务应用: 并发加载
  try {
    const [equiv, disjoint, disjointUnion, propEquiv, propDisjoint] = await Promise.all([
      classMetaApi.listGroup(id, 'equivalent').catch(() => []),
      classMetaApi.listGroup(id, 'disjoint').catch(() => []),
      classMetaApi.listDisjointUnion(id).catch(() => []),
      classMetaApi.listPropEquiv(id).catch(() => []),
      classMetaApi.listPropDisjoint(id).catch(() => [])
    ])
    base.equiv         = equiv.length
    base.disjoint      = disjoint.length
    base.disjointUnion = disjointUnion.length
    base.propEquiv     = propEquiv.length
    base.propDisjoint  = propDisjoint.length
  } catch {}
  tabCounts.value = base
}
function tabCount(k) {
  const n = tabCounts.value[k]
  return (typeof n === 'number' && n > 0) ? n : null
}

/* 概览保存后刷新详情 + 列表 */
async function onClassSaved(id) {
  try { rows.value = await resourceApi.classes({ aggregate: true }) } catch {}
  await loadDetail(id)
}

/* ===== TAB 分组结构（与对象类型设计文档严格一致） ===== */
/* 嵌套抽屉的精简 tab 列表 (避免再嵌套图谱钻取叠加更深) */
const nestedSubTabs = [
  { key: 'overview', label: '概览' },
  { key: 'props',    label: '属性' },
  { key: 'graph',    label: '链接关系' },
  { key: 'objgraph', label: '对象图谱' }
]

const tabGroups = [
  { key: 'basic', label: '基础信息', icon: 'fileText', color: '#1677ff', tabs: [
    { key: 'overview', label: '概览' },
    { key: 'props', label: '属性' }
  ]},
  { key: 'rel', label: '关系', icon: 'link', color: '#FF7D00', tabs: [
    { key: 'graph', label: '链接关系' },
    { key: 'objgraph', label: '对象图谱' }
  ]},
  { key: 'rule', label: '规则约束', icon: 'sliders', color: '#722ED1', tabs: [
    { key: 'equiv', label: '等价类' },
    { key: 'disjoint', label: '不相交类' },
    { key: 'disjointUnion', label: '互斥并集类' },
    { key: 'propEquiv', label: '等价属性' },
    { key: 'propDisjoint', label: '不相交属性' },
    { key: 'iface', label: '接口' }
  ]},
  { key: 'biz', label: '业务应用', icon: 'database', color: '#00B42A', tabs: [
    { key: 'actions', label: '动作' },
    { key: 'fn', label: '函数' },
    { key: 'refBy', label: '被引用' },
    { key: 'ds', label: '数据源' },
    { key: 'usage', label: '使用情况' },
    { key: 'tsChart', label: '时序图表' },
    { key: 'evTimeline', label: '事件时间轴' }
  ]}
]
const collapsedGroups = ref(new Set())
function toggleGroup(k) {
  const s = new Set(collapsedGroups.value)
  s.has(k) ? s.delete(k) : s.add(k)
  collapsedGroups.value = s
}

/* ===== 工具函数 ===== */
function shortRid(rid) {
  if (!rid) return '—'
  return rid.length > 28 ? rid.slice(0, 28) + '…' : rid
}
async function copyText(t) {
  if (!t) return
  try { await navigator.clipboard.writeText(String(t)); BL.success('已复制') }
  catch { BL.warning('复制失败,请手动选取') }
}
function clearFilters() { q.value = ''; filterIndustry.value = ''; filterDomain.value = ''; filterStatus.value = '' }
/* —— 新建对象类型向导 —— */
const wizardOpen = ref(false)
function onCreate() { wizardOpen.value = true }
/* 抽屉头部「新建对象」: 先关掉当前详情抽屉, 再打开向导 (避免抽屉 + 模态叠放) */
function onCreateFromDrawer() {
  closeDrawer()
  wizardOpen.value = true
}
async function onWizardNext(payload) {
  try {
    const body = {
      api_name: payload.main?.physical_table || '',
      display_name: payload.main?.alias_name || payload.main?.physical_table || '新对象类型',
      rdfs_label: payload.main?.alias_name || payload.main?.physical_table || '',
      category_code: payload.categoryCode || '',
      ds_code: payload.dsCode || '',
      ds_id: payload.dsId || '',
      main: payload.main || null,
      subs: payload.subs || [],
      props: payload.props || []
    }
    const created = await classMetaApi.createClass(body)
    // 如果有分组, 创建后绑定分组关联
    if (payload.group_id) {
      try {
        await groupRefApi.create({ ref_id: created.id, group_id: payload.group_id, group_type: 'object_types' })
      } catch (e) { /* 分组绑定失败不影响主流程 */ }
    }
    BL.success(`对象类型「${created.display_name || created.api_name}」已创建，可在详情中继续完善`)
    rows.value = await resourceApi.classes({ aggregate: true }).catch(() => rows.value)
    const row = rows.value.find(r => r.id === created.id) || created
    openDrawer(row)
  } catch (e) {
    BL.error(e?.msg || '创建失败')
  }
}
function onEdit() { BL.info('编辑面板待联调') }
function onAi() { BL.info('AI 助手待联调') }
function onViewInstances() { BL.info('查看实例待联调') }
async function reloadRows() {
  rows.value = await resourceApi.classes({ aggregate: true }).catch(() => rows.value)
  // 抽屉打开的对象若已被删除则关闭, 否则同步状态标签
  if (selected.value) {
    const row = rows.value.find(r => r.id === selected.value.id)
    if (!row) closeDrawer()
    else selected.value.status = row.status
  }
}
async function toggleStatus(r) {
  const next = r.status === 1 ? 0 : 1
  try {
    await classMetaApi.setClassStatus(r.id, next)
    r.status = next
    if (selected.value?.id === r.id) selected.value.status = next
    BL.success(next === 1 ? '已启用' : '已禁用')
  } catch (e) { BL.error(e?.msg || '操作失败') }
}
async function removeOne(r) {
  const ok = await BL.confirm({ title: '删除对象', content: `确定删除「${r.display_name || r.rdfs_label || r.api_name}」?该对象的属性、表映射、规则约束将一并清除。`, danger: true, okText: '删除' })
  if (!ok) return
  try {
    const res = await classMetaApi.removeClass(r.id)
    if (res?.blocked) { BL.warning(res.reason || '该对象被引用,无法删除'); return }
    BL.success('已删除')
    checked.value = new Set([...checked.value].filter(id => id !== r.id))
    await reloadRows()
  } catch (e) { BL.error(e?.msg || '删除失败') }
}
async function removeBatch() {
  const ids = [...checked.value]
  if (!ids.length) return
  const ok = await BL.confirm({ title: '批量删除', content: `确认删除 ${ids.length} 个对象?被引用的对象会自动跳过。`, danger: true, okText: '删除' })
  if (!ok) return
  let okCount = 0, blockedCount = 0
  for (const id of ids) {
    try {
      const res = await classMetaApi.removeClass(id)
      if (res?.blocked) blockedCount++; else okCount++
    } catch { blockedCount++ }
  }
  if (blockedCount) BL.warning(`已删除 ${okCount} 个,${blockedCount} 个被引用未删除`)
  else BL.success(`已删除 ${okCount} 个`)
  checked.value = new Set()
  await reloadRows()
}
async function batchSetStatus(status) {
  const ids = [...checked.value]
  if (!ids.length) return
  const verb = status === 1 ? '启用' : '禁用'
  const ok = await BL.confirm({ title: `批量${verb}`, content: `确认${verb} ${ids.length} 个对象?`, okText: verb })
  if (!ok) return
  let okCount = 0, failCount = 0
  for (const id of ids) {
    try { await classMetaApi.setClassStatus(id, status); okCount++ } catch { failCount++ }
  }
  if (failCount) BL.warning(`成功 ${okCount} 个,失败 ${failCount} 个`)
  else BL.success(`已${verb} ${okCount} 个`)
  checked.value = new Set()
  await reloadRows()
}

/* ===== 生命周期 ===== */
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()
// URL 带 ?openId=<id> 时打开抽屉;消费后清 query,避免刷新自动弹、并支持同页再次点击
function applyOpenId(id) {
  if (!id) return
  const row = rows.value.find(r => r.id === id)
  if (row) { openDrawer(row); router.replace({ query: {} }) }
}
onMounted(async () => {
  try { tree.value = await categoryApi.tree() } catch {}
  try { rows.value = await resourceApi.classes({ aggregate: true }) } catch { rows.value = [] }
  applyOpenId(route.query.openId)
})
watch(() => route.query.openId, applyOpenId)
</script>

<style scoped>
.ot-page { display: flex; flex-direction: column; height: 100%; }

/* —— 顶部操作栏 48px —— */
.ot-topbar {
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 20px 12px; gap: 16px;
  border-bottom: 1px solid var(--bl-border);
  background: var(--bl-bg-1);
}
.ot-title-wrap {
  display: flex; align-items: baseline; gap: 12px;
  min-width: 0; flex: 1; overflow: hidden;
}
.ot-title {
  font-size: 18px; font-weight: 600; line-height: 1.2;
  color: var(--bl-text-1);
  white-space: nowrap; flex-shrink: 0;
}
.ot-subtitle {
  font-size: var(--bl-fs-12); color: var(--bl-text-3);
  min-width: 0; flex: 1;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.ot-actions { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.ot-select { width: 180px; height: 30px; }
.ot-search { position: relative; width: 240px; }
.ot-search-ic { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.ot-search-input { padding-left: 30px; padding-right: 28px; height: 30px; }
.ot-search-clear {
  position: absolute; right: 6px; top: 50%; transform: translateY(-50%);
  width: 16px; height: 16px; border: 0; background: var(--bl-bg-3); color: var(--bl-text-3);
  border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; cursor: pointer;
}

/* —— 主体 —— */
.ot-body { flex: 1; position: relative; display: flex; gap: 12px; padding: 12px; overflow: hidden; }
.ot-pane { flex: 1; display: flex; flex-direction: column; min-width: 0; overflow: hidden; }

/* —— 视图切换 tabs:白底条, 与下方列表连为一体的整体白卡片 —— */
.ot-tabs {
  display: flex; gap: 2px;
  padding: 4px 10px 0;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-bottom: 1px solid var(--bl-divider);
  border-radius: var(--bl-radius-3) var(--bl-radius-3) 0 0;
}
.ot-tab {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 8px 14px;
  border: 0; background: transparent;
  color: var(--bl-text-3); cursor: pointer;
  font-size: var(--bl-fs-13);
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;   /* 激活下划线压住页签栏底边线 */
}
.ot-tab:hover { color: var(--bl-text-1); }
.ot-tab.is-on { color: var(--bl-primary); border-bottom-color: var(--bl-primary); font-weight: 600; }

/* —— 列表:平顶(接页签栏)、圆角底 —— */
.ot-list-card {
  flex: 1; margin: 0; display: flex; flex-direction: column;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border); border-top: 0;
  border-radius: 0 0 var(--bl-radius-3) var(--bl-radius-3);
  overflow: hidden; min-height: 0;
}
.ot-list-scroll { flex: 1; min-height: 0; overflow: auto; }
.ot-table { width: 100%; }
.ot-table th, .ot-table td { white-space: nowrap; }
/* 滚动表头固定 */
.ot-table thead th {
  position: sticky; top: 0; z-index: 2;
  background: var(--bl-thead-bg);
}
/* 表头排序 */
.th-sort { cursor: pointer; user-select: none; display: inline-flex; align-items: center; white-space: nowrap; }
.th-sort:hover { color: var(--bl-primary); }
.th-arrow { color: var(--bl-text-4); font-size: 11px; margin-left: 2px; }
.ot-row { cursor: pointer; }
.ot-row:hover { background: var(--bl-bg-hover); }
.ot-row.is-active { background: var(--bl-primary-soft); }
.ot-row.is-grouped td:nth-child(2) { padding-left: 22px; }
/* 分组标题行 */
.ot-group-row { cursor: pointer; background: var(--bl-bg-2); }
.ot-group-row:hover { background: var(--bl-bg-hover); }
.ot-group-row td {
  padding: 7px 12px !important;
  border-bottom: 1px solid var(--bl-border);
}
.ot-group-chev { display: inline-flex; vertical-align: middle; color: var(--bl-text-3); margin-right: 6px; }
.ot-group-label { font-weight: 600; font-size: var(--bl-fs-13); color: var(--bl-text-1); vertical-align: middle; }
.ot-group-count {
  margin-left: 8px;
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-1); border: 1px solid var(--bl-border);
  border-radius: 9px; padding: 0 8px; line-height: 16px;
  vertical-align: middle; font-feature-settings: "tnum";
}
/* 顶部工具栏里的「分组」开关 */
.ot-group-btn {
  display: inline-flex; align-items: center;
  height: 30px; padding: 0 12px;
  border: 1px solid var(--bl-border);
  background: var(--bl-bg-1);
  color: var(--bl-text-2);
  border-radius: var(--bl-radius-2);
  font-size: var(--bl-fs-13);
  cursor: pointer;
  transition: border-color .12s, color .12s, background-color .12s;
}
.ot-group-btn:hover:not(:disabled) { color: var(--bl-primary); border-color: var(--bl-primary); }
.ot-group-btn.is-on {
  background: var(--bl-primary-soft); border-color: var(--bl-primary);
  color: var(--bl-primary); font-weight: 500;
}
.ot-group-btn:disabled { opacity: .5; cursor: not-allowed; }
.ot-ic {
  width: 20px; height: 20px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.ot-ic-lg { width: 36px; height: 36px; border-radius: var(--bl-radius-3); }
.ot-cell-obj { min-width: 220px; max-width: 320px; }
.ot-name-cell { display: flex; align-items: center; gap: 10px; min-width: 0; }
.ot-name-text { min-width: 0; flex: 1; }
.ot-obj-label { font-size: var(--bl-fs-13); color: var(--bl-text-1); font-weight: 500; }
.ot-obj-api { font-size: 11px; }
.ot-link { color: var(--bl-primary); cursor: pointer; }
.ot-link:hover { text-decoration: underline; }
.ot-rid { display: inline-flex; align-items: center; gap: 4px; }

/* —— 分页钉底 —— */
.ot-pager {
  flex-shrink: 0; padding: 8px 12px; border-top: 1px solid var(--bl-divider);
  display: flex; justify-content: space-between; align-items: center;
  font-size: var(--bl-fs-12);
}
.ot-pager-r { display: inline-flex; align-items: center; gap: 4px; }
.ot-page-size { width: 64px; height: 26px; }

/* 批量操作按钮 (删除红 outline / 启用绿 outline / 禁用灰 outline / 取消选择纯文字) */
.ot-del-btn { background: var(--bl-bg-1); border: 1px solid #f53f3f; color: #f53f3f; }
.ot-del-btn:hover { background: #fff1f0; }
.ot-ena-btn { background: var(--bl-bg-1); border: 1px solid #00b42a; color: #00b42a; }
.ot-ena-btn:hover { background: color-mix(in srgb, var(--bl-success) 14%, var(--bl-bg-1)); }
.ot-dis-btn { background: var(--bl-bg-1); border: 1px solid #86909c; color: #4e5969; }
.ot-dis-btn:hover { background: var(--bl-bg-2); }
.ot-clear-btn { color: var(--bl-text-3); }
.ot-clear-btn:hover { color: var(--bl-primary); }

/* —— 卡片网格 —— */
.ot-card-grid {
  flex: 1; margin: 0 ; padding: 0px;
  display: grid; gap: 12px;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  overflow: auto;
  align-content: start;
}
.ot-card {
  background: var(--bl-bg-1); border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3); padding: 12px; cursor: pointer;
  transition: border-color .15s, box-shadow .15s;
  display: flex; flex-direction: column; gap: 10px;
}
.ot-card:hover { border-color: var(--bl-primary); box-shadow: var(--bl-shadow-1); }
.ot-card-hd { display: flex; align-items: center; gap: 10px; }
.ot-card-title { font-size: 14px; font-weight: 600; color: var(--bl-text-1); }
.ot-card-api { font-size: 11px; }
.ot-card-stats {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px;
  padding-top: 10px; border-top: 1px dashed var(--bl-divider);
}
.ot-card-stat { display: flex; flex-direction: column; align-items: center; gap: 2px; font-size: 11px; }
.ot-card-stat b { font-size: 16px; color: var(--bl-text-1); }
.ot-card-ft { font-size: 11px; }

/* —— 图谱占位 —— */
.ot-graph-stub { flex: 1; margin: 0; display: flex; align-items: center; justify-content: center; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: var(--bl-radius-3); }

/* —— 右侧抽屉 (全局层之上,覆盖整个视口高度) —— */
.ot-drawer {
  position: fixed; top: 0; right: 0; bottom: 0;
  background: var(--bl-bg-1);
  border-left: 1px solid var(--bl-border-strong);
  /* 双层阴影 + 顶部 1px 高光描边, 让抽屉立体浮起 */
  box-shadow:
    -12px 0 32px rgba(0,0,0,0.22),
    -2px 0 6px rgba(0,0,0,0.12);
  display: flex; flex-direction: column;
  z-index: 1000;
  min-width: 450px;
}
/* 深色下: 加深阴影 + 左侧 1px 高光描边模拟"光源从上方" */
:root[data-theme="dark"] .ot-drawer {
  box-shadow:
    -16px 0 48px rgba(0,0,0,0.65),
    -2px 0 8px rgba(0,0,0,0.5),
    inset 1px 0 0 rgba(255,255,255,0.05);
}
.ot-drag-handle {
  position: absolute; left: -2px; top: 0; bottom: 0; width: 5px;
  cursor: col-resize; background: transparent;
  transition: background-color .15s;
}
.ot-drag-handle:hover, .ot-drag-handle.is-resizing { background: var(--bl-primary); }

/* ===== 第二层嵌套抽屉 (从对象图谱钻取查看其他对象时浮起在主抽屉之上) =====
   宽度由 inline style 控制 (可拖拽), 默认 520px. 左边缘 5px 拖拽手柄.
   左侧导航较窄 (102px) 节省空间, 简化 tab 集只有 4 项不需要太宽. */
.ot-drawer-nested {
  z-index: 1010;
  /* 强化阴影让"浮起"层次感更明显 */
  box-shadow:
    -20px 0 40px rgba(0,0,0,0.28),
    -4px 0 12px rgba(0,0,0,0.15);
}
.ot-drawer-nested .ot-drawer-body { grid-template-columns: 102px 1fr; }
.ot-drawer-nested .ot-tab-item { padding: 7px 8px 7px 12px; }
:root[data-theme="dark"] .ot-drawer-nested {
  box-shadow:
    -24px 0 56px rgba(0,0,0,0.7),
    -4px 0 12px rgba(0,0,0,0.5),
    inset 1px 0 0 rgba(255,255,255,0.06) !important;
}

/* ===== 多对象 tab 栈条 (用于嵌套抽屉的顶部) ===== */
.ot-tab-bar {
  flex-shrink: 0;
  display: flex; gap: 4px;
  padding: 6px 8px 0;
  background: var(--bl-bg-2);
  border-bottom: 1px solid var(--bl-border);
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: thin;
}
.ot-tab-bar::-webkit-scrollbar { height: 4px; }
.ot-tab-bar::-webkit-scrollbar-thumb { background: var(--bl-border-strong); border-radius: 2px; }
.ot-tab-bar-item {
  flex-shrink: 0;
  display: inline-flex; align-items: center; gap: 6px;
  height: 30px; padding: 0 8px 0 6px;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-bottom: 0;
  border-radius: 6px 6px 0 0;
  font-size: 12.5px; color: var(--bl-text-2);
  cursor: pointer;
  max-width: 220px;
  transition: background-color .12s, color .12s;
  position: relative;
  top: 1px;
}
.ot-tab-bar-item:hover { background: var(--bl-bg-hover); color: var(--bl-text-1); }
.ot-tab-bar-item.is-active {
  background: var(--bl-bg-1);
  color: var(--bl-primary);
  font-weight: 500;
  border-color: var(--bl-border);
  border-bottom: 1px solid var(--bl-bg-1);   /* 与抽屉头融合 */
  z-index: 2;
}
.ot-tab-bar-ic {
  width: 18px; height: 18px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.ot-tab-bar-lbl {
  flex: 1; min-width: 0;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
/* 关闭按钮: 默认不占位 (width:0 overflow:hidden), hover/active 时才展开显示 */
.ot-tab-bar-close {
  flex-shrink: 0;
  height: 18px;
  width: 0;
  margin-left: 0;
  padding: 0;
  border: 0; background: transparent; cursor: pointer;
  color: var(--bl-text-3);
  display: inline-flex; align-items: center; justify-content: center;
  border-radius: 3px;
  opacity: 0;
  overflow: hidden;
  transition: width .12s, margin-left .12s, opacity .12s, background-color .12s, color .12s;
}
.ot-tab-bar-item:hover .ot-tab-bar-close,
.ot-tab-bar-item.is-active .ot-tab-bar-close {
  width: 18px;
  margin-left: 4px;
  opacity: 1;
}
.ot-tab-bar-close:hover { background: var(--bl-bg-hover); color: var(--bl-danger); }

.ot-drawer-hd {
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 10px;
  background: var(--bl-bg-2);
  border-bottom: 1px solid var(--bl-border);
  gap: 8px;
}
.ot-drawer-hd-l { display: flex; align-items: center; gap: 10px; min-width: 0; flex: 1; }
.ot-drawer-hd-r { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.ot-drawer-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; min-width: 0; }
.ot-drawer-sep { width: 1px; height: 18px; background: var(--bl-divider); margin: 0 4px; }

.ot-drawer-body { flex: 1; display: grid; grid-template-columns: 160px 1fr; overflow: hidden; min-height: 0; }
.ot-tab-side {
  background: var(--bl-bg-2); border-right: 1px solid var(--bl-border);
  overflow: auto; padding: 6px 0;
  /* 顶边线:与右侧内容区顶部对齐. 浅色用白(bg-2 与 border 近似色看不见, 需更亮) */
  /* border-top: 1px solid var(--bl-bg-1); */
}
/* 深色: 侧边栏与内容区分界更明显 + 顶边线改用比 navy 底亮的边框色 */
:root[data-theme="dark"] .ot-tab-side {
  box-shadow: inset -1px 0 0 rgba(255,255,255,0.03);
  /* border-top-color: var(--bl-border-strong); */
}
.ot-tab-group { padding: 0; }
/* 分组之间用顶部分隔线区分 (无左侧竖条) */
.ot-tab-group + .ot-tab-group { margin-top: 2px; border-top: 1px solid var(--bl-divider); }
.ot-tab-group-hd {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 12px; cursor: pointer;
  font-size: var(--bl-fs-12); font-weight: 700;
  color: var(--bl-text-1);
  background: var(--bl-bg-hover);    /* 浅模式 #F7F8FA / 暗模式 #2A2D34 */
  letter-spacing: 0.3px;
  user-select: none;
}
.ot-tab-group-hd:hover {
  background: color-mix(in srgb, var(--bl-text-3) 10%, var(--bl-bg-hover));
}
.ot-tab-group-chev {
  display: inline-flex; transition: transform .15s;
  color: var(--bl-text-3);
}
.ot-tab-group-chev.is-open { transform: rotate(90deg); color: var(--bl-primary); }
/* 分组示意图标 (彩色色块,使 4 个分组在视觉上更可区分) */
.ot-tab-group-ic {
  width: 18px; height: 18px; border-radius: 4px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
}
.ot-tab-group-body { display: flex; flex-direction: column; padding: 4px 8px 6px; gap: 1px; }
.ot-tab-item {
  text-align: left; padding: 7px 10px 7px 18px;
  border: 0; background: transparent; cursor: pointer;
  font-size: var(--bl-fs-13); color: var(--bl-text-2);
  border-left: 3px solid transparent;
  border-radius: 4px;
  display: flex; align-items: center; gap: 6px;
}
.ot-tab-label { flex: 1; min-width: 0; }
.ot-tab-cnt {
  font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-hover); border-radius: 9px;
  padding: 0 7px; min-width: 18px; text-align: center;
  flex-shrink: 0;
}
.ot-tab-item:hover { background: var(--bl-bg-1); color: var(--bl-text-1); }
.ot-tab-item:hover .ot-tab-cnt { background: var(--bl-bg-2); color: var(--bl-text-2); }
.ot-tab-item.is-on { background: var(--bl-bg-1); color: var(--bl-primary); font-weight: 500; border-left-color: var(--bl-primary); }
.ot-tab-item.is-on .ot-tab-cnt { background: var(--bl-primary-soft); color: var(--bl-primary); }

.ot-tab-pane { overflow: auto; padding: 10px 10px; min-width: 0; }
/* height:100% 让该容器具有"确定的"高度,使下方 .tab-props 的 height:100% / .pp-canvas 的 flex:1 能正确解析 */
.ot-tab-content { display: flex; flex-direction: column; gap: 8px; height: 100%; }
/* 对象图谱: 去除上下间隙,让 SVG 画布顶满抽屉空间 */
.ot-tab-content.ot-tab-canvas { gap: 0; padding: 0; height: 100%; }
.ot-tab-toolbar { display: flex; align-items: center; gap: 8px;  }
.ot-ds-table td .bl-input-sm { width: 100%; min-width: 84px; }
.ot-ds-join { display: flex; align-items: center; gap: 4px; }
.ot-ds-join select { flex: 1; min-width: 72px; }
.ot-section-title {
  font-size: var(--bl-fs-13); font-weight: 600; color: var(--bl-text-2);
  padding: 12px 0 6px; border-bottom: 1px solid var(--bl-divider); margin-bottom: 8px;
}
.ot-section-title:first-child { padding-top: 0; }
.ot-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 4px 16px; }

.ot-stat-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; }
.ot-stat-card { padding: 8px 10px; background: var(--bl-bg-2); border-radius: var(--bl-radius-2); }
.ot-stat-lbl { font-size: 11px; color: var(--bl-text-3); }
.ot-stat-val { font-size: 18px; font-weight: 600; color: var(--bl-text-1); margin-top: 2px; font-feature-settings: "tnum"; }

/* —— transition —— */
.ot-drawer-enter-active, .ot-drawer-leave-active { transition: transform .25s ease, opacity .2s ease; }
.ot-drawer-enter-from, .ot-drawer-leave-to { transform: translateX(20px); opacity: 0; }
</style>
