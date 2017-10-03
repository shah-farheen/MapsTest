package com.buggyarts.mapstest;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by farheen on 3/10/17
 */

public class TrackerService extends Service{

    private ServiceHandler mServiceHandler;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private LocationModel locationModel;

    private DatabaseReference mDatabase;

    private int RC_TRACKER_INTENT = 0;
    private int NOTIFICATION_ID = 1;
    private String channelId = "tracker";
    private boolean isAlreadyRunning = false;
    private static final String TAG = "TrackerService";

    private class ServiceHandler extends Handler {
        ServiceHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

        }
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate: ");
        HandlerThread thread = new HandlerThread("TrackerThread", Thread.MAX_PRIORITY);
        thread.start();
        mServiceHandler = new ServiceHandler(thread.getLooper());

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationModel = new LocationModel();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("location");
        isAlreadyRunning = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand: ");
        if(!isAlreadyRunning){
            getLocation();
        }
        isAlreadyRunning = true;
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: service destroyed");
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @SuppressWarnings("MissingPermission")
    private void getLocation(){
        mLocationRequest = Utility.createLocationRequest();
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if(location != null){
                            Log.e(TAG, "onSuccess: " + location.toString());
                            locationModel.updateModel(location);
                        }
                        else {
                            Log.e(TAG, "onSuccess: location is null");
                        }
                    }
                });

        mLocationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
//                for(Location location : locationResult.getLocations()){
//                    Log.e(TAG, "onLocationResult: " + location.toString());
//                }
                if(locationResult.getLastLocation() != null){
                    Log.e(TAG, "onLocationResult: " + locationResult.getLastLocation().getTime());
//                    locationModel.updateModel(locationResult.getLastLocation());
                    LocationModel model = new LocationModel(locationResult.getLastLocation());
                    mDatabase.setValue(model);
                }
            }
        };

        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
            }
        });
        startForeground(NOTIFICATION_ID, getNotification());
    }

    private Notification getNotification(){
        Intent trackerIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent trackerPendingIntent = PendingIntent.getActivity(this, RC_TRACKER_INTENT,
                trackerIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setContentIntent(trackerPendingIntent)
                .setContentText("Tracking Started")
                .setSmallIcon(R.mipmap.ic_launcher);
        return builder.build();
    }
}
