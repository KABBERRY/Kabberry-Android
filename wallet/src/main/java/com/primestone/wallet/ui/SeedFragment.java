package com.primestone.wallet.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import com.coinomi.core.wallet.Wallet;

import com.zhy.view.flowlayout.FlowLayout;
import com.zhy.view.flowlayout.TagAdapter;
import com.zhy.view.flowlayout.TagFlowLayout;

import com.primestone.wallet.R;
import com.primestone.wallet.Configuration;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.ui.WelcomeFragment.Listener;
import com.primestone.wallet.util.UiUtils;

import mehdi.sakout.fancybuttons.FancyButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Generates and shows a new passphrase
 */
public class SeedFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(SeedFragment.class);

    AlertDialog alertDialog = null;
    CheckBox cb_tos;
    Configuration configuration;
    Drawable copydrawable;
    private boolean hasExtraEntropy = false;
    private Listener listener;
    String mnemonic = null;
    private TextView mnemonicView;
    TagFlowLayout seed_FlowLayout;
    Drawable seeddrawable;

    TextView tv_title;
    TextView tv_title_back;
    TextView tv_tos;

    public SeedFragment() { }

    class C11941 implements OnCheckedChangeListener {
        C11941() {
        }

        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (SeedFragment.this.cb_tos.isChecked()) {
                SeedFragment.this.configuration.setTermAccepted(true);
            } else {
                SeedFragment.this.configuration.setTermAccepted(false);
            }
        }
    }

    class C11952 implements OnClickListener {
        C11952() {
        }

        public void onClick(View v) {
            Intent i = new Intent("android.intent.action.VIEW");
            i.setData(Uri.parse("https://primestonecoin.io/"));
            SeedFragment.this.startActivity(i);
        }
    }


    class x implements OnClickListener {
        x() {
        }

        public void onClick(View v) {
            if (listener != null) {
                final Dialog dialog = new Dialog(getActivity());

//                dialog.requestWindowFeature(1);
//                dialog.setContentView(R.layout.popup_copy_warning);
//                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
//                dialog.setCancelable(true);
//                dialog.setCanceledOnTouchOutside(false);

//                FancyButton tv_yes = (FancyButton) dialog.findViewById(R.id.tv_yes);
                FancyButton tv_no = (FancyButton) dialog.findViewById(R.id.tv_no);

//                setAllTypefaceThin(dialog.findViewById(R.id.root_layout));
//                setAllTypefaceBold(dialog.findViewById(R.id.lbl_title));
//                setAllTypefaceBold(dialog.findViewById(R.id.tv_no));
//                setAllTypefaceBold(dialog.findViewById(R.id.tv_yes));

                tv_no.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
//                        dialog.dismiss();
//                        Bundle args = getArguments();
//                        if (args == null) {
//                            args = new Bundle();
//                        }
//                        args.putString("seed", mnemonic);
//                        if (listener != null) {
//                            listener.onSeedVerified(args);
//                        }
                    }
                });

//                tv_yes.setOnClickListener(new OnClickListener() {
//                    public void onClick(View v) {
//                        UiUtils.copy(getActivity(), mnemonic);
//                        dialog.dismiss();
//                        Bundle args = getArguments();
//                        if (args == null) {
//                            args = new Bundle();
//                        }
//                        args.putString("seed", mnemonic);
//                        if (listener != null) {
//                            listener.onSeedVerified(args);
//                        }
//                    }
//                });
                dialog.show();
            }
        }
    }

    class C11994 implements OnClickListener {
        C11994() {
        }

        public void onClick(View v) {
            SeedFragment.this.getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    class C12038 implements OnClickListener {
        C12038() {
        }

        public void onClick(View v) {
            SeedFragment.this.generateNewMnemonic();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_seed, container, false);
        setAllTypefaceThin(view);

        final ImageView seedFontIcon = (ImageView) view.findViewById(R.id.seed_icon);
        final ImageView copyFontIcon = (ImageView) view.findViewById(R.id.copy_icon);

        tv_tos = (TextView) view.findViewById(R.id.tv_tos);
        tv_title = (TextView) view.findViewById(R.id.tv_title);
        cb_tos = (CheckBox) view.findViewById(R.id.accept_terms);

        seed_FlowLayout = (TagFlowLayout) view.findViewById(R.id.seed_flowlayout);
        tv_title_back = (TextView) view.findViewById(R.id.tv_title_back);
        tv_title_back.setTypeface(null, Typeface.ITALIC);

        tv_title.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Bold.ttf"));

        cb_tos.setChecked(this.configuration.getTermsAccepted());

        cb_tos.setOnCheckedChangeListener(new C11941());
        tv_tos.setOnClickListener(new C11952());

        final FancyButton buttonNext = (FancyButton) view.findViewById(R.id.button_next);

        //buttonNext.setOnClickListener(new x());

        //buttonNext.setOnClickListener(new View.OnClickListener() {

        view.findViewById(R.id.button_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    final Dialog dialog = new Dialog(getActivity());
                    dialog.requestWindowFeature(1);
                    dialog.setContentView(R.layout.popup_copy_warning);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                    dialog.setCancelable(true);
                    dialog.setCanceledOnTouchOutside(false);

                    setAllTypefaceThin(dialog.findViewById(R.id.root_layout));
                    setAllTypefaceBold(dialog.findViewById(R.id.lbl_title));
                    setAllTypefaceBold(dialog.findViewById(R.id.tv_no));
                    setAllTypefaceBold(dialog.findViewById(R.id.tv_yes));

                    dialog.findViewById(R.id.tv_no).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            Bundle args = getArguments();
                            if (args == null) {
                                args = new Bundle();
                            }

                            args.putString("seed", mnemonic);
                            if (listener != null) {
                                listener.onSeedVerified(args);
                            }
                        }
                    });

                    dialog.findViewById(R.id.tv_yes).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            UiUtils.copy(getActivity(), mnemonic);
                            dialog.dismiss();
                            Bundle args = getArguments();
                            if (args == null) {
                                args = new Bundle();
                            }
                            args.putString("seed", mnemonic);
                            if (listener != null) {
                                listener.onSeedVerified(args);
                            }
                        }
                    });

                    dialog.show();
                }
            }
        });

/*
        view.findViewById(R.id.button_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (listener != null) {
                    final Dialog dialog = new Dialog(getActivity());

                    dialog.requestWindowFeature(1);
                    dialog.setContentView(R.layout.popup_copy_warning);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                    dialog.setCancelable(true);
                    dialog.setCanceledOnTouchOutside(false);

//                    final FancyButton tv_yes = (FancyButton) dialog.findViewById(R.id.tv_yes);
                    //FancyButton tv_no = (FancyButton) dialog.findViewById(R.id.tv_no);

                    setAllTypefaceThin(dialog.findViewById(R.id.root_layout));
                    setAllTypefaceBold(dialog.findViewById(R.id.lbl_title));
                    setAllTypefaceBold(dialog.findViewById(R.id.tv_no));
                    setAllTypefaceBold(dialog.findViewById(R.id.tv_yes));

                    //tv_no.setOnClickListener(new View.OnClickListener() {
                    //dialog.findViewById(R.id.tv_no).setOnClickListener(new View.OnClickListener() {
                    dialog.findViewById(R.id.tv_no).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            dialog.dismiss();
                            Bundle args = getArguments();
                            if (args == null) {
                                args = new Bundle();
                            }

                            args.putString("seed", mnemonic);
                            if (listener != null) {
                                listener.onSeedVerified(args);
                            }

                        }
                    });

//                    tv_yes.setOnClickListener(new OnClickListener() {
//                        public void onClick(View v) {
//                            UiUtils.copy(SeedFragment.this.getActivity(), SeedFragment.this.mnemonic);
//                            dialog.dismiss();
//                            Bundle args = SeedFragment.this.getArguments();
//                            if (args == null) {
//                                args = new Bundle();
//                            }
//                            args.putString("seed", SeedFragment.this.mnemonic);
//                            if (SeedFragment.this.listener != null) {
//                                SeedFragment.this.listener.onSeedVerified(args);
//                            }
//                        }
//                    });

                    dialog.show();
                }       // if ends here
            }
        });
*/
        view.findViewById(R.id.iv_back).setOnClickListener(new C11994());
        buttonNext.setEnabled(false);

        mnemonicView = (TextView) view.findViewById(R.id.seed);
        generateNewMnemonic();

        seedFontIcon.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                seeddrawable = seedFontIcon.getDrawable();
                if (seeddrawable instanceof Animatable) {
                    ((Animatable) seeddrawable).start();
                }

                SeedFragment.this.hasExtraEntropy = !SeedFragment.this.hasExtraEntropy;
                generateNewMnemonic();
            }
        });

        copyFontIcon.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                copydrawable = copyFontIcon.getDrawable();
                if (copydrawable instanceof Animatable) {
                    ((Animatable) copydrawable).start();
                }
                UiUtils.copy(SeedFragment.this.getActivity(), mnemonicView.getText().toString());
            }
        });

        ((CheckBox) view.findViewById(R.id.backed_up_seed)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                buttonNext.setEnabled(isChecked);
            }
        });

//         Need to change this * new C12038
        OnClickListener generateNewSeedListener = new C12038();
        mnemonicView.setOnClickListener(generateNewSeedListener);
        view.findViewById(R.id.seed_regenerate_title).setOnClickListener(generateNewSeedListener);

        return view;
    }

    private void generateNewMnemonic() {
        log.info("Clicked generate a new mnemonic");

        if (hasExtraEntropy) {
            mnemonic = Wallet.generateMnemonicString(256);
        } else {
            mnemonic = Wallet.generateMnemonicString(192);
        }

        mnemonicView.setText(mnemonic);
        seed_FlowLayout.setAdapter(new TagAdapter<String>(mnemonic.split(" ")) {
            public View getView(FlowLayout parent, int position, String s) {
                View gridView = ((LayoutInflater) getActivity().getSystemService("layout_inflater")).inflate(R.layout.item_seed, null);
                setAllTypefaceThin(gridView);
                ((TextView) gridView.findViewById(R.id.seed_item_label)).setText(s);
                return gridView;
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            configuration = ((WalletApplication) context.getApplicationContext()).getConfiguration();
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
