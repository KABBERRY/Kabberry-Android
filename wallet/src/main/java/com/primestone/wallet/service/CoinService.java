package com.primestone.wallet.service;

public interface CoinService {
    public static final String ACTION_BROADCAST_TRANSACTION = (CoinService.class.getPackage().getName() + ".broadcast_transaction");
    String ACTION_BROADCAST_TRANSACTION_HASH = "hash";
    public static final String ACTION_CANCEL_COINS_RECEIVED = (CoinService.class.getPackage().getName() + ".cancel_coins_received");
    public static final String ACTION_CLEAR_CONNECTIONS = (CoinService.class.getPackage().getName() + ".clear_connections");
    public static final String ACTION_CONNECT_ALL_COIN = (CoinService.class.getPackage().getName() + ".connect_all_coin");
    public static final String ACTION_CONNECT_COIN = (CoinService.class.getPackage().getName() + ".connect_coin");
    public static final String ACTION_RESET_ACCOUNT = (CoinService.class.getPackage().getName() + ".reset_account");
    public static final String ACTION_RESET_WALLET = (CoinService.class.getPackage().getName() + ".reset_wallet");

    public enum ServiceMode {
        NORMAL,
        CANCEL_COINS_RECEIVED
    }
}
