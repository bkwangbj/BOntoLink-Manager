/*
  类型类渲染解析器(地基,P1–P3 共用)。
  输入:后端 /api/tc-render/resolve 返回的 payload
        { classId, properties:[{property_id,api_name,display_name,data_type,typeClasses:[{category_code,name_prefix,name_template,value}]}], links:[{...}] }
  输出:各域「渲染指令」对象,渲染面(时序图表 / GraphG6 图谱 / 事件时间轴 / hubble)直接消费,不再各自解析 JSON。

  说明:类型类 value 已被后端解析为对象(纯标记型为 null)。
*/

function tcByPrefix(prop, prefix) {
  return (prop.typeClasses || []).find(t => t.name_prefix === prefix) || null
}
function anyByPrefix(props, prefix) {
  for (const p of props) { const t = tcByPrefix(p, prefix); if (t) return { prop: p, tc: t } }
  return null
}
const val = tc => (tc && tc.value && typeof tc.value === 'object') ? tc.value : {}

/* —— 时序域:series 样式 + 单位 + 枚举/倒置 + 插值 —— */
export function resolveTimeseries(payload) {
  const props = payload?.properties || []
  const series = []
  for (const p of props) {
    const measure = tcByPrefix(p, 'timeseries_measure')
    const units = tcByPrefix(p, 'timeseries_units')
    const isEnum = tcByPrefix(p, 'timeseries_is_enum')
    const inverted = tcByPrefix(p, 'timeseries_is_value_inverted')
    const depth = tcByPrefix(p, 'timeseries_depth_units')
    // 只有被标为度量(或带单位/枚举/深度)的属性才成为一条 series
    if (!measure && !units && !isEnum && !depth) continue
    const mv = val(measure), uv = val(units), dv = val(depth)
    series.push({
      property_id: p.property_id, api_name: p.api_name, display_name: p.display_name,
      color: mv.default_color || '',
      lineType: mv.line_type || 'solid',
      axis: mv.axis_position || 'left',
      unit: uv.unit_text || '',
      ratio: uv.conversion_ratio != null ? uv.conversion_ratio : 1,
      decimals: uv.decimal_places != null ? uv.decimal_places : 2,
      isEnum: !!(val(isEnum).is_enum),
      inverted: !!(val(inverted).is_inverted),
      depthUnit: dv.depth_unit_text || '',
      depthRatio: dv.depth_conversion_ratio != null ? dv.depth_conversion_ratio : 1,
      depthDecimals: dv.depth_decimal_places != null ? dv.depth_decimal_places : 2
    })
  }
  const interp = anyByPrefix(props, 'timeseries_internal_interpolation')
  const dep = anyByPrefix(props, 'timeseries_is_deprecated')
  return {
    series,
    interpolation: interp ? (val(interp.tc).interpolation_type || 'linear') : null,
    deprecatedFilter: !!dep,       // 存在则前端应过滤已弃用序列
    hasTimeseries: series.length > 0
  }
}

/* —— 知识图谱域(vertex):节点分组/等级/阈值/看板 + 链路合并/边可见 —— */
export function resolveVertex(payload) {
  const props = payload?.properties || []
  const links = payload?.links || []
  const subtypeHit = anyByPrefix(props, 'component_subtype')
  const intentHit = props.map(p => (p.typeClasses || []).find(t => t.name_prefix.startsWith('event_intent'))).find(Boolean)
  const keyHit = props.map(p => (p.typeClasses || []).find(t => t.name_prefix.startsWith('key_measure'))).find(Boolean)
  const evValHit = anyByPrefix(props, 'event_value')
  const evUnitHit = props.map(p => (p.typeClasses || []).find(t => t.name_prefix.startsWith('event_value_unit'))).find(Boolean)
  const thMeasureHit = props.map(p => (p.typeClasses || []).find(t => t.name_prefix.startsWith('threshold_measure'))).find(Boolean)
  const thExceedHit = props.map(p => (p.typeClasses || []).find(t => t.name_prefix.startsWith('threshold_exceed_intent'))).find(Boolean)
  const highHit = anyByPrefix(props, 'threshold_high_limit')
  const lowHit = anyByPrefix(props, 'threshold_low_limit')
  const minHit = anyByPrefix(props, 'min')
  const maxHit = anyByPrefix(props, 'max')

  const eventProps = props.filter(p => tcByPrefix(p, 'event_property')).map(p => p.property_id)

  return {
    subtype: subtypeHit ? val(subtypeHit.tc) : null,                 // {subtype_code,group_color,group_name}
    intent: intentHit ? val(intentHit) : null,                       // {intent_type,color,blink,priority}
    keyMeasure: keyHit ? val(keyHit) : null,                         // {measure_code,measure_name,unit,precision,trend_enable}
    eventValue: evValHit ? { property_id: evValHit.prop.property_id, unit: evUnitHit ? val(evUnitHit) : null } : null,
    eventProps,
    thresholds: {
      measure: thMeasureHit ? val(thMeasureHit) : null,             // {measure_code,measure_name,bind_series_id}
      highProperty: highHit ? highHit.prop.property_id : null,
      lowProperty: lowHit ? lowHit.prop.property_id : null,
      min: minHit ? (val(minHit.tc).min_value ?? null) : null,
      max: maxHit ? (val(maxHit.tc).max_value ?? null) : null,
      exceedIntent: thExceedHit ? val(thExceedHit) : null
    },
    // 中介节点合并:link_merge 挂在中介对象主键属性上(对象级)→ 该对象类型隐藏、桥接上下游
    isMediator: props.some(p => tcByPrefix(p, 'link_merge')),
    // 链接级(挂在链接上):边可见 / 单向合并(需链接绑定数据,category-map 仅含属性绑定)
    edgeVisible: links.some(t => t.name_prefix === 'component'),
    mergeIncoming: links.some(t => t.name_prefix === 'link_merge_incoming'),
    mergeOutgoing: links.some(t => t.name_prefix === 'link_merge_outgoing')
  }
}

/* —— 事件域:业务类型/等级样式映射 + 闭环规则 —— */
export function resolveEvent(payload) {
  const props = payload?.properties || []
  const typeHit = anyByPrefix(props, 'event_type')
  const levelHit = anyByPrefix(props, 'event_level')
  const closedHit = anyByPrefix(props, 'is_closed')
  const lv = levelHit ? val(levelHit.tc) : {}
  return {
    typeStyleMap: typeHit ? (val(typeHit.tc).type_style_map || {}) : null,
    levelStyleMap: levelHit ? (lv.level_style_map || {}) : null,
    enableBadge: !!lv.enable_badge,
    sortByPriority: !!lv.sort_by_priority,
    isClosed: closedHit ? val(closedHit.tc) : null                   // {closed_opacity,default_filter_closed,hide_level_highlight}
  }
}

/* —— 实例级阈值越限:返回可直接对每行实例判定越限的字段名 + 限值 —— */
export function resolveThreshold(payload) {
  const props = payload?.properties || []
  const measure = props.find(p => tcByPrefix(p, 'threshold_measure'))
  if (!measure) return null
  const high = props.find(p => tcByPrefix(p, 'threshold_high_limit'))
  const low = props.find(p => tcByPrefix(p, 'threshold_low_limit'))
  const minHit = props.find(p => tcByPrefix(p, 'min'))
  const maxHit = props.find(p => tcByPrefix(p, 'max'))
  const exHit = props.map(p => (p.typeClasses || []).find(t => t.name_prefix.startsWith('threshold_exceed_intent'))).find(Boolean)
  const mv = val(tcByPrefix(measure, 'threshold_measure'))
  return {
    measureField: measure.api_name,                                 // 当前测量值所在字段(逐行读)
    measureName: mv.measure_name || measure.display_name || measure.api_name,
    highField: high ? high.api_name : null,                          // 高限值字段(逐行读,可为空)
    lowField: low ? low.api_name : null,                             // 低限值字段
    max: maxHit ? (val(tcByPrefix(maxHit, 'max')).max_value ?? null) : null,   // 硬高限(常量,兜底)
    min: minHit ? (val(tcByPrefix(minHit, 'min')).min_value ?? null) : null,   // 硬低限(常量,兜底)
    exceedColor: exHit ? (val(exHit).color || '#f53f3f') : '#f53f3f'
  }
}

/* —— 对象视图(hubble):图标属性 —— */
export function resolveHubble(payload) {
  const props = payload?.properties || []
  const icons = props.filter(p => tcByPrefix(p, 'icon')).map(p => ({ property_id: p.property_id, api_name: p.api_name }))
  return { iconProps: icons, iconField: icons.length ? icons[0].api_name : null }
}

/* 一站式:返回四域渲染指令 */
export function resolveAll(payload) {
  return {
    timeseries: resolveTimeseries(payload),
    vertex: resolveVertex(payload),
    event: resolveEvent(payload),
    hubble: resolveHubble(payload)
  }
}
