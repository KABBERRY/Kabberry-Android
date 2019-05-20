package com.primestone.wallet.ui;

import android.view.View;

import com.coinomi.core.coins.Value;
import com.coinomi.core.util.ExchangeRate;

import com.primestone.wallet.ui.widget.AmountEditView;
import com.primestone.wallet.ui.widget.AmountEditView.Listener;

public final class CurrencyCalculatorLink {
    private final AmountEditView coinAmountView;
    private final Listener coinAmountViewListener = new C11241();
    private boolean enabled = true;
    private boolean exchangeDirection = true;
    private ExchangeRate exchangeRate = null;
    private Listener listener = null;
    private final AmountEditView localAmountView;
    private final Listener localAmountViewListener = new C11252();

    class C11241 implements Listener {
        C11241() {
        }

        public void changed() {
            if (CurrencyCalculatorLink.this.coinAmountView.getAmount() != null) {
                CurrencyCalculatorLink.this.setExchangeDirection(true);
            } else {
                CurrencyCalculatorLink.this.localAmountView.setHint(null);
            }
            if (CurrencyCalculatorLink.this.listener != null) {
                CurrencyCalculatorLink.this.listener.changed();
            }
        }

        public void focusChanged(boolean hasFocus) {
            if (CurrencyCalculatorLink.this.listener != null) {
                CurrencyCalculatorLink.this.listener.focusChanged(hasFocus);
            }
        }
    }

    class C11252 implements Listener {
        C11252() {
        }

        public void changed() {
            if (CurrencyCalculatorLink.this.localAmountView.getAmount() != null) {
                CurrencyCalculatorLink.this.setExchangeDirection(false);
            } else {
                CurrencyCalculatorLink.this.coinAmountView.setHint(null);
            }
            if (CurrencyCalculatorLink.this.listener != null) {
                CurrencyCalculatorLink.this.listener.changed();
            }
        }

        public void focusChanged(boolean hasFocus) {
            if (CurrencyCalculatorLink.this.listener != null) {
                CurrencyCalculatorLink.this.listener.focusChanged(hasFocus);
            }
        }
    }

    public CurrencyCalculatorLink(AmountEditView coinAmountView, AmountEditView localAmountView) {
        this.coinAmountView = coinAmountView;
        this.coinAmountView.setListener(this.coinAmountViewListener);
        this.localAmountView = localAmountView;
        this.localAmountView.setListener(this.localAmountViewListener);
        update();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setExchangeRate(ExchangeRate exchangeRate) {
        this.exchangeRate = exchangeRate;
        update();
    }

    public Value getPrimaryAmount() {
        Value value = null;
        if (this.exchangeDirection) {
            return this.coinAmountView.getAmount();
        }
        if (this.exchangeRate == null) {
            return value;
        }
        Value localAmount = this.localAmountView.getAmount();
        if (localAmount == null) {
            return value;
        }
        try {
            return convertSafe(localAmount);
        } catch (ArithmeticException e) {
            return value;
        }
    }

    public Value getSecondaryAmount() {
        Value value = null;
        if (this.exchangeRate == null) {
            return value;
        }
        if (!this.exchangeDirection) {
            return this.localAmountView.getAmount();
        }
        Value coinAmount = this.coinAmountView.getAmount();
        if (coinAmount == null) {
            return value;
        }
        try {
            return convertSafe(coinAmount);
        } catch (ArithmeticException e) {
            return value;
        }
    }

    public Value getRequestedAmount() {
        if (this.exchangeDirection) {
            return this.coinAmountView.getAmount();
        }
        return this.localAmountView.getAmount();
    }

    private void update() {
        this.coinAmountView.setEnabled(this.enabled);
        if (this.exchangeRate != null) {
            this.localAmountView.setEnabled(this.enabled);
            this.localAmountView.resetType(this.exchangeRate.getDestinationType());
            this.localAmountView.setVisibility(View.VISIBLE);
            this.coinAmountView.resetType(this.exchangeRate.getSourceType());
            if (this.exchangeDirection) {
                Value coinAmount = this.coinAmountView.getAmount();
                if (coinAmount != null) {
                    this.localAmountView.setAmount(null, false);
                    this.localAmountView.setHint(convertSafe(coinAmount));
                    this.coinAmountView.setHint(null);
                    return;
                }
                return;
            }
            Value localAmount = this.localAmountView.getAmount();
            if (localAmount != null) {
                this.localAmountView.setHint(null);
                this.coinAmountView.setAmount(null, false);
                this.coinAmountView.setHint(convertSafe(localAmount));
                return;
            }
            return;
        }
        this.localAmountView.setEnabled(false);
        this.localAmountView.setHint(null);
        this.localAmountView.setVisibility(4);
        this.coinAmountView.setHint(null);
    }

    private Value convertSafe(Value amount) {
        try {
            return this.exchangeRate.convert(amount);
        } catch (Exception e) {
            return null;
        }
    }

    public void setExchangeDirection(boolean exchangeDirection) {
        this.exchangeDirection = exchangeDirection;
        update();
    }

    public boolean getExchangeDirection() {
        return this.exchangeDirection;
    }

    public View activeTextView() {
        if (this.exchangeDirection) {
            return this.coinAmountView.getAmountView();
        }
        return this.localAmountView.getAmountView();
    }

    public void requestFocus() {
        activeTextView().requestFocus();
    }

    public void setExchangeRateHints(Value primaryAmount) {
        if (this.exchangeRate != null) {
            this.coinAmountView.setHint(primaryAmount);
            this.localAmountView.setHint(convertSafe(primaryAmount));
        }
    }

    public void setPrimaryAmount(Value amount) {
        Listener listener = this.listener;
        this.listener = null;
        this.coinAmountView.setAmount(amount, true);
        this.listener = listener;
    }

    public boolean isEmpty() {
        return this.coinAmountView.getAmountText().isEmpty() && this.localAmountView.getAmountText().isEmpty();
    }
}
