package com.lpsolver.backend.service;

import com.lpsolver.backend.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GraphicalSensitivityAnalysisService {

    private static final double EPSILON = 1e-6;
    private final GraphicalSolverService graphicalSolverService;

    public GraphicalSensitivityAnalysisService(GraphicalSolverService graphicalSolverService) {
        this.graphicalSolverService = graphicalSolverService;
    }

    public SensitivityAnalysis analyzeSensitivity(ProblemRequest request, SolutionResponse solution) {
        if (request == null) {
            throw new IllegalArgumentException("Problem request is required for graphical sensitivity analysis.");
        }
        if (solution == null) {
            throw new IllegalArgumentException("Graphical solution is required for sensitivity analysis.");
        }
        if (!"OPTIMAL".equals(solution.getStatus())) {
            throw new IllegalStateException("Sensitivity analysis requires an OPTIMAL graphical solution. Current status: " + solution.getStatus());
        }

        SensitivityAnalysis analysis = new SensitivityAnalysis();
        analysis.setObjectiveCoefficientsRanges(calculateObjectiveCoefficientRanges(request, solution));
        analysis.setShadowPrices(calculateShadowPrices(request, solution));
        analysis.setRhsRanges(calculateRhsRanges(request, solution));
        analysis.setReducedCosts(new ArrayList<>());
        analysis.setDegeneracyWarning(detectDegeneracy(request, solution));
        analysis.setAnalysisNotes(generateAnalysisNotes(analysis));
        return analysis;
    }

    private List<ObjectiveCoeffRange> calculateObjectiveCoefficientRanges(ProblemRequest request, SolutionResponse solution) {
        List<ObjectiveCoeffRange> ranges = new ArrayList<>();
        if (solution.getOptimalPoint() == null || solution.getFeasibleVertices() == null) {
            return ranges;
        }

        Point optimal = solution.getOptimalPoint();
        double objX = request.getObjX();
        double objY = request.getObjY();
        boolean isMax = "MAX".equalsIgnoreCase(request.getObjectiveType());
        List<Point> vertices = solution.getFeasibleVertices();

        ObjectiveCoeffRange xRange = calculateCoefficientRange(
                "x", 0, objX, objY, optimal, vertices, isMax);
        ObjectiveCoeffRange yRange = calculateCoefficientRange(
                "y", 1, objY, objX, optimal, vertices, isMax);

        ranges.add(xRange);
        ranges.add(yRange);
        return ranges;
    }

    private ObjectiveCoeffRange calculateCoefficientRange(String variableName, int variableIndex, double currentValue,
                                                          double fixedValue, Point optimal, List<Point> vertices, boolean isMax) {
        double minRange = Double.NEGATIVE_INFINITY;
        double maxRange = Double.POSITIVE_INFINITY;

        for (Point vertex : vertices) {
            if (vertex == optimal || almostEqual(vertex.getX(), optimal.getX()) && almostEqual(vertex.getY(), optimal.getY())) {
                continue;
            }

            double deltaVar;
            double deltaFixed;
            if (variableIndex == 0) {
                deltaVar = optimal.getX() - vertex.getX();
                deltaFixed = optimal.getY() - vertex.getY();
            } else {
                deltaVar = optimal.getY() - vertex.getY();
                deltaFixed = optimal.getX() - vertex.getX();
            }

            if (almostEqual(deltaVar, 0.0)) {
                continue;
            }

            double boundary = -fixedValue * deltaFixed / deltaVar;
            boolean positiveDirection = deltaVar > 0;
            if (isMax) {
                if (positiveDirection) {
                    minRange = Math.max(minRange, boundary);
                } else {
                    maxRange = Math.min(maxRange, boundary);
                }
            } else {
                if (positiveDirection) {
                    maxRange = Math.min(maxRange, boundary);
                } else {
                    minRange = Math.max(minRange, boundary);
                }
            }
        }

        ObjectiveCoeffRange range = new ObjectiveCoeffRange(variableName, variableIndex, currentValue);
        range.setBasic(false);
        range.setReducedCost(0.0);
        range.setMinRange(minRange);
        range.setMaxRange(maxRange);
        range.setAllowedDecrease(Double.isInfinite(minRange) ? Double.POSITIVE_INFINITY : currentValue - minRange);
        range.setAllowedIncrease(Double.isInfinite(maxRange) ? Double.POSITIVE_INFINITY : maxRange - currentValue);
        range.setInterpretation("Allowed change for coefficient " + variableName + " while the current optimal vertex remains best.");
        return range;
    }

    private List<ShadowPrice> calculateShadowPrices(ProblemRequest request, SolutionResponse solution) {
        List<ShadowPrice> shadowPrices = new ArrayList<>();
        if (solution.getOptimalPoint() == null) {
            return shadowPrices;
        }

        List<ConstraintWrapper> wrappers = buildConstraintWrappers(request);
        List<ConstraintWrapper> active = new ArrayList<>();

        for (ConstraintWrapper wrapper : wrappers) {
            if (isConstraintBinding(wrapper.constraint, solution.getOptimalPoint())) {
                active.add(wrapper);
            }
        }

        double[] multipliers = null;
        ConstraintWrapper first = null;
        ConstraintWrapper second = null;
        for (int i = 0; i < active.size() && multipliers == null; i++) {
            for (int j = i + 1; j < active.size(); j++) {
                multipliers = solveDualMultipliers(active.get(i).constraint, active.get(j).constraint,
                        request.getObjX(), request.getObjY());
                if (multipliers != null) {
                    first = active.get(i);
                    second = active.get(j);
                    break;
                }
            }
        }

        for (int i = 0; i < request.getConstraints().size(); i++) {
            ConstraintRequest constraint = request.getConstraints().get(i);
            double price = 0.0;
            String name = "Constraint " + (i + 1) + " (" + constraint.getOperator() + " " + constraint.getValue() + ")";
            if (first != null && second != null && first.index == i) {
                price = multipliers[0];
            } else if (first != null && second != null && second.index == i) {
                price = multipliers[1];
            }

            ShadowPrice shadowPrice = new ShadowPrice(i, name, price);
            shadowPrice.setInterpretation(buildShadowInterpretation(price, request.getObjectiveType()));
            shadowPrices.add(shadowPrice);
        }

        return shadowPrices;
    }

    private String buildShadowInterpretation(double price, String objectiveType) {
        if (almostEqual(price, 0.0)) {
            return "Shadow price is approximately zero: the constraint is not binding in the local optimal region.";
        }
        if (price > 0) {
            return "A small loosening of this constraint changes the objective by approximately " + String.format("%.4f", price) + " units.";
        }
        return "A small loosening of this constraint changes the objective by approximately -" + String.format("%.4f", Math.abs(price)) + " units.";
    }

    private double[] solveDualMultipliers(ConstraintRequest c1, ConstraintRequest c2, double objX, double objY) {
        double a1 = c1.getX();
        double b1 = c1.getY();
        double a2 = c2.getX();
        double b2 = c2.getY();
        double det = a1 * b2 - a2 * b1;
        if (almostEqual(det, 0.0)) {
            return null;
        }
        double lambda1 = (objX * b2 - objY * a2) / det;
        double lambda2 = (a1 * objY - objX * b1) / det;
        return new double[]{lambda1, lambda2};
    }

    private List<RHSRange> calculateRhsRanges(ProblemRequest request, SolutionResponse solution) {
        List<RHSRange> rhsRanges = new ArrayList<>();
        if (solution.getOptimalPoint() == null) {
            return rhsRanges;
        }

        for (int i = 0; i < request.getConstraints().size(); i++) {
            ConstraintRequest constraint = request.getConstraints().get(i);
            RHSRange rhsRange = new RHSRange(i, "Constraint " + (i + 1), constraint.getValue());
            rhsRange.setShadowPrice(0.0);
            rhsRange.setInterpretation("Calculated sensitivity range for the right-hand side of this constraint.");
            if ("=".equals(constraint.getOperator())) {
                rhsRange.setMinRange(constraint.getValue());
                rhsRange.setMaxRange(constraint.getValue());
            } else {
                double[] bounds = findRhsBounds(request, solution, i);
                rhsRange.setMinRange(bounds[0]);
                rhsRange.setMaxRange(bounds[1]);
            }
            rhsRanges.add(rhsRange);
        }

        return rhsRanges;
    }

    private double[] findRhsBounds(ProblemRequest request, SolutionResponse solution, int constraintIndex) {
        ConstraintRequest constraint = request.getConstraints().get(constraintIndex);
        double currentRhs = constraint.getValue();
        Point optimal = solution.getOptimalPoint();
        List<Double> candidates = generateCandidateRhsValues(request, constraintIndex, optimal);
        double lower = Double.NEGATIVE_INFINITY;
        double upper = Double.POSITIVE_INFINITY;

        if ("<=".equals(constraint.getOperator())) {
            double requiredRhs = constraint.getX() * optimal.getX() + constraint.getY() * optimal.getY();
            lower = requiredRhs;
            upper = searchBoundary(request, optimal, constraintIndex, candidates, currentRhs, true);
        } else if (">=".equals(constraint.getOperator())) {
            double requiredRhs = constraint.getX() * optimal.getX() + constraint.getY() * optimal.getY();
            upper = requiredRhs;
            lower = searchBoundary(request, optimal, constraintIndex, candidates, currentRhs, false);
        }

        if (Double.isNaN(lower)) {
            lower = Double.NEGATIVE_INFINITY;
        }
        if (Double.isNaN(upper)) {
            upper = Double.POSITIVE_INFINITY;
        }
        return new double[]{lower, upper};
    }

    private double searchBoundary(ProblemRequest request, Point optimal, int constraintIndex,
                                  List<Double> candidates, double currentValue, boolean searchAbove) {
        Collections.sort(candidates);
        if (searchAbove) {
            for (double candidate : candidates) {
                if (candidate <= currentValue + EPSILON) continue;
                double probe = currentValue + (candidate - currentValue) / 2.0;
                if (!sameOptimalPoint(graphicalSolverService.solve(cloneRequestWithRhs(request, constraintIndex, probe)), optimal)) {
                    return candidate;
                }
            }
            return Double.POSITIVE_INFINITY;
        }
        for (int index = candidates.size() - 1; index >= 0; index--) {
            double candidate = candidates.get(index);
            if (candidate >= currentValue - EPSILON) continue;
            double probe = candidate + (currentValue - candidate) / 2.0;
            if (!sameOptimalPoint(graphicalSolverService.solve(cloneRequestWithRhs(request, constraintIndex, probe)), optimal)) {
                return candidate;
            }
        }
        return Double.NEGATIVE_INFINITY;
    }

    private List<Double> generateCandidateRhsValues(ProblemRequest request, int constraintIndex, Point optimal) {
        Set<Double> values = new HashSet<>();
        List<ConstraintWrapper> wrappers = buildConstraintWrappers(request);
        ConstraintWrapper target = wrappers.stream().filter(wrapper -> wrapper.index == constraintIndex).findFirst().orElse(null);
        if (target == null) {
            return new ArrayList<>();
        }

        values.add(target.constraint.getValue());
        values.add(target.constraint.getX() * optimal.getX() + target.constraint.getY() * optimal.getY());

        for (ConstraintWrapper other : wrappers) {
            if (other.index == target.index) {
                continue;
            }
            Point intersection = intersect(target.constraint, other.constraint);
            if (intersection != null) {
                double rhs = target.constraint.getX() * intersection.getX() + target.constraint.getY() * intersection.getY();
                values.add(rhs);
            }
        }

        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());
        return sorted;
    }

    private boolean sameOptimalPoint(SolutionResponse candidate, Point optimal) {
        if (candidate == null || candidate.getOptimalPoint() == null || !"OPTIMAL".equals(candidate.getStatus())) {
            return false;
        }
        return almostEqual(candidate.getOptimalPoint().getX(), optimal.getX())
                && almostEqual(candidate.getOptimalPoint().getY(), optimal.getY());
    }

    private List<ConstraintWrapper> buildConstraintWrappers(ProblemRequest request) {
        List<ConstraintWrapper> wrappers = new ArrayList<>();
        if (request.getConstraints() != null) {
            for (int i = 0; i < request.getConstraints().size(); i++) {
                ConstraintRequest constraint = request.getConstraints().get(i);
                wrappers.add(new ConstraintWrapper(i, "Constraint " + (i + 1) + " (" + constraint.getOperator() + " " + constraint.getValue() + ")", constraint, false));
            }
        }
        ConstraintRequest xGte0 = new ConstraintRequest();
        xGte0.setX(1); xGte0.setY(0); xGte0.setOperator(">="); xGte0.setValue(0);
        wrappers.add(new ConstraintWrapper(wrappers.size(), "x >= 0", xGte0, true));

        ConstraintRequest yGte0 = new ConstraintRequest();
        yGte0.setX(0); yGte0.setY(1); yGte0.setOperator(">="); yGte0.setValue(0);
        wrappers.add(new ConstraintWrapper(wrappers.size(), "y >= 0", yGte0, true));

        return wrappers;
    }

    private ProblemRequest cloneRequestWithRhs(ProblemRequest request, int constraintIndex, double newValue) {
        ProblemRequest clone = new ProblemRequest();
        clone.setObjectiveType(request.getObjectiveType());
        clone.setObjX(request.getObjX());
        clone.setObjY(request.getObjY());
        List<ConstraintRequest> clonedConstraints = new ArrayList<>();
        if (request.getConstraints() != null) {
            for (int i = 0; i < request.getConstraints().size(); i++) {
                ConstraintRequest original = request.getConstraints().get(i);
                ConstraintRequest copy = new ConstraintRequest();
                copy.setX(original.getX());
                copy.setY(original.getY());
                copy.setOperator(original.getOperator());
                copy.setValue(i == constraintIndex ? newValue : original.getValue());
                clonedConstraints.add(copy);
            }
        }
        clone.setConstraints(clonedConstraints);
        return clone;
    }

    private Point intersect(ConstraintRequest c1, ConstraintRequest c2) {
        double a1 = c1.getX();
        double b1 = c1.getY();
        double c_1 = c1.getValue();
        double a2 = c2.getX();
        double b2 = c2.getY();
        double c_2 = c2.getValue();
        double det = a1 * b2 - a2 * b1;
        if (almostEqual(det, 0.0)) {
            return null;
        }
        double x = (c_1 * b2 - c_2 * b1) / det;
        double y = (a1 * c_2 - a2 * c_1) / det;
        Point point = new Point(x, y);
        point.setZ(0.0);
        return point;
    }

    private boolean isConstraintBinding(ConstraintRequest constraint, Point point) {
        double lhs = constraint.getX() * point.getX() + constraint.getY() * point.getY();
        return almostEqual(lhs, constraint.getValue());
    }

    private DegeneracyWarning detectDegeneracy(ProblemRequest request, SolutionResponse solution) {
        List<ConstraintWrapper> wrappers = buildConstraintWrappers(request);
        int activeCount = 0;
        for (ConstraintWrapper wrapper : wrappers) {
            if (isConstraintBinding(wrapper.constraint, solution.getOptimalPoint())) {
                activeCount++;
            }
        }
        if (activeCount > 2) {
            return new DegeneracyWarning(true, List.of("Multiple active constraints at the optimal vertex"), "Degenerate graphical solution detected because more than two constraints are active at the optimal point.", "WARNING");
        }
        return new DegeneracyWarning(false, new ArrayList<>(), "No degeneracy detected in the graphical optimum.", "INFO");
    }

    private String generateAnalysisNotes(SensitivityAnalysis analysis) {
        StringBuilder notes = new StringBuilder();
        notes.append("Graphical sensitivity analysis summary:\n");
        notes.append("- Objective coefficient ranges are computed fixing the other coefficient.\n");
        notes.append("- RHS ranges are based on maintaining the current optimal vertex while the constraint boundary moves.\n");
        if (analysis.getShadowPrices() != null) {
            long binding = analysis.getShadowPrices().stream().filter(sp -> Math.abs(sp.getPrice()) > EPSILON).count();
            notes.append("- ").append(binding).append(" binding constraint(s) contribute non-zero shadow prices.\n");
        }
        if (analysis.getDegeneracyWarning() != null && analysis.getDegeneracyWarning().isDegenerateSolution()) {
            notes.append("- WARNING: Degenerate solution detected. Some ranges may be less stable.\n");
        }
        return notes.toString();
    }

    private boolean almostEqual(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }

    private static class ConstraintWrapper {
        final int index;
        final String name;
        final ConstraintRequest constraint;
        final boolean implicit;

        ConstraintWrapper(int index, String name, ConstraintRequest constraint, boolean implicit) {
            this.index = index;
            this.name = name;
            this.constraint = constraint;
            this.implicit = implicit;
        }
    }
}
