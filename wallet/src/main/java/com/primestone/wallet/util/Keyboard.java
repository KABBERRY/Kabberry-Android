package com.primestone.wallet.util;

import android.app.Activity;
import android.view.inputmethod.InputMethodManager;

public class Keyboard {
    public static void hideKeyboard(Activity activity) {
        InputMethodManager mgr = (InputMethodManager) activity.getSystemService("input_method");
        if (activity.getCurrentFocus() != null) {
            mgr.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }
}
