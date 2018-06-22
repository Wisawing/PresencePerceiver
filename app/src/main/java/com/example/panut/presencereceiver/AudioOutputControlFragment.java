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


public class AudioOutputControlFragment extends Fragment {

    /** any sample rate higher than this will result in audio buffer underrun.
     * This is because the phone cpu is not fast enough to process all the samples*/
    public static final int AUDIO_SAMPLE_RATE = 11200;

    private static final int OUTPUT_THREAD_INTERVAL_MS = 10;
    private final int MAX_WRITE_BUFFER_SIZE = AUDIO_SAMPLE_RATE;


    private ImageButton mPlayPauseButton;
    private PlayState mPlayState;

    private AudioTrack mAudioTrack;

    private Handler mHandler;

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
        private long previousFrameTime = 0;
        private short mWriteBuffer[] = new short[MAX_WRITE_BUFFER_SIZE];

        void startOutput(){
            previousFrameTime = 0;
        }

        @Override
        public void run() {
            while(true) {

                // calculate sample size
                long currentTime = System.nanoTime();
                float timePeriod = (currentTime - previousFrameTime) / 1000000000.f; // in second

                // number of sample according to sample rate
                int nSample;
                if (previousFrameTime == 0) { // first time still does not have a time period
                    nSample = AudioOutputControlFragment.AUDIO_SAMPLE_RATE * OUTPUT_THREAD_INTERVAL_MS / 1000;
                } else {
                    nSample = (int) (timePeriod * AudioOutputControlFragment.AUDIO_SAMPLE_RATE);
                }
                previousFrameTime = currentTime;

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
//                            debugS += mWriteBuffer[i] + ", ";
                        }
                    }
                    outputBuffer.clear();
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
                        case BUFFER:
                            int written = mAudioTrack.write(mWriteBuffer, 0, nSample);
                            if(written != nSample){
                                Log.d("MyMonitor", "Audio Track not correctly written " + written + ":" + nSample);
                            }
                    }
                }

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

        synchronized (mBufferMutex) {
            outputBuffer = new ArrayList<>();
        }

        mAudioOutputThread = new AudioOutputThread();
        mAudioOutputThread.start();
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
            synchronized (mStateMutex) {
                switch (mPlayState) {
                    case STOP:
                        mPlayPauseButton.setImageResource(R.drawable.pause);
                        mPlayState = PlayState.PLAY;

                        mAudioTrack.play();
                        mAudioOutputThread.startOutput();
//                    mNetworkOutput.startOutput();

                        Log.d("MyMonitor", "PLAY PRESSED");
                        break;
                    case PLAY:
                    case BUFFER:
                        mPlayPauseButton.setImageResource(R.drawable.play);
                        mAudioTrack.pause();
                        mAudioTrack.flush();
                        mHandler.removeCallbacks(null);
                        mPlayState = PlayState.STOP;

                        Log.d("MyMonitor", "STOP PRESSED");
                }
            }
        });

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
