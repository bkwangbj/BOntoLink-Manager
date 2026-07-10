<template>
  <el-collapse
    v-model="show"
    class="bk-cfg-collapse"
  >
    <el-collapse-item
      name="1"
    >
      <template #title>
        <div
          class="d-flex ai-c collapse-item-title"
          style="flex: 1; min-width: 0;"
        >
          <span class="chart-config-title">
            {{ name }}</span> <slot
            name="titleRight"
            :show="show"
          />
          <el-switch
            v-model="value"
            :disabled="!saveAble"
            class="am-switch active-switch"
            size="small"
            style="margin-left: auto;"
            @change="change"
            @click.stop.prevent
          />
          <span
            class="cfg-enable-text"
            :class="{ 'is-on': value }"
            @click.stop.prevent="toggleEnable"
          >启用</span>
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
      // 开启开关时自动展开配置(关闭不强制收起,可手动展开查看)
      this.show = e ? ['1'] : []
    },
    itemChange (e) {

    },
    // 点「启用」文字等同于切换开关
    toggleEnable () {
      if (!this.saveAble) return
      this.value = !this.value
      this.change(this.value)
    }
  }
}
</script>
<style scoped>
/* 开关后「启用」文字:关时浅灰、开时黑灰,与开关状态联动 */
.cfg-enable-text {
  flex: 0 0 auto;
  margin: 0 24px 0 0;
  font-size: 12px;
  line-height: 1;
  color: #c0c4cc;
  cursor: pointer;
  user-select: none;
  transition: color .15s;
}
.cfg-enable-text.is-on {
  color: #4e5969;
}
</style>
