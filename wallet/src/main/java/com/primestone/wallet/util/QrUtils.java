package com.primestone.wallet.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.Encoder;

import com.primestone.wallet.R;

import java.util.Hashtable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QrUtils {
    private static final ErrorCorrectionLevel ERROR_CORRECTION_LEVEL = ErrorCorrectionLevel.M;
    private static final QRCodeWriter QR_CODE_WRITER = new QRCodeWriter();
    private static final Logger log = LoggerFactory.getLogger(QrUtils.class);

    public static boolean setQr(ImageView view, Resources res, String content) {
        return setQr(view, res, content, R.dimen.qr_code_size, R.dimen.qr_code_quite_zone_pixels);
    }

    private static boolean setQr(ImageView view, Resources res, String content, int viewSizeResId, int qrQuiteZoneResId) {
        int qrCodeViewSize = res.getDimensionPixelSize(viewSizeResId);
        Bitmap bitmap = create(content, (int) res.getDimension(qrQuiteZoneResId));
        if (bitmap == null) {
            return false;
        }
        BitmapDrawable qr = new BitmapDrawable(res, bitmap);
        qr.setFilterBitmap(false);
        int qrSize = (qrCodeViewSize / qr.getIntrinsicHeight()) * qr.getIntrinsicHeight();
        view.getLayoutParams().height = qrSize;
        view.getLayoutParams().width = qrSize;
        view.requestLayout();
        view.setImageDrawable(qr);
        return true;
    }

    public static Bitmap create(String content, int marginSize) {
        return create(content, -587202560, 0, marginSize);
    }

    public static Bitmap create(String content, int darkColor, int lightColor, int marginSize) {
        try {
            int size = Encoder.encode(content, ERROR_CORRECTION_LEVEL, null).getMatrix().getWidth();
            Hashtable<EncodeHintType, Object> hints = new Hashtable();
            hints.put(EncodeHintType.MARGIN, Integer.valueOf(marginSize));
            hints.put(EncodeHintType.ERROR_CORRECTION, ERROR_CORRECTION_LEVEL);
            BitMatrix result = QR_CODE_WRITER.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            int width = result.getWidth();
            int height = result.getHeight();
            int[] pixels = new int[(width * height)];
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    int i;
                    int i2 = offset + x;
                    if (result.get(x, y)) {
                        i = darkColor;
                    } else {
                        i = lightColor;
                    }
                    pixels[i2] = i;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (Throwable x2) {
            log.info("Could not create qr code", x2);
            return null;
        }
    }
}
