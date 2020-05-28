package com.example.scopen;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SCOPEN";

    private static final int MAX_SAMPLES = 100;
    private static final int BG_COLOR = 0xFF47474E;
    private static final int LINE_COLOR = 0xFFFFFACD;

    private float mTimeStep = 0.5f;
    private float mMaxY = 10;
    private float mMinY = 0;

    private LineChart mChart;
    private LineDataSet mDataSet;
    private LineData mLineData;

    private BroadcastReceiver mMessageReceiver;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        // Configure graph
        mChart = (LineChart) findViewById(R.id.chart);
        mChart.setHardwareAccelerationEnabled(true);
        mChart.setBackgroundColor(BG_COLOR);
        mChart.setDescription(null);
        mChart.setAutoScaleMinMaxEnabled(false);
        mChart.setPinchZoom(false);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setTouchEnabled(false);

        // Disable legend
        Legend legend = mChart.getLegend();
        legend.setEnabled(false);

        // Configure y-axis right
        mChart.getAxisRight().setEnabled(false);

        // Configure y-axis left
        YAxis yAxisLeft = mChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setDrawAxisLine(false);
        yAxisLeft.setDrawLabels(false);
        yAxisLeft.setAxisMinimum(mMinY);
        yAxisLeft.setAxisMaximum(mMaxY);
        //yAxisLeft.enableGridDashedLine(10, 0, 0);

        // Configure x-axis
        XAxis xAxis = mChart.getXAxis();
        xAxis.setDrawGridLines(true);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawLabels(false);
        xAxis.setAxisMinimum(0);
        xAxis.setAxisMaximum(mTimeStep * (MAX_SAMPLES - 1));
        //xAxis.enableGridDashedLine(10, 10, 0);

        // Configure data
        mDataSet = new LineDataSet(null, null);
        mDataSet.setDrawValues(false);
        mDataSet.setColor(LINE_COLOR);
        mDataSet.setCircleColor(LINE_COLOR);
        mDataSet.setLineWidth(3f);
        mDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        mLineData = new LineData(mDataSet);
        mChart.setData(mLineData);

        mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO: there's probably a better choice than 0 here..
                addEntry(intent.getIntExtra("voltage", 0));
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("ScopenData"));

        mChart.invalidate();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopService(new Intent(this, DataService.class));
    }

    protected void onResume() {
        super.onResume();
        startService(new Intent(this, DataService.class));
    }

    public void addEntry(final float v) {
        if (mDataSet.getEntryCount() == MAX_SAMPLES) {
            mDataSet.removeFirst();
            for (Entry entry : mDataSet.getValues()) {
                entry.setX(entry.getX() - mTimeStep);
            }
        }
        mDataSet.addEntry(new Entry(mDataSet.getEntryCount() * mTimeStep, v));

        mChart.notifyDataSetChanged();
        mChart.invalidate();
    }
}
