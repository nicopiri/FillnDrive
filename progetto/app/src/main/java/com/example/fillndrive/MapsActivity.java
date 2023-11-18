package com.example.fillndrive;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.fillndrive.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private DBHelper db;
    private SQLiteDatabase dbConnection;
    private GoogleMap googleMap;
    private ActivityMapsBinding binding;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private EditText searchEditText;

    private LatLng currentLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        searchEditText = findViewById(R.id.search_edit_text);
        Button searchButton = findViewById(R.id.search_button);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performSearch();
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void performSearch() {
        String location = searchEditText.getText().toString();
        if (location != null && !location.equals("")) {
            List<Address> addressList = null;

            // Utilizza un Geocoder per cercare l'indirizzo
            Geocoder geocoder = new Geocoder(MapsActivity.this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (addressList != null && addressList.size() > 0) {
                Address address = addressList.get(0);

                // Sposta la camera alla posizione dell'indirizzo trovato
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            } else {
                // Mostra un messaggio di errore se l'indirizzo non viene trovato
                Toast.makeText(MapsActivity.this, "Indirizzo non trovato", Toast.LENGTH_SHORT).show();
            }
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // Controlla se ci sono i permessi per la geolocalizzazione
        if (ContextCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {

            // Richiede i permessi
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            this.googleMap.setMyLocationEnabled(true);

            FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);
            locationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {

                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                    // Trova la lista delle stazioni di rifornimento presenti nel comune della posizione
                    // TODO: rimuovere la ricerca per comune e farla invece per lat long
                    Address address = getAddressFromLatLong(location.getLatitude(), location.getLongitude());
                    String comune = address.getLocality();
                    List<StazioneDiRifornimento> listaStazioniByComune = getListaStazioni(comune.toUpperCase());

                    //TODO: da capire di quanto deve essere il raggio. Momentaneamente posto a 7 km.
                    // Questa parte va eliminata perché la query al db deve essere implementata in modo che
                    // si estragga già soltanto le stazioni di interesse entro una certa distanza.
                    List<StazioneDiRifornimento> listaStazioniIn7Km = getListaStazioniIn7Km(listaStazioniByComune, location.getLatitude(), location.getLongitude());

                    // TODO: calcolare l'indice di convenienza e ordinare la listaStazioni in modo da colorare propriamente i marker nel metodo sotto
                    createMarkers(listaStazioniIn7Km);

                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14));
                }
            });

            googleMap.setOnMarkerClickListener(marker -> {
                showMarkerInformation(marker);
                return true;
            });

        }
    }
    private double calculateDistanceToMarker(LatLng markerCoordinates) {
        // Check if the app has the necessary location permissions
        if (ContextCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return -1; // Return -1 to indicate that the permission is not granted
        }

        // Get the current location
        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);
        locationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {

                LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());

                double distance = calculateDistance(origin.latitude, origin.longitude, markerCoordinates.latitude, markerCoordinates.longitude);

                // Now 'distance' contains the distance in kilometers
                Toast.makeText(MapsActivity.this, "Distanza al marker: " + distance + " km", Toast.LENGTH_SHORT).show();
            }
        });

        return 0; // Placeholder value, you can replace it with a meaningful value or handle it differently
    }
    private void showMarkerInformation(Marker marker) {
        LatLng coordinates = marker.getPosition();
        String title = marker.getTitle();
        String snippet = marker.getSnippet();
        String info = "Custom info";

        CustomMarkerInfoFragment infoFragment = CustomMarkerInfoFragment.newInstance(title, snippet, info, coordinates);
        calculateDistanceToMarker(coordinates);
        // Passa l'istanza di GoogleMap al fragment
        infoFragment.setGoogleMap(googleMap);

        // Mostra il fragment
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, infoFragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Recupera l'indirizzo della posizione attuale dell'utente.
     * @param latitude
     * @param longitude
     * @return
     */
    private Address getAddressFromLatLong(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0);
            }
        } catch (IOException e) {
            Toast.makeText(this, "Impossibile ottenere l'indirizzo", Toast.LENGTH_SHORT).show();
        }
        return null;
    }


    private List<StazioneDiRifornimento> getListaStazioniIn7Km(List<StazioneDiRifornimento> allStazioni, double userLat, double userLng) {
        List<StazioneDiRifornimento> stazioniInRadius = new ArrayList<>();

        for (StazioneDiRifornimento stazione : allStazioni) {
            double stazioneLat = stazione.getLatitudine();
            double stazioneLng = stazione.getLongitudine();

            // Calcola la distanza tra l'utente e la stazione
            double distance = calculateDistance(userLat, userLng, stazioneLat, stazioneLng);

            // Aggiunge la stazione solo se è entro i 7 km
            if (distance <= 7.0) {
                stazioniInRadius.add(stazione);
            }
        }
        return stazioniInRadius;
    }

    /**
     * Haversine formula to calculate distance between two points on a sphere.
     * @param userLat
     * @param userLng
     * @param stationLat
     * @param stationLng
     * @return
     */
    private double calculateDistance(double userLat, double userLng, double stationLat, double stationLng) {
        double earthRadius = 6371; // Radius of the Earth in kilometers

        double latDiff = Math.toRadians(stationLat - userLat);
        double lngDiff = Math.toRadians(stationLng - userLng);

        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2)
                + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(stationLat))
                * Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c; // Distance in kilometers
    }

    /**
     * Crea i marker sulla mappa corrispondenti alle stazioni di rifornimento trovate.
     * @param listaStazioni
     */
    private void createMarkers(List<StazioneDiRifornimento> listaStazioni) {
        for (StazioneDiRifornimento stazione : listaStazioni) {
            googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(stazione.getLatitudine(), stazione.getLongitudine()))
                    .title(String.valueOf(stazione.getPrezzo()))
                    .snippet(stazione.getBandiera())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))); //TODO: cambiare colore icona marker
        }
    }

    /**
     * Legge dal db le stazioni vicine alla posizione di riferimento, che può essere
     * la posizione attuale dell'utente oppure il luogo specifico cercato.
     * @param comune
     * @return
     */
    public List<StazioneDiRifornimento> getListaStazioni(String comune) {
        List<StazioneDiRifornimento> listaStazioni = new ArrayList<>();
        db = DBHelper.getInstance(this);

        int currentDay = DateUtility.getCurrentDay(this);
        int lastDay = DateUtility.getLastDay(this);

        if(lastDay != currentDay) {
            try {
                db.waitForUpdate(); // attende che entrambi i thread della pool siano terminati

                // Aggiorna la data nelle preferenze condivise
                SharedPreferences.Editor editor = DateUtility.getSharedPreferences(MapsActivity.this).edit();
                editor.putInt("day", currentDay);
                editor.commit();

            } catch (InterruptedException e) {
                Log.e(TAG, "An error occurred: " + e.getMessage(), e);
            }
        }

        // Now, it's safe to query the database
        dbConnection = db.getReadableDatabase();

        String query = "SELECT s.IdImpianto, s.bandiera, s.comune, s.latitudine, s.longitudine, MIN(c.prezzo) AS minPrezzo " +
                "FROM stazioni s NATURAL JOIN carburanti c " +
                "WHERE s.comune = ? AND descCarburante LIKE 'Benzina%' " +
                "GROUP BY s.IdImpianto, s.bandiera, s.comune, s.latitudine, s.longitudine";
        Cursor cursor = dbConnection.rawQuery(query, new String[]{comune});

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String id = cursor.getString(0);
                    String bandiera = cursor.getString(1);
                    String comuneFromDb = cursor.getString(2);
                    double latitudine = cursor.getDouble(3);
                    double longitudine = cursor.getDouble(4);
                    double prezzo = cursor.getDouble(5);

                    listaStazioni.add(new StazioneDiRifornimento(Integer.parseInt(id), bandiera, comuneFromDb, prezzo, latitudine, longitudine));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        dbConnection.close();
        return listaStazioni;
    }


    /**
     * Metodo che chiede all'utente il permesso alla localizzazione del dispositivo.
     *
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "I permessi sono necessari", Toast.LENGTH_SHORT).show();
            }
            else {
                Intent intent = new Intent(MapsActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        }
    }
}