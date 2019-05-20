package com.primestone.wallet.ui;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.coinomi.core.Preconditions;
import com.coinomi.core.wallet.WalletAccount;

import com.primestone.wallet.R;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.ui.widget.MDToast;
import com.primestone.wallet.util.QrUtils;
import com.primestone.wallet.util.UiUtils;

public class AccountDetailsFragment extends Fragment {
    private String publicKeySerialized;

    public AccountDetailsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Preconditions.checkNotNull(getArguments(), "Must provide arguments with an account id.");

        WalletAccount account = ((WalletApplication) getActivity().getApplication()).getAccount(getArguments().getString("account_id"));

        if (account == null) {
            MDToast.makeText(getActivity(), getActivity().getString(R.string.no_such_pocket_error), MDToast.LENGTH_LONG, 3);
            getActivity().finish();
            return;
        }

        publicKeySerialized = account.getPublicKeySerialized();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_details, container, false);
        setAllTypefaceThin(view);
        TextView publicKey = (TextView) view.findViewById(R.id.public_key);
        publicKey.setOnClickListener(getPubKeyOnClickListener());
        publicKey.setText(publicKeySerialized);
        QrUtils.setQr((ImageView) view.findViewById(R.id.qr_code_public_key),
                getResources(), publicKeySerialized);

        return view;
    }

    private OnClickListener getPubKeyOnClickListener() {
        return new  View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = getActivity();
                UiUtils.startCopyShareActionMode(publicKeySerialized, activity);
            }
        };
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
