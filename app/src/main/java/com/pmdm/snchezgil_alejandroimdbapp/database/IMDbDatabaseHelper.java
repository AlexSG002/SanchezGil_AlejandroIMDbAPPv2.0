package com.pmdm.snchezgil_alejandroimdbapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

//Clase de la base de datos.
public class IMDbDatabaseHelper extends SQLiteOpenHelper {

    public static final String TABLE_FAVORITOS = "t_favoritos";
    public static final String TABLE_USUARIOS = "t_usuarios";
    private static final int DATABASE_VERSION = 6;
    private static final String DATABASE_NOMBRE = "Favoritos.db";

    public IMDbDatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NOMBRE, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //Creamos las tablas usuario y favoritos.
        db.execSQL("CREATE TABLE " + TABLE_USUARIOS + " (" +
                "idUsuario TEXT PRIMARY KEY," +
                "nombre TEXT," +
                "email TEXT," +
                "loginRegistro TEXT," +
                "logoutRegistro TEXT," +
                "direccion TEXT," +
                "telefono TEXT," +
                "foto TEXT" +
                ")");

        db.execSQL("CREATE TABLE " + TABLE_FAVORITOS + "(" +
                "idPelicula TEXT," +
                "idUsuario TEXT," +
                "nombrePelicula TEXT ," +
                "descripcionPelicula TEXT," +
                "fechaLanzamiento TEXT," +
                "rankingPelicula INTEGER," +
                "ratingPelicula REAL," +
                "caratulaURL TEXT," +
                "PRIMARY KEY (idUsuario, idPelicula)," +
                "FOREIGN KEY (idUsuario) REFERENCES " + TABLE_USUARIOS + "(idUsuario) ON DELETE CASCADE" + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE " + TABLE_FAVORITOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USUARIOS);
        onCreate(db);
    }
    //Método par insertar usuarios.
    public long insertarUsuario(SQLiteDatabase db,
                                String idUsuario,
                                String nombre,
                                String email,
                                String loginRegistro,
                                String logoutRegistro,
                                String direccion,
                                String telefono,
                                String foto) {
        ContentValues valores = new ContentValues();
        valores.put("idUsuario", idUsuario);
        valores.put("nombre", nombre);
        valores.put("email", email);
        valores.put("loginRegistro",
                loginRegistro);
        valores.put("logoutRegistro", logoutRegistro);
        valores.put("direccion", direccion);
        valores.put("telefono", telefono);
        valores.put("foto", foto);
        return db.insert(TABLE_USUARIOS, null, valores);
    }
    //Método para insertar favoritos.
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
    //Métodos para actualizar el login y logout en la base de datos local.
    public void actualizarLoginRegistro(String idUsuario, String loginRegistro) {
        if (loginRegistro == null) {
            loginRegistro = "";
        }
        //Obtenemos la instancia de la base de datos e insertamos en la tabla de usuario según el id de usuario.
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("loginRegistro", loginRegistro);
        db.update(TABLE_USUARIOS, valores, "idUsuario = ?", new String[]{idUsuario});
        db.close();
    }

    public void actualizarLogoutRegistro(String idUsuario, String logoutRegistro) {
        if (logoutRegistro == null) {
            logoutRegistro = "";
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("logoutRegistro", logoutRegistro);
        db.update(TABLE_USUARIOS, valores, "idUsuario = ?", new String[]{idUsuario});
        db.close();
    }


}
