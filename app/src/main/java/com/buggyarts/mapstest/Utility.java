package com.buggyarts.mapstest;

import com.google.android.gms.location.LocationRequest;

/**
 * Created by farheen on 3/10/17
 */

public class Utility {

    public static final String ACTION_START_TRACKING = "ACTION_START_TRACKING";
    public static final String STATUS_DENIED = "REQUEST_DENIED";
    public static final String STATUS_OK = "OK";

    public static LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(3000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

}
