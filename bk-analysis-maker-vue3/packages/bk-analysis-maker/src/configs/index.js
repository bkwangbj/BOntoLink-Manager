export const libPrefix = 'BK'
export const componentConfigs = {
  getStore: () => { return '' },
  request: null,
  emitter: null,
  // 宿主可注入:新增图表时回调,返回默认数据源(绑定宿主真实数据)。(chartConfig) => { dataSourceConfig, items, autoSeries } | null
  embedDefaultDataSource: null
}
