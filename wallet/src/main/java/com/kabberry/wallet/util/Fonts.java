package com.kabberry.wallet.util;

import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;
import java.util.HashMap;

public class Fonts {
    private static final HashMap<Font, Typeface> typefaces = new HashMap();

    public enum Font {
        COINOMI_FONT_ICONS("fonts/coinomi-font-icons.ttf");
        
        private final String fontPath;

        private Font(String path) {
            this.fontPath = path;
        }
    }

    public static synchronized void initFonts(AssetManager assets) {
        synchronized (Fonts.class) {
            if (typefaces.isEmpty()) {
                for (Font font : Font.values()) {
                    typefaces.put(font, Typeface.createFromAsset(assets, font.fontPath));
                }
            }
        }
    }

    public static synchronized void setTypeface(View textView, Font font) {
        synchronized (Fonts.class) {
            if (typefaces.containsKey(font) && (textView instanceof TextView)) {
                ((TextView) textView).setTypeface((Typeface) typefaces.get(font));
            }
        }
    }

    public static void strikeThrough(TextView textView) {
        textView.setPaintFlags(textView.getPaintFlags() | 16);
    }
}
