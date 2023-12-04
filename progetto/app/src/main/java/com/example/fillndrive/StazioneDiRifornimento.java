package com.example.fillndrive;

import com.google.android.gms.maps.model.LatLng;

public class StazioneDiRifornimento {

    private int idImpianto;
    private String bandiera;
    private double prezzo;
    private LatLng coordinate;
    private double distanzaInKm;

    private double indiceConsumo;

    public StazioneDiRifornimento(int idImpianto, String bandiera, double prezzo, LatLng coordinate) {
        this.idImpianto = idImpianto;
        this.bandiera = bandiera;
        this.prezzo = prezzo;
        this.coordinate = coordinate;
    }

    public int getIdImpianto() {
        return idImpianto;
    }

    public String getBandiera() {
        return bandiera;
    }

    public double getPrezzo() {
        return prezzo;
    }

    public LatLng getCoordinate() {
        return coordinate;
    }

    public double getDistanzaInKm() {
        return distanzaInKm;
    }

    public double getIndiceConsumo() {
        return indiceConsumo;
    }

    public void setIndiceConsumo(double indiceConsumo) {
        this.indiceConsumo = indiceConsumo;
    }

    public void setDistanzaInKm(double distanzaInKm) {
        this.distanzaInKm = distanzaInKm;
    }

    public void setIdImpianto(int idImpianto) {
        this.idImpianto = idImpianto;
    }

    public void setBandiera(String bandiera) {
        this.bandiera = bandiera;
    }

    public void setPrezzo(double prezzo) {
        this.prezzo = prezzo;
    }

    public void setCoordinate(LatLng coordinate) {
        this.coordinate = coordinate;
    }
}
