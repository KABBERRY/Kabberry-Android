package com.primestone.wallet.ui.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.WalletAccount;

import com.primestone.wallet.R;
import com.primestone.wallet.ExchangeRatesProvider.ExchangeRate;
import com.primestone.wallet.util.WalletUtils;

public class CoinListItem extends LinearLayout implements Checkable {
    private boolean isChecked = false;
    private CoinType type;
    final View view;

    @Bind(R.id.item_icon) ImageView icon;
    @Bind(R.id.item_text) TextView title;
    @Bind(R.id.amount) Amount amount;


    public CoinListItem(Context context) {
        super(context);
        view = LayoutInflater.from(context).inflate(R.layout.coin_list_row, this, true);
        ButterKnife.bind((Object) this, view);
    }

    public void setAccount(WalletAccount account) {
        type = account.getCoinType();
        title.setText(account.getDescriptionOrCoinName());
        icon.setImageResource(WalletUtils.getIconRes(account));
    }

    public void setCoin(CoinType type) {
        this.type = type;
        title.setText(type.getName());
        this.icon.setImageResource(WalletUtils.getIconRes(type));
    }

    public void setExchangeRate(ExchangeRate exchangeRate) {
        if (exchangeRate == null || type == null) {
            amount.setVisibility(GONE);
        } else {
            setFiatAmount(exchangeRate.rate.convert(type.oneCoin()));
        }
    }

    public void setAmount(Value value) {
        amount.setAmount(GenericUtils.formatCoinValue(value.type, value, true));
        amount.setSymbol(value.type.getSymbol());
        amount.setVisibility(VISIBLE);
    }

    private void setFiatAmount(Value value) {
        amount.setAmount(GenericUtils.formatFiatValue(value));
        amount.setSymbol(value.type.getSymbol());
        amount.setVisibility(VISIBLE);
    }

    public void setAmountSingleLine(boolean isSingleLine) {
        amount.setSingleLine(isSingleLine);
    }

    @Override
    public void setChecked(boolean checked) {
        isChecked = checked;
        if (isChecked) {
            view.setBackgroundResource(R.color.primary_500);
        } else {
            view.setBackgroundResource(0);
        }
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void toggle() {
        setChecked(!isChecked);
    }
}
