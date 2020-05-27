package com.example.scopen;

/**
 * This interface is meant for handler the data receive event while a Scopen transmits command or data to the client.
 */
public interface CommEventsInterface {
  void onDataReceived(byte[] data,byte type);
  void onDisconnected();
  void onConnected();
  void onScanStarted();
  void onScanFinished();
}
