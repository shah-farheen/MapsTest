package com.buggyarts.mapstest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.buggyarts.mapstest.models.StopsModel;
import com.buggyarts.mapstest.models.eta.ETAModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.w3c.dom.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker locationMarker;
    private LatLng locationLatLng;
    private LocationModel locationModel;
    private GeofenceModel geofenceModel;

    private DatabaseReference locationDatabase;
    private DatabaseReference geofenceDatabase;
    private ChildEventListener childEventListener;
    private ValueEventListener geofenceEventListener;
    private ValueEventListener locationEventListener;
    int initial = 0;

    private ArrayList<StopsModel> stopsList;
    private Gson gson;
    private Handler uiHandler;
    private OkHttpClient okHttpClient;
    private static final String TAG = "MapsActivity";
    private PolylineOptions polylineOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
//        polylineOptions = new PolylineOptions().geodesic(true);
//        locationDatabase = FirebaseDatabase.getInstance().getReference().child("raw-locations/up32ht5317");
//        childEventListener = getChildEventListener();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        gson = new Gson();
        uiHandler = new Handler();
        okHttpClient = new OkHttpClient();
        locationDatabase = FirebaseDatabase.getInstance().getReference().child("location");
        geofenceDatabase = FirebaseDatabase.getInstance().getReference().child("geofence");
        locationEventListener = getLocationEventListener();
        geofenceEventListener = getGeofenceEventListener();
//        locationModel = new LocationModel();
        geofenceModel = new GeofenceModel();

        LocalBroadcastManager.getInstance(this).registerReceiver(trackingReceiver, new IntentFilter(Utility.ACTION_START_TRACKING));

//        locationDatabase.addChildEventListener(getChildEventListener());
    }

    private BroadcastReceiver trackingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Utility.ACTION_START_TRACKING)){
                Log.e(TAG, "onReceive: ");
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
//        Log.e(TAG, "onPause: Listener removed");
//        locationDatabase.removeEventListener(childEventListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(locationEventListener != null){
            Log.e(TAG, "onStop: location Listener removed");
            locationDatabase.removeEventListener(locationEventListener);
        }
        if(geofenceEventListener != null){
            Log.e(TAG, "onStop: geofence Listener removed");
            geofenceDatabase.removeEventListener(geofenceEventListener);
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(trackingReceiver);
    }

    private ValueEventListener getLocationEventListener(){
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e(TAG, "onDataChange: " + dataSnapshot.toString());
                if(locationModel == null){
                    locationModel = new LocationModel();
                    locationModel.updateModel(dataSnapshot.getValue(LocationModel.class));
                    makeETARequest(new LatLng(locationModel.getLat(), locationModel.getLng()));
                }
                locationModel.updateModel(dataSnapshot.getValue(LocationModel.class));
                locationMarker.setPosition(new LatLng(locationModel.getLat(), locationModel.getLng()));
                locationMarker.setRotation(locationModel.getBearing());
                locationMarker.setSnippet("Speed: " + locationModel.getSpeed());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private ValueEventListener getGeofenceEventListener(){
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e(TAG, "onDataChange: " + dataSnapshot.toString());
                if(dataSnapshot.getValue() != null){
                    geofenceModel.updateModel(dataSnapshot.getValue(GeofenceModel.class));
                    Toast.makeText(getApplication(), geofenceModel.toString(), Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(getApplicationContext(), "null object", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private ChildEventListener getChildEventListener(){
        return new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
//                Toast.makeText(getApplicationContext(), dataSnapshot.getValue(LatLngModel.class).toString(), Toast.LENGTH_LONG).show();
                addLatLng(dataSnapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                addLatLng(dataSnapshot);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private void addLatLng(DataSnapshot dataSnapshot){
        LatLngModel latLngModel = dataSnapshot.getValue(LatLngModel.class);
        assert latLngModel != null;
        LatLng latLng = new LatLng(latLngModel.getLat(), latLngModel.getLng());
        if(checkDuplicacy(latLng)) return;
        polylineOptions.add(latLng);
//        Log.e(TAG, "addLatLng added: " + latLng.toString());
//        Log.e(TAG, "onChildAdded: " + latLngModel.toString());
        mMap.clear();
        mMap.addPolyline(polylineOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
    }

    private boolean checkDuplicacy(LatLng latLng){
        List<LatLng> latLngList = polylineOptions.getPoints();
        for(int i=0; i < latLngList.size(); i++){
            if(latLngList.get(i).equals(latLng)){
                return true;
            }
        }
        return false;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.e(TAG, "onMapReady: ");

        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(28.5642101, 77.3320011);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
//        mMap.addMarker(new MarkerOptions()
//        .position(new LatLng(28.5642101, 77.3320011)).title("Botanical Metro"));
//        mMap.addPolyline(polylineOptions);
//        locationDatabase.addChildEventListener(childEventListener);

        findDirections(28.5090167, 77.372687, 28.5642101, 77.3320011, GMapV2Direction.MODE_DRIVING);
//        GoogleDirection.withServerKey(getString(R.string.google_maps_key))
//                .from(new LatLng(28.6229978, 77.0743968))
//                .to(new LatLng(28.5086399, 77.3782921))
//                .execute(new DirectionCallback() {
//                    @Override
//                    public void onDirectionSuccess(Direction direction, String rawBody) {
//                        if(direction.isOK()){
//                            // do something
//                        }
//                    }
//
//                    @Override
//                    public void onDirectionFailure(Throwable t) {
//
//                    }
//                });
    }

    public void handleGetDirectionsResult(ArrayList<LatLng> directionPoints)
    {
        Polyline newPolyline;
//        GoogleMap mMap = ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        PolylineOptions rectLine = new PolylineOptions().width(15).color(Color.BLUE);

        for(int i = 0 ; i < directionPoints.size() ; i++)
        {
            rectLine.add(directionPoints.get(i));
        }
        mMap.clear();
        newPolyline = mMap.addPolyline(rectLine);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(directionPoints.get(0), 10));
//        mMap.addMarker(new MarkerOptions()
//                .icon(BitmapDescriptorFactory.fromResource(R.drawable.direction))
//                .position(directionPoints.get(0)).title("Somerville School")
//                .snippet("Source").flat(true).rotation(300)).showInfoWindow();
//        mMap.addMarker(new MarkerOptions()
//                .position(directionPoints.get(directionPoints.size()-1))
//                .title("Botanical Metro")
//        .snippet("Destination")).showInfoWindow();
        locationLatLng = new LatLng(directionPoints.get(0).latitude, directionPoints.get(0).longitude);
        locationMarker = mMap.addMarker(new MarkerOptions()
        .icon(BitmapDescriptorFactory.fromResource(R.drawable.direction))
        .title("Bus UP12")
        .flat(true).position(locationLatLng));
        locationDatabase.addValueEventListener(locationEventListener);
        geofenceDatabase.addValueEventListener(geofenceEventListener);
    }


    public void findDirections(double fromPositionDoubleLat, double fromPositionDoubleLong, double toPositionDoubleLat, double toPositionDoubleLong, String mode) {
        Map<String, String> map = new HashMap<>();
        map.put(GetDirectionsAsyncTask.USER_CURRENT_LAT, String.valueOf(fromPositionDoubleLat));
        map.put(GetDirectionsAsyncTask.USER_CURRENT_LONG, String.valueOf(fromPositionDoubleLong));
        map.put(GetDirectionsAsyncTask.DESTINATION_LAT, String.valueOf(toPositionDoubleLat));
        map.put(GetDirectionsAsyncTask.DESTINATION_LONG, String.valueOf(toPositionDoubleLong));
        map.put(GetDirectionsAsyncTask.DIRECTIONS_MODE, mode);

        GetDirectionsAsyncTask asyncTask = new GetDirectionsAsyncTask(this);
        asyncTask.execute(map);
    }

    public class GetDirectionsAsyncTask extends AsyncTask<Map<String, String>, Integer, ArrayList<LatLng>> {

        public static final String USER_CURRENT_LAT = "user_current_lat";
        public static final String USER_CURRENT_LONG = "user_current_long";
        public static final String DESTINATION_LAT = "destination_lat";
        public static final String DESTINATION_LONG = "destination_long";
        public static final String DIRECTIONS_MODE = "directions_mode";
        private MapsActivity activity;
        private String url;

        private Exception exception;

//        private Dialog progressDialog;

        public GetDirectionsAsyncTask(MapsActivity activity /*String url*/)
        {
            super();
            this.activity = activity;

            //  this.url = url;
        }

        public void onPreExecute() {
//            progressDialog = DialogUtils.createProgressDialog(activity, activity.getString(R.string.get_data_dialog_message));
//            progressDialog.show();
            Log.e(TAG, "onPreExecute: ");
        }

        @Override
        public void onPostExecute(ArrayList<LatLng> result) {
//            progressDialog.dismiss();
            Log.e(TAG, "onPostExecute: ");
            if (exception == null) {
                activity.handleGetDirectionsResult(result);
            } else {
                processException();
            }
        }

        @Override
        protected ArrayList<LatLng> doInBackground(Map<String, String>... params) {

            Map<String, String> paramMap = params[0];
            try{
                LatLng fromPosition = new LatLng(Double.valueOf(paramMap.get(USER_CURRENT_LAT)) , Double.valueOf(paramMap.get(USER_CURRENT_LONG)));
                LatLng toPosition = new LatLng(Double.valueOf(paramMap.get(DESTINATION_LAT)) , Double.valueOf(paramMap.get(DESTINATION_LONG)));
                GMapV2Direction md = new GMapV2Direction();
                Document doc = md.getDocument(fromPosition, toPosition, paramMap.get(DIRECTIONS_MODE));
                ArrayList<LatLng> directionPoints = md.getDirection(doc);
                for(LatLng latLng : directionPoints){
                    Log.e(TAG, "doInBackground: " + latLng.toString());
                }
                Log.e(TAG, "doInBackground: ");
                return directionPoints;
            }
            catch (Exception e) {
                exception = e;
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        private void processException() {
            Log.e(TAG, "processException: exception in getting data");
//            Toast.makeText(activity, activity.getString(R.string.error_when_retrieving_data), 3000).show();
        }

    }

    private void getETA(LatLng origin, LatLng dest, final String destName){
        Request getETARequest = new Request.Builder()
                .url(getETAUrl(origin, dest))
                .build();
        okHttpClient.newCall(getETARequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        makeETARequest(new LatLng(locationModel.getLat(), locationModel.getLng()));
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
//                Log.e(TAG, "onResponse: " + response.body().string());
                final ETAModel etaModel = gson.fromJson(response.body().string(), ETAModel.class);
                if(etaModel.getStatus().equals(Utility.STATUS_OK)){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), destName + ": " +
                                    etaModel.getRows().get(0).getElements()
                                            .get(0).getDuration().getText(), Toast.LENGTH_SHORT).show();
                            makeETARequest(new LatLng(locationModel.getLat(), locationModel.getLng()));
                        }
                    });
                }
                else {
                    Log.e(TAG, "onResponse: " + etaModel.getStatus());
                    Log.e(TAG, "onResponse: " + call.request().url().toString());
                }
            }
        });
    }

    private String getETAUrl(LatLng origin, LatLng dest){
        String BASE_URL = "https://maps.googleapis.com/maps/api/distancematrix/json?" +
                "traffic_model=best_guess";
        StringBuilder stringBuilder = new StringBuilder()
                .append(BASE_URL)
                .append("&origins=").append(origin.latitude).append(",").append(origin.longitude)
                .append("&destinations=").append(dest.latitude).append(",").append(dest.longitude)
                .append("&departure_time=").append(System.currentTimeMillis())
                .append("&key=AIzaSyByN2xQaioGFbjpd-7h9qJg4iWXbxlQ3Zs");
        return stringBuilder.toString();
    }

    private ArrayList<StopsModel> getStopsList(){
        if(stopsList == null) {
            stopsList = new ArrayList<>();
            stopsList.add(new StopsModel("Logix Technova", 28.509452, 77.3721957));
            stopsList.add(new StopsModel("Adobe 132", 28.5073325, 77.3770577));
            stopsList.add(new StopsModel("Between adobe/somerville", 28.507811, 77.376241));
            stopsList.add(new StopsModel("Somerville school", 28.5090114, 77.3726282));
            stopsList.add(new StopsModel("Sunsource Energy", 28.5097953, 77.3713751));
            stopsList.add(new StopsModel("Paras one33", 28.5104963, 77.3701158));
            stopsList.add(new StopsModel("InfoEdge India", 28.5131503, 77.3708086));
            stopsList.add(new StopsModel("Jaypee Hospital", 28.5142023, 77.3707435));
            stopsList.add(new StopsModel("DPS Noida", 28.5166238, 77.3731782));
            stopsList.add(new StopsModel("HDFC bank", 28.5164083, 77.3768369));
            stopsList.add(new StopsModel("Genesis Global School", 28.513897, 77.3792909));
            stopsList.add(new StopsModel("Residential apartment", 28.5135714, 77.3814703));
            stopsList.add(new StopsModel("Turn for questin.co", 28.512234, 77.383984));
            stopsList.add(new StopsModel("ATS bouquet", 28.5102488, 77.3800319));
        }
        return stopsList;
    }

    private void makeETARequest(final LatLng currentLocation){
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for(StopsModel model : getStopsList()){
                    getETA(currentLocation, new LatLng(model.getLat(), model.getLng()), model.getName());
                }
            }
        }, 10000);
    }
}
