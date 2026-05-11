package com.lpsolver.backend.model;

public class ConstraintRequest {
    private double x;
    private double y;
    private String operator; // <=, >=, =
    private double value;

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}
