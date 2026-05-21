export interface ObjectiveCoeffRange {
  variableName: string;
  variableIndex: number;
  currentValue: number;
  minRange: number;
  maxRange: number;
  allowedDecrease: number;
  allowedIncrease: number;
  bindingConstraint?: string;
  interpretation: string;
  basic: boolean;
  reducedCost: number;
}

export interface RHSRange {
  constraintIndex: number;
  constraintName: string;
  currentValue: number;
  minRange: number;
  maxRange: number;
  shadowPrice: number;
  bindingConstraint?: string;
  interpretation: string;
}

export interface ReducedCost {
  variableName: string;
  variableIndex: number;
  cost: number;
  interpretation: string;
}

export interface ShadowPrice {
  constraintIndex: number;
  constraintName: string;
  price: number;
  interpretation: string;
}

export interface DegeneracyWarning {
  degenerateSolution: boolean;
  zeroValuedBasicVariables: string[];
  recommendation: string;
  severity: 'INFO' | 'WARNING' | 'ERROR';
}

export interface SensitivityAnalysis {
  objectiveCoefficientsRanges: ObjectiveCoeffRange[];
  rhsRanges: RHSRange[];
  reducedCosts: ReducedCost[];
  shadowPrices: ShadowPrice[];
  degeneracyWarning: DegeneracyWarning;
  analysisNotes: string;
}
