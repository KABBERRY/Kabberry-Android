// Generated code from Butter Knife. Do not modify!
package com.primestone.wallet.ui;

import android.view.View;
import butterknife.ButterKnife.Finder;
import butterknife.ButterKnife.ViewBinder;

public class SettingsFragment$$ViewBinder<T extends com.primestone.wallet.ui.SettingsFragment> implements ViewBinder<T> {
  @Override public void bind(final Finder finder, final T target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131624024, "field 'cb_pref_receive_address' and method 'onCheckChange'");
    target.cb_pref_receive_address = finder.castView(view, 2131624024, "field 'cb_pref_receive_address'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.onCheckChange();
        }
      });
    view = finder.findRequiredView(source, 2131624029, "field 'coinList'");
    target.coinList = finder.castView(view, 2131624029, "field 'coinList'");
    view = finder.findRequiredView(source, 2131624325, "field 'tv_fee'");
    target.tv_fee = finder.castView(view, 2131624325, "field 'tv_fee'");
    view = finder.findRequiredView(source, 2131624317, "method 'ChangePass'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.ChangePass(finder.<android.widget.TextView>castParam(p0, "doClick", 0, "ChangePass", 0));
        }
      });
    view = finder.findRequiredView(source, 2131624318, "method 'ChangePin'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.ChangePin(finder.<android.widget.TextView>castParam(p0, "doClick", 0, "ChangePin", 0));
        }
      });
    view = finder.findRequiredView(source, 2131624339, "method 'Support'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.Support(finder.<android.widget.TextView>castParam(p0, "doClick", 0, "Support", 0));
        }
      });
    view = finder.findRequiredView(source, 2131624335, "method 'showRecovery'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.showRecovery(finder.<android.widget.TextView>castParam(p0, "doClick", 0, "showRecovery", 0));
        }
      });
    view = finder.findRequiredView(source, 2131624332, "method 'showRecovery1'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.showRecovery1(finder.<android.widget.TextView>castParam(p0, "doClick", 0, "showRecovery1", 0));
        }
      });
    view = finder.findRequiredView(source, 2131624327, "method 'logoutclick'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.logoutclick(finder.<android.widget.TextView>castParam(p0, "doClick", 0, "logoutclick", 0));
        }
      });
    view = finder.findRequiredView(source, 2131624331, "method 'restoreWallet'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.restoreWallet(finder.<android.widget.TextView>castParam(p0, "doClick", 0, "restoreWallet", 0));
        }
      });
    view = finder.findRequiredView(source, 2131624314, "method 'AboutApp'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.AboutApp(finder.<android.widget.TextView>castParam(p0, "doClick", 0, "AboutApp", 0));
        }
      });
    view = finder.findRequiredView(source, 2131624206, "method 'editFee'");
    view.setOnClickListener(
      new butterknife.internal.DebouncingOnClickListener() {
        @Override public void doClick(
          android.view.View p0
        ) {
          target.editFee(finder.<android.widget.RelativeLayout>castParam(p0, "doClick", 0, "editFee", 0));
        }
      });
  }

  @Override public void unbind(T target) {
    target.cb_pref_receive_address = null;
    target.coinList = null;
    target.tv_fee = null;
  }
}
