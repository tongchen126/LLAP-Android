package com.example.llap_android;

import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import com.example.llap_android.Video.StringLogger;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class DataRecordAndBpCalculate {
    enum __EVENT {
      EVENT_ACC,
      EVENT_PPG
    };
    private TextView mTextView;
    private Boolean mRunning;
    private Boolean mCalculating;
    private StringLogger mAccLogger;
    private StringLogger mPpgLogger;
    private Thread mThread;
    private int num;
    private String mPrefix;
    private Handler mTimerHandler;
    static private int calculate_bp(String ppgfile,String accfile){
        String cmd = "chroot /sdcard/mnt /bin/bash /root/workspace/calc_bp.sh\n exit 0\n";
        String result = SuExecAndResult.Exec(cmd);
        float _result;
        try {
            _result = Float.parseFloat(result);
        }catch (NumberFormatException exception){
            exception.printStackTrace();
            _result=0;
        }
        return (int)_result;
       // return 0;
    }
    private void sendbp(final int bp){
        Message msg = new Message();
        if (bp==0){
            msg.what=1;
        }
        else{
            msg.what=0;
            msg.arg1=bp;
        }
        mTimerHandler.sendMessage(msg);
    };
    public void start(Handler handler,String prefix){
        mTimerHandler = handler;
        num = 0;
        mPrefix = prefix;
        mCalculating = false;
        try {
            mAccLogger = new StringLogger(mPrefix +  "slog.txt");
            mPpgLogger = new StringLogger(mPrefix +  "vlog.txt");

        } catch (IOException e) {
            e.printStackTrace();
        }
        mRunning = true;
        mThread = new Thread(){
            @Override
            public void run() {
                super.run();
                while (mRunning == true) {
                    for (int i = 0; i < 20; i++) {
                        if (mRunning == false)
                            return;
                        try {
                            sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (mRunning == false)
                            return;
                    }
                    mCalculating = true;
                    String ppgfile = mPpgLogger.getFilename();
                    String accfile = mAccLogger.getFilename();
                    try {
                        mPpgLogger.close();
                        mAccLogger.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    int bp = calculate_bp(ppgfile,accfile);
                    sendbp(bp);
                    num+=1;
                    try {
                        mAccLogger = new StringLogger(mPrefix +  "slog.txt");
                        mPpgLogger = new StringLogger(mPrefix +  "vlog.txt");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mCalculating = false;
                }
            }
        };
        mThread.start();
    }
    public void OnAccEvent(String data){
        if (mRunning == true && mCalculating == false){
            try {
                mAccLogger.log(data);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e){
                e.printStackTrace();
            }
        }
    }
    public void OnPPGEvent(String data){
        if (mRunning == true && mCalculating == false){
            try {
                mPpgLogger.log(data);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e){
                e.printStackTrace();
            }
        }
    }
    public void stop() {
        mRunning = false;
    }
}
