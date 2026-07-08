<template>
  <div class="cpf" :class="{ 'cpf-compact': !swatches }">
    <div class="cpf-swatches">
      <template v-if="swatches">
        <span v-for="c in palette" :key="c"
              :class="['cpf-dot', isSame(c) && 'is-active']"
              :style="{ background: c }"
              :title="c"
              @click="pick(c)"></span>
      </template>
      <span class="cpf-tail">
        <input class="bl-input bl-mono cpf-hex" :value="modelValue" @input="onHex" @change="onHexChange" placeholder="#RRGGBB" />
        <input type="color" class="cpf-native" :value="normalized" @input="onNative" title="自定义颜色" />
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  swatches: { type: Boolean, default: true },   // 是否显示预设色板;false=仅 hex 输入 + 取色块(简洁)
  // 预设色板（铺满 2 行,小方块）
  palette: {
    type: Array,
    default: () => ([
      '#165DFF','#00B42A','#722ED1','#FF7D00','#EB2F96','#13C2C2','#FADB14','#F53F3F','#86909C','#2F54EB',
      '#52C41A','#9254DE','#FA8C16','#36CFC9','#FF4D4F','#1D39C4','#389E0D','#531DAB','#D48806','#08979C',
      // 追加 4 个：青灰、深粉、暖橙、墨绿
      '#4E5969','#C41D7F','#D46B08','#135200'
    ])
  }
})
const emit = defineEmits(['update:modelValue', 'change'])

function isSame(c) {
  return String(props.modelValue || '').toLowerCase() === String(c).toLowerCase()
}
// <input type=color> 只接受 #rrggbb；非法值兜底为灰色
const normalized = computed(() => {
  const v = String(props.modelValue || '').trim()
  return /^#[0-9a-fA-F]{6}$/.test(v) ? v : '#86909C'
})
function pick(c) { emit('update:modelValue', c); emit('change', c) }
function onHex(e) { emit('update:modelValue', e.target.value) }
function onHexChange(e) { emit('change', e.target.value) }
function onNative(e) { emit('update:modelValue', e.target.value); emit('change', e.target.value) }
</script>

<style scoped>
.cpf { width: 100%; }
.cpf-swatches { display: flex; flex-wrap: wrap; gap: 5px; align-items: center; max-width: 386px; }
.cpf-dot {
  width: 18px; height: 18px; border-radius: 4px;   /* 小方块 */
  cursor: pointer; border: 2px solid transparent;
  flex-shrink: 0;
  box-sizing: border-box;
}
.cpf-dot.is-active { border-color: var(--bl-bg-1); box-shadow: 0 0 0 2px var(--bl-primary); }
.cpf-tail { margin-left: auto; display: inline-flex; align-items: center; flex-shrink: 0; }
.cpf-compact .cpf-swatches { max-width: none; }
.cpf-compact .cpf-tail { margin-left: 0; }
.cpf-compact .cpf-hex { width: 108px; }
.cpf-hex {
  width: 92px; height: 26px;
  border-top-right-radius: 0; border-bottom-right-radius: 0;
}
.cpf-native {
  width: 28px; height: 26px;
  border: 1px solid var(--bl-border); border-left: 0;
  border-top-right-radius: var(--bl-radius-3);
  border-bottom-right-radius: var(--bl-radius-3);
  cursor: pointer; padding: 2px; background: var(--bl-bg-1);
  flex-shrink: 0;
}
.cpf-native::-webkit-color-swatch-wrapper { padding: 0; }
.cpf-native::-webkit-color-swatch { border: 0; border-radius: 6px; }
.cpf-native::-moz-color-swatch { border: 0; border-radius: 6px; }
</style>
