package com.example.scopen;

public class SampleParameters {
    // Predefined sampling options
    public static final int MAX_TRANS_SAMPLE = 20000;
    public static final double[] SAMPLE_SPEED = {15000000.0f, 1000000.0f, 100000.0f, 10000.0f, 10000.0f};
    public static final int[] SAMPLE_LENGTH = {20000, 10000, 10000, 10000, 20000};

    public static final int SAMPLE_SPEED_HIGHEST = 0;
    public static final int SAMPLE_SPEED_HIGH = 1;
    public static final int SAMPLE_SPEED_MEDIUM = 2;
    public static final int SAMPLE_SPEED_LOW = 3;
    public static final int SAMPLE_SPEED_LOWEST = 4;

    private double sampleSpeed = SAMPLE_SPEED[SAMPLE_SPEED_HIGHEST];
    private int sampleLength = SAMPLE_LENGTH[SAMPLE_SPEED_HIGHEST];
    private int select = SAMPLE_SPEED_HIGHEST;
    private int currentIndex = 0;

    private static final int NUM_TIME_DIVS = 27;

    private final String[] timeDivOptions = {
            "10ns", "20ns", "50ns",
            "100ns", "200ns", "500ns",
            "1us", "2us", "5us",
            "10us", "20us", "50us",
            "100us", "200us", "500us",
            "1ms", "2ms", "5ms",
            "10ms", "20ms", "50ms",
            "100ms", "200ms", "500ms",
            "1s", "2s", "5s", "10s"
    };

    private final double[] timeDivValues = {
            0.000000010, 0.000000020, 0.000000050,
            0.000000100, 0.000000200, 0.000000500,
            0.000001000, 0.000002000, 0.000005000,
            0.000010000, 0.000020000, 0.000050000,
            0.000100000, 0.000200000, 0.000500000,
            0.001000000, 0.002000000, 0.005000000,
            0.010000000, 0.020000000, 0.050000000,
            0.100000000, 0.200000000, 0.500000000,
            1.000000000, 2.000000000, 10.000000000
    };

    /**
     * Constructor that returns a sample configuration.
     * @param select
     */
    public SampleParameters(int select) {
        setCurrentIndex(select);
        setSpeedLevel(lookUpSpeedLevel(select));
    }

    /**
     * Select the sampling speed level. Selecting the sampling speed will also change the data length.
     * @param select the target speed level. Ranges from 0 to 2, where 0 is the fastest and 2 is the slowest.
     */
    public void setSpeedLevel(int select) {
        this.select = select;
        sampleSpeed = SAMPLE_SPEED[select];
        sampleLength = SAMPLE_LENGTH[select];
    }

    // Getters and setters.
    /**
     * Get the sample option(index).
     * @return int value indicates the speed index.
     */
    public int getSpeedLevel() {
        return this.select;
    }

    /**
     * Get the current selected sampling speed in Hz.
     * @return
     */
    public double getSampleSpeed() {
        return sampleSpeed;
    }

    /**
     * Get the current selected sampling period in seconds.
     * @return
     */
    public double getSamplePeriod() {
        return 1 / sampleSpeed;
    }

    /**
     * Get the sampling length associated with the current sampling speed.
     * @return
     */
    public int getSampleLength() {
        return sampleLength;
    }
    public double getTimeDiv() { return timeDivValues[currentIndex];}
    /**
     * This defines what speed level should be chosen under given time division setting.
     * @param timediv time division value in seconds.
     * @return Speed level.
     */
    public static int lookUpSpeedLevel(double timediv) {
        int speedLevel = 0;
        if (timediv <= 0.00005) {               // Check if it's smaller than 50us
            speedLevel = SAMPLE_SPEED_HIGHEST;
        } else if (timediv <= 0.0005) {         // Check if the division is samller than 500us
            speedLevel = SAMPLE_SPEED_HIGH;
        } else if (timediv <= 0.005) {          // Check if the division is smaller than 5ms
            speedLevel = SAMPLE_SPEED_MEDIUM;
        } else if (timediv <= 0.05) {           // Check if the division is smaller than 50ms
            speedLevel = SAMPLE_SPEED_LOW;
        } else {
            speedLevel = SAMPLE_SPEED_LOWEST;
        }
        return speedLevel;
    }

    public String getTimeDivLabel() {
        return timeDivOptions[currentIndex];
    }

    public String incTimeDiv() {
        if(currentIndex < NUM_TIME_DIVS - 1){
            currentIndex++;
        }
        this.setSpeedLevel(lookUpSpeedLevel(timeDivValues[currentIndex]));
        return timeDivOptions[currentIndex];
    }

    public String decTimeDiv() {
        if(currentIndex > 0){
            currentIndex--;
        }
        this.setSpeedLevel(lookUpSpeedLevel(timeDivValues[currentIndex]));
        return timeDivOptions[currentIndex];
    }

    public void setCurrentIndex(int index){
        if(index >= 0 && index < NUM_TIME_DIVS) {
            currentIndex = index;
            this.setSpeedLevel(lookUpSpeedLevel(timeDivValues[currentIndex]));
        }
    }

    public int getIndex(){
        return currentIndex;
    }

}