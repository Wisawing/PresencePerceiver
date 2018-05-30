package com.example.panut.presencereceiver;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;

public class NetworkStreamer {
    private static final int CONNECTION_PORT = 5076;
    private static final int DATA_SEND_INTERVAL = 12;
    private ServerThread mServerThread;
    private ClientThread mClientThread;
    private NetworkEventListener mEventListener;
    private ArrayDeque<Short> mDataQueue;
    private Object mQueueMutex = new Object();

    public interface NetworkEventListener {
        void onNetworkConnected();
        void onNetworkDisconnected();
        void onNetworkDataReceived(short data[]);
    }

    private class ServerThread extends Thread {
        private ServerSocket mServerSocket;
//        private Socket mReceiveSocket;

        @Override
        public void run() {
            try {
                mServerSocket = new ServerSocket(CONNECTION_PORT);

                while(true) {
                    Socket receiveSocket = mServerSocket.accept();
                    DataInputStream inputStream = new DataInputStream(receiveSocket.getInputStream());

                    while(receiveSocket.isConnected()) {

                        short data = inputStream.readShort();

//                        Log.d("Monitor", data + "");
//                        Thread.sleep(DATA_SEND_INTERVAL);

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

        private final String mIpAddress;
        private Socket mSocket;

        public ClientThread(String ipAddress) {
            mIpAddress = ipAddress;
        }

        @Override
        public void run() {
            try {
                mSocket = new Socket(mIpAddress, CONNECTION_PORT);
                if(mEventListener!=null)
                    mEventListener.onNetworkConnected();

                while(true) {
                    DataOutputStream outputStream = new DataOutputStream(mSocket.getOutputStream());

                    while(mSocket.isConnected()) {
                        synchronized (mQueueMutex) {
                            while (!mDataQueue.isEmpty())
                                outputStream.writeShort(mDataQueue.pop());
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

    public void sendData(short[] data){
        synchronized (mQueueMutex) {
            for (int i = 0; i < data.length; i++) {
                mDataQueue.add(data[i]);
            }
        }
    }
}
