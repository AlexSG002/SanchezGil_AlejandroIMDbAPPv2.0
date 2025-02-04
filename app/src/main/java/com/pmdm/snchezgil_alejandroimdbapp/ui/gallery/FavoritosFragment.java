package com.pmdm.snchezgil_alejandroimdbapp.ui.gallery;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pmdm.snchezgil_alejandroimdbapp.adapter.MovieAdapter;
import com.pmdm.snchezgil_alejandroimdbapp.database.IMDbDatabaseHelper;
import com.pmdm.snchezgil_alejandroimdbapp.databinding.FragmentFavoritosBinding;
import com.pmdm.snchezgil_alejandroimdbapp.models.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoritosFragment extends Fragment {
    private static final int CODIGO_PERMISOS_BLUETOOTH = 1;
    //Declaramos variables
    private FragmentFavoritosBinding binding;
    private ExecutorService executorService;
    private Handler mainHandler;
    private final boolean favoritos = true;
    private final String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private Button buttonCompartir;
    private final List<Movie> pelisFavoritas = new ArrayList<>();
    private IMDbDatabaseHelper database;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFavoritosBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        binding.recyclerViewFavoritos.setLayoutManager(layoutManager);
        //Utilizamos ViewBinding para localizar los elementos gráficos
        buttonCompartir = binding.buttonCompartir;
        //Se ejecutará un método para solicitar los permisos y compartir el json.
        buttonCompartir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                solicitarPermisosBluetooth();
            }
        });
        //Recuperamos la base de datos para cargar las peliculas
        database = new IMDbDatabaseHelper(requireContext());

        cargarFavoritosDesdeBD();

        return root;
    }

    //Método para cargar las peliculas en favoritos desde la base de datos.
    private void cargarFavoritosDesdeBD() {
        executorService.execute(() -> {
            SQLiteDatabase db = database.getReadableDatabase();
            //Con un cursor creamos una consulta que iremos recorriendo, la limité a 10 porque limité los favoritos a 10.
            Cursor cursor = db.rawQuery(
                    "SELECT * FROM " + IMDbDatabaseHelper.TABLE_FAVORITOS + " WHERE idUsuario=? LIMIT 10",
                    new String[]{idUsuario}
            );
            //Limpiamos la lista cada vez que se carguen nuevos favoritos.
            pelisFavoritas.clear();
            //Comprobamos que el resultado del cursor no sea nulo.
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    //Rescatamos los datos.
                    int colIdPelicula = cursor.getColumnIndex("idPelicula");
                    int colTitulo = cursor.getColumnIndex("nombrePelicula");
                    int colDescripcion = cursor.getColumnIndex("descripcionPelicula");
                    int colFecha = cursor.getColumnIndex("fechaLanzamiento");
                    int colRanking = cursor.getColumnIndex("rankingPelicula");
                    int colRating = cursor.getColumnIndex("ratingPelicula");
                    int colCaratula = cursor.getColumnIndex("caratulaURL");
                    String idPelicula = cursor.getString(colIdPelicula);
                    String titulo = cursor.getString(colTitulo);
                    String descripcion = cursor.getString(colDescripcion);
                    String fecha = cursor.getString(colFecha);
                    String ranking = cursor.getString(colRanking);
                    String rating = cursor.getString(colRating);
                    String caratula = cursor.getString(colCaratula);
                    //Creamos un nuevo objeto película y lo agregamos a una lista.
                    Movie movie = new Movie();
                    movie.setId(String.valueOf(idPelicula));
                    movie.setTitle(titulo);
                    movie.setDescripcion(descripcion);
                    movie.setFecha(fecha);
                    movie.setRank(ranking);
                    movie.setRating(rating);
                    movie.setImageUrl(caratula);
                    movie.setCargada(true);
                    pelisFavoritas.add(movie);
                } while (cursor.moveToNext());
            }
            //Si el cursor no es nulo lo cerramos.
            if (cursor != null) {
                cursor.close();
            }
            //Cerramos la base de datos.
            db.close();
            //En el hilo principal con mainHandler inicializamos una nueva instancia del MovieAdapter
            mainHandler.post(() -> {
                if (!pelisFavoritas.isEmpty()) {
                    MovieAdapter adapter = new MovieAdapter(getContext(), pelisFavoritas, idUsuario, database, favoritos);
                    binding.recyclerViewFavoritos.setAdapter(adapter);
                } else {
                    Toast.makeText(getContext(), "No tienes películas favoritas guardadas", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executorService.shutdown();
        binding = null;
    }

    //Método para solicitar los permisos de bluetooth, ya que dependiendo de la api es diferente,
    //Comprueba la api en la que estamos para solicitar unos permisos u otros.
    private void solicitarPermisosBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, CODIGO_PERMISOS_BLUETOOTH);
            } else {
                activarBluetoothYCompartir();
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, CODIGO_PERMISOS_BLUETOOTH);
            } else {
                activarBluetoothYCompartir();
            }
        }
    }

    //Método que se ejecuta al recibir la respuesta de si se han concedido o no los permisos.
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODIGO_PERMISOS_BLUETOOTH) {
            boolean permisosConcedidos = true;
            for (int resultado : grantResults) {
                if (resultado != PackageManager.PERMISSION_GRANTED) {
                    permisosConcedidos = false;
                    break;
                }
            }

            if (permisosConcedidos) {
                activarBluetoothYCompartir();
            } else {
                Toast.makeText(getContext(), "Permisos de Bluetooth necesarios para compartir.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Método para activar el bluetooth utilizando bluetooth adapter en caso de tener el bluetooth desactivado.
    private void activarBluetoothYCompartir() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), "El dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, CODIGO_PERMISOS_BLUETOOTH);
            Toast.makeText(getContext(), "Solicitando activación de Bluetooth...", Toast.LENGTH_SHORT).show();
        } else {
            compartirFavoritos();
        }
    }

    //Método para convertir las favoritas a un texto json y mostrarlo en un AlertDialog al usuario.
    private void compartirFavoritos() {
        if (pelisFavoritas.isEmpty()) {
            Toast.makeText(getContext(), "No hay peliculas guardadas en favoritos para compartir.", Toast.LENGTH_SHORT).show();
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonFavoritos = gson.toJson(pelisFavoritas);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Películas Favoritas en JSON");

        final ScrollView scrollView = new ScrollView(requireContext());
        final TextView textView = new TextView(requireContext());
        textView.setText(jsonFavoritos);
        textView.setPadding(16, 16, 16, 16);
        scrollView.addView(textView);
        builder.setView(scrollView);

        builder.setPositiveButton("Cerrar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });


        AlertDialog dialog = builder.create();
        dialog.show();

    }


}
