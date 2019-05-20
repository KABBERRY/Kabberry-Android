package com.primestone.wallet.ui;

import android.os.Bundle;

import com.coinomi.core.coins.Value;
import com.coinomi.core.wallet.WalletAccount;

import com.primestone.wallet.R;
import com.primestone.wallet.ExchangeHistoryProvider.ExchangeEntry;
import com.primestone.wallet.ui.MakeTransactionFragment.Listener;

import org.bitcoinj.crypto.KeyCrypterException;

public class TradeActivity extends BaseWalletActivity implements Listener, TradeSelectFragment.Listener, TradeStatusFragment.Listener {
    private int containerRes;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_fragment_wrapper);
        this.containerRes = R.id.container;
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(this.containerRes, new TradeSelectFragment(), "trade_select_fragment_tag").commit();
        }
    }

    public void onMakeTrade(WalletAccount fromAccount, WalletAccount toAccount, Value amount) {
        Bundle args = new Bundle();
        args.putString("account_id", fromAccount.getId());
        args.putString("send_to_account_id", toAccount.getId());
        if (amount.type.equals(fromAccount.getCoinType())) {
            if (amount.compareTo(fromAccount.getBalance()) == 0) {
                args.putSerializable("empty_wallet", Boolean.valueOf(true));
            } else {
                args.putSerializable("send_value", amount);
            }
        } else if (amount.type.equals(toAccount.getCoinType())) {
            args.putSerializable("send_value", amount);
        } else {
            throw new IllegalStateException("Amount does not have the expected type: " + amount.type);
        }
        replaceFragment(MakeTransactionFragment.newInstance(args), this.containerRes);
    }

    public void onSignResult(Exception error, ExchangeEntry exchangeEntry) {
        if (error != null) {
            getSupportFragmentManager().popBackStack();
            if (!(error instanceof KeyCrypterException)) {
                DialogBuilder builder = DialogBuilder.warn(this, R.string.trade_error);
                builder.setMessage(getString(R.string.trade_error_sign_tx_message, new Object[]{error.getMessage()}));
                builder.setPositiveButton((int) R.string.button_ok, null).create().show();
            }
        } else if (exchangeEntry != null) {
            getSupportFragmentManager().popBackStack();
            replaceFragment(TradeStatusFragment.newInstance(exchangeEntry, true), this.containerRes);
        }
    }

    public void onFinish() {
        finish();
    }
}
