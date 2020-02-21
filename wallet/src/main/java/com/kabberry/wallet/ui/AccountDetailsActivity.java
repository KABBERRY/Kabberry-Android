package com.kabberry.wallet.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.MenuItem;

import com.kabberry.wallet.R;
import com.kabberry.wallet.WalletApplication;
import com.kabberry.wallet.ui.TradeStatusFragment.Listener;
import com.kabberry.wallet.util.TypefaceSpan;

public class AccountDetailsActivity extends BaseWalletActivity implements Listener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_fragment_wrapper);

        if (savedInstanceState == null) {
            Fragment fragment = new AccountDetailsFragment();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add((int) R.id.container, fragment).commit();
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        Spannable custom_title = new SpannableString(getString(R.string.title_activity_account_details));
        custom_title.setSpan(new TypefaceSpan(this, "Roboto-Regular.ttf"),
                0, custom_title.length(), 33);
        getSupportActionBar().setTitle(custom_title);
        getSupportActionBar().setElevation(0.0f);
    }

    @Override
    public void onFinish() {
        finish();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        super.onBackPressed();
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
