package com.primestone.wallet.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.families.NxtFamily;
import com.coinomi.core.util.ExchangeRate;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.AbstractTransaction.AbstractOutput;
import com.coinomi.core.wallet.AbstractWallet;

import com.primestone.wallet.R;
import com.primestone.wallet.Configuration;
import com.primestone.wallet.ExchangeRatesProvider;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.ui.widget.SendOutputTanns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TransactionAmountVisualizerAdapter extends BaseAdapter {
    private WalletApplication application;
    private Configuration config;
    private final Context context;
    private Value feeAmount;
    private boolean hasFee;
    private final LayoutInflater inflater;
    private boolean isSending;
    private int itemCount;
    private HashMap<String, ExchangeRate> localRates = new HashMap();
    private List<AbstractOutput> outputs;
    private final AbstractWallet pocket;
    private String symbol;
    private CoinType type;

    public TransactionAmountVisualizerAdapter(Context context, AbstractWallet walletPocket) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.pocket = walletPocket;
        this.type = this.pocket.getCoinType();
        this.symbol = this.type.getSymbol();
        this.outputs = new ArrayList();
        this.application = (WalletApplication) context.getApplicationContext();
        this.config = this.application.getConfiguration();
        for (ExchangeRatesProvider.ExchangeRate rate : ExchangeRatesProvider.getRates(context, this.config.getExchangeCurrencyCode()).values()) {
            this.localRates.put(rate.currencyCodeId, rate.rate);
        }
    }

    public void setTransaction(AbstractTransaction tx) {
        boolean z;
        int i = 1;
        this.outputs.clear();
        if (tx.getValue(this.pocket).signum() < 0) {
            z = true;
        } else {
            z = false;
        }
        this.isSending = z;
        boolean isInternalTransfer = this.isSending;
        for (AbstractOutput output : tx.getSentTo()) {
            if (this.isSending) {
                if (!this.pocket.isAddressMine(output.getAddress())) {
                    isInternalTransfer = false;
                }
            } else if (this.pocket.getCoinType() instanceof NxtFamily) {
                this.outputs.add(new AbstractOutput((AbstractAddress) tx.getReceivedFrom().get(0), tx.getValue(this.pocket)));
                break;
            } else if (!this.pocket.isAddressMine(output.getAddress())) {
            }
            this.outputs.add(output);
        }
        this.feeAmount = tx.getFee();
        if (this.feeAmount == null || this.feeAmount.isZero()) {
            z = false;
        } else {
            z = true;
        }
        this.hasFee = z;
        this.itemCount = isInternalTransfer ? 1 : this.outputs.size();
        int i2 = this.itemCount;
        if (!this.hasFee) {
            i = 0;
        }
        this.itemCount = i2 + i;
        notifyDataSetChanged();
    }

    public int getCount() {
        return this.itemCount;
    }

    public AbstractOutput getItem(int position) {
        if (position < this.outputs.size()) {
            return (AbstractOutput) this.outputs.get(position);
        }
        return null;
    }

    public long getItemId(int position) {
        return (long) position;
    }

    public View getView(int position, View row, ViewGroup parent) {
        if (row == null) {
            row = this.inflater.inflate(R.layout.transaction_details_output_row, null);
            setAllTypefaceThin(row);
            ((SendOutputTanns) row).setSendLabel(this.context.getString(R.string.sent));
            ((SendOutputTanns) row).setReceiveLabel(this.context.getString(R.string.received));
        }
        SendOutputTanns output = (SendOutputTanns) row;
        AbstractOutput txo = getItem(position);
        Value fiatAmount;
        if (txo != null) {
            Value outputAmount = txo.getValue();
            output.setAmount(GenericUtils.formatCoinValue(this.type, outputAmount));
            output.setSymbol(this.symbol);
            output.setLabelAndAddress(txo.getAddress());
            output.setSending(this.isSending);
            try {
                fiatAmount = ((ExchangeRate) this.localRates.get(outputAmount.type.getSymbol())).convert(outputAmount);
                output.setAmountLocal(GenericUtils.formatFiatValue(fiatAmount));
                output.setSymbolLocal(fiatAmount.type.getSymbol());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (position == 0) {
            output.setLabel(this.context.getString(R.string.internal_transfer));
            output.setSending(this.isSending);
            output.setAmount(null);
            output.setSymbol(null);
        } else if (this.hasFee) {
            output.setAmount(GenericUtils.formatCoinValue(this.type, this.feeAmount));
            output.setSymbol(this.symbol);
            output.setIsFee(true);
            try {
                fiatAmount = ((ExchangeRate) this.localRates.get(this.feeAmount.type.getSymbol())).convert(this.feeAmount);
                output.setAmountLocal(GenericUtils.formatFiatValue(fiatAmount));
                output.setSymbolLocal(fiatAmount.type.getSymbol());
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } else {
            output.setLabel("???");
            output.setAmount(null);
            output.setSymbol(null);
        }
        setAllTypefaceThin(row);
        return row;
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
