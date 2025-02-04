package com.pmdm.snchezgil_alejandroimdbapp;


import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pmdm.snchezgil_alejandroimdbapp.database.IMDbDatabaseHelper;
import com.pmdm.snchezgil_alejandroimdbapp.sync.UsersSync;
import com.pmdm.snchezgil_alejandroimdbapp.utils.AppLifecycleManager;

import org.json.JSONException;

public class LoginActivity extends AppCompatActivity {
    //Declaramos las variables de Firebase y GoogleSignInClient.
    private FirebaseAuth mAuth;
    private GoogleSignInClient gClient;
    private CallbackManager callbackManager;
    private final String TAG = "LOGIN ACTIVITY";
    private AuthCredential facebookCredential;
    private ActivityResultLauncher<Intent> activityResultLauncherGoogleSignIn;
    private ActivityResultLauncher<Intent> activityResultLauncherGoogleLinking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        //Declaro e inicializo una nueva instancia del lifecycle para registrar los logs,
        //con esta instancia registro los callbacks y los callbacks de los componentes.
        AppLifecycleManager manager = new AppLifecycleManager(this);
        getApplication().registerActivityLifecycleCallbacks(manager);
        getApplication().registerComponentCallbacks(manager);

        //Declaro e inicializo un nuevo callbackManager para el inicio de sesión con Facebook.
        callbackManager = CallbackManager.Factory.create();

        //Obtenemos la instancia de Firebase.
        mAuth = FirebaseAuth.getInstance();
        //Declaramos una variable de tipo GoogleSignInOptions y la inicializamos al inicio de sesión por defecto.
        GoogleSignInOptions gOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        //Inicializamos la variable de GoogleSignInClient utilizando las opciones de inicio de sesión por defecto.
        gClient = GoogleSignIn.getClient(this, gOptions);
        //Declaramos e inicializamos la variable usuarioActual a el usuario que recogemos con la variable de Firebase.

        //Launcher para lanzar la pestaña de selección de cuenta de Google.
        activityResultLauncherGoogleSignIn = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                            try {
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                if (account != null) {
                                    autentificacionFirebaseGoogle(account.getIdToken());
                                }
                            } catch (ApiException e) {
                                Toast.makeText(LoginActivity.this, "Algo ha ido mal", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        GoogleSignInOptions gLinkOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient googleLinkClient = GoogleSignIn.getClient(this, gLinkOptions);
        //Launcher que muestra la ventana de selcción de cuenta de google para la vinculación Google/Facebook.
        activityResultLauncherGoogleLinking = registerForActivityResult
                (new ActivityResultContracts.StartActivityForResult(),
                        new ActivityResultCallback<ActivityResult>() {
                            @Override
                            public void onActivityResult(ActivityResult result) {
                                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                    Intent data = result.getData();
                                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                                    try {
                                        GoogleSignInAccount account = task.getResult(ApiException.class);
                                        if (account != null) {
                                            vincularGoogleConFacebook(account);
                                        }
                                    } catch (ApiException e) {
                                        Log.e(TAG, "Google sign in failed for linking", e);
                                        Toast.makeText(LoginActivity.this, "Algo ha ido mal durante el inicio de sesión con Google para vincular.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(LoginActivity.this, "Inicio de sesión con Google cancelado.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });


        //Declaramos e inicializamos la variable botón.
        Button button = findViewById(R.id.sign_in_button);
        //Le añadimos el OnClickListener.
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Declaramos e incializamos el intent a la pestaña de selección de cuentas de google de gClient.
                Intent signInIntent = gClient.getSignInIntent();
                //Utilizando el launcher lanzamos el intent de inicio de sesión.
                activityResultLauncherGoogleSignIn.launch(signInIntent);
            }
        });
        //Funciones del botón de login con Facebook.
        LoginButton loginButton = findViewById(R.id.login_button);
        loginButton.setReadPermissions("email", "public_profile");
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
            }

            @Override
            public void onError(@NonNull FacebookException e) {
                Log.d(TAG, "facebook:onError", e);
            }
        });

        EditText editTextEmail = findViewById(R.id.editTextEmail);
        EditText editTextPassword = findViewById(R.id.editTextTextPassword);

        Button buttonRegsitroEmail = findViewById(R.id.buttonRegistro);
        Button buttonInicioEmail = findViewById(R.id.buttonLogIn);
        //Botón para registrar una cuenta por mail.
        buttonRegsitroEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mail = "";
                String password = "";
                if (editTextEmail.getText().toString().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                    if (!String.valueOf(editTextPassword.getText()).isEmpty()) {
                        mail = String.valueOf(editTextEmail.getText());
                        password = String.valueOf(editTextPassword.getText());
                    } else {
                        Toast.makeText(LoginActivity.this, "La clave no puede estar vacía", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "El email introducido no es correcto", Toast.LENGTH_SHORT).show();
                    return;
                }
                mAuth.createUserWithEmailAndPassword(mail, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                            irAMain();
                        } else if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            FirebaseAuthUserCollisionException exception = (FirebaseAuthUserCollisionException) task.getException();
                            String email = exception.getEmail();
                            Toast.makeText(LoginActivity.this, "Cuenta ya en uso, porfavor inicia sesión con el método seleccionado.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Registro fallido, revisa si la contraseña que has introducido tiene más de 6 caracteres.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        });
        //Botón para iniciar sesión por email.
        buttonInicioEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mail = "";
                String password = "";
                if (editTextEmail.getText().toString().matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                    if (!String.valueOf(editTextPassword.getText()).isEmpty()) {
                        mail = String.valueOf(editTextEmail.getText());
                        password = String.valueOf(editTextPassword.getText());
                    } else {
                        Toast.makeText(LoginActivity.this, "La clave no puede estar vacía", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "El email introducido no es correcto", Toast.LENGTH_SHORT).show();
                    return;
                }
                mAuth.signInWithEmailAndPassword(mail, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                            irAMain();
                        } else if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            FirebaseAuthUserCollisionException exception = (FirebaseAuthUserCollisionException) task.getException();
                            String email = exception.getEmail();
                            Toast.makeText(LoginActivity.this, "Cuenta ya en uso, porfavor inicia sesión con el método seleccionado (Facebook o Google).", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Inicio de sesión fallido", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);

    }

    //Método para obtener las credenciales de Google e iniciar sesión en Firebase.
    private void autentificacionFirebaseGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            irAMain();
                        } else {
                            Toast.makeText(LoginActivity.this, "Autentificación fallida.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    //Método para ir al Main.
    private void irAMain() {
        AppLifecycleManager.isLogging = true;
        finish();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
    }

    //Método que sirve par ir directamente al main en caso de que el usuario no haya cerrado la sesión.
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser usuarioActual = mAuth.getCurrentUser();
        if (usuarioActual != null) {
            UsersSync usersSync = new UsersSync(FirebaseFirestore.getInstance(), new IMDbDatabaseHelper(getApplicationContext()));
            usersSync.descargarUsuariosNubeALocal(new UsersSync.CloudSyncCallback() {
                @Override
                public void onSuccess() {
                    irAMain();
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("LoginActivity", "Error sincronizando datos desde la nube: " + e.getMessage());
                }
            });
        }
    }
    //Método que maneja el inicio de sesión con Facebook.
    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        FirebaseUser user = mAuth.getCurrentUser();
        // Usuario no autenticado, intenta iniciar sesión con Facebook
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Inicio de sesión exitoso
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser newUser = mAuth.getCurrentUser();
                        Toast.makeText(LoginActivity.this, "Autenticación con Facebook exitosa.", Toast.LENGTH_SHORT).show();
                        irAMain();
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            // Manejar la colisión de cuentas
                            FirebaseAuthUserCollisionException exception = (FirebaseAuthUserCollisionException) task.getException();
                            String email = exception.getEmail();
                            Log.w(TAG, "signInWithCredential:failure", task.getException());

                            // Luego le muestra un diálogo al usuario para iniciar sesión con Google y luego vincular.
                            mostrarDialogoVinculacion(email, credential);
                        } else {
                            // Otro tipo de error
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Autenticación con Facebook fallida.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    //Método para mostrar el dialogo de vinculación de Google/Facebook.
    private void mostrarDialogoVinculacion(String email, AuthCredential credential) {
        new AlertDialog.Builder(this)
                .setTitle("Cuenta ya existente")
                .setMessage("Ya existe una cuenta con el correo electrónico " + email + ". ¿Deseas vincular tu cuenta de Facebook con tu cuenta de Google?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    // Almacenar la credencial de Facebook para vinculación posterior
                    facebookCredential = credential;
                    // Iniciar el flujo de Google Sign-In para vincular
                    activityResultLauncherGoogleLinking.launch(gClient.getSignInIntent());
                })
                .setNegativeButton("No", (dialog, which) -> {
                    Toast.makeText(LoginActivity.this, "No se han vinculado las cuentas.", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }

    //Método para vincular Google con Facebook.
    private void vincularGoogleConFacebook(GoogleSignInAccount account) {
        AuthCredential googleCredential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

        // Reautenticar con Google
        mAuth.signInWithCredential(googleCredential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Vincular la credencial de Facebook con la cuenta de Google
                        mAuth.getCurrentUser().linkWithCredential(facebookCredential)
                                .addOnCompleteListener(this, linkTask -> {
                                    if (linkTask.isSuccessful()) {
                                        Log.d(TAG, "linkWithCredential:success");
                                        FirebaseUser linkedUser = linkTask.getResult().getUser();
                                        Toast.makeText(LoginActivity.this, "Cuentas de Google y Facebook vinculadas exitosamente.", Toast.LENGTH_SHORT).show();
                                        irAMain();
                                    } else {
                                        Log.w(TAG, "linkWithCredential:failure", linkTask.getException());
                                        Toast.makeText(LoginActivity.this, "Vinculación de cuentas fallida.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Log.w(TAG, "signInWithCredential:failure during linking", task.getException());
                        Toast.makeText(LoginActivity.this, "Autenticación con Google fallida para vincular.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
