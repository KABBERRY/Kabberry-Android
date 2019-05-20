package com.primestone.wallet.ui;

import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.graphics.Typeface;
import android.text.style.StyleSpan;
import android.view.MenuItem;

import com.primestone.wallet.R;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.util.TypefaceSpan;

public class TransactionDetailsActivity extends BaseWalletActivity {
    Toolbar my_toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_fragment_wrapper);
        this.my_toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(this.my_toolbar);

        if (VERSION.SDK_INT > 21) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setHomeAsUpIndicator((int) R.drawable.ic_back);

        if (savedInstanceState == null) {
            Fragment fragment = new TransactionDetailsFragment();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add((int) R.id.container, fragment).commit();
        }

        Spannable custom_title = new SpannableString(getString(R.string.title_activity_transaction_details));
        custom_title.setSpan(new TypefaceSpan(this, "Roboto-Regular.ttf"), 0, custom_title.length(), 33);
        custom_title.setSpan(new StyleSpan(Typeface.ITALIC), 0, custom_title.length(), 0);
        getSupportActionBar().setTitle(custom_title);
    }

    @Override
    protected void onResume() {
        super.onResume();
        WalletApplication.getInstance().stopLogoutTimer(getApplicationContext());
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        WalletApplication.getInstance().startLogoutTimer(getApplicationContext());
        super.onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        WalletApplication.getInstance().startLogoutTimer(getApplicationContext());
        super.onBackPressed();
    }
}
