package com.lpsolver.backend.model;

import java.util.List;

public class TableauMetadata {
    private int numOriginalVariables;
    private int numSlackVariables;
    private int numArtificialVariables;
    private List<String> columnHeaders;
    private List<String> rowHeaders;

    public TableauMetadata() {
    }

    public TableauMetadata(int numOriginalVariables, int numSlackVariables, int numArtificialVariables, List<String> columnHeaders, List<String> rowHeaders) {
        this.numOriginalVariables = numOriginalVariables;
        this.numSlackVariables = numSlackVariables;
        this.numArtificialVariables = numArtificialVariables;
        this.columnHeaders = columnHeaders;
        this.rowHeaders = rowHeaders;
    }

    public int getNumOriginalVariables() {
        return numOriginalVariables;
    }

    public void setNumOriginalVariables(int numOriginalVariables) {
        this.numOriginalVariables = numOriginalVariables;
    }

    public int getNumSlackVariables() {
        return numSlackVariables;
    }

    public void setNumSlackVariables(int numSlackVariables) {
        this.numSlackVariables = numSlackVariables;
    }

    public int getNumArtificialVariables() {
        return numArtificialVariables;
    }

    public void setNumArtificialVariables(int numArtificialVariables) {
        this.numArtificialVariables = numArtificialVariables;
    }

    public List<String> getColumnHeaders() {
        return columnHeaders;
    }

    public void setColumnHeaders(List<String> columnHeaders) {
        this.columnHeaders = columnHeaders;
    }

    public List<String> getRowHeaders() {
        return rowHeaders;
    }

    public void setRowHeaders(List<String> rowHeaders) {
        this.rowHeaders = rowHeaders;
    }
}
