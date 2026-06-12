const emphasis = {
  scale: false,
  itemStyle: {
    color: 'inherit'
  }
}
export function getRingOption ({
  // 颜色
  color,
  position,
  // 数据 [{name: '名称', value: 1 }]
  data
}) {
  const seriesData = []
  // 消除数值过小导致间隔太大的问题
  let sum = 0
  const radius = []
  const center = []
  if (position) {
    radius[0] = Number(position.radius[0])
    radius[1] = Number(position.radius[1])
    center[0] = Number(position.center[0])
    center[1] = Number(position.center[1])
  }
  for (const item of data) {
    sum += item.value
  }
  const factor = 100 / sum
  for (let i = 0; i < data.length; i++) {
    seriesData.push({
      value: data[i].value * factor,
      name: data[i].name,
      originData: data[i],
      itemStyle: {
        borderWidth: 5,
        shadowBlur: 10,

        borderColor: color[i],
        shadowColor: color[i],
        borderRadius: '50%'
      },
      tooltip: {
        position: null
      }
    })
    seriesData.push({
      value: 5,
      name: '',
      tooltip: {
        show: false
      },
      select: {
        disabled: true
      },
      itemStyle: {
        label: {
          show: false
        },
        labelLine: {
          show: false
        },
        color: 'rgba(0, 0, 0, 0)',
        borderColor: 'rgba(0, 0, 0, 0)',
        borderWidth: 0
      }
    })
  }
  return {
    color,
    series: [
      {
        type: 'pie',
        zlevel: 1,
        silent: true,
        radius: [(radius[1] / 100) * 84 + 14, (radius[1] / 100) * 84 + 13],
        center,
        hoverAnimation: false,
        animation: false,
        label: {
          show: false
        },
        labelLine: {
          show: false
        },
        data: [
          { name: '', value: 30, itemStyle: { color: getGradientColor('#16589888', '#ffffff00') } },
          { name: '', value: 40, itemStyle: { color: 'rgb(0,0,0,0)' } },
          { name: '', value: 30, itemStyle: { color: getGradientColor('#16589888', '#ffffff00') } }
        ]
      },
      {
        type: 'pie',
        zlevel: 1,
        silent: true,
        radius: [(radius[1] / 100) * 84 + 9, (radius[1] / 100) * 84 + 8],
        center,
        hoverAnimation: false,
        animation: false,
        label: {
          show: false
        },
        labelLine: {
          show: false
        },
        data: [
          { name: '', value: 40, itemStyle: { color: getGradientColor('#2386d9', '#ffffff00') } },
          { name: '', value: 20, itemStyle: { color: 'rgb(0,0,0,0)' } },
          { name: '', value: 40, itemStyle: { color: getGradientColor('#2386d9', '#ffffff00') } }
        ]
      },
      {
        name: '',
        type: 'pie',
        emphasis,
        radius: [(radius[0] / 100) * 84, (radius[1] / 100) * 84],
        center,
        label: {
          show: false
        },
        data: seriesData
      }
    ]
  }
}
function getGradientColor (startColor, endColor) {
  return {
    type: 'linear',
    x: 0,
    y: 0,
    x2: 0,
    y2: 1,
    colorStops: [
      { offset: 0, color: startColor },
      { offset: 1, color: endColor }
    ]
  }
}
