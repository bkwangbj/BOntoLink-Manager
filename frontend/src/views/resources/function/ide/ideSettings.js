/**
 * IDE 外观设置:默认值 + 持久化 + 到 Monaco 配置的映射
 *
 * 文档「通用设置」共 9 组约 50 项,默认值严格照文档 3.4 分组明细。
 * 存储键名按文档规定为 monacoFullSettings(localStorage)。
 *
 * 说明:少数文档设置项在 Monaco 里没有一一对应的原生开关,映射方式在对应行注释里写明,
 * 不做静默丢弃 —— 设置面板会照常展示,但要知道它实际落到哪个 Monaco 选项上。
 */

export const STORAGE_KEY = 'monacoFullSettings'

export const DEFAULTS = {
  /* 1. 全局视觉外观 */
  theme: 'dark',                       // dark(VS Dark) / light(VS) / hc(High Contrast Dark)
  fontSize: 14,
  fontFamily: 'Consolas',
  fontLigatures: true,
  letterSpacing: 0,
  lineHeight: 1.5,                     // 倍数, 应用时乘以字号
  lineNumbers: 'on',                   // on / off / relative
  lineNumbersMinChars: 5,
  paddingTop: 8,
  paddingBottom: 8,
  renderLineHighlight: 'all',          // all / line(仅线条→gutter) / none
  renderLineHighlightOnlyWhenFocus: false,

  /* 2. 空白与特殊字符 */
  renderWhitespace: 'selection',       // none / boundary / selection / all
  renderControlCharacters: false,

  /* 3. 滚动行为 */
  stickyScroll: true,
  stickyScrollMaxLineCount: 5,
  smoothScrolling: true,
  scrollBeyondLastLine: true,
  scrollBeyondLastColumn: 5,
  horizontalScrollbarSize: 10,
  verticalScrollbarSize: 10,
  mouseWheelScrollSensitivity: 1,

  /* 4. 缩略图 */
  minimap: true,
  minimapScale: 1,                     // Monaco 原生取值 1|2|3(文档写的 0.3~1 在 Monaco 无对应项)
  minimapShowSlider: 'mouseover',      // always / mouseover
  minimapSide: 'right',                // right / left

  /* 5. 光标与选择 */
  cursorBlinking: 'smooth',            // blink / smooth / phase / solid
  cursorSmoothCaretAnimation: 'on',    // 0.45 起为 off/explicit/on, 不再是 boolean
  cursorWidth: 2,
  selectionHighlight: true,
  occurrencesHighlight: true,          // 映射为 'singleFile' / 'off'

  /* 6. 缩进与标尺 */
  guidesIndentation: true,
  guidesBracketPairs: true,
  ruler1: 80,
  ruler2: 120,

  /* 7. 折叠与侧边距 */
  folding: true,
  foldingStrategy: 'auto',             // auto / indentation
  showFoldingControls: 'mouseover',    // mouseover / always
  glyphMargin: true,

  /* 8. 编辑交互行为 */
  mouseStyle: 'text',                  // text / default
  multiCursorModifier: 'ctrlCmd',      // ctrlCmd / alt
  tabSize: 2,
  insertSpaces: true,
  autoClosingBrackets: 'always',       // always / never / beforeWhitespace
  autoClosingQuotes: 'always',
  formatOnPaste: false,
  formatOnType: false,

  /* 9. 悬浮与代码提示 */
  hover: true,
  hoverSticky: true,
  quickSuggestions: true,
}

/** Monaco 内置主题名 */
export const MONACO_THEME = { dark: 'vs-dark', light: 'vs', hc: 'hc-black' }

export function loadSettings() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return { ...DEFAULTS }
    // 只认识已知键, 避免旧版本残留字段污染
    const saved = JSON.parse(raw)
    const out = { ...DEFAULTS }
    Object.keys(DEFAULTS).forEach(k => { if (saved[k] !== undefined) out[k] = saved[k] })
    return out
  } catch {
    return { ...DEFAULTS }
  }
}

export function saveSettings(s) {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(s)) } catch { /* 隐私模式下忽略 */ }
}

/** 设置对象 → editor.updateOptions() 的入参 */
export function toMonacoOptions(s) {
  return {
    fontSize: num(s.fontSize, 14),
    fontFamily: fontStack(s.fontFamily),
    fontLigatures: !!s.fontLigatures,
    letterSpacing: num(s.letterSpacing, 0),
    lineHeight: Math.round(num(s.lineHeight, 1.5) * num(s.fontSize, 14)),
    lineNumbers: s.lineNumbers || 'on',
    lineNumbersMinChars: num(s.lineNumbersMinChars, 5),
    padding: { top: num(s.paddingTop, 8), bottom: num(s.paddingBottom, 8) },
    // 文档「仅线条」= 只高亮行号栏, Monaco 对应 'gutter'
    renderLineHighlight: s.renderLineHighlight === 'line' ? 'gutter' : (s.renderLineHighlight || 'all'),
    renderLineHighlightOnlyWhenFocus: !!s.renderLineHighlightOnlyWhenFocus,

    renderWhitespace: s.renderWhitespace || 'selection',
    renderControlCharacters: !!s.renderControlCharacters,

    stickyScroll: { enabled: !!s.stickyScroll, maxLineCount: num(s.stickyScrollMaxLineCount, 5) },
    smoothScrolling: !!s.smoothScrolling,
    scrollBeyondLastLine: !!s.scrollBeyondLastLine,
    scrollBeyondLastColumn: num(s.scrollBeyondLastColumn, 5),
    scrollbar: {
      horizontalScrollbarSize: num(s.horizontalScrollbarSize, 10),
      verticalScrollbarSize: num(s.verticalScrollbarSize, 10),
    },
    mouseWheelScrollSensitivity: num(s.mouseWheelScrollSensitivity, 1),

    minimap: {
      enabled: !!s.minimap,
      scale: Math.max(1, Math.min(3, Math.round(num(s.minimapScale, 1)))),
      showSlider: s.minimapShowSlider || 'mouseover',
      side: s.minimapSide || 'right',
    },

    cursorBlinking: s.cursorBlinking || 'smooth',
    cursorSmoothCaretAnimation: s.cursorSmoothCaretAnimation || 'on',
    cursorWidth: num(s.cursorWidth, 2),
    selectionHighlight: !!s.selectionHighlight,
    occurrencesHighlight: s.occurrencesHighlight ? 'singleFile' : 'off',

    guides: { indentation: !!s.guidesIndentation, bracketPairs: !!s.guidesBracketPairs },
    rulers: [num(s.ruler1, 0), num(s.ruler2, 0)].filter(n => n > 0),

    folding: !!s.folding,
    foldingStrategy: s.foldingStrategy || 'auto',
    showFoldingControls: s.showFoldingControls || 'mouseover',
    glyphMargin: !!s.glyphMargin,

    mouseStyle: s.mouseStyle || 'text',
    multiCursorModifier: s.multiCursorModifier || 'ctrlCmd',
    tabSize: num(s.tabSize, 2),
    insertSpaces: !!s.insertSpaces,
    autoClosingBrackets: s.autoClosingBrackets || 'always',
    autoClosingQuotes: s.autoClosingQuotes || 'always',
    formatOnPaste: !!s.formatOnPaste,
    formatOnType: !!s.formatOnType,

    hover: { enabled: !!s.hover, sticky: !!s.hoverSticky },
    quickSuggestions: !!s.quickSuggestions,

    columnSelection: !!s.columnSelection,
  }
}

const FONT_STACK = {
  Consolas: "Consolas, 'Courier New', monospace",
  Menlo: "Menlo, Monaco, monospace",
  Monaco: "Monaco, Menlo, monospace",
  'Fira Code': "'Fira Code', Consolas, monospace",
  'JetBrains Mono': "'JetBrains Mono', Consolas, monospace",
  'Source Code Pro': "'Source Code Pro', Consolas, monospace",
  'Courier New': "'Courier New', monospace",
  系统等宽: "ui-monospace, SFMono-Regular, Consolas, monospace",
}
function fontStack(name) { return FONT_STACK[name] || FONT_STACK.Consolas }
function num(v, dft) { const n = Number(v); return Number.isFinite(n) ? n : dft }

/** 按文件后缀推断 Monaco 语言标识 */
export function langOfPath(path) {
  const p = String(path || '').toLowerCase()
  if (p.endsWith('.ts')) return 'typescript'
  if (p.endsWith('.js')) return 'javascript'
  if (p.endsWith('.py')) return 'python'
  if (p.endsWith('.json')) return 'json'
  if (p.endsWith('.md')) return 'markdown'
  if (p.endsWith('.css')) return 'css'
  if (p.endsWith('.html')) return 'html'
  return 'plaintext'
}
