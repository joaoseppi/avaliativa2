package com.example.avaliativa2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class TelaConsulta extends AppCompatActivity {

    private Button btVoltarConsulta = null;

    private TextView tvDados = null;

    private SQLiteDatabase db =null;

    private Cursor cur = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_consulta);

        db = new DatabaseManager(this, "BancoDados", null, 1).getWritableDatabase();

        tvDados = (TextView) findViewById(R.id.tv_dados);

        atualizaDados();

        btVoltarConsulta = (Button) findViewById(R.id.bt_voltar_consulta);
        btVoltarConsulta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void atualizaDados() {
        String informacoes = "";

        cur = db.query("waterManager", new String[]{"id", "vendorid", "productid", "latitude", "longitude", "value", "dateinsert"}, null, null, null, null, null);

        if (cur.moveToFirst()) {
            do {
                int id = cur.getInt(0);
                String vendorid = cur.getString(1);
                String productid = cur.getString(2);
                String latitude = cur.getString(3);
                String longitude = cur.getString(4);
                String value = cur.getString(5);
                String dateinsert = cur.getString(6);

                informacoes += "\nID: " + id + " vendorid: " + vendorid + " productid: " + productid + " lat.: " + latitude +
                                " long.: " + longitude + " value: " + value + " dateinsert: " + dateinsert;

            } while (cur.moveToNext());
        }
        cur.close();

        tvDados.setText(informacoes);
    }

}


