package com.lpsolver.backend.model;

public class Point {
    private double x;
    private double y;
    private double z; // objective value at this point

    public Point() {}

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Point point = (Point) obj;
        return Math.abs(point.x - x) < 1e-6 && Math.abs(point.y - y) < 1e-6;
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(Math.round(x * 1000), Math.round(y * 1000));
    }
}
