package com.warpcore;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class RawComm extends AppCompatActivity implements CommService.Callbacks {
    CommService mService;
    boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raw_comm);

        Intent intent = new Intent(this, CommService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
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
            mService.setCallbacks(RawComm.this);

            // Query for warp core settings
            mService.send("?".getBytes());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    // Called when the send input button is pressed
    public void onSend(View view) {
        if (!mBound) {
            return;
        }

        EditText input = (EditText) findViewById(R.id.editTextInput);
        mService.send(input.toString().getBytes());
    }

    public void onConnected() {
    }

    public void onDisconnected() {
        Intent i = new Intent(getApplicationContext(), ConnectActivity.class);
        startActivity(i);
    }

    public void onMessage(byte[] data, int len) {
        TextView output = (TextView) findViewById(R.id.textViewOutput);

        String msg = new String(data, len);
        output.append(msg);
    }
}
