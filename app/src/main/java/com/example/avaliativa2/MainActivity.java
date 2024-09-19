package com.example.avaliativa2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOCATION_PERMISSIONS = 1;

    private Button btInserir = null;

    private Button btConsultar = null;

    private SQLiteDatabase db = null;
    private Cursor cur = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSIONS);
        }

        db = new DatabaseManager(this, "BancoDados", null, 1).getWritableDatabase();
        executeGetProd();
        executeGetWater();

        btInserir = (Button) findViewById(R.id.bt_inserir_dados);
        btInserir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, TelaInserir.class);
                startActivity(intent);
            }
        });

        btConsultar = (Button) findViewById(R.id.bt_consultar);
        btConsultar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, TelaConsulta.class);
                startActivity(intent);
            }
        });
    }

    public void executeGetProd() {
        int lastSavedId = getLastSavedId("product");

        RequestQueue queue = Volley.newRequestQueue(this);
        String urlConexao = "http://177.44.248.13:8080/WaterManager/productID.jsp?FORMAT=JSON";
        JsonArrayRequest dataRequest = new JsonArrayRequest(
                urlConexao,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jsonObject = response.getJSONObject(i);

                                int id = jsonObject.optInt("id", 0);
                                if (id <= lastSavedId) {
                                    continue; // Pular se o ID já estiver salvo
                                }

                                id = jsonObject.optInt("id");
                                int productid = jsonObject.optInt("productid");
                                String description = jsonObject.optString("description");
                                String type = jsonObject.optString("type");
                                String example = jsonObject.optString("example");
                                String validateexpression = jsonObject.optString("validateexpression");

                                ContentValues data = new ContentValues();
                                data.put("id", id);
                                data.put("productid", productid);
                                data.put("description", description);
                                data.put("type", type);
                                data.put("example", example);
                                data.put("validateexpression", validateexpression);

                                db.insert("product", null, data);
                            }

                        } catch (JSONException e) {
                            Log.e("TelaInserir", "Erro no parsing do JSON: " + e.getMessage(), e);
                            Toast.makeText(getApplicationContext(), "Erro no parsing do JSON", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("TelaInserir", "Erro: " + error.getMessage(), error);
                        Toast.makeText(getApplicationContext(), "Erro na requisição", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        queue.add(dataRequest);
    }

    public void executeGetWater() {
        int limit = 1000;
        int lastSavedId = getLastSavedId("waterManager"); // Obtém o último ID salvo no banco de dados

        RequestQueue queue = Volley.newRequestQueue(this);

        // Requisição para obter o último ID da API
        StringRequest lastIdRequest = new StringRequest(
                Request.Method.GET,
                "http://177.44.248.13:8080/WaterManager?op=LAST",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        int lastId;
                        try {
                            lastId = Integer.parseInt(response.trim()); // Converte a resposta para inteiro
                        } catch (NumberFormatException e) {
                            lastId = 0;
                        }

                        if (lastSavedId < lastId) {
                            String urlConexao = "http://177.44.248.13:8080/WaterManager/?op=SELECT&FORMAT=JSON&LIMIT=" + limit + "&OFFSET=" + lastSavedId;

                            // Requisição para obter os dados
                            JsonArrayRequest dataRequest = new JsonArrayRequest(
                                    urlConexao,
                                    new Response.Listener<JSONArray>() {
                                        @Override
                                        public void onResponse(JSONArray response) {
                                            try {
                                                for (int i = 0; i < response.length(); i++) {
                                                    JSONObject jsonObject = response.getJSONObject(i);

                                                    int id = parseOrDefault(jsonObject.optString("id"), 0);
                                                    int vendorid = parseOrDefault(jsonObject.optString("vendorid"), 0);
                                                    int productid = parseOrDefault(jsonObject.optString("productid"), 0);
                                                    double latitude = parseOrDefaultDouble(jsonObject.optString("latitude"), 0.0);
                                                    double longitude = parseOrDefaultDouble(jsonObject.optString("longitude"), 0.0);
                                                    String value = jsonObject.optString("value");
                                                    String dateinsert = jsonObject.optString("dateinsert");

                                                    ContentValues data = new ContentValues();
                                                    data.put("id", id);
                                                    data.put("vendorid", vendorid);
                                                    data.put("productid", productid);
                                                    data.put("latitude", latitude);
                                                    data.put("longitude", longitude);
                                                    data.put("value", value);
                                                    data.put("dateinsert", dateinsert);

                                                    db.insert("waterManager", null, data);
                                                }

                                                // Verificar se há mais dados a serem buscados
                                                if (response.length() == limit) {
                                                    executeGetWater(); // Buscar mais dados se a resposta foi completa
                                                }

                                            } catch (JSONException e) {
                                                Log.e("TelaConsulta", "Erro no parsing do JSON: " + e.getMessage(), e);
                                                Toast.makeText(getApplicationContext(), "Erro no parsing do JSON", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    },
                                    new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.e("TelaConsulta", "Erro: " + error.getMessage(), error);
                                            Toast.makeText(getApplicationContext(), "Erro na requisição", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                            );

                            queue.add(dataRequest);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("TelaConsulta", "Erro: " + error.getMessage(), error);
                        Toast.makeText(getApplicationContext(), "Erro na requisição", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        queue.add(lastIdRequest);

    }

    // Função para converter String para int, retornando um valor padrão se houver erro
    private int parseOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // Função para converter String para double, retornando um valor padrão se houver erro
    private double parseOrDefaultDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int getLastSavedId(String tab) {
        int lastId = 0;
        String query = "SELECT MAX(id) FROM " + tab;
        cur = db.rawQuery(query, null);
        if (cur.moveToFirst()) {
            lastId = cur.getInt(0);
        }
        cur.close();

        return lastId;
    }



}



