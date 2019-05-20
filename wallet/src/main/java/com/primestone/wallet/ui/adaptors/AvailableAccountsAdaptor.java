package com.primestone.wallet.ui.adaptors;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.wallet.WalletAccount;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;

import com.primestone.wallet.ui.widget.NavDrawerItemView;
import com.primestone.wallet.util.WalletUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class AvailableAccountsAdaptor extends BaseAdapter {
    private final Context context;
    private List<Entry> entries;

    public static class Entry {
        public final Object accountOrCoinType;
        public final int iconRes;
        public final String title;

        public Entry(WalletAccount account) {
            iconRes = WalletUtils.getIconRes(account);
            title = account.getDescriptionOrCoinName();
            accountOrCoinType = account;
        }

        public Entry(CoinType type) {
            iconRes = WalletUtils.getIconRes(type);
            title = type.getName();
            accountOrCoinType = type;
        }

        @Override
        public boolean equals(Object o) {
            return this.accountOrCoinType.getClass().isInstance(o) && this.accountOrCoinType.equals(o);
        }

        public CoinType getType() {
            if (this.accountOrCoinType instanceof CoinType) {
                return (CoinType) this.accountOrCoinType;
            }
            if (this.accountOrCoinType instanceof WalletAccount) {
                return ((WalletAccount) this.accountOrCoinType).getCoinType();
            }
            throw new IllegalStateException("No cointype available");
        }
    }

    public AvailableAccountsAdaptor(final Context context) {
        this.context = context;
        entries = ImmutableList.of();
    }

    public int getAccountOrTypePosition(Object accountOrCoinType) {
        return this.entries.indexOf(accountOrCoinType);
    }

    public void update(List<WalletAccount> accounts, List<CoinType> validTypes, boolean includeTypes) {
        this.entries = createEntries(accounts, validTypes, includeTypes);
        notifyDataSetChanged();
    }

    private static List<Entry> createEntries(List<WalletAccount> accounts, List<CoinType> validTypes, boolean includeTypes) {
        Iterator i$;
        ArrayList<CoinType> typesToAdd = Lists.newArrayList((Iterable) validTypes);
        Builder<Entry> listBuilder = ImmutableList.builder();
        for (WalletAccount account : accounts) {
            if (validTypes.contains(account.getCoinType())) {
                listBuilder.add(new Entry(account));
                typesToAdd.remove(account.getCoinType());
            }
        }
        if (includeTypes) {
            i$ = typesToAdd.iterator();
            while (i$.hasNext()) {
                listBuilder.add(new Entry((CoinType) i$.next()));
            }
        }
        return listBuilder.build();
    }

    public List<CoinType> getTypes() {
        Collection types = new HashSet();
        for (Entry entry : this.entries) {
            types.add(entry.getType());
        }
        return ImmutableList.copyOf(types);
    }

    public List<Entry> getEntries() {
        return entries;
    }

    @Override
    public int getCount() {
        return this.entries.size();
    }

    @Override
    public Entry getItem(int position) {
        return (Entry) this.entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return (long) position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new NavDrawerItemView(this.context);
            setAllTypefaceThin(convertView);
        }
        Entry entry = getItem(position);
        ((NavDrawerItemView) convertView).setData(entry.title, entry.iconRes);
        return convertView;
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
