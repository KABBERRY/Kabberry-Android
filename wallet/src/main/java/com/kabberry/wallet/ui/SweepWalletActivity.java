package com.kabberry.wallet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.MenuItem;

import com.coinomi.core.wallet.SendRequest;

import com.kabberry.wallet.R;
import com.kabberry.wallet.ExchangeHistoryProvider.ExchangeEntry;
import com.kabberry.wallet.WalletApplication;
import com.kabberry.wallet.ui.MakeTransactionFragment.Listener;
import com.kabberry.wallet.util.TypefaceSpan;

public class SweepWalletActivity extends BaseWalletActivity implements Listener, SweepWalletFragment.Listener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_fragment_wrapper);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        if (savedInstanceState == null) {
            Fragment fragment = SweepWalletFragment.newInstance();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add((int) R.id.container, fragment)
                    .commit();
        }

        Spannable custom_title = new SpannableString(getString(R.string.title_activity_sweep_wallet));
        custom_title.setSpan(new TypefaceSpan(this, "Roboto-Regular.ttf"), 0, custom_title.length(), 33);
        getSupportActionBar().setTitle(custom_title);
    }

    protected void onResume() {
        super.onResume();
        WalletApplication.getInstance().stopLogoutTimer(getApplicationContext());
    }

    protected void onPause() {
        super.onPause();
        WalletApplication.getInstance().startLogoutTimer(getApplicationContext());
    }

    public void onSendTransaction(SendRequest request) {
        Bundle args = new Bundle();
        args.putSerializable("send_request", request);
        replaceFragment(MakeTransactionFragment.newInstance(args), R.id.container);
    }

    public void onSignResult(Exception error, ExchangeEntry exchange) {
        Intent result = new Intent();
        result.putExtra("error", error);
        setResult(-1, result);
        finish();
    }

    public void onBackPressed() {
        SweepWalletFragment.clearTasks();
        super.onBackPressed();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        onBackPressed();
        return true;
    }
}
