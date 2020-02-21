package com.kabberry.wallet.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.coinomi.core.Preconditions;
import com.coinomi.core.wallet.Wallet;

import com.kabberry.wallet.R;
import com.kabberry.wallet.Configuration;
import com.kabberry.wallet.WalletApplication;
import com.kabberry.wallet.ui.Dialogs.ProgressDialogFragment;
import com.kabberry.wallet.util.Fonts;
import com.kabberry.wallet.util.Fonts.Font;
import com.kabberry.wallet.util.QrUtils;
import com.kabberry.wallet.util.UiUtils;
import com.kabberry.wallet.util.WeakHandler;

import mehdi.sakout.fancybuttons.FancyButton;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.wallet.DeterministicSeed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

public class ShowSeedFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(ShowSeedFragment.class);

    private static final int UPDATE_VIEW = 0;
    private static final int SET_PASSWORD = 1;
    private static final int SET_SEED = 2;

    private static final String SEED_PROCESSING_DIALOG_TAG = "seed_processing_dialog_tag";
    private static final String PASSWORD_DIALOG_TAG = "password_dialog_tag";

    private View seedLayout;
    private View seedEncryptedLayout;
    private TextView seedView;
    private View seedPasswordProtectedView;
    private ImageView qrView;
    private Listener listener;
    private LoadSeedTask decryptSeedTask;

    private Wallet wallet;
    private CharSequence password;
    private SeedInfo seedInfo;

    private final Handler handler = new MyHandler(this);
    private Configuration configuration;

    private static class MyHandler extends WeakHandler<ShowSeedFragment> {
        public MyHandler(ShowSeedFragment ref) {
            super(ref);
        }

        @Override
        protected void weakHandleMessage(ShowSeedFragment ref, Message msg) {
            switch (msg.what) {
                case UPDATE_VIEW:
                    ref.updateView();
                    return;
                case SET_PASSWORD:
                    ref.setPassword((CharSequence) msg.obj);
                    return;
                case SET_SEED:
                    ref.seedInfo = (SeedInfo) msg.obj;
                    ref.updateView();
                    return;
                default:
                    return;
            }
        }
    }

    public void setPassword(CharSequence password) {
        this.password = password;
        updateView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);    // for the async task
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_show_seed, container, false);
        setAllTypefaceThin(view);

        seedLayout = view.findViewById(R.id.show_seed_layout);
        seedEncryptedLayout = view.findViewById(R.id.seed_encrypted_layout);
        seedEncryptedLayout.setVisibility(View.GONE);
        // hide the layout as maybe we have to show the password dialog
        seedLayout.setVisibility(View.GONE);
        seedView = (TextView) view.findViewById(R.id.seed);
        seedPasswordProtectedView = view.findViewById(R.id.seed_password_protected);
        Fonts.setTypeface(view.findViewById(R.id.seed_password_protected_lock), Font.COINOMI_FONT_ICONS);
        qrView = (ImageView) view.findViewById(R.id.qr_code_seed);

        ((ImageView) view.findViewById(R.id.lock_icon)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowUnlockDialog();
            }
        });

        updateView();

        return view;
    }

    public void ShowUnlockDialog() {
        final Dialog passDialog = new Dialog(getActivity());
        passDialog.requestWindowFeature(1);
        passDialog.setContentView(R.layout.dialog_password);
        passDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        passDialog.setCancelable(false);
        passDialog.setCanceledOnTouchOutside(false);
        FancyButton tv_confirm = (FancyButton) passDialog.findViewById(R.id.tv_confirm);
        FancyButton tv_close = (FancyButton) passDialog.findViewById(R.id.tv_close);
        final EditText passwordView = (EditText) passDialog.findViewById(R.id.password);
        ((ProgressBar) passDialog.findViewById(R.id.process)).setVisibility(8);
        setAllTypefaceThin(passDialog.findViewById(R.id.root_layout));
        setAllTypefaceBold(tv_confirm);
        setAllTypefaceBold(tv_close);

        tv_confirm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (passwordView.getText().toString().equals(configuration.GetPassword())) {
                    passDialog.dismiss();
                    seedEncryptedLayout.setVisibility(View.GONE);
                    maybeStartDecryptTask();
                    return;
                }

                seedEncryptedLayout.setVisibility(0);
                passwordView.setError(ShowSeedFragment.this.getString(R.string.password_failed));
            }
        });

        tv_close.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                passDialog.dismiss();
                seedEncryptedLayout.setVisibility(View.VISIBLE);
            }
        });
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        passDialog.getWindow().setLayout(width - (width / 10), -2);
        passDialog.show();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (Listener) context;
            WalletApplication application = (WalletApplication) context.getApplicationContext();
            wallet = application.getWallet();
            configuration = application.getConfiguration();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + Listener.class);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.listener = null;
    }

    private void updateView() {
        if (seedInfo != null) {
            seedLayout.setVisibility(View.VISIBLE);
            seedEncryptedLayout.setVisibility(View.GONE);
            seedView.setText(this.seedInfo.seedString);
            QrUtils.setQr(qrView, getResources(), seedInfo.seedString);
            if (seedInfo.isSeedPasswordProtected) {
                seedPasswordProtectedView.setVisibility(View.VISIBLE);
            } else {
                seedPasswordProtectedView.setVisibility(View.GONE);
            }
        } else if (wallet == null) {
        } else {
            if (wallet.getSeed() == null) {
                if (listener != null) {
                    listener.onSeedNotAvailable();
                }
            } else if (wallet.getSeed().isEncrypted()) {
                seedEncryptedLayout.setVisibility(0);
                if (password == null) {
                    ShowUnlockDialog();
                    return;
                }
                Log.e("TAG in else", "password != null");
                maybeStartDecryptTask();
            } else {
                Log.e("TAG in else", "isEncrypted false");
                seedEncryptedLayout.setVisibility(View.VISIBLE);
                ShowUnlockDialog();
            }
        }
    }

    private void maybeStartDecryptTask() {
        if (decryptSeedTask == null) {
            decryptSeedTask = new LoadSeedTask();
            decryptSeedTask.execute();
        }
    }

    private class LoadSeedTask extends AsyncTask<Void, Void, Void> {
        SeedInfo seedInfo;

        private LoadSeedTask() {
            this.seedInfo = null;
        }

        @Override
        protected void onPreExecute() {
            ProgressDialogFragment.show(ShowSeedFragment.this.getFragmentManager(),
                    ShowSeedFragment.this.getString(R.string.seed_working),
                    SEED_PROCESSING_DIALOG_TAG);
        }

        @Override
        protected Void doInBackground(Void... params) {
            DeterministicKey testMasterKey;
            DeterministicKey masterKey = null;
            DeterministicSeed seed = wallet.getSeed();
            if (seed != null) {
                try {
                    DeterministicSeed seed2;
                    if (wallet.getKeyCrypter() != null) {
                        KeyCrypter crypter = wallet.getKeyCrypter();
                        KeyParameter aesKey = crypter.deriveKey(password);
                        seed = wallet.getSeed().decrypt(crypter, password.toString(), aesKey);
                        masterKey = wallet.getMasterKey().decrypt(crypter, aesKey);
                        seed2 = seed;
                    } else {
                        masterKey = wallet.getMasterKey();
                        seed2 = seed;
                    }

                    try {
                        Preconditions.checkState(!seed2.isEncrypted());
                        Preconditions.checkState(!masterKey.isEncrypted());
                        seed = new DeterministicSeed(seed2.getMnemonicCode(), null, "", 0);
                    } catch (Exception e) {
                        seed = seed2;
                        ShowSeedFragment.log.warn("Failed recovering seed.");
                        seedInfo = new SeedInfo();
                        seedInfo.seedString = Wallet.mnemonicToString(seed.getMnemonicCode());
                        testMasterKey = HDKeyDerivation.createMasterPrivateKey(seed.getSeedBytes());
                        seedInfo.isSeedPasswordProtected = masterKey.getPrivKey().equals(testMasterKey.getPrivKey());
                        return null;
                    }
                } catch (Exception e2) {
                    ShowSeedFragment.log.warn("Failed recovering seed.");
                    seedInfo = new SeedInfo();
                    seedInfo.seedString = Wallet.mnemonicToString(seed.getMnemonicCode());
                    testMasterKey = HDKeyDerivation.createMasterPrivateKey(seed.getSeedBytes());
                    if (masterKey.getPrivKey().equals(testMasterKey.getPrivKey())) {
                    }
                    seedInfo.isSeedPasswordProtected = masterKey.getPrivKey().equals(testMasterKey.getPrivKey());
                    return null;
                }
            }
            if (!(seed == null || masterKey == null)) {
                seedInfo = new SeedInfo();
                seedInfo.seedString = Wallet.mnemonicToString(seed.getMnemonicCode());
                testMasterKey = HDKeyDerivation.createMasterPrivateKey(seed.getSeedBytes());
                if (masterKey.getPrivKey().equals(testMasterKey.getPrivKey())) {
                }
                seedInfo.isSeedPasswordProtected = masterKey.getPrivKey().equals(testMasterKey.getPrivKey());
            }
            return null;
        }

        protected void onPostExecute(Void aVoid) {
            if (!Dialogs.dismissAllowingStateLoss(ShowSeedFragment.this.getFragmentManager(), SEED_PROCESSING_DIALOG_TAG)) {
                if (seedInfo != null) {
                    Log.e("TAG in if ", "seedInfo != null con");
                    handler.sendMessage(handler.obtainMessage(SET_SEED, seedInfo));
                    return;
                }

                decryptSeedTask = null;
                seedEncryptedLayout.setVisibility(View.VISIBLE);

                final Dialog passDialog = new Dialog(ShowSeedFragment.this.getActivity());
                passDialog.requestWindowFeature(1);
                passDialog.setContentView(R.layout.dialog_retry_password);
                passDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                passDialog.setCancelable(false);
                passDialog.setCanceledOnTouchOutside(false);
                passDialog.getWindow().addFlags(4);

                FancyButton tv_confirm = (FancyButton) passDialog.findViewById(R.id.tv_confirm);
                ImageView tv_cancel = (ImageView) passDialog.findViewById(R.id.tv_cancel);
                ShowSeedFragment.this.setAllTypefaceThin(passDialog.findViewById(R.id.root_layout));

                tv_confirm.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        passDialog.dismiss();
                        ShowUnlockDialog();
                    }
                });

                tv_cancel.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        passDialog.dismiss();
                    }
                });

                DisplayMetrics displayMetrics = new DisplayMetrics();
                ShowSeedFragment.this.getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int width = displayMetrics.widthPixels;
                passDialog.getWindow().setLayout(width - (width / 10), -2);
                passDialog.show();
            }
        }
    }

    private static class SeedInfo {
        boolean isSeedPasswordProtected;
        String seedString;

        private SeedInfo() {
        }
    }

    public interface Listener extends com.kabberry.wallet.ui.UnlockWalletDialog.Listener {
        void onSeedNotAvailable();
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

    protected void setAllTypefaceBold(View view) {
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getChildCount() != 0) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllTypefaceBold(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Bold.ttf"));
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 16908332:
                getActivity().onBackPressed();
                return true;
            case R.id.action_copy:
                if (seedEncryptedLayout.getVisibility() != View.GONE) {
                    return true;
                }
                UiUtils.copy(getActivity(), this.seedView.getText().toString());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
