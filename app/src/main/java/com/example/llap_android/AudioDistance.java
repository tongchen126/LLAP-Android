package com.example.llap_android;

public class AudioDistance {
    static {
        System.loadLibrary("audiodistance");
    }
    public static native String getbaseband(short[] data, double[] outdata, int numdata);

    //C implementation of LEVD

    public static native String removedc(double[] data, double[] data_nodc, short[] outdata);

    //C implementation of distance

    public static native String getdistance(double[] data, double[] outdata, double [] distance, double [] freqpower);

    // Initialize C down converter

    public static native String initdownconvert(int samplerate, int numfreq, double [] wavfreqs);

    public static native String getidftdistance(double[] data, double[] outdata);

    public static native String calibrate(double[] data);
}
