package com.example.panut.presencereceiver;

import android.bluetooth.BluetoothClass;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.provider.MediaStore;

/**
 * Created by Panut on 30-Jan-18.
 */

public class SoundOutput {
    private AudioTrack _track;
    private final int SAMPLE_RATE = 8192;
    private final int ENCODING = AudioFormat.ENCODING_PCM_FLOAT;

    public void init(){
        int mBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, ENCODING);
        _track = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                ENCODING,
                mBufferSize,
                AudioTrack.MODE_STREAM);
    }
    public void write(float[] data, int size) {
        _track.write(data, 0, size, AudioTrack.WRITE_NON_BLOCKING);
    };

    public void play(){
        if(_track.getPlayState() != AudioTrack.PLAYSTATE_PLAYING)
            _track.play();
    }
}
