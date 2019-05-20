package com.primestone.wallet.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.MultiAutoCompleteTextView;
import android.widget.MultiAutoCompleteTextView.Tokenizer;
import android.widget.TextView;

import com.coinomi.core.CoreUtils;

import com.primestone.wallet.R;
import com.primestone.wallet.ui.WelcomeFragment.Listener;
import com.primestone.wallet.util.Fonts;
import com.primestone.wallet.util.Fonts.Font;
import com.primestone.wallet.util.Keyboard;

import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.crypto.MnemonicException.MnemonicChecksumException;
import org.bitcoinj.crypto.MnemonicException.MnemonicWordException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestoreFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(RestoreFragment.class);
    private EditText bip39Passphrase;
    private TextView f80errorMnemonicessage;
    String get_scanR = null;
    private boolean isNewSeed;
    private boolean isSeedProtected = false;
    private Listener listener;
    private MultiAutoCompleteTextView mnemonicTextView;
    private String seed;
    private TextView skipButton;
    private TextView tv_title_back;
    private View v_devider;

    class C11741 implements OnClickListener {
        C11741() {
        }

        public void onClick(View v) {
            RestoreFragment.this.getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    class C11752 implements OnClickListener {
        C11752() {
        }

        public void onClick(View v) {
            RestoreFragment.this.handleScan();
        }
    }

    private static abstract class SpaceTokenizer implements Tokenizer {
        public abstract void onToken();

        private SpaceTokenizer() {
        }

        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;
            while (i > 0 && text.charAt(i - 1) != ' ') {
                i--;
            }
            return i;
        }

        public int findTokenEnd(CharSequence text, int cursor) {
            int len = text.length();
            for (int i = cursor; i < len; i++) {
                if (text.charAt(i) == ' ') {
                    return i;
                }
            }
            return len;
        }

        public CharSequence terminateToken(CharSequence text) {
            onToken();
            return text + " ";
        }
    }

    class C11763 extends SpaceTokenizer {
        C11763() {
            super();
        }

        public void onToken() {
            RestoreFragment.this.clearError(RestoreFragment.this.f80errorMnemonicessage);
        }
    }

    class C11785 implements OnClickListener {
        C11785() {
        }

        public void onClick(View v) {
            RestoreFragment.this.verifyMnemonicAndProceed();
        }
    }

    class C11796 implements OnClickListener {
        C11796() {
        }

        public void onClick(View v) {
            RestoreFragment.this.mnemonicTextView.setText("");
            SkipDialogFragment.newInstance(RestoreFragment.this.seed).show(RestoreFragment.this.getFragmentManager(), null);
        }
    }

    public static class SkipDialogFragment extends DialogFragment {
        private Listener mListener;

        public static SkipDialogFragment newInstance(String seed) {
            SkipDialogFragment newDialog = new SkipDialogFragment();
            Bundle args = new Bundle();
            args.putString("seed", seed);
            newDialog.setArguments(args);
            return newDialog;
        }

        public void onAttach(Activity activity) {
            super.onAttach(activity);
            try {
                this.mListener = (Listener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString() + " must implement WelcomeFragment.OnFragmentInteractionListener");
            }
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String seed = getArguments().getString("seed");
            Dialog dialog = new Dialog(getActivity());
            dialog.requestWindowFeature(1);
            dialog.setContentView(R.layout.dialog_change_password);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = displayMetrics.widthPixels;
            dialog.getWindow().setLayout(width - (width / 10), -2);
            return dialog;
        }
    }

    public static RestoreFragment newInstance() {
        return newInstance(null);
    }

    public static RestoreFragment newInstance(String seed) {
        RestoreFragment fragment = new RestoreFragment();
        if (seed != null) {
            Bundle args = new Bundle();
            args.putString("seed", seed);
            fragment.setArguments(args);
        }
        return fragment;
    }

    public static RestoreFragment newInstance(String seed, Boolean From_QR) {
        RestoreFragment fragment = new RestoreFragment();
        if (seed != null) {
            Bundle args = new Bundle();
            args.putString("seed", seed);
            args.putBoolean("From_QR", From_QR.booleanValue());
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.seed = getArguments().getString("seed");
            this.isNewSeed = this.seed != null;
            if (getArguments().containsKey("From_QR")) {
                this.get_scanR = this.seed;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_restore, container, false);
        setAllTypefaceThin(view);
        Fonts.setTypeface(view.findViewById(R.id.coins_icon), Font.COINOMI_FONT_ICONS);
        this.tv_title_back = (TextView) view.findViewById(R.id.tv_title_back);
        this.tv_title_back.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Bold.ttf"));
        view.findViewById(R.id.iv_back).setOnClickListener(new C11741());
        ((ImageButton) view.findViewById(R.id.scan_qr_code)).setOnClickListener(new C11752());
        ArrayAdapter<String> adapter = new ArrayAdapter(getActivity(), R.layout.item_simple, MnemonicCode.INSTANCE.getWordList());
        this.mnemonicTextView = (MultiAutoCompleteTextView) view.findViewById(R.id.seed);
        this.mnemonicTextView.setAdapter(adapter);
        this.mnemonicTextView.setTokenizer(new C11763());
        this.f80errorMnemonicessage = (TextView) view.findViewById(R.id.restore_message);
        this.f80errorMnemonicessage.setVisibility(8);
        this.bip39Passphrase = (EditText) view.findViewById(R.id.bip39_passphrase);
        this.v_devider = view.findViewById(R.id.v_devider);
        final View bip39PassphraseTitle = view.findViewById(R.id.bip39_passphrase_title);
        this.bip39Passphrase.setVisibility(8);
        bip39PassphraseTitle.setVisibility(8);
        this.v_devider.setVisibility(8);
        final View bip39Info = view.findViewById(R.id.bip39_info);
        bip39Info.setVisibility(8);
        CheckBox useBip39Checkbox = (CheckBox) view.findViewById(R.id.use_bip39);
        if (this.isNewSeed) {
            useBip39Checkbox.setVisibility(8);
        }
        useBip39Checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                RestoreFragment.this.isSeedProtected = isChecked;
                if (isChecked) {
                    if (RestoreFragment.this.isNewSeed) {
                        RestoreFragment.this.skipButton.setVisibility(8);
                    }
                    bip39Info.setVisibility(0);
                    bip39PassphraseTitle.setVisibility(0);
                    RestoreFragment.this.bip39Passphrase.setVisibility(0);
                    RestoreFragment.this.v_devider.setVisibility(0);
                    return;
                }
                if (RestoreFragment.this.isNewSeed) {
                    RestoreFragment.this.skipButton.setVisibility(0);
                }
                bip39Info.setVisibility(8);
                bip39PassphraseTitle.setVisibility(8);
                RestoreFragment.this.bip39Passphrase.setVisibility(8);
                RestoreFragment.this.bip39Passphrase.setText(null);
                RestoreFragment.this.v_devider.setVisibility(8);
            }
        });
        this.skipButton = (TextView) view.findViewById(R.id.seed_entry_skip);
        if (this.isNewSeed) {
            this.skipButton.setOnClickListener(getOnSkipListener());
            this.skipButton.setVisibility(8);
        } else {
            this.skipButton.setVisibility(8);
        }
        if (this.get_scanR != null) {
            this.mnemonicTextView.setText(this.get_scanR);
            verifyMnemonic();
        }
        view.findViewById(R.id.button_next).setOnClickListener(getOnNextListener());
        return view;
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.listener = (Listener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + Listener.class);
        }
    }

    public void onDetach() {
        super.onDetach();
        this.listener = null;
    }

    private OnClickListener getOnNextListener() {
        return new C11785();
    }

    private OnClickListener getOnSkipListener() {
        return new C11796();
    }

    private void verifyMnemonicAndProceed() {
        Keyboard.hideKeyboard(getActivity());
        if (verifyMnemonic()) {
            Bundle args = getArguments();
            if (args == null) {
                args = new Bundle();
            }
            if (!this.isNewSeed && this.isSeedProtected) {
                args.putString("seed_password", this.bip39Passphrase.getText().toString());
            }
            args.putString("seed", this.mnemonicTextView.getText().toString().trim());
            if (this.listener != null) {
                this.listener.onSeedVerified(args);
            }
        }
    }

    private boolean verifyMnemonic() {
        log.info("Verifying seed");
        String seedText = this.mnemonicTextView.getText().toString().trim();
        boolean isSeedValid = false;
        try {
            MnemonicCode.INSTANCE.check(CoreUtils.parseMnemonic(seedText));
            clearError(this.f80errorMnemonicessage);
            isSeedValid = true;
        } catch (MnemonicChecksumException e) {
            log.info("Checksum error in seed: {}", e.getMessage());
            setError(this.f80errorMnemonicessage, R.string.restore_error_checksum, new Object[0]);
        } catch (MnemonicWordException e2) {
            log.info("Unknown words in seed: {}", e2.getMessage());
            setError(this.f80errorMnemonicessage, R.string.restore_error_words, new Object[0]);
        } catch (MnemonicException e3) {
            log.info("Error verifying seed: {}", e3.getMessage());
            setError(this.f80errorMnemonicessage, R.string.restore_error, e3.getMessage());
        }
        if (!isSeedValid || this.seed == null || seedText.equals(this.seed.trim())) {
            return isSeedValid;
        }
        log.info("Typed seed does not match the generated one.");
        setError(this.f80errorMnemonicessage, R.string.restore_error_mismatch, new Object[0]);
        return false;
    }

    private void setError(TextView errorView, int messageId, Object... formatArgs) {
        setError(errorView, getResources().getString(messageId, formatArgs));
    }

    private void setError(TextView errorView, String message) {
        errorView.setText(message);
        showError(errorView);
    }

    private void showError(TextView errorView) {
        errorView.setVisibility(0);
    }

    private void clearError(TextView errorView) {
        errorView.setVisibility(8);
    }

    private void handleScan() {
        startActivityForResult(new Intent(getActivity(), ScanActivity.class), 0);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0 && resultCode == -1) {
            this.mnemonicTextView.setText(intent.getStringExtra("result"));
            verifyMnemonic();
        }
    }

    public void setAllTypefaceThin(View view) {
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getChildCount() != 0) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllTypefaceThin(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Regular.ttf"));
        }
    }
}
