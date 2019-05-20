package com.primestone.wallet.ui;

import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.MenuItem;

import com.primestone.wallet.R;
import com.primestone.wallet.ExchangeHistoryProvider.ExchangeEntry;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.ui.MakeTransactionFragment.Listener;
import com.primestone.wallet.util.TypefaceSpan;

public class SignTransactionActivity extends AbstractWalletFragmentActivity implements Listener,
        TradeStatusFragment.Listener {
    Toolbar my_toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_fragment_wrapper);

        my_toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(my_toolbar);
        if (VERSION.SDK_INT > 21) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setHomeAsUpIndicator((int) R.drawable.ic_back);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add((int) R.id.container,
                    MakeTransactionFragment.newInstance(getIntent().getExtras())).commit();
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        Spannable custom_title = new SpannableString(getString(R.string.app_name));
        custom_title.setSpan(new TypefaceSpan(this, "ChamsRegular.ttf"), 0,
                custom_title.length(), 33);
        getSupportActionBar().setTitle(custom_title);
    }

    @Override
    public void onSignResult(Exception error, ExchangeEntry exchange) {
        Intent result = new Intent();
        result.putExtra("error", error);
        result.putExtra("exchange_entry", exchange);
        setResult(-1, result);
        if (error != null || exchange == null) {
            finish();
            return;
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.container, TradeStatusFragment.newInstance(exchange, true));
        transaction.commit();
    }

    @Override
    public void onFinish() {
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        WalletApplication.getInstance().stopLogoutTimer(getApplicationContext());
    }

    @Override
    protected void onPause() {
        super.onPause();
        WalletApplication.getInstance().startLogoutTimer(getApplicationContext());
    }
}
