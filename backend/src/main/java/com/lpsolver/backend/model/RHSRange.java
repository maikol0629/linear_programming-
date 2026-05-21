package com.lpsolver.backend.model;

public class RHSRange {
    private int constraintIndex;
    private String constraintName;
    private double currentValue;
    private double minRange;
    private double maxRange;
    private double shadowPrice;
    private String bindingConstraint;
    private String interpretation;

    public RHSRange() {
    }

    public RHSRange(int constraintIndex, String constraintName, double currentValue) {
        this.constraintIndex = constraintIndex;
        this.constraintName = constraintName;
        this.currentValue = currentValue;
        this.minRange = Double.NEGATIVE_INFINITY;
        this.maxRange = Double.POSITIVE_INFINITY;
    }

    public int getConstraintIndex() {
        return constraintIndex;
    }

    public void setConstraintIndex(int constraintIndex) {
        this.constraintIndex = constraintIndex;
    }

    public String getConstraintName() {
        return constraintName;
    }

    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
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

    public double getShadowPrice() {
        return shadowPrice;
    }

    public void setShadowPrice(double shadowPrice) {
        this.shadowPrice = shadowPrice;
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
}
