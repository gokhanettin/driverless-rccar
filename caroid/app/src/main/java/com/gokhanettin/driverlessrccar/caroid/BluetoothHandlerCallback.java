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
                BluetoothClient.ReceivedData data = (BluetoothClient.ReceivedData) msg.obj;
                onDataReceived(data.speedCommand, data.steeringCommand,
                        data.speed, data.steering);
                break;
            case BluetoothClient.MESSAGE_SEND:
                int[] commands = (int[])msg.obj;
                onCommandsSent(commands[0], commands[1]);
                break;
            case BluetoothClient.MESSAGE_COMMUNICATION_MODE_CHANGE:
                String mode = msg.getData().getString(BluetoothClient.COMMUNICATION_MODE);
                onCommunicationModeChanged(mode);
                break;
            case BluetoothClient.MESSAGE_DEVICE_NAME:
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
    protected abstract void onDataReceived(int speedCmd, int steeringCmd,
                                           float speed, float steering);
    protected abstract void onCommandsSent(int speedCmd, int steeringCmd);
    protected abstract void onCommunicationModeChanged(String newMode);
    protected abstract void onConnectionEstablished(String connectedDeviceName);
    protected abstract void onConnectionError(String error);
}
