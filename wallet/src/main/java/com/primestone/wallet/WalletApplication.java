package com.primestone.wallet;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy.Builder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.widget.Toast;

import android.support.multidex.MultiDex;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.CoinType.FeeProvider;
import com.coinomi.core.coins.Value;
import com.coinomi.core.exchange.shapeshift.ShapeShift;
import com.coinomi.core.util.HardwareSoftwareCompliance;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.Wallet;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.core.wallet.WalletProtobufSerializer;
import com.google.common.collect.ImmutableList;
import com.primestone.wallet.ui.WalletActivity;

import com.primestone.wallet.service.CoinService;
import com.primestone.wallet.service.CoinService.ServiceMode;
import com.primestone.wallet.service.CoinServiceImpl;
import com.primestone.wallet.ui.WalletActivity;
import com.primestone.wallet.ui.widget.MDToast;
import com.primestone.wallet.util.Fonts;
import com.primestone.wallet.util.LinuxSecureRandom;
import com.primestone.wallet.util.NetworkUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender.Method;
import org.acra.sender.HttpSender.Type;

import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.store.UnreadableWalletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ReportsCrashes(
        httpMethod = Method.PUT,
        reportType = Type.JSON
)
public class WalletApplication extends Application {
    private static final Logger log = LoggerFactory.getLogger(WalletApplication.class);

    private static WalletApplication mInstance;
    private Configuration config;
    private ActivityManager activityManager;

    private Intent coinServiceCancelCoinsReceivedIntent;
    private Intent coinServiceConnectIntent;
    private Intent coinServiceIntent;

    private ConnectivityManager connManager;
    private long lastStop;
    public PendingIntent logoutPendingIntent = null;
    private PackageInfo packageInfo;
    private ShapeShift shapeShift;
    private File txCachePath;
    private String versionString;

    @Nullable
    private Wallet wallet;
    private File walletFile;

    @Override
    public void onCreate() {
        config = new Configuration(PreferenceManager.getDefaultSharedPreferences(this));
        mInstance = this;
        LinuxSecureRandom linuxSecureRandom = new LinuxSecureRandom();  // init proper random number generator
        MultiDex.install(this);
        performComplianceTests();

        initLogging();

        StrictMode.setThreadPolicy(new
                Builder().detectAll().permitDiskReads().permitDiskWrites().penaltyLog().build());
        super.onCreate();

        packageInfo = packageInfoFromContext(this);
        versionString = packageInfo.versionName.replace(" ", "_") + "__" +
                packageInfo.packageName + "_android";

        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        coinServiceIntent = new Intent(this, CoinServiceImpl.class);
        coinServiceConnectIntent = new Intent(CoinService.ACTION_CONNECT_COIN,
                null, this, CoinServiceImpl.class);
        coinServiceCancelCoinsReceivedIntent = new Intent(CoinService.ACTION_CANCEL_COINS_RECEIVED,
                null, this, CoinServiceImpl.class);

        createTxCache();
        Intent intent = new Intent(getApplicationContext(), WalletActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setAction("Primestone.LOGOUT");
        this.logoutPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

        // Set MnemonicCode.INSTANCE if needed
        if (MnemonicCode.INSTANCE == null) {
            try {
                MnemonicCode.INSTANCE = new MnemonicCode();
            } catch (IOException e) {
                throw new RuntimeException("Could not set MnemonicCode.INSTANCE", e);
            }
        }

        config.updateLastVersionCode(this.packageInfo.versionCode);

        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        walletFile = getFileStreamPath("wallet");
        loadWallet();

        afterLoadWallet();
        Fonts.initFonts(getAssets());
    }

    public static synchronized WalletApplication getInstance() {
        WalletApplication walletApplication;
        synchronized (WalletApplication.class) {
            walletApplication = mInstance;
        }
        return walletApplication;
    }

    private void createTxCache() {
        txCachePath = new File(this.getCacheDir(), Constants.TX_CACHE_NAME);
        if (!txCachePath.exists()) {
            if (!txCachePath.mkdirs()) {
                txCachePath = null;
                log.error("Error creating transaction cache folder");
                return;
            }
        }

        // Make cache dirs for all coins
        for (CoinType type : Constants.SUPPORTED_COINS) {
            File coinCachePath = new File(txCachePath, type.getId());
            if (!coinCachePath.exists()) {
                if (!coinCachePath.mkdirs()) {
                    txCachePath = null;
                    log.error("Error creating transaction cache folder");
                    return;
                }
            }
        }
    }

    public boolean isConnected() {
        NetworkInfo activeInfo = connManager.getActiveNetworkInfo();
        return activeInfo != null && activeInfo.isConnected();
    }

    public ShapeShift getShapeShift() {
        if (shapeShift == null) {
            shapeShift = new ShapeShift(NetworkUtils.getHttpClient(getApplicationContext()));
        }
        return this.shapeShift;
    }

    public File getTxCachePath() {
        return this.txCachePath;
    }

    /**
     * Some devices have software bugs that causes the EC crypto to malfunction.
     */
    private void performComplianceTests() {
        if (!config.isDeviceCompatible()) {
            if (HardwareSoftwareCompliance.isEllipticCurveCryptographyCompliant()) {
                this.config.setDeviceCompatible(true);
                return;
            }
            this.config.setDeviceCompatible(false);
            ACRA.getErrorReporter().handleSilentException(new Exception("Device failed EllipticCurveCryptographyCompliant test"));
        }
    }

    private void afterLoadWallet() {
        setupFeeProvider();
    }

    private void setupFeeProvider() {
        CoinType.setFeeProvider(new CoinType.FeeProvider() {
            @Override
            public Value getFeeValue(CoinType type) {
                return config.getFeeValue(type);
            }
        });
    }

    private void initLogging() {
    }

    public void startLogoutTimer(Context context) {
        ((AlarmManager) context.getSystemService("alarm")).set(2, SystemClock.elapsedRealtime() + 25000, this.logoutPendingIntent);
    }

    public void stopLogoutTimer(Context context) {
        ((AlarmManager) context.getSystemService("alarm")).cancel(this.logoutPendingIntent);
    }

    public Configuration getConfiguration() {
        return config;
    }

    @Nullable
    public Wallet getWallet() {
        return wallet;
    }

    @Nullable
    public WalletAccount getAccount(@Nullable String accountId) {
        if (wallet != null) {
            return wallet.getAccount(accountId);
        }
        return null;
    }

    public List<WalletAccount> getAccounts(CoinType type) {
        if (wallet != null) {
            return this.wallet.getAccounts(type);
        }
        return ImmutableList.of();
    }

    public List<WalletAccount> getAccounts(List<CoinType> types) {
        if (wallet != null) {
            return this.wallet.getAccounts(types);
        }
        return ImmutableList.of();
    }

    public List<WalletAccount> getAccounts(AbstractAddress address) {
        if (wallet != null) {
            return wallet.getAccounts(address);
        }
        return ImmutableList.of();
    }

    public List<WalletAccount> getAllAccounts() {
        if (wallet != null) {
            return wallet.getAllAccounts();
        }
        return ImmutableList.of();
    }

    /**
     * Check if account exists
     */
    public boolean isAccountExists(String accountId) {
        if (wallet != null) {
            return wallet.isAccountExists(accountId);
        }
        return false;
    }

    public void setEmptyWallet() {
        setWallet(null);
    }

    public void setWallet(@Nullable Wallet wallet) {
        if (this.wallet != null) {
            this.wallet.shutdownAutosaveAndWait();
        }

        this.wallet = wallet;
        if (this.wallet != null) {
            this.wallet.autosaveToFile(walletFile, Constants.WALLET_WRITE_DELAY,
                    Constants.WALLET_WRITE_DELAY_UNIT, null);
        }
    }

    private void loadWallet() {
        if (walletFile.exists()) {
            final long start = System.currentTimeMillis();

            FileInputStream walletStream = null;

            try {
                walletStream = new FileInputStream(walletFile);

                setWallet(WalletProtobufSerializer.readWallet(walletStream));

                log.info("wallet loaded from: '" + walletFile + "', took " + (System.currentTimeMillis() - start) + "ms");
            } catch (final FileNotFoundException e) {
                ACRA.getErrorReporter().handleException(e);
                MDToast.makeText(WalletApplication.this, R.string.error_could_not_read_wallet, Toast.LENGTH_LONG).show();
            } catch (final UnreadableWalletException e) {
                MDToast.makeText(WalletApplication.this, R.string.error_could_not_read_wallet, Toast.LENGTH_LONG).show();
                ACRA.getErrorReporter().handleException(e);
            } finally {
                if (walletStream != null) {
                    try {
                        walletStream.close();
                    } catch (final IOException x) { /* ignore */ }
                }
            }
        }
    }

    public void saveWalletNow() {
        if (wallet != null) {
            wallet.saveNow();
        }
    }

    public void startBlockchainService(CoinService.ServiceMode mode) {
        switch (mode) {
            case CANCEL_COINS_RECEIVED:
                startService(coinServiceCancelCoinsReceivedIntent);
                //return;
                break;
            case NORMAL:
            default:
                startService(coinServiceIntent);
                //return;
                break;
        }
    }

    public static PackageInfo packageInfoFromContext(final Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (NameNotFoundException x) {
            throw new RuntimeException(x);
        }
    }

    public PackageInfo packageInfo() {
        return packageInfo;
    }

    public String getVersionString() {
        return versionString;
    }

    public void touchLastResume() {
        lastStop = -1;
    }

    public void touchLastStop() {
        lastStop = SystemClock.elapsedRealtime();
    }

    public long getLastStop() {
        return lastStop;
    }

    public void maybeConnectAccount(WalletAccount account) {
        if (!account.isConnected()) {
            coinServiceConnectIntent.putExtra(Constants.ARG_ACCOUNT_ID, account.getId());
            startService(coinServiceConnectIntent);
        }
    }

    public void clearApplicationData() {
        File applicationDirectory = new File(getCacheDir().getParent());
        if (applicationDirectory.exists()) {
            for (String fileName : applicationDirectory.list()) {
                if (!fileName.equals("lib")) {
                    deleteFile(new File(applicationDirectory, fileName));
                }
            }
        }
    }

    public static boolean deleteFile(File file) {
        boolean deletedAll = true;
        if (file == null) {
            return true;
        }
        if (!file.isDirectory()) {
            return file.delete();
        }
        String[] children = file.list();
        for (String file2 : children) {
            deletedAll = deleteFile(new File(file, file2)) && deletedAll;
        }
        return deletedAll;
    }
}
