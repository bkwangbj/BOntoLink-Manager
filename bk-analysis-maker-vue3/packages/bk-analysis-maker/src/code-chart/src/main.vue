<template>
  <div
    ref="rootRef"
    class="code-panel"
    style="position: relative;"
  >
    <slot name="content">
      <component
        :is="contentConfig.component"
        v-if="contentConfig && contentConfig.component"
        v-bind="contentConfig.props || {}"
      />
    </slot>
    <BKCodeCom
      v-show="!isModal && !isPreview && !isBasicMode"
      ref="editor"
      v-model="configs.code"
      class="code-content"
      language="html"
      :readonly="!setMode"
    />
    <div
      v-show="isPreview || !setMode"
      ref="container"
      class="code-content"
    />
  </div>
</template>

<script>
import { mixins } from '../../configs/commom-chart'
import { createApp } from 'vue'
import ejs from 'ejs'
import ElementPlus from 'element-plus'
export default {
  name: 'CodeChart',
  components: {

  },
  mixins: [mixins],
  inheritAttrs: false,
  props: {
    isPreview: {
      type: Boolean,
      default: false
    },
    isModal: {
      type: Boolean,
      default: false
    },
    isBasicMode: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      code: '',
      editorCfg: {
        minimap: { enabled: false }, theme: 'vs-dark', overviewRulerBorder: false
      },
      defaultData: {},
      htmlCode: ''
    }
  },
  computed: {

  },
  watch: {
    relList () {
      this.debouncedCustomResetChart(this.configs)
    },
    isPreview () {
      this.debouncedCustomResetChart(this.configs)
    }
  },
  mounted () {
  },
  created () {
  },
  methods: {
    async customResetChart (config) {
      if (!this.configs.code) {
        this.configs.code = '<p>名称：<%= data.name %></p><p>数量：<%= data.num %></p>'
      }
      this.code = this.configs.code || '<p>名称：<%= data.name %></p><p>数量：<%= data.num %></p>'
      await this.$nextTick()
      // if (!this.isModal && !this.isBasicMode) {
      //   this.$refs.editor && this.$refs.editor.init(this.code, this.editorCfg)
      // }
      try {
        this.htmlCode = ejs.render(this.code, { data: this.relList })
        this.htmlCode = this.unescapeHTML(this.htmlCode)
      } catch (error) {
        if (this.isModal || this.isBasicMode) {
          this.htmlCode = ''
        } else {
          this.htmlCode = "<div style='color: #ff2121;padding: 5px;'>模版解析出错，请检查配置！</div>"
        }
      }
      if (this.isPreview || !this.setMode) {
        this.renderContent()
      }
    },
    renderContent () {
      const container = this.$refs.container
      if (!container) {
        return
      }
      if (this.component && this.component.unmount) {
        this.component.unmount()
      }
      const $this = this
      container.innerHTML = ''
      const component = createApp({
        components: {},
        data: function () {
          return {
          }
        },
        methods: {
          clickItem (data, item) {
            $this.handleEvent(data || $this.relList, 'click', item)
          },
          windowMethod (methodName) {
            const params = Array.from(arguments).slice(1)
            window[methodName](...params)
          }
        },
        template: '<div style="width:100%;height:100%;">' + this.htmlCode + '</div>'
      })
      component.use(ElementPlus)
      component.mount(container)
      this.component = component
      // container.appendChild(component.$el)
    },
    unescapeHTML (str) {
      // var parser = new DOMParser()
      // var dom = parser.parseFromString(str, 'text/html')
      // return dom.documentElement.textContent
      let s = ''
      if (str.length === 0) return ''
      s = str.replace(/&amp;/g, '&')
      s = s.replace(/&lt;/g, '<')
      s = s.replace(/&gt;/g, '>')
      s = s.replace(/&nbsp;/g, ' ')
      /* eslint no-useless-escape: "error" */
      s = s.replace(/&#39;/g, '\\')
      s = s.replace(/&quot;/g, '"')
      s = s.replace(/<br\/>/g, '\n')
      return s
    }

  }
}
</script>

<style lang="scss" scoped>
.code-panel {
  position: relative;
  width: 100%;
  height: 100%;
}

.code-content {
  width: 100%;
  height: 100%;
}

</style>
