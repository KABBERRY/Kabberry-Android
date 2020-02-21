package com.kabberry.wallet.tasks;

import com.coinomi.core.exchange.shapeshift.ShapeShift;
import com.coinomi.core.exchange.shapeshift.data.ShapeShiftMarketInfo;

import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MarketInfoPollTask extends TimerTask {
    private static final Logger log = LoggerFactory.getLogger(MarketInfoPollTask.class);
    private String pair;
    private final ShapeShift shapeShift;

    public abstract void onHandleMarketInfo(ShapeShiftMarketInfo shapeShiftMarketInfo);

    public MarketInfoPollTask(ShapeShift shapeShift, String pair) {
        this.shapeShift = shapeShift;
        this.pair = pair;
    }

    public void updatePair(String newPair) {
        this.pair = newPair;
    }

    public void run() {
        ShapeShiftMarketInfo marketInfo = getMarketInfoSync(this.shapeShift, this.pair);
        if (marketInfo != null) {
            onHandleMarketInfo(marketInfo);
        }
    }

    public static ShapeShiftMarketInfo getMarketInfoSync(ShapeShift shapeShift, String pair) {
        int tries = 1;
        while (tries <= 3) {
            try {
                log.info("Polling market info for pair: {}", (Object) pair);
                return shapeShift.getMarketInfo(pair);
            } catch (Exception e) {
                log.info("Will retry: {}", e.getMessage());
                try {
                    Thread.sleep((long) (tries * 1000));
                } catch (InterruptedException e2) {
                }
                tries++;
            }
        }
        return null;
    }
}
