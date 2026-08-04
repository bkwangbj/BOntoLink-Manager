<template>
  <Teleport to="body">
    <transition name="fdp-fade">
      <div v-if="open" class="fdp-mask" @click.self="close">
        <div class="fdp-modal">
          <div class="fdp-hd">
            <div class="fdp-title">选择目录</div>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="close" v-html="BL.icon('x', 14)"></button>
          </div>

          <div class="fdp-body">
            <div class="fdp-search">
              <span class="fdp-search-ic" v-html="BL.icon('search', 12)"></span>
              <input class="bl-input fdp-search-input" v-model="q" placeholder="搜索行业 / 领域" />
            </div>

            <div class="fdp-tree">
              <div v-for="node in filteredTree" :key="node.industry_dir" class="fdp-nwrap">
                <div :class="['fdp-row', sel.industry === node.industry_dir && !sel.category && 'is-on']"
                     @click="choose(node.industry_dir, null)">
                  <span class="fdp-toggle" :class="expanded.has(node.industry_dir) && 'is-open'"
                        @click.stop="toggle(node.industry_dir)" v-html="BL.icon('chevronRight', 10)"></span>
                  <span class="fdp-ic" style="background:#165DFF" v-html="BL.icon('industry', 11, '#fff')"></span>
                  <span class="fdp-label">{{ node.industry_dir }}</span>
                </div>
                <div v-if="expanded.has(node.industry_dir)" class="fdp-children">
                  <div v-for="c in node.categories" :key="c"
                       :class="['fdp-row', sel.industry === node.industry_dir && sel.category === c && 'is-on']"
                       @click="choose(node.industry_dir, c)">
                    <span class="fdp-toggle fdp-toggle-empty"></span>
                    <span class="fdp-ic" style="background:#00B42A" v-html="BL.icon('folder', 11, '#fff')"></span>
                    <span class="fdp-label">{{ c }}</span>
                  </div>
                </div>
              </div>
              <div v-if="!filteredTree.length" class="bl-empty" style="padding:30px;font-size:12px">无匹配目录</div>
            </div>
          </div>

          <div class="fdp-ft">
            <span class="bl-muted">当前路径:<b class="bl-mono fdp-path">{{ pathPreview }}</b></span>
            <span class="bl-grow"></span>
            <button class="bl-btn" @click="close">取消</button>
            <button class="bl-btn bl-btn-primary" :disabled="!sel.industry || !sel.category" @click="confirm">确定</button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
/**
 * 目录树选择弹窗 (文档 3.2.1 选择按钮)
 * 支持树形层级选择, 选中后由父组件回填行业、领域与路径。
 */
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { versionRepoApi } from '@/api'

const props = defineProps({
  open: { type: Boolean, default: false },
  industry: { type: String, default: '' },
  category: { type: String, default: '' }
})
const emit = defineEmits(['update:open', 'confirm'])

const tree = ref([])
const expanded = ref(new Set())
const sel = ref({ industry: '', category: '' })
const q = ref('')

async function load() {
  const list = await versionRepoApi.dirOptions().catch(() => [])
  tree.value = Array.isArray(list) ? list : []
  expanded.value = new Set(tree.value.map(n => n.industry_dir))
}

watch(() => props.open, (v) => {
  if (!v) return
  load()
  q.value = ''
  sel.value = { industry: props.industry || '', category: props.category || '' }
})

const filteredTree = computed(() => {
  const k = q.value.trim().toLowerCase()
  if (!k) return tree.value
  return tree.value
    .map(n => {
      const hit = n.industry_dir.toLowerCase().includes(k)
      const cats = (n.categories || []).filter(c => hit || c.toLowerCase().includes(k))
      return hit || cats.length ? { ...n, categories: cats } : null
    })
    .filter(Boolean)
})

const pathPreview = computed(() =>
  sel.value.industry ? `/${sel.value.industry}${sel.value.category ? '/' + sel.value.category : ''}` : '—')

function toggle(industry) {
  const s = new Set(expanded.value)
  s.has(industry) ? s.delete(industry) : s.add(industry)
  expanded.value = s
}
function choose(industry, category) {
  sel.value = { industry, category: category || '' }
  if (!expanded.value.has(industry)) toggle(industry)
}
function close() { emit('update:open', false) }
function confirm() {
  if (!sel.value.industry || !sel.value.category) return
  emit('confirm', { industry: sel.value.industry, category: sel.value.category })
  close()
}
</script>

<style scoped>
.fdp-mask {
  position: fixed; inset: 0; z-index: 1300;
  background: rgba(0, 0, 0, .45);
  display: flex; align-items: center; justify-content: center;
}
.fdp-modal {
  width: 520px; max-height: 72vh;
  background: var(--bl-bg-1); border-radius: 8px; border: 1px solid var(--bl-border);
  box-shadow: 0 12px 40px rgba(0, 0, 0, .2);
  display: flex; flex-direction: column; overflow: hidden;
}
.fdp-hd {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-bottom: 1px solid var(--bl-divider);
}
.fdp-title { font-size: 14px; font-weight: 600; color: var(--bl-text-1); }
.fdp-body { flex: 1; min-height: 0; overflow: auto; padding: 12px 16px; }
.fdp-ft {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 16px; border-top: 1px solid var(--bl-divider); background: var(--bl-bg-2);
}
.fdp-path { color: var(--bl-text-1); margin-left: 4px; }
.bl-grow { flex: 1; }

.fdp-search { position: relative; margin-bottom: 10px; }
.fdp-search-ic { position: absolute; left: 8px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.fdp-search-input { height: 30px; padding-left: 26px; font-size: 12px; }

.fdp-tree { max-height: 360px; overflow: auto; }
.fdp-row {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 8px; border-radius: 4px; font-size: 13px; cursor: pointer;
  color: var(--bl-text-1); user-select: none;
}
.fdp-row:hover { background: var(--bl-bg-hover); }
.fdp-row.is-on { background: var(--bl-primary-soft); color: var(--bl-primary); font-weight: 500; }
.fdp-toggle {
  width: 16px; height: 16px; flex-shrink: 0; color: var(--bl-text-3);
  display: inline-flex; align-items: center; justify-content: center; transition: transform .15s;
}
.fdp-toggle.is-open { transform: rotate(90deg); }
.fdp-toggle-empty { color: transparent; cursor: default; }
.fdp-ic {
  width: 20px; height: 20px; border-radius: 4px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
}
.fdp-label { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.fdp-children { margin-left: 18px; padding-left: 8px; }
.fdp-fade-enter-active, .fdp-fade-leave-active { transition: opacity .15s; }
.fdp-fade-enter-from, .fdp-fade-leave-to { opacity: 0; }
</style>
