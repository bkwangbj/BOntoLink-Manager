import addAreaImg from './components/toolbar/images/add-area.png'
import addItemImg from './components/toolbar/images/add-item.png'
import addTabLayoutImg from './components/toolbar/images/tab-layout.png'
import addQuery from './components/toolbar/images/query.png'
const cardRadiusList = [
  { label: '无', value: 'none', class: 'no-radius' },
  { label: '小', value: 'small', class: 'radius-8' },
  { label: '大', value: 'big', class: 'radius-20' }
]

const spaceModeList = [
  { label: '紧凑', value: 'compact' },
  { label: '常规', value: 'normal' },
  { label: '自定义', value: 'custom' }
]

const themeList = [
  { value: 'default', label: '默认', class: 'default-theme' },
  { value: 'blue', label: '深蓝', class: 'blue-theme' }
]

const pageHeightList = [
  { label: '自适应', value: 'adaptive' },
  { label: '自动', value: 'auto' }
]

const pageWidthList = [
  { label: '自适应', value: 'adaptive' },
  { label: '固定', value: 'custom' }
]
const menuList = [
  {
    name: '布局',
    key: 'layout',
    children: [
      { name: '添加区域', isBasic: true, key: 'addItem', img: addAreaImg, payload: true, pKey: 'layout' },
      { name: '添加布局', key: 'addLayout', img: addItemImg, payload: false, pKey: 'layout' },
      { name: '页签布局', isBasic: true, key: 'addTabLayout', img: addTabLayoutImg, pKey: 'layout' },
      { name: '添加查询', key: 'addQuery', img: addQuery, pKey: 'layout' }
    ]
  },
  {
    name: '模板',
    key: 'template',
    children: []
  },
  {
    name: '图表',
    key: 'chart',
    draggable: true,
    children: []
  },
  {
    name: '柱状图',
    key: 'chart',
    draggable: true,
    hasParent: true,
    children: []
  },
  {
    name: '饼形图',
    key: 'ring',
    hasParent: true,
    draggable: true,
    children: []
  },
  {
    name: '表格',
    key: 'table',
    hasParent: true,
    draggable: true,
    children: []
  },
  {
    name: '地图',
    key: 'map',
    hasParent: true,
    draggable: true,
    children: []
  },
  {
    name: '组件',

    key: 'component',
    draggable: true,
    children: []
  },

  {
    name: '其他',
    key: 'other',
    draggable: true,
    children: []
  },
  {
    name: '素材',
    key: 'decorate',
    children: []
  }
]

function getBasicMenus (tools) {
  const menus = menuList.filter(c => c.children && c.children.find(a => tools.indexOf(a.key) !== -1))
  return menus.map(item => { return { ...item, children: item.children.filter(c => tools.indexOf(c.key) !== -1) } })
}
export { cardRadiusList, spaceModeList, themeList, pageHeightList, pageWidthList, menuList, getBasicMenus }
