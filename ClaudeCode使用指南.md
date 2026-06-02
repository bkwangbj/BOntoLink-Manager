# Claude Code 编程开发实战指南

> 沉淀自 BOntoLink02 动态本体管理系统项目(Spring Boot + Vue 3)的协作实践
>
> 版本: v1.0 · 2026-06-01

---

## 引子

Claude Code 是一个"懂代码、能动手、看得到上下文"的 AI 协作者。它不只是补全或问答工具,而是可以承担**真实工程任务**的搭档:读项目、改代码、跑命令、验证结果、调用 API。

但要把它用好,需要双方都掌握一些方法。**沟通方式、任务粒度、上下文管理、验证习惯**比"提示词模板"重要得多。

本指南来自一个真实的多模块系统(后端 51 个 Java 文件 + 前端 50+ 个 Vue 文件 + 100+ 张数据表种子)的协作过程。下面的每条建议都经历过"撞墙 → 修正 → 验证"的迭代。

---

## 一、心法:把 Claude 当"高级实习生"

很多人把 AI 当工具用,问一句答一句。但更高效的模式是把它当成一个**有经验但缺乏项目内幕的实习生**:

- **它会主动查看代码**,而不是凭空猜测
- **它会犯错**,但能从错误中学习上下文
- **它需要明确的目标**,但不需要手把手指令
- **它需要你做最后一公里的把关**

这个心智模型决定了你们之间该怎么对话。

### 1.1 给目标,不给步骤

❌ **不好**:
> 先读 ObjectTypes.vue,然后改 245 行,把 ot-row 改成 lk-row,再去样式那块加 .lk-row { cursor: pointer }

✅ **好**:
> 把链接类型的行做成可点击,点击后右侧滑出详情抽屉,参考对象类型页面的交互

后者节省 80% 沟通成本,Claude 会自己:
1. 读 ObjectTypes 找参考实现
2. 看 LinkTypes 当前结构
3. 应用同样的模式
4. 提示你哪里有偏差

### 1.2 给约束,而非细节

❌ **不好**:
> 把按钮颜色设为 #00b42a,边框 1px,字体 13px,padding 0 10px,圆角 4px

✅ **好**:
> 启用按钮用绿色 outline 风格,与系统其他模块的批量操作栏一致

后者让 Claude 自己去查 Datasources/SharedProperties 现有 outline 配色,保证全局一致。**风格一致性是约束,不是参数。**

### 1.3 用反馈而非重写

❌ **不好**:删除 Claude 写的代码自己重写
✅ **好**:截图标红 + 一句话说"这里不对" → 让 Claude 改

我们 80% 的迭代都是这种模式:用户截图框出问题区域 + 几个字描述(比如"层级不对,正当弹框")。Claude 接过去判断、修复、解释根因。

---

## 二、沟通技巧

### 2.1 截图 + 标注 + 短描述

用户最高效的输入格式:

```
[贴一张截图,Annotated 标红/圈出问题]
"间距优化"
```

或

```
[贴两张截图,一张当前,一张目标]
"参考图2,启用按钮改绿色"
```

我们整个项目这种交互大约用了 60-70 次,每次都直击要点。**比起 200 字的文字描述,一张截图配 10 个字更高效。**

### 2.2 PDF 规范

如果有产品文档(Figma 设计稿、需求 PDF),直接贴 → Claude 会读图、读表、按规范实施。

实战案例:
- **类属性表 ont_class_property 字段 prop_type 增加 struct** → Claude 自动定位文件、改 schema 注释、补 seed data
- **6.1链接类型.pdf** 长达 27 页的规范文档 → Claude 一次性产出 3 张表 + 24 条种子 + 完整 CRUD + 编辑器 UI

技巧:**先贴规范,再说"按规范实施"**,Claude 会做整体设计,而不是拼凑碎片需求。

### 2.3 一次说清楚"我想要什么"

每条指令最好包含三个要素:

1. **对象**:改什么(模块/文件/区域)
2. **变化**:从什么到什么
3. **约束**:对齐谁 / 保持谁不变

例:
> "对象类别这里(对象)的启用按钮 改为白底+绿边+绿字(变化),与共享属性的统一(约束)"

### 2.4 避免"模糊形容词"

❌ "让它好看一点"
❌ "差不多就行"
✅ "对齐 SharedProperty 抽屉的视觉"
✅ "与图2一致"

具体的对标对象比"好看"更可执行。

### 2.5 信任 Claude 的判断

如果 Claude 提出"我建议先做 A,因为 B",大多数时候这是合理的。它能看到比你以为的更多上下文(整个项目结构、依赖关系、潜在冲突)。

但也别盲信:**它可能错,你需要做最后的把关。**

---

## 三、工程化协作模式

### 3.1 让 Claude 先"踩点"

新模块开工前,先让 Claude 看现有项目:

> "survey 项目里所有有 LeftGroupTree 的模块,看看它们如何接入"

Claude 会用 Grep/Glob 快速扫描,给你一份用法清单。然后你才开始下指令"在 X 模块也接入"。

**踩点 5 分钟,省后续 50 分钟。**

### 3.2 复用既有模式,而非重新发明

我们的项目里:
- 6 个模块都用了 `CategoryTreeFilter` 行业分类树 → 第 1 次写组件,后 5 次都是 import + 接线,各 < 5 分钟
- 3 个大模态弹框都用了 `useDraggableModal` 复用拖动/最大化/缩放 → 一处升级,全局受益
- 6 个模块的批量操作栏都用了相同 outline 配色 → 视觉立刻一致

**规则**:看到第二次写类似代码,就要抽组件 / composable / 公共样式。第三次还在重复,就是工程债务。

### 3.3 字段适配器模式

遇到后端字段名不一致(snake / camel / Jackson 怪规则)时,**写适配器,不要让前端到处分支判断**:

```js
const rowCategoryCode = (r) => r.category_code || r.categoryCode || ''
const gLabel = (g) => g.gname || g.gName || g.g_name
```

封装一次,组件树到处用。

### 3.4 不要逐行 commit / 频繁 build

我们的实践:
- 5-10 条修改 → 1 次 build verify
- 用户验收满意 → 一次性 commit

**频繁 build 浪费时间,频繁 commit 让 history 混乱。** Claude 编译失败会立刻告诉你,不需要每改一行都跑一次。

### 3.5 让 Claude 写"自验证脚本"

新接口写完不要立刻交付,而是:

```
$ curl /api/xxx | head -c 500
```

Claude 会自己 curl 验证、看返回 JSON 是否符合预期。比"我猜应该行"靠谱 100 倍。

实战:类型类 API 写完,Claude 直接:
1. 启动后端 (background)
2. 等 /health 通 (until-loop)
3. curl /api/type-classes
4. 在 chat 里贴出真实返回的 JSON

整套自动化,你只用看最后一句"verified ✓"。

### 3.6 持久化"踩坑清单"

每修一个 bug,在文档里记一笔:

```markdown
| 现象 | 根因 | 修复 |
|---|---|---|
| sticky 表头被覆盖 | 角落 sticky 列 z-index 不够 | 角落格 z-index: 4 |
```

我们的 [UI规范.md](UI规范.md) 第 17 节就是这么积累起来的 12 条经验,下次遇到同样症状,5 秒定位。

---

## 四、任务编排

### 4.1 TodoWrite 是计划工具,不是日志

**正确用法**:开工前列计划

```js
todos: [
  { content: "Schema: 3 张新表", status: "in_progress" },
  { content: "Backend Mapper + Controller", status: "pending" },
  { content: "Frontend API", status: "pending" },
  { content: "UI 主列表", status: "pending" },
  { content: "Build + smoke test", status: "pending" }
]
```

**好处**:
- 用户能看到整体路径,知道现在在第几步
- Claude 自己不容易跑偏
- 大任务被切成可验证的小段

**错误用法**:任务完成后才写一条流水账。这是日志,没有规划价值。

### 4.2 大任务拆 5-9 个 todo

低于 5 个:任务还太粗,Claude 会自己分,但你看不到中间节点
高于 9 个:粒度太细,频繁切状态浪费 token

理想区间:**5-9 个 todo**,每个对应 5-20 分钟的工作量。

### 4.3 多步骤先排顺序

复杂任务的"自然顺序":

1. **Schema 先于 Code**(数据库表先定义,Mapper 才能写)
2. **后端先于前端**(API 先 ready,前端才能调)
3. **核心后于壳**(先建文件骨架,再填核心逻辑,最后填样式)
4. **验证先于交付**(build + smoke test 必须最后一步)

我们这次链接类型 + 类型类两个模块,就是按这个顺序走的:
schema → seed → mapper → controller → frontend api → main page → editor → routes/nav → build verify

### 4.4 并行 vs 串行

**可并行**(同时调多个工具):
- 读多个文件检查现状
- 同时 Grep 多种关键词
- 后端启动期间继续准备前端代码

**必须串行**(有依赖):
- Schema 改完才能跑 Mapper
- 前一步 build 失败,下一步不要执行
- 用户没确认方向前,不要 commit 或推送

Claude 会自己判断,但你也可以明确说:"启动后端的同时,开始改前端代码"。

---

## 五、工具使用

### 5.1 文件读写

| 工具 | 用于 |
|---|---|
| `Read` | 看文件内容,**编辑前必须读** |
| `Write` | 新建文件 或 完全重写已读过的文件 |
| `Edit` | 局部修改(`old_string` → `new_string`) |
| `Glob` | 找文件:`frontend/src/**/*.vue` |
| `Grep` | 找内容:正则 + 多种 output_mode |

### 5.2 搜索心法

**Grep 优于 ls + cat**:
```bash
# ❌
ls src/components/
cat src/components/Header.vue

# ✅
Grep pattern: "function .*Header" path: "src/components" output_mode: "content"
```

`Grep` 内置 ripgrep,几百毫秒扫整个项目。

**Glob 优于 find**:
```bash
# ❌
find . -name "*.vue" -path "*/resources/*"

# ✅
Glob pattern: "frontend/src/views/resources/**/*.vue"
```

### 5.3 执行命令

| 工具 | 场景 |
|---|---|
| `Bash` | POSIX 脚本、curl、git、文件操作 |
| `PowerShell` | Windows 原生(maven、IIS、注册表) |
| `run_in_background: true` | 长时间运行(启动后端、build watch) |

**关键诀窍**:**绝不在前台 sleep**。等服务起来用 `until curl -sf URL; do sleep 2; done` 在后台跑,Claude 收到完成通知再继续。

### 5.4 文件编辑的"old_string 陷阱"

`Edit` 要求 `old_string` 精确匹配(含缩进、换行)。

**坑**:Read 输出带行号前缀(`123→...`),复制时容易把行号也带进去。

**修复**:`old_string` 不要包含行号前缀,只要内容本身。

**陷阱 2**:同样的字符串文件里出现多次 → Edit 会拒绝。

**修复**:
- 加上前后文一起匹配(扩大 `old_string` 范围)
- 或用 `replace_all: true`(确认所有匹配都该改)

### 5.5 让 Claude 看错误,而非猜错误

页面报错时,直接给它**真实的报错文本/截图**:

```
[控制台截图]
"Uncaught (in promise) ReferenceError: Cannot access 'refs' before initialization"
```

Claude 立即定位到 TDZ 问题,而不是"我猜可能是..."。

---

## 六、验证与调试

### 6.1 build verify 是底线

每次大改之后:

```bash
cd frontend && npx vite build 2>&1 | grep -E "built in|error"
```

输出 `built in 6.5s` 就行,失败会显示 error 位置。

### 6.2 后端验证流程

```bash
# 1. 杀掉旧进程
Get-Process java | Stop-Process -Force

# 2. 删 DB 强制重新初始化 schema/data
rm bontolink.db

# 3. 后台启动
mvn spring-boot:run (run_in_background)

# 4. 等启动
until curl -sf /api/health; do sleep 2; done

# 5. smoke test 新 API
curl /api/xxx | head -c 500
```

整个流程 30-60 秒。

### 6.3 浏览器问题:让用户帮忙

页面空白、样式错位、交互异常时,Claude 看不到浏览器状态,只能猜。

**最高效**:让用户**打开 F12 → Console → 截图给 Claude**。一条真实报错文本胜过 10 条猜测。

我们这次"页面空白"问题,就是用户截图给了 `Cannot access 'refs' before initialization`,30 秒定位 TDZ。

### 6.4 IDE diagnostics

Claude Code 在 VSCode 里能看到编辑器的实时报错(我们见过几次)。这是免费的额外信号:写错的代码立即被 IDE 标红,Claude 当场修。

---

## 七、常见反模式

### 7.1 过度澄清

❌ "如果用户已经在 X 选了 A,但又想做 B,但 B 的状态又依赖 C 时,应该怎么处理边界?"

这种过度澄清浪费时间。**Claude 看代码 30 秒能搞清楚的边界,不需要你脑补 5 分钟。**

直接说目标,出问题再调整。

### 7.2 一次说太多

❌ 一次性给 10 个需求,期待 Claude 全做完不出错。

✅ 拆成 3-4 个 PR-size 的批次,每批做完验证一次。

我们整个项目 100+ 次往返,每次平均 3-5 个变更,很少一次干 10 件事。

### 7.3 忽略 Claude 的提示

如果 Claude 说:
> "我注意到 X 已经存在,你确定要新建吗?"

**这通常是有原因的**。可能是命名冲突、已废弃、有更好的替代方案。停下来确认比直接覆盖安全。

### 7.4 在错误层级修问题

✅ 找根因:为什么会这样?
❌ 见招拆招:加个 `!important` 盖过去

实战:抽屉打开后 modal 被遮挡 → 不要 `z-index: 99999`,而是查 z-index 全局规划,理顺层级关系。

### 7.5 让 Claude "猜"项目约定

❌ "用合适的图标"
✅ "用 BL.icon('edit', 12),与 ObjectTypes 抽屉的编辑按钮一致"

Claude 不知道你项目的图标库叫什么、什么风格,除非你说。第一次说清楚后,后续它会沿用。

---

## 八、实战案例

### 8.1 案例:重做"对象图谱"页签

**需求**:用户给了 PDF 规范(7 页),要求做一个 Canvas 链接可视化工具。

**Claude 的处理**:
1. 读 PDF,提炼 5 大要点(布局/工具栏/画布/交互/视觉)
2. 看现有 `TabPropsCanvas.vue` 找 SVG 操作模式
3. 检查 `BL.icon` 是否有 cursor/move/zoom 图标 → 缺,补 4 个
4. 写 `TabLinkGraph.vue`(545 行,SVG + 拖动 + 缩放 + 模态)
5. 替换 ObjectTypes 里的 placeholder
6. 调整 `.ot-tab-content` 样式让 SVG 顶满
7. Build verify

**全程用户没有写一行代码**,只贴了规范 PDF + 说"按规范实施"。

### 8.2 案例:统一 6 个模块的左树

**需求**:6 个模块的左侧分组树都改成共享的"行业分类树"。

**Claude 的处理**:
1. 读现有 `LeftGroupTree.vue` 看接口
2. 设计新组件 `CategoryTreeFilter.vue`(emit subtree codes Set)
3. 用 TodoWrite 列出 6 个模块依次替换
4. 每个模块:换 import + 改 template + 调整 filter 逻辑
5. 兼容 snake/camel 字段(`r.category_code || r.categoryCode`)
6. Build verify

**关键点**:
- 设计组件时考虑了**通用性**(emit codes 而不是 group_id,让父组件按 category_code 过滤)
- 第 6 个模块改完时,前 5 个已经稳定,不需要再回头改

### 8.3 案例:"层级不对,遮挡弹框"

**用户输入**:截图 + 7 个字。

**Claude 的处理**:
1. Grep 全项目的 `z-index` 找出 modal 和 drawer 的层级
2. 发现 PropertyFormatModal `z-index: 999` < PropertyDetailDrawer `z-index: 1010`
3. 把所有大模态提升到 `z-index: 1200`
4. 写注释解释层级规划

**3 分钟解决,根因 + 修复 + 注释一气呵成。**

### 8.4 案例:页面空白(TDZ)

**用户输入**:截图 + "问题"。

**Claude 第一轮**:猜 HMR 缓存,让用户硬刷。

**用户提供更多信息**:F12 console 截图,显示
> `ReferenceError: Cannot access 'refs' before initialization at watch.immediate (SharedPropertyDetailDrawer.vue:299:3)`

**Claude 第二轮**:
1. 立即定位 `SharedPropertyDetailDrawer.vue:299`
2. 看到 `watch(immediate: true)` 引用了下方 `const refs = ref([])`
3. 解释 TDZ(`const` 在声明前不可访问)
4. 把 `refs` / `refQ` 提前到 watch 之前
5. 删除下方的重复声明

**教训**:不要让 Claude 猜浏览器错误,**让用户贴 console**。

---

## 九、效率技巧

### 9.1 让 Claude "对照另一个模块写"

最高效的"新模块"指令模板:

> 参考 [模块 A] 的实现,新建一个 [模块 B] 模块,差异是 [...]。

Claude 会:
1. 读模块 A 的全部相关文件
2. 复制结构 + 改名 + 调字段
3. 保留所有视觉/交互约定

实战:做共享属性时 → "参考值类型模块"。做类型类时 → "参考共享属性的列表 + 抽屉"。

### 9.2 Memory 持久化

`C:\Users\Administrator\.claude\projects\...\memory\` 里写下:
- 项目结构常识
- 字段命名约定(snake/camel 陷阱)
- 高频改动文件路径
- 用户偏好(中文回复 / 不要过度澄清 / etc.)

下次开新会话,Claude 自动加载,不用从头解释一遍。

### 9.3 SkillsDocs / CLAUDE.md

项目根目录的 `CLAUDE.md`(或 `UI规范.md` 这类长文档)Claude 进入项目时会主动读。可以放:
- 命名约定
- 常用命令
- 关键约束(如"前端构建用 npx vite build,不要用 npm run build")

### 9.4 Plan Mode

复杂任务想先看 Claude 的方案再决定?用 Plan Mode(ExitPlanMode 工具):
- Claude 输出整体方案
- 你确认/调整/批准
- 才进入实际编辑阶段

避免"已经写完了才发现方向不对"。

### 9.5 委托后台任务

构建、测试、长查询都用 `run_in_background: true`:

```bash
# 启动后端 (15-30s)
mvn spring-boot:run (background)

# Claude 可以同时继续准备前端代码
# 后端起来后会主动通知 Claude 继续
```

并行处理,效率倍增。

### 9.6 别让 Claude 重复说

如果你和它讨论过的事情(如"项目用 PowerShell"),不需要每次都强调。它会记住一个会话内的上下文。

跨会话的偏好放到 Memory 里。

---

## 十、协作节奏

### 10.1 一个迭代周期

健康的协作节奏(按 5-15 分钟一个回合):

```
[用户] 截图 + 一句需求 ←———— 10 秒
       ↓
[Claude] 探查 + 列计划 + 改代码 ←—— 2-5 分钟
       ↓
[Claude] build verify + 简短报告 ←—— 30 秒
       ↓
[用户] 看效果(浏览器/IDE)←———— 30 秒
       ↓
[用户] 满意 / 截图反馈下一轮 ←—— 10 秒
```

整个项目我们就是这个节奏循环上百次。

### 10.2 什么时候打断 Claude

**值得打断**:
- 方向明显错(选错文件、调错 API)
- 你想到更优方案
- 优先级变了

**不值得打断**:
- "我觉得它写得慢"(它在并行)
- "代码风格小问题"(等它写完一起说)
- "不放心想检查"(等 build verify 出结果)

### 10.3 验收标准

每轮交付,Claude 会给出:
- ✅ Build 通过状态
- 📝 改动文件清单
- 💡 关键设计点解释
- ⚠️ 已知限制(如果有)

你需要:
- 浏览器硬刷新看效果
- 截图反馈或文字确认
- 如果通过 → 进入下一轮

---

## 十一、避免"过度依赖"

最后一条警告:**不要把所有思考外包给 AI**。

Claude 适合:
- 实施已经清晰的需求
- 探查项目找相关代码
- 重构、改名、统一风格
- 修明显的 bug
- 写样板代码(CRUD、表格、表单)

Claude 不适合:
- 产品决策("功能要不要做")
- 业务架构("模型怎么分层")
- 性能预算("能不能扛 10万 QPS")
- 道德/合规判断

**用 Claude 把"如何实现"的负担拿走,让自己专注"该做什么、为什么"。**

---

## 附录:速查清单

### A. 一次性指令模板

新建模块:
> "参考 [模块A],新建 [模块B],差异是 [...]"

修复 bug:
> "[贴截图 / 报错文本]\n[一句话现象]"

视觉对齐:
> "参考 [模块A 的 X],把 [模块B 的 Y] 改成一致"

清理重构:
> "把 N 个模块都用的 X 模式抽成共享组件"

### B. 调试 9 步法

1. 截图 / 报错文本贴出来
2. 让 Claude Grep 定位
3. 确认根因(不要见招拆招)
4. 找最小修复
5. Build verify
6. 浏览器硬刷新验证
7. 记录到踩坑清单
8. 如有共性,考虑抽象修复
9. 文档同步更新

### C. 高频命令

```bash
# 前端 build
cd frontend && npx vite build

# 后端编译
cd backend && mvn -DskipTests clean compile

# 后端启动 (background)
cd backend && mvn -q -DskipTests spring-boot:run

# 等服务就绪
until curl -sf http://localhost:8088/bontolink/api/health; do sleep 2; done

# 杀进程
Get-Process java | Stop-Process -Force

# 强制 schema 重建
rm bontolink.db
```

### D. 必读项目文件

新接手项目,让 Claude 先读这些:
- `README.md` / `CLAUDE.md` — 项目目标 + 关键命令
- `UI规范.md`(我们这个项目) — 视觉/交互基线
- `package.json` / `pom.xml` — 依赖栈
- `router/index.js` 或路由表 — 模块清单
- 任意一个最复杂的页面 — 项目代码风格基线

---

## 后记

这份指南不是 Claude 的"用户手册",更像我们这次协作沉淀出来的"双人编程心法"。

核心理念:**Claude 不是工具,是伙伴。**

它需要清晰的目标、可验证的反馈、合理的约束 —— 跟带新人一样。但它的速度、记忆、零厌倦特性,让它能在工程量上把人解放出来,专注真正需要人类判断的地方。

如果你能让 Claude 在 80% 的时间里"看代码 → 提方案 → 改代码 → 自验证 → 简短汇报",而你只在关键节点把关 —— 那就达到了高效协作。

> "Code at the speed of thought." — Claude Code 的理想状态

---

**文档版本**

| 版本 | 日期 | 内容 |
|---|---|---|
| v1.0 | 2026-06-01 | 初版整理,沉淀自 BOntoLink02 项目实践 |
