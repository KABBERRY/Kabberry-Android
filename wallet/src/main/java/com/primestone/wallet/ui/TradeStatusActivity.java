package com.primestone.wallet.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.primestone.wallet.R;
import com.primestone.wallet.ui.TradeStatusFragment.Listener;

public class TradeStatusActivity extends BaseWalletActivity implements Listener {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_fragment_wrapper);
        if (savedInstanceState == null) {
            Fragment fragment = new TradeStatusFragment();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add((int) R.id.container, fragment).commit();
        }
    }

    public void onFinish() {
        finish();
    }
}
