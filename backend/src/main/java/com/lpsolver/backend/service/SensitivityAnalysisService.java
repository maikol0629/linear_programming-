package com.lpsolver.backend.service;

import com.lpsolver.backend.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SensitivityAnalysisService {

    private static final double EPSILON = 1e-6;

    public SensitivityAnalysis analyzeSensitivity(SimplexResponse response, SimplexProblemRequest problem) {
        if (response == null) {
            throw new IllegalArgumentException("Simplex response is required for sensitivity analysis.");
        }
        if (!"OPTIMAL".equals(response.getStatus())) {
            throw new IllegalStateException("Sensitivity analysis requires an OPTIMAL solution. Current status: " + response.getStatus());
        }

        SensitivityAnalysis analysis = new SensitivityAnalysis();
        double[][] tableau = response.getFinalTableau();
        List<String> rowHeaders = response.getFinalRowHeaders();
        List<String> columnHeaders = response.getFinalColumnHeaders();

        if (tableau == null || rowHeaders == null || columnHeaders == null) {
            SimplexStep lastStep = getLastStep(response);
            if (lastStep != null) {
                tableau = convertTableau(lastStep.getTableau());
                rowHeaders = lastStep.getRowHeaders();
                columnHeaders = lastStep.getColumnHeaders();
            }
        }

        if (tableau == null || rowHeaders == null || columnHeaders == null) {
            throw new IllegalStateException("Unable to extract final tableau data for sensitivity analysis.");
        }

        List<ReducedCost> reducedCosts = extractReducedCosts(tableau, problem, columnHeaders);
        analysis.setReducedCosts(reducedCosts);

        List<ObjectiveCoeffRange> objectiveRanges = calculateObjectiveRanges(problem, reducedCosts);
        analysis.setObjectiveCoefficientsRanges(objectiveRanges);

        List<ShadowPrice> shadowPrices = extractShadowPrices(tableau, problem, columnHeaders);
        analysis.setShadowPrices(shadowPrices);

        List<RHSRange> rhsRanges = calculateRHSRanges(problem, shadowPrices);
        analysis.setRhsRanges(rhsRanges);

        DegeneracyWarning warning = detectDegeneracy(response, problem);
        analysis.setDegeneracyWarning(warning);

        analysis.setAnalysisNotes(generateAnalysisNotes(analysis, warning));
        return analysis;
    }

    private SimplexStep getLastStep(SimplexResponse response) {
        if (response.getSteps() == null || response.getSteps().isEmpty()) {
            return null;
        }
        return response.getSteps().get(response.getSteps().size() - 1);
    }

    private double[][] convertTableau(List<List<Double>> tableau) {
        if (tableau == null) {
            return null;
        }
        double[][] result = new double[tableau.size()][];
        for (int i = 0; i < tableau.size(); i++) {
            List<Double> row = tableau.get(i);
            result[i] = new double[row.size()];
            for (int j = 0; j < row.size(); j++) {
                result[i][j] = row.get(j);
            }
        }
        return result;
    }

    private List<ReducedCost> extractReducedCosts(double[][] tableau, SimplexProblemRequest problem, List<String> columnHeaders) {
        List<ReducedCost> reducedCosts = new ArrayList<>();
        int numVars = problem.getObjectiveCoefficients().size();
        int zRow = tableau.length - 1;

        for (int j = 0; j < numVars; j++) {
            double costValue = 0.0;
            if (j < tableau[zRow].length) {
                costValue = tableau[zRow][j];
            }
            String varName = "x" + (j + 1);
            ReducedCost rc = new ReducedCost(varName, j, costValue);
            if (Math.abs(costValue) < EPSILON) {
                rc.setInterpretation("Reduced cost is approximately zero; the variable is basic or there are multiple optimal solutions.");
            } else if (costValue > 0) {
                rc.setInterpretation("Positive reduced cost: increasing this coefficient will eventually change the current basis.");
            } else {
                rc.setInterpretation("Negative reduced cost: the current solution is not optimal for this variable.");
            }
            reducedCosts.add(rc);
        }
        return reducedCosts;
    }

    private List<ObjectiveCoeffRange> calculateObjectiveRanges(SimplexProblemRequest problem, List<ReducedCost> reducedCosts) {
        List<ObjectiveCoeffRange> ranges = new ArrayList<>();
        List<Double> objectiveCoefficients = problem.getObjectiveCoefficients();

        for (int i = 0; i < objectiveCoefficients.size(); i++) {
            String varName = "x" + (i + 1);
            double currentValue = objectiveCoefficients.get(i);
            ReducedCost rc = reducedCosts.get(i);
            ObjectiveCoeffRange range = new ObjectiveCoeffRange(varName, i, currentValue);
            range.setReducedCost(rc.getCost());

            if (Math.abs(rc.getCost()) < EPSILON) {
                range.setBasic(true);
                range.setMinRange(Double.NEGATIVE_INFINITY);
                range.setMaxRange(Double.POSITIVE_INFINITY);
                range.setAllowedDecrease(Double.POSITIVE_INFINITY);
                range.setAllowedIncrease(Double.POSITIVE_INFINITY);
                range.setInterpretation("Variable basic or alternate optimal solution: exact coefficient limits require B^-1 calculation.");
            } else {
                range.setBasic(false);
                range.setMinRange(Double.NEGATIVE_INFINITY);
                range.setAllowedDecrease(Double.POSITIVE_INFINITY);
                range.setAllowedIncrease(Math.abs(rc.getCost()));
                range.setMaxRange(currentValue + Math.abs(rc.getCost()));
                range.setInterpretation("Variable non-basic: current basis remains optimal mientras el coeficiente no aumente más de " + String.format("%.4f", Math.abs(rc.getCost())) + ".");
            }

            ranges.add(range);
        }
        return ranges;
    }

    private List<ShadowPrice> extractShadowPrices(double[][] tableau, SimplexProblemRequest problem, List<String> columnHeaders) {
        List<ShadowPrice> shadowPrices = new ArrayList<>();
        int zRow = tableau.length - 1;
        int numConstraints = problem.getConstraints().size();

        for (int i = 0; i < numConstraints; i++) {
            String constraintName = "Constraint " + (i + 1);
            SimplexConstraintRequest constraint = problem.getConstraints().get(i);
            if (constraint != null) {
                constraintName += " (" + constraint.getOperator() + " " + constraint.getValue() + ")";
            }

            int slackIndex = findSlackColumnIndex(columnHeaders, i + 1);
            double price = slackIndex >= 0 && slackIndex < tableau[zRow].length ? tableau[zRow][slackIndex] : 0.0;

            ShadowPrice shadowPrice = new ShadowPrice(i, constraintName, price);
            if (Math.abs(price) < EPSILON) {
                shadowPrice.setInterpretation("Shadow price is approximately zero: the constraint is non-binding or redundant.");
            } else if (price > 0) {
                shadowPrice.setInterpretation("Increasing the right-hand side by 1 increases the objective by approximately " + String.format("%.4f", price) + ".");
            } else {
                shadowPrice.setInterpretation("Increasing the right-hand side by 1 decreases the objective by approximately " + String.format("%.4f", Math.abs(price)) + ".");
            }
            shadowPrices.add(shadowPrice);
        }
        return shadowPrices;
    }

    private int findSlackColumnIndex(List<String> columnHeaders, int constraintNumber) {
        if (columnHeaders == null) {
            return -1;
        }
        String slackName = "s" + constraintNumber;
        for (int j = 0; j < columnHeaders.size(); j++) {
            if (slackName.equals(columnHeaders.get(j))) {
                return j;
            }
        }
        return -1;
    }

    private List<RHSRange> calculateRHSRanges(SimplexProblemRequest problem, List<ShadowPrice> shadowPrices) {
        List<RHSRange> rhsRanges = new ArrayList<>();
        for (int i = 0; i < problem.getConstraints().size(); i++) {
            SimplexConstraintRequest constraint = problem.getConstraints().get(i);
            double currentValue = constraint.getValue();
            RHSRange rhsRange = new RHSRange(i, "Constraint " + (i + 1), currentValue);
            rhsRange.setShadowPrice(i < shadowPrices.size() ? shadowPrices.get(i).getPrice() : 0.0);
            rhsRange.setMinRange(Double.NEGATIVE_INFINITY);
            rhsRange.setMaxRange(Double.POSITIVE_INFINITY);
            rhsRange.setInterpretation("RHS range calculation requires tableau basis inverse. Shadow price is available.");
            rhsRanges.add(rhsRange);
        }
        return rhsRanges;
    }

    private DegeneracyWarning detectDegeneracy(SimplexResponse response, SimplexProblemRequest problem) {
        List<String> zeroVars = new ArrayList<>();
        List<Double> solution = response.getOptimalSolution();
        if (solution != null) {
            for (int i = 0; i < solution.size(); i++) {
                if (Math.abs(solution.get(i)) < EPSILON) {
                    zeroVars.add("x" + (i + 1));
                }
            }
        }
        if (zeroVars.isEmpty()) {
            return new DegeneracyWarning(false, null, "No degeneracy detected.", "INFO");
        }
        return new DegeneracyWarning(true, zeroVars, "Solution may be degenerate. Some basic variables are zero.", "WARNING");
    }

    private String generateAnalysisNotes(SensitivityAnalysis analysis, DegeneracyWarning warning) {
        StringBuilder notes = new StringBuilder();
        notes.append("Sensitivity analysis summary:\n");
        if (analysis.getReducedCosts() != null) {
            long positiveRC = analysis.getReducedCosts().stream().filter(rc -> rc.getCost() > EPSILON).count();
            notes.append("- ").append(positiveRC).append(" non-basic variables have positive reduced cost.\n");
        }
        if (analysis.getShadowPrices() != null) {
            long binding = analysis.getShadowPrices().stream().filter(sp -> Math.abs(sp.getPrice()) > EPSILON).count();
            notes.append("- ").append(binding).append(" binding constraints detected by shadow price.\n");
        }
        if (warning.isDegenerateSolution()) {
            notes.append("- WARNING: Degenerate solution detected. Use caution interpreting ranges.\n");
        }
        notes.append("- RHS ranges are currently reported as unbounded because exact basis inverse calculations are not yet enabled.\n");
        return notes.toString();
    }
}
