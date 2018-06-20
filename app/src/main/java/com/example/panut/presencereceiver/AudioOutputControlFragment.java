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

    public static final int AUDIO_SAMPLE_RATE = 8000;
//    private static final int MINIMUM_DATA_TO_WRITE = 10;
    private static final int OUTPUT_THREAD_INTERVAL_MS = 13; // ~ a little less than 80 ms
    private final int MAX_WRITE_BUFFER_SIZE = 8096;


    private ImageButton mPlayPauseButton;
    private PlayState mPlayState;
//    private AudioViewModel mAudioViewModel;

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

    public void writeBuffer(short data[]){
        synchronized (mBufferMutex){
            for(short i : data)
                outputBuffer.add(i);
        }
    }

    private class AudioOutputThread extends Thread {
        private long previousFrameTime;
        private short mWriteBuffer[] = new short[MAX_WRITE_BUFFER_SIZE];

        void startOutput(){
            previousFrameTime = 0;
        }

        @Override
        public void run() {
            synchronized (mBufferMutex) {
                outputBuffer = new ArrayList<>();
            }

            // calculate sample size
            long currentTime = System.nanoTime();
            float timePeriod = (currentTime - previousFrameTime) / 1000000000.f; // in second

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

            synchronized (mBufferMutex) {
                if (outputBuffer.size() > 0) { // buffer empty skip interpolation

                    // interpolate data
                    float dataSkipPerSample = (float) outputBuffer.size() / nSample;
                    float dataIndex = 0;

                    for (int i = 0; i < nSample; i++) {
                        short value = outputBuffer.get((int) dataIndex);

                        // linear interpolation between data
                        float secondValueWeight = dataIndex - (int) dataIndex;
                        if (secondValueWeight > 0.001 && dataIndex + 1 < outputBuffer.size()) {
                            short secondValue = outputBuffer.get((int) dataIndex + 1);
                            value = (short) ((1 - secondValueWeight) * (float) value + secondValueWeight * secondValue);
                        }
                        mWriteBuffer[i] = value;
                        dataIndex += dataSkipPerSample;
//                            debugS += mWriteBuffer[i] + ", ";
                    }
                }
            }

            mAudioTrack.write(mWriteBuffer, 0, nSample);


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

//                        Log.d("MyMonitor", debugS);

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

        mHandler = new Handler();
        mPlayState = PlayState.STOP;
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
            switch (mPlayState){
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
                    mAudioTrack.stop();
                    mHandler.removeCallbacks(null);
                    mPlayState = PlayState.STOP;

                    Log.d("MyMonitor", "STOP PRESSED");
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
