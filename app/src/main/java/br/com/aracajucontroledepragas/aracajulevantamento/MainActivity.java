package br.com.aracajucontroledepragas.aracajulevantamento;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static com.google.firebase.Timestamp.now;
import static java.lang.Double.parseDouble;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener  {
    private static final String TAG = "MainActivity";
    private FirebaseFirestore mDatabase;
    String androidId;
    Spinner spinner;
    String stg_levantamento;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int Permission_All = 1;
        String[] Permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE };
        if(!hasPermissions(this, Permissions)){
            ActivityCompat.requestPermissions(this, Permissions, Permission_All);
        }
        spinner = (Spinner) findViewById(R.id.spinner);
        androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        mDatabase = FirebaseFirestore.getInstance();

        Query query = mDatabase.collection("levantamento");
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    QuerySnapshot queryDocumentSnapshots = task.getResult();
                    final List<String> titleList = new ArrayList<String>();
                    titleList.add("");
                    for (DocumentSnapshot readData : queryDocumentSnapshots.getDocuments()) {
                        String titlename = readData.getId();
                        titleList.add(titlename);
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, titleList);
                    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(arrayAdapter);
                }
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.d("Androidview", e.getMessage());
                    }
                });
    }

    public void chamaMapsAtivity(View view) {

        if (String.valueOf(spinner.getSelectedItem()) != "")  {
            Log.d(TAG, "Valor da Variavel tipo_ponto " + String.valueOf(spinner.getSelectedItem()));
            stg_levantamento = String.valueOf(spinner.getSelectedItem());
            Intent intent = new Intent(this, MapsActivity.class);
            Bundle params = new Bundle();
            params.putString("levantamento", stg_levantamento);
            intent.putExtras(params);
            startActivity(intent);
        }else {
            Toast.makeText(MainActivity.this, " Selecione a cidade do levantamento ", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
    public static boolean hasPermissions(Context context, String... permissions){
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M && context!=null && permissions!=null){
            for(String permission: permissions){
                if(ActivityCompat.checkSelfPermission(context, permission)!=PackageManager.PERMISSION_GRANTED){
                    return  false;
                }
            }
        }
        return true;
    }
/*    public void importa_csv(View view) {
        String acuracia,altitude,androidid,data_criacao,data_gravacao,Latitude,Longitude,observacao,regiao,tipodoponto,volumeBTI;
        String line = "";
        Scanner data_in = new Scanner(getResources().openRawResource(R.raw.jaragua));
        while (data_in.hasNext()) {
            line = data_in.nextLine();
            Log.v(TAG, "Line: " + line);

            String[] tokens = line.split(";");
            acuracia = tokens[0]; altitude = tokens[1]; androidid = tokens[2]; data_criacao = tokens[3]; data_gravacao = tokens[4];
            Latitude = tokens[5]; Longitude = tokens[6]; observacao = tokens[7]; regiao = tokens[8]; tipodoponto = tokens[9]; volumeBTI = tokens[10];

            Log.v(TAG, "Strings s:" + acuracia + altitude + androidid + data_criacao + data_gravacao + Latitude + Longitude + observacao + regiao + tipodoponto + volumeBTI);


            int int_volumeBTI = Integer.parseInt(volumeBTI);
            double dou_acuracia = Double.valueOf(acuracia);
            double dou_altitude = Double.valueOf(altitude);

            // cria objeto
            Map<String, Object> objeto = new HashMap<>();
            objeto.put("acuracia", dou_acuracia );
            objeto.put("altitude",dou_altitude );
            objeto.put("androidid",androidid );
            objeto.put("data_criacao", now());
            objeto.put("data_gravacao", now());
            objeto.put("latlon", new GeoPoint( parseDouble(Latitude), parseDouble(Longitude) ));
            objeto.put("observacao",observacao );
            objeto.put("regiao",regiao );
            objeto.put("tipodoponto",tipodoponto);
            objeto.put("volumeBTI", int_volumeBTI );

            //grava objeto no banco
            mDatabase.collection("/levantamento/teste2/pontos" ).document()
                    .set(objeto)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully written!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error writing document", e);
                        }
                    });
        }
    } */
}
