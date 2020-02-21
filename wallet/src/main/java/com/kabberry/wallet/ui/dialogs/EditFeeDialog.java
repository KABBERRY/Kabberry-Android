package com.kabberry.wallet.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinID;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.ValueType;

import com.kabberry.wallet.R;
import com.kabberry.wallet.Configuration;
import com.kabberry.wallet.WalletApplication;
import com.kabberry.wallet.ui.widget.AmountEditView;
import com.kabberry.wallet.ui.widget.MDToast;

import mehdi.sakout.fancybuttons.FancyButton;

public class EditFeeDialog extends DialogFragment {
    Configuration configuration;

    Value fee;
    Resources resources;
    CoinType type;

    @Bind(R.id.fee_description) TextView description;
    @Bind(R.id.fee_amount) AmountEditView feeAmount;
    @Bind(R.id.tv_title) TextView tv_title;

    public static EditFeeDialog newInstance(ValueType type) {
        EditFeeDialog dialog = new EditFeeDialog();
        dialog.setArguments(new Bundle());
        dialog.getArguments().putString("coin_id", type.getId());
        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        WalletApplication application = (WalletApplication) activity.getApplication();
        this.configuration = application.getConfiguration();
        this.resources = application.getResources();
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String feePolicy;
        Preconditions.checkState(getArguments().containsKey("coin_id"), "Must provide coin id");
        Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(1);
        dialog.setContentView(R.layout.edit_fee_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        ButterKnife.bind((Object) this, dialog);
        setAllTypefaceThin(dialog.findViewById(R.id.root_layout));
        setAllTypefaceBold(dialog.findViewById(R.id.tv_confirm));
        setAllTypefaceBold(dialog.findViewById(R.id.tv_title));
        setAllTypefaceBold(dialog.findViewById(R.id.tv_close));
        this.feeAmount.setSingleLine(true);
        this.type = CoinID.typeFromId(getArguments().getString("coin_id"));
        this.feeAmount.resetType(this.type);
        switch (this.type.getFeePolicy()) {
            case FEE_PER_KB:
                feePolicy = this.resources.getString(R.string.tx_fees_per_kilobyte);
                break;
            case FLAT_FEE:
                feePolicy = this.resources.getString(R.string.tx_fees_per_transaction);
                break;
            default:
                throw new RuntimeException("Unknown fee policy " + this.type.getFeePolicy());
        }
        this.description.setText(this.resources.getString(R.string.tx_fees_description, new Object[]{feePolicy}));
        this.fee = this.configuration.getFeeValue(this.type);
        this.feeAmount.setAmount(this.fee, false);
        this.tv_title.setText(this.resources.getString(R.string.tx_fees_title, new Object[]{this.type.getName()}));
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        dialog.getWindow().setLayout(width - (width / 10), -2);
        return dialog;
    }

    @OnClick(R.id.tv_default)
    public void onDefaultClick(FancyButton fancyButton) {
        this.configuration.resetFeeValue(this.type);
        dismiss();
    }

    @SuppressLint({"LongLogTag"})
    @OnClick(R.id.tv_confirm)
    public void onOkClick(FancyButton fancyButton) {
        Value newFee = this.feeAmount.getAmount();
        if (newFee != null && Double.parseDouble(this.feeAmount.getAmountText()) < 1.0E-4d) {
            MDToast.makeText(getActivity(), getString(R.string.fee_limit), MDToast.LENGTH_SHORT, 0);
        } else if (newFee == null || newFee.equals(this.fee)) {
            dismiss();
        } else {
            this.configuration.setFeeValue(newFee);
            dismiss();
        }
    }

    @OnClick(R.id.iv_close)
    public void onCancelClick(ImageView imageView) {
        dismiss();
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
