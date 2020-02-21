package com.kabberry.wallet.util;

import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.FiatType;
import com.coinomi.core.util.MonetaryFormat;
import com.google.common.base.Preconditions;

import org.bitcoinj.core.Monetary;

public final class MonetarySpannable extends SpannableString {
    public static final Object BOLD_SPAN = new StyleSpan(1);
    public static final RelativeSizeSpan SMALLER_SPAN = new RelativeSizeSpan(0.85f);

    public MonetarySpannable(MonetaryFormat format, boolean signed, Monetary value) {
        super(format(format, signed, value));
    }

    private static CharSequence format(MonetaryFormat format, boolean signed, Monetary value) {
        if (value == null) {
            return "";
        }
        boolean z = value.signum() >= 0 || signed;
        Preconditions.checkArgument(z);
        int smallestUnitExponent = value.smallestUnitExponent();

        if (signed) {
            return format.negativeSign('-').positiveSign('+').format(value, smallestUnitExponent);
        }

        return format.format(value, smallestUnitExponent);
    }
}
