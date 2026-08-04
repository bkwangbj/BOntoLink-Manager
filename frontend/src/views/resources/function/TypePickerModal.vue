<template>
  <Teleport to="body">
    <transition name="ftp-fade">
      <div v-if="open" class="ftp-mask" @click.self="close">
        <div class="ftp-modal">
          <div class="ftp-hd">
            <div class="ftp-title">{{ title }}</div>
            <button class="bl-btn bl-btn-text bl-btn-icon" title="关闭" @click="close" v-html="BL.icon('x', 14)"></button>
          </div>

          <div class="ftp-body">
            <!-- 基础类型 -->
            <div class="sec">基础类型</div>
            <div class="ftp-base">
              <div v-for="t in BASE_TYPES" :key="t"
                   :class="['ftp-base-item', pick.type === t && 'is-on']"
                   @click="choose(t, null)">
                <span class="bl-mono">{{ t }}</span>
                <span v-if="pick.type === t" class="ftp-check" v-html="BL.icon('check', 11, '#fff')"></span>
              </div>
            </div>

            <!-- 本体对象类型 -->
            <div class="sec ftp-sec-obj">
              <span>本体对象类型</span>
              <div class="ftp-search">
                <span class="ftp-search-ic" v-html="BL.icon('search', 12)"></span>
                <input class="bl-input ftp-search-input" v-model="q" placeholder="搜索对象名称 / 编码" />
              </div>
            </div>
            <div class="ftp-objs">
              <div v-for="c in filteredClasses" :key="c.id"
                   :class="['ftp-obj', pick.classId === c.id && 'is-on']"
                   @click="choose(typeOf(c), c.id)">
                <span class="ftp-obj-ic" :style="{ background: c.color || '#165DFF' }"
                      v-html="BL.icon(c.icon || 'cube', 12, '#fff')"></span>
                <div class="ftp-obj-txt">
                  <div class="bl-truncate ftp-obj-cn">{{ c.cn }}</div>
                  <div class="bl-mono bl-muted bl-truncate ftp-obj-api">{{ typeOf(c) }}</div>
                </div>
                <span v-if="pick.classId === c.id" class="ftp-check is-obj" v-html="BL.icon('check', 11, '#fff')"></span>
              </div>
              <div v-if="!filteredClasses.length" class="bl-empty" style="padding:24px;font-size:12px">无匹配对象类型</div>
            </div>

            <!-- 选中本体对象后的提示条 (文档 3.3.4) -->
            <div v-if="pick.classId" class="ftp-tip">
              <span v-html="BL.icon('info', 12)"></span>
              已选择本体对象类型,系统将自动关联对应的实体类、属性模型与关联关系,生成类型定义
            </div>
          </div>

          <div class="ftp-ft">
            <span class="ftp-cur bl-muted">当前选择:<b class="bl-mono">{{ pick.type || '—' }}</b></span>
            <span class="bl-grow"></span>
            <button class="bl-btn" @click="close">取消</button>
            <button class="bl-btn bl-btn-primary" :disabled="!pick.type" @click="confirm">确定</button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
/**
 * 类型选择弹窗 (文档 3.3.3)
 * 参数类型与返回值类型共用同一组件, 分「基础类型 / 本体对象类型」两组点选。
 */
import { ref, computed, watch } from 'vue'
import { BL } from '@/lib/bl.js'
import { resourceApi } from '@/api'

const props = defineProps({
  open: { type: Boolean, default: false },
  /** 当前已选类型字符串, 打开时回显 */
  value: { type: String, default: '' },
  title: { type: String, default: '选择类型' }
})
const emit = defineEmits(['update:open', 'confirm'])

const BASE_TYPES = ['string', 'number', 'boolean', 'any']

const classes = ref([])
const q = ref('')
const pick = ref({ type: '', classId: null })

/** 对象类型展示形式: [命名空间] 类名, 与列表页「作用对象」列一致 */
function typeOf(c) { return c.ns_code ? `[${c.ns_code}] ${c.api_name}` : c.api_name }

async function loadClasses() {
  if (classes.value.length) return
  const list = await resourceApi.classes().catch(() => [])
  const arr = Array.isArray(list) ? list : (list?.data || list?.rows || [])
  classes.value = arr.map(c => ({
    id: c.id,
    cn: c.display_name || c.rdfs_label || c.api_name,
    api_name: c.api_name,
    ns_code: c.ns_code || c.nsCode || '',
    icon: c.icon,
    color: c.color
  }))
}

watch(() => props.open, (v) => {
  if (!v) return
  loadClasses()
  q.value = ''
  const cur = String(props.value || '')
  const hit = classes.value.find(c => typeOf(c) === cur)
  pick.value = { type: cur, classId: hit ? hit.id : null }
})

const filteredClasses = computed(() => {
  const k = q.value.trim().toLowerCase()
  if (!k) return classes.value
  return classes.value.filter(c => [c.cn, c.api_name, c.ns_code].filter(Boolean)
    .some(s => String(s).toLowerCase().includes(k)))
})

function choose(type, classId) { pick.value = { type, classId } }
function close() { emit('update:open', false) }
function confirm() {
  if (!pick.value.type) return
  emit('confirm', { param_type: pick.value.type, object_class_id: pick.value.classId })
  close()
}
</script>

<style scoped>
.ftp-mask {
  position: fixed; inset: 0; z-index: 1300;
  background: rgba(0, 0, 0, .45);
  display: flex; align-items: center; justify-content: center;
}
.ftp-modal {
  width: 640px; max-height: 78vh;
  background: var(--bl-bg-1);
  border-radius: 8px; border: 1px solid var(--bl-border);
  box-shadow: 0 12px 40px rgba(0, 0, 0, .2);
  display: flex; flex-direction: column; overflow: hidden;
}
.ftp-hd {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-bottom: 1px solid var(--bl-divider);
}
.ftp-title { font-size: 14px; font-weight: 600; color: var(--bl-text-1); }
.ftp-body { flex: 1; min-height: 0; overflow: auto; padding: 12px 16px 16px; }
.ftp-ft {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 16px; border-top: 1px solid var(--bl-divider); background: var(--bl-bg-2);
}
.ftp-cur b { color: var(--bl-text-1); margin-left: 4px; }
.bl-grow { flex: 1; }

.sec {
  font-size: 12px; color: var(--bl-text-3);
  border-left: 3px solid var(--bl-primary); padding-left: 8px;
  margin: 4px 0 10px; line-height: 16px;
}
.ftp-sec-obj { display: flex; align-items: center; gap: 12px; margin-top: 16px; }
.ftp-search { position: relative; margin-left: auto; width: 200px; }
.ftp-search-ic { position: absolute; left: 8px; top: 50%; transform: translateY(-50%); color: var(--bl-text-3); }
.ftp-search-input { height: 28px; padding-left: 26px; font-size: 12px; }

.ftp-base { display: flex; flex-wrap: wrap; gap: 8px; }
.ftp-base-item {
  position: relative; min-width: 96px;
  padding: 8px 12px; border: 1px solid var(--bl-border); border-radius: 6px;
  cursor: pointer; font-size: 13px; color: var(--bl-text-1); background: var(--bl-bg-1);
}
.ftp-base-item:hover { border-color: var(--bl-primary); }
.ftp-base-item.is-on { border-color: var(--bl-primary); border-width: 2px; background: var(--bl-primary-soft); color: var(--bl-primary); }

.ftp-objs {
  display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px;
  max-height: 300px; overflow: auto;
}
.ftp-obj {
  position: relative; display: flex; align-items: center; gap: 8px; min-width: 0;
  padding: 8px 10px; border: 1px solid var(--bl-border); border-radius: 6px; cursor: pointer;
}
.ftp-obj:hover { border-color: var(--bl-primary); }
.ftp-obj.is-on { border-color: var(--bl-primary); border-width: 2px; background: var(--bl-primary-soft); }
.ftp-obj-ic {
  width: 24px; height: 24px; border-radius: 5px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
}
.ftp-obj-txt { min-width: 0; }
.ftp-obj-cn { font-size: 12.5px; color: var(--bl-text-1); }
.ftp-obj-api { font-size: 11px; }
.ftp-check {
  position: absolute; top: -1px; right: -1px;
  width: 16px; height: 16px; border-radius: 0 5px 0 6px; background: var(--bl-primary);
  display: inline-flex; align-items: center; justify-content: center;
}
.ftp-tip {
  display: flex; align-items: center; gap: 6px;
  margin-top: 12px; padding: 8px 10px; border-radius: 4px;
  background: var(--bl-primary-soft); color: var(--bl-primary); font-size: 12px;
}
.ftp-fade-enter-active, .ftp-fade-leave-active { transition: opacity .15s; }
.ftp-fade-enter-from, .ftp-fade-leave-to { opacity: 0; }
</style>
