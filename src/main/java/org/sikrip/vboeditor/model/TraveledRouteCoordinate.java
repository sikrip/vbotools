package org.sikrip.vboeditor.model;


public class TraveledRouteCoordinate {

    private final double latitude;
    private final double longitude;
    private final double speed;
    private final double time;

    public TraveledRouteCoordinate(double latitude, double longitude, double time, double speed) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
        this.speed = speed;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getTime() {
        return time;
    }

    public double getSpeed() {
        return speed;
    }
}
