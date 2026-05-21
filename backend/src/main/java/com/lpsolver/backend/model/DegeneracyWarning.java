package com.lpsolver.backend.model;

import java.util.List;

public class DegeneracyWarning {
    private boolean degenerateSolution;
    private List<String> zeroValuedBasicVariables;
    private String recommendation;
    private String severity;

    public DegeneracyWarning() {
    }

    public DegeneracyWarning(boolean degenerateSolution, List<String> zeroValuedBasicVariables, String recommendation, String severity) {
        this.degenerateSolution = degenerateSolution;
        this.zeroValuedBasicVariables = zeroValuedBasicVariables;
        this.recommendation = recommendation;
        this.severity = severity;
    }

    public boolean isDegenerateSolution() {
        return degenerateSolution;
    }

    public void setDegenerateSolution(boolean degenerateSolution) {
        this.degenerateSolution = degenerateSolution;
    }

    public List<String> getZeroValuedBasicVariables() {
        return zeroValuedBasicVariables;
    }

    public void setZeroValuedBasicVariables(List<String> zeroValuedBasicVariables) {
        this.zeroValuedBasicVariables = zeroValuedBasicVariables;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
