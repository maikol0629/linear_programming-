package com.lpsolver.backend.service;

import com.lpsolver.backend.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class SimplexSolverService {

    private static final double EPSILON = 1e-6;
    private static final double M = 100000.0; // Big M value

    public SimplexResponse solve(SimplexProblemRequest request) {
        SimplexResponse response = new SimplexResponse();
        List<SimplexStep> steps = new ArrayList<>();

        int numVars = request.getObjectiveCoefficients().size();
        int numConstraints = request.getConstraints().size();

        int numSlacks = 0;
        int numArts = 0;
        
        List<String> colHeaders = new ArrayList<>();
        for (int i = 1; i <= numVars; i++) colHeaders.add("x" + i);

        String[] basicVars = new String[numConstraints];
        int[] slackIndices = new int[numConstraints]; 
        boolean[] hasArt = new boolean[numConstraints];

        for (int i = 0; i < numConstraints; i++) {
            SimplexConstraintRequest c = request.getConstraints().get(i);
            if (c.getValue() < 0) {
                c.setValue(-c.getValue());
                for (int j = 0; j < numVars; j++) c.getCoefficients().set(j, -c.getCoefficients().get(j));
                if (c.getOperator().equals("<=")) c.setOperator(">=");
                else if (c.getOperator().equals(">=")) c.setOperator("<=");
            }

            if (c.getOperator().equals("<=")) {
                numSlacks++;
                colHeaders.add("s" + numSlacks);
                basicVars[i] = "s" + numSlacks;
                slackIndices[i] = numSlacks; 
            } else if (c.getOperator().equals(">=")) {
                numSlacks++;
                numArts++;
                colHeaders.add("s" + numSlacks);
                colHeaders.add("a" + numArts);
                basicVars[i] = "a" + numArts;
                slackIndices[i] = -numSlacks; 
                hasArt[i] = true;
            } else if (c.getOperator().equals("=")) {
                numArts++;
                colHeaders.add("a" + numArts);
                basicVars[i] = "a" + numArts;
                hasArt[i] = true;
            }
        }
        colHeaders.add("RHS");

        int cols = colHeaders.size();
        int rows = numConstraints + 1; 
        double[][] tableau = new double[rows][cols];
        
        for (int i = 0; i < numConstraints; i++) {
            SimplexConstraintRequest c = request.getConstraints().get(i);
            for (int j = 0; j < numVars; j++) tableau[i][j] = c.getCoefficients().get(j);
            
            if (c.getOperator().equals("<=")) {
                int sIdx = colHeaders.indexOf("s" + slackIndices[i]);
                tableau[i][sIdx] = 1.0;
            } else if (c.getOperator().equals(">=")) {
                int sIdx = colHeaders.indexOf("s" + (-slackIndices[i]));
                tableau[i][sIdx] = -1.0;
                int aIdx = colHeaders.indexOf(basicVars[i]);
                tableau[i][aIdx] = 1.0;
            } else if (c.getOperator().equals("=")) {
                int aIdx = colHeaders.indexOf(basicVars[i]);
                tableau[i][aIdx] = 1.0;
            }
            tableau[i][cols - 1] = c.getValue();
        }

        List<String> rowHeaders = new ArrayList<>();
        Collections.addAll(rowHeaders, basicVars);
        rowHeaders.add("Z");

        int iteration = 0;
        boolean isMin = "MIN".equalsIgnoreCase(request.getObjectiveType());
        boolean useBigM = "BIG_M".equalsIgnoreCase(request.getSolverMethod());

        if (numArts > 0 && !useBigM) {
            // TWO PHASE
            steps.add(createStep(iteration, "Phase I: Setting up artificial objective function.", tableau, rowHeaders, colHeaders, null, null));
            
            for (int j = 0; j < cols; j++) tableau[rows - 1][j] = 0;
            for (int i = 0; i < numConstraints; i++) {
                if (hasArt[i]) {
                    int aIdx = colHeaders.indexOf(basicVars[i]);
                    tableau[rows - 1][aIdx] = 1.0;
                }
            }
            
            for (int i = 0; i < numConstraints; i++) {
                if (hasArt[i]) {
                    for (int j = 0; j < cols; j++) tableau[rows - 1][j] -= tableau[i][j];
                }
            }
            steps.add(createStep(++iteration, "Phase I: Row reduced Z row for basic artificial variables.", tableau, rowHeaders, colHeaders, null, null));

            String res = solveTableau(tableau, rows, cols, rowHeaders, colHeaders, steps, iteration, "Phase I");
            if (res.equals("UNBOUNDED")) {
                response.setStatus("UNBOUNDED");
                response.setSteps(steps);
                return response;
            }
            
            iteration = steps.get(steps.size() - 1).getIteration();
            
            if (Math.abs(tableau[rows - 1][cols - 1]) > EPSILON) {
                response.setStatus("INFEASIBLE");
                steps.add(createStep(++iteration, "Phase I optimal Z != 0. The problem is INFEASIBLE.", tableau, rowHeaders, colHeaders, null, null));
                response.setSteps(steps);
                return response;
            }
            
            steps.add(createStep(++iteration, "Phase I completed successfully. Removing artificial variables.", tableau, rowHeaders, colHeaders, null, null));
            
            List<String> newColHeaders = new ArrayList<>();
            List<Integer> keepCols = new ArrayList<>();
            for (int j = 0; j < cols; j++) {
                if (!colHeaders.get(j).startsWith("a")) {
                    newColHeaders.add(colHeaders.get(j));
                    keepCols.add(j);
                }
            }
            
            int newCols = keepCols.size();
            double[][] newTableau = new double[rows][newCols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < newCols; j++) newTableau[i][j] = tableau[i][keepCols.get(j)];
            }
            tableau = newTableau;
            cols = newCols;
            colHeaders = newColHeaders;

            // Phase II Setup
            for (int j = 0; j < cols; j++) tableau[rows - 1][j] = 0;
            for (int j = 0; j < numVars; j++) {
                double c = request.getObjectiveCoefficients().get(j);
                tableau[rows - 1][j] = isMin ? c : -c;
            }
            
            for (int i = 0; i < numConstraints; i++) {
                String basicVar = rowHeaders.get(i);
                int colIdx = colHeaders.indexOf(basicVar);
                if (colIdx != -1) {
                    double factor = tableau[rows - 1][colIdx];
                    if (Math.abs(factor) > EPSILON) {
                        for (int j = 0; j < cols; j++) tableau[rows - 1][j] -= factor * tableau[i][j];
                    }
                }
            }
            
            steps.add(createStep(++iteration, "Phase II: Initial Tableau setup with original objective.", tableau, rowHeaders, colHeaders, null, null));
            res = solveTableau(tableau, rows, cols, rowHeaders, colHeaders, steps, iteration, "Phase II");
            response.setStatus(res);
            
        } else if (numArts > 0 && useBigM) {
            // BIG M
            steps.add(createStep(iteration, "Big M Method: Setting up initial objective with M penalties.", tableau, rowHeaders, colHeaders, null, null));
            
            for (int j = 0; j < cols; j++) tableau[rows - 1][j] = 0;
            for (int j = 0; j < numVars; j++) {
                double c = request.getObjectiveCoefficients().get(j);
                tableau[rows - 1][j] = isMin ? c : -c;
            }
            
            double mCoeff = isMin ? -M : M;
            for (int i = 0; i < numConstraints; i++) {
                if (hasArt[i]) {
                    int aIdx = colHeaders.indexOf(basicVars[i]);
                    tableau[rows - 1][aIdx] = mCoeff;
                }
            }
            
            for (int i = 0; i < numConstraints; i++) {
                if (hasArt[i]) {
                    for (int j = 0; j < cols; j++) tableau[rows - 1][j] -= mCoeff * tableau[i][j];
                }
            }
            steps.add(createStep(++iteration, "Big M Method: Row reduced Z row.", tableau, rowHeaders, colHeaders, null, null));

            String res = solveTableau(tableau, rows, cols, rowHeaders, colHeaders, steps, iteration, "Big M");
            response.setStatus(res);
            
            if ("OPTIMAL".equals(res)) {
                boolean feasible = true;
                for (int i = 0; i < numConstraints; i++) {
                    if (rowHeaders.get(i).startsWith("a") && tableau[i][cols - 1] > EPSILON) {
                        feasible = false;
                        break;
                    }
                }
                if (!feasible) {
                    response.setStatus("INFEASIBLE");
                    steps.add(createStep(++iteration, "Big M optimal Z contains artificial variables. Problem INFEASIBLE.", tableau, rowHeaders, colHeaders, null, null));
                }
            }
            
        } else {
            // Standard
            for (int j = 0; j < cols; j++) tableau[rows - 1][j] = 0;
            for (int j = 0; j < numVars; j++) {
                double c = request.getObjectiveCoefficients().get(j);
                tableau[rows - 1][j] = isMin ? c : -c;
            }
            steps.add(createStep(iteration, "Standard Simplex: Initial Tableau setup.", tableau, rowHeaders, colHeaders, null, null));
            String res = solveTableau(tableau, rows, cols, rowHeaders, colHeaders, steps, iteration, "Simplex");
            response.setStatus(res);
        }

        response.setSteps(steps);

        if ("OPTIMAL".equals(response.getStatus())) {
            List<Double> optimalSolution = new ArrayList<>(Collections.nCopies(numVars, 0.0));
            for (int j = 0; j < numVars; j++) {
                String varName = "x" + (j + 1);
                int rowIndex = rowHeaders.indexOf(varName);
                if (rowIndex != -1) {
                    optimalSolution.set(j, tableau[rowIndex][cols - 1]);
                }
            }
            response.setOptimalSolution(optimalSolution);
            
            double optimalValue = tableau[rows - 1][cols - 1];
            // If Big M, removing any M contribution to Z might be needed if feasible, but if feasible artificials are 0, so Z has no M.
            response.setOptimalValue(isMin ? -optimalValue : optimalValue);
        }

        return response;
    }

    private String solveTableau(double[][] tableau, int rows, int cols, List<String> rowHeaders, List<String> colHeaders, List<SimplexStep> steps, int iteration, String phasePrefix) {
        while (true) {
            int pivotCol = getPivotColumn(tableau, rows, cols);
            if (pivotCol == -1) {
                steps.add(createStep(++iteration, phasePrefix + ": Optimal reached.", tableau, rowHeaders, colHeaders, null, null));
                return "OPTIMAL";
            }

            int pivotRow = getPivotRow(tableau, rows, cols, pivotCol);
            if (pivotRow == -1) {
                steps.add(createStep(++iteration, phasePrefix + ": Problem is unbounded.", tableau, rowHeaders, colHeaders, null, null));
                return "UNBOUNDED";
            }

            String entering = colHeaders.get(pivotCol);
            String leaving = rowHeaders.get(pivotRow);

            SimplexStep prePivotStep = createStep(++iteration, phasePrefix + " Pivot: Entering " + entering + ", Leaving " + leaving, tableau, rowHeaders, colHeaders, pivotRow, pivotCol);
            prePivotStep.setEnteringVariable(entering);
            prePivotStep.setLeavingVariable(leaving);
            steps.add(prePivotStep);

            rowHeaders.set(pivotRow, entering);
            double pivotValue = tableau[pivotRow][pivotCol];
            for (int j = 0; j < cols; j++) tableau[pivotRow][j] /= pivotValue;

            for (int i = 0; i < rows; i++) {
                if (i != pivotRow) {
                    double factor = tableau[i][pivotCol];
                    for (int j = 0; j < cols; j++) tableau[i][j] -= factor * tableau[pivotRow][j];
                }
            }
        }
    }

    private int getPivotColumn(double[][] tableau, int rows, int cols) {
        int bestCol = -1;
        double minVal = -EPSILON;
        for (int j = 0; j < cols - 1; j++) {
            if (tableau[rows - 1][j] < minVal) {
                minVal = tableau[rows - 1][j];
                bestCol = j;
            }
        }
        return bestCol;
    }

    private int getPivotRow(double[][] tableau, int rows, int cols, int pivotCol) {
        int bestRow = -1;
        double minRatio = Double.MAX_VALUE;
        for (int i = 0; i < rows - 1; i++) {
            if (tableau[i][pivotCol] > EPSILON) {
                double ratio = tableau[i][cols - 1] / tableau[i][pivotCol];
                if (ratio >= 0 && ratio < minRatio) {
                    minRatio = ratio;
                    bestRow = i;
                }
            }
        }
        return bestRow;
    }

    private SimplexStep createStep(int iteration, String description, double[][] tab, List<String> rowHeaders, List<String> colHeaders, Integer pivotRow, Integer pivotCol) {
        SimplexStep step = new SimplexStep();
        step.setIteration(iteration);
        step.setDescription(description);
        
        List<List<Double>> tableauList = new ArrayList<>();
        for (double[] row : tab) {
            List<Double> rowList = new ArrayList<>();
            for (double val : row) {
                rowList.add(Math.abs(val) < EPSILON ? 0.0 : val);
            }
            tableauList.add(rowList);
        }
        step.setTableau(tableauList);
        step.setRowHeaders(new ArrayList<>(rowHeaders));
        step.setColumnHeaders(new ArrayList<>(colHeaders));
        step.setPivotRow(pivotRow);
        step.setPivotCol(pivotCol);
        return step;
    }
}
