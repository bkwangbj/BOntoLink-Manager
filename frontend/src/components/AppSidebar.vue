<template>
  <aside :class="['sidebar', collapsed && 'is-collapsed']">
    <!-- 行业领域下拉（最高优先级控件）：点击打开「常用领域选择」弹窗 -->
    <div class="domain-picker" @click="app.openDomainPicker()" :title="hoverTooltip">
      <!-- 已选时显示聚合 icon；未选显示默认 cube -->
      <div class="dp-icon"
           v-html="BL.icon(headIcon.icon, 28, headIcon.color)"></div>
      <div class="dp-text" v-show="!collapsed">
        <div class="dp-label">领域</div>
        <div class="dp-name" :style="{ color: headIcon.color }">
          <template v-if="app.selectedDomains.length">{{ aggregateText }}</template>
          <template v-else>请选择常用领域</template>
        </div>
      </div>
      <span class="dp-arrow" v-show="!collapsed" v-html="BL.icon('chevronDown', 12)"></span>
    </div>

    <!-- 导航 -->
    <nav class="nav">
      <template v-for="group in menus" :key="group.key">
        <div class="nav-group">
          <div v-if="!collapsed && group.title" class="nav-group-title" @click="toggleGroup(group.key)">
            <span>{{ group.title }}</span>
            <span class="nav-group-arrow" v-html="BL.icon(isGroupOpen(group.key) ? 'chevronDown' : 'chevronRight', 10)"></span>
          </div>
          <div v-show="collapsed || isGroupOpen(group.key)">
            <div v-for="item in group.items" :key="item.path">
              <NavItem :item="item" :collapsed="collapsed" />
            </div>
          </div>
        </div>
      </template>
    </nav>

    <!-- 折叠/展开 + 用户 -->
    <div class="footer" v-click-outside="() => userMenuOpen=false">
      <button class="bl-btn bl-btn-text bl-btn-icon collapse-btn" :title="collapsed ? '展开侧栏' : '收起侧栏'" @click="app.toggleSidebar()">
        <span v-html="BL.icon(collapsed ? 'chevronRight' : 'chevronLeft', 14)"></span>
      </button>
      <div class="user-row" :class="userMenuOpen && 'is-open'" @click="userMenuOpen = !userMenuOpen">
        <div class="avatar">G</div>
        <div class="user-info" v-show="!collapsed">
          <div class="user-name">Gary</div>
          <div class="user-role">本体管理员</div>
        </div>
        <span v-show="!collapsed" class="user-arrow" v-html="BL.icon('chevronRight', 12)"></span>
      </div>

      <transition name="fade">
        <div v-if="userMenuOpen" class="bl-popover user-menu" @click.stop>
          <div class="user-card">
            <div class="avatar avatar-lg">G</div>
            <div class="user-info">
              <div class="user-name">Gary</div>
              <div class="user-role">
                <span class="dot-online"></span>本体管理员
              </div>
            </div>
          </div>
          <div class="bl-menu-divider"></div>
          <div class="user-mi" @click="openSettings">
            <span class="user-mi-ic" v-html="BL.icon('sliders', 14)"></span>
            <span class="bl-grow">设置</span>
            <span class="user-mi-arrow" v-html="BL.icon('chevronRight', 12)"></span>
          </div>
          <div class="user-mi" @click="openAbout">
            <span class="user-mi-ic" v-html="BL.icon('cube', 14)"></span>
            <span class="bl-grow">关于 BOntoLink 博智联</span>
            <span class="user-mi-arrow" v-html="BL.icon('chevronRight', 12)"></span>
          </div>
          <div class="user-mi" @click="openHelp">
            <span class="user-mi-ic" v-html="BL.icon('help', 14)"></span>
            <span class="bl-grow">帮助与反馈</span>
            <span class="user-mi-arrow" v-html="BL.icon('chevronRight', 12)"></span>
          </div>
          <div class="bl-menu-divider"></div>
          <div class="user-mi is-danger" @click="doLogout">
            <span class="user-mi-ic" v-html="BL.icon('logout', 14)"></span>
            <span class="bl-grow">退出登录</span>
          </div>
        </div>
      </transition>
    </div>
  </aside>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import NavItem from './NavItem.vue'
import { BL } from '@/lib/bl.js'
import { domainIcon, domainColor } from '@/lib/domain.js'
import { useAppStore } from '@/stores/app.js'
import { namespaceApi } from '@/api'

const app = useAppStore()
const collapsed = computed(() => app.sidebarCollapsed)

const namespaces = ref([])
const userMenuOpen = ref(false)

// 已选领域聚合显示：「行业|领域1/领域2; 行业|领域...」
const aggregateText = computed(() => {
  const groups = {}
  for (const d of app.selectedDomains) {
    const k = d.industryLabel || '—'
    if (!groups[k]) groups[k] = []
    groups[k].push(d.label)
  }
  return Object.entries(groups).map(([k, vs]) => `${k} | ${vs.join('/')}`).join('; ')
})
const hoverTooltip = computed(() => {
  if (!app.selectedDomains.length) return '点击选择常用领域'
  return aggregateText.value
})
// 聚合显示用的图标/颜色：取第一个已选的 profile；未选时为默认 cube
const headIcon = computed(() => {
  const first = app.selectedDomains[0]
  if (!first) return { icon: 'cube', color: '#86909C' }
  return { icon: domainIcon(first.nsCode), color: domainColor(first.nsCode) }
})

function openSettings() { userMenuOpen.value = false; app.openSettings() }
function openAbout()    { userMenuOpen.value = false; BL.info('BOntoLink 博智联 v1.0.0 · 动态本体管理系统') }
function openHelp()     { userMenuOpen.value = false; BL.info('帮助与反馈：⌘K 全局搜索 · ? 快捷键面板') }
async function doLogout() {
  userMenuOpen.value = false
  const ok = await BL.confirm({ title: '退出登录', content: '确认退出当前会话？', danger: true, okText: '退出' })
  if (ok) BL.success('已退出（演示）')
}

// 大类折叠状态（持久化到 localStorage）
const NAV_KEY = 'bl-nav-collapsed-groups'
const collapsedGroups = ref(new Set(JSON.parse(localStorage.getItem(NAV_KEY) || '[]')))
function isGroupOpen(k) { return !collapsedGroups.value.has(k) }
function toggleGroup(k) {
  const s = new Set(collapsedGroups.value)
  s.has(k) ? s.delete(k) : s.add(k)
  collapsedGroups.value = s
  localStorage.setItem(NAV_KEY, JSON.stringify([...s]))
}

const menus = [
  { key: 'workspace', title: '工作区', items: [
    { path: '/workspace/discover',   icon: 'grid',     label: '总览',     en: 'Overview' },
    { path: '/workspace/graph',      icon: 'network',  label: '图谱',     en: 'Graph' },
    { path: '/workspace/instances',  icon: 'database', label: '实例',     en: 'Object instances' }
  ]},
  { key: 'resources', title: '资源', items: [
    { path: '/resources/object-types', icon: 'cube',    label: '对象类型', en: 'Object types' },
    { path: '/resources/link-types',   icon: 'link',    label: '链接', en: 'Links' },
    { path: '/resources/action-types', icon: 'zap',     label: '动作类型', en: 'Action types' },
    { path: '/resources/value-types',  icon: 'list',    label: '值类型',   en: 'Value types' },
    { path: '/resources/enum-types',   icon: 'layers',  label: '枚举类型', en: 'Enum types' },
    { path: '/resources/shared-props', icon: 'share',   label: '共享属性', en: 'Shared props' },
    { path: '/resources/functions',    icon: 'branch',  label: '函数',     en: 'Functions' },
    { path: '/resources/interfaces',   icon: 'station', label: '接口',     en: 'Interfaces' },
    { path: '/resources/datasources',  icon: 'database',label: '数据源',   en: 'Datasources' }
  ]},
  { key: 'tools', title: '辅助工具', items: [
    { path: '/tools/ai',            icon: 'ai',       label: 'AI 助手',   en: 'AI Assistant' },
    { path: '/tools/import-export', icon: 'upload',   label: '导入导出',  en: 'Import & Export' }
  ]},
  { key: 'config', title: '系统配置', items: [
    { path: '/config/category',    icon: 'folder', label: '行业分类管理', en: 'Category' },
    { path: '/config/type-classes', icon: 'tag',    label: '类型类',       en: 'Type classes' },
    { path: '/config/security',     icon: 'user',   label: '权限安全',     en: 'Security' }
  ]}
]

onMounted(async () => {
  // 仍预拉一次命名空间列表（供其他模块直接使用）
  try { namespaces.value = await namespaceApi.list() } catch {}
})

// 主界面"行业领域选择"区显示聚合，hover 时浏览器原生 title 显示完整详情；
// 这里把 namespaces 保留以便未来扩展，但选择交互完全交给 DomainSelectorModal

const vClickOutside = {
  mounted(el, binding) {
    el.__handler__ = (e) => { if (!el.contains(e.target)) binding.value() }
    document.addEventListener('click', el.__handler__)
  },
  unmounted(el) { document.removeEventListener('click', el.__handler__) }
}
</script>

<script>
export default {
  directives: {
    'click-outside': {
      mounted(el, binding) {
        el.__handler__ = (e) => { if (!el.contains(e.target)) binding.value() }
        document.addEventListener('click', el.__handler__)
      },
      unmounted(el) { document.removeEventListener('click', el.__handler__) }
    }
  }
}
</script>

<style scoped>
.sidebar {
  width: var(--bl-sidebar-w);
  background: var(--bl-bg-1);
  border-right: 1px solid var(--bl-border);
  display: flex; flex-direction: column;
  transition: width .18s ease;
  flex-shrink: 0;
  overflow: hidden;
}
.sidebar.is-collapsed { width: var(--bl-sidebar-w-collapsed); overflow: visible; }
.sidebar.is-collapsed .nav { overflow-x: visible; }

.domain-picker {
  margin: 8px;
  padding: 6px 10px;
  background: var(--bl-bg-2);
  border-radius: var(--bl-radius-3);
  display: flex; align-items: center; gap: 6px;
  cursor: pointer; position: relative;
  border: 1px solid transparent;
}
.domain-picker:hover { border-color: var(--bl-primary-border); }
.dp-icon {
  width: 28px; height: 28px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  line-height: 0;
}
.dp-text { flex: 1; min-width: 0; }
.dp-label { font-size: var(--bl-fs-11); color: var(--bl-text-3); }
.dp-name { font-size: var(--bl-fs-13); font-weight: 600; color: var(--bl-text-1); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dp-arrow { color: var(--bl-text-3); }

.dp-menu { top: 56px; left: 12px; right: 12px; width: auto; max-width: none; }
.dp-search { padding: 6px; border-bottom: 1px solid var(--bl-divider); }
.dp-list { max-height: 360px; overflow: auto; padding: 6px; }
.dp-li-ic { width: 18px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }

.sidebar.is-collapsed .domain-picker { padding: 8px; justify-content: center; }
.sidebar.is-collapsed .domain-picker > .dp-text,
.sidebar.is-collapsed .domain-picker > .dp-arrow { display: none; }

.nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 4px 8px 8px;
  /* 完全隐藏滚动条：内容用全宽，靠鼠标滚轮滚动 */
  scrollbar-width: none;       /* Firefox */
  -ms-overflow-style: none;    /* IE / Edge legacy */
}
.nav::-webkit-scrollbar { width: 0; height: 0; display: none; }  /* Chromium / Safari */
.nav-group { padding-top: 6px; margin-top: 6px; border-top: 1px solid var(--bl-border); }
.nav-group:first-child { border-top: 0; margin-top: 0; padding-top: 0; }
.sidebar.is-collapsed .nav-group { margin: 4px 0px 0; padding-top: 4px; }
.nav-group-title {
  padding: 4px 8px 4px 12px;
  font-size: var(--bl-fs-11);
  color: var(--bl-text-3);
  letter-spacing: .5px;
  display: flex; align-items: center; justify-content: space-between;
  cursor: pointer;
  border-radius: var(--bl-radius-2);
  user-select: none;
}
.nav-group-title:hover { color: var(--bl-text-1); background: var(--bl-bg-hover); }
.nav-group-arrow { color: var(--bl-text-3); display: inline-flex; }
.nav-group-title:hover .nav-group-arrow { color: var(--bl-primary); }

.footer {
  border-top: 1px solid var(--bl-divider);
  padding: 8px; display: flex; align-items: center; gap: 8px;
  position: relative;
}
.collapse-btn { flex-shrink: 0; }

/* 折叠态：纵向堆叠 - 用户头像在上，折叠按钮在下 */
.sidebar.is-collapsed .footer {
  flex-direction: column-reverse;
  align-items: stretch;
  gap: 6px;
  padding: 8px 6px;
}
.sidebar.is-collapsed .user-row {
  flex: 0 0 auto;
  justify-content: center;
  padding: 6px 4px;
}
.sidebar.is-collapsed .collapse-btn {
  align-self: center;
}
.user-row {
  display: flex; align-items: center; gap: 8px;
  flex: 1; min-width: 0;
  padding: 4px 6px; border-radius: var(--bl-radius-2);
  cursor: pointer; transition: background-color .15s;
}
.user-row:hover, .user-row.is-open { background: var(--bl-bg-hover); }
.avatar { width: 28px; height: 28px; border-radius: 50%; background: var(--bl-primary); color: #fff; font-weight: 600; display: flex; align-items: center; justify-content: center; font-size: 12px; flex-shrink: 0; }
.avatar-lg { width: 40px; height: 40px; font-size: 16px; }
.user-info { display: flex; flex-direction: column; min-width: 0; flex: 1; }
.user-name { font-size: var(--bl-fs-13); font-weight: 500; }
.user-role { font-size: var(--bl-fs-11); color: var(--bl-text-3); display: inline-flex; align-items: center; gap: 4px; }
.user-arrow { color: var(--bl-text-3); flex-shrink: 0; transform: rotate(-90deg); }
.user-row.is-open .user-arrow { transform: rotate(90deg); }

.user-menu {
  position: absolute;
  left: 8px;
  right: 8px;
  bottom: 100%;
  margin-bottom: 6px;
  width: auto;
  max-width: none;
  padding: 6px;
  z-index: 100;
}
/* 折叠态：弹出菜单从侧栏右侧浮出，避免被压扁 */
.sidebar.is-collapsed .user-menu {
  left: 100%;
  right: auto;
  bottom: 0;
  margin-left: 6px;
  margin-bottom: 0;
  width: 260px;
}
.user-card {
  display: flex; align-items: center; gap: 10px;
  padding: 10px;
  background: var(--bl-primary-soft);
  border-radius: var(--bl-radius-2);
  margin-bottom: 4px;
}
.user-card .avatar-lg { background: linear-gradient(135deg, var(--bl-primary), #4080FF); }
.user-mi {
  display: flex; align-items: center; gap: 10px;
  padding: 9px 10px;
  font-size: var(--bl-fs-13);
  color: var(--bl-text-1);
  border-radius: var(--bl-radius-2);
  cursor: pointer;
}
.user-mi:hover { background: var(--bl-bg-hover); }
.user-mi.is-danger { color: var(--bl-danger); }
.user-mi.is-danger:hover { background: var(--bl-danger-soft); }
.user-mi-ic { width: 16px; display: inline-flex; align-items: center; justify-content: center; color: var(--bl-text-2); flex-shrink: 0; }
.user-mi.is-danger .user-mi-ic { color: var(--bl-danger); }
.user-mi-arrow { color: var(--bl-text-3); flex-shrink: 0; }

.dot-online {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--bl-success);
}

.fade-enter-active, .fade-leave-active { transition: opacity .12s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
