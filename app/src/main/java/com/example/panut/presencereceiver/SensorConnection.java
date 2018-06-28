package com.example.panut.presencereceiver;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import no.nordicsemi.android.nrftoolbox.profile.BleManager;
import no.nordicsemi.android.nrftoolbox.profile.BleManagerCallbacks;
import no.nordicsemi.android.nrftoolbox.profile.BleProfile;

/**
 * Created by Panut on 15-Jan-18.
 */


public class SensorConnection extends BleProfile implements SignalManagerCallbacks {

//    static public class ConnectionViews {
//        public TextView deviceNameView;
//        public TextView dataView;
//        public TextView fpsView;
//        public Button disconnectButton;
//    }

    public interface SensorConnectionListener {
        void onSensorConnected(SensorConnection connection);
        void onSensorDisconnected(SensorConnection connection);
        void onSensorDataRead(short[] data, int ownerId);
    }

    private static int uniqueID = 1;

    private int count = -1;
    private long previousCountResetTime = 0;
//    private long previousFrameTime = 0;
    private final Handler timeHandler = new Handler();
//    private ConnectionViews mConnectionUI;
    private View mRootView;
    private TextView mDeviceNameView;
    private TextView mDataView;
    private TextView mFpsView;
    private Button mDisconnectButton;

//    private AudioViewModel mAudioViewModel;

    private SensorConnectionListener mConnectionListener;

    /** parameters for keeping tracks */
    private Date mLastDataReceivedTime = new Date();
    private short mLastDataReceived = 0;
    private SimpleDateFormat mTimeFormat = new SimpleDateFormat("dd:hh:mm:ss");

    public int id;


    public static SensorConnection createConnection(final BluetoothDevice device, final Activity context) {
        SensorConnection connection = new SensorConnection(context);

        connection.connect(device);

        return connection;
    }

    private void createView(){
        mRootView = View.inflate(mContext, R.layout.device_connection_list_item, null);
        mDeviceNameView = mRootView.findViewById(R.id.device_name);
        mDataView = mRootView.findViewById(R.id.device_data);
        mFpsView = mRootView.findViewById(R.id.fps);
        mDisconnectButton = mRootView.findViewById(R.id.disconnect_button);

        mDisconnectButton.setOnClickListener(v ->  {
            disconnect();

            timeHandler.removeCallbacksAndMessages(null);
            mConnectionListener.onSensorDisconnected(this);
        });
    }

    public void setConnectionListener(SensorConnectionListener connectionListener) {
        this.mConnectionListener = connectionListener;
    }

    public SensorConnection(Activity context) {
        super(context);

        id = uniqueID++;

        createView();
//        mAudioViewModel = ViewModelProviders.of((FragmentActivity)mContext).get(AudioViewModel.class);
    }

    public View getView() {
        return mRootView;
    }

//    // unused currently auto connect can be done in BleManager
//    public void reconnectDevice(String address) {
//        timeHandler.postDelayed(() -> {
//            final BluetoothManager manager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
//            BluetoothAdapter bluetoothAdapter = manager.getAdapter();
//
//            boolean reconnected = false;
//            Log.d("MyMonitor", "disconnected from : " + address);
//            for(BluetoothDevice iDevice : bluetoothAdapter.getBondedDevices()){
//                Log.d("MyMonitor", "Bonded Device : " + iDevice.getAddress());
//                if(address.compareTo(iDevice.getAddress())==0) {
////                    Log.d("MyMonitor", "Bonded Device : " + iDevice.getAddress());
//                    connect(iDevice);
//                    reconnected = true;
//                }
//            }
//
//            if(!reconnected)
//                reconnectDevice(address);
//        }, 3000);
//    }

    @Override
    public void onDeviceDisconnected(BluetoothDevice device) {
        super.onDeviceDisconnected(device);

        Log.d("MyMonitor", "Device Disconnected : " + device.getName());
//        reconnectDevice(device.getAddress());
//        mAudioViewModel.removeBuffer(this);
    }

    public void keepMonitorLog(){

        timeHandler.postDelayed(() -> {
            Log.d("MyMonitor", "Last Received Data : " + mLastDataReceived + " , "
                    + "Last Data Received Time : " + mTimeFormat.format(mLastDataReceivedTime));
            keepMonitorLog();
        }, 30000);
    }

    @Override
    public void onDeviceConnected(BluetoothDevice device) {
        super.onDeviceConnected(device);

        keepMonitorLog();
        Log.d("MyMonitor", "Device Connected : " + device.getName());

        mContext.runOnUiThread(() -> mDeviceNameView.setText(device.getName()));

//        mAudioViewModel.initializeBuffer(this);
        mConnectionListener.onSensorConnected(this);
    }

//    @Nullable
//    @Override
//    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
//        View v = inflater.inflate(R.layout.fragment_sensor_connection, container);
//
//        _accelTextview = v.findViewById(R.id.accelTextView);
//        _fpsView = v.findViewById(R.id.fps_view);
//
//        return v;
//    }

    @Override
    protected BleManager<? extends BleManagerCallbacks> initializeManager()
    {
        final SignalManager signalManager = SignalManager.getSignalManager(mContext.getApplicationContext());
        signalManager.setGattCallbacks(this);
////        signalManager.SetAccelTextView(_accelTextview);

        return signalManager;
    }


    private short dataCount = 0;
    @Override
    public void onAccelDataRead(AccelData data) {
        long currentTime = System.nanoTime();
        float timePeriod = (currentTime - previousCountResetTime) / 1000000000.f; // in second

//        previousFrameTime = currentTime;

        String accel_str;
        accel_str = data.value[0] + " - " + data.value[data.value.length - 1];

//        mAudioViewModel.writeAudioBuffer(data.value, id);
        mConnectionListener.onSensorDataRead(data.value, id);

        float fps = (float)count/timePeriod;
        // fps purpose
        if(mRootView != null && count % 8 == 0){ // update around 10 fps
            mContext.runOnUiThread(() -> {
                mDataView.setText(accel_str);
                mFpsView.setText(fps + "");
            });
        }

        if(count < 0 || count > 100) {
            count = 0;
            previousCountResetTime = System.nanoTime();
        }
        count++;

        // keep log
        mLastDataReceivedTime = new Date();
        mLastDataReceived = data.value[0];
    }
}
