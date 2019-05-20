package com.primestone.wallet.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import com.primestone.wallet.R;
import com.primestone.wallet.WalletApplication;
import com.primestone.wallet.camera.CameraManager;
import com.primestone.wallet.ui.widget.MDToast;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ScanActivity extends FragmentActivity implements Callback {
    public static final String INTENT_EXTRA_RESULT = "result";

    private static final long VIBRATE_DURATION = 50L;
    private static final long AUTO_FOCUS_INTERVAL_MS = 2500L;

    private static boolean DISABLE_CONTINUOUS_AUTOFOCUS;
    private static final Logger log = LoggerFactory.getLogger(ScanActivity.class);
    private Handler cameraHandler;
    private final CameraManager cameraManager = new CameraManager();
    private HandlerThread cameraThread;
    private final Runnable closeRunnable = new C11877();
    private final Runnable fetchAndDecodeRunnable = new C11928();
    private boolean flashStatus = false;
    private boolean isSurfaceCreated = false;
    private ImageView iv_flash;

    private final Runnable openRunnable = new Runnable() {
        @Override
        public void run() {
            try
            {
                final Camera camera = cameraManager.open(surfaceHolder, !DISABLE_CONTINUOUS_AUTOFOCUS);
                camera.setDisplayOrientation(90);

                final Rect framingRect = cameraManager.getFrame();
                final Rect framingRectInPreview = cameraManager.getFramePreview();

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        scannerView.setFraming(framingRect, framingRectInPreview);
                    }
                });

                final String focusMode = camera.getParameters().getFocusMode();
                final boolean nonContinuousAutoFocus = Camera.Parameters.FOCUS_MODE_AUTO.equals(focusMode)
                        || Camera.Parameters.FOCUS_MODE_MACRO.equals(focusMode);

                if (nonContinuousAutoFocus)
                    cameraHandler.post(new AutoFocusRunnable(camera));

                cameraHandler.post(fetchAndDecodeRunnable);
            }
            catch (final IOException x)
            {
                log.info("problem opening camera", x);
                showErrorToast();
            }
            catch (final RuntimeException x)
            {
                log.info("problem opening camera", x);
                showErrorToast();
            }
        }
    };

    private ScannerView scannerView;
    private SurfaceHolder surfaceHolder;
    SurfaceView surfaceView;
    private TextView tv_title_back;
    private Vibrator vibrator;

    class C11801 implements OnClickListener {
        C11801() {
        }

        public void onClick(View v) {
            try {
                ScanActivity.this.toggleTorch();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class C11812 implements OnClickListener {
        C11812() {
        }

        public void onClick(View v) {
            ScanActivity.this.onBackPressed();
        }
    }

    class C11834 implements Runnable {
        C11834() {
        }

        public void run() {
            ScanActivity.this.finish();
        }
    }

    /*
    class C11855 implements Runnable {
        C11855() {
        }

        public void run() {
            boolean nonContinuousAutoFocus = false;
            try {
                Camera camera = ScanActivity.this.cameraManager.open(ScanActivity.this.surfaceHolder, !ScanActivity.DISABLE_CONTINUOUS_AUTOFOCUS);
                camera.setDisplayOrientation(90);
                final Rect framingRect = ScanActivity.this.cameraManager.getFrame();
                final Rect framingRectInPreview = ScanActivity.this.cameraManager.getFramePreview();
                ScanActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        ScanActivity.this.scannerView.setFraming(framingRect, framingRectInPreview);
                    }
                });
                String focusMode = camera.getParameters().getFocusMode();
                if ("auto".equals(focusMode) || "macro".equals(focusMode)) {
                    nonContinuousAutoFocus = true;
                }
                if (nonContinuousAutoFocus) {
                    ScanActivity.this.cameraHandler.post(new AutoFocusRunnable(camera));
                }
                ScanActivity.this.cameraHandler.post(ScanActivity.this.fetchAndDecodeRunnable);
            } catch (Throwable x) {
                ScanActivity.log.info("problem opening camera", x);
                ScanActivity.this.showErrorToast();
            } catch (Throwable x2) {
                ScanActivity.log.info("problem opening camera", x2);
                ScanActivity.this.showErrorToast();
            }
        }
    }
*/
    class C11866 implements Runnable {
        C11866() {
        }

        public void run() {
            MDToast.makeText(ScanActivity.this, ScanActivity.this.getString(R.string.error_camera), MDToast.LENGTH_LONG, 3);
            ScanActivity.this.finish();
        }
    }

    class C11877 implements Runnable {
        C11877() {
        }

        public void run() {
            ScanActivity.this.cameraManager.close();
            ScanActivity.this.cameraHandler.removeCallbacksAndMessages(null);
            ScanActivity.this.cameraThread.quit();
        }
    }

    class C11928 implements Runnable {
        private final Map<DecodeHintType, Object> hints = new EnumMap(DecodeHintType.class);
        private final QRCodeReader reader = new QRCodeReader();

        class C11881 implements PreviewCallback {
            C11881() {
            }

            public void onPreviewFrame(byte[] data, Camera camera) {
                C11928.this.decode(data);
            }
        }

        class C11902 implements ResultPointCallback {
            C11902() {
            }

            public void foundPossibleResultPoint(final ResultPoint dot) {
                ScanActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        ScanActivity.this.scannerView.addDot(dot);
                    }
                });
            }
        }

        C11928() {
        }

        public void run() {
            ScanActivity.this.cameraManager.requestPreviewFrame(new C11881());
        }

        private void decode(byte[] data) {
            PlanarYUVLuminanceSource source = ScanActivity.this.cameraManager.buildLuminanceSource(data);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                this.hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, new C11902());
                final Result scanResult = this.reader.decode(bitmap, this.hints);
                int thumbnailWidth = source.getThumbnailWidth();
                int thumbnailHeight = source.getThumbnailHeight();
                final float thumbnailScaleFactor = ((float) thumbnailWidth) / ((float) source.getWidth());
                final Bitmap thumbnailImage = Bitmap.createBitmap(thumbnailWidth, thumbnailHeight, Config.ARGB_8888);
                thumbnailImage.setPixels(source.renderThumbnail(), 0, thumbnailWidth, 0, 0, thumbnailWidth, thumbnailHeight);
                ScanActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        ScanActivity.this.handleResult(scanResult, thumbnailImage, thumbnailScaleFactor);
                    }
                });
            } catch (ReaderException e) {
                ScanActivity.this.cameraHandler.post(ScanActivity.this.fetchAndDecodeRunnable);
            } finally {
                this.reader.reset();
            }
        }
    }

    private final class AutoFocusRunnable implements Runnable {
        private final Camera camera;

        class C11931 implements AutoFocusCallback {
            C11931() {
            }

            public void onAutoFocus(boolean success, Camera camera) {
                ScanActivity.this.cameraHandler.postDelayed(AutoFocusRunnable.this, 2500);
            }
        }

        public AutoFocusRunnable(Camera camera) {
            this.camera = camera;
        }

        public void run() {
            this.camera.autoFocus(new C11931());
        }
    }

    static {
        boolean z = Build.MODEL.equals("GT-I9100") || Build.MODEL.equals("SGH-T989") || Build.MODEL.equals("SGH-T989D") || Build.MODEL.equals("SAMSUNG-SGH-I727") || Build.MODEL.equals("GT-I9300") || Build.MODEL.equals("GT-N7000");
        DISABLE_CONTINUOUS_AUTOFOCUS = z;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (VERSION.SDK_INT > 21) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }
        this.vibrator = (Vibrator) getSystemService("vibrator");
        setContentView(R.layout.scan_activity);
        this.scannerView = (ScannerView) findViewById(R.id.scan_activity_mask);
        setAllTypefaceThin(findViewById(R.id.root_layout));
        this.tv_title_back = (TextView) findViewById(R.id.tv_title_back);
        this.iv_flash = (ImageView) findViewById(R.id.iv_flash);
        this.iv_flash.setOnClickListener(new C11801());
        this.tv_title_back.setOnClickListener(new C11812());
    }

    protected void onResume() {
        super.onResume();
        WalletApplication.getInstance().stopLogoutTimer(getApplicationContext());
        this.cameraThread = new HandlerThread("cameraThread", 10);
        this.cameraThread.start();
        this.cameraHandler = new Handler(this.cameraThread.getLooper());
        this.surfaceView = (SurfaceView) findViewById(R.id.scan_activity_preview);
        this.surfaceHolder = this.surfaceView.getHolder();
        this.surfaceHolder.addCallback(this);
        this.surfaceHolder.setType(3);
        if (hasCameraPermission()) {
            openCamera();
        } else {
            askCameraPermission();
        }
    }

    protected void onPause() {
        super.onPause();
        WalletApplication.getInstance().startLogoutTimer(getApplicationContext());
        this.cameraHandler.post(this.closeRunnable);
        this.surfaceHolder.removeCallback(this);
    }

    private void askCameraPermission() {
        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, 0);
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") == 0;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            int i = 0;
            while (i < permissions.length) {
                if (permissions[i].equals("android.permission.CAMERA") && grantResults[i] == 0) {
                    this.surfaceView.setVisibility(0);
                    this.isSurfaceCreated = true;
                    openCamera();
                    break;
                }
                i++;
            }
            if (!hasCameraPermission()) {
                showErrorToast();
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        this.isSurfaceCreated = true;
        openCamera();
    }

    private void openCamera() {
        Log.e("TAG isSurfaceCreated", String.valueOf(this.isSurfaceCreated));
        if (this.isSurfaceCreated && hasCameraPermission()) {
            this.cameraHandler.post(this.openRunnable);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    public void onBackPressed() {
        setResult(0);
        finish();
    }

    public boolean onKeyDown(final int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 24:
            case 25:
                this.cameraHandler.post(new Runnable() {
                    public void run() {
                        ScanActivity.this.cameraManager.setTorch(keyCode == 24);
                    }
                });
                return true;
            case 27:
            case 80:
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    public void handleResult(Result scanResult, Bitmap thumbnailImage, float thumbnailScaleFactor) {
        this.vibrator.vibrate(50);
        ResultPoint[] points = scanResult.getResultPoints();
        if (points != null && points.length > 0) {
            Paint paint = new Paint();
            paint.setColor(getResources().getColor(R.color.scan_result_dots));
            paint.setStrokeWidth(10.0f);
            Canvas canvas = new Canvas(thumbnailImage);
            canvas.scale(thumbnailScaleFactor, thumbnailScaleFactor);
            for (ResultPoint point : points) {
                canvas.drawPoint(point.getX(), point.getY(), paint);
            }
        }
        this.scannerView.drawResultBitmap(thumbnailImage);
        Intent result = new Intent();
        result.putExtra("result", scanResult.getText());
        setResult(-1, result);
        new Handler().post(new C11834());
    }

    private void showErrorToast() {
        runOnUiThread(new C11866());
    }

    protected void setAllTypefaceThin(View view) {
        if ((view instanceof ViewGroup) && ((ViewGroup) view).getChildCount() != 0) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllTypefaceThin(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(Typeface.createFromAsset(getAssets(), "fonts/ChamsRegular.ttf"));
        }
    }

    private void toggleTorch() {
        this.flashStatus = !this.flashStatus;
        this.cameraManager.setTorch(this.flashStatus);
    }
}
