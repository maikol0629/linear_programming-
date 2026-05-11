package com.lpsolver.backend.model;

import java.util.List;

public class ProblemRequest {
    private String objectiveType; // MAX or MIN
    private double objX;
    private double objY;
    private List<ConstraintRequest> constraints;

    public String getObjectiveType() { return objectiveType; }
    public void setObjectiveType(String objectiveType) { this.objectiveType = objectiveType; }

    public double getObjX() { return objX; }
    public void setObjX(double objX) { this.objX = objX; }

    public double getObjY() { return objY; }
    public void setObjY(double objY) { this.objY = objY; }

    public List<ConstraintRequest> getConstraints() { return constraints; }
    public void setConstraints(List<ConstraintRequest> constraints) { this.constraints = constraints; }
}
