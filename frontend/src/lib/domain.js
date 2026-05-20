/**
 * 命名空间 → 图标 + 颜色 统一映射
 * 所有界面（侧栏领域选择器、分类树、卡片等）都用这里的同一份配置，保证视觉一致
 */
export const DOMAIN_PROFILE = {
  // 系统层
  w_root:    { icon: 'cube',    color: '#1D2129' },
  w_common:  { icon: 'grid',    color: '#86909C' },
  // 行业
  w_wtr:     { icon: 'droplet', color: '#165DFF' },  // 水利
  w_tfc:     { icon: 'car',     color: '#FF7D00' },  // 交通
  w_eme:     { icon: 'shield',  color: '#F53F3F' },  // 应急
  w_env:     { icon: 'leaf',    color: '#00B42A' },  // 环保
  w_for:     { icon: 'tree',    color: '#13C2C2' },  // 林业
  w_agr:     { icon: 'wheat',   color: '#722ED1' },  // 农业
  // 水利子领域
  w_wtr_sc:  { icon: 'dam',     color: '#165DFF' },  // 水土保持
  w_wtr_wr:  { icon: 'droplet', color: '#00B42A' },  // 水资源
  w_wtr_hyd: { icon: 'wave',    color: '#FF7D00' },  // 水文
  w_wtr_wae: { icon: 'droplet', color: '#722ED1' },  // 水环境
  w_wtr_wec: { icon: 'leaf',    color: '#13C2C2' },  // 水生态
  w_wtr_eng: { icon: 'factory', color: '#EB2F96' },  // 水利工程
  w_wtr_fld: { icon: 'shield',  color: '#F53F3F' },  // 防洪减灾
  w_wtr_irr: { icon: 'wheat',   color: '#FADB14' },  // 农田灌溉
  w_wtr_sup: { icon: 'droplet', color: '#165DFF' },  // 城乡供水
  w_wtr_drn: { icon: 'wave',    color: '#13C2C2' },  // 排涝排水
  w_wtr_saf: { icon: 'shield',  color: '#FF7D00' },  // 水利安全
  w_wtr_mon: { icon: 'station', color: '#722ED1' },  // 水文监测
  w_wtr_reg: { icon: 'sliders', color: '#1D2129' },  // 水利监管
  w_wtr_stat:{ icon: 'grid',    color: '#86909C' }   // 水利统计
}

const FALLBACK_PALETTE = ['#165DFF','#00B42A','#722ED1','#FF7D00','#EB2F96','#13C2C2','#FADB14','#F53F3F']

export function domainProfile(code) {
  if (code && DOMAIN_PROFILE[code]) return DOMAIN_PROFILE[code]
  let h = 0
  for (const ch of String(code || '')) h = (h * 31 + ch.charCodeAt(0)) >>> 0
  return { icon: 'folder', color: FALLBACK_PALETTE[h % FALLBACK_PALETTE.length] }
}
export const domainIcon  = (code) => domainProfile(code).icon
export const domainColor = (code) => domainProfile(code).color

/**
 * 取分类节点的 icon/color：
 *   1) 优先用数据库的 icon（业务方手工定制的不要被覆盖）
 *      但若数据库填的是 'folder' / 'industry' 这种默认占位，则走 ns 映射
 *   2) color 同理
 */
export function nodeProfile(node) {
  if (!node) return { icon: 'folder', color: '#86909C' }
  const p = domainProfile(node.nsCode)
  const dbIcon = node.icon
  const isPlaceholder = !dbIcon || dbIcon === 'folder' || dbIcon === 'industry'
  return {
    icon: isPlaceholder ? p.icon : dbIcon,
    color: node.color || p.color
  }
}
