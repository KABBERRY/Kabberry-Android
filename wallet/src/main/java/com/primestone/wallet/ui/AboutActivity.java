package com.primestone.wallet.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.OnClick;

import com.primestone.wallet.R;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.util.Fonts;

import org.w3c.dom.Text;

public class AboutActivity extends BaseWalletActivity {
    @TargetApi(21)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView title_view;
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);

        setAllTypefaceThin(findViewById(R.id.root_layout));
        setAllTypefaceBold(findViewById(R.id.tv_title_back));
        setAllTypefaceBold(findViewById(R.id.tv_title));
        title_view = (TextView) findViewById(R.id.tv_title_back);
        title_view.setTypeface(null, Typeface.ITALIC);
        //setAllTypefaceBold(findViewById(R.id.rate_app));

        if (VERSION.SDK_INT > 21) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary_900));
        }
    }

    @OnClick(R.id.tv_tos)
    void TOCclick(TextView textView) {
        Intent i = new Intent("android.intent.action.VIEW");
        i.setData(Uri.parse("https://primestone.global/terms"));
        startActivity(i);
    }

    @OnClick(R.id.tv_title_back)
    void backClick(TextView textView) {
        super.onBackPressed();
    }

    @OnClick(R.id.tv_pnp)
    void PNPclick(TextView textView) {
        Intent i = new Intent("android.intent.action.VIEW");
        i.setData(Uri.parse("https://primestone.global/privacy"));
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        WalletApplication.getInstance().stopLogoutTimer(getApplicationContext());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (getWalletApplication().getWallet() != null) {
            WalletApplication.getInstance().startLogoutTimer(getApplicationContext());
        }
    }

    public void setAllTypefaceThin(View view) {
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getChildCount() != 0) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllTypefaceThin(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf"));
        }
    }

    public void setAllTypefaceBold(View view) {
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getChildCount() != 0) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllTypefaceBold(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf"));
        }
    }
}

// Signed