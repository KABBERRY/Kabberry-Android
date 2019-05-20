package com.primestone.wallet.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.exceptions.AddressMalformedException;
import com.coinomi.core.uri.CoinURI;
import com.coinomi.core.uri.CoinURIParseException;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.SerializedKey;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount;

import com.primestone.wallet.R;
import com.primestone.wallet.Constants;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.service.CoinService;
import com.primestone.wallet.service.CoinService.ServiceMode;
import com.primestone.wallet.service.CoinServiceImpl;
import com.primestone.wallet.tasks.CheckUpdateTask;
import com.primestone.wallet.ui.AccountFragment.Listener;
import com.primestone.wallet.util.SystemUtils;
import com.primestone.wallet.util.WeakHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mehdi.sakout.fancybuttons.FancyButton;
import org.bitcoinj.core.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final public class WalletActivity extends BaseWalletActivity implements Listener,
        HistoryFragment.Listener, OverviewFragment.Listener, PayWithDialog.Listener,
        SelectCoinTypeDialog.Listener, SettingsFragment.Listener {
    private static final Logger log = LoggerFactory.getLogger(WalletActivity.class);

    private static final int REQUEST_CODE_SCAN = 0;
    private static final int ADD_COIN = 1;

    private static final int TX_BROADCAST_OK = 0;
    private static final int TX_BROADCAST_ERROR = 1;
    private static final int SET_URI = 2;
    private static final int OPEN_ACCOUNT = 3;
    private static final int OPEN_OVERVIEW = 4;
    private static final int PROCESS_URI = 5;

    // Fragment tags
    private static final String ACCOUNT_TAG = "account_tag";
    private static final String OVERVIEW_TAG = "overview_tag";

    // Saved state variables
    private static final String OVERVIEW_VISIBLE = "overview_visible";

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence title;

    @Nullable private String lastAccountId;
    private Intent connectAllCoinIntent;
    private Intent connectCoinIntent;
    private ActionMode lastActionMode;
    private final Handler handler = new MyHandler(this);
    private boolean isOverviewVisible;
    private OverviewFragment overviewFragment;
    @Nullable private AccountFragment accountFragment;

    public static TextView toolbar_title;
    Boolean IsDialogOpen = false;
    public int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    Dialog dialog;
    private Toolbar my_toolbar;
    private Wallet wallet;

    public WalletActivity() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_new);

        my_toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        toolbar_title = (TextView) my_toolbar.findViewById(R.id.toolbar_title);
        toolbar_title.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf"));
        setSupportActionBar(my_toolbar);

        if (VERSION.SDK_INT > 21) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }

        if (getIntent().getAction() != null && "Primestone.LOGOUT".equals(getIntent().getAction())) {
            finish();
            System.exit(0);
        }

        if (getWalletApplication().getWallet() == null) {
            startIntro();
            finish();
            return;
        }

        if (!getIntent().hasExtra("fromPIN")) {
            startActivity(new Intent(getApplicationContext(), PinEntryActivity.class));
            finish();
        }

        lastAccountId = getWalletApplication().getConfiguration().getLastAccountId();

        try {
            wallet = ((WalletApplication) getApplicationContext()).getWallet();
            FragmentTransaction tr = getFM().beginTransaction();

            if (savedInstanceState == null) {
                checkAlerts();

                overviewFragment = OverviewFragment.getInstance();
                tr.add(R.id.contents, overviewFragment, OVERVIEW_TAG).hide(overviewFragment);

                List<WalletAccount> accounts = getAllAccounts();
                if (accounts.size() > 1) {
                    handler.sendMessage(handler.obtainMessage(OPEN_OVERVIEW));
                } else if (accounts.size() == 1) {
                    handler.sendMessage(handler.obtainMessage(OPEN_ACCOUNT, accounts.get(0)));
                }
            } else {
                isOverviewVisible = savedInstanceState.getBoolean(OVERVIEW_VISIBLE);
                overviewFragment = (OverviewFragment) getFM().findFragmentByTag(OVERVIEW_TAG);
                accountFragment = (AccountFragment) getFM().findFragmentByTag(ACCOUNT_TAG);

                if (isOverviewVisible || accountFragment == null) {
                    tr.show(overviewFragment);
                    if (accountFragment != null) {
                        tr.hide(accountFragment);
                    }
                    setOverviewTitle();
                } else {
                    tr.show(accountFragment).hide(this.overviewFragment);
                    setAccountTitle(accountFragment.getAccount());
                }
            }

            tr.commit();
            if (getIntent().hasExtra("test_wallet")) {
                handler.sendMessage(handler.obtainMessage(PROCESS_URI, getIntent().getStringExtra("test_wallet")));
                getIntent().removeExtra("test_wallet");
            }
            if (VERSION.SDK_INT >= 23) {
                CheckRuntimePermission(this);
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(getApplicationContext() + " must implement ");
        }
    }

    private void setOverviewTitle() {
        title = getResources().getString(R.string.title_activity_overview);
    }

    private void setAccountTitle(@Nullable WalletAccount account) {
        if (account != null) {
            title = account.getDescriptionOrCoinName();
        } else {
            title = "";
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(OVERVIEW_VISIBLE, isOverviewVisible);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getWalletApplication().startBlockchainService(ServiceMode.CANCEL_COINS_RECEIVED);
        connectAllCoinService();
        getWalletApplication().stopLogoutTimer(getApplicationContext());
        if (getSupportActionBar() == null) {
            return;
        }

        if (isOverviewVisible) {
            getSupportActionBar().setElevation((float) getResources().getDimensionPixelSize(R.dimen.active_elevation));
        } else {
            getSupportActionBar().setElevation(0.0f);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        getWalletApplication().startLogoutTimer(getApplicationContext());
    }

    @Override
    public void onRefresh() {
        refreshWallet();
    }

    @Override
    public void onTransactionBroadcastSuccess(WalletAccount pocket, Transaction transaction) {
        handler.sendMessage(handler.obtainMessage(TX_BROADCAST_OK, transaction));
    }

    @Override
    public void onTransactionBroadcastFailure(WalletAccount pocket, Transaction transaction) {
        handler.sendMessage(handler.obtainMessage(TX_BROADCAST_ERROR, transaction));
    }

    @Override
    public void onAccountSelected(String accountId) {
        log.info("Coin selected {}", (Object) accountId);

        openAccount(accountId);
    }

    public void openOverview() {
        openOverview(true);
    }

    public void openOverview(boolean selectInNavDrawer) {
        if (!isOverviewVisible && !isFinishing()) {
            setOverviewTitle();
            FragmentTransaction ft = getFM().beginTransaction();
            ft.show(overviewFragment);

            if (accountFragment != null) {
                ft.hide(accountFragment);
            }

            ft.commit();
            isOverviewVisible = true;
            connectAllCoinService();

            if (getSupportActionBar() != null) {
                getSupportActionBar().setElevation((float) getResources().getDimensionPixelSize(R.dimen.active_elevation));
            }
        }
    }

    private void openAccount(WalletAccount account) {
        openAccount(account, true);
    }

    private void openAccount(String accountId) {
        openAccount(getAccount(accountId), true);
    }

    private void openAccount(WalletAccount account, boolean selectInNavDrawer) {
        if (account != null && !isFinishing() && !isAccountVisible(account)) {
            FragmentTransaction ft = getFM().beginTransaction();
            ft.hide(overviewFragment);
            if (accountFragment == null || !account.getId().equals(this.lastAccountId)) {
                lastAccountId = account.getId();

                if (accountFragment != null) {
                    ft.remove(accountFragment);
                }

                accountFragment = AccountFragment.getInstance(lastAccountId);
                ft.add(R.id.contents, accountFragment, "account_tag");
                getWalletApplication().getConfiguration().touchLastAccountId(lastAccountId);
            } else {
                ft.show(accountFragment);
            }
            ft.commit();
            setAccountTitle(account);
            isOverviewVisible = false;
            connectCoinService(lastAccountId);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setElevation(0.0f);
            }
        }
    }

    private boolean isAccountVisible(WalletAccount account) {
        return account != null && accountFragment != null && accountFragment.isVisible() &&
                account.equals(accountFragment.getAccount());
    }

    private void connectCoinService(String accountId) {
        if (connectCoinIntent == null) {
            connectCoinIntent = new Intent(CoinService.ACTION_CONNECT_COIN, null,
                    getWalletApplication(), CoinServiceImpl.class);
        }
        connectCoinIntent.putExtra("account_id", accountId);
        getWalletApplication().startService(connectCoinIntent);
    }

    private void connectAllCoinService() {
        if (connectAllCoinIntent == null) {
            connectAllCoinIntent = new Intent(CoinService.ACTION_CONNECT_ALL_COIN, null,
                    getWalletApplication(), CoinServiceImpl.class);
        }
        getWalletApplication().startService(connectAllCoinIntent);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setTitle((CharSequence) "");
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    private void checkAlerts() {
        if (!SystemUtils.isStoreVersion(this)) {
            final PackageInfo packageInfo = getWalletApplication().packageInfo();
            new CheckUpdateTask() {
                @Override
                protected void onPostExecute(Integer serverVersionCode) {
                    if (serverVersionCode != null && serverVersionCode.intValue() > packageInfo.versionCode) {
                        showUpdateDialog();
                    }
                }
            }.execute();
        }
    }

    private void showUpdateDialog() {
        final PackageManager pm = getPackageManager();
        final Intent marketIntent = new Intent("android.intent.action.VIEW", Uri.parse(String.format("market://details?id=%s", new Object[]{getPackageName()})));
        Builder builder = new Builder(this);
        builder.setTitle(R.string.wallet_update_title);
        builder.setMessage(R.string.wallet_update_message);

        if (pm.resolveActivity(marketIntent, 0) != null) {
            builder.setPositiveButton("Play Store", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    WalletActivity.this.startActivity(marketIntent);
                    WalletActivity.this.finish();
                }
            });
        }
        builder.setNegativeButton(R.string.button_dismiss, null);
        builder.create().show();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        handler.post(new Runnable() {
            public void run() {
                if (requestCode == 0) {
                    if (resultCode == -1) {
                        try {
                            WalletActivity.this.processInput(intent.getStringExtra("result"));
                        } catch (Exception e) {
                            WalletActivity.this.showScanFailedMessage(e);
                        }
                    }
                } else if (requestCode == 1 && resultCode == -1) {
                    WalletActivity.this.openAccount(intent.getStringExtra("account_id"));
                }
            }
        });
    }

    private void showScanFailedMessage(Exception e) {
        Toast.makeText(this, getResources().getString(R.string.scan_error, new Object[]{e.getMessage()}), 1).show();
    }

    private void processInput(String input) throws CoinURIParseException, AddressMalformedException {
        input = input.trim();
        try {
            processUri(input);
        } catch (CoinURIParseException e) {
            if (SerializedKey.isSerializedKey(input)) {
                sweepWallet(input);
            } else {
                processAddress(input);
            }
        }
    }

    private void processUri(String input) throws CoinURIParseException {
        CoinURI coinUri = new CoinURI(input);
        CoinType scannedType = coinUri.getTypeRequired();
        if (!Constants.SUPPORTED_COINS.contains(scannedType)) {
            throw new CoinURIParseException(getResources().getString(R.string.unsupported_coin,
                    new Object[]{scannedType.getName()}));
        } else if (accountFragment == null || !accountFragment.isVisible() ||
                accountFragment.getAccount() == null) {
            WalletAccount selectedAccount = null;
            List<WalletAccount> allAccounts = getAllAccounts();
            List<WalletAccount> sendFromAccounts = getAccounts(scannedType);
            if (sendFromAccounts.size() == 1) {
                selectedAccount = (WalletAccount) sendFromAccounts.get(0);
            } else if (allAccounts.size() == 1) {
                selectedAccount = (WalletAccount) allAccounts.get(0);
            }
            if (selectedAccount != null) {
                payWith(selectedAccount, coinUri);
            } else {
                showPayWithDialog(coinUri);
            }
        } else {
            payWith(accountFragment.getAccount(), coinUri);
        }
    }

    private void processAddress(String addressStr) throws CoinURIParseException, AddressMalformedException {
        List<CoinType> possibleTypes = GenericUtils.getPossibleTypes(addressStr);

        if (possibleTypes.size() == 1) {
            processUri(CoinURI.convertToCoinURI(((CoinType) possibleTypes.get(0)).
                    newAddress(addressStr), null, null, null));
        } else {
            List<WalletAccount> possibleAccounts = getAccounts(possibleTypes);
            AbstractAddress addressOfAccount = null;

            for (WalletAccount account : possibleAccounts) {
                AbstractAddress testAddress = account.getCoinType().newAddress(addressStr);
                if (account.isAddressMine(testAddress)) {
                    addressOfAccount = testAddress;
                    break;
                }
            }

            if (addressOfAccount != null) {
                // If address is from an account don't show a dialog.
                processUri(CoinURI.convertToCoinURI(addressOfAccount, null, null, null));
            } else {
                // As a last resort let the use choose the correct coin type
                showPayToDialog(addressStr);
            }
        }
    }

    public void showPayToDialog(String addressStr) {
        Dialogs.dismissAllowingStateLoss(getFM(), "pay_to_dialog_tag");
        SelectCoinTypeDialog.getInstance(addressStr).show(getFM(), "pay_to_dialog_tag");
    }

    @Override
    public void onAddressTypeSelected(AbstractAddress selectedAddress) {
        try {
            processUri(CoinURI.convertToCoinURI(selectedAddress, null, null, null));
        } catch (CoinURIParseException e) {
            showScanFailedMessage(e);
        }
    }

    private void showPayWithDialog(CoinURI uri) {
        Dialogs.dismissAllowingStateLoss(getFM(), "pay_with_dialog_tag");
        PayWithDialog.getInstance(uri).show(getFM(), "pay_with_dialog_tag");
    }

    @Override
    public void payWith(WalletAccount account, CoinURI coinUri) {
        openAccount(account);
        // Set url asynchronously as the account may need to open
        handler.sendMessage(handler.obtainMessage(2, coinUri));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.global, menu);
        restoreActionBar();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_scan_qr_code) {
            startActivityForResult(new Intent(this, ScanActivity.class), 0);
            return true;
        } else if (id == R.id.action_refresh_wallet) {
            refreshWallet();
            return true;
        } else if (id != R.id.action_account_details) {
            return super.onOptionsItemSelected(item);
        } else {
            accountDetails();
            return true;
        }
    }

    void accountDetails() {
        if (isAccountExists(lastAccountId)) {
            Intent intent = new Intent(this, AccountDetailsActivity.class);
            intent.putExtra("account_id", lastAccountId);
            startActivity(intent);
            return;
        }
        Toast.makeText(this, R.string.no_wallet_pocket_selected, 1).show();
    }

    void sweepWallet(@Nullable String key) {
        if (isAccountExists(lastAccountId)) {
            Intent intent = new Intent(this, SweepWalletActivity.class);
            intent.putExtra("account_id", lastAccountId);
            if (key != null) {
                intent.putExtra("private_key", key);
            }
            startActivity(intent);
            return;
        }
        Toast.makeText(this, R.string.no_wallet_pocket_selected, 1).show();
    }

    private void refreshWallet() {
        if (getWalletApplication().getWallet() != null) {
            Intent intent;
            if (isOverviewVisible) {
                intent = new Intent(CoinService.ACTION_RESET_WALLET, null, getWalletApplication(), CoinServiceImpl.class);
            } else {
                intent = new Intent(CoinService.ACTION_RESET_ACCOUNT, null, getWalletApplication(), CoinServiceImpl.class);
                intent.putExtra("account_id", lastAccountId);
            }
            getWalletApplication().startService(intent);
        }
    }

    private void startIntro() {
        startActivity(new Intent(this, IntroActivity.class));
    }

    @Override
    public void onBackPressed() {
        finishActionMode();
        List<WalletAccount> accounts = getAllAccounts();

        if (accounts.size() > 1) {
            if (isOverviewVisible) {
                BackPressed();
            } else if (!goToBalance()) {
                openOverview();
            }
        } else if (accounts.size() != 1) {
            BackPressed();
        } else if (accountFragment == null || !accountFragment.isVisible()) {
            openAccount((WalletAccount) accounts.get(0));
        } else if (!goToBalance()) {
            BackPressed();
        }
    }

    private boolean goToBalance() {
        return accountFragment != null && accountFragment.isVisible() && accountFragment.goToBalance(true);
    }

    private boolean goToSend() {
        return accountFragment != null && accountFragment.isVisible() && accountFragment.goToSend(true);
    }

    private boolean resetSend() {
        return accountFragment != null && accountFragment.isVisible() && accountFragment.resetSend();
    }

    @Override
    public void registerActionMode(ActionMode actionMode) {
        finishActionMode();
        lastActionMode = actionMode;
    }

    @Override
    public void onReceiveSelected() {
        //getSupportActionBar().setIcon(R.drawable.ic_rec_checked);
        View view = my_toolbar.findViewById(R.id.toolbar_view);
        view.setBackgroundResource(R.drawable.rv_toolbar_req_left_border);
        ImageView imageView = (ImageView)my_toolbar.findViewById(R.id.toolbar_icon);
        imageView.setImageResource(R.drawable.ic_rec_checked);
        finishActionMode();
    }

    @Override
    public void onBalanceSelected() {
        //getSupportActionBar().setIcon(R.drawable.ic_home_checked);
        View view = my_toolbar.findViewById(R.id.toolbar_view);
        view.setBackgroundResource(R.drawable.rv_toolbar_bal_left_border);
        ImageView imageView = (ImageView)my_toolbar.findViewById(R.id.toolbar_icon);
        imageView.setImageResource(R.drawable.ic_home_checked);
        finishActionMode();
    }


    @Override
    public void onSendSelected() {
        //getSupportActionBar().setIcon(R.drawable.ic_send_checked);
        View view = my_toolbar.findViewById(R.id.toolbar_view);
        view.setBackgroundResource(R.drawable.rv_toolbar_send_left_border);
        ImageView imageView = (ImageView)my_toolbar.findViewById(R.id.toolbar_icon);
        imageView.setImageResource(R.drawable.ic_send_checked);
        finishActionMode();
    }

    @Override
    public void onScanSelected() {
        //getSupportActionBar().setIcon(R.drawable.ic_his_checked);
        View view = my_toolbar.findViewById(R.id.toolbar_view);
        view.setBackgroundResource(R.drawable.rv_toolbar_his_left_border);
        ImageView imageView = (ImageView)my_toolbar.findViewById(R.id.toolbar_icon);
        imageView.setImageResource(R.drawable.ic_his_checked);
        finishActionMode();
    }

    @Override
    public void onSettingSelected() {
        //getSupportActionBar().setIcon(R.drawable.ic_setting_checked);
        View view = my_toolbar.findViewById(R.id.toolbar_view);
        view.setBackgroundResource(R.drawable.rv_toolbar_setting_left_border);
        ImageView imageView = (ImageView)my_toolbar.findViewById(R.id.toolbar_icon);
        imageView.setImageResource(R.drawable.ic_setting_checked);
        finishActionMode();
    }

    private void finishActionMode() {
        if (lastActionMode != null) {
            lastActionMode.finish();
            lastActionMode = null;
        }
    }

    @Override
    public void onAccountModified(WalletAccount account) {
    }

    public void Dologout() {
        getWalletApplication().clearApplicationData();
        Intent intent = new Intent(getApplicationContext(), IntroActivity.class);
        intent.addFlags(268533760);
        startActivity(intent);
    }

    @TargetApi(23)
    public void CheckRuntimePermission(Activity activity) {
        List<String> permissionsNeeded = new ArrayList();
        List<String> permissionsList = new ArrayList();

        if (!addPermission(permissionsList, "android.permission.ACCESS_NETWORK_STATE")) {
            permissionsNeeded.add("Acces Network State");
        }

        if (!addPermission(permissionsList, "android.permission.WRITE_EXTERNAL_STORAGE")) {
            permissionsNeeded.add("Write External Storage");
        }

        if (!addPermission(permissionsList, "android.permission.CAMERA")) {
            permissionsNeeded.add("Camera");
        }

        if (permissionsList.size() > 0) {
            activity.requestPermissions((String[]) permissionsList.toArray(new String[permissionsList.size()]), this.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        }
    }

    @TargetApi(23)
    public boolean addPermission(List<String> permissionsList, String permission) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) != 0) {
            permissionsList.add(permission);
            if (!shouldShowRequestPermissionRationale(permission)) {
                return false;
            }
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 124:
                Map<String, Integer> perms = new HashMap();
                perms.put("android.permission.ACCESS_NETWORK_STATE", Integer.valueOf(0));
                perms.put("android.permission.WRITE_EXTERNAL_STORAGE", Integer.valueOf(0));
                perms.put("android.permission.CAMERA", Integer.valueOf(0));
                for (int i = 0; i < permissions.length; i++) {
                    perms.put(permissions[i], Integer.valueOf(grantResults[i]));
                }
                if (((Integer) perms.get("android.permission.ACCESS_NETWORK_STATE")).intValue() == 0 && ((Integer) perms.get("android.permission.WRITE_EXTERNAL_STORAGE")).intValue() == 0 && ((Integer) perms.get("android.permission.CAMERA")).intValue() == 0) {
                    Toast.makeText(getApplicationContext(), "All Permission is Granted", 0).show();
                    return;
                }
                return;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                return;
        }
    }

    protected void setAllTypefaceThin(View view) {
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getChildCount() != 0) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllTypefaceThin(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf"));
        }
    }

    public void BackPressed() {
        dialog = new Dialog(this);
        dialog.requestWindowFeature(1);
        dialog.setContentView(R.layout.popup_exit);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        FancyButton tv_logout = (FancyButton) dialog.findViewById(R.id.tv_logout);
        FancyButton tv_cancel = (FancyButton) dialog.findViewById(R.id.tv_cancel);
        setAllTypefaceThin(dialog.findViewById(R.id.root_layout));

        tv_cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        tv_logout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.addCategory("android.intent.category.HOME");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                System.exit(0);
            }
        });
        dialog.show();
    }

    private static class MyHandler extends WeakHandler<WalletActivity> {
        public MyHandler(WalletActivity ref) {
            super(ref);
        }

        @Override
        protected void weakHandleMessage(WalletActivity ref, Message msg) {
            switch (msg.what) {
                /* TX_BROADCAST_OK, TX_BROADCAST_ERROR, SET_URI, OPEN_ACCOUNT, OPEN_OVERVIEW
                PROCESS_URI
                 */
                case TX_BROADCAST_OK:
                    Toast.makeText(ref, ref.getString(R.string.sent_msg),
                            Toast.LENGTH_LONG).show();
                    ref.goToBalance();
                    ref.resetSend();
                    break;
                case TX_BROADCAST_ERROR:
                    Toast.makeText(ref, ref.getString(R.string.get_tx_broadcast_error),
                            Toast.LENGTH_LONG).show();
                    ref.goToSend();
                    break;
                case SET_URI:
                    if (ref.accountFragment == null) {
                        Toast.makeText(ref, ref.getString(R.string.no_wallet_pocket_selected),
                                Toast.LENGTH_LONG).show();
                    }
                    ref.accountFragment.sendToUri((CoinURI) msg.obj);
                    break;
                case OPEN_ACCOUNT:
                    ref.openAccount((WalletAccount) msg.obj);
                    break;
                case OPEN_OVERVIEW:
                    ref.openOverview();
                    break;
                case PROCESS_URI:
                    try {
                        ref.processUri((String) msg.obj);
                    } catch (CoinURIParseException e) {
                        ref.showScanFailedMessage(e);
                    }
                    break;
            }
        }
    }
}
