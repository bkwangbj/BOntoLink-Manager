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
  ['smoothLineChart', { ..._lineBaseOption(), series: _lineSeries({ smooth: true }) }],
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
  }], ['posNegBarChart', {
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
      show: true, min: 0, max: 100, orient: 'horizontal', left: 'center', bottom: 0,
      inRange: { color: ['#E8F3FF', '#4080FF', '#0E42D2'] }
    },
    calendar: {
      top: 50, left: 40, right: 20, cellSize: ['auto', 16], range: '2026',
      itemStyle: { borderWidth: 1, borderColor: '#fff', color: '#F2F3F5' },
      splitLine: { show: true, lineStyle: { color: '#C9CDD4', width: 1 } },
      yearLabel: { show: true }, dayLabel: { firstDay: 1 }, monthLabel: { show: true }
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
