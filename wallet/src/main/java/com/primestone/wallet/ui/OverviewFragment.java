package com.primestone.wallet.ui;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.OnItemLongClick;
import com.coinomi.core.coins.Value;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import com.primestone.wallet.R;
import com.primestone.wallet.Configuration;
import com.primestone.wallet.ExchangeRatesProvider;
import com.primestone.wallet.ExchangeRatesProvider.ExchangeRate;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.ui.adaptors.AccountListAdapter;
import com.primestone.wallet.ui.widget.Amount;
import com.primestone.wallet.ui.widget.SwipeRefreshLayout;
import com.primestone.wallet.ui.widget.SwipeRefreshLayout.OnRefreshListener;
import com.primestone.wallet.util.ThrottlingWalletChangeListener;
import com.primestone.wallet.util.UiUtils;
import com.primestone.wallet.util.WeakHandler;

import java.util.Map;
import org.bitcoinj.utils.Threading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverviewFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(OverviewFragment.class);

    private static final int WALLET_CHANGED = 0;
    private static final int UPDATE_VIEW = 1;
    private static final int SET_EXCHANGE_RATES = 2;

    private static final int ID_RATE_LOADER = 0;

    private final Handler handler = new MyHandler(this);

    private static class MyHandler extends WeakHandler<OverviewFragment> {
        public MyHandler(OverviewFragment ref) {
            super(ref);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void weakHandleMessage(OverviewFragment ref, Message msg) {
            switch (msg.what) {
                case WALLET_CHANGED:
                    ref.updateWallet();
                    break;
                case UPDATE_VIEW:
                    ref.updateView();
                    break;
                case SET_EXCHANGE_RATES:
                    ref.setExchangeRates((Map<String, ExchangeRate>) msg.obj);
                    break;
            }
        }
    }

    private Wallet wallet;
    private Value currentBalance;
    private boolean isFullAmount = false;
    private WalletApplication application;
    private Configuration config;

    private AccountListAdapter adapter;
    Map<String, ExchangeRate> exchangeRates;

    @Bind(R.id.swipeContainer) SwipeRefreshLayout swipeContainer;
    @Bind(R.id.account_rows) ListView accountRows;
    @Bind(R.id.account_balance) Amount mainAmount;

    private Listener listener;

    private final LoaderCallbacks<Cursor> rateLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
            String localSymbol = config.getExchangeCurrencyCode();
            return new ExchangeRateLoader(getActivity(), config, localSymbol);
        }

        @Override
        public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
            if (data != null && data.getCount() > 0) {
                ImmutableMap.Builder<String, ExchangeRate> builder = ImmutableMap.builder();
                data.moveToFirst();
                do {
                    ExchangeRate rate = ExchangeRatesProvider.getExchangeRate(data);
                    builder.put(rate.currencyCodeId, rate);
                } while (data.moveToNext());

                handler.sendMessage(handler.obtainMessage(SET_EXCHANGE_RATES, builder.build()));
            }
        }

        @Override
        public void onLoaderReset(final Loader<Cursor> loader) { }
    };


    private final ThrottlingWalletChangeListener walletChangeListener = new ThrottlingWalletChangeListener() {

        @Override
        public void onThrottledWalletChanged() {
            handler.sendMessage(handler.obtainMessage(WALLET_CHANGED));
        }
    };

    public static OverviewFragment getInstance() {
        return new OverviewFragment();
    }

    public OverviewFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        wallet = application.getWallet();
        if (wallet != null) {
            exchangeRates = ExchangeRatesProvider.getRates(application.getApplicationContext(),
                    config.getExchangeCurrencyCode());
            if (this.adapter != null) {
                this.adapter.setExchangeRates(this.exchangeRates);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_overview, container, false);
        View header = inflater.inflate(R.layout.fragment_overview_header, null);
        accountRows = (ListView) ButterKnife.findById(view, (int) R.id.account_rows);
        accountRows.addHeaderView(header, null, false);
        ButterKnife.bind((Object) this, view);

        if (wallet == null) return view;

        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (listener != null) {
                    listener.onRefresh();
                }
            }
        });

        swipeContainer.setColorSchemeResources(
                R.color.progress_bar_color_1,
                R.color.progress_bar_color_2,
                R.color.progress_bar_color_3,
                R.color.progress_bar_color_4);

        View listFooter = new View(getActivity());
        listFooter.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin));
        accountRows.addFooterView(listFooter);

        adapter = new AccountListAdapter(inflater.getContext(), wallet);
        accountRows.setAdapter(adapter);
        adapter.setExchangeRates(exchangeRates);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.listener = (Listener) context;
            this.application = (WalletApplication) context.getApplicationContext();
            this.config = this.application.getConfiguration();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + Listener.class);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this.rateLoaderCallbacks);
    }

    @Override
    public void onDetach() {
        getLoaderManager().destroyLoader(0);
        this.listener = null;
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        for (WalletAccount account : this.wallet.getAllAccounts()) {
            account.addEventListener(this.walletChangeListener, Threading.SAME_THREAD);
        }
        updateWallet();
        updateView();
    }

    @Override
    public void onPause() {
        for (WalletAccount account : this.wallet.getAllAccounts()) {
            account.removeEventListener(this.walletChangeListener);
        }
        this.walletChangeListener.removeCallbacks();
        super.onPause();
    }

    @OnClick(R.id.account_balance)
    public void onMainAmountClick(View v) {
    }

    @OnItemClick(R.id.account_rows)
    public void onAmountClick(int position) {
        if (position >= this.accountRows.getHeaderViewsCount()) {
            Object obj = this.accountRows.getItemAtPosition(position);
            if (this.listener == null || obj == null || !(obj instanceof WalletAccount)) {
                showGenericError();
            } else {
                this.listener.onAccountSelected(((WalletAccount) obj).getId());
            }
        }
    }

    @OnItemLongClick(R.id.account_rows)
    public boolean onAmountLongClick(int position) {
        if (position >= this.accountRows.getHeaderViewsCount()) {
            Object obj = this.accountRows.getItemAtPosition(position);
            Activity activity = getActivity();
            if (obj == null || !(obj instanceof WalletAccount) || activity == null) {
                showGenericError();
            } else {
                ActionMode actionMode = UiUtils.startAccountActionMode((WalletAccount) obj, activity, getFragmentManager());
                if (activity instanceof WalletActivity) {
                    ((WalletActivity) activity).registerActionMode(actionMode);
                }
                return true;
            }
        }
        return false;
    }

    private void showGenericError() {
        Toast.makeText(getActivity(), getString(R.string.error_generic),
                Toast.LENGTH_LONG).show();
    }

    public void updateWallet() {
        if (wallet != null) {
            adapter.replace(wallet);
            calculateNewBalance();
            updateView();
        }
    }

    private void calculateNewBalance() {
        currentBalance = null;
        for (WalletAccount w : wallet.getAllAccounts()) {
            ExchangeRate rate = exchangeRates.get(w.getCoinType().getSymbol());
            if (rate == null) {
                log.info("Missing exchange rate for {}, skipping...", w.getCoinType().getName());
            } else if (currentBalance != null) {
                currentBalance = currentBalance.add(rate.rate.convert(w.getBalance()));
            } else {
                currentBalance = rate.rate.convert(w.getBalance());
            }
        }
    }

    public void setExchangeRates(Map<String, ExchangeRate> newExchangeRates) {
        this.exchangeRates = newExchangeRates;
        this.adapter.setExchangeRates(newExchangeRates);
        calculateNewBalance();
        updateView();
    }

    public void updateView() {
        if (currentBalance != null) {
            mainAmount.setAmount(GenericUtils.formatFiatValue(currentBalance));
            mainAmount.setSymbol(currentBalance.type.getSymbol());
        } else {
            mainAmount.setAmount("-.--");
            mainAmount.setSymbol("");
        }

        swipeContainer.setRefreshing(wallet.isLoading());
    }

    public interface Listener extends EditAccountFragment.Listener {
        //void onLocalAmountClick();
        void onAccountSelected(String str);
        void onRefresh();
    }
}
