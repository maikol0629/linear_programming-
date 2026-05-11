package com.lpsolver.backend.model;

import java.util.List;

public class SimplexProblemRequest {
    private String objectiveType; // MAX or MIN
    private List<Double> objectiveCoefficients;
    private List<SimplexConstraintRequest> constraints;
    private String solverMethod = "TWO_PHASE"; // "TWO_PHASE" or "BIG_M"

    public String getObjectiveType() { return objectiveType; }
    public void setObjectiveType(String objectiveType) { this.objectiveType = objectiveType; }

    public List<Double> getObjectiveCoefficients() { return objectiveCoefficients; }
    public void setObjectiveCoefficients(List<Double> objectiveCoefficients) { this.objectiveCoefficients = objectiveCoefficients; }

    public List<SimplexConstraintRequest> getConstraints() { return constraints; }
    public void setConstraints(List<SimplexConstraintRequest> constraints) { this.constraints = constraints; }

    public String getSolverMethod() { return solverMethod; }
    public void setSolverMethod(String solverMethod) { this.solverMethod = solverMethod; }
}
