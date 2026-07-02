-- =============================================================
-- 类型类实现(2.x)全量种子 — 补齐文档「参数标准对照表」剩余类型类。
-- 跳过 V8/V10 已存在的(icon/altitude/parent/link_merge/event_intent/min/max/
--   link_merge_incoming/timeseries_id/timeseries_is_enum/timeseries_measure/component_subtype)。
-- 多字段 param_json Schema 驱动前端动态表单;两方言共用(纯 INSERT/TEXT)。
-- 列:id,category_code,name_prefix,name_template,name_cn_base,allow_apply_types,allow_multi_bind,
--     system_protected,param_type,frontend_component,param_json,demo_value,description,support_version_min,sort_weight,is_deprecated,deprecated_reason
-- =============================================================

-- ---------- 时序(timeseries)剩余 6 ----------
INSERT INTO ont_type_class(id,category_code,name_prefix,name_template,name_cn_base,allow_apply_types,allow_multi_bind,system_protected,param_type,frontend_component,param_json,demo_value,description,support_version_min,sort_weight,is_deprecated,deprecated_reason) VALUES
 ('type-class-ts-units','timeseries','timeseries_units',NULL,'序列数值单位','["property"]',0,0,'json','json_editor',
  '{"unit_text":{"type":"string","default":"","description":"单位展示文本,如 m、m³/h、℃、kPa;用于坐标轴/悬浮提示单位标注"},"conversion_ratio":{"type":"number","default":1,"description":"单位换算倍率,展示值=原始值×倍率;仅前端展示,不改原始数据"},"decimal_places":{"type":"number","default":2,"description":"数值展示保留小数位数"}}',
  '{"unit_text":"m³/h","conversion_ratio":0.001,"decimal_places":2}',
  '定义普通度量字段的单位、换算倍率与精度,支持按数值范围自动切换量程;与 timeseries_measure 成对使用。','7.10',221,0,NULL),
 ('type-class-ts-interp','timeseries','timeseries_internal_interpolation',NULL,'内部插值方式','["property"]',0,0,'enum','single_select',
  '{"interpolation_type":{"type":"enum","default":"linear","enum":["linear","step","forward","backward","none"],"description":"数据断点插值算法:linear(线性)/step(阶梯)/forward(前向)/backward(后向)/none(不插值);仅影响图表展示"}}',
  '{"interpolation_type":"step"}',
  '配置时序数据断点的插值补全算法,优化图表展示连续性;一个时序实体最多一个,全局生效。','7.10',222,0,NULL),
 ('type-class-ts-root','timeseries','timeseries_root_object_id',NULL,'序列根对象 ID','["property","relation"]',1,0,'rid','rid_selector',
  NULL,'-',
  '标记字段存储上级业务归属外键 ID,支撑按根对象筛选、批量加载、权限管控;可绑定多个(如测站 ID + 流域 ID)。','7.10',223,0,NULL),
 ('type-class-ts-isdep','timeseries','timeseries_is_deprecated',NULL,'序列是否弃用','["property"]',0,0,'boolean','switch',
  '{"is_deprecated":{"type":"boolean","default":false,"description":"序列是否弃用;true 时前端时序列表/图表默认隐藏,仅后台归档可见,不删除原始数据"}}',
  '{"is_deprecated":true}',
  '标记时序实体为废弃数据,检索列表/图表默认过滤;底层不删除历史数据,用于设备下线归档场景。','7.10',224,1,'示例弃用样例:历史临时测点归档'),
 ('type-class-ts-inverted','timeseries','timeseries_is_value_inverted',NULL,'Y 轴数值反转','["property"]',0,0,'boolean','switch',
  '{"is_inverted":{"type":"boolean","default":false,"description":"Y 轴数值是否倒置;true 时数值越大位置越靠下,适配井深/埋深/钻探深度场景"}}',
  '{"is_inverted":true}',
  '控制 Y 轴数值方向,开启后数值越大越靠下,适配深度、埋深等数值倒置的业务场景。','7.10',225,0,NULL),
 ('type-class-ts-depthunits','timeseries','timeseries_depth_units',NULL,'深度单位','["property"]',0,0,'json','json_editor',
  '{"depth_unit_text":{"type":"string","default":"","description":"深度单位展示文本,如 m、km、ft;与通用单位分离避免混淆"},"depth_conversion_ratio":{"type":"number","default":1,"description":"深度单位换算倍率"},"depth_decimal_places":{"type":"number","default":2,"description":"深度数值展示保留小数位数"}}',
  '{"depth_unit_text":"km","depth_conversion_ratio":0.001,"depth_decimal_places":3}',
  '深度场景专属单位配置,独立管理深度数据的换算规则与展示精度,与 timeseries_units 分离。','7.10',226,0,NULL);

-- ---------- 事件(event,归属 timeseries 大类)9 ----------
INSERT INTO ont_type_class(id,category_code,name_prefix,name_template,name_cn_base,allow_apply_types,allow_multi_bind,system_protected,param_type,frontend_component,param_json,demo_value,description,support_version_min,sort_weight,is_deprecated,deprecated_reason) VALUES
 ('type-class-ev-id','timeseries','event_id',NULL,'全局事件唯一 ID','["property"]',0,0,'rid','rid_selector',NULL,'-','纯标记型,标注字段为全局事件唯一主键;引擎据此建主键索引,用于幂等校验、关联绑定、全局检索。','7.10',300,0,NULL),
 ('type-class-ev-start','timeseries','event_start_time',NULL,'事件开始时间','["property"]',0,0,'text','text_input',NULL,'-','纯标记型,标注字段为事件时间区间起始点;引擎建时间范围索引,作为时间查询/时间轴渲染的默认左边界。','7.10',301,0,NULL),
 ('type-class-ev-end','timeseries','event_end_time',NULL,'事件结束时间','["property"]',0,0,'text','text_input',NULL,'-','纯标记型,标注字段为事件时间区间结束点;与 event_start_time 配对构成完整区间,支撑时长计算。','7.10',302,0,NULL),
 ('type-class-ev-desc','timeseries','event_description',NULL,'事件描述','["property"]',0,0,'text','text_input',NULL,'-','纯标记型,标注字段为事件业务语义展示字段;事件列表、时间轴弹窗、告警通知默认取该字段作为标题。','7.10',303,0,NULL),
 ('type-class-ev-type','timeseries','event_type',NULL,'业务类型','["property"]',0,0,'json','json_editor',
  '{"type":"object","properties":{"type_style_map":{"type":"object","description":"业务类型编码到样式的映射","additionalProperties":{"type":"object","properties":{"label":{"type":"string","description":"类型显示名称"},"color":{"type":"string","description":"展示色值"}},"required":["label","color"]}}},"required":["type_style_map"]}',
  '{"type_style_map":{"equipment_fault":{"label":"设备故障","color":"#ef4444"},"waybill_delay":{"label":"运单延误","color":"#f59e0b"},"server_alert":{"label":"服务器告警","color":"#8b5cf6"}}}',
  '配置型,区分不同业务场景的事件,为每种业务类型配置独立展示颜色/标签样式,实现时间轴、列表的分类视觉区分。','7.10',304,0,NULL),
 ('type-class-ev-level','timeseries','event_level',NULL,'事件严重等级','["property"]',0,0,'json','json_editor',
  '{"type":"object","properties":{"level_style_map":{"type":"object","description":"等级枚举值到样式的映射","additionalProperties":{"type":"object","properties":{"color":{"type":"string"},"priority":{"type":"number"},"blink":{"type":"boolean"}},"required":["color","priority","blink"]}},"enable_badge":{"type":"boolean","description":"是否显示等级角标"},"sort_by_priority":{"type":"boolean","description":"是否按优先级默认排序"}},"required":["level_style_map","enable_badge","sort_by_priority"]}',
  '{"level_style_map":{"info":{"color":"#10b981","priority":1,"blink":false},"warning":{"color":"#f59e0b","priority":2,"blink":false},"error":{"color":"#ef4444","priority":3,"blink":false},"critical":{"color":"#7c3aed","priority":4,"blink":true}},"enable_badge":true,"sort_by_priority":true}',
  '配置型,定义事件严重等级的展示规则,为每个等级配置颜色/优先级/闪烁高亮,支撑分级告警、优先级排序、视觉突出。','7.10',305,0,NULL),
 ('type-class-ev-closed','timeseries','is_closed',NULL,'闭环状态','["property"]',0,0,'json','json_editor',
  '{"type":"object","properties":{"closed_opacity":{"type":"number","description":"已闭环事件透明度,0-1"},"default_filter_closed":{"type":"boolean","description":"列表默认过滤已闭环"},"hide_level_highlight":{"type":"boolean","description":"已闭环关闭等级高亮"}},"required":["closed_opacity","default_filter_closed","hide_level_highlight"]}',
  '{"closed_opacity":0.5,"default_filter_closed":true,"hide_level_highlight":true}',
  '配置型,定义事件闭环状态的交互与展示规则,区分已闭环/未闭环样式,支持默认过滤已闭环、降透明度展示。','7.10',306,0,NULL),
 ('type-class-ev-root','timeseries','event_root_object_id',NULL,'根对象归属','["relation"]',0,0,'rid','rid_selector',NULL,'-','纯标记型,挂载在事件指向业务实体的链接上,标注该链接为事件的业务根对象归属,支撑反向查询/按维度聚合。','7.10',307,0,NULL),
 ('type-class-ev-linkedseries','timeseries','event_linked_series_id',NULL,'关联系列','["relation"]',0,0,'json','json_editor',
  '{"type":"object","properties":{"link_type":{"type":"string","enum":["metric","log","event"],"description":"关联序列类型"},"auto_jump":{"type":"boolean","description":"是否自动跳转"},"offset_before":{"type":"number","description":"向前偏移秒数"},"offset_after":{"type":"number","description":"向后偏移秒数"},"auto_zoom":{"type":"boolean","description":"是否自动缩放时间轴"}},"required":["link_type","auto_jump","offset_before","offset_after","auto_zoom"]}',
  '{"link_type":"metric","auto_jump":true,"offset_before":300,"offset_after":300,"auto_zoom":true}',
  '配置型,挂载在事件指向时序序列的链接上,定义事件与时序数据的联动规则(自动跳转、前后偏移、时间轴叠加)。','7.10',308,0,NULL);

-- ---------- 知识图谱(vertex)剩余 10 ----------
INSERT INTO ont_type_class(id,category_code,name_prefix,name_template,name_cn_base,allow_apply_types,allow_multi_bind,system_protected,param_type,frontend_component,param_json,demo_value,description,support_version_min,sort_weight,is_deprecated,deprecated_reason) VALUES
 ('type-class-vx-component','vertex','component',NULL,'图谱边组件','["relation"]',0,0,'text','text_input',NULL,'-','纯标记型,标识该链接需在知识图谱中渲染为可见边;未挂载的业务链接默认不出现在图谱,用于过滤非核心关系。','7.20',400,0,NULL),
 ('type-class-vx-mergeout','vertex','link_merge_outgoing',NULL,'出向链接合并','["relation"]',0,0,'text','text_input',NULL,'-','纯标记型,仅对当前出向链路执行节点合并,入向链路保留节点;简化图谱结构。','7.20',401,0,NULL),
 ('type-class-vx-eventvalue','vertex','event_value',NULL,'事件量化数值','["property"]',0,0,'text','text_input',NULL,'-','纯标记型,标注该属性为事件核心量化数值,自动展示在图谱事件卡片醒目位置。','7.20',402,0,NULL),
 ('type-class-vx-valueunit','vertex','event_value_unit','event_value_unit.<unit_>','事件数值单位','["property"]',0,0,'json','json_editor',
  '{"unit_code":{"type":"string","description":"单位编码"},"unit_name":{"type":"string","description":"单位显示名称"},"precision":{"type":"number","description":"保留小数位数"}}',
  '{"unit_code":"celsius","unit_name":"℃","precision":1}',
  '配置型,搭配 event_value 使用,定义数值的度量维度与单位,统一展示格式。','7.20',403,0,NULL),
 ('type-class-vx-eventprop','vertex','event_property',NULL,'事件详情字段','["property"]',1,0,'text','text_input',NULL,'-','纯标记型,指定该字段展示在图谱事件详情卡片中,灵活配置事件展示信息,新增字段只需加类型类。','7.20',404,0,NULL),
 ('type-class-vx-thmeasure','vertex','threshold_measure','threshold_measure.<measure_>','阈值核心度量','["property"]',0,0,'json','json_editor',
  '{"measure_code":{"type":"string","description":"度量指标编码"},"measure_name":{"type":"string","description":"指标显示名称"},"bind_series_id":{"type":"string","description":"绑定的时序序列 ID"}}',
  '{"measure_code":"winding_temp","measure_name":"绕组温度","bind_series_id":"ts_001"}',
  '配置型,指定对象用于阈值监控的核心度量指标,是整个阈值体系的主入口,绑定对应时序序列。','7.20',405,0,NULL),
 ('type-class-vx-thhigh','vertex','threshold_high_limit',NULL,'上限阈值字段','["property"]',0,0,'text','text_input',NULL,'-','纯标记型,标注该属性存储阈值上限值,引擎自动读取字段值作为超限判断基准;每实例可配不同上限。','7.20',406,0,NULL),
 ('type-class-vx-thlow','vertex','threshold_low_limit',NULL,'下限阈值字段','["property"]',0,0,'text','text_input',NULL,'-','纯标记型,标注该属性存储阈值下限值,用于低限超限判断;逻辑与上限对称。','7.20',407,0,NULL),
 ('type-class-vx-thintent','vertex','threshold_exceed_intent','threshold_exceed_intent.<intent_>','超限告警语义','["property"]',0,0,'json','json_editor',
  '{"intent_type":{"type":"enum","enum":["info","warning","error","critical"],"description":"等级枚举"},"color":{"type":"string","description":"节点展示色"},"blink":{"type":"boolean","description":"是否闪烁高亮"},"priority":{"type":"number","description":"优先级数值"}}',
  '{"intent_type":"critical","color":"#ef4444","blink":true,"priority":4}',
  '配置型,定义阈值突破后自动生成的告警节点的等级/颜色/样式,与 event_intent 语义对齐。','7.20',408,0,NULL),
 ('type-class-vx-keymeasure','vertex','key_measure','key_measure.<measure_>','核心度量指标','["property"]',0,0,'json','json_editor',
  '{"measure_code":{"type":"string","description":"指标编码"},"measure_name":{"type":"string","description":"指标显示名称"},"unit":{"type":"string","description":"单位"},"precision":{"type":"number","description":"小数位数"},"trend_enable":{"type":"boolean","description":"是否显示迷你趋势图"}}',
  '{"measure_code":"run_temp","measure_name":"当前温度","unit":"℃","precision":1,"trend_enable":true}',
  '配置型,指定对象核心度量指标,自动展示在图谱节点主页看板顶部,作为节点核心业务数据概览。','7.20',409,0,NULL);
