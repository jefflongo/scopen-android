package com.example.scopen;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class DataService extends Service {
    private boolean mShouldRunThread = true;
    private boolean mProcessorThread = true;
    private LocalBroadcastManager mScopenServiceBroadcast;
    private ScopenReciever reciever;
    byte [] rawData;
    double [] processedData;
    private SampleParameters sampleParameters = new SampleParameters(15);
    private GainParameters gainParameters = new GainParameters();
    private Semaphore lockData = new Semaphore(1);
    private final IBinder binder = new DataServiceInterfaceClass();
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mScopenServiceBroadcast = LocalBroadcastManager.getInstance(this);
        reciever = new ScopenReciever(this);
        mScopenServiceBroadcast.registerReceiver(reciever, new IntentFilter("DataPlotter"));
        Log.d(Constants.TAG, "Service created");
        Thread dataPlotter = new Thread(new DataProcessor(),"data");
        dataPlotter.start();
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (mShouldRunThread) {
//                    broadcastData(Constants.BROADCAST_VOLTAGE, new Random().nextFloat() * 10);
//                    try {
//                        Thread.sleep(30);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });

        //thread.start();
    }
    class ScopenReciever extends BroadcastReceiver {
        Service service;
        public ScopenReciever(Service service){
            this.service = (DataService) service;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.hasExtra("Data")){
                rawData = intent.getByteArrayExtra("Data");

            }
        }
    }

    private class DataProcessor implements Runnable{
        private void processRawData(){
             if(rawData!=null) {

                rawData = SampleProcessor.formatSamplesInOrder(rawData);

                processedData = SampleProcessor.convertSamplesToVolt(rawData, gainParameters.getCurrentGain());
                int currentWindowSize = (int) Math.ceil(10 * sampleParameters.getTimeDiv() / sampleParameters.getSamplePeriod());
                int startindex = processedData.length/2 - currentWindowSize/2;
                int endIndex =  processedData.length/2 + currentWindowSize/2;
                for(int i = startindex; i<endIndex; i++) {
                    broadcastData(Constants.BROADCAST_VOLTAGE, (float) processedData[i]);
                    lockData.acquireUninterruptibly();
                }

            }
        }
        public void run(){
            while(mProcessorThread) {
                processRawData();
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(Constants.TAG, "Service destroyed");
        mScopenServiceBroadcast.unregisterReceiver(reciever);
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

    public class DataServiceInterfaceClass extends Binder {
        public void setSampleParametersIndex(int index){sampleParameters.setCurrentIndex(index);}
        public void setGainParametersIndex(int index){gainParameters.setCurrentIndex(index);}
        public void finishedPlot() {lockData.release();}
    }
}
