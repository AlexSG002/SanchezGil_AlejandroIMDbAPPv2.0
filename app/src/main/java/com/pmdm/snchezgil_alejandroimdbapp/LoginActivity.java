package com.pmdm.snchezgil_alejandroimdbapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {
    //Declaramos las variables de Firebase y GoogleSignInClient.
    private FirebaseAuth mAuth;
    private GoogleSignInClient gClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        //Obtenemos la instancia de Firebase.
        mAuth = FirebaseAuth.getInstance();
        //Declaramos una variable de tipo GoogleSignInOptions y la inicializamos al inicio de sesión por defecto.
        GoogleSignInOptions gOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build();
        //Inicializamos la variable de GoogleSignInClient utilizando las opciones de inicio de sesión por defecto.
        gClient = GoogleSignIn.getClient(this, gOptions);
        //Declaramos e inicializamos la variable usuarioActual a el usuario que recogemos con la variable de Firebase.
        FirebaseUser usuarioActual = mAuth.getCurrentUser();
        //Comprobamos que el usuario no sea nulo y navegamos al main.
        if (usuarioActual != null){
            irAMain();
        }
        //Launcher para lanzar la pestaña de selección de cuenta de Google.
        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode() == RESULT_OK){
                            Intent data = result.getData();
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                            try{
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                if (account != null){
                                    autentificacionFirebaseGoogle(account.getIdToken());
                                }
                            } catch (ApiException e){
                                Toast.makeText(LoginActivity.this, "Algo ha ido mal", Toast.LENGTH_SHORT).show();
                            }
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
                activityResultLauncher.launch(signInIntent);
            }
        });
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
    private void irAMain(){
        finish();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
    }
    //Método que sirve par ir directamente al main en caso de que el usuario no haya cerrado la sesión.
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser usuarioActual = mAuth.getCurrentUser();
        if(usuarioActual != null){
            irAMain();
        }
    }
}
