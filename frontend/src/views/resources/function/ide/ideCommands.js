/**
 * 命令注册表 (P9 · 文档 模块9-1「命令面板」)
 *
 * 命令面板、快捷键、菜单三者共用这一份定义:
 * 面板负责模糊搜索与执行,ideKeys 负责按键映射,菜单仍走各自的项(避免大改 P5 的菜单结构)。
 *
 * ctx 由 FunctionIde 注入,把 IDE 的能力(编辑器 / 调试 / 运行 / 面板 / 设置)传进来。
 */
export function buildCommands(ctx) {
  const ed = () => ctx.editor?.value
  const monacoAction = (id) => () => {
    const e = ed()
    if (!e) return
    e.focus()
    const a = e.getAction?.(id)
    if (a) a.run()
    else { try { e.trigger('palette', id, null) } catch { /* 命令不存在:静默 */ } }
  }

  return [
    /* —— 文件 —— */
    { id: 'file.save', title: '保存文件', category: '文件', key: 'ctrl+s', run: ctx.saveActive },
    { id: 'file.reloadTree', title: '刷新项目文件树', category: '文件', run: ctx.loadTree },
    { id: 'file.goto', title: '转到文件…', category: '文件', key: 'ctrl+p', run: () => ctx.openPalette('file') },

    /* —— 编辑 —— */
    { id: 'edit.format', title: '格式化文档', category: '编辑', key: 'shift+alt+f', run: monacoAction('editor.action.formatDocument') },
    { id: 'edit.find', title: '查找', category: '编辑', key: 'ctrl+f', run: monacoAction('actions.find') },
    { id: 'edit.replace', title: '替换', category: '编辑', key: 'ctrl+h', run: monacoAction('editor.action.startFindReplaceAction') },
    { id: 'edit.commentLine', title: '切换行注释', category: '编辑', key: 'ctrl+/', run: monacoAction('editor.action.commentLine') },
    { id: 'edit.blockComment', title: '切换块注释', category: '编辑', key: 'shift+alt+a', run: monacoAction('editor.action.blockComment') },
    { id: 'edit.selectAll', title: '全选', category: '编辑', key: 'ctrl+a', run: monacoAction('editor.action.selectAll') },
    { id: 'edit.selectHighlights', title: '选择所有匹配项', category: '编辑', key: 'ctrl+shift+l', run: monacoAction('editor.action.selectHighlights') },

    /* —— 运行与调试 —— */
    { id: 'run.file', title: '以非调试模式运行', category: '运行', key: 'ctrl+f5', run: ctx.runActiveFile },
    { id: 'run.stop', title: '停止运行', category: '运行', run: ctx.stopRun },
    { id: 'debug.start', title: '启动调试', category: '调试', key: 'f5', run: ctx.debugStartOrContinue },
    { id: 'debug.stop', title: '停止调试', category: '调试', key: 'shift+f5', run: ctx.debugStop },
    { id: 'debug.toggleBreakpoint', title: '切换断点', category: '调试', key: 'f9', run: ctx.toggleBreakpoint },
    { id: 'debug.stepOver', title: '逐过程', category: '调试', key: 'f10', run: ctx.stepOver },
    { id: 'debug.stepIn', title: '单步进入', category: '调试', key: 'f11', run: ctx.stepIn },
    { id: 'debug.stepOut', title: '单步跳出', category: '调试', key: 'shift+f11', run: ctx.stepOut },
    { id: 'debug.conditionalBreakpoint', title: '新建条件断点', category: '调试', key: 'ctrl+f9', run: ctx.conditionalBreakpoint },
    { id: 'debug.hitCountBreakpoint', title: '新建命中次数断点', category: '调试', run: ctx.hitCountBreakpoint },
    { id: 'debug.logpoint', title: '新建日志点', category: '调试', run: ctx.logpoint },
    { id: 'debug.enableAllBreakpoints', title: '启用所有断点', category: '调试', run: ctx.enableAllBreakpoints },
    { id: 'debug.disableAllBreakpoints', title: '禁用所有断点', category: '调试', run: ctx.disableAllBreakpoints },
    { id: 'debug.clearBreakpoints', title: '删除所有断点', category: '调试', run: ctx.clearBreakpoints },

    /* —— 代码仓 —— */
    { id: 'scm.push', title: '推送到远程', category: '代码仓', run: ctx.push },
    { id: 'scm.branches', title: '打开分支管理', category: '代码仓', run: () => ctx.showActivity('branch') },
    { id: 'scm.changes', title: '打开版本变更面板', category: '代码仓', run: () => ctx.showActivity('scm') },

    /* —— 视图 —— */
    { id: 'view.palette', title: '显示所有命令', category: '视图', key: 'ctrl+shift+p', run: () => ctx.openPalette('command') },
    { id: 'view.settings', title: '打开通用设置', category: '视图', key: 'ctrl+,', run: ctx.openSettings },
    { id: 'view.keybindings', title: '自定义快捷键', category: '视图', run: ctx.openKeybindings },
    { id: 'view.togglePanel', title: '显示 / 隐藏底部面板', category: '视图', key: 'ctrl+j', run: ctx.togglePanel },
    { id: 'view.toggleSidebar', title: '显示 / 隐藏侧边栏', category: '视图', key: 'ctrl+b', run: ctx.toggleSidebar },
    { id: 'view.themeDark', title: '切换主题:深色', category: '视图', run: () => ctx.setTheme('dark') },
    { id: 'view.themeLight', title: '切换主题:浅色', category: '视图', run: () => ctx.setTheme('light') },
    { id: 'view.themeHc', title: '切换主题:高对比黑', category: '视图', run: () => ctx.setTheme('hc') },
    { id: 'view.zoomIn', title: '放大', category: '视图', key: 'ctrl+=', run: () => ctx.zoom(1) },
    { id: 'view.zoomOut', title: '缩小', category: '视图', key: 'ctrl+-', run: () => ctx.zoom(-1) },
    { id: 'view.zoomReset', title: '重置缩放', category: '视图', run: () => ctx.zoom(0) },

    /* —— 帮助 —— */
    { id: 'help.docs', title: '打开帮助文档', category: '帮助', run: ctx.openHelp },
    { id: 'help.backToPlatform', title: '返回平台(函数详情)', category: '帮助', run: ctx.backToPlatform },
  ]
}

/**
 * 模糊匹配:子序列命中即可(输入 "sf" 能匹配 "保存文件" 的拼音?不做拼音,
 * 只做字符子序列 + 连续片段加权),返回 score,不匹配返回 -1。
 */
export function fuzzyScore(text, query) {
  if (!query) return 0
  const t = String(text).toLowerCase()
  const q = String(query).toLowerCase()
  if (t.includes(q)) return 100 - t.indexOf(q)      // 连续命中优先
  let ti = 0, hit = 0
  for (const ch of q) {
    const idx = t.indexOf(ch, ti)
    if (idx < 0) return -1
    ti = idx + 1
    hit++
  }
  return hit
}
