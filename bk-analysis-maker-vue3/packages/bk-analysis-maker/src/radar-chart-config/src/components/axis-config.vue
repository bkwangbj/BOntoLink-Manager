<template>
  <div class="xAix-cofig">
    <el-form
      :ref="'form-xAix'"
      size="small"
      label-width="80px"
      :disabled="!saveAble"
    >
      <el-form-item label="半径">
        <el-row
          :gutter="10"
          type="flex"
        >
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <el-input-number
                v-model="form.radius"
                size="small"
                controls-position="right"
                class="input-number-box-perc"
                :min="0"
                :max="100"
                @change="$emit('chartChange')"
              />
              <span class="extra-bottom-text" />
            </div>
          </el-col>
        </el-row>
      </el-form-item>
      <el-form-item label="中心位置">
        <el-row
          :gutter="10"
          type="flex"
        >
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <el-input-number
                v-model="form.center[0]"
                size="small"
                class="input-number-box-perc"
                controls-position="right"
                :min="0"
                :max="100"
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
                v-model="form.center[1]"
                size="small"
                class="input-number-box-perc"
                controls-position="right"
                :min="0"
                :max="100"
                @change="$emit('chartChange')"
              />
              <span class="extra-bottom-text"> 垂直</span>
            </div>
          </el-col>
        </el-row>
      </el-form-item>
      <CollapseItem
        v-model="form.axisName.show"
        name="轴名称"
        @change="$emit('chartChange')"
      >
        <el-form-item label="距离">
          <el-input-number
            v-model="form.nameGap"
            size="small"
            class="input-number-box-px"
            controls-position="right"
            @change="$emit('chartChange')"
          />
        </el-form-item>
        <el-form-item label="文本样式">
          <text-style
            ref="axisNameForm"
            @text-style-change="textStyleChange($event,'axisName')"
          />
        </el-form-item>
      </CollapseItem>

      <el-collapse>
        <CollapseItem
          v-model="form.axisLabel.show"
          name="轴标签"
          @change="$emit('chartChange')"
        >
          <el-form-item label="文本">
            <text-style
              ref="axisLabelForm"
              @text-style-change="textStyleChange($event,'axisLabel')"
            />
          </el-form-item>
          <el-form-item label="轴标签">
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
                      :class="{'active':alignActive===ele.value,'is-disabled':!saveAble}"
                      @click="alignChange(ele.value)"
                    >
                      <i :class="['icon am-iconfont align-icon', ele.icon]" />
                    </div>
                  </div>
                  <span class="extra-bottom-text"> 角度</span>
                </div>
              </el-col>
              <el-col
                :span="12"
              >
                <div class="d-flex-c">
                  <el-input-number
                    v-model="alignActive"
                    size="small"
                    class="input-number-box-angle"
                    controls-position="right"
                    @change="alignChange"
                  />
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
                  <el-input-number
                    v-model="form.axisLabel.margin"
                    class="input-number-box-px"
                    size="small"
                    controls-position="right"
                    @change="$emit('chartChange')"
                  />
                  <span class="extra-bottom-text"> 偏移量</span>
                </div>
              </el-col>
            </el-row>
          </el-form-item>
        </CollapseItem>
        <CollapseItem
          v-model="form.axisLine.show"
          name="轴线"
          @change="$emit('chartChange')"
        >
          <el-form-item label="样式">
            <lineStyle
              ref="axisLineForm"
              @line-style-change="lineStyleChange($event,'axisLine')"
            />
          </el-form-item>
        </CollapseItem>
        <CollapseItem
          v-model="form.splitLine.show"
          name="网格线"
          @change="$emit('chartChange')"
        >
          <el-form-item label="样式">
            <lineStyle
              ref="splitLineForm"
              @line-style-change="lineStyleChange($event,'splitLine')"
            />
          </el-form-item>
        </CollapseItem>
        <CollapseItem
          v-model="form.axisTick.show"
          name="刻度线"
          @change="$emit('chartChange')"
        >
          <el-form-item label="长度">
            <el-input-number
              v-model="form.axisTick.length"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="$emit('chartChange')"
            />
          </el-form-item>
          <el-form-item label="样式">
            <lineStyle
              ref="axisTickForm"
              @line-style-change="lineStyleChange($event,'axisTick')"
            />
          </el-form-item>
        </CollapseItem>
        <CollapseItem
          v-model="form.splitArea.show"
          name="分隔颜色"
          @change="$emit('chartChange')"
        >
          <el-form-item label="颜色">
            <el-row
              :gutter="10"
              type="flex"
            >
              <el-col
                :span="12"
              >
                <div class="d-flex-c">
                  <CommonColorPicker
                    v-model="form.splitArea.areaStyle.color[0]"
                    @change="$emit('chartChange')"
                  />
                </div>
              </el-col>
              <el-col
                :span="12"
              >
                <div class="d-flex-c">
                  <CommonColorPicker
                    v-model="form.splitArea.areaStyle.color[1]"
                    @change="$emit('chartChange')"
                  />
                </div>
              </el-col>
            </el-row>
          </el-form-item>
        </CollapseItem>
      </el-collapse>
    </el-form>
  </div>
</template>
<script>

export default {

  inject: ['getSaveAble'],
  emits: ['chartChange'],
  data () {
    return {
      form: {
        center: [50, 50],
        radius: 75,
        axisLabel: { show: false, margin: 8, rotate: 0, dataType: '' },
        axisName: {},
        nameGap: 15,
        axisLine: { show: false },
        axisTick: { show: false, inside: 0, length: 5 },
        splitLine: { show: false },
        splitArea: {
          show: false,
          areaStyle: {
            color: ['rgba(250,250,250,0.3)', 'rgba(200,200,200,0.3)']
          }
        }
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
    setFormData (form) {
      Object.assign(this.$data, this.$options.data())

      Object.keys(form).forEach(key => {
        if (form[key]) {
          this.form[key] = Object.assign(this.form[key] || {}, form[key])
        }
      })

      Object.keys(this.form).forEach(name => {
        if (['axisLabel', 'axisLine', 'splitLine', 'axisName', 'axisTick'].includes(name)) {
          if (this.$refs[name + 'Form'].type === 'text') {
            this.$refs[name + 'Form'].setFormData(form[name] || {})
          }
          if (this.$refs[name + 'Form'].type === 'line') {
            this.$refs[name + 'Form'].setFormData(form[name]?.lineStyle || {})
          }
        }
      }
      )
      this.alignActive = this.form.axisLabel.rotate ? this.form.axisLabel.rotate : 0
    },
    lineStyleChange (lineStyle, type) {
      this.form[type].lineStyle = { ...lineStyle }
      this.$emit('chartChange')
    },
    textStyleChange (textStyle, type) {
      this.form[type] = { ...this.form[type], ...textStyle }
      this.$emit('chartChange')
    },
    typeChange () {
      this.form.axisLabel.dataType = ''
      this.$emit('chartChange')
    },
    saveFormData () {
      const formData = { }
      Object.keys(this.form).forEach(name => {
        if (['axisLabel', 'axisLine', 'splitLine', 'axisName', 'axisTick'].includes(name)) {
          formData[name] = { ...this.form[name] }
          if (this.form[name].show) {
            if (this.$refs[name + 'Form'].type === 'text') {
              formData[name] = { ...formData[name], ...this.$refs[name + 'Form'].getFormData() }
            }
            if (this.$refs[name + 'Form'].type === 'line') {
              formData[name].lineStyle = this.$refs[name + 'Form'].getFormData()
            }
          }
        } else {
          formData[name] = this.form[name]
        }
      })
      return formData
    },
    alignChange (e) {
      this.alignActive = e
      this.form.axisLabel.rotate = Number(e)
      this.$emit('chartChange')
    }
  }
}
</script>

<style lang="scss" scoped>
@import "../../../styles/index.css";

// ::v-deep.el-collapse {
//   .el-collapse-item__header {
//     // height: auto;
//   }
// }
:deep(.el-tabs) {

  .el-tabs__item {
    padding: 10px;
  }
}

.d-flex-c {
  display: flex;
  flex-direction: column;
}

.d-flex {
  display: flex;
}
</style>
