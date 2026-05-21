package com.lpsolver.backend.model;

public class ReducedCost {
    private String variableName;
    private int variableIndex;
    private double cost;
    private String interpretation;

    public ReducedCost() {
    }

    public ReducedCost(String variableName, int variableIndex, double cost) {
        this.variableName = variableName;
        this.variableIndex = variableIndex;
        this.cost = cost;
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

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }
}
