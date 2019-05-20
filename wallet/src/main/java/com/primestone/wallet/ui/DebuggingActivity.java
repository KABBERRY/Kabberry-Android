package com.primestone.wallet.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;

import com.primestone.wallet.R;
import com.primestone.wallet.ui.UnlockWalletDialog.Listener;
import com.primestone.wallet.util.TypefaceSpan;

import javax.annotation.Nullable;

// implements UnlockWalletDialog.Listener - crownpay wallet
public class DebuggingActivity extends BaseWalletActivity implements UnlockWalletDialog.Listener {

    private static final String DEBUGGING_TAG = "debugging_tag";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_fragment_wrapper);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DebuggingFragment(), DEBUGGING_TAG)
                    .commit();
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        Spannable custom_title = new SpannableString(getString(R.string.title_activity_debugging));
        custom_title.setSpan(new TypefaceSpan(this, "Roboto-Regular.ttf"), 0, custom_title.length(), 33);
        getSupportActionBar().setTitle(custom_title);
        getSupportActionBar().setElevation(0.0f);
    }

    @Override
    public void onPassword(CharSequence password) {
        Fragment f = getFM().findFragmentByTag(DEBUGGING_TAG);
        if (f != null && (f instanceof DebuggingFragment)) {
            ((DebuggingFragment) f).setPassword(password);
        }
    }
}
