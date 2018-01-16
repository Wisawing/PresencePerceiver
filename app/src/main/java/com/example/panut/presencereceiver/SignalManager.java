package com.example.panut.presencereceiver;

import android.content.Context;

import java.util.UUID;

import no.nordicsemi.android.nrftoolbox.profile.BleManager;

/**
 * Created by Panut on 29-Nov-17.
 */

public class SignalManager extends BleManager<SignalManagerCallbacks> {

    private static SignalManager managerInstance;

    // UUID for services characteristic.
    // BLE characteristic is always 0000xxxx-0000-1000-8000-00805f9b34fb
    public static final UUID ACCEL_SERVICE_UUID = UUID.fromString("0000beee-0000-1000-8000-00805f9b34fb");

    private SignalManager(final Context context) {
        super(context);
    }

    @Override
    protected BleManagerGattCallback getGattCallback() {
        return null;
    }

    public static SignalManager getSignalManager(Context applicationContext)
    {
        if(managerInstance == null) {
            managerInstance = new SignalManager(applicationContext);
        }

        return managerInstance;
    }
}
