package com.primestone.wallet.ui;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.coinomi.core.coins.PrimestoneMain;
import com.coinomi.core.coins.Value;

import com.primestone.wallet.R;
import com.primestone.wallet.Configuration;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.ui.adaptors.FeesListAdapter;
import com.primestone.wallet.ui.dialogs.EditFeeDialog;
import com.primestone.wallet.ui.widget.MDToast;

import mehdi.sakout.fancybuttons.FancyButton;

public class SettingsFragment extends Fragment implements OnSharedPreferenceChangeListener {
    private FeesListAdapter adapter;
    AlertDialog alertDialog = null;

    @Bind(R.id.cb_pref_receive_address) CheckBox cb_pref_receive_address;
    @Bind(R.id.coins_list) ListView coinList;
    @Bind(R.id.tv_fee) TextView tv_fee;

    private Configuration config;
    private Context context;
    public EditText et_cnf_pass;
    public EditText et_new_pass;
    public EditText et_old_pass;

    Listener listener;

    String symbol;

    class C12443 implements OnClickListener {
        C12443() {
        }

        public void onClick(View v) {
            if (SettingsFragment.this.alertDialog != null && SettingsFragment.this.alertDialog.isShowing()) {
                SettingsFragment.this.alertDialog.cancel();
            }
        }
    }

    class C12454 implements OnClickListener {
        C12454() {
        }

        public void onClick(View v) {
            if (SettingsFragment.this.et_old_pass.getText().toString().isEmpty()) {
                SettingsFragment.this.et_old_pass.setError(SettingsFragment.this.getString(R.string.empty_field));
            } else if (SettingsFragment.this.et_new_pass.getText().toString().isEmpty()) {
                SettingsFragment.this.et_new_pass.setError(SettingsFragment.this.getString(R.string.empty_field));
            } else if (SettingsFragment.this.et_new_pass.getText().toString().equalsIgnoreCase(SettingsFragment.this.et_cnf_pass.getText().toString())) {
                SettingsFragment.this.et_old_pass.setError(null);
                SettingsFragment.this.et_new_pass.setError(null);
                SettingsFragment.this.et_cnf_pass.setError(null);
                if (SettingsFragment.this.config.GetPassword().equalsIgnoreCase(SettingsFragment.this.et_old_pass.getText().toString()) && SettingsFragment.this.et_old_pass.getText().toString().length() > 6) {
                    SettingsFragment.this.config.SavePassword(SettingsFragment.this.et_new_pass.getText().toString());
                    SettingsFragment.this.alertDialog.dismiss();
                    MDToast.makeText(SettingsFragment.this.getActivity(), SettingsFragment.this.getActivity().getString(R.string.passwords_result), MDToast.LENGTH_SHORT, 1);
                } else if (!SettingsFragment.this.config.GetPassword().equalsIgnoreCase(SettingsFragment.this.et_old_pass.getText().toString())) {
                    SettingsFragment.this.et_cnf_pass.setError(SettingsFragment.this.getString(R.string.passwords_mismatch));
                } else if (SettingsFragment.this.et_old_pass.getText().toString().length() < 6) {
                    SettingsFragment.this.et_cnf_pass.setError(SettingsFragment.this.getString(R.string.password_too_short_error, Integer.valueOf(6)));
                }
            } else {
                SettingsFragment.this.et_cnf_pass.setError(SettingsFragment.this.getString(R.string.passwords_mismatch));
            }
        }
    }

    public interface Listener {
        void Dologout();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.adapter = new FeesListAdapter(this.context, this.config);
        this.config = new Configuration(PreferenceManager.getDefaultSharedPreferences(getActivity()));
        this.symbol = PrimestoneMain.get().getSymbol();
    }

    public static SettingsFragment newInstance(String accountId) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putSerializable("account_id", accountId);
        fragment.setArguments(args);
        return fragment;
    }

    @OnClick(R.id.cb_pref_receive_address)
    public void onCheckChange() {
        config.setManualAddressManagement(cb_pref_receive_address.isChecked());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);
        ButterKnife.bind((Object) this, view);
        this.coinList.setAdapter(this.adapter);
        setAllTypefaceThin(view);
        this.tv_fee.setText(String.valueOf(this.adapter.getItem(0)).replace(this.symbol, " " + this.symbol));
        this.cb_pref_receive_address.setChecked(this.config.isManualAddressManagement());
        return view;
    }

    @OnClick(R.id.tv_change_pass)
    void ChangePass(TextView textView) {
        DialogChangePassword();
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        this.config = ((WalletApplication) context.getApplicationContext()).getConfiguration();
        this.config.registerOnSharedPreferenceChangeListener(this);
        try {
            this.listener = (Listener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + Listener.class);
        }
    }

    public void onDetach() {
        super.onDetach();
        this.config.unregisterOnSharedPreferenceChangeListener(this);
    }

    public void ShowUnlockDialog() {
        final Dialog passDialog = new Dialog(getActivity());
        passDialog.requestWindowFeature(1);
        passDialog.setContentView(R.layout.dialog_password);
        passDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        passDialog.setCancelable(false);
        passDialog.setCanceledOnTouchOutside(false);
        FancyButton tv_confirm = (FancyButton) passDialog.findViewById(R.id.tv_confirm);
        FancyButton tv_close = (FancyButton) passDialog.findViewById(R.id.tv_close);

        final EditText passwordView = (EditText) passDialog.findViewById(R.id.password);
        ProgressBar process = (ProgressBar) passDialog.findViewById(R.id.process);
        ((TextView) passDialog.findViewById(R.id.tv_title)).setText(getString(R.string.change_pin));
        process.setVisibility(View.GONE);
        setAllTypefaceThin(passDialog.findViewById(R.id.root_layout));
        setAllTypefaceBold(tv_confirm);
        setAllTypefaceBold(tv_close);

        tv_confirm.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (passwordView.getText().toString().equals(SettingsFragment.this.config.GetPassword())) {
                    passDialog.dismiss();
                    SettingsFragment.this.config.setPinFail(Integer.valueOf(0));
                    Intent pinIntent = new Intent(getActivity(), PinEntryActivity.class);
                    pinIntent.putExtra("FromSetting", true);
                    startActivity(pinIntent);
                    return;
                }
                passwordView.setError(SettingsFragment.this.getString(R.string.password_failed));
            }
        });

        tv_close.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                passDialog.dismiss();
            }
        });

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        passDialog.getWindow().setLayout(width - (width / 10), -2);
        passDialog.show();
    }

    @OnClick(R.id.tv_change_pin)
    void ChangePin(TextView textView) {
        ShowUnlockDialog();
    }

    @OnClick(R.id.tv_support)
    void Support(TextView textView) {
        WalletApplication.getInstance().stopLogoutTimer(getActivity());
        sendSupportEmail();
    }

    public void DialogChangePassword() {
        Builder dialogBuilder = new Builder(getActivity());
        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        dialogBuilder.setView(dialogView);
        setAllTypefaceThin(dialogView);
        setAllTypefaceBold(dialogView.findViewById(R.id.confirm_cancel));
        setAllTypefaceBold(dialogView.findViewById(R.id.confirm_unpair));
        this.alertDialog = dialogBuilder.create();
        this.alertDialog.setCanceledOnTouchOutside(false);
        this.alertDialog.setCancelable(false);
        this.alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        FancyButton confirmCancel = (FancyButton) dialogView.findViewById(R.id.confirm_cancel);
        this.et_old_pass = (EditText) dialogView.findViewById(R.id.et_old_pass);
        this.et_new_pass = (EditText) dialogView.findViewById(R.id.et_new_pass);
        this.et_cnf_pass = (EditText) dialogView.findViewById(R.id.et_cnf_pass);
        confirmCancel.setOnClickListener(new C12443());
        ((FancyButton) dialogView.findViewById(R.id.confirm_unpair)).setOnClickListener(new C12454());
        this.alertDialog.show();
    }

    public void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }

    @OnClick(R.id.tv_show_recovery)
    void showRecovery(TextView textView) {
        startActivity(new Intent(getActivity(), ShowSeedActivity.class));
    }

    @OnClick(R.id.tv_rph_info)
    void showRecovery1(TextView textView) {
        startActivity(new Intent(getActivity(), ShowSeedActivity.class));
    }

    @OnClick(R.id.tv_logout)
    void logoutclick(TextView textView) {
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(1);
        dialog.setContentView(R.layout.popup_logout);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        FancyButton tv_logout = (FancyButton) dialog.findViewById(R.id.tv_logout);
        FancyButton tv_cancel = (FancyButton) dialog.findViewById(R.id.tv_cancel);
        setAllTypefaceThin(dialog.findViewById(R.id.lbltitle));
        setAllTypefaceBold(dialog.findViewById(R.id.tv_logout));
        setAllTypefaceBold(dialog.findViewById(R.id.tv_cancel));

        tv_cancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        tv_logout.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                SettingsFragment.this.listener.Dologout();
            }
        });

        int width = getResources().getDisplayMetrics().widthPixels;
        dialog.show();
        dialog.getWindow().setLayout(width - (width / 10), -2);
    }

    @OnClick(R.id.tv_restorewallet)
    void restoreWallet(TextView textView) {
        startActivity(new Intent(getActivity(), IntroActivity.class));
    }

    @OnClick(R.id.tv_about_app)
    void AboutApp(TextView textView) {
        startActivity(new Intent(getActivity(), AboutActivity.class));
    }

    private void sendSupportEmail() {
        Intent intent = new Intent("android.intent.action.SENDTO");
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra("android.intent.extra.EMAIL", new String[]{"admin@primestone.global"});
        intent.putExtra("android.intent.extra.SUBJECT", "");
        intent.putExtra("android.intent.extra.TEXT", "Dear " + getString(R.string.app_name) + " Support," + "\n" + "Explain your issue here," + "\n" + "App: " + getString(R.string.app_name) + ", Version " + "1.1" + " \n" + "System: " + Build.MANUFACTURER + "\n" + "Model: " + Build.MODEL + "\n" + "Version: " + VERSION.RELEASE);
        try {
            startActivity(Intent.createChooser(intent, getResources().getString(R.string.support_message)));
        } catch (ActivityNotFoundException e) {
            MDToast.makeText(getActivity(), getActivity().getString(R.string.error_generic), 0, 3);
        }
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

    @OnClick(R.id.rl_fee)
    void editFee(RelativeLayout textView) {
        EditFeeDialog.newInstance(((Value) this.coinList.getItemAtPosition(0)).type).show(getFragmentManager(), "edit_fee_dialog");
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("fees".equals(key)) {
            this.adapter.update();
            this.tv_fee.setText(String.valueOf(this.adapter.getItem(0)).replace(this.symbol, " " + this.symbol));
        }
    }

}
