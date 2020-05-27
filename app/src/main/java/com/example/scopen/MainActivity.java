package com.example.scopen;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.IBinder;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
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

    static final byte ERROR_BYTE = 0x05;

    LocalBroadcastManager lbc;
    ScopenReciever reciever;
    TextView textView;TextView textView2;
    CommService.CommServiceInterfaceClass mCommService;
    boolean mBound = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        textView = findViewById(R.id.textView);
        textView2 = findViewById(R.id.textView2);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mCommService.scanNetwork();
            }
        });

        lbc = LocalBroadcastManager.getInstance(this);
        reciever = new ScopenReciever(this);
        lbc.registerReceiver(reciever, new IntentFilter("Scopen"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    class ScopenReciever extends BroadcastReceiver{
        MainActivity mActivity;
        public ScopenReciever(Activity activity){mActivity = (MainActivity) activity;}
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.hasExtra("ScopenConnection")){
                mActivity.processScopenConnectionState(intent.getByteExtra("ScopenConnection",ERROR_BYTE));
            }
            else if(intent.hasExtra("ScopenCommand")){
                mActivity.processScopenCommand(intent.getByteExtra("ScopenCommand", ERROR_BYTE));
            }
        }
    }

    public void processScopenCommand(byte command){
        switch (command){
            case DATA:
                break;
            case BATTERY_REPORTED:
                break;
            case SWIPE_UP:
                break;
            case SWIPE_DOWN:
                break;
            case TAP:
                break;
            default:
                break;
        }
    }

    public void processScopenConnectionState(byte connectionState){
        switch(connectionState){
            case DISCONNECTED:
                break;
            case CONNECTED:
                textView2.setText("Connected");
                break;
            case SCANNING:
                textView.setText("Scanning");
                break;
            case STOPPED_SCAN:
                if(!mCommService.getScopens().isEmpty()){
                    mCommService.connectScopen(mCommService.getScopens().get(0));
                    textView.setText("Stopped Scan");
                }
                break;
            default:
                break;
        }
    }
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mCommService = (CommService.CommServiceInterfaceClass) service;
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, CommService.class);
        getApplicationContext().bindService(intent,connection,Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
        mBound = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lbc.unregisterReceiver(reciever);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
