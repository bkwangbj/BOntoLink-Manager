<template>
  <transition name="modal-fade">
    <div v-if="app.settingsOpen" class="settings-mask" @click.self="app.closeSettings()">
      <div class="settings-modal" role="dialog">
        <header class="hd">
          <span class="hd-l"><span class="hd-ic" v-html="BL.icon('sliders', 16)"></span> 设置</span>
          <button class="bl-btn bl-btn-text bl-btn-icon" @click="app.closeSettings()" v-html="BL.icon('x', 14)"></button>
        </header>

        <div class="body">
          <!-- 显示主题 / 主题色 -->
          <section class="block">
            <div class="block-title">显示主题</div>
            <div class="theme-row">
              <div v-for="t in themes" :key="t.k" :class="['theme-card', app.theme===t.k && 'is-active']" @click="app.setTheme(t.k)">
                <div :class="['theme-preview', 'theme-'+t.k]">
                  <span v-if="app.theme===t.k" class="theme-check" v-html="BL.icon('check', 12, '#fff')"></span>
                </div>
                <div class="theme-label">
                  <span class="theme-mini-ic" v-html="themeIcon(t.k)"></span>
                  {{ t.label }}
                </div>
              </div>
            </div>

            <div class="block-title" style="margin-top:16px">主题色</div>
            <div class="accent-row">
              <span v-for="a in accents" :key="a.k"
                    :class="['accent-dot', app.accent===a.k && 'is-active']"
                    :style="{ background: a.color }"
                    @click="app.setAccent(a.k)" :title="a.label">
                <span v-if="app.accent===a.k" v-html="BL.icon('check', 12, '#fff')"></span>
              </span>
            </div>
          </section>

          <!-- 语音 / 声音 -->
          <section class="block">
            <div class="block-title">语音 / 声音</div>
            <div class="row">
              <span class="row-l">声音模式</span>
              <select class="bl-input row-r" style="width:140px" v-model="voicePreset" @change="app.setVoicePreset(voicePreset)">
                <option value="gentle_peach">温柔桃子</option>
                <option value="warm_lemon">暖意柠檬</option>
                <option value="calm_mint">沉静薄荷</option>
                <option value="bright_oat">明亮燕麦</option>
              </select>
            </div>
            <div class="row">
              <span class="row-l">消息提示音</span>
              <button :class="['toggle', app.voiceNotify && 'is-on']" @click="app.toggleVoiceNotify()">
                <span class="toggle-knob"></span>
              </button>
            </div>
            <div class="row">
              <span class="row-l">语音输入识别</span>
              <button :class="['toggle', app.voiceInput && 'is-on']" @click="app.toggleVoiceInput()">
                <span class="toggle-knob"></span>
              </button>
            </div>

            <div class="block-title" style="margin-top:16px">语言</div>
            <div class="row">
              <span class="row-l">界面语言</span>
              <select class="bl-input row-r" style="width:140px" v-model="locale" @change="app.setLocale(locale)">
                <option value="zh_CN">中文 (简体)</option>
                <option value="zh_TW">中文 (繁体)</option>
                <option value="en_US">English</option>
              </select>
            </div>
          </section>

          <!-- 版本信息 -->
          <section class="block">
            <div class="block-title">版本信息</div>
            <div class="ver-card">
              <div class="ver-hd">
                <span class="ver-logo" v-html="BL.icon('cube', 14, '#fff')"></span>
                <span class="ver-title"><b>BOntoLink</b> 博智联</span>
                <span class="bl-sep"></span>
                <button class="bl-btn bl-btn-sm bl-btn-primary"><span style="display:inline-flex;align-items:center;gap:4px" v-html="iconText('zap','检查更新')"></span></button>
              </div>
              <div class="ver-rows">
                <div class="ver-row"><span class="vk">应用版本</span><span class="vv bl-mono">v1.0.2</span><span class="bl-tag bl-tag-success">最新</span></div>
                <div class="ver-row"><span class="vk">本体引擎</span><span class="vv bl-mono">v2.4.1</span></div>
                <div class="ver-row"><span class="vk">推理内核</span><span class="vv bl-mono">BOnto-Reasoner 0.18.3</span></div>
                <div class="ver-row"><span class="vk">构建版本</span><span class="vv bl-mono">build_20260512_b73e9c1 (release)</span></div>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { useAppStore } from '@/stores/app.js'

const app = useAppStore()
const voicePreset = ref(app.voicePreset)
const locale = ref(app.locale)
watch(() => app.voicePreset, v => voicePreset.value = v)
watch(() => app.locale,      v => locale.value = v)

const themes = [
  { k: 'light',  label: '浅色' },
  { k: 'dark',   label: '深色' },
  { k: 'system', label: '昼夜切换' }
]
const accents = [
  { k: 'blue',   color: '#165DFF', label: '科技蓝' },
  { k: 'green',  color: '#00B42A', label: '生机绿' },
  { k: 'purple', color: '#722ED1', label: '神秘紫' },
  { k: 'orange', color: '#FF7D00', label: '活力橙' },
  { k: 'red',    color: '#F53F3F', label: '热情红' },
  { k: 'black',  color: '#1D2129', label: '极简黑' }
]

function themeIcon(k) {
  const map = { light: 'droplet', dark: 'wave', system: 'grid' }
  return BL.icon(map[k] || 'cog', 12)
}
function iconText(ic, t) { return `${BL.icon(ic, 12, '#fff')}<span>${t}</span>` }
</script>

<style scoped>
.settings-mask {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.32);
  z-index: 1300;
  display: flex; align-items: flex-start; justify-content: center;
  padding-top: 6vh;
  overflow: auto;
}
.settings-modal {
  width: 560px; max-width: 92vw;
  background: var(--bl-bg-1);
  border-radius: var(--bl-radius-3);
  box-shadow: var(--bl-shadow-3);
  display: flex; flex-direction: column;
  margin-bottom: 6vh;
}
.hd {
  height: 56px; padding: 0 16px;
  display: flex; align-items: center; justify-content: space-between;
}
.hd-l { font-weight: 600; display: inline-flex; align-items: center; gap: 8px; }
.hd-ic { color: var(--bl-primary); }

.body { padding: 0 16px 16px; display: flex; flex-direction: column; gap: 12px; }

.block {
  background: var(--bl-bg-2);
  border-radius: var(--bl-radius-3);
  padding: 14px 16px;
}
.block-title { font-size: var(--bl-fs-13); color: var(--bl-text-2); margin-bottom: 10px; font-weight: 500; }

/* 主题卡片 */
.theme-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.theme-card {
  background: var(--bl-bg-1);
  border: 2px solid var(--bl-border);
  border-radius: var(--bl-radius-3);
  padding: 10px;
  cursor: pointer;
  transition: border-color .15s;
}
.theme-card.is-active { border-color: var(--bl-primary); }
.theme-preview {
  position: relative;
  height: 64px; border-radius: var(--bl-radius-2);
  margin-bottom: 8px;
  overflow: hidden;
}
.theme-light  { background: linear-gradient(135deg, #fff, #f1f3f6); border: 1px solid var(--bl-border); }
.theme-light::after { content:''; position:absolute; top:14px; left:14px; width:50%; height:6px; background: var(--bl-primary); border-radius:2px; }
.theme-dark   { background: linear-gradient(135deg, #1F2126, #16181C); }
.theme-dark::after { content:''; position:absolute; top:14px; left:14px; width:50%; height:6px; background: var(--bl-primary); border-radius:2px; }
.theme-system { background: linear-gradient(135deg, #fff 50%, #1F2126 50%); }
.theme-system::after { content:''; position:absolute; top:14px; left:14px; width:50%; height:6px; background: var(--bl-primary); border-radius:2px; }
.theme-check {
  position: absolute; top: 6px; right: 6px;
  width: 18px; height: 18px; border-radius: 50%;
  background: var(--bl-primary);
  display: flex; align-items: center; justify-content: center;
}
.theme-label { text-align: center; font-size: var(--bl-fs-13); display: inline-flex; align-items: center; gap: 6px; width: 100%; justify-content: center; }
.theme-mini-ic { color: var(--bl-text-3); display: inline-flex; }

/* 主题色 */
.accent-row { display: flex; gap: 12px; }
.accent-dot {
  width: 28px; height: 28px; border-radius: 50%; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  position: relative; border: 2px solid transparent;
}
.accent-dot.is-active { border-color: var(--bl-bg-1); box-shadow: 0 0 0 2px var(--bl-primary); }

/* 行 */
.row {
  display: flex; align-items: center;
  padding: 8px 0;
  font-size: var(--bl-fs-13);
  border-bottom: 1px dashed var(--bl-border);
}
.row:last-child { border-bottom: 0; }
.row-l { flex: 1; color: var(--bl-text-1); }
.row-r { font-size: var(--bl-fs-13); }

/* 开关 */
.toggle {
  width: 38px; height: 22px;
  background: var(--bl-text-4);
  border-radius: 999px;
  border: 0; position: relative;
  cursor: pointer; transition: background-color .15s;
  padding: 0;
}
.toggle.is-on { background: var(--bl-primary); }
.toggle-knob {
  position: absolute; top: 2px; left: 2px;
  width: 18px; height: 18px; border-radius: 50%;
  background: #fff;
  transition: transform .15s;
}
.toggle.is-on .toggle-knob { transform: translateX(16px); }

/* 版本卡 */
.ver-card {
  background: var(--bl-bg-1);
  border-radius: var(--bl-radius-3);
  border: 1px solid var(--bl-border);
}
.ver-hd {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--bl-divider);
}
.ver-logo {
  width: 24px; height: 24px; border-radius: 6px;
  background: linear-gradient(135deg, var(--bl-primary), #4080FF);
  color: #fff; display: inline-flex; align-items: center; justify-content: center;
}
.ver-title { font-size: var(--bl-fs-14); }
.ver-rows { padding: 6px 14px 10px; }
.ver-row {
  display: flex; align-items: center; gap: 10px;
  padding: 7px 0;
  font-size: var(--bl-fs-13);
  border-bottom: 1px dashed var(--bl-divider);
}
.ver-row:last-child { border-bottom: 0; }
.vk { width: 80px; color: var(--bl-text-3); }
.vv { color: var(--bl-text-1); flex: 1; word-break: break-all; }

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity .15s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
</style>
