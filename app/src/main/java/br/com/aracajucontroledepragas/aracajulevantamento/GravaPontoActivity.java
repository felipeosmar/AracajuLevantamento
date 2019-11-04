package br.com.aracajucontroledepragas.aracajulevantamento;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.firebase.Timestamp.now;
import static com.google.firebase.firestore.FieldValue.serverTimestamp;

public class GravaPontoActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "GravaPontoActivity";
    private FirebaseFirestore mDatabase;
    Spinner spinner_tipoponto, spinner_volume;
    List<String> spinnerList_tipoponto = new ArrayList<String>();
    List<String> spinnerList_volume = new ArrayList<String>();
    String androidId, stg_levantamento;
    EditText edt_obs;
    String volume, tipo_ponto;
    Double dou_lat, dou_lng, dou_alt, dou_acc;
    TextView tv_androidID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grava_ponto);
        spinner_tipoponto = (Spinner) findViewById(R.id.spinner_tipoponto);
        spinner_volume = (Spinner) findViewById(R.id.spinner_volume);
        tv_androidID = findViewById(R.id.tv_androidID);
        edt_obs = findViewById(R.id.edt_obs);

        androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        tv_androidID.setText(androidId);
        TextView regialView = (TextView) findViewById(R.id.tv_levantameno);

        Intent i = getIntent();
        if (i != null) {
            Bundle params = i.getExtras();
            if (params != null) {
                stg_levantamento = params.getString("levantamento");
                dou_lat = params.getDouble("latitude");
                dou_lng = params.getDouble("longitude");
                dou_alt = params.getDouble("altitude");
                dou_acc = params.getDouble("acuracia");
            }
        }
        regialView.setText("Reg: " + stg_levantamento + " Lat " + dou_lat + " Long " + dou_lng + " Alt " + dou_alt + " Acc " + dou_acc);



        mDatabase = FirebaseFirestore.getInstance();

        DocumentReference docRef_tipoponto = mDatabase.collection("respostas").document("tipo_ponto");
        docRef_tipoponto.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    //DocumentSnapshot queryDocumentSnapshots_tipoponto = task.getResult();

                    for (Object item : task.getResult().getData().values()) {
                        spinnerList_tipoponto.add(item.toString());
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(GravaPontoActivity.this, android.R.layout.simple_spinner_item, spinnerList_tipoponto);
                    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner_tipoponto.setAdapter(arrayAdapter);
                }
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(GravaPontoActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.d("Androidview", e.getMessage());
                    }
                });

        DocumentReference docRef_volume = mDatabase.collection("respostas").document("volume");
        docRef_volume.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot queryDocumentSnapshots_volume = task.getResult();

                    for (Object item : task.getResult().getData().values()) {
                        spinnerList_volume.add(item.toString());
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(GravaPontoActivity.this, android.R.layout.simple_spinner_item, spinnerList_volume);
                    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner_volume.setAdapter(arrayAdapter);
                }
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(GravaPontoActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.d("Androidview", e.getMessage());
                    }
                });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void gravapontonabase(View view) {
        volume = spinner_volume.getSelectedItem().toString();
        tipo_ponto = String.valueOf(spinner_tipoponto.getSelectedItem());

        if ((tipo_ponto.trim().equals("Aplicação")) && (volume.trim().equals("" ))) {
            Toast.makeText(GravaPontoActivity.this, " Selecione VOLUME ", Toast.LENGTH_SHORT).show();
        } else {
            // cria objeto
            Map<String, Object> objeto = new HashMap<>();
            objeto.put("latlon", new GeoPoint(dou_lat, dou_lng));
            objeto.put("altitude", dou_alt);
            objeto.put("acuracia", dou_acc);
            objeto.put("data_criacao", now());
            objeto.put("data_gravacao", serverTimestamp());
            objeto.put("regiao", stg_levantamento.toLowerCase().trim());
            objeto.put("tipoponto", spinner_tipoponto.getSelectedItem());
            objeto.put("volumeBTI", volume);
            objeto.put("observacao", edt_obs.getText().toString());
            objeto.put("androidid", androidId);

            //grava objeto no banco
            mDatabase.collection("/levantamento/" + stg_levantamento + "/pontos").document()
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
            finish(); //volta para ativity principal
        }
    }


}
