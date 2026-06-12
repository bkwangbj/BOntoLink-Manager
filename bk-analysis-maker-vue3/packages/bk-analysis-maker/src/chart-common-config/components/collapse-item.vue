<template>
  <el-collapse v-model="show">
    <el-collapse-item
      name="1"
      :disabled="!value"
    >
      <template #title>
        <div class="d-flex ai-c collapse-item-title">
          <el-switch
            v-model="value"
            :disabled="!saveAble"
            class="am-switch active-switch"
            size="small"
            @change="change"
            @click.stop.prevent
          />
          <span class="chart-config-title">
            {{ name }}</span> <slot
            name="titleRight"
            :show="show"
          />
        </div>
      </template>
      <div class="plane-inner-bg">
        <slot />
      </div>
    </el-collapse-item>
  </el-collapse>
</template>

<script>
export default {
  name: 'CollapseItem',
  inject: ['getSaveAble'],
  props: {
    name: {
      type: String,
      default: ''
    },
    isExpand: {
      type: Boolean,
      default: false
    },
    modelValue: {
      type: Boolean,
      default: false
    }
  },
  emits: ['change', 'update:modelValue'],
  data () {
    return {
      disable: true,
      show: [],
      value: false
    }
  },
  computed: {
    saveAble () {
      return this.getSaveAble()
    }
  },
  watch: {
    modelValue: {
      handler (nv, ov) {
        this.value = nv
        if (!nv) {
          this.show = []
        }
      },
      immediate: true
    },
    isExpand: {
      handler () {
        if (this.isExpand) {
          this.show = ['1']
        }
      },
      immediate: true
    }
  },
  methods: {
    change (e) {
      this.$emit('update:modelValue', e)
      this.$emit('change', e)
      if (!e) {
        this.show = []
      }
    },
    itemChange (e) {

    }
  }
}
</script>
