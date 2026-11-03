package com.bike.trailgo;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import java.util.HashMap;

public class SoundManager {
    private SoundPool mSoundPool;
    private HashMap<Integer, Integer> mSoundPoolMap;
    private AudioManager mAudioManager;
    private Context mContext;

    public SoundManager(Context mContext, SoundPool mSoundPool){
        this.mContext = mContext;
        this.mSoundPool = mSoundPool;
        mSoundPoolMap = new HashMap<Integer, Integer>();
        mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    public void addSound(int index, int soundId){
        mSoundPoolMap.put(index,mSoundPool.load(mContext,soundId,1));
    }

    public int playSound(int index){
        int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        return mSoundPool.play(mSoundPoolMap.get(index),streamVolume,streamVolume,1,0,1f);
    }

    public void stopSound(int streamId){
        mSoundPool.stop(streamId);
    }

    public void pauseSound(int streamId){
        mSoundPool.pause(streamId);
    }

    public void resumeSound(int streamId){
        mSoundPool.resume(streamId);
    }



}
