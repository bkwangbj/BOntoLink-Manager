# UI 协作速查卡(一页)

> 提需求前扫一眼。完整版见 [UI协作规范用语.md](UI协作规范用语.md)

## 🎯 三原则
1. **组件 + 部位 + 状态**(下拉菜单 · 选中项 · 悬停态)
2. **三段式**:现状 → 期望 → 触发条件
3. **给期望值**,不只说“不对”

## 🧩 常用组件(别叫错)
- 居中带蒙层阻断 = **模态弹框 modal**
- 边缘滑出 = **抽屉 drawer**
- 依附触发点不阻断 = **浮层 popover**
- 悬停轻提示 = **tooltip**
- 自动消失提示 = **Toast**
- 主操作+右侧下拉 = **分裂按钮 split button**
- 整块=**看板 dashboard**;单个=**图表 chart**

## 🔩 常用部位
指示箭头 **caret/arrow**(≠燕尾) · 触发器 **trigger** · 浮层 **popper** · 拖拽手柄 **handle** · 菜单项高度 **item height**(≠line-height) · 画布 **canvas** · 蒙层 **mask**

## 🖱 交互 / 定位
对齐:左/右对齐 start/end · **同宽 same-width** · 方向 bottom-start/bottom-end · 溢出 **翻转 flip / 贴边 shift**
时机:防抖 debounce · 回车 on enter · 失焦 on blur
尺寸:固定 fixed / **自适应铺满 fluid** / min-max-width
数据:拉取 fetch · 失效重取 invalidate · 持久化 persist · 脏检查 dirty
浮层被裁:挂 body **teleport**;被遮:提 **z-index**

## 🧭 状态
default / hover / active / focus / selected / disabled / readonly / loading / error / empty / dimmed

## ✍️ 四个万能句式
- **样式**:把〔组件·部位〕的〔属性〕从〔现状〕改成〔期望〕
- **交互**:〔点击/悬停〕〔组件〕时应〔期望〕,当前是〔现状〕
- **数据**:〔触发〕下应〔拉取/刷新/持久化〕,边界〔空/错误〕如何处理
- **定位**:〔浮层〕相对〔触发器〕〔对齐〕,宽度〔固定N/同宽/自适应〕,溢出〔翻转/贴边〕

## 🚫 别这么说 → 这么说
那个东西→组件名 · 不好看→现状+期望 · 大一点→具体数值/与X一致 · 弹的框→modal/drawer/popover · 行高→项高/line-height · 适配→自适应/响应式/溢出(指明) · 燕尾→caret
