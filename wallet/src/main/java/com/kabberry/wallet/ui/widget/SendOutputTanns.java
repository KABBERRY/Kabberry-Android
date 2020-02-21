package com.kabberry.wallet.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractAddress;

import com.kabberry.wallet.AddressBookProvider;
import com.kabberry.wallet.R;

public class SendOutputTanns extends LinearLayout {
    private AbstractAddress address;
    private TextView addressLabelView;
    private TextView addressLabelView2;
    private TextView addressView;
    private TextView addressView2;
    private TextView address_direction;
    private TextView address_opt;
    private TextView amount;
    private TextView amountLocal;
    private int colorBlack;
    private int colorFee;
    private int colorReceive;
    private int colorSend;
    private final Context context;
    private String feeLabel;
    private boolean isSending;
    private String label;
    private RelativeLayout ll_address;
    private LinearLayout ll_amount;
    private String receiveLabel;
    private Resources res = null;
    private String sendLabel;
    private TextView sendTypeText;
    private TextView symbol;
    private TextView symbolLocal;

    public SendOutputTanns(Context context) {
        super(context);
        this.context = context;
        inflateView(context);
    }

    public SendOutputTanns(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        inflateView(context);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SendOutput, 0, 0);

        try {
            setIsFee(a.getBoolean(0, false));
            res = context.getResources();
            colorSend = res.getColor(R.color.send_normal);
            colorReceive = res.getColor(R.color.receive_normal);
            colorFee = res.getColor(R.color.white);
            colorBlack = res.getColor(R.color.gray_87_text);
        } finally {
            a.recycle();
        }
    }

    private void inflateView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.transaction_output_new, this, true);
        sendTypeText = (TextView) findViewById(R.id.send_output_type_text);
        amount = (TextView) findViewById(R.id.amount);
        symbol = (TextView) findViewById(R.id.symbol);
        amountLocal = (TextView) findViewById(R.id.local_amount);
        symbolLocal = (TextView) findViewById(R.id.local_symbol);
        address_direction = (TextView) findViewById(R.id.address_direction);
        address_opt = (TextView) findViewById(R.id.address_opt);
        ll_amount = (LinearLayout) findViewById(R.id.ll_amount);
        addressLabelView = (TextView) findViewById(R.id.output_label);
        addressView = (TextView) findViewById(R.id.output_address);
        addressLabelView2 = (TextView) findViewById(R.id.output_label2);
        addressView2 = (TextView) findViewById(R.id.output_address2);
        ll_address = (RelativeLayout) findViewById(R.id.ll_address);

        setAllTypefaceThin(sendTypeText);
        setAllTypefaceThin(amount);
        setAllTypefaceThin(symbol);
        setAllTypefaceThin(addressLabelView);
        setAllTypefaceThin(addressView);
        setAllTypefaceThin(addressLabelView2);
        setAllTypefaceThin(addressView2);
        amountLocal.setVisibility(GONE);
        symbolLocal.setVisibility(GONE);
    }

    public void setAmount(String amount) {
        this.amount.setText(amount);
    }

    public void setSymbol(String symbol) {
        this.symbol.setText(symbol);
    }

    public void setAmountLocal(String amount) {
        amountLocal.setText(amount);
        amountLocal.setVisibility(VISIBLE);
    }

    public void setSymbolLocal(String symbol) {
        symbolLocal.setText(symbol);
        symbolLocal.setVisibility(VISIBLE);
    }

    public void setAddress(AbstractAddress address) {
        this.address = address;
        updateView();
    }

    private void updateView() {
        if (label != null) {
            addressLabelView.setText(label);
            addressLabelView.setVisibility(VISIBLE);

            if (address != null) {
                addressView.setText(GenericUtils.addressSplitToGroups(address));
                addressView.setVisibility(VISIBLE);
                return;
            }
            addressView.setVisibility(GONE);
        } else if (address != null) {
            addressLabelView.setText(GenericUtils.addressSplitToGroups(address));
            addressLabelView.setVisibility(VISIBLE);
            addressView.setVisibility(GONE);
        } else {
            ll_address.setVisibility(GONE);
        }
    }

    public void setLabel(String label) {
        this.label = label;
        updateView();
    }

    public void setIsFee(boolean isFee) {
        if (isFee) {
            setTypeLabel(getFeeLabel());
            ll_address.setVisibility(GONE);
            sendTypeText.setTextColor(colorFee);
            amount.setTextColor(colorFee);
            symbol.setTextColor(colorFee);
            ll_amount.setBackgroundResource(0);
            return;
        }
        updateDirectionLabels();
    }

    private void updateDirectionLabels() {
        if (isSending) {
            setTypeLabel(getSendLabel());
            sendTypeText.setTextColor(colorSend);
            amount.setTextColor(colorSend);
            symbol.setTextColor(colorSend);
            address_opt.setText(context.getString(R.string.tx_message_from));
            addressLabelView2.setText(context.getString(R.string.my_wallet));
            address_direction.setText(context.getString(R.string.to));
            addressView2.setVisibility(GONE);
            return;
        }
        setTypeLabel(getReceiveLabel());
        sendTypeText.setTextColor(colorReceive);
        amount.setTextColor(colorReceive);
        symbol.setTextColor(colorReceive);
        address_opt.setText(context.getString(R.string.to));
        addressLabelView2.setText(context.getString(R.string.my_wallet));
        address_direction.setText(context.getString(R.string.tx_message_from));
        addressView2.setVisibility(GONE);
    }

    private void setTypeLabel(String typeLabel) {
        if (typeLabel.isEmpty()) {
            sendTypeText.setVisibility(GONE);
            return;
        }
        sendTypeText.setVisibility(VISIBLE);
        sendTypeText.setText(typeLabel);
    }

    private String getSendLabel() {
        if (sendLabel == null) {
            return getResources().getString(R.string.send);
        }
        return sendLabel;
    }

    private String getReceiveLabel() {
        if (receiveLabel == null) {
            return getResources().getString(R.string.receive);
        }
        return receiveLabel;
    }

    private String getFeeLabel() {
        if (feeLabel == null) {
            return getResources().getString(R.string.fee);
        }
        return feeLabel;
    }

    public void setSendLabel(String sendLabel) {
        this.sendLabel = sendLabel;
        updateDirectionLabels();
    }

    public void setReceiveLabel(String receiveLabel) {
        this.receiveLabel = receiveLabel;
        updateDirectionLabels();
    }

    public void setFeeLabel(String feeLabel) {
        this.feeLabel = feeLabel;
        updateDirectionLabels();
    }

    public void setSending(boolean isSending) {
        this.isSending = isSending;
        updateDirectionLabels();
    }

    public void setLabelAndAddress(AbstractAddress address) {
        label = AddressBookProvider.resolveLabel(this.context, address);
        this.address = address;
        updateView();
    }

    public void hideLabelAndAddress() {
        label = null;
        address = null;
        updateView();
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
