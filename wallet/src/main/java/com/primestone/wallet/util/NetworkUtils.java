package com.primestone.wallet.util;

import android.content.Context;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.ConnectionSpec;
import com.squareup.okhttp.OkHttpClient;
import java.io.File;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class NetworkUtils {
    private static OkHttpClient httpClient;

    public static OkHttpClient getHttpClient(Context context) {
        if (httpClient == null) {
            httpClient = new OkHttpClient();
            httpClient.setConnectionSpecs(Collections.singletonList(ConnectionSpec.CLEARTEXT));
            httpClient.setConnectTimeout(15000, TimeUnit.MILLISECONDS);
            httpClient.setCache(new Cache(new File(context.getCacheDir(), "http_cache"), 262144));
        }
        return httpClient;
    }
}
