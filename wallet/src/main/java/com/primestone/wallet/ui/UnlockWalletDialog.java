package com.primestone.wallet.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.primestone.wallet.R;

import mehdi.sakout.fancybuttons.FancyButton;

public class UnlockWalletDialog extends DialogFragment {
    private Listener listener;
    private TextView passwordView;

    public interface Listener {
        void onPassword(CharSequence charSequence);
    }

    class C12791 implements OnClickListener {
        C12791() {
        }

        public void onClick(View v) {
            if (UnlockWalletDialog.this.listener != null) {
                UnlockWalletDialog.this.listener.onPassword(UnlockWalletDialog.this.passwordView.getText());
            }
            UnlockWalletDialog.this.dismissAllowingStateLoss();
        }
    }

    public static DialogFragment getInstance() {
        return new UnlockWalletDialog();
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.listener = (Listener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.getClass() + " must implement " + Listener.class);
        }
    }

    public void onDetach() {
        this.listener = null;
        super.onDetach();
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final Dialog passDialog = new Dialog(getActivity());
        passDialog.requestWindowFeature(1);
        passDialog.setContentView(R.layout.dialog_password);
        passDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        passDialog.setCancelable(false);
        passDialog.setCanceledOnTouchOutside(false);
        FancyButton tv_confirm = (FancyButton) passDialog.findViewById(R.id.tv_confirm);
        FancyButton tv_close = (FancyButton) passDialog.findViewById(R.id.tv_close);
        this.passwordView = (EditText) passDialog.findViewById(R.id.password);
        ((ProgressBar) passDialog.findViewById(R.id.process)).setVisibility(8);
        setAllTypefaceThin(passDialog.findViewById(R.id.root_layout));
        setAllTypefaceBold(tv_confirm);
        setAllTypefaceBold(tv_close);
        tv_confirm.setOnClickListener(new C12791());
        tv_close.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                passDialog.dismiss();
                UnlockWalletDialog.this.dismissAllowingStateLoss();
            }
        });
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        passDialog.getWindow().setLayout(width - (width / 10), -2);
        return passDialog;
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
