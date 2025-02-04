package com.pmdm.snchezgil_alejandroimdbapp.sync;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.pmdm.snchezgil_alejandroimdbapp.database.IMDbDatabaseHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersSync {

    private final FirebaseFirestore firestore;
    private final IMDbDatabaseHelper database;

    public UsersSync(FirebaseFirestore firestore, IMDbDatabaseHelper database) {
        this.firestore = firestore;
        this.database = database;
    }

    public void subirUsuariosLocalANube() {
        // Abrimos la base de datos en modo lectura
        SQLiteDatabase db = database.getReadableDatabase();
        // Consultamos para obtener todos los usuarios de la tabla de usuarios.
        String query = "SELECT * FROM " + IMDbDatabaseHelper.TABLE_USUARIOS;
        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            // Obtenemos cada campo comprobando que el índice no sea -1
            String idUsuario = "";
            int colIdUsuario = cursor.getColumnIndex("idUsuario");
            if (colIdUsuario != -1) {
                idUsuario = cursor.getString(colIdUsuario);
            } else {
                idUsuario = "";
            }

            String nombre = "";
            int colNombre = cursor.getColumnIndex("nombre");
            if (colNombre != -1) {
                nombre = cursor.getString(colNombre);
            } else {
                nombre = "";
            }

            String email = "";
            int colEmail = cursor.getColumnIndex("email");
            if (colEmail != -1) {
                email = cursor.getString(colEmail);
            } else {
                email = "";
            }

            String loginRegistro = "";
            int colLoginRegistro = cursor.getColumnIndex("loginRegistro");
            if (colLoginRegistro != -1) {
                loginRegistro = cursor.getString(colLoginRegistro);
            } else {
                loginRegistro = "";
            }

            String logoutRegistro = "";
            int colLogoutRegistro = cursor.getColumnIndex("logoutRegistro");
            if (colLogoutRegistro != -1) {
                logoutRegistro = cursor.getString(colLogoutRegistro);
            } else {
                logoutRegistro = "";
            }

            String direccion = "";
            int colDireccion = cursor.getColumnIndex("direccion");
            if (colDireccion != -1) {
                direccion = cursor.getString(colDireccion);
            } else {
                direccion = "";
            }

            String telefono = "";
            int colTelefono = cursor.getColumnIndex("telefono");
            if (colTelefono != -1) {
                telefono = cursor.getString(colTelefono);
            } else {
                telefono = "";
            }

            String foto = "";
            int colFoto = cursor.getColumnIndex("foto");
            if (colFoto != -1) {
                foto = cursor.getString(colFoto);
            } else {
                foto = "";
            }

            // Creamos el objeto HashMap que subiremos a Firestore con los datos recogidos.
            Map<String, Object> usuario = new HashMap<>();
            usuario.put("idUsuario", idUsuario);
            usuario.put("nombre", nombre);
            usuario.put("email", email);
            //Guardamos los campos de logs como campo de union array para unir los campos anteriores en vez de borrarlos.
            usuario.put("loginRegistro", FieldValue.arrayUnion(loginRegistro));
            usuario.put("logoutRegistro", FieldValue.arrayUnion(logoutRegistro));
            usuario.put("direccion", direccion);
            usuario.put("telefono", telefono);
            usuario.put("foto", foto);
            //Variable para el seguimiento con logs.
            final String finalIdUsuario = idUsuario;
            //Creamos la colección.
            firestore.collection("usuarios")
                    .document(idUsuario)
                    //Lo guardamos como merge para que los datos de los logs se mantengan y simplemente se guarde el historial.
                    .set(usuario, SetOptions.merge())
                    .addOnSuccessListener(aVoid ->
                            Log.d("Firestore", "Usuario " + finalIdUsuario + " subido correctamente"))
                    .addOnFailureListener(e ->
                            Log.e("Firestore", "Error al subir el usuario " + finalIdUsuario, e));
        }
        cursor.close();
    }
    //Método para descargarnos los usuarios de la nube al local.
    public void descargarUsuariosNubeALocal(final CloudSyncCallback callback) {
        //Obtenemos la colección por su nombre.
        firestore.collection("usuarios")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        //Abrimos la base de datos.
                        SQLiteDatabase db = database.getWritableDatabase();
                        //Obtenemos los datos de una snapshot del documento de donde vamos a ir sacando todos los datos.
                        List<DocumentSnapshot> documentos = queryDocumentSnapshots.getDocuments();
                        for (DocumentSnapshot document : documentos) {
                            String idUsuario = document.getString("idUsuario");
                            String nombre = document.getString("nombre");
                            String email = document.getString("email");
                            //Los logs tanto de inicio de sesión como de cierre son un objeto ya que son
                            //Un array de strings, los obtenemos para guardar el último log en la base de datos local.
                            Object loginRegistroObj = document.get("loginRegistro");
                            String loginRegistro = "";
                            if (loginRegistroObj instanceof String) {
                                loginRegistro = (String) loginRegistroObj;
                            } else if (loginRegistroObj instanceof List) {
                                List<?> loginList = (List<?>) loginRegistroObj;
                                if (!loginList.isEmpty()) {
                                    loginRegistro = loginList.get(loginList.size() - 1).toString();
                                }
                            }

                            Object logoutRegistroObj = document.get("logoutRegistro");
                            String logoutRegistro = "";
                            if (logoutRegistroObj instanceof String) {
                                logoutRegistro = (String) logoutRegistroObj;
                            } else if (logoutRegistroObj instanceof List) {
                                List<?> logoutList = (List<?>) logoutRegistroObj;
                                if (!logoutList.isEmpty()) {
                                    logoutRegistro = logoutList.get(logoutList.size() - 1).toString();
                                }
                            }
                            String direccion = document.getString("direccion");
                            String telefono = document.getString("telefono");
                            String foto = document.getString("foto");
                            //Si el usuario es nulo no hace nada.
                            if (idUsuario == null) {
                                Log.e("UsersSync", "idUsuario es null, ignorando entrada.");
                                continue;
                            }
                            //En caso de que haya datos nulos en la bd de la nube.
                            if (nombre == null) nombre = "Nombre desconocido";
                            if (email == null) email = "Correo no disponible";
                            if (direccion == null) direccion = "Sin dirección";
                            if (telefono == null) telefono = "Sin teléfono";
                            if (foto == null) foto = "";

                            //Insertamos en la base local.
                            Cursor cursor = db.rawQuery("SELECT idUsuario FROM " + IMDbDatabaseHelper.TABLE_USUARIOS + " WHERE idUsuario = ?", new String[]{idUsuario});
                            ContentValues cv = new ContentValues();
                            cv.put("nombre", nombre);
                            cv.put("email", email);
                            cv.put("loginRegistro", loginRegistro);
                            cv.put("logoutRegistro", logoutRegistro);
                            cv.put("direccion", direccion);
                            cv.put("telefono", telefono);
                            cv.put("foto", foto);

                            if (cursor.moveToFirst()) {
                                db.update(IMDbDatabaseHelper.TABLE_USUARIOS, cv, "idUsuario = ?", new String[]{idUsuario});
                                Log.d("Firestore", "Usuario " + idUsuario + " actualizado en la base local");
                            } else {
                                cv.put("idUsuario", idUsuario);
                                db.insert(IMDbDatabaseHelper.TABLE_USUARIOS, null, cv);
                                Log.d("Firestore", "Usuario " + idUsuario + " insertado en la base local");
                            }
                            cursor.close();
                        }
                        db.close();
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e("Firestore", "Error al descargar usuarios", e);
                        callback.onFailure(e);
                    }
                });
    }
    //Métodos de callback para hacer llamadas a los métodos desde otras clases ya que la descarga de la bd en la nube
    //Es una tarea asíncrona.
    public interface CloudSyncCallback {
        void onSuccess();

        void onFailure(Exception e);
    }
}



