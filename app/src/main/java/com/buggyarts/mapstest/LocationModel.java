package com.buggyarts.mapstest;

import android.location.Location;

/**
 * Created by farheen on 3/10/17
 */

public class LocationModel {
    private double lat;
    private double lng;
    private float speed;
    private float bearing;
    private long time;

    public LocationModel() {

    }

    public LocationModel(LocationModel locationModel){
        this.lat = locationModel.getLat();
        this.lng = locationModel.getLng();
        this.speed = locationModel.getSpeed();
        this.bearing = locationModel.getBearing();
        this.time = locationModel.getTime();
    }

    public LocationModel(Location location){
        this.lat = location.getLatitude();
        this.lng = location.getLongitude();
        this.speed = location.getSpeed();
        this.bearing = location.getBearing();
        this.time = location.getTime();
    }

    public void updateModel(Location location){
        this.lat = location.getLatitude();
        this.lng = location.getLongitude();
        this.speed = location.getSpeed();
        this.bearing = location.getBearing();
        this.time = location.getTime();
    }

    public void updateModel(LocationModel locationModel){
        this.lat = locationModel.getLat();
        this.lng = locationModel.getLng();
        this.speed = locationModel.getSpeed();
        this.bearing = locationModel.getBearing();
        this.time = locationModel.getTime();
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

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getBearing() {
        return bearing;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "LocationModel{" +
                "lat=" + lat +
                ", lng=" + lng +
                ", speed=" + speed +
                ", bearing=" + bearing +
                ", time=" + time +
                '}';
    }
}
