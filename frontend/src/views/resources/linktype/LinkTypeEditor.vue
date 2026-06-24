<template>
  <Teleport to="body">
    <transition name="lke-drawer">
      <aside v-if="open" class="lke-drawer" :style="{ width: width + 'px' }">
        <div class="lke-handle" @mousedown="onHandleDown" :class="resizing && 'is-resizing'"></div>

        <!-- 头部 (对齐 SharedPropertyDetailDrawer 视觉) -->
        <header class="lke-hd">
          <div class="lke-hd-l">
            <!-- 大头像图标 (按基数染色) -->
            <span class="lke-ic lke-ic-lg" :style="{ background: cardColor }"
                  v-html="BL.icon('link', 18, '#fff')"></span>
            <div class="lke-title-wrap">
              <div class="lke-title bl-truncate" :title="form.rdfs_label || form.link_type_id || props.fallbackTitle || '新建链接'">
                {{ form.rdfs_label || form.link_type_id || props.fallbackTitle || '新建链接' }}
                <span v-if="form.link_type_id" class="bl-mono bl-muted lke-code">- {{ form.link_type_id }}</span>
              </div>
              <div class="lke-meta" v-if="form.id">
                <span :class="['bl-tag', statusTagCls(form.status)]">{{ statusLabel(form.status) }}</span>
                <span class="bl-muted" style="margin-left:6px">更新于 {{ shortTime(form.updated_at) }}</span>
              </div>
            </div>
          </div>
          <div class="lke-hd-r">
            <!-- 编辑模式 (图标 + 文字 胶囊按钮, 与共享属性抽屉一致) -->
            <button :class="['bl-btn bl-btn-text bl-btn-sm lke-edit-btn', editMode && 'is-edit-on']"
                    :title="editMode ? '关闭编辑模式 · 切回只读' : '开启编辑模式'"
                    @click="editMode = !editMode">
              <span v-html="BL.icon(editMode ? 'edit' : 'lock', 12)"></span>
              <span style="margin-left:4px">{{ editMode ? '编辑' : '只读' }}</span>
            </button>
            <span class="lke-divider"></span>
            <button class="bl-btn bl-btn-text bl-btn-icon" :title="isMax ? '还原' : '最大化'"
                    @click="toggleMax"
                    v-html="BL.icon(isMax ? 'minimize' : 'maximize', 13)"></button>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="onClose" v-html="BL.icon('x', 14)"></button>
          </div>
        </header>

        <!-- 页签 (showTabs=true 时显示, 链接模块保留; 对象模块下的 TabLinkGraph 不要 tab) -->
        <nav v-if="showTabs" class="lke-tabs">
          <button v-for="t in tabs" :key="t.k"
                  :class="['lke-tab', activeTab === t.k && 'is-on']" @click="activeTab = t.k">{{ t.label }}</button>
        </nav>

        <!-- 主体 -->
        <div class="lke-body" :class="{ 'is-readonly': !editMode }">

          <section v-if="!showTabs || activeTab === 'basic'">
            <!-- 全局元数据 (状态 / ID / RID 一行三列) -->
            <div class="lke-meta-row">
              <div class="lke-meta-cell">
                <span class="lke-meta-lbl">状态</span>
                <select class="bl-input lke-status-sel" v-model="form.status" :disabled="!editMode">
                  <option value="experimental">实验中</option>
                  <option value="active">正式</option>
                  <option value="deprecated">已废弃</option>
                </select>
              </div>
              <div class="lke-meta-cell">
                <span class="lke-meta-lbl">ID</span>
                <input class="bl-input bl-mono" v-model="form.link_type_id" :disabled="!editMode || !!form.id"
                       placeholder="aircraft-flight-operate" />
                <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" :title="copiedId ? '已复制' : '复制 ID'"
                        @click="copy(form.link_type_id, 'id')"
                        v-html="BL.icon(copiedId ? 'check' : 'copy', 12)"></button>
              </div>
              <div class="lke-meta-cell">
                <span class="lke-meta-lbl">RID</span>
                <input class="bl-input bl-mono" :value="form.rid || '—'" disabled />
                <button class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm" :title="copiedRid ? '已复制' : '复制 RID'"
                        @click="copy(form.rid, 'rid')"
                        v-html="BL.icon(copiedRid ? 'check' : 'copy', 12)"></button>
              </div>
            </div>

            <!-- 名称 (rdfs_label, 列表/标题显示) + 所属领域 (与 ont_biz_category 对齐, 列表左侧分类树按此过滤) -->
            <div class="lke-meta-row lke-meta-row-2">
              <div class="lke-meta-cell">
                <span class="lke-meta-lbl">名称</span>
                <input class="bl-input" v-model="form.rdfs_label" :disabled="!editMode" placeholder="链接类型名称 (如: 执飞)" />
              </div>
              <div class="lke-meta-cell">
                <span class="lke-meta-lbl">领域</span>
                <select class="bl-input" v-model="form.category_code" :disabled="!editMode">
                  <option value="">— 通用 (不属于任何领域) —</option>
                  <option v-for="c in domainOpts" :key="c.code" :value="c.code">{{ c.indent }}{{ c.label }}</option>
                </select>
              </div>
            </div>

            <!-- 左右对称配置: 数据卡(上/下) + 中间数据源横跨行 + 展示卡 -->
            <div class="lke-sym">
              <!-- 中间数据源 (横跨两列, 位于基数与键之间) -->
              <div class="lke-data-source">
                <label class="lke-ds-toggle">
                  <input type="checkbox" v-model="form.is_data_source_rel" :true-value="1" :false-value="0" :disabled="!editMode" />
                  <span>中间数据源 (依托物理中间表关联)</span>
                </label>
                <button v-if="form.is_data_source_rel"
                        :class="['lke-table-btn', !form.rel_data_table && 'is-empty']"
                        :disabled="!editMode"
                        @click="openTablePicker">
                  <span v-if="form.rel_data_table" class="bl-mono">{{ form.rel_data_table }}</span>
                  <span v-else>
                    <span v-html="BL.icon('plus', 11)"></span>
                    <span style="margin-left:4px">选择中间表 (物理表)</span>
                  </span>
                  <span v-if="form.rel_data_table" class="lke-table-chev" v-html="BL.icon('chevronDown', 10)"></span>
                </button>
                <button v-if="form.is_data_source_rel && form.rel_data_table && editMode"
                        class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon"
                        title="清除中间表"
                        @click="form.rel_data_table = ''"
                        v-html="BL.icon('x', 11)"></button>
              </div>
              <!-- 卡 1a (数据上半·源): 头部 + 基数 -->
              <div class="lke-card lke-card-data-top is-source">
                  <div class="lke-col-hd">
                    <span class="lke-col-tag">源</span>
                    <select class="bl-input" v-model="form.l_object_type_id" :disabled="!editMode">
                      <option value="">— 源对象类型 —</option>
                      <option v-for="c in classOptions" :key="'l-'+c.id" :value="c.id">{{ c.cn || c.api_name }}</option>
                    </select>
                    <label class="lke-enabled" :title="form.l_enabled ? '已启用' : '已禁用'">
                      <input type="checkbox" v-model="form.l_enabled" :true-value="1" :false-value="0" :disabled="!editMode" />
                      <span class="lke-en-track"><span class="lke-en-dot"></span></span>
                    </label>
                  </div>

                  <div class="lke-cell">
                    <div class="lke-cell-lbl">基数 <span class="bl-muted" v-html="BL.icon('help', 11)" title="约束源端关联数量"></span></div>
                    <div class="lke-card-seg">
                      <button :class="['lke-card-btn', form.l_cardinality === 'one' && 'is-on']"
                              @click="setCard('l', 'one')" :disabled="!editMode">一个</button>
                      <button :class="['lke-card-btn', form.l_cardinality === 'many' && 'is-on']"
                              @click="setCard('l', 'many')" :disabled="!editMode">多个</button>
                    </div>
                  </div>
              </div>

              <!-- 卡 1b (数据下半·源): 键 -->
              <div class="lke-card lke-card-data-bot is-source">
                  <div class="lke-cell">
                    <div class="lke-cell-lbl">{{ keyLabel('l') }} <span class="bl-muted" v-html="BL.icon('help', 11)" title="参与关联的字段"></span></div>
                    <div class="lke-keys-hd" :class="{ 'is-single': !form.is_data_source_rel }">
                      <span class="lke-key-seq"></span>
                      <span>对象属性</span>
                      <span v-if="form.is_data_source_rel">关联属性</span>
                    </div>
                    <div class="lke-keys">
                      <div v-for="(m, i) in leftMappings" :key="'lm-'+i" class="lke-key-row">
                        <span class="lke-key-seq">{{ i + 1 }}</span>
                        <select class="bl-input bl-mono" v-model="m.object_field"
                                :disabled="!editMode || !form.l_object_type_id">
                          <option value="">{{ form.l_object_type_id ? '— 对象属性 —' : '请先选源对象类型' }}</option>
                          <option v-for="o in leftObjectFields" :key="'lf-'+i+'-'+o.value" :value="o.value">{{ o.label }}</option>
                        </select>
                        <select v-if="form.is_data_source_rel" class="bl-input bl-mono" v-model="m.join_table_column"
                                :disabled="!editMode || !form.rel_data_table">
                          <option value="">{{ joinPlaceholder('l') }}</option>
                          <option v-for="o in leftJoinOptions" :key="'lj-'+i+'-'+o.value" :value="o.value">{{ o.label }}</option>
                        </select>
                        <button v-if="editMode && leftMappings.length > 1" class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon"
                                @click="removeMapping('left', i)" v-html="BL.icon('trash', 11)"></button>
                      </div>
                      <div class="lke-key-tip">
                        <span v-html="BL.icon('help', 11)"></span>
                        <span>支持复合主键, 可添加多个列映射</span>
                        <a v-if="editMode" class="lke-add-link" @click="addMapping('left')">
                          <span v-html="BL.icon('plus', 11)"></span><span>添加</span>
                        </a>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 卡 3 (展示·源): grid 区 show-l (CSS 强制放第 2 行第 1 列) -->
                <div class="lke-card lke-card-display is-source">
                  <div class="lke-cell">
                    <div class="lke-cell-lbl">显示名称 <span class="bl-muted" v-html="BL.icon('help', 11)" title="前端展示文案"></span></div>
                    <div class="lke-input-wrap">
                      <input class="bl-input" v-model="form.l_display_name" :disabled="!editMode" placeholder="如: 执飞航班" />
                      <span v-if="form.l_display_name" class="lke-valid-tag">有效</span>
                    </div>
                  </div>

                  <div class="lke-cell" :class="{ 'is-na': form.l_cardinality !== 'many' }">
                    <div class="lke-cell-lbl">复数显示名称
                      <span v-if="form.l_cardinality !== 'many'" class="bl-muted lke-na-tip">(基数=多个 时启用)</span>
                    </div>
                    <input class="bl-input" v-model="form.l_plural_name"
                           :disabled="!editMode || form.l_cardinality !== 'many'"
                           :placeholder="form.l_cardinality === 'many' ? '如: 执飞航班列表' : '—'" />
                  </div>

                  <div class="lke-cell">
                    <div class="lke-cell-lbl">可见性</div>
                    <select class="bl-input" v-model="form.l_visibility" :disabled="!editMode">
                      <option value="normal">正常</option>
                      <option value="prominent">优先</option>
                      <option value="hidden">隐藏</option>
                    </select>
                  </div>

                  <div class="lke-cell">
                    <div class="lke-cell-lbl">API 名称 <span class="bl-muted" v-html="BL.icon('help', 11)" title="代码调用名,驼峰命名,同对象下唯一"></span></div>
                    <div class="lke-input-wrap">
                      <input class="bl-input bl-mono" v-model="form.l_api_name" :disabled="!editMode" placeholder="operatedFlights" />
                      <span v-if="isValidApi(form.l_api_name)" class="lke-valid-tag">有效</span>
                    </div>
                  </div>

                  <!-- 类型类 (源) -->
                  <div class="lke-cell lke-tc-cell">
                    <div class="lke-cell-lbl lke-tc-hd">
                      <span>类型类 <span class="bl-muted">({{ leftTypeClasses.length }})</span></span>
                      <a v-if="editMode" class="lke-add-link" @click="addTypeClass('left')">
                        <span v-html="BL.icon('plus', 11)"></span><span>添加</span>
                      </a>
                    </div>
                    <div v-if="leftTypeClasses.length" class="lke-tc-list">
                      <div v-for="(tc, i) in leftTypeClasses" :key="'l-tc-'+i" class="lke-tc-row">
                        <input class="bl-input bl-mono" v-model="tc.category" placeholder="种类 (vertex)" :disabled="!editMode" />
                        <span class="bl-muted">:</span>
                        <input class="bl-input bl-mono" v-model="tc.name" placeholder="名称 (component)" :disabled="!editMode" />
                        <button v-if="editMode" class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm"
                                @click="removeTypeClass('left', i)" v-html="BL.icon('x', 11)"></button>
                      </div>
                    </div>
                    <div v-else class="bl-muted lke-tc-empty">尚未配置类型类</div>
                  </div>
                </div>

              <!-- 卡 2a (数据上半·目标): 头部 + 基数 -->
              <div class="lke-card lke-card-data-top is-target">
                  <div class="lke-col-hd">
                    <span class="lke-col-tag lke-col-tag-r">目标</span>
                    <select class="bl-input" v-model="form.r_object_type_id" :disabled="!editMode">
                      <option value="">— 目标对象类型 —</option>
                      <option v-for="c in classOptions" :key="'r-'+c.id" :value="c.id">{{ c.cn || c.api_name }}</option>
                    </select>
                    <label class="lke-enabled">
                      <input type="checkbox" v-model="form.r_enabled" :true-value="1" :false-value="0" :disabled="!editMode" />
                      <span class="lke-en-track"><span class="lke-en-dot"></span></span>
                    </label>
                  </div>

                  <div class="lke-cell">
                    <div class="lke-cell-lbl">基数</div>
                    <div class="lke-card-seg">
                      <button :class="['lke-card-btn', form.r_cardinality === 'one' && 'is-on']"
                              @click="setCard('r', 'one')" :disabled="!editMode">一个</button>
                      <button :class="['lke-card-btn', form.r_cardinality === 'many' && 'is-on']"
                              @click="setCard('r', 'many')" :disabled="!editMode">多个</button>
                    </div>
                  </div>
              </div>

              <!-- 卡 2b (数据下半·目标): 键 -->
              <div class="lke-card lke-card-data-bot is-target">
                  <div class="lke-cell">
                    <div class="lke-cell-lbl">{{ keyLabel('r') }}</div>
                    <div class="lke-keys-hd" :class="{ 'is-single': !form.is_data_source_rel }">
                      <span class="lke-key-seq"></span>
                      <span>对象属性</span>
                      <span v-if="form.is_data_source_rel">关联属性</span>
                    </div>
                    <div class="lke-keys">
                      <div v-for="(m, i) in rightMappings" :key="'rm-'+i" class="lke-key-row">
                        <span class="lke-key-seq">{{ i + 1 }}</span>
                        <select class="bl-input bl-mono" v-model="m.object_field"
                                :disabled="!editMode || !form.r_object_type_id">
                          <option value="">{{ form.r_object_type_id ? '— 对象属性 —' : '请先选目标对象类型' }}</option>
                          <option v-for="o in rightObjectFields" :key="'rf-'+i+'-'+o.value" :value="o.value">{{ o.label }}</option>
                        </select>
                        <select v-if="form.is_data_source_rel" class="bl-input bl-mono" v-model="m.join_table_column"
                                :disabled="!editMode || !form.rel_data_table">
                          <option value="">{{ joinPlaceholder('r') }}</option>
                          <option v-for="o in rightJoinOptions" :key="'rj-'+i+'-'+o.value" :value="o.value">{{ o.label }}</option>
                        </select>
                        <button v-if="editMode && rightMappings.length > 1" class="bl-btn bl-btn-text bl-btn-sm bl-btn-icon"
                                @click="removeMapping('right', i)" v-html="BL.icon('trash', 11)"></button>
                      </div>
                      <div class="lke-key-tip">
                        <span v-html="BL.icon('help', 11)"></span>
                        <span>支持复合主键, 可添加多个列映射</span>
                        <a v-if="editMode" class="lke-add-link" @click="addMapping('right')">
                          <span v-html="BL.icon('plus', 11)"></span><span>添加</span>
                        </a>
                      </div>
                    </div>
                  </div>
                </div>

              <!-- 卡 4 (展示·目标): grid 区 show-r -->
              <div class="lke-card lke-card-display is-target">
                  <div class="lke-cell">
                    <div class="lke-cell-lbl">显示名称</div>
                    <div class="lke-input-wrap">
                      <input class="bl-input" v-model="form.r_display_name" :disabled="!editMode" placeholder="如: 执飞机型" />
                      <span v-if="form.r_display_name" class="lke-valid-tag">有效</span>
                    </div>
                  </div>

                  <div class="lke-cell" :class="{ 'is-na': form.r_cardinality !== 'many' }">
                    <div class="lke-cell-lbl">复数显示名称
                      <span v-if="form.r_cardinality !== 'many'" class="bl-muted lke-na-tip">(基数=多个 时启用)</span>
                    </div>
                    <input class="bl-input" v-model="form.r_plural_name"
                           :disabled="!editMode || form.r_cardinality !== 'many'"
                           :placeholder="form.r_cardinality === 'many' ? '如: 执飞机队列表' : '—'" />
                  </div>

                  <div class="lke-cell">
                    <div class="lke-cell-lbl">可见性</div>
                    <select class="bl-input" v-model="form.r_visibility" :disabled="!editMode">
                      <option value="normal">正常</option>
                      <option value="prominent">优先</option>
                      <option value="hidden">隐藏</option>
                    </select>
                  </div>

                  <div class="lke-cell">
                    <div class="lke-cell-lbl">API 名称</div>
                    <div class="lke-input-wrap">
                      <input class="bl-input bl-mono" v-model="form.r_api_name" :disabled="!editMode" placeholder="operatingAircraft" />
                      <span v-if="isValidApi(form.r_api_name)" class="lke-valid-tag">有效</span>
                    </div>
                  </div>

                  <!-- 类型类 (目标) -->
                  <div class="lke-cell lke-tc-cell">
                    <div class="lke-cell-lbl lke-tc-hd">
                      <span>类型类 <span class="bl-muted">({{ rightTypeClasses.length }})</span></span>
                      <a v-if="editMode" class="lke-add-link" @click="addTypeClass('right')">
                        <span v-html="BL.icon('plus', 11)"></span><span>添加</span>
                      </a>
                    </div>
                    <div v-if="rightTypeClasses.length" class="lke-tc-list">
                      <div v-for="(tc, i) in rightTypeClasses" :key="'r-tc-'+i" class="lke-tc-row">
                        <input class="bl-input bl-mono" v-model="tc.category" placeholder="种类 (vertex)" :disabled="!editMode" />
                        <span class="bl-muted">:</span>
                        <input class="bl-input bl-mono" v-model="tc.name" placeholder="名称 (component)" :disabled="!editMode" />
                        <button v-if="editMode" class="bl-btn bl-btn-text bl-btn-icon bl-btn-sm"
                                @click="removeTypeClass('right', i)" v-html="BL.icon('x', 11)"></button>
                      </div>
                    </div>
                    <div v-else class="bl-muted lke-tc-empty">尚未配置类型类</div>
                  </div>
              </div>
            </div>

            <!-- 备注 -->
            <div class="lke-section">
              <div class="lke-section-hd">备注</div>
              <textarea class="bl-textarea" rows="3" v-model="form.rdfs_comment" :disabled="!editMode"
                        placeholder="业务说明 / 使用场景"></textarea>
            </div>
          </section>

          <!-- 链接关系图 (仅 showTabs 模式下作为第二个 tab 内容) -->
          <section v-if="showTabs && activeTab === 'graph'" class="lke-graph-stub">
            <div class="bl-empty" style="padding:48px">
              <span v-html="BL.icon('link', 32)"></span>
              <div style="margin-top:12px">参考「对象类型 → 对象图谱」中的链接可视化</div>
              <div class="bl-muted" style="font-size:12px;margin-top:4px">该页签集成 Canvas 链接图谱组件 (与对象类型详情共用)</div>
            </div>
          </section>

        </div>

        <!-- 底部 -->
        <footer class="lke-ft">
          <button v-if="editMode && form.id" class="bl-btn bl-btn-danger" @click="onDelete">
            <span v-html="BL.icon('trash', 12)"></span><span style="margin-left:4px">删除</span>
          </button>
          <span style="flex:1"></span>
          <button class="bl-btn" @click="onClose">取消</button>
          <button v-if="editMode" class="bl-btn bl-btn-primary" :disabled="!canSave" @click="onSave">保存</button>
        </footer>
      </aside>
    </transition>

    <!-- 中间表选择面板 (单选, 与新建对象类型向导同款) -->
    <div v-if="tablePickerOpen" class="bl-modal-mask lke-table-mask" @click.self="tablePickerOpen=false">
      <div class="bl-modal lke-table-modal">
        <div class="bl-modal-hd">选择中间表 (单选)</div>
        <div class="bl-modal-body lke-table-body">
          <input class="bl-input" placeholder="搜索物理表" v-model="tablePickerQ" />
          <div class="lke-table-list">
            <div v-for="t in tableOptions" :key="t.physical_table"
                 :class="['tbl-row', form.rel_data_table === t.physical_table && 'is-on']"
                 @click="pickTable(t)">
              <span class="tbl-side"></span>
              <span class="bl-mono" style="font-weight:500">{{ t.physical_table }}</span>
              <span class="bl-muted" style="font-size:11px;margin-left:6px">{{ t.columns.length }} 字段</span>
              <span v-if="form.rel_data_table === t.physical_table" class="bl-tag bl-tag-primary" style="margin-left:auto;font-size:10px">已选</span>
            </div>
            <div v-if="!tableOptions.length" class="bl-empty" style="padding:24px">未匹配到物理表</div>
          </div>
        </div>
        <div class="bl-modal-ft">
          <button class="bl-btn" @click="tablePickerOpen=false">取消</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { linkTypeApi, resourceApi, categoryApi } from '@/api'

const props = defineProps({
  open: { type: Boolean, default: false },
  linkId: { type: String, default: '' },
  allClasses: { type: Array, default: () => [] },
  /* 是否显示顶部 tab (基础信息 / 链接关系图)
     默认 true: 链接模块主页 LinkTypes.vue 用; 对象模块下 TabLinkGraph 传 false */
  showTabs: { type: Boolean, default: true },
  /* 新建模式时的默认标题 (e.g. 图谱里选中边的关系名).
     有 form.rdfs_label / link_type_id 时仍以那个为准. */
  fallbackTitle: { type: String, default: '' },
  /* 新建模式时默认带入的领域 (来自列表左侧已选行业分类) */
  initCategory: { type: String, default: '' }
})
const emit = defineEmits(['update:open', 'saved', 'deleted'])

const tabs = [
  { k: 'basic', label: '基础信息' },
  { k: 'graph', label: '链接关系图' }
]
const activeTab = ref('basic')
const editMode = ref(true)
const form = reactive(defaultForm())
function defaultForm() {
  return {
    id: '', link_type_id: '', rid: '', status: 'experimental',
    l_object_type_id: '', r_object_type_id: '',
    l_cardinality: 'one', r_cardinality: 'one',
    l_display_name: '', l_plural_name: '', r_display_name: '', r_plural_name: '',
    l_visibility: 'normal', r_visibility: 'normal',
    l_api_name: '', r_api_name: '',
    l_enabled: 1, r_enabled: 1,
    is_data_source_rel: 0, rel_data_table: '',
    rdfs_label: '', rdfs_comment: '', category_code: '',
    mappings: [], type_classes: []
  }
}

const leftMappings = computed({
  get: () => form.mappings.filter(m => m.side === 'left'),
  set: (v) => {}
})
const rightMappings = computed({
  get: () => form.mappings.filter(m => m.side === 'right'),
  set: (v) => {}
})

const classOptions = computed(() => (props.allClasses || []).map(c => ({
  id: c.id, cn: c.display_name || c.rdfs_label || c.api_name, api_name: c.api_name
})))

/* —— 所属领域候选 (行业分类树拍平, 与其他模块一致) —— */
const domainOpts = ref([])
async function loadDomainOpts() {
  if (domainOpts.value.length) return
  const tree = await categoryApi.tree().catch(() => [])
  const list = []
  const walk = (ns, depth) => (ns || []).forEach(n => {
    if (n.categoryCode) list.push({ code: n.categoryCode, label: n.label || n.rdfsLabel || n.categoryCode, indent: '　'.repeat(depth) })
    if (n.children) walk(n.children, depth + 1)
  })
  walk(tree, 0)
  domainOpts.value = list
}

/* —— 打开生命周期 —— */
async function loadEditor() {
  activeTab.value = 'basic'
  loadDomainOpts()
  if (props.linkId) {
    const res = await linkTypeApi.get(props.linkId).catch(() => null)
    if (res) {
      Object.assign(form, defaultForm(), res)
      form.mappings = (res.mappings || []).map(m => ({ ...m }))
      form.type_classes = (res.type_classes || []).map(tc => ({ ...tc }))
    } else {
      Object.assign(form, defaultForm())
      form.mappings = []
      form.type_classes = []
    }
  } else {
    Object.assign(form, defaultForm())
    if (props.initCategory) form.category_code = props.initCategory   // 带入左侧已选领域
    form.mappings = [
      { side: 'left',  seq: 1, object_field: '', join_table_column: '' },
      { side: 'right', seq: 1, object_field: '', join_table_column: '' }
    ]
    form.type_classes = []
  }
  editMode.value = true   // 不论新建 / 已有, 默认就是编辑模式
  if (form.l_object_type_id) ensureProps(form.l_object_type_id)
  if (form.r_object_type_id) ensureProps(form.r_object_type_id)
  // 非中间表模式: 加载老数据若两侧不等, 自动补齐 (一次性修复历史数据)
  syncMappingCountsIfPaired()
}
// open 0→1 加载;切换 linkId (抽屉常驻、连续点不同链接) 也要重新加载
watch(() => props.open,  v => { if (v) loadEditor() })
watch(() => props.linkId, () => { if (props.open) loadEditor() })

/* —— 基数切换 —— */
function setCard(side, val) {
  if (!editMode.value) return
  if (side === 'l') form.l_cardinality = val
  else form.r_cardinality = val
}
function keyLabel(side) {
  const card = side === 'l' ? form.l_cardinality : form.r_cardinality
  if (form.is_data_source_rel) return '键 (中间表列)'
  if (card === 'one') return '键 (主键)'
  return '键 (外键)'
}

/* —— 映射 (左右独立增删,视觉自动占位补齐) —— */
function addMapping(side) {
  form.mappings.push({ side, seq: 0, object_field: '', join_table_column: '' })
  // 非中间表模式: 对端自动同步加一条, 保证 1:1 对应
  if (!form.is_data_source_rel) {
    const other = side === 'left' ? 'right' : 'left'
    const thisLen  = form.mappings.filter(m => m.side === side).length
    const otherLen = form.mappings.filter(m => m.side === other).length
    if (otherLen < thisLen) form.mappings.push({ side: other, seq: 0, object_field: '', join_table_column: '' })
  }
  renumberMappings()
}
function removeMapping(side, idx) {
  const list = side === 'left' ? leftMappings.value : rightMappings.value
  const target = list[idx]
  if (target) form.mappings = form.mappings.filter(m => m !== target)
  // 非中间表模式: 对端同 idx 也一起删, 保持等长
  if (!form.is_data_source_rel) {
    const other = side === 'left' ? 'right' : 'left'
    const otherList = form.mappings.filter(m => m.side === other)
    const otherTarget = otherList[idx]
    if (otherTarget) form.mappings = form.mappings.filter(m => m !== otherTarget)
  }
  renumberMappings()
}
function renumberMappings() {
  let lSeq = 1, rSeq = 1
  form.mappings.forEach(m => {
    if (m.side === 'left') m.seq = lSeq++
    else m.seq = rSeq++
  })
}

/* 非中间表模式下补齐两侧, 保证 left.length === right.length
   触发场景: ① 切到非中间表模式 ② 加载已有数据时 */
function syncMappingCountsIfPaired() {
  if (form.is_data_source_rel) return
  const lLen = form.mappings.filter(m => m.side === 'left').length
  const rLen = form.mappings.filter(m => m.side === 'right').length
  if (lLen === rLen) return
  if (lLen > rLen) {
    for (let i = rLen; i < lLen; i++) form.mappings.push({ side: 'right', seq: 0, object_field: '', join_table_column: '' })
  } else {
    for (let i = lLen; i < rLen; i++) form.mappings.push({ side: 'left',  seq: 0, object_field: '', join_table_column: '' })
  }
  renumberMappings()
}
watch(() => form.is_data_source_rel, (v) => { if (!v) syncMappingCountsIfPaired() })


/* —— 类型类 (按 side 分到左右两侧, 每个 entry 含 side: 'left'|'right') —— */
const leftTypeClasses  = computed(() => (form.type_classes || []).filter(t => (t.side || 'left') === 'left'))
const rightTypeClasses = computed(() => (form.type_classes || []).filter(t => t.side === 'right'))
function addTypeClass(side) {
  form.type_classes = [...(form.type_classes || []), { side, category: '', name: '', applicable_type: 'relation' }]
}
function removeTypeClass(side, idx) {
  const list = side === 'left' ? leftTypeClasses.value : rightTypeClasses.value
  const target = list[idx]
  if (target) form.type_classes = form.type_classes.filter(t => t !== target)
}

/* —— 校验 —— */
function isValidApi(s) {
  return !!s && /^[a-z][A-Za-z0-9]*$/.test(s)
}
const canSave = computed(() =>
  form.link_type_id && /^[a-z][a-z0-9-]*$/.test(form.link_type_id)
  && form.l_object_type_id && form.r_object_type_id
  && form.l_display_name && form.r_display_name
  && isValidApi(form.l_api_name) && isValidApi(form.r_api_name)
  && (!form.is_data_source_rel || form.rel_data_table)
  // 非中间表模式下两侧数量由 addMapping/removeMapping 自动对齐, 此处不再校验
)

/* —— 保存 / 删除 —— */
async function onSave() {
  try {
    if (form.id) {
      await linkTypeApi.update(form.id, { ...form })
      BL.success('已保存')
    } else {
      await linkTypeApi.create({ ...form })
      BL.success('已创建')
    }
    emit('saved')
    emit('update:open', false)
  } catch (e) {
    BL.error(e?.msg || '保存失败')
  }
}
async function onDelete() {
  const ok = await BL.confirm({
    title: '删除链接',
    content: `确定删除「${form.rdfs_label || form.link_type_id}」?`,
    danger: true, okText: '删除'
  })
  if (!ok) return
  try {
    await linkTypeApi.remove(form.id)
    BL.success('已删除')
    emit('deleted')
  } catch (e) { BL.error(e?.msg || '删除失败') }
}
function onClose() { emit('update:open', false) }

/* —— RID / ID 复制 —— */
const copiedId = ref(false)
const copiedRid = ref(false)
async function copy(text, kind) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    if (kind === 'id') { copiedId.value = true; setTimeout(() => copiedId.value = false, 1500) }
    else { copiedRid.value = true; setTimeout(() => copiedRid.value = false, 1500) }
  } catch {}
}

/* —— 宽度 / 最大化 / 拖拽 —— */
const width = ref(parseInt(localStorage.getItem('bl.lke.width') || '900'))
const isMax = computed(() => width.value >= 1200)
function toggleMax() {
  width.value = isMax.value ? 900 : 1200
  localStorage.setItem('bl.lke.width', String(width.value))
}
const resizing = ref(false)
function onHandleDown(ev) {
  ev.preventDefault()
  resizing.value = true
  const startX = ev.clientX, baseW = width.value
  function move(e) {
    const dx = startX - e.clientX
    width.value = Math.max(720, Math.min(Math.floor(window.innerWidth * 0.95), baseW + dx))
  }
  function up() {
    resizing.value = false
    localStorage.setItem('bl.lke.width', String(width.value))
    window.removeEventListener('mousemove', move)
    window.removeEventListener('mouseup', up)
    document.body.style.userSelect = ''
  }
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', move)
  window.addEventListener('mouseup', up)
}

/* —— 工具 —— */
function statusLabel(s) { return ({ active: '正式', experimental: '实验中', deprecated: '已废弃' })[s] || s }
function statusTagCls(s) {
  return ({ active: 'bl-tag-success', experimental: 'bl-tag-warning', deprecated: 'bl-tag-danger' })[s] || ''
}
function shortTime(t) { if (!t) return '—'; return String(t).slice(0, 16) }

/* 头部图标染色: 按基数对应 (与列表行图标配色一致) */
const cardColor = computed(() => {
  const k = `${form.l_cardinality}:${form.r_cardinality}`
  return ({ 'one:one': '#1677ff', 'one:many': '#00B42A', 'many:one': '#FF7D00', 'many:many': '#722ED1' })[k] || '#1677ff'
})

/* —— 中间表选择面板 (单选) ——
   数据源: 暂时用 mock, 后续可改为 datasourceApi.tables() 或 backend 提供的物理表清单接口 */
const MOCK_TABLES = [
  { physical_table: 't_hydrology_station', columns: ['station_code','station_name','river_id','region_id','longitude','latitude','altitude','status','created_at','updated_at','remark','owner'] },
  { physical_table: 't_river',             columns: ['river_id','river_name','basin_id','length_km','source','mouth'] },
  { physical_table: 't_water_quality',     columns: ['sample_id','station_id','ph','do_value','ammonia','tn','tp','sampled_at'] },
  { physical_table: 't_reservoir',         columns: ['reservoir_id','reservoir_name','basin_id','capacity','height','built_year','status','region_id','remark'] },
  { physical_table: 't_water_basin_info',  columns: ['basin_id','basin_name','area_km2','region_id','remark'] },
  { physical_table: 't_water_level_monitor', columns: ['monitor_id','station_id','water_level','recorded_at','source','warning','remark'] },
  { physical_table: 'aircraft_flight_rel', columns: ['aircraft_id','flight_no','flight_date','created_at'] },
  { physical_table: 'employee_dept_rel',   columns: ['employee_id','dept_id','position'] },
  { physical_table: 'crew_flight_rel',     columns: ['crew_id','flight_no','role','created_at'] }
]
/* 已选中间表的列清单 — 用于 is_data_source_rel=1 时的关联属性下拉 */
const middleTableColumns = computed(() => {
  const t = MOCK_TABLES.find(x => x.physical_table === form.rel_data_table)
  return t ? t.columns : []
})

/* —— 对象类型属性缓存 (惰性加载, 同会话复用) —— */
const propsCache = reactive({})  // { [classId]: [{api_name, display_name}, ...] }
async function ensureProps(classId) {
  if (!classId || propsCache[classId]) return
  propsCache[classId] = []
  try {
    const list = await resourceApi.properties(classId)
    propsCache[classId] = Array.isArray(list) ? list : (list?.data || list?.rows || [])
  } catch { propsCache[classId] = [] }
}
watch(() => form.l_object_type_id, v => ensureProps(v))
watch(() => form.r_object_type_id, v => ensureProps(v))

function fieldOptionsOf(classId) {
  const list = propsCache[classId] || []
  return list.map(p => ({ value: p.api_name || p.prop_code, label: p.display_name || p.api_name || p.prop_code }))
            .filter(o => o.value)
}
/* 对象属性下拉 — 当前侧自身的属性 */
const leftObjectFields  = computed(() => fieldOptionsOf(form.l_object_type_id))
const rightObjectFields = computed(() => fieldOptionsOf(form.r_object_type_id))
/* 关联属性下拉 — 中间表模式取中间表列; 否则取对端对象的属性 (FK ↔ PK 形态) */
const leftJoinOptions  = computed(() => form.is_data_source_rel
  ? middleTableColumns.value.map(c => ({ value: c, label: c }))
  : rightObjectFields.value)
const rightJoinOptions = computed(() => form.is_data_source_rel
  ? middleTableColumns.value.map(c => ({ value: c, label: c }))
  : leftObjectFields.value)

function joinPlaceholder(side) {
  if (form.is_data_source_rel) return form.rel_data_table ? '— 关联属性 —' : '请先选择中间表'
  const otherId = side === 'l' ? form.r_object_type_id : form.l_object_type_id
  return otherId ? '— 关联属性 —' : (side === 'l' ? '请先选目标对象类型' : '请先选源对象类型')
}

const tablePickerOpen = ref(false)
const tablePickerQ = ref('')
function openTablePicker() {
  if (!editMode.value) return
  tablePickerQ.value = ''
  tablePickerOpen.value = true
}
const tableOptions = computed(() => {
  const k = tablePickerQ.value.trim().toLowerCase()
  if (!k) return MOCK_TABLES
  return MOCK_TABLES.filter(t => t.physical_table.toLowerCase().includes(k))
})
function pickTable(t) {
  form.rel_data_table = t.physical_table
  tablePickerOpen.value = false
}
</script>

<style scoped>
.lke-drawer {
  position: fixed; top: 0; right: 0; bottom: 0;
  background: var(--bl-bg-1);
  border-left: 1px solid var(--bl-border);
  box-shadow: -4px 0 16px rgba(0,0,0,.10);
  display: flex; flex-direction: column;
  min-width: 720px; max-width: 95vw; z-index: 1000;
  overflow: hidden;
}
.lke-handle {
  position: absolute; left: -2px; top: 0; bottom: 0; width: 5px;
  cursor: col-resize; transition: background-color .15s; z-index: 6;
}
.lke-handle:hover, .lke-handle.is-resizing { background: var(--bl-primary); }
.lke-drawer-enter-active, .lke-drawer-leave-active { transition: transform .25s, opacity .2s; }
.lke-drawer-enter-from, .lke-drawer-leave-to { transform: translateX(20px); opacity: 0; }

/* 头部 (对齐 SharedPropertyDetailDrawer .detail-hd 视觉) */
.lke-hd {
  height: 56px; padding: 0 14px;
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid var(--bl-divider);
  gap: 8px; flex-shrink: 0;
}
.lke-hd-l { display: flex; align-items: center; gap: 10px; min-width: 0; flex: 1; }
.lke-ic {
  width: 22px; height: 22px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.lke-ic-lg { width: 36px; height: 36px; border-radius: 8px; }
.lke-title-wrap { min-width: 0; }
.lke-title { font-size: 14px; font-weight: 600; color: var(--bl-text-1); }
.lke-code { font-size: 12px; font-weight: 400; margin-left: 4px; }
.lke-meta { font-size: 11px; color: var(--bl-text-3); margin-top: 2px; display: flex; align-items: center; }
.lke-hd-r { display: inline-flex; align-items: center; gap: 4px; flex-shrink: 0; }
.lke-divider { width: 1px; height: 18px; background: var(--bl-divider); margin: 0 6px; flex-shrink: 0; }

/* 页签 (showTabs=true 时) */
.lke-tabs { display: flex; padding: 0 16px; border-bottom: 1px solid var(--bl-divider); flex-shrink: 0; }
.lke-tab {
  padding: 10px 14px; font-size: 13px;
  color: var(--bl-text-2); cursor: pointer;
  background: transparent; border: 0; border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}
.lke-tab:hover { color: var(--bl-text-1); }
.lke-tab.is-on { color: var(--bl-primary); border-color: var(--bl-primary); font-weight: 500; }

.lke-graph-stub { display: flex; align-items: center; justify-content: center; padding: 60px 20px; text-align: center; color: var(--bl-text-3); }

/* 编辑模式按钮 (图标 + 文字 胶囊): 默认灰, 启用蓝 */
.lke-edit-btn {
  display: inline-flex; align-items: center;
  padding: 0 8px; height: 26px;
  font-size: 12px; color: var(--bl-text-2);
  border-radius: 4px;
  transition: background-color .15s, color .15s;
}
.lke-edit-btn:hover { color: var(--bl-text-1); background: var(--bl-bg-2); }
.lke-edit-btn.is-edit-on {
  color: var(--bl-primary);
  background: var(--bl-primary-soft);
  font-weight: 500;
}


/* 主体 */
.lke-body { flex: 1; overflow: auto; padding: 14px 18px; }
.lke-body.is-readonly :deep(input):disabled,
.lke-body.is-readonly :deep(select):disabled,
.lke-body.is-readonly :deep(textarea):disabled,
.lke-body.is-readonly :deep(button):disabled { background: var(--bl-bg-2); color: var(--bl-text-2); }

/* 全局元数据: 状态 / ID / RID 一行三列 */
.lke-meta-row {
  display: grid;
  grid-template-columns: 200px 1fr 1fr;
  gap: 12px;
  padding: 10px 12px; background: var(--bl-bg-2); border-radius: 6px;
  margin-bottom: 12px;
}
.lke-meta-cell { display: flex; align-items: center; gap: 6px; min-width: 0; }
.lke-meta-lbl {
  width: 36px; flex-shrink: 0;
  font-size: 12px; color: var(--bl-text-3);
  text-align: right;
}
.lke-meta-cell .bl-input { flex: 1; min-width: 0; height: 28px; font-size: 12px; }
.lke-status-sel { width: 100%; }
/* 名称 + 领域行: 两列, 紧贴上方元数据行 */
.lke-meta-row-2 { grid-template-columns: 1fr 1fr; margin-top: -4px; }
.lke-meta-row-2 .lke-meta-lbl { width: 36px; }

/* 左右对称 — 4 行网格: 数据上半 / 中间数据源横跨 / 数据下半 / 展示
   每行的左右两张卡 stretch 到同行最高, 中间数据源横跨两列 */
.lke-sym {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-areas:
    "data-top-l  data-top-r"
    "ds-row      ds-row"
    "data-bot-l  data-bot-r"
    "show-l      show-r";
  gap: 8px 12px;
}
.lke-bridge { display: none; }

/* 卡片基础样式: 两端染色由 .is-source / .is-target 控制 */
.lke-card {
  padding: 12px 14px;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: 6px;
  min-width: 0;
  display: flex; flex-direction: column;
}
/* 数据卡上下半 → 视觉连续 (相同侧的 top/bot 边框颜色相同, 中间数据源横穿) */
.lke-card-data-top.is-source { grid-area: data-top-l; border-bottom-left-radius: 0; border-bottom-right-radius: 0; }
.lke-card-data-top.is-target { grid-area: data-top-r; border-bottom-left-radius: 0; border-bottom-right-radius: 0; }
.lke-card-data-bot.is-source { grid-area: data-bot-l; border-top-left-radius: 0; border-top-right-radius: 0; border-top-style: dashed; }
.lke-card-data-bot.is-target { grid-area: data-bot-r; border-top-left-radius: 0; border-top-right-radius: 0; border-top-style: dashed; }
.lke-card-display.is-source  { grid-area: show-l; }
.lke-card-display.is-target  { grid-area: show-r; }

.lke-card.is-source { border-color: rgba(22, 93, 255, .35); }
.lke-card.is-target { border-color: rgba(255, 125, 0, .35); }

.lke-col-hd {
  display: flex; align-items: center; gap: 8px;
  padding-bottom: 10px;
  border-bottom: 2px solid var(--bl-divider);
  margin-bottom: 10px;
}
.lke-card.is-source .lke-col-hd { border-bottom-color: var(--bl-primary); }
.lke-card.is-target .lke-col-hd { border-bottom-color: #FF7D00; }

.lke-col-tag {
  padding: 2px 8px; border-radius: 4px;
  background: var(--bl-primary-soft); color: var(--bl-primary);
  font-size: 11px; font-weight: 600; flex-shrink: 0;
}
.lke-col-tag-r { background: var(--bl-warning-soft); color: var(--bl-warning); }
.lke-col-hd .bl-input { flex: 1; height: 30px; }
.lke-enabled { display: inline-flex; align-items: center; cursor: pointer; flex-shrink: 0; }
.lke-enabled input { display: none; }
.lke-en-track { width: 28px; height: 14px; border-radius: 8px; background: var(--bl-border-strong); position: relative; }
.lke-enabled input:checked + .lke-en-track { background: var(--bl-success); }
.lke-en-dot { position: absolute; left: 2px; top: 2px; width: 10px; height: 10px; border-radius: 50%; background: #fff; transition: left .15s; }
.lke-enabled input:checked + .lke-en-track .lke-en-dot { left: 16px; }

.lke-cell { padding: 8px 0; }
/* 占位行 (cardinality=one 时的复数显示名称): 整体灰化, 保持高度占位 */
.lke-cell.is-na { opacity: .55; }
.lke-cell.is-na .bl-input { background: var(--bl-bg-2); cursor: not-allowed; }
.lke-na-tip { font-size: 11px; margin-left: 4px; }

/* 键映射列头 (仅 is_data_source_rel 时显示对象属性 / 关联属性 双列) */
.lke-keys-hd {
  display: flex; align-items: center; gap: 4px;
  font-size: 12px; color: var(--bl-text-3);
  margin-bottom: 6px;
  padding: 0 2px;
}
.lke-keys-hd > span:not(.lke-key-seq) {
  flex: 1; min-width: 0;
}
.lke-cell-lbl { font-size: 12px; color: var(--bl-text-3); margin-bottom: 4px; display: flex; align-items: center; gap: 4px; }
.lke-input-wrap { position: relative; display: flex; align-items: center; }
.lke-input-wrap .bl-input { flex: 1; }
.lke-valid-tag {
  position: absolute; right: 8px; top: 50%; transform: translateY(-50%);
  font-size: 10px; background: var(--bl-success-soft); color: var(--bl-success);
  padding: 1px 6px; border-radius: 3px;
}

/* 基数 segmented control */
.lke-card-seg {
  display: flex; background: var(--bl-bg-2);
  border-radius: 4px; padding: 2px;
}
.lke-card-btn {
  flex: 1; padding: 6px 0; border: 0; background: transparent;
  cursor: pointer; font-size: 12.5px; color: var(--bl-text-2);
  border-radius: 3px;
}
.lke-card-btn:hover:not(:disabled) { color: var(--bl-text-1); }
.lke-card-btn.is-on { background: var(--bl-bg-1); color: var(--bl-primary); font-weight: 500;
  box-shadow: 0 1px 2px rgba(0,0,0,.08); }
.lke-card-btn:disabled { opacity: .55; cursor: not-allowed; }

/* 键配置 */
.lke-keys { display: flex; flex-direction: column; gap: 4px; }
.lke-key-row { display: flex; align-items: center; gap: 4px; }
.lke-key-seq {
  width: 22px; height: 22px; line-height: 22px;
  text-align: center; background: var(--bl-primary-soft); color: var(--bl-primary);
  font-size: 11px; font-weight: 600; border-radius: 50%; flex-shrink: 0;
}
.lke-key-row .bl-input { flex: 1; height: 28px; font-size: 12px; min-width: 0; }
.lke-key-row select.bl-input { padding-right: 22px; }

/* 提示行: ⓘ 描述 · + 添加 (inline) — 对齐图 2 */
.lke-key-tip {
  display: flex; align-items: center; gap: 4px;
  font-size: 11px; color: var(--bl-text-3);
  margin-top: 4px; padding: 0 2px;
}
.lke-key-tip > :first-child { display: inline-flex; }
.lke-add-link {
  display: inline-flex; align-items: center; gap: 2px;
  color: var(--bl-primary); cursor: pointer; user-select: none;
  margin-left: auto;
}
.lke-add-link:hover { text-decoration: underline; }


/* 中间数据源 (现位于 lke-sym 网格的 ds-row 区, 横跨左右两列, 切在基数与键之间) */
.lke-data-source {
  grid-area: ds-row;
  margin: 0;
  padding: 8px 12px;
  background: color-mix(in srgb, var(--bl-warning) 8%, var(--bl-bg-1));
  border: 1px dashed var(--bl-warning);
  border-radius: 6px;
  display: flex; align-items: center; gap: 10px;
}
.lke-ds-toggle {
  display: inline-flex; align-items: center; gap: 6px;
  cursor: pointer; font-size: 13px; color: var(--bl-text-1);
}

/* 中间表选择按钮 (代替原文本输入) */
.lke-table-btn {
  margin-left: 8px;
  display: inline-flex; align-items: center; gap: 4px;
  height: 30px; padding: 0 12px;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: 4px;
  font-size: 13px; color: var(--bl-text-1);
  cursor: pointer;
  flex: 1; max-width: 320px;
  transition: border-color .12s;
}
.lke-table-btn:hover:not(:disabled) { border-color: var(--bl-primary); }
.lke-table-btn:disabled { cursor: not-allowed; background: var(--bl-bg-2); color: var(--bl-text-3); }
.lke-table-btn.is-empty {
  border-style: dashed;
  color: var(--bl-text-3);
  font-weight: 500;
}
.lke-table-btn.is-empty:hover:not(:disabled) {
  color: var(--bl-primary); border-color: var(--bl-primary);
  background: var(--bl-primary-soft);
}
.lke-table-chev { margin-left: auto; color: var(--bl-text-3); display: inline-flex; }

/* 中间表选择面板 (与 NewObjectTypeWizard 选择主表同款样式) */
.lke-table-mask { z-index: 1200; }
.lke-table-modal { width: 520px; max-height: 78vh; display: flex; flex-direction: column; }
.lke-table-body { display: flex; flex-direction: column; gap: 8px; overflow: hidden; padding: 12px 16px; }
.lke-table-list { flex: 1; overflow: auto; display: flex; flex-direction: column; gap: 4px; }
.tbl-row {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 12px;
  background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: 4px;
  cursor: pointer;
  transition: border-color .12s, background-color .12s;
}
.tbl-row:hover { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.tbl-row.is-on { border-color: var(--bl-primary); background: var(--bl-primary-soft); }
.tbl-side {
  width: 3px; height: 16px; border-radius: 2px;
  background: var(--bl-primary);
  flex-shrink: 0;
}

/* 章节 */
.lke-section { margin-top: 16px; }
.lke-section-hd {
  display: flex; justify-content: space-between; align-items: center;
  padding-left: 8px; border-left: 3px solid var(--bl-primary);
  font-size: 12px; color: var(--bl-text-3); font-weight: 500;
  margin-bottom: 8px;
}

/* 类型类 (内嵌每侧展示卡末尾) */
.lke-tc-cell { margin-top: 8px; padding-top: 12px; border-top: 1px dashed var(--bl-divider); }
.lke-tc-hd { display: flex; align-items: center; justify-content: space-between; }
.lke-tc-list { display: flex; flex-direction: column; gap: 4px; }
.lke-tc-row {
  display: flex; align-items: center; gap: 4px;
  min-width: 0;
}
.lke-tc-row .bl-input { flex: 1; height: 28px; font-size: 12px; min-width: 0; }
.lke-tc-empty { font-size: 11px; padding: 2px 0; }

/* textarea */
.bl-textarea {
  width: 100%; min-height: 70px; padding: 8px 10px;
  border: 1px solid var(--bl-border); border-radius: 4px;
  background: var(--bl-bg-1); color: var(--bl-text-1);
  font-family: inherit; font-size: 13px;
  line-height: 1.55; resize: vertical;
}
.bl-textarea:focus { outline: none; border-color: var(--bl-primary); }
.bl-textarea:disabled { background: var(--bl-bg-2); color: var(--bl-text-2); }

/* 底部 */
.lke-ft {
  padding: 10px 14px;
  border-top: 1px solid var(--bl-divider);
  display: flex; align-items: center; gap: 8px;
  flex-shrink: 0;
}

</style>
