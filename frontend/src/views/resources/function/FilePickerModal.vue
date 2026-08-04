<template>
  <Teleport to="body">
    <transition name="ffp-fade">
      <div v-if="open" class="ffp-mask" @click.self="close">
        <div class="ffp-modal">
          <div class="ffp-hd">
            <div class="ffp-title">选择文件</div>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="close" v-html="BL.icon('x', 14)"></button>
          </div>

          <div class="ffp-body">
            <div class="ffp-bar">
              <label class="ffp-scope">
                <input type="checkbox" :checked="onlyCurrentDir" @change="onlyCurrentDir = !onlyCurrentDir" />
                仅当前目录<span class="bl-muted" v-if="industry">({{ industry }}{{ category ? ' / ' + category : '' }})</span>
              </label>
              <div class="ffp-search">
                <span class="ffp-search-ic" v-html="BL.icon('search', 12)"></span>
                <input class="bl-input ffp-search-input" v-model="q" placeholder="搜索文件路径 / 类名" />
              </div>
            </div>

            <table class="bl-table ffp-table">
              <thead>
                <tr>
                  <th class="t-left">文件路径</th>
                  <th class="t-left">所属目录</th>
                  <th class="t-left">类名</th>
                  <th class="t-center">函数数</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="f in filteredFiles" :key="f.code_file_path + f.industry_dir + f.category_dir"
                    :class="['ffp-row', sel === f.code_file_path && 'is-on']"
                    @click="sel = f.code_file_path">
                  <td class="t-left"><span class="bl-mono">{{ f.code_file_path }}</span></td>
                  <td class="t-left"><span class="bl-muted">{{ f.industry_dir }} / {{ f.category_dir }}</span></td>
                  <td class="t-left"><span class="bl-mono bl-muted">{{ f.class_name || '—' }}</span></td>
                  <td class="t-center">{{ f.fn_count }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="!filteredFiles.length" class="bl-empty" style="padding:32px;font-size:12px">
              暂无匹配文件,可直接在输入框里填写新文件名,创建函数时会自动生成
            </div>
          </div>

          <div class="ffp-ft">
            <span class="bl-muted">已选文件:<b class="bl-mono ffp-cur">{{ sel || '—' }}</b></span>
            <span class="bl-grow"></span>
            <button class="bl-btn" @click="close">取消</button>
            <button class="bl-btn bl-btn-primary" :disabled="!sel" @click="confirm">确定</button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
/**
 * 文件列表选择弹窗 (文档 3.2.2 文件「选择」按钮)
 * 选中已有文件后, 新函数会追加到该文件中。
 */
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { functionApi } from '@/api'

const props = defineProps({
  open: { type: Boolean, default: false },
  industry: { type: String, default: '' },
  category: { type: String, default: '' },
  /** 语言对应的文件后缀, 只列同语言文件 */
  ext: { type: String, default: 'ts' }
})
const emit = defineEmits(['update:open', 'confirm'])

const files = ref([])
const q = ref('')
const sel = ref('')
const onlyCurrentDir = ref(true)

async function load() {
  const list = await functionApi.files().catch(() => [])
  files.value = Array.isArray(list) ? list : []
}
watch(() => props.open, (v) => {
  if (!v) return
  load()
  q.value = ''
  sel.value = ''
  onlyCurrentDir.value = !!props.industry
})

const filteredFiles = computed(() => {
  let list = files.value.filter(f => String(f.code_file_path || '').endsWith('.' + props.ext))
  if (onlyCurrentDir.value && props.industry) {
    list = list.filter(f => f.industry_dir === props.industry && (!props.category || f.category_dir === props.category))
  }
  const k = q.value.trim().toLowerCase()
  if (k) list = list.filter(f => [f.code_file_path, f.class_name].filter(Boolean)
    .some(s => String(s).toLowerCase().includes(k)))
  return list
})

function close() { emit('update:open', false) }
function confirm() {
  if (!sel.value) return
  emit('confirm', sel.value)
  close()
}
</script>

<style scoped>
.ffp-mask {
  position: fixed; inset: 0; z-index: 1300;
  background: rgba(0, 0, 0, .45);
  display: flex; align-items: center; justify-content: center;
}
.ffp-modal {
  width: 720px; max-height: 72vh;
  background: var(--bl-bg-1); border-radius: 8px; border: 1px solid var(--bl-border);
  box-shadow: 0 12px 40px rgba(0, 0, 0, .2);
  display: flex; flex-direction: column; overflow: hidden;
}
.ffp-hd {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-bottom: 1px solid var(--bl-divider);
}
.ffp-title { font-size: 14px; font-weight: 600; color: var(--bl-text-1); }
.ffp-body { flex: 1; min-height: 0; overflow: auto; padding: 12px 16px; }
.ffp-ft {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 16px; border-top: 1px solid var(--bl-divider); background: var(--bl-bg-2);
}
.ffp-cur { color: var(--bl-text-1); margin-left: 4px; }
.bl-grow { flex: 1; }

.ffp-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
.ffp-scope { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: var(--bl-text-2); cursor: pointer; }
.ffp-search { position: relative; margin-left: auto; width: 240px; }
.ffp-search-ic { position: absolute; left: 8px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.ffp-search-input { height: 28px; padding-left: 26px; font-size: 12px; }

.ffp-table { width: 100%; }
.ffp-table thead th {
  background: var(--bl-thead-bg); font-size: 12px; font-weight: 600;
  height: 34px; padding: 0 8px; color: var(--bl-text-1); white-space: nowrap;
}
.ffp-table thead th.t-left { text-align: left; }
.ffp-table td { padding: 0 8px; height: 38px; font-size: 12px; }
.ffp-table td.t-center { text-align: center; }
.ffp-row { cursor: pointer; }
.ffp-row:hover { background: var(--bl-bg-hover); }
.ffp-row.is-on { background: var(--bl-primary-soft); }
.ffp-fade-enter-active, .ffp-fade-leave-active { transition: opacity .15s; }
.ffp-fade-enter-from, .ffp-fade-leave-to { opacity: 0; }
</style>
