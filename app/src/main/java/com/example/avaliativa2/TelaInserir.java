package com.example.avaliativa2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.Spinner;
import android.widget.Toast;
import android.provider.Settings.Secure;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TelaInserir extends AppCompatActivity implements LocationListener, AdapterView.OnItemSelectedListener {


    private LocationManager mlocManager = null;

    private Button btInserir2 = null;
    private Button btVoltar = null;

    private EditText etValue = null;

    private Spinner sProductId = null;
    private List<String> productsList = new ArrayList<>();

    private SQLiteDatabase db = null;

    private Cursor cur = null;

    private double lon, lat;

    private String vendorid = null;
    private String value = null;
    private String numProd = null;

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inserir);

        db = new DatabaseManager(this, "BancoDadosW", null, 1).getWritableDatabase();

        atualizaDados();

        vendorid = Secure.getString(getContentResolver(), Secure.ANDROID_ID);

        etValue = (EditText) (findViewById(R.id.et_value));

        mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        verificarGPS();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mlocManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 10, 0, this);

        btInserir2 = (Button) findViewById(R.id.bt_inserir2);
        btInserir2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (lat == 0.0 && lon == 0.0) {
                    verificarGPS();
                    return;
                }
                
                value = etValue.getText().toString();
                boolean ok = true;

                if (numProd != null) {
                    if (numProd.equals("1") || numProd.equals("3") || numProd.equals("4")) {
                        if (!value.equals("0") && !value.equals("1")) {
                            Toast.makeText(TelaInserir.this, "Valor inválido! Deve ser 0 ou 1 para o produto " + numProd, Toast.LENGTH_SHORT).show();
                            ok = false;
                        }
                    } else if (numProd.equals("6")) {
                        if (!value.matches("[0-9]+")) {
                            Toast.makeText(TelaInserir.this, "Valor inválido! Deve ser um número inteiro para o produto " + numProd, Toast.LENGTH_SHORT).show();
                            ok = false;
                        }
                    } else if (numProd.equals("5")) {
                        if (value.trim().isEmpty()) {
                            Toast.makeText(TelaInserir.this, "Valor inválido! O campo de texto não pode estar vazio para o produto " + numProd, Toast.LENGTH_SHORT).show();
                            ok = false;
                        }
                    } else if (numProd.equals("2")) {
                        if (!value.matches("^[-+]?\\d*.?\\d*$")) {
                            Toast.makeText(TelaInserir.this, "Valor inválido! Deve ser um número decimal (ex: 10.5) para o produto " + numProd, Toast.LENGTH_SHORT).show();
                            ok = false;
                        }
                    }
                    if(ok){
                        executePost();
                    }

                }
            }
        });


        btVoltar = (Button) findViewById(R.id.bt_voltar);
        btVoltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


        sProductId = (Spinner) findViewById(R.id.s_product_id);

        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, productsList);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sProductId.setAdapter(ad);

        sProductId.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = sProductId.getSelectedItem().toString();
                String[] parts = selectedItem.split("- ");
                numProd = parts[0];
                buscarDados();
                Toast.makeText(getApplicationContext(), "Selecionado: " + selectedItem, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // O que fazer quando nada é selecionado (opcional)
            }
        });

    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        lon = (location.getLongitude());
        lat = (location.getLatitude());
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private void atualizaDados() {
        cur = db.query("product", new String[]{"productid", "description"}, null, null, null, null, null);

        productsList.clear();

        if (cur.moveToFirst()) {
            do {
                String productid = cur.getString(0);
                String description = cur.getString(1);

                productsList.add(productid + "- " + description);
            } while (cur.moveToNext());
        }
        cur.close();
    }

    public void executePost() {
        RequestQueue queue = Volley.newRequestQueue(this);


        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                "http://177.44.248.13:8080/WaterManager/?op=INSERT",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("API Response", response);  // Logar a resposta da API
                        Toast.makeText(getApplicationContext(), "Dados inseridos com sucesso", Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Erro", Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("VENDORID", vendorid);
                params.put("PRODUCTID", numProd);
                params.put("LATITUDE", String.valueOf(lat));
                params.put("LONGITUDE", String.valueOf(lon));
                params.put("VALUE", value);
                return params;
            }

        };

        queue.add(stringRequest);
    }

    private void buscarDados() {
        Cursor cur = db.query("product", new String[]{"example"}, "productid = ?", new String[]{numProd}, null, null, null);

        if (cur != null && cur.moveToFirst()) {
            String exemplo = cur.getString(0);
            etValue.setText(exemplo);
        } else {
            etValue.setText("Valor");
        }
        cur.close();  // Fechar o cursor após o uso
    }

    private void verificarGPS() {
        if (!mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "O GPS não está ativado. Por favor, ative-o para continuar.", Toast.LENGTH_SHORT).show();

            // Abre as configurações de localização
            Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }
}


