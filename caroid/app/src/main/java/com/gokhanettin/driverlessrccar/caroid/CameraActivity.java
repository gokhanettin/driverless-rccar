package com.gokhanettin.driverlessrccar.caroid;

import android.content.DialogInterface;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    private CameraPreview mCameraPreview;
    private CameraManager mCameraManager;
    private TcpClient mTcpClient = null;
    private String mIP;
    private int mPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mCameraManager = new CameraManager(this);
        // Create our Preview view and set it as the content of our activity.
        mCameraPreview = new CameraPreview(this, mCameraManager.getCamera());
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mCameraPreview);
        askForServerAddress();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTcpClient != null) {
            mTcpClient.disconnect();
        }
        mCameraManager.releaseCamera();
    }

    private void askForServerAddress() {
        LayoutInflater infilater = this.getLayoutInflater();
        final View textEntryView = infilater.inflate(R.layout.server_address, null);
        Log.d(TAG, "Asking for server address");
        AlertDialog dialog =  new AlertDialog.Builder(this)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle("Server Address")
                .setView(textEntryView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        EditText ipEdit = (EditText)textEntryView.findViewById(R.id.ip_edit);
                        EditText portEdit = (EditText)textEntryView.findViewById(R.id.port_edit);
                         //mIP = ipEdit.getText().toString();
                         //mPort = Integer.parseInt(portEdit.getText().toString());
                        mIP = "192.168.0.67";
                        mPort = 5000;
                        mTcpClient = new TcpClient(mHandler);
                        mTcpClient.connect(mIP, mPort);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        finish();
                    }
                })
                .create();
        dialog.show();
    }

    private final Handler.Callback mHandlerCallback = new TcpHandlerCallback() {
        @Override
        protected void onConnectionStateChanged(int newState) {
            String state = "";
            switch (newState) {
                case TcpClient.STATE_NONE:
                    state = "STATE_NONE";
                    break;
                case TcpClient.STATE_CONNECTING:
                    state = "STATE_CONNECTING";
                    break;
                case TcpClient.STATE_CONNECTED:
                    state = "STATE_CONNECTED";
                    break;
            }
            Log.d(TAG, "onConnectionStateChanged: " + state);
        }

        @Override
        protected void onReceived(int speedCmd, int steeringCmd) {

        }

        @Override
        protected void onSent(int speedCmd, int steeringCmd,
                              float speed, float steering, byte[] frame) {
            Log.d(TAG, "onSent() size: " + frame.length);
        }

        @Override
        protected void onConnectionEstablished(String serverAddress) {
            new CountDownTimer(120001, 50) {

                public void onTick(long millisUntilFinished) {
                    mTcpClient.send(1500, 1500, 1.0f, 2.1f, mCameraPreview);
                    Log.d(TAG, "onTick()");
                }

                public void onFinish() {
                    Log.d(TAG, "Timer onFinish()");
                    cancel();
                    finish();
                }
            }.start();
        }

        @Override
        protected void onConnectionError(String error) {

        }
    };

    private final Handler mHandler = new Handler(mHandlerCallback);
}
