package com.warpcore;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import java.util.*;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class Connect extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static final int REQUEST_ENABLE_BT = 1;

    // Device holds information about available bluetooth devices in the selection spinner
    private class Device {
        public String name = "";
        public String address = "";

        public Device(String name, String address) {
            this.name = name;
            this.address = address;
        }

        public String toString() {
            return name;
        }
    }

    // First we need to ensure that bluetooth is enabled
    @Override
    protected void onStart() {
        super.onStart();

        // Disable the connect button, until we know we have bluetooth
        Button btnConnect = (Button)findViewById(R.id.btnConnect);
        btnConnect.setEnabled(false);

        // If the device doesn't support bluetooth, nothing we can do.
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if( bt == null ) {
            Toast.makeText(getApplicationContext(), "Device doesn't Support Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        // If bluetooth is disabled, ask the user to enable it
        // Otherwise we can just populate the device list.
        if( !bt.isEnabled() ) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            populateDeviceList();
        }
    }

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

        ArrayList<Device> devices = new ArrayList<>();
        for( BluetoothDevice device : pairedDevices ) {
            Device d = new Device(device.getName(), device.getAddress());
            devices.add(d);
        }

        // Set the spinner contents to the list of adapters
        ArrayAdapter<Device> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, devices);
        Spinner spinDevices = (Spinner)findViewById(R.id.spinDevices);
        spinDevices.setAdapter(adapter);

        // this activity implements the item selection listener
        // tell the spinner to notify us on selection changes
        spinDevices.setOnItemSelectedListener(this);
    }

    // An item in the spinner was selected, enable the connect button
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Button btnConnect = (Button)findViewById(R.id.btnConnect);
        btnConnect.setEnabled(true);
    }

    // Nothing in the spinner was selected, disable the connect button
    public void onNothingSelected(AdapterView parent) {
        Button btnConnect = (Button)findViewById(R.id.btnConnect);
        btnConnect.setEnabled(false);
    }

    public void onConnect() {
        Spinner spinDevices = (Spinner)findViewById(R.id.spinDevices);
        Device selectedDevice = (Device)spinDevices.getItemAtPosition(spinDevices.getSelectedItemPosition());

        Intent i = new Intent(getApplicationContext(), WarpControl.class);
        i.putExtra("address", selectedDevice.address);
        startActivity(i);
    }

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_connect);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.btnConnect).setOnTouchListener(mDelayHideTouchListener);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
