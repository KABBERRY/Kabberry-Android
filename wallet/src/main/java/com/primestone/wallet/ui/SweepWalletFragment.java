package com.primestone.wallet.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import butterknife.OnTextChanged.Callback;

import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.network.ConnectivityHelper;
import com.coinomi.core.network.ServerClients;
import com.coinomi.core.wallet.BitWalletSingleKey;
import com.coinomi.core.wallet.SendRequest;
import com.coinomi.core.wallet.SerializedKey;
import com.coinomi.core.wallet.SerializedKey.BadPassphraseException;
import com.coinomi.core.wallet.SerializedKey.KeyFormatException;
import com.coinomi.core.wallet.SerializedKey.TypedKey;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.core.wallet.WalletAccount.WalletAccountException;
import com.coinomi.core.wallet.families.bitcoin.BitTransaction;

import com.primestone.wallet.R;
import com.primestone.wallet.Constants;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.ui.widget.MDToast;
import com.primestone.wallet.util.Keyboard;
import com.primestone.wallet.util.UiUtils;
import com.primestone.wallet.util.WeakHandler;

import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class SweepWalletFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(SweepWalletFragment.class);

    enum Error {NONE, BAD_FORMAT, BAD_COIN_TYPE, BAD_PASSWORD, ZERO_COINS, NO_CONNECTION, GENERIC_ERROR, LOADING}
    enum TxStatus {INITIAL, DECODING, LOADING, SIGNING}

    static SweepWalletTask sweepWalletTask;

    private final Handler handler = new MyHandler(this);
    private Listener listener;
    private ServerClients serverClients;
    private WalletAccount account;
    private SerializedKey serializedKey;
    private Error error = Error.NONE;
    private TxStatus status = TxStatus.INITIAL;

    @Bind(R.id.private_key_input) View privateKeyInputView;
    @Bind(R.id.sweep_wallet_key) EditText privateKeyText;
    @Bind(R.id.passwordView) View passwordView;
    @Bind(R.id.sweep_error) TextView errorMessage;
    @Bind(R.id.passwordInput) EditText password;
    @Bind(R.id.sweep_loading) View sweepLoadingView;
    @Bind(R.id.sweeping_status) TextView sweepStatus;
    @Bind(R.id.button_next) ImageView nextButton;   // TextView

    public SweepWalletFragment() { }

    public static SweepWalletFragment newInstance() {
        clearTasks();
        return new SweepWalletFragment();
    }

    static void clearTasks() {
        if (sweepWalletTask != null) {
            sweepWalletTask.cancel(true);
            sweepWalletTask = null;
        }
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        Preconditions.checkNotNull(getArguments(), "Must provide arguments with an account id.");

        account = ((WalletApplication) getActivity().getApplication()).getAccount(getArguments().getString("account_id"));
        if (account == null) {
            MDToast.makeText(getActivity(), getActivity().getString(R.string.no_such_pocket_error), MDToast.LENGTH_LONG, 3);
            getActivity().finish();
        }

        if (savedState != null) {
            error = (Error) savedState.getSerializable("error");
            status = (TxStatus) savedState.getSerializable("status");
        }

        if (sweepWalletTask != null) {
            switch (sweepWalletTask.getStatus()) {
                case FINISHED:     // FINISHED
                    sweepWalletTask.onPostExecute(null);
                    return;
                case RUNNING:     // RUNNING
                case PENDING:     // PENDING
                    sweepWalletTask.handler = handler;
                    return;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sweep, container, false);
        ButterKnife.bind((Object) this, view);

        setAllTypefaceThin(view);
        if (getArguments().containsKey("private_key")) {
            privateKeyText.setText(getArguments().getString("private_key"));
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("status", status);
        outState.putSerializable("error", error);
    }

    @Override
    public void onResume() {
        super.onResume();
        validatePrivateKey();
        updateView();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        try {
            listener = (Listener) context;
            serverClients = new ServerClients(Constants.DEFAULT_COINS_SERVERS, new ConnectivityHelper() {
                ConnectivityManager connManager = ((ConnectivityManager) context.getSystemService("connectivity"));

                @Override
                public boolean isConnected() {
                    NetworkInfo activeInfo = connManager.getActiveNetworkInfo();
                    return activeInfo != null && activeInfo.isConnected();
                }
            });
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + Listener.class);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @OnClick(R.id.scan_qr_code)
    void handleScan() {
        startActivityForResult(new Intent(getActivity(), ScanActivity.class), 0);
    }

    @OnClick(R.id.button_next)
    void verifyKeyAndProceed() {
        Keyboard.hideKeyboard(getActivity());
        if (validatePrivateKey()) {
            maybeStartSweepTask();
        }
    }

    @OnFocusChange(R.id.sweep_wallet_key)
    void onPrivateKeyInputFocusChange(final boolean hasFocus) {
        if (!hasFocus) validatePrivateKey();
    }

    @OnTextChanged(callback = Callback.AFTER_TEXT_CHANGED, value = R.id.sweep_wallet_key)
    void onPrivateKeyInputTextChange() {
        validatePrivateKey(true);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (requestCode == 0 && resultCode == -1) {
            privateKeyText.setText(intent.getStringExtra("result"));
            validatePrivateKey();
        }
    }

    private void updateView() {
        updateErrorView();
        updateStatusView();
        nextButton.setEnabled(status == TxStatus.INITIAL);
    }

    private void updateErrorView() {
        switch (error) {
            case NONE:
                UiUtils.setGone(errorMessage);
                break;
            case BAD_FORMAT:
                errorMessage.setText(R.string.sweep_wallet_bad_format);
                UiUtils.setVisible(errorMessage);
                break;
            case BAD_COIN_TYPE:
                errorMessage.setText(R.string.sweep_wallet_bad_coin_type);
                UiUtils.setVisible(errorMessage);
                break;
            case BAD_PASSWORD:
                errorMessage.setText(R.string.sweep_wallet_bad_password);
                UiUtils.setVisible(errorMessage);
                break;
            case ZERO_COINS:
                errorMessage.setText(R.string.sweep_wallet_zero_coins);
                UiUtils.setVisible(errorMessage);
                break;
            case NO_CONNECTION:
                errorMessage.setText(R.string.disconnected_label);
                UiUtils.setVisible(errorMessage);
                break;
            case GENERIC_ERROR:
                errorMessage.setText(R.string.error_generic);
                UiUtils.setVisible(errorMessage);
                break;
            case LOADING:
                errorMessage.setText(R.string.loading);
                UiUtils.setVisible(errorMessage);
                break;
        }
    }

    private void updateStatusView() {
        if (error != Error.NONE) {
            UiUtils.setGone(sweepLoadingView);
            UiUtils.setVisible(privateKeyInputView);
            return;
        }

        switch (status) {
            case DECODING:
                sweepStatus.setText(R.string.sweep_wallet_key_decoding);
                break;
            case LOADING:
                sweepStatus.setText(R.string.sweep_wallet_key_loading);
                break;
            case SIGNING:
                sweepStatus.setText(R.string.sweep_wallet_key_signing);
                break;
        }

        if (status == TxStatus.INITIAL) {
            UiUtils.setGone(sweepLoadingView);
            UiUtils.setVisible(privateKeyInputView);

            if (serializedKey != null && serializedKey.isEncrypted()) {
                passwordView.setVisibility(View.VISIBLE);
            } else {
                passwordView.setVisibility(View.GONE);
                password.setText("");
            }
        } else {
            UiUtils.setVisible(sweepLoadingView);
            UiUtils.setGone(privateKeyInputView);
        }
    }

    private void onTransactionPrepared(SendRequest request) {
        sweepWalletTask = null;
        error = Error.NONE;
        status = TxStatus.INITIAL;

        if (listener != null) {
            listener.onSendTransaction((SendRequest) Preconditions.checkNotNull(request));
        }
    }

    private void onStatusUpdate(TxStatus status) {
        this.status = status;
        updateStatusView();
    }

    private void onError(Error error) {
        sweepWalletTask = null;
        this.error = error;
        status = TxStatus.INITIAL;
        updateView();
    }

    private void maybeStartSweepTask() {
        if (sweepWalletTask == null) {
            sweepWalletTask = new SweepWalletTask(handler, serverClients, account, serializedKey,
                    password.getText().toString());
            sweepWalletTask.execute();
            error = Error.NONE;
            status = TxStatus.DECODING;
            updateView();
        }
    }

    private boolean validatePrivateKey() {
        return validatePrivateKey(false);
    }

    private boolean validatePrivateKey(boolean isTyping) {
        if (privateKeyText == null) {
            return false;
        }

        String privateKey = privateKeyText.getText().toString().trim();

        if (privateKey.isEmpty()) return false;

        try {
            serializedKey = new SerializedKey(privateKey);
            error = Error.NONE;
        } catch (SerializedKey.KeyFormatException e) {
            serializedKey = null;
            if (isTyping) {
                error = Error.NONE;
            } else {
                log.info("Invalid private key: {}", e.getMessage());
                error = Error.BAD_FORMAT;
            }
        }

        updateView();

        return serializedKey != null;
    }

    static class SweepWalletTask extends AsyncTask<Void, TxStatus, Void> {
        Handler handler;
        Error error = Error.NONE;
        SendRequest request = null;
        final WalletAccount sendToAccount;
        final CoinType type;
        final ServerClients serverClients;
        final SerializedKey key;
        @Nullable final String keyPassword;

        public SweepWalletTask(Handler handler, ServerClients serverClients,
                               WalletAccount sendToAccount, SerializedKey key,
                               @Nullable String keyPassword) {
            this.handler = handler;
            this.serverClients = serverClients;
            this.key = key;
            this.sendToAccount = sendToAccount;
            this.type = sendToAccount.getCoinType();
            this.keyPassword = keyPassword;
        }

        protected Void doInBackground(Void... params) {
            startSweeping();
            serverClients.stopAllAsync();
            return null;
        }

        private void startSweeping() {
            /*
            SweepWalletFragment.log.info("Starting sweep wallet task. Decoding private key...");
            this.publishProgress(TxStatus.DECODING);
            SerializedKey.TypedKey rawKey;

            try {
                if (key.isEncrypted()) {
                    rawKey = key.getKey(keyPassword);
                } else {
                    rawKey = key.getKey();
                }

                if (rawKey.possibleType.contains(type)) {
                    log.info("Creating temporary wallet");
                    publishProgress(new TxStatus[]{TxStatus.LOADING});
                    BitWalletSingleKey sweepWallet = new BitWalletSingleKey(this.type, rawKey.key);
                    this.serverClients.startAsync(sweepWallet);
                    int maxWaitMs = 15000;
                    SweepWalletFragment.log.info("Waiting wallet to connect...");
                    while (!sweepWallet.isConnected() && maxWaitMs > 0) {
                        try {
                            Thread.sleep(100);
                            maxWaitMs -= 100;
                        } catch (InterruptedException e) {
                            SweepWalletFragment.log.info("Stopping wallet loading task...");
                            return;
                        }
                    }
                    if (!sweepWallet.isConnected()) {
                        this.error = Error.NO_CONNECTION;
                        return;
                    } else if (sweepWallet.isLoading()) {
                        SweepWalletFragment.log.info("Waiting wallet to load...");
                        while (sweepWallet.isLoading()) {
                            try {
                                Thread.sleep(100);
                                this.error = Error.LOADING;
                            } catch (InterruptedException e2) {
                                SweepWalletFragment.log.info("Stopping wallet loading task...");
                                return;
                            }
                        }
                        Object balance = sweepWallet.getBalance(true);
                        if (balance.isPositive()) {
                            SweepWalletFragment.log.info("Wallet balance is {}", balance);
                            publishProgress(new TxStatus[]{TxStatus.SIGNING});
                            try {
                                this.request = sweepWallet.getEmptyWalletRequest(this.sendToAccount.getReceiveAddress());
                                this.request.useUnsafeOutputs = true;
                                sweepWallet.completeAndSignTx(this.request);
                                for (TransactionInput txi : this.request.tx.getInputs()) {
                                    TransactionOutPoint outPoint = txi.getOutpoint();
                                    txi.connect(((BitTransaction) Preconditions.checkNotNull(sweepWallet.getTransaction(outPoint.getHash()))).getOutput((int) outPoint.getIndex()));
                                }
                                return;
                            } catch (WalletAccountException e3) {
                                SweepWalletFragment.log.info("Could not create transaction: {}", e3.getMessage());
                                this.error = Error.GENERIC_ERROR;
                                return;
                            }
                        }
                        SweepWalletFragment.log.info("Wallet is empty");
                        this.error = Error.ZERO_COINS;
                        return;
                    } else {
                        this.error = Error.LOADING;
                        return;
                    }
                }
                SweepWalletFragment.log.info("Incorrect coin type");
                this.error = Error.BAD_COIN_TYPE;
            } catch (BadPassphraseException e4) {
                SweepWalletFragment.log.info("Could not get key due to bad passphrase");
                this.error = Error.BAD_PASSWORD;
            }
            */
            log.info("Starting sweep wallet task. Decoding private key...");
            this.publishProgress(TxStatus.DECODING);
            SerializedKey.TypedKey rawKey;
            try {
                if (key.isEncrypted()) {
                    rawKey = key.getKey(keyPassword);
                } else {
                    rawKey = key.getKey();
                }
            } catch (SerializedKey.BadPassphraseException e) {
                log.info("Could not get key due to bad passphrase");
                error = Error.BAD_PASSWORD;
                return;
            }

            if (!rawKey.possibleType.contains(type)) {
                log.info("Incorrect coin type");
                error = Error.BAD_COIN_TYPE;
                return;
            }

            log.info("Creating temporary wallet");
            this.publishProgress(TxStatus.LOADING);
            BitWalletSingleKey sweepWallet = new BitWalletSingleKey(type, rawKey.key);
            serverClients.startAsync(sweepWallet);

            int maxWaitMs = Constants.NETWORK_TIMEOUT_MS;
            log.info("Waiting wallet to connect...");
            while(!sweepWallet.isConnected() && maxWaitMs > 0) {
                try {
                    Thread.sleep(100);
                    maxWaitMs -= 100;
                } catch (InterruptedException e) {
                    log.info("Stopping wallet loading task...");
                    return;
                }
            }
            if (!sweepWallet.isConnected()) {
                error = Error.NO_CONNECTION;
                return;
            }

            log.info("Waiting wallet to load...");
            while(sweepWallet.isLoading()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.info("Stopping wallet loading task...");
                    return;
                }
            }

            Value balance = sweepWallet.getBalance(true);
            if (balance.isPositive()) {
                log.info("Wallet balance is {}", balance);
                this.publishProgress(TxStatus.SIGNING);
                try {
                    request = sweepWallet
                            .getEmptyWalletRequest(sendToAccount.getReceiveAddress());
                    request.useUnsafeOutputs = true;
                    sweepWallet.completeAndSignTx(request);
                    // Connect inputs so we can show the fees in the next screen
                    BitTransaction tx = (BitTransaction) request.tx;
                    for (TransactionInput txi : tx.getInputs()) {
                        TransactionOutPoint outPoint = txi.getOutpoint();
                        BitTransaction connectedTx =
                                Preconditions.checkNotNull(sweepWallet.getTransaction(outPoint.getHash()));
                        txi.connect(connectedTx.getOutput((int) outPoint.getIndex()));
                    }
                } catch (WalletAccount.WalletAccountException e) {
                    log.info("Could not create transaction: {}", e.getMessage());
                    error = Error.GENERIC_ERROR;
                }
            } else {
                log.info("Wallet is empty");
                error = Error.ZERO_COINS;
            }
        }

        @Override
        protected void onProgressUpdate(TxStatus... values) {
            //handler.sendMessage(handler.obtainMessage(0, values[0]));
            handler.sendMessage(handler.obtainMessage(MyHandler.TX_STATUS_UPDATE, values[0]));
        }

        protected void onPostExecute(Void param) {
            if (request != null) {
                //handler.sendMessage(handler.obtainMessage(1, request));
                handler.sendMessage(handler.obtainMessage(MyHandler.TX_PREPARATION_FINISHED, request));
            } else if (error != Error.NONE) {
                //handler.sendMessage(handler.obtainMessage(2, error));
                handler.sendMessage(handler.obtainMessage(MyHandler.TX_PREPARATION_ERROR, error));
            }
        }
    }

    private static class MyHandler extends WeakHandler<SweepWalletFragment> {
        static final int TX_STATUS_UPDATE = 0;
        static final int TX_PREPARATION_FINISHED = 1;
        static final int TX_PREPARATION_ERROR = 2;

        public MyHandler(SweepWalletFragment ref) {
            super(ref);
        }

        @Override
        protected void weakHandleMessage(SweepWalletFragment ref, Message msg) {
            switch (msg.what) {
                case TX_STATUS_UPDATE:
                    ref.onStatusUpdate((TxStatus) msg.obj);
                    break;
                case TX_PREPARATION_FINISHED:
                    ref.onTransactionPrepared((SendRequest) msg.obj);
                    break;
                case TX_PREPARATION_ERROR:
                    ref.onError((Error) msg.obj);
                    break;
            }
        }
    }

    public interface Listener {
        void onSendTransaction(SendRequest sendRequest);
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
