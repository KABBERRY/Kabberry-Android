package com.primestone.wallet.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.coinomi.core.exceptions.Bip44KeyLookAheadExceededException;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.WalletAccount;
import com.coinomi.core.wallet.WalletPocketHD;

import com.primestone.wallet.AddressBookProvider;
import com.primestone.wallet.R;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.ui.widget.MDToast;

import javax.annotation.Nullable;

import mehdi.sakout.fancybuttons.FancyButton;

public class CreateNewAddressDialog extends DialogFragment {
    private WalletApplication app;
    @Nullable private ContentResolver resolver;

    public static DialogFragment getInstance(WalletAccount account) {
        DialogFragment dialog = new CreateNewAddressDialog();
        dialog.setArguments(new Bundle());
        dialog.getArguments().putString("account_id", account.getId());
        return dialog;
    }

    public CreateNewAddressDialog() { }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.app = (WalletApplication) activity.getApplication();
        this.resolver = activity.getContentResolver();
    }

    @Override
    public void onDetach() {
        this.resolver = null;
        super.onDetach();
    }

    @Override
    public Dialog onCreateDialog(Bundle state) {
        DialogInterface.OnClickListener dismissListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissAllowingStateLoss();
            }
        };

        WalletAccount account = app.getAccount(getArguments().getString("account_id"));

        if (account == null || !(account instanceof WalletPocketHD)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.error_generic).setPositiveButton(R.string.button_ok, dismissListener);
            return builder.create();
        }

        final WalletPocketHD pocketHD = (WalletPocketHD) account;

        if (pocketHD.canCreateFreshReceiveAddress()) {
            final Dialog dialog = new Dialog(getActivity());
            dialog.requestWindowFeature(1);
            dialog.setContentView(R.layout.new_address_dialog);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);

            final TextView viewLabel = (TextView) dialog.findViewById(R.id.new_address_label);
            setAllTypefaceThin(dialog.findViewById(R.id.root_layout));
            setAllTypefaceBold(dialog.findViewById(R.id.tv_title));
            setAllTypefaceBold(dialog.findViewById(R.id.tv_title));
            setAllTypefaceBold(dialog.findViewById(R.id.tv_title));

            //ImageView iv_close = (ImageView) dialog.findViewById(R.id.tv_close);
            ((FancyButton) dialog.findViewById(R.id.tv_confirm)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    createAddress(pocketHD, viewLabel.getText().toString().trim());
                    dismissAllowingStateLoss();
                }
            });

            ((FancyButton) dialog.findViewById(R.id.tv_close)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    CreateNewAddressDialog.this.dismissAllowingStateLoss();
                }
            });

            DisplayMetrics displayMetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = displayMetrics.widthPixels;
            dialog.getWindow().setLayout(width - (width / 10), -2);
            return dialog;
        }
        Builder builder = new Builder(getActivity());
        builder.setMessage(R.string.too_many_unused_addresses).setPositiveButton(R.string.button_ok, dismissListener);

        return builder.create();
    }

    private void createAddress(WalletPocketHD account, String newLabel) {
        if (account.canCreateFreshReceiveAddress()) {
            try {
                AbstractAddress newAddress = account.getFreshReceiveAddress(this.app.getConfiguration().isManualAddressManagement());
                if (newLabel != null && !newLabel.isEmpty()) {
                    Uri uri = AddressBookProvider.contentUri(getActivity().getPackageName(), account.getCoinType()).buildUpon().appendPath(newAddress.toString()).build();
                    ContentValues values = new ContentValues();
                    values.put("label", newLabel);
                    if (this.resolver != null) {
                        this.resolver.insert(uri, values);
                        return;
                    }
                    return;
                }
                return;
            } catch (Bip44KeyLookAheadExceededException e) {
                MDToast.makeText(getActivity(), getActivity().getString(R.string.too_many_unused_addresses), MDToast.LENGTH_LONG, 0);
                return;
            }
        }
        MDToast.makeText(getActivity(), getActivity().getString(R.string.too_many_unused_addresses), MDToast.LENGTH_LONG, 0);
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
}
