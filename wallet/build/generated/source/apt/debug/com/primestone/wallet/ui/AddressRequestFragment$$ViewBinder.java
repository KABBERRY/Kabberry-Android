// Generated code from Butter Knife. Do not modify!
package com.primestone.wallet.ui;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.ViewBinder;

public class AddressRequestFragment$$ViewBinder<T extends com.primestone.wallet.ui.AddressRequestFragment> implements ViewBinder<T> {
  @Override public void bind(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131624194, "field 'addressLabelView' and method 'onNewAddress'");
    target.addressLabelView = finder.castView(view, 2131624194, "field 'addressLabelView'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onNewAddress(finder.<android.widget.TextView>castParam(p0, "doClick", 0, "onNewAddress", 0));
        }
      });
    view = finder.findRequiredView(source, 2131624192, "field 'addressView' and method 'onAddressClick'");
    target.addressView = finder.castView(view, 2131624192, "field 'addressView'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onAddressClick();
        }
      });
    view = finder.findRequiredView(source, 2131624196, "field 'sendCoinAmountView' and method 'onEditAddress'");
    target.sendCoinAmountView = finder.castView(view, 2131624196, "field 'sendCoinAmountView'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onEditAddress(finder.<android.widget.TextView>castParam(p0, "doClick", 0, "onEditAddress", 0));
        }
      });
    view = finder.findRequiredView(source, 2131624361, "field 'previousAddressesLink' and method 'onPreviousAddressesClick'");
    target.previousAddressesLink = finder.castView(view, 2131624361, "field 'previousAddressesLink'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onPreviousAddressesClick();
        }
      });
    view = finder.findRequiredView(source, 2131624186, "field 'qrView' and method 'onqrClick'");
    target.qrView = finder.castView(view, 2131624186, "field 'qrView'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onqrClick();
        }
      });
  }

  @Override public void unbind(T target) {
    target.addressLabelView = null;
    target.addressView = null;
    target.sendCoinAmountView = null;
    target.previousAddressesLink = null;
    target.qrView = null;
  }
}
