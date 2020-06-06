package com.example.scopen;

abstract class Constants {
    static final String TAG = "DEBUG_LOG";
    static final String BROADCAST_INTENT = "ScopenData";
    static final String BROADCAST_VOLTAGE = "voltage";


    static final byte DISCONNECTED = 0x00;
    static final byte CONNECTED = 0x01;
    static final byte SCANNING = 0x02;
    static final byte STOPPED_SCAN = 0x03;


    static final byte DATA = 0x00;
    static final byte BATTERY_REPORTED = 0x01;
    static final byte SWIPE_UP = 0x02;
    static final byte SWIPE_DOWN = 0x03;
    static final byte TAP = 0x04;

    static final byte ERROR_BYTE = 0x05;

}