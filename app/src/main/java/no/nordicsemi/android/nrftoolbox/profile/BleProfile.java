/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package no.nordicsemi.android.nrftoolbox.profile;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.panut.presencereceiver.R;
import com.example.panut.presencereceiver.SensorConnection;

import java.util.UUID;

import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.nrftoolbox.scanner.ScannerFragment;
import no.nordicsemi.android.nrftoolbox.utility.DebugLogger;

public abstract class BleProfile implements BleManagerCallbacks {

    private static final String TAG = "BaseProfileActivity";

    private static final String SIS_CONNECTION_STATUS = "connection_status";
    private static final String SIS_DEVICE_NAME = "device_name";

    private BleManager<? extends BleManagerCallbacks> mBleManager;
    protected Activity mContext;

    // TODO move ui out of here
//    private View mRootView;
//    private TextView mDeviceNameView;
////    private TextView mBatteryLevelView;
//    private Button mConnectButton;


    private ILogSession mLogSession;

    private boolean mDeviceConnected = false;
    private String mDeviceName;

//    @Override
//    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//        mRootView = view;
//
//        mConnectButton = mRootView.findViewById(R.id.action_connect);
//        mDeviceNameView = mRootView.findViewById(R.id.device_name);
//
//        mConnectButton.setOnClickListener(this::onAddSensorClicked);
//    }

    public BleProfile(Activity context) {
        mContext = context;
        /*
         * We use the managers using a singleton pattern. It's not recommended for the Android, because the singleton instance remains after Activity has been
         * destroyed but it's simple and is used only for this demo purpose. In final application Managers should be created as a non-static objects in
         * Services. The Service should implement ManagerCallbacks interface. The application Activity may communicate with such Service using binding,
         * broadcast listeners, local broadcast listeners (see support.v4 library), or messages. See the Proximity profile for Service approach.
         */
        mBleManager = initializeManager();

        // In onInitialize method a final class may register local broadcast receivers that will listen for events from the service
        onInitialize();
//        // The onCreateView class should... create the view
//        onCreateView(savedInstanceState);

//        final Toolbar toolbar = findViewById(R.id.toolbar_actionbar);
//        setActionBar(toolbar);

        // Common nRF Toolbox view references are obtained here
//        setUpView();
//        // View is ready to be used
//        onViewCreated(savedInstanceState);
    }

    /**
     * You may do some initialization here. This method is called from {@link #BleProfile(Activity)} before the view was created.
     */
    protected void onInitialize() {
        // empty default implementation
    }

//    /**
//     * Called after the view and the toolbar has been created.
//     */
//    protected final void setUpView() {
//        // set GUI
////        getActionBar().setDisplayHomeAsUpEnabled(true);
//
//        mConnectButton = mRootView.findViewById(R.id.action_connect);
//        mDeviceNameView = mRootView.findViewById(R.id.device_name);
//
//        mConnectButton.setOnClickListener(this::onAddSensorClicked);
////        mBatteryLevelView = findViewById(R.id.battery);
//    }

//    @Override
//    public void onBackPressed() {
//        mBleManager.disconnect();
//        super.onBackPressed();
//    }

    // must be called from outside to work. does not get called automatically.
    public void onSaveInstanceState(final Bundle outState) {
//        super.onSaveInstanceState(outState);
        outState.putBoolean(SIS_CONNECTION_STATUS, mDeviceConnected);
        outState.putString(SIS_DEVICE_NAME, mDeviceName);
    }

//    @Override
//    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//        mDeviceConnected = savedInstanceState.getBoolean(SIS_CONNECTION_STATUS);
//        mDeviceName = savedInstanceState.getString(SIS_DEVICE_NAME);
//
//        if (mDeviceConnected) {
//            mConnectButton.setText(R.string.action_disconnect);
//        } else {
//            mConnectButton.setText(R.string.action_connect);
//        }
//    }

//    @Override
//    public boolean onCreateOptionsMenu(final Menu menu) {
//        getMenuInflater().inflate(R.menu.help, menu);
//        return true;
//    }

//    /**
//     * Use this method to handle menu actions other than home and about.
//     *
//     * @param itemId the menu item id
//     * @return <code>true</code> if action has been handled
//     */
//    protected boolean onOptionsItemSelected(final int itemId) {
//        // Overwrite when using menu other than R.menu.help
//        return false;
//    }

//    @Override
//    public boolean onOptionsItemSelected(final MenuItem item) {
//        final int id = item.getItemId();
//        switch (id) {
//            case android.R.id.home:
//                onBackPressed();
//                break;
//            case R.id.action_about:
//                final AppHelpFragment fragment = AppHelpFragment.getInstance(getAboutTextId());
//                fragment.show(getFragmentManager(), "help_fragment");
//
//                break;
//            default:
//                return onOptionsItemSelected(id);
//        }
//        return true;
//    }


    /**
     * Returns the title resource id that will be used to create logger session. If 0 is returned (default) logger will not be used.
     *
     * @return the title resource id
     */
    protected int getLoggerProfileTitle() {
        return 0;
    }

    /**
     * This method may return the local log content provider authority if local log sessions are supported.
     *
     * @return local log session content provider URI
     */
    protected Uri getLocalAuthorityLogger() {
        return null;
    }

    public void connect(final BluetoothDevice device) {
        mBleManager.connect(device);

        // TODO handle these log?
//        final int titleId = getLoggerProfileTitle();
//        if (titleId > 0) {
//            mLogSession = Logger.newSession(getActivity().getApplicationContext(), getString(titleId), device.getAddress(), name);
//            // If nRF Logger is not installed we may want to use local logger
//            if (mLogSession == null && getLocalAuthorityLogger() != null) {
//                mLogSession = LocalLogSession.newSession(getActivity().getApplicationContext(), getLocalAuthorityLogger(), device.getAddress(), name);
//            }
//        }

        mDeviceName = device.getName();
        mBleManager.setLogger(mLogSession);
        mBleManager.connect(device);
    }

    public void disconnect() {
        mBleManager.disconnect();
    }

    // TODO update ui
    @Override
    public void onDeviceConnecting(final BluetoothDevice device) {
//        getActivity().runOnUiThread(() -> {
//            mDeviceNameView.setText(mDeviceName != null ? mDeviceName : getString(R.string.not_available));
//            mConnectButton.setText(R.string.action_connecting);
//        });
    }

    @Override
    public void onDeviceConnected(final BluetoothDevice device) {
        mDeviceConnected = true;
//        getActivity().runOnUiThread(() -> mConnectButton.setText(R.string.action_disconnect));
    }

    @Override
    public void onDeviceDisconnecting(final BluetoothDevice device) {
//        getActivity().runOnUiThread(() -> mConnectButton.setText(R.string.action_disconnecting));
    }

    @Override
    public void onDeviceDisconnected(final BluetoothDevice device) {
        mDeviceConnected = false;
        mBleManager.close();
//        getActivity().runOnUiThread(() -> {
//            mConnectButton.setText(R.string.action_connect);
//			mDeviceNameView.setText(getDefaultDeviceName());
////            mBatteryLevelView.setText(R.string.not_available);
//        });
    }

    @Override
    public void onLinklossOccur(final BluetoothDevice device) {
        mDeviceConnected = false;
//        getActivity().runOnUiThread(() -> {
//            if (mBatteryLevelView != null)
//                mBatteryLevelView.setText(R.string.not_available);
//        });
    }

    @Override
    public void onServicesDiscovered(final BluetoothDevice device, boolean optionalServicesFound) {
        // this may notify user or show some views
    }

    @Override
    public void onDeviceReady(final BluetoothDevice device) {
        // empty default implementation
    }

    @Override
    public void onBondingRequired(final BluetoothDevice device) {
        showToast(R.string.bonding);
    }

    @Override
    public void onBonded(final BluetoothDevice device) {
        showToast(R.string.bonded);
    }

    @Override
    public boolean shouldEnableBatteryLevelNotifications(final BluetoothDevice device) {
        // Yes, we want battery level updates
        return true;
    }

    @Override
    public void onBatteryValueReceived(final BluetoothDevice device, final int value) {
//        runOnUiThread(() -> {
//            if (mBatteryLevelView != null)
//                mBatteryLevelView.setText(getString(R.string.battery, value));
//        });
    }

    @Override
    public void onError(final BluetoothDevice device, final String message, final int errorCode) {
        DebugLogger.e(TAG, "Error occurred: " + message + ",  error code: " + errorCode);
        showToast(message + " (" + errorCode + ")");
    }

    @Override
    public void onDeviceNotSupported(final BluetoothDevice device) {
        showToast(R.string.not_supported);
    }


    /**
     * Shows a message as a Toast notification. This method is thread safe, you can call it from any thread
     *
     * @param message a message to be shown
     */
    protected void showToast(final String message) {
        ((Activity)mContext).runOnUiThread(() -> Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show());
    }

    /**
     * Shows a message as a Toast notification. This method is thread safe, you can call it from any thread
     *
     * @param messageResId an resource id of the message to be shown
     */
    protected void showToast(final int messageResId) {
        ((Activity)mContext).runOnUiThread(() -> Toast.makeText(mContext, messageResId, Toast.LENGTH_SHORT).show());
    }

    /**
     * Returns <code>true</code> if the device is connected. Services may not have been discovered yet.
     */
    protected boolean isDeviceConnected() {
        return mDeviceConnected;
    }

    /**
     * Returns the name of the device that the phone is currently connected to or was connected last time
     */
    protected String getDeviceName() {
        return mDeviceName;
    }

    /**
     * Initializes the Bluetooth Low Energy manager. A manager is used to communicate with profile's services.
     *
     * @return the manager that was created
     */
    protected abstract BleManager<? extends BleManagerCallbacks> initializeManager();

//    /**
//     * Restores the default UI before reconnecting
//     */
//    protected abstract void setDefaultUI();
//
//    /**
//     * Returns the default device name resource id. The real device name is obtained when connecting to the device. This one is used when device has
//     * disconnected.
//     *
//     * @return the default device name resource id
//     */
//    protected abstract int getDefaultDeviceName();
//
//    /**
//     * Returns the string resource id that will be shown in About box
//     *
//     * @return the about resource id
//     */
//    protected abstract int getAboutTextId();
//
//    /**
//     * The UUID filter is used to filter out available devices that does not have such UUID in their advertisement packet.
//     *
//     * @return the required UUID or <code>null</code>
//     */
//    protected abstract UUID getFilterUUID();



    /**
     * Returns the log session. Log session is created when the device was selected using the {@link ScannerFragment} and released when user press DISCONNECT.
     *
     * @return the logger session or <code>null</code>
     */
    protected ILogSession getLogSession() {
        return mLogSession;
    }

}
