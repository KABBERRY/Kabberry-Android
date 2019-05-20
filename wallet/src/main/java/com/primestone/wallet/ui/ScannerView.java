package com.primestone.wallet.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;

import com.primestone.wallet.R;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class ScannerView extends View {
    private final Paint dotPaint;
    private final Map<ResultPoint, Long> dots = new HashMap(16);
    private Rect frame;
    private Rect framePreview;
    private final Paint laserPaint;
    private final int maskColor;
    private final Paint maskPaint;
    private Bitmap resultBitmap;
    private final int resultColor;

    public ScannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = getResources();
        this.maskColor = res.getColor(R.color.scan_mask);
        this.resultColor = res.getColor(R.color.scan_result_view);
        int laserColor = res.getColor(R.color.scan_laser);
        int dotColor = res.getColor(R.color.scan_dot);
        this.maskPaint = new Paint();
        this.maskPaint.setStyle(Style.FILL);
        this.laserPaint = new Paint();
        this.laserPaint.setColor(laserColor);
        this.laserPaint.setStrokeWidth(8.0f);
        this.laserPaint.setStyle(Style.STROKE);
        this.dotPaint = new Paint();
        this.dotPaint.setColor(dotColor);
        this.dotPaint.setAlpha(160);
        this.dotPaint.setStyle(Style.STROKE);
        this.dotPaint.setStrokeWidth(8.0f);
        this.dotPaint.setAntiAlias(true);
    }

    public void setFraming(Rect frame, Rect framePreview) {
        this.frame = frame;
        this.framePreview = framePreview;
        invalidate();
    }

    public void drawResultBitmap(Bitmap bitmap) {
        this.resultBitmap = bitmap;
        invalidate();
    }

    public void addDot(ResultPoint dot) {
        this.dots.put(dot, Long.valueOf(System.currentTimeMillis()));
        invalidate();
    }

    public void onDraw(Canvas canvas) {
        if (this.frame != null) {
            int i;
            long now = System.currentTimeMillis();
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            Paint paint = this.maskPaint;
            if (this.resultBitmap != null) {
                i = this.resultColor;
            } else {
                i = this.maskColor;
            }
            paint.setColor(i);
            canvas.drawRect(0.0f, 0.0f, (float) width, (float) this.frame.top, this.maskPaint);
            canvas.drawRect(0.0f, (float) this.frame.top, (float) this.frame.left, (float) (this.frame.bottom + 1), this.maskPaint);
            canvas.drawRect((float) (this.frame.right + 1), (float) this.frame.top, (float) width, (float) (this.frame.bottom + 1), this.maskPaint);
            canvas.drawRect(0.0f, (float) (this.frame.bottom + 1), (float) width, (float) height, this.maskPaint);
            if (this.resultBitmap != null) {
                canvas.drawBitmap(this.resultBitmap, null, this.frame, this.maskPaint);
                return;
            }
            this.laserPaint.setAlpha((((now / 600) % 2) > 0 ? 1 : (((now / 600) % 2) == 0 ? 0 : -1)) == 0 ? 160 : 255);
            canvas.drawRect(this.frame, this.laserPaint);
            int frameLeft = this.frame.left;
            int frameTop = this.frame.top;
            float scaleX = ((float) this.frame.width()) / ((float) this.framePreview.width());
            float scaleY = ((float) this.frame.height()) / ((float) this.framePreview.height());
            Iterator<Entry<ResultPoint, Long>> i2 = this.dots.entrySet().iterator();
            while (i2.hasNext()) {
                Entry<ResultPoint, Long> entry = (Entry) i2.next();
                long age = now - ((Long) entry.getValue()).longValue();
                if (age < 500) {
                    this.dotPaint.setAlpha((int) (((500 - age) * 256) / 500));
                    ResultPoint point = (ResultPoint) entry.getKey();
                    canvas.drawPoint((float) (((int) (point.getX() * scaleX)) + frameLeft), (float) (((int) (point.getY() * scaleY)) + frameTop), this.dotPaint);
                } else {
                    i2.remove();
                }
            }
            postInvalidateDelayed(100);
        }
    }
}
