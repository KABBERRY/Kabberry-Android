package com.primestone.wallet.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.primestone.wallet.R;

public class Amount extends LinearLayout {
    private final TextView amountView;
    boolean isBig = false;
    boolean isSingleLine = false;
    boolean isSmall = false;
    private final TextView symbolView;
    Typeface typeface;

    public Amount(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.amount_new, this, true);

        this.typeface = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Regular.ttf");
        symbolView = (TextView) findViewById(R.id.symbol);
        amountView = (TextView) findViewById(R.id.amount_text);
        symbolView.setTypeface(this.typeface);
        amountView.setTypeface(this.typeface);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Amount, 0, 0);
        try {
            isBig = a.getBoolean(R.styleable.Amount_show_big, false);
            isSmall = a.getBoolean(R.styleable.Amount_show_small, false);
            isSingleLine = a.getBoolean(R.styleable.Amount_single_line, false);

            if (!getRootView().isInEditMode()) {
                if (isBig) {
                    amountView.setTextAppearance(context, R.style.AmountBig);
                    symbolView.setTextAppearance(context, R.style.AmountBig);
                    symbolView.setTypeface(this.typeface);
                } else if (isSmall) {
                    amountView.setTextAppearance(context, R.style.AmountSmall);
                    symbolView.setTextAppearance(context, R.style.AmountSmall);
                    symbolView.setTypeface(this.typeface);
                } else {
                    amountView.setTextAppearance(context, R.style.Amount);
                    symbolView.setTextAppearance(context, R.style.Amount);
                    symbolView.setTypeface(this.typeface);
                }
            }
            setSingleLine(this.isSingleLine);
            if (getRootView().isInEditMode()) {
                amountView.setText("3.14159265");
                amountView.setTypeface(this.typeface);
            }
        } finally {
            a.recycle();
        }
    }

    public void setAmount(CharSequence amount) {
        amountView.setText(amount);
        amountView.setTypeface(this.typeface);
    }

    public void setSymbol(CharSequence symbol) {
        this.symbolView.setText(symbol);
    }

    public void setSingleLine(boolean isSingleLine) {
        this.isSingleLine = isSingleLine;
        if (isSingleLine) {
            ((LinearLayout) findViewById(R.id.amount_layout)).setOrientation(0);
        } else {
            ((LinearLayout) findViewById(R.id.amount_layout)).setOrientation(1);
        }
    }

    public void setAmountPending(String pendingAmount) {
    }
}
