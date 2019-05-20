// Generated code from Butter Knife. Do not modify!
package com.primestone.wallet.ui;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.ViewBinder;

public class SendFragment$$ViewBinder<T extends com.primestone.wallet.ui.SendFragment> implements ViewBinder<T> {
  @Override public void bind(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131624242, "field 'sendToAddressView'");
    target.sendToAddressView = finder.castView(view, 2131624242, "field 'sendToAddressView'");
    view = finder.findRequiredView(source, 2131624244, "field 'sendToStaticAddressView' and method 'onStaticAddressClick'");
    target.sendToStaticAddressView = finder.castView(view, 2131624244, "field 'sendToStaticAddressView'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onStaticAddressClick();
        }
      });
    view = finder.findRequiredView(source, 2131624238, "field 'sendCoinAmountView'");
    target.sendCoinAmountView = finder.castView(view, 2131624238, "field 'sendCoinAmountView'");
    view = finder.findRequiredView(source, 2131624240, "field 'sendLocalAmountView'");
    target.sendLocalAmountView = finder.castView(view, 2131624240, "field 'sendLocalAmountView'");
    view = finder.findRequiredView(source, 2131623982, "field 'addressError'");
    target.addressError = finder.castView(view, 2131623982, "field 'addressError'");
    view = finder.findRequiredView(source, 2131623991, "field 'amountError'");
    target.amountError = finder.castView(view, 2131623991, "field 'amountError'");
    view = finder.findRequiredView(source, 2131623996, "field 'amountWarning'");
    target.amountWarning = finder.castView(view, 2131623996, "field 'amountWarning'");
    view = finder.findRequiredView(source, 2131624212, "field 'scanQrCodeButton' and method 'handleScan'");
    target.scanQrCodeButton = finder.castView(view, 2131624212, "field 'scanQrCodeButton'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.handleScan();
        }
      });
    view = finder.findRequiredView(source, 2131624056, "field 'eraseAddressButton' and method 'onAddressClearClick'");
    target.eraseAddressButton = finder.castView(view, 2131624056, "field 'eraseAddressButton'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onAddressClearClick();
        }
      });
    view = finder.findRequiredView(source, 2131624351, "field 'txMessageButton'");
    target.txMessageButton = finder.castView(view, 2131624351, "field 'txMessageButton'");
    view = finder.findRequiredView(source, 2131624353, "field 'txMessageLabel'");
    target.txMessageLabel = finder.castView(view, 2131624353, "field 'txMessageLabel'");
    view = finder.findRequiredView(source, 2131624350, "field 'txMessageView'");
    target.txMessageView = finder.castView(view, 2131624350, "field 'txMessageView'");
    view = finder.findRequiredView(source, 2131624352, "field 'txMessageCounter'");
    target.txMessageCounter = finder.castView(view, 2131624352, "field 'txMessageCounter'");
    view = finder.findRequiredView(source, 2131624239, "field 'sendConfirmButton' and method 'onSendClick'");
    target.sendConfirmButton = finder.castView(view, 2131624239, "field 'sendConfirmButton'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onSendClick(finder.<mehdi.sakout.fancybuttons.FancyButton>castParam(p0, "doClick", 0, "onSendClick", 0));
        }
      });
    view = finder.findRequiredView(source, 2131624315, "field 'tv_balance'");
    target.tv_balance = finder.castView(view, 2131624315, "field 'tv_balance'");
    view = finder.findRequiredView(source, 2131624345, "method 'onUseallFunds'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onUseallFunds(finder.<android.widget.TextView>castParam(p0, "doClick", 0, "onUseallFunds", 0));
        }
      });
  }

  @Override public void unbind(T target) {
    target.sendToAddressView = null;
    target.sendToStaticAddressView = null;
    target.sendCoinAmountView = null;
    target.sendLocalAmountView = null;
    target.addressError = null;
    target.amountError = null;
    target.amountWarning = null;
    target.scanQrCodeButton = null;
    target.eraseAddressButton = null;
    target.txMessageButton = null;
    target.txMessageLabel = null;
    target.txMessageView = null;
    target.txMessageCounter = null;
    target.sendConfirmButton = null;
    target.tv_balance = null;
  }
}
