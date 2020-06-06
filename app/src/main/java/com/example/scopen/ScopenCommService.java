package com.example.scopen;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;

public class ScopenCommService extends Service {
    private final IBinder binder = new CommServiceInterfaceClass();
    public CommManager commManager;
    public CmdManager cmdManager;
    public CommEventsInterface commEventsInterface;
    public CmdEventsInterface cmdEventsInterface;

    byte ConnectionState;
    static final byte DISCONNECTED = 0x00;
    static final byte CONNECTED = 0x01;
    static final byte SCANNING = 0x02;
    static final byte STOPPED_SCAN = 0x03;

    byte Command;
    static final byte DATA = 0x00;
    static final byte BATTERY_REPORTED = 0x01;
    static final byte SWIPE_UP = 0x02;
    static final byte SWIPE_DOWN = 0x03;
    static final byte TAP = 0x04;

    byte [] scopenData;
    byte [] batteryStatus;

    @Override
    public void onCreate() {
        super.onCreate();
        initCmdEventInterface();
        initCommEventInterface();
        commManager = new CommManager(commEventsInterface);
        cmdManager = new CmdManager(commManager,cmdEventsInterface);
    }
    private void initCommEventInterface(){
        commEventsInterface = new CommEventsInterface() {
            @Override
            public void onDataReceived(byte[] data, int type) {
                cmdManager.parseReceivedMessage(data,type);
            }

            @Override
            public void onDisconnected() {
                sendConnectionState(DISCONNECTED);
            }

            @Override
            public void onConnected() {
                sendConnectionState(CONNECTED);
            }

            @Override
            public void onScanStarted() {
                sendConnectionState(SCANNING);
            }

            @Override
            public void onScanFinished() {
                sendConnectionState(STOPPED_SCAN);
            }
        };
    }
    private void initCmdEventInterface(){
        cmdEventsInterface = new CmdEventsInterface() {
            @Override
            public void onDataReceived(byte[] data) {
                sendCommand(DATA);
                scopenData = data;
            }

            @Override
            public void onBatteryReported(byte[] data) {
                sendCommand(BATTERY_REPORTED);
                batteryStatus = data;
            }

            @Override
            public void onSwipeUp(byte[] data) {
                sendCommand(SWIPE_UP);
            }

            @Override
            public void onSwipeDown(byte[] data) {
                sendCommand(SWIPE_DOWN);
            }

            @Override
            public void onChangeSelect(byte[] data) {
                sendCommand(TAP);
            }
        };
    }

    private void sendConnectionState(byte connectionState){
        Intent toActivityBroadcast = new Intent("Scopen");
        toActivityBroadcast.putExtra("ScopenConnection", connectionState);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(toActivityBroadcast);
    }

    private void sendCommand(byte command){
        Intent toActivityBroadcast = new Intent("Scopen");
        toActivityBroadcast.putExtra("ScopenCommand", command);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(toActivityBroadcast);
    }

    public int onStartCommand(Intent intent, int flags, int startId){
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    public class CommServiceInterfaceClass extends Binder {
        public byte[] getScopenData(){
            return scopenData;
        }
        public void startSampling(){cmdManager.sendStartCommand();}
        public void stopSampling(){cmdManager.sendStopCommand();}
        public byte[] getBatteryStatus(){
            return batteryStatus;
        }
        public void scanNetwork(){
            commManager.scanNetwork(2);
        }
        public ArrayList<ScopenInfo> getScopens(){
            return commManager.getAvaliableScopens();
        }
        public void connectScopen(ScopenInfo scopenInfo){
            commManager.connectScopen(scopenInfo);
        }
        public void disconnectScopen(){
            commManager.disconnectScopen();
        }
        public boolean isConnected(){
            return commManager.isConnected();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
