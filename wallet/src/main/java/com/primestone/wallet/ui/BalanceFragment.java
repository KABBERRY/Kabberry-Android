package com.primestone.wallet.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import com.baoyz.widget.PullRefreshLayout;
import com.baoyz.widget.PullRefreshLayout.OnRefreshListener;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.protos.Protos.WalletPocket.Builder;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.AbstractWallet;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.core.wallet.WalletConnectivityStatus;
import com.google.common.collect.Lists;

import com.primestone.wallet.AddressBookProvider;
import com.primestone.wallet.Constants;
import com.primestone.wallet.R;
import com.primestone.wallet.Configuration;
import com.primestone.wallet.ExchangeRatesProvider;
import com.primestone.wallet.ExchangeRatesProvider.ExchangeRate;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.ui.widget.Amount;
import com.primestone.wallet.ui.widget.MDToast;
import com.primestone.wallet.util.ThrottlingWalletChangeListener;
import com.primestone.wallet.util.WeakHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.utils.Threading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BalanceFragment extends WalletFragment implements LoaderCallbacks<List<AbstractTransaction>> {
    private static final Logger log = LoggerFactory.getLogger(BalanceFragment.class);

    private static final int WALLET_CHANGED = 0;
    private static final int UPDATE_VIEW = 1;
    private static final int CLEAR_LABEL_CACHE = 2;

    private static final int AMOUNT_FULL_PRECISION = 8;
    private static final int AMOUNT_MEDIUM_PRECISION = 6;
    private static final int AMOUNT_SHORT_PRECISION = 4;
    private static final int AMOUNT_SHIFT = 0;

    private static final int ID_TRANSACTION_LOADER = 0;
    private static final int ID_RATE_LOADER = 1;

    private String accountId;
    private WalletAccount pocket;
    private CoinType type;
    private Coin currentBalance;
    private ExchangeRate exchangeRate;

    private boolean isFullAmount = false;
    private WalletApplication application;
    private Configuration config;
    private final MyHandler handler = new MyHandler(this);
    private final ContentObserver addressBookObserver = new AddressBookObserver(this.handler);

    private Builder builder = null;

    @Bind(R.id.transaction_rows) ListView transactionRows;
    @Bind(R.id.swipeContainer) PullRefreshLayout swipeContainer;
    @Bind(R.id.ll_empty) LinearLayout emptyPocketMessage;
    @Bind(R.id.account_balance) Amount accountBalance;
    @Bind(R.id.account_exchanged_balance) Amount accountExchangedBalance;
    @Bind(R.id.connection_label) TextView connectionLabel;
    @Bind(R.id.tv_local_price) TextView tv_local_price;
    @Bind(R.id.blockheight_label) TextView blockHeight;
    @Bind(R.id.empty_title) TextView empty_title;
    private TransactionsListAdapter adapter;
    private Listener listener;
    private ContentResolver resolver;

    public static BalanceFragment newInstance(String accountId) {
        BalanceFragment fragment = new BalanceFragment();
        Bundle args = new Bundle();
        args.putSerializable("account_id", accountId);
        fragment.setArguments(args);
        return fragment;
    }

    public BalanceFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        if (getArguments() != null) {
            accountId = getArguments().getString(Constants.ARG_ACCOUNT_ID);
        }

        pocket = application.getAccount(accountId);
        if (pocket == null) {
            MDToast.makeText(getActivity(), getString(R.string.no_such_pocket_error), MDToast.LENGTH_LONG, 3).show();
            return;
        }
        type = pocket.getCoinType();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_balance, container, false);
        addHeaderAndFooterToList(inflater, container, view);
        ButterKnife.bind((Object) this, view);

        setAllTypefaceThin(view);
        setupSwipeContainer();

        tv_local_price.setTypeface(Typeface.createFromAsset(getActivity().getAssets(),
                "fonts/Roboto-Bold.ttf"));
        empty_title.setTypeface(Typeface.createFromAsset(getActivity().getAssets(),
                "fonts/Roboto-Bold.ttf"));

        if (pocket.getTransactions().size() > 0) {
            emptyPocketMessage.setVisibility(View.GONE);
        }

        setupAdapter(inflater);
        accountBalance.setSymbol(type.getSymbol());
        exchangeRate = ExchangeRatesProvider.getRate(
                application.getApplicationContext(), type.getSymbol(), config.getExchangeCurrencyCode());

        // Update the amount
        updateBalance(pocket.getBalance());

        return view;
    }

    @Override
    public void onDestroyView() {
        adapter = null;
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    private void setupAdapter(LayoutInflater inflater) {
        adapter = new TransactionsListAdapter(inflater.getContext(), (AbstractWallet) pocket);
        adapter.setPrecision(AMOUNT_MEDIUM_PRECISION, 0);
        transactionRows.setAdapter(adapter);
    }

    private void setupSwipeContainer() {
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (listener != null) {
                    listener.onRefresh();
                }
            }
        });
    }

    private void addHeaderAndFooterToList(LayoutInflater inflater, ViewGroup container, View view) {
        ListView list = (ListView) ButterKnife.findById(view, (int) R.id.transaction_rows);
        View header = inflater.inflate(R.layout.fragment_balance_header, null);
        setAllTypefaceThin(header);
        list.addHeaderView(header, null, true);

        View listFooter = new View(inflater.getContext());
        setAllTypefaceThin(listFooter);
        listFooter.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin));
        list.addFooterView(listFooter);
    }

    private void setupConnectivityStatus() {
        setConnectivityStatus(WalletConnectivityStatus.CONNECTED);
        handler.sendMessageDelayed(handler.obtainMessage(WALLET_CHANGED), 2000);
    }

    @OnItemClick(R.id.transaction_rows)
    public void onItemClick(int position) {
        if (position >= transactionRows.getHeaderViewsCount()) {
            Object obj = transactionRows.getItemAtPosition(position);

            if (obj == null || !(obj instanceof AbstractTransaction)) {
                MDToast.makeText(getActivity(), getString(R.string.get_tx_info_error), 1, 3).show();
                return;
            }

            Intent intent = new Intent(getActivity(), TransactionDetailsActivity.class);
            intent.putExtra(Constants.ARG_ACCOUNT_ID, accountId);
            intent.putExtra("transaction_id", ((AbstractTransaction) obj).getHashAsString());
            startActivity(intent);
        }
    }

    @OnClick(R.id.account_balance)
    public void onMainAmountClick() {
        isFullAmount = !isFullAmount;
        updateView();
    }

    @OnClick(R.id.account_exchanged_balance)
    public void onLocalAmountClick() {
        ////////
        //if (listener != null) listener.onLocalAmountClick();
        ////////
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

    // TODO use the ListView feature that shows a view on empty list. Check exchange rates fragment
    @Deprecated
    private void checkEmptyPocketMessage() {
        if (emptyPocketMessage.isShown()) {
            if (!pocket.isNew()) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        emptyPocketMessage.setVisibility(View.GONE);
                    }
                });
            }
        }
    }

    private void updateBalance() {
        updateBalance(pocket.getBalance());
    }

    private void updateBalance(Value newBalance) {
        currentBalance = newBalance.toCoin();

        updateView();
    }

    private void updateConnectivityStatus() {
        setConnectivityStatus(pocket.getConnectivityStatus());
    }

    private void setConnectivityStatus(final WalletConnectivityStatus connectivity) {
        switch (connectivity) {
            case CONNECTED:
            case LOADING:
                connectionLabel.setVisibility(View.GONE);
                break;
            case DISCONNECTED:
                this.connectionLabel.setVisibility(View.VISIBLE);
                break;
            default:
                throw new RuntimeException("Unknown connectivity status: " + connectivity);
        }
    }

    private final ThrottlingWalletChangeListener walletChangeListener = new ThrottlingWalletChangeListener() {
        @Override
        public void onThrottledWalletChanged() {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            handler.sendMessage(handler.obtainMessage(WALLET_CHANGED));
        }

        @Override
        public void onTransactionConfidenceChanged(WalletAccount pocket, AbstractTransaction tx) {

        }
    };

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        try {
            listener = (Listener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.getClass() + " must implement " + Listener.class);
        }
        resolver = context.getContentResolver();
        application = (WalletApplication) context.getApplicationContext();
        config = application.getConfiguration();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(ID_TRANSACTION_LOADER, null, this);
        getLoaderManager().initLoader(ID_RATE_LOADER, null, rateLoaderCallbacks);
    }

    @Override
    public void onDetach() {
        getLoaderManager().destroyLoader(ID_TRANSACTION_LOADER);
        getLoaderManager().destroyLoader(ID_RATE_LOADER);
        listener = null;
        resolver = null;
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();

        resolver.registerContentObserver(AddressBookProvider.contentUri(
                getActivity().getPackageName(), type), true, addressBookObserver);

        pocket.addEventListener(walletChangeListener, Threading.SAME_THREAD);

        checkEmptyPocketMessage();

        updateView();
    }

    @Override
    public void onPause() {
        pocket.removeEventListener(walletChangeListener);
        walletChangeListener.removeCallbacks();

        resolver.unregisterContentObserver(addressBookObserver);

        super.onPause();
    }

    @Override
    public Loader<List<AbstractTransaction>> onCreateLoader(int id, Bundle args) {
        return new AbstractTransactionsLoader(getActivity(), pocket);
        //return null;
    }

    @Override
    public void onLoadFinished(Loader<List<AbstractTransaction>> loader, final List<AbstractTransaction> transactions) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (adapter != null) {
                    adapter.replace(transactions);
                }
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<List<AbstractTransaction>> loader) {
    }

    private static class AbstractTransactionsLoader extends AsyncTaskLoader<List<AbstractTransaction>> {
        private final WalletAccount account;
        private final ThrottlingWalletChangeListener transactionAddRemoveListener;

        private AbstractTransactionsLoader(final Context context, final WalletAccount account) {
            super(context);

            this.account = account;
            transactionAddRemoveListener = new ThrottlingWalletChangeListener() {
                @Override
                public void onThrottledWalletChanged() {
                    try {
                        AbstractTransactionsLoader.this.forceLoad();
                    } catch (RejectedExecutionException e) {
                        BalanceFragment.log.info("rejected execution: " + AbstractTransactionsLoader.this.toString());
                    }
                }

                @Override
                public void onTransactionConfidenceChanged(WalletAccount pocket, AbstractTransaction tx) {

                }
            };
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();

            account.addEventListener(transactionAddRemoveListener, Threading.SAME_THREAD);
            transactionAddRemoveListener.onWalletChanged(null);

            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            account.removeEventListener(transactionAddRemoveListener);
            transactionAddRemoveListener.removeCallbacks();

            super.onStopLoading();
        }

        @Override
        public List<AbstractTransaction> loadInBackground() {
            final List<AbstractTransaction> filteredAbstractTransactions = Lists.newArrayList(account.getTransactions().values());

            Collections.sort(filteredAbstractTransactions, TRANSACTION_COMPARATOR);

            return filteredAbstractTransactions;
        }

        private static final Comparator<AbstractTransaction> TRANSACTION_COMPARATOR = new Comparator<AbstractTransaction>() {

            @Override
            public int compare(AbstractTransaction tx1, AbstractTransaction tx2) {
                final boolean pending1 = tx1.getConfidenceType() == TransactionConfidence.ConfidenceType.PENDING;
                final boolean pending2 = tx2.getConfidenceType() == TransactionConfidence.ConfidenceType.PENDING;

                int i = -1;

                if (pending1 != pending2)
                    return pending1 ? -1 : 1;


                if (!pending1 && !pending2) {
                    final int time1 = tx1.getAppearedAtChainHeight();
                    final int time2 = tx2.getAppearedAtChainHeight();
                    if (time1 != time2)
                        return time1 > time2 ? -1 : 1;
                }

                return Arrays.equals(tx1.getHashBytes(),tx2.getHashBytes()) ? 1 : -1;
            }
        };

    }

    private final LoaderCallbacks<Cursor> rateLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String localSymbol = config.getExchangeCurrencyCode();
            String coinSymbol = type.getSymbol();
            return new ExchangeRateLoader(getActivity(), config, localSymbol, coinSymbol);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
            if (data != null && data.getCount() > 0) {
                data.moveToFirst();
                exchangeRate = ExchangeRatesProvider.getExchangeRate(data);
                handler.sendEmptyMessage(UPDATE_VIEW);
                if (log.isInfoEnabled()) {
                    try {
                        log.info("Got exchange rate: {}", exchangeRate.rate.convert(type.oneCoin()).toFriendlyString());
                    } catch (Exception e) {
                        log.warn(e.getMessage());
                    }
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    //@Override
    public void updateView() {
        if (isRemoving() || isDetached()) return;

        if (currentBalance != null) {
            String newBalanceStr = GenericUtils.formatCoinValue(type, currentBalance,
                    isFullAmount ? AMOUNT_FULL_PRECISION : AMOUNT_SHORT_PRECISION, AMOUNT_SHIFT);
            accountBalance.setAmount(newBalanceStr);
//                accountBalance.setAmount(GenericUtils.formatCoinValue(type, currentBalance,
//                        isFullAmount ? 8 : 4, 0));
        }

        if (currentBalance != null && exchangeRate != null || getView() != null) {
            try {
                Value fiatAmount = exchangeRate.rate.convert(type, currentBalance);
                String Exchange_rate = exchangeRate.rate.convert(this.type.oneCoin()).toFriendlyString();
                tv_local_price.setText("$" + Exchange_rate.substring(0, Exchange_rate.indexOf(" ")));
                accountExchangedBalance.setAmount(GenericUtils.formatFiatValue(fiatAmount));
                accountExchangedBalance.setSymbol(fiatAmount.type.getSymbol());
            } catch (Exception e) {
                tv_local_price.setText("");
                accountExchangedBalance.setAmount("");
                accountExchangedBalance.setSymbol("ERROR");
            }
        }

        swipeContainer.setRefreshing(pocket.isLoading());

        blockHeight.setText(getResources().getString(R.string.block_height) + " " +
                String.valueOf(pocket.getLastBlockSeenHeight()));
        setAllTypeNumber(blockHeight);

        if (adapter != null) {
            adapter.clearLabelCache();
        }
    }

    private void clearLabelCache() {
        if (adapter != null) {
            adapter.clearLabelCache();
        }
    }

    private static class MyHandler extends WeakHandler<BalanceFragment> {
        public MyHandler(BalanceFragment ref) {
            super(ref);
        }

        @Override
        protected void weakHandleMessage(BalanceFragment ref, Message msg) {
            switch (msg.what) {
                case WALLET_CHANGED:
                    ref.updateBalance();
                    ref.checkEmptyPocketMessage();
                    ref.updateConnectivityStatus();
                    break;
                case UPDATE_VIEW:
                    ref.updateView();
                    break;
                case CLEAR_LABEL_CACHE:
                    ref.clearLabelCache();
                    break;
            }
        }
    }

    static class AddressBookObserver extends ContentObserver {
        private final MyHandler handler;

        public AddressBookObserver(MyHandler handler) {
            super(handler);
            this.handler = handler;
        }

        @Override
        public void onChange(final boolean selfChange) {
            handler.sendEmptyMessage(CLEAR_LABEL_CACHE);
        }
    }

    public interface Listener {
        //void onLocalAmountClick();
        void onRefresh();
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

    protected void setAllTypeNumber(View view) {
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getChildCount() != 0) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllTypeNumber(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Regular.ttf"));
        }
    }
}
