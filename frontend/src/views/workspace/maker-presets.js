const config = {
  layout: [
    {
      isChart: true,
      x: 0,
      y: 0,
      w: 3,
      h: 13,
      i: 'd8b737c8-e5fa-4500-8c0a-0402b10ef911',
      id: 'd8b737c8-e5fa-4500-8c0a-0402b10ef911',
      moved: false,
      tabList: [
        {
          title: '各地区碳汇量对比',
          type: 'BKRankChart',
          explainConfig: {
            show: true,
            text: '单位：万吨',
            position: 'topRight',
            x: 0,
            y: -11,
            textStyle: {
              color: '#FFF',
              fontSize: 12
            }
          },
          img: 'rank-list.png',
          chartComId: '14',
          eventConfig: [
            {
              title: '当数据项被点击时',
              event: 'click',
              isActive: false,
              items: [
                {
                  field: 'number',
                  varField: ''
                },
                {
                  field: 'name',
                  varField: ''
                }
              ]
            }
          ],
          items: [
            {
              label: '数字',
              field: 'number',
              value: 'number'
            },
            {
              label: '文本',
              field: 'name',
              value: 'name'
            }
          ],
          id: 'd8b737c8-e5fa-4500-8c0a-0402b10ef911',
          chartId: 'e876bedc-cf33-4c24-9ed5-423d4d8a0e5e',
          isShowTitle: '1',
          varListener: [],
          dataSourceConfig: {
            type: 'static',
            data: [
              {
                name: '项目1',
                number: 14947.7
              },
              {
                name: '项目2',
                number: 3330.9
              },
              {
                name: '项目3',
                number: 3330
              },
              {
                name: '项目4',
                number: 3290.5
              },
              {
                name: '项目5',
                number: 1542.4
              },
              {
                name: '项目6',
                number: 1276
              },
              {
                name: '项目7',
                number: 1228
              },
              {
                name: '项目8',
                number: 859.5
              }
            ]
          },
          configOption: {
            props: {
              type: 'blue',
              dataType: ''
            },
            styles: {
              '--progressBgColor': '#f8f8f9',
              '--numcolor': '#fff',
              '--numfontSize': 14,
              '--numfontWeight': 'bold',
              '--titlecolor': '#fff',
              '--titlefontSize': 14,
              '--titlefontWeight': 'normal',
              textColor: '#FFF',
              backgroundColor: 'rgba(0, 54, 176, 0.5)',
              borderColor: '#333',
              fontSize: 12,
              colorList: [
                '#5478FD',
                '#7B2CFF',
                '#FFC72F',
                '#008FFF',
                '#3ED848',
                '#C96765'
              ]
            }
          },
          chartTheme: {
            textColor: '#FFF',
            backgroundColor: 'rgba(0, 54, 176, 0.5)',
            borderColor: '#333',
            fontSize: 12,
            colorList: [
              '#5478FD',
              '#7B2CFF',
              '#FFC72F',
              '#008FFF',
              '#3ED848',
              '#C96765'
            ],
            '--progressBgColor': '#f8f8f9',
            '--numcolor': '#fff',
            '--numfontSize': 12,
            '--numfontWeight': 'bold',
            '--titlecolor': '#fff',
            '--titlefontSize': 12,
            '--titlefontWeight': 'normal'
          },
          hookId: 'RankChart20250924160723',
          isChart: true,
          x: 0,
          y: 0,
          w: 3,
          h: 18,
          i: 'd8b737c8-e5fa-4500-8c0a-0402b10ef911',
          moved: false,
          extendedItem: '',
          paddingItem: '',
          hookItem: '',
          queryItem: '',
          hookConfig: {
            show: false,
            script: "\nonMount(function () {\n  console.log('组件已挂载');\n\n  // 获取当前组件\n  var myImage = stage.get('#hookId#');\n  console.log(myImage);\n  if (myImage) {\n\n    // 触发自定义事件\n    myImage.emit('custom-event', {\n      message: '来自Hook的自定义事件'\n    });\n  }\n})\n\n\n// 数据变化时\nonDataChange(function (newData) {\n  console.log('数据已更新:', newData);\n\n});\n\n// 组件更新时\nonUpdate(function () {\n  console.log('组件已更新');\n});\n\n// 组件销毁时\nonDestroy(function () {\n  console.log('组件即将销毁');\n});"
          },
          paddingConfig: {},
          queryConfig: {
            '--right': 0,
            '--top': 3,
            '--width': 120
          }
        }
      ]
    },
    {
      isChart: true,
      x: 3,
      y: 12,
      w: 4,
      h: 9,
      i: '0e9edf01-8243-4ce8-8156-44419586a796',
      id: '0e9edf01-8243-4ce8-8156-44419586a796',
      moved: false,
      tabList: [
        {
          title: '碳汇全口径数据合格率',
          type: 'BKGaugeChart',
          explainConfig: {
            show: false,
            text: '',
            position: 'topLeft',
            x: 0,
            y: 0,
            textStyle: {
              color: '#FFF',
              fontSize: 12
            }
          },
          img: 'guage.png',
          chartComId: '15',
          eventConfig: [
            {
              title: '当数据项被点击时',
              event: 'click',
              isActive: false,
              items: [
                {
                  field: 'name',
                  varField: ''
                },
                {
                  field: 'value',
                  varField: ''
                }
              ]
            }
          ],
          items: [
            {
              label: 'name',
              field: 'name',
              value: 'name'
            },
            {
              label: 'value',
              field: 'value',
              value: 'value'
            }
          ],
          chartId: 'f68d61ca-a4e9-48c6-b968-06bda261bc79',
          varListener: [],
          dataSourceConfig: {
            type: 'static',
            data: {
              name: '任务完成度',
              value: '70'
            }
          },
          configOption: {
            center: [
              50,
              50
            ],
            min: 0,
            max: 100,
            startAngle: 225,
            endAngle: -45,
            clockwise: 1,
            splitNumber: 5,
            radius: 50,
            axisLabel: {
              show: true,
              distance: 80,
              color: '#fff',
              fontSize: 12,
              fontFamily: 'Microsoft YaHei',
              fontWeight: 'normal'
            },
            detail: {
              show: true,
              suffix: '%',
              prefix: '',
              offsetCenter: [
                0,
                70
              ],
              fontSize: 18,
              color: '#FFF',
              fontWeight: 'bold',
              dataType: ''
            },
            title: {
              show: true,
              offsetCenter: [
                0,
                90
              ],
              color: '#FFF',
              fontSize: 12,
              fontWeight: 'normal'
            },
            splitLine: {
              show: false,
              distance: -70,
              length: 20,
              lineStyle: {
                color: '#468EFD',
                width: 1
              }
            },
            axisTick: {
              show: true,
              splitNumber: 1,
              distance: 10,
              lineStyle: {
                width: 1,
                color: '#fff',
                type: 'dotted'
              },
              length: 2
            },
            progress: {
              show: true,
              width: 8,
              itemStyle: {
                color: '#468EFD'
              },
              clip: true
            },
            axisLine: {
              lineStyle: {
                color: '#111F42',
                width: 8
              },
              show: true
            },
            pointer: {
              show: true,
              length: 55,
              width: 5,
              itemStyle: {
                color: '#468EFD'
              }
            },
            name: '',
            type: 'gauge',
            data: [],
            animationDuration: 4000
          },
          chartTheme: {
            textColor: '#FFF',
            backgroundColor: 'rgba(0, 54, 176, 0.5)',
            borderColor: '#333',
            fontSize: 12,
            colorList: [
              '#5478FD',
              '#7B2CFF',
              '#FFC72F',
              '#008FFF',
              '#3ED848',
              '#C96765'
            ],
            configStyle: {
              tooltip: {
                show: true,
                backgroundColor: 'rgba(0, 54, 176, 0.5)',
                padding: 5,
                borderWidth: 0,
                borderColor: '#000',
                textStyle: {
                  color: '#FFF',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'normal'
                }
              },
              legend: {
                show: true,
                itemGap: 10,
                itemHeight: 15,
                itemWidth: 25,
                textStyle: {
                  color: '#FFF',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'normal'
                }
              },
              pieStyle: {
                label: {
                  show: false,
                  position: 'outside',
                  color: '#FFF',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'normal'
                },
                labelLine: {
                  show: false,
                  length: 5,
                  length2: 10,
                  lineStyle: {
                    width: 1
                  }
                }
              }
            },
            activeColor: '#468EFD',
            bgColor: '#111F42'
          },
          hookId: 'GaugeChart20250925133016',
          isShowTitle: '1',
          id: '0e9edf01-8243-4ce8-8156-44419586a796',
          isChart: true,
          x: 3,
          y: 12,
          w: 4,
          h: 9,
          i: '0e9edf01-8243-4ce8-8156-44419586a796',
          moved: false,
          extendedItem: '',
          paddingItem: '',
          hookItem: '',
          queryItem: '',
          hookConfig: {
            show: false,
            script: "\nonMount(function () {\n  console.log('组件已挂载');\n\n  // 获取当前组件\n  var myImage = stage.get('#hookId#');\n  console.log(myImage);\n  if (myImage) {\n\n    // 触发自定义事件\n    myImage.emit('custom-event', {\n      message: '来自Hook的自定义事件'\n    });\n  }\n})\n\n\n// 数据变化时\nonDataChange(function (newData) {\n  console.log('数据已更新:', newData);\n\n});\n\n// 组件更新时\nonUpdate(function () {\n  console.log('组件已更新');\n});\n\n// 组件销毁时\nonDestroy(function () {\n  console.log('组件即将销毁');\n});"
          },
          paddingConfig: {},
          queryConfig: {
            '--right': 0,
            '--top': 3,
            '--width': 120
          }
        }
      ]
    },
    {
      isChart: true,
      x: 3,
      y: 0,
      w: 6,
      h: 12,
      i: '85bdc388-5b8d-4836-8347-0e177327dc6c',
      id: '85bdc388-5b8d-4836-8347-0e177327dc6c',
      moved: false,
      tabList: [
        {
          title: '立体地图',
          type: 'BKMapChart',
          branchType: 'solidtMap',
          explainConfig: {
            show: false,
            text: '',
            position: 'topLeft',
            x: 0,
            y: 0,
            textStyle: {
              color: '#FFF',
              fontSize: 12
            }
          },
          img: '3d-map.png',
          chartComId: '20',
          mapName: '320000',
          isDeep: true,
          eventConfig: [
            {
              title: '当数据项被点击时',
              event: 'click',
              isActive: false,
              items: [
                {
                  field: 'name',
                  varField: ''
                },
                {
                  field: 'value',
                  varField: ''
                }
              ]
            }
          ],
          items: [
            {
              label: 'name',
              field: 'name',
              value: 'name'
            },
            {
              label: 'value',
              field: 'value',
              value: 'value'
            },
            {
              label: 'code',
              field: 'code',
              value: 'code'
            }
          ],
          chartId: '96d63547-572b-4169-b21b-1694067ce63e',
          varListener: [],
          dataSourceConfig: {
            type: 'static',
            data: [
              {
                name: '北京市',
                value: 142,
                code: 110000
              },
              {
                name: '天津市',
                value: 233,
                code: 120000
              },
              {
                name: '上海市',
                value: 466
              },
              {
                name: '重庆市',
                value: 494
              },
              {
                name: '河北',
                value: 498
              },
              {
                name: '河南',
                value: 389
              },
              {
                name: '云南',
                value: 89
              },
              {
                name: '辽宁',
                value: 152
              },
              {
                name: '黑龙江',
                value: 149
              },
              {
                name: '湖南',
                value: 210
              },
              {
                name: '安徽',
                value: 345
              },
              {
                name: '山东',
                value: 386
              },
              {
                name: '新疆',
                value: 174
              },
              {
                name: '江苏',
                value: 479
              },
              {
                name: '浙江',
                value: 328
              },
              {
                name: '江西',
                value: 265
              },
              {
                name: '湖北',
                value: 362
              },
              {
                name: '广西',
                value: 161
              },
              {
                name: '甘肃',
                value: 223
              },
              {
                name: '山西',
                value: 214
              },
              {
                name: '内蒙古',
                value: 454
              },
              {
                name: '陕西',
                value: 41
              },
              {
                name: '吉林',
                value: 245
              },
              {
                name: '福建',
                value: 486
              },
              {
                name: '贵州',
                value: 59
              },
              {
                name: '广东',
                value: 168
              },
              {
                name: '青海',
                value: 81
              },
              {
                name: '西藏',
                value: 0
              },
              {
                name: '四川',
                value: 481
              },
              {
                name: '宁夏',
                value: 126
              },
              {
                name: '海南',
                value: 227
              },
              {
                name: '台湾',
                value: 99
              },
              {
                name: '香港',
                value: 94
              },
              {
                name: '澳门',
                value: 317
              }
            ]
          },
          configOption: {
            tooltip: {
              show: true,
              hideDelay: 100,
              backgroundColor: 'rgba(0, 54, 176, 0.5)',
              trigger: 'item',
              triggerOn: 'mousemove',
              padding: 5,
              borderWidth: 0,
              borderColor: '#000',
              textStyle: {
                color: '#FFF',
                fontSize: 12,
                fontFamily: 'Microsoft YaHei',
                fontWeight: 'normal'
              }
            },
            geo: {
              roam: false,
              labelContent: false,
              label: {
                show: true,
                color: '#fff'
              },
              itemStyle: {
                areaColor: '#2d66b3',
                borderColor: '#70e9f2',
                borderWidth: 1,
                shadowBlur: 15,
                shadowColor: 'rgb(58,115,192)',
                shadowOffsetX: 7,
                shadowOffsetY: 6
              },
              emphasis: {
                disabled: false,
                show: true,
                label: {
                  show: true,
                  color: '#f75a00'
                },
                itemStyle: {
                  areaColor: '#6fe9f1',
                  borderColor: 'rgba(36, 66, 181, 1)',
                  borderWidth: 1
                }
              },
              select: {
                disabled: true,
                label: {
                  show: true,
                  color: '#333'
                },
                itemStyle: {
                  areaColor: '#fff',
                  borderColor: '',
                  borderWidth: 0
                }
              },
              map: 'china',
              zoom: 1.1,
              selectedMode: true,
              zlevel: 1
            },
            showScatter: false,
            showVisualMap: true,
            scatterSeries: [
              {
                symbolSize: 15,
                symbol: 'circle',
                symbolKeepAspect: true,
                symbolOffset: [
                  0,
                  0
                ],
                itemStyle: {
                  color: 'rgba(255, 145, 0, 1)'
                },
                label: {
                  show: false
                },
                emphasis: {
                  show: false,
                  scale: true
                },
                select: {
                  show: false
                },
                type: 'scatter',
                coordinateSystem: 'geo',
                zlevel: 2
              }
            ],
            visualMap: {
              max: 500,
              min: 0,
              inRange: {
                color: [
                  '#24CFF4',
                  '#1E62AC'
                ]
              },
              type: 'continuous',
              left: '3%',
              bottom: '5%',
              calculable: true,
              seriesIndex: [
                0
              ],
              textStyle: {
                color: '#24CFF4'
              }
            },
            series: [
              {
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
            ],
            animation: false,
            customConfig: {
              show: false,
              config: 'return option'
            }
          },
          chartTheme: {
            textColor: '#FFF',
            backgroundColor: 'rgba(0, 54, 176, 0.5)',
            borderColor: '#53D9FF',
            fontSize: 12,
            colorList: [
              '#5478FD',
              '#7B2CFF',
              '#FFC72F',
              '#008FFF',
              '#3ED848',
              '#C96765'
            ],
            tooltipBackgroundColor: 'rgba(0, 54, 176, 0.5)',
            tooltipBorderColor: '#000',
            visualMapTextColor: '#24CFF4',
            tooltipColor: '#ffffff',
            eTextColor: '#f75a00',
            sTextColor: '#333',
            areaColor: '#24CFF4',
            eAreaColor: '#8dd7fc',
            sAreaColor: '#fff',
            eBorderColor: '#53D9FF',
            sBorderColor: '',
            mapColorList: [
              '#24CFF4',
              '#1E62AC'
            ],
            configStyle: {
              tooltip: {
                show: true,
                backgroundColor: 'rgba(0, 54, 176, 0.5)',
                padding: 5,
                borderWidth: 0,
                borderColor: '#000',
                textStyle: {
                  color: '#FFF',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'normal'
                }
              },
              map: {
                select: {
                  itemStyle: {
                    areaColor: '#fff',
                    borderColor: '',
                    borderWidth: 0
                  },
                  label: {
                    show: true,
                    color: '#333'
                  }
                },
                emphasis: {
                  itemStyle: {
                    areaColor: '#8dd7fc',
                    borderColor: '',
                    borderWidth: 0
                  },
                  label: {
                    show: true,
                    color: '#f75a00'
                  }
                },
                normal: {
                  itemStyle: {
                    areaColor: '#24CFF4',
                    borderColor: '',
                    borderWidth: 0
                  },
                  label: {
                    show: true,
                    color: '#fff'
                  }
                }
              }
            }
          },
          hookId: 'MapChart20250924161010',
          isShowTitle: '0',
          id: '85bdc388-5b8d-4836-8347-0e177327dc6c',
          isChart: true,
          x: 3,
          y: 0,
          w: 6,
          h: 18,
          i: '85bdc388-5b8d-4836-8347-0e177327dc6c',
          moved: false,
          seriesName: '',
          deepLevel: '3',
          extendedItem: '',
          paddingItem: '',
          hookItem: '',
          queryItem: '',
          hookConfig: {
            show: false,
            script: "\nonMount(function () {\n  console.log('组件已挂载');\n\n  // 获取当前组件\n  var myImage = stage.get('#hookId#');\n  console.log(myImage);\n  if (myImage) {\n\n    // 触发自定义事件\n    myImage.emit('custom-event', {\n      message: '来自Hook的自定义事件'\n    });\n  }\n})\n\n\n// 数据变化时\nonDataChange(function (newData) {\n  console.log('数据已更新:', newData);\n\n});\n\n// 组件更新时\nonUpdate(function () {\n  console.log('组件已更新');\n});\n\n// 组件销毁时\nonDestroy(function () {\n  console.log('组件即将销毁');\n});"
          },
          paddingConfig: {},
          queryConfig: {
            '--right': 0,
            '--top': 3,
            '--width': 120
          }
        }
      ]
    },
    {
      isChart: true,
      x: 0,
      y: 13,
      w: 3,
      h: 8,
      i: '356faf9a-10d9-4f36-9162-e08b71c423cb',
      id: '356faf9a-10d9-4f36-9162-e08b71c423cb',
      moved: false,
      tabList: [
        {
          title: '各类型碳汇量对比',
          type: 'BKPieChart',
          img: 'light-pie.png',
          explainConfig: {
            show: false,
            text: '',
            position: 'topLeft',
            x: 0,
            y: 0,
            textStyle: {
              color: '#FFF',
              fontSize: 12
            }
          },
          chartComId: '18',
          branchType: 'ringChart2',
          eventConfig: [
            {
              title: '当数据项被点击时',
              event: 'click',
              isActive: false,
              items: [
                {
                  field: 'name',
                  varField: ''
                },
                {
                  field: 'value',
                  varField: ''
                }
              ]
            }
          ],
          items: [
            {
              label: 'name',
              field: 'name',
              value: 'name'
            },
            {
              label: 'value',
              field: 'value',
              value: 'value'
            }
          ],
          chartId: 'b7cc4a41-e44f-4571-81d9-885b6ff36e39',
          varListener: [],
          dataSourceConfig: {
            type: 'static',
            data: [
              {
                name: '男',
                value: 11
              },
              {
                name: '女',
                value: 5
              }
            ]
          },
          configOption: {
            color: [
              '#5478FD',
              '#7B2CFF',
              '#FFC72F',
              '#008FFF',
              '#3ED848',
              '#C96765'
            ],
            position: {
              radius: [
                65,
                75
              ],
              center: [
                50,
                50
              ]
            },
            tooltip: {
              trigger: 'item',
              show: true,
              backgroundColor: 'rgba(0, 54, 176, 0.5)',
              padding: 5,
              borderWidth: 0,
              borderColor: '#000',
              textStyle: {
                color: '#FFF',
                fontSize: 12,
                fontFamily: 'Microsoft YaHei',
                fontWeight: 'normal'
              },
              hideDelay: 100,
              triggerOn: 'mousemove'
            },
            legend: {
              show: true,
              alignPosition: 'topCenter',
              itemGap: 10,
              itemHeight: 15,
              itemWidth: 25,
              textStyle: {
                color: '#FFF',
                fontSize: 12,
                fontFamily: 'Microsoft YaHei',
                fontWeight: 'normal'
              },
              icon: '',
              type: 'plain',
              top: 0,
              left: 0,
              orient: 'horizontal'
            },
            series: [],
            grid: {},
            customConfig: {
              show: false,
              config: 'return option'
            },
            legendConfig: {
              show: false,
              unit: '',
              dataType: '',
              header: [
                '',
                '',
                ''
              ],
              position: 'topLeft',
              showPerc: false,
              percNumber: 2,
              unitPosition: 'onHeader',
              x: 0,
              y: 0
            }
          },
          chartTheme: {
            textColor: '#FFF',
            backgroundColor: 'rgba(0, 54, 176, 0.5)',
            borderColor: '#333',
            fontSize: 12,
            colorList: [
              '#5478FD',
              '#7B2CFF',
              '#FFC72F',
              '#008FFF',
              '#3ED848',
              '#C96765'
            ],
            configStyle: {
              tooltip: {
                show: true,
                backgroundColor: 'rgba(0, 54, 176, 0.5)',
                padding: 5,
                borderWidth: 0,
                borderColor: '#000',
                textStyle: {
                  color: '#FFF',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'normal'
                }
              },
              legend: {
                show: true,
                itemGap: 10,
                itemHeight: 15,
                itemWidth: 25,
                textStyle: {
                  color: '#FFF',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'normal'
                }
              },
              pieStyle: {
                label: {
                  show: false,
                  position: 'outside',
                  color: '#FFF',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'normal'
                },
                labelLine: {
                  show: false,
                  length: 5,
                  length2: 10,
                  lineStyle: {
                    width: 1
                  }
                }
              }
            }
          },
          hookId: 'PieChart20250924160837',
          isShowTitle: '1',
          id: '356faf9a-10d9-4f36-9162-e08b71c423cb',
          isChart: true,
          x: 0,
          y: 18,
          w: 3,
          h: 8,
          i: '356faf9a-10d9-4f36-9162-e08b71c423cb',
          moved: false,
          extendedItem: '',
          paddingItem: '',
          hookItem: '',
          queryItem: '',
          hookConfig: {
            show: false,
            script: "\nonMount(function () {\n  console.log('组件已挂载');\n\n  // 获取当前组件\n  var myImage = stage.get('#hookId#');\n  console.log(myImage);\n  if (myImage) {\n\n    // 触发自定义事件\n    myImage.emit('custom-event', {\n      message: '来自Hook的自定义事件'\n    });\n  }\n})\n\n\n// 数据变化时\nonDataChange(function (newData) {\n  console.log('数据已更新:', newData);\n\n});\n\n// 组件更新时\nonUpdate(function () {\n  console.log('组件已更新');\n});\n\n// 组件销毁时\nonDestroy(function () {\n  console.log('组件即将销毁');\n});"
          },
          paddingConfig: {},
          queryConfig: {
            '--right': 0,
            '--top': 3,
            '--width': 120
          }
        }
      ]
    },
    {
      isChart: true,
      x: 7,
      y: 12,
      w: 2,
      h: 9,
      i: 'f40cbe33-a124-4631-9480-a61b80d0092b',
      id: 'f40cbe33-a124-4631-9480-a61b80d0092b',
      moved: false,
      tabList: [
        {
          title: '碳汇效益评估',
          img: 'html.png',
          explainConfig: {
            show: false,
            text: '',
            position: 'topLeft',
            x: 0,
            y: 0,
            textStyle: {
              color: '#FFF',
              fontSize: 12
            }
          },
          type: 'BKCodeChart',
          eventConfig: [
            {
              title: '当数据项被点击时',
              event: 'click',
              isActive: false,
              items: []
            }
          ],
          chartComId: '5',
          isBasic: true,
          id: 'f40cbe33-a124-4631-9480-a61b80d0092b',
          chartId: 'f76f936b-12bb-4bef-9c93-d2d1225b05b4',
          isShowTitle: '1',
          varListener: [],
          dataSourceConfig: {
            type: 'static',
            data: {
              name: '测试',
              num: 3
            }
          },
          configOption: {},
          chartTheme: {
            textColor: '#FFF',
            backgroundColor: 'rgba(0, 54, 176, 0.5)',
            borderColor: '#333',
            fontSize: 12,
            colorList: [
              '#5478FD',
              '#7B2CFF',
              '#FFC72F',
              '#008FFF',
              '#3ED848',
              '#C96765'
            ],
            configStyle: {
              tooltip: {
                show: true,
                backgroundColor: 'rgba(0, 54, 176, 0.5)',
                padding: 5,
                borderWidth: 0,
                borderColor: '#000',
                textStyle: {
                  color: '#FFF',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'normal'
                }
              },
              legend: {
                show: true,
                itemGap: 10,
                itemHeight: 15,
                itemWidth: 25,
                textStyle: {
                  color: '#FFF',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'normal'
                }
              },
              pieStyle: {
                label: {
                  show: false,
                  position: 'outside',
                  color: '#FFF',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'normal'
                },
                labelLine: {
                  show: false,
                  length: 5,
                  length2: 10,
                  lineStyle: {
                    width: 1
                  }
                }
              }
            }
          },
          hookId: 'CodeChart20250924160914',
          code: '<div class="h-full th-pg-content" style="overflow-y: auto;">\r\n    <div class="th-pg-item">\r\n        <div class="th-pg-item-title">\r\n            <img src="@/assets/images/icon-jiantou.png">\r\n            <span>生态效益</span>\r\n        </div>\r\n        <div class="th-pg-item-text">\r\n            碳汇增加有助于降低大气中二氧化碳浓度，为野生动植物提供适宜的生存环境。\r\n        </div>\r\n    </div>\r\n    <div class="th-pg-item">\r\n        <div class="th-pg-item-title">\r\n            <img src="@/assets/images/icon-jiantou.png">\r\n            <span>社会效益</span>\r\n        </div>\r\n        <div class="th-pg-item-text">\r\n            水土保持碳汇项目在实施过程中，可带动当地农民参与工程建设和植被管护，增加就业岗位。\r\n        </div>\r\n    </div>\r\n    <div class="th-pg-item">\r\n        <div class="th-pg-item-title">\r\n            <img src="@/assets/images/icon-jiantou.png">\r\n            <span>经济效益</span>\r\n        </div>\r\n        <div class="th-pg-item-text">\r\n            碳汇项目共获得了 xxx 万元\r\n        </div>\r\n    </div>\r\n</div>',
          isChart: true,
          x: 7,
          y: 18,
          w: 2,
          h: 8,
          i: 'f40cbe33-a124-4631-9480-a61b80d0092b',
          moved: false,
          extendedItem: '',
          paddingItem: '',
          hookItem: '',
          queryItem: '',
          hookConfig: {
            show: false,
            script: "\nonMount(function () {\n  console.log('组件已挂载');\n\n  // 获取当前组件\n  var myImage = stage.get('#hookId#');\n  console.log(myImage);\n  if (myImage) {\n\n    // 触发自定义事件\n    myImage.emit('custom-event', {\n      message: '来自Hook的自定义事件'\n    });\n  }\n})\n\n\n// 数据变化时\nonDataChange(function (newData) {\n  console.log('数据已更新:', newData);\n\n});\n\n// 组件更新时\nonUpdate(function () {\n  console.log('组件已更新');\n});\n\n// 组件销毁时\nonDestroy(function () {\n  console.log('组件即将销毁');\n});"
          },
          paddingConfig: {},
          queryConfig: {
            '--right': 0,
            '--top': 3,
            '--width': 120
          }
        }
      ]
    },
    {
      isChart: true,
      isTabLayout: false,
      x: 9,
      y: 0,
      w: 3,
      h: 11,
      i: 'efd48923-ccdf-4ffa-b08e-2549dbf4b557',
      id: 'efd48923-ccdf-4ffa-b08e-2549dbf4b557',
      moved: false,
      tabList: [
        {
          title: '水土保持碳汇能力评估',
          type: 'BKPieChart',
          explainConfig: {
            show: false,
            text: '',
            position: 'topLeft',
            x: 0,
            y: 0,
            textStyle: {
              color: '#FFF',
              fontSize: 12
            }
          },
          img: '3d-pie.png',
          chartComId: '17',
          branchType: '3dPieChart',
          eventConfig: [
            {
              title: '当数据项被点击时',
              event: 'click',
              isActive: false,
              items: [
                {
                  field: 'name',
                  varField: ''
                },
                {
                  field: 'value',
                  varField: ''
                }
              ]
            }
          ],
          items: [
            {
              label: 'name',
              field: 'name',
              value: 'name'
            },
            {
              label: 'value',
              field: 'value',
              value: 'value'
            }
          ],
          id: 'efd48923-ccdf-4ffa-b08e-2549dbf4b557',
          chartId: 'e207775c-eca7-48ca-8090-792a699b48f6',
          isShowTitle: '1',
          varListener: [],
          dataSourceConfig: {
            type: 'static',
            data: [
              {
                name: '男',
                value: 11
              },
              {
                name: '女',
                value: 5
              }
            ]
          },
          configOption: {
            tooltip: {
              show: true,
              backgroundColor: 'rgba(0, 54, 176, 0.5)',
              padding: 5,
              borderWidth: 0,
              borderColor: '#000',
              textStyle: {
                color: '#FFF',
                fontSize: 12,
                fontFamily: 'Microsoft YaHei',
                fontWeight: 'normal'
              },
              hideDelay: 100,
              trigger: 'item',
              triggerOn: 'mousemove'
            },
            legend: {
              show: false,
              alignPosition: 'topCenter',
              itemGap: 10,
              itemHeight: 15,
              itemWidth: 25,
              textStyle: {
                color: '#000',
                fontSize: 12,
                fontFamily: 'Microsoft YaHei',
                fontWeight: 'normal'
              },
              icon: '',
              type: 'plain',
              top: 0,
              left: 0,
              orient: 'horizontal'
            },
            color: [
              '#0090FF',
              '#00E5C0',
              '#FFD96D',
              '#FF9C57',
              '#FF5757',
              '#C96765'
            ],
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
              viewControl: {
                distance: 270,
                alpha: 25,
                beta: 60
              }
            },
            series: [
              {
                name: '男',
                type: 'surface',
                parametric: true,
                wireframe: {
                  show: false
                },
                pieData: {
                  name: '男',
                  value: 2.0625,
                  raw: 11,
                  percentage: 69,
                  color: '',
                  startRatio: 0,
                  endRatio: 0.6875
                },
                pieStatus: {
                  selected: false,
                  hovered: false,
                  k: 0.3333333333333333
                },
                itemStyle: {
                  opacity: 0.8,
                  color: '#5478FD'
                },
                parametricEquation: {
                  u: {
                    min: -3.141592653589793,
                    max: 9.42477796076938,
                    step: 0.09817477042468103
                  },
                  v: {
                    min: 0,
                    max: 6.283185307179586,
                    step: 0.15707963267948966
                  }
                },
                label: {
                  color: '#FFF'
                }
              },
              {
                name: '女',
                type: 'surface',
                parametric: true,
                wireframe: {
                  show: false
                },
                pieData: {
                  name: '女',
                  value: 0.9375,
                  raw: 5,
                  percentage: 31,
                  color: '',
                  startRatio: 0.6875,
                  endRatio: 1
                },
                pieStatus: {
                  selected: false,
                  hovered: false,
                  k: 0.3333333333333333
                },
                itemStyle: {
                  opacity: 0.8,
                  color: '#7B2CFF'
                },
                parametricEquation: {
                  u: {
                    min: -3.141592653589793,
                    max: 9.42477796076938,
                    step: 0.09817477042468103
                  },
                  v: {
                    min: 0,
                    max: 6.283185307179586,
                    step: 0.15707963267948966
                  }
                },
                label: {
                  color: '#FFF'
                }
              }
            ],
            grid: {},
            customConfig: {
              show: false,
              config: 'return option'
            },
            legendConfig: {
              show: true,
              unit: '',
              dataType: '',
              header: [
                '',
                '',
                ''
              ],
              position: 'bottomCenter',
              showPerc: true,
              percNumber: 2,
              unitPosition: 'onHeader',
              x: 0,
              y: 0
            },
            internalDiameterRatio: 0.6
          },
          chartTheme: {
            textColor: '#FFF',
            backgroundColor: 'rgba(0, 54, 176, 0.5)',
            borderColor: '#333',
            fontSize: 12,
            colorList: [
              '#5478FD',
              '#7B2CFF',
              '#FFC72F',
              '#008FFF',
              '#3ED848',
              '#C96765'
            ],
            configStyle: {
              tooltip: {
                show: true,
                backgroundColor: 'rgba(0, 54, 176, 0.5)',
                padding: 5,
                borderWidth: 0,
                borderColor: '#000',
                textStyle: {
                  color: '#FFF',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'normal'
                }
              },
              legend: {
                show: true,
                itemGap: 10,
                itemHeight: 15,
                itemWidth: 25,
                textStyle: {
                  color: '#FFF',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'normal'
                }
              },
              pieStyle: {
                label: {
                  show: false,
                  position: 'outside',
                  color: '#FFF',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'normal'
                },
                labelLine: {
                  show: false,
                  length: 5,
                  length2: 10,
                  lineStyle: {
                    width: 1
                  }
                }
              }
            }
          },
          hookId: 'PieChart20250924160949',
          isChart: true,
          isTabLayout: false,
          x: 9,
          y: 0,
          w: 3,
          h: 9,
          i: 'efd48923-ccdf-4ffa-b08e-2549dbf4b557',
          moved: false,
          extendedItem: '',
          paddingItem: '',
          hookItem: '',
          queryItem: '',
          hookConfig: {
            show: false,
            script: "\nonMount(function () {\n  console.log('组件已挂载');\n\n  // 获取当前组件\n  var myImage = stage.get('#hookId#');\n  console.log(myImage);\n  if (myImage) {\n\n    // 触发自定义事件\n    myImage.emit('custom-event', {\n      message: '来自Hook的自定义事件'\n    });\n  }\n})\n\n\n// 数据变化时\nonDataChange(function (newData) {\n  console.log('数据已更新:', newData);\n\n});\n\n// 组件更新时\nonUpdate(function () {\n  console.log('组件已更新');\n});\n\n// 组件销毁时\nonDestroy(function () {\n  console.log('组件即将销毁');\n});"
          },
          paddingConfig: {},
          queryConfig: {
            '--right': 0,
            '--top': 3,
            '--width': 120
          }
        }
      ]
    },
    {
      isChart: true,
      isTabLayout: false,
      x: 9,
      y: 11,
      w: 3,
      h: 10,
      i: 'd37093bc-122c-4f00-beec-fd9b71a02533',
      id: 'd37093bc-122c-4f00-beec-fd9b71a02533',
      moved: false,
      tabList: [
        {
          title: '表格',
          type: 'BKTableChart',
          branchType: 'commonTable',
          explainConfig: {
            show: false,
            text: '',
            position: 'topLeft',
            x: 0,
            y: 0,
            textStyle: {
              color: '#FFF',
              fontSize: 12
            }
          },
          img: 'table.png',
          chartComId: '1',
          eventConfig: [
            {
              title: '当单元格被点击时',
              event: 'cellClick',
              isActive: false,
              items: []
            }
          ],
          id: 'd37093bc-122c-4f00-beec-fd9b71a02533',
          chartId: '5ac9f817-cc10-42ff-8fba-06928c6a46af',
          isShowTitle: '0',
          varListener: [],
          dataSourceConfig: {
            type: 'static',
            data: [
              {
                name: '姓名',
                value: 11,
                _X_ROW_KEY: 'row_40'
              },
              {
                name: '姓名',
                value: 5,
                _X_ROW_KEY: 'row_41'
              }
            ]
          },
          configOption: {
            props: {
              seqWidth: 60,
              hasPager: false,
              stripe: false,
              pageSize: 20,
              isStatic: false,
              highlightCurrentRow: false,
              showHeader: true,
              hasSeq: true,
              headerAlign: 'center',
              border: 'full',
              align: 'center',
              showHeaderOverflow: true
            },
            styles: {
              '--borderColor': '#0F57E5',
              '--stripeColor': '#0c3991',
              '--bodyBg': 'rgb(26 114 192 / 26%)',
              '--tablecellheight': 48,
              '--headercellheight': 48,
              '--headerBg': 'rgb(26 114 192 / 26%)',
              '--rowCurrentColor': '#53b0f7',
              '--bodycolor': '#FFF',
              '--bodyfontSize': 12,
              '--bodyfontFamily': 'Microsoft YaHei',
              '--bodyfontWeight': 'normal',
              '--headercolor': '#ffffff',
              '--headerfontSize': 12,
              '--headerfontFamily': 'Microsoft YaHei',
              '--headerfontWeight': 'normal'
            },
            columns: [
              {
                width: null,
                minWidth: null,
                sourceType: '',
                dataType: '',
                align: 'center',
                visible: true,
                sortable: false,
                headerAlign: 'center',
                columnsConfig: '',
                field: 'name',
                title: '列1'
              },
              {
                width: null,
                minWidth: null,
                sourceType: '',
                dataType: '',
                align: 'center',
                visible: true,
                sortable: false,
                headerAlign: 'center',
                columnsConfig: '',
                field: 'value',
                title: '列2'
              }
            ],
            filterDataEmpty: false
          },
          chartTheme: {
            textColor: '#FFF',
            backgroundColor: 'rgba(0, 54, 176, 0.5)',
            borderColor: '#333',
            fontSize: 12,
            colorList: [
              '#5478FD',
              '#7B2CFF',
              '#FFC72F',
              '#008FFF',
              '#3ED848',
              '#C96765'
            ],
            configStyle: {
              table: {
                body: {
                  styles: {
                    '--borderColor': '#0F57E5',
                    '--stripeColor': '#0c3991',
                    '--bodyBg': '#0C2D6F',
                    '--tablecellheight': 48,
                    '--bodyfontFamily': 'Microsoft YaHei',
                    '--bodyfontWeight': 'normal',
                    '--rowCurrentColor': '#53b0f7',
                    '--bodyfontSize': 12,
                    '--bodycolor': '#FFF'
                  },
                  props: {
                    seqWidth: 60,
                    stripe: false,
                    highlightCurrentRow: false,
                    hasSeq: true,
                    border: 'outer',
                    align: 'center'
                  }
                },
                header: {
                  styles: {
                    '--headercolor': '#ffffff',
                    '--headerfontSize': 12,
                    '--headercellheight': 48,
                    '--headerBg': '#1153D6',
                    '--headerfontFamily': 'Microsoft YaHei',
                    '--headerfontWeight': 'normal'
                  },
                  props: {
                    showHeader: true,
                    headerAlign: 'center'
                  }
                }
              }
            },
            '--borderColor': '#0F57E5',
            '--stripeColor': '#0c3991',
            '--bodyBg': '#0C2D6F',
            '--headerBg': '#1153D6',
            '--bodycolor': '#ffffff',
            '--bodyfontSize': 12,
            '--bodyfontFamily': 'Microsoft YaHei',
            '--bodyfontWeight': 'normal',
            '--rowCurrentColor': '#53b0f7',
            '--headercolor': '#ffffff',
            '--headerfontSize': 12,
            '--headerfontFamily': 'Microsoft YaHei',
            '--headerfontWeight': 'normal'
          },
          hookId: 'TableChart20250924160930',
          isChart: true,
          isTabLayout: false,
          x: 9,
          y: 9,
          w: 3,
          h: 17,
          i: 'd37093bc-122c-4f00-beec-fd9b71a02533',
          moved: false,
          extendedItem: '',
          paddingItem: '',
          hookItem: '',
          queryItem: '',
          hookConfig: {
            show: false,
            script: "\nonMount(function () {\n  console.log('组件已挂载');\n\n  // 获取当前组件\n  var myImage = stage.get('#hookId#');\n  console.log(myImage);\n  if (myImage) {\n\n    // 触发自定义事件\n    myImage.emit('custom-event', {\n      message: '来自Hook的自定义事件'\n    });\n  }\n})\n\n\n// 数据变化时\nonDataChange(function (newData) {\n  console.log('数据已更新:', newData);\n\n});\n\n// 组件更新时\nonUpdate(function () {\n  console.log('组件已更新');\n});\n\n// 组件销毁时\nonDestroy(function () {\n  console.log('组件即将销毁');\n});"
          },
          paddingConfig: {},
          queryConfig: {
            '--right': 0,
            '--top': 3,
            '--width': 120
          }
        }
      ]
    }
  ],
  decorateLayout: [],
  margin: [
    20,
    20
  ],
  colNum: 12,
  rowHeight: 30,
  autoRowHeight: 30.23809523809524,
  draggable: true,
  resizable: true,
  maxRows: 21,
  themeConfigs: {
    preTheme: 'default',
    themeMode: 'default',
    themeList: [
      {
        value: 'default',
        label: '默认'
      },
      {
        value: 'blue',
        label: '深蓝'
      }
    ],
    globalCss: {
      themeMode: 'blue',
      backgroundColor: '#001132',
      pageBackground: {
        backgroundImage: '',
        backgroundRepeat: 'repeat'
      },
      cardBackgroundColor: 'linear-gradient(180deg, rgba(5, 29, 79, 0), rgba(8, 54, 129, 0.6))',
      cardBackground: {
        backgroundImage: '',
        backgroundRepeat: 'repeat'
      },
      bgBorder: {
        width: 1,
        color: '#ddd',
        type: 'solid'
      },
      cardStyle: {
        width: 0,
        color: '',
        type: 'solid'
      },
      queryStyle: {
        backgroundColor: 'transparent',
        width: 0,
        color: '',
        type: 'solid',
        radius: 0
      },
      operateStyle: {
        color: '#a3a3a3',
        fontSize: 12,
        fontFamily: 'Microsoft YaHei',
        fontWeight: 'bold'
      },
      textStyle: {
        color: '#fff',
        fontSize: 14,
        fontFamily: 'Microsoft YaHei',
        fontWeight: 'normal'
      },
      tabCommonStyle: {
        color: '#fff',
        fontSize: 14,
        height: 35,
        fontWeight: 'bold',
        backgroundColor: 'linear-gradient(90deg, rgba(0, 99, 255, 0.71) 37%, rgba(20, 64, 151, 0))'
      },
      tabStyle: {
        type: 'custom',
        underlineStyle: {
          textColor: '#fff',
          lineColor: '#0c214a',
          lineWidth: 2
        },
        splitlineStyle: {
          lineWidth: 1,
          lineColor: '#fff',
          textColor: '#fff'
        },
        blockStyle: {
          radius: 4,
          padding: 0,
          textColor: '#fff',
          backgroundColor: '#1f6aff',
          borderWidth: 0,
          borderColor: '#3f6fce',
          borderType: 'solid',
          selectedBorderWidth: 0,
          selectedBorderColor: '#001132'
        },
        customStyle: {
          backgroundImage: '/profile/upload/am-image/2024/06/06/F1798538859103236096.png',
          backgroundRepeat: 'no-repeat'
        }
      },
      tabSplitStyle: {
        width: 0,
        color: '',
        type: 'solid'
      },
      cardRadius: 'none',
      spaceMode: 'normal',
      margin: [
        20,
        20
      ],
      cardPadding: {
        paddingTop: 0,
        paddingBottom: 0,
        paddingLeft: 0,
        paddingRight: 0
      },
      cardShadow: {
        shadowBlur: 0,
        shadowColor: 'transparent',
        shadowOffsetX: 0,
        shadowOffsetY: 0
      },
      childConfig: {
        mergeChildChart: 'independence',
        colNum: 12,
        rowHeight: 5,
        margin: [
          0,
          5
        ]
      },
      decorateStyle: {
        color: '#fff',
        fontSize: 12,
        fontFamily: 'Microsoft YaHei',
        fontWeight: 'normal'
      }
    },
    pageLayout: {
      pageWidth: '100%',
      pageMinWidth: 800,
      pageMinHeight: 500,
      pageWidthMode: 'adaptive',
      pageHeightMode: 'adaptive',
      pagePadding: {
        paddingTop: 10,
        paddingBottom: 0,
        paddingLeft: 0,
        paddingRight: 0
      }
    },
    globalCustomStyle: ' .vgl-item {\n            & > .vgl-item__resizer::before {\n              content: "\\e652";\n            }\n          }\n\n          .set-chart-content {\n            &:hover {\n              cursor: pointer;\n            }\n\n            &.foucs-chart-content {\n              border: 1px solid rgba(18, 219, 255, 0.5) !important;\n              box-shadow: 0 0 9px rgba(0, 208, 239, 0.49);\n            }\n          }\n\n        //   .chart-title {\n        //     border-radius: 4px 4px 0 0;\n        //     height: 33px;\n\n        //     &::before {\n        //       content: "";\n        //       height: 1px;\n        //       width: 100%;\n        //       background: linear-gradient(221deg, rgba(15, 88, 233, 0) 5%, #0f58e9 58%, #0f58e9 98%);\n        //       position: absolute;\n        //       bottom: 0;\n        //     }\n        //   }\n\n        //   .chart-content-main::before,\n        //   .item-layout::before {\n        //     content: "";\n        //     height: 0.5px;\n        //     width: 70%;\n        //     border: 0.5px solid;\n        //     border-image: linear-gradient(90deg, rgba(9, 56, 102, 0), #fff 53%, rgba(8, 19, 96, 0)) 1 1;\n        //     position: absolute;\n        //     bottom: 0;\n        //     left: 15%;\n        //     z-index: -999;\n        //   }\n\n        //   .chart-content-main::after,\n        //   .item-layout::after {\n        //     content: "";\n        //     height: 72px;\n        //     width: 100%;\n        //     background: linear-gradient(180deg, rgba(62, 94, 255, 0), #3e7dff);\n        //     filter: blur(72px);\n        //     position: absolute;\n        //     bottom: 0;\n        //     z-index: -999;\n        //   }\n\n          .merge-child-wrapper .chart-content-main::before {\n            content: none;\n          }\n\n          .merge-child-wrapper .chart-content-main::after {\n            content: none;\n          }\n\n          .vue-draggable-handle {\n            color: #a3a3a3 !important;\n          }\n\n          .vgl-item__resizer {\n            background: transparent;\n          }\n\n          .empty-area {\n            background: linear-gradient(180deg, #072161, #0a2c84);\n            border-radius: 8px;\n            box-shadow: 0 0 11px 0 rgba(33, 171, 234, 0.5) inset;\n          }\n\n          .empty-container {\n            .empty-img {\n              background: url("/profile/upload/am-image/2024/07/02/F1807946072943046656.png") no-repeat;\n            }\n          }',
    chartStyle: {
      defaultColors: [
        '#409EFF,#67C23A,#E6A23C,#F56C6C,#39D895,#ba55d3',
        '#E6A23C,#F56C6C,#909399,#E6A23C,#409EFF,#67C23A',
        '#ff7f50,#87cefa,#da70d6,#32cd32,#6495ed,#ff69b4,#ba55d3,#cd5c5c,#ffa500,#40e0d0',
        '#F5A3A3,#F7C685,#85AEEA,#6FD2C3,#BBA0E2,#A8D5A2',
        '#516b91,#59c4e6,#edafda,#93b7e3,#a5e7f0,#cbb0e3',
        '#3fb1e3,#6be6c1,#626c91,#a0a7e6,#c4ebad,#96dee8'
      ],
      themeStyle: {
        textColor: '#FFF',
        backgroundColor: 'rgba(0, 54, 176, 0.5)',
        borderColor: '#333',
        fontSize: 12,
        colorList: [
          '#5478FD',
          '#7B2CFF',
          '#FFC72F',
          '#008FFF',
          '#3ED848',
          '#C96765'
        ],
        configStyle: {
          tooltip: {
            show: true,
            backgroundColor: 'rgba(0, 54, 176, 0.5)',
            padding: 5,
            borderWidth: 0,
            borderColor: '#000',
            textStyle: {
              color: '#FFF',
              fontSize: 12,
              fontFamily: 'Microsoft YaHei',
              fontWeight: 'normal'
            }
          },
          legend: {
            show: true,
            itemGap: 10,
            itemHeight: 15,
            itemWidth: 25,
            textStyle: {
              color: '#FFF',
              fontSize: 12,
              fontFamily: 'Microsoft YaHei',
              fontWeight: 'normal'
            }
          },
          pieStyle: {
            label: {
              show: false,
              position: 'outside',
              color: '#FFF',
              fontSize: 12,
              fontFamily: 'Microsoft YaHei',
              fontWeight: 'normal'
            },
            labelLine: {
              show: false,
              length: 5,
              length2: 10,
              lineStyle: {
                width: 1
              }
            }
          }
        }
      },
      BKGaugeChart: {
        activeColor: '#468EFD',
        bgColor: '#111F42'
      },
      BKRankChart: {
        '--progressBgColor': '#f8f8f9',
        '--numcolor': '#fff',
        '--numfontSize': 12,
        '--numfontWeight': 'bold',
        '--titlecolor': '#fff',
        '--titlefontSize': 12,
        '--titlefontWeight': 'normal'
      },
      BKBarChart: {
        axisLineColor: '#1E54BE',
        splitLineColor: '#1E54BE'
      },
      BKMapChart: {
        tooltipBackgroundColor: 'rgba(0, 54, 176, 0.5)',
        tooltipBorderColor: '#000',
        visualMapTextColor: '#24CFF4',
        tooltipColor: '#ffffff',
        eTextColor: '#f75a00',
        sTextColor: '#333',
        areaColor: '#24CFF4',
        eAreaColor: '#8dd7fc',
        sAreaColor: '#fff',
        borderColor: '#53D9FF',
        eBorderColor: '#53D9FF',
        sBorderColor: '',
        mapColorList: [
          '#24CFF4',
          '#1E62AC'
        ]
      },
      BKTableChart: {
        '--borderColor': '#0F57E5',
        '--stripeColor': '#0c3991',
        '--bodyBg': '#0C2D6F',
        '--headerBg': '#1153D6',
        '--bodycolor': '#ffffff',
        '--bodyfontSize': 12,
        '--bodyfontFamily': 'Microsoft YaHei',
        '--bodyfontWeight': 'normal',
        '--rowCurrentColor': '#53b0f7',
        '--headercolor': '#ffffff',
        '--headerfontSize': 12,
        '--headerfontFamily': 'Microsoft YaHei',
        '--headerfontWeight': 'normal'
      },
      BKStatisticsChart: {
        numColor: '#53b0f7',
        textColor: '#ffffff'
      },
      progressChart: {
        titleColor: '#38a700',
        barBackgroundColor: 'rgba(66, 66, 66, 0.3)',
        barColor: '#38a700'
      },
      legend: {
        show: true,
        itemGap: 10,
        itemHeight: 15,
        itemWidth: 25,
        textStyle: {
          color: '#FFF',
          fontSize: 12,
          fontFamily: 'Microsoft YaHei',
          fontWeight: 'normal'
        }
      },
      barStyle: {
        borderRadius: [
          0,
          0,
          0,
          0
        ],
        barWidth: 20
      },
      lineStyle: {
        showSymbol: true,
        symbolSize: 4,
        symbol: 'emptyCircle',
        lineStyle: {
          type: 'solid',
          width: 2
        },
        isArea: false,
        smooth: false,
        label: {
          show: false
        }
      },
      tooltip: {
        show: true,
        backgroundColor: 'rgba(0, 54, 176, 0.5)',
        padding: 5,
        borderWidth: 0,
        borderColor: '#000',
        textStyle: {
          color: '#FFF',
          fontSize: 12,
          fontFamily: 'Microsoft YaHei',
          fontWeight: 'normal'
        }
      },
      table: {
        body: {
          styles: {
            '--borderColor': '#0F57E5',
            '--stripeColor': '#0c3991',
            '--bodyBg': '#0C2D6F',
            '--tablecellheight': 48,
            '--bodyfontFamily': 'Microsoft YaHei',
            '--bodyfontWeight': 'normal',
            '--rowCurrentColor': '#53b0f7',
            '--bodyfontSize': 12,
            '--bodycolor': '#FFF'
          },
          props: {
            seqWidth: 60,
            stripe: false,
            highlightCurrentRow: false,
            hasSeq: true,
            border: 'outer',
            align: 'center'
          }
        },
        header: {
          styles: {
            '--headercolor': '#ffffff',
            '--headerfontSize': 12,
            '--headercellheight': 48,
            '--headerBg': '#1153D6',
            '--headerfontFamily': 'Microsoft YaHei',
            '--headerfontWeight': 'normal'
          },
          props: {
            showHeader: true,
            headerAlign: 'center'
          }
        }
      },
      pieStyle: {
        label: {
          show: false,
          position: 'outside',
          color: '#FFF',
          fontSize: 12,
          fontFamily: 'Microsoft YaHei',
          fontWeight: 'normal'
        },
        labelLine: {
          show: false,
          length: 5,
          length2: 10,
          lineStyle: {
            width: 1
          }
        }
      },
      xAxis: {
        axisLabel: {
          show: true,
          margin: 8,
          rotate: 0,
          color: '#FFF',
          fontSize: 12,
          fontFamily: 'Microsoft YaHei',
          fontWeight: 'bold'
        },
        axisLine: {
          show: true,
          lineStyle: {
            width: 1,
            color: '#1E54BE',
            type: 'solid'
          }
        },
        axisTick: {
          show: true,
          inside: 0,
          length: 5,
          lineStyle: {
            width: 1,
            color: '#1E54BE'
          }
        },
        splitLine: {
          show: false,
          lineStyle: {
            width: 1,
            color: '#1E54BE',
            type: 'solid'
          }
        },
        nameStyle: {
          nameGap: 15,
          nameLocation: 'end',
          nameTextStyle: {
            color: '#FFF',
            fontSize: 12,
            fontFamily: 'Microsoft YaHei',
            fontWeight: 'bold'
          }
        }
      },
      yAxis: {
        axisLabel: {
          show: true,
          margin: 8,
          rotate: 0,
          color: '#FFF',
          fontSize: 12,
          fontFamily: 'Microsoft YaHei',
          fontWeight: 'bold'
        },
        axisLine: {
          show: false,
          lineStyle: {
            width: 1,
            color: '#1E54BE',
            type: 'solid'
          }
        },
        axisTick: {
          show: false,
          inside: 0,
          length: 5,
          lineStyle: {
            width: 1,
            color: '#1E54BE'
          }
        },
        splitLine: {
          show: true,
          lineStyle: {
            width: 1,
            color: '#1E54BE',
            type: 'dashed'
          }
        },
        nameStyle: {
          nameGap: 15,
          nameLocation: 'end',
          nameTextStyle: {
            color: '#FFF',
            fontSize: 12,
            fontFamily: 'Microsoft YaHei',
            fontWeight: 'bold'
          }
        }
      },
      map: {
        select: {
          itemStyle: {
            areaColor: '#fff',
            borderColor: '',
            borderWidth: 0
          },
          label: {
            show: true,
            color: '#333'
          }
        },
        emphasis: {
          itemStyle: {
            areaColor: '#8dd7fc',
            borderColor: '',
            borderWidth: 0
          },
          label: {
            show: true,
            color: '#f75a00'
          }
        },
        normal: {
          itemStyle: {
            areaColor: '#24CFF4',
            borderColor: '',
            borderWidth: 0
          },
          label: {
            show: true,
            color: '#fff'
          }
        }
      },
      BKPolarChart: {
        axisLineColor: '#1E54BE',
        splitLineColor: '1E54BE'
      }
    }
  },
  varConfig: [
    {
      id: '5882a059-2fdb-4b75-b8c9-7c48d01cfe61',
      changeType: 'refreshData',
      isRequired: '0',
      isDefault: '0',
      isQuery: '1',
      isAdd: true,
      beforeDefault: '0',
      type: 'internal',
      _X_ROW_KEY: 'row_19',
      name: 'type',
      initValue: '1',
      queryConfig: {
        label: '',
        showType: 'radio',
        dataSource: 'static',
        dicKey: '',
        staticData: '[\r\n    {"value":"1","label":"水位"},\r\n    {"value":"2","label":"水质"},\r\n    {"value":"3","label":"应力"}\r\n]',
        interfaceFunc: '',
        placeholder: '',
        props: ''
      },
      value: ''
    }
  ]
}

const chartList = [
  {
    name: '示例1',
    key: 'example1',
    draggable: true,
    children: [
      {
        isChart: true,
        x: 0,
        y: 8,
        w: 4,
        h: 8,
        i: 'cfff7ae4-f28f-4091-8fdd-a7445f0f1ebf',
        id: 'cfff7ae4-f28f-4091-8fdd-a7445f0f1ebf',
        moved: false,
        title: '防洪抗旱/防汛抗旱',
        type: 'BKPieChart',
        img: 'pie.png',
        explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0, textStyle: { color: '#7A7A7A', fontSize: 12 } },
        chartComId: '01104246-a89d-4e5c-ae45-c2b0937d752c',
        branchType: 'multiplePieChart',
        eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }, { field: 'value', varField: '' }] }],
        items: [{ label: 'name', field: 'name', value: 'name' }, { label: 'value', field: 'value', value: 'value' }, { label: '颜色映射', field: 'colorField', value: 'colorField' }],
        chartId: 'ea7f35ae-ad3c-498e-8d73-dc6a2bdfdff2',
        isShowTitle: '1',
        varListener: [],
        dataSourceConfig: { type: 'static', data: [{ name: '防洪蓄水（万m³）', value: 11, colorField: '1' }, { name: '防洪放水（万m³）', value: 5, colorField: '1' }, { name: '防汛蓄水（万m³）', value: 10, colorField: '2' }, { name: '防汛放水（万m³）', value: 6, colorField: '2' }], value: '[\r\n    {\r\n        "name": "防洪蓄水（万m³）",\r\n        "value": 11,\r\n        "colorField": "1"\r\n    },\r\n    {\r\n        "name": "防洪放水（万m³）",\r\n        "value": 5,\r\n        "colorField": "1"\r\n    },\r\n    {\r\n        "name": "防汛蓄水（万m³）",\r\n        "value": 10,\r\n        "colorField": "2"\r\n    },\r\n    {\r\n        "name": "防汛放水（万m³）",\r\n        "value": 6,\r\n        "colorField": "2"\r\n    }\r\n]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} },
        configOption: { color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#39D895', '#ba55d3'], tooltip: { show: true, hideDelay: 100, backgroundColor: '#fff', trigger: 'item', triggerOn: 'mousemove', padding: 5, borderWidth: 0, borderColor: '#333', textStyle: { color: '#7A7A7A', fontSize: 12, fontFamily: 'Microsoft YaHei', fontWeight: 'normal' } }, legend: { show: true, itemGap: 10, icon: '', type: 'plain', itemHeight: 14, itemWidth: 25, top: 0, left: 0, orient: 'horizontal', alignPosition: 'topCenter', textStyle: { color: '#7A7A7A', fontSize: 12, fontFamily: 'Microsoft YaHei', fontWeight: 'normal' } }, series: [{ radius: [0, 50], center: [30, 50], roseType: false, label: { show: false, position: 'outside', formatter: '{d}%', color: '#7A7A7A' }, labelLine: { show: false, length: 5, length2: 5, lineStyle: { width: 1 } }, type: 'pie', clockwise: true, avoidLabelOverlap: true, hoverOffset: 15, name: '1', dataId: '1', childSeries: {} }, { radius: [0, 50], center: [70, 50], roseType: false, label: { show: false, position: 'outside', formatter: '{d}%', color: '#7A7A7A' }, labelLine: { show: true, length: 20, length2: 30, lineStyle: { width: 1 } }, type: 'pie', clockwise: true, avoidLabelOverlap: true, hoverOffset: 15, name: '2', dataId: '2', childSeries: {} }], grid: {}, legendConfig: { show: false, unit: '', dataType: '', header: ['', '', ''], position: 'topLeft', showPerc: false, percNumber: 2, unitPosition: 'onHeader', x: 0, y: 0 } },
        chartTheme: { textColor: '#7A7A7A', backgroundColor: '#fff', borderColor: '#333', fontSize: 12, colorList: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#39D895', '#ba55d3'], configStyle: {} },
        extendedItem: '',
        paddingItem: '',
        queryItem: '',
        paddingConfig: {},
        queryConfig: { '--right': 0, '--top': 3, '--width': 120 },
        tabord: 0,
        initChartId: true
      },
      {
        isChart: true,
        x: 4,
        y: 8,
        w: 4,
        h: 8,
        i: '9f0bcc22-194a-4d12-a38c-e7e08464fed5',
        id: '9f0bcc22-194a-4d12-a38c-e7e08464fed5',
        moved: false,
        title: '水库巡查次数统计',
        type: 'BKBarChart',
        explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0, textStyle: { color: '#7A7A7A', fontSize: 12 } },
        branchType: 'mixChart',
        img: 'line-bar.png',
        chartComId: 'ea3570f6-d9aa-4c6a-8dae-be49d3739610',
        eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
        items: [{ label: 'x', field: 'x', value: 'x' }, { label: 'y', field: 'y', value: 'y' }, { label: '颜色映射', field: 'colorField', value: 'colorField' }],
        chartId: 'bb0505b2-1601-4648-8245-c049112e418b',
        isShowTitle: '1',
        varListener: [],
        dataSourceConfig: { type: 'static', data: [{ colorField: '周巡查趋势', x: '1', y: 1 }, { colorField: '月巡查趋势', x: '1', y: 4 }, { colorField: '周巡查趋势', x: '2', y: 2 }, { colorField: '月巡查趋势', x: '2', y: 3 }, { colorField: '周巡查趋势', x: '3', y: 3 }, { colorField: '月巡查趋势', x: '3', y: 2 }, { colorField: '周巡查趋势', x: '4', y: 4 }, { colorField: '月巡查趋势', x: '4', y: 4 }], value: '[\r\n    {\r\n        "colorField": "周巡查趋势",\r\n        "x": "1",\r\n        "y": 1\r\n    },\r\n    {\r\n        "colorField": "月巡查趋势",\r\n        "x": "1",\r\n        "y": 4\r\n    },\r\n    {\r\n        "colorField": "周巡查趋势",\r\n        "x": "2",\r\n        "y": 2\r\n    },\r\n    {\r\n        "colorField": "月巡查趋势",\r\n        "x": "2",\r\n        "y": 3\r\n    },\r\n    {\r\n        "colorField": "周巡查趋势",\r\n        "x": "3",\r\n        "y": 3\r\n    },\r\n    {\r\n        "colorField": "月巡查趋势",\r\n        "x": "3",\r\n        "y": 2\r\n    },\r\n    {\r\n        "colorField": "周巡查趋势",\r\n        "x": "4",\r\n        "y": 4\r\n    },\r\n    {\r\n        "colorField": "月巡查趋势",\r\n        "x": "4",\r\n        "y": 4\r\n    }\r\n]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} },
        configOption: { tooltip: { show: true, hideDelay: 100, backgroundColor: '#fff', trigger: 'axis', triggerOn: 'mousemove', padding: 5, borderWidth: 0, borderColor: '#333', textStyle: { color: '#7A7A7A', fontSize: 12, fontFamily: 'Microsoft YaHei', fontWeight: 'normal' } }, legend: { show: true, itemGap: 10, icon: '', type: 'plain', itemHeight: 14, itemWidth: 25, top: 0, left: 0, orient: 'horizontal', alignPosition: 'topCenter', textStyle: { color: '#7A7A7A', fontSize: 12, fontFamily: 'Microsoft YaHei', fontWeight: 'normal' } }, grid: { top: 50, left: 50, right: 50, bottom: 20, containLabel: true, show: false }, xAxis: { show: true, name: '', position: 'bottom', type: 'category', nameGap: 15, nameLocation: 'end', scale: false, axisLabel: { show: true, margin: 8, rotate: 0, dataType: '', color: '#7A7A7A', fontSize: 12, fontFamily: 'Microsoft YaHei', fontWeight: 'normal' }, nameTextStyle: { color: '#7A7A7A', fontSize: 12 }, axisLine: { show: true, lineStyle: { width: 1, color: '#eaeff4', type: 'solid' } }, axisTick: { show: true, inside: 0, length: 5, lineStyle: { width: 1, color: '#DDD', type: 'solid' } }, splitLine: { show: false } }, yAxis: [{ show: true, name: '次', type: 'value', alignTicks: true, nameGap: 15, nameLocation: 'end', scale: false, position: 'left', axisLabel: { show: true, margin: 8, rotate: 0, dataType: '', color: '#7A7A7A', fontSize: 12, fontFamily: 'Microsoft YaHei', fontWeight: 'normal' }, axisLine: { show: false, lineStyle: { color: '#eaeff4' } }, splitLine: { show: true, lineStyle: { width: 1, color: '#e6e6e6', type: 'dashed' } }, axisTick: { show: false, inside: 0, length: 5, lineStyle: { width: 1, color: '#DDD' } }, nameTextStyle: { color: '#7A7A7A', fontSize: 12 } }, { show: false, name: '', type: 'value', alignTicks: true, nameGap: 15, nameLocation: 'end', scale: false, position: 'left', axisLabel: { show: false, margin: 8, rotate: 0, dataType: '' }, axisLine: { show: false }, splitLine: { show: false }, axisTick: { show: false, inside: 0, length: 5 }, nameTextStyle: {} }], series: [{ type: 'bar', itemStyle: { color: null }, backgroundStyle: {}, name: '周巡查趋势', dataId: '周巡查趋势', barMaxWidth: 30, yAxisIndex: 0, legendConfig: { show: false, icon: '' } }, { type: 'line', showSymbol: true, symbolSize: 4, symbol: 'circle', lineStyle: { width: 2, color: null, shadowColor: 'rgba(71,216,190, 0.5)', shadowBlur: 10, shadowOffsetY: 7 }, itemStyle: { color: null }, smooth: false, name: '月巡查趋势', dataId: '月巡查趋势', yAxisIndex: 0, legendConfig: { show: false, icon: '' } }], color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#39D895', '#ba55d3'], borderRadius: [0, 0, 0, 0], filterDataEmpty: false, autoSeries: false },
        chartTheme: { textColor: '#7A7A7A', backgroundColor: '#fff', borderColor: '#333', fontSize: 12, colorList: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#39D895', '#ba55d3'], axisLineColor: '#eaeff4', splitLineColor: '#e6e6e6', configStyle: {} },
        extendedItem: '',
        paddingItem: '',
        queryItem: '',
        paddingConfig: {},
        queryConfig: { '--right': 0, '--top': 3, '--width': 120 },
        tabord: 0,
        initChartId: true
      }
    ]
  },
  {
    name: '示例2',
    key: 'example2',
    draggable: true,
    children: [
      {
        isChart: true,
        x: 8,
        y: 0,
        w: 4,
        h: 8,
        i: 'c32dbee8-027c-4d23-af1c-ae4d1c207c51',
        id: 'c32dbee8-027c-4d23-af1c-ae4d1c207c51',
        moved: false,
        title: '实时监测数据',
        type: 'BKBarChart',
        explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0, textStyle: { color: '#7A7A7A', fontSize: 12 } },
        img: 'line.png',
        branchType: 'lineChart',
        chartComId: '660470ac-2ebd-40fa-84ea-1668df909ed9',
        eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'x', varField: '' }, { field: 'y', varField: '' }] }],
        items: [{ label: 'x', field: 'x', value: 'x' }, { label: 'y', field: 'y', value: 'y' }, { label: '颜色映射', field: 'colorField', value: 'colorField' }],
        chartId: 'dd717fc9-d71a-4fe6-bbbc-e5ce85a10a50',
        isShowTitle: '1',
        varListener: [{ id: '5882a059-2fdb-4b75-b8c9-7c48d01cfe61', changeType: 'refreshData', isRequired: '0', isDefault: '0', isQuery: '1', isAdd: true, beforeDefault: '0', type: 'internal', _X_ROW_KEY: 'row_19', name: 'type', initValue: '1', queryConfig: { label: '', showType: 'radio', dataSource: 'static', dicKey: '', staticData: '[\r\n    {"value":"1","label":"水位"},\r\n    {"value":"2","label":"水质"},\r\n    {"value":"3","label":"应力"}\r\n]', interfaceFunc: '', placeholder: '', props: '' }, alias: 'type', operator: 'eq', isShowQuery: '1', ord: '' }],
        dataSourceConfig: { type: 'static', data: [{ colorField: '水位', x: '1', y: 1, type: '1' }, { colorField: '水位', x: '2', y: 4, type: '1' }, { colorField: '水位', x: '3', y: 2, type: '1' }, { colorField: '水位', x: '4', y: 3, type: '1' }, { colorField: '水质', x: '1', y: 3, type: '2' }, { colorField: '水质', x: '2', y: 2, type: '2' }, { colorField: '水质', x: '3', y: 4, type: '2' }, { colorField: '水质', x: '4', y: 4, type: '2' }, { colorField: '应力', x: '1', y: 3, type: '3' }, { colorField: '应力', x: '2', y: 6, type: '3' }, { colorField: '应力', x: '3', y: 7, type: '3' }, { colorField: '应力', x: '4', y: 7, type: '3' }], value: '[\r\n    {\r\n        "colorField": "水位",\r\n        "x": "1",\r\n        "y": 1,\r\n        "type": "1"\r\n    },\r\n    {\r\n        "colorField": "水位",\r\n        "x": "2",\r\n        "y": 4,\r\n        "type": "1"\r\n    },\r\n    {\r\n        "colorField": "水位",\r\n        "x": "3",\r\n        "y": 2,\r\n        "type": "1"\r\n    },\r\n    {\r\n        "colorField": "水位",\r\n        "x": "4",\r\n        "y": 3,\r\n        "type": "1"\r\n    },\r\n    {\r\n        "colorField": "水质",\r\n        "x": "1",\r\n        "y": 3,\r\n        "type": "2"\r\n    },\r\n    {\r\n        "colorField": "水质",\r\n        "x": "2",\r\n        "y": 2,\r\n        "type": "2"\r\n    },\r\n    {\r\n        "colorField": "水质",\r\n        "x": "3",\r\n        "y": 4,\r\n        "type": "2"\r\n    },\r\n    {\r\n        "colorField": "水质",\r\n        "x": "4",\r\n        "y": 4,\r\n        "type": "2"\r\n    },\r\n    {\r\n        "colorField": "应力",\r\n        "x": "1",\r\n        "y": 3,\r\n        "type": "3"\r\n    },\r\n    {\r\n        "colorField": "应力",\r\n        "x": "2",\r\n        "y": 6,\r\n        "type": "3"\r\n    },\r\n    {\r\n        "colorField": "应力",\r\n        "x": "3",\r\n        "y": 7,\r\n        "type": "3"\r\n    },\r\n    {\r\n        "colorField": "应力",\r\n        "x": "4",\r\n        "y": 7,\r\n        "type": "3"\r\n    }\r\n]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} },
        configOption: { tooltip: { show: true, hideDelay: 100, backgroundColor: '#fff', trigger: 'axis', triggerOn: 'mousemove', padding: 5, borderWidth: 0, borderColor: '#333', textStyle: { color: '#7A7A7A', fontSize: 12, fontFamily: 'Microsoft YaHei', fontWeight: 'normal' } }, legend: { show: false, itemGap: 10, icon: '', type: 'plain', itemHeight: 14, itemWidth: 25, top: 0, left: 0, orient: 'horizontal', alignPosition: 'topCenter', textStyle: { color: '#7A7A7A', fontSize: 12, fontFamily: 'Microsoft YaHei', fontWeight: 'normal' } }, grid: { top: 50, left: 50, right: 50, bottom: 20, containLabel: true, show: false }, xAxis: { show: true, name: '', position: 'bottom', type: 'category', nameGap: 15, nameLocation: 'end', scale: false, axisLabel: { show: true, margin: 8, rotate: 0, dataType: '', color: '#7A7A7A', fontSize: 12, fontFamily: 'Microsoft YaHei', fontWeight: 'normal' }, nameTextStyle: { color: '#7A7A7A', fontSize: 12 }, axisLine: { show: true, lineStyle: { width: 1, color: '#eaeff4', type: 'solid' } }, axisTick: { show: true, inside: 0, length: 5, lineStyle: { width: 1, color: '#DDD', type: 'solid' } }, splitLine: { show: false } }, yAxis: [{ show: true, name: '', type: 'value', alignTicks: true, nameGap: 15, nameLocation: 'end', scale: false, position: 'left', axisLabel: { show: true, margin: 8, rotate: 0, dataType: '', color: '#7A7A7A', fontSize: 12, fontFamily: 'Microsoft YaHei', fontWeight: 'normal' }, axisLine: { show: false, lineStyle: { color: '#eaeff4' } }, splitLine: { show: true, lineStyle: { width: 1, color: '#e6e6e6', type: 'dashed' } }, axisTick: { show: false, inside: 0, length: 5, lineStyle: { width: 1, color: '#DDD' } }, nameTextStyle: { color: '#7A7A7A', fontSize: 12 } }, { show: false, name: '', type: 'value', alignTicks: true, nameGap: 15, nameLocation: 'end', scale: false, position: 'left', axisLabel: { show: false, margin: 8, rotate: 0, dataType: '' }, axisLine: { show: false }, splitLine: { show: false }, axisTick: { show: false, inside: 0, length: 5 }, nameTextStyle: {} }], series: [{ symbolSize: 4, symbol: 'emptyCircle', showSymbol: true, smooth: true, areaColor: '#000', itemStyle: { color: null }, label: { show: false }, lineStyle: { width: 2, color: null }, type: 'line', color: null, name: '水位', dataId: '水位', yAxisIndex: 0, legendConfig: { show: false, icon: '' } }, { symbolSize: 4, symbol: 'emptyCircle', showSymbol: true, smooth: true, areaColor: '#000', itemStyle: { color: null }, label: { show: false }, lineStyle: { width: 2, color: null }, type: 'line', name: '水质', dataId: '水质', yAxisIndex: 0, legendConfig: { show: false, icon: '' } }, { symbolSize: 4, symbol: 'emptyCircle', showSymbol: true, smooth: true, areaColor: '#000', itemStyle: {}, label: { show: false }, lineStyle: { width: 2 }, type: 'line', name: '应力', dataId: '应力', yAxisIndex: 0, legendConfig: { show: false, icon: '' } }], color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#39D895', '#ba55d3'], borderRadius: [0, 0, 0, 0], filterDataEmpty: false, autoSeries: false },
        chartTheme: { textColor: '#7A7A7A', backgroundColor: '#fff', borderColor: '#333', fontSize: 12, colorList: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#39D895', '#ba55d3'], axisLineColor: '#eaeff4', splitLineColor: '#e6e6e6', configStyle: {} },
        extendedItem: '',
        paddingItem: '',
        queryItem: '',
        paddingConfig: {},
        queryConfig: { '--right': 100, '--top': 40, '--width': 250 },
        operatorConfig: [],
        tabord: 0,
        initChartId: true
      },
      {
        isChart: true,
        x: 0,
        y: 0,
        w: 4,
        h: 8,
        i: '606b53f2-43a9-4728-8c75-e4cc2c83a7c0',
        id: '4c70853f-a891-4121-b07a-e1671e026810',
        moved: false,
        title: '水库标准化考核',
        type: 'BKPieChart',
        customType: 'progress',
        explainConfig: { show: false, text: '', position: 'topLeft', x: 0, y: 0, textStyle: { color: '#7A7A7A', fontSize: 12 } },
        img: 'multiple-pie.png',
        chartComId: '37c5e974-1f4e-4170-87e8-68f2c874c962',
        branchType: 'multiplePieChart',
        eventConfig: [{ title: '当数据项被点击时', event: 'click', isActive: false, items: [{ field: 'name', varField: '' }, { field: 'value', varField: '' }] }],
        items: [{ label: 'name', field: 'name', value: 'name' }, { label: 'value', field: 'value', value: 'value' }, { label: '颜色映射', field: 'colorField', value: 'colorField' }],
        chartId: '4faa8e9f-31df-454c-9e2c-8beb5e013e22',
        isShowTitle: '1',
        varListener: [],
        dataSourceConfig: { type: 'static', data: [{ name: '工程状况', value: 205, colorField: '1' }, { name: '工程状况1', value: 25, colorField: '1' }, { name: '安全管理', value: 280, colorField: '2' }, { name: '安全管理2', value: 0, colorField: '2' }, { name: '运行管护', value: 200, colorField: '3' }, { name: '运行管护2', value: 10, colorField: '3' }, { name: '管理保障', value: 160, colorField: '4' }, { name: '管理保障2', value: 20, colorField: '4' }, { name: '信息化管理', value: 87, colorField: '5' }, { name: '信息化管理2', value: 13, colorField: '5' }], value: '[\r\n    {\r\n        "name": "工程状况",\r\n        "value": 11,\r\n        "colorField": "1"\r\n    },\r\n    {\r\n        "name": "工程状况1",\r\n        "value": 11,\r\n        "colorField": "1"\r\n    },\r\n    {\r\n        "name": "安全管理",\r\n        "value": 5,\r\n        "colorField": "2"\r\n    },\r\n    {\r\n        "name": "安全管理2",\r\n        "value": 5,\r\n        "colorField": "2"\r\n    },\r\n    {\r\n        "name": "运行管护",\r\n        "value": 3,\r\n        "colorField": "3"\r\n    },\r\n       {\r\n        "name": "运行管护2",\r\n        "value": 3,\r\n        "colorField": "3"\r\n    },\r\n    {\r\n        "name": "管理保障",\r\n        "value": 7,\r\n        "colorField": "4"\r\n    },\r\n    {\r\n        "name": "管理保障2",\r\n        "value": 7,\r\n        "colorField": "4"\r\n    },\r\n    {\r\n        "name": "信息化管理",\r\n        "value": 7,\r\n        "colorField": "5"\r\n    },\r\n    {\r\n        "name": "信息化管理2",\r\n        "value": 7,\r\n        "colorField": "5"\r\n    }\r\n]', paramsType: 'json', interfaceFilterVisible: false, interfaceTempParamsVisible: false, paramHandlerVisible: false, dataMapping: {} },
        configOption: { color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#39D895', '#ba55d3'], tooltip: { show: true, hideDelay: 100, backgroundColor: '#fff', trigger: 'item', triggerOn: 'mousemove', padding: 5, borderWidth: 0, borderColor: '#333', textStyle: { color: '#7A7A7A', fontSize: 12, fontFamily: 'Microsoft YaHei', fontWeight: 'normal' } }, legend: { show: false, itemGap: 10, icon: '', type: 'plain', itemHeight: 14, itemWidth: 25, top: 0, left: 0, orient: 'horizontal', alignPosition: 'topCenter', textStyle: { color: '#7A7A7A', fontSize: 12, fontFamily: 'Microsoft YaHei', fontWeight: 'normal' } }, series: [{ radius: [30, 40], center: [20, 25], roseType: false, label: { show: false, position: 'outside', formatter: '{c}', color: '#7A7A7A' }, labelLine: { show: false, length: 5, length2: 5, lineStyle: { width: 1 } }, type: 'pie', clockwise: true, avoidLabelOverlap: true, hoverOffset: 15, name: '1', dataId: '1', childSeries: {} }, { radius: [30, 40], center: [50, 25], roseType: false, label: { show: false, position: 'outside', formatter: '{c}', color: '#7A7A7A' }, labelLine: { show: true, length: 20, length2: 30, lineStyle: { width: 1 } }, type: 'pie', clockwise: true, avoidLabelOverlap: true, hoverOffset: 15, name: '2', dataId: '2', childSeries: { radius: [0, 80], center: [50, 50], roseType: false, label: { show: false, formatter: '{d}%', position: 'outside', prefix: '', suffix: '' }, labelLine: { show: false, length: 5, length2: 10, lineStyle: { width: 1 } }, name: '2', dataId: '2' }, hasChildren: false }, { radius: [30, 40], center: [80, 25], roseType: false, label: { show: false, formatter: '{d}%', position: 'outside', prefix: '', suffix: '' }, labelLine: { show: false, length: 5, length2: 10, lineStyle: { width: 1 } }, name: '3', dataId: '3', type: 'pie', childSeries: {} }, { radius: [30, 40], center: [35, 68], roseType: false, label: { show: false, formatter: '{d}%', position: 'outside', prefix: '', suffix: '' }, labelLine: { show: false, length: 5, length2: 10, lineStyle: { width: 1 } }, name: '4', dataId: '4', type: 'pie', childSeries: {} }, { radius: [30, 40], center: [70, 68], roseType: false, label: { show: false, formatter: '{d}%', position: 'outside', prefix: '', suffix: '' }, labelLine: { show: false, length: 5, length2: 10, lineStyle: { width: 1 } }, name: '5', dataId: '5', type: 'pie', childSeries: {} }], grid: {}, legendConfig: { show: false, unit: '', dataType: '', header: ['', '', ''], position: 'topLeft', showPerc: false, percNumber: 2, unitPosition: 'onHeader', x: 0, y: 0 }, customConfig: { show: true, config: "const titleList = []\r\noption.series.forEach((ser, i) => {\r\n    titleList.push({\r\n        text: ser.data[0].name,\r\n        left: ser.center[0],\r\n        top: (Number(ser.center[1].split('%')[0]) + 22) + '%',\r\n        textAlign: 'center',\r\n        textStyle: {\r\n            fontWeight: 'normal',\r\n            fontSize: '14',\r\n            color: '#000',\r\n            textAlign: 'center'\r\n        }\r\n    })\r\n    ser.data[0].label = {\r\n        normal: {\r\n            formatter: function (params) {\r\n                return ser.data[0].value + '/' + (ser.data[0].value + ser.data[1].value)\r\n            },\r\n            position: 'center',\r\n            show: true,\r\n            textStyle: {\r\n                fontSize: '16',\r\n                fontWeight: 'bold',\r\n                color: '#000'\r\n            }\r\n        }\r\n    }\r\n    ser.data[0].itemStyle = {\r\n        normal: { color: option.color[i] }\r\n    }\r\n    ser.data[1].itemStyle = {\r\n        normal: { color: '#D8E5FA' }\r\n    }\r\n})\r\noption.title = titleList\r\nreturn option" } },
        chartTheme: { textColor: '#7A7A7A', backgroundColor: '#fff', borderColor: '#333', fontSize: 12, colorList: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#39D895', '#ba55d3'], configStyle: {} },
        extendedItem: '',
        paddingItem: '',
        queryItem: '',
        paddingConfig: {},
        queryConfig: { '--right': 0, '--top': 3, '--width': 120 },
        tabord: 0,
        initChartId: true
      }
    ]
  }
]

export { config, chartList }
