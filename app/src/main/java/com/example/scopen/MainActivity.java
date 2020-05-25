package com.example.scopen;

import android.annotation.SuppressLint;
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

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int MAX_SAMPLES = 10;
    private static final int BG_COLOR = 0xFF47474E;
    private static final int LINE_COLOR = 0xFFFFFACD;

    private int mTimeStep = 1;

    private LineChart mChart;
    private LineDataSet mDataSet;
    private LineData mLineData;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
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

        // Configure y-axis left
        YAxis yAxisLeft = mChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setDrawAxisLine(false);
        yAxisLeft.setDrawLabels(true);
        //yAxisLeft.enableGridDashedLine(10, 0, 0);

        // Configure y-axis right
        YAxis yAxisRight = mChart.getAxisRight();
        yAxisRight.setDrawGridLines(true);
        yAxisRight.setDrawAxisLine(false);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setEnabled(false);
        //yAxisRight.enableGridDashedLine(10, 10, 0);

        // Configure x-axis
        XAxis xAxis = mChart.getXAxis();
        xAxis.setDrawGridLines(true);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawLabels(false);
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
        mChart.invalidate();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    addEntry(mLineData.getEntryCount() * mTimeStep, new Random().nextInt(10));
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

    public void addEntry(final float t, final float v) {
        if (mDataSet.getEntryCount() == MAX_SAMPLES) {
            mDataSet.removeFirst();
            for (Entry entry : mDataSet.getValues()) {
                entry.setX(entry.getX() - mTimeStep);
            }
        }
        mDataSet.addEntry(new Entry(t, v));
        mDataSet.notifyDataSetChanged();
        mLineData.notifyDataChanged();
        mChart.notifyDataSetChanged();
        mChart.invalidate();
    }
}
