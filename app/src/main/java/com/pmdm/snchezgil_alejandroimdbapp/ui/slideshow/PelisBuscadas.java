package com.pmdm.snchezgil_alejandroimdbapp.ui.slideshow;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.pmdm.snchezgil_alejandroimdbapp.adapter.MovieAdapter;
import com.pmdm.snchezgil_alejandroimdbapp.database.FavoritesDatabaseHelper;
import com.pmdm.snchezgil_alejandroimdbapp.databinding.FragmentPelisBuscadasBinding;
import com.pmdm.snchezgil_alejandroimdbapp.models.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PelisBuscadas extends Fragment {
    //Declaramos las variables que vamos a utilizar.
    private FragmentPelisBuscadasBinding binding;
    private ExecutorService executorService;
    private Handler mainHandler;
    private String idUsuario;
    private List<Movie> pelis = new ArrayList<>();
    private boolean favoritos = false;
    private FavoritesDatabaseHelper database;
    private MovieAdapter adapter;

    public PelisBuscadas() {

    }
    //Obtenemos la lista de pelis filtradas como argumento y la pasamos como parcelable para añadirla a favoritos.
    public static PelisBuscadas newInstance(ArrayList<Movie> pelisFiltradas) {
        PelisBuscadas fragment = new PelisBuscadas();
        Bundle args = new Bundle();
        args.putParcelable("pelis", (Parcelable) pelisFiltradas);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Obtenemos la instancia del usuario.
            idUsuario = FirebaseAuth.getInstance().getCurrentUser().getUid();
            //Si los argumentos obtenidos de la listas de pelíoculas no es nulo establecemos el parcelable de películas.
            if(getArguments()!=null){
                ArrayList<Movie> peliculasArrayList = getArguments().getParcelableArrayList("peliculas");
                if(peliculasArrayList != null){
                    //Y añadimos todas las películas al array list para cargarlas.
                    pelis.addAll(peliculasArrayList);
                }
        }

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentPelisBuscadasBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        //Inicializamos el executor, el mainHandler, la base de datos y configuramos el recyclerview para el adapter.
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        database = new FavoritesDatabaseHelper(requireContext());

        configurarRecyclerView();


        return root;
    }

    //Método para inicializar el adapter.
    private void configurarRecyclerView() {
        binding.recyclerViewBuscar.setLayoutManager(new LinearLayoutManager(requireContext()));
        if(pelis!=null && !pelis.isEmpty()) {
            adapter = new MovieAdapter(requireContext(), pelis, idUsuario, database, favoritos);
            binding.recyclerViewBuscar.setAdapter(adapter);
        }else {
            Toast.makeText(getContext(), "No se pudieron obtener las películas", Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executorService.shutdown();
        binding = null;
    }
}
