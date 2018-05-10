package com.example.panut.presencereceiver;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created by Panut on 09-May-18.
 */

public class AudioViewModel extends ViewModel {

    public class AudioData {
        public short buffer[];
    }

    private MutableLiveData<AudioData> mAudioBuffer;

//    public LiveData<Queue<Short>> getAudioBuffer(){
//        if(mAudioBuffer == null)
//            mAudioBuffer = new MutableLiveData<Queue<Short>>();
//
//        return mAudioBuffer;
//    }

    public LiveData<AudioData> getAudioBuffer(){
        if(mAudioBuffer == null) {
            mAudioBuffer = new MutableLiveData<AudioData>();
        }
        return mAudioBuffer;
    }

    public void setAudioBuffer(short[] audioData){
        AudioData data = new AudioData();
        data.buffer = audioData;
        mAudioBuffer.setValue(data);
    }

//    public short[] getAllAudioBuffer(int maxSize){
//        Queue<Short> dataQueue = mAudioBuffer.getValue();
//
//        int bufferSize;
//        if(dataQueue.size() < maxSize)
//            bufferSize = dataQueue.size();
//        else
//            bufferSize = maxSize;
//
//        return getAudioBuffer(bufferSize);
//    }

//    public void writeToAudioBuffer(short data[]){
//        if(mAudioBuffer == null) {
//            mAudioBuffer = new MutableLiveData<Queue<Short>>();
//            mAudioBuffer.setValue(new ArrayDeque<>());
//        }
//
//        for(int i = 0; i < data.length; i++)
//            mAudioBuffer.getValue().add(data[i]);
//    }
}
