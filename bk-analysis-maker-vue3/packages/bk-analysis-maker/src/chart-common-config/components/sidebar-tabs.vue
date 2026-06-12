<template>
  <div class="chart-sidebar">
    <div
      v-for="m in menu"
      :key="m.key"
      :class="['menu-item', activeMenu === m.key ? 'active' : '']"
      @click="clickTabItem(m)"
    >
      <i
        :class="['icon am-iconfont sidebar-icon', m.icon]"
      />
      <span>{{ m.name }}</span>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    menu: {
      type: Array,
      default: () => []
    },
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['change', 'update:modelValue'],
  data () {
    return {
      activeMenu: ''
    }
  },
  watch: {
    modelValue: {
      handler (nv, ov) {
        this.activeMenu = nv
      },
      immediate: true
    }
  },
  methods: {
    clickTabItem (m) {
      this.$emit('update:modelValue', m.key)
      this.$emit('change', m.key)
    }
  }

}
</script>
