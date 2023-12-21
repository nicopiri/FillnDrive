package com.example.fillndrive;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.common.collect.Lists;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TravelMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private DBHelper db;
    private SQLiteDatabase dbConnection;
    private GoogleMap googleMap;
    private ActivityMapsBinding binding;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private EditText searchEditText;
    private GeoApiContext context;
    private LatLng currentLocation;
    private SharedPreferences preferences;
    protected static Polyline currentPolyline;
    protected DirectionsRoute route;
    private static final String DIESEL_QUERY = "\'Gasolio%\' OR \'Diesel%\'";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        context = new GeoApiContext.Builder()
                .apiKey("AIzaSyDaOcHgOYCNOLMlIgmugkb3QC-ZxcQpFbs")
                .build();

        searchEditText = findViewById(R.id.search_edit_text);
        Button searchButton = findViewById(R.id.search_button);
        Button homeButton = findViewById(R.id.home_button); // Aggiunto il riferimento al pulsante "Home"

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performSearch();
            }
        });

        // Aggiunto il listener per il pulsante "Home"
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Aggiungi qui la logica per navigare al primo fragment o all'attività principale
                Intent intent = new Intent(MapsActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Supporta la ricerca per luogo specifico tramite inserimento della città nella barra di ricerca.
     */
    private void performSearch() {
        String location = searchEditText.getText().toString();
        if (location != null && !location.equals("")) {
            List<Address> addressList = null;

            // Utilizza un Geocoder per cercare l'indirizzo
            Geocoder geocoder = new Geocoder(MapsActivity.this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);
            } catch (IOException e) {
                Log.e(TAG, "An error occurred: " + e.getMessage(), e);
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

                    currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

                    // Trova la lista di stazioni in un raggio di 5km
                    List<StazioneDiRifornimento> listaStazioni = getListaStazioni(currentLocation);

                    if (!listaStazioni.isEmpty()) {

                        // Per ogni stazione, calcola la distanza dalla posizione corrente
                        listaStazioni.forEach(stazione -> {
                            try {
                                double distanza = calcolaDistanza(stazione, currentLocation);
                                stazione.setDistanzaInKm(distanza);

                            } catch (IOException | InterruptedException | ApiException e) {
                                Log.e(TAG, "An error occurred: " + e.getMessage(), e);
                            }
                        });

                        // STEP 1. Imposta l'indice di consumo e in base a questo ordina la lista
                        listaStazioni.forEach(stazione -> stazione.setIndiceConsumo(calcolaIndice(stazione)));

                        List<StazioneDiRifornimento> listaStazioniOrdinataPerIndice = listaStazioni.stream()
                                .sorted(Comparator.comparingDouble(StazioneDiRifornimento::getIndiceConsumo))
                                .collect(Collectors.toList());

                        // STEP 2. Ordina la lista per convenienza considerando 500 mt come distanza trascurabile
                        List<StazioneDiRifornimento> listaStazioniOrdinata = ordinaPrezzoCrescenteOgni500mt(listaStazioniOrdinataPerIndice);

                        // Imposta il percorso di default per la stazione più conveniente
                        StazioneDiRifornimento stazionePredefinita = listaStazioniOrdinata.stream().findFirst().orElse(null);
                        Marker marker = createNewMarker(stazionePredefinita, BitmapDescriptorFactory.HUE_GREEN);

                        // Imposta l'icona del marker predefinito
                        marker.setIcon(BitmapDescriptorFactory.fromBitmap(resizeBitmap(R.drawable.pin, 60, 100)));
                        
                        showMarkerInformation(marker);
                        drawRoute(currentLocation, marker.getPosition());
                        listaStazioniOrdinata.remove(0);

                        if (!listaStazioniOrdinata.isEmpty()) {

                            // STEP 3. Divide la lista in 3 parti uguali sulle quali stabilisce il colore del marker
                            int partitionSize = (int) Math.ceil((double) listaStazioniOrdinata.size() / 3);
                            List<List<StazioneDiRifornimento>> partitions = Lists.partition(listaStazioniOrdinata, partitionSize);
                            List<StazioneDiRifornimento> part1 = partitions.get(0);
                            List<StazioneDiRifornimento> part2;
                            List<StazioneDiRifornimento> part3;

                            createMarkersIntoMap(part1, BitmapDescriptorFactory.HUE_GREEN);
                            if (partitions.size() == 3) {
                                part3 = partitions.get(2);
                                createMarkersIntoMap(part3, BitmapDescriptorFactory.HUE_RED);
                            }
                            if (partitions.size() >= 2) {
                                part2 = partitions.get(1);
                                createMarkersIntoMap(part2, BitmapDescriptorFactory.HUE_YELLOW);
                            }
                        }
                    }
                    else {
                        Toast.makeText(MapsActivity.this, "Nessun distributore rilevato nelle vicinanze.", Toast.LENGTH_SHORT).show();
                    }

                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                }
            });

            googleMap.setOnMarkerClickListener(marker -> {
                drawRoute(currentLocation, marker.getPosition());
                showMarkerInformation(marker);
                return true;
            });

        }
    }

    private List<StazioneDiRifornimento> ordinaPrezzoCrescenteOgni500mt(List<StazioneDiRifornimento> listaStazioniOrdinataPerIndice) {
        List<StazioneDiRifornimento> listaOrdinata = new ArrayList<>();
        List<List<StazioneDiRifornimento>> partizioniPerDistanza = new ArrayList<>();

        // Divide la lista in partizioni. Ogni partizione contiene le stazioni ubicate al massimo a 500 mt di distanza dalle altre.
        for (StazioneDiRifornimento stazione : listaStazioniOrdinataPerIndice) {
            boolean aggiuntaAllaPartizione = false;

            for (List<StazioneDiRifornimento> partizione : partizioniPerDistanza) {
                if (partizione.stream().allMatch(s -> Math.abs(s.getDistanzaInKm() - stazione.getDistanzaInKm()) <= 0.500)) {
                    partizione.add(stazione);
                    aggiuntaAllaPartizione = true;
                    break;
                }
            }

            if (!aggiuntaAllaPartizione) {
                List<StazioneDiRifornimento> novaPartizione = new ArrayList<>();
                novaPartizione.add(stazione);
                partizioniPerDistanza.add(novaPartizione);
            }
        }

        // Ordina ogni partizione per prezzo crescente e crea una lista unica
        for (List<StazioneDiRifornimento> partizione : partizioniPerDistanza) {
            partizione.sort(Comparator.comparingDouble(s -> s.getPrezzo()));
            listaOrdinata.addAll(partizione);
        }

        return listaOrdinata;
    }

    private Bitmap resizeBitmap(int drawable_id, int width, int height){
        Bitmap b = BitmapFactory.decodeResource(getResources(),  drawable_id);
        return Bitmap.createScaledBitmap(b, width, height, false);
    }

    private double calcolaIndice(StazioneDiRifornimento stazione) {
        double indice = stazione.getDistanzaInKm() * stazione.getPrezzo();
        return Math.round(indice * 1000.0) / 1000.0;
    }

    private double calcolaDistanza(StazioneDiRifornimento stazione, LatLng origin) throws IOException, InterruptedException, ApiException {
        route = calculateRoute(origin, stazione.getCoordinate());
        return getRouteDistanceKm(route);
    }

    /**
     * Mostra la finestra con le informazioni aggiuntiva sulla stazione di rifornimento selezionata.
     * @param marker
     */
    private void showMarkerInformation(Marker marker) {
        LatLng coordinates = marker.getPosition();
        String title = marker.getTitle() + " €";
        String snippet = marker.getSnippet();
        String info = getRouteDurationMinutes(route) +  " Minuti  --  " + getRouteDistanceKm(route) +  "Km";
        CustomMarkerInfoFragment infoFragment = CustomMarkerInfoFragment.newInstance(title, snippet, info, coordinates);

        // Passa l'istanza di GoogleMap al fragment
        infoFragment.setGoogleMap(googleMap);

        // Mostra il fragment
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, infoFragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Crea i marker sulla mappa corrispondenti alle stazioni di rifornimento trovate.
     * @param listaStazioni
     */
    private void createMarkersIntoMap(List<StazioneDiRifornimento> listaStazioni, float colorCode) {
        for (StazioneDiRifornimento stazione : listaStazioni) {
            googleMap.addMarker(createMarkerOptions(stazione, colorCode));
        }
    }

    @NonNull
    private MarkerOptions createMarkerOptions(StazioneDiRifornimento stazione, float colorCode) {
        return new MarkerOptions()
                .position(stazione.getCoordinate())
                .title(String.valueOf(stazione.getPrezzo()))
                .snippet(stazione.getBandiera())
                .icon(BitmapDescriptorFactory.defaultMarker(colorCode));
    }

    private Marker createNewMarker(StazioneDiRifornimento stazione, float colorCode) {
        MarkerOptions markerOptions = createMarkerOptions(stazione, colorCode);
        return googleMap.addMarker(markerOptions);
    }

    /** 
     * Dati due punti disegna il percorso sulla mappa 
     * @param origin
     * @param destination
     */
    private void drawRoute(LatLng origin, LatLng destination) {
        try {
            route = calculateRoute(origin, destination);

            if (currentPolyline != null) {
                currentPolyline.remove();
            }
            PolylineOptions polylineOptions = new PolylineOptions()
                    .color(Color.BLUE)
                    .width(10);

            route.overviewPolyline.decodePath().forEach(point ->
                    polylineOptions.add(new LatLng(point.lat, point.lng)));

            currentPolyline = googleMap.addPolyline(polylineOptions);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DirectionsRoute calculateRoute(LatLng origin, LatLng destination) throws ApiException, InterruptedException, IOException {
        DirectionsResult result = DirectionsApi.newRequest(context)
                .origin(new com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                .destination(new com.google.maps.model.LatLng(destination.latitude, destination.longitude))
                .mode(TravelMode.DRIVING)
                .await();

        return result.routes[0];
    }


    /**
     * Dato un percorso restituisce la durata in minuti
     * @param route percorso
     */
    private int getRouteDurationMinutes(DirectionsRoute route){
        if(route != null) {
            long secondi = route.legs[0].duration.inSeconds;
            int minuti = (int) (secondi / 60);
            long secondiRimanenti = secondi % 60;
            if (secondiRimanenti >= 30) {
                minuti++; // Round up to the next minute
            }
            return minuti;
        }
        return -1;
    }

    /**
     * Dato un percorso restituisce la sua lunghezza in km
     * @param route percorso
     */
    private double getRouteDistanceKm(DirectionsRoute route){
        if(route != null)
            return (double)route.legs[0].distance.inMeters / 1000;
        return -1;
    }

    
    /**
     * Legge dal db le stazioni vicine alla posizione di riferimento, che può essere
     * la posizione attuale dell'utente oppure il luogo specifico cercato.
     * Il raggio di default è 1.5 km.
     * Se la lista è vuota aumenta il raggio e ripete la query.
     *
     * @param userLocation La posizione dell'utente o quella da lui cercata
     * @return List di StazioniDiRifornimento entro 5km dalla posizione.
     */
    public List<StazioneDiRifornimento> getListaStazioni(LatLng userLocation) {

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
                editor.apply();

            } catch (InterruptedException e) {
                Log.e(TAG, "An error occurred: " + e.getMessage(), e);
            }
        }

        dbConnection = db.getReadableDatabase();
        double km = 1.5; // raggio di default

        // prende dalla cache i filtri selezionati dall'utente
        preferences = MainActivity.getPreferences();
        String clausolaCarburante = "\'"+ preferences.getString("fuel", "Benzina%") +"\'";
        if (clausolaCarburante != null && clausolaCarburante.contains("Gasolio%")){
            clausolaCarburante = DIESEL_QUERY;
        }
        int clausolaSelf = preferences.getInt("self", 1);

        do {
            // fattori in gradi per il calcolo della distanza di 2.5km dalla userLocation
            // 1 deg latitude = 110.574km
            double lat = km/110.574;
            // 1 deg longitude = 111.320*cos(Latitude)km
            double lon = km/(111.320 * Math.abs(Math.cos(Math.toRadians(userLocation.latitude))));

            // coordinate entro cui fare la select delle stazioni dal db
            double maxLatitude = userLocation.latitude + lat;
            double minLatitude = userLocation.latitude - lat;
            double maxLongitude = userLocation.longitude + lon;
            double minLongitude = userLocation.longitude - lon;

            String query = "SELECT s.IdImpianto, s.bandiera, s.comune, s.latitudine, s.longitudine, MIN(c.prezzo) AS minPrezzo " +
                    "FROM stazioni s NATURAL JOIN carburanti c " +
                    "WHERE s.latitudine BETWEEN \'" + minLatitude + "\' AND \'" + maxLatitude +
                    "\' AND s.longitudine BETWEEN \'" + minLongitude + "\' AND \'" + maxLongitude +
                    "\' AND descCarburante LIKE " + clausolaCarburante +
                    " AND isSelf=\'" + clausolaSelf +
                    "\' GROUP BY s.IdImpianto, s.bandiera, s.comune, s.latitudine, s.longitudine";

            Cursor cursor = dbConnection.rawQuery(query, new String[]{});

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        String id = cursor.getString(0);
                        String bandiera = cursor.getString(1);
                        double latitudine = cursor.getDouble(3);
                        double longitudine = cursor.getDouble(4);
                        double prezzo = cursor.getDouble(5);
                        LatLng coordinate = new LatLng(latitudine, longitudine);

                        listaStazioni.add(new StazioneDiRifornimento(Integer.parseInt(id), bandiera, prezzo, coordinate));
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
            km += 1;
            // se la lista è vuota ripete la query fino ad un raggio massimo di 40km
        } while (listaStazioni.isEmpty() && km < 25);

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
