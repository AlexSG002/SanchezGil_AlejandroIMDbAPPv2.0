package com.pmdm.snchezgil_alejandroimdbapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.pmdm.snchezgil_alejandroimdbapp.models.Movie;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MovieDetailsActivity extends AppCompatActivity {
    //Declaramos la variables que vamos a utilizar.
    private ImageView imagePosterLarge;
    private TextView textTitle, textRank, textPlot, textDate, textRating;
    private ExecutorService executorService;
    private Button buttonEnviar;
    private static final int CODIGO_PERMISO_LEER_CONTACTOS = 1;
    private static final int CODIGO_PERMISO_ENVIAR_SMS = 2;
    private ActivityResultLauncher<Intent> launcherSeleccionarContacto;

    private String numeroSMSPendiente;
    private String textoSMSPendiente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);
        //Inicializamos las variables de los textos, la imagen y el botón de enviar a los elementos gráficos de la Activity.
        imagePosterLarge = findViewById(R.id.imagePosterLarge);
        textTitle = findViewById(R.id.textTitle);
        textRank = findViewById(R.id.textRank);
        textPlot = findViewById(R.id.textPlot);
        textDate = findViewById(R.id.textDate);
        textRating = findViewById(R.id.textRating);
        buttonEnviar = findViewById(R.id.buttonEnviarSMS);
        //Launcher de intent para que aparezca la lista de contactos.
        launcherSeleccionarContacto = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri contactUri = result.getData().getData();
                        if (contactUri != null) {
                            String idContacto = obtenerIdContacto(contactUri);
                            if (idContacto != null) {
                                String numTelefono = obtenerTelefono(idContacto);
                                if (numTelefono != null && !numTelefono.isEmpty()) {
                                    String textoSMS = "¡Te recomiendo la película: " + textTitle.getText().toString() + "!"+" Con "+textRating.getText().toString();
                                    enviarSMS(numTelefono, textoSMS);
                                } else {
                                    Toast.makeText(this, "El contacto no tiene número de teléfono.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                }
        );
        //Funcionalidad de botón enviar
        buttonEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Comprobamos si tenemos permisos de contactos, en caso de no tenerlos los solicita.
                if (ContextCompat.checkSelfPermission(MovieDetailsActivity.this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MovieDetailsActivity.this, new String[]{android.Manifest.permission.READ_CONTACTS}, CODIGO_PERMISO_LEER_CONTACTOS);
                }else{
                    lanzarSelectorContactos();
                }

            }
        });
        //Declaramos un executor para la imagen de la pelicula
        executorService = Executors.newSingleThreadExecutor();
        //Obtenemos los datos del parcelable
        Movie movie = getIntent().getParcelableExtra("MOVIE");
        //Comprobamos que la película y el título no sean nulos.
        if (movie != null) {
            if (movie.getTitle() != null) {
                textTitle.setText(movie.getTitle());
            } else {
                textTitle.setText("Sin título");
            }
            //Establecemos los datos obtenidos del parcelable.
            textRank.setText("Rank: " + movie.getRank());
            textPlot.setText("Descripción: "+movie.getDescripcion());
            textDate.setText("Fecha de lanzamiento: "+movie.getFecha());
            textRating.setText("Rating: "+movie.getRating());
            //Obtenemos la caratula.
            executorService.execute(() -> {
                try {
                    URL url = new URL(movie.getImageUrl());
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    runOnUiThread(() -> imagePosterLarge.setImageBitmap(bitmap));
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> imagePosterLarge.setImageResource(R.drawable.ic_launcher_foreground));
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    //Método que se ejecuta al obtener el resultado de solicitar los contactos
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //Si se han otorgado los contactos lanzamos el selector de contactos, en caso contrario nos lanza un toast.
        if (requestCode == CODIGO_PERMISO_LEER_CONTACTOS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                lanzarSelectorContactos();
            } else {
                Toast.makeText(this, "Permiso de lectura de contactos denegado.", Toast.LENGTH_SHORT).show();
            }
        }
        //Lo miosmo pero para enviar sms.
        if (requestCode == CODIGO_PERMISO_ENVIAR_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirAppSMS(numeroSMSPendiente, textoSMSPendiente);
            } else {
                Toast.makeText(this, "Permiso para enviar SMS denegado.", Toast.LENGTH_SHORT).show();
            }
        }

    }
    //Lanzamos el intent de seleccionar contactos con Content_URI y el launcher que hemos definido antes.
    private void lanzarSelectorContactos() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        launcherSeleccionarContacto.launch(intent);
    }
    //Obtenemos el id del contacto como en la práctica de SMSToContact.
    private String obtenerIdContacto(Uri contactUri) {
        String idContacto = null;
        Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
            idContacto = cursor.getString(idIndex);
        }
        if (cursor != null) {
            cursor.close();
        }
        return idContacto;
    }
    //Lo mismo para el teléfono.
    private String obtenerTelefono(String contactId) {
        String numTelefono = null;
        Cursor cursorTelefono = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{contactId},
                null
        );
        if (cursorTelefono != null && cursorTelefono.moveToFirst()) {
            int numberIndex = cursorTelefono.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            numTelefono = cursorTelefono.getString(numberIndex);
        }
        if (cursorTelefono != null) {
            cursorTelefono.close();
        }
        return numTelefono;
    }
    //Método para enviar el sms con el número de teléfono y el texto.
    private void enviarSMS(String numero, String texto) {
        numeroSMSPendiente = numero;
        textoSMSPendiente = texto;
        //Comprobnamos que hemos solicitado los permisos de sms.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.SEND_SMS},
                    CODIGO_PERMISO_ENVIAR_SMS
            );
        } else {
            abrirAppSMS(numero, texto);
        }
    }
    //Método para abrir la app de sms con el intent de ACTION_SENDTO.
    private void abrirAppSMS(String numero, String texto) {
        if (numero == null || texto == null || numero.isEmpty() || texto.isEmpty()) {
            Toast.makeText(this, "No se tiene número o texto para enviar.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        //Establecemos los datos para abrir el contacto y mostrar el mensaje.
        smsIntent.setData(Uri.parse("smsto:" + numero));
        smsIntent.putExtra("sms_body", texto);
        startActivity(smsIntent);
    }
}


