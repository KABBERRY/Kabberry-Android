package com.primestone.wallet.tasks;

import android.os.AsyncTask;

import com.coinomi.core.exchange.shapeshift.data.ShapeShiftCoins;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftException;

import com.primestone.wallet.WalletApplication;

public class ExchangeCheckSupportedCoinsTask extends AsyncTask<Void, Void, Void> {
    private final WalletApplication application;
    private Exception error;
    private final Listener listener;
    private ShapeShiftCoins shapeShiftCoins;

    public interface Listener {
        void onExchangeCheckSupportedCoinsTaskFinished(Exception exception, ShapeShiftCoins shapeShiftCoins);

        void onExchangeCheckSupportedCoinsTaskStarted();
    }

    public ExchangeCheckSupportedCoinsTask(Listener listener, WalletApplication application) {
        this.listener = listener;
        this.application = application;
    }

    protected void onPreExecute() {
        this.listener.onExchangeCheckSupportedCoinsTaskStarted();
    }

    protected Void doInBackground(Void... params) {
        if (this.application.isConnected()) {
            try {
                this.shapeShiftCoins = this.application.getShapeShift().getCoins();
            } catch (Exception e) {
                this.error = e;
            }
        } else {
            this.error = new ShapeShiftException("No connection");
        }
        return null;
    }

    protected void onPostExecute(Void v) {
        this.listener.onExchangeCheckSupportedCoinsTaskFinished(this.error, this.shapeShiftCoins);
    }
}
