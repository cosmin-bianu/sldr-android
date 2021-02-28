package com.kitsuneark.slider.handlers;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;


public class VolumeKeyListenerService extends Service {
    //private static final String TAG = "VolumeKeyListener";

    private static MediaSessionCompat mediaSession;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        mediaSession = new MediaSessionCompat(this, "SLDR Service");
        mediaSession.setCallback(new MediaSessionCompat.Callback(){ });
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING,0,0)
                .build());
        VolumeProviderCompat volumeProvider =
                new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE,0,0) {
                    @Override
                    public void onAdjustVolume(int direction) {
                        if (mediaSession.isActive()) {
                            if (direction > 0) MessageHandler.getInstance().onButtonUp();
                            else if (direction < 0) MessageHandler.getInstance().onButtonDown();
                        }
                    }
                };
        mediaSession.setPlaybackToRemote(volumeProvider);
        //Toast.makeText(this, "Service created.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mediaSession.release();
    }

    public static void setIsActive(boolean isActive) {
        mediaSession.setActive(isActive);
    }


}
