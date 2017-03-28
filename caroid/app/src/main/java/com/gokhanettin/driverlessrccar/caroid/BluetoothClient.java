package com.gokhanettin.driverlessrccar.caroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by gokhanettin on 26.03.2017.
 */

class BluetoothClient {
    private static final String TAG = "BluetoothClient";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mBluetoothAdapter;
    private final Handler mHandler;
    private final BluetoothDevice mDevice;
    private ConnectThread mConnectThread = null;
    private ConnectedThread mConnectedThread = null;
    private int mState;
    private int mNewState;

    // Message types sent from the BluetoothClient to activities
    public static final int MESSAGE_CONNECTION_STATE_CHANGE = 0;
    public static final int MESSAGE_RECEIVE = 1;
    public static final int MESSAGE_COMMUNICATION_MODE_CHANGE = 2;
    public static final int MESSAGE_SEND = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_CONNECTION_ERROR = 5;

    // Key names to identify some messages
    public static final String DEVICE_NAME = "device_name";
    public static final String CONNECTION_ERROR = "conn_error";
    public static final String COMMUNICATION_MODE = "comm_mode";

    // Communication modes
    public static final String MODE_NONE = "N";
    public static final String MODE_MONITOR = "M";
    public static final String MODE_CONTROL = "C";

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device

    public BluetoothClient(String address, Handler handler) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mDevice = mBluetoothAdapter.getRemoteDevice(address);
        mState = STATE_NONE;
        mNewState = mState;
        mHandler = handler;
    }

    private synchronized void notifyStateChange() {
        mState = getState();
        Log.d(TAG, "notifyStateChange() " + mNewState + " -> " + mState);
        mNewState = mState;

        // Give the new state to the Handler so the Activity can update
        mHandler.obtainMessage(MESSAGE_CONNECTION_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void connect() {
        Log.d(TAG, "Connecting to: " + mDevice);

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(mDevice);
        mConnectThread.start();
        notifyStateChange();
    }

    private synchronized void connected(BluetoothSocket socket) {
        Log.d(TAG, "Connected to " + mDevice);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back
        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, mDevice.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        notifyStateChange();
    }

    public synchronized void disconnect() {
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mState = STATE_NONE;
        notifyStateChange();
    }

    public void requestCommunicationMode(String mode) {
        requestCommunicationMode(mode, 0);
    }

    public void requestCommunicationMode(String mode, int delay) {
        // Create temporary object
        ConnectedThread t;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            t = mConnectedThread;
        }
        // Perform the write unsynchronized
        t.requestCommunicationMode(mode, delay);
    }

    public void sendCommands(int speedCmd, int steeringCmd) {
        sendCommands(speedCmd, steeringCmd, 0);
    }

    public void sendCommands(int speedCmd, int steeringCmd, int delay) {
        int[] commands = {speedCmd, steeringCmd};
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.sendCommands(commands, delay);
    }

    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_CONNECTION_ERROR);
        Bundle bundle = new Bundle();
        bundle.putString(CONNECTION_ERROR, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        notifyStateChange();
    }

    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_CONNECTION_ERROR);
        Bundle bundle = new Bundle();
        bundle.putString(CONNECTION_ERROR, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        notifyStateChange();
    }

    public class ReceivedData {
        public int speedCommand;
        public int steeringCommand;
        public float speed;
        public float steering;

        public ReceivedData() {
            this(0, 0, 0.0f, 0.0f);
        }

        public ReceivedData(int speedCommand, int steeringCommand, float speed, float steering) {
            this.speedCommand = speedCommand;
            this.steeringCommand = steeringCommand;
            this.speed = speed;
            this.steering = steering;
        }
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {

                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "createInsecureRfcommSocketToServiceRecord() failed", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "Unable to close() socket on connection failure", e2);
                }
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothClient.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket);
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() failed at ConnectThread:cancel", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        private StringBuilder mmStringBuilder;
        private boolean mmValid;

        ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Tmp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
            mmStringBuilder = new StringBuilder();
            mmValid = false;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            setName("ConnectedThread");
            int c = 0;
            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    // "[<throttle_cmd>;<steering_cmd>;<velocity>;<steering>]"
                    while(mmInStream.available() > 0) {
                        c = mmInStream.read();
                        if (c == '[') {
                            mmValid = true;
                            mmStringBuilder.setLength(0);
                            continue;
                        }
                        if (mmValid) {
                            if (c == ']') {
                                parse();
                                mmValid = false;
                                break;
                            } else {
                                mmStringBuilder.append((char)c);
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        void sendCommands(int[] commands, int delay) {
            // "[<throttle_cmd>;<steering_cmd>]"
            String string = String.format(Locale.getDefault(),
                    "[%d;%d]", commands[0], commands[1]);
            byte[] buffer = string.getBytes();
            try {
                mmOutStream.write(buffer);
                mHandler.obtainMessage(MESSAGE_SEND, -1, -1, commands).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception on sendCommands()", e);
            }

            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        void requestCommunicationMode(String mode, int delay) {
            // "[<mode>]"
            byte[] buffer = ("[" + mode + "]").getBytes();
            try {
                mmOutStream.write(buffer);
                Message msg = mHandler.obtainMessage(MESSAGE_COMMUNICATION_MODE_CHANGE);
                Bundle bundle = new Bundle();
                bundle.putString(COMMUNICATION_MODE, mode);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            } catch (IOException e) {
                Log.e(TAG, "Exception on requestCommunicationMode()", e);
            }

            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }

        private void parse() {
            // "<throttle_cmd>;<steering_cmd>;<velocity>;<steering>"
            String string = mmStringBuilder.toString();
            Log.d(TAG, "Parse string: " + string);
            String tokens[] = string.split(";");
            ReceivedData data = new ReceivedData();
            data.speedCommand = Integer.parseInt(tokens[0]);
            data.steeringCommand = Integer.parseInt(tokens[1]);
            data.speed = Float.parseFloat(tokens[2]);
            data.steering = Float.parseFloat(tokens[3]);
            mHandler.obtainMessage(MESSAGE_RECEIVE, -1, -1, data).sendToTarget();
        }
    }
}
