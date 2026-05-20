<template>
  <div class="page">
    <PageHeader title="AI 助手" subtitle="AI Assistant · 自然语言交互式本体管理" />
    <div class="body">
      <div class="chat">
        <div class="msgs">
          <div v-for="(m,i) in messages" :key="i" :class="['msg', m.role]">
            <div class="bubble">{{ m.text }}</div>
          </div>
          <div v-if="!messages.length" class="bl-empty" style="padding:60px">
            <div class="bl-empty-icon" v-html="BL.icon('ai', 36)"></div>
            和 AI 聊一聊本体怎么建——「我想加一个水库相关的对象类型」
          </div>
        </div>
        <div class="composer">
          <input class="bl-input" v-model="input" placeholder="输入提问，Enter 发送…" @keydown.enter="send" />
          <button class="bl-btn bl-btn-primary" @click="send">发送</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { BL } from '@/lib/bl.js'

const input = ref('')
const messages = ref([])

function send() {
  const t = input.value.trim()
  if (!t) return
  messages.value.push({ role: 'user', text: t })
  input.value = ''
  setTimeout(() => {
    messages.value.push({ role: 'ai', text: `已收到你的需求：「${t}」。我会基于当前选中的领域命名空间为你建议对象、属性、关系。（演示数据，后续接入推理引擎）` })
  }, 300)
}
</script>
<style scoped>
.page { display: flex; flex-direction: column; height: 100%; }
.body { padding: 12px; flex: 1; overflow: hidden; display: flex; }
.chat {
  flex: 1; background: var(--bl-bg-1);
  border: 1px solid var(--bl-border); border-radius: var(--bl-radius-3);
  display: flex; flex-direction: column;
  overflow: hidden;
}
.msgs { flex: 1; overflow: auto; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.msg { display: flex; }
.msg.user { justify-content: flex-end; }
.bubble {
  padding: 10px 14px; max-width: 70%;
  border-radius: 12px;
  background: var(--bl-bg-2); color: var(--bl-text-1);
  font-size: var(--bl-fs-13);
}
.msg.user .bubble { background: var(--bl-primary); color: #fff; }
.composer { display: flex; gap: 8px; padding: 12px; border-top: 1px solid var(--bl-divider); }
</style>
