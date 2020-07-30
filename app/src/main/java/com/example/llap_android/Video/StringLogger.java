package com.example.llap_android.Video;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;

public class StringLogger {
    private FileWriter mOutput;
    public StringLogger(String file) throws IOException {
        File _f = new File(file);
        if (_f.exists()){
            _f.delete();
        }
        mOutput = new FileWriter(file,false);
    }
    public void log(String data) throws IOException {
        mOutput.write(data);
    }
    public void close() throws IOException {
        mOutput.close();
    }
}
