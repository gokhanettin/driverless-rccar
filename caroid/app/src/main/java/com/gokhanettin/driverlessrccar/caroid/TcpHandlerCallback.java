package com.gokhanettin.driverlessrccar.caroid;

import android.os.Handler;
import android.os.Message;

/**
 * Created by gokhanettin on 01.04.2017.
 */

abstract class TcpHandlerCallback implements Handler.Callback {
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case TcpClient.MESSAGE_CONNECTION_STATE_CHANGE:
                onConnectionStateChanged(msg.arg1);
                break;
            case TcpClient.MESSAGE_RECEIVE:
                TcpClient.Input in = (TcpClient.Input) msg.obj;
                onReceived(in.speedCommand, in.steeringCommand);
                break;
            case TcpClient.MESSAGE_SEND:
                TcpClient.Output out = (TcpClient.Output) msg.obj;
                onSent(out.speedCommand, out.steeringCommand, out.speed, out.steering, out.jpeg);
                break;
            case TcpClient.MESSAGE_CONNECTION_ESTABLISHED:
                String serverAddress = msg.getData().getString(TcpClient.SERVER_ADDRESS);
                onConnectionEstablished(serverAddress);
                break;
            case TcpClient.MESSAGE_CONNECTION_ERROR:
                String error = msg.getData().getString(TcpClient.CONNECTION_ERROR);
                onConnectionError(error);
                break;
        }
        return true;
    }

    protected abstract void onConnectionStateChanged(int newState);
    protected abstract void onReceived(int speedCmd, int steeringCmd);
    protected abstract void onSent(int speedCmd, int steeringCmd, float speed,
                                   float steering, byte[] jpeg);
    protected abstract void onConnectionEstablished(String serverAddress);
    protected abstract void onConnectionError(String error);
}
