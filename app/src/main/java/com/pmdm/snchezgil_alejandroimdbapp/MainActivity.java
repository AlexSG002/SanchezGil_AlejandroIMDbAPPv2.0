package com.pmdm.snchezgil_alejandroimdbapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.pmdm.snchezgil_alejandroimdbapp.database.IMDbDatabaseHelper;
import com.pmdm.snchezgil_alejandroimdbapp.databinding.ActivityMainBinding;
import com.pmdm.snchezgil_alejandroimdbapp.sync.FavoritesSync;
import com.pmdm.snchezgil_alejandroimdbapp.sync.UsersSync;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {
    //Declaramos variables
    private FirebaseFirestore firestore;
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private GoogleSignInClient gClient;
    private GoogleSignInOptions gOptions;
    private TextView textViewNombre;
    private TextView textViewEmail;
    private ImageView imageViewImagen;
    private ExecutorService executorService;
    private Handler mainHandler;
    private FirebaseAuth mAuth;
    private IMDbDatabaseHelper database;
    private ActivityResultLauncher<Intent> editUserLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        database = new IMDbDatabaseHelper(this);
        SQLiteDatabase db = database.getWritableDatabase();


        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        //Obtenemos de nuevo la instancia de Firebase.
        mAuth = FirebaseAuth.getInstance();
        //Obtenemos de nuevo el usuario.
        FirebaseUser usuario = mAuth.getCurrentUser();
        //Declaramos e inicializamos un executor y un Handler para ejecutar los métodos en el hilo principal.
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        //Declaramos las opciones y el cliente de google y lo inicializamos de nuevo al inicio de sesión por defecto.
        gOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gClient = GoogleSignIn.getClient(this, gOptions);
        //Con ViewBinding inflamos el layout para obtener los elementos gráficos con los que interactuará el usuario.
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);
        //Navegación entre fragmentos del drawer.
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_favoritos, R.id.nav_buscar)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        //Obtenemos una instancia del header view para poder configurar el botón de Logout.
        View headerView = navigationView.getHeaderView(0);

        //Obtenemos los detalles del header para modificar el nombre, email e imagen.
        textViewNombre = headerView.findViewById(R.id.nombre);
        textViewEmail = headerView.findViewById(R.id.email);
        imageViewImagen = headerView.findViewById(R.id.imageView);

        //Botón de logout para cerrar sesión.
        Button LogoutButton = headerView.findViewById(R.id.buttonLogout);
        LogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Generamos el logout timestamp.
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String logoutLog = sdf.format(System.currentTimeMillis());

                // Actualizamos la BD local.
                IMDbDatabaseHelper dbHelper = new IMDbDatabaseHelper(MainActivity.this);
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    dbHelper.actualizarLogoutRegistro(user.getUid(), logoutLog);

                    // También actualizamos Firestore.
                    actualizarLogoutNube(user.getUid(), logoutLog);
                }

                // Luego cerramos sesión en Firebase y en los clientes de Google/Facebook.
                mAuth.signOut();
                LoginManager.getInstance().logOut();
                gClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        volverALogin();
                    }
                });
            }
        });

        //Configuramos el EditUserLauncher donde pasamos los datos.
        configurarEditUserLauncher();
        //Declaramos e inicializamos una nueva instancia de UsersSync para vincular los usuarios de las bases de datos.
        UsersSync usersSync = new UsersSync(firestore, database);
        //Descargamos los usuarios existentes en la nube al almacenamiento local.
        usersSync.descargarUsuariosNubeALocal(new UsersSync.CloudSyncCallback() {
            @Override
            public void onSuccess() {
                SQLiteDatabase nuevoDb = database.getReadableDatabase();
                // Una vez sincronizados, comprobamos si el usuario existe en la base local.
                if (comprobarUsuarioExisteBD(nuevoDb, usuario)) {
                    cargarUsuarioDesdeBD(nuevoDb, usuario);
                } else {
                    // Si no existe, se crea la información local
                    crearInformacionUsuarioBD(nuevoDb, usuario, null, null, null);
                    cargarInformacionUsuario(usuario, accessToken);
                }
                //Actualizamos los datos de usuario para actualizar los logins y subimos la información a la nube.
                actualizarDatos(usuario);
                usersSync.subirUsuariosLocalANube();
            }
            //En caso de error no hace nada, log para verificar estado de la base de datos.
            @Override
            public void onFailure(Exception e) {
                Log.e("MainActivity", "Error sincronizando datos desde la nube: " + e.getMessage());
            }
        });
        //Declaramos e inicializamos una nueva instancia de la base de datos de las peliculas favoritas para sincronizar las bases de datos.
        FavoritesSync favoritesSync = new FavoritesSync(firestore, database);
        //Nos descargamos la base de datos de la Nube y la actualizamos a la local.
        favoritesSync.descargarFavoritosNubeALocal(new FavoritesSync.CloudSyncCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "Lista de favoritos actualizada!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("MainActivity", "Error sincronizando datos desde la nube: " + e.getMessage());

            }
        });

    }
    //Método para actualizar los datos de logs.
    private void actualizarDatos(FirebaseUser usuarioActual) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = sdf.format(System.currentTimeMillis());
        String loginLog = formattedDate;
        IMDbDatabaseHelper dbHelper = new IMDbDatabaseHelper(getApplicationContext());
        dbHelper.actualizarLoginRegistro(usuarioActual.getUid(), loginLog);
        actualizarLoginNube(usuarioActual.getUid(), loginLog);
        actualizarLogoutPendiente();
    }
    //Método para actualiazr el login de la nube.
    private void actualizarLoginNube(String idUsuario, String loginLog) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("loginRegistro", FieldValue.arrayUnion(loginLog));
        firestore.collection("usuarios").document(idUsuario)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "Login de usuario " + idUsuario + " actualizado en la nube"))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error actualizando login de usuario " + idUsuario, e));
    }
    //Método que guarda el último logout en SharedPreferences en caso de que el usuario destruya la actividad para que al hacer login se vuelva a guardar.
    private void actualizarLogoutPendiente() {
        SharedPreferences preferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String lastLogout = preferences.getString("last_logout", null);
        if (lastLogout != null) {
            IMDbDatabaseHelper dbHelper = new IMDbDatabaseHelper(getApplicationContext());
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                dbHelper.actualizarLogoutRegistro(user.getUid(), lastLogout);
                Log.d("LoginActivity", "Logout pendiente (" + lastLogout + ") guardado en la BD local para: " + user.getEmail());
            }

            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("last_logout");
            editor.apply();
        }
    }
    //Método para actualizar el logout de la nube.
    private void actualizarLogoutNube(String idUsuario, String logoutLog) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("logoutRegistro", FieldValue.arrayUnion(logoutLog));
        firestore.collection("usuarios").document(idUsuario)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "Logout de usuario " + idUsuario + " actualizado en la nube"))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error actualizando logout de usuario " + idUsuario, e));
    }

    //Método prara cargar el usuario desde la base de datos local.
    private void cargarUsuarioDesdeBD(SQLiteDatabase db, FirebaseUser usuario) {
        String sql = "SELECT * FROM t_usuarios WHERE idUsuario = ?";
        Cursor cursor = db.rawQuery(sql, new String[]{usuario.getUid()});

        if (cursor.moveToFirst()) {
            int colNombre = cursor.getColumnIndex("nombre");
            int colEmail = cursor.getColumnIndex("email");
            int colFoto = cursor.getColumnIndex("foto");

            String nombre = "";
            String email = "";
            String foto = "";

            if (colNombre != -1) {
                nombre = cursor.getString(colNombre);
            }
            if (colEmail != -1) {
                email = cursor.getString(colEmail);
            }
            if (colFoto != -1) {
                foto = cursor.getString(colFoto);
            }

            //Cargamos los datos en la barra lateral de información de usuario.
            textViewNombre.setText(nombre);
            textViewEmail.setText(email);

            if (foto != null && !foto.isEmpty()) {
                File imageFile = new File(foto);
                if (imageFile.exists()) {
                    Bitmap scaledBitmap = decodificarBitMap(imageFile.getAbsolutePath(), 300, 300);
                    imageViewImagen.setImageBitmap(scaledBitmap);
                } else {
                    Log.e("MainActivity", "Archivo de imagen no encontrado: " + foto);
                }
            } else {
                Log.d("MainActivity", "No se proporcionó una foto para el usuario.");
            }
        } else {
            Toast.makeText(this, "Usuario no encontrado en la base de datos", Toast.LENGTH_SHORT).show();
        }

        cursor.close();
    }
    //Método para decodificar el BitMap de la imagen de perfil.
    private Bitmap decodificarBitMap(String filePath, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        options.inSampleSize = reescalado(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }
    //Método para reescalar la imagen y evitar cuelgues.
    private int reescalado(BitmapFactory.Options options, int reqWidth, int reqHeight) {
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
    //Método para desencriptar los datos obtenidos de la bd.
    private String desencriptar(String texto) throws Exception {
        String secret = "MyDifficultPassw";
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decrypted = cipher.doFinal(Base64.decode(texto, Base64.DEFAULT));
        return new String(decrypted, StandardCharsets.UTF_8);
    }
    //Método para comprobar que existe el usuario en la BD local.
    private boolean comprobarUsuarioExisteBD(SQLiteDatabase db, FirebaseUser usuario) {
        if (usuario != null) {
            String sql = "SELECT idUsuario FROM t_usuarios WHERE idUsuario = ?";
            Cursor cursor = db.rawQuery(sql, new String[]{usuario.getUid()});

            boolean existe = cursor.moveToFirst();

            cursor.close();

            return existe;
        }

        return false;
    }
    //Método apra crear un usuario en la BD local.
    private void crearInformacionUsuarioBD(SQLiteDatabase db, FirebaseUser usuario, String direccion, String telefono, String imagenObtenida) {

        database.insertarUsuario(db, usuario.getUid(), usuario.getDisplayName(), usuario.getEmail(), null, null, direccion, telefono, imagenObtenida);

    }
    //Método para cargar la información del usuario en la barra lateral al iniciar sesión.
    private void cargarInformacionUsuario(FirebaseUser usuario, AccessToken accessToken) {
        //Si el usuario no es nulo obtendra los datos del usuario de Firebase.
        if (usuario != null) {
            usuario.reload().addOnCompleteListener(task -> {
                String nombreCuenta = usuario.getDisplayName();
                String emailCuenta = usuario.getEmail();
                Uri imagenCuenta = usuario.getPhotoUrl();
                if (accessToken != null) {
                    textViewNombre.setText(nombreCuenta);
                    textViewEmail.setText("Conectado con Facebook");
                    GraphRequest request = GraphRequest.newGraphPathRequest(accessToken,
                            "/me/picture",
                            new GraphRequest.Callback() {
                                @Override
                                public void onCompleted(@NonNull GraphResponse graphResponse) {
                                    try {
                                        JSONObject data = graphResponse.getJSONObject();
                                        if (data != null) {
                                            JSONObject pictureData = data.getJSONObject("data");
                                            String imageUrl = pictureData.getString("url");

                                            cargarImagen(imageUrl, imageViewImagen);
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                    Bundle parameters = new Bundle();
                    parameters.putBoolean("redirect", false);
                    parameters.putInt("height", 300);
                    parameters.putInt("width", 300);
                    request.setParameters(parameters);
                    request.executeAsync();

                } else {
                    //Establecemos los datos en los textView.
                    textViewNombre.setText(nombreCuenta);
                    textViewEmail.setText(emailCuenta);
                }
                //Comprobamos que la imagen no sea nula y utilizamos executor para obtener la imagen del usuario.
                if (imagenCuenta != null) {
                    executorService.execute(new DescargarImagen(imagenCuenta.toString(), imageViewImagen));
                }
            });
            //En caso de que el usuario sea nulo, vuelve al login.
        } else {
            volverALogin();
        }
    }

    //Método que configura el editUserLauncher para establecer los datos que debemos pasar y obtener en el intent.
    private void configurarEditUserLauncher() {
        editUserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            String nombreActualizado = data.getStringExtra("nombreActualizado");
                            String fotoActualizada = data.getStringExtra("imagenActualizada");
                            String direccionActualizada = data.getStringExtra("direccionActualizada");
                            String telefonoActualizado = data.getStringExtra("telefonoActualizado");
                            if (nombreActualizado != null) {
                                textViewNombre.setText(nombreActualizado);
                            }

                            if (fotoActualizada != null) {
                                Uri uri = Uri.parse(fotoActualizada);
                                imageViewImagen.setImageURI(uri);

                            }
                        }
                    }
                });
    }
    //Método del menú.
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_editUser) {
            irAEditarUsuario();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //Método para ir a editar usuario desde el menú.
    private void irAEditarUsuario() {
        Intent i = new Intent(MainActivity.this, EditUserActivity.class);
        String nombreCuenta = "";
        String idUsuario = mAuth.getCurrentUser().getUid();
        if (textViewNombre != null) {
            nombreCuenta = String.valueOf(textViewNombre.getText());
        }
        String emailCuenta = String.valueOf(textViewEmail.getText());
        i.putExtra("idUsuario", idUsuario);
        i.putExtra("nombre", nombreCuenta);
        i.putExtra("email", emailCuenta);

        SQLiteDatabase db = database.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT telefono, direccion, foto FROM t_usuarios WHERE idUsuario = ?", new String[]{idUsuario});
        if (cursor.moveToFirst()) {
            String telefonoEncriptado = "";
            int colTelefono = cursor.getColumnIndex("telefono");
            if (colTelefono != -1) {
                telefonoEncriptado = cursor.getString(colTelefono);
            }
            String direccionEncriptada = "";
            int colDireccion = cursor.getColumnIndex("direccion");
            if (colDireccion != -1) {
                direccionEncriptada = cursor.getString(colDireccion);
            }
            String foto = "";
            int colFoto = cursor.getColumnIndex("foto");
            if (colFoto != -1) {
                foto = cursor.getString(colFoto);
            }

            String telefonoDesencriptado = telefonoEncriptado;
            String direccionDesencriptada = direccionEncriptada;
            try {
                telefonoDesencriptado = desencriptar(telefonoEncriptado);
            } catch (Exception e) {
                Log.e("MainActivity", "Error descifrando teléfono: " + e.getMessage());
            }
            try {
                direccionDesencriptada = desencriptar(direccionEncriptada);
            } catch (Exception e) {
                Log.e("MainActivity", "Error descifrando dirección: " + e.getMessage());
            }

            i.putExtra("telefono", telefonoDesencriptado);
            i.putExtra("direccion", direccionDesencriptada);
            if (foto != null && !foto.isEmpty()) {
                i.putExtra("imagenUri", foto);
            }
        }
        cursor.close();

        editUserLauncher.launch(i);
    }

    //Método para cargar la imagen.
    private void cargarImagen(String url, ImageView imageView) {
        executorService.execute(new DescargarImagen(url, imageView));
    }

    //Método para volver al login, finaliza la activity y empieza una nueva de LoginActivity.
    private void volverALogin() {
        finish();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    //Método por defecto para finalizar el executor una vez ejecutado el método.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    //Si ya está iniciada la sesión obtiene el usuario.
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser usuarioActual = mAuth.getCurrentUser();
        if (usuarioActual == null) {
            volverALogin();
        }
    }

    //Clase a ejecutar para descargar la imagen
    private class DescargarImagen implements Runnable {
        private static final int MAX_WIDTH = 300;
        private static final int MAX_HEIGHT = 300;
        private final String url;
        private final ImageView imageView;

        //Obtenemos la url y el imageView.
        private DescargarImagen(String url, ImageView imageView) {
            this.url = url;
            this.imageView = imageView;
        }

        //Método run para ejecutar en el hilo principal
        @Override
        public void run() {
            try {
                byte[] imagenBytes = descargaImagen(url);
                //Con este método obtenemos el insample size para decodificar la imágen con el tamaño que queremos como máximo de 300x300.
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(imagenBytes, 0, imagenBytes.length, options);

                options.inSampleSize = reescalado(options, MAX_WIDTH, MAX_HEIGHT);

                options.inJustDecodeBounds = false;
                Bitmap bitmapEscalado = BitmapFactory.decodeByteArray(imagenBytes, 0, imagenBytes.length, options);

                mainHandler.post(() -> imageView.setImageBitmap(bitmapEscalado));

            } catch (IOException e) {
                mainHandler.post(() -> Toast.makeText(MainActivity.this,
                        "Error al descargar la imagen.",
                        Toast.LENGTH_SHORT).show());
            }
        }

        // Método para calcular el inSampleSize
        private int reescalado(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            // Dimensiones originales de la imagen
            final int altura = options.outHeight;
            final int anchura = options.outWidth;
            int inSampleSize = 1;

            if (altura > reqHeight || anchura > reqWidth) {
                final int mitadAltura = altura / 2;
                final int mitadAnchura = anchura / 2;

                // Calcular el inSampleSize más grande que sea potencia de 2 y mantenga ambas dimensiones
                // mayores que las requeridas
                while ((mitadAltura / inSampleSize) >= reqHeight
                        && (mitadAnchura / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }

            return inSampleSize;
        }

        //Método que conecta a la url y obtiene la imagen igual que en la práctica de executor.
        private byte[] descargaImagen(String myurl) throws IOException {
            InputStream is = null;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try {
                URL url = new URL(myurl);
                if (url.getProtocol().equals("file")) {
                    is = new FileInputStream(url.getPath());
                } else {
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(15000);
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);

                    conn.connect();
                    int response = conn.getResponseCode();
                    if (response != HttpURLConnection.HTTP_OK) {
                        throw new IOException("Error en la conexión: " + response);
                    }
                    is = conn.getInputStream();
                    byte[] datos = new byte[1024];
                    int nRead;

                    while ((nRead = is.read(datos, 0, datos.length)) != -1) {
                        buffer.write(datos, 0, nRead);
                    }
                }
                return buffer.toByteArray();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
