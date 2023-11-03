package com.example.fillndrive;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DBHelper extends SQLiteOpenHelper {

    // Definizione di costanti per il nome del database e gli URL dei dati
    private static final String DB_NAME = "Distributori";
    private static final String stazioniUrl = "https://www.mimit.gov.it/images/exportCSV/anagrafica_impianti_attivi.csv";
    private static final String carburantiUrl = "https://www.mimit.gov.it/images/exportCSV/prezzo_alle_8.csv";


    // Costruttore della classe DBHelper
    public DBHelper(Context context) {
        super(context, DB_NAME, null, 1);
    }



    @Override
    public void onCreate(SQLiteDatabase db) {
        // Creazione delle tabelle nel database se non esistono già
        db.execSQL("CREATE TABLE IF NOT EXISTS stazioni(idImpianto INTEGER PRIMARY KEY, Bandiera TEXT, Tipo_Impianto TEXT, Comune TEXT, Provincia TEXT, Latitudine TEXT, Longitudine TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS carburanti(idImpianto INTEGER, descCarburante TEXT, Prezzo REAL, isSelf BOOLEAN)");
    }


    // Metodo per aggiornare i dati dai file CSV
    public void updateData() throws IOException, InterruptedException {
        SQLiteDatabase db = this.getWritableDatabase();

        String sqlS = "insert into stazioni(idImpianto, Bandiera, Tipo_Impianto, Comune, Provincia, Latitudine, Longitudine) values (?,?,?,?,?,?,?);";
        String sqlC = "insert into carburanti(idImpianto, descCarburante, Prezzo, isSelf) values (?,?,?,?);";

        // Crea un pool di thread con 2 thread
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> updateTable(db, sqlS, stazioniUrl, "stazioni", 10));
        executor.submit(() -> updateTable(db, sqlC, carburantiUrl, "carburanti", 5));

        executor.shutdown();
    }

    // Metodo per l'aggiornamento di una tabella nel database da un file CSV
    private void updateTable(SQLiteDatabase db, String query, String csvUrl, String tableName, int expectedCol) {
        try {
            final URL cu = new URL(csvUrl);
            SQLiteStatement stm = db.compileStatement(query);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(cu.openStream()))) {
                String line;
                int i = 0;

                db.beginTransaction();

                String deleteQuery = "DELETE FROM " + tableName;
                db.execSQL(deleteQuery); // Cancella tutti i dati dalla tabella prima di aggiornarla

                while ((line = in.readLine()) != null) {
                    if (i >= 2) {
                        String[] data = line.split(";");
                        if(data.length == expectedCol) {
                            insert(tableName, stm, data);
                            stm.clearBindings();
                        }
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

    // Metodo per l'inserimento dei dati in base al nome della tabella
    private void insert(String tableName, SQLiteStatement stm, String[] data) {
        if(tableName.equals("stazioni")){
            stm.bindString(1, data[0]);
            stm.bindString(2, data[2]);
            stm.bindString(3, data[3]);
            stm.bindString(4, data[6])
            stm.bindString(5, data[7])
            stm.bindString(6, data[8]);
            stm.bindString(7, data[9]);
            stm.executeInsert();
        }else if(tableName.equals("carburanti")){
            stm.bindString(1, data[0]);
            stm.bindString(2, data[1]);
            stm.bindString(3, data[2]);
            stm.bindString(4, data[3]);
            stm.executeInsert();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

}
