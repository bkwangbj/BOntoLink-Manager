<template>
  <SeriesPlane
    name="数据系列"
    :series-list="seriesList"
    @del-series="delSeries"
    @copy-series="copySeries"
    @series-change="$emit('chartChange')"
    @add-series="addSeries"
  >
    <template #title="{index}">
      <span>系列{{ index + 1 }}</span>
    </template>
    <template #content="{series,index}">
      <el-form
        :ref="'form'+index"
        :model="series"
        size="small"
        :disabled="!saveAble"
        label-width="80px"
      >
        <el-form-item
          label-width="80px"
          label="显示系列"
        >
          <el-input
            v-model="series.name"
            size="small"
            @input="nameChange($event,index)"
          />
        </el-form-item>
        <el-form-item
          label-width="80px"
          label="数据系列"
        >
          <el-input
            v-model="series.dataId"
            size="small"
            @input="dataIdChange($event,index)"
          />
        </el-form-item>
        <PieSeriesConfig
          :ref="index+'seriesRef'"
          :series="series"
          @pie-series-change="pieSeriesChange($event,index)"
        />
        <CollapseItem
          v-model="series.hasChildren"
          name="子系列"
          @change="initChildren($event,index)"
        >
          <el-form-item
            label-width="80px"
            label="分组逻辑"
          >
            <div class="d-flex">
              <el-input
                size="small"
                :value="series.collapseRule"
                @input="collapseRuleChange($event,index)"
              />     <el-popover
                :append-to-body="false"
                placement="bottom-start"
                title=""
                popper-class="tooltip-text"
                width="200"
                trigger="click"
              >
                <div>
                  示例：1-,-2,3-4,5 <br>
                  1-：选择大于等于序号为1的数据；<br>
                  -2：选择小于等于序号为2的数据；<br>
                  3-4：选择序号为3到4的数据；<br>
                  5：选择序号为5的数据；<br>
                  以上规则并列，并以,分隔
                </div>

                <template #reference>
                  <el-button

                    class="popover-click"
                    :icon="Warning"
                    circle
                    size="small"
                  />
                </template>
              </el-popover>
            </div>
          </el-form-item>
          <PieSeriesConfig
            :ref="index+'childSeriesRef'"
            :series="series.childSeries"
            @pie-series-change="childSeriesChange($event,index)"
          />
        </CollapseItem>
      </el-form>
    </template>
  </SeriesPlane>
</template>

<script>
import SeriesPlane from '../../../chart-common-config/components/series-plane.vue'
import PieSeriesConfig from './pie-series-config.vue'
import { utils } from 'efficient-suite'
import { Warning } from '@element-plus/icons-vue'
export default {
  components: {
    PieSeriesConfig,
    SeriesPlane
  },
  inject: ['getSaveAble'],
  props: {
    branchType: {
      type: String,
      default: ''
    }
  },
  emits: ['chartChange'],
  data () {
    return {
      Warning: shallowRef(Warning),
      show: [],
      seriesList: []
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  methods: {
    setSeries (data) {
      const formData = utils.deepClone(data)
      this.seriesList = formData
    },
    saveFormData () {
      const data = utils.deepClone(this.seriesList)
      return data
    },
    pieSeriesChange (form, index) {
      this.$nextTick(() => {
        this.seriesList[index] = {
          ...form,
          name: this.seriesList[index].name || '',
          dataId: this.seriesList[index].dataId || '',
          hasChildren: this.seriesList[index].hasChildren,
          childSeries: this.seriesList[index].childSeries || {}
        }
        this.$emit('chartChange')
      })
    },
    childSeriesChange (form, index) {
      this.$nextTick(() => {
        this.seriesList[index].childSeries = { ...form, name: this.seriesList[index].name || '', dataId: this.seriesList[index].dataId || '' }

        this.$emit('chartChange')
      })
    },
    addSeries () {
      const form = { type: 'pie', name: '', dataId: this.seriesList.length + 1 }
      this.seriesList.push(form)
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
    initChildren (e, index) {
      this.$nextTick(() => {
        const arr = utils.deepClone(this.seriesList)
        arr[index].hasChildren = e
        if (e && !this.seriesList[index].childSeries) {
          arr[index].childSeries = { name: this.seriesList[index].name || '', dataId: this.seriesList[index].dataId || '' }
        }
        this.seriesList = arr
        this.$emit('chartChange')
      })
    },
    collapseRuleChange (e, index) {
      const arr = utils.deepClone(this.seriesList)
      arr[index].collapseRule = e
      this.seriesList = arr
      this.$forceUpdate()
      this.$emit('chartChange')
    },
    nameChange (e, index) {
      const arr = utils.deepClone(this.seriesList)
      arr[index].name = e
      this.seriesList = arr
      this.$forceUpdate()
      this.$emit('chartChange')
    },
    dataIdChange (e, index) {
      const arr = utils.deepClone(this.seriesList)
      arr[index].dataId = e
      this.seriesList = arr
      this.$forceUpdate()
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

:deep() {

  .tooltip-text {
    padding: 5px;
    font-size: 12px;
    color: #a3a3a3;
  }

  .popover-click {
    background: transparent;
    border: 0;
  }
}

.el-button.type-icon {
  color: #a3a3a3;
}
</style>
