// Generated code from Butter Knife. Do not modify!
package com.kabberry.wallet.ui.widget;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.ViewBinder;

public class CoinListItem$$ViewBinder<T extends com.kabberry.wallet.ui.widget.CoinListItem> implements ViewBinder<T> {
  @Override public void bind(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131624091, "field 'icon'");
    target.icon = finder.castView(view, 2131624091, "field 'icon'");
    view = finder.findRequiredView(source, 2131624092, "field 'title'");
    target.title = finder.castView(view, 2131624092, "field 'title'");
    view = finder.findRequiredView(source, 2131623990, "field 'amount'");
    target.amount = finder.castView(view, 2131623990, "field 'amount'");
  }

  @Override public void unbind(T target) {
    target.icon = null;
    target.title = null;
    target.amount = null;
  }
}
