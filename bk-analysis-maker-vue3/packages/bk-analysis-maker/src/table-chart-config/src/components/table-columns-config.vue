<template>
  <div class="full-box">
    <div style="width: 100px;">
      <el-switch
        v-model="filterDataEmpty"
        :disabled="!saveAble"
        class="am-switch active-switch"
        size="small"
        @click.stop.prevent
        @change="$emit('chartChange')"
      />
      <span class="chart-config-title">
        无数据列过滤 </span>
    </div>
    <SeriesPlane
      name="列配置"
      :series-list="seriesList"
      @del-series="delSeries"
      @copy-series="copySeries"
      @add-series="addSeries"
      @series-change="$emit('chartChange')"
    >
      <template #header-button>
        <el-tooltip
          class="item"
          effect="dark"
          content="同步表格宽度"
          placement="top"
        >
          <el-button
            style="margin-right: 10px;"
            text
            @click.stop="syncColumn"
          >
            <el-icon><CopyDocument /></el-icon>
          </el-button>
        </el-tooltip>
      </template>
      <template #title="{index}">
        <span>第{{ index + 1 }}列</span>
      </template>
      <template #content="{series,index}">
        <el-form
          :ref="'form'+index"
          :model="series"
          label-width="80px"
          size="small"
          :disabled="!saveAble"
        >
          <el-form-item label="列字段名">
            <el-input
              v-model="series.basic.field"
              size="small"
              @change="$emit('chartChange')"
            />
          </el-form-item>

          <el-form-item
            label="列显示名"
          >
            <el-input
              v-model="series.basic.title"
              size="small"
              @change="$emit('chartChange')"
            />
          </el-form-item>
          <el-form-item
            label="显示"
          >
            <el-switch
              v-model="series.branchProps.visible"
              class="am-switch active-switch"
              size="small"
              @click.stop.prevent
              @change="$emit('chartChange')"
            />
          </el-form-item>
          <el-form-item
            label="排序"
          >
            <el-switch
              v-model="series.branchProps.sortable"
              class="am-switch active-switch"
              size="small"
              @click.stop.prevent
              @change="$emit('chartChange')"
            />
          </el-form-item>
          <el-form-item
            label="宽度"
          >
            <el-input-number
              v-model="series.branchProps.width"
              size="small"
              controls-position="right"
              class="input-number-box-px"
              @change="$emit('chartChange')"
            />
          </el-form-item>
          <el-form-item
            label="最佳宽度"
          >
            <el-input-number
              v-model="series.branchProps.minWidth"
              size="small"
              controls-position="right"
              class="input-number-box-px"
              @change="$emit('chartChange')"
            />
          </el-form-item>

          <el-form-item
            label="对齐"
          >
            <el-radio-group
              v-model="series.branchProps.align"
              size="small"
              @change="$emit('chartChange')"
            >
              <el-radio-button value="left">
                左对齐
              </el-radio-button>
              <el-radio-button value="center">
                居中
              </el-radio-button>
              <el-radio-button value="right">
                右对齐
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item
            label="表头对齐"
          >
            <el-radio-group
              v-model="series.branchProps.headerAlign"
              size="small"
              @change="$emit('chartChange')"
            >
              <el-radio-button value="left">
                左对齐
              </el-radio-button>
              <el-radio-button value="center">
                居中
              </el-radio-button>
              <el-radio-button value="right">
                右对齐
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item
            label="数据类型"
          >
            <el-select
              v-model="series.branchProps.sourceType"
              placeholder="请选择"
              size="small"
              @change="typeChange($event,index)"
            >
              <el-option
                v-for="item in sourceTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item
            label="自定义"
          >
            <el-button
              size="small"
              @click="openConfig(series)"
            >
              设置
            </el-button>
          </el-form-item>
          <DataFormatSelect
            v-show="series.branchProps.sourceType==='value'||series.branchProps.sourceType==='time'"
            v-model="series.branchProps.dataType"
            :type="series.branchProps.sourceType"
            @data-type-change="$emit('chartChange')"
          />
        </el-form>
      </template>
    </SeriesPlane>
    <ColumnCustomConfig
      v-if="configVisible"
      :visible="configVisible"
      :column="currentSeries"
      :set-mode="saveAble"
      @close="configVisible=false"
      @save="saveCustomConfig"
    />
  </div>
</template>

<script>
import SeriesPlane from '../../../chart-common-config/components/series-plane.vue'
import ColumnCustomConfig from './column-custom-config.vue'
import { utils } from 'efficient-suite'
import emitter from '../../../configs/emitter'
export default {
  components: {
    SeriesPlane,
    ColumnCustomConfig
  },
  inject: ['getSaveAble'],
  props: {
    seriesType: {
      type: String,
      default: ''
    }
  },
  emits: ['chartChange', 'syncColumn'],
  data () {
    return {
      show: [],
      seriesList: [],
      filterDataEmpty: false,
      sourceTypeOptions: [{ label: '类目型', value: '' }, { label: '数值型', value: 'value' }, { label: '时间型', value: 'time' }, { label: '下载', value: 'download' }],
      branchProps: {
        width: null,
        minWidth: null,
        sourceType: '',
        dataType: '',
        align: 'center',
        visible: true,
        sortable: false,
        headerAlign: 'center',
        columnsConfig: ''
      },
      currentSeries: { branchProps: {} },
      configVisible: false
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  created () {
    emitter.on('resizeChange', this.resizeColumn)
  },
  beforeUnmount () {
    emitter.off('resizeChange', this.resizeColumn)
  },
  methods: {
    openConfig (series) {
      this.currentSeries = series
      this.configVisible = true
    },
    saveCustomConfig (config) {
      this.currentSeries.branchProps.columnsConfig = config
      this.$emit('chartChange')
    },
    setColumns (data) {
      this.seriesList = utils.deepClone(data).map(ele => {
        return { basic: { field: ele.field, title: ele.title }, branchProps: { ...this.branchProps, ...ele } }
      })
    },
    setAlign (prop, value) {
      for (let i = 0; i < this.seriesList.length; i++) {
        this.seriesList[i].branchProps[prop] = value
      }
    },
    syncColumn () {
      this.$emit('syncColumn')
    },
    setFilterDataEmpty (e) {
      this.filterDataEmpty = e
    },
    saveFormData () {
      const data = this.seriesList.map(ele => {
        return { ...ele.branchProps, ...ele.basic, width: ele.branchProps.width || null, minWidth: ele.branchProps.minWidth || null }
      })
      return { columns: data, filterDataEmpty: this.filterDataEmpty }
    },
    addSeries () {
      this.seriesList.push({ basic: { title: '', id: this.seriesList.length + 1, field: this.seriesList.length + 1 }, branchProps: utils.deepClone(this.branchProps) })
      this.$emit('chartChange')
    },
    copySeries (series) {
      this.seriesList.push({ ...utils.deepClone(series) })
      this.$emit('chartChange')
    },
    delSeries (index) {
      this.seriesList.splice(index, 1)
      this.$emit('chartChange')
    },
    typeChange (e, index) {
      const list = utils.deepClone(this.seriesList)
      list[index].branchProps.dataType = ''
      this.seriesList = list
      this.$emit('chartChange')
    },
    resizeColumn (column) {
      const list = utils.deepClone(this.seriesList)
      list.forEach(item => {
        const data = column.find(ele => { return ele.field === item.basic.field })
        item.branchProps.width = data.width
      })
      this.seriesList = list
      this.$emit('chartChange')
    }
  }
}
</script>

<style lang="scss" scoped>
/* stylelint-disable-next-line scss/at-import-partial-extension */
@import "../../../styles/index.scss";

.flex-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
