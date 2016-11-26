package org.sikrip.vboeditor.model;


public class TraveledRoutePoint {

    private final int x;
    private final int y;
    private final long time;
    private final double speed;


    public TraveledRoutePoint(int x, int y, long time, double speed) {
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

    public long getTime() {
        return time;
    }

    public double getSpeed() {
        return speed;
    }
}
