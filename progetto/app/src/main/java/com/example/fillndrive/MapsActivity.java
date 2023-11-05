package com.example.fillndrive;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private Marker randomMarker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Controlla se ci sono i permessi per la geolocalizzazione
        if (ContextCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {

            // Richiede i permessi
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            mMap.setMyLocationEnabled(true);

            FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);
            locationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {

                    // Trova la lista delle stazioni di rifornimento presenti nel comune della posizione
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    Address address = getAddressFromLatLong(location.getLatitude(), location.getLongitude());
                    String comune = address.getLocality();
                    List<StazioneDiRifornimento> listaStazioni = getListaStazioni(comune);

                    //Crea i marker sulla mappa corrispondenti alle stazioni di rifornimento trovate
//                    createMarkers(listaStazioni);

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14));
                }
            });

            mMap.setOnMarkerClickListener(marker -> {
                if (marker.equals(randomMarker)) {
                    // Handle marker click event here
                    // You can navigate to the marker's location or perform any other action.
                    navigateToMarkerLocation(marker.getPosition());
                }
                return false;
            });
        }

    }

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

    private void createMarkers(List<StazioneDiRifornimento> listaStazioni) {
        // Add a random marker
        randomMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(45.50570629421494, 12.132273545222072))
                .title("1.899 €")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        randomMarker.showInfoWindow();
    }

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

//        String query = "SELECT bandiera, comune, provincia, prezzo, longitudine, latitudine FROM stazioni NATURAL JOIN carburanti WHERE comune = ?";
//        Cursor cursor = dbConnection.rawQuery(query, new String[]{comune});

        String query = "SELECT s.bandiera, s.latitudine, s.longitudine, c.prezzo FROM stazioni s NATURAL JOIN carburanti c";
        Cursor cursor = dbConnection.rawQuery(query, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String bandiera = cursor.getString(0);
                    String latitudine = cursor.getString(1);
                    String longitudine = cursor.getString(2);
                    double prezzo = cursor.getDouble(3);
//                    double latitudine = cursor.getDouble(1);
//                    double longitudine = cursor.getDouble(2);

                    listaStazioni.add(new StazioneDiRifornimento(bandiera, null, prezzo, latitudine, longitudine));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        //dbConnection.close();
        return listaStazioni;
    }


    /**
     * Metodo che avvia la navigazione verso una destinazione utilizzando le API di Google.
     *
     * @param destination
     */
    private void navigateToMarkerLocation(LatLng destination) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destination.latitude + "," + destination.longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps"); // Specify the Google Maps app package

        // Check if the Google Maps app is installed
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Google Maps non installato", Toast.LENGTH_SHORT).show();
        }
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