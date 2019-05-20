// Generated code from Butter Knife. Do not modify!
package com.primestone.wallet.ui.dialogs;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.ViewBinder;

public class EditFeeDialog$$ViewBinder<T extends com.primestone.wallet.ui.dialogs.EditFeeDialog> implements ViewBinder<T> {
  @Override public void bind(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131624081, "field 'description'");
    target.description = finder.castView(view, 2131624081, "field 'description'");
    view = finder.findRequiredView(source, 2131624080, "field 'feeAmount'");
    target.feeAmount = finder.castView(view, 2131624080, "field 'feeAmount'");
    view = finder.findRequiredView(source, 2131624340, "field 'tv_title'");
    target.tv_title = finder.castView(view, 2131624340, "field 'tv_title'");
    view = finder.findRequiredView(source, 2131624321, "method 'onDefaultClick'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onDefaultClick(finder.<mehdi.sakout.fancybuttons.FancyButton>castParam(p0, "doClick", 0, "onDefaultClick", 0));
        }
      });
    view = finder.findRequiredView(source, 2131624320, "method 'onOkClick'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onOkClick(finder.<mehdi.sakout.fancybuttons.FancyButton>castParam(p0, "doClick", 0, "onOkClick", 0));
        }
      });
    view = finder.findRequiredView(source, 2131624095, "method 'onCancelClick'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onCancelClick(finder.<android.widget.ImageView>castParam(p0, "doClick", 0, "onCancelClick", 0));
        }
      });
  }

  @Override public void unbind(T target) {
    target.description = null;
    target.feeAmount = null;
    target.tv_title = null;
  }
}
