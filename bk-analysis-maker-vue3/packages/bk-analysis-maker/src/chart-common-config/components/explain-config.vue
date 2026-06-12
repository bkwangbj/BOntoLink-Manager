<template>
  <CollapseItem
    v-model="form.show"
    name="说明"
    @change="$emit('chartChange')"
  >
    <div class="explain-config">
      <el-form
        size="small"
        label-width="80px"
        :disabled="!form.show||!saveAble"
        class="inline-col-form explain-form"
      >
        <el-form-item label="内容">
          <BKCodeCom
            ref="editor"
            v-model="form.text"
            style="width: 100%;height: 80px;"
            language="html"
            :readonly="!saveAble"
            @blur="$emit('chartChange')"
          />
        </el-form-item>
        <el-form-item label="显示区域">
          <EfSelect
            v-model="form.showArea"
            :mock="areaOptions"
            :clearable="false"
            @change="$emit('chartChange')"
          />
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
        <el-form-item
          label="文本样式"
        >
          <text-style
            ref="textRef"
            config-type="html"
            @text-style-change="textStyleChange"
          />
        </el-form-item>
      </el-form>
    </div>
  </CollapseItem>
</template>

<script>

import TextStyle from '../../chart-common-config/components/style-config/text-style.vue'
export default {

  name: 'ExplainConfig',
  components: {
    TextStyle
  },
  inject: ['getSaveAble'],
  emits: ['chartChange'],
  data () {
    return {
      form: {
        show: false,
        text: '',
        position: 'topLeft',
        x: 0,
        y: 0,
        showArea: 'component'
      },
      editorCfg: {
        minimap: { enabled: false }, theme: 'vs-dark', overviewRulerBorder: false
      },
      areaOptions: [
        { label: '组件', value: 'component' },
        { label: '卡片', value: 'card' }
      ],
      alignOption: [{ label: '顶部居左', value: 'topLeft', icon: 'icon-tuli-dingbujuzuo' }, { label: '顶部居中', value: 'topCenter', icon: 'icon-tuli-dingbujuzhong' }, { label: '顶部居右', value: 'topRight', icon: 'icon-tuli-dingbujuyou' },
        { label: '底部居左', value: 'bottomLeft', icon: 'icon-tuli-dibujuzuo' }, { label: '底部居中', value: 'bottomCenter', icon: 'icon-a-tuli-dibujuzhong-' }, { label: '底部居右', value: 'bottomRight', icon: 'icon-tuli-dibujuyou' }]
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  mounted () {
  },
  methods: {
    setFormData (form) {
      this.form = { ...this.$options.data().form, ...form }
      this.$refs.textRef.setFormData(this.form?.textStyle || {})
    },
    textStyleChange (form) {
      this.form.textStyle = { ...form }
      this.$emit('chartChange')
    },
    alignChange (ele) {
      this.form.position = ele.value
      this.$emit('chartChange')
      this.$nextTick(() => {})
    },

    saveFormData () {
      return { ...this.form }
    }
  }
}
</script>
<style lang="scss" scoped>
.explain-form {

  >:deep(.el-form-item) {
    margin-bottom: 10px !important;
  }
}
</style>
