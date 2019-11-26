package io.branch.search.widget.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

/**
 * A View that can show an app icon properly. This means displaying a shadow that follows
 * the app icon boundaries.
 */
public class AppIconView extends AppCompatImageView {

    public AppIconView(Context context) {
        super(context);
        initialize();
    }

    public AppIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public AppIconView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        Drawable fakeForeground = new ColorDrawable(Color.RED);
        Drawable fakeBackground = new ColorDrawable(Color.GREEN);
        AdaptiveIconDrawable fakeAdaptive = new AdaptiveIconDrawable(fakeBackground,
                fakeForeground);
        final Path fakePath = fakeAdaptive.getIconMask();
        final float fakePathDim = 100F; // AdaptiveIconDrawable.MASK_SIZE

        setOutlineProvider(new ViewOutlineProvider() {

            private Path mPath = new Path();
            private Matrix mMatrix = new Matrix();

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void getOutline(View view, Outline outline) {
                // We COULD check if the current drawable is adaptive, then use drawable.getOutline
                // but that's not reliable - the drawable path will be scaled differently depending
                // on the moment and we can't query the path size easily.
                mPath.set(fakePath);
                mMatrix.setScale(
                        view.getWidth() / fakePathDim,
                        view.getHeight() / fakePathDim);
                mPath.transform(mMatrix);
                outline.setConvexPath(mPath);
            }
        });
    }
}
