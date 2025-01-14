package com.pmdm.snchezgil_alejandroimdbapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;
//Clase de la base de datos.
public class FavoritesDatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_NOMBRE = "Favoritos.db";
    public static final String TABLE_FAVORITOS = "t_favoritos";

    public FavoritesDatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NOMBRE, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE "+TABLE_FAVORITOS + "(" +
                "idPelicula TEXT," +
                "idUsuario TEXT,"+
                "nombrePelicula TEXT ," +
                "descripcionPelicula TEXT," +
                "fechaLanzamiento TEXT," +
                "rankingPelicula INTEGER," +
                "ratingPelicula REAL," +
                "caratulaURL TEXT," +
                "PRIMARY KEY (idUsuario, idPelicula))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE "+TABLE_FAVORITOS);
        onCreate(db);
    }

    public long insertarFavorito(SQLiteDatabase db,
                                 String idUsuario,
                                 String idPelicula,
                                 String nombre,
                                 String descripcion,
                                 String fechaLanzamiento,
                                 String ranking,
                                 String ratingPelicula,
                                 String caratulaURL) {

        ContentValues valores = new ContentValues();
        valores.put("idPelicula", idPelicula);
        valores.put("idUsuario", idUsuario);
        valores.put("nombrePelicula", nombre);
        valores.put("descripcionPelicula", descripcion);
        valores.put("fechaLanzamiento", fechaLanzamiento);
        valores.put("rankingPelicula", ranking);
        valores.put("ratingPelicula", ratingPelicula);
        valores.put("caratulaURL", caratulaURL);

        return db.insert(TABLE_FAVORITOS, null, valores);
    }

}
