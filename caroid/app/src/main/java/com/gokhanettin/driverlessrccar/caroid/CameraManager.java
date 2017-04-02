package com.gokhanettin.driverlessrccar.caroid;

import android.hardware.Camera;
import android.util.Log;

/**
 * Created by gokhanettin on 01.04.2017.
 */

public class CameraManager {
    public static final String TAG = "CameraManager";
    private Camera mCamera;

    public CameraManager() {
        // Create an instance of Camera
        mCamera = getCameraInstance();
    }

    public Camera getCamera() {
        if (mCamera != null) {
            return mCamera;
        }
        mCamera = getCameraInstance();
        return mCamera;
    }

    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.release(); // release the camera for other applications
            mCamera = null;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    private static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            Log.e(TAG, "Failed to open camera", e);
        }
        return c; // returns null if camera is unavailable
    }
}
