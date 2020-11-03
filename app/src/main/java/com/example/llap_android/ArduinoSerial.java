package com.example.llap_android;
import android.renderscript.ScriptGroup;
import android.serialport.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.example.llap_android.Video.StringLogger;
public class ArduinoSerial {
    private String device;
    private String saved_file;
    private SerialPort serialPort;
    private int baud;
    private static int DEFAULT_BAUD = 115200;
    private InputStream dataInput;
    private OutputStream dataOutput;
    private StringLogger logger;
    private Thread backGroundThread;
    private onSerialDataAvailable available;
    private enum STATE {
        INITIALIZED,
        STARTING,
        STARTED,
        STOPPING,
        STOPPED,
    }
    private STATE state;
    public ArduinoSerial(String _device,String _saved_file,int baud) throws IOException {
        device=_device;
        saved_file=_saved_file;
        serialPort = new SerialPort(new File(device),baud);
        dataInput=serialPort.getInputStream();
        dataOutput=serialPort.getOutputStream();
        logger=new StringLogger(saved_file);
        backGroundThread=new Thread(new Runnable() {
            @Override
            public void run() {
                if (state!=STATE.STARTING){
                    return;
                }
                state=STATE.STARTED;
                byte[] serialRead = new byte[15];
                while (state==STATE.STARTED) {
                    try {
                        SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
                        String s=time.format(new Date());
                        int read = dataInput.read(serialRead);
                        String str = new String(serialRead).substring(0,read);
                        String[] split_str = str.split(",");
                        try {
                            if (split_str.length == 3 && available != null) {
                                String strip = str.replaceAll("\r|\n","");
                                logger.log(strip+","+s+"\n");
                                split_str=strip.split(",");
                                int i1 = new Integer(split_str[0]);
                                int i2 = new Integer(split_str[1]);
                                int i3 = new Integer(split_str[2]);
                                available.onAvailable(new int[]{i1, i2, i3});
                                System.out.println("Data Available " + strip);
                            }
                        }catch (Exception e){
                            System.out.println("Error String:"+ str+","+split_str[0]+","+split_str[1]+","+split_str[2]);
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    logger.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        state = STATE.INITIALIZED;
    }
    interface onSerialDataAvailable{
        void onAvailable(int[] data);
    }
    public void setOnSerialDataAvailable(onSerialDataAvailable avail){
        available=avail;
    }
    public void start(){
        state=STATE.STARTING;
        backGroundThread.start();
    }
    public void stop(){
        state=STATE.STOPPING;
        try {
            backGroundThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
