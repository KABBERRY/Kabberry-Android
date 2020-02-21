package com.kabberry.wallet.camera;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.view.SurfaceHolder;

import com.google.zxing.PlanarYUVLuminanceSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CameraManager {
    private static final Logger log = LoggerFactory.getLogger(CameraManager.class);
    private static final Comparator<Size> numPixelComparator = new C10801();
    private Camera camera;
    private Size cameraResolution;
    private Rect frame;
    private Rect framePreview;

    static class C10801 implements Comparator<Size> {
        C10801() {
        }

        public int compare(Size size1, Size size2) {
            int pixels1 = size1.height * size1.width;
            int pixels2 = size2.height * size2.width;
            if (pixels1 < pixels2) {
                return 1;
            }
            if (pixels1 > pixels2) {
                return -1;
            }
            return 0;
        }
    }

    public Rect getFrame() {
        return this.frame;
    }

    public Rect getFramePreview() {
        return this.framePreview;
    }

    public Camera open(SurfaceHolder holder, boolean continuousAutoFocus) throws IOException {
        this.camera = Camera.open();
        if (this.camera == null) {
            int cameraCount = Camera.getNumberOfCameras();
            CameraInfo cameraInfo = new CameraInfo();
            for (int i = 0; i < cameraCount; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == 1) {
                    this.camera = Camera.open(i);
                    break;
                }
            }
        }
        this.camera.setPreviewDisplay(holder);
        Parameters parameters = this.camera.getParameters();
        Rect surfaceFrame = holder.getSurfaceFrame();
        this.cameraResolution = findBestPreviewSizeValue(parameters, surfaceFrame);
        int surfaceWidth = surfaceFrame.width();
        int surfaceHeight = surfaceFrame.height();
        int frameSize = Math.max(180, Math.min(600, Math.min((surfaceWidth * 2) / 3, (surfaceHeight * 2) / 3)));
        int leftOffset = (surfaceWidth - frameSize) / 2;
        int topOffset = (surfaceHeight - frameSize) / 2;
        this.frame = new Rect(leftOffset, topOffset, leftOffset + frameSize, topOffset + frameSize);
        this.framePreview = new Rect((this.frame.left * this.cameraResolution.width) / surfaceWidth, (this.frame.top * this.cameraResolution.height) / surfaceHeight, (this.frame.right * this.cameraResolution.width) / surfaceWidth, (this.frame.bottom * this.cameraResolution.height) / surfaceHeight);
        String savedParameters = parameters == null ? null : parameters.flatten();
        try {
            setDesiredCameraParameters(this.camera, this.cameraResolution, continuousAutoFocus);
        } catch (RuntimeException e) {
            if (savedParameters != null) {
                Parameters parameters2 = this.camera.getParameters();
                parameters2.unflatten(savedParameters);
                try {
                    this.camera.setParameters(parameters2);
                    setDesiredCameraParameters(this.camera, this.cameraResolution, continuousAutoFocus);
                } catch (Throwable x2) {
                    log.info("problem setting camera parameters", x2);
                }
            }
        }
        this.camera.startPreview();
        return this.camera;
    }

    public void close() {
        if (this.camera != null) {
            try {
                this.camera.stopPreview();
                this.camera.release();
            } catch (RuntimeException re) {
                re.printStackTrace();
            }
        }
    }

    private static Size findBestPreviewSizeValue(Parameters parameters, Rect surfaceResolution) {
        if (surfaceResolution.height() > surfaceResolution.width()) {
            surfaceResolution = new Rect(0, 0, surfaceResolution.height(), surfaceResolution.width());
        }
        float screenAspectRatio = ((float) surfaceResolution.width()) / ((float) surfaceResolution.height());
        List<Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            return parameters.getPreviewSize();
        }
        List<Size> arrayList = new ArrayList(rawSupportedSizes);
        Collections.sort(arrayList, numPixelComparator);
        Size bestSize = null;
        float diff = Float.POSITIVE_INFINITY;
        for (Size supportedPreviewSize : arrayList) {
            int realWidth = supportedPreviewSize.width;
            int realHeight = supportedPreviewSize.height;
            int realPixels = realWidth * realHeight;
            if (realPixels >= 150400 && realPixels <= 921600) {
                int maybeFlippedWidth;
                int maybeFlippedHeight;
                boolean isCandidatePortrait = realWidth < realHeight;
                if (isCandidatePortrait) {
                    maybeFlippedWidth = realHeight;
                } else {
                    maybeFlippedWidth = realWidth;
                }
                if (isCandidatePortrait) {
                    maybeFlippedHeight = realWidth;
                } else {
                    maybeFlippedHeight = realHeight;
                }
                if (maybeFlippedWidth == surfaceResolution.width() && maybeFlippedHeight == surfaceResolution.height()) {
                    return supportedPreviewSize;
                }
                float newDiff = Math.abs((((float) maybeFlippedWidth) / ((float) maybeFlippedHeight)) - screenAspectRatio);
                if (newDiff < diff) {
                    bestSize = supportedPreviewSize;
                    diff = newDiff;
                }
            }
        }
        if (bestSize != null) {
            return bestSize;
        }
        return parameters.getPreviewSize();
    }

    @SuppressLint({"InlinedApi"})
    private static void setDesiredCameraParameters(Camera camera, Size cameraResolution, boolean continuousAutoFocus) {
        Parameters parameters = camera.getParameters();
        if (parameters != null) {
            String focusMode;
            List<String> supportedFocusModes = parameters.getSupportedFocusModes();
            if (continuousAutoFocus) {
                focusMode = findValue(supportedFocusModes, "continuous-picture", "continuous-video", "auto", "macro");
            } else {
                focusMode = findValue(supportedFocusModes, "auto", "macro");
            }
            if (focusMode != null) {
                parameters.setFocusMode(focusMode);
            }
            parameters.setPreviewSize(cameraResolution.width, cameraResolution.height);
            camera.setParameters(parameters);
        }
    }

    public void requestPreviewFrame(PreviewCallback callback) {
        this.camera.setOneShotPreviewCallback(callback);
    }

    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data) {
        return new PlanarYUVLuminanceSource(data, this.cameraResolution.width, this.cameraResolution.height, this.framePreview.left, this.framePreview.top, this.framePreview.width(), this.framePreview.height(), false);
    }

    public void setTorch(boolean enabled) {
        if (enabled != getTorchEnabled(this.camera)) {
            setTorchEnabled(this.camera, enabled);
        }
    }

    private static boolean getTorchEnabled(Camera camera) {
        if (camera.getParameters() == null) {
            return false;
        }
        String flashMode = camera.getParameters().getFlashMode();
        if (flashMode == null) {
            return false;
        }
        if ("on".equals(flashMode) || "torch".equals(flashMode)) {
            return true;
        }
        return false;
    }

    private static void setTorchEnabled(Camera camera, boolean enabled) {
        Parameters parameters = camera.getParameters();
        List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        if (supportedFlashModes != null) {
            String flashMode;
            if (enabled) {
                flashMode = findValue(supportedFlashModes, "torch", "on");
            } else {
                flashMode = findValue(supportedFlashModes, "off");
            }
            if (flashMode != null) {
                camera.cancelAutoFocus();
                parameters.setFlashMode(flashMode);
                camera.setParameters(parameters);
            }
        }
    }

    private static String findValue(Collection<String> values, String... valuesToFind) {
        for (String valueToFind : valuesToFind) {
            if (values.contains(valueToFind)) {
                return valueToFind;
            }
        }
        return null;
    }
}
