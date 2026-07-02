import { exploreDesignApi } from '@/api'

/* 实例探索 — 「看板/设计」持久化(后端入库, 替代原 localStorage)
 * - 默认看板:每对象类型一份(name=''), config 即 maker 的 layoutConfig, 由 maker「保存」写回。
 * - 命名设计:name 非空, config 为整个设计对象(含 meta + layoutConfig), 由「另存为」创建。 */

// 后端行 → 前端命名设计对象:展开 config(含 meta/layoutConfig) + 带 id/savedAt
function toDesign (row) {
  if (!row) return null
  const cfg = (row.config && typeof row.config === 'object') ? row.config : {}
  return { ...cfg, id: row.id, name: row.name || cfg.name || '', savedAt: row.updated_at || row.updatedAt }
}

export function useDesigns () {
  /* 某对象类型下的命名设计列表 */
  const listFor = async (classId) => {
    if (!classId) return []
    const rows = await exploreDesignApi.listNamed(classId)
    return (rows || []).map(toDesign)
  }

  /* 新建命名设计(design 为整个设计对象);返回带 id 的对象 */
  const save = async (design) => {
    const row = await exploreDesignApi.create({
      classId: design.classId, name: design.name, kind: design.kind || 'query', config: design
    })
    return toDesign(row)
  }

  /* 原地更新命名设计(保存到当前列表);返回带 id 的对象 */
  const update = async (id, design) => {
    const row = await exploreDesignApi.update(id, { name: design.name, kind: design.kind || 'query', config: design })
    return toDesign(row)
  }

  const remove = (id) => exploreDesignApi.remove(id)

  /* 默认看板:config 直接是 maker 的 layoutConfig(无则 null) */
  const getDefault = async (classId) => {
    if (!classId) return null
    const row = await exploreDesignApi.getDefault(classId)
    return row && row.config ? row.config : null
  }
  const saveDefault = (classId, layoutConfig, kind = 'query') =>
    exploreDesignApi.saveDefault(classId, layoutConfig, kind)

  return { listFor, save, update, remove, getDefault, saveDefault }
}
