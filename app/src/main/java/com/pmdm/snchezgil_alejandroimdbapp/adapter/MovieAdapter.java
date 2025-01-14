package com.pmdm.snchezgil_alejandroimdbapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pmdm.snchezgil_alejandroimdbapp.MovieDetailsActivity;
import com.pmdm.snchezgil_alejandroimdbapp.R;
import com.pmdm.snchezgil_alejandroimdbapp.database.FavoritesDatabaseHelper;
import com.pmdm.snchezgil_alejandroimdbapp.models.Movie;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.ViewHolder>{
    //Declaramos las variables necesarias.
    private Context context;
    private List<Movie> movies;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler();
    private String idUsuario;
    private FavoritesDatabaseHelper databaseHelper;
    private boolean favoritos;

    public MovieAdapter(Context context, List<Movie> movies, String idUsuario, FavoritesDatabaseHelper databaseHelper, boolean favoritos){
        this.context = context;
        this.movies = movies;
        this.idUsuario = idUsuario;
        this.databaseHelper = databaseHelper;
        this.favoritos = favoritos;

    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position){
        //Declaramos una instancia de película y la inicializamos a la posición obtenida de la lista de películas que nos pasan como argumento.
        Movie movie = movies.get(position);
        //Por cada película de la lista recibida cargamos la imagen.
        executorService.execute(() -> {
            try {
                URL url = new URL(movie.getImageUrl());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                InputStream is = conn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                mainHandler.post(() -> holder.posterImageView.setImageBitmap(bitmap));
            } catch (Exception e) {
                e.printStackTrace();
                //Imagen por defecto que no debería de llegarse a ver en ningún momento porque tengo la excepción manejada con nulos en otra parte con otra carátula.
                mainHandler.post(() -> holder.posterImageView.setImageResource(R.drawable.ic_launcher_foreground));
            }
        });
        //Obtenemos del ViewHolder el item, es decir el objeto película que aparece en la lista del recycler view.
        //Le damos un onClickListener para que abra los detalles de la película con un intent.
        holder.itemView.setOnClickListener(v -> {
            if(movie.isCargada()) {
                Intent intent = new Intent(context, MovieDetailsActivity.class);
                intent.putExtra("MOVIE", movie);
                context.startActivity(intent);
            }else{
                Toast.makeText(context, "Cargando datos, por favor espera...", Toast.LENGTH_SHORT).show();
            }
        });
        //También añadiremos un onLongClickListener.
        holder.itemView.setOnLongClickListener(v -> {
            executorService.execute(() -> {
                //Si no estamos en favoritos y los detalles de la película han cargado la agregamos a favoritos.
                if (!favoritos) {
                    if(movie.isCargada()) {
                        agregarFavorito(movie, holder.getAdapterPosition());
                    }else{
                        mainHandler.post(() ->
                        Toast.makeText(context, "Cargando datos, por favor espera... ",Toast.LENGTH_SHORT).show()
                        );
                    }
                    //Si no estamos en favoritos la borramos.
                } else {
                    eliminarFavorito(movie, holder.getAdapterPosition());
                }
            });

            return true;
        });
    }
    //Método que sirve para mostrar en pantalla un número determinado de objetos en este caso de tipo movie.
    //Por lo que depende del tamaño de la lista de películas y en caso de ser nulo pues es 0.
    @Override
    public int getItemCount(){
        if (movies != null) {
            return movies.size();
        } else {
            return 0;
        }
    }
    //Muestra el ViewHolder con el elemento que queremos que muestre que son las caratulas en el RecyclerView
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView posterImageView;
        public ViewHolder(@NonNull View itemView){
            super(itemView);
            posterImageView = itemView.findViewById(R.id.posterImageView);
        }
    }
    //Método para agregar los favoritos utilizando la base de datos, lo insertamos con el método de la clase de la base de datos.
    private void agregarFavorito(Movie movie, int position) {
        SQLiteDatabase dbWrite = databaseHelper.getWritableDatabase();
        long result = databaseHelper.insertarFavorito(dbWrite, idUsuario, movie.getId(), movie.getTitle(), movie.getDescripcion(), movie.getFecha(), movie.getRank(),movie.getRating() , movie.getImageUrl());
        dbWrite.close();

        if (result != -1) {
            mainHandler.post(() ->
                    Toast.makeText(context, "Se ha agregado a favoritos: " + movie.getTitle(), Toast.LENGTH_SHORT).show()
            );
        } else {
            mainHandler.post(() ->
                    Toast.makeText(context, "Error al agregar a favoritos, comprueba que no tengas ya la película añadida.", Toast.LENGTH_SHORT).show()
            );
        }
    }
    //Método para eliminar favoritos, le hacemos una consulta a la base de datos y borramos la película cuyo usuario e id de película corresponda.
    private void eliminarFavorito(Movie movie, int position) {
        SQLiteDatabase dbWrite = databaseHelper.getWritableDatabase();
        int rowsDeleted = dbWrite.delete(
                FavoritesDatabaseHelper.TABLE_FAVORITOS,
                "idUsuario=? AND idPelicula=?",
                new String[]{idUsuario, movie.getId()}
        );
        dbWrite.close();

        if (rowsDeleted > 0) {
            mainHandler.post(() -> {
                Toast.makeText(context, "Eliminado de favoritos: " + movie.getTitle(), Toast.LENGTH_SHORT).show();
                movies.remove(position);
                notifyItemRemoved(position);
            });
        } else {
            mainHandler.post(() ->
                    Toast.makeText(context, "Error al eliminar: " + movie.getTitle(), Toast.LENGTH_SHORT).show()
            );
        }
    }
}
