<template>
  <div style="width: 100%;">
    <el-form
      :disabled="!saveAble"
      :model="form"
      size="small"
      label-width="80px"
    >
      <el-form-item label="开启映射">
        <el-switch

          v-model="colorShow"
          :disabled="!saveAble"
          class="am-switch active-switch"
          size="small"
          @change="showChange"
        />
      </el-form-item>
      <el-form-item label="类型">
        <el-radio-group
          v-model="form.type"
          size="small"
          @change="$emit('chartChange')"
        >
          <el-radio-button value="continuous">
            连续型
          </el-radio-button>
          <el-radio-button value="piecewise">
            分段型
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item
        v-show="form.type==='continuous'"
        label="区间"
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
                v-model="continuousForm.min"
                controls-position="right"
                size="small"
                @change="$emit('chartChange')"
              />
              <div class="extra-bottom-text">
                最小值
              </div>
            </div>
          </el-col>
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <el-input-number
                v-model="continuousForm.max"
                controls-position="right"
                size="small"
                @change="$emit('chartChange')"
              />
              <div class="extra-bottom-text">
                最大值
              </div>
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
            <CommonColorPicker
              v-model="continuousForm.inRange.color[0]"
              @change="$emit('chartChange')"
            />
          </el-col>
          <el-col
            :span="12"
          >
            <CommonColorPicker
              v-model="continuousForm.inRange.color[1]"
              @change="$emit('chartChange')"
            />
          </el-col>
        </el-row>
      </el-form-item>

      <el-collapse
        v-show="form.type==='piecewise'"
        v-model="show"
      >
        <el-collapse-item
          name="1"
        >
          <template #title>
            <div>
              <span
                class="axis-item-title"
                style="display: inline-block;width: 90px;padding-left: 20px;text-align: left;"
              >
                区间  <el-button
                  v-show="show.length"
                  size="small"
                  text
                  class="add-seris-btn"
                  type="primary"
                  @click.stop="addPieces"
                ><el-icon><CirclePlus /></el-icon></el-button></span>
            </div>
          </template>
          <el-collapse>
            <el-collapse-item
              v-for="(ele,index) in piecewiseForm.pieces"
              :key="index"
            >
              <template #title>
                <div
                  class="series-header flex-between"
                  style="width: 100%;"
                >
                  <span style="display: inline-block;width: 90px;padding-left: 20px;text-align: left;">区间{{ index + 1 }}</span>
                  <div>
                    <el-button
                      size="small"
                      text
                      class="add-seris-btn"
                      type="primary"
                      @click.stop="copyPieces(ele)"
                    >
                      <i-ri-file-copy-2-fill />
                    </el-button>
                    <el-button
                      size="small"
                      text
                      class="del-seris-btn"
                      @click.stop="delPieces(index)"
                    >
                      <el-icon><DeleteFilled /></el-icon>
                    </el-button>
                  </div>
                </div>
              </template>
              <el-row
                :gutter="10"
                type="flex"
              >
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <el-input-number
                      v-model="ele.gt"
                      controls-position="right"
                      size="small"
                      @change="$emit('chartChange')"
                    />
                    <span class="extra-bottom-text">大于</span>
                  </div>
                </el-col>
                <el-col
                  :span="12"
                >
                  <div class="d-flex-c">
                    <el-input-number
                      v-model="ele.lte"
                      controls-position="right"
                      size="small"
                      @change="$emit('chartChange')"
                    />
                    <span class="extra-bottom-text">小于</span>
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
                    <CommonColorPicker
                      v-model="ele.color"
                      @change="$emit('chartChange')"
                    />
                    <span class="extra-bottom-text">区间颜色</span>
                  </div>
                </el-col>
              </el-row>
            </el-collapse-item>
          </el-collapse>
        </el-collapse-item>
      </el-collapse>
    </el-form>
  </div>
</template>

<script>
import { utils } from 'efficient-suite'
const colorList = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc']
export default {
  components: {

  },
  inject: ['getSaveAble'],
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    formData: {

      type: Object,
      default: () => {}
    }
  },
  emits: ['chartChange', 'showChange', 'update:modelValue'],
  data () {
    return {

      show: [],
      continuousForm: {
        max: 0,
        min: 0,
        inRange: { color: ['#24CFF4', '#1E62AC'] }

      },
      piecewiseForm: {
        pieces: []
      },
      form: {
        type: 'continuous'
      },
      colorShow: false,
      borderOptions: [{ label: '完整边框', value: ' full' }, { label: '外边框', value: 'outer' }, { label: '内边框', value: 'inner' }, { label: '无边框', value: 'none' }]
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  watch: {
    modelValue: {
      handler () {
        this.colorShow = this.modelValue
      },
      immediate: true
    },
    formData: {
      immediate: true,
      handler () {
        this.$nextTick(() => {
          if (this.formData) {
            if (JSON.stringify(this.formData !== '{}')) {
              this.setFormData(utils.deepClone(this.formData))
            }
          }
        })
      }
    }
  },
  methods: {
    setFormData (form) {
      Object.assign(this.$data, this.$options.data())
      this.colorShow = this.modelValue
      this.form.type = form.type
      this.continuousForm = Object.assign(this.continuousForm, form)
      this.piecewiseForm = Object.assign(this.piecewiseForm, form)
    },
    saveFormData () {
      const formData = { ...this[this.form.type + 'Form'], type: this.form.type }

      const form = utils.deepClone({ visualMap: formData })
      return form
    },
    showChange (v) {
      this.$emit('update:modelValue', v)
      this.$emit('showChange', v)
    },
    addPieces () {
      let index = this.piecewiseForm.pieces.length
      if (index >= colorList.length) {
        index = index % colorList.length
        if (index < 0) {
          index = 0
        }
      }
      this.piecewiseForm.pieces.push({ gt: null, lte: null, color: colorList[index] })
      this.$emit('chartChange')
    },
    copyPieces (series) {
      this.piecewiseForm.pieces.push({ ...utils.deepClone(series) })
      this.$emit('chartChange')
    },
    delPieces (index) {
      this.piecewiseForm.pieces.splice(index, 1)
      this.$emit('chartChange')
    },
    getGradientColor (startColor, endColor) {
      return {
        type: 'linear',
        x: 0,
        y: 0,
        x2: 0,
        y2: 1,
        colorStops: [
          { offset: 0, color: startColor },
          { offset: 1, color: endColor }
        ]
      }
    }
  }
}
</script>
<style  lang="scss" scoped>
.text-style-form > .el-form-item__content {
  margin-left: 0 !important;
}

.d-flex-c {
  display: flex;
  flex-direction: column;
}

.ai-c {
  align-items: center;
}

.d-flex {
  display: flex;
}

</style>
