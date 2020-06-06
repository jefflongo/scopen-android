package com.example.scopen;

/**
 * This class records the configurations for the gain of the AFE.
 */
public class GainParameters {
  /**
   * Gain settings for each voltage division.
   * Each gain value is corresponding to a voltage division.
   * Those values are determined by the Scopen hardware and firmware.
   */
  public static double[] GAINS = {18, 9, 3.6, 1.8, 1.587, 0.7199997, 0.3599951, 0.1058, 0.036, 0.024};

  /**
   * Lookup the gain of the input voltage index.
   * @param index Voltage level index
   * @return  Return the gain in double.
   */
  public static double getGain(int index) {
    return GAINS[index];
  }
}
