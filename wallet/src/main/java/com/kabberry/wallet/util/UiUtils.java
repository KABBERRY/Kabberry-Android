package com.kabberry.wallet.util;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Build.VERSION;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ShareCompat.IntentBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.view.ActionMode.Callback;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.coinomi.core.uri.CoinURI;
import com.coinomi.core.uri.CoinURIParseException;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.WalletAccount;

import com.kabberry.wallet.AddressBookProvider;
import com.kabberry.wallet.R;
import com.kabberry.wallet.ui.AccountDetailsActivity;
import com.kabberry.wallet.ui.EditAccountFragment;
import com.kabberry.wallet.ui.EditAccountFragment.Listener;
import com.kabberry.wallet.ui.EditAddressBookEntryFragment;
import com.kabberry.wallet.ui.widget.MDToast;

import org.acra.ACRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UiUtils {
    private static final Logger log = LoggerFactory.getLogger(UiUtils.class);

    public static class AddressActionModeCallback implements Callback {
        private final AbstractAddress address;
        private final Context context;
        private final FragmentManager fragmentManager;

        public AddressActionModeCallback(AbstractAddress address, Context context, FragmentManager fragmentManager) {
            this.address = address;
            this.context = context;
            this.fragmentManager = fragmentManager;
        }

        public AbstractAddress getAddress() {
            return this.address;
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.address_options, menu);
            CharSequence label = AddressBookProvider.resolveLabel(this.context, this.address);
            if (label == null) {
                label = GenericUtils.addressSplitToGroups(this.address);
            }
            mode.setTitle(label);
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_edit_label:
                    EditAddressBookEntryFragment.edit(this.fragmentManager, this.address);
                    mode.finish();
                    return true;
                case R.id.action_copy:
                    UiUtils.copy(this.context, CoinURI.convertToCoinURI(this.address));
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        public void onDestroyActionMode(ActionMode actionMode) {
        }
    }

    public static class AccountActionModeCallback implements Callback {
        private final WalletAccount account;
        private final Activity activity;
        private final FragmentManager fragmentManager;

        class C13051 implements OnClickListener {
            C13051() {
            }

            public void onClick(DialogInterface dialog, int which) {
                /*
                AccountActionModeCallback.this.account.getWallet().deleteAccount(AccountActionModeCallback.this.account.getId());
                if (AccountActionModeCallback.this.activity instanceof Listener) {
                    ((Listener) AccountActionModeCallback.this.activity).onAccountModified(AccountActionModeCallback.this.account);
                }
                */
            }
        }

        public AccountActionModeCallback(WalletAccount account, Activity activity, FragmentManager fragmentManager) {
            this.account = account;
            this.activity = activity;
            this.fragmentManager = fragmentManager;
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.account_options, menu);
            mode.setTitle(this.account.getDescriptionOrCoinName());
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
            /*
            switch (menuItem.getItemId()) {
                case R.id.action_edit_description:
                    EditAccountFragment.edit(this.fragmentManager, this.account);
                    mode.finish();
                    return true;
                case R.id.action_delete:
                    new Builder(this.activity).setTitle(this.activity.getString(R.string.edit_account_delete_title, new Object[]{this.account.getDescriptionOrCoinName()})).setMessage(C1077R.string.edit_account_delete_description).setNegativeButton(R.string.button_cancel, null).setPositiveButton(R.string.button_ok, new C13051()).create().show();
                    mode.finish();
                    return true;
                case R.id.action_account_details:
                    Intent intent = new Intent(this.activity, AccountDetailsActivity.class);
                    intent.putExtra("account_id", this.account.getId());
                    this.activity.startActivity(intent);
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        */
            return false;
        }

        public void onDestroyActionMode(ActionMode actionMode) {
        }
    }

    public static class CopyShareActionModeCallback implements Callback {
        private final Activity activity;
        private final String string;

        public CopyShareActionModeCallback(String string, Activity activity) {
            this.string = string;
            this.activity = activity;
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.copy_share_options, menu);
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_copy:
                    UiUtils.copy(this.activity, this.string);
                    mode.finish();
                    return true;
                case R.id.action_share:
                    UiUtils.share(this.activity, this.string);
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        public void onDestroyActionMode(ActionMode actionMode) {
        }
    }

    public static void toastGenericError(Context context) {
        MDToast.makeText(context, context.getString(R.string.error_generic), MDToast.LENGTH_LONG, 3);
    }

    public static void replyAddressRequest(Activity activity, CoinURI uri, WalletAccount pocket) throws CoinURIParseException {
        try {
            activity.startActivity(Intent.parseUri(uri.getAddressRequestUriResponse(pocket.getReceiveAddress()).toString(), 0));
        } catch (Exception e) {
            ACRA.getErrorReporter().handleSilentException(e);
            MDToast.makeText(activity, activity.getString(R.string.error_generic), MDToast.LENGTH_LONG, 3);
        }
    }

    public static void share(Activity activity, String text) {
        IntentBuilder builder;
        String[] final_str = text.split(":");
        if (final_str.length == 2) {
            builder = IntentBuilder.from(activity).setType("text/plain").setText(final_str[1]);
        } else {
            builder = IntentBuilder.from(activity).setType("text/plain").setText(final_str[0]);
        }
        activity.startActivity(Intent.createChooser(builder.getIntent(), activity.getString(R.string.action_share)));
    }

    public static void copy(Context context, String string) {
        Object clipboardService = context.getSystemService("clipboard");
        String[] final_str = string.split(":");
        try {
            if (VERSION.SDK_INT >= 11) {
                ClipboardManager clipboard = (ClipboardManager) clipboardService;
                if (final_str.length == 2) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("simple text", final_str[1]));
                } else {
                    clipboard.setPrimaryClip(ClipData.newPlainText("simple text", final_str[0]));
                }
            } else {
                android.text.ClipboardManager clipboard2 = (android.text.ClipboardManager) clipboardService;
                if (final_str.length == 2) {
                    clipboard2.setText(final_str[1]);
                } else {
                    clipboard2.setText(final_str[0]);
                }
            }
            MDToast.makeText(context, context.getString(R.string.copied_to_clipboard), MDToast.LENGTH_SHORT, 0).show();
        } catch (Exception e) {
            ACRA.getErrorReporter().handleSilentException(e);
            MDToast.makeText(context, context.getString(R.string.error_generic), MDToast.LENGTH_LONG, 3);
        }
    }

    public static void setVisible(View view) {
        setVisibility(view, 0);
    }

    public static void setInvisible(View view) {
        setVisibility(view, 4);
    }

    public static void setGone(View view) {
        setVisibility(view, 8);
    }

    public static void setVisibility(View view, int visibility) {
        if (view.getVisibility() != visibility) {
            view.setVisibility(visibility);
        }
    }

    public static ActionMode startActionMode(Activity activity, Callback callback) {
        if (activity != null && (activity instanceof AppCompatActivity)) {
            return ((AppCompatActivity) activity).startSupportActionMode(callback);
        }
        log.warn("To show action mode, your activity must extend " + AppCompatActivity.class);
        return null;
    }

    public static ActionMode startAddressActionMode(AbstractAddress address, Activity activity, FragmentManager fragmentManager) {
        return startActionMode(activity, new AddressActionModeCallback(address, activity, fragmentManager));
    }

    public static ActionMode startCopyShareActionMode(String string, Activity activity) {
        return startActionMode(activity, new CopyShareActionModeCallback(string, activity));
    }

    public static ActionMode startAccountActionMode(WalletAccount account, Activity activity, FragmentManager fragmentManager) {
        return startActionMode(activity, new AccountActionModeCallback(account, activity, fragmentManager));
    }
}
