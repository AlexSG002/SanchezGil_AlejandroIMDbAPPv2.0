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
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class EditAddressActivity extends AppCompatActivity {
    //Declaramos variables.
    private EditText editTextDireccion;
    private ImageView imageViewMapa;
    private PlacesClient placesClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_address);
        //Inicializamos variables de elementos gráficos y botones.
        editTextDireccion = findViewById(R.id.editTextSeleccionarDir);
        imageViewMapa = findViewById(R.id.imageViewMapa);
        Button buttonBuscar = findViewById(R.id.button2);
        Button buttonConfirmar = findViewById(R.id.button3);
        //Comprobamos si la herramienta Places está inicializada, si no la inicializamos con la Key
        //que nos han facilitado.
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyAER7D-uvYpBOG3wZjz9z3AeGulqAci-OU");
        }
        //Creamos un nuevo cliente de la herramienta Places.
        placesClient = Places.createClient(this);
        //Al hacer clic en el botón de buscar abrimos el Buscador de Lugares que es la barra de buscar localización.
        buttonBuscar.setOnClickListener(v -> abrirBuscadorDeLugares());
        //Al confirmar obtenemos la dirección del editText y la devolvemos a EditUser a través del intent.
        buttonConfirmar.setOnClickListener(v -> {
            String direccionSeleccionada = editTextDireccion.getText().toString();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("direccionSeleccionada", direccionSeleccionada);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
    //Abrimos el buscador de lugares con una lista de campos de la herramienta Places que nos obtiene los
    //"Places" y nos obtiene los campos de las localizaciones, es decir su id, su nombre etc.
    private void abrirBuscadorDeLugares() {
        //Y utilizando la herramienta de la librería Autocomplete obtenemos las localizaciones exactas.
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(this);
        //Se lo pasamos como un intent para establecerlo en el campo de dirección.
        startActivityForResult(intent, 1);
    }
    //Obtenemos el dato del intent y con la latitud y longitud obtenidas mostramos el mapa estático
    //de la dirección seleccionada.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            editTextDireccion.setText(place.getName());
            mostrarMapaEstatico(place.getLatLng().latitude, place.getLatLng().longitude);
        }
    }
    //Método para mostrar el mapa estático que simplemente obtiene del enlace la imagen según la latitud y longitud.
    private void mostrarMapaEstatico(double lat, double lng) {
        String url = "https://maps.googleapis.com/maps/api/staticmap?center=" + lat + "," + lng +
                "&zoom=15&size=600x300&markers=color:red%7C" + lat + "," + lng +
                "&key=AIzaSyAER7D-uvYpBOG3wZjz9z3AeGulqAci-OU";
        //Método para descargar la imagen en un hilo ya que es una tarea asíncrona y establecerlo en el imageView.
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
