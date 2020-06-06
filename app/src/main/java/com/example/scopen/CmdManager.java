package com.example.scopen;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CmdManager {

  /**
   * Command type constants - Pen to Software
   */
  // Format [length:4, type:1][Data bytes]
  public static final byte CMD_DATA = 0x00;
  // Format [length:4, type:1][battery percent: 1]
  public static final byte CMD_REPORT_BAT = 0x01;
  // Format [length:4, type:1][0xFF]
  public static final byte CMD_SWIPE_UP = 0x11;
  // Format [length:4, type:1][0xFF]
  public static final byte CMD_SWIPE_DOWN = 0x12;
  // Format [length:4, type:1][0xFF]
  public static final byte CMD_CHANGE_SEL = 0x13;

  /**
   * Command type constants - Software to Pen
   */
  // Format [length:4, type:1][0xFF]
  public static final byte CMD_START_SAMPLE = 0x21;
  // Format [length:4, type:1][0xFF]
  public static final byte CMD_STOP_SAMPLE = 0x22;
  // Format [length:4, type:1][0xFF]
  public static final byte CMD_CHECK_BAT = 0x23;
  // Format [length:4, type:1][Voltage Div Index: 1]
  public static final byte CMD_SET_VOLTAGE = 0x41;
  // Format [length:4, type:1][Sampling Speed Index: 1][Buffer length: 4]
  public static final byte CMD_SET_SAMPLE_PARAS = 0x42;

  private CommManager commManager;
  private CmdEventsInterface cmdCallbacksHandler;

  public CmdManager(CommManager commManager, CmdEventsInterface cmdCallbacksHandler) {
    this.commManager = commManager;
    this.cmdCallbacksHandler = cmdCallbacksHandler;
  }

  /**
   * Static methods
   */
  public static boolean verifyCommandType(int type) {
    switch (type & 0xff) {
      case CMD_DATA:
      case CMD_REPORT_BAT:
      case CMD_SWIPE_DOWN:
      case CMD_SWIPE_UP:
      case CMD_CHANGE_SEL:
        return true;
    }
    return false;
  }

  /**
   * Sending method
   */
  public void sendStartCommand() {
    System.out.println("Sending Start");
    byte[] buffer = {(byte)0xFF};
    commManager.transmitCommand(buffer, CMD_START_SAMPLE);
  }

  public void sendStopCommand() {
    System.out.println("Sending Stop");
    byte[] buffer = {(byte)0xFF};
    commManager.transmitCommand(buffer, CMD_STOP_SAMPLE);
  }

  public void sendCheckBattry() {
    System.out.println("Sending Check Battery");
    byte[] buffer = {(byte)0xFF};
    commManager.transmitCommand(buffer, CMD_CHECK_BAT);
  }

  public void sendSetVoltCommand(int gainIndex) {
    System.out.println("Sending set voltage");
    byte[] buffer = ByteBuffer.allocate(1).order(ByteOrder.BIG_ENDIAN).put((byte)gainIndex).array();
    commManager.transmitCommand(buffer, CMD_SET_VOLTAGE);
  }

  public void sendSetSampleParasCommand(int speedOption, int bufferLength) {
    System.out.println("Sending set sample");
    byte[] buffer = ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN).put((byte)speedOption).putInt(bufferLength).array();
    commManager.transmitCommand(buffer, CMD_SET_SAMPLE_PARAS);
  }

  /**
   * Receiving hanlder
   */
  public void parseReceivedMessage(byte[] data, int type) {
    switch (0xFF & type) {
      case CMD_DATA:
        cmdCallbacksHandler.onDataReceived(data);
        break;
      case CMD_REPORT_BAT:
        cmdCallbacksHandler.onBatteryReported(data);
        break;
      case CMD_SWIPE_UP:
        cmdCallbacksHandler.onSwipeUp(data);
        break;
      case CMD_SWIPE_DOWN:
        cmdCallbacksHandler.onSwipeDown(data);
        break;
      case CMD_CHANGE_SEL:
        cmdCallbacksHandler.onChangeSelect(data);
        break;
      default:
        System.out.println("Unsupported command received.");
    }
  }


}
