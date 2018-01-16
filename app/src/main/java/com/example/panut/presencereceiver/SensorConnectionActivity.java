package com.example.panut.presencereceiver;

import android.os.Bundle;

import java.util.UUID;

import no.nordicsemi.android.nrftoolbox.profile.BleManager;
import no.nordicsemi.android.nrftoolbox.profile.BleManagerCallbacks;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileActivity;
import com.example.panut.presencereceiver.SignalManager;

/**
 * Created by Panut on 15-Jan-18.
 */

public class SensorConnectionActivity extends BleProfileActivity implements SignalManagerCallbacks {
    @Override
    protected void onCreateView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_sensor_connection);
    }

    @Override
    protected BleManager<? extends BleManagerCallbacks> initializeManager()
    {
        final SignalManager signalManager = SignalManager.getSignalManager(getApplicationContext());
        signalManager.setGattCallbacks(this);
        return signalManager;
    }

    @Override
    protected void setDefaultUI() {

    }

    @Override
    protected int getDefaultDeviceName() {
        return 0;
    }

    @Override
    protected int getAboutTextId() {
        return 0;
    }

    @Override
    protected UUID getFilterUUID() {
        return SignalManager.ACCEL_SERVICE_UUID;
    }
}
