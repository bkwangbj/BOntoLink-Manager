/**
 * 新增函数向导 —— 初始模板代码生成
 *
 * 文档 5.2 六、6.3:创建成功后按所选语言、函数名、参数签名、返回值类型生成标准模板,
 * 包含函数类与函数签名、参数类型定义、返回值类型定义、基础注释与占位逻辑、
 * 关联本体对象的类型引用。
 *
 * 本体对象类型在向导里以 "[命名空间] 类名" 形式存储 (与列表页展示一致),
 * 生成代码时剥掉命名空间前缀, 只保留类名作为语言侧类型标识。
 */

/** 函数类型 → 核心装饰器 (文档 3.2 业务枚举与装饰器对照表) */
const DECORATOR = {
  1: 'Function',                 // 常规函数
  2: 'OntologyEditFunction',     // 动作函数: 默认开启编辑事务
  3: 'Function',                 // 聚合函数
  4: 'Function',                 // 衍生函数
  5: 'Function',                 // 时序函数
}

/** 装饰器所在的平台虚拟模块;IDE 侧注入了对应 .d.ts, 编辑器能解析 */
const API_MODULE = '@foundry/functions-api'

function decoratorOf(functionType) {
  return DECORATOR[Number(functionType)] || 'Function'
}

const PY_BASE_TYPE = { string: 'str', number: 'float', boolean: 'bool', any: 'Any' }

/** "[Hydro] Station" → "Station";基础类型原样返回 */
export function bareType(type) {
  return String(type || 'any').replace(/^\s*\[[^\]]*\]\s*/, '').trim() || 'any'
}

/** 是否本体对象类型 (带 [命名空间] 前缀) */
export function isObjectType(type) {
  return /^\s*\[[^\]]*\]/.test(String(type || ''))
}

/** threshold_calc / thresholdCalc → ThresholdCalc (类名由系统按文件名派生) */
export function toClassName(fileBase) {
  const s = String(fileBase || '').trim()
  if (!s) return ''
  return s.split(/[^a-zA-Z0-9]+/).filter(Boolean)
    .map(w => w.charAt(0).toUpperCase() + w.slice(1))
    .join('') || ''
}

/** getHydrologyStationThresholds → get_hydrology_station_thresholds */
export function toSnakeCase(name) {
  return String(name || '').replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase()
}

function pyType(type) {
  const t = bareType(type)
  return PY_BASE_TYPE[t] || t
}

/** 收集需要 import 的本体对象类型 (去重, 保持出现顺序) */
function collectObjectTypes(params, ret) {
  const out = []
  const push = (t) => {
    if (isObjectType(t) && !out.includes(bareType(t))) out.push(bareType(t))
  }
  params.forEach(p => push(p.param_type))
  push(ret?.param_type)
  return out
}

/**
 * 生成 TypeScript 模板
 * @param {object} f { api_name, function_label, function_type, class_name, rdfs_comment, version_no }
 * @param {Array}  params 入参 [{ param_name, param_type, param_desc }]
 * @param {object} ret 返回值 { param_type, param_desc }
 */
function buildTs(f, params, ret) {
  const cls = f.class_name || toClassName(f.api_name)
  const retType = bareType(ret?.param_type) || 'void'
  const objects = collectObjectTypes(params, ret)
  const args = params.map(p => `${p.param_name || 'arg'}: ${bareType(p.param_type)}`).join(', ')
  const deco = decoratorOf(f.function_type)

  const lines = []
  lines.push('/**')
  lines.push(` * ${f.function_label || f.api_name}`)
  if (f.rdfs_comment) lines.push(` * ${f.rdfs_comment}`)
  lines.push(' *')
  lines.push(` * @generated 由新增函数向导生成 · ${f.version_no || 'v0.0.1'}`)
  lines.push(' */')
  lines.push(`import { ${deco} } from "${API_MODULE}";`, '')
  // 本体类型由「资源导入」注入为全局声明, 不需要 import
  if (objects.length) lines.push(`// 本体类型:${objects.join('、')}(在 IDE 左侧「资源导入」面板导入后获得补全与校验)`, '')

  // 非基础类型的返回值: 生成一个待补全的数据模型类型定义
  if (retType !== 'void' && !isObjectType(ret?.param_type) && !['string', 'number', 'boolean', 'any'].includes(retType)) {
    lines.push(`/** ${ret?.param_desc || '返回结果'} */`)
    lines.push(`export interface ${retType} {`)
    lines.push('  // TODO: 补全返回字段类型定义')
    lines.push('}', '')
  }

  lines.push(`export class ${cls} {`)
  params.forEach(p => { if (p.param_desc) lines.push(`  /** @param ${p.param_name} ${p.param_desc} */`) })
  lines.push(`  @${deco}()`)
  lines.push(`  public ${f.api_name}(${args}): ${retType} {`)
  lines.push('    // TODO: 在此实现业务逻辑')
  lines.push(`    throw new Error("Not implemented: ${f.api_name}");`)
  lines.push('  }')
  lines.push('}')
  return lines.join('\n')
}

/** 生成 Python 模板 */
function buildPy(f, params, ret) {
  const cls = f.class_name || toClassName(f.api_name)
  const method = toSnakeCase(f.api_name)
  const retType = pyType(ret?.param_type) || 'None'
  const objects = collectObjectTypes(params, ret)
  const args = ['self', ...params.map(p => `${toSnakeCase(p.param_name || 'arg')}: "${pyType(p.param_type)}"`)].join(', ')

  const lines = []
  lines.push('"""')
  lines.push(`${f.function_label || f.api_name}`)
  if (f.rdfs_comment) lines.push(f.rdfs_comment)
  lines.push('')
  lines.push(`@generated 由新增函数向导生成 · ${f.version_no || 'v0.0.1'}`)
  lines.push('"""')
  lines.push('from typing import Any')
  if (objects.length) lines.push(`from ontology.objects import ${objects.join(', ')}`)
  lines.push('', '')
  lines.push(`class ${cls}:`)
  lines.push(`    @${decoratorOf(f.function_type)}()`)
  lines.push(`    def ${method}(${args}) -> "${retType}":`)
  lines.push('        """')
  lines.push(`        ${f.rdfs_comment || f.function_label || f.api_name}`)
  params.forEach(p => {
    if (p.param_desc) lines.push(`        :param ${toSnakeCase(p.param_name || 'arg')}: ${p.param_desc}`)
  })
  if (ret?.param_desc) lines.push(`        :return: ${ret.param_desc}`)
  lines.push('        """')
  lines.push('        # TODO: 在此实现业务逻辑')
  lines.push(`        raise NotImplementedError("${method}")`)
  return lines.join('\n')
}

/**
 * 按语言生成初始模板代码
 * @param {number} language 1=Python 2=TypeScript
 */
export function buildTemplateCode(language, f, params = [], ret = null) {
  return Number(language) === 1 ? buildPy(f, params, ret) : buildTs(f, params, ret)
}

/** 语言 → 文件后缀 */
export function extOf(language) { return Number(language) === 1 ? 'py' : 'ts' }
