package com.buggyarts.mapstest;

import com.google.android.gms.location.LocationRequest;

/**
 * Created by farheen on 3/10/17
 */

public class Utility {

    public static LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(3000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

}
