package com.example.scopen;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Random;

public class DataService extends Service {
    private boolean mShouldRunThread = true;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Constants.TAG, "Service created");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mShouldRunThread) {
                    broadcastData(Constants.BROADCAST_VOLTAGE, new Random().nextFloat() * 10);
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

    @Override
    public void onDestroy() {
        Log.d(Constants.TAG, "Service destroyed");
        mShouldRunThread = false;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    private void broadcastData(final String key, float value) {
        Intent intent = new Intent(Constants.BROADCAST_INTENT);
        intent.putExtra(key, value);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
}
