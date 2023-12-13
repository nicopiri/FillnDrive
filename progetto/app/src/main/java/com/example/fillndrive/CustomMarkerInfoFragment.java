package com.example.fillndrive;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class CustomMarkerInfoFragment extends BottomSheetDialogFragment {

    private TextView titleTextView;
    private TextView snippetTextView;
    private TextView infoTextView;
    private Button startRouteButton;
    private GoogleMap googleMap;
    private LatLng destinationCoordinates;

    public static CustomMarkerInfoFragment newInstance(String title, String snippet, String info, LatLng coordinates) {
        CustomMarkerInfoFragment fragment = new CustomMarkerInfoFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("snippet", snippet); 
        args.putString("info", info);
        args.putParcelable("coordinates", coordinates);
        fragment.setArguments(args);
        return fragment;
    }

    public void setGoogleMap(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.custom_marker_info, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        titleTextView = view.findViewById(R.id.titleTextView);
        snippetTextView = view.findViewById(R.id.snippetTextView);
        infoTextView = view.findViewById(R.id.infoTextView);
        startRouteButton = view.findViewById(R.id.startRouteButton);

        Bundle args = getArguments();
        if (args != null) {
            destinationCoordinates = args.getParcelable("coordinates");
            titleTextView.setText(args.getString("title"));
            snippetTextView.setText(args.getString("snippet"));
            infoTextView.setText(args.getString("info"));
        }

        // Chiude il fragment e cancella il percorso quando si clicca sulla mappa
        googleMap.setOnMapClickListener(latLng -> {
            if(isVisible()){
                MapsActivity.currentPolyline.remove();
//                MapsActivity.route = null;
                dismiss();
            }
        });
        Button closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(view1 -> {
            MapsActivity.currentPolyline.remove();
//            MapsActivity.route = null;
            dismiss();
        }); // Chiude il fragment quando il pulsante è premuto

        startRouteButton.setOnClickListener(v -> startRoute());
    }


    private void startRoute() {
        dismiss(); // Chiude il fragment
        if (destinationCoordinates != null) {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destinationCoordinates.latitude + "," + destinationCoordinates.longitude);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Gestisci il caso in cui l'app di Google Maps non sia installata
            }
        }
    }

}
