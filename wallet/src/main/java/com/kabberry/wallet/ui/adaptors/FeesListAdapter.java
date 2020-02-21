package com.kabberry.wallet.ui.adaptors;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.Value;

import com.kabberry.wallet.Configuration;
import com.kabberry.wallet.ui.widget.CoinListItem;

import java.util.ArrayList;
import java.util.List;

public class FeesListAdapter extends BaseAdapter {
    private final Configuration config;
    private final Context context;
    private List<Value> fees = new ArrayList<>();

    public FeesListAdapter(Context context, Configuration config) {
        this.context = context;
        this.config = config;
        update();
    }

    public void update() {
        this.fees.clear();
        this.fees.addAll(this.config.getFeeValues().values());
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return fees.size();
    }

    @Override
    public Value getItem(int position) {
        return (Value) fees.get(position);
    }

    @Override
    public long getItemId(int position) {
        return (long) position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = new CoinListItem(this.context);
        }

        CoinListItem row = (CoinListItem) view;
        setAllTypefaceThin(row);

        Value fee = getItem(position);
        row.setCoin((CoinType) fee.type);
        row.setAmount(fee);
        return row;
    }

    protected void setAllTypefaceThin(View view) {
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getChildCount() != 0) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllTypefaceThin(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.createFromAsset(this.context.getAssets(), "fonts/Roboto-Regular.ttf"));
        }
    }
}
