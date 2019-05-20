package com.primestone.wallet.ui;

import android.os.Bundle;

import com.primestone.wallet.R;

public class ExchangeHistoryActivity extends BaseWalletActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_fragment_wrapper);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add((int) R.id.container, new ExchangeHistoryFragment()).commit();
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
    }
}
