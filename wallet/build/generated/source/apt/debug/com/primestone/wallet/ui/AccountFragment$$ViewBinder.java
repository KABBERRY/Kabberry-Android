// Generated code from Butter Knife. Do not modify!
package com.primestone.wallet.ui;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.ViewBinder;

public class AccountFragment$$ViewBinder<T extends com.primestone.wallet.ui.AccountFragment> implements ViewBinder<T> {
  @Override public void bind(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131624158, "field 'pager_title_strip'");
    target.pager_title_strip = finder.castView(view, 2131624158, "field 'pager_title_strip'");
    view = finder.findRequiredView(source, 2131624157, "field 'viewPager'");
    target.viewPager = finder.castView(view, 2131624157, "field 'viewPager'");
  }

  @Override public void unbind(T target) {
    target.pager_title_strip = null;
    target.viewPager = null;
  }
}
