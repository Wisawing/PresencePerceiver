package com.example.panut.presencereceiver;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ConnectionDetailActivity extends Activity implements Button.OnClickListener{

    private enum ConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED
    };

    public static final String EXTRA_DEVICE_NAME = "device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";

    private String mDeviceName;
    private String mDeviceAddress;

    private TextView mDeviceNameView;
    private TextView mDeviceAddressView;
    private TextView mConnectionStatusView;
    private TextView mDataReceivedView;
    private Button mConnectButton;

    private ConnectionStatus mIsConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_detail);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);

        mDeviceNameView = (TextView) findViewById(R.id.DeviceName);
        mDeviceAddressView = (TextView) findViewById(R.id.MacAdress);
        mConnectionStatusView = (TextView) findViewById(R.id.ConnectionStatus);
        mDataReceivedView = (TextView) findViewById(R.id.ReceivedData);
        mConnectButton = (Button) findViewById(R.id.ConnectButton);

        mDeviceNameView.setText("Device : " + mDeviceName);
        mDeviceAddressView.setText("Address : " + mDeviceAddress);

        mConnectButton.setOnClickListener(this);

        setConnectionState(ConnectionStatus.DISCONNECTED);
    }

    @Override
    public void onClick(View v) {
        switch (mIsConnected) {
            case DISCONNECTED:
                setConnectionState(ConnectionStatus.CONNECTED);
                break;
            case CONNECTING:
                break;
            case CONNECTED:
                setConnectionState(ConnectionStatus.DISCONNECTED);
                break;
        }
    }

    private void setConnectionState(ConnectionStatus state)
    {
        mIsConnected = state;

        String connectionStatusText = "Status : ";
        String ButtonText = "";

        switch (mIsConnected) {
            case DISCONNECTED:
                connectionStatusText += "Disconnected";
                ButtonText = "Connect";
                break;
            case CONNECTING:
                connectionStatusText += "Connecting";
                ButtonText = "Connecting";
                break;
            case CONNECTED:
                connectionStatusText += "Connected";
                ButtonText = "Disconnect";
                break;
        }

        mConnectionStatusView.setText((connectionStatusText));
        mConnectButton.setText(ButtonText);
    }
}
