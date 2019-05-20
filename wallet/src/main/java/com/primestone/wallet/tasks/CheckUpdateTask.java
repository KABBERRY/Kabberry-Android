package com.primestone.wallet.tasks;

import android.os.AsyncTask;

import android.support.v7.widget.helper.ItemTouchHelper.Callback;
import com.google.common.base.Charsets;

import com.primestone.wallet.Constants;
import com.primestone.wallet.service.CoinService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckUpdateTask extends AsyncTask<Void, Void, Integer> {
    private static final Logger log = LoggerFactory.getLogger(CheckUpdateTask.class);

    @Override
    protected Integer doInBackground(Void... params) {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) new URL(Constants.VERSION_URL).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(Constants.NETWORK_TIMEOUT_MS);
            connection.setReadTimeout(Constants.NETWORK_TIMEOUT_MS);
            connection.setRequestProperty("Accept-Charset", "utf-8");
            connection.connect();

            // CPW
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                final BufferedReader reader =
                        new BufferedReader(new InputStreamReader(connection.getInputStream(),
                                Charsets.UTF_8), 64);
                String line = reader.readLine().trim();
                log.info("verson responce: " + line);
                reader.close();
                return Integer.valueOf(line);
            }
        } catch (final Exception e) {
            log.info("Could not check for update: {}", e.getMessage());
        } finally {
            if (connection != null)
                connection.disconnect();
        }

        return null;
    }
}
