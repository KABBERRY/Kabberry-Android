package com.kabberry.wallet.util;

import android.os.Handler;

import com.coinomi.core.coins.Value;
import com.coinomi.core.wallet.AbstractTransaction;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.core.wallet.WalletAccountEventListener;
import com.coinomi.core.wallet.WalletConnectivityStatus;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ThrottlingWalletChangeListener implements WalletAccountEventListener {
    private final long throttleMs;
    private final boolean coinsRelevant;
    private final boolean reorganizeRelevant;
    private final boolean confidenceRelevant;
    private final boolean connectivityRelevant;

    private final AtomicLong lastMessageTime;
    private final Handler handler;
    private final AtomicBoolean relevant;

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            lastMessageTime.set(System.currentTimeMillis());
            onThrottledWalletChanged();
        }
    };

    /*
    class C13041 implements Runnable {
        C13041() {
        }

        public void run() {
            ThrottlingWalletChangeListener.this.lastMessageTime.set(System.currentTimeMillis());
            ThrottlingWalletChangeListener.this.onThrottledWalletChanged();
        }
    }
    */

    public abstract void onThrottledWalletChanged();

    public ThrottlingWalletChangeListener() {
        this(500);
    }

    public ThrottlingWalletChangeListener(final long throttleMs) {
        this(throttleMs, true, true, true, true);
    }

    public ThrottlingWalletChangeListener(final long throttleMs, final boolean coinsRelevant,
                                          final boolean reorganizeRelevant, final boolean confidenceRelevant,
                                          final boolean connectivityRelevant) {
        this.lastMessageTime = new AtomicLong(0);
        this.handler = new Handler();
        this.relevant = new AtomicBoolean();
//        this.runnable = new C13041();
        this.throttleMs = throttleMs;
        this.coinsRelevant = coinsRelevant;
        this.reorganizeRelevant = reorganizeRelevant;
        this.confidenceRelevant = confidenceRelevant;
        this.connectivityRelevant = connectivityRelevant;
    }

    @Override
    public final void onWalletChanged(WalletAccount pocket) {
        if (relevant.getAndSet(false)) {
            handler.removeCallbacksAndMessages(null);

            if (System.currentTimeMillis() - lastMessageTime.get() > throttleMs) {
                handler.post(runnable);
            } else {
                handler.postDelayed(runnable, throttleMs);
            }
        }
    }

    public void removeCallbacks() {
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onNewBalance(Value newBalance) {
        if (coinsRelevant) {
            relevant.set(true);
        }
    }

    @Override
    public void onTransactionConfidenceChanged(final WalletAccount pocket, final AbstractTransaction tx) {
        if (confidenceRelevant) relevant.set(true);
    }

    @Override
    public void onNewBlock(WalletAccount pocket) {
        if (confidenceRelevant) {
            relevant.set(true);
        }
    }

    @Override
    public void onConnectivityStatus(WalletConnectivityStatus pocketConnectivity) {
        if (connectivityRelevant) {
            relevant.set(true);
        }
    }

    @Override
    public void onTransactionBroadcastFailure(WalletAccount pocket, AbstractTransaction tx) {
    }

    @Override
    public void onTransactionBroadcastSuccess(WalletAccount pocket, AbstractTransaction tx) {
    }
}
