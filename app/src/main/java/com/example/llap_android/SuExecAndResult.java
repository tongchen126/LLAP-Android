package com.example.llap_android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SuExecAndResult {

    public static String Exec(String cmd,String su_path){
        String result = "";
        try {
            Process su;
            byte[] _result = new byte[256];
            su = Runtime.getRuntime().exec(su_path);
            OutputStream out = su.getOutputStream();
            InputStream in = su.getInputStream();
            out.write(cmd.getBytes());
            out.flush();
            su.waitFor();
            int read_num = in.read(_result);
            result = new String(_result).substring(0,read_num);
            System.out.println(result);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }
    public static String Exec(String cmd){
        return Exec(cmd,"/sbin/su");
    }
}
