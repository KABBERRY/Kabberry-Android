package com.kabberry.wallet;

import android.text.format.DateUtils;

import com.coinomi.core.coins.CoinID;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.KabberryMain;
import com.coinomi.core.network.CoinAddress;
import com.coinomi.stratumj.ServerAddress;
import com.google.common.collect.ImmutableList;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Constants {
    public static final String ARG_ACCOUNT_ID = "account_id";
    public static final String ARG_SEND_TO_ACCOUNT_ID = "send_to_account_id";

    public static final HashMap<CoinType, String> COINS_BLOCK_EXPLORERS = new HashMap<>();
    public static final HashMap<CoinType, Integer> COINS_ICONS;

    public static final CoinType DEFAULT_COIN = KabberryMain.get();
    public static final List<CoinType> DEFAULT_COINS = ImmutableList.of((CoinType)KabberryMain.get());

    public static final List<CoinAddress> DEFAULT_COINS_SERVERS =
            ImmutableList.of(new CoinAddress(KabberryMain.get(),
                    new ServerAddress("77.55.216.249", 50001)));

    public static boolean IsfromBackground = false;
    public static final List<CoinType> SUPPORTED_COINS = ImmutableList.of((CoinType)KabberryMain.get());
    public static final Charset US_ASCII = Charset.forName("US-ASCII");
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    public static final String TX_CACHE_NAME = "tx_cache";

    public static final long WALLET_WRITE_DELAY = 5;

    public static final TimeUnit WALLET_WRITE_DELAY_UNIT = TimeUnit.SECONDS;
    public static final long STOP_SERVICE_AFTER_IDLE_SECS = 30 * 60; // 30 mins

    public static final int NETWORK_TIMEOUT_MS = 15 * (int) DateUtils.SECOND_IN_MILLIS;

    public static final long RATE_UPDATE_FREQ_MS = 30 * DateUtils.SECOND_IN_MILLIS;

    /** Default currency to use if all default mechanisms fail. */
    public static final String DEFAULT_EXCHANGE_CURRENCY = "USD";

    public static final String VERSION_URL = "http://explorer.kabberry.com/api/getversion";
    static {
        COINS_ICONS = new HashMap<>();
        COINS_ICONS.put(CoinID.Primestone_MAIN.getCoinType(), R.drawable.app_launcher);
        COINS_BLOCK_EXPLORERS.put(CoinID.Primestone_MAIN.getCoinType(), "http://explorer.kabberry.com/api/tx/%s");
    }
}
