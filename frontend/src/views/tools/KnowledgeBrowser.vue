<template>
  <div class="kb">
    <PageHeader title="知识浏览器" subtitle="Knowledge Browser · 浏览 Jena 本体与向量库内容" />

    <!-- 数据源状态概览 -->
    <section class="status-row" v-if="status">
      <div :class="['stat-card', status.jena && status.jena.ready ? 'is-ok' : 'is-off']">
        <div class="stat-ic" v-html="BL.icon('cube', 18)"></div>
        <div class="stat-body">
          <div class="stat-name">Jena 本体</div>
          <div class="stat-meta">
            <template v-if="status.jena && status.jena.ready">
              就绪 · {{ status.jena.classes }} 类 / {{ status.jena.properties }} 属性 · {{ status.jena.statements }} 三元组
            </template>
            <template v-else>未就绪</template>
          </div>
        </div>
      </div>
      <div :class="['stat-card', status.vector && status.vector.available ? 'is-ok' : 'is-off']">
        <div class="stat-ic" v-html="BL.icon('layers', 18)"></div>
        <div class="stat-body">
          <div class="stat-name">向量库</div>
          <div class="stat-meta">
            <template v-if="status.vector && status.vector.available">
              {{ status.vector.type }} · 维度 {{ status.vector.dimension }}
              <span v-if="status.vector.tableExists"> · {{ status.vector.vectorCount || 0 }} 条</span>
              <span v-else> · 表未初始化</span>
            </template>
            <template v-else>不可用</template>
          </div>
        </div>
      </div>
      <div class="stat-card is-ok">
        <div class="stat-ic" v-html="BL.icon('database', 18)"></div>
        <div class="stat-body">
          <div class="stat-name">数据库</div>
          <div class="stat-meta" v-if="status.database">
            {{ status.database.databaseProduct }} · {{ status.database.dialect }} · {{ status.database.tableCount }} 表
          </div>
        </div>
      </div>
    </section>

    <!-- 主区域 -->
    <div class="kb-body">
      <div class="kb-tabs">
        <button :class="['kb-tab', activeTab === 'jena' && 'is-active']" @click="switchTab('jena')">
          <span v-html="BL.icon('cube', 15)"></span> Jena 本体
        </button>
        <button :class="['kb-tab', activeTab === 'vector' && 'is-active']" @click="switchTab('vector')">
          <span v-html="BL.icon('layers', 15)"></span> 向量库
        </button>
        <button class="bl-btn bl-btn-text kb-refresh" @click="loadStatus" title="刷新状态">
          <span v-html="BL.icon('refresh', 14)"></span>
        </button>
      </div>

      <!-- ============ Jena ============ -->
      <div class="kb-panel" v-show="activeTab === 'jena'">
        <div class="sub-tabs">
          <button :class="['sub-tab', jenaSub === 'classes' && 'is-active']" @click="setJenaSub('classes')">类 ({{ classCount }})</button>
          <button :class="['sub-tab', jenaSub === 'properties' && 'is-active']" @click="setJenaSub('properties')">属性 ({{ propCount }})</button>
          <button :class="['sub-tab', jenaSub === 'members' && 'is-active']" @click="setJenaSub('members')">成员·个体</button>
          <button :class="['sub-tab', jenaSub === 'resource' && 'is-active']" @click="setJenaSub('resource')">资源页</button>
          <button :class="['sub-tab', jenaSub === 'catalog' && 'is-active']" @click="setJenaSub('catalog')">目录</button>
          <button :class="['sub-tab', jenaSub === 'tree' && 'is-active']" @click="setJenaSub('tree')">类树</button>
          <button :class="['sub-tab', jenaSub === 'sparql' && 'is-active']" @click="setJenaSub('sparql')">SPARQL 查询</button>
        </div>

        <!-- 类列表 -->
        <div class="sub-panel" v-show="jenaSub === 'classes'">
          <div class="toolbar">
            <div class="bl-input-wrap">
              <span class="bl-input-ic" v-html="BL.icon('search', 14)"></span>
              <input class="bl-input" v-model="classFilter" placeholder="按类名 / 标签过滤…" @input="onClassFilter" />
            </div>
            <span class="bl-muted">点击类名查看详情</span>
          </div>
          <div class="kb-table-wrap">
            <table class="kb-table">
              <thead>
                <tr><th>类名</th><th>标签</th><th>父类</th><th>URI</th></tr>
              </thead>
              <tbody>
                <tr v-for="c in filteredClasses" :key="c.uri" class="row-click" @click="selectClass(c)">
                  <td class="mono">{{ c.localName }}</td>
                  <td>{{ c.label }}</td>
                  <td>
                    <span v-if="!c.superClasses || !c.superClasses.length" class="bl-muted">—</span>
                    <span v-else class="chip" v-for="s in c.superClasses" :key="s">{{ s }}</span>
                  </td>
                  <td class="mono uri">{{ c.uri }}</td>
                </tr>
                <tr v-if="!filteredClasses.length"><td colspan="4" class="empty">无匹配类</td></tr>
              </tbody>
            </table>
          </div>

          <!-- 类详情 -->
          <transition name="fade">
            <div class="detail-card" v-if="selectedClass">
              <div class="detail-hd">
                <div>
                  <div class="detail-title">{{ selectedClass.localName }}</div>
                  <div class="bl-muted">{{ selectedClass.label || selectedClass.localName }}</div>
                </div>
                <div class="hd-actions">
                  <button class="bl-btn" @click="gotoMembers(selectedClass.localName)">查成员</button>
                  <button class="bl-btn" @click="gotoResource(selectedClass.uri)">资源页</button>
                  <button class="bl-btn" @click="searchSimilarClass(selectedClass.label || selectedClass.localName)">查相似</button>
                  <button class="bl-btn bl-btn-text" @click="selectedClass = null">
                    <span v-html="BL.icon('x', 14)"></span>
                  </button>
                </div>
              </div>
              <div class="detail-grid">
                <div><span class="k">URI</span><span class="v mono">{{ selectedClass.uri }}</span></div>
                <div><span class="k">父类</span><span class="v">
                  <span v-if="!selectedClass.superClasses || !selectedClass.superClasses.length" class="bl-muted">—</span>
                  <span v-else class="chip" v-for="s in selectedClass.superClasses" :key="s">{{ s }}</span>
                </span></div>
                <div><span class="k">子类</span><span class="v">
                  <span v-if="!selectedClass.subClasses || !selectedClass.subClasses.length" class="bl-muted">—</span>
                  <span v-else class="chip" v-for="s in selectedClass.subClasses" :key="s">{{ s }}</span>
                </span></div>
              </div>
              <div class="detail-sub">关联属性 ({{ (selectedClass.properties || []).length }})</div>
              <div class="prop-list">
                <div class="prop-item" v-for="p in (selectedClass.properties || [])" :key="p.localName">
                  <span class="prop-name mono">{{ p.localName }}</span>
                  <span class="prop-type" :class="p.type === 'ObjectProperty' ? 't-obj' : 't-dt'">{{ p.type }}</span>
                  <span class="bl-muted" v-if="p.label && p.label !== p.localName">· {{ p.label }}</span>
                </div>
                <div class="bl-muted" v-if="!selectedClass.properties || !selectedClass.properties.length">无</div>
              </div>
            </div>
          </transition>
        </div>

        <!-- 属性列表 -->
        <div class="sub-panel" v-show="jenaSub === 'properties'">
          <div class="toolbar">
            <div class="bl-input-wrap">
              <span class="bl-input-ic" v-html="BL.icon('search', 14)"></span>
              <input class="bl-input" v-model="propFilter" placeholder="按属性名 / 标签过滤…" />
            </div>
            <span class="bl-muted">点击属性查看详情</span>
          </div>
          <div class="kb-table-wrap">
            <table class="kb-table">
              <thead>
                <tr><th>属性名</th><th>标签</th><th>类型</th><th>定义域</th><th>值域</th></tr>
              </thead>
              <tbody>
                <tr v-for="p in filteredProperties" :key="p.uri" class="row-click"
                    :class="selectedProperty && selectedProperty.uri === p.uri && 'is-sel'"
                    @click="selectProperty(p)">
                  <td class="mono">{{ p.localName }}</td>
                  <td>{{ p.label }}</td>
                  <td><span class="prop-type" :class="p.type === 'ObjectProperty' ? 't-obj' : 't-dt'">{{ p.type }}</span></td>
                  <td>
                    <span v-if="!p.domains || !p.domains.length" class="bl-muted">—</span>
                    <span v-else class="chip" v-for="d in p.domains" :key="d">{{ d }}</span>
                  </td>
                  <td>
                    <span v-if="!p.ranges || !p.ranges.length" class="bl-muted">—</span>
                    <span v-else class="chip" v-for="r in p.ranges" :key="r">{{ r }}</span>
                  </td>
                </tr>
                <tr v-if="!filteredProperties.length"><td colspan="5" class="empty">无匹配属性</td></tr>
              </tbody>
            </table>
          </div>

          <!-- 属性详情 -->
          <transition name="fade">
            <div class="detail-card" v-if="selectedProperty">
              <div class="detail-hd">
                <div>
                  <div class="detail-title">{{ selectedProperty.localName }}</div>
                  <div class="bl-muted">{{ selectedProperty.label || selectedProperty.localName }}</div>
                </div>
                <div class="hd-actions">
                  <button class="bl-btn" @click="gotoResource(selectedProperty.uri)">资源页</button>
                  <button class="bl-btn bl-btn-text" @click="selectedProperty = null">
                    <span v-html="BL.icon('x', 14)"></span>
                  </button>
                </div>
              </div>
              <div class="detail-grid">
                <div><span class="k">URI</span><span class="v mono">{{ selectedProperty.uri }}</span></div>
                <div><span class="k">类型</span><span class="v">
                  <span class="prop-type" :class="selectedProperty.type === 'ObjectProperty' ? 't-obj' : 't-dt'">{{ selectedProperty.type }}</span>
                </span></div>
                <div><span class="k">注释</span><span class="v">
                  <span v-if="selectedProperty.comment">{{ selectedProperty.comment }}</span>
                  <span v-else-if="selectedProperty.commentEn" class="bl-muted">{{ selectedProperty.commentEn }}</span>
                  <span v-else class="bl-muted">—</span>
                </span></div>
                <div><span class="k">定义域</span><span class="v">
                  <span v-if="!selectedProperty.domains || !selectedProperty.domains.length" class="bl-muted">—</span>
                  <span v-else class="chip" v-for="d in selectedProperty.domains" :key="d">{{ d }}</span>
                </span></div>
                <div><span class="k">值域</span><span class="v">
                  <span v-if="!selectedProperty.ranges || !selectedProperty.ranges.length" class="bl-muted">—</span>
                  <span v-else class="chip" v-for="r in selectedProperty.ranges" :key="r">{{ r }}</span>
                </span></div>
                <div><span class="k">超属性</span><span class="v">
                  <span v-if="!selectedProperty.superProperties || !selectedProperty.superProperties.length" class="bl-muted">—</span>
                  <span v-else class="chip" v-for="s in selectedProperty.superProperties" :key="s">{{ s }}</span>
                </span></div>
                <div><span class="k">子属性</span><span class="v">
                  <span v-if="!selectedProperty.subProperties || !selectedProperty.subProperties.length" class="bl-muted">—</span>
                  <span v-else class="chip" v-for="s in selectedProperty.subProperties" :key="s">{{ s }}</span>
                </span></div>
                <div><span class="k">逆属性</span><span class="v">
                  <span v-if="!selectedProperty.inverseOf" class="bl-muted">—</span>
                  <span v-else class="chip">{{ selectedProperty.inverseOf }}</span>
                </span></div>
                <div><span class="k">等价属性</span><span class="v">
                  <span v-if="!selectedProperty.equivalentProperties || !selectedProperty.equivalentProperties.length" class="bl-muted">—</span>
                  <span v-else class="chip" v-for="e in selectedProperty.equivalentProperties" :key="e">{{ e }}</span>
                </span></div>
                <div><span class="k">特征</span><span class="v">
                  <span v-for="(on, name) in (selectedProperty.characteristics || {})" :key="name"
                        class="feat" :class="on ? 'feat-on' : 'feat-off'">{{ featLabel(name) }}</span>
                </span></div>
              </div>
            </div>
          </transition>
        </div>

        <!-- 成员·个体浏览 -->
        <div class="sub-panel" v-show="jenaSub === 'members'">
          <div class="toolbar">
            <div class="bl-input-wrap">
              <span class="bl-input-ic" v-html="BL.icon('cube', 14)"></span>
              <input class="bl-input" list="kbClsList" v-model="memberClassRef" placeholder="选择或输入类名，如 Enum_DWLX / Reservoir" @keyup.enter="loadMembers(1)" />
              <datalist id="kbClsList">
                <option v-for="c in classes" :key="c.uri" :value="c.localName">{{ c.label }}</option>
              </datalist>
            </div>
            <div class="bl-input-wrap">
              <span class="bl-input-ic" v-html="BL.icon('search', 14)"></span>
              <input class="bl-input" v-model="memberKeyword" placeholder="按编码 / 名称过滤…" @keyup.enter="loadMembers(1)" />
            </div>
            <button class="bl-btn bl-btn-primary" :disabled="memberLoading" @click="loadMembers(1)">
              <span v-html="BL.icon('search', 13)"></span> 查询
            </button>
            <span class="bl-muted" v-if="memberClassLabel">{{ memberClassLabel }} · 共 {{ memberTotal }} 个</span>
          </div>
          <div class="kb-table-wrap">
            <table class="kb-table">
              <thead><tr><th>编码</th><th>名称</th><th>URI</th></tr></thead>
              <tbody>
                <tr v-for="m in members" :key="m.uri" class="row-click" @click="gotoResource(m.uri)">
                  <td class="mono">{{ m.code || '—' }}</td>
                  <td>{{ m.label || m.localName }}</td>
                  <td class="mono uri">{{ m.uri }}</td>
                </tr>
                <tr v-if="!members.length"><td colspan="3" class="empty">{{ memberLoading ? '加载中…' : '请选择类并查询' }}</td></tr>
              </tbody>
            </table>
          </div>
          <div class="pager" v-if="memberTotal > 0">
            <button class="bl-btn" :disabled="memberPage <= 1" @click="loadMembers(memberPage - 1)">上一页</button>
            <span class="bl-muted">第 {{ memberPage }} 页 · 共 {{ memberTotal }} 条</span>
            <button class="bl-btn" :disabled="memberPage * memberSize >= memberTotal" @click="loadMembers(memberPage + 1)">下一页</button>
          </div>
        </div>

        <!-- 通用资源页（仿 Fuseki 下钻浏览） -->
        <div class="sub-panel" v-show="jenaSub === 'resource'">
          <div class="toolbar">
            <button class="bl-btn" v-if="resourceHistory.length > 1" @click="resourceBack" title="返回上一级">
              <span v-html="BL.icon('back', 13)"></span> 返回
            </button>
            <div class="bl-input-wrap grow">
              <span class="bl-input-ic" v-html="BL.icon('compass', 14)"></span>
              <input class="bl-input" v-model="resourceUri" placeholder="输入或粘贴资源 URI，如 …/ontology#Enum_DWLX_201" @keyup.enter="openResource(resourceUri)" />
            </div>
            <button class="bl-btn bl-btn-primary" :disabled="resourceLoading" @click="openResource(resourceUri)">
              <span v-html="BL.icon('search', 13)"></span> 打开
            </button>
          </div>

          <div class="res-empty bl-muted" v-if="!resourceData">输入 URI 打开资源页，或从类 / 属性 / 成员列表点选进入（点击三元组中的资源可继续下钻）</div>

          <template v-else>
            <div class="res-hd">
              <div class="res-title">{{ resourceData.label || resourceData.localName }}</div>
              <div class="bl-muted mono uri-long">{{ resourceData.uri }}</div>
              <div class="res-types" v-if="resourceData.types && resourceData.types.length">
                <span class="k">类型</span>
                <span class="chip link" v-for="t in resourceData.types" :key="t.uri" @click="openResource(t.uri)" :title="t.uri">{{ t.label || t.localName }}</span>
              </div>
            </div>

            <div class="triple-card">
              <div class="triple-hd">出站 · 作为主体（{{ resourceData.outgoingTotal }}）<span class="bl-muted" v-if="resourceData.outgoingTruncated"> · 已截断</span></div>
              <table class="kb-table triple-table">
                <tbody>
                  <template v-for="g in resourceData.outgoing" :key="g.predicate">
                    <tr v-for="(o, i) in g.objects" :key="g.predicate + i">
                      <td class="mono pred" v-if="i === 0" :rowspan="g.objects.length" :title="g.predicate">
                        <span class="res-link" @click="openResource(g.predicate)">{{ g.predicateLabel || g.predicateLocalName }}</span>
                      </td>
                      <td>
                        <span v-if="o.isLiteral" class="lit">{{ o.value }}<span class="tag" v-if="o.lang">@{{ o.lang }}</span><span class="tag" v-else-if="o.datatype">^{{ shortDt(o.datatype) }}</span></span>
                        <span v-else-if="o.isAnon" class="bl-muted mono">{{ o.value }}</span>
                        <span v-else class="res-link" @click="openResource(o.uri)" :title="o.uri">{{ o.label || o.localName }}</span>
                      </td>
                    </tr>
                  </template>
                  <tr v-if="!resourceData.outgoing || !resourceData.outgoing.length"><td class="empty">无出站三元组</td></tr>
                </tbody>
              </table>
            </div>

            <div class="triple-card">
              <div class="triple-hd">入站 · 作为宾语（{{ resourceData.incomingTotal }}）</div>
              <table class="kb-table triple-table">
                <tbody>
                  <tr v-for="(row, i) in resourceData.incoming" :key="i">
                    <td><span class="res-link" @click="openResource(row.subject.uri)" :title="row.subject.uri">{{ row.subject.label || row.subject.localName }}</span></td>
                    <td class="mono pred"><span class="res-link" @click="openResource(row.predicate.uri)" :title="row.predicate.uri">{{ row.predicate.label || row.predicate.localName }}</span></td>
                  </tr>
                  <tr v-if="!resourceData.incoming || !resourceData.incoming.length"><td class="empty">无入站三元组</td></tr>
                </tbody>
              </table>
              <div class="pager" v-if="resourceData.incomingTotal > resourceData.size">
                <button class="bl-btn" :disabled="resourceData.page <= 1" @click="loadIncoming(resourceData.page - 1)">上一页</button>
                <span class="bl-muted">第 {{ resourceData.page }} 页 · 共 {{ resourceData.incomingTotal }} 条</span>
                <button class="bl-btn" :disabled="resourceData.page * resourceData.size >= resourceData.incomingTotal" @click="loadIncoming(resourceData.page + 1)">下一页</button>
              </div>
            </div>
          </template>
        </div>

        <!-- 目录（通用实体检索） -->
        <div class="sub-panel" v-show="jenaSub === 'catalog'">
          <div class="vocab-bar" v-if="vocab">
            <span class="vocab-item">命名空间 <b>{{ vocab.prefixCount }}</b></span>
            <span class="vocab-item">类 <b>{{ vocab.classes }}</b></span>
            <span class="vocab-item">属性 <b>{{ vocab.properties }}</b></span>
            <span class="vocab-item">个体 <b>{{ vocab.individuals }}</b></span>
            <span class="vocab-item">三元组 <b>{{ vocab.statements }}</b></span>
          </div>
          <div class="toolbar">
            <select class="bl-select" v-model="entityKind" @change="loadEntities(1)">
              <option value="all">全部类型</option>
              <option value="class">类</option>
              <option value="objectProperty">对象属性</option>
              <option value="datatypeProperty">数据属性</option>
              <option value="annotationProperty">注释属性</option>
              <option value="individual">个体</option>
            </select>
            <div class="bl-input-wrap">
              <span class="bl-input-ic" v-html="BL.icon('search', 14)"></span>
              <input class="bl-input" v-model="entityKeyword" placeholder="按名称 / 标签过滤…" @keyup.enter="loadEntities(1)" />
            </div>
            <button class="bl-btn bl-btn-primary" :disabled="entityLoading" @click="loadEntities(1)">查询</button>
            <span class="bl-muted">共 {{ entityTotal }} 条 · 点击行进入资源页</span>
          </div>
          <div class="kb-table-wrap">
            <table class="kb-table">
              <thead><tr><th>名称</th><th>标签</th><th>类型</th><th>URI</th></tr></thead>
              <tbody>
                <tr v-for="e in entityItems" :key="e.uri" class="row-click" @click="gotoResource(e.uri)">
                  <td class="mono">{{ e.localName }}</td>
                  <td>{{ e.label }}</td>
                  <td><span class="kind-tag" :class="kindClass(e.kind)">{{ kindLabel(e.kind) }}</span></td>
                  <td class="mono uri">{{ e.uri }}</td>
                </tr>
                <tr v-if="!entityItems.length"><td colspan="4" class="empty">{{ entityLoading ? '加载中…' : '无结果' }}</td></tr>
              </tbody>
            </table>
          </div>
          <div class="pager" v-if="entityTotal > 0">
            <button class="bl-btn" :disabled="entityPage <= 1" @click="loadEntities(entityPage - 1)">上一页</button>
            <span class="bl-muted">第 {{ entityPage }} 页 · 共 {{ entityTotal }} 条</span>
            <button class="bl-btn" :disabled="entityPage * entitySize >= entityTotal" @click="loadEntities(entityPage + 1)">下一页</button>
          </div>
        </div>

        <!-- 类树 -->
        <div class="sub-panel" v-show="jenaSub === 'tree'">
          <div class="toolbar">
            <span class="bl-muted">类层级（subClassOf）· 点类名看详情 · 数字徽标为个体数（点击看成员）</span>
            <button class="bl-btn bl-btn-text kb-refresh" @click="loadTree('')" title="刷新"><span v-html="BL.icon('refresh', 14)"></span></button>
          </div>
          <div class="kb-table-wrap tree-wrap">
            <div class="tree">
              <div v-for="n in flatTree" :key="n.uri" class="tree-row" :style="{ paddingLeft: (n.depth * 18 + 8) + 'px' }">
                <span :class="['tree-caret', n.children && n.children.length ? '' : 'is-leaf']" @click="n.children && n.children.length && toggleNode(n.uri)">{{ n.children && n.children.length ? (expanded.has(n.uri) ? '▾' : '▸') : '·' }}</span>
                <span class="tree-name res-link" @click="gotoClassDetail(n.localName)" :title="n.uri">{{ n.label || n.localName }}</span>
                <span class="mono tree-ln">{{ n.localName }}</span>
                <span class="chip link" v-if="n.memberCount > 0" @click="gotoMembers(n.localName)" :title="'查看 ' + n.memberCount + ' 个个体'">{{ n.memberCount }}</span>
              </div>
              <div class="empty" v-if="!flatTree.length">{{ treeLoading ? '加载中…' : '暂无类树数据' }}</div>
            </div>
          </div>
        </div>

        <!-- SPARQL -->
        <div class="sub-panel" v-show="jenaSub === 'sparql'">
          <div class="sparql-box">
            <textarea v-model="sparqlText" class="sparql-input mono" spellcheck="false"
              placeholder="输入 SPARQL (SELECT / ASK / CONSTRUCT / DESCRIBE)，回车换行"></textarea>
            <div class="sparql-bar">
              <button class="bl-btn bl-btn-primary" :disabled="sparqlLoading" @click="runSparql">
                <span v-html="BL.icon('play', 13)"></span> 执行
              </button>
              <select class="bl-select" v-model="selectedTemplate" @change="applyTemplate" title="查询模板">
                <option value="">模板…</option>
                <option v-for="(t, i) in queryTemplates" :key="i" :value="i">{{ t.name }}</option>
              </select>
              <select class="bl-select" v-model="sparqlFormat" title="结果格式（CSV 仅 SELECT，Turtle 仅 CONSTRUCT/DESCRIBE）">
                <option value="json">表格</option>
                <option value="csv">CSV</option>
                <option value="turtle">Turtle</option>
              </select>
              <button class="bl-btn" v-if="sparqlResult && sparqlResult.csv" @click="exportCsv">导出 CSV</button>
              <select class="bl-select" v-if="sparqlHistory.length" v-model="selectedHistory" @change="applyHistory" title="历史查询">
                <option value="">历史…</option>
                <option v-for="(h, i) in sparqlHistory" :key="i" :value="i">{{ h.label }}</option>
              </select>
              <span class="bl-muted" v-if="sparqlResult">类型: {{ sparqlResult.queryType }} · 行数: {{ sparqlRowCount }}</span>
            </div>
          </div>
          <div class="sparql-result" v-if="sparqlResult">
            <!-- CSV 视图 -->
            <div v-if="sparqlResult.csv">
              <pre class="triples">{{ sparqlResult.csv }}</pre>
            </div>
            <!-- Turtle 视图 -->
            <div v-else-if="sparqlResult.turtle">
              <pre class="triples">{{ sparqlResult.turtle }}</pre>
            </div>
            <!-- SELECT 表格 -->
            <div v-else-if="sparqlResult.queryType === 'SELECT'">
              <div class="kb-table-wrap">
                <table class="kb-table">
                  <thead><tr><th v-for="v in sparqlResult.vars" :key="v">{{ v }}</th></tr></thead>
                  <tbody>
                    <tr v-for="(row, i) in sparqlResult.rows" :key="i">
                      <td v-for="v in sparqlResult.vars" :key="v" class="mono">{{ fmt(row[v]) }}</td>
                    </tr>
                    <tr v-if="!sparqlResult.rows || !sparqlResult.rows.length"><td :colspan="sparqlResult.vars.length" class="empty">无结果</td></tr>
                  </tbody>
                </table>
              </div>
            </div>
            <!-- ASK -->
            <div v-else-if="sparqlResult.queryType === 'ASK'" class="ask-result">
              <span :class="['badge', sparqlResult.ask ? 'badge-ok' : 'badge-no']">{{ sparqlResult.ask ? 'true' : 'false' }}</span>
            </div>
            <!-- CONSTRUCT / DESCRIBE -->
            <div v-else>
              <div class="bl-muted" style="margin-bottom:6px">三元组数: {{ sparqlResult.tripleCount }}</div>
              <pre class="triples">{{ (sparqlResult.triples || []).join('\n') }}</pre>
            </div>
          </div>
          <div class="sparql-empty bl-muted" v-else>执行 SPARQL 以查看结果</div>
        </div>
      </div>

      <!-- ============ 向量库 ============ -->
      <div class="kb-panel" v-show="activeTab === 'vector'">
        <div class="sub-tabs">
          <button :class="['sub-tab', vectorMode === 'search' && 'is-active']" @click="vectorMode = 'search'">相似度检索</button>
          <button :class="['sub-tab', vectorMode === 'browse' && 'is-active']" @click="switchVectorBrowse">浏览全部</button>
        </div>

        <!-- 相似度检索 -->
        <div v-show="vectorMode === 'search'">
          <div class="vector-form">
            <div class="bl-input-wrap grow">
              <span class="bl-input-ic" v-html="BL.icon('search', 14)"></span>
              <input class="bl-input" v-model="vectorText" placeholder="输入查询文本，如：水库 调度 预案" @keyup.enter="runVectorSearch" />
            </div>
            <label class="vf-field">TopK
              <input class="bl-input sm" type="number" v-model.number="vectorTopK" min="1" max="50" />
            </label>
            <label class="vf-field">阈值
              <input class="bl-input sm" type="number" v-model.number="vectorThreshold" min="0" max="1" step="0.05" />
            </label>
            <button class="bl-btn bl-btn-primary" :disabled="vectorLoading" @click="runVectorSearch">
              <span v-html="BL.icon('play', 13)"></span> 检索
            </button>
          </div>

          <div class="kb-table-wrap" v-if="vectorResults.length">
            <table class="kb-table">
              <thead><tr><th>#</th><th>类 ID</th><th>命名空间</th><th>来源文本</th><th>相似度</th><th></th></tr></thead>
              <tbody>
                <tr v-for="(m, i) in vectorResults" :key="i">
                  <td class="bl-muted">{{ i + 1 }}</td>
                  <td class="mono">{{ m.class_id }}</td>
                  <td class="mono">{{ m.ns_code }}</td>
                  <td>{{ m.source_text }}</td>
                  <td>
                    <div class="sim-bar">
                      <div class="sim-fill" :style="{ width: (m.similarity * 100).toFixed(1) + '%' }"></div>
                      <span class="sim-val">{{ (m.similarity * 100).toFixed(1) }}%</span>
                    </div>
                  </td>
                  <td><button class="bl-btn bl-btn-text" @click="gotoClassDetail(m.class_id)" title="查看本体类">本体</button></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="vector-empty" v-else>
            <div class="ve-ic" v-html="BL.icon('layers', 40)"></div>
            <div v-if="vectorMsg" class="ve-msg">{{ vectorMsg }}</div>
            <div v-else class="bl-muted">输入查询文本并点击「检索」查看相似本体类</div>
            <div class="bl-muted sm" v-if="vectorMsg">需先执行 deploy/ontology/setup-pgvector.sql 并对类同步 embedding</div>
          </div>
        </div>

        <!-- 浏览全部 -->
        <div v-show="vectorMode === 'browse'">
          <div class="toolbar">
            <div class="bl-input-wrap">
              <span class="bl-input-ic" v-html="BL.icon('search', 14)"></span>
              <input class="bl-input" v-model="vecNsCode" placeholder="按命名空间(ns_code)过滤，可空…" @keyup.enter="loadVectorList(1)" />
            </div>
            <button class="bl-btn bl-btn-primary" :disabled="vecLoading" @click="loadVectorList(1)">查询</button>
            <span class="bl-muted">共 {{ vecTotal }} 条 · 点行看详情</span>
          </div>
          <div class="kb-table-wrap">
            <table class="kb-table">
              <thead><tr><th>类 ID</th><th>命名空间</th><th>来源文本</th><th>维度</th><th>更新时间</th><th></th></tr></thead>
              <tbody>
                <tr v-for="it in vecItems" :key="it.class_id" class="row-click" @click="showVectorDetail(it.class_id)">
                  <td class="mono">{{ it.class_id }}</td>
                  <td class="mono">{{ it.ns_code }}</td>
                  <td>{{ it.source_text }}</td>
                  <td>{{ it.dimension }}</td>
                  <td class="bl-muted">{{ fmtTime(it.updated_at) }}</td>
                  <td><button class="bl-btn bl-btn-text" @click.stop="gotoClassDetail(it.class_id)" title="查看本体类">本体</button></td>
                </tr>
                <tr v-if="!vecItems.length"><td colspan="6" class="empty">{{ vecLoading ? '加载中…' : (vecMsg || '点击「查询」浏览向量库') }}</td></tr>
              </tbody>
            </table>
          </div>
          <div class="pager" v-if="vecTotal > 0">
            <button class="bl-btn" :disabled="vecPage <= 1" @click="loadVectorList(vecPage - 1)">上一页</button>
            <span class="bl-muted">第 {{ vecPage }} 页 · 共 {{ vecTotal }} 条</span>
            <button class="bl-btn" :disabled="vecPage * vecSize >= vecTotal" @click="loadVectorList(vecPage + 1)">下一页</button>
          </div>

          <transition name="fade">
            <div class="detail-card" v-if="vecDetail">
              <div class="detail-hd">
                <div>
                  <div class="detail-title">{{ vecDetail.class_id }}</div>
                  <div class="bl-muted">命名空间 {{ vecDetail.ns_code }}</div>
                </div>
                <button class="bl-btn bl-btn-text" @click="vecDetail = null"><span v-html="BL.icon('x', 14)"></span></button>
              </div>
              <div class="detail-grid">
                <div><span class="k">来源文本</span><span class="v">{{ vecDetail.source_text }}</span></div>
                <div><span class="k">维度</span><span class="v">{{ vecDetail.dimension }}</span></div>
                <div><span class="k">版本</span><span class="v">{{ vecDetail.version }}</span></div>
                <div><span class="k">创建时间</span><span class="v">{{ fmtTime(vecDetail.created_at) }}</span></div>
                <div><span class="k">更新时间</span><span class="v">{{ fmtTime(vecDetail.updated_at) }}</span></div>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { BL } from '@/lib/bl.js'
import { toolApi } from '@/api'

const SPARQL_EXAMPLE = `SELECT ?c ?l WHERE {
  ?c a <http://www.w3.org/2002/07/owl#Class> ;
     <http://www.w3.org/2000/01/rdf-schema#label> ?l
} LIMIT 20`

const activeTab = ref('jena')
const status = ref(null)

/* ---- Jena ---- */
const jenaSub = ref('classes')
const classes = ref([])
const properties = ref([])
const classFilter = ref('')
const propFilter = ref('')
const selectedClass = ref(null)
const selectedProperty = ref(null)
const sparqlText = ref(SPARQL_EXAMPLE)
const sparqlLoading = ref(false)
const sparqlResult = ref(null)

/* ---- 成员·个体浏览 ---- */
const memberClassRef = ref('')
const memberKeyword = ref('')
const memberLoading = ref(false)
const members = ref([])
const memberTotal = ref(0)
const memberPage = ref(1)
const memberSize = ref(50)
const memberClassLabel = ref('')

/* ---- 通用资源页 ---- */
const resourceUri = ref('')
const resourceData = ref(null)
const resourceLoading = ref(false)
const resourceHistory = ref([])

const classCount = computed(() => classes.value.length)
const propCount = computed(() => properties.value.length)
const filteredClasses = computed(() => {
  const q = classFilter.value.trim().toLowerCase()
  if (!q) return classes.value
  return classes.value.filter(c =>
    (c.localName || '').toLowerCase().includes(q) || (c.label || '').toLowerCase().includes(q))
})
const filteredProperties = computed(() => {
  const q = propFilter.value.trim().toLowerCase()
  if (!q) return properties.value
  return properties.value.filter(p =>
    (p.localName || '').toLowerCase().includes(q) || (p.label || '').toLowerCase().includes(q))
})
const sparqlRowCount = computed(() => sparqlResult.value?.rows?.length || 0)

/* ---- 向量库 ---- */
const vectorText = ref('')
const vectorTopK = ref(10)
const vectorThreshold = ref(0.6)
const vectorLoading = ref(false)
const vectorResults = ref([])
const vectorMsg = ref('')

/* ---- 目录 (M2) ---- */
const entityKind = ref('all')
const entityKeyword = ref('')
const entityLoading = ref(false)
const entityItems = ref([])
const entityTotal = ref(0)
const entityPage = ref(1)
const entitySize = ref(50)
const vocab = ref(null)

/* ---- 类树 (M2) ---- */
const treeData = ref([])
const treeLoading = ref(false)
const expanded = ref(new Set())

/* ---- SPARQL 工作台 (M3) ---- */
const sparqlFormat = ref('json')
const queryTemplates = ref([])
const selectedTemplate = ref('')
const selectedHistory = ref('')
const sparqlHistory = ref(JSON.parse(localStorage.getItem('kb_sparql_history') || '[]'))

/* ---- 向量浏览 (M4) ---- */
const vectorMode = ref('search')
const vecNsCode = ref('')
const vecLoading = ref(false)
const vecItems = ref([])
const vecTotal = ref(0)
const vecPage = ref(1)
const vecSize = ref(50)
const vecDetail = ref(null)
const vecMsg = ref('')

const flatTree = computed(() => {
  const out = []
  const walk = (nodes, depth) => {
    for (const n of nodes || []) {
      out.push({ ...n, depth })
      if (expanded.value.has(n.uri) && n.children && n.children.length) walk(n.children, depth + 1)
    }
  }
  walk(treeData.value, 0)
  return out
})

async function loadStatus() {
  try { status.value = await toolApi.status() } catch (e) { /* 错误已由拦截器提示 */ }
}

function setJenaSub(sub) {
  jenaSub.value = sub
  if (sub === 'classes' && !classes.value.length) loadClasses()
  if (sub === 'properties' && !properties.value.length) loadProperties()
  if ((sub === 'members' || sub === 'resource') && !classes.value.length) loadClasses()
  if (sub === 'catalog') { loadVocab(); if (!entityItems.value.length) loadEntities(1) }
  if (sub === 'tree' && !treeData.value.length) loadTree('')
  if (sub === 'sparql' && !queryTemplates.value.length) loadTemplates()
}
function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'jena') loadClasses()
}

async function loadClasses() {
  try { const d = await toolApi.jenaClasses(); classes.value = d.classes || [] }
  catch (e) { classes.value = [] }
}
async function loadProperties() {
  try { const d = await toolApi.jenaProperties(); properties.value = d.properties || [] }
  catch (e) { properties.value = [] }
}
async function selectClass(c) {
  try {
    const d = await toolApi.jenaClassDetail(c.localName)
    selectedClass.value = d
  } catch (e) {
    BL.error((e && e.msg) || '加载类详情失败')
  }
}
async function selectProperty(p) {
  try {
    const d = await toolApi.jenaPropertyDetail(p.localName)
    selectedProperty.value = d
  } catch (e) { /* 提示已出 */ }
}
function featLabel(name) {
  return { functional: '函数属性', inverseFunctional: '反函数属性', symmetric: '对称属性', transitive: '传递属性' }[name] || name
}

/* ---- 成员·个体浏览 ---- */
async function loadMembers(page = 1) {
  if (!memberClassRef.value.trim()) { BL.warning('请先选择或输入类名'); return }
  memberLoading.value = true
  try {
    const d = await toolApi.jenaMembers(memberClassRef.value.trim(), memberKeyword.value.trim(), page, memberSize.value)
    members.value = d.members || []
    memberTotal.value = d.total || 0
    memberPage.value = d.page || page
    memberClassLabel.value = d.classLabel || d.classLocalName || memberClassRef.value
  } catch (e) {
    members.value = []
    memberTotal.value = 0
  } finally {
    memberLoading.value = false
  }
}
function gotoMembers(classRef) {
  jenaSub.value = 'members'
  memberClassRef.value = classRef
  memberKeyword.value = ''
  loadMembers(1)
}

/* ---- 通用资源页 ---- */
async function openResource(uri, push = true, inPage = 1) {
  if (!uri || !String(uri).trim()) { BL.warning('请输入资源 URI'); return }
  resourceLoading.value = true
  try {
    const d = await toolApi.jenaResource(String(uri).trim(), inPage, 50)
    resourceData.value = d
    resourceUri.value = String(uri).trim()
    if (push && resourceHistory.value[resourceHistory.value.length - 1] !== d.uri) {
      resourceHistory.value.push(d.uri)
    }
  } catch (e) {
    resourceData.value = null
  } finally {
    resourceLoading.value = false
  }
}
function gotoResource(uri) {
  jenaSub.value = 'resource'
  openResource(uri)
}
function resourceBack() {
  if (resourceHistory.value.length < 2) return
  resourceHistory.value.pop()
  const prev = resourceHistory.value[resourceHistory.value.length - 1]
  openResource(prev, false)
}
function loadIncoming(page) {
  if (resourceData.value) openResource(resourceData.value.uri, false, page)
}
function shortDt(dt) {
  if (!dt) return ''
  const i = Math.max(dt.lastIndexOf('#'), dt.lastIndexOf('/'))
  return i >= 0 ? dt.slice(i + 1) : dt
}

/* ---- 目录 (M2) ---- */
async function loadVocab() {
  try { vocab.value = await toolApi.jenaVocab() } catch (e) { /* 提示已出 */ }
}
async function loadEntities(page = 1) {
  entityLoading.value = true
  try {
    const d = await toolApi.jenaEntities(entityKind.value, '', entityKeyword.value.trim(), page, entitySize.value)
    entityItems.value = d.items || []
    entityTotal.value = d.total || 0
    entityPage.value = d.page || page
  } catch (e) {
    entityItems.value = []
    entityTotal.value = 0
  } finally {
    entityLoading.value = false
  }
}
function kindLabel(k) {
  return { class: '类', objectProperty: '对象属性', datatypeProperty: '数据属性', annotationProperty: '注释属性', individual: '个体', property: '属性' }[k] || k
}
function kindClass(k) {
  return 'kind-' + ({ class: 'class', objectProperty: 'obj', datatypeProperty: 'dt', annotationProperty: 'ann', individual: 'ind' }[k] || 'default')
}

/* ---- 类树 (M2) ---- */
async function loadTree(root = '') {
  treeLoading.value = true
  try {
    const d = await toolApi.jenaHierarchy(root, 0)
    treeData.value = d.tree || []
  } catch (e) {
    treeData.value = []
  } finally {
    treeLoading.value = false
  }
}
function toggleNode(uri) {
  const s = new Set(expanded.value)
  if (s.has(uri)) s.delete(uri); else s.add(uri)
  expanded.value = s
}
function gotoClassDetail(localName) {
  jenaSub.value = 'classes'
  selectClass({ localName })
}

/* ---- SPARQL 工作台 (M3) ---- */
async function loadTemplates() {
  try {
    const d = await toolApi.jenaQueryTemplates()
    queryTemplates.value = d.templates || []
  } catch (e) { /* 提示已出 */ }
}
function applyTemplate() {
  const i = selectedTemplate.value
  if (i === '' || i === null || i === undefined) return
  sparqlText.value = queryTemplates.value[i].sparql
  selectedTemplate.value = ''
}
function applyHistory() {
  const i = selectedHistory.value
  if (i === '' || i === null || i === undefined) return
  sparqlText.value = sparqlHistory.value[i].sparql
  selectedHistory.value = ''
}
function saveHistory(sparql) {
  const label = sparql.replace(/\s+/g, ' ').trim().slice(0, 40)
  let arr = sparqlHistory.value.filter(h => h.sparql !== sparql)
  arr.unshift({ label, sparql })
  arr = arr.slice(0, 20)
  sparqlHistory.value = arr
  try { localStorage.setItem('kb_sparql_history', JSON.stringify(arr)) } catch (e) { /* 忽略 */ }
}
function exportCsv() {
  const csv = sparqlResult.value && sparqlResult.value.csv
  if (!csv) return
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = 'sparql-result.csv'
  a.click()
  URL.revokeObjectURL(a.href)
}

/* ---- 向量浏览 (M4) ---- */
function switchVectorBrowse() {
  vectorMode.value = 'browse'
  if (!vecItems.value.length) loadVectorList(1)
}
async function loadVectorList(page = 1) {
  vecLoading.value = true
  try {
    const d = await toolApi.vectorList(vecNsCode.value.trim(), page, vecSize.value)
    vecItems.value = d.items || []
    vecTotal.value = d.total || 0
    vecPage.value = d.page || page
    vecMsg.value = ''
  } catch (e) {
    vecItems.value = []
    vecTotal.value = 0
    vecMsg.value = (e && e.msg) || '浏览失败（向量表可能未初始化）'
  } finally {
    vecLoading.value = false
  }
}
async function showVectorDetail(classId) {
  try {
    const d = await toolApi.vectorDetail(classId)
    vecDetail.value = d.item || null
  } catch (e) {
    vecDetail.value = null
  }
}
function searchSimilarClass(label) {
  activeTab.value = 'vector'
  vectorMode.value = 'search'
  vectorText.value = label || ''
  if (label) runVectorSearch()
}
function fmtTime(v) {
  if (v === null || v === undefined || v === '') return '—'
  if (typeof v === 'number') return new Date(v).toLocaleString()
  return String(v)
}
function onClassFilter() { /* 计算属性即时过滤 */ }

async function runSparql() {
  sparqlLoading.value = true
  sparqlResult.value = null
  try {
    const d = await toolApi.jenaSparql(sparqlText.value, sparqlFormat.value)
    if (d && d.success) {
      sparqlResult.value = d
      saveHistory(sparqlText.value)
    } else {
      BL.warning((d && d.message) || 'SPARQL 无结果')
    }
  } catch (e) {
    BL.error(e?.msg || 'SPARQL 执行失败')
  } finally {
    sparqlLoading.value = false
  }
}

async function runVectorSearch() {
  if (!vectorText.value.trim()) { BL.warning('查询文本不能为空'); return }
  vectorLoading.value = true
  vectorResults.value = []
  vectorMsg.value = ''
  try {
    const d = await toolApi.vectorSearch(vectorText.value.trim(), vectorTopK.value, vectorThreshold.value)
    if (d && d.success) vectorResults.value = d.matches || []
    else vectorMsg.value = (d && d.message) || '向量检索无结果'
  } catch (e) {
    // 表未初始化等：拦截器已 toast，这里补充展示在空状态区
    vectorMsg.value = (e && e.msg) || '向量检索失败'
  } finally {
    vectorLoading.value = false
  }
}

function fmt(v) {
  if (v === null || v === undefined) return ''
  if (typeof v === 'object') return JSON.stringify(v)
  return String(v)
}

onMounted(() => { loadStatus(); loadClasses() })
</script>

<style scoped>
.kb { display: flex; flex-direction: column; height: 100%; }
.status-row { display: flex; gap: 12px; padding: 12px 20px; }
.stat-card {
  flex: 1; display: flex; align-items: center; gap: 10px;
  background: var(--bl-bg-1); border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3); padding: 10px 14px;
}
.stat-card.is-ok { border-left: 3px solid var(--bl-success); }
.stat-card.is-off { border-left: 3px solid var(--bl-text-3); opacity: .85; }
.stat-ic { width: 30px; height: 30px; border-radius: var(--bl-radius-2); display: flex; align-items: center; justify-content: center;
  background: var(--bl-primary-soft); color: var(--bl-primary); flex-shrink: 0; }
.stat-name { font-size: var(--bl-fs-13); font-weight: 600; color: var(--bl-text-1); }
.stat-meta { font-size: var(--bl-fs-11); color: var(--bl-text-3); }

.kb-body { flex: 1; display: flex; flex-direction: column; overflow: hidden; padding: 0 20px 20px; }
.kb-tabs { display: flex; align-items: center; gap: 4px; border-bottom: 1px solid var(--bl-border); margin-bottom: 12px; }
.kb-tab {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 9px 14px; border: 0; background: transparent; cursor: pointer;
  font-size: var(--bl-fs-14); color: var(--bl-text-2); border-bottom: 2px solid transparent;
}
.kb-tab:hover { color: var(--bl-text-1); }
.kb-tab.is-active { color: var(--bl-primary); border-bottom-color: var(--bl-primary); font-weight: 500; }
.kb-refresh { margin-left: auto; }

.kb-panel { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.sub-tabs { display: flex; gap: 4px; margin-bottom: 10px; }
.sub-tab {
  padding: 6px 12px; border: 1px solid var(--bl-border); background: var(--bl-bg-1);
  border-radius: var(--bl-radius-2); cursor: pointer; font-size: var(--bl-fs-13); color: var(--bl-text-2);
}
.sub-tab.is-active { background: var(--bl-primary-soft); color: var(--bl-primary); border-color: var(--bl-primary-border); }
.sub-panel { flex: 1; display: flex; flex-direction: column; overflow: auto; min-height: 0; gap: 10px; }

.toolbar { display: flex; align-items: center; gap: 12px; }
.bl-input-wrap { position: relative; display: inline-flex; align-items: center; }
.bl-input-ic { position: absolute; left: 9px; color: var(--bl-text-3); display: inline-flex; }
.bl-input {
  height: 32px; padding: 0 10px 0 30px; border: 1px solid var(--bl-border); border-radius: var(--bl-radius-2);
  background: var(--bl-bg-2); color: var(--bl-text-1); font-size: var(--bl-fs-13); min-width: 260px;
}
.bl-input.sm { width: 64px; min-width: 0; padding-left: 10px; }
.bl-input:focus { outline: none; border-color: var(--bl-primary); }
.grow { flex: 1; }
.grow .bl-input { width: 100%; min-width: 0; }

.kb-table-wrap { flex: 0 0 auto; max-height: 52vh; overflow: auto; border: 1px solid var(--bl-border); border-radius: var(--bl-radius-3); background: var(--bl-bg-1); }
.kb-table { width: 100%; border-collapse: collapse; font-size: var(--bl-fs-13); }
.kb-table thead th {
  position: sticky; top: 0; text-align: left; padding: 8px 12px;
  background: var(--bl-bg-2); color: var(--bl-text-3); font-weight: 600; border-bottom: 1px solid var(--bl-border);
  white-space: nowrap;
}
.kb-table tbody td { padding: 8px 12px; border-bottom: 1px solid var(--bl-divider); color: var(--bl-text-1); vertical-align: top; }
.kb-table tbody tr:hover { background: var(--bl-bg-hover); }
.row-click { cursor: pointer; }
.row-click.is-sel, .row-click:hover { background: var(--bl-primary-soft); }
.mono { font-family: var(--bl-font-mono, Consolas, monospace); font-size: var(--bl-fs-12); }
.uri { color: var(--bl-text-3); max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.empty { text-align: center; color: var(--bl-text-3); padding: 24px; }
.chip {
  display: inline-block; padding: 1px 7px; margin: 1px 3px 1px 0; border-radius: 10px;
  background: var(--bl-bg-2); color: var(--bl-text-2); font-size: var(--bl-fs-11); border: 1px solid var(--bl-divider);
}

.detail-card { border: 1px solid var(--bl-primary-border); border-radius: var(--bl-radius-3); background: var(--bl-bg-1); padding: 12px 14px; }
.detail-hd { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; }
.detail-title { font-size: var(--bl-fs-15); font-weight: 600; color: var(--bl-text-1); }
.detail-grid { display: grid; grid-template-columns: 1fr; gap: 6px; margin-bottom: 8px; }
.detail-grid > div { display: flex; gap: 8px; align-items: baseline; }
.detail-grid .k { color: var(--bl-text-3); font-size: var(--bl-fs-12); min-width: 48px; flex-shrink: 0; }
.detail-grid .v { color: var(--bl-text-1); flex-wrap: wrap; }
.detail-sub { font-size: var(--bl-fs-12); color: var(--bl-text-3); margin: 4px 0 6px; }
.prop-list { display: flex; flex-direction: column; gap: 4px; }
.prop-item { display: flex; align-items: center; gap: 8px; }
.prop-name { font-size: var(--bl-fs-12); }
.prop-type { font-size: var(--bl-fs-11); padding: 1px 6px; border-radius: 8px; }
.prop-type.t-obj { background: var(--bl-primary-soft); color: var(--bl-primary); }
.prop-type.t-dt { background: var(--bl-warning-soft); color: var(--bl-warning); }
.feat {
  display: inline-block; padding: 1px 9px; margin: 1px 4px 1px 0; border-radius: 9px;
  font-size: var(--bl-fs-11); border: 1px solid var(--bl-divider);
}
.feat-on { background: var(--bl-success-soft); color: var(--bl-success); border-color: transparent; }
.feat-off { background: var(--bl-bg-2); color: var(--bl-text-3); opacity: .7; }

.hd-actions { display: flex; align-items: center; gap: 8px; }
.pager { display: flex; align-items: center; justify-content: center; gap: 12px; padding: 4px 0; }

/* 通用资源页 */
.res-empty { padding: 24px; text-align: center; }
.res-hd { padding: 2px 2px 8px; }
.res-title { font-size: var(--bl-fs-15); font-weight: 600; color: var(--bl-text-1); }
.uri-long { word-break: break-all; }
.res-types { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; margin-top: 6px; }
.res-types .k { color: var(--bl-text-3); font-size: var(--bl-fs-12); }
.chip.link { cursor: pointer; color: var(--bl-primary); background: var(--bl-primary-soft); border-color: var(--bl-primary-border); }

.triple-card { border: 1px solid var(--bl-border); border-radius: var(--bl-radius-3); background: var(--bl-bg-1); overflow: hidden; }
.triple-hd { padding: 8px 12px; background: var(--bl-bg-2); color: var(--bl-text-2); font-size: var(--bl-fs-12); font-weight: 600; border-bottom: 1px solid var(--bl-border); }
.triple-table .pred { width: 220px; color: var(--bl-text-3); border-right: 1px solid var(--bl-divider); }
.res-link { color: var(--bl-primary); cursor: pointer; }
.res-link:hover { color: var(--bl-primary-hover); text-decoration: underline; }
.lit { color: var(--bl-text-1); }
.tag { display: inline-block; margin-left: 6px; padding: 0 5px; border-radius: 8px; font-size: var(--bl-fs-11); background: var(--bl-bg-2); color: var(--bl-text-3); border: 1px solid var(--bl-divider); }

/* 下拉（模板/格式/历史/类型） */
.bl-select { height: 32px; padding: 0 8px; border: 1px solid var(--bl-border); border-radius: var(--bl-radius-2); background: var(--bl-bg-2); color: var(--bl-text-1); font-size: var(--bl-fs-13); }
.bl-select:focus { outline: none; border-color: var(--bl-primary); }

/* 目录：元数据条 */
.vocab-bar { display: flex; flex-wrap: wrap; gap: 6px 18px; padding: 8px 12px; background: var(--bl-bg-1); border: 1px solid var(--bl-border); border-radius: var(--bl-radius-3); }
.vocab-item { font-size: var(--bl-fs-12); color: var(--bl-text-3); }
.vocab-item b { color: var(--bl-text-1); font-weight: 600; margin-left: 3px; }

/* 目录：实体类型徽标 */
.kind-tag { font-size: var(--bl-fs-11); padding: 1px 7px; border-radius: 8px; }
.kind-class { background: var(--bl-primary-soft); color: var(--bl-primary); }
.kind-obj { background: var(--bl-ai-soft); color: var(--bl-ai); }
.kind-dt { background: var(--bl-warning-soft); color: var(--bl-warning); }
.kind-ann { background: var(--bl-bg-2); color: var(--bl-text-3); }
.kind-ind { background: var(--bl-success-soft); color: var(--bl-success); }
.kind-default { background: var(--bl-bg-2); color: var(--bl-text-2); }

/* 类树 */
.tree-wrap { padding: 4px 0; }
.tree { font-size: var(--bl-fs-13); }
.tree-row { display: flex; align-items: center; gap: 8px; padding: 4px 8px; border-radius: var(--bl-radius-2); }
.tree-row:hover { background: var(--bl-bg-hover); }
.tree-caret { width: 16px; text-align: center; cursor: pointer; color: var(--bl-text-3); user-select: none; }
.tree-caret.is-leaf { color: var(--bl-text-4); cursor: default; }
.tree-name { color: var(--bl-text-1); }
.tree-ln { color: var(--bl-text-4); font-size: var(--bl-fs-11); }

.sparql-box { border: 1px solid var(--bl-border); border-radius: var(--bl-radius-3); overflow: hidden; }
.sparql-input { width: 100%; min-height: 140px; border: 0; resize: vertical; padding: 10px 12px;
  background: var(--bl-bg-1); color: var(--bl-text-1); font-size: var(--bl-fs-13); line-height: 1.5; }
.sparql-input:focus { outline: none; }
.sparql-bar { display: flex; align-items: center; gap: 8px; padding: 8px 10px; background: var(--bl-bg-2); border-top: 1px solid var(--bl-border); }
.sparql-result { flex: 1; overflow: auto; }
.sparql-empty { padding: 20px; }
.ask-result { padding: 16px; }
.badge { display: inline-block; padding: 4px 14px; border-radius: var(--bl-radius-2); font-weight: 600; }
.badge-ok { background: var(--bl-success-soft); color: var(--bl-success); }
.badge-no { background: var(--bl-danger-soft); color: var(--bl-danger); }
.triples { background: var(--bl-bg-0); border: 1px solid var(--bl-border); border-radius: var(--bl-radius-2);
  padding: 10px; font-size: var(--bl-fs-12); line-height: 1.5; white-space: pre-wrap; color: var(--bl-text-2); max-height: 320px; overflow: auto; }

.vector-form { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.vf-field { display: inline-flex; align-items: center; gap: 6px; font-size: var(--bl-fs-12); color: var(--bl-text-3); }
.vector-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; color: var(--bl-text-3); }
.ve-ic { color: var(--bl-text-3); opacity: .5; }
.ve-msg { color: var(--bl-text-2); max-width: 520px; text-align: center; }
.ve-msg.sm, .sm { font-size: var(--bl-fs-11); }
.sim-bar { position: relative; height: 18px; min-width: 120px; background: var(--bl-bg-2); border-radius: 9px; overflow: hidden; }
.sim-fill { position: absolute; left: 0; top: 0; bottom: 0; background: linear-gradient(90deg, var(--bl-primary), var(--bl-primary-hover)); }
.sim-val { position: absolute; right: 6px; top: 0; line-height: 18px; font-size: var(--bl-fs-11); color: var(--bl-text-1); }

.fade-enter-active, .fade-leave-active { transition: opacity .15s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
