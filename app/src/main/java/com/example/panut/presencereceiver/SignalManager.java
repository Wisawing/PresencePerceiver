package com.example.panut.presencereceiver;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import java.util.Deque;
import java.util.LinkedList;
import java.util.UUID;

import no.nordicsemi.android.log.Logger;
import no.nordicsemi.android.nrftoolbox.profile.BleManager;
import no.nordicsemi.android.nrftoolbox.profile.BleManagerCallbacks;

import static com.example.panut.presencereceiver.SignalManagerCallbacks.AccelData.VALUE_LENGTH;

/**
 * Created by Panut on 29-Nov-17.
 */

public class SignalManager extends BleManager<SignalManagerCallbacks> {

    private static SignalManager managerInstance;

//    private TextView _accelTextview;

    // UUID for services characteristic.
    // BLE characteristic is always 0000xxxx-0000-1000-8000-00805f9b34fb

//    public static final UUID ACCEL_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
//    public static final UUID ACCEL_CHAR_UUID = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb");

    public static final UUID ACCEL_SERVICE_UUID = UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb");
    public static final UUID ACCEL_CHAR_UUID = UUID.fromString("0000beee-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic mAcceleration;

    private SignalManager(final Context context) {
        super(context);
    }

//    public void SetAccelTextView(TextView textview)
//    {
//        _accelTextview = textview;
//    }

    public static SignalManager getSignalManager(Context applicationContext)
    {
        if(managerInstance == null) {
            managerInstance = new SignalManager(applicationContext);
        }

        return managerInstance;
    }


    private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {
        @Override
        protected boolean isRequiredServiceSupported(BluetoothGatt gatt) {
            BluetoothGattService service = gatt.getService(ACCEL_SERVICE_UUID);

            Log.d("debug", "isRequiredServiceSupported");

            if(service != null) {
                mAcceleration = service.getCharacteristic(ACCEL_CHAR_UUID);
            }
            return mAcceleration != null;
        }

        @Override
        protected Deque<Request> initGatt(BluetoothGatt gatt) {
            LinkedList<Request> requests = new LinkedList<Request>();

            Log.d("debug", "initGatt");

            if(mAcceleration != null) {
                requests.add(Request.newEnableNotificationsRequest(mAcceleration));
            }

            return requests;
        }


        @Override
        protected void onDeviceDisconnected() {
            mAcceleration = null;
        }

        short previousValue = 0;
        long previousTime = 0;
        protected void parseAccelValue(BluetoothGattCharacteristic characteristic)
        {
//            int x = 0, y = 0, z = 0;

//            x = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
//            offset += 2;
//            y = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
//            offset += 2;
//            z = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
//            offset += 2;

//            System.out.println(x + ", " + y + ", " + z);
//            System.out.println("test");

            byte[] data = characteristic.getValue();

            short values[] = new short[VALUE_LENGTH];
            for(int i = 0; i < data.length; i+=2) {
                short value = 0;

                // if the data's first bit is '1' (in other word a minus value)
                // the value will be first casted to int and all the front bits will be 1 to maintain the minus property
                // thus '& 0xff' is necessary to get rid off all those extra 1s.
                value = (short)(value | ((data[i] & 0xff)<<8));
                value = (short)(value | (data[i+1] & 0xff));

                values[i/2] = value;
                if(value != previousValue + 1) {
                    Log.d("Monitor", "disconnected data for " + (System.nanoTime()-previousTime) + " : " + previousValue + ", " + value);
                }

//                System.out.print(value + ":");
//                System.out.print("(" + data[i] + ", ");
//                System.out.print(data[i+1] + "), ");

                previousValue = value;
                previousTime = System.nanoTime();
            }

            mCallbacks.onAccelDataRead(new SignalManagerCallbacks.AccelData(values));
        }

        @Override
        protected void onCharacteristicNotified(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//            Logger.a(mLogSession, "\"" + IntermediateCuffPressureParser.parse(characteristic) + "\" received");

//            Log.d("debug", "Notified");
            parseAccelValue(characteristic);
        }

        @Override
        protected void onCharacteristicIndicated(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//            Logger.a(mLogSession, "\"" + IntermediateCuffPressureParser.parse(characteristic) + "\" received");

//            Log.d("debug", "Indicated");
            parseAccelValue(characteristic);
        }
    };

    @Override
    protected BleManagerGattCallback getGattCallback() {
        return mGattCallback;
    }
}
