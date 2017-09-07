package com.sutanu.blureffectview;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Created by sutanurath on 06/09/17.
 */

public class BlurEffectView extends View {

    private float mSampleFactor;
    private int mOverlayColor;
    private float mBlurRadius;
    private boolean mDirty;
    private Bitmap mBitmapToBlur, mBlurBitmap;
    private Canvas mBlurCanvas;
    private RenderScript mRenderScript;
    private ScriptIntrinsicBlur mBlurScript;
    private Allocation mBlurInput, mBlurOutput;
    private boolean mIsRendering;
    private final Rect mRectSrc = new Rect(), mRectDst = new Rect();
    private View mDecorView;
    private boolean mDifferentRoot;
    private static int RENDER_COUNT;

    public BlurEffectView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BlurEffectView);
        mBlurRadius = a.getDimension(R.styleable.BlurEffectView_blurEffectViewRadius,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, context.getResources().getDisplayMetrics()));
        mSampleFactor = a.getFloat(R.styleable.BlurEffectView_blurEffectViewSampleFactor, 4);
        mOverlayColor = a.getColor(R.styleable.BlurEffectView_blurEffectViewOverlayColor, 0xAAFFFFFF);
        a.recycle();
    }

    public void setBlurRadius(float radius) {
        if (mBlurRadius != radius) {
            mBlurRadius = radius;
            mDirty = true;
            invalidate();
        }
    }

    public void setSampleFactor(float factor) {
        if (factor <= 0) {
            throw new IllegalArgumentException("Sample factor must be greater than 0.");
        }

        if (mSampleFactor != factor) {
            mSampleFactor = factor;
            mDirty = true; // may also change blur radius
            releaseBitmap();
            invalidate();
        }
    }

    public void setOverlayColor(int color) {
        if (mOverlayColor != color) {
            mOverlayColor = color;
            invalidate();
        }
    }

    private void releaseBitmap() {
        if (mBlurInput != null) {
            mBlurInput.destroy();
            mBlurInput = null;
        }
        if (mBlurOutput != null) {
            mBlurOutput.destroy();
            mBlurOutput = null;
        }
        if (mBitmapToBlur != null) {
            mBitmapToBlur.recycle();
            mBitmapToBlur = null;
        }
        if (mBlurBitmap != null) {
            mBlurBitmap.recycle();
            mBlurBitmap = null;
        }
    }

    private void releaseScript() {
        if (mRenderScript != null) {
            mRenderScript.destroy();
            mRenderScript = null;
        }
        if (mBlurScript != null) {
            mBlurScript.destroy();
            mBlurScript = null;
        }
    }

    protected void release() {
        releaseBitmap();
        releaseScript();
    }

    protected boolean prepare() {
        if (mBlurRadius == 0) {
            release();
            return false;
        }

        float downsampleFactor = mSampleFactor;

        if (mDirty || mRenderScript == null) {
            if (mRenderScript == null) {
                try {
                    mRenderScript = RenderScript.create(getContext());
                    mBlurScript = ScriptIntrinsicBlur.create(mRenderScript, Element.U8_4(mRenderScript));
                } catch (android.support.v8.renderscript.RSRuntimeException e) {
                    if (isDebug(getContext())) {
                        if (e.getMessage() != null && e.getMessage().startsWith("Error loading RS jni library: java.lang.UnsatisfiedLinkError:")) {
                            throw new RuntimeException("Error loading RS jni library, Upgrade buildToolsVersion=\"24.0.2\" or higher may solve this issue");
                        } else {
                            throw e;
                        }
                    } else {
                        // In release mode, just ignore
                        releaseScript();
                        return false;
                    }
                }
            }

            mDirty = false;
            float radius = mBlurRadius / downsampleFactor;
            if (radius > 25) {
                downsampleFactor = downsampleFactor * radius / 25;
                radius = 25;
            }
            mBlurScript.setRadius(radius);
        }

        final int width = getWidth();
        final int height = getHeight();

        int scaledWidth = Math.max(1, (int) (width / downsampleFactor));
        int scaledHeight = Math.max(1, (int) (height / downsampleFactor));

        if (mBlurCanvas == null || mBlurBitmap == null
                || mBlurBitmap.getWidth() != scaledWidth
                || mBlurBitmap.getHeight() != scaledHeight) {
            releaseBitmap();

            boolean r = false;
            try {
                mBitmapToBlur = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
                if (mBitmapToBlur == null) {
                    return false;
                }
                mBlurCanvas = new Canvas(mBitmapToBlur);

                mBlurInput = Allocation.createFromBitmap(mRenderScript, mBitmapToBlur,
                        Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
                mBlurOutput = Allocation.createTyped(mRenderScript, mBlurInput.getType());

                mBlurBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
                if (mBlurBitmap == null) {
                    return false;
                }

                r = true;
            } catch (OutOfMemoryError e) {

            } finally {
                if (!r) {
                    releaseBitmap();
                    return false;
                }
            }
        }
        return true;
    }

    protected void blur(Bitmap bitmapToBlur, Bitmap blurredBitmap) {
        mBlurInput.copyFrom(bitmapToBlur);
        mBlurScript.setInput(mBlurInput);
        mBlurScript.forEach(mBlurOutput);
        mBlurOutput.copyTo(blurredBitmap);
    }

    private final ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            final int[] locations = new int[2];
            Bitmap oldBmp = mBlurBitmap;
            View decor = mDecorView;
            if (decor != null && isShown() && prepare()) {
                boolean redrawBitmap = mBlurBitmap != oldBmp;
                oldBmp = null;
                decor.getLocationOnScreen(locations);
                int x = -locations[0];
                int y = -locations[1];

                getLocationOnScreen(locations);
                x += locations[0];
                y += locations[1];

                mBitmapToBlur.eraseColor(mOverlayColor & 0xffffff);

                int rc = mBlurCanvas.save();
                mIsRendering = true;
                RENDER_COUNT++;
                try {
                    mBlurCanvas.scale(1.f * mBitmapToBlur.getWidth() / getWidth(), 1.f * mBitmapToBlur.getHeight() / getHeight());
                    mBlurCanvas.translate(-x, -y);
                    if (decor.getBackground() != null) {
                        decor.getBackground().draw(mBlurCanvas);
                    }
                    decor.draw(mBlurCanvas);
                } catch (StopException e) {
                } finally {
                    mIsRendering = false;
                    RENDER_COUNT--;
                    mBlurCanvas.restoreToCount(rc);
                }

                blur(mBitmapToBlur, mBlurBitmap);

                if (redrawBitmap || mDifferentRoot) {
                    invalidate();
                }
            }

            return true;
        }
    };

    protected View getActivityDecorView() {
        Context ctx = getContext();
        for (int i = 0; i < 4 && ctx != null && !(ctx instanceof Activity) && ctx instanceof ContextWrapper; i++) {
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        if (ctx instanceof Activity) {
            return ((Activity) ctx).getWindow().getDecorView();
        } else {
            return null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mDecorView = getActivityDecorView();
        if (mDecorView != null) {
            mDecorView.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
            mDifferentRoot = mDecorView.getRootView() != getRootView();
            if (mDifferentRoot) {
                mDecorView.postInvalidate();
            }
        } else {
            mDifferentRoot = false;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mDecorView != null) {
            mDecorView.getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
        }
        release();
        super.onDetachedFromWindow();
    }

    @Override
    public void draw(Canvas canvas) {
        if (mIsRendering) {
            // Quit here, don't draw views above me
            throw STOP_EXCEPTION;
        } else if (RENDER_COUNT > 0) {
            // Doesn't support blurview overlap on another blurview
        } else {
            super.draw(canvas);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBlurredBitmap(canvas, mBlurBitmap, mOverlayColor);
    }

    /**
     * Custom draw the blurred bitmap and color to define your own shape
     *
     * @param canvas
     * @param blurredBitmap
     * @param overlayColor
     */
    protected void drawBlurredBitmap(Canvas canvas, Bitmap blurredBitmap, int overlayColor) {
        if (blurredBitmap != null) {
            mRectSrc.right = blurredBitmap.getWidth();
            mRectSrc.bottom = blurredBitmap.getHeight();
            mRectDst.right = getWidth();
            mRectDst.bottom = getHeight();
            canvas.drawBitmap(blurredBitmap, mRectSrc, mRectDst, null);
        }
        canvas.drawColor(overlayColor);
    }

    private static class StopException extends RuntimeException {
    }

    private static StopException STOP_EXCEPTION = new StopException();

    static {
        try {
            BlurEffectView.class.getClassLoader().loadClass("android.support.v8.renderscript.RenderScript");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("RenderScript support not enabled. Add \"android { defaultConfig { renderscriptSupportModeEnabled true }}\" in your build.gradle");
        }
    }

    static Boolean DEBUG = null;

    static boolean isDebug(Context ctx) {
        if (DEBUG == null && ctx != null) {
            DEBUG = (ctx.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        }
        return DEBUG == Boolean.TRUE;
    }
}