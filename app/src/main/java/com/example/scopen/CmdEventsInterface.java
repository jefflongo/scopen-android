package com.example.scopen;

public interface CmdEventsInterface {
  void onDataReceived(byte[] data);
  void onBatteryReported(byte[] data);
  void onSwipeUp(byte[] data);
  void onSwipeDown(byte[] data);
  void onChangeSelect(byte[] data);
}
