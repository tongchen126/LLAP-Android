package com.example.llap_android.Video;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class VideoRecord {
    public static final int DEFAULT_WIDTH = 1280;
    public static final int DEFAULT_HEIGHT = 720;
    public static final int DEFAULT_FPS = 30;
    public static final int DEFAULT_MAXIMAGES = 30;
    public static final int DEFAULT_SKIPPED_IMAGES = 60;
    public static final int DEFAULT_FACING = CameraCharacteristics.LENS_FACING_FRONT;
    private Camera2Provider mCamera;
    private ImageReader mImageReader;
    private OnImageWritten mCB;
    private StringLogger mLogger;
    private VideoWriterV2 mWriter;
    private VIDEORECORD_STATE mState;
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        private int i = 0;
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            Date date = new Date();
            if (image == null)
                return;
            if (i <= DEFAULT_SKIPPED_IMAGES){
                i+=1;
                image.close();
                return;
            }
            synchronized (mState) {
                if (mState != VIDEORECORD_STATE.STATE_STARTED) {
                    image.close();
                    return;
                }
            }
            try {
                mWriter.pushImage(image);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            /*
            image=null;
            try {
                if (mCB != null)
                    mCB.callback(image,date);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            */
        }
    };
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public VideoRecord(Activity context,String file) throws IOException {
        this(file,context,DEFAULT_WIDTH,DEFAULT_HEIGHT,DEFAULT_FPS);
    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public VideoRecord(String file,Activity context, int width, int height, int fps) throws IOException {
        mCamera = new Camera2Provider(context);
        mImageReader = ImageReader.newInstance(width,height,ImageFormat.YUV_420_888,DEFAULT_MAXIMAGES);
        mWriter = new VideoWriterV2(file,ImageFormat.YUV_420_888,width,height,fps,DEFAULT_MAXIMAGES);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener,mCamera.getmCameraHandler());
        mCamera.addSurface(mImageReader.getSurface());
        mState = VIDEORECORD_STATE.STATE_INITED;
    };
    public boolean start() throws CameraAccessException, IOException {
        synchronized (mState) {
            if (mState != VIDEORECORD_STATE.STATE_INITED)
                return false;
            mCamera.openCamera(DEFAULT_FACING);
            mWriter.start();
            mState = VIDEORECORD_STATE.STATE_STARTED;
        }
        return true;
    }
    public void stop(){
        synchronized (mState) {
            if (mState == VIDEORECORD_STATE.STATE_STARTED) {
                mCamera.closeCamera();
                mWriter.stop();
                mState = VIDEORECORD_STATE.STATE_STOPPED;
            }
        }
    }
    public void setOnImageWrittenCallback(OnImageWritten cb){
        mCB = cb;
    }
    public interface OnImageWritten{
        void callback(Image image, Date date);
    };
    private enum VIDEORECORD_STATE {
        STATE_INITED,
        STATE_STARTED,
        STATE_STOPPED,
    }
}
