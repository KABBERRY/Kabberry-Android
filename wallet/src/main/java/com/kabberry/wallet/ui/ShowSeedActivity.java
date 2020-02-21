package com.kabberry.wallet.ui;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Typeface;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.ImageView;

import com.kabberry.wallet.R;
import com.kabberry.wallet.WalletApplication;
import com.kabberry.wallet.ui.ShowSeedFragment.Listener;
import com.kabberry.wallet.util.TypefaceSpan;

public class ShowSeedActivity extends BaseWalletActivity implements Listener {
    Toolbar my_toolbar;
    public static TextView toolbar_title;

    class C12481 implements OnClickListener {
        C12481() {
        }

        public void onClick(DialogInterface dialog, int which) {
            ShowSeedActivity.this.finish();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_fragment_wrapper);
        my_toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        //
//        toolbar_title = (TextView) my_toolbar.findViewById(R.id.toolbar_title);
//        toolbar_title.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf"));
//        toolbar_title.setTypeface(null, Typeface.ITALIC);
//        toolbar_title.setText(R.string.seed_title);
        //
        setSupportActionBar(my_toolbar);

        if (VERSION.SDK_INT > 21) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setHomeAsUpIndicator((int) R.drawable.ic_back);

//        ImageView imageView = (ImageView)my_toolbar.findViewById(R.id.toolbar_icon);
//        imageView.setImageResource(R.drawable.ic_back);
//        my_toolbar.setNavigationIcon(R.drawable.ic_back);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, new ShowSeedFragment(), "show_seed_tag")
                    .commit();
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        Spannable custom_title = new SpannableString(getString(R.string.seed_title));
        custom_title.setSpan(new TypefaceSpan(this, "Roboto-Regular.ttf"), 0, custom_title.length(), 33);
        getSupportActionBar().setTitle(custom_title);
        getSupportActionBar().setElevation(0.0f);
    }

    public void onSeedNotAvailable() {
        DialogBuilder
                .warn(this, R.string.seed_not_available_title)
                .setMessage((int) R.string.seed_not_available)
                .setPositiveButton((int) R.string.button_ok, new C12481())
                .create()
                .show();
    }

    public void onPassword(CharSequence password) {
        ShowSeedFragment f = (ShowSeedFragment) getFM().findFragmentByTag("show_seed_tag");
        if (f != null) {
            f.setPassword(password);
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_seed, menu);
        return super.onCreateOptionsMenu(menu);
    }
}
