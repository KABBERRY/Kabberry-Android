package com.primestone.wallet.ui;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;

import com.primestone.wallet.R;
import com.primestone.wallet.util.TypefaceSpan;

public class FeesSettingsActivity extends BaseWalletActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_fragment_wrapper);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add((int) R.id.container, new FeesSettingsFragment()).commit();
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        Spannable custom_title = new SpannableString(getString(R.string.title_activity_transaction_fees));
        custom_title.setSpan(new TypefaceSpan(this, "Roboto-Regular.ttf"), 0, custom_title.length(), 33);
        getSupportActionBar().setTitle(custom_title);
        getSupportActionBar().setElevation(0.0f);
    }
}
