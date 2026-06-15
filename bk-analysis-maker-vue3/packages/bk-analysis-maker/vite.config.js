import { resolve } from 'path'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { visualizer } from 'rollup-plugin-visualizer'
import ElementPlus from 'unplugin-element-plus/vite'
import vueJsx from '@vitejs/plugin-vue-jsx'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import Icons from 'unplugin-icons/vite'
import IconsResolver from 'unplugin-icons/resolver'
export default defineConfig({
  plugins: [
    vue(),
    vueJsx(),
    ElementPlus({}),
    AutoImport({
      // 自动导入 Vue 相关函数，如：ref, reactive, toRef 等
      imports: ['vue'],
      // 自动导入 Element Plus 相关函数，如：ElMessage, ElMessageBox... (带样式)
      resolvers: [
        // 自动导入图标组件
        IconsResolver({
          prefix: 'Icon'
        })
      ],
      eslintrc: {
        enabled: false
      }
    }),
    Components({
      resolvers: [
        // https://github.com/antfu/unplugin-icons
        IconsResolver()
      ]
    }),
    // https://github.com/antfu/unplugin-icons
    Icons({
      autoInstall: true
    })
  ],
  build: {
    // esnext + 不压缩:跳过 vite 的 esbuild-transpile/minify 阶段
    // (该阶段会在系统 TEMP 写临时文件,被 Windows Defender 锁导致 Access denied)
    target: 'esnext',
    minify: false,
    lib: {
      entry: resolve(__dirname, 'src/index.js'),
      name: 'AnalysisMaker',
      fileName: 'analysis-maker'
    },
    rollupOptions: {
      // 确保外部化处理那些你不想打包进库的依赖
      external: ['vue', 'element-plus', 'vxe-table', 'sortablejs', 'throttle-debounce', 'vue-echarts', 'echarts', 'echarts-gl',
        'monaco-editor', 'efficient-suite', 'vuedraggable'
      ],
      output: {
        exports: 'named',
        // 在 UMD 构建模式下为这些外部化的依赖提供一个全局变量
        globals: {
          vue: 'Vue',
          'element-plus': 'ElementPlus',
          'vxe-table': 'VXETable',
          sortablejs: 'Sortable',
          'throttle-debounce': 'throttleDebounce',
          'vue-echarts': 'VueECharts',
          echarts: 'Echarts',
          'echarts-gl': 'EchartsGl',
          'monaco-editor': 'MonacoEditor',
          'efficient-suite': 'EfficientSuite',
          vuedraggable: 'vuedraggable'
        }
      },
      plugins: [visualizer()]
    }
  },
  optimizeDeps: {
    include: [
      'vuedraggable'
    ]
  }
})
