package com.primestone.wallet.ui;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.primestone.wallet.R;
import com.primestone.wallet.Configuration;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.ui.widget.MDToast;
import com.primestone.wallet.util.Keyboard;
import com.primestone.wallet.util.PasswordQualityChecker;
import com.primestone.wallet.util.PasswordUtil;

import java.util.Timer;
import java.util.TimerTask;
import mehdi.sakout.fancybuttons.FancyButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
    Fragment that sets a password
 */
public class SetPasswordFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(SetPasswordFragment.class);

    private Listener listener;
    private boolean isPasswordGood;
    private boolean isPasswordsMatch;
    private PasswordQualityChecker passwordQualityChecker;
    private EditText password1;
    private EditText password2;
    private TextView errorPassword;
    private TextView errorPasswordsMismatch;

    Configuration configuration;
    LinearLayout entropyMeter;

    ImageView iv_showpass;
    ImageView iv_showrepass;

    ProgressBar passStrengthBar;
    TextView passStrengthVerdict;

    int pwStrength;
    int[] strengthColors = new int[]{R.drawable.progress_red, R.drawable.progress_orange, R.drawable.progress_green, R.drawable.progress_green};
    int[] strengthVerdicts = new int[]{R.string.strength_weak, R.string.strength_medium, R.string.strength_strong, R.string.strength_very_strong};
    TextView tv_title_back;
    TextView tv_toc;

    public interface Listener {
        void onPasswordSet(Bundle bundle);
    }

    class C12201 extends ClickableSpan {
        C12201() {
        }

        public void onClick(View textView) {
            Intent i = new Intent("android.intent.action.VIEW");
            i.setData(Uri.parse("https://primestone.global"));
            SetPasswordFragment.this.startActivity(i);
        }

        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
        }
    }

    class C12223 implements OnClickListener {
        C12223() {
        }

        public void onClick(View v) {
            if (SetPasswordFragment.this.password2.getInputType() == 129) {
                SetPasswordFragment.this.password2.setInputType(1);
                SetPasswordFragment.this.iv_showrepass.setImageResource(R.drawable.ic_view);
                SetPasswordFragment.this.setAllTypefaceThin(SetPasswordFragment.this.password2);
                if (SetPasswordFragment.this.password2.getText().length() != 0) {
                    SetPasswordFragment.this.password2.setSelection(SetPasswordFragment.this.password2.getText().length());
                }
            } else if (SetPasswordFragment.this.password2.getInputType() == 1) {
                SetPasswordFragment.this.password2.setInputType(129);
                SetPasswordFragment.this.iv_showrepass.setImageResource(R.drawable.ic_view_not);
                SetPasswordFragment.this.setAllTypefaceThin(SetPasswordFragment.this.password2);
                if (SetPasswordFragment.this.password2.getText().length() != 0) {
                    SetPasswordFragment.this.password2.setSelection(SetPasswordFragment.this.password2.getText().length());
                }
            }
        }
    }

    class C12234 implements OnClickListener {
        C12234() {
        }

        public void onClick(View v) {
            SetPasswordFragment.this.getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    class C12276 implements OnFocusChangeListener {
        C12276() {
        }

        public void onFocusChange(View textView, boolean hasFocus) {
            if (hasFocus) {
                SetPasswordFragment.this.clearError(SetPasswordFragment.this.errorPassword);
            }
        }
    }

    class C12287 implements OnFocusChangeListener {
        C12287() {
        }

        public void onFocusChange(View textView, boolean hasFocus) {
            if (hasFocus) {
                SetPasswordFragment.this.clearError(SetPasswordFragment.this.errorPasswordsMismatch);
            } else {
                SetPasswordFragment.this.checkPasswordsMatch();
            }
        }
    }

    class C12298 implements OnClickListener {
        C12298() {
        }

        public void onClick(View v) {
            SkipPasswordDialogFragment.newInstance(SetPasswordFragment.this.getArguments()).show(SetPasswordFragment.this.getFragmentManager(), null);
        }
    }

    public static class SkipPasswordDialogFragment extends DialogFragment {
        private Listener mListener;

        class C12311 implements DialogInterface.OnClickListener {
            C12311() {
            }

            public void onClick(DialogInterface dialog, int which) {
                SkipPasswordDialogFragment.this.dismiss();
            }
        }

        class C12322 implements DialogInterface.OnClickListener {
            C12322() {
            }

            public void onClick(DialogInterface dialog, int which) {
                SkipPasswordDialogFragment.this.dismiss();
                Keyboard.hideKeyboard(SkipPasswordDialogFragment.this.getActivity());
                SkipPasswordDialogFragment.this.getArguments().putString("password", "");
                SkipPasswordDialogFragment.this.mListener.onPasswordSet(SkipPasswordDialogFragment.this.getArguments());
            }
        }

        public static SkipPasswordDialogFragment newInstance(Bundle args) {
            SkipPasswordDialogFragment dialog = new SkipPasswordDialogFragment();
            dialog.setArguments(args);
            return dialog;
        }

        public void onAttach(Activity activity) {
            super.onAttach(activity);
            try {
                this.mListener = (Listener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString() + " must implement " + Listener.class);
            }
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new Builder(getActivity()).setMessage(getResources().getString(R.string.password_skip_warn)).setPositiveButton(R.string.button_ok, new C12322()).setNegativeButton(R.string.button_cancel, new C12311()).create();
        }
    }

    public static SetPasswordFragment newInstance(Bundle args) {
        SetPasswordFragment fragment = new SetPasswordFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        passwordQualityChecker = new PasswordQualityChecker(getActivity());
        isPasswordGood = false;
        isPasswordsMatch = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_set_password, container, false);
        setAllTypefaceThin(view);

        errorPassword = (TextView) view.findViewById(R.id.password_error);
        errorPasswordsMismatch = (TextView) view.findViewById(R.id.passwords_mismatch);
        passStrengthBar = (ProgressBar) view.findViewById(R.id.pass_strength_bar);
        passStrengthBar.setMax(100);
        passStrengthVerdict = (TextView) view.findViewById(R.id.pass_strength_verdict);
        tv_toc = (TextView) view.findViewById(R.id.tv_toc);
        entropyMeter = (LinearLayout) view.findViewById(R.id.entropy_meter);

        clearError(errorPassword);
        clearError(errorPasswordsMismatch);

        SpannableString ss = new SpannableString(getString(R.string.accept_toc));
        ss.setSpan(new C12201(), 39, 60, 33);
        ss.setSpan(new ForegroundColorSpan(-16776961), 39, 60, 33);

        tv_toc.setText(ss);
        tv_toc.setMovementMethod(LinkMovementMethod.getInstance());
        tv_toc.setHighlightColor(0);

        tv_title_back = (TextView) view.findViewById(R.id.tv_title_back);
        tv_title_back.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Bold.ttf"));
        tv_title_back.setTypeface(null, Typeface.ITALIC);

        password1 = (EditText) view.findViewById(R.id.password1);
        password2 = (EditText) view.findViewById(R.id.password2);

        iv_showpass = (ImageView) view.findViewById(R.id.iv_showpass);
        iv_showrepass = (ImageView) view.findViewById(R.id.iv_showrepass);

        iv_showpass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (password1.getInputType() == 129) {
                    password1.setInputType(1);
                    iv_showpass.setImageResource(R.drawable.ic_view);
                    setAllTypefaceThin(password1);

                    if (password1.getText().length() != 0) {
                        password1.setSelection(password1.getText().length());
                    }
                } else if (password1.getInputType() == 1) {
                    password1.setInputType(129);
                    iv_showpass.setImageResource(R.drawable.ic_view_not);
                    setAllTypefaceThin(password1);

                    if (password1.getText().length() != 0) {
                        password1.setSelection(password1.getText().length());
                    }
                }
            }
        });

        iv_showrepass.setOnClickListener(new C12223());

        view.findViewById(R.id.iv_back).setOnClickListener(new C12234());

        password1.addTextChangedListener(new TextWatcher() {
            private final long DELAY = 200;
            private Timer timer = new Timer();

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(final Editable editable) {
                if (editable.length() >= 6) {
                    clearError(errorPassword);
                }

                timer.cancel();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    public void run() {
                        setEntropyMeterVisible(0);
                        String pw = editable.toString();
                        pwStrength = (int) Math.round(PasswordUtil.getInstance().getStrength(pw));
                        int pwStrengthLevel = 0;

                        if (pwStrength >= 65) {
                            pwStrengthLevel = 3;
                        } else if (pwStrength >= 40) {
                            pwStrengthLevel = 2;
                        } else if (pwStrength >= 20) {
                            pwStrengthLevel = 1;
                        }
                        setProgress(pwStrengthLevel, pwStrength);
                    }

                    private void setProgress(final int pwStrengthLevel, final int scorePerc) {
                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                passStrengthBar.setProgress(scorePerc);
                                passStrengthBar.setProgressDrawable(ContextCompat.getDrawable(SetPasswordFragment.this.getActivity(), SetPasswordFragment.this.strengthColors[pwStrengthLevel]));
                                passStrengthVerdict.setText(getResources().getString(strengthVerdicts[pwStrengthLevel]));
                            }
                        });
                    }
                }, 200);
            }
        });

        password1.setOnFocusChangeListener(new C12276());
        password2.setOnFocusChangeListener(new C12287());

        ((FancyButton) view.findViewById(R.id.button_next)).setOnClickListener(getOnFinishListener());
        view.findViewById(R.id.password_skip).setOnClickListener(new C12298());

        return view;
    }

    private void checkPasswordsMatch() {
        isPasswordsMatch = password1.getText().toString().equals(password2.getText().toString());
        if (!isPasswordsMatch) {
            showError(errorPasswordsMismatch);
        }
        log.info("Passwords match = {}", isPasswordsMatch);
    }

    private OnClickListener getOnFinishListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Keyboard.hideKeyboard(getActivity());

                checkPasswordsMatch();
                if (pwStrength > 40 && password1.getText().toString().length() >= 6 &&
                        isPasswordsMatch) {
                    configuration.sevePinKey("");
                    configuration.SavePassword(password1.getText().toString());
                    Bundle args = getArguments();
                    args.putString("password", "");
                    listener.onPasswordSet(args);
                } else if (password1.getText().toString().length() < 6) {
                    setError(errorPassword, R.string.password_too_short_error, 6);
                } else if (pwStrength < 40) {
                    setError(errorPassword, R.string.weak_password, 6);
                } else {
                    MDToast.makeText(getActivity(), getActivity().getString(R.string.password_error),
                            MDToast.LENGTH_LONG, 3);
                }
            }
        };
    }

    private void setError(TextView errorView, int messageId, Object... formatArgs) {
        setError(errorView, getResources().getString(messageId, formatArgs));
    }

    private void setError(TextView errorView, String message) {
        errorView.setText(message);
        showError(errorView);
    }

    private void showError(TextView errorView) {
        errorView.setVisibility(View.VISIBLE);
    }

    private void clearError(TextView errorView) {
        errorView.setVisibility(View.GONE);
    }

    private void setEntropyMeterVisible(final int visible) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                entropyMeter.setVisibility(visible);
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        configuration = ((WalletApplication) context.getApplicationContext()).getConfiguration();

        try {
            listener = (Listener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + Listener.class);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        listener = null;
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
