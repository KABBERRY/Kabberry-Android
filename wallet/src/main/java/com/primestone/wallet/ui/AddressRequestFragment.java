package com.primestone.wallet.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore.Images.Media;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.coinomi.core.Preconditions;
import com.coinomi.core.coins.CoinType;
import com.coinomi.core.coins.FiatType;
import com.coinomi.core.coins.Value;
import com.coinomi.core.coins.families.BitFamily;
import com.coinomi.core.coins.families.NxtFamily;
import com.coinomi.core.exceptions.UnsupportedCoinTypeException;
import com.coinomi.core.uri.CoinURI;
import com.coinomi.core.util.ExchangeRate;
import com.coinomi.core.util.GenericUtils;
import com.coinomi.core.wallet.AbstractAddress;
import com.coinomi.core.wallet.WalletAccount;

import com.primestone.wallet.AddressBookProvider;
import com.primestone.wallet.R;
import com.primestone.wallet.Configuration;
import com.primestone.wallet.ExchangeRatesProvider;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.ui.dialogs.CreateNewAddressDialog;
import com.primestone.wallet.ui.widget.AmountEditView;
import com.primestone.wallet.ui.widget.MDToast;
import com.primestone.wallet.util.QrUtils;
import com.primestone.wallet.util.ThrottlingWalletChangeListener;
import com.primestone.wallet.util.UiUtils;
import com.primestone.wallet.util.WeakHandler;

import java.io.ByteArrayOutputStream;
import mehdi.sakout.fancybuttons.FancyButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class AddressRequestFragment extends WalletFragment {
    private static final Logger log = LoggerFactory.getLogger(AddressRequestFragment.class);

    private static final int UPDATE_VIEW = 0;
    private static final int UPDATE_EXCHANGE_RATE = 1;

    // Loader IDs
    private static final int ID_RATE_LOADER = 0;

    // Fragment tags
    private static final String NEW_ADDRESS_TAG = "new_address_tag";

    private CoinType type;
    @Nullable private AbstractAddress showAddress;
    private AbstractAddress receiveAddress;
    private Value amount;
    private String label;
    private String accountId;
    private WalletAccount account;
    private String message;

    @Bind(R.id.request_address_label) TextView addressLabelView;
    @Bind(R.id.request_address) TextView addressView;
    @Bind(R.id.request_coin_amount) AmountEditView sendCoinAmountView;
    @Bind(R.id.view_previous_addresses) FancyButton previousAddressesLink;
    @Bind(R.id.qr_code) ImageView qrView;

    String lastQrContent;
    CurrencyCalculatorLink amountCalculatorLink;
    ContentResolver resolver;

    private final MyHandler handler = new MyHandler(this);
    private final ContentObserver addressBookObserver = new AddressBookObserver(handler);
    private Configuration config;
    FrameLayout ll_qrcode;

    private static class MyHandler extends WeakHandler<AddressRequestFragment> {
        public MyHandler(AddressRequestFragment ref) {
            super(ref);
        }

        @Override
        protected void weakHandleMessage(AddressRequestFragment ref, Message msg) {
            switch (msg.what) {
                case UPDATE_VIEW:
                    ref.updateView();
                    break;
                case UPDATE_EXCHANGE_RATE:
                    ref.updateExchangeRate((ExchangeRate) msg.obj);
                    break;
            }
        }
    }

    static class AddressBookObserver extends ContentObserver {
        private final MyHandler handler;

        public AddressBookObserver(MyHandler handler) {
            super(handler);
            this.handler = handler;
        }

        @Override
        public void onChange(boolean selfChange) {
            handler.sendEmptyMessage(UPDATE_VIEW);
        }
    }

    public static AddressRequestFragment newInstance(Bundle args) {
        AddressRequestFragment fragment = new AddressRequestFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static AddressRequestFragment newInstance(String accountId) {
        return newInstance(accountId, null);
    }

    public static AddressRequestFragment newInstance(String accountId,
                                                     @Nullable AbstractAddress showAddress) {
        Bundle args = new Bundle();
        args.putString("account_id", accountId);
        if (showAddress != null) {
            args.putSerializable("address", showAddress);
        }
        return newInstance(args);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        WalletApplication walletApplication = (WalletApplication) getActivity().getApplication();
        Bundle args = getArguments();
        if (args != null) {
            accountId = args.getString("account_id");
            if (args.containsKey("address")) {
                showAddress = (AbstractAddress) args.getSerializable("address");
            }
        }
        account = (WalletAccount) Preconditions.checkNotNull(walletApplication.getAccount(accountId));
        if (account == null) {
            MDToast.makeText(getActivity(), getActivity().getString(R.string.no_such_pocket_error), MDToast.LENGTH_LONG, 3).show();
        } else {
            type = account.getCoinType();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_request, container, false);
        ButterKnife.bind((Object) this, view);

        setAllTypefaceThin(view);
        sendCoinAmountView.resetType(type, true);

        AmountEditView sendLocalAmountView = (AmountEditView) ButterKnife.findById(view,
                R.id.request_local_amount);
        sendLocalAmountView.setFormat(FiatType.FRIENDLY_FORMAT);

        amountCalculatorLink = new CurrencyCalculatorLink(sendCoinAmountView, sendLocalAmountView);
        ll_qrcode = (FrameLayout) view.findViewById(R.id.ll_qrcode);

        return view;
    }

    @Override
    public void onViewStateRestored(@android.support.annotation.Nullable Bundle savedInstanceState) {
        ExchangeRatesProvider.ExchangeRate rate = ExchangeRatesProvider.getRate(getContext(),
                type.getSymbol(), config.getExchangeCurrencyCode());
        if (rate != null) {
            updateExchangeRate(rate.rate);
        }
        updateView();
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        amountCalculatorLink = null;
        lastQrContent = null;
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    @OnClick(R.id.request_address)
    public void onAddressClick() {
        if (showAddress != null) {
            receiveAddress = showAddress;
        }
        Activity activity = getActivity();

        ///////////
        ActionMode actionMode = UiUtils.startAddressActionMode(receiveAddress, activity,
                getFragmentManager());
        // Hack to dismiss this action mode when back is pressed
        if (activity != null && activity instanceof WalletActivity) {
            ((WalletActivity) activity).registerActionMode(actionMode);
        }
        ///////////
        WalletApplication.getInstance().stopLogoutTimer(activity);
    }

    @OnClick(R.id.view_previous_addresses)
    public void onPreviousAddressesClick() {
        Intent intent = new Intent(getActivity(), PreviousAddressesActivity.class);
        intent.putExtra("account_id", accountId);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        account.addEventListener(walletListener);
        amountCalculatorLink.setListener(amountsListener);
        resolver.registerContentObserver(AddressBookProvider.contentUri(getActivity().getPackageName(),
                type), true, addressBookObserver);

        updateView();
    }

    @Override
    public void onPause() {
        resolver.unregisterContentObserver(addressBookObserver);
        amountCalculatorLink.setListener(null);
        account.removeEventListener(walletListener);
        walletListener.removeCallbacks();

        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_label:
                EditAddressBookEntryFragment.edit(getFragmentManager(), type, receiveAddress);
                return true;
            case R.id.action_copy:
                UiUtils.copy(getActivity(), getUri());
                return true;
            case R.id.action_share:
                final Dialog dialog = new Dialog(getActivity());
                dialog.requestWindowFeature(1);
                dialog.setContentView(R.layout.dialog_chooselist);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(false);
                TextView tv_share_address = (TextView) dialog.findViewById(R.id.tv_share_address);
                TextView tv_share_qrcode = (TextView) dialog.findViewById(R.id.tv_share_qrcode);
                setAllTypefaceThin(dialog.findViewById(R.id.root_layout));

                tv_share_address.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        dialog.dismiss();
                        UiUtils.share(getActivity(), getUri());
                    }
                });

                tv_share_qrcode.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        dialog.dismiss();

                        ll_qrcode.setBackgroundColor(getResources().getColor(R.color.white));
                        Bitmap bitmap = getBitmapFromView(ll_qrcode);
                        ll_qrcode.setBackgroundColor(getResources().getColor(R.color.white));
                        Intent shareIntent = new Intent();
                        shareIntent.setAction("android.intent.action.SEND");
                        shareIntent.putExtra("android.intent.extra.STREAM",
                                getImageUri(getActivity(), bitmap));
                        shareIntent.setType("image/jpeg");
                        startActivity(Intent.createChooser(shareIntent, "Share to"));
                        WalletApplication.getInstance().stopLogoutTimer(getActivity());
                    }
                });

                dialog.show();

                return true;
            case R.id.action_pre_address:
                Intent intent = new Intent(getActivity(), PreviousAddressesActivity.class);
                intent.putExtra("account_id", accountId);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        resolver = context.getContentResolver();
        config = ((WalletApplication) context.getApplicationContext()).getConfiguration();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(ID_RATE_LOADER, null, rateLoaderCallbacks);
    }

    @Override
    public void onDetach() {
        getLoaderManager().destroyLoader(ID_RATE_LOADER);
        resolver = null;
        super.onDetach();
    }

    private void showNewAddressDialog() {
        if (isVisible() && isResumed()) {
            Dialogs.dismissAllowingStateLoss(getFragmentManager(), NEW_ADDRESS_TAG);
            CreateNewAddressDialog.getInstance(account).show(getFragmentManager(), NEW_ADDRESS_TAG);
        }
    }

    private void updateExchangeRate(ExchangeRate exchangeRate) {
        amountCalculatorLink.setExchangeRate(exchangeRate);
    }

    //@Override
    public void updateView() {
        if (!isRemoving() && !isDetached()) {
            receiveAddress = null;

            if (showAddress != null) {
                receiveAddress = showAddress;
            } else {
                receiveAddress = account.getReceiveAddress();
            }

            if (showAddress == null && account.hasUsedAddresses()) {
                previousAddressesLink.setVisibility(View.VISIBLE);
            } else {
                previousAddressesLink.setVisibility(View.GONE);
            }

            updateLabel();

            updateQrCode(getUri());
        }
    }

    private String getUri() {
        if (type instanceof BitFamily) {
            return CoinURI.convertToCoinURI(receiveAddress, amount, label, message);
        }

        if (type instanceof NxtFamily) {
            return CoinURI.convertToCoinURI(receiveAddress, amount, label, message,
                    account.getPublicKeySerialized());
        }
        throw new UnsupportedCoinTypeException(type);
    }

    private void updateQrCode(final String qrContent) {
        if (lastQrContent == null || !lastQrContent.equals(qrContent)) {
            QrUtils.setQr(qrView, getResources(), qrContent);
            lastQrContent = qrContent;
        }
    }

    private void updateLabel() {
        label = resolveLabel(receiveAddress);
        if (label != null) {
            addressLabelView.setText(label);
            addressView.setText(GenericUtils.addressSplitToGroups(receiveAddress));
            addressView.setVisibility(View.VISIBLE);
            setAllTypefaceThin(addressLabelView);
            setAllTypefaceThin(addressView);
            return;
        }

        addressLabelView.setText(GenericUtils.addressSplitToGroupsMultiline(receiveAddress));
        setAllTypefaceThin(addressLabelView);
        addressView.setVisibility(View.GONE);
    }

    // C10983
    private final ThrottlingWalletChangeListener walletListener = new ThrottlingWalletChangeListener() {
        @Override
        public void onThrottledWalletChanged() {
            handler.sendEmptyMessage(UPDATE_VIEW);
        }
    };

    private String resolveLabel(@Nonnull AbstractAddress address) {
        return AddressBookProvider.resolveLabel(getActivity(), address);
    }

    private final LoaderManager.LoaderCallbacks<Cursor> rateLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
            String localSymbol = config.getExchangeCurrencyCode();
            String coinSymbol = type.getSymbol();
            return new ExchangeRateLoader(getActivity(), config, localSymbol, coinSymbol);
        }

        @Override
        public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
            if (data != null && data.getCount() > 0) {
                data.moveToFirst();
                final ExchangeRatesProvider.ExchangeRate exchangeRate = ExchangeRatesProvider.getExchangeRate(data);
                handler.sendMessage(handler.obtainMessage(UPDATE_EXCHANGE_RATE,
                        exchangeRate.rate));
            }
        }

        @Override
        public void onLoaderReset(final Loader<Cursor> loader) {
        }
    };

    private final AmountEditView.Listener amountsListener = new AmountEditView.Listener() {
        boolean isValid(Value amount) {
            return amount != null && amount.isPositive()
                    && amount.compareTo(type.getMinNonDust()) >= 0;
        }

        void checkAndUpdateAmount() {
            Value amountParsed = amountCalculatorLink.getPrimaryAmount();
            if (isValid(amountParsed)) {
                amount = amountParsed;
            } else {
                amount = null;
            }

            updateView();
        }

        @Override
        public void changed() {
            checkAndUpdateAmount();
        }

        @Override
        public void focusChanged(final boolean hasFocus) {
            if (!hasFocus) {
                checkAndUpdateAmount();
            }
        }
    };

    @OnClick(R.id.request_coin_amount)
    public void onEditAddress(TextView textView) {
        EditAddressBookEntryFragment.edit(getFragmentManager(), type, receiveAddress);
    }

    @OnClick(R.id.request_address_label)
    public void onNewAddress(TextView textView) {
        showNewAddressDialog();
    }

    @OnClick(R.id.qr_code)
    public void onqrClick() {
        if (showAddress != null) {
            receiveAddress = showAddress;
        }
        UiUtils.copy(getActivity(), receiveAddress.toString());
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        try {
            inImage.compress(CompressFormat.JPEG, 100, new ByteArrayOutputStream());
            return Uri.parse(Media.insertImage(inContext.getContentResolver(), inImage, "", ""));
        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }

    private Bitmap getBitmapFromView(FrameLayout view) {
        try {
            view.setDrawingCacheEnabled(true);
            view.buildDrawingCache();
            return view.getDrawingCache();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
}
