// Generated code from Butter Knife. Do not modify!
package com.kabberry.wallet.ui;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.ViewBinder;

public class AboutActivity$$ViewBinder<T extends com.kabberry.wallet.ui.AboutActivity> implements ViewBinder<T> {
  @Override public void bind(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131624344, "method 'TOCclick'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.TOCclick(finder.<android.widget.TextView>castParam(p0, "doClick", 0, "TOCclick", 0));
        }
      });
    view = finder.findRequiredView(source, 2131624341, "method 'backClick'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.backClick(finder.<android.widget.TextView>castParam(p0, "doClick", 0, "backClick", 0));
        }
      });
    view = finder.findRequiredView(source, 2131624330, "method 'PNPclick'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.PNPclick(finder.<android.widget.TextView>castParam(p0, "doClick", 0, "PNPclick", 0));
        }
      });
  }

  @Override public void unbind(T target) {
  }
}
