<template>
  <EfModal
    v-model="modalVisible"
    :width="isApp ? '400px' :'70%'"
    height="70%"
    :title="configs.openPage.name"
    @close="$emit('previewClose')"
  >
    <template #title>
      {{ configs.openPage.name }}
      (<el-button
        size="small"
        text
        title="下载图片"
        @click="exportImage"
      >
        <el-icon><Download /></el-icon>
      </el-button>)
    </template>
    <AnalysisMaker
      ref="analysisMaker"
      v-bind="$attrs"
      :set-mode="false"
      :page-config="configs.config"
      class="analysis-page"
      :is-modal="true"
      :custom-chart="customChart"
      :map-source="mapSource"
      :tjb-u-r-l="tjbURL"
      :map-path="mapPath"
      :is-basic-mode="isBasicMode"
      :pre-theme-list="preThemeList"
      :default-params="configs.varConfig.filter(c => c.changeType !== 'openModal')"
    />
  </EfModal>
</template>
<script>
import html2canvas from 'html2canvas'
import { ElMessage } from 'element-plus'
export default {
  name: 'OpenPageModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    configs: {
      type: Object,
      default: () => { return { config: {}, openPage: {}, varConfig: [] } }
    },
    customChart: {
      type: Array,
      default: () => []
    },
    mapSource: {
      type: Array,
      default: () => []
    },
    mapPath: {
      type: String,
      default: '/map/'
    },
    isBasicMode: {
      type: Boolean,
      default: false
    },
    tjbURL: {
      type: String,
      default: ''
    },
    preThemeList: {
      type: Array,
      default: () => []
    },
    isApp: {
      type: Boolean,
      default: false
    }
  },
  emits: ['previewClose'],
  data () {
    return {
      modalVisible: false
    }
  },
  computed: {

  },
  watch: {
    visible:
    {
      handler () {
        this.$nextTick(() => {
          this.modalVisible = this.visible
          if (this.modalVisible) {
            this.$nextTick(() => {
              this.init()
            })
          }
        })
      },
      immediate: true
    }
  },
  mounted () {

  },
  methods: {
    init () {
      if (this.configs.varConfig) {
        // this.$refs.analysisMaker.setParams(this.configs.varConfig.filter(c => c.changeType !== 'openModal'))
      }
    },
    async exportImage () {
      try {
        // 等待 DOM 更新完成
        await this.$nextTick()

        // 获取 AnalysisMaker 组件的根元素
        const element = this.$refs.analysisMaker.$el

        // 使用 html2canvas 进行截图
        const canvas = await html2canvas(element, {
          useCORS: true, // 允许跨域图片
          allowTaint: true, // 允许跨域图片污染画布
          backgroundColor: '#ffffff' // 设置背景色
        })

        // 将 canvas 转换为图片并下载
        const imageLink = document.createElement('a')
        const imageUrl = canvas.toDataURL('image/png')

        imageLink.href = imageUrl
        imageLink.download = `${this.configs.openPage.name || '预览'}.png`
        document.body.appendChild(imageLink)
        imageLink.click()
        document.body.removeChild(imageLink)
        window.URL.revokeObjectURL(imageUrl)
      } catch (error) {
        console.error('导出图片失败:', error)
        ElMessage.error('导出图片失败')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.analysis-page {
  width: 100%;
  height: 100%;
}
</style>
