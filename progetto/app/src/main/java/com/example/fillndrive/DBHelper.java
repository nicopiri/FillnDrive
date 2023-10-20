package com.example.fillndrive;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "Distributori";
    SQLiteDatabase db;
    public DBHelper(Context context) {
        super(context, DB_NAME, null, 1);
        db = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS stazioni(idImpianto INTEGER PRIMARY KEY, Bandiera TEXT, Tipo_Impianto TEXT, Latitudine REAL, Longitudine REAL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS carburanti" +
                "(idImpianto INTEGER, descCarburante TEXT, Prezzo REAL, isSelf INTEGER)");
    }


    public void addData(String csvUrl, String tableName) throws IOException {
        URL cu = new URL(csvUrl);
        BufferedReader in = new BufferedReader(new InputStreamReader(cu.openStream()));
        List<String> lines = in.lines().collect(Collectors.toList());
        in.close();

        if(lines.size() > 0) {
            lines.remove(0);
            lines.remove(1);
        }


        db.beginTransaction(); // Inizia una transazione per migliorare le prestazioni
        db.execSQL("DELETE FROM stazioni");
        db.execSQL("DELETE FROM carburanti");
        for (String line : lines) {
            String[] data = line.split(";");
            if (data.length != 10) {
                continue; // Salta questa riga e passa alla successiva
            }
            ContentValues cv = new ContentValues();
            cv.put("idImpianto", data[0]);
            cv.put("Bandiera", data[2]);
            cv.put("Tipo_Impianto", data[3]);
            cv.put("Latitudine", data[8]);
            cv.put("Longitudine", data[9]);

            db.insert("stazioni", null, cv);
        }

        db.endTransaction();
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
