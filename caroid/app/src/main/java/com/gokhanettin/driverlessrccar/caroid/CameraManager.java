package com.gokhanettin.driverlessrccar.caroid;

import android.content.Context;
import android.hardware.Camera;
import android.widget.Toast;

/**
 * Created by gokhanettin on 01.04.2017.
 */

public class CameraManager {
    private Camera mCamera;
    private Context mContext;


    public CameraManager(Context context) {
        mContext = context;
        // Create an instance of Camera
        mCamera = getCameraInstance();
    }

    public Camera getCamera() {
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
        }
        return c; // returns null if camera is unavailable
    }
}
