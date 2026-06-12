<template>
  <div class="">
    <el-form
      ref="form"
      :disabled="!saveAble"
      size="small"
      label-width="80px"
    >
      <el-form-item
        label="类型"
      >
        <el-select
          v-model="chartComId"
          placeholder="请选择"
          :filterable="false"
          :clearable="false"
          size="small"
          style="width: 100%;"
          @change="changeType"
        >
          <el-option
            v-for="item in chartList"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { allComponents } from '../../configs/chart-com'
import { ElMessageBox } from 'element-plus'
export default {
  name: 'ChartTypeChange',
  inject: ['getSaveAble'],
  props: {
    type: {
      default: '',
      type: String
    }
  },
  emits: ['changeType'],
  data () {
    return {
      chartComId: ''
    }
  },
  computed: {
    chartList () {
      const item = allComponents.find(ele => { return ele.branchType === this.type })
      if (item) {
        return allComponents.filter(ele => { return ele.type === item.type }).map(ele => {
          return { label: ele.title, value: ele.chartComId }
        })
      } else {
        return []
      }
    },
    saveAble () {
      return this.getSaveAble()
    }

  },
  watch: {
    type: {
      handler () {
        if (this.type) {
          const item = allComponents.find(ele => { return ele.branchType === this.type })
          if (item) {
            this.chartComId = item.chartComId
          }
        }
      },
      immediate: true
    }
  },
  created () {
    if (this.type) {
      const item = allComponents.find(ele => { return ele.branchType === this.type })
      if (item) {
        this.chartComId = item.chartComId
      }
    }
  },
  methods: {
    async changeType () {
      this.$nextTick(() => {
        ElMessageBox.confirm('是否切换组件?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          const item = allComponents.find(ele => { return ele.chartComId === this.chartComId })
          this.$emit('changeType', item)
        }).catch(() => {
          this.chartComId = allComponents.find(ele => { return ele.branchType === this.type }).chartComId
        })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.d-flex {
  display: flex;
}

</style>
