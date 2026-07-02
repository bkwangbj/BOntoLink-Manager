<!--
  参数编辑弹窗(1.5)。900×700 左右分栏:
    左=绑定参数(Schema 动态表单);右上=实时预览(160px);右下=参数 JSON(实时)。
  用于对「已绑定类型类」中的配置型类型类编辑参数值 → 保存回 bind.value。
-->
<template>
  <div class="pem-mask" @click.self="$emit('close')">
    <div class="pem">
      <header class="pem-hd">
        <span class="pem-title">编辑 {{ bind.name_cn_base || bind.name_prefix }} - 绑定参数</span>
        <button class="pem-x" @click="$emit('close')" v-html="BL.icon('x', 16)"></button>
      </header>

      <div class="pem-body">
        <!-- 左:参数表单 -->
        <div class="pem-left">
          <div class="pem-sec">绑定参数</div>
          <ParamSchemaForm v-model="values" :schema="bind.param_json" />
        </div>
        <!-- 右:预览 + JSON -->
        <div class="pem-right">
          <div class="pem-sec">实时预览</div>
          <div class="pem-preview">
            <ParamPreview :name-prefix="bind.name_prefix" :category="bind.category_code" :values="values" />
          </div>
          <div class="pem-sec" style="margin-top:12px">参数 JSON</div>
          <pre class="pem-json">{{ jsonText }}</pre>
        </div>
      </div>

      <footer class="pem-ft">
        <button class="bl-btn bl-btn-sm" @click="$emit('close')">取消</button>
        <button class="bl-btn bl-btn-sm bl-btn-primary" @click="$emit('save', values)">保存</button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { BL } from '@/lib/bl.js'
import ParamSchemaForm from './ParamSchemaForm.vue'
import ParamPreview from './ParamPreview.vue'

const props = defineProps({
  bind: { type: Object, required: true }   // 含 param_json / name_cn_base / name_prefix / category_code / value
})
defineEmits(['save', 'close'])

function initValues() {
  const v = props.bind.value
  if (v && typeof v === 'object') return { ...v }
  if (typeof v === 'string' && v.trim()) { try { return JSON.parse(v) } catch { return {} } }
  return {}
}
const values = ref(initValues())
const jsonText = computed(() => JSON.stringify(values.value, null, 2))
</script>

<style scoped>
.pem-mask { position: fixed; inset: 0; z-index: 1300; background: rgba(0,0,0,.32); display: flex; align-items: center; justify-content: center; }
.pem { width: 900px; max-width: 94vw; height: 700px; max-height: 92vh; background: var(--bl-bg-1); border-radius: 10px; box-shadow: 0 16px 48px rgba(0,0,0,.24); display: flex; flex-direction: column; overflow: hidden; }
.pem-hd { display: flex; align-items: center; padding: 14px 18px; border-bottom: 1px solid var(--bl-divider); }
.pem-title { flex: 1; font-size: 15px; font-weight: 600; color: var(--bl-text-1); }
.pem-x { width: 28px; height: 28px; border: 0; background: transparent; border-radius: 6px; color: var(--bl-text-3); cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.pem-x:hover { background: var(--bl-bg-hover); color: var(--bl-text-1); }
.pem-body { flex: 1; display: flex; min-height: 0; }
.pem-left { flex: 1; min-width: 0; padding: 14px 18px; overflow: auto; border-right: 1px solid var(--bl-divider); }
.pem-right { flex: 1; min-width: 0; padding: 14px 18px; overflow: auto; display: flex; flex-direction: column; }
.pem-sec { font-size: 14px; font-weight: 600; color: var(--bl-text-1); margin-bottom: 10px; }
.pem-preview { height: 160px; border: 1px solid var(--bl-border); border-radius: 8px; background: var(--bl-bg-2); display: flex; align-items: center; justify-content: center; overflow: hidden; }
.pem-json { flex: 1; min-height: 120px; margin: 0; padding: 10px 12px; background: var(--bl-bg-2); border: 1px solid var(--bl-border); border-radius: 8px; font-family: var(--bl-mono, ui-monospace, monospace); font-size: 12px; line-height: 1.6; color: var(--bl-text-1); white-space: pre-wrap; overflow: auto; }
.pem-ft { display: flex; justify-content: flex-end; gap: 10px; padding: 12px 18px; border-top: 1px solid var(--bl-divider); background: var(--bl-bg-2); }
</style>
