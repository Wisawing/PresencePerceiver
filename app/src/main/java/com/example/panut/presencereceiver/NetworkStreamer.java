package com.example.panut.presencereceiver;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayDeque;


public class NetworkStreamer {
    private static final int CONNECTION_PORT = 5076;
    private static final int DATA_SEND_INTERVAL = 12;
    private static final int MAXIMUM_PACKET_SIZE = 10; // size in short
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
//        mServerThread.disconnect();
    }

    public interface NetworkEventListener {
        /** client events */
        void onNetworkConnected();
        void onNetworkDisconnected();

        /** server events */
        void onNetworkDataReceived(short data[]);
    }

    private class ServerThread extends Thread {
//        private ServerSocket mServerSocket;
        private DatagramSocket mSocket;
        private byte[] mReceiveBuffer = new byte[MAXIMUM_PACKET_SIZE*2];
        private DatagramPacket mPacket = new DatagramPacket(mReceiveBuffer, mReceiveBuffer.length);

        private long receiveCount = 0;
        private volatile boolean mKeepAlive = false;

//        private short previousShort = -1;
//        private int dropCount = 0;

        @Override
        public void run() {
            mKeepAlive = true;

            /** always keep the server opened */
            while(true) {
                try {
                    mSocket = new DatagramSocket(CONNECTION_PORT);

                    mSocket.receive(mPacket);

                    /** try to connect two ways */
                    connect(mPacket.getAddress().getHostAddress());

                    while(mKeepAlive && !mSocket.isClosed()) {
                        mSocket.receive(mPacket);
//                        byte data[] = mPacket.getData();
                        int nData = mPacket.getLength();
                        short data_s[] = new short[nData/2];

                        /** parse from byte to short */
                        for(int i = 0; i < nData/2; i++){
                            int index = i*2;
                            data_s[i] = (short)(((mReceiveBuffer[index]&0xff)<<8) + (mReceiveBuffer[index+1]&0xff));
                        }

                        /** test data continuity */
//                        for(int i = 0; i < nData/2; i++) {
//                            int diff = data_s[i] - previousShort;
//                            if (diff < 0) { // in case overflow
//                                diff += Short.MAX_VALUE;
//                            }
//                            diff--;
//
//                            dropCount += diff;
//
//                            if (diff != 0) {
//                                Log.d("MyMonitor", "Network Data between " + previousShort + " and " + data_s[i]);
//                            }
//
//                            previousShort = data_s[i];
//                        }
//                        if (receiveCount % 100 == 0) {
//                            Log.d("MyMonitor", "Data received :" + (float) (receiveCount*10) / ((receiveCount*10) + dropCount) * 100 + " %");
//                        }
                        /** end test data continuity */

                        if(nData >= 2 && receiveCount % 300 == 0){
                            Log.d("MyMonitor", "Received data from client : "  + data_s[0]);
                        }

                        receiveCount++;

                        if(mEventListener != null && data_s.length != 0) {
                            mEventListener.onNetworkDataReceived(data_s);
                        }
                    }

                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e){
                    e.printStackTrace();
                }

                if(mSocket != null)
                    mSocket.close();

                Log.d("MyMonitor", "Server Closed");
            }
        }

        public void disconnect() {
            mKeepAlive = false;
        }
    }

    private class ClientThread extends Thread {
        private String mIpAddress;
//        private Socket mSocket;
        private DatagramSocket mSocket;
        private DatagramPacket mPacket;
        private byte[] mSendBuffer = new byte[MAXIMUM_PACKET_SIZE*2];

        private volatile boolean mKeepAlive;

//        short count = 0;

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
            mPacket = new DatagramPacket(mSendBuffer, MAXIMUM_PACKET_SIZE);
            mPacket.setPort(CONNECTION_PORT);
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
            InetAddress inetAddress;

            mKeepAlive = true;

            try {
                inetAddress = InetAddress.getByName(mIpAddress);
                mPacket.setAddress(inetAddress);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                mKeepAlive = false;
            }

            while(mKeepAlive) {
                try {
                    mSocket = new DatagramSocket();
                    if (mEventListener != null)
                        mEventListener.onNetworkConnected();

                    Log.d("MyMonitor", "Client connected to server at " + mIpAddress);

                    /** notify server the start of the connection */
                    mPacket.setLength(0);
                    mSocket.send(mPacket);


                    while (mKeepAlive && !mSocket.isClosed()) {

                        int nData = 0;

                        synchronized (mQueueMutex) {
                            /** send data in a chunk of 10 or less */

                            nData = Math.min(mDataQueue.size(), 10);

                            for(int i = 0; i < nData; i++){
                                int bufferIndex = i*2;
                                short iData = mDataQueue.pop();
                                mSendBuffer[bufferIndex] = (byte) ((iData >> 8)&0xff);
                                mSendBuffer[bufferIndex+1] = (byte) (iData &0xff);
                            }
                        }

//                        /** test data continuity */
//                        nData = MAXIMUM_PACKET_SIZE;
//                        for (int i = 0; i < nData; i++) {
//                            int bufferIndex = i * 2;
//                            short iData = count++;
//                            mSendBuffer[bufferIndex] = (byte) ((iData >> 8) & 0xff);
//                            mSendBuffer[bufferIndex + 1] = (byte) (iData & 0xff);
//                        }
//                        /** end test data continuity */

//                        mPacket.setData(mSendBuffer);
                        if (nData > 0){
                            mPacket.setLength(nData * 2);
                            mSocket.send(mPacket);
                        }

                        Thread.sleep(DATA_SEND_INTERVAL);
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Log.d("MyMonitor", "Client Disconnected");
            }

            if(mSocket != null)
                mSocket.close();
            if(mEventListener!=null)
                mEventListener.onNetworkDisconnected();

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
