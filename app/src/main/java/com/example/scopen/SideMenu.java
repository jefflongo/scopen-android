package com.example.scopen;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

public class SideMenu {
    private boolean scanMenuOn = false;
    private boolean isConnected = false;
    private boolean samplingOn = false;

    private final Button connect;
    private final Button startScan;

    private final ImageButton startSample;

    private final MainActivity mainActivity;

    private final TextView scanResult;
    private final TextView timeLabel;
    private final TextView voltLabel;

    private ScopenInfo scopenInfo;

    private final ConstraintLayout sideMenuStatic;

    public SideMenu(Activity activity){
        //Main menu buttons
        mainActivity = (MainActivity) activity;
        //Button to open network menu
        ImageButton searchButton = activity.findViewById(R.id.search);
        //Buttons to change division settings
        ImageButton voltDivInc = activity.findViewById(R.id.voltDivInc);
        ImageButton voltDivDec = activity.findViewById(R.id.voltDivDec);
        ImageButton timeDivInc = activity.findViewById(R.id.timeDivInc);
        ImageButton timeDivDec = activity.findViewById(R.id.timeDivDec);
        //Configs startSample button when Scopen is disconnected
        startSample= activity.findViewById(R.id.runStopStart);
        startSample.animate().translationY(-150);
        sideMenuStatic = activity.findViewById(R.id.sideMenuStatic);
        sideMenuStatic.animate().translationY(-150);
        //labels for volt/div and time/div
        voltLabel = activity.findViewById(R.id.voltLabel);
        timeLabel = activity.findViewById(R.id.timeLabel);
        //start network scan for scopen
        startScan = activity.findViewById(R.id.startScan);
        //connect button
        connect  = activity.findViewById(R.id.connect);
        connect.setVisibility(View.INVISIBLE);
        //displays scopen we found
        scanResult = activity.findViewById(R.id.scanResult);

        timeLabel.setText(mainActivity.sampleParameters.getTimeDivLabel());
        voltLabel.setText(mainActivity.gainParameters.getVoltDivLabel());

        final ConstraintLayout networkMenu = activity.findViewById(R.id.networkMenu);
        //animations for networkMenu
        final ValueAnimator networkMenuOpen = ValueAnimator.ofInt(networkMenu.getWidth(), 500);
        networkMenuOpen.setDuration(300);
        networkMenuOpen.setInterpolator(new DecelerateInterpolator());
        networkMenuOpen.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                networkMenu.getLayoutParams().width = (int) animation.getAnimatedValue();
                networkMenu.requestLayout();
            }
        });
        final ValueAnimator networkMenuClose = ValueAnimator.ofInt(500, 0);
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
                    onStartSetup();
                    mainActivity.mCommService.startSampling();
                    startSample.setImageResource(android.R.drawable.ic_media_pause);
                    samplingOn = true;
                }
                mainActivity.onRunStop(samplingOn);
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
                incTimeDivLabel();
            }
        });

        timeDivDec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decTimeDivLabel();
            }
        });

        voltDivInc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                incVoltDivLabel();
            }
        });

        voltDivDec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decVoltDivLabel();
            }
        });

    }

    public void incTimeDivLabel(){
        timeLabel.setText(mainActivity.sampleParameters.incTimeDiv());
        mainActivity.mDataService.setSampleParametersIndex(mainActivity.sampleParameters.getIndex());
        mainActivity.mCommService.updateTimeDiv(mainActivity.sampleParameters.getSpeedLevel(),
                mainActivity.sampleParameters.getSampleLength());
        mainActivity.updateChartTimeDiv();

    }

    public void decTimeDivLabel(){
        timeLabel.setText(mainActivity.sampleParameters.decTimeDiv());
        mainActivity.mDataService.setSampleParametersIndex(mainActivity.sampleParameters.getIndex());
        mainActivity.mCommService.updateTimeDiv(mainActivity.sampleParameters.getSpeedLevel(),
                mainActivity.sampleParameters.getSampleLength());
        mainActivity.updateChartTimeDiv();

    }

    public void decVoltDivLabel(){
        voltLabel.setText(mainActivity.gainParameters.decVoltDiv());
        mainActivity.mDataService.setGainParametersIndex(mainActivity.gainParameters.getIndex());
        mainActivity.mCommService.updateVoltDiv(mainActivity.gainParameters.getIndex());
        mainActivity.updateChartVoltDiv();
    }

    public void incVoltDivLabel(){
        voltLabel.setText(mainActivity.gainParameters.incVoltDiv());
        mainActivity.mDataService.setGainParametersIndex(mainActivity.gainParameters.getIndex());
        mainActivity.mCommService.updateVoltDiv(mainActivity.gainParameters.getIndex());
        mainActivity.updateChartVoltDiv();
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

    private void onStartSetup(){
        try{
            Thread.sleep(500);
        }catch (InterruptedException e){

        }
        mainActivity.mCommService.updateVoltDiv(mainActivity.gainParameters.getIndex());
        try{
            Thread.sleep(500);
        }catch (InterruptedException e){

        }
        mainActivity.mCommService.updateTimeDiv(mainActivity.sampleParameters.getSpeedLevel(),
                mainActivity.sampleParameters.getSampleLength());
        try{
            Thread.sleep(500);
        }catch (InterruptedException e){

        }
        mainActivity.mDataService.setGainParametersIndex(mainActivity.gainParameters.getIndex());
        mainActivity.mDataService.setSampleParametersIndex(mainActivity.sampleParameters.getIndex());
    }

    public void updateConnectButtonState(boolean connected){
        if(connected){
            sideMenuStatic.animate().translationY(0);
            startSample.animate().translationY(0);

            startSample.setClickable(true);
            isConnected = true;
            onStartSetup();
            connect.setText("Disconnect");
        }else{
            connect.setText("Connect");
            startSample.animate().translationY(-150);
            sideMenuStatic.animate().translationY(-150);
            startSample.setClickable(true);
            isConnected = false;
        }
    }



}
