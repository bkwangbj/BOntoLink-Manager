/**
 * useDraggableModal — 给"大弹框"统一接入 拖动 / 最大化-还原 / 八向缩放 的能力。
 *
 * 使用约定:
 *   1. 模板里需要三处接线:
 *      - `<div class="...-modal" :style="dm.modalStyle.value" :class="{ 'dm-max': dm.state.maximized }">`
 *      - 标题栏: `@mousedown="dm.startDrag"` (按钮上自己加 @mousedown.stop 防误拖)
 *      - 资源缩放把手 (建议放最外层 modal 节点的最末尾):
 *          `<DraggableHandles v-if="!dm.state.maximized" :on="dm.startResize" />`
 *        或者直接展开 8 个 div (见 ValueTypePickerModal 范例)
 *   2. 在标题栏的关闭按钮旁加一个最大化按钮:
 *        `<button @click="dm.toggleMax">…</button>`
 *   3. open 变 true 时调用 `dm.reset()`,让弹框回到居中默认尺寸。
 *
 * 关键设计:
 *   - centered 模式: 不写死 left/top, 让外层 mask 用 flex 居中 (与原样式一致)。
 *   - free 模式: 第一次拖/拉时把当前 BoundingClientRect 锁定为基线, 改为 `position: fixed` + 显式 x/y/w/h。
 *   - maximized 模式: 撑满视口, 关闭/再次点最大化时回到之前的 free 快照, 若无快照则回 centered。
 */
import { reactive, computed, onBeforeUnmount } from 'vue'

export function useDraggableModal(opts = {}) {
  const minW = opts.minWidth || 480
  const minH = opts.minHeight || 320

  const state = reactive({
    free: false,          // 是否已脱离 centered (拖过/拉过)
    maximized: false,
    x: 0, y: 0, w: 0, h: 0,
    snapshot: null,       // 最大化前的快照 { free, x, y, w, h }
  })

  /* 切换到 free 模式: 把当前 DOM 的实际位置/尺寸固化下来 */
  function ensureFree(ev) {
    if (state.free) return
    const modalEl = (ev?.currentTarget || ev?.target)?.closest?.('[data-dm-root]')
                 || document.querySelector('[data-dm-root]')
    if (!modalEl) { state.free = true; return }
    const r = modalEl.getBoundingClientRect()
    state.x = r.left
    state.y = r.top
    state.w = r.width
    state.h = r.height
    state.free = true
  }

  /* —— 拖动 —— */
  function startDrag(ev) {
    if (state.maximized) return
    if (ev.button !== 0) return
    // 点到按钮 / input / 链接 时不拖
    const t = ev.target
    if (t.closest && t.closest('button, input, select, textarea, a, [data-dm-no-drag]')) return
    ensureFree(ev)
    ev.preventDefault()
    const sx = ev.clientX, sy = ev.clientY
    const bx = state.x, by = state.y
    function move(e) {
      let nx = bx + (e.clientX - sx)
      let ny = by + (e.clientY - sy)
      // clamp: 保留至少 80px 在视口内, 标题栏不被拖出顶部
      nx = Math.max(-state.w + 120, Math.min(window.innerWidth - 120, nx))
      ny = Math.max(0, Math.min(window.innerHeight - 50, ny))
      state.x = nx; state.y = ny
    }
    function up() {
      window.removeEventListener('mousemove', move)
      window.removeEventListener('mouseup', up)
      document.body.style.userSelect = ''
    }
    document.body.style.userSelect = 'none'
    window.addEventListener('mousemove', move)
    window.addEventListener('mouseup', up)
  }

  /* —— 八向缩放 —— dir ∈ {n,s,e,w,ne,nw,se,sw} */
  function startResize(dir, ev) {
    if (state.maximized) return
    if (ev.button !== 0) return
    ev.preventDefault(); ev.stopPropagation()
    ensureFree(ev)
    const sx = ev.clientX, sy = ev.clientY
    const bx = state.x, by = state.y, bw = state.w, bh = state.h
    function move(e) {
      const dx = e.clientX - sx
      const dy = e.clientY - sy
      if (dir.includes('e')) state.w = Math.max(minW, bw + dx)
      if (dir.includes('s')) state.h = Math.max(minH, bh + dy)
      if (dir.includes('w')) {
        const nw = Math.max(minW, bw - dx)
        state.x = bx + (bw - nw); state.w = nw
      }
      if (dir.includes('n')) {
        const nh = Math.max(minH, bh - dy)
        state.y = by + (bh - nh); state.h = nh
      }
    }
    function up() {
      window.removeEventListener('mousemove', move)
      window.removeEventListener('mouseup', up)
      document.body.style.userSelect = ''
    }
    document.body.style.userSelect = 'none'
    window.addEventListener('mousemove', move)
    window.addEventListener('mouseup', up)
  }

  /* —— 最大化 / 还原 —— */
  function toggleMax() {
    if (state.maximized) {
      // 还原
      if (state.snapshot) {
        state.free = state.snapshot.free
        state.x = state.snapshot.x; state.y = state.snapshot.y
        state.w = state.snapshot.w; state.h = state.snapshot.h
      } else {
        state.free = false
      }
      state.snapshot = null
      state.maximized = false
    } else {
      state.snapshot = { free: state.free, x: state.x, y: state.y, w: state.w, h: state.h }
      state.maximized = true
    }
  }

  /* 关闭时调用: 还原默认 */
  function reset() {
    state.free = false
    state.maximized = false
    state.snapshot = null
    state.x = state.y = state.w = state.h = 0
  }

  /* 给 .xxx-modal 直接绑定的 style */
  const modalStyle = computed(() => {
    if (state.maximized) {
      return {
        position: 'fixed', left: '0', top: '0',
        width: '100vw', height: '100vh',
        maxWidth: '100vw', maxHeight: '100vh',
        borderRadius: '0'
      }
    }
    if (state.free) {
      return {
        position: 'fixed',
        left: state.x + 'px', top: state.y + 'px',
        width: state.w + 'px', height: state.h + 'px',
        maxWidth: 'none', maxHeight: 'none', margin: 0
      }
    }
    return {} // centered: 由外层 flex 居中
  })

  onBeforeUnmount(() => {
    document.body.style.userSelect = ''
  })

  return { state, startDrag, startResize, toggleMax, reset, modalStyle }
}
