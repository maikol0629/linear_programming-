package com.lpsolver.backend.model;

import java.util.List;

public class SimplexResponse {
    private String status; // OPTIMAL, UNBOUNDED, INFEASIBLE
    private List<Double> optimalSolution; // values of x1, x2...
    private double optimalValue;
    private List<SimplexStep> steps;
    private double[][] finalTableau;
    private List<String> finalRowHeaders;
    private List<String> finalColumnHeaders;
    private TableauMetadata tableauMetadata;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<Double> getOptimalSolution() { return optimalSolution; }
    public void setOptimalSolution(List<Double> optimalSolution) { this.optimalSolution = optimalSolution; }

    public double getOptimalValue() { return optimalValue; }
    public void setOptimalValue(double optimalValue) { this.optimalValue = optimalValue; }

    public List<SimplexStep> getSteps() { return steps; }
    public void setSteps(List<SimplexStep> steps) { this.steps = steps; }

    public double[][] getFinalTableau() { return finalTableau; }
    public void setFinalTableau(double[][] finalTableau) { this.finalTableau = finalTableau; }

    public List<String> getFinalRowHeaders() { return finalRowHeaders; }
    public void setFinalRowHeaders(List<String> finalRowHeaders) { this.finalRowHeaders = finalRowHeaders; }

    public List<String> getFinalColumnHeaders() { return finalColumnHeaders; }
    public void setFinalColumnHeaders(List<String> finalColumnHeaders) { this.finalColumnHeaders = finalColumnHeaders; }

    public TableauMetadata getTableauMetadata() { return tableauMetadata; }
    public void setTableauMetadata(TableauMetadata tableauMetadata) { this.tableauMetadata = tableauMetadata; }
}
