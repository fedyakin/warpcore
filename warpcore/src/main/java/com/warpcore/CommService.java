package com.warpcore;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

public class CommService extends Service {
    public interface Callbacks {
        void onConnected();

        void onDisconnected();

        void onMessage(byte[] data, int len);
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
    private Callbacks mCallbacks;

    // Thread containing the actual bluetooth processing
    private ConnectedThread mConn = null;

    // UUID for the hc-o6
    private final UUID mModemUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

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

    public void setCallbacks(Callbacks callbacks) {
        mCallbacks = callbacks;
    }

    public void connect(BluetoothDevice device) {
        synchronized (this) {
            try {
                mConn = new ConnectedThread(device);
                if (mCallbacks != null) {
                    mCallbacks.onConnected();
                }
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Failed to connect: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void send(byte[] data) {
        synchronized (this) {
            if (mConn != null) {
                mConn.write(data);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        public ConnectedThread(BluetoothDevice device) throws IOException {
            mSocket = device.createRfcommSocketToServiceRecord(mModemUUID);
            mSocket.connect();
            mInStream = mSocket.getInputStream();
            mOutStream = mSocket.getOutputStream();
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int numBytes;

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    numBytes = mInStream.read(buffer);
                    if (mCallbacks != null) {
                        mCallbacks.onMessage(Arrays.copyOf(buffer, numBytes), numBytes);
                    }
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "Failed to read from device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (mCallbacks != null) {
                        mCallbacks.onDisconnected();
                    }
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mOutStream.write(bytes);
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
    }
}
