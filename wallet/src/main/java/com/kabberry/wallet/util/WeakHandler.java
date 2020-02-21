package com.kabberry.wallet.util;

import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import java.lang.ref.WeakReference;

public abstract class WeakHandler<T> extends Handler {
    private final WeakReference<T> reference;

    protected abstract void weakHandleMessage(T t, Message message);

    public WeakHandler(T ref) {
        this.reference = new WeakReference(ref);
    }

    public void handleMessage(Message msg) {
        T ref = this.reference.get();
        if (ref != null) {
            if (ref instanceof Fragment) {
                Fragment f = (Fragment) ref;
                if (f.isRemoving() || f.isDetached() || f.getActivity() == null) {
                    return;
                }
            }
            weakHandleMessage(ref, msg);
        }
    }
}
