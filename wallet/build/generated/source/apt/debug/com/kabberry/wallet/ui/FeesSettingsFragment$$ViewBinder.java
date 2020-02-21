// Generated code from Butter Knife. Do not modify!
package com.kabberry.wallet.ui;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.ViewBinder;

public class FeesSettingsFragment$$ViewBinder<T extends com.kabberry.wallet.ui.FeesSettingsFragment> implements ViewBinder<T> {
  @Override public void bind(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131624029, "field 'coinList' and method 'editFee'");
    target.coinList = finder.castView(view, 2131624029, "field 'coinList'");
    ((android.widget.AdapterView<?>) view).setOnItemClickListener(
      new android.widget.AdapterView.OnItemClickListener() {
        @Override public void onItemClick(
          android.widget.AdapterView<?> p0,
          android.view.View p1,
          int p2,
          long p3
        ) {
          target.editFee(p2);
        }
      });
  }

  @Override public void unbind(T target) {
    target.coinList = null;
  }
}
