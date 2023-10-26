package com.example.fillndrive;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "Distributori";
    private static final String stazioniUrl = "https://www.mimit.gov.it/images/exportCSV/anagrafica_impianti_attivi.csv";
    private static final String carburantiUrl = "https://www.mimit.gov.it/images/exportCSV/prezzo_alle_8.csv";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        System.out.println("onCreate avviata");
        db.execSQL("CREATE TABLE IF NOT EXISTS stazioni(idImpianto INTEGER PRIMARY KEY, Bandiera TEXT, Tipo_Impianto TEXT, Latitudine REAL, Longitudine REAL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS carburanti(idImpianto INTEGER, descCarburante TEXT, Prezzo REAL, isSelf INTEGER)");
    }

    public void updateData() throws IOException, InterruptedException {
        SQLiteDatabase db = this.getWritableDatabase();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> updateTable(db, stazioniUrl, "stazioni", 10));
        executor.submit(() -> updateTable(db, carburantiUrl, "carburanti", 5));

        executor.shutdown();
        db.close();
    }

    private void updateTable(SQLiteDatabase db, String csvUrl, String tableName, int expectedCol) {
        try {
            final URL cu = new URL(csvUrl);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(cu.openStream()))) {
                String line;
                int i = 0;

                db.beginTransaction();

                String deleteQuery = "DELETE FROM " + tableName;
                db.execSQL(deleteQuery);

                while ((line = in.readLine()) != null) {
                    if (i >= 2) {
                        String[] data = line.split(";");
                        if(data.length == expectedCol)
                            insert(db, tableName, data);
                    }
                    i++;
                }
                in.close();
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void insert(SQLiteDatabase db, String tableName, String[] data) {
        if(tableName.equals("stazioni")){
            ContentValues cv = new ContentValues();
            cv.put("idImpianto", data[0]);
            cv.put("Bandiera", data[2]);
            cv.put("Tipo_Impianto", data[3]);
            cv.put("Latitudine", data[8]);
            cv.put("Longitudine", data[9]);
            db.insert("stazioni", null, cv);
        }else if(tableName.equals("carburanti")){
            ContentValues cv = new ContentValues();
            cv.put("idImpianto", data[0]);
            cv.put("descCarburante", data[1]);
            cv.put("Prezzo", data[2]);
            cv.put("isSelf", data[3]);
            db.insert("carburanti", null, cv);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

}
