package com.example.llap_android.Video;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Camera2Provider {
    private Activity mContext;
    private String mCameraId;
    private List<Surface> mSurfaces;
    private Handler mCameraHandler;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mBuilder;
    private static int FLASH_MODE = CaptureRequest.FLASH_MODE_TORCH;
    //private Range<Integer> mFPSAvailable;
    public Handler getmCameraHandler(){
        return mCameraHandler;
    }
    public Camera2Provider(Activity mContext) {
        this.mContext = mContext;
        HandlerThread handlerThread = new HandlerThread("camera");
        handlerThread.start();
        mCameraHandler = new Handler(handlerThread.getLooper());
        mSurfaces = new ArrayList<Surface>();

    }
    public void addSurface(Surface s) {
        mSurfaces.add(s);
    }
    public void openCamera() throws CameraAccessException {
        openCamera(CameraCharacteristics.LENS_FACING_BACK);
    }
    public void openCamera(Integer facing) throws CameraAccessException {
        String[] params = new String[]{Manifest.permission.CAMERA};
        while (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mContext, new String[]{Manifest.permission.CAMERA}, 1);
        }
        CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        for (String cameraId : cameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Integer cameraFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
/*
            Range<Integer>[] fpsRanges;
            fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            mFPSAvailable = fpsRanges[9];
 */
            if (cameraFacing != null && cameraFacing == facing) {
                mCameraId = cameraId;
                break;
            }
        }

        cameraManager.openCamera(mCameraId, mStateCallback, mCameraHandler);
    }

    /**
     * 状态回调
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            try {
                mBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                mBuilder.set(CaptureRequest.FLASH_MODE,FLASH_MODE);
       //         mBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,mFPSAvailable);
                for (Surface s:mSurfaces) {
                    mBuilder.addTarget(s);
                }
                mCameraDevice.createCaptureSession(mSurfaces, mCaptureStateCallBack, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
        }
    };

    private CameraCaptureSession.StateCallback mCaptureStateCallBack = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            CaptureRequest request = mBuilder.build();
            try {
                session.setRepeatingRequest(request, null, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {}
    };
    /**
     * 记得关掉Camera
     */
    public void closeCamera() {
        mCameraDevice.close();
    }
}