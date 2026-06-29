-- =============================================================
-- 类型类(Type Classes)模块重构 — 升级版需求(260625)— PostgreSQL
-- JSON 列用 TEXT、布尔标记用 SMALLINT(0/1)与 SQLite 方言对齐,Mapper 统一处理。
-- =============================================================

DROP TABLE IF EXISTS ont_type_class;

CREATE TABLE IF NOT EXISTS ont_dic_type_class (
  id          VARCHAR(64)  PRIMARY KEY,
  enum_name   VARCHAR(64)  NOT NULL,
  code        VARCHAR(64)  NOT NULL,
  name        VARCHAR(128) NOT NULL,
  sort_no     INTEGER      NOT NULL DEFAULT 0,
  status      SMALLINT     NOT NULL DEFAULT 1,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(enum_name, code)
);
CREATE INDEX IF NOT EXISTS idx_tcdic_enum ON ont_dic_type_class(enum_name);

CREATE TABLE IF NOT EXISTS ont_type_class_category_dict (
  category_code            VARCHAR(64)  PRIMARY KEY,
  icon                     VARCHAR(64),
  color                    VARCHAR(20),
  category_name_cn         VARCHAR(64)  NOT NULL,
  global_allow_apply_types TEXT         NOT NULL DEFAULT '[]',
  source_type              VARCHAR(32)  NOT NULL DEFAULT 'platform_built',
  sort_weight              INTEGER      NOT NULL DEFAULT 999,
  description              VARCHAR(512),
  created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_type_class (
  id                   VARCHAR(46)  PRIMARY KEY,
  category_code        VARCHAR(64)  NOT NULL,
  icon                 VARCHAR(64),
  color                VARCHAR(20),
  name_prefix          VARCHAR(128) NOT NULL,
  name_template        VARCHAR(256),
  name_cn_base         VARCHAR(64)  NOT NULL,
  source_type          VARCHAR(32)  NOT NULL DEFAULT 'platform_built',
  group_tag            VARCHAR(64),
  allow_apply_types    TEXT         NOT NULL DEFAULT '[]',
  allow_multi_bind     SMALLINT     NOT NULL DEFAULT 0,
  is_array_value       SMALLINT     NOT NULL DEFAULT 0,
  system_protected     SMALLINT     NOT NULL DEFAULT 0,
  param_type           VARCHAR(32)  NOT NULL DEFAULT 'text',
  frontend_component   VARCHAR(64)  NOT NULL DEFAULT 'text_input',
  param_options_json   TEXT,
  param_validator_json TEXT,
  param_desc           VARCHAR(512),
  demo_value           TEXT,
  depend_on_meta_ids   TEXT         NOT NULL DEFAULT '[]',
  description          VARCHAR(1024),
  replacement_meta_id  VARCHAR(46),
  is_deprecated        SMALLINT     NOT NULL DEFAULT 0,
  deprecated_reason    VARCHAR(512),
  support_version_min  VARCHAR(16),
  current_version_no   INTEGER      NOT NULL DEFAULT 1,
  sort_weight          INTEGER      NOT NULL DEFAULT 999,
  create_user          VARCHAR(128),
  update_user          VARCHAR(128),
  created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(category_code, name_prefix)
);
CREATE INDEX IF NOT EXISTS idx_tc_category ON ont_type_class(category_code);
CREATE INDEX IF NOT EXISTS idx_tc_deprecated ON ont_type_class(is_deprecated);

CREATE TABLE IF NOT EXISTS ont_type_class_bind (
  id                  VARCHAR(46)  PRIMARY KEY,
  env                 VARCHAR(32)  NOT NULL DEFAULT 'prod',
  type_class_meta_id  VARCHAR(46)  NOT NULL,
  applicable_type     VARCHAR(32)  NOT NULL,
  property_owner_type VARCHAR(32),
  property_owner_id   VARCHAR(128),
  property_id         VARCHAR(128),
  link_type_id        VARCHAR(128),
  action_type_id      VARCHAR(128),
  suffix_custom       VARCHAR(128),
  value               TEXT,
  bind_deprecated     SMALLINT     NOT NULL DEFAULT 0,
  remark              VARCHAR(512),
  create_user         VARCHAR(128),
  update_user         VARCHAR(128),
  created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tcbind_meta ON ont_type_class_bind(type_class_meta_id);
CREATE INDEX IF NOT EXISTS idx_tcbind_apt  ON ont_type_class_bind(applicable_type);

-- ========================= 种子 =========================

INSERT INTO ont_dic_type_class(id, enum_name, code, name, sort_no) VALUES
 ('tcdic-applicable_type-property','ont_dic_tc_applicable_type','property','属性',10),
 ('tcdic-applicable_type-relation','ont_dic_tc_applicable_type','relation','关系',20),
 ('tcdic-applicable_type-action','ont_dic_tc_applicable_type','action','操作',30),
 ('tcdic-property_owner_type-object','ont_dic_tc_property_owner_type','object','对象类型',10),
 ('tcdic-property_owner_type-interface','ont_dic_tc_property_owner_type','interface','接口类型',20),
 ('tcdic-source_type-platform_built','ont_dic_tc_source_type','platform_built','平台内置',10),
 ('tcdic-source_type-user_custom','ont_dic_tc_source_type','user_custom','用户自定义',20),
 ('tcdic-param_type-boolean','ont_dic_tc_param_type','boolean','布尔',10),
 ('tcdic-param_type-rid','ont_dic_tc_param_type','rid','RID',20),
 ('tcdic-param_type-enum','ont_dic_tc_param_type','enum','枚举',30),
 ('tcdic-param_type-text','ont_dic_tc_param_type','text','文本',40),
 ('tcdic-param_type-numeric','ont_dic_tc_param_type','numeric','数值',50),
 ('tcdic-param_type-json','ont_dic_tc_param_type','json','JSON',60),
 ('tcdic-fc-switch','ont_dic_tc_frontend_component','switch','开关',10),
 ('tcdic-fc-single_select','ont_dic_tc_frontend_component','single_select','单选下拉',20),
 ('tcdic-fc-multi_select','ont_dic_tc_frontend_component','multi_select','多选下拉',30),
 ('tcdic-fc-text_input','ont_dic_tc_frontend_component','text_input','文本输入',40),
 ('tcdic-fc-number_input','ont_dic_tc_frontend_component','number_input','数字输入',50),
 ('tcdic-fc-json_editor','ont_dic_tc_frontend_component','json_editor','JSON编辑器',60),
 ('tcdic-fc-rid_selector','ont_dic_tc_frontend_component','rid_selector','RID选择器',70),
 ('tcdic-env-dev','ont_dic_tc_env','dev','开发',10),
 ('tcdic-env-test','ont_dic_tc_env','test','测试',20),
 ('tcdic-env-prod','ont_dic_tc_env','prod','生产',30);

INSERT INTO ont_type_class_category_dict(category_code, icon, color, category_name_cn, global_allow_apply_types, sort_weight, description) VALUES
 ('hubble','eye','#1890ff','可视化展示类','["property"]',10,'前端页面渲染样式控制:控制字段在页面如何展示(地图、时间、货币、标签样式)'),
 ('analyzer','search','#13c2c2','索引分析类','["property"]',20,'底层存储检索索引配置:决定字段搜索、过滤底层存储行为'),
 ('business','tag','#722ed1','业务语义类','["property","relation","action"]',30,'标准化业务字段标记:给字段打上统一业务语义标签'),
 ('custom','puzzle','#fa8c16','自定义类型类','["property","relation","action"]',40,'企业私有元标签扩展:企业内部自建私有语义标签'),
 ('geo','mapPin','#52c41a','地理坐标类','["property"]',50,'地理空间数据专用标记:管理经纬度、海拔等空间属性'),
 ('choropleth_map_config_id','map','#2f54eb','等值热力地图类','["property"]',60,'区域热力分层地图专用标记:给区域编码属性配置内置地理边界'),
 ('vertex','network','#eb2f96','知识图谱类','["property","relation"]',70,'知识图谱可视化与规则配置:控制节点、链路合并、事件等级、阈值告警'),
 ('timeseries','clock','#fa541c','时间序列类','["property","relation"]',80,'Quiver 时序图表专属配置:定义时序唯一 ID、度量、单位、插值规则'),
 ('hierarchy','layers','#faad14','层级导航类','["relation"]',90,'业务层级关系定义:配置父子节点关联,自动生成面包屑、树形结构'),
 ('hubble-oe','mousePointer','#1677ff','对象操作展示类','["action"]',100,'操作按钮前端渲染控制:管控操作按钮显隐、动态数据集、权限绑定'),
 ('actions','zap','#f5222d','通用操作能力类','["action"]',110,'动作执行内置逻辑赋能:生成 UUID、预填充当前用户、执行后跳转');

INSERT INTO ont_type_class(id, category_code, name_prefix, name_template, name_cn_base, allow_apply_types, param_type, frontend_component, param_options_json, demo_value, description, support_version_min, sort_weight) VALUES
 ('type-class-hubble-icon','hubble','icon',NULL,'图标','["property"]','text','text_input',NULL,'shturl.cc/WonxnSlV8HkgNg','标识属性值为图标 URL,用于展示对象自定义图标','7.20',10),
 ('type-class-analyzer-not_analyzed','analyzer','not_analyzed',NULL,'不分词','["property"]','text','text_input',NULL,'-','禁止 ES 对属性分词,适用于含特殊符号、唯一标识类字段','7.0',30),
 ('type-class-analyzer-analyzer_name','analyzer','analyzer_name',NULL,'指定分词器','["property"]','enum','single_select','[{"value":"simple","label_cn":"简单分词器"},{"value":"standard","label_cn":"标准分词器"},{"value":"whitespace","label_cn":"空格分词器"},{"value":"french","label_cn":"法语分词器"},{"value":"japanese","label_cn":"日语分词器"},{"value":"arabic","label_cn":"阿拉伯语分词器"}]','standard','为属性指定 ES 内置分词器,适配多语言文本检索场景','7.0',40),
 ('type-class-geo-altitude','geo','altitude',NULL,'海拔','["property"]','numeric','number_input',NULL,'100.5','存储地理对象海拔高度,用于地图 3D 模式渲染展示','7.10',50),
 ('type-class-vertex-link_merge','vertex','link_merge',NULL,'链接合并','["property"]','text','text_input',NULL,'-','将当前对象作为中介节点隐藏,直接展示二级关联关系,简化图谱链路','7.20',60),
 ('type-class-vertex-event_intent','vertex','event_intent','event_intent.<intent_>','事件意图','["property"]','enum','single_select','[{"value":"danger","label_cn":"危险"},{"value":"warning","label_cn":"预警"},{"value":"major","label_cn":"重大"},{"value":"success","label_cn":"正常"}]','danger','定义事件 / 告警颜色与严重等级,配置于事件对象主键','7.20',80),
 ('type-class-vertex-min','vertex','min',NULL,'阈值最小值','["property"]','numeric','number_input',NULL,'10','时间序列数值低于该值时触发图谱告警','7.20',120),
 ('type-class-vertex-max','vertex','max',NULL,'阈值最大值','["property"]','numeric','number_input',NULL,'100','时间序列数值超出该值时触发图谱告警','7.20',130),
 ('type-class-vertex-link_merge_incoming','vertex','link_merge_incoming',NULL,'入向链接合并','["relation"]','text','text_input',NULL,'-','仅对当前关系的入向链路执行节点合并,简化图谱结构','7.20',190),
 ('type-class-timeseries-timeseries_id','timeseries','timeseries_id',NULL,'时间序列唯一ID','["property"]','text','text_input',NULL,'-','时间序列全局唯一标识,是 Quiver 识别序列的核心必填配置','7.10',220),
 ('type-class-timeseries-is_enum','timeseries','timeseries_is_enum',NULL,'是否枚举序列','["property"]','boolean','switch',NULL,'false','标记当前时间序列为枚举值类型,适配分类序列场景','7.30',270),
 ('type-class-hierarchy-parent','hierarchy','parent',NULL,'父级节点','["relation"]','text','text_input',NULL,'-','定义关系层级方向,自动生成页面面包屑导航,支撑层级浏览','7.0',380),
 ('type-class-hubbleoe-hide_action','hubble-oe','hide-action',NULL,'隐藏操作按钮','["action"]','boolean','switch',NULL,'true','在对象视图、对象浏览器中隐藏指定操作,不展示在下拉菜单','7.10',390),
 ('type-class-actions-generate_uuid','actions','generate_uuid',NULL,'生成UUID','["action"]','text','text_input',NULL,'-','自动将操作字符串参数替换为全局唯一 UUID','7.10',420);

INSERT INTO ont_type_class(id, category_code, name_prefix, name_cn_base, allow_apply_types, param_type, frontend_component, demo_value, is_deprecated, deprecated_reason, description, support_version_min, sort_weight) VALUES
 ('type-class-hubble-media_url','hubble','media_url','媒体链接展示','["property"]','text','text_input','shturl.cc/A0R7ih1IYwKhqqp',1,'新版对象视图已原生支持媒体展示,无需配置','旧版媒体渲染能力,仅适配老版 Object 视图','7.0',900),
 ('type-class-geo-latitude','geo','latitude','纬度','["property"]','numeric','number_input','39.9042',1,'独立纬度字段已弃用,统一使用 geohash 存储经纬度','旧版独立纬度字段配置','7.0',1040);

INSERT INTO ont_type_class_bind(id, type_class_meta_id, applicable_type, property_owner_type, property_owner_id, property_id, value, remark) VALUES
 ('bind-demo-icon-1','type-class-hubble-icon','property','object','class-00000000-0000-0000-0000-000000000001','iconUrl','shturl.cc/icon1','水文测站图标'),
 ('bind-demo-altitude-1','type-class-geo-altitude','property','object','class-00000000-0000-0000-0000-000000000001','altitude','120.5','测站海拔');
INSERT INTO ont_type_class_bind(id, type_class_meta_id, applicable_type, link_type_id, value, remark) VALUES
 ('bind-demo-parent-1','type-class-hierarchy-parent','relation','link-types-demo-001',NULL,'河流层级父子关系');
INSERT INTO ont_type_class_bind(id, type_class_meta_id, applicable_type, action_type_id, value, remark) VALUES
 ('bind-demo-hide-1','type-class-hubbleoe-hide_action','action','action-demo-001','true','隐藏导出按钮');
