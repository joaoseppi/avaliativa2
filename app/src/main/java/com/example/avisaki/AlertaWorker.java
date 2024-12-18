package com.example.avisaki;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

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

public class AlertaWorker extends Worker implements LocationListener {

    private SQLiteDatabase db = null;

    private static final int REQUEST_CODE_LOCATION_PERMISSIONS = 1;

    private LocationManager mlocManager = null;

    private double lon, lat;

    private Cursor cur = null;

    private Context mContext;

    public AlertaWorker(Context context, WorkerParameters params) {
        super(context, params);
        mContext = context;
    }

    @Override
    public Result doWork() {
        Log.d("AlertaWorker", "Alerta disparado");

        db = new DatabaseManager(mContext, "BancoDadosW", null, 1).getWritableDatabase();

        // Executar as funções necessárias
        executeGetWater();
        executeGetProd();

        mlocManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure();
        }

        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> {
            try {
                mlocManager.requestSingleUpdate(LocationManager.FUSED_PROVIDER, this, null);
            } catch (SecurityException e) {
                Log.e("AlertaWorker", "Permissão de localização não garantida.", e);
            }
        });

        verificarAlertasProximos();
        return Result.success();
    }

    public void executeGetProd() {
        int lastSavedId = getLastSavedId("product");

        RequestQueue queue = Volley.newRequestQueue(mContext);
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
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("TelaInserir", "Erro: " + error.getMessage(), error);
                    }
                }
        );

        queue.add(dataRequest);
    }

    public void executeGetWater() {
        int limit = 1000;
        int lastSavedId = getLastSavedId("waterManager"); // Obtém o último ID salvo no banco de dados
        String lastSavedDate = getLastSavedDate("waterManager");

        RequestQueue queue = Volley.newRequestQueue(mContext);

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
                            String urlConexao = "http://177.44.248.13:8080/WaterManager/?op=SELECT&FORMAT=JSON&LIMIT=" + limit + "&DATEINI=" + lastSavedDate;

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
                                            }
                                        }
                                    },
                                    new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.e("TelaConsulta", "Erro: " + error.getMessage(), error);
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

    private String getLastSavedDate(String tab) {
        String lastDate = "";
        String query = "SELECT MAX(dateinsert) FROM " + tab;
        cur = db.rawQuery(query, null);
        if (cur.moveToFirst()) {
            lastDate = cur.getString(0);
        }
        cur.close();
        if(lastDate!=null) {
            return lastDate;
        }else{
            return lastDate = "2024-09-01";
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        lat = location.getLatitude();
        lon = location.getLongitude();
        if(lat!=0.0 && lon!=0.0) {
            salvarLocalizacao();
        }
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
    }

    private void salvarLocalizacao() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("UsuarioLocalizacao", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("latitude", (float) lat);
        editor.putFloat("longitude", (float) lon);
        editor.apply();
    }

    private double[] obterLocalizacao() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("UsuarioLocalizacao", Context.MODE_PRIVATE);
        lat = sharedPreferences.getFloat("latitude", 0.0f);
        lon = sharedPreferences.getFloat("longitude", 0.0f);
        return new double[]{lat, lon};
    }

    private void verificarAlertasProximos() {
        // Obter a localização salva
        double[] localizacao = obterLocalizacao();
        double usuarioLat = localizacao[0];
        double usuarioLon = localizacao[1];

        // Consultar o banco de dados por alertas próximos
        String query = "SELECT * FROM waterManager WHERE ABS(latitude - ?) < 0.01 AND ABS(longitude - ?) < 0.01 AND value != '1'";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(usuarioLat), String.valueOf(usuarioLon)});

        if (cursor.getCount() > 0) {
            // Se houver alertas, enviar notificação
            enviarNotificacao();
        }

        cursor.close();
    }

    private void enviarNotificacao() {
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        String canalId = "canal_alertas";

        // Criar o canal (necessário para Android 8+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(canalId, "Alertas", NotificationManager.IMPORTANCE_HIGH);
            canal.setDescription("Canal para alertas de falta de água/energia");
            notificationManager.createNotificationChannel(canal);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, canalId)
                .setSmallIcon(R.mipmap.ic_launcher) // Ícone da notificação
                .setContentTitle("Alerta nas proximidades")
                .setContentText("Falta de água ou energia registrada nas proximidades.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }

}