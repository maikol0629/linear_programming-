package com.lpsolver.backend.model;

import java.util.List;

public class SimplexResponse {
    private String status; // OPTIMAL, UNBOUNDED
    private List<Double> optimalSolution; // values of x1, x2...
    private double optimalValue;
    private List<SimplexStep> steps;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<Double> getOptimalSolution() { return optimalSolution; }
    public void setOptimalSolution(List<Double> optimalSolution) { this.optimalSolution = optimalSolution; }

    public double getOptimalValue() { return optimalValue; }
    public void setOptimalValue(double optimalValue) { this.optimalValue = optimalValue; }

    public List<SimplexStep> getSteps() { return steps; }
    public void setSteps(List<SimplexStep> steps) { this.steps = steps; }
}
