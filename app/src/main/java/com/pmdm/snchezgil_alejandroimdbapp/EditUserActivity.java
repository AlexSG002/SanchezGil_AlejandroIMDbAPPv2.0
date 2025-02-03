package com.pmdm.snchezgil_alejandroimdbapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;
import com.pmdm.snchezgil_alejandroimdbapp.database.IMDbDatabaseHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import android.util.Base64;

public class EditUserActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> camaraLauncher;
    private ActivityResultLauncher<Intent> galeriaLauncher;
    private ActivityResultLauncher<String[]> obtenerPermisosLauncher;
    private ActivityResultLauncher<Intent> direccionLauncher;
    private ActivityResultLauncher<String[]> obtenerPermisosLocalizacionLauncher;
    private Uri imageUri;
    private ImageView imagen;
    private String nuevoNombre;
    private CountryCodePicker ccp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);

        EditText editTextNombre = findViewById(R.id.editTextNombre);
        EditText editTextTelefono = findViewById(R.id.editTextTlf);
        EditText editTextEmail = findViewById(R.id.editTextEmailEditar);
        EditText editTextDireccion = findViewById(R.id.editTextDir);
        EditText editTextURL = findViewById(R.id.editTextURL);
        ccp = findViewById(R.id.countryCodePicker);


        imagen = findViewById(R.id.imageView3);

        configurarActivityResultLaunchers();


        if (getIntent().hasExtra("nombre")) {
            editTextNombre.setText(getIntent().getStringExtra("nombre"));
        }
        if (getIntent().hasExtra("email")) {
            editTextEmail.setText(getIntent().getStringExtra("email"));
        }
        if (getIntent().hasExtra("telefono")) {
            editTextTelefono.setText(getIntent().getStringExtra("telefono"));
        }
        if (getIntent().hasExtra("direccion")) {
            editTextDireccion.setText(getIntent().getStringExtra("direccion"));
        }

        if (getIntent().hasExtra("imagenUri")) {
            String fotoPath = getIntent().getStringExtra("imagenUri");
            if (fotoPath != null && !fotoPath.isEmpty()) {
                Uri uri = Uri.fromFile(new File(fotoPath));
                Bitmap bitmap = decodeSampledBitmapFromFile(fotoPath, 300, 300);
                imagen.setImageBitmap(bitmap);
            }
        }


        obtenerPermisosLocalizacionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean permisosConcedidos = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                            result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if (permisosConcedidos) {
                        abrirEditarDireccion();
                    } else {
                        Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                    }
                });

        direccionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String direccion = result.getData().getStringExtra("direccionSeleccionada");
                        editTextDireccion.setText(direccion);
                    }
                });

        Button buttonImagen = findViewById(R.id.buttonImagen);
        Button buttonGuardar = findViewById(R.id.buttonGuardar);
        Button buttonDir = findViewById(R.id.buttonDireccion);
        Button buttonImagenURL = findViewById(R.id.buttonImagen2);

        buttonImagenURL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String urlString = editTextURL.getText().toString().trim();
                if (!urlString.toLowerCase().endsWith(".jpg") &&
                        !urlString.toLowerCase().endsWith(".jpeg") &&
                        !urlString.toLowerCase().endsWith(".png")) {
                    Toast.makeText(EditUserActivity.this,
                            "La URL debe terminar en .jpg, .jpeg o .png",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                new Thread(() -> {
                    try {
                        URL url = new URL(urlString);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(input);
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
                        String localPath = guardarBitMapLocalmente(scaledBitmap);
                        imageUri = Uri.fromFile(new File(localPath));
                        runOnUiThread(() -> {
                            imagen.setImageBitmap(scaledBitmap);
                            Toast.makeText(EditUserActivity.this,
                                    "Imagen cargada correctamente",
                                    Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(EditUserActivity.this,
                                    "Error al cargar la imagen desde URL",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            }
        });

        buttonDir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tienePermisosUbicacion()) {
                    abrirEditarDireccion();
                } else {
                    solicitarPermisosUbicacion();
                }
            }
        });

        buttonImagen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seleccionarImagen();
            }
        });

        buttonGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String telefono = String.valueOf(editTextTelefono.getText());
                String prefijo = ccp.getSelectedCountryCodeWithPlus();
                String telefonoCompleto = prefijo + telefono;
                nuevoNombre = editTextNombre.getText().toString().trim();
                if (nuevoNombre.isEmpty()) {
                    Toast.makeText(EditUserActivity.this, "El nombre no puede estar vacío.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!telefonoValido(telefonoCompleto)) {
                    Toast.makeText(EditUserActivity.this, "Número de teléfono inválido.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent resultIntent = new Intent();
                resultIntent.putExtra("nombreActualizado", nuevoNombre);
                if (imageUri != null) {
                    resultIntent.putExtra("imagenActualizada", imageUri.toString());
                }
                setResult(RESULT_OK, resultIntent);

                modificarUsuario(telefonoCompleto);
                
                finish();
            }
        });

    }

    private boolean tienePermisosUbicacion() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void solicitarPermisosUbicacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            obtenerPermisosLocalizacionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            obtenerPermisosLocalizacionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
        }
    }

    private void abrirEditarDireccion() {
        Intent intent = new Intent(EditUserActivity.this, EditAddressActivity.class);
        direccionLauncher.launch(intent);
    }

    private boolean telefonoValido(String telefonoCompleto) {
        if (telefonoCompleto == null || telefonoCompleto.trim().isEmpty()) {
            Log.d("EditUserActivity", "Teléfono vacío o nulo.");
            return false;
        }

        boolean valido = telefonoCompleto.matches("^\\+[1-9]\\d{1,14}$") && telefonoCompleto.length() > ccp.getSelectedCountryCodeWithPlus().length();
        Log.d("EditUserActivity", "Teléfono: " + telefonoCompleto + " - Válido: " + valido);
        return valido;
    }

    private String guardarBitMapLocalmente(Bitmap bitmap) throws IOException {
        String fileName = "user_image_" + System.currentTimeMillis() + ".jpg";
        File localFile = new File(getFilesDir(), fileName);
        FileOutputStream out = new FileOutputStream(localFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        out.flush();
        out.close();
        Log.d("EditUserActivity", "Imagen guardada localmente en: " + localFile.getAbsolutePath());
        return localFile.getAbsolutePath();
    }

    private String guardarImagenDeUriLocalmente(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            Log.e("EditUserActivity", "No se pudo abrir el InputStream de la URI: " + uri);
            return null;
        }

        String fileName = "user_image_" + System.currentTimeMillis() + ".jpg";
        File localFile = new File(getFilesDir(), fileName);

        try (FileOutputStream outputStream = new FileOutputStream(localFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        inputStream.close();
        Log.d("EditUserActivity", "Imagen guardada localmente en: " + localFile.getAbsolutePath());
        return localFile.getAbsolutePath();
    }


    private Bitmap decodeSampledBitmapFromFile(String filePath, int reqWidth, int reqHeight) {
        // Primero, decodifica solo las dimensiones
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calcula el factor de escalado
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decodifica el bitmap ya escalado
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }


    private void modificarUsuario(String telefonoCompleto) {
        IMDbDatabaseHelper database = new IMDbDatabaseHelper(this);
        SQLiteDatabase db = database.getWritableDatabase();
        String idUsuario = getIntent().getStringExtra("idUsuario");
        EditText editTextDireccion = findViewById(R.id.editTextDir);
        String direccion = String.valueOf(editTextDireccion.getText());
        ContentValues valores = new ContentValues();
        valores.put("nombre", nuevoNombre);
        try {
            // Cifrado de teléfono y dirección
            String encryptedTelefono = encriptar(telefonoCompleto);
            String encryptedDireccion = encriptar(direccion);
            valores.put("telefono", encryptedTelefono);
            valores.put("direccion", encryptedDireccion);
        } catch (Exception e) {
            Log.e("EditUserActivity", "Error cifrando datos: " + e.getMessage());
            // Si ocurre un error, se pueden almacenar sin cifrar o abortar la operación
            valores.put("telefono", telefonoCompleto);
            valores.put("direccion", direccion);
        }

        if (imageUri != null) {
            try {
                String localPath = guardarImagenDeUriLocalmente(imageUri);
                if (localPath != null) {
                    valores.put("foto", localPath);
                } else {
                    Log.e("EditUserActivity", "Error al guardar la imagen localmente.");
                }
            } catch (IOException e) {
                Log.e("EditUserActivity", "Error al guardar la imagen: " + e.getMessage(), e);
            }
        }

        int filasActualizadas = db.update("t_usuarios", valores, "idUsuario = ?", new String[]{idUsuario});

        if (filasActualizadas > 0) {
            Toast.makeText(this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show();
            actualizarUsuariosNube(idUsuario, valores);
        } else {
            Toast.makeText(this, "Error al actualizar los datos", Toast.LENGTH_SHORT).show();
        }

        db.close();
    }

    private void actualizarUsuariosNube(String idUsuario, ContentValues valores) {
        Map<String, Object> data = new HashMap<>();
        if (valores.containsKey("nombre")) {
            data.put("nombre", valores.getAsString("nombre"));
        }
        if (valores.containsKey("telefono")) {
            data.put("telefono", valores.getAsString("telefono"));
        }
        if (valores.containsKey("direccion")) {
            data.put("direccion", valores.getAsString("direccion"));
        }
        if (valores.containsKey("foto")) {
            data.put("foto", valores.getAsString("foto"));
        }


        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("usuarios").document(idUsuario)
                .update(data)
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "Usuario " + idUsuario + " actualizado en la nube"))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error actualizando usuario " + idUsuario, e));
    }

    private String encriptar(String texto) throws Exception {
        String secret = "MyDifficultPassw";
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(texto.getBytes("UTF-8"));
        return Base64.encodeToString(encrypted, Base64.DEFAULT);
    }

    private String desencriptar(String texto) throws Exception {
        String secret = "MyDifficultPassw";
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decrypted = cipher.doFinal(Base64.decode(texto, Base64.DEFAULT));
        return new String(decrypted, "UTF-8");
    }


    private void configurarActivityResultLaunchers() {
        // Launcher para capturar imagen con la cámara
        camaraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            if (imageUri != null) {
                                try {
                                    // Usar ContentResolver para obtener el InputStream desde la URI
                                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                                    if (inputStream != null) {
                                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
                                        imagen.setImageBitmap(scaledBitmap);
                                    } else {
                                        Toast.makeText(EditUserActivity.this, "No se pudo abrir la imagen.", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Toast.makeText(EditUserActivity.this, "Error al acceder al archivo de la imagen.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(EditUserActivity.this, "Error: imageUri es nulo", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });


        galeriaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            imageUri = result.getData().getData();
                            imagen.setImageURI(imageUri);
                        }
                    }
                });

        obtenerPermisosLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean permisosCamara = result.getOrDefault(Manifest.permission.CAMERA, false);
                    Boolean permisosAlmacenamiento;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permisosAlmacenamiento = result.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false);
                    } else {
                        permisosAlmacenamiento = result.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false);
                    }
                    if (permisosCamara && permisosAlmacenamiento) {
                        mostrarOpcionesImagen();
                    } else {
                        Toast.makeText(this, "Permisos denegados. No se puede seleccionar la imagen.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void seleccionarImagen() {
        if (comprobarPermisos()) {
            mostrarOpcionesImagen();
        } else {
            solicitarPermisos();
        }
    }

    private boolean comprobarPermisos() {
        boolean permisosCamara = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean permisosAlmacenamiento;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisosAlmacenamiento = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            permisosAlmacenamiento = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }

        return permisosCamara && permisosAlmacenamiento;
    }

    private void solicitarPermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            obtenerPermisosLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
            });
        } else {
            obtenerPermisosLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            });
        }
    }

    private void mostrarOpcionesImagen() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar Imagen")
                .setItems(new CharSequence[]{"Cámara", "Galería"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            abrirCamara();
                        } else if (which == 1) {
                            abrirGaleria();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void abrirCamara() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                File fotoArchivo = crearArchivoImagen();
                if (fotoArchivo != null) {
                    Log.d("EditUserActivity", "Ruta de la imagen: " + fotoArchivo.getAbsolutePath());
                    imageUri = FileProvider.getUriForFile(
                            this,
                            getApplicationContext().getPackageName() + ".provider",
                            fotoArchivo);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    camaraLauncher.launch(intent);
                } else {
                    Toast.makeText(this, "Error al crear el archivo de imagen.", Toast.LENGTH_SHORT).show();
                }

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al crear el archivo de imagen.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No hay una aplicación de cámara disponible.", Toast.LENGTH_SHORT).show();
        }
    }

    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String nombreArchivo = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir("Pictures");
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        return File.createTempFile(nombreArchivo, ".jpg", storageDir);
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galeriaLauncher.launch(intent);
    }
}
