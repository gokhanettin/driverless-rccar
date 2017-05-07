package com.gokhanettin.driverlessrccar.caroid;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;

/**
 * Created by gokhanettin on 01.04.2017.
 */

public class TcpClient {
    private static final String TAG = "TcpClient";

    // Message types sent from the TcpClient to activities
    public static final int MESSAGE_CONNECTION_STATE_CHANGE = 0;
    public static final int MESSAGE_RECEIVE = 1;
    public static final int MESSAGE_SEND = 2;
    public static final int MESSAGE_CONNECTION_ESTABLISHED = 3;
    public static final int MESSAGE_CONNECTION_ERROR = 4;

    // Key names to identify some messages
    public static final String SERVER_ADDRESS = "server_address";
    public static final String CONNECTION_ERROR = "tcp_conn_error";

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to the server

    private ConnectThread mConnectThread = null;
    private ConnectedThread mConnectedThread = null;

    private Handler mHandler;
    private int mState;
    private int mNewState;

    public TcpClient(Handler handler) {
        mHandler = handler;
        mState = STATE_NONE;
        mNewState = mState;
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

    public synchronized void connect(String ip, int port) {
        Log.d(TAG, "Connecting to: " + ip + ":" + port);

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
        mConnectThread = new ConnectThread(ip, port);
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

    public void send(int speedCmd, int steeringCmd, float speed, float steering, CameraPreview cameraPreview) {
        byte[] preview;
        int width;
        int height;

        ConnectedThread t;

        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            t = mConnectedThread;
            preview = cameraPreview.getPreview();
            width = cameraPreview.getPreviewWidth();
            height = cameraPreview.getPreviewHeight();
        }

        if(preview == null) {
            return;
        }

        Output out = new Output();
        out.speedCommand = speedCmd;
        out.steeringCommand = steeringCmd;
        out.speed = speed;
        out.steering = steering;
        out.jpeg = null;

        // Process the preview and send unsynchronized
        t.send(out, preview, width, height);
    }

    private synchronized void connected(Socket socket) {
        String serverAddress = socket.getInetAddress().toString();
        Log.d(TAG, "Connected to " + serverAddress);

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
        bundle.putString(SERVER_ADDRESS, serverAddress);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        notifyStateChange();
    }


    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_CONNECTION_ERROR);
        Bundle bundle = new Bundle();
        bundle.putString(CONNECTION_ERROR, "Unable to connect tcp server");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        notifyStateChange();
    }

    private void connectionLost() {
        // Send a conn lost message back to the Activity
        Message msg = mHandler.obtainMessage(MESSAGE_CONNECTION_ERROR);
        Bundle bundle = new Bundle();
        bundle.putString(CONNECTION_ERROR, "Tcp connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        notifyStateChange();
    }

    public class Input {
        public String newCommunicationMode;
        public int speedCommand;
        public int steeringCommand;
    }

    public class Output {
        public int speedCommand;
        public int steeringCommand;
        public float speed;
        public float steering;
        public byte[] jpeg;
    }

    private class ConnectThread extends Thread {
        String mmIP;
        int mmPort;
        Socket mmSocket;

        public ConnectThread(String ip, int port) {
            mmIP = ip;
            mmPort = port;
            mmSocket = new Socket();
        }

        @Override
        public void run() {
            super.run();

            try {
                mmSocket.connect(new InetSocketAddress(mmIP, mmPort), 10000);
            } catch (IOException e) {
                Log.e(TAG, "Failed to connect to " + mmIP + ":" + mmPort, e);
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
            synchronized (TcpClient.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket);
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() failed at ConnectThread.cancel()", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final Socket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        private StringBuilder mmStringBuilder;
        private boolean mmValid;

        public ConnectedThread(Socket socket) {
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

        @Override
        public void run() {
            super.run();
            Log.i(TAG, "BEGIN mConnectedThread");
            setName("ConnectedThread");
            int c;
            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    // "[<speed_cmd>;<steering_cmd>]"
                    while (mmInStream.available() > 0) {
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
                                mmStringBuilder.append((char) c);
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Connection lost to the tcp server", e);
                    connectionLost();
                    break;
                }
            }
        }

        void send(Output out, byte[] preview, int width, int height) {
            byte[] jpeg = preprocess(preview, width, height);
            if (jpeg == null) return;

            out.jpeg = jpeg;

            byte[] header = String.format(Locale.US, "[%d;%d;%.3f;%.3f;%d]",
                    out.speedCommand, out.steeringCommand, out.speed,
                    out.steering, jpeg.length).getBytes();
            try {
                mmOutStream.write(header);
                mmOutStream.write(jpeg);
                mmOutStream.flush();
                mHandler.obtainMessage(MESSAGE_SEND, -1, -1, out).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception on send()", e);
                connectionLost();
            }
        }

        private byte[] preprocess(byte[] preview, int width, int height) {
            byte[] jpeg = null;
            YuvImage image = new YuvImage(preview, ImageFormat.NV21, width, height, null);
            Rect r = new Rect(0, 0, width, height);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean ok = image.compressToJpeg(r, 100, baos);
            if (ok) {
                jpeg = baos.toByteArray();
            }
            return jpeg;
        }

        void cancel() {
            try {
                mmOutStream.write("$".getBytes());
                mmOutStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Failed to send communication end indication", e);
            }
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed at ConnectedThread.cancel()", e);
            }
        }

        private void parse() {
            // "<mode>" or "<speed_cmd>;<steering_cmd>"
            String string = mmStringBuilder.toString();
            Log.d(TAG, "Parse string: " + string);
            Input in = new Input();
            if (string.length() == 1) {
                // communication mode requested
                in.newCommunicationMode = string;
                in.speedCommand = 0;
                in.steeringCommand = 0;
            } else {
                // commands received
                in.newCommunicationMode = null;
                String tokens[] = string.split(";");
                in.speedCommand = Integer.parseInt(tokens[0]);
                in.steeringCommand = Integer.parseInt(tokens[1]);
            }
            mHandler.obtainMessage(MESSAGE_RECEIVE, -1, -1, in).sendToTarget();
        }
    }
}
