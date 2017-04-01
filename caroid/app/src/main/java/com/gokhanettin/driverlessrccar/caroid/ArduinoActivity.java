package com.gokhanettin.driverlessrccar.caroid;

import android.content.Intent;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class ArduinoActivity extends AppCompatActivity {
    public static final String TAG = "ArduinoActivity";
    private static final int REQUEST_BT_ADDRESS = 0;

    private static final int SPEED_CMD_MIN = 1000;
    private static final int SPEED_CMD_MAX = 2000;
    private static final int SPEED_CMD_STEP = 10;
    private static final int STEERING_CMD_MIN = 1000;
    private static final int STEERING_CMD_MAX = 2000;
    private static final int STEERING_CMD_STEP = 10;

    private BluetoothClient mBluetoothClient = null;

    private ConstraintLayout mConstraintLayoutTx;
    private CheckBox mCheckBoxTx;
    private TextView mTextViewSpeedCmd;
    private TextView mTextViewSteeringCmd;
    private TextView mTextViewSpeed;
    private TextView mTextViewSteering;
    private SeekBar mSeekBarSpeedCmd;
    private SeekBar mSeekBarSteeringCmd;

    private int mSendSpeedCmd = 0;
    private int mSendSteeringCmd = 0;
    private int mReceiveSpeedCmd = 0;
    private int mReceiveSteeringCmd = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arduino);

        mConstraintLayoutTx = (ConstraintLayout) findViewById(R.id.constraint_layout_tx);
        mCheckBoxTx = (CheckBox) findViewById(R.id.check_box_tx);
        mTextViewSpeedCmd = (TextView) findViewById(R.id.text_view_speed_cmd);
        mTextViewSteeringCmd = (TextView) findViewById(R.id.text_view_steering_cmd);
        mTextViewSpeed = (TextView) findViewById(R.id.text_view_speed);
        mTextViewSteering = (TextView) findViewById(R.id.text_view_steering);
        mSeekBarSpeedCmd = (SeekBar) findViewById(R.id.seek_bar_speed_cmd);
        mSeekBarSteeringCmd = (SeekBar) findViewById(R.id.seek_bar_steering_cmd);

        if (mCheckBoxTx.isChecked()) {
            mConstraintLayoutTx.setVisibility(View.VISIBLE);
        } else {
            mConstraintLayoutTx.setVisibility(View.INVISIBLE);
        }
        mCheckBoxTx.setOnCheckedChangeListener(mTxVisibilityListener);
        mSeekBarSpeedCmd.setMax((SPEED_CMD_MAX - SPEED_CMD_MIN)/ SPEED_CMD_STEP);
        mSeekBarSteeringCmd.setMax((STEERING_CMD_MAX - STEERING_CMD_MIN)/STEERING_CMD_STEP);
        mSeekBarSpeedCmd.setOnSeekBarChangeListener(mSpeedCmdChangeListener);
        mSeekBarSteeringCmd.setOnSeekBarChangeListener(mSteeringCmdChangeListener);

        Intent intent = new Intent(ArduinoActivity.this, DeviceListActivity.class);
        Log.d(TAG, "Asking for BT device address");
        startActivityForResult(intent, REQUEST_BT_ADDRESS);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothClient != null) {
            mBluetoothClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BT_ADDRESS) {
            if (resultCode == RESULT_OK) {
                String address = data.getStringExtra(DeviceListActivity.EXTRA_BT_ADDRESS);
                Log.d(TAG, "Device address received: " + address);
                mBluetoothClient = new BluetoothClient(mHandler);
                Log.d(TAG, "Connecting...");
                mBluetoothClient.connect(address);
            } else {
                finish();
            }
        }
    }

    private final CompoundButton.OnCheckedChangeListener mTxVisibilityListener =
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        mConstraintLayoutTx.setVisibility(View.VISIBLE);
                        mBluetoothClient.requestCommunicationMode(BluetoothClient.MODE_CONTROL);
                        int speedCmd = (mReceiveSpeedCmd - SPEED_CMD_MIN) / SPEED_CMD_STEP;
                        int steeringCmd = (mReceiveSteeringCmd - STEERING_CMD_MIN) / STEERING_CMD_STEP;
                        mSendSpeedCmd = mReceiveSpeedCmd;
                        mSendSteeringCmd = mReceiveSteeringCmd;
                        mSeekBarSpeedCmd.setProgress(speedCmd);
                        mSeekBarSteeringCmd.setProgress(steeringCmd);
                    } else {
                        mConstraintLayoutTx.setVisibility(View.INVISIBLE);
                        mBluetoothClient.requestCommunicationMode(BluetoothClient.MODE_MONITOR);
                    }
                }
            };

    private final SeekBar.OnSeekBarChangeListener mSpeedCmdChangeListener =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mSendSpeedCmd = SPEED_CMD_MIN + SPEED_CMD_STEP * progress;
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            };

    private final SeekBar.OnSeekBarChangeListener mSteeringCmdChangeListener =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mSendSteeringCmd = STEERING_CMD_MIN + STEERING_CMD_STEP * progress;
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            };

    private final Handler.Callback mHandlerCallback =
            new  BluetoothHandlerCallback() {
                @Override
                protected void onConnectionStateChanged(int newState) {
                    String state = "";
                    switch (newState) {
                        case BluetoothClient.STATE_NONE:
                            state = "STATE_NONE";
                            break;
                        case BluetoothClient.STATE_CONNECTING:
                            state = "STATE_CONNECTING";
                            break;
                        case BluetoothClient.STATE_CONNECTED:
                            state = "STATE_CONNECTED";
                            break;
                    }
                    Log.d(TAG, "onConnectionStateChanged: " + state);

                }

                @Override
                protected void onReceived(int speedCmd, int steeringCmd,
                                          float speed, float steering) {
                    Locale locale = Locale.getDefault();
                    Log.d(TAG, "onReceived: " + String.format(locale,
                            "[%d;%d;%.2f;%.2f]", speedCmd, steeringCmd, speed, steering));

                    mTextViewSpeedCmd.setText(String.format(locale, "%d", speedCmd));
                    mTextViewSteeringCmd.setText(String.format(locale, "%d", steeringCmd));
                    mTextViewSpeed.setText(String.format(locale, "%.2f m/s", speed));
                    mTextViewSteering.setText(String.format(locale, "%.2f°", steering));
                    mReceiveSpeedCmd = speedCmd;
                    mReceiveSteeringCmd = steeringCmd;

                    // Send commands only when we receive data to stay in sync
                    mBluetoothClient.send(mSendSpeedCmd, mSendSteeringCmd);
                }

                @Override
                protected void onSent(int speedCmd, int steeringCmd) {
                    Log.d(TAG, "onSent: " + String.format(Locale.getDefault(),
                            "[%d;%d]", speedCmd, steeringCmd));
                }

                @Override
                protected void onCommunicationModeChanged(String newMode) {
                    Log.d(TAG, "onCommunicationModeChanged: " + newMode);
                }

                @Override
                protected void onConnectionEstablished(String connectedDeviceName) {
                    Log.d(TAG, "onConnectionEstablished: " + connectedDeviceName);

                    // Set communication mode when we are connected
                    if (mCheckBoxTx.isChecked()) {
                        mBluetoothClient.requestCommunicationMode(BluetoothClient.MODE_CONTROL);
                    } else {
                        mBluetoothClient.requestCommunicationMode(BluetoothClient.MODE_MONITOR);
                    }
                }

                @Override
                protected void onConnectionError(String error) {
                    Log.d(TAG, "OnConnectionError: " + error);
                    Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
                }
            };

    private final Handler mHandler = new Handler(mHandlerCallback);
}
