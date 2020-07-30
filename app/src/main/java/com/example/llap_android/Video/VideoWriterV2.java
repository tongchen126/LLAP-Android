package com.example.llap_android.Video;

import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageWriter;
import android.media.MediaRecorder;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.IOException;

public class VideoWriterV2 {
    private ImageWriter mImageWriter;
    private MediaRecorder mMediaRecorder;
    private int count = 0;
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public VideoWriterV2(String file) throws IOException {
        this(file,ImageFormat.YUV_420_888,1920,1080,30,10);
    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public VideoWriterV2(String file, int format,int width,int height,int fps,int maximages) throws IOException {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(file);
        mMediaRecorder.setVideoFrameRate(fps);
        mMediaRecorder.setVideoSize(width,height);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.prepare();
        mImageWriter = ImageWriter.newInstance(mMediaRecorder.getSurface(),maximages, format);
    }
    public void pushImage(Image img){
        System.out.println(img.getTimestamp());
        mImageWriter.queueInputImage(img);
        count += 1;
        System.out.printf("----------count:%d\n-----------",count);
    }
    void start() throws IOException {
        mMediaRecorder.start();
    }
    void stop(){
        mMediaRecorder.stop();
    }
}
