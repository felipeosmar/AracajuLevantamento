package br.com.aracajucontroledepragas.aracajulevantamento;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE;
import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED;
import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_YELLOW;

public class MapsActivity extends FragmentActivity implements GoogleMap.OnInfoWindowClickListener, OnMapReadyCallback, LocationListener {
    private static final String TAG = "MainActivity";

    private FirebaseFirestore mDatabase;
    public int pontos_levantados = 0, zoom_reg;
    private GoogleMap mMap;
    String locationText = "";
    public LocationManager locationManager;
    private int mInterval = 3000; // 3 seconds by default, can be changed later
    private long UPDATE_INTERVAL = 5 * 1000;  /* 30 secs */
    private Handler mHandler;
    public String latitude_string = "", longitude_string = "", acuracia_string = "", stg_levantamento;
    double Lat, Lng, Acc = 100, alt;
    TextView tv_npontos;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mDatabase = FirebaseFirestore.getInstance();

        Intent i = getIntent();
        if (i != null) {
            Bundle params = i.getExtras();
            if (params != null) {
                stg_levantamento = params.getString("levantamento");
                TextView regialView = (TextView) findViewById(R.id.tv_regiao);
                regialView.setText(stg_levantamento);

            }
        }

        tv_npontos = (TextView) findViewById(R.id.tv_npontos);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Handler handler2 = new Handler();
        handler2.postDelayed(new Runnable() {
            public void run() {
                mHandler = new Handler();
                startRepeatingTask();
            }
        }, UPDATE_INTERVAL);   //5 seconds


        carregapontosnomapa();

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
        mMap.setMyLocationEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(-26.3594415, -49.2029469)));

        mMap.setOnInfoWindowClickListener(this);

        DocumentReference documentReference = mDatabase.collection("levantamento").document(stg_levantamento);
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Lat = document.getGeoPoint("centro").getLatitude();
                        Lng = document.getGeoPoint("centro").getLongitude();
                        Log.d(TAG, "DocumentSnapshot data: " + Lat + Lng);
                        CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(Lat, Lng));
                        CameraUpdate zoom = CameraUpdateFactory.zoomTo(11);
                        mMap.moveCamera(center);
                        mMap.animateCamera(zoom);
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });


    }

    @Override
    public void onInfoWindowClick(Marker marker) {

        Intent intent = new Intent(this, EditaPontoActivity.class);

        Bundle params = new Bundle();
        params.putString("pontoID", marker.getTag().toString());
        params.putString("levantamento", stg_levantamento);
        intent.putExtras(params);
        startActivity(intent);

        Toast.makeText(this, marker.getTag().toString(),
                Toast.LENGTH_SHORT).show();
    }

    public void chamaGravaPonto(View view) {
        if (Acc < 21) {
            Intent intent = new Intent(this, GravaPontoActivity.class);

            Bundle params = new Bundle();
            params.putString("levantamento", stg_levantamento);
            params.putDouble("latitude", Lat);
            params.putDouble("longitude", Lng);
            params.putDouble("altitude", alt);
            params.putDouble("acuracia", Acc);
            intent.putExtras(params);

            startActivity(intent);
        } else {
            Toast.makeText(MapsActivity.this, " Precisão de GPS muito baixa, aguarde... ", Toast.LENGTH_SHORT).show();
        }
    }


    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            final TextView txtLocation = findViewById(R.id.txtLocation);
            try {
                getLocation(); //this function can change value of mInterval.
                if (locationText == "") {
                    //Toast.makeText(getApplicationContext(), "Trying to retrieve coordinates.", Toast.LENGTH_LONG).show();
                } else {
                    txtLocation.setText(locationText);
                }
            } finally {
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void getLocation() {
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            assert locationManager != null;
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 5, this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        Lat = location.getLatitude();
        Lng = location.getLongitude();
        alt = location.getAltitude();
        Acc = location.getAccuracy();

        latitude_string = String.valueOf(location.getLatitude());
        longitude_string = String.valueOf(location.getLongitude());
        acuracia_string = String.valueOf(location.getAccuracy());
        locationText = "Lat:" + location.getLatitude() + " Lng:" + location.getLongitude() + " Acc:" + location.getAccuracy();


        if (((Switch) findViewById(R.id.sw_camera)).isChecked()) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 18));
        }


    }




    private void carregapontosnomapa() {



        mDatabase.collection("/levantamento/" + stg_levantamento + "/pontos")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Double lat = document.getGeoPoint("latlon").getLatitude();
                                Double lng = document.getGeoPoint("latlon").getLongitude();
                                String tipoponto = String.valueOf(document.get("tipoponto"));
                                String titulo = document.get("volumeBTI").toString() + " ml";
                                Float color = HUE_RED;

                                if (tipoponto.trim().equals("Coleta")) {
                                    color = HUE_AZURE;
                                    titulo = document.get("observacao").toString();
                                }
                                if (tipoponto.trim().equals("Referência")) {
                                    color = HUE_YELLOW;
                                    titulo = document.get("observacao").toString();
                                }


                                mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(lat, lng))
                                        .title(titulo)
                                        .icon(BitmapDescriptorFactory.defaultMarker(color)))
                                        .setTag(document.getId());


                                pontos_levantados++;
                            }
                            pontos_levantados = pontos_levantados / 2;
                            tv_npontos.setText(String.valueOf(pontos_levantados));
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });

    }

    @Override
    protected void onResume() {
        super.onResume();

        carregapontosnomapa();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

}
