package com.kabberry.wallet.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kabberry.wallet.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WelcomeFragment extends Fragment {
    private static final Logger log = LoggerFactory.getLogger(WelcomeFragment.class);

    private Listener listener;

    public WelcomeFragment() { }

    private TextView create_wallet;

    public interface Listener {
        void onCreateNewWallet();

        void onRestoreWallet();

        void onRestoreWallet_QR(String str);

        void onSeedVerified(Bundle bundle);
    }

    // Need to change this one
    class C12861 implements OnClickListener {
        C12861() {
        }

        public void onClick(View v) {
            WelcomeFragment.this.handleScan();
        }
    }

    /*
    private View.OnClickListener getOnHandleScan() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRestoreWallet();
                }
            }
        };
    }
    */

    // You can safely remove it after fixing the above class
    /*
    class C12883 implements OnClickListener {
        C12883() {
        }

        public void onClick(View v) {
            if (WelcomeFragment.this.listener != null) {
                WelcomeFragment.this.listener.onRestoreWallet();
            }
        }
    }
    */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_welcome, container, false);

        setAllTypefaceThin(view);
        create_wallet = (TextView) view.findViewById(R.id.create_wallet);
        create_wallet.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Bold.ttf"));

        view.findViewById(R.id.create_wallet).setOnClickListener(getOnCreateListener());
        view.findViewById(R.id.restore_wallet).setOnClickListener(getOnRestoreListener());
        ((ImageView) view.findViewById(R.id.iv_scan)).setOnClickListener(new C12861());

        return view;
    }

    private void handleScan() {
        startActivityForResult(new Intent(getActivity(), ScanActivity.class), 0);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0 && resultCode == -1) {
            listener.onRestoreWallet_QR(intent.getStringExtra("result"));
        }
    }

//    private OnClickListener getOnCreateListener() {
//        return new C12872();
//    }
//    private OnClickListener getOnRestoreListener() {
//        return new C12883();
//    }

    private View.OnClickListener getOnCreateListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log.info("Clicked create new wallet");
                if (listener != null) {
                    listener.onCreateNewWallet();
                }
            }
        };
    }

    private View.OnClickListener getOnRestoreListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log.info("Clicked restore wallet");
                if (listener != null) {
                    listener.onRestoreWallet();
                }
            }
        };
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
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
