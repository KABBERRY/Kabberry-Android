package com.primestone.wallet.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.support.v4.content.CursorLoader;

import com.primestone.wallet.Configuration;
import com.primestone.wallet.ExchangeRatesProvider;

public final class ExchangeRateLoader extends CursorLoader implements OnSharedPreferenceChangeListener {
    private final BroadcastReceiver broadcastReceiver = new C11331();
    private final Configuration config;
    private final Context context;
    private String localCurrency;
    private final String packageName;

    class C11331 extends BroadcastReceiver {
        private boolean hasConnectivity = true;

        C11331() {
        }

        public void onReceive(Context context, Intent intent) {
            boolean z = false;
            String action = intent.getAction();
            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
                if (!intent.getBooleanExtra("noConnectivity", false)) {
                    z = true;
                }
                this.hasConnectivity = z;
                if (this.hasConnectivity) {
                    ExchangeRateLoader.this.forceLoad();
                }
            } else if ("android.intent.action.TIME_TICK".equals(action) && this.hasConnectivity) {
                ExchangeRateLoader.this.forceLoad();
            }
        }
    }

    public ExchangeRateLoader(final Context context, final Configuration config,
                              final String localSymbol, final String coinSymbol) {
        //Context context2 = context;
        super(context, ExchangeRatesProvider.contentUriToCrypto(context.getPackageName(),
                localSymbol, false), null, "currency_id",
                new String[]{coinSymbol}, null);

        this.config = config;
        this.packageName = context.getPackageName();
        this.context = context;
        this.localCurrency = localSymbol;
    }

    public ExchangeRateLoader(final Context context, final Configuration config,
                              final String localSymbol) {
        super(context, ExchangeRatesProvider.contentUriToCrypto(context.getPackageName(),
                localSymbol, false), null, null, new String[]{null}, null);

        this.config = config;
        this.packageName = context.getPackageName();
        this.context = context;
        this.localCurrency = localSymbol;
    }

    protected void onStartLoading() {
        super.onStartLoading();
        refreshUri(this.config.getExchangeCurrencyCode());
        this.config.registerOnSharedPreferenceChangeListener(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction("android.intent.action.TIME_TICK");
        this.context.registerReceiver(this.broadcastReceiver, intentFilter);
        forceLoad();
    }

    protected void onStopLoading() {
        this.config.unregisterOnSharedPreferenceChangeListener(this);
        try {
            this.context.unregisterReceiver(this.broadcastReceiver);
        } catch (IllegalArgumentException e) {
        }
        super.onStopLoading();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("exchange_currency".equals(key)) {
            onCurrencyChange();
        }
    }

    private void onCurrencyChange() {
        refreshUri(this.config.getExchangeCurrencyCode());
        forceLoad();
    }

    private void refreshUri(String newLocalCurrency) {
        if (!newLocalCurrency.equals(this.localCurrency)) {
            this.localCurrency = newLocalCurrency;
            setUri(ExchangeRatesProvider.contentUriToCrypto(this.packageName, this.localCurrency, false));
        }
    }
}
