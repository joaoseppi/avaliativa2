package com.example.avaliativa2;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;


public class DatabaseManager extends SQLiteOpenHelper {
    private int last_id = 0;
    public DatabaseManager(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);

    }

    public DatabaseManager(Context context) {
        super(context, "e", null, 1);

    }

    public DatabaseManager(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table waterManager(id integer primary key, vendorid integer not null, productid integer not null, " +
                "latitude real not null, longitude real not null, value text not null, dateinsert text not null);");

        db.execSQL("create table product(id integer primary key, productid integer not null, description text not null, type text not null, example text not null, validateexpression text not null)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void deleteDatabase(Context context) {

    }
}

