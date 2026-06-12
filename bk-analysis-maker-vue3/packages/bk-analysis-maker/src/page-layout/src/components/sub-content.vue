<template>
  <EfFlexBox
    v-if="configs.children && configs.children.length"
    gap="20"
    v-bind="{...configs,children:undefined}"
  >
    <template v-for="r in configs.children">
      <SubContent
        v-if="r.children && r.children.length"
        :key="r.id"
        :ref="'chart-' + r.id"
        :configs="r"
        :set-mode="setMode"
      />
      <ChartContent
        v-else
        :key="r.id"
        :ref="'chart-' + r.id"
        :configs="r"
        :set-mode="setMode"
      />
    </template>
  </EfFlexBox>
  <ChartContent
    v-else
    :key="configs.id"
    :ref="'chart-' + configs.id"
    :configs="configs"
    :set-mode="setMode"
  />
</template>

<script>
import ChartContent from './chart-content.vue'
export default {
  name: 'SubContent',
  components: {
    ChartContent
  },
  props: {
    configs: {
      type: Object,
      default: () => {}
    },
    setMode: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {

    }
  },
  created () {

  },
  methods: {

  }
}
</script>
