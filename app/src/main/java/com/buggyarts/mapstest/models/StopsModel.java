package com.buggyarts.mapstest.models;

/**
 * Created by farheen on 5/10/17
 */

public class StopsModel {
    private String name;
    private double lat;
    private double lng;

    public StopsModel() {

    }

    public StopsModel(String name, double lat, double lng) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
