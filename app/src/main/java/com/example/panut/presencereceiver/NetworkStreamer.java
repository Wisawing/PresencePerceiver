package com.example.panut.presencereceiver;

import android.util.Log;

//import com.google.common.net.InetAddresses;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
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

    public void disconnect() {
        mClientThread.disconnect();
        mServerThread.disconnect();
    }

    public interface NetworkEventListener {
        void onNetworkConnected();
        void onNetworkDisconnected();
        void onNetworkDataReceived(short data[]);
    }

    private class ServerThread extends Thread {
        private ServerSocket mServerSocket;

        private long receiveCount = 0;
        private volatile boolean mKeepAlive = false;

        @Override
        public void run() {
            mKeepAlive = true;

            try {
                while(mKeepAlive) {
                    mServerSocket = new ServerSocket(CONNECTION_PORT);
                    Socket receiveSocket = mServerSocket.accept(); // this block until a connection is made.

                    Log.d("MyMonitor", "Server received a connection");

                    /** try to connect two ways */
                    connect(receiveSocket.getInetAddress().getHostAddress());

                    DataInputStream inputStream = new DataInputStream(receiveSocket.getInputStream());

//                    previousShort = inputStream.readShort();
                    while(mKeepAlive && receiveSocket.isConnected()) {
                        short data = inputStream.readShort();

//                        // test data continuity
//                        int diff = data - previousShort;
//                        if(diff < 0){ // in case overflow
//                            diff += Short.MAX_VALUE+1;
//                        }
//                        diff--;
//
//                        dropCount += diff;
//
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

                        if(receiveCount % 1000 == 0){
                            Log.d("MyMonitor", "Received data from client : "  + data);
                        }

                        receiveCount++;

                        if(mEventListener != null) {
                            short temp[] = new short[1];
                            temp[0] = data;
                            mEventListener.onNetworkDataReceived(temp);
                        }
                    }
                }
            } catch (SocketException e) {

            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if(mServerSocket != null)
                    mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.d("MyMonitor", "Server Closed");
        }

        public void disconnect() {
            mKeepAlive = false;
        }
    }

    private class ClientThread extends Thread {
        private String mIpAddress;
        private Socket mSocket;
        private volatile boolean mKeepAlive;

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
            mIpAddress = address;
        }

        /** disconnect function for manual use */
        public void disconnect(){
            mKeepAlive = false;
        }

        /** If the ClientThread is trying to connect or already connected
         * the @Link{mKeepAlive} variable will be set to true.
         * @return
         */
        public boolean isConnected(){
            return mKeepAlive;
        }

        @Override
        public void run() {
            try {
                mIpAddress = InetAddress.getByName(mIpAddress).getHostAddress();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
//          mIpAddress = "192.168.91.194"; // temp for local testing TODO remove this
            mKeepAlive = true;

            while(mKeepAlive) {
                try {
                    mSocket = new Socket(mIpAddress, CONNECTION_PORT);
                    if (mEventListener != null)
                        mEventListener.onNetworkConnected();

                    Log.d("MyMonitor", "Client connected to server at " + mIpAddress);

                    DataOutputStream outputStream = new DataOutputStream(mSocket.getOutputStream());

                    while (mKeepAlive && mSocket.isConnected()) {
                        synchronized (mQueueMutex) {
                            while (!mDataQueue.isEmpty()) {
                                outputStream.writeShort(mDataQueue.pop());

////                                mDataQueue.pop();
////                                outputStream.writeShort(count++);
                            }
                        }
                        Thread.sleep(DATA_SEND_INTERVAL);
                    }

                    if (mEventListener != null)
                        mEventListener.onNetworkDisconnected();
                }catch (SocketException e) {
                    if(mEventListener!=null)
                        mEventListener.onNetworkDisconnected();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                if(mSocket != null)
                    mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.d("MyMonitor", "Client Closed");
        }

    }

    public NetworkStreamer() {
        mServerThread = new ServerThread();
        mServerThread.start();

        mDataQueue = new ArrayDeque<>();
    }

    public boolean isConnectionAlive(){
        return mClientThread != null && mClientThread.isConnected();
    }

    public void connect(String ipAddress) {
        // only connect when it isn't already connect.
        if(isConnectionAlive())
            return;

        mClientThread = new ClientThread(ipAddress);
        mClientThread.start();
    }

    public void sendData(short[] data, int size){
        synchronized (mQueueMutex) {
            if(mDataQueue.size() < 20) {
                for (int i = 0; i < size; i++) {
                    mDataQueue.add(data[i]);
                }
            }
        }
    }
}
