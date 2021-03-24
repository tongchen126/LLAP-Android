package com.example.llap_android;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;
import android.widget.Button;
import android.widget.Toolbar;
import android.widget.ProgressBar;
import android.view.View.OnClickListener;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.widget.TextView;

import com.example.llap_android.Video.ImageAuxiliaries;
import com.example.llap_android.Video.StringLogger;
import com.example.llap_android.Video.VideoRecord;

import com.github.mikephil.charting.charts.LineChart;

public class MainActivity extends AppCompatActivity {

    private Button btnPlayRecord;

    private Button btnStopRecord;

    private TextView texDistance_x;

    private TextView texDistance_y;


    private TraceView mytrace;

    private int recBufSize = 0;

    private int frameSize = 512;

    private double disx,disy;

    private double displaydis=0;

    private String sysname= "llap";

    private AudioRecord audioRecord;


    private double temperature =20;

    private double freqinter = 350;

    private int numfreq= 16;

    private double[] wavefreqs=new double[numfreq];

    private double[] wavelength=new double[numfreq];

    private double[] phasechange= new double [numfreq*2];

    private double[] freqpower= new double [numfreq*2];

    private double[] dischange = new double [2];

    private double[] idftdis = new double [2];

    private double startfreq=15050;//17150

    private double soundspeed = 0;

    private int playBufSize;

    private boolean sendDatatoMatlab =false;
    private boolean sendbaseband = false;
    private boolean logenabled = true;
    /**
     */
    private boolean blnPlayRecord = false;

    private int coscycle=1920;

    /**
     */
    //private int sampleRateInHz = 44100;
    private int sampleRateInHz = 48000;

    /**
     */
    //private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int channelConfig = AudioFormat.CHANNEL_IN_STEREO;

    /**
     */
    private int encodingBitrate = AudioFormat.ENCODING_PCM_16BIT;

    private int cicdec=16;
    private int cicsec=3;
    private int cicdelay=cicdec*17;


    private double [] baseband=new double[2*numfreq*2*frameSize/cicdec];

    private double [] baseband_nodc=new double[2*numfreq*2*frameSize/cicdec];

    private short [] dcvalue=new short[4*numfreq];


    private int [] trace_x=new int[1000];
    private int [] trace_y=new int[1000];
    private int tracecount=0;


    private boolean isCalibrated=false;
    private int now;
    private int lastcalibration;

    private double distrend=0.05;

    private double micdis1=5;
    private double micdis2=115;
    private double dischangehist=0;

    /**
     */



    private Socket datasocket;
    private OutputStream datastream;

    private ChartView mChartView;
    private ChartView mPPGView;
    private Activity mActivity;
    private StringLogger mVLogger;
    //private StringLogger mDLogger;
    private VideoRecord mRecord;
    private Handler updateviews;

    //Elapsed Time showing
    private Handler mTimerHandler;
    private Timer mTimer;
    private TimerTask mTimerTask;
    private TextView mTimerText;
    private MotionSensorRecord motionSensorRecord;
    private StringLogger mSLogger;
    private TextView mBPText;
    DataRecordAndBpCalculate mBP;
    private Handler mBPHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO}, 1);
        btnPlayRecord = (Button)findViewById(R.id.button);
        btnStopRecord = (Button)findViewById(R.id.button2);
        texDistance_x =(TextView) findViewById(R.id.textView);
        texDistance_y =(TextView) findViewById(R.id.textView2);
        mytrace = (TraceView) findViewById(R.id.trace);
        soundspeed =331.3 + 0.606 *temperature;


        for(int i=0;i<numfreq;i++)
        {
            wavefreqs[i]=startfreq+i*freqinter;
            wavelength[i]=soundspeed/wavefreqs[i]*1000;
        }
        mActivity = this;

        disx=0;
        disy=250;
        now=0;
        lastcalibration=0;

        tracecount=0;
        mylog("initialization start at time: " + System.currentTimeMillis());
        mylog( AudioDistance.initdownconvert(sampleRateInHz, numfreq, wavefreqs));

        mylog("initialization finished at time: " + System.currentTimeMillis());

        LineChart lineChart = (LineChart) findViewById(R.id.chart);
        mChartView = new ChartView(lineChart, "ACC", Color.BLUE);
        mChartView.setDescription("");
        LineChart ppgChart = (LineChart) findViewById(R.id.chart_ppg);
        mPPGView = new ChartView(ppgChart,"PPG",Color.RED);
        mPPGView.setDescription("");
        mBPText = (TextView)findViewById(R.id.textView3);
        ImageAuxiliaries.init(this);
        updateviews = new Handler(getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg)
            {
                return false;
            };
        });
        mTimerText = (TextView)findViewById(R.id.textView4);
        mTimerHandler=new Handler(getMainLooper(), new Handler.Callback() {
            Date sdate;
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                Date date = new Date();
                if (msg.what==0) {
                    sdate = new Date();
                    long delta = date.getTime()-sdate.getTime();
                    mTimerText.setText(Long.toString(delta));
                }
                if (msg.what == 1){
                    long delta = (date.getTime()-sdate.getTime())/1000;
                    mTimerText.setText(Long.toString(delta));
                }
                return false;
            }
        });
        mBPHandler =new Handler(getMainLooper(), new Handler.Callback() {
            Date sdate;
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                if (msg.what==0) {
                    mBPText.setText(Integer.toString(msg.arg1));
                }
                else
                    mBPText.setText("---");
                return false;
            }
        });
        btnPlayRecord.setOnClickListener(new OnClickListener()
        {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onClick(View v)
            {
         //       String cmd = "chroot /sdcard/mnt /bin/bash /root/workspace/calc_bp.sh && exit 0\n";
        //        String result = SuExecAndResult.Exec(cmd);
                mBP = new DataRecordAndBpCalculate();
                String currentDate = new SimpleDateFormat("MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(new Date());
                String _fprefix = Objects.requireNonNull(mActivity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)).getAbsolutePath()+ File.separator;
                String fprefix = _fprefix+currentDate;
                motionSensorRecord = new MotionSensorRecord((SensorManager) getSystemService(SENSOR_SERVICE), Sensor.TYPE_LINEAR_ACCELERATION);
                try {
                    mVLogger = new StringLogger(fprefix+"-vlog.txt");
                 //   mDLogger = new StringLogger(fprefix+"-dlog.txt");
                    mSLogger = new StringLogger(fprefix+"-slog.txt");

                    mRecord = new VideoRecord(mActivity);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                motionSensorRecord.registeronSensorChangedEvent(new MotionSensorRecord.onSensorChangedEvent() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
                        String s=time.format(new Date());
                        try {
//                            mSLogger.log(s + "," + event.values[0] + "," + event.values[1] + "," + event.values[2]+"\n");
                            mSLogger.log(s + "," + event.values[1] + "\n");
                            mBP.OnAccEvent(s + "," + event.values[1] + "\n");
                            float y_acc = event.values[1];
                //            mChartView.setYAxis(5, -5,10);
                 //           mChartView.addEntry(y_acc);
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {

                    }
                });
                mRecord.setOnImageWrittenCallback(new VideoRecord.OnImageWritten() {
                    int i = 0;
                    @Override
                    public void callback(Image image,Date date) {
               //         String s = SystemClock.uptimeMillis()+"\n";
                        SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
                        String s=time.format(date);
                        ImageAuxiliaries imgaux = ImageAuxiliaries.getInstance();
                        double gavg = 0; //gavg is the average of green channel
                        try {
                            gavg = imgaux.averageGreen(image);
                            float min = (float)(gavg - 10);
                            float max = (float) (gavg + 10);
                       //     mPPGView.setYAxis((float)max, (float)min,10);
                       //     mPPGView.addEntry(gavg);
                            i+=1;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            mVLogger.log(s+","+gavg+"\n");
                            mBP.OnPPGEvent(s+","+gavg+"\n");
                        }
                        catch (IOException e){
                            e.printStackTrace();
                        }




                    }
                });
                try {
                    mRecord.start();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                btnPlayRecord.setEnabled(false);
                btnStopRecord.setEnabled(true);







                mTimer = new Timer();

                mTimerTask = new TimerTask() {
                    int first=0;
                    @Override
                    public void run() {
                        Message msg = new Message();
                        msg.what=first==0?0:1;
                        first+=1;
                        mTimerHandler.sendMessage(msg);
                    }
                };
                mTimer.schedule(mTimerTask,0,500);
                mBP.start(mBPHandler,_fprefix);
            //    new ThreadSocket().start();
            }
        });
        btnStopRecord.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                motionSensorRecord.unregisteronSensorChangedEvent();
                btnPlayRecord.setEnabled(true);
                btnStopRecord.setEnabled(false);
                blnPlayRecord=false;
                isCalibrated=false;

                mRecord.stop();
                try {
                    mVLogger.close();
                 //   mDLogger.close();
                    mSLogger.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mTimer.cancel();
                mBP.stop();
            }
        });

    }
    private void mylog(String information)
    {
        if(logenabled)
        {
        }
    }

    class ThreadInstantPlay extends Thread
    {
        @Override
        public void run()
        {
        }
    }

    class ThreadInstantRecord extends Thread {

        //private short [] bsRecord = new short[recBufSize];
        //

        @Override
        public void run() {

        }
    }


}