package com.lpsolver.backend.model;

import java.util.List;

public class SimplexStep {
    private int iteration;
    private String description;
    private List<List<Double>> tableau;
    private List<String> rowHeaders;
    private List<String> columnHeaders;
    private Integer pivotRow;
    private Integer pivotCol;
    private String enteringVariable;
    private String leavingVariable;

    // Getters and Setters
    public int getIteration() { return iteration; }
    public void setIteration(int iteration) { this.iteration = iteration; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<List<Double>> getTableau() { return tableau; }
    public void setTableau(List<List<Double>> tableau) { this.tableau = tableau; }

    public List<String> getRowHeaders() { return rowHeaders; }
    public void setRowHeaders(List<String> rowHeaders) { this.rowHeaders = rowHeaders; }

    public List<String> getColumnHeaders() { return columnHeaders; }
    public void setColumnHeaders(List<String> columnHeaders) { this.columnHeaders = columnHeaders; }

    public Integer getPivotRow() { return pivotRow; }
    public void setPivotRow(Integer pivotRow) { this.pivotRow = pivotRow; }

    public Integer getPivotCol() { return pivotCol; }
    public void setPivotCol(Integer pivotCol) { this.pivotCol = pivotCol; }

    public String getEnteringVariable() { return enteringVariable; }
    public void setEnteringVariable(String enteringVariable) { this.enteringVariable = enteringVariable; }

    public String getLeavingVariable() { return leavingVariable; }
    public void setLeavingVariable(String leavingVariable) { this.leavingVariable = leavingVariable; }
}
