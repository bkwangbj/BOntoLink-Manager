import XEUtils from 'xe-utils'
// import Vue from 'vue'

// const themeModeConfig = {
//   blue: {
//     globalCss: {
//       // default, blue
//       themeMode: 'blue',
//       // 页面背景颜色
//       backgroundColor: '#001132',
//       pageBackground: {
//         backgroundImage: '',
//         backgroundRepeat: 'repeat'
//       },
//       // 卡片背景颜色
//       cardBackgroundColor: 'linear-gradient(180deg, rgba(5, 29, 79, 0), rgba(8, 54, 129, 0.6))',
//       cardBackground: {
//         backgroundImage: '',
//         backgroundRepeat: 'repeat'
//       },
//       // 背景边框
//       bgBorder: {
//         width: 1,
//         color: '#ddd',
//         type: 'solid'
//       },
//       cardStyle: {
//         width: 0,
//         color: '',
//         type: 'solid'
//       },
//       // 查询区域样式
//       queryStyle: {
//         backgroundColor: 'transparent',
//         width: 0,
//         color: '',
//         type: 'solid',
//         radius: 0
//       },
//       // 操作按钮样式
//       operateStyle: {
//         color: '#a3a3a3',
//         fontSize: 12,
//         fontFamily: 'Microsoft YaHei',
//         fontWeight: 'bold'
//       },
//       // 文本样式
//       textStyle: {
//         color: '#fff',
//         fontSize: 14,
//         fontFamily: 'Microsoft YaHei',
//         fontWeight: 'normal'
//       },
//       // 页签基础样式
//       tabCommonStyle: {
//         color: '#fff',
//         fontSize: 14,
//         height: 35, // 高度,
//         fontWeight: 'bold', // 选中文字粗细
//         backgroundColor: 'linear-gradient(90deg, rgba(0, 99, 255, 0.71) 37%, rgba(20, 64, 151, 0))' // 背景颜色
//       },
//       // 页签样式
//       tabStyle: {
//         type: 'custom', // underline 下划线, splitline 分割线,block 选中块, custom 自定义
//         underlineStyle: {
//           textColor: '#fff', // 选中文字颜色
//           lineColor: '#0c214a', // 选中下划线颜色
//           lineWidth: '2' // 选中下划线宽度
//         },
//         splitlineStyle: {
//           lineWidth: '1', // 分割线宽度
//           lineColor: '#fff', // 分割线颜色
//           textColor: '#fff' // 选中文字颜色
//         },
//         blockStyle: {
//           radius: '4', // 圆角
//           padding: '0', // 间距
//           textColor: '#fff', // 选中文字颜色
//           backgroundColor: '#1f6aff', // 选中背景颜色
//           borderWidth: 0, // 边框
//           borderColor: '#3f6fce',
//           borderType: 'solid',
//           selectedBorderWidth: 0,
//           selectedBorderColor: '#001132'
//         },
//         customStyle: {
//           backgroundImage: '/profile/upload/am-image/2024/06/06/F1798538859103236096.png',
//           backgroundRepeat: 'no-repeat'
//         }
//       },
//       // 页签分隔线
//       tabSplitStyle: {
//         width: 0,
//         color: '',
//         type: 'solid'
//       },
//       // none, small, big
//       cardRadius: 'none',
//       // compact, normal, custom
//       spaceMode: 'normal',
//       margin: [20, 20],
//       cardPadding: {
//         paddingTop: 0,
//         paddingBottom: 0,
//         paddingLeft: 0,
//         paddingRight: 0
//       },
//       // 卡片阴影
//       cardShadow: {
//         shadowBlur: 0,
//         shadowColor: 'transparent',
//         shadowOffsetX: 0,
//         shadowOffsetY: 0
//       },
//       childConfig: {
//         mergeChildChart: 'independence', // 子页面组件合并
//         colNum: 12,
//         rowHeight: 5,
//         margin: [0, 5]
//       }
//     },
//     pageLayout: {
//       pageWidth: '100%',
//       pageMinWidth: 1000,
//       pageMinHeight: 800,
//       // adaptive, custom
//       pageWidthMode: 'custom',
//       pageHeightMode: 'auto',
//       pagePadding: {
//         paddingTop: 10,
//         paddingBottom: 0,
//         paddingLeft: 0,
//         paddingRight: 0
//       }
//     },
//     globalCustomStyle: ` .vgl-item {
//       & > .vgl-item__resizer::before {
//         content: "\\e652";
//       }
//     }

//     .set-chart-content {
//       &:hover {
//         cursor: pointer;
//       }

//       &.foucs-chart-content {
//         border: 1px solid rgba(18, 219, 255, 0.5) !important;
//         box-shadow: 0 0 9px rgba(0, 208, 239, 0.49);
//       }
//     }

//     .chart-title {
//       border-radius: 4px 4px 0 0;
//       height: 33px;

//       &::before {
//         content: "";
//         height: 1px;
//         width: 100%;
//         background: linear-gradient(221deg, rgba(15, 88, 233, 0) 5%, #0f58e9 58%, #0f58e9 98%);
//         position: absolute;
//         bottom: 0;
//       }
//     }

//     .chart-content-main::before,
//     .item-layout::before {
//       content: "";
//       height: 0.5px;
//       width: 70%;
//       border: 0.5px solid;
//       border-image: linear-gradient(90deg, rgba(9, 56, 102, 0), #fff 53%, rgba(8, 19, 96, 0)) 1 1;
//       position: absolute;
//       bottom: 0;
//       left: 15%;
//       z-index: -999;
//     }

//     .chart-content-main::after,
//     .item-layout::after {
//       content: "";
//       height: 72px;
//       width: 100%;
//       background: linear-gradient(180deg, rgba(62, 94, 255, 0), #3e7dff);
//       filter: blur(72px);
//       position: absolute;
//       bottom: 0;
//       z-index: -999;
//     }

//     .merge-child-wrapper .chart-content-main::before {
//       content: none;
//     }

//     .merge-child-wrapper .chart-content-main::after {
//       content: none;
//     }

//     .vue-draggable-handle {
//       color: #a3a3a3 !important;
//     }

//     .vgl-item__resizer {
//       background: transparent;
//     }

//     .empty-area {
//       background: linear-gradient(180deg, #072161, #0a2c84);
//       border-radius: 8px;
//       box-shadow: 0 0 11px 0 rgba(33, 171, 234, 0.5) inset;
//     }

//     .empty-container {
//       .empty-img {
//         background: url("/profile/upload/am-image/2024/07/02/F1807946072943046656.png") no-repeat;
//       }
//     }`,
//     chartStyle: {
//       themeStyle: {
//         fontSize: 12,
//         textColor: '#ffffff',
//         backgroundColor: 'rgba(0, 54, 176, 0.5)',
//         borderColor: '#000',
//         colorList: ['#5478FD', '#7B2CFF', '#FFC72F', '#008FFF', '#3ED848', '#C96765']
//       },
//       BKGaugeChart: {
//         activeColor: '#468EFD', bgColor: '#111F42'
//       },
//       BKRankChart: {
//         '--progressBgColor': '#f8f8f9',
//         '--numcolor': '#fff',
//         '--numfontSize': 12,
//         '--numfontWeight': 'bold',
//         '--titlecolor': '#fff',
//         '--titlefontSize': 12,
//         '--titlefontWeight': 'normal'
//       },
//       BKBarChart: { axisLineColor: '#1E54BE', splitLineColor: '#1E54BE' },
//       BKMapChart: {
//         tooltipBackgroundColor: 'rgba(0, 54, 176, 0.5)',
//         tooltipBorderColor: '#000',
//         visualMapTextColor: '#24CFF4',
//         tooltipColor: '#ffffff',
//         eTextColor: '#f75a00',
//         sTextColor: '#333',
//         areaColor: '#24CFF4',
//         eAreaColor: '#8dd7fc',
//         sAreaColor: '#fff',
//         borderColor: '#53D9FF',
//         eBorderColor: '#53D9FF',
//         sBorderColor: '',
//         mapColorList: ['#24CFF4', '#1E62AC']
//       },
//       BKTableChart:
//         {
//           '--borderColor': '#0F57E5',
//           '--stripeColor': '#0c3991',
//           '--bodyBg': '#0C2D6F',
//           '--headerBg': '#1153D6',
//           '--bodycolor': '#ffffff',
//           '--bodyfontSize': 12,
//           '--bodyfontFamily': 'Microsoft YaHei',
//           '--bodyfontWeight': 'normal',
//           '--rowCurrentColor': '#53b0f7',
//           '--headercolor': '#ffffff',
//           '--headerfontSize': 12,
//           '--headerfontFamily': 'Microsoft YaHei',
//           '--headerfontWeight': 'normal'
//         },
//       BKStatisticsChart: { numColor: '#53b0f7', textColor: '#ffffff' },
//       progressChart: { titleColor: '#38a700', barBackgroundColor: 'rgba(66, 66, 66, 0.3)', barColor: '#38a700' }
//     }
//   },
//   default: {
//     globalCss: {
//       // default, blue
//       themeMode: 'default',
//       // 页面背景颜色
//       backgroundColor: '#fff',
//       // 卡片背景颜色
//       cardBackgroundColor: 'linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.54))',
//       cardBackground: {
//         backgroundImage: '',
//         backgroundRepeat: 'repeat'
//       },
//       pageBackground: {
//         backgroundImage: '',
//         backgroundRepeat: 'repeat'
//       },
//       bgBorder: {
//         width: 0,
//         color: 'transparent',
//         type: 'solid'
//       },
//       cardStyle: {
//         width: 1,
//         color: '#e9e9e9',
//         type: 'solid'
//       },
//       // 查询区域样式
//       queryStyle: {
//         backgroundColor: '#fff',
//         width: 1,
//         color: '#e9e9e9',
//         type: 'solid',
//         radius: 0
//       },
//       // 操作按钮样式
//       operateStyle: {
//         color: '#a3a3a3',
//         fontSize: 12,
//         fontFamily: 'Microsoft YaHei',
//         fontWeight: 'normal'
//       },
//       // 文本样式
//       textStyle: {
//         color: '#000',
//         fontSize: 14,
//         fontFamily: 'Microsoft YaHei',
//         fontWeight: 'normal'
//       },
//       // 页签基础样式
//       tabCommonStyle: {
//         color: '#747c8b',
//         fontSize: 14,
//         height: 35, // 高度
//         fontWeight: 'bold', // 选中文字粗细
//         backgroundColor: '#fff' // 背景颜色
//       },
//       // 页签样式
//       tabStyle: {
//         type: 'underline', // underline 下划线, splitline 分割线,block 选中块, custom 自定义
//         underlineStyle: {
//           textColor: '#1f6aff', // 选中文字颜色
//           lineColor: '#1f6aff', // 选中下划线颜色
//           lineWidth: '2' // 选中下划线宽度
//         },
//         splitlineStyle: {
//           lineWidth: '1', // 分割线宽度
//           lineColor: '#d2d3d9', // 分割线颜色
//           textColor: '#1f6aff' // 选中文字颜色
//         },
//         blockStyle: {
//           radius: '4', // 圆角
//           padding: '0', // 间距
//           textColor: '#1f6aff', // 选中文字颜色
//           backgroundColor: 'rgba(31,106,255,0.1)', // 选中背景颜色
//           borderWidth: 0, // 边框
//           borderColor: '#d2d3d9',
//           borderType: 'solid',
//           selectedBorderWidth: 0,
//           selectedBorderColor: '#fff'
//         },
//         customStyle: {
//           backgroundImage: '',
//           backgroundRepeat: 'no-repeat'
//         }
//       },
//       // 页签分隔线
//       tabSplitStyle: {
//         width: 1,
//         color: '#e8eaec',
//         type: 'solid'
//       },
//       // 卡片阴影
//       cardShadow: {
//         shadowBlur: 0,
//         shadowColor: 'transparent',
//         shadowOffsetX: 0,
//         shadowOffsetY: 0
//       },
//       // none, small, big
//       cardRadius: 'small',
//       // compact, normal, custom
//       spaceMode: 'compact',
//       margin: [10, 10],
//       cardPadding: {
//         paddingTop: 0,
//         paddingBottom: 0,
//         paddingLeft: 0,
//         paddingRight: 0
//       },
//       childConfig: {
//         mergeChildChart: 'independence', // 子页面组件合并
//         colNum: 12,
//         rowHeight: 5,
//         margin: [0, 5]
//       }
//     },
//     pageLayout: {
//       pageWidth: 900,
//       pageMinWidth: 1000,
//       pageMinHeight: 500,
//       // adaptive, custom
//       pageWidthMode: 'adaptive',
//       pageHeightMode: 'auto',
//       pagePadding: {
//         paddingTop: 0,
//         paddingBottom: 0,
//         paddingLeft: 0,
//         paddingRight: 0
//       }
//     },
//     globalCustomStyle: `.vgl-item {
//       & > .vgl-item__resizer {
//         display: inline-block;

//         &::before {
//           content: "\\e652";
//           font-size: 12px;
//           color: #7a7a7a;
//         }
//       }
//     }

//     .set-chart-content {
//       &:hover {
//         cursor: pointer;
//         border: 1px dashed #1f6aff !important;
//       }

//       &.foucs-chart-content {
//         border: 1px solid #1777f5 !important;
//       }
//     }

//     .empty-container {
//       .empty-img {
//         background: url("profile/upload/am-image/2024/07/02/F1807946128328830976.png") no-repeat;
//       }
//     }`,
//     chartStyle: {
//       themeStyle: {
//         textColor: '#7A7A7A',
//         backgroundColor: '#fff',
//         borderColor: '#333',
//         fontSize: 12,
//         colorList: ['#00E4BF', '#FFC72F', '#008FFF', '#3ED848', '#C96765', '#D8E5FA']
//       },
//       BKGaugeChart: {
//         activeColor: '#468EFD', bgColor: '#111F42', textColor: '#468EFD'
//       },
//       BKRankChart: {
//         '--progressBgColor': '#f8f8f9',
//         '--numcolor': '#424e61',
//         '--numfontSize': 12,
//         '--numfontWeight': 'bold',
//         '--titlecolor': '#000',
//         '--titlefontSize': 12,
//         '--titlefontWeight': 'normal'
//       },
//       BKBarChart: { axisLineColor: '#eaeff4', splitLineColor: '#e6e6e6', textColor: '#7A7A7A' },
//       BKMapChart: {
//         tooltipBackgroundColor: '#fff',
//         tooltipBorderColor: '#333',
//         textColor: '#333333',
//         tooltipColor: '#7A7A7A',
//         visualMapTextColor: '#666666',
//         eTextColor: '#333333',
//         sTextColor: '#333333',
//         areaColor: '#D9EEFF',
//         eAreaColor: '#FFAE00',
//         sAreaColor: '#FFAE00',
//         borderColor: '#fff',
//         eBorderColor: '#fff',
//         sBorderColor: '#fff',
//         mapColorList: ['#D9EEFF', '#2F9BFF']
//       },
//       BKTableChart:
//         {
//           '--borderColor': '#e8eaec',
//           '--stripeColor': '#fafafa',
//           '--bodyBg': '#fff',
//           '--headerBg': '#f8f8f9',
//           '--bodycolor': '#606266',
//           '--bodyfontSize': 12,
//           '--bodyfontFamily': 'Microsoft YaHei',
//           '--bodyfontWeight': 'normal',
//           '--rowCurrentColor': '#e6f7ff',
//           '--headercolor': '#606266',
//           '--headerfontSize': 12,
//           '--headerfontFamily': 'Microsoft YaHei',
//           '--headerfontWeight': 'normal'
//         },
//       BKStatisticsChart: { numColor: '#409eff', textColor: '#747c8b' },
//       progressChart: { titleColor: '#468EFD', barBackgroundColor: '#f7f7f7', barColor: '#468EFD' }
//     }
//   }
// }
export const defaultThemeList = [
  {
    key: 'default',
    name: '浅色系',
    isDefault: '1',
    flag: '0',
    config: {
      preTheme: 'default',
      themeMode: 'default',
      themeList: [
        {
          value: 'default',
          label: '默认',
          config: {
            globalCss: {
            // default, blue
              themeMode: 'default',
              // 页面背景颜色
              backgroundColor: '#fff',
              // 卡片背景颜色
              cardBackgroundColor: 'linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.54))',
              cardBackground: {
                backgroundImage: '',
                backgroundRepeat: 'repeat'
              },
              pageBackground: {
                backgroundImage: '',
                backgroundRepeat: 'repeat'
              },
              bgBorder: {
                width: 0,
                color: 'transparent',
                type: 'solid'
              },
              cardStyle: {
                width: 1,
                color: '#e9e9e9',
                type: 'solid'
              },
              // 查询区域样式
              queryStyle: {
                backgroundColor: '#fff',
                width: 1,
                color: '#e9e9e9',
                type: 'solid',
                radius: 0
              },
              // 操作按钮样式
              operateStyle: {
                color: '#a3a3a3',
                fontSize: 12,
                fontFamily: 'Microsoft YaHei',
                fontWeight: 'normal'
              },
              // 文本样式
              textStyle: {
                color: '#000',
                fontSize: 14,
                fontFamily: 'Microsoft YaHei',
                fontWeight: 'normal'
              },
              // 页签基础样式
              tabCommonStyle: {
                color: '#747c8b',
                fontSize: 14,
                height: 35, // 高度
                fontWeight: 'bold', // 选中文字粗细
                backgroundColor: '#fff' // 背景颜色
              },
              // 页签样式
              tabStyle: {
                type: 'underline', // underline 下划线, splitline 分割线,block 选中块, custom 自定义
                underlineStyle: {
                  textColor: '#1f6aff', // 选中文字颜色
                  lineColor: '#1f6aff', // 选中下划线颜色
                  lineWidth: 2 // 选中下划线宽度
                },
                splitlineStyle: {
                  lineWidth: 1, // 分割线宽度
                  lineColor: '#d2d3d9', // 分割线颜色
                  textColor: '#1f6aff' // 选中文字颜色
                },
                blockStyle: {
                  radius: 4, // 圆角
                  padding: 0, // 间距
                  textColor: '#1f6aff', // 选中文字颜色
                  backgroundColor: 'rgba(31,106,255,0.1)', // 选中背景颜色
                  borderWidth: 0, // 边框
                  borderColor: '#d2d3d9',
                  borderType: 'solid',
                  selectedBorderWidth: 0,
                  selectedBorderColor: '#fff'
                },
                customStyle: {
                  backgroundImage: '',
                  backgroundRepeat: 'no-repeat'
                }
              },
              // 页签分隔线
              tabSplitStyle: {
                width: 1,
                color: '#e8eaec',
                type: 'solid'
              },
              // 卡片阴影
              cardShadow: {
                shadowBlur: 0,
                shadowColor: 'transparent',
                shadowOffsetX: 0,
                shadowOffsetY: 0
              },
              // none, small, big
              cardRadius: 'small',
              // compact, normal, custom
              spaceMode: 'compact',
              margin: [10, 10],
              cardPadding: {
                paddingTop: 0,
                paddingBottom: 0,
                paddingLeft: 0,
                paddingRight: 0
              },
              childConfig: {
                mergeChildChart: 'independence', // 子页面组件合并
                colNum: 12,
                rowHeight: 5,
                margin: [0, 5]
              },
              // 素材样式
              decorateStyle: {
                color: '#000',
                fontSize: 12,
                fontFamily: 'Microsoft YaHei',
                fontWeight: 'normal'
              }
            },
            pageLayout: {
              pageWidth: 900,
              pageMinWidth: 1000,
              pageMinHeight: 500,
              // adaptive, custom
              pageWidthMode: 'adaptive',
              pageHeightMode: 'auto',
              pagePadding: {
                paddingTop: 0,
                paddingBottom: 0,
                paddingLeft: 0,
                paddingRight: 0
              }
            },
            globalCustomStyle: `.vgl-item {
            & > .vgl-item__resizer {
              display: inline-block;

              &::before {
                content: "\\e652";
                font-size: 12px;
                color: #7a7a7a;
              }
            }
          }

          .set-chart-content {
            &:hover {
              cursor: pointer;
              border: 1px dashed #1f6aff !important;
            }

            &.foucs-chart-content {
              border: 1px solid #1777f5 !important;
            }
          }

          .empty-container {
            .empty-img {
              background: url("/profile/upload/am-image/2024/07/02/F1807946128328830976.png") no-repeat;
            }
          }`,
            chartStyle: {
              defaultColors: [
                '#409EFF,#67C23A,#E6A23C,#F56C6C,#39D895,#ba55d3',
                '#E6A23C,#F56C6C,#909399,#E6A23C,#409EFF,#67C23A',
                '#ff7f50,#87cefa,#da70d6,#32cd32,#6495ed,#ff69b4,#ba55d3,#cd5c5c,#ffa500,#40e0d0'
              ],
              themeStyle: {
                textColor: '#7A7A7A',
                backgroundColor: '#fff',
                borderColor: '#333',
                fontSize: 12,
                colorList: [
                  '#00E4BF',
                  '#FFC72F',
                  '#008FFF',
                  '#3ED848',
                  '#C96765',
                  '#D8E5FA'
                ]
              },
              BKGaugeChart: {
                activeColor: '#468EFD',
                bgColor: '#111F42',
                textColor: '#468EFD'
              },
              BKRankChart: {
                '--progressBgColor': '#f8f8f9',
                '--numcolor': '#424e61',
                '--numfontSize': 12,
                '--numfontWeight': 'bold',
                '--titlecolor': '#000',
                '--titlefontSize': 12,
                '--titlefontWeight': 'normal'
              },
              BKBarChart: {
                axisLineColor: '#eaeff4',
                splitLineColor: '#e6e6e6',
                textColor: '#7A7A7A'
              },
              BKMapChart: {
                tooltipBackgroundColor: '#fff',
                tooltipBorderColor: '#333',
                textColor: '#333333',
                tooltipColor: '#7A7A7A',
                visualMapTextColor: '#666666',
                eTextColor: '#333333',
                sTextColor: '#333333',
                areaColor: '#D9EEFF',
                eAreaColor: '#FFAE00',
                sAreaColor: '#FFAE00',
                borderColor: '#fff',
                eBorderColor: '#fff',
                sBorderColor: '#fff',
                mapColorList: [
                  '#D9EEFF',
                  '#2F9BFF'
                ]
              },
              BKTableChart: {
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
              BKStatisticsChart: {
                numColor: '#409eff',
                textColor: '#747c8b'
              },
              progressChart: {
                titleColor: '#468EFD',
                barBackgroundColor: '#f7f7f7',
                barColor: '#468EFD'
              },
              legend: {
                show: true,
                itemGap: 10,
                itemHeight: 15,
                itemWidth: 25,
                textStyle: {
                  color: '#7A7A7A',
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
                backgroundColor: '#fff',
                padding: 5,
                borderWidth: 0,
                borderColor: '#000',
                textStyle: {
                  color: '#7A7A7A',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'normal'
                }
              },
              table: {
                body: {
                  styles: {
                    '--borderColor': '#e8eaec',
                    '--stripeColor': '#fafafa',
                    '--bodyBg': '#fff',
                    '--tablecellheight': 48,
                    '--rowCurrentColor': '#e6f7ff',
                    '--bodyfontSize': 12,
                    '--bodycolor': '#7A7A7A'
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
                    '--headercolor': '#606266',
                    '--headerfontSize': 12,
                    '--headercellheight': 48,
                    '--headerBg': '#f8f8f9',
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
                  color: '#7A7A7A',
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
                  color: '#7A7A7A',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'bold'
                },
                axisLine: {
                  show: true,
                  lineStyle: {
                    width: 1,
                    color: '#eaeff4',
                    type: 'solid'
                  }
                },
                axisTick: {
                  show: true,
                  inside: 0,
                  length: 5,
                  lineStyle: {
                    width: 1,
                    color: '#DDD'
                  }
                },
                splitLine: {
                  show: false,
                  lineStyle: {
                    width: 1,
                    color: '#eaeff4',
                    type: 'dashed'
                  }
                },
                nameStyle: {
                  nameGap: 15,
                  nameLocation: 'end',
                  nameTextStyle: {
                    color: '#7A7A7A',
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
                  color: '#7A7A7A',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'bold'
                },
                axisLine: {
                  show: false,
                  lineStyle: {
                    width: 1,
                    color: '#eaeff4',
                    type: 'solid'
                  }
                },
                axisTick: {
                  show: false,
                  inside: 0,
                  length: 5,
                  lineStyle: {
                    width: 1,
                    color: '#DDD'
                  }
                },
                splitLine: {
                  show: true,
                  lineStyle: {
                    width: 1,
                    color: '#eaeff4',
                    type: 'dashed'
                  }
                },
                nameStyle: {
                  nameGap: 15,
                  nameLocation: 'end',
                  nameTextStyle: {
                    color: '#7A7A7A',
                    fontSize: 12,
                    fontFamily: 'Microsoft YaHei',
                    fontWeight: 'bold'
                  }
                }
              },
              map: {
                select: {
                  itemStyle: {
                    areaColor: '#FFAE00',
                    borderColor: '',
                    borderWidth: 0
                  },
                  label: {
                    show: true,
                    color: '#333333'
                  }
                },
                emphasis: {
                  itemStyle: {
                    areaColor: '#FFAE00',
                    borderColor: '',
                    borderWidth: 0
                  },
                  label: {
                    show: true,
                    color: '#333333'
                  }
                },
                normal: {
                  itemStyle: {
                    areaColor: '#D9EEFF',
                    borderColor: '',
                    borderWidth: 0
                  },
                  label: {
                    show: true,
                    color: '#333333'
                  }
                }
              },
              BKPolarChart: {
                axisLineColor: '#eaeff4',
                splitLineColor: '#e6e6e6'
              }
            }
          }
        },
        {
          value: 'blue',
          label: '深蓝',
          config: {
            globalCss: {
            // default, blue
              themeMode: 'blue',
              // 页面背景颜色
              backgroundColor: '#001132',
              pageBackground: {
                backgroundImage: '',
                backgroundRepeat: 'repeat'
              },
              // 卡片背景颜色
              cardBackgroundColor: 'linear-gradient(180deg, rgba(5, 29, 79, 0), rgba(8, 54, 129, 0.6))',
              cardBackground: {
                backgroundImage: '',
                backgroundRepeat: 'repeat'
              },
              // 背景边框
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
              // 查询区域样式
              queryStyle: {
                backgroundColor: 'transparent',
                width: 0,
                color: '',
                type: 'solid',
                radius: 0
              },
              // 操作按钮样式
              operateStyle: {
                color: '#a3a3a3',
                fontSize: 12,
                fontFamily: 'Microsoft YaHei',
                fontWeight: 'bold'
              },
              // 文本样式
              textStyle: {
                color: '#fff',
                fontSize: 14,
                fontFamily: 'Microsoft YaHei',
                fontWeight: 'normal'
              },
              // 页签基础样式
              tabCommonStyle: {
                color: '#fff',
                fontSize: 14,
                height: 35, // 高度,
                fontWeight: 'bold', // 选中文字粗细
                backgroundColor: 'linear-gradient(90deg, rgba(0, 99, 255, 0.71) 37%, rgba(20, 64, 151, 0))' // 背景颜色
              },
              // 页签样式
              tabStyle: {
                type: 'custom', // underline 下划线, splitline 分割线,block 选中块, custom 自定义
                underlineStyle: {
                  textColor: '#fff', // 选中文字颜色
                  lineColor: '#0c214a', // 选中下划线颜色
                  lineWidth: 2 // 选中下划线宽度
                },
                splitlineStyle: {
                  lineWidth: 1, // 分割线宽度
                  lineColor: '#fff', // 分割线颜色
                  textColor: '#fff' // 选中文字颜色
                },
                blockStyle: {
                  radius: 4, // 圆角
                  padding: 0, // 间距
                  textColor: '#fff', // 选中文字颜色
                  backgroundColor: '#1f6aff', // 选中背景颜色
                  borderWidth: 0, // 边框
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
              // 页签分隔线
              tabSplitStyle: {
                width: 0,
                color: '',
                type: 'solid'
              },
              // none, small, big
              cardRadius: 'none',
              // compact, normal, custom
              spaceMode: 'normal',
              margin: [20, 20],
              cardPadding: {
                paddingTop: 0,
                paddingBottom: 0,
                paddingLeft: 0,
                paddingRight: 0
              },
              // 卡片阴影
              cardShadow: {
                shadowBlur: 0,
                shadowColor: 'transparent',
                shadowOffsetX: 0,
                shadowOffsetY: 0
              },
              childConfig: {
                mergeChildChart: 'independence', // 子页面组件合并
                colNum: 12,
                rowHeight: 5,
                margin: [0, 5]
              },
              // 素材样式
              decorateStyle: {
                color: '#fff',
                fontSize: 12,
                fontFamily: 'Microsoft YaHei',
                fontWeight: 'normal'
              }
            },
            pageLayout: {
              pageWidth: '100%',
              pageMinWidth: 1000,
              pageMinHeight: 800,
              // adaptive, custom
              pageWidthMode: 'custom',
              pageHeightMode: 'auto',
              pagePadding: {
                paddingTop: 10,
                paddingBottom: 0,
                paddingLeft: 0,
                paddingRight: 0
              }
            },
            globalCustomStyle: ` .vgl-item {
            & > .vgl-item__resizer::before {
              content: "\\e652";
            }
          }

          .set-chart-content {
            &:hover {
              cursor: pointer;
            }

            &.foucs-chart-content {
              border: 1px solid rgba(18, 219, 255, 0.5) !important;
              box-shadow: 0 0 9px rgba(0, 208, 239, 0.49);
            }
          }

          .chart-title {
            border-radius: 4px 4px 0 0;
            height: 33px;

            &::before {
              content: "";
              height: 1px;
              width: 100%;
              background: linear-gradient(221deg, rgba(15, 88, 233, 0) 5%, #0f58e9 58%, #0f58e9 98%);
              position: absolute;
              bottom: 0;
            }
          }

          .chart-content-main::before,
          .item-layout::before {
            content: "";
            height: 0.5px;
            width: 70%;
            border: 0.5px solid;
            border-image: linear-gradient(90deg, rgba(9, 56, 102, 0), #fff 53%, rgba(8, 19, 96, 0)) 1 1;
            position: absolute;
            bottom: 0;
            left: 15%;
            z-index: -999;
          }

          .chart-content-main::after,
          .item-layout::after {
            content: "";
            height: 72px;
            width: 100%;
            background: linear-gradient(180deg, rgba(62, 94, 255, 0), #3e7dff);
            filter: blur(72px);
            position: absolute;
            bottom: 0;
            z-index: -999;
          }

          .merge-child-wrapper .chart-content-main::before {
            content: none;
          }

          .merge-child-wrapper .chart-content-main::after {
            content: none;
          }

          .vue-draggable-handle {
            color: #a3a3a3 !important;
          }

          .vgl-item__resizer {
            background: transparent;
          }

          .empty-area {
            background: linear-gradient(180deg, #072161, #0a2c84);
            border-radius: 8px;
            box-shadow: 0 0 11px 0 rgba(33, 171, 234, 0.5) inset;
          }

          .empty-container {
            .empty-img {
              background: url("/profile/upload/am-image/2024/07/02/F1807946072943046656.png") no-repeat;
            }
          }`,
            chartStyle: {
              defaultColors: [
                '#409EFF,#67C23A,#E6A23C,#F56C6C,#39D895,#ba55d3',
                '#E6A23C,#F56C6C,#909399,#E6A23C,#409EFF,#67C23A',
                '#ff7f50,#87cefa,#da70d6,#32cd32,#6495ed,#ff69b4,#ba55d3,#cd5c5c,#ffa500,#40e0d0'
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
                ]
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
                splitLineColor: '#1E54BE'
              }
            }
          }
        }
      ]
    }
  },
  {
    key: 'blue',
    name: '深色系',
    isDefault: '0',
    flag: '0',
    config: {
      preTheme: 'blue',
      themeMode: 'blue',
      themeList: [
        {
          value: 'default',
          label: '默认',
          config: {
            globalCss: {
            // default, blue
              themeMode: 'default',
              // 页面背景颜色
              backgroundColor: '#fff',
              // 卡片背景颜色
              cardBackgroundColor: 'linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.54))',
              cardBackground: {
                backgroundImage: '',
                backgroundRepeat: 'repeat'
              },
              pageBackground: {
                backgroundImage: '',
                backgroundRepeat: 'repeat'
              },
              bgBorder: {
                width: 0,
                color: 'transparent',
                type: 'solid'
              },
              cardStyle: {
                width: 1,
                color: '#e9e9e9',
                type: 'solid'
              },
              // 查询区域样式
              queryStyle: {
                backgroundColor: '#fff',
                width: 1,
                color: '#e9e9e9',
                type: 'solid',
                radius: 0
              },
              // 操作按钮样式
              operateStyle: {
                color: '#a3a3a3',
                fontSize: 12,
                fontFamily: 'Microsoft YaHei',
                fontWeight: 'normal'
              },
              // 文本样式
              textStyle: {
                color: '#000',
                fontSize: 14,
                fontFamily: 'Microsoft YaHei',
                fontWeight: 'normal'
              },
              // 页签基础样式
              tabCommonStyle: {
                color: '#747c8b',
                fontSize: 14,
                height: 35, // 高度
                fontWeight: 'bold', // 选中文字粗细
                backgroundColor: '#fff' // 背景颜色
              },
              // 页签样式
              tabStyle: {
                type: 'underline', // underline 下划线, splitline 分割线,block 选中块, custom 自定义
                underlineStyle: {
                  textColor: '#1f6aff', // 选中文字颜色
                  lineColor: '#1f6aff', // 选中下划线颜色
                  lineWidth: 2 // 选中下划线宽度
                },
                splitlineStyle: {
                  lineWidth: 1, // 分割线宽度
                  lineColor: '#d2d3d9', // 分割线颜色
                  textColor: '#1f6aff' // 选中文字颜色
                },
                blockStyle: {
                  radius: 4, // 圆角
                  padding: 0, // 间距
                  textColor: '#1f6aff', // 选中文字颜色
                  backgroundColor: 'rgba(31,106,255,0.1)', // 选中背景颜色
                  borderWidth: 0, // 边框
                  borderColor: '#d2d3d9',
                  borderType: 'solid',
                  selectedBorderWidth: 0,
                  selectedBorderColor: '#fff'
                },
                customStyle: {
                  backgroundImage: '',
                  backgroundRepeat: 'no-repeat'
                }
              },
              // 页签分隔线
              tabSplitStyle: {
                width: 1,
                color: '#e8eaec',
                type: 'solid'
              },
              // 卡片阴影
              cardShadow: {
                shadowBlur: 0,
                shadowColor: 'transparent',
                shadowOffsetX: 0,
                shadowOffsetY: 0
              },
              // none, small, big
              cardRadius: 'small',
              // compact, normal, custom
              spaceMode: 'compact',
              margin: [10, 10],
              cardPadding: {
                paddingTop: 0,
                paddingBottom: 0,
                paddingLeft: 0,
                paddingRight: 0
              },
              childConfig: {
                mergeChildChart: 'independence', // 子页面组件合并
                colNum: 12,
                rowHeight: 5,
                margin: [0, 5]
              },
              // 素材样式
              decorateStyle: {
                color: '#000',
                fontSize: 12,
                fontFamily: 'Microsoft YaHei',
                fontWeight: 'normal'
              }
            },
            pageLayout: {
              pageWidth: 900,
              pageMinWidth: 1000,
              pageMinHeight: 500,
              // adaptive, custom
              pageWidthMode: 'adaptive',
              pageHeightMode: 'auto',
              pagePadding: {
                paddingTop: 0,
                paddingBottom: 0,
                paddingLeft: 0,
                paddingRight: 0
              }
            },
            globalCustomStyle: `.vgl-item {
            & > .vgl-item__resizer {
              display: inline-block;

              &::before {
                content: "\\e652";
                font-size: 12px;
                color: #7a7a7a;
              }
            }
          }

          .set-chart-content {
            &:hover {
              cursor: pointer;
              border: 1px dashed #1f6aff !important;
            }

            &.foucs-chart-content {
              border: 1px solid #1777f5 !important;
            }
          }

          .empty-container {
            .empty-img {
              background: url("/profile/upload/am-image/2024/07/02/F1807946128328830976.png") no-repeat;
            }
          }`,
            chartStyle: {
              defaultColors: [
                '#409EFF,#67C23A,#E6A23C,#F56C6C,#39D895,#ba55d3',
                '#E6A23C,#F56C6C,#909399,#E6A23C,#409EFF,#67C23A',
                '#ff7f50,#87cefa,#da70d6,#32cd32,#6495ed,#ff69b4,#ba55d3,#cd5c5c,#ffa500,#40e0d0'
              ],
              themeStyle: {
                textColor: '#7A7A7A',
                backgroundColor: '#fff',
                borderColor: '#333',
                fontSize: 12,
                colorList: [
                  '#00E4BF',
                  '#FFC72F',
                  '#008FFF',
                  '#3ED848',
                  '#C96765',
                  '#D8E5FA'
                ]
              },
              BKGaugeChart: {
                activeColor: '#468EFD',
                bgColor: '#111F42',
                textColor: '#468EFD'
              },
              BKRankChart: {
                '--progressBgColor': '#f8f8f9',
                '--numcolor': '#424e61',
                '--numfontSize': 12,
                '--numfontWeight': 'bold',
                '--titlecolor': '#000',
                '--titlefontSize': 12,
                '--titlefontWeight': 'normal'
              },
              BKBarChart: {
                axisLineColor: '#eaeff4',
                splitLineColor: '#e6e6e6',
                textColor: '#7A7A7A'
              },
              BKMapChart: {
                tooltipBackgroundColor: '#fff',
                tooltipBorderColor: '#333',
                textColor: '#333333',
                tooltipColor: '#7A7A7A',
                visualMapTextColor: '#666666',
                eTextColor: '#333333',
                sTextColor: '#333333',
                areaColor: '#D9EEFF',
                eAreaColor: '#FFAE00',
                sAreaColor: '#FFAE00',
                borderColor: '#fff',
                eBorderColor: '#fff',
                sBorderColor: '#fff',
                mapColorList: [
                  '#D9EEFF',
                  '#2F9BFF'
                ]
              },
              BKTableChart: {
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
              BKStatisticsChart: {
                numColor: '#409eff',
                textColor: '#747c8b'
              },
              progressChart: {
                titleColor: '#468EFD',
                barBackgroundColor: '#f7f7f7',
                barColor: '#468EFD'
              },
              legend: {
                show: true,
                itemGap: 10,
                itemHeight: 15,
                itemWidth: 25,
                textStyle: {
                  color: '#7A7A7A',
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
                backgroundColor: '#fff',
                padding: 5,
                borderWidth: 0,
                borderColor: '#000',
                textStyle: {
                  color: '#7A7A7A',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'normal'
                }
              },
              table: {
                body: {
                  styles: {
                    '--borderColor': '#e8eaec',
                    '--stripeColor': '#fafafa',
                    '--bodyBg': '#fff',
                    '--tablecellheight': 48,
                    '--rowCurrentColor': '#e6f7ff',
                    '--bodyfontSize': 12,
                    '--bodycolor': '#7A7A7A'
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
                    '--headercolor': '#606266',
                    '--headerfontSize': 12,
                    '--headercellheight': 48,
                    '--headerBg': '#f8f8f9',
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
                  color: '#7A7A7A',
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
                  color: '#7A7A7A',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'bold'
                },
                axisLine: {
                  show: true,
                  lineStyle: {
                    width: 1,
                    color: '#eaeff4',
                    type: 'solid'
                  }
                },
                axisTick: {
                  show: true,
                  inside: 0,
                  length: 5,
                  lineStyle: {
                    width: 1,
                    color: '#DDD'
                  }
                },
                splitLine: {
                  show: false,
                  lineStyle: {
                    width: 1,
                    color: '#eaeff4',
                    type: 'dashed'
                  }
                },
                nameStyle: {
                  nameGap: 15,
                  nameLocation: 'end',
                  nameTextStyle: {
                    color: '#7A7A7A',
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
                  color: '#7A7A7A',
                  fontSize: 12,
                  fontFamily: 'Microsoft YaHei',
                  fontWeight: 'bold'
                },
                axisLine: {
                  show: false,
                  lineStyle: {
                    width: 1,
                    color: '#eaeff4',
                    type: 'solid'
                  }
                },
                axisTick: {
                  show: false,
                  inside: 0,
                  length: 5,
                  lineStyle: {
                    width: 1,
                    color: '#DDD'
                  }
                },
                splitLine: {
                  show: true,
                  lineStyle: {
                    width: 1,
                    color: '#eaeff4',
                    type: 'dashed'
                  }
                },
                nameStyle: {
                  nameGap: 15,
                  nameLocation: 'end',
                  nameTextStyle: {
                    color: '#7A7A7A',
                    fontSize: 12,
                    fontFamily: 'Microsoft YaHei',
                    fontWeight: 'bold'
                  }
                }
              },
              map: {
                select: {
                  itemStyle: {
                    areaColor: '#FFAE00',
                    borderColor: '',
                    borderWidth: 0
                  },
                  label: {
                    show: true,
                    color: '#333333'
                  }
                },
                emphasis: {
                  itemStyle: {
                    areaColor: '#FFAE00',
                    borderColor: '',
                    borderWidth: 0
                  },
                  label: {
                    show: true,
                    color: '#333333'
                  }
                },
                normal: {
                  itemStyle: {
                    areaColor: '#D9EEFF',
                    borderColor: '',
                    borderWidth: 0
                  },
                  label: {
                    show: true,
                    color: '#333333'
                  }
                }
              },
              BKPolarChart: {
                axisLineColor: '#eaeff4',
                splitLineColor: '#e6e6e6'
              }
            }
          }
        },
        {
          value: 'blue',
          label: '深蓝',
          config: {
            globalCss: {
            // default, blue
              themeMode: 'blue',
              // 页面背景颜色
              backgroundColor: '#001132',
              pageBackground: {
                backgroundImage: '',
                backgroundRepeat: 'repeat'
              },
              // 卡片背景颜色
              cardBackgroundColor: 'linear-gradient(180deg, rgba(5, 29, 79, 0), rgba(8, 54, 129, 0.6))',
              cardBackground: {
                backgroundImage: '',
                backgroundRepeat: 'repeat'
              },
              // 背景边框
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
              // 查询区域样式
              queryStyle: {
                backgroundColor: 'transparent',
                width: 0,
                color: '',
                type: 'solid',
                radius: 0
              },
              // 操作按钮样式
              operateStyle: {
                color: '#a3a3a3',
                fontSize: 12,
                fontFamily: 'Microsoft YaHei',
                fontWeight: 'bold'
              },
              // 文本样式
              textStyle: {
                color: '#fff',
                fontSize: 14,
                fontFamily: 'Microsoft YaHei',
                fontWeight: 'normal'
              },
              // 页签基础样式
              tabCommonStyle: {
                color: '#fff',
                fontSize: 14,
                height: 35, // 高度,
                fontWeight: 'bold', // 选中文字粗细
                backgroundColor: 'linear-gradient(90deg, rgba(0, 99, 255, 0.71) 37%, rgba(20, 64, 151, 0))' // 背景颜色
              },
              // 页签样式
              tabStyle: {
                type: 'custom', // underline 下划线, splitline 分割线,block 选中块, custom 自定义
                underlineStyle: {
                  textColor: '#fff', // 选中文字颜色
                  lineColor: '#0c214a', // 选中下划线颜色
                  lineWidth: 2 // 选中下划线宽度
                },
                splitlineStyle: {
                  lineWidth: 1, // 分割线宽度
                  lineColor: '#fff', // 分割线颜色
                  textColor: '#fff' // 选中文字颜色
                },
                blockStyle: {
                  radius: 4, // 圆角
                  padding: 0, // 间距
                  textColor: '#fff', // 选中文字颜色
                  backgroundColor: '#1f6aff', // 选中背景颜色
                  borderWidth: 0, // 边框
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
              // 页签分隔线
              tabSplitStyle: {
                width: 0,
                color: '',
                type: 'solid'
              },
              // none, small, big
              cardRadius: 'none',
              // compact, normal, custom
              spaceMode: 'normal',
              margin: [20, 20],
              cardPadding: {
                paddingTop: 0,
                paddingBottom: 0,
                paddingLeft: 0,
                paddingRight: 0
              },
              // 卡片阴影
              cardShadow: {
                shadowBlur: 0,
                shadowColor: 'transparent',
                shadowOffsetX: 0,
                shadowOffsetY: 0
              },
              childConfig: {
                mergeChildChart: 'independence', // 子页面组件合并
                colNum: 12,
                rowHeight: 5,
                margin: [0, 5]
              },
              // 素材样式
              decorateStyle: {
                color: '#fff',
                fontSize: 12,
                fontFamily: 'Microsoft YaHei',
                fontWeight: 'normal'
              }
            },
            pageLayout: {
              pageWidth: '100%',
              pageMinWidth: 1000,
              pageMinHeight: 800,
              // adaptive, custom
              pageWidthMode: 'custom',
              pageHeightMode: 'auto',
              pagePadding: {
                paddingTop: 10,
                paddingBottom: 0,
                paddingLeft: 0,
                paddingRight: 0
              }
            },
            globalCustomStyle: ` .vgl-item {
            & > .vgl-item__resizer::before {
              content: "\\e652";
            }
          }

          .set-chart-content {
            &:hover {
              cursor: pointer;
            }

            &.foucs-chart-content {
              border: 1px solid rgba(18, 219, 255, 0.5) !important;
              box-shadow: 0 0 9px rgba(0, 208, 239, 0.49);
            }
          }

          .chart-title {
            border-radius: 4px 4px 0 0;
            height: 33px;

            &::before {
              content: "";
              height: 1px;
              width: 100%;
              background: linear-gradient(221deg, rgba(15, 88, 233, 0) 5%, #0f58e9 58%, #0f58e9 98%);
              position: absolute;
              bottom: 0;
            }
          }

          .chart-content-main::before,
          .item-layout::before {
            content: "";
            height: 0.5px;
            width: 70%;
            border: 0.5px solid;
            border-image: linear-gradient(90deg, rgba(9, 56, 102, 0), #fff 53%, rgba(8, 19, 96, 0)) 1 1;
            position: absolute;
            bottom: 0;
            left: 15%;
            z-index: -999;
          }

          .chart-content-main::after,
          .item-layout::after {
            content: "";
            height: 72px;
            width: 100%;
            background: linear-gradient(180deg, rgba(62, 94, 255, 0), #3e7dff);
            filter: blur(72px);
            position: absolute;
            bottom: 0;
            z-index: -999;
          }

          .merge-child-wrapper .chart-content-main::before {
            content: none;
          }

          .merge-child-wrapper .chart-content-main::after {
            content: none;
          }

          .vue-draggable-handle {
            color: #a3a3a3 !important;
          }

          .vgl-item__resizer {
            background: transparent;
          }

          .empty-area {
            background: linear-gradient(180deg, #072161, #0a2c84);
            border-radius: 8px;
            box-shadow: 0 0 11px 0 rgba(33, 171, 234, 0.5) inset;
          }

          .empty-container {
            .empty-img {
              background: url("/profile/upload/am-image/2024/07/02/F1807946072943046656.png") no-repeat;
            }
          }`,
            chartStyle: {
              defaultColors: [
                '#409EFF,#67C23A,#E6A23C,#F56C6C,#39D895,#ba55d3',
                '#E6A23C,#F56C6C,#909399,#E6A23C,#409EFF,#67C23A',
                '#ff7f50,#87cefa,#da70d6,#32cd32,#6495ed,#ff69b4,#ba55d3,#cd5c5c,#ffa500,#40e0d0'
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
                ]
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
                splitLineColor: '#1E54BE'
              }
            }
          }
        }
      ]
    }
  }
]

// function getThemeConfig (theme, mode, isAll = false) {
//   let themeConfig = preThemeList.find(c => c.key === (theme || 'default')) || {}
//   themeConfig = utils.deepClone(themeConfig)
//   if (mode) {
//     const modeConfig = utils.deepClone(getModeConfig(mode, isAll))
//     themeConfig.config = { ...themeConfig.config, ...modeConfig }
//   }
//   return themeConfig || {}
// }

// function getModeConfig (mode, isAll = false) {
//   const config = XEUtils.clone(themeModeConfig[mode])
//   if (config && !isAll) {
//     delete config.chartStyle
//   }
//   return config || {}
// }
export function getChartTheme (chartStyle, type, branchType) {
  // const theme = chartTheme.split(',')[0]
  // const mode = chartTheme.split(',')[1]
  // const { config } = getThemeConfig(theme, mode, true)
  // const { chartStyle } = config
  let form = {}
  if (chartStyle) {
    form = chartStyle.themeStyle
    if (chartStyle[type] || chartStyle[branchType]) {
      form = { ...form, ...chartStyle[branchType] || chartStyle[type] }
    }
    if (styleMap[type] || styleMap[branchType]) {
      const list = styleMap[branchType] || styleMap[type]
      form.configStyle = {}
      list.forEach(ele => {
        form.configStyle[ele] = XEUtils.clone(chartStyle[ele])
      })
    }
  }
  return form
}
function getAxis (style) {
  return { ...style.nameStyle, ...style }
}
const styleMap = {
  BKBarChart: ['tooltip', 'legend', 'barStyle', 'lineStyle', 'yAxis', 'xAxis'],
  BKPieChart: ['tooltip', 'legend', 'pieStyle'],
  BKMapChart: ['tooltip', 'map'],
  progressChart: ['tooltip'],
  BKPolarChart: ['tooltip', 'legend'],
  BKTableChart: ['table'],
  BKRadarChart: ['tooltip', 'legend']
}
function BKBarChartThemeInit (option, { textColor, axisLineColor, fontSize, colorList, splitLineColor, backgroundColor, borderColor, configStyle }, branchType) {
  const series = option.series
  const seriesList = []
  const barStyle = configStyle?.barStyle || {}
  const lineStyle = configStyle?.lineStyle || {}
  let borderRadius = barStyle?.borderRadius || [0, 0, 0, 0]
  if (branchType === 'barRadiusChart') {
    borderRadius = option?.borderRadius
  }
  let color = colorList
  if (branchType === 'barLineargradientChart') {
    color = option.color
  }
  if (series?.length) {
    for (let i = 0; i < series.length; i++) {
      let index = i
      if (index >= colorList.length) {
        index = index % colorList.length
        if (index < 0) {
          index = 0
        }
      }
      if (series[i].type === 'bar') {
        seriesList.push({
          barMaxWidth: barStyle?.barWidth || null,
          itemStyle: {
            color: null
          },
          backgroundStyle: {
            color: null
          }
        })
      } else {
        seriesList.push({
          barMaxWidth: barStyle?.barWidth || null,
          backgroundStyle: {
            color: null
          },
          ...lineStyle
        })
      }
    }
  }

  let yAxis = []
  if (Array.isArray(option.yAxis)) {
    option.yAxis.forEach(ele => {
      yAxis.push(configStyle?.yAxis
        ? getAxis(configStyle?.yAxis)
        : {
            splitLine: { lineStyle: { color: splitLineColor } },
            nameTextStyle: { color: textColor, fontSize },
            axisLabel: { color: textColor, fontSize },
            axisLine: { lineStyle: { color: axisLineColor } }
          })
    })
  } else {
    yAxis = configStyle?.yAxis
      ? getAxis(configStyle?.yAxis)
      : {
          splitLine: { lineStyle: { color: splitLineColor } },
          nameTextStyle: { color: textColor, fontSize },
          axisLabel: { color: textColor, fontSize },
          axisLine: { lineStyle: { color: axisLineColor } }
        }
  }
  return {
    color,
    borderRadius,
    legend: configStyle?.legend || { textStyle: { color: textColor, fontSize } },
    tooltip: configStyle?.tooltip || {
      backgroundColor,
      borderColor,
      borderWidth: 0,
      textStyle: {
        color: textColor,
        fontSize
      }
    },
    xAxis: configStyle?.xAxis
      ? getAxis(configStyle?.xAxis)
      : {
          axisLabel: { color: textColor, fontSize },
          nameTextStyle: { color: textColor, fontSize },
          axisLine: {
            lineStyle: { color: axisLineColor }
          }

        },
    yAxis,
    series: seriesList
  }
}

function BKPolarChartThemeInit (option, { axisLineColor, splitLineColor, textColor, fontSize, colorList, backgroundColor, borderColor, configStyle }, branchType) {
  const series = option.series
  const seriesList = []
  const color = colorList
  if (series?.length) {
    for (let i = 0; i < series.length; i++) {
      if (series[i].type === 'bar') {
        seriesList.push({

          itemStyle: {
            color: null
          }

        })
      }
    }
  }
  return {
    color,
    legend: configStyle?.legend || { textStyle: { color: textColor, fontSize } },
    tooltip: configStyle?.tooltip || {
      backgroundColor,
      borderColor,
      borderWidth: 0,
      textStyle: {
        color: textColor,
        fontSize
      }
    },
    radiusAxis: {
      splitLine: { lineStyle: { color: splitLineColor } },
      nameTextStyle: { color: textColor, fontSize },
      axisLabel: { color: textColor, fontSize },
      axisLine: { lineStyle: { color: axisLineColor } }
    },
    angleAxis: {
      axisLabel: { color: textColor, fontSize },
      nameTextStyle: { color: textColor, fontSize },
      axisLine: {
        lineStyle: { color: axisLineColor }
      }

    },
    series: seriesList
  }
}
function BKPieChartThemeInit (option, { textColor, fontSize, colorList, backgroundColor, borderColor, configStyle }) {
  const series = option.series
  const pieStyle = configStyle?.pieStyle
  const seriesList = []
  if (series?.length) {
    for (let i = 0; i < series.length; i++) {
      if (series[i].type === 'surface') {
        let index = i
        if (index >= colorList.length) {
          index = index % colorList.length
          if (index < 0) {
            index = 0
          }
        }
        seriesList.push({
          label: {
            color: textColor
          },
          itemStyle: {
            color: colorList[index]
          }
        })
      } else {
        seriesList.push({
          label: pieStyle?.label || {
            color: textColor
          },
          labelLine: pieStyle?.labelLine || {}
        })
      }
    }
  }

  return {
    tooltip: configStyle?.tooltip || {
      backgroundColor,
      borderColor,
      borderWidth: 0,
      textStyle: {
        color: textColor,
        fontSize
      }
    },
    legend: configStyle?.legend || {
      textStyle: {
        color: textColor,
        fontSize
      }
    },
    color: colorList,
    series: seriesList
  }
}
function BKMapChartThemeInit (option, { tooltipBackgroundColor, tooltipColor, fontSize, tooltipBorderColor, textColor, visualMapTextColor, eTextColor, sTextColor, areaColor, eAreaColor, sAreaColor, borderColor, eBorderColor, sBorderColor, mapColorList, configStyle }) {
  let visualMap = {}

  if (option.visualMap.type === 'piecewise') {
    if (option.visualMap?.pieces?.length) {
      visualMap = { pieces: [] }
      for (let i = 0; i < option.visualMap?.pieces; i++) {
        let index = i
        if (index >= mapColorList.length) {
          index = index % mapColorList.length
          if (index < 0) {
            index = 0
          }
        }
        visualMap.pieces.push({ color: mapColorList[index] })
      }
    }
  } else if (option.visualMap.type === 'continuous') {
    visualMap = { inRange: { color: [mapColorList[0], mapColorList[mapColorList.length - 1]] } }
  }
  visualMap = { visualMap: { ...visualMap, textStyle: { color: visualMapTextColor } } }
  let config = {}
  if (configStyle.map) {
    const { normal, select, emphasis } = configStyle.map
    config = { ...normal, emphasis: { ...emphasis }, select: { ...select } }
  }
  return {
    animation: false,
    tooltip: configStyle?.tooltip || {
      backgroundColor: tooltipBackgroundColor,
      borderColor: tooltipBorderColor,
      borderWidth: 0,
      textStyle: {
        color: tooltipColor,
        fontSize
      }
    },
    geo:
  {
    label: {
      color: textColor
    },
    itemStyle: {
      areaColor,
      borderColor,
      shadowColor: 'rgb(58,115,192)'
    },
    emphasis: {
      label: {
        color: eTextColor
      },
      itemStyle: {
        areaColor: eAreaColor,
        borderColor: eBorderColor
      }
    },
    select: {
      label: {
        color: sTextColor
      },
      itemStyle: { areaColor: sAreaColor, borderColor: sBorderColor, shadowColor: 'rgb(58,115,192)' }

    },
    ...config

  },
    ...visualMap
  }
}
function BKProgressChartThemeInit (option, { configStyle, textColor, fontSize, titleColor, barBackgroundColor, barColor, backgroundColor, borderColor }) {
  const series = option.series
  const seriesList = []
  for (let i = 0; i < series.length; i++) {
    seriesList.push({
      label: {
        color: textColor
      }
    })
  }
  return {
    tooltip: configStyle?.tooltip || {
      backgroundColor,
      borderColor,
      borderWidth: 0,
      textStyle: {
        color: textColor,
        fontSize
      }
    },
    title: {
      text: '',
      textStyle: {
        fontSize: '30',
        color: titleColor
      }
    },
    series: [
      {
        type: 'bar',
        backgroundStyle: {
          color: barBackgroundColor
        },

        itemStyle: {

          color: barColor

        }
      }
    ]
  }
}
function BKRadarChartThemeInit (option, { configStyle, textColor, fontSize, colorList, backgroundColor, borderColor }) {
  return {
    color: colorList,
    tooltip: configStyle?.tooltip || {
      backgroundColor,
      borderColor,
      borderWidth: 0,
      textStyle: {
        color: textColor,
        fontSize
      }
    },
    legend: configStyle?.legend || { textStyle: { color: textColor, fontSize } }
  }
}
function BKGaugeChartThemeInit (option, { activeColor, bgColor, textColor }) {
  return {

    progress: {
      itemStyle: {
        color: activeColor
      }
    },
    axisLine: {
      lineStyle: {
        color: bgColor

      }
    },
    axisLabel: {
      color: activeColor
    },
    axisTick: {

      lineStyle: {
        color: activeColor
      }

    },
    splitLine: {
      lineStyle: {
        color: activeColor
      }
    },
    detail: {
      color: textColor
    },
    title: {
      color: textColor

    },
    data: [],
    pointer: {
      itemStyle: {
        color: activeColor
      }
    }
  }
}
function BKTimeChartThemeInit (option, { activeColor, bgColor, textColor }) {
  return {
    upTextConfig: {
      normal: { color: textColor },
      select: { color: textColor }
    },
    downTextConfig: {
      normal: { color: textColor },
      select: { color: textColor }
    }
  }
}
function BKStatisticsChartThemeInit (option, { numColor, textColor }) {
  return {

    number: {

      textStyle: {
        color: numColor
      }
    },
    unit: {
      textStyle: {
        color: textColor
      }
    },
    text: {

      textStyle: {
        color: textColor
      }
    }
  }
}
function BKTableChartThemeInit (option, { configStyle }) {
  const config = {}
  if (configStyle?.table) {
    const { body, header } = configStyle.table
    config.styles = { ...body.styles, ...header.styles }
    config.props = { ...body.props, ...header.props }
  }
  return config
}
// 全局组件样式
export function getChartStyleInit (chartStyle) {
  // const theme = chartTheme.split(',')[0]
  // const mode = chartTheme.split(',')[1]
  // const { config } = getThemeConfig(theme, mode, true)
  // const { chartStyle } = config
  const chart = {}
  const table = {}
  // 目前使用柱状图样式
  if (chartStyle) {
    const barChartStyle = getChartTheme(chartStyle, 'BKBarChart')
    chart.axisLabel = { fontSize: barChartStyle.fontSize, color: barChartStyle.textColor }
    chart.nameTextStyle = { fontSize: barChartStyle.fontSize, color: barChartStyle.textColor }
    chart.tooltip = { fontSize: barChartStyle.fontSize, color: barChartStyle.textColor }
    chart.legend = { fontSize: barChartStyle.fontSize, color: barChartStyle.textColor }
    chart.colorList = barChartStyle.colorList
    const tableChartStyle = getChartTheme(chartStyle, 'BKTableChart')
    table['--headerfontSize'] = tableChartStyle['--headerfontSize']
    table['--headercolor'] = tableChartStyle['--headercolor']
    table['--bodyfontSize'] = tableChartStyle['--bodyfontSize']
    table['--bodycolor'] = tableChartStyle['--bodycolor']
  }
  return { table, chart }
}
export const chartThemeInit = new Map([['BKGaugeChart', BKGaugeChartThemeInit], ['BKPolarChart', BKPolarChartThemeInit], ['BKRadarChart', BKRadarChartThemeInit], ['BKTableChart', BKTableChartThemeInit], ['BKStatisticsChart', BKStatisticsChartThemeInit], ['BKTimeChart', BKTimeChartThemeInit], ['BKPieChart', BKPieChartThemeInit], ['BKMapChart', BKMapChartThemeInit], ['BKBarChart', BKBarChartThemeInit], ['progressChart', BKProgressChartThemeInit]])

export const styleProps = ['backgroundColor', 'bgBorder', 'cardStyle', 'cardShadow', 'tabSplitStyle', 'textStyle', 'tabCommonStyle', 'tabStyle', 'pageBackground', 'cardBackground', 'cardBackgroundColor', 'queryStyle', 'operateStyle', 'decorateStyle']
const needPxProps = ['width', 'shadowBlur', 'shadowOffsetX', 'shadowOffsetY', 'fontSize',
  'height', 'lineWidth', 'radius', 'padding', 'selectedBorderWidth', 'borderWidth', 'backgroundBlur']
const defaultStyleValue = {
  backgroundImage: 'none'
}
export function getStyleText (configs) {
  let str = ''
  for (let i = 0; i < styleProps.length; i++) {
    str = buildStyleText(configs, styleProps[i], str, '')
  }
  return str
}

function buildStyleText (configs, field, str, preStr) {
  let data = configs[field]
  if (typeof (data) === 'object') {
    preStr = preStr + field
    for (const key in data) {
      str = buildStyleText(data, key, str, preStr)
    }
  } else {
    if (needPxProps.indexOf(field) !== -1) {
      data = data + 'px'
    }
    if (data && field === 'backgroundImage') {
      data = `url(${data})`
    }
    if (!data && defaultStyleValue[field] !== undefined) {
      data = defaultStyleValue[field]
    }
    str = str + `--${preStr}${field}: ${data};`
    if (field === 'backgroundRepeat' && data === 'no-repeat') {
      str = str + `--${preStr}backgroundSize: 100% 100%;`
    }
  }
  return str
}
