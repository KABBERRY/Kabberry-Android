package com.primestone.wallet.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.messages.TxMessage;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.AbstractTransaction.AbstractOutput;
import com.coinomi.core.wallet.AbstractWallet;

import com.primestone.wallet.R;
import com.primestone.wallet.Constants;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.ui.widget.MDToast;
import com.primestone.wallet.util.ThrottlingWalletChangeListener;
import com.primestone.wallet.util.TimeUtils;
import com.primestone.wallet.util.UiUtils;
import com.primestone.wallet.util.WeakHandler;
import com.uncopt.android.widget.text.justify.JustifiedEditText;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import mehdi.sakout.fancybuttons.FancyButton;
import org.acra.ACRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionDetailsFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(TransactionDetailsFragment.class);
    private String accountId;
    private TransactionAmountVisualizerAdapter adapter;
    public FancyButton blockExplorerLink;
    private final Handler handler = new MyHandler(this);
    public LinearLayout ll_date;
    private ListView outputRows;
    private AbstractWallet pocket;
    private TextView txDate;
    private TextView txDateLabel;
    private String txId;
    private TextView txIdView;
    private TextView txMessage;
    private TextView txMessageLabel;
    private TextView txStatusView;
    private CoinType type;
    private final ThrottlingWalletChangeListener walletListener = new C12773();

    class C12751 implements OnItemClickListener {
        C12751() {
        }

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (position >= TransactionDetailsFragment.this.outputRows.getHeaderViewsCount()) {
                Object obj = parent.getItemAtPosition(position);
                if (obj != null && (obj instanceof AbstractOutput)) {
                    UiUtils.startAddressActionMode(((AbstractOutput) obj).getAddress(), TransactionDetailsFragment.this.getActivity(), TransactionDetailsFragment.this.getFragmentManager());
                }
            }
        }
    }

    class C12773 extends ThrottlingWalletChangeListener {
        C12773() {
        }

        public void onThrottledWalletChanged() {
            TransactionDetailsFragment.this.handler.sendMessage(TransactionDetailsFragment.this.handler.obtainMessage(0));
        }
    }

    private static class MyHandler extends WeakHandler<TransactionDetailsFragment> {
        public MyHandler(TransactionDetailsFragment ref) {
            super(ref);
        }

        protected void weakHandleMessage(TransactionDetailsFragment ref, Message msg) {
            switch (msg.what) {
                case 0:
                    ref.updateView();
                    return;
                default:
                    return;
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.accountId = getArguments().getString("account_id");
        }
        this.pocket = (AbstractWallet) getWalletApplication().getAccount(this.accountId);
        if (this.pocket == null) {
            MDToast.makeText(getActivity(), getActivity().getString(R.string.no_such_pocket_error), MDToast.LENGTH_LONG, 3);
        } else {
            this.type = this.pocket.getCoinType();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transaction_details, container, false);
        setAllTypefaceThin(view);
        this.outputRows = (ListView) view.findViewById(R.id.output_rows);
        this.outputRows.setOnItemClickListener(getListener());

        View footer = inflater.inflate(R.layout.fragment_transaction_row_footer, null);
        ButterKnife.bind((Object) this, footer);
        setAllTypefaceThin(footer);

        this.outputRows.addFooterView(footer, null, false);
        this.txIdView = (TextView) footer.findViewById(R.id.tx_id);
        this.txMessageLabel = (TextView) footer.findViewById(R.id.tx_message_label);
        this.txMessage = (TextView) footer.findViewById(R.id.tx_message);
        this.blockExplorerLink = (FancyButton) footer.findViewById(R.id.block_explorer_link);
        this.ll_date = (LinearLayout) footer.findViewById(R.id.ll_date);
        this.txStatusView = (TextView) footer.findViewById(R.id.tx_status);
        this.txDateLabel = (TextView) footer.findViewById(R.id.tx_date_label);
        this.txDate = (TextView) footer.findViewById(R.id.tx_date);
        this.pocket.addEventListener(this.walletListener);
        this.adapter = new TransactionAmountVisualizerAdapter(inflater.getContext(), this.pocket);
        this.outputRows.setAdapter(this.adapter);
        this.txId = getArguments().getString("transaction_id");

        updateView();

        return view;
    }

    private OnItemClickListener getListener() {
        return new C12751();
    }

    public void onDestroyView() {
        this.pocket.removeEventListener(this.walletListener);
        this.walletListener.removeCallbacks();
        super.onDestroyView();
    }

    private void updateView() {
        if (!isRemoving() && !isDetached()) {
            if (this.txId == null) {
                cannotShowTxDetails();
                return;
            }
            AbstractTransaction tx = this.pocket.getTransaction(this.txId);
            if (tx == null) {
                cannotShowTxDetails();
            } else {
                showTxDetails(this.pocket, tx);
            }
        }
    }

    private void showTxDetails(AbstractWallet pocket, AbstractTransaction tx) {
        String txStatusText;
        switch (tx.getConfidenceType()) {
            case BUILDING:
                txStatusText = getResources().getQuantityString(R.plurals.status_building, tx.getDepthInBlocks(), new Object[]{Integer.valueOf(tx.getDepthInBlocks())});
                this.txStatusView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_done_grey600_24dp, 0);
                break;
            case PENDING:
                txStatusText = getString(R.string.pending);
                this.txStatusView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_schedule_grey600_24dp, 0);
                break;
            default:
                txStatusText = getString(R.string.status_unknown);
                break;
        }
        this.txStatusView.setText(txStatusText);
        long timestamp = tx.getTimestamp();
        if (timestamp > 0) {
            this.txDate.setText(TimeUtils.toTimeString(getWalletApplication(), timestamp));
            this.txDateLabel.setVisibility(0);
            this.txDate.setVisibility(0);
        } else {
            this.txDateLabel.setVisibility(8);
            this.txDate.setVisibility(8);
            this.ll_date.setVisibility(8);
        }
        this.adapter.setTransaction(tx);
        this.txIdView.setText(tx.getHashAsString());
        setupBlockExplorerLink(pocket.getCoinType(), tx.getHashAsString());
        if (this.type.canHandleMessages() && tx.getMessage() != null) {
            try {
                TxMessage message = tx.getMessage();
                if (message != null) {
                    this.txMessageLabel.setText(getString(R.string.tx_message_public));
                    this.txMessageLabel.setVisibility(0);
                    this.txMessage.setText(message.toString());
                    this.txMessage.setVisibility(0);
                }
            } catch (Exception e) {
                ACRA.getErrorReporter().handleSilentException(e);
            }
        }
    }

    private void setupBlockExplorerLink(CoinType type, String txHash) {
        if (Constants.COINS_BLOCK_EXPLORERS.containsKey(type)) {
            final String url = String.format((String) Constants.COINS_BLOCK_EXPLORERS.get(type), new Object[]{txHash});
            this.blockExplorerLink.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Intent i = new Intent("android.intent.action.VIEW");
                    i.setData(Uri.parse(url));
                    TransactionDetailsFragment.this.startActivity(i);
                }
            });
            return;
        }
        this.blockExplorerLink.setVisibility(View.GONE);
    }

    private void cannotShowTxDetails() {
        MDToast.makeText(getActivity(), getActivity().getString(R.string.get_tx_info_error), MDToast.LENGTH_LONG, 3);
        getActivity().finish();
    }

    WalletApplication getWalletApplication() {
        return (WalletApplication) getActivity().getApplication();
    }

    @OnClick(R.id.tx_id)
    public void ontxHashClick() {
        UiUtils.copy(getActivity(), txId);
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
