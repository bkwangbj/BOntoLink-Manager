<template>
  <Teleport to="body">
    <transition name="idehelp-fade">
      <div v-if="open" class="fnide idehelp-mask" :data-ide-theme="theme" @click.self="close">
        <div class="idehelp">
          <div class="idehelp-hd">
            <span class="idehelp-title">帮助文档</span>
            <span class="bl-grow"></span>
            <div class="idehelp-search">
              <span class="idehelp-search-ic" v-html="BL.icon('search', 11)"></span>
              <input class="idehelp-input" v-model="q" placeholder="搜索文档内容" />
            </div>
            <button class="idehelp-x" title="关闭" @click="close" v-html="BL.icon('x', 14)"></button>
          </div>

          <div class="idehelp-body">
            <aside class="idehelp-nav">
              <div v-for="d in filtered" :key="d.k"
                   :class="['idehelp-nav-item', active === d.k && 'is-on']"
                   @click="active = d.k">
                <span v-html="BL.icon(d.icon, 12)"></span>{{ d.title }}
              </div>
              <div v-if="!filtered.length" class="idehelp-empty">无匹配文档</div>
            </aside>

            <article class="idehelp-content">
              <h2 class="idehelp-h2">{{ current.title }}</h2>
              <template v-for="(b, i) in current.blocks" :key="i">
                <h3 v-if="b.h" class="idehelp-h3">{{ b.h }}</h3>
                <p v-else-if="b.p" class="idehelp-p">{{ b.p }}</p>
                <ul v-else-if="b.ul" class="idehelp-ul">
                  <li v-for="(li, li_i) in b.ul" :key="li_i">{{ li }}</li>
                </ul>
                <pre v-else-if="b.code" class="idehelp-code"><code>{{ b.code }}</code></pre>
                <div v-else-if="b.tip" class="idehelp-tip">
                  <span v-html="BL.icon('info', 12)"></span>{{ b.tip }}
                </div>
              </template>
            </article>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
/**
 * 内置文档中心 (P9 · 文档 模块9-6)
 * 本体开发规范 / 函数最佳实践 / 快捷键speed / 常见问题,支持全文过滤。
 * 内容内嵌而不是外链:IDE 是全屏页面,跳出去看文档会打断编辑上下文。
 */
import { ref, computed } from 'vue'
import { BL } from '@/lib/bl.js'

const props = defineProps({
  open: { type: Boolean, default: false },
  theme: { type: String, default: 'dark' },
})
const emit = defineEmits(['update:open'])

const DOCS = [
  {
    k: 'start', title: '快速上手', icon: 'rocket', blocks: [
      { p: '本 IDE 用于编写平台函数的实现代码。文件来自服务端的 Git 工作区,与函数元数据(名称、参数、运行配置)分开管理。' },
      { h: '典型流程' },
      { ul: [
        '从左侧「项目文件树」打开文件,或用 Ctrl+P 按文件名跳转',
        '编辑后 Ctrl+S 保存 —— 保存 = 在服务端本地提交一次,不会自动推送',
        '在「版本变更」面板确认待推送数,点「推送」一次性推到远程',
        'Ctrl+F5 以非调试模式运行;F9 打断点后按 F5 启动调试',
      ] },
      { tip: '函数的参数、类型、运行配置在平台的「函数详情页」维护,不在这里改。' },
    ],
  },
  {
    k: 'ontology', title: '本体开发规范', icon: 'cube', blocks: [
      { p: '函数与本体对象强绑定:入参 / 返回值声明的本体类型,决定了平台能否做类型校验与血缘追溯。' },
      { h: '类型书写' },
      { ul: [
        '本体对象类型写成「[命名空间] 类名」,例如 [w_wtr_hyd] HydrologyStation',
        '基础类型只用 string / number / boolean / any 四种',
        '返回复合结果时定义一个结果类型,不要返回裸 dict / 匿名对象',
      ] },
      { h: '资源导入' },
      { p: '在左侧「资源导入」面板导入本体对象后,系统会自动生成 TypeScript 类型定义注入编辑器,写代码时就有属性补全与类型校验。' },
      { tip: '导入清单按分支隔离存在本地,不写进代码仓 —— 避免每导入一个资源就产生一个提交。' },
    ],
  },
  {
    k: 'practice', title: '函数最佳实践', icon: 'award', blocks: [
      { h: '函数类型与装饰器' },
      { ul: [
        '常规 / 聚合 / 衍生 / 时序函数 → @Function()',
        '动作函数(会改本体数据)→ @OntologyEditFunction(),默认开启编辑事务、原子提交',
      ] },
      { h: '运行配置' },
      { ul: [
        '超时时间按最坏情况设,别贴着平均耗时设,否则偶发慢查询会被误杀',
        '重试只对「幂等」函数开;写操作开重试会产生重复数据',
        '结果缓存适合入参空间小、数据变化慢的查询;实时性要求高的别开',
      ] },
      { h: '环境变量' },
      { p: '密钥类变量务必勾选「加密存储」,值在详情页会以 •••••• 展示。' },
    ],
  },
  {
    k: 'shortcuts', title: '快捷键速查', icon: 'keyboard', blocks: [
      { h: '编辑' },
      { ul: ['Ctrl+S 保存', 'Ctrl+F / Ctrl+H 查找 / 替换', 'Ctrl+/ 行注释', 'Shift+Alt+A 块注释',
             'Ctrl+D 添加下一个匹配项', 'Ctrl+Shift+L 选择所有匹配项', 'Shift+Alt+F 格式化'] },
      { h: '运行与调试' },
      { ul: ['Ctrl+F5 非调试运行', 'F5 启动调试 / 继续', 'Shift+F5 停止调试', 'F9 切换断点',
             'Ctrl+F9 新建条件断点', 'F10 逐过程', 'F11 单步进入', 'Shift+F11 单步跳出'] },
      { h: '视图' },
      { ul: ['Ctrl+Shift+P 命令面板', 'Ctrl+P 按文件名跳转', 'Ctrl+J 显示/隐藏底部面板',
             'Ctrl+B 显示/隐藏侧边栏', 'Ctrl+= / Ctrl+- 缩放'] },
      { tip: '所有快捷键都可以在「命令面板 → 自定义快捷键」里改,带冲突检测。' },
    ],
  },
  {
    k: 'faq', title: '常见问题', icon: 'help', blocks: [
      { h: '保存了但远程仓库没变化?' },
      { p: '这是预期行为。保存只在服务端做本地提交,推送是显式动作 —— 去左侧「版本变更」面板点「推送」。状态栏的 ↑N 就是待推送提交数。' },
      { h: '切换分支提示「工作区有未提交的改动」' },
      { p: '先把改动保存(提交)掉再切。系统不会自动 stash,以免你的改动去向变得难以追踪。' },
      { h: 'TypeScript 文件运行失败' },
      { p: '服务器默认用 npx --yes tsx 跑 .ts,首次会联网下载。也可以在底部「终端」里执行 npm i -D tsx 装到代码仓里。' },
      { h: '断点的几种形态' },
      { ul: [
        '实心红点 = 普通断点;白心红点 = 条件断点 / 命中次数断点;橙色菱形 = 日志点(不中断)',
        '灰点 = 已禁用(在调试面板取消勾选);空心 = 调试器未确认',
        '条件断点 / 命中次数 / 日志点在「运行」菜单或命令面板里新建, 打在光标所在行',
      ] },
      { h: '断点打了不生效 / 显示空心' },
      { ul: [
        '空心表示调试器未确认该断点:多半是行号上没有可执行语句(空行、注释、纯声明)',
        '目前只支持 Python 断点调试;TypeScript 需要额外的 js-debug 适配器',
        '改完代码要先保存再启动调试,否则调的是磁盘上的旧代码',
      ] },
      { h: '「问题」面板对 Python 文件永远是空的' },
      { p: '编辑器自带的语言服务只覆盖 TypeScript / JavaScript。Python 的语法检查要接服务端 pylint,尚未落地。' },
    ],
  },
]

const q = ref('')
const active = ref('start')

function textOf(d) {
  return [d.title, ...d.blocks.flatMap(b => [b.h, b.p, b.tip, b.code, ...(b.ul || [])])]
    .filter(Boolean).join(' ').toLowerCase()
}
const filtered = computed(() => {
  const k = q.value.trim().toLowerCase()
  return k ? DOCS.filter(d => textOf(d).includes(k)) : DOCS
})
const current = computed(() =>
  filtered.value.find(d => d.k === active.value) || filtered.value[0] || DOCS[0])

function close() { emit('update:open', false) }
</script>

<style scoped>
.idehelp-mask {
  position: fixed; inset: 0; z-index: 1500;
  background: rgba(0, 0, 0, .45);
  display: flex; align-items: center; justify-content: center;
}
.idehelp {
  width: 840px; max-width: calc(100vw - 40px); height: 72vh;
  background: var(--ide-bg-2); color: var(--ide-text);
  border: 1px solid var(--ide-border); border-radius: 6px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, .5);
  display: flex; flex-direction: column; overflow: hidden;
}
.idehelp-hd {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; border-bottom: 1px solid var(--ide-border);
}
.idehelp-title { font-size: 14px; font-weight: 600; color: var(--ide-text-strong); }
.idehelp-search { position: relative; width: 220px; }
.idehelp-search-ic { position: absolute; left: 6px; top: 50%; transform: translateY(-50%); color: var(--ide-text-dim); }
.idehelp-input {
  width: 100%; height: 26px; padding: 0 6px 0 22px; border-radius: 3px;
  background: var(--ide-bg); border: 1px solid var(--ide-border);
  color: var(--ide-text); font-size: 12px; outline: none;
}
.idehelp-x {
  width: 24px; height: 24px; border: 0; border-radius: 3px; cursor: pointer;
  background: transparent; color: var(--ide-text-dim);
  display: inline-flex; align-items: center; justify-content: center;
}
.idehelp-x:hover { background: var(--ide-hover); color: var(--ide-text-strong); }

.idehelp-body { flex: 1; min-height: 0; display: flex; }
.idehelp-nav {
  width: 190px; flex-shrink: 0; overflow: auto;
  border-right: 1px solid var(--ide-border); padding: 8px 0;
}
.idehelp-nav-item {
  display: flex; align-items: center; gap: 7px;
  padding: 8px 12px; font-size: 12.5px; color: var(--ide-text-dim); cursor: pointer;
  border-left: 3px solid transparent;
}
.idehelp-nav-item:hover { background: var(--ide-hover); color: var(--ide-text); }
.idehelp-nav-item.is-on { border-left-color: var(--ide-blue); background: var(--ide-hover); color: var(--ide-text-strong); }
.idehelp-empty { padding: 16px 12px; font-size: 12px; color: var(--ide-text-dim); }

.idehelp-content { flex: 1; min-width: 0; overflow: auto; padding: 16px 22px 28px; }
.idehelp-h2 { margin: 0 0 12px; font-size: 17px; font-weight: 600; color: var(--ide-text-strong); }
.idehelp-h3 {
  margin: 18px 0 8px; font-size: 13px; font-weight: 600; color: var(--ide-text-strong);
  padding-left: 8px; border-left: 3px solid var(--ide-blue);
}
.idehelp-p { margin: 0 0 10px; font-size: 12.5px; line-height: 21px; color: var(--ide-text); }
.idehelp-ul { margin: 0 0 10px; padding-left: 20px; }
.idehelp-ul li { font-size: 12.5px; line-height: 22px; color: var(--ide-text); }
.idehelp-code {
  margin: 0 0 10px; padding: 10px 12px; border-radius: 4px; overflow: auto;
  background: var(--ide-bg); border: 1px solid var(--ide-border);
  font-family: Consolas, Monaco, monospace; font-size: 12px; color: var(--ide-text);
}
.idehelp-tip {
  display: flex; align-items: flex-start; gap: 6px;
  margin: 10px 0; padding: 8px 10px; border-radius: 4px;
  background: rgba(0, 120, 212, .12); color: var(--ide-blue); font-size: 12px; line-height: 19px;
}
.bl-grow { flex: 1; }
.idehelp-fade-enter-active, .idehelp-fade-leave-active { transition: opacity .15s; }
.idehelp-fade-enter-from, .idehelp-fade-leave-to { opacity: 0; }
</style>
