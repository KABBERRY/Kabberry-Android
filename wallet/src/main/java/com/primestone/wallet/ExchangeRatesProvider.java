package com.primestone.wallet;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.Uri.Builder;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;

import com.coinomi.core.coins.CoinID;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.FiatValue;
import com.coinomi.core.coins.Value;
import com.coinomi.core.util.ExchangeRateBase;
import com.google.common.collect.ImmutableMap;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ConnectionSpec;

import com.primestone.wallet.util.NetworkUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import java.security.cert.CertificateException;



import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ExchangeRatesProvider extends ContentProvider {

    public static class ExchangeRate {
        @Nonnull public final ExchangeRateBase rate;
        public final String currencyCodeId;
        @Nullable public final String source;

        public ExchangeRate(@Nonnull final ExchangeRateBase rate, final String currencyCodeId,
                            @Nullable final String source) {
            this.rate = rate;
            this.currencyCodeId = currencyCodeId;
            this.source = source;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '[' + this.rate.value1 + " ~ " + this.rate.value2 + ']';
        }
    }

    public static final String KEY_CURRENCY_ID = "currency_id";
    private static final String KEY_RATE_COIN = "rate_coin";
    private static final String KEY_RATE_FIAT = "rate_fiat";
    private static final String KEY_RATE_COIN_CODE = "rate_coin_code";
    private static final String KEY_RATE_FIAT_CODE = "rate_fiat_code";
    private static final String KEY_SOURCE = "source";

    private static final String QUERY_PARAM_OFFLINE = "offline";

    private ConnectivityManager connManager;
    private Configuration config;

    private Map<String, ExchangeRate> localToCryptoRates = null;
    private long localToCryptoLastUpdated = 0;
    private String lastLocalCurrency = null;

    private Map<String, ExchangeRate> cryptoToLocalRates = null;
    private long cryptoToLocalLastUpdated = 0;
    private String lastCryptoCurrency = "USD";

    private static final String BASE_URL = "http://primestone-explorer.com";
    private static final String TO_CRYPTO_URL = BASE_URL + "/getdata/%s";
    private static final String TO_LOCAL_URL = BASE_URL + "/getdata/%s";
    private static final Logger log = LoggerFactory.getLogger(ExchangeRatesProvider.class);

    @Override
    public boolean onCreate() {
        final Context context = getContext();

        connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        config = new Configuration(PreferenceManager.getDefaultSharedPreferences(context));

        lastLocalCurrency = config.getCachedExchangeLocalCurrency();
        if (lastLocalCurrency != null) {
            localToCryptoRates = parseExchangeRates(
                    config.getCachedExchangeRatesJson(), lastLocalCurrency, true);
            localToCryptoLastUpdated = 0;
        }

        return true;
    }

    private static Builder contentUri(@Nonnull final String packageName, final boolean offline) {
        final Builder builder = Uri.parse("content://" + packageName + ".exchange_rates").buildUpon();
        if (offline) {
            builder.appendQueryParameter(QUERY_PARAM_OFFLINE, "1");
        }
        return builder;
    }

    public static Uri contentUriToLocal(@Nonnull final String packageName,
                                        @Nonnull final String coinSymbol, final boolean offline) {
        final Builder uri = contentUri(packageName, offline);
        uri.appendPath("to-local").appendPath(coinSymbol);
        return uri.build();
    }

    public static Uri contentUriToCrypto(String packageName, String localSymbol, boolean offline) {
        Builder uri = contentUri(packageName, offline);
        uri.appendPath("to-crypto").appendPath(localSymbol);
        return uri.build();
    }

    public static ExchangeRate getRate(Context context, String coinSymbol, String localSymbol) {
        ExchangeRate rate = null;
        if (context != null) {
            Cursor cursor = context.getContentResolver().query(contentUriToCrypto(context.getPackageName(), localSymbol, true), null, "currency_id", new String[]{coinSymbol}, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    rate = getExchangeRate(cursor);
                }
                cursor.close();
            }
        }
        return rate;
    }

    public static Map<String, ExchangeRate> getRates(final Context context,
                                                     @Nonnull String localSymbol) {
        ImmutableMap.Builder<String, ExchangeRate> builder = ImmutableMap.builder();

        if (context != null) {
            final Uri uri = contentUriToCrypto(context.getPackageName(), localSymbol, true);
            final Cursor cursor = context.getContentResolver().query(uri, null, null,
                    new String[]{null}, null);

            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    ExchangeRate rate = getExchangeRate(cursor);
                    builder.put(rate.currencyCodeId, rate);
                } while (cursor.moveToNext());
                cursor.close();
            }
        }

        return builder.build();
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, final String selection,
                        final String[] selectionArgs, final String sortOrder) {
        final long now = System.currentTimeMillis();

        final List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() != 2) {
            throw new IllegalArgumentException("Unrecognized URI: " + uri);
        }

        final boolean offline = uri.getQueryParameter(QUERY_PARAM_OFFLINE) != null;
        long lastUpdated;

        final String symbol;
        final boolean isLocalToCrypto;

        if (pathSegments.get(0).equals("to-crypto")) {
            isLocalToCrypto = true;
            symbol = pathSegments.get(1);
            lastUpdated = symbol.equals(lastLocalCurrency) ? localToCryptoLastUpdated : 0;
        } else if (pathSegments.get(0).equals("to-local")) {
            isLocalToCrypto = false;
            symbol = "USD";
            lastUpdated = symbol.equals(lastCryptoCurrency) ? cryptoToLocalLastUpdated : 0;
        } else {
            throw new IllegalArgumentException("Unrecognized URI path: " + uri);
        }

        if (!offline && (lastUpdated == 0 || now - lastUpdated > Constants.RATE_UPDATE_FREQ_MS)) {
            URL url;
            try {
                if (isLocalToCrypto) {
                    url = new URL(String.format(TO_CRYPTO_URL, symbol));
                } else {
                    url = new URL(String.format(TO_LOCAL_URL, symbol));
                }
            }
            catch (final MalformedURLException x) {
                throw new RuntimeException(x); // Should not happen
            }

            JSONObject newExchangeRatesJson = requestExchangeRatesJson(url);
            Map<String, ExchangeRate> newExchangeRates =
                    parseExchangeRates(newExchangeRatesJson, symbol, isLocalToCrypto);

            if (newExchangeRates != null) {
                if (isLocalToCrypto) {
                    localToCryptoRates = newExchangeRates;
                    localToCryptoLastUpdated = now;
                    lastLocalCurrency = symbol;
                    config.setCachedExchangeRates(lastLocalCurrency, newExchangeRatesJson);
                } else {
                    cryptoToLocalRates = newExchangeRates;
                    cryptoToLocalLastUpdated = now;
                    lastCryptoCurrency = symbol;
                }
            }
        }

        Map<String, ExchangeRate> exchangeRates = isLocalToCrypto ? localToCryptoRates : cryptoToLocalRates;

        if (exchangeRates == null) {
            return null;
        }

        final MatrixCursor cursor = new MatrixCursor(new String[]{BaseColumns._ID,
                KEY_CURRENCY_ID, KEY_RATE_COIN, KEY_RATE_COIN_CODE, KEY_RATE_FIAT, KEY_RATE_FIAT_CODE, KEY_SOURCE});
                //{"_id", "currency_id", "rate_coin", "rate_coin_code", "rate_fiat", "rate_fiat_code", "source"});

        if (selection == null) {
            for (final Map.Entry<String, ExchangeRate> entry : exchangeRates.entrySet()) {
                final ExchangeRate exchangeRate = entry.getValue();
                addRow(cursor, exchangeRate);
            }
        } else if (selection.equals(KEY_CURRENCY_ID)) {
            final ExchangeRate exchangeRate = exchangeRates.get(selectionArgs[0]);
            if (exchangeRate != null) {
                addRow(cursor, exchangeRate);
            }
        }

        return cursor;
    }

    private void addRow(MatrixCursor cursor, ExchangeRate exchangeRate) {
        ExchangeRateBase rate = exchangeRate.rate;
        String codeId = exchangeRate.currencyCodeId;
        cursor.newRow().add(Integer.valueOf(codeId.hashCode())).add(codeId).add(Long.valueOf(rate.value1.value)).add(rate.value1.type.getSymbol()).add(Long.valueOf(rate.value2.value)).add(rate.value2.type.getSymbol()).add(exchangeRate.source);
    }

    public static String getCurrencyCodeId(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndexOrThrow("currency_id"));
    }

    public static ExchangeRate getExchangeRate(@Nonnull final Cursor cursor) {
        final String codeId = getCurrencyCodeId(cursor);
        final Value rateCoin = Value.valueOf(CoinID.typeFromSymbol(cursor.getString(cursor.getColumnIndexOrThrow("rate_coin_code"))), cursor.getLong(cursor.getColumnIndexOrThrow("rate_coin")));
        final Value rateFiat = FiatValue.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("rate_fiat_code")), cursor.getLong(cursor.getColumnIndexOrThrow("rate_fiat")));
        return new ExchangeRate(new ExchangeRateBase(rateCoin, rateFiat), codeId, cursor.getString(cursor.getColumnIndexOrThrow("source")));
    }

    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.setSslSocketFactory(sslSocketFactory);
            okHttpClient.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private JSONObject requestExchangeRatesJson(final URL url) {
        // Return null if no connection
        final NetworkInfo activeInfo = connManager.getActiveNetworkInfo();

        if (activeInfo == null || !activeInfo.isConnected()) {
            return null;
        }

        final long start = System.currentTimeMillis();

//        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
//                .tlsVersions(TlsVersion.TLS_1_2)
//                .cipherSuites(
//                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
//                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
//                        CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
//                .build();

//        OkHttpClient client = NetworkUtils.getHttpClient(getContext().getApplicationContext());
        OkHttpClient client = getUnsafeOkHttpClient();
        Request request = new Request.Builder().url(url).build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                log.info("fetched exchange rates from {}, took {} ms", url,
                        System.currentTimeMillis() - start);
                String responce = response.body().string().trim();
                Log.e("Ticker responce = ", responce);
                return new JSONObject(responce);
            }
            log.warn("Error HTTP code '{}' when fetching exchange rates from {}",
                    response.code(), url);
        } catch (IOException e) {
            log.warn("Error '{}' when fetching exchange rates from {}", e.getMessage(), url);
        } catch (JSONException e) {
            log.warn("Could not parse exchange rates JSON: {}", e.getMessage());
        }

        return null;
    }

    private Map<String, ExchangeRate> parseExchangeRates(JSONObject json, String fromSymbol, boolean isLocalToCrypto) {
        if (json == null) {
            return null;
        }

        JSONObject jsonTemp = new JSONObject();
        try {
            jsonTemp.put("PSC", json.get("price").toString());
        } catch (JSONException e) {

        }

        Map<String, ExchangeRate> rates = new TreeMap();
        CoinType type = isLocalToCrypto ? null : CoinID.typeFromSymbol(fromSymbol);
        try {
            Iterator<String> i = jsonTemp.keys();
            while (i.hasNext()) {
                String toSymbol = (String) i.next();
                if (!"extras".equals(toSymbol)) {
                    String rateStr = jsonTemp.optString(toSymbol, null);
                    if (rateStr == null) {
                        continue;
                    } else {
                        String localSymbol;
                        if (isLocalToCrypto) {
                            try {
                                type = CoinID.typeFromSymbol(toSymbol);
                            } catch (Exception x) {
                                log.debug("ignoring {}/{}: {}", toSymbol, fromSymbol, x.getMessage());
                            }
                        }
                        if (isLocalToCrypto) {
                            localSymbol = fromSymbol;
                        } else {
                            localSymbol = toSymbol;
                        }
                        rates.put(toSymbol, new ExchangeRate(new ExchangeRateBase(type.oneCoin(), FiatValue.parse(localSymbol, rateStr)), toSymbol, "coinomi.com"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("problem parsing exchange rates: {}", e.getMessage());
        }
        if (rates.size() == 0) {
            return null;
        }
        return rates;
    }
}
