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
import android.widget.RadioButton;
import android.widget.SeekBar;

public class EngineControl extends AppCompatActivity {
    private Settings currentSettings;

    CommService mService;
    boolean mBound = false;
    boolean mSettingsNext = false;
    Handler mHandler = new Handler(new CommCallback());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_engine_control);

        SeekBar warp = (SeekBar) findViewById(R.id.seekBarWarp);
        warp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }

                currentSettings.WarpFactor = ((byte) (progress + 1));
                updateSettings();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        Intent intent = new Intent(this, EngineControl.class);
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
        mService.send("?".getBytes());
    }

    public void onDisconnected() {
        Intent i = new Intent(getApplicationContext(), ConnectActivity.class);
        startActivity(i);
    }

    public void onMessage(String line) {
        if (line.contains("Current Settings")) {
            mSettingsNext = true;
            return;
        }

        Settings newSettings = new Settings();
        if (newSettings.ParseString(line)) {
            updateFromSettings(newSettings);
        }
    }

    public void onRadioButtonClicked(View view) {
        // We only care about a pattern being selected
        boolean checked = ((RadioButton) view).isChecked();
        if (!checked) {
            return;
        }

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.radioButtonStandard:
                currentSettings.Pattern = 1;
                break;
            case R.id.radioButtonBreach:
                currentSettings.Pattern = 2;
                break;
            case R.id.radioButtonRainbow:
                currentSettings.Pattern = 3;
                break;
            case R.id.radioButtonFade:
                currentSettings.Pattern = 4;
                break;
            case R.id.radioButtonSlowFade:
                currentSettings.Pattern = 5;
                break;
        }

        updateSettings();
    }

    private void updateFromSettings(Settings newSettings) {
        SeekBar warp = (SeekBar) findViewById(R.id.seekBarWarp);
        warp.setProgress(newSettings.WarpFactor - 1, true);

        RadioButton b = (RadioButton) findViewById(R.id.radioButtonStandard);
        ;
        switch (newSettings.Pattern) {
            case 1:
                break;
            case 2:
                b = (RadioButton) findViewById(R.id.radioButtonBreach);
                break;
            case 3:
                b = (RadioButton) findViewById(R.id.radioButtonRainbow);
                break;
            case 4:
                b = (RadioButton) findViewById(R.id.radioButtonFade);
                break;
            case 5:
                b = (RadioButton) findViewById(R.id.radioButtonSlowFade);
                break;
        }
        b.setChecked(true);

        currentSettings = newSettings;
    }

    private void updateSettings() {
        mService.send(currentSettings.Encode());
    }
}
