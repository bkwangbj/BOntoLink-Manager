---
name: maker-add-chart
description: 给 bk-analysis-maker 新增一种图表类型的完整流程与踩坑清单(触点、rawEChart 类图主题/标签/tooltip 处理、验证步骤)。当需要往 maker 组件加图表、或排查新图表不显示/标签丢失/切主题异常时使用。
---

# maker 加图表 — 流程 + 闭坑清单

> 沉淀自 2026-07 批量新增 10 图(散点/漏斗/热力/箱线/桑基/矩形树/旭日/关系/主题河流/分段仪表盘)。
> 核心目录:`bk-analysis-maker-vue3/packages/bk-analysis-maker/`(下称 `<maker>`)。

---

## 一、两类图,先分清

| 类别 | 特征 | 例子 | 关键标记 |
|---|---|---|---|
| **直角系图** | x/y 轴 + series 按数据重建 | 散点/漏斗/热力/柱线 | 走标准 series 重建 |
| **rawEChart 图** | 整套 option 内置,结构特殊(层级/关系/单轴) | 桑基/树图/旭日/关系/主题河流/箱线/仪表盘 | `rawEChart:true`,多数 `noGrid:true` |

**判断**:能用「维度→x、指标→y」表达的 = 直角系;否则(层级 data / links / 单轴 / 内置固定结构)= rawEChart。
rawEChart 的坑几乎都来自「它被当普通柱图处理」。

---

## 二、触点清单(每个新图都要过一遍)

1. **图表模板** `<maker>/src/configs/chart-default-config.js`
   - Map 加 `['xxxChart', { ...echarts option }]`。
   - rawEChart 图:加 `rawEChart: true`;非直角系再加 `noGrid: true`(渲染时会剥离 xAxis/yAxis/grid/polar,防叠空网格)。
   - 语义固定色图(仪表盘红黄绿):加 `keepColor: true`(不被看板色板覆盖),但 series 仍从 `option.color[0..n]` 取色 → 用户可自定义。

2. **注册项** `<maker>/src/configs/chart-com.jsx`
   - 加 `chartComId`、`type:'BKBarChart'`、唯一 `branchType`、`items`、`dataSourceConfig`。rawEChart 图 `items:[]` + 空 data。

3. **⚠️ ECharts 注册(tree-shaking)** `frontend/src/views/workspace/maker-chart.vue`
   - 在 `import { ... } from 'echarts/charts'` 与 `use([...])` 里**加上新图表类型**(SankeyChart / GraphChart / TreemapChart / SunburstChart / BoxplotChart / ThemeRiverChart / FunnelChart …)及所需组件(SingleAxisComponent / VisualMapComponent / DataZoomComponent …)。
   - **漏这步 = 图形空白**(漏斗当初就栽在这)。

4. **渲染分发** `<maker>/src/bar-chart/src/main.vue` `customResetChart(config)`
   - rawEChart 分支:`noGrid` 剥离直角坐标 → 直接 `this.$refs.chart.setOption(option, true)`。
   - 需要按 `config.branchType` 微调的(箱线盒体色随色系、仪表盘 3 段刻度色、节点标签兜底补显)也在此分支。

5. **配置面板** `<maker>/src/bar-chart-config/src/main.vue`
   - `isRawEChart` 计算属性纳入新 branchType;`visibleMenu` 过滤掉「坐标轴/系列」菜单。
   - **ref 守卫**:隐藏「系列」后 `seriesConfigRef` 不存在 → `saveChartCfg` / `configOptionInit` 必须判空,否则抛错导致**保存/回显中断**。

6. **主题套用** `<maker>/src/configs/common-func.js`
   - rawEChart 图统一走共享 helper `applyRawEChartTheme(option, theme, type, branchType)`。
   - **两条路径都要短路**:`getDefaultOption`(创建时) + `getConfigTheme`(切主题时)。见第三节。

7. **实例数据绑定** `frontend/src/views/workspace/maker-instance.js`
   - `buildEmbedDefaultDataSource`:直角系图(散点/漏斗/热力)接真实数据;rawEChart 图 `return null`(用内置 demo)。
   - `applyDarkText`:深色看板文字/轴线适配(第 69 行会给 series.label 染色)。

8. **高级图名单** 3 处 ADV / ADVANCED_TYPES 数组要同步加 branchType:
   `<maker>/src/analysis-maker/src/main.vue`、`.../grid-layout-content/src/components/chart-content.vue`、`.../analysis-maker/src/components/toolbar/pannel.vue`。

9. **工具栏图标** `<maker>/src/analysis-maker/src/components/toolbar/images/xxx.svg`(320×180,蓝系 #4080FF/#6AA1FF/#0E42D2/#A9C9FF)。

---

## 三、rawEChart 图主题处理(今天最大的坑)

**根因链**:普通柱图靠 `BKBarChartThemeInit` 套主题(重建 series、展开 lineStyle)。rawEChart 图的 series 是桑基/树图/关系等,被当柱图重建后 **type 被覆盖、节点标签丢失**。

**两条独立路径,必须都短路**:
- `getDefaultOption` —— 图表**创建**时生成 configOption。
- `getConfigTheme` —— **切换深浅主题**时对每个图重套主题(经 `analysis-maker/src/main.vue` 的 `themeChange`)。

→ 只修 getDefaultOption,创建时正常,**一切主题标签就没了**。二者共用 `applyRawEChartTheme`,只套:配色(`option.color`,keepColor 除外)、坐标轴(轴文字/轴线/分隔线)、图例文字、**tooltip 配色**(`chartTheme.tooltip` 深底/浅底)、**series 节点标签色**(树图/旭日压色块→白字;桑基/关系白底→主题文字色),**跳过** BKBarChartThemeInit。

**节点标签兜底**:老看板保存的 configOption 是旧快照,不会重跑 getDefaultOption → 在 `customResetChart` 渲染时也强制补 `label.show/color`,保证新老图一致。

---

## 四、验证工作流(必跑,别跳)

```powershell
# 1. 重建 maker
cd c:\beiktech-jyx\BOntoLink02\bk-analysis-maker-vue3\packages\bk-analysis-maker
npx vite build

# 2. ⚠️ 清 Vite 预打包缓存(maker 是 file: 依赖,不清则 dist 改动不生效)
Remove-Item -Recurse -Force c:\beiktech-jyx\BOntoLink02\frontend\node_modules\.vite

# 3. dev server 用 --force 重启(用户跑;若已授权 Claude 管理则杀 5173 端口进程后重启)
#    npm run dev -- --force
```

浏览器**硬刷新**(Ctrl+Shift+R)后逐项验:
- [ ] 图形正常渲染(空白 → 查触点 3 tree-shaking 注册)
- [ ] 节点/数据标签可见(不可见 → 查第三节标签处理)
- [ ] 色系配置能改色且回显正确(不生效 → 查 keepColor / heatmap 用 visualMap 非 option.color)
- [ ] **来回切深浅主题**:标签/配色/tooltip 一直在(丢 → getConfigTheme 没短路)
- [ ] tooltip 深色深底浅字、浅色白底
- [ ] 配置面板保存/回显不报错(报错 → ref 守卫)

---

## 五、高频坑速查

| 现象 | 根因 | 修复 |
|---|---|---|
| maker 改了 dist 前端看不到 | Vite 把 file: 依赖预打包进 `.vite/deps` | 清 `.vite` + `--force` 重启 |
| 图形空白(形状都没有) | echarts 图表类型没在 `maker-chart.vue` `use([])` 注册 | 补注册图表类型 + 组件 |
| 切主题后节点标签消失 | `getConfigTheme` 把 rawEChart 当柱图跑 themeInit | getConfigTheme 也走 `applyRawEChartTheme` 短路 |
| 老图无标签、新图有 | 老看板用旧 configOption 快照 | `customResetChart` 渲染时兜底补 label |
| 节点标签死活不显示 | series 被 themeInit 重建 / label 无显式色 | 短路 themeInit + 显式 label.color(压色块用白字) |
| tooltip 深色不变白底 | rawEChart 跳过 themeInit 未套 tooltip | helper 里套 `chartTheme.tooltip` |
| 色系配置保存/回显崩 | 隐藏「系列」后 seriesConfigRef 为空仍被调用 | saveChartCfg/configOptionInit 判空守卫 |
| 热力图改色无效 | 热力用 visualMap 非 option.color | 从色系首色派生 visualMap 渐变 |
| 仪表盘想默认红黄绿又可改 | 被看板色板覆盖 | `keepColor:true` + 渲染时把 color[0..2] 映射到 3 段 |
| `getDefaultConfig` 报 series 相关 TDZ/undefined | rawEChart 短路未在 series 重建之前 | 短路必须在 getChartSeries/themeInit **之前** return |

---

## 六、rawEChart 图当前名单(改动时一并考虑)

`sankeyChart` `treemapChart` `sunburstChart` `graphChart` `themeRiverChart` `boxplotChart` `gradeGaugeChart`
(以上在 `isRawEChart`、`applyRawEChartTheme`、`buildEmbedDefaultDataSource` return null、ADV 数组中均需登记)
