package com.primestone.wallet.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;

import com.coinomi.core.wallet.WalletAccount;

import com.primestone.wallet.R;
import com.primestone.wallet.WalletApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class HistoryFragment extends WalletFragment {
    private static final Logger log = LoggerFactory.getLogger(HistoryFragment.class);

    private WalletAccount account;
    private WalletApplication application;
    private int currentScreen;

    private Listener listener;

    @Bind(R.id.pager_title_strip) TabLayout pager_title_strip;
    @Bind(R.id.pager) ViewPager viewPager;

    class C11371 implements OnPageChangeListener {
        C11371() {
        }

        @Override
        public void onPageSelected(int position) {
            HistoryFragment.this.currentScreen = position;
            if (HistoryFragment.this.listener != null) {
                switch (position) {
                    case 0:
                        listener.onReceiveSelected();
                        break;
                    case 1:
                        listener.onSendSelected();
                        break;
                    default:
                        throw new RuntimeException("Unknown screen item: " + position);
                }
            }
            View view = HistoryFragment.this.getActivity().getCurrentFocus();
            if (view != null) {
                ((InputMethodManager) HistoryFragment.this.getActivity().getSystemService("input_method")).hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }

        public void onPageScrolled(int pos, float posOffset, int posOffsetPixels) {
        }

        public void onPageScrollStateChanged(int state) {
        }
    }

    private static class AppSectionsPagerAdapter extends FragmentStatePagerAdapter {
        private WalletAccount account;
        private final String receiveTitle;
        private BalanceListReceive request;
        private BalanceListSend send;
        private final String sendTitle;

        public AppSectionsPagerAdapter(Context context, FragmentManager fm, WalletAccount account) {
            super(fm);
            this.receiveTitle = context.getString(R.string.wallet_title_request);
            this.sendTitle = context.getString(R.string.wallet_title_send);
            this.account = account;
        }

        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    if (this.request == null) {
                        this.request = (BalanceListReceive) HistoryFragment.createFragment(this.account, i);
                    }
                    return this.request;
                case 1:
                    if (this.send == null) {
                        this.send = (BalanceListSend) HistoryFragment.createFragment(this.account, i);
                    }
                    return this.send;
                default:
                    throw new RuntimeException("Cannot get item, unknown screen item: " + i);
            }
        }

        public int getCount() {
            return 2;
        }

        public CharSequence getPageTitle(int i) {
            switch (i) {
                case 0:
                    return this.receiveTitle;
                case 1:
                    return this.sendTitle;
                default:
                    throw new RuntimeException("Cannot get item, unknown screen item: " + i);
            }
        }
    }

    public interface Listener extends BalanceListReceive.Listener, BalanceListSend.Listener {
        void onReceiveSelected();
        void onSendSelected();
    }

    public static HistoryFragment newInstance(String accountId) {
        HistoryFragment fragment = new HistoryFragment();
        Bundle args = new Bundle();
        args.putSerializable("account_id", accountId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        this.account = this.application.getAccount(getArguments().getString("account_id"));
    }

    @TargetApi(21)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        ButterKnife.bind((Object) this, view);
        setAllTypefaceThin(view);
        WalletActivity.toolbar_title.setText(getString(R.string.action_balance));
        viewPager.setOffscreenPageLimit(0);
        viewPager.addOnPageChangeListener(new C11371());
        viewPager.setAdapter(new AppSectionsPagerAdapter(getActivity(), getChildFragmentManager(), account));
        pager_title_strip.setupWithViewPager(viewPager);
        createTabIcons();
        return view;
    }

    private void createTabIcons() {
        TextView tab0 = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.custom_tab_his, null);
        tab0.setText(getString(R.string.received));
        tab0.setTextColor(Color.parseColor("#186eed"));
        setAllTypefacebold(tab0);
        pager_title_strip.getTabAt(0).setCustomView(tab0);

        TextView tab1 = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.custom_tab_his, null);
        tab1.setText(getString(R.string.sent));
        tab1.setTextColor(Color.parseColor("#8ab71b"));
        setAllTypefacebold(tab1);
        pager_title_strip.getTabAt(1).setCustomView(tab1);
    }

    @Override
    public void onDestroyView() {
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.listener = (Listener) context;
            this.application = (WalletApplication) context.getApplicationContext();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + Listener.class);
        }
    }

    @Override
    public void onDetach() {
        this.listener = null;
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("account_current_screen", this.currentScreen);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        updateView();
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        switch (this.viewPager.getCurrentItem()) {
            case 0:
                inflater.inflate(R.menu.send, menu);
                return;
            case 1:
                inflater.inflate(R.menu.send, menu);
                return;
            default:
                return;
        }
    }

    public void updateView() {
        goToItem(this.currentScreen, true);
    }

    private static <T extends Fragment> T createFragment(WalletAccount account, int item) {
        String accountId = account.getId();
        switch (item) {
            case 0:
                return (T) BalanceListReceive.newInstance(accountId);
            case 1:
                return (T) BalanceListSend.newInstance(accountId);
            default:
                throw new RuntimeException("Cannot create fragment, unknown screen item: " + item);
        }
    }

    private boolean goToItem(int item, boolean smoothScroll) {
        if (this.viewPager == null || this.viewPager.getCurrentItem() == item) {
            return false;
        }
        this.viewPager.setCurrentItem(item, smoothScroll);
        return true;
    }

    protected void setAllTypefaceThin(View view) {
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getChildCount() != 0) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllTypefaceThin(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Regular.ttf"));
        }
    }

    protected void setAllTypefacebold(View view) {
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getChildCount() != 0) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllTypefacebold(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Bold.ttf"));
        }
    }
}
