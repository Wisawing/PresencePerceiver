package com.example.panut.presencereceiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.nrftoolbox.scanner.ScannerFragment;

/**
 * Created by Panut on 15-Jan-18.
 */

public class SensorConnectionActivity extends FragmentActivity implements ScannerFragment.OnDeviceSelectedListener, SensorConnection.SensorConnectionListener, NetworkStreamer.NetworkEventListener {

    protected static final int REQUEST_ENABLE_BT = 2;

    private final Handler timeHandler = new Handler();

    private List<SensorConnection> mConnections = new ArrayList<>();
    private LinearLayout mDeviceListView;

//    private AudioViewModel mAudioViewModel;
    private AudioOutputControlFragment mAudioControlFragment;
    private NetworkStreamer mNetworkStreamer;
    private OutputControlThread mOutputControlThread;

    private EditText ipAddressView;
    private Button mConnectButton;
    private TextView mNetworkDataView;

    private SharedPreferences mPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sensor_connection);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
        }

        ensureBLESupported();
        if (!isBLEEnabled()) {
            showBLEDialog();
        }

        mPreferences = getPreferences(MODE_PRIVATE);

        mAudioControlFragment = (AudioOutputControlFragment) getSupportFragmentManager().findFragmentById(R.id.audio_output_fragment);

//        if(mAudioViewModel == null)
//            mAudioViewModel = ViewModelProviders.of(this).get(AudioViewModel.class);
//        mAudioViewModel.setBufferChangedListener(mAudioControlFragment);

        // setup ui
        mDeviceListView = findViewById(R.id.device_list);

        Button addSensorButton = findViewById(R.id.action_connect);
        addSensorButton.setOnClickListener(this::onAddSensorClicked);

        mConnectButton = findViewById(R.id.connect_network_button);
        mConnectButton.setOnClickListener(this::onConnectClicked);
//        mConnectButton.setOnClickListener(this::forceCrash);

        ipAddressView = findViewById(R.id.ip_address_text);
        String ipAddress = mPreferences.getString(getString(R.string.ip_address), "haselab.ddns.com");
        ipAddressView.setText(ipAddress);

        mNetworkDataView = findViewById(R.id.network_data);

        // initialize network module
        mNetworkStreamer = new NetworkStreamer();
        mNetworkStreamer.setEventListener(this);

        mOutputControlThread = new OutputControlThread();
        mOutputControlThread.registerStreamer(mNetworkStreamer);
        mOutputControlThread.start();
    }

//    public void forceCrash(View view) {
//        throw new RuntimeException("This is a crash");
//    }


    @Override
    protected void onResume()
    {
        super.onResume();
    }

//    @Override
//    public void onDeviceDisconnected(BluetoothDevice device) {
//        super.onDeviceDisconnected(device);
//
//        Log.d("MyMonitor", "Device Disconnected : " + device.getName());
//
//        timeHandler.removeCallbacksAndMessages(null);
//    }

//    public void keepLog(){
//        timeHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                SimpleDateFormat timeFormat =  new SimpleDateFormat("hh:mm:ss");
//
//                Log.d("MyMonitor", timeFormat.format(new Date()));
//                keepLog();
//            }
//        }, 3000);
//    }

//    @Override
//    public void onDeviceConnected(BluetoothDevice device) {
//        super.onDeviceConnected(device);
//
//        keepLog();
//
//        Log.d("MyMonitor", "Device Connected : " + device.getName());
//    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        for(SensorConnection sc : mConnections){
            sc.onSaveInstanceState(outState);
        }
    }


    public void onAddSensorClicked(final View view) {
        if (isBLEEnabled()) {
            showDeviceScanningDialog(SignalManager.ACCEL_SERVICE_UUID);
        } else {
            showBLEDialog();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences.Editor prefEditor = mPreferences.edit();

        // save current ip address
        String address = ipAddressView.getText().toString();
        prefEditor.putString(getString(R.string.ip_address), address);
        prefEditor.commit();
    }

    public void onConnectClicked(final View view) {
        if(mNetworkStreamer.isConnectionAlive())
            mNetworkStreamer.disconnect();
        else {
            String address = ipAddressView.getText().toString();

            mNetworkStreamer.connect(address);
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
        connection.setConnectionListener(this);
    }

    @Override
    public void onDialogCanceled() {
        // do nothing

    }

    @Override
    public void onSensorConnected(SensorConnection connection) {
        mConnections.add(connection);
        mOutputControlThread.addNewBuffer(connection.id);


        runOnUiThread(() ->        {
            View sensorView = connection.getView();
            if(sensorView.getParent() == null)
                mDeviceListView.addView(sensorView);

        });
    }

    @Override
    public void onSensorDisconnected(SensorConnection connection) {
        mConnections.remove(connection);
        mOutputControlThread.deleteBuffer(connection.id);
        runOnUiThread(() -> mDeviceListView.removeView(connection.getView()));
    }

    @Override
    public void onSensorDataRead(short[] data, int ownerId) {
//        mNetworkStreamer.sendData(data);
        mOutputControlThread.writeToBuffer(data, ownerId);
    }

    @Override
    public void onNetworkConnected() {

        runOnUiThread(() -> {
            mConnectButton.setText("Disconnect");
//            mConnectButton.setEnabled(false);
        });
    }

    @Override
    public void onNetworkDisconnected() {
        runOnUiThread(() -> {
            mConnectButton.setText("Connect");
//            mConnectButton.setEnabled(true);
        });
    }

    int dataReceivedCount = 0;
    @Override
    public void onNetworkDataReceived(short[] data) {
        mAudioControlFragment.writeBuffer(data);

        // let's show this on UI
        if(dataReceivedCount % 8 == 0){ // this should be about 10 fps
            String data_s = "\tNetwork Data : " + data[0] + " - " + data[data.length-1];
            runOnUiThread(() -> {
                mNetworkDataView.setText(data_s);
            });
        }

        dataReceivedCount++;
    }
}


