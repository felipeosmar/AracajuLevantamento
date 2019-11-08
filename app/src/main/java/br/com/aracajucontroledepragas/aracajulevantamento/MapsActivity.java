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
import androidx.annotation.Nullable;
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
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Objects;

import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE;
import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN;
import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED;
import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_YELLOW;
import static java.lang.Math.PI;
import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class MapsActivity extends FragmentActivity implements GoogleMap.OnInfoWindowClickListener, OnMapReadyCallback, LocationListener {
    private static final String TAG = "MainActivity";

    private FirebaseFirestore mDatabase;
    public int num_pontos = 0, zoom_reg, pontos_levantados, num_pontos_30;
    private GoogleMap mMap;
    String locationText = "";
    public LocationManager locationManager;
    private int mInterval = 3000; // 3 seconds by default, can be changed later
    private long UPDATE_INTERVAL = 5 * 1000;  /* 30 secs */
    private Handler mHandler;
    public String latitude_string = "", longitude_string = "", acuracia_string = "", stg_regiao;
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
                stg_regiao = params.getString("levantamento");
                TextView regialView = (TextView) findViewById(R.id.tv_regiao);
                regialView.setText(stg_regiao);

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

        DocumentReference documentReference = mDatabase.collection("levantamento").document(stg_regiao);

        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Lat = document.getGeoPoint("centro").getLatitude();
                        Lng = document.getGeoPoint("centro").getLongitude();
                        zoom_reg = document.getDouble("zoom").intValue();
                        Log.d(TAG, "DocumentSnapshot data: " + Lat + Lng);
                        CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(Lat, Lng));
                        CameraUpdate zoom = CameraUpdateFactory.zoomTo(zoom_reg);
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
        mMap.clear();
        carregapontosnomapa();
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

        Intent intent = new Intent(this, EditaPontoActivity.class);
        Bundle params = new Bundle();
        params.putString("pontoID", marker.getTag().toString());
        params.putString("markerID", marker.getId());
        params.putString("levantamento", stg_regiao);
        intent.putExtras(params);
        startActivity(intent);
    }

    public void chamaGravaPonto(View view) {
        if (Acc < 21) {
            num_pontos_30 = 0;
            mDatabase.collection("/producao/" + stg_regiao + "/pontos")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : (task.getResult())) {
                                    double lat = Objects.requireNonNull(document.getGeoPoint("latlon")).getLatitude();
                                    double lng = Objects.requireNonNull(document.getGeoPoint("latlon")).getLongitude();
                                    double distancia = Distance(Lat, Lng, lat, lng, "K");
                                    if (distancia <= 30) { //Se encontrar um ponto na base de dados a menos  de 30 metros
                                        num_pontos_30++;
                                    }
                                }
                                num_pontos = num_pontos_30 / 2; // nao sei porque o numero é somado em duplicidade, para corrigir dividi por 2
                                if (num_pontos_30 == 0) {
                                    chamaGravaPontoActivity();
                                }
                                if (num_pontos_30 == 1) { //se encontrado apenas um ponto em 30 metros
                                    Log.d(TAG, "Encontrado 1: " + num_pontos_30);
                                }
                                // fazer analise se encontrar mais de um ponto proximo.
                                if (num_pontos_30 > 1) {
                                    Log.d(TAG, "Encontrado +1: " + num_pontos_30);
                                }
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        }
                    });
        } else {
            Toast.makeText(MapsActivity.this, " Precisão de GPS muito baixa, aguarde... ", Toast.LENGTH_SHORT).show();
        }
    }

    private void chamaGravaPontoActivity() {

        Intent intent = new Intent(this, GravaPontoActivity.class);
        Bundle params = new Bundle();
        params.putString("levantamento", stg_regiao);
        params.putDouble("latitude", Lat);
        params.putDouble("longitude", Lng);
        params.putDouble("altitude", alt);
        params.putDouble("acuracia", Acc);
        intent.putExtras(params);
        startActivity(intent);
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

        mDatabase.collection("/levantamento/" + stg_regiao + "/pontos")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                    Double lat = dc.getDocument().getGeoPoint("latlon").getLatitude();
                                    Double lng = dc.getDocument().getGeoPoint("latlon").getLongitude();
                                    String tipoponto = String.valueOf(dc.getDocument().get("tipoponto"));
                                    String titulo = dc.getDocument().get("volumeBTI").toString() + " ml";
                                    Float color = HUE_RED;
                                    if (tipoponto.trim().equals("Coleta")) {
                                        color = HUE_AZURE;
                                        titulo = dc.getDocument().get("observacao").toString();
                                    }
                                    if (tipoponto.trim().equals("Referência")) {
                                        color = HUE_YELLOW;
                                        titulo = dc.getDocument().get("observacao").toString();
                                    }
                                    if (tipoponto.trim().equals("Vazão")) {
                                        color = HUE_GREEN;
                                        titulo = dc.getDocument().get("observacao").toString();
                                    }
                                    mMap.addMarker(new MarkerOptions()
                                            .position(new LatLng(lat, lng))
                                            .title(titulo)
                                            .icon(BitmapDescriptorFactory.defaultMarker(color)))
                                            .setTag(dc.getDocument().getId());
                                    pontos_levantados++;
                                    break;
                                case MODIFIED:
                                    Log.d(TAG, "Modificado: " + dc.getDocument().getData());
                                    break;
                                case REMOVED:
                                    Log.d(TAG, "Removido: " + dc.getDocument().getData());
                                    break;
                            }
                            //pontos_levantados = pontos_levantados / 2;
                            tv_npontos.setText(String.valueOf(pontos_levantados));
                            //Log.d(TAG, "Pontos: " + pontos );
                        }
                    }
                });


//        mDatabase.collection("/levantamento/" + stg_levantamento + "/pontos")
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                            for (QueryDocumentSnapshot document : task.getResult()) {
//                                Double lat = document.getGeoPoint("latlon").getLatitude();
//                                Double lng = document.getGeoPoint("latlon").getLongitude();
//                                String tipoponto = String.valueOf(document.get("tipoponto"));
//                                String titulo = document.get("volumeBTI").toString() + " ml";
//                                Float color = HUE_RED;
//
//                                if (tipoponto.trim().equals("Coleta")) {
//                                    color = HUE_AZURE;
//                                    titulo = document.get("observacao").toString();
//                                }
//                                if (tipoponto.trim().equals("Referência")) {
//                                    color = HUE_YELLOW;
//                                    titulo = document.get("observacao").toString();
//                                }
//                                if (tipoponto.trim().equals("Vazão")) {
//                                    color = HUE_GREEN;
//                                    titulo = document.get("observacao").toString();
//                                }
//                                mMap.addMarker(new MarkerOptions()
//                                        .position(new LatLng(lat, lng))
//                                        .title(titulo)
//                                        .icon(BitmapDescriptorFactory.defaultMarker(color)))
//                                        .setTag(document.getId());
//                                pontos_levantados++;
//                            }
//                            pontos_levantados = pontos_levantados / 2;
//                            tv_npontos.setText(String.valueOf(pontos_levantados));
//                        } else {
//                            Log.d(TAG, "Error getting documents: ", task.getException());
//                        }
//                    }
//                });

    }

    @Override
    protected void onResume() {
        super.onResume();
        //carregapontosnomapa();
    }

    public static double deg2rad(double angdeg) {
        return angdeg / 180.0 * PI;
    }

    double Distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double radius = 6378.137; // earth mean radius defined by WGS84
        double dlon = lon1 - lon2;
        double distance = acos(sin(deg2rad(lat1)) * sin(deg2rad(lat2)) + cos(deg2rad(lat1)) * cos(deg2rad(lat2)) * cos(deg2rad(dlon))) * radius;
        if (unit == "K") {
            return (distance) * 1000;
        } else if (unit == "M") {
            return (distance * 0.621371192);
        } else if (unit == "N") {
            return (distance * 0.539956803);
        } else {
            return 0;
        }
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
