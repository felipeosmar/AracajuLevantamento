package br.com.aracajucontroledepragas.aracajulevantamento;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EditaPontoActivity extends AppCompatActivity {
    private static final String TAG = "EditaPontoActivity";
    private FirebaseFirestore mDatabase;
    Spinner spinner_tipoponto, spinner_volume;
    List<String> spinnerList_tipoponto = new ArrayList<String>();
    List<String> spinnerList_volume = new ArrayList<String>();
    EditText edt_obs;
    String pontoID, stg_levantamento, tipoponto, volume;
    TextView tv_pontoID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edita_ponto);
        tv_pontoID = (TextView) findViewById(R.id.tv_pontoID);
        spinner_tipoponto = (Spinner) findViewById(R.id.spinner_tipoponto);
        spinner_volume = (Spinner) findViewById(R.id.spinner_volume);
        edt_obs = findViewById(R.id.edt_obs);

        Intent i = getIntent();
        if (i != null) {
            Bundle params = i.getExtras();
            if (params != null) {
                pontoID = params.getString("pontoID");
                stg_levantamento = params.getString("levantamento");
            }
        }
        tv_pontoID.setText("pontoID : " + pontoID );

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
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(EditaPontoActivity.this, android.R.layout.simple_spinner_item, spinnerList_tipoponto);
                    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner_tipoponto.setAdapter(arrayAdapter);

                }
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EditaPontoActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.d("Androidview", e.getMessage());
                    }
                });

        DocumentReference docRef_volume = mDatabase.collection("respostas").document("volume");
        docRef_volume.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    //DocumentSnapshot queryDocumentSnapshots_volume = task.getResult();

                    for (Object item : task.getResult().getData().values()) {
                        spinnerList_volume.add(item.toString());
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(EditaPontoActivity.this, android.R.layout.simple_spinner_dropdown_item, spinnerList_volume);
                    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner_volume.setAdapter(arrayAdapter);
                }
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EditaPontoActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.d("Androidview", e.getMessage());
                    }
                });


        DocumentReference docRef = mDatabase.collection("/levantamento/" + stg_levantamento + "/pontos").document(pontoID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {

                        tipoponto = document.getString("tipoponto");
                        volume = document.getString("volumeBTI");
                        edt_obs.setText(document.getString("observacao"));

                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

        //TODO fazer carregar valores do banco nos campos de tela.

    }



    public void excluirponto(View view) {
        AlertDialog alertDialog = new AlertDialog.Builder(EditaPontoActivity.this).create();
        alertDialog.setTitle("Excluindo ponto");
        alertDialog.setMessage("Tem certeza que deseja excluir este ponto?");
        // Alert dialog button
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "EXCLUIR",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Alert dialog action goes here
                        // onClick button code here

                        mDatabase.collection("/levantamento/" + stg_levantamento + "/pontos").document(pontoID)
                                .delete()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(EditaPontoActivity.this, " Ponto EXCLUIDO com sucesso", Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "DocumentSnapshot successfully deleted!");
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Error deleting document", e);
                                    }
                                });



                        finish();
                        dialog.dismiss();// use dismiss to cancel alert dialog

                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCELAR",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Alert dialog action goes here
                        // onClick button code here

                        dialog.dismiss();// use dismiss to cancel alert dialog

                    }
                });

        alertDialog.show();


    }



}
