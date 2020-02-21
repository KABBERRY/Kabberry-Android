package com.kabberry.wallet.circleindicator;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.CoordinatorLayout.Behavior;
import android.support.design.widget.Snackbar.SnackbarLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import com.kabberry.wallet.circleindicator.CircleIndicator;

import java.util.List;

public class SnackbarBehavior extends Behavior<CircleIndicator> {
    public SnackbarBehavior(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public boolean layoutDependsOn(CoordinatorLayout parent, CircleIndicator child, View dependency) {
        return dependency instanceof SnackbarLayout;
    }

    public boolean onDependentViewChanged(CoordinatorLayout parent, CircleIndicator child, View dependency) {
        child.setTranslationY(getTranslationYForSnackbar(parent, child));
        return true;
    }

    private float getTranslationYForSnackbar(CoordinatorLayout parent, CircleIndicator ci) {
        float minOffset = 0.0f;
        List<View> dependencies = parent.getDependencies(ci);
        int z = dependencies.size();
        for (int i = 0; i < z; i++) {
            View view = (View) dependencies.get(i);
            if ((view instanceof SnackbarLayout) && parent.doViewsOverlap(ci, view)) {
                minOffset = Math.min(minOffset, ViewCompat.getTranslationY(view) - ((float) view.getHeight()));
            }
        }
        return minOffset;
    }
}
