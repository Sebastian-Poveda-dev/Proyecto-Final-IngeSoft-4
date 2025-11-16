package com.mio.model;

/**
 * Representa una parada del sistema MIO
 */
public class Stop {
    private int stopId;
    private int planVersionId;
    private String shortName;
    private String longName;
    private double gpsX;
    private double gpsY;
    private double decimalLongitude;
    private double decimalLatitude;

    public Stop(int stopId, int planVersionId, String shortName, String longName, 
                double gpsX, double gpsY, double decimalLongitude, double decimalLatitude) {
        this.stopId = stopId;
        this.planVersionId = planVersionId;
        this.shortName = shortName;
        this.longName = longName;
        this.gpsX = gpsX;
        this.gpsY = gpsY;
        this.decimalLongitude = decimalLongitude;
        this.decimalLatitude = decimalLatitude;
    }

    public int getStopId() {
        return stopId;
    }

    public int getPlanVersionId() {
        return planVersionId;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    public double getGpsX() {
        return gpsX;
    }

    public double getGpsY() {
        return gpsY;
    }

    public double getDecimalLongitude() {
        return decimalLongitude;
    }

    public double getDecimalLatitude() {
        return decimalLatitude;
    }

    @Override
    public String toString() {
        return "Stop{" +
                "stopId=" + stopId +
                ", shortName='" + shortName + '\'' +
                ", longName='" + longName + '\'' +
                '}';
    }
}
