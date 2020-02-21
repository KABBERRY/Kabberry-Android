package com.kabberry.wallet.util;

import android.content.Context;
import android.text.format.DateUtils;
import android.text.format.Time;

public class TimeUtils {
    private static Time nowTime = new Time();
    private static Time thenTime = new Time();

    public static CharSequence toRelativeTimeString(long seconds) {
        CharSequence relativeTimeSpanString;
        long now = System.currentTimeMillis();
        long millis = seconds * 1000;
        synchronized (TimeUtils.class) {
            nowTime.set(now);
            thenTime.set(millis);
            if (nowTime.year == thenTime.year) {
                relativeTimeSpanString = DateUtils.getRelativeTimeSpanString(millis, now, 60000, 0);
            } else {
                relativeTimeSpanString = DateUtils.getRelativeTimeSpanString(millis, now, 60000, 524288);
            }
        }
        return relativeTimeSpanString;
    }

    public static CharSequence toTimeString(Context context, long seconds) {
        return DateUtils.formatDateTime(context, 1000 * seconds, 21);
    }
}
