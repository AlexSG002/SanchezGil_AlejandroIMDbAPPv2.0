package com.pmdm.snchezgil_alejandroimdbapp.ui.home;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.GridLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pmdm.snchezgil_alejandroimdbapp.adapter.MovieAdapter;
import com.pmdm.snchezgil_alejandroimdbapp.database.FavoritesDatabaseHelper;
import com.pmdm.snchezgil_alejandroimdbapp.databinding.FragmentHomeBinding;
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


public class HomeFragment extends Fragment {

    //Declaramos las variables que necesitamos.
    private FragmentHomeBinding binding;
    private ExecutorService executorService;
    private Handler mainHandler;
    //Obtenemos el id de usuario al que se lo pasaremos al MovieAdapter para identificar al usuario al añadir sus favoritos.
    private String idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private static final String BASE_URL = "https://imdb-com.p.rapidapi.com/";
    //private static final String API_KEY = "200ca2873dmsh3c28ce355613a89p1dd78cjsndb8f2f9c0b09";
    //private static final String API_KEY = "ab93ab0e94mshebd8e2eb069c3e5p12c6b7jsn40f5cdaf18f8";
    //private static final String API_KEY = "8387dd50bamsh70639397777c48dp1f8dc5jsn8138e37a8f4f";
    //private static final String API_KEY = "7b9666c90cmsh018cf98d92659e1p1f7b9ejsn03cf7efd6bab";
    private static final String API_KEY = "10d6f51c11msh656b9bf6c5f2dafp186d10jsndf3eedfbfec3";
    private static final String HOST = "imdb-com.p.rapidapi.com";
    private static final String ENDPOINT_TOP10 = "title/get-top-meter?topMeterTitlesType=ALL";
    private static final String ENDPOINT_DESCRIPCION = "title/get-overview?tconst=";
    private static List<Movie> peliculasCargadas = new ArrayList<>();
    private FavoritesDatabaseHelper database;
    private boolean favoritos = false;

    //Al crearse la vista cargamos las películas.
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        database = new FavoritesDatabaseHelper(getContext());
        SQLiteDatabase db = database.getWritableDatabase();

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        binding.recyclerView.setLayoutManager(layoutManager);
        cargarTopMovies();

        return root;
    }
    //Método para cargar las descripciones de las películas que se ejecuta cuando se cargan las películas.
    private void cargarDescripciones(){
        //Con un executor
        executorService.execute(() ->{
            //Por cada película cargada
            for(Movie movie : peliculasCargadas) {
                //Hacemos la llamada a la api con el endpoint y el id de la película, para obtener su descripción y rating.
                try {
                    String urlString = BASE_URL + ENDPOINT_DESCRIPCION+movie.getId();
                    //Utilizando HttpURL nos conectamos y establecemos el método "GET" para obtener los datos
                    //Establecemos como propiedades la api y el host.
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("x-rapidapi-key", API_KEY);
                    conn.setRequestProperty("x-rapidapi-host", HOST);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(10000);
                    conn.connect();
                    int codRespuesta = conn.getResponseCode();
                    if (codRespuesta == HttpURLConnection.HTTP_OK) {
                        String respuesta = convertirAString(conn.getInputStream());
                        conn.disconnect();
                        //Log para pruebas para comprobar que recuperaba los datos del json correctamente.
                        Log.d("HomeFragment", "Respuesta: " + respuesta);

                        //Declaramos e inicializamos una nueva variable de la clas gson (librerías para parsear archivos json).
                        Gson gson = new Gson();
                        //Siguiendo la estructura del archivo json obtenemos cada objeto que nos hace falta para obtener los datos relevantes de las películas.
                        JsonObject jsonObject = gson.fromJson(respuesta, JsonObject.class);
                        JsonObject dataObject = jsonObject.getAsJsonObject("data");
                        JsonObject titleObject = dataObject.getAsJsonObject("title");
                        JsonObject plotObject = titleObject.getAsJsonObject("plot");
                        JsonObject ratingsObject = titleObject.getAsJsonObject("ratingsSummary");


                        //Obtenemos los datos.
                        String id = titleObject.get("id").getAsString();
                        String plotText = plotObject.getAsJsonObject("plotText").get("plainText").getAsString();
                        String rating = ratingsObject.get("aggregateRating").getAsString();
                        //Declaro e inicializo una nueva variable movieDesc para establecer los datos y añadirlas a un array de películas.
                        Movie movieDesc = new Movie();
                        movieDesc.setId(id);
                        movieDesc.setDescripcion(plotText);
                        movieDesc.setRating(rating);
                        List<Movie> peliculasDescripcion = new ArrayList<>();

                        peliculasDescripcion.add(movieDesc);
                        //Comparo por cada película cargada que obtenemos en el método de cargar películas con las de la descripción
                        for (Movie m : peliculasCargadas) {
                            for (Movie m1 : peliculasDescripcion) {
                                //Compruebo que tengan el mismo id para añadir a la lista de películas cargadas la descripción y el rating.
                                if (m.getId().equals(m1.getId())) {
                                   m.setDescripcion(m1.getDescripcion());
                                   m.setRating(m1.getRating());
                                   //Establezco la película como cargada para que el usuario pueda ver los detalles y añadirla a favoritos.
                                   m.setCargada(true);
                                }
                            }
                        }
                    }


                } catch (ProtocolException e) {
                    throw new RuntimeException(e);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    //Método para cargar el top de peliculas, en esencia igual que el anterior.
    private void cargarTopMovies() {
        executorService.execute(() -> {
            try {
                String urlString = BASE_URL + ENDPOINT_TOP10;
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("x-rapidapi-key", API_KEY);
                conn.setRequestProperty("x-rapidapi-host", HOST);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(10000);
                conn.connect();

                int codRespuesta = conn.getResponseCode();
                if (codRespuesta == HttpURLConnection.HTTP_OK) {
                    String respuesta = convertirAString(conn.getInputStream());
                    conn.disconnect();
                    //Código para comprobar el json.
                    Log.d("HomeFragment", "Respuesta: " + respuesta);

                    Gson gson = new Gson();
                    JsonObject jsonObject = gson.fromJson(respuesta, JsonObject.class);
                    JsonObject dataObject = jsonObject.getAsJsonObject("data");
                    JsonObject topMeterTitlesObject = dataObject.getAsJsonObject("topMeterTitles");
                    JsonArray edgesArray = topMeterTitlesObject.getAsJsonArray("edges");

                    List<Movie> movies = new ArrayList<>();

                    for (JsonElement edgeElement : edgesArray) {
                        JsonObject edgeObject = edgeElement.getAsJsonObject();
                        JsonObject nodeObject = edgeObject.getAsJsonObject("node");

                        String id = nodeObject.get("id").getAsString();
                        String rank = nodeObject.getAsJsonObject("meterRanking").get("currentRank").getAsString();
                        String titulo = nodeObject.getAsJsonObject("titleText").get("text").getAsString();
                        String imageUrl = nodeObject.getAsJsonObject("primaryImage").get("url").getAsString();
                        String mes = nodeObject.getAsJsonObject("releaseDate").get("month").getAsString();
                        String dia = nodeObject.getAsJsonObject("releaseDate").get("day").getAsString();
                        String year = nodeObject.getAsJsonObject("releaseDate").get("year").getAsString();

                        Movie movie = new Movie();
                        movie.setImageUrl(imageUrl);
                        movie.setTitle(titulo);
                        movie.setRank(rank);
                        movie.setId(id);
                        movie.setFecha(dia+"-"+mes+"-"+year);
                        movies.add(movie);
                        //Guardo las peliculas cargadas en el array declarado de inicio.
                        peliculasCargadas = movies;

                    }

                    //Declaramos una variable final de peliculas cargadas y la igualamos a la lista movies a la que hemos igualado también antes la lista de peliculas cargadas.
                    final List<Movie> finalMovies = movies;
                    //Para poder comprobar que la lista no sea nula cuando se llama al adaptador de movies.
                    mainHandler.post(() -> {
                        if (finalMovies != null && !finalMovies.isEmpty()) {
                            MovieAdapter adapter = new MovieAdapter(getContext(), finalMovies, idUsuario, database, favoritos);
                            binding.recyclerView.setAdapter(adapter);

                            cargarDescripciones();
                        } else {
                            Toast.makeText(getContext(), "No se pudieron obtener las películas", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    conn.disconnect();
                    mainHandler.post(() ->
                            Toast.makeText(getContext(), "Error en la petición: " + codRespuesta, Toast.LENGTH_SHORT).show()
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() ->
                        Toast.makeText(getContext(), "Error al cargar las películas: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    //Método para convertir el stream de entrada en strings como lo vimos el executor y añadiendo el paso a Strings
    private String convertirAString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executorService.shutdown();
        binding = null;
    }
}
