<template>
  <div class="page">
    <PageHeader title="枚举类型" subtitle="统一枚举库管理 · 一次定义全系统引用">
      <template #actions>
        <select class="bl-input hd-filter" v-model="filterStatus" title="状态">
          <option value="">全部状态</option>
          <option value="active">启用</option>
          <option value="inactive">禁用</option>
        </select>
        <div class="search-wrap">
          <span class="search-icon" v-html="BL.icon('search', 14)"></span>
          <input class="bl-input search-input" placeholder="搜索枚举名称 / API-NAME" v-model="q" />
        </div>
        <button class="bl-btn bl-btn-primary" @click="openCreateType()">
          <span v-html="BL.icon('plus', 12, '#fff')"></span>
          <span style="margin-left:4px">新建枚举</span>
        </button>
      </template>
    </PageHeader>

    <div class="et-body">
     <div class="et-inner">
      <!-- 左侧：分组 + 枚举树 -->
      <aside class="et-tree">
        <div class="et-tree-hd">
          <span style="font-weight:600">分组 | 枚举</span>
          <button class="bl-btn bl-btn-sm bl-btn-text" title="新建分组" @click="openCreateGroup()" v-html="BL.icon('plus', 12)"></button>
        </div>
        <div class="et-tree-list">
          <div :class="['et-row', !activeId && 'is-active']" @click="selectAll">
            <span v-html="BL.icon('grid', 12)"></span>
            <span style="margin-left:6px">全部枚举</span>
            <span class="et-count">{{ enumTypes.length }}</span>
          </div>
          <template v-for="g in topGroups" :key="g.id">
            <div :class="['et-row et-group', activeGroupId===g.id && 'is-active']" @click="selectGroup(g.id)">
              <span class="et-chev" :class="expanded.has(g.id) && 'is-open'" @click.stop="toggleExpand(g.id)" v-html="BL.icon('chevronRight', 11)"></span>
              <span v-html="BL.icon('folder', 12)"></span>
              <span style="margin-left:6px">{{ g.group_name }}</span>
              <span class="et-count">{{ countInGroup(g.id) }}</span>
            </div>
            <template v-if="expanded.has(g.id)">
              <div v-for="sg in childrenOf(g.id)" :key="sg.id"
                   :class="['et-row et-subgroup', activeGroupId===sg.id && 'is-active']" @click="selectGroup(sg.id)">
                <span class="et-pad"></span>
                <span v-html="BL.icon('folder', 12)"></span>
                <span style="margin-left:6px">{{ sg.group_name }}</span>
                <span class="et-count">{{ countInGroup(sg.id) }}</span>
              </div>
              <div v-for="e in enumsInGroup(g.id)" :key="e.id"
                   :class="['et-row et-enum', activeId===e.id && 'is-active']" @click="selectEnum(e)">
                <span class="et-pad"></span>
                <span class="enum-ic" :style="{ background: enumColor(e) }" v-html="BL.icon(enumIcon(e), 11, '#fff')"></span>
                <span style="margin-left:6px" class="bl-truncate">{{ e.rdfs_label || e.api_name }}</span>
                <span class="et-count">{{ e.item_count || 0 }}</span>
              </div>
            </template>
          </template>
        </div>
      </aside>

      <!-- 右侧：列表 / 详情 -->
      <section class="et-main">
        <!-- 类型列表(未选中具体枚举时) -->
        <template v-if="!current">
          <div class="bl-card list-card">
            <div class="list-scroll">
              <table class="bl-table et-table">
                <thead>
                  <tr>
                    <th><span class="th-sort" @click="toggleTypeSort('name')">名称<span class="th-arrow">{{ typeSortArrow('name') }}</span></span></th>
                    <th><span class="th-sort" @click="toggleTypeSort('api')">API-NAME<span class="th-arrow">{{ typeSortArrow('api') }}</span></span></th>
                    <th><span class="th-sort" @click="toggleTypeSort('kind')">枚举类型<span class="th-arrow">{{ typeSortArrow('kind') }}</span></span></th>
                    <th><span class="th-sort" @click="toggleTypeSort('level')">最大层级<span class="th-arrow">{{ typeSortArrow('level') }}</span></span></th>
                    <th><span class="th-sort" @click="toggleTypeSort('count')">项数<span class="th-arrow">{{ typeSortArrow('count') }}</span></span></th>
                    <th>说明</th>
                    <th><span class="th-sort" @click="toggleTypeSort('status')">状态<span class="th-arrow">{{ typeSortArrow('status') }}</span></span></th>
                    <th style="width:90px"></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="e in filteredTypes" :key="e.id" @click="selectEnum(e)" class="et-trow">
                    <td>
                      <div class="bl-row" style="gap:6px;align-items:center">
                        <span class="enum-ic" :style="{ background: enumColor(e) }" v-html="BL.icon(enumIcon(e), 11, '#fff')"></span>
                        <span style="font-weight:500">{{ e.rdfs_label || e.api_name }}</span>
                      </div>
                    </td>
                    <td><span class="bl-mono">{{ e.api_name }}</span></td>
                    <td><span class="bl-tag">{{ enumTypeLabel(e.enum_type) }}</span></td>
                    <td>{{ e.max_level }}</td>
                    <td><a class="bl-link">{{ e.item_count || 0 }}</a></td>
                    <td class="bl-truncate" style="max-width:260px" :title="e.rdfs_comment">{{ e.rdfs_comment || '—' }}</td>
                    <td><span :class="['bl-tag', e.status === 'active' ? 'bl-tag-success' : 'bl-tag-danger']">{{ e.status === 'active' ? '启用' : '禁用' }}</span></td>
                    <td @click.stop>
                      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="编辑" @click="openEditType(e)" v-html="BL.icon('edit', 12)"></button>
                      <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click="removeType(e)" v-html="BL.icon('trash', 12)"></button>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div v-if="!filteredTypes.length" class="bl-empty" style="padding:48px">未匹配到枚举类型</div>
            </div>
          </div>
        </template>

        <!-- 单枚举详情(三页签) -->
        <template v-else>
          <div class="et-detail">
            <div class="et-detail-hd">
              <div class="bl-row" style="gap:8px;align-items:center;flex:1;min-width:0">
                <span class="enum-ic-lg" :style="{ background: enumColor(current) }" v-html="BL.icon(enumIcon(current), 16, '#fff')"></span>
                <div style="min-width:0">
                  <div class="et-detail-title">{{ current.rdfs_label || current.api_name }}</div>
                  <div class="bl-mono bl-muted" style="font-size:11px">{{ current.api_name }} · {{ enumTypeLabel(current.enum_type) }}</div>
                </div>
                <span :class="['bl-tag', current.status === 'active' ? 'bl-tag-success' : 'bl-tag-danger']">{{ current.status === 'active' ? '启用' : '禁用' }}</span>
              </div>
              <!-- 顶部"编辑"按钮暂时隐藏,使用"基本信息"区右侧的编辑入口代替
              <button class="bl-btn bl-btn-sm" @click="openEditType(current)"><span v-html="BL.icon('edit', 12)"></span><span style="margin-left:4px">编辑</span></button>
              -->
              <button class="bl-btn bl-btn-sm" @click="current=null"><span v-html="BL.icon('arrowLeft', 12)"></span><span style="margin-left:4px">返回列表</span></button>
            </div>
            <div class="et-tabs">
              <button v-for="t in detailTabs" :key="t.k" :class="['et-tab', activeTab===t.k && 'is-on']" @click="onPickTab(t.k)">{{ t.label }}</button>
            </div>
            <div class="et-tab-pane">
              <!-- 详情 -->
              <div v-if="activeTab === 'detail'">
                <div class="sec-row">
                  <div class="sec">基本信息</div>
                  <button class="bl-btn bl-btn-sm bl-btn-text" @click="openEditType(current)">
                    <span v-html="BL.icon('edit', 12)"></span><span style="margin-left:4px">编辑</span>
                  </button>
                </div>
                <div class="kv-grid">
                  <div class="kv"><span class="kv-lbl">枚举名称</span><span>{{ current.rdfs_label || '—' }}</span></div>
                  <div class="kv"><span class="kv-lbl">API-NAME</span><span class="bl-mono">{{ current.api_name }}</span></div>
                  <div class="kv"><span class="kv-lbl">所属领域</span><span>{{ current.category_code || '—' }}</span></div>
                  <div class="kv"><span class="kv-lbl">枚举类型</span><span>{{ enumTypeLabel(current.enum_type) }}</span></div>
                  <div class="kv"><span class="kv-lbl">状态</span>
                    <span :class="['bl-tag', current.status === 'active' ? 'bl-tag-success' : 'bl-tag-danger']">
                      {{ current.status === 'active' ? '启用' : '禁用' }}
                    </span>
                  </div>
                  <div class="kv"><span class="kv-lbl">RID</span><span class="bl-mono bl-muted bl-truncate" :title="current.rid">{{ current.rid }}</span></div>
                  <div class="kv"><span class="kv-lbl">最大层级</span><span>{{ current.max_level }}</span></div>
                  <div class="kv"><span class="kv-lbl">顶级编码</span><span class="bl-mono">{{ current.top_code || 'NULL' }}</span></div>
                  <div class="kv"><span class="kv-lbl">定义来源</span><span>{{ current.rdfs_defined_by || '—' }}</span></div>
                  <div class="kv"><span class="kv-lbl">参考资料</span><span class="bl-truncate" :title="current.rdfs_see_also">{{ current.rdfs_see_also || '—' }}</span></div>
                  <div class="kv" style="grid-column:1/-1"><span class="kv-lbl">说明</span><span>{{ current.rdfs_comment || '—' }}</span></div>
                </div>

                <div class="sec-row">
                  <div class="sec">编码管理</div>
                  <template v-if="!editingRules">
                    <button class="bl-btn bl-btn-sm bl-btn-text" @click="startEditRules">
                      <span v-html="BL.icon('edit', 12)"></span><span style="margin-left:4px">编辑规则</span>
                    </button>
                  </template>
                  <template v-else>
                    <button class="bl-btn bl-btn-sm" @click="cancelEditRules">取消</button>
                    <button class="bl-btn bl-btn-sm bl-btn-primary" @click="saveRules">保存</button>
                  </template>
                </div>

                <!-- 子页签: 配置 / 示例 -->
                <div class="sub-tabs">
                  <button :class="['sub-tab', codeSubTab==='config' && 'is-on']" @click="codeSubTab='config'">配置</button>
                  <button :class="['sub-tab', codeSubTab==='example' && 'is-on']" @click="codeSubTab='example'">示例</button>
                </div>

                <!-- 配置子页签 -->
                <div v-if="codeSubTab === 'config'">
                  <table class="bl-table mini-table">
                    <thead>
                      <tr>
                        <th style="width:60px">层次</th>
                        <th>名称</th>
                        <th style="width:90px">编码长度</th>
                        <th style="width:80px">总长度</th>
                        <th style="width:70px">补位符</th>
                        <th style="width:140px">补位位置</th>
                        <th v-if="editingRules" style="width:48px"></th>
                      </tr>
                    </thead>
                    <tbody v-if="!editingRules">
                      <tr v-for="r in levelRules" :key="r.id">
                        <td>{{ r.rule_level }}</td>
                        <td>{{ r.code_name }}</td>
                        <td>{{ r.code_len }}</td>
                        <td>{{ r.total_len }}</td>
                        <td><span class="bl-mono">{{ r.fill_char }}</span></td>
                        <td>{{ r.fill_pos === 0 ? '前补' : '后补' }}</td>
                      </tr>
                    </tbody>
                    <tbody v-else>
                      <tr v-for="(r, idx) in ruleDraft" :key="idx">
                        <td>{{ idx + 1 }}</td>
                        <td><input class="bl-input bl-input-xs" v-model="r.code_name" placeholder="层级名称" /></td>
                        <td><input type="number" class="bl-input bl-input-xs" v-model.number="r.code_len" min="0" /></td>
                        <td><input type="number" class="bl-input bl-input-xs" v-model.number="r.total_len" min="0" /></td>
                        <td><input class="bl-input bl-input-xs bl-mono" v-model="r.fill_char" maxlength="1" /></td>
                        <td>
                          <div class="bl-switch-wrap">
                            <span :class="['sw-side', r.fill_pos === 0 && 'is-active']">前补</span>
                            <label class="bl-switch">
                              <input type="checkbox" :checked="r.fill_pos === 1" @change="r.fill_pos = $event.target.checked ? 1 : 0" />
                              <span class="bl-switch-slider"></span>
                            </label>
                            <span :class="['sw-side', r.fill_pos === 1 && 'is-active']">后补</span>
                          </div>
                        </td>
                        <td @click.stop>
                          <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click="removeRuleLevel(idx)" v-html="BL.icon('trash', 12)"></button>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                  <div v-if="editingRules" style="margin-top:6px">
                    <button class="bl-btn bl-btn-sm bl-btn-text" @click="addRuleLevel">
                      <span v-html="BL.icon('plus', 12)"></span><span style="margin-left:4px">新增层级</span>
                    </button>
                  </div>
                  <div v-if="!editingRules && !levelRules.length" class="bl-empty" style="padding:24px">尚未配置层次编码规则</div>
                </div>

                <!-- 示例子页签 -->
                <div v-if="codeSubTab === 'example'">
                  <table class="bl-table mini-table">
                    <thead>
                      <tr>
                        <th style="width:60px">层次</th>
                        <th>层级名称</th>
                        <th>名称示例</th>
                        <th>编码示例</th>
                        <th style="width:90px">示例总长度</th>
                        <th>规则说明</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="ex in codeExamples" :key="ex.rule_level">
                        <td>{{ ex.rule_level }}</td>
                        <td>{{ ex.name_example }}</td>
                        <td class="bl-muted">—</td>
                        <td><span class="bl-mono">{{ ex.code_example }}</span></td>
                        <td>{{ ex.total_len }}位</td>
                        <td class="bl-muted">{{ ex.rule_desc }}</td>
                      </tr>
                    </tbody>
                  </table>
                  <div v-if="!codeExamples.length" class="bl-empty" style="padding:24px">配置层次规则后此处自动生成示例</div>
                </div>
              </div>

              <!-- 数据 -->
              <div v-if="activeTab === 'data'">
                <div class="bl-row" style="gap:8px;margin-bottom:8px;align-items:center">
                  <div class="seg" style="flex-shrink:0">
                    <button :class="['seg-btn', dataView==='tree' && 'is-on']" @click="dataView='tree'">
                      <span v-html="BL.icon('layers', 11)"></span><span style="margin-left:4px">树视图</span>
                    </button>
                    <button :class="['seg-btn', dataView==='table' && 'is-on']" @click="dataView='table'">
                      <span v-html="BL.icon('list', 11)"></span><span style="margin-left:4px">表格视图</span>
                    </button>
                  </div>
                  <input class="bl-input" placeholder="关键词查询" v-model="itemQ" style="width:200px" />
                  <select class="bl-input" v-model="itemFilterStatus" style="width:120px">
                    <option value="">全部状态</option>
                    <option value="active">启用</option>
                    <option value="inactive">禁用</option>
                  </select>
                  <div class="bl-grow"></div>
                  <button class="bl-btn bl-btn-sm bl-btn-primary" @click="addItemRow">
                    <span v-html="BL.icon('plus', 12, '#fff')"></span><span style="margin-left:4px">新增项</span>
                  </button>
                </div>

                <!-- 树视图 -->
                <div v-if="dataView === 'tree'" class="item-tree">
                  <ul class="item-tree-root">
                    <ItemTreeNode v-for="n in itemTree" :key="n.id" :node="n" />
                  </ul>
                  <div v-if="!itemTree.length" class="bl-empty" style="padding:24px">尚无枚举项,点击「新增项」添加</div>
                </div>

                <!-- 表格视图 -->
                <template v-if="dataView === 'table'">
                <table class="bl-table mini-table item-table">
                  <thead>
                    <tr>
                      <th><span class="th-sort" @click="toggleItemSort('label')">名称<span class="th-arrow">{{ itemSortArrow('label') }}</span></span></th>
                      <th><span class="th-sort" @click="toggleItemSort('api')">代码<span class="th-arrow">{{ itemSortArrow('api') }}</span></span></th>
                      <th><span class="th-sort" @click="toggleItemSort('code')">编码<span class="th-arrow">{{ itemSortArrow('code') }}</span></span></th>
                      <th><span class="th-sort" @click="toggleItemSort('parent')">上级编码<span class="th-arrow">{{ itemSortArrow('parent') }}</span></span></th>
                      <th style="width:60px"><span class="th-sort" @click="toggleItemSort('level')">层级<span class="th-arrow">{{ itemSortArrow('level') }}</span></span></th>
                      <th style="width:90px"><span class="th-sort" @click="toggleItemSort('status')">状态<span class="th-arrow">{{ itemSortArrow('status') }}</span></span></th>
                      <th style="width:80px"></th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="it in filteredItems" :key="it.id" :class="['item-row', it._editing && 'is-editing']" @dblclick="editItemRow(it)">
                      <td>
                        <input v-if="it._editing" class="bl-input bl-input-xs" v-model="it.label" placeholder="名称" />
                        <span v-else>{{ it.label }}</span>
                      </td>
                      <td>
                        <input v-if="it._editing" class="bl-input bl-input-xs bl-mono" v-model="it.api_name" placeholder="snake_case" />
                        <span v-else class="bl-mono">{{ it.api_name || '—' }}</span>
                      </td>
                      <td>
                        <input v-if="it._editing" class="bl-input bl-input-xs bl-mono" v-model="it.code" placeholder="编码" />
                        <span v-else class="bl-mono">{{ it.code }}</span>
                      </td>
                      <td>
                        <input v-if="it._editing" class="bl-input bl-input-xs bl-mono" v-model="it.parent_code" placeholder="NULL" />
                        <span v-else class="bl-mono bl-muted">{{ it.parent_code || 'NULL' }}</span>
                      </td>
                      <td>
                        <input v-if="it._editing" type="number" class="bl-input bl-input-xs" v-model.number="it.level" min="1" max="10" />
                        <span v-else>{{ it.level }}</span>
                      </td>
                      <td>
                        <select v-if="it._editing" class="bl-input bl-input-xs" v-model="it.status">
                          <option value="active">启用</option>
                          <option value="inactive">禁用</option>
                        </select>
                        <span v-else :class="['bl-tag', it.status === 'active' ? 'bl-tag-success' : 'bl-tag-danger']">{{ it.status === 'active' ? '启用' : '禁用' }}</span>
                      </td>
                      <td @click.stop>
                        <div v-if="it._editing" class="bl-row" style="gap:0">
                          <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="保存" @click="saveItemRow(it)" v-html="BL.icon('check', 13)"></button>
                          <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="取消" @click="cancelItemRow(it)" v-html="BL.icon('x', 13)"></button>
                        </div>
                        <template v-else>
                          <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="编辑" @click="editItemRow(it)" v-html="BL.icon('edit', 12)"></button>
                          <button class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon" title="删除" @click="removeItem(it)" v-html="BL.icon('trash', 12)"></button>
                        </template>
                      </td>
                    </tr>
                  </tbody>
                </table>
                <div v-if="!filteredItems.length" class="bl-empty" style="padding:24px">尚无枚举项,点击「新增项」添加</div>
                </template>
              </div>

              <!-- 同步规则 -->
              <div v-if="activeTab === 'sync'">
                <div class="sync-form">
                  <FieldRow label="数据源类型 *" inline>
                    <select class="bl-input" v-model="syncForm.data_source_id">
                      <option value="">— 请选择 —</option>
                      <option value="ds-mysql-main">MySQL 数据库</option>
                      <option value="ds-oracle-main">Oracle 数据库</option>
                      <option value="ds-http">HTTP 接口</option>
                    </select>
                  </FieldRow>
                  <FieldRow label="数据表 *" inline>
                    <div class="bl-row" style="gap:8px;flex:1">
                      <input class="bl-input" v-model="syncForm.table_alias" placeholder="备注名 (例: 行政区划表)" />
                      <input class="bl-input bl-mono" v-model="syncForm.table_name" placeholder="表代码名 (例: t_area)" />
                    </div>
                  </FieldRow>
                  <FieldRow label="字段配置" inline>
                    <div class="bl-row" style="gap:8px;flex:1">
                      <input class="bl-input" v-model="syncForm.field_code" placeholder="编码字段 (area_code)" />
                      <input class="bl-input" v-model="syncForm.field_name" placeholder="名称字段 (area_name)" />
                    </div>
                  </FieldRow>
                  <FieldRow label="排序字段" inline>
                    <input class="bl-input" v-model="syncForm.field_sort" placeholder="排序依据字段 (默认按编码)" />
                  </FieldRow>
                  <FieldRow label="状态字段" inline>
                    <input class="bl-input" v-model="syncForm.field_status" placeholder="筛选启用/禁用的字段" />
                  </FieldRow>
                  <FieldRow label="顶级筛选表达式" inline>
                    <input class="bl-input bl-mono" v-model="syncForm.filter_sql" placeholder="例: parent_code IS NULL" />
                  </FieldRow>
                  <FieldRow label="同步模式" inline>
                    <select class="bl-input" v-model="syncForm.sync_mode">
                      <option value="level_diff">逐级对比同步,以业务系统数据为准</option>
                      <option value="full_overwrite">全量覆盖</option>
                      <option value="incremental">增量更新</option>
                    </select>
                  </FieldRow>
                  <FieldRow label="同步策略" inline>
                    <select class="bl-input" v-model="syncForm.sync_strategy">
                      <option value="once">一次性同步</option>
                      <option value="daily">每日定时</option>
                      <option value="weekly">每周定时</option>
                      <option value="monthly">每月定时</option>
                    </select>
                  </FieldRow>

                  <div class="sync-actions">
                    <button class="bl-btn" @click="testConnection">
                      <span v-html="BL.icon('zap', 12)"></span><span style="margin-left:4px">测试连接</span>
                    </button>
                    <button class="bl-btn bl-btn-primary" :disabled="syncRunning" @click="runSync">
                      <span v-html="BL.icon('refresh', 12, '#fff')"></span><span style="margin-left:4px">{{ syncRunning ? '同步中...' : '立即执行同步' }}</span>
                    </button>
                    <button class="bl-btn" @click="openSyncLogs">
                      <span v-html="BL.icon('fileText', 12)"></span><span style="margin-left:4px">查看同步日志</span>
                    </button>
                    <div class="bl-grow"></div>
                    <button class="bl-btn bl-btn-primary" @click="saveSyncConfig">保存配置</button>
                  </div>
                </div>
              </div>

              <!-- 引用 -->
              <div v-if="activeTab === 'ref'">
                <div class="bl-row" style="gap:8px;align-items:center;margin-bottom:8px">
                  <div style="font-weight:500">被引用列表 (共 {{ refs.length }} 条)</div>
                  <div class="bl-grow"></div>
                  <input class="bl-input" placeholder="按对象名/属性名搜索" v-model="refQ" style="width:240px" />
                </div>
                <table class="bl-table mini-table">
                  <thead>
                    <tr>
                      <th style="width:120px">引用类型</th>
                      <th>对象/接口名</th>
                      <th>引用属性名</th>
                      <th>值类型</th>
                      <th style="width:64px"></th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(r, i) in filteredRefs" :key="i">
                      <td><span class="bl-tag">{{ refTypeLabel(r.ref_type) }}</span></td>
                      <td>
                        <div>{{ r.object_label || '—' }}</div>
                        <div class="bl-mono bl-muted" style="font-size:11px">{{ r.object_api || '—' }}</div>
                      </td>
                      <td>
                        <div>{{ r.prop_label || '—' }}</div>
                        <div class="bl-mono bl-muted" style="font-size:11px">{{ r.prop_api || '—' }}</div>
                      </td>
                      <td><span class="bl-tag">{{ r.value_type_label || r.value_type_api }}</span></td>
                      <td><a class="bl-link bl-btn-text bl-btn-sm">查看</a></td>
                    </tr>
                  </tbody>
                </table>
                <div v-if="!filteredRefs.length" class="bl-empty" style="padding:24px">尚未被任何对象/接口引用</div>
              </div>
            </div>
          </div>
        </template>
      </section>
     </div>

      <!-- 枚举类型新建/编辑抽屉 (浮在右侧主区上方，上下贴边) -->
      <transition name="et-drawer">
        <aside v-if="typeFormOpen" class="et-drawer" :style="{ width: drawerWidth + 'px' }">
          <div class="et-drag-handle" @mousedown="onDragStart" :class="resizing && 'is-resizing'"></div>
          <div class="et-drawer-hd">
            <div class="bl-row" style="gap:8px;flex:1;min-width:0">
              <span class="et-drawer-ic" v-html="BL.icon('layers', 18, '#fff')"></span>
              <div class="bl-grow" style="min-width:0">
                <div class="et-drawer-title">{{ typeForm.id ? '编辑枚举类型' : '新建枚举类型' }}</div>
                <div class="bl-mono bl-muted" style="font-size:11px">{{ typeForm.api_name || '—' }}</div>
              </div>
            </div>
            <button class="bl-btn bl-btn-text bl-btn-icon" @click="typeFormOpen=false" v-html="BL.icon('x', 14)"></button>
          </div>
          <div class="et-drawer-body">
            <div class="sec">元数据</div>
            <FieldRow label="名称 *" inline><input class="bl-input" v-model="typeForm.rdfs_label" /></FieldRow>
            <FieldRow label="API *" inline hint="snake_case · 全局唯一 · 创建后不可改">
              <input class="bl-input bl-mono" v-model="typeForm.api_name" :disabled="!!typeForm.id" />
            </FieldRow>
            <FieldRow label="所属分组" inline>
              <select class="bl-input" v-model="typeForm.group_id">
                <option value="">— 顶级 —</option>
                <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.group_name }}</option>
              </select>
            </FieldRow>
            <FieldRow label="枚举类型" inline>
              <select class="bl-input" v-model="typeForm.enum_type">
                <option value="general_single">一级通用</option>
                <option value="general_multi">多级通用</option>
                <option value="biz_single">业务一级</option>
                <option value="biz_multi">业务多级</option>
              </select>
            </FieldRow>
            <FieldRow label="最大层级" inline><input type="number" class="bl-input" v-model.number="typeForm.max_level" min="1" max="10" /></FieldRow>

            <div class="sec">注释 / 来源</div>
            <FieldRow label="描述"><textarea class="bl-textarea" rows="2" v-model="typeForm.rdfs_comment"></textarea></FieldRow>
            <FieldRow label="参考资料"><input class="bl-input" v-model="typeForm.rdfs_see_also" placeholder="rdfs:seeAlso" /></FieldRow>
            <FieldRow label="定义来源"><input class="bl-input" v-model="typeForm.rdfs_defined_by" placeholder="rdfs:isDefinedBy" /></FieldRow>

            <div class="sec">状态</div>
            <FieldRow label="启用 / 禁用" inline>
              <div class="radio-group">
                <label class="radio-item"><input type="radio" value="active" v-model="typeForm.status" /> 启用</label>
                <label class="radio-item"><input type="radio" value="inactive" v-model="typeForm.status" /> 禁用</label>
              </div>
            </FieldRow>
          </div>
          <div class="et-drawer-ft">
            <button class="bl-btn" @click="typeFormOpen=false">取消</button>
            <button class="bl-btn bl-btn-primary" @click="submitType">保存</button>
          </div>
        </aside>
      </transition>
    </div>


    <!-- 新建分组弹窗 -->
    <div v-if="groupFormOpen" class="bl-modal-mask" @click.self="groupFormOpen=false">
      <div class="bl-modal" style="width:420px">
        <div class="bl-modal-hd">新建分组</div>
        <div class="bl-modal-body bl-col" style="gap:10px">
          <FieldRow label="分组名称 *" inline><input class="bl-input" v-model="groupForm.group_name" /></FieldRow>
          <FieldRow label="父分组" inline>
            <select class="bl-input" v-model="groupForm.parent_id">
              <option value="">— 顶级 —</option>
              <option v-for="g in topGroups" :key="g.id" :value="g.id">{{ g.group_name }}</option>
            </select>
          </FieldRow>
        </div>
        <div class="bl-modal-ft">
          <button class="bl-btn" @click="groupFormOpen=false">取消</button>
          <button class="bl-btn bl-btn-primary" @click="submitGroup">保存</button>
        </div>
      </div>
    </div>

    <!-- 同步日志弹窗 -->
    <div v-if="syncLogsOpen" class="bl-modal-mask" @click.self="syncLogsOpen=false">
      <div class="bl-modal" style="width:780px;max-width:90vw">
        <div class="bl-modal-hd">
          同步日志 <span class="bl-muted" style="font-size:12px;margin-left:6px">{{ current?.rdfs_label || current?.api_name }}</span>
        </div>
        <div class="bl-modal-body" style="max-height:60vh;overflow:auto;padding:0">
          <table class="bl-table mini-table">
            <thead>
              <tr>
                <th style="width:160px">时间</th>
                <th style="width:70px">类型</th>
                <th style="width:60px">+</th>
                <th style="width:60px">~</th>
                <th style="width:60px">-</th>
                <th style="width:60px">失败</th>
                <th style="width:80px">状态</th>
                <th>异常 / 操作人</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="l in syncLogs" :key="l.id">
                <td class="bl-mono" style="font-size:11px">{{ l.sync_time }}</td>
                <td><span class="bl-tag">{{ l.sync_type === 'auto' ? '定时' : '手动' }}</span></td>
                <td>{{ l.add_count }}</td>
                <td>{{ l.update_count }}</td>
                <td>{{ l.del_count }}</td>
                <td>{{ l.fail_count }}</td>
                <td>
                  <span :class="['bl-tag', l.sync_status === 'success' ? 'bl-tag-success' : l.sync_status === 'failed' ? 'bl-tag-danger' : 'bl-tag-warning']">
                    {{ l.sync_status === 'success' ? '成功' : l.sync_status === 'failed' ? '失败' : '执行中' }}
                  </span>
                </td>
                <td class="bl-truncate" style="max-width:200px" :title="l.error_msg || l.oper_user">{{ l.error_msg || l.oper_user || '—' }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="!syncLogs.length" class="bl-empty" style="padding:30px">暂无同步日志</div>
        </div>
        <div class="bl-modal-ft">
          <button class="bl-btn" @click="syncLogsOpen=false">关闭</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, h } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import FieldRow from '@/views/config/category/FieldRow.vue'
import { BL } from '@/lib/bl.js'
import { enumTypeApi } from '@/api'

/* —— 数据 Tab 树节点 (递归) —— */
const ItemTreeNode = {
  name: 'ItemTreeNode',
  props: ['node'],
  setup(props) {
    const open = ref(true)
    return () => {
      const n = props.node
      const kids = n.children || []
      return h('li', { class: 'item-tnode' }, [
        h('div', { class: 'item-tn-row' }, [
          kids.length
            ? h('span', { class: ['item-tn-chev', open.value && 'is-open'], onClick: () => (open.value = !open.value), innerHTML: BL.icon('chevronRight', 11) })
            : h('span', { class: 'item-tn-pad' }),
          h('span', { class: 'item-tn-code bl-mono' }, n.code),
          h('span', { class: 'item-tn-label' }, n.label),
          h('span', { class: ['bl-tag', n.status === 'active' ? 'bl-tag-success' : 'bl-tag-danger'] }, n.status === 'active' ? '启用' : '禁用')
        ]),
        kids.length && open.value ? h('ul', { class: 'item-tn-kids' }, kids.map(c => h(ItemTreeNode, { node: c, key: c.id }))) : null
      ])
    }
  }
}

const groups = ref([])
const enumTypes = ref([])
const items = ref([])
const levelRules = ref([])

const q = ref('')
const filterStatus = ref('')
const itemQ = ref('')
const itemFilterStatus = ref('')

const activeGroupId = ref(null)
const activeId = ref(null)
const current = ref(null)
const activeTab = ref('detail')
const expanded = ref(new Set())

const detailTabs = [
  { k: 'detail', label: '详情' },
  { k: 'data',   label: '数据' },
  { k: 'sync',   label: '同步规则' },
  { k: 'ref',    label: '引用' }
]

/* —— 编码管理 配置 / 示例 子页签 + 行内编辑 —— */
const codeSubTab = ref('config')           // 'config' | 'example'
const editingRules = ref(false)            // 是否处于"编辑层次规则"模式
const ruleDraft = ref([])                  // 编辑期间的副本,保存时整体提交

/* —— 数据 Tab 视图模式 —— */
const dataView = ref('table')              // 'table' | 'tree'

/* —— 同步规则 表单 + 日志 —— */
const syncForm = reactive({
  data_source_id: '', table_alias: '', table_name: '',
  field_code: '', field_name: '', field_sort: '', field_status: '',
  filter_sql: '', sync_mode: 'level_diff', sync_strategy: 'once'
})
const syncLogs = ref([])
const syncLogsOpen = ref(false)
const syncRunning = ref(false)

/* —— 引用 Tab —— */
const refs = ref([])
const refQ = ref('')
const filteredRefs = computed(() => {
  const k = refQ.value.trim().toLowerCase()
  if (!k) return refs.value
  return refs.value.filter(r => [r.object_label, r.object_api, r.prop_label, r.prop_api, r.value_type_label]
    .filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
})

const topGroups = computed(() => groups.value.filter(g => !g.parent_id))
function childrenOf(id) { return groups.value.filter(g => g.parent_id === id) }
function countInGroup(id) {
  const allKids = new Set([id, ...childrenOf(id).map(c => c.id)])
  return enumTypes.value.filter(e => allKids.has(e.group_id)).length
}
function enumsInGroup(id) {
  return enumTypes.value.filter(e => e.group_id === id)
}

/* —— 表头排序: 类型列表 —— */
const typeSortKey = ref('')
const typeSortDir = ref('')
function toggleTypeSort(key) {
  if (typeSortKey.value !== key) { typeSortKey.value = key; typeSortDir.value = 'asc' }
  else if (typeSortDir.value === 'asc') typeSortDir.value = 'desc'
  else { typeSortKey.value = ''; typeSortDir.value = '' }
}
function typeSortArrow(key) {
  if (typeSortKey.value !== key) return ' ⇅'
  return typeSortDir.value === 'asc' ? ' ↑' : ' ↓'
}

/* —— 表头排序: 枚举项 —— */
const itemSortKey = ref('')
const itemSortDir = ref('')
function toggleItemSort(key) {
  if (itemSortKey.value !== key) { itemSortKey.value = key; itemSortDir.value = 'asc' }
  else if (itemSortDir.value === 'asc') itemSortDir.value = 'desc'
  else { itemSortKey.value = ''; itemSortDir.value = '' }
}
function itemSortArrow(key) {
  if (itemSortKey.value !== key) return ' ⇅'
  return itemSortDir.value === 'asc' ? ' ↑' : ' ↓'
}

const filteredTypes = computed(() => {
  let list = enumTypes.value
  if (activeGroupId.value) {
    const allKids = new Set([activeGroupId.value, ...childrenOf(activeGroupId.value).map(c => c.id)])
    list = list.filter(e => allKids.has(e.group_id))
  }
  if (filterStatus.value) list = list.filter(e => e.status === filterStatus.value)
  const k = q.value.trim().toLowerCase()
  if (k) list = list.filter(e => [e.api_name, e.rdfs_label, e.rdfs_comment].filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
  if (typeSortKey.value && typeSortDir.value) {
    const dir = typeSortDir.value === 'asc' ? 1 : -1
    list = [...list].sort((a, b) => {
      let va, vb
      switch (typeSortKey.value) {
        case 'name':   va = a.rdfs_label || a.api_name || ''; vb = b.rdfs_label || b.api_name || ''; break
        case 'api':    va = a.api_name || ''; vb = b.api_name || ''; break
        case 'kind':   va = a.enum_type || ''; vb = b.enum_type || ''; break
        case 'level':  va = a.max_level || 0; vb = b.max_level || 0; break
        case 'count':  va = a.item_count || 0; vb = b.item_count || 0; break
        case 'status': va = a.status || ''; vb = b.status || ''; break
        default:       va = ''; vb = ''
      }
      if (typeof va === 'number') return (va - vb) * dir
      return String(va).localeCompare(String(vb)) * dir
    })
  }
  return list
})

const filteredItems = computed(() => {
  let list = items.value
  if (itemFilterStatus.value) list = list.filter(it => it.status === itemFilterStatus.value)
  const k = itemQ.value.trim().toLowerCase()
  if (k) list = list.filter(it => [it.label, it.code, it.api_name].filter(Boolean).some(s => String(s).toLowerCase().includes(k)))
  if (itemSortKey.value && itemSortDir.value) {
    const dir = itemSortDir.value === 'asc' ? 1 : -1
    list = [...list].sort((a, b) => {
      let va, vb
      switch (itemSortKey.value) {
        case 'label':  va = a.label || ''; vb = b.label || ''; break
        case 'api':    va = a.api_name || ''; vb = b.api_name || ''; break
        case 'code':   va = a.code || ''; vb = b.code || ''; break
        case 'parent': va = a.parent_code || ''; vb = b.parent_code || ''; break
        case 'level':  va = a.level || 0; vb = b.level || 0; break
        case 'status': va = a.status || ''; vb = b.status || ''; break
        default:       va = ''; vb = ''
      }
      if (typeof va === 'number') return (va - vb) * dir
      return String(va).localeCompare(String(vb)) * dir
    })
  }
  return list
})

function enumTypeLabel(t) {
  return ({ general_single: '一级通用', general_multi: '多级通用', biz_single: '业务一级', biz_multi: '业务多级' })[t] || t
}
function enumIcon(e) {
  const t = e.enum_type || 'general_single'
  return t.endsWith('_multi') ? 'layers' : 'list'
}
function enumColor(e) {
  if (e.status !== 'active') return '#86909C'
  const t = e.enum_type || 'general_single'
  if (t.startsWith('biz')) return '#FF7D00'
  return '#165DFF'
}

async function loadAll() {
  groups.value = await enumTypeApi.listGroups().catch(() => [])
  enumTypes.value = await enumTypeApi.list().catch(() => [])
  // 默认展开所有顶级
  expanded.value = new Set(topGroups.value.map(g => g.id))
}
async function loadDetail(id) {
  const d = await enumTypeApi.get(id).catch(() => null)
  if (!d) { current.value = null; items.value = []; levelRules.value = []; return }
  current.value = d
  items.value = d.items || []
  levelRules.value = d.levelRules || []
}

onMounted(loadAll)

function toggleExpand(id) {
  const s = new Set(expanded.value)
  s.has(id) ? s.delete(id) : s.add(id)
  expanded.value = s
}
function selectAll() { activeGroupId.value = null; current.value = null }
function selectGroup(id) { activeGroupId.value = id; current.value = null }
function selectEnum(e) {
  activeId.value = e.id; activeTab.value = 'detail'
  codeSubTab.value = 'config'; editingRules.value = false
  dataView.value = 'table'
  refs.value = []; syncLogs.value = []
  resetSyncForm()
  loadDetail(e.id)
}

/* —— 切换 Tab 时按需加载数据 —— */
function onPickTab(k) {
  activeTab.value = k
  if (!current.value) return
  if (k === 'sync') loadSyncConfig()
  if (k === 'ref')  loadReferences()
}

/* —— 层次编码规则 编辑 / 示例 —— */
function startEditRules() {
  ruleDraft.value = JSON.parse(JSON.stringify(levelRules.value || []))
  editingRules.value = true
}
function cancelEditRules() { editingRules.value = false; ruleDraft.value = [] }
function addRuleLevel() {
  const next = ruleDraft.value.length + 1
  ruleDraft.value.push({ rule_level: next, code_name: `层级${next}`, code_len: 2, total_len: 2, code_separator: '', fill_char: '0', fill_pos: 0 })
}
function removeRuleLevel(idx) { ruleDraft.value.splice(idx, 1) }
async function saveRules() {
  // 规整层级号 + 默认值
  ruleDraft.value.forEach((r, i) => {
    r.rule_level = i + 1
    r.code_len = Number(r.code_len || 0)
    r.total_len = Number(r.total_len || r.code_len || 0)
    r.fill_pos = Number(r.fill_pos || 0)
    r.code_separator = r.code_separator || ''
    r.fill_char = r.fill_char || '0'
  })
  await enumTypeApi.saveLevelRules(current.value.id, ruleDraft.value)
  BL.success('编码规则已保存')
  editingRules.value = false
  await loadDetail(current.value.id)
}
/* 根据规则推算示例: 北京市 110000 → 11 + 00 + 00 + ... */
const codeExamples = computed(() => {
  const rules = (editingRules.value ? ruleDraft.value : levelRules.value) || []
  let acc = ''
  return rules.map(r => {
    // 用占位字符 X 演示编码长度
    const seg = String(r.code_len > 0 ? '1'.padStart(r.code_len, '0') : '')
    acc = acc ? (r.code_separator ? acc + r.code_separator + seg : acc + seg) : seg
    const padded = r.fill_pos === 1
      ? acc.padEnd(r.total_len || acc.length, r.fill_char || '0')
      : acc.padStart(r.total_len || acc.length, r.fill_char || '0')
    return {
      rule_level: r.rule_level,
      name_example: r.code_name || `层级${r.rule_level}`,
      code_example: padded,
      total_len: r.total_len || padded.length,
      rule_desc: `单段${r.code_len}位、总长${r.total_len || r.code_len}位、补位${r.fill_char || '0'}、${r.fill_pos === 1 ? '后补' : '前补'}`
    }
  })
})

/* —— 数据 Tab: 构建树视图 —— */
const itemTree = computed(() => {
  const list = filteredItems.value
  if (!list.length) return []
  const byCode = new Map()
  list.forEach(x => byCode.set(x.code, { ...x, children: [] }))
  const roots = []
  list.forEach(x => {
    const node = byCode.get(x.code)
    if (x.parent_code && byCode.has(x.parent_code)) byCode.get(x.parent_code).children.push(node)
    else roots.push(node)
  })
  return roots
})

/* —— 同步规则 —— */
function resetSyncForm() {
  Object.assign(syncForm, {
    data_source_id: '', table_alias: '', table_name: '',
    field_code: '', field_name: '', field_sort: '', field_status: '',
    filter_sql: '', sync_mode: 'level_diff', sync_strategy: 'once'
  })
}
async function loadSyncConfig() {
  if (!current.value) return
  const cfg = await enumTypeApi.getSyncConfig(current.value.id).catch(() => null)
  if (cfg && cfg.id) Object.assign(syncForm, cfg)
  else resetSyncForm()
}
async function saveSyncConfig() {
  if (!syncForm.data_source_id || !syncForm.table_name) { BL.warning('数据源、表名为必填'); return }
  await enumTypeApi.saveSyncConfig(current.value.id, { ...syncForm })
  BL.success('同步配置已保存')
  await loadSyncConfig()
}
async function testConnection() {
  const r = await enumTypeApi.testSync(current.value.id, { data_source_id: syncForm.data_source_id }).catch(() => null)
  if (r && r.ok) BL.success(r.message || '连接测试通过')
  else BL.error(r?.message || '连接测试失败')
}
async function runSync() {
  syncRunning.value = true
  try {
    const r = await enumTypeApi.runSync(current.value.id, { sync_type: 'manual', oper_user: 'admin' })
    BL.success(`同步完成: +${r.add_count || 0} / ~${r.update_count || 0} / -${r.del_count || 0}`)
    await loadDetail(current.value.id)
  } catch (e) { BL.error(e?.msg || '同步失败') }
  finally { syncRunning.value = false }
}
async function openSyncLogs() {
  syncLogs.value = await enumTypeApi.listSyncLogs(current.value.id).catch(() => [])
  syncLogsOpen.value = true
}
function syncModeLabel(m) {
  return ({ level_diff: '逐级对比同步', full_overwrite: '全量覆盖', incremental: '增量更新' })[m] || m
}
function syncStrategyLabel(s) {
  return ({ once: '一次性同步', daily: '每日定时', weekly: '每周定时', monthly: '每月定时' })[s] || s
}

/* —— 引用 —— */
async function loadReferences() {
  refs.value = await enumTypeApi.listReferences(current.value.id).catch(() => [])
}
function refTypeLabel(t) {
  return ({ class_prop: '对象类型', interface_prop: '接口' })[t] || t
}

/* —— 类型抽屉 —— */
const typeFormOpen = ref(false)
const typeForm = reactive({})
function openCreateType() {
  Object.keys(typeForm).forEach(k => delete typeForm[k])
  Object.assign(typeForm, { enum_type: 'general_single', max_level: 1, status: 'active', group_id: activeGroupId.value || '' })
  ensureDrawerSize()
  typeFormOpen.value = true
}
function openEditType(e) {
  Object.keys(typeForm).forEach(k => delete typeForm[k])
  Object.assign(typeForm, e)
  ensureDrawerSize()
  typeFormOpen.value = true
}

/* —— 抽屉拖拽 / 宽度 —— */
const drawerWidth = ref(0)
const resizing = ref(false)
const DRAWER_MIN = 420
function defaultDrawerWidth() { return Math.max(DRAWER_MIN, Math.min(560, Math.floor(window.innerWidth * 0.42))) }
function ensureDrawerSize() { if (!drawerWidth.value) drawerWidth.value = defaultDrawerWidth() }
let dragStartX = 0, dragStartW = 0
function onDragStart(e) {
  resizing.value = true
  dragStartX = e.clientX; dragStartW = drawerWidth.value
  document.body.style.cursor = 'col-resize'; document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragEnd)
}
function onDragMove(e) {
  const max = Math.floor(window.innerWidth * 0.85)
  const dx = dragStartX - e.clientX
  drawerWidth.value = Math.min(max, Math.max(DRAWER_MIN, dragStartW + dx))
}
function onDragEnd() {
  resizing.value = false
  document.body.style.cursor = ''; document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
}
onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
})
async function submitType() {
  if (!typeForm.rdfs_label || !typeForm.api_name) { BL.warning('名称 / API 为必填'); return }
  if (typeForm.id) await enumTypeApi.update(typeForm.id, typeForm)
  else             await enumTypeApi.create(typeForm)
  BL.success('已保存')
  typeFormOpen.value = false
  await loadAll()
  if (current.value && typeForm.id === current.value.id) await loadDetail(current.value.id)
}
async function removeType(e) {
  const ok = await BL.confirm({ title: '删除枚举', content: `确定删除「${e.rdfs_label || e.api_name}」及其全部枚举项？`, danger: true, okText: '删除' })
  if (!ok) return
  await enumTypeApi.remove(e.id)
  BL.success('已删除')
  await loadAll()
  if (current.value && e.id === current.value.id) current.value = null
}

/* —— 分组表单 —— */
const groupFormOpen = ref(false)
const groupForm = reactive({})
function openCreateGroup() {
  Object.keys(groupForm).forEach(k => delete groupForm[k])
  Object.assign(groupForm, { status: 'active' })
  groupFormOpen.value = true
}
async function submitGroup() {
  if (!groupForm.group_name) { BL.warning('分组名称必填'); return }
  await enumTypeApi.createGroup(groupForm)
  BL.success('已保存')
  groupFormOpen.value = false
  await loadAll()
}

/* —— 枚举项行内编辑 —— */
function addItemRow() {
  items.value.unshift({
    id: 'new_' + Date.now(),
    label: '', api_name: '', code: '', parent_code: '',
    level: 1, sort_num: items.value.length, status: 'active',
    _editing: true, _isNew: true
  })
}
function editItemRow(it) {
  if (it._editing) return
  it._backup = JSON.parse(JSON.stringify({
    label: it.label, api_name: it.api_name, code: it.code,
    parent_code: it.parent_code, level: it.level, status: it.status
  }))
  it._editing = true
}
function cancelItemRow(it) {
  if (it._isNew) {
    items.value = items.value.filter(x => x !== it)
  } else {
    if (it._backup) Object.assign(it, it._backup)
    it._editing = false
    delete it._backup
  }
}
async function saveItemRow(it) {
  if (!it.label || !it.code) { BL.warning('名称、编码必填'); return }
  const { _editing, _isNew, _backup, ...payload } = it
  try {
    if (it._isNew) {
      delete payload.id
      await enumTypeApi.addItem(current.value.id, payload)
    } else {
      await enumTypeApi.updateItem(it.id, payload)
    }
    BL.success('已保存')
    await loadDetail(current.value.id)
    await loadAll()
  } catch (e) { BL.error(e?.msg || '保存失败') }
}
async function removeItem(it) {
  const ok = await BL.confirm({ title: '删除枚举项', content: `确定删除「${it.label}」？`, danger: true, okText: '删除' })
  if (!ok) return
  await enumTypeApi.removeItem(it.id)
  BL.success('已删除')
  await loadDetail(current.value.id)
  await loadAll()
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.hd-filter { width: 130px; }
.search-wrap { position: relative; width: 240px; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.search-input { padding-left: 30px; }

.et-body { flex: 1; display: flex; position: relative; overflow: hidden; }
.et-inner { flex: 1; display: grid; grid-template-columns: 260px 1fr; gap: 12px; padding: 12px; min-width: 0; overflow: hidden; }
.et-tree {
  background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 6px;
  display: flex; flex-direction: column; overflow: hidden;
}
.et-tree-hd { display: flex; align-items: center; justify-content: space-between;
  padding: 8px 12px; border-bottom: 1px solid var(--bl-divider); font-size: 13px; background: var(--bl-bg-2); }
.et-tree-list { flex: 1; overflow: auto; padding: 6px 4px; }
.et-row {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 8px; border-radius: 4px; cursor: pointer;
  font-size: 13px; color: var(--bl-text-2); user-select: none;
}
.et-row:hover { background: var(--bl-bg-hover); color: var(--bl-text-1); }
.et-row.is-active { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.et-pad { width: 14px; flex-shrink: 0; }
.et-chev { display: inline-flex; transition: transform .15s; flex-shrink: 0; }
.et-chev.is-open { transform: rotate(90deg); }
.et-count { margin-left: auto; font-size: 11px; color: var(--bl-text-3);
  background: var(--bl-bg-2); border-radius: 9px; padding: 0 7px; min-width: 20px; text-align: center; }
.et-subgroup { padding-left: 22px; }
.et-enum { padding-left: 22px; }
.enum-ic { width: 16px; height: 16px; border-radius: 3px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.enum-ic-lg { width: 32px; height: 32px; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }

.et-main { background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: 6px;
  display: flex; flex-direction: column; overflow: hidden; min-width: 0; }
.list-card { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.list-scroll { flex: 1; overflow: auto; }
.et-table thead th { position: sticky; top: 0; background: var(--bl-bg-2); z-index: 1; box-shadow: inset 0 -1px 0 var(--bl-divider); }
.th-sort { cursor: pointer; user-select: none; display: inline-flex; align-items: center; white-space: nowrap; }
.th-sort:hover { color: var(--bl-primary); }
.th-arrow { color: var(--bl-text-4); font-size: 11px; margin-left: 2px; }
.et-trow { cursor: pointer; }
.et-trow:hover { background: var(--bl-bg-hover); }

.et-detail { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.et-detail-hd { display: flex; align-items: center; gap: 8px; padding: 12px 14px; border-bottom: 1px solid var(--bl-divider); }
.et-detail-title { font-size: 15px; font-weight: 600; }
.et-tabs { display: flex; border-bottom: 1px solid var(--bl-divider); padding: 0 14px; }
.et-tab { padding: 8px 14px; border: 0; background: transparent; cursor: pointer;
  font-size: 13px; color: var(--bl-text-2); border-bottom: 2px solid transparent; margin-bottom: -1px; }
.et-tab.is-on { color: var(--bl-primary); border-color: var(--bl-primary); font-weight: 500; }
.et-tab-pane { flex: 1; overflow: auto; padding: 14px; }

.kv-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 16px; margin-bottom: 14px; }
.kv { display: flex; gap: 12px; align-items: center; font-size: 13px; padding: 6px 0; border-bottom: 1px dashed var(--bl-divider); }
.kv-lbl { width: 80px; color: var(--bl-text-3); font-size: 12px; flex-shrink: 0; }

.sec { font-size: 12px; color: var(--bl-text-3); margin: 12px 0 6px;
  padding-left: 8px; border-left: 3px solid var(--bl-primary); font-weight: 500; }
.mini-table { width: 100%; font-size: 12px; }
.mini-table th { background: #fafafa; padding: 6px 8px; }
.mini-table td { padding: 6px 8px; }
.item-table .bl-input.bl-input-xs { height: 26px; padding: 0 6px; font-size: 12px; width: 100%; min-width: 80px; }
.item-row.is-editing { background: var(--bl-primary-soft) !important; }
.item-row { cursor: default; }
.item-row:hover { background: #f5f7fa; }

.radio-group { display: inline-flex; gap: 20px; }
.radio-item { display: inline-flex; align-items: center; gap: 6px; cursor: pointer; font-size: 13px; }

/* 弹窗内 FieldRow 标签较长(分组名称 * / 所属分组 / 枚举类型 等),加宽避免换行 */
.bl-modal :deep(.fr.fr-inline .fr-label) { width: 84px; }

/* 枚举类型新建/编辑抽屉 — 上下贴 body 边线，仅左侧描边 */
.et-drawer {
  position: absolute; top: 0; right: 0; bottom: 0;
  background: var(--bl-bg-1);
  border-left: 1px solid var(--bl-border);
  box-shadow: -2px 0 12px rgba(0,0,0,.06);
  display: flex; flex-direction: column;
  min-width: 420px; z-index: 5;
  overflow: hidden;
}
.et-drag-handle {
  position: absolute; left: -2px; top: 0; bottom: 0; width: 5px;
  cursor: col-resize; transition: background-color .15s; z-index: 6;
}
.et-drag-handle:hover, .et-drag-handle.is-resizing { background: var(--bl-primary); }
.et-drawer-hd {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; border-bottom: 1px solid var(--bl-divider);
}
.et-drawer-ic {
  width: 32px; height: 32px; border-radius: 6px; background: var(--bl-primary);
  display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.et-drawer-title { font-size: 14px; font-weight: 600; }
.et-drawer-body { flex: 1; overflow: auto; padding: 10px 14px; }
.et-drawer-ft { padding: 10px 14px; display: flex; justify-content: flex-end; gap: 8px; border-top: 1px solid var(--bl-divider); }
.et-drawer :deep(.fr.fr-inline .fr-label) { width: 84px; }

.et-drawer-enter-active, .et-drawer-leave-active { transition: transform .25s, opacity .2s; }
.et-drawer-enter-from, .et-drawer-leave-to { transform: translateX(20px); opacity: 0; }

/* ====== 详情 Tab: 基本信息 / 编码管理 头部按钮行 ====== */
.sec-row { display: flex; align-items: center; gap: 8px; margin: 12px 0 6px; }
.sec-row .sec { margin: 0; flex: 1; }

/* ====== 子页签 (配置 / 示例) ====== */
.sub-tabs { display: flex; gap: 0; border-bottom: 1px solid var(--bl-divider); margin-bottom: 8px; }
.sub-tab {
  padding: 6px 14px; border: 0; background: transparent; cursor: pointer;
  font-size: 12.5px; color: var(--bl-text-2); border-bottom: 2px solid transparent; margin-bottom: -1px;
}
.sub-tab.is-on { color: var(--bl-primary); border-color: var(--bl-primary); font-weight: 500; }

/* ====== 数据 Tab: 视图切换 segmented ====== */
.seg { display: inline-flex; background: var(--bl-bg-2); border-radius: var(--bl-radius-2); padding: 2px; gap: 2px; }
.seg-btn {
  height: 26px; padding: 0 10px; border: 0; background: transparent;
  font-size: var(--bl-fs-12); color: var(--bl-text-2); cursor: pointer;
  border-radius: 4px; display: inline-flex; align-items: center;
}
.seg-btn.is-on { background: var(--bl-bg-1); color: var(--bl-primary); box-shadow: var(--bl-shadow-1); font-weight: 500; }

/* 数据 Tab 树视图样式已迁移至下方非 scoped 块 (h() 渲染的子组件不继承 data-v-xxx 属性) */

/* ====== 编辑层级规则 行内输入 ====== */
.mini-table .bl-input.bl-input-xs { height: 26px; padding: 0 6px; font-size: 12px; width: 100%; min-width: 60px; }

/* ====== 前/后补开关 (在补位位置列使用) ====== */
.bl-switch-wrap { display: inline-flex; align-items: center; gap: 6px; }
.bl-switch-wrap .sw-side { font-size: 11.5px; color: var(--bl-text-3); user-select: none; }
.bl-switch-wrap .sw-side.is-active { color: var(--bl-primary); font-weight: 500; }
.bl-switch { position: relative; display: inline-block; width: 32px; height: 18px; }
.bl-switch input { opacity: 0; width: 0; height: 0; }
.bl-switch-slider {
  position: absolute; cursor: pointer; inset: 0;
  background: var(--bl-bg-3); border-radius: 999px; transition: background-color .15s;
}
.bl-switch-slider::before {
  content: ''; position: absolute; top: 2px; left: 2px;
  width: 14px; height: 14px; background: #fff; border-radius: 50%; transition: transform .15s;
  box-shadow: 0 1px 3px rgba(0,0,0,.2);
}
.bl-switch input:checked + .bl-switch-slider { background: var(--bl-primary); }
.bl-switch input:checked + .bl-switch-slider::before { transform: translateX(14px); }

/* ====== 同步规则表单 ====== */
.sync-form { max-width: 720px; }
.sync-form :deep(.fr.fr-inline .fr-label) { width: 110px; }
.sync-actions { display: flex; gap: 8px; align-items: center; margin-top: 16px;
  padding-top: 14px; border-top: 1px dashed var(--bl-divider); }
</style>

<!-- 非 scoped: 树视图通过 h() 渲染,子组件不继承父级 data-v-xxx,需用全局选择器 (.item-tree 前缀避免泄漏) -->
<style>
.item-tree { border: 1px solid var(--bl-divider); border-radius: 4px; padding: 10px 12px; }
.item-tree ul { list-style: none; margin: 0; padding: 0; }
.item-tree .item-tn-kids { padding-left: 22px; border-left: 1px dashed var(--bl-divider); margin-left: 11px; }
.item-tree .item-tnode { padding: 3px 0; }
.item-tree .item-tn-row {
  display: flex; align-items: center; gap: 10px;
  min-height: 36px; padding: 6px 10px; border-radius: 4px; font-size: 13px;
}
.item-tree .item-tn-row:hover { background: var(--bl-bg-hover); }
/* 收展触点: 加大点击区域 (28x28),图标视觉居中 */
.item-tree .item-tn-chev {
  display: inline-flex; align-items: center; justify-content: center;
  width: 28px; height: 28px; flex-shrink: 0;
  cursor: pointer; transition: transform .15s, background-color .15s;
  color: var(--bl-text-3); border-radius: 4px;
}
.item-tree .item-tn-chev:hover { background: var(--bl-bg-3); color: var(--bl-primary); }
.item-tree .item-tn-chev.is-open { transform: rotate(90deg); }
.item-tree .item-tn-pad { width: 28px; display: inline-block; flex-shrink: 0; }
.item-tree .item-tn-code {
  color: var(--bl-text-3); font-size: 11.5px;
  padding: 3px 8px; background: var(--bl-bg-2); border-radius: 3px;
  flex-shrink: 0;
}
.item-tree .item-tn-label { color: var(--bl-text-1); flex: 1; font-size: 13px; }
</style>
