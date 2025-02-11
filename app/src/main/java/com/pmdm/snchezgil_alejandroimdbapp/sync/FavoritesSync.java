package com.pmdm.snchezgil_alejandroimdbapp.sync;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pmdm.snchezgil_alejandroimdbapp.database.IMDbDatabaseHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
//Clase FavoritesSync que maneja la vinculación de la base de datos local con la base de datos en la nube.
public class FavoritesSync {

    private final FirebaseFirestore firestore;
    private final IMDbDatabaseHelper database;
    private FirebaseAuth mAuth;

    public FavoritesSync(FirebaseFirestore firestore, IMDbDatabaseHelper database) {
        this.firestore = firestore;
        this.database = database;
    }
    //Método para subir favoritos a la nube, obtenemos los datos haciendo una consulta a la tabla de
    //favoritos de la base de datos local donde sacamos los datos de las películas favoritas por id de usuario.
    public void subirFavoritosANube() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e("FavoritesSync", "El usuario no está autenticado.");
            return;
        }

        String idUsuario = currentUser.getUid();
        SQLiteDatabase db = database.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + IMDbDatabaseHelper.TABLE_FAVORITOS + " WHERE idUsuario=? LIMIT 10",
                new String[]{idUsuario}
        );

        if (cursor != null && cursor.moveToFirst()) {
            Log.d("FavoritesSync", "Favoritos encontrados para el usuario: " + idUsuario);

            do {
                // Rescatar índices de columnas
                int colIdPelicula = cursor.getColumnIndex("idPelicula");
                int colTitulo = cursor.getColumnIndex("nombrePelicula");
                int colDescripcion = cursor.getColumnIndex("descripcionPelicula");
                int colFecha = cursor.getColumnIndex("fechaLanzamiento");
                int colRanking = cursor.getColumnIndex("rankingPelicula");
                int colRating = cursor.getColumnIndex("ratingPelicula");
                int colCaratula = cursor.getColumnIndex("caratulaURL");

                // Inicializar variables verificando que las columnas existen
                String idPelicula = "";
                if (colIdPelicula != -1) {
                    idPelicula = cursor.getString(colIdPelicula);
                }

                String titulo = "";
                if (colTitulo != -1) {
                    titulo = cursor.getString(colTitulo);
                }

                String descripcion = "";
                if (colDescripcion != -1) {
                    descripcion = cursor.getString(colDescripcion);
                }

                String fecha = "";
                if (colFecha != -1) {
                    fecha = cursor.getString(colFecha);
                }

                String ranking = "";
                if (colRanking != -1) {
                    ranking = cursor.getString(colRanking);
                }

                String rating = "";
                if (colRating != -1) {
                    rating = cursor.getString(colRating);
                }

                String caratula = "";
                if (colCaratula != -1) {
                    caratula = cursor.getString(colCaratula);
                }

                // Comprobamos que los datos no están vacíos.
                if (idPelicula.isEmpty() || titulo.isEmpty()) {
                    Log.w("FavoritesSync", "Película con datos incompletos, omitiendo...");
                    continue;
                }

                // Crear mapa para Firestore
                Map<String, Object> pelis = new HashMap<>();
                pelis.put("idUsuario", idUsuario);
                pelis.put("idPelicula", idPelicula);
                pelis.put("nombrePelicula", titulo);
                pelis.put("descripcionPelicula", descripcion);
                pelis.put("fechaLanzamiento", fecha);
                pelis.put("rankingPelicula", ranking);
                pelis.put("ratingPelicula", rating);
                pelis.put("caratulaURL", caratula);

                // Subir a Firestore
                firestore.collection("favoritas")
                        .document(idUsuario).collection("peliculas").document(idPelicula)
                        .set(pelis)
                        .addOnSuccessListener(aVoid ->
                                Log.d("FavoritesSync", "Película subida a la nube")
                        )
                        .addOnFailureListener(e ->
                                Log.e("FavoritesSync", "Error al subir película")
                        );

            } while (cursor.moveToNext());
        } else {
            Log.d("FavoritesSync", "No se encontraron favoritos para el usuario: " + idUsuario);
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
    }

    public void descargarFavoritosNubeALocal(final FavoritesSync.CloudSyncCallback callback) {
        firestore.collection("pelis")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    SQLiteDatabase db = database.getWritableDatabase();
                    List<DocumentSnapshot> documentos = queryDocumentSnapshots.getDocuments();
                    for (DocumentSnapshot document : documentos) {
                        String idUsuario = document.getString("idUsuario");
                        String idPelicula = document.getString("idPelicula");
                        String nombrePelicula = document.getString("nombrePelicula");
                        String descripcionPelicula = document.getString("descripcionPelicula");
                        String fechaLanzamiento = document.getString("fechaLanzamiento");
                        String rankingPelicula = document.getString("rankingPelicula");
                        String ratingPelicula = document.getString("ratingPelicula");
                        String caratulaURL = document.getString("caratulaURL");

                        ContentValues cv = new ContentValues();
                        cv.put("idUsuario", idUsuario);
                        cv.put("idPelicula", idPelicula);
                        cv.put("nombrePelicula", nombrePelicula);
                        cv.put("descripcionPelicula", descripcionPelicula);
                        cv.put("fechaLanzamiento", fechaLanzamiento);
                        cv.put("rankingPelicula", rankingPelicula);
                        cv.put("ratingPelicula", ratingPelicula);
                        cv.put("caratulaURL", caratulaURL);

                        // Primero, comprueba si ya existe
                        Cursor cursor = db.rawQuery(
                                "SELECT idPelicula FROM " + IMDbDatabaseHelper.TABLE_FAVORITOS + " WHERE idUsuario = ? AND idPelicula = ?",
                                new String[]{idUsuario, idPelicula});

                        if (cursor.moveToFirst()) {
                            // Si existe, actualiza los datos
                            db.update(IMDbDatabaseHelper.TABLE_FAVORITOS, cv, "idUsuario = ? AND idPelicula = ?", new String[]{idUsuario, idPelicula});
                            Log.d("Firestore", "Lista de favoritos de: " + idUsuario + " actualizado en la base local");
                        } else {
                            // Si no existe, inserta
                            db.insert(IMDbDatabaseHelper.TABLE_FAVORITOS, null, cv);
                            Log.d("Firestore", "Lista de favoritos de: " + idUsuario + " insertado en la base local");
                        }

                        cursor.close();
                    }
                    db.close();
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error al descargar usuarios", e);
                    callback.onFailure(e);
                });
    }
    //Método para eliminar los favoritos de la nube.
    public void eliminarFavoritoDeNube(String idUsuario, String idPelicula) {
        if (idUsuario == null || idPelicula == null || idUsuario.isEmpty() || idPelicula.isEmpty()) {
            Log.e("FavoritesSync", "No se puede eliminar: idUsuario o idPelicula inválidos.");
            return;
        }
        //Obtenemos el id de usuario y el id de la película y borramos el documento que tenga el id con las dos.
        firestore.collection("pelis")
                .document(idUsuario + "_" + idPelicula)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("FavoritesSync", "Película eliminada de la nube: " + idPelicula))
                .addOnFailureListener(e -> Log.e("FavoritesSync", "Error al eliminar película de la nube: " + idPelicula, e));
    }

    //Métodos de callback como en UsersSync.
    public interface CloudSyncCallback {
        void onSuccess();

        void onFailure(Exception e);
    }


}
