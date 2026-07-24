export const chartDefaultConfig = new Map([
  ['lineChart', {

    tooltip: {
      trigger: 'axis'
    },
    legend: {
      show: true,
      alignPosition: 'topCenter'
    },
    grid: {
      top: 50,
      left: 50,
      right: 50,
      bottom: 20,
      containLabel: true
    },
    xAxis: {
      show: true,
      type: 'category',
      axisLabel: { show: true },
      axisLine: {
        show: true,
        lineStyle: {
          color: '#999'
        }
      },
      axisTick: {
        show: true,
        lineStyle: {
          width: 1,

          color: '#DDD'
        }
      }
    },
    yAxis: {
      show: true,
      type: 'value',
      alignTicks: true,
      splitLine: {
        show: true,
        lineStyle: {
          width: 1,
          type: 'dashed',
          color: '#DDD'
        }
      },
      axisTick: {
        show: false,
        lineStyle: {
          width: 1,

          color: '#DDD'
        }
      },
      axisLabel: { show: true },
      axisLine: {
        show: false,
        lineStyle: {
          color: '#333'
        }
      },
      nameTextStyle: {
        color: '#999'
      },
      splitArea: {
        show: false
      }
    },
    series: [{
      type: 'line',
      color: null,
      showSymbol: true,
      symbolSize: 4,
      symbol: 'emptyCircle',
      lineStyle: {

        width: 2,
        color: null

      },
      itemStyle: {

        color: null

      },
      smooth: false
    },
    {
      showSymbol: true,
      symbolSize: 4,
      symbol: 'emptyCircle',
      type: 'line',
      lineStyle: {

        width: 2,
        color: null

      },
      itemStyle: {
        color: null
      },
      smooth: false
    }
    ]
  }],
  ['smoothLineChart', { ..._lineBaseOption(), series: _lineSeries({ smooth: 0.6, smoothMonotone: 'none' }) }],
  ['markLineChart', { ..._lineBaseOption(), series: _lineSeries({ smooth: 0.6, smoothMonotone: 'none', symbol: 'circle', symbolSize: 8, lineStyle: { width: 3, color: null }, areaStyle: { opacity: 0.3 }, label: { show: true, position: 'top', color: '#4e5969', fontSize: 12, fontWeight: 'bold', formatter: '{c}' } }) }],
  ['areaChart', { ..._lineBaseOption(), series: _lineSeries({ areaStyle: {}, smooth: true }) }],
  ['stackAreaChart', { ..._lineBaseOption(), series: _lineSeries({ areaStyle: {}, stack: 'total', smooth: false }) }],
  ['stepLineChart', { ..._lineBaseOption(), series: _lineSeries({ step: 'end', showSymbol: true }) }],
  ['barHorizontalChart', {
    showBorderRadius: false,
    tooltip: { trigger: 'axis' },
    legend: { show: true, alignPosition: 'topCenter' },
    grid: { top: 50, left: 70, right: 50, bottom: 20, containLabel: true },
    xAxis: { show: true, type: 'value', axisLabel: { show: true }, axisLine: { show: true, lineStyle: { color: '#999' } }, splitLine: { show: true, lineStyle: { width: 1, type: 'dashed', color: '#DDD' } } },
    yAxis: { show: true, type: 'category', axisLabel: { show: true }, axisLine: { show: true, lineStyle: { color: '#999' } }, axisTick: { show: true, lineStyle: { width: 1, color: '#DDD' } } },
    series: [{ type: 'bar', itemStyle: { color: null }, backgroundStyle: {} }, { type: 'bar', itemStyle: { color: null }, backgroundStyle: {} }]
  }],
  ['rainfallEvap', {
    rawEChart: true,
    color: ['#008FFF', '#FFC72F', '#00E4BF'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['降雨量', '蒸发量', '降水量'], top: 6 },
    axisPointer: { link: [{ xAxisIndex: 'all' }] },
    dataZoom: [{ show: true, realtime: true, type: 'slider', bottom: 6, height: 14, start: 0, end: 100, xAxisIndex: [0, 1] }],
    grid: [
      { left: 56, right: 30, top: 50, height: '33%' },
      { left: 56, right: 30, top: '56%', height: '30%' }
    ],
    xAxis: [
      { type: 'category', boundaryGap: true, axisLine: { onZero: true }, data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'] },
      { gridIndex: 1, type: 'category', boundaryGap: true, axisLine: { onZero: true }, position: 'top', data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'] }
    ],
    yAxis: [
      { name: '雨量/蒸发(mm)', type: 'value', splitLine: { lineStyle: { type: 'dashed', color: '#DDD' } } },
      { gridIndex: 1, name: '降水量(mm)', type: 'value', inverse: true, splitLine: { lineStyle: { type: 'dashed', color: '#DDD' } } }
    ],
    series: [
      { name: '降雨量', type: 'bar', barMaxWidth: 24, data: [42, 38, 65, 88, 120, 175, 210, 198, 142, 96, 58, 40] },
      { name: '蒸发量', type: 'line', smooth: true, symbolSize: 5, data: [30, 45, 70, 96, 135, 162, 188, 176, 130, 88, 52, 34] },
      { name: '降水量', type: 'line', xAxisIndex: 1, yAxisIndex: 1, smooth: true, areaStyle: {}, symbolSize: 5, data: [48, 40, 72, 95, 128, 180, 220, 205, 150, 100, 62, 44] }
    ]
  }],
  ['barChart', {
    showBorderRadius: false,
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      show: true,
      alignPosition: 'topCenter'
    },
    grid: {
      top: 50,
      left: 50,
      right: 50,
      bottom: 20,
      containLabel: true
    },
    xAxis: {
      show: true,
      type: 'category',
      axisLabel: { show: true },
      axisTick: {
        show: true,
        lineStyle: {
          width: 1,

          color: '#DDD'
        }
      },
      axisLine: {
        show: true,
        lineStyle: {
          color: '#999'
        }
      }
    },
    yAxis: {
      show: true,
      type: 'value',
      alignTicks: true,
      splitLine: {
        show: true,
        lineStyle: {
          width: 1,
          type: 'dashed',
          color: '#DDD'
        }
      },
      axisLabel: { show: true },
      axisTick: {
        show: false,
        lineStyle: {
          width: 1,

          color: '#DDD'
        }
      },
      axisLine: {
        show: false,
        lineStyle: {
          color: '#333'
        }
      },
      nameTextStyle: {
        color: '#999'
      },
      splitArea: {
        show: false
      }
    },
    series: [{
      type: 'bar',
      itemStyle: {
        color: null
      },
      backgroundStyle: {}

    },
    {

      type: 'bar',
      itemStyle: {
        color: null
      },
      backgroundStyle: {}

    }
    ]
  }], ['barRadiusChart',
    {
      showBorderRadius: true,
      borderRadius: [10, 10, 0, 0],
      tooltip: {
        trigger: 'axis'
      },
      legend: {
        show: true,
        alignPosition: 'topCenter'
      },
      grid: {
        top: 50,
        left: 50,
        right: 50,
        bottom: 20,
        containLabel: true
      },
      xAxis: {
        show: true,
        type: 'category',
        axisLabel: { show: true },
        axisTick: {
          show: true,
          lineStyle: {
            width: 1,

            color: '#DDD'
          }
        },
        axisLine: {
          show: true,
          lineStyle: {
            color: '#999'
          }
        }
      },
      yAxis: {
        show: true,
        type: 'value',
        alignTicks: true,
        splitLine: {
          show: true,
          lineStyle: {
            width: 1,
            type: 'dashed',
            color: '#DDD'
          }
        },
        axisLabel: { show: true },
        axisTick: {
          show: false,
          lineStyle: {
            width: 1,

            color: '#DDD'
          }
        },
        axisLine: {
          show: false,
          lineStyle: {
            color: '#333'
          }
        },
        nameTextStyle: {
          color: '#999'
        },
        splitArea: {
          show: false
        }
      },
      series: [{
        type: 'bar',
        itemStyle: {
          color: null
        },
        barMaxWidth: 20,
        backgroundStyle: {}

      },
      {

        type: 'bar',
        itemStyle: {
          color: null
        },
        barMaxWidth: 20
      }
      ]
    }], ['barLineargradientChart', {
    showBorderRadius: true,
    borderRadius: [10, 10, 0, 0],
    color: ['linear-gradient(90deg,rgba(3,130,252,0.10) 0%, rgb(3,130,252) 100%)',
      'linear-gradient(90deg,rgba(13,221,255,0.00) 0%, rgb(13,221,255) 100%)',
      'linear-gradient(90deg,rgba(254,190,19,0.10) 0%, rgb(254,190,19) 100%)',
      'linear-gradient(90deg,rgba(133,245,121,0.10) 0%, rgb(133,245,121) 100%)'],
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      show: true,
      alignPosition: 'topCenter'
    },
    grid: {
      top: 50,
      left: 50,
      right: 50,
      bottom: 20,
      containLabel: true
    },
    xAxis: {
      show: true,
      type: 'category',
      axisLabel: { show: true },
      axisTick: {
        show: true,
        lineStyle: {
          width: 1,

          color: '#DDD'
        }
      },
      axisLine: {
        show: true,
        lineStyle: {
          color: '#999'
        }
      }
    },
    yAxis: {
      show: true,
      type: 'value',
      alignTicks: true,
      splitLine: {
        show: true,
        lineStyle: {
          width: 1,
          type: 'dashed',
          color: '#DDD'
        }
      },
      axisLabel: { show: true },
      axisTick: {
        show: false,
        lineStyle: {
          width: 1,

          color: '#DDD'
        }
      },
      axisLine: {
        show: false,
        lineStyle: {
          color: '#333'
        }
      },
      nameTextStyle: {
        color: '#999'
      },
      splitArea: {
        show: false
      }
    },
    series: [{
      type: 'bar',
      itemStyle: {
        color: null
      },
      barMaxWidth: 20,
      backgroundStyle: {}

    },
    {

      type: 'bar',
      itemStyle: {
        color: null
      },
      barMaxWidth: 20,
      backgroundStyle: {}

    }
    ]
  }], ['mixChart', {
    showBorderRadius: false,
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      show: true,
      alignPosition: 'topCenter'
    },
    grid: {
      top: 50,
      left: 50,
      right: 50,
      bottom: 20,
      containLabel: true
    },
    xAxis: {
      show: true,
      type: 'category',
      axisLabel: { show: true },
      axisTick: {
        show: true,
        lineStyle: {
          width: 1,

          color: '#DDD'
        }
      },
      axisLine: {
        show: true,
        lineStyle: {
          color: '#999'
        }
      }
    },
    yAxis: {
      show: true,
      type: 'value',
      alignTicks: true,
      splitLine: {
        show: true,
        lineStyle: {
          width: 1,
          type: 'dashed',
          color: '#DDD'
        }
      },
      axisLabel: { show: true },
      axisTick: {
        show: false,
        lineStyle: {
          width: 1,

          color: '#DDD'
        }
      },
      axisLine: {
        show: false,
        lineStyle: {
          color: '#333'
        }
      },
      nameTextStyle: {
        color: '#999'
      },
      splitArea: {
        show: false
      }
    },
    series: [{
      type: 'bar',
      itemStyle: {
        color: null
      },
      backgroundStyle: {}

    },
    {

      type: 'line',
      showSymbol: true,
      symbolSize: 4,
      symbol: 'circle',
      lineStyle: {

        width: 2,
        color: null,
        shadowColor: 'rgba(71,216,190, 0.5)',
        shadowBlur: 10,
        shadowOffsetY: 7
      },
      itemStyle: {
        color: null
      },
      smooth: false
    }
    ]
  }], ['stackedChart', {
    showBorderRadius: false,
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      show: true,
      alignPosition: 'topCenter'
    },
    grid: {
      top: 50,
      left: 50,
      right: 50,
      bottom: 20,
      containLabel: true
    },
    xAxis: {
      show: true,
      type: 'category',
      axisLabel: { show: true },
      axisTick: {
        show: true,
        lineStyle: {
          width: 1,

          color: '#DDD'
        }
      },
      axisLine: {
        show: true,
        lineStyle: {
          color: '#999'
        }
      }
    },
    yAxis: {
      show: true,
      type: 'value',
      alignTicks: true,
      splitLine: {
        show: true,
        lineStyle: {
          width: 1,
          type: 'dashed',
          color: '#DDD'
        }
      },
      axisLabel: { show: true },
      axisTick: {
        show: false,
        lineStyle: {
          width: 1,

          color: '#DDD'
        }
      },
      axisLine: {
        show: false,
        lineStyle: {
          color: '#333'
        }
      },
      nameTextStyle: {
        color: '#999'
      },
      splitArea: {
        show: false
      }
    },
    series: [{
      type: 'bar',
      color: null,
      stack: '1',
      backgroundStyle: {}
    },
    {

      type: 'bar',
      color: null,
      stack: '1',
      backgroundStyle: {}
    }
    ]
  }], ['polarChart',
    {
      tooltip: {
        trigger: 'axis'
      },
      polar: {
        radius: [0, 70],
        center: [50, 50]
      },
      legend: {
        show: true,
        alignPosition: 'topCenter'
      },
      grid: {
        top: 50,
        left: 50,
        right: 50,
        bottom: 20,
        containLabel: true
      },
      angleAxis: {
        show: true,
        type: 'category',
        axisLabel: { show: true },
        axisTick: {
          show: true,
          lineStyle: {
            width: 1,

            color: '#DDD'
          }
        },
        axisLine: {
          show: true,
          lineStyle: {
            color: '#999'
          }
        }
      },
      radiusAxis: {
        show: true,
        type: 'value',
        alignTicks: true,
        splitLine: {
          show: true,
          lineStyle: {
            width: 1,
            type: 'dashed',
            color: '#DDD'
          }
        },
        axisLabel: { show: false },
        axisTick: {
          show: false,
          lineStyle: {
            width: 1,

            color: '#DDD'
          }
        },
        axisLine: {
          show: false,
          lineStyle: {
            color: '#333'
          }
        },
        nameTextStyle: {
          color: '#999'
        },
        splitArea: {
          show: false
        }
      },
      series: [
        {
          type: 'bar',
          coordinateSystem: 'polar'
        },
        {
          type: 'bar',
          coordinateSystem: 'polar'
        },
        {
          type: 'bar',
          coordinateSystem: 'polar'
        }
      ]
    }], ['pieChart', {
    color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#909399'],
    tooltip: {
      trigger: 'item'
    },
    legend: {
      show: true,
      alignPosition: 'topCenter'
    },
    series: [{
      type: 'pie',
      center: [50, 50],
      radius: [0, 45],
      clockwise: true,
      avoidLabelOverlap: true,
      hoverOffset: 15,
      label: {
        show: true,
        position: 'outside',
        formatter: '{d}%'

      },
      labelLine: {
        show: true,
        length: 20,
        length2: 30,
        lineStyle: {
          width: 1
        }

      }
    }]
  }], ['ringChart', {
    color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#909399'],
    tooltip: {
      trigger: 'item'
    },
    legend: {
      show: true,
      alignPosition: 'topCenter'
    },
    series: [{
      type: 'pie',
      center: [50, 50],
      radius: [24, 45],
      clockwise: true,
      avoidLabelOverlap: true,
      hoverOffset: 15,
      label: {
        show: true,
        position: 'outside',
        formatter: '{d}%'

      },
      labelLine: {
        show: true,
        length: 20,
        length2: 30,
        lineStyle: {
          width: 1
        }

      }
    }]
  }], ['roundPieChart', {
    color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#909399'],
    tooltip: { trigger: 'item' },
    legend: { show: true, alignPosition: 'topCenter' },
    series: [{
      type: 'pie', center: [50, 50], radius: [0, 45], clockwise: true, avoidLabelOverlap: true, hoverOffset: 15,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, position: 'outside', formatter: '{d}%' },
      labelLine: { show: true, length: 20, length2: 30, lineStyle: { width: 1 } }
    }]
  }], ['roundRingChart', {
    color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#909399'],
    tooltip: { trigger: 'item' },
    legend: { show: true, alignPosition: 'topCenter' },
    series: [{
      type: 'pie', center: [50, 50], radius: [30, 48], clockwise: true, avoidLabelOverlap: true, hoverOffset: 15,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, position: 'outside', formatter: '{d}%' },
      labelLine: { show: true, length: 20, length2: 30, lineStyle: { width: 1 } }
    }]
  }], ['carouselPieChart', {
    color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9B59B6'],
    tooltip: { trigger: 'item' },
    legend: { show: true, alignPosition: 'bottomCenter' },
    series: [{
      type: 'pie', center: [50, 50], radius: [38, 62], clockwise: true, avoidLabelOverlap: false, hoverOffset: 8,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: false, position: 'center' },
      emphasis: {
        scale: true, scaleSize: 8,
        label: { show: true, position: 'center', formatter: '{d}%\n{b}', fontSize: 16, fontWeight: 'bold', lineHeight: 22, color: '#303133' }
      },
      labelLine: { show: false }
    }]
  }], ['petalPieChart', {
    color: ['#409EFF', '#67C23A', '#9B59B6', '#F5B041', '#F56C6C', '#5DADE2'],
    tooltip: { trigger: 'item' },
    legend: { show: true, alignPosition: 'bottomCenter' },
    series: [{
      type: 'pie', center: [50, 50], radius: [0, 62], clockwise: true, avoidLabelOverlap: true, hoverOffset: 12, padAngle: 4,
      itemStyle: { borderRadius: 24, borderColor: '#fff', borderWidth: 4 },
      label: { show: false },
      labelLine: { show: false }
    }]
  }], ['posNegBarChart', {
    color: ['#3ED848', '#F56C6C'],
    showBorderRadius: false,
    tooltip: { trigger: 'axis' },
    legend: { show: true, alignPosition: 'topCenter' },
    grid: { top: 50, left: 50, right: 50, bottom: 20, containLabel: true },
    xAxis: {
      show: true, type: 'category', axisLabel: { show: true },
      axisTick: { show: true, lineStyle: { width: 1, color: '#DDD' } },
      axisLine: { show: true, lineStyle: { color: '#999' } }
    },
    yAxis: {
      show: true, type: 'value', alignTicks: true,
      splitLine: { show: true, lineStyle: { width: 1, type: 'dashed', color: '#DDD' } },
      axisLabel: { show: true },
      axisTick: { show: false, lineStyle: { width: 1, color: '#DDD' } },
      axisLine: { show: false, lineStyle: { color: '#333' } },
      nameTextStyle: { color: '#999' }, splitArea: { show: false }
    },
    series: [
      { type: 'bar', itemStyle: { color: null }, backgroundStyle: {} },
      { type: 'bar', itemStyle: { color: null }, backgroundStyle: {} }
    ]
  }], ['bubbleChart', {
    color: ['#00E4BF', '#FFC72F', '#008FFF', '#3ED848', '#C96765', '#D8E5FA'],
    tooltip: { trigger: 'item' },
    legend: { show: false, alignPosition: 'topCenter' },
    grid: { top: 40, left: 50, right: 40, bottom: 40, containLabel: true },
    xAxis: {
      show: true, type: 'value', scale: true, name: '',
      axisLabel: { show: true }, axisLine: { show: true, lineStyle: { color: '#999' } },
      splitLine: { show: true, lineStyle: { color: '#eee', type: 'dashed' } }
    },
    yAxis: {
      show: true, type: 'value', scale: true, name: '',
      axisLabel: { show: true }, axisLine: { show: false },
      splitLine: { show: true, lineStyle: { color: '#eee', type: 'dashed' } }
    },
    series: [{ type: 'scatter', symbolSize: 22, data: [] }]
  }], ['calendarHeatmap', {
    color: ['#00E4BF', '#FFC72F', '#008FFF', '#3ED848', '#C96765', '#D8E5FA'],
    tooltip: { trigger: 'item' },
    legend: { show: false, alignPosition: 'topCenter' },
    visualMap: {
      show: true, min: 0, max: 100, orient: 'horizontal', left: 'center', bottom: 8,
      itemWidth: 12, itemHeight: 90, text: ['高', '低'], textStyle: { fontSize: 11 },
      inRange: { color: ['#E8F3FF', '#4080FF', '#0E42D2'] }
    },
    calendar: {
      top: 45, bottom: 55, left: 55, right: 25, cellSize: 'auto', range: '2026',
      itemStyle: { borderWidth: 1, borderColor: '#fff', color: '#F2F3F5' },
      splitLine: { show: true, lineStyle: { color: '#C9CDD4', width: 1 } },
      yearLabel: { show: false },
      dayLabel: { firstDay: 1, margin: 10, nameMap: ['日', '一', '二', '三', '四', '五', '六'] },
      monthLabel: { show: true, nameMap: 'cn' }
    },
    series: [{ type: 'heatmap', coordinateSystem: 'calendar', data: [] }]
  }], ['ringChart2', {
    color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#909399'],
    position: {
      radius: [65, 75],
      center: [50, 50]
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      show: false,
      alignPosition: 'topCenter'
    }
  }], ['progressChart', {
    title: {
      show: true,
      text: '',
      x: 'center',
      top: 'center',
      textStyle: {
        fontSize: '30',
        color: '#fdf914',
        fontFamily: 'Microsoft YaHei',
        fontWeight: 'bold'
      }
    },
    polar: {
      radius: [60, 70],
      center: [50, 50]
    },
    angleAxis: {
      max: 100,
      show: false
    },
    radiusAxis: {
      type: 'category',
      show: false,
      axisLabel: {
        show: false
      },
      axisLine: {
        show: false

      },
      axisTick: {
        show: false
      }
    },
    series: [
      {
        name: '',
        type: 'bar',
        roundCap: true,
        showBackground: true,
        backgroundStyle: {
          color: 'rgba(66, 66, 66, .3)'
        },
        data: '',
        coordinateSystem: 'polar',

        itemStyle: {

          color: '#fdf914'

        }

      }
    ]
  }], ['multiplePieChart', {
    color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#909399'],
    tooltip: {
      trigger: 'item'
    },
    legend: {
      show: true,
      alignPosition: 'topCenter'
    },
    series: [{
      type: 'pie',
      center: [50, 50],
      radius: [0, 30],
      clockwise: true,
      avoidLabelOverlap: true,

      hoverOffset: 15,
      label: {
        show: true,
        position: 'outside',
        formatter: '{d}%'

      },
      labelLine: {
        show: false,
        length: 5,
        length2: 5,
        lineStyle: {
          width: 1
        }

      }
    }, {
      type: 'pie',
      center: [50, 50],
      radius: [60, 80],
      clockwise: true,
      avoidLabelOverlap: true,
      hoverOffset: 15,
      label: {
        show: true,
        position: 'outside',
        formatter: '{d}%'

      },
      labelLine: {
        show: true,
        length: 20,
        length2: 30,
        lineStyle: {
          width: 1
        }

      }
    }]
  }], ['BKStatisticsChart', {
    widthType: 'adaptive',
    widthNum: 4,
    heightType: 'custom',
    heightNum: 1,
    pieces: [],
    styles: {
      gap: 10,
      isPieces: false,
      borderWidth: 0,
      width: '25%',
      height: '100%',
      borderColor: '#000',
      borderRadius: 4,
      backgroundColor: ''

    },
    image: {
      width: '60px',
      height: '60px',
      show: true,
      position: {
        x: 'left',
        y: 'center',
        top: '0',
        left: '10px'
      }
    },
    unit: {
      show: false,
      posi: 'text',
      position: {
        x: '',
        y: '',
        top: '0px',
        left: '0px'
      },
      textStyle: {

      }
    },
    number: {
      position: {
        x: 'center',
        y: 'center',
        top: '10px',
        left: '10px'
      },
      textStyle: {

      }
    },
    text: {
      position: {
        x: 'center',
        y: 'center',
        top: '-10px',
        left: '10px'
      },
      textStyle: {

      }
    }

  }

  ], ['commonTable',
    {
      props: {
        hasPager: false,
        hasSeq: true,
        pageSize: 20,
        highlightCurrentRow: false,
        stripe: false,
        showHeader: true,
        headerAlign: 'center',
        border: 'full',
        align: 'center',
        showHeaderOverflow: true
      },
      styles:
      {
        '--borderColor': '#e8eaec',
        '--stripeColor': '#fafafa',
        '--bodyBg': '#fff',
        '--headerBg': '#f8f8f9',
        '--bodycolor': '#606266',
        '--bodyfontSize': 12,
        '--bodyfontFamily': 'Microsoft YaHei',
        '--bodyfontWeight': 'normal',
        '--rowCurrentColor': '#e6f7ff',
        '--headercolor': '#606266',
        '--headerfontSize': 12,
        '--headerfontFamily': 'Microsoft YaHei',
        '--headerfontWeight': 'normal'
      },
      columns: []
    }
  ], ['tjbTable',
    {
      props: {
        hasPager: false,
        hasSeq: false,
        pageSize: 20,
        highlightCurrentRow: false,
        stripe: false,
        showHeader: true,
        headerAlign: 'center',
        border: 'full',
        align: 'center',
        showHeaderOverflow: true
      },
      styles:
    {
      '--borderColor': '#e8eaec',
      '--stripeColor': '#fafafa',
      '--bodyBg': '#fff',
      '--headerBg': '#f8f8f9',
      '--bodycolor': '#606266',
      '--bodyfontSize': 12,
      '--bodyfontFamily': 'Microsoft YaHei',
      '--bodyfontWeight': 'normal',
      '--rowCurrentColor': '#e6f7ff',
      '--headercolor': '#606266',
      '--headerfontSize': 12,
      '--headerfontFamily': 'Microsoft YaHei',
      '--headerfontWeight': 'normal'
    },
      columns: []
    }
  ], ['heatMap', {
    tooltip: {
      show: true
    },
    geo: {
      map: 'china',
      zoom: 1.1,
      roam: true,
      selectedMode: true,
      label: {
        show: true,
        color: 'rgb(249, 249, 249)'
      },

      itemStyle: {
        areaColor: '#24CFF4',
        borderColor: '#53D9FF',
        borderWidth: 1.3
        //  shadowBlur: 15,
        //  shadowColor: 'rgb(58,115,192)',
        //  shadowOffsetX: 7,
        //  shadowOffsetY: 6

      },
      emphasis: {
        disabled: false,
        itemStyle: {
          areaColor: '#8dd7fc',
          borderColor: '#53D9FF',
          borderWidth: 1.6
          // shadowBlur: 25
        },
        label: {
          show: true,
          color: '#f75a00'
        }

      },
      select: {
        disabled: true,
        label: {
          show: true
        }
      }
    },
    series: [{
      type: 'map',
      geoIndex: 0,
      roam: true,
      zoom: 1.3,
      label: {
        normal: {
          show: false
        },
        emphasis: {
          show: false
        }
      }

    }

    ],
    showScatter: false,
    showVisualMap: true,
    scatterSeries: [
      {
        type: 'scatter',
        coordinateSystem: 'geo',
        symbolSize: 15,
        zlevel: 2,
        symbol: 'circle',
        symbolKeepAspect: true,
        symbolOffset: [0, 0],
        itemStyle: {
          color: 'rgba(255, 145, 0, 1)'
        },
        emphasis: {
          show: false,
          scale: true
        },
        select: {
          show: false

        },
        label: { show: false }
      }
    ],
    visualMap: {
      type: 'continuous',
      left: '3%',
      bottom: '5%',
      min: 0,
      max: 200,
      calculable: true,
      seriesIndex: [0],
      inRange: {
        color: ['#24CFF4', '#1E62AC']
      },
      textStyle: {
        color: '#24CFF4'
      }
    }

  }], ['solidtMap', {
    tooltip: {
      show: true
    },
    geo: {
      map: 'china',
      zoom: 1.1,
      roam: true,
      selectedMode: true,
      zlevel: 1,
      label: {
        show: true,
        color: 'rgb(249, 249, 249)'
      },

      itemStyle: {
        areaColor: '#24CFF4',
        borderColor: '#53D9FF',
        borderWidth: 1.3
        //  shadowBlur: 15,
        //  shadowColor: 'rgb(58,115,192)',
        //  shadowOffsetX: 7,
        //  shadowOffsetY: 6

      },
      emphasis: {
        disabled: false,
        itemStyle: {
          areaColor: '#8dd7fc',
          borderColor: '#53D9FF',
          borderWidth: 1.6
          // shadowBlur: 25
        },
        label: {
          show: true,
          color: '#f75a00'
        }

      },
      select: {
        disabled: true,
        label: {
          show: true
        }
      }
    },
    showScatter: false,
    showVisualMap: true,
    scatterSeries: [
      {
        type: 'scatter',
        coordinateSystem: 'geo',
        symbolSize: 15,
        zlevel: 2,
        symbol: 'circle',
        symbolKeepAspect: true,
        symbolOffset: [0, 0],
        itemStyle: {
          color: 'rgba(255, 145, 0, 1)'
        },
        emphasis: {
          show: false,
          scale: true
        },
        select: {
          show: false

        },
        label: { show: false }
      }
    ],
    visualMap: {
      type: 'continuous',
      left: '3%',
      bottom: '5%',
      min: 0,
      max: 200,
      calculable: true,
      seriesIndex: [0],
      inRange: {
        color: ['#24CFF4', '#1E62AC']
      },
      textStyle: {
        color: '#24CFF4'
      }
    },

    series: [{
      type: 'map',
      roam: true,
      zoom: 1.1,
      zlevel: 2,
      label: {
        normal: {
          show: false
        },
        emphasis: {
          show: false
        }
      }

    }

    ]

  }], ['scatterMap', {
    tooltip: {
      show: true
    },
    geo: {
      map: 'china',
      zoom: 1.1,
      roam: true,
      selectedMode: true,
      zlevel: 1,
      label: {
        show: false,
        color: 'rgb(249, 249, 249)'
      },
      itemStyle: {
        areaColor: '#24CFF4',
        borderColor: '#53D9FF',
        borderWidth: 1.3
        //  shadowBlur: 15,
        //  shadowColor: 'rgb(58,115,192)',
        //  shadowOffsetX: 7,
        //  shadowOffsetY: 6

      },
      emphasis: {
        disabled: false,
        itemStyle: {
          areaColor: '#8dd7fc',
          borderColor: '#53D9FF',
          borderWidth: 1.6
          // shadowBlur: 25
        },
        label: {
          show: true,
          color: '#f75a00'
        }

      },
      select: {
        disabled: true,
        label: {
          show: true
        }
      }
    },
    series: [{
      type: 'map',
      roam: true,
      zlevel: 2,
      geoIndex: 0,
      zoom: 1.1,
      tooltip: {
        show: false
      },
      label: {
        normal: {
          show: false
        },
        emphasis: {
          show: false
        }
      }

    }

    ],
    showScatter: true,
    showVisualMap: false,
    visualMap: {
      type: 'continuous',
      left: '3%',
      bottom: '5%',
      min: 0,
      max: 200,
      calculable: true,
      seriesIndex: [0],
      inRange: {
        color: ['#24CFF4', '#1E62AC']
      },
      textStyle: {
        color: '#24CFF4'
      }
    },
    scatterSeries: [
      {
        type: 'scatter',
        coordinateSystem: 'geo',
        symbolSize: 15,
        zlevel: 2,
        symbol: 'circle',
        symbolKeepAspect: true,
        symbolOffset: [0, 0],
        itemStyle: {
          color: 'rgba(255, 145, 0, 1)'
        },
        emphasis: {
          show: false,
          scale: true
        },
        select: {
          show: false

        },
        label: { show: false }
      }
    ]

  }], ['BKRankChart', {
    props: {
      type: 'yellow'
    },
    styles:
{
  '--progressBgColor': '#f8f8f9',
  '--numcolor': '#424e61',
  '--numfontSize': 12,
  '--numfontWeight': 'bold',
  '--titlecolor': '#000',
  '--titlefontSize': 12,
  '--titlefontWeight': 'normal'
}
  }], [
    'BKGaugeChart',
    {
      name: '',
      type: 'gauge',
      center: [50, 50],
      radius: 50,
      clockwise: 1,
      endAngle: -45,
      startAngle: 225,
      max: 100,
      min: 0,
      splitNumber: 10,
      progress: {
        show: true,
        width: 8,
        clip: true,
        itemStyle: {
          color: '#468EFD'
        }
      },
      axisLine: {
        show: true,
        lineStyle: {
          color: '#111F42',
          width: 8
        }
      },
      axisLabel: {
        show: true,
        color: '#4d5bd1',
        distance: 25,
        formatter: function (v) {
          switch (v + '') {
            case '0':
              return '0'
            case '10':
              return '10'
            case '20':
              return '20'
            case '30':
              return '30'
            case '40':
              return '40'
            case '50':
              return '50'
            case '60':
              return '60'
            case '70':
              return '70'
            case '80':
              return '80'
            case '90':
              return '90'
            case '100':
              return '100'
          }
        }
      }, // 刻度标签。
      axisTick: {
        show: true,
        splitNumber: 5,
        distance: -60,
        lineStyle: {
          color: '#468EFD', // 用颜色渐变函数不起作用
          width: 1
        },
        length: 8
      }, // 刻度样式
      splitLine: {
        show: true,
        distance: -70,
        length: 20,
        lineStyle: {
          color: '#468EFD',
          width: 1
        }
      },

      detail: {
        show: true,
        suffix: '%',
        prefix: '',
        offsetCenter: [0, 70],
        fontSize: 18,
        color: '#468EFD',
        fontWeight: 'bold'
      },
      title: { // 标题
        show: true,
        offsetCenter: [0, 46], // x, y，单位px
        color: '#468EFD',
        fontSize: 12, // 表盘上的标题文字大小
        fontWeight: 'normal'
      },
      data: [],
      pointer: {
        show: true,
        length: 75,
        width: 10, // 指针粗细,
        itemStyle: {
          color: '#468EFD'
        }
      },
      animationDuration: 4000
    }

  ], ['BKTimeChart', {
    isVertical: false,
    spotConfig: {
      config: {
        type: 'category',
        dataType: '',
        spotNumber: 3
      },
      styles: {
        spot: {
          borderColor: '',
          borderWidth: 0,
          bgColor: '#00b8fc',
          height: 6,
          width: 6
        },
        selectspot: {
          borderColor: '',
          borderWidth: 0,
          bgColor: '#ff7a75',
          height: 6,
          width: 6
        }
      }
    },
    basicConfig: {
      retention: 3,
      interval: 3,
      carousel: false
    },
    sliderConfig: {
      width: 6,
      length: 80,
      color: '#5686CB',
      selectColor: '#354089'
    },
    upTextConfig: {
      top: 0,
      normal: {
        color: '#7B7171',
        fontSize: 12,
        fontFamily: 'Microsoft YaHei',
        fontWeight: 'normal'
      },
      select: {
        show: false
      }
    },
    downTextConfig: {
      top: 0,
      normal: {
        color: '#181818',
        fontSize: 12,
        fontFamily: 'Microsoft YaHei',
        fontWeight: 'normal'
      },
      select: {
        show: true,
        color: '#4A4545',
        fontSize: 12,
        fontFamily: 'Microsoft YaHei',
        fontWeight: 'normal'
      }
    }
  }], ['3dPieChart', {
    tooltip: {

    },
    legend: {
      show: true,
      alignPosition: 'topCenter'
    },
    color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#909399'],
    backgroundColor: 'transparent',
    xAxis3D: {
      min: -1,
      max: 1
    },
    yAxis3D: {
      min: -1,
      max: 1
    },
    zAxis3D: {
      min: -1,
      max: 1
    },
    grid3D: {
      show: false,
      boxHeight: 20,
      bottom: '50%',
      // environment: '#021041',
      viewControl: {
        distance: 180,
        alpha: 25,
        beta: 30
      }

    }

  }], ['BKRadarChart', {
    tooltip: {

    },
    legend: {
      show: true,
      alignPosition: 'topCenter'
    },
    radar: {

      shape: 'polygon',
      center: [50, 55],
      radius: 70,
      nameGap: 15,

      axisName: {
        show: true,
        color: '#7A7A7A',
        fontWeight: 'normal',
        fontFamily: 'fontFamily',
        fontSize: 12

      },
      axisLine: {
        show: true,
        lineStyle: {
          width: 1,
          color: '#333',
          type: 'solid'
        }
      },
      splitLine: {
        show: true,
        lineStyle: {
          width: 1,
          type: 'dashed',
          color: '#DDD'
        }
      },
      axisTick: {
        show: false,
        lineStyle: {
          width: 1,

          color: '#DDD'
        }
      },
      axisLabel: {
        show: true,
        color: '#7A7A7A',
        fontWeight: 'normal',
        fontFamily: 'fontFamily'
      },
      splitArea: {
        show: false,
        areaStyle: {
          color: ['rgba(250,250,250,0.3)', 'rgba(200,200,200,0.3)']
        }
      }
    },
    series: [{
      type: 'radar',
      symbol: 'circle',
      symbolSize: 4,
      areaStyle: {
        show: false,
        color: null,
        opacity: 0.7
      },
      itemStyle: {
        color: null
      },
      lineStyle: {
        width: 1,
        color: null,
        type: 'solid'
      }
    }, {
      type: 'radar',
      symbol: 'circle',
      symbolSize: 4,
      areaStyle: {
        show: false,
        color: null,
        opacity: 0.7
      },
      itemStyle: {
        color: null
      },
      lineStyle: {
        width: 1,
        color: null,
        type: 'solid'
      }
    }, {
      type: 'radar',
      symbol: 'circle',
      symbolSize: 4,
      areaStyle: {
        show: false,
        color: null,
        opacity: 0.7
      },
      itemStyle: {
        color: null
      },
      lineStyle: {
        width: 1,
        color: null,
        type: 'solid'
      }
    }]

  }],
  ['scatterChart', {
    color: ['#00E4BF', '#FFC72F', '#008FFF', '#3ED848', '#C96765', '#D8E5FA'],
    tooltip: { trigger: 'item' },
    legend: { show: false, alignPosition: 'topCenter' },
    grid: { top: 40, left: 50, right: 40, bottom: 40, containLabel: true },
    xAxis: {
      show: true, type: 'value', scale: true, name: '',
      axisLabel: { show: true }, axisLine: { show: true, lineStyle: { color: '#999' } },
      splitLine: { show: true, lineStyle: { color: '#eee', type: 'dashed' } }
    },
    yAxis: {
      show: true, type: 'value', scale: true, name: '',
      axisLabel: { show: true }, axisLine: { show: false },
      splitLine: { show: true, lineStyle: { color: '#eee', type: 'dashed' } }
    },
    series: [{ type: 'scatter', symbolSize: 14, data: [] }]
  }],
  ['funnelChart', {
    color: ['#00E4BF', '#FFC72F', '#008FFF', '#3ED848', '#C96765', '#D8E5FA'],
    tooltip: { trigger: 'item', formatter: '{b}: {c}' },
    legend: { show: true, alignPosition: 'topCenter' },
    series: [{
      type: 'funnel', left: '10%', right: '10%', top: 40, bottom: 20,
      minSize: '0%', maxSize: '100%', sort: 'descending', gap: 2,
      label: { show: true, position: 'inside' },
      labelLine: { length: 10, lineStyle: { width: 1, type: 'solid' } },
      itemStyle: { borderColor: '#fff', borderWidth: 1 },
      data: []
    }]
  }],
  ['heatmapChart', {
    color: ['#00E4BF', '#FFC72F', '#008FFF', '#3ED848', '#C96765', '#D8E5FA'],
    tooltip: { trigger: 'item' },
    legend: { show: false, alignPosition: 'topCenter' },
    grid: { top: 40, left: 60, right: 30, bottom: 66, containLabel: true },
    xAxis: {
      show: true, type: 'category', data: [], name: '',
      axisLabel: { show: true }, splitArea: { show: true }
    },
    yAxis: {
      show: true, type: 'category', data: [], name: '',
      axisLabel: { show: true }, splitArea: { show: true }
    },
    visualMap: {
      show: true, min: 0, max: 100, calculable: true, orient: 'horizontal',
      left: 'center', bottom: 6, itemWidth: 14, itemHeight: 90,
      inRange: { color: ['#E8F3FF', '#4080FF', '#0E42D2'] }
    },
    series: [{
      type: 'heatmap', data: [], label: { show: true },
      emphasis: { itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0,0,0,.3)' } }
    }]
  }],
  ['sankeyChart', {
    rawEChart: true, noGrid: true,
    color: ['#00E4BF', '#FFC72F', '#008FFF', '#3ED848', '#C96765', '#D8E5FA'],
    tooltip: { trigger: 'item', triggerOn: 'mousemove' },
    series: [{
      type: 'sankey', left: 20, right: 110, top: 24, bottom: 24,
      emphasis: { focus: 'adjacency' },
      lineStyle: { color: 'gradient', curveness: 0.5 },
      label: { fontSize: 12 },
      data: [{ name: '水库' }, { name: '河流' }, { name: '水厂' }, { name: '泵站' }, { name: '生活用水' }, { name: '工业用水' }, { name: '农业用水' }],
      links: [
        { source: '水库', target: '水厂', value: 60 }, { source: '河流', target: '水厂', value: 40 },
        { source: '河流', target: '泵站', value: 30 }, { source: '水厂', target: '生活用水', value: 55 },
        { source: '水厂', target: '工业用水', value: 45 }, { source: '泵站', target: '农业用水', value: 30 }
      ]
    }]
  }],
  ['treemapChart', {
    rawEChart: true, noGrid: true,
    color: ['#00E4BF', '#FFC72F', '#008FFF', '#3ED848', '#C96765', '#D8E5FA'],
    tooltip: { trigger: 'item', formatter: '{b}: {c}' },
    series: [{
      type: 'treemap', roam: false, nodeClick: false, breadcrumb: { show: false },
      label: { show: true, formatter: '{b}' },
      levels: [{ itemStyle: { borderColor: '#fff', borderWidth: 2, gapWidth: 2 } }],
      data: [
        { name: '水利工程', value: 100, children: [{ name: '水库', value: 40 }, { name: '泵站', value: 20 }, { name: '水闸', value: 15 }, { name: '堤防', value: 25 }] },
        { name: '水资源', value: 80, children: [{ name: '地表水', value: 50 }, { name: '地下水', value: 30 }] },
        { name: '水环境', value: 50, children: [{ name: '水质', value: 30 }, { name: '污染源', value: 20 }] }
      ]
    }]
  }],
  ['sunburstChart', {
    rawEChart: true, noGrid: true,
    color: ['#00E4BF', '#FFC72F', '#008FFF', '#3ED848', '#C96765', '#D8E5FA'],
    tooltip: { trigger: 'item' },
    series: [{
      type: 'sunburst', radius: [0, '92%'], label: { rotate: 'radial', minAngle: 8 },
      itemStyle: { borderColor: '#fff', borderWidth: 1 },
      data: [
        { name: '水文', value: 30, children: [{ name: '测站', value: 15 }, { name: '河流', value: 15 }] },
        { name: '水利工程', value: 40, children: [{ name: '水库', value: 20 }, { name: '泵站', value: 10 }, { name: '水闸', value: 10 }] },
        { name: '水环境', value: 30, children: [{ name: '水质', value: 18 }, { name: '污染源', value: 12 }] }
      ]
    }]
  }],
  ['graphChart', {
    rawEChart: true, noGrid: true,
    color: ['#00E4BF', '#FFC72F', '#008FFF', '#3ED848', '#C96765', '#D8E5FA'],
    tooltip: { trigger: 'item' },
    legend: { show: true, alignPosition: 'topCenter' },
    series: [{
      type: 'graph', layout: 'force', roam: true,
      label: { show: true, position: 'right' },
      force: { repulsion: 140, edgeLength: 90, gravity: 0.1 },
      lineStyle: { color: 'source', curveness: 0.12, width: 1.5 },
      emphasis: { focus: 'adjacency' },
      data: [
        { name: '水文测站', symbolSize: 44, category: 0 }, { name: '河流', symbolSize: 34, category: 1 },
        { name: '水库', symbolSize: 30, category: 1 }, { name: '水质指标', symbolSize: 26, category: 2 },
        { name: '泵站', symbolSize: 24, category: 3 }, { name: '污染源', symbolSize: 22, category: 2 }
      ],
      categories: [{ name: '测站' }, { name: '水体' }, { name: '监测' }, { name: '设施' }],
      links: [
        { source: '水文测站', target: '河流' }, { source: '河流', target: '水库' },
        { source: '河流', target: '水质指标' }, { source: '水库', target: '泵站' },
        { source: '水质指标', target: '污染源' }, { source: '水文测站', target: '水质指标' }
      ]
    }]
  }],
  ['themeRiverChart', {
    rawEChart: true, noGrid: true,
    color: ['#00E4BF', '#FFC72F', '#008FFF', '#3ED848', '#C96765', '#D8E5FA'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'line', lineStyle: { color: 'rgba(0,0,0,0.2)', width: 1 } } },
    legend: { show: true, alignPosition: 'topCenter' },
    singleAxis: { type: 'time', top: 40, bottom: 40, axisTick: {}, axisLabel: {}, splitLine: { show: true, lineStyle: { color: ['#eee'] } } },
    series: [{
      type: 'themeRiver', label: { show: false },
      emphasis: { itemStyle: { shadowBlur: 20, shadowColor: 'rgba(0,0,0,0.4)' } },
      data: [
        ['2024-01-01', 30, '降雨量'], ['2024-02-01', 45, '降雨量'], ['2024-03-01', 60, '降雨量'], ['2024-04-01', 40, '降雨量'], ['2024-05-01', 55, '降雨量'], ['2024-06-01', 70, '降雨量'],
        ['2024-01-01', 20, '蒸发量'], ['2024-02-01', 28, '蒸发量'], ['2024-03-01', 35, '蒸发量'], ['2024-04-01', 50, '蒸发量'], ['2024-05-01', 42, '蒸发量'], ['2024-06-01', 38, '蒸发量'],
        ['2024-01-01', 15, '径流量'], ['2024-02-01', 22, '径流量'], ['2024-03-01', 40, '径流量'], ['2024-04-01', 30, '径流量'], ['2024-05-01', 48, '径流量'], ['2024-06-01', 60, '径流量']
      ]
    }]
  }],
  ['boxplotChart', {
    rawEChart: true,
    color: ['#00E4BF', '#FFC72F', '#008FFF', '#3ED848', '#C96765', '#D8E5FA'],
    tooltip: { trigger: 'item' },
    grid: { top: 40, left: 60, right: 30, bottom: 40, containLabel: true },
    xAxis: { type: 'category', data: ['测站A', '测站B', '测站C', '测站D'], boundaryGap: true, axisLabel: { show: true }, splitArea: { show: false }, splitLine: { show: false } },
    yAxis: { type: 'value', name: '水位', splitArea: { show: true }, axisLabel: { show: true } },
    series: [{
      type: 'boxplot', itemStyle: { color: '#D6E4FF', borderColor: '#4080FF', borderWidth: 1.5 },
      data: [[60, 75, 85, 95, 110], [50, 65, 78, 88, 100], [70, 82, 90, 100, 118], [55, 70, 80, 92, 105]]
    }]
  }],
  ['gradeGaugeChart', {
    rawEChart: true, noGrid: true, keepColor: true,
    color: ['#F56C6C', '#FFC72F', '#3ED848'],
    tooltip: { show: false },
    series: [{
      type: 'gauge', center: ['50%', '58%'], radius: '82%',
      startAngle: 200, endAngle: -20, min: 0, max: 100, splitNumber: 10,
      axisLine: { lineStyle: { width: 16, color: [[0.3, '#F56C6C'], [0.7, '#FFC72F'], [1, '#3ED848']] } },
      pointer: { itemStyle: { color: 'auto' }, length: '62%', width: 5 },
      axisTick: { distance: -22, length: 6, lineStyle: { color: '#fff', width: 1 } },
      splitLine: { distance: -22, length: 16, lineStyle: { color: '#fff', width: 2 } },
      axisLabel: { color: 'inherit', distance: 26, fontSize: 11 },
      detail: { valueAnimation: true, formatter: '{value}%', color: 'inherit', fontSize: 24, offsetCenter: [0, '46%'] },
      title: { offsetCenter: [0, '74%'], fontSize: 13, color: '#666' },
      data: [{ value: 68, name: '水质达标率' }]
    }]
  }],
  ['parallelChart', {
    rawEChart: true, noGrid: true,
    color: ['#4080FF', '#3ED848', '#FF9F40'],
    tooltip: { trigger: 'item' },
    legend: { show: true, alignPosition: 'bottomCenter', data: ['测站A', '测站B', '测站C'] },
    parallelAxis: [
      { dim: 0, name: '水位', min: 0, max: 100 },
      { dim: 1, name: '流量', min: 0, max: 200 },
      { dim: 2, name: '浊度', min: 0, max: 50 },
      { dim: 3, name: '水温', min: 0, max: 30 },
      { dim: 4, name: 'PH', min: 0, max: 14 }
    ],
    parallel: { left: 60, right: 90, top: 40, bottom: 44 },
    series: [
      { name: '测站A', type: 'parallel', lineStyle: { width: 2 }, data: [[85, 120, 30, 18, 7.2]] },
      { name: '测站B', type: 'parallel', lineStyle: { width: 2 }, data: [[72, 150, 22, 21, 6.8]] },
      { name: '测站C', type: 'parallel', lineStyle: { width: 2 }, data: [[90, 95, 40, 16, 7.6]] }
    ]
  }],
  ['pictorialBarChart', {
    rawEChart: true,
    color: ['#4080FF'],
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 24, bottom: 36, containLabel: true },
    xAxis: { type: 'category', data: ['测站A', '测站B', '测站C', '测站D', '测站E'], axisTick: { show: false } },
    yAxis: { type: 'value', name: '雨量(mm)', splitLine: { show: false } },
    series: [{
      type: 'pictorialBar', name: '雨量',
      symbol: 'roundRect', symbolSize: ['70%', 6], symbolMargin: 3, symbolRepeat: true,
      itemStyle: { color: '#4080FF' },
      data: [45, 62, 38, 70, 55]
    }]
  }],
  ['candlestickChart', {
    rawEChart: true,
    color: ['#F56C6C', '#3ED848'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { show: false },
    grid: { left: 50, right: 20, top: 24, bottom: 36, containLabel: true },
    xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'], boundaryGap: true },
    yAxis: { type: 'value', name: '水位', scale: true },
    series: [{
      type: 'candlestick', name: '水位',
      itemStyle: { color: '#F56C6C', color0: '#3ED848', borderColor: '#F56C6C', borderColor0: '#3ED848' },
      // [开盘, 收盘, 最低, 最高] = [日初, 日末, 日最低, 日最高]
      data: [[82, 88, 80, 90], [88, 85, 83, 92], [85, 91, 84, 95], [91, 89, 87, 96], [89, 94, 88, 98], [94, 90, 89, 97], [90, 93, 88, 95]]
    }]
  }],
  ['treeChart', {
    rawEChart: true, noGrid: true,
    tooltip: { trigger: 'item', triggerOn: 'mousemove' },
    series: [{
      type: 'tree', left: 40, right: 120, top: 16, bottom: 16,
      symbolSize: 9, orient: 'LR',
      label: { position: 'left', verticalAlign: 'middle', align: 'right', fontSize: 12 },
      leaves: { label: { position: 'right', verticalAlign: 'middle', align: 'left' } },
      lineStyle: { width: 1.2, curveness: 0.5 },
      expandAndCollapse: true, initialTreeDepth: 3,
      emphasis: { focus: 'descendant' },
      data: [{
        name: '水利枢纽',
        children: [
          { name: '水库', children: [{ name: '大坝' }, { name: '溢洪道' }, { name: '输水洞' }] },
          { name: '泵站', children: [{ name: '进水池' }, { name: '机组' }, { name: '出水管' }] },
          { name: '管网', children: [{ name: '干管' }, { name: '支管' }, { name: '阀门井' }] }
        ]
      }]
    }]
  }]

])

export function getParametricEquation (startRatio, endRatio, isSelected, isHovered, k, height) {
  // 计算
  const midRatio = (startRatio + endRatio) / 2
  const startRadian = startRatio * Math.PI * 2
  const endRadian = endRatio * Math.PI * 2
  const midRadian = midRatio * Math.PI * 2
  // 如果只有一个扇形，则不实现选中效果。
  if (startRatio === 0 && endRatio === 1) {
    isSelected = false
  }
  // 通过扇形内径/外径的值，换算出辅助参数 k（默认值 1/3）
  k = typeof k !== 'undefined' ? k : 1 / 3
  // 计算选中效果分别在 x 轴、y 轴方向上的位移（未选中，则位移均为 0）
  const offsetX = isSelected ? Math.cos(midRadian) * 0.1 : 0
  const offsetY = isSelected ? Math.sin(midRadian) * 0.1 : 0
  // 计算高亮效果的放大比例（未高亮，则比例为 1）
  const hoverRate = isHovered ? 1.05 : 1
  // 返回曲面参数方程
  return {
    u: {
      min: -Math.PI,
      max: Math.PI * 3,
      step: Math.PI / 32
    },
    v: {
      min: 0,
      max: Math.PI * 2,
      step: Math.PI / 20
    },
    x: function (u, v) {
      if (u < startRadian) {
        return offsetX + Math.cos(startRadian) * (1 + Math.cos(v) * k) * hoverRate
      }
      if (u > endRadian) {
        return offsetX + Math.cos(endRadian) * (1 + Math.cos(v) * k) * hoverRate
      }
      return offsetX + Math.cos(u) * (1 + Math.cos(v) * k) * hoverRate
    },
    y: function (u, v) {
      if (u < startRadian) {
        return offsetY + Math.sin(startRadian) * (1 + Math.cos(v) * k) * hoverRate
      }
      if (u > endRadian) {
        return offsetY + Math.sin(endRadian) * (1 + Math.cos(v) * k) * hoverRate
      }
      return offsetY + Math.sin(u) * (1 + Math.cos(v) * k) * hoverRate
    },
    z: function (u, v) {
      if (u < -Math.PI * 0.5) {
        return Math.sin(u)
      }
      if (u > Math.PI * 2.5) {
        return Math.sin(u)
      }
      return Math.sin(v) > 0 ? 1 * height : -1
    }
  }
}

/* —— 折线类变体默认 option 的基础(每次返回独立对象,避免共享引用) —— */
function _lineBaseOption () {
  return {
    tooltip: { trigger: 'axis' },
    legend: { show: true, alignPosition: 'topCenter' },
    grid: { top: 50, left: 50, right: 50, bottom: 20, containLabel: true },
    xAxis: { show: true, type: 'category', axisLabel: { show: true }, axisLine: { show: true, lineStyle: { color: '#999' } }, axisTick: { show: true, lineStyle: { width: 1, color: '#DDD' } } },
    yAxis: { show: true, type: 'value', alignTicks: true, splitLine: { show: true, lineStyle: { width: 1, type: 'dashed', color: '#DDD' } }, axisTick: { show: false, lineStyle: { width: 1, color: '#DDD' } }, axisLabel: { show: true }, axisLine: { show: false, lineStyle: { color: '#333' } }, nameTextStyle: { color: '#999' }, splitArea: { show: false } }
  }
}
function _lineSeries (extra) {
  const base = Object.assign({ type: 'line', color: null, showSymbol: true, symbolSize: 4, symbol: 'emptyCircle', lineStyle: { width: 2, color: null }, itemStyle: { color: null }, smooth: false }, extra || {})
  const one = JSON.stringify(base)
  return [JSON.parse(one), JSON.parse(one)]
}
