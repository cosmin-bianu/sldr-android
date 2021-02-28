package com.kitsuneark.slider.handlers;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.PowerManager;
import android.support.constraint.ConstraintLayout;
import android.widget.Toast;

import com.kitsuneark.slider.R;
import com.kitsuneark.slider.aux.ActionButton;

import static android.content.Context.POWER_SERVICE;

public class BackgroundServiceHandler {
    private final Context context;
    private boolean alive =false;
    private PowerManager.WakeLock wakeLock;
    private TransitionDrawable background;
    private final int TRANSITION_DURATION = 200;

    BackgroundServiceHandler(Context context, Activity activity) {
        this.context = context;
        ConstraintLayout layout = activity.findViewById(R.id.cl_constraint_layout);
        ColorDrawable[] colors = new ColorDrawable[2];
        colors[0] = new ColorDrawable(context.getResources().getColor(R.color.colorNegative));
        colors[1] = new ColorDrawable(context.getResources().getColor(R.color.colorPositive));
        background = new TransitionDrawable(colors);
        layout.setBackground(background);
    }


    private void backgroundServiceOn(ActionButton toggleButton) {
        try {
            VolumeKeyListenerService.setIsActive(true);
            if(wakeLock==null) {
                PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        "SLDR::BackgroundControlWakeLock");
            }
            wakeLock.acquire();
            if(toggleButton!= null)
                toggleButton.update(context.getString(R.string.turn_bgs_off));
            alive = true;
            Toast.makeText(context, "Volume keys control: ON", Toast.LENGTH_SHORT).show();
            background.startTransition(TRANSITION_DURATION);
        } catch (Exception e) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
        }

    }

    public void backgroundServiceOff(ActionButton toggleButton) {
        if(alive) {
            try {
                VolumeKeyListenerService.setIsActive(false);
                if (wakeLock != null) wakeLock.release();
                alive = false;
                if(toggleButton!= null)
                    toggleButton.update(context.getString(R.string.turn_bgs_on));
                Toast.makeText(context, "Volume keys control: OFF", Toast.LENGTH_SHORT).show();
                background.reverseTransition(TRANSITION_DURATION);
            } catch (Exception e) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void toggleBackgroundService(ActionButton toggleButton){
        if (alive) backgroundServiceOff(toggleButton);
        else backgroundServiceOn(toggleButton);
    }

    //public boolean isAlive() {
    //    return alive;
    //}
}
