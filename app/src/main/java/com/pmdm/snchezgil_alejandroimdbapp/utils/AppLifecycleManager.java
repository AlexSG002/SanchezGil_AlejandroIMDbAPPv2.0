package com.pmdm.snchezgil_alejandroimdbapp.utils;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pmdm.snchezgil_alejandroimdbapp.database.IMDbDatabaseHelper;

import java.text.SimpleDateFormat;

public class AppLifecycleManager implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {

    private static final String PREF_NAME = "user_prefs";
    private static final String PREF_IS_LOGGED_IN = "is_logged_in";
    private boolean isInBackground = false;
    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;
    public static boolean isLogging = false;
    private Context appContext;

    public AppLifecycleManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {
        if (activityReferences == 0 && isInBackground) {
            isInBackground = false;
            Log.d("AppLifecycleManager", "App en foreground.");
        }
        activityReferences++;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        isInBackground = false;
        SharedPreferences preferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREF_IS_LOGGED_IN, true);
        editor.apply();
        Log.d("AppLifecycleManager", "Usuario activo.");
    }

    @Override
    public void onActivityPaused(Activity activity) {
        isInBackground = true;
        Log.d("AppLifecycleManager", "App en background. Se iniciará el temporizador de logout...");
    }

    @Override
    public void onActivityStopped(Activity activity) {
        activityReferences--;
        if (activityReferences == 0) {
            isInBackground = true;
            Log.d("AppLifecycleManager", "No hay actividades activas. Se marcará como cerrada.");
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (activity.isFinishing()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                registerUserLogout(user);
            }
        }
        isActivityChangingConfigurations = activity.isChangingConfigurations();
    }


    @Override
    public void onTrimMemory(int level) {
        if (level == TRIM_MEMORY_UI_HIDDEN) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                registerUserLogout(user);
            }
            Log.d("AppLifecycleManager", "Logout registrado al minimizar la aplicación.");
        }
    }

    private void registerUserLogout(FirebaseUser user) {
        if(isLogging){
            isLogging = false;
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = sdf.format(System.currentTimeMillis());
        String logoutLog = formattedDate;
        IMDbDatabaseHelper dbHelper = new IMDbDatabaseHelper(appContext);
        dbHelper.actualizarLogoutRegistro(user.getUid(), logoutLog);

        SharedPreferences preferences = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("last_logout", logoutLog);
        editor.apply();

        Log.d("AppLifecycleManager", "Logout registrado para el usuario: " + user.getEmail());
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {

    }

    @Override
    public void onLowMemory() {

    }
}
