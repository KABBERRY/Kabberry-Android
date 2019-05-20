package com.primestone.wallet.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnItemClick;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.protos.Protos.WalletPocket.Builder;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.AbstractWallet;
import com.coinomi.core.wallet.WalletAccount;
import com.google.common.collect.Lists;

import com.primestone.wallet.AddressBookProvider;
import com.primestone.wallet.R;
import com.primestone.wallet.Configuration;
import com.primestone.wallet.ExchangeRatesProvider;
import com.primestone.wallet.ExchangeRatesProvider.ExchangeRate;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.ui.widget.MDToast;
import com.primestone.wallet.util.ThrottlingWalletChangeListener;
import com.primestone.wallet.util.WeakHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.TransactionConfidence.ConfidenceType;
import org.bitcoinj.utils.Threading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BalanceListSend extends WalletFragment implements LoaderCallbacks<List<AbstractTransaction>> {
    private static final Logger log = LoggerFactory.getLogger(BalanceListSend.class);

    private String accountId;
    private TransListAdapterSend adapter;
    private final ContentObserver addressBookObserver = new AddressBookObserver(this.handler);
    private WalletApplication application;
    private Builder builder = null;
    private Configuration config;
    private Coin currentBalance;
    private ExchangeRate exchangeRate;
    private final MyHandler handler = new MyHandler(this);
    private boolean isFullAmount = false;

    private Listener listener;

    private WalletAccount pocket;
    private final LoaderCallbacks<Cursor> rateLoaderCallbacks = new C11213();
    private ContentResolver resolver;

    @Bind(R.id.transaction_rows) ListView transactionRows;

    private CoinType type;
    private final ThrottlingWalletChangeListener walletChangeListener = new C11191();

    class C11191 extends ThrottlingWalletChangeListener {
        C11191() {
        }

        public void onThrottledWalletChanged() {
            if (BalanceListSend.this.adapter != null) {
                BalanceListSend.this.adapter.notifyDataSetChanged();
            }
            BalanceListSend.this.handler.sendMessage(BalanceListSend.this.handler.obtainMessage(0));
        }
    }

    class C11213 implements LoaderCallbacks<Cursor> {
        C11213() {
        }

        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new ExchangeRateLoader(BalanceListSend.this.getActivity(), BalanceListSend.this.config, BalanceListSend.this.config.getExchangeCurrencyCode(), BalanceListSend.this.type.getSymbol());
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null && data.getCount() > 0) {
                data.moveToFirst();
                BalanceListSend.this.exchangeRate = ExchangeRatesProvider.getExchangeRate(data);
                BalanceListSend.this.handler.sendEmptyMessage(1);
                if (BalanceListSend.log.isInfoEnabled()) {
                    try {
                        BalanceListSend.log.info("Got exchange rate: {}", BalanceListSend.this.exchangeRate.rate.convert(BalanceListSend.this.type.oneCoin()).toFriendlyString());
                    } catch (Exception e) {
                        BalanceListSend.log.warn(e.getMessage());
                    }
                }
            }
        }

        public void onLoaderReset(Loader<Cursor> loader) {
        }
    }

    private static class AbstractTransactionsLoader extends AsyncTaskLoader<List<AbstractTransaction>> {
        private static final Comparator<AbstractTransaction> TRANSACTION_COMPARATOR = new C11232();
        private final WalletAccount account;
        private final ThrottlingWalletChangeListener transactionAddRemoveListener;

        class C11221 extends ThrottlingWalletChangeListener {
            C11221() {
            }

            public void onThrottledWalletChanged() {
                try {
                    AbstractTransactionsLoader.this.forceLoad();
                } catch (RejectedExecutionException e) {
                    BalanceListSend.log.info("rejected execution: " + AbstractTransactionsLoader.this.toString());
                }
            }
        }

        static class C11232 implements Comparator<AbstractTransaction> {
            C11232() {
            }

            public int compare(AbstractTransaction tx1, AbstractTransaction tx2) {
                boolean pending1;
                boolean pending2;
                int i = -1;
                if (tx1.getConfidenceType() == ConfidenceType.PENDING) {
                    pending1 = true;
                } else {
                    pending1 = false;
                }
                if (tx2.getConfidenceType() == ConfidenceType.PENDING) {
                    pending2 = true;
                } else {
                    pending2 = false;
                }
                if (pending1 == pending2) {
                    if (!(pending1 || pending2)) {
                        int time1 = tx1.getAppearedAtChainHeight();
                        int time2 = tx2.getAppearedAtChainHeight();
                        if (time1 != time2) {
                            if (time1 <= time2) {
                                i = 1;
                            }
                            return i;
                        }
                    }
                    if (Arrays.equals(tx1.getHashBytes(), tx2.getHashBytes())) {
                        return 1;
                    }
                    return -1;
                } else if (pending1) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }

        private AbstractTransactionsLoader(Context context, WalletAccount account) {
            super(context);
            this.account = account;
            this.transactionAddRemoveListener = new C11221();
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();
            this.account.addEventListener(this.transactionAddRemoveListener, Threading.SAME_THREAD);
            this.transactionAddRemoveListener.onWalletChanged(null);
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            this.account.removeEventListener(this.transactionAddRemoveListener);
            this.transactionAddRemoveListener.removeCallbacks();
            super.onStopLoading();
        }

        public List<AbstractTransaction> loadInBackground() {
            List<AbstractTransaction> filteredAbstractTransactions = Lists.newArrayList(this.account.getTransactions().values());
            Collections.sort(filteredAbstractTransactions, TRANSACTION_COMPARATOR);
            return filteredAbstractTransactions;
        }
    }

    static class AddressBookObserver extends ContentObserver {
        private final MyHandler handler;

        public AddressBookObserver(MyHandler handler) {
            super(handler);
            this.handler = handler;
        }

        public void onChange(boolean selfChange) {
            this.handler.sendEmptyMessage(2);
        }
    }

    public interface Listener {
        void onRefresh();
    }

    private static class MyHandler extends WeakHandler<BalanceListSend> {
        public MyHandler(BalanceListSend ref) {
            super(ref);
        }

        protected void weakHandleMessage(BalanceListSend ref, Message msg) {
            switch (msg.what) {
                case 0:
                    ref.updateBalance();
                    return;
                case 1:
                    ref.updateView();
                    return;
                case 2:
                    ref.clearLabelCache();
                    return;
                default:
                    return;
            }
        }
    }

    public static BalanceListSend newInstance(String accountId) {
        BalanceListSend fragment = new BalanceListSend();
        Bundle args = new Bundle();
        args.putSerializable("account_id", accountId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            this.accountId = getArguments().getString("account_id");
        }
        this.pocket = this.application.getAccount(this.accountId);
        if (this.pocket == null) {
            MDToast.makeText(getActivity(), getString(R.string.no_such_pocket_error), MDToast.LENGTH_LONG, 3);
        } else {
            this.type = this.pocket.getCoinType();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_send_balance, container, false);
        ButterKnife.bind((Object) this, view);
        setAllTypefaceThin(view);
        setupAdapter(inflater);
        return view;
    }

    @Override
    public void onDestroyView() {
        this.adapter = null;
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    private void setupAdapter(LayoutInflater inflater) {
        adapter = new TransListAdapterSend(inflater.getContext(), (AbstractWallet) this.pocket);
        adapter.setPrecision(6, 0);
        transactionRows.setAdapter(this.adapter);
    }

    private void setupConnectivityStatus() {
        this.handler.sendMessageDelayed(this.handler.obtainMessage(0), 2000);
    }

    @OnItemClick(R.id.transaction_rows)
    public void onItemClick(int position) {
        if (position >= this.transactionRows.getHeaderViewsCount()) {
            Object obj = this.transactionRows.getItemAtPosition(position);
            if (obj == null || !(obj instanceof AbstractTransaction)) {
                MDToast.makeText(getActivity(), getString(R.string.get_tx_info_error), 1, 3);
                return;
            }
            Intent intent = new Intent(getActivity(), TransactionDetailsActivity.class);
            intent.putExtra("account_id", this.accountId);
            intent.putExtra("transaction_id", ((AbstractTransaction) obj).getHashAsString());
            startActivity(intent);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setupConnectivityStatus();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void updateBalance() {
        updateBalance(this.pocket.getBalance());
    }

    private void updateBalance(Value newBalance) {
        this.currentBalance = newBalance.toCoin();
        updateView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.listener = (Listener) context;
            this.resolver = context.getContentResolver();
            this.application = (WalletApplication) context.getApplicationContext();
            this.config = this.application.getConfiguration();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.getClass() + " must implement " + Listener.class);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().initLoader(1, null, this.rateLoaderCallbacks);
    }

    @Override
    public void onDetach() {
        getLoaderManager().destroyLoader(0);
        getLoaderManager().destroyLoader(1);
        this.listener = null;
        this.resolver = null;
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.resolver.registerContentObserver(AddressBookProvider.contentUri(getActivity().getPackageName(), this.type), true, this.addressBookObserver);
        this.pocket.addEventListener(this.walletChangeListener, Threading.SAME_THREAD);
        updateView();
    }

    @Override
    public void onPause() {
        this.pocket.removeEventListener(this.walletChangeListener);
        this.walletChangeListener.removeCallbacks();
        this.resolver.unregisterContentObserver(this.addressBookObserver);
        super.onPause();
    }

    @Override
    public Loader<List<AbstractTransaction>> onCreateLoader(int id, Bundle args) {
        return new AbstractTransactionsLoader(getActivity(), this.pocket);
    }

    @Override
    public void onLoadFinished(Loader<List<AbstractTransaction>> loader, final List<AbstractTransaction> transactions) {
        this.handler.post(new Runnable() {
            public void run() {
                if (BalanceListSend.this.adapter != null) {
                    BalanceListSend.this.adapter.replace(transactions);
                }
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<List<AbstractTransaction>> loader) {
    }

    public void updateView() {
        if (!isRemoving() && !isDetached()) {
            if (this.currentBalance != null) {
                GenericUtils.formatCoinValue(this.type, this.currentBalance, this.isFullAmount ? 8 : 4, 0);
            }
            if (!(this.currentBalance == null || this.exchangeRate == null || getView() == null)) {
                try {
                    Value fiatAmount = this.exchangeRate.rate.convert(this.type, this.currentBalance);
                    this.exchangeRate.rate.convert(this.type.oneCoin()).toFriendlyString();
                } catch (Exception e) {
                }
            }
            if (this.adapter != null) {
                this.adapter.clearLabelCache();
            }
        }
    }

    private void clearLabelCache() {
        if (this.adapter != null) {
            this.adapter.clearLabelCache();
        }
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
