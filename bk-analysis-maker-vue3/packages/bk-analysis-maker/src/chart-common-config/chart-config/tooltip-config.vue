<template>
  <div class="tooltip-config">
    <CollapseItem
      v-model="form.show"
      is-expand
      name=" 提示框"
      @change="$emit('chartChange')"
    >
      <el-form
        ref="form"
        size="small"
        :disabled="!saveAble"
        label-width="80px"
      >
        <el-form-item
          label="消失延迟"
        >
          <el-input-number
            v-model="form.hideDelay"
            controls-position="right"
            size="small"
            class="input-number-box-ms"
            @change="$emit('chartChange')"
          />
        </el-form-item>
        <el-form-item
          label="触发方式"
        >
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
                    v-for="ele in triggerOption"
                    :key="ele.value"
                    class="chart-align-item trigger-btn"
                    :class="{'active':form.trigger===ele.value,'is-disabled':!saveAble}"
                    @click="triggerChange(ele)"
                  >
                    {{ ele.label }}
                  </div>
                </div>
                <span class="extra-bottom-text"> 触发类型</span>
              </div>
            </el-col>
            <el-col
              :span="12"
            >
              <div class="d-flex-c">
                <div class="d-flex">
                  <div
                    v-for="el in triggerOnOption"

                    :key="el.value"
                    class="chart-align-item trigger-btn"
                    :class="{'active':form.triggerOn===el.value,'is-disabled':!saveAble}"
                    @click="triggerOnChange(el)"
                  >
                    {{ el.label }}
                  </div>
                </div>
                <span class="extra-bottom-text"> 触发动作</span>
              </div>
            </el-col>
          </el-row>
        </el-form-item>
        <el-form-item
          label="文本样式"
        >
          <TextStyle
            ref="textStyleFrom"
            @text-style-change="textStyleChange"
          />
        </el-form-item>

        <el-collapse>
          <el-collapse-item>
            <template #title>
              <div>
                <span
                  class="collapse-title-common "
                >
                  背景框样式 </span>
              </div>
            </template>

            <el-form-item
              label="背景色"
            >
              <CommonColorPicker
                v-model="form.backgroundColor"
                show-alpha
                @change="$emit('chartChange')"
              />
            </el-form-item>
            <el-form-item
              label="内边距"
            >
              <el-input-number
                v-model="form.padding"
                size="small"
                class="input-number-box-px"
                controls-position="right"
                @change="$emit('chartChange')"
              />
            </el-form-item>
            <el-form-item
              label="边框"
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
                      v-model="form.borderWidth"
                      size="small"
                      class="input-number-box-px"
                      controls-position="right"
                      @change="$emit('chartChange')"
                    />
                    <span class="extra-bottom-text"> 边框粗细</span>
                  </div>
                </el-col>
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <CommonColorPicker
                      v-model="form.borderColor"
                      @change="$emit('chartChange')"
                    />
                    <span class="extra-bottom-text"> 边框颜色</span>
                  </div>
                </el-col>
              </el-row>
            </el-form-item>
          </el-collapse-item>
        </el-collapse>
      </el-form>
    </CollapseItem>
    <CollapseItem
      v-model="customConfig.show"
      name=" 个性化配置"
      @change="$emit('chartChange')"
    >
      <InterfaceFilterConfig
        :interface-filter="customConfig.config"
        :set-mode="saveAble"
        method-name=" setCustomConfig(option, config, data, params) "
        return-text="return option;"
        @save="saveCustomConfig"
        @blur="configBlur"
      />
    </CollapseItem>
  </div>
</template>

<script>
import TextStyle from '../components/style-config/text-style.vue'
import { utils } from 'efficient-suite'
import InterfaceFilterConfig from '../../data-source-config/src/components/interface-filter-config.vue'
export default {
  name: 'TooltipConfig',
  components: {
    TextStyle, InterfaceFilterConfig
  },
  inject: ['getSaveAble'],
  emits: ['chartChange'],
  data () {
    return {
      customConfig: {
        show: false,
        config: 'return option'
      },
      form: {
        show: true,
        hideDelay: 100,
        backgroundColor: '#fff',
        trigger: 'item',
        triggerOn: 'mousemove',
        padding: 5,
        borderWidth: 0,
        borderColor: '#000'
      },
      triggerOnOption: [{ label: '悬浮', value: 'mousemove', icon: '' }, { label: '点击', value: 'click', icon: '' }],
      triggerOption: [{ label: '数据项', value: 'item', icon: '' }, { label: '坐标轴', value: 'axis', icon: '' }],
      alignOption: [{ label: '顶部居左', value: 'topLeft', icon: 'icon-tuli-dingbujuzuo' }, { label: '顶部居中', value: 'topCenter', icon: 'icon-tuli-dingbujuzhong' }, { label: '顶部居右', value: 'topRight', icon: 'icon-tuli-dingbujuyou' },
        { label: '底部居左', value: 'bottomLeft', icon: 'icon-tuli-dibujuzuo' }, { label: '底部居中', value: 'bottomCenter', icon: 'icon-a-tuli-dibujuzhong-' }, { label: '底部居右', value: 'bottomRight', icon: 'icon-tuli-dibujuyou' }]
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  methods: {
    saveFormData () {
      const form = utils.deepClone(this.form)
      const customConfig = utils.deepClone(this.customConfig)
      form.textStyle = this.$refs.textStyleFrom.getFormData()
      return { form, customConfig }
    },
    setFormData (form, customConfig) {
      this.form = { ...this.$options.data().form, ...form }
      this.$refs.textStyleFrom.setFormData(form?.textStyle)
      this.customConfig = { ...this.$options.data().customConfig, ...customConfig }
    },
    textStyleChange (textStyle) {
      this.$emit('chartChange')
    },
    triggerOnChange (ele) {
      this.form.triggerOn = ele.value
      this.$emit('chartChange')
    },
    triggerChange (ele) {
      this.form.trigger = ele.value
      this.$emit('chartChange')
    },
    saveCustomConfig (script) {
      this.customConfig.config = script
    },
    configBlur () {
      this.$emit('chartChange')
    }
  }
}
</script>

<style lang="scss" scoped>
.d-flex {
  display: flex;
}

/* 触发类型/触发动作:文字按钮 —— 自适应宽度不折行、选中蓝底用白字 */
.chart-align-item.trigger-btn {
  width: auto;
  min-width: 29px;
  height: 26px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  line-height: 1;
}
.chart-align-item.trigger-btn.active {
  color: #fff;
}
</style>
