package com.example.scopen;

/**
 * This class records the configurations for the gain of the AFE.
 */
public class GainParameters {
  private int currentIndex = 6;
  /**
   * Gain settings for each voltage division.
   * Each gain value is corresponding to a voltage division.
   * Those values are determined by the Scopen hardware and firmware.
   */
  public static double[] GAINS = {1, 1, 1, 1, 1, 0.36,0.18, 0.09, 0.036, 0.024};
  public static double[] VOLTDIV = {0.01,0.02,0.05,0.1,0.2,0.5,1.0,2.0,50.0,100.0};
  public static String[] VOLTLABEL = {"10mV","20mV","50mV","100mV","200mV","500mV","1V","2V","50V","100V"};

  /**
   * Lookup the gain of the input voltage index.
   * @param index Voltage level index
   * @return  Return the gain in double.
   */
  public static double getGain(int index) {
    return GAINS[index];
  }

  public int getIndex(){
    return currentIndex;
  }

  public double getCurrentGain(){
    return GAINS[currentIndex];
  }

  public double getVoltDiv(){
    return VOLTDIV[currentIndex];
  }

  public String getVoltDivLabel() {
    return VOLTLABEL[currentIndex];
  }
  public String incVoltDiv(){
    if(currentIndex < 9){
      currentIndex++;
    }
    return VOLTLABEL[currentIndex];
  }

  public String decVoltDiv(){
    if(currentIndex>0){
      currentIndex--;
    }
    return VOLTLABEL[currentIndex];
  }

  public void setCurrentIndex(int index) {
    if(index>0 && index < 9){
      currentIndex = index;
    }
  }
}
