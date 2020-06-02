package com.example.scopen;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements OnChartValueSelectedListener {

    private static final int MAX_SAMPLES = 50;
    private static final int BG_COLOR = 0xFF47474E;
    private static final int LINE_COLOR = 0xFFFFFACD;

    private boolean mRunning = false;

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

        Button button = (Button) findViewById(R.id.runStopButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRunStop();
            }
        });

        // Configure graph
        mChart = (LineChart) findViewById(R.id.chart);
        mChart.setHardwareAccelerationEnabled(true);
        mChart.setBackgroundColor(BG_COLOR);
        mChart.setDescription(null);
        mChart.setAutoScaleMinMaxEnabled(false);
        mChart.setPinchZoom(false);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(false);
        mChart.setTouchEnabled(false);
        mChart.setOnChartValueSelectedListener(this);

        // Disable legend
        Legend legend = mChart.getLegend();
        legend.setEnabled(false);

        // Configure y-axis right
        mChart.getAxisRight().setEnabled(false);

        // Configure y-axis left
        YAxis yAxisLeft = mChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setDrawAxisLine(false);
        yAxisLeft.setDrawLabels(true);
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
                if (mRunning) {
                    // TODO: there's probably a better choice than 0 here..
                    addEntry(intent.getFloatExtra(Constants.BROADCAST_VOLTAGE, 0));
                }
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(Constants.BROADCAST_INTENT));

        mChart.invalidate();
    }

    @Override
    protected void onPause() {
        mRunning = false;
        stopService(new Intent(this, DataService.class));
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
        mRunning = true;
        startService(new Intent(this, DataService.class));
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Log.i(Constants.TAG, "Selected (" + e.getX() + ", " + e.getY() + ")");
        TextView cursorTextView = findViewById(R.id.cursorTextView);
        cursorTextView.setVisibility(View.VISIBLE);
        String display = String.format(java.util.Locale.US, "%.3f", e.getY()) + "V";
        cursorTextView.setText(display);
    }

    @Override
    public void onNothingSelected() {
        Log.i(Constants.TAG, "Deselected data point");
        TextView cursorTextView = findViewById(R.id.cursorTextView);
        cursorTextView.setText("");
    }

    private void addEntry(final float v) {
        if (mDataSet.getEntryCount() == MAX_SAMPLES) {
            mDataSet.removeFirst();
            for (Entry entry : mDataSet.getValues()) {
                entry.setX(entry.getX() - mTimeStep);
            }
        }
        mDataSet.addEntry(new Entry(mDataSet.getEntryCount() * mTimeStep, v));
        Log.d(Constants.TAG, "\nEntries:");
        for (Entry e : mDataSet.getValues()) {
            Log.d(Constants.TAG, "(" + e.getX() + ", " + e.getY() + ")");
        }

        mChart.notifyDataSetChanged();
        mChart.invalidate();
    }

    private void onRunStop() {
        mRunning = !mRunning;
        TextView cursorTextView = findViewById(R.id.cursorTextView);
        if (mRunning) {
            cursorTextView.setVisibility(View.GONE);
            mDataSet.setDrawHighlightIndicators(false);
            mChart.setTouchEnabled(false);
        } else {
            cursorTextView.setText("");
            cursorTextView.setVisibility(View.VISIBLE);
            mDataSet.setDrawHighlightIndicators(true);
            mChart.setTouchEnabled(true);
        }
    }
}
