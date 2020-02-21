package com.kabberry.wallet.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.AbstractWallet;

import com.kabberry.wallet.AddressBookProvider;
import com.kabberry.wallet.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

public class AddressesListAdapter extends BaseAdapter {
    private static final Object CACHE_NULL_MARKER = "";
    private final List<AbstractAddress> addresses = new ArrayList();
    private final Context context;
    private final LayoutInflater inflater;
    private final Map<AbstractAddress, String> labelCache = new HashMap();
    private final AbstractWallet pocket;
    private final Resources res;
    private final Set<AbstractAddress> usedAddresses = new HashSet();

    public AddressesListAdapter(Context context, AbstractWallet walletPocket) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        res = context.getResources();

        pocket = walletPocket;
    }

    public void replace(@Nonnull final Collection<AbstractAddress> addresses,
                        Set<AbstractAddress> usedAddresses) {
        addresses.clear();
        addresses.addAll(addresses);
        usedAddresses.clear();
        usedAddresses.addAll(usedAddresses);

        notifyDataSetChanged();
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public int getCount() {
        return addresses.size();
    }

    @Override
    public AbstractAddress getItem(int position) {
        if (position == addresses.size()) {
            return null;
        }
        return (AbstractAddress) addresses.get(position);
    }

    @Override
    public long getItemId(int position) {
        if (position == addresses.size()) {
            return 0;
        }

        return ((AbstractAddress) addresses.get(position)).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(final int position, View row, final ViewGroup parent) {
        if (row == null) {
            row = this.inflater.inflate(R.layout.address_row, null);
        }

        final AbstractAddress address = getItem(position);
        bindView(row, address);

        return row;
    }

    public void bindView(View row, AbstractAddress address) {
        TextView addressLabel = (TextView) row.findViewById(R.id.address_row_label);
        TextView addressRaw = (TextView) row.findViewById(R.id.address_row_address);
        setAllTypefaceThin(row);

        String label = resolveLabel(address);

        if (label != null) {
            addressLabel.setText(label);
            addressRaw.setText(GenericUtils.addressSplitToGroups(address));
        } else {
            addressLabel.setText(GenericUtils.addressSplitToGroups(address));
            addressRaw.setVisibility(8);
        }

        TextView addressUsageLabel = (TextView) row.findViewById(R.id.address_row_usage);
        TextView addressUsageFontIcon = (TextView) row.findViewById(R.id.address_row_usage_font_icon);

        if (usedAddresses.contains(address)) {
            addressUsageLabel.setText(R.string.previous_addresses_used);
            addressUsageFontIcon.setText(R.string.fa_icon_used);
        } else {
            addressUsageLabel.setText(R.string.previous_addresses_unused);
            addressUsageFontIcon.setText(R.string.fa_icon_unused);
        }

        addressUsageFontIcon.setTypeface(Typeface.createFromAsset(this.context.getAssets(), "fonts/fontawesome-webfont.ttf"));
    }

    private String resolveLabel(@Nonnull final AbstractAddress address) {
        String cachedLabel = (String) labelCache.get(address);

        if (cachedLabel == null) {
            String label = AddressBookProvider.resolveLabel(context, address);
            if (label != null) {
                labelCache.put(address, label);
                return label;
            }

            labelCache.put(address, (String) CACHE_NULL_MARKER);
            return label;
        }

        if (cachedLabel == CACHE_NULL_MARKER) {
            cachedLabel = null;
        }

        return cachedLabel;
    }

    public void clearLabelCache() {
        this.labelCache.clear();
        notifyDataSetChanged();
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
