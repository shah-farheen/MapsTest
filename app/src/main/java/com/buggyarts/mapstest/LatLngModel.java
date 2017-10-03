package com.buggyarts.mapstest;

/**
 * Created by farheen on 25/9/17
 */

public class LatLngModel {
    private double lat;
    private double lng;
    private double power;
    private long time;

    public LatLngModel() {

    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "LatLngModel{" +
                "lat=" + lat +
                ", lng=" + lng +
                ", power=" + power +
                ", time=" + time +
                '}';
    }
}
