<template>
  <div class="fcp">
    <div class="fcp-gutter">
      <div v-for="n in lineCount" :key="n" class="fcp-ln">{{ n }}</div>
    </div>
    <pre class="fcp-code"><code v-html="highlighted"></code></pre>
  </div>
</template>

<script setup>
/**
 * 代码预览 (文档 4.4.2):带行号的语法高亮代码块, 只读, 支持纵向滚动。
 *
 * 用轻量正则着色而不是拉 Monaco:详情抽屉只需要只读展示, Monaco 会把这个页面的
 * 首屏体积推上去。P5 在线编排 IDE 落地后, 那边用 Monaco, 两处职责分开。
 */
import { computed } from 'vue'

const props = defineProps({
  code: { type: String, default: '' },
  /** 1=Python 2=TypeScript */
  language: { type: Number, default: 2 }
})

const TS_KEYWORDS = ['import', 'from', 'export', 'class', 'interface', 'public', 'private', 'protected',
  'static', 'const', 'let', 'var', 'function', 'return', 'if', 'else', 'for', 'while', 'throw', 'new',
  'try', 'catch', 'finally', 'async', 'await', 'extends', 'implements', 'this', 'true', 'false', 'null',
  'undefined', 'void', 'string', 'number', 'boolean', 'any', 'type', 'enum', 'switch', 'case', 'break',
  'default', 'continue', 'typeof', 'instanceof']
const PY_KEYWORDS = ['import', 'from', 'class', 'def', 'return', 'if', 'elif', 'else', 'for', 'while',
  'raise', 'try', 'except', 'finally', 'with', 'as', 'pass', 'None', 'True', 'False', 'and', 'or', 'not',
  'in', 'is', 'lambda', 'yield', 'self', 'async', 'await', 'global', 'assert']

const lines = computed(() => String(props.code || '').split('\n'))
const lineCount = computed(() => Math.max(1, lines.value.length))

function escapeHtml(s) {
  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

const highlighted = computed(() => {
  const src = escapeHtml(props.code || '')
  if (!src) return '<span class="fcp-muted">暂无代码内容</span>'
  const kws = (Number(props.language) === 1 ? PY_KEYWORDS : TS_KEYWORDS).join('|')
  /*
   * 单遍扫描, 顺序即优先级: 块注释 → 行注释 → 文档字符串 → 各类字符串 → 装饰器 → 关键字 → 数字。
   * 先匹配注释与字符串, 避免把字符串里的单词误染成关键字。
   */
  const re = new RegExp(
    '(\\/\\*[\\s\\S]*?\\*\\/)' +                       // 1 块注释
    '|(\\/\\/[^\\n]*)' +                               // 2 行注释
    '|(#[^\\n]*)' +                                    // 3 Python 行注释
    '|("""[\\s\\S]*?""")' +                            // 4 Python 文档字符串
    '|(`(?:\\\\.|[^`\\\\])*`)' +                       // 5 模板字符串
    '|("(?:\\\\.|[^"\\\\])*")' +                       // 6 双引号字符串
    "|('(?:\\\\.|[^'\\\\])*')" +                       // 7 单引号字符串
    '|(@[A-Za-z_][\\w]*)' +                            // 8 装饰器
    '|\\b(' + kws + ')\\b' +                           // 9 关键字
    '|\\b(\\d+(?:\\.\\d+)?)\\b',                       // 10 数字
    'g')
  return src.replace(re, (m, blk, line, hash, doc, tpl, dq, sq, dec, kw, num) => {
    if (blk || line || hash || doc) return `<span class="fcp-cmt">${m}</span>`
    if (tpl || dq || sq) return `<span class="fcp-str">${m}</span>`
    if (dec) return `<span class="fcp-dec">${m}</span>`
    if (kw) return `<span class="fcp-kw">${m}</span>`
    if (num) return `<span class="fcp-num">${m}</span>`
    return m
  })
})
</script>

<style scoped>
.fcp {
  display: flex; align-items: stretch;
  background: var(--bl-bg-2);
  border: 1px solid var(--bl-border);
  border-radius: 4px;
  overflow: auto;
  max-height: 340px;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px; line-height: 19px;
}
.fcp-gutter {
  flex-shrink: 0; position: sticky; left: 0; z-index: 1;
  padding: 10px 8px; text-align: right;
  background: var(--bl-bg-1); color: var(--bl-text-3);
  border-right: 1px solid var(--bl-divider);
  user-select: none; min-width: 40px;
}
.fcp-ln { height: 19px; font-variant-numeric: tabular-nums; }
.fcp-code {
  flex: 1; margin: 0; padding: 10px 12px;
  white-space: pre; color: var(--bl-text-1);
  font-family: inherit; font-size: inherit; line-height: inherit;
}
.fcp-code :deep(.fcp-kw)  { color: #c586c0; }
.fcp-code :deep(.fcp-str) { color: #ce9178; }
.fcp-code :deep(.fcp-cmt) { color: #6a9955; }
.fcp-code :deep(.fcp-dec) { color: #dcdcaa; }
.fcp-code :deep(.fcp-num) { color: #b5cea8; }
.fcp-code :deep(.fcp-muted) { color: var(--bl-text-3); }
/* 浅色主题下换一套对比度更合适的配色 */
:root[data-theme="light"] .fcp-code :deep(.fcp-kw)  { color: #af00db; }
:root[data-theme="light"] .fcp-code :deep(.fcp-str) { color: #a31515; }
:root[data-theme="light"] .fcp-code :deep(.fcp-cmt) { color: #008000; }
:root[data-theme="light"] .fcp-code :deep(.fcp-dec) { color: #795e26; }
:root[data-theme="light"] .fcp-code :deep(.fcp-num) { color: #098658; }
</style>
