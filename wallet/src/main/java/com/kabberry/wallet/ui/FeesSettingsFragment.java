package com.kabberry.wallet.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnItemClick;

import com.coinomi.core.coins.Value;

import com.kabberry.wallet.R;
import com.kabberry.wallet.Configuration;
import com.kabberry.wallet.WalletApplication;
import com.kabberry.wallet.ui.adaptors.FeesListAdapter;
import com.kabberry.wallet.ui.dialogs.EditFeeDialog;

public class FeesSettingsFragment extends Fragment implements OnSharedPreferenceChangeListener {
    private FeesListAdapter adapter;
    private Configuration config;
    private Context context;
    @Bind(R.id.coins_list) ListView coinList;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.adapter = new FeesListAdapter(this.context, this.config);
    }

    public void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fees_settings_list, container, false);
        ButterKnife.bind((Object) this, view);
        this.coinList.setAdapter(this.adapter);
        return view;
    }

    @OnItemClick(R.id.coins_list)
    void editFee(int currentSelection) {
        EditFeeDialog.newInstance(((Value) this.coinList.getItemAtPosition(currentSelection)).type).show(getFragmentManager(), "edit_fee_dialog");
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        this.config = ((WalletApplication) context.getApplicationContext()).getConfiguration();
        this.config.registerOnSharedPreferenceChangeListener(this);
    }

    public void onDetach() {
        super.onDetach();
        this.config.unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        System.out.println("TAG..Pref fee=" + key);
        if ("fees".equals(key)) {
            this.adapter.update();
        }
    }
}
