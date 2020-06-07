package com.example.scopen;

import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.media.Image;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

public class SideMenu {
    boolean scanMenuOn = false;
    boolean isConnected = false;
    boolean samplingOn = false;
    Button connect;
    ImageButton startSample;
    MainActivity mainActivity;
    TextView scanResult;
    ScopenInfo scopenInfo;
    ConstraintLayout sideMenuStatic;
    final Button startScan;
    public SideMenu(Activity activity){
        //Main menu buttons
        mainActivity = (MainActivity) activity;

        startSample= activity.findViewById(R.id.runStopStart);
        startSample.animate().translationY(-150);
        sideMenuStatic = activity.findViewById(R.id.sideMenuStatic);
        sideMenuStatic.animate().translationY(-150);
        ImageButton searchButton = activity.findViewById(R.id.search);
        ImageButton voltDivInc = activity.findViewById(R.id.voltDivInc);
        ImageButton voltDivDec = activity.findViewById(R.id.voltDivDec);
        ImageButton timeDivInc = activity.findViewById(R.id.timeDivInc);
        ImageButton timeDivDec = activity.findViewById(R.id.timeDivDec);
        final TextView voltLabel = activity.findViewById(R.id.voltLabel);
        startScan = activity.findViewById(R.id.startScan);
        connect  = activity.findViewById(R.id.connect);
        scanResult = activity.findViewById(R.id.scanResult);
        connect.setVisibility(View.INVISIBLE);
        final ConstraintLayout networkMenu = activity.findViewById(R.id.networkMenu);
        final ValueAnimator networkMenuOpen = ValueAnimator.ofInt(networkMenu.getWidth(), 400);
        networkMenuOpen.setDuration(300);
        networkMenuOpen.setInterpolator(new DecelerateInterpolator());
        networkMenuOpen.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                networkMenu.getLayoutParams().width = (int) animation.getAnimatedValue();
                networkMenu.requestLayout();
            }
        });
        final ValueAnimator networkMenuClose = ValueAnimator.ofInt(400, 0);
        networkMenuClose.setDuration(300);
        networkMenuClose.setInterpolator(new DecelerateInterpolator());
        networkMenuClose.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                networkMenu.getLayoutParams().width = (int) animation.getAnimatedValue();
                networkMenu.requestLayout();
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!scanMenuOn) {
                    networkMenuOpen.start();
                }
                else {
                    networkMenuClose.start();
                }
                scanMenuOn = !scanMenuOn;
            }
        });


        startSample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(samplingOn){
                    mainActivity.mCommService.stopSampling();
                    startSample.setImageResource(android.R.drawable.ic_media_play);
                    samplingOn = false;
                }else{
                    mainActivity.mCommService.startSampling();
                    startSample.setImageResource(android.R.drawable.ic_media_pause);
                    samplingOn = true;
                }
            }
        });

        startScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.mCommService.scanNetwork();
                startScan.setText("Scanning...");
                startScan.setClickable(false);
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

        timeDivInc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mainActivity.mCommService.;
            }
        });

        voltDivInc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                voltLabel.setText(mainActivity.gainParameters.incVoltDiv());
                mainActivity.mCommService.updateVoltDiv(mainActivity.gainParameters.getIndex());
            }
        });
        voltDivDec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                voltLabel.setText(mainActivity.gainParameters.decVoltDiv());
                mainActivity.mCommService.updateVoltDiv(mainActivity.gainParameters.getIndex());
            }
        });

    }

    public void setScopenScanResult(ScopenInfo scopenInfo){
        this.scopenInfo = scopenInfo;
        scanResult.setText("Scopen" + scopenInfo.getVersion());
        connect.setVisibility(View.VISIBLE);
    }
    public void updateScanButton(){
        startScan.setText("Scan");
        startScan.setClickable(true);
    }

    public void updateConnectButtonState(boolean connected){
        if(connected){
            connect.setText("Disconnect");
            sideMenuStatic.animate().translationY(0);
            startSample.animate().translationY(0);

            startSample.setClickable(true);
            isConnected = true;
        }else{
            connect.setText("Connect");
            startSample.animate().translationY(-150);
            sideMenuStatic.animate().translationY(-150);
            startSample.setClickable(true);
            isConnected = false;
        }
    }


}
