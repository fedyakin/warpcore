package com.warpcore;

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
import android.widget.EditText;
import android.widget.TextView;

public class RawComm extends AppCompatActivity {
    CommService mService;
    boolean mBound = false;
    Handler mHandler = new Handler(new CommCallback());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raw_comm);

        Intent intent = new Intent(this, CommService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
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

    // Called when the send input button is pressed
    public void onSend(View view) {
        if (!mBound) {
            return;
        }

        EditText input = (EditText) findViewById(R.id.editTextInput);
        String msg = input.getText().toString();
        mService.send(msg.getBytes());
    }

    private class CommCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.getData().getInt("event")) {
                case CommService.CONNECTED:
                    onConnected();
                    break;
                case CommService.DISCONNECTED:
                    onDisconnected();
                    break;
                case CommService.MESSAGE:
                    onMessage(message.getData().getString("data"));
                    break;
            }
            return true;
        }
    }

    public void onConnected() {
    }

    public void onDisconnected() {
        Intent i = new Intent(getApplicationContext(), ConnectActivity.class);
        startActivity(i);
    }

    public void onMessage(String line) {
        TextView output = (TextView) findViewById(R.id.textViewOutput);
        output.append(line);
    }
}
