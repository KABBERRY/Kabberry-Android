package com.primestone.wallet.ui.adaptors;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.coinomi.core.coins.Value;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount;

import com.primestone.wallet.R;
import com.primestone.wallet.Constants;
import com.primestone.wallet.ExchangeRatesProvider.ExchangeRate;
import com.primestone.wallet.ui.widget.Amount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

public class AccountListAdapter extends BaseAdapter {
    private final List<WalletAccount> accounts = new ArrayList<>();
    private Context context;
    private final LayoutInflater inflater;
    private final HashMap<String, ExchangeRate> rates;

    public AccountListAdapter(Context context, Wallet wallet) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        accounts.addAll(wallet.getAllAccounts());
        this.rates = new HashMap<>();
    }

    public void replace(@Nonnull final Wallet wallet) {
        this.accounts.clear();
        this.accounts.addAll(wallet.getAllAccounts());
        notifyDataSetChanged();
    }

    @Override
    public boolean isEmpty() {
        return accounts.isEmpty();
    }

    @Override
    public int getCount() {
        return accounts.size();
    }

    @Override
    public WalletAccount getItem(int position) {
        return (WalletAccount) accounts.get(position);
    }

    @Override
    public long getItemId(int position) {
        if (position == accounts.size()) {
            return 0;
        }
        return (long) ((WalletAccount) accounts.get(position)).getId().hashCode();
    }

    public void setExchangeRates(@Nullable Map<String, ExchangeRate> newRates) {
        if (newRates != null) {
            rates.putAll(newRates);
        } else {
            rates.clear();
        }

        notifyDataSetChanged();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View row, ViewGroup parent) {
        if (row == null) {
            row = inflater.inflate(R.layout.account_row, null);
        }

        bindView(row, getItem(position));
        return row;
    }

    private void bindView(View row, WalletAccount account) {
        ((ImageView) row.findViewById(R.id.account_icon)).setImageResource(((Integer) Constants.COINS_ICONS.get(account.getCoinType())).intValue());
        setAllTypefaceThin(row);
        ((TextView) row.findViewById(R.id.account_description)).setText(account.getDescriptionOrCoinName());

        final Amount rowValue = (Amount) row.findViewById(R.id.account_balance);
        rowValue.setAmount(GenericUtils.formatFiatValue(account.getBalance(), 4, 0));
        rowValue.setSymbol(account.getCoinType().getSymbol());

        ExchangeRate rate = (ExchangeRate) this.rates.get(account.getCoinType().getSymbol());
        Amount rowBalanceRateValue = (Amount) row.findViewById(R.id.account_balance_rate);
        if (rate == null || account.getCoinType() == null) {
            rowBalanceRateValue.setVisibility(View.GONE);
        } else {
            Value localAmount = rate.rate.convert(account.getBalance());
            GenericUtils.formatCoinValue(localAmount.type, localAmount, true);
            rowBalanceRateValue.setAmount(GenericUtils.formatFiatValue(localAmount, 2, 0));
            rowBalanceRateValue.setSymbol(localAmount.type.getSymbol());
            rowBalanceRateValue.setVisibility(View.VISIBLE);
        }

        final Amount rowRateValue = (Amount) row.findViewById(R.id.exchange_rate_row_rate);
        if (rate == null || account.getCoinType() == null) {
            rowRateValue.setVisibility(View.GONE);
            return;
        }
        Value localAmount = rate.rate.convert(account.getCoinType().oneCoin());
        GenericUtils.formatCoinValue(localAmount.type, localAmount, true);
        rowRateValue.setAmount(GenericUtils.formatFiatValue(localAmount, 2, 0));
        rowRateValue.setSymbol(localAmount.type.getSymbol());
        rowRateValue.setVisibility(View.VISIBLE);
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
