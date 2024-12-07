package com.example.avaliativa2;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;
import java.util.List;

public class TelaConsulta extends AppCompatActivity {

    private Button btVoltarConsulta = null;

    private TextView tvDate = null;

    private SQLiteDatabase db =null;

    private Cursor cur = null;

    private List<String> productsList = new ArrayList<>();

    private Spinner scProductId = null;

    private String numProd = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_consulta);

        db = new DatabaseManager(this, "BancoDados", null, 1).getWritableDatabase();

        WebView webView = (WebView) findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/mapa.html");

        tvDate = (TextView) findViewById(R.id.tv_date);

        atualizaDados();

        scProductId = (Spinner) findViewById(R.id.s_cproduct_id); // Spinner que o usuário vai usar para filtrar

        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, productsList);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        scProductId.setAdapter(ad);

        scProductId.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedProductId = scProductId.getSelectedItem().toString();

                if (selectedProductId.equals("Selecione uma opção")) {
                    return;
                }

                String[] parts = selectedProductId.split("- ");
                numProd = parts[0];
                String lastdate = null;

                // Realize a consulta no banco filtrando pelo ProductId
                Cursor cursor = db.query("waterManager", new String[]{"latitude", "longitude", "dateinsert"}, "productid=?", new String[]{numProd}, null, null, null);

                // Coletando coordenadas e passando para a WebView
                StringBuilder coordinates = new StringBuilder("[");
                while (cursor.moveToNext()) {
                    coordinates.append("[")
                            .append(cursor.getString(0)).append(", ") // Latitude
                            .append(cursor.getString(1)).append(", ") // Longitude
                            .append("0.5") // Intensidade fixa
                            .append("],");
                    lastdate = cursor.getString(2);
                }
                cursor.close();

                if (coordinates.length() > 1) {
                    coordinates.setLength(coordinates.length() - 1); // Remove a última vírgula
                }
                coordinates.append("]");

                webView.evaluateJavascript("setMapData(" + coordinates + ");", null);
                Log.d("WebViewData", "Coordenadas: " + coordinates);

                if(lastdate == null){
                    lastdate =  "Sem informações";
                } else {
                lastdate = ("Última informação: " + lastdate);
                }
                tvDate.setText(lastdate);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Caso nada seja selecionado, você pode exibir todos os dados ou fazer outra ação
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e("WebViewError", description); // Aqui vai aparecer o erro no Log
            }
        });

        btVoltarConsulta = (Button) findViewById(R.id.bt_voltar_consulta);
        btVoltarConsulta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void atualizaDados() {
        cur = db.query("product", new String[]{"productid", "description"}, null, null, null, null, null);

        productsList.clear();

        productsList.add("Selecione uma opção");

        if (cur.moveToFirst()) {
            do {
                String productid = cur.getString(0);
                String description = cur.getString(1);

                productsList.add(productid + "- " + description);
            } while (cur.moveToNext());
        }
        cur.close();
    }


}


