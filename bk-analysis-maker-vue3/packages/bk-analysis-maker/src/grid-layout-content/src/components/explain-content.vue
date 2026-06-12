<template>
  <div
    v-if="explainConfig.show"
    class="explain-cotent"
    :style="`--x: ${explainConfig.x};--y: ${explainConfig.y};${style}`"
  >
    <div
      ref="container"
      style="position: relative;"
      class="explain-text"
      :style="textStyle"
    />
  </div>
</template>

<script>
import { utils } from 'efficient-suite'
import ejs from 'ejs'
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
export default {
  name: 'ExplainContent',

  props: {
    configs: {
      type: Object,
      default: () => {}
    },
    relList: {
      type: [Array, Object],
      default: () => {}
    },
    showClear: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      position: '',
      textStyle: {},
      htmlCode: '',
      code: '',
      explainConfig: {
        show: false,
        text: '',
        position: 'topLeft',
        x: 0,
        y: 0

      }
    }
  },
  computed: {
    style () {
      let string = ''
      const alignList = {
        topLeft: 'left:10px;top:10px;',
        topCenter: 'left: 50%; transform: translateX(-50%);top:10px;',
        topRight: 'right:10px;top:10px;',
        bottomLeft: 'left:10px;bottom:10px;',
        bottomCenter: 'left: 50%; transform: translateX(-50%);bottom:10px;',
        bottomRight: 'right:10px;bottom:10px;'
      }

      if (this.explainConfig?.position) {
        string = alignList[this.explainConfig.position]
      }
      return string
    }
  },
  watch: {
    configs: {
      handler () {
        if (this.configs.explainConfig) {
          this.explainConfig = { ...this.configs.explainConfig }
          this.renderHtml()
          if (this.explainConfig.textStyle) {
            for (const key in this.explainConfig.textStyle) {
              this.textStyle['--' + key] = this.explainConfig.textStyle[key]
            }
          }
        }
      },
      immediate: true
    },
    relList: {
      handler () {
        this.renderHtml()
      },
      deep: true
    }
  },
  methods: {
    async renderHtml (config) {
      this.code = this.explainConfig.text || ''
      await this.$nextTick()
      try {
        this.htmlCode = ejs.render(this.code, { data: this.relList })
        this.htmlCode = this.unescapeHTML(this.htmlCode)
        this.renderContent()
      } catch (error) {
        this.htmlCode = "<div style='color: #ff2121;padding: 5px;'>模版解析出错，请检查配置！</div>"
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
      container.innerHTML = ''
      const component = createApp({
        components: {},
        data: function () {
          return {
            dayjs: utils
          }
        },
        methods: {
          windowMethod (methodName) {
            const params = Array.from(arguments).slice(1)
            window[methodName](...params)
          }
        },
        template: '<div>' + this.htmlCode + '</div>'
      })
      component.use(ElementPlus)
      component.mount(container)
      this.component = component
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
/* stylelint-disable custom-property-pattern */
.explain-cotent {
  position: absolute;
  z-index: 99;

  .explain-text {
    top: calc(var(--y) * 1px);
    left: calc(var(--x) * 1px);
    z-index: 99;
    font-family: var(--fontFamily);
    font-size: calc(var(--fontSize) * 1px);
    font-style: var(--fontStyle);
    font-weight: var(--fontWeight);
    color: var(--color);
    text-decoration: var(--textDecoration);
  }
}

</style>
