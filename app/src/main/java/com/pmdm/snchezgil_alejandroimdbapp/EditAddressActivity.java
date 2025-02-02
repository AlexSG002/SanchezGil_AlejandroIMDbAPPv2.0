package com.pmdm.snchezgil_alejandroimdbapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;


import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.widget.Autocomplete;


import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class EditAddressActivity extends AppCompatActivity {

    private EditText editTextDireccion;
    private ImageView imageViewMapa;
    private PlacesClient placesClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_address);

        editTextDireccion = findViewById(R.id.editTextSeleccionarDir);
        imageViewMapa = findViewById(R.id.imageViewMapa);
        Button buttonBuscar = findViewById(R.id.button2);
        Button buttonConfirmar = findViewById(R.id.button3);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyAER7D-uvYpBOG3wZjz9z3AeGulqAci-OU");
        }
        placesClient = Places.createClient(this);

        buttonBuscar.setOnClickListener(v -> abrirBuscadorDeLugares());

        buttonConfirmar.setOnClickListener(v -> {
            String direccionSeleccionada = editTextDireccion.getText().toString();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("direccionSeleccionada", direccionSeleccionada);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void abrirBuscadorDeLugares() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(this);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            editTextDireccion.setText(place.getName());

            mostrarMapaEstatico(place.getLatLng().latitude, place.getLatLng().longitude);
        }
    }

    private void mostrarMapaEstatico(double lat, double lng) {
        String url = "https://maps.googleapis.com/maps/api/staticmap?center=" + lat + "," + lng +
                "&zoom=15&size=600x300&markers=color:red%7C" + lat + "," + lng +
                "&key=AIzaSyAER7D-uvYpBOG3wZjz9z3AeGulqAci-OU";

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream in = new URL(url).openStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(in);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageViewMapa.setImageBitmap(bitmap);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
