package com.example.llap_android;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
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
import android.os.Bundle;

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
    private StringLogger mDLogger;
    private VideoRecord mRecord;
    private Handler updateviews;

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
        mChartView = new ChartView(lineChart, "BP", Color.BLUE);
        mChartView.setDescription("");
        LineChart ppgChart = (LineChart) findViewById(R.id.chart_ppg);
        mPPGView = new ChartView(ppgChart,"PPG",Color.GREEN);
        mPPGView.setDescription("");

        ImageAuxiliaries.init(this);
        updateviews = new Handler(getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg)
            {
                if(msg.what== 0)
                {
                    if(isCalibrated) {
                        texDistance_x.setText(String.format("x=%04.2f", disx / 20) + "cm");
                        texDistance_y.setText(String.format("y=%04.2f", disy / 20) + "cm");
                        int chart_max,chart_min;
                        int idisx = ((int)disx / 20);
                        chart_max = idisx + 5;
                        chart_min = idisx > 5 ? idisx - 5 : 0;
                        mChartView.setYAxis(chart_max, chart_min,10);
                        mChartView.addEntry(disx / 20);
                    }
                    else
                    {texDistance_x.setText("Calibrating...");
                        texDistance_y.setText("");

                    }
                    mylog("count" + tracecount);
                    mytrace.setTrace(trace_x, trace_y, tracecount);
                    tracecount=0;
                }
                return false;
            };
        });
        btnPlayRecord.setOnClickListener(new OnClickListener()
        {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onClick(View v)
            {
                String currentDate = new SimpleDateFormat("MM-dd-mm:ss", Locale.getDefault()).format(new Date());
                String fprefix = Objects.requireNonNull(mActivity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)).getAbsolutePath()+ File.separator+currentDate;
                try {
                    mVLogger = new StringLogger(fprefix+"-vlog.txt");
                    mDLogger = new StringLogger(fprefix+"-dlog.txt");
                    mRecord = new VideoRecord(mActivity);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                mRecord.setOnImageWrittenCallback(new VideoRecord.OnImageWritten() {
                    @Override
                    public void callback(Image image) {

                        String s = SystemClock.uptimeMillis()+"\n";
                        ImageAuxiliaries imgaux = ImageAuxiliaries.getInstance();
                        double gavg = 0; //gavg is the average of green channel
                        try {
                            gavg = imgaux.averageGreen(image);
                            //Problem occured here
                            float min = gavg > 10 ? (int) (gavg - 10) : 0;
                            float max = (float) (gavg + 10);
                            mPPGView.setYAxis((float)max, (float)min,10);
                            mPPGView.addEntry(0.0);
                            //Problem end

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            mVLogger.log(gavg+" "+s);
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




                playBufSize = AudioTrack.getMinBufferSize(sampleRateInHz,
                        channelConfig, encodingBitrate);

                recBufSize = AudioRecord.getMinBufferSize(sampleRateInHz,
                        channelConfig, encodingBitrate);
                mylog( "recbuffersize:" + recBufSize);
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        sampleRateInHz, channelConfig, encodingBitrate, recBufSize);

                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    mylog("Record Initialized");
                }
                mylog("channels:" + audioRecord.getChannelConfiguration());

                new ThreadInstantPlay().start();
                new ThreadInstantRecord().start();
            //    new ThreadSocket().start();
            }
        });
        btnStopRecord.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                btnPlayRecord.setEnabled(true);
                btnStopRecord.setEnabled(false);
                blnPlayRecord=false;
                isCalibrated=false;
                try{
                    datastream.close();
                    datasocket.close();
                }catch (Exception e) {
                    //TODOL handle this
                }
                mRecord.stop();
                try {
                    mVLogger.close();
                    mDLogger.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }
    private void mylog(String information)
    {
        if(logenabled)
        {

            Log.i(sysname,information);
        }
    }

    class ThreadInstantPlay extends Thread
    {
        @Override
        public void run()
        {
            SoundPlayer Player= new SoundPlayer(sampleRateInHz,numfreq,wavefreqs);
            blnPlayRecord=true;
            Player.play();
            while (blnPlayRecord==true){}
            Player.stop();
        }
    }

    class ThreadInstantRecord extends Thread {

        //private short [] bsRecord = new short[recBufSize];
        //

        @Override
        public void run() {
            short[] bsRecord = new short[recBufSize * 2];
            byte[] networkbuf = new byte[recBufSize * 4];
            int datacount = 0;
            int curpos = 0;
            long starttime,endtime;
            String c_result;

            while (blnPlayRecord == false) {
            }
            try {
                audioRecord.startRecording();
            }
            catch (Exception e){
                String a = e.toString();
                mylog(e.toString());
            }
            /*
             *
             */
            while (blnPlayRecord) {
                /*
                 *
                 */
                int line = audioRecord.read(bsRecord, 0, frameSize * 2);
                datacount = datacount + line / 2;
                now=now+1;

                mylog("recevied data:" + line + " at time" + System.currentTimeMillis());
                if (line >= frameSize) {

                    //get baseband


                    starttime=System.currentTimeMillis();
                    mylog(AudioDistance.getbaseband(bsRecord, baseband, line / 2));
                    endtime=System.currentTimeMillis();

                    mylog("time used forbaseband:"+(endtime-starttime));

                    starttime=System.currentTimeMillis();
                    mylog( AudioDistance.removedc(baseband, baseband_nodc, dcvalue));
                    endtime=System.currentTimeMillis();

                    mylog("time used LEVD:"+(endtime-starttime));

                    starttime=System.currentTimeMillis();
                    mylog( AudioDistance.getdistance(baseband_nodc, phasechange, dischange, freqpower));
                    endtime=System.currentTimeMillis();

                    mylog("time used distance:"+(endtime-starttime));


                    if(!isCalibrated&&Math.abs(dischange[0])<0.05&&now-lastcalibration>10) {


                        c_result=AudioDistance.calibrate(baseband);
                        mylog(c_result) ;
                        lastcalibration=now;
                        if(c_result.equals("calibrate OK")){
                            isCalibrated=true;
                        }

                    }
                    if(isCalibrated) {
                        starttime = System.currentTimeMillis();
                        mylog(AudioDistance.getidftdistance(baseband_nodc, idftdis));
                        endtime = System.currentTimeMillis();

                        mylog("time used idftdistance:" + (endtime - starttime));

                        //keep difference stable;

                        double disdiff,dissum;
                        disdiff=dischange[0]-dischange[1];
                        dissum=dischange[0]+dischange[1];
                        dischangehist=dischangehist*0.5+disdiff*0.5;
                        dischange[0]=(dissum+dischangehist)/2;
                        dischange[1]=(dissum-dischangehist)/2;

                        disx=disx+dischange[0];
                        if(disx>1000)
                            disx=1000;
                        if(disx<0)
                            disx=0;
                        disy=disy+dischange[1];
                        if(disy>1000)
                            disy=1000;
                        if(disy<0)
                            disy=0;
                        if(Math.abs(dischange[0])<0.2&&Math.abs(dischange[1])<0.2&&Math.abs(idftdis[0])>0.1&&Math.abs(idftdis[1])>0.1)
                        {
                            disx=disx*(1-distrend)+idftdis[0]*distrend;
                            disy=disy*(1-distrend)+idftdis[1]*distrend;
                        }
                        if(disx<micdis1)
                            disx=micdis1;
                        if(disy<micdis2)
                            disy=micdis2;
                        if(Math.abs(disx-disy)>(micdis1+micdis2))
                        {
                            double tempsum=disx+disy;
                            if(disx>disy)
                            {
                                disx=(tempsum+micdis1+micdis2)/2;
                                disy=(tempsum-micdis1-micdis2)/2;

                            }
                            else
                            {
                                disx=(tempsum-micdis1-micdis2)/2;
                                disy=(tempsum+micdis1+micdis2)/2;
                            }
                        }
                        trace_x[tracecount]= (int) Math.round((disy*micdis1*micdis1-disx*micdis2*micdis2+disx*disy*(disy-disx))/2/(disx*micdis2+disy*micdis1));
                        trace_y[tracecount]=(int) Math.round(Math.sqrt(  Math.abs((disx*disx-micdis1*micdis1)*(disy*disy-micdis2*micdis2)*((micdis1+micdis2)*(micdis1+micdis2)-(disx-disy)*(disx-disy))  )  )/2/(disx*micdis2+disy*micdis1) );
                        System.out.println("x="+trace_x[tracecount]+"y="+trace_y[tracecount]);
                        try {
                            mDLogger.log(SystemClock.uptimeMillis()+","+"x:"+disx+",y:"+disy+"\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        tracecount++;

                    }
                    if(Math.abs(displaydis-disx)>2||(tracecount>10)) {
                        Message msg = new Message();
                        msg.what = 0;
                        displaydis=disx;
                        updateviews.sendMessage(msg);
                    }
                    if(!isCalibrated)
                    {
                        Message msg = new Message();
                        msg.what = 0;
                        updateviews.sendMessage(msg);
                    }




                    curpos = curpos + line / 2;
                    if (curpos > coscycle)
                        curpos = curpos - coscycle;
                    if(sendbaseband&&datastream != null)
                    {
                        int j=0;
                        for (int i = 0; i < 2*numfreq*2*frameSize/cicdec; i++) {
                            //sum = sum + bsRecord[i];
                            networkbuf[j++] = (byte) (((short) baseband_nodc[i]) & 0xFF);
                            networkbuf[j++] = (byte) (((short) baseband_nodc[i]) >> 8);
                        }
                        //Log.i("wavedemo", "data sum:" + sum);

                        if (datastream != null) {
                            try {
                                datastream.write(networkbuf, 0, j);
                                mylog( "socket write" + j);
                            } catch (Exception e) {
                                // TODO: handle this
                                mylog( "socket error" + e);
                            }
                        }

                    }

                    if (sendDatatoMatlab&&datastream != null) {
                        int j = 0;
                        int sum = 0;
                        for (int i = 0; i < line; i++) {
                            //sum = sum + bsRecord[i];
                            networkbuf[j++] = (byte) (bsRecord[i] & 0xFF);
                            networkbuf[j++] = (byte) (bsRecord[i] >> 8);
                        }
                        //Log.i("wavedemo", "data sum:" + sum);

                        if (datastream != null) {
                            try {
                                datastream.write(networkbuf, 0, j);
                                mylog( "socket write" + j);
                            } catch (Exception e) {
                                // TODO: handle this
                                mylog("socket error" + e);
                            }
                        }
                    }
                }
                mylog("endtime" + System.currentTimeMillis());

            }
            audioRecord.stop();

        }
    }


}