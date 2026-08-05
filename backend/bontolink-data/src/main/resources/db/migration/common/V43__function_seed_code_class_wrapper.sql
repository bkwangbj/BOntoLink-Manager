-- 函数种子代码修正:补上类壳与装饰器导入
--
-- V41 种下的 TypeScript 代码是「装饰器 + public 方法」的片段, 顶层出现 public 不是合法 TS,
-- 编辑器语言服务会连锁报出 Declaration expected / Cannot find name 等一串错, 问题面板全是红叉。
-- 平台约定的函数写法本就是「类 + 方法」(新增函数向导生成的模板也是这样), 这里把种子对齐:
--   1. 包一层 export class(类名取 ont_function.class_name)
--   2. 从 @foundry/functions-api 导入装饰器(IDE 侧注入了对应 .d.ts)
--   3. 补齐 ThresholdsResult / SoilLossResult / ReservoirCreateInput 等结果类型定义
--   4. this.helperFunctions 声明为类成员(平台注入的通用工具函数集)
--
-- 本体对象类型(HydrologyStation 等)保持裸用法, 不 import:
-- 它们由 IDE「资源导入」面板生成 .d.ts 注入为全局声明。
--
-- Python 种子不动:Python 侧没有编辑器诊断, 且其模板本就是类结构。

UPDATE ont_function SET code_content =
'import { Function } from "@foundry/functions-api";

/** 阈值计算结果 */
export interface ThresholdsResult {
  waterLevel: number;
  flow: number;
  levelGroup: string;
}

export class ThresholdCalculator {
  /** 平台注入的通用工具函数集 */
  private helperFunctions: any;

  @Function()
  public getHydrologyStationThresholds(station: HydrologyStation): ThresholdsResult {
    const stationId = station.stationId;
    console.log("获取水文站阈值, 站点ID: ", stationId);

    // 调用通用工具函数计算站龄
    const age = this.helperFunctions.calculateStationAge(station);

    if (age === undefined) {
      // 注意: 此处刻意不用模板字符串拼接, 否则会被 Flyway 当成占位符解析而报错
      throw new Error("无法计算阈值: 水文站 " + stationId + " 无投运日期");
    }

    console.log("水文站站龄: ", age);

    // 根据站龄确定等级与基准阈值
    let levelGroup: string;
    let baseThresholds: ThresholdsResult;

    if (age >= 20) {
      levelGroup = "legacy";
      baseThresholds = { waterLevel: 82.5, flow: 1350, levelGroup };
    } else if (age >= 5) {
      levelGroup = "normal";
      baseThresholds = { waterLevel: 85.0, flow: 1500, levelGroup };
    } else {
      levelGroup = "new";
      baseThresholds = { waterLevel: 86.2, flow: 1620, levelGroup };
    }

    return baseThresholds;
  }
}'
 WHERE id = 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31';

UPDATE ont_function SET code_content =
'import { Function } from "@foundry/functions-api";

/** 阈值计算结果 */
export interface ThresholdsResult {
  waterLevel: number;
  flow: number;
  levelGroup: string;
}

export class ThresholdCalculator {
  @Function()
  public getHydrologyStationThresholds(station: HydrologyStation): ThresholdsResult {
    // v1.1.0: 尚未引入站龄分组, 固定返回基准阈值
    return { waterLevel: 85.0, flow: 1500, levelGroup: "normal" };
  }
}'
 WHERE id = 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a30';

UPDATE ont_function SET code_content =
'import { OntologyEditFunction } from "@foundry/functions-api";

/** 新建水库记录的入参 */
export interface ReservoirCreateInput {
  reservoirName: string;
  totalCapacity: number;
  damHeight: number;
}

export class ReservoirEditor {
  @OntologyEditFunction()
  public createReservoirRecord(payload: ReservoirCreateInput): Reservoir {
    // TODO: 校验库容与坝高约束后写入本体
    throw new Error("not implemented");
  }
}'
 WHERE id = 'ont_function-3b8c4d2e-6f5a-4b7c-9d0e-1f2a3b4c5d6e';

UPDATE ont_function SET code_content =
'import { Function } from "@foundry/functions-api";

/** 土壤流失计算结果 */
export interface SoilLossResult {
  lossAmount: number;
  level: string;
}

export class SoilLossCalculator {
  @Function()
  public calculateSoilLossAmount(plot: SoilErosionPlot, rainfall: number): SoilLossResult {
    // TODO: 接入 USLE 模型系数表
    return { lossAmount: 0, level: "unknown" };
  }
}'
 WHERE id = 'ont_function-2a7b3c1d-5e4f-4a6b-8c9d-0e1f2a3b4c5d';

UPDATE ont_function SET code_content =
'import { Function } from "@foundry/functions-api";

export class ProjectDerived {
  @Function()
  public getProjectRunningYears(project: HydropowerStation): number {
    const start = project.commissionDate;
    if (!start) return 0;
    return new Date().getFullYear() - new Date(start).getFullYear();
  }
}'
 WHERE id = 'ont_function-5d0e6f4a-8b7c-4d9e-1f2a-3b4c5d6e7f8a';

-- 代码行数变了, 详情页展示的「代码位置」跟着改(装饰器行 → 方法闭合行)
UPDATE ont_function SET file_line_start = 14, file_line_end = 45
 WHERE id = 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31';
UPDATE ont_function SET file_line_start = 11, file_line_end = 15
 WHERE id = 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a30';
UPDATE ont_function SET file_line_start = 11, file_line_end = 15
 WHERE id = 'ont_function-3b8c4d2e-6f5a-4b7c-9d0e-1f2a3b4c5d6e';
UPDATE ont_function SET file_line_start = 10, file_line_end = 14
 WHERE id = 'ont_function-2a7b3c1d-5e4f-4a6b-8c9d-0e1f2a3b4c5d';
UPDATE ont_function SET file_line_start = 4, file_line_end = 9
 WHERE id = 'ont_function-5d0e6f4a-8b7c-4d9e-1f2a-3b4c5d6e7f8a';

-- 旧指纹对不上新代码了。留空而不是留错值:控制器在保存时会按 code_content 重算。
UPDATE ont_function SET code_md5 = NULL
 WHERE id IN (
   'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31',
   'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a30',
   'ont_function-3b8c4d2e-6f5a-4b7c-9d0e-1f2a3b4c5d6e',
   'ont_function-2a7b3c1d-5e4f-4a6b-8c9d-0e1f2a3b4c5d',
   'ont_function-5d0e6f4a-8b7c-4d9e-1f2a-3b4c5d6e7f8a');
