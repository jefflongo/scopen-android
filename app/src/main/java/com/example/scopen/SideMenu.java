package com.example.scopen;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

public class SideMenu {
    boolean scanMenuOn = false;
    boolean isConnected = false;
    Button connect;
    MainActivity mainActivity;
    TextView scanResult;

    ScopenInfo scopenInfo;

    public SideMenu(Activity activity){
        //Main menu buttons
        mainActivity = (MainActivity) activity;
        ImageButton startSample = activity.findViewById(R.id.runStopStart);
        ImageButton searchButton = activity.findViewById(R.id.search);
        ImageButton voltDivInc = activity.findViewById(R.id.voltDivInc);
        ImageButton voltDivDec = activity.findViewById(R.id.voltDivDec);
        ImageButton timeDivInc = activity.findViewById(R.id.timeDivInc);
        ImageButton timeDivDec = activity.findViewById(R.id.timeDivDec);

        Button startScan = activity.findViewById(R.id.startScan);

        connect  = activity.findViewById(R.id.connect);
        scanResult = activity.findViewById(R.id.scanResult);
        connect.setVisibility(View.INVISIBLE);
        final ConstraintLayout networkMenu = activity.findViewById(R.id.networkMenu);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewGroup.LayoutParams layoutParams = networkMenu.getLayoutParams();
                if(!scanMenuOn) {
                    layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                else {
                    layoutParams.width = 0;
                }
                scanMenuOn = !scanMenuOn;
                networkMenu.setLayoutParams(layoutParams);
            }
        });

        startScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        startSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.mCommService.startSampling();
            }
        });

        startScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.mCommService.scanNetwork();
            }
        });

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isConnected) {
                    mainActivity.mCommService.connectScopen(scopenInfo);
                }else{
                    mainActivity.mCommService.disconnectScopen();
                }
            }
        });
    }

    public void setScopenScanResult(ScopenInfo scopenInfo){
        this.scopenInfo = scopenInfo;
        scanResult.setText("Scopen" + scopenInfo.getVersion());
        connect.setVisibility(View.VISIBLE);
    }

    public void updateConnectButtonState(boolean connected){
        if(connected){
            connect.setText("Disconnect");
            isConnected = true;
        }else{
            connect.setText("Connect");
            isConnected = false;
        }
    }
}
