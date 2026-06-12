<template>
  <div class="full-box">
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
          size="small"
          :model="series"
          label-width="80px"
          :disabled="!saveAble"
        >
          <el-form-item label="显示系列">
            <el-input
              v-model="series.name"
              size="small"
              @change="formChange"
            />
          </el-form-item>

          <el-form-item label="数据系列">
            <el-input
              v-model="series.dataId"
              size="small"
              @change="formChange"
            />
          </el-form-item>
          <el-form-item
            label="颜色"
          >
            <CommonColorPicker
              v-model="series.itemStyle.color"
              size="small"
              :color-modes="['monochrome', 'linear-gradient']"
              @change="$emit('chartChange')"
            />
          </el-form-item>
        </el-form>
      </template>
    </seriesplane>
  </div>
</template>

<script>
import SeriesPlane from '../../../chart-common-config/components/series-plane.vue'
import { utils } from 'efficient-suite'
export default {
  components: {
    SeriesPlane
  },
  inject: ['getSaveAble'],
  props: {
    branchType: {
      type: String,
      default: ''
    },
    colorList: {
      type: Array,
      default: () => []
    },
    seriesLength: {
      type: Number,
      default: 0
    }
  },
  emits: ['chartChange'],
  data () {
    return {
      show: [],
      seriesList: []
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  watch: {
    seriesList: {
      handler (nv, ov) {
        this.$emit('chartChange')
      },
      deep: false

    }
  },
  methods: {
    setSeries (data) {
      const formData = utils.deepClone(data)
      this.seriesList = formData
    },
    saveFormData () {
      const data = utils.deepClone(this.seriesList)
      return { series: data }
    },
    addSeries () {
      this.seriesList.push({ type: 'bar', name: '', coordinateSystem: 'polar', dataId: this.seriesList.length + 1 })

      this.$emit('chartChange')
    },
    copySeries (series) {
      const defaultSeries = utils.deepClone(series)
      this.seriesList.push(defaultSeries)
      this.$emit('chartChange')
    },
    delSeries (index) {
      this.seriesList.splice(index, 1)
      this.$emit('chartChange')
    },
    formChange () {
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
