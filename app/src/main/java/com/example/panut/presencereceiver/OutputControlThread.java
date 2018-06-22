package com.example.panut.presencereceiver;

import android.app.Activity;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;


/**
// This class control the output to the network class.
// It cumulates the data gotten from writeToBuffer() to various buffers
// then average all the data from all buffers into a single stream of data
 */

public class OutputControlThread extends Thread {

    private final int NUM_DATA_PER_FRAME = 10;
    private short mReadBuffer[] = new short[NUM_DATA_PER_FRAME];
    private static final int DEFAULT_BUFFER_SIZE = 1000;
    private static final int OUTPUT_THREAD_INTERVAL_MS = 10;

    private HashMap<Integer, LocalBuffer> mLocalBuffers;
    private final Object bufferLock = new Object();
    private NetworkStreamer mNetworkStreamer;

    public OutputControlThread() {
        mLocalBuffers = new HashMap<>();
    }

    public void addNewBuffer(int bufferOwnerId) {

        LocalBuffer localBuffer = new LocalBuffer();

        synchronized (bufferLock) {
            mLocalBuffers.put(bufferOwnerId, localBuffer);
        }
    }

    public void deleteBuffer(int bufferOwnerId) {
        synchronized (bufferLock) {
            mLocalBuffers.remove(bufferOwnerId);
        }
    }

    public void writeToBuffer(short[] data, int ownerId) {
        synchronized (bufferLock) {
            // write data to temp buffer
            LocalBuffer localBuffer = mLocalBuffers.get(ownerId);
            if (localBuffer == null) {
                Log.d("MyMonitor", "Error: Audio data changed on uninitialized buffer");
                return;
            }

            localBuffer.write(data);
        }
    }

    public void registerStreamer(NetworkStreamer networkStreamer) {
        mNetworkStreamer = networkStreamer;
    }

    private class LocalBuffer {
        short buffer[];
        int startIndex;
        public int size;
        short readBuffer[]; // buffer for when reading array of data

        LocalBuffer(){
            buffer = new short[DEFAULT_BUFFER_SIZE];
            readBuffer = new short[DEFAULT_BUFFER_SIZE];
            startIndex = 0;
            size = 0;
        }

//        public int size(){
//            int size = endIndex - startIndex;5
//
//            // in case endIndex is less than startIndex;
//            if(size < 0)
//                size += DEFAULT_BUFFER_SIZE;
//
//            return size;
//        }

        int write(short[] data){
            // check available size
            int nWrite = Math.min(DEFAULT_BUFFER_SIZE - size, data.length);

            // buffer full.
            if(nWrite == 0)
                return 0;

            int startWritingIndex = startIndex + size;
            if(startWritingIndex >= DEFAULT_BUFFER_SIZE)
                startWritingIndex -= DEFAULT_BUFFER_SIZE;

            try {
                if (startWritingIndex + nWrite > DEFAULT_BUFFER_SIZE) {
                    int bufferLeftTillEnd = DEFAULT_BUFFER_SIZE - startWritingIndex;
                    System.arraycopy(data, 0, buffer, startWritingIndex, bufferLeftTillEnd);
                    System.arraycopy(data, 0, buffer, 0, nWrite - bufferLeftTillEnd);
                } else {
                    System.arraycopy(data, 0, buffer, startWritingIndex, nWrite);
                }
            }
            catch (ArrayIndexOutOfBoundsException e){
                e.printStackTrace();
            }

            size += nWrite;

            return nWrite;
        }

        /** read <readSize> number of buffer. if the data in the buffer is not enough the rest is 0 filled.
         *  The data read is considered erased from the buffer.
         *
         * @param readSize
         * @return
         */
        public short[] read(int readSize) {
            int nRead = Math.min(readSize, size);

            if (startIndex + nRead > DEFAULT_BUFFER_SIZE - 1) {
                int bufferLeftTillEnd = DEFAULT_BUFFER_SIZE - startIndex;
                System.arraycopy(buffer, startIndex, readBuffer, 0, bufferLeftTillEnd);
                System.arraycopy(buffer, 0, readBuffer, bufferLeftTillEnd, nRead - bufferLeftTillEnd);
            } else {
                System.arraycopy(buffer, startIndex, readBuffer, 0, nRead);
            }

            // re arrange buffer
            startIndex += nRead;
            size -= nRead;
            if(startIndex >= DEFAULT_BUFFER_SIZE)
                startIndex -= DEFAULT_BUFFER_SIZE;

            // zero fill the rest
            if(nRead < readSize){
                Arrays.fill(buffer, nRead, readSize-1, (short)0);
            }

            return readBuffer;
        }
    }

    @Override
    public void run() {
        while (true) {
            Arrays.fill(mReadBuffer, 0, NUM_DATA_PER_FRAME, (short)0);

            int nBuffer;

            synchronized (bufferLock) {
                nBuffer = mLocalBuffers.size();
            }

            if(nBuffer > 0) {

                synchronized (bufferLock) {
                    for (HashMap.Entry<Integer, OutputControlThread.LocalBuffer> iBuffer : mLocalBuffers.entrySet()) {
                        short buffer[] = iBuffer.getValue().read(NUM_DATA_PER_FRAME);

                        // accumulate from all buffer.
                        for (int i = 0; i < NUM_DATA_PER_FRAME; i++) {
                            mReadBuffer[i] += buffer[i];
                        }
                    }
                }

                // average the data
                for (int i = 0; i < NUM_DATA_PER_FRAME; i++) {
                    mReadBuffer[i] /= nBuffer * 5; // blindly amplify
                }

                mNetworkStreamer.sendData(mReadBuffer, NUM_DATA_PER_FRAME);
            }

            try {
                Thread.sleep(OUTPUT_THREAD_INTERVAL_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
