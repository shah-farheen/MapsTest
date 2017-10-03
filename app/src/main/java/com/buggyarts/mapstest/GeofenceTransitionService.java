package com.buggyarts.mapstest;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/**
 * Created by farheen on 29/9/17
 */

public class GeofenceTransitionService extends IntentService {

    private static final String TAG = "GeofenceTransitionServi";

    public GeofenceTransitionService(){
        super("GeofencingService");
    }

    public GeofenceTransitionService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if(geofencingEvent.hasError()){
            Log.e(TAG, "onHandleIntent: " + geofencingEvent.getErrorCode());
            Toast.makeText(getApplicationContext(), "geofence event error",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
            Log.e(TAG, "onHandleIntent transition: " + geofenceTransition);
            Toast.makeText(getApplicationContext(), "transition: " + geofenceTransition,
                    Toast.LENGTH_SHORT).show();
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            for(Geofence geofence : triggeringGeofences){
                Log.e(TAG, "onHandleIntent geofence: " + geofence.getRequestId());
                Toast.makeText(getApplicationContext(), "geofence: " + geofence.getRequestId(),
                        Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Log.e(TAG, "onHandleIntent: transition not required " + geofenceTransition);
            Toast.makeText(getApplicationContext(), "transition not required: " + geofenceTransition,
                    Toast.LENGTH_SHORT).show();
        }
    }
}
