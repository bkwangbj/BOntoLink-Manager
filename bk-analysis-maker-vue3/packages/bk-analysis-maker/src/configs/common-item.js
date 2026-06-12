const stateDic = [{ label: '是', value: '1' }, { label: '否', value: '0' }]
const commonItems = [
  { id: 0, type: 'input', isBasic: true, label: '标题', field: 'title', eventsType: 'basic' },
  { id: 14, type: 'input', isBasic: true, label: '唯一标识', field: 'hookId', eventsType: 'basic' },
  { id: 2, type: 'radio', isBasic: true, label: '显示标题', field: 'isShowTitle', value: '1', props: { mock: stateDic }, itemClass: 'item-border-bottom', eventsType: 'basic' },
  { id: 9, isBasic: true, label: '', field: 'extendedItem', slot: 'explain', itemClass: 'explain-item' },
  { id: 11, isBasic: true, label: '', field: 'paddingItem', slot: 'paddingBox', itemClass: 'padding-item' },
  { id: 13, isBasic: true, label: '', field: 'hookItem', slot: 'hook', itemClass: 'hook-item' },
  { id: 12, isBasic: true, label: '', field: 'queryItem', slot: 'queryBox', itemClass: 'query-item' },
  // { id: 3, type: 'radio', isBasic: false, label: '是否反查', field: 'isFC', group: '基础配置', value: '1', props: { mock: stateDic } },
  { id: 5, isBasic: false, label: '', field: 'dataSourceItem', group: '数据源配置', groupType: 'tab', itemClass: 'data-source-group', slot: 'dataSourceItem' },
  { id: 6, isBasic: false, label: '', field: 'varItem', group: '变量监听配置', groupType: 'tab', itemClass: 'var-group', slot: 'varItem' },
  { id: 7, isBasic: false, label: '', field: 'eventItem', group: '事件配置', groupType: 'tab', itemClass: 'event-group', slot: 'eventItem' },
  { id: 8, isBasic: false, label: '', field: 'operatorItem', group: '操作配置', groupType: 'tab', itemClass: 'operator-group', slot: 'operatorItem' }

]
const commonRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
}
export { commonItems, commonRules }
