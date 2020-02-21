package com.kabberry.wallet.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;

import com.coinomi.core.coins.families.Families;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.AbstractWallet;

import com.kabberry.wallet.AddressBookProvider;
import com.kabberry.wallet.R;
import com.kabberry.wallet.util.WalletUtils;
import com.kabberry.wallet.util.TimeUtils;
import com.kabberry.wallet.util.Fonts;

import org.bitcoinj.core.TransactionConfidence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

public class TransactionsListAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater inflater;
    private final AbstractWallet walletPocket;

    private final List<AbstractTransaction> transactions = new ArrayList<>();
    private final Resources res;
    private int precision = 0;
    private int shift = 0;
    private boolean showEmptyText = false;

    private final int colorSignificant;
    private final int colorLessSignificant;
    private final int colorInsignificant;
    private final int colorError;
    private final String minedTitle;
    private final String fontIconMined;
    private final String sentToTitle;
    private final String receivedFromTitle;
    private final String receivedWithTitle;

    private final Map<AbstractAddress, String> labelCache = new HashMap<>();
    private static final Object CACHE_NULL_MARKER = "";

    private static final String CONFIDENCE_SYMBOL_DEAD = "\u271D"; // latin cross
    private static final String CONFIDENCE_SYMBOL_UNKNOWN = "?";

    private static final int VIEW_TYPE_TRANSACTION = 0;

    Typeface fonticons;
    Spannable span_value = null;
    String symbol;

    @Deprecated
    public TransactionsListAdapter(final Context context, @Nonnull final AbstractWallet walletPocket) {
        this.context = context;
        inflater = LayoutInflater.from(context);

        this.walletPocket = walletPocket;

        res = context.getResources();
        colorSignificant = res.getColor(R.color.app_green);
        colorLessSignificant = res.getColor(R.color.gray_54_sec_text_icons);
        colorInsignificant = res.getColor(R.color.gray_26_hint_text);
        colorError = res.getColor(R.color.fg_error);
        minedTitle = res.getString(R.string.wallet_transactions_coinbase);
        fontIconMined = res.getString(R.string.font_icon_mining);

        sentToTitle = res.getString(R.string.sent);
        receivedWithTitle = res.getString(R.string.received);
        receivedFromTitle = res.getString(R.string.received);
        fonticons = Typeface.createFromAsset(context.getAssets(), "fonts/fontawesome-webfont.ttf");
    }

    public void setPrecision(final int precision, final int shift) {
        this.precision = precision;
        this.shift = shift;

        notifyDataSetChanged();
    }

    public void replace(@Nonnull final AbstractTransaction tx) {
        transactions.clear();
        transactions.add(tx);

        notifyDataSetChanged();
    }

    public void replace(@Nonnull final Collection<AbstractTransaction> transactions) {
        this.transactions.clear();
        this.transactions.addAll(transactions);

        showEmptyText = true;

        notifyDataSetChanged();
    }

    @Override
    public boolean isEmpty() {
        return showEmptyText && super.isEmpty();
    }

    @Override
    public int getCount() {
        return transactions.size();
    }

    @Override
    public AbstractTransaction getItem(final int position) {
        if (position == transactions.size()) {
            return null;
        }

        return (AbstractTransaction) transactions.get(position);
    }

    @Override
    public long getItemId(final int position) {
        if (position == transactions.size()) {
            return 0;
        }

        return WalletUtils.longHash(((AbstractTransaction) transactions.get(position)).getHashBytes());
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(final int position, View row, final ViewGroup parent) {
        final int type = getItemViewType(position);

        if (type == VIEW_TYPE_TRANSACTION) {
            if (row == null) {
                row = inflater.inflate(R.layout.transaction_row, null);
            }
            setAllTypefaceThin(row);
            final AbstractTransaction tx = getItem(position);
            bindView(row, tx);
        } else {
            throw new IllegalStateException("unknown type: " + type);
        }
        return row;
    }

    public void bindView(@Nonnull final View view, @Nonnull final AbstractTransaction tx) {
//        Resources res = context.getResources();
        final TransactionConfidence.ConfidenceType confidenceType = tx.getConfidenceType();
//        final boolean isOwn = tx.getSource().equals(TransactionConfidence.Source.SELF);
//
        final Value value = tx.getValue(walletPocket);
//        final boolean sent = value.signum() < 0;
//
        final CoinType type = walletPocket.getCoinType();
//
//        setAllTypefaceThin(view);

        // TextView
        final TextView rowDirectionText = (TextView) view.findViewById(R.id.transaction_row_direction_text);
        // ImageView
//        final ImageView rowDirectionFontIcon = (ImageView) view.findViewById(R.id.transaction_row_direction_font_icon);
//        Fonts.setTypeface(rowDirectionFontIcon, Fonts.Font.COINOMI_FONT_ICONS);
        // ImageView
        final ImageView rowConfirmationsFontIcon = (ImageView) view.findViewById(R.id.transaction_row_confirmations_font_icon);
//        Fonts.setTypeface(rowConfirmationsFontIcon, Fonts.Font.COINOMI_FONT_ICONS);
        // TextView
//        final TextView rowMessageFontIcon = (TextView) view.findViewById(R.id.transaction_row_message_font_icon);
//        Fonts.setTypeface(rowMessageFontIcon, Fonts.Font.COINOMI_FONT_ICONS);

        final TextView rowDate = (TextView) view.findViewById(R.id.transaction_row_time);
//        setAllTypeNumber(rowDate);
        // TextView - transaction_row_value
        final TextView rowValue = (TextView) view.findViewById(R.id.transaction_row_value);

//        LinearLayout rootlayout = (LinearLayout)view.findViewById(R.id.ll_root_layout);
//        setAllTypeNumber(rowValue);

//        View v_down = view.findViewById(R.id.v_down);

        // confidence
        if (confidenceType == TransactionConfidence.ConfidenceType.PENDING) {
            rowValue.setTextColor(colorInsignificant);
            rowValue.setBackgroundResource(R.drawable.rv_pending);
        } else if (confidenceType == TransactionConfidence.ConfidenceType.BUILDING) {
            rowValue.setTextColor(colorSignificant);

            if (value.isNegative()) {
                rowValue.setTextColor(res.getColor(R.color.receive_color_fg));
                rowValue.setBackgroundResource(R.drawable.rv_receive_bg);
            } else {
                rowValue.setTextColor(res.getColor(R.color.receive_color_fg));
                rowValue.setBackgroundResource(R.drawable.rv_receive_bg);
            }
        } else if (confidenceType == TransactionConfidence.ConfidenceType.DEAD) {
            rowValue.setTextColor(colorSignificant);
            Fonts.strikeThrough(rowValue);
        }
        else {
            rowValue.setTextColor(colorInsignificant);
        }

        // Confirmations
        if (tx.getDepthInBlocks() < 4) {
            rowConfirmationsFontIcon.setVisibility(View.VISIBLE);
            rowConfirmationsFontIcon.setColorFilter(colorSignificant);

            switch (tx.getDepthInBlocks()) {
                case 0:     // No confirmations
                    rowConfirmationsFontIcon.setImageResource(R.drawable.ic_icon_trans_0);
                    rowConfirmationsFontIcon.setColorFilter(colorInsignificant);    // PENDING
                    break;
                case 1:     // 1 out of 3 confirmations
                    rowConfirmationsFontIcon.setImageResource(R.drawable.ic_icon_trans_1);
                    break;
                case 2:     // 2 out of 3 confirmations
                    rowConfirmationsFontIcon.setImageResource(R.drawable.ic_icon_trans_2);
                    break;
                case 3:     // 3 out of 3 confirmations
                    rowConfirmationsFontIcon.setImageResource(R.drawable.ic_icon_trans_3);
                    break;
            }
        } else {
            rowConfirmationsFontIcon.setVisibility(View.GONE);
        }

        // Money direction and icon
        if (tx.isGenerated()) {
            rowDirectionText.setText(minedTitle);
        }
        else {
            if (value.isNegative()) {
                rowDirectionText.setText(sentToTitle);
                //rowDirectionText.setVisibility(View.GONE);
            }
            else if (walletPocket.getCoinType().getFamily() != Families.NXT) {
                rowDirectionText.setText(receivedWithTitle);
            }
            else {
                rowDirectionText.setText(receivedFromTitle);
            }
        }

        // date
        final long time = tx.getTimestamp();

        if (time > 0) {
            rowDate.setText(TimeUtils.toRelativeTimeString(time));
            rowDate.setVisibility(View.VISIBLE);
        } else {
            rowDate.setText(context.getString(R.string.pending));
            rowDate.setVisibility(View.VISIBLE);
        }

        // address - label
//        final AbstractAddress address;
//        final String label;

//        if (sent) {
//            // we send payment to those addresses
//            List<AbstractAddress> sentTo = WalletUtils.getSendToAddress(tx, walletPocket);
//            // For now show only the first address
//            address = sentTo.size() == 0 ? null : sentTo.get(0);
//        } else {
//            // received with those addresses
//            List<AbstractAddress> receivedWith = WalletUtils.getReceivedWithOrFrom(tx, walletPocket);
//            // Should be one
//            address = receivedWith.size() == 0 ? null : receivedWith.get(0);
//        }

//        if (address != null) {
//            label = resolveLabel(address);
//        } else {
//            if (sent) {
//                // If no address found, assume it is an internal transfer
//                label = res.getString(R.string.internal_transfer);
//            } else {
//                label = "?";
//            }
//        }

        //////
        symbol = type.getSymbol();
        if (value.toString().startsWith("-"))
        {
            String v = value.toString().replace("-", "").replace(symbol, (new StringBuilder()).append(" ").append(symbol).toString());
            span_value = android.text.Spannable.Factory.getInstance().newSpannable(v);
            span_value.setSpan(new RelativeSizeSpan(0.67F), span_value.length() - type.getSymbol().length(), span_value.length(), 33);
            rowValue.setText(span_value);
        } else
        {
            String v = value.toString().replace(symbol, (new StringBuilder()).append(" ").append(symbol).toString());
            span_value = android.text.Spannable.Factory.getInstance().newSpannable(v);
            span_value.setSpan(new RelativeSizeSpan(0.67F), span_value.length() - type.getSymbol().length(), span_value.length(), 33);
            rowValue.setText(span_value);
        }
    }

    private String resolveLabel(@Nonnull final AbstractAddress address) {
        final String cachedLabel = (String) labelCache.get(address);
        if (cachedLabel == null) {
            final String label = AddressBookProvider.resolveLabel(context, address);
            if (label != null) {
                labelCache.put(address, label);
            } else {
                labelCache.put(address, (String) CACHE_NULL_MARKER);
            }
            return label;
        } else {
            return cachedLabel != CACHE_NULL_MARKER ? cachedLabel : null;
        }
    }

    public void clearLabelCache() {
        labelCache.clear();

        notifyDataSetChanged();
    }

    protected void setAllTypefaceThin(View view) {
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getChildCount() != 0) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllTypefaceThin(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.createFromAsset(this.context.getAssets(), "fonts/Roboto-Regular.ttf"));
        }
    }

    protected void setAllTypeNumber(View view) {
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getChildCount() != 0) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllTypeNumber(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.createFromAsset(this.context.getAssets(), "fonts/Roboto-Regular.ttf"));
        }
    }
}
