/*
  分词器资源总表(文档 2.6.6):28 个核心分词器 + 22 个附加过滤器 = 50。
  role: 'core'(核心分词器,单选互斥) | 'filter'(附加过滤器,可多选叠加)
  scope: 'common'(常用+全量可见) | 'full'(仅全量模式可见)
  group: 分组标题(按技术属性归类)
*/
export const ANALYZERS = [
  // ===== 核心分词器 · 中文分词类 =====
  { en: 'ik_max_word', cn: '中文细粒度分词', role: 'core', scope: 'common', group: '中文分词', feat: '穷尽所有可能词组,词条数量多、召回率高', scene: '商品标题、知识库、全文检索(默认推荐)' },
  { en: 'ik_smart', cn: '中文智能分词', role: 'core', scope: 'common', group: '中文分词', feat: '粗粒度拆分,仅输出完整语义词组,准确率高', scene: '政务办事、分类检索、精准排序' },
  { en: 'jieba', cn: '结巴分词', role: 'core', scope: 'common', group: '中文分词', feat: '轻量概率模型分词,支持新词识别,资源占用低', scene: '轻量中文系统、内容标签、中小规模检索' },
  { en: 'hanlp', cn: 'NLP 智能分词', role: 'core', scope: 'common', group: '中文分词', feat: '基于自然语言模型,支持实体识别、词性标注', scene: '智能问答、语义检索、实体抽取' },
  { en: 'smartcn', cn: '官方中文分词', role: 'core', scope: 'full', group: '中文分词', feat: 'Lucene 原生内置轻量中文分词,零配置', scene: '简易中文检索、不想额外装插件' },
  { en: 'mmseg', cn: 'MMSEG 分词', role: 'core', scope: 'full', group: '中文分词', feat: '基于 MMSEG 算法,准确率高、分词结果稳定', scene: '传统中文检索、对分词一致性要求高' },
  { en: 'ansj', cn: 'Ansj 分词', role: 'core', scope: 'full', group: '中文分词', feat: '支持人名识别、词性标注、新词发现', scene: '文本分析、内容标签提取、NLP 预处理' },
  { en: 'jcseg', cn: 'Jcseg 分词', role: 'core', scope: 'full', group: '中文分词', feat: '轻量 Java 中文分词,支持同义词、拼音扩展', scene: 'Java 技术栈轻量检索、企业内部搜索' },
  // ===== 核心分词器 · 内置基础分词类 =====
  { en: 'standard', cn: '标准分词', role: 'core', scope: 'common', group: '内置基础分词', feat: '系统默认;英文按词拆、中文按单字拆', scene: '纯英文文档、简单英文检索' },
  { en: 'keyword', cn: '关键字精确匹配', role: 'core', scope: 'common', group: '内置基础分词', feat: '完全不拆分,整段作为单个词条', scene: '编码、证件号、枚举值等精确匹配' },
  { en: 'whitespace', cn: '空格分词', role: 'core', scope: 'full', group: '内置基础分词', feat: '仅按空格拆分,不做大小写转换', scene: '空格分隔的标签、结构化编码' },
  { en: 'simple', cn: '简单分词', role: 'core', scope: 'full', group: '内置基础分词', feat: '按非字母字符拆分,自动转小写', scene: '纯英文轻量检索、英文标签匹配' },
  { en: 'letter', cn: '字母分词', role: 'core', scope: 'full', group: '内置基础分词', feat: '按非字母字符切分,过滤数字符号', scene: '纯英文文献检索、纯文本英文内容' },
  { en: 'classic', cn: '经典英文分词', role: 'core', scope: 'full', group: '内置基础分词', feat: '优化英文缩写、所有格、域名、邮箱', scene: '英文官网、学术文献、英文资料检索' },
  { en: 'uax_url_email', cn: 'URL 邮箱分词', role: 'core', scope: 'full', group: '内置基础分词', feat: '标准分词增强版,识别 URL/邮箱/手机号', scene: '通讯录、网址库、客户信息检索' },
  { en: 'pattern', cn: '正则分词', role: 'core', scope: 'full', group: '内置基础分词', feat: '按自定义正则规则切分,高度灵活', scene: '特殊格式日志、编码规则文本' },
  { en: 'path_hierarchy', cn: '路径层级分词', role: 'core', scope: 'full', group: '内置基础分词', feat: '按 / 逐层拆分,生成各级父路径词条', scene: '文件路径、目录树、地址层级检索' },
  // ===== 核心分词器 · 多语言分词类 =====
  { en: 'icu_tokenizer', cn: 'ICU 国际分词', role: 'core', scope: 'full', group: '多语言分词', feat: '基于 ICU 标准的国际化分词,支持数十种语言', scene: '多语言混合文档、国际化站点' },
  { en: 'english', cn: '英文分词', role: 'core', scope: 'full', group: '多语言分词', feat: '英文专用分词 + 词形还原 + 停用词', scene: '英文全文检索、英文知识库' },
  { en: 'german', cn: '德语分词', role: 'core', scope: 'full', group: '多语言分词', feat: '德语专用分词 + 词干提取,适配复合词', scene: '德文内容检索、德语区业务系统' },
  { en: 'french', cn: '法语分词', role: 'core', scope: 'full', group: '多语言分词', feat: '法语专用分词 + 省略音处理', scene: '法文内容检索、法语区业务系统' },
  { en: 'spanish', cn: '西班牙语分词', role: 'core', scope: 'full', group: '多语言分词', feat: '西班牙语专用分词 + 词形还原', scene: '西班牙文内容检索、拉美业务系统' },
  { en: 'russian', cn: '俄语分词', role: 'core', scope: 'full', group: '多语言分词', feat: '俄语专用分词 + 词干提取,适配变格', scene: '俄文内容检索、独联体区业务系统' },
  { en: 'kuromoji', cn: '日语分词', role: 'core', scope: 'full', group: '多语言分词', feat: '日语专用分词,基于词典和语法规则', scene: '日文内容检索、日本区业务系统' },
  { en: 'nori', cn: '韩语分词', role: 'core', scope: 'full', group: '多语言分词', feat: '韩语专用分词,适配音节和词尾变化', scene: '韩文内容检索、韩国区业务系统' },
  { en: 'cjk', cn: '中日韩二元分词', role: 'core', scope: 'full', group: '多语言分词', feat: '中日韩文字按两字一组切分,简易通用', scene: '中日韩混合文本简易检索、快速上线' },
  { en: 'thai', cn: '泰语分词', role: 'core', scope: 'full', group: '多语言分词', feat: '泰语专用分词,适配无空格分隔特性', scene: '泰文内容检索、泰国区业务系统' },
  { en: 'arabic', cn: '阿拉伯语分词', role: 'core', scope: 'full', group: '多语言分词', feat: '阿拉伯语专用分词 + 归一化', scene: '阿拉伯文内容检索、中东区业务系统' },

  // ===== 附加过滤器 · 词条控制类 =====
  { en: 'stop', cn: '停用词过滤', role: 'filter', scope: 'common', group: '词条控制', feat: '过滤「的、了、the、of」等无意义助词', scene: '长文本检索,减少无效词条,提升性能' },
  { en: 'edge_ngram', cn: '前缀联想', role: 'filter', scope: 'common', group: '词条控制', feat: '从文本开头滑动切片,生成前缀词条', scene: '搜索框输入联想、自动补全、编码前缀' },
  { en: 'ngram', cn: '全字符模糊匹配', role: 'filter', scope: 'common', group: '词条控制', feat: '全位置滑动切片,生成任意位置字符片段', scene: '输入容错、错字匹配、模糊检索' },
  { en: 'length', cn: '长度过滤', role: 'filter', scope: 'full', group: '词条控制', feat: '过滤过长或过短的词条,可配置长度阈值', scene: '过滤无意义单字、超长乱码、异常词条' },
  { en: 'unique', cn: '去重', role: 'filter', scope: 'full', group: '词条控制', feat: '去除重复词条,保留唯一值', scene: '减少索引冗余、节省存储、提升性能' },
  { en: 'limit_token_count', cn: '词条数量限制', role: 'filter', scope: 'full', group: '词条控制', feat: '限制单文档最大词条数,超出截断', scene: '超长文本性能优化、防止大文档拖慢索引' },
  { en: 'reverse', cn: '字符串反转', role: 'filter', scope: 'full', group: '词条控制', feat: '将词条字符串前后反转', scene: '后缀模糊匹配、倒序检索、特殊编码' },
  // ===== 附加过滤器 · 高级处理类 =====
  { en: 'pinyin', cn: '拼音检索', role: 'filter', scope: 'common', group: '高级处理', feat: '中文转全拼 + 首字母,自动生成拼音词条', scene: '人名、药品名、商品名的拼音/首字母检索' },
  { en: 'synonym', cn: '同义词匹配', role: 'filter', scope: 'common', group: '高级处理', feat: '基于同义词典扩展近义词、行业术语、别名', scene: '行业知识库、术语匹配、别名检索' },
  { en: 'shingle', cn: '相邻词组生成', role: 'filter', scope: 'full', group: '高级处理', feat: '生成相邻单词的二元/三元词组,保留语序', scene: '短语匹配、提升语义相关性' },
  { en: 'word_delimiter', cn: '单词拆分', role: 'filter', scope: 'full', group: '高级处理', feat: '按大小写、数字、符号拆分复合词', scene: '产品型号、代码标识符、英文复合词检索' },
  { en: 'pattern_replace', cn: '正则替换', role: 'filter', scope: 'full', group: '高级处理', feat: '按正则表达式替换词条内容', scene: '特殊格式规整、敏感词替换、数据标准化' },
  { en: 'fingerprint', cn: '指纹生成', role: 'filter', scope: 'full', group: '高级处理', feat: '对词条排序去重后生成指纹字符串', scene: '文档去重、相似内容识别、重复数据聚类' },
  // ===== 附加过滤器 · 基础文本处理类 =====
  { en: 'lowercase', cn: '转小写', role: 'filter', scope: 'full', group: '基础文本处理', feat: '所有英文字词统一转小写', scene: '英文检索,忽略大小写差异' },
  { en: 'uppercase', cn: '转大写', role: 'filter', scope: 'full', group: '基础文本处理', feat: '所有英文字词统一转大写', scene: '特定编码、编号、序列号检索' },
  { en: 'trim', cn: '去空格', role: 'filter', scope: 'full', group: '基础文本处理', feat: '去除词条首尾多余空格', scene: '清洗脏数据、规整不规则文本' },
  { en: 'decimal_digit', cn: '数字归一化', role: 'filter', scope: 'full', group: '基础文本处理', feat: '将全角数字统一转为半角数字', scene: '中英文混排、全角字符数据清洗' },
  { en: 'asciifolding', cn: 'ASCII 归一化', role: 'filter', scope: 'full', group: '基础文本处理', feat: '重音/特殊拉丁字符转为普通 ASCII', scene: '多语言拉丁字符处理、法德语重音兼容' },
  { en: 'elision', cn: '省略音去除', role: 'filter', scope: 'full', group: '基础文本处理', feat: "去除 l'、d'、c' 等冠词缩写前缀", scene: '法语、意大利语等拉丁语系文本处理' },
  // ===== 附加过滤器 · 词形归一化类 =====
  { en: 'stemmer', cn: '通用词干提取', role: 'filter', scope: 'full', group: '词形归一化', feat: '多语言通用词干提取,running/ran → run', scene: '英文全文检索,大幅提升召回率' },
  { en: 'kstem', cn: '轻量词干提取', role: 'filter', scope: 'full', group: '词形归一化', feat: '英文轻量级词干算法,过度词干少、准确率高', scene: '高精度英文检索、专业文献' },
  { en: 'porter_stem', cn: '波特词干算法', role: 'filter', scope: 'full', group: '词形归一化', feat: '最经典的英文词干提取算法,兼容性强', scene: '传统英文搜索引擎、通用英文检索' }
]

export const CORES = ANALYZERS.filter(a => a.role === 'core')
export const FILTERS = ANALYZERS.filter(a => a.role === 'filter')
export const byEn = Object.fromEntries(ANALYZERS.map(a => [a.en, a]))

/* 核心分词器分组(全量模式下分组展示) */
export const CORE_GROUPS = ['中文分词', '内置基础分词', '多语言分词']
/* 附加过滤器分组 */
export const FILTER_GROUPS = ['词条控制', '高级处理', '基础文本处理', '词形归一化']

/* 固定示例文本 + 各分词器的演示拆分结果(mock,前端预览用) */
export const SAMPLE_TEXT = '夏季纯棉男士短袖 T 恤'
const CORE_SPLIT = {
  ik_max_word: ['夏季', '纯棉', '男士', '短袖', 'T 恤', '纯棉男士', '男士短袖'],
  ik_smart: ['夏季', '纯棉', '男士', '短袖 T 恤'],
  standard: ['夏', '季', '纯', '棉', '男', '士', '短', '袖', 'T', '恤'],
  keyword: ['夏季纯棉男士短袖 T 恤(完整不拆分)']
}
export function tokenize(coreEn, filterEns = []) {
  const base = CORE_SPLIT[coreEn] || ['夏季', '纯棉', '男士', '短袖', 'T 恤']
  const extra = []
  if (filterEns.includes('pinyin')) extra.push({ label: '拼音词条', words: ['xiaji', 'chunmian', 'nanshi', 'duanxiu', 'txu'] })
  if (filterEns.includes('synonym')) extra.push({ label: '同义词扩展', words: ['半袖', '汗衫', '纯棉布'] })
  if (filterEns.includes('edge_ngram')) extra.push({ label: '前缀词条', words: ['夏', '夏季'] })
  if (filterEns.includes('ngram')) extra.push({ label: '模糊片段', words: ['夏', '夏季', '季'] })
  const stopped = filterEns.includes('stop') ? '的、了、是 等停用词' : ''
  return { base, extra, stopped }
}
