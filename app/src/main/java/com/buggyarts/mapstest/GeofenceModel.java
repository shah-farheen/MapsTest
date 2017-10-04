package com.buggyarts.mapstest;

/**
 * Created by farheen on 4/10/17
 */

public class GeofenceModel {
    private String requestId;
    private int transition;

    public GeofenceModel() {

    }

    public GeofenceModel(String requestId, int transition) {
        this.requestId = requestId;
        this.transition = transition;
    }

    public void updateModel(GeofenceModel model){
        this.requestId = model.getRequestId();
        this.transition = model.getTransition();
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int getTransition() {
        return transition;
    }

    public void setTransition(int transition) {
        this.transition = transition;
    }

    @Override
    public String toString() {
        return "GeofenceModel{" +
                "requestId='" + requestId + '\'' +
                ", transition=" + transition +
                '}';
    }
}
