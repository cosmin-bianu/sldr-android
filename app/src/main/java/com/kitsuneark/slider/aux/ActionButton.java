package com.kitsuneark.slider.aux;

import android.view.View;
import android.widget.Button;

public class ActionButton {
    public String text;
    public View.OnClickListener action;
    public boolean isActive = false;
    public Button actionButton;

    public ActionButton(String text, View.OnClickListener action) {
        this.text = text;
        this.action = action;
    }

    public void update(String str){
        text = str;
        if (actionButton!=null && isActive)
            actionButton.setText(str);
    }
}
