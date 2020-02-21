package com.kabberry.wallet.ui;

import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.exchange.shapeshift.ShapeShift;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftException;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftTxStatus;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.WalletAccount;

import com.kabberry.wallet.R;
import com.kabberry.wallet.Constants;
import com.kabberry.wallet.ExchangeHistoryProvider;
import com.kabberry.wallet.ExchangeHistoryProvider.ExchangeEntry;
import com.kabberry.wallet.WalletApplication;
import com.kabberry.wallet.util.Fonts;
import com.kabberry.wallet.util.Fonts.Font;
import com.kabberry.wallet.util.UiUtils;
import com.kabberry.wallet.util.WeakHandler;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradeStatusFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(TradeStatusFragment.class);

    private static final int UPDATE_SHAPESHIFT_STATUS = 0;
    private static final int UPDATE_STATUS = 1;
    private static final int ERROR_MESSAGE = 2;

    private static final int ID_STATUS_LOADER = 0;

    private static final long POLLING_MS = 3000;

    private static final String ARG_SHOW_EXIT_BUTTON = "show_exit_button";

    private MenuItem actionDeleteMenu;
    private WalletApplication application;
    private ContentResolver contentResolver;
    private TextView depositIcon;
    private ProgressBar depositProgress;
    private TextView depositText;
    private Button emailReceipt;
    private TextView errorIcon;
    private TextView errorText;
    private TextView exchangeIcon;
    private TextView exchangeInfo;
    private ProgressBar exchangeProgress;
    private ExchangeEntry exchangeStatus;
    private TextView exchangeText;
    private final Handler handler = new MyHandler(this);
    private Listener listener;
    private LoaderManager loaderManager;
    private StatusPollTask pollTask;
    private boolean showExitButton;
    private final LoaderCallbacks<Cursor> statusLoaderCallbacks = new C12745();
    private Uri statusUri;
    private Timer timer;
    private Button viewTransaction;

    public interface Listener {
        void onFinish();
    }

    class C12701 implements OnClickListener {
        C12701() {
        }

        public void onClick(View v) {
            TradeStatusFragment.this.onExitPressed();
        }
    }

    class C12712 implements OnClickListener {
        C12712() {
        }

        public void onClick(View v) {
            new Builder(TradeStatusFragment.this.getActivity()).setTitle(R.string.about_shapeshift_title).setMessage(R.string.about_shapeshift_message).setPositiveButton(R.string.button_ok, null).create().show();
        }
    }

    class C12723 implements DialogInterface.OnClickListener {
        C12723() {
        }

        public void onClick(DialogInterface dialog, int which) {
            TradeStatusFragment.this.contentResolver.delete(TradeStatusFragment.this.statusUri, null, null);
            TradeStatusFragment.this.onExitPressed();
        }
    }

    class C12745 implements LoaderCallbacks<Cursor> {
        C12745() {
        }

        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(TradeStatusFragment.this.application.getApplicationContext(), TradeStatusFragment.this.statusUri, null, null, null, null);
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null && data.getCount() > 0) {
                data.moveToFirst();
                TradeStatusFragment.this.handler.sendMessage(TradeStatusFragment.this.handler.obtainMessage(1, ExchangeHistoryProvider.getExchangeEntry(data)));
            }
        }

        public void onLoaderReset(Loader<Cursor> loader) {
        }
    }

    private static class MyHandler extends WeakHandler<TradeStatusFragment> {
        public MyHandler(TradeStatusFragment ref) {
            super(ref);
        }

        protected void weakHandleMessage(TradeStatusFragment ref, Message msg) {
            switch (msg.what) {
                case 0:
                    ref.updateShapeShiftStatus((ShapeShiftTxStatus) msg.obj);
                    return;
                case 1:
                    ref.updateStatus((ExchangeEntry) msg.obj);
                    return;
                case 2:
                    ref.errorIcon.setVisibility(0);
                    ref.errorText.setVisibility(0);
                    ref.errorText.setText(ref.getString(R.string.trade_status_failed_detail, msg.obj));
                    ref.stopPolling();
                    return;
                default:
                    return;
            }
        }
    }

    private static class StatusPollTask extends TimerTask {
        private final AbstractAddress depositAddress;
        private final Handler handler;
        private final ShapeShift shapeShift;

        private StatusPollTask(ShapeShift shapeShift, AbstractAddress depositAddress, Handler handler) {
            this.shapeShift = shapeShift;
            this.depositAddress = depositAddress;
            this.handler = handler;
        }

        @Override
        public void run() {
            for (int tries = 3; tries > 0; tries--) {
                try {
                    log.info("Polling status for deposit: {}", depositAddress);
                    ShapeShiftTxStatus newStatus = shapeShift.getTxStatus(depositAddress);
                    handler.sendMessage(handler.obtainMessage(UPDATE_SHAPESHIFT_STATUS, newStatus));
                    break;
                } catch (ShapeShiftException e) {
                    log.warn("Error occurred while polling", e);
                    handler.sendMessage(handler.obtainMessage(ERROR_MESSAGE, e.getMessage()));
                    break;
                } catch (IOException e) {
                    /* ignore and retry */
                }
            }
        }
    }

    private void updateStatus(ExchangeEntry newStatus) {
        this.exchangeStatus = (ExchangeEntry) Preconditions.checkNotNull(newStatus);
        updateView();
    }

    private void updateShapeShiftStatus(ShapeShiftTxStatus shapeShiftTxStatus) {
        ExchangeEntry newStatus = new ExchangeEntry(this.exchangeStatus, shapeShiftTxStatus);
        if (this.exchangeStatus.status != newStatus.status) {
            this.contentResolver.update(this.statusUri, newStatus.getContentValues(), null, null);
        }
    }

    public static TradeStatusFragment newInstance(ExchangeEntry exchangeEntry, boolean showExitButton) {
        TradeStatusFragment fragment = new TradeStatusFragment();
        Bundle args = new Bundle();
        args.putSerializable("exchange_entry", exchangeEntry);
        args.putBoolean("show_exit_button", showExitButton);
        fragment.setArguments(args);
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        this.showExitButton = args.getBoolean("show_exit_button", false);
        this.exchangeStatus = (ExchangeEntry) args.getSerializable("exchange_entry");
        this.statusUri = ExchangeHistoryProvider.contentUri(this.application.getPackageName(), this.exchangeStatus.depositAddress);
        this.loaderManager.initLoader(0, null, this.statusLoaderCallbacks);
        setHasOptionsMenu(true);
    }

    public void onDestroy() {
        this.loaderManager.destroyLoader(0);
        super.onDestroy();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trade_status, container, false);
        this.exchangeInfo = (TextView) view.findViewById(R.id.exchange_status_info);
        this.depositIcon = (TextView) view.findViewById(R.id.trade_deposit_status_icon);
        this.depositProgress = (ProgressBar) view.findViewById(R.id.trade_deposit_status_progress);
        this.depositText = (TextView) view.findViewById(R.id.trade_deposit_status_text);
        this.exchangeIcon = (TextView) view.findViewById(R.id.trade_exchange_status_icon);
        this.exchangeProgress = (ProgressBar) view.findViewById(R.id.trade_exchange_status_progress);
        this.exchangeText = (TextView) view.findViewById(R.id.trade_exchange_status_text);
        this.errorIcon = (TextView) view.findViewById(R.id.trade_error_status_icon);
        this.errorText = (TextView) view.findViewById(R.id.trade_error_status_text);
        Fonts.setTypeface(this.depositIcon, Font.COINOMI_FONT_ICONS);
        Fonts.setTypeface(this.exchangeIcon, Font.COINOMI_FONT_ICONS);
        Fonts.setTypeface(this.errorIcon, Font.COINOMI_FONT_ICONS);
        this.viewTransaction = (Button) view.findViewById(R.id.trade_view_transaction);
        this.emailReceipt = (Button) view.findViewById(R.id.trade_email_receipt);
        if (this.showExitButton) {
            view.findViewById(R.id.button_exit).setOnClickListener(new C12701());
        } else {
            view.findViewById(R.id.button_exit).setVisibility(8);
        }
        updateView();
        view.findViewById(R.id.powered_by_shapeshift).setOnClickListener(new C12712());
        return view;
    }

    public void onPause() {
        super.onPause();
        stopPolling();
    }

    public void onResume() {
        super.onResume();
        startPolling();
    }

    private void startPolling() {
        if (this.timer == null && this.exchangeStatus.status != 2) {
            this.pollTask = new StatusPollTask(this.application.getShapeShift(), this.exchangeStatus.depositAddress, this.handler);
            this.timer = new Timer();
            this.timer.schedule(this.pollTask, 0, 3000);
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

    public void onExitPressed() {
        stopPolling();
        if (this.listener != null) {
            this.listener.onFinish();
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.exchange_status, menu);
        this.actionDeleteMenu = menu.findItem(R.id.action_delete);
        updateView();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                new Builder(getActivity()).setMessage(R.string.trade_status_delete_message).setNegativeButton(R.string.button_cancel, null).setPositiveButton(R.string.button_ok, new C12723()).create().show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateView() {
        switch (this.exchangeStatus.status) {
            case -1:
                if (this.actionDeleteMenu != null) {
                    this.actionDeleteMenu.setVisible(true);
                }
                UiUtils.setVisible(this.errorIcon);
                UiUtils.setVisible(this.errorText);
                this.errorText.setText(R.string.trade_status_failed);
                stopPolling();
                return;
            case 0:
                if (this.actionDeleteMenu != null) {
                    this.actionDeleteMenu.setVisible(false);
                }
                this.exchangeInfo.setText(R.string.trade_status_message);
                UiUtils.setGone(this.depositIcon);
                UiUtils.setVisible(this.depositProgress);
                UiUtils.setVisible(this.depositText);
                this.depositText.setText(R.string.trade_status_waiting_deposit);
                UiUtils.setInvisible(this.exchangeIcon);
                UiUtils.setGone(this.exchangeProgress);
                UiUtils.setInvisible(this.exchangeText);
                UiUtils.setGone(this.errorIcon);
                UiUtils.setGone(this.errorText);
                UiUtils.setInvisible(this.viewTransaction);
                UiUtils.setInvisible(this.emailReceipt);
                return;
            case 1:
                if (this.actionDeleteMenu != null) {
                    this.actionDeleteMenu.setVisible(false);
                }
                this.exchangeInfo.setText(R.string.trade_status_message);
                UiUtils.setVisible(this.depositIcon);
                UiUtils.setGone(this.depositProgress);
                UiUtils.setVisible(this.depositText);
                this.depositText.setText(getString(R.string.trade_status_received_deposit, this.exchangeStatus.depositAmount));
                UiUtils.setGone(this.exchangeIcon);
                UiUtils.setVisible(this.exchangeProgress);
                UiUtils.setVisible(this.exchangeText);
                this.exchangeText.setText(R.string.trade_status_waiting_trade);
                UiUtils.setGone(this.errorIcon);
                UiUtils.setGone(this.errorText);
                UiUtils.setInvisible(this.viewTransaction);
                UiUtils.setInvisible(this.emailReceipt);
                return;
            case 2:
                if (this.actionDeleteMenu != null) {
                    this.actionDeleteMenu.setVisible(true);
                }
                this.exchangeInfo.setText(R.string.trade_status_complete_message);
                UiUtils.setVisible(this.depositIcon);
                UiUtils.setGone(this.depositProgress);
                UiUtils.setVisible(this.depositText);
                this.depositText.setText(getString(R.string.trade_status_received_deposit, this.exchangeStatus.depositAmount));
                UiUtils.setVisible(this.exchangeIcon);
                UiUtils.setGone(this.exchangeProgress);
                UiUtils.setVisible(this.exchangeText);
                this.exchangeText.setText(getString(R.string.trade_status_complete_detail, this.exchangeStatus.withdrawAmount));
                UiUtils.setGone(this.errorIcon);
                UiUtils.setGone(this.errorText);
                updateViewTransaction();
                updateEmailReceipt();
                stopPolling();
                return;
            default:
                return;
        }
    }

    private void updateViewTransaction() {
        final String txId = this.exchangeStatus.withdrawTransactionId;
        AbstractAddress withdrawAddress = this.exchangeStatus.withdrawAddress;
        final CoinType withdrawType = withdrawAddress.getType();
        final List<WalletAccount> accounts = this.application.getAccounts(withdrawAddress);
        if (accounts.size() > 0 || Constants.COINS_BLOCK_EXPLORERS.containsKey(withdrawType)) {
            UiUtils.setVisible(this.viewTransaction);
        } else {
            UiUtils.setGone(this.viewTransaction);
        }
        this.viewTransaction.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String accountId = null;
                for (WalletAccount account : accounts) {
                    if (account.getTransaction(txId) != null) {
                        accountId = account.getId();
                        break;
                    }
                }
                if (accountId != null && txId != null) {
                    Intent intent = new Intent(TradeStatusFragment.this.getActivity(), TransactionDetailsActivity.class);
                    intent.putExtra("account_id", accountId);
                    intent.putExtra("transaction_id", txId);
                    TradeStatusFragment.this.startActivity(intent);
                } else if (Constants.COINS_BLOCK_EXPLORERS.containsKey(withdrawType)) {
                    String url = String.format((String) Constants.COINS_BLOCK_EXPLORERS.get(withdrawType), new Object[]{txId});
                    Intent i = new Intent("android.intent.action.VIEW");
                    i.setData(Uri.parse(url));
                    TradeStatusFragment.this.startActivity(i);
                } else {
                    Toast.makeText(TradeStatusFragment.this.getActivity(), R.string.error_generic, 0).show();
                }
            }
        });
    }

    private void updateEmailReceipt() {
        UiUtils.setInvisible(this.emailReceipt);
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.listener = (Listener) context;
            this.contentResolver = context.getContentResolver();
            this.application = (WalletApplication) context.getApplicationContext();
            this.loaderManager = getLoaderManager();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + Listener.class);
        }
    }

    public void onDetach() {
        super.onDetach();
        this.listener = null;
    }
}
