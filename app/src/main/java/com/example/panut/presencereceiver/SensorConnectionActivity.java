package com.example.panut.presencereceiver;

import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import no.nordicsemi.android.nrftoolbox.profile.BleManager;
import no.nordicsemi.android.nrftoolbox.profile.BleManagerCallbacks;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileActivity;
import com.example.panut.presencereceiver.SignalManager;

import static com.example.panut.presencereceiver.SignalManagerCallbacks.AccelData.VALUE_LENGTH;

/**
 * Created by Panut on 15-Jan-18.
 */

public class SensorConnectionActivity extends BleProfileActivity implements SignalManagerCallbacks {

    private TextView _accelTextview;
    private TextView _fpsView;
    private int count = -1;
    private long previousCountResetTime = 0;

    private SoundOutput _soundOutput;

    private final Handler timeHandler = new Handler();

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    public void onDeviceDisconnected(BluetoothDevice device) {
        super.onDeviceDisconnected(device);

        Log.d("Monitor", "Device Disconnected : " + device.getName());

        timeHandler.removeCallbacksAndMessages(null);
    }

    public void keepLog(){

        timeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat timeFormat =  new SimpleDateFormat("hh:mm:ss");

                Log.d("Monitor", timeFormat.format(new Date()));
                keepLog();
            }
        }, 3000);
    }

    @Override
    public void onDeviceConnected(BluetoothDevice device) {
        super.onDeviceConnected(device);

        keepLog();

        Log.d("Monitor", "Device Connected : " + device.getName());
    }

    @Override
    protected void onCreateView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_sensor_connection);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
        }

        _accelTextview = findViewById(R.id.accelTextView);
        _fpsView = findViewById(R.id.fps_view);
//        _soundOutput = new SoundOutput();
//        _soundOutput.init();

        soundData = new float[1024];
        Random rand = new Random();
        for(int i = 0; i < 1024; i++){
            soundData[i] = rand.nextFloat();
        }
    }

    @Override
    protected BleManager<? extends BleManagerCallbacks> initializeManager()
    {
        final SignalManager signalManager = SignalManager.getSignalManager(getApplicationContext());
        signalManager.setGattCallbacks(this);
//        signalManager.SetAccelTextView(_accelTextview);

        return signalManager;
    }

    @Override
    protected void setDefaultUI() {
        _accelTextview.setText("x, y, z");
    }

    @Override
    protected int getDefaultDeviceName() {
        return R.string.accel_default_name;
    }

    @Override
    protected int getAboutTextId() {
        return R.string.about_text;
    }

    @Override
    protected UUID getFilterUUID() {
        return SignalManager.ACCEL_SERVICE_UUID;
    }


    static float soundData[] = new float[1024];

    @Override
    public void onAccelDataRead(AccelData data) {
        String accel_str;
        accel_str = data.value[0] + " - " + data.value[VALUE_LENGTH - 1];

        long currentTime = System.nanoTime();
        float fps = (float)count/(currentTime - previousCountResetTime) * 1000000000;

        // fps purpose
        if(count > 10){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _fpsView.setText(fps + "");
                    _accelTextview.setText(accel_str);
                }
            });
        }


        if(count < 0 || count > 100) {
            count = 0;
            previousCountResetTime = System.nanoTime();
        }
        count++;

//        float soundData[] = new float[3];
//        soundData[0] = x/65535.f;
//        soundData[1] = y/65535.f;
//        soundData[2] = z/65535.f;

//        _soundOutput.write(soundData, 1024);
    }
}
