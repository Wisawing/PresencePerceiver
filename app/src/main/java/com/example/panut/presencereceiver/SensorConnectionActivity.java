package com.example.panut.presencereceiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.nrftoolbox.scanner.ScannerFragment;

/**
 * Created by Panut on 15-Jan-18.
 */

public class SensorConnectionActivity extends FragmentActivity implements ScannerFragment.OnDeviceSelectedListener {

    protected static final int REQUEST_ENABLE_BT = 2;
//    private TextView _accelTextview;
//    private TextView _fpsView;
//    private int count = -1;
//    private long previousCountResetTime = 0;
//    private long previousFrameTime = 0;


    private final Handler timeHandler = new Handler();

    private List<SensorConnection> mConnections = new ArrayList<>();
    private LinearLayout mDeviceListView;

//    private AudioViewModel mAudioViewModel;
    private AudioOutputControlFragment mAudioControlFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
        }

        ensureBLESupported();
        if (!isBLEEnabled()) {
            showBLEDialog();
        }

        mAudioControlFragment = (AudioOutputControlFragment) getSupportFragmentManager().findFragmentById(R.id.audio_output_fragment);
        // TODO fix audio output
//        if(mAudioViewModel == null)
//            mAudioViewModel = ViewModelProviders.of(this).get(AudioViewModel.class);
//        mAudioViewModel.getAudioBuffer(this).observe(this, mAudioControlFragment);

        setContentView(R.layout.activity_sensor_connection);

        // setup ui
        mDeviceListView = findViewById(R.id.device_list);

        Button connectButton = findViewById(R.id.action_connect);
        connectButton.setOnClickListener(this::onConnectClicked);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

//    @Override
//    public void onDeviceDisconnected(BluetoothDevice device) {
//        super.onDeviceDisconnected(device);
//
//        Log.d("Monitor", "Device Disconnected : " + device.getName());
//
//        timeHandler.removeCallbacksAndMessages(null);
//    }

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

//    @Override
//    public void onDeviceConnected(BluetoothDevice device) {
//        super.onDeviceConnected(device);
//
//        keepLog();
//
//        Log.d("Monitor", "Device Connected : " + device.getName());
//    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // TODO call each SensorConnection
    }

    /**
     * Called when user press CONNECT or DISCONNECT button. See layout files -> onClick attribute.
     */
    public void onConnectClicked(final View view) {
        if (isBLEEnabled()) {
            showDeviceScanningDialog(SignalManager.ACCEL_SERVICE_UUID);
        } else {
            showBLEDialog();
        }
    }

    /**
     * Shows the scanner fragment.
     *
     * @param filter               the UUID filter used to filter out available devices. The fragment will always show all bonded devices as there is no information about their
     *                             services
     */
    private void showDeviceScanningDialog(final UUID filter) {
        runOnUiThread(() -> {
            final ScannerFragment dialog = ScannerFragment.getInstance(filter);
            dialog.show(getFragmentManager(), "scan_fragment");
        });
    }

    private void ensureBLESupported() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    protected boolean isBLEEnabled() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter adapter = bluetoothManager.getAdapter();
        return adapter != null && adapter.isEnabled();
    }

    protected void showBLEDialog() {
        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    @Override
    public void onDeviceSelected(BluetoothDevice device, String name) {
        SensorConnection connection = SensorConnection.createConnection(device, this);
        mConnections.add(connection);

        mDeviceListView.addView(connection.getView());
    }

    @Override
    public void onDialogCanceled() {
        // do nothing

    }
}


