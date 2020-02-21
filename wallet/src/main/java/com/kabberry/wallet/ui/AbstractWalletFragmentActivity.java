package com.kabberry.wallet.ui;

import android.support.v7.app.AppCompatActivity;
import com.kabberry.wallet.WalletApplication;

public abstract class AbstractWalletFragmentActivity extends AppCompatActivity {
    protected WalletApplication getWalletApplication() {
        return (WalletApplication) getApplication();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWalletApplication().touchLastResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        getWalletApplication().touchLastStop();
    }
}

// Signed