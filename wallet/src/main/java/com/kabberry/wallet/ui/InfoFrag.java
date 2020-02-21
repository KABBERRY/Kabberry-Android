package com.kabberry.wallet.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kabberry.wallet.R;

public class InfoFrag extends Fragment {
    int get_pos = 0;
    ImageView iv_image;
    TextView tv_sub_title;

    public static InfoFrag newInstance(int position) {
        InfoFrag fragment = new InfoFrag();
        Bundle args = new Bundle();
        args.putInt("POS", position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            get_pos = getArguments().getInt("POS");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info, container, false);
        //tv_title = (TextView) view.findViewById(R.id.tv_title);
        tv_sub_title = (TextView) view.findViewById(R.id.tv_sub_title);
        iv_image = (ImageView) view.findViewById(R.id.iv_image);
        setAllTypefaceThin(view.findViewById(R.id.root_layout));

        //tv_title.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Bold.ttf"));

        switch (this.get_pos) {
            case 0:
                //this.tv_title.setText(getString(R.string.infotitle_1));
                this.tv_sub_title.setText(getString(R.string.info_1));
                this.iv_image.setImageResource(R.drawable.wallet_3);
                break;
            case 1:
                //this.tv_title.setText(getString(R.string.send));
                tv_sub_title.setText(getString(R.string.info_2));
                iv_image.setImageResource(R.drawable.wallet_1);
                break;
            case 2:
                this.tv_sub_title.setText(getString(R.string.info_3));
                iv_image.setPadding(270, 0, 0, 0);
                iv_image.setImageResource(R.drawable.wallet_2);
                break;
        }
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void setAllTypefaceThin(View view) {
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getChildCount() != 0) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllTypefaceThin(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Regular.ttf"));
        }
    }
}
