package com.kabberry.wallet.ui;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

public class Dialogs {

    public static class ProgressDialogFragment extends DialogFragment {
        public static void show(FragmentManager fm, String string, String tag) {
            Dialogs.showDialog(fm, new ProgressDialogFragment(), string, tag);
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ProgressDialog dialog = new ProgressDialog(getActivity());
            dialog.setProgressStyle(0);
            dialog.setMessage(getArguments().getString("message"));
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }
    }

    public static DialogFragment setMessage(DialogFragment newDialog, String message) {
        if (newDialog.getArguments() == null) {
            newDialog.setArguments(new Bundle());
        }
        newDialog.getArguments().putString("message", message);
        return newDialog;
    }

    public static void showDialog(FragmentManager fm, DialogFragment dialog, String string, String tag) {
        setMessage(dialog, string).show(fm, tag);
    }

    public static boolean dismissAllowingStateLoss(FragmentManager fm, String tag) {
        if (fm == null) {
            return true;
        }

        DialogFragment dialog = (DialogFragment) fm.findFragmentByTag(tag);

        if (dialog != null) {
            dialog.dismissAllowingStateLoss();
        }
        return false;
    }
}
