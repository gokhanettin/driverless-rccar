package com.gokhanettin.driverlessrccar.caroid;

import android.os.Handler;
import android.os.Message;

/**
 * Created by gokhanettin on 27.03.2017.
 */

abstract class BluetoothHandlerCallback implements Handler.Callback {
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case BluetoothClient.MESSAGE_CONNECTION_STATE_CHANGE:
                onConnectionStateChanged(msg.arg1);
                break;
            case BluetoothClient.MESSAGE_RECEIVE:
                BluetoothClient.Input in = (BluetoothClient.Input) msg.obj;
                onReceived(in.speedCommand, in.steeringCommand, in.speed, in.steering);
                break;
            case BluetoothClient.MESSAGE_SEND:
                BluetoothClient.Output out = (BluetoothClient.Output) msg.obj;
                onSent(out.speedCommand, out.steeringCommand);
                break;
            case BluetoothClient.MESSAGE_COMMUNICATION_MODE_CHANGE:
                String mode = msg.getData().getString(BluetoothClient.COMMUNICATION_MODE);
                onCommunicationModeChanged(mode);
                break;
            case BluetoothClient.MESSAGE_CONNECTION_ESTABLISHED:
                String deviceName = msg.getData().getString(BluetoothClient.DEVICE_NAME);
                onConnectionEstablished(deviceName);
                break;
            case BluetoothClient.MESSAGE_CONNECTION_ERROR:
                String error = msg.getData().getString(BluetoothClient.CONNECTION_ERROR);
                onConnectionError(error);
                break;
        }
        return true;
    }
    protected abstract void onConnectionStateChanged(int newState);
    protected abstract void onReceived(int speedCmd, int steeringCmd,
                                       float speed, float steering);
    protected abstract void onSent(int speedCmd, int steeringCmd);
    protected abstract void onCommunicationModeChanged(String newMode);
    protected abstract void onConnectionEstablished(String connectedDeviceName);
    protected abstract void onConnectionError(String error);
}
