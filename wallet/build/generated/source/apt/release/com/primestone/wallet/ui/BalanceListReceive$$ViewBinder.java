// Generated code from Butter Knife. Do not modify!
package com.kabberry.wallet.ui;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.ViewBinder;

public class BalanceListReceive$$ViewBinder<T extends com.kabberry.wallet.ui.BalanceListReceive> implements ViewBinder<T> {
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
  }

  @Override public void unbind(T target) {
    target.transactionRows = null;
  }
}
