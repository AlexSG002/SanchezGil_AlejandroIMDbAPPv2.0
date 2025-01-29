package com.pmdm.snchezgil_alejandroimdbapp;

import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.pmdm.snchezgil_alejandroimdbapp.databinding.ActivityMainBinding;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    //Declaramos variables
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private GoogleSignInClient gClient;
    private GoogleSignInOptions gOptions;
    private TextView nombre;
    private TextView email;
    private ImageView imagen;
    private ExecutorService executorService;
    private Handler mainHandler;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        //Profile p = Profile.getCurrentProfile();
        //p.getFirstName();
        //p.getLastName();
        //p.getProfilePictureUri(300,300);
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

        //Botón de logout para cerrar sesión.
        Button LogoutButton = headerView.findViewById(R.id.buttonLogout);
        LogoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Cerramos sesión en firebase y en el cliente de google.
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
        if(usuario!=null && accessToken!=null) {
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

                                    cargarImagen(imageUrl, imagen);
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
        }
        //Obtenemos los detalles del header para modificar el nombre, email e imagen.
        nombre = headerView.findViewById(R.id.nombre);
        email = headerView.findViewById(R.id.email);
        imagen = headerView.findViewById(R.id.imageView);
        //Si el usuario no es nulo obtendra los datos del usuario de Firebase.
        if(usuario != null) {
            String nombreCuenta = usuario.getDisplayName();
            String emailCuenta = usuario.getEmail();
            Uri imagenCuenta = usuario.getPhotoUrl();
            if(accessToken!=null){
                nombre.setText(nombreCuenta);
                email.setText("Conectado con Facebook");
            }else {
                //Establecemos los datos en los textView.
                nombre.setText(nombreCuenta);
                email.setText(emailCuenta);
            }
            //Comprobamos que la imagen no sea nula y utilizamos executor para obtener la imagen del usuario.
            if(imagenCuenta!=null){
                executorService.execute(new DescargarImagen(imagenCuenta.toString(), imagen));
            }
            //En caso de que el usuario sea nulo, vuelve al login.
        }else{
            volverALogin();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_editUser){
            irAEditarUsuario();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void irAEditarUsuario() {
        Intent i = new Intent(MainActivity.this, EditUserActivity.class);
        String nombreCuenta = String.valueOf(nombre.getText());
        String emailCuenta = String.valueOf(email.getText());
        i.putExtra("nombre",nombreCuenta);
        i.putExtra("email", emailCuenta);
        startActivity(i);
    }


    private void cargarImagen(String url, ImageView imageView) {
        executorService.execute(new DescargarImagen(url, imageView));
    }

    //Método para volver al login, finaliza la activity y empieza una nueva de LoginActivity.
    private void volverALogin(){
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
    //Clase a ejecutar para descargar la imagen
    private class DescargarImagen implements Runnable {
        private final String url;
        private final ImageView imageView;
        private static final int MAX_WIDTH = 300;
        private static final int MAX_HEIGHT = 300;
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

                options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);

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
        private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
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
        if(usuarioActual == null){
            volverALogin();
        }
    }
}
