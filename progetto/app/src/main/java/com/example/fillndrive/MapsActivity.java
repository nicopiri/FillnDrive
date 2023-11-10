package com.example.fillndrive;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
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
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private Marker randomMarker;
    private Marker randomMarker2;
    private EditText searchEditText;

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
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            } else {
                // Mostra un messaggio di errore se l'indirizzo non viene trovato
                Toast.makeText(MapsActivity.this, "Indirizzo non trovato", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void addCustomMarker(String title, String snippet, LatLng coordinates) {
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(coordinates)
                .title(title)
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        marker.showInfoWindow();
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Richiesta permessi per la geolocalizzazione
        if (ContextCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {

            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            mMap.setMyLocationEnabled(true);

            CustomMarker customMarker1 = new CustomMarker("1.899 €", "snippet", "info", new LatLng(45.50570629421494, 12.132273545222072));
            CustomMarker customMarker2 = new CustomMarker("1.899 €", "snippet", "info", new LatLng(37.407215, -122.090009));

            addCustomMarker(customMarker1.getTitle(), customMarker1.getSnippet(), customMarker1.getCoordinates());
            addCustomMarker(customMarker2.getTitle(), customMarker2.getSnippet(), customMarker2.getCoordinates());


        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);
            locationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

               /*



                   // Add a random marker
                    randomMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(45.50570629421494, 12.132273545222072))
                            .title("1.899 €")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                    randomMarker.showInfoWindow();

                    randomMarker2 = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(37.407215, -122.090009))
                            .title("1.899 €")
                            .snippet("bella napoli")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                 randomMarker2.showInfoWindow();
                */
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14));
                }
           });

            mMap.setOnMarkerClickListener(marker -> {

                    showMarkerInformation(marker);
                    return true;
            });

        }
    }

    private void showMarkerInformation(Marker marker) {
        LatLng coordinates = marker.getPosition();
        String title = marker.getTitle();
        String snippet = marker.getSnippet();
        String info = "Custom info";

        CustomMarkerInfoFragment infoFragment = CustomMarkerInfoFragment.newInstance(title, snippet, info, coordinates);
        infoFragment.show(getSupportFragmentManager(), "marker_info");
    }




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
