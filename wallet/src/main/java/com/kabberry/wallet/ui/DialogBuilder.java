package com.kabberry.wallet.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.kabberry.wallet.R;

public class DialogBuilder extends AlertDialog.Builder {
    private final View customTitle;
    private final ImageView iconView;
    private final TextView titleView;

    public static DialogBuilder warn(final Context context, final int titleResId) {
        final DialogBuilder builder = new DialogBuilder(context);
        builder.setTitle(titleResId);
        return builder;
    }

    public DialogBuilder(final Context context) {
        super(context);

        this.customTitle = LayoutInflater.from(context).inflate(R.layout.dialog_title, null);
        this.iconView = (ImageView) customTitle.findViewById(android.R.id.icon);
        this.titleView = (TextView) customTitle.findViewById(android.R.id.title);
        this.titleView.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Regular.ttf"));
    }

    @Override
    public DialogBuilder setIcon(Drawable icon) {
        if (icon != null) {
            setCustomTitle(customTitle);
            iconView.setImageDrawable(icon);
            iconView.setVisibility(View.VISIBLE);
        }

        return this;
    }

    @Override
    public DialogBuilder setIcon(final int iconResId) {
        if (iconResId != 0) {
            setCustomTitle(customTitle);
            iconView.setImageResource(iconResId);
            iconView.setVisibility(View.VISIBLE);
        }

        return this;
    }

    @Override
    public DialogBuilder setTitle(final CharSequence title) {
        if (title != null) {
            setCustomTitle(customTitle);
            titleView.setText(title);
        }

        return this;
    }

    @Override
    public DialogBuilder setTitle(final int titleResId) {
        if (titleResId != 0) {
            setCustomTitle(customTitle);
            titleView.setText(titleResId);
        }

        return this;
    }

    @Override
    public DialogBuilder setMessage(final CharSequence message) {
        super.setMessage(message);

        return this;
    }

    @Override
    public DialogBuilder setMessage(final int messageResId) {
        super.setMessage(messageResId);

        return this;
    }
}
