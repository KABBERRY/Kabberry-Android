package com.primestone.wallet.ui;

import android.content.Intent;
import android.os.Bundle;

@Deprecated
public class SendActivity extends BaseWalletActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, WalletActivity.class);
        intent.putExtra("test_wallet", getIntent().getDataString());
        startActivity(intent);
        finish();
    }
}
