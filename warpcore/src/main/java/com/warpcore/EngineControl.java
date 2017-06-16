package com.warpcore;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;

public class EngineControl extends AppCompatActivity {
    private Settings currentSettings = new Settings();

    BluetoothCommService mService;
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
                currentSettings.mWarpFactor = ((byte) (progress + 1));
                updateSettings();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        RadioGroup corePattern = (RadioGroup) findViewById(R.id.radioGroupPattern);
        corePattern.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch (checkedId) {
                    case R.id.radioButtonStandard:
                        currentSettings.mCorePattern = 1;
                        break;
                    case R.id.radioButtonBreach:
                        currentSettings.mCorePattern = 2;
                        break;
                    case R.id.radioButtonRainbow:
                        currentSettings.mCorePattern = 3;
                        break;
                    case R.id.radioButtonFade:
                        currentSettings.mCorePattern = 4;
                        break;
                    case R.id.radioButtonSlowFade:
                        currentSettings.mCorePattern = 5;
                        break;
                }

                updateSettings();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBound) {
            Intent intent = new Intent(this, BluetoothCommService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to BluetoothCommService, cast the IBinder and get LocalService instance
            BluetoothCommService.LocalBinder binder = (BluetoothCommService.LocalBinder) service;
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
                case BluetoothCommService.CONNECTED:
                    onConnected();
                    break;
                case BluetoothCommService.DISCONNECTED:
                    onDisconnected();
                    break;
                case BluetoothCommService.MESSAGE:
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
        Toast.makeText(getApplicationContext(), "Received bluetooth line: " + line, Toast.LENGTH_SHORT).show();
        Settings newSettings = Settings.ParseString(line);
        if (newSettings != null) {
            Toast.makeText(getApplicationContext(), "Received current settings: " + new String(newSettings.Encode()), Toast.LENGTH_SHORT).show();
            updateFromSettings(newSettings);
        }
    }

    private void updateFromSettings(Settings newSettings) {
        SeekBar warp = (SeekBar) findViewById(R.id.seekBarWarp);
        warp.setProgress(((int) newSettings.mWarpFactor - 1));

        RadioButton b = (RadioButton) findViewById(R.id.radioButtonStandard);
        switch (newSettings.mCorePattern) {
            case 1:
                b = (RadioButton) findViewById(R.id.radioButtonStandard);
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
        if (mBound) {
            mService.send(currentSettings.Encode());
            mService.send("?".getBytes());
            Toast.makeText(getApplicationContext(), "Settings sent to core: " + new String(currentSettings.Encode()), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth comm service not yet bound", Toast.LENGTH_SHORT).show();
        }
    }
}
