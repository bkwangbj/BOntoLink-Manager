/**
 * BOntoLink 全局 UI 工具：BL.toast / BL.confirm / BL.icon
 * 遵循 UI 规范：禁止原生 alert/confirm；toast/confirm/drawer 统一走 BL.*
 */
import { h, render } from 'vue'
import * as antdIcons from '@ant-design/icons-svg'

/* ---------- ICON ---------- */
// 仅描边风格，与规范保持一致。补齐业务图标供 BL.icon(name) 调用
const ICONS = {
  /* —— 通用动作 —— */
  search: 'M11 19a8 8 0 1 1 5.29-14.06A8 8 0 0 1 11 19Zm10 2-4.35-4.35',
  plus:   'M12 5v14M5 12h14',
  minus:  'M5 12h14',
  edit:   'M17 3l4 4-11 11H6v-4z M14 6l4 4',
  trash:  'M3 6h18 M8 6V4h8v2 M6 6l1 14h10l1-14',
  more:   'M5 12h.01 M12 12h.01 M19 12h.01',
  moreV:  'M12 5h.01 M12 12h.01 M12 19h.01',
  /* 拖拽手柄: 6 个点 (2 列 × 3 行), 行业事实标准的 "drag handle" 视觉 */
  grip:   'M9 5h.01 M15 5h.01 M9 12h.01 M15 12h.01 M9 19h.01 M15 19h.01',
  /* 选择模式: 鼠标指针 (上左斜的箭头) */
  cursor: 'M3 3l7.07 16.97 2.51-7.39 7.39-2.51z M13 13l6 6',
  /* 移动 / 四向箭头 (适配最优视角) */
  move:   'M5 9l-3 3 3 3 M9 5l3-3 3 3 M15 19l-3 3-3-3 M19 9l3 3-3 3 M2 12h20 M12 2v20',
  /* 放大 / 缩小 (带 +/- 的放大镜) */
  zoomIn:  'M11 19a8 8 0 1 1 5.29-14.06A8 8 0 0 1 11 19Zm10 2-4.35-4.35 M11 8v6 M8 11h6',
  zoomOut: 'M11 19a8 8 0 1 1 5.29-14.06A8 8 0 0 1 11 19Zm10 2-4.35-4.35 M8 11h6',
  check:  'M5 13l4 4L19 7',
  x:      'M6 6l12 12 M18 6L6 18',
  // 复制：清晰的「两层文档」叠放，前层为闭合方形，后层为可见的左上 L 形（小尺寸下也一眼识别）
  copy:   'M9 9h11v11H9z M15 9V4H4v11h5',
  save:   'M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z M17 21v-8H7v8 M7 3v5h8',
  refresh:'M21 12a9 9 0 0 0-15.5-6.3L3 8 M3 4v4h4 M3 12a9 9 0 0 0 15.5 6.3L21 16 M21 20v-4h-4',
  scissors:'M6 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M6 21a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M20 4L8.12 15.88 M14.47 14.48L20 20 M8.12 8.12L12 12',

  /* —— 导航/箭头 —— */
  chevronRight: 'M9 6l6 6-6 6',
  chevronLeft:  'M15 6l-6 6 6 6',
  chevronUp:    'M6 15l6-6 6 6',
  chevronDown:  'M6 9l6 6 6-6',
  chevronsRight: 'M7 6l6 6-6 6 M13 6l6 6-6 6',
  arrowUp:    'M12 19V5 M5 12l7-7 7 7',
  arrowDown:  'M12 5v14 M5 12l7 7 7-7',
  arrowLeft:  'M19 12H5 M12 19l-7-7 7-7',
  arrowRight: 'M5 12h14 M12 5l7 7-7 7',
  back:       'M19 12H5 M12 19l-7-7 7-7',
  forward:    'M5 12h14 M12 5l7 7-7 7',
  externalLink: 'M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6 M15 3h6v6 M10 14L21 3',
  home:       'M3 12l9-9 9 9 M5 10v10h14V10',
  menu:       'M3 6h18 M3 12h18 M3 18h18',
  navigation: 'M12 2l4 19-4-9-9-4z',

  /* —— 文件/文档 —— */
  folder:     'M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z',
  folderOpen: 'M3 7v12a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-7l-2-2H5a2 2 0 0 0-2 2z',
  folderPlus: 'M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z M12 11v6 M9 14h6',
  file:       'M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z M14 3v6h6',
  fileText:   'M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z M14 3v6h6 M9 13h6 M9 17h6',
  fileCode:   'M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z M14 3v6h6 M10 13l-2 2 2 2 M14 13l2 2-2 2',
  clipboard:  'M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2 M9 5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2 M9 5v2h6V5',
  book:       'M4 19.5A2.5 2.5 0 0 1 6.5 17H20 M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z',
  bookmark:   'M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z',
  archive:    'M21 8v13H3V8 M1 3h22v5H1z M10 12h4',
  inbox:      'M22 12h-6l-2 3h-4l-2-3H2 M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z',

  /* —— 通讯 —— */
  mail:    'M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M22 6l-10 7L2 6',
  phone:   'M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z',
  message: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z',
  chat:    'M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8z',
  send:    'M22 2L11 13 M22 2l-7 20-4-9-9-4z',
  atSign:  'M12 16a4 4 0 1 1 0-8 4 4 0 0 1 0 8z M16 8v5a3 3 0 0 0 6 0v-1a10 10 0 1 0-3.92 7.94',

  /* —— 媒体 —— */
  image:      'M3 3h18v18H3z M8 11a2 2 0 1 1 0-4 2 2 0 0 1 0 4z M21 15l-5-5L5 21',
  video:      'M23 7l-7 5 7 5z M3 5h13a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z',
  camera:     'M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z M12 17a4 4 0 1 1 0-8 4 4 0 0 1 0 8z',
  microphone: 'M12 2a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3z M19 10v1a7 7 0 0 1-14 0v-1 M12 18v4 M8 22h8',
  music:      'M9 18V5l12-2v13 M9 18a3 3 0 1 1-6 0 3 3 0 0 1 6 0z M21 16a3 3 0 1 1-6 0 3 3 0 0 1 6 0z',
  volume:     'M11 5L6 9H2v6h4l5 4z M19 12a5 5 0 0 0-3-4.6 M22 12a8 8 0 0 0-5-7.5',
  volumeOff:  'M11 5L6 9H2v6h4l5 4z M23 9l-6 6 M17 9l6 6',
  play:       'M5 3l14 9-14 9z',
  pause:      'M6 4h4v16H6z M14 4h4v16h-4z',
  stop:       'M5 5h14v14H5z',
  headphones: 'M3 18v-6a9 9 0 0 1 18 0v6 M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3z M3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z',
  speaker:    'M4 2h16a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z M12 14a2 2 0 1 0 0-4 2 2 0 0 0 0 4z M12 6h.01',

  /* —— 时间 —— */
  clock:    'M12 22a10 10 0 1 1 0-20 10 10 0 0 1 0 20z M12 6v6l4 2',
  calendar: 'M19 4H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2z M16 2v4 M8 2v4 M3 10h18',
  timer:    'M10 2h4 M12 14l3-3 M12 22a8 8 0 1 1 0-16 8 8 0 0 1 0 16z',
  history:  'M3 12a9 9 0 1 0 3-6.7L3 8 M3 3v5h5 M12 7v5l4 2',
  hourglass:'M5 22h14 M5 2h14 M17 22v-4.2a2 2 0 0 0-.6-1.4L13 13l3.4-3.4a2 2 0 0 0 .6-1.4V4 M7 22v-4.2a2 2 0 0 1 .6-1.4L11 13 7.6 9.4A2 2 0 0 1 7 8V4',

  /* —— 状态/反馈 —— */
  info:    'M12 22a10 10 0 1 1 0-20 10 10 0 0 1 0 20z M12 16v-4 M12 8h.01',
  alert:   'M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z M12 9v4 M12 17h.01',
  success: 'M22 11.08V12a10 10 0 1 1-5.93-9.14 M22 4L12 14.01l-3-3',
  error:   'M12 22a10 10 0 1 1 0-20 10 10 0 0 1 0 20z M15 9l-6 6 M9 9l6 6',
  help:    'M12 22a10 10 0 1 1 0-20 10 10 0 0 1 0 20z M9.5 9a2.5 2.5 0 1 1 3.5 2.3c-1 .5-1.5 1.2-1.5 2.2 M12 17h.01',
  lock:    'M5 11h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2z M7 11V7a5 5 0 0 1 10 0v4',
  unlock:  'M5 11h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2z M7 11V7a5 5 0 0 1 9.9-1',
  eye:     'M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z M12 15a3 3 0 1 1 0-6 3 3 0 0 1 0 6z',
  eyeOff:  'M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94 M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19 M14.12 14.12a3 3 0 1 1-4.24-4.24 M1 1l22 22',

  /* —— 数据/结构 —— */
  database: 'M4 7c0-2 4-3 8-3s8 1 8 3v10c0 2-4 3-8 3s-8-1-8-3z M4 7c0 2 4 3 8 3s8-1 8-3 M4 12c0 2 4 3 8 3s8-1 8-3',
  list:     'M4 6h16 M4 12h16 M4 18h16',
  grid:     'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
  cube:     'M12 2l9 5v10l-9 5-9-5V7z M12 12l9-5 M12 12l-9-5 M12 12v10',
  box:      'M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z M3.27 6.96L12 12.01l8.73-5.05 M12 22V12',
  package:  'M16.5 9.4L7.55 4.24 M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z M3.27 6.96L12 12.01l8.73-5.05 M12 22.08V12',
  layers:   'M12 2l-10 5 10 5 10-5z M2 17l10 5 10-5 M2 12l10 5 10-5',
  network:  'M5 5h4v4H5z M15 5h4v4h-4z M5 15h4v4H5z M15 15h4v4h-4z M9 7h6 M9 17h6 M7 9v6 M17 9v6',
  link:     'M10 14a5 5 0 0 0 7 0l3-3a5 5 0 0 0-7-7l-1.5 1.5 M14 10a5 5 0 0 0-7 0l-3 3a5 5 0 0 0 7 7l1.5-1.5',
  share:    'M4 12v7a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-7 M16 6l-4-4-4 4 M12 2v13',
  download: 'M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4 M7 10l5 5 5-5 M12 15V3',
  branch:   'M6 3v12 M18 6a3 3 0 1 1-6 0 3 3 0 0 1 6 0z M6 21a3 3 0 1 1 0-6 3 3 0 0 1 0 6z M18 9c0 6-6 6-6 12',
  workflow: 'M3 3h6v6H3z M15 3h6v6h-6z M9 6h6 M9 9v6 M3 15h6v6H3z M15 15h6v6h-6z M15 18H9',
  hash:     'M4 9h16 M4 15h16 M10 3L8 21 M16 3l-2 18',
  tag:      'M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z M7 7h.01',

  /* —— 图表/统计 —— */
  chart:        'M3 3v18h18 M7 14l4-4 4 4 5-5',
  barChart:     'M12 20V10 M18 20V4 M6 20v-4',
  pieChart:     'M21 12A9 9 0 1 1 12 3v9z M21 12a9 9 0 0 0-9-9v9z',
  trendingUp:   'M23 6l-9.5 9.5-5-5L1 18 M17 6h6v6',
  trendingDown: 'M23 18l-9.5-9.5-5 5L1 6 M17 18h6v-6',
  activity:     'M22 12h-4l-3 9L9 3l-3 9H2',
  dashboard:    'M3 3h7v9H3z M14 3h7v5h-7z M14 12h7v9h-7z M3 16h7v5H3z',

  /* —— 用户/人员 —— */
  user:      'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M12 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
  users:     'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 1 0-8 4 4 0 0 1 0 8z M23 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75',
  userPlus:  'M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 1 0-8 4 4 0 0 1 0 8z M20 8v6 M23 11h-6',
  userCheck: 'M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 1 0-8 4 4 0 0 1 0 8z M17 11l2 2 4-4',
  userX:     'M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 1 0-8 4 4 0 0 1 0 8z M17 8l5 5 M22 8l-5 5',
  team:      'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 1 0-8 4 4 0 0 1 0 8z M23 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75',

  /* —— 通知/系统 —— */
  bell:    'M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9 M14 21a2 2 0 0 1-4 0',
  cog:     'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z M19 12l2 1-2 4-2-1 M5 12l-2 1 2 4 2-1 M12 5V3 M12 21v-2',
  settings:'M12 15a3 3 0 1 1 0-6 3 3 0 0 1 0 6z M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z',
  sliders: 'M4 21v-7 M4 10V3 M12 21v-9 M12 8V3 M20 21v-5 M20 12V3 M1 14h6 M9 8h6 M17 16h6',
  tool:    'M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 1 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z',
  filter:  'M22 3H2l8 9.46V19l4 2v-8.54z',
  power:   'M18.36 6.64a9 9 0 1 1-12.73 0 M12 2v10',
  logout:  'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9',
  login:   'M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4 M10 17l5-5-5-5 M15 12H3',

  /* —— 主题/外观 —— */
  sun:      'M12 17a5 5 0 1 1 0-10 5 5 0 0 1 0 10z M12 1v2 M12 21v2 M4.2 4.2l1.4 1.4 M18.4 18.4l1.4 1.4 M1 12h2 M21 12h2 M4.2 19.8l1.4-1.4 M18.4 5.6l1.4-1.4',
  moon:     'M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z',
  maximize: 'M3 9V3h6 M21 9V3h-6 M3 15v6h6 M21 15v6h-6',
  minimize: 'M9 3v6H3 M15 3v6h6 M9 21v-6H3 M15 21v-6h6',
  winMax: 'M5 5h14v14H5z',
  winRestore: 'M4 8h12v12H4z M8 4h12v12',

  /* —— 标记/收藏 —— */
  star:  'M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01z',
  heart: 'M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z',
  flag:  'M4 22V4a2 2 0 0 1 2-2h10l2 4h4v10h-8l-2-4H4z',
  award: 'M12 15a7 7 0 1 1 0-14 7 7 0 0 1 0 14z M8.21 13.89L7 23l5-3 5 3-1.21-9.11',
  gift:  'M20 12v10H4V12 M2 7h20v5H2z M12 22V7 M12 7H7.5a2.5 2.5 0 1 1 0-5C11 2 12 7 12 7z M12 7h4.5a2.5 2.5 0 1 0 0-5C13 2 12 7 12 7z',

  /* —— 位置/地图 —— */
  globe:   'M12 22a10 10 0 1 1 0-20 10 10 0 0 1 0 20z M2 12h20 M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z',
  map:     'M1 6v16l7-3 8 3 7-3V3l-7 3-8-3z M8 3v16 M16 6v15',
  pin:     'M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z M12 13a3 3 0 1 1 0-6 3 3 0 0 1 0 6z',
  compass: 'M12 22a10 10 0 1 1 0-20 10 10 0 0 1 0 20z M16.24 7.76l-2.12 6.36-6.36 2.12 2.12-6.36z',

  /* —— 天气/能源 —— */
  cloud:       'M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z',
  rain:        'M16 13v8 M8 13v8 M12 15v8 M20 16.58A5 5 0 0 0 18 7h-1.26A8 8 0 1 0 4 15.25',
  snowflake:   'M2 12h20 M12 2v20 M20 16l-8-4 8-4 M4 16l8-4-8-4 M16 4l-4 8 4 8 M8 4l4 8-4 8',
  umbrella:    'M23 12a11.05 11.05 0 0 0-22 0zM5 12a7 7 0 0 1 14 0',
  thermometer: 'M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z',
  flame:       'M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z',
  zap:         'M13 2L4 14h7l-1 8 9-12h-7z',
  battery:     'M2 6h18v12H2z M22 13v-2',
  wifi:        'M5 12.55a11 11 0 0 1 14 0 M1.42 9a16 16 0 0 1 21.16 0 M8.53 16.11a6 6 0 0 1 6.95 0 M12 20h.01',
  bluetooth:   'M6.5 6.5l11 11L12 23V1l5.5 5.5L6.5 17.5',

  /* —— 水利/水务 —— */
  wave:    'M2 12c2-3 4-3 6 0s4 3 6 0 4-3 6 0',
  droplet: 'M12 3l5 7a6 6 0 1 1-10 0z',
  dam:     'M3 4v16 M21 4v16 M5 6h14v2H5z M5 12h14v2H5z M5 18h14',
  station: 'M12 3v14 M5 8c0 4 2 7 7 7s7-3 7-7 M9 21l3-4 3 4',
  ship:    'M2 21c.6.5 1.2 1 2.5 1 2.5 0 2.5-2 5-2 1.3 0 1.9.5 2.5 1 .6.5 1.2 1 2.5 1 2.5 0 2.5-2 5-2 1.3 0 1.9.5 2.5 1 M19.38 20A11.6 11.6 0 0 0 21 14l-9-4-9 4c0 2.9.94 5.34 2.81 7.76 M19 13V7a2 2 0 0 0-2-2H7a2 2 0 0 0-2 2v6 M12 10v4 M12 2v3',

  /* —— 生态/农业 —— */
  leaf:  'M20 4c0 9-7 16-16 16 0-9 7-16 16-16z M4 20l8-8',
  tree:  'M12 22v-6 M7 16h10 M12 3l5 7H7z M12 9l4 6H8z',
  wheat: 'M12 22V8 M12 12l-4-4 M12 12l4-4 M12 17l-4-4 M12 17l4-4 M12 8l-3-3 M12 8l3-3',

  /* —— 工业/工程 —— */
  industry: 'M3 21h18 M5 21V8l5 3V8l5 3V8l4 3v10',
  factory:  'M2 20h20 M4 20V9l4 2V9l4 2V9l4 2V9l4 2v9 M8 20v-4 M14 20v-4',
  building: 'M3 21h18 M5 21V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16 M9 9h.01 M15 9h.01 M9 13h.01 M15 13h.01 M9 17h.01 M15 17h.01',
  warehouse:'M22 8.35V20a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8.35A2 2 0 0 1 3.26 6.5l8-3.2a2 2 0 0 1 1.48 0l8 3.2A2 2 0 0 1 22 8.35z M6 18h12 M6 14h12 M6 10h12',
  briefcase:'M20 7h-4V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2z M14 7V5h-4v2',
  shield:   'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z',

  /* —— 交通运输 —— */
  car:    'M5 17a2 2 0 1 0 0-4 2 2 0 0 0 0 4z M19 17a2 2 0 1 0 0-4 2 2 0 0 0 0 4z M3 17V9l3-5h12l3 5v8h-3 M7 17h10 M5 9h14',
  truck:  'M1 3h15v13H1z M16 8h5l3 3v5h-8V8z M5.5 21a2.5 2.5 0 1 1 0-5 2.5 2.5 0 0 1 0 5z M18.5 21a2.5 2.5 0 1 1 0-5 2.5 2.5 0 0 1 0 5z',
  bus:    'M8 6v6 M16 6v6 M2 12h20 M7 18h10 M18 18v3 M6 18v3 M5 16h14a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2z',
  plane:  'M16 16l4 2v-4l-4-2v-7a2 2 0 0 0-4 0v7L8 14v4l4-2v3l-2 2v1l4-1 4 1v-1l-2-2v-3z',
  bike:   'M5 17a3 3 0 1 1 0-6 3 3 0 0 1 0 6z M19 17a3 3 0 1 1 0-6 3 3 0 0 1 0 6z M12 17.5V14l-3-3 4-3 2 3h2',
  rocket: 'M12 2l3 6 6 1-4 5 1 6-6-3-6 3 1-6L3 9l6-1z',

  /* —— 金融 —— */
  dollar:     'M12 1v22 M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6',
  creditCard: 'M2 4h20v16H2z M2 10h20',
  wallet:     'M21 12V7H5a2 2 0 0 1 0-4h14v4 M3 5v14a2 2 0 0 0 2 2h16v-5 M18 12a2 2 0 0 0-2 2v0a2 2 0 0 0 2 2h4v-4z',
  percent:    'M19 5L5 19 M7.5 8a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z M16.5 19a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z',

  /* —— 设备/硬件 —— */
  monitor:    'M2 3h20v14H2z M8 21h8 M12 17v4',
  smartphone: 'M5 2h14a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z M12 18h.01',
  tablet:     'M5 2h14a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z M12 18h.01',
  laptop:     'M3 5h18v11H3z M1 20h22',
  server:     'M2 2h20v8H2z M2 14h20v8H2z M6 6h.01 M6 18h.01',
  printer:    'M6 9V2h12v7 M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2 M6 14h12v8H6z',
  mouse:      'M9 2h6v10a3 3 0 1 1-6 0z M12 6v6 M5 12a7 7 0 1 1 14 0v6a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3v-6z',
  keyboard:   'M2 4h20v16H2z M6 8h.01 M10 8h.01 M14 8h.01 M18 8h.01 M6 12h.01 M10 12h.01 M14 12h.01 M18 12h.01 M7 16h10',

  /* —— 布局 —— */
  layout:  'M3 3h18v18H3z M3 9h18 M9 21V9',
  columns: 'M12 3v18 M3 3h18v18H3z',
  sidebar: 'M3 3h18v18H3z M9 3v18',
  rows:    'M3 12h18 M3 3h18v18H3z',

  /* —— 代码/开发 —— */
  code:     'M16 18l6-6-6-6 M8 6l-6 6 6 6',
  terminal: 'M4 17l6-6-6-6 M12 19h8',

  /* —— 其他 —— */
  ai:        'M9 3v6a3 3 0 0 1-3 3H3 M15 3v6a3 3 0 0 0 3 3h3 M9 21v-6a3 3 0 0 0-3-3H3 M15 21v-6a3 3 0 0 1 3-3h3',
  ribbon:    'M19 21l-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z'
}

/* ---------- Ant Design Icons (阿里图标) ---------- */
// 解析 antd:xxx / antd:xxx:f / antd:xxx:tt → IconDefinition
function pascal(kebab) {
  return kebab.split('-').map(s => s ? s[0].toUpperCase() + s.slice(1) : '').join('')
}
function resolveAntdDef(name) {
  if (!name || typeof name !== 'string') return null
  // 支持两种写法： 'antd:database' / 'antd:database:f' / 'antd:database:tt' / 'antd:database:o'
  const m = name.match(/^antd:([a-z0-9-]+)(?::(o|f|tt|outlined|filled|twotone))?$/i)
  if (!m) return null
  const base = m[1]
  const tag = (m[2] || 'o').toLowerCase()
  const theme = (tag === 'f' || tag === 'filled') ? 'Filled'
              : (tag === 'tt' || tag === 'twotone') ? 'TwoTone'
              : 'Outlined'
  const key = pascal(base) + theme
  return antdIcons[key] || null
}
// 把 antd IconDefinition 渲染成 SVG 字符串
function renderAntdIcon(def, size, color) {
  const root = def.icon
  const viewBox = (root.attrs && root.attrs.viewBox) || '64 64 896 896'
  const fill = color || 'currentColor'
  const renderNode = (node) => {
    const attrs = Object.entries(node.attrs || {})
      .filter(([k]) => k !== 'focusable')
      .map(([k, v]) => `${k}="${String(v).replace(/"/g, '&quot;')}"`)
      .join(' ')
    const kids = (node.children || []).map(renderNode).join('')
    return kids
      ? `<${node.tag}${attrs ? ' ' + attrs : ''}>${kids}</${node.tag}>`
      : `<${node.tag}${attrs ? ' ' + attrs : ''}/>`
  }
  const inner = (root.children || []).map(renderNode).join('')
  return `<svg width="${size}" height="${size}" viewBox="${viewBox}" fill="${fill}" aria-hidden="true">${inner}</svg>`
}

// 已注册的 antd 图标名（kebab-case）— 仅含 Outlined，给图标选择器使用
const ANTD_OUTLINED_NAMES = Object.keys(antdIcons)
  .filter(k => k.endsWith('Outlined'))
  .map(k => k.slice(0, -8).replace(/([a-z0-9])([A-Z])/g, '$1-$2').replace(/([A-Z])([A-Z][a-z])/g, '$1-$2').toLowerCase())
const ANTD_FILLED_NAMES = Object.keys(antdIcons)
  .filter(k => k.endsWith('Filled'))
  .map(k => k.slice(0, -6).replace(/([a-z0-9])([A-Z])/g, '$1-$2').replace(/([A-Z])([A-Z][a-z])/g, '$1-$2').toLowerCase())
const ANTD_TWOTONE_NAMES = Object.keys(antdIcons)
  .filter(k => k.endsWith('TwoTone'))
  .map(k => k.slice(0, -7).replace(/([a-z0-9])([A-Z])/g, '$1-$2').replace(/([A-Z])([A-Z][a-z])/g, '$1-$2').toLowerCase())

export function antdIconNames(theme = 'outlined') {
  if (theme === 'filled') return ANTD_FILLED_NAMES.slice()
  if (theme === 'twotone') return ANTD_TWOTONE_NAMES.slice()
  return ANTD_OUTLINED_NAMES.slice()
}

/* ---------- 共享「业务图库」(API + 内存缓存) ---------- */
// 结构与后端对齐：groups: [{ id, parentId, name, sort }], icons: [{ id, groupId, name, viewBox, content }]
// 内存中再构造一份 iconsById 供 BL.icon('custom:<id>') O(1) 命中
let CUSTOM = { groups: [], icons: [], iconsById: {} }
const customListeners = new Set()

function rebuildIconsById() {
  const m = {}
  for (const ic of CUSTOM.icons) m[ic.id] = ic
  CUSTOM.iconsById = m
}

/** 取当前快照（仅供组件读取，写入用 setCustomIconData） */
export function getCustomIconData() {
  return {
    groups: CUSTOM.groups.slice(),
    icons: CUSTOM.icons.slice(),
    iconsById: { ...CUSTOM.iconsById }
  }
}

/** 全量替换（用于 API 拉取后写入） */
export function setCustomIconData(next) {
  CUSTOM = {
    groups: Array.isArray(next?.groups) ? next.groups.slice() : [],
    icons: Array.isArray(next?.icons) ? next.icons.slice() : [],
    iconsById: {}
  }
  rebuildIconsById()
  customListeners.forEach(fn => { try { fn(CUSTOM) } catch {} })
}

export function onCustomIconsChange(fn) {
  customListeners.add(fn)
  return () => customListeners.delete(fn)
}

/** 局部更新工具：插入/删除单条图标（供组件在乐观更新时使用） */
export function customIconUpsert(icon) {
  const i = CUSTOM.icons.findIndex(x => x.id === icon.id)
  if (i >= 0) CUSTOM.icons.splice(i, 1, icon)
  else CUSTOM.icons.push(icon)
  CUSTOM.iconsById[icon.id] = icon
  customListeners.forEach(fn => { try { fn(CUSTOM) } catch {} })
}
export function customIconRemoveByIds(ids) {
  const s = new Set(ids)
  CUSTOM.icons = CUSTOM.icons.filter(x => !s.has(x.id))
  for (const id of ids) delete CUSTOM.iconsById[id]
  customListeners.forEach(fn => { try { fn(CUSTOM) } catch {} })
}
export function customGroupUpsert(group) {
  const i = CUSTOM.groups.findIndex(x => x.id === group.id)
  if (i >= 0) CUSTOM.groups.splice(i, 1, group)
  else CUSTOM.groups.push(group)
  customListeners.forEach(fn => { try { fn(CUSTOM) } catch {} })
}
export function customGroupRemove(id) {
  CUSTOM.groups = CUSTOM.groups.filter(x => x.id !== id && x.parentId !== id)
  // 同步清掉孤立的 icon（所属组已删）
  const liveGroupIds = new Set(CUSTOM.groups.map(g => g.id))
  CUSTOM.icons = CUSTOM.icons.filter(ic => liveGroupIds.has(ic.groupId))
  rebuildIconsById()
  customListeners.forEach(fn => { try { fn(CUSTOM) } catch {} })
}

function resolveCustomIcon(name) {
  if (!name || typeof name !== 'string') return null
  const m = name.match(/^custom:(.+)$/)
  if (!m) return null
  return CUSTOM.iconsById[m[1]] || null
}

export function blIcon(name, size = 16, color) {
  // 1) 兼容已有内置图标（描边风格）
  if (name && Object.prototype.hasOwnProperty.call(ICONS, name)) {
    const stroke = color || 'currentColor'
    return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${stroke}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="${ICONS[name]}"></path></svg>`
  }
  // 2) Ant Design 图标（阿里图标库）
  const def = resolveAntdDef(name)
  if (def) return renderAntdIcon(def, size, color)
  // 3) 用户自定义上传图标
  const custom = resolveCustomIcon(name)
  if (custom) {
    const vb = custom.viewBox || '0 0 1024 1024'
    const c = color || 'currentColor'
    // 同时设置 fill 属性（被无 fill 的子元素继承）与 CSS color（用于解析 currentColor）
    return `<svg width="${size}" height="${size}" viewBox="${vb}" fill="${c}" style="color:${c}" aria-hidden="true">${custom.content || ''}</svg>`
  }
  // 4) 回退
  const stroke = color || 'currentColor'
  return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${stroke}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="${ICONS.cube}"></path></svg>`
}
export function blIconPath(name) { return ICONS[name] || ICONS.cube }

/* ---------- TOAST ---------- */
function ensureToastWrap() {
  let w = document.getElementById('bl-toast-wrap')
  if (!w) {
    w = document.createElement('div')
    w.id = 'bl-toast-wrap'
    w.className = 'bl-toast-wrap'
    document.body.appendChild(w)
  }
  return w
}

function toast(msg, type = 'info', duration = 2400) {
  const wrap = ensureToastWrap()
  const el = document.createElement('div')
  el.className = `bl-toast bl-toast-${type}`
  el.textContent = msg
  wrap.appendChild(el)
  setTimeout(() => {
    el.style.transition = 'opacity .2s'
    el.style.opacity = '0'
    setTimeout(() => el.remove(), 220)
  }, duration)
}

/* ---------- CONFIRM ---------- */
function confirm({ title = '操作确认', content = '', okText = '确定', cancelText = '取消', danger = false } = {}) {
  return new Promise(resolve => {
    const mask = document.createElement('div')
    mask.className = 'bl-modal-mask'
    mask.innerHTML = `
      <div class="bl-modal" role="dialog">
        <div class="bl-modal-hd">${title}</div>
        <div class="bl-modal-body">${content}</div>
        <div class="bl-modal-ft">
          <button class="bl-btn" data-act="cancel">${cancelText}</button>
          <button class="bl-btn ${danger ? 'bl-btn-danger' : 'bl-btn-primary'}" data-act="ok">${okText}</button>
        </div>
      </div>`
    document.body.appendChild(mask)
    const cleanup = (val) => { mask.remove(); resolve(val) }
    mask.addEventListener('click', e => {
      const act = e.target?.dataset?.act
      if (act === 'ok') cleanup(true)
      else if (act === 'cancel' || e.target === mask) cleanup(false)
    })
  })
}

/* ---------- PROMPT ---------- */
function prompt({
  title = '请输入',
  label = '',
  placeholder = '',
  defaultValue = '',
  okText = '确定',
  cancelText = '取消',
  validate = null   // (value) => true | string(错误信息)
} = {}) {
  return new Promise(resolve => {
    const esc = s => String(s ?? '').replace(/[&<>"]/g, c => ({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;' }[c]))
    const mask = document.createElement('div')
    mask.className = 'bl-modal-mask'
    mask.innerHTML = `
      <div class="bl-modal bl-prompt" role="dialog">
        <div class="bl-modal-hd">${esc(title)}</div>
        <div class="bl-modal-body">
          ${label ? `<div class="bl-prompt-label">${esc(label)}</div>` : ''}
          <input class="bl-input bl-prompt-input" placeholder="${esc(placeholder)}" value="${esc(defaultValue)}" />
          <div class="bl-prompt-err" style="display:none"></div>
        </div>
        <div class="bl-modal-ft">
          <button class="bl-btn" data-act="cancel">${esc(cancelText)}</button>
          <button class="bl-btn bl-btn-primary" data-act="ok">${esc(okText)}</button>
        </div>
      </div>`
    document.body.appendChild(mask)
    const input = mask.querySelector('.bl-prompt-input')
    const errEl = mask.querySelector('.bl-prompt-err')
    const cleanup = (val) => { document.removeEventListener('keydown', onKey); mask.remove(); resolve(val) }
    const tryOk = () => {
      const v = input.value
      if (typeof validate === 'function') {
        const r = validate(v)
        if (r !== true && r != null) {
          errEl.textContent = String(r || '输入无效')
          errEl.style.display = 'block'
          input.focus()
          return
        }
      }
      cleanup(v)
    }
    const onKey = (e) => {
      if (e.key === 'Escape') { e.preventDefault(); cleanup(null) }
      else if (e.key === 'Enter') { e.preventDefault(); tryOk() }
    }
    document.addEventListener('keydown', onKey)
    mask.addEventListener('click', e => {
      const act = e.target?.dataset?.act
      if (act === 'ok') tryOk()
      else if (act === 'cancel' || e.target === mask) cleanup(null)
    })
    // 自动聚焦并选中默认值
    setTimeout(() => { input.focus(); input.select() }, 0)
  })
}

export const BL = {
  icon: blIcon,
  iconPath: blIconPath,
  toast,
  success: (m) => toast(m, 'success'),
  warning: (m) => toast(m, 'warning'),
  error:   (m) => toast(m, 'error'),
  info:    (m) => toast(m, 'info'),
  confirm,
  prompt
}
