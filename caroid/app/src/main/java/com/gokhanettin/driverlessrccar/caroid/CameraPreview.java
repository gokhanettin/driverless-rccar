package com.gokhanettin.driverlessrccar.caroid;


import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by gokhanettin on 01.04.2017.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Camera.Size mPreviewSize;
    private final LinkedList<byte[]> mQueue = new LinkedList<>();
    private static final int MAX_QUEUE_SIZE = 15;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        mHolder = getHolder();

        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        Camera.Size smallest = sizes.get(0);
        for (Camera.Size s : sizes) {
            Log.d(TAG, "Supported preview size = " + s.width + ", " + s.height);
            if (smallest.width > s.width) {
                smallest = s;
            }
        }

        params.setPreviewSize(smallest.width, smallest.height); // Smaller is better
        mCamera.setParameters(params);

        mPreviewSize = mCamera.getParameters().getPreviewSize();
        Log.d(TAG, "Preview size = " + mPreviewSize.width + ", " + mPreviewSize.height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "Error setting camera preview: ", e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
            clearQueue();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewCallback(mPreviewCallback);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.e(TAG, "Error starting camera preview: ", e);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void setCamera(Camera camera) {
        if (camera == null) {
            mCamera.setPreviewCallback(null);
            mHolder.removeCallback(this);
        } else {
            mHolder.addCallback(this);
            Camera.Parameters params = camera.getParameters();
            params.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            camera.setParameters(params);
        }
        mCamera = camera;
    }

    public byte[] getPreview() {
        byte[] preview = null;
        synchronized (mQueue) {
            if (mQueue.size() > 0) {
                preview = mQueue.poll();
            }
        }
        return preview;
    }

    public int getPreviewWidth() {
        return mPreviewSize.width;
    }

    public int getPreviewHeight() {
        return mPreviewSize.height;
    }

    private void clearQueue() {
        synchronized (mQueue) {
            mQueue.clear();
        }
    }

    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            // TODO Auto-generated method stub
            if (data.length >= getPreviewWidth() * getPreviewHeight()) {
                synchronized (mQueue) {
                    if (mQueue.size() == MAX_QUEUE_SIZE) {
                        mQueue.poll();
                    }
                    mQueue.add(data);
                }
            }
        }
    };
}
