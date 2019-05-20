package com.coinomi.core.coins;

import com.coinomi.core.coins.families.BitFamily;
import com.coinomi.core.coins.families.PeerFamily;

public class PrimestoneMain extends BitFamily {
    private static PrimestoneMain instance = new PrimestoneMain();

    private PrimestoneMain() {
        id = "primestone.main";

        addressHeader = 56;
        p2shHeader = 118;
        acceptableAddressCodes = new int[]{this.addressHeader, this.p2shHeader};
        spendableCoinbaseDepth = 520;
        dumpedPrivateKeyHeader = 188;

        name = "Primestone";
        symbol = "PSC";
        uriScheme = "primestone";
        bip44Index = 119;
        unitExponent = 8;
        feeValue = value(10000);
        minNonDust = value(1);
        softDustLimit = value(10000);
        softDustPolicy = SoftDustPolicy.BASE_FEE_FOR_EACH_SOFT_DUST_TXO;
        signedMessageHeader = CoinType.toBytes("primestone signed Message:\n");
    }

    public static synchronized PrimestoneMain get() {
        PrimestoneMain primestoneMain;
        synchronized (PrimestoneMain.class) {
            primestoneMain = instance;
        }
        return primestoneMain;
    }
}
