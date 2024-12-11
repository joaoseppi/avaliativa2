package com.example.avisaki;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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

        db = new DatabaseManager(this, "BancoDadosW", null, 1).getWritableDatabase();
        //DatabaseManager dbm = new DatabaseManager(this);
        //dbm.deleteDatabase(BancoDados);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSIONS);
        }

        Intent serviceIntent = new Intent(this, Services.class);
        startService(serviceIntent);

        mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mlocManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 10, 0, this);

        verificarGPS();

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
                obterLocalizacao();
                Intent intent = new Intent(MainActivity.this, TelaConsulta.class);
                intent.putExtra("latitude", lat);
                intent.putExtra("longitude", lon);
                startActivity(intent);
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

    private void obterLocalizacao() {
        SharedPreferences sharedPreferences = getSharedPreferences("UsuarioLocalizacao", MODE_PRIVATE);
        lat = (double) sharedPreferences.getFloat("latitude", 0.0f);
        lon = (double) sharedPreferences.getFloat("longitude", 0.0f);
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



