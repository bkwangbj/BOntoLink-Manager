import { defineStore } from 'pinia'

const STORAGE_KEY = 'bl-prefs'

function loadPrefs() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}') }
  catch { return {} }
}

export const useAppStore = defineStore('app', {
  state: () => {
    const p = loadPrefs()
    return {
      sidebarCollapsed: !!p.sidebarCollapsed,
      theme: p.theme || 'light',          // light | dark | system
      accent: p.accent || 'blue',          // blue/green/purple/orange/red/black
      domain: p.domain || 'w_wtr',         // 当前主工作领域(向后兼容)
      selectedDomains: Array.isArray(p.selectedDomains) ? p.selectedDomains : [], // 多领域工作范围
      domainPickerOpen: false,
      settingsOpen: false,
      voicePreset: p.voicePreset || 'gentle_peach',
      voiceNotify: p.voiceNotify !== false,
      voiceInput:  p.voiceInput !== false,
      locale: p.locale || 'zh_CN'
    }
  },
  actions: {
    openSettings()  { this.settingsOpen = true },
    closeSettings() { this.settingsOpen = false },
    openDomainPicker()  { this.domainPickerOpen = true },
    closeDomainPicker() { this.domainPickerOpen = false },
    setSelectedDomains(arr) {
      this.selectedDomains = arr || []
      this.persist()
    },
    setVoicePreset(v) { this.voicePreset = v; this.persist() },
    toggleVoiceNotify() { this.voiceNotify = !this.voiceNotify; this.persist() },
    toggleVoiceInput()  { this.voiceInput  = !this.voiceInput;  this.persist() },
    setLocale(v) { this.locale = v; this.persist() },
    persist() {
      const p = {
        sidebarCollapsed: this.sidebarCollapsed,
        theme: this.theme, accent: this.accent, domain: this.domain,
        selectedDomains: this.selectedDomains,
        voicePreset: this.voicePreset, voiceNotify: this.voiceNotify, voiceInput: this.voiceInput,
        locale: this.locale
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(p))
    },
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      this.persist()
    },
    setTheme(t) {
      this.theme = t
      this.persist()
      const actual = t === 'system'
        ? (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
        : t
      document.documentElement.setAttribute('data-theme', actual)
    },
    setAccent(a) {
      this.accent = a
      this.persist()
      if (a === 'blue') document.documentElement.removeAttribute('data-accent')
      else document.documentElement.setAttribute('data-accent', a)
    },
    setDomain(d) { this.domain = d; this.persist() }
  }
})
