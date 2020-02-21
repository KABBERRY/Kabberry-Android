package com.kabberry.wallet.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.ValueType;
import com.coinomi.core.util.MonetaryFormat;

import com.kabberry.wallet.R;
import com.kabberry.wallet.util.MonetarySpannable;
import org.bitcoinj.core.Coin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AmountEditView extends LinearLayout {
    private boolean amountSigned = false;
    private EditText amountText;
    private final TextViewListener amountTextListener = new TextViewListener();
    private MonetaryFormat format = new MonetaryFormat().noCode();
    @Nonnull
    private Value hint;
    private Listener listener;
    private TextView symbol;
    @Nonnull
    private ValueType type;
    Typeface typeface;

    public interface Listener {
        void changed();
        void focusChanged(final boolean hasFocus);
    }

    private final class TextViewListener implements TextWatcher, OnFocusChangeListener {
        private boolean fire;

        private TextViewListener() {
            fire = true;
        }

        public void setFire(boolean fire) {
            this.fire = fire;
        }

        @Override
        public void afterTextChanged(final Editable s) {
            String original = s.toString();
            String replaced = original.replace(',', '.');
            if (!replaced.equals(original)) {
                s.clear();
                s.append(replaced);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (listener != null && fire) {
                listener.changed();
            }
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                Value amount = getAmount();
                if (amount != null) {
                    setAmount(amount, false);
                }
            }

            if (listener != null && fire) {
                listener.focusChanged(hasFocus);
            }
        }
    }

    public AmountEditView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.amount_edit_my, this, true);
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.amount_layout);
        typeface = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Bold.ttf");

        amountText = (EditText) layout.findViewById(R.id.et_amount);
        amountText.addTextChangedListener(amountTextListener);
        amountText.setOnFocusChangeListener(amountTextListener);
        amountText.setTextColor(Color.parseColor("#FFFFFF"));
        amountText.setTypeface(typeface);

        symbol = (TextView) layout.findViewById(R.id.amount_symbol);

        symbol.setTypeface(Typeface.createFromAsset(context.getAssets(),
                "fonts/Roboto-Regular.ttf"));
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("amount_edit_view_super_state", super.onSaveInstanceState());
        bundle.putSerializable("amount_edit_view_type", type);
        bundle.putSerializable("amount_edit_view_format", format);
        bundle.putString("amount_edit_view_text", amountText.getText().toString());
        bundle.putBoolean("amount_edit_view_amount_signed", amountSigned);
        bundle.putSerializable("amount_edit_view_value", getAmount());
        bundle.putSerializable("amount_edit_view_hint_value", hint);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            setType((ValueType) bundle.getSerializable("amount_edit_view_type"));
            bundle.putSerializable("amount_edit_view_format", format);
            amountText.setText(bundle.getString("amount_edit_view_text"));
            bundle.putBoolean("amount_edit_view_amount_signed", amountSigned);
            setAmount((Value) bundle.getSerializable("amount_edit_view_value"), false);
            setHint((Value) bundle.getSerializable("amount_edit_view_hint_value"));
            state = bundle.getParcelable("amount_edit_view_super_state");
            amountText.setTypeface(typeface);
        }

        super.onRestoreInstanceState(state);
    }

    public void reset() {
        amountText.setText(null);
        symbol.setText(null);
        type = null;
        hint = null;
    }

    public void resetType(CoinType type, boolean updateView) {
        if (resetType(type) && updateView) {
            updateAppearance();
        }
    }

    public boolean resetType(ValueType newType) {
        if (type != null && type.equals(newType)) {
            return false;
        }

        type = newType;
        hint = null;
        setFormat(newType.getMonetaryFormat());
        return true;
    }

    public void setFormat(MonetaryFormat inputFormat) {
        format = inputFormat.noCode();
        updateAppearance();
    }

    public void setType(ValueType type) {
        this.type = type;
        updateAppearance();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setHint(Value hint) {
        this.hint = hint;
        updateAppearance();
    }

    public void setAmountSigned(boolean amountSigned) {
        amountSigned = amountSigned;
    }

    public void setSingleLine(boolean isSingleLine) {
        if (isSingleLine) {
            setOrientation(0);
        } else {
            setOrientation(1);
        }
    }

    @Nullable
    public Value getAmount() {
        String str = amountText.getText().toString().trim();
        Value amount = null;

        try {
            if (!(str.isEmpty() || type == null)) {
                amount = format.parse(type, str);
            }
        } catch (Exception e) {
        }
        return amount;
    }

    public void setAmount(@Nullable final Value value, boolean fireListener) {
        if (!fireListener) {
            amountTextListener.setFire(false);
        }

        if (value != null) {
            amountText.setText(new MonetarySpannable(format, amountSigned, value));
            amountText.setTypeface(typeface);
        } else {
            amountText.setText(null);
            amountText.setTypeface(typeface);
        }
        if (!fireListener) {
            amountTextListener.setFire(true);
        }
    }

    public String getAmountText()
    {
        return amountText.getText().toString().trim();
    }

    public TextView getAmountView() {
        return amountText;
    }

    private void updateAppearance() {
        if (type != null) {
            if (type.getSymbol().toString().equals("USD"))
                symbol.setText(type.getSymbol() + " Value");
            else
                symbol.setText(type.getSymbol() + " Amount");
            symbol.setVisibility(VISIBLE);
        } else {
            symbol.setText(null);
            symbol.setVisibility(GONE);
        }

        if (type != null) {
            if (type.getSymbol().toString().equals("USD"))
                amountText.setHint(new MonetarySpannable(format.positiveSign('$'), amountSigned, hint != null ? hint : Coin.ZERO));
        } else
            amountText.setHint(new MonetarySpannable(format, amountSigned, hint != null ? hint : Coin.ZERO));

        amountText.setTypeface(typeface);
    }
}
