package com.example.scopen;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;

import org.w3c.dom.Text;

public class SideMenu {
    private boolean scanMenuOn = false;
    private boolean isConnected = false;
    private boolean samplingOn = false;

    private final Button connect;
    private final Button startScan;

    private final ImageButton startSample;

    private final MainActivity mainActivity;

    private final TextView scanResult;
//    private final TextView timeLabel;
//    private final TextView voltLabel;

    private final TextSwitcher voltLabelSwitcher;
    private final TextSwitcher timeLabelSwitcher;

    private ScopenInfo scopenInfo;

    private final Animation animUpIn;
    private final Animation animUpOut;


    private final Animation animDownOut;
    private final Animation animDownIn;

    private final ConstraintLayout sideMenuStatic;
    private final ConstraintLayout mainMenu;

    public static float convertDpToPixel(float dp, Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public SideMenu(final Activity activity){
        //Main menu buttons
        mainActivity = (MainActivity) activity;
        //Button to open network menu
        ImageButton searchButton = activity.findViewById(R.id.search);
        //searchButton.setColorFilter(Color.WHITE);
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
//        voltLabel = activity.findViewById(R.id.voltLabel);
//        timeLabel = activity.findViewById(R.id.timeLabel);
        //start network scan for scopen
        startScan = activity.findViewById(R.id.startScan);
        //connect button
        connect  = activity.findViewById(R.id.connect);
        connect.setVisibility(View.INVISIBLE);
        //displays scopen we found
        scanResult = activity.findViewById(R.id.scanResult);

//        timeLabel.setText(mainActivity.sampleParameters.getTimeDivLabel());
//        voltLabel.setText(mainActivity.gainParameters.getVoltDivLabel());
        mainMenu = activity.findViewById(R.id.MainMenu);
        final ConstraintLayout networkMenu = activity.findViewById(R.id.networkMenu);
        //animations for networkMenu
        final ValueAnimator networkMenuOpen = ValueAnimator.ofInt(
                networkMenu.getWidth(),
                (int)convertDpToPixel(150, mainActivity));
        networkMenuOpen.setDuration(300);
        networkMenuOpen.setInterpolator(new DecelerateInterpolator());
        networkMenuOpen.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                networkMenu.getLayoutParams().width = (int) animation.getAnimatedValue();
                networkMenu.requestLayout();
            }
        });
        final ValueAnimator networkMenuClose = ValueAnimator.ofInt(
                (int)convertDpToPixel(150, mainActivity), 
                0);
        networkMenuClose.setDuration(300);
        networkMenuClose.setInterpolator(new DecelerateInterpolator());
        networkMenuClose.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                networkMenu.getLayoutParams().width = (int) animation.getAnimatedValue();
                networkMenu.requestLayout();
                if((int)animation.getAnimatedValue() < 2){
                    mainMenu.setBackground(activity.getDrawable(R.drawable.network_menu_rounded));
                }
            }
        });

        voltLabelSwitcher = activity.findViewById(R.id.voltLabelSwitcher);
        timeLabelSwitcher = activity.findViewById(R.id.timeLabelSwitcher);

        timeLabelSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                TextView t  = new TextView(mainActivity);
                t.setGravity(Gravity.CENTER_HORIZONTAL);
                t.setTextSize(18);
                t.setTextColor(Color.WHITE);
                return t;
            }
        });

        voltLabelSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                TextView t  = new TextView(mainActivity);
                t.setGravity(Gravity.CENTER_HORIZONTAL);
                t.setTextSize(18);
                t.setTextColor(Color.WHITE);
                return t;
            }
        });

        timeLabelSwitcher.setText(mainActivity.sampleParameters.getTimeDivLabel());
        voltLabelSwitcher.setText(mainActivity.gainParameters.getVoltDivLabel());

        animUpOut = AnimationUtils.loadAnimation(activity,
                R.anim.slide_up_out);
        animUpIn = AnimationUtils.loadAnimation(activity,
                R.anim.slide_up);
        animDownOut = AnimationUtils.loadAnimation(activity,
                R.anim.slide_down_out);
        animDownIn = AnimationUtils.loadAnimation(activity,
                R.anim.slide_down);

        animDownIn.setDuration(200);
        animDownOut.setDuration(200);
        animUpIn.setDuration(200);
        animUpOut.setDuration(200);


        searchButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                if (!scanMenuOn) {
                    mainMenu.setBackgroundColor(Color.BLACK);
                    networkMenuOpen.start();
                } else {
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
                    //onStartSetup();
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
                connect.setText("Connecting...");
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
        timeLabelSwitcher.setOutAnimation(animUpOut);
        timeLabelSwitcher.setInAnimation(animUpIn);
        timeLabelSwitcher.setText(mainActivity.sampleParameters.incTimeDiv());
        mainActivity.mDataService.setSampleParametersIndex(mainActivity.sampleParameters.getIndex());
        mainActivity.mCommService.updateTimeDiv(mainActivity.sampleParameters.getSpeedLevel(),
                mainActivity.sampleParameters.getSampleLength());
        mainActivity.updateChartTimeDiv();

    }

    public void decTimeDivLabel(){
        timeLabelSwitcher.setOutAnimation(animDownOut);
        timeLabelSwitcher.setInAnimation(animDownIn);
        timeLabelSwitcher.setText(mainActivity.sampleParameters.decTimeDiv());
        mainActivity.mDataService.setSampleParametersIndex(mainActivity.sampleParameters.getIndex());
        mainActivity.mCommService.updateTimeDiv(mainActivity.sampleParameters.getSpeedLevel(),
                mainActivity.sampleParameters.getSampleLength());
        mainActivity.updateChartTimeDiv();

    }

    public void decVoltDivLabel(){
        voltLabelSwitcher.setOutAnimation(animDownOut);
        voltLabelSwitcher.setInAnimation(animDownIn);
        voltLabelSwitcher.setText(mainActivity.gainParameters.decVoltDiv());
        mainActivity.mDataService.setGainParametersIndex(mainActivity.gainParameters.getIndex());
        mainActivity.mCommService.updateVoltDiv(mainActivity.gainParameters.getIndex());
        mainActivity.updateChartVoltDiv();
    }

    public void incVoltDivLabel(){
        voltLabelSwitcher.setOutAnimation(animUpOut);
        voltLabelSwitcher.setInAnimation(animUpIn);
        voltLabelSwitcher.setText(mainActivity.gainParameters.incVoltDiv());
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
