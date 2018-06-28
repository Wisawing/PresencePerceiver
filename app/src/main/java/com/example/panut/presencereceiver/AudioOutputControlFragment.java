package com.example.panut.presencereceiver;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;


public class AudioOutputControlFragment extends Fragment {

    /** any sample rate higher than this will result in audio buffer underrun.
     * This is speculated to be because the phone cpu is not fast enough to process all the samples*/
    public static final int AUDIO_SAMPLE_RATE = 11200;

    private static final int OUTPUT_THREAD_INTERVAL_MS = 10;
    private final int MAX_WRITE_BUFFER_SIZE = AUDIO_SAMPLE_RATE;


    private ImageButton mPlayPauseButton;
    private PlayState mPlayState;

    private AudioTrack mAudioTrack;
    private Handler mHandler;

    public enum PlayState {
        STOP,
        PLAY
    }

    public AudioOutputControlFragment() {
        // Required empty public constructor
    }


    public static AudioOutputControlFragment newInstance(String param1, String param2) {
        AudioOutputControlFragment fragment = new AudioOutputControlFragment();
        return fragment;
    }

    private AudioOutputThread mAudioOutputThread;
    private ArrayList<Short> outputBuffer;
    private Object mBufferMutex = new Object();
    private Object mStateMutex = new Object();

    private int freqIndex = 0;
    public void writeBuffer(short data[]){
        synchronized (mBufferMutex){
            for(short i : data)
                outputBuffer.add(i);
        }
    }

    private class AudioOutputThread extends Thread {
//        private long previousFrameTime = 0;
        private short mWriteBuffer[] = new short[MAX_WRITE_BUFFER_SIZE];
        private int mWrittenFrame = 0;
        private long mStartTime = 0;
        private Object mStartTimeMutex = new Object();

//        void startOutput(){
////            previousFrameTime = 0;
//            synchronized (mStartTimeMutex){
//                mStartTime = System.nanoTime();
//                mWrittenFrame = 0;
//            }
//        }

        @Override
        public void run() {
            mStartTime = System.nanoTime();
            mWrittenFrame = 0;

            while(true) {

                // calculate sample size
                long currentTime = System.nanoTime();
//                float timePeriod = (currentTime - previousFrameTime) / 1000000000.f; // in second

                // number of sample according to sample rate
                int bufferSize;

                synchronized (mAudioTrack) {
                    bufferSize = mAudioTrack.getBufferSizeInFrames();
                }

                int nAllSample = (int)((currentTime - mStartTime)/1000000000.f * AUDIO_SAMPLE_RATE) +
                                bufferSize * 2; // try to fill up the buffer. * 2 because 16 bits format.
                int nSample = nAllSample - mWrittenFrame;
//                int nSample;
//                if (previousFrameTime == 0) { // first time still does not have a time period
//                    nSample = AudioOutputControlFragment.AUDIO_SAMPLE_RATE * OUTPUT_THREAD_INTERVAL_MS / 1000;
//                } else {
//                    nSample = (int) (timePeriod * AudioOutputControlFragment.AUDIO_SAMPLE_RATE);
//                }
//                previousFrameTime = currentTime;

                // make sure it does not go over
                if (nSample > MAX_WRITE_BUFFER_SIZE)
                    nSample = MAX_WRITE_BUFFER_SIZE;

                synchronized (mBufferMutex) {
                    if (outputBuffer.size() > 0) { // buffer empty skip interpolation

                        // interpolate data
                        double dataSkipPerSample = (double) (outputBuffer.size() - 1)/ nSample;
                        double dataIndex_d = 0;
                        int dataIndex;
                        int prevDataIndex = -1;
                        short value = 0;

//                        String debug = "";
                        for (int i = 0; i < nSample; i++) {
                            dataIndex = (int)dataIndex_d;

                            if(dataIndex != prevDataIndex) {
                                try {
                                    value = outputBuffer.get(dataIndex);
                                } catch(IndexOutOfBoundsException e){
                                    e.printStackTrace();
                                    Log.d("MyMonitor", "nSample : " + nSample);
                                }

                                // Disable Interpolation because cannot compute in time and will result in underrun.
//                                // linear interpolation between data
//                                float secondValueWeight = dataIndex_d - dataIndex;
//                                if (secondValueWeight > 0.001 && dataIndex_d + 1 < outputBuffer.size()) {
//                                    short secondValue = outputBuffer.get((int) dataIndex_d + 1);
//                                    value = (short) ((1 - secondValueWeight) * (float) value + secondValueWeight * secondValue);
//                                }

                                prevDataIndex = dataIndex;
                            }


                            mWriteBuffer[i] = value;
                            dataIndex_d += dataSkipPerSample;
//                            debug += mWriteBuffer[i] + ", ";
                        }
//                        Log.d("MyMonitor", debug);

                        outputBuffer.clear();
                    }
                    // don't fill with zero because this will create noises when when packet drops.
//                    else { // write zeroes when there is no data.
//                        Arrays.fill(mWriteBuffer, 0, nSample, (short)0);
//                    }
                }

//                // debugging let's try code generated sound first
//                final int amp = 10000;
//                final int freq = 100;
//
//                for(int i = 0; i < nSample; i++){
//                    float sample_f = (float)Math.sin((freqIndex % freq) / (float)freq * Math.PI)*amp;
//                    short sample_s = (short)sample_f;
//                    mWriteBuffer[i] = sample_s;
//
//                    freqIndex++;
//                }

                synchronized (mStateMutex) {
                    switch (mPlayState) {
                        case STOP:
                            break;
                        case PLAY:
                            int written;

                            synchronized (mAudioTrack) {
                                written = mAudioTrack.write(mWriteBuffer, 0, nSample);
                            }

                            if(written != nSample){
                                Log.d("MyMonitor", "Audio Track not correctly written " + written + ":" + nSample);
                            }
                    }
                }

                mWrittenFrame += nSample;

                try {
                    sleep(OUTPUT_THREAD_INTERVAL_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bufferSize =  AudioTrack.getMinBufferSize(AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
//        Log.d("Monitor", "minBufferSize : " + bufferSize);
        mAudioTrack = new AudioTrack.Builder()
            .setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            .setAudioFormat(new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(AUDIO_SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_DEFAULT)
                .build())
            .setBufferSizeInBytes(bufferSize)
            .build();

        mHandler = new Handler();
        mPlayState = PlayState.STOP;

//        Log.d("Monitor", "getBufferSize : " + mAudioTrack.getBufferSizeInFrames());

        synchronized (mBufferMutex) {
            outputBuffer = new ArrayList<>();
        }
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



    private void play() {
        mPlayPauseButton.setImageResource(R.drawable.pause);
        mPlayState = PlayState.PLAY;
        synchronized (mAudioTrack) {
            mAudioTrack.play();
        }
//                        mAudioOutputThread.startOutput();
//                    mNetworkOutput.startOutput();

        Log.d("MyMonitor", "PLAY PRESSED");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_audio_output_control, container, false);

        mPlayPauseButton = view.findViewById(R.id.play_pause_button);
        mPlayPauseButton.setOnClickListener(v -> {
            synchronized (mStateMutex) {
                switch (mPlayState) {
                    case STOP:
                        play();
                        break;
                    case PLAY:
//                    case BUFFER:
                        mPlayPauseButton.setImageResource(R.drawable.play);
                        synchronized (mAudioTrack) {
                            mAudioTrack.pause();
                            mAudioTrack.flush();
                        }
                        mHandler.removeCallbacks(null);
                        mPlayState = PlayState.STOP;

                        Log.d("MyMonitor", "STOP PRESSED");
                }
            }
        });

        play();
        mAudioOutputThread = new AudioOutputThread();
        mAudioOutputThread.start();

        return view;
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
