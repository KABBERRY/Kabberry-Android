package com.kabberry.wallet.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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
import android.widget.Toast;

import com.coinomi.core.coins.CoinID;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.Wallet;

import com.kabberry.wallet.R;
import com.kabberry.wallet.Configuration;
import com.kabberry.wallet.ExchangeRatesProvider;
import com.kabberry.wallet.ExchangeRatesProvider.ExchangeRate;
import com.kabberry.wallet.WalletApplication;
import com.kabberry.wallet.ui.widget.Amount;
import com.kabberry.wallet.util.WalletUtils;

import org.bitcoinj.core.Coin;

public final class ExchangeRatesFragment extends ListFragment implements OnSharedPreferenceChangeListener {
    private ExchangeRatesAdapter adapter;
    private WalletApplication application;
    private Coin balance = null;
    private Configuration config;
    private Uri contentUri;
    private Context context;
    private String defaultCurrency = null;
    private LoaderManager loaderManager;
    private String query = null;
    private final LoaderCallbacks<Cursor> rateLoaderCallbacks = new C11341();
    private CoinType type;
    private Wallet wallet;

    class C11341 implements LoaderCallbacks<Cursor> {
        C11341() {
        }

        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (ExchangeRatesFragment.this.query == null) {
                return new CursorLoader(ExchangeRatesFragment.this.context, ExchangeRatesFragment.this.contentUri, null, null, null, null);
            }
            return new CursorLoader(ExchangeRatesFragment.this.context, ExchangeRatesFragment.this.contentUri, null, null, null, null);
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            Cursor oldCursor = ExchangeRatesFragment.this.adapter.swapCursor(data);
            if (!(data == null || oldCursor != null || ExchangeRatesFragment.this.defaultCurrency == null)) {
                int defaultCurrencyPosition = findCurrencyCode(data, ExchangeRatesFragment.this.defaultCurrency);
                if (defaultCurrencyPosition >= 0) {
                    ExchangeRatesFragment.this.getListView().setSelection(defaultCurrencyPosition);
                }
            }
            ExchangeRatesFragment.this.setEmptyText(ExchangeRatesFragment.this.getString(ExchangeRatesFragment.this.query != null ? R.string.exchange_rates_empty_search : R.string.exchange_rates_load_error));
        }

        public void onLoaderReset(Loader<Cursor> loader) {
        }

        private int findCurrencyCode(Cursor cursor, String currencyCode) {
            int currencyCodeColumn = cursor.getColumnIndexOrThrow("currency_id");
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                if (cursor.getString(currencyCodeColumn).equals(currencyCode)) {
                    return cursor.getPosition();
                }
            }
            return -1;
        }
    }

    private final class ExchangeRatesAdapter extends ResourceCursorAdapter {
        private Coin rateBase;

        private ExchangeRatesAdapter(Context context) {
            super(context, (int) R.layout.exchange_rate_row, null, true);
            this.rateBase = Coin.COIN;
        }

        public void setRateBase(Coin rateBase) {
            this.rateBase = rateBase;
            notifyDataSetChanged();
        }

        public void bindView(View view, Context context, Cursor cursor) {
            ExchangeRate exchangeRate = ExchangeRatesProvider.getExchangeRate(cursor);
            view.setBackgroundResource(exchangeRate.currencyCodeId.equals(ExchangeRatesFragment.this.defaultCurrency) ? R.color.bg_list_selected : R.color.bg_list);
            ((TextView) view.findViewById(R.id.exchange_rate_row_currency_code)).setText(exchangeRate.currencyCodeId);
            TextView currencyNameView = (TextView) view.findViewById(R.id.exchange_rate_row_currency_name);
            String currencyName = WalletUtils.getCurrencyName(exchangeRate.currencyCodeId);

            if (currencyName != null) {
                currencyNameView.setText(currencyName);
                currencyNameView.setVisibility(0);
            } else {
                currencyNameView.setText(null);
                currencyNameView.setVisibility(4);
            }

            Amount rateAmountUnitView = (Amount) view.findViewById(R.id.exchange_rate_row_rate_unit);
            rateAmountUnitView.setAmount(GenericUtils.formatCoinValue(ExchangeRatesFragment.this.type, this.rateBase, true));
            rateAmountUnitView.setSymbol(ExchangeRatesFragment.this.type.getSymbol());
            Amount rateAmountView = (Amount) view.findViewById(R.id.exchange_rate_row_rate);
            Value fiatAmount = exchangeRate.rate.convert(ExchangeRatesFragment.this.type, this.rateBase);
            rateAmountView.setAmount(GenericUtils.formatFiatValue(fiatAmount));
            rateAmountView.setSymbol(fiatAmount.type.getSymbol());
        }
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        this.application = (WalletApplication) context.getApplicationContext();
        this.config = this.application.getConfiguration();
        this.wallet = this.application.getWallet();
        this.loaderManager = getLoaderManager();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey("coin_id")) {
            this.type = CoinID.typeFromId(getArguments().getString("coin_id"));
        }
        this.contentUri = ExchangeRatesProvider.contentUriToLocal(this.context.getPackageName(), this.type.getSymbol(), false);
        this.defaultCurrency = this.config.getExchangeCurrencyCode();
        this.config.registerOnSharedPreferenceChangeListener(this);
        this.adapter = new ExchangeRatesAdapter(this.context);
        setListAdapter(this.adapter);
        this.loaderManager.initLoader(1, null, this.rateLoaderCallbacks);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exchange_rates, container, false);
    }

    public void setEmptyText(CharSequence text) {
        ((TextView) getView().findViewById(android.R.id.empty)).setText(text);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setFastScrollEnabled(true);
        setEmptyText(getString(R.string.exchange_rates_loading));
    }

    public void onResume() {
        super.onResume();
        updateView();
    }

    public void onPause() {
        super.onPause();
    }

    public void onDestroy() {
        this.config.unregisterOnSharedPreferenceChangeListener(this);
        this.loaderManager.destroyLoader(1);
        super.onDestroy();
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        this.defaultCurrency = ExchangeRatesProvider.getExchangeRate((Cursor) this.adapter.getItem(position)).currencyCodeId;
        this.config.setExchangeCurrencyCode(this.defaultCurrency);
        Toast.makeText(getActivity(), getString(R.string.set_local_currency, this.defaultCurrency), 0).show();
        getActivity().finish();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("exchange_currency".equals(key)) {
            this.defaultCurrency = this.config.getExchangeCurrencyCode();
            updateView();
        }
    }

    private void updateView() {
        if (this.adapter != null && this.type != null) {
            this.adapter.setRateBase(this.type.getOneCoin());
        }
    }
}
