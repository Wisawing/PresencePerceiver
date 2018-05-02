package com.example.panut.presencereceiver;

import no.nordicsemi.android.nrftoolbox.profile.BleManagerCallbacks;

/**
 * Created by Panut on 29-Nov-17.
 */


public interface SignalManagerCallbacks extends BleManagerCallbacks {
    public class AccelData{
        public short value[];
        public static final int VALUE_LENGTH = 10;

        AccelData(short[] _value){
            value = _value;
        }
    }

    public void onAccelDataRead(AccelData data);
}
