// 图表配色模版(单一数据源):
// - 页面设置「图表配色模版」网格选择器用 CHART_PALETTES(带名称)
// - 单图表「色系配置」备选下拉用 paletteColorStrings(逗号拼接字符串,与主题 defaultColors 同格式)
export const CHART_PALETTES = [
  { key: 'app', name: '默认', colors: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#39D895', '#ba55d3'] },
  { key: 'classic', name: '经典', colors: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'] },
  { key: 'tech', name: '科技蓝', colors: ['#4992ff', '#7cffb2', '#fddd60', '#ff6e76', '#58d9f9', '#05c091', '#ff8a45', '#8d48e3', '#dd79ff'] },
  { key: 'gbi', name: '缤纷', colors: ['#4a90f0', '#37cfc0', '#9b6cff', '#f5c15b', '#f56a6a', '#4a90f0', '#f89f8c', '#6cd06c', '#4b4bf0', '#ffd84d'] },
  { key: 'colorui', name: '亮彩', colors: ['#f37b1d', '#39b54a', '#0081ff', '#6739b6', '#e54d42', '#8dc63f', '#1cbbb4', '#9c27b0', '#fbbd08', '#e03997'] },
  { key: 'modi', name: '雅致', colors: ['#5b8a72', '#8a6d3b', '#b23a48', '#5e3a5e', '#3f7fd6', '#c9a84a', '#8e6fb0', '#5ec9c9', '#6cbf6c', '#f2a08e'] },
  { key: 'aipage', name: '清朗', colors: ['#17a2a2', '#e94b6b', '#3f8cff', '#4caf50', '#f5a623', '#7b5cd6', '#e0563a'] },
  { key: 'macarons', name: '马卡龙', colors: ['#2ec7c9', '#b6a2de', '#5ab1ef', '#ffb980', '#d87a80', '#8d98b3', '#e5cf0d', '#97b552', '#95706d', '#dc69aa'] },
  { key: 'infographic', name: '信息图', colors: ['#c1232b', '#27727b', '#fcce10', '#e87c25', '#b5c334', '#fe8463', '#9bca63', '#fad860', '#f3a43b', '#60c0dd'] },
  { key: 'vintage', name: '复古', colors: ['#d87c7c', '#919e8b', '#d7ab82', '#6e7074', '#61a0a8', '#efa18d', '#787464', '#cc7e63', '#724e58', '#4b565b'] },
  { key: 'roma', name: '罗马', colors: ['#e01f54', '#001852', '#f5e8c8', '#b8d2c7', '#c6b38e', '#a4d8c2', '#f3d999', '#d3758f', '#dcc392', '#2e4783'] },
  { key: 'westeros', name: '西境', colors: ['#516b91', '#59c4e6', '#edafda', '#93b7e3', '#a5e7f0', '#cbb0e3'] },
  { key: 'walden', name: '瓦尔登', colors: ['#3fb1e3', '#6be6c1', '#626c91', '#a0a7e6', '#c4ebad', '#96dee8'] },
  { key: 'wonderland', name: '仙境', colors: ['#4ea397', '#22c3aa', '#7bd9a5', '#d0648a', '#f58db2', '#f2b3c9'] },
  { key: 'chalk', name: '粉笔', colors: ['#fc97af', '#87f7cf', '#f7f494', '#72ccff', '#f7c5a0', '#d4a4eb', '#d2f5a6', '#76f2f2'] },
  { key: 'morandi', name: '莫兰迪', colors: ['#f5a3a3', '#f7c685', '#85aeea', '#6fd2c3', '#bba0e2', '#a8d5a2'] },
  { key: 'sugar', name: '低饱和', colors: ['#b03b48', '#1b2b45', '#5b8a8f', '#9cbcae', '#7a9a8e', '#c48d3f', '#8a8f78', '#5e6b6b', '#3f4b4b'] },
  { key: 'purple', name: '紫韵', colors: ['#9b8bba', '#e098c7', '#8fd3e8', '#71669e', '#cc70af', '#7cb4cc'] },
  { key: 'dark', name: '暗夜', colors: ['#dd6b66', '#759aa0', '#e69d87', '#8dc1a9', '#ea7e53', '#eedd78', '#73a373', '#73b9bc', '#7289ab', '#91ca8c'] },
  { key: 'warm', name: '暖色', colors: ['#c98a5e', '#8a5638', '#e8843c', '#e2543a', '#d24a4a', '#3fa89a', '#9e6cae', '#6f57a0', '#e0a860', '#c9b07e'] },
  { key: 'cold', name: '冷色', colors: ['#57d6a0', '#3fc4c4', '#2f7f92', '#43b7a6', '#4aa590', '#4a7ac8', '#5b5cc8', '#3fc4d6', '#6ad6c6', '#4a95c8'] }
]

// 与主题 chartStyle.defaultColors 同格式:每套用逗号拼接
export const paletteColorStrings = CHART_PALETTES.map(p => p.colors.join(','))
