package com.kabberry.wallet.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.kabberry.wallet.R;
import com.kabberry.wallet.Configuration;
import com.kabberry.wallet.WalletApplication;

import java.util.Timer;
import java.util.TimerTask;
import mehdi.sakout.fancybuttons.FancyButton;

public class PinEntryActivity extends ActionBarActivity {
    final int PIN_LENGTH = 4;
    boolean allowExit = true;
    Configuration configuration;
    int exitClickCooldown = 2;
    int exitClickCount = 0;
    final int maxAttempts = 4;
    Dialog passDialog;
    TextView pinBox0 = null;
    TextView pinBox1 = null;
    TextView pinBox2 = null;
    TextView pinBox3 = null;
    TextView[] pinBoxArray = null;
    TextView titleView = null;
    String userEnteredPIN = "";
    String userEnteredPINConfirm = null;

    class C11642 implements Runnable {
        C11642() {
        }

        public void run() {
            for (int j = 0; j <= PinEntryActivity.this.exitClickCooldown; j++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (j >= PinEntryActivity.this.exitClickCooldown) {
                    PinEntryActivity.this.exitClickCount = 0;
                }
            }
        }
    }

    class C11653 implements Runnable {
        C11653() {
        }

        public void run() {
            PinEntryActivity.this.clearPinBoxes();
            PinEntryActivity.this.userEnteredPIN = "";
            PinEntryActivity.this.userEnteredPINConfirm = null;
        }
    }

    class C11674 extends TimerTask {

        class C11661 implements Runnable {
            C11661() {
            }

            public void run() {
                titleView.setText(R.string.confirm_pin);
                PinEntryActivity.this.clearPinBoxes();
                PinEntryActivity.this.userEnteredPINConfirm = PinEntryActivity.this.userEnteredPIN;
                PinEntryActivity.this.userEnteredPIN = "";
            }
        }

        C11674() {
        }

        public void run() {
            PinEntryActivity.this.runOnUiThread(new C11661());
        }
    }

    class C11695 extends TimerTask {

        class C11681 implements Runnable {
            C11681() {
            }

            public void run() {
                PinEntryActivity.this.titleView.setText(R.string.confirm_pin);
                PinEntryActivity.this.clearPinBoxes();
                PinEntryActivity.this.userEnteredPINConfirm = PinEntryActivity.this.userEnteredPIN;
                PinEntryActivity.this.userEnteredPIN = "";
            }
        }

        C11695() {
        }

        public void run() {
            PinEntryActivity.this.runOnUiThread(new C11681());
        }
    }

    class C11706 implements Runnable {
        C11706() {
        }

        public void run() {
            PinEntryActivity.this.clearPinBoxes();
            PinEntryActivity.this.userEnteredPIN = "";
            PinEntryActivity.this.userEnteredPINConfirm = null;
            PinEntryActivity.this.titleView.setText(R.string.create_pin);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.activity_pin_entry);
        setRequestedOrientation(1);
        titleView = (TextView) findViewById(R.id.tv_title_back);
        configuration = new Configuration(PreferenceManager.getDefaultSharedPreferences(this));
        pinBox0 = (TextView) findViewById(R.id.pinBox0);
        this.pinBox1 = (TextView) findViewById(R.id.pinBox1);
        this.pinBox2 = (TextView) findViewById(R.id.pinBox2);
        this.pinBox3 = (TextView) findViewById(R.id.pinBox3);
        this.pinBoxArray = new TextView[4];
        this.pinBoxArray[0] = this.pinBox0;
        this.pinBoxArray[1] = this.pinBox1;
        this.pinBoxArray[2] = this.pinBox2;
        this.pinBoxArray[3] = this.pinBox3;
        setAllTypefaceBold(findViewById(R.id.root_layout));
        setAllTypefaceThin(this.titleView);

        if (this.configuration.GetPinFail().intValue() >= 4) {
            Toast.makeText(getApplicationContext(), getString(R.string.max_pin_end), 0).show();
            this.passDialog = new Dialog(this, R.style.NewDialog);
            this.passDialog.requestWindowFeature(1);
            this.passDialog.setContentView(R.layout.dialog_pin_trial);
            this.passDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            this.passDialog.setCancelable(false);
            this.passDialog.setCanceledOnTouchOutside(false);
            FancyButton tv_submit = (FancyButton) this.passDialog.findViewById(R.id.tv_submit);
            final EditText et_password = (EditText) this.passDialog.findViewById(R.id.et_password);
            setAllTypefaceThin(this.passDialog.findViewById(R.id.root_layout));
            setAllTypefaceBold(this.passDialog.findViewById(R.id.tv_submit));
            tv_submit.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (et_password.getText().toString().equals(PinEntryActivity.this.configuration.GetPassword())) {
                        PinEntryActivity.this.configuration.setPinFail(Integer.valueOf(0));
                        Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                        intent.addFlags(268468224);
                        intent.putExtra("FromSetting", true);
                        PinEntryActivity.this.startActivity(intent);
                        PinEntryActivity.this.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        return;
                    }
                    et_password.setError(PinEntryActivity.this.getString(R.string.passwords_mismatch));
                    et_password.setFocusable(true);
                }
            });
            this.passDialog.show();
            LayoutParams lp = this.passDialog.getWindow().getAttributes();
            lp.dimAmount = 0.9f;
            this.passDialog.getWindow().setAttributes(lp);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = displayMetrics.widthPixels;
            this.passDialog.getWindow().setLayout(width - (width / 15), -2);
        }
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        WalletApplication.getInstance().stopLogoutTimer(getApplicationContext());
        cancelClicked(null);
        if (this.configuration.GetPinKey().length() > 1 && getIntent().hasExtra("FromSetting")) {
            titleView.setText(R.string.create_pin);
            titleView.setTypeface(null, Typeface.ITALIC);
        } else if (this.configuration.GetPinKey().length() < 1) {
            titleView.setText(R.string.create_pin);
            titleView.setTypeface(null, Typeface.ITALIC);
        } else {
            titleView.setText(R.string.pin_entry);
            titleView.setTypeface(null, Typeface.ITALIC);
        }
    }

    public void onBackPressed() {
        if (this.allowExit) {
            this.exitClickCount++;
            if (this.exitClickCount == 2) {
                finish();
                System.exit(0);
            } else {
                Toast.makeText(this, getString(R.string.want_to_exit), 0).show();
            }
            new Thread(new C11642()).start();
            return;
        }
        super.onBackPressed();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public void padClicked(View view) {
        if (this.userEnteredPIN.length() != 4) {
            this.userEnteredPIN += view.getTag().toString().substring(0, 1);
            this.pinBoxArray[this.userEnteredPIN.length() - 1].setBackgroundResource(R.drawable.ic_view_pin_open);
            if (this.userEnteredPIN.length() != 4) {
                return;
            }
            if (this.userEnteredPIN.equals("0000")) {
                Toast.makeText(this, getString(R.string.zero_pin), 0).show();
                new Handler().postDelayed(new C11653(), 200);
            } else if (this.configuration.GetPinKey().length() >= 1) {
                if (!getIntent().hasExtra("FromSetting")) {
                    //this.titleView.setVisibility(4);
                    validatePIN(this.userEnteredPIN);
                } else if (this.userEnteredPINConfirm == null) {
                    new Timer().schedule(new C11674(), 200);
                } else if (this.userEnteredPINConfirm.equals(this.userEnteredPIN)) {
                    createPINThread(this.userEnteredPIN);
                }
            } else if (this.userEnteredPINConfirm == null) {
                new Timer().schedule(new C11695(), 200);
            } else if (this.userEnteredPINConfirm.equals(this.userEnteredPIN)) {
                createPINThread(this.userEnteredPIN);
            } else {
                Toast.makeText(this, getString(R.string.pin_mismatch_error), 0).show();
                new Handler().postDelayed(new C11706(), 200);
            }
        }
    }

    private void createPINThread(String pin) {
        this.configuration.sevePinKey(pin);
        Intent intent = new Intent(getApplicationContext(), WalletActivity.class);
        intent.putExtra("fromPIN", "fromPIN");
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    public void validatePIN(String PIN) {
        validatePINThread(PIN);
    }

    private void validatePINThread(String pin) {
        if (this.configuration.GetPinKey().equals(pin)) {
            this.configuration.setPinFail(Integer.valueOf(0));
            Intent intent = new Intent(getApplicationContext(), WalletActivity.class);
            intent.putExtra("fromPIN", "fromPIN");
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            return;
        }
        Toast.makeText(this, getString(R.string.pin_mismatch_error), 0).show();
        incrementFailureCount();
    }

    public void cancelClicked(View view) {
        clearPinBoxes();
        this.userEnteredPIN = "";
    }

    private void clearPinBoxes() {
        if (this.userEnteredPIN.length() > 0) {
            for (TextView pinBox : this.pinBoxArray) {
                pinBox.setBackgroundResource(R.drawable.ic_view_pin);
            }
        }
    }

    private void incrementFailureCount() {
        this.configuration.setPinFail(Integer.valueOf(this.configuration.GetPinFail().intValue() + 1));
        Intent intent = new Intent(this, PinEntryActivity.class);
        intent.addFlags(268468224);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    protected void onPause() {
        super.onPause();
        WalletApplication.getInstance().startLogoutTimer(getApplicationContext());
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
}
