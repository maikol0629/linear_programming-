package com.lpsolver.backend.service;

import com.lpsolver.backend.model.ConstraintRequest;
import com.lpsolver.backend.model.Point;
import com.lpsolver.backend.model.ProblemRequest;
import com.lpsolver.backend.model.SolutionResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GraphicalSolverService {

    private static final double EPSILON = 1e-6;

    public SolutionResponse solve(ProblemRequest request) {
        SolutionResponse response = new SolutionResponse();
        List<String> steps = new ArrayList<>();
        steps.add("Started solving using Graphical Method.");

        List<ConstraintRequest> allConstraints = new ArrayList<>(request.getConstraints());
        // Implicit non-negativity constraints x >= 0, y >= 0
        ConstraintRequest xGte0 = new ConstraintRequest(); xGte0.setX(1); xGte0.setY(0); xGte0.setOperator(">="); xGte0.setValue(0);
        ConstraintRequest yGte0 = new ConstraintRequest(); yGte0.setX(0); yGte0.setY(1); yGte0.setOperator(">="); yGte0.setValue(0);
        allConstraints.add(xGte0);
        allConstraints.add(yGte0);
        
        steps.add("Added implicit non-negativity constraints: x >= 0, y >= 0.");

        List<Point> intersections = findIntersections(allConstraints);
        steps.add("Found " + intersections.size() + " intersection points between all constraint boundaries.");

        List<Point> feasibleVertices = new ArrayList<>();
        for (Point p : intersections) {
            if (isFeasible(p, allConstraints)) {
                if (!feasibleVertices.contains(p)) {
                    feasibleVertices.add(p);
                }
            }
        }
        steps.add("Filtered intersections to find " + feasibleVertices.size() + " feasible vertices.");
        response.setFeasibleVertices(feasibleVertices);

        if (feasibleVertices.isEmpty()) {
            response.setStatus("INFEASIBLE");
            steps.add("No feasible region found. The problem is INFEASIBLE.");
            response.setSteps(steps);
            return response;
        }

        // Evaluate objective function
        boolean isMax = "MAX".equalsIgnoreCase(request.getObjectiveType());
        Point bestPoint = null;
        double bestValue = isMax ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;

        for (Point p : feasibleVertices) {
            double z = request.getObjX() * p.getX() + request.getObjY() * p.getY();
            p.setZ(z);
            if (isMax) {
                if (z > bestValue) {
                    bestValue = z;
                    bestPoint = p;
                }
            } else {
                if (z < bestValue) {
                    bestValue = z;
                    bestPoint = p;
                }
            }
        }

        response.setOptimalPoint(bestPoint);
        response.setOptimalValue(bestValue);
        response.setStatus("OPTIMAL"); // simplified bounded check for now
        steps.add("Evaluated objective function Z = " + request.getObjX() + "x + " + request.getObjY() + "y at all feasible vertices.");
        steps.add("Found optimal solution Z = " + bestValue + " at point (" + bestPoint.getX() + ", " + bestPoint.getY() + ").");

        response.setSteps(steps);
        return response;
    }

    private List<Point> findIntersections(List<ConstraintRequest> constraints) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < constraints.size(); i++) {
            for (int j = i + 1; j < constraints.size(); j++) {
                Point p = intersect(constraints.get(i), constraints.get(j));
                if (p != null) {
                    points.add(p);
                }
            }
        }
        return points;
    }

    private Point intersect(ConstraintRequest c1, ConstraintRequest c2) {
        // A1x + B1y = C1
        // A2x + B2y = C2
        double a1 = c1.getX(), b1 = c1.getY(), c_1 = c1.getValue();
        double a2 = c2.getX(), b2 = c2.getY(), c_2 = c2.getValue();

        double det = a1 * b2 - a2 * b1;
        if (Math.abs(det) < EPSILON) {
            return null; // Parallel lines
        }

        double x = (c_1 * b2 - c_2 * b1) / det;
        double y = (a1 * c_2 - a2 * c_1) / det;
        return new Point(x, y);
    }

    private boolean isFeasible(Point p, List<ConstraintRequest> constraints) {
        for (ConstraintRequest c : constraints) {
            double val = c.getX() * p.getX() + c.getY() * p.getY();
            if (c.getOperator().equals("<=")) {
                if (val > c.getValue() + EPSILON) return false;
            } else if (c.getOperator().equals(">=")) {
                if (val < c.getValue() - EPSILON) return false;
            } else if (c.getOperator().equals("=")) {
                if (Math.abs(val - c.getValue()) > EPSILON) return false;
            }
        }
        return true;
    }
}
