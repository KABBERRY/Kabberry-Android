package com.primestone.wallet.ui;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.ValueType;
import com.coinomi.core.uri.CoinURI;
import com.coinomi.core.uri.CoinURIParseException;
import com.coinomi.core.wallet.WalletAccount;

import com.primestone.wallet.R;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.ui.widget.CoinListItem;
import com.primestone.wallet.util.UiUtils;

public class PayWithDialog extends DialogFragment {

    private Listener listener;

    public interface Listener {
        void payWith(WalletAccount walletAccount, CoinURI coinURI);
    }

    /*
    class C11611 implements OnClickListener {
        C11611() {
        }

        public void onClick(View v) {
            new Builder(PayWithDialog.this.getActivity()).setTitle(R.string.about_shapeshift_title).setMessage(R.string.about_shapeshift_message).setPositiveButton(R.string.button_ok, null).create().show();
        }
    }
*/
    public static DialogFragment getInstance(CoinURI uri) {
        DialogFragment dialog = new PayWithDialog();
        dialog.setArguments(new Bundle());
        dialog.getArguments().putString("test_wallet", uri.toUriString());
        return dialog;
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.listener = (Listener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.getClass() + " must implement " + Listener.class);
        }
    }

    public void onDetach() {
        this.listener = null;
        super.onDetach();
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        WalletApplication app = (WalletApplication) getActivity().getApplication();
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.select_pay_with, null);
        setAllTypefaceThin(view);

        try {
            CoinURI uri = new CoinURI(getArguments().getString("test_wallet"));
            ValueType type = uri.getTypeRequired();
            ViewGroup typeAccounts = (ViewGroup) view.findViewById(R.id.pay_with_layout);
            boolean canSend = false;

            for (WalletAccount account : app.getAccounts((CoinType) type)) {
                if (account.getBalance().isPositive()) {
                    addPayWithAccountRow(typeAccounts, account, uri);
                    canSend = true;
                }
            }

            if (!canSend) {
                UiUtils.setGone(view.findViewById(R.id.pay_with_title));
                UiUtils.setGone(typeAccounts);
            }

            ViewGroup exchangeAccounts = (ViewGroup) view.findViewById(R.id.exchange_and_pay_layout);
            boolean canExchange = false;

            for (WalletAccount account2 : app.getAllAccounts()) {
                if (!account2.isType(type) && account2.getBalance().isPositive()) {
                    addPayWithAccountRow(exchangeAccounts, account2, uri);
                    canExchange = true;
                }
            }

            if (canExchange) {
                ((TextView) view.findViewById(R.id.powered_by_shapeshift)).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        new Builder(PayWithDialog.this.getActivity()).setTitle(R.string.about_shapeshift_title).setMessage(R.string.about_shapeshift_message).setPositiveButton(R.string.button_ok, null).create().show();
                    }
                });
            } else {
                UiUtils.setGone(view.findViewById(R.id.exchange_and_pay_title));
                UiUtils.setGone(exchangeAccounts);
                UiUtils.setGone(view.findViewById(R.id.powered_by_shapeshift));
            }

            return new DialogBuilder(getActivity()).setView(view).create();
        } catch (CoinURIParseException e) {
            return new DialogBuilder(getActivity()).setMessage(getString(R.string.scan_error, e.getMessage())).create();
        }
    }

    private void addPayWithAccountRow(ViewGroup container, final WalletAccount account, final CoinURI uri) {
        CoinListItem row = new CoinListItem(getActivity());
        row.setAccount(account);
        row.setExchangeRate(null);
        row.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.payWith(account, uri);
                }
                PayWithDialog.this.dismiss();
            }
        });
        container.addView(row);
    }

    protected void setAllTypefaceThin(View view) {
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getChildCount() != 0) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllTypefaceThin(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Regular.ttf"));
        }
    }
}
