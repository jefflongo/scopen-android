package com.example.scopen;

import java.util.ArrayList;

/**
 * This class handles processing the sampled data.
 */
public class SampleProcessor {

  /**
   * The numbers of ADC used. This is determined by the Scopen hardware and software design.
   */
  public static final int ADC_NUMERS = 4;

  /**
   * This function will format the raw data and put them in order.
   * The raw data sent from the ESP32 is not in order because the results are grouped per ADC.
   * @return
   */
  public static byte[] formatSamplesInOrder(byte[] data) {
    int totalLength = data.length;
    int adcSampleOffset = totalLength / ADC_NUMERS;
    byte[] orderedSamples = new byte[totalLength];
    for (int i = 0; i <= totalLength - ADC_NUMERS; i += ADC_NUMERS) {
      for (int offsets = 0; offsets < ADC_NUMERS; offsets++) {
        orderedSamples[i + offsets] = data[i/ADC_NUMERS + adcSampleOffset * offsets];
      }
    }
    System.out.println("Sample Data: ");
    for (int i = 0; i < data.length; i++) {
      System.out.println("index: " + i + " " + (data[i] & 0xff));
    }

    System.out.println("Ordered Data: ");
    for (int i = 0; i < data.length; i++) {
      System.out.println("index: " + i + " " + (orderedSamples[i] & 0xff));
    }

    return orderedSamples;
  }

  /**
   * Converted the passed in raw data to voltage values.
   * @param data  Raw data from the ADC. These data should be formated to ordered first.
   * @param gain  The gain value at this voltage index.
   * @return
   */
  public static ArrayList<Double> convertSamplesToVolt(byte[] data, double gain) {
    if (data == null) {
      System.err.println("[SAMPLE PROCESSOR] Data is null");
    }
    double v;
    final double ref = 1.8;
    ArrayList<Double> voltValues = new ArrayList<>();
    for (int i = 0; i < data.length; i++) {
      v = ((Byte.toUnsignedInt(data[i]) * 1.0 - 127) / 255.0) * (ref * 2) / gain;
      voltValues.add(v);
    }
    return voltValues;
  }
}
