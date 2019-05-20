package com.primestone.wallet.util;

import android.content.Context;
import android.text.TextUtils;

public class SystemUtils {
    public static boolean isStoreVersion(Context context) {
        try {
            return !TextUtils.isEmpty(context.getPackageManager().getInstallerPackageName(context.getPackageName()));
        } catch (Throwable th) {
            return false;
        }
    }
}
