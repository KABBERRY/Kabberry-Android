package com.kabberry.wallet.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.OnClick;

import com.coinomi.core.Preconditions;
import com.coinomi.core.wallet.Wallet;

import com.kabberry.wallet.R;
import com.kabberry.wallet.WalletApplication;
import com.kabberry.wallet.ui.Dialogs.ProgressDialogFragment;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.crypto.KeyCrypterScrypt;
import org.bitcoinj.wallet.Protos.ScryptParameters;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.util.encoders.Hex;

public class DebuggingFragment extends Fragment {
    private CharSequence password;
    private PasswordTestTask passwordTestTask;
    private Wallet wallet;

    private class PasswordTestTask extends AsyncTask<Void, Void, Void> {
        UnlockResult result;

        private PasswordTestTask() {
            this.result = new UnlockResult();
        }

        protected void onPreExecute() {
            ProgressDialogFragment.show(DebuggingFragment.this.getFragmentManager(), DebuggingFragment.this.getString(R.string.seed_working), "processing_dialog_tag");
        }

        protected Void doInBackground(Void... params) {
            tryDecrypt(DebuggingFragment.this.wallet.getMasterKey(), DebuggingFragment.this.password, this.result);
            return null;
        }

        private void tryDecrypt(DeterministicKey masterKey, CharSequence password, UnlockResult result) {
            KeyCrypter crypter = (KeyCrypter) Preconditions.checkNotNull(masterKey.getKeyCrypter());
            KeyParameter k = crypter.deriveKey(password);
            try {
                result.inputFingerprint = DebuggingFragment.this.getFingerprint(password.toString().getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
            }
            result.keyFingerprint = DebuggingFragment.this.getFingerprint(k.getKey());
            if (crypter instanceof KeyCrypterScrypt) {
                result.scryptParams = ((KeyCrypterScrypt) crypter).getScryptParameters();
            }
            try {
                masterKey.decrypt(crypter, k);
                result.isUnlockSuccess = true;
            } catch (KeyCrypterException e2) {
                result.isUnlockSuccess = false;
                result.error = e2.getMessage();
            }
        }

        protected void onPostExecute(Void aVoid) {
            DebuggingFragment.this.passwordTestTask = null;
            if (!Dialogs.dismissAllowingStateLoss(DebuggingFragment.this.getFragmentManager(), "processing_dialog_tag")) {
                String yes = DebuggingFragment.this.getString(R.string.yes);
                String no = DebuggingFragment.this.getString(R.string.no);
                DebuggingFragment debuggingFragment = DebuggingFragment.this;
                Object[] objArr = new Object[3];
                if (!this.result.isUnlockSuccess) {
                    yes = no;
                }
                objArr[0] = yes;
                objArr[1] = this.result.inputFingerprint;
                objArr[2] = this.result.keyFingerprint;
                String message = debuggingFragment.getString(R.string.debugging_test_wallet_password_results, objArr);
                if (this.result.scryptParams != null) {
                    ScryptParameters sp = this.result.scryptParams;
                    message = message + "\n\nScrypt:\nS = " + Hex.toHexString(sp.getSalt().toByteArray()) + "\nN = " + sp.getN() + "\nP = " + sp.getP() + "\nR = " + sp.getR();
                }
                if (this.result.error != null) {
                    message = message + "\n\n" + this.result.error;
                }
            }
        }
    }

    static class UnlockResult {
        String error;
        String inputFingerprint;
        boolean isUnlockSuccess = false;
        String keyFingerprint;
        ScryptParameters scryptParams;

        UnlockResult() {
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_debugging, container, false);
        ButterKnife.bind((Object) this, view);
        setAllTypefaceThin(view);
        return view;
    }

    public void onDestroyView() {
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        this.wallet = ((WalletApplication) context.getApplicationContext()).getWallet();
    }

    @OnClick(R.id.button_execute_password_test)
    void onExecutePasswordTest() {
        if (this.wallet.isEncrypted()) {
            showUnlockDialog();
        } else {
            DialogBuilder.warn(getActivity(), R.string.wallet_is_not_locked_message).setPositiveButton((int) R.string.button_ok, null).create().show();
        }
    }

    private void showUnlockDialog() {
        Dialogs.dismissAllowingStateLoss(getFragmentManager(), "password_dialog_tag");
        UnlockWalletDialog.getInstance().show(getFragmentManager(), "password_dialog_tag");
    }

    public void setPassword(CharSequence password) {
        this.password = password;
        maybeStartPasswordTestTask();
    }

    private void maybeStartPasswordTestTask() {
        if (this.passwordTestTask == null) {
            this.passwordTestTask = new PasswordTestTask();
            this.passwordTestTask.execute(new Void[0]);
        }
    }

    private String getFingerprint(byte[] b) {
        return Hex.toHexString(Arrays.copyOf(Sha256Hash.create(b).getBytes(), 4));
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
