package com.primestone.wallet.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.coinomi.core.wallet.Wallet;

import com.primestone.wallet.R;
import com.primestone.wallet.ExchangeHistoryProvider;
import com.primestone.wallet.ExchangeHistoryProvider.ExchangeEntry;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.ui.widget.AddressView;
import com.primestone.wallet.ui.widget.Amount;
import com.primestone.wallet.util.Fonts;
import com.primestone.wallet.util.Fonts.Font;

import org.bitcoinj.core.Coin;

public final class ExchangeHistoryFragment extends ListFragment {
    private Context activity;
    private ExchangeEntryAdapter adapter;
    private WalletApplication application;
    private Coin balance = null;
    private Uri contentUri;
    private String defaultCurrency = null;
    private final LoaderCallbacks<Cursor> exchangesLoaderCallbacks = new C11321();
    private LoaderManager loaderManager;
    private String query = null;
    private Wallet wallet;

    class C11321 implements LoaderCallbacks<Cursor> {
        C11321() {
        }

        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(ExchangeHistoryFragment.this.activity, ExchangeHistoryFragment.this.contentUri, null, null, null, "_id DESC");
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            ExchangeHistoryFragment.this.adapter.swapCursor(data);
            ExchangeHistoryFragment.this.setEmptyText(ExchangeHistoryFragment.this.getString(R.string.exchange_history_empty));
        }

        public void onLoaderReset(Loader<Cursor> loader) {
        }
    }

    private final class ExchangeEntryAdapter extends ResourceCursorAdapter {
        private ExchangeEntryAdapter(Context context) {
            super(context, (int) R.layout.exchange_status_row, null, true);
        }

        public void bindView(View view, Context context, Cursor cursor) {
            ExchangeEntry entry = ExchangeHistoryProvider.getExchangeEntry(cursor);
            View okIcon = view.findViewById(R.id.exchange_status_ok_icon);
            View errorIcon = view.findViewById(R.id.exchange_status_error_icon);
            Fonts.setTypeface(okIcon, Font.COINOMI_FONT_ICONS);
            Fonts.setTypeface(errorIcon, Font.COINOMI_FONT_ICONS);
            Fonts.setTypeface(view.findViewById(R.id.exchange_arrow), Font.COINOMI_FONT_ICONS);
            View progress = view.findViewById(R.id.exchange_status_progress);
            TextView statusText = (TextView) view.findViewById(R.id.exchange_status_text);
            View values = view.findViewById(R.id.exchange_values);
            Amount deposit = (Amount) view.findViewById(R.id.exchange_deposit);
            Amount withdraw = (Amount) view.findViewById(R.id.exchange_withdraw);
            AddressView addressView = (AddressView) view.findViewById(R.id.withdraw_address);
            switch (entry.status) {
                case -1:
                    okIcon.setVisibility(8);
                    errorIcon.setVisibility(0);
                    progress.setVisibility(8);
                    statusText.setVisibility(0);
                    statusText.setText(R.string.trade_status_failed);
                    values.setVisibility(8);
                    addressView.setVisibility(8);
                    return;
                case 0:
                    okIcon.setVisibility(8);
                    errorIcon.setVisibility(8);
                    progress.setVisibility(0);
                    statusText.setVisibility(0);
                    statusText.setText(R.string.trade_status_waiting_deposit);
                    values.setVisibility(8);
                    addressView.setVisibility(8);
                    return;
                case 1:
                    okIcon.setVisibility(8);
                    errorIcon.setVisibility(8);
                    progress.setVisibility(0);
                    statusText.setVisibility(0);
                    statusText.setText(R.string.trade_status_waiting_trade);
                    values.setVisibility(8);
                    addressView.setVisibility(8);
                    return;
                case 2:
                    okIcon.setVisibility(0);
                    errorIcon.setVisibility(8);
                    progress.setVisibility(8);
                    statusText.setVisibility(8);
                    values.setVisibility(0);
                    deposit.setAmount(entry.depositAmount.toPlainString());
                    deposit.setSymbol(entry.depositAmount.type.getSymbol());
                    withdraw.setAmount(entry.withdrawAmount.toPlainString());
                    withdraw.setSymbol(entry.withdrawAmount.type.getSymbol());
                    addressView.setVisibility(0);
                    addressView.setAddressAndLabel(entry.withdrawAddress);
                    return;
                default:
                    return;
            }
        }
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        this.activity = context;
        this.application = (WalletApplication) context.getApplicationContext();
        this.wallet = this.application.getWallet();
        this.loaderManager = getLoaderManager();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.contentUri = ExchangeHistoryProvider.contentUri(this.application.getPackageName());
        this.adapter = new ExchangeEntryAdapter(this.activity);
        setListAdapter(this.adapter);
        this.loaderManager.initLoader(0, null, this.exchangesLoaderCallbacks);
    }

    public void onDestroy() {
        this.loaderManager.destroyLoader(0);
        super.onDestroy();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exchange_history, container, false);
    }

    public void setEmptyText(CharSequence text) {
        if (getView() != null) {
            ((TextView) getView().findViewById(android.R.id.empty)).setText(text);
        }
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        ExchangeEntry entry = ExchangeHistoryProvider.getExchangeEntry((Cursor) this.adapter.getItem(position));
        Intent intent = new Intent(getActivity(), TradeStatusActivity.class);
        intent.putExtra("exchange_entry", entry);
        startActivity(intent);
    }
}
