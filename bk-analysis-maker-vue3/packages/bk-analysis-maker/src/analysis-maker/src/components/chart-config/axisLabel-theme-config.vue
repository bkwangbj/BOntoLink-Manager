<template>
  <el-form
    ref="form"
    size="small"
    :disabled="!saveAble || !editAble"
    label-width="0px"
  >
    <el-form-item label="">
      <el-row
        :gutter="10"
        type="flex"
      >
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-switch
              v-model="form.show"
              class="am-switch active-switch"
              size="small"
              @click.stop.prevent
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 是否显示</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="form.margin"
              class="input-number-box-px"
              size="small"
              controls-position="right"
              @change="$emit('chartChange')"
            />
            <span class="extra-bottom-text"> 偏移量</span>
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
            <div class="d-flex">
              <div
                v-for="ele in alignOption"
                :key="ele.value"
                class="chart-align-item"
                :class="{'active':alignActive===ele.value,'is-disabled':!saveAble||!editAble}"
                @click="alignChange(ele.value)"
              >
                <i :class="['icon am-iconfont align-icon', ele.icon]" />
              </div>
            </div>
            <span class="extra-bottom-text"> 轴标签角度</span>
          </div>
        </el-col>
        <el-col
          :span="12"
        >
          <div class="d-flex-c">
            <el-input-number
              v-model="alignActive"
              size="small"
              :edit-able="editAble"
              class="input-number-box-angle"
              controls-position="right"
              @change="alignChange"
            />
          </div>
        </el-col>
      </el-row>
    </el-form-item>
    <el-form-item label="">
      <text-style
        ref="textStyleFrom"
        :edit-able="editAble"
        @text-style-change="textStyleChange"
      />
    </el-form-item>
  </el-form>
</template>

<script>
export default {
  name: 'AxisLabelConfig',
  inject: ['getSaveAble'],
  props: {
    editAble: {
      type: Boolean,
      default: true
    }
  },
  emits: ['chartChange'],
  data () {
    return {
      form: {
        show: true,
        margin: 8,
        rotate: 0,
        color: '#7A7A7A',
        fontSize: 12,
        fontFamily: 'Microsoft YaHei',
        fontWeight: 'bold'
      },
      alignActive: 0,
      alignOption: [{ label: '水平', value: 0, icon: 'icon-jiaodu-shuiping' }, { label: '斜角', value: 45, icon: 'icon-jiaodu-xiejiao' }, { label: '垂直', value: -90, icon: 'icon-jiaodu-chuizhi' }]
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  methods: {
    saveFormData () {
      const form = { ...this.form, ...this.$refs.textStyleFrom.getFormData() }
      return form
    },
    setFormData (form) {
      this.form = { ...this.$options.data().form, ...form }
      this.alignActive = this.form.rotate ? this.form.rotate : 0
      this.$refs.textStyleFrom.setFormData(form)
    },
    textStyleChange (textStyle) {
      this.$emit('chartChange')
    },
    alignChange (e) {
      this.alignActive = e
      this.form.rotate = Number(e)
      this.$emit('chartChange')
    }
  }
}
</script>

<style lang="scss" scoped>
.d-flex {
  display: flex;
}

</style>
