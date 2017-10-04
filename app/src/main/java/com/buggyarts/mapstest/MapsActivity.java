package com.buggyarts.mapstest;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

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

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        locationDatabase = FirebaseDatabase.getInstance().getReference().child("location");
        geofenceDatabase = FirebaseDatabase.getInstance().getReference().child("geofence");
        locationEventListener = getLocationEventListener();
        geofenceEventListener = getGeofenceEventListener();
        locationModel = new LocationModel();
        geofenceModel = new GeofenceModel();

//        locationDatabase.addChildEventListener(getChildEventListener());
    }

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
    }

    private ValueEventListener getLocationEventListener(){
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e(TAG, "onDataChange: " + dataSnapshot.toString());
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
}
