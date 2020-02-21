package com.kabberry.wallet.ui;

import android.os.Bundle;

import com.kabberry.wallet.R;
import com.kabberry.wallet.ui.UnlockWalletDialog.Listener;

public class SignVerifyMessageActivity extends BaseWalletActivity implements Listener {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_fragment_wrapper);
        if (savedInstanceState == null) {
            SignVerifyMessageFragment fragment = new SignVerifyMessageFragment();
            fragment.setArguments(getIntent().getExtras());
            getFM().beginTransaction().add(R.id.container, fragment, "sign_verify_fragment").commit();
        }
    }

    public void onPassword(CharSequence password) {
        SignVerifyMessageFragment f = (SignVerifyMessageFragment) getFM().findFragmentByTag("sign_verify_fragment");
        if (f != null) {
            f.maybeStartSigningTask(password);
        }
    }
}
