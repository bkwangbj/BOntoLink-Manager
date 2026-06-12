<template>
  <div class="yAix-cofig">
    <el-form
      :ref="'form-yAix'"
      size="small"
      :model="basic"
      :disabled="!saveAble"
      label-width="80px"
    >
      <el-form-item label="y轴可见">
        <el-switch
          v-model="basic.show"
          class="am-switch active-switch"
          size="small"
          @change="$emit('axisChange')"
        />
      </el-form-item>
      <el-form-item label="位置">
        <el-radio-group
          v-model="basic.position"
          size="small"
          @change="$emit('axisChange')"
        >
          <el-radio-button value="left">
            左
          </el-radio-button>
          <el-radio-button value="right">
            右
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="轴单位">
        <el-input
          v-model="basic.name"
          size="small"
          @change="$emit('axisChange')"
        />
      </el-form-item>
      <el-collapse>
        <el-collapse-item>
          <template #title>
            <div>
              <span
                class="collapse-title-common "
              >
                轴单位样式 </span>
            </div>
          </template>
          <div
            class="plane-inner-bg"
            style="margin-bottom: 10px;"
          >
            <el-form-item label="偏移量">
              <el-input-number
                v-model="basic.nameGap"
                size="small"
                class="input-number-box-px"
                controls-position="right"
                @change="$emit('axisChange')"
              />
            </el-form-item>
            <el-form-item label="位置">
              <el-radio-group
                v-model="basic.nameLocation"
                size="small"
                @change="$emit('axisChange')"
              >
                <el-radio-button value="start">
                  起始
                </el-radio-button>
                <el-radio-button value="middle">
                  中部
                </el-radio-button>
                <el-radio-button value="end">
                  末尾
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="文本样式">
              <text-style
                ref="nameTextStyleForm"
                @text-style-change="textStyleChange($event,'nameTextStyle')"
              />
            </el-form-item>
          </div>
        </el-collapse-item>
      </el-collapse>
      <el-form-item label="数据类型">
        <el-radio-group
          v-model="basic.type"
          size="small"
          @change="typeChange"
        >
          <el-radio-button value="category">
            类目轴
          </el-radio-button>
          <el-radio-button value="value">
            数值轴
          </el-radio-button>
          <el-radio-button value="time">
            时间轴
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item
        v-show="basic.type==='value'"
        label="数据缩放"
      >
        <el-switch
          v-model="basic.scale"
          class="am-switch active-switch"
          size="small"
          @change="$emit('axisChange')"
        />
      </el-form-item>
      <el-collapse>
        <CollapseItem
          v-model="form.axisLabel.show"
          name="轴标签"
          @change="$emit('axisChange')"
        >
          <DataFormatSelect
            v-show="basic.type==='value'||basic.type==='time'"
            v-model="form.axisLabel.dataType"
            :type="basic.type"
            @data-type-change="$emit('axisChange')"
          />
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
                    @change="$emit('axisChange')"
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
          @change="$emit('axisChange')"
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
          @change="$emit('axisChange')"
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
          @change="$emit('axisChange')"
        >
          <el-form-item label="长度">
            <el-input-number
              v-model="form.axisTick.length"
              size="small"
              class="input-number-box-px"
              controls-position="right"
              @change="$emit('axisChange')"
            />
          </el-form-item>
          <el-form-item label="朝向">
            <el-radio-group
              v-model="form.axisTick.inside"
              size="small"
              @change="$emit('axisChange')"
            >
              <el-radio-button :value="0">
                朝外
              </el-radio-button>
              <el-radio-button :value="1">
                朝内
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="样式">
            <lineStyle
              ref="axisTickForm"
              @line-style-change="lineStyleChange($event,'axisTick')"
            />
          </el-form-item>
        </CollapseItem>
      </el-collapse>
    </el-form>
  </div>
</template>
<script>
import lineStyle from '../style-config/line-style.vue'
import TextStyle from '../style-config/text-style.vue'
export default {
  components: {
    lineStyle,
    TextStyle
  },
  inject: ['getSaveAble'],
  emits: ['axisChange'],
  data () {
    return {
      form: {
        axisLabel: { show: false, margin: 8, rotate: 0, dataType: '' },
        axisLine: { show: false },
        splitLine: { show: false },
        axisTick: { show: false, inside: 0, length: 5 },
        nameTextStyle: {}
      },
      basic: { show: false, name: '', type: 'value', alignTicks: true, nameGap: 15, nameLocation: 'end', scale: false, position: 'left' },
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

      Object.keys(this.form).forEach(key => {
        if (form[key]) {
          this.form[key] = Object.assign(this.form[key], form[key])
        }
      })
      Object.keys(this.basic).forEach(key => {
        if (form[key]) {
          this.basic[key] = form[key]
        }
      })
      Object.keys(this.form).forEach(name => {
        if (['axisLabel', 'axisLine', 'splitLine', 'nameTextStyle', 'axisTick'].includes(name)) {
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
      this.$emit('axisChange')
    },
    textStyleChange (textStyle, type) {
      this.form[type] = { ...this.form[type], ...textStyle }
      this.$emit('axisChange')
    },
    saveFormData () {
      const formData = { ...this.basic }
      Object.keys(this.form).forEach(name => {
        if (['axisLabel', 'axisLine', 'splitLine', 'nameTextStyle', 'axisTick'].includes(name)) {
          formData[name] = { ...this.form[name] }
          if (this.form[name].show) {
            if (this.$refs[name + 'Form'].type === 'text') {
              formData[name] = { ...formData[name], ...this.$refs[name + 'Form'].getFormData() }
            }
            if (this.$refs[name + 'Form'].type === 'line') {
              formData[name].lineStyle = this.$refs[name + 'Form'].getFormData()
            }
          }
        }
      })
      return formData
    },
    typeChange () {
      this.form.axisLabel.dataType = ''
      this.$emit('axisChange')
    },
    alignChange (e) {
      this.alignActive = e
      this.form.axisLabel.rotate = Number(e)
      this.$emit('axisChange')
    }
  }
}
</script>

<style lang="scss" scoped>
@import "../../../styles/index.css";

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
