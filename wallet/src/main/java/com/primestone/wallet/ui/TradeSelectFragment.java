package com.primestone.wallet.ui;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.exchange.shapeshift.ShapeShift;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftCoins;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftMarketInfo;
import com.coinomi.core.util.ExchangeRate;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.primestone.wallet.R;
import com.primestone.wallet.Constants;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.tasks.ExchangeCheckSupportedCoinsTask;
import com.primestone.wallet.tasks.MarketInfoPollTask;
import com.primestone.wallet.ui.Dialogs.ProgressDialogFragment;
import com.primestone.wallet.ui.adaptors.AvailableAccountsAdaptor;
import com.primestone.wallet.ui.adaptors.AvailableAccountsAdaptor.Entry;
import com.primestone.wallet.ui.widget.AmountEditView;
import com.primestone.wallet.util.Keyboard;
import com.primestone.wallet.util.ThrottlingWalletChangeListener;
import com.primestone.wallet.util.WeakHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.Threading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class TradeSelectFragment extends Fragment implements ExchangeCheckSupportedCoinsTask.Listener {
    private static final Logger log = LoggerFactory.getLogger(TradeSelectFragment.class);

    private final Handler handler = new MyHandler(this);
    @Nullable private MenuItem actionSwapMenu;
    private CurrencyCalculatorLink amountCalculatorLink;
    private TextView amountError;
    private TextView amountWarning;
    private final AmountListener amountsListener = new AmountListener(handler);
    private WalletApplication application;
    private WalletAccount destinationAccount;
    private AvailableAccountsAdaptor destinationAdapter;
    private AmountEditView destinationAmountView;
    private Spinner destinationSpinner;
    private CoinType destinationType;

    private ExchangeCheckSupportedCoinsTask initialTask;
    private Value lastBalance;
    private ExchangeRate lastRate;
    @Nullable private Listener listener;
    private MarketInfoTask marketTask;
    private Value maximumDeposit;
    private Value minimumDeposit;
    private Button nextButton;
    private MyMarketInfoPollTask pollTask;
    private Value sendAmount;
    private WalletAccount sourceAccount;
    private final AccountListener sourceAccountListener = new AccountListener(handler);
    private AvailableAccountsAdaptor sourceAdapter;
    private AmountEditView sourceAmountView;
    private Spinner sourceSpinner;
    private Timer timer;
    private Wallet wallet;

    public interface Listener {
        void onMakeTrade(WalletAccount walletAccount, WalletAccount walletAccount2, Value value);
    }

    class C12651 implements OnClickListener {
        C12651() {
        }

        public void onClick(View v) {
            new Builder(TradeSelectFragment.this.getActivity()).setTitle(R.string.about_shapeshift_title).setMessage(R.string.about_shapeshift_message).setPositiveButton(R.string.button_ok, null).create().show();
        }
    }

    class C12662 implements OnClickListener {
        C12662() {
        }

        public void onClick(View v) {
            TradeSelectFragment.this.validateAmount();
            if (TradeSelectFragment.this.everythingValid()) {
                TradeSelectFragment.this.onHandleNext();
            } else if (TradeSelectFragment.this.amountCalculatorLink.isEmpty()) {
                TradeSelectFragment.this.amountError.setText(R.string.amount_error_empty);
                TradeSelectFragment.this.amountError.setVisibility(0);
            }
        }
    }

    class C12673 implements DialogInterface.OnClickListener {
        C12673() {
        }

        public void onClick(DialogInterface dialog, int which) {
            TradeSelectFragment.this.initialTask = null;
            TradeSelectFragment.this.maybeStartInitialTask();
        }
    }

    private static class AccountListener extends ThrottlingWalletChangeListener {
        private final Handler handler;

        private AccountListener(Handler handler) {
            this.handler = handler;
        }

        public void onThrottledWalletChanged() {
            this.handler.sendEmptyMessage(2);
        }
    }

    private static class AmountListener implements com.primestone.wallet.ui.widget.AmountEditView.Listener {
        private final Handler handler;

        private AmountListener(Handler handler) {
            this.handler = handler;
        }

        public void changed() {
            this.handler.sendMessage(this.handler.obtainMessage(3, Boolean.valueOf(true)));
        }

        public void focusChanged(boolean hasFocus) {
            if (!hasFocus) {
                this.handler.sendMessage(this.handler.obtainMessage(3, Boolean.valueOf(false)));
            }
        }
    }

    private static class MarketInfoTask extends AsyncTask<Void, Void, ShapeShiftMarketInfo> {
        final Handler handler;
        final String pair;
        final ShapeShift shapeShift;

        private MarketInfoTask(Handler handler, ShapeShift shift, String pair) {
            this.shapeShift = shift;
            this.handler = handler;
            this.pair = pair;
        }

        protected ShapeShiftMarketInfo doInBackground(Void... params) {
            return MarketInfoPollTask.getMarketInfoSync(this.shapeShift, this.pair);
        }

        protected void onPostExecute(ShapeShiftMarketInfo marketInfo) {
            if (marketInfo != null) {
                this.handler.sendMessage(this.handler.obtainMessage(0, marketInfo));
            } else {
                this.handler.sendEmptyMessage(1);
            }
        }
    }

    private static class MyHandler extends WeakHandler<TradeSelectFragment> {
        public MyHandler(TradeSelectFragment referencingObject) {
            super(referencingObject);
        }

        protected void weakHandleMessage(TradeSelectFragment ref, Message msg) {
            switch (msg.what) {
                case 0:
                    ref.onMarketUpdate((ShapeShiftMarketInfo) msg.obj);
                    return;
                case 1:
                    Toast.makeText(ref.getActivity(), ref.getString(R.string.trade_error_market_info, ref.sourceAccount.getCoinType().getName(), ref.destinationType.getName()), 1).show();
                    return;
                case 2:
                    ref.onWalletUpdate();
                    return;
                case 3:
                    ref.validateAmount(((Boolean) msg.obj).booleanValue());
                    return;
                case 4:
                    ref.showInitialTaskErrorDialog((String) msg.obj);
                    return;
                case 5:
                    ref.updateAvailableCoins((ShapeShiftCoins) msg.obj);
                    ref.startMarketInfoTask();
                    return;
                default:
                    return;
            }
        }
    }

    private static class MyMarketInfoPollTask extends MarketInfoPollTask {
        private final Handler handler;

        MyMarketInfoPollTask(Handler handler, ShapeShift shapeShift, String pair) {
            super(shapeShift, pair);
            this.handler = handler;
        }

        public void onHandleMarketInfo(ShapeShiftMarketInfo marketInfo) {
            this.handler.sendMessage(this.handler.obtainMessage(0, marketInfo));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(true);
        sourceAccount = application.getAccount(application.getConfiguration().getLastAccountId());
        if (sourceAccount == null) {
            sourceAccount = (WalletAccount) application.getAllAccounts().get(0);
        }
        for (CoinType type : Constants.SUPPORTED_COINS) {
            if (!type.equals(sourceAccount.getCoinType())) {
                destinationType = type;
                break;
            }
        }
        updateBalance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trade_select, container, false);

        sourceSpinner = (Spinner) view.findViewById(R.id.from_coin);
        sourceSpinner.setAdapter(getSourceSpinnerAdapter());
        sourceSpinner.setOnItemSelectedListener(getSourceSpinnerListener());

        destinationSpinner = (Spinner) view.findViewById(R.id.to_coin);
        destinationSpinner.setAdapter(getDestinationSpinnerAdapter());
        destinationSpinner.setOnItemSelectedListener(getDestinationSpinnerListener());

        sourceAmountView = (AmountEditView) view.findViewById(R.id.trade_coin_amount);
        destinationAmountView = (AmountEditView) view.findViewById(R.id.receive_coin_amount);

        amountCalculatorLink = new CurrencyCalculatorLink(sourceAmountView, destinationAmountView);

        amountError = (TextView) view.findViewById(R.id.amount_error_message);
        amountError.setVisibility(View.GONE);
        amountWarning = (TextView) view.findViewById(R.id.amount_warning_message);
        amountWarning.setVisibility(View.GONE);

        view.findViewById(R.id.powered_by_shapeshift).setOnClickListener(new C12651());

        nextButton = (Button) view.findViewById(R.id.button_next);

        nextButton.setOnClickListener(new C12662());

        setSource(sourceAccount, false);

        if (destinationAccount != null) {
            setDestination(destinationAccount, false);
        } else {
            setDestination(destinationType, false);
        }

        if (application.isConnected()) {
            maybeStartInitialTask();
        } else {
            showInitialTaskErrorDialog(null);
        }

        return view;
    }

    private AvailableAccountsAdaptor getDestinationSpinnerAdapter() {
        if (this.destinationAdapter == null) {
            this.destinationAdapter = new AvailableAccountsAdaptor(getActivity());
        }
        return this.destinationAdapter;
    }

    private AvailableAccountsAdaptor getSourceSpinnerAdapter() {
        if (this.sourceAdapter == null) {
            this.sourceAdapter = new AvailableAccountsAdaptor(getActivity());
        }
        return this.sourceAdapter;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.listener = (Listener) context;
            this.application = (WalletApplication) context.getApplicationContext();
            this.wallet = this.application.getWallet();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.getClass() + " must implement " + Listener.class);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.trade, menu);
        this.actionSwapMenu = menu.findItem(R.id.action_swap_coins);
    }

    @Override
    public void onPause() {
        stopPolling();
        removeSourceListener();
        amountCalculatorLink.setListener(null);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        startPolling();
        amountCalculatorLink.setListener(amountsListener);
        addSourceListener();
        updateNextButtonState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_swap_coins:
                swapAccounts();
                return true;
            case R.id.action_refresh:
                refreshStartInitialTask();
                return true;
            case R.id.action_exchange_history:
                startActivity(new Intent(getActivity(), ExchangeHistoryActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //////// Methods
    private void onHandleNext() {
        if (listener == null) {
            return;
        }
        if (destinationAccount == null) {
            createToAccountAndProceed();
        } else if (everythingValid()) {
            Keyboard.hideKeyboard(getActivity());
            listener.onMakeTrade(sourceAccount, destinationAccount, sendAmount);
        } else {
            Toast.makeText(getActivity(), R.string.amount_error, 1).show();
        }
    }

    private void createToAccountAndProceed() {
        if (destinationType == null) {
            Toast.makeText(getActivity(), R.string.error_generic,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void addSourceListener() {
        sourceAccount.addEventListener(sourceAccountListener, Threading.SAME_THREAD);
        onWalletUpdate();
    }

    private void removeSourceListener() {
        sourceAccount.removeEventListener(sourceAccountListener);
        sourceAccountListener.removeCallbacks();
    }

    private void startPolling() {
        if (timer == null) {
            pollTask = new MyMarketInfoPollTask(handler, application.getShapeShift(), getPair());
            timer = new Timer();
            timer.schedule(pollTask, 0, 30000);
        }
    }

    private void stopPolling() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer.purge();
            this.timer = null;
            this.pollTask.cancel();
            this.pollTask = null;
        }
    }

    /**
     * Updates the spinners to include only available and supported coins
     */
    private void updateAvailableCoins(ShapeShiftCoins availableCoins) {
        List<CoinType> supportedTypes = getSupportedTypes(availableCoins.availableCoinTypes);
        List<WalletAccount> allAccounts = application.getAllAccounts();

        sourceAdapter.update(allAccounts, supportedTypes, false);
        List<CoinType> sourceTypes = sourceAdapter.getTypes();

        // No supported source accounts found
        if (sourceTypes.size() == 0) {
            new Builder(getActivity())
                    .setTitle(R.string.trade_error)
                    .setMessage(R.string.trade_error_no_supported_source_accounts)
                    .setPositiveButton(R.string.button_ok, null)
                    .create()
                    .show();
            return;
        }

        if (this.sourceSpinner.getSelectedItemPosition() == -1) {
            if (this.sourceAccount == null || this.sourceAdapter.getAccountOrTypePosition(this.sourceAccount) == -1) {
                this.sourceSpinner.setSelection(0);
            } else {
                this.sourceSpinner.setSelection(this.sourceAdapter.getAccountOrTypePosition(this.sourceAccount));
            }
        }

        CoinType sourceType = ((Entry) sourceSpinner.getSelectedItem()).getType();

        // If we have only one source type, remove it as a destination
        if (sourceTypes.size() == 1) {
            ArrayList<CoinType> typesWithoutSourceType = Lists.newArrayList(supportedTypes);
            typesWithoutSourceType.remove(sourceType);
            this.destinationAdapter.update(allAccounts, typesWithoutSourceType, true);
        } else {
            this.destinationAdapter.update(allAccounts, supportedTypes, true);
        }

        if (this.destinationSpinner.getSelectedItemPosition() == -1) {
            for (Entry entry : this.destinationAdapter.getEntries()) {
                if (!sourceType.equals(entry.getType())) {
                    this.destinationSpinner.setSelection(this.destinationAdapter.getAccountOrTypePosition(entry.accountOrCoinType));
                    return;
                }
            }
        }
    }

    /**
     * Show a no connectivity error
     */
    private void showInitialTaskErrorDialog(String error) {
        if (getActivity() != null) {
            DialogBuilder builder;
            if (error == null) {
                builder = DialogBuilder.warn(getActivity(), R.string.trade_warn_no_connection_title);
                builder.setMessage((int) R.string.trade_warn_no_connection_message);
            } else {
                builder = DialogBuilder.warn(getActivity(), R.string.trade_error);
                builder.setMessage((int) R.string.trade_error_service_not_available);
            }
            builder.setNegativeButton((int) R.string.button_dismiss, null);
            builder.setPositiveButton((int) R.string.button_retry, new C12673());
            builder.create().show();
        }
    }

    /**
     * Returns a list of the supported coins from the list of the available coins
     */
    private List<CoinType> getSupportedTypes(List<CoinType> availableCoins) {
        ImmutableList.Builder<CoinType> builder = ImmutableList.builder();
        for (CoinType supportedType : Constants.SUPPORTED_COINS) {
            if (availableCoins.contains(supportedType)) {
                builder.add(supportedType);
            }
        }
        return builder.build();
    }

    private void refreshStartInitialTask() {
        if (initialTask != null) {
            initialTask.cancel(true);
            initialTask = null;
        }
        maybeStartInitialTask();
    }

    private void maybeStartInitialTask() {
        if (initialTask == null) {
            initialTask = new ExchangeCheckSupportedCoinsTask(this, application);
            initialTask.execute(new Void[0]);
        }
    }

    @Override
    public void onExchangeCheckSupportedCoinsTaskStarted() {
        ProgressDialogFragment.show(getFragmentManager(), getString(R.string.contacting_exchange), "initial_task_busy_dialog_tag");
    }

    @Override
    public void onExchangeCheckSupportedCoinsTaskFinished(Exception error, ShapeShiftCoins shapeShiftCoins) {
        if (!Dialogs.dismissAllowingStateLoss(getFragmentManager(), "initial_task_busy_dialog_tag")) {
            if (error != null) {
                log.warn("Could not get ShapeShift coins", (Throwable) error);
                this.handler.sendMessage(this.handler.obtainMessage(4, error.getMessage()));
            } else if (shapeShiftCoins.isError) {
                log.warn("Could not get ShapeShift coins: {}", shapeShiftCoins.errorMessage);
                this.handler.sendMessage(this.handler.obtainMessage(4, shapeShiftCoins.errorMessage));
            } else {
                this.handler.sendMessage(this.handler.obtainMessage(5, shapeShiftCoins));
            }
        }
    }

    /**
     * Starts a new task to query about the market of the currently selected pair.
     * Notes:
     *  - If a task is already running, this call will cancel it.
     *  - If the fragment is detached, it will not run.
     */
    private void startMarketInfoTask() {
        if (marketTask != null) {
            marketTask.cancel(true);
            marketTask = null;
        }
        if (getActivity() != null) {
            marketTask = new MarketInfoTask(handler, application.getShapeShift(), getPair());
            marketTask.execute();
        }
    }

    /**
     * Get the current source and destination pair
     */
    private String getPair() {
        return ShapeShift.getPair(this.sourceAccount.getCoinType(), this.destinationType);
    }

    /**
     * Updates the exchange rate and limits for the specific market.
     * Note: if the current pair is different that the marketInfo pair, do nothing
     */
    private void onMarketUpdate(ShapeShiftMarketInfo marketInfo) {
        if (marketInfo.isPair(this.sourceAccount.getCoinType(), this.destinationType)) {
            this.maximumDeposit = marketInfo.limit;
            this.minimumDeposit = marketInfo.minimum;

            this.lastRate = marketInfo.rate;
            this.amountCalculatorLink.setExchangeRate(this.lastRate);
            if (this.amountCalculatorLink.isEmpty() && this.lastRate != null) {
                Value hintValue = this.sourceAccount.getCoinType().oneCoin();
                Value exchangedValue = this.lastRate.convert(hintValue);
                Value minerFee100 = marketInfo.rate.minerFee.multiply(100);
                for (int tries = 8; tries > 0 && (exchangedValue.isZero() || exchangedValue.compareTo(minerFee100) < 0); tries--) {
                    hintValue = hintValue.multiply(10);
                    exchangedValue = this.lastRate.convert(hintValue);
                }
                this.amountCalculatorLink.setExchangeRateHints(hintValue);
            }
        }
    }

    // 12684
    /**
     * Get the item selected listener for the source spinner. It will swap the accounts if the
     * destination account is the same as the new source account.
     */
    private OnItemSelectedListener getSourceSpinnerListener() {
        return new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Entry entry = (Entry) parent.getSelectedItem();
                if (entry.accountOrCoinType instanceof WalletAccount) {
                    WalletAccount newSource = (WalletAccount) entry.accountOrCoinType;

                    if (!newSource.equals(sourceAccount)) {
                        if (destinationAccount != null && destinationAccount.isType(newSource)) {
                            setDestinationSpinner(sourceAccount);
                            setDestination(sourceAccount, false);
                        }
                        setSource(newSource, true);
                        return;
                    }
                    return;
                }
                throw new IllegalStateException("Unexpected class: " + entry.accountOrCoinType.getClass());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        };
    }

    // 12695
    /**
     * Get the item selected listener for the destination spinner. It will swap the accounts if the
     * source account is the same as the new destination account.
     */
    private OnItemSelectedListener getDestinationSpinnerListener() {
        return new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Entry entry = (Entry) parent.getSelectedItem();
                if (entry.accountOrCoinType instanceof WalletAccount) {
                    WalletAccount newDestination = (WalletAccount) entry.accountOrCoinType;
                    if (!newDestination.equals(destinationAccount)) {
                        if (destinationAccount != null && sourceAccount.isType(newDestination)) {
                            setSourceSpinner(destinationAccount);
                            setSource(destinationAccount, false);
                        }
                        TradeSelectFragment.this.setDestination(newDestination, true);
                    }
                } else if (entry.accountOrCoinType instanceof CoinType) {
                    setDestination((CoinType) entry.accountOrCoinType, true);
                } else {
                    throw new IllegalStateException("Unexpected class: " + entry.accountOrCoinType.getClass());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        };
    }

    /**
     * Selects an account on the sourceSpinner without calling the callback. If no account found in
     * the adaptor, does not do anything
     */
    private void setSourceSpinner(WalletAccount account) {
        int newPosition = this.sourceAdapter.getAccountOrTypePosition(account);
        if (newPosition >= 0) {
            OnItemSelectedListener cb = this.sourceSpinner.getOnItemSelectedListener();
            this.sourceSpinner.setOnItemSelectedListener(null);
            this.sourceSpinner.setSelection(newPosition);
            this.sourceSpinner.setOnItemSelectedListener(cb);
        }
    }

    /**
     * Selects an account on the destinationSpinner without calling the callback. If no account
     * found in the adaptor, does not do anything
     */
    private void setDestinationSpinner(Object accountOrType) {
        int newPosition = this.destinationAdapter.getAccountOrTypePosition(accountOrType);
        if (newPosition >= 0) {
            OnItemSelectedListener cb = this.destinationSpinner.getOnItemSelectedListener();
            this.destinationSpinner.setOnItemSelectedListener(null);
            this.destinationSpinner.setSelection(newPosition);
            this.destinationSpinner.setOnItemSelectedListener(cb);
        }
    }

    /**
     * Sets the source account and makes a network call to ask about the new pair.
     * Note: this does not update the source spinner, use {@link #setSourceSpinner(WalletAccount)}
     */
    private void setSource(WalletAccount account, boolean startNetworkTask) {
        removeSourceListener();
        this.sourceAccount = account;
        addSourceListener();
        this.sourceAmountView.reset();
        this.sourceAmountView.setType(this.sourceAccount.getCoinType());
        this.sourceAmountView.setFormat(this.sourceAccount.getCoinType().getMonetaryFormat());
        this.amountCalculatorLink.setExchangeRate(null);
        this.minimumDeposit = null;
        this.maximumDeposit = null;
        updateOptionsMenu();
        if (startNetworkTask) {
            startMarketInfoTask();
            if (this.pollTask != null) {
                this.pollTask.updatePair(getPair());
            }
            this.application.maybeConnectAccount(this.sourceAccount);
        }
    }

    /**
     * Sets the destination account and makes a network call to ask about the new pair.
     * Note: this does not update the destination spinner, use
     * {@link #setDestinationSpinner(Object)}
     */
    private void setDestination(WalletAccount account, boolean startNetworkTask) {
        setDestination(account.getCoinType(), false);
        this.destinationAccount = account;
        updateOptionsMenu();
        if (startNetworkTask) {
            startMarketInfoTask();
            if (this.pollTask != null) {
                this.pollTask.updatePair(getPair());
            }
        }
    }

    /**
     * Sets the destination coin type and makes a network call to ask about the new pair.
     * Note: this does not update the destination spinner, use
     * {@link #setDestinationSpinner(Object)}
     */
    private void setDestination(CoinType type, boolean startNetworkTask) {
        this.destinationAccount = null;
        this.destinationType = type;
        this.destinationAmountView.reset();
        this.destinationAmountView.setType(this.destinationType);
        this.destinationAmountView.setFormat(this.destinationType.getMonetaryFormat());
        this.amountCalculatorLink.setExchangeRate(null);
        this.minimumDeposit = null;
        this.maximumDeposit = null;
        updateOptionsMenu();
        if (startNetworkTask) {
            startMarketInfoTask();
            if (this.pollTask != null) {
                this.pollTask.updatePair(getPair());
            }
        }
    }

    /**
     * Swap the source & destination accounts.
     * Note: this works if the destination is an account, not a CoinType.
     */
    private void swapAccounts() {
        if (isSwapAccountPossible()) {
            WalletAccount newSource = this.destinationAccount;
            WalletAccount newDestination = this.sourceAccount;
            setSourceSpinner(newSource);
            setDestinationSpinner(newDestination);
            setSource(newSource, false);
            setDestination(newDestination, true);
            return;
        }
        Toast.makeText(getActivity(), R.string.error_generic, 0).show();
    }

    /**
     * Check is if possible to perform the {@link #swapAccounts()} action
     */
    private boolean isSwapAccountPossible() {
        return this.destinationAccount != null;
    }

    /**
     * Updates the options menu to take in to account the new selected accounts types, i.e. disable
     * the swap action
     */
    private void updateOptionsMenu() {
        if (this.actionSwapMenu != null) {
            this.actionSwapMenu.setEnabled(isSwapAccountPossible());
        }
    }

    private void updateBalance() {
        this.lastBalance = this.sourceAccount.getBalance();
    }

    private void onWalletUpdate() {
        updateBalance();
        validateAmount();
    }

    /**
     * Check if amount is within the minimum and maximum deposit limits and if is dust or if is more
     * money than currently in the wallet
     */
    private boolean isAmountWithinLimits(Value amount) {
        boolean isWithinLimits;
        if (amount.isDust()) {
            isWithinLimits = false;
        } else {
            isWithinLimits = true;
        }
        if (isWithinLimits && this.minimumDeposit != null && this.maximumDeposit != null && this.minimumDeposit.isOfType(amount) && this.maximumDeposit.isOfType(amount)) {
            isWithinLimits = amount.within(this.minimumDeposit, this.maximumDeposit);
        }
        if (!isWithinLimits || !Value.canCompare(this.lastBalance, amount)) {
            return isWithinLimits;
        }
        if (amount.compareTo(this.lastBalance) <= 0) {
            return true;
        }
        return false;
    }

    /**
     * Check if amount is smaller than the dust limit or if applicable, the minimum deposit.
     */
    private boolean isAmountTooSmall(Value amount) {
        return amount.compareTo(getLowestAmount(amount)) < 0;
    }

    /**
     * Get the lowest deposit or withdraw for the provided amount type
     */
    private Value getLowestAmount(Value amount) {
        Value min = amount.type.getMinNonDust();
        if (this.minimumDeposit == null) {
            return min;
        }
        if (this.minimumDeposit.isOfType(min)) {
            return Value.max(this.minimumDeposit, min);
        }
        if (this.lastRate == null || !this.lastRate.canConvert(amount.type, this.minimumDeposit.type)) {
            return min;
        }
        return Value.max(this.lastRate.convert(this.minimumDeposit), min);
    }

    /**
     * Check if the amount is valid
     */
    private boolean isAmountValid(Value amount) {
        boolean isValid = (amount == null || amount.isDust()) ? false : true;
        if (isValid && amount.isOfType(this.sourceAccount.getCoinType())) {
            return isAmountWithinLimits(amount);
        }
        return isValid;
    }

    /**
     * {@inheritDoc #validateAmount(boolean)}
     */
    private void validateAmount() {
        validateAmount(false);
    }

    /**
     * Validate amount and show errors if needed
     */
    private void validateAmount(boolean isTyping) {
        Value depositAmount = this.amountCalculatorLink.getPrimaryAmount();
        Value withdrawAmount = this.amountCalculatorLink.getSecondaryAmount();
        Value requestedAmount = this.amountCalculatorLink.getRequestedAmount();
        if (isAmountValid(depositAmount) && isAmountValid(withdrawAmount)) {
            this.sendAmount = requestedAmount;
            this.amountError.setVisibility(8);
            if (Value.canCompare(this.lastBalance, depositAmount) && this.lastBalance.compareTo(depositAmount) == 0) {
                this.amountWarning.setText(R.string.amount_warn_fees_apply);
                this.amountWarning.setVisibility(0);
            } else {
                this.amountWarning.setVisibility(8);
            }
        } else {
            boolean showErrors;
            this.amountWarning.setVisibility(8);
            this.sendAmount = null;
            if (shouldShowErrors(isTyping, depositAmount) || shouldShowErrors(isTyping, withdrawAmount)) {
                showErrors = true;
            } else {
                showErrors = false;
            }
            if (showErrors) {
                if (depositAmount == null || withdrawAmount == null) {
                    this.amountError.setText(R.string.amount_error);
                } else if (depositAmount.isNegative() || withdrawAmount.isNegative()) {
                    this.amountError.setText(R.string.amount_error_negative);
                } else if (isAmountWithinLimits(depositAmount) && isAmountWithinLimits(withdrawAmount)) {
                    this.amountError.setText(R.string.amount_error);
                } else {
                    String message = getString(R.string.error_generic);
                    if (isAmountTooSmall(depositAmount) || isAmountTooSmall(withdrawAmount)) {
                        Value minimumDeposit = getLowestAmount(depositAmount);
                        Value minimumWithdraw = getLowestAmount(withdrawAmount);
                        message = getString(R.string.trade_error_min_limit, minimumDeposit.toFriendlyString() + " (" + minimumWithdraw.toFriendlyString() + ")");
                    } else {
                        if (Value.canCompare(this.lastBalance, depositAmount) && depositAmount.compareTo(this.lastBalance) > 0) {
                            message = getString(R.string.amount_error_not_enough_money, this.lastBalance.toFriendlyString());
                        }
                        if (Value.canCompare(this.maximumDeposit, depositAmount) && depositAmount.compareTo(this.maximumDeposit) > 0) {
                            String maxDepositString = this.maximumDeposit.toFriendlyString();
                            if (this.lastRate != null && this.lastRate.canConvert(this.maximumDeposit.type, this.destinationType)) {
                                maxDepositString = maxDepositString + " (" + this.lastRate.convert(this.maximumDeposit).toFriendlyString() + ")";
                            }
                            message = getString(R.string.trade_error_max_limit, maxDepositString);
                        }
                    }
                    this.amountError.setText(message);
                }
                this.amountError.setVisibility(0);
            } else {
                this.amountError.setVisibility(8);
            }
        }
        updateNextButtonState();
    }

    private boolean isOutputsValid() {
        return true;
    }

    private boolean everythingValid() {
        return isOutputsValid() && isAmountValid(this.sendAmount);
    }

    private void updateNextButtonState() {
    }

    /**
     * Decide if should show errors in the UI.
     */
    private boolean shouldShowErrors(boolean isTyping, Value amountParsed) {
        if (Value.canCompare(amountParsed, this.lastBalance) && amountParsed.compareTo(this.lastBalance) >= 0) {
            return true;
        }
        if (isTyping) {
            return false;
        }
        if (this.amountCalculatorLink.isEmpty()) {
            return false;
        }
        if (amountParsed == null || !amountParsed.isZero()) {
            return true;
        }
        return false;
    }
}
