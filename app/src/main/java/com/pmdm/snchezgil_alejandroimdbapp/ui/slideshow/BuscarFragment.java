package com.pmdm.snchezgil_alejandroimdbapp.ui.slideshow;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pmdm.snchezgil_alejandroimdbapp.R;
import com.pmdm.snchezgil_alejandroimdbapp.databinding.FragmentBuscarBinding;
import com.pmdm.snchezgil_alejandroimdbapp.models.Genero;
import com.pmdm.snchezgil_alejandroimdbapp.models.Movie;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BuscarFragment extends Fragment {
    //Declaramos e inicializamos las variables necesarias.
    private static final String BASE_URL = "https://api.themoviedb.org/3/";
    private static final String ENDPOINT_GENEROS = "genre/movie/list?language=en";
    private static final String AUTHORIZATION = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJjZTU5ODExZDEzZWViNjQzYWUxMzg5ZTM2MGExMDNkZCIsIm5iZiI6MTczNjUwMjMyMy4yMTQwMDAyLCJzdWIiOiI2NzgwZWMzMzE0MzFlMDU5MWFiYjJmYzQiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.ykP4qXAyJ40Io_luvUthjnYOawrFEMu-cMULOzsTwoQ";
    private static final String ACCEPT = "application/json";
    private static final String ENDPOINT_PELIS = "/discover/movie?include_adult=false&include_video=false&language=en-US&page=1&primary_release_year=";
    private static final String urlGenero = "&with_genres=";
    private static final String urlSortDesc = "&sort_by=popularity.desc";
    private static final List<Genero> generos = new ArrayList<Genero>();
    private FragmentBuscarBinding binding;
    private ExecutorService executorService;
    private Handler mainHandler;
    private EditText year;
    private Button buttonBuscar;
    private Spinner spinnerGeneros;

    //Método para obtener de la respuesta las películas, lo que devuelve el array de películas filtradas.
    private static @NonNull List<Movie> getMovies(String respuesta) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(respuesta, JsonObject.class);
        JsonArray resultsArray = jsonObject.getAsJsonArray("results");

        List<Movie> peliculasFiltradas = new ArrayList<>();
        //Obtenemos los datos del json.
        for (JsonElement resultsElement : resultsArray) {
            JsonObject jsonResultado = resultsElement.getAsJsonObject();
            String title = "Sin título";
            if (jsonResultado.has("title") && !jsonResultado.get("title").isJsonNull()) {
                title = jsonResultado.get("title").getAsString();
            }
            String id = "Desconocido";
            if (jsonResultado.has("id") && !jsonResultado.get("id").isJsonNull()) {
                id = jsonResultado.get("id").getAsString();
            }
            String overview = "Descripción no disponible";
            if (jsonResultado.has("overview") && !jsonResultado.get("overview").isJsonNull()) {
                overview = jsonResultado.get("overview").getAsString();
            }
            String vote_average = "0.0";
            if (jsonResultado.has("vote_average") && !jsonResultado.get("vote_average").isJsonNull()) {
                vote_average = jsonResultado.get("vote_average").getAsString();
            }
            String releaseDate = "Fecha no disponible";
            if (jsonResultado.has("release_date") && !jsonResultado.get("release_date").isJsonNull()) {
                releaseDate = jsonResultado.get("release_date").getAsString();
            }
            String imageURL = "https://www.cucea.udg.mx/sites/default/files/styles/publicaciones/public/publicaciones/portadas/sin_portada_8.jpg?itok=yR2MLoZs";
            if (jsonResultado.has("poster_path") && !jsonResultado.get("poster_path").isJsonNull()) {
                imageURL = "https://image.tmdb.org/t/p/w600_and_h900_bestv2" + jsonResultado.get("poster_path").getAsString();
            }
            //Y formamos una nueva película.
            Movie m = new Movie();
            m.setId(id);
            m.setTitle(title);
            m.setDescripcion(overview);
            m.setRank("Fuera del top 10");
            m.setRating(vote_average);
            m.setFecha(releaseDate);
            m.setImageUrl(imageURL);
            m.setCargada(true);
            peliculasFiltradas.add(m);
        }
        return peliculasFiltradas;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        binding = FragmentBuscarBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        //Declaramos e inicializamos un executor y mainHandler para las imágenes de las carátulas y la conexión con los enlaces en MovieAdapter.
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        //Localizamos los elementos gráficos para darles funciones.
        spinnerGeneros = binding.spinnerGeneros;
        year = binding.editTextNumber;
        buttonBuscar = binding.buttonBuscar;

        cargarGeneros();
        //Función para el botón buscar que obtiene el año introducido en el cajón de texto.
        buttonBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String yearText = year.getText().toString().trim();
                //Obtiene el género seleccionado de la lista de generos (spinner).
                String generoSeleccionado = spinnerGeneros.getSelectedItem().toString();
                //Esto lo hacemos comparando el nombre del género que tenemos seleccionado con el nombre
                //Del género en la lista, de donde obtenemos el id para añadirlo a la búsqueda de películas.
                int idGeneroSeleccionado = 0;
                for (Genero genero : generos) {
                    if (genero.getNombre().equals(generoSeleccionado)) {
                        idGeneroSeleccionado = genero.getId();
                        break;
                    }
                }
                //Comprobamos que el año no este vacío y sea coherente.
                if ((yearText.isEmpty() || Integer.parseInt(yearText) < 1894 || Integer.parseInt(yearText) > 2028)) {
                    Toast.makeText(getContext(), "Ingrese un año válido.", Toast.LENGTH_SHORT).show();
                    return;
                }
                //Creamos el endpoint completo para buscar las películas con el año y genero seleccionado.
                String url = ENDPOINT_PELIS + yearText + urlSortDesc + urlGenero + idGeneroSeleccionado;
                cargarPeliculas(url);
            }
        });


        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executorService.shutdown();
        binding = null;
    }

    //Método para cargar los géneros en una lista para poder obtenerlos y establecerlos en un spinner para que el usuario pueda filtrar.
    private void cargarGeneros() {
        executorService.execute(() -> {
            try {
                String urlString = BASE_URL + ENDPOINT_GENEROS;
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", AUTHORIZATION);
                conn.setRequestProperty("accept", ACCEPT);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(10000);
                conn.connect();
                int codRespuesta = conn.getResponseCode();
                if (codRespuesta == HttpURLConnection.HTTP_OK) {
                    String respuesta = convertirAString(conn.getInputStream());
                    conn.disconnect();
                    //Pruebas para comprobar si recibía el json.
                    Log.d("HomeFragment", "Respuesta: " + respuesta);
                    //De nuevo con una nueva instancia de gson obtenemos los datos.
                    Gson gson = new Gson();
                    JsonObject jsonObject = gson.fromJson(respuesta, JsonObject.class);
                    JsonArray genresArray = jsonObject.getAsJsonArray("genres");
                    for (JsonElement genreElement : genresArray) {
                        JsonObject jsonGenre = genreElement.getAsJsonObject();

                        int idGenero = jsonGenre.get("id").getAsInt();
                        String nombreGenero = jsonGenre.get("name").getAsString();
                        //Una vez obtenidos los datos creamos un nuevo genero y lo añadimos a la lista.
                        Genero g = new Genero(idGenero, nombreGenero);
                        generos.add(g);
                    }
                    //Declaro e inicializo una nueva lista de Strings para los nombres de los géneros.
                    List<String> nombresGeneros = new ArrayList<>();
                    //Donde añado solo el nombre de los genros.
                    for (Genero genero : generos) {
                        nombresGeneros.add(genero.getNombre());
                    }
                    //Establecemos el nombre del género en el spinner.
                    mainHandler.post(() -> {
                        if (!nombresGeneros.isEmpty()) {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    requireContext(),
                                    android.R.layout.simple_spinner_item,
                                    nombresGeneros
                            );
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerGeneros.setAdapter(adapter);
                        } else {
                            Toast.makeText(getContext(), "No se encontraron géneros.", Toast.LENGTH_SHORT).show();
                        }
                    });

                }


            } catch (ProtocolException e) {
                throw new RuntimeException(e);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    //Método para cargar las películas igual que los anteriores.
    private void cargarPeliculas(String urlPeliculas) {
        executorService.execute(() -> {
            try {
                String urlString = BASE_URL + urlPeliculas;
                Log.d("URL", urlString);
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", AUTHORIZATION);
                conn.setRequestProperty("accept", ACCEPT);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(10000);
                conn.connect();
                int codRespuesta = conn.getResponseCode();
                if (codRespuesta == HttpURLConnection.HTTP_OK) {
                    String respuesta = convertirAString(conn.getInputStream());
                    conn.disconnect();
                    //Código de prueba para ver el json.
                    Log.d("BuscarFragment", "Respuesta: " + respuesta);

                    //Obtenemos las peliculasFiltradas del método getMovies
                    List<Movie> peliculasFiltradas = getMovies(respuesta);

                    //Con el MainHandler para trabajar sobre el hilo principal comprobamos que la lista de pelis filtradas no esté vacía.
                    mainHandler.post(() -> {
                        if (!peliculasFiltradas.isEmpty()) {
                            //Declaramos un array list y lo igualamos a la lista de películas filtradas
                            ArrayList<Movie> peliculasArrayList = new ArrayList<>(peliculasFiltradas);
                            //Para que con el bundle que declaramos aquí
                            Bundle bundle = new Bundle();
                            //Podamos poner de parcelable el bundle del array list de películas
                            bundle.putParcelableArrayList("peliculas", peliculasArrayList);
                            //Para navegar al fragmento de búsqueda de películas.
                            NavController navController = NavHostFragment.findNavController(this);
                            navController.navigate(R.id.action_buscarFragment_to_pelisBuscadas, bundle);
                        } else {
                            Toast.makeText(getContext(), "No se encontraron películas", Toast.LENGTH_SHORT).show();
                        }
                    });

                }


            } catch (ProtocolException e) {
                throw new RuntimeException(e);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String convertirAString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

}