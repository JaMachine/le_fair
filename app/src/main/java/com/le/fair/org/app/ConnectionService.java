package com.le.fair.org.app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;

public class ConnectionService  extends Service {
    public static String status = "status";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.post(networkStateUpdate);
        return START_STICKY;
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info != null && info.isConnectedOrConnecting()) return true;
        else return false;
    }

    Handler handler = new Handler();
    private Runnable networkStateUpdate = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(networkStateUpdate, 333);
            Intent intent = new Intent();
            intent.setAction(status);
            intent.putExtra("online_status", "" + isOnline(ConnectionService.this));
            sendBroadcast(intent);
        }
    };
}
