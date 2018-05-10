package com.example.panut.presencereceiver;

import android.annotation.TargetApi;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

/**
 * A fragment with a Google +1 button.
 * Activities that contain this fragment must implement the
 * {@link AudioOutputControlFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AudioOutputControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AudioOutputControlFragment extends Fragment implements Observer<AudioViewModel.AudioData> {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static final int AUDIO_SAMPLE_RATE = 8000;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private ImageButton mPlayPauseButton;
    private PlayState mPlayState;
    private AudioViewModel mAudioViewModel;

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

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AudioOutputControlFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AudioOutputControlFragment newInstance(String param1, String param2) {
        AudioOutputControlFragment fragment = new AudioOutputControlFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        int bufferSize =  AudioTrack.getMinBufferSize(AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack.Builder()
            .setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            .setAudioFormat(new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(AUDIO_SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build())
            .setBufferSizeInBytes(bufferSize)
            .build();

        mAudioViewModel = ViewModelProviders.of(getActivity()).get(AudioViewModel.class);
        mHandler = new Handler();
        mPlayState = PlayState.STOP;
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
        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mPlayState){
                    case STOP:
                        mPlayPauseButton.setImageResource(R.drawable.pause);
                        mPlayState = PlayState.PLAY;

                        mAudioTrack.play();

                        break;
                    case PLAY:
                    case BUFFER:
                        mPlayPauseButton.setImageResource(R.drawable.play);
                        mAudioTrack.stop();
                        mHandler.removeCallbacks(null);
                        mPlayState = PlayState.STOP;

                }
            }
        });

        return view;
    }

    // on audio data changed
    @Override
    public void onChanged(@Nullable AudioViewModel.AudioData audioData) {
        if(audioData == null || mAudioTrack == null)
            return;

        int nSampleWritten = mAudioTrack.write(audioData.buffer, 0, audioData.buffer.length);

        int nSampleLeftOver = audioData.buffer.length - nSampleWritten;

        if(nSampleLeftOver > 0){
            Log.d("Monitor", "Left over data to write : " + nSampleLeftOver + ", data written : " + nSampleWritten);
        }

//        int nUnderrun = mAudioTrack.getUnderrunCount();
//        if(nUnderrun > 0){
//            Log.d("Monitor", "Underrun : " + nUnderrun);
//        }
    }


    @Override
    public void onResume() {
        super.onResume();


    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

}
