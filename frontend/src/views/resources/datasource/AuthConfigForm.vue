<template>
  <div class="acf">
    <div class="acf-row">
      <span class="acf-lbl">鉴权类型 <i>*</i></span>
      <BlSelect :model-value="authType" @update:modelValue="onTypeChange" :options="AUTH_TYPE_OPTS" style="flex:1" />
    </div>

    <div v-if="meta" class="acf-desc">{{ meta.desc }}</div>

    <!-- 无鉴权只给提示条 -->
    <div v-if="authType === 'none'" class="acf-none">当前数据源无需认证,所有接口直接调用。</div>

    <div v-else class="acf-fields">
      <div v-for="f in fields" :key="f.key" class="acf-row">
        <span class="acf-lbl">{{ f.label }} <i v-if="f.required">*</i></span>
        <div class="acf-ctl">
          <BlSelect v-if="f.type === 'select'" :model-value="val(f)" @update:modelValue="v => set(f, v)" :options="f.options" />
          <div v-else-if="f.type === 'multi'" class="acf-multi">
            <span v-for="v in (val(f) || [])" :key="v" class="acf-tag">{{ v }}
              <button class="acf-tag-x" @click="toggleMulti(f, v)" v-html="BL.icon('x', 9)"></button></span>
            <BlSelect :model-value="''" @update:modelValue="v => v && toggleMulti(f, v)"
                      :options="restOptions(f)" :placeholder="(val(f) || []).length ? '继续添加…' : '选择权限范围'" size="sm" style="min-width:150px" />
          </div>
          <label v-else-if="f.type === 'switch'" class="acf-sw">
            <span class="adw-showsw" :class="{ 'is-on': Number(val(f)) === 1 }" @click="set(f, Number(val(f)) === 1 ? 0 : 1)"><span class="adw-showsw-dot"></span></span>
            <span class="acf-sw-txt">{{ Number(val(f)) === 1 ? '开启' : '关闭' }}</span>
          </label>
          <textarea v-else-if="f.type === 'textarea'" class="bl-textarea bl-mono" rows="3"
                    :value="val(f)" @input="e => set(f, e.target.value)" :placeholder="f.secret ? '粘贴密钥内容(保存后掩码显示)' : ''"></textarea>
          <input v-else-if="f.type === 'password'" class="bl-input" type="password" autocomplete="new-password"
                 :value="val(f)" @input="e => set(f, e.target.value)" placeholder="••••••••" />
          <input v-else-if="f.type === 'number'" class="bl-input" type="number" :value="val(f)" @input="e => set(f, e.target.value)" />
          <input v-else class="bl-input" :value="val(f)" @input="e => set(f, e.target.value)" />
          <div v-if="f.tip" class="acf-tip">{{ f.tip }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { BL } from '@/lib/bl.js'
import BlSelect from '@/components/BlSelect.vue'
import { AUTH_TYPE_OPTS, AUTH_META, visibleAuthFields, defaultAuthConfig } from './authModel.js'

const props = defineProps({
  authType: { type: String, default: 'none' },
  config: { type: Object, default: () => ({}) },
})
const emit = defineEmits(['update:authType', 'update:config'])

const meta = computed(() => AUTH_META[props.authType] || null)
const fields = computed(() => visibleAuthFields(props.authType, props.config))

function val(f) { return props.config?.[f.key] ?? (f.def !== undefined ? f.def : '') }
function set(f, v) { emit('update:config', { ...props.config, [f.key]: v }) }
/* 换鉴权类型 = 换一套完全不同的参数, 直接重置成新类型的默认值 */
function onTypeChange(t) {
  emit('update:authType', t)
  emit('update:config', defaultAuthConfig(t))
}
function toggleMulti(f, v) {
  const cur = Array.isArray(val(f)) ? val(f) : []
  set(f, cur.includes(v) ? cur.filter(x => x !== v) : [...cur, v])
}
function restOptions(f) {
  const cur = Array.isArray(val(f)) ? val(f) : []
  return (f.options || []).filter(o => !cur.includes(o.value))
}
</script>

<style scoped>
.acf { display: flex; flex-direction: column; gap: 12px; }
.acf-row { display: flex; align-items: flex-start; gap: 10px; }
.acf-lbl { flex: 0 0 132px; text-align: right; font-size: 12.5px; color: var(--bl-text-2); line-height: 32px; }
.acf-lbl i { color: #f53f3f; font-style: normal; margin-left: 2px; }
.acf-ctl { flex: 1; min-width: 0; }
.acf-ctl .bl-input, .acf-ctl .bl-textarea { width: 100%; box-sizing: border-box; }
.acf-desc { font-size: 11.5px; color: var(--bl-text-3); padding-left: 142px; margin-top: -6px; }
.acf-none { margin-left: 142px; background: var(--bl-bg-2); border: 1px solid var(--bl-divider); border-radius: 6px;
  padding: 10px 14px; font-size: 12.5px; color: var(--bl-text-2); }
.acf-fields { display: flex; flex-direction: column; gap: 12px; background: var(--bl-bg-2); border-radius: 8px; padding: 14px 16px 16px; }
.acf-tip { font-size: 11px; color: var(--bl-text-3); margin-top: 4px; }
.acf-sw { display: inline-flex; align-items: center; gap: 8px; height: 32px; cursor: pointer; }
.acf-sw-txt { font-size: 12.5px; color: var(--bl-text-2); }
.acf-multi { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; }
.acf-tag { display: inline-flex; align-items: center; gap: 4px; height: 24px; padding: 0 4px 0 8px; border-radius: 4px;
  background: var(--bl-primary-soft); color: var(--bl-primary); font-size: 12px; }
.acf-tag-x { border: 0; background: transparent; color: inherit; cursor: pointer; display: inline-flex; padding: 2px; border-radius: 3px; }
.acf-tag-x:hover { background: color-mix(in srgb, var(--bl-primary) 20%, transparent); }
/* 开关沿用全局形态 */
.adw-showsw { display: inline-block; width: 40px; height: 22px; border-radius: 11px; background: var(--bl-bg-3, #c9cdd4); position: relative; cursor: pointer; transition: background .15s; flex-shrink: 0; }
.adw-showsw.is-on { background: var(--bl-primary); }
.adw-showsw-dot { position: absolute; left: 2px; top: 2px; width: 18px; height: 18px; border-radius: 50%; background: #fff; transition: left .15s; box-shadow: 0 1px 2px rgba(0,0,0,.3); }
.adw-showsw.is-on .adw-showsw-dot { left: 20px; }
</style>
