package com.kabberry.wallet.ui;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kabberry.wallet.R;
import com.kabberry.wallet.WalletApplication;
import com.kabberry.wallet.ui.SetPasswordFragment.Listener;

import mehdi.sakout.fancybuttons.FancyButton;

public class IntroActivity extends AbstractWalletFragmentActivity
        implements Listener, WelcomeFragment.Listener {
    Dialog dialog;

    /*
    class C11401 implements OnClickListener {
        C11401() {
        }

        public void onClick(DialogInterface dialog, int which) {
            IntroActivity.this.finish();
        }
    }
*/
    class C11478 implements View.OnClickListener {
        C11478() {
        }

        public void onClick(View v) {
            IntroActivity.this.dialog.dismiss();
        }
    }

    class C11489 implements View.OnClickListener {
        C11489() {
        }

        public void onClick(View v) {
            IntroActivity.this.dialog.dismiss();
            IntroActivity.this.finish();
            IntroActivity.this.overridePendingTransition(0, R.anim.scale_old_tv);
            System.exit(0);
        }
    }

    @TargetApi(21)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.fragment_intro);

        /*
        if (VERSION.SDK_INT > 21) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }
        */
        if (!getWalletApplication().getConfiguration().isDeviceCompatible()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.incompatible_device_warning_title)
                    .setMessage(R.string.incompatible_device_warning_message)
                    //.setPositiveButton(R.string.button_ok, new C11401()).setCancelable(false).create().show();
                    .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();
        } else if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new WelcomeFragment())
                    .commit();
        }
    }

    protected void onResume() {
        super.onResume();
        WalletApplication.getInstance().stopLogoutTimer(getApplicationContext());
    }

    protected void onPause() {
        super.onPause();
        if (getWalletApplication().getWallet() != null) {
            WalletApplication.getInstance().startLogoutTimer(getApplicationContext());
        }
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onCreateNewWallet() {
        if (getWalletApplication().getWallet() == null) {
            replaceFragment(new SeedFragment());
        }
        else {
            final Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(1);
            dialog.setContentView(R.layout.popup_wallet_warning);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(false);

            FancyButton tv_confirm = (FancyButton) dialog.findViewById(R.id.tv_confirm);
            FancyButton tv_cancel = (FancyButton) dialog.findViewById(R.id.tv_cancel);

            setAllTypefaceThin(dialog.findViewById(R.id.root_layout));
            setAllTypefaceBold(dialog.findViewById(R.id.lbl_sub_title));
            setAllTypefaceBold(dialog.findViewById(R.id.tv_confirm));
            setAllTypefaceBold(dialog.findViewById(R.id.tv_cancel));

            tv_cancel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            tv_confirm.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialog.dismiss();
                    IntroActivity.this.replaceFragment(new SeedFragment());
                }
            });

            int width = getResources().getDisplayMetrics().widthPixels;
            dialog.show();
            dialog.getWindow().setLayout(width - (width / 10), -2);
        }
    }

    public void onRestoreWallet() {
        if (getWalletApplication().getWallet() == null) {
            replaceFragment(RestoreFragment.newInstance());
            return;
        }
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(1);
        dialog.setContentView(R.layout.popup_wallet_warning);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        FancyButton tv_confirm = (FancyButton) dialog.findViewById(R.id.tv_confirm);
        FancyButton tv_cancel = (FancyButton) dialog.findViewById(R.id.tv_cancel);
        setAllTypefaceThin(dialog.findViewById(R.id.root_layout));
        setAllTypefaceBold(dialog.findViewById(R.id.lbl_sub_title));
        setAllTypefaceBold(dialog.findViewById(R.id.tv_confirm));
        setAllTypefaceBold(dialog.findViewById(R.id.tv_cancel));
        tv_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        tv_confirm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                IntroActivity.this.replaceFragment(RestoreFragment.newInstance());
            }
        });
        int width = getResources().getDisplayMetrics().widthPixels;
        dialog.show();
        dialog.getWindow().setLayout(width - (width / 10), -2);
    }

    public void onRestoreWallet_QR(final String sqr_seed) {
        if (getWalletApplication().getWallet() == null) {
            replaceFragment(RestoreFragment.newInstance(sqr_seed, Boolean.valueOf(true)));
            return;
        }

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(1);
        dialog.setContentView(R.layout.popup_wallet_warning);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        FancyButton tv_confirm = (FancyButton) dialog.findViewById(R.id.tv_confirm);
        FancyButton tv_cancel = (FancyButton) dialog.findViewById(R.id.tv_cancel);
        setAllTypefaceThin(dialog.findViewById(R.id.root_layout));
        setAllTypefaceBold(dialog.findViewById(R.id.lbl_sub_title));
        setAllTypefaceBold(dialog.findViewById(R.id.tv_confirm));
        setAllTypefaceBold(dialog.findViewById(R.id.tv_cancel));

        tv_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        tv_confirm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                IntroActivity.this.replaceFragment(RestoreFragment.newInstance(sqr_seed));
            }
        });

        dialog.show();
    }

    public void onSeedVerified(Bundle args) {
        replaceFragment(SetPasswordFragment.newInstance(args));
    }

    public void onPasswordSet(Bundle args) {
        selectCoins(args);
    }

    private void selectCoins(Bundle args) {
        replaceFragment(FinalizeWalletRestorationFragment.newInstance(args));
    }

    protected void setAllTypefaceThin(View view) {
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getChildCount() != 0) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllTypefaceThin(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf"));
        }
    }

    protected void setAllTypefaceBold(View view) {
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getChildCount() != 0) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllTypefaceBold(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf"));
        }
    }

    public void onBackPressed() {
        if (getWalletApplication().getWallet() == null && (getSupportFragmentManager().findFragmentById(R.id.container) instanceof WelcomeFragment)) {
            this.dialog = new Dialog(this);
            this.dialog.requestWindowFeature(1);
            this.dialog.setContentView(R.layout.popup_exit);
            this.dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            this.dialog.setCancelable(true);
            this.dialog.setCanceledOnTouchOutside(false);

            FancyButton tv_logout = (FancyButton) this.dialog.findViewById(R.id.tv_logout);
            FancyButton tv_cancel = (FancyButton) this.dialog.findViewById(R.id.tv_cancel);

            setAllTypefaceThin(this.dialog.findViewById(R.id.root_layout));
            tv_cancel.setOnClickListener(new C11478());
            tv_logout.setOnClickListener(new C11489());
            this.dialog.show();
            return;
        }
        super.onBackPressed();
    }
}
