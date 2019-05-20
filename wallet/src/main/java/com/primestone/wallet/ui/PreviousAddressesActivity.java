package com.primestone.wallet.ui;

import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.Menu;
import android.view.MenuItem;

import com.primestone.wallet.R;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.ui.PreviousAddressesFragment;
import com.primestone.wallet.util.TypefaceSpan;

public class PreviousAddressesActivity extends BaseWalletActivity implements
        PreviousAddressesFragment.Listener {
    private static final String LIST_ADDRESSES_TAG = "list_addresses_tag";
    private static final String ADDRESS_TAG = "address_tag";

    Toolbar my_toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_fragment_wrapper);

        my_toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(this.my_toolbar);
        if (VERSION.SDK_INT > 21) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setHomeAsUpIndicator((int) R.drawable.ic_back);

        if (savedInstanceState == null) {
            PreviousAddressesFragment addressesList = new PreviousAddressesFragment();
            addressesList.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, addressesList, LIST_ADDRESSES_TAG)
                    .commit();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
            Spannable custom_title = new SpannableString(getString(R.string.title_activity_previous_addresses));
            custom_title.setSpan(new TypefaceSpan(this, "Roboto-Regular.ttf"), 0, custom_title.length(), 33);
            getSupportActionBar().setTitle(custom_title);
            getSupportActionBar().setElevation(0.0f);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getFM().findFragmentByTag(LIST_ADDRESSES_TAG).isVisible()) {
                    finish();
                    return true;
                } else {
                    getSupportFragmentManager().popBackStack();
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Fragment f = getFM().findFragmentByTag(ADDRESS_TAG);
        if (f == null || !f.isVisible()) {
            return super.onCreateOptionsMenu(menu);
        }
        getMenuInflater().inflate(R.menu.request_single_address, menu);
        return true;
    }

    @Override
    public void onAddressSelected(Bundle args) {
        replaceFragment(AddressRequestFragment.newInstance(args), R.id.container, ADDRESS_TAG);
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
