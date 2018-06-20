//package com.example.panut.presencereceiver;
//
//import android.arch.lifecycle.LiveData;
//import android.arch.lifecycle.MutableLiveData;
//import android.arch.lifecycle.ViewModel;
//
//import java.util.HashMap;
//
///**
// * Created by Panut on 09-May-18.
// */
//
//public class AudioViewModel extends ViewModel {
//
//    public interface OnBufferChangedListener {
//        void onNewBuffer(Object bufferOwner, LiveData<AudioData> buffer);
//        void onBufferDeleted(Object bufferOwner);
//    }
//
//    public class AudioData {
//        public Object owner;
//        public short buffer[];
//    }
//
//    private HashMap<Object, MutableLiveData<AudioData>> mAudioBuffers;
//    private OnBufferChangedListener mBufferChangedListener;
//
//    // TODO this method is a bit odd. It currently both init and getter in the same method. refactoring recommended
//    // make sure for this owner has already existed
//    public LiveData<AudioData> initializeBuffer(Object owner){
//        if(mAudioBuffers == null) {
//            mAudioBuffers = new HashMap<>();
//        }
//
//        MutableLiveData<AudioData> buffer = mAudioBuffers.get(owner);
//
//        if(buffer == null) {
//            buffer = new MutableLiveData<>();
//            mAudioBuffers.put(owner, buffer);
//        }
//
//        mBufferChangedListener.onNewBuffer(owner, buffer);
//
//        return buffer;
//    }
//
//    public void setBufferChangedListener(OnBufferChangedListener bufferChangedListener) {
//        this.mBufferChangedListener = bufferChangedListener;
//    }
//
//    public void removeBuffer(Object owner) {
//        mAudioBuffers.remove(owner);
//        mBufferChangedListener.onBufferDeleted(owner);
//
//    }
//
////    public LiveData<AudioData> getAudioBuffer(Object owner){
////
////        LiveData<AudioData> buffer = initializeBuffer(owner);
////
////        return buffer;
////    }
//
////    public void observeAllBuffer(LifecycleOwner observerOwner, Observer<AudioData> observer) {
////        for(Map.Entry<Object, MutableLiveData<AudioData>> iEntry : mAudioBuffers.entrySet()){
////            iEntry.getValue().observe(observerOwner, observer);
////        }
////    }
//
//
//    public void writeAudioBuffer(short[] audioData, int ownerID){
//        MutableLiveData<AudioData> liveBuffer = mAudioBuffers.get(ownerID);
//
//        AudioData data = new AudioData();
//        data.buffer = audioData;
//        data.owner = ownerID;
//
//        liveBuffer.postValue(data); // post data can be called outside of the main thread
//
//        // TODO Check Data Continuity using live data might skip some value if the data is set within the same frame
//    }
//
////    public short[] getAllAudioBuffer(int maxSize){
////        Queue<Short> dataQueue = mAudioBuffer.getValue();
////
////        int bufferSize;
////        if(dataQueue.size() < maxSize)
////            bufferSize = dataQueue.size();
////        else
////            bufferSize = maxSize;
////
////        return getAudioBuffer(bufferSize);
////    }
//
////    public void writeToAudioBuffer(short data[]){
////        if(mAudioBuffer == null) {
////            mAudioBuffer = new MutableLiveData<Queue<Short>>();
////            mAudioBuffer.setValue(new ArrayDeque<>());
////        }
////
////        for(int i = 0; i < data.length; i++)
////            mAudioBuffer.getValue().add(data[i]);
////    }
//}
