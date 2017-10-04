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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by farheen on 3/10/17
 */

public class TrackerService extends Service{

    private ServiceHandler mServiceHandler;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private LocationModel locationModel;

    private GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent;
    private DatabaseReference locationDatabase;
    private List<Geofence> geofenceList;

    private int RC_TRACKER_INTENT = 0;
    private int RC_GEOFENCE_PENDING_INTENT = 1;
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

        geofenceList = new ArrayList<>();
        mGeofencingClient = LocationServices.getGeofencingClient(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationModel = new LocationModel();
        locationDatabase = FirebaseDatabase.getInstance().getReference().child("location");
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

        mGeofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getApplicationContext(), "geofence removed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "geofence remove error", Toast.LENGTH_SHORT).show();
                    }
                });
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
                    locationDatabase.setValue(model);
                }
            }
        };

        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
                addGeofences();
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

    private Geofence createGeofence(String requestId, double lat, double lng){
        return new Geofence.Builder()
                .setRequestId(requestId)
                .setCircularRegion(lat, lng, 50)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    private GeofencingRequest getGeofencingRequest(){
        addGeofencesToList();
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofenceList)
                .build();
    }

    private PendingIntent getGeofencePendingIntent(){
        if(mGeofencePendingIntent != null){
            return mGeofencePendingIntent;
        }
        Intent geofenceIntent = new Intent(this, GeofenceTransitionService.class);
        mGeofencePendingIntent = PendingIntent.getService(this, RC_GEOFENCE_PENDING_INTENT,
                geofenceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    @SuppressWarnings("MissingPermission")
    private void addGeofences(){
        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.e(TAG, "onSuccess: geofence added");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: geofence add failed");
                    }
                });
    }

    private void addGeofencesToList(){
        geofenceList.add(createGeofence("Questin", 28.5077602, 77.3788803));
        geofenceList.add(createGeofence("Logix Technova", 28.509452, 77.3721957));
        geofenceList.add(createGeofence("Adobe 132", 28.5073325, 77.3770577));
        geofenceList.add(createGeofence("Between adobe/somerville", 28.507811, 77.376241));
        geofenceList.add(createGeofence("Somerville school", 28.5090114, 77.3726282));
        geofenceList.add(createGeofence("Sunsource Energy", 28.5097953, 77.3713751));
        geofenceList.add(createGeofence("Paras one33", 28.5104963, 77.3701158));
        geofenceList.add(createGeofence("InfoEdge India", 28.5131503, 77.3708086));
        geofenceList.add(createGeofence("Jaypee Hospital", 28.5142023, 77.3707435));
        geofenceList.add(createGeofence("DPS Noida", 28.5166238, 77.3731782));
        geofenceList.add(createGeofence("HDFC bank", 28.5164083, 77.3768369));
        geofenceList.add(createGeofence("Genesis Global School", 28.513897, 77.3792909));
        geofenceList.add(createGeofence("Residential apartment", 28.5135714, 77.3814703));
        geofenceList.add(createGeofence("Turn for questin.co", 28.512234, 77.383984));
        geofenceList.add(createGeofence("ATS bouquet", 28.5102488, 77.3800319));
    }

}
