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
    private BluetoothDevice mDevice;
    private ConnectThread mConnectThread = null;
    private ConnectedThread mConnectedThread = null;
    private int mState;
    private int mNewState;

    // Message types sent from the BluetoothClient to activities
    public static final int MESSAGE_CONNECTION_STATE_CHANGE = 0;
    public static final int MESSAGE_RECEIVE = 1;
    public static final int MESSAGE_COMMUNICATION_MODE_CHANGE = 2;
    public static final int MESSAGE_SEND = 3;
    public static final int MESSAGE_CONNECTION_ESTABLISHED = 4;
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

    public BluetoothClient(Handler handler) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mNewState = mState;
        mHandler = handler;
    }

    private synchronized void notifyStateChange() {
        mState = getState();
        if (mNewState != mState) {
            Log.d(TAG, "notifyStateChange() " + mNewState + " -> " + mState);
            mNewState = mState;

            // Give the new state to the Handler so the Activity can update
            mHandler.obtainMessage(MESSAGE_CONNECTION_STATE_CHANGE, mNewState, -1).sendToTarget();
        }
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void connect(String address) {
        mDevice = mBluetoothAdapter.getRemoteDevice(address);
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
        mConnectThread = new ConnectThread();
        mConnectThread.start();
        notifyStateChange();
    }

    public synchronized void disconnect() {
        mState = STATE_NONE;
        notifyStateChange();

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
        // Request unsynchronized
        t.requestCommunicationMode(mode, delay);
    }

    public void send(int speedCmd, int steeringCmd) {
        send(speedCmd, steeringCmd, 0);
    }

    public void send(int speedCmd, int steeringCmd, int delay) {
        Output out = new Output();
        out.speedCommand = speedCmd;
        out.steeringCommand = steeringCmd;

        ConnectedThread t;

        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            t = mConnectedThread;
        }
        // Send unsynchronized
        t.send(out, delay);
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
        Message msg = mHandler.obtainMessage(MESSAGE_CONNECTION_ESTABLISHED);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, mDevice.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        notifyStateChange();
    }

    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_CONNECTION_ERROR);
        Bundle bundle = new Bundle();
        bundle.putString(CONNECTION_ERROR, "Unable to connect to bluetooth device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        notifyStateChange();
    }

    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_CONNECTION_ERROR);
        Bundle bundle = new Bundle();
        bundle.putString(CONNECTION_ERROR, "Bluetooth device connection lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        notifyStateChange();
    }

    public class Input {
        public int speedCommand;
        public int steeringCommand;
        public float speed;
        public float steering;
    }

    public class Output {
        public int speedCommand;
        public int steeringCommand;
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        ConnectThread() {
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {

                tmp = mDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
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
                Log.e(TAG, "Tmp in/out streams not created", e);
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
            int c;
            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    // "[<speed_cmd>;<steering_cmd>;<speed>;<steering>]"
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
                    Log.e(TAG, "Connection lost to bluetooth device", e);
                    connectionLost();
                    break;
                }
            }
        }

        void send(Output out, int delay) {
            // "[<speed_cmd>;<steering_cmd>]"
            String string = String.format(Locale.US, "[%d;%d]",
                    out.speedCommand, out.steeringCommand);
            byte[] buffer = string.getBytes();
            try {
                mmOutStream.write(buffer);
                mHandler.obtainMessage(MESSAGE_SEND, -1, -1, out).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception on send()", e);
                connectionLost();
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
            // "<speed_cmd>;<steering_cmd>;<speed>;<steering>"
            String string = mmStringBuilder.toString();
            Log.d(TAG, "Parse string: " + string);
            String tokens[] = string.split(";");
            Input in = new Input();
            in.speedCommand = Integer.parseInt(tokens[0]);
            in.steeringCommand = Integer.parseInt(tokens[1]);
            in.speed = Float.parseFloat(tokens[2]);
            in.steering = Float.parseFloat(tokens[3]);
            mHandler.obtainMessage(MESSAGE_RECEIVE, -1, -1, in).sendToTarget();
        }
    }
}
