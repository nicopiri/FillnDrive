package com.example.myapplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {
    public DBHelper(Context context) {
        super(context, "", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS stazioni(idImpianto INTEGER PRIMARY KEY, Bandiera TEXT, Tipo_Impianto TEXT, Latitudine REAL, Longitudine REAL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS carburanti" +
                "(idImpianto INTEGER PRIMARY KEY, descCarburante TEXT PRIMARY KEY, Prezzo REAL, isSelf INTEGER PRIMARY KEY, FOREIGN KEY(idImpianto) REFERENCES stazioni(idImpianto))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
