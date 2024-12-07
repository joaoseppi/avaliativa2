package com.example.avaliativa2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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

public class MainActivity extends AppCompatActivity implements LocationListener{

    private static final int REQUEST_CODE_LOCATION_PERMISSIONS = 1;

    private LocationManager mlocManager = null;

    private double lon, lat;

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

        db = new DatabaseManager(this, "BancoDadosW", null, 1).getWritableDatabase();
        //DatabaseManager dbm = new DatabaseManager(this);
        //dbm.deleteDatabase(BancoDados);

        executeGetProd();
        executeGetWater();

        mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mlocManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 10, 0, this);

        verificarAlertasProximos();

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

    @Override
    public void onLocationChanged(@NonNull Location location) {
        lon = (location.getLongitude());
        lat = (location.getLatitude());
        salvarLocalizacao(lat,lon);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
    }

    private void salvarLocalizacao(double latitude, double longitude) {
        SharedPreferences sharedPreferences = getSharedPreferences("UsuarioLocalizacao", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("latitude", (float) latitude);
        editor.putFloat("longitude", (float) longitude);
        editor.apply();
    }

    private double[] obterLocalizacao() {
        SharedPreferences sharedPreferences = getSharedPreferences("UsuarioLocalizacao", MODE_PRIVATE);
        double latitude = sharedPreferences.getFloat("latitude", 0.0f);
        double longitude = sharedPreferences.getFloat("longitude", 0.0f);
        return new double[]{latitude, longitude};
    }

    private void enviarNotificacao() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String canalId = "canal_alertas";

        // Criar o canal (necessário para Android 8+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(canalId, "Alertas", NotificationManager.IMPORTANCE_HIGH);
            canal.setDescription("Canal para alertas de falta de água/energia");
            notificationManager.createNotificationChannel(canal);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, canalId)
                .setSmallIcon(R.mipmap.ic_launcher) // Ícone da notificação
                .setContentTitle("Alerta nas proximidades")
                .setContentText("Falta de água ou energia registrada nas proximidades.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }

    private void verificarAlertasProximos() {
        // Obter a localização salva
        double[] localizacao = obterLocalizacao();
        double usuarioLat = localizacao[0];
        double usuarioLon = localizacao[1];

        // Consultar o banco de dados por alertas próximos
        String query = "SELECT * FROM waterManager WHERE ABS(latitude - ?) < 0.01 AND ABS(longitude - ?) < 0.01";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(usuarioLat), String.valueOf(usuarioLon)});

        if (cursor.getCount() > 0) {
            // Se houver alertas, enviar notificação
            enviarNotificacao();
        }

        cursor.close();
    }

}



