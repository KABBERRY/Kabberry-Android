package com.primestone.wallet.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.ButterKnife;

import com.coinomi.core.Preconditions;
import com.coinomi.core.wallet.WalletAccount;

import com.primestone.wallet.R;
import com.primestone.wallet.WalletApplication;

public final class EditAccountFragment extends DialogFragment {

    private static final String FRAGMENT_TAG = EditAccountFragment.class.getName();
    private WalletApplication app;
    private Context context;

    private Listener listener;

    public interface Listener {
        void onAccountModified(WalletAccount walletAccount);
    }

    public static void edit(FragmentManager fm, WalletAccount account) {
        instance(account).show(fm, FRAGMENT_TAG);
    }

    private static EditAccountFragment instance(WalletAccount account) {
        EditAccountFragment fragment = new EditAccountFragment();
        Bundle args = new Bundle();
        args.putString("account_id", account.getId());
        fragment.setArguments(args);
        return fragment;
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        this.app = (WalletApplication) context.getApplicationContext();
        try {
            this.listener = (Listener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + Listener.class);
        }
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final WalletAccount account = (WalletAccount) Preconditions.checkNotNull(this.app.getAccount(getArguments().getString("account_id")));
        LayoutInflater inflater = LayoutInflater.from(this.context);
        DialogBuilder dialog = new DialogBuilder(this.context);
        View view = inflater.inflate(R.layout.edit_account_dialog, null);
        final EditText descriptionView = (EditText) ButterKnife.findById(view, (int) R.id.edit_account_description);
        descriptionView.setText(account.getDescription());
        descriptionView.setHint(account.getCoinType().getName());
        setAllTypefaceThin(view);
        dialog.setTitle((int) R.string.edit_account_title);
        dialog.setView(view);
        OnClickListener onClickListener = new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == -1) {
                    account.setDescription(descriptionView.getText().toString().trim());
                    if (EditAccountFragment.this.listener != null) {
                        EditAccountFragment.this.listener.onAccountModified(account);
                    }
                }
                EditAccountFragment.this.dismiss();
            }
        };
        dialog.setPositiveButton((int) R.string.button_save, onClickListener);
        dialog.setNegativeButton((int) R.string.button_cancel, onClickListener);
        return dialog.create();
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
