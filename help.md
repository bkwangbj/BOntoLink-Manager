# Claude Code 协作复盘与改进建议

> 基于 BOntoLink02 项目实际协作过程的复盘
>
> 版本: v1.0 · 2026-06-01

---

## 引言

本文是对你在 BOntoLink02 项目中与 Claude Code 协作过程的**复盘性反馈**,包含:

1. 你这次协作中**做得不足**的地方 + 具体改进建议
2. 社区其他用户的**最佳实践**与方法论
3. 你可以**立刻执行**的 5 个行动项

目标:把你从"打字员"升级到"导演",让 Claude Code 在 70-80% 的时间为你输出,你专注关键决策。

---

## 一、你这次协作里的不足

### 1.1 "让 Claude 猜浏览器错误"——浪费了不止一轮

**实例**:页面空白那次。
- 第一次:你只发了截图 + 说"页面空白,如何处理解决" → 我猜 HMR 缓存 / layout 问题 → 让你硬刷 → 没解决
- 第二次:你发 F12 Console 截图(`Cannot access 'refs' before initialization`)→ 我 30 秒定位 TDZ 修复

**改进**:**浏览器 / 运行时问题,第一时间贴 F12 Console**,不要让我从症状反推根因。一张 console 截图 ≈ 10 条猜测。

---

### 1.2 频繁的"还原上一步修改"

**实例**:
- "启用按钮改实心绿" → 几分钟后 → "还原上一步修改"
- "层级不对" → 我改了 → "遮挡"(还要补)

**改进**:
- **多看两眼参考图再下指令**,避免改完才发现要的不是这个
- 把"要改的"和"不能动的"**一次说清楚**(否则容易过度发挥)
- 给 Claude 一个**视觉对标**:"参考图2 但不要那个圆圈样式"

---

### 1.3 项目约定**太晚才沉淀**

**实例**:
- 做了 5 个模块后才统一"批量操作栏的 outline 配色"
- 做了 10 个模块的左树后才统一改成"行业分类树"
- `UI规范.md` 是项目末期才写的

**改进**:**在做第 2 个同类模块时就写下规范**,而不是做了 6 个之后回头改。

- 第 1 个模块:摸索
- 第 2 个模块:写下"这是约定"
- 第 3 个开始:照搬

---

### 1.4 缺少 CLAUDE.md / Memory

**问题**:每次新会话我都得从头扫项目结构、问"你们前端用 vite 还是 webpack"。

**改进**:在项目根目录写 `CLAUDE.md`:

```markdown
# BOntoLink02

## 技术栈
- 后端: Spring Boot + SQLite + MyBatis
- 前端: Vue 3 + Vite + 原生 CSS (无 Element/Antd)

## 常用命令
- 前端 build: cd frontend && npx vite build
- 后端启动: cd backend && mvn -q -DskipTests spring-boot:run
- 后端编译: cd backend && mvn -DskipTests clean compile

## 关键约定
- CSS 类前缀: ot-/lk-/vt-/et-/sp-/if-/ds-/tc-
- API 前缀: /api,响应包装 R<T> = { code, msg, data }
- 主键: "{prefix}-" + UUID
- 时间: TEXT NOT NULL DEFAULT (datetime('now','localtime'))

## 避免
- 不要新增 Element/Antd 依赖
- 不要改 src/lib/bl.js 已有 icon
- 不要用 fetch,统一用 http (拦截器包装)
```

每个新会话 Claude 都会自动读,免去 5 分钟的"项目摸底"。

---

### 1.5 接受"前端模拟"实现

**实例**:`TabLinkGraph.vue` 里我说"新建链接是前端模拟,后端 API 待联调",你**没有追问**。结果这个功能一直是半成品。

**改进**:看到"模拟 / 待联调 / TODO"立刻问:**"是真的没法接,还是没空接?"**

- 如果是没空 → 加到 todo list 单独成一轮
- 如果是没法接 → 至少留个明显的 UI 提示("暂不支持")

---

### 1.6 改变方向但没说原因

**实例**:有时你说"删掉这项""不要这个" — 我照做了,但**不知道为什么**。下次类似场景我可能又会加上。

**改进**:删除 / 拒绝时**带一句理由**:

- ❌ "删掉这项"
- ✅ "删掉值类型子页签 — 用户在属性表里点击值类型列单元格已经能唤起选择面板,这个 tab 是冗余的"

理由比指令更重要 — 它能避免我以后犯同样错。

---

### 1.7 一次说太多碎需求

**实例**:有几次你一次发了 3-4 张图 + 4-5 条需求,我处理时容易遗漏或合并错位。

**改进**:**一次 1-3 个相关变更**最容易准确执行。

- 真有 5 个需求 → 列个小清单,每条单独处理
- 或让我先 plan 再做

---

### 1.8 没有及时让我跑 verify

**实例**:几次你说"做完了吗" — 实际上我已经写了代码但**没跑 build**。需要你提醒。

**改进**:可以在 CLAUDE.md 或一开始就声明:

> "每个改动结束必须 `npx vite build` 验证通过才算交付"

或者直接在每个回合最后加一句"build 一下"。

---

### 1.9 没用过 Plan Mode / 子 agent

**实例**:做对象图谱(Canvas)那次,我直接开干,写了 545 行。如果你让我先 plan,可能会少几次返工。

**改进**:**复杂任务(> 200 行 / 涉及 3+ 文件)**先让 Claude plan:

> "先给我方案,我确认后再写代码"

我会输出大纲、关键决策点、可选方案,你认可再实施。

---

### 1.10 没有积累 Memory

**问题**:用户偏好(比如"我喜欢中文回复 / 不要过度澄清 / 喜欢简短总结")每次都得重新告诉我。

**改进**:遇到偏好性的事,说"记下来":

> "以后所有抽屉的关闭按钮都放右上角,记下来"

我会写到 memory,跨会话保留。

---

## 二、社区里"做得好"的 Claude Code 用户怎么干

### 2.1 工作流自动化

**Hooks**:在 settings.json 里配置自动行为

- `PostToolUse`: 每次 Edit 后自动 prettier / eslint --fix
- `PreToolUse`: 阻止某些危险命令
- `Stop`: 任务完成时桌面通知

**示例** (`.claude/settings.json`):

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [{ "type": "command", "command": "npx prettier --write $TOOL_INPUT_FILE_PATH" }]
      }
    ]
  }
}
```

**Permission allowlist**:常用安全命令免授权

```json
{
  "permissions": {
    "allow": ["Bash(npm:*)", "Bash(git status)", "Bash(git diff)", "Bash(curl:*)"]
  }
}
```

社区高手用 `/fewer-permission-prompts` skill 自动扫描自己常用的命令,加白名单。

---

### 2.2 多 agent 并行(Workflow)

复杂任务用 Workflow 把多个子任务**并行**:

- 一个 agent 找 bug
- 一个 agent 验证 bug 是否真实
- 一个 agent 写修复
- 一个 agent code review

社区里有人用 Workflow 在 30 分钟内做完了"5 个候选方案各写一个 demo,judge 评分,选最优"的设计探索。

---

### 2.3 Subagents 分工

- **Explore agent**:专门做"广度搜索"(7-8 个文件以上的扫描),返回结论不返回原文 → 节省 context
- **Plan agent**:专门做架构方案设计
- **Code-reviewer**:专门做 PR 审核

社区典型用法:

```
"Use Explore agent to find all places we call the old auth API. 
Then Plan a migration strategy. Then implement."
```

---

### 2.4 Skill 系统

我们这次系统里**没用过任何 Skill**(`/code-review` / `/security-review` / `/verify` 等),但社区高手会:

- **提交 PR 前**:`/code-review high` — 多 agent 对差异做对抗式审核
- **重大改动后**:`/security-review` — 专门扫安全问题
- **担心引入 bug**:`/verify` — 启动 app 实际验证行为

---

### 2.5 长期记忆(Memory)

社区里有人在 Memory 中记录:

```markdown
# user.md
- 全栈工程师, 10 年经验, 熟悉 Vue / Spring Boot
- 偏好中文回复, 简短总结, 不要展开"为什么"长篇大论
- 不喜欢"建议你看看 X 文档"这种甩锅式回答

# feedback.md
- 提交代码前必须 build verify
- 别加多余注释, 代码自解释优先
- 用户经常用截图沟通, 优先理解图意

# project.md  
- 项目用 SQLite + 单机部署, 不需要考虑分布式
- 后端 PowerShell 启动, 前端 vite dev
```

---

### 2.6 Git 工作流

社区里推荐:

- **小步提交**:每完成一个 task 就 commit,不等所有 task 完成
- **分支命名**:`feat/link-types` / `fix/sticky-table-gap`
- **commit message** 让 Claude 自动生成符合 conventional commits 规范
- **PR 描述**:让 Claude 基于 git log 自动生成

---

### 2.7 调试技巧

- **Network tab 截图** > 描述请求失败:省时间
- **F12 console 截图** > 描述错误:省时间
- **`console.log` 输出截图** > 让 Claude 猜变量值
- **Sources 断点截图** > "我想看 X 函数的执行流"

---

### 2.8 用 `/init` 起项目

`/init` skill 会:

1. 扫描你的项目
2. 自动生成 `CLAUDE.md` 草稿
3. 包含目录结构、命令、依赖、约定

新接手项目第一件事就是跑 `/init`,而不是问 Claude "这个项目是干啥的"。

---

### 2.9 让 Claude 写测试

社区共识:**让 Claude 写代码 + 让 Claude 写测试,比让人类写测试更高效**。

最有效的工作流:

1. 让 Claude 写新功能 + 单测
2. 跑测试通过 → 提交
3. 测试失败 → Claude 自动修
4. 改 bug 时先写**复现 bug 的失败测试**,再修

我们这次项目几乎**没写过测试**。如果加上测试,后续重构的信心会大很多。

---

### 2.10 限制 Claude 的破坏力

社区里"翻车故事"大多是:

- 让 Claude 直接 `git push --force`
- 让 Claude `rm -rf` 错地方
- 让 Claude 改生产配置

**对策**:

- `--dangerously-skip-permissions` 慎用(只在沙箱环境)
- 危险命令始终保留权限提示
- 重要分支保护(GitHub branch protection rules)
- 重要操作前先 dry-run

---

### 2.11 "Pair Programming" 节奏

社区里的高手描述节奏:

> "我和 Claude 协作时,我大概 30% 的时间在思考下一步,70% 的时间在看它干活 / 给反馈。
> 反过来一开始我 80% 在打字给指令 — 那是没用对。"

把自己从"打字员"升级到"导演",才是 Claude Code 的正确打开方式。

---

### 2.12 文档作为协作"配置"

社区有人把项目文档当 **Claude 的输入**而非"给人看的"来设计:

- 命名约定 → 写到 CLAUDE.md → Claude 自动遵守
- API 规范 → 写到 docs/ → 新模块 Claude 自动对齐
- 设计 token → CSS 变量 → Claude 不会"自创颜色"

文档不再是"事后整理",而是"事前约束"。我们这次的 `UI规范.md` 应该早 3 个月写。

---

## 三、给你的 5 个具体行动项(Quick Wins)

按优先级排:

### Action 1: 立刻写 CLAUDE.md(10 分钟)

把项目最常用的命令 + 关键约定写进去。下次新会话 Claude 自动读,省 5 分钟"摸底"。

### Action 2: 配置 permission allowlist(5 分钟)

`.claude/settings.json` 加白名单:

```json
{
  "permissions": {
    "allow": [
      "Bash(npm:*)",
      "Bash(npx:*)",
      "Bash(curl:*)",
      "Bash(git status)",
      "Bash(git diff:*)",
      "Bash(git log:*)",
      "PowerShell(*)"
    ]
  }
}
```

或运行 `/fewer-permission-prompts` 让 Claude 帮你自动生成。

### Action 3: 浏览器问题 → 优先贴 F12 console(永久习惯)

下次再遇到页面空白 / 报错,F12 → 右键 console → 选 + 复制(或截图)→ 第一句话就贴上。

### Action 4: 写"为什么不要"的反馈(永久习惯)

删除 / 拒绝某个改动时,带一句理由。我会记到 memory,避免重复错。

### Action 5: 复杂任务先 plan(可选,看任务大小)

> "先 plan 一下方案,我确认再写代码"

特别是 200+ 行 / 涉及多文件 / 改架构的任务。

---

## 四、你做得**好**的方面(别变)

平衡一下,你这些做得非常好:

1. **截图 + 短描述** — 高效到爆,业界平均水平很多倍
2. **PDF 规范直接贴** — 一次贴完整规范,省去拆碎需求
3. **频繁视觉对标** — "参考图2 / 与共享属性一致" — 节省大量决策成本
4. **承认错误并还原** — "还原上一步修改" 这种果断回退能力是很多人没有的
5. **追求 UI 一致性** — 不放任各模块各自为政,主动要求统一
6. **整理沉淀文档** — 主动要 UI规范 + Claude Code 使用指南,把经验沉淀

这些占了我们高效协作的大头。**保持。**

---

## 五、终极建议:养成"调音"习惯

每周(或每个项目阶段)花 15 分钟做一次复盘:

1. 这周哪 3 件事我重复说了多次 → 写进 CLAUDE.md
2. 这周 Claude 哪 2 次答得不对 → 加到 memory feedback
3. 这周哪些权限提示烦人 → 加白名单
4. 这周哪种类型的任务效率最高 → 找规律复用

**Claude Code 不是"配置一次用一辈子"的工具,是个需要持续调音的乐器。** 一开始可能磕磕碰碰,3 个月后会越来越顺手。

你这次项目积累的 `UI规范.md` + `ClaudeCode使用指南.md` 就是个很好的开始,继续保持迭代。

---

## 附录:相关文档

本项目还有两份配套文档,建议结合使用:

| 文档 | 内容 | 适合 |
|---|---|---|
| **UI规范.md** | 项目视觉、交互、组件、命名约定 | 新模块开发 / 设计对齐 |
| **ClaudeCode使用指南.md** | 与 Claude Code 协作的具体技巧 | 团队培训 / 个人提升 |
| **help.md**(本文) | 个人协作复盘 + 业界经验 + 行动项 | 自我反思 / 持续改进 |

三份合起来形成"项目知识体系 + AI 协作能力 + 个人迭代闭环"。

---

## 六、推荐工具 / Skill 速查

下面是社区高频使用的 Skill 和工具,可以直接在 Claude Code 里用:

| Skill / 工具 | 触发命令 | 用途 |
|---|---|---|
| `/init` | `/init` | 新项目自动生成 CLAUDE.md |
| `/fewer-permission-prompts` | `/fewer-permission-prompts` | 自动扫描常用命令,加白名单 |
| `/code-review` | `/code-review high` | 多 agent 对当前 diff 做严格审核 |
| `/security-review` | `/security-review` | 专门扫安全漏洞 |
| `/verify` | `/verify` | 启动 app 验证改动实际是否工作 |
| `/simplify` | `/simplify` | 自动重构、复用、简化代码 |
| `update-config` | "allow npm commands" | 配置 settings.json |
| `deep-research` | "深度调研 X 技术" | 多源对抗式研究,生成有引用的报告 |
| `schedule` | "每天早上跑 X" | 创建定时任务 |
| `loop` | `/loop 5m /foo` | 周期性运行某个任务 |

---

## 文档版本

| 版本 | 日期 | 内容 |
|---|---|---|
| v1.0 | 2026-06-01 | 初版整理,基于 BOntoLink02 项目实践 |

---

**最后一句话**

> Claude Code 是一面镜子。你怎么沟通,它就怎么回应。
> 投入 1 小时调优工具配置和习惯,后续 100 小时都会受益。
