package com.buggyarts.mapstest;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

//    private static final String MAPS_API_KEY = "AIzaSyByN2xQaioGFbjpd-7h9qJg4iWXbxlQ3Zs";

    private int RC_FINE_LOCATION_PERMISSION = 1;
    private int RC_COARSE_LOCATION_PERMISSION = 2;
    private int RC_CHECK_LOCATION_SETTINGS = 3;
    private int RC_GEOFENCE_PENDING_INTENT = 4;

    private static final String TAG = "MainActivity";
//    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
//    private LocationCallback mLocationCallback;

    private GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent;

//    private LocationModel locationModel;
//    private DatabaseReference mDatabase;

    private Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceIntent = new Intent(getApplicationContext(), TrackerService.class);
//        locationModel = new LocationModel();
//        mDatabase = FirebaseDatabase.getInstance().getReference().child("location");
//        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
//        mGeofencingClient = LocationServices.getGeofencingClient(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    RC_FINE_LOCATION_PERMISSION);
        }
        else {
            checkLocationSettings();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
//        if(mLocationCallback != null){
//            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
//        }

//        mGeofencingClient.removeGeofences(getGeofencePendingIntent())
//                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        Toast.makeText(getApplicationContext(), "geofence removed", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .addOnFailureListener(this, new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Toast.makeText(getApplicationContext(), "geofence remove error", Toast.LENGTH_SHORT).show();
//                    }
//                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == RC_FINE_LOCATION_PERMISSION){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                checkLocationSettings();
            }
            else {
                Toast.makeText(getApplicationContext(), "Grant Location Permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_CHECK_LOCATION_SETTINGS){
            if(resultCode == RESULT_OK){
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
//                        getLocation();
                    }
                }, 1000);
            }
            else if(resultCode == RESULT_CANCELED){
                Toast.makeText(getApplicationContext(), "Location not enabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkLocationSettings(){
        mLocationRequest = Utility.createLocationRequest();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        client.checkLocationSettings(builder.build())
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.e(TAG, "onSuccess: " + locationSettingsResponse.toString());
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
//                                getLocation();
                            }
                        }, 1000);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode){
                            case CommonStatusCodes.RESOLUTION_REQUIRED :
                                ResolvableApiException resolvable = (ResolvableApiException) e;
                                try {
                                    resolvable.startResolutionForResult(MainActivity.this, RC_CHECK_LOCATION_SETTINGS);
                                } catch (IntentSender.SendIntentException e1) {
                                    e1.printStackTrace();
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE :
                                Log.e(TAG, "onFailure: can't change settings");
                                break;
                        }
                    }
                });
    }

//    @SuppressWarnings("MissingPermission")
//    private void getLocation(){
//        mFusedLocationClient.getLastLocation()
//                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
//                    @Override
//                    public void onSuccess(Location location) {
//                        if(location != null){
//                            Log.e(TAG, "onSuccess: " + location.toString());
//                            locationModel.updateModel(location);
//                        }
//                        else {
//                            Log.e(TAG, "onSuccess: location is null");
//                        }
//                    }
//                });
//
//        mLocationCallback = new LocationCallback(){
//            @Override
//            public void onLocationResult(LocationResult locationResult) {
////                for(Location location : locationResult.getLocations()){
////                    Log.e(TAG, "onLocationResult: " + location.toString());
////                }
//                if(locationResult.getLastLocation() != null){
//                    Log.e(TAG, "onLocationResult: " + locationResult.getLastLocation().toString());
//                    locationModel.updateModel(locationResult.getLastLocation());
//                    mDatabase.setValue(locationModel);
//                }
//            }
//        };
//
//        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
////        addGeofence();
//    }

//    protected void createLocationRequest() {
//        mLocationRequest = new LocationRequest();
//        mLocationRequest.setInterval(10000);
//        mLocationRequest.setFastestInterval(5000);
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//    }

//    private Geofence createGeofence(){
//        return new Geofence.Builder()
//                .setRequestId("Office")
//                .setCircularRegion(28.5086399, 77.3782921, 1)
//                .setExpirationDuration(Geofence.NEVER_EXPIRE)
//                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
//                .build();
//    }
//
//    private GeofencingRequest getGeofencingRequest(){
//        return new GeofencingRequest.Builder()
//                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
//                .addGeofence(createGeofence())
//                .build();
//    }

//    private PendingIntent getGeofencePendingIntent(){
//        if(mGeofencePendingIntent != null){
//            return mGeofencePendingIntent;
//        }
//        Intent geofenceIntent = new Intent(this, GeofenceTransitionService.class);
//        mGeofencePendingIntent = PendingIntent.getService(this, RC_GEOFENCE_PENDING_INTENT,
//                geofenceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//        return mGeofencePendingIntent;
//    }

    @SuppressWarnings("MissingPermission")
//    private void addGeofence(){
//        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
//                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        Log.e(TAG, "onSuccess: geofence added");
//                    }
//                })
//                .addOnFailureListener(this, new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.e(TAG, "onFailure: geofence add failed");
//                    }
//                });
//    }

    public void btnStartTracking(View view){
//        mDatabase.setValue(locationModel, new DatabaseReference.CompletionListener() {
//            @Override
//            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
////                Toast.makeText(getApplicationContext(), "Data written", Toast.LENGTH_SHORT).show();
//                Log.e(TAG, "onComplete: ");
//            }
//        });
//        Log.e(TAG, "btnSendClick: " + locationModel.toString());
        startService(serviceIntent);
        Toast.makeText(getApplicationContext(), "Tracking Started", Toast.LENGTH_SHORT).show();
    }

    public void btnStopTracking(View view){
        stopService(serviceIntent);
        Toast.makeText(getApplicationContext(), "Tracking stopped", Toast.LENGTH_SHORT).show();
    }
}
