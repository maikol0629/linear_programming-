package com.lpsolver.backend.model;

import java.util.List;

public class SensitivityAnalysis {
    private List<ObjectiveCoeffRange> objectiveCoefficientsRanges;
    private List<RHSRange> rhsRanges;
    private List<ReducedCost> reducedCosts;
    private List<ShadowPrice> shadowPrices;
    private DegeneracyWarning degeneracyWarning;
    private String analysisNotes;

    public SensitivityAnalysis() {
    }

    public List<ObjectiveCoeffRange> getObjectiveCoefficientsRanges() {
        return objectiveCoefficientsRanges;
    }

    public void setObjectiveCoefficientsRanges(List<ObjectiveCoeffRange> objectiveCoefficientsRanges) {
        this.objectiveCoefficientsRanges = objectiveCoefficientsRanges;
    }

    public List<RHSRange> getRhsRanges() {
        return rhsRanges;
    }

    public void setRhsRanges(List<RHSRange> rhsRanges) {
        this.rhsRanges = rhsRanges;
    }

    public List<ReducedCost> getReducedCosts() {
        return reducedCosts;
    }

    public void setReducedCosts(List<ReducedCost> reducedCosts) {
        this.reducedCosts = reducedCosts;
    }

    public List<ShadowPrice> getShadowPrices() {
        return shadowPrices;
    }

    public void setShadowPrices(List<ShadowPrice> shadowPrices) {
        this.shadowPrices = shadowPrices;
    }

    public DegeneracyWarning getDegeneracyWarning() {
        return degeneracyWarning;
    }

    public void setDegeneracyWarning(DegeneracyWarning degeneracyWarning) {
        this.degeneracyWarning = degeneracyWarning;
    }

    public String getAnalysisNotes() {
        return analysisNotes;
    }

    public void setAnalysisNotes(String analysisNotes) {
        this.analysisNotes = analysisNotes;
    }
}
