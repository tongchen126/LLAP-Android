package com.example.llap_android.Video;

import android.graphics.ImageFormat;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageWriter;
import android.media.MediaRecorder;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.IOException;

public class VideoWriterV2 {
    private MediaRecorder mMediaRecorder;
    private int count = 0;
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public VideoWriterV2(String file) throws IOException {
        this(file,ImageFormat.YUV_420_888,1920,1080,30,10);
    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public VideoWriterV2(String file, int format,int width,int height,int fps,int maximages) throws IOException {
        mMediaRecorder = new MediaRecorder();
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(file);
        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth,profile.videoFrameHeight);
        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.prepare();
    }
    public MediaRecorder getmMediaRecorderr(){
        return mMediaRecorder;
    }
    void start() throws IOException {
        mMediaRecorder.start();
    }
    void stop(){
        mMediaRecorder.stop();
    }
}
