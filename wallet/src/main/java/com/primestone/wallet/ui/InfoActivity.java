package com.primestone.wallet.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.view.WindowManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.primestone.wallet.R;
import com.primestone.wallet.circleindicator.CircleIndicator;

import org.w3c.dom.Text;

public class InfoActivity extends BaseWalletActivity {
    public CircleIndicator indicator;
    ImageView iv_next;
    TextView tv_skip;
    TextView tv_title_star;
    TextView goto_wallet_title;
    ViewPager viewpager;

    class C11381 implements OnClickListener {
        C11381() {
        }

        public void onClick(View v) {
            Intent intent = new Intent(InfoActivity.this.getApplicationContext(), WalletActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            InfoActivity.this.startActivity(intent);
            InfoActivity.this.finish();
        }
    }

    class C11392 implements OnClickListener {
        C11392() {
        }

        public void onClick(View v) {
            if (viewpager.getCurrentItem() == 2) {
                Intent intent = new Intent(InfoActivity.this.getApplicationContext(), WalletActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                InfoActivity.this.startActivity(intent);
                InfoActivity.this.finish();
            } else if (viewpager.getCurrentItem() == 0) {
                tv_title_star.setText("Send");
                viewpager.setCurrentItem(1);
                goto_wallet_title.setVisibility(View.GONE);
            } else {
                tv_title_star.setText("Receive");
                viewpager.setCurrentItem(2);
                goto_wallet_title.setVisibility(View.VISIBLE);
            }
        }
    }

    public class InfoPagerAdapter extends FragmentStatePagerAdapter {
        int TabCount;

        public InfoPagerAdapter(FragmentManager fragmentManager, int CountTabs) {
            super(fragmentManager);
            this.TabCount = CountTabs;
        }

        public Fragment getItem(int position) {
            return InfoFrag.newInstance(position);
        }

        public int getCount() {
            return this.TabCount;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView((int) R.layout.activity_info);
        if (VERSION.SDK_INT > 21) {
//            getWindow().setStatusBarColor(getResources().getColor(R.color.white));
        }

        viewpager = (ViewPager) findViewById(R.id.viewpager);
        iv_next = (ImageView) findViewById(R.id.iv_next);
        tv_skip = (TextView) findViewById(R.id.tv_skip);
        tv_skip.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf"));
        indicator = (CircleIndicator) findViewById(R.id.indicator);
        viewpager.setAdapter(new InfoPagerAdapter(getSupportFragmentManager(), 3));
        indicator.setViewPager(this.viewpager);
        viewpager.setCurrentItem(0);
        tv_title_star = (TextView) findViewById(R.id.tv_title_star);
        tv_skip.setOnClickListener(new C11381());
        iv_next.setOnClickListener(new C11392());
        goto_wallet_title = (TextView) findViewById(R.id.goto_wallet);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWalletApplication().stopLogoutTimer(getApplicationContext());
    }
}
