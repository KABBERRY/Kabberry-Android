package com.kabberry.wallet.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TableLayout.LayoutParams;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;

import com.coinomi.core.uri.CoinURI;
import com.coinomi.core.uri.CoinURIParseException;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount;

import com.kabberry.wallet.R;
import com.kabberry.wallet.WalletApplication;
import com.kabberry.wallet.ui.AddressRequestFragment;
import com.kabberry.wallet.ui.BalanceFragment;
import com.kabberry.wallet.ui.HistoryFragment;
import com.kabberry.wallet.ui.ScannerView;
import com.kabberry.wallet.ui.widget.MDToast;
import com.kabberry.wallet.util.Keyboard;
import com.kabberry.wallet.util.UiUtils;
import com.kabberry.wallet.util.WeakHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class AccountFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(AccountFragment.class);

    public static ScannerView scannerView = null;
    public static SurfaceView surfaceView = null;
    private WalletAccount account;
    private WalletApplication application;
    private int currentScreen;
    private final MyHandler handler = new MyHandler(this);
    private boolean hasFlashLight = false;
    private Listener listener;

    @Bind(R.id.pager_title_strip) TabLayout pager_title_strip;
    @Bind(R.id.pager) ViewPager viewPager;

    /*
    class C10901 implements OnPageChangeListener {
        C10901() {
        }


    }
*/
    private static class AppSectionsPagerAdapter extends FragmentStatePagerAdapter {
        private final String HistoryTitle;
        private final String SettingsTitle;
        private WalletAccount account;
        private BalanceFragment balance;
        private final String balanceTitle;
        private HistoryFragment history;
        private final String receiveTitle;
        private AddressRequestFragment request;
        private SendFragment send;
        private final String sendTitle;
        private SettingsFragment settings;

        public AppSectionsPagerAdapter(Context context, FragmentManager fm, WalletAccount account) {
            super(fm);
            receiveTitle = context.getString(R.string.wallet_title_request);
            sendTitle = context.getString(R.string.wallet_title_send);
            balanceTitle = context.getString(R.string.wallet_title_balance);
            HistoryTitle = context.getString(R.string.action_history);
            SettingsTitle = context.getString(R.string.action_settings);
            this.account = account;
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    if (history == null) {
                        history = (HistoryFragment) AccountFragment.createFragment(account, i);
                    }
                    return history;
                case 1:
                    if (request == null) {
                        request = (AddressRequestFragment) AccountFragment.createFragment(account, i);
                    }
                    return request;
                case 2:
                    if (balance == null) {
                        balance = (BalanceFragment) AccountFragment.createFragment(account, i);
                    }
                    return balance;
                case 3:
                    if (send == null) {
                        send = (SendFragment) AccountFragment.createFragment(account, i);
                    }
                    return send;
                case 4:
                    if (settings == null) {
                        settings = (SettingsFragment) AccountFragment.createFragment(account, i);
                    }
                    return settings;
                default:
                    throw new RuntimeException("Cannot get item, unknown screen item: " + i);
            }
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int i) {
            switch (i) {
                case 0:
                    return HistoryTitle;
                case 1:
                    return receiveTitle;
                case 2:
                    return balanceTitle;
                case 3:
                    return sendTitle;
                case 4:
                    return SettingsTitle;
                default:
                    throw new RuntimeException("Cannot get item, unknown screen item: " + i);
            }
        }
    }

    public interface Listener extends com.kabberry.wallet.ui.BalanceFragment.Listener,
            com.kabberry.wallet.ui.SendFragment.Listener {
        void registerActionMode(ActionMode actionMode);

        void onBalanceSelected();

        void onReceiveSelected();

        void onScanSelected();      // History

        void onSendSelected();

        void onSettingSelected();
    }

    private static class MyHandler extends WeakHandler<AccountFragment> {
        public MyHandler(AccountFragment ref) {
            super(ref);
        }

        protected void weakHandleMessage(AccountFragment ref, Message msg) {
            switch (msg.what) {
                case 0:
                    ref.setSendToUri((CoinURI) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    public static AccountFragment getInstance() {
        AccountFragment fragment = new AccountFragment();
        fragment.setArguments(new Bundle());
        return fragment;
    }

    public static AccountFragment getInstance(String accountId) {
        AccountFragment fragment = getInstance();
        fragment.setupArgs(accountId);
        return fragment;
    }

    private void setupArgs(String accountId) {
        getArguments().putString("account_id", accountId);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        hasFlashLight = getActivity().getPackageManager().hasSystemFeature("android.hardware.camera.flash");
        account = application.getAccount(getArguments().getString("account_id"));
    }

    @TargetApi(21)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);
        ButterKnife.bind((Object) this, view);

        setAllTypefaceThin(view);
        WalletActivity.toolbar_title.setText(getString(R.string.wallet_title_balance));
        viewPager.setOffscreenPageLimit(2);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                AccountFragment.this.currentScreen = position;
                if (position == 2) {
                    Keyboard.hideKeyboard(AccountFragment.this.getActivity());
                }

                if (AccountFragment.this.listener != null) {
                    switch (position) {
                        case 0:
                            listener.onScanSelected();
                            WalletActivity.toolbar_title.setText(AccountFragment.this.getString(R.string.action_history));
                            WalletActivity.toolbar_title.setTypeface(null, Typeface.ITALIC);
                            break;
                        case 1:
                            listener.onReceiveSelected();
                            WalletActivity.toolbar_title.setText(AccountFragment.this.getString(R.string.action_request));
                            WalletActivity.toolbar_title.setTypeface(null, Typeface.ITALIC);
                            break;
                        case 2:
                            listener.onBalanceSelected();
                            WalletActivity.toolbar_title.setText(AccountFragment.this.getString(R.string.action_balance));
                            WalletActivity.toolbar_title.setTypeface(null, Typeface.ITALIC);
                            break;
                        case 3:
                            listener.onSendSelected();
                            WalletActivity.toolbar_title.setText(AccountFragment.this.getString(R.string.action_send));
                            WalletActivity.toolbar_title.setTypeface(null, Typeface.ITALIC);
                            break;
                        case 4:
                            listener.onSettingSelected();
                            WalletActivity.toolbar_title.setText(AccountFragment.this.getString(R.string.action_settings));
                            WalletActivity.toolbar_title.setTypeface(null, Typeface.ITALIC);
                            break;
                        default:
                            throw new RuntimeException("Unknown screen item: " + position);
                    }
                }

                View view = AccountFragment.this.getActivity().getCurrentFocus();
                if (view != null) {
                    ((InputMethodManager) AccountFragment.this.getActivity()
                            .getSystemService("input_method"))
                            .hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }

            @Override
            public void onPageScrolled(int pos, float posOffset, int posOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        viewPager.setAdapter(new AppSectionsPagerAdapter(getActivity(), getChildFragmentManager(),
                account));
        pager_title_strip.setupWithViewPager(viewPager);
        createTabIcons();

        // History tab border color
        View mainTab = ((ViewGroup) pager_title_strip.getChildAt(0)).getChildAt(0);
        mainTab.setBackgroundResource(R.drawable.rv_history_tab_border);

        // Request tab border color
        mainTab = ((ViewGroup) pager_title_strip.getChildAt(0)).getChildAt(1);
        mainTab.setBackgroundResource(R.drawable.rv_req_tab_border);

        // Balance tab border color
        mainTab = ((ViewGroup) pager_title_strip.getChildAt(0)).getChildAt(2);
        mainTab.setBackgroundResource(R.drawable.rv_bal_tab_border);

        // Send tab border color
        mainTab = ((ViewGroup) pager_title_strip.getChildAt(0)).getChildAt(3);
        mainTab.setBackgroundResource(R.drawable.rv_send_tab_border);

        // Settings tab border color
        mainTab = ((ViewGroup) pager_title_strip.getChildAt(0)).getChildAt(4);
        mainTab.setBackgroundResource(R.drawable.rv_settings_tab_border);

        return view;
    }

    private void createTabIcons() {
        TextView tab0 = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.custom_tab, null);
        tab0.setText(getString(R.string.wallet_title_history));
        setAllTypefaceThin(tab0);
        tab0.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.tab_his_selector, 0, 0);
        pager_title_strip.getTabAt(0).setCustomView(tab0);

        TextView tab1 = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.custom_tab, null);
        tab1.setText(getString(R.string.wallet_title_request));
        setAllTypefaceThin(tab1);
        tab1.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.tab_rec_selector, 0, 0);
        pager_title_strip.getTabAt(1).setCustomView(tab1);

        TextView tab3 = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.custom_tab, null);
        tab3.setText(getString(R.string.wallet_title_send));
        setAllTypefaceThin(tab3);
        tab3.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.tab_send_selector, 0, 0);
        pager_title_strip.getTabAt(3).setCustomView(tab3);

        TextView tab4 = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.custom_tab, null);
        tab4.setText(getString(R.string.title_activity_settings));
        setAllTypefaceThin(tab4);
        tab4.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.tab_setting_selector, 0, 0);
        pager_title_strip.getTabAt(4).setCustomView(tab4);

        View tabView = LayoutInflater.from(getActivity()).inflate(R.layout.custom_bal_tab, null);
        tabView.setLayoutParams(new LayoutParams(-1, -1));
        tabView.setPadding(0, 0, 0, 0);
        tabView.setBackgroundResource(R.drawable.rv_bal_tab_border);

        TextView tab2 = (TextView) tabView.findViewById(R.id.title);
        tab2.setText(getString(R.string.wallet_title_balance));
        setAllTypefaceThin(tab2);
        tab2.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_home_checked, 0, 0);
        this.pager_title_strip.getTabAt(2).setCustomView(tabView);
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
        listener = null;
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("account_current_screen", currentScreen);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            currentScreen = savedInstanceState.getInt("account_current_screen", 2);
        } else {
            this.currentScreen = 2;
        }
        updateView();
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        switch (viewPager.getCurrentItem()) {
            case 0:
                if (hasFlashLight) {
                    inflater.inflate(R.menu.global, menu);
                    return;
                }
                return;
            case 1:
                inflater.inflate(R.menu.request, menu);
                return;
            case 2:
                inflater.inflate(R.menu.global, menu);
                return;
            case 3:
                inflater.inflate(R.menu.send, menu);
                return;
            case 4:
                inflater.inflate(R.menu.global, menu);
                return;
            default:
                return;
        }
    }

    private void updateView() {
        goToItem(currentScreen, true);
    }

    @Nullable
    public WalletAccount getAccount() {
        return account;
    }

    public void sendToUri(CoinURI coinUri) {
        if (viewPager != null) {
            viewPager.setCurrentItem(3);
            handler.sendMessage(this.handler.obtainMessage(0, coinUri));
            return;
        } else {
            // should not happen
            UiUtils.toastGenericError(getContext());
        }
    }

    private void setSendToUri(CoinURI coinURI) {
        if (viewPager != null) {
            viewPager.setCurrentItem(3);
        }

        SendFragment f = getSendFragment();

        if (f != null) {
            try {
                f.updateStateFrom(coinURI);
            } catch (CoinURIParseException e) {
                MDToast.makeText(getContext(), getString(R.string.scan_error, e.getMessage()), MDToast.LENGTH_LONG, 3);
            }
        } else {
            log.warn("Expected fragment to be not null");
            UiUtils.toastGenericError(getContext());
        }
    }

    @Nullable
    private SendFragment getSendFragment() {
        return (SendFragment) getFragment(getChildFragmentManager(), 3);
    }

    @Nullable
    private static Fragment getFragment(FragmentManager fm, int item) {
        if (fm.getFragments() == null) {
            return null;
        }

        for (Fragment f : fm.getFragments()) {
            switch (item) {
                case 0:
                    if (!(f instanceof HistoryFragment)) {
                        break;
                    }
                    return f;
                case 1:
                    if (!(f instanceof AddressRequestFragment)) {
                        break;
                    }
                    return f;
                case 2:
                    if (!(f instanceof BalanceFragment)) {
                        break;
                    }
                    return f;
                case 3:
                    if (!(f instanceof SendFragment)) {
                        break;
                    }
                    return f;
                case 4:
                    if (!(f instanceof SettingsFragment)) {
                        break;
                    }
                    return f;
                default:
                    throw new RuntimeException("Cannot get fragment, unknown screen item: " + item);
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked"})
    private static <T extends Fragment> T createFragment(WalletAccount account, int item) {
        String accountId = account.getId();

        switch (item) {
            case 0:
                return (T) HistoryFragment.newInstance(accountId);
            case 1:
                return (T) AddressRequestFragment.newInstance(accountId);
            case 2:
                return (T) BalanceFragment.newInstance(accountId);
            case 3:
                return (T) SendFragment.newInstance(accountId);
            case 4:
                return (T) SettingsFragment.newInstance(accountId);
            default:
                throw new RuntimeException("Cannot create fragment, unknown screen item: " + item);
        }
    }

    public boolean goToBalance(boolean smoothScroll) {
        return goToItem(2, smoothScroll);
    }

    public boolean goToSend(boolean smoothScroll) {
        return goToItem(3, smoothScroll);
    }

    private boolean goToItem(int item, boolean smoothScroll) {
        if (viewPager == null || viewPager.getCurrentItem() == item) {
            return false;
        }
        viewPager.setCurrentItem(item, smoothScroll);
        return true;
    }

    public boolean resetSend() {
        SendFragment f = getSendFragment();

        if (f == null) {
            return false;
        }
        f.reset();
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
}
