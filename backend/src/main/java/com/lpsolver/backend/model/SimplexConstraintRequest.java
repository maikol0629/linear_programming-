package com.lpsolver.backend.model;

import java.util.List;

public class SimplexConstraintRequest {
    private List<Double> coefficients;
    private String operator; // <=
    private double value;

    public List<Double> getCoefficients() { return coefficients; }
    public void setCoefficients(List<Double> coefficients) { this.coefficients = coefficients; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}
