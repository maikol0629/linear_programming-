package com.lpsolver.backend.model;

import java.util.List;

public class SolutionResponse {
    private String status; // OPTIMAL, INFEASIBLE, UNBOUNDED
    private Point optimalPoint;
    private double optimalValue;
    private List<Point> feasibleVertices;
    private List<String> steps; // Explanation steps

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Point getOptimalPoint() { return optimalPoint; }
    public void setOptimalPoint(Point optimalPoint) { this.optimalPoint = optimalPoint; }

    public double getOptimalValue() { return optimalValue; }
    public void setOptimalValue(double optimalValue) { this.optimalValue = optimalValue; }

    public List<Point> getFeasibleVertices() { return feasibleVertices; }
    public void setFeasibleVertices(List<Point> feasibleVertices) { this.feasibleVertices = feasibleVertices; }

    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }
}
