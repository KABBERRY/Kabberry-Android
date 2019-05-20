package com.primestone.wallet.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.wallet.Wallet;

import com.primestone.wallet.R;
import com.primestone.wallet.Constants;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.service.CoinService;
import com.primestone.wallet.service.CoinServiceImpl;
import com.primestone.wallet.util.WeakHandler;

import java.util.ArrayList;
import java.util.List;

import org.bitcoinj.crypto.KeyCrypterScrypt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import javax.annotation.Nullable;

/**
 * Fragment that restores a wallet
 */
public class FinalizeWalletRestorationFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(FinalizeWalletRestorationFragment.class);

    private static final int RESTORE_STATUS_UPDATE = 0;
    private static final int RESTORE_FINISHED = 1;

    private final Handler handler = new MyHandler(this);

    private static WalletFromSeedTask walletFromSeedTask;
    private TextView status;

    /**
     * Get a fragment instance.
     */
    public static Fragment newInstance(Bundle args) {
        FinalizeWalletRestorationFragment fragment = new FinalizeWalletRestorationFragment();
        fragment.setRetainInstance(true);
        fragment.setArguments(args);
        return fragment;
    }

    public FinalizeWalletRestorationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WalletApplication app = getWalletApplication();
        if (getArguments() != null) {
            Bundle args = getArguments();
            String seed = args.getString("seed");
            String password = args.getString("password");
            String seedPassword = args.getString("seed_password");
            List<CoinType> coinsToCreate = getCoinsTypes(args);

            if (walletFromSeedTask == null) {
                walletFromSeedTask = new WalletFromSeedTask(this.handler, app, coinsToCreate, seed, password, seedPassword);
                walletFromSeedTask.execute();
                return;
            } else {
                switch (walletFromSeedTask.getStatus()) {
                    case FINISHED:
                        handler.sendEmptyMessage(RESTORE_FINISHED);
                        break;
                    case RUNNING:
                    case PENDING:
                        walletFromSeedTask.handler = handler;
                }
            }

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_finalize_wallet_restoration, container, false);
        status = (TextView) view.findViewById(R.id.restoration_status);
        setAllTypefaceThin(view);

        return view;
    }

    private List<CoinType> getCoinsTypes(Bundle args) {
        /*
        ArrayList<String> coinIds = args.getStringArrayList(Constants.ARG_MULTIPLE_COIN_IDS);
        if (coinIds != null) {
            List<CoinType> coinTypes = new ArrayList<CoinType>();
            for (String id : coinIds) {
                coinTypes.add(CoinID.typeFromId(id));
            }
            return coinTypes;
        } else {
            return Constants.DEFAULT_COINS;
        }
         */
        return Constants.DEFAULT_COINS;
    }

    WalletApplication getWalletApplication() {
        return (WalletApplication) getActivity().getApplication();
    }

    static class WalletFromSeedTask extends AsyncTask<Void, String, Wallet> {
        private final List<CoinType> coinsToCreate;
        String errorMessage = "";
        Handler handler;
        private final String password;
        private final String seed;
        @Nullable private final String seedPassword;
        Wallet wallet;
        private final WalletApplication walletApplication;

        public WalletFromSeedTask(Handler handler, WalletApplication walletApplication, List<CoinType> coinsToCreate, String seed, String password, String seedPassword) {
            this.handler = handler;
            this.walletApplication = walletApplication;
            this.coinsToCreate = coinsToCreate;
            this.seed = seed;
            this.password = password;
            this.seedPassword = seedPassword;
        }

        protected Wallet doInBackground(Void... params) {
            this.walletApplication.startService(new Intent(CoinService.ACTION_CLEAR_CONNECTIONS, null, this.walletApplication, CoinServiceImpl.class));
            List seedWords = new ArrayList();
            for (String word : this.seed.trim().split(" ")) {
                if (!word.isEmpty()) {
                    seedWords.add(word);
                }
            }
            try {
                publishProgress(new String[]{""});
                this.walletApplication.setEmptyWallet();
                this.wallet = new Wallet(seedWords, this.seedPassword);
                KeyParameter aesKey = null;
                if (!(this.password == null || this.password.isEmpty())) {
                    KeyCrypterScrypt crypter = new KeyCrypterScrypt();
                    aesKey = crypter.deriveKey(this.password);
                    this.wallet.encrypt(crypter, aesKey);
                }
                for (CoinType type : this.coinsToCreate) {
                    publishProgress(new String[]{type.getName()});
                    this.wallet.createAccount(type, false, aesKey);
                }
                this.walletApplication.setWallet(this.wallet);
                this.walletApplication.saveWalletNow();
            } catch (Throwable e) {
                FinalizeWalletRestorationFragment.log.error("Error creating a wallet", e);
                this.errorMessage = e.getMessage();
            }

            return wallet;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            this.handler.sendMessage(this.handler.obtainMessage(0, values[0]));
        }

        protected void onPostExecute(Wallet wallet) {
            handler.sendEmptyMessage(1);
        }
    }

    private static class MyHandler extends WeakHandler<FinalizeWalletRestorationFragment> {
        public MyHandler(FinalizeWalletRestorationFragment ref) {
            super(ref);
        }

        @Override
        protected void weakHandleMessage(FinalizeWalletRestorationFragment ref, Message msg) {
            switch (msg.what) {
                case 0:     //RESTORE_STATUS_UPDATE
                    String workingOn = (String) msg.obj;
                    if (workingOn.isEmpty()) {
                        ref.status.setText(ref.getString(R.string.wallet_restoration_master_key));
                    } else {
                        ref.status.setText(ref.getString(R.string.wallet_restoration_coin, workingOn));
                    }
                    break;
                case 1:     //RESTORE_FINISHED:
                    WalletFromSeedTask task = FinalizeWalletRestorationFragment.walletFromSeedTask;
                    FinalizeWalletRestorationFragment.walletFromSeedTask = null;
                    if (task.wallet != null) {
                        ref.startWalletActivity();
                    } else {
                        String errorMessage = ref.getResources().getString(
                                R.string.wallet_restoration_error, task.errorMessage);
                        ref.showErrorAndStartIntroActivity(errorMessage);
                    }

            }
        }
    }

    public void startWalletActivity() {
        Intent intent = new Intent(getActivity(), InfoActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        getActivity().finish();
    }

    private void showErrorAndStartIntroActivity(String errorMessage) {
        Toast.makeText(getActivity(), errorMessage, 1).show();
        startActivity(new Intent(getActivity(), IntroActivity.class));
        getActivity().finish();
    }


    static /* synthetic */ class C11361 {
        static final /* synthetic */ int[] $SwitchMap$android$os$AsyncTask$Status = new int[Status.values().length];

        static {
            try {
                $SwitchMap$android$os$AsyncTask$Status[Status.FINISHED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$os$AsyncTask$Status[Status.RUNNING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$os$AsyncTask$Status[Status.PENDING.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
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
