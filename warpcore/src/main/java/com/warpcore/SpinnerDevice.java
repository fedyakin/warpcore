package com.warpcore;

import android.bluetooth.BluetoothDevice;

// Holds information about bluetooth devices in a spinner
public class SpinnerDevice {
    public final BluetoothDevice mDevice;

    public SpinnerDevice(BluetoothDevice device) {
        mDevice = device;
    }

    public String toString() {
        return mDevice.getName();
    }
}
