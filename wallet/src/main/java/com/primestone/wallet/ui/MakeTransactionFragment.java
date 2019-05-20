package com.primestone.wallet.ui;

import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.exceptions.NoSuchPocketException;
import com.coinomi.core.exchange.shapeshift.ShapeShift;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftAmountTx;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftMarketInfo;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftNormalTx;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftTime;
import com.coinomi.core.messages.TxMessage;
import com.coinomi.core.util.ExchangeRate;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.AbstractTransaction.AbstractOutput;
import com.coinomi.core.wallet.AbstractWallet;
import com.coinomi.core.wallet.SendRequest;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.core.wallet.WalletAccount.WalletAccountException;

import com.primestone.wallet.R;
import com.primestone.wallet.Configuration;
import com.primestone.wallet.ExchangeHistoryProvider;
import com.primestone.wallet.ExchangeHistoryProvider.ExchangeEntry;
import com.primestone.wallet.ExchangeRatesProvider;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.ui.Dialogs.ProgressDialogFragment;
import com.primestone.wallet.ui.widget.MDToast;
import com.primestone.wallet.ui.widget.SendOutputTanns;
import com.primestone.wallet.ui.widget.TransactionAmountVisualizer;
import com.primestone.wallet.util.Keyboard;
import com.primestone.wallet.util.WeakHandler;

import java.util.HashMap;
import mehdi.sakout.fancybuttons.FancyButton;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import static com.primestone.wallet.Constants.ARG_SEND_TO_ACCOUNT_ID;

public class MakeTransactionFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(MakeTransactionFragment.class);

    private static final int START_TRADE_TIMEOUT = 0;
    private static final int UPDATE_TRADE_TIMEOUT = 1;
    private static final int TRADE_EXPIRED = 2;
    private static final int STOP_TRADE_TIMEOUT = 3;

    private static final int SAFE_TIMEOUT_MARGIN_SEC = 60;

    // Loader IDs
    private static final int ID_RATE_LOADER = 0;

    private static final String TRANSACTION_BROADCAST = "transaction_broadcast";
    private static final String ERROR = "error";
    private static final String EXCHANGE_ENTRY = "exchange_entry";
    private static final String DEPOSIT_ADDRESS = "deposit_address";
    private static final String DEPOSIT_AMOUNT = "deposit_amount";
    private static final String WITHDRAW_ADDRESS = "withdraw_address";
    private static final String WITHDRAW_AMOUNT = "withdraw_amount";

    private static final String PREPARE_TRANSACTION_BUSY_DIALOG_TAG = "prepare_transaction_busy_dialog_tag";
    private static final String SIGNING_TRANSACTION_BUSY_DIALOG_TAG = "signing_transaction_busy_dialog_tag";

    private WalletApplication application;
    private Configuration config;
    private ContentResolver contentResolver;
    private CountDownTimer countDownTimer;
    private CreateTransactionTask createTransactionTask;
    boolean emptyWallet;
    @Nullable private Exception error;
    @Nullable private ExchangeEntry exchangeEntry;
    private Handler handler = new MyHandler(this);
    private Listener listener;
    private HashMap<String, ExchangeRate> localRates = new HashMap<>();
    @Nullable private String password;
    private final LoaderCallbacks<Cursor> rateLoaderCallbacks = new C11523();
    private SendRequest request;
    @Nullable private Value sendAmount;
    @Nullable AbstractAddress sendToAddress;
    boolean sendingToAccount;
    private SignAndBroadcastTask signAndBroadcastTask;
    @Nullable private AbstractWallet sourceAccount;
    private CoinType sourceType;
    @Nullable private AbstractAddress tradeDepositAddress;
    @Nullable private Value tradeDepositAmount;
    @Nullable private AbstractAddress tradeWithdrawAddress;
    @Nullable private Value tradeWithdrawAmount;
    private boolean transactionBroadcast = false;
    @Nullable private TxMessage txMessage;

    @Bind(R.id.transaction_info) TextView transactionInfo;
    @Bind(R.id.password) EditText passwordView;
    @Bind(R.id.transaction_amount_visualizer) TransactionAmountVisualizer txVisualizer;
    @Bind(R.id.transaction_trade_withdraw) SendOutputTanns tradeWithdrawSendOutput;

    class C11501 implements OnClickListener {
        C11501() {
        }

        public void onClick(View v) {
            new Builder(MakeTransactionFragment.this.getActivity()).setTitle(R.string.about_shapeshift_title).setMessage(R.string.about_shapeshift_message).setPositiveButton(R.string.button_ok, null).create().show();
        }
    }

    class C11523 implements LoaderCallbacks<Cursor> {
        C11523() {
        }

        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new ExchangeRateLoader(MakeTransactionFragment.this.getActivity(), MakeTransactionFragment.this.config, MakeTransactionFragment.this.config.getExchangeCurrencyCode());
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null && data.getCount() > 0) {
                HashMap<String, ExchangeRate> rates = new HashMap(data.getCount());
                data.moveToFirst();
                do {
                    ExchangeRatesProvider.ExchangeRate rate = ExchangeRatesProvider.getExchangeRate(data);
                    rates.put(rate.currencyCodeId, rate.rate);
                } while (data.moveToNext());
                MakeTransactionFragment.this.updateLocalRates(rates);
            }
        }

        public void onLoaderReset(Loader<Cursor> loader) {
        }
    }

    private class CreateTransactionTask extends AsyncTask<Void, Void, Void> {
        private CreateTransactionTask() {
        }

        protected void onPreExecute() {
            if (MakeTransactionFragment.this.isExchangeNeeded()) {
                ProgressDialogFragment.show(MakeTransactionFragment.this.getFragmentManager(), MakeTransactionFragment.this.getString(R.string.contacting_exchange), "prepare_transaction_busy_dialog_tag");
            }
        }

        protected Void doInBackground(Void... params) {
            try {
                if (isExchangeNeeded()) {

                    ShapeShift shapeShift = application.getShapeShift();
                    AbstractAddress refundAddress =
                            sourceAccount.getRefundAddress(config.isManualAddressManagement());

                    // If emptying wallet or the amount is the same type as the source account
                    if (isSendingFromSourceAccount()) {
                        ShapeShiftMarketInfo marketInfo = shapeShift.getMarketInfo(
                                sourceType, sendToAddress.getType());

                        // If no values set, make the call
                        if (tradeDepositAddress == null || tradeDepositAmount == null ||
                                tradeWithdrawAddress == null || tradeWithdrawAmount == null) {
                            ShapeShiftNormalTx normalTx =
                                    shapeShift.exchange(sendToAddress, refundAddress);
                            if (normalTx.isError) throw new Exception(normalTx.errorMessage);
                            tradeDepositAddress = normalTx.deposit;
                            tradeDepositAmount = sendAmount;
                            tradeWithdrawAddress = sendToAddress;
                        }

                        request = generateSendRequest(tradeDepositAddress, isEmptyWallet(),
                                tradeDepositAmount, txMessage);

                        tradeWithdrawAmount = marketInfo.rate.convert(
                                request.tx.getValue(sourceAccount).negate().subtract(request.tx.getFee()));
                    } else {
                        if (tradeDepositAddress == null || tradeDepositAmount == null ||
                                tradeWithdrawAddress == null || tradeWithdrawAmount == null) {
                            ShapeShiftAmountTx fixedAmountTx =
                                    shapeShift.exchangeForAmount(sendAmount, sendToAddress, refundAddress);
                            if (fixedAmountTx.isError) throw new Exception(fixedAmountTx.errorMessage);
                            tradeDepositAddress = fixedAmountTx.deposit;
                            tradeDepositAmount = fixedAmountTx.depositAmount;
                            tradeWithdrawAddress = fixedAmountTx.withdrawal;
                            tradeWithdrawAmount = fixedAmountTx.withdrawalAmount;
                        }

                        ShapeShiftTime time = getTimeLeftSync(shapeShift, tradeDepositAddress);
                        if (time != null && !time.isError) {
                            int secondsLeft = time.secondsRemaining - SAFE_TIMEOUT_MARGIN_SEC;
                            handler.sendMessage(handler.obtainMessage(
                                    START_TRADE_TIMEOUT, secondsLeft));
                        } else {
                            throw new Exception(time == null ? "Error getting trade expiration time" : time.errorMessage);
                        }
                        request = generateSendRequest(tradeDepositAddress, false,
                                tradeDepositAmount, txMessage);
                    }
                } else {
                    request = generateSendRequest(sendToAddress, isEmptyWallet(),
                            sendAmount, txMessage);
                }
            } catch (Exception e) {
                error = e;
            }
            return null;
        }

        protected void onPostExecute(Void aVoid) {
            if (!Dialogs.dismissAllowingStateLoss(MakeTransactionFragment.this.getFragmentManager(), "prepare_transaction_busy_dialog_tag")) {
                if (MakeTransactionFragment.this.error != null && MakeTransactionFragment.this.listener != null) {
                    MakeTransactionFragment.this.listener.onSignResult(MakeTransactionFragment.this.error, null);
                } else if (MakeTransactionFragment.this.error == null) {
                    MakeTransactionFragment.this.showTransaction();
                } else {
                    MakeTransactionFragment.log.warn("Error occurred while creating transaction", MakeTransactionFragment.this.error);
                }
            }
        }
    }

    public interface Listener {
        void onSignResult(@Nullable Exception exception, @Nullable ExchangeEntry exchangeEntry);
    }

    // fragment handler
    private static class MyHandler extends WeakHandler<MakeTransactionFragment> {
        public MyHandler(MakeTransactionFragment referencingObject) {
            super(referencingObject);
        }

        @Override
        protected void weakHandleMessage(MakeTransactionFragment ref, Message msg) {
            switch (msg.what) {
                case 0:
                    ref.onStartTradeCountDown((int) msg.obj);
                    return;
                case 1:
                    ref.onUpdateTradeCountDown((int) msg.obj);
                    return;
                case 2:
                    ref.onTradeExpired();
                    return;
                case 3:
                    ref.onStopTradeCountDown();
                    return;
                default:
                    return;
            }
        }
    }

    private class SignAndBroadcastTask extends AsyncTask<Void, Void, Exception> {

        class C11531 implements DialogInterface.OnClickListener {
            C11531() {
            }

            public void onClick(DialogInterface dialog, int which) {
                MakeTransactionFragment.this.password = null;
                MakeTransactionFragment.this.passwordView.setText(null);
                MakeTransactionFragment.this.signAndBroadcastTask = null;
                MakeTransactionFragment.this.error = null;
            }
        }

        private SignAndBroadcastTask() {
        }

        @Override
        protected void onPreExecute() {
            ProgressDialogFragment.show(MakeTransactionFragment.this.getFragmentManager(), MakeTransactionFragment.this.getString(R.string.preparing_transaction), "signing_transaction_busy_dialog_tag");
        }

        @Override
        protected Exception doInBackground(Void... params) {
            Wallet wallet = application.getWallet();
            if (wallet == null) return new NoSuchPocketException("No wallet found.");

            try {
                if (sourceAccount != null) {
                    if (wallet.isEncrypted()) {
                        KeyCrypter crypter = (KeyCrypter) Preconditions.checkNotNull(wallet.getKeyCrypter());
                        request.aesKey = crypter.deriveKey(password);
                    }
                    request.signTransaction = true;
                    sourceAccount.completeAndSignTx(request);
                }

                if (error != null) throw error;

                if (sourceAccount != null) {
                    if (!sourceAccount.broadcastTxSync(request.tx)) {
                        throw new Exception("Error broadcasting transaction: " + request.tx.getHashAsString());
                    }
                } else if (!((WalletAccount) wallet.getAccounts(((AbstractOutput) request.tx.getSentTo().get(0)).getAddress()).get(0)).broadcastTxSync(request.tx)) {
                    throw new Exception("Error broadcasting transaction: " + request.tx.getHashAsString());
                }
                transactionBroadcast = true;
                if (!(!isExchangeNeeded() || tradeDepositAddress == null || tradeDepositAmount == null)) {
                    exchangeEntry = new ExchangeEntry(tradeDepositAddress, tradeDepositAmount,
                            request.tx.getHashAsString());
                    contentResolver.insert(ExchangeHistoryProvider.contentUri(application.getPackageName(),
                            tradeDepositAddress), exchangeEntry.getContentValues());
                }
                handler.sendEmptyMessage(3);
                return error;
            } catch (Exception e) {
                error = e;
            }

            return error;
        }

        protected void onPostExecute(final Exception e) {
            if (!Dialogs.dismissAllowingStateLoss(MakeTransactionFragment.this.getFragmentManager(), "signing_transaction_busy_dialog_tag")) {
                if (e instanceof KeyCrypterException) {
                    DialogBuilder.warn(MakeTransactionFragment.this.getActivity(), R.string.unlocking_wallet_error_title).setMessage((int) R.string.unlocking_wallet_error_detail).setNegativeButton((int) R.string.button_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            MakeTransactionFragment.this.listener.onSignResult(e, MakeTransactionFragment.this.exchangeEntry);
                        }
                    }).setPositiveButton((int) R.string.button_retry, new C11531()).create().show();
                } else if (MakeTransactionFragment.this.listener != null) {
                    MakeTransactionFragment.this.listener.onSignResult(e, MakeTransactionFragment.this.exchangeEntry);
                }
            }
        }
    }

    public static MakeTransactionFragment newInstance(Bundle args) {
        MakeTransactionFragment fragment = new MakeTransactionFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        signAndBroadcastTask = null;

        setRetainInstance(true);
        Bundle args = getArguments();
        Preconditions.checkNotNull(args, "Must provide arguments");

        try {
            if (args.containsKey("send_request")) {
                request = (SendRequest) Preconditions.checkNotNull(args.getSerializable("send_request"));
                Preconditions.checkState(this.request.isCompleted(), "Only completed requests are currently supported.");
                Preconditions.checkState(this.request.tx.getSentTo().size() == 1, "Only one output is currently supported");
                sendToAddress = ((AbstractOutput) this.request.tx.getSentTo().get(0)).getAddress();
                sourceType = request.type;
                return;
            }

            sourceAccount = (AbstractWallet) Preconditions.checkNotNull(this.application.getAccount(args.getString("account_id")));
            application.maybeConnectAccount(sourceAccount);
            sourceType = this.sourceAccount.getCoinType();
            emptyWallet = args.getBoolean("empty_wallet", false);
            sendAmount = (Value) args.getSerializable("send_value");

            if (!emptyWallet || sendAmount == null) {
                if (args.containsKey("send_to_account_id")) {
                    String toAccountId = args.getString(ARG_SEND_TO_ACCOUNT_ID);
                    AbstractWallet toAccount = (AbstractWallet) Preconditions.checkNotNull(application.getAccount(toAccountId));
                    //sendToAddress = ((AbstractWallet) Preconditions.checkNotNull(this.application.getAccount(args.getString("send_to_account_id")))).getReceiveAddress(this.config.isManualAddressManagement());
                    sendToAddress = toAccount.getReceiveAddress(config.isManualAddressManagement());
                    sendingToAccount = true;
                } else {
                    sendToAddress = (AbstractAddress) Preconditions.checkNotNull(args.getSerializable("send_to_address"));
                    sendingToAccount = false;
                }
                txMessage = (TxMessage) args.getSerializable("tx_message");
                if (savedState != null) {
                    error = (Exception) savedState.getSerializable("error");
                    transactionBroadcast = savedState.getBoolean("transaction_broadcast");
                    exchangeEntry = (ExchangeEntry) savedState.getSerializable("exchange_entry");
                    tradeDepositAddress = (AbstractAddress) savedState.getSerializable("deposit_address");
                    tradeDepositAmount = (Value) savedState.getSerializable("deposit_amount");
                    tradeWithdrawAddress = (AbstractAddress) savedState.getSerializable("withdraw_address");
                    tradeWithdrawAmount = (Value) savedState.getSerializable("withdraw_amount");
                }
                maybeStartCreateTransaction();
                for (ExchangeRatesProvider.ExchangeRate rate : ExchangeRatesProvider.getRates(getActivity(), this.config.getExchangeCurrencyCode()).values()) {
                    localRates.put(rate.currencyCodeId, rate.rate);
                }
                return;
            }
            throw new IllegalArgumentException("Cannot set 'empty wallet' and 'send amount' at the same time");
        } catch (Exception e) {
            error = e;
            if (listener != null) {
                listener.onSignResult(e, null);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int i = 0;
        View view = inflater.inflate(R.layout.fragment_make_transaction, container, false);
        ButterKnife.bind((Object) this, view);
        setAllTypefaceThin(view);
        if (error == null) {
            transactionInfo.setVisibility(View.GONE);
            TextView passwordLabelView = (TextView) view.findViewById(R.id.enter_password_label);
            if (sourceAccount == null || !sourceAccount.isEncrypted()) {
                passwordView.requestFocus();
                passwordView.setVisibility(View.VISIBLE);
                passwordLabelView.setVisibility(View.VISIBLE);
            } else {
                passwordView.requestFocus();
                passwordView.setVisibility(View.VISIBLE);
                passwordLabelView.setVisibility(View.VISIBLE);
            }
            this.tradeWithdrawSendOutput.setVisibility(View.GONE);
            showTransaction();
            //TextView poweredByShapeShift = (TextView) view.findViewById(R.id.powered_by_shapeshift);
            //poweredByShapeShift.setOnClickListener(new C11501());
            if (!isExchangeNeeded()) {
                i = 8;
            }
            //poweredByShapeShift.setVisibility(i);
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @OnClick(R.id.button_confirm)
    void onConfirmClick(FancyButton fancyButton) {
        if (this.passwordView.isShown()) {
            Keyboard.hideKeyboard(getActivity());
            this.password = this.passwordView.getText().toString();
            if (this.password.equals(this.config.GetPassword())) {
                this.passwordView.setError(null);
                maybeStartSignAndBroadcast();
                return;
            }
            this.passwordView.setError(getActivity().getString(R.string.passwords_mismatch));
        }
    }

    private void showTransaction() {
        if (request != null && txVisualizer != null) {
            txVisualizer.setTransaction(sourceAccount, request.tx);
            if (tradeWithdrawAmount != null && tradeWithdrawAddress != null) {
                tradeWithdrawSendOutput.setVisibility(View.VISIBLE);
                if (sendingToAccount) {
                    tradeWithdrawSendOutput.setSending(false);
                } else {
                    tradeWithdrawSendOutput.setSending(true);
                    tradeWithdrawSendOutput.setLabelAndAddress(tradeWithdrawAddress);
                }

                tradeWithdrawSendOutput.setAmount(GenericUtils.formatValue(tradeWithdrawAmount));
                tradeWithdrawSendOutput.setSymbol(tradeWithdrawAmount.type.getSymbol());
                ((SendOutputTanns) txVisualizer.getOutputs().get(0)).setSendLabel(getString(R.string.trade));
                txVisualizer.hideAddresses();
            }
        }
    }

    boolean isExchangeNeeded() {
        return !sourceType.equals(sendToAddress.getType());
    }

    private void maybeStartCreateTransaction() {
        if (createTransactionTask == null && !transactionBroadcast && error == null) {
            createTransactionTask = new CreateTransactionTask();
            createTransactionTask.execute(new Void[0]);
        } else if (createTransactionTask != null && createTransactionTask.getStatus() == Status.FINISHED) {
            Dialogs.dismissAllowingStateLoss(getFragmentManager(), "prepare_transaction_busy_dialog_tag");
        }
    }

    private SendRequest generateSendRequest(AbstractAddress sendTo, boolean emptyWallet,
                                            @Nullable Value amount, @Nullable TxMessage txMessage)
            throws WalletAccountException {

        SendRequest sendRequest;
        if (emptyWallet) {
            sendRequest = sourceAccount.getEmptyWalletRequest(sendTo);
        } else {
            sendRequest = sourceAccount.getSendToRequest(sendTo, (Value) Preconditions.checkNotNull(amount));
        }
        sendRequest.txMessage = txMessage;
        sendRequest.signTransaction = false;
        sourceAccount.completeTransaction(sendRequest);

        return sendRequest;
    }

    private boolean isSendingFromSourceAccount() {
        return isEmptyWallet() || (sendAmount != null && sourceType.equals(sendAmount.type));
    }

    private boolean isEmptyWallet() {
        return emptyWallet && sendAmount == null;
    }

    private void maybeStartSignAndBroadcast() {
        if (signAndBroadcastTask == null && !transactionBroadcast && request != null && error == null) {
            signAndBroadcastTask = new SignAndBroadcastTask();
            signAndBroadcastTask.execute();
        } else if (transactionBroadcast) {
            Dialogs.dismissAllowingStateLoss(getFragmentManager(), "signing_transaction_busy_dialog_tag");
            MDToast.makeText(getActivity(), getActivity().getString(R.string.tx_already_broadcast), MDToast.LENGTH_SHORT, 0).show();
            if (listener != null) {
                listener.onSignResult(error, exchangeEntry);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("transaction_broadcast", transactionBroadcast);
        outState.putSerializable("error", error);
        if (isExchangeNeeded()) {
            outState.putSerializable("exchange_entry", exchangeEntry);
            outState.putSerializable("deposit_address", tradeDepositAddress);
            outState.putSerializable("deposit_amount", tradeDepositAmount);
            outState.putSerializable("withdraw_address", tradeWithdrawAddress);
            outState.putSerializable("withdraw_amount", tradeWithdrawAmount);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (Listener) context;
            contentResolver = context.getContentResolver();
            application = (WalletApplication) context.getApplicationContext();
            config = application.getConfiguration();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + Listener.class);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, rateLoaderCallbacks);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getLoaderManager().destroyLoader(0);
        listener = null;
        onStopTradeCountDown();
    }

    void onStartTradeCountDown(int secondsLeft) {
        if (countDownTimer == null) {
            countDownTimer = new CountDownTimer((long) (secondsLeft * 1000), 1000) {
                public void onTick(long millisUntilFinished) {
                    handler.sendMessage(handler.obtainMessage(1, Integer.valueOf((int) (millisUntilFinished / 1000))));
                }

                public void onFinish() {
                    handler.sendEmptyMessage(2);
                }
            };
            this.countDownTimer.start();
        }
    }

    void onStopTradeCountDown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
            handler.removeMessages(0);  // START_TRADE_TIMEOUT
            handler.removeMessages(1);  // UPDATE_TRADE_TIMEOUT
            handler.removeMessages(2);  // TRADE_EXPIRED
        }
    }

    private void onTradeExpired() {
        if (!transactionBroadcast) {
            if (transactionInfo.getVisibility() != View.VISIBLE) {
                transactionInfo.setVisibility(View.VISIBLE);
            }
            String errorString = getString(R.string.trade_expired);
            transactionInfo.setText(errorString);
            if (listener != null) {
                error = new Exception(errorString);
                listener.onSignResult(error, null);
            }
        }
    }

    private void onUpdateTradeCountDown(int secondsRemaining) {
        String timeLeft;
        if (transactionInfo.getVisibility() != View.VISIBLE) {
            transactionInfo.setVisibility(View.VISIBLE);
        }
        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;
        Resources res = getResources();
        if (minutes > 0) {
            Object[] objArr = new Object[1];
            objArr[0] = String.format("%d:%02d", new Object[]{Integer.valueOf(minutes), Integer.valueOf(seconds)});
            timeLeft = res.getQuantityString(R.plurals.tx_confirm_timer_minute, minutes, objArr);
        } else {
            timeLeft = res.getQuantityString(R.plurals.tx_confirm_timer_second, seconds, new Object[]{Integer.valueOf(seconds)});
        }
        transactionInfo.setText(getString(R.string.tx_confirm_timer_message, timeLeft));
    }

    private static ShapeShiftTime getTimeLeftSync(ShapeShift shapeShift, AbstractAddress address) {
        int tries = 1;
        while (tries <= 3) {
            try {
                log.info("Getting time left for: {}", (Object) address);
                return shapeShift.getTime(address);
            } catch (Exception e) {
                log.info("Will retry: {}", e.getMessage());
                try {
                    Thread.sleep((long) (tries * 1000));
                } catch (InterruptedException e2) {
                }
                tries++;
            }
        }
        return null;
    }

    private void updateLocalRates() {
        if (localRates != null) {
            if (txVisualizer != null && localRates.containsKey(sourceType.getSymbol())) {
                txVisualizer.setExchangeRate((ExchangeRate) localRates.get(sourceType.getSymbol()));
            }
            if (tradeWithdrawAmount != null && localRates.containsKey(tradeWithdrawAmount.type.getSymbol())) {
                Value fiatAmount = ((ExchangeRate) localRates.get(tradeWithdrawAmount.type.getSymbol())).convert(this.tradeWithdrawAmount);
                tradeWithdrawSendOutput.setAmountLocal(GenericUtils.formatFiatValue(fiatAmount));
                tradeWithdrawSendOutput.setSymbolLocal(fiatAmount.type.getSymbol());
            }
        }
    }

    private void updateLocalRates(HashMap<String, ExchangeRate> rates) {
        localRates = rates;
        updateLocalRates();
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
