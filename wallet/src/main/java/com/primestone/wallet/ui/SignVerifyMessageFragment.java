package com.primestone.wallet.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.wallet.SignedMessage;
import com.coinomi.core.wallet.SignedMessage.Status;
import com.coinomi.core.wallet.WalletAccount;

import com.primestone.wallet.R;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.tasks.SignVerifyMessageTask;
import com.primestone.wallet.util.Keyboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignVerifyMessageFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(SignVerifyMessageFragment.class);
    private WalletAccount account;
    private TextView addressError;
    private WalletApplication application;
    private EditText messageView;
    private Button signButton;
    private SignVerifyMessageTask signVerifyMessageTask;
    private TextView signatureError;
    private TextView signatureOK;
    private EditText signatureView;
    private AutoCompleteTextView signingAddressView;
    private CoinType type;
    private Button verifyButton;

    class C12541 implements OnClickListener {
        C12541() {
        }

        public void onClick(View v) {
            SignVerifyMessageFragment.this.clearFocusAndHideKeyboard();
            SignVerifyMessageFragment.this.verifyMessage();
        }
    }

    class C12552 implements OnClickListener {
        C12552() {
        }

        public void onClick(View v) {
            SignVerifyMessageFragment.this.clearFocusAndHideKeyboard();
            SignVerifyMessageFragment.this.signMessage();
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey("account_id")) {
                this.account = (WalletAccount) Preconditions.checkNotNull(this.application.getAccount(args.getString("account_id")));
            }
            Preconditions.checkNotNull(this.account, "No account selected");
            this.type = this.account.getCoinType();
            return;
        }
        throw new RuntimeException("Must provide account ID");
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_message, container, false);
        setAllTypefaceThin(view);
        this.signingAddressView = (AutoCompleteTextView) view.findViewById(R.id.signing_address);
        this.signingAddressView.setAdapter(new ArrayAdapter(getActivity(), R.layout.item_simple, this.account.getActiveAddresses()));
        this.messageView = (EditText) view.findViewById(R.id.message);
        this.signatureView = (EditText) view.findViewById(R.id.signature);
        this.addressError = (TextView) view.findViewById(R.id.address_error_message);
        this.signatureOK = (TextView) view.findViewById(R.id.signature_ok);
        this.signatureError = (TextView) view.findViewById(R.id.signature_error);
        this.verifyButton = (Button) view.findViewById(R.id.button_verify);
        this.verifyButton.setOnClickListener(new C12541());
        this.signButton = (Button) view.findViewById(R.id.button_sign);
        this.signButton.setOnClickListener(new C12552());
        return view;
    }

    private void clearFocusAndHideKeyboard() {
        this.signingAddressView.clearFocus();
        this.messageView.clearFocus();
        this.signatureView.clearFocus();
        Keyboard.hideKeyboard(getActivity());
    }

    private void signMessage() {
        if (this.account.isEncrypted()) {
            showUnlockDialog();
        } else {
            maybeStartSigningTask();
        }
    }

    private void verifyMessage() {
        maybeStartVerifyingTask();
    }

    private void showUnlockDialog() {
        Dialogs.dismissAllowingStateLoss(getFragmentManager(), "password_dialog_tag");
        UnlockWalletDialog.getInstance().show(getFragmentManager(), "password_dialog_tag");
    }

    private void clearMessages() {
        this.addressError.setVisibility(8);
        this.signatureOK.setVisibility(8);
        this.signatureError.setVisibility(8);
    }

    private void showSignVerifyStatus(SignedMessage signedMessage) {
        clearMessages();
        switch (signedMessage.getStatus()) {
            case SignedOK:
                this.signatureView.setText(signedMessage.getSignature());
                this.signatureOK.setVisibility(0);
                this.signatureOK.setText(R.string.message_signed);
                return;
            case VerifiedOK:
                this.signatureOK.setVisibility(0);
                this.signatureOK.setText(R.string.message_verified);
                return;
            default:
                showSignVerifyError(signedMessage.getStatus());
                return;
        }
    }

    private void showSignVerifyError(Status status) {
        boolean z = true;
        Preconditions.checkState(status != Status.SignedOK);
        if (status == Status.VerifiedOK) {
            z = false;
        }
        Preconditions.checkState(z);
        clearMessages();
        switch (status) {
            case AddressMalformed:
                this.addressError.setVisibility(0);
                this.addressError.setText(R.string.address_error);
                return;
            case KeyIsEncrypted:
                this.addressError.setVisibility(0);
                this.addressError.setText(R.string.wallet_locked_message);
                return;
            case MissingPrivateKey:
                this.addressError.setVisibility(0);
                this.addressError.setText(R.string.address_not_found_error);
                return;
            case InvalidSigningAddress:
            case InvalidMessageSignature:
                this.signatureError.setVisibility(0);
                this.signatureError.setText(R.string.message_verification_failed);
                return;
            default:
                this.signatureError.setVisibility(0);
                this.signatureError.setText(R.string.error_generic);
                return;
        }
    }

    private void maybeStartSigningTask() {
        maybeStartSigningTask(null);
    }

    public void maybeStartSigningTask(CharSequence password) {
        maybeStartSigningVerifyingTask(true, password);
    }

    private void maybeStartVerifyingTask() {
        maybeStartSigningVerifyingTask(false, null);
    }

    private void maybeStartSigningVerifyingTask(boolean sign, CharSequence password) {
        if (this.signVerifyMessageTask == null) {
            SignedMessage signedMessage;
            String address = this.signingAddressView.getText().toString().trim();
            String message = this.messageView.getText().toString();
            String signature = this.signatureView.getText().toString();
            if (sign) {
                signedMessage = new SignedMessage(address, message);
            } else {
                signedMessage = new SignedMessage(address, message, signature);
            }
            this.signVerifyMessageTask = new SignVerifyMessageTask(this.account, sign, password) {
                protected void onPostExecute(SignedMessage message) {
                    SignVerifyMessageFragment.this.showSignVerifyStatus(message);
                    SignVerifyMessageFragment.this.signVerifyMessageTask = null;
                }
            };
            this.signVerifyMessageTask.execute(new SignedMessage[]{signedMessage});
        }
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        this.application = (WalletApplication) context.getApplicationContext();
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
