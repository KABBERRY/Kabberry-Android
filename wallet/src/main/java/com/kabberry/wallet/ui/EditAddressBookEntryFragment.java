package com.kabberry.wallet.ui;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.coinomi.core.coins.CoinID;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractAddress;

import com.kabberry.wallet.AddressBookProvider;
import com.kabberry.wallet.R;
import mehdi.sakout.fancybuttons.FancyButton;

public final class EditAddressBookEntryFragment extends DialogFragment {
    private static final String FRAGMENT_TAG = EditAddressBookEntryFragment.class.getName();
    private ContentResolver contentResolver;
    private Context context;

    public static void edit(FragmentManager fm, AbstractAddress address) {
        edit(fm, address.getType(), address, null);
    }

    public static void edit(FragmentManager fm, CoinType type, AbstractAddress address) {
        edit(fm, type, address, null);
    }

    public static void edit(FragmentManager fm, CoinType type, AbstractAddress address, String suggestedAddressLabel) {
        instance(type, address, suggestedAddressLabel).show(fm, FRAGMENT_TAG);
    }

    private static EditAddressBookEntryFragment instance(CoinType type, AbstractAddress address, String suggestedAddressLabel) {
        EditAddressBookEntryFragment fragment = new EditAddressBookEntryFragment();
        Bundle args = new Bundle();
        args.putString("coin_id", type.getId());
        args.putSerializable("address", address);
        args.putString("suggested_address_label", suggestedAddressLabel);
        fragment.setArguments(args);
        return fragment;
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        this.contentResolver = context.getContentResolver();
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        CoinType type = CoinID.typeFromId(args.getString("coin_id"));
        AbstractAddress address = (AbstractAddress) args.getSerializable("address");
        String suggestedAddressLabel = args.getString("suggested_address_label");
        LayoutInflater inflater = LayoutInflater.from(this.context);
        final Uri uri = AddressBookProvider.contentUri(this.context.getPackageName(), type).buildUpon().appendPath(address.toString()).build();
        String label = AddressBookProvider.resolveLabel(this.context, address);
        final boolean isAdd = label == null;
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(1);
        dialog.setContentView(R.layout.edit_address_book_entry_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        setAllTypefaceThin(dialog.findViewById(R.id.root_layout));
        setAllTypefaceBold(dialog.findViewById(R.id.tv_title));
        setAllTypefaceBold(dialog.findViewById(R.id.tv_confirm));
        setAllTypefaceBold(dialog.findViewById(R.id.tv_delete));
        ((TextView) dialog.findViewById(R.id.edit_address_book_entry_address)).setText(GenericUtils.addressSplitToGroups(address));
        final TextView viewLabel = (TextView) dialog.findViewById(R.id.edit_address_book_entry_label);
        if (label == null) {
            label = suggestedAddressLabel;
        }
        viewLabel.setText(label);
        FancyButton tv_confirm = (FancyButton) dialog.findViewById(R.id.tv_confirm);
        ImageView iv_close = (ImageView) dialog.findViewById(R.id.iv_close);
        FancyButton tv_delete = (FancyButton) dialog.findViewById(R.id.tv_delete);
        tv_delete.setVisibility(8);
        if (!isAdd) {
            tv_delete.setVisibility(0);
        }
        tv_confirm.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String newLabel = viewLabel.getText().toString().trim();
                if (!newLabel.isEmpty()) {
                    ContentValues values = new ContentValues();
                    values.put("label", newLabel);
                    if (isAdd) {
                        EditAddressBookEntryFragment.this.contentResolver.insert(uri, values);
                    } else {
                        EditAddressBookEntryFragment.this.contentResolver.update(uri, values, null, null);
                    }
                } else if (!isAdd) {
                    EditAddressBookEntryFragment.this.contentResolver.delete(uri, null, null);
                }
                dialog.dismiss();
            }
        });
        iv_close.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        tv_delete.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                EditAddressBookEntryFragment.this.contentResolver.delete(uri, null, null);
                dialog.dismiss();
            }
        });
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        dialog.getWindow().setLayout(width - (width / 10), -2);
        return dialog;
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
