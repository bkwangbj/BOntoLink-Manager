import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/styles/tokens.css'
import './assets/styles/base.css'
import { BL } from './lib/bl.js'

const saved = JSON.parse(localStorage.getItem('bl-prefs') || '{}')
document.documentElement.setAttribute('data-theme', saved.theme || 'light')
if (saved.accent) document.documentElement.setAttribute('data-accent', saved.accent)

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.config.globalProperties.$bl = BL
app.mount('#app')
