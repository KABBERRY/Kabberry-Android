package com.primestone.wallet.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractAddress;

import com.primestone.wallet.AddressBookProvider;
import com.primestone.wallet.R;
import com.primestone.wallet.util.WalletUtils;

public class AddressView extends LinearLayout {
    private AbstractAddress address;
    private TextView addressLabelView;
    private TextView addressView;
    private Context context;
    private ImageView iconView;
    private boolean isIconShown;
    private boolean isMultiLine;

    public AddressView(Context context) {
        super(context);
        inflateLayout(context);
    }

    public AddressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Address, 0, 0);
        try {
            this.isMultiLine = a.getBoolean(R.styleable.Address_multi_line, false);
            this.isIconShown = a.getBoolean(R.styleable.Address_show_coin_icon, false);
            inflateLayout(context);
        } finally {
            a.recycle();
        }
    }

    private void inflateLayout(Context context) {
        LayoutInflater.from(context).inflate(R.layout.address, this, true);
        this.iconView = (ImageView) findViewById(R.id.icon);
        this.addressLabelView = (TextView) findViewById(R.id.address_label);
        this.addressView = (TextView) findViewById(R.id.address);
    }

    public void setAddressAndLabel(AbstractAddress address) {
        this.address = address;
        updateView();
    }

    public void setMultiLine(boolean isMultiLine) {
        this.isMultiLine = isMultiLine;
        updateView();
    }

    public void setIconShown(boolean isIconShown) {
        this.isIconShown = isIconShown;
        updateView();
    }

    private void updateView() {
        String label = AddressBookProvider.resolveLabel(getContext(), this.address);
        Log.e("---TAG address--", this.address.toString());
        if (label != null) {
            this.addressLabelView.setText(label);
            this.addressView.setText(GenericUtils.addressSplitToGroups(this.address));
            this.addressView.setVisibility(0);
            setAllTypefaceThin(this.addressView);
        } else {
            if (this.isMultiLine) {
                this.addressLabelView.setText(GenericUtils.addressSplitToGroupsMultiline(this.address));
            } else {
                this.addressLabelView.setText(GenericUtils.addressSplitToGroups(this.address));
            }
            setAllTypefaceThin(this.addressLabelView);
            this.addressView.setVisibility(8);
        }
        if (this.isIconShown) {
            this.iconView.setVisibility(8);
            this.iconView.setContentDescription(this.address.getType().getName());
            this.iconView.setImageResource(WalletUtils.getIconRes(this.address.getType()));
            return;
        }
        this.iconView.setVisibility(8);
    }

    protected void setAllTypefaceThin(View view) {
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getChildCount() != 0) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllTypefaceThin(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.createFromAsset(this.context.getAssets(), "fonts/Roboto-Regular.ttf"));
        }
    }
}
