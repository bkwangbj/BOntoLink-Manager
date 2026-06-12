const colorList = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc']
const stateDic = [{ label: '是', value: '1' }, { label: '否', value: '0' }]
const fontWeightOptions = [{ label: 'normal', value: 'normal' }, { label: 'bold', value: 'bold' }, { label: 'bolder', value: 'bolder' }, { label: 'lighter', value: 'lighter' }]
const lineTypeOptions = [{ label: '实线', value: 'solid' }, { label: '虚线', value: 'dashed' }, { label: '点线', value: 'dotted' }]
const mergeChildChartList = [
  { label: '合并', value: 'merge' },
  { label: '独立', value: 'independence' }
]
const backgroundRepeatList = [
  { label: '平铺', value: 'repeat' },
  { label: '拉伸', value: 'no-repeat' }
]

const cardModeList = [
  { label: '自适应', value: 'adaptive' },
  { label: '自动', value: 'auto' }
]
export { colorList, stateDic, fontWeightOptions, lineTypeOptions, mergeChildChartList, backgroundRepeatList, cardModeList }
