package org.sikrip.vboeditor.model;


public class TraveledRoutePoint {

    private final int x;
    private final int y;
    private final double time;
    private final double speed;


    public TraveledRoutePoint(int x, int y, double time, double speed) {
        this.x = x;
        this.y = y;
        this.time = time;
        this.speed = speed;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double getTime() {
        return time;
    }

    public double getSpeed() {
        return speed;
    }
}
