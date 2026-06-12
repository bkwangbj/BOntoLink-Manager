<template>
  <div style="width: 100%;">
    <el-form
      label-width="80px"
      size="small"
      :disabled="!saveAble"
    >
      <el-form-item label="内外径比">
        <el-row
          :gutter="10"
          type="flex"
        >
          <el-col
            :span="24"
          >
            <div class="d-flex-c">
              <el-input-number
                v-model="internalDiameterRatio"
                size="small"
                :precision="2"
                controls-position="right"
                :step="0.1"
                class="input-number-box"
                :max="1"
                :min="0"
                @change="$emit('chartChange')"
              />
            </div>
          </el-col>
        </el-row>
      </el-form-item>
      <el-form-item label="视角距离">
        <el-row
          :gutter="10"
          type="flex"
        >
          <el-col
            :span="24"
          >
            <div class="d-flex-c">
              <el-input-number
                v-model="viewControl.distance"
                size="small"

                :precision="2"
                controls-position="right"
                class="input-number-box"
                @change="$emit('chartChange')"
              />
            </div>
          </el-col>
        </el-row>
      </el-form-item>
      <el-form-item label="视角">
        <el-row
          :gutter="10"
          type="flex"
        >
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <el-input-number
                v-model="viewControl.beta"
                size="small"
                :precision="2"
                controls-position="right"
                :max="90"
                :min="-90"

                class="input-number-box"
                @change="$emit('chartChange')"
              />
              <span class="extra-bottom-text"> 水平旋转角度</span>
            </div>
          </el-col>
          <el-col
            :span="12"
          >
            <div class="d-flex-c">
              <el-input-number
                v-model="viewControl.alpha"
                size="small"
                :precision="2"
                controls-position="right"

                class="input-number-box"
                :max="90"
                :min="-90"
                @change="$emit('chartChange')"
              />
              <span class="extra-bottom-text"> 垂直旋转角度</span>
            </div>
          </el-col>
        </el-row>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>

export default {

  inject: ['getSaveAble'],
  emits: ['chartChange'],
  data () {
    return {
      internalDiameterRatio: 0.50,
      viewControl: {
        distance: 180,
        alpha: 25,
        beta: 30
      }
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  methods: {
    setFormData ({ internalDiameterRatio, viewControl }) {
      Object.assign(this.$data, this.$options.data())
      this.internalDiameterRatio = internalDiameterRatio || 0.50
      if (viewControl) {
        this.viewControl = Object.assign(this.viewControl, viewControl)
      }
    },
    saveFormData () {
      return { internalDiameterRatio: this.internalDiameterRatio, viewControl: this.viewControl }
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
