package com.primestone.wallet.util;

import android.os.Build.VERSION;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinID;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.families.NxtFamily;
import com.coinomi.core.util.Currencies;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.AbstractTransaction.AbstractOutput;
import com.coinomi.core.wallet.AbstractWallet;
import com.coinomi.core.wallet.WalletAccount;
import com.primestone.wallet.Constants;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class WalletUtils {
    private static final Pattern P_SIGNIFICANT = Pattern.compile("^([-+] )?\\d*(\\.\\d{0,2})?");
    private static final Object SIGNIFICANT_SPAN = new StyleSpan(1);
    public static final RelativeSizeSpan SMALLER_SPAN = new RelativeSizeSpan(0.85f);

    public static int getIconRes(CoinType type) {
        return ((Integer) Constants.COINS_ICONS.get(type)).intValue();
    }

    public static int getIconRes(WalletAccount account) {
        return getIconRes(account.getCoinType());
    }

    public static long longHash(byte[] bytes) {
        int len = bytes.length;
        Preconditions.checkState(len >= 8);
        return (((((((((long) bytes[len - 1]) & 255) | ((((long) bytes[len - 2]) & 255) << 8)) | ((((long) bytes[len - 3]) & 255) << 16)) | ((((long) bytes[len - 4]) & 255) << 24)) | ((((long) bytes[len - 5]) & 255) << 32)) | ((((long) bytes[len - 6]) & 255) << 40)) | ((((long) bytes[len - 7]) & 255) << 48)) | ((((long) bytes[len - 8]) & 255) << 56);
    }

    public static List<AbstractAddress> getSendToAddress(AbstractTransaction tx, AbstractWallet pocket) {
        return getToAddresses(tx, pocket, false);
    }

    public static List<AbstractAddress> getReceivedWithOrFrom(AbstractTransaction tx, AbstractWallet pocket) {
        if (pocket.getCoinType() instanceof NxtFamily) {
            return tx.getReceivedFrom();
        }
        return getToAddresses(tx, pocket, true);
    }

    private static List<AbstractAddress> getToAddresses(AbstractTransaction tx, AbstractWallet pocket, boolean toMe) {
        List<AbstractAddress> addresses = new ArrayList();
        for (AbstractOutput output : tx.getSentTo()) {
            if (pocket.isAddressMine(output.getAddress()) == toMe) {
                addresses.add(output.getAddress());
            }
        }
        return addresses;
    }

    public static String localeCurrencyCode() {
        try {
            return "USD";
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String getCurrencyName(String code) {
        String currencyName = null;
        if (VERSION.SDK_INT >= 19) {
            try {
                currencyName = Currency.getInstance(code).getDisplayName(Locale.getDefault());
            } catch (IllegalArgumentException e) {
            }
        } else {
            currencyName = (String) Currencies.CURRENCY_NAMES.get(code);
        }
        if (currencyName == null) {
            try {
                currencyName = CoinID.typeFromSymbol(code).getName();
            } catch (IllegalArgumentException e2) {
            }
        }
        return currencyName;
    }
}
