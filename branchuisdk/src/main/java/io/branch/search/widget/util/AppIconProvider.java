package io.branch.search.widget.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import io.branch.search.widget.R;

/**
 * This class deals with {@link AdaptiveIconDrawable}s in Oreo+ devices.
 * Some apps still do not have an adaptive icon in their manifest, so when we get
 * the icon from the package manager, it won't look good with respect to the other apps.
 *
 * This class will wrap the normal icon into a new {@link AdaptiveIconDrawable}.
 */
public class AppIconProvider {

    /**
     * Provides the right drawable for this app icon.
     */
    @NonNull
    public Drawable provideAppIcon(@NonNull Context context, @NonNull Drawable sourceAppIcon) {
        // If < 26, there's nothing we can do. If already adaptive, there's nothing we should do.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return sourceAppIcon;
        if (sourceAppIcon instanceof AdaptiveIconDrawable) return sourceAppIcon;

        Drawable background = new ColorDrawable(ContextCompat.getColor(context,
                R.color.branch_adaptive_icon_background));
        Drawable foreground = new ForegroundWrapper(sourceAppIcon, context.getResources());
        return new AdaptiveIconDrawable(background, foreground);
    }

    /**
     * Wraps a {@link #mSource} drawable. As per specs, this drawable will appear to be 108dp,
     * and will draw the source in the center part of itself, with 72dp dim reduced by a small
     * margin.
     */
    private class ForegroundWrapper extends Drawable {

        @NonNull private final Drawable mSource;

        // As per spec, the full dimension of the foreground drawable should be 108dp.
        // NOTE: this could also be taken from this drawable's bounds.width or height.
        private final int mFullDim;

        // As per spec, the canvas dimension of the foreground drawable will be 72dp. It is the
        // normally visible part.
        // NOTE: this could also be taken from canvas.width() or height().
        private final int mCanvasDim;

        // In addition, we will add a small border to the canvas, which means applying
        // a < 1 factor to the canvasDim.
        private final static float FACTOR = 0.8F;

        private ForegroundWrapper(@NonNull Drawable source, @NonNull Resources resources) {
            mSource = source;
            mFullDim = Math.round(108 * resources.getDisplayMetrics().density);
            mCanvasDim = Math.round(72 * resources.getDisplayMetrics().density);
        }

        /**
         * This is set to 108dp, but with negative left-top such that a 72dp Canvas will
         * be exactly centered.
         */
        @Override
        public void setBounds(int left, int top, int right, int bottom) {
            super.setBounds(left, top, right, bottom);
            float targetDim = mCanvasDim * FACTOR;
            float space = mCanvasDim - targetDim;
            int start = Math.round(space / 2F);
            int end = start + Math.round(targetDim);
            mSource.setBounds(start, start, end, end);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            mSource.draw(canvas);
        }

        @Override
        public void setAlpha(int alpha) {
            mSource.setAlpha(alpha);
        }

        @Override
        public int getOpacity() {
            return mSource.getOpacity();
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            mSource.setColorFilter(colorFilter);
        }

        @Override
        public int getIntrinsicWidth() {
            return mFullDim;
        }

        @Override
        public int getIntrinsicHeight() {
            return mFullDim;
        }
    }
}
