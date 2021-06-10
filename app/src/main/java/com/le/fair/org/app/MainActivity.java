package com.le.fair.org.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.View;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.UnsupportedEncodingException;

import pl.droidsonroids.gif.GifImageView;

import static com.le.fair.org.app.ConnectionService.BroadcastStringForAction;

public class MainActivity extends AppCompatActivity {

    private IntentFilter intentFilter;
    boolean connected;


    int timer;
    boolean stopTimer;
    protected GifImageView loading, myInternetStatus;
    private FirebaseRemoteConfig myFirebaseRemoteConfig;
    static String main;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fullScreen();
        main = getResources().getString(R.string.salo);
        loading = findViewById(R.id.load);
        myInternetStatus = findViewById(R.id.no_signal);

        intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastStringForAction);
        Intent intent = new Intent(this, ConnectionService.class);
        startService(intent);
        if (isOnline(getApplicationContext()))
            startApp();
        else showConnectionMessage();

    }

    @Override
    public void onResume() {
        fullScreen();
        registerReceiver(broadcastReceiver, intentFilter);
        if (isOnline(getApplicationContext()))
            startApp();
        else showConnectionMessage();
        super.onResume();
    }

    private void fullScreen() {
        View v = findViewById(R.id.loading_screen);
        v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void loadingProcess() {
        timer = 0;
        stopTimer = false;
        final Handler handler = new Handler();
        final int delay = 1000;
        loading.setVisibility(View.VISIBLE);
        handler.postDelayed(new Runnable() {
            public void run() {
                if (!stopTimer) {
                    timer++;
                    if (timer >= 6) {
                        stopTimer = true;
                        loading.setVisibility(View.GONE);
                        MainActivity.this.startActivity(new Intent(MainActivity.this, WebViewActivity.class));
                    }
                    handler.postDelayed(this, delay);
                }
            }
        }, delay);
    }

    public boolean isOnline(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info != null && info.isConnectedOrConnecting()) return true;
        else return false;
    }


    public static String dc(String str) {
        String text = "";
        byte[] data = Base64.decode(str, Base64.DEFAULT);
        try {
            text = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return text;
    }


    public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BroadcastStringForAction)) {
                if (intent.getStringExtra("online_status").equals("true"))
                    startApp();
                else showConnectionMessage();
            }
        }
    };

    void showConnectionMessage() {
        myInternetStatus.setVisibility(View.VISIBLE);
        connected = false;
    }

    void startApp() {
        if (!connected) {
            myInternetStatus.setVisibility(View.GONE);
            myFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
            FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(2600)
                    .build();
            myFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
            myFirebaseRemoteConfig.setDefaultsAsync(R.xml.mdpnp);
            myFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                @Override
                public void onComplete(@NonNull Task<Boolean> task) {
                    if (myFirebaseRemoteConfig.getString("salo").contains("salo")) {
                        main = dc(main);
                    } else {
                        main = myFirebaseRemoteConfig.getString("salo");
                    }
                }
            });

            loadingProcess();
            connected = true;
        }
    }

    @Override
    protected void onRestart() {
        registerReceiver(broadcastReceiver, intentFilter);
        fullScreen();
        super.onRestart();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(broadcastReceiver);
        fullScreen();
        super.onPause();
    }
}