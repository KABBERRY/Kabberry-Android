package com.kabberry.wallet;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SharedPreferences.Editor;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.ValueType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.kabberry.wallet.util.WalletUtils;

import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class Configuration {

    public final int lastVersionCode;

    private final SharedPreferences prefs;

    private static final String PREFS_KEY_LAST_VERSION = "last_version";
    private static final String PREFS_KEY_LAST_USED = "last_used";
    @Deprecated
    private static final String PREFS_KEY_LAST_POCKET = "last_pocket";
    private static final String PREFS_KEY_LAST_ACCOUNT = "last_account";

    /* Preference keys. Check also res/xml/preferences.xml */
    public static final String PREFS_KEY_BTC_PRECISION = "btc_precision";
    public static final String PREFS_KEY_CONNECTIVITY_NOTIFICATION = "connectivity_notification";
    public static final String PREFS_KEY_EXCHANGE_CURRENCY = "exchange_currency";
    public static final String PREFS_KEY_FEES = "fees";
    public static final String PREFS_KEY_DISCLAIMER = "disclaimer";
    public static final String PREFS_KEY_SELECTED_ADDRESS = "selected_address";

    private static final String PREFS_KEY_LABS_QR_PAYMENT_REQUEST = "labs_qr_payment_request";

    private static final String PREFS_KEY_CACHED_EXCHANGE_LOCAL_CURRENCY = "cached_exchange_local_currency";
    private static final String PREFS_KEY_CACHED_EXCHANGE_RATES_JSON = "cached_exchange_rates_json";

    private static final String PREFS_KEY_LAST_EXCHANGE_DIRECTION = "last_exchange_direction";
    private static final String PREFS_KEY_CHANGE_LOG_VERSION = "change_log_version";
    public static final String PREFS_KEY_REMIND_BACKUP = "remind_backup";

    public static final String PREFS_KEY_MANUAL_RECEIVING_ADDRESSES = "manual_receiving_addresses";

    public static final String PREFS_KEY_DEVICE_COMPATIBLE = "device_compatible";

    public static final String PREFS_KEY_TERMS_ACCEPTED = "terms_accepted";

    private static final int PREFS_DEFAULT_BTC_SHIFT = 3;
    private static final int PREFS_DEFAULT_BTC_PRECISION = 2;

    private static final Logger log = LoggerFactory.getLogger(Configuration.class);

    public Configuration(final SharedPreferences prefs) {
        this.prefs = prefs;

        this.lastVersionCode = prefs.getInt(PREFS_KEY_LAST_VERSION, 0);
    }

    public void registerOnSharedPreferenceChangeListener(final OnSharedPreferenceChangeListener listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(final OnSharedPreferenceChangeListener listener) {
        this.prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public void updateLastVersionCode(final int currentVersionCode) {
        if (currentVersionCode != lastVersionCode) {
            prefs.edit().putInt(PREFS_KEY_LAST_VERSION, currentVersionCode).apply();
        }

        if (currentVersionCode > this.lastVersionCode) {
            log.info("detected app upgrade: " + this.lastVersionCode + " -> " + currentVersionCode);
        } else if (currentVersionCode < lastVersionCode) {
            log.warn("detected app downgrade: " + this.lastVersionCode + " -> " + currentVersionCode);
        }

        applyUpdates();
    }

    private void applyUpdates() {
        if (prefs.contains(PREFS_KEY_LAST_POCKET)) {
            prefs.edit().remove(PREFS_KEY_LAST_POCKET).apply();
        }
    }

    public void SavePassword(String password) {
        prefs.edit().putString("password", password).apply();
    }

    public String GetPassword() {
        return prefs.getString("password", null);
    }

    @Nullable
    public String getLastAccountId() {
        return prefs.getString("last_account", null);
    }

    public void touchLastAccountId(String accountId) {
        String lastAccountId = prefs.getString(PREFS_KEY_LAST_ACCOUNT, Constants.DEFAULT_COIN.getId());
        if (!lastAccountId.equals(accountId)) {
            prefs.edit().putString(PREFS_KEY_LAST_ACCOUNT, accountId).apply();
            log.info("last used wallet account id: {}", (Object) accountId);
        }
    }

    public Map<CoinType, Value> getFeeValues() {
        JSONObject feesJson = getFeesJson();
        Builder<CoinType, Value> feesMapBuilder = ImmutableMap.builder();
        for (CoinType type : Constants.SUPPORTED_COINS) {
            feesMapBuilder.put(type, getFeeFromJson(feesJson, type));
        }
        return feesMapBuilder.build();
    }

    public Value getFeeValue(CoinType type) {
        return getFeeFromJson(getFeesJson(), type);
    }

    public void resetFeeValue(CoinType type) {
        JSONObject feesJson = getFeesJson();
        feesJson.remove(type.getId());
        prefs.edit().putString("fees", feesJson.toString()).apply();
    }

    public void setFeeValue(Value feeValue) {
        JSONObject feesJson = getFeesJson();
        try {
            feesJson.put(feeValue.type.getId(), feeValue.toUnitsString());
        } catch (Throwable e) {
            log.error("Error setting fee value", e);
        }
        prefs.edit().putString("fees", feesJson.toString()).apply();
    }

    private Value getFeeFromJson(JSONObject feesJson, CoinType type) {
        String feeStr = feesJson.optString(type.getId());
        if (feeStr.isEmpty()) {
            return type.getDefaultFeeValue();
        }
        return Value.valueOf((ValueType) type, feeStr);
    }

    private JSONObject getFeesJson() {
        try {
            log.error("TAG json fee==", this.prefs.getString("fees", ""));
            return new JSONObject(prefs.getString("fees", ""));
        } catch (JSONException e) {
            log.error("TAG json fee  JSONException==", prefs.getString("fees", ""));
            return new JSONObject();
        }
    }

    public void setPinFail(Integer isverify) {
        prefs.edit().putInt("KEY_PIN_FAILS", isverify.intValue()).apply();
    }

    public Integer GetPinFail() {
        return Integer.valueOf(prefs.getInt("KEY_PIN_FAILS", 0));
    }

    public void sevePinKey(String pinkey) {
        prefs.edit().putString("KEY_PIN_IDENTIFIER", pinkey).apply();
    }

    public String GetPinKey() {
        return prefs.getString("KEY_PIN_IDENTIFIER", "");
    }

    @Nullable
    public String getExchangeCurrencyCode(boolean useDefaultFallback) {
        String defaultCode = null;
        if (useDefaultFallback) {
            defaultCode = WalletUtils.localeCurrencyCode();
            defaultCode = defaultCode == null ? Constants.DEFAULT_EXCHANGE_CURRENCY : defaultCode;
        }
        return prefs.getString("exchange_currency", defaultCode);
    }

    public String getExchangeCurrencyCode() {
        return getExchangeCurrencyCode(true);
    }

    public void setExchangeCurrencyCode(String exchangeCurrencyCode) {
        prefs.edit().putString("exchange_currency", exchangeCurrencyCode).apply();
    }

    public JSONObject getCachedExchangeRatesJson() {
        try {
            return new JSONObject(prefs.getString("cached_exchange_rates_json", ""));
        } catch (JSONException e) {
            return null;
        }
    }

    public String getCachedExchangeLocalCurrency() {
        return prefs.getString("cached_exchange_local_currency", null);
    }

    public void setCachedExchangeRates(String currency, JSONObject exchangeRatesJson) {
        Editor edit = prefs.edit();
        edit.putString("cached_exchange_local_currency", currency);
        edit.putString("cached_exchange_rates_json", exchangeRatesJson.toString());
        edit.apply();
    }

    public boolean getLastExchangeDirection() {
        return prefs.getBoolean("last_exchange_direction", true);
    }

    public void setLastExchangeDirection(boolean exchangeDirection) {
        prefs.edit().putBoolean("last_exchange_direction", exchangeDirection).apply();
    }

    public boolean isManualAddressManagement() {
        return prefs.getBoolean("manual_receiving_addresses", false);
    }

    public void setManualAddressManagement(boolean isManualAddress) {
        prefs.edit().putBoolean("manual_receiving_addresses", isManualAddress).apply();
    }

    public void setDeviceCompatible(boolean isDeviceCompatible) {
        prefs.edit().putBoolean("device_compatible", isDeviceCompatible).apply();
    }

    public boolean isDeviceCompatible() {
        return prefs.getBoolean("device_compatible", false);
    }

    public boolean getTermsAccepted() {
        return prefs.getBoolean("terms_accepted", false);
    }

    public void setTermAccepted(boolean isTermsAccepted) {
        prefs.edit().putBoolean("terms_accepted", isTermsAccepted).apply();
    }
}
