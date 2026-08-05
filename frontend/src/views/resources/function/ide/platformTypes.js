/**
 * 平台侧 TypeScript 环境
 *
 * 函数代码用的是平台约定的写法(装饰器 + 类),要让编辑器的 TS 语言服务认得,
 * 必须做两件事:
 *   1. 打开 experimentalDecorators —— 否则 `@Function()` 一律报「装饰器是实验特性」
 *   2. 声明 `@foundry/functions-api` 这个虚拟模块 —— 装饰器从这里导入
 *
 * 本体对象类型不在这里:它们由「资源导入」面板按需生成 .d.ts 注入(全局 interface),
 * 代码里直接写类名即可,不需要 import。
 */

export const PLATFORM_DTS = `// BOntoLink 平台函数运行时声明 —— 由 IDE 注入, 不在代码仓里

declare module "@foundry/functions-api" {
  /** 常规 / 聚合 / 衍生 / 时序函数 */
  export function Function(): MethodDecorator;
  /** 动作函数:默认开启本体编辑事务, 原子提交 */
  export function OntologyEditFunction(): MethodDecorator;
  /** 标注入参, 供平台生成调用表单 */
  export function Param(description?: string): ParameterDecorator;
}
`

/**
 * 一次性配置 monaco 的 TS 语言服务。
 * 幂等:重复调用只是覆盖同名 extraLib。
 */
export function setupTypescriptEnv(monaco) {
  const ts = monaco.languages.typescript
  try {
    ts.typescriptDefaults.setCompilerOptions({
      target: ts.ScriptTarget.ES2020,
      module: ts.ModuleKind.ESNext,
      moduleResolution: ts.ModuleResolutionKind.NodeJs,
      experimentalDecorators: true,
      allowNonTsExtensions: true,
      noEmit: true,
      strict: false,
      lib: ['es2020', 'dom'],
    })
    ts.typescriptDefaults.addExtraLib(PLATFORM_DTS, 'ts:bontolink/platform.d.ts')
  } catch (e) {
    console.warn('[ide] 配置 TypeScript 环境失败:', e)
  }
}
