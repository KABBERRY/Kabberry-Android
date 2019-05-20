package com.primestone.wallet.ui.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.messages.TxMessage;
import com.coinomi.core.util.ExchangeRate;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.AbstractTransaction.AbstractOutput;
import com.coinomi.core.wallet.AbstractWallet;
import com.google.common.collect.ImmutableList;

import com.primestone.wallet.R;
import com.primestone.wallet.Configuration;
import com.primestone.wallet.WalletApplication;

import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

public class TransactionAmountVisualizer extends LinearLayout {
    private AbstractAddress address;
    private WalletApplication application;
    private Configuration config;
    private Context context;
    private final SendOutputTanns fee;
    private Value feeAmount;
    private boolean isSending;
    private HashMap<String, ExchangeRate> localRates = new HashMap();
    private final SendOutputTanns output;
    private Value outputAmount;
    private final TextView txMessage;
    private final TextView txMessageLabel;
    private CoinType type;

    public TransactionAmountVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.transaction_amount_visualizer, this, true);
        this.output = (SendOutputTanns) findViewById(R.id.transaction_output);
        this.output.setVisibility(GONE);
        this.fee = (SendOutputTanns) findViewById(R.id.transaction_fee);
        this.fee.setVisibility(GONE);
        this.txMessageLabel = (TextView) findViewById(R.id.tx_message_label);
        this.txMessage = (TextView) findViewById(R.id.tx_message);
        if (isInEditMode()) {
            this.output.setVisibility(0);
            this.fee.setVisibility(0);
        }
        this.application = (WalletApplication) context.getApplicationContext();
        this.config = this.application.getConfiguration();
    }

    public void setTransaction(@Nullable AbstractWallet pocket, AbstractTransaction tx) {
        type = tx.getType();
        String symbol = type.getSymbol();

        final Value value = pocket != null ? tx.getValue(pocket) : type.value(0);
        isSending = pocket != null ? value.signum() < 0 : true;
        // if sending and all the outputs point inside the current pocket. If received
        boolean isInternalTransfer = this.isSending;
        this.output.setVisibility(VISIBLE);
        List<AbstractOutput> outputs = tx.getSentTo();

        for (AbstractOutput txo : outputs) {
            if (isSending) {
                if (pocket == null || !pocket.isAddressMine(txo.getAddress())) {
                    isInternalTransfer = false;
                }
            } else if (pocket == null || pocket.isAddressMine(txo.getAddress())) {
            }

            outputAmount = txo.getValue();
            output.setAmount(GenericUtils.formatCoinValue(type, outputAmount));
            output.setSymbol(symbol);
            address = txo.getAddress();
            output.setLabelAndAddress(this.address);
        }

        if (isInternalTransfer) {
            output.setLabel(getResources().getString(R.string.internal_transfer));
        }

        output.setSending(this.isSending);

        feeAmount = tx.getFee();
        if (!(!this.isSending || this.feeAmount == null || this.feeAmount.isZero())) {
            System.out.println("TAG in if Fee condition");
            fee.setVisibility(View.VISIBLE);
            fee.setAmount(GenericUtils.formatCoinValue(type, feeAmount));
            fee.setSymbol(symbol);
            fee.setIsFee(true);
        }
        if (this.type.canHandleMessages()) {
            setMessage(this.type.getMessagesFactory().extractPublicMessage(tx));
        }
    }

    private void setMessage(@Nullable TxMessage message) {
        if (message != null) {
            switch (message.getType()) {
                case PRIVATE:
                    txMessageLabel.setText(R.string.tx_message_private);
                    break;
                case PUBLIC:
                    txMessageLabel.setText(R.string.tx_message_public);
                    break;
            }
            this.txMessageLabel.setVisibility(VISIBLE);
            this.txMessage.setText(message.toString());
            this.txMessage.setVisibility(VISIBLE);
        }
    }

    public void setExchangeRate(ExchangeRate rate) {
        if (outputAmount != null) {
            Value fiatAmount = rate.convert(type, outputAmount.toCoin());
            output.setAmountLocal(GenericUtils.formatFiatValue(fiatAmount));
            this.output.setSymbolLocal(fiatAmount.type.getSymbol());
        }
        if (this.isSending && this.feeAmount != null) {
            Value fiatAmount = rate.convert(type, feeAmount.toCoin());
            fee.setAmountLocal(GenericUtils.formatFiatValue(fiatAmount));
            fee.setSymbolLocal(fiatAmount.type.getSymbol());
        }
    }

    /*
     * Hide the output address and label. Useful when we are exchanging, where the send address is
     * not important to the user.
     */
    public void hideAddresses() {
        output.hideLabelAndAddress();
    }

    public List<SendOutputTanns> getOutputs() {
        return ImmutableList.of(output);
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
