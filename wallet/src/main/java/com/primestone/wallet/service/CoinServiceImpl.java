package com.primestone.wallet.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;

import com.coinomi.core.network.ConnectivityHelper;
import com.coinomi.core.network.ServerClients;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount;

import com.primestone.wallet.Configuration;
import com.primestone.wallet.Constants;
import com.primestone.wallet.WalletApplication;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.CheckForNull;


public class CoinServiceImpl extends Service implements CoinService {
    private WalletApplication application;
    private Configuration config;
    private ConnectivityHelper connHelper;
    private BroadcastReceiver connectivityReceiver;

    @CheckForNull
    private ServerClients clients;

    private String lastAccount;

    private NotificationManager nm;
    private static final int NOTIFICATION_ID_CONNECTED = 0;
    private static final int NOTIFICATION_ID_COINS_RECEIVED = 1;

    private int notificationCount = 0;
    private BigInteger notificationAccumulatedAmount = BigInteger.ZERO;
    private final List<AbstractAddress> notificationAddresses = new LinkedList<>();
    private AtomicInteger transactionsReceived = new AtomicInteger();
    private long serviceCreatedAt;

    private static final int MIN_COLLECT_HISTORY = 2;
    private static final int IDLE_BLOCK_TIMEOUT_MIN = 2;
    private static final int IDLE_TRANSACTION_TIMEOUT_MIN = 9;
    private static final int MAX_HISTORY_SIZE = Math.max(IDLE_TRANSACTION_TIMEOUT_MIN, IDLE_BLOCK_TIMEOUT_MIN);

    private static final Logger log = LoggerFactory.getLogger(CoinService.class);

    private final IBinder mBinder = new LocalBinder();

    private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            log.debug("Received a tick {}", (Object) intent);

            if (clients != null) {
                clients.ping(application.getVersionString());
            }

            long lastStop = application.getLastStop();
            if (lastStop > 0) {
                long secondsIdle = (SystemClock.elapsedRealtime() - lastStop) / 1000;
                if (secondsIdle > Constants.STOP_SERVICE_AFTER_IDLE_SECS) {
                    log.info("Idling detected, stopping service");
                    stopSelf();
                }
            }
        }
    };

    public class LocalBinder extends Binder {
        public CoinService getService() {
            return CoinServiceImpl.this;
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        private final ConnectivityManager connectivityManager;
        private boolean hasConnectivity;
        private boolean hasStorage = true;
        private int currentNetworkType = -1;

        public MyBroadcastReceiver(ConnectivityManager connectivityManager) {
            this.connectivityManager = connectivityManager;
            checkNetworkType();
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();

            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                hasConnectivity = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

                boolean isNetworkChanged = checkNetworkType();
                log.info("network is " + (hasConnectivity ? "up" : "down"));
                log.info("network type " + (isNetworkChanged ? "changed" : "didn't change"));

                check(isNetworkChanged);
            } else if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(action)) {
                hasStorage = false;
                log.info("device storage low");

                check(false);
            } else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(action)) {
                hasStorage = true;
                log.info("device storage ok");

                check(false);
            }
        }

        private boolean checkNetworkType() {
            boolean isNetworkChanged;
            NetworkInfo activeInfo = connectivityManager.getActiveNetworkInfo();
            if (activeInfo != null && activeInfo.isConnected()) {
                isNetworkChanged = currentNetworkType != activeInfo.getType();
                currentNetworkType = activeInfo.getType();
            } else {
                isNetworkChanged = false;
                currentNetworkType = -1;
            }
            return isNetworkChanged;
        }

        private void check(boolean isNetworkChanged) {
            Wallet wallet = CoinServiceImpl.this.application.getWallet();
            boolean hasEverything = this.hasConnectivity && this.hasStorage && wallet != null;
            if (hasEverything && CoinServiceImpl.this.clients == null) {
                CoinServiceImpl.log.info("Creating coins clients");
                CoinServiceImpl.this.clients = CoinServiceImpl.this.getServerClients(wallet);
            } else if (hasEverything && isNetworkChanged) {
                CoinServiceImpl.log.info("Restarting coins clients as network changed");
                CoinServiceImpl.this.clients.resetConnections();
            } else if (!hasEverything && CoinServiceImpl.this.clients != null) {
                CoinServiceImpl.log.info("stopping stratum clients");
                CoinServiceImpl.this.disconnectClients();
            }
        }
    }

    private ServerClients getServerClients(Wallet wallet) {
        ServerClients newClients = new ServerClients(Constants.DEFAULT_COINS_SERVERS, this.connHelper);
        if (this.application.getTxCachePath() != null) {
            newClients.setCacheDir(this.application.getTxCachePath(), 5242880);
        }
        return newClients;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        log.debug(".onBind()");
        return mBinder;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        log.debug(".onUnbind()");
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        serviceCreatedAt = System.currentTimeMillis();
        log.debug(".onCreate()");

        super.onCreate();

        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        application = (WalletApplication) getApplication();
        config = application.getConfiguration();
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        connHelper = getConnectivityHelper(connManager);

        connectivityReceiver = new MyBroadcastReceiver(connManager);
        final IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction("android.intent.action.DEVICE_STORAGE_LOW");
        intentFilter.addAction("android.intent.action.DEVICE_STORAGE_OK");
        registerReceiver(connectivityReceiver, intentFilter);
        registerReceiver(tickReceiver, new IntentFilter("android.intent.action.TIME_TICK"));
    }

    private ConnectivityHelper getConnectivityHelper(final ConnectivityManager manager) {
        return new ConnectivityHelper() {
            @Override
            public boolean isConnected() {
                NetworkInfo activeInfo = manager.getActiveNetworkInfo();
                return activeInfo != null && activeInfo.isConnected();
            }
        };
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        log.info("service start command: " + intent
                + (intent.hasExtra("android.intent.extra.ALARM_COUNT") ? " (alarm count: " + intent.getIntExtra("android.intent.extra.ALARM_COUNT", 0) + ")" : ""));

        final String action = intent.getAction();

        if (CoinService.ACTION_CANCEL_COINS_RECEIVED.equals(action)) {
            notificationCount = 0;
            notificationAccumulatedAmount = BigInteger.ZERO;
            notificationAddresses.clear();

            nm.cancel(NOTIFICATION_ID_COINS_RECEIVED);
        } else if (CoinService.ACTION_CLEAR_CONNECTIONS.equals(action)) {
            disconnectClients();
        } else if (CoinService.ACTION_RESET_ACCOUNT.equals(action)) {
            if (application.getWallet() != null) {
                Wallet wallet = application.getWallet();
                if (intent.hasExtra(Constants.ARG_ACCOUNT_ID)) {
                    String accountId = intent.getStringExtra(Constants.ARG_ACCOUNT_ID);
                    WalletAccount account = wallet.getAccount(accountId);
                    if (account != null) {
                        account.refresh();

                        if (clients == null) {
                            if (connHelper.isConnected()) {
                                clients = getServerClients(wallet);
                                clients.startAsync(account);
                            }
                        } else {
                            clients.resetAccount(account);
                        }
                    } else {
                        log.warn("Tried to start a service for account id {} but no account found.", accountId);
                    }
                } else {
                    log.warn("Missing account id argument, not doing anything");
                }
            } else {
                log.warn("Got wallet reset intent, but no wallet is available");
            }
        } else if (CoinService.ACTION_RESET_WALLET.equals(action)) {
            if (application.getWallet() != null) {
                Wallet wallet = application.getWallet();

                if (clients == null) {
                    if (connHelper.isConnected()) {
                        clients = getServerClients(wallet);
                    }
                }

                for (WalletAccount account : wallet.getAllAccounts()) {
                    account.refresh();

                    if (clients != null) {
                        this.clients.startOrResetAccountAsync(account);
                    }
                }
            } else {
                log.warn("Got wallet reset intent, but no wallet is available");
            }
        } else if (CoinService.ACTION_CONNECT_COIN.equals(action)) {
            if (application.getWallet() != null) {
                Wallet wallet = application.getWallet();
                if (intent.hasExtra(Constants.ARG_ACCOUNT_ID)) {
                    lastAccount = intent.getStringExtra(Constants.ARG_ACCOUNT_ID);
                    WalletAccount account = wallet.getAccount(lastAccount);
                    if (account != null) {
                        if (clients == null && connHelper.isConnected()) {
                            clients = getServerClients(wallet);
                        }
                        if (clients != null) {
                            this.clients.startAsync(account);
                        }
                    } else {
                        log.warn("Tried to start a service for account id {} but no account found.", this.lastAccount);
                    }
                } else {
                    log.warn("Missing account id argument, not doing anything");
                }
            } else {
                log.error("Got connect coin intent, but no wallet is available");
            }
        } else if (CoinService.ACTION_CONNECT_ALL_COIN.equals(action)) {
            if (application.getWallet() != null) {
                Wallet wallet = application.getWallet();
                if (clients == null && connHelper.isConnected()) {
                    clients = getServerClients(wallet);
                }
                if (clients != null) {
                    for (WalletAccount account : wallet.getAllAccounts()) {
                        clients.startAsync(account);
                    }
                }
            } else {
                log.error("Got connect coin intent, but no wallet is available");
            }
        } else if (CoinService.ACTION_BROADCAST_TRANSACTION.equals(action)) {
            final Sha256Hash hash = new Sha256Hash(intent.getByteArrayExtra(CoinService.ACTION_BROADCAST_TRANSACTION_HASH));
            Transaction tx = null;

            if (clients != null) {
                log.info("broadcasting transaction " + tx.getHashAsString());
                broadcastTransaction(tx);
            } else {
                log.info("client not available, not broadcasting transaction " + tx.getHashAsString());
            }
        }
        return START_REDELIVER_INTENT;
    }

    private void broadcastTransaction(Transaction tx) {
        // TODO send broadcast message
    }

    @Override
    public void onDestroy() {
        log.debug(".onDestroy()");

        unregisterReceiver(tickReceiver);
        unregisterReceiver(connectivityReceiver);

        disconnectClients();
        application.saveWalletNow();

        super.onDestroy();
        log.info("service was up for " + (((System.currentTimeMillis() - this.serviceCreatedAt) / 1000) / 60) + " minutes");
    }

    private void disconnectClients() {
        if (clients != null) {
            clients.stopAllAsync();
            clients = null;
        }
    }

    @Override
    public void onLowMemory() {
        log.warn("low memory detected, stopping service");
        stopSelf();
    }
}
