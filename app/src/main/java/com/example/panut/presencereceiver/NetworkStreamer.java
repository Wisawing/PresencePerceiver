package com.example.panut.presencereceiver;

import android.util.Log;

//import com.google.common.net.InetAddresses;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayDeque;


public class NetworkStreamer {
    private static final int CONNECTION_PORT = 5076;
    private static final int DATA_SEND_INTERVAL = 12;
    private ServerThread mServerThread;
    private ClientThread mClientThread;
    private NetworkEventListener mEventListener;
    private ArrayDeque<Short> mDataQueue;
    private Object mQueueMutex = new Object();

    public void setEventListener(NetworkEventListener eventListener) {
        this.mEventListener = eventListener;
    }

    public interface NetworkEventListener {
        void onNetworkConnected();
        void onNetworkDisconnected();
        void onNetworkDataReceived(short data[]);
    }

    private class ServerThread extends Thread {
        private ServerSocket mServerSocket;

//        private long receiveCount = 0;
//        private long dropCount = 0;
//        private short previousShort = 0;

        @Override
        public void run() {
            try {
                mServerSocket = new ServerSocket(CONNECTION_PORT);

                while(true) {
                    Socket receiveSocket = mServerSocket.accept();
                    DataInputStream inputStream = new DataInputStream(receiveSocket.getInputStream());

//                    previousShort = inputStream.readShort();
                    while(receiveSocket.isConnected()) {
                        short data = inputStream.readShort();

//                        // test data continuity
//                        int diff = data - previousShort;
//                        if(diff < 0){ // in case overflow
//                            diff += Short.MAX_VALUE+1;
//                        }
//                        diff--;
//
//                        receiveCount++;
//                        dropCount += diff;
//
////                        Log.d("MyMonitor", "Network Data between " + previousShort + " and " + data);
//
//                        if(diff > 0){
//                           Log.d("MyMonitor", "Network Data between " + previousShort + " and " + data);
//
//                        }
//
//                        if(receiveCount%100==0){
//                            Log.d("MyMonitor", "Data received :" + (float)receiveCount/(receiveCount+dropCount)*100 + " %");
//                        }
//
//                        previousShort = data;
//                        // end test data continuity

                        //TODO Not use temp
                        if(mEventListener != null) {
                            short temp[] = new short[1];
                            temp[0] = data;
                            mEventListener.onNetworkDataReceived(temp);
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
            }

        }
    }

    private class ClientThread extends Thread {

        private String mIpAddress;
        private Socket mSocket;

        public ClientThread(String address) {
//            if(InetAddresses.isInetAddress(address)){
//                mIpAddress = address;
//            }
//            else {
//                try {
//                    mIpAddress = InetAddress.getByName(address).toString();
//                } catch (UnknownHostException e) {
//                    e.printStackTrace();
//                    Log.d("MyMonitor", "Invalid IpAddress : " + address);
//                }
//            }

            try {
                mIpAddress = InetAddress.getByName(address).toString();
            } catch (UnknownHostException e) {
                e.printStackTrace();
                mIpAddress = "127.0.0.1";
            }
        }

        @Override
        public void run() {
            try {
                mSocket = new Socket(mIpAddress, CONNECTION_PORT);
                if(mEventListener!=null)
                    mEventListener.onNetworkConnected();

                DataOutputStream outputStream = new DataOutputStream(mSocket.getOutputStream());

                while(true) {
                    while(mSocket.isConnected()) {
                        synchronized (mQueueMutex) {
                            while (!mDataQueue.isEmpty()) {
                                outputStream.writeShort(mDataQueue.pop());
//                                mDataQueue.pop();
//                                outputStream.writeShort(count++);
                            }
                        }
                        Thread.sleep(DATA_SEND_INTERVAL);
                    }

                    if(mEventListener!=null)
                        mEventListener.onNetworkDisconnected();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public NetworkStreamer() {
        mServerThread = new ServerThread();
        mServerThread.start();

        mDataQueue = new ArrayDeque<>();
    }

    public void connect(String ipAddress) {
        mClientThread = new ClientThread(ipAddress);
        mClientThread.start();
    }

    public void sendData(short[] data, int size){
        synchronized (mQueueMutex) {
            for (int i = 0; i < size; i++) {
                mDataQueue.add(data[i]);
            }
        }
    }
}
