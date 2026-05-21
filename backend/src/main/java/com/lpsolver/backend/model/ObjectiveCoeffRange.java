package com.lpsolver.backend.model;

public class ObjectiveCoeffRange {
    private String variableName;
    private int variableIndex;
    private double currentValue;
    private double minRange;
    private double maxRange;
    private double allowedDecrease;
    private double allowedIncrease;
    private String bindingConstraint;
    private String interpretation;
    private boolean basic;
    private double reducedCost;

    public ObjectiveCoeffRange() {
    }

    public ObjectiveCoeffRange(String variableName, int variableIndex, double currentValue) {
        this.variableName = variableName;
        this.variableIndex = variableIndex;
        this.currentValue = currentValue;
        this.minRange = Double.NEGATIVE_INFINITY;
        this.maxRange = Double.POSITIVE_INFINITY;
        this.allowedDecrease = Double.POSITIVE_INFINITY;
        this.allowedIncrease = Double.POSITIVE_INFINITY;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public int getVariableIndex() {
        return variableIndex;
    }

    public void setVariableIndex(int variableIndex) {
        this.variableIndex = variableIndex;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public double getMinRange() {
        return minRange;
    }

    public void setMinRange(double minRange) {
        this.minRange = minRange;
    }

    public double getMaxRange() {
        return maxRange;
    }

    public void setMaxRange(double maxRange) {
        this.maxRange = maxRange;
    }

    public double getAllowedDecrease() {
        return allowedDecrease;
    }

    public void setAllowedDecrease(double allowedDecrease) {
        this.allowedDecrease = allowedDecrease;
    }

    public double getAllowedIncrease() {
        return allowedIncrease;
    }

    public void setAllowedIncrease(double allowedIncrease) {
        this.allowedIncrease = allowedIncrease;
    }

    public String getBindingConstraint() {
        return bindingConstraint;
    }

    public void setBindingConstraint(String bindingConstraint) {
        this.bindingConstraint = bindingConstraint;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }

    public boolean isBasic() {
        return basic;
    }

    public void setBasic(boolean basic) {
        this.basic = basic;
    }

    public double getReducedCost() {
        return reducedCost;
    }

    public void setReducedCost(double reducedCost) {
        this.reducedCost = reducedCost;
    }
}
