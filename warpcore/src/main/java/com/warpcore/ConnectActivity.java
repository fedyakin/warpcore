package com.warpcore;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class ConnectActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;

    CommService mService;
    boolean mBound = false;
    Handler mHandler = new Handler(new CommCallback());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, CommService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // If the device doesn't support bluetooth, nothing we can do.
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt == null) {
            Toast.makeText(getApplicationContext(), "Device doesn't Support Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        // If bluetooth is disabled, ask the user to enable it
        if (!bt.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            populateDeviceList();
        }
    }

    @Override
    protected void onDestroy() {
        if (mBound) {
            mService.removeHandler(mHandler);
        }
        super.onDestroy();
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to CommService, cast the IBinder and get LocalService instance
            CommService.LocalBinder binder = (CommService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.addHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // We only care about the attempt to enable bluetooth
        if (requestCode != REQUEST_ENABLE_BT) {
            return;
        }

        // If bluetooth has become enabled, then populate the device list
        // Otherwise we can't do anything
        if (resultCode == RESULT_OK) {
            populateDeviceList();
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth was not enabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void populateDeviceList() {
        // Build a list of paired bluetooth devices
        Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();

        ArrayList<SpinnerDevice> devices = new ArrayList<>();
        for (BluetoothDevice device : pairedDevices) {
            devices.add(new SpinnerDevice(device));
        }

        // Set the spinner contents to the list of adapters
        ArrayAdapter<SpinnerDevice> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, devices);
        Spinner spinDevices = (Spinner) findViewById(R.id.spinDevices);
        spinDevices.setAdapter(adapter);
    }

    public void onConnect(View view) {
        Spinner spinDevices = (Spinner) findViewById(R.id.spinDevices);
        int selected = spinDevices.getSelectedItemPosition();
        if (selected == -1) {
            Toast.makeText(getApplicationContext(), "No paired device selected", Toast.LENGTH_SHORT).show();
            return;
        }

        SpinnerDevice selectedDevice = (SpinnerDevice) spinDevices.getItemAtPosition(selected);
        mService.connect(selectedDevice.mDevice);
    }


    private class CommCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.getData().getInt("event")) {
                case CommService.CONNECTED:
                    onConnected();
            }
            return true;
        }
    }

    public void onConnected() {
        Intent i = new Intent(getApplicationContext(), RawComm.class);
        startActivity(i);
    }
}

