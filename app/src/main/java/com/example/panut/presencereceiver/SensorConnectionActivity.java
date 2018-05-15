package com.example.panut.presencereceiver;

import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import no.nordicsemi.android.nrftoolbox.profile.BleManager;
import no.nordicsemi.android.nrftoolbox.profile.BleManagerCallbacks;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileActivity;

/**
 * Created by Panut on 15-Jan-18.
 */

public class SensorConnectionActivity extends BleProfileActivity implements SignalManagerCallbacks {

    private TextView _accelTextview;
    private TextView _fpsView;
    private int count = -1;
    private long previousCountResetTime = 0;
    private long previousFrameTime = 0;

    private final Handler timeHandler = new Handler();

    private AudioViewModel mAudioViewModel;
    private AudioOutputControlFragment mAudioControlFragment;

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

        mAudioControlFragment = (AudioOutputControlFragment) getSupportFragmentManager().findFragmentById(R.id.audio_output_fragment);

        if(mAudioViewModel == null)
            mAudioViewModel = ViewModelProviders.of(this).get(AudioViewModel.class);

        mAudioViewModel.getAudioBuffer().observe(this, mAudioControlFragment);
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


    private int freqIndex = 0;
    private short maxValue = Short.MIN_VALUE;
    private short minValue = Short.MAX_VALUE;
    @Override
    public void onAccelDataRead(AccelData data) {
        long currentTime = System.nanoTime();
        float timePeriod = (currentTime - previousFrameTime) / 1000000000.f; // in second


        // number of sample according to sample rate
        int nSample;
        if(previousFrameTime == 0) { // first time still does not have a time period
            nSample = data.value.length;
        }
        else {
            nSample =(int) (timePeriod * AudioOutputControlFragment.AUDIO_SAMPLE_RATE) * 2;
        }
        previousFrameTime = currentTime;

        // pre process data a bit
        short pData[] = new short[nSample];
        float dataSkipPerSample = (float)data.value.length/nSample;
        float dataIndex = 0;

        for(int i = 0; i < pData.length; i++){
            short temp = data.value[(int)dataIndex];

            pData[i] = (short)(temp*2); // blindly amplified
//            pData[i] = 10000;


            // from experiment raw data seems to be between 0 - 1024
            // highest value from actual experiment is 6 - 1003
            // try to see the min and max value of the data
            maxValue = (short)Math.max(maxValue, pData[i]);
            minValue = (short)Math.min(minValue, pData[i]);

            dataIndex += dataSkipPerSample;
        }

        String accel_str;
        accel_str = pData[0] + " - " + pData[pData.length - 1] + " : (" + minValue + " - " + maxValue + ")";

        // This works!
//        // debugging let's try code generated sound first
//        final int nSample = 512;
//        final int amp = 10000;
//        final int freq = 100;
//        short samples[] = new short[nSample];
//
//        for(int i = 0; i < nSample; i++){
//            float sample_f = (float)Math.sin((freqIndex % freq) / (float)freq * Math.PI)*amp;
//            short sample_s = (short)sample_f;
//            samples[i] = sample_s;
//
//            freqIndex++;
//        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAudioViewModel.setAudioBuffer(pData);
//                mAudioViewModel.setAudioBuffer(samples);
            }
        });


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
    }
}
