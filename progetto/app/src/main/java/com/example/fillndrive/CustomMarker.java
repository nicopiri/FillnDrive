package com.example.fillndrive;

import com.google.android.gms.maps.model.LatLng;

public class CustomMarker {
    private String title;
    private String snippet;

    private String info;
    private LatLng coordinates;

    public CustomMarker(String title, String snippet, String info, LatLng coordinates) {
        this.title = title;
        this.snippet = snippet;
        this.coordinates = coordinates;
        this.info = info;
    }

    public String getInfo() {
        return info;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public LatLng getCoordinates() {
        return coordinates;
    }
}
