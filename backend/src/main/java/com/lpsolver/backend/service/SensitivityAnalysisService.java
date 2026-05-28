package com.lpsolver.backend.service;

import com.lpsolver.backend.model.*;
import org.apache.commons.math3.linear.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SensitivityAnalysisService {

    private static final double EPSILON = 1e-6;

    public SensitivityAnalysis analyzeSensitivity(SimplexResponse response, SimplexProblemRequest problem) {
        if (response == null) {
            throw new IllegalArgumentException("Se requiere una respuesta simplex para el análisis de sensibilidad.");
        }
        if (!"OPTIMAL".equals(response.getStatus())) {
            throw new IllegalStateException("El análisis de sensibilidad requiere una solución ÓPTIMA. Estado actual: " + response.getStatus());
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
            throw new IllegalStateException("No se pudieron extraer los datos del tablero final para el análisis de sensibilidad.");
        }

        List<ReducedCost> reducedCosts = extractReducedCosts(tableau, problem, columnHeaders);
        analysis.setReducedCosts(reducedCosts);

        int numVars = problem.getObjectiveCoefficients().size();
        int numConstraints = problem.getConstraints().size();

        double[][] body = extractBody(tableau, numConstraints);
        double[] rhs = extractRhsColumn(tableau, numConstraints);
        double[] zRow = tableau[tableau.length - 1];

        RealMatrix B_inv = extractBInverse(body, columnHeaders, problem);

        List<ObjectiveCoeffRange> objectiveRanges = calculateObjectiveRanges(
                problem, reducedCosts, body, zRow, rowHeaders, columnHeaders,
                numConstraints, numVars);
        analysis.setObjectiveCoefficientsRanges(objectiveRanges);

        List<ShadowPrice> shadowPrices = extractShadowPrices(tableau, problem, columnHeaders);
        analysis.setShadowPrices(shadowPrices);

        List<RHSRange> rhsRanges = calculateRHSRanges(problem, shadowPrices, B_inv, rhs);
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

    private double[][] extractBody(double[][] tableau, int numConstraints) {
        int cols = tableau[0].length - 1;
        double[][] body = new double[numConstraints][cols];
        for (int i = 0; i < numConstraints; i++) {
            System.arraycopy(tableau[i], 0, body[i], 0, cols);
        }
        return body;
    }

    private double[] extractRhsColumn(double[][] tableau, int numConstraints) {
        int rhsCol = tableau[0].length - 1;
        double[] rhs = new double[numConstraints];
        for (int i = 0; i < numConstraints; i++) {
            rhs[i] = tableau[i][rhsCol];
        }
        return rhs;
    }

    private RealMatrix extractBInverse(double[][] body, List<String> columnHeaders, SimplexProblemRequest problem) {
        int m = problem.getConstraints().size();
        RealMatrix B_inv = MatrixUtils.createRealMatrix(m, m);

        int slackNumber = 0;
        for (int i = 0; i < m; i++) {
            SimplexConstraintRequest constraint = problem.getConstraints().get(i);
            if (!"=".equals(constraint.getOperator())) {
                slackNumber++;
                String slackName = "s" + slackNumber;
                int colIdx = columnHeaders.indexOf(slackName);
                if (colIdx >= 0) {
                    double[] colData = new double[m];
                    for (int r = 0; r < m; r++) {
                        colData[r] = body[r][colIdx];
                    }
                    if (">=".equals(constraint.getOperator())) {
                        RealVector colVec = MatrixUtils.createRealVector(colData);
                        B_inv.setColumnVector(i, colVec.mapMultiply(-1.0));
                    } else {
                        B_inv.setColumn(i, colData);
                    }
                }
            }
        }

        return B_inv;
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
                rc.setInterpretation("El costo reducido es aproximadamente cero; la variable es básica o existen soluciones óptimas alternativas.");
            } else if (costValue > 0) {
                rc.setInterpretation("Costo reducido positivo: aumentar este coeficiente eventualmente cambiará la base actual.");
            } else {
                rc.setInterpretation("Costo reducido negativo: la solución actual no es óptima para esta variable.");
            }
            reducedCosts.add(rc);
        }
        return reducedCosts;
    }

    private List<ObjectiveCoeffRange> calculateObjectiveRanges(
            SimplexProblemRequest problem, List<ReducedCost> reducedCosts,
            double[][] body, double[] zRow, List<String> rowHeaders,
            List<String> columnHeaders, int numConstraints, int numVars) {

        List<ObjectiveCoeffRange> ranges = new ArrayList<>();
        List<Double> objectiveCoefficients = problem.getObjectiveCoefficients();

        Map<String, Integer> basicRowMap = new HashMap<>();
        for (int r = 0; r < rowHeaders.size(); r++) {
            basicRowMap.put(rowHeaders.get(r), r);
        }

        for (int i = 0; i < objectiveCoefficients.size(); i++) {
            String varName = "x" + (i + 1);
            double currentValue = objectiveCoefficients.get(i);
            ReducedCost rc = reducedCosts.get(i);
            ObjectiveCoeffRange range = new ObjectiveCoeffRange(varName, i, currentValue);
            range.setReducedCost(rc.getCost());

            if (Math.abs(rc.getCost()) < EPSILON) {
                range.setBasic(true);
                Integer basicRow = basicRowMap.get(varName);
                if (basicRow == null) {
                    range.setInterpretation("Variable básica: no se pudo determinar la fila en la base. Los rangos son infinitos.");
                    ranges.add(range);
                    continue;
                }
                double minD = Double.NEGATIVE_INFINITY;
                double maxD = Double.POSITIVE_INFINITY;
                int cols = columnHeaders.size() - 1;
                for (int j = 0; j < cols; j++) {
                    String colName = columnHeaders.get(j);
                    if (basicRowMap.containsKey(colName)) continue;
                    double bodyEntry = body[basicRow][j];
                    if (Math.abs(bodyEntry) < EPSILON) continue;
                    double ratio = -zRow[j] / bodyEntry;
                    if (bodyEntry > 0) {
                        if (ratio > minD) minD = ratio;
                    } else {
                        if (ratio < maxD) maxD = ratio;
                    }
                }
                double cMin = Double.isInfinite(minD) ? Double.NEGATIVE_INFINITY : currentValue + minD;
                double cMax = Double.isInfinite(maxD) ? Double.POSITIVE_INFINITY : currentValue + maxD;
                range.setMinRange(cMin);
                range.setMaxRange(cMax);
                range.setAllowedDecrease(Double.isInfinite(cMin) ? Double.POSITIVE_INFINITY : currentValue - cMin);
                range.setAllowedIncrease(Double.isInfinite(cMax) ? Double.POSITIVE_INFINITY : cMax - currentValue);
                String dMinStr = Double.isInfinite(minD) ? "-∞" : String.format("%.4f", minD);
                String dMaxStr = Double.isInfinite(maxD) ? "∞" : String.format("%.4f", maxD);
                range.setInterpretation("Variable básica: el coeficiente puede variar entre " +
                        formatNum(cMin) + " y " + formatNum(cMax) +
                        " sin cambiar la base óptima. (Δ ∈ [" + dMinStr + ", " + dMaxStr + "])");
            } else {
                range.setBasic(false);
                range.setMinRange(Double.NEGATIVE_INFINITY);
                range.setAllowedDecrease(Double.POSITIVE_INFINITY);
                range.setAllowedIncrease(Math.abs(rc.getCost()));
                range.setMaxRange(currentValue + Math.abs(rc.getCost()));
                range.setInterpretation("Variable no básica: la base actual se mantiene óptima mientras el coeficiente no aumente más de " +
                        String.format("%.4f", Math.abs(rc.getCost())) + ".");
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
            String constraintName = "Restricción " + (i + 1);
            SimplexConstraintRequest constraint = problem.getConstraints().get(i);
            if (constraint != null) {
                constraintName += " (" + constraint.getOperator() + " " + constraint.getValue() + ")";
            }

            int slackIndex = findSlackColumnIndex(columnHeaders, i + 1);
            double price = slackIndex >= 0 && slackIndex < tableau[zRow].length ? tableau[zRow][slackIndex] : 0.0;

            ShadowPrice shadowPrice = new ShadowPrice(i, constraintName, price);
            if (Math.abs(price) < EPSILON) {
                shadowPrice.setInterpretation("El precio sombra es aproximadamente cero: la restricción no está activa o es redundante.");
            } else if (price > 0) {
                shadowPrice.setInterpretation("Aumentar el lado derecho en 1 incrementa el objetivo en aproximadamente " + String.format("%.4f", price) + ".");
            } else {
                shadowPrice.setInterpretation("Aumentar el lado derecho en 1 disminuye el objetivo en aproximadamente " + String.format("%.4f", Math.abs(price)) + ".");
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

    private List<RHSRange> calculateRHSRanges(SimplexProblemRequest problem, List<ShadowPrice> shadowPrices,
                                               RealMatrix B_inv, double[] rhs) {
        List<RHSRange> rhsRanges = new ArrayList<>();
        int m = problem.getConstraints().size();

        for (int i = 0; i < m; i++) {
            SimplexConstraintRequest constraint = problem.getConstraints().get(i);
            double currentValue = constraint.getValue();
            RHSRange rhsRange = new RHSRange(i, "Restricción " + (i + 1), currentValue);
            rhsRange.setShadowPrice(i < shadowPrices.size() ? shadowPrices.get(i).getPrice() : 0.0);

            if ("=".equals(constraint.getOperator())) {
                rhsRange.setMinRange(currentValue);
                rhsRange.setMaxRange(currentValue);
                rhsRange.setInterpretation("Restricción de igualdad: el RHS no puede cambiar sin perder factibilidad.");
                rhsRanges.add(rhsRange);
                continue;
            }

            double minD = Double.NEGATIVE_INFINITY;
            double maxD = Double.POSITIVE_INFINITY;

            for (int r = 0; r < m; r++) {
                double entry = B_inv.getEntry(r, i);
                if (Math.abs(entry) < EPSILON) continue;
                double ratio = -rhs[r] / entry;
                if (entry > 0) {
                    minD = Math.max(minD, ratio);
                } else {
                    maxD = Math.min(maxD, ratio);
                }
            }

            double cMin = Double.isInfinite(minD) ? Double.NEGATIVE_INFINITY : currentValue + minD;
            double cMax = Double.isInfinite(maxD) ? Double.POSITIVE_INFINITY : currentValue + maxD;

            rhsRange.setMinRange(cMin);
            rhsRange.setMaxRange(cMax);

            rhsRange.setInterpretation("El RHS puede variar entre " + formatNum(cMin) +
                    " y " + formatNum(cMax) +
                    " sin cambiar la base óptima.");

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
            return new DegeneracyWarning(false, null, "No se detectó degeneración.", "INFO");
        }
        return new DegeneracyWarning(true, zeroVars, "La solución puede ser degenerada. Algunas variables básicas son cero.", "WARNING");
    }

    private String generateAnalysisNotes(SensitivityAnalysis analysis, DegeneracyWarning warning) {
        StringBuilder notes = new StringBuilder();
        notes.append("Resumen del análisis de sensibilidad:\n");
        if (analysis.getObjectiveCoefficientsRanges() != null) {
            long basicWithFinite = analysis.getObjectiveCoefficientsRanges().stream()
                    .filter(r -> r.isBasic() && Double.isFinite(r.getMinRange()) && Double.isFinite(r.getMaxRange()))
                    .count();
            long nonBasic = analysis.getObjectiveCoefficientsRanges().stream()
                    .filter(r -> !r.isBasic())
                    .count();
            notes.append("- ").append(basicWithFinite).append(" variables básicas con rangos calculados.\n");
            notes.append("- ").append(nonBasic).append(" variables no básicas con rangos calculados.\n");
        }
        if (analysis.getShadowPrices() != null) {
            long binding = analysis.getShadowPrices().stream().filter(sp -> Math.abs(sp.getPrice()) > EPSILON).count();
            notes.append("- ").append(binding).append(" restricciones activas detectadas por precio sombra.\n");
        }
        if (analysis.getRhsRanges() != null) {
            long withFiniteRange = analysis.getRhsRanges().stream()
                    .filter(r -> Double.isFinite(r.getMinRange()) || Double.isFinite(r.getMaxRange()))
                    .count();
            notes.append("- ").append(withFiniteRange).append(" restricciones con rangos RHS calculados mediante B⁻¹.\n");
        }
        if (warning.isDegenerateSolution()) {
            notes.append("- ADVERTENCIA: Solución degenerada detectada. Tenga cuidado al interpretar los rangos.\n");
        }
        return notes.toString();
    }

    private String formatNum(double d) {
        if (Double.isInfinite(d)) {
            return d > 0 ? "∞" : "-∞";
        }
        return String.format("%.4f", d);
    }
}
