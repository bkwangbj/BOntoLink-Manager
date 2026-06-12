<template>
  <CollapseItem
    v-model="form.show"
    name="数据图例"
    @change="exLegendShowChange"
  >
    <div class="legend-config">
      <el-form
        size="small"
        label-width="80px"
        :disabled="!form.show||!saveAble"
      >
        <DataFormatSelect
          v-model="form.dataType"
          type="value"
          @data-type-change="$emit('chartChange')"
        />
        <el-form-item
          label="单位"
        >
          <el-row
            :gutter="10"
            type="flex"
          >
            <el-col
              :span="12"
            >
              <div class="d-flex-c">
                <el-input
                  v-model="form.unit"
                  size="small"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text"> 内容</span>
              </div>
            </el-col>
            <el-col
              :span="12"
            >
              <div class="d-flex-c">
                <div class="d-flex">
                  <div
                    v-for="ele in positionOption"
                    :key="ele.value"
                    class="chart-align-item"
                    :class="{'active':form.unitPosition===ele.value,'is-disabled':!saveAble}"
                    @click="unitPositionChange(ele)"
                  >
                    {{ ele.label }}
                  </div>
                </div>
                <span class="extra-bottom-text"> 位置</span>
              </div>
            </el-col>
          </el-row>
        </el-form-item>
        <el-form-item
          label="头内容"
        >
          <el-row
            :gutter="10"
            type="flex"
          >
            <el-col
              :span="12"
            >
              <div class="d-flex-c">
                <el-input
                  v-model="form.header[0]"
                  size="small"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text"> 名称</span>
              </div>
            </el-col>
            <el-col
              :span="12"
            >
              <div class="d-flex-c">
                <el-input
                  v-model="form.header[1]"
                  size="small"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text"> 值</span>
              </div>
            </el-col>
          </el-row>
          <el-row
            :gutter="10"
            type="flex"
          >
            <el-col
              :span="12"
            >
              <div class="d-flex-c">
                <el-input
                  v-model="form.header[2]"
                  size="small"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text"> 比例</span>
              </div>
            </el-col>
          </el-row>
        </el-form-item>
        <el-form-item
          label="显示比例"
        >
          <el-row
            :gutter="10"
            type="flex"
          >
            <el-col
              :span="12"
            >
              <el-switch
                v-model="form.showPerc"
                class="am-switch active-switch"
                size="small"
                @click.stop.prevent
                @change="$emit('chartChange')"
              />
            </el-col>  <el-col
              :span="12"
            >
              <div class="d-flex-c">
                <el-input-number
                  v-model="form.percNumber"
                  size="small"
                  controls-position="right"
                  class="input-number-box"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text"> 保留小数位</span>
              </div>
            </el-col>
          </el-row>
        </el-form-item>
        <el-form-item
          label="位置"
        >
          <div class="d-flex">
            <div
              v-for="ele in alignOption"
              :key="ele.value"
              class="chart-align-item"
              :class="{'active':form.position===ele.value,'is-disabled':!saveAble}"
              @click="alignChange(ele)"
            >
              <i :class="['icon am-iconfont align-icon', ele.icon]" />
            </div>
          </div>
        </el-form-item>
        <el-form-item
          label="偏移量"
        >
          <el-row
            :gutter="10"
            type="flex"
          >
            <el-col
              :span="12"
            >
              <div class="d-flex-c">
                <el-input-number
                  v-model="form.x"
                  size="small"
                  controls-position="right"
                  class="input-number-box-px"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text"> 水平</span>
              </div>
            </el-col>
            <el-col
              :span="12"
            >
              <div class="d-flex-c">
                <el-input-number
                  v-model="form.y"
                  size="small"
                  controls-position="right"
                  class="input-number-box-px"
                  @change="$emit('chartChange')"
                />
                <span class="extra-bottom-text"> 垂直</span>
              </div>
            </el-col>
          </el-row>
        </el-form-item>
      </el-form>
    </div>
  </CollapseItem>
</template>

<script>

export default {

  name: 'ExLegendConfig',
  components: {

  },
  inject: ['getSaveAble'],
  emits: ['chartChange', 'exLegendShowChange'],
  data () {
    return {
      form: {
        show: false,
        unit: '',
        dataType: '',
        header: ['', '', ''],
        position: 'topLeft',
        showPerc: false,
        percNumber: 2,
        unitPosition: 'onHeader',
        x: 0,
        y: 0
      },
      positionOption: Object.freeze([{ label: '表头', value: 'onHeader', icon: 'icon-tuli-dingbujuzuo' }, { label: '数据项', value: 'onData', icon: 'icon-tuli-dingbujuzhong' }]),
      alignOption: Object.freeze([{ label: '顶部居左', value: 'topLeft', icon: 'icon-tuli-dingbujuzuo' }, { label: '顶部居中', value: 'topCenter', icon: 'icon-tuli-dingbujuzhong' }, { label: '顶部居右', value: 'topRight', icon: 'icon-tuli-dingbujuyou' },
        { label: '底部居左', value: 'bottomLeft', icon: 'icon-tuli-dibujuzuo' }, { label: '底部居中', value: 'bottomCenter', icon: 'icon-a-tuli-dibujuzhong-' }, { label: '底部居右', value: 'bottomRight', icon: 'icon-tuli-dibujuyou' }])
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  methods: {
    setFormData (form) {
      Object.assign(this.$data.form, this.$options.data().form)
      this.form = { ...this.form, ...form }
    },
    alignChange (ele) {
      this.form.position = ele.value
      this.$emit('chartChange')
    },
    exLegendShowChange () {
      this.$emit('chartChange')
      this.$emit('exLegendShowChange', this.form.show)
    },

    unitPositionChange (ele) {
      this.form.unitPosition = ele.value
      this.$emit('chartChange')
    },
    saveFormData () {
      return { ...this.form }
    }
  }
}
</script>
<style lang="scss" scoped>
.grid-config {
  display: flex;
  margin-bottom: 10px;
}
</style>
