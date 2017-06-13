package com.warpcore;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class CommService extends Service {
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
    private final ArrayList<Handler> mHandlers = new ArrayList<>();

    // Thread containing the actual bluetooth processing
    private ConnectedThread mConn = null;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        CommService getService() {
            return CommService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public static final int CONNECTED = 1;
    public static final int DISCONNECTED = 2;
    public static final int MESSAGE = 3;

    public void addHandler(Handler handler) {
        synchronized (mHandlers) {
            mHandlers.add(handler);
        }
    }

    public void removeHandler(Handler handler) {
        synchronized (mHandlers) {
            mHandlers.remove(handler);
        }
    }

    public void connect(BluetoothDevice device) {
        if (mConn != null ) {
            mConn.cancel();
            mConn.interrupt();
        }
        mConn = new ConnectedThread(device);
        mConn.start();
    }

    public void send(byte[] data) {
        if (mConn != null) {
            mConn.write(data);
        }
    }

    private class ConnectedThread extends Thread {
        // UUID for the hc-o6
        private final UUID mModemUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        private final BluetoothDevice mDevice;
        private BluetoothSocket mSocket;
        private BufferedReader mInput;
        private OutputStream mOutput;

        public ConnectedThread(BluetoothDevice device) {
            mDevice = device;
        }

        public void run() {
            try {
                mSocket = mDevice.createRfcommSocketToServiceRecord(mModemUUID);
                mSocket.connect();
                mInput = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                mOutput = mSocket.getOutputStream();

                // Keep listening to the InputStream until an exception occurs.
                emitConnected();
                while (mSocket.isConnected() && !Thread.interrupted()) {
                    try {
                        String line = mInput.readLine();
                        emitMessage(line);
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "Failed to read from device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Failed to connect: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            emitDisconnected();
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mOutput.write(bytes);
                mOutput.flush();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Failed to write to device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Error closing socket: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }


        private void emitConnected() {
            Bundle msgBundle = new Bundle();
            msgBundle.putInt("event", CONNECTED);
            sendBundle(msgBundle);
        }

        private void emitDisconnected() {
            Bundle msgBundle = new Bundle();
            msgBundle.putInt("event", DISCONNECTED);
            sendBundle(msgBundle);
        }

        private void emitMessage(String data) {
            Bundle msgBundle = new Bundle();
            msgBundle.putInt("event", MESSAGE);
            msgBundle.putString("data", data);
            sendBundle(msgBundle);
        }

        private void sendBundle(Bundle msgBundle) {
            synchronized (CommService.this.mHandlers) {
                for (Handler handler : CommService.this.mHandlers) {
                    Message msg = handler.obtainMessage();
                   // Message msg = new Message();
                    msg.setData(msgBundle);
                   // handler.sendMessage(msg);
                    msg.sendToTarget();
                }
            }
        }
    }
}
