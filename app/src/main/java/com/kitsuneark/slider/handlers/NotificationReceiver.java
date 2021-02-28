package com.kitsuneark.slider.handlers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kitsuneark.slider.activities.RemoteControlActivity;

public class NotificationReceiver extends BroadcastReceiver{
    //private static String TAG = "NotificationReceiver";

    BackgroundServiceHandler bsh;

    public NotificationReceiver(Context context, Activity activity){
        bsh = new BackgroundServiceHandler(context, activity);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        bsh.backgroundServiceOff(RemoteControlActivity.buttons
                .get(RemoteControlActivity.ButtonType.BACKGROUND_SERVICE));
    }

    public BackgroundServiceHandler getBackgroundServiceHandler() {
        return bsh;
    }
}
