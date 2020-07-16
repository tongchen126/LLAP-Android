package com.example.llap_android;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.provider.MediaStore;
import android.renderscript.Sampler;
import android.util.Log;
import java.io.OutputStreamWriter;
/**
 * Created by weiwang on 1/23/16.
 */
public class SoundPlayer {
    private AudioTrack audioTrack;

    private int sampleRate = 48000;
    private int numSamples=19200;
    private int numfreq=16;
    private double sample[] = new double[numSamples];

    private double freqOfTone[]= {17150,17500,17850,18200,18550,18900,19250,19600,19950,20300,20650,21000,21350,21700,22050,22400};

    private byte generatedSound[] = new byte[2 * numSamples];

    SoundPlayer(int setsamplerate, int setnumfreq, double setfreqs[]) {


        sampleRate=setsamplerate;
        numfreq=setnumfreq;
        for (int i=0;i<numfreq;i++)
        {
            freqOfTone[i]=setfreqs[i];
        }
        //STREAM_MUSIC  STREAM_VOICE_CALL
        audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSound.length, AudioTrack.MODE_STATIC);

        PrepareSound();
    }


    public void PrepareSound() {
        genTone();

        Log.d("llap", "" +audioTrack.write(generatedSound, 0, generatedSound.length));
        audioTrack.setLoopPoints(0, generatedSound.length / 2, -1);
    }

    public void play() {
        //16 bit because it's supported by all phones
        audioTrack.play();
    }
    public void pause() {
        audioTrack.pause();
    }

    public void stop() {
        audioTrack.stop();
        audioTrack.release();
    }

    void genTone(){
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {

            sample[i]=0;
              for(int j=0;j<numfreq;j++) {
                sample[i] =sample[i]+ Math.cos(2 * Math.PI * i / (sampleRate / freqOfTone[j]));
            }
            sample[i]=sample[i]/numfreq;
        }


        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 30000));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSound[idx++] = (byte) (val & 0x00ff);
            generatedSound[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

}
