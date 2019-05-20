// Generated code from Butter Knife. Do not modify!
package com.primestone.wallet.ui;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.ViewBinder;

public class BalanceFragment$$ViewBinder<T extends com.primestone.wallet.ui.BalanceFragment> implements ViewBinder<T> {
  @Override public void bind(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131624310, "field 'transactionRows' and method 'onItemClick'");
    target.transactionRows = finder.castView(view, 2131624310, "field 'transactionRows'");
    ((android.widget.AdapterView<?>) view).setOnItemClickListener(
      new android.widget.AdapterView.OnItemClickListener() {
        @Override public void onItemClick(
          android.widget.AdapterView<?> p0,
          android.view.View p1,
          int p2,
          long p3
        ) {
          target.onItemClick(p2);
        }
      });
    view = finder.findRequiredView(source, 2131624266, "field 'swipeContainer'");
    target.swipeContainer = finder.castView(view, 2131624266, "field 'swipeContainer'");
    view = finder.findRequiredView(source, 2131624126, "field 'emptyPocketMessage'");
    target.emptyPocketMessage = finder.castView(view, 2131624126, "field 'emptyPocketMessage'");
    view = finder.findRequiredView(source, 2131623937, "field 'accountBalance' and method 'onMainAmountClick'");
    target.accountBalance = finder.castView(view, 2131623937, "field 'accountBalance'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onMainAmountClick();
        }
      });
    view = finder.findRequiredView(source, 2131623940, "field 'accountExchangedBalance' and method 'onLocalAmountClick'");
    target.accountExchangedBalance = finder.castView(view, 2131623940, "field 'accountExchangedBalance'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onLocalAmountClick();
        }
      });
    view = finder.findRequiredView(source, 2131624032, "field 'connectionLabel'");
    target.connectionLabel = finder.castView(view, 2131624032, "field 'connectionLabel'");
    view = finder.findRequiredView(source, 2131624326, "field 'tv_local_price'");
    target.tv_local_price = finder.castView(view, 2131624326, "field 'tv_local_price'");
    view = finder.findRequiredView(source, 2131624003, "field 'blockHeight'");
    target.blockHeight = finder.castView(view, 2131624003, "field 'blockHeight'");
    view = finder.findRequiredView(source, 2131624052, "field 'empty_title'");
    target.empty_title = finder.castView(view, 2131624052, "field 'empty_title'");
  }

  @Override public void unbind(T target) {
    target.transactionRows = null;
    target.swipeContainer = null;
    target.emptyPocketMessage = null;
    target.accountBalance = null;
    target.accountExchangedBalance = null;
    target.connectionLabel = null;
    target.tv_local_price = null;
    target.blockHeight = null;
    target.empty_title = null;
  }
}
