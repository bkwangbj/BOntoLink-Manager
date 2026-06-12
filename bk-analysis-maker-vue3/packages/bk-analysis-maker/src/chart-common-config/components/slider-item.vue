<template>
  <div class="full-width">
    <el-slider
      v-model="value1"
      @change="handleChange"
    />
    <el-input-number
      v-model="value1"
      controls-position="right"
      :step="0.05"
      :min="0"
      :max="1"
      @change="handleChange"
    />
  </div>
</template>

<script>
export default {
  props: {
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      value1: 0
    }
  },
  watch: {
    modelValue: {
      handler (nv, ov) {
        this.value1 = nv
      },
      immediate: true
    }
  }
}
</script>

<style>

</style>
