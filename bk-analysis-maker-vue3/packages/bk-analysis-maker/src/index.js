import AnalysisMaker from './analysis-maker'
import PageLayout from './page-layout'
import RankChart from './rank-chart'
import RankChartConfig from './rank-chart-config'
import RadarChart from './radar-chart'
import RadarChartConfig from './radar-chart-config'
import PieChart from './pie-chart'
import PieChartConfig from './pie-chart-config'
import BarChart from './bar-chart'
import BarChartConfig from './bar-chart-config'
import PolarChart from './polar-chart'
import PolarChartConfig from './polar-chart-config'
import MapChart from './map-chart'
import MapChartConfig from './map-chart-config'
import TableChart from './table-chart'
import TableChartConfig from './table-chart-config'
import GridLayoutContent from './grid-layout-content'
import GridLayoutDecorate from './grid-layout-decorate'
import GaugeChartConfig from './gauge-chart-config'
import GaugeChart from './gauge-chart'
import StatisticsChart from './statistics-chart'
import StatisticsChartConfig from './statistics-chart-config'
import TimeChart from './time-chart'
import TimeChartConfig from './time-chart-config'
import CodeCom from './code-com'
import CodeChart from './code-chart'
import BasicChartConfig from './basic-chart-config'
import DataSourceConfig from './data-source-config'
import VarListenerConfig from './var-listener-config'
import EventConfig from './event-config'
import OperatorConfig from './operator-config'
import { chartComponents } from './chart-common-config/components/index'
import TabLayout from './tab-layout'
import DecorateChart from './decorate-chart'
import './iconfont/iconfont.css'
import { componentConfigs } from './configs'
import './styles/index.scss'
import { stage } from './utils/stage'
const components = [
  AnalysisMaker,
  PageLayout,
  chartComponents,
  PieChart,
  BarChart,
  PolarChart,
  RankChart,
  RankChartConfig,
  MapChart,
  MapChartConfig,
  RadarChartConfig,
  RadarChart,
  TableChart,
  BarChartConfig,
  PolarChartConfig,
  PieChartConfig,
  GaugeChart,
  GaugeChartConfig,
  TimeChart,
  TimeChartConfig,
  TableChartConfig,
  GridLayoutContent,
  CodeCom,
  CodeChart,
  BasicChartConfig,
  DataSourceConfig,
  StatisticsChart,
  StatisticsChartConfig,
  VarListenerConfig,
  EventConfig,
  OperatorConfig,
  TabLayout,
  GridLayoutDecorate,
  DecorateChart
]

const install = function (app, opts = {}) {
  components.forEach(component => {
    let cName = component.name || ''
    cName = cName.charAt(0).toLowerCase() + cName.slice(1)
    component.install(app, opts[cName])
  })
  if (opts.getStore) {
    componentConfigs.getStore = opts.getStore
  }
  componentConfigs.request = opts.request
  componentConfigs.emitter = opts.emitter
  componentConfigs.embedDefaultDataSource = opts.embedDefaultDataSource || null
  app.config.globalProperties.$stage = stage
}

export default {
  install
}
