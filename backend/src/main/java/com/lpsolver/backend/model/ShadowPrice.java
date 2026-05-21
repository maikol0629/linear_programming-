package com.lpsolver.backend.model;

public class ShadowPrice {
    private int constraintIndex;
    private String constraintName;
    private double price;
    private String interpretation;

    public ShadowPrice() {
    }

    public ShadowPrice(int constraintIndex, String constraintName, double price) {
        this.constraintIndex = constraintIndex;
        this.constraintName = constraintName;
        this.price = price;
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }
}
