# 人 × AI 协作前端 UI 规范用语(完整版)

> 通用规范,与具体项目无关。目标:把「模糊的整体印象」翻译成「组件 · 部位 · 属性 · 状态 · 数值 · 触发条件」六要素。
> 配套:[UI协作速查卡.md](UI协作速查卡.md)(一页速查) · [UI术语中英对照.md](UI术语中英对照.md)(中英词表)

---

## 0. 沟通三原则

1. **组件名 + 部位名 + 状态**:定位精确到「哪个组件的哪个部位在什么状态」。
   - ✅「下拉菜单的选中项在悬停态下背景太浅」
   - ❌「那个东西选中的时候不明显」
2. **需求三段式:现状 → 期望 → 触发条件**。
   - ✅「现状:切页签后列表没刷新;期望:切换即重新拉数据;触发:点第二个页签时」
3. **给期望值,不只给否定**:说清「应该怎样」,而不仅是「不对」。

---

## 1. 组件术语(Component Vocabulary)

### 1.1 容器与布局
页面 page · 区块/面板 panel/section · 卡片 card · 栅格 grid · 分栏/分割视图 split view · 侧边栏 sidebar · 抽屉 drawer(边缘滑出) · 模态弹框 modal/dialog(居中带蒙层、阻断背景) · 非模态浮层 popover · 气泡提示 tooltip · 折叠面板 accordion/collapse · 标签页 tabs

### 1.2 导航
面包屑 breadcrumb · 分页器 pagination · 步骤条 stepper · 锚点 anchor · 菜单 menu · 下拉菜单 dropdown

### 1.3 录入与表单
输入框 input · 文本域 textarea · 下拉选择 select · 可搜索下拉 combobox/filterable select · 多选 multi-select · 单选组/复选组 radio/checkbox group · 开关 switch · 分段控制器 segmented control · 步进器 number stepper · 滑块 slider · 日期时间选择器 date/time picker · 颜色选择器 color picker · 上传 uploader · 表单项 form field(标签 label + 控件 control + 校验提示 hint/error)

### 1.4 反馈与浮层
Toast(轻提示,自动消失) · 通知 notification(可停留/可关) · 确认框 confirm · 加载态 loading/spinner · 骨架屏 skeleton · 空状态 empty state · 进度条 progress · 徽标 badge · 标签 tag/chip

### 1.5 数据展示
表格 table · 列表 list · 树 tree · 时间轴 timeline · 描述列表 descriptions(键值对) · 统计卡 stat/KPI card · 图表 chart · 看板/仪表盘 dashboard
> 注意:看板 = 整块;图表 = 单个。别混用。

### 1.6 操作触发
按钮 button · 图标按钮 icon button · 分裂按钮 split button(主操作 + 右侧下拉更多) · 链接按钮 text/link button · 悬浮按钮 FAB · 按钮组 button group

---

## 2. 部位 / 解剖术语(Anatomy)

- 指示箭头/尖角 **arrow / caret**:气泡、下拉指向触发点的小三角(不要叫“燕尾”)
- 触发器 **trigger**:点开浮层的元素
- 浮层 **popper / overlay**:被弹出的层本身
- 拖拽手柄 **handle / grip**
- 可操作暗示 **affordance**
- 占位符 **placeholder**
- 前缀/后缀 **prefix / suffix / addon**
- 菜单项 **menu item**;项高/条目高度 item height(≠ 文字行高 line-height)
- 表头/表体/单元格 header / body / cell;吸顶表头 sticky header;冻结列 frozen/pinned column
- 蒙层/遮罩 **mask / backdrop / scrim**
- 图例/坐标轴/网格线 legend / axis / grid line(图表)
- 画布 **canvas**:设计器里可编辑的作图区

---

## 3. 状态术语(States)

默认 default · 悬停 hover · 按下 active/pressed · 聚焦 focus · 选中 selected/checked · 禁用 disabled · 只读 readonly · 加载中 loading · 错误 error · 校验通过 valid · 展开/收起 expanded/collapsed · 空 empty · 高亮 highlighted · 置灰/弱化 dimmed/muted

---

## 4. 交互与逻辑术语(Interaction & Logic)

### 4.1 触发与时机
点击 click · 悬停 hover · 聚焦 focus · 右键 context menu · 长按 long press · 拖拽 drag ｜ 即时 immediate · 防抖 debounce · 节流 throttle · 失焦 on blur · 回车 on enter · 挂载时 on mount

### 4.2 定位与对齐
锚定 anchored · 跟随光标 follow cursor · 固定 fixed ｜ 左/右对齐 align start/end · 居中 center · 同宽 same-width ｜ 放置方向 bottom-start / bottom-end / top / right ｜ 自动翻转 flip · 贴边收拢 shift

### 4.3 层级
z-index · 层叠上下文 stacking context · 传送到 body teleport/portal(浮层挂 body 避免被裁剪)

### 4.4 尺寸自适应
固定宽 fixed · 流式/自适应 fluid/adaptive(铺满容器) · min/max-width · 撑满 fill · 等比 fit/contain · 铺满裁剪 cover

### 4.5 校验与反馈
必填 required · 即时校验 inline validation · 提交时校验 on submit · 错误提示 error hint · 阻断提交 block submit

### 4.6 数据流与联动
拉取 fetch · 刷新 reload/refetch · 乐观更新 optimistic update · 缓存 cache · 失效 invalidate ｜ 主从联动 master-detail · 级联 cascade · 受控/非受控 controlled/uncontrolled ｜ 持久化 persist · 草稿 draft · 脏检查 dirty check

### 4.7 编辑器 / 设计器
设计模式 design mode · 预览模式 preview mode · 空白画布 blank canvas · 拖拽布局 drag layout · 自动保存 autosave · 另存为 save as

---

## 5. 布局 · 尺寸 · 对齐(视觉度量)

间距 gap · 内边距 padding · 外边距 margin · 圆角 radius · 描边 border/stroke · 阴影 shadow/elevation · 断点 breakpoint · 响应式 responsive · 密度 density · 基线对齐 baseline · 垂直居中 vertically centered

---

## 6. 需求表达模板

**A. 改样式**:把【组件·部位】的【属性】从【现状】改成【期望】,仅在【状态/场景】下生效。
**B. 改交互**:【触发方式】【组件】时,应【期望行为】;当前是【现状】。
**C. 改逻辑/数据**:【场景/触发】下,数据应【拉取/刷新/持久化/联动】;边界:【空/错误/并发】如何处理。
**D. 定位/尺寸**:【浮层】相对【触发器】【对齐方式】,宽度【固定 N/同宽/自适应】,溢出时【翻转/贴边】。

---

## 7. 反模式(易歧义 → 精准替代)

| ❌ 模糊 | ✅ 精准 |
|---|---|
| 那个东西 / 这里 | 组件名 + 部位名 |
| 不好看 / 不对 / 逻辑乱 | 现状 + 期望值 |
| 大一点 / 小一点 | 具体数值或“与 X 一致” |
| 弹出来的框 | 区分 modal / drawer / popover / tooltip |
| 行高(泛指) | 菜单项高度 / 行距 line-height / 表格行高 |
| 适配问题 | 自适应宽度 / 响应式断点 / 溢出处理(指明哪个) |
| 做成一致的 | 与【参照物】的【宽/高/字号/间距】一致 |
| 燕尾 / 小三角 | 指示箭头 caret / arrow |

---

**心法**:给得越结构化(组件·部位·属性·状态·数值·触发条件),返工越少。
