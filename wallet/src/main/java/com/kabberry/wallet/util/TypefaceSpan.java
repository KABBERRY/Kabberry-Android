package com.kabberry.wallet.util;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.util.LruCache;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

public class TypefaceSpan extends MetricAffectingSpan {
    private static LruCache<String, Typeface> sTypefaceCache = new LruCache(12);
    private Typeface mTypeface;

    public TypefaceSpan(Context context, String typefaceName) {
        this.mTypeface = (Typeface) sTypefaceCache.get(typefaceName);
        if (this.mTypeface == null) {
            this.mTypeface = Typeface.createFromAsset(context.getApplicationContext().getAssets(), String.format("fonts/%s", new Object[]{typefaceName}));
            sTypefaceCache.put(typefaceName, this.mTypeface);
        }
    }

    public void updateMeasureState(TextPaint p) {
        p.setTypeface(this.mTypeface);
        p.setFlags(p.getFlags() | 128);
    }

    public void updateDrawState(TextPaint tp) {
        tp.setTypeface(this.mTypeface);
        tp.setFlags(tp.getFlags() | 128);
    }
}
