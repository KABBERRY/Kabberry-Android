// Generated code from Butter Knife. Do not modify!
package com.primestone.wallet.ui;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.ViewBinder;

public class MakeTransactionFragment$$ViewBinder<T extends com.primestone.wallet.ui.MakeTransactionFragment> implements ViewBinder<T> {
  @Override public void bind(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131624300, "field 'transactionInfo'");
    target.transactionInfo = finder.castView(view, 2131624300, "field 'transactionInfo'");
    view = finder.findRequiredView(source, 2131624162, "field 'passwordView'");
    target.passwordView = finder.castView(view, 2131624162, "field 'passwordView'");
    view = finder.findRequiredView(source, 2131624298, "field 'txVisualizer'");
    target.txVisualizer = finder.castView(view, 2131624298, "field 'txVisualizer'");
    view = finder.findRequiredView(source, 2131624311, "field 'tradeWithdrawSendOutput'");
    target.tradeWithdrawSendOutput = finder.castView(view, 2131624311, "field 'tradeWithdrawSendOutput'");
    view = finder.findRequiredView(source, 2131624017, "method 'onConfirmClick'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onConfirmClick(finder.<mehdi.sakout.fancybuttons.FancyButton>castParam(p0, "doClick", 0, "onConfirmClick", 0));
        }
      });
  }

  @Override public void unbind(T target) {
    target.transactionInfo = null;
    target.passwordView = null;
    target.txVisualizer = null;
    target.tradeWithdrawSendOutput = null;
  }
}
