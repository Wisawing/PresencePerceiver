package com.example.panut.presencereceiver;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.util.Arrays;
import java.util.HashMap;


public class AudioOutputControlFragment extends Fragment implements Observer<AudioViewModel.AudioData>, AudioViewModel.OnBufferChangedListener {

    public static final int AUDIO_SAMPLE_RATE = 8000;
//    private static final int MINIMUM_DATA_TO_WRITE = 10;
    private static final int DEFAULT_BUFFER_SIZE = 1000;
    private static final int OUTPUT_THREAD_INTERVAL_MS = 13; // ~ a little less than 80 ms

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

        // read <readSize> number of buffer. if the data in the buffer is not enough the rest is 0 filled.
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

    private ImageButton mPlayPauseButton;
    private PlayState mPlayState;
//    private AudioViewModel mAudioViewModel;

    private AudioTrack mAudioTrack;

    private Handler mHandler;

    private HashMap<Object, LocalBuffer> mLocalBuffers;
    private final Object bufferLock = new Object();

    @Override
    public void onNewBuffer(Object bufferOwner, LiveData<AudioViewModel.AudioData> buffer) {
        buffer.observe(getActivity(), this);

        LocalBuffer localBuffer = new LocalBuffer();

        synchronized (bufferLock) {
            mLocalBuffers.put(bufferOwner, localBuffer);
        }
    }

    @Override
    public void onBufferDeleted(Object bufferOwner) {
        synchronized (bufferLock) {
            mLocalBuffers.remove(bufferOwner);
        }
    }

    public enum PlayState {
        STOP,
        BUFFER,
        PLAY
    }

    public AudioOutputControlFragment() {
        // Required empty public constructor
    }


    public static AudioOutputControlFragment newInstance(String param1, String param2) {
        AudioOutputControlFragment fragment = new AudioOutputControlFragment();
        return fragment;
    }

    private AudioOutputRunnable mAudioOutputRunnable;

    private class AudioOutputRunnable implements Runnable {
        private long previousFrameTime;
        private final int MAX_WRITE_BUFFER_SIZE = 8096;
        private final int NUM_DATA_PER_FRAME = 10;
        private short mReadBuffer[] = new short[NUM_DATA_PER_FRAME];
        private short mWriteBuffer[] = new short[MAX_WRITE_BUFFER_SIZE];

        void startOutput(){
            previousFrameTime = 0;
        }

//            private int freqIndex = 0;
        @Override
        public void run() {
            while (true) {
                switch (mPlayState) {
                    case PLAY:

                        long currentTime = System.nanoTime();
                        float timePeriod = (currentTime - previousFrameTime) / 1000000000.f; // in second
//                        Log.d("Monitor", "time period" + timePeriod);
                        // number of sample according to sample rate
                        int nSample;
                        if(previousFrameTime == 0) { // first time still does not have a time period
                            nSample = AudioOutputControlFragment.AUDIO_SAMPLE_RATE * OUTPUT_THREAD_INTERVAL_MS / 1000;
                        }
                        else {
                            nSample =(int) (timePeriod * AudioOutputControlFragment.AUDIO_SAMPLE_RATE);
                        }
                        previousFrameTime = currentTime;

                        // make sure it does not go over
                        if (nSample > MAX_WRITE_BUFFER_SIZE)
                            nSample = MAX_WRITE_BUFFER_SIZE;



                        Arrays.fill(mReadBuffer, 0, NUM_DATA_PER_FRAME, (short)0);

                        int nBuffer;
                        synchronized (bufferLock) {
                            nBuffer = mLocalBuffers.size();
                            for (HashMap.Entry<Object, LocalBuffer> iBuffer : mLocalBuffers.entrySet()) {
                                short buffer[] = iBuffer.getValue().read(NUM_DATA_PER_FRAME);

                                // accumulate from all buffer.
                                for (int i = 0; i < NUM_DATA_PER_FRAME; i++) {
                                    mReadBuffer[i] += buffer[i];
                                }
                            }
                        }

                        if(nBuffer > 0) { // buffer empty skip interpolation
                            // average the data
                            for (int i = 0; i < NUM_DATA_PER_FRAME; i++) {
                                mReadBuffer[i] /= nBuffer * 5; // blindly amplify
                            }

                            // interpolate data

                            float dataSkipPerSample = (float) mReadBuffer.length / nSample;
                            float dataIndex = 0;

//                        String debugS = "";

                            for (int i = 0; i < nSample; i++) {
                                short value = mReadBuffer[(int) dataIndex];

                                // linear interpolation between data
                                float secondValueWeight = dataIndex - (int) dataIndex;
                                if( secondValueWeight > 0.001 && dataIndex + 1 < mReadBuffer.length) {
                                    short secondValue = mReadBuffer[(int)dataIndex+1];
                                    value = (short)((1-secondValueWeight)*(float)value + secondValueWeight*secondValue);
                                }
                                mWriteBuffer[i] = value;
                                dataIndex += dataSkipPerSample;
//                            debugS += mWriteBuffer[i] + ", ";
                            }
                        }


//                        // debugging let's try code generated sound first
////                        final int nSample = 512;
//                        final int amp = 10000;
//                        final int freq = 100;
////                        short samples[] = new short[nSample];
//
//                        for(int i = 0; i < nSample; i++){
//                            float sample_f = (float)Math.sin((freqIndex % freq) / (float)freq * Math.PI)*amp;
//                            short sample_s = (short)sample_f;
//                            mWriteBuffer[i] = sample_s;
//
//                            freqIndex++;
//                        }

//                        Log.d("Monitor", debugS);

                        mAudioTrack.write(mWriteBuffer, 0, nSample);
                        break;
                    case STOP: // do nothing for now

//                        Log.d("Monitor", "STOP");
                        break;
                }

                try {
                    Thread.sleep(OUTPUT_THREAD_INTERVAL_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bufferSize =  AudioTrack.getMinBufferSize(AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack.Builder()
            .setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            .setAudioFormat(new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(AUDIO_SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build())
            .setBufferSizeInBytes(bufferSize)
            .build();

        mLocalBuffers = new HashMap<>();

//        mAudioViewModel = ViewModelProviders.of(getActivity()).get(AudioViewModel.class);

        mHandler = new Handler();
        mPlayState = PlayState.STOP;
        mAudioOutputRunnable = new AudioOutputRunnable();
        Thread mOutputThread = new Thread(mAudioOutputRunnable);
        mOutputThread.start();
    }




//    public void updateBuffer(){
//
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                // write data to buffer
//                short data[] = mAudioViewModel.getAudioBuffer(100);
//                mAudioTrack.write(data, 0, data.length);
//                updateBuffer();
//            }
//        }, 125); // ~ 800 fps
//
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_audio_output_control, container, false);

        mPlayPauseButton = view.findViewById(R.id.play_pause_button);
        mPlayPauseButton.setOnClickListener(v -> {
            switch (mPlayState){
                case STOP:
                    mPlayPauseButton.setImageResource(R.drawable.pause);
                    mPlayState = PlayState.PLAY;

                    mAudioTrack.play();
                    mAudioOutputRunnable.startOutput();

                    Log.d("Monitor", "PLAY PRESSED");
                    break;
                case PLAY:
                case BUFFER:
                    mPlayPauseButton.setImageResource(R.drawable.play);
                    mAudioTrack.stop();
                    mHandler.removeCallbacks(null);
                    mPlayState = PlayState.STOP;

                    Log.d("Monitor", "STOP PRESSED");
            }
        });

        return view;
    }




    // on audio data changed
    @Override
    public void onChanged(@Nullable AudioViewModel.AudioData audioData) {
        if(audioData == null || mAudioTrack == null)
            return;

        synchronized (bufferLock) {
            // write data to temp buffer
            LocalBuffer localBuffer = mLocalBuffers.get(audioData.owner);
            if (localBuffer == null) {
                Log.d("Monitor", "Error: Audio data changed on uninitialized buffer");
                return;
            }

            localBuffer.write(audioData.buffer);
        }

        // now writing to audio buffer in thread
        // should always write whether or not the data is ready.
//        // check if we should write to track
//        boolean shouldWrite = false;
//
//        // check if data is full then we should just write
//        if(nWritten < audioData.buffer.length)
//            shouldWrite = true;
//
//        // check if all data in all buffer are ready
//        if(!shouldWrite) {
//            boolean hasData = true;
//
//            for (HashMap.Entry<Object, LocalBuffer> iBuffer : mLocalBuffers.entrySet()) {
//                if (iBuffer.getValue().size < MINIMUM_DATA_TO_WRITE)
//                    hasData = false;
//            }
//
//            if(hasData)
//                shouldWrite = true;
//        }
//
//        // write data.
//        if(shouldWrite) {
//        }

//        Log.d("Monitor", audioData.id + "");
//        int nSampleWritten = mAudioTrack.write(audioData.buffer, 0, audioData.buffer.length);
//
//        int nSampleLeftOver = audioData.buffer.length - nSampleWritten;
//
//        if(nSampleLeftOver > 0){
//            Log.d("Monitor", "Left over data to write : " + nSampleLeftOver + ", data written : " + nSampleWritten);
//        }
    }


    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
