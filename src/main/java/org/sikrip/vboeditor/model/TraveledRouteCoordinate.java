package org.sikrip.vboeditor.model;


/**
 * Represents a GNSS data point.
 */
public class TraveledRouteCoordinate {

    private final double latitude;
    private final double longitude;
    private final double speed;
    private final long time;
    private final long gpsDataInterval;

    public TraveledRouteCoordinate(double latitude, double longitude, long time, double speed, long gpsDataInterval) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
        this.speed = speed;
        this.gpsDataInterval = gpsDataInterval;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getTime() {
        return time;
    }

    public double getSpeed() {
        return speed;
    }

    public long getGpsDataInterval() {
        return gpsDataInterval;
    }
}
