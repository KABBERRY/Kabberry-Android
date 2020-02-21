package com.kabberry.wallet.ui.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kabberry.wallet.R;

public class NavDrawerItemView extends LinearLayout implements Checkable {
    private final ImageView icon;
    private boolean isChecked = false;
    private final TextView title;
    private final View view;

    public NavDrawerItemView(Context context) {
        super(context);
        this.view = LayoutInflater.from(context).inflate(R.layout.account_row, this, true);
        this.title = (TextView) findViewById(R.id.item_text);
        this.icon = (ImageView) findViewById(R.id.item_icon);
    }

    public void setData(String titleStr, int iconRes) {
        this.title.setText(titleStr);
        this.icon.setImageResource(iconRes);
    }

    public void setChecked(boolean checked) {
        this.isChecked = checked;
        if (this.isChecked) {
            this.view.setBackgroundResource(R.color.primary_500);
        } else {
            this.view.setBackgroundResource(0);
        }
    }

    public boolean isChecked() {
        return this.isChecked;
    }

    public void toggle() {
        setChecked(!this.isChecked);
    }
}
