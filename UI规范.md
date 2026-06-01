# BOntoLink 系统 UI 与交互规范

> 版本: v1.0 · 适用于 BOntoLink02 动态本体管理系统
>
> 本规范沉淀自 2026-05 ~ 2026-06 期间多模块迭代实现的统一约定,作为后续新增模块、迭代既有模块的视觉与行为基准。

---

## 1. 设计基础

### 1.1 设计语言

- **简洁专业**:企业级数据字典 / 本体配置场景,克制使用色彩与装饰,信息密度优先
- **结构清晰**:三栏 / 四栏布局,职责分区明确,操作流自上而下 / 自左至右
- **视觉统一**:同语义元素跨模块必须同视觉(图标、配色、行高、按钮、徽章)

### 1.2 颜色 Token

| 用途 | 变量 | 16 进制 | 用法 |
|---|---|---|---|
| 主色 | `--bl-primary` | `#1677ff` | 链接、激活、主按钮 |
| 主色浅 | `--bl-primary-soft` | `#e6f4ff` | 选中行底色、tag 底色 |
| 成功 | `--bl-success` | `#00b42a` | 启用、通过 |
| 警告 | `--bl-warning` | `#FF7D00` | 实验、风险 |
| 错误 | `--bl-danger` | `#f53f3f` | 删除、失败 |
| 文本 1 | `--bl-text-1` | `#1d2129` | 正文标题 |
| 文本 2 | `--bl-text-2` | `#4e5969` | 正文内容 |
| 文本 3 | `--bl-text-3` | `#86909c` | 辅助说明 |
| 边框 | `--bl-border` | `#e5e6eb` | 卡片、输入框 |
| 分隔 | `--bl-divider` | `#e5e6eb` | 列表、tab 底线 |
| 背景 1 | `--bl-bg-1` | `#ffffff` | 内容卡片 |
| 背景 2 | `--bl-bg-2` | `#f7f8fa` | 主体外侧、表头、章节 |
| 悬停背景 | `--bl-bg-hover` | `#f2f3f5` | 鼠标 hover 行 |

### 1.3 语义色板(业务对象)

**属性类型** (`prop_type`):

| 类型 | 颜色 | Icon |
|---|---|---|
| data 数据属性 | `#1677ff` 蓝 | `database` |
| object 对象属性 | `#FF7D00` 橙 | `link` |
| annotation 注释属性 | `#00B42A` 绿 | `chat` |
| struct 结构属性 | `#722ED1` 紫 | `layers` |

**链接基数** (`cardinality`):

| 基数 | 颜色 | 文案 |
|---|---|---|
| `one:one` 一对一 | `#1677ff` 蓝 | 1:1 |
| `one:many` 一对多 | `#00B42A` 绿 | 1:* |
| `many:one` 多对一 | `#FF7D00` 橙 | *:1 |
| `many:many` 多对多 | `#722ED1` 紫 | *:* |

**枚举类型** (`enum_type`):

| 类型 | 颜色 | Icon |
|---|---|---|
| `general_single` 一级通用 | `#165DFF` 蓝 | `list` |
| `general_multi` 多级通用 | `#165DFF` 蓝 | `layers` |
| `biz_single` 业务一级 | `#FF7D00` 橙 | `list` |
| `biz_multi` 业务多级 | `#FF7D00` 橙 | `layers` |

### 1.4 字号 / 行高

| 用途 | 字号 | 行高 | 备注 |
|---|---|---|---|
| 页面标题 | 18px / 600 | 1.2 | PageHeader `h1` |
| 抽屉标题 | 14px / 600 | 1.4 | drawer 头部 |
| 章节标题 `.sec` | 12px / 500 | 1.4 | 灰色文字 + 蓝色 3px 左边条 |
| 正文 | 13px / 400 | 1.55 | 输入框、表单 |
| 表格内容 | 12px / 400 | 1.4 | 紧凑信息密度 |
| 辅助文字 | 11px | — | mono 编码、副标题 |
| 徽章 / 计数 | 11px / 500 | 17-18px | tnum 等宽数字 |

### 1.5 间距

- 内卡片内边距:`12-16px`
- 行高:表头 `34-36px`,数据行 `36-44px`(双行单元格 44px)
- 章节间距:`14-16px`(`.sec` 上方 margin)
- 表单字段:`8-10px` 垂直间距
- 按钮组:`gap: 6-8px`

### 1.6 圆角

| 元素 | 圆角 |
|---|---|
| 卡片 / 抽屉 / 主区 | `var(--bl-radius-3)` ≈ 8px |
| 输入框 / 按钮 / Tag | 4px |
| 头像图标(36×36) | 8px |
| 小图标块(22×22) | 4px |
| 徽章 / 计数胶囊 | 9-10px(完全圆角) |
| 单选/复选框 | 默认 |

---

## 2. 页面骨架(标准模块布局)

### 2.1 三栏式资源管理页

适用于:对象类型 / 链接 / 值类型 / 枚举类型 / 共享属性 / 接口 / 数据源 / 类型类

```
┌──────────────────────────────────────────────────┐
│ PageHeader (标题 + 副标题 + #actions 插槽)       │  ← 固定高度
├──────────────────────────────────────────────────┤
│ ┌──────────┐ ┌─────────────────────────────────┐ │
│ │ Category │ │ 列表 / 表格                      │ │
│ │ TreeFilter│ │  (sticky thead + 数据行)         │ │
│ │ (左 230px)│ │                                  │ │
│ │  可拖宽   │ │                                  │ │
│ │          │ ├─────────────────────────────────┤ │
│ │          │ │ 分页栏 / 批量操作 (钉底)          │ │
│ └──────────┘ └─────────────────────────────────┘ │
└──────────────────────────────────────────────────┘
       右侧抽屉 (drawer) → 点击行从右滑入,position: fixed
```

**HTML 骨架**:
```vue
<div class="page">
  <PageHeader title="..." subtitle="...">
    <template #actions>
      <div class="ov"> ...统计... </div>
      <select class="bl-input hd-filter"> ...筛选... </select>
      <div class="search-wrap"> ...搜索... </div>
      <button class="bl-btn bl-btn-primary"> + 新建XX </button>
    </template>
  </PageHeader>

  <div class="xx-main">
    <CategoryTreeFilter :rows="rows" title="行业分类"
                        total-label="全部XX" store-key="xxx"
                        @change="onCategoryChange" />
    <section class="pane pane-list">
      <div class="xx-list-scroll">
        <table class="bl-table xx-table"> ... </table>
      </div>
      <div class="xx-pager"> ...批量 / 分页... </div>
    </section>
  </div>

  <XxDetailDrawer v-model:open="drawerOpen" :data="selected" />
</div>
```

**CSS 骨架**:
```css
.page { display: flex; flex-direction: column; height: 100%; }
.xx-main { flex: 1; display: flex; gap: 12px; padding: 12px; overflow: hidden; }
.pane {
  flex: 1; background: var(--bl-bg-1);
  border: 1px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  display: flex; flex-direction: column; overflow: hidden;
}
.xx-list-scroll { flex: 1; min-height: 0; overflow: auto; }
.xx-pager {
  flex-shrink: 0; padding: 8px 12px;
  border-top: 1px solid var(--bl-divider);
  display: flex; justify-content: space-between; align-items: center;
  font-size: 12px;
}
```

### 2.2 PageHeader 顶部条

```vue
<PageHeader title="链接" subtitle="Links · 对象之间的连接定义">
  <template #actions>
    <!-- 统计概览 -->
    <div class="ov">
      <span class="ov-item">    <span class="ov-lbl">总数</span><b>{{ total }}</b></span>
      <span class="ov-item ov-ok">  <span class="ov-lbl">正常</span><b>{{ ok }}</b></span>
      <span class="ov-item ov-risk"><span class="ov-lbl">风险</span><b>{{ risk }}</b></span>
    </div>
    <!-- 视图切换 -->
    <div class="ds-view-toggle">
      <button :class="['vt-btn', !groupMode && 'is-on']" @click="groupMode=false">列表</button>
      <button :class="['vt-btn', groupMode && 'is-on']"  @click="groupMode=true">分组</button>
    </div>
    <!-- 筛选 -->
    <select class="bl-input hd-filter" v-model="filterDomain">...</select>
    <select class="bl-input hd-filter" v-model="filterStatus">...</select>
    <!-- 搜索 -->
    <div class="search-wrap">
      <span class="search-icon" v-html="BL.icon('search', 14)"></span>
      <input class="bl-input search-input" placeholder="..." v-model="q" />
    </div>
    <!-- 主操作 -->
    <button class="bl-btn bl-btn-primary"> + 新建 </button>
  </template>
</PageHeader>
```

| 类名 | 宽度 | 用途 |
|---|---|---|
| `.ov` | auto | 统计组容器 |
| `.hd-filter` | 130px | 单个筛选下拉 |
| `.search-wrap / .search-input` | 240-280px | 搜索框 + icon |

---

## 3. 核心组件库

### 3.1 CategoryTreeFilter `<CategoryTreeFilter>`

**用途**:6 个资源模块统一行业领域分类树。从 `categoryApi.tree()` 加载,按 `category_code` 过滤右侧列表。

**Props**:
```ts
{
  rows: Array,           // 上层数据列表 (用于计数)
  title: String,         // 标题, 默认 '行业分类'
  totalLabel: String,    // '全部' 文案
  storeKey: String,      // localStorage 键名前缀
}
```

**Emits**:
- `change({ categoryCode, codes, node })` — codes 是当前节点子树的 `category_code` Set,父组件直接 `.has(row.category_code)` 过滤

**特性**:
- 头部:标题 + 🔍 搜索 toggle 按钮
- 行高 30px,带计数胶囊
- 选中:蓝色背景 + 蓝色文字,胶囊变白底蓝字
- 拖拽手柄:右边缘 5px,180-360px 宽度限制
- 字段兼容:`r.category_code` (snake) 和 `r.categoryCode` (camel)

### 3.2 大模态弹框 `<ValueTypePickerModal> <EnumPickerModal> <SharedPropertyPickerModal>`

**用途**:从已存在的资源中选择若干项,如值类型 / 枚举 / 共享属性。

**通用结构**:
```
┌─────────────────────────────────────────┐
│ [📋] 标题 副标题            [□] [×]       │  ← 标题栏 (整条作拖动把手)
├─────────────────────────────────────────┤
│  ← 已选 chips (锁定 / 可选) + 搜索框 →    │  ← 上层
├──────────┬──────────────────────────────┤
│ 业务领域 │ 范围 · 共 N 项               │  ← 内容
│ (树)    │ ───────────────────────────  │
│         │ [✓ 待选列表]                  │
├─────────┴──────────────────────────────┤
│ 已选 N 项              [取消] [确定]    │  ← 底部
└─────────────────────────────────────────┘
   ↖ 八向缩放热区,可拖动,可最大化
```

**关键约定**:
- 居中模态 + 全局蒙层
- z-index: `1200`(高于所有详情抽屉 1010)
- 复用 `useDraggableModal` composable + `<DraggableHandles>` 组件
- 支持单选 (`multi: false`) / 多选 (`multi: true`)
- `excludeIds` 已使用项灰显示 + 锁定
- 关闭时调 `dm.reset()`,下次打开回到居中默认尺寸

**Composable**: `useDraggableModal({ minWidth, minHeight })`
- 返回 `{ state, startDrag, startResize, toggleMax, reset, modalStyle }`
- 状态机:`centered → free → maximized → free`(还原快照)

### 3.3 右侧详情抽屉

**统一头部** (适用于:共享属性详情 / 链接编辑器 / 类型类编辑 / 数据源 / 接口):

```vue
<header class="xx-hd"><!-- 56px 高 -->
  <div class="xx-hd-l">
    <!-- 36×36 大图标 (按 prop_type / cardinality / applicable_type 染色) -->
    <span class="xx-ic xx-ic-lg" :style="{ background: '...' }"></span>
    <div class="xx-title-wrap">
      <div class="xx-title">{{ title }}<span class="bl-mono bl-muted">- {{ code }}</span></div>
      <div class="xx-meta">
        <span class="bl-tag">状态</span>
        <span>更新于 ...</span>
      </div>
    </div>
  </div>
  <div class="xx-hd-r">
    <!-- 编辑模式 (胶囊按钮 图标+文字) -->
    <button :class="['bl-btn-text bl-btn-sm xx-edit-btn', editMode && 'is-edit-on']">
      <span v-html="BL.icon(editMode ? 'edit' : 'lock', 12)"></span>
      <span>{{ editMode ? '编辑' : '只读' }}</span>
    </button>
    <span class="xx-divider"></span><!-- 1px×18px 灰色竖线 -->
    <button class="bl-btn-text bl-btn-icon" @click="toggleMax"><!-- 最大化 --></button>
    <button class="bl-btn-text bl-btn-icon" @click="onClose"><!-- × 关闭 --></button>
  </div>
</header>
```

**抽屉骨架**:
```css
.xx-drawer {
  position: fixed; top: 0; right: 0; bottom: 0;
  background: var(--bl-bg-1);
  border-left: 1px solid var(--bl-border);
  box-shadow: -4px 0 16px rgba(0,0,0,.10);
  display: flex; flex-direction: column;
  min-width: 420px; max-width: 95vw;
  z-index: 1010;
}
.xx-drag-handle {
  position: absolute; left: -2px; top: 0; bottom: 0; width: 5px;
  cursor: col-resize; transition: background-color .15s; z-index: 6;
}
.xx-drag-handle:hover, .xx-drag-handle.is-resizing { background: var(--bl-primary); }
```

**统一规则**:
- **不带蒙层**(允许背景列表点击切换其他行)
- 左边缘 5px 拖拽手柄,hover/resizing 转蓝
- 宽度持久化到 `localStorage.bl.xx.width`
- 最大化:在默认宽 / `min(800/960, 90vw)` 之间切换
- 滑入过渡 `transform: translateX(20px)` + `.25s ease`
- 头部、底部 `flex-shrink: 0`,主体 `flex: 1; overflow: auto`

### 3.4 抽屉内 Tabs

```vue
<nav class="tabs"><!-- padding: 0 16px -->
  <button v-for="t in tabs" :key="t.k"
          :class="['tab', activeTab === t.k && 'is-active']" @click="activeTab = t.k">
    {{ t.label }}
    <span v-if="t.k === 'ref' && count > 0" class="tab-cnt">{{ count }}</span>
  </button>
</nav>
```

```css
.tabs { display: flex; padding: 0 16px; border-bottom: 1px solid var(--bl-divider); }
.tab {
  padding: 10px 14px; font-size: 13px;
  color: var(--bl-text-2); cursor: pointer;
  border-bottom: 2px solid transparent; margin-bottom: -1px;
}
.tab:hover { color: var(--bl-text-1); }
.tab.is-active { color: var(--bl-primary); border-color: var(--bl-primary); font-weight: 500; }
.tab-cnt { /* 数字气球 */
  padding: 0 6px; min-width: 16px; height: 16px;
  background: var(--bl-primary); color: #fff;
  border-radius: 8px; font-size: 11px; line-height: 16px;
}
```

### 3.5 章节标题 `.sec` 蓝色左条

```html
<div class="sec">基础信息</div>
```

```css
.sec {
  font-size: 12px; color: var(--bl-text-3);
  margin: 16px 0 8px;
  padding-left: 8px;
  border-left: 3px solid var(--bl-primary);
}
```

### 3.6 FieldRow 字段行

短字段 `inline`:label 左 / value 右
```vue
<FieldRow label="名称 *" inline>
  <input class="bl-input" v-model="form.name" />
</FieldRow>
```

长文本不带 `inline`:label 上 / value 下
```vue
<FieldRow label="RDFS 注释" hint="rdfs:comment · 业务规则说明">
  <textarea class="bl-textarea" rows="5" v-model="form.rdfs_comment"></textarea>
</FieldRow>
```

抽屉内 inline label 宽度统一 `78-90px`:
```css
.xx-drawer :deep(.fr.fr-inline .fr-label) { width: 86px; }
```

### 3.7 大文本域 `.bl-textarea`

```css
.bl-textarea {
  width: 100%; min-height: 96px;
  padding: 8px 10px;
  border: 1px solid var(--bl-border);
  border-radius: 4px;
  background: #fff;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.55;
  resize: vertical;
}
.bl-textarea:focus { outline: none; border-color: var(--bl-primary); }
.bl-textarea:disabled { background: var(--bl-bg-2); color: var(--bl-text-2); resize: none; }
```

注释 / 来源 / 元数据等"长文本"场景统一用 `.bl-textarea`,默认 `min-height: 96px`,可拖高。

---

## 4. 表格

### 4.1 标准表头

```css
.xx-table thead th {
  position: sticky; top: 0; z-index: 2;
  background: var(--bl-bg-2);
  box-shadow: inset 0 -1px 0 var(--bl-divider);
  font-weight: 600; font-size: 12px;
  height: 34-36px; padding: 0 8px;
  color: #333; white-space: nowrap;
}
```

**关键点**:
- `inset 0 -1px 0` 代替 `border-bottom`,避免 sticky 时 border 消失
- 表头默认居中 (`text-align: center`),`.t-left` 类强制左对齐

### 4.2 数据行

```css
.xx-table tbody tr { background: #fff; cursor: pointer; }
.xx-table tbody tr:hover { background: #f5f7fa; }
.xx-table tbody tr:nth-child(even) { background: #fafafa; }  /* 可选条纹 */
.xx-table tbody tr.is-selected { background: var(--bl-primary-soft) !important; }
.xx-table td { padding: 0 8px; font-size: 12px; height: 36px; vertical-align: middle; }
```

### 4.3 双行单元格(对象 / 属性 / 链接)

```vue
<td>
  <div class="xx-name-cell">
    <span class="xx-ic" :style="{ background: color }" v-html="BL.icon(icon, 12, '#fff')"></span>
    <div class="xx-name-text">
      <div class="xx-name bl-truncate" :title="rdfs_label">{{ rdfs_label }}</div>
      <div class="bl-mono bl-muted">{{ api_name }} · {{ prop_type }}</div>
    </div>
  </div>
</td>
```

```css
.xx-name-cell { display: flex; align-items: center; gap: 10px; min-width: 0; }
.xx-name-text { min-width: 0; }
.xx-name { font-weight: 500; color: var(--bl-text-1); }
.xx-name-text .bl-mono { font-size: 11px; color: var(--bl-text-3); }
.xx-ic {
  width: 22px; height: 22px; border-radius: 4px;
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
```

### 4.4 表头排序

```vue
<th>
  <span class="th-sort" @click="toggleSort('name')">
    名称<span class="th-arrow">{{ sortArrow('name') }}</span>
  </span>
</th>
```

```css
.th-sort { cursor: pointer; user-select: none; display: inline-flex; align-items: center; }
.th-sort:hover { color: var(--bl-primary); }
.th-arrow { color: var(--bl-text-4); font-size: 11px; margin-left: 2px; }
```

- 排序状态:`⇅`(未排序) / `↑`(asc) / `↓`(desc)
- 三态切换:无 → asc → desc → 无

### 4.5 锁定列 (sticky-left / sticky-right)

适用于横向超宽表格(对象类型属性列表、共享属性列表)。

```css
/* 左锁定 1-3 列 */
.stk-l1, .stk-l2, .stk-l3 { position: sticky; background: inherit; z-index: 2; }
.stk-l1 { left: 0; }
.stk-l2 { left: 40px; }
.stk-l3 { left: 210px; box-shadow: 2px 0 4px -2px rgba(0,0,0,0.1); }
/* 右锁定操作列 */
.stk-r1 { position: sticky; right: 0; background: inherit; z-index: 2;
          box-shadow: -2px 0 4px -2px rgba(0,0,0,0.1); }
/* 表头角落 z-index 高于普通表头 */
.thead th.stk-l1, .thead th.stk-r1 { z-index: 4; }
```

**前提**:
- 表格 `table-layout: fixed`(否则真实列宽与 `left:` 偏移错位 → 出现"空白列")
- 行 `background: inherit`,锁定列 `background: inherit` 跟随
- hover / selected 时锁定列需要 `background` 跟随父行

---

## 5. 批量操作栏(全局统一)

### 5.1 触发条件

底部分页栏左侧,当 `checked.size > 0` 时显示。

### 5.2 按钮顺序

```
[已选 N 项]  [🗑️ 批量删除]  [✓ 启用]  [⏻ 禁用]  [取消选择]
```

### 5.3 统一配色(outline 风格)

```css
/* 批量删除: 红 outline */
.xx-del-btn { background: #fff; border: 1px solid #f53f3f; color: #f53f3f; }
.xx-del-btn:hover { background: #fff1f0; }

/* 启用: 绿 outline */
.xx-ena-btn { background: #fff; border: 1px solid #00b42a; color: #00b42a; }
.xx-ena-btn:hover { background: #e8fff4; }

/* 禁用: 灰 outline */
.xx-dis-btn { background: #fff; border: 1px solid #86909c; color: #4e5969; }
.xx-dis-btn:hover { background: #f7f8fa; }

/* 取消选择: 纯文字 */
.xx-clear-btn { color: var(--bl-text-3); }
.xx-clear-btn:hover { color: var(--bl-primary); }
```

### 5.4 处理逻辑约定

- 启用 / 禁用按钮智能跳过已是目标状态的项:全部已是 → `BL.warning` 不发请求
- `BL.confirm` 二次确认
- 部分失败 → `BL.warning("成功 N 个,失败 M 个")`,全部成功 → `BL.success`
- 状态字段名:`status: 1/0`(常规) 或 `status: 'active'/'inactive'`(枚举)

---

## 6. BL.* 全局工具

### 6.1 BL.icon(name, size, color?)

返回 SVG 字符串,可通过 `v-html` 渲染。

**常用图标**:
- 基础:`plus / minus / edit / trash / check / x / copy / save / refresh`
- 导航:`chevronRight/Left/Up/Down / arrowUp/Down/Left/Right / back / forward`
- 文件:`folder / folderOpen / folderPlus / file / fileText / fileCode`
- 通信:`mail / phone / chat / send`
- 状态:`info / alert / success / error / help / lock / unlock / eye / eyeOff`
- 数据:`database / list / grid / cube / box / layers / network / link / share`
- 时间:`clock / calendar / timer / history`
- 编辑器工具:`cursor / move / zoomIn / zoomOut / maximize / minimize`
- 拖拽 / 排序:`grip`(6 点拖动手柄) / `moreV`(3 点垂直省略)
- 其他:`zap / sliders / search / power / archive / tag`

**调用示例**:
```vue
<span v-html="BL.icon('trash', 12)"></span>                <!-- 默认 currentColor -->
<span v-html="BL.icon('plus', 12, '#fff')"></span>         <!-- 白色 (在彩色按钮内) -->
<span v-html="BL.icon('edit', 13)"></span>                 <!-- 13px 用于工具栏 -->
```

**规则**:
- 默认 16px,工具栏 12-14px,头像 18px
- 在彩色按钮里强制白色 `'#fff'`,否则 currentColor 自动跟随文字颜色
- 不写死颜色 → 让 hover / active 状态的文字色自然传递

### 6.2 BL.success / BL.warning / BL.error / BL.info

```js
BL.success('已删除')
BL.warning('选中的属性均为未保存的新增项, 无法批量格式化')
BL.error('保存失败')
```

- 右上角弹 toast,2.5s 自动消失
- 多条会堆叠

### 6.3 BL.confirm({ title, content, danger, okText, cancelText })

```js
const ok = await BL.confirm({
  title: '删除属性',
  content: `确定删除「${name}」?`,
  danger: true,
  okText: '删除'
})
if (!ok) return
```

- 返回 Promise<boolean>
- `danger: true` 时确认按钮红色

### 6.4 BL.prompt({ title, label, placeholder, defaultValue, validate })

```js
const name = await BL.prompt({
  title: '新建分组',
  label: '分组名称',
  placeholder: '例如: 时间属性',
  validate: (v) => v.trim() ? true : '名称不能为空'
})
```

---

## 7. 按钮规范

### 7.1 按钮层级

```html
<button class="bl-btn bl-btn-primary">主按钮 (蓝填充)</button>
<button class="bl-btn">次按钮 (白底灰边)</button>
<button class="bl-btn bl-btn-text">文字按钮 (透明)</button>
<button class="bl-btn bl-btn-icon">图标按钮 (方形 28×28)</button>
<button class="bl-btn bl-btn-text bl-btn-icon">文字图标 (透明圆角)</button>
<button class="bl-btn bl-btn-danger">危险按钮 (红填充)</button>
<button class="bl-btn bl-btn-sm">小尺寸 (26px)</button>
```

### 7.2 图标 + 文字组合

```vue
<button class="bl-btn bl-btn-primary">
  <span v-html="BL.icon('plus', 12, '#fff')"></span>
  <span style="margin-left:4px">新建</span>
</button>
```

`margin-left: 4px` 是图标与文字的统一间距。

### 7.3 危险按钮 outline 变体

在批量操作 / 抽屉底部"删除"等场景,用 outline 红而非 filled `bl-btn-danger`,避免视觉过载:

```css
.xx-del-btn { background: #fff; border: 1px solid #f53f3f; color: #f53f3f; }
.xx-del-btn:hover { background: #fff1f0; }
```

---

## 8. Tag / 徽章

### 8.1 标签 `.bl-tag` 系列

```html
<span class="bl-tag">数据类型</span>
<span class="bl-tag bl-tag-primary">默认蓝</span>
<span class="bl-tag bl-tag-success">启用</span>
<span class="bl-tag bl-tag-warning">实验</span>
<span class="bl-tag bl-tag-danger">已废弃</span>
<span class="bl-tag bl-tag-muted">禁用</span>  <!-- 灰色 -->
```

`.bl-tag-muted` 自定义:
```css
:deep(.bl-tag-muted) { background: #f2f3f5; color: #86909c; }
```

### 8.2 计数徽章(tab / 行末)

```css
.xx-cnt {
  display: inline-block;
  margin-left: 6px; padding: 0 7px;
  min-width: 20px; height: 18px;
  background: var(--bl-bg-2); color: var(--bl-text-3);
  border-radius: 9px;
  font-size: 11px; font-weight: 500; line-height: 18px;
  text-align: center;
  font-feature-settings: "tnum";
}
.xx-cnt.is-active { background: var(--bl-primary); color: #fff; }
```

**重要**:计数为 0 时,**`v-if` 不渲染徽章**(避免空徽章渲染成纯色圆点)。

```vue
<span v-if="count > 0" class="xx-cnt">{{ count }}</span>
```

---

## 9. MiniSwitch 迷你开关

用于列表 / 表格内的布尔字段(必填 / 多值 / 启用 / 双向关联 等)。

```vue
<MiniSwitch :checked="!!row.is_required" disabled />
<MiniSwitch :checked="row.status === 1" @change="(v) => toggleStatus(row, v)" />
```

```css
.mini-sw {
  display: inline-block; width: 28px; height: 14px;
  border-radius: 8px; background: #c9cdd4;
  position: relative; cursor: pointer;
  transition: background-color .15s;
}
.mini-sw.is-on { background: var(--bl-primary); }
.mini-sw.is-disabled { opacity: .55; cursor: default; }
.mini-sw-dot {
  position: absolute; left: 2px; top: 2px;
  width: 10px; height: 10px; border-radius: 50%;
  background: #fff; transition: left .15s;
  box-shadow: 0 1px 2px rgba(0,0,0,.2);
}
.mini-sw.is-on .mini-sw-dot { left: 16px; }
```

---

## 10. 交互模式

### 10.1 行点击 → 抽屉

- 单击数据行 → 选中(蓝色背景) + 右侧抽屉滑入加载详情
- 抽屉常驻,可继续点击其他行,内容跟随刷新,无需先关闭
- 点击 × 或拖拽手柄外侧关闭

### 10.2 编辑模式 lock / unlock

抽屉头部 `🔒 只读 / ✏️ 编辑` 胶囊按钮:
- 默认只读,所有输入框 `disabled`,背景灰色
- 点击切换 → 全字段可编辑,按钮变蓝高亮
- 状态持久化到 localStorage:`bontolink.xx.editMode`

```js
const editMode = ref(localStorage.getItem('bontolink.xx.editMode') === '1')
watch(editMode, v => localStorage.setItem('bontolink.xx.editMode', v ? '1' : '0'))
```

### 10.3 拖拽排序(行重排)

```vue
<tr draggable="true"
    @dragstart="onDragStart(idx, $event)"
    @dragover.prevent="onDragOver(idx)"
    @dragend="onDragEnd">
  <td class="drag-cell" v-html="BL.icon('grip', 12)"></td>
  ...
</tr>
```

**规则**:
- 仅在 `selectMode === true && 无搜索 / 无筛选 / 无表头排序` 时启用
- 拖动结束持久化新顺序到后端
- 列表头明示"(搜索 / 筛选 / 排序时自动禁用拖拽)"

### 10.4 复制 RID / ID

```vue
<button :title="copied ? '已复制' : '复制 RID'" @click="copy(form.rid)">
  <span v-html="BL.icon(copied ? 'check' : 'copy', 12)"></span>
</button>
```

```js
async function copy(text) {
  await navigator.clipboard.writeText(text)
  copied.value = true
  setTimeout(() => copied.value = false, 2000)
}
```

复制后图标变 ✓,2 秒后恢复 📋。

### 10.5 模态层级

| z-index | 用途 |
|---|---|
| 6 | 抽屉内拖拽手柄(局部) |
| 999 | 抽屉蒙层(若有) |
| 1000 | 对象类型详情抽屉 / 数据源抽屉 |
| 1010 | 嵌套抽屉(在第一层之上) |
| 1011 | 嵌套抽屉的 resize 把手 |
| 1100-1200 | 大模态弹框 (ValueTypePickerModal / 等),必须高于所有抽屉 |
| 1300+ | toast / confirm / prompt |

**规则**:嵌套抽屉点击触发的弹框(如属性选择面板)**必须** z-index `≥ 1200`,否则会被外层抽屉遮挡。

---

## 11. 数据库与后端约定

### 11.1 表 / 字段命名

- 表名:`ont_xxx_yyy` (lowercase + 下划线)
- 主键:`id TEXT PRIMARY KEY` 格式 `"{module-prefix}-" + UUID`
  - `class-...` / `link-types-...` / `value-types-...` / `enum-types-...` / `shared-properties-...` / `struct-types-...` / `struct-items-...` / `type-class-...` / `property-format-...`
- 字段:snake_case (`category_code`, `prop_type`, `created_at`)
- 时间戳:`create_time / update_time TEXT NOT NULL DEFAULT (datetime('now','localtime'))`
- 状态:`status INTEGER NOT NULL DEFAULT 1` (1=启用 / 0=禁用) 或 `'active'/'inactive'/'deprecated'`
- 全局唯一资源标识:`rid TEXT` 格式 `"ri.ont.{module}.{code}"`

### 11.2 字段对照(snake ↔ camel)

MyBatis 配置 `mapUnderscoreToCamelCase: true`,Jackson 自动转 camelCase 输出。

**前端通用兼容**:
```js
const code = r.category_code || r.categoryCode || ''
```

⚠️ **Jackson 名字陷阱**:字段名 `gName` / `gSort`(单字母 + 大写)会被序列化为 **小写** `gname` / `gsort`。字段适配器需先取小写:
```js
const label = g.gname || g.gName || g.g_name
```

### 11.3 后端 Mapper / Controller 模式

**Mapper** (`@Mapper interface XxxMapper`):
- 用 `Map<String, Object>` 作为通用 row 类型,避免每张表写 Entity
- 注解 SQL `@Select / @Insert / @Update / @Delete`,需要动态 SQL 时用 `<script>` 包裹
- 列表 JOIN:子查询或 LEFT JOIN 拿出 label / count 等冗余字段供前端展示

**Controller** (`@RestController @RequestMapping("/api/xxx")`):
```java
@GetMapping public R<List<Map<String, Object>>> list() { return R.ok(mapper.listAll()); }
@GetMapping("/{id}") public R<Map<String, Object>> get(@PathVariable String id) { ... }
@PostMapping public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
  body.put("id", "xxx-" + UUID.randomUUID());
  body.putIfAbsent("status", 1);
  mapper.insert(body);
  return R.ok(mapper.findById(id));
}
@PutMapping("/{id}") public R<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
  body.put("id", id);
  mapper.update(body);
  return R.ok();
}
@DeleteMapping("/{id}") public R<?> delete(@PathVariable String id) { mapper.delete(id); return R.ok(); }
@PostMapping("/batch-delete") public R<Map<String, Object>> batchDelete(@RequestBody Map<String, Object> body) { ... }
```

**响应包装** `R<T>`:`{ code: 0, msg: 'ok', data: T }`;失败 `R.error(400, "...")`。

### 11.4 复合表关联约定

涉及"主体 + 子条目"模型(如结构属性、链接映射):

- **主表 + 条目表**(`ont_struct_types` + `ont_struct_items`,`ont_link_types` + `ont_link_mappings`)
- 详情 GET `/api/xxx/{id}` 返回 `{ ...main, items: [...], type_classes: [...] }` 一次性整包
- 更新 PUT 接受 `items` 字段,**整体覆盖**(删除旧条目 → 插入新条目),避免 diff 复杂度
- 条目主键 `mapping_id / struct_items_id` 用 UUID,无业务含义

### 11.5 分组关联统一表

所有资源模块的"业务分组"统一走 `ont_biz_group_class` 表,通过 `group_type` 区分:
- `object_types / link_types / action_types / value_types / shared_props / functions / interface / datasources / enum_types`

字段:`id / group_id / ref_id / group_type / category_code / g_sort`

---

## 12. 前端 API 客户端

### 12.1 命名

`xxxApi` 导出在 `frontend/src/api/index.js`,与后端模块 1:1 对齐。

### 12.2 标准方法集

```js
export const xxxApi = {
  list:        () => http.get('/xxx'),
  get:         (id) => http.get(`/xxx/${id}`),
  create:      (data) => http.post('/xxx', data),
  update:      (id, data) => http.put(`/xxx/${id}`, data),
  remove:      (id) => http.delete(`/xxx/${id}`),
  batchRemove: (ids) => http.post('/xxx/batch-delete', { ids }),
}
```

复杂模块附加:
- `stats() / overview()` 统计
- `references(id)` 引用查询
- `setStatus(id, status)` 状态切换
- 子资源 endpoint:`{id}/items`, `{id}/mappings`, `{id}/references`

### 12.3 HTTP 拦截器

- 自动加 `/api` 前缀(`baseURL`)
- 自动解包 `R<T>`:成功返回 `data`,失败抛 `{ code, msg }`
- 401 跳登录(尚未启用)

---

## 13. 文件结构

```
backend/
├── src/main/java/com/beiktech/bontolink/
│   ├── common/         R.java (响应包装)
│   ├── entity/         (按需,大多用 Map)
│   ├── mapper/         XxxMapper.java
│   ├── service/        XxxService.java (可选)
│   └── controller/     XxxController.java
└── src/main/resources/db/
    ├── schema-sqlite.sql
    └── data.sql

frontend/src/
├── api/
│   └── index.js                          所有 API 集中导出
├── components/                           跨模块共用
│   ├── CategoryTreeFilter.vue            统一行业分类树
│   ├── DraggableHandles.vue              8 向缩放手柄
│   ├── PageHeader.vue                    页面顶栏
│   ├── PropertyFormatModal.vue           属性格式化弹框
│   ├── ValueTypePickerModal.vue          值类型选择面板
│   ├── EnumPickerModal.vue               枚举选择面板
│   └── SharedPropertyPickerModal.vue     共享属性选择面板
├── lib/
│   ├── bl.js                             BL.* 工具 + ICONS 目录
│   └── useDraggableModal.js              大弹框拖动 composable
├── views/
│   ├── resources/                        资源类模块
│   │   ├── ObjectTypes.vue
│   │   ├── LinkTypes.vue
│   │   ├── SharedProperties.vue
│   │   ├── Interfaces.vue
│   │   ├── Datasources.vue
│   │   ├── linktype/LinkTypeEditor.vue
│   │   └── sharedproperty/{Drawer, Wizard, StructTypesView, MiniSwitch}.vue
│   └── config/                           配置类模块
│       ├── Category.vue
│       ├── ValueTypes.vue
│       ├── EnumTypes.vue
│       ├── TypeClasses.vue
│       └── Security.vue
└── router/index.js
```

---

## 14. 命名约定

### 14.1 CSS 类前缀

每个模块用 2-3 字母前缀,避免全局类冲突:

| 模块 | 前缀 | 示例 |
|---|---|---|
| ObjectTypes | `ot-` | `.ot-pager / .ot-tab-content` |
| LinkTypes | `lk-` / `lke-` | `.lk-row / .lke-drawer` |
| ValueTypes | `vt-` | `.vt-table / .vt-drawer` |
| EnumTypes | `et-` | `.et-tree / .et-drawer` |
| SharedProperties | `sp-` | `.sp-stab / .sp-list-scroll` |
| Interfaces | `if-` | `.if-detail` |
| Datasources | `ds-` | `.ds-main / .ds-drawer` |
| TypeClasses | `tc-` | `.tc-tree / .tc-drawer` |
| Category | `cat-` | (TBD) |
| 大弹框 | `vtp- / spp- / ep-` | (Picker modals) |

### 14.2 Vue 组件命名

- Page 组件:`Xxx.vue`(`ObjectTypes / LinkTypes / TypeClasses`)
- 抽屉:`XxxDetailDrawer.vue / XxxEditor.vue`
- 向导:`NewXxxWizard.vue`
- Tab 内容:`Tab{XxxName}.vue`(`TabProps / TabOverview / TabPropsCanvas`)
- 选择器:`XxxPickerModal.vue`(`ValueTypePickerModal / EnumPickerModal`)

### 14.3 state 命名

- 弹窗 / 抽屉 开关:`xxxOpen: ref(false)`
- 编辑模式:`editMode: ref(...)`
- 选中:`selectedId: ref(null) / selected: ref(null)`
- 批选:`checked: ref(new Set())`
- 表单:`form: reactive({...})`
- 加载列表:`rows: ref([]) / list: ref([])`
- 当前激活子页签:`activeTab / activeMainTab`

---

## 15. 性能 / 可访问性

### 15.1 大列表分页

数据量 > 50 强制分页:每页 20 / 50 / 100 三档,`select.xx-page-size width: 64px`。

### 15.2 V-show vs V-if

- 频繁切换的页签内容:`v-show`(保留 DOM,避免重渲染)
- 一次性弹框 / 抽屉:`v-if`(Teleport + 过渡)

### 15.3 Sticky table 边界条件

- 表格容器必须 `overflow: auto` 且有定高(`flex: 1; min-height: 0`)
- 否则 sticky 不生效或 z-index 错乱

### 15.4 字段长内容

- 长名称:`bl-truncate` + `title=` 悬浮显示完整内容
- 长说明:`-webkit-line-clamp: 2` 两行截断 + `title=`

```css
.bl-truncate {
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
```

---

## 16. 已建立的模块对照表

| 模块 | 路径 | 主要文件 | 数据表 |
|---|---|---|---|
| 对象类型 | /resources/object-types | ObjectTypes.vue + objecttype/* | ont_class + ont_class_property + ont_class_link |
| 链接 | /resources/link-types | LinkTypes.vue + linktype/LinkTypeEditor.vue | **ont_link_types + ont_link_mappings** |
| 值类型 | /resources/value-types | ValueTypes.vue | ont_value_types + ont_valuetypes_usage_config |
| 枚举类型 | /resources/enum-types | EnumTypes.vue | ont_enum_types + ont_enum_items + ont_enum_level_code_rule |
| 共享属性 | /resources/shared-props | SharedProperties.vue + sharedproperty/* | ont_shared_properties + ont_struct_types + ont_struct_items |
| 接口 | /resources/interfaces | Interfaces.vue | ont_interface + ont_interface_property + ont_interface_class |
| 数据源 | /resources/datasources | Datasources.vue | sys_data_source |
| 行业分类 | /config/category | Category.vue | ont_biz_category + ont_biz_namespace |
| 类型类 | /config/type-classes | TypeClasses.vue | **ont_type_class** |
| 共用分组 | (内部) | LeftGroupTree.vue(legacy) | ont_biz_group + ont_biz_group_class |
| 属性格式化 | (子组件) | PropertyFormatModal.vue | ont_property_format |
| 图标库 | (子组件) | IconPickerField.vue | ont_icon_lib_group + ont_icon_lib_icon |

---

## 17. 常见踩坑清单

| 现象 | 根因 | 修复 |
|---|---|---|
| sticky 表头横滚时被覆盖 | 角落 sticky 列 z-index 不够 | 角落格 `z-index: 4`(高于普通表头 3) |
| 表格列出现空白列 | `table-layout: auto` 实际列宽与 sticky `left:` 偏移错位 | `table-layout: fixed` 或干脆移除 sticky |
| 抽屉里弹模态被遮挡 | 模态 z-index ≤ 抽屉 | 大弹框统一 z-index ≥ 1200 |
| 全屏 dist 模式抽屉不全高 | `position: absolute` 被祖先 clip | `position: fixed` |
| 高度链 `height: 100%` 计算为 0 | 父级用了 `min-height: 100%` 而非 `height` | 父级 `display: flex` 让 flex 拉伸 |
| 计数徽章渲染成纯色圆点 | 内容为 0/undefined 但 CSS 强制 `min-width / height` | `v-if="count > 0"` 不渲染徽章 |
| Vue refresh 后旧路由仍指向 PlaceholderList | router 组件级 lazy import 替换 HMR 不灵 | 浏览器硬刷新(Ctrl+Shift+R) |
| `watch immediate` 抛 `Cannot access X before initialization` | 引用了下方 const 声明的 ref | 把被引用的 ref 提前到 watch 之前声明 |
| `BL.icon('unknown')` 渲染成六边形 | ICONS 字典里没找到 → fallback 到 cube | 在 `bl.js ICONS` 表里新增对应 path |
| 拖拽 / drag handle 渲染成奇怪图形 | 同上,使用 `grip` 但 grip 未注册 | 已在 bl.js 注册 `grip: 6 点拖动手柄` |
| Datasources `r.categoryCode` 与其它 `r.category_code` 不一致 | Jackson 配置对该模块输出 camelCase | 组件内字段适配 `r.category_code || r.categoryCode` |
| 改 router 后 sidebar 高亮错误 | sidebar 用路径精确匹配 | 改 sidebar 配置和 router meta 同步 |
| 模板里 `@event="(n) => ref = n"` 不生效 | `ref` 是 const,模板无法重新指向 | 改用方法 `function setX(n) { x.value = n }` |

---

## 18. 新增模块 Checklist

新增一个资源类模块时,逐项核对:

**数据层**
- [ ] 表名 `ont_xxx_yyy`,主键 `id` 用 `"prefix-"+UUID` 格式
- [ ] 时间字段 `create_time/update_time TEXT DEFAULT (datetime('now','localtime'))`
- [ ] 索引:`category_code` / `status` / 业务唯一字段
- [ ] 在 `data.sql` 写 4-12 条种子数据,覆盖主要业务场景

**后端**
- [ ] `XxxMapper.java` — list/get/insert/update/delete + 必要的子查询统计
- [ ] `XxxController.java` — REST `/api/xxx`,标准 CRUD + `batch-delete`
- [ ] 主键唯一性校验:`existsByCode` + `R.error(400, "...")` 拒绝重复

**前端 API**
- [ ] `xxxApi` in `api/index.js`,7 个标准方法

**前端页面**
- [ ] `Xxx.vue` 使用三栏式布局,`PageHeader` + `CategoryTreeFilter` + `pane-list`
- [ ] 顶部:统计 + 视图切换(可选)+ 筛选 + 搜索 + 新建按钮
- [ ] 中间:表格 + sticky thead + 分页栏
- [ ] 批量栏:🗑️ 删除 / ✓ 启用 / ⏻ 禁用 / 取消选择(outline 配色)
- [ ] 详情抽屉:36×36 大图标 + 标题 + 编辑模式开关 + 最大化 + 关闭

**路由 / 导航**
- [ ] `router/index.js` 添加路由
- [ ] `AppSidebar.vue` 添加导航项

**联调**
- [ ] 后端 `mvn clean compile` 通过
- [ ] 前端 `vite build` 通过
- [ ] 删 `bontolink.db` 重启后端,curl 验证 API
- [ ] 浏览器硬刷新,测试 CRUD 全流程

---

## 19. 文档版本记录

| 版本 | 日期 | 内容 |
|---|---|---|
| v1.0 | 2026-06-01 | 初版整理,覆盖 10 个主要模块的视觉与交互规范 |

---

**附录:模块图标 + 配色对照**

| 资源 | 图标 | 配色 |
|---|---|---|
| 对象类型 | `cube` | `#1677ff` 蓝 |
| 链接 | `link` | 按基数 |
| 值类型 | `list` | `#165DFF` 蓝 |
| 枚举类型 | `layers / list` | 按 enum_type |
| 共享属性 | `database / link / chat / layers` | 按 prop_type |
| 接口 | `network / station` | 自定义 |
| 数据源 | `database` | 按 dsType |
| 类型类 | `tag` | 按 applicable_type |
| 行业分类 | `industry / folder` | 自定义 |

> 本规范为活文档,新增模块如需扩展约定,请在 PR 中同步更新本文件。
