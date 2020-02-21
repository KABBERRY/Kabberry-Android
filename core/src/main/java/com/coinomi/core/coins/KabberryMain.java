package com.coinomi.core.coins;

import com.coinomi.core.coins.families.BitFamily;
import com.coinomi.core.coins.families.PeerFamily;

public class KabberryMain extends BitFamily {
    private static KabberryMain instance = new KabberryMain();

    private KabberryMain() {
        id = "kabberry.main";

        addressHeader = 56;
        p2shHeader = 118;
        acceptableAddressCodes = new int[]{this.addressHeader, this.p2shHeader};
        spendableCoinbaseDepth = 520;
        dumpedPrivateKeyHeader = 188;

        name = "Kabberry";
        symbol = "KKC";
        uriScheme = "kabberry";
        bip44Index = 119;
        unitExponent = 8;
        feeValue = value(10000);
        minNonDust = value(1);
        softDustLimit = value(10000);
        softDustPolicy = SoftDustPolicy.BASE_FEE_FOR_EACH_SOFT_DUST_TXO;
        signedMessageHeader = CoinType.toBytes("Kabberry signed Message:\n");
    }

    public static synchronized KabberryMain get() {
        KabberryMain kabberryMain;
        synchronized (KabberryMain.class) {
            kabberryMain = instance;
        }
        return kabberryMain;
    }
}
