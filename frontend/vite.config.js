import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import UnoCSS from 'unocss/vite'
import { presetUno } from 'unocss'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    // 仅为 analysis-maker 提供原子类:只扫描 maker 的预编译 dist,不扫 BOntoLink02 自身源码(避免类名冲突)
    UnoCSS({
      presets: [presetUno()],
      content: { filesystem: ['node_modules/analysis-maker-vue3/dist/analysis-maker.js'] }
    })
  ],
  resolve: {
    // analysis-maker-vue3 是 file: 符号链接(指向 frontend 外部),其 dist 把
    // vuedraggable/throttle-debounce/monaco-editor 当外部依赖、要求宿主提供。
    // 默认 Vite 会把符号链接解析成真实路径,导致从包外部找 node_modules 而漏掉
    // frontend/node_modules。保留符号链接路径后,这些依赖才能命中宿主依赖。
    preserveSymlinks: true,
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      // analysis-maker 预构建 dist 引用了旧内部包名 bk-suite,实为 efficient-suite
      'bk-suite': 'efficient-suite'
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8088/bontolink',
        changeOrigin: true
      }
    }
  }
})
