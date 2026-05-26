#!/usr/bin/env node
/**
 * 把 node_modules/@ant-design/icons-svg 的全部 outlined SVG（447 个，Apache-2.0）
 * 抽取为 backend/src/main/resources/seed/icons-antd.json
 *
 * 用法：node frontend/scripts/extract-antd-icons.cjs
 */
const fs = require('fs')
const path = require('path')

const SRC = path.resolve(__dirname, '../node_modules/@ant-design/icons-svg/inline-svg/outlined')
const OUT = path.resolve(__dirname, '../../backend/src/main/resources/seed/icons-antd.json')

if (!fs.existsSync(SRC)) {
  console.error('找不到 @ant-design/icons-svg, 请先 npm install。')
  process.exit(1)
}

// 中文别名映射：把 frontend/src/lib/icon-zh.js (ESM) 解析成普通对象
const ZH = (() => {
  try {
    const txt = fs.readFileSync(path.resolve(__dirname, '../src/lib/icon-zh.js'), 'utf8')
    const start = txt.indexOf('ANTD_ZH = {')
    if (start < 0) return {}
    let depth = 0, i = start + 'ANTD_ZH = '.length, body = ''
    for (; i < txt.length; i++) {
      const c = txt[i]
      body += c
      if (c === '{') depth++
      else if (c === '}') { depth--; if (depth === 0) break }
    }
    // body 形如 { 'foo':'值', "bar": "值", ... }，eval 解析
    return Function('"use strict";return (' + body + ')')()
  } catch { return {} }
})()

function extractSvg(text) {
  // 兼容 <svg viewBox="..." focusable="false"> 与 <svg ...>
  const vb = (text.match(/viewBox="([^"]+)"/) || [])[1] || '0 0 1024 1024'
  const inner = text.replace(/^[\s\S]*?<svg[^>]*>/, '').replace(/<\/svg>\s*$/, '').trim()
  // 把硬编码颜色（fill="#..." / stroke="#..."）替换为 currentColor；纯路径的 antd 图标本来就没颜色，这里只是兜底
  const cleaned = inner
    .replace(/\sfill="(?!none)#[0-9a-fA-F]{3,8}"/g, ' fill="currentColor"')
    .replace(/\sstroke="(?!none)#[0-9a-fA-F]{3,8}"/g, ' stroke="currentColor"')
  return { viewBox: vb, content: cleaned }
}

const files = fs.readdirSync(SRC).filter(f => f.endsWith('.svg')).sort()
const icons = []
for (const f of files) {
  const text = fs.readFileSync(path.join(SRC, f), 'utf8')
  const { viewBox, content } = extractSvg(text)
  if (!content) continue
  const name = f.replace(/\.svg$/, '')
  icons.push({ name, viewBox, content, zh: ZH[name] || '' })
}

fs.mkdirSync(path.dirname(OUT), { recursive: true })
fs.writeFileSync(OUT, JSON.stringify({ source: 'antd-icons-svg-outlined', count: icons.length, icons }, null, 0))
console.log(`✓ extracted ${icons.length} icons → ${OUT}`)
