package com.kitsuneark.slider.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.kitsuneark.slider.aux.ActionButton;
import com.kitsuneark.slider.handlers.BackgroundServiceHandler;
import com.kitsuneark.slider.handlers.MessageHandler;
import com.kitsuneark.slider.handlers.NotificationReceiver;
import com.kitsuneark.slider.handlers.VolumeKeyListenerService;
import com.kitsuneark.slider.R;
import java.util.HashMap;
import java.util.Objects;


public class RemoteControlActivity extends AppCompatActivity{

    public enum ButtonType { DARK_MODE, BACKGROUND_SERVICE, LOCK}
    private ButtonType[] buttonTypes = { ButtonType.DARK_MODE, ButtonType.BACKGROUND_SERVICE, ButtonType.LOCK};
    public static HashMap<ButtonType, ActionButton> buttons = new HashMap<>();

    private static final String TAG = "RemoteControlActivity";
    public static final String ACTION_SHUTDOWN_SERVICE = "com.kitsuneark.slider.ACTION_SHUTDOWN_SERVICE";
    private static final int TRANSITION_DURATION = 400;

    private NotificationReceiver mReceiver;

    private ActionButton currentActionButton;
    BackgroundServiceHandler backgroundServiceHandler;

    private static Intent serviceIntent;

    private Button buttonAction;
    private Drawable arrowUp;
    private Drawable arrowDown;

    private boolean darkModeOn = false;
    private int actionIndex = 0;
    private boolean locked;
    private boolean externallyLocked;

    private ColorDrawable[] colorsUp = {new ColorDrawable(Color.WHITE), new ColorDrawable(Color.BLACK)};
    private ColorDrawable[] colorsDown = {new ColorDrawable(Color.WHITE), new ColorDrawable(Color.BLACK)};
    private TransitionDrawable transitionDrawableUp;
    private TransitionDrawable transitionDrawableDown;


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Button buttonUp = findViewById(R.id.ib_button_up);
        Button buttonDown = findViewById(R.id.ib_button_down);
        Button buttonLeft = findViewById(R.id.btn_go_left);
        Button buttonRight = findViewById(R.id.btn_go_right);
        buttonAction = findViewById(R.id.btn_action);

        buttonUp.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(!locked) {
                    MessageHandler.getInstance().onButtonUp();
                    vibrate(100);
                } else if(externallyLocked){
                    Toast.makeText(RemoteControlActivity.this, "The controls are externally locked", Toast.LENGTH_SHORT).show();
                } else{
                    Toast.makeText(RemoteControlActivity.this, "The controls are locked.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        arrowUp = getResources().getDrawable
                (R.drawable.ic_keyboard_arrow_up_black_24dp);

        buttonUp.setCompoundDrawablesWithIntrinsicBounds
                (null, arrowUp, null, null);

        Log.d(TAG, "onCreate: buttonUp " +
                buttonUp.getCompoundDrawables()[1].getAlpha());

        buttonDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!locked) {
                    MessageHandler.getInstance().onButtonDown();
                    vibrate(100);
                } else if(externallyLocked){
                    Toast.makeText(RemoteControlActivity.this, "The controls are externally locked", Toast.LENGTH_SHORT).show();
                } else{
                    Toast.makeText(RemoteControlActivity.this, "The controls are locked.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        arrowDown = getResources().getDrawable
                (R.drawable.ic_keyboard_arrow_down_black_24dp);

        buttonDown.setCompoundDrawablesWithIntrinsicBounds
                (null, null, null, arrowDown);

        buttonLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToLeftAction();
            }
        });
        buttonRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToRightAction();
            }
        });


        addButton(ButtonType.DARK_MODE, getString(R.string.light_mode), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDarkMode(buttons.get(ButtonType.DARK_MODE));
                vibrate(50);
            }
        });

        addButton(ButtonType.BACKGROUND_SERVICE, getString(R.string.turn_bgs_on), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                backgroundServiceHandler
                        .toggleBackgroundService
                                (buttons.get(ButtonType.BACKGROUND_SERVICE));
                vibrate(80);
            }
        });

        addButton(ButtonType.LOCK, getString(R.string.locked), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!externallyLocked) {
                    setLock(buttons.get(ButtonType.LOCK), !locked, false);
                    vibrate(50);
                } else Toast.makeText(RemoteControlActivity.this, "The controls are externally locked", Toast.LENGTH_SHORT).show();
            }
        });

        transitionDrawableUp = new TransitionDrawable(colorsUp);
        buttonUp.setBackground(transitionDrawableUp);
        transitionDrawableDown = new TransitionDrawable(colorsDown);
        buttonDown.setBackground(transitionDrawableDown);


        setCurrentAction(0);
        setLock(buttons.get(ButtonType.LOCK), false, false);
        setDarkMode(true, buttons.get(ButtonType.DARK_MODE));




        mReceiver = new NotificationReceiver(this, this);
        registerReceiver(mReceiver, new IntentFilter(ACTION_SHUTDOWN_SERVICE));

        serviceIntent = new Intent
                (this, VolumeKeyListenerService.class);
        startService(serviceIntent);

        NotificationReceiver receiver = new NotificationReceiver(this,this);
        backgroundServiceHandler = receiver.getBackgroundServiceHandler() ;

        MessageHandler.getInstance().setRemoteControlActivity(this);
    }

    private void toggleDarkMode(ActionButton btn) {
        setDarkMode(!darkModeOn, btn);
    }

    private void setDarkMode(boolean isOn, ActionButton btn){
        if(isOn){
            transitionDrawableUp.startTransition(TRANSITION_DURATION);
            transitionDrawableDown.startTransition(TRANSITION_DURATION);
            if(btn!=null)
                btn.update(getString(R.string.dark_mode));
        }else{
            transitionDrawableUp.reverseTransition(TRANSITION_DURATION);
            transitionDrawableDown.reverseTransition(TRANSITION_DURATION);
            if(btn!=null)
                btn.update(getString(R.string.light_mode));
        }

        darkModeOn = isOn;
    }

    private void vibrate(long ms) {
        if (Build.VERSION.SDK_INT >= 26) {
            ((Vibrator) Objects.requireNonNull(getSystemService(VIBRATOR_SERVICE))).vibrate(VibrationEffect.createOneShot(ms, 1));
        } else {
            ((Vibrator) Objects.requireNonNull(getSystemService(VIBRATOR_SERVICE))).vibrate(ms);
        }
    }

    @Override
    protected void onDestroy() {
        backgroundServiceHandler.backgroundServiceOff(null);
        unregisterReceiver(mReceiver);
        stopService(serviceIntent);
        //Toast.makeText(this, "Slider service shutting down.", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    private void goToLeftAction(){
        int newIndex = actionIndex-1;
        if(newIndex<0) newIndex = buttons.size()-1;
        setCurrentAction(newIndex);
    }

    private void goToRightAction(){
        int newIndex = actionIndex+1;
        if(newIndex>=buttons.size()) newIndex = 0;
        setCurrentAction(newIndex);
    }

    private void setCurrentAction(int index){
        if(currentActionButton != null)
            currentActionButton.isActive = false;
        currentActionButton = buttons.get(buttonTypes[index]);
        currentActionButton.isActive = true;
        if(currentActionButton.actionButton == null)
            currentActionButton.actionButton = buttonAction;
        buttonAction.setText(currentActionButton.text);
        buttonAction.setOnClickListener(currentActionButton.action);
        actionIndex = index;
        vibrate(30);
    }

    private void addButton(ButtonType type, String text, View.OnClickListener action){
        ActionButton btn = new ActionButton(text, action);
        buttons.put(type, btn);
    }

    public void setLock(boolean lock, boolean externalAction){
        Log.d(TAG, "setLock: called this");
        setLock(buttons.get(ButtonType.LOCK), lock, externalAction);
    }

    public void setLock(@NonNull ActionButton btn, boolean lock, boolean externalAction){
        locked = lock;
        externallyLocked = lock && externalAction;
        Log.d(TAG, "setLock: external action=" + externalAction);
        int startAlpha = arrowUp.getAlpha();
        int animationTime = 300;
        int endAlpha;
        if(lock){
            btn.update(getString(R.string.locked));
            endAlpha = 0;
            if(!externalAction)MessageHandler.getInstance().onLocked();
        }else{
            btn.update(getString(R.string.unlocked));
            endAlpha=255;
            if(!externalAction)MessageHandler.getInstance().onUnlocked();
        }

        animate(arrowUp, startAlpha, endAlpha, animationTime);
        animate(arrowDown, startAlpha, endAlpha, animationTime);
    }


    private void animate(Drawable target, int start, int end, long duration){
        ObjectAnimator animator = ObjectAnimator.ofInt(target, "alpha", start, end);
        animator.setDuration(duration);
        animator.start();
    }
}